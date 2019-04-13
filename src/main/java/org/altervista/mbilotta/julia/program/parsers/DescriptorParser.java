/*
 * Copyright (C) 2015 Maurizio Bilotta.
 * 
 * This file is part of Julia. See <http://mbilotta.altervista.org/>.
 * 
 * Julia is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Julia is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Julia. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.altervista.mbilotta.julia.program.parsers;

import java.awt.Color;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.Formula;
import org.altervista.mbilotta.julia.Gradient;
import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.math.Real;
import org.altervista.mbilotta.julia.program.Application;
import org.altervista.mbilotta.julia.program.Cache;
import org.altervista.mbilotta.julia.program.JuliaSetPoint;
import org.altervista.mbilotta.julia.program.Profile;
import org.altervista.mbilotta.julia.program.Rectangle;
import org.altervista.mbilotta.julia.program.gui.SplashScreen;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class DescriptorParser extends Parser<Plugin> {
	
	public static final String DESCRIPTOR_NS_URI = "http://mbilotta.altervista.org/julia/descriptor";

	private final Profile profile;
	private final ClassLoader classLoader;
	private final BinaryRelation<String> localizationPreferences;
	private final Cache<Author> authorCache;
	private final Cache<Decimal> decimalCache;
	private final Cache<Color> colorCache;
	private final Cache<Gradient> gradientCache;

	private DocumentationWriter documentationWriter;
	private boolean localizationPreferencesChanged = false;
	
	private Map<String, PropertyDescriptor> propertyMap;

	public DescriptorParser(Profile profile,
			ClassLoader classLoader,
			BinaryRelation<String> localizationPreferences,
			Cache<Author> authorCache,
			Cache<Decimal> decimalCache,
			Cache<Color> colorCache,
			Cache<Gradient> gradientCache) throws SAXException, ParserConfigurationException {
		super("descriptor.xsd");
		this.profile = profile;
		this.classLoader = classLoader;
		this.localizationPreferences = localizationPreferences;
		this.authorCache = authorCache;
		this.decimalCache = decimalCache;
		this.colorCache = colorCache;
		this.gradientCache = gradientCache;
	}

	@Override
	protected void reset() {
		super.reset();
		documentationWriter = null;
		propertyMap = null;
	}

	@Override
	protected Path relativize(Path path) {
		return profile.relativizeDescriptor(path);
	}

	@Override
	protected Plugin validate(Document dom) throws ValidationException, InterruptedException {
		Element root = dom.getDocumentElement();
		XmlPath rootPath = new XmlPath(root);
		XmlPath currentPath = rootPath;
		if (!(root.getNamespaceURI().equals(DESCRIPTOR_NS_URI) && root.getLocalName().equals("plugin"))) {
			fatalError(ValidationException.atStartOf(
					currentPath,
					"Invalid root element: " + root));
			return null;
		}

		PluginFamily pluginFamily = PluginFamily.valueOf(getTypeAttribute(currentPath, root));

		Element offset = (Element) root.getFirstChild();
		assert offset.getLocalName().equals("class") : offset;
		XmlPath pluginTypePath = currentPath = currentPath.getChild(offset);
		Class<?> pluginType = parseClass(currentPath, offset, false, true);
		if (pluginType != null) {
			if (!pluginFamily.getInterfaceType().isAssignableFrom(pluginType)) {
				fatalError(ValidationException.atEndOf(
						currentPath,
						"Class " + pluginType.getName() + " does not implement " + pluginFamily.getInterfaceType()));
			}
		}

		try {
			propertyMap = inspectProperties(pluginType);
		} catch (IntrospectionException e) {
			fatalError(ValidationException.atEndOf(
					currentPath,
					"Could not inspect properties of class " + pluginType.getName(), e));
			propertyMap = Collections.emptyMap();
		}

		currentPath = currentPath.getParent();

		offset = (Element) offset.getNextSibling();

		Plugin rv;
		String pluginId = getPluginId();
		switch (pluginFamily) {
		case numberFactory: rv = new NumberFactoryPlugin(pluginId, pluginType); break;
		case formula: {
			assert offset.getLocalName().equals("mandelbrotSetDefaults") : offset;
			Decimal[] v = parseDecimals(offset, 4);
			Rectangle defaultMandelbrotSetRectangle = new Rectangle(v[0], v[1], v[2], v[3]);
			offset = (Element) offset.getNextSibling();

			assert offset.getLocalName().equals("juliaSetDefaults") : offset;
			v = parseDecimals(offset, 6);
			Rectangle defaultJuliaSetRectangle = new Rectangle(v[0], v[1], v[2], v[3]);
			JuliaSetPoint defaultJuliaSetPoint = new JuliaSetPoint(v[4], v[5]);
			offset = (Element) offset.getNextSibling();

			rv = new FormulaPlugin(pluginId, pluginType,
					defaultMandelbrotSetRectangle,
					defaultJuliaSetRectangle,
					defaultJuliaSetPoint);
		} break;
		case representation: rv = new RepresentationPlugin(pluginId, pluginType); break;
		default: throw new AssertionError(pluginFamily);
		}
		rv.setAuthors(Author.inspectAnnotations(pluginType, authorCache != null ? author -> authorCache.replace(author) : null));

		Object pluginInstance = null;
		if (pluginType != null) {
			try {
				pluginInstance = rv.initializeConstructor()
						.newInstance((Object[]) null);
			} catch (NoSuchMethodException e) {
				fatalError(ValidationException.atEndOf(
						pluginTypePath,
						"Could not find public default constructor in " + pluginType + "."));
			} catch (ReflectiveOperationException | ExceptionInInitializerError e) {
				fatalError(ValidationException.atEndOf(
						pluginTypePath,
						"Could not instantiate " + pluginType + ".", e));
			}
		}

		int index = 1;
		Map<String, Parameter<?>.Validator> idToValidator = new HashMap<>();
		LinkedList<Parameter<?>.Validator> validators = new LinkedList<>();
		while (offset != null && offset.getLocalName().equals("parameter")) {
			currentPath = currentPath.getChild(offset, index);

			String id = offset.getAttribute("id");
			println(currentPath.getAttributeChild("id"), id);

			String typeName = getTypeAttribute(currentPath, offset);
			
			Parameter<?> parameter;
			switch (typeName) {
			case "int": parameter = new IntParameter(id); break;
			case "double": parameter = new DoubleParameter(id); break;
			case "decimal": parameter = new DecimalParameter(id); break;
			case "real": parameter = new RealParameter(id); break;
			case "boolean": parameter = new BooleanParameter(id); break;
			case "enum": parameter = new EnumParameter(id); break;
			case "gradient": parameter = new GradientParameter(id); break;
			case "color": parameter = new ColorParameter(id); break;
			case "any": parameter = new AnyParameter(id); break;
			default: throw new AssertionError(typeName);
			}
			parameter.setIndex(index - 1);

			Parameter<?>.Validator validator = parameter.createValidator(this,
					currentPath,
					pluginType, pluginInstance);
			validator.validate(offset);

			idToValidator.put(id, validator);
			validators.add(validator);
			propertyMap.remove(id);

			currentPath = currentPath.getParent();
			offset = (Element) offset.getNextSibling();
			index++;
		}

		for (PropertyDescriptor propertyDescriptor : propertyMap.values()) {
			Parameter<?> parameter = createParameter(propertyDescriptor);
			parameter.setIndex(validators.size());
			Parameter<?>.Validator validator = parameter.createValidator(this,
					rootPath,
					pluginType, pluginInstance);
			idToValidator.put(parameter.getId(), validator);
			validators.add(validator);
		}

		Element presentationOffset = offset;
		List<String> languageTags = new ArrayList<>();
		while (offset != null) {
			Attr langAttr = offset.getAttributeNodeNS(XMLConstants.XML_NS_URI, "lang");
			languageTags.add(langAttr.getValue());
			offset = (Element) offset.getNextSibling();
		}

		String description = "";
		if (!languageTags.isEmpty()) {
			String languageTag = askUserWhichLanguageToUse(languageTags);
			if (languageTag != null) {
				offset = presentationOffset;
				Attr langAttr = offset.getAttributeNodeNS(XMLConstants.XML_NS_URI, "lang");
				XmlPath presentationPath = currentPath.getChild(offset, langAttr);
				while (offset != null && !languageTag.equals(langAttr.getValue())) {
					offset = (Element) offset.getNextSibling();
					langAttr = offset.getAttributeNodeNS(XMLConstants.XML_NS_URI, "lang");
					presentationPath = currentPath.getChild(offset, langAttr);
				}
				
				assert offset != null;
				
				currentPath = presentationPath;
				offset = (Element) offset.getFirstChild();
	
				String name = getNodeValueUnescaped(offset);
				if (name.length() == 0) {
					warning(ValidationException.atStartOf(currentPath.getChild(offset), "Empty plugin name."));
				} else {
					rv.setName(name);
				}
				offset = (Element) offset.getNextSibling();
	
				description = getNodeValueUnescaped(offset);
				if (description.length() == 0) {
					warning(ValidationException.atStartOf(currentPath.getChild(offset), "Empty plugin description."));
				}
				offset = (Element) offset.getNextSibling();
				
				for ( ; offset != null; offset = (Element) offset.getNextSibling()) {
					Attr idrefAttr = offset.getAttributeNode("id");
					currentPath = currentPath.getChild(offset, idrefAttr);
					String idref = idrefAttr.getValue();
					Parameter<?>.Validator validator = idToValidator.get(idref);
					if (validator == null) {
						warning(ValidationException.atStartOf(currentPath, "Attribute id points to the unspecified property " + idref + "."));
					} else {
						Element nameElement = (Element) offset.getFirstChild();
						String parameterName = getNodeValueUnescaped(nameElement);
						if (parameterName.length() == 0) {
							warning(ValidationException.atEndOf(currentPath.getChild(nameElement), "Empty name for property " + idref + "."));
						} else {
							validator.setParameterName(parameterName);
						}
						
						Element descriptionElement = (Element) offset.getLastChild();
						String parameterDescription = getNodeValueUnescaped(descriptionElement);
						if (parameterDescription.length() == 0) {
							warning(ValidationException.atEndOf(currentPath.getChild(descriptionElement), "Empty description for property " + idref + "."));
						} else {
							validator.setParameterDescription(parameterDescription);
						}
					}
					currentPath = currentPath.getParent();
				}
			}
		}

		// Validate remaining parameters
		for (PropertyDescriptor propertyDescriptor : propertyMap.values()) {
			idToValidator.get(propertyDescriptor.getName()).validate();
		}

		rv.setParameters(validators);
		rv.setHintGroups(validators);

		documentationWriter = new DocumentationWriter(rv, description, validators);

		return rv;
	}

	public Map<String, PropertyDescriptor> getPropertyMap() {
		return propertyMap;
	}

	public boolean localizationPreferencesChanged() {
		return localizationPreferencesChanged;
	}

	private String getPluginId() {
		Path relativeParent = profile.relativizeDescriptor(getCurrentFile().getParent());
		String fileName = getCurrentFile().getFileName().toString();
		assert fileName.endsWith(".xml");
		return relativeParent.resolve(fileName.substring(0, fileName.length() - 4))
				.toString()
				.replace(File.separatorChar, '/');
	}

	private String askUserWhichLanguageToUse(List<String> languages) throws InterruptedException {
		if (languages.size() > 1) {
			final List<String> reduction = localizationPreferences.reduce(languages);
			if (reduction.size() > 1) {
				try {
					String choice = Utilities.callSynchronously(new Callable<String>() {
						@Override
						public String call() throws Exception {
							SplashScreen splashScreen = Application.getSplashScreen();
							if (splashScreen != null) {
								splashScreen.setIndeterminate(true);
							}

							String rv = (String) JOptionPane.showInputDialog(
									splashScreen,
									"Choose a language between those available in " + getCurrentFile().getFileName(),
									"Julia",
									JOptionPane.PLAIN_MESSAGE,
									null,
									reduction.toArray(),
									reduction.get(0));

							if (splashScreen != null) {
								splashScreen.setIndeterminate(false);
							}

							return rv;
						}
					});

					if (choice != null) {
						reduction.remove(choice);
						localizationPreferences.put(choice, reduction);
						localizationPreferences.complete();
						localizationPreferencesChanged = true;
					}

					return choice;
				} catch (ExecutionException e) {
					throw new AssertionError(e);
				}
			}
			
			return reduction.get(0);
		}
		
		return languages.get(0);
	}

	static String getTypeAttribute(XmlPath elementPath, Element element) {
		Attr typeAttr = element.getAttributeNodeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type");
		String rv = typeAttr.getValue();
		int colonIndex = rv.indexOf(':');
		if (colonIndex != -1)
			rv = rv.substring(colonIndex + 1);
		
		println(elementPath.getChild(typeAttr), rv);
		return rv;
	}

	Class<?> parseClass(XmlPath elementPath, Node element, boolean enforceEnum, boolean enforceConcrete) throws ValidationException {
		String className = getNodeValue(element);

		Class<?> rv;
		try {
			rv = Class.forName(className, false, classLoader);
			println(elementPath, rv);
			if (enforceEnum && !rv.isEnum()) {
				rv = null;
				fatalError(ValidationException.atEndOf(elementPath, "Type " + className + " not an enum type"));
			} else if (rv.isPrimitive() || rv.isArray()) {
				rv = null;
				fatalError(ValidationException.atEndOf(elementPath, "Type " + className + " is a primitive type or array type"));
			} else {
				int modifiers = rv.getModifiers();
				if (!Modifier.isPublic(modifiers))
					fatalError(ValidationException.atEndOf(elementPath, "Type " + className + " does not have public visibilty"));

				if (Modifier.isInterface(modifiers)) {
					rv = null;
					fatalError(ValidationException.atEndOf(elementPath, "Type " + className + " is an interface"));
				} else {
					if (rv.getEnclosingClass() != null && !Modifier.isStatic(modifiers))
						fatalError(ValidationException.atEndOf(elementPath, "Type " + className + " is a non-static member type"));
					if (enforceConcrete && Modifier.isAbstract(modifiers))
						fatalError(ValidationException.atEndOf(elementPath, "Type " + className + " is an abstract class"));
				}
			}
		} catch (ClassNotFoundException | LinkageError e) {
			rv = null;
			println(elementPath, null);
			fatalError(ValidationException.atEndOf(elementPath, "Reflection of type " + className + " has failed", e));
		}

		return rv;
	}

	Decimal parseDecimal(Node element) {
		return replace(new Decimal(getNodeValue(element)));
	}

	Color parseColor(Element element) {
		String[] components = getNodeValue(element).split("\\s+");
		assert !components[0].isEmpty();
		return replace(new Color(
				Integer.parseInt(components[0]),
				Integer.parseInt(components[1]),
				Integer.parseInt(components[2]),
				Integer.parseInt(element.getAttribute("alpha"))));
	}

	Decimal[] parseDecimals(Node parent, int n) {
		Decimal[] rv = new Decimal[n];
		int i = 0;
		for (Node offset = parent.getFirstChild(); offset != null; offset = offset.getNextSibling()) {
			Decimal value = parseDecimal(offset);
			rv[i++] = value;
		}
		
		return rv;
	}

	Decimal replace(Decimal d) {
		return decimalCache != null ? decimalCache.replace(d) : d;
	}

	Color replace(Color c) {
		return colorCache != null ? colorCache.replace(c) : c;
	}

	Gradient replace(Gradient g) {
		return gradientCache != null ? gradientCache.replace(g) : g;
	}

	public DocumentationWriter getDocumentationWriter() {
		return documentationWriter;
	}

	private static String getNodeValueUnescaped(Node node) {
		Node child = node.getFirstChild();
		if (child == null)
			return "";
		if (child == node.getLastChild())
			return unescape(child);

		StringBuilder sb = new StringBuilder();
		for ( ; child != null; child = child.getNextSibling()) {
			sb.append(unescape(child));
		}
		
		return sb.toString();
	}

	private static String unescape(Node node) {
		String value = node.getNodeValue();
		if (node.getNodeType() == Node.CDATA_SECTION_NODE)
			return value;

		int length = value.length();
		StringBuilder sb = null;
		for (int i = 0; i < length; i++) {
			char c = value.charAt(i);
			switch (c) {
			case '[': if (sb == null) sb = new StringBuilder(value); sb.setCharAt(i, '<'); break;
			case ']': if (sb == null) sb = new StringBuilder(value); sb.setCharAt(i, '>'); break;
			case '$': if (sb == null) sb = new StringBuilder(value); sb.setCharAt(i, '&'); break;
			}
		}

		if (sb != null) return sb.toString();
		return value;
	}
	
	static Parameter<?> createParameter(PropertyDescriptor propertyDescriptor) {
		Class<?> type = propertyDescriptor.getPropertyType();
		String name = propertyDescriptor.getName();
		Parameter<?> parameter;
		if (type == int.class) {
			parameter = new IntParameter(name);
		} else if (type == double.class) {
			parameter = new DoubleParameter(name);
		} else if (type == Decimal.class) {
			parameter = new DecimalParameter(name);
		} else if (type == Real.class) {
			parameter = new RealParameter(name);
		} else if (type == boolean.class) {
			parameter = new BooleanParameter(name);
		} else if (type.isEnum()) {
			parameter = new EnumParameter(name);
		} else if (type == Gradient.class) {
			parameter = new GradientParameter(name);
		} else if (type == Color.class) {
			parameter = new ColorParameter(name);
		} else {
			parameter = new AnyParameter(name);
		}
		return parameter;
	}

	static Map<String, PropertyDescriptor> inspectProperties(Class<?> pluginType) throws IntrospectionException {
		BeanInfo beanInfo = Introspector.getBeanInfo(pluginType, Object.class, Introspector.IGNORE_ALL_BEANINFO);
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		boolean isFormula = Formula.class.isAssignableFrom(pluginType);

		Map<String, PropertyDescriptor> rv = new LinkedHashMap<>(propertyDescriptors.length);
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			if (propertyDescriptor.getPropertyType() == null) {
				// Indexed property that does not support non-indexed access
				continue;
			}

			if (isFormula && (propertyDescriptor.getName().equals("c") || propertyDescriptor.getName().equals("z"))) {
				// Property required by interface Formula
				continue;
			}

			rv.put(propertyDescriptor.getName(), propertyDescriptor);
		}

		return rv;
	}
}

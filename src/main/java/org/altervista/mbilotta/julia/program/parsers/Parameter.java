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

import static org.altervista.mbilotta.julia.Utilities.join;
import static org.altervista.mbilotta.julia.Utilities.writeList;
import static org.altervista.mbilotta.julia.program.parsers.Parser.println;

import java.awt.event.KeyEvent;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ReflectPermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import org.altervista.mbilotta.julia.Groups;
import org.altervista.mbilotta.julia.Previewable;
import org.altervista.mbilotta.julia.Representation;
import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.math.Real;
import org.altervista.mbilotta.julia.program.gui.ParameterChangeListener;
import org.altervista.mbilotta.julia.program.gui.PreviewUpdater;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;


public abstract class Parameter<T> implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private final String id;
	private transient int index;
	private transient Plugin plugin;
	private transient Class<? super T> type;
	private transient Method setterMethod;
	private transient ArrayList<T> hints;
	private String name;

	static final boolean CAN_SUPPRESS_ACCESS_CHECKS;
	
	static {
		SecurityManager sm = System.getSecurityManager();
		boolean canSuppressAccessChecks = true;
		if (sm != null) {
			try {
				sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
			} catch (SecurityException e) {
				canSuppressAccessChecks = false;
			}
		}
		CAN_SUPPRESS_ACCESS_CHECKS = canSuppressAccessChecks;
	}

	abstract class Validator {
		final DescriptorParser descriptorParser;
		final XmlPath parameterPath;
		boolean parameterPreviewable;
		final Class<?> pluginType;
		final Object pluginInstance;
		T getterHint;
		Set<String> getterGroupSet;
		Map<String, T> referencedGroups = new HashMap<>();
		String parameterDescription = "";
		final PropertyDescriptor propertyDescriptor;
		final int position;

		public Validator(DescriptorParser descriptorParser,
				XmlPath parameterPath,
				Class<?> pluginType, Object pluginInstance) throws DomValidationException {
			this.descriptorParser = descriptorParser;
			this.parameterPath = parameterPath;
			this.pluginType = pluginType;
			this.pluginInstance = pluginInstance;
			this.propertyDescriptor = descriptorParser.getPropertyMap().get(id);
			this.position = parameterPath.isRoot() ? DomValidationException.END_OF_ELEMENT : DomValidationException.START_OF_ELEMENT;
		}

		public final void validate() throws DomValidationException, ClassValidationException {
			validate(null);
		}

		public void validate(Element node) throws DomValidationException, ClassValidationException {
			init(parameterPath);
			validateHints(node != null ? (Element) node.getFirstChild() : null);
		}

		public void validateHints(Element offset) throws DomValidationException {
			if (getterHint != null) {
				hints.add(getterHint);
			}
			if (getterGroupSet != null) {
				for (String group : getterGroupSet) {
					referencedGroups.put(group, getterHint);
				}
			}

			int index = 1;
			while (offset != null && offset.getLocalName().equals("hint")) {
				XmlPath hintPath = parameterPath.getChild(offset, index);
				T value = validateHint(hintPath, offset);
				Attr groupsAttr = offset.getAttributeNode("groups");
				if (groupsAttr != null) {
					List<String> groupList = Arrays.asList(groupsAttr.getValue().split("\\s+"));
					assert !groupList.isEmpty() && !groupList.get(0).isEmpty();
					println(hintPath.getAttributeChild("groups"), groupList);
					Set<String> groupSet;
					if (groupList.size() == 1) {
						groupSet = Collections.singleton(groupList.get(0));
					} else {
						groupSet = new HashSet<>(groupList);
					}
					if (groupSet.size() < groupList.size()) {
						descriptorParser.warning(DomValidationException.atStartOf(
								hintPath,
								"Redundant group list."));
					}
					for (String group : groupSet) {
						if (referencedGroups.containsKey(group)) {
							T referencedValue = referencedGroups.get(group);
							if (referencedValue == null || value == null || !referencedValue.equals(value)) {
								descriptorParser.error(DomValidationException.atEndOf(
										hintPath,
										"Parameter " + id + " is already partecipating in group " + group +
										" with a (possibly) different value."));
								if (referencedValue != null)
									referencedGroups.put(group, null);
							} else {
								descriptorParser.warning(DomValidationException.atEndOf(
										hintPath,
										"Parameter " + id + " is already partecipating in group " + group +
										" with the same value."));
							}
						} else {
							referencedGroups.put(group, value);
							if (value != null && !hints.contains(value)) {
								hints.add(value);
							}
						}
					}
				} else if (value != null) {
					if (!hints.contains(value)) {
						hints.add(value);
					} else {
						descriptorParser.warning(DomValidationException.atEndOf(
								hintPath,
								"Redundant hint for parameter " + id +  "."));
					}
				}

				index++;
				offset = (Element) offset.getNextSibling();
			}

			hints.trimToSize();
			T explicitDefault = referencedGroups.get("default");
			if (explicitDefault != null) {
				int i = hints.indexOf(explicitDefault);
				if (i > 0) {
					hints.remove(i);
					hints.add(0, explicitDefault);
				}
			} else if (hints.size() > 0) {
				referencedGroups.put("default", hints.get(0));
			} else {
				descriptorParser.fatalError(DomValidationException.atEndOf(
						parameterPath,
						"No default value for parameter " + id + "."));
			}
		}
		
		public Parameter<T> getParameter() {
			return Parameter.this;
		}

		public Class<?> getPluginType() {
			return pluginType;
		}

		public boolean isParameterPreviewable() {
			return parameterPreviewable;
		}

		public Map<String, T> getReferencedGroups() {
			return referencedGroups;
		}

		public T getParameterDefault() {
			return getHint(0);
		}
		
		public String getParameterId() {
			return getId();
		}

		public Class<? super T> getParameterType() {
			return getType();
		}

		public String getParameterName() {
			return getName();
		}

		public void setParameterName(String parameterName) {
			setName(parameterName);
		}

		public String getParameterDescription() {
			return parameterDescription;
		}

		public void setParameterDescription(String parameterDescription) {
			this.parameterDescription = parameterDescription;
		}

		public abstract T validateHint(XmlPath hintPath, Element hint) throws DomValidationException;

		public abstract String getXMLParameterType();

		public void writeValueToHTML(HTMLWriter out, Object value) {
			out.openAndClose("td", value.toString(), false, "class", "value");
		}

		public abstract void writeConstraintsToHTML(HTMLWriter out);
		
		Class<?> inspectType(XmlPath currentPath) throws DomValidationException {
			if (propertyDescriptor != null) {
				return propertyDescriptor.getPropertyType();
			}

			descriptorParser.fatalError(new DomValidationException(
					currentPath,
					position,
					"Could not find property \"" + id + "\" in class " + pluginType.getName()));
			return null;
		}

		void init(XmlPath currentPath) throws DomValidationException, ClassValidationException {
			if (pluginType != null) {
				if (type == null) {
					setDescriptorType(inspectType(currentPath));
				}
				
				if (type != null) {
					Method setter = findSetter();
					Method getter = findGetter();
					setSetterMethod(setter);
					parameterPreviewable = setter != null && setter.isAnnotationPresent(Previewable.class);
					if (parameterPreviewable) {
						if (Representation.class.isAssignableFrom(pluginType)) {
							if (type == Real.class) {
								descriptorParser.warning(new ClassValidationException(
										this,
										"Previewable parameters of type \"real\" will not be passed to NumberFactory"));
							}
						} else {
							descriptorParser.error(new ClassValidationException(
									this,
									"Previewable parameters can be specified only when plugin type is \"representation\"."));
						}
					}
					if (pluginInstance != null && getter != null) {
						try {
							getterHint = (T) getter.invoke(pluginInstance, (Object[]) null);
							if (getterHint != null) {
								Groups annotation = getter.getAnnotation(Groups.class);
								if (annotation != null) {
									List<String> groupList = Arrays.asList(annotation.value().split("\\s+"));
									if (groupList.get(0).isEmpty()) {
										groupList = groupList.subList(1, groupList.size());
									}
									switch (groupList.size()) {
									case 0:
										getterGroupSet = Collections.emptySet();
										descriptorParser.warning(new ClassValidationException(this, "Empty group list from getter method annotation."));
										break;
									case 1:
										getterGroupSet = Collections.singleton(groupList.get(0));
										break;
									default:
										getterGroupSet = new HashSet<>(groupList);
									}
									if (getterGroupSet.size() < groupList.size()) {
										descriptorParser.warning(new ClassValidationException(this, "Redundant group list from getter method annotation."));
									}
								}
							} else {
								String message = "Getter method " + getter.getName() + " has returned null.";
								descriptorParser.warning(new ClassValidationException(this, message));
							}
						} catch (ReflectiveOperationException e) {
							String message = "Reflective invocation of getter method " + getter.getName() + " has failed.";
							descriptorParser.error(new ClassValidationException(this, message, e));
						}
					}
				}
			}
		}

		private Method findSetter() throws ClassValidationException {
			Method rv;

			if (propertyDescriptor != null) {
				rv = propertyDescriptor.getWriteMethod();
				if (rv != null) {
					if (rv.getParameterTypes()[0] == type) {
						if (!rv.isAccessible() && CAN_SUPPRESS_ACCESS_CHECKS) {
							rv.setAccessible(true);
						}
						return rv;
					}
				}
			}

			try {
				rv = Parameter.findSetter(id, type, pluginType);
			} catch (NoSuchMethodException e) {
				descriptorParser.fatalError(new ClassValidationException(this, e.getMessage()));
				return null;
			}

			return rv;
		}

		private Method findGetter() throws ClassValidationException {
			Method rv;

			if (propertyDescriptor != null) {
				rv = propertyDescriptor.getReadMethod();
				if (rv != null) {
					if (rv.getReturnType() == type) {
						if (!rv.isAccessible() && CAN_SUPPRESS_ACCESS_CHECKS) {
							rv.setAccessible(true);
						}
						return rv;
					}
				}
			}

			try {
				rv = Parameter.findGetter(id, type, pluginType);
			} catch (NoSuchMethodException e) {
				descriptorParser.fatalError(new ClassValidationException(this, e.getMessage()));
				return null;
			}

			return rv;
		}

	}

	public Parameter(String id) {
		this.id = id;
		this.hints = new ArrayList<>();
		this.name = id;
	}

	public Parameter(String id, Class<? super T> type) {
		this(id);
		this.type = type;
	}

	void setIndex(int index) {
		this.index = index;
	}

	void setPlugin(Plugin plugin) {
		this.plugin = plugin;
	}

	void setType(Class<? super T> type) {
		this.type = type;
	}
	
	@SuppressWarnings("unchecked")
	void setDescriptorType(Class<?> type) {
		this.type = (Class<? super T>) type;
	}

	void setSetterMethod(Method setterMethod) {
		this.setterMethod = setterMethod;
	}

	void setName(String name) {
		this.name = name;
	}

	abstract void initConstraints() throws ClassValidationException;
	
	abstract Validator createValidator(DescriptorParser descriptorParser,
			XmlPath parameterPath,
			Class<?> pluginType, Object pluginInstance) throws DomValidationException;

	public final String getId() {
		return id;
	}

	public final int getIndex() {
		return index;
	}

	public final Plugin getPlugin() {
		return plugin;
	}

	public Class<? super T> getType() {
		return type;
	}

	public final Method getSetterMethod() {
		return setterMethod;
	}
	
	public final T getHint(int index) {
		return hints.get(index);
	}

	public final int getNumOfHints() {
		return hints.size();
	}

	public final List<T> getHints() {
		return Collections.unmodifiableList(hints);
	}

	public final String getName() {
		return name;
	}

	public boolean acceptsValue(Object value) {
		return type.isInstance(value);
	}
	
	public String toString() {
		return toStringBuilder()
				.append(']').toString();
	}

	public JComponent createEditor() {
		return createEditor(getHint(0));
	}

	public JComponent cloneEditor(JComponent editor) {
		return createEditor(getEditorValue(editor));
	}

	public abstract JComponent createEditor(Object initialValue);
	public abstract void disposeEditor(JComponent editor);
	public abstract Object getEditorValue(JComponent editor);
	public abstract void setEditorValue(JComponent editor, Object value);
	public abstract void setPreviewUpdater(PreviewUpdater previewUpdater, JComponent editor);
	public abstract void addParameterChangeListener(ParameterChangeListener l, JComponent editor);
	public abstract boolean isEditorExpandable();
	public boolean isEditorBaselineProvided() { return true; }
	public abstract boolean isEditorConsistent(JComponent editor);

	public JMenuItem createHintMenuItem(int index) {
		int mnemonic = index % 10;
		JMenuItem rv = new JMenuItem("<html><u>" + mnemonic + "</u>&nbsp;&nbsp;&nbsp;" + hints.get(index));
		rv.setMnemonic(mnemonic + KeyEvent.VK_0);
		return rv;
	}

	public static String getSetterName(String propertyName) {
		return getXterName("set", propertyName);
	}

	public static String getGetterName(String propertyName) {
		return getXterName("get", propertyName);
	}

	public static String getXterName(String prefix, String propertyName) {
		char[] chars = propertyName.toCharArray();
		chars[0] = Character.toUpperCase(chars[0]);
		return new StringBuilder()
			.append(prefix).append(chars).toString();
	}

	public static Method findSetter(String propertyName, Class<?> propertyType, Class<?> targetType)
			throws NoSuchMethodException {
		return findMethod(getSetterName(propertyName), propertyType, null, targetType, false, true);
	}

	public static Method findGetter(String propertyName, Class<?> propertyType, Class<?> targetType)
			throws NoSuchMethodException {
		if (propertyType == boolean.class) {
			NoSuchMethodException rethrow = null;
			Method rv = null;
			try {
				rv = findMethod(getGetterName(propertyName), null, propertyType, targetType, false, false);
			} catch (NoSuchMethodException e) {
				rethrow = e;
			}
			if (rv == null) {
				try {
					rv = findMethod(getXterName("is", propertyName), null, propertyType, targetType, false, false);
				} catch (NoSuchMethodException e) {
					if (rethrow == null) {
						rethrow = e;
					}
				}
			}
			if (rv == null && rethrow != null) {
				throw rethrow;
			}
			return rv;
		}
		return findMethod(getGetterName(propertyName), null, propertyType, targetType, false, false);
	}

	public static String getMethodString(Method m) {
		Class<?>[] parameterTypes = m.getParameterTypes();
		assert parameterTypes.length < 2;
		return getMethodString(m.getName(),
				parameterTypes.length > 0 ? parameterTypes[0] : null,
				m.getReturnType(),
				m.getDeclaringClass(),
				Modifier.isStatic(m.getModifiers()));
	}

	public static String getMethodString(String name, Class<?> argumentType, Class<?> returnType, Class<?> targetType, boolean isStatic) {
		if (returnType == null)
			returnType = Void.TYPE;
		StringBuilder sb = new StringBuilder("public ");
		if (isStatic) {
			sb.append("static ");
		}
		sb.append(returnType.getName()).append(' ')
			.append(targetType.getName()).append('.')
			.append(name).append('(');
		if (argumentType != null) {
			sb.append(argumentType.getName());
		}
		return sb.append(')').toString();
	}

	public static String getNoSuchMethodMessage(String name, Class<?> argumentType, Class<?> returnType, Class<?> targetType, boolean isStatic) {
		if (returnType == null)
			returnType = Void.TYPE;
		boolean getterOrSetter = !isStatic && name.matches("[sg]et[A-Z].*") &&
				((name.charAt(0) == 'g' && returnType != Void.TYPE && argumentType == null) ||
				 (name.charAt(0) == 's' && returnType == Void.TYPE && argumentType != null));
		return "Could not find " +
			(getterOrSetter ? name.substring(0, 3) + "ter" : "method") + ": " +
			getMethodString(name, argumentType, returnType, targetType, isStatic) + ".";
	}

	public static Constructor<?> findConstructor(Class<?> argumentType, Class<?> targetType)
			throws NoSuchMethodException {
		Class<?>[] parameterTypes = argumentType == null ? null : new Class<?>[] { argumentType };
		Constructor<?> rv;
		try {
			rv = targetType.getConstructor(parameterTypes);
		} catch (NoSuchMethodException e) {
			StringBuilder sb = new StringBuilder("Could not find constructor: public ");
			sb.append(targetType.getName()).append('(');
			if (argumentType != null) {
				sb.append(argumentType.getName());
			}
			throw new NoSuchMethodException(sb.append(')').toString());
		}

		if (!rv.isAccessible() && CAN_SUPPRESS_ACCESS_CHECKS) {
			rv.setAccessible(true);
		}
		return rv;
	}

	public static Method findMethod(String name, Class<?> argumentType, Class<?> returnType, Class<?> targetType, boolean isStatic, boolean required)
			throws NoSuchMethodException {
		Class<?>[] parameterTypes = argumentType == null ? null : new Class<?>[] { argumentType };
		if (returnType == null)
			returnType = Void.TYPE;
		Method rv;
		try {
			rv = targetType.getMethod(name, parameterTypes);
		} catch (NoSuchMethodException e) {
			if (required) {
				throw new NoSuchMethodException(
						getNoSuchMethodMessage(name, argumentType, returnType, targetType, isStatic));
			}
			return null;
		}

		StringBuilder sb = new StringBuilder();
		if (isStatic && !Modifier.isStatic(rv.getModifiers())) {
			sb.append("An instance method ");
		}
		if (!isStatic && Modifier.isStatic(rv.getModifiers())) {
			sb.append("A static method ");
		}
		if (rv.getReturnType() != returnType) {
			if (sb.length() == 0) {
				sb.append("A method ");
			}
			sb.append("with different return type ");
		}
		if (sb.length() > 0) {
			sb.append("was found instead.");
			throw new NoSuchMethodException(
					getNoSuchMethodMessage(name, argumentType, returnType, targetType, isStatic) +
					" " + sb.toString());
		}
		
		if (!rv.isAccessible() && CAN_SUPPRESS_ACCESS_CHECKS) {
			rv.setAccessible(true);
		}
		return rv;
	}

	StringBuilder toStringBuilder() {
		return new StringBuilder().append(getClass().getSimpleName()).append("[id=").append(id)
				.append(", type=").append(type.getName())
				.append(", hints=").append(hints)
				.append(", name=").append(name);
	}

	static InvalidObjectException newIOException(String message) {
		return new InvalidObjectException(message);
	}

	static InvalidObjectException newIOException(String message, Throwable cause) {
		InvalidObjectException rv = new InvalidObjectException(message);
		rv.initCause(cause);
		return rv;
	}

	static InvalidObjectException newIOException(String fieldName, NoSuchMethodException e) {
		String message = e.getMessage();
		String[] parts = message.split("[\\.:]\\s");
		assert parts.length == 2 || parts.length == 3;
		StringBuilder sb = new StringBuilder().
				append('[').append(fieldName).append('=').
				append(parts[1], 0, parts.length > 2 ? parts[1].length() : parts[1].length() - 1).
				append("] could not be found.");
		if (parts.length > 2) {
			sb.append(' ').append(parts[2]);
		}
		return newIOException(sb.toString());
	}

	void writeHints(ObjectOutputStream out)
			throws IOException {
		writeList(out, hints);
	}

	void readHints(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		int size = in.readInt();
		if (size < 0)
			throw newIOException('[' + id + ".hints.length=" + size + "] is negative");

		hints = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			Object hint = Utilities.read(in, join(id, ".hints[", i, "]"));
			addHint((T) hint);
		}
	}

	void validateHints() throws IOException {
		List<T> hints = this.hints;
		int size = hints.size();
		for (int i = 0; i < size; i++) {
			T hint = hints.get(i);
			if (!acceptsValue(hint)) {
				throw newIOException('[' + id + ".hints[" + i + "]=" + hint + "] not accepted");
			}
		}
	}

	void addHint(T value) {
		hints.add(value);
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		if (id == null) throw newIOException("id is null.");
		if (name == null) name = id;
	}
}

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
import static org.altervista.mbilotta.julia.Utilities.readNonNull;
import static org.altervista.mbilotta.julia.program.parsers.Parser.getNodeValue;
import static org.altervista.mbilotta.julia.program.parsers.Parser.println;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JMenuItem;
import javax.swing.text.DefaultFormatter;

import org.altervista.mbilotta.julia.Out;
import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.program.gui.JuliaFormattedTextField;
import org.altervista.mbilotta.julia.program.gui.ParameterChangeListener;
import org.altervista.mbilotta.julia.program.gui.PreviewUpdater;
import org.w3c.dom.Element;


final class AnyParameter extends Parameter<Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private transient Constructor<?> constructor;
	private transient Method parseMethod;
	private transient Method toStringMethod;
	private transient Method acceptsMethod;

	public static String getTypeName(Class<?> type) {
		if (type.isArray()) {
			return "ArrayOf" + getTypeName(type.getComponentType());
		}
		if (type.isPrimitive()) {
			return capitalize(type.getName());
		}
		return capitalize(type.getSimpleName());
	}

	public static String capitalize(String s) {
		if (s.isEmpty()) {
			return s;
		}
		StringBuilder sb = new StringBuilder(s);
		sb.setCharAt(0, Character.toUpperCase(s.charAt(0)));
		return sb.toString();
	}

	@Override
	void initConstraints() {
	}

	private class Validator extends Parameter<Object>.Validator {

		public Validator(DescriptorParser descriptorParser,
				XmlPath parameterPath,
				Class<?> pluginType, Object pluginInstance) throws DomValidationException {
			AnyParameter.this.super(descriptorParser, parameterPath, pluginType, pluginInstance);
		}

		@Override
		Class<?> inspectType(XmlPath currentPath) throws DomValidationException {
			Class<?> rv = super.inspectType(currentPath);
			if (rv != null) {
				try {
					initMethods(pluginType, rv, null);
				} catch (NoSuchMethodException e) {
					descriptorParser.fatalError(new DomValidationException(currentPath, position, e.getMessage()));
					rv = null;
				}
			}
			return rv;
		}

		public Object validateHint(XmlPath hintPath, Element hint) throws DomValidationException {
			String valueString = getNodeValue(hint);
			Object value;
			try {
				if (getType().equals(String.class)) {
					value = valueString;
				} else {
					value = parseMethod == null ?
							(constructor == null ?
									null :
									constructor.newInstance(valueString)) :
							parseMethod.invoke(null, valueString);
				}
				if (value != null) {
					if (acceptsMethod != null && !((Boolean) acceptsMethod.invoke(null, value))) {
						value = null;
						descriptorParser.fatalError(DomValidationException.atEndOf(hintPath, "Suggested value " + valueString + " not accepted."));
					}
				} else {
					valueString = "null";
					descriptorParser.fatalError(DomValidationException.atEndOf(hintPath, "Parse method " + parseMethod.getName() + " has returned null."));
				}
			} catch (ReflectiveOperationException e) {
				value = null;
				valueString = "null";
				descriptorParser.fatalError(DomValidationException.atEndOf(hintPath, "Reflective invocation has failed.", e));
			} finally {
				println(hintPath, valueString);
			}

			return value;
		}

		public void validate(Element node) throws DomValidationException, ClassValidationException {
			XmlPath currentPath = parameterPath;
			init(currentPath);

			try {
				if (getterHint != null &&
						acceptsMethod != null &&
						!((Boolean) acceptsMethod.invoke(null, getterHint))) {
					String valueString;
					try {
						valueString = toStringMethod != null ? (String) toStringMethod.invoke(null, getterHint) : getterHint.toString();
					} catch (ReflectiveOperationException e) {
						valueString = null;
					}
					getterHint = null;
					descriptorParser.fatalError(new ClassValidationException(this,
							"Suggested value (from getter) " + (valueString != null ? valueString : "null") + " not accepted."));
				}
			} catch (ReflectiveOperationException e) {
				getterHint = null;
				descriptorParser.fatalError(new ClassValidationException(this, "Reflective invocation has failed.", e));
			}

			Element offset = node != null ? (Element) node.getFirstChild() : null;
			validateHints(offset);
		}

		public String getXMLParameterType() {
			return "any";
		}

		public void writeValueToHTML(HTMLWriter out, Object value) {
			AnyParameter parameter = (AnyParameter) getParameter();
			String text = parameter.toString(value);
			if (text != null) {
				out.openAndClose("td", text, true, "class", "value");
			} else {
				out.open("td");
				out.openAndClose("em", "Error while printing value", false);
				out.close();
			}
		}

		public void writeConstraintsToHTML(HTMLWriter out) {
			AnyParameter p = (AnyParameter) getParameter();
			out.openAndClose("h3", "Constraints", false);
			StringBuilder sb = new StringBuilder();
			sb.append("Accepts any value of type <code>" + p.getType().getCanonicalName() + "</code>");
			if (p.getAcceptsMethod() != null) {
				sb.append(" for which method <code>" + p.getAcceptsMethod() + "</code> returns <code>true</code>");
			}
			sb.append('.');
			out.openAndClose("p", sb.toString(), false);
		}
	}

	Validator createValidator(DescriptorParser descriptorParser,
			XmlPath parameterPath,
			Class<?> pluginType, Object pluginInstance) throws DomValidationException {
		return new Validator(descriptorParser, parameterPath, pluginType, pluginInstance);
	}
	
	void initMethods(Class<?> pluginType, Out<String> fieldNameOut) throws NoSuchMethodException {
		initMethods(pluginType, getType(), fieldNameOut);
	}

	void initMethods(Class<?> pluginType, Class<?> propertyType, Out<String> fieldNameOut) throws NoSuchMethodException {
		if (fieldNameOut == null) fieldNameOut = Out.nullOut();

		if (propertyType != String.class) {
			fieldNameOut.set("parseMethod");
			parseMethod = findMethod(
					"parse" + getTypeName(propertyType),
					String.class,
					propertyType,
					pluginType,
					true,
					propertyType.isPrimitive() || propertyType.isArray() || propertyType.isInterface() || Modifier.isAbstract(propertyType.getModifiers()));
			
			if (parseMethod == null) {
				fieldNameOut.set("constructor");
				constructor = findConstructor(String.class, propertyType);
			}

			fieldNameOut.set("toStringMethod");
			toStringMethod = findMethod(
					"format",
					propertyType,
					String.class,
					pluginType,
					true,
					propertyType.isArray());
		}

		fieldNameOut.set("acceptsMethod");
		acceptsMethod = findMethod(
				"validate" + capitalize(getId()),
				propertyType,
				boolean.class,
				pluginType,
				true,
				false);
		
		if (acceptsMethod == null) {
			acceptsMethod = findMethod(
					"validate",
					propertyType,
					boolean.class,
					pluginType,
					true,
					false);
		}
	}

	public AnyParameter(String id) {
		super(id);
	}

	@Override
	public Object parseValue(String s) {
		try {
			return parseValueImpl(s);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(s, e);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	public JComponent createEditor(Object initialValue) {
		return new JuliaFormattedTextField(new Formatter(), initialValue, this);
	}

	public void disposeEditor(JComponent editor) {
	}

	@Override
	public JComponent cloneEditor(JComponent editor) {
		JuliaFormattedTextField src = (JuliaFormattedTextField) editor;
		JuliaFormattedTextField dst = (JuliaFormattedTextField) createEditor(src.getValue());
		dst.setText(src.getText());
		try {
			dst.commitEdit();
		} catch (ParseException e) {}
		return dst;
	}

	public Object getEditorValue(JComponent editor) {
		return ((JFormattedTextField) editor).getValue();
	}

	public void setEditorValue(JComponent editor, Object value) {
		((JFormattedTextField) editor).setValue(value);
	}

	public void setPreviewUpdater(PreviewUpdater previewUpdater, JComponent editor) {
		((JuliaFormattedTextField) editor).setPreviewUpdater(previewUpdater);
	}

	public void addParameterChangeListener(ParameterChangeListener listener, JComponent editor) {
		((JuliaFormattedTextField) editor).addParameterChangeListener(listener);
	}

	public boolean isEditorExpandable() {
		return true;
	}

	public boolean isEditorConsistent(JComponent editor) {
		return ((JFormattedTextField) editor).isEditValid();
	}

	@Override
	public JMenuItem createHintMenuItem(int index) {
		String text = toString(getHint(index));
		if (text == null) {
			return new JMenuItem("<html><em>Error while printing value");
		}

		int mnemonic = index % 10;
		JMenuItem rv = new JMenuItem("<html><u>" + mnemonic + "</u>&nbsp;&nbsp;&nbsp;" + text);
		rv.setMnemonic(mnemonic + KeyEvent.VK_0);
		return rv;
	}

	public Method getParseMethod() {
		return parseMethod;
	}

	public Method getToStringMethod() {
		return toStringMethod;
	}

	public Method getAcceptsMethod() {
		return acceptsMethod;
	}

	@Override
	public boolean acceptsValue(Object value) {
		if (super.acceptsValue(value)) {
			try {
				return acceptsMethod == null ? true : (Boolean) acceptsMethod.invoke(null, value);
			} catch (InvocationTargetException e) {
				logClientCodeError(e);
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(e);
			}
		}
		return false;
	}

	static void logClientCodeError(Throwable throwable) {
		Utilities.printStackTrace(throwable);
	}

	private String toString(Object value) {
		try {
			return toStringMethod == null ? value.toString() : (String) toStringMethod.invoke(null, value);
		} catch (InvocationTargetException e) {
			logClientCodeError(e);
			return null;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private Object parseValueImpl(String text) throws InvocationTargetException, ReflectiveOperationException {
		if (getType().equals(String.class)) {
			return text;
		}
		return parseMethod == null ? constructor.newInstance(text) : parseMethod.invoke(null, text);
	}

	private class Formatter extends DefaultFormatter {
		public Formatter() {
			setAllowsInvalid(true);
			setCommitsOnValidEdit(false);
			setOverwriteMode(false);
			setValueClass(getType());
		}

		public Object stringToValue(String text) throws ParseException {
			Object value;
			try {
				value = parseValueImpl(text);
			} catch (InvocationTargetException e) {
				throw new ParseException("Value could not be parsed.", 0);
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(e);
			}

			try {
				if (acceptsMethod == null || (Boolean) acceptsMethod.invoke(null, value)) {
					return value;
				}
			} catch (InvocationTargetException e) {
				logClientCodeError(e);
				throw new ParseException("Client code error caught.", 0);
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(e);
			}

			throw new ParseException("Parameter constraints violated.", 0);
		}

		public String valueToString(Object value) throws ParseException {
			String rv = AnyParameter.this.toString(value);
			if (rv != null) {
				return rv;
			}
			
			throw new ParseException("Client code error caught.", 0);
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();

		out.writeObject(getType());

		writeHints(out);
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		Class<?> type = readNonNull(in, join(getId(), ".type"), Class.class);
		if (type.isPrimitive() || type.isArray())
			throw newIOException('[' + getId() + ".type=" + type.getName() + "] is a primitive type or array type.");

		int modifiers = type.getModifiers();
		if (!Modifier.isPublic(modifiers))
			throw newIOException('[' + getId() + ".type=" + type.getName() + "] does not have public visibilty.");
		if (Modifier.isInterface(modifiers))
			throw newIOException('[' + getId() + ".type=" + type.getName() + "] is an interface.");
		if (type.getEnclosingClass() != null && !Modifier.isStatic(modifiers)) 
			throw newIOException('[' + getId() + ".type=" + type.getName() + "] is a non-static member class.");

		setDescriptorType(type);

		readHints(in);
	}
}

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

import static org.altervista.mbilotta.julia.program.parsers.Parser.getNodeValue;
import static org.altervista.mbilotta.julia.program.parsers.Parser.println;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import javax.swing.JComponent;
import javax.swing.JSpinner;

import org.altervista.mbilotta.julia.program.gui.IntParameterEditor;
import org.altervista.mbilotta.julia.program.gui.ParameterChangeListener;
import org.altervista.mbilotta.julia.program.gui.PreviewUpdater;
import org.w3c.dom.Element;


public final class IntParameter extends Parameter<Integer> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Min {
		int value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Max {
		int value();
	}

	private transient int min;
	private transient int max;

	@Override
	void initConstraints() throws ClassValidationException {
		Method setter = getSetterMethod();
		if (setter == null) {
			min = Integer.MIN_VALUE;
			max = Integer.MAX_VALUE;
			return;
		}

		Min minAnnotation = setter.getAnnotation(Min.class);
		if (minAnnotation != null) {
			min = minAnnotation.value();
		} else {
			min = Integer.MIN_VALUE;
		}

		Max maxAnnotation = setter.getAnnotation(Max.class);
		if (maxAnnotation != null) {
			max = maxAnnotation.value();
		} else {
			max = Integer.MAX_VALUE;
		}

		if (max < min) {
			String message = "Invalid range [" + min + ", " + max + "].";
			max = Integer.MAX_VALUE;
			min = Integer.MIN_VALUE;
			throw new ClassValidationException(this, message);
		}
	}
	
	private class Validator extends Parameter<Integer>.Validator {

		public Validator(DescriptorParser descriptorParser,
				XmlPath parameterPath,
				Class<?> pluginType, Object pluginInstance) throws DomValidationException {
			IntParameter.this.super(descriptorParser, parameterPath, pluginType, pluginInstance);
		}

		public Integer validateHint(XmlPath hintPath, Element hint) throws DomValidationException {
			int value = Integer.parseInt(getNodeValue(hint));
			println(hintPath, value);
			if (value > max || value < min) {
				descriptorParser.fatalError(DomValidationException.atEndOf(
						hintPath,
						"Suggested value "+ value + " lies outside range."));
				return null;
			}
			return value;
		}

		public void validate(Element node) throws DomValidationException, ClassValidationException {
			XmlPath currentPath = parameterPath;
			init(currentPath);

			try {
				initConstraints();
			} catch (ClassValidationException e) {
				descriptorParser.fatalError(e);
			}

			Element offset = node != null ? (Element) node.getFirstChild() : null;
			validateHints(offset);
		}

		public String getXMLParameterType() {
			return "int";
		}

		public void writeConstraintsToHTML(HTMLWriter out) {
			out.openAndClose("h3", "Constraints", false);
			out.openAndClose("p", getDomainString(), false);
		}
	}

	public IntParameter(String id) {
		super(id, int.class);
	}

	Validator createValidator(DescriptorParser descriptorParser,
			XmlPath parameterPath,
			Class<?> pluginType, Object pluginInstance) throws DomValidationException {
		return new Validator(descriptorParser, parameterPath, pluginType, pluginInstance);
	}

	@Override
	public Integer parseValue(String s) {
		return Integer.valueOf(s);
	}

	public JComponent createEditor(Object initialValue) {
		return new IntParameterEditor((Integer) initialValue,
				min,
				max,
				1,
				this);
	}

	public void disposeEditor(JComponent editor) {
	}

	public Object getEditorValue(JComponent editor) {
		return ((JSpinner) editor).getValue();
	}

	public void setEditorValue(JComponent editor, Object value) {
		((JSpinner) editor).setValue(value);
	}

	public void setPreviewUpdater(PreviewUpdater previewUpdater, JComponent editor) {
		((IntParameterEditor) editor).setPreviewUpdater(previewUpdater);
	}

	public void addParameterChangeListener(ParameterChangeListener listener, JComponent editor) {
		((IntParameterEditor) editor).addParameterChangeListener(listener);
	}

	public boolean isEditorExpandable() {
		return false;
	}

	public boolean isEditorConsistent(JComponent editor) {
		return ((IntParameterEditor) editor).isEditValid();
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	@Override
	public boolean acceptsValue(Object value) {
		if (value instanceof Integer) {
			int i = (Integer) value;
			return min <= i && max >= i;
		}
		
		return false;
	}

	public String getDomainString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Accepts any ");
		if (min == Integer.MIN_VALUE && max == Integer.MAX_VALUE) {
			sb.append("value in the <code>int</code> range");
		} else {
			sb.append("value in the range from ");
			sb.append("<code>");
			if (min == Integer.MIN_VALUE)
				sb.append("Integer.MIN_VALUE");
			else
				sb.append(min);
			sb.append("</code>");
			sb.append(" to ");
			sb.append("<code>");
			if (max == Integer.MAX_VALUE)
				sb.append("Integer.MAX_VALUE");
			else
				sb.append(max);
			sb.append("</code>");
		}
		sb.append('.');

		return sb.toString();
	}

	public String toString() {
		return toStringBuilder()
				.append(", min=").append(min)
				.append(", max=").append(max).append(']').toString();
	}

	private void writeObject(ObjectOutputStream out)
			throws IOException {
		out.defaultWriteObject();
		writeHints(out);
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		setType(int.class);
		readHints(in);
	}
}

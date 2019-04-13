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

import static org.altervista.mbilotta.julia.Utilities.readNonNullList;
import static org.altervista.mbilotta.julia.Utilities.join;
import static org.altervista.mbilotta.julia.Utilities.writeList;
import static org.altervista.mbilotta.julia.program.parsers.Parser.println;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.text.DefaultFormatter;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.program.gui.JuliaFormattedTextField;
import org.altervista.mbilotta.julia.program.gui.ParameterChangeListener;
import org.altervista.mbilotta.julia.program.gui.PreviewUpdater;

import org.w3c.dom.Element;


class DecimalParameter extends Parameter<Decimal> {

	private static final long serialVersionUID = 1L;

	private Decimal min;
	private Decimal max;
	private boolean minInclusive;
	private boolean maxInclusive;
	private transient List<Decimal> exceptions = new LinkedList<>();

	class Validator extends Parameter<Decimal>.Validator {

		public Validator(DescriptorParser descriptorParser,
				XmlPath parameterPath,
				Class<?> pluginType, Object pluginInstance) throws ValidationException {
			DecimalParameter.this.super(descriptorParser, parameterPath, pluginType, pluginInstance);
		}

		@Override
		public void validate(Element node) throws ValidationException {
			XmlPath currentPath = parameterPath;
			init(currentPath);

			Element offset = node != null ? (Element) node.getFirstChild() : null;
			if (offset!= null && offset.getLocalName().startsWith("min")) {
				currentPath = parameterPath.getChild(offset);
				min = descriptorParser.parseDecimal(offset);
				println(currentPath, min);

				if (offset.getLocalName().endsWith("Inclusive")) {
					minInclusive = true;
				}
				
				offset = (Element) offset.getNextSibling();
			}

			if (offset != null && offset.getLocalName().startsWith("max")) {
				currentPath = parameterPath.getChild(offset);
				max = descriptorParser.parseDecimal(offset);
				println(currentPath, max);

				if (offset.getLocalName().endsWith("Inclusive")) {
					maxInclusive = true;
				}

				offset = (Element) offset.getNextSibling();
			}
			
			if (max != null && min != null &&
				(max.compareTo(min) < 0 || (max.compareTo(min) == 0 && (!minInclusive || !maxInclusive)))) {
				String leftPar = minInclusive ? "[" : "(";
				String rightPar = maxInclusive ? "]" : ")";
				String message = "Invalid range " + leftPar + min + ", " + max + rightPar + ".";
				max = null;
				min = null;
				descriptorParser.fatalError(ValidationException.atEndOf(currentPath, message));
			}
			
			for (int index = 1; offset != null && offset.getLocalName().equals("exception"); index++) {
				currentPath = parameterPath.getChild(offset, index);
				Decimal exception = descriptorParser.parseDecimal(offset);
				println(currentPath, exception);
				if (isValueInsideDomain(exception, min, max, minInclusive, maxInclusive, exceptions)) {
					exceptions.add(exception);
				} else {
					descriptorParser.warning(ValidationException.atEndOf(
							currentPath,
							"Value " + exception + " already excluded from the domain."));
				}

				offset = (Element) offset.getNextSibling();
			}

			if (getterHint != null &&
					!isValueInsideDomain(getterHint, min, max, minInclusive, maxInclusive, exceptions)) {
				String message = "Suggested value (from getter) " + getterHint + " lies outside domain."; 
				getterHint = null;
				descriptorParser.fatalError(ValidationException.atEndOf(currentPath, message));
			} else {
				getterHint = descriptorParser.replace(getterHint);
			}
			validateHints(offset);
		}

		public Decimal validateHint(XmlPath hintPath, Element hint) throws ValidationException {
			Decimal value = descriptorParser.parseDecimal(hint);
			println(hintPath, value);
			if (!isValueInsideDomain(value, min, max, minInclusive, maxInclusive, exceptions)) {
				descriptorParser.fatalError(ValidationException.atEndOf(
						hintPath,
						"Suggested value "+ value + " lies outside domain."));
				return null;
			}
			return value;
		}

		public String getXMLParameterType() {
			return "decimal";
		}

		public void writeConstraintsToHTML(HTMLWriter out) {
			out.openAndClose("h3", "Constraints", false);
			out.openAndClose("p", getDomainString(), false);
		}
	}

	public DecimalParameter(String id) {
		super(id);
		setType(getType());
	}

	@Override
	public Class<? super Decimal> getType() {
		return Decimal.class;
	}

	Validator createValidator(DescriptorParser descriptorParser,
			XmlPath parameterPath,
			Class<?> pluginType, Object pluginInstance) throws ValidationException {
		return new Validator(descriptorParser, parameterPath, pluginType, pluginInstance);
	}

	public static boolean isValueInsideDomain(Decimal value,
			Decimal min, Decimal max,
			boolean minInclusive, boolean maxInclusive,
			List<Decimal> exceptions) {
		boolean rangeRespected =
				(min == null || (minInclusive ? min.compareTo(value) <= 0 : min.compareTo(value) < 0)) &&
				(max == null || (maxInclusive ? max.compareTo(value) >= 0 : max.compareTo(value) > 0));
			
			if (rangeRespected) {
				for (Decimal exception : exceptions) {
					if (exception.compareTo(value) == 0)
						return false;
				}
				
				return true;
			}
			
			return false;
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

	public boolean isEditorExpandable() {
		return true;
	}

	public boolean isEditorConsistent(JComponent editor) {
		return ((JFormattedTextField) editor).isEditValid();
	}

	public void setPreviewUpdater(PreviewUpdater previewUpdater, JComponent editor) {
		((JuliaFormattedTextField) editor).setPreviewUpdater(previewUpdater);
	}

	public void addParameterChangeListener(ParameterChangeListener listener, JComponent editor) {
		((JuliaFormattedTextField) editor).addParameterChangeListener(listener);
	}

	public Decimal getMin() {
		return min;
	}

	public Decimal getMax() {
		return max;
	}

	public boolean isMinInclusive() {
		return minInclusive;
	}

	public boolean isMaxInclusive() {
		return maxInclusive;
	}

	public List<Decimal> getExceptions() {
		return Collections.unmodifiableList(exceptions);
	}

	@Override
	public boolean acceptsValue(Object value) {
		if (value instanceof Decimal) {
			return isValueInsideDomain((Decimal) value, min, max, minInclusive, maxInclusive, exceptions);
		}

		return false;
	}

	public String getDomainString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Accepts any value");
		if (min != null && max != null) {
			sb.append(" in the range from ");
			sb.append("<code>").append(min).append("</code>");
			if (minInclusive)
				sb.append(" (inclusive)");
			else
				sb.append(" (exclusive)");
			sb.append(" to ");
			sb.append("<code>").append(max).append("</code>");
			if (maxInclusive)
				sb.append(" (inclusive)");
			else
				sb.append(" (exclusive)");
		} else if (min != null) {
			sb.append(" greater than ");
			if (minInclusive)
				sb.append("or equal to ");
			sb.append("<code>").append(min).append("</code>");
		} else if (max != null) {
			sb.append(" less than ");
			if (maxInclusive)
				sb.append("or equal to ");
			sb.append("<code>").append(max).append("</code>");
		}

		List<Decimal> exceptions = this.exceptions;
		int exceptionCount = exceptions.size();
		if (exceptionCount > 0) {
			sb.append(", except for ");
			sb.append("<code>").append(exceptions.get(0)).append("</code>");
			for (int i = 1; i < exceptionCount; i++) {
				sb.append(", <code>").append(exceptions.get(i)).append("</code>");	
			}
		}
		sb.append('.');

		return sb.toString();
	}

	public String toString() {
		return toStringBuilder()
				.append(", min=").append(min)
				.append(", minInclusive=").append(minInclusive)
				.append(", max=").append(max)
				.append(", maxInclusive=").append(maxInclusive)
				.append(", exceptions=").append(exceptions).append(']').toString();
	}

	private class Formatter extends DefaultFormatter {
		public Formatter() {
			setAllowsInvalid(true);
			setCommitsOnValidEdit(false);
			setOverwriteMode(false);
			setValueClass(Decimal.class);
		}

		public Object stringToValue(String text) throws ParseException {
			try {
				Decimal value = new Decimal(text);
				if (isValueInsideDomain(value, min, max, minInclusive, maxInclusive, exceptions))
					return value;
			} catch (NumberFormatException e) {
				throw new ParseException("NumberFormatException caught.", 0);
			}

			throw new ParseException("Parameter constraints violated.", 0);
		}
	}

	private void writeObject(ObjectOutputStream out)
			throws IOException {
		out.defaultWriteObject();
		writeList(out, exceptions);
		writeHints(out);
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		if (max != null && min != null &&
			(max.compareTo(min) < 0 || (max.compareTo(min) == 0 && (!minInclusive || !maxInclusive)))) {
			String minString = minInclusive ? ".minInclusive=" : ".minExclusive=";
			String maxString = maxInclusive ? ".maxInclusive=" : ".maxExclusive=";
			throw newIOException('[' + getId() + minString + min + "; " + getId() + maxString + max + "] is not a valid range.");
		}

		exceptions = readNonNullList(in, join(getId(), ".exceptions"), Decimal.class);

		setType(getType());
		readHints(in);
	}
}

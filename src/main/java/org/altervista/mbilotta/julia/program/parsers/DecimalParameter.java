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

import static org.altervista.mbilotta.julia.program.parsers.Parser.println;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.text.DefaultFormatter;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.program.gui.JuliaFormattedTextField;
import org.altervista.mbilotta.julia.program.gui.ParameterChangeListener;
import org.altervista.mbilotta.julia.program.gui.PreviewUpdater;
import org.w3c.dom.Element;


public class DecimalParameter extends Parameter<Decimal> {

	private static final long serialVersionUID = 1L;

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Min {
		String value();
		boolean inclusive() default true;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Max {
		String value();
		boolean inclusive() default true;
	}

	@Repeatable(ForbidValues.class)
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Forbid {
		String value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface ForbidValues {
		Forbid[] value();
	}

	private transient Decimal min;
	private transient Decimal max;
	private transient boolean minInclusive;
	private transient boolean maxInclusive;
	private transient List<Decimal> exceptions;

	@Override
	void initConstraints() throws ClassValidationException {
		Method setter = getSetterMethod();

		Forbid[] forbidAnnotations = setter.getAnnotationsByType(Forbid.class);
		exceptions = new ArrayList<>(forbidAnnotations.length);

		Min minAnnotation = setter.getAnnotation(Min.class);
		if (minAnnotation != null) {
			try {
				min = new Decimal(minAnnotation.value().trim());
			} catch (NumberFormatException e) {
				throw new ClassValidationException(this, "Invalid @Min = " + e.getMessage());
			}

			minInclusive = minAnnotation.inclusive();
		}

		Max maxAnnotation = setter.getAnnotation(Max.class);
		if (maxAnnotation != null) {
			try {
				max = new Decimal(maxAnnotation.value().trim());
			} catch (NumberFormatException e) {
				throw new ClassValidationException(this, "Invalid @Max = " + e.getMessage());
			}

			maxInclusive = maxAnnotation.inclusive();
		}

		if (!hasValidRange()) {
			String leftPar = minInclusive ? "[" : "(";
			String rightPar = maxInclusive ? "]" : ")";
			String message = "Invalid range " + leftPar + min + ", " + max + rightPar + ".";
			max = null;
			min = null;
			throw new ClassValidationException(this, message);
		}

		for (int i = 0; i < forbidAnnotations.length; i++) {
			try {
				Decimal value = new Decimal(forbidAnnotations[i].value().trim());
				if (isValueInsideDomain(value, min, max, minInclusive, maxInclusive, exceptions)) {
					exceptions.add(value);
				}
			} catch (NumberFormatException e) {
				throw new ClassValidationException(this, "Invalid @Forbid = " + e.getMessage());
			}
		}
	}

	class Validator extends Parameter<Decimal>.Validator {

		public Validator(DescriptorParser descriptorParser,
				XmlPath parameterPath,
				Class<?> pluginType, Object pluginInstance) throws DomValidationException {
			DecimalParameter.this.super(descriptorParser, parameterPath, pluginType, pluginInstance);
		}

		private void checkForbidRedundancy() throws ClassValidationException {
			Forbid[] forbidAnnotations = getSetterMethod().getAnnotationsByType(Forbid.class);
			if (exceptions.size() < forbidAnnotations.length) {
				List<Decimal> redundantValues = Arrays.stream(forbidAnnotations)
					.map(annotation -> new Decimal(annotation.value().trim()))
					.collect(Collectors.toList());

				exceptions.forEach(exception -> {
					redundantValues.remove(exception);
				});

				for (Decimal value : redundantValues) {
					descriptorParser.warning(new ClassValidationException(this,
							"Value " + value + " already excluded from the domain."));
				}
			}
		}

		@Override
		public void validate(Element node) throws DomValidationException, ClassValidationException {
			XmlPath currentPath = parameterPath;
			init(currentPath);

			boolean hasValidConstraints = true;
			try {
				initConstraints();
			} catch (ClassValidationException e) {
				hasValidConstraints = false;
				descriptorParser.fatalError(e);
			}

			if (hasValidConstraints) {
				checkForbidRedundancy();
			}

			if (getterHint != null &&
					!isValueInsideDomain(getterHint, min, max, minInclusive, maxInclusive, exceptions)) {
				String message = "Suggested value (from getter) " + getterHint + " lies outside domain."; 
				getterHint = null;
				descriptorParser.fatalError(new ClassValidationException(this, message));
			} else {
				getterHint = descriptorParser.replace(getterHint);
			}

			Element offset = node != null ? (Element) node.getFirstChild() : null;
			validateHints(offset);
		}

		public Decimal validateHint(XmlPath hintPath, Element hint) throws DomValidationException {
			Decimal value = descriptorParser.parseDecimal(hint);
			println(hintPath, value);
			if (!isValueInsideDomain(value, min, max, minInclusive, maxInclusive, exceptions)) {
				descriptorParser.fatalError(DomValidationException.atEndOf(
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
			Class<?> pluginType, Object pluginInstance) throws DomValidationException {
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

	public boolean hasValidRange() {
		return !(
			   max != null
			&& min != null
			&& (max.compareTo(min) < 0 || (max.compareTo(min) == 0 && !(minInclusive && maxInclusive)))
		);
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
		writeHints(out);
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		setType(getType());
		readHints(in);
	}
}

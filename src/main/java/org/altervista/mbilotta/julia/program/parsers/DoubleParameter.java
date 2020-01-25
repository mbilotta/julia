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

import org.altervista.mbilotta.julia.program.gui.JuliaFormattedTextField;
import org.altervista.mbilotta.julia.program.gui.ParameterChangeListener;
import org.altervista.mbilotta.julia.program.gui.PreviewUpdater;
import org.w3c.dom.Element;


public final class DoubleParameter extends Parameter<Double> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Min {
		double value();
		boolean inclusive() default true;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Max {
		double value();
		boolean inclusive() default true;
	}

	@Repeatable(ForbidValues.class)
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Forbid {
		double value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface ForbidValues {
		Forbid[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface AllowNaN {

	}

	private transient Double min;
	private transient Double max;
	private transient boolean minInclusive;
	private transient boolean maxInclusive;
	private transient List<Double> exceptions;
	private transient boolean acceptsNaN;

	@Override
	void initConstraints() throws ClassValidationException {
		Method setter = getSetterMethod();
		if (setter == null) {
			exceptions = Collections.emptyList();
			return;
		}

		Forbid[] forbidAnnotations = setter.getAnnotationsByType(Forbid.class);
		exceptions = new ArrayList<>(forbidAnnotations.length);

		Min minAnnotation = setter.getAnnotation(Min.class);
		if (minAnnotation != null) {
			min = minAnnotation.value();
			minInclusive = minAnnotation.inclusive();
		}

		Max maxAnnotation = setter.getAnnotation(Max.class);
		if (maxAnnotation != null) {
			max = maxAnnotation.value();
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
			double value = forbidAnnotations[i].value();
			if (isValueInsideDomain(value, min, max, minInclusive, maxInclusive, exceptions, false)) {
				exceptions.add(value);
			}
		}
		
		acceptsNaN = setter.isAnnotationPresent(AllowNaN.class);
	}

	private class Validator extends Parameter<Double>.Validator {

		public Validator(DescriptorParser descriptorParser,
				XmlPath parameterPath,
				Class<?> pluginType, Object pluginInstance) throws DomValidationException {
			DoubleParameter.this.super(descriptorParser, parameterPath, pluginType, pluginInstance);
		}

		private void checkForbidRedundancy() throws ClassValidationException {
			Forbid[] forbidAnnotations = getSetterMethod().getAnnotationsByType(Forbid.class);
			if (exceptions.size() < forbidAnnotations.length) {
				List<Double> redundantValues = Arrays.stream(forbidAnnotations)
					.map(annotation -> annotation.value())
					.collect(Collectors.toList());

				exceptions.forEach(exception -> {
					redundantValues.remove(exception);
				});

				for (Double value : redundantValues) {
					descriptorParser.warning(new ClassValidationException(this,
							"Value " + value + " already excluded from the domain."));
				}
			}
		}

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
					!isValueInsideDomain(getterHint, min, max, minInclusive, maxInclusive, exceptions, acceptsNaN)) {
				String message = "Suggested value (from getter) " + getterHint + " lies outside domain.";
				getterHint = null;
				descriptorParser.fatalError(new ClassValidationException(this, message));
			}

			Element offset = node != null ? (Element) node.getFirstChild() : null;
			validateHints(offset);
		}

		public Double validateHint(XmlPath hintPath, Element hint) throws DomValidationException {
			Double value = Double.valueOf(DescriptorParser.getNodeValue(hint));
			println(hintPath, value);
			if (!DoubleParameter.isValueInsideDomain(value, min, max, minInclusive, maxInclusive, exceptions, acceptsNaN)) {
				descriptorParser.fatalError(DomValidationException.atEndOf(
						hintPath,
						"Suggested value "+ value + " lies outside domain."));
				return null;
			}
			return value;
		}

		public String getXMLParameterType() {
			return "double";
		}

		public void writeConstraintsToHTML(HTMLWriter out) {
			out.openAndClose("h3", "Constraints", false);
			out.openAndClose("p", getDomainString(), false);
		}
	}
	
	Validator createValidator(DescriptorParser descriptorParser,
			XmlPath parameterPath,
			Class<?> pluginType, Object pluginInstance) throws DomValidationException {
		return new Validator(descriptorParser, parameterPath, pluginType, pluginInstance);
	}

	public DoubleParameter(String id) {
		super(id, double.class);
	}

	public static boolean isValueInsideDomain(double value,
			Double min, Double max,
			boolean minInclusive, boolean maxInclusive,
			List<Double> exceptions, boolean includesNaN) {
		if (Double.isNaN(value))
			return includesNaN;

		boolean rangeRespected =
			(min == null || (minInclusive ? Double.compare(min, value) <= 0 : Double.compare(min, value) < 0)) &&
			(max == null || (maxInclusive ? Double.compare(max, value) >= 0 : Double.compare(max, value) > 0));
		
		if (rangeRespected) {
			for (Double exception : exceptions) {
				if (Double.compare(exception, value) == 0)
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

	public Double getMin() {
		return min;
	}

	public Double getMax() {
		return max;
	}

	public boolean isMinInclusive() {
		return minInclusive;
	}

	public boolean isMaxInclusive() {
		return maxInclusive;
	}

	public boolean hasValidRange() {
		if ((min != null && min.isNaN()) || (max != null && max.isNaN())) {
			return false;
		}
		if (max != null && min != null
				&& (max.compareTo(min) < 0 || (max.compareTo(min) == 0 && !(minInclusive && maxInclusive)))) {
			return false;
		}
		return true;
	}

	public List<Double> getExceptions() {
		return Collections.unmodifiableList(exceptions);
	}

	public boolean acceptsNaN() {
		return acceptsNaN;
	}

	@Override
	public boolean acceptsValue(Object value) {
		if (value instanceof Double) {
			return isValueInsideDomain((Double) value, min, max, minInclusive, maxInclusive, exceptions, acceptsNaN);
		}

		return false;
	}

	public String getDomainString() {
		String numericDomain = getNumericDomainString();
		return acceptsNaN ? numericDomain + " Also accepts <code>NaN</code>, <code>+NaN</code>, <code>-NaN</code>." : numericDomain;
	}

	public String getNumericDomainString() {
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

		List<Double> exceptions = this.exceptions;
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
				.append(", exceptions=").append(exceptions)
				.append(", acceptsNaN=").append(acceptsNaN).append(']').toString();
	}

	private class Formatter extends DefaultFormatter {
		public Formatter() {
			setAllowsInvalid(true);
			setCommitsOnValidEdit(false);
			setOverwriteMode(false);
			setValueClass(Double.class);
		}

		@Override
		public Object stringToValue(String text) throws ParseException {
			try {
				Double value = Double.valueOf(text);
				if (isValueInsideDomain(value, min, max, minInclusive, maxInclusive, exceptions, acceptsNaN))
					return value;
			} catch (NumberFormatException e) {
				throw new ParseException("NumberFormatException caught", 0);
			}

			throw new ParseException("Parameter constraints violated", 0);
		}

		@Override
		public String valueToString(Object value) throws ParseException {
			String rv = super.valueToString(value);
			if (rv.endsWith(".0"))
				rv = rv.substring(0, rv.length() - 2);
			return rv;
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

		setType(double.class);
		readHints(in);
	}
}

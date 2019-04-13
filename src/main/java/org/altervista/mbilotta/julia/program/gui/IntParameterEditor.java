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

package org.altervista.mbilotta.julia.program.gui;

import java.awt.event.FocusListener;
import java.awt.event.MouseListener;
import java.text.ParseException;

import javax.swing.AbstractSpinnerModel;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.text.DefaultFormatter;

import org.altervista.mbilotta.julia.program.parsers.Parameter;


public class IntParameterEditor extends JSpinner {
	
	private JuliaFormattedTextField textField;

	public IntParameterEditor(Integer value, int min, int max, int stepSize, Parameter<Integer> subject) {
		super(new Model(value, min, max, stepSize, subject));
		textField = ((Editor) getEditor()).getTextField();
	}

	public void setPreviewUpdater(PreviewUpdater previewUpdater) {
		textField.setPreviewUpdater(previewUpdater);
	}

	public void addParameterChangeListener(ParameterChangeListener listener) {
		textField.addParameterChangeListener(listener);
	}

	@Override
	public void addFocusListener(FocusListener l) {
		textField.addFocusListener(l);
	}

	@Override
	public void removeFocusListener(FocusListener l) {
		textField.removeFocusListener(l);
	}

	@Override
	public void addMouseListener(MouseListener l) {
		textField.addMouseListener(l);
	}

	@Override
	public void removeMouseListener(MouseListener l) {
		textField.removeMouseListener(l);
	}

	@Override
	public boolean requestFocusInWindow() {
		return textField.requestFocusInWindow();
	}

	public boolean isEditValid() {
		return textField.isEditValid();
	}

	@Override
	protected JComponent createEditor(SpinnerModel model) {
		return new Editor(this);
	}

	private static class Editor extends DefaultEditor {

		public Editor(IntParameterEditor spinner) {
			super(spinner);
			JFormattedTextField oldTextField = super.getTextField();

			Model model = (Model) spinner.getModel();
			Formatter formatter = new Formatter(model);
			JFormattedTextField textField =
					new JuliaFormattedTextField(formatter, spinner.getValue(), model.getSubject());
			textField.setName("Spinner.formattedTextField");
			textField.addPropertyChangeListener(this);
			textField.setInheritsPopupMenu(true);

			String toolTipText = spinner.getToolTipText();
			if (toolTipText != null) {
				textField.setToolTipText(toolTipText);
			}

			ActionMap oldActionMap = oldTextField.getActionMap();
			ActionMap actionMap = textField.getActionMap();
			if (oldActionMap != null && actionMap != null) {
				actionMap.put("increment", oldActionMap.get("increment"));
				actionMap.put("decrement", oldActionMap.get("decrement"));
			}

			textField.setHorizontalAlignment(JTextField.RIGHT);

			try {
				String maxString = formatter.valueToString(model.getMinimum());
				String minString = formatter.valueToString(model.getMaximum());
				textField.setColumns(Math.max(maxString.length(), minString.length()));
			} catch (ParseException e) {
			}

			remove(oldTextField);
			add(textField);
		}

		public JuliaFormattedTextField getTextField() {
			return (JuliaFormattedTextField) super.getTextField();
		}
	}

	private static class Formatter extends DefaultFormatter {

		private final Model model;

		public Formatter(Model model) {
			this.model = model;
			setAllowsInvalid(true);
			setCommitsOnValidEdit(false);
			setOverwriteMode(false);
			setValueClass(Integer.class);
		}

		public Object stringToValue(String text) throws ParseException {
			try {
				Integer value = Integer.valueOf(text);
				if (value >= model.getMinimum() &&  value <= model.getMaximum()) {
					return value;
				}
			} catch (NumberFormatException e) {
				throw new ParseException("NumberFormatException caught", 0);
			}

			throw new ParseException("Parameter constraints violated", 0);
		}
	}

	private static class Model extends AbstractSpinnerModel {

		private Integer value;
		private final int min;
		private final int max;
		private final int stepSize;

		private final Parameter<Integer> subject;

		public Model(Integer value, int min, int max, int stepSize, Parameter<Integer> subject) {
			assert min <= max;
			assert stepSize > 0;
			assert value >= min && value <= max;

			this.value = value;
			this.min = min;
			this.max = max;
			this.stepSize = stepSize;
			this.subject = subject;
		}

		public int getMinimum() {
			return min;
		}

		public int getMaximum() {
			return max;
		}

		public Parameter<Integer> getSubject() {
			return subject;
		}

		public void setValue(Object value) {
			this.value = (Integer) value;
			fireStateChanged();
		}

		public Integer getValue() {
			return value;
		}

		public Object getNextValue() {
			int rv = value + stepSize;
			if (value > 0 && rv < 0) // Overflow
				return null;

			if (rv > max)
				return null;

			return rv;
		}

		public Object getPreviousValue() {
			int rv = value - stepSize;
			if (value < 0 && rv > 0) // Overflow
				return null;

			if (rv < min)
				return null;

			return rv;
		}
	}
}

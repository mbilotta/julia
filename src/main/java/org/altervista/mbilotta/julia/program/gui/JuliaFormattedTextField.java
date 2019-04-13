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

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFormattedTextField;
import javax.swing.UIManager;
import javax.swing.text.DefaultFormatterFactory;

import org.altervista.mbilotta.julia.program.parsers.Parameter;


public class JuliaFormattedTextField extends JFormattedTextField {
	
	private final Parameter<?> subject;
	private UpdaterAdapter updaterAdapter;

	private static Color defaultForeground;

	static {
		defaultForeground = UIManager.getColor("FormattedTextField.foreground");
		UIManager.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				if (e.getPropertyName().equals("lookAndFeel")) {
					defaultForeground = UIManager.getColor("FormattedTextField.foreground");
				}
			}
		});
	}

	public JuliaFormattedTextField(AbstractFormatter formatter, Object value, Parameter<?> subject) {
		super(value);
		setFormatterFactory(new DefaultFormatterFactory(formatter));
		setFocusLostBehavior(COMMIT);
		this.subject = subject;
		addPropertyChangeListener("editValid", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ((Boolean) evt.getNewValue()) {
					setForeground(defaultForeground);
				} else {
					setForeground(Color.RED);
				}
			}
		});
	}

	public void setPreviewUpdater(PreviewUpdater previewUpdater) {
		if (updaterAdapter != null) {
			removePropertyChangeListener("value", updaterAdapter);
			updaterAdapter = null;
		}

		if (previewUpdater != null) {
			updaterAdapter = new UpdaterAdapter(previewUpdater);
			addPropertyChangeListener("value", updaterAdapter);
		}
	}

	public void addParameterChangeListener(ParameterChangeListener listener) {
		addPropertyChangeListener("value", new ListenerAdapter(listener));
	}

	public void postChangeEvent() {
		PropertyChangeListener[] listeners = getPropertyChangeListeners("value");
		for (PropertyChangeListener listener : listeners) {
			if (listener instanceof ListenerAdapter) {
				((ListenerAdapter) listener).parameterChanged(getValue());
			}
		}
	}

	public void revert() {
		setValue(getValue());
	}

	private class ListenerAdapter implements PropertyChangeListener {
		private final ParameterChangeListener listener;

		public ListenerAdapter(ParameterChangeListener listener) {
			this.listener = listener;
		}

		public void propertyChange(PropertyChangeEvent e) {
			listener.parameterChanged(subject, e.getNewValue());
		}

		public void parameterChanged(Object value) {
			listener.parameterChanged(subject, value);
		}
	}

	private class UpdaterAdapter implements PropertyChangeListener {
		private final PreviewUpdater previewUpdater;

		public UpdaterAdapter(PreviewUpdater previewUpdater) {
			this.previewUpdater = previewUpdater;
		}

		public void propertyChange(PropertyChangeEvent e) {
			previewUpdater.updatePreview(subject, e.getNewValue());
		}
	}
}

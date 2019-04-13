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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.altervista.mbilotta.julia.program.gui.EditorTransferHandler;
import org.altervista.mbilotta.julia.program.gui.ParameterChangeListener;
import org.altervista.mbilotta.julia.program.gui.PreviewUpdater;
import org.w3c.dom.Element;



final class BooleanParameter extends Parameter<Boolean> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static TransferHandler TRANSFER_HANDLER;

	private class Validator extends Parameter<Boolean>.Validator {

		public Validator(DescriptorParser descriptorParser,
				XmlPath parameterPath,
				Class<?> pluginType, Object pluginInstance) throws ValidationException {
			BooleanParameter.this.super(descriptorParser, parameterPath, pluginType, pluginInstance);
		}

		public Boolean validateHint(XmlPath hintPath, Element hint) {
			return Boolean.parseBoolean(DescriptorParser.getNodeValue(hint));
		}

		public String getXMLParameterType() {
			return "boolean";
		}

		public void writeConstraintsToHTML(HTMLWriter out) {
		}
	}

	public BooleanParameter(String id) {
		super(id, boolean.class);
	}

	Validator createValidator(DescriptorParser descriptorParser,
			XmlPath parameterPath,
			Class<?> pluginType, Object pluginInstance) throws ValidationException {
		return new Validator(descriptorParser, parameterPath, pluginType, pluginInstance);
	}
	
	public JComponent createEditor(Object initialValue) {
		JCheckBox checkBox = new JCheckBox();
		checkBox.setOpaque(false);
		checkBox.setBorder(BorderFactory.createEmptyBorder());
		checkBox.setSelected((Boolean) initialValue);

		if (TRANSFER_HANDLER == null) {
			TRANSFER_HANDLER = new EditorTransferHandler(this);
		}
		checkBox.setTransferHandler(TRANSFER_HANDLER);
		ActionMap actionMap = checkBox.getActionMap();
		actionMap.put("copy", TransferHandler.getCopyAction());
		actionMap.put("paste", TransferHandler.getPasteAction());

		return checkBox;
	}

	public void disposeEditor(JComponent editor) {
	}

	public Object getEditorValue(JComponent editor) {
		return ((JCheckBox) editor).isSelected();
	}

	public void setEditorValue(JComponent editor, Object value) {
		((JCheckBox) editor).setSelected((Boolean) value);
	}

	public void setPreviewUpdater(
			final PreviewUpdater previewUpdater, JComponent editor) {
		((JCheckBox) editor).addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				previewUpdater.updatePreview(BooleanParameter.this, e.getStateChange() == ItemEvent.SELECTED);
			}
		});
	}

	public void addParameterChangeListener(
			final ParameterChangeListener l, JComponent editor) {
		((JCheckBox) editor).addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				l.parameterChanged(BooleanParameter.this, e.getStateChange() == ItemEvent.SELECTED);
			}
		});
	}

	public boolean isEditorExpandable() {
		return false;
	}

	public boolean isEditorBaselineProvided() {
		return false;
	}

	public boolean isEditorConsistent(JComponent editor) {
		return true;
	}

	@Override
	public boolean acceptsValue(Object value) {
		return value instanceof Boolean;
	}

	private void writeObject(ObjectOutputStream out)
			throws IOException {
		out.defaultWriteObject();
		writeHints(out);
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		setType(boolean.class);
		readHints(in);
	}
}

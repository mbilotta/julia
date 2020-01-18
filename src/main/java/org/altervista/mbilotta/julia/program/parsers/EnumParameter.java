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
import static org.altervista.mbilotta.julia.program.parsers.Parser.println;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import javax.swing.ActionMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.TransferHandler;

import org.altervista.mbilotta.julia.program.gui.EditorTransferHandler;
import org.altervista.mbilotta.julia.program.gui.ParameterChangeListener;
import org.altervista.mbilotta.julia.program.gui.PreviewUpdater;
import org.w3c.dom.Element;


final class EnumParameter extends Parameter<Enum<?>> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	void initConstraints() {
	}

	private class Validator extends Parameter<Enum<?>>.Validator {

		public Validator(DescriptorParser descriptorParser,
				XmlPath parameterPath,
				Class<?> pluginType, Object pluginInstance) throws DomValidationException {
			EnumParameter.this.super(descriptorParser, parameterPath, pluginType, pluginInstance);
		}

		@Override
		Class<?> inspectType(XmlPath currentPath) throws DomValidationException {
			Class<?> rv = super.inspectType(currentPath);
			if (rv != null && !rv.isEnum()) {
				descriptorParser.fatalError(new DomValidationException(
						currentPath,
						position,
						"Type " + rv.getName() + " not an enum type"));
				rv = null;
			}
			return rv;
		}

		public Enum<?> validateHint(XmlPath hintPath, Element hint) throws DomValidationException {
			String valueString = DescriptorParser.getNodeValue(hint);
			Enum<?> value = null;
			try {
				if (getType() != null)
					value = Enum.valueOf((Class) getType(), valueString);
			} catch (IllegalArgumentException e) {
				descriptorParser.fatalError(DomValidationException.atEndOf(
						hintPath,
						"Invalid enum constant " + valueString +
						". Possible ones are: " + Arrays.toString(getType().getEnumConstants()) + "."));
			} finally {
				println(hintPath, value);
			}
			return value;
		}

		public String getXMLParameterType() {
			return "enum";
		}

		public void writeValueToHTML(HTMLWriter out, Object value) {
			out.openAndClose("td", ((Enum<?>) value).name(), false, "class", "value");
		}

		public void writeConstraintsToHTML(HTMLWriter out) {
		}
	}

	public EnumParameter(String id) {
		super(id);
	}

	Validator createValidator(DescriptorParser descriptorParser,
			XmlPath parameterPath,
			Class<?> pluginType, Object pluginInstance) throws DomValidationException {
		return new Validator(descriptorParser, parameterPath, pluginType, pluginInstance);
	}

	public JComponent createEditor(Object initialValue) {
		JComboBox<?> comboBox = new JComboBox<>(getType().getEnumConstants());
		comboBox.setSelectedItem(initialValue);
		
		TransferHandler transferHandler = new EditorTransferHandler(this);
		comboBox.setTransferHandler(transferHandler);
		ActionMap actionMap = comboBox.getActionMap();
		actionMap.put("copy", TransferHandler.getCopyAction());
		actionMap.put("paste", TransferHandler.getPasteAction());

		return comboBox;
	}

	@Override
	public JComponent cloneEditor(JComponent editor) {
		JComboBox<?> comboBox = new JComboBox<>(getType().getEnumConstants());
		comboBox.setSelectedItem(getEditorValue(editor));

		comboBox.setTransferHandler(editor.getTransferHandler());
		ActionMap actionMap = comboBox.getActionMap();
		actionMap.put("copy", TransferHandler.getCopyAction());
		actionMap.put("paste", TransferHandler.getPasteAction());
		
		return comboBox;
	}

	public void disposeEditor(JComponent editor) {
	}

	public Object getEditorValue(JComponent editor) {
		return ((JComboBox<?>) editor).getSelectedItem();
	}

	public void setEditorValue(JComponent editor, Object value) {
		((JComboBox<?>) editor).setSelectedItem(value);
	}

	public void setPreviewUpdater(
			final PreviewUpdater previewUpdater, JComponent editor) {
		((JComboBox<?>) editor).addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					previewUpdater.updatePreview(EnumParameter.this, e.getItem());
				}
			}
		});
	}

	public void addParameterChangeListener(
			final ParameterChangeListener l, JComponent editor) {
		((JComboBox<?>) editor).addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					l.parameterChanged(EnumParameter.this, e.getItem());
				}
			}
		});
	}

	public boolean isEditorExpandable() {
		return false;
	}

	public boolean isEditorConsistent(JComponent editor) {
		return true;
	}

	@Override
	public JMenuItem createHintMenuItem(int index) {
		int mnemonic = index % 10;
		JMenuItem rv = new JMenuItem("<html><u>" + mnemonic + "</u>&nbsp;&nbsp;&nbsp;" + getHint(index).name());
		rv.setMnemonic(mnemonic + KeyEvent.VK_0);
		return rv;
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
		if (!type.isEnum())
			throw newIOException('[' + getId() + ".type=" + type.getName() + "] is not an enum type.");

		setDescriptorType(type);
		readHints(in);
	}
}

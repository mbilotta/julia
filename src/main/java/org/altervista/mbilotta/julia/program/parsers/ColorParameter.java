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

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import org.altervista.mbilotta.julia.program.gui.ColorIcon;
import org.altervista.mbilotta.julia.program.gui.ColorParameterEditor;
import org.altervista.mbilotta.julia.program.gui.ParameterChangeListener;
import org.altervista.mbilotta.julia.program.gui.PreviewUpdater;

import org.w3c.dom.Element;


final class ColorParameter extends Parameter<Color> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	void initConstraints() {
	}

	private class Validator extends Parameter<Color>.Validator {

		public Validator(DescriptorParser descriptorParser,
				XmlPath parameterPath,
				Class<?> pluginType, Object pluginInstance) throws DomValidationException {
			ColorParameter.this.super(descriptorParser, parameterPath, pluginType, pluginInstance);
			getterHint = descriptorParser.replace(getterHint);
		}

		public Color validateHint(XmlPath hintPath, Element hint) {
			Color rv = descriptorParser.parseColor(hint);
			println(hintPath, rv);
			return rv;
		}

		public String getXMLParameterType() {
			return "color";
		}

		public void writeValueToHTML(HTMLWriter out, Object value) {
			Color c = (Color) value;
			out.open("td");
			String title = String.format("ARGB: %d, %d, %d, %d", c.getAlpha(), c.getRed(), c.getGreen(), c.getBlue());
			out.open("div", "class", "chessboard-background", "title", title);
			out.open("svg", "xmlns", "http://www.w3.org/2000/svg", "width", "100%", "height", "100%", "viewBox", "0 0 1 1", "preserveAspectRatio", "none");
			String style;
			if (c.getAlpha() != 0xff) {
				style = String.format("fill:rgb(%d, %d, %d); fill-opacity:%s", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 255f);
			} else {
				style = String.format("fill:rgb(%d, %d, %d)", c.getRed(), c.getGreen(), c.getBlue());
			}
			out.openAndClose("rect", "x", "0", "y", "0", "width", "1", "height", "1", "style", style);
			out.close();
			out.close();
			out.close();
		}

		public void writeConstraintsToHTML(HTMLWriter out) {
		}
	}
	
	public ColorParameter(String id) {
		super(id, Color.class);
	}

	Validator createValidator(DescriptorParser descriptorParser,
			XmlPath parameterPath,
			Class<?> pluginType, Object pluginInstance) throws DomValidationException {
		return new Validator(descriptorParser, parameterPath, pluginType, pluginInstance);
	}

	@Override
	public Color parseValue(String s) {
		String[] components = s.split(",");
		int r = Integer.parseInt(components[0]);
		int g = Integer.parseInt(components[1]);
		int b = Integer.parseInt(components[2]);
		int a = components.length > 3 ? Integer.parseInt(components[3]) : 255;
		return new Color(r, g, b, a);
	}

	public JComponent createEditor(Object initialValue) {
		return new ColorParameterEditor(this, (Color) initialValue, false);
	}

	public void disposeEditor(JComponent editor) {
		((ColorParameterEditor) editor).dispose();
	}

	public Object getEditorValue(JComponent editor) {
		return ((ColorParameterEditor) editor).getValue();
	}

	public void setEditorValue(JComponent editor, Object value) {
		((ColorParameterEditor) editor).setValue((Color) value);
	}

	public void setPreviewUpdater(
			final PreviewUpdater previewUpdater, final JComponent editor) {
		((ColorParameterEditor) editor).setPreviewUpdater(previewUpdater);
	}

	public void addParameterChangeListener(ParameterChangeListener listener, JComponent editor) {
		((ColorParameterEditor) editor).addParameterChangeListener(listener);
	}

	public boolean isEditorExpandable() {
		return false;
	}

	@Override
	public boolean isEditorBaselineProvided() {
		return false;
	}

	public boolean isEditorConsistent(JComponent editor) {
		return true;
	}

	@Override
	public JMenuItem createHintMenuItem(int index) {
		int mnemonic = index % 10;
		JMenuItem rv = new JMenuItem("<html><u>" + mnemonic, new ColorIcon(100, 18, getHint(index)));
		rv.setMnemonic(mnemonic + KeyEvent.VK_0);
		return rv;
	}

	private void writeObject(ObjectOutputStream out)
			throws IOException {
		out.defaultWriteObject();
		writeHints(out);
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		setType(Color.class);
		readHints(in);
	}
}

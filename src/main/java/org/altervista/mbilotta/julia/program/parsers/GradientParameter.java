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
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import org.altervista.mbilotta.julia.Gradient;
import org.altervista.mbilotta.julia.GradientBuilder;
import org.altervista.mbilotta.julia.Gradient.Stop;
import org.altervista.mbilotta.julia.program.gui.GradientIcon;
import org.altervista.mbilotta.julia.program.gui.GradientParameterEditor;
import org.altervista.mbilotta.julia.program.gui.ParameterChangeListener;
import org.altervista.mbilotta.julia.program.gui.PreviewUpdater;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


final class GradientParameter extends Parameter<Gradient> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	void initConstraints() {
	}

	private class Validator extends Parameter<Gradient>.Validator {

		public Validator(DescriptorParser descriptorParser,
				XmlPath parameterPath,
				Class<?> pluginType, Object pluginInstance) throws DomValidationException {
			GradientParameter.this.super(descriptorParser, parameterPath, pluginType, pluginInstance);
			getterHint = descriptorParser.replace(getterHint);
		}

		public Gradient validateHint(XmlPath hintPath, Element hint) throws DomValidationException {
			boolean errorsEncountered = false;
			NodeList children = hint.getChildNodes();
			int numOfStops = children.getLength();
			Gradient.Stop[] stops = new Gradient.Stop[numOfStops];
			Gradient.Stop previous = null;
			for (int i = 0; i < numOfStops; i++) {
				Element child = (Element) children.item(i);
				XmlPath stopPath = parameterPath.getChild(child, i + 1);
				Color stopColor = descriptorParser.parseColor(child);
				Stop stop = new Stop(Float.parseFloat(child.getAttribute("location")), stopColor);
				println(stopPath, stop.getColor());
				println(stopPath.getAttributeChild("location"), stop.getLocation());
				if (previous != null) {
					if (stop.getLocation() <= previous.getLocation()) {
						descriptorParser.error(DomValidationException.atStartOf(
								stopPath,
								"Location values must appear in (strictly) increasing order: " +
								stop.getLocation() + " <= " + previous.getLocation() + "."));
						errorsEncountered = true;
					}
				}

				previous = stop;
				stops[i] = stop;
			}

			Gradient rv = errorsEncountered ? null : descriptorParser.replace(new Gradient(stops));
			println(hintPath, rv);
			return rv;
		}

		private Map<Gradient, String> gradientToId = new HashMap<Gradient, String>();
		private int k = 1;

		public String getXMLParameterType() {
			return "gradient";
		}

		public void writeValueToHTML(HTMLWriter out, Object value) {
			Gradient g = (Gradient) value;
			out.open("td", "style", "min-width:100px");
			out.open("div", "class", "chessboard-background");
			out.open("svg", "xmlns", "http://www.w3.org/2000/svg", "width", "100%", "height", "100%", "viewBox", "0 0 1 1", "preserveAspectRatio", "none");
			String id = gradientToId.get(g);
			if (id == null) {
				id = getParameter().getId() + '-' + k;
				gradientToId.put(g, id);
				k++;
				out.open("linearGradient", "id", id, "x1", "0%", "y1", "0%", "x2", "100%", "y2", "0%");
				int stopsCount = g.getNumOfStops();
				for (int i = 0; i < stopsCount; i++) {
					Gradient.Stop s = g.getStop(i);
					Color c = s.getColor();
					String style;
					if (c.getAlpha() != 0xff) {
						style = String.format("stop-color:rgb(%d, %d, %d); stop-opacity:%s", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 255f);
					} else {
						style = String.format("stop-color:rgb(%d, %d, %d)", c.getRed(), c.getGreen(), c.getBlue());
					}
					out.openAndClose("stop", "offset", Float.toString(s.getLocation()), "style", style);
				}
				out.close();
			}
			String style = String.format("fill:url(#%s)", id);
			out.openAndClose("rect", "x", "0", "y", "0", "width", "1", "height", "1", "style", style);
			out.close();
			out.close();
			out.close();
		}

		public void writeConstraintsToHTML(HTMLWriter out) {
		}
	}

	public GradientParameter(String id) {
		super(id, Gradient.class);
	}
	
	Validator createValidator(DescriptorParser descriptorParser,
			XmlPath parameterPath,
			Class<?> pluginType, Object pluginInstance) throws DomValidationException {
		return new Validator(descriptorParser, parameterPath, pluginType, pluginInstance);
	}

	@Override
	public Gradient parseValue(String s) {
		GradientBuilder builder = new GradientBuilder();
		String[] stops = s.split("\\^");
		for (String stop : stops) {
			String[] components = stop.split("@");
			String[] color = components[0].split(",");
			int r = Integer.parseInt(color[0]);
			int g = Integer.parseInt(color[1]);
			int b = Integer.parseInt(color[2]);
			int a = color.length > 3 ? Integer.parseInt(color[3]) : 255;
			float location = Float.parseFloat(components[1]);
			builder.withStop(r, g, b, a, location);
		}
		return builder.build();
	}

	public JComponent createEditor(Object initialValue) {
		return new GradientParameterEditor(this, (Gradient) initialValue);
	}

	public void disposeEditor(JComponent editor) {
		((GradientParameterEditor) editor).dispose();
	}

	@Override
	public JComponent cloneEditor(JComponent editor) {
		GradientParameterEditor src = (GradientParameterEditor) editor;
		return new GradientParameterEditor(this, src.getGradients(), src.getValue());
	}

	public Object getEditorValue(JComponent editor) {
		return ((GradientParameterEditor) editor).getValue();
	}

	public void setEditorValue(JComponent editor, Object value) {
		((GradientParameterEditor) editor).setValue((Gradient) value);
	}

	public void setPreviewUpdater(
			PreviewUpdater previewUpdater, JComponent editor) {
		((GradientParameterEditor) editor).setPreviewUpdater(previewUpdater);
	}

	public void addParameterChangeListener(ParameterChangeListener listener, JComponent editor) {
		((GradientParameterEditor) editor).addParameterChangeListener(listener);
	}

	public boolean isEditorExpandable() {
		return true;
	}

	public boolean isEditorConsistent(JComponent editor) {
		return true;
	}

	@Override
	public JMenuItem createHintMenuItem(int index) {
		int mnemonic = index % 10;
		JMenuItem rv = new JMenuItem("<html><u>" + mnemonic, new GradientIcon(100, 18, getHint(index)));
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

		setType(Gradient.class);
		readHints(in);
	}
}

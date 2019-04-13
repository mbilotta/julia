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

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.altervista.mbilotta.julia.program.parsers.FormulaPlugin;
import org.altervista.mbilotta.julia.program.parsers.NumberFactoryPlugin;
import org.altervista.mbilotta.julia.program.parsers.Plugin;
import org.altervista.mbilotta.julia.program.parsers.RepresentationPlugin;


public class PluginSelectionPane extends JOptionPane {

	public static class Entry<P extends Plugin> {

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private static final Entry ASK = new Entry(null) {
			public String toString() {
				return "Ask";
			}
		};
		
		@SuppressWarnings("unchecked")
		public static <P extends Plugin> Entry<P> getAskElement() {
			return (Entry<P>) ASK;
		}

		private final P plugin;

		public Entry(P plugin) {
			this.plugin = plugin;
		}

		public P getPlugin() {
			return plugin;
		}

		public String toString() {
			return plugin.getName() + " (" + plugin.getId() + ")";
		}
	}

	public static final class ComboBoxModel<P extends Plugin>
			extends AbstractListModel<Entry<P>>
			implements javax.swing.ComboBoxModel<Entry<P>> {

		private Object selectedElement;
		private final List<Entry<P>> elements;

		public ComboBoxModel(List<P> plugins) {
			this(plugins, plugins.get(0));
		}

		public ComboBoxModel(List<P> plugins, P selectedPlugin) {
			elements = new ArrayList<>(plugins.size());
			for (P plugin : plugins) {
				assert plugin != null;
				Entry<P> element = new Entry<P>(plugin);
				elements.add(element);
				if (selectedElement == null && plugin == selectedPlugin)  {
					selectedElement = element;
				}
			}
			
			if (selectedElement == null && elements.size() > 0) {
				selectedElement = elements.get(0);
			}
		}

		public ComboBoxModel(List<P> plugins, String selectedPlugin) {
			elements = new ArrayList<>(plugins.size() + 1);
			for (P plugin : plugins) {
				assert plugin != null;
				Entry<P> element = new Entry<P>(plugin);
				elements.add(element);
				if (selectedElement == null && plugin.getId().equals(selectedPlugin))  {
					selectedElement = element;
				}
			}

			Entry<P> ask = Entry.getAskElement();
			elements.add(ask);
			if (selectedElement == null) {
				selectedElement = ask;
			}
		}

		public int getSize() {
			return elements.size();
		}

		public Entry<P> getElementAt(int index) {
			return elements.get(index);
		}

		public void setSelectedItem(Object plugin) {
			if (selectedElement != plugin) {
				selectedElement = plugin;
				fireContentsChanged(this, -1, -1);
			}
		}

		@SuppressWarnings("unchecked")
		public void setSelectedPlugin(String plugin) {
			if (plugin == null) {
				if (selectedElement != Entry.ASK) {
					selectedElement = Entry.ASK;
					fireContentsChanged(this, -1, -1);
				}
			} else if (selectedElement == Entry.ASK ||
					!plugin.equals(((Entry<P>) selectedElement).getPlugin().getId())) {

				for (Entry<P> element : elements) {
					if (element != Entry.ASK && element.getPlugin().getId().equals(plugin)) {
						selectedElement = element;
						fireContentsChanged(this, -1, -1);
						return;
					}
				}

				if (selectedElement != Entry.ASK) {
					selectedElement = Entry.getAskElement();
					fireContentsChanged(this, -1, -1);
				}
			}
		}

		@SuppressWarnings("unchecked")
		public void setSelectedPlugin(P plugin) {
			if (selectedElement == null || plugin != ((Entry<P>) selectedElement).getPlugin()) {
				for (Entry<P> element : elements) {
					if (plugin == element.getPlugin()) {
						selectedElement = element;
						fireContentsChanged(this, -1, -1);
						return;
					}
				}
			}
		}

		@SuppressWarnings("unchecked")
		public String getSelectedPlugin() {
			if (selectedElement == null || selectedElement == Entry.ASK) {
				return null;
			}
			
			return ((Entry<P>) selectedElement).getPlugin().getId();
		}

		@Override
		public Object getSelectedItem() {
			return selectedElement;
		}
	}

	private JComboBox<Entry<NumberFactoryPlugin>> numberFactoryComboBox;
	private JComboBox<Entry<FormulaPlugin>> formulaComboBox;
	private JComboBox<Entry<RepresentationPlugin>> representationComboBox;
	private JCheckBox juliaSetCheckBox;
	private JCheckBox forceEqualScalesCheckBox;
	private JCheckBox rememberTheseChoicesCheckBox;

	public PluginSelectionPane(
			List<NumberFactoryPlugin> numberFactories,
			List<FormulaPlugin> formulas,
			List<RepresentationPlugin> representations) {
		numberFactoryComboBox = new JComboBox<>(new ComboBoxModel<>(numberFactories));
		formulaComboBox = new JComboBox<>(new ComboBoxModel<>(formulas));
		representationComboBox = new JComboBox<>(new ComboBoxModel<>(representations));
		forceEqualScalesCheckBox = new JCheckBox("Force equal scales", false);
		juliaSetCheckBox = new JCheckBox("Julia set", false);
		rememberTheseChoicesCheckBox = new JCheckBox("Remember these choices", false);

		JLabel numberFactoryComboBoxLabel = new JLabel("Choose a number factory:");
		numberFactoryComboBoxLabel.setLabelFor(numberFactoryComboBox);
		numberFactoryComboBoxLabel.setDisplayedMnemonic(KeyEvent.VK_N);

		JLabel formulaComboBoxLabel = new JLabel("Choose a formula:");
		formulaComboBoxLabel.setLabelFor(formulaComboBox);
		formulaComboBoxLabel.setDisplayedMnemonic(KeyEvent.VK_F);

		JLabel representationComboBoxLabel = new JLabel("Choose a representation:");
		representationComboBoxLabel.setLabelFor(representationComboBox);
		representationComboBoxLabel.setDisplayedMnemonic(KeyEvent.VK_R);

		forceEqualScalesCheckBox.setMnemonic(KeyEvent.VK_O);
		juliaSetCheckBox.setMnemonic(KeyEvent.VK_J);

		rememberTheseChoicesCheckBox.setMnemonic(KeyEvent.VK_E);
		rememberTheseChoicesCheckBox.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(10, 0, 0, 0),
				rememberTheseChoicesCheckBox.getBorder()));

		setMessage(new Object[] {
				numberFactoryComboBoxLabel, numberFactoryComboBox,
				formulaComboBoxLabel, formulaComboBox,
				representationComboBoxLabel, representationComboBox,
				forceEqualScalesCheckBox,
				juliaSetCheckBox,
				rememberTheseChoicesCheckBox });
		setMessageType(PLAIN_MESSAGE);
		setOptionType(OK_CANCEL_OPTION);
	}

	@SuppressWarnings("unchecked")
	public NumberFactoryPlugin getSelectedNumberFactory() {
		return ((Entry<NumberFactoryPlugin>) numberFactoryComboBox.getSelectedItem()).getPlugin();
	}

	@SuppressWarnings("unchecked")
	public FormulaPlugin getSelectedFormula() {
		return ((Entry<FormulaPlugin>) formulaComboBox.getSelectedItem()).getPlugin();
	}

	@SuppressWarnings("unchecked")
	public RepresentationPlugin getSelectedRepresentation() {
		return ((Entry<RepresentationPlugin>) representationComboBox.getSelectedItem()).getPlugin();
	}

	public void setSelectedNumberFactory(NumberFactoryPlugin numberFactory) {
		((ComboBoxModel<NumberFactoryPlugin>) numberFactoryComboBox.getModel()).setSelectedPlugin(numberFactory);
	}

	public void setSelectedFormula(FormulaPlugin formula) {
		((ComboBoxModel<FormulaPlugin>) formulaComboBox.getModel()).setSelectedPlugin(formula);
	}

	public void setSelectedRepresentation(RepresentationPlugin representation) {
		((ComboBoxModel<RepresentationPlugin>) representationComboBox.getModel()).setSelectedPlugin(representation);
	}

	public boolean isForceEqualScalesChecked() {
		return forceEqualScalesCheckBox.isSelected();
	}

	public void setForceEqualScalesChecked(boolean checked) {
		forceEqualScalesCheckBox.setSelected(checked);
	}

	public boolean isJuliaSetChecked() {
		return juliaSetCheckBox.isSelected();
	}

	public void setJuliaSetChecked(boolean checked) {
		juliaSetCheckBox.setSelected(checked);
	}

	public boolean isRememberTheseChoicesSelected() {
		return rememberTheseChoicesCheckBox.isSelected();
	}

	public static <P extends Plugin> P showSelectionPane(Component parentComponent, String title, P currentChoice, List<P> choices) {
		String pluginFamily;
		switch (choices.get(0).getFamily()) {
		case numberFactory:  pluginFamily = "number factory"; break;
		case formula:		 pluginFamily = "formula"; break;
		case representation: pluginFamily = "representation"; break;
		default: throw new AssertionError();
		}

		JLabel label = new JLabel("Choose a " + pluginFamily + ':');
		JComboBox<Entry<P>> comboBox = new JComboBox<Entry<P>>(new ComboBoxModel<>(choices, currentChoice));
		label.setLabelFor(comboBox);
		label.setDisplayedMnemonic(pluginFamily.charAt(0));
		JOptionPane optionPane = new JOptionPane(
				new Object[] {label, comboBox},
				PLAIN_MESSAGE,
				OK_CANCEL_OPTION);
		JDialog dialog = optionPane.createDialog(parentComponent, title);
		dialog.setVisible(true);
		dialog.dispose();
		Integer value = (Integer) optionPane.getValue();
		if (value != null && value.intValue() == JOptionPane.OK_OPTION) {
			return ((Entry<P>) comboBox.getSelectedItem()).getPlugin();
		}
		return null;
	}
}

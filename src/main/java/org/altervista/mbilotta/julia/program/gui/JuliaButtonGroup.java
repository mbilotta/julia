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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.event.SwingPropertyChangeSupport;


public class JuliaButtonGroup {

	private List<AbstractButton> buttonList = new LinkedList<>();
	private AbstractButton selectedButton;
	private final boolean inverted;
	private final ButtonGroupListener LISTENER = new ButtonGroupListener();

	private final SwingPropertyChangeSupport propertyChangeSupport =
			new SwingPropertyChangeSupport(this, true);

	private class ButtonGroupListener implements ItemListener {
		@Override
		public void itemStateChanged(ItemEvent e) {
			setSelected((AbstractButton) e.getSource(),
					inverted ? e.getStateChange() == ItemEvent.DESELECTED : e.getStateChange() == ItemEvent.SELECTED);
		}

		public JuliaButtonGroup getGroup() {
			return JuliaButtonGroup.this;
		}
	}

	public static JuliaButtonGroup getGroup(AbstractButton button) {
		ItemListener[] itemListeners = button.getItemListeners();
		for (int i = 0; i < itemListeners.length; i++) {
			if (itemListeners[i] instanceof ButtonGroupListener) {
				return ((ButtonGroupListener) itemListeners[i]).getGroup();
			}
		}

		return null;
	}

	public JuliaButtonGroup() {
		this(false);
	}

	public JuliaButtonGroup(boolean inverted) {
		this.inverted = inverted;
	}

	public void add(AbstractButton button) {
		JuliaButtonGroup buttonGroup = getGroup(button);
		if (buttonGroup != null) {
			if (buttonGroup == this) return;
			buttonGroup.remove(button);
		}
		buttonList.add(button);
		if (isSelectedImpl(button)) {
			if (selectedButton == null) {
				selectedButton = button;
			} else {
				setSelectedImpl(button, false);
			}
		}
		button.addItemListener(LISTENER);
	}

	public void remove(AbstractButton button) {
		if (buttonList.remove(button)) {
			if (button == selectedButton) {
				selectedButton = null;
				firePropertyChanged("selectedButton", button, null);
			}
			button.removeItemListener(LISTENER);
		}
	}

	public void clearSelection() {
		if (selectedButton != null)
			setSelectedImpl(selectedButton, false);
	}

	public AbstractButton getSelectedButton() {
		return selectedButton;
	}

	public void setSelected(AbstractButton button, boolean flag) {
		if (flag) {
			if (button != selectedButton) {
				AbstractButton oldSelectedButton = selectedButton;
				selectedButton = button;
				if (oldSelectedButton != null) {
					setSelectedImpl(oldSelectedButton, false);
				}
				setSelectedImpl(button, true);
				firePropertyChanged("selectedButton", oldSelectedButton, selectedButton);
			}
		} else if (button == selectedButton) {
			selectedButton = null;
			setSelectedImpl(button, false);
			firePropertyChanged("selectedButton", button, null);
		}
	}

	public boolean isSelected(AbstractButton button) {
		return button == selectedButton;
	}

	public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	public PropertyChangeListener[] getPropertyChangeListeners() {
		return propertyChangeSupport.getPropertyChangeListeners();
	}

	protected void firePropertyChanged(String propertyName, Object oldValue,
			Object newValue) {
		propertyChangeSupport.firePropertyChange(new PropertyChangeEvent(
				this, propertyName, oldValue, newValue));
	}

	private boolean isSelectedImpl(AbstractButton button) {
		return inverted ? !button.isSelected() : button.isSelected(); 
	}

	private void setSelectedImpl(AbstractButton button, boolean flag) {
		button.setSelected(inverted ? !flag : flag); 
	}
}

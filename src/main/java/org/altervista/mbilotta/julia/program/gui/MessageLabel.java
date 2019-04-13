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

import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.ToolTipManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;


public class MessageLabel extends JLabel implements PropertyChangeListener, ComponentListener {
	
	private int preferredWidth;
	private boolean registered = false;

	public MessageLabel(String text) {
		super(text);
		Border border = BorderFactory.createCompoundBorder(
				BorderFactory.createBevelBorder(BevelBorder.LOWERED),
				getBorder());
		setBorder(border);
		preferredWidth = ui.getPreferredSize(this).width;
		addPropertyChangeListener(this);
		addComponentListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource() == this && e.getPropertyName().equals("text")) {
			preferredWidth = ui.getPreferredSize(this).width;
			toggleToolTip();
		}
	}

	@Override
	public void componentResized(ComponentEvent e) {
		if (e.getSource() == this) {
			toggleToolTip();
		}
	}

	@Override
	public Point getToolTipLocation(MouseEvent event) {
		Insets insets = getInsets();
		return new Point(insets.left, insets.top);
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		return getText();
	}

	private void toggleToolTip() {
		ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
		if (getWidth() < preferredWidth) {
			if (!registered) {
				toolTipManager.registerComponent(this);
				registered = true;
			}
		} else if (registered) {
			toolTipManager.unregisterComponent(this);
			registered = false;
		}
	}

	@Override
	public void componentMoved(ComponentEvent e) {}
	@Override
	public void componentShown(ComponentEvent e) {}
	@Override
	public void componentHidden(ComponentEvent e) {}
}

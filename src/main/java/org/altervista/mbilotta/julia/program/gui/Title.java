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

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSeparator;


public class Title extends Box {

	public Title(String text) {
		super(BoxLayout.X_AXIS);
		JLabel label = new JLabel(text);
		label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		JSeparator leftSeparator = new JSeparator(JSeparator.HORIZONTAL);
		JSeparator rightSeparator = new JSeparator(JSeparator.HORIZONTAL);
		Dimension preferredSize = leftSeparator.getPreferredSize();
		Dimension minimumSize = new Dimension(0, preferredSize.height);
		Dimension maximumSize = new Dimension(Short.MAX_VALUE, preferredSize.height);
		rightSeparator.setMinimumSize(minimumSize);
		rightSeparator.setMaximumSize(maximumSize);
		leftSeparator.setMinimumSize(minimumSize);
		leftSeparator.setMaximumSize(maximumSize);

		add(leftSeparator);
		add(label);
		add(rightSeparator);
	}
}

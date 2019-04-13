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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.border.Border;

import org.altervista.mbilotta.julia.Gradient;


class GradientListCellRenderer extends DefaultListCellRenderer {
	
	private Object value;

	static final int GAP = 4;
	static final int GRADIENT_RECTANGLE_HEIGHT = 30;

	static final int PREFERRED_HEIGHT =
			GRADIENT_RECTANGLE_HEIGHT +
			2 * GAP;
	
	static final ChessboardPainter chessboardPainter = new ChessboardPainter(4);

	static final GradientListCellRenderer INSTANCE = new GradientListCellRenderer();

	public GradientListCellRenderer() {
		setPreferredSize(new Dimension(0, PREFERRED_HEIGHT));
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends Object> list,
			Object value, int index, boolean isSelected, boolean cellHasFocus) {
		setValue(value);

		super.getListCellRendererComponent(list, value, index, isSelected,
				cellHasFocus);
		
		if (value instanceof String) {
			Border currentBorder = getBorder();
			int indentationWidth = currentBorder != null ?
					GAP - currentBorder.getBorderInsets(this).left :
					GAP;
			Border indentation = BorderFactory.createEmptyBorder(0, indentationWidth, 0, 0);
			setBorder(BorderFactory.createCompoundBorder(currentBorder, indentation));
		}
		
		return this;
	}

	public void paintComponent(Graphics g) {
		if (value instanceof Gradient) {
			Gradient gradient = (Gradient) value;

			if (isOpaque()) {
				g.setColor(getBackground());
				g.fillRect(0, 0, getWidth(), getHeight());
			}
			
			int x = GAP;
			int y = GAP;
			int width = getWidth() - 2 * GAP;
			int height = GRADIENT_RECTANGLE_HEIGHT;
			g.setColor(Color.black);
			g.drawRect(x, y, width - 1, height - 1);

			x++; y++;
			width -= 2;
			height -= 2;
			Graphics2D g2D = (Graphics2D) g;
			if (gradient.getTransparency() != Transparency.OPAQUE) {
				chessboardPainter.paintChessboard(g2D, this, new Rectangle(x, y, width, height), new Point(x, y));
			}

			Paint gradientPaint = gradient.createPaint(width);
			Paint oldPaint = g2D.getPaint();
			g2D.setPaint(gradientPaint);
			g2D.fillRect(x, y, width, height);
			g2D.setPaint(oldPaint);
		} else {
			super.paintComponent(g);
		}
	}

	public void setValue(Object value) {
		this.value = value;
	}
}

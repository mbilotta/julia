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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;

import javax.swing.Icon;

import org.altervista.mbilotta.julia.Gradient;



public class GradientIcon implements Icon {

	private int width;
	private int height;
	private Gradient gradient;

	public GradientIcon(int width, int height, Gradient gradient) {
		this.width = width;
		this.height = height;
		this.gradient = gradient;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		g.setColor(Color.black);
		g.drawRect(x, y, width - 1, height - 1);

		x++; y++;
		int width = this.width - 2;
		int height = this.height - 2;
		Graphics2D g2D = (Graphics2D) g;
		if (gradient.getTransparency() != Transparency.OPAQUE) {
			GradientListCellRenderer.chessboardPainter
				.paintChessboard(g2D, null, new Rectangle(x, y, width, height), new Point(x, y));
		}

		Paint gradientPaint = gradient.createPaint(width);
		Paint oldPaint = g2D.getPaint();
		g2D.setPaint(gradientPaint);
		g2D.fillRect(x, y, width, height);
		g2D.setPaint(oldPaint);
	}

	public int getIconWidth() {
		return width;
	}

	public int getIconHeight() {
		return height;
	}
}

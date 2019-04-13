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
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.Icon;



public class ColorIcon implements Icon {
	
	private int width;
	private int height;
	private Color color;

	public ColorIcon(int width, int height, Color color) {
		this.width = width;
		this.height = height;
		this.color = color;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		x++; y++;
		int w = width - 2;
		int h = height - 2;
		if (color.getAlpha() != 255) {
			Graphics2D g2D = (Graphics2D) g;

			GradientListCellRenderer.chessboardPainter
				.paintChessboard(g2D, null, new Rectangle(x, y, w, h), new Point(x, y));
			Color opaqueColor = new Color(color.getRGB(), false);
			LinearGradientPaint previewPaint = new LinearGradientPaint(
					new Point(x, 0),
					new Point(x + w - 1, 0),
					new float[] { 0.45f, 0.55f },
					new Color[] {opaqueColor, color});
			Paint oldPaint = g2D.getPaint();
			g2D.setPaint(previewPaint);
			g2D.fillRect(x, y, w, h);
			g2D.setPaint(oldPaint);
		} else {
			g.setColor(color);
			g.fillRect(x, y, w, h);
		}

		g.setColor(Color.black);
		g.drawRect(x - 1, y - 1, w + 1, h + 1);
	}

	@Override
	public int getIconWidth() {
		return width;
	}

	@Override
	public int getIconHeight() {
		return height;
	}

	public Color getColor() {
		return color;
	}
}
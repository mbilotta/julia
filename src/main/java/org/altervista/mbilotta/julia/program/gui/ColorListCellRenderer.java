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
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;




class ColorListCellRenderer extends DefaultListCellRenderer {

	private Color color;
	private int height;
	private int gap;
	private ChessboardPainter chessboardPainter;

	public ColorListCellRenderer(int height, int gap, ChessboardPainter chessboardPainter) {
		this.height = height;
		this.gap = gap;
		this.chessboardPainter = chessboardPainter;
	}

	public Dimension getPreferredSize() {
		Insets i = getInsets();
		return new Dimension(0, height + i.top + i.bottom);
	}

	public Insets getInsets() {
		int gap = this.gap;

		Insets i = super.getInsets();
		i.top = Math.max(i.top, gap);
		i.bottom = Math.max(i.bottom, gap);
		i.left = Math.max(i.left, gap);
		i.right = Math.max(i.right, gap);
		return i;
	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		setColor((Color) value);

		return super.getListCellRendererComponent(list, value, index, isSelected,
				cellHasFocus);
	}

	public void paintComponent(Graphics g) {
		if (isOpaque()) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}

		Insets i = getInsets();
		int x = i.left;
		int y = i.top;
		int w = getWidth() - i.left - i.right;
		int h = getHeight() - i.top - i.bottom;

		if (color.getAlpha() != 255) {
			Graphics2D g2D = (Graphics2D) g;

			chessboardPainter.paintChessboard(g2D, this, new Rectangle(x, y, w, h));
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

	protected void setColor(Color c) {
		color = c;
	}
}

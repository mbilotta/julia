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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Julia. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.altervista.mbilotta.julia.program.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;


public final class ChessboardPainter {

	public static final Color SQUARE_COLOR = Color.lightGray;

	private final int squareSize;
	private BufferedImage texture;

	public ChessboardPainter(int squareSize) {
		this.squareSize = squareSize;
	}

	public void paintChessboard(Graphics2D g2D, JComponent component) {
		paintChessboard(g2D, component, null);
	}

	public void paintChessboard(Graphics2D g2D, JComponent component, Rectangle area) {
		paintChessboard(g2D, component, area, null);
	}

	public void paintChessboard(Graphics2D g2D, JComponent component, Rectangle area, Point anchor) {
		if (texture == null)
			texture = createChessboardTextureFor(component, squareSize, SQUARE_COLOR);

		paintTexturedBackground(g2D, component, area, texture, anchor);
	}

	public int getSquareSize() {
		return squareSize;
	}

	public static void paintTexturedBackground(Graphics2D g2D, JComponent component, Rectangle area, BufferedImage texture) {
		paintTexturedBackground(g2D, component, area, texture, null);
	}

	public static void paintTexturedBackground(Graphics2D g2D, JComponent component, Rectangle area, BufferedImage texture, Point anchor) {
		if (area == null)
			area = new Rectangle(
					0,
					0,
					component.getWidth(),
					component.getHeight());

		if (anchor == null) {
			Insets i = component.getInsets();
			anchor = new Point(i.left, i.top);
		}

		Rectangle anchorRectangle = new Rectangle(
				anchor.x,
				anchor.y,
				texture.getWidth(), texture.getHeight());

		TexturePaint texturePaint = new TexturePaint(texture, anchorRectangle);

		Paint oldPaint = g2D.getPaint();
		g2D.setPaint(texturePaint);
		g2D.fillRect(area.x, area.y, area.width, area.height);
		g2D.setPaint(oldPaint);
	}

	public static BufferedImage createChessboardTextureFor(JComponent component, int squareSize, Color squareColor) {
		int textureSize = 2 * squareSize;
		BufferedImage texture = component.getGraphicsConfiguration().createCompatibleImage(textureSize, textureSize);
		Graphics g = texture.getGraphics();
		g.setColor(squareColor);
		g.fillRect(0, 0, squareSize, squareSize);
		g.fillRect(squareSize, squareSize, squareSize, squareSize);
		g.setColor(Color.white);
		g.fillRect(squareSize, 0, squareSize, squareSize);
		g.fillRect(0, squareSize, squareSize, squareSize);
		
		g.dispose();

		return texture;
	}
}

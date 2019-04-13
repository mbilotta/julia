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

import static org.altervista.mbilotta.julia.Utilities.subtract;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.altervista.mbilotta.julia.Consumer;
import org.altervista.mbilotta.julia.program.Application;


public class ImagePanel extends JPanel {
	
	private static final int SELECTION_INITIAL_WIDTH = 199;
	private static final int SELECTION_THICKNESS = 2;
	private static final int SELECTION_WHEEL_AMOUNT = 20;
	
	private double heightOverWidth;
	private int selectionCenterX;
	private int selectionCenterY;
	private int selectionWidth;
	private Rectangle selection;
	private Color selectionColor;
	private Color selectionColorPreview;
	private BufferedImage fimg;
	private Consumer consumer;
	static final ChessboardPainter chessboardPainter = new ChessboardPainter(10);

	private final Application application;

	public ImagePanel(int imgWidth, int imgHeight, Color selectionColor, Application application) {
		setOpaque(true);
		setBorder(BorderFactory.createEmptyBorder());
		this.heightOverWidth = (double)imgHeight / imgWidth;
		this.selectionColor = selectionColor;
		this.application = application;
		setPreferredSize(new Dimension(imgWidth, imgHeight));

		MouseEventHandler handler = new MouseEventHandler();
		addMouseListener(handler);
		addMouseWheelListener(handler);
	}

	public ImagePanel(BufferedImage fimg, Color selectionColor, Consumer consumer) {
		setOpaque(true);
		setBorder(BorderFactory.createEmptyBorder());
		this.heightOverWidth = (double)fimg.getHeight() / fimg.getWidth();
		this.fimg = fimg;
		this.selectionColor = selectionColor;
		this.consumer = consumer;
		this.application = null;
		setPreferredSize(new Dimension(fimg.getWidth(), fimg.getHeight()));

		MouseEventHandler handler = new MouseEventHandler();
		addMouseListener(handler);
		addMouseWheelListener(handler);
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2D = (Graphics2D) g;
		Rectangle clip = g.getClipBounds();
		if (fimg == null) {
			chessboardPainter.paintChessboard(g2D, this, clip);
		} else {
			boolean paintBackground = false;
			switch (fimg.getTransparency()) {
			case Transparency.OPAQUE:
				paintBackground = !new Rectangle(0, 0, fimg.getWidth(), fimg.getHeight()).contains(clip); break;
			case Transparency.BITMASK:
			case Transparency.TRANSLUCENT:
				paintBackground = consumer.getTransparency() != Transparency.OPAQUE ||
						!subtract(clip, consumer.getAvailableRegions()).isEmpty(); break;
			default: throw new AssertionError(fimg.getTransparency());
			}

			if (paintBackground) {
				chessboardPainter.paintChessboard(g2D, this, clip);
			}
			g2D.drawImage(fimg, 0, 0, null);			
		}

		if (selection != null && selection.intersects(clip)) {
			g.setColor(selectionColorPreview != null ? selectionColorPreview : selectionColor);
			drawSelection(g2D, selection, SELECTION_THICKNESS);
		}
	}

	public Color getSelectionColor() {
		return selectionColor;
	}

	public void setSelectionColor(Color color) {
		assert color != null;
		if (!selectionColor.equals(color)) {
			selectionColor = color;
			if (selection != null && selectionColorPreview == null) repaint(selection);
		}
	}

	public Color getSelectionColorPreview() {
		return selectionColorPreview;
	}

	public void setSelectionColorPreview(Color color) {
		if ((selectionColorPreview != null && !selectionColorPreview.equals(color)) ||
				(selectionColorPreview == null && color != null)) {
			selectionColorPreview = color;
			if (selection != null) repaint(selection);
		}
	}

	public ImageSelection getSelection() {
		return hasSelection() ?
				new ImageSelection(selectionCenterX, selectionCenterY, selectionWidth) :
				null;
	}

	public boolean hasSelection() {
		return selection != null;
	}

	public void setTransparency(int transparency) {
		if (fimg.getTransparency() != transparency) {
	
			int oldTransparency = fimg.getTransparency();
			BufferedImage newImage = getGraphicsConfiguration()
					.createCompatibleImage(
							fimg.getWidth(),
							fimg.getHeight(),
							transparency);
			clear(newImage);
			consumer.consume(newImage);
			fimg = newImage;
			int requiredTransparency = consumer.getTransparency();
			if (oldTransparency < requiredTransparency || transparency < requiredTransparency) {
				for (Rectangle rectangle : consumer.getAvailableRegions()) {
					repaint(rectangle);
				}
			}

		}
	}

	public void refresh(int[] percentagesRv) {
		Rectangle[] toBeRepainted = consumer.consume(fimg, percentagesRv);
		for (int i = 0; i < toBeRepainted.length; i++) {
			repaint(toBeRepainted[i]);
		}
	}

	public void refresh(Consumer consumer) {
		consumer.consume(fimg);
		this.consumer = consumer;
		repaint(0, 0, fimg.getWidth(), fimg.getHeight());
	}

	public void consumeAndReset(int width, int height,
			int transparency,
			Consumer consumer, int[] percentagesRv) {
	
		BufferedImage finalImage = getGraphicsConfiguration().createCompatibleImage(
				width,
				height,
				transparency);
		clear(finalImage);
		consumer.consume(finalImage, percentagesRv);

		this.consumer = consumer;

		BufferedImage oldFinalImage = getFinalImage();
		if (oldFinalImage == null) {
			
			this.fimg = finalImage;
			Dimension oldSize = getPreferredSize();
			if (oldSize.width != width || oldSize.height != height) {
				setImageSize(width, height);
			}
			repaint(0, 0, width, height);

		} else if (oldFinalImage.getWidth() != width ||
				oldFinalImage.getHeight() != height) {
			
			int oldWidth = oldFinalImage.getWidth();
			int oldHeight = oldFinalImage.getHeight();
			this.fimg = finalImage;
			setImageSize(width, height);
			repaint(0, 0,
					Math.max(oldWidth, width),
					Math.max(oldHeight, height));

		} else {

			this.fimg = finalImage;
			repaint(0, 0, width, height);

		}
	}

	public void reset(int width, int height, int transparency, Consumer consumer) {
		this.consumer = consumer;

		if (fimg == null) {

			Dimension size = getPreferredSize();
			fimg = getGraphicsConfiguration().createCompatibleImage(
					size.width,
					size.height,
					transparency);
			clear(fimg);

		} else if (fimg.getWidth() != width || fimg.getHeight() != height) {

			int oldWidth = fimg.getWidth();
			int oldHeight = fimg.getHeight();
			fimg = getGraphicsConfiguration().createCompatibleImage(
					width,
					height,
					transparency);
			clear(fimg);
			setImageSize(width, height);
			repaint(0, 0,
					Math.max(oldWidth, width),
					Math.max(oldHeight, height));

		} else {
		
			if (fimg.getTransparency() != transparency) {
				fimg = getGraphicsConfiguration().createCompatibleImage(
						fimg.getWidth(),
						fimg.getHeight(),
						transparency);
			}
			clear(fimg);
			repaint(0, 0,
					fimg.getWidth(),
					fimg.getHeight());

		}
	}

	private void setImageSize(int width, int height) {
		Dimension newSize = new Dimension(width, height);
		heightOverWidth = (double)height / width;
		if (hasSelection()) {
			Rectangle oldSelection = selection;
			Dimension oldSize = getPreferredSize();
			double sx = (double)width / oldSize.width;
			double sy = (double)height / oldSize.height;
			selectionCenterX = (int)(sx * selectionCenterX);
			selectionCenterY = (int)(sy * selectionCenterY);
			selectionWidth = (int)(sx * selectionWidth);
			int selectionHeight = (int) (heightOverWidth * selectionWidth);
			int x = selectionCenterX - selectionWidth / 2;
			int y = selectionCenterY - selectionHeight / 2;
			selection = addThickness(new Rectangle(x, y, selectionWidth, selectionHeight), SELECTION_THICKNESS);

			if (oldSelection.intersects(selection)) {
				repaint(oldSelection.union(selection));
			} else {
				repaint(oldSelection);
				repaint(selection);
			}
		}
		
		setPreferredSize(newSize);
		revalidate();
	}

	public Consumer getConsumer() {
		return consumer;
	}

	public BufferedImage getFinalImage() {
		return fimg;
	}

	private void clear(BufferedImage bimg) {
		Graphics2D g2D = (Graphics2D) bimg.getGraphics();
		if (bimg.getTransparency() == Transparency.OPAQUE) {
			chessboardPainter.paintChessboard(
					g2D,
					this,
					new Rectangle(0, 0, bimg.getWidth(), bimg.getHeight()),
					new Point(0, 0));
		} else {
			g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
			g2D.fillRect(0, 0, bimg.getWidth(), bimg.getHeight());
		}
		g2D.dispose();
	}

	public void clearSelection() {
		Rectangle oldSelection = selection;
		if (oldSelection != null) {
			selection = null;
			repaint(oldSelection);
			if (application != null) application.setStatusMessage(null, false);
		}
	}

	private class MouseEventHandler extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e)) {
				if (selection == null) {
					selectionCenterX = e.getX();
					selectionCenterY = e.getY();
					selectionWidth = SELECTION_INITIAL_WIDTH;
					int selectionHeight = (int) (heightOverWidth * selectionWidth);
					int x = selectionCenterX - selectionWidth / 2;
					int y = selectionCenterY - selectionHeight / 2;
					selection = addThickness(new Rectangle(x, y, selectionWidth, selectionHeight), SELECTION_THICKNESS);
					repaint(selection);
					if (application != null) application.setStatusMessage(null, false);
				} else {
					Rectangle oldSelection = new Rectangle(selection);
					selection.translate(e.getX() - selectionCenterX, e.getY() - selectionCenterY);
					selectionCenterX = e.getX();
					selectionCenterY = e.getY();
					if (oldSelection.intersects(selection)) {
						repaint(oldSelection.union(selection));
					} else {
						repaint(oldSelection);
						repaint(selection);
					}
				}
			} else if (SwingUtilities.isRightMouseButton(e)) {
				clearSelection();
			}
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (selection != null) {
				int oldSelectionWidth = selectionWidth;
				int newSelectionWidth = oldSelectionWidth + e.getWheelRotation() * SELECTION_WHEEL_AMOUNT;
				int newSelectionHeight = (int) (heightOverWidth * newSelectionWidth);
				int x = selectionCenterX - newSelectionWidth / 2;
				int y = selectionCenterY - newSelectionHeight / 2;
				Rectangle oldSelection = selection;
				Rectangle newSelection = new Rectangle(x, y, newSelectionWidth, newSelectionHeight);
				if (!newSelection.isEmpty()) {
					selection = addThickness(new Rectangle(x, y, newSelectionWidth, newSelectionHeight), SELECTION_THICKNESS);
					selectionWidth = newSelectionWidth;
					repaint(e.getWheelRotation() < 0 ? oldSelection : selection);
				}
			}
		}
	}

	private static void drawSelection(Graphics g, Rectangle selection, int thickness) {
		int x = selection.x;
		int y = selection.y;
		int width = selection.width;
		int height = selection.height;
		for (int i = 0; i < thickness; i++) {
			g.drawRect(x, y, width - 1, height - 1);
			x++;
			y++;
			width -= 2;
			height -= 2;
		}
	}

	private static Rectangle addThickness(Rectangle rectangle, int thickness) {
		int delta = thickness - 1;
		rectangle.x -= delta;
		rectangle.y -= delta;
		rectangle.width += delta * 2;
		rectangle.height += delta * 2;
		return rectangle;
	}
}

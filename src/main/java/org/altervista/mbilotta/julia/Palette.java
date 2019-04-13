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

package org.altervista.mbilotta.julia;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.util.Arrays;


public final class Palette implements Transparency {

	private final int[] palette;
	private final Gradient gradient;

	public Palette(int size, Gradient gradient) {
		this(size, gradient, createBufferedImage(size), false);
	}

	private Palette(int size, Gradient gradient, BufferedImage bufferedImage, boolean cloneBuffer) {
		paintGradient(bufferedImage, gradient);
		int[] buffer = grabBuffer(bufferedImage);
		assert buffer.length == size;
		this.palette = cloneBuffer ? Arrays.copyOf(buffer, size) : buffer;
		this.gradient = gradient;
	}

	public static Palette[] createPalettes(int size, Gradient... gradients) {
		Palette[] rv = new Palette[gradients.length];
		BufferedImage bufferedImage = createBufferedImage(size);
		for (int i = gradients.length - 1; i >= 0; i--) {
			rv[i] = new Palette(size, gradients[i], bufferedImage, i > 0);
		}
		return rv;
	}

	public int getColorAt(int i) {
		return palette[i];
	}

	public int getColorAt(int i, int offset) {
		i += offset;
		if (i < 0) {
			i = (Integer.MAX_VALUE % palette.length) + (i - Integer.MAX_VALUE); 
		}
		return palette[i % palette.length];
	}

	public int getSize() {
		return palette.length;
	}

	public Gradient getGradient() {
		return gradient;
	}

	public int getTransparency() {
		return gradient.getTransparency();
	}

	public boolean isCircular() {
		return gradient.isCircular();
	}

	public static void paintGradient(BufferedImage bufferedImage, Gradient gradient) {
		Paint gradientPaint = gradient.createPaint(bufferedImage.getWidth() + (gradient.isCircular() ? 1 : 0));
		Graphics2D g2D = bufferedImage.createGraphics();
		g2D.setPaint(gradientPaint);
		g2D.fillRect(0, 0, bufferedImage.getWidth(), 1);
		g2D.dispose();
	}

	private static int[] grabBuffer(BufferedImage bufferedImage) {
		WritableRaster writableRaster = bufferedImage.getRaster();
		DataBuffer dataBuffer = writableRaster.getDataBuffer();
		return ((DataBufferInt) dataBuffer).getData();
	}

	private static BufferedImage createBufferedImage(int size) {
		return new BufferedImage(size, 1, BufferedImage.TYPE_INT_ARGB);
	}
}

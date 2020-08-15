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

package org.altervista.mbilotta.julia.impl;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import org.altervista.mbilotta.julia.Consumer;
import org.altervista.mbilotta.julia.IntermediateImage;
import org.altervista.mbilotta.julia.Progress;


public class RasterScanConsumer extends Consumer {
	
	private final PixelCalculator calculator;

	private int offset;

	public RasterScanConsumer(IntermediateImage iimg, PixelCalculator calculator) {
		this(iimg, calculator, 0);
	}

	public RasterScanConsumer(IntermediateImage iimg, PixelCalculator calculator, int offset) {
		super(iimg);
		this.calculator = calculator;
		this.offset = offset;
	}

	public Rectangle[] consume(BufferedImage fimg, int[] percentagesRv) {
		int start = offset;
		int end = mergeOffsets(percentagesRv);
		consumeImpl(fimg, start, end);
		int yStart = start / iimg.getWidth();
		int yEnd = end / iimg.getWidth() + 1;
		Rectangle[] rv = new Rectangle[] { new Rectangle(0, yStart, iimg.getWidth(), yEnd - yStart) };
		offset = end;
		return rv;
	}

	public void consume(BufferedImage fimg) {
		consumeImpl(fimg, 0, offset);
	}

	public int getTransparency() {
		return calculator.getTransparency();
	}

	public Rectangle[] getAvailableRegions() {
		int x = offset % iimg.getWidth();
		int y = offset / iimg.getWidth();
		return x == 0 ?
				new Rectangle[] { new Rectangle(0, 0, iimg.getWidth(), y) } :
				new Rectangle[] {
					new Rectangle(0, 0, x, y + 1),
					new Rectangle(x, 0, iimg.getWidth() - x, y) };
	}

	private int mergeOffsets(int[] percentagesRv) {
		int numOfProducers = iimg.getNumOfProducers();
		assert percentagesRv == null || percentagesRv.length == numOfProducers;

		int min = Integer.MAX_VALUE;
		for (int i = 0; i < numOfProducers; i++) {
			Progress progress = iimg.getProgressOf(i);
			int initialOffset = (Integer) progress.getInitialValue();
			int finalOffset = (Integer) progress.getFinalValue();
			int offset = (Integer) progress.getValue();
			if (offset < min) {
				min = offset;
			}

			if (percentagesRv != null) {
				int percentage = (100 * (offset - initialOffset)) / (finalOffset - initialOffset);
				percentagesRv[i] = percentage;
			}
		}
		return min;
	}

	private void consumeImpl(BufferedImage fimg, int start, int end) {
		int width = iimg.getWidth();
		WritableRaster raster = fimg.getRaster();
		ColorModel colorModel = fimg.getColorModel();
		Object cmOutData = colorModel.getDataElements(0, null);
		for (int i = start; i < end; i++) {
			int x = i % width;
			int y = i / width;
			int rgb = calculator.computePixel(x, y, iimg);
			raster.setDataElements(x, y, colorModel.getDataElements(rgb, cmOutData));
		}
	}

	public final PixelCalculator getPixelCalculator() {
		return calculator;
	}

	public int getOffset() {
		return offset;
	}
}

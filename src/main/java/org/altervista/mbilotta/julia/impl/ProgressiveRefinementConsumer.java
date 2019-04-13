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
import org.altervista.mbilotta.julia.impl.ProgressiveRefinement.ProgressValue;


public class ProgressiveRefinementConsumer extends Consumer {

	private final PixelCalculator calculator;

	private ProgressValue value;

	public static int getInitialChunkSize(IntermediateImage iimg) {
		return ((ProgressValue) iimg.getProgressOf(0).getInitialValue()).getChunkSize();
	}
	
	public ProgressiveRefinementConsumer(IntermediateImage iimg, PixelCalculator calculator) {
		this(iimg, calculator, new ProgressValue(0, 0, getInitialChunkSize(iimg)));
	}

	public ProgressiveRefinementConsumer(IntermediateImage iimg, PixelCalculator calculator, ProgressValue value) {
		super(iimg);
		this.calculator = calculator;
		this.value = value;
	}

	public Rectangle[] consume(BufferedImage fimg, int[] percentagesRv) {
		ProgressValue newValue = mergeProgressValues(percentagesRv);
		Rectangle[] rv;
		if (newValue.getChunkSize() == value.getChunkSize()) {
			consume(fimg,
					value.getOffsetX(), value.getOffsetY(),
					newValue.getOffsetX(), newValue.getOffsetY(),
					value.getChunkSize());
			rv = new Rectangle[] {
					new Rectangle(
							0, value.getOffsetY(),
							iimg.getWidth(), newValue.getOffsetY() + value.getChunkSize() - value.getOffsetY())
			};
		} else if (newValue.getChunkSize() << 1 == value.getChunkSize()) {
			consume(fimg,
					value.getOffsetX(), value.getOffsetY(),
					0, iimg.getHeight(),
					value.getChunkSize());
			consume(fimg,
					0, 0,
					newValue.getOffsetX(), newValue.getOffsetY(),
					newValue.getChunkSize());
			rv = new Rectangle[] {
					new Rectangle(0, value.getOffsetY(), iimg.getWidth(), iimg.getHeight() - value.getOffsetY()),
					new Rectangle(0, 0, iimg.getWidth(), newValue.getOffsetY() + newValue.getChunkSize()) };
		} else {
			consumeCompletely(fimg,
					0, 0,
					newValue.getOffsetX(), newValue.getOffsetY(),
					newValue.getChunkSize());
			rv = new Rectangle[] { new Rectangle(0, 0, iimg.getWidth(), iimg.getHeight()) };
		}

		value = newValue;
		return rv;
	}

	public int getTransparency() {
		return calculator.getTransparency();
	}

	public Rectangle[] getAvailableRegions() {
		int x = value.getOffsetX();
		int y = value.getOffsetY();
		int chunkSize = value.getChunkSize();
		if (chunkSize == getInitialChunkSize(iimg)) {
			return x == 0 ?
					new Rectangle[] { new Rectangle(0, 0, iimg.getWidth(), y) } :
					new Rectangle[] {
						new Rectangle(0, 0, x, y + chunkSize),
						new Rectangle(x, 0, iimg.getWidth() - x, y) };
		}

		return new Rectangle[] { new Rectangle(0, 0, iimg.getWidth(), iimg.getHeight()) };
	}

	private ProgressValue mergeProgressValues(int[] percentagesRv) {
		int numOfProducers = iimg.getNumOfProducers();
		assert percentagesRv != null;
		assert numOfProducers == percentagesRv.length;

		int numOfPasses = Integer.numberOfTrailingZeros(getInitialChunkSize(iimg)) + 1;
		long passLength = (long)iimg.getWidth() * iimg.getHeight();
		long fullLength = passLength * numOfPasses;
		ProgressValue min = null;
		for (int i = 0; i < numOfProducers; i++) {
			ProgressValue value = (ProgressValue) iimg.getProgressOf(i).getValue();
			if (min == null || value.compareTo(min) < 0) {
				min = value;
			}

			int currentPass = numOfPasses - Integer.numberOfTrailingZeros(value.getChunkSize()) - 1;
			long currentProgress = currentPass * passLength + computeStepProgress(value);
			percentagesRv[i] = (int)((100 * currentProgress) / fullLength);
		}

		return min;
	}

	private long computeStepProgress(ProgressValue value) {
		int chunkW = Math.min(iimg.getWidth() - value.getOffsetX(), value.getChunkSize());
		int chunkH = Math.min(iimg.getHeight() - value.getOffsetY(), value.getChunkSize());
		return (long)value.getOffsetY() * iimg.getWidth() + (chunkW * chunkH) * value.getOffsetX();
	}

	private static void fillChunk(WritableRaster raster, int x, int y, int chunkSize, Object inData) {
		if (chunkSize == 1) {
			raster.setDataElements(x, y, inData);
		} else {
			int xEnd = Math.min(x + chunkSize, raster.getWidth());
			int yEnd = Math.min(y + chunkSize, raster.getHeight());
			for (int yChunk = y; yChunk < yEnd; yChunk++) {
				for (int xChunk = x; xChunk < xEnd; xChunk++) {
					raster.setDataElements(xChunk, yChunk, inData);
				}
			}
		}
	}

	private void consume(BufferedImage fimg, int startX, int startY, int endX, int endY, int chunkSize) {
		int width = iimg.getWidth();

		PixelCalculator calculator = getPixelCalculator();
		WritableRaster raster = fimg.getRaster();
		ColorModel colorModel = fimg.getColorModel();
		Object cmOutData = colorModel.getDataElements(0, null);
		int x = startX;
		for (int y = startY; y < endY; y += chunkSize) {
			for ( ; x < width; x += chunkSize) {
				int rgb = calculator.computePixel(x, y, iimg);
				cmOutData = colorModel.getDataElements(rgb, cmOutData);
				fillChunk(raster, x, y, chunkSize, cmOutData);
			}
			x = 0;
		}

		for (; x < endX; x += chunkSize) {
			int rgb = calculator.computePixel(x, endY, iimg);
			cmOutData = colorModel.getDataElements(rgb, cmOutData);
			fillChunk(raster, x, endY, chunkSize, cmOutData);
		}
	}

	private void consumeCompletely(BufferedImage fimg, int startX, int startY, int endX, int endY, int chunkSize) {
		consume(fimg, startX, startY, endX, endY, chunkSize);

		int width = iimg.getWidth();
		int height = iimg.getHeight();

		PixelCalculator calculator = getPixelCalculator();
		WritableRaster raster = fimg.getRaster();
		ColorModel colorModel = fimg.getColorModel();
		Object cmOutData = colorModel.getDataElements(0, null);
		int x = endX;
		int y = endY;
		int parentChunkSize = chunkSize << 1;
		for ( ; y < height && !(y % parentChunkSize == 0 && x == 0); y += chunkSize) {
			for ( ; x < width; x += chunkSize) {
				int parentX = x % parentChunkSize == 0 ? x : x - chunkSize;
				int parentY = y % parentChunkSize == 0 ? y : y - chunkSize;
				int rgb = calculator.computePixel(parentX, parentY, iimg);
				cmOutData = colorModel.getDataElements(rgb, cmOutData);
				fillChunk(raster, x, y, chunkSize, cmOutData);
			}
			x = 0;
		}

		consume(fimg, x, y, 0, height, parentChunkSize);
	}

	public void consume(BufferedImage fimg) {
		if (value.getChunkSize() == getInitialChunkSize(iimg)) {
			consume(fimg,
					0, 0,
					value.getOffsetX(), value.getOffsetY(),
					value.getChunkSize());
		} else {
			consumeCompletely(fimg,
					0, 0,
					value.getOffsetX(), value.getOffsetY(),
					value.getChunkSize());
		}
	}

	public final PixelCalculator getPixelCalculator() {
		return calculator;
	}

	public ProgressValue getProgressValue() {
		return value;
	}
}

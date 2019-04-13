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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.altervista.mbilotta.julia.Formula;
import org.altervista.mbilotta.julia.IntermediateImage;
import org.altervista.mbilotta.julia.NumberFactory;
import org.altervista.mbilotta.julia.Production;
import org.altervista.mbilotta.julia.Progress;
import org.altervista.mbilotta.julia.Representation;
import org.altervista.mbilotta.julia.math.Complex;
import org.altervista.mbilotta.julia.math.CoordinateTransform;


public class RasterScan extends Production {
	
	private final PointCalculator pointCalculator;

	protected class Producer extends Production.Producer {

		private final PointCalculator pointCalculator;

		public Producer(Progress progress) {
			super(progress);
			pointCalculator = RasterScan.this.pointCalculator.newInstance();
		}

		protected void start() {
			IntermediateImage iimg = getIntermediateImage();
			int width = iimg.getWidth();
			Formula formula = getFormula();
			CoordinateTransform coordinateTransform = getCoordinateTransform();
			PointCalculator pointCalculator = getPointCalculator();
			int offset = (Integer) getProgressValue();
			int stride = iimg.getNumOfProducers();
			int length = width * iimg.getHeight();

			Thread currentThread = Thread.currentThread();
			for (int i = offset; i < length && !currentThread.isInterrupted(); ) {
				int x = i % width;
				int y = i / width;
				pointCalculator.computePoint(x, y, coordinateTransform, formula);

				i += stride;
				setProgressValue(i);
			}
			
		}

		protected final PointCalculator getPointCalculator() {
			return pointCalculator;
		}

		@Override
		public void handleException(Throwable t) {
			super.handleException(t);
			int offset = (Integer) getProgressValue();
			setProgressValue(offset + getIntermediateImage().getNumOfProducers());
		}
	}

	public RasterScan(NumberFactory numberFactory,
			Formula formula,
			Representation representation,
			CoordinateTransform coordinateTransform,
			Complex juliaSetPoint,
			PointCalculator pointCalculator) {

		super(numberFactory,
			formula,
			representation,
			coordinateTransform,
			juliaSetPoint,
			pointCalculator.getIntermediateImage());

		assert pointCalculator != null;
		this.pointCalculator = pointCalculator;
	}

	public Producer createProducer(Progress progress) {
		return new Producer(progress);
	}

	public static Progress[] createInitialProgress(int imgWidth, int imgHeight, int numOfProducers) {
		Progress[] rv = new Progress[numOfProducers];
		int arrayLength = imgWidth * imgHeight;
		int remainder = arrayLength % numOfProducers;
		int lastFrameIndex = arrayLength - remainder;
		int endFrameIndex = numOfProducers + lastFrameIndex;
		int i = 0;
		for ( ; i < remainder; i++)
			rv[i] = new Progress(i, i + endFrameIndex);
		for ( ; i < numOfProducers; i++)
			rv[i] = new Progress(i, i + lastFrameIndex);

		return rv;
	}

	public static Progress[] readProgress(int imgWidth, int imgHeight, int numOfProducers, ObjectInputStream in)
			throws IOException {
		Progress[] rv = new Progress[numOfProducers];
		int arrayLength = imgWidth * imgHeight;
		int remainder = arrayLength % numOfProducers;
		int lastFrameIndex = arrayLength - remainder;
		int endFrameIndex = numOfProducers + lastFrameIndex;
		int i = 0;
		for ( ; i < remainder; i++)
			rv[i] = new Progress(i, i + endFrameIndex, in.readInt());
		for ( ; i < numOfProducers; i++)
			rv[i] = new Progress(i, i + lastFrameIndex, in.readInt());

		return rv;
	}

	public static void readPoints(ObjectInputStream in, RasterImage iimg)
			throws IOException, ClassNotFoundException {
		int width = iimg.getWidth();
		int numOfProducers = iimg.getNumOfProducers();
		Thread currentThread = Thread.currentThread();
		for (int i = 0; i < numOfProducers; i++) {
			int end = (Integer) iimg.getProgressOf(i).getValue();
			for (int j = i; j < end; j += numOfProducers) {
				if (currentThread.isInterrupted()) return;
				int x = j % width;
				int y = j / width;
				iimg.readPoint(x, y, in);
			}
		}
	}

	public static void writePoints(ObjectOutputStream out, Object[] progressValues, RasterImage iimg)
			throws IOException {
		int width = iimg.getWidth();
		int numOfProducers = iimg.getNumOfProducers();
		Thread currentThread = Thread.currentThread();
		for (int i = 0; i < numOfProducers; i++) {
			int end = (Integer) progressValues[i];
			for (int j = i; j < end; j += numOfProducers) {
				if (currentThread.isInterrupted()) return;
				int x = j % width;
				int y = j / width;
				iimg.writePoint(x, y, out);
			}
		}
	}
}

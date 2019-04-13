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
import java.io.Serializable;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.altervista.mbilotta.julia.Formula;
import org.altervista.mbilotta.julia.IntermediateImage;
import org.altervista.mbilotta.julia.NumberFactory;
import org.altervista.mbilotta.julia.Printer;
import org.altervista.mbilotta.julia.Production;
import org.altervista.mbilotta.julia.Progress;
import org.altervista.mbilotta.julia.Representation;
import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.math.Complex;
import org.altervista.mbilotta.julia.math.CoordinateTransform;


public class ProgressiveRefinement extends Production {

	private final ProgressiveRefinementPointCalculator pointCalculator;
	protected final CyclicBarrier cyclicBarrier;

	public static class PointCalculatorAdapter implements ProgressiveRefinementPointCalculator {

		protected final PointCalculator pointCalculator;

		public PointCalculatorAdapter(PointCalculator pointCalculator) {
			this.pointCalculator = pointCalculator;
		}

		public void computePoint(int x, int y,
				CoordinateTransform coordinateTransform, Formula formula) {
			pointCalculator.computePoint(x, y, coordinateTransform, formula);
		}

		public IntermediateImage getIntermediateImage() {
			return pointCalculator.getIntermediateImage();
		}

		public void stepStarting(int chunkSize) {
		}

		public void stepFinished(int chunkSize) {
		}

		public void stepInterrupted(int chunkSize) {
		}

		public ProgressiveRefinementPointCalculator newInstance() {
			return new PointCalculatorAdapter(pointCalculator.newInstance());
		}
		
	}

	public static final class ProgressValue implements Serializable, Comparable<ProgressValue> {

		private static final long serialVersionUID = 1L;

		private final int turn;
		private final int offsetX;
		private final int offsetY;
		private final int chunkSize;

		public ProgressValue(int offsetX, int offsetY, int chunkSize) {
			this(0, offsetX, offsetY, chunkSize);
		}

		private ProgressValue(int turn, int offsetX, int offsetY, int chunkSize) {
			this.turn = turn;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.chunkSize = chunkSize;
		}

		private int getTurn() {
			return turn;
		}

		public int getOffsetX() {
			return offsetX;
		}

		public int getOffsetY() {
			return offsetY;
		}

		public int getChunkSize() {
			return chunkSize;
		}

		@Override
		public String toString() {
			return getClass().getCanonicalName() +
					"[offsetX=" + offsetX +
					", offsetY=" + offsetY +
					", chunkSize=" + chunkSize + "]";
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;

			if (obj != null && obj instanceof ProgressValue) {
				ProgressValue progressValue = (ProgressValue) obj;
				return offsetX == progressValue.offsetX &&
						offsetY == progressValue.offsetY &&
						chunkSize == progressValue.chunkSize;
			}
			return false;
		}

		@Override
		public int compareTo(ProgressValue progressValue) {
			int result = progressValue.chunkSize - chunkSize;
			if (result != 0)
				return result;

			result = offsetY - progressValue.offsetY;
			if (result != 0)
				return result;

			return offsetX - progressValue.offsetX;
		}
	}

	protected class Producer extends Production.Producer {

		private final ProgressiveRefinementPointCalculator pointCalculator;

		public Producer(Progress progress) {
			super(progress);
			pointCalculator = ProgressiveRefinement.this.pointCalculator.newInstance();
		}

		protected void start() {
			IntermediateImage iimg = getIntermediateImage();
			int width = iimg.getWidth();
			int height = iimg.getHeight();
			Formula formula = getFormula();
			CoordinateTransform coordinateTransform = getCoordinateTransform();
			ProgressiveRefinementPointCalculator pointCalculator = getPointCalculator();
			int numOfProducers = getNumOfProducers();

			ProgressValue progressValue = (ProgressValue) getProgressValue();
			int turn = progressValue.getTurn();
			int chunkSize = progressValue.getChunkSize();

			Thread currentThread = Thread.currentThread();
			boolean doSetProgress = false;
			int x = progressValue.getOffsetX();
			int y = progressValue.getOffsetY();
			boolean stepNotEmpty = y < height;
			
			Printer printer = getPrinter();

			while (true) {

				if (stepNotEmpty) pointCalculator.stepStarting(chunkSize);
				for ( ; y < height; y += chunkSize) {
					for ( ; x < width; x += chunkSize) {
						if (doSetProgress) {
							setProgressValue(new ProgressValue(turn, x, y, chunkSize));
							doSetProgress = false;
						}

						if (turn == 0) {
							if (currentThread.isInterrupted()) {
								pointCalculator.stepInterrupted(chunkSize);
								return;
							}
							printer.println("x=", x, ", y=", y);
							pointCalculator.computePoint(x, y, coordinateTransform, formula);
							doSetProgress = true;
						}
						turn = (turn + 1) % numOfProducers;
					}
					x = 0;
				}
				if (stepNotEmpty) pointCalculator.stepFinished(chunkSize);
				else stepNotEmpty = true;

				if (chunkSize > 1) {
					if (cyclicBarrier != null) {
						try {
							cyclicBarrier.await();
						} catch (BrokenBarrierException | InterruptedException e) {
							setProgressValue(new ProgressValue(turn, 0, height, chunkSize));
							return;
						}
					}
					chunkSize >>= 1;
					y = 0;
				} else {
					break;
				}
			}

			setProgressValue(new ProgressValue(turn, 0, height, 1));
		}

		protected final ProgressiveRefinementPointCalculator getPointCalculator() {
			return pointCalculator;
		}

		@Override
		public void handleException(Throwable t) {
			super.handleException(t);
			IntermediateImage iimg = getIntermediateImage();
			int width = iimg.getWidth();
			int height = iimg.getHeight();
			int numOfProducers = getNumOfProducers();

			ProgressValue progressValue = (ProgressValue) getProgressValue();
			int turn = progressValue.getTurn();
			int chunkSize = progressValue.getChunkSize();

			boolean doSetProgress = false;
			int x = progressValue.getOffsetX();
			int y = progressValue.getOffsetY();
			while (true) {

				for ( ; y < height; y += chunkSize) {
					for ( ; x < width; x += chunkSize) {
						if (doSetProgress) {
							setProgressValue(new ProgressValue(turn, x, y, chunkSize));
							return;
						}

						if (turn == 0) {
							doSetProgress = true;
						}
						turn = (turn + 1) % numOfProducers;
					}
					x = 0;
				}

				if (chunkSize > 1) {
					if (cyclicBarrier != null) {
						if (doSetProgress) {
							setProgressValue(new ProgressValue(turn, 0, height, chunkSize));
						}
						return;
					}
					chunkSize >>= 1;
					y = 0;
				} else {
					break;
				}
			}

			setProgressValue(new ProgressValue(turn, 0, height, 1));
		}
	}

	public ProgressiveRefinement(NumberFactory numberFactory,
			Formula formula,
			Representation representation,
			CoordinateTransform coordinateTransform,
			Complex juliaSetPoint,
			PointCalculator pointCalculator) {
		this(numberFactory,
			formula,
			representation,
			coordinateTransform,
			juliaSetPoint,
			pointCalculator instanceof ProgressiveRefinementPointCalculator ?
					(ProgressiveRefinementPointCalculator) pointCalculator :
					new PointCalculatorAdapter(pointCalculator));
	}

	public ProgressiveRefinement(NumberFactory numberFactory,
			Formula formula,
			Representation representation,
			CoordinateTransform coordinateTransform,
			Complex juliaSetPoint,
			ProgressiveRefinementPointCalculator pointCalculator) {

		super(numberFactory,
			formula,
			representation,
			coordinateTransform,
			juliaSetPoint,
			pointCalculator.getIntermediateImage());

		assert pointCalculator != null;
		this.pointCalculator = pointCalculator;

		int numOfProducers = pointCalculator.getIntermediateImage().getNumOfProducers();
		this.cyclicBarrier = numOfProducers > 1 ?
				new CyclicBarrier(numOfProducers) : null;
	}

	public Producer createProducer(Progress progress) {
		return new Producer(progress);
	}

	@Override
	public void resetSynchronizers() {
		if (cyclicBarrier != null) {
			cyclicBarrier.reset();
		}
	}

	public static Progress[] createInitialProgress(int imgWidth, int imgHeight, int numOfProducers, int numOfSteps) {
		Progress[] rv = new Progress[numOfProducers];
		int initialChunkSize = 1 << (numOfSteps - 1);
		for (int i = 0; i < numOfProducers; i++) {
			rv[i] = new Progress(
					new ProgressValue(i, 0, 0, initialChunkSize),
					new ProgressValue(i, 0, imgHeight, 1));
		}
		return rv;
	}

	public static Progress[] readProgress(int imgWidth, int imgHeight, int numOfProducers, int numOfSteps, ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		Progress[] rv = new Progress[numOfProducers];
		int initialChunkSize = 1 << (numOfSteps - 1);
		for (int i = 0; i < numOfProducers; i++) {
			rv[i] = new Progress(
					new ProgressValue(i, 0, 0, initialChunkSize),
					new ProgressValue(i, 0, imgHeight, 1),
					Utilities.readNonNull(in, "value", ProgressValue.class));
		}
		return rv;
	}

	public static void readPoints(ObjectInputStream in, RasterImage iimg)
			throws IOException,	ClassNotFoundException {
		int numOfProducers = iimg.getNumOfProducers();

		int chunkSize = Integer.MAX_VALUE;
		for (int i = 0; i < numOfProducers; i++) {
			ProgressValue progressValue = (ProgressValue) iimg.getProgressOf(i).getValue();
			if (progressValue.getChunkSize() < chunkSize) {
				chunkSize = progressValue.getChunkSize();
			}
		}

		Thread currentThread = Thread.currentThread();
		for (int y = 0; y < iimg.getHeight(); y += chunkSize) {
			for (int x = 0; x < iimg.getWidth(); x += chunkSize) {
				if (currentThread.isInterrupted()) return;
				iimg.readPoint(x, y, in);
			}
		}
	}

	public static void writePoints(ObjectOutputStream out, Object[] progressValues, RasterImage iimg)
			throws IOException {
		int numOfProducers = iimg.getNumOfProducers();
		int chunkSize = Integer.MAX_VALUE;
		for (int i = 0; i < numOfProducers; i++) {
			ProgressValue progressValue = (ProgressValue) progressValues[i];
			if (progressValue.getChunkSize() < chunkSize) {
				chunkSize = progressValue.getChunkSize();
			}
		}

		Thread currentThread = Thread.currentThread();
		for (int y = 0; y < iimg.getHeight(); y += chunkSize) {
			for (int x = 0; x < iimg.getWidth(); x += chunkSize) {
				if (currentThread.isInterrupted()) return;
				iimg.writePoint(x, y, out);
			}
		}
	}
}

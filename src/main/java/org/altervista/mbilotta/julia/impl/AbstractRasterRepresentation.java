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

import org.altervista.mbilotta.julia.Consumer;
import org.altervista.mbilotta.julia.Formula;
import org.altervista.mbilotta.julia.IntermediateImage;
import org.altervista.mbilotta.julia.NumberFactory;
import org.altervista.mbilotta.julia.Production;
import org.altervista.mbilotta.julia.Representation;
import org.altervista.mbilotta.julia.math.Complex;
import org.altervista.mbilotta.julia.math.CoordinateTransform;


public abstract class AbstractRasterRepresentation implements Representation {
	
	private int numOfSteps;

	public AbstractRasterRepresentation() {
		numOfSteps = 1;
	}

	public AbstractRasterRepresentation(int numOfSteps) {
		this.numOfSteps = numOfSteps;
	}

	protected abstract PointCalculator createPointCalculator(IntermediateImage iimg, NumberFactory numberFactory, CoordinateTransform coordinateTransform, boolean isJuliaSet);
	protected abstract PixelCalculator createPixelCalculator();

	protected PixelCalculator getPixelCalculator(Consumer consumer) {
		PixelCalculator rv = null;
		if (consumer instanceof RasterScanConsumer) {
			rv = ((RasterScanConsumer) consumer).getPixelCalculator();
		} else if (consumer instanceof ProgressiveRefinementConsumer) {
			rv = ((ProgressiveRefinementConsumer) consumer).getPixelCalculator();
		}
		return rv;
	}

	protected PixelCalculator recyclePixelCalculator(PixelCalculator pixelCalculator) {
		return createPixelCalculator();
	}

	protected abstract IntermediateImage readIntermediateImageImpl(int width, int height, int numOfProducers, ObjectInputStream in)
			throws ClassNotFoundException, IOException;

	protected void writeProgressValue(Object progressValue, ObjectOutputStream out)
			throws IOException {
		if (numOfSteps > 1) {
			out.writeObject(progressValue);
		} else {
			out.writeInt((Integer) progressValue);
		}
	}

	public IntermediateImage readIntermediateImage(ObjectInputStream in) throws ClassNotFoundException, IOException {
		int width = in.readInt();
		int height = in.readInt();
		int numOfProducers = in.readInt();
		return readIntermediateImageImpl(width, height, numOfProducers, in);
	}

	public void writeIntermediateImage(IntermediateImage iimg, ObjectOutputStream out) throws IOException {
		out.writeInt(iimg.getWidth());
		out.writeInt(iimg.getHeight());
		int numOfProducers = iimg.getNumOfProducers();
		out.writeInt(numOfProducers);
		Object[] progressValues = new Object[numOfProducers];
		for (int i = 0; i < numOfProducers; i++) {
			progressValues[i] = iimg.getProgressOf(i).getValue();
			writeProgressValue(progressValues[i], out);
		}
		if (numOfSteps > 1) {
			if (iimg instanceof ProgressivelyRefinedImage) {
				out.writeInt(((ProgressivelyRefinedImage) iimg).getMinIterations());
			}
			ProgressiveRefinement.writePoints(out, progressValues, (RasterImage) iimg);
		} else {
			RasterScan.writePoints(out, progressValues, (RasterImage) iimg);
		}
	}

	public Production createProduction(IntermediateImage iimg,
			NumberFactory numberFactory,
			Formula formula,
			CoordinateTransform coordinateTransform,
			Complex juliaSetPoint) {
		PointCalculator pointCalculator = createPointCalculator(iimg, numberFactory, coordinateTransform, juliaSetPoint != null);

		return numOfSteps > 1 ?
				new ProgressiveRefinement(
						numberFactory,
						formula,
						this,
						coordinateTransform,
						juliaSetPoint,
						pointCalculator) :
				new RasterScan(
						numberFactory,
						formula,
						this,
						coordinateTransform,
						juliaSetPoint,
						pointCalculator);
	}

	public Consumer createConsumer(IntermediateImage iimg) {
		PixelCalculator pixelCalculator = createPixelCalculator();
		return numOfSteps > 1 ?
				new ProgressiveRefinementConsumer(iimg, pixelCalculator) :
				new RasterScanConsumer(iimg, pixelCalculator);
	}

	public Consumer createConsumer(IntermediateImage iimg, Consumer recyclableConsumer, boolean keepProgress) {
		PixelCalculator pixelCalculator = getPixelCalculator(recyclableConsumer);
		pixelCalculator = pixelCalculator != null ? recyclePixelCalculator(pixelCalculator) : createPixelCalculator();
		if (numOfSteps > 1) {
			if (keepProgress && recyclableConsumer instanceof ProgressiveRefinementConsumer) {
				return new ProgressiveRefinementConsumer(iimg, pixelCalculator, ((ProgressiveRefinementConsumer) recyclableConsumer).getProgressValue());
			}
			return new ProgressiveRefinementConsumer(iimg, pixelCalculator);
		}

		if (keepProgress && recyclableConsumer instanceof RasterScanConsumer) {
			return new RasterScanConsumer(iimg, pixelCalculator, ((RasterScanConsumer) recyclableConsumer).getOffset());
		}
		return new RasterScanConsumer(iimg, pixelCalculator);
	}

	protected int getNumOfSteps() {
		return numOfSteps;
	}

	protected void setNumOfSteps(int numOfSteps) {
		this.numOfSteps = numOfSteps;
	}
}

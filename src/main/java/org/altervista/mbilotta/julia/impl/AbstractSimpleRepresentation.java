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

import org.altervista.mbilotta.julia.Progress;


public abstract class AbstractSimpleRepresentation extends AbstractRasterRepresentation {

	public AbstractSimpleRepresentation() {
		super(1);
	}

	public AbstractSimpleRepresentation(int numOfSteps) {
		super(numOfSteps);
	}

	protected abstract RasterImage createIntermediateImage(int width, int height, Progress[] progress);

	public RasterImage createIntermediateImage(int width, int height, int numOfProducers) {
		return getNumOfSteps() > 1 ?
				createIntermediateImage(width, height, ProgressiveRefinement.createInitialProgress(width, height, numOfProducers, getNumOfSteps())) :
				createIntermediateImage(width,	height, RasterScan.createInitialProgress(width, height, numOfProducers));
	}

	protected RasterImage readIntermediateImageImpl(int width, int height, int numOfProducers, ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		if (getNumOfSteps() > 1) {
			RasterImage rv = createIntermediateImage(width, height, ProgressiveRefinement.readProgress(width, height, numOfProducers, getNumOfSteps(), in));
			ProgressiveRefinement.readPoints(in, rv);
			return rv;
		}

		RasterImage rv = createIntermediateImage(width, height, RasterScan.readProgress(width, height, numOfProducers, in));
		RasterScan.readPoints(in, rv);
		return rv;
	}
}

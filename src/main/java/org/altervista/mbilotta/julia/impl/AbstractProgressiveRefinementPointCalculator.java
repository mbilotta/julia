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


public abstract class AbstractProgressiveRefinementPointCalculator implements
		ProgressiveRefinementPointCalculator {

	protected final ProgressivelyRefinedImage iimg;
	protected final int initialChunkSize;
	private int chunkSize;
	private int minIterations;

	public AbstractProgressiveRefinementPointCalculator(ProgressivelyRefinedImage image, int numOfSteps) {
		this.iimg = image;
		initialChunkSize = 1 << (numOfSteps - 1);
		chunkSize = initialChunkSize;
		minIterations = image.getMinIterations();
	}

	@Override
	public ProgressivelyRefinedImage getIntermediateImage() {
		return iimg;
	}

	@Override
	public void stepStarting(int chunkSize) {
		this.chunkSize = chunkSize;
		if (chunkSize == initialChunkSize >> 1) {
			minIterations = iimg.getMinIterations();
		}
	}

	@Override
	public void stepFinished(int chunkSize) {
		if (chunkSize == initialChunkSize) {
			iimg.offerMinIterations(minIterations);
		}
	}

	@Override
	public void stepInterrupted(int chunkSize) {
		stepFinished(chunkSize);
	}

	protected void offerMinIterations(int i) {
		minIterations = Math.min(minIterations, i);
	}

	protected int getMinIterations() {
		return minIterations;
	}

	protected int getChunkSize() {
		return chunkSize;
	}
}

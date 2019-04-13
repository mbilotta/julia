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

import org.altervista.mbilotta.julia.Progress;


public class ProgressivelyRefinedImage extends IntegerImage {

	private int minIterations;

	public ProgressivelyRefinedImage(int width, int height, int numOfProducers, int numOfSteps) {
		this(width, height, ProgressiveRefinement.createInitialProgress(width, height, numOfProducers, numOfSteps));
	}

	public ProgressivelyRefinedImage(int width, int height, Progress[] progress) {
		super(width, height, progress);
		minIterations = Integer.MAX_VALUE;
	}

	public boolean hasAllNeighborsEqual(int x, int y, int value, int chunkSize, boolean falseAtBorderPoints) {
		int leftX = x - chunkSize;
		int upY = y - chunkSize;
		int rightX = x + chunkSize;
		int downY = y + chunkSize;

		boolean borderPoint = false;
		if (leftX < 0) {
			leftX = x;
			borderPoint = true;
		}
		if (rightX >= width) {
			rightX = x;
			borderPoint = true;
		}
		if (upY < 0) {
			upY = y;
			borderPoint = true;
		}
		if (downY >= height) {
			downY = y;
			borderPoint = true;
		}

		return !(borderPoint && falseAtBorderPoints)
				&& getPoint(leftX, upY) == value
				&& getPoint(x, upY) == value
				&& getPoint(rightX, upY) == value
				&& getPoint(leftX, y) == value
				&& getPoint(rightX, y) == value
				&& getPoint(leftX, downY) == value
				&& getPoint(x, downY) == value
				&& getPoint(rightX, downY) == value;
	}

	public synchronized int getMinIterations() {
		return minIterations;
	}

	public synchronized void offerMinIterations(int i) {
		minIterations = Math.min(minIterations, i);
	}
}

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

import java.util.Arrays;


public abstract class IntermediateImage {

	protected final int width;
	protected final int height;
	private final Progress[] progress;

	protected IntermediateImage(int width, int height, Progress[] progress) {
		assert width >= 0 : "[width=" + width + "] < 0";
		assert height >= 0 : "[height=" + height + "] < 0";
		assert progress != null : "progress is null";
		assert progress.length > 0 : "[progress=" + Arrays.toString(progress) + "] is empty";
		assert Arrays.asList(progress).indexOf(null) == -1 : "[progress=" + Arrays.toString(progress) + "] contains null(s)";

		this.width = width;
		this.height = height;
		this.progress = progress.clone();
	}

	public final int getWidth() {
		return width;
	}

	public final int getHeight() {
		return height;
	}

	public final Progress[] getProgress() {
		return progress.clone();
	}

	public final Progress getProgressOf(int i) {
		return progress[i];
	}

	public final int getNumOfProducers() {
		return progress.length;
	}

	public boolean isComplete() {
		for (Progress p : progress) {
			if (!p.isFinalValue())
				return false;
		}

		return true;
	}
}

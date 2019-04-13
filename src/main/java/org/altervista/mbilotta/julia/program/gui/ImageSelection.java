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

package org.altervista.mbilotta.julia.program.gui;


public final class ImageSelection {

	private final int centerX;
	private final int centerY;
	private final int width;

	public ImageSelection(int centerX, int centerY, int width) {
		this.centerX = centerX;
		this.centerY = centerY;
		this.width = width;
	}

	public int getCenterX() {
		return centerX;
	}

	public int getCenterY() {
		return centerY;
	}

	public int getWidth() {
		return width;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (o != null && o instanceof ImageSelection) {
			ImageSelection is = (ImageSelection) o;
			return centerX == is.centerX && centerY == is.centerY && width == is.width;
		}
		return false;
	}

	@Override
	public String toString() {
		return getClass().getCanonicalName() +
				"[centerX=" + centerX +
				", centerY=" + centerY +
				", width=" + width + ']';
	}
}

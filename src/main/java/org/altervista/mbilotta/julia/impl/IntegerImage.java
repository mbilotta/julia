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

import org.altervista.mbilotta.julia.Progress;


public class IntegerImage extends RasterImage {
	
	public class Point {

		protected final int index;
		
		public Point(int x, int y) {
			index = y * width + x;
		}

		public int getX() {
			return index % width;
		}

		public int getY() {
			return index / width;
		}
		
		public final void set(int value) {
			array[index] = value;
		}
		
		public final int get() {
			return array[index];
		}
	}

	private final int[] array;

	public IntegerImage(int width, int height, Progress[] progress) {
		super(width, height, progress);
		array = new int[width * height];
	}

	public void setPoint(int x, int y, int value) {
		array[y * width + x] = value;
	}

	public int getPoint(int x, int y) {
		return array[y * width + x];
	}

	@Override
	public void readPoint(int x, int y, ObjectInputStream in)
			throws IOException {
		setPoint(x, y, in.readInt());
	}

	@Override
	public void writePoint(int x, int y, ObjectOutputStream out)
			throws IOException {
		out.writeInt(getPoint(x, y));
	}
}

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


final class MutableInteger {

	private int i;

	public MutableInteger() {
		this(0);
	}

	public MutableInteger(int i) {
		this.i = i;
	}

	public void set(int i) { this.i = i; }

	public int get() { return i; }

	public int getAndIncrement() {
		return i++;
	}

	public int getAndDecrement() {
		return i--;
	}

	public int incrementAndGet() {
		return ++i;
	}

	public int decrementAndGet() {
		return --i;
	}

	public int hashCode() {
		return i;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o instanceof MutableInteger) {
			return i == ((MutableInteger) o).i;
		}
		return false;
	}

	public String toString() {
		return Integer.toString(i);
	}
}
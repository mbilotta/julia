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


public final class Progress {

	private final Object initialValue;
	private final Object finalValue;
	private volatile Object value;

	public Progress(Object initialValue, Object finalValue) {
		this(initialValue, finalValue, initialValue);
	}

	public Progress(Object initialValue, Object finalValue, Object value) {
		assert initialValue != null : "initalValue is null";
		assert value != null : "value is null";

		this.initialValue = initialValue;
		this.finalValue = finalValue;
		this.value = value;
	}

	public Object getInitialValue() {
		return initialValue;
	}

	public Object getFinalValue() {
		return finalValue;
	}

	public Object getValue() {
		return value;
	}

	public boolean isFinalValue() {
		return value.equals(finalValue);
	}

	void setValue(Object value) {
		assert value != null : "value is null";
		this.value = value;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() +
				"[initialValue=" + initialValue +
				", finalValue=" + finalValue +
				", value=" + value + "]";
	}
}

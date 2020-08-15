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

package org.altervista.mbilotta.julia.program;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.NumberFactory;
import org.altervista.mbilotta.julia.math.Complex;


public final class JuliaSetPoint implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Decimal re;
	private final Decimal im;

	public JuliaSetPoint(Decimal re, Decimal im) {
		assert re != null && im != null;
		this.re = re;
		this.im = im;
	}

	public Decimal getRe() {
		return re;
	}

	public Decimal getIm() {
		return im;
	}

	public Complex toComplex(NumberFactory nf) {
		return nf.valueOf(re, im);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (o != null && o instanceof JuliaSetPoint) {
			JuliaSetPoint jsp = (JuliaSetPoint) o;
			return re.equals(jsp.re) && im.equals(jsp.im);
		}
		return false;
	}

	@Override
	public String toString() {
		return getClass().getCanonicalName() +
				"[re=" + re +
				", im=" + im + ']';
	}

	private void readObject(ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		in.defaultReadObject();

		if (re == null || im == null)
			throw new InvalidObjectException(toString());
	}
}

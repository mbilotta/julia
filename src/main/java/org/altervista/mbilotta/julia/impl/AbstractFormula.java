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

import org.altervista.mbilotta.julia.Formula;
import org.altervista.mbilotta.julia.NumberFactory;
import org.altervista.mbilotta.julia.math.Complex;


public abstract class AbstractFormula<F extends AbstractFormula<F>> implements Formula {
	
	protected Complex z;
	protected Complex c;

	@Override
	public Complex getZ() {
		return z;
	}

	@Override
	public Complex getC() {
		return c;
	}

	@Override
	public void setC(Complex c) {
		this.c = c;
	}

	protected void setZ(Complex z) {
		this.z = z;
	}

	@Override
	public void cacheConstants(NumberFactory numberFactory) {
	}

	@Override
	public abstract F newInstance();
}

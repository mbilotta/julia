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

package org.altervista.mbilotta.julia.math;

import org.altervista.mbilotta.julia.Decimal;


public interface Real extends Comparable<Real>, Complex {

	Real plus(int i);
	Real plus(Real r);

	Real minus(int i);
	Real minus(Real r);

	Real times(int i);
	Real times(Real r);

	Real dividedBy(int i);
	Real dividedBy(Real r);

	Real toThe(int n);

	Complex i();
	Real reciprocal();
	Real signum();
	Real negate();
	Real conj();
	Real rLn();
	Real exp();
	Real sin();
	Real cos();
	Real tan();
	Real atan();
	Real rSqrt();
	Real rNthRoot(int n);
	Real rPow(Real x);
	Real square();

	Decimal decimalValue();
	double doubleValue();
	float floatValue();
	int intValue();

	Real max(Real r);
	Real min(Real r);

	boolean lt(int i);
	boolean lte(int i);
	boolean gt(int i);
	boolean gte(int i);
	boolean eq(int i);

	boolean lt(Real r);
	boolean lte(Real r);
	boolean gt(Real r);
	boolean gte(Real r);
	boolean eq(Real r);
}

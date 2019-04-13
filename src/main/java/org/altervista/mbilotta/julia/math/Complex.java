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


public interface Complex {

	Complex plus(int i);
	Complex plus(Real r);
	Complex plus(Complex c);

	Complex minus(int i);
	Complex minus(Real r);
	Complex minus(Complex c);

	Complex times(int i);
	Complex times(Real r);
	Complex times(Complex c);

	Complex dividedBy(int i);
	Complex dividedBy(Real r);
	Complex dividedBy(Complex c);

	Complex toThe(int n);

	Complex reciprocal();
	Real abs();
	Real absSquared();
	Real arg();
	Real re();
	Real im();
	Complex exp();
	Complex ln();
	Complex sin();
	Complex cos();
	Complex tan();
	Complex atan();
	Complex conj();
	Complex negate();
	Complex square();
	Complex sqrt();
	Complex nthRoot(int n);
	Complex pow(Real x);
	Complex pow(Complex z);

	boolean eq(Complex c);

}

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

package org.altervista.mbilotta.julia.math.impl;

import java.util.Objects;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.math.Complex;
import org.altervista.mbilotta.julia.math.Real;


public abstract class AbstractComplex<V> implements Complex {
	
	protected abstract V getReal();
	protected abstract V getImag();

	@Override
	public Complex plus(int i) {
		return complex(
				add(getReal(), re(i)),
				getImag());
	}

	@Override
	public Complex plus(Real r) {
		return complex(
				add(getReal(), re(r)),
				getImag());
	}

	@Override
	public Complex plus(Complex c) {
		return complex(
				add(getReal(), re(c)),
				add(getImag(), im(c)));
	}

	@Override
	public Complex minus(int i) {
		return complex(
				subtract(getReal(), re(i)),
				getImag());
	}

	@Override
	public Complex minus(Real r) {
		return complex(
				subtract(getReal(), re(r)),
				getImag());
	}

	@Override
	public Complex minus(Complex c) {
		return complex(
				subtract(getReal(), re(c)),
				subtract(getImag(), im(c)));
	}

	@Override
	public Complex times(int i) {
		return times(re(i));
	}

	@Override
	public Complex times(Real r) {
		return times(re(r));
	}

	private Complex times(V v) {
		return complex(
				multiply(getReal(), v),
				multiply(getImag(), v));
	}

	@Override
	public Complex times(Complex c) {
		V re1, re2, im1, im2;
		return complex(
				multiplySub(re1=getReal(), re2=re(c), im1=getImag(), im2=im(c)),
				multiplyAdd(re2, im1, re1, im2));
	}

	@Override
	public Complex dividedBy(int i) {
		return dividedBy(re(i));
	}

	@Override
	public Complex dividedBy(Real r) {
		return dividedBy(re(r));
	}

	private Complex dividedBy(V v) {
		return complex(
				divide(getReal(), v),
				divide(getImag(), v));
	}

	@Override
	public Complex dividedBy(Complex c) {
		V re1, re2, im1, im2, norm2;
		norm2 = multiplyAdd(re2=re(c), re2, im2=im(c), im2);
		return complex(
				divide(multiplyAdd(re1=getReal(), re2, im1=getImag(), im2), norm2),
				divide(multiplySub(re2, im1, re1, im2), norm2));
	}

	@Override
	public Complex reciprocal() {
		V re, im, norm;
		norm = multiplyAdd(re=getReal(), re, im=getImag(), im);
		return complex(
				divide(re, norm),
				negate(divide(im, norm)));
	}

	@Override
	public Real abs() {
		return real(hypot(getReal(), getImag()));
	}

	@Override
	public Real absSquared() {
		V re, im;
		return real(multiplyAdd(re=getReal(), re, im=getImag(), im));
	}

	@Override
	public Real arg() {
		return real(atan2(getImag(), getReal()));
	}

	@Override
	public Real re() {
		return real(getReal());
	}

	@Override
	public Real im() {
		return real(getImag());
	}

	public Complex i() {
		return complex(zero(), getReal());
	}

	@Override
	public Complex conj() {
		return complex(getReal(), negate(getImag()));
	}

	public Real signum() {
		return real(signum(getReal()));
	}

	@Override
	public Complex exp() {
		V im1, abs;
		return complex(
				multiply(abs=exp(getReal()), cos(im1=getImag())),
				multiply(abs, sin(im1)));
	}

	@Override
	public Complex ln() {
		V re, im;
		return complex(
				ln(hypot(re=getReal(), im=getImag())),
				atan2(im, re));
	}

	public Real rLn() {
		return real(ln(getReal()));
	}

	@Override
	public Complex sin() {
		V re, exp, expm1, two;
		return complex(
				multiply(sin(re=getReal()), divide(add(exp=exp(getImag()), expm1=reciprocal(exp)), two=re(2))),
				multiply(cos(re), divide(subtract(exp, expm1), two)));
	}

	@Override
	public Complex cos() {
		V re, exp, expm1, two;
		return complex(
				multiply(cos(re=getReal()), divide(add(exp=exp(getImag()), expm1=reciprocal(exp)), two=re(2))),
				negate(multiply(sin(re), divide(subtract(exp, expm1), two))));
	}

	@Override
	public Complex tan() {
		V re, sin, cos, exp, expm1, two, sinh, cosh;
		return complex(
				multiply(sin=sin(re=getReal()), cosh=divide(add(exp=exp(getImag()), expm1=reciprocal(exp)), two=re(2))),
				multiply(cos=cos(re), sinh=divide(subtract(exp, expm1), two))).dividedBy(
						complex(
							multiply(cos, cosh),
							negate(multiply(sin, sinh))));
	}

	@Override
	public Complex atan() {
		Complex i = complex(zero(), one());
		return i.dividedBy(2).times(i.plus(this).dividedBy(i.minus(this)).ln());
	}

	@Override
	public Complex negate() {
		return complex(negate(getReal()), negate(getImag()));
	}

	@Override
	public Complex square() {
		return times(this);
	}

	@Override
	public Complex toThe(int n) {
		if (n == 0) {
			return real(one());
		}

		int exponent = n;
		Complex base = this;

		boolean isMinValue = false;
		if (exponent < 0) {
			base = base.reciprocal();
			exponent = -exponent;
			if (exponent < 0) {
				exponent = Integer.MAX_VALUE;
				isMinValue = true;
			}
		}

		switch (exponent) {
		case 2: return base.square();
		case 3: return base.square().times(base);
		case 4: return base.square().square();
		}

		while ((exponent & 1) != 1) {
			base = base.square();
			exponent >>= 1;
		}
		
		if (exponent == 1) {
			return base;
		}

		Complex result = base;
		base = base.square();
		exponent >>= 1;

		while (true) {
			if ((exponent & 1) == 1)  {
				result = result.times(base);
				if (exponent == 1) break;
			}
			
			base = base.square();
			exponent >>= 1;
		}

		if (isMinValue)
			result = result.dividedBy(this);
		
		return result;
	}

	@Override
	public Complex sqrt() {
		V re1, im1, two;
		V abs1 = hypot(re1=getReal(), im1=getImag());
		V im = sqrt(divide(subtract(abs1, re1), two=re(2)));
		return complex(
				sqrt(divide(add(abs1, re1), two)),
				multiply(signum(im1), im));
	}

	@Override
	public Complex nthRoot(int n) {
		if (n < 1)
			throw new IllegalArgumentException("" + n);
		if (n == 1)
			return this;
		if (n == 2)
			return sqrt();

		V re1, im1, abs;
		abs = nthRoot(hypot(re1=getReal(), im1=getImag()), n);
		V arg = divide(atan2(im1, re1), re(n));
		return complex(
				multiply(abs, cos(arg)),
				multiply(abs, sin(arg)));
	}

	@Override
	public Complex pow(Real x) {
		V re1, im1, re2, abs, arg;
		return complex(
				multiply(abs=pow(hypot(re1=getReal(), im1=getImag()), re2=re(x)), cos(arg=multiply(re2, atan2(im1, re1)))),
				multiply(abs, sin(arg)));
	}

	@Override
	public Complex pow(Complex z) {
		V re1, im1, re2, im2, abs1, arg1, abs, arg;
		abs=multiply(pow(abs1=hypot(re1=getReal(), im1=getImag()), re2=re(z)), exp(negate(multiply(im2=im(z), arg1=atan2(im1, re1)))));
		arg=multiplyAdd(im2, ln(abs1), re2, arg1);
		return complex(
				multiply(abs, cos(arg)),
				multiply(abs, sin(arg)));
	}

	public Real rSqrt() {
		return real(sqrt(getReal()));
	}

	public Real rNthRoot(int n) {
		return real(nthRoot(getReal(), n));
	}

	public Real rPow(Real x) {
		return real(pow(getReal(), re(x)));
	}

	public Real max(Real r) {
		return compareTo(r) > 0 ? (Real) this : r;
	}

	public Real min(Real r) {
		return compareTo(r) < 0 ? (Real) this : r;
	}

	public boolean lt(int i) {
		return lt(getReal(), re(i));
	}

	public boolean lte(int i) {
		return lte(getReal(), re(i));
	}

	public boolean gt(int i) {
		return gt(getReal(), re(i));
	}

	public boolean gte(int i) {
		return gte(getReal(), re(i));
	}

	public boolean eq(int i) {
		return eq(getReal(), re(i));
	}

	public boolean lt(Real r) {
		return lt(getReal(), re(r));
	}

	public boolean lte(Real r) {
		return lte(getReal(), re(r));
	}

	public boolean gt(Real r) {
		return gt(getReal(), re(r));
	}

	public boolean gte(Real r) {
		return gte(getReal(), re(r));
	}

	public boolean eq(Real r) {
		return eq(getReal(), re(r));
	}

	public int compareTo(Real r) {
		return compare(getReal(), re(r));
	}

	@Override
	public boolean eq(Complex c) {
		return eq(getReal(), re(c)) && eq(getImag(), im(c));
	}

	public Decimal decimalValue() {
		return toDecimal(getReal());
	}

	public double doubleValue() {
		return toDouble(getReal());
	}

	public float floatValue() {
		return toFloat(getReal());
	}

	public int intValue() {
		return toInt(getReal());
	}

	protected abstract V re(int i);
	protected abstract V re(Complex c);
	protected abstract V im(Complex c);

	protected abstract Real real(V v);
	protected abstract Complex complex(V re, V im);

	protected abstract V add(V a, V b);
	protected abstract V multiply(V a, V b);
	protected abstract V subtract(V a, V b);
	protected abstract V divide(V a, V b);

	protected V multiplyAdd(V a, V b, V c, V d) {
		return add(multiply(a, b), multiply(c, d));
	}
	protected V multiplySub(V a, V b, V c, V d) {
		return subtract(multiply(a, b), multiply(c, d));
	}

	protected V reciprocal(V x) {
		return divide(one(), x);
	}

	protected abstract V abs(V x);
	protected abstract V signum(V x);
	protected abstract V hypot(V x, V y);
	protected abstract V negate(V x);
	protected abstract V ln(V x);
	protected abstract V exp(V x);
	protected abstract V sin(V x);
	protected abstract V cos(V x);
	protected V tan(V x) {
		return divide(sin(x), cos(x));
	}
	protected V sinh(V x) {
		V exp;
		return divide(subtract(exp=exp(x), reciprocal(exp)), re(2));
	}
	protected V cosh(V x) {
		V exp;
		return divide(add(exp=exp(x), reciprocal(exp)), re(2));
	}
	protected V tanh(V x) {
		V exp, expm1;
		return divide(subtract(exp=exp(x), expm1=reciprocal(exp)), add(exp, expm1));
	}
	protected abstract V atan(V x);
	protected abstract V atan2(V y, V x);

	protected V sqrt(V x) {
		return nthRoot(x, 2);
	}
	protected V nthRoot(V x, int n) {
		return pow(x, reciprocal(re(n)));
	}
	protected V nthPow(V x, int n) {
		return pow(x, re(n));
	}
	protected abstract V pow(V x, V r);

	protected abstract V zero();
	protected abstract V one();

	protected String toString(V x) {
		return x.toString();
	}
	protected Decimal toDecimal(V x) {
		return new Decimal(toString(x));
	}
	protected abstract double toDouble(V x);
	protected abstract float toFloat(V x);
	protected abstract int toInt(V x);

	protected boolean lt(V a, V b) {
		return compare(a, b) < 0;
	}
	protected boolean lte(V a, V b) {
		return compare(a, b) <= 0;
	}
	protected boolean gt(V a, V b) {
		return compare(a, b) > 0;
	}
	protected boolean gte(V a, V b) {
		return compare(a, b) >= 0;
	}
	protected boolean eq(V a, V b) {
		return compare(a, b) == 0;
	}
	protected abstract int compare(V a, V b);

	@Override
	public int hashCode() {
		return Objects.hash(getReal(), getImag());
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		
		if (o instanceof AbstractComplex) {
			AbstractComplex<?> c = (AbstractComplex<?>) o;
			return getReal().equals(c.getReal()) &&
					getImag().equals(c.getImag());
		}
		return false;
	}

	public String toString() {
		return "(" + toString(getReal()) + ", " + toString(getImag()) + ")";
	}
}

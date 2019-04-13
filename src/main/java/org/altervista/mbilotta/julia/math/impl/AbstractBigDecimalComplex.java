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

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.math.Complex;
import org.altervista.mbilotta.julia.math.Real;


public abstract class AbstractBigDecimalComplex extends AbstractComplex<BigDecimal> {
	
	private final MathContext mc;
	private final BigDecimal re;

	protected AbstractBigDecimalComplex(MathContext mc, BigDecimal v) {
		this.mc = mc;
		this.re = v;
	}

	public AbstractBigDecimalComplex(MathContext mc, int i) {
		this(mc, BigDecimal.valueOf(i).round(mc));
	}

	public AbstractBigDecimalComplex(MathContext mc, String s) {
		this(mc, new BigDecimal(s, mc));
	}

	public AbstractBigDecimalComplex(MathContext mc, Decimal d) {
		this(mc, d.toNormalizedString());
	}

	public AbstractBigDecimalComplex(MathContext mc, Real r) {
		this(mc, ((AbstractBigDecimalComplex) r).getReal().round(mc));
	}

	@Override
	protected final BigDecimal getReal() {
		return re;
	}

	@Override
	protected BigDecimal getImag() {
		return BigDecimal.ZERO;
	}

	public final MathContext getMathContext() {
		return mc;
	}

	@Override
	protected BigDecimal re(int i) {
		return BigDecimal.valueOf(i);
	}

	@Override
	protected final BigDecimal re(Complex c) {
		return ((AbstractBigDecimalComplex) c).getReal();
	}

	@Override
	protected final BigDecimal im(Complex c) {
		return ((AbstractBigDecimalComplex) c).getImag();
	}

	@Override
	protected BigDecimal add(BigDecimal a, BigDecimal b) {
		return a.add(b, mc);
	}

	@Override
	protected BigDecimal multiply(BigDecimal a, BigDecimal b) {
		return a.multiply(b, mc);
	}

	@Override
	protected BigDecimal subtract(BigDecimal a, BigDecimal b) {
		return a.subtract(b, mc);
	}

	@Override
	protected BigDecimal divide(BigDecimal a, BigDecimal b) {
		return a.divide(b, mc);
	}

	@Override
	protected BigDecimal multiplyAdd(BigDecimal a, BigDecimal b, BigDecimal c, BigDecimal d) {
		return a.multiply(b).add(c.multiply(d), mc);
	}

	@Override
	protected BigDecimal multiplySub(BigDecimal a, BigDecimal b, BigDecimal c, BigDecimal d) {
		return a.multiply(b).subtract(c.multiply(d), mc);
	}

	@Override
	protected BigDecimal abs(BigDecimal x) {
		return x.abs();
	}

	@Override
	protected BigDecimal signum(BigDecimal x) {
		return BigDecimal.valueOf(x.signum());
	}

	@Override
	protected BigDecimal negate(BigDecimal x) {
		return x.negate();
	}

	@Override
	protected BigDecimal nthPow(BigDecimal x, int n) {
		return x.pow(n, mc);
	}

	@Override
	protected BigDecimal zero() {
		return BigDecimal.ZERO;
	}

	@Override
	protected BigDecimal one() {
		return BigDecimal.ONE;
	}

	@Override
	protected double toDouble(BigDecimal x) {
		return x.doubleValue();
	}

	@Override
	protected float toFloat(BigDecimal x) {
		return x.floatValue();
	}

	@Override
	protected int toInt(BigDecimal x) {
		return x.intValue();
	}

	@Override
	protected int compare(BigDecimal a, BigDecimal b) {
		return a.compareTo(b);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mc, getReal(), getImag());
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		
		if (o instanceof AbstractBigDecimalComplex) {
			AbstractBigDecimalComplex c = (AbstractBigDecimalComplex) o;
			return mc.equals(c.mc)
					&& getReal().equals(c.getReal())
					&& getImag().equals(c.getImag());
		}
		return false;
	}
}

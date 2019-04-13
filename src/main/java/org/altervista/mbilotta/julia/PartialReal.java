package org.altervista.mbilotta.julia;

import org.altervista.mbilotta.julia.math.Complex;
import org.altervista.mbilotta.julia.math.Real;


public abstract class PartialReal implements Real {

	@Override
	public int compareTo(Real o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Complex plus(Complex c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Complex minus(Complex c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Complex times(Complex c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Complex dividedBy(Complex c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real abs() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real absSquared() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real arg() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real re() {
		return this;
	}

	@Override
	public Real im() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Complex ln() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Complex sqrt() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Complex nthRoot(int n) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Complex pow(Real x) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Complex pow(Complex z) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean eq(Complex c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real plus(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real plus(Real r) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real minus(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real minus(Real r) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real times(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real times(Real r) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real dividedBy(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real dividedBy(Real r) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real toThe(int n) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Complex i() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real reciprocal() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real signum() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real negate() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real conj() {
		return this;
	}

	@Override
	public Real rLn() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real exp() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real sin() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real cos() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real tan() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real atan() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real rSqrt() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real rNthRoot(int n) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real rPow(Real x) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real square() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Decimal decimalValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double doubleValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public float floatValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int intValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Real max(Real r) {
		return compareTo(r) > 0 ? this : r;
	}

	@Override
	public Real min(Real r) {
		return compareTo(r) < 0 ? this : r;
	}

	@Override
	public boolean lt(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean lte(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean gt(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean gte(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean eq(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean lt(Real r) {
		return compareTo(r) < 0;
	}

	@Override
	public boolean lte(Real r) {
		return compareTo(r) <= 0;
	}

	@Override
	public boolean gt(Real r) {
		return compareTo(r) > 0;
	}

	@Override
	public boolean gte(Real r) {
		return compareTo(r) >= 0;
	}

	@Override
	public boolean eq(Real r) {
		return compareTo(r) == 0;
	}
}

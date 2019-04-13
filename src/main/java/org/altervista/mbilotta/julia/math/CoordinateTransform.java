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


public final class CoordinateTransform {

	private final Real re0, im0;
	private final Real x0, y0;
	private final Real scaleRe;
	private final Real scaleIm;

	public CoordinateTransform(Real re0, Real im0, Real re1, Real im1,
			Real x0, Real y0, Real x1, Real y1) {
		this(re0, im0, x0, y0,
				re1.minus(re0).dividedBy(x1.minus(x0)),
				im1.minus(im0).dividedBy(y1.minus(y0)));
	}

	public CoordinateTransform(Real re0, Real im0,
			Real x0, Real y0, Real scaleRe, Real scaleIm) {
		this.re0 = re0;
		this.im0 = im0;
		this.x0 = x0;
		this.y0 = y0;
		this.scaleRe = scaleRe;
		this.scaleIm = scaleIm;
	}

	public Real toRe(int x) {
		return re0.plus(scaleRe.times(x0.minus(x).negate()));
	}

	public Real toIm(int y) {
		return im0.plus(scaleIm.times(y0.minus(y).negate()));
	}

	public Real toRe(Real x) {
		return re0.plus(scaleRe.times(x.minus(x0)));
	}

	public Real toIm(Real y) {
		return im0.plus(scaleIm.times(y.minus(y0)));
	}

	public Complex toComplex(int x, int y) {
		return toRe(x).plus(toIm(y).i());
	}

	public Complex toComplex(Real x, Real y) {
		return toRe(x).plus(toIm(y).i());
	}

	public Real getRe0() {
		return re0;
	}

	public Real getIm0() {
		return im0;
	}

	public Real toX(Real re) {
		return x0.plus(re.minus(re0).dividedBy(scaleRe));
	}

	public Real toY(Real im) {
		return y0.plus(im.minus(im0).dividedBy(scaleIm));
	}

	public Real getX0() {
		return x0;
	}

	public Real getY0() {
		return y0;
	}

	public Real getScaleRe() {
		return scaleRe;
	}

	public Real getScaleIm() {
		return scaleIm;
	}

	@Override
	public String toString() {
		return "CoordinateTransform[x0=" + x0 + ", y0=" + y0 + ", re0=" + re0
				+ ", im0=" + im0 + ", scaleRe=" + scaleRe + ", scaleIm="
				+ scaleIm + "]";
	}
}

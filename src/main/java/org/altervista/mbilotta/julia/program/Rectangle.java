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
import org.altervista.mbilotta.julia.math.CoordinateTransform;
import org.altervista.mbilotta.julia.math.Real;


public final class Rectangle implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Decimal re0;
	private final Decimal im0;
	private final Decimal re1;
	private final Decimal im1;

	public Rectangle(Decimal re0, Decimal im0, Decimal re1, Decimal im1) {
		assert re0 != null && im0 != null && re1 != null && im1 != null;
		this.re0 = re0;
		this.im0 = im0;
		this.re1 = re1;
		this.im1 = im1;
	}

	public static boolean isEmptyRectangle(Decimal re0, Decimal im0, Decimal re1, Decimal im1) {
		return re0.equals(re1) || im0.equals(im1);
	}

	public Decimal getRe0() {
		return re0;
	}

	public Decimal getIm0() {
		return im0;
	}

	public Decimal getRe1() {
		return re1;
	}

	public Decimal getIm1() {
		return im1;
	}

	public boolean isEmpty() {
		return re0.equals(re1) || im0.equals(im1);
	}

	public Rectangle normalize() {
		Decimal[] re = sortAscending(re0, re1);
		Decimal[] im = sortDescending(im0, im1);
		return new Rectangle(re[0], im[0], re[1], im[1]);
	}

	public CoordinateTransform createCoordinateTransform(int imageWidth, int imageHeight, boolean forceEqualScales, NumberFactory nf) {
		Rectangle rectangle = normalize();
		if (forceEqualScales) {
			Real re0 = nf.valueOf(rectangle.getRe0());
			Real im0 = nf.valueOf(rectangle.getIm0());
			Real re1 = nf.valueOf(rectangle.getRe1());
			Real im1 = nf.valueOf(rectangle.getIm1());
			Real width = re1.minus(re0);
			Real height = im0.minus(im1);
			Real srcRatio = height.dividedBy(width);
			Real dstRatio = nf.valueOf(imageHeight).dividedBy(imageWidth);
			Real scaleRe, scaleIm;
			int result = srcRatio.compareTo(dstRatio);
			if (result > 0) {
				scaleRe = height.dividedBy(imageHeight);
				scaleIm = scaleRe.negate();
				Real centerRe = re0.plus(re1).dividedBy(2);
				re0 = centerRe.minus(scaleRe.times((imageWidth - 1) / 2));
			} else if (result < 0) {
				scaleRe = width.dividedBy(imageWidth);
				scaleIm = scaleRe.negate();
				Real centerIm = im0.plus(im1).dividedBy(2);
				im0 = centerIm.minus(scaleIm.times((imageHeight - 1) / 2));
			} else {
				scaleRe = width.dividedBy(imageWidth);
				scaleIm = scaleRe.negate();
			}

			return new CoordinateTransform(re0, im0,
					nf.zero(), nf.zero(),
					scaleRe, scaleIm);
		}
		
		return new CoordinateTransform(nf.valueOf(rectangle.getRe0()), nf.valueOf(rectangle.getIm0()),
				nf.valueOf(rectangle.getRe1()), nf.valueOf(rectangle.getIm1()),
				nf.zero(), nf.zero(),
				nf.valueOf(imageWidth), nf.valueOf(imageHeight));
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		
		if (o instanceof Rectangle) {
			Rectangle r = (Rectangle) o;
			return re0.equals(r.re0) &&
					im0.equals(r.im0) &&
					re1.equals(r.re1) &&
					im1.equals(r.im1);
		}
		
		return false;
	}

	public String toString() {
		return getClass().getCanonicalName() +
				"[re0=" + re0 +
				", im0=" + im0 +
				", re1=" + re1 +
				", im1=" + im1 + ']';
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		
		if (re0 == null || im0 == null ||
			re1 == null || im1 == null)
			throw new InvalidObjectException(toString());
	}

	private static Decimal[] sortAscending(Decimal a, Decimal b) {
		return a.compareTo(b) < 0 ? new Decimal[] { a, b } : new Decimal[] { b, a };
	}

	private static Decimal[] sortDescending(Decimal a, Decimal b) {
		return a.compareTo(b) > 0 ? new Decimal[] { a, b } : new Decimal[] { b, a };
	}
}

/*
 * Copyright (C) 2020 Maurizio Bilotta.
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

public final class Circle implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final Decimal centerRe;
    private final Decimal centerIm;
    private final Decimal diameter;

    public Circle(Decimal centerRe, Decimal centerIm, Decimal diameter) {
        assert centerRe != null && centerIm != null && diameter != null;
        this.centerRe = centerRe;
        this.centerIm = centerIm;
        this.diameter = diameter;
    }

    public Decimal getCenterRe() {
        return centerRe;
    }

    public Decimal getCenterIm() {
        return centerIm;
    }

    public Decimal getDiameter() {
        return diameter;
    }

	public boolean isEmpty() {
		return diameter.getSignum() <= 0;
	}

	public Rectangle createRectangle(int imageWidth, int imageHeight, NumberFactory nf) {
		if (isEmpty()) {
			return new Rectangle(centerRe, centerIm, centerRe, centerIm);
		}
		Real aspectRatio = nf.valueOf(imageHeight).dividedBy(imageWidth);
		Real width = nf.valueOf(diameter);
		Real height = aspectRatio.times(width);
		Real re0 = nf.valueOf(centerRe).minus(width.dividedBy(2));
		Real im0 = nf.valueOf(centerIm).plus(height.dividedBy(2));
		Real re1 = re0.plus(width);
		Real im1 = im0.minus(height);
		return new Rectangle(re0.decimalValue(), im0.decimalValue(), re1.decimalValue(), im1.decimalValue());
	}

	public CoordinateTransform createCoordinateTransform(int imageWidth, int imageHeight, NumberFactory nf) {
		Real aspectRatio = nf.valueOf(imageHeight).dividedBy(imageWidth);
		Real width = nf.valueOf(diameter);
		Real height = aspectRatio.times(width);
		Real re0 = nf.valueOf(centerRe).minus(width.dividedBy(2));
		Real im0 = nf.valueOf(centerIm).plus(height.dividedBy(2));
		Real scaleRe = width.dividedBy(imageWidth);
		Real scaleIm = scaleRe.negate();
		return new CoordinateTransform(re0, im0, nf.zero(), nf.zero(), scaleRe, scaleIm);
	}

    @Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		
		if (o instanceof Circle) {
			Circle c = (Circle) o;
			return centerRe.equals(c.centerRe) &&
				    centerIm.equals(c.centerIm) &&
				    diameter.equals(c.diameter);
		}
		
		return false;
	}

    @Override
	public String toString() {
		return getClass().getCanonicalName() +
				"[centerRe=" + centerRe +
				", centerIm=" + centerIm +
				", diameter=" + diameter + ']';
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		
		if (centerRe == null || centerIm == null || diameter == null) {
			throw new InvalidObjectException(toString());
		}
	}
}

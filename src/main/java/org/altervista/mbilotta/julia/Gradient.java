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

package org.altervista.mbilotta.julia;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Transparency;
import java.util.Arrays;
import java.util.Iterator;
import java.io.*;

import static org.altervista.mbilotta.julia.Utilities.*;


public final class Gradient implements Transparency, Serializable, Iterable<Gradient.Stop> {

	public static final class Stop implements Transparency {
		
		private final Color color;
		private final float location;

		public Stop(float location, int r, int g, int b) {
			this(location, new Color(r, g, b));
		}
		
		public Stop(float location, int r, int g, int b, int a) {
			this(location, new Color(r, g, b, a));
		}

		public Stop(float location, Color color) {
			if (location < 0f || location > 1f)
				throw new IllegalArgumentException("[location=" + location + "] out of the range 0 to 1");
			
			this.color = color;
			this.location = location;
		}

		private Stop(Color color, float location) {
			this.color = color;
			this.location = location;
		}

		public Color getColor() {
			return color;
		}

		public float getLocation() {
			return location;
		}

		public int getRed() {
			return color.getRed();
		}

		public int getGreen() {
			return color.getGreen();
		}

		public int getBlue() {
			return color.getBlue();
		}

		public int getAlpha() {
			return color.getAlpha();
		}

		public int getTransparency() {
			return color.getTransparency();
		}
		
		public int hashCode() {
			int hashCode = 17;
			hashCode = 31 * hashCode + color.hashCode();
			hashCode = 31 * hashCode + Float.floatToIntBits(location);
			return hashCode;
		}

		public boolean equals(Object o) {
			if (o == this)
				return true;
			
			if (o instanceof Stop) {
				return equals((Stop) o);
			}
			
			return false;
		}
		
		private boolean equals(Stop s) {
			return s.color.equals(color) && s.location == location;
		}
		
		public String toString() {
			return "[" + location + ", 0x" + Integer.toHexString(color.getRGB()) + "]";
		}
	}

	private static final long serialVersionUID = 7030221179952492512L;
	private static final int VERSION = 1;

	private transient Stop[] stops;
	private transient int transparency;
	private transient boolean circular;

	public Gradient(int startR, int startG, int startB,
			int endR, int endG, int endB) {
		this(new Color(startR, startG, startB), new Color(endR, endG, endB));
	}

	public Gradient(int startR, int startG, int startB, int startA,
			int endR, int endG, int endB, int endA) {
		this(new Color(startR, startG, startB, startA), new Color(endR, endG, endB, endA));
	}

	public Gradient(Color startColor, Color endColor) {
		stops = new Stop[] { new Stop(startColor, 0f), new Stop(endColor, 1f) };
		if (startColor.getAlpha() == 255 && endColor.getAlpha() == 255) {
			transparency = Transparency.OPAQUE;
		} else if (startColor.getAlpha() == 0 && endColor.getAlpha() == 0) {
			transparency = Transparency.BITMASK;
		} else {
			transparency = Transparency.TRANSLUCENT;
		}
		circular = startColor.equals(endColor);
	}

	public Gradient(Stop[] stops) {
		this.stops = fixStops(stops);
		transparency = computeTransparency();
		circular = computeCircular();
	}

	public Gradient(Stop stop0, Stop stop1, Stop... otherStops) {
		stops = fixStops(stop0, stop1, otherStops);
		transparency = computeTransparency();
		circular = computeCircular();
	}

	public Stop getStop(int index) {
		return stops[index];
	}

	public int getNumOfStops() {
		return stops.length;
	}

	public Stop[] getStops() {
		return Arrays.copyOf(stops, stops.length);
	}

	public Iterator<Stop> iterator() {
		return Arrays.asList(stops).iterator();
	}

	public boolean isCircular() {
		return circular;
	}

	public int getTransparency() {
		return transparency;
	}

	public int hashCode() {
		int hashCode = 17;
		Stop[] stops = this.stops;
		int numOfStops = stops.length;
		for (int i = 0; i < numOfStops; i++) {
			hashCode = 31 * hashCode + stops[i].hashCode();
		}

		return hashCode;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (o instanceof Gradient) {
			return equals((Gradient) o);
		}

		return false;
	}

	private boolean equals(Gradient g) {
		Stop[] stops = this.stops;
		Stop[] gstops = g.stops;
		int numOfStops = stops.length;
		if (numOfStops != gstops.length)
			return false;

		for (int i = 0; i < numOfStops; i++) {
			if (!stops[i].equals(gstops[i]))
				return false;
		}
		
		return true;
	}

	public Paint createPaint(int size) {
		Stop[] stops = this.stops;
		int numOfStops = stops.length;
		if (numOfStops == 2) {
			return new GradientPaint(0, 0, stops[0].color, size - 1, 0, stops[1].color);
		}

		Color[] colors = new Color[numOfStops];
		float[] fractions = new float[numOfStops];
		for (int i = 0; i < numOfStops; i++) {
			Stop stop = stops[i];
			colors[i] = stop.color;
			fractions[i] = stop.location;
		}

		return new LinearGradientPaint(0, 0, size - 1, 0, fractions, colors);
	}

	public String toString() {
		return getClass().getSimpleName() +
			"[stops=" + Arrays.toString(stops) + ']';
	}

	private int computeTransparency() {
		boolean opaque = true;
		boolean bitmask = true;
		int numOfStops = stops.length;
		for (int i = 0; i < numOfStops && (opaque || bitmask); i++) {
			int alpha = stops[i].getColor().getAlpha();
			opaque &= alpha == 255;
			bitmask &= alpha == 0;
		}
		if (opaque)	return OPAQUE;
		if (bitmask) return BITMASK;
		return TRANSLUCENT;
	}

	private boolean computeCircular() {
		return stops[0].color.equals(stops[stops.length - 1].color);
	}

	private Stop[] fixStops(Stop[] stops) {
		int length = stops.length;
		if (length < 2)
			throw new IllegalArgumentException("[stops.length=" + length + "] < 2");

		boolean fix0 = stops[0].location != 0f;
		boolean fix1 = stops[length - 1].location != 1f;
		int rvOffset = 0;
		int rvLength = length;
		if (fix0) { rvLength++; rvOffset++; }
		if (fix1) { rvLength++; }
		Stop[] rv = new Stop[rvLength];
		if (fix0) { rv[0] = new Stop(stops[0].color, 0f); }
		if (fix1) { rv[rvLength - 1] = new Stop(stops[length - 1].color, 1f); }

		// Check that locations progress in increasing order
		float previous = -1f;
		for (int i = 0; i < length; i++) {
			Stop stop = stops[i];
			rv[rvOffset++] = stop;

			float current = stop.location;
			if (previous >= current)
				throw new IllegalArgumentException("[stops=" + Arrays.toString(stops) + "] does not have strictly increasing locations");
			previous = current;
		}

		return rv;
	}

	private Stop[] fixStops(Stop stop0, Stop stop1, Stop[] otherStops) {
		int length = otherStops.length;

		boolean fix0 = stop0.location != 0f;
		boolean fix1 = otherStops[length - 1].location != 1f;
		int rvOffset = 2;
		int rvLength = 2 + length;
		if (fix0) { rvLength++; rvOffset++; }
		if (fix1) { rvLength++; }
		Stop[] rv = new Stop[rvLength];
		if (fix0) {
			rv[0] = new Stop(stop0.color, 0f);
			rv[1] = stop0;
			rv[2] = stop1;
		} else {
			rv[0] = stop0;
			rv[1] = stop1;
		}
		if (fix1) { rv[rvLength - 1] = new Stop(otherStops[length - 1].color, 1f); }

		// Check that locations progress in increasing order
		boolean locationsOutOfOrder = stop0.location >= stop1.location;
		float previous = stop1.location;
		for (int i = 0; i < length; i++) {
			Stop stop = otherStops[i];
			rv[rvOffset++] = stop;

			float current = stop.location;
			locationsOutOfOrder |= previous >= current;
			previous = current;
		}

		if (locationsOutOfOrder) {
			int from = fix0 ? 1 : 0;
			int to = from + 2 + length;
			throw new IllegalArgumentException("[stops=" + Arrays.toString(Arrays.copyOfRange(rv, from, to)) + "] does not have strictly increasing locations");
		}

		return rv;
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		int version = in.readInt();
		if (version != VERSION)
			throw new InvalidObjectException("Invalid version number: " + version);

		int numOfStops = in.readInt();
		if (numOfStops < 2)
			throw new InvalidObjectException("[numOfStops=" + numOfStops + "] < 2");
		
		Stop[] stops = new Stop[numOfStops];
		Color color = readNonNull(in, "stops[0].color", Color.class);
			
		float location = in.readFloat();
		if (location != 0f)
			throw new InvalidObjectException("[stops[0].location=" + location + "] != 0");

		stops[0] = new Stop(color, 0f);
		float previous = 0f;
		for (int i = 1; i < numOfStops; i++) {
			color = readNonNull(in, join("stops[", i, "].color"), Color.class);

			location = in.readFloat();
			if (location < 0f || location > 1f)
				throw new InvalidObjectException("[stops[" + i + "].location=" + location + "] out of the range 0 to 1");
			if (location <= previous)
				throw new InvalidObjectException("[stops[" + i + "].location=" + location + "] <= [stops[" + (i - 1) + "].location=" + previous + "]");
			previous = location;

			stops[i] = new Stop(color, location);
		}

		if (location != 1f)
			throw new InvalidObjectException("[stops[numOfStops - 1].location=" + location + "] != 1");

		this.stops = stops;
		this.transparency = computeTransparency();
		this.circular = computeCircular();
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();

		out.writeInt(VERSION);

		Stop[] stops = this.stops;
		int numOfStops = stops.length;
		out.writeInt(numOfStops);
		for (int i = 0; i < numOfStops; i++) {
			Stop stop = stops[i];
			out.writeObject(stop.color);
			out.writeFloat(stop.location);
		}
	}
}

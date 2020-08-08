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
package org.altervista.mbilotta.julia;

import java.awt.Color;

import java.util.ArrayList;
import java.util.List;


public final class GradientBuilder {

    private final List<Gradient.Stop> stops = new ArrayList<>();

    public GradientBuilder withStop(Color color) {
        if (stops.isEmpty()) {
            return withStop(color, 0f);
        }
        return withStop(color, 1f);
    }

    public GradientBuilder withStop(Color color, float location) {
        stops.add(new Gradient.Stop(location, color));
        return this;
    }

    public GradientBuilder withStop(int r, int g, int b) {
        if (stops.isEmpty()) {
            return withStop(r, g, b, 0f);
        }
        return withStop(r, g, b, 1f);
    }

    public GradientBuilder withStop(int r, int g, int b, int a) {
        if (stops.isEmpty()) {
            return withStop(r, g, b, a, 0f);
        }
        return withStop(r, g, b, a, 1f);
    }

    public GradientBuilder withStop(int r, int g, int b, float location) {
        stops.add(new Gradient.Stop(location, r, g, b));
        return this;
    }

    public GradientBuilder withStop(int r, int g, int b, int a, float location) {
        stops.add(new Gradient.Stop(location, r, g, b, a));
        return this;
    }

    public GradientBuilder makeCircular() {
        stops.add(new Gradient.Stop(1f, stops.get(0).getColor()));
        return this;
    }

    public Gradient build() {
        Gradient rv = new Gradient(stops.toArray(new Gradient.Stop[0]));
        stops.clear();
        return rv;
    }
}
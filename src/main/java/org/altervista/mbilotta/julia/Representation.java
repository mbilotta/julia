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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.altervista.mbilotta.julia.math.Complex;
import org.altervista.mbilotta.julia.math.CoordinateTransform;


public interface Representation {
	
	IntermediateImage createIntermediateImage(int width, int height, int numOfProducers);
	IntermediateImage readIntermediateImage(ObjectInputStream in) throws ClassNotFoundException, IOException;
	void writeIntermediateImage(IntermediateImage iimg, ObjectOutputStream out) throws IOException;

	Production createProduction(IntermediateImage iimg,
			NumberFactory numberFactory,
			Formula formula,
			CoordinateTransform coordinateTransform,
			Complex juliaSetPoint);

	Consumer createConsumer(IntermediateImage iimg);
	Consumer createConsumer(IntermediateImage iimg, Consumer recyclableConsumer, boolean keepProgress);

}

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

import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;


public abstract class Consumer implements Transparency {
	
	protected final IntermediateImage iimg;

	protected Consumer(IntermediateImage iimg) {
		assert iimg != null;
		this.iimg = iimg;
	}

	public abstract Rectangle[] consume(BufferedImage fimg, int[] percentagesRv);
	public abstract void consume(BufferedImage fimg);
	public abstract Rectangle[] getAvailableRegions();

	public final IntermediateImage getIntermediateImage() {
		return iimg;
	}

	public final int getNumOfProducers() {
		return iimg.getNumOfProducers();
	}

	public BufferedImage createFinalImage() {
		int imageType = getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		return new BufferedImage(iimg.getWidth(), iimg.getHeight(), imageType);
	}
}

package org.altervista.mbilotta.julia.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.altervista.mbilotta.julia.IntermediateImage;
import org.altervista.mbilotta.julia.Progress;


public abstract class RasterImage extends IntermediateImage {
	
	protected RasterImage(int width, int height, Progress[] progress) {
		super(width, height, progress);
	}

	public abstract void readPoint(int x, int y, ObjectInputStream in) throws IOException, ClassNotFoundException;
	public abstract void writePoint(int x, int y, ObjectOutputStream out) throws IOException;
}

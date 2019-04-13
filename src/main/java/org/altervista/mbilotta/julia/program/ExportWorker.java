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

import static org.altervista.mbilotta.julia.Utilities.println;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.event.IIOWriteWarningListener;
import javax.imageio.stream.ImageOutputStream;


public class ExportWorker extends BlockingSwingWorker<Void> {

	private final BufferedImage fimg;
	private final File file;
	private final ImageWriter imageWriter;
	private final ImageWriteParam imageWriteParam;

	private class Listener implements IIOWriteProgressListener, IIOWriteWarningListener {
		@Override
		public void imageStarted(ImageWriter source, int imageIndex) {}
		@Override
		public void imageComplete(ImageWriter source) {
			setProgress(100);
		}
		@Override
		public void thumbnailStarted(ImageWriter source, int imageIndex, int thumbnailIndex) {}
		@Override
		public void thumbnailProgress(ImageWriter source, float percentageDone) {}
		@Override
		public void thumbnailComplete(ImageWriter source) {}
		@Override
		public void writeAborted(ImageWriter source) {
			cancel(false);
		}
		@Override
		public void imageProgress(ImageWriter source, float percentageDone) {
			setProgress((int) percentageDone);
		}
		@Override
		public void warningOccurred(ImageWriter source, int imageIndex, String warning) {
			println(warning);
		}
	}

	public ExportWorker(BufferedImage fimg, File file,
			ImageWriter imageWriter, ImageWriteParam imageWriteParam,
			Component parent) {
		this.fimg = fimg;
		this.file = file;
		this.imageWriter = imageWriter;
		this.imageWriteParam = imageWriteParam;
	}

	@Override
	protected Void doInBackground() throws Exception {
		Listener listener = new Listener();
		imageWriter.addIIOWriteProgressListener(listener);
		imageWriter.addIIOWriteWarningListener(listener);
		IIOImage iioImage = new IIOImage(fimg, null, null);
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(file)) {
			imageWriter.setOutput(ios);
			imageWriter.write(null, iioImage, imageWriteParam);
			ios.flush();
		} finally {
			imageWriter.dispose();
		}
		return null;
	}

	public void cancel() {
		imageWriter.abort();
	}
}

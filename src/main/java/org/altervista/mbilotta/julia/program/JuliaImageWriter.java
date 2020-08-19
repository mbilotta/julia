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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.altervista.mbilotta.julia.IntermediateImage;
import org.altervista.mbilotta.julia.Representation;
import org.altervista.mbilotta.julia.program.parsers.Parameter;
import org.altervista.mbilotta.julia.program.parsers.Plugin;


public class JuliaImageWriter extends BlockingSwingWorker<Void> {
	
	private final File file;
	private final Application.Image metadata;
	private final IntermediateImage iimg;

	public JuliaImageWriter(File file, Application.Image metadata, IntermediateImage iimg) {
		assert file != null;
		assert metadata != null;
		this.file = file;
		this.metadata = metadata;
		this.iimg = iimg;
	}

	public File getFile() {
		return file;
	}

	public void write() throws IOException, ReflectiveOperationException {
		try (FileOutputStream fos = new FileOutputStream(file);
				BufferedOutputStream bos = new BufferedOutputStream(fos, 2048);
				ZipOutputStream zos = new ZipOutputStream(bos)) {
			publishToGui("number factory...");
			zos.putNextEntry(new ZipEntry("numberFactory"));
			writeEntry(zos, metadata.getNumberFactoryInstance());
			if (isCancelled()) return;
			setGuiProgress(12);

			publishToGui("formula...");
			zos.putNextEntry(new ZipEntry("formula"));
			writeEntry(zos, metadata.getFormulaInstance());
			if (isCancelled()) return;
			setGuiProgress(24);

			publishToGui("representation...");
			zos.putNextEntry(new ZipEntry("representation"));
			writeEntry(zos, metadata.getRepresentationInstance());
			if (isCancelled()) return;
			setGuiProgress(36);

			publishToGui("rectangle...");
			zos.putNextEntry(new ZipEntry("rectangle"));
			ObjectOutputStream oos = new ObjectOutputStream(zos);
			oos.writeObject(metadata.getRectangle());
			oos.writeBoolean(metadata.getForceEqualScales());
			oos.flush();
			if (isCancelled()) return;
			setGuiProgress(48);

			JuliaSetPoint juliaSetPoint = metadata.getJuliaSetPoint();
			if (juliaSetPoint != null) {
				publishToGui("julia set point...");
				zos.putNextEntry(new ZipEntry("juliaSetPoint"));
				oos = new ObjectOutputStream(zos);
				oos.writeObject(juliaSetPoint);
				oos.flush();
				if (isCancelled()) return;
			}
			setGuiProgress(60);
			
			if (iimg != null) {
				publishToGui("intermediate image...");
				Representation representation = (Representation) metadata.getRepresentationInstance().create();
				zos.putNextEntry(new ZipEntry("intermediateImage"));
				oos = new ObjectOutputStream(zos);
				representation.writeIntermediateImage(iimg, oos);
				oos.flush();
				if (isCancelled()) return;
			}
			setGuiProgress(100);
		}
	}

	@Override
	protected Void doInBackground() throws Exception {
		write();
		return null;
	}

	private static void writeEntry(OutputStream os, PluginInstance<?> pluginInstance)
			throws IOException {
		Plugin plugin = pluginInstance.getPlugin();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(plugin.getId());
		for (Parameter<?> parameter : plugin.getParameters()) {
			oos.writeObject(parameter.getId());
			oos.writeObject(pluginInstance.getParameterValue(parameter));
		}
		oos.writeObject(null);
		oos.flush();
	}
}

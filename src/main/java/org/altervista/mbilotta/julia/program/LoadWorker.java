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

import static org.altervista.mbilotta.julia.Utilities.readNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.altervista.mbilotta.julia.IntermediateImage;
import org.altervista.mbilotta.julia.Out;
import org.altervista.mbilotta.julia.Printer;
import org.altervista.mbilotta.julia.Representation;
import org.altervista.mbilotta.julia.program.parsers.FormulaPlugin;
import org.altervista.mbilotta.julia.program.parsers.NumberFactoryPlugin;
import org.altervista.mbilotta.julia.program.parsers.RepresentationPlugin;


public class LoadWorker extends BlockingSwingWorker<Void> {

	private final File file;
	private final Application application;
	private final Printer errorOutput;
	private final boolean loadIntermediateImage;
	private int errorCount = 0;
	private int fatalCount = 0;
	private IntermediateImage iimg;

	private final Out<Integer> errorCountOut = new Out<Integer>() {
		@Override
		public Integer get() {
			return errorCount;
		}
		@Override
		public void set(Integer value) {
			errorCount = value;
		}
	};
	private final Out<Integer> fatalCountOut = new Out<Integer>() {
		public Integer get() {
			return fatalCount;
		}
		public void set(Integer value) {
			fatalCount = value;
		}
	};
	
	private PluginInstance<NumberFactoryPlugin> numberFactoryInstance;
	private PluginInstance<FormulaPlugin> formulaInstance;
	private PluginInstance<RepresentationPlugin> representationInstance;
	private Rectangle rectangle;
	private final Out<Boolean> forceEqualScalesOut = Out.newOut();
	private final Out<JuliaSetPoint> juliaSetPointOut = Out.newOut();

	public LoadWorker(File file, Application application, Printer errorOutput, boolean loadIntermediateImage) {
		assert file != null;
		assert application != null;
		this.file = file;
		this.application = application;
		this.errorOutput = errorOutput == null ? Printer.nullPrinter() : errorOutput;
		this.loadIntermediateImage = loadIntermediateImage;
	}

	public File getFile() {
		return file;
	}

	public Printer getErrorOutput() {
		return errorOutput;
	}

	public NumberFactoryPlugin getNumberFactory() {
		return numberFactoryInstance != null ? numberFactoryInstance.getPlugin() : null;
	}

	public PluginInstance<NumberFactoryPlugin> getNumberFactoryInstance() {
		return numberFactoryInstance;
	}

	public FormulaPlugin getFormula() {
		return formulaInstance != null ? formulaInstance.getPlugin() : null;
	}

	public PluginInstance<FormulaPlugin> getFormulaInstance() {
		return formulaInstance;
	}

	public RepresentationPlugin getRepresentation() {
		return representationInstance != null ? representationInstance.getPlugin() : null;
	}

	public PluginInstance<RepresentationPlugin> getRepresentationInstance() {
		return representationInstance;
	}

	public Rectangle getRectangle() {
		return rectangle;
	}

	public boolean getForceEqualScales() {
		return forceEqualScalesOut.get();
	}

	public boolean getForceEqualScales(boolean fallbackValue) {
		return forceEqualScalesOut.get(fallbackValue);
	}

	public JuliaSetPoint getJuliaSetPoint() {
		return juliaSetPointOut.get();
	}

	public JuliaSetPoint getJuliaSetPoint(JuliaSetPoint fallbackValue) {
		return juliaSetPointOut.get(fallbackValue);
	}

	public IntermediateImage getIntermediateImage() {
		return iimg;
	}

	public boolean hasErrors() {
		return errorCount > 0;
	}

	public boolean hasFatalErrors() {
		return fatalCount > 0;
	}

	public boolean hasHeader() {
		return formulaInstance != null &&
				representationInstance != null &&
				rectangle != null &&
				forceEqualScalesOut.isSet() &&
				juliaSetPointOut.isSet();
	}

	@Override
	protected Void doInBackground() throws Exception {
		try (ZipFile zipFile = new ZipFile(file)) {

			publish("number factory...");
			numberFactoryInstance = readNumberFactory(zipFile);
			if (isCancelled()) return null;
			setProgress(12);

			publish("formula...");
			formulaInstance = readFormula(zipFile);
			if (isCancelled()) return null;
			setProgress(24);

			publish("representation...");
			representationInstance = readRepresentation(zipFile);
			if (isCancelled()) return null;
			setProgress(36);

			publish("rectangle...");
			ZipEntry entry = zipFile.getEntry("rectangle");
			if (entry == null) {
				addFatalError(null);
				errorOutput.println("could not find rectangle zip entry.");
			} else {
				try (InputStream is = zipFile.getInputStream(entry);
						ObjectInputStream ois = new ObjectInputStream(is)) {
					rectangle = readNonNull(ois, "rectangle", Rectangle.class);
					forceEqualScalesOut.set(ois.readBoolean());
				} catch (ClassNotFoundException | IOException e) {
					addFatalError(entry);
					errorOutput.printStackTrace(e);
				}
				if (isCancelled()) return null;
			}
			setProgress(48);

			entry = zipFile.getEntry("juliaSetPoint");
			if (entry == null) {
				juliaSetPointOut.set(null);
			} else {
				publish("julia set point...");
				try (InputStream is = zipFile.getInputStream(entry);
						ObjectInputStream ois = new ObjectInputStream(is)) {
					JuliaSetPoint juliaSetPoint = readNonNull(ois, "juliaSetPoint", JuliaSetPoint.class);
					juliaSetPointOut.set(juliaSetPoint);
				} catch (ClassNotFoundException | IOException e) {
					addFatalError(entry);
					errorOutput.printStackTrace(e);
				}
				if (isCancelled()) return null;
			}
			setProgress(60);

			if (loadIntermediateImage) {
				entry = zipFile.getEntry("intermediateImage");
				if (entry == null) {
					addFatalError(null);
					errorOutput.println("could not find intermediate image zip entry.");
				} else if (fatalCount == 0) {
					publish("intermediate image...");
					try {
						Representation representation = (Representation) representationInstance.create();
						try (InputStream is = zipFile.getInputStream(entry);
								ObjectInputStream ois = new ObjectInputStream(is)) {
							iimg = representation.readIntermediateImage(ois);
						} catch (ClassNotFoundException | IOException e) {
							addFatalError(entry);
							errorOutput.printStackTrace(e);
						}
					} catch (ReflectiveOperationException e) {
						addFatalError(entry);
						errorOutput.print("could not create instance ");
						representationInstance.printTo(errorOutput);
						errorOutput.print(". Cause: ");
						errorOutput.printStackTrace(e);
						representationInstance = null;
					}
					if (isCancelled()) return null;
				}
			}
			setProgress(100);

		} catch (IOException e) {
			addFatalError(null);
			errorOutput.printStackTrace(e);
		}

		return null;
	}

	private void addInfo(ZipEntry entry) {
		errorOutput.print("- ", file.getName());
		if (entry != null) errorOutput.print("/", entry.getName());
		errorOutput.print(" (info): ");
	}

	private void addError(ZipEntry entry) {
		errorOutput.print("- ", file.getName());
		if (entry != null) errorOutput.print("/", entry.getName());
		errorOutput.print(" (error): ");
		errorCount++;
	}

	private void addFatalError(ZipEntry entry) {
		errorOutput.print("- ", file.getName());
		if (entry != null) errorOutput.print("/", entry.getName());
		errorOutput.print(" (fatal): ");
		errorCount++;
		fatalCount++;
	}

	private PluginInstance<NumberFactoryPlugin> readNumberFactory(ZipFile zipFile) {
		ZipEntry entry = zipFile.getEntry("numberFactory");
		if (entry == null) {
			addError(null);
			errorOutput.println("could not find number factory zip entry.");
			return null;
		}
		
		try (InputStream is = zipFile.getInputStream(entry);
				ObjectInputStream ois = new ObjectInputStream(is)) {
			String id = readNonNull(ois, "id", String.class);
			NumberFactoryPlugin numberFactory = application.findNumberFactory(id);
			if (numberFactory == null) {
				addError(entry);
				errorOutput.println("unknown number factory \"", id, "\".");
				return null;
			}
			addInfo(entry);
			errorOutput.println("reading number factory \"", id, "\"...");
			
			return PluginInstance.readNumberFactory(numberFactory,
					ois,
					file.getName() + "/" + entry.getName(),
					errorOutput,
					errorCountOut, fatalCountOut);
		} catch (ClassNotFoundException | IOException e) {
			addError(entry);
			errorOutput.printStackTrace(e);
			return null;
		}
	}

	private PluginInstance<FormulaPlugin> readFormula(ZipFile zipFile) {
		ZipEntry entry = zipFile.getEntry("formula");
		if (entry == null) {
			addFatalError(null);
			errorOutput.println("could not find formula zip entry.");
			return null;
		}
		
		try (InputStream is = zipFile.getInputStream(entry);
				ObjectInputStream ois = new ObjectInputStream(is)) {
			String id = readNonNull(ois, "id", String.class);
			FormulaPlugin formula = application.findFormula(id);
			if (formula == null) {
				addFatalError(entry);
				errorOutput.println("unknown formula \"", id, "\".");
				return null;
			}
			addInfo(entry);
			errorOutput.println("reading formula \"", id, "\"...");
			
			return PluginInstance.readFormula(formula,
					ois,
					file.getName() + "/" + entry.getName(),
					errorOutput,
					errorCountOut, fatalCountOut);
		} catch (ClassNotFoundException | IOException e) {
			addFatalError(entry);
			errorOutput.printStackTrace(e);
			return null;
		}
	}

	private PluginInstance<RepresentationPlugin> readRepresentation(ZipFile zipFile) {
		ZipEntry entry = zipFile.getEntry("representation");
		if (entry == null) {
			addFatalError(null);
			errorOutput.println("could not find representation zip entry.");
			return null;
		}
		
		try (InputStream is = zipFile.getInputStream(entry);
				ObjectInputStream ois = new ObjectInputStream(is)) {
			String id = readNonNull(ois, "id", String.class);
			RepresentationPlugin representation = application.findRepresentation(id);
			if (representation == null) {
				addFatalError(entry);
				errorOutput.println("unknown representation \"", id, "\".");
				return null;
			}
			addInfo(entry);
			errorOutput.println("reading representation \"", id, "\"...");
			
			return PluginInstance.readRepresentation(representation,
					ois,
					file.getName() + "/" + entry.getName(),
					errorOutput,
					errorCountOut, fatalCountOut);
		} catch (ClassNotFoundException | IOException e) {
			addFatalError(entry);
			errorOutput.printStackTrace(e);
			return null;
		}
	}
}

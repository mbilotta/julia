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

package org.altervista.mbilotta.julia.program.cli;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.altervista.mbilotta.julia.Consumer;
import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.Formula;
import org.altervista.mbilotta.julia.IntermediateImage;
import org.altervista.mbilotta.julia.NumberFactory;
import org.altervista.mbilotta.julia.Production;
import org.altervista.mbilotta.julia.Representation;
import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.math.CoordinateTransform;
import org.altervista.mbilotta.julia.program.Application;
import org.altervista.mbilotta.julia.program.ExecutionObserver;
import org.altervista.mbilotta.julia.program.JuliaExecutorService;
import org.altervista.mbilotta.julia.program.JuliaImageReader;
import org.altervista.mbilotta.julia.program.JuliaSetPoint;
import org.altervista.mbilotta.julia.program.Loader;
import org.altervista.mbilotta.julia.program.PluginInstance;
import org.altervista.mbilotta.julia.program.Preferences;
import org.altervista.mbilotta.julia.program.Rectangle;
import org.altervista.mbilotta.julia.program.Timer;
import org.altervista.mbilotta.julia.program.JuliaImageWriter;
import org.altervista.mbilotta.julia.program.parsers.DescriptorParser;
import org.altervista.mbilotta.julia.program.parsers.FormulaPlugin;
import org.altervista.mbilotta.julia.program.parsers.NumberFactoryPlugin;
import org.altervista.mbilotta.julia.program.parsers.Parameter;
import org.altervista.mbilotta.julia.program.parsers.Plugin;
import org.altervista.mbilotta.julia.program.parsers.RepresentationPlugin;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "generate",
	description = "Generate a fractal.",
	descriptionHeading = "%n",
	parameterListHeading = "%nParameters:%n",
	optionListHeading = "%nOptions:%n",
	sortOptions = false)
public class ImageGenerationCli implements Runnable {
	
	@ParentCommand
	MainCli mainCli;

	@Option(names = { "-W", "--width" }, description = "Width of the image in px.")
	Integer width;

	@Option(names = { "-H", "--height" }, description = "Height of the image in px.")
	Integer height;

	@Option(names = { "-t", "--producer-threads" }, paramLabel = "<numThreads>")
	Integer numOfProducersHint;

	@Option(names = { "--force-equal-scales" }, negatable = true)
	Boolean forceEqualScales;

	@Option(names = { "-n", "--number-factory" })
	String numberFactoryId;

	@Option(names = { "-f", "--formula" })
	String formulaId;

	@Option(names = { "-r", "--representation" })
	String representationId;

	@Option(names = { "-o", "--output" }, paramLabel = "OUTPUT_PATH", required = true,
		description = "Output file path.")
	Path outputPath;

	@Option(names = "--no-iimg-output")
	boolean noIntermImgOutput;

	@Option(names = { "-x", "--replace-existing" },
		description = "Use this flag to eventually replace an already existing file at output path.")
	boolean replaceExisting;

	@Option(names = { "-i", "--input" }, paramLabel = "INPUT_PATH",
		description = "Input file path (JIM format).")
	Path inputPath;

	@Option(names = "--no-iimg-input")
	boolean noIntermImgInput;

	@Parameters
	List<String> parameters;

	private PluginInstance<NumberFactoryPlugin> numberFactoryInstance;
	private PluginInstance<FormulaPlugin> formulaInstance;
	private PluginInstance<RepresentationPlugin> representationInstance;
	private Rectangle rectangle;
	private JuliaSetPoint juliaSetPoint;

	private Timer timer = new Timer();

	private JuliaExecutorService executorService;

	private JuliaImageReader reader;

	@Override
	public void run() {
		try {
			Utilities.debug.setEnabled(mainCli.debugOutputEnabled);
			Utilities.println("Loading profile...");
			Loader loader = new Loader(mainCli);
			loader.loadProfile();
			// I can't see the need of keeping this lock active
			loader.getPreferencesFile().close();

			warnOfParsingProblems(loader);

			if (!loader.hasMinimalSetOfPlugins()) {
				warnOfInsufficientPluginsInstalled(loader);
				return;
			}

			if (inputPath != null) {
				Utilities.println("Reading input file...");
				reader = new JuliaImageReader(inputPath.toFile(), loader, Utilities.err, true);
				reader.setGuiRunning(false);
				reader.readHeader();

				if (reader.hasFatalErrors()) {
					return;
				}

				numberFactoryInstance = Utilities.safelyClone(reader.getNumberFactoryInstance());
				formulaInstance = Utilities.safelyClone(reader.getFormulaInstance());
				representationInstance = Utilities.safelyClone(reader.getRepresentationInstance());
				juliaSetPoint = reader.getJuliaSetPoint();
				rectangle = reader.getRectangle();
				if (forceEqualScales == null) {
					forceEqualScales = reader.getForceEqualScales();
				}
			}

			if (numberFactoryId != null || numberFactoryInstance == null) {
				// Find matching number factory
				List<NumberFactoryPlugin> matchingNumberFactories = findMatchingPlugins(loader.getAvailableNumberFactories(), numberFactoryId);
				if (matchingNumberFactories.isEmpty()) {
					warnOfMatchNotFound("number factory", numberFactoryId);
					return;
				}
				if (matchingNumberFactories.size() > 1) {
					warnOfMultipleMatches(matchingNumberFactories, "number factories", numberFactoryId);
					return;
				}
				numberFactoryInstance = new PluginInstance<NumberFactoryPlugin>(matchingNumberFactories.get(0));
			}

			if (formulaId != null || formulaInstance == null) {
				// Find matching formula
				List<FormulaPlugin> matchingFormulas = findMatchingPlugins(loader.getAvailableFormulas(), formulaId);
				if (matchingFormulas.isEmpty()) {
					warnOfMatchNotFound("formula", formulaId);
					return;
				}
				if (matchingFormulas.size() > 1) {
					warnOfMultipleMatches(matchingFormulas, "formulas", formulaId);
					return;
				}
				formulaInstance = new PluginInstance<FormulaPlugin>(matchingFormulas.get(0));
			}

			if (representationId != null || representationInstance == null) {
				// Find matching representation
				List<RepresentationPlugin> matchingRepresentations = findMatchingPlugins(loader.getAvailableRepresentations(), representationId);
				if (matchingRepresentations.isEmpty()) {
					warnOfMatchNotFound("representation", representationId);
					return;
				}
				if (matchingRepresentations.size() > 1) {
					warnOfMultipleMatches(matchingRepresentations, "representations", representationId);
					return;
				}
				representationInstance = new PluginInstance<RepresentationPlugin>(matchingRepresentations.get(0));
			}

			parseParameters();

			// Assign default rectangle
			if (rectangle == null) {
				if (juliaSetPoint == null) {
					rectangle = formulaInstance.getPlugin().getDefaultMandelbrotSetRectangle();
				} else {
					rectangle = formulaInstance.getPlugin().getDefaultJuliaSetRectangle();
				}
			}
			if (forceEqualScales == null) {
				forceEqualScales = true;
			}

			// Infer output format
			File outputFile = outputPath.toFile();
			String outputFileName = outputFile.getName();
			String outputExtension = outputFileName.substring(outputFileName.lastIndexOf('.'));
			String outputFormat = outputExtension.substring(1);

			// Read intermediate image if available
			IntermediateImage intermediateImage = null;
			if (reader != null) {
				if ( !noIntermImgInput || !(outputFormat.equalsIgnoreCase("jim") && noIntermImgOutput) ) {
					if (reader.hasIntermediateImage()) {
						boolean canUseIntermediateImage = numberFactoryInstance.equals(reader.getNumberFactoryInstance())
								&& formulaInstance.equals(reader.getFormulaInstance())
								&& representationInstance.equalsIgnorePreviewables(reader.getRepresentationInstance())
								&& Objects.equals(juliaSetPoint, reader.getJuliaSetPoint())
								&& rectangle.equals(reader.getRectangle())
								&& forceEqualScales.equals(reader.getForceEqualScales());

						if (canUseIntermediateImage) {
							reader.readIntermediateImage();
							if (reader.hasFatalErrors()) {
								return;
							}
							intermediateImage = reader.getIntermediateImage();
							if (width == null) {
								width = intermediateImage.getWidth();
							}
							if (height == null) {
								height = intermediateImage.getHeight();
							}
							canUseIntermediateImage = width.equals(intermediateImage.getWidth()) && height.equals(intermediateImage.getHeight());
						}

						if (!canUseIntermediateImage) {
							intermediateImage = null;
							reader.disposeIntermediateImage();
							Utilities.println("Warning: the requested image cannot be computed starting from ", inputPath);
						}
					} else {
						Utilities.println("Warning: no intermediate image was found in ", inputPath);
					}
				}
				reader.close();
			}

			Preferences preferences = loader.getPreferences();
			if (width == null) {
				width = preferences.getImageWidth();
			}
			if (height == null) {
				height = preferences.getImageHeight();
			}
			if (numOfProducersHint == null) {
				numOfProducersHint = preferences.getNumOfProducerThreads();
			}

			// Instantiate NumberFactory
			NumberFactory numberFactory = (NumberFactory) numberFactoryInstance.create();
			// Instantiate Formula
			Formula formula = (Formula) formulaInstance.create(numberFactory);
			// Instantiate Representation
			Representation representation = (Representation) representationInstance.create(numberFactory);

			if ( !(outputFormat.equalsIgnoreCase("jim") && noIntermImgOutput) ) {
				// Instantiate IntermediateImage
				if (intermediateImage == null) {
					intermediateImage = representation.createIntermediateImage(
						width, height,
						Math.min(Runtime.getRuntime().availableProcessors(), numOfProducersHint)
					);
				}

				// Run computation
				if (!intermediateImage.isComplete()) {
					// Instantiate CoordinateTransform
					CoordinateTransform coordinateTransform = rectangle.createCoordinateTransform(width, height, forceEqualScales, numberFactory);

					// Instantiate Production
					Production production = representation.createProduction(
						intermediateImage, numberFactory, formula,
						coordinateTransform,
						juliaSetPoint != null ? juliaSetPoint.toComplex(numberFactory) : null);

					// Pre-rendering output file check
					if (!replaceExisting && outputFile.exists()) {
						warnOfOutputFileAlreadyExisting();
						return;
					}

					Utilities.print("Rendering intermediate image...");
					Utilities.flush();
					int numOfProducers = production.getNumOfProducers();
					executorService = new JuliaExecutorService(0, 10l, TimeUnit.MINUTES);
					CountDownLatch done = new CountDownLatch(numOfProducers);
					timer.start();
					for (int i = 0; i < numOfProducers; i++) {
						Production.Producer producer = production.createProducer(i);
						executorService.submitAndObserve(producer, new ExecutionObserver() {
							@Override
							public void executionCancelled(Runnable target) {
								done.countDown();
							}

							@Override
							public void executionFinished(Runnable target) {
								done.countDown();
							}
						});
					}
					done.await();
					timer.stop();

					Utilities.println(" ", Utilities.formatDuration(timer.getElapsedTime()));
				}
			}

			if (outputFormat.equalsIgnoreCase("jim")) {
				// Write to file
				Utilities.println("Writing to output file...");
				if (canWriteTo(outputFile)) {
					Application.Image metadata = new Application.Image(
						numberFactoryInstance, formulaInstance, representationInstance,
						rectangle, forceEqualScales,
						juliaSetPoint
					);
					JuliaImageWriter jimWriter = new JuliaImageWriter(outputFile, metadata, intermediateImage);
					jimWriter.setGuiRunning(false);
					jimWriter.write();
				}
			} else if (intermediateImage.isComplete()) {
				Utilities.print("Rendering final image...");
				Utilities.flush();

				timer.start();

				// Instantiate Consumer
				Consumer consumer = representation.createConsumer(intermediateImage);

				// Compute final image
				BufferedImage finalImage = consumer.createFinalImage();
				consumer.consume(finalImage, null);

				timer.stop();
				Utilities.println(
					" ", Utilities.formatDuration(timer.getElapsedTime()),
					" (total: ", Utilities.formatDuration(timer.getTotalElapsedTime()), ")"
				);
				
				// Write to file
				Utilities.println("Writing to output file...");
				if (canWriteTo(outputFile)) {
					ImageIO.write(finalImage, outputFormat, outputFile);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (executorService != null) {
				executorService.shutdownNow();
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	@Override
	public String toString() {
		return "[" +
			", width=" + width +
			", height=" + height +
			", numOfProducersHint=" + numOfProducersHint +
			", forceEqualScales=" + forceEqualScales +
			", numberFactoryId=" + numberFactoryId +
			", formulaId=" + formulaId +
			", representationId=" + representationId +
			", outputPath=" + outputPath +
			", replaceExisting=" + replaceExisting +
			", parameters=" + parameters +
			"]";
	}

	private boolean canWriteTo(File file) throws IOException {
		if (!replaceExisting) {
			if (!file.createNewFile()) {
				warnOfOutputFileAlreadyExisting();
				return false;
			}
		}
		return true;
	}

	private void warnOfOutputFileAlreadyExisting() {
		Utilities.println("Error: cannot write to ", outputPath.toAbsolutePath(), " because a file already exists at that location. Add --replace-existing to overwrite that file.");
	}

	private void parseParameters() {
		if (parameters != null) {
			for (String parameter : parameters) {
				String[] sides = parameter.split("=", 2);
				String lSide = sides[0];
				String rSide = sides[1];
				switch (lSide) {
					case "r":
					case "rect":
					case "rectangle": parseRectangle(rSide); break;
					case "c":
					case "julia":
					case "juliaSet":
					case "juliaSetPoint": parseJuliaSetPoint(rSide); break;
					default: if (!parseAssignment(lSide, rSide)) throw new IllegalArgumentException("cannot perform assignment " + parameter); break;
				}
			}
		}
	}

	private void parseRectangle(String rectangleString) {
		if (rectangleString.equals("default")) {
			rectangle = null;
		} else {
			String[] components = rectangleString.split(",", 4);
			if (components.length < 4) {
				throw new IllegalArgumentException("cannot parse rectangle \"" + rectangleString + "\"");
			}
			Decimal re0 = new Decimal(components[0]);
			Decimal im0 = new Decimal(components[1]);
			Decimal re1 = new Decimal(components[2]);
			Decimal im1 = new Decimal(components[3]);
			rectangle = new Rectangle(re0, im0, re1, im1);    
		}
	}

	private void parseJuliaSetPoint(String juliaSetPointString) {
		if (juliaSetPointString.equals("default")) {
			juliaSetPoint = formulaInstance.getPlugin().getDefaultJuliaSetPoint();
		} else {
			String[] components = juliaSetPointString.split(",", 2);
			if (components.length < 2) {
				throw new IllegalArgumentException("cannot parse Julia set point \"" + juliaSetPointString + "\"");
			}
			Decimal re = new Decimal(components[0]);
			Decimal im = new Decimal(components[1]);
			juliaSetPoint = new JuliaSetPoint(re, im);
		}
	}

	private boolean parseAssignment(String lSide, String rSide) {
		String[] path = lSide.split("\\.", 2);
		List<PluginInstance<?>> targetInstances;
		String targetInstanceId = path[0];
		String targetParameterId = path[1];
		switch (targetInstanceId) {
			case "n":
			case "nf":
			case "numFact":
			case "numberFactory": targetInstances = Arrays.asList(numberFactoryInstance); break;
			case "f":
			case "formula": targetInstances = Arrays.asList(formulaInstance); break;
			case "r":
			case "repr":
			case "representation": targetInstances = Arrays.asList(representationInstance); break;
			case "*": targetInstances = Arrays.asList(numberFactoryInstance, formulaInstance, representationInstance); break;
			default: Utilities.println("Error: cannot parse token \"", targetInstanceId, "\"."); return false;
		}
		return assignTo(targetInstances, targetParameterId, rSide);
	}

	private boolean assignTo(List<PluginInstance<?>> targetInstances, String targetParameterId, String valueString) {
		final Predicate<? super Boolean> IDENTITY = a -> a;
		String[] path = targetParameterId.split("\\.", 2);
		if (path.length == 2 && path[0].equals("hint")) {
			if (path[1].equals("*")) {
				return targetInstances.stream()
					.map(targetInstance -> assignHintGroupTo(targetInstance, valueString))
					.anyMatch(IDENTITY);
			} else if (!path[1].isEmpty()) {
				return targetInstances.stream()
					.map(targetInstance -> assignHintToParameter(targetInstance, path[1], valueString))
					.anyMatch(IDENTITY);
			}
		} else if (path.length == 1 && !path[0].isEmpty()) {
			return targetInstances.stream()
				.map(targetInstance -> assignToParameter(targetInstance, path[0], valueString))
				.anyMatch(IDENTITY);
		}
		Utilities.println("Error: cannot parse token \"", targetParameterId, "\".");
		return false;
	}

	private boolean assignToParameter(PluginInstance<?> targetInstance, String targetParameterId, String valueString) {
		Plugin plugin = targetInstance.getPlugin();
		Parameter<?> targetParameter = plugin.getParameter(targetParameterId);
		if (targetParameter == null) {
			Utilities.println("Warning: parameter ", plugin.getFamily(), ".", targetParameterId, " does not exist.");
			return false;
		}
		Object value = targetParameter.parseValue(valueString);
		if (!targetParameter.acceptsValue(value)) {
			throw new IllegalArgumentException(plugin.getFamily() + "." + targetParameterId + " does not permit value " + valueString);
		}
		targetInstance.setParameterValue(targetParameter, value);
		return true;
	}

	private boolean assignHintToParameter(PluginInstance<?> targetInstance, String targetParameterId, String hintGroupName) {
		Plugin plugin = targetInstance.getPlugin();
		Parameter<?> targetParameter = plugin.getParameter(targetParameterId);
		if (targetParameter == null) {
			Utilities.println("Warning: parameter ", plugin.getFamily(), ".", targetParameterId, " does not exist.");
			return false;
		}
		List<Object> hintGroup = plugin.getHintGroup(hintGroupName);
		if (hintGroup == null) {
			if (hintGroupName.matches("[0-9]+")) {
				int hintIndex = Integer.valueOf(hintGroupName);
				if (targetParameter.hasHint(hintIndex)) {
					Object value = targetParameter.getHint(hintIndex);
					targetInstance.setParameterValue(targetParameter, value);
					return true;
				}
				Utilities.println("Warning: parameter ", plugin.getFamily(), ".", targetParameterId, " has no hint at index ", hintIndex, ".");
			} else {
				Utilities.println("Warning: ", plugin.getFamily(), " has no hint group named ", hintGroupName, ".");
			}
			return false;
		}
		Object value = hintGroup.get(targetParameter.getIndex());
		if (value != null) {
			targetInstance.setParameterValue(targetParameter, value);
			return true;
		}
		Utilities.println("Warning: parameter ", plugin.getFamily(), ".", targetParameterId, " is not touched by hint group named ", hintGroupName, ".");
		return false;
	}

	private boolean assignHintGroupTo(PluginInstance<?> targetInstance, String hintGroupName) {
		Plugin plugin = targetInstance.getPlugin();
		List<Parameter<?>> parameters = plugin.getParameters();
		List<Object> hintGroup = plugin.getHintGroup(hintGroupName);
		if (hintGroup == null) {
			if (hintGroupName.matches("[0-9]+")) {
				int hintIndex = Integer.valueOf(hintGroupName);
				boolean hintFound = false;
				for (Parameter<?> parameter : parameters) {
					if (parameter.hasHint(hintIndex)) {
						targetInstance.setParameterValue(parameter, parameter.getHint(hintIndex));
						hintFound = true;
					}
				}
				if (!hintFound) {
					Utilities.println("Warning: no parameter of ", plugin.getFamily(), " has a hint at index ", hintIndex, ".");
				}
				return hintFound;
			} else {
				Utilities.println("Warning: ", plugin.getFamily(), " has no hint group named ", hintGroupName, ".");
			}
			return false;
		}
		for (Parameter<?> parameter : parameters) {
			Object hint = hintGroup.get(parameter.getIndex());
			if (hint != null) {
				targetInstance.setParameterValue(parameter, hint);
			}
		}
		return true;
	}

	private static <P extends Plugin> List<P> findMatchingPlugins(List<P> plugins, String searchString) {
		if (searchString == null || searchString.isEmpty()) {
			return plugins;
		}

		List<P> matches = plugins.stream()
			.filter(p -> p.getId().contains(searchString))
			.collect(Collectors.toList());
		Optional<P> exactMatch = matches.stream()
			.filter(p -> p.getId().equals(searchString))
			.findFirst();
		if (exactMatch.isPresent()) {
			return Collections.singletonList(exactMatch.get());
		}
		return matches;
	}

	private static void warnOfParsingProblems(Loader loader) {
		if (loader.getParserOutput() != null) {
			Utilities.print("Warning: ");
			if (loader.getProblemCount(DescriptorParser.Problem.FATAL_ERROR) > 0) {
				Utilities.print("one or more plugins will not be available due to errors in their relative descriptors.");
			} else if (loader.getProblemCount(DescriptorParser.Problem.ERROR) > 0) {
				Utilities.print("one or more plugins will possibly miss feautures due to errors in their relative descriptors.");
			} else if (loader.getProblemCount(DescriptorParser.Problem.WARNING) > 0) {
				Utilities.print("one or more plugin descriptors were parsed with warnings.");
			}
			Utilities.println(" See ", loader.getProfile().getDescriptorParserOutputFile(), " for details.");
		}
	}

	private static void warnOfInsufficientPluginsInstalled(Loader loader) {
		Utilities.println("Error: at least 1 plugin for each of the 3 plugin families must be available in order to run the program.");
		Utilities.println("Details:");
		Utilities.println("- Available number factories: ", loader.getAvailableNumberFactories().size());
		Utilities.println("- Available formulas: ", loader.getAvailableFormulas().size());
		Utilities.println("- Available representations: ", loader.getAvailableRepresentations().size());
	}

	private static void warnOfMatchNotFound(String pluginName, String searchString) {
		Utilities.println("Error: cannot find ", pluginName, " matching search string \"", Objects.toString(searchString, ""), "\".");
	}

	private static void warnOfMultipleMatches(List<? extends Plugin> matches, String pluginName, String searchString) {
		Utilities.println("Error: multiple ", pluginName, " matching search string \"", Objects.toString(searchString, ""), "\":");
		matches.forEach(p -> {
			Utilities.println("- ", p.getId());
		});
	}
}
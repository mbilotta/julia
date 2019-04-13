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

import static org.altervista.mbilotta.julia.Utilities.append;
import static org.altervista.mbilotta.julia.Utilities.out;
import static org.altervista.mbilotta.julia.Utilities.print;
import static org.altervista.mbilotta.julia.Utilities.printStackTrace;
import static org.altervista.mbilotta.julia.Utilities.println;
import static org.altervista.mbilotta.julia.Utilities.read;
import static org.altervista.mbilotta.julia.Utilities.readNonNull;
import static org.altervista.mbilotta.julia.Utilities.toHexString;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;
import javax.xml.parsers.ParserConfigurationException;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.Gradient;
import org.altervista.mbilotta.julia.Out;
import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.program.LockedFile.CloseableHider;
import org.altervista.mbilotta.julia.program.gui.MessagePane;
import org.altervista.mbilotta.julia.program.gui.SplashScreen;
import org.altervista.mbilotta.julia.program.parsers.Author;
import org.altervista.mbilotta.julia.program.parsers.BinaryRelation;
import org.altervista.mbilotta.julia.program.parsers.ClasspathParser;
import org.altervista.mbilotta.julia.program.parsers.DescriptorParser;
import org.altervista.mbilotta.julia.program.parsers.DocumentationWriter;
import org.altervista.mbilotta.julia.program.parsers.FormulaPlugin;
import org.altervista.mbilotta.julia.program.parsers.NumberFactoryPlugin;
import org.altervista.mbilotta.julia.program.parsers.Parser;
import org.altervista.mbilotta.julia.program.parsers.Plugin;
import org.altervista.mbilotta.julia.program.parsers.RepresentationPlugin;
import org.altervista.mbilotta.julia.program.parsers.ValidationException;
import org.xml.sax.SAXException;


class Loader extends SwingWorker<Void, String> {

	protected final Profile profile;
	private LockedFile preferencesFile;
	private Preferences preferences;
	private List<NumberFactoryPlugin> availableNumberFactories;
	private List<FormulaPlugin> availableFormulas;
	private List<RepresentationPlugin> availableRepresentations;
	private DateFormat dateFormat;
	private String parserOutput;
	private int[] problemCount = new int[3];
	private final JuliaExecutorService executorService;

	private SplashScreen splashScreen;

	public Loader(Profile profile, JuliaExecutorService executorService) {
		this.profile = profile;
		this.executorService = executorService;
		executorService.execute(this);
	}

	@Override
	protected final Void doInBackground() throws Exception {
		dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		println("[On ", dateFormat.format(new Date()), "]");

		publish("Scanning profile...");
		println("Scanning profile ", profile.getRootDirectory(), "...");

		Out<Boolean> failure = Out.newOut(false);
		List<Path> descriptors = profile.scanForDescriptors(out, failure);
		if (descriptors.isEmpty()) {
			if (failure.get()) {
				println("...failure. Some descriptors may got discarded.");
			} else {
				println("...success.");
			}

			try {
				publish("Reading preferences...");
				println("Reading preferences from ", profile.getPreferencesFile(), "...");
				preferencesFile = new LockedFile(profile.getPreferencesFile(), true);
				preferences = readNonNull(preferencesFile.readObjectsFrom(),
						"preferences",
						Preferences.class);
				println("...success: ", preferences);
			} catch (IOException | ClassNotFoundException e) {
				preferences = new Preferences();
				print("...failure. Cause: ");
				printStackTrace(e);
				println("Default preferences will be used.");
			}
		} else {
			List<Path> otherFiles = new ArrayList<>(2 + descriptors.size() * 3);
			otherFiles.add(profile.getLocalizationPreferencesFile());
			otherFiles.add(profile.getDescriptorParserOutputFile());
			otherFiles.addAll(descriptors);
			for (Path descriptor : descriptors) {
				otherFiles.add(profile.getCacheFileFor(descriptor));
			}
			for (Path descriptor : descriptors) {
				otherFiles.add(profile.getDocumentationFileFor(descriptor));
			}

			Set<Path> descriptorSet =
					Collections.newSetFromMap(new IdentityHashMap<Path, Boolean>(descriptors.size()));
			descriptorSet.addAll(descriptors);

			if (failure.get()) {
				println("...failure. Some descriptors may got discarded.");
			} else {
				println("...success.");
			}

			publish("Locking profile...");
			println("Locking profile ", profile.getRootDirectory(), "...");

			lockProfile(profile.getPreferencesFile(),
					otherFiles,
					descriptorSet);

			println("...success.");
		}
		
		publish("Loading user interface...");

		return null;
	}

	@Override
	protected void process(List<String> chunks) {
		if (splashScreen == null) {
			splashScreen = Application.getSplashScreen();
		}
		splashScreen.setText(chunks.get(chunks.size() - 1));
		splashScreen.setProgress(getProgress());
	}

	@Override
	protected void done() {
		if (splashScreen == null) {
			splashScreen = Application.getSplashScreen();
		}

		try {
			get();
			if (parserOutput != null) {
				if (getProblemCount(DescriptorParser.Problem.FATAL_ERROR) > 0) {
					MessagePane.showErrorMessage(splashScreen,
							"Julia",
							"One or more plugins will not be available due to errors in their relative descriptors. See the details.",
							parserOutput);
				} else if (getProblemCount(DescriptorParser.Problem.ERROR) > 0) {
					MessagePane.showWarningMessage(splashScreen,
							"Julia",
							"One or more plugins will possibly miss feautures due to errors in their relative descriptors. See the details.",
							parserOutput);
				} else if (getProblemCount(DescriptorParser.Problem.WARNING) > 0) {
					MessagePane.showInformationMessage(splashScreen,
							"Julia",
							"One or more plugin descriptors were parsed with warnings. See the details.",
							parserOutput);
				}
				parserOutput = null;
			}
			splashScreen = null;
			Application application = new Application(this);
			application.startUp();
		} catch (InterruptedException e) {
			executorService.shutdown();
			splashScreen.setVisible(false);
			splashScreen.dispose();
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			executorService.shutdown();
			MessagePane.showErrorMessage(splashScreen, "Julia", "Could not run the program.", e.getCause());
			splashScreen.setVisible(false);
			splashScreen.dispose();
		}
	}

	private void lockProfile(Path preferencesFile,
			List<Path> otherFiles,
			Set<Path> descriptors) throws FileAlreadyLockedException, Exception {

		List<LockedFile> lockedProfile = new ArrayList<>(1 + otherFiles.size());
		try (CloseableHider<LockedFile> c =
				new CloseableHider<LockedFile>(new LockedFile(preferencesFile, false))) {
			lockedProfile.add(c.getCloseable());
			lockProfileImpl(otherFiles,
					descriptors,
					lockedProfile);
			c.setCloseable(null);
		}
	}

	private void lockProfileImpl(List<Path> otherFiles,
			Set<Path> descriptorFiles,
			List<LockedFile> lockedProfile) throws Exception {

		if (otherFiles.isEmpty()) {
			loadProfile(lockedProfile);
			return;
		}

		Path path = otherFiles.get(0);
		boolean sharedlyLocked = descriptorFiles.contains(path);
		try (LockedFile lockedFile = new LockedFile(path, sharedlyLocked)) {
			lockedProfile.add(lockedFile);
			lockProfileImpl(otherFiles.subList(1, otherFiles.size()),
					descriptorFiles,
					lockedProfile);
		}
	}

	private void addAvailablePlugin(Plugin plugin) {
		switch (plugin.getFamily()) {
		case numberFactory:  availableNumberFactories.add((NumberFactoryPlugin) plugin); break;
		case formula:		 availableFormulas.add((FormulaPlugin) plugin); break;
		case representation: availableRepresentations.add((RepresentationPlugin) plugin); break;
		default: throw new AssertionError(plugin.getFamily());
		}
	}

	private Classpath parseClasspathFile() throws IOException, InterruptedException {
		try (LockedFile classpathFile = new LockedFile(profile.getClasspathFile(), true)) {
			try {
				publish("Parsing classpath...");
				println("Constructing classpath parser...");
				Parser<Classpath> parser = new ClasspathParser(profile);
				println("...success.");

				StringBuilder parserOutputBuilder = new StringBuilder();
				append(parserOutputBuilder, "[On ", dateFormat.format(new Date()), "]", System.lineSeparator());
				try {
					println("Parsing classpath...");
					Classpath rv = parser.parse(classpathFile, parserOutputBuilder);
					if (rv == null) {
						println("...failure. Classpath contains errors. Default classpath will be used.");
					} else if (parser.getErrorCount() > 0) {
						println("...failure. Classpath contains errors. Some entries were discarded.");
					} else {
						println("...success.");
					}
					return rv;
				} catch (SAXException | IOException | ValidationException e) {
					print("...failure. Cause: ");
					printStackTrace(e);
					println("Default classpath will be used.");
					return null;
				} finally {
					try (LockedFile parserOutputFile = new LockedFile(profile.getClasspathParserOutputFile(), false)) {
						try {
							println("Writing classpath parser output to ", parserOutputFile, "...");
							Writer w = parserOutputFile.writeCharsTo(true);
							w.write(parserOutputBuilder.toString());
							w.write(System.lineSeparator());
							w.flush();
							println("...success.");
						} catch (IOException e) {
							print("...failure. Cause: ");
							printStackTrace(e);
						}
					}
				}
			} catch (SAXException | ParserConfigurationException e) {
				print("...failure. Cause: ");
				printStackTrace(e);
				println("Default classpath will be used.");
				return null;
			}
		} catch (NoSuchFileException e) {
			return null;
		}
	}

	private void loadProfile(List<LockedFile> lockedProfile) throws Exception {
		println("...success.");

		int offset = 0;
		preferencesFile = lockedProfile.get(offset++);
		LockedFile localizationPreferencesFile = lockedProfile.get(offset++);
		LockedFile parserOutputFile = lockedProfile.get(offset++);
		final int numOfDescriptors = (lockedProfile.size() - offset) / 3;
		List<LockedFile> descriptors = lockedProfile.subList(offset, offset += numOfDescriptors);
		List<LockedFile> cacheFiles = lockedProfile.subList(offset, offset += numOfDescriptors);
		List<LockedFile> documentationFiles = lockedProfile.subList(offset, offset += numOfDescriptors);
		assert offset == lockedProfile.size();

		Classpath classpath = parseClasspathFile();
		if (classpath == null) {
			classpath = profile.createDefaultClasspath(descriptors);
		}

		publish("Inspecting classpath...");
		println("Inspecting classpath...");
		Out<Boolean> failure = Out.newOut(false);
		ClassLoader classLoader = classpath.createClassLoader(profile, Utilities.out, failure);
		if (failure.get()) {
			println("...failure. Some paths may got discarded.");
		} else {
			println("...success.");
		}

		StringBuilder parserOutputBuilder = null;

		Cache<Author> authorCache = new Cache<>();
		Cache<Decimal> decimalCache = new Cache<>();
		Cache<Color> colorCache = new Cache<>();
		Cache<Gradient> gradientCache = new Cache<>();

		MessageDigest md = MessageDigest.getInstance("MD5");
		Buffer buffer = null;
		BinaryRelation<String> localizationPreferences = null;
		DescriptorParser parser = null;
		boolean parserInstantiationFailed = false;
		availableNumberFactories = new ArrayList<>(numOfDescriptors);
		availableFormulas = new ArrayList<>(numOfDescriptors);
		availableRepresentations = new ArrayList<>(numOfDescriptors);
		
		for (int k = 0; k < numOfDescriptors; k++) {
			int progress = (int) (k * 100f / numOfDescriptors);
			setProgress(progress > 100 ? 100 : progress);

			LockedFile descriptor = descriptors.get(k);
			LockedFile cacheFile = cacheFiles.get(k);
			LockedFile documentationFile = documentationFiles.get(k);

			publish("Reading descriptor " + descriptor + "...");
			try {
				println("Reading descriptor ", descriptor, "...");
				buffer = Buffer.readFully(descriptor, buffer, md);
			} catch (IOException e) {
				print("...failure. Cause: ");
				printStackTrace(e);
				println("Descriptor ", descriptor, " discarded.");
				continue;
			}

			byte[] newChecksum = md.digest();
			println("...success. ", buffer.size(), " bytes read. MD5 checksum is ", new Checksum(newChecksum), ".");

			publish("Reading descriptor cache " + cacheFile + "...");
			Plugin plugin = null;
			try {
				println("Reading descriptor cache ", cacheFile, "...");
				ObjectInputStream ois = new JuliaObjectInputStream(cacheFile.readBytesFrom(),
						classLoader,
						authorCache, decimalCache, colorCache, gradientCache);
				println("...reading cached checksum...");
				byte[] oldChecksum = new byte[ois.readInt()];
				ois.readFully(oldChecksum);
				if (Arrays.equals(newChecksum, oldChecksum)) {
					println("...checksums match! Continue reading...");
					plugin = read(ois, "plugin", Plugin.class);
					if (plugin != null) {
						println("...success. Plugin successfully retrieved from descriptor cache: ", plugin, ".");
					} else {
						println("...descriptor was previously parsed with errors. Descriptor ", descriptor, " discarded.");
						continue;
					}
				} else {
					println("...checksums differ. Cached checksum is ", new Checksum(oldChecksum),
							". Descriptor ", descriptor, " will be reparsed.");
				}
			} catch (IOException | ClassNotFoundException e) {
				print("...failure. Cause: ");
				printStackTrace(e);
				println("Descriptor ", descriptor, " will be reparsed.");
			}
			
			if (plugin != null) {
				addAvailablePlugin(plugin);
			} else if (parserInstantiationFailed) {
				println("Descriptor ", descriptor, " discarded because there is no parser.");
			} else {
				if (parser == null) {
					publish("Reading localization preferences...");
					try {
						println("Reading localization preferences from ", localizationPreferencesFile, "...");
						localizationPreferences = readNonNull(localizationPreferencesFile.readObjectsFrom(),
								"localizationPreferences",
								BinaryRelation.class);
						if (localizationPreferences.hasElementType(String.class)) {
							println("...success: ", localizationPreferences);
						} else {
							localizationPreferences = new BinaryRelation<>();
							println("...failure. Localization preferences broken. Empty preferences will be used.");
						}
					} catch (IOException | ClassNotFoundException e) {
						localizationPreferences = new BinaryRelation<>();
						print("...failure. Cause: ");
						printStackTrace(e);
						println("Empty localization preferences will be used.");
					}

					publish("Constructing parser...");
					try {
						println("Constructing parser...");
						parser = new DescriptorParser(profile,
								classLoader,
								localizationPreferences,
								authorCache, decimalCache, colorCache, gradientCache);
						println("...success.");
						parserOutputBuilder = new StringBuilder();
						append(parserOutputBuilder, "[On ", dateFormat.format(new Date()), "]", System.lineSeparator());
					} catch (SAXException | ParserConfigurationException e) {
						parserInstantiationFailed = true;
						print("...failure. Cause: ");
						printStackTrace(e);
						println("Descriptor ", descriptor, " discarded because there is no parser.");
						continue;
					}
				}

				publish("Parsing descriptor " + descriptor + "...");
				boolean noExceptionsThrown = true;
				try {
					println("Parsing descriptor ", descriptor, "...");
					plugin = parser.parse(buffer, descriptor.getPath(), parserOutputBuilder);
				} catch (SAXException | ValidationException e) {
					noExceptionsThrown = false;
					print("...failure. Cause: ");
					printStackTrace(e);
					println("Descriptor discarded.");
				}

				problemCount[DescriptorParser.Problem.WARNING] += parser.getWarningCount();
				problemCount[DescriptorParser.Problem.ERROR] += parser.getErrorCount();
				problemCount[DescriptorParser.Problem.FATAL_ERROR] += parser.getFatalErrorCount();

				if (plugin != null) {
					println("...success. Plugin successfully retrieved after descriptor parsing: ", plugin, ".");
					addAvailablePlugin(plugin);

					publish("Writing documentation to " + documentationFile + "...");
					DocumentationWriter documentationWriter = parser.getDocumentationWriter();
					try {
						println("Writing documentation to ", documentationFile, "...");
						boolean success = documentationWriter.writeTo(documentationFile);
						println(success ? "...success." : "...failure.");
					} catch (IOException e) {
						print("...failure. Cause: ");
						printStackTrace(e);
					}
				} else if (noExceptionsThrown) {
					println("...failure. Descriptor contains errors. Descriptor discarded.");
				}

				publish("Writing descriptor cache " + cacheFile + "...");
				try {
					println("Writing descriptor cache ", cacheFile, "...");
					ObjectOutputStream oos = cacheFile.writeObjectsTo();
					oos.writeInt(newChecksum.length);
					oos.write(newChecksum);
					oos.writeObject(plugin);
					oos.flush();
					println("...success.");
				} catch (IOException e) {
					print("...failure. Cause: ");
					printStackTrace(e);
				}
			}
		}

		if (parser != null && parser.localizationPreferencesChanged()) {
			assert localizationPreferences != null;
			publish("Writing localization preferences...");
			try {
				println("Writing localization preferences to ", localizationPreferencesFile, "...");
				ObjectOutputStream oos = localizationPreferencesFile.writeObjectsTo();
				oos.writeObject(localizationPreferences);
				oos.flush();
				println("...success.");
			} catch (IOException e) {
				print("...failure. Cause: ");
				printStackTrace(e);
			}
		}

		if (parserOutputBuilder != null) {
			publish("Writing parser output...");
			parserOutput = parserOutputBuilder.toString();
			try {
				println("Writing parser output to ", parserOutputFile, "...");
				Writer w = parserOutputFile.writeCharsTo(true);
				w.write(parserOutput);
				w.write(System.lineSeparator());
				w.flush();
				println("...success.");
			} catch (IOException e) {
				print("...failure. Cause: ");
				printStackTrace(e);
			}
		}
		
		try {
			publish("Ensuring existence of julia.css...");
			println("Ensuring existence of julia.css...");
			boolean fileWritten = profile.installCss();
			if (fileWritten) {
				println("...success. File written.");
			} else {
				println("...success. File already existent.");
			}
		} catch (IOException e) {
			print("...failure. Cause: ");
			printStackTrace(e);
		}

		try {
			publish("Reading preferences...");
			println("Reading preferences from ", preferencesFile, "...");
			preferences = readNonNull(preferencesFile.readObjectsFrom(),
					"preferences",
					Preferences.class);
			println("...success: ", preferences);
		} catch (IOException | ClassNotFoundException e) {
			preferences = new Preferences();
			print("...failure. Cause: ");
			printStackTrace(e);
			println("Default preferences will be used.");
		}

		trimToSize(availableNumberFactories);
		trimToSize(availableFormulas);
		trimToSize(availableRepresentations);

		setProgress(100);
		publish("Releasing profile...");
		println("Releasing profile ", profile.getRootDirectory(), "...");
	}

	private static final class Checksum {
		private final byte[] checksum;

		public Checksum(byte[] checksum) {
			this.checksum = checksum;
		}

		public String toString() {
			return toHexString(checksum);
		}
	}

	private static void trimToSize(List<?> list) {
		if (list instanceof ArrayList) {
			((ArrayList<?>) list).trimToSize();
		}
	}

	public Profile getProfile() {
		return profile;
	}

	public LockedFile getPreferencesFile() {
		return preferencesFile;
	}

	public Preferences getPreferences() {
		return preferences;
	}

	public String getParserOutput() {
		return parserOutput;
	}

	public List<NumberFactoryPlugin> getAvailableNumberFactories() {
		if (availableNumberFactories == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(availableNumberFactories);
	}

	public List<FormulaPlugin> getAvailableFormulas() {
		if (availableFormulas == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(availableFormulas);
	}

	public List<RepresentationPlugin> getAvailableRepresentations() {
		if (availableRepresentations == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(availableRepresentations);
	}

	public int getProblemCount(int type) {
		return problemCount[type];
	}

	public JuliaExecutorService getExecutorService() {
		return executorService;
	}
}

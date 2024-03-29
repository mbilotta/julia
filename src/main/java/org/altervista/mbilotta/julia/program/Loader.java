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
import static org.altervista.mbilotta.julia.Utilities.debug;
import static org.altervista.mbilotta.julia.Utilities.read;
import static org.altervista.mbilotta.julia.Utilities.readNonNull;
import static org.altervista.mbilotta.julia.Utilities.toHexString;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.nio.file.Files;
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
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;
import javax.xml.parsers.ParserConfigurationException;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.Gradient;
import org.altervista.mbilotta.julia.Out;
import org.altervista.mbilotta.julia.program.LockedFile.CloseableHider;
import org.altervista.mbilotta.julia.program.cli.MainCli;
import org.altervista.mbilotta.julia.program.gui.MessagePane;
import org.altervista.mbilotta.julia.program.gui.SplashScreen;
import org.altervista.mbilotta.julia.program.parsers.AliasPlugin;
import org.altervista.mbilotta.julia.program.parsers.Author;
import org.altervista.mbilotta.julia.program.parsers.BinaryRelation;
import org.altervista.mbilotta.julia.program.parsers.ClassValidationException;
import org.altervista.mbilotta.julia.program.parsers.ClasspathParser;
import org.altervista.mbilotta.julia.program.parsers.DescriptorParser;
import org.altervista.mbilotta.julia.program.parsers.DocumentationWriter;
import org.altervista.mbilotta.julia.program.parsers.FormulaPlugin;
import org.altervista.mbilotta.julia.program.parsers.NumberFactoryPlugin;
import org.altervista.mbilotta.julia.program.parsers.Parser;
import org.altervista.mbilotta.julia.program.parsers.Plugin;
import org.altervista.mbilotta.julia.program.parsers.PluginFamily;
import org.altervista.mbilotta.julia.program.parsers.RepresentationPlugin;
import org.altervista.mbilotta.julia.program.parsers.DomValidationException;
import org.xml.sax.SAXException;


public class Loader extends SwingWorker<Void, String> {

	protected final Profile profile;
	private LockedFile preferencesFile;
	private Preferences preferences;
	private List<NumberFactoryPlugin> availableNumberFactories;
	private List<FormulaPlugin> availableFormulas;
	private List<RepresentationPlugin> availableRepresentations;
	private List<AliasPlugin> availableAliases;
	private DateFormat dateFormat;
	private String parserOutput;
	private int[] problemCount = new int[3];
	private final JuliaExecutorService executorService;
	private final boolean cacheRefreshRequested;
	private final boolean guiRunning;

	private SplashScreen splashScreen;

	public Loader(MainCli cli) {
		this(cli, null);
	}

	public Loader(MainCli cli, JuliaExecutorService executorService) {
		this.profile = cli.getProfilePath() == null ?
			Profile.getDefaultProfile() : new Profile(cli.getProfilePath());
		this.executorService = executorService;
		this.cacheRefreshRequested = cli.isCacheRefreshRequested();
		this.guiRunning = cli.isGuiRunning();
	}

	public boolean hasMinimalSetOfPlugins() {
		return availableNumberFactories != null && !availableFormulas.isEmpty()
			&& availableFormulas != null && !availableFormulas.isEmpty()
			&& availableRepresentations != null && !availableRepresentations.isEmpty();
	}

	public void loadGui() {
		executorService.execute(this);
	}

	@Override
	protected Void doInBackground() throws Exception {
		this.loadProfile();
		return null;
	}

	public final void loadProfile() throws Exception {
		dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		debug.println("[On ", dateFormat.format(new Date()), "]");

		publishToGui("Scanning profile...");
		debug.println("Scanning profile ", profile.getRootDirectory(), "...");

		Out<Boolean> failure = Out.newOut(false);
		List<Path> descriptors = profile.scanForDescriptors(debug, failure);
		if (descriptors.isEmpty()) {
			if (failure.get()) {
				debug.println("...failure. Some descriptors may got discarded.");
			} else {
				debug.println("...success.");
			}

			try {
				publishToGui("Reading preferences...");
				debug.println("Reading preferences from ", profile.getPreferencesFile(), "...");
				preferencesFile = new LockedFile(profile.getPreferencesFile(), true);
				preferences = new Preferences();
				Properties properties = preferencesFile.readPropertiesFrom();
				preferences.readFrom(properties);
				debug.println("...success: ", preferences);
			} catch (IOException e) {
				debug.print("...failure. Cause: ");
				debug.printStackTrace(e);
				readLegacyPreferencesFile();
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
				debug.println("...failure. Some descriptors may got discarded.");
			} else {
				debug.println("...success.");
			}

			publishToGui("Locking profile...");
			debug.println("Locking profile ", profile.getRootDirectory(), "...");

			lockProfile(profile.getPreferencesFile(),
					otherFiles,
					descriptorSet);

			debug.println("...success.");
		}
		
		publishToGui("Loading user interface...");
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

	private boolean readLegacyPreferencesFile() {
		debug.println("Reading preferences from ", profile.getLegacyPreferencesFile(), "...");
		try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(profile.getLegacyPreferencesFile()))) {
			preferences = readNonNull(ois, "preferences", Preferences.class);
			debug.println("...success: ", preferences);
			return true;
		} catch (IOException | ClassNotFoundException e) {
			preferences = new Preferences();
			debug.print("...failure. Cause: ");
			debug.printStackTrace(e);
			debug.println("Default preferences will be used.");	
			return false;
		}
	}

	private void setGuiProgress(int progress) {
		if (guiRunning) {
			setProgress(progress);
		}
	}

	private void publishToGui(String status) {
		if (guiRunning) {
			publish(status);
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
			loadLockedProfile(lockedProfile);
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
		case alias:			 availableAliases.add((AliasPlugin) plugin); break;
		default: throw new AssertionError(plugin.getFamily());
		}
	}

	private Classpath parseClasspathFile() throws IOException, InterruptedException {
		try (LockedFile classpathFile = new LockedFile(profile.getClasspathFile(), true)) {
			try {
				publishToGui("Parsing classpath...");
				debug.println("Constructing classpath parser...");
				Parser<Classpath> parser = new ClasspathParser(profile);
				debug.println("...success.");

				StringBuilder parserOutputBuilder = new StringBuilder();
				append(parserOutputBuilder, "[On ", dateFormat.format(new Date()), "]", System.lineSeparator());
				try {
					debug.println("Parsing classpath...");
					Classpath rv = parser.parse(classpathFile, parserOutputBuilder);
					if (rv == null) {
						debug.println("...failure. Classpath contains errors. Default classpath will be used.");
					} else if (parser.getErrorCount() > 0) {
						debug.println("...failure. Classpath contains errors. Some entries were discarded.");
					} else {
						debug.println("...success.");
					}
					return rv;
				} catch (SAXException | IOException | DomValidationException | ClassValidationException e) {
					debug.print("...failure. Cause: ");
					debug.printStackTrace(e);
					debug.println("Default classpath will be used.");
					return null;
				} finally {
					try (LockedFile parserOutputFile = new LockedFile(profile.getClasspathParserOutputFile(), false)) {
						try {
							debug.println("Writing classpath parser output to ", parserOutputFile, "...");
							Writer w = parserOutputFile.writeCharsTo(true);
							w.write(parserOutputBuilder.toString());
							w.write(System.lineSeparator());
							w.flush();
							debug.println("...success.");
						} catch (IOException e) {
							debug.print("...failure. Cause: ");
							debug.printStackTrace(e);
						}
					}
				}
			} catch (SAXException | ParserConfigurationException e) {
				debug.print("...failure. Cause: ");
				debug.printStackTrace(e);
				debug.println("Default classpath will be used.");
				return null;
			}
		} catch (NoSuchFileException e) {
			return null;
		}
	}

	private void loadLockedProfile(List<LockedFile> lockedProfile) throws Exception {
		debug.println("...success.");

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

		publishToGui("Inspecting classpath...");
		debug.println("Inspecting classpath...");
		Out<Boolean> failure = Out.newOut(false);
		ClassLoader classLoader = classpath.createClassLoader(profile, debug, failure);
		if (failure.get()) {
			debug.println("...failure. Some paths may got discarded.");
		} else {
			debug.println("...success.");
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
		availableAliases = new ArrayList<>(numOfDescriptors);
		
		for (int k = 0; k < numOfDescriptors; k++) {
			int progress = (int) (k * 100f / numOfDescriptors);
			setGuiProgress(progress > 100 ? 100 : progress);

			LockedFile descriptor = descriptors.get(k);
			LockedFile cacheFile = cacheFiles.get(k);
			LockedFile documentationFile = documentationFiles.get(k);

			publishToGui("Reading descriptor " + descriptor + "...");
			try {
				debug.println("Reading descriptor ", descriptor, "...");
				buffer = Buffer.readFully(descriptor, buffer, md);
			} catch (IOException e) {
				debug.print("...failure. Cause: ");
				debug.printStackTrace(e);
				debug.println("Descriptor ", descriptor, " discarded.");
				continue;
			}

			byte[] newChecksum = md.digest();
			debug.println("...success. ", buffer.size(), " bytes read. MD5 checksum is ", new Checksum(newChecksum), ".");

			Plugin plugin = null;
			if (this.cacheRefreshRequested) {
				debug.println("Ignoring descriptor cache ", cacheFile, " because a refresh has been requested...");
			} else if (guiRunning && documentationFile.isEmpty()) {
				debug.println("Ignoring descriptor cache ", cacheFile, " because the documentation file is empty...");
			} else {
				publishToGui("Reading descriptor cache " + cacheFile + "...");
				try {
					debug.println("Reading descriptor cache ", cacheFile, "...");
					ObjectInputStream ois = new JuliaObjectInputStream(cacheFile.readBytesFrom(),
							classLoader,
							authorCache, decimalCache, colorCache, gradientCache);
					debug.println("...reading cached checksum...");
					byte[] oldChecksum = new byte[ois.readInt()];
					ois.readFully(oldChecksum);
					if (Arrays.equals(newChecksum, oldChecksum)) {
						debug.println("...checksums match! Continue reading...");
						plugin = read(ois, "plugin", Plugin.class);
						if (plugin != null) {
							debug.println("...success. Plugin successfully retrieved from descriptor cache: ", plugin, ".");
						} else {
							debug.println("...descriptor was previously parsed with errors. Descriptor ", descriptor, " discarded.");
							continue;
						}
					} else {
						debug.println("...checksums differ. Cached checksum is ", new Checksum(oldChecksum),
								". Descriptor ", descriptor, " will be reparsed.");
					}
				} catch (IOException | ClassNotFoundException e) {
					debug.print("...failure. Cause: ");
					debug.printStackTrace(e);
					debug.println("Descriptor ", descriptor, " will be reparsed.");
				}
			}
			
			if (plugin != null) {
				addAvailablePlugin(plugin);
			} else if (parserInstantiationFailed) {
				debug.println("Descriptor ", descriptor, " discarded because there is no parser.");
			} else {
				if (parser == null) {
					publishToGui("Reading localization preferences...");
					try {
						debug.println("Reading localization preferences from ", localizationPreferencesFile, "...");
						localizationPreferences = readNonNull(localizationPreferencesFile.readObjectsFrom(),
								"localizationPreferences",
								BinaryRelation.class);
						if (localizationPreferences.hasElementType(String.class)) {
							debug.println("...success: ", localizationPreferences);
						} else {
							localizationPreferences = new BinaryRelation<>();
							debug.println("...failure. Localization preferences broken. Empty preferences will be used.");
						}
					} catch (IOException | ClassNotFoundException e) {
						localizationPreferences = new BinaryRelation<>();
						debug.print("...failure. Cause: ");
						debug.printStackTrace(e);
						debug.println("Empty localization preferences will be used.");
					}

					publishToGui("Constructing parser...");
					try {
						debug.println("Constructing parser...");
						parser = new DescriptorParser(profile,
								classLoader,
								localizationPreferences,
								authorCache, decimalCache, colorCache, gradientCache,
								guiRunning);
						debug.println("...success.");
						parserOutputBuilder = new StringBuilder();
						append(parserOutputBuilder, "[On ", dateFormat.format(new Date()), "]", System.lineSeparator());
					} catch (SAXException | ParserConfigurationException e) {
						parserInstantiationFailed = true;
						debug.print("...failure. Cause: ");
						debug.printStackTrace(e);
						debug.println("Descriptor ", descriptor, " discarded because there is no parser.");
						continue;
					}
				}

				publishToGui("Parsing descriptor " + descriptor + "...");
				boolean noExceptionsThrown = true;
				try {
					debug.println("Parsing descriptor ", descriptor, "...");
					plugin = parser.parse(buffer, descriptor.getPath(), parserOutputBuilder);
				} catch (SAXException | DomValidationException e) {
					noExceptionsThrown = false;
					debug.print("...failure. Cause: ");
					debug.printStackTrace(e);
					debug.println("Descriptor discarded.");
				}

				problemCount[DescriptorParser.Problem.WARNING] += parser.getWarningCount();
				problemCount[DescriptorParser.Problem.ERROR] += parser.getErrorCount();
				problemCount[DescriptorParser.Problem.FATAL_ERROR] += parser.getFatalErrorCount();

				if (plugin != null) {
					debug.println("...success. Plugin successfully retrieved after descriptor parsing: ", plugin, ".");
					addAvailablePlugin(plugin);

					if (plugin.getFamily() != PluginFamily.alias && (guiRunning || parser.getDocumentationLanguage() != null)) {
						publishToGui("Writing documentation to " + documentationFile + "...");
						DocumentationWriter documentationWriter = parser.getDocumentationWriter();
						try {
							debug.println("Writing documentation to ", documentationFile, "...");
							boolean success = documentationWriter.writeTo(documentationFile);
							debug.println(success ? "...success." : "...failure.");
						} catch (IOException e) {
							debug.print("...failure. Cause: ");
							debug.printStackTrace(e);
						}
					}
				} else if (noExceptionsThrown) {
					debug.println("...failure. Descriptor contains errors. Descriptor discarded.");
				}

				publishToGui("Writing descriptor cache " + cacheFile + "...");
				try {
					debug.println("Writing descriptor cache ", cacheFile, "...");
					ObjectOutputStream oos = cacheFile.writeObjectsTo();
					oos.writeInt(newChecksum.length);
					oos.write(newChecksum);
					oos.writeObject(plugin);
					oos.flush();
					debug.println("...success.");
				} catch (IOException e) {
					debug.print("...failure. Cause: ");
					debug.printStackTrace(e);
				}
			}
		}

		if (parser != null && parser.localizationPreferencesChanged()) {
			assert localizationPreferences != null;
			publishToGui("Writing localization preferences...");
			try {
				debug.println("Writing localization preferences to ", localizationPreferencesFile, "...");
				ObjectOutputStream oos = localizationPreferencesFile.writeObjectsTo();
				oos.writeObject(localizationPreferences);
				oos.flush();
				debug.println("...success.");
			} catch (IOException e) {
				debug.print("...failure. Cause: ");
				debug.printStackTrace(e);
			}
		}

		if (parserOutputBuilder != null) {
			publishToGui("Writing parser output...");
			parserOutput = parserOutputBuilder.toString();
			try {
				debug.println("Writing parser output to ", parserOutputFile, "...");
				Writer w = parserOutputFile.writeCharsTo(true);
				w.write(parserOutput);
				w.write(System.lineSeparator());
				w.flush();
				debug.println("...success.");
			} catch (IOException e) {
				debug.print("...failure. Cause: ");
				debug.printStackTrace(e);
			}
		}
		
		try {
			publishToGui("Ensuring existence of julia.css...");
			debug.println("Ensuring existence of julia.css...");
			boolean fileWritten = profile.installCss();
			if (fileWritten) {
				debug.println("...success. File written.");
			} else {
				debug.println("...success. File already existent.");
			}
		} catch (IOException e) {
			debug.print("...failure. Cause: ");
			debug.printStackTrace(e);
		}

		boolean hasLegacyPreferencesFile = false;
		try {
			publishToGui("Reading preferences...");
			debug.println("Reading preferences from ", preferencesFile, "...");
			preferences = new Preferences();
			Properties properties = preferencesFile.readPropertiesFrom();
			preferences.readFrom(properties);
			debug.println("...success: ", preferences);
		} catch (IOException e) {
			debug.print("...failure. Cause: ");
			debug.printStackTrace(e);
			hasLegacyPreferencesFile = readLegacyPreferencesFile();
		}

		if (preferencesFile.isEmpty()) {
			try {
				if (hasLegacyPreferencesFile) {
					publishToGui("Converting legacy preferences...");
					debug.println("Writing legacy preferences to ", preferencesFile, "...");
				} else {
					publishToGui("Writing default preferences...");
					debug.println("Writing default preferences to ", preferencesFile, "...");
				}
				Properties properties = new Properties();
				preferences.writeTo(properties);
				preferencesFile.writePropertiesTo(properties);
				debug.println("...success. File written.");
			} catch (IOException e) {
				debug.print("...failure. Cause: ");
				debug.printStackTrace(e);
			}
		}

		trimToSize(availableNumberFactories);
		trimToSize(availableFormulas);
		trimToSize(availableRepresentations);
		trimToSize(availableAliases);

		setGuiProgress(100);
		publishToGui("Releasing profile...");
		debug.println("Releasing profile ", profile.getRootDirectory(), "...");
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

	public List<AliasPlugin> getAvailableAliases() {
		if (availableAliases == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(availableAliases);
	}

	public int getProblemCount(int type) {
		return problemCount[type];
	}

	public JuliaExecutorService getExecutorService() {
		return executorService;
	}
}

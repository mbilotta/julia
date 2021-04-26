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

import static org.altervista.mbilotta.julia.Utilities.callSynchronously;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JDialog;

import org.altervista.mbilotta.julia.Gradient;
import org.altervista.mbilotta.julia.Gradient.Stop;
import org.altervista.mbilotta.julia.Out;
import org.altervista.mbilotta.julia.Printer;
import org.altervista.mbilotta.julia.StringPrinter;
import org.altervista.mbilotta.julia.program.gui.MessagePane;


public class Profile {

	private final Path root;
	private final Path descriptorRoot;
	private final Path classesRoot;
	private final Path cacheRoot;
	private final Path documentationRoot;
	private final Path preferences;
	private final Path localizationPreferences;
	private final Path descriptorParserOutput;
	private final Path classpath;
	private final Path classpathParserOutput;
	private final Path installerOutput;

	public Profile(Path rootDirectory) {
		root = rootDirectory;
		descriptorRoot = rootDirectory.resolve("xml");
		classesRoot = Paths.get("bin");
		cacheRoot = rootDirectory.resolve("cache");
		documentationRoot = rootDirectory.resolve("doc");
		preferences = rootDirectory.resolve("preferences");
		localizationPreferences = rootDirectory.resolve("localization-preferences");
		descriptorParserOutput = descriptorRoot.resolve("parser.log");
		classpath = rootDirectory.resolve("classpath.xml");
		classpathParserOutput = rootDirectory.resolve("classpath-parser.log");
		installerOutput = rootDirectory.resolve("installer.log");
	}

	public static Profile getDefaultProfile() {
		return new Profile(Paths.get(System.getProperty("user.home"), ".juliafg"));
	}

	public LockedFile lock() throws IOException {
		try {
			Files.createDirectory(root);
		} catch (FileAlreadyExistsException e) {}
		return new LockedFile(preferences, false);
	}

	public boolean installCss() throws IOException {
		try {
			Files.createDirectory(documentationRoot);
		} catch (FileAlreadyExistsException e) {}
		try (InputStream in = getClass().getResourceAsStream("julia.css")) {
			Files.copy(in, documentationRoot.resolve("julia.css"));
			return true;
		} catch (FileAlreadyExistsException e) {
			return false;
		}
	}

	public Path getRootDirectory() {
		return root;
	}

	public Path getPreferencesFile() {
		return preferences;
	}

	public Path getLocalizationPreferencesFile() {
		return localizationPreferences;
	}

	public Path getClasspathFile() {
		return classpath;
	}

	public Path getClasspathParserOutputFile() {
		return classpathParserOutput;
	}

	public Classpath createDefaultClasspath(List<LockedFile> descriptors) {
		Classpath rv = new Classpath();
		for (LockedFile descriptor : descriptors) {
			Path relativeParent = descriptorRoot.relativize(descriptor.getPath().getParent());
			Path path = classesRoot.resolve(relativeParent);
			rv.addEntry(path);
			rv.addJarFolderEntry(path);
		}
		return rv;
	}

	public List<Path> scanForDescriptors(Printer log, Out<Boolean> failureOut) throws IOException {
		if (log == null) {
			log = Printer.nullPrinter();
		}

		if (Files.notExists(descriptorRoot)) {
			return Collections.emptyList();
		}

		DescriptorVisitor fileVisitor = new DescriptorVisitor(log, failureOut);
		Files.walkFileTree(descriptorRoot, fileVisitor);
		return fileVisitor.getDescriptors();
	}

	public Path getDescriptorParserOutputFile() {
		return descriptorParserOutput;
	}

	public Path getInstallerOutputFile() {
		return installerOutput;
	}

	public Path getCacheFileFor(Path descriptor) {
		Path relativeParent = descriptorRoot.relativize(descriptor.getParent());
		String fileName = descriptor.getFileName().toString();
		assert fileName.endsWith(".xml");
		return cacheRoot.resolve(relativeParent).resolve(fileName.substring(0, fileName.length() - 4));
	}

	public Path getDocumentationFileFor(Path descriptor) {
		Path relativeParent = descriptorRoot.relativize(descriptor.getParent());
		String fileName = descriptor.getFileName().toString();
		assert fileName.endsWith(".xml");
		return documentationRoot.resolve(relativeParent).resolve(fileName.substring(0, fileName.length() - 4) + ".html");
	}

	public Path getDocumentationFileFor(String id) {
		return documentationRoot.resolve(id + ".html");
	}

	public Path relativizeDescriptor(Path descriptor) {
		return descriptorRoot.relativize(descriptor);
	}

	public Path relativize(Path path) {
		return root.relativize(path);
	}

	public static List<Gradient> getSampleGradients() {
		return Arrays.asList(
			new Gradient( // WIKIPEDIA
				new Stop(0, 	 66, 30, 15),
				new Stop(.0625f, 25, 7, 26),
				new Stop(.125f,  9, 1, 47),
				new Stop(.1875f, 4, 4, 73),
		        new Stop(.25f, 	 0, 7, 100),
		        new Stop(.3125f, 12, 44, 138),
		        new Stop(.375f,  24, 82, 177),
		        new Stop(.4375f, 57, 125, 209),
		        new Stop(.5f,    134, 181, 229),
		        new Stop(.5625f, 211, 236, 248),
		        new Stop(.625f,  241, 233, 191),
		        new Stop(.6875f, 248, 201, 95),
		        new Stop(.75f,   255, 170, 0),
		        new Stop(.8125f, 204, 128, 0),
		        new Stop(.875f,  153, 87, 0),
		        new Stop(.9375f, 106, 52, 3),
		        new Stop(1,      66, 30, 15)),
		    new Gradient(
		    	new Stop(0,		 255, 0, 0),
		    	new Stop(0.07f,	 255, 255, 0),
		    	new Stop(0.49f,  0, 255, 0),
		    	new Stop(0.57f,  0, 0, 255),
		    	new Stop(1,		 255, 0, 0)),
	        new Gradient(
	        	new Stop(0,		 51, 10, 112),
	        	new Stop(.37f, 	 0, 255, 0),
	        	new Stop(.46f, 	 255, 255, 255),
	        	new Stop(.88f, 	 51, 153, 255),
	        	new Stop(1, 	 51, 10, 112)),
			new Gradient(
				new Stop(0, 	 0, 95, 255),
				new Stop(.28f, 	 255, 255, 255),
				new Stop(.57f, 	 51, 0, 102),
				new Stop(.65f, 	 0, 0, 255),
				new Stop(1, 	 0, 95, 255)),
			new Gradient(
				new Stop(0, 	 95, 47, 0),
				new Stop(.21f, 	 153, 102, 51),
				new Stop(.34f, 	 255, 255, 204),
				new Stop(.68f, 	 0, 0, 0),
				new Stop(1, 	 95, 47, 0)),
			new Gradient(
				new Stop(0, 	 208, 251, 102),
				new Stop(.31f, 	 0, 0, 102),
				new Stop(.41f, 	 255, 51, 102),
				new Stop(.75f, 	 255, 204, 102),
				new Stop(1, 	 208, 251, 102)),
			new Gradient(
				new Stop(0,		 112, 0, 191),
				new Stop(.06f,	 0, 0, 0),
				new Stop(.45f,	 0, 102, 255),
				new Stop(.73f,	 255, 0, 0),
				new Stop(1,		 112, 0, 191)),
			new Gradient(
				new Stop(0, 	 245, 239, 157),
				new Stop(.08f, 	 0, 255, 0),
				new Stop(.49f, 	 0, 0, 255),
				new Stop(.61f, 	 102, 0, 204),
				new Stop(1, 	 245, 239, 157)),
			new Gradient(
				new Stop(0,		 10, 150, 10),
				new Stop(.27f,	 150, 0, 0),
				new Stop(.48f,	 0, 0, 150),
				new Stop(.65f,	 150, 150, 150),
				new Stop(1,		 10, 150, 10))
		);
	}

	private class DescriptorVisitor implements FileVisitor<Path> {
		
		private final Printer log;
		private final Out<Boolean> failureOut;
		private final List<Path> descriptors = new LinkedList<>();

		public DescriptorVisitor(Printer log, Out<Boolean> failureOut) {
			this.log = log;
			this.failureOut = failureOut;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
				throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
			String fileName = file.getFileName().toString();
			if (fileName.endsWith(".xml")) {
				Path relativeParent = descriptorRoot.relativize(file.getParent());
				Path created = null;
				try {
					created = Files.createDirectories(cacheRoot.resolve(relativeParent));
					created = Files.createDirectories(documentationRoot.resolve(relativeParent));
					descriptors.add(file);
				} catch (IOException e) {
					failureOut.set(true);
					log.print("- could not create directory \"");
					if (created == null) {
						log.print(cacheRoot.resolve(relativeParent), "\". Cause: ");
					} else {
						log.print(documentationRoot.resolve(relativeParent), "\". Cause: ");
					}
					log.printStackTrace(e);
					log.println("- descriptor \"", file, "\" discarded.");
				}
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc)
				throws IOException {
			failureOut.set(true);
			log.print("- could not inspect path \"", file, "\". Cause: ");
			log.printStackTrace(exc);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc)
				throws IOException {
			if (exc != null) {
				failureOut.set(true);
				log.print("- could not list directory \"", dir, "\". Cause: ");
				log.printStackTrace(exc);
			}
			return FileVisitResult.CONTINUE;
		}
		
		public List<Path> getDescriptors() {
			return descriptors;
		}
	}
	
	private enum FileOverwriteBehaviour {
		ASK, REPLACE_EXISTING, PRESERVE_EXISTING
	}

	private enum UserAnswer {
		YES("Yes"), YES_TO_ALL("Yes to All"), NO("No"), NO_TO_ALL("No to All"), CANCEL("Cancel");

		private final String label;

		private UserAnswer(String label) {
			this.label = label;
		}

		@Override
		public String toString() {
			return this.label;
		}
	}

	private static final class ZipExtraction implements Comparable<ZipExtraction> {

		private final ZipEntry src;
		private final Path dst;
		
		public ZipExtraction(ZipEntry src, Path dst) {
			this.src = src;
			this.dst = dst;
		}
		
		public ZipEntry getSrc() {
			return src;
		}

		public Path getDst() {
			return dst;
		}

		@Override
		public int compareTo(ZipExtraction other) {
			Path parent;
			boolean lt = (parent=dst.getParent()) == null || other.dst.startsWith(parent);
			boolean gt = (parent=other.dst.getParent()) == null || dst.startsWith(parent);
			if (lt && !gt) {
				return -1;
			}
			if (!lt && gt) {
				return 1;
			}
			if (lt && gt) {
				return dst.getFileName().compareTo(other.dst.getFileName());
			}
			return dst.compareTo(other.dst);
		}

		@Override
		public String toString() {
			return "ZipExtraction[src=" + src + ", dst=" + dst + "]";
		}
	}

	public abstract class PluginInstaller extends BlockingSwingWorker<Boolean> {
		
		private final File file;
		private final Printer printer;
		private int extractedCount = 0;

		private FileOverwriteBehaviour fowb = FileOverwriteBehaviour.ASK;

		public PluginInstaller(File file, Printer printer) {
			this.file = file;
			this.printer = printer == null ? Printer.nullPrinter() : printer;
		}

		@Override
		public void cancel() {
			cancel(false);
		}

		public int getExtractedCount() {
			return extractedCount;
		}

		public int getExtractionsCount() {
			return extractions.size();
		}

		protected abstract void showSuccess(String details);

		protected abstract void showFailure(String details);

		protected abstract void showCancellation(String details);

		protected abstract void showError(Throwable e);

		protected abstract UserAnswer askIfShouldRetryToCreateDirectory(Path path, Path relativePath, Throwable problem) throws Exception;

		protected abstract UserAnswer askIfShouldRetryToWriteFile(String src, Path dst, Path dstRelative, Throwable problem) throws Exception;

		@Override
		protected void processResult(Boolean result) {
			String details;
			if (printer instanceof StringPrinter) {
				details = printer.toString();
			} else {
				details = null;
			}
			if (result) {
				showSuccess(details);
			} else {
				showFailure(details);
			}
		}

		@Override
		protected void processCancellation() {
			String details;
			if (printer instanceof StringPrinter) {
				details = printer.toString();
			} else {
				details = null;
			}
			showCancellation(details);
		}

		@Override
		protected void processException(Throwable e) {
			printer.flush();
			printer.close();
			showError(e);
		}

		private boolean createDirs(Path relativePath) throws Exception {
			Path path = root.resolve(relativePath);
			try {
				Files.createDirectories(path);
				return true;
			} catch (IOException e) {
				UserAnswer answer = askIfShouldRetryToCreateDirectory(path, relativePath, e);
				switch (answer) {
				case YES:    return createDirs(path);
				case NO:     break;
				case CANCEL: cancel(false); break;
				default: throw new AssertionError(answer);
				}

				return !(e instanceof FileAlreadyExistsException);
			}
		}

		private boolean extract(ZipFile zipFile, ZipExtraction extraction, boolean replaceExisting) throws Exception {
			Path path = root.resolve(extraction.getDst());
			Path relativePath = extraction.getDst();
			try (InputStream in = zipFile.getInputStream(extraction.getSrc())) {
				CopyOption[] copyOptions = fowb == FileOverwriteBehaviour.REPLACE_EXISTING || replaceExisting ?
						new CopyOption[] { StandardCopyOption.REPLACE_EXISTING } : new CopyOption[] {};

				long numBytes = Files.copy(in, path, copyOptions);
				extractedCount++;
				printer.println("Done. ", numBytes, " bytes written.");
				return true;
			} catch (FileAlreadyExistsException e) {
				printer.print("File already exists. ");
				if (fowb == FileOverwriteBehaviour.ASK) {
					String sourcePath = extraction.getSrc().getName();
					UserAnswer answer = askIfShouldRetryToWriteFile(sourcePath, path, relativePath, e);
					if (answer == UserAnswer.YES) {
						printer.print("Replacing file... ");
						return extract(zipFile, extraction, true);
					} else if (answer == UserAnswer.YES_TO_ALL) {
						fowb = FileOverwriteBehaviour.REPLACE_EXISTING;
						printer.print("Replacing file (from now on, existing files will always be replaced)... ");
						return extract(zipFile, extraction, true);
					} else if (answer == UserAnswer.NO_TO_ALL) {
						fowb = FileOverwriteBehaviour.PRESERVE_EXISTING;
						printer.println("Preserved (from now on, existing files will always be preserved).");
					} else if (answer == UserAnswer.NO || answer == UserAnswer.CANCEL) {
						printer.println("Preserved.");
					}
					
					if (answer == UserAnswer.CANCEL) {
						cancel(false);
					}
				} else {
					printer.println("Preserved.");
				}

				return true;
			} catch (IOException e) {
				printer.print(e, ". ");
				String sourcePath = extraction.getSrc().getName();
				UserAnswer answer = askIfShouldRetryToWriteFile(sourcePath, path, relativePath, e);
				switch (answer) {
				case YES:    printer.print("Retrying... "); return extract(zipFile, extraction, replaceExisting);
				case NO:     printer.println("Failed."); break;
				case CANCEL: printer.println("Failed."); cancel(false); break;
				default: throw new AssertionError(answer);
				}
				
				return false;
			}
		}

		@Override
		protected Boolean doInBackground() throws Exception {
			Date date = new Date();
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			printer.println("[On ", dateFormat.format(date), "]");

			boolean rv = true;
			printer.print("Opening archive ", file, "... ");
			try (ZipFile zipFile = new ZipFile(file)) {
				printer.println("Done.");
				printer.print("Scanning entries... ");
				scanPackage(zipFile);
				final int maxProgress = extractions.size();
				printer.println("Done. ", maxProgress, " file(s) to be copied.");
				
				Path createdDir = null;
				Path notCreatedDir = null;
				int progress = 0;
				for (ZipExtraction extraction : extractions) {
					printer.print("Extracting ", extraction.getSrc(), " to ", extraction.getDst(), "... ");
					Path dir = extraction.getDst().getParent();
					publishToGui(extraction.getSrc().getName());
					if (notCreatedDir != null && dir.startsWith(notCreatedDir)) {
						printer.println("Skipped. Could not create target directory.");
						continue;
					}
					if (dir.equals(createdDir) || createDirs(dir)) {
						notCreatedDir = null;
						createdDir = dir;
						rv &= extract(zipFile, extraction, false);
						if (isCancelled()) {
							break;
						}
					} else {
						rv = false;
						printer.println("Skipped. Could not create target directory.");
						notCreatedDir = dir;
					}

					progress++;
					setGuiProgress(progress * 100 / maxProgress);
				}

				setGuiProgress(100);
				if (isCancelled()) {
					printer.print("Installation cancelled. ");
				} else {
					printer.print("Installation ", rv ? "succeeded. " : "failed. ");
				}
				printer.println(extractedCount, " of ", maxProgress, " file(s) were successfully written.");
				publishToGui("Closing...");
				printer.print("Closing archive... ");
			} catch (IOException e) {
				printer.println(e, ". Failed.");
				printer.println();
				return extractedCount > 0 ? rv : false;
			}

			printer.println("Done.");
			printer.println();
			printer.flush();
			printer.close();
			return rv;
		}

		private Set<ZipExtraction> extractions;

		private void scanPackage(ZipFile zipFile) {
			extractions = new TreeSet<>();
			Path binPath = null;
			Set<String> idPaths = new TreeSet<>();
			String xmlPrefix = descriptorRoot.getFileName().toString() + "/";
			String docPrefix = documentationRoot.getFileName().toString() + "/";
			String binPrefix = classesRoot.getFileName().toString() + "/";
			for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements(); ) {
				ZipEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if (!entryName.endsWith("/")) {
					if (entryName.startsWith(xmlPrefix) && entryName.endsWith(".xml")) {
						String idPath = entryName.substring(xmlPrefix.length(), entryName.lastIndexOf('/'));
						if (binPath == null) {
							binPath = classesRoot.resolve(idPath);
						}
						idPaths.add(idPath);
						extractions.add(new ZipExtraction(entry, Paths.get(entryName)));
					}
				}
			}

			Path docRootPath = root.relativize(documentationRoot);
			for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements(); ) {
				ZipEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if (!entryName.endsWith("/")) {
					if (entryName.startsWith(docPrefix)) {
						String resourcePath = entryName.substring(docPrefix.length());
						for (String idPath : idPaths) {
							extractions.add(new ZipExtraction(
									entry,
									docRootPath.resolve(idPath).resolve(resourcePath)));
						}
					} else if (entryName.startsWith(binPrefix) && binPath != null) {
						String resourcePath = entryName.substring(binPrefix.length());
						extractions.add(new ZipExtraction(
								entry,
								binPath.resolve(resourcePath)));
					}
				}
			}
		}
	}

	public class GuiPluginInstaller extends PluginInstaller {

		public GuiPluginInstaller(File file, Printer printer) {
			super(file, printer);
			setGuiRunning(true);
		}

		@Override
		protected void showSuccess(String details) {
			MessagePane.showInformationMessage(getBlockingDialog(),
					"Julia",
					"Installation succeeded. " + getExtractedCount() + " file(s) were written. Restart the "
					+ "program for the changes to take effect.",
					details);
		}

		@Override
		protected void showFailure(String details) {
			MessagePane.showWarningMessage(getBlockingDialog(),
					"Julia",
					"Installation failed. " + getExtractedCount() + " of " + getExtractionsCount()
					+ " file(s) were written. " + (details == null ?
					"Check out \"" + installerOutput.getFileName() + "\" for details." : "See details."),
					details);
		}

		@Override
		protected void showCancellation(String details) {
			MessagePane.showWarningMessage(getBlockingDialog(),
					"Julia",
					"Installation cancelled. " + getExtractedCount() + " of " + getExtractionsCount()
					+ " file(s) were written. " + (details == null ?
					"Check out \"" + installerOutput.getFileName() + "\" for details." : "See details."),
					details);
		}

		@Override
		protected void showError(Throwable e) {
			MessagePane.showErrorMessage(getBlockingDialog(),
					"Julia",
					"Installation halted unexpectedly. See details.",
					e);
		}

		@Override
		protected UserAnswer askIfShouldRetryToCreateDirectory(Path dst, Path dstRelative, Throwable problem) throws Exception {
			UserAnswer rv = callSynchronously(new Callable<UserAnswer>() {
				@Override
				public UserAnswer call() {
					String nl = System.lineSeparator();
					MessagePane messagePane = new MessagePane(
							"Could not create directory \"" + dst.getFileName() + "\""
							+ (problem instanceof FileAlreadyExistsException ?
								". An existing file prevented the creation of this directory at "
								+ "the target path" : "")
							+ " (see details). Retry?",
							"Target path (relative to the profile path): " + dstRelative + nl
							+ "Target path (full): " + dst + nl
							+ "Problem description: " + problem,
							MessagePane.ERROR_MESSAGE);
					messagePane.setOptions(new UserAnswer[]{ UserAnswer.YES, UserAnswer.NO, UserAnswer.CANCEL });
					JDialog dialog = messagePane.createDialog(getBlockingDialog(), "Julia");
					dialog.setResizable(true);
					dialog.setMinimumSize(dialog.getPreferredSize());
					dialog.setVisible(true);
					dialog.dispose();
					return (UserAnswer) messagePane.getValue();
				}
			});

			if (rv == null) {
				return UserAnswer.CANCEL;
			}
			return rv;
		}

		@Override
		protected UserAnswer askIfShouldRetryToWriteFile(String src, Path dst, Path dstRelative, Throwable problem) throws Exception {
			UserAnswer[] options;
			String message;
			if (problem instanceof FileAlreadyExistsException) {
				message = "Target file \"" + dst.getFileName() + "\" already exists (see details). "
					+ "Overwrite the existing file?";
				options = UserAnswer.values();
			} else {
				message = "Could not write file \"" + dst.getFileName() + "\" (see details). Retry?";
				options = new UserAnswer[]{ UserAnswer.YES, UserAnswer.NO, UserAnswer.CANCEL };
			}
			UserAnswer rv = callSynchronously(new Callable<UserAnswer>() {
				@Override
				public UserAnswer call() throws Exception {
					String nl = System.lineSeparator();
					MessagePane messagePane = new MessagePane(message,
							"Source path: " + src + nl
							+ "Target path (relative to the profile path): " + dstRelative + nl
							+ "Target path (full): " + dst + nl
							+ "Problem description: " + problem,
							MessagePane.ERROR_MESSAGE);
					messagePane.setOptions(options);
					JDialog dialog = messagePane.createDialog(getBlockingDialog(), "Julia");
					dialog.setResizable(true);
					dialog.setMinimumSize(dialog.getPreferredSize());
					dialog.setVisible(true);
					dialog.dispose();
					return (UserAnswer) messagePane.getValue();
				}
			});

			if (rv == null) {
				return UserAnswer.CANCEL;
			}
			return rv;
		}
	}
}

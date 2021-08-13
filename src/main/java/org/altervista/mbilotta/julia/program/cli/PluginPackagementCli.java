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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.program.JuliaZipOutputStream;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


@Command(name = "package", 
	description = "Create a JUP archive containing one or more Julia plugins.",
	descriptionHeading = "%n",
	parameterListHeading = "%nParameters:%n",
	optionListHeading = "%nOptions:%n",
	sortOptions = false)
public class PluginPackagementCli implements Runnable {

	@Option(names = { "-l", "--license" }, paramLabel = "FILE",
		description = "License file for this JUP archive.")
	Path licensePath;

	@Option(names = { "-o", "--output" }, paramLabel = "OUTPUT_PATH",
		description = "Output file path. If this option is absent, a .jup file named as the last segment of PLUGIN_PATH will be created in the current directory. If OUTPUT_PATH is a directory, such file will be created there. If OUTPUT_PATH ends with \".jup\", OUTPUT_PATH will be the output file.")
	Path outputPath;

	@Option(names = { "-x", "--replace-existing" },
		description = "Use this flag to eventually replace an already existing file at output path.")
	boolean replaceExisting;

	@Parameters(index = "0",
		arity = "1",
		paramLabel = "PLUGIN_PATH",
		description = "Relative path that will be created inside the /xml, /bin, /doc subdirectories of the target profile when this plugin is installed.")
	Path pluginPath;

	@Parameters(index = "1..*", arity = "1", paramLabel = "INPUT_PATH",
		description = "Each INPUT_PATH can be a file or a directory. Files will be added to the right entry in the archive based on their extension: plugin descriptors must end with \".xml\"; JARs must end with \".jar\"; files that are none of the two will be treated as documentation resources. When INPUT_PATH is a directory, its contents will be added to the archive following the same logic. Subdirectories will be scanned recursively.")
	List<Path> inputPaths;

	@Override
	public String toString() {
		return "[" +
			"pluginPath=" + pluginPath +
			", licensePath=" + licensePath +
			", outputPath=" + outputPath +
			", inputPaths=" + inputPaths +
			"]";
	}

	static final int KILOBYTE = 1024;
	static final int BUFFER_SIZE = 8 * KILOBYTE;
	private byte[] buffer;

	private Path xmlEntry = Path.of("xml");
	private Path binEntry = Path.of("bin");
	private Path docEntry = Path.of("doc");

	private int descriptorCount = 0;
	private int jarCount = 0;

	private void handleFile(Path file, Path treeRoot, ZipOutputStream target) throws IOException {
		Path dstPath;
		if (treeRoot != null) {
			dstPath = treeRoot.relativize(file);
		} else {
			dstPath = file.getFileName();
		}
		String fileName = file.getFileName().toString();
		if (fileName.endsWith(".xml")) {
			descriptorCount++;
			dstPath = xmlEntry.resolve(pluginPath).resolve(dstPath);
		} else if (fileName.endsWith(".jar")) {
			jarCount++;
			dstPath = binEntry.resolve(dstPath);
		} else {
			dstPath = docEntry.resolve(dstPath);
		}
		compressFile(file, dstPath, target);
	}

	private void handleLicenseFile(Path file, ZipOutputStream target) throws IOException {
		compressFile(file, docEntry.resolve(file.getFileName()), target);
		compressFile(file, binEntry.resolve(file.getFileName()), target);
	}

	private void compressFile(Path srcPath, Path dstPath, ZipOutputStream target) throws IOException {
		target.putNextEntry(new ZipEntry(Utilities.toUnixString(dstPath)));
		try (FileInputStream fis = new FileInputStream(srcPath.toFile())) {
			int length;
			while ((length = fis.read(buffer)) >= 0) {
				target.write(buffer, 0, length);
			}
		}
	}

	private void compressFiles(List<Path> paths, ZipOutputStream target) throws IOException {
		for (Path path : paths) {
			if (Files.isDirectory(path)) {
				Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						handleFile(file, path, target);
						return FileVisitResult.CONTINUE;
					}
				});
			} else {
				handleFile(path, null, target);
			}
		}
	}

	@Override
	public void run() {
		if (outputPath == null) {
			String fileName = pluginPath.getFileName() + ".jup";
			outputPath = Paths.get(fileName);
		} else if (!outputPath.getFileName().toString().endsWith(".jup")) {
			String fileName = pluginPath.getFileName() + ".jup";
			outputPath = outputPath.resolve(Paths.get(fileName));
		}

		File tempFile;
		try {
			tempFile = File.createTempFile("juliafg-", "-jup");
		} catch (IOException e) {
			System.err.printf("Error (%s): %s%n", e.getClass().getSimpleName(), e.getMessage());
			return;
		}
		tempFile.deleteOnExit();

		try (ZipOutputStream zos = new JuliaZipOutputStream(new FileOutputStream(tempFile))) {
			buffer = new byte[BUFFER_SIZE];
			if (licensePath != null) {
				handleLicenseFile(licensePath, zos);
			}
			compressFiles(inputPaths, zos);

			if (descriptorCount == 0) {
				System.err.println("Error: no descriptor was found. At least one must be provided.");
				return;
			} else if (jarCount == 0) {
				System.err.println("Error: no JAR was found. At least one must be provided.");
				return;
			}
		} catch (IOException e) {
			System.err.printf("Error (%s): %s%n", e.getClass().getSimpleName(), e.getMessage());
			return;
		}

		try {
			if (replaceExisting) {
				Files.copy(tempFile.toPath(), outputPath, StandardCopyOption.REPLACE_EXISTING);
			} else {
				Files.copy(tempFile.toPath(), outputPath);
			}
		} catch (NoSuchFileException e) {
			System.err.println("Error: cannot write to " + outputPath.toAbsolutePath() + " because the parent directory does not exist.");
		} catch (FileAlreadyExistsException e) {
			System.err.println("Error: cannot write to " + outputPath.toAbsolutePath() + " because a file already exists at that location. Add --replace-existing to overwrite that file.");
		} catch (DirectoryNotEmptyException e) {
			System.err.println("Error: cannot write to " + outputPath.toAbsolutePath() + " because a non-empty directory already exists at that location.");
		} catch (IOException e) {
			System.err.printf("Error (%s): %s%n", e.getClass().getSimpleName(), e.getMessage());
		}
	}
}
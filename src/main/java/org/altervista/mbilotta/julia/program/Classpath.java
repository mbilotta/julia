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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.altervista.mbilotta.julia.Out;
import org.altervista.mbilotta.julia.Printer;


public class Classpath implements Iterable<Classpath.Entry> {

	public static class Entry {

		private final Path path;
		private final boolean jarFolder;

		public Entry(Path path, boolean jarFolder) {
			assert path != null;
			this.path = path;
			this.jarFolder = jarFolder;
		}

		public Path getPath() {
			return path;
		}

		public boolean isJarFolder() {
			return jarFolder;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			
			if (o instanceof Entry) {
				Entry entry = (Entry) o;
				return entry.path.equals(path) &&
						entry.jarFolder == jarFolder;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(jarFolder, path);
		}

		@Override
		public String toString() {
			return "[" + "path=" + path + ", jarFolder=" + jarFolder + "]";
		}
	}
	
	private Set<Entry> entries = new LinkedHashSet<>();

	public Iterator<Entry> iterator() {
		return entries.iterator();
	}

	public boolean addEntry(Path path) {
		return entries.add(new Entry(path, false));
	}

	public boolean addJarFolderEntry(Path path) {
		return entries.add(new Entry(path, true));
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public int size() {
		return entries.size();
	}

	@Override
	public String toString() {
		return entries.toString();
	}

	public ClassLoader createClassLoader(Profile profile, Printer log, Out<Boolean> failureOut) {
		if (log == null) {
			log = Printer.nullPrinter();
		}

		List<URL> urls = new LinkedList<>();
		for (Entry entry : this) {
			Path path = entry.getPath();
			
			if (!path.isAbsolute()) {
				path = profile.getRootDirectory().resolve(path);
			}

			try {
				path = path.toRealPath();
			} catch (NoSuchFileException e) {
				failureOut.set(true);
				log.println("- path \"", path, "\" does not exists.");
				continue;
			} catch (IOException e) {
				failureOut.set(true);
				log.print("- path \"", path, "\" could not be inspected. Cause: ");
				log.printStackTrace(e);
				continue;
			}

			if (entry.isJarFolder()) {
				try (DirectoryStream<Path> children = Files.newDirectoryStream(path, "*.{jar,JAR}")) {
					for (Path child : children) {
						try {
							URL url = child.toUri().toURL();
							if (Files.readAttributes(child, BasicFileAttributes.class).isRegularFile()) {
								urls.add(removeEndingSlashIfPresent(url));
								log.println("- found jar \"", child, "\".");
							}
						} catch (MalformedURLException e) {
							failureOut.set(true);
							log.print("- path \"", child, "\" could not be converted to an URL. Cause: ");
							log.printStackTrace(e);
						} catch (IOException e) {
							failureOut.set(true);
							log.print("- path \"", child, "\" could not be inspected. Cause: ");
							log.printStackTrace(e);
						}
					}
				} catch (DirectoryIteratorException e) {
					failureOut.set(true);
					log.print("- directory \"", path, "\" could not be listed. Cause: ");
					log.printStackTrace(e.getCause());
				} catch (NotDirectoryException e) {
					failureOut.set(true);
					log.println("- path \"", path, "\" is not a directory.");
				} catch (IOException e) {
					failureOut.set(true);
					log.print("- path \"", path, "\" could not be inspected. Cause: ");
					log.printStackTrace(e);
				}
			} else {
				try {
					URL url = path.toUri().toURL();
					BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
					if (attributes.isDirectory()) {
						urls.add(forceEndingSlash(url));
						log.println("- found directory \"", path, "\".");
					} else if (attributes.isRegularFile()) {
						urls.add(removeEndingSlashIfPresent(url));
						log.println("- found regular file \"", path, "\".");
					} else {
						failureOut.set(true);
						log.println("- path \"", path, "\" is not a regular file or a directory.");
					}
				} catch (MalformedURLException e) {
					failureOut.set(true);
					log.print("- path \"", path, "\" could not be converted to an URL. Cause: ");
					log.printStackTrace(e);
				} catch (IOException e) {
					failureOut.set(true);
					log.print("- path \"", path, "\" could not be inspected. Cause: ");
					log.printStackTrace(e);
				}
			}
		}

		if (urls.isEmpty()) {
			return ClassLoader.getSystemClassLoader();
		}

		return new URLClassLoader(urls.toArray(new URL[urls.size()]));
	}

	private static URL forceEndingSlash(URL url) throws MalformedURLException {
		String urlString = url.toString();
		if (urlString.endsWith("/")) {
			return url;
		}
		return new URL(urlString + "/");
	}

	private static URL removeEndingSlashIfPresent(URL url) throws MalformedURLException {
		String urlString = url.toString();
		if (urlString.endsWith("/")) {
			return new URL(urlString.substring(0, urlString.length() - 1));
		}
		return url;
	}
}

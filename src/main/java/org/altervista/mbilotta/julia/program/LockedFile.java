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

import static java.nio.file.StandardOpenOption.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;


public class LockedFile implements Closeable {

	private final Path path;
	private final FileChannel channel;
	private final FileLock lock;

	public LockedFile(Path path, boolean readOnly) throws FileAlreadyLockedException, IOException {
		this.path = path;
		channel = FileChannel.open(path, readOnly ?
				new OpenOption[] { READ } : new OpenOption[] { CREATE, READ, WRITE });
		boolean lockAcquired = false;
		try {
			lock = channel.tryLock(0, Long.MAX_VALUE, readOnly);
			if (lock == null) {
				throw new FileAlreadyLockedException(path.toString());
			}
			lockAcquired = true;
		} finally {
			if (!lockAcquired)
				channel.close();
		}
	}

	public Path getPath() {
		return path;
	}

	public long getSize() throws IOException {
		return channel.size();
	}

	public boolean isOpen() {
		return channel.isOpen();
	}

	public boolean isReadOnly() {
		return lock.isShared();
	}

	public boolean isEmpty() throws IOException {
		return getSize() == 0;
	}

	public ObjectInputStream readObjectsFrom() throws IOException {
		return new ObjectInputStream(readBytesFrom());
	}

	public ObjectOutputStream writeObjectsTo() throws IOException {
		return new ObjectOutputStream(writeBytesTo(false));
	}

	public Properties readPropertiesFrom() throws IOException {
		Properties rv = new Properties();
		rv.load(readCharsFrom());
		return rv;
	}

	public void writePropertiesTo(Properties properties) throws IOException {
		Writer out = writeCharsTo(false);
		properties.store(out, null);
		out.flush();
	}

	public InputStream readBytesFrom() {
		return Channels.newInputStream(channel);
	}

	public OutputStream writeBytesTo(boolean append) throws IOException {
		if (append) {
			channel.position(channel.size());
		} else {
			channel.truncate(0);
		}
		return Channels.newOutputStream(channel);
	}

	public Reader readCharsFrom() {
		return Channels.newReader(channel, Charset.defaultCharset().newDecoder(), -1);
	}

	public Reader readCharsFrom(String charsetName) {
		return Channels.newReader(channel, charsetName);
	}

	public Writer writeCharsTo(boolean append) throws IOException {
		if (append) {
			channel.position(channel.size());
		} else {
			channel.truncate(0);
		}
		return Channels.newWriter(channel, Charset.defaultCharset().newEncoder(), -1);
	}

	public Writer writeCharsTo(boolean append, String charsetName) throws IOException {
		if (append) {
			channel.position(channel.size());
		} else {
			channel.truncate(0);
		}
		return Channels.newWriter(channel, charsetName);
	}

	public void close() throws IOException {
		try (FileChannel fc = channel) {
			lock.release();
		}
	}

	@Override
	public String toString() {
		return path.toString();
	}

	public static List<LockedFile> open(List<Path> paths, boolean readOnly)
			throws FileAlreadyLockedException, IOException {
		Set<Path> readOnlySet;
		if (readOnly) {
			readOnlySet =
					Collections.newSetFromMap(new IdentityHashMap<Path, Boolean>(paths.size()));
			readOnlySet.addAll(paths);
		} else {
			readOnlySet = Collections.emptySet();
		}
		return open(paths, readOnlySet);
	}

	public static List<LockedFile> open(List<Path> paths, Set<Path> readOnlySet)
			throws FileAlreadyLockedException, IOException {
		return open(paths, readOnlySet, new ArrayList<LockedFile>(paths.size()));
	}

	public static List<LockedFile> open(List<Path> paths, boolean readOnly, List<LockedFile> rv)
			throws FileAlreadyLockedException, IOException {
		Set<Path> readOnlySet;
		if (readOnly) {
			readOnlySet =
					Collections.newSetFromMap(new IdentityHashMap<Path, Boolean>(paths.size()));
			readOnlySet.addAll(paths);
		} else {
			readOnlySet = Collections.emptySet();
		}
		return open(paths, readOnlySet, rv);
	}

	public static List<LockedFile> open(List<Path> paths, Set<Path> readOnlySet, List<LockedFile> rv)
			throws FileAlreadyLockedException, IOException {
		if (paths.isEmpty()) {
			return rv;
		}

		int lastIndex = paths.size() - 1;
		Path path = paths.get(lastIndex);
		boolean readOnly = readOnlySet.contains(path); 

		try (CloseableHider<LockedFile> c =
				new CloseableHider<>(new LockedFile(path, readOnly))) {

			open(paths.subList(0, lastIndex), readOnlySet, rv);
			rv.add(c.getCloseable());
			c.setCloseable(null);
			return rv;
		}
	}

	public static final class CloseableHider<C extends Closeable> implements Closeable {

		private C closeable;

		public CloseableHider(C closeable) {
			this.closeable = closeable;
		}

		public C getCloseable() {
			return closeable;
		}

		public void setCloseable(C closeable) {
			this.closeable = closeable;
		}

		public void close() throws IOException {
			if (closeable != null) {
				closeable.close();
			}
		}
	}
}

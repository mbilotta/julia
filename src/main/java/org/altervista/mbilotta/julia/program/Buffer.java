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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;


public class Buffer {

	private byte[] buffer;
	private int size;

	public Buffer(int capacity) {
		buffer = new byte[capacity];
		size = 0;
	}

	public int size() {
		return size;
	}

	public int capacity() {
		return buffer.length;
	}

	public void put(byte value) {
		buffer[size++] = value;
	}

	public void reset(int capacity) {
		size = 0;
		if (capacity > buffer.length) {
			buffer = new byte[capacity];
		}
	}

	public InputStream toInputStream() {
		return new ByteArrayInputStream(buffer, 0, size);
	}

	public static Buffer readFully(LockedFile file, Buffer rv, MessageDigest md) throws IOException {
		long size = file.getSize();
		if (size > Integer.MAX_VALUE)
			throw new OutOfMemoryError("Required array size too large");

		if (rv != null) {
			rv.reset((int) size);
		} else {
			rv = new Buffer((int) size);
		}

		InputStream is = file.readBytesFrom();
		if (md != null) {
			md.reset();
			is = new DigestInputStream(is, md);
		}
		is = new BufferedInputStream(is);
		int b;
		while ( (b = is.read()) != -1 ) {
			rv.put((byte) b);
		}

		return rv;
	}
}

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

package org.altervista.mbilotta.julia;

import java.util.Arrays;


public final class LimitedCharBuffer {
	
	private char[] buffer;
	private final int capacity;
	private int start = 0;
	private int count = 0;

	public LimitedCharBuffer(int capacity) {
		this.buffer = new char[Math.min(16, capacity)];
		this.capacity = capacity;
	}

	public synchronized int getSize() {
		return count;
	}

	public int getCapacity() {
		return capacity;
	}

	public synchronized boolean isFull() {
		return count == capacity;
	}

	public synchronized void append(char c) {
		if (count < capacity) {
			ensureBufferLength(count + 1);
			buffer[count] = c;
			count++;
		} else if (capacity > 0) {
			buffer[start] = c;
			start = (start + 1) % capacity;
		}
	}

	public void append(char[] c) {
		append(c, 0, c.length);
	}

	public synchronized void append(char[] c, int offset, int length) {
		if (offset < 0
				|| length < 0
				|| offset + length > c.length
				|| offset + length < 0) {
			throw new IndexOutOfBoundsException();
		}

		if (length >= capacity) {
			if (buffer.length < capacity) {
				buffer = new char[capacity];
			}
			System.arraycopy(c, offset + length - capacity, buffer, 0, capacity);
			start = 0;
			count = capacity;
		} else if (count < capacity) {
			ensureBufferLength(count + length);
			int nUntilLoop = Math.min(length, capacity - count);
			int nPastLoop = length - nUntilLoop;
			System.arraycopy(c, offset, buffer, count, nUntilLoop);
			System.arraycopy(c, offset + nUntilLoop, buffer, 0, nPastLoop);
			start = nPastLoop;
			count += nUntilLoop;
		} else {
			int nUntilLoop = Math.min(length, capacity - start);
			int nPastLoop = length - nUntilLoop;
			System.arraycopy(c, offset, buffer, start, nUntilLoop);
			System.arraycopy(c, offset + nUntilLoop, buffer, 0, nPastLoop);
			start = (start + nUntilLoop) % capacity;
			start += nPastLoop;
		}
	}
	
	public void append(String s) {
		append(s, 0, s.length());
	}

	public synchronized void append(String s, int offset, int length) {
		if (offset < 0
				|| length < 0
				|| offset + length > s.length()
				|| offset + length < 0) {
			throw new IndexOutOfBoundsException();
		}

		if (length >= capacity) {
			if (buffer.length < capacity) {
				buffer = new char[capacity];
			}
			s.getChars(offset + length - capacity, offset + length, buffer, 0);
			start = 0;
			count = capacity;
		} else if (count < capacity) {
			ensureBufferLength(count + length);
			int nUntilLoop = Math.min(length, capacity - count);
			int loopOffset = offset + nUntilLoop;
			s.getChars(offset, loopOffset, buffer, count);
			s.getChars(loopOffset, offset + length, buffer, 0);
			start = length - nUntilLoop;
			count += nUntilLoop;
		} else {
			int nUntilLoop = Math.min(length, capacity - start);
			int loopOffset = offset + nUntilLoop;
			s.getChars(offset, loopOffset, buffer, start);
			s.getChars(loopOffset, offset + length, buffer, 0);
			start = (start + nUntilLoop) % capacity;
			start += length - nUntilLoop;
		}
	}

	public synchronized String toString() {
		if (start > 0) {
			assert count == capacity;
			char[] newBuffer = new char[capacity];
			int nUntilLoop = capacity - start;
			System.arraycopy(buffer, start, newBuffer, 0, nUntilLoop);
			System.arraycopy(buffer, 0, newBuffer, nUntilLoop, start);
			start = 0;
			buffer = newBuffer;
		}
		return new String(buffer, 0, count);
	}

	public synchronized void clear() {
		start = 0;
		count = 0;
	}

	public synchronized String empty() {
		String rv = toString();
		count = 0;
		return rv;
	}

	private void ensureBufferLength(int minimumLength) {
		if (buffer.length < capacity && minimumLength - buffer.length > 0) {
			int newLength = buffer.length * 2 + 2;
			if (newLength - minimumLength < 0)
				newLength = minimumLength;
			if (newLength < 0 || newLength > capacity) {
				newLength = capacity;
			}
			buffer = Arrays.copyOf(buffer, newLength);
		}
	}

	public static void main(String[] args) {
		LimitedCharBuffer buf = new LimitedCharBuffer(0);
		buf.append("Cantami o diva del Pelide Achille", 0, 3);
		buf.append("Cantami o diva del Pelide Achille", 3, 4);
		buf.append("Cantami o diva del Pelide Achille", 7, 2);
		buf.append("Cantami o diva del Pelide Achille", 9, 5);
		buf.append("Cantami o diva del Pelide Achille", 14, 4);
		buf.append("Cantami o diva del Pelide Achille", 18, 7);
		buf.append("Cantami o diva del Pelide Achille", 25, 8);
		System.out.println(buf.toString()); //de Achille
	}
}

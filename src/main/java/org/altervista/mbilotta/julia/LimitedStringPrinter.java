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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Formatter;
import java.util.Locale;


public final class LimitedStringPrinter extends Printer {
	
	private final LimitedCharBuffer buffer;

	private Formatter formatter = null;
	private final Writer wOut;
	private final PrintWriter pwOut;
	
	private final class WriterAdapter extends Writer {
		@Override
		public void write(int c) {
			buffer.append((char) c);
		}
		@Override
		public void write(char[] chars, int offset, int length) {
			buffer.append(chars, offset, length);
		}
		@Override
		public void write(String s, int offset, int length) {
			buffer.append(s, offset, length);
		}
		@Override
		public void flush() {
		}
		@Override
		public void close() {
		}
	}

	public LimitedStringPrinter(int maxLength) {
		this(new LimitedCharBuffer(maxLength));
	}

	public LimitedStringPrinter(LimitedCharBuffer target) {
		super(target);
		buffer = target;
		wOut = new WriterAdapter();
		pwOut = new PrintWriter(wOut);
	}

	public int getMaxLength() {
		return buffer.getCapacity();
	}

	public int getLength() {
		return buffer.getSize();
	}

	@Override
	protected void printStackTraceImpl(Throwable t) {
		t.printStackTrace(pwOut);
	}

	@Override
	protected void printImpl(String s) {
		buffer.append(s);
	}

	@Override
	protected void printImpl(char[] chars) {
		buffer.append(chars);
	}

	@Override
	protected void printfImpl(Locale l, String format, Object[] args)
			throws IOException {
		if (formatter == null || formatter.locale() != l) {
			formatter = new Formatter(wOut, l);
		}
		formatter.format(l, format, args);
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() {
	}

	@Override
	public boolean checkError() {
		return false;
	}

	public void clear() {
		buffer.clear();
	}

	public String empty() {
		return buffer.empty();
	}

	public String toString() {
		return buffer.toString();
	}
}

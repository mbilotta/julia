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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import java.util.Locale;


public final class StringPrinter extends Printer {
	
	private final StringBuffer buffer;
	private Formatter formatter = null;
	private final PrintWriter pwOut;

	public StringPrinter() {
		this(new StringWriter());
	}

	public StringPrinter(int initialSize) {
		this(new StringWriter(initialSize));
	}

	public StringPrinter(StringWriter out) {
		super(out.getBuffer());
		buffer = out.getBuffer();
		pwOut = new PrintWriter(out);
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
	protected void printfImpl(Locale l, String format, Object[] args) {
		if (formatter == null || formatter.locale() != l) {
			formatter = new Formatter(buffer, l);
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

	public String toString() {
		return buffer.toString();
	}

	public String empty() {
		synchronized (buffer) {
			String rv = buffer.toString();
			buffer.setLength(0);
			return rv;
		}
	}
}

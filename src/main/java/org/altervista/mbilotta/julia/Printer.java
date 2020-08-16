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

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Locale;


public abstract class Printer implements Flushable, Closeable {

	protected final Object lock;
	private volatile boolean enabled = true;
	private boolean trouble = false;

	public static final int MIN_BUFFER_SIZE = 256;
	
	private static Writer writeTo(OutputStream out) {
		return new OutputStreamWriter(out);
	}

	private static Writer bufferize(Writer out, int bufferSize) {
		if (bufferSize == -1)
			return new BufferedWriter(out);

		return new BufferedWriter(out, Math.max(bufferSize, MIN_BUFFER_SIZE));
	}

	public static final Printer NULL_PRINTER = new Printer() {
		@Override
		public boolean isEnabled() { return false; }
		@Override
		public void setEnabled(boolean enabled) {};
		@Override
		public void println(String s) {}
		@Override
		public void println(char[] chars) {}
		@Override
		public void println() {}
		@Override
		public void println(Object o) {}
		@Override
		public void println(Object o1, Object o2) {}
		@Override
		public void println(Object o1, Object o2, Object o3) {}
		@Override
		public void println(Object... oi) {}
		@Override
		public void print(String s) {}
		@Override
		public void print(char[] chars) {}
		@Override
		public void print(Object o) {}
		@Override
		public void print(Object o1, Object o2) {}
		@Override
		public void print(Object o1, Object o2, Object o3) {}
		@Override
		public void print(Object o1, Object... oi) {}
		@Override
		public Printer printf(String format, Object... args) {
			return this;
		}
		@Override
		public Printer printf(Locale l, String format, Object... args) {
			return this;
		}
		@Override
		public void printStackTrace(Throwable t) {}
		@Override
		public boolean checkError() { return false; };
		@Override
		public void flush() {}
		@Override
		public void close() {}
		@Override
		protected void printStackTraceImpl(Throwable t) {}
		@Override
		protected void printImpl(String s) throws IOException {}
		@Override
		protected void printImpl(char[] chars) throws IOException {}
		@Override
		protected void printfImpl(Locale l, String format, Object[] args)
				throws IOException {}
		@Override
		public String toString() { return "NULL_PRINTER"; };
	};

	protected Printer() {
		this.lock = this;
	}

	protected Printer(Object lock) {
		if (lock == null)
			throw new NullPointerException();
		this.lock = lock;
	}

	static final class Impl extends Printer {

		private final Writer out;
		private final PrintWriter pwOut;
		private final boolean autoFlush;
		private final Flushable concurrent;

		Impl(Writer out, boolean autoFlush) {
			this.out = out;
			this.pwOut = out instanceof PrintWriter ? (PrintWriter) out : new PrintWriter(out);
			this.autoFlush = autoFlush;
			this.concurrent = null;
		}

		Impl(Writer out, boolean autoFlush, int bufferSize) {
			this.out = bufferize(out, bufferSize);
			this.pwOut = new PrintWriter(this.out);
			this.autoFlush = autoFlush;
			this.concurrent = null;
		}

		Impl(OutputStream out, boolean autoFlush, int bufferSize) {
			this(writeTo(out), autoFlush, bufferSize);
		}

		Impl(Writer out) {
			this(out, out);
		}

		Impl(Writer out, int bufferSize) {
			this(out, out, bufferSize);
		}

		Impl(OutputStream out, int bufferSize) {
			this(out, out, bufferSize);
		}

		Impl(Writer out, Object lock) {
			this(out, lock, out);
		}

		Impl(Writer out, Object lock, int bufferSize) {
			this(out, lock, out, bufferSize);
		}

		Impl(OutputStream out, Object lock, int bufferSize) {
			this(writeTo(out), lock, out, bufferSize);
		}

		Impl(Writer out, Object lock, Flushable concurrent) {
			super(lock);
			this.out = out;
			this.pwOut = out instanceof PrintWriter ? (PrintWriter) out : new PrintWriter(out);
			this.autoFlush = true;
			this.concurrent = concurrent;
		}

		Impl(Writer out, Object lock, Flushable concurrent, int bufferSize) {
			super(lock);
			this.out = bufferize(out, bufferSize);
			this.pwOut = new PrintWriter(this.out);
			this.autoFlush = true;
			this.concurrent = concurrent;
		}

		Impl(OutputStream out, Object lock, Flushable concurrent, int bufferSize) {
			this(writeTo(out), lock, concurrent, bufferSize);
		}

		@Override
		public void flush() {
			synchronized (lock) {
				try {
					prepare();
					out.flush();
				} catch (IOException e) {
					setError();
				}
			}
		}

		@Override
		public void close() {
			synchronized (lock) {
				try {
					prepare();
					pwOut.close();
					if (pwOut.checkError()) {
						setError();
					}
				} catch (IOException e) {
					setError();
				}
			}
		}

		@Override
		protected void printStackTraceImpl(Throwable t) {
			t.printStackTrace(pwOut);
			if (pwOut.checkError()) {
				setError();
			}
		}

		@Override
		protected void prepare() throws IOException {
			if (concurrent != null) {
				concurrent.flush();
			}
		}

		@Override
		protected void conclude(boolean flush) throws IOException {
			if (concurrent != null || (autoFlush && flush)) {
				out.flush();
			}
		}

		@Override
		protected void printImpl(String s) throws IOException {
			out.write(s);
		}

		@Override
		protected void printImpl(char[] chars) throws IOException {
			out.write(chars);
		}

		@Override
		protected void printfImpl(Locale l, String format, Object[] args) {
			pwOut.printf(l, format, args);
			if (pwOut.checkError()) {
				setError();
			}
		}
	}

	public static Printer newPrinter(Writer out, boolean autoFlush) {
		return new Impl(out, autoFlush);
	}

	public static Printer newPrinter(Writer out, boolean autoFlush, int bufferSize) {
		return new Impl(out, autoFlush, bufferSize);
	}

	public static Printer newPrinter(OutputStream out, boolean autoFlush, int bufferSize) {
		return new Impl(out, autoFlush, bufferSize);
	}

	public static Printer newConcurrentPrinter(Writer out) {
		return new Impl(out);
	}

	public static Printer newConcurrentPrinter(Writer out, int bufferSize) {
		return new Impl(out, bufferSize);
	}

	public static Printer newConcurrentPrinter(OutputStream out, int bufferSize) {
		return new Impl(out, bufferSize);
	}

	public static Printer newStandardOutput() {
		return newStandardOutput(1024);
	}

	public static Printer newStandardOutput(int bufferSize) {
		return new Impl(new FileWriter(FileDescriptor.out), true, bufferSize);
	}

	public static Printer newConcurrentStandardOutput() {
		return newConcurrentStandardOutput(1024);
	}

	public static Printer newConcurrentStandardOutput(int bufferSize) {
		return new Impl(new FileWriter(FileDescriptor.out), System.out, System.out, bufferSize);
	}

	public static Printer newStandardError() {
		return newStandardError(1024);
	}

	public static Printer newStandardError(int bufferSize) {
		return new Impl(new FileWriter(FileDescriptor.err), true, bufferSize);
	}

	public static Printer newConcurrentStandardError() {
		return newConcurrentStandardError(1024);
	}

	public static Printer newConcurrentStandardError(int bufferSize) {
		return new Impl(new FileWriter(FileDescriptor.err), System.err, System.err, bufferSize);
	}

	public static Printer nullPrinter() {
		return NULL_PRINTER;
	}

	public static Printer newStringPrinter() {
		return new StringPrinter();
	}

	public static Printer newStringPrinter(int initialSize) {
		return new StringPrinter(initialSize);
	}

	public static Printer newStringPrinter(StringWriter out) {
		return new StringPrinter(out);
	}

	public static Printer newLimitedStringPrinter(int capacity) {
		return new LimitedStringPrinter(capacity);
	}

	public static String newLine() {
		return System.lineSeparator();
	}

	public static Printer wrapPrinter(Printer printer) {
		return new WrappedPrinter(printer);
	}

	public static Object lateToString(final boolean[] a) {
		return new Object() {
			@Override
			public String toString() {
				return Arrays.toString(a);
			}
		};
	}

	public static Object lateToString(final byte[] a) {
		return new Object() {
			@Override
			public String toString() {
				return Arrays.toString(a);
			}
		};
	}

	public static Object lateToString(final char[] a) {
		return new Object() {
			@Override
			public String toString() {
				return Arrays.toString(a);
			}
		};
	}

	public static Object lateToString(final short[] a) {
		return new Object() {
			@Override
			public String toString() {
				return Arrays.toString(a);
			}
		};
	}

	public static Object lateToString(final int[] a) {
		return new Object() {
			@Override
			public String toString() {
				return Arrays.toString(a);
			}
		};
	}

	public static Object lateToString(final long[] a) {
		return new Object() {
			@Override
			public String toString() {
				return Arrays.toString(a);
			}
		};
	}

	public static Object lateToString(final float[] a) {
		return new Object() {
			@Override
			public String toString() {
				return Arrays.toString(a);
			}
		};
	}

	public static Object lateToString(final double[] a) {
		return new Object() {
			@Override
			public String toString() {
				return Arrays.toString(a);
			}
		};
	}

	public static Object lateToString(final Object[] a) {
		return new Object() {
			@Override
			public String toString() {
				if (a == null) {
					return "null";
				}

				int iMax = a.length - 1;
				if (iMax == -1) {
					return "[]";
				}

				StringBuilder b = new StringBuilder();
				b.append('[');
				for (int i = 0; ; i++) {
					if (a[i] == this) {
						b.append(super.toString());
					} else {
						b.append(String.valueOf(a[i]));
					}
					if (i == iMax) {
						return b.append(']').toString();
					}
					b.append(", ");
				}
			}
		};
	}

	public static Object lateToString(final Throwable t) {
		return new Object() {
			@Override
			public String toString() {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				t.printStackTrace(pw);
				return sw.toString();
			}
		};
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void println(String s) {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printlnImpl(s);
					conclude(true);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
	}

	public void println(char[] chars) {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printlnImpl(chars);
					conclude(true);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
	}

	public void println() {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printlnImpl();
					conclude(true);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
	}

	public void println(Object o) {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printlnImpl(o);
					conclude(true);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
	}

	public void println(Object o1, Object o2) {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printlnImpl(o1, o2);
					conclude(true);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
	}

	public void println(Object o1, Object o2, Object o3) {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printlnImpl(o1, o2, o3);
					conclude(true);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
	}

	public void println(Object... oi) {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printlnImpl(oi);
					conclude(true);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
	}

	public void print(String s) {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printImpl(s);
					conclude(false);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
	}

	public void print(char[] chars) {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printImpl(chars);
					conclude(false);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
	}

	public void print(Object o) {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printImpl(o);
					conclude(false);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
	}

	public void print(Object o1, Object o2) {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printImpl(o1, o2);
					conclude(false);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
	}

	public void print(Object o1, Object o2, Object o3) {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printImpl(o1, o2, o3);
					conclude(false);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
	}

	public void print(Object o1, Object... oi) {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printImpl(o1, oi);
					conclude(false);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
	}

	public Printer printf(String format, Object ... args) {
		return printf(Locale.getDefault(Locale.Category.FORMAT), format, args);
	}

	public Printer printf(Locale l, String format, Object ... args) {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printfImpl(l, format, args);
					conclude(true);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
		return this;
	}

	public void printStackTrace(Throwable t) {
		if (enabled) {
			synchronized (lock) {
				try {
					prepare();
					printStackTraceImpl(t);
					conclude(true);
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					trouble = true;
				}
			}
		}
	}

	public boolean checkError() {
		synchronized (lock) {
			return trouble;
		}
	}

	protected void setError() {
		trouble = true;
	}

	protected void clearError() {
		trouble = false;
	}

	public abstract void flush();
	public abstract void close();

	protected abstract void printStackTraceImpl(Throwable t);
	protected void prepare() throws IOException {};
	protected void conclude(boolean flush) throws IOException {};

	protected abstract void printImpl(String s) throws IOException;
	protected abstract void printImpl(char[] chars) throws IOException;
	protected void printImpl(Object o) throws IOException {
		printImpl(String.valueOf(o));
	}
	protected void printImpl(Object o1, Object o2) throws IOException {
		printImpl(o1);
		printImpl(o2);
	}
	protected void printImpl(Object o1, Object o2, Object o3) throws IOException {
		printImpl(o1);
		printImpl(o2);
		printImpl(o3);
	}
	protected void printImpl(Object o1, Object[] oi) throws IOException {
		printImpl(o1);
		for (Object o : oi) {
			printImpl(o);
		}
	}

	protected void printlnImpl(String s) throws IOException {
		printImpl(s);
		printlnImpl();
	}
	protected void printlnImpl(char[] chars) throws IOException {
		printImpl(chars);
		printlnImpl();
	}
	protected void printlnImpl() throws IOException {
		printImpl(System.lineSeparator());
	}
	protected void printlnImpl(Object o) throws IOException {
		printImpl(o);
		printlnImpl();
	}
	protected void printlnImpl(Object o1, Object o2) throws IOException {
		printImpl(o1);
		printlnImpl(o2);
	}
	protected void printlnImpl(Object o1, Object o2, Object o3) throws IOException {
		printImpl(o1);
		printImpl(o2);
		printlnImpl(o3);
	}
	protected void printlnImpl(Object[] oi) throws IOException {
		for (Object o : oi) {
			printImpl(o);
		}
		printlnImpl();
	}

	protected abstract void printfImpl(Locale l, String format, Object[] args) throws IOException;
}

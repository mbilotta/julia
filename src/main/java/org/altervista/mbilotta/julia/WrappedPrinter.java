package org.altervista.mbilotta.julia;

import java.io.IOException;
import java.util.Locale;

public class WrappedPrinter extends Printer {

	private final Printer printer;

	public WrappedPrinter(Printer printer) {
		super(printer.lock);
		this.printer = printer;
	}

	@Override
	public void flush() {
		printer.flush();
	}

	@Override
	public void close() {
		printer.close();
	}

	@Override
	protected void printStackTraceImpl(Throwable t) {
		printer.printStackTraceImpl(t);
	}

	@Override
	protected void printImpl(String s) throws IOException {
		printer.printImpl(s);
	}

	@Override
	protected void printImpl(char[] chars) throws IOException {
		printer.printImpl(chars);
	}

	@Override
	protected void printfImpl(Locale l, String format, Object[] args) throws IOException {
		printer.printImpl(l, format, args);
	}   
}
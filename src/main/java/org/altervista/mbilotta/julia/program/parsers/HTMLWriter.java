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

package org.altervista.mbilotta.julia.program.parsers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.LinkedList;


public class HTMLWriter {
	
	private PrintWriter out;
	private String tabString;
	private Deque<String> stack;

	public HTMLWriter(PrintWriter out, String doctypeString) {
		this(out, doctypeString, "\t");
	}

	public HTMLWriter(PrintWriter out, String doctypeString, String tabString) {
		this.out = out;
		this.tabString = tabString;
		this.stack = new LinkedList<>();

		out.println(doctypeString);
	}
	
	public void println(String s, boolean escape) {
		printTabs();
		if (escape) {
			printEscaped(s);
			out.println();
		} else {
			out.println(s);
		}
	}

	public void open(String tag) {
		printTabs();
		out.print('<');
		out.print(tag);
		out.println('>');

		stack.push(tag);
	}

	public void open(String tag, String... attributes) {
		printTabs();
		out.print('<');
		out.print(tag);
		printAttributes(attributes);
		out.println('>');

		stack.push(tag);
	}

	public void openAndClose(String tag, String content, boolean escape) {
		printTabs();
		out.print('<');
		out.print(tag);
		out.print('>');
		if (escape)
			printEscaped(content);
		else
			out.print(content);
		out.print('<');
		out.print('/');
		out.print(tag);
		out.println('>');
	}

	public void openAndClose(String tag, String content, boolean escape, String... attributes) {
		printTabs();
		out.print('<');
		out.print(tag);
		printAttributes(attributes);
		out.print('>');
		if (escape)
			printEscaped(content);
		else
			out.print(content);
		out.print('<');
		out.print('/');
		out.print(tag);
		out.println('>');
	}

	public void openAndClose(String tag) {
		printTabs();
		out.print('<');
		out.print(tag);
		out.print('/');
		out.println('>');
	}

	public void openAndClose(String tag, String... attributes) {
		printTabs();
		out.print('<');
		out.print(tag);
		printAttributes(attributes);
		out.print('/');
		out.println('>');
	}

	public void close() {
		String tag = stack.pop();
		printTabs();
		out.print('<');
		out.print('/');
		out.print(tag);
		out.println('>');
	}
	
	public void close(String tag) {
		assert tag.equals(stack.peek()) : stack.peek();

		stack.pop();
		printTabs();
		out.print('<');
		out.print('/');
		out.print(tag);
		out.println('>');
	}

	public boolean isOpen(String tag) {
		return tag.equals(stack.peek());
	}

	public PrintWriter getPrintWriter() {
		return out;
	}

	private void printTabs() {
		int tabCount = stack.size();
		for (int i = 0; i < tabCount; i++)
			out.print(tabString);
	}

	private void printAttributes(String[] attributes) {
		int attributeCount = attributes.length;
		if (attributeCount > 0) {
			for (int i = 0; i < attributeCount; i++) {
				if (i % 2 == 0) {
					out.print(' ');
					out.print(attributes[i]);
				} else {
					out.print('=');
					out.print('"');
					out.print(attributes[i]);
					out.print('"');
				}
			}
		}
	}

	private void printEscaped(String content) {
		int length = content.length();
		int offset = 0;
		int i = 0;
		for ( ; i < length; i++) {
			char c = content.charAt(i);
			switch (c) {
			case '"': out.write(content, offset, i - offset); out.write("&quot;"); offset = i + 1; break;
			case '\'': out.write(content, offset, i - offset); out.write("&apos;"); offset = i + 1; break;
			case '<': out.write(content, offset, i - offset); out.write("&lt;"); offset = i + 1; break;
			case '>': out.write(content, offset, i - offset); out.write("&gt;"); offset = i + 1; break;
			case '&': out.write(content, offset, i - offset); out.write("&amp;"); offset = i + 1; break;
			}
		}
		out.write(content, offset, i - offset);
	}

	public static void main(String[] args) throws IOException {
		PrintWriter out = new PrintWriter(new File("prova.html"), "UTF-8");
		HTMLWriter html = new HTMLWriter(out, "<!DOCTYPE html>");
		html.open("html");
		html.open("head");
		html.openAndClose("meta", "charset", "UTF-8");
		html.openAndClose("title", "Pagina di prova & ancora prova", true);
		html.close();
		html.open("body");
		html.openAndClose("h1", "Pagina di prova", false, "style", "text-align: center;");
		html.openAndClose("p", "Questa &egrave; una pagina di prova", false);
		html.openAndClose("hr");
		html.close();
		html.close();
		out.close();
	}
}

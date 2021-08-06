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

	public TagAttributes attrs() {
		return new TagAttributes();
	}

	public TagAttributes attrs(String name, String value) {
		return new TagAttributes().attr(name, value);
	}

	public void tag(String tag) {
		openAndClose(tag);
	}

	public void tag(String tag, TagBody body) {
		open(tag);
		body.write();
		close(tag);
	}

	public void tag(String tag, TagAttributes attrs, TagBody body) {
		open(tag, attrs.toArray());
		body.write();
		close(tag);
	}

	public void tag(String tag, String content) {
		openAndClose(tag, content, true);
	}

	public void tag(String tag, TagAttributes attrs, String content) {
		openAndClose(tag, content, true, attrs.toArray());
	}

	public void tag(String tag, TagAttributes attrs) {
		openAndClose(tag, attrs.toArray());
	}

	public void text(String text) {
		println(text, true);
	}

	public TagBody textBody(String text) {
		return () -> {
			text(text);
		};
	}

	public void html(TagBody body) {
		tag("html", body);
	}

	public void head(TagBody body) {
		tag("head", body);
	}

	public void title(String title) {
		tag("title", title);
	}

	public void body(TagBody body) {
		tag("body", body);
	}

	public void span(String clazz, String text) {
		tag("span", attrs().clazz(clazz), text);
	}

	public void span(String text) {
		tag("span", text);
	}

	public void div(String clazz, TagBody body) {
		tag("div", attrs().clazz(clazz), body);
	}

	public void div(TagBody body) {
		tag("div", body);
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
		HTMLWriter w = new HTMLWriter(out, "<!DOCTYPE html>");
		w.html(() -> {
			w.head(() -> {
				w.tag("meta", w.attrs("charset", "UTF-8"));
				w.title("Pagina di prova & ancora prova");
			});
			w.body(() -> {
				w.tag("h1", w.attrs().style("text-align: center;"), "Pagina di prova");
				w.tag("p", "Questa è una pagina di prova");
				w.tag("hr");
			});
		});
		out.close();
	}
}

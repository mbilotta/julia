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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.altervista.mbilotta.julia.program.LockedFile;


public final class DocumentationWriter {
	
	private Plugin plugin;
	private String description;
	private List<Parameter<?>.Validator> validators;

	DocumentationWriter(Plugin plugin,
			String description,
			List<Parameter<?>.Validator> validators) {
		this.plugin = plugin;
		this.description = description;
		this.validators = validators;
	}

	private static String getStylesheetPath(String id) {
		StringBuilder sb = new StringBuilder();
		for (int i = Paths.get(id).getNameCount() - 1; i > 0; i--) {
			sb.append("../");
		}
		sb.append("julia.css");
		return sb.toString();
	}

	public boolean writeTo(LockedFile file) throws IOException {
		HTMLWriter out =
				new HTMLWriter(new PrintWriter(file.writeCharsTo(false, "UTF-8")), "<!DOCTYPE html>", "    ");
		out.open("html");
		out.open("head");

		out.openAndClose("meta", "charset", "UTF-8");
		out.openAndClose("link", "type", "text/css", "rel", "stylesheet", "href", getStylesheetPath(plugin.getId()));

		out.openAndClose("title", plugin.getName(), false);

		out.close(); // head
		
		out.open("body", "id", "page-top");

		out.openAndClose("h1", plugin.getName(), false);

		out.openAndClose("h2", "Description", false);
		out.openAndClose("p", description, false);
		
		out.openAndClose("h3", "Class", false);
		out.openAndClose("code", plugin.getType().getCanonicalName(), false);
		
		out.openAndClose("h3", "Parameters", false);
		if (validators.size() > 0) {
			out.open("table", "border", "1");
			out.open("tbody");
			out.open("tr");
			out.openAndClose("th", "Name", false);
			out.openAndClose("th", "Type", false);
			out.openAndClose("th", "Java Type", false);
			out.openAndClose("th", "Default", false);
			if (plugin.getFamily() == PluginFamily.representation)
				out.openAndClose("th", "Previewable", false);
			out.close(); // tr
			for (Parameter<?>.Validator validator : validators) {
				out.open("tr");

				out.open("td");
				out.openAndClose("a", validator.getParameterName(), false, "href", '#' + validator.getParameterId());
				out.close(); // td

				out.openAndClose("td", validator.getXMLParameterType(), false, "style", "font-family:monospace");

				out.openAndClose("td", validator.getParameterType().getCanonicalName(), false, "style", "font-family:monospace");

				validator.writeValueToHTML(out, validator.getParameterDefault());

				if (plugin.getFamily() == PluginFamily.representation)
					out.openAndClose("td", validator.isParameterPreviewable() ? "Yes" : "No", false);

				out.close(); // tr
			}
			out.close(); // tbody
			out.close(); // table
		} else {
			out.openAndClose("p", "None.", false);
		}

		out.openAndClose("h3", "Authors", false);
		List<Author> authors = plugin.getAuthors();
		if (authors.size() > 0) {
			out.open("ul");
			for (Author author : plugin.getAuthors()) {
				out.open("li");
				out.println(author.getName(), true);
				out.openAndClose("br");
				String contact = author.getContact();
				out.openAndClose("a", contact.startsWith("mailto:") ? contact.substring(7) : contact, true, "href", contact);
				out.close(); // li
			}
			out.close(); // ul
		} else {
			out.openAndClose("p", "Unknown.", false);
		}

		out.openAndClose("hr");
		
		for (Parameter<?>.Validator validator : validators) {
			out.openAndClose("h2", validator.getParameterName(), false, "id", validator.getParameterId());
			out.openAndClose("p", validator.getParameterDescription(), false);
			validator.writeConstraintsToHTML(out);
			out.openAndClose("h3", "Hints", false);
			out.open("table", "border", "1");
			out.open("tbody");

			out.open("tr");
			out.openAndClose("th", "Hint#", false);
			out.openAndClose("th", "Value", false);
			out.openAndClose("th", "Group(s)", false);
			out.close(); // tr

			int i = 1;
			for (Object hint : validator.getParameter().getHints()) {
				out.open("tr");
				out.openAndClose("td", i == 1 ? "1 (Default)" : Integer.toString(i), false, "style", "text-align:center");
				validator.writeValueToHTML(out, hint);
				for (Map.Entry<String, ?> e : validator.getReferencedGroups().entrySet()) {
					if (e.getValue() != null && e.getValue().equals(hint)) {
						if (!out.isOpen("td")) {
							out.open("td");
						}
						out.openAndClose("a", e.getKey(), true, "href", "#hint-groups");
					}
				}
				if (out.isOpen("td")) {
					out.close(); // td
				}
				out.close(); // tr
				i++;
			}

			out.close(); // tbody
			out.close(); // table
			
			out.open("p", "style", "text-align:right");
			out.openAndClose("a", "Back to top", false, "href", "#page-top");
			out.close(); // p

			out.openAndClose("hr");
		}

		Map<String, List<Object>> hintGroups = plugin.getHintGroups();
		out.openAndClose("h2", "Hint groups", false, "id", "hint-groups");
		if (hintGroups.isEmpty()) {
			out.openAndClose("p", "None.", false);
		} else {
			out.open("table", "border", "1");

			out.open("tbody");

			out.open("tr");
			out.openAndClose("th", "Name", false);
			for (Parameter<?>.Validator parser : validators) {
				out.openAndClose("th", parser.getParameterName(), false);
			}
			out.close(); // tr
			
			for (Map.Entry<String, List<Object>> e : hintGroups.entrySet()) {
				out.open("tr");
				out.openAndClose("td", e.getKey(), true);
				Iterator<Object> i = e.getValue().iterator();
				for (Parameter<?>.Validator validator : validators) {
					Object value = i.next();
					if (value == null) {
						out.openAndClose("td", "<em>null</em>", false);
					} else {
						validator.writeValueToHTML(out, value);
					}
				}
				out.close(); // tr
			}

			out.close(); // tbody
			out.close(); // table
		}

		out.open("p", "style", "text-align:right");
		out.openAndClose("a", "Back to top", false, "href", "#page-top");
		out.close(); // p

		out.close(); // body
		out.close("html"); // html

		return !out.getPrintWriter().checkError();
	}
}

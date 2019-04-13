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

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;

import org.altervista.mbilotta.julia.program.Classpath;
import org.altervista.mbilotta.julia.program.Profile;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


public class ClasspathParser extends Parser<Classpath> {

	public static final String CLASSPATH_NS_URI = "http://mbilotta.altervista.org/julia/classpath";
	
	private final Profile profile;

	public ClasspathParser(Profile profile) throws SAXException, ParserConfigurationException {
		super("classpath.xsd");
		this.profile = profile;
	}

	@Override
	protected Path relativize(Path path) {
		return profile.relativize(path);
	}

	@Override
	protected Classpath validate(Document dom)
			throws ValidationException, InterruptedException {
		Element root = dom.getDocumentElement();
		XmlPath currentPath = new XmlPath(root);
		if (!(root.getNamespaceURI().equals(CLASSPATH_NS_URI) && root.getLocalName().equals("classpath"))) {
			fatalError(ValidationException.atStartOf(
					currentPath,
					"Invalid root element: " + root));
			return null;
		}

		Classpath rv = new Classpath();
		Element offset = (Element) root.getFirstChild();
		int index = 1;
		while (offset != null) {
			currentPath = currentPath.getChild(offset, index);
			try {
				Path path = Paths.get(getNodeValue(offset));
				boolean jarFolder = offset.getAttributeNode("jarFolder") != null;
				if (jarFolder) {
					rv.addJarFolderEntry(path);
				} else {
					rv.addEntry(path);
				}
			} catch (InvalidPathException e) {
				error(ValidationException.atEndOf(currentPath, "Not a valid path.", e));
			}

			offset = (Element) offset.getNextSibling();
			currentPath = currentPath.getParent();
			index++;
		}

		if (rv.isEmpty()) {
			fatalError(ValidationException.atEndOf(currentPath, "Classpath is empty."));
		}
		
		return rv;
	}
}

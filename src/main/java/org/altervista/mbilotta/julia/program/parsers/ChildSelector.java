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

import org.w3c.dom.Attr;
import org.w3c.dom.Element;


public class ChildSelector {
	
	private String name;
	private String localName;
	private boolean attribute;
	private int index;
	private String attributeName;
	private String attributeValue;

	public ChildSelector(Attr child) {
		this.name = child.getName();
		this.localName = child.getLocalName();
		this.attribute = true;
	}

	public ChildSelector(Element child) {
		this.name = child.getTagName();
		this.localName = child.getLocalName();
		this.attribute = false;
	}

	public ChildSelector(String name, boolean attribute) {
		this.name = name;
		this.localName = removePrefix(name);
		this.attribute = attribute;
	}

	public ChildSelector(Element child, int index) {
		this(child);
		this.index = index;
	}

	public ChildSelector(String name, int index) {
		this(name, false);
		this.index = index;
	}

	public ChildSelector(Element child, String attributeName, String attributeValue) {
		this(child);
		this.attributeName = attributeName;
		this.attributeValue = attributeValue;
	}

	public ChildSelector(Element child, Attr attribute) {
		this(child, attribute.getName(), attribute.getValue());
	}

	public ChildSelector(String name, String attributeName, String attributeValue) {
		this(name, false);
		this.attributeName = attributeName;
		this.attributeValue = attributeValue;
	}

	public String getName() { return name; }
	public String getLocalName() { return localName; }
	public boolean isAttributeChild() { return attribute; }
	public String getAttributeName() { return attributeName; }
	public String getAttributeValue() { return attributeValue; }
	public int getIndex() { return index; } 

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (attribute)
			sb.append('@');
		sb.append(name);
		if (index > 0) {
			sb.append('[').append(index).append(']');
		} else if (attributeName != null) {
			sb.append("[@").append(attributeName).append("='")
				.append(attributeValue).append("']");
		}
		
		return sb.toString();
	}

	private static String removePrefix(String name) {
		int colonIndex = name.indexOf(':');
		if (colonIndex != -1)
			return name.substring(colonIndex + 1);
		return name;
	}
}

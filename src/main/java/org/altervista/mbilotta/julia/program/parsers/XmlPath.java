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

import java.util.LinkedList;
import java.util.ListIterator;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;


public class XmlPath implements Iterable<ChildSelector> {
	
	private XmlPath parent;
	private ChildSelector childSelector;
	
	public XmlPath(Element root) {
		childSelector = new ChildSelector(root);
	}

	public XmlPath(String root) {
		childSelector = new ChildSelector(root, false);
	}

	private XmlPath(XmlPath parent, ChildSelector childSelector) {
		this.parent = parent;
		this.childSelector = childSelector;
	}
	
	public XmlPath getChild(Attr child) {
		return new XmlPath(this, new ChildSelector(child));
	}

	public XmlPath getAttributeChild(String name) {
		return new XmlPath(this, new ChildSelector(name, true));
	}

	public XmlPath getChild(Element child) {
		return new XmlPath(this, new ChildSelector(child));
	}

	public XmlPath getChild(String name) {
		return new XmlPath(this, new ChildSelector(name, false));
	}

	public XmlPath getChild(Element child, int index) {
		return new XmlPath(this, new ChildSelector(child, index));
	}

	public XmlPath getChild(String name, int index) {
		return new XmlPath(this, new ChildSelector(name, index));
	}
	
	public XmlPath getChild(Element child, String attributeName, String attributeValue) {
		return new XmlPath(this, new ChildSelector(child, attributeName, attributeValue));
	}

	public XmlPath getChild(Element child, Attr attribute) {
		return new XmlPath(this, new ChildSelector(child, attribute));
	}

	public XmlPath getChild(String name, String attributeName, String attributeValue) {
		return new XmlPath(this, new ChildSelector(name, attributeName, attributeValue));
	}

	public XmlPath getParent() {
		return parent;
	}

	public boolean isRoot() {
		return parent == null;
	}
	
	public String getName() { return childSelector.getName(); }
	public String getLocalName() { return childSelector.getLocalName(); }
	public boolean isAttributeChild() { return childSelector.isAttributeChild(); }
	public String getAttributeName() { return childSelector.getAttributeName(); }
	public String getAttributeValue() { return childSelector.getAttributeValue(); }
	public int getIndex() { return childSelector.getIndex(); } 

	public ListIterator<ChildSelector> iterator() {
		LinkedList<ChildSelector> l = new LinkedList<>();
		XmlPath p = this;
		do {
			l.addFirst(p.childSelector);
			p = p.parent;
		} while (p != null);

		return l.listIterator();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(100);
		for (ChildSelector childSelector : this) {
			sb.append('/').append(childSelector);
		}
		
		return sb.toString();
	}
}

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;


public final class Author implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String name;
	private final String contact;

	public Author(String name, String contact) {
		this.name = name;
		this.contact = contact;
	}

	public static List<Author> inspectAnnotations(Class<?> type) {
		return inspectAnnotations(type, null);
	}

	public static List<Author> inspectAnnotations(Class<?> type, UnaryOperator<Author> replace) {
		if (replace == null) replace = UnaryOperator.identity();

		org.altervista.mbilotta.julia.Author[] authors = type.getAnnotationsByType(org.altervista.mbilotta.julia.Author.class);
		List<Author> rv = new ArrayList<>(authors.length);
		for (org.altervista.mbilotta.julia.Author author : authors) {
			rv.add(replace.apply(new Author(author.name(), author.contact())));
		}
		return rv;
	}

	public String getName() {
		return name;
	}

	public String getContact() {
		return contact;
	}
	
	public int hashCode() {
		int hashCode = 17;
		hashCode = 31 * hashCode + name.hashCode();
		hashCode = 31 * hashCode + contact.hashCode();
		return hashCode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		
		if (o instanceof Author) {
			Author a = (Author) o;
			return name.equals(a.name) && contact.equals(a.contact);
		}
		
		return false;
	}

	public String toString() {
		return getClass().getSimpleName() + "[name=" + name + ", contact=" + contact + "]";
	}
}

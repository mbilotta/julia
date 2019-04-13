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

package org.altervista.mbilotta.julia.program;

import java.util.AbstractCollection;
import java.util.HashMap;
import java.util.Iterator;


public final class Cache<T> extends AbstractCollection<T> {
	
	private final HashMap<Object, T> cache = new HashMap<>();

	public Cache() {
	}

	public T replace(T o) {
		if (o == null)
			return null;

		T cached = cache.putIfAbsent(o, o);
		if (cached == null) {
			return o;
		}

		return cached;
	}

	public String toString() {
		return cache.keySet().toString();
	}

	public Iterator<T> iterator() {
		return (Iterator<T>) cache.keySet().iterator();
	}

	public boolean add(T o) {
		if (o == null)
			throw new NullPointerException();

		return cache.putIfAbsent(o, o) == null;
	}

	public boolean contains(Object o) {
		return cache.containsKey(o);
	}

	public boolean remove(Object o) {
		return cache.remove(o) != null;
	}

	public void clear() {
		cache.clear();
	}

	public int size() {
		return cache.size();
	}
}

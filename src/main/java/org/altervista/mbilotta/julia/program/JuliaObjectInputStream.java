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

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.Gradient;
import org.altervista.mbilotta.julia.program.parsers.Author;



public class JuliaObjectInputStream extends ObjectInputStream {

	private final ClassLoader classLoader;
	private final Cache<Author> authorCache;
	private final Cache<Decimal> decimalCache;
	private final Cache<Color> colorCache;
	private final Cache<Gradient> gradientCache;

	public JuliaObjectInputStream(InputStream in,
			ClassLoader classLoader,
			Cache<Author> authorCache,
			Cache<Decimal> decimalCache,
			Cache<Color> colorCache,
			Cache<Gradient> gradientCache) throws IOException, SecurityException {
		super(in);
		enableResolveObject(true);
		this.classLoader = classLoader;
		this.authorCache = authorCache;
		this.decimalCache = decimalCache;
		this.colorCache = colorCache;
		this.gradientCache = gradientCache;
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		String name = desc.getName();
		try {
			return Class.forName(name, false, classLoader);
		} catch (ClassNotFoundException e) {
			switch (name) {
			case "boolean": return boolean.class;
			case "byte":	return byte.class;
			case "char":	return char.class;
			case "short":	return short.class;
			case "int":		return int.class;
			case "long":	return long.class;
			case "float":	return float.class;
			case "double":	return double.class;
			case "void":	return void.class;
			default: throw e;
			}
		}
	}

	@Override
	protected Object resolveObject(Object o) {
		Object replacement = o;
		if (o instanceof Decimal) {
			if (decimalCache != null) replacement = decimalCache.replace((Decimal) o);
		} else if (o instanceof Color) {
			if (colorCache != null) replacement = colorCache.replace((Color) o);
		} else if (o instanceof Gradient) {
			if (gradientCache != null) replacement = gradientCache.replace((Gradient) o);
		}

		return replacement;
	}

	public Author resolveAuthor(Author author) {
		return authorCache.replace(author);
	}	
}
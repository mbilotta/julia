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

import org.altervista.mbilotta.julia.Formula;
import org.altervista.mbilotta.julia.NumberFactory;
import org.altervista.mbilotta.julia.Representation;


public enum PluginFamily {
	formula(Formula.class),
	representation(Representation.class),
	numberFactory(NumberFactory.class),
	alias(Object.class);
	
	private final Class<?> interfaceType;
	
	private PluginFamily(Class<?> interfaceType) {
		this.interfaceType = interfaceType;
	}
	
	public Class<?> getInterfaceType() {
		return interfaceType;
	}
}
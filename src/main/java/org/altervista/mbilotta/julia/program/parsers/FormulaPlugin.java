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

import org.altervista.mbilotta.julia.program.JuliaSetPoint;
import org.altervista.mbilotta.julia.program.Rectangle;


public final class FormulaPlugin extends Plugin {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Rectangle defaultMandelbrotSetRectangle;
	private final Rectangle defaultJuliaSetRectangle;
	private final JuliaSetPoint defaultJuliaSetPoint;

	FormulaPlugin(String id, Class<?> type,
			Rectangle defaultMandelbrotSetRectangle,
			Rectangle defaultJuliaSetRectangle,
			JuliaSetPoint defaultJuliaSetPoint) {
		super(id, type);
		this.defaultMandelbrotSetRectangle = defaultMandelbrotSetRectangle;
		this.defaultJuliaSetRectangle = defaultJuliaSetRectangle;
		this.defaultJuliaSetPoint = defaultJuliaSetPoint;
	}

	public PluginFamily getFamily() {
		return PluginFamily.formula;
	}

	public Rectangle getDefaultMandelbrotSetRectangle() {
		return defaultMandelbrotSetRectangle;
	}

	public Rectangle getDefaultJuliaSetRectangle() {
		return defaultJuliaSetRectangle;
	}

	public JuliaSetPoint getDefaultJuliaSetPoint() {
		return defaultJuliaSetPoint;
	}

	@Override
	public String toString() {
		return toStringBuilder()
				.append(", defaultMandelbrotSetRectangle=").append(defaultMandelbrotSetRectangle)
				.append(", defaultJuliaSetRectangle=").append(defaultJuliaSetRectangle)
				.append(", defaultJuliaSetPoint=").append(defaultJuliaSetPoint)
				.append(']').toString();
	}
}

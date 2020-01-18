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


public class DomValidationException extends Exception {
	
	public static final int START_OF_ELEMENT = 0;
	public static final int END_OF_ELEMENT = 1;

	private final XmlPath elementPath;
	private final int position;

	public DomValidationException(XmlPath elementPath, int position, String message) {
		super(message);
		this.position = position;
		this.elementPath = elementPath;
	}

	public DomValidationException(XmlPath elementPath, int position, String message, Throwable cause) {
		super(message, cause);
		this.position = position;
		this.elementPath = elementPath;
	}

	public XmlPath getElementPath() {
		return elementPath;
	}
	
	public int getPosition() {
		return position;
	}

	public static DomValidationException atStartOf(XmlPath elementPath, String message) {
		return new DomValidationException(elementPath, START_OF_ELEMENT, message);
	}

	public static DomValidationException atStartOf(XmlPath elementPath, String message, Throwable cause) {
		return new DomValidationException(elementPath, START_OF_ELEMENT, message, cause);
	}

	public static DomValidationException atEndOf(XmlPath elementPath, String message) {
		return new DomValidationException(elementPath, END_OF_ELEMENT, message);
	}

	public static DomValidationException atEndOf(XmlPath elementPath, String message, Throwable cause) {
		return new DomValidationException(elementPath, END_OF_ELEMENT, message, cause);
	}

	public String getCompleteMessage() {
		return getMessage() + " at " + (position == START_OF_ELEMENT ? "start of " : "end of ") + elementPath;
	}
}

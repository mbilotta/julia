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

import java.lang.reflect.Method;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.NumberFactory;


public final class RepresentationPlugin extends Plugin {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RepresentationPlugin(String id, Class<?> type) {
		super(id, type);
	}

	public PluginFamily getFamily() {
		return PluginFamily.representation;
	}

	public boolean hasPreviewableParameters() {
		int numOfParams = getNumOfParameters();
		for (int i = 0; i < numOfParams; i++) {
			if (getParameter(i).isPreviewable()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void populate(Object target, Object[] parameterValues, NumberFactory numberFactory)
			throws ReflectiveOperationException {
		int numOfParameters = getNumOfParameters();
		for (int i = 0; i < numOfParameters; i++) {
			Parameter<?> parameter = getParameter(i);
			Method setter = parameter.getSetterMethod();
			Object argument = parameterValues[i];
			if (parameter instanceof RealParameter && !parameter.isPreviewable()) {
				argument = numberFactory.valueOf((Decimal) argument);
			}
			setter.invoke(target, argument);
		}
	}
}

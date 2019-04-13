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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.NumberFactory;
import org.altervista.mbilotta.julia.Previewable;


public final class RepresentationPlugin extends Plugin {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private transient boolean[] previewables;

	public RepresentationPlugin(String id, Class<?> type) {
		super(id, type);
	}

	public PluginFamily getFamily() {
		return PluginFamily.representation;
	}

	@Override
	void setParameters(List<Parameter<?>.Validator> domParsers) /* throws NoSuchMethodException */ {
		super.setParameters(domParsers);

		int i = 0;
		previewables = new boolean[domParsers.size()];
		for (Parameter<?>.Validator domParser : domParsers) {
			previewables[i++] = domParser.isParameterPreviewable();
		}
	}

	public boolean isPreviewable(Parameter<?> parameter) {
		assert parameter.getPlugin() == this;
		return previewables[parameter.getIndex()];
	}

	public boolean hasPreviewableParameters() {
		int numOfParams = getNumOfParameters();
		for (int i = 0; i < numOfParams; i++) {
			if (previewables[i])
				return true;
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
			if (parameter instanceof RealParameter && !previewables[i]) {
				argument = numberFactory.valueOf((Decimal) argument);
			}
			setter.invoke(target, argument);
		}
	}

	public String toString() {
		return toStringBuilder()
				.append(", previewables=").append(Arrays.toString(previewables))
				.append(']').toString();
	}

	private void readObject(ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		in.defaultReadObject();

		int numOfParams = getNumOfParameters();
		previewables = new boolean[numOfParams];
		for (int i = 0; i < numOfParams; i++) {
			previewables[i] = getParameter(i).getSetterMethod().isAnnotationPresent(Previewable.class);
		}
	}
}

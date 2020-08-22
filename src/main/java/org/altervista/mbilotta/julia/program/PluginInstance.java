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

import static org.altervista.mbilotta.julia.Utilities.join;
import static org.altervista.mbilotta.julia.Utilities.read;
import static org.altervista.mbilotta.julia.Utilities.readNonNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.stream.IntStream;

import javax.swing.JComponent;

import org.altervista.mbilotta.julia.NumberFactory;
import org.altervista.mbilotta.julia.Out;
import org.altervista.mbilotta.julia.Printer;
import org.altervista.mbilotta.julia.program.parsers.FormulaPlugin;
import org.altervista.mbilotta.julia.program.parsers.NumberFactoryPlugin;
import org.altervista.mbilotta.julia.program.parsers.Parameter;
import org.altervista.mbilotta.julia.program.parsers.Plugin;
import org.altervista.mbilotta.julia.program.parsers.RepresentationPlugin;


public final class PluginInstance<P extends Plugin> implements Cloneable {

	private final P plugin;
	private final Object[] parameterValues;

	private PluginInstance(Object[] parameterValues, P plugin) {
		this.plugin = plugin;
		this.parameterValues = parameterValues;
	}

	public PluginInstance(P plugin) {
		int numOfParameters = plugin.getNumOfParameters();
		Object[] parameterValues = new Object[numOfParameters];
		for (int i = 0; i < numOfParameters; i++) {
			parameterValues[i] = plugin.getParameter(i).getHint(0);
		}
		this.plugin = plugin;
		this.parameterValues = parameterValues;
	}

	public PluginInstance(P plugin, Object... parameterValues) {
		this.plugin = plugin;
		this.parameterValues = Arrays.copyOf(parameterValues, parameterValues.length);
	}

	public PluginInstance(PluginInstance<P> other) {
		this(other.plugin, other.parameterValues);
	}

	public PluginInstance(P plugin, JComponent[] editors) {
		int numOfParameters = plugin.getNumOfParameters();
		assert numOfParameters == editors.length;
		Object[] parameterValues = new Object[numOfParameters];
		for (int i = 0; i < numOfParameters; i++) {
			parameterValues[i] = plugin.getParameter(i).getEditorValue(editors[i]);
		}
		this.plugin = plugin;
		this.parameterValues = parameterValues;
	}

	public JComponent[] createEditors() {
		int numOfParameters = plugin.getNumOfParameters();
		JComponent[] editors = new JComponent[numOfParameters];
		for (int i = 0; i < numOfParameters; i++) {
			editors[i] = plugin.getParameter(i).createEditor(parameterValues[i]);
		}
		return editors;
	}

	public void setEditorValues(JComponent[] editors) {
		int numOfParameters = plugin.getNumOfParameters();
		for (int i = 0; i < numOfParameters; i++) {
			plugin.getParameter(i).setEditorValue(editors[i], parameterValues[i]);
		}
	}

	public void setParameterValue(Parameter<?> parameter, Object value) {
		assert parameter.getPlugin() == plugin;
		assert parameter.acceptsValue(value);
		parameterValues[parameter.getIndex()] = value;
	}

	public Object getParameterValue(Parameter<?> parameter) {
		assert parameter.getPlugin() == plugin;
		return parameterValues[parameter.getIndex()];
	}

	public P getPlugin() {
		return plugin;
	}

	public Object create() throws ReflectiveOperationException {
		Object rv = plugin.getConstructor().newInstance((Object[]) null);
		plugin.populate(rv, parameterValues);
		return rv;
	}

	public Object create(NumberFactory numberFactory) throws ReflectiveOperationException {
		Object rv = plugin.getConstructor().newInstance((Object[]) null);
		plugin.populate(rv, parameterValues, numberFactory);
		return rv;
	}

	@Override
	public Object clone() {
		return new PluginInstance<P>(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof PluginInstance) {
			PluginInstance<P> other = (PluginInstance<P>) o;
			return equalsImpl(other, false);
		}

		return false;
	}

	public boolean equalsIgnorePreviewables(PluginInstance<P> other) {
		if (this == other) {
			return true;
		}

		return other != null && equalsImpl(other, true);
	}

	private boolean equalsImpl(PluginInstance<P> other, boolean ignorePreviewables) {
		if (plugin.equals(other.plugin)) {
			assert parameterValues.length == other.parameterValues.length;
			return IntStream.range(0, parameterValues.length).allMatch(i -> {
				Parameter<?> parameter = plugin.getParameter(i);
				return (ignorePreviewables && parameter.isPreviewable()) || parameterValues[i].equals(other.parameterValues[i]);
			});
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(plugin.getType().getCanonicalName());
		sb.append('[');
		if (plugin.getNumOfParameters() > 0) for (int i = 0; ; ) {
			sb.append(plugin.getParameter(i).getId())
				.append('=').append(parameterValues[i]);
			i++;
			if (i < plugin.getNumOfParameters()) sb.append(", ");
			else break;
		}
		return sb.append(']').toString();
	}

	public void printTo(Printer p) {
		p.print(plugin.getType().getCanonicalName(), '[');
		if (plugin.getNumOfParameters() > 0) for (int i = 0; ; ) {
			p.print(plugin.getParameter(i).getId(), '=', parameterValues[i]);
			i++;
			if (i < plugin.getNumOfParameters()) p.print(", ");
			else break;
		}
		p.print(']');
	}

	public static PluginInstance<NumberFactoryPlugin> readNumberFactory(NumberFactoryPlugin numberFactory,
			ObjectInputStream in, String entryName, Printer errorOutput,
			Out<Integer> errorCount, Out<Integer> fatalCount) {
		if (errorOutput == null) errorOutput = Printer.nullPrinter();
		if (errorCount == null) errorCount = Out.newOut();

		Object[] parameterValues = new Object[numberFactory.getNumOfParameters()];
		int ec = errorCount.get(0);
		int k = 0;
		int readCount = 0;
		String parameterId;
		try {
			parameterId = read(in, join("parameterId[", k, "]"), String.class);
			for ( ; readCount < numberFactory.getNumOfParameters() && parameterId != null; ) {
				Parameter<?> parameter = numberFactory.getParameter(parameterId);
				if (parameter == null) {
					errorOutput.println("- ", entryName, " (error): could not find parameter \"", parameterId, "\".");
					ec++;
				}

				Object value = readNonNull(in, join(numberFactory.getId(), ".", parameterId), Object.class);

				if (parameter != null) {
					int index = parameter.getIndex();
					if (parameterValues[index] == null) {
						if (parameter.acceptsValue(value)) {
							parameterValues[index] = value;
						} else {
							errorOutput.println("- ", entryName, " (error): value ", value,
									" violates constraints for parameter \"", parameterId, "\".");
							ec++;
							parameterValues[index] = parameter.getHint(0);
						}
						readCount++;
					} else {
						errorOutput.println("- ", entryName,
								" (error): multiple values for parameter \"", parameterId, "\".");
						ec++;
					}
				}

				parameterId = read(in, join("parameterId[", ++k, "]"), String.class);
			}
		} catch (ClassNotFoundException | IOException e) {
			errorOutput.print("- ", entryName, ": ");
			errorOutput.printStackTrace(e);
			ec++;
			parameterId = null;
		}

		if (readCount < numberFactory.getNumOfParameters()) {
			for (int i = 0; i < numberFactory.getNumOfParameters(); i++) {
				Parameter<?> parameter = numberFactory.getParameter(i);
				if (parameterValues[i] == null) {
					errorOutput.println("- ", entryName,
							" (error): missing value for parameter \"", parameter.getId(), "\".");
					ec++;
					parameterValues[i] = parameter.getHint(0);
				}
			}
		} else if (parameterId != null) {
			errorOutput.println("- ", entryName, " (error): extra parameters ignored.");
			ec++;
		}

		errorCount.set(ec);
		return new PluginInstance<NumberFactoryPlugin>(parameterValues, numberFactory);
	}

	public static PluginInstance<FormulaPlugin> readFormula(FormulaPlugin formula,
			ObjectInputStream in, String entryName, Printer errorOutput,
			Out<Integer> errorCount, Out<Integer> fatalCount) {
		if (errorOutput == null) errorOutput = Printer.nullPrinter();
		if (errorCount == null) errorCount = Out.newOut();
		if (fatalCount == null) fatalCount = Out.newOut();

		Object[] parameterValues = new Object[formula.getNumOfParameters()];
		int ec = errorCount.get(0);
		int fc = fatalCount.get(0);
		int k = 0;
		int readCount = 0;
		String parameterId;
		try {
			parameterId = read(in, join("parameterId[", k, "]"), String.class);
			for ( ; readCount < formula.getNumOfParameters() && parameterId != null; ) {
				Parameter<?> parameter = formula.getParameter(parameterId);
				if (parameter == null) {
					errorOutput.println("- ", entryName, " (fatal): could not find parameter \"", parameterId, "\".");
					fc++;
				}

				Object value = readNonNull(in, join(formula.getId(), ".", parameterId), Object.class);

				if (parameter != null) {
					int index = parameter.getIndex();
					if (parameterValues[index] == null) {
						if (parameter.acceptsValue(value)) {
							parameterValues[index] = value;
						} else {
							errorOutput.println("- ", entryName, " (fatal): value ", value,
									" violates constraints for parameter \"", parameterId, "\".");
							fc++;
							parameterValues[index] = parameter.getHint(0);
						}
						readCount++;
					} else {
						errorOutput.println("- ", entryName,
								" (fatal): multiple values for parameter \"", parameterId, "\".");
						fc++;
					}
				}

				parameterId = read(in, join("parameterId[", ++k, "]"), String.class);
			}
		} catch (ClassNotFoundException | IOException e) {
			errorOutput.print("- ", entryName, " (fatal): ");
			errorOutput.printStackTrace(e);
			fc++;
			parameterId = null;
		}

		if (readCount < formula.getNumOfParameters()) {
			for (int i = 0; i < formula.getNumOfParameters(); i++) {
				Parameter<?> parameter = formula.getParameter(i);
				if (parameterValues[i] == null) {
					errorOutput.println("- ", entryName,
							" (fatal): missing value for parameter \"", parameter.getId(), "\".");
					fc++;
					parameterValues[i] = parameter.getHint(0);
				}
			}
		} else if (parameterId != null) {
			errorOutput.println("- ", entryName, " (fatal): extra parameters ignored.");
			fc++;
		}

		errorCount.set(ec + fc);
		fatalCount.set(fc);
		return new PluginInstance<FormulaPlugin>(parameterValues, formula);
	}

	public static PluginInstance<RepresentationPlugin> readRepresentation(RepresentationPlugin representation,
			ObjectInputStream in, String entryName, Printer errorOutput,
			Out<Integer> errorCount, Out<Integer> fatalCount) {
		if (errorOutput == null) errorOutput = Printer.nullPrinter();
		if (errorCount == null) errorCount = Out.newOut();
		if (fatalCount == null) fatalCount = Out.newOut();

		Object[] parameterValues = new Object[representation.getNumOfParameters()];
		int ec = errorCount.get(0);
		int fc = fatalCount.get(0);
		int k = 0;
		int readCount = 0;
		String parameterId;
		try {
			parameterId = read(in, join("parameterId[", k, "]"), String.class);
			for ( ; readCount < representation.getNumOfParameters() && parameterId != null; ) {
				Parameter<?> parameter = representation.getParameter(parameterId);
				if (parameter == null) {
					errorOutput.println("- ", entryName, " (fatal): could not find parameter \"", parameterId, "\".");
					fc++;
				}

				Object value = readNonNull(in, join(representation.getId(), ".", parameterId), Object.class);

				if (parameter != null) {
					int index = parameter.getIndex();
					if (parameterValues[index] == null) {
						if (parameter.acceptsValue(value)) {
							parameterValues[index] = value; 
						} else {
							errorOutput.print("- ", entryName);
							if (parameter.isPreviewable()) {
								errorOutput.print(" (error)");
								ec++;
							} else {
								errorOutput.print(" (fatal)");
								fc++;
							}
							errorOutput.println(": value ", value, " violates constraints for parameter \"", parameterId, "\".");
							parameterValues[index] = parameter.getHint(0);
						}
						readCount++;
					} else {
						errorOutput.println("- ", entryName,
								" (fatal): multiple values for parameter \"", parameterId, "\".");
						fc++;
					}
				}

				parameterId = read(in, join("parameterId[", ++k, "]"), String.class);
			}
		} catch (ClassNotFoundException | IOException e) {
			errorOutput.print("- ", entryName, " (fatal): ");
			errorOutput.printStackTrace(e);
			fc++;
			parameterId = null;
		}

		if (readCount < representation.getNumOfParameters()) {
			for (int i = 0; i < representation.getNumOfParameters(); i++) {
				Parameter<?> parameter = representation.getParameter(i);
				if (parameterValues[i] == null) {
					errorOutput.print("- ", entryName);
					if (parameter.isPreviewable()) {
						errorOutput.print(" (error)");
						ec++;
					} else {
						errorOutput.print(" (fatal)");
						fc++;
					}
					errorOutput.println(": missing value for parameter \"", parameter.getId(), "\".");
					parameterValues[i] = parameter.getHint(0);
				}
			}
		} else if (parameterId != null) {
			errorOutput.println("- ", entryName, " (fatal): extra parameters ignored.");
			fc++;
		}

		errorCount.set(ec + fc);
		fatalCount.set(fc);
		return new PluginInstance<RepresentationPlugin>(parameterValues, representation);
	}
}
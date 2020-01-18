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

import static org.altervista.mbilotta.julia.Utilities.read;
import static org.altervista.mbilotta.julia.Utilities.readNonNull;
import static org.altervista.mbilotta.julia.Utilities.readNonNullList;
import static org.altervista.mbilotta.julia.Utilities.join;
import static org.altervista.mbilotta.julia.Utilities.writeList;
import static org.altervista.mbilotta.julia.program.parsers.Parameter.findConstructor;
import static org.altervista.mbilotta.julia.program.parsers.Parameter.findGetter;
import static org.altervista.mbilotta.julia.program.parsers.Parameter.findSetter;
import static org.altervista.mbilotta.julia.program.parsers.Parameter.getMethodString;
import static org.altervista.mbilotta.julia.program.parsers.Parameter.newIOException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JComponent;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.Groups;
import org.altervista.mbilotta.julia.NumberFactory;
import org.altervista.mbilotta.julia.Out;
import org.altervista.mbilotta.julia.program.JuliaObjectInputStream;


public abstract class Plugin implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String id;
	private final Class<?> type;
	private transient Constructor<?> constructor;
	private transient List<Author> authors;
	private String name;
	private transient List<Parameter<?>> parameters;
	private transient Map<String, List<Object>> hintGroups;

	Plugin(String id, Class<?> type) {
		this.id = id;
		this.type = type;
		if (type != null) {
			assert getFamily().getInterfaceType().isAssignableFrom(type);
			name = type.getSimpleName();
		}
	}

	public abstract PluginFamily getFamily();

	public String getId() {
		return id;
	}

	public final Class<?> getType() {
		return type;
	}

	public final Constructor<?> getConstructor() {
		return constructor;
	}

	public final List<Author> getAuthors() {
		return Collections.unmodifiableList(authors);
	}

	public final String getName() {
		return name;
	}

	public final Parameter<?> getParameter(int index) {
		return parameters.get(index);
	}

	public final Parameter<?> getParameter(String id) {
		for (Parameter<?> parameter : parameters) {
			if (parameter.getId().equals(id)) {
				return parameter;
			}
		}
		return null;
	}

	public final int getNumOfParameters() {
		return parameters.size();
	}

	public final List<Parameter<?>> getParameters() {
		return Collections.unmodifiableList(parameters);
	}

	public final Set<String> getHintGroupNames() {
		return Collections.unmodifiableSet(hintGroups.keySet());
	}

	public final List<Object> getHintGroup(String name) {
		return Collections.unmodifiableList(hintGroups.get(name));
	}

	public final JComponent[] createEditors() {
		int numOfParameters = getNumOfParameters();
		JComponent[] editors = new JComponent[numOfParameters];
		for (int i = 0; i < numOfParameters; i++) {
			editors[i] = getParameter(i).createEditor();
		}
		return editors;
	}

	public boolean hasPreviewableParameters() {
		return false;
	}

	public void populate(Object target, Object[] parameterValues) throws ReflectiveOperationException {
		int numOfParameters = getNumOfParameters();
		for (int i = 0; i < numOfParameters; i++) {
			Parameter<?> parameter = getParameter(i);
			Method setter = parameter.getSetterMethod();
			Object argument = parameterValues[i];
			setter.invoke(target, argument);
		}
	}

	public void populate(Object target, Object[] parameterValues, NumberFactory numberFactory) throws ReflectiveOperationException {
		int numOfParameters = getNumOfParameters();
		for (int i = 0; i < numOfParameters; i++) {
			Parameter<?> parameter = getParameter(i);
			Method setter = parameter.getSetterMethod();
			Object argument = parameterValues[i];
			if (parameter instanceof RealParameter) {
				argument = numberFactory.valueOf((Decimal) argument);
			}
			setter.invoke(target, argument);
		}
	}

	public String toString() {
		return toStringBuilder()
				.append(']').toString();
	}

	Constructor<?> initializeConstructor() throws NoSuchMethodException {
		constructor = findConstructor(null, type);
		return constructor;
	}

	void setAuthors(List<Author> authors) {
		this.authors = new ArrayList<>(authors);
	}

	void setName(String name) {
		this.name = name;
	}

	void setParameters(List<Parameter<?>.Validator> validators) {
		parameters = new ArrayList<>(validators.size());
		for (Parameter<?>.Validator validator : validators) {
			Parameter<?> parameter = validator.getParameter();
			parameter.setPlugin(this);
			parameters.add(parameter);
		}
	}

	void setHintGroups(List<Parameter<?>.Validator> validators) {
		hintGroups = new TreeMap<>(HintGroupNameComparator.INSTANCE);
		int numOfParameters = validators.size();
		int i = 0;
		for (Parameter<?>.Validator validator : validators) {
			for (Map.Entry<String, ?> e : validator.getReferencedGroups().entrySet()) {
				List<Object> hintGroup = hintGroups.get(e.getKey());
				if (hintGroup == null) {
					if (e.getValue() != null) {
						hintGroup = new ArrayList<>(Collections.nCopies(numOfParameters, null));
						hintGroup.set(i, e.getValue());
						hintGroups.put(e.getKey(), hintGroup);
					}
				} else {
					hintGroup.set(i, e.getValue());
				}
			}
			i++;
		}
	}

	Map<String, List<Object>> getHintGroups() {
		return hintGroups;
	}

	StringBuilder toStringBuilder() {
		return new StringBuilder().append(getClass().getSimpleName())
				.append("[id=").append(id)
				.append(", type=").append(type.getName())
				.append(", authors=").append(authors)
				.append(", name=").append(name)
				.append(", parameters=").append(parameters)
				.append(", hintGroups=").append(hintGroups);
	}

	private static class HintGroupNameComparator implements Comparator<String> {

		public static final Comparator<String> INSTANCE = new HintGroupNameComparator();

		@Override
		public int compare(String first, String second) {
			int result = first.compareTo(second);
			if (result == 0)
				return 0;

			if (first.equals("default")) {
				return -1;
			}
			if (second.equals("default")) {
				return 1;
			}
			return result;
		}
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		if (type == null)
			throw newIOException("type is null.");

		if (type.isPrimitive() || type.isArray())
			throw newIOException("Type " + type.getName() + " is a primitive type or array type.");
		int modifiers = type.getModifiers();
		if (!Modifier.isPublic(modifiers))
			throw newIOException("Type " + type.getName() + " does not have public visibilty.");
		if (Modifier.isAbstract(modifiers))
			throw newIOException("Type " + type.getName() + " is an abstract type.");
		if (type.getEnclosingClass() != null && !Modifier.isStatic(modifiers))
			throw newIOException("Type " + type.getName() + " is a non-static member class.");

		if (name == null) name = type.getName();

		authors = Author.inspectAnnotations(type, in instanceof JuliaObjectInputStream ? author -> ((JuliaObjectInputStream) in).resolveAuthor(author) : null);
		parameters = readNonNullList(in, "parameters", Parameter.class);

		int index = 0;
		for (Parameter<?> p : parameters) {
			p.setIndex(index++);
			p.setPlugin(this);

			try {
				Method setter = findSetter(p.getId(), p.getType(), type);
				p.setSetterMethod(setter);
				p.initConstraints();
				p.validateHints();
			} catch (NoSuchMethodException e) {
				throw newIOException(p.getId() + ".setterMethod", e);
			} catch (ClassValidationException e) {
				throw newIOException(p.getId(), e);
			}

			if (p instanceof AnyParameter) {
				Out<String> fieldNameOut = Out.newOut();
				try {
					((AnyParameter) p).initMethods(type, fieldNameOut);
				} catch (NoSuchMethodException e) {
					throw newIOException(p.getId() + "." + fieldNameOut.get(), e);
				}
			}
		}

		hintGroups = new TreeMap<>(HintGroupNameComparator.INSTANCE);
		int numOfParams = parameters.size();
		int numOfGroups = in.readInt();
		if (numOfGroups < 0)
			throw newIOException("[hintGroups.length=" + numOfGroups + "] is negative.");
		for (int i = 0; i < numOfGroups; i++) {
			String name = readNonNull(in, join("hintGroups[", i, "].name"), String.class);
			List<Object> values = new ArrayList<>(numOfParams);
			for (int k = 0; k < numOfParams; k++) {
				Parameter<?> parameter = parameters.get(k);
				Object value = read(in, join("hintGroups[\"", name, "\"].values[\"", parameter.getId(), "\"]"));
				if (value == null) {
					if (name.equals("default")) {
						throw newIOException(
								"hintGroups[\"default\"].values[\"" + parameter.getId() + "\"] is null.");
					}
					values.add(null);
				} else if (parameter.acceptsValue(value)) {
					values.add(value);
				} else {
					throw newIOException(
							"[hintGroups[\"" + name + "\"].values[\"" + parameter.getId() + "\"]=" + value + "] not accepted.");
				}
			}
			hintGroups.put(name, values);
		}

		if (!hintGroups.isEmpty() && !hintGroups.containsKey("default")) {
			throw newIOException(
					"hintGroups[\"default\"] not found.");
		}

		try {
			initializeConstructor();
		} catch (NoSuchMethodException e) {
			throw newIOException("constructor", e);
		}

		Object pluginInstance = null;
		for (Parameter<?> p : parameters) {
			try {
				Method getter = findGetter(p.getId(), p.getType(), type);
				if (getter != null) {
					if (pluginInstance == null) {
						pluginInstance = constructor.newInstance((Object[]) null);
					}

					Object getterHint = getter.invoke(pluginInstance, (Object[]) null);
					if (getterHint != null) {
						Set<String> getterGroupSet = null;
						Groups annotation = getter.getAnnotation(Groups.class);
						if (annotation != null) {
							List<String> groupList = Arrays.asList(annotation.value().split("\\s+"));
							if (groupList.get(0).isEmpty()) {
								groupList = groupList.subList(1, groupList.size());
							}
							switch (groupList.size()) {
							case 0:
								getterGroupSet = Collections.emptySet();
								break;
							case 1:
								getterGroupSet = Collections.singleton(groupList.get(0));
								break;
							default:
								getterGroupSet = new HashSet<>(groupList);
							}
						}
	
						if (getterGroupSet == null || getterGroupSet.isEmpty()) {
							if (!p.getHints().contains(getterHint)) {
								throw newIOException("Possible code change was detected.");
							}
						} else {
							for (String group : getterGroupSet) {
								List<Object> hintGroup = hintGroups.get(group);
								if (hintGroup == null) {
									throw newIOException("Possible code change was detected.");
								}
								Object hint = hintGroup.get(p.getIndex());
								if (hint == null || !hint.equals(getterHint)) {
									throw newIOException("Possible code change was detected.");
								}
							}
						}
					}
				}
			} catch (NoSuchMethodException e) {
				throw newIOException(p.getId() + ".getterMethod", e);
			} catch (ReflectiveOperationException e) {
				throw newIOException("Reflective invocation has failed.", e);
			}
		}
	}

	private void writeObject(ObjectOutputStream out)
			throws IOException {
		out.defaultWriteObject();

		writeList(out, parameters);

		out.writeInt(hintGroups.size());
		for (Map.Entry<String, List<Object>> hintGroup : hintGroups.entrySet()) {
			out.writeObject(hintGroup.getKey());
			for (Object value : hintGroup.getValue()) {
				out.writeObject(value);
			}
		}
	}

}

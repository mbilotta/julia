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

package org.altervista.mbilotta.julia;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.stream.Stream;

import javax.accessibility.AccessibleContext;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FileChooserUI;

import org.altervista.mbilotta.julia.program.parsers.AliasPlugin;
import org.altervista.mbilotta.julia.program.parsers.Plugin;


public class Utilities {

	public static final Printer out = Printer.newStandardOutput();

	public static final Printer err = Printer.newStandardError();

	public static final Printer debug = Printer.wrapPrinter(out);

	// Don't let anyone instantiate this class
	private Utilities() {
	}

	public static <T extends Cloneable> T safelyClone(T obj) {
		return obj != null ? (T) invoke(obj, "clone") : null;
	}

	public static <T> T read(ObjectInputStream in, Object name)
			throws ClassNotFoundException, IOException {
		try {
			return (T) in.readObject();
		} catch (InvalidObjectException e) {
			InvalidObjectException wrapper = new InvalidObjectException(name.toString());
			wrapper.initCause(e);
			throw wrapper;
		}
	}

	public static <T> T read(ObjectInputStream in, Object name, Class<?> type)
			throws ClassNotFoundException, IOException {
		T rv = read(in, name);
		if (rv != null && !type.isInstance(rv))
			throw new InvalidObjectException("[" + name + '=' + rv + "] not an instance of " + type);

		return rv;
	}

	public static <T> T readNonNull(ObjectInputStream in, Object name, Class<?> type)
			throws ClassNotFoundException, IOException {
		T rv = read(in, name);
		if (!type.isInstance(rv))
			throw rv != null ?
				new InvalidObjectException("[" + name + '=' + rv + "] not an instance of " + type) :
				new InvalidObjectException(name + " is null");

		return rv;
	}

	public static <E> List<E> readList(ObjectInputStream in, Object name)
			throws ClassNotFoundException, IOException {
		int size = in.readInt();
		if (size < 0)
			throw new InvalidObjectException("[" + name + ".length=" + size + "] is negative");

		List<E> rv = new ArrayList<>(size);
		for (int k = 0; k < size; k++) {
			E element = read(in, join(name, '[', k, ']'));
			rv.add(element);
		}

		return rv;
	}

	public static <E> List<E> readList(ObjectInputStream in, Object name, Class<?> elementType)
			throws ClassNotFoundException, IOException {
		int size = in.readInt();
		if (size < 0)
			throw new InvalidObjectException("[" + name + ".length=" + size + "] is negative");

		List<E> rv = new ArrayList<>(size);
		for (int k = 0; k < size; k++) {
			E element = read(in, join(name, '[', k, ']'), elementType);
			rv.add(element);
		}

		return rv;
	}

	public static <E> List<E> readNonNullList(ObjectInputStream in, Object name, Class<?> elementType)
			throws ClassNotFoundException, IOException {
		int size = in.readInt();
		if (size < 0)
			throw new InvalidObjectException("[" + name + ".length=" + size + "] is negative");

		List<E> rv = new ArrayList<>(size);
		for (int k = 0; k < size; k++) {
			E element = readNonNull(in, join(name, '[', k, ']'), elementType);
			rv.add(element);
		}

		return rv;
	}

	public static boolean hasElementType(Collection<?> collection, Class<?> elementType) {
		for (Object element : collection) {
			if (element != null && !elementType.isInstance(element))
				return false;
		}
		return true;
	}

	public static void writeList(ObjectOutputStream out, List<?> list)
			throws IOException {
		out.writeInt(list.size());
		for (Object element : list) {
			out.writeObject(element);
		}
	}

	public static Object join(final Object o1, final Object o2) {
		return new Object() {
			@Override
			public String toString() {
				return String.valueOf(o1).concat(String.valueOf(o2));
			}
		};
	}

	public static Object join(final Object o1, final Object o2, final Object... oi) {
		return new Object() {
			@Override
			public String toString() {
				StringBuilder sb =
						new StringBuilder(String.valueOf(o1)).append(String.valueOf(o2));
				for (Object o : oi) {
					if (o == this) {
						sb.append(super.toString());
					} else {
						sb.append(o);
					}
				}
				return sb.toString();
			}
		};
	}

	public static StringBuffer append(StringBuffer sb, Object... oi) {
		for (Object o : oi) {
			sb.append(o);
		}
		return sb;
	}

	public static StringBuilder append(StringBuilder sb, Object... oi) {
		for (Object o : oi) {
			sb.append(o);
		}
		return sb;
	}

	public static <T> T[] toArray(T o1, T... oi) {
		T[] rv = (T[]) Array.newInstance(o1.getClass(), oi.length + 1);
		rv[0] = o1;
		System.arraycopy(oi, 0, rv, 1, oi.length);
		return rv;
	}

	public static <T> List<T> toList(T o1, T... oi) {
		return Arrays.asList(toArray(o1, oi));
	}

	@SuppressWarnings("unchecked")
	public static <P extends Plugin> P findPlugin(String id, List<P> plugins, List<AliasPlugin> aliases) {
		Plugin rv = Stream.concat(plugins.stream(), aliases.stream())
			.filter(p -> p.getId().equals(id))
			.findFirst()
			.orElse(null);
		
		if (rv instanceof AliasPlugin) {
			Class<?> type = rv.getType();
			return plugins.stream()
				.filter(p -> p.getType() == type)
				.findFirst()
				.orElse(null);
		}
		return (P) rv;
	}

	public static void readFully(InputStream in, byte[] b)
			throws IOException {
		int n = 0, len = b.length;
		while (n < len) {
			int count = in.read(b, n, len - n);
			if (count < 0)
				throw new EOFException();
			n += count;
		}
	}

	public static Printer println() {
		return out.println();
	}

	public static Printer println(Object o) {
		return out.println(o);
	}

	public static Printer println(Object o1, Object o2) {
		return out.println(o1, o2);
	}

	public static Printer println(Object o1, Object o2, Object o3) {
		return out.println(o1, o2, o3);
	}

	public static Printer println(Object... oi) {
		return out.println(oi);
	}

	public static Printer print(Object o) {
		return out.print(o);
	}

	public static Printer print(Object o1, Object o2) {
		return out.print(o1, o2);
	}

	public static Printer print(Object o1, Object o2, Object o3) {
		return out.print(o1, o2, o3);
	}

	public static Printer print(Object o1, Object... oi) {
		return out.print(o1, oi);
	}

	public static void flush() {
		out.flush();
	}

	public static Printer printStackTrace(Throwable t) {
		return out.printStackTrace(t);
	}

	public static String formatMillisDuration(long ms) {
		return String.format("%d:%02d:%02d.%03d", ms/3_600_000, (ms%3_600_000)/60_000, (ms/1_000)%60, ms%1_000);
	}

	public static String formatDuration(Duration d) {
		return formatMillisDuration(d.toMillis());
	}

	public static List<Rectangle> subtract(Rectangle a, Rectangle b) {
		Rectangle i = a.intersection(b);
		if (i.isEmpty()) {
			return Collections.singletonList(a);
		}
		
		if (i.equals(a)) {
			return Collections.emptyList();
		}
		
		List<Rectangle> rv = new ArrayList<>(4);
		Rectangle r = new Rectangle(i.x, a.y, a.x + a.width - i.x, i.y - a.y);
		if (!r.isEmpty()) {
			rv.add(r);
		}
		
		r = new Rectangle(i.x + i.width, i.y, a.x + a.width - (i.x + i.width), a.y + a.height - i.y);
		if (!r.isEmpty()) {
			rv.add(r);
		}

		r = new Rectangle(a.x, i.y + i.height, i.x + i.width - a.x, a.y + a.height - (i.y + i.height));
		if (!r.isEmpty()) {
			rv.add(r);
		}

		r = new Rectangle(a.x, a.y, i.x - a.x, i.y + i.height - a.y);
		if (!r.isEmpty()) {
			rv.add(r);
		}

		return rv;
	}

	public static List<Rectangle> subtract(Rectangle a, Rectangle... b) {
		return subtract(Collections.singletonList(a), b);
	}

	public static List<Rectangle> subtract(List<Rectangle> a, Rectangle b) {
		List<Rectangle> rv = new LinkedList<>();
		for (Rectangle r : a) {
			rv.addAll(subtract(r, b));
		}
		return rv;
	}

	public static List<Rectangle> subtract(List<Rectangle> a, Rectangle... b) {
		List<Rectangle> rv = a;
		for (Rectangle r : b) {
			rv = subtract(rv, r);
		}
		return rv;
	}

	private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String toHexString(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	private static boolean isPrimitiveAssignableFrom(Class<?> dst, Class<?> src) {
		assert dst.isPrimitive() && src.isPrimitive();
		if (dst == src)
			return true;

		Object srcArray = Array.newInstance(src, 1);
		Object dstArray = Array.newInstance(dst, 1);
		try {
			Array.set(dstArray, 0, Array.get(srcArray, 0));
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static Class<?> getWrapperType(Class<?> primitiveType) {
		assert primitiveType.isPrimitive() && primitiveType != void.class;
		Object array = Array.newInstance(primitiveType, 1);
		return Array.get(array, 0).getClass();
	}

	private static boolean isAssignableFrom(Class<?> dst, Class<?> src) {
		if (src.isPrimitive()) {
			if (dst.isPrimitive()) {
				return isPrimitiveAssignableFrom(dst, src);
			} else {
				return dst.isAssignableFrom(getWrapperType(src));
			}
		} else {
			if (dst.isPrimitive()) {
				if (src == Boolean.class) return dst == boolean.class;
				if (src == Character.class) return isPrimitiveAssignableFrom(dst, char.class);
				if (src == Byte.class) return isPrimitiveAssignableFrom(dst, byte.class);
				if (src == Short.class) return isPrimitiveAssignableFrom(dst, short.class);
				if (src == Integer.class) return isPrimitiveAssignableFrom(dst, int.class);
				if (src == Long.class) return isPrimitiveAssignableFrom(dst, long.class);
				if (src == Float.class) return isPrimitiveAssignableFrom(dst, float.class);
				if (src == Double.class) return isPrimitiveAssignableFrom(dst, double.class);
				return false;
			} else {
				return dst.isAssignableFrom(src);
			}
		}
	}

	private static boolean isCompatibleSignature(Object[] args, Class<?>[] signature, Out<Boolean> exact) {
		int l = args.length;
		if (l != signature.length) {
			return false;
		}
		exact.set(true);
		for (int i = 0; i < l; i++) {
			if (args[i] == null) {
				if (signature[i].isPrimitive()) {
					return false;
				}
			} else if (signature[i] != args[i].getClass()) {
				if (!isAssignableFrom(signature[i], args[i].getClass())) {
					return false;
				}
				exact.set(false);
			}
		}
		return true;
	}

	private static boolean isMoreSpecific(Object arg, Class<?> a, Class<?> b) {
		if (arg == null || b == a || b == arg.getClass())
			return false;
		return isAssignableFrom(b, a);
	}

	private static boolean isMoreSpecificSignature(Object[] args, Class<?>[] a, Class<?>[] b) {
		int l = args.length;
		for (int i = 0; i < l; i++) {
			if (isMoreSpecific(args[i], a[i], b[i]))
				return true;
		}
		return false;
	}

	private static boolean throwsCheckedExceptions(Method method) {
		for (Class<?> exceptionType : method.getExceptionTypes()) {
			if (!RuntimeException.class.isAssignableFrom(exceptionType) &&
					!Error.class.isAssignableFrom(exceptionType))
				return true;
		}
		return false;
	}

	private static boolean hasCompatibleReturnType(Class<?> dst, Method method) {
		Class<?> src = method.getReturnType();
		if (dst == src)
			return true;
		if (dst == void.class || src == void.class)
			return false;

		if (dst.isPrimitive())
			return src.isPrimitive() && isPrimitiveAssignableFrom(dst, src);
		
		return isAssignableFrom(dst, src);
	}

	private static Class<?>[] toFixedArgSignature(Class<?>[] signature, int numOfParams) {
		Class<?> compType = signature[signature.length - 1].getComponentType();
		assert compType != null;
		Class<?>[] rv = new Class<?>[numOfParams];
		System.arraycopy(signature, 0, rv, 0, signature.length - 1);
		Arrays.fill(rv, signature.length - 1, numOfParams, compType);
		return rv;
	}

	private static Object[] toVarArgParams(Object[] args, Class<?> compType, int numOfParams) {
		Object[] rv = new Object[numOfParams];
		Object varArg = Array.newInstance(compType, args.length - numOfParams + 1);
		rv[numOfParams - 1] = varArg;
		System.arraycopy(args, 0, rv, 0, numOfParams - 1);
		System.arraycopy(args, numOfParams - 1, varArg, 0, Array.getLength(varArg));
		return rv;
	}

	private static <T> T defaultReturnValueFor(Class<?> retType) {
		if (retType == null || retType == void.class || !retType.isPrimitive())
			return null;

		Object array = Array.newInstance(retType, 1);
		return (T) Array.get(array, 0);
	}

	private static Object convertReturnValue(Class<?> retType, Class<?> actualRetType, Object retVal) {
		if (retType == null || retType.isAssignableFrom(actualRetType) ||
				(actualRetType.isPrimitive() && retType.isAssignableFrom(getWrapperType(actualRetType))))
			return retVal;
		Object array = Array.newInstance(retType, 1);
		Array.set(array, 0, retVal);
		return Array.get(array, 0);
	}

	private static <T> T invoke(Class<?> targetClass, Object target, String name, Class<? extends T> returnType,
			boolean virtual,
			Out<Boolean> invoked, Out<Throwable> throwable,
			Object... args) {

		assert !(virtual && target == null);

		if (args == null) {
			args = new Object[0];
		}

		Class<?> currRetType = null;
		Class<?>[] currSignature = null;
		Method match = null;
		List<Method> varArgs = null;
		Out<Boolean> exact = Out.newOut();
		boolean exactFound = false;
		for (Method method : targetClass.getMethods()) {
			if (Modifier.isStatic(method.getModifiers())) {
				if (virtual) {
					continue;
				}
			} else {
				if (target == null) {
					continue;
				}
			}

			if (returnType != null && !hasCompatibleReturnType(returnType, method)) {
				continue;
			}
			
			if (throwable == null && throwsCheckedExceptions(method)) {
				continue;
			}
			
			if (name.equals(method.getName())) {
				Class<?> retType = method.getReturnType();
				Class<?>[] signature = method.getParameterTypes();
				if (isCompatibleSignature(args, signature, exact)) {
					if (match == null) {
						match = method;
						if (returnType == null) {
							if (exact.get()) break;
						} else {
							if (retType == returnType && exact.get()) break;
							currRetType = retType;
							exactFound = exact.get();
						}
						currSignature = signature;
					} else if (returnType == null) {
						if (exact.get()) {
							match = method;
							break;
						}
						if (isMoreSpecificSignature(args, signature, currSignature)) {
							match = method;
							currSignature = signature;
						}
					} else {
						if (currRetType == returnType) {
							if (retType == returnType) {
								if (exact.get()) {
									match = method;
									break;
								}
								if (isMoreSpecificSignature(args, signature, currSignature)) {
									match = method;
									currSignature = signature;
								}
							}
						} else if (currRetType == retType) {
							if (!exactFound) {
								exactFound = exact.get();
								if (exactFound || isMoreSpecificSignature(args, signature, currSignature)) {
									match = method;
									currSignature = signature;
								}
							}
						} else if (isAssignableFrom(retType, currRetType)) {
							match = method;
							currSignature = signature;
							currRetType = retType;
							exactFound = exact.get();
						}
					}
				}
				
				if (match == null && method.isVarArgs() && args.length >= signature.length - 1) {
					if (varArgs == null) {
						varArgs = new LinkedList<>();
					}
					varArgs.add(method);
				}
			}
		}

		if (match == null && varArgs != null) {
			exactFound = false;
			Class<?>[] currVarArgSignature = null;
			for (Method method : varArgs) {
				Class<?> retType = method.getReturnType();
				Class<?>[] varArgSignature = method.getParameterTypes();
				Class<?>[] signature = toFixedArgSignature(varArgSignature, args.length);
				if (isCompatibleSignature(args, signature, exact)) {
					if (match == null) {
						match = method;
						currVarArgSignature = varArgSignature;
						if (returnType == null) {
							if (exact.get()) break;
						} else {
							if (retType == returnType && exact.get()) break;
							currRetType = retType;
							exactFound = exact.get();
						}
						currSignature = signature;
					} else if (returnType == null) {
						if (exact.get()) {
							match = method;
							currVarArgSignature = varArgSignature;
							break;
						}
						if (isMoreSpecificSignature(args, signature, currSignature)) {
							match = method;
							currVarArgSignature = varArgSignature;
							currSignature = signature;
						}
					} else {
						if (currRetType == returnType) {
							if (retType == returnType) {
								if (exact.get()) {
									match = method;
									currVarArgSignature = varArgSignature;
									break;
								}
								if (isMoreSpecificSignature(args, signature, currSignature)) {
									match = method;
									currVarArgSignature = varArgSignature;
									currSignature = signature;
								}
							}
						} else if (currRetType == retType) {
							if (!exactFound) {
								exactFound = exact.get();
								if (exactFound || isMoreSpecificSignature(args, signature, currSignature)) {
									match = method;
									currVarArgSignature = varArgSignature;
									currSignature = signature;
								}
							}
						} else if (isAssignableFrom(retType, currRetType)) {
							match = method;
							currVarArgSignature = varArgSignature;
							currSignature = signature;
							currRetType = retType;
							exactFound = exact.get();
						}
					}
				}
			}
			
			if (match != null) {
				args = toVarArgParams(args,
						currVarArgSignature[currVarArgSignature.length - 1],
						currVarArgSignature.length);
			}
		}

		if (match != null) {
			try {
				Object rv = match.invoke(target, args);
				if (invoked != null) invoked.set(true);
				if (throwable != null) throwable.set(null);
				return (T) convertReturnValue(returnType, match.getReturnType(), rv);
			} catch (IllegalAccessException e) {
				if (invoked != null) invoked.set(false);
				if (throwable != null) throwable.clear();
			} catch (InvocationTargetException e) {
				if (invoked != null) invoked.set(true);

				Throwable cause = e.getCause();
				if (throwable != null) {
					throwable.set(cause);
				} else if (cause != null) {
					if (cause instanceof RuntimeException) {
						throw (RuntimeException) cause;
					}
					if (cause instanceof Error) {
						throw (Error) cause;
					}
				}
			}
		} else {
			if (invoked != null) invoked.set(false);
			if (throwable != null) throwable.clear();
		}

		if (returnType == null && match != null) {
			return defaultReturnValueFor(match.getReturnType());
		}
		return defaultReturnValueFor(returnType);
	}

	public static <T> T invoke(Object target, String name, Object... args) {
		return invoke(null, null, target, name, args);
	}

	public static <T> T invoke(Out<Boolean> invoked, Class<? extends T> returnType, Object target, String name, Object... args) {
		return invoke(target.getClass(), target, name, returnType, false, invoked, null, args);
	}

	public static <V> V callSynchronously(Callable<V> doCall) throws InterruptedException, ExecutionException {
		if (EventQueue.isDispatchThread()) {
			try {
				return doCall.call();
			} catch (InterruptedException e) {
				throw new InterruptedException();
			} catch (Throwable t) {
				throw new ExecutionException(t);
			}
		} else {
			Future<V> future = callAsynchronously(doCall);
			return future.get();
		}
	}

	public static <V> Future<V> callAsynchronously(Callable<V> doCall) {
		RunnableFuture<V> rv = new FutureTask<>(doCall);
		EventQueue.invokeLater(rv);
		return rv;
	}

	public static <V> V runSynchronously(Runnable doRun, V result) throws InterruptedException, ExecutionException {
		if (EventQueue.isDispatchThread()) {
			try {
				doRun.run();
				return result;
			} catch (Throwable t) {
				throw new ExecutionException(t);
			}
		} else {
			Future<V> future = runAsynchronously(doRun, result);
			return future.get();
		}
	}

	public static <V> Future<V> runAsynchronously(Runnable doRun, V result) {
		RunnableFuture<V> rv = new FutureTask<>(doRun, result);
		EventQueue.invokeLater(rv);
		return rv;
	}

	public static Future<Void> showAsynchronously(final Window window) {
		return runAsynchronously(new Runnable() {
			@Override
			public void run() {
				window.setVisible(true);
			}
		}, null);
	}

	private static int styleFromMessageType(int messageType) {
		switch (messageType) {
		case JOptionPane.ERROR_MESSAGE:
			return JRootPane.ERROR_DIALOG;
		case JOptionPane.QUESTION_MESSAGE:
			return JRootPane.QUESTION_DIALOG;
		case JOptionPane.WARNING_MESSAGE:
			return JRootPane.WARNING_DIALOG;
		case JOptionPane.INFORMATION_MESSAGE:
			return JRootPane.INFORMATION_DIALOG;
		case JOptionPane.PLAIN_MESSAGE:
		default:
			return JRootPane.PLAIN_DIALOG;
		}
	}

	public static Window getWindowForComponent(Component parent)
			throws HeadlessException {
		if (parent == null)
			return null;
		if (parent instanceof Frame || parent instanceof Dialog)
			return (Window) parent;
		return getWindowForComponent(parent.getParent());
	}

	public static JDialog createDialog(final JOptionPane optionPane, Component parent, String title, boolean autoHide) {
		final JDialog dialog;

		Window window = getWindowForComponent(parent);
		if (window == null || window instanceof Frame) {
			dialog = new JDialog((Frame) window, title, true);
		} else {
			dialog = new JDialog((Dialog) window, title, true);
		}
		
		dialog.setComponentOrientation(optionPane.getComponentOrientation());
		Container contentPane = dialog.getContentPane();

		contentPane.setLayout(new BorderLayout());
		contentPane.add(optionPane, BorderLayout.CENTER);
		dialog.setResizable(false);
		if (JDialog.isDefaultLookAndFeelDecorated()) {
			boolean supportsWindowDecorations = UIManager.getLookAndFeel()
					.getSupportsWindowDecorations();
			if (supportsWindowDecorations) {
				dialog.setUndecorated(true);
		        int style = styleFromMessageType(optionPane.getMessageType());
				optionPane.getRootPane().setWindowDecorationStyle(style);
			}
		}
		dialog.pack();
		dialog.setLocationRelativeTo(parent);

		final PropertyChangeListener hider;
		if (autoHide) {
			hider = new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent e) {
					// Let the defaultCloseOperation handle the closing
					// if the user closed the window without selecting a button
					// (newValue = null in that case). Otherwise, close the dialog.
					if (e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)
							&& e.getNewValue() != null
							&& e.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {
						dialog.setVisible(false);
					}
				}
			};
		} else {
			hider = null;
		}

		WindowAdapter adapter = new WindowAdapter() {
			private boolean gotFocus = false;

			public void windowClosing(WindowEvent e) {
				optionPane.setValue(null);
			}

			public void windowClosed(WindowEvent e) {
				optionPane.removePropertyChangeListener(hider);
				dialog.getContentPane().removeAll();
			}

			public void windowGainedFocus(WindowEvent e) {
				// Once window gets focus, set initial focus
				if (!gotFocus) {
					optionPane.selectInitialValue();
					gotFocus = true;
				}
			}
		};
		dialog.addWindowListener(adapter);
		dialog.addWindowFocusListener(adapter);
		dialog.addComponentListener(new ComponentAdapter() {
			public void componentShown(ComponentEvent e) {
				// reset value to ensure closing works properly
				optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			}
		});
		
		if (autoHide)
			optionPane.addPropertyChangeListener(hider);
		
		return dialog;
	}

	public static JOptionPane createOptionPane(final JColorChooser colorChooser) {
		final String ok = UIManager.getString("ColorChooser.okText");
		final String cancel = UIManager.getString("ColorChooser.cancelText");
		final String reset = UIManager.getString("ColorChooser.resetText");
		final Color resetColor = colorChooser.getColor();
		return new JOptionPane(colorChooser,
				JOptionPane.PLAIN_MESSAGE,
				JOptionPane.DEFAULT_OPTION,
				null,
				new Object[] { ok, cancel, reset },
				ok) {
			@Override
			public void setValue(Object newValue) {
				if (newValue == ok) {
					newValue = OK_OPTION;
				} else if (newValue == cancel) {
					newValue = CANCEL_OPTION;
				} else if (newValue == reset) {
					colorChooser.setColor(resetColor);
					return;
				}
				super.setValue(newValue);
			}
		};
	}

	public static JDialog createDialog(JFileChooser fileChooser, Component parent) {
		FileChooserUI ui = fileChooser.getUI();
		String title = ui.getDialogTitle(fileChooser);
		fileChooser.putClientProperty(AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY,
				title);

		JDialog dialog;
		Window window = getWindowForComponent(parent);
		if (window == null || window instanceof Frame) {
			dialog = new JDialog((Frame) window, title, true);
		} else {
			dialog = new JDialog((Dialog) window, title, true);
		}
		dialog.setComponentOrientation(fileChooser.getComponentOrientation());

		Container contentPane = dialog.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(fileChooser, BorderLayout.CENTER);

		if (JDialog.isDefaultLookAndFeelDecorated()) {
			boolean supportsWindowDecorations = UIManager.getLookAndFeel()
					.getSupportsWindowDecorations();
			if (supportsWindowDecorations) {
				dialog.getRootPane().setWindowDecorationStyle(
						JRootPane.FILE_CHOOSER_DIALOG);
			}
		}
		dialog.getRootPane().setDefaultButton(ui.getDefaultButton(fileChooser));
		dialog.pack();
		dialog.setLocationRelativeTo(parent);

		return dialog;
	}

	public static int showSaveDialog(final JFileChooser fileChooser, Component parent) {
		final JDialog dialog = createDialog(fileChooser, parent);
		final Out<Integer> rv = Out.newOut(JFileChooser.ERROR_OPTION);
		ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand() == JFileChooser.APPROVE_SELECTION) {
					File selectedFile = fileChooser.getSelectedFile();
					FileFilter fileFilter = fileChooser.getFileFilter();
					boolean extensionAppended = false;
					if (fileFilter != fileChooser.getAcceptAllFileFilter() &&
							fileFilter instanceof FileNameExtensionFilter &&
							!fileFilter.accept(selectedFile)) {
						String extension = ((FileNameExtensionFilter) fileFilter).getExtensions()[0];
						selectedFile = new File(selectedFile.getPath() + '.' + extension);
						extensionAppended = true;
					}
					
					if (selectedFile.exists()) {
						int rv = JOptionPane.showConfirmDialog(dialog,
								"File " + selectedFile + " already exists.\n" +
								"Do you want to overwrite it?",
								"Julia",
								JOptionPane.YES_NO_OPTION);
						if (rv != JOptionPane.YES_OPTION) {
							return;
						}
					}
					
					dialog.setVisible(false);
					rv.set(JFileChooser.APPROVE_OPTION);
					
					if (extensionAppended) {
						fileChooser.setSelectedFile(selectedFile);
					}
				} else {
					dialog.setVisible(false);
					rv.set(JFileChooser.CANCEL_OPTION);
				}
			}
		};
		fileChooser.addActionListener(actionListener);
		
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				rv.set(JFileChooser.CANCEL_OPTION);
			}
		});

		fileChooser.rescanCurrentDirectory();
		dialog.setVisible(true);
		fileChooser.removeActionListener(actionListener);
		dialog.getContentPane().removeAll();
		dialog.dispose();

		return rv.get();
	}

	public static void toFront(JFrame frame) {
		if ((frame.getExtendedState() & JFrame.ICONIFIED) != 0) {
			WindowStateListener listener = new WindowStateListener() {
				@Override
				public void windowStateChanged(WindowEvent e) {
					assert (e.getNewState() & JFrame.ICONIFIED) == 0;
					Window w = e.getWindow();
					w.toFront();
					w.removeWindowStateListener(this);
				}
			};
			frame.addWindowStateListener(listener);
			int state = frame.getExtendedState();
			state &= ~JFrame.ICONIFIED;
			frame.setExtendedState(state);
		} else {
			frame.toFront();
		}
	}

	public static void show(JFrame frame) {
		frame.setVisible(true);
		toFront(frame);
	}
}

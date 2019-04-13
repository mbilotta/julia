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

import java.lang.reflect.Array;
import java.util.List;


public abstract class Out<V> {
	
	private static final Object UNSET = new Object() {
		@Override
		public String toString() {
			return "unset";
		}
	};

	private static final Out<?> NULL_IMPL = new NullImpl<>();

	private V unset() {
		return (V) UNSET;
	}
	
	private static final class NullImpl<V> extends Out<V> {
		
		private final V value;
		
		public NullImpl() {
			this(null);
		}

		public NullImpl(V value) {
			this.value = value;
		}
		
		@Override
		public V get() {
			return value;
		}

		@Override
		public void set(V value) {
		}
	}

	public static <V> Out<V> nullOut() {
		return (Out<V>) NULL_IMPL;
	}

	public static <V> Out<V> nullOut(V value) {
		return new NullImpl<>(value);
	}

	public static <V> Out<V> newOut() {
		return new Impl<>();
	}

	public static <V> Out<V> newOut(V initialValue) {
		return new Impl<>(initialValue);
	}

	public static <V> Out<V> newVolatileOut() {
		return new VolatileImpl<>();
	}

	public static <V> Out<V> newVolatileOut(V initialValue) {
		return new VolatileImpl<>(initialValue);
	}

	public static <V> Out<V> newSynchronizedOut() {
		return new SynchronizedImpl<>();
	}

	public static <V> Out<V> newSynchronizedOut(V initialValue) {
		return new SynchronizedImpl<>(initialValue);
	}

	public static Out<Boolean> newOut(boolean[] array, int index) {
		return new ArrayImpl<>(array, index);
	}

	public static Out<Character> newOut(char[] array, int index) {
		return new ArrayImpl<>(array, index);
	}

	public static Out<Byte> newOut(byte[] array, int index) {
		return new ArrayImpl<>(array, index);
	}

	public static Out<Short> newOut(short[] array, int index) {
		return new ArrayImpl<>(array, index);
	}

	public static Out<Integer> newOut(int[] array, int index) {
		return new ArrayImpl<>(array, index);
	}

	public static Out<Long> newOut(long[] array, int index) {
		return new ArrayImpl<>(array, index);
	}

	public static Out<Float> newOut(float[] array, int index) {
		return new ArrayImpl<>(array, index);
	}

	public static Out<Double> newOut(double[] array, int index) {
		return new ArrayImpl<>(array, index);
	}

	public static <T> Out<T> newOut(T[] array, int index) {
		return new ArrayImpl<>(array, index);
	}

	public static <T> Out<T> newOut(List<T> list, int index) {
		return new ListImpl<>(list, index);
	}

	public static final class Impl<V> extends Out<V> {

		private V value;
		
		public Impl() {
			clear();
		}

		public Impl(V initialValue) {
			set(initialValue);
		}

		@Override
		public V get() {
			V rv = value;
			if (rv == UNSET)
				throw new IllegalStateException("out not set");
			return rv;
		}

		@Override
		public V get(V defaultValue) {
			V rv = value;
			return rv == UNSET ? defaultValue : rv;
		}

		@Override
		public void set(V value) {
			this.value = value;
		}

		@Override
		public boolean isSet() {
			return value != UNSET;
		}

		@Override
		public void clear() {
			value = super.unset();
		}

		@Override
		public String toString() {
			return String.valueOf(get(super.unset()));
		}
	}

	public static final class VolatileImpl<V> extends Out<V> {

		private volatile V value;
		
		public VolatileImpl() {
			clear();
		}

		public VolatileImpl(V initialValue) {
			set(initialValue);
		}
		
		@Override
		public V get() {
			V rv = value;
			if (rv == UNSET)
				throw new IllegalStateException("out not set");
			return rv;
		}

		@Override
		public V get(V defaultValue) {
			V rv = value;
			return rv == UNSET ? defaultValue : rv;
		}

		@Override
		public void set(V value) {
			this.value = value;
		}

		@Override
		public boolean isSet() {
			return value != UNSET;
		}

		@Override
		public void clear() {
			value = super.unset();
		}

		@Override
		public String toString() {
			return String.valueOf(get(super.unset()));
		}
	}

	public static final class SynchronizedImpl<V> extends Out<V> {

		private V value;
		
		public SynchronizedImpl() {
			clear();
		}

		public SynchronizedImpl(V initialValue) {
			set(initialValue);
		}

		@Override
		public synchronized V get() {
			V rv = value;
			if (rv == UNSET)
				throw new IllegalStateException("out not set");
			return rv;
		}

		@Override
		public synchronized V get(V defaultValue) {
			V rv = value;
			return rv == UNSET ? defaultValue : rv;
		}

		@Override
		public synchronized void set(V value) {
			this.value = value;
		}

		@Override
		public synchronized boolean isSet() {
			return value != UNSET;
		}

		@Override
		public synchronized void clear() {
			value = super.unset();
		}

		@Override
		public String toString() {
			return String.valueOf(get(super.unset()));
		}
	}

	public static final class ArrayImpl<V> extends Out<V> {
		
		private final Object array;
		private final int index;
		
		public ArrayImpl(Object array, int index) {
			if (index < 0 || index >= Array.getLength(array)) {
				throw new ArrayIndexOutOfBoundsException(Integer.toString(index));
			}
			this.array = array;
			this.index = index;
		}
		
		@Override
		public V get() {
			return (V) Array.get(array, index);
		}

		@Override
		public void set(V value) {
			Array.set(array, index, value);
		}
	}

	public static final class ListImpl<V> extends Out<V> {

		private final List<V> list;
		private final int index;

		public ListImpl(List<V> list, int index) {
			if (index < 0 || index >= list.size()) {
				throw new IndexOutOfBoundsException(Integer.toString(index));
			}
			this.list = list;
			this.index = index;
		}

		@Override
		public V get() {
			return list.get(index);
		}

		@Override
		public void set(V value) {
			list.set(index, value);
		}
	}

	public abstract V get();

	public V get(V defaultValue) {
		return get();
	}

	public abstract void set(V value);

	public boolean isSet() {
		return true;
	}

	public void clear() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return String.valueOf(get());
	}
}

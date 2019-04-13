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
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.altervista.mbilotta.julia.Utilities;



public class BinaryRelation<E> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private transient Map<Object, Integer> elementToIndex = new HashMap<>();
	private transient List<E> indexToElement = new ArrayList<>();
	private transient int n = 0;
	private transient boolean[][] adjacencyMatrix = new boolean[0][0];

	public BinaryRelation() {
	}

	public boolean contains(Object a, Object b) {
		Integer aIndex = elementToIndex.get(a);
		Integer bIndex = elementToIndex.get(b);
		return aIndex != null && bIndex != null && adjacencyMatrix[aIndex][bIndex];
	}

	public void put(E a, E b) {
		Integer aIndex = elementToIndex.get(a);
		Integer bIndex = elementToIndex.get(b);
		if (aIndex == null && bIndex == null) {
			adjacencyMatrix = copyOfAdjacencyMatrix(n + 2);
			aIndex = n++;
			bIndex = n++;
			elementToIndex.put(a, aIndex);
			elementToIndex.put(b, bIndex);
			indexToElement.add(a);
			indexToElement.add(b);
		} else if (aIndex == null) {
			adjacencyMatrix = copyOfAdjacencyMatrix(n + 1);
			aIndex = n++;
			elementToIndex.put(a, aIndex);
			indexToElement.add(a);
		} else if (bIndex == null) {
			adjacencyMatrix = copyOfAdjacencyMatrix(n + 1);
			bIndex = n++;
			elementToIndex.put(b, bIndex);
			indexToElement.add(b);
		}
		
		adjacencyMatrix[aIndex][bIndex] = true;
	}

	public void put(E a, List<E> bs) {
		int size = n;

		Integer aIndex = elementToIndex.get(a);
		if (aIndex == null) {
			aIndex = size++;
			elementToIndex.put(a, aIndex);
			indexToElement.add(a);
		}

		List<Integer> bIndexes = new ArrayList<>(bs.size());
		for (E b : bs) {
			Integer bIndex = elementToIndex.get(b);
			if (bIndex == null) {
				bIndex = size++;
				elementToIndex.put(b, bIndex);
				indexToElement.add(b);
			}

			bIndexes.add(bIndex);
		}

		adjacencyMatrix = copyOfAdjacencyMatrix(size);
		n = size;
		
		for (int bIndex : bIndexes) {
			adjacencyMatrix[aIndex][bIndex] = true;
		}
	}

	public void complete() {
		int n = this.n;
		boolean[][] src = adjacencyMatrix;
		for (int k = 0; k < n; k++) {
			adjacencyMatrix = new boolean[n][n];
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					adjacencyMatrix[i][j] = src[i][j] || (src[i][k] && src[k][j]);
				}
			}

			src = adjacencyMatrix;
		}
	}

	public List<E> reduce(List<E> set) {
		List<E> rv = new ArrayList<>(set.size());
		List<Integer> indexes = new ArrayList<>(set.size());
		for (E element : set) {
			Integer index = elementToIndex.get(element);
			if (index == null)
				rv.add(element);
			else
				indexes.add(index);
		}

		List<Integer> indexesToRemove = new ArrayList<>(indexes.size());
		for (int a : indexes) {
			for (int b : indexes) {
				if (adjacencyMatrix[a][b])
					indexesToRemove.add(b);
			}
		}

		indexes.removeAll(indexesToRemove);
		
		for (int index : indexes) {
			rv.add(indexToElement.get(index));
		}
		
		return rv;
	}
	
	public boolean contains(BinaryRelation<?> other) {
		int n = other.n;
		
		if (n > this.n)
			return false;

		boolean[][] adjacencyMatrix = other.adjacencyMatrix;
		List<?> indexToName = other.indexToElement;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (adjacencyMatrix[i][j] && !this.contains(indexToName.get(i), indexToName.get(j)))
					return false;
			}
		}
		
		return true;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		
		if (o instanceof BinaryRelation) {
			BinaryRelation<?> other = (BinaryRelation<?>) o;
			return this.contains(other) && other.contains(this);
		}
		
		return false;
	}

	public boolean hasElementType(Class<?> elementType) {
		return Utilities.hasElementType(indexToElement, elementType);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder().append(getClass().getName());
		sb.append('[');
		int n = this.n;
		boolean[][] adjacencyMatrix = this.adjacencyMatrix;
		List<E> indexToName = this.indexToElement;
		for (int i = 0; i < n; ) {
			for (int j = 0; j < n; ) {
				if (adjacencyMatrix[i][j])
					sb.append('(').append(indexToName.get(i))
						.append(", ").append(indexToName.get(j))
						.append(')');
				j++;
				if (j < n) sb.append(", "); else break;
			}
			i++;
			if (i < n) sb.append(", "); else break;
		}

		return sb.append(']').toString();
	}

	private boolean[][] copyOfAdjacencyMatrix(int size) {
		int n = this.n;
		boolean[][] adjacencyMatrix = this.adjacencyMatrix;
		boolean[][] rv = new boolean[size][size];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				rv[i][j] = adjacencyMatrix[i][j];
			}
		}

		return rv;
	}

	private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
		in.defaultReadObject();
		
		List<E> indexToName = (List<E>) Utilities.readList(in, "indexToName");
		int n = indexToName.size();
		Map<Object, Integer> nameToIndex = new HashMap<>();
		for (int i = 0; i < n; i++) {
			E name = indexToName.get(i);
			Integer j;
			if ( (j = nameToIndex.put(name, i)) != null )
				throw new InvalidObjectException("[indexToName[" + i + "]=" + name + "] already present at index " + j);
		}

		boolean[][] adjacencyMatrix = new boolean[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				adjacencyMatrix[i][j] = in.readBoolean();
			}
		}

		this.elementToIndex = nameToIndex;
		this.indexToElement = indexToName;
		this.n = n;
		this.adjacencyMatrix = adjacencyMatrix;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();

		Utilities.writeList(out, indexToElement);
		int n = this.n;
		boolean[][] adjacencyMatrix = this.adjacencyMatrix;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				out.writeBoolean(adjacencyMatrix[i][j]);
			}
		}
	}
}

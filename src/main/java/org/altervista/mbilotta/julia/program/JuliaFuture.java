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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.FutureTask;

import org.altervista.mbilotta.julia.Utilities;


public class JuliaFuture extends FutureTask<Void> implements Iterable<ExecutionObserver> {

	private final Runnable runnable;
	private final List<ExecutionObserver> executionObservers;

	public JuliaFuture(Runnable runnable, ExecutionObserver eo) {
		this(runnable, eo, (ExecutionObserver[]) null);
	}

	public JuliaFuture(Runnable runnable,
			ExecutionObserver executionObserver,
			ExecutionObserver... executionObservers) {
		super(runnable, null);
		assert executionObserver != null &&
				(executionObservers == null || Arrays.asList(executionObservers).indexOf(null) == -1);
		if (executionObservers == null || executionObservers.length == 0) {
			this.executionObservers = Collections.singletonList(executionObserver);
		} else {
			this.executionObservers = Collections.unmodifiableList(
					Utilities.toList(executionObserver, executionObservers));
		}
		this.runnable = runnable;
	}

	public Runnable getTarget() {
		return runnable;
	}

	@Override
	public Iterator<ExecutionObserver> iterator() {
		return executionObservers.iterator();
	}
}

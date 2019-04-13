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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class JuliaExecutorService extends ThreadPoolExecutor {

	public JuliaExecutorService(int corePoolSize, long keepAliveTime, TimeUnit unit) {
		super(corePoolSize, Integer.MAX_VALUE, keepAliveTime, unit, new SynchronousQueue<Runnable>());
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		if (r instanceof JuliaFuture) {
			JuliaFuture future = (JuliaFuture) r;
			Runnable target = future.getTarget();
			for (ExecutionObserver o : future) {
				o.executionStarting(target);
			}
		}
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		if (r instanceof JuliaFuture) {
			JuliaFuture future = (JuliaFuture) r;
			Runnable target = future.getTarget();
			try {
				try {
					future.get();
					for (ExecutionObserver o : future) {
						o.executionFinished(target);
					}
				} catch (CancellationException e) {
					for (ExecutionObserver o : future) {
						o.executionCancelled(target);
					}
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					for (ExecutionObserver o : future) {
						o.executionFinished(target, cause);
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public JuliaFuture submitAndObserve(Runnable runnable, ExecutionObserver executionObserver) {
		return submitAndObserve(runnable, executionObserver, (ExecutionObserver[]) null);
	}

	public JuliaFuture submitAndObserve(Runnable runnable,
			ExecutionObserver executionObserver,
			ExecutionObserver... executionObservers) {
		if (runnable == null) throw new NullPointerException();
		JuliaFuture rv = new JuliaFuture(runnable, executionObserver, executionObservers);
		execute(rv);
		return rv;
	}
}

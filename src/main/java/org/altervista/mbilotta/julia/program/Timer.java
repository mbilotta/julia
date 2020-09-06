/*
 * Copyright (C) 2020 Maurizio Bilotta.
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

import java.time.Duration;
import java.time.Instant;

public class Timer {
	private Instant start;
	private Duration elapsedTime;
	private Duration totalElapsedTime = Duration.ZERO;

	public void start() {
		start = Instant.now();
	}

	public void stop() {
		elapsedTime = Duration.between(start, Instant.now());
		totalElapsedTime = totalElapsedTime.plus(elapsedTime);
	}

	public Duration getElapsedTime() {
		return elapsedTime;
	}

	public Duration getTotalElapsedTime() {
		return totalElapsedTime;
	}
}

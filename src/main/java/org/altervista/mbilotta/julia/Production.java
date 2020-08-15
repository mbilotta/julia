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

import org.altervista.mbilotta.julia.math.Complex;
import org.altervista.mbilotta.julia.math.CoordinateTransform;


public abstract class Production {

	private final NumberFactory numberFactory;
	private final Formula formula;
	private final Representation representation;
	private final CoordinateTransform coordinateTransform;
	private final Complex juliaSetPoint;
	private final IntermediateImage iimg;

	private int maxLogLength = -1;

	public Production(NumberFactory numberFactory,
			Formula formula,
			Representation representation,
			CoordinateTransform coordinateTransform,
			Complex juliaSetPoint,
			IntermediateImage iimg) {

		assert numberFactory != null;
		assert formula != null;
		assert representation != null;
		assert coordinateTransform != null;
		assert iimg != null;

		this.numberFactory = numberFactory;
		this.formula = formula;
		this.representation = representation;
		this.coordinateTransform = coordinateTransform;
		this.juliaSetPoint = juliaSetPoint;
		this.iimg = iimg;
	}

	public abstract class Producer implements Runnable {

		private final Progress progress;
		private final Formula formula;
		private final Printer printer;
		private volatile boolean started;

		public Producer(Progress progress) {
			assert progress != null;
			this.progress = progress;
			this.formula = Production.this.formula.newInstance();
			this.printer = maxLogLength > 0 ? new LimitedStringPrinter(maxLogLength) : Printer.nullPrinter();
		}

		public final void run() {
			if (started) {
				resume();
			} else {
				Formula formula = getFormula();
				Complex juliaSetPoint = getJuliaSetPoint();
				formula.cacheConstants(getNumberFactory());
				if (juliaSetPoint != null)
					formula.setC(juliaSetPoint);
				try {
					start();
				} finally {
					started = true;
				}
			}
		}

		public void handleException(Throwable t) {
			t.printStackTrace();
		}

		protected abstract void start();
		
		protected void resume() {
			start();
		}

		public final Production getProduction() {
			return Production.this;
		}

		public final Object getProgressValue() {
			return progress.getValue();
		}

		public final boolean hasFinished() {
			return progress.isFinalValue();
		}

		public final Printer getPrinter() {
			return printer;
		}

		protected final void setProgressValue(Object value) {
			progress.setValue(value);
		}

		protected final Formula getFormula() {
			return formula;
		}

		@Override
		public String toString() {
			return getClass().getCanonicalName() + "[progress=" + progress + "]";
		}
	}

	protected final NumberFactory getNumberFactory() {
		return numberFactory;
	}

	public final Producer createProducer(int index) {
		return createProducer(iimg.getProgressOf(index));
	}

	public abstract Producer createProducer(Progress progress);

	public final IntermediateImage getIntermediateImage() {
		return iimg;
	}

	public final int getImageWidth() {
		return iimg.getWidth();
	}

	public final int getImageHeight() {
		return iimg.getHeight();
	}

	public final int getNumOfProducers() {
		return iimg.getNumOfProducers();
	}

	public final Representation getRepresentation() {
		return representation;
	}

	public final CoordinateTransform getCoordinateTransform() {
		return coordinateTransform;
	}

	public final Complex getJuliaSetPoint() {
		return juliaSetPoint;
	}

	public void resetSynchronizers() {
	}

	public final void setMaxLogLength(int length) {
		maxLogLength = length;
	}
}

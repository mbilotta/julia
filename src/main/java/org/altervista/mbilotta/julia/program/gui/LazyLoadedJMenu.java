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

package org.altervista.mbilotta.julia.program.gui;

import javax.swing.JMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;


public class LazyLoadedJMenu extends JMenu {

	private boolean loaded = false;
	private Listener listener;
	private MenuLoader loader;

	public LazyLoadedJMenu(String text) {
		this(text, null);
	}

	public LazyLoadedJMenu(String text, MenuLoader loader) {
		super(text);
		setLoader(loader);
	}

	private class Listener implements PopupMenuListener {
		@Override
		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
			load();
		}

		@Override
		public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		}

		@Override
		public void popupMenuCanceled(PopupMenuEvent e) {
		}
	}

	public void setLoader(MenuLoader loader) {
		if (listener == null && loader != null) {
			listener = new Listener();
			getPopupMenu().addPopupMenuListener(listener);
		}
		this.loader = loader;
	}

	public void load() {
		if (!loaded && loader != null) {
			loader.loadMenu(getPopupMenu());
			loaded = true;
		}
	}

	public void unload() {
		removeAll();
		loaded = false;
	}
}

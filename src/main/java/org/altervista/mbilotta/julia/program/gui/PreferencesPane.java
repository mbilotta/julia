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

import java.awt.BorderLayout;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.altervista.mbilotta.julia.program.Application;
import org.altervista.mbilotta.julia.program.Preferences;


public class PreferencesPane extends JOptionPane {
	
	private PreferenceTabs tabs;

	/**
	 * Create the panel.
	 */
	public PreferencesPane() {
		JPanel panel = new JPanel(new BorderLayout());
		tabs = new PreferenceTabs();
		panel.add(tabs, BorderLayout.CENTER);
		setMessage(panel);
		setOptionType(JOptionPane.OK_CANCEL_OPTION);
	}

	public void init(Application application) {
		tabs.init(application);
	}

	public Preferences commit() {
		return tabs.commit();
	}

	public void cancel() {
		tabs.cancel();
	}

	public void update() {
		tabs.update();
	}
}

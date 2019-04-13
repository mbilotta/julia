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
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.altervista.mbilotta.julia.Production;


public class LogPane extends JTabbedPane {

	private JDialog dialog;
	private boolean hasOutput;
	private boolean scrollToBottom;

	/**
	 * Create the panel.
	 */
	public LogPane() {
		LogTab logTab = new LogTab();
		addTab("Thread 0", null, logTab, null);
		setMnemonicAt(0, KeyEvent.VK_0);
		
		LogTab logTab_1 = new LogTab();
		addTab("Thread 1", null, logTab_1, null);
		setMnemonicAt(1, KeyEvent.VK_1);
	}

	public void init(int maxLength, List<Production.Producer> producers) {
		if (getTabCount() < producers.size()) {
			do {
				LogTab logTab = new LogTab();
				int n = getTabCount();
				addTab("Thread " + n, null, logTab, null);
				setMnemonicAt(n, KeyEvent.VK_0 + (n % 10));
			} while (getTabCount() < producers.size());
		} else if (getTabCount() > producers.size()) {
			do {
				removeTabAt(getTabCount() - 1);
			} while (getTabCount() > producers.size());
		}

		hasOutput = false;
		for (int i = 0; i < getTabCount(); i++) {
			LogTab logTab = getLogTab(i);
			logTab.init(maxLength, producers.get(i));
			hasOutput |= logTab.hasOutput();
		}

		if (dialog == null) {
			scrollToBottom = true;
			setSelectedIndex(0);
		}
	}

	public void refresh() {
		if (hasOutput) {
			for (int i = 0; i < getTabCount(); i++) {
				getLogTab(i).refresh();
			}
		}
	}

	public boolean hasOutput() {
		return hasOutput;
	}

	public LogTab getLogTab(int index) {
		return (LogTab) getComponentAt(index);
	}

	public void showDialog(Component parent) {
		if (dialog == null) {
			JPanel message = new JPanel(new BorderLayout());
			message.add(this, BorderLayout.CENTER);
			JOptionPane optionPane = new JOptionPane(message,
					JOptionPane.PLAIN_MESSAGE,
					JOptionPane.DEFAULT_OPTION,
					null,
					new Object[] { "Close" },
					"Close");
			dialog = optionPane.createDialog(parent, "Calculation logs");
			dialog.setResizable(true);
			dialog.setModal(false);
			dialog.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					dialog.dispose();
					dialog = null;
				}
			});
			if (scrollToBottom) {
				for (int i = 0; i < getTabCount(); i++) {
					getLogTab(i).scrollToBottom();
				}
				scrollToBottom = false;
			}
			dialog.setVisible(true);
		} else {
			dialog.toFront();
		}
	}
}

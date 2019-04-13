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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.altervista.mbilotta.julia.Gradient;
import org.altervista.mbilotta.julia.program.Profile;


public class GradientSelectionPane extends JOptionPane {

	private JList<Gradient> gradientList;
	private JButton selectAllButton = new JButton("Select all");
	private JButton selectNoneButton = new JButton("Select none");

	public GradientSelectionPane(final List<Gradient> gradients, String message) {
		gradientList = new JList<>(new AbstractListModel<Gradient>() {
			@Override
			public int getSize() {
				return gradients.size();
			}
			@Override
			public Gradient getElementAt(int index) {
				return gradients.get(index);
			}
		});
		gradientList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		gradientList.setCellRenderer(GradientListCellRenderer.INSTANCE);
		setMessageType(PLAIN_MESSAGE);
		setOptionType(OK_CANCEL_OPTION);
		Object[] contents = new Object[] {
				message,
				new JScrollPane(gradientList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
				selectAllButton,
				selectNoneButton};
		setMessage(contents);

		ButtonListener buttonListener = new ButtonListener();
		selectAllButton.addActionListener(buttonListener);
		selectNoneButton.addActionListener(buttonListener);
	}

	public List<Gradient> getSelectedGradients() {
		ListModel<Gradient> listModel = gradientList.getModel();
		ListSelectionModel listSelectionModel = gradientList.getSelectionModel();
		List<Gradient> rv = new ArrayList<>(listModel.getSize());
		for (int i = 0; i < listModel.getSize(); i++) {
			if (listSelectionModel.isSelectedIndex(i)) {
				rv.add(listModel.getElementAt(i));
			}
		}
		return rv;
	}

	public boolean isSelectionEmpty() {
		return gradientList.isSelectionEmpty();
	}

	public static void main(final String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				swingMain(args);
			}
		});
	}

	private class ButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == selectAllButton) {
				gradientList.setSelectionInterval(0, gradientList.getModel().getSize() - 1);
			} else if (e.getSource() == selectNoneButton) {
				gradientList.clearSelection();
			}
		}
	}

	public static void swingMain(String[] args) {
		GradientSelectionPane gsp = new GradientSelectionPane(Profile.getSampleGradients(), "Select which gradients to import:");
		JDialog gspDialog = gsp.createDialog(GradientSelectionPane.class.getSimpleName());
		gspDialog.setVisible(true);
		gspDialog.dispose();
	}
}

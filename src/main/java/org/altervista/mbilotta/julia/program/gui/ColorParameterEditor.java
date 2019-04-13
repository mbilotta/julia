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
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.BevelBorder;
import javax.swing.colorchooser.ColorSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.altervista.mbilotta.julia.program.parsers.Parameter;



public class ColorParameterEditor extends JButton implements ActionListener, ChangeListener {

	private static TransferHandler TRANSFER_HANDLER;

	private JColorChooser colorChooser;
	private JDialog colorChooserDialog;

	private final Parameter<Color> subject;
	private final boolean modal;
	private PreviewUpdater previewUpdater;

	static final int ICON_WIDTH = 52;
	static final int ICON_HEIGHT = 22;

	public ColorParameterEditor(Parameter<Color> subject, Color value, boolean modal) {
		setIcon(new ColorIcon(ICON_WIDTH, ICON_HEIGHT, value == null ? Color.BLACK : value));
		addActionListener(this);
		this.subject = subject;
		this.modal = modal;

		if (TRANSFER_HANDLER != null) {
			setTransferHandler(TRANSFER_HANDLER);
		} else if (subject != null) {
			TRANSFER_HANDLER = new EditorTransferHandler(subject);
			setTransferHandler(TRANSFER_HANDLER);
		}
		if (getTransferHandler() != null) {
			ActionMap actionMap = getActionMap();
			actionMap.put("copy", TransferHandler.getCopyAction());
			actionMap.put("paste", TransferHandler.getPasteAction());
		}
	}

	public void setValue(Color value) {
		if (!value.equals(getValue())) {
			setIcon(new ColorIcon(ICON_WIDTH, ICON_HEIGHT, value));
			if (previewUpdater != null)
				previewUpdater.updatePreview(subject, value);
		}
	}

	public Color getValue() {
		return ((ColorIcon) getIcon()).getColor();
	}

	public void stateChanged(ChangeEvent e) {
		ColorSelectionModel selectionModel = (ColorSelectionModel) e.getSource();
		if (previewUpdater != null)
			previewUpdater.updatePreview(subject, selectionModel.getSelectedColor());
	}

	public void actionPerformed(ActionEvent e) {
		if (colorChooserDialog == null) {
			colorChooser = createColorChooser();
			colorChooserDialog = new ColorChooserDialog(this, "Seleziona un colore");
			colorChooserDialog.setVisible(true);
		} else {
			colorChooserDialog.toFront();
		}
	}

	public void setPreviewUpdater(PreviewUpdater previewUpdater) {
		this.previewUpdater = previewUpdater;
	}

	public void addParameterChangeListener(final ParameterChangeListener listener) {
		addPropertyChangeListener(JButton.ICON_CHANGED_PROPERTY, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				listener.parameterChanged(ColorParameterEditor.this.subject, ((ColorIcon) e.getNewValue()).getColor());
			}
		});
	}

	public void dispose() {
		if (colorChooserDialog != null) {
			colorChooserDialog.setVisible(false);
			colorChooserDialog.dispose();
			colorChooserDialog = null;
			colorChooser = null;
		}
	}

	private JColorChooser createColorChooser() {
		JColorChooser rv = new JColorChooser(getValue());
		rv.setPreviewPanel(new JPanel());
		rv.getSelectionModel().addChangeListener(this);
		return rv;
	}

	private static JComponent addPreviewPanel(JColorChooser colorChooser) {
		JPanel rv = new JPanel(new BorderLayout(0, 5));
		ColorPreviewPanel previewPanel = new ColorPreviewPanel(0, 50);
		previewPanel.setColor(colorChooser.getColor());
		previewPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		colorChooser.getSelectionModel().addChangeListener(previewPanel);
		CollapsiblePanel collapsiblePreviewPanel = new CollapsiblePanel(previewPanel, "Colore selezionato", false);
		collapsiblePreviewPanel.addChangeListener(new CollapsiblePanel.WindowResizer());
		rv.add(colorChooser, BorderLayout.CENTER);
		rv.add(collapsiblePreviewPanel, BorderLayout.SOUTH);
		return rv;
	}

	private class OptionPaneListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent e) {
			if (colorChooserDialog != null &&
					e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY) &&
					e.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {

				int option = (Integer) e.getNewValue();
				Color color = colorChooser.getColor();
				disposeColorChooserDialog();
				if (!color.equals(getValue())) {
					if (option == JOptionPane.OK_OPTION) {
						setIcon(new ColorIcon(ICON_WIDTH, ICON_HEIGHT, color));
					} else if (previewUpdater != null) {
						previewUpdater.updatePreview(subject, getValue());
					}
				}
			}
		}

		private void disposeColorChooserDialog() {
			colorChooserDialog.setVisible(false);
			colorChooserDialog.dispose();
			colorChooserDialog = null;
			colorChooser = null;
		}
	}

	private class ColorChooserDialog extends JDialog {

		public ColorChooserDialog(JComponent parentComponent, String title) {
			super(SwingUtilities.windowForComponent(parentComponent), title, modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
			init(parentComponent);
		}

		private void init(JComponent parentComponent) {
			final JOptionPane optionPane = new JOptionPane(
				addPreviewPanel(colorChooser),
				JOptionPane.PLAIN_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION);

			setComponentOrientation(optionPane.getComponentOrientation());

			Container contentPane = getContentPane();
			contentPane.setLayout(new BorderLayout());
			contentPane.add(optionPane, BorderLayout.CENTER);

			setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					optionPane.setValue(JOptionPane.CLOSED_OPTION);
				}
			});
			optionPane.addPropertyChangeListener(new OptionPaneListener());

			pack();
			setLocationRelativeTo(parentComponent);

			setMinimumSize(getPreferredSize());
			setResizable(false);
		}
	}
}

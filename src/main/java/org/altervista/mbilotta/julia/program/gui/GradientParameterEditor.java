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
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.ActionMap;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.MutableComboBoxModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.altervista.mbilotta.julia.Gradient;
import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.program.Profile;
import org.altervista.mbilotta.julia.program.parsers.Parameter;


public class GradientParameterEditor extends JComboBox<Object> implements ItemListener {
	
	private static TransferHandler TRANSFER_HANDLER;

	private static final Object EDIT_ITEM = "Edit...";
	private static final Object IMPORT_FROM_FILE_ITEM = "Import from file...";
	private static final Object EXPORT_TO_FILE_ITEM = "Export to file...";

	private GradientEditor gradientEditor;
	private JDialog gradientEditorDialog;

	private Parameter<Gradient> subject;
	private PreviewUpdater previewUpdater;

	private Gradient value;
	private Gradient lastPreviewUpdate;
	private boolean changed = true;

	public GradientParameterEditor() {
		this(Profile.getSampleGradients());
	}

	public GradientParameterEditor(List<Gradient> gradients) {
		super(createComboBoxModel(gradients, null));
		setRenderer(GradientListCellRenderer.INSTANCE);
		value = (Gradient) getSelectedItem();
		addItemListener(this);
	}

	public GradientParameterEditor(Parameter<Gradient> subject) {
		this(subject, subject == null ? null : subject.getHint(0));
	}

	public GradientParameterEditor(Parameter<Gradient> subject, Gradient initialValue) {
		this(subject, subject == null ? new LinkedList<Gradient>() : subject.getHints(), initialValue);
	}

	public GradientParameterEditor(Parameter<Gradient> subject, List<Gradient> gradients, Gradient initialValue) {
		super(createComboBoxModel(gradients, initialValue));
		setRenderer(GradientListCellRenderer.INSTANCE);
		value = (Gradient) getSelectedItem();
		addItemListener(this);
		this.subject = subject;

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

	public Gradient getValue() {
		return value;
	}

	public void setValue(Gradient gradient) {
		MutableComboBoxModel<Object> model = (MutableComboBoxModel<Object>) getModel();
		if (indexOf(gradient, model.getSize() - 3) == -1) {
			model.insertElementAt(gradient, model.getSize() - 3);
		}
		model.setSelectedItem(gradient);
	}

	public List<Gradient> getGradients() {
		ComboBoxModel<Object> model = getModel();
		List<Gradient> rv = new ArrayList<>(model.getSize() - 3);
		for (int i = 0; i < model.getSize() - 3; i++) {
			rv.add((Gradient) model.getElementAt(i));
		}
		return rv;
	}

	public void dispose() {
		if (gradientEditorDialog != null) {
			gradientEditorDialog.setVisible(false);
			gradientEditorDialog.dispose();
			gradientEditorDialog = null;
			gradientEditor = null;
		}
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			Object selectedItem = e.getItem();
			if (selectedItem == EDIT_ITEM) {
				doEdit();
			} else if (selectedItem == IMPORT_FROM_FILE_ITEM) {
				doImport();
			} else if (selectedItem == EXPORT_TO_FILE_ITEM) {
				doExport();
			} else if (changed) {
				Gradient selectedGradient = (Gradient) selectedItem;
				value = selectedGradient;
				if (previewUpdater != null && !selectedGradient.equals(lastPreviewUpdate)) {
					previewUpdater.updatePreview(subject, selectedGradient);
					lastPreviewUpdate = selectedGradient;
				}
			}
		}
	}

	private void doEdit() {
		changed = false;
		setSelectedItem(value);
		changed = true;
		if (gradientEditorDialog == null) {
			gradientEditor = createGradientEditor();
			gradientEditorDialog = new GradientEditorDialog(this, "Edit gradient");
			gradientEditorDialog.setVisible(true);
		} else {
			gradientEditorDialog.toFront();
		}
	}

	private void doImport() {
		changed = false;
		setSelectedItem(value);
		changed = true;
		JFileChooser fc = new JFileChooser();
		int rv = fc.showOpenDialog(this);
		if (rv == JFileChooser.APPROVE_OPTION) {
			List<Gradient> gradients;
			try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fc.getSelectedFile()))) {
				gradients = Utilities.readNonNullList(in, "gradients", Gradient.class);
			} catch (ClassNotFoundException | IOException e) {
				MessagePane.showReadErrorMessage(this, fc.getSelectedFile(), e);
				return;
			}
			GradientSelectionPane gsp = new GradientSelectionPane(gradients, "Select which gradients to import:");
			JDialog gspDialog = gsp.createDialog(this, "Select gradients");
			gspDialog.setVisible(true);
			gspDialog.dispose();
			if (gsp.getValue() != null && gsp.getValue().equals(JOptionPane.OK_OPTION) && !gsp.isSelectionEmpty()) {
				List<Gradient> selectedGradients = gsp.getSelectedGradients();
				MutableComboBoxModel<Object> model = (MutableComboBoxModel<Object>) getModel();
				int size = model.getSize() - 3;
				for (Gradient gradient : selectedGradients) {
					if (indexOf(gradient, size) == -1) {
						model.insertElementAt(gradient, model.getSize() - 3);
					}
				}
			}
		}
	}

	private int indexOf(Gradient gradient, int size) {
		ComboBoxModel<Object> model = getModel();
		for (int i = 0; i < size; i++) {
			if (model.getElementAt(i).equals(gradient)) {
				return i;
			}
		}
		
		return -1;
	}

	private void doExport() {
		changed = false;
		setSelectedItem(value);
		changed = true;
		ComboBoxModel<Object> model = getModel();
		List<Gradient> gradients = new ArrayList<>(getModel().getSize() - 3);
		for (int i = 0; i < model.getSize() - 3; i++) {
			gradients.add((Gradient) model.getElementAt(i));
		}
		GradientSelectionPane gsp = new GradientSelectionPane(gradients, "Select which gradients to export:");
		JDialog gspDialog = gsp.createDialog(this, "Select gradients");
		gspDialog.setVisible(true);
		gspDialog.dispose();
		if (gsp.getValue() != null && gsp.getValue().equals(JOptionPane.OK_OPTION) && !gsp.isSelectionEmpty()) {
			List<Gradient> selectedGradients = gsp.getSelectedGradients();
			JFileChooser fc = new JFileChooser();
			int rv = fc.showSaveDialog(this);
			if (rv == JFileChooser.APPROVE_OPTION) {
				try (FileOutputStream fos = new FileOutputStream(fc.getSelectedFile());
						BufferedOutputStream bos = new BufferedOutputStream(fos, 2048);
						ObjectOutputStream oos = new ObjectOutputStream(bos)) {
					Utilities.writeList(oos, selectedGradients);
				} catch (IOException e) {
					MessagePane.showWriteErrorMessage(this, fc.getSelectedFile(), e);
				}
			}
		}
	}

	private GradientEditor createGradientEditor() {
		GradientEditor rv = new GradientEditor(value);
		rv.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (previewUpdater != null) {
					Gradient editedGradient = ((GradientEditor) e.getSource()).getGradient();
					if (editedGradient != null) {
						previewUpdater.updatePreview(subject, editedGradient);
						lastPreviewUpdate = editedGradient;
					}
				}
			}
		});

		return rv;
	}

	public void setPreviewUpdater(PreviewUpdater previewUpdater) {
		this.previewUpdater = previewUpdater;
	}

	public void addParameterChangeListener(final ParameterChangeListener listener) {
		addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof Gradient && changed) {
					listener.parameterChanged(GradientParameterEditor.this.subject, e.getItem());
				}
			}
		});
	}

	private static DefaultComboBoxModel<Object> createComboBoxModel(List<Gradient> gradients, Gradient selected) {
		Vector<Object> actualItems = new Vector<>(gradients.size() + 4);
		boolean found = false;
		for (Gradient gradient : gradients) {
			if (!found && gradient.equals(selected))
				found = true;
			actualItems.add(gradient);
		}
		if (selected != null && !found)
			actualItems.add(selected);

		actualItems.add(EDIT_ITEM);
		actualItems.add(IMPORT_FROM_FILE_ITEM);
		actualItems.add(EXPORT_TO_FILE_ITEM);

		DefaultComboBoxModel<Object> comboBoxModel = new DefaultComboBoxModel<>(actualItems);
		if (selected != null)
			comboBoxModel.setSelectedItem(selected);

		return comboBoxModel;
	}

	private class OptionPaneListener implements PropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent e) {
			if (gradientEditorDialog != null &&
					e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY) &&
					e.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {
				int option = (Integer) e.getNewValue();
				if (option == JOptionPane.OK_OPTION) {
					Gradient editedGradient = gradientEditor.getGradient();
					if (editedGradient != null) {
						DefaultComboBoxModel<Object> comboBoxModel = (DefaultComboBoxModel<Object>) getModel();
						int index = comboBoxModel.getIndexOf(editedGradient);
						if (index == -1) {
							comboBoxModel.insertElementAt(editedGradient, comboBoxModel.getSize() - 3);
							changed = true;
						} else {
							changed = !value.equals(comboBoxModel.getElementAt(index));
						}
						comboBoxModel.setSelectedItem(editedGradient);
						changed = true;

						disposeGradientEditorDialog();
					} else {
						Toolkit.getDefaultToolkit().beep();
						JOptionPane.showMessageDialog(
								gradientEditorDialog,
								"Please at least add two colors.",
								"Julia",
								JOptionPane.INFORMATION_MESSAGE);

						JOptionPane optionPane = (JOptionPane) e.getSource();
						optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
					}
				} else if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
					disposeGradientEditorDialog();

					Gradient selectedGradient = value;
					if (previewUpdater != null && !selectedGradient.equals(lastPreviewUpdate)) {
						previewUpdater.updatePreview(subject, selectedGradient);
						lastPreviewUpdate = selectedGradient;
					}
				}
			}
		}
		
		private void disposeGradientEditorDialog() {
			gradientEditorDialog.setVisible(false);
			gradientEditorDialog.dispose();
			gradientEditorDialog = null;
			gradientEditor = null;
		}
	}

	private class GradientEditorDialog extends JDialog {

		public GradientEditorDialog(JComponent parentComponent, String title) {
			super(SwingUtilities.windowForComponent(parentComponent), title);
			init(parentComponent);
		}

		private void init(JComponent parentComponent) {
			final JOptionPane optionPane = new JOptionPane(
				gradientEditor,
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
		}
	}
}

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

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.altervista.mbilotta.julia.Gradient;



class GradientEditor extends JPanel {

	private JList<Color> list;
	private DefaultListModel<Color> listModel;
	private JColorChooser colorChooser;
	private JButton addButton;
	private JButton insertButton;
	private JButton replaceButton;
	private JButton removeButton;
	private JButton clearButton;
	private JButton pickButton;
	private JButton moveUpButton;
	private JButton moveDownButton;
	private LocationsEditor locationsEditor;

	static final ChessboardPainter chessboardPainter = new ChessboardPainter(5);

	private ChangeEvent changeEvent;
	
	private Gradient gradientCache;
	private int transparentCount = 0;

	static final int COLOR_PREVIEW_WIDTH = 100;
	static final int COLOR_PREVIEW_HEIGHT = 50;
	static final int COLOR_LIST_PREFERRED_WIDTH = 120;
	static final int COLOR_RENDERER_HEIGHT = 25;
	static final int COLOR_RENDERER_GAP = 5;

	static final int GRADIENT_PREVIEW_HEIGHT = chessboardPainter.getSquareSize() * 10;

	public GradientEditor(Gradient gradient) {
		super(new BorderLayout(0, 5));

		init(gradient);
		registerListeners();
		buildGUI();
	}

	public GradientEditor() {
		this(null);
	}

	public void setGradient(Gradient gradient) {
		listModel.clear();
		transparentCount = 0;
		if (gradient != null) {
			int numOfStops = gradient.getNumOfStops();
			for (int i = 0; i < numOfStops; i++) {
				Color color = gradient.getStop(i).getColor();
				listModel.addElement(color);
				if (color.getAlpha() != 255) transparentCount++;
			}

			locationsEditor.setLocations(gradient);
			gradientCache = gradient;
		}
	}

	public Gradient getGradient() {
		if (gradientCache != null)
			return gradientCache;

		int size = listModel.size();
		if (size < 2)
			return null;

		ArrayList<Float> locations = locationsEditor.getLocations();
		Gradient.Stop[] stops = new Gradient.Stop[size];
		for (int i = 0; i < size; i++) {
			stops[i] = new Gradient.Stop(locations.get(i), listModel.get(i));
		}

		gradientCache = new Gradient(stops);
		return gradientCache;
	}

	public boolean hasGradient() {
		return listModel.size() >= 2;
	}

	public boolean hasTransparentGradient() {
		return hasGradient() && transparentCount > 0;
	}

	public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

	protected void fireStateChanged() {
		gradientCache = null;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
            }
        }
    }
	
	int getTrasparentCount() {
		return transparentCount;
	}

	private class ButtonListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == addButton) {
				performAdd();
			} else if (e.getSource() == insertButton) {
				performInsert();
			} else if (e.getSource() == replaceButton) {
				performReplace();
			} else if (e.getSource() == moveUpButton) {
				performMoveUp();
			} else if (e.getSource() == moveDownButton) {
				performMoveDown();
			} else if (e.getSource() == removeButton) {
				performRemove();
			} else if (e.getSource() == clearButton) {
				performClear();
			} else if (e.getSource() == pickButton) {
				performPick();
			}
		}
	}

	private void init(Gradient gradient) {
		listModel = new DefaultListModel<Color>();
		list = new JList<Color>(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		list.setCellRenderer(new ColorListCellRenderer(COLOR_RENDERER_HEIGHT, COLOR_RENDERER_GAP, chessboardPainter));

		locationsEditor = new LocationsEditor(this);
		listModel.addListDataListener(locationsEditor);

		setGradient(gradient);

		colorChooser = new JColorChooser();
		colorChooser.setPreviewPanel(new JPanel());

		addButton = new JButton("Add");
		addButton.setMnemonic(KeyEvent.VK_A);

		insertButton = new JButton("Insert");
		insertButton.setMnemonic(KeyEvent.VK_I);

		replaceButton = new JButton("Replace");
		replaceButton.setMnemonic(KeyEvent.VK_E);

		moveUpButton = new JButton("Move up");
		moveUpButton.setMnemonic(KeyEvent.VK_U);

		moveDownButton = new JButton("Move down");
		moveDownButton.setMnemonic(KeyEvent.VK_D);

		removeButton = new JButton("Remove");
		removeButton.setMnemonic(KeyEvent.VK_V);

		clearButton = new JButton("Clear");
		clearButton.setMnemonic(KeyEvent.VK_R);

		pickButton = new JButton("Pick");
		pickButton.setMnemonic(KeyEvent.VK_K);
	}
	
	private void buildGUI() {
		CollapsiblePanel topGUI = buildTopGUI();
		CollapsiblePanel bottomGUI = buildBottomGUI();
		ChangeListener windowResizer = new CollapsiblePanel.WindowResizer();
		topGUI.addChangeListener(windowResizer);
		bottomGUI.addChangeListener(windowResizer);

		Dimension topSize = topGUI.getChild().getPreferredSize();
		JScrollPane bottomScroller = (JScrollPane) bottomGUI.getChild();
		Dimension bottomSize = bottomScroller.getPreferredSize();
		bottomSize.width = topSize.width;
		bottomScroller.setPreferredSize(bottomSize);

		add(topGUI, BorderLayout.CENTER);
		add(bottomGUI, BorderLayout.SOUTH);
	}

	private CollapsiblePanel buildTopGUI() {
		JPanel topGUI = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.VERTICAL;
		c.gridx = 0;
		c.gridy = 0;
//		c.gridwidth = 1;
		c.gridheight = 9;
		c.weightx = 1.0;
//		c.weighty = 0.0;
//		c.insets = new Insets(0, 0, 0, 0);
		topGUI.add(colorChooser, c);

		c.anchor = GridBagConstraints.LINE_END;
		c.fill = GridBagConstraints.VERTICAL;
		c.gridx = 2;
//		c.gridy = 0;
//		c.gridwidth = 1;
//		c.gridheight = 9;
//		c.weightx = 1.0;
//		c.weighty = 0.0;
//		c.insets = new Insets(0, 0, 0, 0);
		JScrollPane listScroller = new JScrollPane(list,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		listScroller.setMinimumSize(new Dimension(COLOR_LIST_PREFERRED_WIDTH, 0));
		listScroller.setPreferredSize(new Dimension(COLOR_LIST_PREFERRED_WIDTH, 0));
		topGUI.add(listScroller, c);

		c.anchor = GridBagConstraints.PAGE_END;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 1;
//		c.gridy = 0;
//		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 1.0;
		c.insets = new Insets(0, 5, 0, 5);
		ColorPreviewPanel colorPreview = new ColorPreviewPanel(
				COLOR_PREVIEW_WIDTH,
				COLOR_PREVIEW_HEIGHT);
		colorChooser.getSelectionModel().addChangeListener(colorPreview);
		colorPreview.setColor(colorChooser.getColor());
		colorPreview.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		topGUI.add(colorPreview, c);

		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
//		c.gridx = 1;
		c.gridy = 1;
//		c.gridwidth = 1;
//		c.gridheight = 1;
//		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(2, 5, 0, 5);
		topGUI.add(addButton, c);

//		c.anchor = GridBagConstraints.CENTER;
//		c.fill = GridBagConstraints.HORIZONTAL;
//		c.gridx = 1;
		c.gridy = 2;
//		c.gridwidth = 1;
//		c.gridheight = 1;
//		c.weightx = 0.0;
//		c.weighty = 0.0;
//		c.insets = new Insets(2, 5, 0, 5);
		topGUI.add(insertButton, c);

//		c.anchor = GridBagConstraints.CENTER;
//		c.fill = GridBagConstraints.HORIZONTAL;
//		c.gridx = 1;
		c.gridy = 3;
//		c.gridwidth = 1;
//		c.gridheight = 1;
//		c.weightx = 0.0;
//		c.weighty = 0.0;
//		c.insets = new Insets(2, 5, 0, 5);
		topGUI.add(replaceButton, c);

//		c.anchor = GridBagConstraints.CENTER;
//		c.fill = GridBagConstraints.HORIZONTAL;
//		c.gridx = 1;
		c.gridy = 4;
//		c.gridwidth = 1;
//		c.gridheight = 1;
//		c.weightx = 0.0;
//		c.weighty = 0.0;
//		c.insets = new Insets(2, 5, 0, 5);
		topGUI.add(moveUpButton, c);

//		c.anchor = GridBagConstraints.CENTER;
//		c.fill = GridBagConstraints.HORIZONTAL;
//		c.gridx = 1;
		c.gridy = 5;
//		c.gridwidth = 1;
//		c.gridheight = 1;
//		c.weightx = 0.0;
//		c.weighty = 0.0;
//		c.insets = new Insets(2, 5, 0, 5);
		topGUI.add(moveDownButton, c);

//		c.anchor = GridBagConstraints.CENTER;
//		c.fill = GridBagConstraints.HORIZONTAL;
//		c.gridx = 1;
		c.gridy = 6;
//		c.gridwidth = 1;
//		c.gridheight = 1;
//		c.weightx = 0.0;
//		c.weighty = 0.0;
//		c.insets = new Insets(2, 5, 0, 5);
		topGUI.add(removeButton, c);

//		c.anchor = GridBagConstraints.CENTER;
//		c.fill = GridBagConstraints.HORIZONTAL;
//		c.gridx = 1;
		c.gridy = 7;
//		c.gridwidth = 1;
//		c.gridheight = 1;
//		c.weightx = 0.0;
//		c.weighty = 0.0;
//		c.insets = new Insets(2, 5, 0, 5);
		topGUI.add(clearButton, c);

		c.anchor = GridBagConstraints.PAGE_START;
//		c.fill = GridBagConstraints.HORIZONTAL;
//		c.gridx = 1;
		c.gridy = 8;
//		c.gridwidth = 1;
//		c.gridheight = 1;
//		c.weightx = 0.0;
		c.weighty = 1.0;
//		c.insets = new Insets(2, 5, 0, 5);
		topGUI.add(pickButton, c);

		return new CollapsiblePanel(topGUI, "Colors", false);
	}

	private CollapsiblePanel buildBottomGUI() {
		return new CollapsiblePanel(
				new JScrollPane(locationsEditor, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS),
				"Color locations",
				false);
	}

	private void registerListeners() {
		ActionListener buttonListener = new ButtonListener();

		addButton.addActionListener(buttonListener);
		insertButton.addActionListener(buttonListener);
		replaceButton.addActionListener(buttonListener);
		moveUpButton.addActionListener(buttonListener);
		moveDownButton.addActionListener(buttonListener);
		removeButton.addActionListener(buttonListener);
		clearButton.addActionListener(buttonListener);
		pickButton.addActionListener(buttonListener);
	}

	private void performAdd() {
		Color c = colorChooser.getColor();
		if (c.getAlpha() != 255)
			transparentCount++;
		listModel.addElement(c);
		int lastIndex = listModel.size() - 1;
		list.setSelectedIndex(lastIndex);
		list.ensureIndexIsVisible(lastIndex);
	}

	private void performInsert() {
		if (list.isSelectionEmpty()) {
			showEmptySelectionWarning();
		} else if (list.getMinSelectionIndex() < list.getMaxSelectionIndex()) {
			showAmbiguousSelectionWarning();
		} else {
			Color c = colorChooser.getColor();
			if (c.getAlpha() != 255)
				transparentCount++;
			int insertionIndex = list.getSelectedIndex();
			listModel.add(insertionIndex, c);
			list.setSelectedIndex(insertionIndex + 1);
			list.ensureIndexIsVisible(insertionIndex + 1);
		}
	}

	private void performMoveUp() {
		if (list.isSelectionEmpty()) {
			showEmptySelectionWarning();
		} else if (list.getMinSelectionIndex() < list.getMaxSelectionIndex()) {
			showAmbiguousSelectionWarning();
		} else {
			DefaultListModel<Color> model = (DefaultListModel<Color>) list.getModel();
			int srcIndex = list.getSelectedIndex();
			int dstIndex = srcIndex - 1;
			if (dstIndex == -1)
				dstIndex = model.getSize() - 1;
			
			locationsEditor.setListDataEventsEnabled(false);
			Color color = model.remove(srcIndex);
			model.add(dstIndex, color);
			locationsEditor.colorMovedUp(srcIndex, dstIndex);
			locationsEditor.setListDataEventsEnabled(true);
			list.setSelectedIndex(dstIndex);
			list.ensureIndexIsVisible(dstIndex);
		}
	}

	private void performMoveDown() {
		if (list.isSelectionEmpty()) {
			showEmptySelectionWarning();
		} else if (list.getMinSelectionIndex() < list.getMaxSelectionIndex()) {
			showAmbiguousSelectionWarning();
		} else {
			DefaultListModel<Color> model = (DefaultListModel<Color>) list.getModel();
			int srcIndex = list.getSelectedIndex();
			int dstIndex = srcIndex + 1;
			if (dstIndex == model.getSize())
				dstIndex = 0;
			
			locationsEditor.setListDataEventsEnabled(false);
			Color color = model.remove(srcIndex);
			model.add(dstIndex, color);
			locationsEditor.colorMovedDown(srcIndex, dstIndex);
			locationsEditor.setListDataEventsEnabled(true);
			list.setSelectedIndex(dstIndex);
			list.ensureIndexIsVisible(dstIndex);
		}
	}

	private void performRemove() {
		if (list.isSelectionEmpty()) {
			showEmptySelectionWarning();
		} else {
			int fromIndex = list.getMinSelectionIndex();
			int toIndex = list.getMaxSelectionIndex();
			for (int i = fromIndex; i <= toIndex; i++) {
				if (listModel.get(i).getAlpha() != 255)
					transparentCount--;
			}
			listModel.removeRange(fromIndex, toIndex);
			list.clearSelection();
		}
	}

	private void performReplace() {
		if (list.isSelectionEmpty()) {
			showEmptySelectionWarning();
		} else if (list.getMinSelectionIndex() < list.getMaxSelectionIndex()) {
			showAmbiguousSelectionWarning();
		} else {
			Color c = colorChooser.getColor();
			if (c.getAlpha() != 255)
				transparentCount++;
			int replacingIndex = list.getSelectedIndex();
			Color replaced = listModel.get(replacingIndex);
			if (replaced.getAlpha() != 255)
				transparentCount--;
			
			if (!c.equals(replaced))
				listModel.set(replacingIndex, c);

			list.ensureIndexIsVisible(replacingIndex);
		}
	}

	private void performClear() {
		transparentCount = 0;
		listModel.clear();
	}

	private void performPick() {
		if (list.isSelectionEmpty()) {
			showEmptySelectionWarning();
		} else {
			colorChooser.setColor(list.getSelectedValue());
		}
	}

	private void showEmptySelectionWarning() {
		JOptionPane.showMessageDialog(
				this,
				"Please select at least one color from the right.", "Julia",
				JOptionPane.INFORMATION_MESSAGE);
	}

	private void showAmbiguousSelectionWarning() {
		JOptionPane.showMessageDialog(
				this,
				"Please select at most one color from the right.", "Julia",
				JOptionPane.INFORMATION_MESSAGE);
	}
}

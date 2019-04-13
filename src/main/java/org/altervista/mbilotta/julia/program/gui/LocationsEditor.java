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

import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.altervista.mbilotta.julia.Gradient;



class LocationsEditor extends JPanel implements ListDataListener {

	private GradientEditor client;
	private ActualEditor editor;
	private ArrayList<Float> locations;
	private boolean listDataEventsEnabled = true;

	private static final TableModel NO_GRADIENT_TABLE_MODEL = new NoGradientTableModel();

	public LocationsEditor(GradientEditor client) {
		super(new BorderLayout());
		this.client = client;
		this.editor = new ActualEditor();

		buildGUI();
	}

	@Override
	public void intervalAdded(ListDataEvent e) {
		if (listDataEventsEnabled) {
			ListModel<Color> colorListModel = (ListModel<Color>) e.getSource();
			int stopsCount = colorListModel.getSize();
			if (stopsCount == 2) {
				locations = new ArrayList<>();
				locations.add(0f);
				locations.add(1f);
				editor.setModel(new SubgradientsTableModel(colorListModel));
				client.fireStateChanged();
			} else if (stopsCount > 2) {
				int index = e.getIndex0();
				if (index == 0) {
					locations.add(1, locations.get(1) / 2);
				} else if (index == stopsCount - 1) {
					locations.add(index - 1, (locations.get(index - 2) + 1) / 2);
				} else {
					locations.add(index, (locations.get(index) + locations.get(index - 1)) / 2);
				}
	
				((AbstractTableModel) editor.getModel()).fireTableStructureChanged();
				client.fireStateChanged();
			}
		}
	}

	@Override
	public void intervalRemoved(ListDataEvent e) {
		if (listDataEventsEnabled) {
			ListModel<Color> colorListModel = (ListModel<Color>) e.getSource();
			int stopsCount = colorListModel.getSize();
			if (stopsCount >= 2) {
				int fromIndex = e.getIndex0();
				int toIndex = e.getIndex1();
				int rangeLength = toIndex - fromIndex + 1;
				while (rangeLength > 0) {
					locations.remove(fromIndex);
					rangeLength--;
				}
				locations.set(0, 0f);
				locations.set(stopsCount - 1, 1f);
	
				((AbstractTableModel) editor.getModel()).fireTableStructureChanged();
				client.fireStateChanged();
			} else if (editor.getModel() != NO_GRADIENT_TABLE_MODEL) {
				editor.setModel(NO_GRADIENT_TABLE_MODEL);
				client.fireStateChanged();
			}
		}
	}

	@Override
	public void contentsChanged(ListDataEvent e) {
		if (listDataEventsEnabled) {
			ListModel<Color> colorListModel = (ListModel<Color>) e.getSource();
			int replacedIndex = e.getIndex0();
			Rectangle clip;
			if (replacedIndex == 0)
				clip = editor.getCellRect(0, replacedIndex, true);
			else if (replacedIndex == colorListModel.getSize() - 1)
				clip = editor.getCellRect(0, replacedIndex - 1, true);
			else
				clip = editor.getCellRect(0, replacedIndex - 1, true)
					.union(editor.getCellRect(0, replacedIndex, true));
			
			editor.repaint(clip);
			client.fireStateChanged();
		}
	}

	public void colorMovedUp(int srcIndex, int dstIndex) {
		if (srcIndex == 0 || locations.size() == 2) {
			editor.repaint();
		} else {
			Rectangle clip = editor.getCellRect(0, dstIndex, true);
			if (dstIndex > 0)
				clip = clip.union(editor.getCellRect(0, dstIndex - 1, true));
			if (srcIndex < locations.size() - 1)
				clip = clip.union(editor.getCellRect(0, srcIndex, true));
			editor.repaint(clip);
		}
		client.fireStateChanged();
	}

	public void colorMovedDown(int srcIndex, int dstIndex) {
		if (srcIndex == locations.size() - 1 || locations.size() == 2) {
			editor.repaint();
		} else {
			Rectangle clip = editor.getCellRect(0, srcIndex, true);
			if (srcIndex > 0)
				clip = clip.union(editor.getCellRect(0, srcIndex - 1, true));
			if (dstIndex < locations.size() - 1)
				clip = clip.union(editor.getCellRect(0, dstIndex, true));
			editor.repaint(clip);
		}
		client.fireStateChanged();
	}

	public void setListDataEventsEnabled(boolean listDataEventsEnabled) {
		this.listDataEventsEnabled = listDataEventsEnabled;
	}

	public void setLocations(Gradient gradient) {
		locations = new ArrayList<>();
		int numOfStops = gradient.getNumOfStops();
		locations.ensureCapacity(numOfStops);
		for (int i = 0; i < numOfStops; i++) {
			locations.add(gradient.getStop(i).getLocation());
		}
		
		((AbstractTableModel) editor.getModel()).fireTableStructureChanged();
	}

	public ArrayList<Float> getLocations() {
		if (editor.getModel() == NO_GRADIENT_TABLE_MODEL)
			return null;

		return locations;
	}

	private void buildGUI() {
		add(editor.getTableHeader(), BorderLayout.PAGE_START);
		add(editor, BorderLayout.CENTER);
	}

	private class ActualEditor extends JTable {

		public ActualEditor() {
			super(NO_GRADIENT_TABLE_MODEL);
			setDefaultRenderer(Object.class, new NoGradientRenderer());
			setDefaultRenderer(Color[].class, new SubgradientRenderer());
			setAutoCreateColumnsFromModel(true);

			setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
			getTableHeader().setReorderingAllowed(false);	
			setCellSelectionEnabled(false);

			setIntercellSpacing(new Dimension(0, 0));
			setShowGrid(false);

			setRowHeight(GradientEditor.GRADIENT_PREVIEW_HEIGHT);
			setFocusable(false);
			setOpaque(false);
		}

		@Override
		public void paintComponent(Graphics g) {
			if (getModel() == NO_GRADIENT_TABLE_MODEL || client.getTrasparentCount() > 0) {
				Rectangle clip = g.getClipBounds();
				GradientEditor.chessboardPainter.paintChessboard((Graphics2D) g, ActualEditor.this, clip);
			}

			super.paintComponent(g);
		}

		@Override
		public void doLayout() {
			TableColumnModel columnModel = getColumnModel();
			int columnCount = columnModel.getColumnCount();
			TableColumn resizingColumn = tableHeader != null ? tableHeader.getResizingColumn() : null;
			if (resizingColumn != null) {
				super.doLayout();
				if (getModel() != NO_GRADIENT_TABLE_MODEL) {
					int resizedColumnIndex = resizingColumn.getModelIndex();
					if (resizedColumnIndex < columnCount - 1) {
						float totalColumnWidth = ActualEditor.this.getWidth();
						int currentColumnPos = 0;
						int nextColumnIndex = resizedColumnIndex + 1;
						for (int i = 0; i < nextColumnIndex; i++) {
							currentColumnPos += columnModel.getColumn(i).getWidth();
						}
						locations.set(nextColumnIndex, currentColumnPos / totalColumnWidth);
						SubgradientsTableModel tableModel = (SubgradientsTableModel) getModel();
						TableColumn nextColumn = columnModel.getColumn(nextColumnIndex);
						nextColumn.setHeaderValue(tableModel.getColumnName(nextColumnIndex));
					}
					client.fireStateChanged();
				} else {
//					System.out.println("L'IF CI VUOLE!");
				}
			} else {
				int tableWidth = ActualEditor.this.getWidth();
				if (getModel() == NO_GRADIENT_TABLE_MODEL) {
					columnModel.getColumn(0).setWidth(tableWidth);
				} else {
					int lastColumnIndex = columnCount - 1;
					int reminder = tableWidth;
					for (int i = 0; i < lastColumnIndex; i++) {
						TableColumn column = columnModel.getColumn(i);
						int columnWidth = (int)(0.5f + (locations.get(i + 1) - locations.get(i)) * tableWidth);
						column.setWidth(columnWidth);
						reminder -= columnWidth;
					}

					columnModel.getColumn(lastColumnIndex).setWidth(reminder);
				}
			}
		}

		@Override
		public void createDefaultColumnsFromModel() {
			TableModel model = getModel();
			if (model == NO_GRADIENT_TABLE_MODEL) {
				super.createDefaultColumnsFromModel();
			} else if (model != null) {
	            // Remove any current columns
	            TableColumnModel columnModel = getColumnModel();
	            while (columnModel.getColumnCount() > 0) {
	            	columnModel.removeColumn(columnModel.getColumn(0));
	            }

				int columnCount = locations.size() - 1;
	            float minWidth = Float.POSITIVE_INFINITY;
				for (int i = 1; i < columnCount; i++) {
					float width = locations.get(i) - locations.get(i - 1);
					if (width < minWidth)
						minWidth = width;
				}

				int minTotalWidth = (int) Math.ceil(15 / minWidth);
				int columnWidth = minTotalWidth / columnCount;
				int remainder = minTotalWidth % columnCount;

	            // Create new columns from the data model info
                TableColumn column = new TableColumn(0, columnWidth + remainder);
                addColumn(column);
	            for (int i = 1; i < columnCount; i++) {
	                column = new TableColumn(i, columnWidth);
	                addColumn(column);
	            }
	        }
		}
	}

	private class SubgradientsTableModel extends AbstractTableModel {
		
		private ListModel<Color> colorListModel;
		
		public SubgradientsTableModel(ListModel<Color> colorListModel) {
			this.colorListModel = colorListModel;
		}

		public int getColumnCount() {
			return colorListModel.getSize() - 1;
		}

		public int getRowCount() {
			return 1;
		}

		public String getColumnName(int column) {
			return locations.get(column).toString();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			return new Color[] {
					colorListModel.getElementAt(columnIndex),
					colorListModel.getElementAt(columnIndex + 1) };
		}

		public Class<?> getColumnClass(int columnIndex) {
			return Color[].class;
		}
	}
	
	private static class NoGradientTableModel extends AbstractTableModel {
		private Object value = new Object();
		public int getColumnCount() {
			return 1;
		}
		public int getRowCount() {
			return 1;
		}
		public Object getValueAt(int rowIndex, int columnIndex) {
			return value;
		}
		public String getColumnName(int column) {
			return "Not available"; // NO STRINGA VUOTA!
		}
	}

	private static class NoGradientRenderer extends JLabel implements TableCellRenderer {
		public NoGradientRenderer() {
			super.setOpaque(false);
			super.setBorder(BorderFactory.createEmptyBorder());
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			return this;
		}

		public void setBorder(Border border) {}
		public void invalidate() {}
		public void validate() {}
		public void revalidate() {}
		public void repaint(long tm, int x, int y, int width, int height) {}
		public void repaint(Rectangle r) { }
		public void repaint() {}
	}

	private static class SubgradientRenderer extends JPanel implements TableCellRenderer {

		private Color color0;
		private Color color1;

		public SubgradientRenderer() {
			super.setBorder(BorderFactory.createEmptyBorder());
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Color[] subgradient = (Color[]) value;
			color0 = subgradient[0];
			color1 = subgradient[1];
			return this;
		}

		public boolean isOpaque() {
			return true;
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2D = (Graphics2D) g;
			GradientPaint newPaint = new GradientPaint(0, 0, color0, getWidth() + 1, 0 , color1);
			Paint oldPaint = g2D.getPaint();
			g2D.setPaint(newPaint);
			g2D.fillRect(0, 0, getWidth(), getHeight());
			g2D.setPaint(oldPaint);
		}

		public void setBorder(Border border) {}
		public void invalidate() {}
		public void validate() {}
		public void revalidate() {}
		public void repaint(long tm, int x, int y, int width, int height) {}
		public void repaint(Rectangle r) { }
		public void repaint() {}
	}
}

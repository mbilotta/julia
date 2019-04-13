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
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class CollapsiblePanel extends JPanel {
	
	public static final String EXPAND_SYMBOL = "<html>+ ";
	public static final String COLLAPSE_SYMBOL = "<html>\u2212 ";

	private JComponent expandedView;
	private JComponent collapsedView;
	private Border expandedBorder;
	private Border collapsedBorder;
	private boolean collapsed;

	private String title;
	private Color borderColor;

	private ChangeEvent changeEvent;

	public CollapsiblePanel(JComponent child, String title, Color borderColor, boolean collapsed) {
		super(new BorderLayout());

		JLabel dotsLabel = new JLabel("...");
		dotsLabel.setHorizontalAlignment(JLabel.CENTER);
		add(child, BorderLayout.CENTER);
		add(dotsLabel, BorderLayout.SOUTH);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getPoint().y < getInsets().top) {
					setCollapsedImpl(!isCollapsed());
				}
			}
		});

		if (title == null)
			title = "";

		if (borderColor == null)
			borderColor = getBackground().darker();

		this.expandedView = child;
		this.collapsedView = dotsLabel;
		this.expandedBorder = createExpandedBorder(title, borderColor);
		this.collapsedBorder = createCollapsedBorder(title, borderColor);
		
		this.title = title;
		this.borderColor = borderColor;

		setCollapsedImpl(collapsed);
	}

	public CollapsiblePanel(JComponent child, String title, boolean collapsed) {
		this(child, title, null, collapsed);
	}

	public void setCollapsed(boolean collapsed) {
		if (collapsed != this.collapsed)
			setCollapsedImpl(collapsed);
	}

	public boolean isCollapsed() {
		return collapsed;
	}

	public void setTitle(String title) {
		setBorder(title, borderColor);
	}

	public String getTitle() {
		return title;
	}

	public void setBorderColor(Color borderColor) {
		setBorder(title, borderColor);
	}

	public Color getBorderColor() {
		return borderColor;
	}

	public void setBorder(String title, Color color) {
		if (title == null)
			title = "";

		if (borderColor == null)
			borderColor = getBackground().darker();

		if (!title.equals(getTitle()) || !color.equals(getBorderColor())) {
			expandedBorder = createExpandedBorder(title, color);
			collapsedBorder = createCollapsedBorder(title, color);
			setBorder(collapsed ? collapsedBorder : expandedBorder);
			this.title = title;
			this.borderColor = color;
		}
	}

	public JComponent getChild() {
		return expandedView;
	}

	public int getMinimumCollapsedHeight() {
		Insets insets = collapsedBorder.getBorderInsets(this);
		return collapsedView.getMinimumSize().height + insets.top + insets.bottom;
	}

	public int getCollapsedHeight() {
		Insets insets = collapsedBorder.getBorderInsets(this);
		return  collapsedView.getHeight() + insets.top + insets.bottom;
	}

	public int getPreferredCollapsedHeight() {
		Insets insets = collapsedBorder.getBorderInsets(this);
		Dimension preferredSize = collapsedView.getPreferredSize();
		return preferredSize.height + insets.top + insets.bottom;
	}

	public Dimension getPreferredCollapsedSize() {
		Insets insets = collapsedBorder.getBorderInsets(this);
		Dimension preferredSize = expandedView.getPreferredSize();
		return new Dimension(preferredSize.width + insets.left + insets.right, getPreferredCollapsedHeight());
	}

	public int getMinimumExpandedHeight() {
		Insets insets = expandedBorder.getBorderInsets(this);
		return expandedView.getMinimumSize().height + insets.top + insets.bottom;
	}

	public int getExpandedHeight() {
		Insets insets = expandedBorder.getBorderInsets(this);
		return expandedView.getHeight() + insets.top + insets.bottom;
	}

	public int getPreferredExpandedHeight() {
		Insets insets = expandedBorder.getBorderInsets(this);
		Dimension preferredSize = expandedView.getPreferredSize();
		return preferredSize.height + insets.top + insets.bottom;
	}

	public Dimension getPreferredExpandedSize() {
		Insets insets = expandedBorder.getBorderInsets(this);
		Dimension preferredSize = expandedView.getPreferredSize();
		return new Dimension(preferredSize.width + insets.left + insets.right, getPreferredExpandedHeight());
	}

	public Dimension getPreferredSize() {
		if (collapsed) {
			return getPreferredCollapsedSize();
		}

		return getPreferredExpandedSize();
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

    public static class WindowResizer implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			CollapsiblePanel source = (CollapsiblePanel) e.getSource();
			Window enclosingWindow = SwingUtilities.windowForComponent(source);
			if (enclosingWindow != null) {
				if (source.isCollapsed()) {
					int heightDelta = source.getPreferredCollapsedHeight() - source.getHeight();
					enclosingWindow.setMinimumSize(enclosingWindow.getPreferredSize());
					enclosingWindow.setSize(enclosingWindow.getWidth(), enclosingWindow.getHeight() + heightDelta);
				} else {
					int heightDelta = source.getExpandedHeight() - source.getHeight();
					if (heightDelta > 0) {
						enclosingWindow.setSize(enclosingWindow.getWidth(), enclosingWindow.getHeight() + heightDelta);
						enclosingWindow.setMinimumSize(enclosingWindow.getPreferredSize());
					}
				}
			}
		}
	}

	protected void fireStateChanged() {
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

	public static Border createCollapsedBorder(String title, Color color) {
		return BorderFactory.createTitledBorder(
				BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color, 1, true), BorderFactory.createEmptyBorder(0, 5, 0, 5)),
				EXPAND_SYMBOL + title,
				TitledBorder.LEADING,
				TitledBorder.TOP);
	}

	public static Border createExpandedBorder(String title, Color color) {
		return BorderFactory.createTitledBorder(
				BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color, 1, true), BorderFactory.createEmptyBorder(0, 5, 5, 5)),
				COLLAPSE_SYMBOL + title,
				TitledBorder.LEADING,
				TitledBorder.TOP);
	}

	private void setCollapsedImpl(boolean collapsed) {
		setBorder(collapsed ? collapsedBorder : expandedBorder);
		collapsedView.setVisible(collapsed);
		expandedView.setVisible(!collapsed);
		this.collapsed = collapsed;
		
		fireStateChanged();
	}
}

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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;


public class MessagePane extends JOptionPane {

	public static final int MESSAGE_WIDTH = 500;
	public static final int DETAILS_HEIGHT = 100;
	
	public MessagePane(String message, Object details, int messageType) {
		setMessage(createMessage(message, details));
		setMessageType(messageType);
		setOptionType(DEFAULT_OPTION);
	}

	public static void showErrorMessage(Component parentComponent, String title, String message, Object details) {
		showMessage(parentComponent, title, message, details, ERROR_MESSAGE);
	}

	public static void showWarningMessage(Component parentComponent, String title, String message, Object details) {
		showMessage(parentComponent, title, message, details, WARNING_MESSAGE);
	}

	public static void showInformationMessage(Component parentComponent, String title, String message, Object details) {
		showMessage(parentComponent, title, message, details, INFORMATION_MESSAGE);
	}

	public static void showReadErrorMessage(Component parentComponent, Path file, Exception e) {
		showErrorMessage(parentComponent, "Julia", "I/O error trying to read from file <b>" + file + "</b>.", e);
	}

	public static void showWriteErrorMessage(Component parentComponent, Path file, Exception e) {
		showErrorMessage(parentComponent, "Julia", "I/O error trying to write to file <b>" + file + "</b>.", e);
	}

	public static void showReadErrorMessage(Component parentComponent, File file, Exception e) {
		showErrorMessage(parentComponent, "Julia", "I/O error trying to read from file <b>" + file + "</b>.", e);
	}

	public static void showWriteErrorMessage(Component parentComponent, File file, Exception e) {
		showErrorMessage(parentComponent, "Julia", "I/O error trying to write to file <b>" + file + "</b>.", e);
	}

	public static void showMessage(Component parentComponent, String title, String message, Object details, int messageType) {
		JDialog dialog = new MessagePane(message, details, messageType).createDialog(parentComponent, title);
		if (details != null) {
			dialog.setResizable(true);
			dialog.setMinimumSize(dialog.getPreferredSize());
		}
		dialog.setVisible(true);
		dialog.dispose();
	}

	public static Object createMessage(String message, Object details) {
		JLabel messageLabel = new JLabel(BasicHTML.isHTMLString(message) ? message : "<html>".concat(message));
		messageLabel.setVerticalAlignment(JLabel.TOP);
		final View view = (View) messageLabel.getClientProperty(BasicHTML.propertyKey);
		float prefSpanX = view.getPreferredSpan(View.X_AXIS);
		float prefSpanY = view.getPreferredSpan(View.Y_AXIS);
		view.setSize(Math.min(MESSAGE_WIDTH, prefSpanX), prefSpanY);

		Object rv = messageLabel;
		if (details != null) {
			messageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
			JComponent container = new JPanel(new BorderLayout());
			String detailsString;
			if (details instanceof Throwable) {
				Throwable throwable = (Throwable) details;
				StringWriter buffer = new StringWriter();
				throwable.printStackTrace(new PrintWriter(buffer));
				detailsString = buffer.toString();
			} else {
				detailsString = details.toString();
			}
			JTextArea detailsArea = new JTextArea(detailsString);
			detailsArea.setEditable(false);
			JScrollPane detailsScroller = new JScrollPane(detailsArea);
			detailsScroller.setPreferredSize(new Dimension(0, DETAILS_HEIGHT));
			detailsScroller.setSize(detailsScroller.getPreferredSize());
			CollapsiblePanel collapsiblePanel = new CollapsiblePanel(detailsScroller, "Details", true);
			WindowResizer windowResizer = new WindowResizer();
			collapsiblePanel.addChangeListener(windowResizer);
			collapsiblePanel.addHierarchyListener(windowResizer);

			container.add(messageLabel, BorderLayout.NORTH);
			container.add(collapsiblePanel, BorderLayout.CENTER);

			rv = container;
		}

		messageLabel.setPreferredSize(messageLabel.getPreferredSize());
		view.setSize(prefSpanX, prefSpanY);
		return rv;
	}

	public static final class WindowResizer implements ChangeListener, HierarchyListener {
		private Dimension collapsedMinSize;
		private Dimension expandedMinSize;
		@Override
		public void stateChanged(ChangeEvent e) {
			CollapsiblePanel source = (CollapsiblePanel) e.getSource();
			JDialog dialog = (JDialog) SwingUtilities.windowForComponent(source);
			if (dialog != null) {
				Container contentPane = dialog.getContentPane();
				if (source.isCollapsed()) {
					int heightDelta = source.getMinimumCollapsedHeight() - source.getHeight();
					dialog.setMinimumSize(collapsedMinSize);
					contentPane.setPreferredSize(new Dimension(
							contentPane.getWidth(),
							contentPane.getHeight() + heightDelta));
					dialog.pack();
				} else {
					int heightDelta = source.getExpandedHeight() - source.getHeight();
					dialog.setMinimumSize(expandedMinSize);
					if (heightDelta < 0) {
						heightDelta = 0;
					}
					contentPane.setPreferredSize(new Dimension(
							contentPane.getWidth(),
							contentPane.getHeight() + heightDelta));
					dialog.pack();
				}
			}
		}

		@Override
		public void hierarchyChanged(HierarchyEvent e) {
			if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
				CollapsiblePanel source = (CollapsiblePanel) e.getSource();
				Window window = SwingUtilities.windowForComponent(source);
				collapsedMinSize = window.getSize();
				expandedMinSize = new Dimension(collapsedMinSize);
				expandedMinSize.height = expandedMinSize.height
						- source.getMinimumCollapsedHeight()
						+ source.getMinimumExpandedHeight();
				source.removeHierarchyListener(this);
			}
		}
	}

    public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				swingMain(args);
			}
		});
	}

	private static void swingMain(String[] args) {
		String msg = "<html><strong>Pellentesque habitant morbi tristique</strong> senectus et netus " +
				"et malesuada fames ac turpis egestas. Vestibulum tortor quam, feugiat vitae, ultricies " +
				"eget, tempor sit amet, ante. Donec eu libero sit amet quam egestas semper. <em>Aenean " +
				"ultricies mi vitae est.</em> Mauris placerat eleifend leo. Quisque sit amet est et sapien " +
				"ullamcorper pharetra. Vestibulum erat wisi, condimentum sed, <code>commodo vitae</code>, " +
				"ornare sit amet, wisi. Aenean fermentum, elit eget tincidunt condimentum, eros ipsum rutrum " +
				"orci, sagittis tempus lacus enim ac dui. <a href=\"#\">Donec non enim</a> in turpis pulvinar " +
				"facilisis. Ut felis. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus magna. " +
				"Cras in mi at felis aliquet congue. Ut a est eget ligula molestie gravida. Curabitur massa. " +
				"Donec eleifend, libero at sagittis mollis, tellus est malesuada tellus, at luctus turpis elit sit " +
				"amet quam. Vivamus pretium ornare est.";
		showErrorMessage(null, "Prova", msg, msg);
	}
}

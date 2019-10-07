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
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.border.BevelBorder;

import org.altervista.mbilotta.julia.Consumer;
import org.altervista.mbilotta.julia.program.Application;
import org.altervista.mbilotta.julia.program.Preferences;


public class MainWindow extends JFrame {

	private final Application application;
	private ImagePanel imagePanel;
	private JLabel messageLabel;
	private JProgressBar progressBar;
	private JMenu windowMenu;

	private JLabel timerLabel;
	private long timerValue = 0l;
	private long timerStart;
	private boolean timerRunning = false;

	private int[] percentages;

	public MainWindow(Application application) {
		super("Julia: The Fractal Generator");
		this.application = application;
		Preferences preferences = application.getPreferences();
		imagePanel = new ImagePanel(
				preferences.getImageWidth(),
				preferences.getImageHeight(),
				preferences.getSelectionColor(),
				application);
		messageLabel = new MessageLabel("Left-click on the image to select an area you want to zoom in.");
		timerLabel = new JLabel();
		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);

		build();
	}

	private JComponent buildStatusBar() {
		JComponent statusBar = new Box(BoxLayout.X_AXIS);
		timerLabel.setBorder(messageLabel.getBorder());
		timerLabel.setIcon(application.getIcon("clock"));
		timerLabel.setText("00:00:00");
		progressBar.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createBevelBorder(BevelBorder.LOWERED),
				progressBar.getBorder()));

		Dimension messageLabelSize = messageLabel.getPreferredSize();
		Dimension timerLabelSize = timerLabel.getPreferredSize();
		Dimension progressBarSize = progressBar.getPreferredSize();
		int heightMax = Math.max(Math.max(messageLabelSize.height, timerLabelSize.height), progressBarSize.height);
		messageLabelSize.width = Short.MAX_VALUE;
		messageLabelSize.height = heightMax;
		progressBarSize.width = 200;
		progressBarSize.height = heightMax;
		timerLabelSize.height = heightMax;
		messageLabel.setMinimumSize(new Dimension(0, heightMax));
		messageLabel.setPreferredSize(new Dimension(0, heightMax));
		messageLabel.setMaximumSize(messageLabelSize);
		timerLabel.setMaximumSize(timerLabelSize);
		progressBar.setMaximumSize(progressBarSize);
		
		statusBar.add(messageLabel);
		statusBar.add(timerLabel);
		statusBar.add(progressBar);
		return statusBar;
	}

	private JToolBar buildToolBar() {
		JToolBar toolBar = new JToolBar();
		toolBar.setLayout(new WrapLayout(WrapLayout.LEADING, 0, 0));
		toolBar.setFloatable(false);

		add(toolBar, application.getUndoAction());
		add(toolBar, application.getRedoAction());
		toolBar.addSeparator();
		add(toolBar, application.getZoomAction());
		add(toolBar, application.getHaltAction());
		add(toolBar, application.getResumeAction());
		toolBar.addSeparator();
		JToggleButton toggleButton = new JToggleButton(application.getRefreshPeriodicallyAction());
		add(toolBar, toggleButton);
		add(toolBar, application.getRefreshAction());
		toolBar.addSeparator();
		add(toolBar, application.getEditSelectionColorAction());
		add(toolBar, application.getEditPreferencesAction());

		return toolBar;
	}

	private static JMenuItem add(JMenu menu, Action action) {
		JMenuItem rv = menu.add(action);
		rv.setToolTipText(null);
		return rv;
	}

	private static JMenuItem add(JMenu menu, JMenuItem menuItem) {
		JMenuItem rv = menu.add(menuItem);
		rv.setToolTipText(null);
		return rv;
	}

	private static JButton add(JToolBar toolBar, Action action) {
		JButton rv = toolBar.add(action);
		rv.setMnemonic(0);
		return rv;
	}

	private static JButton add(JToolBar toolBar, JButton button) {
		JButton rv = (JButton) toolBar.add(button);
		rv.setMnemonic(0);
		return rv;
	}

	private static JToggleButton add(JToolBar toolBar, JToggleButton button) {
		JToggleButton rv = (JToggleButton) toolBar.add(button);
		rv.setMnemonic(0);
		rv.setText("");
		return rv;
	}

	private JMenuBar buildMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.setLayout(new WrapLayout(WrapLayout.LEADING, 0, 0));
		
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		add(fileMenu, application.getLoadAction());
		add(fileMenu, application.getSaveAction());
		add(fileMenu, application.getExportAction());
		fileMenu.addSeparator();
		add(fileMenu, application.getInstallNewPluginsAction());
		fileMenu.addSeparator();
		add(fileMenu, application.getQuitAction());
		menuBar.add(fileMenu);
		
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);
		add(editMenu, application.getUndoAction());
		add(editMenu, application.getRedoAction());
		editMenu.addSeparator();
		add(editMenu, application.getEditSelectionColorAction());
		add(editMenu, application.getEditPreferencesAction());
		menuBar.add(editMenu);

		JMenu toolsMenu = new JMenu("Tools");
		toolsMenu.setMnemonic(KeyEvent.VK_T);
		add(toolsMenu, application.getZoomAction());
		add(toolsMenu, application.getHaltAction());
		add(toolsMenu, application.getResumeAction());
		add(toolsMenu, application.getShowLogsAction());
		toolsMenu.addSeparator();
		add(toolsMenu, application.getRefreshAction());
		add(toolsMenu, new JCheckBoxMenuItem(application.getRefreshPeriodicallyAction()));
		menuBar.add(toolsMenu);

		windowMenu = new JMenu("Window");
		windowMenu.setMnemonic(KeyEvent.VK_W);
		windowMenu.addSeparator();
		add(windowMenu, application.getHideAllAction());
		add(windowMenu, application.getShowAllAction());
		menuBar.add(windowMenu);

		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic(KeyEvent.VK_H);
		add(helpMenu, application.getBrowseJuliaHomePageAction());
		add(helpMenu, application.getShowInfosAction());
		menuBar.add(helpMenu);

		return menuBar;
	}

	private void build() {
		Container contentPane = getContentPane();
		JScrollPane scrollPane = new JScrollPane(imagePanel);
		contentPane.add(scrollPane, BorderLayout.CENTER);
		contentPane.add(buildStatusBar(), BorderLayout.SOUTH);
		contentPane.add(buildToolBar(), BorderLayout.NORTH);
		setJMenuBar(buildMenuBar());
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		application.installMoveWindowFocusKeyBindings(this);
	}

	public boolean hasSelection() {
		return imagePanel.hasSelection();
	}

	public ImageSelection getSelection() {
		return imagePanel.getSelection();
	}

	public void clearSelection() {
		imagePanel.clearSelection();
	}

	public void setTransparency(int transparency) {
		imagePanel.setTransparency(transparency);
	}

	public void startTimer() {
		timerStart = System.currentTimeMillis();
		timerRunning = true;
	}

	public void stopTimer() {
		timerValue += System.currentTimeMillis() - timerStart;
		showElapsedTime(timerValue);
		timerRunning = false;
	}

	public void resetTimer() {
		timerValue = 0;
		timerLabel.setText("00:00:00.000");
	}

	private void showElapsedTime(long t) {
		timerLabel.setText(String.format("%02d:%02d:%02d.%03d", t/3_600_000, (t%3_600_000)/60_000, (t/1_000)%60, t%1_000));
	}

	public void refresh() {
		imagePanel.refresh(percentages);
		if (timerRunning) {
			showElapsedTime(timerValue + System.currentTimeMillis() - timerStart);
		}
		refreshProgressBar();
	}

	public void refresh(Consumer consumer) {
		imagePanel.refresh(consumer);
	}

	public void consumeAndReset(int width, int height, int transparency, Consumer consumer) {
		int[] percentages = new int[consumer.getNumOfProducers()];
		imagePanel.consumeAndReset(width, height, transparency, consumer, percentages);
		this.percentages = percentages;
		refreshProgressBar();
	}

	public void reset(int width, int height, int transparency, Consumer consumer) {
		imagePanel.reset(width, height, transparency, consumer);
		progressBar.setValue(0);
		progressBar.setString("0%");
		percentages = new int[consumer.getNumOfProducers()];
	}

	public void setSelectionColor(Color color) {
		imagePanel.setSelectionColor(color);
	}

	public Color getSelectionColor() {
		return imagePanel.getSelectionColor();
	}

	public void setSelectionColorPreview(Color color) {
		imagePanel.setSelectionColorPreview(color);
	}

	public Color getSelectionColorPreview() {
		return imagePanel.getSelectionColorPreview();
	}

	public Consumer getConsumer() {
		return imagePanel.getConsumer();
	}

	public BufferedImage getFinalImage() {
		return imagePanel.getFinalImage();
	}

	public Dimension getImagePanelSize() {
		JViewport viewport = (JViewport) imagePanel.getParent();
		JScrollPane scrollPane = (JScrollPane) viewport.getParent();
		Dimension rv = viewport.getSize();
		JScrollBar vsb = scrollPane.getVerticalScrollBar();
		if (vsb != null && vsb.isVisible()) {
			rv.width += vsb.getWidth();
		}
		JScrollBar hsb = scrollPane.getHorizontalScrollBar();
		if (hsb != null && hsb.isVisible()) {
			rv.height += hsb.getHeight();
		}
		return rv;
	}

	public Dimension getImageSize() {
		return imagePanel.getPreferredSize();
	}

	public void addToWindowMenu(JMenuItem menuItem) {
		windowMenu.add(menuItem, windowMenu.getMenuComponentCount() - 3);
	}

	public void removeFromWindowMenu(JMenuItem menuItem) {
		windowMenu.remove(menuItem);
	}

	private boolean highlighting = false;
	private Color labelForeground = null;

	public void setStatusMessage(String message, boolean highlight) {
		if (message == null) {
			if (imagePanel.hasSelection()) {
				message = "Move the mouse wheel to resize the selection, rigth-click to cancel it.";
			} else {
				message = "Left-click on the image to select an area you want to zoom in.";
			}
		}

		if (highlight != highlighting) {
			if (highlight) {
				if (messageLabel.isForegroundSet()) {
					labelForeground = messageLabel.getForeground();
				}
				messageLabel.setForeground(Color.RED);
			} else {
				messageLabel.setForeground(labelForeground);
			}
			highlighting = highlight;
		}
		messageLabel.setText(message);
	}

	private void refreshProgressBar() {
		if (percentages != null) {
			int sum = 0;
			for (int percentage : percentages) {
				sum += percentage;
			}
			int value = sum / percentages.length;
			progressBar.setValue(value);
			progressBar.setString(value + "%");
		}
	}
}

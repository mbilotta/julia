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

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;


public class SplashScreen extends JFrame {

	private JLabel textLabel;
	private JProgressBar progressBar;

	public SplashScreen() {
		textLabel = new JLabel("Initializing...");
		textLabel.setForeground(Color.WHITE);
		textLabel.setHorizontalAlignment(JLabel.CENTER);
		textLabel.setVerticalAlignment(JLabel.BOTTOM);
		progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
		progressBar.setStringPainted(true);

		JLabel contentPane = new JLabel(new ImageIcon(getClass().getResource("splash.png")));
		contentPane.setLayout(new BorderLayout());
		contentPane.add(textLabel, BorderLayout.CENTER);
		contentPane.add(progressBar, BorderLayout.SOUTH);
		contentPane.setOpaque(true);

		setContentPane(contentPane);
		setTitle("Julia");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setUndecorated(true);
		pack();
		setLocationRelativeTo(null);
	}

	public void setProgress(int progress) {
		progressBar.setValue(progress);
	}

	public int getProgress() {
		return progressBar.getValue();
	}

	public void setText(String text) {
		textLabel.setText(text);
	}

	public String getText() {
		return textLabel.getText();
	}

	public void setIndeterminate(boolean indeterminate) {
		progressBar.setIndeterminate(indeterminate);
	}

	public boolean isIndeterminate() {
		return progressBar.isIndeterminate();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SplashScreen splashScreen = new SplashScreen();
				splashScreen.setIndeterminate(true);
				splashScreen.setVisible(true);
			}
		});
	}
}

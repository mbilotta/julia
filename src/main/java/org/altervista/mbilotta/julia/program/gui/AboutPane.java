package org.altervista.mbilotta.julia.program.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.altervista.mbilotta.julia.program.Application;


public class AboutPane extends JTabbedPane {
	private JButton btnLink0;
	private JButton btnLink1;
	private JButton btnLink2;

	/**
	 * Create the panel.
	 */
	public AboutPane() {
		
		JPanel licenseTab = new JPanel();
		licenseTab.setBorder(new EmptyBorder(10, 10, 10, 10));
		licenseTab.setLayout(new BorderLayout(0, 0));
		addTab("License", null, licenseTab, null);

		URL url = getClass().getResource("lgplv3-147x51.png");
		JLabel lblPreamble = new JLabel("<html><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">"
				+ "<tr><td align=\"left\" valign=\"top\">"
				+ "Julia: The Fractal Generator<br>"
				+ "version 1.0.8<br><br>"
				+ "Copyright &copy; 2015 Maurizio Bilotta.</td>"
				+ "<td align=\"right\" valign=\"top\"><img src=\"" + url + "\"></td></tr></table><br>"
				+ "Julia is free software: you can redistribute it and/or modify it under the terms of the"
				+ " GNU Lesser General Public License as published by the Free Software Foundation, either"
				+ " version 3 of the License, or (at your option) any later version.<br><br>"
				+ "Julia is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without"
				+ " even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See"
				+ " full license below for more details.<br><br>");
		licenseTab.add(lblPreamble, BorderLayout.NORTH);

		JTabbedPane tbdpFullTerms = new JTabbedPane();
		licenseTab.add(tbdpFullTerms, BorderLayout.CENTER);

		JTextArea txtrGpl = new JTextArea();
		txtrGpl.setEditable(false);
		txtrGpl.setFont(new Font("monospaced", Font.PLAIN, 12));
		txtrGpl.setText(readFully("COPYING"));
		txtrGpl.setCaretPosition(0);
		JScrollPane scpnGpl = new JScrollPane(txtrGpl);
		
		JTextArea txtrLgpl = new JTextArea();
		txtrLgpl.setEditable(false);
		txtrLgpl.setFont(new Font("monospaced", Font.PLAIN, 12));
		txtrLgpl.setText(readFully("COPYING.LESSER"));
		txtrLgpl.setCaretPosition(0);
		JScrollPane scpnLgpl = new JScrollPane(txtrLgpl);

		tbdpFullTerms.addTab("GNU GPLv3", scpnGpl);
		tbdpFullTerms.addTab("GNU LGPLv3", scpnLgpl);
		
		JPanel creditsTab = new JPanel();
		creditsTab.setBorder(new EmptyBorder(10, 10, 10, 10));
		addTab("Credits", null, creditsTab, null);
		creditsTab.setLayout(new BorderLayout(0, 0));
		
		JPanel pnlLinks = new JPanel();
		creditsTab.add(pnlLinks, BorderLayout.SOUTH);
		pnlLinks.setLayout(new GridLayout(3, 1, 0, 5));
		
		btnLink0 = new JButton("<html><em>Momentum Design Lab</em> home page");
		btnLink0.setActionCommand("http://momentumdesignlab.com/");
		pnlLinks.add(btnLink0);
		
		btnLink1 = new JButton("<html><em>Momenticons</em> set page on iconfinder.com");
		btnLink1.setActionCommand("https://www.iconfinder.com/iconsets/momenticons-basic");
		pnlLinks.add(btnLink1);
		
		btnLink2 = new JButton("CC BY 3.0 license page");
		btnLink2.setActionCommand("http://creativecommons.org/licenses/by/3.0/legalcode");
		pnlLinks.add(btnLink2);
		
		JLabel lblIconAttrib = new JLabel("<html>Icons are taken/derived from the <em>Momenticons</em> "
				+ "matte set by <em>Momentum Design Lab</em>, licensed under "
				+ "Creative Commons Attribution 3.0 unported (CC BY 3.0).");
		lblIconAttrib.setVerticalAlignment(SwingConstants.TOP);
		creditsTab.add(lblIconAttrib, BorderLayout.CENTER);
		
		setPreferredSize(new Dimension(595, 380));
	}

	public void init(final Application application) {
		ActionListener buttonListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				application.visitPage(URI.create(e.getActionCommand()), AboutPane.this);
			}
		};
		
		btnLink0.addActionListener(buttonListener);
		btnLink1.addActionListener(buttonListener);
		btnLink2.addActionListener(buttonListener);
	}

	private static String readFully(String resource) {
		InputStream source = AboutPane.class.getResourceAsStream(resource);
		Scanner scanner = new Scanner(source, "UTF-8");
		scanner.useDelimiter("\\A");
		String fullText = scanner.next();
		scanner.close();
		return fullText;
	}

	public static void showDialog(Component parentComponent, String title, Application application) {
		AboutPane aboutPane = new AboutPane();
		if (application != null) {
			aboutPane.init(application);
		}
		JOptionPane.showMessageDialog(parentComponent, aboutPane, title, JOptionPane.PLAIN_MESSAGE);
	}
}

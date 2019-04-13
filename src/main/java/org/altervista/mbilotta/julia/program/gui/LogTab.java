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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.altervista.mbilotta.julia.LimitedStringPrinter;
import org.altervista.mbilotta.julia.Printer;
import org.altervista.mbilotta.julia.Production;


public class LogTab extends JPanel {
	
	private LimitedStringPrinter printer;
	private JTextPane textPane;
	private JButton btnRefresh;
	private int maxLength;

	/**
	 * Create the panel.
	 */
	public LogTab() {
		setBorder(new EmptyBorder(5, 5, 5, 5));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane);
		
		textPane = new JTextPane();
		JPanel textPaneWrapper = new JPanel(new BorderLayout());
		textPaneWrapper.add(textPane, BorderLayout.CENTER);

		scrollPane.setViewportView(textPaneWrapper);
		scrollPane.setPreferredSize(new Dimension(400, 300));
		
		Component rigidArea = Box.createRigidArea(new Dimension(0, 5));
		add(rigidArea);
		
		JPanel buttonWrapper = new JPanel();
		add(buttonWrapper);
		buttonWrapper.setLayout(new BoxLayout(buttonWrapper, BoxLayout.X_AXIS));
		
		Component horizontalGlue = Box.createHorizontalGlue();
		buttonWrapper.add(horizontalGlue);
		
		btnRefresh = new JButton("Refresh");
		btnRefresh.setMnemonic(KeyEvent.VK_R);
		buttonWrapper.add(btnRefresh);
		
		btnRefresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});

		textPane.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				textPane.setEditable(true);
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				textPane.setEditable(false);
			}
		});
	}

	public void refresh() {
		if (printer != null) {
			append(printer.empty());
		}
	}

	public boolean hasOutput() {
		return printer != null;
	}

	public void init(int maxLength, Production.Producer producer) {
		textPane.setStyledDocument(new DefaultStyledDocument());
		if (producer != null) {
			if (producer.getPrinter() instanceof LimitedStringPrinter) {
				printer = (LimitedStringPrinter) producer.getPrinter();
				this.maxLength = printer.getMaxLength();
			} else {
				printer = null;
				this.maxLength = maxLength;
				appendLine("No output set.", Color.RED);
			}
		} else {
			printer = null;
			this.maxLength = maxLength;
			append("Terminated.", Color.RED);
		}
	}

	public void append(String text) {
		append(text, (AttributeSet) null);
	}

	public void appendLine(String text) {
		append(text + System.lineSeparator());
	}

	public void append(String text, AttributeSet attributeSet) {
		Document document = textPane.getDocument();
		try {
			if (text.length() > maxLength) {
				text = text.substring(text.length() - maxLength, text.length());
			}
			int newLength = document.getLength() + text.length();
			if (newLength - maxLength > 0) { // entro pure se newLength trabocca
				int leftLength = maxLength - document.getLength();
				document.remove(0, text.length() - leftLength);
			}
			document.insertString(document.getLength(), text, attributeSet);
		} catch (BadLocationException e) {
			throw new AssertionError(e);
		}
	}

	public void appendLine(String text, AttributeSet attributeSet) {
		append(text + System.lineSeparator(), attributeSet);
	}

	public void append(String text, Color color) {
		SimpleAttributeSet attributeSet = new SimpleAttributeSet();
		StyleConstants.setForeground(attributeSet, color);
		append(text, attributeSet);
	}

	public void appendLine(String text, Color color) {
		append(text + System.lineSeparator(), color);
	}

	public void scrollToBottom() {
		Rectangle visibleRect = textPane.getVisibleRect();
		visibleRect.y = textPane.getHeight() - visibleRect.height;
		textPane.scrollRectToVisible(visibleRect);
	}

	@SuppressWarnings("resource")
	public void appendStackTrace(Throwable t) {
		Printer printer = new LimitedStringPrinter(maxLength);
		printer.printStackTrace(t);
		append(printer.toString(), Color.RED);
	}
}

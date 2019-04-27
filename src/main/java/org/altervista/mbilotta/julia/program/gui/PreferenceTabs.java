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

import static org.altervista.mbilotta.julia.Utilities.createDialog;
import static org.altervista.mbilotta.julia.Utilities.createOptionPane;
import static org.altervista.mbilotta.julia.Utilities.readFully;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.altervista.mbilotta.julia.program.Application;
import org.altervista.mbilotta.julia.program.DefaultCloseBehaviour;
import org.altervista.mbilotta.julia.program.Preferences;
import org.altervista.mbilotta.julia.program.gui.PluginSelectionPane.Entry;
import org.altervista.mbilotta.julia.program.parsers.FormulaPlugin;
import org.altervista.mbilotta.julia.program.parsers.NumberFactoryPlugin;
import org.altervista.mbilotta.julia.program.parsers.RepresentationPlugin;


public class PreferenceTabs extends JTabbedPane {
	private JSpinner spnWidth;
	private JSpinner spnHeight;
	private JSpinner spnNumOfProducerThreads;
	private JSpinner spnRefreshDelay;
	private JRadioButton rdbtnComputeThisCombinationWDefVal;
	private JRadioButton rdbtnLoadProductionFromFile;
	private JCheckBox chckbxJuliaSet;
	private JComboBox<Entry<NumberFactoryPlugin>> cmbbxNumberFactory;
	private JComboBox<Entry<RepresentationPlugin>> cmbbxRepresentation;
	private JComboBox<Entry<FormulaPlugin>> cmbbxFormula;
	private JTextField tfStartupImagePath;
	private JRadioButton rdbtnHideTheWindow;
	private JRadioButton rdbtnDisposeTheWindow;
	private JRadioButton rdbtnAskWhatTo;
	private JTextField tfBrowserCommand;
	private JButton btnEditSelectionColor;
	private JCheckBox chckbxLoggingEnabled;
	private JLabel lblMaxLogLength;
	private JSpinner spnMaxLogLength;
	private JLabel lblChars;
	private JRadioButton rdbtnUseJavaDesktopInteraction;
	private JRadioButton rdbtnLaunchCommand;
	private JButton btnBrowseCmd;
	private JComboBox<String> cmbbxTransparency;
	private JButton btnMatchCurrImgSize;
	private JButton btnMatchWindowSize;
	private JButton btnMatchCurrImgTransp;
	private JLabel lblNumberFactory;
	private JLabel lblFormula;
	private JLabel lblRepresentation;
	private JButton btnBrowseImg;

	private FileFilter executableFileFilter;
	private Preferences preferences;
	private JCheckBox chckbxForceEqualScales;
	private JLabel lblLoggingEnabled;

	static class MacOSXExecutableFileFilter extends FileFilter {
		@Override
		public boolean accept(File f) {
			if (f != null) {
				if (f.isDirectory()) {
					return true;
				}
				
				try (FileInputStream in = new FileInputStream(f)) {
					byte[] first4 = new byte[4];
					readFully(in, first4);
					if (first4[0] == 0xFE &&
							first4[1] == 0xED &&
							first4[2] == 0xFA &&
							first4[3] == 0xCE) {
						return true;
					}
					return false;
				} catch (EOFException e) {
					return false;
				} catch (IOException e) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		public String getDescription() {
			return "Executable files";
		}
	}
	
	static class UnixExecutableFileFilter extends FileFilter {
		@Override
		public boolean accept(File f) {
			if (f != null) {
				if (f.isDirectory()) {
					return true;
				}
				
				try (FileInputStream in = new FileInputStream(f)) {
					byte[] first4 = new byte[4];
					readFully(in, first4);
					if (first4[0] == 0x7F &&
							first4[1] == 0x45 &&
							first4[2] == 0x4C &&
							first4[3] == 0x46) {
						return true;
					}
					return false;
				} catch (EOFException e) {
					return false;
				} catch (IOException e) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		public String getDescription() {
			return "Executable files";
		}
	}

	static class WindowsExecutableFileFilter extends FileFilter {
		@Override
		public boolean accept(File f) {
			if (f != null) {
				if (f.isDirectory()) {
					return true;
				}
				
				String fileName = f.getName();
				int i = fileName.lastIndexOf('.');
				if (i > 0 && i < fileName.length() - 1) {
					String fileNameExtension = fileName.substring(i + 1)
							.toLowerCase(Locale.ENGLISH);
					if (fileNameExtension.equals("exe")) {
						try (FileInputStream in = new FileInputStream(f)) {
							byte[] first2 = new byte[2];
							readFully(in, first2);
							if (first2[0] == 0x4D && first2[1] == 0x5A) {
								return true;
							}
							return false;
						} catch (EOFException e) {
							return false;
						} catch (IOException e) {
							return true;
						}
					}
					return false;
				}
			}
			return false;
		}
		
		@Override
		public String getDescription() {
			return "Executable files (*.exe)";
		}
	}

	/**
	 * Create the panel.
	 */
	public PreferenceTabs() {

		JPanel generalTab = new JPanel();
		addTab("General", null, generalTab, null);
		setMnemonicAt(0, KeyEvent.VK_G);
		GridBagLayout gbl_generalTab = new GridBagLayout();
		gbl_generalTab.columnWidths = new int[] { 0, 0, 0, 0 };
		gbl_generalTab.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gbl_generalTab.columnWeights = new double[] { 1.0, 1.0, 0.0,
				Double.MIN_VALUE };
		gbl_generalTab.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		generalTab.setLayout(gbl_generalTab);

		Title title = new Title("Image");
		GridBagConstraints gbc_title = new GridBagConstraints();
		gbc_title.weightx = 1.0;
		gbc_title.insets = new Insets(10, 10, 5, 10);
		gbc_title.gridwidth = 3;
		gbc_title.fill = GridBagConstraints.HORIZONTAL;
		gbc_title.gridx = 0;
		gbc_title.gridy = 0;
		generalTab.add(title, gbc_title);

		JLabel lblWidth = new JLabel("Width:");
		lblWidth.setDisplayedMnemonic(KeyEvent.VK_W);
		GridBagConstraints gbc_lblWidth = new GridBagConstraints();
		gbc_lblWidth.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblWidth.insets = new Insets(0, 20, 5, 5);
		gbc_lblWidth.gridx = 0;
		gbc_lblWidth.gridy = 1;
		generalTab.add(lblWidth, gbc_lblWidth);

		spnWidth = new JSpinner();
		lblWidth.setLabelFor(spnWidth);
		spnWidth.setModel(new SpinnerNumberModel(500, 1, 2147483647, 1));
		GridBagConstraints gbc_spnWidth = new GridBagConstraints();
		gbc_spnWidth.weightx = 1.0;
		gbc_spnWidth.anchor = GridBagConstraints.BASELINE_TRAILING;
		gbc_spnWidth.insets = new Insets(0, 0, 5, 5);
		gbc_spnWidth.gridx = 1;
		gbc_spnWidth.gridy = 1;
		generalTab.add(spnWidth, gbc_spnWidth);

		JLabel lblPx = new JLabel("px");
		GridBagConstraints gbc_lblPx = new GridBagConstraints();
		gbc_lblPx.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblPx.insets = new Insets(0, 0, 5, 20);
		gbc_lblPx.gridx = 2;
		gbc_lblPx.gridy = 1;
		generalTab.add(lblPx, gbc_lblPx);

		JLabel lblHeight = new JLabel("Height:");
		lblHeight.setDisplayedMnemonic(KeyEvent.VK_H);
		GridBagConstraints gbc_lblHeight = new GridBagConstraints();
		gbc_lblHeight.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblHeight.insets = new Insets(0, 20, 5, 5);
		gbc_lblHeight.gridx = 0;
		gbc_lblHeight.gridy = 2;
		generalTab.add(lblHeight, gbc_lblHeight);

		spnHeight = new JSpinner();
		lblHeight.setLabelFor(spnHeight);
		spnHeight.setModel(new SpinnerNumberModel(500, 1, 2147483647, 1));
		GridBagConstraints gbc_spnHeight = new GridBagConstraints();
		gbc_spnHeight.anchor = GridBagConstraints.BASELINE_TRAILING;
		gbc_spnHeight.insets = new Insets(0, 0, 5, 5);
		gbc_spnHeight.gridx = 1;
		gbc_spnHeight.gridy = 2;
		generalTab.add(spnHeight, gbc_spnHeight);

		JLabel lblPx_1 = new JLabel("px");
		GridBagConstraints gbc_lblPx_1 = new GridBagConstraints();
		gbc_lblPx_1.insets = new Insets(0, 0, 5, 20);
		gbc_lblPx_1.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblPx_1.gridx = 2;
		gbc_lblPx_1.gridy = 2;
		generalTab.add(lblPx_1, gbc_lblPx_1);
		
		JPanel panel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel.getLayout();
		flowLayout.setVgap(0);
		flowLayout.setAlignment(FlowLayout.LEADING);
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.anchor = GridBagConstraints.LINE_START;
		gbc_panel.gridwidth = 3;
		gbc_panel.insets = new Insets(0, 15, 10, 15);
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 3;
		generalTab.add(panel, gbc_panel);
		
		btnMatchCurrImgSize = new JButton("Match current image");
		btnMatchCurrImgSize.setMnemonic(KeyEvent.VK_M);
		panel.add(btnMatchCurrImgSize);
		
		btnMatchWindowSize = new JButton("Match window");
		btnMatchWindowSize.setMnemonic(KeyEvent.VK_A);
		panel.add(btnMatchWindowSize);
		
		JLabel lblTransparency = new JLabel("Transparency:");
		lblTransparency.setDisplayedMnemonic(KeyEvent.VK_T);
		GridBagConstraints gbc_lblTransparency = new GridBagConstraints();
		gbc_lblTransparency.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblTransparency.insets = new Insets(0, 20, 5, 5);
		gbc_lblTransparency.gridx = 0;
		gbc_lblTransparency.gridy = 4;
		generalTab.add(lblTransparency, gbc_lblTransparency);
		
		cmbbxTransparency = new JComboBox<String>();
		lblTransparency.setLabelFor(cmbbxTransparency);
		cmbbxTransparency.setModel(new DefaultComboBoxModel<String>(new String[] {"Opaque", "Bitmask", "Translucent"}));
		cmbbxTransparency.setSelectedIndex(2);
		GridBagConstraints gbc_cmbbxTransparency = new GridBagConstraints();
		gbc_cmbbxTransparency.anchor = GridBagConstraints.BASELINE_TRAILING;
		gbc_cmbbxTransparency.insets = new Insets(0, 0, 5, 5);
		gbc_cmbbxTransparency.gridx = 1;
		gbc_cmbbxTransparency.gridy = 4;
		generalTab.add(cmbbxTransparency, gbc_cmbbxTransparency);
		
		btnMatchCurrImgTransp = new JButton("Match current image");
		btnMatchCurrImgTransp.setMnemonic(KeyEvent.VK_T);
		GridBagConstraints gbc_btnMatchCurrImgTransp = new GridBagConstraints();
		gbc_btnMatchCurrImgTransp.gridwidth = 3;
		gbc_btnMatchCurrImgTransp.anchor = GridBagConstraints.LINE_START;
		gbc_btnMatchCurrImgTransp.insets = new Insets(0, 20, 5, 20);
		gbc_btnMatchCurrImgTransp.gridx = 0;
		gbc_btnMatchCurrImgTransp.gridy = 5;
		generalTab.add(btnMatchCurrImgTransp, gbc_btnMatchCurrImgTransp);

		Title title_1 = new Title("Performance/CPU usage");
		GridBagConstraints gbc_title_1 = new GridBagConstraints();
		gbc_title_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_title_1.gridwidth = 3;
		gbc_title_1.insets = new Insets(15, 10, 5, 10);
		gbc_title_1.gridx = 0;
		gbc_title_1.gridy = 6;
		generalTab.add(title_1, gbc_title_1);

		JLabel lblNumOfProducer = new JLabel("Num. of producer threads:");
		lblNumOfProducer.setDisplayedMnemonic(KeyEvent.VK_N);
		GridBagConstraints gbc_lblNumOfProducer = new GridBagConstraints();
		gbc_lblNumOfProducer.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblNumOfProducer.insets = new Insets(0, 20, 5, 5);
		gbc_lblNumOfProducer.gridx = 0;
		gbc_lblNumOfProducer.gridy = 7;
		generalTab.add(lblNumOfProducer, gbc_lblNumOfProducer);

		spnNumOfProducerThreads = new JSpinner();
		lblNumOfProducer.setLabelFor(spnNumOfProducerThreads);
		spnNumOfProducerThreads.setModel(new SpinnerNumberModel(2, 1,
				2147483647, 1));
		GridBagConstraints gbc_spnNumOfProducerThreads = new GridBagConstraints();
		gbc_spnNumOfProducerThreads.anchor = GridBagConstraints.BASELINE_TRAILING;
		gbc_spnNumOfProducerThreads.insets = new Insets(0, 0, 5, 5);
		gbc_spnNumOfProducerThreads.gridx = 1;
		gbc_spnNumOfProducerThreads.gridy = 7;
		generalTab.add(spnNumOfProducerThreads, gbc_spnNumOfProducerThreads);

		JLabel lblRefreshDelay = new JLabel("Refresh delay:");
		lblRefreshDelay.setDisplayedMnemonic(KeyEvent.VK_R);
		GridBagConstraints gbc_lblRefreshDelay = new GridBagConstraints();
		gbc_lblRefreshDelay.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblRefreshDelay.insets = new Insets(0, 20, 5, 5);
		gbc_lblRefreshDelay.gridx = 0;
		gbc_lblRefreshDelay.gridy = 8;
		generalTab.add(lblRefreshDelay, gbc_lblRefreshDelay);

		spnRefreshDelay = new JSpinner();
		lblRefreshDelay.setLabelFor(spnRefreshDelay);
		spnRefreshDelay.setModel(new SpinnerNumberModel(500, 1, 2147483647, 1));
		GridBagConstraints gbc_spnRefreshDelay = new GridBagConstraints();
		gbc_spnRefreshDelay.anchor = GridBagConstraints.BASELINE_TRAILING;
		gbc_spnRefreshDelay.insets = new Insets(0, 0, 5, 5);
		gbc_spnRefreshDelay.gridx = 1;
		gbc_spnRefreshDelay.gridy = 8;
		generalTab.add(spnRefreshDelay, gbc_spnRefreshDelay);

		JLabel lblMs = new JLabel("ms");
		GridBagConstraints gbc_lblMs = new GridBagConstraints();
		gbc_lblMs.insets = new Insets(0, 0, 5, 20);
		gbc_lblMs.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblMs.gridx = 2;
		gbc_lblMs.gridy = 8;
		generalTab.add(lblMs, gbc_lblMs);
		
		lblLoggingEnabled = new JLabel("Logging enabled:");
		lblLoggingEnabled.setDisplayedMnemonic(KeyEvent.VK_L);
		GridBagConstraints gbc_lblLoggingEnabled = new GridBagConstraints();
		gbc_lblLoggingEnabled.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblLoggingEnabled.insets = new Insets(0, 20, 5, 5);
		gbc_lblLoggingEnabled.gridx = 0;
		gbc_lblLoggingEnabled.gridy = 9;
		generalTab.add(lblLoggingEnabled, gbc_lblLoggingEnabled);
		
		chckbxLoggingEnabled = new JCheckBox("no");
		chckbxLoggingEnabled.setMnemonic(KeyEvent.VK_L);
		lblLoggingEnabled.setLabelFor(chckbxLoggingEnabled);
		chckbxLoggingEnabled.setHorizontalTextPosition(SwingConstants.LEADING);
		GridBagConstraints gbc_chckbxLoggingEnabled = new GridBagConstraints();
		gbc_chckbxLoggingEnabled.anchor = GridBagConstraints.BASELINE_TRAILING;
		gbc_chckbxLoggingEnabled.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxLoggingEnabled.gridx = 1;
		gbc_chckbxLoggingEnabled.gridy = 9;
		generalTab.add(chckbxLoggingEnabled, gbc_chckbxLoggingEnabled);
		
		lblMaxLogLength = new JLabel("Max. log length:");
		lblMaxLogLength.setDisplayedMnemonic(KeyEvent.VK_X);
		GridBagConstraints gbc_lblMaxLogLength = new GridBagConstraints();
		gbc_lblMaxLogLength.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblMaxLogLength.insets = new Insets(0, 20, 5, 5);
		gbc_lblMaxLogLength.gridx = 0;
		gbc_lblMaxLogLength.gridy = 10;
		generalTab.add(lblMaxLogLength, gbc_lblMaxLogLength);
		
		spnMaxLogLength = new JSpinner();
		lblMaxLogLength.setLabelFor(spnMaxLogLength);
		spnMaxLogLength.setModel(new SpinnerNumberModel(500, 1, 2147483647, 1));
		GridBagConstraints gbc_spnMaxLogLength = new GridBagConstraints();
		gbc_spnMaxLogLength.anchor = GridBagConstraints.BASELINE_TRAILING;
		gbc_spnMaxLogLength.insets = new Insets(0, 0, 5, 5);
		gbc_spnMaxLogLength.gridx = 1;
		gbc_spnMaxLogLength.gridy = 10;
		generalTab.add(spnMaxLogLength, gbc_spnMaxLogLength);
		
		lblChars = new JLabel("chars");
		GridBagConstraints gbc_lblChars = new GridBagConstraints();
		gbc_lblChars.insets = new Insets(0, 0, 5, 20);
		gbc_lblChars.gridx = 2;
		gbc_lblChars.gridy = 10;
		generalTab.add(lblChars, gbc_lblChars);
		
		Title title_2 = new Title("Selection color");
		GridBagConstraints gbc_title_2 = new GridBagConstraints();
		gbc_title_2.fill = GridBagConstraints.BOTH;
		gbc_title_2.gridwidth = 3;
		gbc_title_2.insets = new Insets(15, 10, 5, 10);
		gbc_title_2.gridx = 0;
		gbc_title_2.gridy = 11;
		generalTab.add(title_2, gbc_title_2);
		
		btnEditSelectionColor = new JButton("Edit...");
		btnEditSelectionColor.setMnemonic(KeyEvent.VK_D);
		btnEditSelectionColor.setIcon(newColorIcon(Color.BLUE));
		GridBagConstraints gbc_btnEditSelectionColor = new GridBagConstraints();
		gbc_btnEditSelectionColor.gridwidth = 3;
		gbc_btnEditSelectionColor.anchor = GridBagConstraints.LINE_START;
		gbc_btnEditSelectionColor.insets = new Insets(0, 20, 30, 0);
		gbc_btnEditSelectionColor.gridx = 0;
		gbc_btnEditSelectionColor.gridy = 12;
		generalTab.add(btnEditSelectionColor, gbc_btnEditSelectionColor);

		ButtonGroup buttonGroup = new ButtonGroup();
		ButtonGroup buttonGroup1 = new ButtonGroup();
		ButtonGroup buttonGroup2 = new ButtonGroup();

		JPanel uiTab = new JPanel();
		addTab("User interface", null, uiTab, null);
		setMnemonicAt(1, KeyEvent.VK_I);
		GridBagLayout gbl_uiTab = new GridBagLayout();
		gbl_uiTab.rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_uiTab.columnWidths = new int[] { 0, 0 };
		gbl_uiTab.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_uiTab.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		uiTab.setLayout(gbl_uiTab);

		Title title_3 = new Title("Control windows");
		GridBagConstraints gbc_title_3 = new GridBagConstraints();
		gbc_title_3.weightx = 1.0;
		gbc_title_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_title_3.insets = new Insets(10, 10, 5, 10);
		gbc_title_3.gridx = 0;
		gbc_title_3.gridy = 0;
		uiTab.add(title_3, gbc_title_3);

		JLabel lblNewLabel = new JLabel("When a control window is closed:");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.LINE_START;
		gbc_lblNewLabel.insets = new Insets(0, 20, 5, 20);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 1;
		uiTab.add(lblNewLabel, gbc_lblNewLabel);

		rdbtnHideTheWindow = new JRadioButton("Hide the window");
		rdbtnHideTheWindow.setMnemonic(KeyEvent.VK_H);
		buttonGroup.add(rdbtnHideTheWindow);
		GridBagConstraints gbc_rdbtnHideTheWindow = new GridBagConstraints();
		gbc_rdbtnHideTheWindow.anchor = GridBagConstraints.LINE_START;
		gbc_rdbtnHideTheWindow.insets = new Insets(0, 30, 5, 20);
		gbc_rdbtnHideTheWindow.gridx = 0;
		gbc_rdbtnHideTheWindow.gridy = 2;
		uiTab.add(rdbtnHideTheWindow, gbc_rdbtnHideTheWindow);

		rdbtnDisposeTheWindow = new JRadioButton(
				"Dispose the window (dangerous)");
		rdbtnDisposeTheWindow.setMnemonic(KeyEvent.VK_D);
		buttonGroup.add(rdbtnDisposeTheWindow);
		GridBagConstraints gbc_rdbtnDisposeTheWindow = new GridBagConstraints();
		gbc_rdbtnDisposeTheWindow.anchor = GridBagConstraints.LINE_START;
		gbc_rdbtnDisposeTheWindow.insets = new Insets(0, 30, 5, 20);
		gbc_rdbtnDisposeTheWindow.gridx = 0;
		gbc_rdbtnDisposeTheWindow.gridy = 3;
		uiTab.add(rdbtnDisposeTheWindow, gbc_rdbtnDisposeTheWindow);

		rdbtnAskWhatTo = new JRadioButton("Ask what to do");
		rdbtnAskWhatTo.setMnemonic(KeyEvent.VK_A);
		rdbtnAskWhatTo.setSelected(true);
		buttonGroup.add(rdbtnAskWhatTo);
		GridBagConstraints gbc_rdbtnAskWhatTo = new GridBagConstraints();
		gbc_rdbtnAskWhatTo.insets = new Insets(0, 30, 10, 0);
		gbc_rdbtnAskWhatTo.anchor = GridBagConstraints.LINE_START;
		gbc_rdbtnAskWhatTo.gridx = 0;
		gbc_rdbtnAskWhatTo.gridy = 4;
		uiTab.add(rdbtnAskWhatTo, gbc_rdbtnAskWhatTo);
		
		JLabel lblNewLabel_1 = new JLabel();
		URI uri;
		try {
			uri = getClass().getResource("icons/trash_can.png").toURI();
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		}
		lblNewLabel_1.setText("<html>Remember it is always possible to dispose a control window by<br>" +
				"clicking the trash can button in the toolbar.</html>");
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_1.anchor = GridBagConstraints.LINE_START;
		gbc_lblNewLabel_1.insets = new Insets(0, 20, 5, 20);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 5;
		uiTab.add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		Title title_5 = new Title("Browser invocation");
		GridBagConstraints gbc_title_5 = new GridBagConstraints();
		gbc_title_5.fill = GridBagConstraints.HORIZONTAL;
		gbc_title_5.insets = new Insets(15, 10, 5, 10);
		gbc_title_5.gridx = 0;
		gbc_title_5.gridy = 6;
		uiTab.add(title_5, gbc_title_5);
		
		rdbtnUseJavaDesktopInteraction = new JRadioButton("Use Java Desktop interaction");
		buttonGroup1.add(rdbtnUseJavaDesktopInteraction);
		rdbtnUseJavaDesktopInteraction.setSelected(true);
		rdbtnUseJavaDesktopInteraction.setMnemonic(KeyEvent.VK_U);
		GridBagConstraints gbc_rdbtnUseJavaDesktopInteraction = new GridBagConstraints();
		gbc_rdbtnUseJavaDesktopInteraction.anchor = GridBagConstraints.LINE_START;
		gbc_rdbtnUseJavaDesktopInteraction.insets = new Insets(0, 20, 5, 20);
		gbc_rdbtnUseJavaDesktopInteraction.gridx = 0;
		gbc_rdbtnUseJavaDesktopInteraction.gridy = 7;
		uiTab.add(rdbtnUseJavaDesktopInteraction, gbc_rdbtnUseJavaDesktopInteraction);
		
		rdbtnLaunchCommand = new JRadioButton("Launch command:");
		buttonGroup1.add(rdbtnLaunchCommand);
		rdbtnLaunchCommand.setMnemonic(KeyEvent.VK_L);
		GridBagConstraints gbc_rdbtnLaunchCommand = new GridBagConstraints();
		gbc_rdbtnLaunchCommand.anchor = GridBagConstraints.LINE_START;
		gbc_rdbtnLaunchCommand.insets = new Insets(0, 20, 5, 20);
		gbc_rdbtnLaunchCommand.gridx = 0;
		gbc_rdbtnLaunchCommand.gridy = 8;
		uiTab.add(rdbtnLaunchCommand, gbc_rdbtnLaunchCommand);
		
		tfBrowserCommand = new JTextField();
		tfBrowserCommand.setEnabled(false);
		tfBrowserCommand.setColumns(10);
		GridBagConstraints gbc_tfBrowserCommand = new GridBagConstraints();
		gbc_tfBrowserCommand.fill = GridBagConstraints.HORIZONTAL;
		gbc_tfBrowserCommand.insets = new Insets(0, 40, 5, 20);
		gbc_tfBrowserCommand.gridx = 0;
		gbc_tfBrowserCommand.gridy = 9;
		uiTab.add(tfBrowserCommand, gbc_tfBrowserCommand);
		
		btnBrowseCmd = new JButton("Browse...");
		btnBrowseCmd.setMnemonic(KeyEvent.VK_B);
		btnBrowseCmd.setEnabled(false);
		GridBagConstraints gbc_btnBrowseCmd = new GridBagConstraints();
		gbc_btnBrowseCmd.anchor = GridBagConstraints.LINE_START;
		gbc_btnBrowseCmd.insets = new Insets(0, 40, 30, 20);
		gbc_btnBrowseCmd.gridx = 0;
		gbc_btnBrowseCmd.gridy = 10;
		uiTab.add(btnBrowseCmd, gbc_btnBrowseCmd);

		JPanel startupTab = new JPanel();
		addTab("On startup", null, startupTab, null);
		setMnemonicAt(2, KeyEvent.VK_S);
		GridBagLayout gbl_startupTab = new GridBagLayout();
		gbl_startupTab.columnWidths = new int[] { 0, 0, 0 };
		gbl_startupTab.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0 };
		gbl_startupTab.columnWeights = new double[] { 1.0, 0.0,
				Double.MIN_VALUE };
		gbl_startupTab.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		startupTab.setLayout(gbl_startupTab);
		rdbtnComputeThisCombinationWDefVal = new JRadioButton(
				"Compute this combination with default values:");
		rdbtnComputeThisCombinationWDefVal.setMnemonic(KeyEvent.VK_C);
		rdbtnComputeThisCombinationWDefVal.setSelected(true);
		buttonGroup2.add(rdbtnComputeThisCombinationWDefVal);
		GridBagConstraints gbc_rdbtnComputeThisCombinationWDefVal = new GridBagConstraints();
		gbc_rdbtnComputeThisCombinationWDefVal.gridwidth = 2;
		gbc_rdbtnComputeThisCombinationWDefVal.anchor = GridBagConstraints.LINE_START;
		gbc_rdbtnComputeThisCombinationWDefVal.insets = new Insets(10, 10, 5,
				10);
		gbc_rdbtnComputeThisCombinationWDefVal.gridx = 0;
		gbc_rdbtnComputeThisCombinationWDefVal.gridy = 0;
		startupTab.add(rdbtnComputeThisCombinationWDefVal,
				gbc_rdbtnComputeThisCombinationWDefVal);

		lblNumberFactory = new JLabel("Number factory:");
		lblNumberFactory.setDisplayedMnemonic(KeyEvent.VK_N);
		GridBagConstraints gbc_lblNumberFactory = new GridBagConstraints();
		gbc_lblNumberFactory.gridwidth = 2;
		gbc_lblNumberFactory.insets = new Insets(0, 30, 5, 0);
		gbc_lblNumberFactory.anchor = GridBagConstraints.LINE_START;
		gbc_lblNumberFactory.gridx = 0;
		gbc_lblNumberFactory.gridy = 1;
		startupTab.add(lblNumberFactory, gbc_lblNumberFactory);

		cmbbxNumberFactory = new JComboBox<>();
		lblNumberFactory.setLabelFor(cmbbxNumberFactory);
		GridBagConstraints gbc_cmbbxNumberFactory = new GridBagConstraints();
		gbc_cmbbxNumberFactory.gridwidth = 2;
		gbc_cmbbxNumberFactory.insets = new Insets(0, 30, 5, 30);
		gbc_cmbbxNumberFactory.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbbxNumberFactory.gridx = 0;
		gbc_cmbbxNumberFactory.gridy = 2;
		startupTab.add(cmbbxNumberFactory, gbc_cmbbxNumberFactory);

		lblFormula = new JLabel("Formula:");
		lblFormula.setDisplayedMnemonic(KeyEvent.VK_F);
		GridBagConstraints gbc_lblFormula = new GridBagConstraints();
		gbc_lblFormula.gridwidth = 2;
		gbc_lblFormula.insets = new Insets(0, 30, 5, 0);
		gbc_lblFormula.anchor = GridBagConstraints.LINE_START;
		gbc_lblFormula.gridx = 0;
		gbc_lblFormula.gridy = 3;
		startupTab.add(lblFormula, gbc_lblFormula);

		cmbbxFormula = new JComboBox<>();
		lblFormula.setLabelFor(cmbbxFormula);
		GridBagConstraints gbc_cmbbxFormula = new GridBagConstraints();
		gbc_cmbbxFormula.gridwidth = 2;
		gbc_cmbbxFormula.insets = new Insets(0, 30, 5, 30);
		gbc_cmbbxFormula.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbbxFormula.gridx = 0;
		gbc_cmbbxFormula.gridy = 4;
		startupTab.add(cmbbxFormula, gbc_cmbbxFormula);

		lblRepresentation = new JLabel("Representation:");
		lblRepresentation.setDisplayedMnemonic(KeyEvent.VK_R);
		GridBagConstraints gbc_lblRepresentation = new GridBagConstraints();
		gbc_lblRepresentation.gridwidth = 2;
		gbc_lblRepresentation.insets = new Insets(0, 30, 5, 0);
		gbc_lblRepresentation.anchor = GridBagConstraints.LINE_START;
		gbc_lblRepresentation.gridx = 0;
		gbc_lblRepresentation.gridy = 5;
		startupTab.add(lblRepresentation, gbc_lblRepresentation);

		cmbbxRepresentation = new JComboBox<>();
		lblRepresentation.setLabelFor(cmbbxRepresentation);
		GridBagConstraints gbc_cmbbxRepresentation = new GridBagConstraints();
		gbc_cmbbxRepresentation.gridwidth = 2;
		gbc_cmbbxRepresentation.insets = new Insets(0, 30, 5, 30);
		gbc_cmbbxRepresentation.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbbxRepresentation.gridx = 0;
		gbc_cmbbxRepresentation.gridy = 6;
		startupTab.add(cmbbxRepresentation, gbc_cmbbxRepresentation);
		
		chckbxForceEqualScales = new JCheckBox("Force equal scales");
		chckbxForceEqualScales.setMnemonic(KeyEvent.VK_O);
		GridBagConstraints gbc_chckbxForceEqualScales = new GridBagConstraints();
		gbc_chckbxForceEqualScales.gridwidth = 2;
		gbc_chckbxForceEqualScales.anchor = GridBagConstraints.LINE_START;
		gbc_chckbxForceEqualScales.insets = new Insets(0, 30, 5, 30);
		gbc_chckbxForceEqualScales.gridx = 0;
		gbc_chckbxForceEqualScales.gridy = 7;
		startupTab.add(chckbxForceEqualScales, gbc_chckbxForceEqualScales);

		chckbxJuliaSet = new JCheckBox("Julia set");
		chckbxJuliaSet.setMnemonic(KeyEvent.VK_J);
		GridBagConstraints gbc_chckbxJuliaSet = new GridBagConstraints();
		gbc_chckbxJuliaSet.gridwidth = 2;
		gbc_chckbxJuliaSet.anchor = GridBagConstraints.LINE_START;
		gbc_chckbxJuliaSet.insets = new Insets(0, 30, 5, 30);
		gbc_chckbxJuliaSet.gridx = 0;
		gbc_chckbxJuliaSet.gridy = 8;
		startupTab.add(chckbxJuliaSet, gbc_chckbxJuliaSet);


		rdbtnLoadProductionFromFile = new JRadioButton("Load image from file:");
		rdbtnLoadProductionFromFile.setMnemonic(KeyEvent.VK_L);
		buttonGroup2.add(rdbtnLoadProductionFromFile);
		GridBagConstraints gbc_rdbtnLoadProductionFromFile = new GridBagConstraints();
		gbc_rdbtnLoadProductionFromFile.gridwidth = 2;
		gbc_rdbtnLoadProductionFromFile.anchor = GridBagConstraints.LINE_START;
		gbc_rdbtnLoadProductionFromFile.insets = new Insets(10, 10, 5, 10);
		gbc_rdbtnLoadProductionFromFile.gridx = 0;
		gbc_rdbtnLoadProductionFromFile.gridy = 9;
		startupTab.add(rdbtnLoadProductionFromFile, gbc_rdbtnLoadProductionFromFile);

		tfStartupImagePath = new JTextField();
		tfStartupImagePath.setEnabled(false);
		GridBagConstraints gbc_tfStartupImagePath = new GridBagConstraints();
		gbc_tfStartupImagePath.gridwidth = 2;
		gbc_tfStartupImagePath.anchor = GridBagConstraints.BASELINE;
		gbc_tfStartupImagePath.insets = new Insets(0, 30, 5, 30);
		gbc_tfStartupImagePath.fill = GridBagConstraints.HORIZONTAL;
		gbc_tfStartupImagePath.gridx = 0;
		gbc_tfStartupImagePath.gridy = 10;
		startupTab.add(tfStartupImagePath, gbc_tfStartupImagePath);
		tfStartupImagePath.setColumns(10);

		btnBrowseImg = new JButton("Browse...");
		btnBrowseImg.setEnabled(false);
		btnBrowseImg.setMnemonic(KeyEvent.VK_B);
		GridBagConstraints gbc_btnBrowseImg = new GridBagConstraints();
		gbc_btnBrowseImg.gridwidth = 2;
		gbc_btnBrowseImg.insets = new Insets(0, 30, 30, 30);
		gbc_btnBrowseImg.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_btnBrowseImg.gridx = 0;
		gbc_btnBrowseImg.gridy = 11;

		startupTab.add(btnBrowseImg, gbc_btnBrowseImg);
		
		installListeners();
	}

	private void installListeners() {
		chckbxLoggingEnabled.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED) {
					chckbxLoggingEnabled.setText("yes");
				} else {
					chckbxLoggingEnabled.setText("no");
				}
			}
		});

		rdbtnLaunchCommand.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean enabled = e.getStateChange() == ItemEvent.SELECTED;
				tfBrowserCommand.setEnabled(enabled);
				btnBrowseCmd.setEnabled(enabled);
			}
		});

		btnBrowseCmd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (executableFileFilter == null) {
					String osName = System.getProperty("os.name");
					if (osName != null) {
						osName = osName.toLowerCase(Locale.ENGLISH);
						if (osName.contains("mac os") || osName.contains("darwin")) {
							executableFileFilter = new MacOSXExecutableFileFilter();
						} else if (osName.contains("windows")) {
							executableFileFilter = new WindowsExecutableFileFilter();
						} else if (osName.contains("linux") ||
								osName.contains("unix") ||
								osName.contains("solaris") ||
								osName.contains("sunos")) {
							executableFileFilter = new UnixExecutableFileFilter();
						}
					}
				}
				
				JFileChooser fc = new JFileChooser();
				if (executableFileFilter != null) {
					fc.setFileFilter(executableFileFilter);
					fc.setAcceptAllFileFilterUsed(false);
				}
				fc.setApproveButtonText("OK");
				fc.setDialogTitle("Browse");
				int rv = fc.showDialog(PreferenceTabs.this, null);
				if (rv == JFileChooser.APPROVE_OPTION) {
					tfBrowserCommand.setText(fc.getSelectedFile().toString());
				}
			}
		});

		rdbtnComputeThisCombinationWDefVal.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean enabled = e.getStateChange() == ItemEvent.SELECTED;
				lblNumberFactory.setEnabled(enabled);
				cmbbxNumberFactory.setEnabled(enabled);
				lblFormula.setEnabled(enabled);
				cmbbxFormula.setEnabled(enabled);
				lblRepresentation.setEnabled(enabled);
				cmbbxRepresentation.setEnabled(enabled);
				chckbxJuliaSet.setEnabled(enabled);
				chckbxForceEqualScales.setEnabled(enabled);
			}
		});

		rdbtnLoadProductionFromFile.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean enabled = e.getStateChange() == ItemEvent.SELECTED;
				tfStartupImagePath.setEnabled(enabled);
				btnBrowseImg.setEnabled(enabled);
			}
		});

		btnBrowseImg.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(new FileNameExtensionFilter("Julia image (*.jim)", "jim"));
				fc.setDialogTitle("Browse");
				fc.setApproveButtonText("OK");
				int rv = fc.showDialog(PreferenceTabs.this, null);
				if (rv == JFileChooser.APPROVE_OPTION) {
					tfStartupImagePath.setText(fc.getSelectedFile().toString());
				}
			}
		});
	}

	public void init(final Application application) {
		preferences = application.getPreferences();
		spnWidth.setValue(preferences.getImageWidth());
		spnHeight.setValue(preferences.getImageHeight());
		cmbbxTransparency.setSelectedIndex(preferences.getTransparency() - 1);
		spnNumOfProducerThreads.setValue(preferences.getNumOfProducerThreads());
		spnRefreshDelay.setValue(preferences.getRefreshDelay());
		chckbxLoggingEnabled.setSelected(preferences.isLoggingEnabled());
		spnMaxLogLength.setValue(preferences.getMaxLogLength());

		switch (preferences.getDefaultCloseBehaviour()) {
		case HIDE: rdbtnHideTheWindow.setSelected(true); break;
		case DISPOSE: rdbtnDisposeTheWindow.setSelected(true); break;
		case ASK: rdbtnAskWhatTo.setSelected(true); break;
		default: throw new AssertionError(preferences.getDefaultCloseBehaviour());
		}
		rdbtnUseJavaDesktopInteraction.setSelected(preferences
				.isJavaDesktopInteractionEnabled());
		rdbtnLaunchCommand.setSelected(!preferences
				.isJavaDesktopInteractionEnabled());
		tfBrowserCommand.setText(preferences.getBrowserCommand());

		rdbtnComputeThisCombinationWDefVal.setSelected(preferences
				.isStartupCombinationEnabled());
		rdbtnLoadProductionFromFile.setSelected(!preferences
				.isStartupCombinationEnabled());
		cmbbxNumberFactory.setModel(new PluginSelectionPane.ComboBoxModel<>(
				application.getNumberFactories(), preferences
						.getStartupNumberFactory()));
		cmbbxFormula.setModel(new PluginSelectionPane.ComboBoxModel<>(
				application.getFormulas(), preferences.getStartupFormula()));
		cmbbxRepresentation.setModel(new PluginSelectionPane.ComboBoxModel<>(
				application.getRepresentations(), preferences
						.getStartupRepresentation()));
		chckbxJuliaSet.setSelected(preferences.getStartupJuliaSetFlag());
		chckbxForceEqualScales.setSelected(preferences.getStartupForceEqualScalesFlag());
		tfStartupImagePath.setText(preferences.getStartupImagePath());
		
		ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == btnEditSelectionColor) {
					JColorChooser colorChooser = new JColorChooser(getSelectionColor());
					application.addSelectionPreviewTo(colorChooser);
					JOptionPane optionPane = createOptionPane(colorChooser);
					JDialog dialog = createDialog(optionPane,
							PreferenceTabs.this,
							"Selection color",
							true);
					dialog.setVisible(true);
					dialog.dispose();
					Integer value = (Integer) optionPane.getValue();
					if (value != null && value.intValue() == JOptionPane.OK_OPTION) {
						btnEditSelectionColor.setIcon(newColorIcon(colorChooser.getColor()));
					}
				} else if (e.getSource() == btnMatchCurrImgSize) {
					Dimension size = application.getCurrentImageSize();
					spnWidth.setValue(size.width);
					spnHeight.setValue(size.height);
				} else if (e.getSource() == btnMatchWindowSize) {
					Dimension size = application.getImagePanelSize();
					spnWidth.setValue(size.width);
					spnHeight.setValue(size.height);
				} else if (e.getSource() == btnMatchCurrImgTransp) {
					cmbbxTransparency.setSelectedIndex(application.getCurrentImageTransparency() - 1);
				}
			}
		};

		btnEditSelectionColor.setIcon(newColorIcon(preferences.getSelectionColor()));
		btnEditSelectionColor.addActionListener(actionListener);
		btnMatchCurrImgSize.addActionListener(actionListener);
		btnMatchWindowSize.addActionListener(actionListener);
		btnMatchCurrImgTransp.addActionListener(actionListener);
	}

	private static ColorIcon newColorIcon(Color color) {
		return new ColorIcon(52, 22, color);
	}

	public Preferences commit() {
		if (isJavaDesktopInteractionEnabled()) {
			tfBrowserCommand.setText(preferences.getBrowserCommand());
		}
		if (isStartupCombinationEnabled()) {
			tfStartupImagePath.setText(preferences.getStartupImagePath());
		} else {
			((PluginSelectionPane.ComboBoxModel<NumberFactoryPlugin>) cmbbxNumberFactory
					.getModel()).setSelectedPlugin(preferences
					.getStartupNumberFactory());
			((PluginSelectionPane.ComboBoxModel<FormulaPlugin>) cmbbxFormula
					.getModel()).setSelectedPlugin(preferences
					.getStartupFormula());
			((PluginSelectionPane.ComboBoxModel<RepresentationPlugin>) cmbbxRepresentation
					.getModel()).setSelectedPlugin(preferences
					.getStartupRepresentation());
			chckbxJuliaSet.setSelected(preferences.getStartupJuliaSetFlag());
			chckbxForceEqualScales.setSelected(preferences.getStartupForceEqualScalesFlag());
		}

		setSelectedIndex(0);

		preferences = new Preferences(getImageWidth(), getImageHeight(), getTransparency(),
				getRefreshDelay(), getNumOfProducerThreads(), isLoggingEnabled(), getMaxLogLength(),
				getSelectionColor(),
				getDefaultDocumentCloseOperation(),
				isJavaDesktopInteractionEnabled(), getBrowserCommand(),
				isStartupCombinationEnabled(),
				getStartupNumberFactory(), getStartupFormula(), getStartupRepresentation(),
				getStartupJuliaSetFlag(),
				getStartupForceEqualScalesFlag(),
				getStartupImagePath());
		return preferences;
	}

	public void cancel() {
		spnWidth.setValue(preferences.getImageWidth());
		spnHeight.setValue(preferences.getImageHeight());
		cmbbxTransparency.setSelectedIndex(preferences.getTransparency() - 1);
		spnRefreshDelay.setValue(preferences.getRefreshDelay());
		spnNumOfProducerThreads.setValue(preferences.getNumOfProducerThreads());
		chckbxLoggingEnabled.setSelected(preferences.isLoggingEnabled());
		spnMaxLogLength.setValue(preferences.getMaxLogLength());
		btnEditSelectionColor.setIcon(newColorIcon(preferences.getSelectionColor()));
		
		switch (preferences.getDefaultCloseBehaviour()) {
		case HIDE: rdbtnHideTheWindow.setSelected(true); break;
		case DISPOSE: rdbtnDisposeTheWindow.setSelected(true); break;
		case ASK: rdbtnAskWhatTo.setSelected(true); break;
		default: throw new AssertionError(preferences.getDefaultCloseBehaviour());
		}
		rdbtnUseJavaDesktopInteraction.setSelected(preferences
				.isJavaDesktopInteractionEnabled());
		rdbtnLaunchCommand.setSelected(!preferences
				.isJavaDesktopInteractionEnabled());
		tfBrowserCommand.setText(preferences.getBrowserCommand());

		rdbtnComputeThisCombinationWDefVal.setSelected(preferences
				.isStartupCombinationEnabled());
		rdbtnLoadProductionFromFile.setSelected(!preferences
				.isStartupCombinationEnabled());
		((PluginSelectionPane.ComboBoxModel<NumberFactoryPlugin>) cmbbxNumberFactory
				.getModel()).setSelectedPlugin(preferences
				.getStartupNumberFactory());
		((PluginSelectionPane.ComboBoxModel<FormulaPlugin>) cmbbxFormula
				.getModel()).setSelectedPlugin(preferences.getStartupFormula());
		((PluginSelectionPane.ComboBoxModel<RepresentationPlugin>) cmbbxRepresentation
				.getModel()).setSelectedPlugin(preferences
				.getStartupRepresentation());
		chckbxJuliaSet.setSelected(preferences.getStartupJuliaSetFlag());
		chckbxForceEqualScales.setSelected(preferences.getStartupForceEqualScalesFlag());
		tfStartupImagePath.setText(preferences.getStartupImagePath());

		setSelectedIndex(0);
	}

	public void update() {
		if (!preferences.getSelectionColor().equals(getSelectionColor())) {
			btnEditSelectionColor.setIcon(newColorIcon(preferences.getSelectionColor()));
		}
		if (preferences.getDefaultCloseBehaviour() != getDefaultDocumentCloseOperation()) {
			switch (preferences.getDefaultCloseBehaviour()) {
			case HIDE: rdbtnHideTheWindow.setSelected(true); break;
			case DISPOSE: rdbtnDisposeTheWindow.setSelected(true); break;
			case ASK: rdbtnAskWhatTo.setSelected(true); break;
			default: throw new AssertionError(preferences.getDefaultCloseBehaviour());
			}
		}
	}

	public int getImageWidth() {
		return (Integer) spnWidth.getValue();
	}

	public int getImageHeight() {
		return (Integer) spnHeight.getValue();
	}

	public int getTransparency() {
		return cmbbxTransparency.getSelectedIndex() + 1;
	}

	public int getRefreshDelay() {
		return (Integer) spnRefreshDelay.getValue();
	}

	public int getNumOfProducerThreads() {
		return (Integer) spnNumOfProducerThreads.getValue();
	}

	public Color getSelectionColor() {
		return ((ColorIcon) btnEditSelectionColor.getIcon()).getColor();
	}

	public boolean isJavaDesktopInteractionEnabled() {
		return rdbtnUseJavaDesktopInteraction.isSelected();
	}

	public String getBrowserCommand() {
		return tfBrowserCommand.getText();
	}

	public boolean isLoggingEnabled() {
		return chckbxLoggingEnabled.isSelected();
	}

	public int getMaxLogLength() {
		return (Integer) spnMaxLogLength.getValue();
	}

	public boolean isStartupCombinationEnabled() {
		return rdbtnComputeThisCombinationWDefVal.isSelected();
	}

	public String getStartupNumberFactory() {
		return ((PluginSelectionPane.ComboBoxModel<NumberFactoryPlugin>) cmbbxNumberFactory
				.getModel()).getSelectedPlugin();
	}

	public String getStartupFormula() {
		return ((PluginSelectionPane.ComboBoxModel<FormulaPlugin>) cmbbxFormula
				.getModel()).getSelectedPlugin();
	}

	public String getStartupRepresentation() {
		return ((PluginSelectionPane.ComboBoxModel<RepresentationPlugin>) cmbbxRepresentation
				.getModel()).getSelectedPlugin();
	}

	public boolean getStartupJuliaSetFlag() {
		return chckbxJuliaSet.isSelected();
	}

	public boolean getStartupForceEqualScalesFlag() {
		return chckbxForceEqualScales.isSelected();
	}

	public String getStartupImagePath() {
		return tfStartupImagePath.getText();
	}

	public DefaultCloseBehaviour getDefaultDocumentCloseOperation() {
		if (rdbtnHideTheWindow.isSelected())
			return DefaultCloseBehaviour.HIDE;
		if (rdbtnDisposeTheWindow.isSelected())
			return DefaultCloseBehaviour.DISPOSE;
		if (rdbtnAskWhatTo.isSelected())
			return DefaultCloseBehaviour.ASK;
		throw new AssertionError();
	}
}

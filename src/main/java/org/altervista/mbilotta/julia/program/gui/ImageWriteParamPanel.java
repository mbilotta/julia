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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.bmp.BMPImageWriteParam;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;


public class ImageWriteParamPanel extends JPanel {
	private JRadioButton rdbtnUseDefaultCompSettings;
	private JRadioButton rdbtnUseThisCompSettings;
	private JComboBox<String> comboBoxCompType;
	private JSlider sliderCompLevel;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private JLabel lblCompType;
	private JLabel lblCompLevel;
	private JLabel lblNoSettings;
	private JCheckBox chckbxProgressive;
	private JCheckBox chckbxTopDown;
	private JCheckBox chckbxOptimizeHuffmanTables;
	private Title titleOther;
	
	private ImageWriter imageWriter;

	/**
	 * Create the panel.
	 */
	public ImageWriteParamPanel() {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		Title titleComp = new Title("Compression");
		GridBagConstraints gbc_titleComp = new GridBagConstraints();
		gbc_titleComp.fill = GridBagConstraints.HORIZONTAL;
		gbc_titleComp.gridwidth = 2;
		gbc_titleComp.insets = new Insets(0, 0, 5, 0);
		gbc_titleComp.gridx = 0;
		gbc_titleComp.gridy = 0;
		add(titleComp, gbc_titleComp);
		
		lblNoSettings = new JLabel("No settings");
		GridBagConstraints gbc_lblNoSettings = new GridBagConstraints();
		gbc_lblNoSettings.gridwidth = 2;
		gbc_lblNoSettings.insets = new Insets(0, 10, 5, 10);
		gbc_lblNoSettings.gridx = 0;
		gbc_lblNoSettings.gridy = 1;
		add(lblNoSettings, gbc_lblNoSettings);
		
		rdbtnUseDefaultCompSettings = new JRadioButton("Use default settings");
		rdbtnUseDefaultCompSettings.setSelected(true);
		buttonGroup.add(rdbtnUseDefaultCompSettings);
		rdbtnUseDefaultCompSettings.setMnemonic(KeyEvent.VK_U);
		GridBagConstraints gbc_rdbtnUseDefaultCompSettings = new GridBagConstraints();
		gbc_rdbtnUseDefaultCompSettings.anchor = GridBagConstraints.LINE_START;
		gbc_rdbtnUseDefaultCompSettings.gridwidth = 2;
		gbc_rdbtnUseDefaultCompSettings.insets = new Insets(0, 10, 5, 10);
		gbc_rdbtnUseDefaultCompSettings.gridx = 0;
		gbc_rdbtnUseDefaultCompSettings.gridy = 2;
		add(rdbtnUseDefaultCompSettings, gbc_rdbtnUseDefaultCompSettings);
		
		rdbtnUseThisCompSettings = new JRadioButton("Use this settings:");
		buttonGroup.add(rdbtnUseThisCompSettings);
		rdbtnUseThisCompSettings.setMnemonic(KeyEvent.VK_T);
		GridBagConstraints gbc_rdbtnUseThisCompSettings = new GridBagConstraints();
		gbc_rdbtnUseThisCompSettings.anchor = GridBagConstraints.LINE_START;
		gbc_rdbtnUseThisCompSettings.gridwidth = 2;
		gbc_rdbtnUseThisCompSettings.insets = new Insets(0, 10, 5, 10);
		gbc_rdbtnUseThisCompSettings.gridx = 0;
		gbc_rdbtnUseThisCompSettings.gridy = 3;
		add(rdbtnUseThisCompSettings, gbc_rdbtnUseThisCompSettings);
		
		lblCompType = new JLabel("Compression type:");
		lblCompType.setEnabled(false);
		lblCompType.setDisplayedMnemonic(KeyEvent.VK_C);
		GridBagConstraints gbc_lblCompType = new GridBagConstraints();
		gbc_lblCompType.insets = new Insets(0, 30, 5, 5);
		gbc_lblCompType.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblCompType.gridx = 0;
		gbc_lblCompType.gridy = 4;
		add(lblCompType, gbc_lblCompType);
		
		comboBoxCompType = new JComboBox<String>();
		comboBoxCompType.setEnabled(false);
		lblCompType.setLabelFor(comboBoxCompType);
		GridBagConstraints gbc_comboBoxCompType = new GridBagConstraints();
		gbc_comboBoxCompType.anchor = GridBagConstraints.BASELINE_TRAILING;
		gbc_comboBoxCompType.insets = new Insets(0, 0, 5, 30);
		gbc_comboBoxCompType.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxCompType.gridx = 1;
		gbc_comboBoxCompType.gridy = 4;
		add(comboBoxCompType, gbc_comboBoxCompType);
		
		lblCompLevel = new JLabel("Compression level:");
		lblCompLevel.setEnabled(false);
		lblCompLevel.setDisplayedMnemonic(KeyEvent.VK_L);
		GridBagConstraints gbc_lblCompLevel = new GridBagConstraints();
		gbc_lblCompLevel.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblCompLevel.insets = new Insets(0, 30, 5, 5);
		gbc_lblCompLevel.gridx = 0;
		gbc_lblCompLevel.gridy = 5;
		add(lblCompLevel, gbc_lblCompLevel);
		
		sliderCompLevel = new JSlider();
		sliderCompLevel.setEnabled(false);
		lblCompLevel.setLabelFor(sliderCompLevel);
		sliderCompLevel.setMinorTickSpacing(10);
		sliderCompLevel.setMajorTickSpacing(50);
		sliderCompLevel.setPaintTicks(true);
		sliderCompLevel.setPaintLabels(true);
		GridBagConstraints gbc_sliderCompLevel = new GridBagConstraints();
		gbc_sliderCompLevel.insets = new Insets(0, 0, 5, 30);
		gbc_sliderCompLevel.anchor = GridBagConstraints.BASELINE_TRAILING;
		gbc_sliderCompLevel.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderCompLevel.gridx = 1;
		gbc_sliderCompLevel.gridy = 5;
		add(sliderCompLevel, gbc_sliderCompLevel);

		rdbtnUseThisCompSettings.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean enabled = e.getStateChange() == ItemEvent.SELECTED;
				lblCompType.setEnabled(enabled);
				comboBoxCompType.setEnabled(enabled);
				lblCompLevel.setEnabled(enabled);
				sliderCompLevel.setEnabled(enabled);
			}
		});
		
		titleOther = new Title("Other");
		GridBagConstraints gbc_titleOther = new GridBagConstraints();
		gbc_titleOther.fill = GridBagConstraints.HORIZONTAL;
		gbc_titleOther.gridwidth = 2;
		gbc_titleOther.insets = new Insets(15, 0, 5, 0);
		gbc_titleOther.gridx = 0;
		gbc_titleOther.gridy = 6;
		add(titleOther, gbc_titleOther);
		
		chckbxProgressive = new JCheckBox("Progressive");
		chckbxProgressive.setMnemonic(KeyEvent.VK_P);
		GridBagConstraints gbc_chckbxProgressive = new GridBagConstraints();
		gbc_chckbxProgressive.anchor = GridBagConstraints.LINE_START;
		gbc_chckbxProgressive.gridwidth = 2;
		gbc_chckbxProgressive.insets = new Insets(0, 10, 5, 10);
		gbc_chckbxProgressive.gridx = 0;
		gbc_chckbxProgressive.gridy = 7;
		add(chckbxProgressive, gbc_chckbxProgressive);
		
		chckbxTopDown = new JCheckBox("Top down");
		chckbxTopDown.setMnemonic(KeyEvent.VK_D);
		GridBagConstraints gbc_chckbxTopDown = new GridBagConstraints();
		gbc_chckbxTopDown.anchor = GridBagConstraints.LINE_START;
		gbc_chckbxTopDown.insets = new Insets(0, 10, 5, 10);
		gbc_chckbxTopDown.gridwidth = 2;
		gbc_chckbxTopDown.gridx = 0;
		gbc_chckbxTopDown.gridy = 8;
		add(chckbxTopDown, gbc_chckbxTopDown);
		
		chckbxOptimizeHuffmanTables = new JCheckBox("Optimize Huffman tables");
		chckbxOptimizeHuffmanTables.setMnemonic(KeyEvent.VK_H);
		GridBagConstraints gbc_chckbxOptimizeHuffmanTables = new GridBagConstraints();
		gbc_chckbxOptimizeHuffmanTables.anchor = GridBagConstraints.FIRST_LINE_START;
		gbc_chckbxOptimizeHuffmanTables.gridwidth = 2;
		gbc_chckbxOptimizeHuffmanTables.insets = new Insets(0, 10, 0, 10);
		gbc_chckbxOptimizeHuffmanTables.gridx = 0;
		gbc_chckbxOptimizeHuffmanTables.gridy = 9;
		add(chckbxOptimizeHuffmanTables, gbc_chckbxOptimizeHuffmanTables);
	}

	private void init(ImageWriter imageWriter) {
		this.imageWriter = imageWriter;
		ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();
		lblNoSettings.setVisible(!imageWriteParam.canWriteCompressed());
		rdbtnUseDefaultCompSettings.setVisible(imageWriteParam.canWriteCompressed());
		rdbtnUseDefaultCompSettings.setSelected(true);
		rdbtnUseThisCompSettings.setVisible(imageWriteParam.canWriteCompressed());
		lblCompType.setVisible(imageWriteParam.canWriteCompressed());
		comboBoxCompType.setVisible(imageWriteParam.canWriteCompressed());
		lblCompLevel.setVisible(imageWriteParam.canWriteCompressed());
		sliderCompLevel.setVisible(imageWriteParam.canWriteCompressed());
		if (imageWriteParam.canWriteCompressed()) {
			String[] compressionTypes = imageWriteParam.getCompressionTypes();
			imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			if (compressionTypes == null) {
				lblCompType.setVisible(false);
				comboBoxCompType.setVisible(false);
			} else {
				lblCompType.setVisible(true);
				comboBoxCompType.setVisible(true);
				comboBoxCompType.setModel(new DefaultComboBoxModel<>(compressionTypes));
				imageWriteParam.setCompressionType((String) comboBoxCompType.getSelectedItem());
			}
			sliderCompLevel.setValue((int) (100 * (1 - imageWriteParam.getCompressionQuality())));
		}
		chckbxProgressive.setVisible(imageWriteParam.canWriteProgressive());
		chckbxProgressive.setSelected(false);
		chckbxTopDown.setVisible(imageWriteParam instanceof BMPImageWriteParam);
		chckbxTopDown.setSelected(false);
		chckbxOptimizeHuffmanTables.setVisible(imageWriteParam instanceof JPEGImageWriteParam);
		chckbxOptimizeHuffmanTables.setSelected(false);
		if (!chckbxProgressive.isVisible() &&
				!chckbxTopDown.isVisible() &&
				!chckbxOptimizeHuffmanTables.isVisible()) {
			titleOther.setVisible(false);
		}
	}

	private int getCompressionMode() {
		return rdbtnUseDefaultCompSettings.isSelected() ? ImageWriteParam.MODE_DEFAULT :
			ImageWriteParam.MODE_EXPLICIT;
	}

	private String getCompressionType() {
		return (String) comboBoxCompType.getSelectedItem();
	}

	private float getCompressionQuality() {
		return (100 - sliderCompLevel.getValue()) / 100f;
	}

	private int getProgressiveMode() {
		return chckbxProgressive.isSelected() ? ImageWriteParam.MODE_DEFAULT :
			ImageWriteParam.MODE_DISABLED;
	}
	
	private boolean getTopDown() {
		return chckbxTopDown.isSelected();
	}

	private boolean getOptimizeHuffmanTables() {
		return chckbxOptimizeHuffmanTables.isSelected();
	}

	public ImageWriteParam getImageWriteParam() {
		ImageWriteParam rv = imageWriter.getDefaultWriteParam();
		if (rv.canWriteCompressed()) {
			int compressionMode = getCompressionMode();
			rv.setCompressionMode(compressionMode);
			if (compressionMode == ImageWriteParam.MODE_EXPLICIT) {
				if (rv.getCompressionTypes() != null) {
					rv.setCompressionType(getCompressionType());
				}
				rv.setCompressionQuality(getCompressionQuality());
			}
		}
		if (rv.canWriteProgressive()) {
			rv.setProgressiveMode(getProgressiveMode());
		}
		if (rv instanceof JPEGImageWriteParam) {
			((JPEGImageWriteParam) rv).setOptimizeHuffmanTables(getOptimizeHuffmanTables());
		}
		if (rv instanceof BMPImageWriteParam) {
			((BMPImageWriteParam) rv).setTopDown(getTopDown());
		}
		return rv;
	}

	public int showDialog(Component parentComponent, ImageWriter imageWriter) {
		init(imageWriter);
		return JOptionPane.showOptionDialog(parentComponent,
				this,
				"Encoding options",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				new Object[] { "Export", "Cancel" },
				"Export");
	}
}

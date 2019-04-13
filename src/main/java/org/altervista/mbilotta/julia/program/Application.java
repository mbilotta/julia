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

package org.altervista.mbilotta.julia.program;

import static org.altervista.mbilotta.julia.Utilities.createDialog;
import static org.altervista.mbilotta.julia.Utilities.createOptionPane;
import static org.altervista.mbilotta.julia.Utilities.getWindowForComponent;
import static org.altervista.mbilotta.julia.Utilities.show;
import static org.altervista.mbilotta.julia.Utilities.showSaveDialog;
import static org.altervista.mbilotta.julia.Utilities.toFront;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.colorchooser.ColorSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.altervista.mbilotta.julia.Consumer;
import org.altervista.mbilotta.julia.Formula;
import org.altervista.mbilotta.julia.IntermediateImage;
import org.altervista.mbilotta.julia.NumberFactory;
import org.altervista.mbilotta.julia.Printer;
import org.altervista.mbilotta.julia.Production;
import org.altervista.mbilotta.julia.Progress;
import org.altervista.mbilotta.julia.Representation;
import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.math.Complex;
import org.altervista.mbilotta.julia.math.CoordinateTransform;
import org.altervista.mbilotta.julia.math.Real;
import org.altervista.mbilotta.julia.program.gui.AboutPane;
import org.altervista.mbilotta.julia.program.gui.ControlWindow;
import org.altervista.mbilotta.julia.program.gui.ImagePanel;
import org.altervista.mbilotta.julia.program.gui.ImageSelection;
import org.altervista.mbilotta.julia.program.gui.ImageWriteParamPanel;
import org.altervista.mbilotta.julia.program.gui.JuliaButtonGroup;
import org.altervista.mbilotta.julia.program.gui.LogPane;
import org.altervista.mbilotta.julia.program.gui.LogTab;
import org.altervista.mbilotta.julia.program.gui.MainWindow;
import org.altervista.mbilotta.julia.program.gui.MessagePane;
import org.altervista.mbilotta.julia.program.gui.PluginSelectionPane;
import org.altervista.mbilotta.julia.program.gui.PreferencesPane;
import org.altervista.mbilotta.julia.program.gui.SplashScreen;
import org.altervista.mbilotta.julia.program.parsers.FormulaPlugin;
import org.altervista.mbilotta.julia.program.parsers.NumberFactoryPlugin;
import org.altervista.mbilotta.julia.program.parsers.Parameter;
import org.altervista.mbilotta.julia.program.parsers.Plugin;
import org.altervista.mbilotta.julia.program.parsers.PluginFamily;
import org.altervista.mbilotta.julia.program.parsers.RepresentationPlugin;


public class Application {
	
	public static final class Image {
		private PluginInstance<NumberFactoryPlugin> numberFactoryInstance;
		private PluginInstance<FormulaPlugin> formulaInstance;
		private PluginInstance<RepresentationPlugin> representationInstance;
		private Rectangle rectangle;
		private boolean forceEqualScales;
		private JuliaSetPoint juliaSetPoint;

		private Image previous;
		private Image next;

		public Image(PluginInstance<NumberFactoryPlugin> numberFactoryInstance,
				PluginInstance<FormulaPlugin> formulaInstance,
				PluginInstance<RepresentationPlugin> representationInstance,
				Rectangle rectangle, boolean forceEqualScales,
				JuliaSetPoint juliaSetPoint) {
			this(numberFactoryInstance, formulaInstance, representationInstance, rectangle, forceEqualScales, juliaSetPoint, null);
		}

		public Image(PluginInstance<NumberFactoryPlugin> numberFactoryInstance,
				PluginInstance<FormulaPlugin> formulaInstance,
				PluginInstance<RepresentationPlugin> representationInstance,
				Rectangle rectangle, boolean forceEqualScales,
				JuliaSetPoint juliaSetPoint,
				Image previous) {
			this.formulaInstance = formulaInstance;
			this.numberFactoryInstance = numberFactoryInstance;
			this.representationInstance = representationInstance;
			this.rectangle = rectangle;
			this.forceEqualScales = forceEqualScales;
			this.juliaSetPoint = juliaSetPoint;
			this.previous = previous;
		}

		public PluginInstance<NumberFactoryPlugin> getNumberFactoryInstance() {
			return numberFactoryInstance;
		}

		public PluginInstance<FormulaPlugin> getFormulaInstance() {
			return formulaInstance;
		}

		public PluginInstance<RepresentationPlugin> getRepresentationInstance() {
			return representationInstance;
		}

		public NumberFactoryPlugin getNumberFactory() {
			return numberFactoryInstance == null ? null : numberFactoryInstance.getPlugin();
		}

		public FormulaPlugin getFormula() {
			return formulaInstance == null ? null : formulaInstance.getPlugin();
		}

		public RepresentationPlugin getRepresentation() {
			return representationInstance == null ? null :representationInstance.getPlugin();
		}

		public Rectangle getRectangle() {
			return rectangle;
		}

		public boolean getForceEqualScales() {
			return forceEqualScales;
		}

		public JuliaSetPoint getJuliaSetPoint() {
			return juliaSetPoint;
		}

		public Image getPrevious() {
			return previous;
		}

		public Image getNext() {
			return next;
		}
	}

	private Preferences preferences;

	private List<NumberFactoryPlugin> numberFactories;
	private List<FormulaPlugin> formulas;
	private List<RepresentationPlugin> representations;

	private Image currentImage;

	private Representation representation;
	private IntermediateImage iimg;

	private boolean periodicRefreshEnabled = true;
	private boolean halted = false;
	private PreferencesPane preferencesPane;
	private MainWindow mainWindow;
	private ControlWindow previewOwner;
	private JuliaButtonGroup previewCheckBoxGroup;
	private JuliaButtonGroup pinButtonGroup;
	private List<ControlWindow> cwList;
	private List<ControlWindow> cwPool;
	private static final int MAX_CW_POOL_SIZE = 5;

	public static final String APPLY_ICON_KEY = "accept";
	public static final String ZOOM_IN_ICON_KEY = "zoom_in";
	public static final String DISCARD_ICON_KEY = "eraser";
	public static final String PIN_ICON_KEY = "pin_yellow";
	public static final String CLONE_ICON_KEY = "clone";
	public static final String RENAME_ICON_KEY = "document_pencil";
	public static final String HIDE_ICON_KEY = "document_minus";
	public static final String HIDED_ICON_KEY = "minus";
	public static final String DISPOSE_ICON_KEY = "trash_can";
	public static final String SAVE_ICON_KEY = "save";
	public static final String OPEN_ICON_KEY = "open";
	public static final String UNDO_ICON_KEY = "undo";
	public static final String REDO_ICON_KEY = "redo";
	public static final String HALT_ICON_KEY = "pause";
	public static final String RESUME_ICON_KEY = "play";
	public static final String REFRESH_ICON_KEY = "refresh";
	public static final String REFRESH_PERIODICALLY_ICON_KEY = "refresh_clock";
	public static final String EDIT_SELECTION_COLOR_ICON_KEY = "highlighter";
	public static final String EDIT_PREFERENCES_ICON_KEY = "tools";
	private Map<String, Icon> iconCache = new HashMap<>();

	private final Profile profile;
	private LockedFile preferencesFile;
	private final JuliaExecutorService executorService;
	private List<Future<?>> futures;
	private List<Runnable> resumables;
	private Timer refreshTimer;

	private class PeriodicRefreshPerformer implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			refresh();
		}
	}

	private UndoAction undoAction;
	private RedoAction redoAction;
	private HaltAction haltAction;
	private ResumeAction resumeAction;
	private RefreshAction refreshAction;
	private RefreshPeriodicallyAction refreshPeriodicallyAction;
	private EditPreferencesAction editPreferencesAction;
	private EditSelectionColorAction editSelectionColorAction;
	private HideAllAction hideAllAction;
	private ShowAllAction showAllAction;
	private LoadAction loadAction;
	private SaveAction saveAction;
	private ExportAction exportAction;
	private QuitAction quitAction;
	private ShowLogsAction showLogsAction;
	private InstallNewPluginsAction installNewPluginsAction;
	private BrowseJuliaHomePageAction browseJuliaHomePageAction;
	private ShowInfosAction showInfosAction;

	public Icon getIcon(String name) {
		Icon rv = iconCache.get(name);
		if (rv == null) {
			rv = new ImageIcon(getClass().getResource("gui/icons/" + name + ".png"));
			iconCache.put(name, rv);
		}
		return rv;
	}

	private class HaltAction extends AbstractAction {
		public HaltAction() {
			putValue(NAME, "Halt");
			putValue(SHORT_DESCRIPTION, "Halt calculation");
			putValue(MNEMONIC_KEY, KeyEvent.VK_H);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));
			putValue(LARGE_ICON_KEY, getIcon(HALT_ICON_KEY));
		}

		public void actionPerformed(ActionEvent e) {
			halt();
		}
	}

	private class ResumeAction extends AbstractAction {
		public ResumeAction() {
			putValue(NAME, "Resume");
			putValue(SHORT_DESCRIPTION, "Resume calculation");
			putValue(MNEMONIC_KEY, KeyEvent.VK_R);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
			putValue(LARGE_ICON_KEY, getIcon(RESUME_ICON_KEY));
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e) {
			resume();
		}
	}

	private class UndoAction extends AbstractAction {
		public UndoAction() {
			putValue(NAME, "Undo");
			putValue(SHORT_DESCRIPTION, "Undo");
			putValue(MNEMONIC_KEY, KeyEvent.VK_U);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
			putValue(LARGE_ICON_KEY, getIcon(UNDO_ICON_KEY));
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e) {
			Image image = currentImage.previous;
			setCurrentImage(image, null, false);
			setEnabled(currentImage.previous != null);
			redoAction.setEnabled(true);
			setStatusMessage(null, false);
		}
	}

	private class RedoAction extends AbstractAction {
		public RedoAction() {
			putValue(NAME, "Redo");
			putValue(SHORT_DESCRIPTION, "Redo");
			putValue(MNEMONIC_KEY, KeyEvent.VK_R);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
			putValue(LARGE_ICON_KEY, getIcon(REDO_ICON_KEY));
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e) {
			Image image = currentImage.next;
			setCurrentImage(image, null, false);
			undoAction.setEnabled(true);
			setEnabled(currentImage.next != null);
			setStatusMessage(null, false);
		}
	}

	private class RefreshAction extends AbstractAction {
		public RefreshAction() {
			putValue(NAME, "Refresh");
			putValue(SHORT_DESCRIPTION, "Refresh");
			putValue(MNEMONIC_KEY, KeyEvent.VK_R);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
			putValue(LARGE_ICON_KEY, getIcon(REFRESH_ICON_KEY));
		}

		public void actionPerformed(ActionEvent e) {
			refresh();
		}
	}

	private class RefreshPeriodicallyAction extends AbstractAction {
		public RefreshPeriodicallyAction() {
			putValue(NAME, "Refresh periodically");
			putValue(SHORT_DESCRIPTION, "Refresh periodically");
			putValue(MNEMONIC_KEY, KeyEvent.VK_E);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F5, ActionEvent.SHIFT_MASK));
			putValue(LARGE_ICON_KEY, getIcon(REFRESH_PERIODICALLY_ICON_KEY));
			putValue(SELECTED_KEY, periodicRefreshEnabled);
		}

		public void actionPerformed(ActionEvent e) {
			setPeriodicRefreshEnabled((Boolean) getValue(SELECTED_KEY));
		}
	}

	private class EditSelectionColorAction extends AbstractAction implements ChangeListener {

		private JDialog dialog;

		public EditSelectionColorAction() {
			putValue(NAME, "Selection color...");
			putValue(SHORT_DESCRIPTION, "Edit selection color...");
			putValue(MNEMONIC_KEY, KeyEvent.VK_S);
			putValue(LARGE_ICON_KEY, getIcon(EDIT_SELECTION_COLOR_ICON_KEY));
		}

		public void actionPerformed(ActionEvent e) {
			if (dialog == null) {
				final JColorChooser colorChooser = new JColorChooser(mainWindow.getSelectionColor());
				colorChooser.setPreviewPanel(new JPanel());
				colorChooser.getSelectionModel().addChangeListener(this);
				
				JOptionPane optionPane = createOptionPane(colorChooser);
				dialog = createDialog(optionPane, mainWindow, "Selection color", false);
				dialog.setModal(false);
				optionPane.addPropertyChangeListener(new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent e) {
						if (e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY) &&
								e.getNewValue() != null &&
								e.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {
							int newValue = (Integer) e.getNewValue();
							if (newValue == JOptionPane.OK_OPTION) {
								Color color = colorChooser.getColor();
								if (!preferences.getSelectionColor().equals(color)) {
									preferences.setSelectionColor(color);
									savePreferences(dialog);
								}
								mainWindow.setSelectionColor(color);
							}
							dialog.setVisible(false);
							dialog.dispose();
							dialog = null;
							mainWindow.setSelectionColorPreview(null);
						}
					}
				});
				dialog.setVisible(true);
			} else {
				dialog.toFront();
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			ColorSelectionModel csm = (ColorSelectionModel) e.getSource();
			mainWindow.setSelectionColorPreview(csm.getSelectedColor());
		}
	}

	private class EditPreferencesAction extends AbstractAction implements PropertyChangeListener {
		public EditPreferencesAction() {
			putValue(NAME, "Preferences...");
			putValue(SHORT_DESCRIPTION, "Edit preferences...");
			putValue(MNEMONIC_KEY, KeyEvent.VK_P);
			putValue(LARGE_ICON_KEY, getIcon(EDIT_PREFERENCES_ICON_KEY));
		}

		public void actionPerformed(ActionEvent e) {
			if (preferencesPane == null) {
				preferencesPane = new PreferencesPane();
				preferencesPane.addPropertyChangeListener(this);
				preferencesPane.init(Application.this);
			} else {
				preferencesPane.update();
			}

			Preferences oldPreferences = preferences;
			JDialog dialog = createDialog(preferencesPane, mainWindow, "Preferences", false);
			dialog.setVisible(true);
			dialog.dispose();

			Integer value = (Integer) preferencesPane.getValue();
			if (value != null && value.intValue() == JOptionPane.OK_OPTION) {
				mainWindow.setSelectionColor(preferences.getSelectionColor());

				if (refreshTimer != null) {
					refreshTimer.setInitialDelay(preferences.getRefreshDelay());
					refreshTimer.setDelay(preferences.getRefreshDelay());
				}

				if ((oldPreferences.getImageHeight() != preferences.getImageHeight() ||
						oldPreferences.getImageWidth() != preferences.getImageWidth()) &&
					(iimg.getWidth() != preferences.getImageWidth() ||
						iimg.getHeight() != preferences.getImageHeight())) {
					recompute(true);
					setStatusMessage(null, false);
				} else {
					mainWindow.setTransparency(preferences.getTransparency());
				}
			} else {
				preferencesPane.cancel();
			}
		}
		
		@Override
		public void propertyChange(PropertyChangeEvent e) {
			if (e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY) &&
					e.getNewValue() != null &&
					e.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {
				int newValue = (Integer) e.getNewValue();
				if (newValue == JOptionPane.OK_OPTION) {
					preferences = preferencesPane.commit();
					savePreferences(preferencesPane);
				}
				Window w = getWindowForComponent(preferencesPane);
				if (w != null) {
					w.setVisible(false);
				}
			}
		}
	}

	private class HideAllAction extends AbstractAction {
		public HideAllAction() {
			putValue(NAME, "Hide all");
			putValue(SHORT_DESCRIPTION, "Hide all control windows");
			putValue(MNEMONIC_KEY, KeyEvent.VK_H);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.CTRL_MASK));
		}

		public void actionPerformed(ActionEvent e) {
			for (ControlWindow cw : cwList) {
				cw.setVisible(false);
			}
		}
	}

	private class ShowAllAction extends AbstractAction {
		public ShowAllAction() {
			putValue(NAME, "Show all");
			putValue(SHORT_DESCRIPTION, "Show all control windows");
			putValue(MNEMONIC_KEY, KeyEvent.VK_S);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
		}

		public void actionPerformed(ActionEvent e) {
			for (ControlWindow cw : cwList) {
				show(cw);
			}
		}
	}

	private class LoadAction extends AbstractAction {
		public LoadAction() {
			putValue(NAME, "Open...");
			putValue(SHORT_DESCRIPTION, "Open...");
			putValue(MNEMONIC_KEY, KeyEvent.VK_O);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		}

		public void actionPerformed(ActionEvent e) {
			JFileChooser fc = new JFileChooser();
			fc.setDialogType(JFileChooser.OPEN_DIALOG);
			fc.setFileFilter(new FileNameExtensionFilter("Julia image (*.jim)", "jim"));
			int rv = fc.showDialog(mainWindow, null);
			if (rv == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				Printer errorOutput = Printer.newStringPrinter();
				LoadWorker productionLoader = new LoadWorker(file, Application.this, errorOutput, true) {
					@Override
					protected void processResult(Void result) {
						if (hasErrors()) {
							MessagePane.showErrorMessage(getBlockingDialog(),
									"Julia",
									"Error(s) encountered during loading. See the details.",
									getErrorOutput().toString());
						}

						if (hasHeader()) {
							PluginInstance<NumberFactoryPlugin> numberFactoryInstance = getNumberFactoryInstance();
							if (numberFactoryInstance == null) {
								NumberFactoryPlugin numberFactory =
										PluginSelectionPane.showSelectionPane(getBlockingDialog(), "Julia", null, getNumberFactories());
								numberFactoryInstance = new PluginInstance<NumberFactoryPlugin>(numberFactory);
							}

							Image header = new Image(numberFactoryInstance,
									getFormulaInstance(),
									getRepresentationInstance(),
									getRectangle(),
									getForceEqualScales(),
									getJuliaSetPoint());
							
							load(header, getIntermediateImage(), false);

							setStatusMessage(null, false);
						}
					}
				};

				executorService.execute(productionLoader);
				productionLoader.block(mainWindow, "Reading from " + file + ":", "number factory...");
			}
		}
	}

	private class SaveAction extends AbstractAction {
		public SaveAction() {
			putValue(NAME, "Save...");
			putValue(SHORT_DESCRIPTION, "Save...");
			putValue(MNEMONIC_KEY, KeyEvent.VK_S);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		}

		public void actionPerformed(ActionEvent e) {
			JFileChooser fc = new JFileChooser();
			fc.setDialogType(JFileChooser.SAVE_DIALOG);
			fc.setFileFilter(new FileNameExtensionFilter("Julia image (*.jim)", "jim"));
			int rv = showSaveDialog(fc, mainWindow);
			if (rv == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				SaveWorker saveWorker = new SaveWorker(file, currentImage, iimg);
				executorService.execute(saveWorker);
				saveWorker.block(mainWindow, "Writing to " + file + ":", "number factory...");
			}
		}
	}

	private class ExportAction extends AbstractAction {
		public ExportAction() {
			putValue(NAME, "Export...");
			putValue(SHORT_DESCRIPTION, "Export...");
			putValue(MNEMONIC_KEY, KeyEvent.VK_E);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
			putValue(ACTION_COMMAND_KEY, "ExportImage");
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand() == "ExportImage") {
				JFileChooser fc = new JFileChooser();
				fc.setAcceptAllFileFilterUsed(false);
				String[] fileSuffixes = ImageIO.getWriterFileSuffixes();
				Set<String> duplicates = new HashSet<>();
				for (String fileSuffix : fileSuffixes) {
					switch (fileSuffix) {
					case "jpg": if (duplicates.add("jpeg")) fc.addChoosableFileFilter(
							new FileNameExtensionFilter("Joint Photographic Experts Group (*.jpg; *.jpeg)", fileSuffix, "jpeg")); break;
					case "jpeg": if (duplicates.add("jpeg")) fc.addChoosableFileFilter(
							new FileNameExtensionFilter("Joint Photographic Experts Group (*.jpeg; *.jpg)", fileSuffix, "jpg")); break;
					case "png": fc.addChoosableFileFilter(
							new FileNameExtensionFilter("Portable Network Graphics (*.png)", fileSuffix)); break;
					case "gif": fc.addChoosableFileFilter(
							new FileNameExtensionFilter("Graphics Interchange Format (*.gif)", fileSuffix)); break;
					case "bmp": fc.addChoosableFileFilter(
							new FileNameExtensionFilter("Windows bitmap (*.bmp)", fileSuffix)); break;
					case "wbmp": fc.addChoosableFileFilter(
							new FileNameExtensionFilter("WAP bitmap (*.wbmp)", fileSuffix)); break;
					case "tif": if (duplicates.add("tiff")) fc.addChoosableFileFilter(
							new FileNameExtensionFilter("Tagged Image File Format (*.tif; *.tiff)", fileSuffix, "tiff")); break;
					case "tiff": if (duplicates.add("tiff")) fc.addChoosableFileFilter(
							new FileNameExtensionFilter("Tagged Image File Format (*.tiff; *.tif)", fileSuffix, "tif")); break;
					default: fc.addChoosableFileFilter(
							new FileNameExtensionFilter(fileSuffix.toUpperCase() + " format " + "(*." + fileSuffix + ")", fileSuffix)); break;
					}
				}
				fc.setDialogTitle("Export");
				fc.setApproveButtonText("Export");
				fc.setApproveButtonMnemonic(KeyEvent.VK_E);
				fc.addActionListener(this);
				JDialog dialog = createDialog(fc, mainWindow);
				fc.rescanCurrentDirectory();
				dialog.setVisible(true);
				dialog.getContentPane().removeAll();
				dialog.dispose();
			} else if (e.getSource() instanceof JFileChooser) {
				JFileChooser fc = (JFileChooser) e.getSource();
				Window w = getWindowForComponent(fc);
				if (e.getActionCommand() == JFileChooser.APPROVE_SELECTION) {
					File file = fc.getSelectedFile();
					FileNameExtensionFilter fileFilter = (FileNameExtensionFilter) fc.getFileFilter();
					String extension = fileFilter.getExtensions()[0];
					if (!fileFilter.accept(file)) {
						file = new File(file.getPath() + '.' + extension);
					}
					if (file.exists()) {
						int rv = JOptionPane.showConfirmDialog(fc,
								"File " + file + " already exists.\n" +
								"Do you want to overwrite it?",
								"Julia",
								JOptionPane.YES_NO_OPTION);
						if (rv != JOptionPane.YES_OPTION) {
							return;
						}
					}
	
					ImageWriter imageWriter = null;
					Iterator<ImageWriter> i = ImageIO.getImageWritersBySuffix(extension);
					for ( ; i.hasNext(); ) {
						ImageWriter iw = i.next();
						ImageWriterSpi spi = iw.getOriginatingProvider();
						if (spi != null && spi.canEncodeImage(mainWindow.getFinalImage())) {
							imageWriter = iw;
							break;
						}
					}
		
					int requiredTransparency = mainWindow.getConsumer().getTransparency();
					if (imageWriter == null) {
						String message = "Current image cannot be encoded in this format.";
						if (preferences.getTransparency() > requiredTransparency) {
							String[] transparencyStrings = new String[] { "Opaque", "Bitmask", "Translucent" };
							message += " Try again after setting the transparency to <i>"
									+ transparencyStrings[requiredTransparency - 1] + "</i> (go to "
									+ "<i>Edit</i>&#8594;<i>Preferences...</i>&#8594;<i>General</i>).";
						} else {
							message += "<br>Please select another one.";
						}
						MessagePane.showErrorMessage(fc,
								"Export image",
								message,
								null);
						return;
					} else if (extension.equals("gif") &&
							preferences.getTransparency() == Transparency.TRANSLUCENT &&
							requiredTransparency == Transparency.TRANSLUCENT) {
						MessagePane.showErrorMessage(fc,
								"Export image",
								"Current image cannot be encoded in this format.<br>Please select another one.",
								null);
						return;
					} else if (extension.matches("jpg|jpeg") &&
							preferences.getTransparency() > Transparency.OPAQUE) {
						String message = "Current image cannot be encoded in this format.";
						if (requiredTransparency > Transparency.OPAQUE) {
							message += "<br>Please select another one.";
						} else {
							message += " Try again after setting the transparency to <i>Opaque</i> (go to "
									+ "<i>Edit</i>&#8594;<i>Preferences...</i>&#8594;<i>General</i>).";
						}
						MessagePane.showErrorMessage(fc,
								"Export image",
								message,
								null);
						return;
					}
			
					ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();
					if (imageWriteParam.canWriteCompressed() || imageWriteParam.canWriteProgressive()) {
						ImageWriteParamPanel iwpPanel = new ImageWriteParamPanel();
						int rv = iwpPanel.showDialog(fc, imageWriter);
						if (rv == JOptionPane.OK_OPTION) {
							imageWriteParam = iwpPanel.getImageWriteParam();
		 				} else {
		 					return;
		 				}
					}
				
					w.setVisible(false);

					ExportWorker exportWorker = new ExportWorker(mainWindow.getFinalImage(),
							file,
							imageWriter,
							imageWriteParam,
							mainWindow);
					executorService.execute(exportWorker);
					exportWorker.block(mainWindow, "Writing to " + file + "...", null);
		
				} else if (e.getActionCommand() == JFileChooser.CANCEL_SELECTION) {
					w.setVisible(false);
				}
			}
		}
	}

	private class QuitAction extends AbstractAction {
		public QuitAction() {
			putValue(NAME, "Quit");
			putValue(SHORT_DESCRIPTION, "Quit");
			putValue(MNEMONIC_KEY, KeyEvent.VK_Q);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
		}

		public void actionPerformed(ActionEvent e) {
			quit();
		}
	}

	private class BrowseJuliaHomePageAction extends AbstractAction {
		public BrowseJuliaHomePageAction() {
			putValue(NAME, "Julia home page");
			putValue(SHORT_DESCRIPTION, "Julia home page");
			putValue(MNEMONIC_KEY, KeyEvent.VK_J);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			visitPage(URI.create("http://mbilotta.altervista.org/"), mainWindow);
		}
	}

	private class ShowInfosAction extends AbstractAction {
		public ShowInfosAction() {
			putValue(NAME, "About Julia");
			putValue(SHORT_DESCRIPTION, "About Julia");
			putValue(MNEMONIC_KEY, KeyEvent.VK_A);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			AboutPane.showDialog(mainWindow, "About Julia", Application.this);
		}
	}

	private LogPane logPane;

	private class ShowLogsAction extends AbstractAction {
		public ShowLogsAction() {
			putValue(NAME, "Show logs");
			putValue(SHORT_DESCRIPTION, "Show logs");
			putValue(MNEMONIC_KEY, KeyEvent.VK_L);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
		}

		public void actionPerformed(ActionEvent e) {
			if (logPane == null) {
				logPane = new LogPane();
				logPane.init(preferences.getMaxLogLength(), 
						Collections.nCopies(iimg.getNumOfProducers(), (Production.Producer) null));
			}
			logPane.showDialog(mainWindow);
		}
	}

	private class InstallNewPluginsAction extends AbstractAction {
		public InstallNewPluginsAction() {
			putValue(NAME, "Install new plugins...");
			putValue(SHORT_DESCRIPTION, "Install new plugins...");
			putValue(MNEMONIC_KEY, KeyEvent.VK_N);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			installNewPlugins(mainWindow);
		}
	}

	private void installNewPlugins(Component parent) {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Select a package");
		fc.setApproveButtonText("Install");
		fc.setApproveButtonMnemonic(KeyEvent.VK_I);
		fc.setFileFilter(new FileNameExtensionFilter("Julia plugin package (*.jup)", "jup"));
		int rv = fc.showDialog(parent, null);
		if (rv == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			Printer printer;
			try {
				if (preferencesFile == null) {
					preferencesFile = profile.lock();
				}
			} catch (IOException e) {
				MessagePane.showErrorMessage(
						parent,
						"Julia",
						"Could not lock profile \"" + profile.getRootDirectory() + "\". See details.",
						e);
				return;
			}

			try {
				printer = Printer.newPrinter(
						Files.newBufferedWriter(
								profile.getInstallerOutputFile(),
								Charset.defaultCharset(),
								StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND),
						false);
			} catch (IOException e) {
				MessagePane.showErrorMessage(
						parent,
						"Julia",
						"Could not open/create log file \"" + profile.getInstallerOutputFile().getFileName()
						+ "\". See details.",
						e);
				printer = Printer.newStringPrinter();
			}

			Profile.PluginInstaller installer = profile.new PluginInstaller(file, printer);
			executorService.execute(installer);
			installer.block(parent, "Reading from " + file + ":", "Opening...");
		}
	}

	Application(Loader loader) {
		numberFactories = loader.getAvailableNumberFactories();
		formulas = loader.getAvailableFormulas();
		representations = loader.getAvailableRepresentations();
		profile = loader.getProfile();
		preferences = loader.getPreferences();
		preferencesFile = loader.getPreferencesFile();
		executorService = loader.getExecutorService();
	}

	public void preview(ControlWindow cw, Parameter<?> parameter, Object value) {
		assert cw != null;
		RepresentationPlugin plugin = getRepresentation();
		if (previewOwner == cw && plugin == previewOwner.getRepresentation()) {
			assert plugin.isPreviewable(parameter);
			Method setter = parameter.getSetterMethod();
			try {
				setter.invoke(representation, value);
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
				return;
			}

			Consumer consumer = representation.createConsumer(iimg, mainWindow.getConsumer(), true);
			mainWindow.refresh(consumer);
		}
	}

	public void setPreviewOwner(ControlWindow cw) {
		if (previewOwner != cw) {
			ControlWindow oldPreviewOwner = previewOwner;
			previewOwner = cw;
			PluginInstance<RepresentationPlugin> oldPreview =
					oldPreviewOwner != null && oldPreviewOwner.getRepresentation() == getRepresentation() ?
					oldPreviewOwner.getRepresentationPreviewInstance() : getRepresentationInstance();
			PluginInstance<RepresentationPlugin> newPreview =
					previewOwner != null && previewOwner.getRepresentation() == getRepresentation() ?
					previewOwner.getRepresentationPreviewInstance() : getRepresentationInstance();
			if (mustUpdatePreview(oldPreview, newPreview)) {
				try {
					representation = (Representation) newPreview.create();
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
					return;
				}

				Consumer consumer = representation.createConsumer(iimg, mainWindow.getConsumer(), true);
				mainWindow.refresh(consumer);
			}
		}
	}

	public void handleControlWindowClosing(ControlWindow cw) {
		if (cwList.size() == 1) {
			cw.setVisible(false);
			return;
		}

		switch (preferences.getDefaultCloseBehaviour()) {
		case ASK: {
			JCheckBox rememberMyDecisionCheckBox = new JCheckBox("Remember my decision", false);
			rememberMyDecisionCheckBox.setBorder(
					BorderFactory.createCompoundBorder(
							BorderFactory.createEmptyBorder(4, 0, 0, 0),
							rememberMyDecisionCheckBox.getBorder()));
			Object[] message = new Object[] {
					"Would you like to hide or dispose this window?\nChoose \"Hide\" if you want to bring it back later.",
					rememberMyDecisionCheckBox };
			Object[] options = new Object[] { "Dispose", "Hide", "Cancel" };
			int result = JOptionPane.showOptionDialog(
					cw,
					message,
					"Julia",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[1]);
			switch (result) {
			case JOptionPane.CANCEL_OPTION:
			case JOptionPane.CLOSED_OPTION: return;
			case JOptionPane.YES_OPTION: {
				disposeControlWindow(cw);
				if (rememberMyDecisionCheckBox.isSelected()) {
					preferences.setDefaultCloseBehaviour(DefaultCloseBehaviour.DISPOSE);
				}
			} break;
			case JOptionPane.NO_OPTION: {
				cw.setVisible(false);
				if (rememberMyDecisionCheckBox.isSelected()) {
					preferences.setDefaultCloseBehaviour(DefaultCloseBehaviour.HIDE);
				}
			} break;
			}
			if (rememberMyDecisionCheckBox.isSelected()) {
				savePreferences(null);
			}
			
		} break;
		case HIDE: cw.setVisible(false); break;
		case DISPOSE: disposeControlWindow(cw); break;
		default: throw new AssertionError(preferences.getDefaultCloseBehaviour());
		}
	}

	public void addSelectionPreviewTo(JColorChooser colorChooser) {
		final ImagePanel imagePanel = new ImagePanel(
				mainWindow.getFinalImage(),
				colorChooser.getColor(),
				representation.createConsumer(iimg, mainWindow.getConsumer(), true));
		colorChooser.getSelectionModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				ColorSelectionModel model = (ColorSelectionModel) e.getSource();
				imagePanel.setSelectionColor(model.getSelectedColor());
			}
		});
		JScrollPane scrollPane = new JScrollPane(imagePanel);
		int maxWidth = colorChooser.getPreferredSize().width;
		if (colorChooser.getPreviewPanel().getParent().getName().equals("ColorChooser.previewPanelHolder")) {
			Insets insets = colorChooser.getPreviewPanel().getParent().getInsets();
			maxWidth -= insets.left + insets.right;
		}
		Dimension preferredSize = scrollPane.getPreferredSize();
		if (preferredSize.width > maxWidth) {
			preferredSize.width = maxWidth;
		}
		if (preferredSize.height > 100) {
			preferredSize.height = 100;
			preferredSize.width += scrollPane.getVerticalScrollBar().getPreferredSize().width;
			if (preferredSize.width > maxWidth) {
				preferredSize.width = maxWidth;
			}
		}
		scrollPane.setPreferredSize(preferredSize);
		colorChooser.setPreviewPanel(scrollPane);
	}

	private static boolean mustUpdatePreview(PluginInstance<RepresentationPlugin> oldPreview, PluginInstance<RepresentationPlugin> newPreview) {
		for (Parameter<?> parameter : oldPreview.getPlugin().getParameters()) {
			if (oldPreview.getPlugin().isPreviewable(parameter) &&
				!oldPreview.getParameterValue(parameter).equals(newPreview.getParameterValue(parameter))) {
				return true;
			}
		}
		return false;
	}

	public ControlWindow getPreviewOwner() {
		return previewOwner;
	}

	public ControlWindow createControlWindow(String title) {
		ControlWindow rv = cwPool.isEmpty() ?
				new ControlWindow(this) : cwPool.remove(0);
		rv.setTitle(title);
		rv.getItemInWindowMenu().setMnemonic(KeyEvent.VK_0 + cwList.size() % 10);
		if (cwList.size() == 1) {
			cwList.get(0).setDisposeButtonEnabled(true);
		}
		cwList.add(rv);
		mainWindow.addToWindowMenu(rv.getItemInWindowMenu());
		return rv;
	}

	public void disposeControlWindow(ControlWindow cw) {
		cw.dispose();
		if (cw == previewOwner) {
			setPreviewOwner(null);
		}
		cw.uninstall();
		boolean found = false;
		for (Iterator<ControlWindow> i = cwList.iterator(); i.hasNext(); ) {
			if (found) {
				JMenuItem menuItem = i.next().getItemInWindowMenu();
				int mnemonic = menuItem.getMnemonic();
				if (mnemonic == KeyEvent.VK_0) {
					menuItem.setMnemonic(KeyEvent.VK_9);
				} else {
					menuItem.setMnemonic(mnemonic - 1);
				}
			} else if (i.next() == cw) {
				found = true;
				i.remove();
				mainWindow.removeFromWindowMenu(cw.getItemInWindowMenu());
			}
		}
		assert found;
		if (cwList.size() == 1) {
			cwList.get(0).setDisposeButtonEnabled(false);
		}
		if (cwPool.size() < MAX_CW_POOL_SIZE) {
			cwPool.add(cw);
		}
	}

	public List<ControlWindow> getControlWindows() {
		return Collections.unmodifiableList(cwList);
	}

	public Profile getProfile() {
		return profile;
	}

	private File redirectFile;

	public void visitPage(URI uri, Component src) {
		if (preferences.isJavaDesktopInteractionEnabled()) {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				if (uri.getRawFragment() != null) {
					try {
						if (redirectFile == null) {
							redirectFile = File.createTempFile("julia", ".html");
							redirectFile.deleteOnExit();
						}
						PrintWriter out = new PrintWriter(redirectFile);
						out.print("<!DOCTYPE html>"
								+ "<html>"
								+ "<head><meta http-equiv=\"refresh\" content=\"0;url=");
						out.print(uri);
						out.print("\"/></head></html>");
						out.close();
						uri = redirectFile.toURI();
					} catch (IOException e) {
						if (redirectFile == null) {
							MessagePane.showErrorMessage(src, "Julia", "Error trying to create a temporary file.", e);
						} else {
							MessagePane.showWriteErrorMessage(src, redirectFile, e);
						}
					}
				}
				
				try {
					Desktop.getDesktop().browse(uri);
				} catch (IOException e) {
					MessagePane.showErrorMessage(src, "Julia", "Error trying to launch the default browser. See the details.", e);
				}
			} else {
				MessagePane.showErrorMessage(src, "Julia", "Desktop interaction not supported by the Java platform. Could not launch the default browser.", uri);
			}
		} else {
			String command = preferences.getBrowserCommand();
			if (command != null && !command.isEmpty()) {
				try {
					Runtime.getRuntime().exec(command + " " + uri);
				} catch (IOException e) {
					MessagePane.showErrorMessage(src, "Julia", "Error trying to launch external program. See the details.", e);
				}
			}
		}
	}

	private JFrame getNextVisibleJFrame(JFrame frame) {
		boolean found = mainWindow == frame;
		for (ControlWindow cw : cwList) {
			if (found && cw.isVisible()) {
				return cw;
			}
			if (cw == frame) {
				found = true;
			}
		}
		
		return mainWindow;
	}

	private JFrame getPreviousVisibleJFrame(JFrame frame) {
		JFrame rv = mainWindow;
		for (ControlWindow cw : cwList) {
			if (cw == frame) {
				break;
			}
			if (cw.isVisible()) {
				rv = cw;
			}
		}
	
		return rv;
	}

	public void refresh() {
		if (resumableCount + finishedCount < futures.size()) {
			mainWindow.refresh();
			logPane.refresh();
		}
	}

	public void setPeriodicRefreshEnabled(boolean flag) {
		if (flag != periodicRefreshEnabled) {
			if (resumableCount + finishedCount < futures.size()) {
				if (flag) {
					mainWindow.refresh();
					logPane.refresh();
					refreshTimer.start();
				} else {
					refreshTimer.stop();
				}
			}
			periodicRefreshEnabled = flag;
		}
	}

	public boolean isPeriodicRefreshEnabled() {
		return periodicRefreshEnabled;
	}

	public void setStatusMessage(String message, boolean highlight) {
		mainWindow.setStatusMessage(message, highlight);
	}

	public int getCurrentImageTransparency() {
		return mainWindow.getConsumer().getTransparency();
	}

	public Dimension getCurrentImageSize() {
		return mainWindow.getImageSize();
	}

	public Dimension getImagePanelSize() {
		return mainWindow.getImagePanelSize();
	}

	public void apply(ControlWindow source, boolean zoomIn) {
		assert cwList.contains(source);
		if (!source.isConsistent()) {
			setStatusMessage("Highlighted fields contain an illegal/incorrect value. Please review your input.", true);
			return;
		}

		if (zoomIn && !mainWindow.hasSelection()) {
			setStatusMessage("Please select an area of the image first.", true);
			return;
		}

		Image oldCurrentImage = currentImage;
		Image oldNextImage = currentImage.next;
		Image image = source.getImage();
		image.previous = oldCurrentImage;

		setCurrentImage(image, source, zoomIn);
		if (currentImage != oldCurrentImage) {
			oldCurrentImage.next = image;
			if (oldNextImage != null) {
				oldNextImage.previous = null;
				for (Image nextImage = oldNextImage.next; nextImage != null; nextImage = nextImage.next) {
					nextImage.previous.next = null;
					nextImage.previous = null;
				}
			}
		}

		undoAction.setEnabled(currentImage.previous != null);
		redoAction.setEnabled(currentImage.next != null);
		setStatusMessage(null, false);
	}

	private boolean mustRecomputeRepresentation(PluginInstance<RepresentationPlugin> representationInstance) {
		assert getRepresentation() == representationInstance.getPlugin();
		RepresentationPlugin plugin = getRepresentation();
		PluginInstance<RepresentationPlugin> currentInstance = getRepresentationInstance();
		for (Parameter<?> parameter : plugin.getParameters()) {
			Object currentValue = currentInstance.getParameterValue(parameter);
			if (!plugin.isPreviewable(parameter) &&
					!currentValue.equals(representationInstance.getParameterValue(parameter)))
				return true;
		}
		
		return false;
	}

	private void savePreferences(Component parent) {
		try {
			ObjectOutputStream out = preferencesFile.writeObjectsTo();
			out.writeObject(preferences);
			out.flush();
		} catch (IOException e) {
			MessagePane.showWriteErrorMessage(parent, preferencesFile.getPath(), e);
		}
	}
	
	private void cancelCurrentProduction() {
		if (productionObserver != null) {
			productionObserver.override();
			for (int i = 0; i < futures.size(); i++) {
				Future<?> future = futures.get(i);
				if (future != null) {
					future.cancel(true);
				}
			}
			productionObserver = null;
			halted = false;
		}
	}

	private static CoordinateTransform createCoordinateTransform(int imgWidth, int imgHeight,
			Image header,
			NumberFactory nf) {
		Rectangle rect = header.getRectangle().normalize();
		if (header.getForceEqualScales()) {
			Real re0 = nf.valueOf(rect.getRe0());
			Real im0 = nf.valueOf(rect.getIm0());
			Real re1 = nf.valueOf(rect.getRe1());
			Real im1 = nf.valueOf(rect.getIm1());
			Real width = re1.minus(re0);
			Real height = im0.minus(im1);
			Real srcRatio = height.dividedBy(width);
			Real dstRatio = nf.valueOf(imgHeight).dividedBy(imgWidth);
			Real scaleRe, scaleIm;
			int result = srcRatio.compareTo(dstRatio);
			if (result > 0) {
				scaleRe = height.dividedBy(imgHeight);
				scaleIm = scaleRe.negate();
				Real centerRe = re0.plus(re1).dividedBy(2);
				re0 = centerRe.minus(scaleRe.times((imgWidth - 1) / 2));
			} else if (result < 0) {
				scaleRe = width.dividedBy(imgWidth);
				scaleIm = scaleRe.negate();
				Real centerIm = im0.plus(im1).dividedBy(2);
				im0 = centerIm.minus(scaleIm.times((imgHeight - 1) / 2));
			} else {
				scaleRe = width.dividedBy(imgWidth);
				scaleIm = scaleRe.negate();
			}

			return new CoordinateTransform(re0, im0,
					nf.zero(), nf.zero(),
					scaleRe, scaleIm);
		}
		
		return new CoordinateTransform(nf.valueOf(rect.getRe0()), nf.valueOf(rect.getIm0()),
				nf.valueOf(rect.getRe1()), nf.valueOf(rect.getIm1()),
				nf.zero(), nf.zero(),
				nf.valueOf(imgWidth), nf.valueOf(imgHeight));
	}

	private void load(Image header, IntermediateImage iimg, boolean runProduction) {
		Image oldCurrentImage = currentImage;
		Image oldNextImage = currentImage != null ? currentImage.next : null;
		header.previous = oldCurrentImage;

		PluginInstance<RepresentationPlugin> representationInstance = header.getRepresentationInstance();
		if (previewOwner != null) {
			PluginInstance<RepresentationPlugin> representationPreviewInstance = previewOwner.getRepresentationPreviewInstance();
			if (representationPreviewInstance != null && representationPreviewInstance.getPlugin() == representationInstance.getPlugin() && previewOwner.isPinned()) {
				representationPreviewInstance = new PluginInstance<>(representationPreviewInstance);
				RepresentationPlugin plugin = representationInstance.getPlugin();
				for (Parameter<?> parameter : plugin.getParameters()) {
					if (!plugin.isPreviewable(parameter)) {
						representationPreviewInstance.setParameterValue(
								parameter,
								representationInstance.getParameterValue(parameter));
					}
				}
				representationInstance = representationPreviewInstance;				
			}
		}

		NumberFactory numberFactory;
		Formula formula;
		Representation representation;
		Representation representationPreview;
		try {
			numberFactory = (NumberFactory) header.getNumberFactoryInstance().create();
			formula = (Formula) header.getFormulaInstance().create(numberFactory);
			representation = (Representation) representationInstance.create(numberFactory);
			representationPreview = (Representation) representationInstance.create();
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
			return;
		}

		if (iimg == null) {
			IntermediateImage oldIntermediateImage = this.iimg;
			int imgWidth, imgHeight;
			if (oldIntermediateImage != null) {
				imgWidth = oldIntermediateImage.getWidth();
				imgHeight = oldIntermediateImage.getHeight();
			} else {
				imgWidth = preferences.getImageWidth();
				imgHeight = preferences.getImageHeight();
			}
			iimg = representation.createIntermediateImage(imgWidth, imgHeight,
					Math.min(Runtime.getRuntime().availableProcessors(), preferences.getNumOfProducerThreads()));
		}

		int imgWidth = iimg.getWidth();
		int imgHeight = iimg.getHeight();
		CoordinateTransform coordinateTransform = createCoordinateTransform(imgWidth, imgHeight, header, numberFactory);

		JuliaSetPoint juliaSetPoint = header.getJuliaSetPoint();
		Complex cJuliaSetPoint = juliaSetPoint != null ?
				numberFactory.valueOf(juliaSetPoint.getRe(), juliaSetPoint.getIm()) : null;

		Consumer consumer = representation.createConsumer(iimg, mainWindow.getConsumer(), false);

		if (iimg.isComplete()) {
			mainWindow.consumeAndReset(imgWidth, imgHeight,
					preferences.getTransparency(),
					consumer);
			refreshTimer.stop();
			cancelCurrentProduction();
			mainWindow.resetTimer();

			futures = Collections.nCopies(iimg.getNumOfProducers(), null);
			resumables = Collections.nCopies(iimg.getNumOfProducers(), null);
			finishedCount = iimg.getNumOfProducers();
			resumableCount = 0;

			haltAction.setEnabled(false);
			resumeAction.setEnabled(false);
		} else {
			Production production = representation.createProduction(
					iimg, numberFactory, formula, coordinateTransform, cJuliaSetPoint);

			if (preferences.isLoggingEnabled())
				production.setMaxLogLength(preferences.getMaxLogLength());

			int numOfProducers = iimg.getNumOfProducers();
			List<Production.Producer> producers = new ArrayList<>(numOfProducers);
			for (int i = 0; i < numOfProducers; i++) {
				Progress progress = iimg.getProgressOf(i);
				if (progress.isFinalValue()) {
					producers.add(null);
				} else {
					producers.add(production.createProducer(progress));
				}
			}

			ProductionObserver productionObserver = new ProductionObserver();
			List<Future<?>> futures = new ArrayList<Future<?>>(Collections.nCopies(numOfProducers, (Future<?>) null));
			List<Runnable> resumables = new ArrayList<>(numOfProducers);
			int finishedCount = 0;
			int resumableCount = 0;
			for (int i = 0; i < numOfProducers; i++) {
				Production.Producer producer = producers.get(i);
				if (producer == null) {
					resumables.add(null);
					finishedCount++;
				} else {
					resumables.add(new IndexedProducer(producer, i));
					resumableCount++;
				}
			}

			mainWindow.consumeAndReset(imgWidth, imgHeight,
					preferences.getTransparency(),
					consumer);
			refreshTimer.stop();
			cancelCurrentProduction();
			mainWindow.resetTimer();

			this.productionObserver = productionObserver;
			this.futures = futures;
			this.resumables = resumables;
			this.finishedCount = finishedCount;
			this.resumableCount = resumableCount;
			
			if (logPane == null)
				logPane = new LogPane();
			logPane.init(preferences.getMaxLogLength(), producers);

			if (runProduction) {
				resume();
			} else {
				halted = true;
				haltAction.setEnabled(false);
				resumeAction.setEnabled(true);
			}
		}
		
		this.currentImage = header;
		this.iimg = iimg;
		this.representation = representationPreview;

		for (ControlWindow cw : cwList) {
			if (cw.isPinned()) {
				cw.markEdits();
			} else {
				cw.discardEdits();
			}
		}

		if (oldCurrentImage != null) {
			oldCurrentImage.next = header;
			if (oldNextImage != null) {
				oldNextImage.previous = null;
				for (Image nextImage = oldNextImage.next; nextImage != null; nextImage = nextImage.next) {
					nextImage.previous.next = null;
					nextImage.previous = null;
				}
			}
		}

		undoAction.setEnabled(currentImage.previous != null);
		redoAction.setEnabled(currentImage.next != null);
	}

	private void recompute(boolean resize) {
		PluginInstance<RepresentationPlugin> representationInstance = getRepresentationInstance();
		if (previewOwner != null) {
			PluginInstance<RepresentationPlugin> representationPreviewInstance = previewOwner.getRepresentationPreviewInstance();
			if (representationInstance != null && representationPreviewInstance.getPlugin() == representationInstance.getPlugin()) {
				representationInstance = representationPreviewInstance;
			}
		}

		NumberFactory numberFactory;
		Formula formula;
		Representation representation;
		try {
			numberFactory = (NumberFactory) getNumberFactoryInstance().create();
			formula = (Formula) getFormulaInstance().create(numberFactory);
			representation = (Representation) representationInstance.create(numberFactory);
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
			return;
		}

		int imgWidth, imgHeight;
		if (resize) {
			imgWidth = preferences.getImageWidth();
			imgHeight = preferences.getImageHeight();
		} else {
			imgWidth = iimg.getWidth();
			imgHeight = iimg.getHeight();
		}
		CoordinateTransform coordinateTransform = createCoordinateTransform(imgWidth, imgHeight, currentImage, numberFactory);

		JuliaSetPoint juliaSetPoint = getJuliaSetPoint();
		Complex cJuliaSetPoint = juliaSetPoint != null ?
				numberFactory.valueOf(juliaSetPoint.getRe(), juliaSetPoint.getIm()) : null;

		IntermediateImage iimg = representation.createIntermediateImage(imgWidth, imgHeight,
				Math.min(Runtime.getRuntime().availableProcessors(), preferences.getNumOfProducerThreads()));
		Production production = representation.createProduction(
				iimg, numberFactory, formula, coordinateTransform, cJuliaSetPoint);
		Consumer consumer = representation.createConsumer(iimg, mainWindow.getConsumer(), false);

		run(production);

		mainWindow.reset(imgWidth, imgHeight, preferences.getTransparency(), consumer);
		if (periodicRefreshEnabled) {
			refreshTimer.restart();
		}
		haltAction.setEnabled(true);
		resumeAction.setEnabled(false);

		this.iimg = iimg;
	}

	private void setCurrentImage(Image image, ControlWindow source, boolean zoomIn) {

		PluginInstance<NumberFactoryPlugin> numberFactoryInstance = image.getNumberFactoryInstance();
		boolean numberFactoryChanged = numberFactoryInstance != getNumberFactoryInstance();

		PluginInstance<FormulaPlugin> formulaInstance = image.getFormulaInstance();
		boolean formulaChanged = formulaInstance != getFormulaInstance();

		PluginInstance<RepresentationPlugin> representationInstance = image.getRepresentationInstance();
		boolean representationChanged = representationInstance != getRepresentationInstance();
		boolean representationPluginChanged = representationChanged &&
				(getRepresentationInstance() == null ||
				getRepresentationInstance().getPlugin() != representationInstance.getPlugin());

		Rectangle rectangle = image.getRectangle();
		boolean forceEqualScales = image.getForceEqualScales();
		boolean rectangleChanged = !zoomIn && (rectangle != getRectangle() || forceEqualScales != getForceEqualScales());

		JuliaSetPoint juliaSetPoint = image.getJuliaSetPoint();
		boolean juliaSetPointChanged = juliaSetPoint != getJuliaSetPoint();
		
		boolean previewing = false;
		if (previewOwner != null) {
			PluginInstance<RepresentationPlugin> representationPreviewInstance = previewOwner.getRepresentationPreviewInstance();
			if (representationPreviewInstance != null && representationPreviewInstance.getPlugin() == representationInstance.getPlugin() && previewOwner.isPinned()) {
				representationPreviewInstance = new PluginInstance<>(representationPreviewInstance);
				RepresentationPlugin plugin = representationInstance.getPlugin();
				for (Parameter<?> parameter : plugin.getParameters()) {
					if (!plugin.isPreviewable(parameter)) {
						representationPreviewInstance.setParameterValue(
								parameter,
								representationInstance.getParameterValue(parameter));
					}
				}
				representationInstance = representationPreviewInstance;				
				previewing = true;
			}
		}

		if (numberFactoryChanged || formulaChanged || representationPluginChanged || rectangleChanged || juliaSetPointChanged || (zoomIn && mainWindow.hasSelection()) ||
				(representationChanged && mustRecomputeRepresentation(representationInstance))) {

			NumberFactory numberFactory;
			Formula formula;
			Representation representation;
			Representation representationPreview;
			try {
				numberFactory = (NumberFactory) numberFactoryInstance.create();
				formula = (Formula) formulaInstance.create(numberFactory);
				representation = (Representation) representationInstance.create(numberFactory);
				representationPreview = (Representation) representationInstance.create();
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
				return;
			}

			int imgWidth = iimg.getWidth();
			int imgHeight = iimg.getHeight();
			CoordinateTransform coordinateTransform = createCoordinateTransform(imgWidth, imgHeight, image, numberFactory);

			if (source != null && zoomIn && mainWindow.hasSelection()) {
				ImageSelection selection = mainWindow.getSelection();
				Real width = coordinateTransform.getScaleRe().times(selection.getWidth());
				Real centerRe = coordinateTransform.toRe(selection.getCenterX());
				Real centerIm = coordinateTransform.toIm(selection.getCenterY());
				Real scaleRe = width.dividedBy(imgWidth);
				Real scaleIm = forceEqualScales ?
						scaleRe.negate() :
						coordinateTransform.getScaleIm().times(selection.getWidth()).dividedBy(imgWidth);
				Real re0 = centerRe.minus(scaleRe.times((imgWidth - 1) / 2));
				Real im0 = centerIm.minus(scaleIm.times((imgHeight - 1) / 2));
				Real zero = numberFactory.zero();
				coordinateTransform = new CoordinateTransform(re0, im0, zero, zero, scaleRe, scaleIm);
				Real re1 = coordinateTransform.toRe(imgWidth);
				Real im1 = coordinateTransform.toIm(imgHeight);
				
				image.rectangle = new Rectangle(re0.decimalValue(), im0.decimalValue(), re1.decimalValue(), im1.decimalValue());
			}

			Complex cJuliaSetPoint = juliaSetPoint != null ?
					numberFactory.valueOf(juliaSetPoint.getRe(), juliaSetPoint.getIm()) : null;

			IntermediateImage iimg = representation.createIntermediateImage(imgWidth, imgHeight,
					Math.min(Runtime.getRuntime().availableProcessors(), preferences.getNumOfProducerThreads()));
			Production production = representation.createProduction(
					iimg, numberFactory, formula, coordinateTransform, cJuliaSetPoint);
			Consumer consumer = representation.createConsumer(iimg, mainWindow.getConsumer(), false);

			run(production);

			if (zoomIn) {
				mainWindow.clearSelection();
			}
			mainWindow.reset(imgWidth, imgHeight, preferences.getTransparency(), consumer);
			if (periodicRefreshEnabled) {
				refreshTimer.restart();
			}
			haltAction.setEnabled(true);
			resumeAction.setEnabled(false);

			this.currentImage = image;
			this.iimg = iimg;
			this.representation = representationPreview;

			if (source != null) {
				source.unmarkEdits(zoomIn);
			}
			for (ControlWindow cw : cwList) {
				if (cw != source) {
					if (cw.isPinned()) {
						cw.markEdits();
					} else {
						cw.discardEdits();
					}
				}
			}

		} else if (representationChanged) {

			Representation representation;
			try {
				representation = (Representation) representationInstance.create();
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
				return;
			}

			if (!previewing) {
				Consumer consumer = representation.createConsumer(iimg, mainWindow.getConsumer(), true);
				mainWindow.refresh(consumer);
			}

			this.currentImage = image;
			this.representation = representation;

			if (source != null) {
				source.unmarkEdits(zoomIn);
			}
			for (ControlWindow cw : cwList) {
				if (cw != source) {
					if (cw.isPinned()) {
						cw.markEdits();
					} else {
						cw.discardEdits();
					}
				}
			}
		}
	}

	public Preferences getPreferences() {
		return preferences;
	}

	public List<NumberFactoryPlugin> getNumberFactories() {
		return numberFactories;
	}

	public List<FormulaPlugin> getFormulas() {
		return formulas;
	}

	public List<RepresentationPlugin> getRepresentations() {
		return representations;
	}

	public <P extends Plugin> List<P> getPluginsLikeThis(P plugin) {
		List<? extends Plugin> rv;
		switch (plugin.getFamily()) {
		case numberFactory : rv = numberFactories; break;
		case formula       : rv = formulas; break;
		case representation: rv = representations; break;
		default: throw new AssertionError(plugin.getFamily());
		}
		return (List<P>) rv;
	}

	private void initUi() {
		removeFocusTraversalKeysWithCtrl();
		refreshTimer = new Timer(preferences.getRefreshDelay(), new PeriodicRefreshPerformer());
		cwPool = new LinkedList<>();
		for (int i = 0; i < MAX_CW_POOL_SIZE; i++) {
			cwPool.add(new ControlWindow(this));
		}
		ControlWindow initialCw = new ControlWindow(this);
		initialCw.setDisposeButtonEnabled(false);
		initialCw.setPinned(false);
		initialCw.setPinButtonGroup(getPinButtonGroup());
		initialCw.setPreviewCheckBoxGroup(getPreviewCheckBoxGroup());
		cwList = new LinkedList<>();
		cwList.add(initialCw);

		undoAction = new UndoAction();
		redoAction = new RedoAction();
		haltAction = new HaltAction();
		resumeAction = new ResumeAction();
		refreshAction = new RefreshAction();
		refreshPeriodicallyAction = new RefreshPeriodicallyAction();
		editPreferencesAction = new EditPreferencesAction();
		editSelectionColorAction = new EditSelectionColorAction();
		hideAllAction = new HideAllAction();
		showAllAction = new ShowAllAction();
		loadAction = new LoadAction();
		saveAction = new SaveAction();
		exportAction = new ExportAction();
		quitAction = new QuitAction();
		showLogsAction = new ShowLogsAction();
		installNewPluginsAction = new InstallNewPluginsAction();
		browseJuliaHomePageAction = new BrowseJuliaHomePageAction();
		showInfosAction = new ShowInfosAction();

		mainWindow = new MainWindow(this);
		mainWindow.addToWindowMenu(initialCw.getItemInWindowMenu());
		mainWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});
	}

	private Image showWelcome(Window parent,
			NumberFactoryPlugin numberFactory,
			FormulaPlugin formula,
			RepresentationPlugin representation,
			boolean forceEqualScalesChecked,
			boolean juliaSetChecked) {

		PluginSelectionPane pluginSelectionPane = new PluginSelectionPane(numberFactories, formulas, representations);
		pluginSelectionPane.setSelectedNumberFactory(numberFactory);
		pluginSelectionPane.setSelectedFormula(formula);
		pluginSelectionPane.setSelectedRepresentation(representation);
		pluginSelectionPane.setForceEqualScalesChecked(forceEqualScalesChecked);
		pluginSelectionPane.setJuliaSetChecked(juliaSetChecked);
		
		JDialog dialog = pluginSelectionPane.createDialog(parent, "Welcome to Julia");
		dialog.setVisible(true);
		dialog.dispose();

		Integer value = (Integer) pluginSelectionPane.getValue();
		if (value == null || value.intValue() != JOptionPane.OK_OPTION) {
			executorService.shutdown();
			if (parent != null) {
				parent.setVisible(false);
				parent.dispose();
			}
			
			try {
				preferencesFile.close();
			} catch (IOException e) {}

			return null;
		}

		numberFactory = pluginSelectionPane.getSelectedNumberFactory();
		formula = pluginSelectionPane.getSelectedFormula();
		representation = pluginSelectionPane.getSelectedRepresentation();
		forceEqualScalesChecked = pluginSelectionPane.isForceEqualScalesChecked();
		juliaSetChecked = pluginSelectionPane.isJuliaSetChecked();

		if (pluginSelectionPane.isRememberTheseChoicesSelected()) {
			preferences.setStartupCombinationEnabled(true);
			preferences.setStartupNumberFactory(numberFactory.getId());
			preferences.setStartupFormula(formula.getId());
			preferences.setStartupRepresentation(representation.getId());
			preferences.setStartupForceEqualScalesFlag(forceEqualScalesChecked);
			preferences.setStartupJuliaSetFlag(juliaSetChecked);

			savePreferences(parent);
		}

		return new Image(
				new PluginInstance<>(numberFactory),
				new PluginInstance<>(formula),
				new PluginInstance<>(representation),
				juliaSetChecked ?
						formula.getDefaultJuliaSetRectangle() : formula.getDefaultMandelbrotSetRectangle(),
				forceEqualScalesChecked,
				juliaSetChecked ?
						formula.getDefaultJuliaSetPoint() : null);
	}

	public void startUp() {
		final SplashScreen splashScreen = Application.getSplashScreen();
		if (numberFactories.size() == 0 || formulas.size() == 0 || representations.size() == 0) {
			String details =
				"- Available number factories: " + numberFactories.size() +
				"\n- Available formulas: " + formulas.size() +
				"\n- Available representations: " + representations.size();
			MessagePane messagePane = new MessagePane(
					"At least 1 plugin for each of the 3 plugin families must be available in order to run the program. "
					+ "See details.",
					details,
					MessagePane.ERROR_MESSAGE);
			final String EXIT = "Exit";
			final String ABOUT_JULIA = "About Julia";
			final String INSTALL_NEW_PLUGINS = "Install new plugins...";
			messagePane.setOptions(new Object[] { INSTALL_NEW_PLUGINS, ABOUT_JULIA, EXIT });
			final JDialog dialog = createDialog(messagePane, splashScreen, "Julia", false);
			messagePane.addPropertyChangeListener(new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent e) {
					if (e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)
							&& e.getNewValue() != null
							&& e.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {
						String newValue = (String) e.getNewValue();
						if (newValue == EXIT) {
							dialog.setVisible(false);
						} else if (newValue == INSTALL_NEW_PLUGINS) {
							installNewPlugins(dialog);
						} else if (newValue == ABOUT_JULIA) {
							AboutPane.showDialog(dialog, "About Julia", Application.this);
						}
					}
				}
			});
			dialog.setResizable(true);
			dialog.setMinimumSize(dialog.getPreferredSize());
			dialog.setVisible(true);
			dialog.dispose();

			executorService.shutdown();
			splashScreen.setVisible(false);
			splashScreen.dispose();

			if (preferencesFile != null) {
				try {
					preferencesFile.close();
				} catch (IOException e) {}
			}

			return;
		}

		Image header;
		IntermediateImage intemediateImage;
		if (preferences.isStartupCombinationEnabled()) {
			NumberFactoryPlugin numberFactory = findNumberFactory(preferences.getStartupNumberFactory());
			FormulaPlugin formula = findFormula(preferences.getStartupFormula());
			RepresentationPlugin representation = findRepresentation(preferences.getStartupRepresentation());
			boolean forceEqualScalesChecked = preferences.getStartupForceEqualScalesFlag();
			boolean juliaSetChecked = preferences.getStartupJuliaSetFlag();

			if (numberFactory == null || formula == null || representation == null) {
				header = showWelcome(splashScreen, numberFactory, formula, representation,
						forceEqualScalesChecked,
						juliaSetChecked);
				if (header == null) {
					return;
				}
			} else {
				header = new Image(
						new PluginInstance<>(numberFactory),
						new PluginInstance<>(formula),
						new PluginInstance<>(representation),
						juliaSetChecked ?
								formula.getDefaultJuliaSetRectangle() : formula.getDefaultMandelbrotSetRectangle(),
						forceEqualScalesChecked,
						juliaSetChecked ?
								formula.getDefaultJuliaSetPoint() : null);
			}
			intemediateImage = null;
			initUi();
		} else {
			String pathString = preferences.getStartupImagePath();
			if (pathString != null && !pathString.isEmpty()) {
				Path path = Paths.get(pathString);
				if (!path.isAbsolute()) {
					path = profile.getRootDirectory().resolve(path);
				}
				
				Printer errorOutput = Printer.newStringPrinter();
				LoadWorker productionLoader = new LoadWorker(path.toFile(), this, errorOutput, true) {
					@Override
					protected void processResult(Void result) {
						if (hasErrors()) {
							MessagePane.showErrorMessage(getBlockingDialog(),
									"Julia",
									"Error(s) encountered during loading. See the details.",
									getErrorOutput().toString());
						}
					}
				};
				executorService.execute(productionLoader);
				initUi();
				productionLoader.block(splashScreen, "Reading from " + path + ":", "number factory...");

				if (productionLoader.hasHeader()) {
					PluginInstance<NumberFactoryPlugin> numberFactoryInstance = productionLoader.getNumberFactoryInstance();
					if (numberFactoryInstance == null) {
						NumberFactoryPlugin numberFactory =
								PluginSelectionPane.showSelectionPane(splashScreen, "Julia", null, getNumberFactories());
						numberFactoryInstance = new PluginInstance<NumberFactoryPlugin>(numberFactory);
					}

					header = new Image(numberFactoryInstance,
							productionLoader.getFormulaInstance(),
							productionLoader.getRepresentationInstance(),
							productionLoader.getRectangle(),
							productionLoader.getForceEqualScales(),
							productionLoader.getJuliaSetPoint());
				} else {
					header = showWelcome(splashScreen,
							productionLoader.getNumberFactory(),
							productionLoader.getFormula(),
							productionLoader.getRepresentation(),
							productionLoader.getForceEqualScales(false),
							productionLoader.getJuliaSetPoint(null) != null);
					if (header == null) {
						return;
					}
				}
				intemediateImage = productionLoader.getIntermediateImage();
			} else {
				header = showWelcome(splashScreen, null, null, null, false, false);
				if (header == null) {
					return;
				}
				intemediateImage = null;
				initUi();
			}
		}

		load(header, intemediateImage, true);

		splashScreen.setVisible(false);
		splashScreen.dispose();

		mainWindow.pack();
		ControlWindow initialCw = cwList.get(0);
		initialCw.pack();

		mainWindow.setVisible(true);
		initialCw.setVisible(true);
	}

	public void saveImage(final ControlWindow src, File dst) {
		Image image = src.getImage();
		SaveWorker saveWorker = new SaveWorker(dst, image, null) {
			@Override
			protected void processException(Throwable e) {
				if (e instanceof IOException) {
					MessagePane.showWriteErrorMessage(src, getFile(), (IOException) e);
				} else {
					MessagePane.showErrorMessage(src,
							"Julia",
							"Operation failed. See the details.",
							e);
				}
			}
		};
		executorService.execute(saveWorker);
	}

	public void loadImage(File src, final ControlWindow dst) {
		Printer errorOutput = Printer.newStringPrinter();
		LoadWorker loadWorker = new LoadWorker(src, this, errorOutput, false) {
			@Override
			protected void processResult(Void result) {
				if (hasErrors()) {
					MessagePane.showErrorMessage(getBlockingDialog(),
							"Julia",
							"Error(s) encountered during loading. See the details.",
							getErrorOutput().toString());
				}

				Image image = new Image(
						getNumberFactoryInstance(),
						getFormulaInstance(),
						getRepresentationInstance(),
						getRectangle(),
						getForceEqualScales(dst.getForceEqualScales()),
						getJuliaSetPoint(dst.getJuliaSetPoint()));
				
				dst.load(image);
			}
		};

		executorService.execute(loadWorker);
		loadWorker.block(dst, "Reading from " + src + ":", "number factory...");
	}

	private void quit() {
		int rv = JOptionPane.showConfirmDialog(
				mainWindow,
				"Are you sure you want to quit?",
				"Julia",
				JOptionPane.YES_NO_OPTION);
		if (rv == JOptionPane.YES_OPTION) {
			executorService.shutdownNow();
			for (ControlWindow cw : cwList) {
				cw.setVisible(false);
				cw.dispose();
			}
			mainWindow.setVisible(false);
			mainWindow.dispose();
			
			try {
				preferencesFile.close();
			} catch (IOException e) {}
		}
	}

	public NumberFactoryPlugin findNumberFactory(String id) {
		return findPlugin(id, numberFactories);
	}

	public FormulaPlugin findFormula(String id) {
		return findPlugin(id, formulas);
	}

	public RepresentationPlugin findRepresentation(String id) {
		return findPlugin(id, representations);
	}

	public Plugin findPlugin(String id, PluginFamily pluginFamily) {
		switch (pluginFamily) {
		case numberFactory:  return findNumberFactory(id);
		case formula:		 return findFormula(id);
		case representation: return findRepresentation(id);
		default: throw new AssertionError(pluginFamily);
		}
	}

	private static <P extends Plugin> P findPlugin(String id, List<P> plugins) {
		for (P plugin : plugins) {
			if (plugin.getId().equals(id)) {
				return plugin;
			}
		}
		return null;
	}

	public JuliaButtonGroup getPreviewCheckBoxGroup() {
		if (previewCheckBoxGroup == null) {
			previewCheckBoxGroup = new JuliaButtonGroup();
			previewCheckBoxGroup.addPropertyChangeListener(new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getPropertyName().equals("selectedButton")) {
						if (event.getNewValue() == null) {
							setPreviewOwner(null);
						} else {
							setPreviewOwner(
									(ControlWindow) SwingUtilities.getWindowAncestor((Component) event.getNewValue()));
						}
					}
				}
			});
		}

		return previewCheckBoxGroup;
	}

	public JuliaButtonGroup getPinButtonGroup() {
		if (pinButtonGroup == null) {
			pinButtonGroup = new JuliaButtonGroup(true);
		}
		return pinButtonGroup;
	}

	public PluginInstance<NumberFactoryPlugin> getNumberFactoryInstance() {
		return currentImage.numberFactoryInstance;
	}

	public PluginInstance<FormulaPlugin> getFormulaInstance() {
		return currentImage.formulaInstance;
	}

	public PluginInstance<RepresentationPlugin> getRepresentationInstance() {
		return currentImage.representationInstance;
	}

	public NumberFactoryPlugin getNumberFactory() {
		return currentImage.numberFactoryInstance.getPlugin();
	}

	public FormulaPlugin getFormula() {
		return currentImage.formulaInstance.getPlugin();
	}

	public RepresentationPlugin getRepresentation() {
		return currentImage.representationInstance.getPlugin();
	}

	public Rectangle getRectangle() {
		return currentImage.rectangle;
	}

	public boolean getForceEqualScales() {
		return currentImage.forceEqualScales;
	}

	public JuliaSetPoint getJuliaSetPoint() {
		return currentImage.juliaSetPoint;
	}

	public Image getCurrentImage() {
		return currentImage;
	}

	public JuliaSetPoint getCurrentSelectionCenter() {
		ImageSelection selection = mainWindow.getSelection();
		if (selection != null) {
			NumberFactory numberFactory;
			try {
				numberFactory = (NumberFactory) getNumberFactoryInstance().create();
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
				return null;
			}
			CoordinateTransform coordinateTransform = createCoordinateTransform(iimg.getWidth(), iimg.getHeight(), currentImage, numberFactory);
			int x = selection.getCenterX();
			int y = selection.getCenterY();
			Real re = coordinateTransform.toRe(x);
			Real im = coordinateTransform.toIm(y);
			JuliaSetPoint rv = new JuliaSetPoint(re.decimalValue(), im.decimalValue());
			return rv;
		}
		return null;
	}

	public Action getUndoAction() {
		return undoAction;
	}

	public Action getRedoAction() {
		return redoAction;
	}

	public Action getHaltAction() {
		return haltAction;
	}

	public Action getResumeAction() {
		return resumeAction;
	}

	public Action getRefreshAction() {
		return refreshAction;
	}

	public Action getRefreshPeriodicallyAction() {
		return refreshPeriodicallyAction;
	}

	public Action getEditPreferencesAction() {
		return editPreferencesAction;
	}

	public Action getEditSelectionColorAction() {
		return editSelectionColorAction;
	}

	public Action getHideAllAction() {
		return hideAllAction;
	}

	public Action getShowAllAction() {
		return showAllAction;
	}

	public Action getLoadAction() {
		return loadAction;
	}

	public Action getSaveAction() {
		return saveAction;
	}

	public Action getExportAction() {
		return exportAction;
	}

	public Action getQuitAction() {
		return quitAction;
	}

	public Action getShowLogsAction() {
		return showLogsAction;
	}

	public Action getInstallNewPluginsAction() {
		return installNewPluginsAction;
	}

	public Action getBrowseJuliaHomePageAction() {
		return browseJuliaHomePageAction;
	}

	public Action getShowInfosAction() {
		return showInfosAction;
	}

	private ProductionObserver productionObserver;
	private int resumableCount;
	private int finishedCount;

	private static final class IndexedProducer implements Runnable {
		private final Production.Producer target;
		private final int index;
		public IndexedProducer(Production.Producer target, int index) {
			this.target = target;
			this.index = index;
		}
		public Production.Producer getTarget() {
			return target;
		}
		public int getIndex() {
			return index;
		}
		@Override
		public void run() {
			target.run();
		}
	}

	private class ProductionObserver implements ExecutionObserver {

		private volatile boolean overridden = false;

		public void executionStarting(Runnable runnable) {
		}

		public void executionCancelled(final Runnable runnable) {
			if (!overridden) {
				addResumable((IndexedProducer) runnable);
			}
		}

		public void executionFinished(Runnable runnable) {
			if (!overridden) {
				IndexedProducer indexedProducer = (IndexedProducer) runnable;
				if (indexedProducer.getTarget().hasFinished()) {
					addFinished(indexedProducer);
				} else {
					addResumable(indexedProducer);
				}
			}
		}

		public void executionFinished(final Runnable runnable, Throwable cause) {
			if (!overridden) {
				IndexedProducer indexedProducer = (IndexedProducer) runnable;
				try {
					Utilities.invoke(indexedProducer.getTarget(),
							"handleException",
							cause);
				} finally {
					addResumable(indexedProducer, cause);
				}
			}
		}

		private void addFinished(final IndexedProducer indexedProducer) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					if (!overridden) {
						futures.set(indexedProducer.getIndex(), null);
						finishedCount++;
						if (resumableCount + finishedCount == futures.size()) {
							refreshTimer.stop();
							mainWindow.stopTimer();
							mainWindow.refresh();
							haltAction.setEnabled(false);
						}

						LogTab logTab = logPane.getLogTab(indexedProducer.getIndex());
						logTab.refresh();
						logTab.appendLine("Terminated.", Color.RED);
					}
				}
			});
		}
		
		private void addResumable(IndexedProducer indexedProducer) {
			addResumable(indexedProducer, null);
		}

		private void addResumable(final IndexedProducer indexedProducer, final Throwable cause) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					if (!overridden) {
						futures.set(indexedProducer.getIndex(), null);
						resumables.set(indexedProducer.getIndex(), indexedProducer);
						resumableCount++;
						if (resumableCount + finishedCount == futures.size()) {
							refreshTimer.stop();
							mainWindow.stopTimer();
							mainWindow.refresh();
							haltAction.setEnabled(false);
							if (finishedCount == 0)
								indexedProducer.getTarget().getProduction().resetSynchronizers();
							resumeAction.setEnabled(true);
						}

						LogTab logTab = logPane.getLogTab(indexedProducer.getIndex());
						logTab.refresh();
						if (halted) {
							logTab.appendLine("Halted by the user.", Color.RED);
						} else {
							resumeAction.setEnabled(true);
							if (cause != null) {
								logTab.appendStackTrace(cause);
							} else {
								logTab.appendLine("Halted unexpectedly.", Color.RED);
							}
							setStatusMessage("WARNING: some producer threads stopped before completion. See logs.", true);
						}
					}
				}
			});
		}

		public void override() {
			overridden = true;
		}
	}

	public void halt() {
		halted = true;
		for (Future<?> future : futures) {
			if (future != null) {
				future.cancel(true);
			}
		}
		haltAction.setEnabled(false);
	}

	public void resume() {
		halted = false;
		for (int i = 0; i < resumables.size(); i++) {
			Runnable resumable = resumables.get(i);
			if (resumable != null) {
				futures.set(i, executorService.submitAndObserve(resumable, productionObserver));
				resumables.set(i, null);
			}
		}
		mainWindow.startTimer();
		resumableCount = 0;
		if (periodicRefreshEnabled) {
			refreshTimer.start();
		}
		haltAction.setEnabled(true);
		resumeAction.setEnabled(false);
		setStatusMessage(null, false);
	}

	private void run(Production production) {
		if (preferences.isLoggingEnabled())
			production.setMaxLogLength(preferences.getMaxLogLength());

		IntermediateImage iimg = production.getIntermediateImage();
		int numOfProducers = iimg.getNumOfProducers();
		List<Production.Producer> producers = new ArrayList<>(numOfProducers);
		for (int i = 0; i < numOfProducers; i++) {
			producers.add(production.createProducer(iimg.getProgressOf(i)));
		}

		cancelCurrentProduction();
		
		mainWindow.resetTimer();
		List<Future<?>> futures = new ArrayList<>(numOfProducers); 
		ProductionObserver productionObserver = new ProductionObserver();
		for (int i = 0; i < numOfProducers; i++) {
			futures.add(executorService.submitAndObserve(
					new IndexedProducer(producers.get(i), i),
					productionObserver));
		}
		mainWindow.startTimer();

		this.productionObserver = productionObserver;
		this.futures = futures;
		this.resumables = new ArrayList<>(Collections.nCopies(numOfProducers, (Runnable) null));
		this.resumableCount = 0;
		this.finishedCount = 0;
		
		logPane.init(preferences.getMaxLogLength(), producers);
	}

	private Action createMoveWindowFocusForwardAction(final JFrame target) {
		return new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toFront(getNextVisibleJFrame(target));
			}
		};
	}

	private Action createMoveWindowFocusBackAction(final JFrame target) {
		return new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toFront(getPreviousVisibleJFrame(target));
			}
		};
	}

	public void installMoveWindowFocusKeyBindings(JFrame frame) {
		InputMap inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.CTRL_DOWN_MASK),
				"moveWindowFocusForward");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
				"moveWindowFocusBack");
		
		ActionMap actionMap = frame.getRootPane().getActionMap();
		actionMap.put("moveWindowFocusForward", createMoveWindowFocusForwardAction(frame));
		actionMap.put("moveWindowFocusBack", createMoveWindowFocusBackAction(frame));
	}

	public static SplashScreen getSplashScreen() {
		for (Window w : Window.getWindows()) {
			if (w instanceof SplashScreen) {
				return (SplashScreen) w;
			}
		}
		return null;
	}

	public static void removeFocusTraversalKeysWithCtrl() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.setDefaultFocusTraversalKeys(
						KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
						Collections.singleton(KeyStroke.getKeyStroke(
								KeyEvent.VK_TAB, 0)));
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.setDefaultFocusTraversalKeys(
						KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
						Collections.singleton(KeyStroke.getKeyStroke(
								KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK)));
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				SplashScreen splashScreen = new SplashScreen();
				splashScreen.setVisible(true);
			}
		});
		
		Profile profile = Profile.getDefaultProfile();
		JuliaExecutorService executorService = new JuliaExecutorService(0, 10l, TimeUnit.MINUTES);
		new Loader(profile, executorService);
	}
}

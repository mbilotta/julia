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

import static org.altervista.mbilotta.julia.Utilities.showSaveDialog;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.text.ParseException;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultFormatter;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.program.Application;
import org.altervista.mbilotta.julia.program.JuliaSetPoint;
import org.altervista.mbilotta.julia.program.PluginInstance;
import org.altervista.mbilotta.julia.program.Rectangle;
import org.altervista.mbilotta.julia.program.parsers.FormulaPlugin;
import org.altervista.mbilotta.julia.program.parsers.NumberFactoryPlugin;
import org.altervista.mbilotta.julia.program.parsers.Parameter;
import org.altervista.mbilotta.julia.program.parsers.Plugin;
import org.altervista.mbilotta.julia.program.parsers.PluginFamily;
import org.altervista.mbilotta.julia.program.parsers.RepresentationPlugin;


public class ControlWindow extends JFrame {
	
	private PluginPanel<NumberFactoryPlugin> numberFactoryPanel;
	private PluginPanel<FormulaPlugin> formulaPanel;
	private PluginPanel<RepresentationPlugin> representationPanel;
	private JCheckBox previewCheckBox;
	private RectanglePanel rectanglePanel;
	private JuliaSetPointPanel juliaSetPointPanel;

	private PluginInstance<RepresentationPlugin> representationPreviewInstance;

	private JMenuItem itemInWindowMenu;
	private JToggleButton connectionToggler;
	private JButton disposeButton;

	private Application application;

	private static final Color GREEN = Color.GREEN.darker();
	private static final FocusTracker FOCUS_TRACKER = new FocusTracker();
	private static final int SPACING = 3;

	public ControlWindow(Application application) {
		this.application = application;
		initCommon();
	}

	private static void updateConnectionTogglerTooltip(JToggleButton toggler) {
		if (toggler.isSelected()) {
			toggler.setToolTipText("Disconnect from main UI");
		} else {
			toggler.setToolTipText("Connect to main UI");
		}
	}

	private JMenuBar createMenuBar() {
		JMenuBar rv = new JMenuBar();
		rv.setLayout(new WrapLayout(WrapLayout.LEADING, 0, 0));
		rv.add(numberFactoryPanel.getMenu());
		rv.add(formulaPanel.getMenu());
		rv.add(representationPanel.getMenu());

		JMenu rectangleMenu = new LazyLoadedJMenu("Rectangle", rectanglePanel);
		rectangleMenu.setMnemonic(KeyEvent.VK_T);
		rv.add(rectangleMenu);

		JMenu juliaSetPointMenu = new LazyLoadedJMenu("Julia set point", juliaSetPointPanel);
		juliaSetPointMenu.setMnemonic(KeyEvent.VK_J);
		rv.add(juliaSetPointMenu);

		return rv;
	}

	private JToolBar createToolbar() {

		JButton applyButton = new JButton(application.getIcon(Application.APPLY_ICON_KEY));
		applyButton.setToolTipText("Apply edits");

		JButton zoomInButton = new JButton(application.getIcon(Application.APPLY_WITH_ZOOM_ICON_KEY));
		zoomInButton.setToolTipText("Apply edits but zoom selection");

		JButton discardButton = new JButton(application.getIcon(Application.DISCARD_ICON_KEY));
		discardButton.setToolTipText("Discard edits");

		connectionToggler = new JToggleButton(application.getIcon(Application.DISCONNECTED_ICON_KEY));
		connectionToggler.setSelectedIcon(application.getIcon(Application.CONNECTED_ICON_KEY));
		updateConnectionTogglerTooltip(connectionToggler);
		
		JButton cloneButton = new JButton(application.getIcon(Application.CLONE_ICON_KEY));
		cloneButton.setToolTipText("Clone window");

		JButton renameButton = new JButton(application.getIcon(Application.RENAME_ICON_KEY));
		renameButton.setToolTipText("Rename window");
		
		JButton hideButton = new JButton(application.getIcon(Application.HIDE_ICON_KEY));
		hideButton.setToolTipText("Hide window");

		disposeButton = new JButton(application.getIcon(Application.DISPOSE_ICON_KEY));
		disposeButton.setToolTipText("Dispose window");

		JButton saveButton = new JButton(application.getIcon(Application.SAVE_ICON_KEY));
		saveButton.setToolTipText("Save as a file");

		JButton loadButton = new JButton(application.getIcon(Application.OPEN_ICON_KEY));
		loadButton.setToolTipText("Load from file");

		applyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				applyEdits();
			}
		});

		zoomInButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				zoomIn();
			}
		});

		discardButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				discardEdits();
			}
		});

		cloneButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cloneCw();
			}
		});

		hideButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				hideCw();
			}
		});

		disposeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				disposeCw();
			}
		});

		renameButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				renameCw();
			}
		});

		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});

		loadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				load();
			}
		});

		connectionToggler.addItemListener(e -> {
			updateConnectionTogglerTooltip(connectionToggler);
		});

		JToolBar rv = new JToolBar(JToolBar.HORIZONTAL);
		rv.setLayout(new WrapLayout(WrapLayout.LEADING, 0, 0));
		rv.setFloatable(false);

		rv.add(applyButton);
		rv.add(zoomInButton);
		rv.add(discardButton);
		rv.addSeparator();
		rv.add(connectionToggler);
		rv.addSeparator();
		rv.add(cloneButton);
		rv.add(renameButton);
		rv.add(hideButton);
		rv.add(disposeButton);
		rv.addSeparator();
		rv.add(saveButton);
		rv.add(loadButton);

		return rv;
	}

	private void initCommon() {
		LazyLoadedJMenu numberFactoryMenu = new LazyLoadedJMenu("Number Factory");
		numberFactoryMenu.setMnemonic(KeyEvent.VK_N);

		LazyLoadedJMenu formulaMenu = new LazyLoadedJMenu("Formula");
		formulaMenu.setMnemonic(KeyEvent.VK_F);

		LazyLoadedJMenu representationMenu = new LazyLoadedJMenu("Representation");
		representationMenu.setMnemonic(KeyEvent.VK_R);

		numberFactoryPanel = new PluginPanel<>(numberFactoryMenu);
		formulaPanel = new PluginPanel<>(formulaMenu);
		representationPanel = new PluginPanel<>(representationMenu);

		previewCheckBox = new JCheckBox("Preview", false);
		previewCheckBox.setOpaque(false);
		previewCheckBox.addFocusListener(FOCUS_TRACKER);
		previewCheckBox.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 0, SPACING, 0),
				previewCheckBox.getBorder()));

		rectanglePanel = new RectanglePanel();
		juliaSetPointPanel = new JuliaSetPointPanel();

		setTitle("Unnamed");
		itemInWindowMenu = new JMenuItem("0   " + getTitle(), KeyEvent.VK_0);
		itemInWindowMenu.addPropertyChangeListener(AbstractButton.MNEMONIC_CHANGED_PROPERTY, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				itemInWindowMenu.setText(getNewText());
			}

			private String getNewText() {
				StringBuilder sb = new StringBuilder(itemInWindowMenu.getText());
				sb.setCharAt(0, (char) itemInWindowMenu.getMnemonic());
				return sb.toString();
			}
		});
		itemInWindowMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Utilities.show(ControlWindow.this);
			}
		});
		addPropertyChangeListener("title", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				char mnemonic = (char) itemInWindowMenu.getMnemonic();
				itemInWindowMenu.setText(mnemonic + "   " + ((String) e.getNewValue()));
			}
		});

		JMenuBar menuBar = createMenuBar();
		JToolBar toolBar = createToolbar();

		Box parametersPane = new Box(BoxLayout.Y_AXIS);
		parametersPane.setOpaque(true);
		parametersPane.setBackground(Color.WHITE);
		parametersPane.setBorder(BorderFactory.createEmptyBorder(SPACING, SPACING, SPACING, SPACING));
		Dimension spacing = new Dimension(0, SPACING);
		parametersPane.add(numberFactoryPanel);
		parametersPane.add(Box.createRigidArea(spacing));
		parametersPane.add(formulaPanel);
		parametersPane.add(Box.createRigidArea(spacing));
		parametersPane.add(representationPanel);
		parametersPane.add(Box.createRigidArea(spacing));
		parametersPane.add(rectanglePanel);
		parametersPane.add(Box.createRigidArea(spacing));
		parametersPane.add(juliaSetPointPanel);
		parametersPane.add(Box.createVerticalGlue());

		Container contentPane = getContentPane();
		JScrollPane scrollPane = new JScrollPane(parametersPane);
		contentPane.add(toolBar, BorderLayout.PAGE_START);
		contentPane.add(scrollPane, BorderLayout.CENTER);

		setJMenuBar(menuBar);

		addWindowFocusListener(new WindowFocusListener() {
			public void windowLostFocus(WindowEvent e) {
				Window window = e.getWindow();
				Component oldFocusOwner = window.getMostRecentFocusOwner();
				if (oldFocusOwner instanceof JFormattedTextField) {
					JFormattedTextField formattedTextField = (JFormattedTextField) oldFocusOwner;
					try {
						formattedTextField.commitEdit();
					} catch (ParseException exception) {
					}
				}
			}
			public void windowGainedFocus(WindowEvent e) {
			}
		});

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				application.handleControlWindowClosing(ControlWindow.this);
			}
		});
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				itemInWindowMenu.setIcon(null);
			}
			@Override
			public void componentHidden(ComponentEvent e) {
				itemInWindowMenu.setIcon(application.getIcon(Application.HIDED_ICON_KEY));
			}
		});

		application.installMoveWindowFocusKeyBindings(this);
	}

	private static class DecimalFormatter extends DefaultFormatter {
		public DecimalFormatter() {
			setAllowsInvalid(true);
			setCommitsOnValidEdit(false);
			setOverwriteMode(false);
			setValueClass(Decimal.class);
		}
	};

	private static class HyperlinkLabelMouseListener extends MouseAdapter {
		
		private final int hyperlinkWidth;
		private final UriHandle uri;
		private boolean handCursorSet = false;

		public HyperlinkLabelMouseListener(int hyperlinkWidth, UriHandle uri) {
			this.hyperlinkWidth = hyperlinkWidth;
			this.uri = uri;
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			Component label = e.getComponent();
			if (e.getX() < hyperlinkWidth) {
				label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				handCursorSet = true;
			}
		}

		@Override
		public void mouseExited(MouseEvent e) {
			Component label = e.getComponent();
			if (handCursorSet) {
				label.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				handCursorSet = false;
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			Component label = e.getComponent();
			if (e.getX() < hyperlinkWidth) {
				if (!handCursorSet) {
					label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					handCursorSet = true;
				}
			} else if (handCursorSet) {
				label.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				handCursorSet = false;
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			uri.visit(e.getComponent());
		}
	}

	private static abstract class LabelChanger implements ParameterChangeListener {

		private final JLabel label;
		private final MutableInteger editCount;
		private boolean edited = false;

		public LabelChanger(JLabel label, MutableInteger editCount) {
			this.label = label;
			this.editCount = editCount;
		}

		@Override
		public void parameterChanged(Parameter<?> subject, Object value) {
			setEditedFlag(!getValue().equals(value));
		}

		public abstract Object getValue();

		private CollapsiblePanel getCollapsiblePanelParent() {
			return (CollapsiblePanel) SwingUtilities.getAncestorOfClass(CollapsiblePanel.class, label);
		}

		private void setEditedFlag(boolean value) {
			if (value) {
				if (!edited) {
					String text = label.getText();
					String modifiedText = text.concat("*");
					label.setText(modifiedText);
					edited = true;
					if (editCount.getAndIncrement() == 0) {
						CollapsiblePanel parent = getCollapsiblePanelParent();
						text = parent.getTitle();
						modifiedText = text.concat("*");
						parent.setTitle(modifiedText);
					}
				}
			} else {
				if (edited) {
					String modifiedText = label.getText();
					String text = modifiedText.substring(0, modifiedText.length() - 1);
					label.setText(text);
					edited = false;
					if (editCount.decrementAndGet() == 0) {
						CollapsiblePanel parent = getCollapsiblePanelParent();
						modifiedText = parent.getTitle();
						text = modifiedText.substring(0, modifiedText.length() - 1);
						parent.setTitle(text);
					}
				}
			}
		}
	}

	private static class FocusTracker extends FocusAdapter {
		@Override
		public void focusGained(FocusEvent e) {
			JComponent source = (JComponent) e.getSource();
			JComponent parent = (JComponent) source.getParent();

			if (parent instanceof JSpinner.DefaultEditor) {
				source = (JComponent) parent.getParent();
				parent = (JComponent) source.getParent();
			}

			parent.scrollRectToVisible(source.getBounds());
		}
	}

	private class UriHandle implements ActionListener {
		
		private Parameter<?> parameter;
		private URI uri;

		public UriHandle(Parameter<?> parameter) {
			this.parameter = parameter;
		}

		public UriHandle(URI uri) {
			this.uri = uri;
		}

		public void actionPerformed(ActionEvent e) {
			visit(ControlWindow.this);
		}

		public void visit(Component src) {
			if (uri == null) {
				uri = createDocumentationURI(parameter);
			}
			application.visitPage(uri, src);
		}
	}

	private static JLabel createLabelFor(Parameter<?> parameter, boolean edited, UriHandle uri) {
		String text = "<html><a href>" + parameter.getName() + "</a>";
		String modifiedText = text.concat("*");
		JLabel label = new JLabel(modifiedText);
		Dimension preferredSize = label.getPreferredSize();
		label.setText(text);
		int hyperlinkWidth = label.getPreferredSize().width;
		label.setPreferredSize(preferredSize);
		HyperlinkLabelMouseListener labelMouseListener = new HyperlinkLabelMouseListener(hyperlinkWidth, uri);
		label.addMouseListener(labelMouseListener);
		label.addMouseMotionListener(labelMouseListener);
		if (edited)
			label.setText(modifiedText);

		return label;
	}

	private static JLabel createLabel(String text) {
		String modifiedText = text.concat("*");
		JLabel label = new JLabel(modifiedText);
		Dimension preferredSize = label.getPreferredSize();
		label.setText(text);
		label.setPreferredSize(preferredSize);
		return label;
	}

	private static void copyState(JuliaFormattedTextField src, JuliaFormattedTextField dst) {
		dst.setValue(src.getValue());
		dst.setText(src.getText());
		try {
			dst.commitEdit();
		} catch (ParseException e) {}
	}

	private URI createDocumentationURI(Parameter<?> parameter) {
		return URI.create(createDocumentationURI(parameter.getPlugin()) + "#" + parameter.getId());
	}

	private URI createDocumentationURI(Plugin plugin) {
		return application.getProfile().getDocumentationFileFor(plugin.getId()).toUri();
	}

	private static int getCurrentEventModifiers() {
		int modifiers = 0;
		AWTEvent currentEvent = EventQueue.getCurrentEvent();
		if (currentEvent instanceof InputEvent) {
			modifiers = ((InputEvent) currentEvent).getModifiers();
		} else if (currentEvent instanceof ActionEvent) {
			modifiers = ((ActionEvent) currentEvent).getModifiers();
		}
		return modifiers;
	}
	
	private static final DataFlavor PLUGIN_INSTANCE_FLAVOR;
	private static final DataFlavor CW_RECTANGLE_FLAVOR;
	private static final DataFlavor JULIA_SET_POINT_FLAVOR;
	
	static {
		try {
			PLUGIN_INSTANCE_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + PluginInstance.class.getName());
			CW_RECTANGLE_FLAVOR = new DataFlavor(DataFlavor.javaSerializedObjectMimeType + ";class=" + CWRectangle.class.getName());
			JULIA_SET_POINT_FLAVOR = new DataFlavor(DataFlavor.javaSerializedObjectMimeType + ";class=" + JuliaSetPoint.class.getName());
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e);
		}
	}

	private static class CWRectangle implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private final Rectangle rectangle;
		private final boolean forceEqualScales;
		
		public CWRectangle(Rectangle rectangle, boolean forceEqualScales) {
			this.rectangle = rectangle;
			this.forceEqualScales = forceEqualScales;
		}
		
		public Rectangle getRectangle() {
			return rectangle;
		}
		
		public boolean getForceEqualScales() {
			return forceEqualScales;
		}
	}

	private static class CWTransferHandler extends TransferHandler {

		@SuppressWarnings({"rawtypes", "unchecked"})
		@Override
		public boolean importData(TransferSupport support) {
			Component target = support.getComponent();
			try {
				if (target instanceof PluginPanel) {
					PluginInstance data = (PluginInstance) support.getTransferable().getTransferData(PLUGIN_INSTANCE_FLAVOR);
					((PluginPanel) target).load(data);
				} else if (target instanceof RectanglePanel) {
					CWRectangle data = (CWRectangle) support.getTransferable().getTransferData(CW_RECTANGLE_FLAVOR);
					((RectanglePanel) target).load(data.getRectangle(), data.getForceEqualScales());
				} else {
					JuliaSetPoint data = (JuliaSetPoint) support.getTransferable().getTransferData(JULIA_SET_POINT_FLAVOR);
					((JuliaSetPointPanel) target).load(data);
				}
			} catch (IOException | UnsupportedFlavorException e) {
				return false;
			}
			return true;
		}

		@Override
		public boolean canImport(TransferSupport support) {
			Component target = support.getComponent();
			if (target instanceof PluginPanel) {
				try {
					return ((PluginPanel<?>) target).getPlugin().getFamily() ==
							((PluginInstance<?>) support.getTransferable().getTransferData(PLUGIN_INSTANCE_FLAVOR)).getPlugin().getFamily();
				} catch (IOException | UnsupportedFlavorException e) {
					return false;
				}
			} else if (target instanceof RectanglePanel) {
				return support.isDataFlavorSupported(CW_RECTANGLE_FLAVOR);
			} else {
				return support.isDataFlavorSupported(JULIA_SET_POINT_FLAVOR);
			}
		}

		@Override
		public int getSourceActions(JComponent c) {
			return COPY_OR_MOVE;
		}

		@Override
		protected Transferable createTransferable(JComponent c) {
			final DataFlavor flavor;
			final Object data;
			if (c instanceof PluginPanel) {
				flavor = PLUGIN_INSTANCE_FLAVOR;
				data = ((PluginPanel<?>) c).getPluginInstance();
			} else if (c instanceof RectanglePanel) {
				RectanglePanel rp = (RectanglePanel) c;
				flavor = CW_RECTANGLE_FLAVOR;
				Rectangle r = new Rectangle(
						rp.getRe0(),
						rp.getIm0(),
						rp.getRe1(),
						rp.getIm1());
				data = new CWRectangle(r, rp.getForceEqualScales());
			} else {
				flavor = JULIA_SET_POINT_FLAVOR;
				JuliaSetPointPanel jspp = (JuliaSetPointPanel) c;
				data = jspp.isVisible() ? new JuliaSetPoint(jspp.getReC(), jspp.getImC()) : null;
			}
			return new Transferable() {
				@Override
				public boolean isDataFlavorSupported(DataFlavor df) {
					return df.equals(flavor);
				}
				
				@Override
				public DataFlavor[] getTransferDataFlavors() {
					return new DataFlavor[] { flavor };
				}
				
				@Override
				public Object getTransferData(DataFlavor df)
						throws UnsupportedFlavorException, IOException {
					if (df.equals(flavor)) {
						return data;
					}
					throw new UnsupportedFlavorException(df);
				}
			};
		}
	}

	private static final TransferHandler CW_TRANSFER_HANDLER = new CWTransferHandler();

	private static void installDataTransferSupport(JComponent target) {
		target.setTransferHandler(CW_TRANSFER_HANDLER);
		ActionMap actionMap = target.getActionMap();
		actionMap.put("copy", TransferHandler.getCopyAction());
		actionMap.put("paste", TransferHandler.getPasteAction());
	}

	private static boolean canPasteTo(JComponent target) {
		if (target instanceof JSpinner) {
			target = ((DefaultEditor) ((JSpinner) target).getEditor()).getTextField();
		}
		TransferHandler transferHandler = target.getTransferHandler();
		if (transferHandler != null) {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable contents;
			try {
				contents = clipboard.getContents(null);
			} catch (IllegalStateException e) {
				Utilities.printStackTrace(e);
				return false;
			}
			return transferHandler.canImport(new TransferHandler.TransferSupport(target, contents));
		}
		return false;
	}

	private static void addDataTransferItems(JPopupMenu menu, final JComponent target) {
		JMenuItem[] dataTransferItems = createDataTransferItems(target);
		menu.add(dataTransferItems[0]);
		menu.add(dataTransferItems[1]);
		final JMenuItem pasteItem = dataTransferItems[1];
		menu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				pasteItem.setEnabled(canPasteTo(target));
			}
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {}
		});
	}

	private static JMenuItem[] createDataTransferItems(final JComponent target) {
		JMenuItem copyItem = new JMenuItem("Copy");
		copyItem.setMnemonic(KeyEvent.VK_C);
		copyItem.setActionCommand("copy");
		ActionListener copyPasteListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComponent actualTarget;
				if (target instanceof JSpinner) {
					actualTarget = ((DefaultEditor) ((JSpinner) target).getEditor()).getTextField();
				} else {
					actualTarget = target;
				}
				ActionMap actionMap = actualTarget.getActionMap();
				Action action = actionMap.get(e.getActionCommand());
				action.actionPerformed(new ActionEvent(actualTarget,
						ActionEvent.ACTION_PERFORMED,
						(String) action.getValue(Action.NAME),
						EventQueue.getMostRecentEventTime(),
						getCurrentEventModifiers()));
			}
		};
		copyItem.addActionListener(copyPasteListener);
		
		JMenuItem pasteItem = new JMenuItem("Paste");
		pasteItem.setMnemonic(KeyEvent.VK_P);
		pasteItem.setActionCommand("paste");
		pasteItem.addActionListener(copyPasteListener);
		pasteItem.setEnabled(canPasteTo(target));
		return new JMenuItem[] { copyItem, pasteItem };
	}

	private class PluginPanel<P extends Plugin> extends CollapsiblePanel implements ParameterChangeListener, PreviewUpdater, MenuLoader {
		
		private PluginInstance<P> backupInstance;
		private P plugin;
		private JLabel[] labels;
		private boolean[] edited;
		private int editCount;
		private JComponent[] editors;
		private UriHandle[] uris;
		private LazyLoadedJMenu menu;

		private JButton switchButton;

		private class SwitchButtonListener implements ActionListener {
			@Override
			public void actionPerformed(ActionEvent e) {
				P plugin = PluginSelectionPane.showSelectionPane(PluginPanel.this, "Switch plugin", getPlugin(), application.getPluginsLikeThis(getPlugin()));
				if (plugin != null)
					switchPlugin(plugin);
			}
		}

		private class PopClickListener extends MouseAdapter {
			private JPopupMenu menu;
			private Parameter<?> parameter;
			
			public PopClickListener(Parameter<?> parameter) {
				this.parameter = parameter;
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					if (menu == null) {
						menu = new JPopupMenu();
						loadParameterMenu(menu, parameter, true);
					}
					menu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		}

		public PluginPanel(LazyLoadedJMenu pluginMenu) {
			super(new JPanel(new GridBagLayout()), "", Color.BLACK, false);
			menu = pluginMenu;
			menu.setLoader(this);
			switchButton = new JButton("Switch...");
			switchButton.addActionListener(new SwitchButtonListener());
			switchButton.addFocusListener(FOCUS_TRACKER);
			setOpaque(false);
			getChild().setOpaque(false);
			installDataTransferSupport(this);
		}

		private void disposeEditors() {
			if (plugin != null) {
				int numOfParameters = plugin.getNumOfParameters();
				for (int i = 0; i < numOfParameters; i++) {
					plugin.getParameter(i).disposeEditor(editors[i]);
				}
			}
		}

		private void setEditedFlag(Parameter<?> parameter, boolean value) {
			int i = parameter.getIndex();
			if (value) {
				if (!edited[i]) {
					JLabel label = labels[i];
					String text = label.getText();
					String modifiedText = text.concat("*");
					label.setText(modifiedText);
					edited[i] = true;
					if (editCount++ == 0) {
						text = getTitle();
						modifiedText = text.concat("*");
						setTitle(modifiedText);
					}
				}
			} else {
				if (edited[i]) {
					JLabel label = labels[i];
					String modifiedText = label.getText();
					String text = modifiedText.substring(0, modifiedText.length() - 1);
					label.setText(text);
					edited[i] = false;
					if (--editCount == 0) {
						modifiedText = getTitle();
						text = modifiedText.substring(0, modifiedText.length() - 1);
						setTitle(text);
					}
				}
			}
		}

		private void clearEditedFlags() {
			if (editCount > 0) {
				for (Parameter<?> parameter : plugin.getParameters()) {
					setEditedFlag(parameter, false);
				}
			}
		}

		private void updateEditedFlags() {
			for (Parameter<?> parameter : plugin.getParameters()) {
				Object value = parameter.getEditorValue(getEditorFor(parameter));
				setEditedFlag(parameter, !value.equals(backupInstance.getParameterValue(parameter)));
			}
		}

		private void loadHintGroupsMenu(JPopupMenu menu) {
			int mnemonic = 0;
			Set<String> hintGroupNames = plugin.getHintGroupNames();
			for (String name : hintGroupNames) {
				JMenuItem menuItem = new JMenuItem("<html><u>" + mnemonic + "</u>&nbsp;&nbsp;&nbsp;" + name);
				menuItem.setMnemonic(mnemonic + KeyEvent.VK_0);
				final List<Object> values = plugin.getHintGroup(name);
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setGroupOfParameters(values);
					}
				});
				menu.add(menuItem);
				mnemonic = (mnemonic + 1) % 10;
			}

			menu.addSeparator();
			JMenuItem menuItem = new JMenuItem("Open documentation");
			menuItem.setMnemonic(KeyEvent.VK_D);
			menuItem.addActionListener(new UriHandle(URI.create(createDocumentationURI(plugin) + "#hint-groups")));
			menu.add(menuItem);
		}

		public void loadMenu(JPopupMenu menu) {
			boolean hasParameters = false;
			for (final Parameter<?> parameter : plugin.getParameters()) {
				hasParameters = true;
				int mnemonic = parameter.getIndex() % 10;
				String text = "<html><u>" + mnemonic + "</u>&nbsp;&nbsp;&nbsp;" + parameter.getName();
				JMenu parameterSubmenu = new LazyLoadedJMenu(text, new MenuLoader() {
					@Override
					public void loadMenu(JPopupMenu popupMenu) {
						loadParameterMenu(popupMenu, parameter, false);
					}
				});
				parameterSubmenu.setMnemonic(mnemonic + KeyEvent.VK_0);
				menu.add(parameterSubmenu);
			}
			
			if (hasParameters) {
				menu.addSeparator();
			}

			addDataTransferItems(menu, this);

			menu.addSeparator();

			JMenuItem menuItem = new JMenuItem("Open documentation");
			menuItem.setMnemonic(KeyEvent.VK_D);
			menuItem.addActionListener(new UriHandle(createDocumentationURI(plugin)));
			menu.add(menuItem);

			menuItem = new JMenuItem("Discard edits");
			menuItem.setMnemonic(KeyEvent.VK_R);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					discardEdits();
				}
			});
			menu.add(menuItem);

			if (hasParameters) {
				JMenu hintGroupsSubmenu = new LazyLoadedJMenu("Hint groups", new MenuLoader() {
					@Override
					public void loadMenu(JPopupMenu popupMenu) {
						loadHintGroupsMenu(popupMenu);
					}
				});
				hintGroupsSubmenu.setMnemonic(KeyEvent.VK_H);
				menu.add(hintGroupsSubmenu);
			}

			menu.addSeparator();

			menuItem = new JMenuItem("Switch...");
			menuItem.setMnemonic(KeyEvent.VK_W);
			menuItem.addActionListener(new SwitchButtonListener());
			menu.add(menuItem);
		}

		private void loadHintsMenu(JPopupMenu menu, final Parameter<?> parameter) {
			int numOfHints = parameter.getNumOfHints();
			for (int i = 0; i < numOfHints; i++) {
				JMenuItem hintMenuItem = parameter.createHintMenuItem(i);
				final Object hint = parameter.getHint(i);
				hintMenuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setSingleParameter(parameter, hint);
					}
				});
				menu.add(hintMenuItem);
			}
		}

		private void loadParameterMenu(JPopupMenu menu, final Parameter<?> parameter, boolean isPopup) {
			JMenuItem[] dataTransferItems = createDataTransferItems(getEditorFor(parameter));
			menu.add(dataTransferItems[0]);
			menu.add(dataTransferItems[1]);
			menu.addSeparator();

			JMenuItem menuItem = new JMenuItem("Open documentation");
			menuItem.setMnemonic(KeyEvent.VK_D);
			menuItem.addActionListener(getDocumentationUri(parameter));
			menu.add(menuItem);

			if (!isPopup) {
				menuItem = new JMenuItem("Edit");
				menuItem.setMnemonic(KeyEvent.VK_E);
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						moveFocusTo(parameter);
					}
				});
				menu.add(menuItem);
			}
			
			final JMenuItem discardEditsItem = new JMenuItem("Discard edits");
			discardEditsItem.setMnemonic(KeyEvent.VK_R);
			discardEditsItem.setEnabled(plugin == backupInstance.getPlugin());
			discardEditsItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					discardEdits(parameter);
				}
			});
			menu.add(discardEditsItem);

			JMenu hintSubmenu = new LazyLoadedJMenu("Hints", new MenuLoader() {
				@Override
				public void loadMenu(JPopupMenu popupMenu) {
					loadHintsMenu(popupMenu, parameter);
				}
			});
			hintSubmenu.setMnemonic(KeyEvent.VK_H);
			menu.add(hintSubmenu);

			final JMenuItem pasteItem = dataTransferItems[1];
			menu.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					discardEditsItem.setEnabled(plugin == backupInstance.getPlugin());
					pasteItem.setEnabled(canPasteTo(getEditorFor(parameter)));
				}
				
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
				public void popupMenuCanceled(PopupMenuEvent e) {}
			});
		}

		public void init(PluginInstance<P> backupInstance) {
			init(backupInstance, backupInstance);
		}

		public void init(PluginInstance<P> backupInstance, P plugin) {
			init(backupInstance, new PluginInstance<P>(plugin));
		}

		public void init(PluginInstance<P> backupInstance, PluginInstance<P> instance) {
			this.backupInstance = backupInstance;
			this.plugin = instance.getPlugin();
			int numOfParameters = plugin.getNumOfParameters();
			labels = new JLabel[numOfParameters];
			edited = new boolean[numOfParameters];
			editCount = 0;
			editors = new JComponent[numOfParameters];
			uris = new UriHandle[numOfParameters];
			boolean samePlugin = backupInstance.getPlugin() == instance.getPlugin();
			boolean isRepresentation = instance.getPlugin().getFamily() == PluginFamily.representation;
			boolean hasPreviewableParameters = false;
			for (int i = 0; i < numOfParameters; i++) {
				Parameter<?> parameter = plugin.getParameter(i);
				edited[i] = samePlugin && !instance.getParameterValue(parameter).equals(backupInstance.getParameterValue(parameter));
				if (edited[i])
					editCount++;
				uris[i] = new UriHandle(parameter);
				labels[i] = createLabelFor(parameter, edited[i], uris[i]);
				editors[i] = parameter.createEditor(instance.getParameterValue(parameter));
				parameter.addParameterChangeListener(this, editors[i]);
				editors[i].addFocusListener(FOCUS_TRACKER);
				editors[i].addMouseListener(new PopClickListener(parameter));
				if (isRepresentation && ((RepresentationPlugin) plugin).isPreviewable(parameter)) {
					hasPreviewableParameters = true;
					parameter.setPreviewUpdater(this, editors[i]);
				}
			}
			if (isRepresentation) {
				previewCheckBox.setVisible(samePlugin && hasPreviewableParameters);
				if (hasPreviewableParameters) {
					if (samePlugin) {
						representationPreviewInstance = new PluginInstance<>((PluginInstance<RepresentationPlugin>) backupInstance);
						if (backupInstance != instance) {
							RepresentationPlugin representationPlugin = (RepresentationPlugin) plugin;
							for (Parameter<?> parameter : representationPlugin.getParameters()) {
								if (representationPlugin.isPreviewable(parameter)) {
									representationPreviewInstance.setParameterValue(parameter, instance.getParameterValue(parameter));
								}
							}
						}
					} else {
						representationPreviewInstance = new PluginInstance<>((PluginInstance<RepresentationPlugin>) instance);
					}
				} else {
					representationPreviewInstance = null;
				}
			}
			String title = plugin.getName();
			if (editCount > 0) title = title.concat("*");
			setBorder(title, samePlugin ? Color.BLACK : GREEN);
		}

		public void init(PluginPanel<P> pluginPanel) {
			this.backupInstance = pluginPanel.backupInstance;
			this.plugin = pluginPanel.plugin;
			int numOfParameters = plugin.getNumOfParameters();
			labels = new JLabel[numOfParameters];
			edited = new boolean[numOfParameters];
			editCount = pluginPanel.editCount;
			editors = new JComponent[numOfParameters];
			uris = pluginPanel.uris;
			boolean hasPreviewableParameters = false;
			for (int i = 0; i < numOfParameters; i++) {
				Parameter<?> parameter = plugin.getParameter(i);
				edited[i] = pluginPanel.edited[i];
				labels[i] = createLabelFor(parameter, edited[i], uris[i]);
				editors[i] = parameter.cloneEditor(pluginPanel.editors[i]);
				parameter.addParameterChangeListener(this, editors[i]);
				editors[i].addFocusListener(FOCUS_TRACKER);
				editors[i].addMouseListener(new PopClickListener(parameter));
				if (plugin.getFamily() == PluginFamily.representation && ((RepresentationPlugin) plugin).isPreviewable(parameter)) {
					hasPreviewableParameters = true;
					parameter.setPreviewUpdater(this, editors[i]);
				}
			}
			if (plugin.getFamily() == PluginFamily.representation) {
				previewCheckBox.setVisible(plugin == backupInstance.getPlugin() && hasPreviewableParameters);
				if (hasPreviewableParameters) {
					representationPreviewInstance = new PluginInstance<>(pluginPanel.getRepresentationPreviewInstance());
				} else {
					representationPreviewInstance = null;
				}
			}
			setBorder(pluginPanel.getTitle(), pluginPanel.getBorderColor());
		}

		public void install() {
			JComponent container = getChild();
			int numOfParameters = plugin.getNumOfParameters();
			GridBagConstraints c = new GridBagConstraints();
			if (numOfParameters == 0) {
				c.gridx = c.gridy = 0;
				c.weightx = 1;
				c.insets.bottom = SPACING * 4;
				container.add(new JLabel("No parameters"), c);
			} else {
				for (int i = 0; i < numOfParameters; i++) {
					Parameter<?> parameter = plugin.getParameter(i);
					c.gridy = i;
					if (i == numOfParameters - 1) {
						c.insets.bottom = SPACING * 4;
					} else {
						c.insets.bottom = SPACING;
					}

					c.gridx = 0;
					c.anchor = parameter.isEditorBaselineProvided() ?
							GridBagConstraints.BASELINE_TRAILING :
							GridBagConstraints.LINE_END;
					c.fill = GridBagConstraints.NONE;
					c.weightx = 0;
					c.insets.right = SPACING;
					container.add(labels[i], c);

					c.gridx = 1;
					c.fill = parameter.isEditorExpandable() ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
					c.weightx = 1;
					c.insets.right = 0;
					container.add(editors[i], c);
				}
			}

			c.gridx = 0;
			if (numOfParameters > 0) c.gridwidth = 2;
			c.anchor = GridBagConstraints.BASELINE_TRAILING;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;
			c.insets.bottom = 0;

			if (plugin.getFamily() == PluginFamily.representation) {
				c.gridy++;
				container.add(previewCheckBox, c);
			}

			c.gridy++;
			container.add(switchButton, c);

			Dimension maximumSize = getPreferredExpandedSize();
			maximumSize.width = Short.MAX_VALUE;
			setMaximumSize(maximumSize);

			container.revalidate();
			container.repaint();
		}

		public void uninstall() {
			disposeEditors();
			getChild().removeAll();
			menu.unload();

			backupInstance = null;
			plugin = null;
			labels = null;
			edited = null;
			editors = null;
			uris = null;
		}

		public void discardEdits() {
			discardEdits(backupInstance);
		}

		public void discardEdits(PluginInstance<P> pluginInstance) {
			if (plugin == pluginInstance.getPlugin()) {
				if (pluginInstance != backupInstance) {
					backupInstance = pluginInstance;
					if (plugin.getFamily() == PluginFamily.representation) {
						representationPreviewInstance = new PluginInstance<>((PluginInstance<RepresentationPlugin>) pluginInstance);
					}
					clearEditedFlags();
				}
				boolean previewEnabled = plugin.getFamily() == PluginFamily.representation &&
						application.getPreviewOwner() == ControlWindow.this;
				if (previewEnabled) {
					application.setPreviewOwner(null);
				}
				pluginInstance.setEditorValues(editors);
				if (previewEnabled) {
					application.setPreviewOwner(ControlWindow.this);
				}
				setBorderColor(Color.BLACK);
			} else {
				uninstall();
				init(pluginInstance);
				install();
			}
		}

		public void discardEdits(Parameter<?> parameter) {
			assert parameter.getPlugin() == backupInstance.getPlugin();
			parameter.setEditorValue(getEditorFor(parameter), backupInstance.getParameterValue(parameter));
		}

		public void unmarkEdits(PluginInstance<P> pluginInstance) {
			backupInstance = pluginInstance;
			clearEditedFlags();
			if (plugin.getFamily() == PluginFamily.representation && representationPreviewInstance != null) {
				RepresentationPlugin representationPlugin = (RepresentationPlugin) plugin;
				boolean hasPreviewableParametetrs = false;
				for (Parameter<?> parameter : representationPlugin.getParameters()) {
					if (!representationPlugin.isPreviewable(parameter)) {
						representationPreviewInstance.setParameterValue(
								parameter,
								pluginInstance.getParameterValue(parameter));
					} else {
						hasPreviewableParametetrs = true;
					}
				}
				previewCheckBox.setVisible(hasPreviewableParametetrs);
			}
			setBorderColor(Color.BLACK);
		}

		public void markEdits(PluginInstance<P> pluginInstance) {
			backupInstance = pluginInstance;
			if (plugin == backupInstance.getPlugin()) {
				updateEditedFlags();
				if (plugin.getFamily() == PluginFamily.representation && representationPreviewInstance != null) {
					RepresentationPlugin representationPlugin = (RepresentationPlugin) plugin;
					boolean hasPreviewableParametetrs = false;
					for (Parameter<?> parameter : representationPlugin.getParameters()) {
						if (!representationPlugin.isPreviewable(parameter)) {
							representationPreviewInstance.setParameterValue(
									parameter,
									backupInstance.getParameterValue(parameter));
						} else {
							hasPreviewableParametetrs = true;
						}
					}
					previewCheckBox.setVisible(hasPreviewableParametetrs);
				}
				setBorderColor(Color.BLACK);
			} else {
				clearEditedFlags();
				if (plugin.getFamily() == PluginFamily.representation) {
					previewCheckBox.setVisible(false);
				}
				setBorderColor(GREEN);
			}
		}

		public void switchPlugin(P otherPlugin) {
			if (plugin != otherPlugin) {
				if (otherPlugin == backupInstance.getPlugin()) {
					discardEdits();
				} else {
					boolean previewEnabled = plugin.getFamily() == PluginFamily.representation &&
							application.getPreviewOwner() == ControlWindow.this;
					if (previewEnabled) {
						application.setPreviewOwner(null);
					}
					PluginInstance<P> backupInstance = this.backupInstance;
					uninstall();
					init(backupInstance, otherPlugin);
					install();
					if (previewEnabled) {
						application.setPreviewOwner(ControlWindow.this);
					}
				}
			}
		}

		public void load(PluginInstance<P> pluginInstance) {
			boolean previewEnabled = plugin.getFamily() == PluginFamily.representation &&
					application.getPreviewOwner() == ControlWindow.this;
			if (previewEnabled) {
				application.setPreviewOwner(null);
			}
			if (plugin != pluginInstance.getPlugin()) {
				PluginInstance<P> backupInstance = this.backupInstance;
				uninstall();
				init(backupInstance, pluginInstance);
				install();
			} else {
				pluginInstance.setEditorValues(editors);
			}
			if (previewEnabled) {
				application.setPreviewOwner(ControlWindow.this);
			}
		}

		public void updatePreview(Parameter<?> subject, Object value) {
			representationPreviewInstance.setParameterValue(subject, value);
			application.preview(ControlWindow.this, subject, value);
		}

		public void parameterChanged(Parameter<?> subject, Object value) {
			if (backupInstance.getPlugin() == plugin) {
				setEditedFlag(subject, !value.equals(backupInstance.getParameterValue(subject)));
			}
		}

		public UriHandle getDocumentationUri(Parameter<?> parameter) {
			assert parameter.getPlugin() == plugin;
			return uris[parameter.getIndex()];
		}

		public void moveFocusTo(Parameter<?> parameter) {
			assert parameter.getPlugin() == plugin;
			setCollapsed(false);
			JComponent editor = editors[parameter.getIndex()];
			editor.requestFocusInWindow();
		}

		public void setSingleParameter(Parameter<?> parameter, Object value) {
			assert parameter.getPlugin() == plugin;
			parameter.setEditorValue(editors[parameter.getIndex()], value);
		}

		public void setGroupOfParameters(List<Object> values) {
			assert values.size() == editors.length;
			int i = 0;
			for (Object value : values) {
				if (value != null) {
					plugin.getParameter(i).setEditorValue(editors[i], value);
				}
				i++;
			}
		}

		public PluginInstance<P> getPluginInstance() {
			if (backupInstance.getPlugin() == plugin && editCount == 0) {
				return backupInstance;
			}

			return new PluginInstance<>(plugin, editors);
		}

		public PluginInstance<RepresentationPlugin> getRepresentationPreviewInstance() {
			return representationPreviewInstance;
		}

		public P getPlugin() {
			return plugin;
		}

		public JMenu getMenu() {
			return menu;
		}

		public boolean isConsistent() {
			int numOfParameters = plugin.getNumOfParameters();
			for (int i = 0; i < numOfParameters; i++) {
				if (!plugin.getParameter(i).isEditorConsistent(editors[i])) {
					setBorderColor(Color.RED);
					return false;
				}
			}
			setBorderColor(backupInstance.getPlugin() == plugin ? Color.BLACK : GREEN);
			return true;
		}

		public JComponent getEditorFor(Parameter<?> parameter) {
			assert parameter.getPlugin() == plugin;
			return editors[parameter.getIndex()];
		}

		public boolean hasEdits() {
			return backupInstance.getPlugin() != plugin || editCount > 0;
		}
	}

	private class RectanglePanel extends CollapsiblePanel implements MenuLoader {
		
		private JuliaFormattedTextField re0Field;
		private JuliaFormattedTextField im0Field;
		private JuliaFormattedTextField re1Field;
		private JuliaFormattedTextField im1Field;
		private JCheckBox forceEqualScalesCheckBox;

		private MutableInteger editCount;

		public RectanglePanel() {
			super(new JPanel(new GridBagLayout()), "Rectangle", Color.BLACK, false);
			editCount = new MutableInteger();
			installDataTransferSupport(this);
			build();
		}
		
		private void build() {
			JComponent container = getChild();

			setOpaque(false);
			container.setOpaque(false);
			
			GridBagConstraints c = new GridBagConstraints();
			
			Decimal zero = new Decimal("0");
			Decimal one = new Decimal("1");

			c.gridx = 0;
			c.anchor = GridBagConstraints.BASELINE_TRAILING;
			c.insets.bottom = SPACING;
			c.insets.right = SPACING;

			c.gridy = 0;
			JLabel re0Label = createLabel("<html>Re<sub>0</sub>");
			container.add(re0Label, c);

			c.gridy = 1;
			JLabel im0Label = createLabel("<html>Im<sub>0</sub>");
			container.add(im0Label, c);

			c.gridy = 2;
			JLabel re1Label = createLabel("<html>Re<sub>1</sub>");
			container.add(re1Label, c);

			c.gridy = 3;
			JLabel im1Label = createLabel("<html>Im<sub>1</sub>");
			container.add(im1Label, c);

			c.gridx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
			c.insets.right = 0;

			c.gridy = 0;
			re0Field = new JuliaFormattedTextField(new DecimalFormatter(), zero, null);
			re0Field.addParameterChangeListener(new LabelChanger(re0Label, editCount) {
				public Object getValue() {
					return application.getRectangle().getRe0();
				}
			});
			re0Field.addFocusListener(FOCUS_TRACKER);
			container.add(re0Field, c);

			c.gridy = 1;
			im0Field = new JuliaFormattedTextField(new DecimalFormatter(), zero, null);
			im0Field.addParameterChangeListener(new LabelChanger(im0Label, editCount) {
				public Object getValue() {
					return application.getRectangle().getIm0();
				}
			});
			im0Field.addFocusListener(FOCUS_TRACKER);
			container.add(im0Field, c);

			c.gridy = 2;
			re1Field = new JuliaFormattedTextField(new DecimalFormatter(), one, null);
			re1Field.addParameterChangeListener(new LabelChanger(re1Label, editCount) {
				public Object getValue() {
					return application.getRectangle().getRe1();
				}
			});
			re1Field.addFocusListener(FOCUS_TRACKER);
			container.add(re1Field, c);

			c.gridy = 3;
			im1Field = new JuliaFormattedTextField(new DecimalFormatter(), one, null);
			im1Field.addParameterChangeListener(new LabelChanger(im1Label, editCount) {
				public Object getValue() {
					return application.getRectangle().getIm1();
				}
			});
			im1Field.addFocusListener(FOCUS_TRACKER);
			container.add(im1Field, c);

			forceEqualScalesCheckBox = new JCheckBox("Force equal scales", false);
			forceEqualScalesCheckBox.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					toggleAsteriskFromCheckBox();
				}
			});
			forceEqualScalesCheckBox.addFocusListener(FOCUS_TRACKER);
			forceEqualScalesCheckBox.setOpaque(false);
			c.gridx = 0;
			c.gridy = 4;
			c.gridwidth = 2;
			c.fill = GridBagConstraints.NONE;
			c.insets.bottom = 0;
			container.add(forceEqualScalesCheckBox, c);

			Dimension maximumSize = getPreferredExpandedSize();
			maximumSize.width = Short.MAX_VALUE;
			setMaximumSize(maximumSize);
		}

		public void loadMenu(JPopupMenu menu) {
			addDataTransferItems(menu, this);

			menu.addSeparator();

			JMenuItem menuItem = new JMenuItem("Set to default (Mandelbrot set)");
			menuItem.setMnemonic(KeyEvent.VK_M);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setToMandelbrotDefault();
				}
			});
			menu.add(menuItem);
			
			menuItem = new JMenuItem("Set to default (Julia set)");
			menuItem.setMnemonic(KeyEvent.VK_J);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setToJuliaDefault();
				}
			});
			menu.add(menuItem);

			menuItem = new JMenuItem("Edit");
			menuItem.setMnemonic(KeyEvent.VK_E);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					moveFocusTo();
				}
			});
			menu.add(menuItem);
			
			menuItem = new JMenuItem("Discard edits");
			menuItem.setMnemonic(KeyEvent.VK_R);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					discardEdits();
				}
			});
			menu.add(menuItem);
		}

		private void setRectangleImpl(Rectangle rectangle) {
			re0Field.setValue(rectangle.getRe0());
			im0Field.setValue(rectangle.getIm0());
			re1Field.setValue(rectangle.getRe1());
			im1Field.setValue(rectangle.getIm1());
		}

		private void setRectangleImpl(Rectangle rectangle, boolean forceEqualScales) {
			setRectangleImpl(rectangle);
			forceEqualScalesCheckBox.setSelected(forceEqualScales);
		}

		public void init(Rectangle rectangle, boolean forceEqualScales) {
			setRectangleImpl(rectangle, forceEqualScales);
			markEdits();
		}

		public void load(Rectangle rectangle, boolean forceEqualScales) {
			setRectangleImpl(rectangle, forceEqualScales);
		}

		public void init(RectanglePanel rectanglePanel) {
			copyState(rectanglePanel.re0Field, re0Field);
			copyState(rectanglePanel.im0Field, im0Field);
			copyState(rectanglePanel.re1Field, re1Field);
			copyState(rectanglePanel.im1Field, im1Field);
			forceEqualScalesCheckBox.setSelected(rectanglePanel.getForceEqualScales());
			markEdits();
		}

		public boolean hasEdits() {
			return editCount.get() > 0;
		}

		public boolean hasRectangleEdits() {
			return editCount.get() > 1 ||
					(editCount.get() == 1 && application.getForceEqualScales() == forceEqualScalesCheckBox.isSelected());
		}

		public void setToJuliaDefault() {
			setRectangleImpl(formulaPanel.getPlugin().getDefaultJuliaSetRectangle());
		}

		public void setToMandelbrotDefault() {
			setRectangleImpl(formulaPanel.getPlugin().getDefaultMandelbrotSetRectangle());
		}

		public void discardEdits() {
			setRectangleImpl(application.getRectangle(), application.getForceEqualScales());
			if (editCount.get() > 0) {
				markEdits();
			}
		}

		public void moveFocusTo() {
			setCollapsed(false);
			re0Field.requestFocusInWindow();
		}

		public void unmarkEdits(boolean zoomIn) {
			if (zoomIn) {
				discardEdits();
			} else if (editCount.get() > 0) {
				markEdits();
			}
		}

		public void markEdits() {
			re0Field.postChangeEvent();
			im0Field.postChangeEvent();
			re1Field.postChangeEvent();
			im1Field.postChangeEvent();
			toggleAsteriskFromCheckBox();
		}

		public boolean isConsistent() {
			if (!re0Field.isEditValid() || !im0Field.isEditValid() || !re1Field.isEditValid() || !im1Field.isEditValid() ||
					Rectangle.isEmptyRectangle(getRe0(), getIm0(), getRe1(), getIm1())) {
				setBorderColor(Color.RED);
				return false;
			}
			setBorderColor(Color.BLACK);
			return true;
		}

		public Decimal getRe0() {
			return (Decimal) re0Field.getValue();
		}

		public Decimal getIm0() {
			return (Decimal) im0Field.getValue();
		}

		public Decimal getRe1() {
			return (Decimal) re1Field.getValue();
		}

		public Decimal getIm1() {
			return (Decimal) im1Field.getValue();
		}

		public boolean getForceEqualScales() {
			return forceEqualScalesCheckBox.isSelected();
		}

		private void toggleAsteriskFromCheckBox() {
			boolean edited = forceEqualScalesCheckBox.getText().endsWith("*");
			if (forceEqualScalesCheckBox.isSelected() != application.getForceEqualScales()) {
				if (!edited) {
					String text = forceEqualScalesCheckBox.getText();
					String modifiedText = text.concat("*");
					forceEqualScalesCheckBox.setText(modifiedText);
					if (editCount.getAndIncrement() == 0) {
						text = getTitle();
						modifiedText = text.concat("*");
						setTitle(modifiedText);
					}
				}
			} else {
				if (edited) {
					String modifiedText = forceEqualScalesCheckBox.getText();
					String text = modifiedText.substring(0, modifiedText.length() - 1);
					forceEqualScalesCheckBox.setText(text);
					if (editCount.decrementAndGet() == 0) {
						modifiedText = getTitle();
						text = modifiedText.substring(0, modifiedText.length() - 1);
						setTitle(text);
					}
				}
			}
		}
	}


	private class JuliaSetPointPanel extends CollapsiblePanel implements MenuLoader {
		
		private JuliaFormattedTextField reCField;
		private JuliaFormattedTextField imCField;
		private SetToSelectionAction setToSelectionAction;

		private MutableInteger editCount;
		
		private class SetToSelectionAction extends AbstractAction {
			public SetToSelectionAction() {
				putValue(NAME, "Set to selection");
				putValue(MNEMONIC_KEY, KeyEvent.VK_S);
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				JuliaSetPoint selection = application.getCurrentSelectionCenter();
				if (selection != null) {
					setJuliaSetPointImpl(selection);
				}
			}
		}

		public JuliaSetPointPanel() {
			super(new JPanel(new GridBagLayout()), "Julia set point", Color.BLACK, false);
			editCount = new MutableInteger();
			installDataTransferSupport(this);
			build();
		}

		private void build() {
			JComponent container = getChild();

			setOpaque(false);
			container.setOpaque(false);

			GridBagConstraints c = new GridBagConstraints();

			Decimal zero = new Decimal("0");

			c.gridx = 0;
			c.anchor = GridBagConstraints.BASELINE_TRAILING;
			c.insets.bottom = SPACING;
			c.insets.right = SPACING;

			c.gridy = 0;
			JLabel reCLabel = createLabel("<html>Re<sub>c</sub>");
			container.add(reCLabel, c);

			c.gridy = 1;
//			c.insets.bottom = 0;
			JLabel imCLabel = createLabel("<html>Im<sub>c</sub>");
			container.add(imCLabel, c);

			c.gridx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
//			c.insets.bottom = SPACING;
			c.insets.right = 0;

			c.gridy = 0;
			reCField = new JuliaFormattedTextField(new DecimalFormatter(), zero, null);
			reCField.addParameterChangeListener(new LabelChanger(reCLabel, editCount) {
				public Object getValue() {
					if (application.getJuliaSetPoint() != null) {
						return application.getJuliaSetPoint().getRe();
					}

					return reCField.getValue();
				}
			});
			reCField.addFocusListener(FOCUS_TRACKER);
			container.add(reCField, c);

			c.gridy = 1;
//			c.insets.bottom = 0;
			imCField = new JuliaFormattedTextField(new DecimalFormatter(), zero, null);
			imCField.addParameterChangeListener(new LabelChanger(imCLabel, editCount) {
				public Object getValue() {
					if (application.getJuliaSetPoint() != null) {
						return application.getJuliaSetPoint().getIm();
					}

					return imCField.getValue();
				}
			});
			imCField.addFocusListener(FOCUS_TRACKER);
			container.add(imCField, c);

			c.gridx = 0;
			c.gridy = 2;
			c.gridwidth = 2;
			c.insets.bottom = 0;
			c.fill = GridBagConstraints.NONE;
			if (setToSelectionAction == null) {
				setToSelectionAction = new SetToSelectionAction();
			}
			JButton setToSelectionButton = new JButton(setToSelectionAction);
			setToSelectionButton.setMnemonic(0);
			setToSelectionButton.addFocusListener(FOCUS_TRACKER);
			container.add(setToSelectionButton, c);

			Dimension maximumSize = getPreferredExpandedSize();
			maximumSize.width = Short.MAX_VALUE;
			setMaximumSize(maximumSize);
		}

		public void loadMenu(JPopupMenu menu) {
			addDataTransferItems(menu, this);

			menu.addSeparator();

			boolean enabled = isVisible();
			final JMenuItem enabledItem = new JCheckBoxMenuItem("Enabled", enabled);
			enabledItem.setMnemonic(KeyEvent.VK_B);
			menu.add(enabledItem);

			final JMenuItem setToDefaultItem = new JMenuItem("Set to default");
			setToDefaultItem.setMnemonic(KeyEvent.VK_D);
			setToDefaultItem.setEnabled(enabled);
			setToDefaultItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setToDefault();
				}
			});
			menu.add(setToDefaultItem);

			if (setToSelectionAction == null) {
				setToSelectionAction = new SetToSelectionAction();
			}
			setToSelectionAction.setEnabled(enabled);
			menu.add(setToSelectionAction);

			final JMenuItem editItem = new JMenuItem("Edit");
			editItem.setMnemonic(KeyEvent.VK_E);
			editItem.setEnabled(enabled);
			editItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					moveFocusTo();
				}
			});
			menu.add(editItem);

			final JMenuItem discardEditsItem = new JMenuItem("Discard edits");
			discardEditsItem.setMnemonic(KeyEvent.VK_R);
			discardEditsItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					discardEdits();
//					enabledItem.setSelected(isVisible());
				}
			});
			menu.add(discardEditsItem);

			enabledItem.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					boolean enabled = e.getStateChange() == ItemEvent.SELECTED;
					setToDefaultItem.setEnabled(enabled);
					setToSelectionAction.setEnabled(enabled);
					editItem.setEnabled(enabled);
					setVisible(enabled);
				}
			});

			addComponentListener(new ComponentAdapter() {
				@Override
				public void componentShown(ComponentEvent e) {
					enabledItem.setSelected(true);
				}
				@Override
				public void componentHidden(ComponentEvent e) {
					enabledItem.setSelected(false);
				}
			});
		}

		public void init(JuliaSetPoint c) {
			if (c != null) {
				setJuliaSetPointImpl(c);
			} else {
				setJuliaSetPointImpl(formulaPanel.getPlugin().getDefaultJuliaSetPoint());
			}
			setVisible(c != null);
			
			markEdits();
		}

		public void load(JuliaSetPoint c) {
			if (c != null) {
				setJuliaSetPointImpl(c);
			}
			setVisible(c != null);
		}

		public void init(JuliaSetPointPanel juliaSetPointPanel) {
			setVisible(juliaSetPointPanel.isVisible());
			copyState(juliaSetPointPanel.reCField, reCField);
			copyState(juliaSetPointPanel.imCField, imCField);
			markEdits();
		}

		public Decimal getReC() {
			return (Decimal) reCField.getValue();
		}

		public Decimal getImC() {
			return (Decimal) imCField.getValue();
		}

		public boolean hasEdits() {
			return (application.getJuliaSetPoint() == null && isVisible()) ||
					(application.getJuliaSetPoint() != null && !isVisible()) ||
					editCount.get() > 0;
		}

		private void setJuliaSetPointImpl(JuliaSetPoint c) {
			reCField.setValue(c.getRe());
			imCField.setValue(c.getIm());
		}

		public void setToDefault() {
			setJuliaSetPointImpl(formulaPanel.getPlugin().getDefaultJuliaSetPoint());
		}

		public void unmarkEdits() {
			if (isVisible()) {
				setBorderColor(Color.BLACK);
			} else {
				setBorderColor(GREEN);
			}
			if (editCount.get() > 0) {
				reCField.postChangeEvent();
				imCField.postChangeEvent();
			}
		}

		public void markEdits() {
			if (application.getJuliaSetPoint() != null) {
				setBorderColor(Color.BLACK);
			} else {
				setBorderColor(GREEN);
			}
			reCField.postChangeEvent();
			imCField.postChangeEvent();
		}

		public boolean isConsistent() {
			if (isVisible()) {
				if (!reCField.isEditValid() || !imCField.isEditValid()) {
					setBorderColor(Color.RED);
					return false;
				}
				setBorderColor(application.getJuliaSetPoint() != null ? Color.BLACK : GREEN);
			}
			return true;
		}

		public void discardEdits() {
			if (application.getJuliaSetPoint() != null) {
				setJuliaSetPointImpl(application.getJuliaSetPoint());
				if (editCount.get() > 0) {
					reCField.postChangeEvent();
					imCField.postChangeEvent();
				}
				setBorderColor(Color.BLACK);
				setVisible(true);
			} else {
				setVisible(false);
				if (editCount.get() > 0) {
					reCField.postChangeEvent();
					imCField.postChangeEvent();
				}
				setBorderColor(GREEN);
			}
		}

		public void moveFocusTo() {
			setCollapsed(false);
			reCField.requestFocusInWindow();
		}
	}

	private void applyEdits() {
		application.apply(this, false);
	}

	private void zoomIn() {
		application.apply(this, true);
	}

	public void discardEdits() {
		Application.Image currentImage = application.getCurrentImage();
		numberFactoryPanel.discardEdits(currentImage.getNumberFactoryInstance());
		formulaPanel.discardEdits(currentImage.getFormulaInstance());
		representationPanel.discardEdits(currentImage.getRepresentationInstance());
		rectanglePanel.discardEdits();
		juliaSetPointPanel.discardEdits();
	}

	private void cloneCw() {
		// First of all, let's compute the title
		Pattern pattern = Pattern.compile("(.*) \\([1-9][0-9]*\\)");
		Matcher matcher = pattern.matcher(getTitle());
		String title = matcher.matches() ? matcher.group(1) : getTitle();
		pattern = Pattern.compile(Pattern.quote(title) + " \\(([1-9][0-9]*)\\)");
		int max = 0;
		for (ControlWindow cw : application.getControlWindows()) {
			matcher = pattern.matcher(cw.getTitle());
			if (matcher.matches()) {
				int number = Integer.valueOf(matcher.group(1));
				if (number > max) {
					max = number;
				}
			}
		}
		title = title + " (" + (max + 1) + ")";

		ControlWindow clone = application.createControlWindow(title);
		clone.setTitle(title);
		clone.init(this);
		clone.install();
		clone.getContentPane().setPreferredSize(getContentPane().getSize());
		clone.pack();
		clone.setVisible(true);
	}

	private void hideCw() {
		setVisible(false);
	}

	private void disposeCw() {
		application.disposeControlWindow(this);
	}

	private void renameCw() {
		String input = (String) JOptionPane.showInputDialog(this,
				"Enter a name for this window:",
				"Rename",
				JOptionPane.QUESTION_MESSAGE,
				null,
				null,
				getTitle());

		if (input != null && !input.isEmpty()) {
			setTitle(input);
		}
	}

	private void save() {
		JFileChooser fc = new JFileChooser();
		FileFilter jimFilter = new FileNameExtensionFilter("Julia image w/o intermediate data (*.jim)", "jim");
		fc.setFileFilter(jimFilter);
		fc.setDialogType(JFileChooser.SAVE_DIALOG);
		int rv = showSaveDialog(fc, this);
		if (rv == JFileChooser.APPROVE_OPTION) {
			application.saveImage(this, fc.getSelectedFile());
		}
	}

	private void load() {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileNameExtensionFilter("Julia image (*.jim)", "jim"));
		int rv = fc.showOpenDialog(this);
		if (rv == JFileChooser.APPROVE_OPTION) {
			application.loadImage(fc.getSelectedFile(), this);
		}
	}	

	private void init(ControlWindow cw) {
		numberFactoryPanel.init(cw.numberFactoryPanel);
		formulaPanel.init(cw.formulaPanel);
		representationPanel.init(cw.representationPanel);
		rectanglePanel.init(cw.rectanglePanel);
		juliaSetPointPanel.init(cw.juliaSetPointPanel);
	}

	public void init(Application.Image image) {
		numberFactoryPanel.init(application.getNumberFactoryInstance(), image.getNumberFactoryInstance());
		formulaPanel.init(application.getFormulaInstance(), image.getFormulaInstance());
		representationPanel.init(application.getRepresentationInstance(), image.getRepresentationInstance());
		rectanglePanel.init(image.getRectangle(), image.getForceEqualScales());
		juliaSetPointPanel.init(image.getJuliaSetPoint());
	}

	public void init(NumberFactoryPlugin numberFactoryPlugin,
			FormulaPlugin formulaPlugin,
			RepresentationPlugin representationPlugin,
			boolean isJuliaSet) {
		numberFactoryPanel.init(application.getNumberFactoryInstance(), numberFactoryPlugin);
		formulaPanel.init(application.getFormulaInstance(), formulaPlugin);
		representationPanel.init(application.getRepresentationInstance(), representationPlugin);
		rectanglePanel.init(isJuliaSet ? formulaPlugin.getDefaultJuliaSetRectangle() : formulaPlugin.getDefaultMandelbrotSetRectangle(), false);
		juliaSetPointPanel.init(isJuliaSet ? formulaPlugin.getDefaultJuliaSetPoint() : null);
	}

	public void install() {
		numberFactoryPanel.install();
		formulaPanel.install();
		representationPanel.install();
		connectionToggler.setSelected(false);
		previewCheckBox.setSelected(false);
		application.getConnectionTogglerGroup().add(connectionToggler);
		application.getPreviewCheckBoxGroup().add(previewCheckBox);
	}

	public void uninstall() {
		numberFactoryPanel.uninstall();
		formulaPanel.uninstall();
		representationPanel.uninstall();
		application.getConnectionTogglerGroup().remove(connectionToggler);
		application.getPreviewCheckBoxGroup().remove(previewCheckBox);
	}

	public boolean isConsistent() {
		return numberFactoryPanel.isConsistent() &&
				formulaPanel.isConsistent() &&
				representationPanel.isConsistent() &&
				rectanglePanel.isConsistent() &&
				juliaSetPointPanel.isConsistent();
	}

	public void setDisposeButtonEnabled(boolean enabled) {
		disposeButton.setEnabled(enabled);
	}

	public void setConnectionTogglerGroup(JuliaButtonGroup buttonGroup) {
		buttonGroup.add(connectionToggler);
	}

	public void setPreviewCheckBoxGroup(JuliaButtonGroup buttonGroup) {
		buttonGroup.add(previewCheckBox);
	}

	public NumberFactoryPlugin getNumberFactory() {
		return numberFactoryPanel.getPlugin();
	}

	public PluginInstance<NumberFactoryPlugin> getNumberFactoryInstance() {
		return numberFactoryPanel.getPluginInstance();
	}

	public FormulaPlugin getFormula() {
		return formulaPanel.getPlugin();
	}

	public PluginInstance<FormulaPlugin> getFormulaInstance() {
		return formulaPanel.getPluginInstance();
	}

	public RepresentationPlugin getRepresentation() {
		return representationPanel.getPlugin();
	}

	public PluginInstance<RepresentationPlugin> getRepresentationInstance() {
		return representationPanel.getPluginInstance();
	}

	public PluginInstance<RepresentationPlugin> getRepresentationPreviewInstance() {
		return representationPreviewInstance;
	}

	public Rectangle getRectangle() {
		if (rectanglePanel.hasRectangleEdits()) {
			return new Rectangle(
					rectanglePanel.getRe0(),
					rectanglePanel.getIm0(),
					rectanglePanel.getRe1(),
					rectanglePanel.getIm1());
		}

		return application.getRectangle();
	}

	public boolean getForceEqualScales() {
		return rectanglePanel.getForceEqualScales();
	}

	public JuliaSetPoint getJuliaSetPoint() {
		if (juliaSetPointPanel.isVisible()) {
			if (juliaSetPointPanel.hasEdits()) {
				return new JuliaSetPoint(
						juliaSetPointPanel.getReC(),
						juliaSetPointPanel.getImC());
			}
			
			return application.getJuliaSetPoint();
		}

		return null;
	}

	public Application.Image getImage() {
		return new Application.Image(
				getNumberFactoryInstance(),
				getFormulaInstance(),
				getRepresentationInstance(),
				getRectangle(),
				getForceEqualScales(),
				getJuliaSetPoint());
	}

	public JMenuItem getItemInWindowMenu() {
		return itemInWindowMenu;
	}

	public boolean hasEdits() {
		return numberFactoryPanel.hasEdits() ||
				formulaPanel.hasEdits() ||
				representationPanel.hasEdits() ||
				rectanglePanel.hasEdits() ||
				juliaSetPointPanel.hasEdits();
	}

	public void unmarkEdits(boolean zoomIn) {
		Application.Image currentImage = application.getCurrentImage();
		numberFactoryPanel.unmarkEdits(currentImage.getNumberFactoryInstance());
		formulaPanel.unmarkEdits(currentImage.getFormulaInstance());
		representationPanel.unmarkEdits(currentImage.getRepresentationInstance());
		rectanglePanel.unmarkEdits(zoomIn);
		juliaSetPointPanel.unmarkEdits();
	}

	public void markEdits() {
		Application.Image currentImage = application.getCurrentImage();
		numberFactoryPanel.markEdits(currentImage.getNumberFactoryInstance());
		formulaPanel.markEdits(currentImage.getFormulaInstance());
		representationPanel.markEdits(currentImage.getRepresentationInstance());
		rectanglePanel.markEdits();
		juliaSetPointPanel.markEdits();
	}

	public void load(Application.Image image) {
		if (image.getNumberFactoryInstance() != null)
			numberFactoryPanel.load(image.getNumberFactoryInstance());
		if (image.getFormulaInstance() != null)
			formulaPanel.load(image.getFormulaInstance());
		if (image.getRepresentationInstance() != null)
			representationPanel.load(image.getRepresentationInstance());
		if (image.getRectangle() != null)
			rectanglePanel.load(image.getRectangle(), image.getForceEqualScales());
		juliaSetPointPanel.load(image.getJuliaSetPoint());
	}

	public void setConnected(boolean connected) {
		connectionToggler.setSelected(connected);
	}

	public boolean isConnected() {
		return connectionToggler.isSelected();
	}
}

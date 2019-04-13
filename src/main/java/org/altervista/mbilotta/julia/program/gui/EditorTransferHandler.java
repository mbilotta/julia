package org.altervista.mbilotta.julia.program.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.altervista.mbilotta.julia.program.parsers.Parameter;


public class EditorTransferHandler extends TransferHandler {
	
	private final Parameter<?> parameter;
	private final DataFlavor parameterFlavor;
	
	public EditorTransferHandler(Parameter<?> parameter) {
		this.parameter = parameter;
		this.parameterFlavor = new DataFlavor(
				parameter.getType(),
				parameter.getType().getSimpleName());
	}

	@Override
	public boolean importData(TransferSupport support) {
		Object data;
		try {
			data = support.getTransferable().getTransferData(parameterFlavor);
		} catch (UnsupportedFlavorException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

		JComponent component = (JComponent) support.getComponent();
		parameter.setEditorValue(component, data);
		return true;
	}

	@Override
	public boolean canImport(TransferSupport support) {
		return support.isDataFlavorSupported(parameterFlavor);
	}

	@Override
	public int getSourceActions(JComponent c) {
		return COPY_OR_MOVE;
	}

	@Override
	protected Transferable createTransferable(JComponent c) {
		final Object data = parameter.getEditorValue(c);
		return new Transferable() {
			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return flavor.equals(parameterFlavor);
			}
			
			@Override
			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[]{ parameterFlavor };
			}
			
			@Override
			public Object getTransferData(DataFlavor flavor)
					throws UnsupportedFlavorException, IOException {
				if (flavor.equals(parameterFlavor)) {
					return data; 
				}
				throw new UnsupportedFlavorException(flavor);
			}
		};
	}
}

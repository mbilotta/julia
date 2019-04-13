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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import org.altervista.mbilotta.julia.program.gui.MessagePane;


public abstract class BlockingSwingWorker<T> extends SwingWorker<T, String> {

	private JLabel noteLabel;
	private JProgressBar progressBar;
	private JDialog dialog;

	public BlockingSwingWorker() {
		addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if (e.getPropertyName().equals("progress")) {
					if (progressBar == null) {
						progressBar = new JProgressBar(0, 100);
					}
					progressBar.setValue((Integer) e.getNewValue());
				}
			}
		});
	}

	public void cancel() {
		cancel(true);
	}

	public final void block(Component parent, Object message, String note) {
		if (dialog == null) {
			if (noteLabel == null && note != null) {
				noteLabel = new JLabel(note);
			}

			if (progressBar == null) {
				progressBar = new JProgressBar(0, 100);
				progressBar.setValue(getProgress());
			}

			final JButton cancelButton = new JButton(UIManager.getString("OptionPane.cancelButtonText"));
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					cancel();
					cancelButton.setEnabled(false);
				}
			});

			JOptionPane optionPane = new JOptionPane(
					new Object[] { message, noteLabel, progressBar },
					JOptionPane.INFORMATION_MESSAGE,
					JOptionPane.DEFAULT_OPTION,
					null,
					new Object[] { cancelButton },
					null);
			
			dialog = optionPane.createDialog(parent, "Julia");
			dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			dialog.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					cancel();
					cancelButton.setEnabled(false);
				}
			});

			dialog.setVisible(true);
		}
	}

	@Override
	protected final void process(List<String> chunks) {
		String note = chunks.get(chunks.size() - 1);
		if (noteLabel == null) {
			noteLabel = new JLabel();
		}
		noteLabel.setText(note);
	}

	@Override
	protected final void done() {
		try {
			processResult(get());
		} catch (ExecutionException e) {
			processException(e.getCause());
		} catch (CancellationException e) {
			processCancellation();
		} catch (InterruptedException e) {}

		if (dialog != null) {
			dialog.setVisible(false);
			dialog.dispose();
			dialog = null;
		}
	}
	
	protected void processResult(T result) {}
	protected void processException(Throwable e) {
		MessagePane.showErrorMessage(getBlockingDialog(),
				"Julia",
				"Operation failed. See the details.",
				e);
	}
	protected void processCancellation() {
		JOptionPane.showMessageDialog(getBlockingDialog(),
				"Operation canceled.",
				"Julia",
				JOptionPane.INFORMATION_MESSAGE);
	}

	protected final JDialog getBlockingDialog() {
		return dialog;
	}

}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ProgressDialog.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.user.ui;

import java.awt.BorderLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JDesktopPane;

/**
 * This class displays a progress dialog.
 */
public class ProgressDialog extends JInternalFrame
{
    private JProgressBar progressBar;
    private JTextArea taskOutput;
	private JFrame jf;
	/**
	 * The constructor displays the progress dialog.
	 * @param title the title of the dialog.
	 */
	public ProgressDialog(String title)
	{
		super(title);
		setSize(300, 80);
		setLocation(300, 300);

		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);

		taskOutput = new JTextArea();
		taskOutput.setMargin(new Insets(5,5,5,5));
		taskOutput.setEditable(false);
		taskOutput.setCursor(null);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(progressBar, BorderLayout.CENTER);
		panel.add(taskOutput, BorderLayout.SOUTH);

		if(TopLevel.getMode()==TopLevel.MDIMode)
		{
			this.getContentPane().add(panel);
			show();
			JDesktopPane desktop = TopLevel.getDesktop();
			desktop.add(this);
		}
		else
		{	
			jf = new JFrame();
			jf.setSize(300,80);
			jf.getContentPane().add(panel);
			jf.setLocation(300,300);
			jf.show();	
		}
		moveToFront();
	}

	/**
	 * Routine to terminate the progress dialog.
	 */
	public void close()
	{
		if(TopLevel.getMode()==TopLevel.MDIMode)
		{
			JDesktopPane desktop = TopLevel.getDesktop();
			dispose();
		}
		else
		{
			jf.dispose();
		}		
	}

	/**
	 * Routine to set the progress amount.
	 * @param progress the amount of progress (from 0 to 100);
	 */
	public void setProgress(int progress)
	{
		progressBar.setValue(progress);
	}

	/**
	 * Routine to return the progress amount.
	 * @return the amount of progress (from 0 to 100);
	 */
	public int getProgress()
	{
		return progressBar.getValue();
	}

	/**
	 * Routine to set a text message in the progress dialog.
	 * @param note the message to display.
	 */
	public void setNote(String note)
	{
		taskOutput.setText(note);
	}

	/**
	 * Routine to return the text message in the progress dialog.
	 * @return the message currently being displayed.
	 */
	public String getNote()
	{
		return taskOutput.getText();
	}
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Progress.java
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.BorderLayout;
import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;

/**
 * This class displays a progress dialog.
 */
public class Progress
{
    private JProgressBar progressBar;
    private JTextArea taskOutput;
	private JFrame jf;
	private JInternalFrame jif;

	/**
	 * The constructor displays the progress dialog.
	 * @param title the title of the dialog.
	 */
	public Progress(String title)
	{
		if (TopLevel.isMDIMode())
		{
			jif = new JInternalFrame(title);
			jif.setSize(300, 80);
			jif.setLocation(300, 300);
		} else
		{
			jf = new JFrame(title);
			jf.setSize(300, 80);
			jf.setLocation(300, 300);
		}

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

		if(TopLevel.isMDIMode())
		{
			jif.getContentPane().add(panel);
			jif.show();
			TopLevel.addToDesktop(jif);
			jif.moveToFront();
		} else
		{	
			jf.getContentPane().add(panel);
			jf.show();	
		}
	}

	/**
	 * Method to terminate the progress dialog.
	 */
	public void close()
	{
		if (TopLevel.isMDIMode())
		{
			jif.dispose();
		} else
		{
			jf.dispose();
		}		
	}

	/**
	 * Method to set the progress amount.
	 * @param progress the amount of progress (from 0 to 100);
	 */
	public void setProgress(int progress)
	{
		if (progress < 0) progress = 0;
		if (progress > 100) progress = 100;
		progressBar.setValue(progress);
	}

	/**
	 * Method to return the progress amount.
	 * @return the amount of progress (from 0 to 100);
	 */
	public int getProgress()
	{
		return progressBar.getValue();
	}

	/**
	 * Method to set a text message in the progress dialog.
	 * @param note the message to display.
	 */
	public void setNote(String note)
	{
		taskOutput.setText(note);
	}

	/**
	 * Method to return the text message in the progress dialog.
	 * @return the message currently being displayed.
	 */
	public String getNote()
	{
		return taskOutput.getText();
	}
}

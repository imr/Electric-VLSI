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
import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JDesktopPane;

/**
 */
public class ProgressDialog extends JInternalFrame
{
    private JProgressBar progressBar;
    private JTextArea taskOutput;

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

		this.getContentPane().add(panel);
		show();
		JDesktopPane desktop = UITopLevel.getDesktop();
		desktop.add(this); 
		moveToFront();
	}

	public void close()
	{
		JDesktopPane desktop = UITopLevel.getDesktop();
		desktop.remove(this); 
		dispose();
	}

	public void setProgress(int progress)
	{
		progressBar.setValue(progress);
	}

	public int getProgress()
	{
		return progressBar.getValue();
	}
	
	public void setNote(String note)
	{
		taskOutput.setText(note);
	}
	
	public String getNote()
	{
		return taskOutput.getText();
	}
}

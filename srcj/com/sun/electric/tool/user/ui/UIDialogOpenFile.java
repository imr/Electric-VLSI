/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UIDialogOpenFile.java
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

import com.sun.electric.tool.user.ui.UIFileFilter;

import javax.swing.*;
import java.io.File;

public class UIDialogOpenFile extends JFileChooser
{
	/** The file extension associated with this dialog */			private String extension;
	/** The description of files associated with this dialog */		private String description;
	/** True if this is a file save dialog */						private boolean saveDialog;

	public static final UIDialogOpenFile ANY = new UIDialogOpenFile(null, "Any File");
	public static final UIDialogOpenFile TEXT = new UIDialogOpenFile("txt", "Text File");
	public static final UIDialogOpenFile ELIB = new UIDialogOpenFile("elib", "Library File");

	/**
	 * Constructor creates a link to files with the extension "extension" and
	 * description "description".
	 *
	 * After construction, the "chooseInputFile()" or "chooseOutputFile()" methods
	 * may be called to return a selected input or output file.
	 */
	public UIDialogOpenFile(String extension, String description)
	{
		this.extension = extension;
		this.description = description;
	}

	/**
	 * Routine to invoke an "open file" dialog and return the selected file.
	 */
	public String chooseInputFile(String newDescription)
	{
		saveDialog = false;
		if (newDescription == null) setDialogTitle("Read " + description); else
			setDialogTitle(newDescription);
		if (extension != null)
		{
			UIFileFilter filter = new UIFileFilter();
			filter.addExtension(extension);
			filter.setDescription(description);
			setFileFilter(filter);
		}
		int returnVal = showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File file = getSelectedFile();
			return file.getPath();
		}
		return null;
	}

	/**
	 * Routine to invoke a "save file" dialog and return the selected file.
	 */
	public String chooseOutputFile(String initialFile)
	{
		saveDialog = true;
		setDialogTitle("Write " + description);
		UIFileFilter filter = new UIFileFilter();
		if (extension != null) filter.addExtension(extension);
		filter.setDescription(description);
		setFileFilter(filter);
		setSelectedFile(new File(initialFile));
		int returnVal = showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File file = getSelectedFile();
			return file.getPath();
		}
		return null;
	}

	/**
	 * Routine called when the user clicks "ok" during file choosing.
	 * Prevents overwriting of existing files.
	 */
	public void approveSelection()
	{
		File f = getSelectedFile();
		if (saveDialog)
		{
			String filename = f.getName();
			if (f.exists())
			{
				int result = JOptionPane.showConfirmDialog(this, "The file "+filename+" already exists, would you like to overwrite it?",
					"Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (result != JOptionPane.OK_OPTION) return;
			}
		}
		setSelectedFile(f);
		super.approveSelection();
	}
}

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

public class UIDialogOpenFile
{
	private String extension;
	private String description;

	public static final UIDialogOpenFile TEXT = new UIDialogOpenFile(null, "Any File");
	public static final UIDialogOpenFile ELIB = new UIDialogOpenFile("elib", "Library File");

	public UIDialogOpenFile(String extension, String description)
	{
		this.extension = extension;
		this.description = description;
	}

	public String chooseInputFile()
	{
		JFileChooser fc = new JFileChooser();
		UIFileFilter filter = new UIFileFilter();
		if (extension != null) filter.addExtension(extension);
		fc.setDialogTitle("Read " + description);
		filter.setDescription(description);
		fc.setFileFilter(filter);
		int returnVal = fc.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File file = fc.getSelectedFile();
			return file.getPath();
		}
		return null;
	}

	public String chooseOutputFile()
	{
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Write " + description);
		UIFileFilter filter = new UIFileFilter();
		if (extension != null) filter.addExtension(extension);
		filter.setDescription(description);
		fc.setFileFilter(filter);
		int returnVal = fc.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File file = fc.getSelectedFile();
			return file.getPath();
		}
		return null;
	}
}

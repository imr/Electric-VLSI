/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DialogOpenFile.java
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

import com.sun.electric.tool.user.User;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

public class OpenFile extends JFileChooser
{
	public static class EFileFilter extends FileFilter
	{
		/** description of filter */		private String desc;
		/** list of valid extensions */		private String[] extensions;
		/** List of all filters */			private static ArrayList allFilters = new ArrayList();
		
		/** Creates a new instance of EFileFilter */
		public EFileFilter(String[] extensions, String desc)
		{
			this.extensions = extensions;
			this.desc = desc;
			EFileFilter.allFilters.add(this);
		}

		/** Returns true if file has extension matching any of
		 * extensions is @param extensions.  Also returns true
		 * if file is a directory.  Returns false otherwise
		 */
		public boolean accept(java.io.File f)
		{
			if (f == null) return false;
			if (f.isDirectory()) return true;
			String filename = f.getName();
			int i = filename.lastIndexOf('.');
			if (i < 0) return false;
			String thisExtension = filename.substring(i+1);
			if (thisExtension == null) return false;
			if (extensions.length == 0) return true;	// special case for ANY
			for (int j=0; j<extensions.length; j++) 
			{
				String extension = extensions[j];
				if (extension.equalsIgnoreCase(thisExtension)) return true;
			}
			return false;
		}

		public static ArrayList getAllFilters() {  return allFilters; }
		
		public String getDescription() { return desc; }
		
		public String [] getExtensions() { return extensions; }

		public void setDescription(String desc) { this.desc = desc; }
	}
		
	public static final EFileFilter ANY        = null;					// default to built-in filter "All Files"
	public static final EFileFilter TEXT       = new EFileFilter(new String[] {"txt"}, "Text File (txt)");
	public static final EFileFilter ELIB       = new EFileFilter(new String[] {"elib"}, "Library File (elib)");
	public static final EFileFilter SPI        = new EFileFilter(new String[] {"spi", "sp"}, "Spice Deck (spi, sp)");
	public static final EFileFilter VERILOG    = new EFileFilter(new String[] {"v"}, "Verilog Deck (v)");
	public static final EFileFilter JAVA       = new EFileFilter(new String[] {"java", "bsh"}, "Java Script File (java, bsh)");
	public static final EFileFilter CIF        = new EFileFilter(new String[] {"cif"}, "CIF File (cif)");
	public static final EFileFilter GDS        = new EFileFilter(new String[] {"gds"}, "GDS File (gds)");
	public static final EFileFilter ARR        = new EFileFilter(new String[] {"arr"}, "Pad Generator Array File (arr)");
	public static final EFileFilter POSTSCRIPT = new EFileFilter(new String[] {"ps"}, "PostScript (ps)");

	/** True if this is a file save dialog */						private boolean saveDialog;
	/** single dialog box...cause it takes so long to pop up */		private static OpenFile dialog = new OpenFile();

	/** Private constructor, use factory methods chooseInputFile or
	 * chooseOutputFile instead.
	 */
	private OpenFile()
	{
		// add list of file filters to this dialog
		for (Iterator it = EFileFilter.getAllFilters().iterator(); it.hasNext(); )
			addChoosableFileFilter((EFileFilter)it.next());
	}
		
	/**
	 * Factory method to create a new open dialog box using the default EFileFilter.
	 * @param filter used to filter file types. Defaults to ANY if null.
	 * @param title dialog title to use; if null uses "Open 'filetype'".
	 */
	public static String chooseInputFile(FileFilter filter, String title)
	{
		dialog.saveDialog = false;

		if (title != null) dialog.setDialogTitle(title); else
		{
			String dialogTitle = "Open file";
			if (filter != null) dialogTitle = "Open " + filter.getDescription();
			dialog.setDialogTitle(dialogTitle);
		}

		// note that if filter is null it defaults to the built in filter "All Files"
		dialog.setFileFilter(filter);

		dialog.setCurrentDirectory(new File(User.getWorkingDirectory()));
		int returnVal = dialog.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File file = dialog.getSelectedFile();
			return file.getPath();
		}
		return null;
	}
	

	/**
	 * Factory method to create a new save dialog box using the
	 * default EFileFilter.
	 * @param filter used to filter file types. Defaults to ANY if null.
	 * @param title dialog title to use; if null uses "Write 'filetype'".
	 * @param defaultFile default file name to write.
	 */
	public static String chooseOutputFile(FileFilter filter, String title, String defaultFile)
	{
		OpenFile dialog = new OpenFile();
		dialog.addChoosableFileFilter(filter);
		
		dialog.saveDialog = true;
		
		if (title == null) dialog.setDialogTitle("Write " + filter.getDescription()); else
			dialog.setDialogTitle(title);
		// note that if filter is null it defaults to the built in filter "All Files"
		dialog.setFileFilter(filter);
		
		dialog.setFileFilter(filter);
		dialog.setSelectedFile(new File(defaultFile));
		dialog.setCurrentDirectory(new File(User.getWorkingDirectory()));
		int returnVal = dialog.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File file = dialog.getSelectedFile();
			return file.getPath();
		}
		return null;
	}

	/**
	 * Method called when the user clicks "ok" during file choosing.
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
		User.setWorkingDirectory(getCurrentDirectory().getPath());
		super.approveSelection();
	}
}

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
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

public class OpenFile
{
	public static final EFileFilter ANY        = null;					// default to built-in filter "All Files"
	public static final EFileFilter TEXT       = new EFileFilter(new String[] {"txt"}, "Text File (txt)");
	public static final EFileFilter ELIB       = new EFileFilter(new String[] {"elib"}, "Library File (elib)");
	public static final EFileFilter SPI        = new EFileFilter(new String[] {"spi", "sp"}, "Spice Deck (spi, sp)");
	public static final EFileFilter VERILOG    = new EFileFilter(new String[] {"v"}, "Verilog Deck (v)");
	public static final EFileFilter MAXWELL    = new EFileFilter(new String[] {"mac"}, "Maxwell Deck (mac)");
	public static final EFileFilter JAVA       = new EFileFilter(new String[] {"java", "bsh"}, "Java Script File (java, bsh)");
	public static final EFileFilter CIF        = new EFileFilter(new String[] {"cif"}, "CIF File (cif)");
	public static final EFileFilter GDS        = new EFileFilter(new String[] {"gds"}, "GDS File (gds)");
	public static final EFileFilter ARR        = new EFileFilter(new String[] {"arr"}, "Pad Generator Array File (arr)");
	public static final EFileFilter POSTSCRIPT = new EFileFilter(new String[] {"ps"}, "PostScript (ps)");

	/**
	 * Class to filter appropriate files during the open and save dialogs.
	 */
	public static class EFileFilter
	{
		/** description of filter */		private FileFilterSwing swingFilter;
		/** description of filter */		private FileFilterAWT awtFilter;
		/** list of valid extensions */		private String[] extensions;

		/** Creates a new instance of EFileFilter */
		public EFileFilter(String[] extensions, String desc)
		{
			this.extensions = extensions;
			swingFilter = new FileFilterSwing(extensions, desc);
			awtFilter = new FileFilterAWT(extensions, desc);
		}

		public String getDescription() { return swingFilter.getDescription(); }

		public String [] getExtensions() { return extensions; }

		FileFilterSwing getSwingFilter() { return swingFilter; }

		FileFilterAWT getAWTFilter() { return awtFilter; }
	}

	public static class FileFilterSwing extends FileFilter
	{
		/** list of valid extensions */		private String[] extensions;
		/** description of filter */		private String desc;

		/** Creates a new instance of FileFilterSwing */
		public FileFilterSwing(String[] extensions, String desc)
		{
			this.extensions = extensions;
			this.desc = desc;
		}

		/** Returns true if file has extension matching any of
		 * extensions is @param extensions.  Also returns true
		 * if file is a directory.  Returns false otherwise
		 */
		public boolean accept(File f)
		{
			if (f == null) return false;
			if (f.isDirectory()) return true;
			if (extensions.length == 0) return true;	// special case for ANY
			String filename = f.getName();
			int i = filename.lastIndexOf('.');
			if (i < 0) return false;
			String thisExtension = filename.substring(i+1);
			if (thisExtension == null) return false;
			for (int j=0; j<extensions.length; j++) 
			{
				String extension = extensions[j];
				if (extension.equalsIgnoreCase(thisExtension)) return true;
			}
			return false;
		}

		public String getDescription() { return desc; }
	}


	public static class FileFilterAWT implements FilenameFilter
	{
		/** list of valid extensions */		private String[] extensions;
		/** description of filter */		private String desc;

		/** Creates a new instance of FileFilterAWT */
		public FileFilterAWT(String[] extensions, String desc)
		{
			this.extensions = extensions;
			this.desc = desc;
		}

		/** Returns true if file has extension matching any of
		 * extensions is @param extensions.  Also returns true
		 * if file is a directory.  Returns false otherwise
		 */
		public boolean accept(File f, String filename)
		{
			if (extensions.length == 0) return true;	// special case for ANY
			int i = filename.lastIndexOf('.');
			if (i < 0) return false;
			String thisExtension = filename.substring(i+1);
			if (thisExtension == null) return false;
			for (int j=0; j<extensions.length; j++) 
			{
				String extension = extensions[j];
				if (extension.equalsIgnoreCase(thisExtension)) return true;
			}
			return false;
		}

		public String getDescription() { return desc; }
	}

	public static class OpenFileSwing extends JFileChooser
	{
		/** True if this is a file save dialog */						private boolean saveDialog;

		/** Private constructor, use factory methods chooseInputFile or
		 * chooseOutputFile instead.
		 */
		private OpenFileSwing() {}

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

	/**
	 * Factory method to create a new open dialog box using the default EFileFilter.
	 * @param filter used to filter file types. Defaults to ANY if null.
	 * @param title dialog title to use; if null uses "Open 'filetype'".
	 */
	public static String chooseInputFile(EFileFilter filter, String title)
	{
		if (title == null)
		{
			title = "Open file";
			if (filter != null) title = "Open " + filter.getDescription();
		}

		boolean useSwing = true;
		if (TopLevel.getOperatingSystem() == TopLevel.OS.MACINTOSH)
			useSwing = false;

		if (useSwing)
		{
			OpenFileSwing dialog = new OpenFileSwing();
			dialog.saveDialog = false;
			dialog.setDialogTitle(title);
			dialog.setFileFilter(filter.getSwingFilter());
			dialog.setCurrentDirectory(new File(User.getWorkingDirectory()));
			int returnVal = dialog.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				File file = dialog.getSelectedFile();
				return file.getPath();
			}
			return null;
		} else
		{
			// the AWT way
			FileDialog dialog = new FileDialog(TopLevel.getCurrentJFrame(), title, FileDialog.LOAD);
			dialog.setDirectory(User.getWorkingDirectory());
			dialog.setFilenameFilter(filter.getAWTFilter());
			dialog.show();
			String fileName = dialog.getFile();
			if (fileName == null) return null;
			User.setWorkingDirectory(dialog.getDirectory());
			return dialog.getDirectory() + fileName;
		}
	}
	

	/**
	 * Factory method to create a new save dialog box using the
	 * default EFileFilter.
	 * @param filter used to filter file types. Defaults to ANY if null.
	 * @param title dialog title to use; if null uses "Write 'filetype'".
	 * @param defaultFile default file name to write.
	 */
	public static String chooseOutputFile(EFileFilter filter, String title, String defaultFile)
	{
		if (title != null)
		{
			if (filter != null) title = "Write " + filter.getDescription(); else
				title = "Write file";
		}

		boolean useSwing = true;
		if (TopLevel.getOperatingSystem() == TopLevel.OS.MACINTOSH)
			useSwing = false;

		if (useSwing)
		{
			OpenFileSwing dialog = new OpenFileSwing();
			dialog.saveDialog = true;
			dialog.setDialogTitle(title);
			dialog.setFileFilter(filter.getSwingFilter());
			dialog.addChoosableFileFilter(filter.getSwingFilter());
			dialog.setCurrentDirectory(new File(User.getWorkingDirectory()));
			dialog.setSelectedFile(new File(defaultFile));
			int returnVal = dialog.showSaveDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				File file = dialog.getSelectedFile();
				return file.getPath();
			}
			return null;
		} else
		{
			// the AWT way
			FileDialog awtDialog = new FileDialog(TopLevel.getCurrentJFrame(), title, FileDialog.SAVE);
			awtDialog.setDirectory(User.getWorkingDirectory());
			awtDialog.setFile(defaultFile);
			awtDialog.setFilenameFilter(filter.getAWTFilter());
			awtDialog.show();
			String fileName = awtDialog.getFile();
			if (fileName == null) return null;
			return awtDialog.getDirectory() + fileName;
		}
	}
}

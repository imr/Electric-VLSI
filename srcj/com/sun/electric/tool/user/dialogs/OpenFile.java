/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OpenFile.java
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
//import java.awt.Point;
//import java.awt.Rectangle;
//import java.awt.event.ComponentListener;
//import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

public class OpenFile
{
	/**
	 * Function is a typesafe enum class that describes the types of files that can be read or written.
	 */
	public static class Type
	{
		/** Describes any file.*/				public static final Type ANY          = new Type("All", new String[] {}, "All Files");
		/** Describes ELIB files.*/				public static final Type ELIB         = new Type("ELIB", new String[] {"elib"}, "Library File (elib)");
		/** Describes text files. */			public static final Type TEXT         = new Type("Text", new String[] {"txt"}, "Text File (txt)");
		/** Describes Readable Dump files. */	public static final Type READABLEDUMP = new Type("ReadableDump", new String[] {"txt"}, "Readable Dump Library File (txt)");
		/** Describes CIF files. */				public static final Type CIF          = new Type("CIF", new String[] {"cif"}, "CIF File (cif)");
		/** Describes GDS files. */				public static final Type GDS          = new Type("GDS", new String[] {"gds"}, "GDS File (gds)");
		/** Describes PostScript files. */		public static final Type POSTSCRIPT   = new Type("PostScript", new String[] {"ps"}, "PostScript (ps)");
		/** Describes SPICE decks.*/			public static final Type SPICE        = new Type("Spice", new String[] {"spi", "sp"}, "Spice Deck (spi, sp)");
		/** Describes SPICE standard output.*/	public static final Type SPICEOUT     = new Type("SpiceOutput", new String[] {"spo"}, "Spice/GNUCap Output FIle (spo)");
		/** Describes PSpice standard output.*/	public static final Type PSPICEOUT    = new Type("PSpiceOutput", new String[] {"spo"}, "PSpice/Spice3 Output FIle (spo)");
		/** Describes HSpice output. */			public static final Type HSPICEOUT    = new Type("HSpiceOutput", new String[] {"tr0", "pa0"}, "HSpice Output File (tr0/pa0)");
		/** Describes Raw Spice output. */		public static final Type RAWSPICEOUT  = new Type("RawSpiceOutput", new String[] {"raw"}, "Spice Raw Output File (raw)");
		/** Describes Raw SmartSpice output. */	public static final Type RAWSSPICEOUT = new Type("RawSmartSpiceOutput", new String[] {"raw"}, "SmartSPICE Raw Output File (raw)");
		/** Describes CDL decks.*/				public static final Type CDL          = new Type("CDL", new String[] {"cdl"}, "CDL Deck (cdl)");
		/** Describes VERILOG decks. */			public static final Type VERILOG      = new Type("Verilog", new String[] {"v"}, "Verilog Deck (v)");
		/** Describes VERILOG output. */		public static final Type VERILOGOUT   = new Type("VerilogOutput", new String[] {"dump"}, "Verilog VCD Dump (vcd)");
		/** Describes MAXWELL decks. */			public static final Type MAXWELL      = new Type("Maxwell", new String[] {"mac"}, "Maxwell Deck (mac)");
		/** Describes IRSIM decks. */			public static final Type IRSIM        = new Type("IRSIM", new String[] {"sim"}, "IRSIM Deck (sim)");
		/** Describes Java source. */			public static final Type JAVA         = new Type("Java", new String[] {"java", "bsh"}, "Java Script File (java, bsh)");
		/** Describes Pad Frame Array spec. */	public static final Type PADARR       = new Type("PadArray", new String[] {"arr"}, "Pad Generator Array File (arr)");

		private final String name;
		private final String [] extensions;
		private final String desc;
		private FileFilterSwing ffs;
		private FileFilterAWT ffa;

		private Type(String name, String [] extensions, String desc)
		{
			this.name = name;
			this.extensions = extensions;
			this.desc = desc;
			this.ffs = null;
			this.ffa = null;
		}

		public String getName() { return name; }
		public String getDescription() { return desc; }
		public String [] getExtensions() { return extensions; }

		public FileFilterSwing getFileFilterSwing()
		{
			if (ffs == null) ffs = new FileFilterSwing(extensions, desc);
			return ffs;
		}

		public FileFilterAWT getFileFilterAWT()
		{
			if (ffa == null) ffa = new FileFilterAWT(extensions, desc);
			return ffa;
		}

		/**
		 * Returns a printable version of this Type.
		 * @return a printable version of this Type.
		 */
		public String toString() { return name; }
	}

	private static class FileFilterSwing extends FileFilter
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


	private static class FileFilterAWT implements FilenameFilter
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

	private static class OpenFileSwing extends JFileChooser
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

//	/** the location of the open file dialog */		private static Point location = null;

	/**
	 * Factory method to create a new open dialog box using the default Type.
	 * @param type the type of file to read. Defaults to ANY if null.
	 * @param title dialog title to use; if null uses "Open 'filetype'".
	 */
	public static String chooseInputFile(Type type, String title)
	{
		if (title == null)
		{
			title = "Open file";
			if (type != null) title = "Open " + type.getDescription();
		}

		boolean useSwing = true;
		if (TopLevel.getOperatingSystem() == TopLevel.OS.MACINTOSH)
			useSwing = false;

//		if (location == null) location = new Point(100, 50);
//		System.out.println("Put it at "+location);
		if (useSwing)
		{
			OpenFileSwing dialog = new OpenFileSwing();
			dialog.saveDialog = false;
			dialog.setDialogTitle(title);
			dialog.setFileFilter(type.getFileFilterSwing());
			dialog.setCurrentDirectory(new File(User.getWorkingDirectory()));
//			dialog.setLocation(location.x, location.y);
//			dialog.addComponentListener(new MoveComponentListener());
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
			dialog.setFilenameFilter(type.getFileFilterAWT());
			dialog.show();
			String fileName = dialog.getFile();
			if (fileName == null) return null;
			User.setWorkingDirectory(dialog.getDirectory());
			return dialog.getDirectory() + fileName;
		}
	}
	
//	private static class MoveComponentListener implements ComponentListener
//	{
//		public void componentHidden(ComponentEvent e) {}
//		public void componentShown(ComponentEvent e) {}
//		public void componentResized(ComponentEvent e) {}
//		public void componentMoved(ComponentEvent e)
//		{
//			Rectangle bound = ((JFileChooser)e.getSource()).getBounds();
//			location.x = (int)bound.getMinX();
//			location.y = (int)bound.getMinY();
//System.out.println("Moved to "+location);
//		}
//	}

	/**
	 * Factory method to create a new save dialog box using the
	 * default EFileFilter.
	 * @param type the type of file. Defaults to ANY if null.
	 * @param title dialog title to use; if null uses "Write 'filetype'".
	 * @param defaultFile default file name to write.
	 */
	public static String chooseOutputFile(Type type, String title, String defaultFile)
	{
		if (title != null)
		{
			if (type != null) title = "Write " + type.getDescription(); else
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
			dialog.setFileFilter(type.getFileFilterSwing());
			dialog.addChoosableFileFilter(type.getFileFilterSwing());
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
			awtDialog.setFilenameFilter(type.getFileFilterAWT());
			awtDialog.show();
			String fileName = awtDialog.getFile();
			if (fileName == null) return null;
			return awtDialog.getDirectory() + fileName;
		}
	}
}

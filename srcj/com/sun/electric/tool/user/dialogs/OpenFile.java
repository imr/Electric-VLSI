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
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.FileDialog;
//import java.awt.Point;
//import java.awt.Rectangle;
//import java.awt.event.ComponentListener;
//import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
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
        /** all types */                        private static final ArrayList allTypes = new ArrayList();

		/** Describes any file.*/				public static final Type ANY          = new Type("All", new String[] {""}, "All Files");
		/** Describes CDL decks.*/				public static final Type CDL          = new Type("CDL", new String[] {"cdl"}, "CDL Deck (cdl)");
		/** Describes CIF files. */				public static final Type CIF          = new Type("CIF", new String[] {"cif"}, "CIF File (cif)");
		/** Describes COSMOS output. */			public static final Type COSMOS       = new Type("COSMOS", new String[] {"sim"}, "COSMOS File (sim)");
		/** Describes DEF output. */			public static final Type DEF          = new Type("DEF", new String[] {"def"}, "DEF File (def)");
		/** Describes DXF output. */			public static final Type DXF          = new Type("DXF", new String[] {"dxf"}, "DXF File (dxf)");
		/** Describes Eagle files.*/			public static final Type EAGLE        = new Type("Eagle", new String[] {"txt"}, "Eagle File (txt)");
		/** Describes ECAD files.*/				public static final Type ECAD         = new Type("ECAD", new String[] {"enl"}, "ECAD File (enl)");
		/** Describes EDIF files.*/				public static final Type EDIF         = new Type("EDIF", new String[] {"edif"}, "EDIF File (edif)");
		/** Describes ELIB files.*/				public static final Type ELIB         = new Type("ELIB", new String[] {"elib"}, "Library File (elib)");
		/** Describes Encapsulated PS files. */	public static final Type EPS          = new Type("Encapsulated PostScript", new String[] {"eps"}, "Encapsulated PostScript (eps)");
		/** Describes ESIM/RNL output. */		public static final Type ESIM         = new Type("ESIM", new String[] {"sim"}, "ESIM File (sim)");
		/** Describes FastHenry files.*/		public static final Type FASTHENRY    = new Type("FastHenry", new String[] {"inp"}, "FastHenry File (inp)");
		/** Describes GDS files. */				public static final Type GDS          = new Type("GDS", new String[] {"gds"}, "GDS File (gds)");
		/** Describes HSpice output. */			public static final Type HSPICEOUT    = new Type("HSpiceOutput", new String[] {"tr0", "pa0"}, "HSpice Output File (tr0/pa0)");
		/** Describes HTML files. */			public static final Type HTML         = new Type("HTML", new String[] {"html"}, "HTML File (html)");
		/** Describes IRSIM decks. */			public static final Type IRSIM        = new Type("IRSIM", new String[] {"sim"}, "IRSIM Deck (sim)");
		/** Describes Java source. */			public static final Type JAVA         = new Type("Java", new String[] {"java", "bsh"}, "Java Script File (java, bsh)");
		/** Describes JELIB files.*/			public static final Type JELIB        = new Type("JELIB", new String[] {"jelib"}, "Library File (jelib)");
		/** Describes L files.*/				public static final Type L            = new Type("L", new String[] {"L"}, "L File (L)");
		/** Describes LEF files.*/				public static final Type LEF          = new Type("LEF", new String[] {"lef"}, "LEF File (lef)");
		/** Describes Maxwell decks. */			public static final Type MAXWELL      = new Type("Maxwell", new String[] {"mac"}, "Maxwell Deck (mac)");
		/** Describes MOSSIM decks. */			public static final Type MOSSIM       = new Type("MOSSIM", new String[] {"ntk"}, "MOSSIM Deck (ntk)");
		/** Describes Pad Frame Array spec. */	public static final Type PADARR       = new Type("PadArray", new String[] {"arr"}, "Pad Generator Array File (arr)");
		/** Describes Pads files. */			public static final Type PADS         = new Type("Pads", new String[] {"asc"}, "Pads File (asc)");
		/** Describes PAL files. */				public static final Type PAL          = new Type("PAL", new String[] {"pal"}, "PAL File (pal)");
		/** Describes PostScript files. */		public static final Type POSTSCRIPT   = new Type("PostScript", new String[] {"ps"}, "PostScript (ps)");
		/** Describes PSpice standard output.*/	public static final Type PSPICEOUT    = new Type("PSpiceOutput", new String[] {"spo"}, "PSpice/Spice3 Output File (spo)");
		/** Describes Raw Spice output. */		public static final Type RAWSPICEOUT  = new Type("RawSpiceOutput", new String[] {"raw"}, "Spice Raw Output File (raw)");
		/** Describes Raw SmartSpice output. */	public static final Type RAWSSPICEOUT = new Type("RawSmartSpiceOutput", new String[] {"raw"}, "SmartSPICE Raw Output File (raw)");
		/** Describes Readable Dump files. */	public static final Type READABLEDUMP = new Type("ReadableDump", new String[] {"txt"}, "Readable Dump Library File (txt)");
		/** Describes RSIM output. */			public static final Type RSIM         = new Type("RSIM", new String[] {"sim"}, "RSIM File (sim)");
		/** Describes Silos decks.*/			public static final Type SILOS        = new Type("Silos", new String[] {"sil"}, "Silos Deck (sil)");
		/** Describes Skill decks.*/			public static final Type SKILL        = new Type("Skill", new String[] {"il"}, "Skill Deck (il)");
		/** Describes Spice decks.*/			public static final Type SPICE        = new Type("Spice", new String[] {"spi", "sp"}, "Spice Deck (spi, sp)");
		/** Describes Spice standard output.*/	public static final Type SPICEOUT     = new Type("SpiceOutput", new String[] {"spo"}, "Spice/GNUCap Output File (spo)");
		/** Describes Sue files.*/				public static final Type SUE          = new Type("Sue", new String[] {"sue"}, "Sue File (sue)");
		/** Describes Tegas files. */			public static final Type TEGAS        = new Type("Tegas", new String[] {"tdl"}, "Tegas File (tdl)");
		/** Describes text files. */			public static final Type TEXT         = new Type("Text", new String[] {"txt"}, "Text File (txt)");
		/** Describes Verilog decks. */			public static final Type VERILOG      = new Type("Verilog", new String[] {"v"}, "Verilog Deck (v)");
		/** Describes Verilog output. */		public static final Type VERILOGOUT   = new Type("VerilogOutput", new String[] {"dump"}, "Verilog VCD Dump (vcd)");

		/** Describes default file format.*/	public static final Type DEFAULTLIB   = ELIB;

        /** Valid Library formats */            public static final Type libraryTypes[] = {ELIB, JELIB};
        private static String [] libraryTypesExt;
        private static String libraryTypesExtReadable;
        static {
            ArrayList exts = new ArrayList();
            for (int i=0; i<libraryTypes.length; i++) {
                Type type = libraryTypes[i];
                String [] typeExts = type.getExtensions();
                for (int j=0; j<typeExts.length; j++) exts.add(typeExts[j]);
            }
            libraryTypesExt = new String[exts.size()];
            StringBuffer buf = new StringBuffer("(");
            for (int i=0; i<exts.size(); i++) {
                libraryTypesExt[i] = (String)exts.get(i);
                buf.append((String)exts.get(i));
                buf.append(", ");
            }
            if (buf.length() > 2) buf.replace(buf.length()-2, buf.length(), ")");
            libraryTypesExtReadable = buf.toString();
        }

        /** Valid library formats as a Type */  public static final Type LIBRARYFORMATS = new Type("LibraryFormtas", libraryTypesExt, "Library Formats "+libraryTypesExtReadable);

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
            allTypes.add(this);
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

        /**
         * Get the Type for the specified filter
         */
        public static Type getType(FileFilter filter) {
            for (Iterator it = allTypes.iterator(); it.hasNext(); ) {
                Type type = (Type)it.next();
                if (type.ffs == filter) return type;
            }
            return null;
        }

        /**
         * Get the Type for the specified filter
         */
        public static Type getType(FilenameFilter filter) {
            for (Iterator it = allTypes.iterator(); it.hasNext(); ) {
                Type type = (Type)it.next();
                if (type.ffa == filter) return type;
            }
            return null;
        }
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
			dialog.setVisible(true);
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
        Type [] types;
        if (type == null) types = null;
        else types = new Type [] {type};
        return chooseOutputFile(types, title, defaultFile);
    }

	/**
	 * Factory method to create a new save dialog box using the
	 * default EFileFilter.
	 * @param types the types of file. Defaults to ANY if null.
	 * @param title dialog title to use; if null uses "Write 'filetype'".
	 * @param defaultFile default file name to write.
	 */
	public static String chooseOutputFile(Type [] types, String title, String defaultFile)
	{
		if (title != null)
		{
			if (types != null) title = "Write " + types[0].getDescription(); else
				title = "Write file";
		}
        if (types == null) types = new Type [] {Type.ANY};

		boolean useSwing = true;
		if (TopLevel.getOperatingSystem() == TopLevel.OS.MACINTOSH)
			useSwing = false;

		if (useSwing)
		{
			OpenFileSwing dialog = new OpenFileSwing();
			dialog.saveDialog = true;
			dialog.setDialogTitle(title);
            for (int i=0; i<types.length; i++) {
			    dialog.addChoosableFileFilter(types[i].getFileFilterSwing());
            }
			dialog.setFileFilter(FileMenu.getLibraryFormat(defaultFile, types[0]).getFileFilterSwing());
			dialog.setCurrentDirectory(new File(User.getWorkingDirectory()));
			dialog.setSelectedFile(new File(defaultFile));
			int returnVal = dialog.showSaveDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				File file = dialog.getSelectedFile();
                String fileName = file.getPath();
                Type selectedType = Type.getType(dialog.getFileFilter());
                if (selectedType != null)
                {
	                String extension = selectedType.getExtensions()[0];
	                int dotPos = fileName.lastIndexOf('.');
	                if (dotPos < 0) fileName += "." + extension; else
	                {
	                    if (!fileName.substring(dotPos+1).equals(extension))
	                    {
	                        //fileName = fileName.substring(0, dotPos) + "." + extension;
	                        fileName = fileName + "." + extension;
	                    }
	                }
                }
				return fileName;
			}
			return null;
		} else
		{
			// the AWT way
			FileDialog awtDialog = new FileDialog(TopLevel.getCurrentJFrame(), title, FileDialog.SAVE);
			awtDialog.setDirectory(User.getWorkingDirectory());
			awtDialog.setFile(defaultFile);
			awtDialog.setFilenameFilter(types[0].getFileFilterAWT());
			awtDialog.setVisible(true);
			String fileName = awtDialog.getFile();
			if (fileName == null) return null;
			return awtDialog.getDirectory() + fileName;
		}
	}

	/**
	 * Method to determine OpenFile type based on extension
	 * @param libName
	 * @return OpenFile.Type extension
	 */
	public static Type getOpenFileType(String libName, Type def)
	{
		if (libName.endsWith(".elib"))
			return Type.ELIB;
		else if (libName.endsWith(".jelib"))
			return Type.JELIB;
		else if (libName.endsWith(".txt"))
			return Type.READABLEDUMP;
		return (def);
	}
}

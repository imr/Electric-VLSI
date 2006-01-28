/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FileType.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io;

import javax.swing.filechooser.FileFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.FilenameFilter;
import java.io.File;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * A typesafe enum class that describes the types of files that can be read or written.
 */
public class FileType implements Serializable {
	/** all types */                        private static final ArrayList<FileType> allTypes = new ArrayList<FileType>();

	/** Describes any file.*/				public static final FileType ANY          = makeFileType("All", new String[] {}, "All Files");
	/** Describes ALS decks. */				public static final FileType ALS          = makeFileType("ALS", new String[] {"als"}, "ALS Simulation Deck (als)");
	/** Describes ALS vector decks. */		public static final FileType ALSVECTOR    = makeFileType("ALS Vectors", new String[] {"vec"}, "ALS Vector Deck (vec)");
	/** Describes ArchSim decks.*/			public static final FileType ARCHSIM      = makeFileType("ArchSim", new String[] {"xml"}, "ArchSim Deck (xml)");
	/** Describes ArchSim output.*/			public static final FileType ARCHSIMOUT   = makeFileType("ArchSim Output", new String[] {"asj"}, "ArchSim Journal (asj)");
	/** Describes CDL decks.*/				public static final FileType CDL          = makeFileType("CDL", new String[] {"cdl"}, "CDL Deck (cdl)");
	/** Describes CIF files. */				public static final FileType CIF          = makeFileType("CIF", new String[] {"cif"}, "CIF File (cif)");
	/** Describes COSMOS output. */			public static final FileType COSMOS       = makeFileType("COSMOS", new String[] {"sim"}, "COSMOS File (sim)");
    /** Describes Calibre DRC Error files. */public static final FileType DB          = makeFileType("DB", new String[] {"db"}, "Calibre DRC Error File (db)");
	/** Describes DEF output. */			public static final FileType DEF          = makeFileType("DEF", new String[] {"def"}, "DEF File (def)");
	/** Describes Dais input. */			public static final FileType DAIS         = makeFileType("Dais", new String[] {""}, "Dais Workspace");
	/** Describes DXF output. */			public static final FileType DXF          = makeFileType("DXF", new String[] {"dxf"}, "DXF File (dxf)");
	/** Describes Eagle files.*/			public static final FileType EAGLE        = makeFileType("Eagle", new String[] {"txt"}, "Eagle File (txt)");
	/** Describes ECAD files.*/				public static final FileType ECAD         = makeFileType("ECAD", new String[] {"enl"}, "ECAD File (enl)");
	/** Describes EDIF files.*/				public static final FileType EDIF         = makeFileType("EDIF", new String[] {"edif"}, "EDIF File (edif)");
	/** Describes ELIB files.*/				public static final FileType ELIB         = makeFileType("ELIB", new String[] {"elib"}, "Library File (elib)");
	/** Describes Encapsulated PS files. */	public static final FileType EPS          = makeFileType("Encapsulated PostScript", new String[] {"eps"}, "Encapsulated PostScript (eps)");
    /** Describes EPIC simulation output. */public static final FileType EPIC         = makeFileType("EPIC output", new String[] {"out"}, "EPIC simulation output (out)");
    /** Describes Assura DRC Error files. */public static final FileType ERR          = makeFileType("ERR", new String[] {"err"}, "Assura DRC Error File (err)");
	/** Describes ESIM/RNL output. */		public static final FileType ESIM         = makeFileType("ESIM", new String[] {"sim"}, "ESIM File (sim)");
	/** Describes FastHenry files.*/		public static final FileType FASTHENRY    = makeFileType("FastHenry", new String[] {"inp"}, "FastHenry File (inp)");
	/** Describes FPGA files.*/				public static final FileType FPGA         = makeFileType("FPGA", new String[] {"fpga"}, "FPGA Architecture File (fpga)");
	/** Describes GDS files. */				public static final FileType GDS          = makeFileType("GDS", new String[] {"gds"}, "GDS File (gds)");
	/** Describes GDS layer Map files. */	public static final FileType GDSMAP       = makeFileType("GDS Map", new String[] {"map"}, "GDS Layer Map File (map)");
	/** Describes HSpice output. */			public static final FileType HSPICEOUT    = makeFileTypeNumeric("HSpice Output", new String[] {"tr"}, "HSpice Output File (tr0,1,2...)");
	/** Describes HPGL files. */			public static final FileType HPGL         = makeFileType("HPGL", new String[] {"hpgl2"}, "HPGL File (hpgl2)");
	/** Describes HTML files. */			public static final FileType HTML         = makeFileType("HTML", new String[] {"html"}, "HTML File (html)");
	/** Describes IRSIM decks. */			public static final FileType IRSIM        = makeFileType("IRSIM", new String[] {"sim"}, "IRSIM Deck (sim)");
	/** Describes IRSIM parameter decks. */	public static final FileType IRSIMPARAM   = makeFileType("IRSIM Parameters", new String[] {"prm"}, "IRSIM Parameter Deck (prm)");
	/** Describes IRSIM vector decks. */	public static final FileType IRSIMVECTOR  = makeFileType("IRSIM Vectors", new String[] {"cmd"}, "IRSIM Vector Deck (cmd)");
	/** Describes Java source. */			public static final FileType JAVA         = makeFileType("Java", new String[] {"java", "bsh"}, "Java Script File (java, bsh)");
	/** Describes JELIB files.*/			public static final FileType JELIB        = makeFileType("JELIB", new String[] {"jelib"}, "Library File (jelib)");
    /** Describes J3D files.*/				public static final FileType J3D          = makeFileType("J3D", new String[] {"j3d"}, "Java3D Demo File (j3d}");
    /** Describes L files.*/				public static final FileType L            = makeFileType("L", new String[] {"L"}, "L File (L)");
	/** Describes LEF files.*/				public static final FileType LEF          = makeFileType("LEF", new String[] {"lef"}, "LEF File (lef)");
	/** Describes Library files.*/			public static final FileType LIBFILE      = makeFileType("LIBFILE", new String[] {"jelib", "elib", "txt"}, "Library File");
	/** Describes Maxwell decks. */			public static final FileType MAXWELL      = makeFileType("Maxwell", new String[] {"mac"}, "Maxwell Deck (mac)");
	/** Describes MOSSIM decks. */			public static final FileType MOSSIM       = makeFileType("MOSSIM", new String[] {"ntk"}, "MOSSIM Deck (ntk)");
    /** Describes Movie files. */			public static final FileType MOV          = makeFileType("Movie", new String[] {"mov"}, "Movie File (mov)");
    /** Describes Pad Frame Array spec. */	public static final FileType PADARR       = makeFileType("Pad Array", new String[] {"arr"}, "Pad Generator Array File (arr)");
	/** Describes Pads files. */			public static final FileType PADS         = makeFileType("Pads", new String[] {"asc"}, "Pads File (asc)");
	/** Describes PAL files. */				public static final FileType PAL          = makeFileType("PAL", new String[] {"pal"}, "PAL File (pal)");
	/** Describes PostScript files. */		public static final FileType POSTSCRIPT   = makeFileType("PostScript", new String[] {"ps"}, "PostScript (ps)");
	/** Describes PostScript files. */		public static final FileType PNG          = makeFileType("PNG", new String[] {"png"}, "PNG (png)");
	/** Describes Preferences files. */		public static final FileType PREFS        = makeFileType("Preferences", new String[] {"xml"}, "Preferences (xml)");
	/** Describes Project files. */			public static final FileType PROJECT      = makeFileType("Project Management", new String[] {"proj"}, "Project Management (proj)");
	/** Describes PSpice standard output.*/	public static final FileType PSPICEOUT    = makeFileType("PSpice Output", new String[] {"txt"}, "PSpice/Spice3 Text Output File (txt)");
	/** Describes Raw Spice output. */		public static final FileType RAWSPICEOUT  = makeFileType("RawSpice Output", new String[] {"raw"}, "Spice Raw Output File (raw)");
	/** Describes Raw SmartSpice output. */	public static final FileType RAWSSPICEOUT = makeFileType("Raw SmartSpice Output", new String[] {"raw"}, "SmartSPICE Raw Output File (raw)");
	/** Describes Readable Dump files. */	public static final FileType READABLEDUMP = makeFileType("ReadableDump", new String[] {"txt"}, "Readable Dump Library File (txt)");
	/** Describes RSIM output. */			public static final FileType RSIM         = makeFileType("RSIM", new String[] {"sim"}, "RSIM File (sim)");
	/** Describes Silos decks.*/			public static final FileType SILOS        = makeFileType("Silos", new String[] {"sil"}, "Silos Deck (sil)");
	/** Describes Skill decks.*/			public static final FileType SKILL        = makeFileType("Skill", new String[] {"il"}, "Skill Deck (il)");
    /** Describes Skill decks.*/			public static final FileType SKILLEXPORTSONLY = makeFileType("SkillExports Only", new String[] {"il"}, "Skill Deck (il)");
	/** Describes Spice decks.*/			public static final FileType SPICE        = makeFileType("Spice", new String[] {"spi", "sp"}, "Spice Deck (spi, sp)");
	/** Describes Spice standard output.*/	public static final FileType SPICEOUT     = makeFileType("Spice Output", new String[] {"spo"}, "Spice/GNUCap Output File (spo)");
	/** Describes Sue files.*/				public static final FileType SUE          = makeFileType("Sue", new String[] {"sue"}, "Sue File (sue)");
	/** Describes Tegas files. */			public static final FileType TEGAS        = makeFileType("Tegas", new String[] {"tdl"}, "Tegas File (tdl)");
	/** Describes text files. */			public static final FileType TEXT         = makeFileType("Text", new String[] {"txt"}, "Text File (txt)");
	/** Describes Verilog decks. */			public static final FileType VERILOG      = makeFileType("Verilog", new String[] {"v"}, "Verilog Deck (v)");
	/** Describes Verilog output. */		public static final FileType VERILOGOUT   = makeFileType("Verilog Output", new String[] {"dump"}, "Verilog VCD Dump (vcd)");
	/** Describes Xml files. */				public static final FileType XML          = makeFileType("XML", new String[] {"xml"}, "XML File (xml)");

	/** Describes default file format.*/	public static final FileType DEFAULTLIB   = JELIB;

	/** Valid Library formats */            public static final FileType libraryTypes[] = {JELIB, ELIB};
	private static String [] libraryTypesExt;
	private static String libraryTypesExtReadable;
	static {
		ArrayList<String> exts = new ArrayList<String>();
		for (int i=0; i<libraryTypes.length; i++) {
			FileType type = libraryTypes[i];
			String [] typeExts = type.getExtensions();
			for (int j=0; j<typeExts.length; j++) exts.add(typeExts[j]);
		}
		libraryTypesExt = new String[exts.size()];
		StringBuffer buf = new StringBuffer("(");
		for (int i=0; i<exts.size(); i++) {
			libraryTypesExt[i] = exts.get(i);
			buf.append(exts.get(i));
			buf.append(", ");
		}
		if (buf.length() > 2) buf.replace(buf.length()-2, buf.length(), ")");
		libraryTypesExtReadable = buf.toString();
	}

	/** Valid library formats as a Type */  public static final FileType LIBRARYFORMATS = makeFileType("LibraryFormats", libraryTypesExt, "Library Formats "+libraryTypesExtReadable);

	private String name;
	private String [] extensions;
	private String desc;
	private boolean allowNumbers;
	private transient FileFilterSwing ffs;
	private transient FileFilterAWT ffa;

	private FileType() {}

	private static FileType makeFileType(String name, String [] extensions, String desc)
	{
		FileType ft = new FileType();
		ft.name = name;
		ft.extensions = extensions;
		ft.desc = desc;
		ft.ffs = null;
		ft.ffa = null;
		ft.allowNumbers = false;
		allTypes.add(ft);
		return ft;
	}

	private static FileType makeFileTypeNumeric(String name, String [] extensions, String desc)
	{
		FileType ft = makeFileType(name, extensions, desc);
		ft.allowNumbers = true;
		return ft;
	}

	public String getName() { return name; }
	public String getDescription() { return desc; }
	public String [] getExtensions()
	{
		if (allowNumbers)
		{
			String [] newExtensions = new String[extensions.length];
			for(int i=0; i<extensions.length; i++)
				newExtensions[i] = extensions[i] + "0";
			return newExtensions;
		}
		return extensions;
	}

	public FileFilterSwing getFileFilterSwing()
	{
		if (ffs == null) ffs = new FileFilterSwing(extensions, desc, allowNumbers);
		return ffs;
	}

	public FileFilterAWT getFileFilterAWT()
	{
		if (ffa == null) ffa = new FileFilterAWT(extensions, desc, allowNumbers);
		return ffa;
	}

    private Object readResolve() throws ObjectStreamException {
        for (FileType ft: allTypes) {
            if (name.equals(ft.name)) return ft;
        }
        return this;
    }
    
	/**
	 * Returns a printable version of this Type.
	 * @return a printable version of this Type.
	 */
	public String toString() { return name; }

	/**
	 * Get the Type for the specified filter
	 */
	public static FileType getType(FileFilter filter) {
		for (FileType type : allTypes) {
			if (type.ffs == filter) return type;
		}
		return null;
	}

	/**
	 * Get the Type for the specified filter
	 */
	public static FileType getType(FilenameFilter filter) {
		for (FileType type : allTypes) {
			if (type.ffa == filter) return type;
		}
		return null;
	}

	private static class FileFilterSwing extends FileFilter
	{
		/** list of valid extensions */				private String[] extensions;
		/** description of filter */				private String desc;
		/** true to allow digits in extension */	private boolean allowNumbers;

		/** Creates a new instance of FileFilterSwing */
		public FileFilterSwing(String[] extensions, String desc, boolean allowNumbers)
		{
			this.extensions = extensions;
			this.desc = desc;
			this.allowNumbers = allowNumbers;
		}

		/** Returns true if file has extension matching any of
		 * extensions is @param extensions.  Also returns true
		 * if file is a directory.  Returns false otherwise
		 */
		public boolean accept(File f)
		{
			if (f == null) return false;
			if (f.isDirectory()) return true;
			String fileName = f.getName();
			return matches(fileName, extensions, allowNumbers);
		}

		public String getDescription() { return desc; }
	}

	private static class FileFilterAWT implements FilenameFilter
	{
		/** list of valid extensions */				private String[] extensions;
		/** description of filter */				private String desc;
		/** true to allow digits in extension */	private boolean allowNumbers;

		/** Creates a new instance of FileFilterAWT */
		public FileFilterAWT(String[] extensions, String desc, boolean allowNumbers)
		{
			this.extensions = extensions;
			this.desc = desc;
			this.allowNumbers = allowNumbers;
		}

		/** Returns true if file has extension matching any of
		 * extensions is @param extensions.  Also returns true
		 * if file is a directory.  Returns false otherwise
		 */
		public boolean accept(File f, String fileName)
		{
			return matches(fileName, extensions, allowNumbers);
		}

		public String getDescription() { return desc; }
	}

	private static boolean matches(String fileName, String [] extensions, boolean allowNumbers)
	{
		// special case for ANY
		if (extensions.length == 0) return true;
		int i = fileName.lastIndexOf('.');
		if (i < 0) return false;
		String thisExtension = fileName.substring(i+1);
		if (thisExtension == null) return false;
		for (int j=0; j<extensions.length; j++)
		{
			String extension = extensions[j];
			if (extension.equalsIgnoreCase(thisExtension)) return true;
			if (allowNumbers)
			{
				if (thisExtension.length() > extension.length())
				{
					if (thisExtension.startsWith(extension))
					{
						boolean allDigits = true;
						for(int k=extension.length(); k<thisExtension.length(); k++)
						{
							if (!Character.isDigit(thisExtension.charAt(k))) allDigits = false;
						}
						if (allDigits) return true;
					}
				}
			}
		}
		return false;
	}
}

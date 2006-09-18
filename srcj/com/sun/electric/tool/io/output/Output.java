/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Output.java
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.IdMapper;
import com.sun.electric.database.LibId;
import com.sun.electric.database.LibraryBackup;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Job.Priority;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.cvspm.CVS;
import com.sun.electric.tool.cvspm.CVSLibrary;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.User;

import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * This class manages writing files in different formats.
 * The class is subclassed by the different file writers.
 */
public class Output
{
    /**
     * Used in ECAD
     */
	static class NetNames
	{
		String nodeName;
		String netName;
		String portName;
	}

    static class NetNamesSort implements Comparator<NetNames>
    {
        public int compare(NetNames nn1, NetNames nn2)
        {
            String name1 = nn1.netName;
            String name2 = nn2.netName;
            return name1.compareToIgnoreCase(name2);
        }
    }

    /**
     * This is the non-interactive version of exportCellCommand
     * @param cell the Cell to be written.
     * @param context the VarContext of the Cell (its position in the hierarchy above it).
     * @param filePath the path to the disk file to be written.
     * @param type the format of the output file.
     * @param override a list of Polys to draw instead of the cell contents.
    */
    public static void exportCellCommand(Cell cell, VarContext context, String filePath,
                                         FileType type, List<PolyBase> override)
    {
    	new OutputCellInfo(cell, context, filePath, type, override);
    }

	static class OrderedExports implements Comparator<Export>
	{
		public int compare(Export e1, Export e2)
		{
			int i1 = e1.getOriginalPort().getPortProto().getPortIndex();
			int i2 = e2.getOriginalPort().getPortProto().getPortIndex();
			int cmp = i1 - i2;
			if (cmp != 0) return cmp;
			return e1.getPortIndex() - e2.getPortIndex();
		}
	}

	/** exports comparator for writeNodeInst */			static final Comparator<Export> EXPORTS_ORDER = new OrderedExports();

	/** file path */									protected String filePath;
	/** for writing text files */						protected PrintWriter printWriter;
	/** for writing binary files */						protected DataOutputStream dataOutputStream;
	/** Map of referenced objects for library files */	HashMap<Object,Integer> objInfo;
	/** Maps memory face index to disk face index */	int[] faceMap;
	/** Name space of variable names */					TreeMap<String,Short> nameSpace;
	/** True to write with less information displayed */protected boolean quiet;

	public Output()
	{
	}

//	/**
//	 * Method to write a Library.
//	 * This method is never called.
//	 * Instead, it is always overridden by the appropriate write subclass.
//	 * @param lib the Library to be written.
//     * @return true on error.
//	 */
//	protected boolean writeLib(Library lib) { return true; }
//
    /**
     * Method to write a cell.
     * This method is never called.
     * Instead, it is always overridden by the appropriate write subclass.
     * @param cell the Cell to be written.
     * @param context the VarContext of the cell (for parameter evaluation)
     * @return true on error.
     */
    protected boolean writeCell(Cell cell, VarContext context) { return true; }

	/**
	 * Method to write all Libraries in Snapsht into a panic directory.
     * @param panicSnapshot Snapshot to save.
     * @param panicDir panic directory to save.
     * @return true on error.
	 */
	public static boolean writePanicSnapshot(Snapshot panicSnapshot, File panicDir, boolean oldRevision) {
        FileType type = FileType.JELIB;
        HashMap<LibId,URL> libFiles = new HashMap<LibId,URL>();
        TreeMap<String,LibId> sortedLibs = new TreeMap<String,LibId>(TextUtils.STRING_NUMBER_ORDER);
        for (LibraryBackup libBackup: panicSnapshot.libBackups) {
            if (libBackup == null) continue;
            if ((libBackup.d.flags & Library.HIDDENLIBRARY) != 0) continue;
            LibId libId = libBackup.d.libId;
            String libName = libBackup.d.libId.libName;
            URL libURL = libBackup.d.libFile;
            File newLibFile = null;
            if (libURL == null || libURL.getPath() == null) {
                newLibFile = new File(panicDir.getAbsolutePath(), libName + "." + type.getExtensions()[0]);
            } else {
                File libFile = new File(libURL.getPath());
                String fileName = libFile.getName();
                if (fileName == null) fileName = libName + "." + type.getExtensions()[0];
                newLibFile = new File(panicDir.getAbsolutePath(), fileName);
            }
            URL newLibURL = TextUtils.makeURLToFile(newLibFile.getAbsolutePath());
            libFiles.put(libId, newLibURL);
            sortedLibs.put(libName, libId);
        }
        boolean error = false;
        for (LibId libId: sortedLibs.values()) {
            System.out.print("."); System.out.flush();
            URL libURL = libFiles.get(libId);
            JELIB jelib = new JELIB();
    		String properOutputName = TextUtils.getFilePath(libURL) + TextUtils.getFileNameWithoutExtension(libURL) + ".jelib";
            if (jelib.openTextOutputStream(properOutputName) ||
                    jelib.writeLib(panicSnapshot, libId, libFiles, oldRevision) ||
                    jelib.closeTextOutputStream()) {
                System.out.println("Error saving "+panicSnapshot.getLib(libId).d.libId.libName);
                error = true;
            }
        }
        System.out.println(" Libraries saved");
        return error;
    }
    
    /**
     * Method to write an entire Library in JELIB format.
     * This method doesn't modifiy library, so it can be run out of Job.
     * @param newName name which is used to prepare file name of the library.
     * @param lib library to save
     */
    public static boolean saveJelib(String newName, Library lib) {
        HashMap<LibId,URL> libFiles = new HashMap<LibId,URL>();
        // rename the library if requested
        URL libURL = lib.getLibFile();
        if (newName != null) {
            libURL = TextUtils.makeURLToFile(newName);
            libFiles.put(lib.getId(), libURL);
//            lib.setLibFile(libURL);
//            lib.setName(TextUtils.getFileNameWithoutExtension(libURL));
        }
        boolean error = false;
        String properOutputName = TextUtils.getFilePath(libURL) + TextUtils.getFileNameWithoutExtension(libURL) + ".jelib";
        JELIB jelib = new JELIB();
        if (jelib.openTextOutputStream(properOutputName) ||
                jelib.writeLib(lib.getDatabase().backup(), lib.getId(), libFiles, false) ||
                jelib.closeTextOutputStream()) {
            System.out.println("Error saving "+lib.getName());
            error = true;
        }
        return error;
    }

	/**
	 * Method to write an entire Library with a particular format.
	 * This is used for output formats that capture the entire library
	 * (only the ELIB and Readable Dump formats).
	 * The alternative to writing the entire library is writing a single
	 * cell and the hierarchy below it (use "writeCell").
	 * @param lib the Library to be written.
	 * @param type the format of the output file.
	 * @param compatibleWith6 true to write a library that is compatible with version 6 Electric.
	 * @param quiet true to save with less information displayed.
     * @param delibHeaderOnly true to write only the header for a DELIB type library
     * @return true on error.
	 */
	public static boolean writeLibrary(Library lib, FileType type, boolean compatibleWith6, boolean quiet, boolean delibHeaderOnly)
	{
		// make sure that all "meaning" options are attached to the database
//		Pref.installMeaningVariables();

		// make sure that this library save is announced
        Constraints.getCurrent().writeLibrary(lib);
		for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
		{
			Listener listener = it.next();
			listener.writeLibrary(lib);
		}

		// make sure all technologies with irrelevant scale information have the same scale value
		double largestScale = 0;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			if (tech.isScaleRelevant()) continue;
			if (tech == Generic.tech) continue;
			if (tech.getScale() > largestScale) largestScale = tech.getScale();
		}
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			if (tech.isScaleRelevant()) continue;
			if (tech == Generic.tech) continue;
			tech.setScale(largestScale);
		}

		// handle different file types
		URL libFile = lib.getLibFile();
		if (libFile == null)
			libFile = TextUtils.makeURLToFile(lib.getName());

		// make the proper output file name
		String properOutputNameWithoutExtension = TextUtils.getFilePath(libFile) + TextUtils.getFileNameWithoutExtension(libFile);
		String properOutputName = properOutputNameWithoutExtension;
		if (type == FileType.ELIB) properOutputName += ".elib";
		if (type == FileType.JELIB) properOutputName += ".jelib";
		if (type == FileType.DELIB) properOutputName += ".delib";
		if (type == FileType.READABLEDUMP) properOutputName += ".txt";
		if (type == FileType.ELIB || type == FileType.JELIB)
		{
			// backup previous files if requested
			int backupScheme = IOTool.getBackupRedundancy();
			if (backupScheme == 1)
			{
				// one-level backup
				File newFile = new File(properOutputName);
				if (newFile.exists())
				{
					String backupFileName = properOutputName + "~";
					File oldFile = new File(backupFileName);
					boolean canRename = true;
					if (oldFile.exists())
					{
						if (!oldFile.delete())
						{
							System.out.println("Unable to delete former library file " + oldFile);
							canRename = false;
						}
					}
					if (canRename)
					{
						if (!newFile.renameTo(oldFile))
						{
							System.out.println("Unable to rename " + newFile + " to " + oldFile);
						}
					}
				}
			} else if (backupScheme == 2)
			{
				// full-history backup
				File newFile = new File(properOutputName);
				if (newFile.exists())
				{
					long modified = newFile.lastModified();
					Date modifiedDate = new Date(modified);
					SimpleDateFormat sdf = new SimpleDateFormat("-yyyy-MM-dd");
					for(int i=0; i<1000; i++)
					{
						String backupFileName = properOutputNameWithoutExtension + sdf.format(modifiedDate);
						if (i != 0)
							backupFileName += "--" + i;
						backupFileName += "." + type.getExtensions()[0];
						File oldFile = new File(backupFileName);
						if (oldFile.exists()) continue;
						if (!newFile.renameTo(oldFile))
						{
							System.out.println("Unable to rename " + newFile + " to " + oldFile);
						}
						break;
					}
				}
			}
			if (type == FileType.ELIB)
			{
				ELIB elib = new ELIB();
				elib.quiet = quiet;
				if (compatibleWith6) elib.write6Compatible();
				if (elib.openBinaryOutputStream(properOutputName)) return true;
                if (CVS.isEnabled()) {
                    CVSLibrary.savingLibrary(lib);
                }
				if (elib.writeLib(lib)) return true;
				if (elib.closeBinaryOutputStream()) return true;
			} else
			{
				JELIB jelib = new JELIB();
				jelib.quiet = quiet;
				if (jelib.openTextOutputStream(properOutputName)) return true;
                if (CVS.isEnabled()) {
                    CVSLibrary.savingLibrary(lib);
                }
				if (jelib.writeLib(lib.getDatabase().backup(), lib.getId(), null, false)) return true;
				if (jelib.closeTextOutputStream()) return true;
			}
 		} else if (type == FileType.READABLEDUMP)
		{
            ReadableDump readableDump = new ReadableDump();
			readableDump.quiet = quiet;
			if (readableDump.openTextOutputStream(properOutputName)) return true;
			if (readableDump.writeLib(lib)) return true;
			if (readableDump.closeTextOutputStream()) return true;
        } else if (type == FileType.DELIB)
        {
            DELIB delib = new DELIB(delibHeaderOnly);
            delib.quiet = quiet;
            if (delib.openTextOutputStream(properOutputName)) return true;
            if (CVS.isEnabled() && !delibHeaderOnly) {
                CVSLibrary.savingLibrary(lib);
            }
            if (delib.writeLib(lib.getDatabase().backup(), lib.getId(), lib.getDelibCellFiles())) return true;
            if (delib.closeTextOutputStream()) return true;
            if (CVS.isEnabled() && !delibHeaderOnly) {
                CVSLibrary.savedLibrary(lib, delib.getDeletedCellFiles(), delib.getWrittenCellFiles());
            }
		} else
		{
			System.out.println("Unknown export type: " + type);
			return true;
		}
		// clean up and return
        lib.setFromDisk();
		if (!quiet) System.out.println(properOutputName + " written");
        // Update the version in library read in memory
        lib.setVersion(Version.getVersion());
        // if using CVS, update state
/*
        if (CVS.isEnabled()) {
            CVSLibrary.updateState(lib);
        }
*/
		return false;
	}
    
    /**
     * Method to write a Cell to a file with a particular format.
	 * In addition to the specified Cell, these formats typically
	 * also include the hierarchy below it.
	 * The alternative is to write the entire library, regardless of
	 * hierarchical structure (use "WriteLibrary").
     * @param cell the Cell to be written.
     * @param context the VarContext of the Cell (its position in the hierarchy above it).
     * @param filePath the path to the disk file to be written.
     * @param type the format of the output file. 
     * @param override a list of overriding polygons to write.
     * NOTE: Keep public for regressions
     */
    public static void writeCell(Cell cell, VarContext context, String filePath, FileType type, List<PolyBase> override)
    {
		if (type == FileType.ARCHSIM)
		{
			ArchSim.writeArchSimFile(cell, filePath);
		} else if (type == FileType.CDL)
		{
			Spice.writeSpiceFile(cell, context, filePath, true);
		} else if (type == FileType.CIF)
		{
			CIF.writeCIFFile(cell, context, filePath);
		} else if (type == FileType.COSMOS)
		{
			Sim.writeSimFile(cell, context, filePath, type);
		} else if (type == FileType.DXF)
		{
			DXF.writeDXFFile(cell, filePath);
		} else if (type == FileType.EAGLE)
		{
			Eagle.writeEagleFile(cell, context, filePath);
		} else if (type == FileType.ECAD)
		{
			ECAD.writeECADFile(cell, context, filePath);
		} else if (type == FileType.EDIF)
		{
			EDIF.writeEDIFFile(cell, context, filePath);
		} else if (type == FileType.ESIM || type == FileType.RSIM)
		{
			Sim.writeSimFile(cell, context, filePath, type);
		} else if (type == FileType.FASTHENRY)
		{
			FastHenry.writeFastHenryFile(cell, context, filePath);
		} else if (type == FileType.HPGL)
		{
			HPGL.writeHPGLFile(cell, context, filePath);
		} else if (type == FileType.GDS)
		{
			GDS.writeGDSFile(cell, context, filePath);
		} else if (type == FileType.IRSIM)
		{
			IRSIM.writeIRSIMFile(cell, context, filePath);
		} else if (type == FileType.L)
		{
			L.writeLFile(cell, filePath);
		} else if (type == FileType.LEF)
		{
			LEF.writeLEFFile(cell, context, filePath);
		} else if (type == FileType.MAXWELL)
		{
			Maxwell.writeMaxwellFile(cell, context, filePath);
		} else if (type == FileType.MOSSIM)
		{
			MOSSIM.writeMOSSIMFile(cell, context, filePath);
		} else if (type == FileType.PADS)
		{
			Pads.writePadsFile(cell, context, filePath);
		} else if (type == FileType.PAL)
		{
			PAL.writePALFile(cell, context, filePath);
		} else if (type == FileType.POSTSCRIPT || type == FileType.EPS)
		{
			PostScript.writePostScriptFile(cell, filePath, override);
		} else if (type == FileType.SILOS)
		{
			Silos.writeSilosFile(cell, context, filePath);
		} else if (type == FileType.SKILL)
		{
			IOTool.writeSkill(cell, filePath, false);
        } else if (type == FileType.SKILLEXPORTSONLY)
        {
            IOTool.writeSkill(cell, filePath, true);
		} else if (type == FileType.SPICE)
		{
			Spice.writeSpiceFile(cell, context, filePath, false);
		} else if (type == FileType.TEGAS)
		{
			Tegas.writeTegasFile(cell, context, filePath);
		} else if (type == FileType.VERILOG)
		{
			Verilog.writeVerilogFile(cell, context, filePath);
		}
    }

	/**
	 * Gathers into objInfo map all objects references from library objects.
	 * @param lib the Library to examine.
	 */
	void gatherReferencedObjects(Library lib)
	{
		objInfo = new HashMap<Object,Integer>();
		nameSpace = new TreeMap<String,Short>(TextUtils.STRING_NUMBER_ORDER);
		for (Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
		{
			Cell cell = cIt.next();
			gatherCell(cell);

			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.getName() == null)
					System.out.println("ERROR: " + cell + " has " + ni + " with no name");
				NodeProto np = ni.getProto();
				if (ni.isCellInstance())
				{
					gatherCell((Cell)np);
				} else
				{
					gatherObj(np);
					gatherObj(((PrimitiveNode)np).getTechnology());
				}
				for (Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
				{
					PortInst pi = pIt.next();
					gatherVariables(pi);
				}
				gatherVariables(ni);
                gatherFont(ni.getTextDescriptor(NodeInst.NODE_NAME));
                gatherFont(ni.getTextDescriptor(NodeInst.NODE_PROTO));
			}

			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				ArcProto ap = ai.getProto();
				gatherObj(ap);
				gatherObj(ap.getTechnology());
				//gatherObj(ai.getHead().getPortInst().getPortProto());
				//gatherObj(ai.getTail().getPortInst().getPortProto());
				gatherVariables(ai);
				gatherFont(ai.getTextDescriptor(ArcInst.ARC_NAME));
			}

			for(Iterator<Export> it = cell.getExports(); it.hasNext(); )
			{
				Export e = it.next();
				//gatherObj(e.getOriginalPort().getPortProto());
				gatherVariables(e);
				gatherFont(e.getTextDescriptor(Export.EXPORT_NAME));
			}

			gatherVariables(cell);
		}
		gatherVariables(lib);

		for (Iterator<Tool> it = Tool.getTools(); it.hasNext(); )
			gatherMeaningPrefs(it.next());

		for (Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
			gatherMeaningPrefs(it.next());

        putNameSpace(Library.FONT_ASSOCIATIONS.getName());
        putNameSpace(NodeInst.NODE_NAME.getName());
        putNameSpace(ArcInst.ARC_NAME.getName());
        short varIndex = 0;
        for (Map.Entry<String,Short> e : nameSpace.entrySet()) {
            e.setValue(new Short(varIndex++));
        }
	}

	/**
	 * Gather variables of ElectricObject into objInfo map.
	 * @param eObj ElectricObject with variables.
	 */
	private void gatherVariables(ElectricObject eObj)
	{
		for (Iterator<Variable> it = eObj.getVariables(); it.hasNext(); )
		{
			Variable var = it.next();
			Object value = var.getObjectInDatabase(EDatabase.serverDatabase());
			if (nameSpace != null) putNameSpace(diskName(eObj, var));
			gatherFont(var.getTextDescriptor());
			int length = value instanceof Object[] ? ((Object[])value).length : 1;
			for (int i = 0; i < length; i++)
			{
				Object v = value instanceof Object[] ? ((Object[])value)[i] : value;
				if (v == null) continue;
				if (v instanceof Technology || v instanceof Tool)
				{
					gatherObj(v);
				} else if (v instanceof PrimitiveNode)
				{
					gatherObj(v);
					gatherObj(((PrimitiveNode)v).getTechnology());
				} else if (v instanceof PrimitivePort)
				{
					//gatherObj(v);
					PrimitiveNode pn = (PrimitiveNode)((PrimitivePort)v).getParent();
					gatherObj(pn);
					gatherObj(pn.getTechnology());
				} else if (v instanceof ArcProto)
				{
					gatherObj(v);
					gatherObj(((ArcProto)v).getTechnology());
				} else if (v instanceof ElectricObject)
				{
					gatherObj(v);
					Cell cell = ((ElectricObject)v).whichCell();
					if (cell != null) gatherCell(cell);
				}
			}
		}
	}

	/**
	 * Gather meaning preferences attached to object into objInfo map.
	 * @param obj Object with attached meaning preferences.
	 */
	private void gatherMeaningPrefs(Object obj)
	{
		for(Pref pref : Pref.getMeaningVariables(obj))
		{
			gatherObj(obj);
			String name = pref.getPrefName();
			if (nameSpace != null) putNameSpace(name);
		}
	}

	/**
	 * Gather Cell, its Library and its font into objInfo map.
	 * @param cell Cell to examine.
	 */
	private void gatherCell(Cell cell)
	{
		gatherObj(cell);
		gatherObj(cell.getLibrary());
		gatherObj(cell.getView());
	}

	/**
	 * Gather ActiveFont object of the TextDescriptor into objInfo map.
	 * @param td TextDescriptor to examine.
	 */
	private void gatherFont(TextDescriptor td)
	{
		int face = td.getFace();
		if (face != 0) gatherObj(TextDescriptor.ActiveFont.findActiveFont(face));
	}

	/**
	 * Gather object into objInfo map.
	 * @param obj Object to put.
	 */
	private void gatherObj(Object obj)
	{
		objInfo.put(obj, null);
	}

	/**
	 * Put index of object into objInfo map.
	 * @param obj Object to put.
	 * @param index index of object.
	 */
	void putObjIndex(Object obj, int index)
	{
		objInfo.put(obj, new Integer(index));
	}

	/**
	 * Put string into variable name space.
	 * @param name name to put.
	 */
	void putNameSpace(String name)
	{
		nameSpace.put(name, null);
	}

	/**
	 * Returns variable disk name. Usually it is variable name.
	 * Disk name of PortInst variables is key ATTRP_portName_varName.
     * @param owner owner of the Variable.
	 * @param var Variable.
	 * @return disk name of variable.
	 */
    String diskName(ElectricObject owner, Variable var) {
        String portName = null;
        if (owner instanceof PortInst) {
            PortInst pi = (PortInst)owner;
            portName = pi.getPortProto().getName();
        }
        return diskName(portName, var);
    }

	/**
	 * Returns variable disk name. Usually it is variable name.
	 * Disk name of PortInst variables is key ATTRP_portName_varName.
     * @param portName port name or null.
	 * @param var Variable.
	 * @return disk name of variable.
	 */
    String diskName(String portName, Variable var) {
        if (portName == null) return var.getKey().getName();
        StringBuffer sb = new StringBuffer("ATTRP_");
        for (int i = 0; i < portName.length(); i++) {
            char ch = portName.charAt(i);
            if (ch == '\\' || ch == '_') sb.append('\\');
            sb.append(ch);
        }
        sb.append('_');
        sb.append(var.getKey().getName());
        return sb.toString();
    }

	/**
	 * Method to gather all font settings in a Library.
	 * The method examines all TextDescriptors that might be saved with the Library
	 * and returns an array of Strings that describes the font associations.
	 * Each String is of the format NUMBER/FONTNAME where NUMBER is the font number
	 * in the TextDescriptor and FONTNAME is the font name.
	 * @return font association array or null.
	 */
	String[] createFontAssociation()
	{
		int maxIndices = TextDescriptor.ActiveFont.getMaxIndex();
		faceMap = new int[maxIndices + 1];
		TreeMap<String,TextDescriptor.ActiveFont> sortedFonts = new TreeMap<String,TextDescriptor.ActiveFont>();
		for (int i = 1; i <= maxIndices; i++)
		{
			TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(i);
			if (!objInfo.containsKey(af)) continue;
			sortedFonts.put(af.getName(), af);
		}
		if (sortedFonts.size() == 0) return null;
		String[] fontAssociation = new String[sortedFonts.size()];
		int face = 0;
		for (TextDescriptor.ActiveFont af : sortedFonts.values())
		{
			face++;
			faceMap[af.getIndex()] = face;
			fontAssociation[face-1] = face + "/" + af.getName();
		}
		return fontAssociation;
	}

    /**
     * Opens the dataOutputStream for writing of binary files.
     * @return true on error.
     */
    protected boolean openBinaryOutputStream(String filePath)
    {
		this.filePath = filePath;
        FileOutputStream fileOutputStream;
		try
		{
			fileOutputStream = new FileOutputStream(filePath);
		} catch (FileNotFoundException e)
		{
			System.out.println("Could not write file " + filePath);
			System.out.println("Reason: " + e.getMessage());
			return true;
		}
		BufferedOutputStream bufStrm = new BufferedOutputStream(fileOutputStream);
		dataOutputStream = new DataOutputStream(bufStrm);
        return false;
    }
    
    /** 
     * Closes the dataOutputStream.
     * @return true on error.
     */
    protected boolean closeBinaryOutputStream()
    {
		try
		{
			dataOutputStream.close();
		} catch (IOException e)
		{
			System.out.println("Error closing " + filePath);
			return true;
		}
        return false;
    }
    
    /**
     * Open the printWriter for writing text files
     * @return true on error.
     */
    protected boolean openTextOutputStream(String filePath)
    {
		this.filePath = filePath;
        try
		{
            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
        } catch (IOException e)
		{
            System.out.println("Error opening " + filePath+": "+e.getMessage());
            return true;
        }
        return false;
    }

	/** 
     * Close the printWriter.
     * @return true on error.
     */
    protected boolean closeTextOutputStream()
    {
        printWriter.close();
        return false;
    }

	protected void emitCopyright(String prefix, String postfix)
	{
		if (!IOTool.isUseCopyrightMessage()) return;
		String str = IOTool.getCopyrightMessage();
		int start = 0;
		while (start < str.length())
		{
			int endPos = str.indexOf('\n', start);
			if (endPos < 0) endPos = str.length();
			String oneLine = str.substring(start, endPos);
			printWriter.println(prefix + oneLine + postfix);
			start = endPos+1;
		}
	}

	/** number of characters written on a line */	private int lineCharCount = 0;
	private int maxWidth = 80;
	private boolean strictWidthLimit = false;
	private char commentChar = 0;
	private String continuationString = "";

	protected void setOutputWidth(int width, boolean strict) { maxWidth = width;   strictWidthLimit = strict; }

	protected void setCommentChar(char ch) { commentChar = ch; }

	protected void setContinuationString(String str) { continuationString = str; }

	private void writeChunk(String str)
	{
		int len = str.length();
		if (len <= 0) return;
		printWriter.print(str);
		lineCharCount += len;
		if (str.charAt(len-1) == '\n') lineCharCount = 0;
	}

	/**
	 * Write to the file, but break into printable lines
	 */
	protected void writeWidthLimited(String str)
	{
		for(;;)
		{
			int len = str.length();
			if (len <= 0) break;
			int i = str.indexOf('\n');
			if (i < 0) i = len; else i++;
			if (lineCharCount + i < maxWidth)
			{
				// the next chunk fits: write it
				String chunk = str;
				if (i < len) chunk = str.substring(0, i);
				writeChunk(chunk);

				str = str.substring(i);
				if (str.length() == 0) break;
				continue;
			}

			// find place to break the line
			int left = maxWidth - lineCharCount;
			String exact = str.substring(0, left);
			int splitPos = exact.lastIndexOf(' ');
			if (splitPos < 0)
			{
				splitPos = exact.lastIndexOf(',');
				if (splitPos < 0)
				{
					splitPos = exact.lastIndexOf('(');
					if (splitPos < 0)
					{
						splitPos = exact.lastIndexOf(')');
					}
				}
			}
			if (splitPos > 0) exact = exact.substring(0, splitPos+1); else
			{
				if (!strictWidthLimit)
				{
					// allow a wider line
					splitPos = str.indexOf(' ', left);
					if (splitPos < 0)
					{
						splitPos = str.indexOf(',', left);
						if (splitPos < 0)
						{
							splitPos = str.indexOf('(', left);
							if (splitPos < 0)
							{
								splitPos = str.indexOf(')', left);
							}
						}
					}
					if (splitPos > 0) exact = str.substring(0, splitPos+1);
				}
			}

			writeChunk(exact);
			writeChunk("\n");
			if (continuationString.length() > 0) writeChunk(continuationString);
			str = str.substring(exact.length());
		}
	}

	/**
	 * Method to determine the area of a cell that is to be printed.
	 * Returns null if the area cannot be determined.
	 */
	public Rectangle2D getAreaToPrint(Cell cell, boolean reduce, EditWindow_ wnd)
	{
		Rectangle2D bounds = cell.getBounds();
		if (wnd != null) bounds = wnd.getBoundsInWindow();

		// extend it and make it square
		if (reduce)
		{
			double wid = bounds.getWidth() * 0.75;
			double hei = bounds.getHeight() * 0.75;
//			us_squarescreen(el_curwindowpart, NOWINDOWPART, FALSE, lx, hx, ly, hy, 0);
			bounds.setRect(bounds.getCenterX(), bounds.getCenterY(), wid, hei);
		}

		if (IOTool.getPlotArea() != 0)
		{
			if (wnd == null)
			{
				System.out.println("No current window: printing entire cell");
			} else
			{
				if (IOTool.getPlotArea() == 2)
				{
					bounds = wnd.getDisplayedBounds();
				} else
				{
					Rectangle2D hBounds = wnd.getHighlightedArea();
					if (hBounds == null || hBounds.getWidth() == 0 || hBounds.getHeight() == 0)
					{
						System.out.println("Warning: no highlighted area; printing entire cell");
					} else
					{
						bounds = hBounds;
					}
				}
			}
		}
		return bounds;
	}

    /**
     * Class to define cell information during output.
     */
    public static class OutputCellInfo extends Job
    {
        private Cell cell;
        private VarContext context;
        private String filePath;
        private FileType type;
        private List<PolyBase> override;

        /**
         * @param cell the Cell to be written.
         * @param context the VarContext of the Cell (its position in the hierarchy above it).
         * @param filePath the path to the disk file to be written.
         * @param override the list of Polys to write instead of cell contents.
         */
        public OutputCellInfo(Cell cell, VarContext context, String filePath, FileType type, List<PolyBase> override)
        {
            super("Export "+cell+" ("+type+")", IOTool.getIOTool(), Job.Type.EXAMINE, null, null, Priority.USER);
            this.cell = cell;
            this.context = context;
            this.filePath = filePath;
            this.type = type;
            this.override = override;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            writeCell(cell, context, filePath, type, override);
            return true;
        }

    }

	/**
	 * Class to write a library in a CHANGE Job.
	 * Used by regressions that need to queue the output after existing change jobs.
	 */
	public static class WriteJELIB extends Job
    {
        private Library lib;
        private String newName;
        private IdMapper idMapper;

        public WriteJELIB(Library lib, String newName)
        {
            super("Write "+lib, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.lib = lib;
            this.newName = newName;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            boolean error = false;
            try
            {
                if (newName != null) {
                    URL libURL = TextUtils.makeURLToFile(newName);
                    lib.setLibFile(libURL);
                    idMapper = lib.setName(TextUtils.getFileNameWithoutExtension(libURL));
                    if (idMapper != null)
                        lib = EDatabase.serverDatabase().getLib(idMapper.get(lib.getId()));
                }
                fieldVariableChanged("idMapper");
                error = Output.writeLibrary(lib, FileType.JELIB, false, false, false);
            } catch (Exception e)
            {
            	throw new JobException("Exception caught when saving library: " + e.getMessage());
            }
            return !error;
        }
        
        public void terminateOK() {
            User.fixStaleCellReferences(idMapper);
        }
    }
}


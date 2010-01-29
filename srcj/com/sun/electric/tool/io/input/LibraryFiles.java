/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibraryFiles.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.IdMapper;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.id.*;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.IconParameters;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class reads Library files (ELIB or readable dump) format.
 */
public abstract class LibraryFiles extends Input
{
	/** key of Varible holding true library of fake cell. */                public static final Variable.Key IO_TRUE_LIBRARY = Variable.newKey("IO_true_library");
	/** key of Variable to denote a dummy cell or library */                public static final Variable.Key IO_DUMMY_OBJECT = Variable.newKey("IO_dummy_object");

    /** Database where to load libraries */                                 final EDatabase database = EDatabase.serverDatabase();
    /** IdManager where to create ids */                                    final IdManager idManager = database.getIdManager();
	/** The Library being input. */                                         protected Library lib;
	/** true if the library is the main one being read. */                  protected boolean topLevelLibrary;
	// the cell information
	/** The number of Cells in the file. */									protected int nodeProtoCount;
	/** A list of cells being read. */										protected Cell [] nodeProtoList;
	/** lambda value for each cell of the library */						protected double [] cellLambda;
	/** total number of cells in all read libraries */						protected static int totalCells;
	/** number of cells constructed so far. */								protected static int cellsConstructed;
	/** a List of scaled Cells that got created */							protected List<Cell> scaledCells;
	/** Number of errors in this LibraryFile */								protected int errorCount;
	/** the Electric version in the library file. */						protected Version version;
	/** true if old MOSIS CMOS technologies appear in the library */		protected boolean convertMosisCmosTechnologies;
	/** true to scale lambda by 20 */										protected boolean scaleLambdaBy20;
	/** true if rotation mirror bits are used */							protected boolean rotationMirrorBits;
    /** SizeCorrectors for used technologies */                             protected HashMap<Technology,Technology.SizeCorrector> sizeCorrectors = new HashMap<Technology,Technology.SizeCorrector>();
	/** font names obtained from FONT_ASSOCIATIONS */                       private String [] fontNames;
    /** buffer for reading text descriptors and variable flags. */          MutableTextDescriptor mtd = new MutableTextDescriptor();
    /** buffer for reading Variables. */                                    ArrayList<Variable> variablesBuf = new ArrayList<Variable>();

	/** the path to the library being read. */                              protected static String mainLibDirectory = null;
	/** collection of libraries and their input objects. */					private static List<LibraryFiles> libsBeingRead;
    /** collection of undefined Nodes/Technologies */                       static Map<TechId, Set<PrimitiveNodeId>> undefinedTechsAndPrimitives;
    /** Project preferences from library file. */                              HashMap<Setting,Object> projectSettings = new HashMap<Setting,Object>();
	protected static final boolean VERBOSE = false;
	protected static final double TINYDISTANCE = DBMath.getEpsilon()*2;

    static class NodeInstList
	{
		NodeInst []  theNode;
		NodeProto [] protoType;
		String []    name;
        TextDescriptor[] nameTextDescriptor;
		int []       lowX;
		int []       highX;
		int []       lowY;
		int []       highY;
		int []       anchorX;
		int []       anchorY;
		short []     rotation;
		int []       transpose;
        TextDescriptor[] protoTextDescriptor;
		int []       userBits;
        Variable[][] vars;

        NodeInstList(int nodeCount, boolean hasAnchor)
        {
            theNode = new NodeInst[nodeCount];
            protoType = new NodeProto[nodeCount];
            name = new String[nodeCount];
            nameTextDescriptor = new TextDescriptor[nodeCount];
            lowX = new int[nodeCount];
            highX = new int[nodeCount];
            lowY = new int[nodeCount];
            highY = new int[nodeCount];
            if (hasAnchor) {
                anchorX = new int[nodeCount];
                anchorY = new int[nodeCount];
            }
            rotation = new short[nodeCount];
            transpose = new int[nodeCount];
            protoTextDescriptor = new TextDescriptor[nodeCount];
            userBits = new int[nodeCount];
            vars = new Variable[nodeCount][];
        }
	};

	LibraryFiles() {}

	/**
	 * Method to read a Library from disk.
	 * This method is for reading full Electric libraries in ELIB, JELIB, and Readable Dump format.
     * This method doesn't read project preferences contained in library file.
	 * @param fileURL the URL to the disk file.
	 * @param libName the name to give the library (null to derive it from the file path)
	 * @param type the type of library file (ELIB, JELIB, etc.)
	 * @param quick true to read the library without verbosity (used when reading a library internally).
     * @param iconParams icon parameters used in the creation of exports.
	 * @return the read Library, or null if an error occurred.
	 */
	public static Library readLibrary(URL fileURL, String libName, FileType type, boolean quick, IconParameters iconParams) {
        return readLibrary(fileURL, libName, type, quick, null, iconParams);
    }

	/**
	 * Method to read a Library from disk.
	 * This method is for reading full Electric libraries in ELIB, JELIB, and Readable Dump format.
     * This method doesn't read project preferences contained in library file.
	 * @param fileURL the URL to the disk file.
	 * @param libName the name to give the library (null to derive it from the file path)
	 * @param type the type of library file (ELIB, JELIB, etc.)
	 * @param quick true to read the library without verbosity (used when reading a library internally).
	 * @param projectSettings an output map which is filled by project preferences of top library
	 * @return the read Library, or null if an error occurred.
	 */
	public static Library readLibrary(URL fileURL, String libName, FileType type, boolean quick,
                                      Map<Setting,Object> projectSettings, IconParameters iconParams) {
		if (fileURL == null) return null;
		long startTime = System.currentTimeMillis();
        errorLogger = ErrorLogger.newInstance("Library Read");

        File f = new File(fileURL.getPath());

        if (f != null && f.exists()) {
            LibDirs.readLibDirs(f.getParent());
        }
		LibraryFiles.initializeLibraryInput();

		Library lib = null;
		boolean formerQuiet = isChangeQuiet();
		if (!formerQuiet) changesQuiet(true);
		try {
			// show progress
			if (!quick) startProgressDialog("library", fileURL.getFile());

			Cell.setAllowCircularLibraryDependences(true);

			StringBuffer errmsg = new StringBuffer();
			boolean exists = TextUtils.URLExists(fileURL, errmsg);
			if (!exists)
			{
				System.out.print(errmsg.toString());
				// if doesn't have extension, assume DEFAULTLIB as extension
				String fileName = fileURL.toString();
				if (fileName.indexOf(".") == -1)
				{
					fileURL = TextUtils.makeURLToFile(fileName+"."+type.getFirstExtension());
					System.out.print("Attempting to open " + fileURL+"\n");
					errmsg.setLength(0);
					exists = TextUtils.URLExists(fileURL, errmsg);
					if (!exists && (type != FileType.DELIB)) { // Check if this is a DELIB
						fileURL = TextUtils.makeURLToFile(fileName+"."+FileType.DELIB.getFirstExtension());
						System.out.print("Attempting to open " + fileURL+"\n");
						errmsg.setLength(0);
						exists = TextUtils.URLExists(fileURL, errmsg);
						if (exists) { // If it is a DELIB
							libName = null;        // Get the library name from fileURL
							type = FileType.DELIB; // Set the type to DELIB
						}
					}
					if (!exists) System.out.print(errmsg.toString());
				}
			}
			if (exists)
			{
				// get the library name
				if (libName == null) libName = TextUtils.getFileNameWithoutExtension(fileURL);
				lib = readALibrary(fileURL, null, libName, type, projectSettings, iconParams);
			}
			if (LibraryFiles.VERBOSE)
				System.out.println("Done reading data for all libraries");

			LibraryFiles.cleanupLibraryInput();
			if (LibraryFiles.VERBOSE)
				System.out.println("Done instantiating data for all libraries");
		} finally {
			if (!quick) stopProgressDialog();
			Cell.setAllowCircularLibraryDependences(false);
		}
		if (!formerQuiet) changesQuiet(formerQuiet);
		if (lib != null && !quick)
		{
			long endTime = System.currentTimeMillis();
			float finalTime = (endTime - startTime) / 1000F;
			System.out.println("Library " + fileURL.getFile() + " read, took " + finalTime + " seconds");
		}

        // if CVS is enabled, get status of library
//        if (CVS.isEnabled()) {
//            Update.updateOpenLibraries(Update.UpdateEnum.STATUS);
//        }
		errorLogger.termLogging(true);

		return lib;
	}

	/**
	 * Method to read a Library from disk.
	 * This method is for reading full Electric libraries in ELIB, JELIB, and Readable Dump format.
	 * @param fileURL the URL to the disk file.
	 * @param type the type of library file (ELIB, JELIB, etc.)
	 * @return the read Library, or null if an error occurred.
	 */
	public static synchronized Map<String,Object> readProjectsSettingsFromLibrary(URL fileURL, FileType type)
	{
		if (fileURL == null) return null;
		long startTime = System.currentTimeMillis();
        errorLogger = ErrorLogger.newInstance("Library Read Project Preferences");

		Library lib = null;
        StringBuffer errmsg = new StringBuffer();
        boolean exists = TextUtils.URLExists(fileURL, errmsg);
        if (!exists)
        {
            System.out.print(errmsg.toString());
            // if doesn't have extension, assume DEFAULTLIB as extension
            String fileName = fileURL.toString();
            if (fileName.indexOf(".") == -1)
            {
                fileURL = TextUtils.makeURLToFile(fileName+"."+type.getFirstExtension());
                System.out.print("Attempting to open " + fileURL+"\n");
                errmsg.setLength(0);
                exists = TextUtils.URLExists(fileURL, errmsg);
                if (!exists) {
                    System.out.print(errmsg.toString());
                    return null;
                }
            }
        }
        // handle different file types
        if (type == FileType.JELIB || type == FileType.DELIB) {
            TechPool techPool = EDatabase.serverDatabase().getTechPool();
            try {
                return settingsByXml(JELIB.readProjectSettings(fileURL, type, techPool, errorLogger));
            } finally {
                errorLogger.termLogging(true);
            }
        }
        LibraryFiles in;
        if (type == FileType.ELIB)
        {
            in = new ELIB(null);
            if (in.openBinaryInput(fileURL)) return null;
        } else if (type == FileType.READABLEDUMP)
        {
            in = new ReadableDump();
            if (in.openTextInput(fileURL)) return null;
        } else
        {
            System.out.println("Unknown import type: " + type);
            return null;
        }

        // determine whether this is top-level
        in.topLevelLibrary = true;

        // read the library
        boolean error = in.readProjectSettings();
        in.closeInput();
        if (error)
        {
            System.out.println("Error reading " + lib);
            if (in.topLevelLibrary) mainLibDirectory = null;
            return null;
        }
        long endTime = System.currentTimeMillis();
        float finalTime = (endTime - startTime) / 1000F;
        System.out.println("Library " + fileURL.getFile() + " read, took " + finalTime + " seconds");
        errorLogger.termLogging(true);

		return settingsByXml(in.projectSettings);
	}

    private static Map<String,Object> settingsByXml(Map<Setting,Object> settings) {
        Map<String,Object> settingsByXml = new HashMap<String,Object>();
        // settings is null if there were previous errros in the execution
        if (settings != null)
        {
            for (Map.Entry<Setting,Object> e: settings.entrySet())
                settingsByXml.put(e.getKey().getXmlPath(), e.getValue());
        }
        return settingsByXml;
    }

	/**
	 * Method to read a single library file.
	 * @param fileURL the URL to the file.
	 * @param lib the Library to read.
	 * If the "lib" is null, this is an entry-level library read, and one is created.
	 * If "lib" is not null, this is a recursive read caused by a cross-library
	 * reference from inside another library.
	 * @param type the type of library file (ELIB, CIF, GDS, etc.)
	 * @return the read Library, or null if an error occurred.
	 */
	protected static Library readALibrary(URL fileURL, Library lib, String libName, FileType type,
                                          Map<Setting,Object> projectSettings, IconParameters iconParams)
	{
		// handle different file types
		LibraryFiles in;
		if (type == FileType.ELIB)
		{
			in = new ELIB(iconParams);
			if (in.openBinaryInput(fileURL)) return null;
		} else if (type == FileType.JELIB || type == FileType.DELIB)
		{
            try {
                LibId libId = lib != null ? lib.getId() : EDatabase.serverDatabase().getIdManager().newLibId(libName);
                in = new JELIB(libId, fileURL, type, iconParams);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
		} else if (type == FileType.READABLEDUMP)
		{
			in = new ReadableDump();
			if (in.openTextInput(fileURL)) return null;
		} else
		{
			System.out.println("Unknown import type: " + type);
			return null;
		}

		// determine whether this is top-level
		in.topLevelLibrary = false;
		if (lib == null)
		{
			mainLibDirectory = TextUtils.getFilePath(fileURL);
            if (type == FileType.DELIB) {
                mainLibDirectory = mainLibDirectory.replaceAll(libName+"."+type.getFirstExtension(), "");
            }
			in.topLevelLibrary = true;
		}

		if (lib == null)
		{
			// create a new library
			lib = Library.newInstance(libName, fileURL);
		}

		in.lib = lib;

		// read the library
		boolean error = in.readInputLibrary();
		in.closeInput();
		if (error)
		{
			System.out.println("Error reading " + lib);
			if (in.topLevelLibrary) mainLibDirectory = null;
			return null;
		}
//        if (CVS.isEnabled()) {
//            CVSLibrary.addLibrary(lib);
//        }
        if (projectSettings != null)
            projectSettings.putAll(in.projectSettings);
		return in.lib;
	}

    /**
     * Reload a library from disk.  Note this is different from calling
     * reloadLibraryCells(List<Cell>) with a list of Cells in the library,
     * because it also reloads new cells from disk that are not currently in memory.
     * @param lib the Library to reload.
	 * @return mapping of Library/Cell/Export ids, null if the library was renamed.
     */
    public static IdMapper reloadLibrary(Library lib, IconParameters iconParameters) {
        if (!lib.isFromDisk()) {
            System.out.println("No disk file associated with this library, cannot reload from disk");
            return null;
        }
        FileType type = OpenFile.getOpenFileType(lib.getLibFile().getPath(), FileType.JELIB);

        // rename old library, read in disk library as replacement
        // replace all cells in old library by newly read-in cells from disk library

        String name = lib.getName();
        URL libFile = lib.getLibFile();
        boolean isCurrent = Library.getCurrent() == lib;
        IdMapper idMapper = lib.setName("___old___"+name);
        if (idMapper == null) return null;
        lib = idMapper.get(lib.getId()).inDatabase(EDatabase.serverDatabase());
        lib.setHidden();                // hide the old library
        startProgressDialog("library", name);
        Library newLib = readLibrary(libFile, name, type, true, iconParameters);
        stopProgressDialog();
        if (isCurrent)
            Job.setCurrentLibraryInJob(newLib);

        // replace all old cells with new cells
        Cell.setAllowCircularLibraryDependences(true);
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell oldCell = it.next();
            String cellName = oldCell.getCellName().toString();

            Cell newCell = newLib.findNodeProto(cellName);
            if (newCell == null) {
                // doesn't exist in new library, copy it over
                System.out.println("Warning, Cell "+oldCell.describe(false)+" does not exist in reloaded library. Copying it to reloaded library.");
                newCell = Cell.copyNodeProto(oldCell, newLib, cellName, true);
            }
            // replace all usages of old cell with new cell
            List<NodeInst> instances = new ArrayList<NodeInst>();
            for(Iterator<NodeInst> it2 = oldCell.getInstancesOf(); it2.hasNext(); ) {
                instances.add(it2.next());
            }
            for (NodeInst ni : instances) {
                CircuitChangeJobs.replaceNodeInst(ni, newCell, true, true);
            }
        }
        Cell.setAllowCircularLibraryDependences(false);

        // close temp library
        lib.kill("delete old library");
        System.out.println("Reloaded library "+newLib.getName()+" from disk.");
        return idMapper;
    }

    /**
     * Reload Cells from disk.  Other cells in the library will not be affected.
     * These cells must all be from the same library.
     */
//    public static void reloadLibraryCells(List<Cell> cellList) {
//        if (cellList == null || cellList.size() == 0) return;
//        Library lib = cellList.get(0).getLibrary();
//
//        if (!lib.isFromDisk()) {
//            System.out.println("No disk file associated with this library, cannot reload from disk");
//            return;
//        }
//        FileType type = OpenFile.getOpenFileType(lib.getLibFile().getPath(), FileType.JELIB);
//
//        // Load disk library as temp library.
//        // Then, copy over new cells from temp library to replace current cells in memory,
//        // then close temp library.
//
//        String name = lib.getName();
//        URL libFile = lib.getLibFile();
//        startProgressDialog("library", name);
//        Library newLib = readLibrary(libFile, "___reloaded___"+name, type, true, iconParams);
//        stopProgressDialog();
//        newLib.setHidden();
//
//        // replace all old cells with new cells
//        Cell.setAllowCircularLibraryDependences(true);
//        for (Cell oldCell : cellList) {
//            String cellName = oldCell.getCellName().toString(); // contains version and view info
//
//            Cell newCell = newLib.findNodeProto(cellName);
//            if (newCell == null) {
//                System.out.println("Cell "+oldCell.describe(false)+" cannot be reloaded, it does not exist in disk library");
//                continue;
//            }
//
//            // rename oldCell
//            String renamedName = "___old___"+oldCell.getName();
//            IdMapper idMapper = oldCell.rename(renamedName, renamedName);      // must not contain version or view info
//            if (idMapper == null) continue;
//            oldCell = idMapper.get(oldCell.getId()).inDatabase(EDatabase.serverDatabase());
//
//            // copy newCell over with oldCell's name
//            newCell = Cell.copyNodeProto(newCell, lib, cellName, true);
//
//            // replace all usages of old cell with new cell
//            List<NodeInst> instances = new ArrayList<NodeInst>();
//            for(Iterator<NodeInst> it2 = oldCell.getInstancesOf(); it2.hasNext(); ) {
//                instances.add(it2.next());
//            }
//            for (NodeInst ni : instances) {
//                CircuitChangeJobs.replaceNodeInst(ni, newCell, true, true);
//            }
//
//            // kill oldCell
//            oldCell.kill();
//        }
//        Cell.setAllowCircularLibraryDependences(false);
//
//        // close temp library
//        newLib.kill("delete temp library");
//        System.out.println("Reloaded Cells from disk.");
//    }

	// *************************** THE CREATION INTERFACE ***************************

	public static void initializeLibraryInput()
	{
		libsBeingRead = new ArrayList<LibraryFiles>();
        undefinedTechsAndPrimitives = new HashMap<TechId, Set<PrimitiveNodeId>>();
    }

	public boolean readInputLibrary()
	{
		// add this reader to the list
        assert(!libsBeingRead.contains(this));
        libsBeingRead.add(this);
		//libsBeingRead.put(lib, this);
		scaledCells = new ArrayList<Cell>();

		try
		{
			if (readTheLibrary(false, null))
                return true;
            Map<Cell,Variable[]> originalVars = createLibraryCells(false);
            realizeCellVariables(originalVars);
            return false;
		} catch (IOException e)
		{
			System.out.println("End of file reached while reading " + filePath);
			return true;
		}
	}

    abstract boolean readTheLibrary(boolean onlyProjectSettings, LibraryStatistics.FileContents fc) throws IOException;
    abstract Map<Cell,Variable[]> createLibraryCells(boolean onlyProjectSettings);

    private void realizeCellVariables(Map<Cell,Variable[]> originalVars) {
        HashSet<Cell.CellGroup> visitedCellGroups = new HashSet<Cell.CellGroup>();
        for (Cell cell: originalVars.keySet()) {
            Cell.CellGroup cellGroup = cell.getCellGroup();
            if (visitedCellGroups.contains(cellGroup)) continue;
            realizeCellVariables(cellGroup, originalVars);
            visitedCellGroups.add(cellGroup);
        }
    }

    /**
     * Realize cell variables on all cells of a cell group
     * @param cellGroup the cell group to realize variables
     * @param originalVars original variables from disk library
     */
    private void realizeCellVariables(Cell.CellGroup cellGroup, Map<Cell,Variable[]> originalVars) {
        // Determine and create group parameters
        Cell cellOwner = cellGroup.getParameterOwner();
        HashMap<Variable.AttrKey,Variable> groupParams = null;
        if (cellOwner != null) {
            groupParams = new HashMap<Variable.AttrKey,Variable>();
            Variable[] ownerVars = originalVars.get(cellOwner);
            for (Variable var: ownerVars) {
                if (!var.getTextDescriptor().isParam()) continue;
                if (cellOwner.isDeprecatedVariable(var.getKey())) continue;
                if (var.getKey() == NccCellAnnotations.NCC_ANNOTATION_KEY) continue;
                var = var.withInherit(true);
                cellGroup.addParam(var);
                groupParams.put((Variable.AttrKey)var.getKey(), var);
            }
        }

        /**
         * Modify attributes of group parameters in each icon and schematic cell
         * Realize variables
         */
        for (Iterator<Cell> it = cellGroup.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            Variable[] origVars = originalVars.get(cell);
            if (cell.isIcon() || cell.isSchematic())
                realizeCellVariables(cell, origVars, groupParams);
            else
                realizeVariables(cell, origVars);
        }
    }

    private void realizeCellVariables(Cell cell, Variable[] origVars, HashMap<Variable.AttrKey,Variable> groupParams) {
        int foundParams = 0;
        for (Variable var: origVars) {
            if (var.isAttribute()) {
                Variable.Key varKey = var.getKey();
                Variable groupParam = groupParams.get(varKey);
                if (groupParam != null) {
                    if (!var.getTextDescriptor().isParam()) {
                        String msg = "Attribute \"" + var.getTrueName() + "\" on " + cell +
                                " must be parameter as on " + cell.getCellGroup().getParameterOwner();
                        errorLogger.logError(msg, EPoint.fromLambda(var.getXOff(), var.getYOff()), cell, 2);
                        var = var.withParam(true);
                    }
                    var = var.withInherit(true);
                    if (!var.getObject().equals(groupParam.getObject())) {
                        String msg = "Parameter \"" + var.getTrueName() +
                                "\" has value " + var.getPureValue(-1) + " on " + cell +
                                " instead of " + groupParam.getPureValue(-1) + " as on " + cell.getCellGroup().getParameterOwner();
                        errorLogger.logError(msg, EPoint.fromLambda(var.getXOff(), var.getYOff()), cell, 2);
                        var = var.withObject(groupParam.getObject());
                    }
                    if (var.getUnit() != groupParam.getUnit()) {
                        String msg = "Parameter \"" + var.getTrueName() +
                                "\" has unit " + var.getUnit() + " on " + cell +
                                " instead of " + groupParam.getUnit() + " as on " + cell.getCellGroup().getParameterOwner();
                        errorLogger.logError(msg, EPoint.fromLambda(var.getXOff(), var.getYOff()), cell, 2);
                        var = var.withUnit(groupParam.getUnit());
                    }
                    foundParams++;
                    cell.setTextDescriptor(var.getKey(), var.getTextDescriptor());
                    continue;
                }
                if (var.getTextDescriptor().isParam()) {
                    String msg = cell + " has extra parameter \"" + var.getTrueName() +
                            "\" compared to " + cell.getCellGroup().getParameterOwner();
                    errorLogger.logError(msg, EPoint.fromLambda(var.getXOff(), var.getYOff()), cell, 2);
                    var = var.withParam(false);
                    if (var.getKey() == NccCellAnnotations.NCC_ANNOTATION_KEY)
                        var = var.withInherit(false).withInterior(false);
                }
            }
            realizeVariable(cell, var);
        }
        if (foundParams == groupParams.size()) return;

        // Create parameters which were omitted in the disk library
        HashSet<Variable.AttrKey> omittedParams = new HashSet<Variable.AttrKey>(groupParams.keySet());
        for (Variable var: origVars)
            omittedParams.remove(var.getKey());
        assert omittedParams.size() + foundParams == groupParams.size();

        // For icon cell try to find variables on an example icon in schematic cell
        Variable[] exampleVars = null;
        if (cell.isIcon()) {
            for (Iterator<Cell> cit = cell.getCellGroup().getCells(); cit.hasNext();) {
                Cell schCell = cit.next();
                if (!schCell.isSchematic()) continue;
                exampleVars = findVarsOnExampleIcon(schCell, cell);
                if (exampleVars == null) continue;
                break;
            }
        }
        for (Variable.AttrKey omittedParam: omittedParams) {
            Variable param = groupParams.get(omittedParam);
            String msg = cell + " must have parameter \"" + param.getTrueName() + "\" as on " + cell.getCellGroup().getParameterOwner();
            Variable exampleParam = null;
            if (exampleVars != null) {
                for (Variable var: exampleVars) {
                    if (var == null || var.getKey() != omittedParam) continue;
                    exampleParam = var;
                    break;
                }
            }
            if (exampleParam != null) {
                TextDescriptor td = exampleParam.getTextDescriptor();
                td = td.withInherit(true).withParam(true).withUnit(param.getUnit());
                boolean interior = !exampleParam.isDisplay();
                td = td.withInterior(interior).withDisplay(true);
                param = param.withTextDescriptor(td);
            }
            assert param.getTextDescriptor().isParam() && param.isInherit();
            errorLogger.logError(msg, EPoint.fromLambda(param.getXOff(), param.getYOff()), cell, 2);
            cell.setTextDescriptor(param.getKey(), param.getTextDescriptor());
        }
    }

    abstract Variable[] findVarsOnExampleIcon(Cell parentCell, Cell iconCell);

	protected void scanNodesForRecursion(Cell cell, HashSet<Cell> markCellForNodes, NodeProto [] nil, int start, int end)
	{
		// scan the nodes in this cell and recurse
		for(int j=start; j<end; j++)
		{
			NodeProto np = nil[j];
			if (np instanceof PrimitiveNode) continue;
			Cell otherCell = (Cell)np;
			if (otherCell == null) continue;

			// subcell: make sure that cell is setup
			if (markCellForNodes.contains(otherCell)) continue;

			LibraryFiles reader = this;
			if (otherCell.getLibrary() != cell.getLibrary())
				reader = getReaderForLib(otherCell.getLibrary());

			// subcell: make sure that cell is setup
			if (reader != null)
				reader.realizeCellsRecursively(otherCell, markCellForNodes, null, 0);
		}
		markCellForNodes.add(cell);
	}

	/**
	 * Method to read project preferences from a Library.
	 * This method is never called.
	 * Instead, it is always overridden by the appropriate read subclass.
	 * @return true on error.
	 */
	protected boolean readProjectSettings() { return true; }

	/**
	 * Method to find the View to use for an old View name.
	 * @param viewName the old View name.
	 * @return the View to use (null if not found).
	 */
	protected View findOldViewName(String viewName)
	{
		if (version.getMajor() < 8)
		{
			if (viewName.equals("compensated")) return View.LAYOUTCOMP;
			if (viewName.equals("skeleton")) return View.LAYOUTSKEL;
			if (viewName.equals("simulation-snapshot")) return View.DOCWAVE;
			if (viewName.equals("netlist-netlisp-format")) return View.NETLISTNETLISP;
			if (viewName.equals("netlist-rsim-format")) return View.NETLISTRSIM;
			if (viewName.equals("netlist-silos-format")) return View.NETLISTSILOS;
			if (viewName.equals("netlist-quisc-format")) return View.NETLISTQUISC;
			if (viewName.equals("netlist-als-format")) return View.NETLISTALS;
		}
		return null;
	}

	protected Technology findTechnologyName(String name)
	{
		Technology tech = null;
		if (convertMosisCmosTechnologies)
		{
			if (name.equals("mocmossub")) tech = Technology.getMocmosTechnology(); else
				if (name.equals("mocmos")) tech = Technology.findTechnology("mocmosold");
		}
		if (tech == null) tech = Technology.findTechnology(name);
		if (tech == null && name.equals("logic"))
			tech = Schematics.tech();
		if (tech == null && (name.equals("epic8c") || name.equals("epic7c")))
			tech = Technology.findTechnology("epic7s");
		return tech;
	}

	/**
	 * Method to read an external library file, given its name as stored on disk.
	 * Attempts to find the file in many different ways, including asking the user.
	 * @param theFileName the full path to the file, as written to disk.
	 * @return a Library that was read. If library cannot be read or found, creates
	 * a Library called DUMMYname, and returns that.
	 */
	protected Library readExternalLibraryFromFilename(String theFileName, FileType defaultType,
                                                      IconParameters iconParams)
	{
		// get the path to the library file
		String legalLibName = TextUtils.getFileNameWithoutExtension(theFileName, true);
        // Checking if the library is already open
		Library elib = Library.findLibrary(legalLibName);
		if (elib != null) return elib;

//		URL url = TextUtils.makeURLToFile(theFileName);
//		String fileName = url.getFile();
//		File libFile = new File(fileName);

		// see if this library is already read in
		String libFileName = theFileName;
//		String libFileName = libFile.getName();

		// special case if the library path came from a different computer system and still has separators
		while (libFileName.endsWith("\\") || libFileName.endsWith(":") || libFileName.endsWith("/"))
			libFileName = libFileName.substring(0, libFileName.length()-1);
		int backSlashPos = libFileName.lastIndexOf('\\');
		int colonPos = libFileName.lastIndexOf(':');
		int slashPos = libFileName.lastIndexOf('/');
		int charPos = Math.max(backSlashPos, Math.max(colonPos, slashPos));
		if (charPos >= 0)
		{
			libFileName = libFileName.substring(charPos+1);
		}
		String libName = libFileName;
		FileType importType = OpenFile.getOpenFileType(libName, defaultType);
        FileType preferredType = importType;

        // this is just to remove the extension from the lib name string
		if (libName.endsWith(".elib"))
		{
			libName = libName.substring(0, libName.length()-5);
		} else if (libName.endsWith(".jelib"))
		{
			libName = libName.substring(0, libName.length()-6);
        } else if (libName.endsWith(".delib"))
        {
            libName = libName.substring(0, libName.length()-6);
		} else if (libName.endsWith(".txt"))
		{
			libName = libName.substring(0, libName.length()-4);
		} else
		{
			// no recognizable extension, add one to the file name
			libFileName += "." + defaultType.getFirstExtension();
		}

        StringBuffer errmsg = new StringBuffer();

        // first try the library name with the extension it came with
        // However, do not look in electric library area to avoid problems with spice configurations for old chips
        URL externalURL = getLibrary(libName + "." + preferredType.getFirstExtension(), theFileName, errmsg, true);
        // Now try all file types, starting with jelib
        // try JELIB
        if (externalURL == null && preferredType != FileType.JELIB) {
            externalURL = getLibrary(libName + "." + FileType.JELIB.getFirstExtension(), theFileName, errmsg, true);
        }
        // try ELIB
        if (externalURL == null && preferredType != FileType.ELIB) {
            externalURL = getLibrary(libName + "." + FileType.ELIB.getFirstExtension(), theFileName, errmsg, true);
        }
        // try DELIB
        if (externalURL == null && preferredType != FileType.DELIB) {
            externalURL = getLibrary(libName + "." + FileType.DELIB.getFirstExtension(), theFileName, errmsg, true);
        }
        // try txt
        if (externalURL == null && preferredType != FileType.READABLEDUMP) {
            externalURL = getLibrary(libName + "." + FileType.READABLEDUMP.getFirstExtension(), theFileName, errmsg, true);
        }

        boolean exists = (externalURL == null) ? false : true;
        // last option: let user pick library location
		if (!exists)
		{
			System.out.println("Error: cannot find referenced library " + libName+":");
			System.out.print(errmsg.toString());
			String pt = null;
			while (true) {
				// continue to ask the user where the library is until they hit "cancel"
				String description = "Reference library '" + libFileName + "'";
				pt = OpenFile.chooseInputFile(FileType.LIBFILE, description);
				if (pt == null) {
					// user cancelled, break
					break;
				}
				// see if user chose a file we can read
				externalURL = TextUtils.makeURLToFile(pt);
				if (externalURL != null) {
					exists = TextUtils.URLExists(externalURL, null);
					if (exists) {
						// good pt, opened it, get out of here
						break;
					}
				}
			}
		}

		if (exists)
		{
			System.out.println("Reading referenced library " + externalURL.getFile());
            importType = OpenFile.getOpenFileType(externalURL.getFile(), defaultType);
            elib = Library.newInstance(legalLibName, externalURL);
		}

        if (elib != null)
        {
            // read the external library
            String oldNote = getProgressNote();
            setProgressValue(0);
            setProgressNote("Reading referenced library " + legalLibName + "...");

			// get the library name
			String eLibName = TextUtils.getFileNameWithoutExtension(externalURL);
            elib = readALibrary(externalURL, elib, eLibName, importType, null, iconParams);
            setProgressValue(100);
            setProgressNote(oldNote);
        }

        if (elib == null)
        {
            System.out.println("Error: cannot find referenced library " + theFileName);
            System.out.println("...Creating new "+legalLibName+" Library instead");
            elib = Library.newInstance(legalLibName, null);
            elib.setLibFile(TextUtils.makeURLToFile(theFileName));
            elib.clearFromDisk();
        }
//		if (failed) elib->userbits |= UNWANTEDLIB; else
//		{
//			// queue this library for announcement through change control
//			io_queuereadlibraryannouncement(elib);
//		}

		return elib;
	}

    /** Get the URL to the library named libFileName
     * @param libFileName the library file name (with extension)
     * @param originalPath the original, exact path of the reference if any
     * @param errmsg a StringBuffer into which errors may be placed.
     * @param checkElectricLib to force search in Electric library area
     * @return null if not found, or valid URL if file found
     */
    private URL getLibrary(String libFileName, String originalPath, StringBuffer errmsg, boolean checkElectricLib)
    {
        // library does not exist: see if file is in the same directory as the main file
		URL firstURL = TextUtils.makeURLToFile(mainLibDirectory + libFileName);
        boolean exists = TextUtils.URLExists(firstURL, errmsg);
        if (exists) return firstURL;
        HashMap<String,String> searchedURLs = new HashMap<String,String>();

        // try secondary library file locations
        for (Iterator<String> libIt = LibDirs.getLibDirs(); libIt.hasNext(); )
        {
			URL url = TextUtils.makeURLToFile(libIt.next() + File.separator + libFileName);
            exists = TextUtils.URLExists(url, errmsg);
            if (exists) return url;
            if (url != null) searchedURLs.put(url.getFile(), url.getFile());
        }

        // check the current working dir
        // (Note that this is not necessarily the same as the mainLibDirectory above)
        // Do NOT search User.getCurrentWorkingDir, as another Electric process can
        // modify that during library read instead, search System.getProperty("user.dir");
        URL thirdURL = TextUtils.makeURLToFile(System.getProperty("user.dir") + File.separator + libFileName);
        if (thirdURL != null && !searchedURLs.containsKey(thirdURL.getFile()))
        {
            exists = TextUtils.URLExists(thirdURL, errmsg);
            if (exists) return thirdURL;
            if (thirdURL != null) searchedURLs.put(thirdURL.getFile(), thirdURL.getFile());
        }

        // try the exact path specified in the reference
        if (originalPath != null) {
    		URL url = TextUtils.makeURLToFile(originalPath);
        	String fileName = url.getFile();
            File libFile = new File(fileName);
            originalPath = libFile.getParent();

            URL secondURL = TextUtils.makeURLToFile(originalPath + File.separator + libFileName);
            if (secondURL != null && !searchedURLs.containsKey(secondURL.getFile()))
            {
                exists = TextUtils.URLExists(secondURL, errmsg);
                if (exists) return secondURL;
                if (secondURL != null) searchedURLs.put(secondURL.getFile(), secondURL.getFile());
            }
        }

		if (checkElectricLib)
        {
            // try the Electric library area
            URL url = LibFile.getLibFile(libFileName);
            exists = TextUtils.URLExists(url, errmsg);
            if (exists) return url;
        }
        return null;
    }

	public static void cleanupLibraryInput()
	{
        setProgressValue(0);
        setProgressNote("Constructing cell contents...");

		// Compute technology of new cells
		Set<Cell> uncomputedCells = new HashSet<Cell>();
		for(LibraryFiles reader : libsBeingRead)
		{
			for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
			{
				Cell cell = reader.nodeProtoList[cellIndex];
				if (cell == null) continue;
				if (cell.getLibrary() != reader.lib) continue;
				uncomputedCells.add(cell);
			}
		}
		for(LibraryFiles reader : libsBeingRead)
		{
			for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
			{
				Cell cell = reader.nodeProtoList[cellIndex];
				if (cell == null) continue;
				if (cell.getLibrary() != reader.lib) continue;
				reader.computeTech(cell, uncomputedCells);
			}
		}

		// clear flag bits for scanning the library hierarchically
		totalCells = 0;
		HashSet<Cell> markCellForNodes = new HashSet<Cell>();
		for(LibraryFiles reader : libsBeingRead)
		{
			totalCells += reader.nodeProtoCount;
			for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
			{
				Cell cell = reader.nodeProtoList[cellIndex];
				if (cell == null) continue;
				if (cell.getLibrary() != reader.lib) continue;
				reader.cellLambda[cellIndex] = reader.computeLambda(cell, cellIndex);
				cell.setTempInt(cellIndex);
			}
		}
		cellsConstructed = 0;

		// now recursively adjust lambda sizes
		if (LibraryFiles.VERBOSE)
			System.out.println("Preparing to compute scale factors");
		for(int i=0; i<20; i++)
		{
			boolean unchanged = true;
			for(LibraryFiles reader : libsBeingRead)
			{
				for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
				{
					Cell cell = reader.nodeProtoList[cellIndex];
					if (cell == null) continue;
					if (cell.getLibrary() != reader.lib) continue;
					if (reader.spreadLambda(cell, cellIndex))
					{
						unchanged = false;
					}
				}
			}
			if (unchanged) break;
		}
		if (LibraryFiles.VERBOSE)
			System.out.println("Finished computing scale factors");

		// recursively create the cell contents
		for(LibraryFiles reader : libsBeingRead)
		{
			for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
			{
				Cell cell = reader.nodeProtoList[cellIndex];
				if (cell == null) continue;
				if (markCellForNodes.contains(cell)) continue;
				reader.realizeCellsRecursively(cell, markCellForNodes, null, 0);
			}
		}
        for (LibraryFiles reader: libsBeingRead)
            reader.lib.clearChanged();

		// tell which libraries had extra "scaled" cells added
		boolean first = true;
		for(LibraryFiles reader : libsBeingRead)
		{
			if (reader.scaledCells != null && reader.scaledCells.size() != 0)
			{
				if (first)
				{
					System.out.println("WARNING: to accommodate scaling inconsistencies, these cells were created:");
					first = false;
				}
				StringBuffer sb = new StringBuffer();
				sb.append("   Library " + reader.lib.getName() + ":");
				for(Cell cell : reader.scaledCells)
				{
					sb.append(" " + cell.noLibDescribe());
				}
				System.out.println(sb.toString());
			}
		}

		// adjust for old library conversion
//		convertOldLibraries();

		// broadcast the library-read to all listeners
		for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
		{
			Listener listener = it.next();
			for(LibraryFiles reader : libsBeingRead)
			{
				listener.readLibrary(reader.lib);
			}
		}
        if (!undefinedTechsAndPrimitives.isEmpty()) {
            String prims = "";
            for (TechId techId: undefinedTechsAndPrimitives.keySet())
            {
                int count = 0;
                for (PrimitiveNodeId primId : undefinedTechsAndPrimitives.get(techId))
                {
                    prims += " " + primId + ", ";
                    count += primId.name.length();
                    if (count > 50) // 50 is an arbritary number
                    {
                        count = 0;
                        prims += "\n";
                    }
                }
                Job.getUserInterface().showErrorMessage("Library contains unknown nodes from the technology '" + techId + "':\n" + prims,
                    "Unknown nodes/technologies");
            }
        }

        // clean up init (free LibraryFiles for garbage collection)
        libsBeingRead.clear();
        undefinedTechsAndPrimitives.clear();
	}

// 	private static void convertOldLibraries()
// 	{
// 		// see if the MOSIS CMOS technology now has old-style state information
// 		MoCMOS.tech.convertOldState();
// 	}

	protected LibraryFiles getReaderForLib(Library lib) {
        for (LibraryFiles reader : libsBeingRead) {
            if (reader.lib == lib) return reader;
        }
        return null;
    }

    Technology.SizeCorrector getSizeCorrector(Technology tech) {
        Technology.SizeCorrector corrector = sizeCorrectors.get(tech);
        if (corrector == null) {
            corrector = tech.getSizeCorrector(version, projectSettings, false, false);
            sizeCorrectors.put(tech, corrector);
        }
        return corrector;
    }

	String convertCellName(String s)
	{
		StringBuffer buf = null;
		for (int i = 0; i < s.length(); i++)
		{
			char ch = s.charAt(i);
			if (ch == '\n' || ch == '|' || ch == ':')
			{
				if (buf == null)
				{
					buf = new StringBuffer();
					buf.append(s.substring(0, i));
				}
				buf.append('-');
				continue;
			} else if (buf != null)
			{
				buf.append(ch);
			}
		}
		if (buf != null)
		{
			String newS = buf.toString();
			System.out.println("Cell name " + s + " was converted to " + newS);
			return newS;
		}
		return s;
	}

	/**
	 * Method to conver name of Geometric object.
	 * @param value name of object
	 * @param isDisplay true if this is displayable variable.
	 */
	protected static String convertGeomName(Object value, boolean isDisplay)
	{
		if (value == null || !(value instanceof String)) return null;
		String str = (String)value;
		int indexOfAt = str.indexOf('@');
		if (isDisplay)
		{
			if (indexOfAt >= 0)
			{
				String newS = "";
				for (int i = 0; i < str.length(); i++)
				{
					char c = str.charAt(i);
					if (c == '@') c = '_';
					newS += c;
				}
				str = newS;
			}
		} else if (indexOfAt < 0) return null;
		return str;
	}

	/**
	 * Method to build a NodeInst.
     * @param nil arrays with data of new NodeInst
     * @param nodeIndex index in nik array
     * @param xoff x-offset of NodeInst in integer coordinates
     * @param yoff y-offset of NodeInst in integer coordinates
     * @param lambda scale factor
     * @param parent parent Cell
     * @param proto actual node proto (may be different for instance of scaled Cells)
	 */
    void realizeNode(NodeInstList nil, int nodeIndex, int xoff, int yoff, double lambda, Cell parent, NodeProto proto)
    {
		double lowX = nil.lowX[nodeIndex]-xoff;
		double lowY = nil.lowY[nodeIndex]-yoff;
		double highX = nil.highX[nodeIndex]-xoff;
		double highY = nil.highY[nodeIndex]-yoff;
		Point2D center = new Point2D.Double(((lowX + highX) / 2) / lambda, ((lowY + highY) / 2) / lambda);
        EPoint size = EPoint.ORIGIN;
        if (proto instanceof PrimitiveNode) {
            PrimitiveNode pn = (PrimitiveNode)proto;
            size = getSizeCorrector(pn.getTechnology()).getSizeFromDisk(pn, (highX - lowX) / lambda, (highY - lowY) / lambda);
        }

		int rotation = nil.rotation[nodeIndex];
        boolean flipX = false;
        boolean flipY = false;
		if (rotationMirrorBits)
		{
			// new version: allow mirror bits
			if ((nil.transpose[nodeIndex]&1) != 0)
			{
                flipY = true;
				rotation = (rotation + 900) % 3600;
			}
			if ((nil.transpose[nodeIndex]&2) != 0)
			{
				// mirror in X
                flipX = true;
			}
			if ((nil.transpose[nodeIndex]&4) != 0)
			{
				// mirror in Y
                flipY = !flipY;
			}
		} else
		{
			// old version: just use transpose information
			if (nil.transpose[nodeIndex] != 0)
			{
                flipY = true;
				rotation = (rotation + 900) % 3600;
			}
		}
        Orientation orient = Orientation.fromJava(rotation, flipX, flipY);

		// figure out the grab center if this is a cell instance
		if (proto instanceof Cell)
		{
            if (nil.anchorX != null)
//            if (magic <= ELIBConstants.MAGIC13)
            {
                // version 13 and later stores and uses anchor location
                double anchorX = (nil.anchorX[nodeIndex]-xoff) / lambda;
                double anchorY = (nil.anchorY[nodeIndex]-yoff) / lambda;
                center.setLocation(anchorX, anchorY);
            } else
            {
                Cell subCell = (Cell)proto;
                Rectangle2D bounds = subCell.getBounds();
                Point2D shift = new Point2D.Double(-bounds.getCenterX(), -bounds.getCenterY());
                AffineTransform trans = orient.pureRotate();
                trans.transform(shift, shift);
                center.setLocation(center.getX() + shift.getX(), center.getY() + shift.getY());
            }
		}

        int flags = ImmutableNodeInst.flagsFromElib(nil.userBits[nodeIndex]);
        int techBits = ImmutableNodeInst.techSpecificFromElib(nil.userBits[nodeIndex]);
        Integer cutAlignment = null;
        if (proto instanceof PrimitiveNode && ((PrimitiveNode)proto).isMulticut() && techBits != 0) {
            cutAlignment = Integer.valueOf(techBits);
            techBits = 0;
        }
		NodeInst ni = NodeInst.newInstance(parent, proto, nil.name[nodeIndex], nil.nameTextDescriptor[nodeIndex],
                center, size, orient, flags, techBits, nil.protoTextDescriptor[nodeIndex], Input.errorLogger);
        nil.theNode[nodeIndex] = ni;
//        if (proto instanceof PrimitiveNode) {
//            PrimitiveNode pn = (PrimitiveNode)proto;
//            PrimitiveNode.NodeSizeRule nodeSizeRule = pn.getMinSizeRule();
//            if (nodeSizeRule != null) {
//                if (size.getLambdaX() < nodeSizeRule.getWidth())
//                    Input.errorLogger.logWarning(" (" + parent + ") node " + ni.getName() + " width is less than minimum by " +
//                            (nodeSizeRule.getWidth() - size.getLambdaX()), ni, parent, null, 2);
//                if (size.getLambdaY() < nodeSizeRule.getHeight())
//                    Input.errorLogger.logWarning(" (" + parent + ") node " + ni.getName() + " height is less than minimum by " +
//                            (nodeSizeRule.getHeight() - size.getLambdaY()), ni, parent, null, 2);
//            }
//        }
        if (ni == null) return;
        Variable[] vars = nil.vars[nodeIndex];
        if (vars != null) {
            for (int j = 0; j < vars.length; j++) {
                Variable var = vars[j];
                if (var == null) continue;
	            // convert outline information
				if (var.getKey() == NodeInst.TRACE && proto instanceof PrimitiveNode && ((PrimitiveNode)proto).isHoldsOutline() ) {
                    Object value = var.getObject();
                    if (value instanceof Integer[] || value instanceof Float[]) {
                        // convert outline information, if present
                        Number[] outline = (Number[])value;
                        int newLength = outline.length / 2;
                        EPoint [] newOutline = new EPoint[newLength];
                        double lam = outline instanceof Integer[] ? lambda : 1.0;
                        for(int k=0; k<newLength; k++) {
                            double oldX = outline[k*2].doubleValue()/lam;
                            double oldY = outline[k*2+1].doubleValue()/lam;
                            if (!Double.isNaN(oldX) && !Double.isNaN(oldY))
                                newOutline[k] = new EPoint(oldX, oldY);
                        }
                        var = var.withObject(newOutline);
                    }
                }
                if (ni.isDeprecatedVariable(var.getKey())) continue;
                if (ni.isParam(var.getKey()))
                    ni.addParameter(var);
                else
                    ni.addVar(var.withParam(false));
            }
        }
        if (cutAlignment != null)
            ni.newVar(Technology.NodeLayer.CUT_ALIGNMENT, cutAlignment);

        // if this was a dummy cell, log instance as an error so the user can find easily
        if (proto instanceof Cell && ((Cell)proto).getVar(IO_DUMMY_OBJECT) != null) {
            Input.errorLogger.logError("Instance of dummy cell "+proto.getName(), ni, parent, null, 1);
        }
    }

    void realizeVariables(ElectricObject eObj, Variable[] vars) {
        if (vars == null) return;
        for (Variable var: vars)
            realizeVariable(eObj, var);
    }

    private void realizeVariable(ElectricObject eObj, Variable var) {
        if (var == null || eObj.isDeprecatedVariable(var.getKey())) return;
        if (eObj.isParam(var.getKey()) && eObj instanceof NodeInst) {
            ((NodeInst)eObj).addParameter(var);
            return;
        }

        var = var.withParam(false);
        String origVarName = var.getKey().toString();
        // convert old port variables
        if (eObj instanceof NodeInst && var.getKey().getName().startsWith("ATTRP_")) {
            NodeInst ni = (NodeInst)eObj;
            // the form is "ATTRP_portName_variableName" with "\" escapes
            StringBuffer portName = new StringBuffer();
            String varName = null;
            int len = origVarName.length();
            for(int j=6; j<len; j++) {
                char ch = origVarName.charAt(j);
                if (ch == '\\') {
                    j++;
                    portName.append(origVarName.charAt(j));
                    continue;
                }
                if (ch == '_') {
                    varName = origVarName.substring(j+1);
                    break;
                }
                portName.append(ch);
            }
            if (varName != null) {
                String thePortName = portName.toString();
                PortProto pp = findPortProto(ni.getProto(), thePortName);
                PortInst pi = pp != null ? ni.findPortInstFromProto(pp) : null;
                if (pi != null) {
                    pi.newVar(Variable.newKey(varName), var.getObject(), var.getTextDescriptor());
                    return;
                }
            }
        }
        eObj.addVar(var);
    }

    static PortProto findPortProto(NodeProto np, String portId) {
        PortProtoId portProtoId = np.getId().newPortId(portId);
        PortProto pp = np.getPort(portProtoId);
        if (pp != null) return pp;
        if (np.getNumPorts() == 1 && portId.length() == 0)
            return np.getPort(0);
        if (np instanceof PrimitiveNode) {
            PrimitiveNode primNode = (PrimitiveNode)np;
            pp = primNode.findPortProto(portId);
            if (pp == null)
                pp = primNode.getTechnology().convertOldPortName(portId, primNode);
        }
        return pp;
    }

	/**
	 * Method to add meaning preferences to an ElectricObject from a List of strings.
	 * @param obj the Object to augment with meaning preferences.
	 * @param vars Variables with meaning preferences.
	 */
    void realizeMeaningPrefs(Object obj, Variable[] vars) {
        realizeMeaningPrefs(projectSettings, obj, vars);
	}

	/**
	 * Method to add meaning preferences to an ElectricObject from a List of strings.
     * @param projectSettings a map for result
	 * @param obj the Object to augment with meaning preferences.
	 * @param vars Variables with meaning preferences.
	 */
    static void realizeMeaningPrefs(HashMap<Setting,Object> projectSettings, Object obj, Variable[] vars) {
        HashMap<String,Object> settingsByPrefName = new HashMap<String,Object>();
        for (int i = 0; i < vars.length; i++) {
            Variable var = vars[i];
            if (var == null) continue;
            Object value = var.getObject();
            if (!(value instanceof String)) {
                if (value instanceof Short || value instanceof Byte)
                    value = new Integer(((Number)value).intValue());
                if (!(value instanceof Number) && !(value instanceof Boolean))
                    continue;
            }
            String prefName = var.getKey().getName();
            String prefPath = null;
            if (obj instanceof Technology) {
                prefPath = Technology.TECH_NODE + "/";
                Map<Setting,Object> convertedVars = ((Technology)obj).convertOldVariable(prefName, value);
                if (convertedVars != null) {
                    for (Map.Entry<Setting,Object> e: convertedVars.entrySet()) {
                        Setting setting = e.getKey();
                        prefName = setting.getPrefName();
                        value = e.getValue();
                        projectSettings.put(setting, value);
                    }
                    continue;
                }
            } else if (obj instanceof Tool) {
                prefPath = ((Tool)obj).prefs.relativePath() + "/";
            }
            settingsByPrefName.put(prefPath + prefName, value);
        }
        Setting.Group grp = obj instanceof Technology ? ((Technology)obj).getProjectSettings() : ((Tool)obj).getProjectSettings();
        for (Setting setting: grp.getSettings()) {
            Object value = settingsByPrefName.get(setting.getPrefPath());
            if (value != null)
                projectSettings.put(setting, value);
        }
	}

    TextDescriptor makeDescriptor(int td0, int td1) {
        mtd.setCBits(td0, fixTextDescriptorFont(td1));
        return TextDescriptor.newTextDescriptor(mtd);
    }

    TextDescriptor makeDescriptor(int td0, int td1, int flags) {
        mtd.setCBits(td0, fixTextDescriptorFont(td1), flags);
        return TextDescriptor.newTextDescriptor(mtd);
    }

	/**
	 * Method to grab font associations that were stored on a Library.
	 * The font associations are used later to convert indices to true font names and numbers.
	 * @param associationArray array from FONT_ASSOCIATIONS variable.
	 */
	void setFontNames(String[] associationArray)
	{
		int maxAssociation = 0;
		for(int i=0; i<associationArray.length; i++)
		{
            if (associationArray[i] == null) continue;
			int fontNumber = TextUtils.atoi(associationArray[i]);
			if (fontNumber > maxAssociation) maxAssociation = fontNumber;
		}
		if (maxAssociation <= 0) return;

		fontNames = new String[maxAssociation];
		for(int i=0; i<maxAssociation; i++) fontNames[i] = null;
		for(int i=0; i<associationArray.length; i++)
		{
            if (associationArray[i] == null) continue;
			int fontNumber = TextUtils.atoi(associationArray[i]);
			if (fontNumber <= 0) continue;
			int slashPos = associationArray[i].indexOf('/');
			if (slashPos < 0) continue;
			fontNames[fontNumber-1] = associationArray[i].substring(slashPos+1);
		}
	}

	/**
	 * Method to convert the font number in a TextDescriptor to the proper value as
	 * cached in the Library.  The caching is examined by "getFontAssociationVariable()".
	 * @param descriptor1 value of descriptor1 from disk.
	 * @return patched value of descriptor1.
	 */
	private int fixTextDescriptorFont(int descriptor1)
	{
		int fontNumber = (descriptor1 & ELIBConstants.VTFACE) >> ELIBConstants.VTFACESH;
        if (fontNumber == 0) return descriptor1;
        descriptor1 &= ~ELIBConstants.VTFACE;
		if (fontNames != null && fontNumber <= fontNames.length)
        {
			String fontName = fontNames[fontNumber-1];
			TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(fontName);
            if (af != null) {
                fontNumber = af.getIndex();
                if (fontNumber <= (ELIBConstants.VTFACE >> ELIBConstants.VTFACESH))
                    descriptor1 |= fontNumber << ELIBConstants.VTFACESH;
            }
		}
        return descriptor1;
	}

	/**
	 * Set line number for following errors and warnings.
	 * @param lineNumber line numnber for following erros and warnings.
	 */
	void setLineNumber(int lineNumber) {}

	/**
	 * Issue error message.
	 * @param message message string.
	 * @return MessageLog object for attaching further geometry details.
	 */
	void logError(String message)
	{
		errorCount++;
		System.out.println(message);
		errorLogger.logError(message, -1);
	}

	/**
	 * Issue warning message.
	 * @param message message string.
	 * @return MessageLog object for attaching further geometry details.
	 */
	void logWarning(String message)
	{
		System.out.println(message);
		errorLogger.logWarning(message, null, -1);
	}

	// *************************** THE CELL CLEANUP INTERFACE ***************************

	protected void computeTech(Cell cell, Set uncomputedCells)
	{
		uncomputedCells.remove(cell);
	}

	protected double computeLambda(Cell cell, int cellIndex) { return 1; }

	protected boolean spreadLambda(Cell cell, int cellIndex) { return false; }

	protected boolean canScale() { return false; }

	/**
	 * Method to recursively create the contents of each cell in the library.
	 */
	abstract void realizeCellsRecursively(Cell cell, HashSet<Cell> recursiveSetupFlag, String scaledCellName, double scale);
}

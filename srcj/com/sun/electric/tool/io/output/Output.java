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
package com.sun.electric.tool.io.output;

import com.sun.electric.Main;
import com.sun.electric.database.IdMapper;
import com.sun.electric.database.LibraryBackup;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.output.bookshelf.BookshelfOutput;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.IconParameters;
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
import java.io.StringWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.SwingUtilities;

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
     * @param fileP the path to the disk file to be written.
     * @param type the format of the output file.
     * @param override a list of Polys to draw instead of the cell contents.
    */
    public static void exportCellCommand(Cell cell, VarContext context, String fileP, FileType type, List<PolyBase> override)
    {
    	new OutputCellInfo(cell, context, fileP, type, override);
    }

    /**
     * Return OutputPreferences for a specified FileType.
     * This includes currnet default values of IO ProjectSettings
     * and either factory default or current default values of Prefs
     * Current default value of Prefs can be obtained only from client thread
     * @param type specified file type.
     * @param factory get factory default values of Prefs
     * @param override the list of Polys to write instead of cell contents.
     * @return an OutputPreferences object for the given file type.
     * @throws InvalidStateException on attemt to get current default values of Prefs from server thread
     */
    public static OutputPreferences getOutputPreferences(FileType type, Cell cell, boolean factory, List<PolyBase> override)
    {
        if (!factory && !SwingUtilities.isEventDispatchThread() && !Main.isBatch())
                throw new IllegalStateException("Current default Prefs can be accessed only from client thread");

		if (type == FileType.CDL) return new Spice.SpicePreferences(factory, true);
		if (type == FileType.CIF) return new CIF.CIFPreferences(factory);
		if (type == FileType.COSMOS || type == FileType.ESIM || type == FileType.RSIM) return new Sim.SimPreferences(factory, type);
		if (type == FileType.DXF) return new DXF.DXFPreferences(factory);
		if (type == FileType.EAGLE) return new Eagle.EaglePreferences(factory);
		if (type == FileType.ECAD) return new ECAD.ECADPreferences(factory);
		if (type == FileType.EDIF) return new EDIF.EDIFPreferences(factory);
		if (type == FileType.FASTHENRY) return new FastHenry.FastHenryPreferences(factory);
		if (type == FileType.HPGL) return new HPGL.HPGLPreferences(factory);
		if (type == FileType.GDS) return new GDS.GDSPreferences(factory);
		if (type == FileType.IRSIM) return new IRSIM.IRSIMPreferences(factory);
		if (type == FileType.L) return new L.LPreferences(factory);
		if (type == FileType.LEF) return new LEF.LEFPreferences(factory);
		if (type == FileType.MAXWELL) return new Maxwell.MaxwellPreferences(factory);
		if (type == FileType.MOSSIM) return new MOSSIM.MOSSIMPreferences(factory);
		if (type == FileType.PADS) return new Pads.PadsPreferences(factory);
		if (type == FileType.PAL) return new PAL.PALPreferences(factory);
		if (type == FileType.POSTSCRIPT || type == FileType.EPS) return new PostScript.PostScriptPreferences(factory, override);
		if (type == FileType.SILOS) return new Silos.SilosPreferences(factory);
		if (type == FileType.SKILL) return new IOTool.SkillPreferences(factory, false, cell);
        if (type == FileType.SKILLEXPORTSONLY) return new IOTool.SkillPreferences(factory, true, cell);
		if (type == FileType.SPICE) return new Spice.SpicePreferences(factory, false);
		if (type == FileType.SVG) return new SVG.SVGPreferences(factory);
		if (type == FileType.TEGAS) return new Tegas.TegasPreferences(factory);
		if (type == FileType.VERILOG) return new Verilog.VerilogPreferences(factory, false);
		if (type == FileType.VERILOGA) return new Verilog.VerilogPreferences(factory, true);
		if (type == FileType.BOOKSHELF) return new BookshelfOutput.BookshelfOutputPreferences(factory);
        return null;
    }

    /**
     * Class to define cell information during output.
     */
    private static class OutputCellInfo extends Job
    {
        private Cell cell;
        private VarContext context;
        private String filePath;
        private OutputPreferences prefs;

        /**
         * @param cell the Cell to be written.
         * @param context the VarContext of the Cell (its position in the hierarchy above it).
         * @param filePath the path to the disk file to be written.
         * @param override the list of Polys to write instead of cell contents.
         */
        public OutputCellInfo(Cell cell, VarContext context, String filePath, FileType type, List<PolyBase> override)
        {
            super("Export "+cell+" ("+type+")", IOTool.getIOTool(), Job.Type.SERVER_EXAMINE, null, null, Priority.USER);
            this.cell = cell;
            this.context = context;
            this.filePath = filePath;
            prefs = getOutputPreferences(type, cell, false, override);
            if (prefs != null)
            	startJob();
        }

        public boolean doIt() throws JobException
        {
        	prefs.doOutput(cell, context, filePath);
            return true;
        }
    }

    public abstract static class OutputPreferences implements java.io.Serializable
	{
        // IO Settings
        public boolean useCopyrightMessage = IOTool.isUseCopyrightMessage();
        public boolean includeDateAndVersionInOutput = User.isIncludeDateAndVersionInOutput();
        public IconParameters iconParameters = IconParameters.makeInstance(true);

        protected OutputPreferences() { this(false); }
        protected OutputPreferences(boolean factory) {
            if (!factory && !SwingUtilities.isEventDispatchThread() && !Main.isBatch())
                throw new IllegalStateException("Current default Prefs can be accessed only from client thread");
        }

        public abstract Output doOutput(Cell cell, VarContext context, String filePath);
	}

	/** file path */									protected String filePath;
	/** for writing text files */						protected PrintWriter printWriter;
	/** for writing text arrays */						protected StringWriter stringWriter;
	/** for writing binary files */						protected DataOutputStream dataOutputStream;
	/** True to write with less information displayed */protected boolean quiet;
	/** for storing generated errors */					protected ErrorLogger errorLogger;

	public Output()
	{
        errorLogger = ErrorLogger.newInstance(this.getClass().getName() + " Output");
    }

    /**
     * Method to retrieve number of errors during the writing process
     */
    public int getNumErrors() { return errorLogger.getNumErrors(); }

    /**
     * Method to retrieve number of warnings during the writting process
     */
    public int getNumWarnings() { return errorLogger.getNumWarnings(); }

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
            File newLibFile;
            if (libURL == null || libURL.getPath() == null) {
                newLibFile = new File(panicDir.getAbsolutePath(), libName + "." + type.getFirstExtension());
            } else {
                File libFile = new File(libURL.getPath());
                String fileName = libFile.getName();
                if (fileName == null) fileName = libName + "." + type.getFirstExtension();
                newLibFile = new File(panicDir.getAbsolutePath(), fileName);
            }
            URL newLibURL = TextUtils.makeURLToFile(newLibFile.getAbsolutePath());
            libFiles.put(libId, newLibURL);
            sortedLibs.put(libName, libId);
        }
        boolean error = false;
        if (oldRevision) {
            IdMapper idMapper = IdMapper.consolidateExportIds(panicSnapshot);
            panicSnapshot = panicSnapshot.withRenamedIds(idMapper, null, null);
        }
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
        }
        boolean error = false;
        String properOutputName = TextUtils.getFilePath(libURL) + TextUtils.getFileNameWithoutExtension(libURL) + ".jelib";
        JELIB jelib = new JELIB();
        if (jelib.openTextOutputStream(properOutputName) ||
                jelib.writeLib(lib.getDatabase().backupUnsafe(), lib.getId(), libFiles, false) ||
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
	 * @param thisQuiet true to save with less information displayed.
     * @param delibHeaderOnly true to write only the header for a DELIB type library
     * @param backupScheme controls how older files are backed-up.
	 */
	public static void writeLibrary(Library lib, FileType type, boolean compatibleWith6,
		boolean thisQuiet, boolean delibHeaderOnly, int backupScheme) throws JobException {
        writeLibrary(lib, type, compatibleWith6, thisQuiet, delibHeaderOnly, backupScheme, null, null);
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
     * @param thisQuiet true to save with less information displayed.
     * @param delibHeaderOnly true to write only the header for a DELIB type library
     * @param backupScheme controls how older files are backed-up.
     * @param deletedCellFiles output list of deleted cell files of DELIB library
     * @param writtenCellFiles output list of written cell files of DELIB library
     */
    public static void writeLibrary(Library lib, FileType type, boolean compatibleWith6,
        boolean thisQuiet, boolean delibHeaderOnly, int backupScheme,
        List<String> deletedCellFiles, List<String> writtenCellFiles) throws JobException
    {
        // make sure that all "meaning" options are attached to the database
//		Pref.installMeaningVariables();

        // make sure that this library save is announced
        for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); )
        {
            Listener listener = it.next();
            listener.writeLibrary(lib);
        }
        Snapshot snapshot = lib.getDatabase().backup();
        LibId libId = lib.getId();

//		// make sure all technologies with irrelevant scale information have the same scale value
//		double largestScale = 0;
//		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
//		{
//			Technology tech = it.next();
//			if (tech.isScaleRelevant()) continue;
//			if (tech == Generic.tech) continue;
//			if (tech.getScale() > largestScale) largestScale = tech.getScale();
//		}
//        Setting.SettingChangeBatch changeBatch = new Setting.SettingChangeBatch();
//		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
//		{
//			Technology tech = it.next();
//			if (tech.isScaleRelevant()) continue;
//			if (tech == Generic.tech) continue;
//            changeBatch.add(tech.getScaleSetting(), Double.valueOf(largestScale));
////			tech.setScale(largestScale);
//		}
//        Setting.implementSettingChanges(changeBatch);

        // handle different file types
        URL libFile = lib.getLibFile();
        if (libFile == null)
            libFile = TextUtils.makeURLToFile(lib.getName());

        // make the proper output file name
        File f = new File(libFile.getPath());
        String fullOutputName = f.getAbsolutePath();
        String properOutputNameWithoutExtension = TextUtils.getFilePath(libFile) + TextUtils.getFileNameWithoutExtension(libFile);
        String properOutputName = fullOutputName;
        if (properOutputNameWithoutExtension.equals(fullOutputName))
        {
            if (type == FileType.ELIB) properOutputName = properOutputNameWithoutExtension + ".elib";
            if (type == FileType.JELIB) properOutputName = properOutputNameWithoutExtension + ".jelib";
            if (type == FileType.DELIB) properOutputName = properOutputNameWithoutExtension + ".delib";
            if (type == FileType.READABLEDUMP) properOutputName = properOutputNameWithoutExtension + ".txt";
        }

//		String properOutputNameWithoutExtension = TextUtils.getFilePath(libFile) + TextUtils.getFileNameWithoutExtension(libFile);
//		String properOutputName = properOutputNameWithoutExtension;
//		if (type == FileType.ELIB) properOutputName += ".elib";
//		if (type == FileType.JELIB) properOutputName += ".jelib";
//		if (type == FileType.DELIB) properOutputName += ".delib";
//		if (type == FileType.READABLEDUMP) properOutputName += ".txt";
        if (type == FileType.ELIB || type == FileType.JELIB || type == FileType.DELIB)
        {
            // backup previous files if requested
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
                        backupFileName += "." + type.getFirstExtension();
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
        }
        if (type == FileType.ELIB || type == FileType.JELIB)
        {
            if (type == FileType.ELIB)
            {
                ELIB elib = new ELIB();
                elib.quiet = thisQuiet;
                if (compatibleWith6) elib.write6Compatible();
                if (elib.openBinaryOutputStream(properOutputName))
                    throw new JobException("elib.openBinaryOutputStream() failed");
//                if (CVS.isEnabled()) {
//                    CVSLibrary.savingLibrary(lib);
//                }
                if (elib.writeLib(snapshot, libId))
                    throw new JobException("elib.writeLib() failed");
                if (elib.closeBinaryOutputStream())
                    throw new JobException("elib.closeBinaryOutputStream() failed");
//                if (CVS.isEnabled()) {
//                    CVSLibrary.savedLibrary(lib);
//                }
            } else
            {
                JELIB jelib = new JELIB();
                jelib.quiet = thisQuiet;
                if (jelib.openTextOutputStream(properOutputName))
                    throw new JobException("jelib.openTextOutputStream() failed");
//                if (CVS.isEnabled()) {
//                    CVSLibrary.savingLibrary(lib);
//                }
                if (jelib.writeLib(snapshot, libId, null, false))
                    throw new JobException("jelib.writeLib() failed");
                if (jelib.closeTextOutputStream())
                    throw new JobException("jelib.closeTextOutputStream() failed");
//                if (CVS.isEnabled()) {
//                    CVSLibrary.savedLibrary(lib);
//                }
            }
        } else if (type == FileType.READABLEDUMP)
        {
            ReadableDump readableDump = new ReadableDump();
            readableDump.quiet = thisQuiet;
            if (readableDump.openTextOutputStream(properOutputName))
                throw new JobException("readableDump.openTextOutputStream() failed");
            if (readableDump.writeLib(snapshot, libId))
                throw new JobException("readableDump.writeLib() failed");
            if (readableDump.closeTextOutputStream())
                throw new JobException("readableDump.closeTextOutputStream() failed");
        } else if (type == FileType.DELIB)
        {
            DELIB delib = new DELIB(delibHeaderOnly);
            delib.quiet = thisQuiet;
            if (delib.openTextOutputStream(properOutputName))
                throw new JobException("delib.openTextOutputStream() failed");
//            if (CVS.isEnabled() && !delibHeaderOnly) {
//                CVSLibrary.savingLibrary(lib);
//            }
            Set<CellId> oldCells = lib.getDelibCells();
            if (delib.writeLib(snapshot, libId, oldCells))
                throw new JobException("delib.writeLib() failed");
            lib.setDelibCells();
//            HashSet<String> cellFiles = new HashSet<String>(lib.getDelibCellFiles());
//            if (delib.writeLib(snapshot, libId, cellFiles)) return true;
//            lib.setDelibCellFiles(cellFiles);
            if (delib.closeTextOutputStream())
                throw new JobException("delib.closeTextOutputStream() failed");
            if (deletedCellFiles != null)
                deletedCellFiles.addAll(delib.getDeletedCellFiles());
            if (writtenCellFiles != null)
                writtenCellFiles.addAll(delib.getWrittenCellFiles());
//            if (CVS.isEnabled() && !delibHeaderOnly) {
//                CVSLibrary.savedLibrary(lib, delib.getDeletedCellFiles(), delib.getWrittenCellFiles());
//            }
        } else
        {
            throw new JobException("Unknown export type: " + type);
        }
		// clean up and return
        lib.setFromDisk();
        if (!thisQuiet) System.out.println(properOutputName + " written");
        // Update the version in library read in memory
        lib.setVersion(Version.getVersion());
        // if using CVS, update state
/*
        if (CVS.isEnabled()) {
            CVSLibrary.updateState(lib);
        }
*/
        lib.clearChanged();
        Constraints.getCurrent().writeLibrary(lib);
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
     * Opens output for writing binary files.
     * @param filePath the name of the file.
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
     * Close output for writing binary to a file.
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
     * Open output for writing text to a file.
     * @param fileP the name of the file.
     * @return true on error.
     */
    protected boolean openTextOutputStream(String fileP)
    {
		this.filePath = fileP;
        try
		{
            //printWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileP)));
            // Extra step with URL is needed for MacOSX paths.
            URL fileURL = TextUtils.makeURLToFile(fileP);
            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(TextUtils.getFile(fileURL))));
        } catch (IOException e)
		{
            reportError("Error opening " + fileP+": "+e.getMessage());
            return true;
        }
        return false;
    }

	/**
     * Close output for writing text to a file.
     * @return true on error.
     */
    protected boolean closeTextOutputStream()
    {
        printWriter.close();
        return false;
    }

    /**
     * Open output for collecting a list of strings.
     */
    protected void openStringsOutputStream()
    {
        stringWriter = new StringWriter();
    }

	/**
     * Close output for collecting a list of strings.
     * @return the list of strings.
     */
    protected List<String> closeStringsOutputStream()
    {
    	StringBuffer sb = stringWriter.getBuffer();
    	String [] lines = sb.toString().split("\n");
    	List<String> strings = new ArrayList<String>();
    	for(int i=0; i<lines.length; i++)
    		strings.add(lines[i]);
        return strings;
    }

    /**
     * Method to terminate the logging process in the ErrorLogger
     */
    protected Output finishWrite()
    {
        errorLogger.termLogging(true);
        return this;
    }

    /**
     * Method to report errors during the output process
     * @param msg
     */
    protected void reportWarning(String msg)
    {
        errorLogger.logWarning(msg, null, -1);
        System.out.println(msg);
    }

    /**
     * Method to report errors during the output process. It must be public
     * because it is used in parent package
     * @param msg
     */
    public void reportError(String msg)
    {
        errorLogger.logError(msg, -1);
        System.out.println(msg);
    }

    /**
     * Method to write copyright header information to the output.
     * @param prefix the characters that start a line of commented output.
     * @param postfix the characters that end a line of commented output.
     */
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
			writeChunk(prefix + oneLine + postfix + "\n");
			start = endPos+1;
		}
	}

	/** number of characters written on a line */	private int lineCharCount = 0;
	private int maxWidth = 80;
	private boolean strictWidthLimit = false;
	private String continuationString = "";

	/**
	 * Method to set the size of a line of output.
	 * @param width the maximum number of characters on a line of output (default is 80).
	 * @param strict true to strictly enforce the line-width limit, even if it means breaking
	 * a symbol in the middle.  When false, very long names may exceed the width limit.
	 */
	protected void setOutputWidth(int width, boolean strict) { maxWidth = width;   strictWidthLimit = strict; }

	/**
	 * Method to set the string that will be emitted at the start of a "continuation line".
	 * The continuation line is the line that follows a line which is broken up because
	 * of width limits.
	 * @param str the string that will be emitted at the start of a "continuation line".
	 */
	protected void setContinuationString(String str) { continuationString = str; }

	private void writeChunk(String str)
	{
		int len = str.length();
		if (len <= 0) return;
		if (printWriter != null) printWriter.print(str); else
			if (stringWriter != null) stringWriter.write(str);
		lineCharCount += len;
		if (str.charAt(len-1) == '\n') lineCharCount = 0;
	}

	/**
	 * Method to add a string to the output, limited by the maximum
	 * width of an output line.
	 * @param str the string to add to the output.
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
			String exact = str.substring(0, maxWidth - lineCharCount);
			int splitPos = exact.lastIndexOf(' ');
			if (splitPos < 0)
			{
				int commaPos = exact.lastIndexOf(',');
				if (commaPos > splitPos) splitPos = commaPos;
				int openPos = exact.lastIndexOf('(');
				if (openPos > splitPos) splitPos = openPos;
				int closePos = exact.lastIndexOf(')');
				if (closePos > splitPos) splitPos = closePos;
				int semiPos = exact.lastIndexOf(';');
				if (semiPos > splitPos) splitPos = semiPos;

				if (splitPos < 0)
				{
					splitPos = exact.length()-1;
					if (!strictWidthLimit)
					{
						splitPos = str.length()-1;
						int spacePos = str.indexOf(' ');
						if (spacePos >= 0 && spacePos < splitPos) splitPos = spacePos;
						commaPos = str.indexOf(',');
						if (commaPos >= 0 && commaPos < splitPos) splitPos = commaPos;
						openPos = str.indexOf('(');
						if (openPos >= 0 && openPos < splitPos) splitPos = openPos;
						closePos = str.indexOf(')');
						if (closePos >= 0 && closePos < splitPos) splitPos = closePos;
						semiPos = str.indexOf(';');
						if (semiPos >= 0 && semiPos < splitPos) splitPos = semiPos;
					}
				}
			}
			while (splitPos+1 < str.length() && str.charAt(splitPos+1) == '\n') splitPos++;
			exact = str.substring(0, splitPos+1);

			writeChunk(exact);
			if (!exact.endsWith("\n"))
			{
				writeChunk("\n");
				if (continuationString.length() > 0) writeChunk(continuationString);
			}
			str = str.substring(exact.length());
		}
	}

	/**
	 * Method to determine the area of a cell that is to be printed.
	 * Returns null if the area cannot be determined.
	 */
	public static Rectangle2D getAreaToPrint(Cell cell, boolean reduce, EditWindow_ wnd)
	{
		ERectangle cb = cell.getBounds();
		Rectangle2D bounds = new Rectangle2D.Double(cb.getMinX(), cb.getMinY(), cb.getWidth(), cb.getHeight());
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
	 * Class to write a library in a CHANGE Job.
	 * Used by regressions that need to queue the output after existing change jobs.
	 */
	public static class WriteJELIB extends Job
    {
        private Library lib;
        private String newName;
        private IdMapper idMapper;
        private int backupScheme;

        public WriteJELIB(Library lib, String newName)
        {
            super("Write "+lib, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.lib = lib;
            this.newName = newName;
            backupScheme = IOTool.getBackupRedundancy();
            startJob();
        }

        public boolean doIt() throws JobException
        {
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
            } catch (Exception e)
            {
            	throw new JobException("Exception caught when saving library: " + e.getMessage());
            }
            Output.writeLibrary(lib, FileType.JELIB, false, false, false, backupScheme);
            return false;
        }

        public void terminateOK() {
            User.fixStaleCellReferences(idMapper);
        }
    }
}


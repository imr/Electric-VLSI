/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FileMenu.java
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

package com.sun.electric.tool.user.menus;

import static com.sun.electric.tool.user.menus.EMenuItem.SEPARATOR;

import com.sun.electric.database.IdMapper;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.JobManager;
import com.sun.electric.tool.cvspm.CVS;
import com.sun.electric.tool.cvspm.Commit;
import com.sun.electric.tool.cvspm.Edit;
import com.sun.electric.tool.cvspm.Update;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.input.GDSMap;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.output.PostScript;
import com.sun.electric.tool.project.AddCellJob;
import com.sun.electric.tool.project.AddLibraryJob;
import com.sun.electric.tool.project.CancelCheckOutJob;
import com.sun.electric.tool.project.CheckInJob;
import com.sun.electric.tool.project.CheckOutJob;
import com.sun.electric.tool.project.DeleteCellJob;
import com.sun.electric.tool.project.HistoryDialog;
import com.sun.electric.tool.project.LibraryDialog;
import com.sun.electric.tool.project.UpdateJob;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Clipboard;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.projectSettings.ProjSettings;
import com.sun.electric.tool.user.dialogs.ChangeCurrentLib;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.dialogs.ProjectSettingsFrame;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ElectricPrinter;
import com.sun.electric.tool.user.ui.TextWindow;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

/**
 * Class to handle the commands in the "File" pulldown menu.
 */
public class FileMenu {

    static EMenu makeMenu() {
		/****************************** THE FILE MENU ******************************/

		// mnemonic keys available:    D               T  WXYZ
        return new EMenu("_File",

            new EMenuItem("_New Library...") { public void run() {
                newLibraryCommand(); }},
			ToolBar.openLibraryCommand, // O

		// mnemonic keys available: A    F HIJK  NO QR   VW YZ
            new EMenu("_Import",
                new EMenuItem("_CIF (Caltech Intermediate Format)...") { public void run() {
                    importLibraryCommand(FileType.CIF); }},
                new EMenuItem("_GDS II (Stream)...") { public void run() {
                    importLibraryCommand(FileType.GDS); }},
                new EMenuItem("GDS _Map File...") {	public void run() {
                    GDSMap.importMapFile(); }},
                new EMenuItem("_EDIF (Electronic Design Interchange Format)...") { public void run() {
                    importLibraryCommand(FileType.EDIF); }},
                new EMenuItem("_LEF (Library Exchange Format)...") { public void run() {
                    importLibraryCommand(FileType.LEF); }},
                new EMenuItem("_DEF (Design Exchange Format)...") {	public void run() {
                    importLibraryCommand(FileType.DEF); }},
                new EMenuItem("D_XF (AutoCAD)...") { public void run() {
                    importLibraryCommand(FileType.DXF); }},
                new EMenuItem("S_UE (Schematic User Environment)...") {	public void run() {
                    importLibraryCommand(FileType.SUE); }},
                IOTool.hasDais() ? new EMenuItem("Dais (_Sun CAD)...") { public void run() {
                    importLibraryCommand(FileType.DAIS); }} : null,
                SEPARATOR,
                new EMenuItem("ELI_B...") {	public void run() {
                    importLibraryCommand(FileType.ELIB); }},
                new EMenuItem("_Readable Dump...") { public void run() {
                    importLibraryCommand(FileType.READABLEDUMP); }},
                new EMenuItem("_Text Cell Contents...") { public void run() {
                    TextWindow.readTextCell(); }},
                new EMenuItem("_Preferences...") { public void run() {
                    Job.getUserInterface().importPrefs(); }},
                new EMenuItem("Project Settings...") { public void run() {
                    ProjSettings.importSettings(); }}
            ),

            SEPARATOR,

            new EMenuItem("_Close Library") { public void run() {
                closeLibraryCommand(Library.getCurrent()); }},
			ToolBar.saveLibraryCommand, // V
            new EMenuItem("Save Library _As...") { public void run() {
                if (checkInvariants()) saveAsLibraryCommand(Library.getCurrent()); }},
            new EMenuItem("_Save All Libraries", 'S') { public void run() {
                if (checkInvariants()) saveAllLibrariesCommand(); }},
            new EMenuItem("Save All Libraries in _Format...") { public void run() {
                if (checkInvariants()) saveAllLibrariesInFormatCommand(); }},

		// mnemonic keys available:    D     J  M   Q   UVW YZ
            new EMenu("_Export",
                new EMenuItem("_CIF (Caltech Intermediate Format)...") { public void run() {
                    exportCommand(FileType.CIF, false); }},
                new EMenuItem("_GDS II (Stream)...") { public void run() {
                    exportCommand(FileType.GDS, false); }},
                new EMenuItem("ED_IF (Electronic Design Interchange Format)...") { public void run() {
                    exportCommand(FileType.EDIF, false); }},
                new EMenuItem("LE_F (Library Exchange Format)...") { public void run() {
                    exportCommand(FileType.LEF, false); }},
                new EMenuItem("_L...") { public void run() {
                    exportCommand(FileType.L, false); }},
                IOTool.hasSkill() ? new EMenuItem("S_kill (Cadence Commands)...") {	public void run() {
                    exportCommand(FileType.SKILL, false); }} : null,
                IOTool.hasSkill() ? new EMenuItem("Skill Exports _Only (Cadence Commands)...") { public void run() { 
                    exportCommand(FileType.SKILLEXPORTSONLY, false); }} : null,
                SEPARATOR,
                new EMenuItem("_Eagle...") { public void run() {
                    exportCommand(FileType.EAGLE, false); }},
                new EMenuItem("EC_AD...") {	public void run() {
                    exportCommand(FileType.ECAD, false); }},
                new EMenuItem("Pad_s...") {	public void run() {
                    exportCommand(FileType.PADS, false); }},
                SEPARATOR,
                new EMenuItem("_Text Cell Contents...") { public void run() {
                    TextWindow.writeTextCell(); }},
                new EMenuItem("_PostScript...") { public void run() {
                    exportCommand(FileType.POSTSCRIPT, false); }},
                new EMenuItem("P_NG (Portable Network Graphics)...") { public void run() {
                    exportCommand(FileType.PNG, false); }},
                new EMenuItem("_HPGL...") { public void run() {
                    exportCommand(FileType.HPGL, false); }},
                new EMenuItem("D_XF (AutoCAD)...") { public void run() {
                    exportCommand(FileType.DXF, false); }},
                SEPARATOR,
                new EMenuItem("ELI_B (Version 6)...") {	public void run() {
                    saveLibraryCommand(Library.getCurrent(), FileType.ELIB, true, false, false); }},
                new EMenuItem(Job.getDebug() ? "_JELIB (Version 8.04k)" : "_JELIB (Version 8.03)") { public void run() {
                    saveOldJelib(); }},
                new EMenuItem("P_references...") { public void run() { 
                    Job.getUserInterface().exportPrefs(); }},
                new EMenuItem("Project Settings...") { public void run() {
                    ProjSettings.exportSettings(); }}
            ),

            SEPARATOR,

            new EMenuItem("Change Current _Library...") { public void run() {
                ChangeCurrentLib.showDialog(); }},
            new EMenuItem("List Li_braries") { public void run() {
                CircuitChanges.listLibrariesCommand(); }},
            new EMenuItem("Rena_me Library...") { public void run() {
                CircuitChanges.renameLibrary(Library.getCurrent()); }},
            new EMenuItem("Mar_k All Libraries for Saving") { public void run() {
                CircuitChangeJobs.markAllLibrariesForSavingCommand(); }},

		// mnemonic keys available: AB DEFGHIJKLMNOPQ STUVWXYZ
            new EMenu("C_heck Libraries",
                new EMenuItem("_Check") { public void run() {
                    CircuitChanges.checkAndRepairCommand(false); }},
                new EMenuItem("_Repair") { public void run() {
                    CircuitChanges.checkAndRepairCommand(true); }}),

            SEPARATOR,

        // mnemonic keys available:   C EFG  JK MN PQ ST VWXYZ
            new EMenu("P_roject Management",
                new EMenuItem("_Update") { public void run() {
                    UpdateJob.updateProject(); }},
                new EMenuItem("Check _Out This Cell") { public void run() {
                    CheckOutJob.checkOutThisCell(); }},
                new EMenuItem("Check _In This Cell...") { public void run() {
                    CheckInJob.checkInThisCell(); }},
                SEPARATOR,
                new EMenuItem("Roll_back and Release Check-Out") { public void run() {
                    CancelCheckOutJob.cancelCheckOutThisCell(); }},
                new EMenuItem("_Add This Cell") { public void run() {
                    AddCellJob.addThisCell(); }},
                new EMenuItem("_Remove This Cell") { public void run() {
                    DeleteCellJob.removeThisCell(); }},
                new EMenuItem("Show _History of This Cell...") { public void run() {
                    HistoryDialog.examineThisHistory(); }},
                SEPARATOR,
                new EMenuItem("Get Library From Repository...") { public void run() {
                    LibraryDialog.getALibrary(); }},
                new EMenuItem("Add Current _Library To Repository") { public void run() {
                    AddLibraryJob.addThisLibrary(); }},
                new EMenuItem("Ad_d All Libraries To Repository") {	public void run() {
                    AddLibraryJob.addAllLibraries(); }}),

        // mnemonic keys available: ABCDEFGHIJKLMNOPQRSTUVWXYZ
            new EMenu("C_VS",
                new EMenuItem("Commit All Open Libraries") { public void run() {
                    Commit.commitAllLibraries(); }},
                new EMenuItem("Update Open Libraries") { public void run() {
                    Update.updateOpenLibraries(Update.UPDATE); }},
                new EMenuItem("Get Status Open Libraries") { public void run() {
                    Update.updateOpenLibraries(Update.STATUS); }},
                new EMenuItem("List Editors Open Libraries") { public void run() {
                    Edit.listEditorsOpenLibraries(); }},
                SEPARATOR,
                new EMenuItem("Update Project Libraries") { public void run() {
                    Update.updateProject(Update.UPDATE); }},
                new EMenuItem("Get Status Project Libraries") { public void run() {
                    Update.updateProject(Update.STATUS); }},
                new EMenuItem("List Editors Project Libraries") { public void run() {
                    Edit.listEditorsProject(); }},
                SEPARATOR,
                new EMenuItem("Checkout From Repository") { public void run() {
                    CVS.checkoutFromRepository(); }})
                {
                    @Override
                    public boolean isEnabled() { return CVS.isEnabled(); }
                    
                    @Override
                    protected void registerItem() {
                        super.registerItem();
                        registerUpdatable();
                    }
                 },

            SEPARATOR,

            new EMenuItem("Pa_ge Setup...") { public void run() {
                pageSetupCommand(); }},
            new EMenuItem("_Print...") { public void run() {
                printCommand(); }},

            SEPARATOR,

			ToolBar.preferencesCommand, // R
            new EMenuItem("Pro_ject Settings...") { public void run() {
                ProjectSettingsFrame.projectSettingsCommand(); }},

            SEPARATOR,

            !Client.isOSMac() ?	new EMenuItem("_Quit", 'Q') { public void run() {
                quitCommand(); }} : null,
            new EMenuItem("Force Q_uit (and Save)") { public void run() {
                forceQuit(); }});
    }

    // ------------------------------ File Menu -----------------------------------

    public static void newLibraryCommand()
    {
        String newLibName = JOptionPane.showInputDialog("New Library Name", "");
        if (newLibName == null) return;
        NewLibrary job = new NewLibrary(newLibName);
    }

	private static class NewLibrary extends Job {
        private String newLibName;

        public NewLibrary(String newLibName) {
            super("New Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.newLibName = newLibName;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            Library lib = Library.newInstance(newLibName, null);
            if (lib == null) return false;
            lib.setCurrent();
            System.out.println("New "+lib+" created");
            return true;
        }

        public void terminateOK()
        {
            EditWindow.repaintAll();
            ToolBar.setSaveLibraryButton();
        }
    }

    /**
     * This method implements the command to read a library.
     * It is interactive, and pops up a dialog box.
     */
    public static void openLibraryCommand()
    {
        String fileName = OpenFile.chooseInputFile(FileType.LIBRARYFORMATS, null);
        if (fileName != null)
        {
            // start a job to do the input
            URL fileURL = TextUtils.makeURLToFile(fileName);
			String libName = TextUtils.getFileNameWithoutExtension(fileURL);
			Library deleteLib = Library.findLibrary(libName);
			if (deleteLib != null)
			{
				// library already exists, prompt for save
				if (FileMenu.preventLoss(deleteLib, 2)) return;
				WindowFrame.removeLibraryReferences(deleteLib);
			}
			FileType type = getLibraryFormat(fileName, FileType.DEFAULTLIB);
			new ReadLibrary(fileURL, type, deleteLib, null);
        }
    }

    /** Get the type from the fileName, or if no valid Library type found, return defaultType.
     */
    public static FileType getLibraryFormat(String fileName, FileType defaultType) {
        if (fileName != null)
        {
            if (fileName.endsWith(File.separator)) {
                fileName = fileName.substring(0, fileName.length()-File.separator.length());
            }
            for (int i=0; i<FileType.libraryTypes.length; i++) {
                FileType type = FileType.libraryTypes[i];
                if (fileName.endsWith("."+type.getExtensions()[0])) return type;
            }
        }
        return defaultType;
    }

	/**
	 * Class to read a library in a new thread.
	 * For a non-interactive script, use ReadLibrary job = new ReadLibrary(filename, format).
	 */
	public static class ReadLibrary extends Job
	{
		private URL fileURL;
		private FileType type;
		private Library deleteLib;
        private String cellName; // cell to view once the library is open
        private HashMap<Object,Map<String,Object>> meaningVariables;
        private Library lib;

		public ReadLibrary(URL fileURL, FileType type, Library deleteLib, String cellName)
		{
			super("Read External Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fileURL = fileURL;
			this.type = type;
			this.deleteLib = deleteLib;
            this.cellName = cellName;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// see if the former library can be deleted
			if (deleteLib != null)
			{
//				if (Library.getVisibleLibraries().size() > 1)
//				{
					if (!deleteLib.kill("replace")) return false;
					deleteLib = null;
//				} else
//				{
//					// cannot delete last library: must delete it later
//					// mangle the name so that the new one can be created
//					deleteLib.setName("FORMERVERSIONOF" + deleteLib.getName());
//				}
			}
            fieldVariableChanged("meaningVariables");
            meaningVariables = new HashMap<Object,Map<String,Object>>();
        	lib = LibraryFiles.readLibrary(fileURL, null, type, false, meaningVariables);
            if (lib == null) return false;
            fieldVariableChanged("lib");
            // new library open: check for default "noname" library and close if empty
            Library noname = Library.findLibrary("noname");
            if (noname != null) {
                if (!noname.getCells().hasNext()) {
                    noname.kill("delete");
                }
            }
//            Undo.noUndoAllowed();
            lib.setCurrent();
			return true;
		}

        public void terminateOK()
        {
            // read project settings
            File projsettings = new File(User.getWorkingDirectory(), "projsettings.xml");
            if (projsettings.exists()) {
                ProjSettings.readSettings(projsettings, false);
            } else {
                Pref.reconcileMeaningVariables(lib.getName(), meaningVariables);
                meaningVariables = null;
            }

            Cell showThisCell = (cellName != null) ?
                    lib.findNodeProto(cellName) :
                    Job.getUserInterface().getCurrentCell(lib);
        	doneOpeningLibrary(showThisCell);

            // Repair libraries.
            CircuitChanges.checkAndRepairCommand(true);
        }
	}

	/**
	 * Class to import a library in a new thread.
	 */
	public static class ImportLibrary extends Job
	{
		private URL fileURL;
		private FileType type;
		private Library createLib;
		private Library deleteLib;
//		private Cell showThisCell;

		public ImportLibrary(URL fileURL, FileType type, Library deleteLib)
		{
			super("Import External Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fileURL = fileURL;
			this.type = type;
			this.deleteLib = deleteLib;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// see if the former library can be deleted
			if (deleteLib != null)
			{
//				if (Library.getVisibleLibraries().size() > 1)
//				{
					if (!deleteLib.kill("replace")) return false;
					deleteLib = null;
//				} else
//				{
//					// cannot delete last library: must delete it later
//					// mangle the name so that the new one can be created
//					deleteLib.setName("FORMERVERSIONOF" + deleteLib.getName());
//				}
			}
			createLib = Input.importLibrary(fileURL, type);
			if (createLib == null) return false;

            // new library open: check for default "noname" library and close if empty
            Library noname = Library.findLibrary("noname");
            if (noname != null) {
            	if (!noname.getCells().hasNext()) {
                	noname.kill("delete");
                }
            }
            
//            Undo.noUndoAllowed();
//            showThisCell =  Job.getUserInterface().getCurrentCell(createLib);
// 			fieldVariableChanged("showThisCell");
//			if (deleteLib != null)
//				deleteLib.kill("replace");
			fieldVariableChanged("createLib");
			return true;
		}

		public void terminateOK()
        {
            createLib.setCurrent();
        	Cell showThisCell = Job.getUserInterface().getCurrentCell(createLib);
        	doneOpeningLibrary(showThisCell);
//        	fieldVariableChanged("showThisCell");
        }
	}

    /**
     * Method to clean up from opening a library.
     * Called from the "terminateOK()" method of a job.
     */
    private static void doneOpeningLibrary(final Cell cell)
    {
        if (cell == null) System.out.println("No current cell in this library");
        else if (!Job.BATCHMODE)
        {
            CreateCellWindow creator = new CreateCellWindow(cell);
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(creator);
            } else {
                creator.run();
            }
        }
		SwingUtilities.invokeLater(new Runnable() {
			public void run()
            {
                // Redo explorer trees to add new library
                WindowFrame.wantToRedoLibraryTree();
                WindowFrame.wantToOpenCurrentLibrary(true, cell);
            }});
    }

	/**
	 * Class to read initial libraries into Electric.
	 */
	public static class ReadInitialELIBs extends Job
    {
        private List<URL> fileURLs;
//        private Cell showThisCell;

        public ReadInitialELIBs(List<URL> fileURLs) {
            super("Read Initial Libraries", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.fileURLs = fileURLs;
            startJob();
        }

        public boolean doIt() throws JobException {
            // try to open initial libraries
//            boolean success = false;
            for (URL file : fileURLs) {
                FileType defType = null;
                String fileName = file.getFile();
                defType = getLibraryFormat(fileName, defType);
                if (defType == null) {
                    // no valid extension, search for file with extension
                    URL f = TextUtils.makeURLToFile(fileName + "." + FileType.JELIB.getExtensions()[0]);
                    if (TextUtils.URLExists(f, null)) {
                        defType = FileType.JELIB;
                        file = f;
                    }
                }
                if (defType == null) {
                    // no valid extension, search for file with extension
                    URL f = TextUtils.makeURLToFile(fileName + "." + FileType.ELIB.getExtensions()[0]);
                    if (TextUtils.URLExists(f, null)) {
                        defType = FileType.ELIB;
                        file = f;
                    }
                }
                if (defType == null) {
                    // no valid extension, search for file with extension
                    URL f = TextUtils.makeURLToFile(fileName + "." + FileType.DELIB.getExtensions()[0]);
                    if (TextUtils.URLExists(f, null)) {
                        defType = FileType.DELIB;
                        file = f;
                    }
                }
                if (defType == null) {
                    // no valid extension, search for file with extension
                    URL f = TextUtils.makeURLToFile(fileName + "." + FileType.READABLEDUMP.getExtensions()[0]);
                    if (TextUtils.URLExists(f, null)) {
                        defType = FileType.READABLEDUMP;
                        file = f;
                    }
                }
                if (defType == null) defType = FileType.DEFAULTLIB;
                new ReadLibrary(file, defType, null, null);
//                showThisCell = openALibrary(file, defType);
            }
//			fieldVariableChanged("showThisCell");
            return true;
        }

//        public void terminateOK()
//        {
//        	doneOpeningLibrary(showThisCell);
//        }
    }

	/**
	 * Class to display a new window.
	 */
    public static class CreateCellWindow implements Runnable {
        private Cell cell;
        public CreateCellWindow(Cell cell) { this.cell = cell; }
        public void run() {
            // check if edit window open with null cell, use that one if exists
            for (Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
            {
                WindowFrame wf = it.next();
                WindowContent content = wf.getContent();
                if (content.getCell() == null)
                {
                    wf.setCellWindow(cell, null);
                    //wf.requestFocus();
                    ToolBar.setSaveLibraryButton();
                    return;
                }
            }
            WindowFrame.createEditWindow(cell);

            // no clean for now.
            ToolBar.setSaveLibraryButton();
        }
    }

	/**
	 * This method implements the command to import a library (Readable Dump or JELIB format).
	 * It is interactive, and pops up a dialog box.
	 */
	public static void importLibraryCommand(FileType type)
	{
		String fileName = null;
		if (type == FileType.DAIS)
		{
			fileName = OpenFile.chooseDirectory(type.getDescription());
		} else
		{
			fileName = OpenFile.chooseInputFile(type, null);
		}
		if (fileName != null)
		{
			// start a job to do the input
			URL fileURL = TextUtils.makeURLToFile(fileName);
			String libName = TextUtils.getFileNameWithoutExtension(fileURL);
			Library deleteLib = Library.findLibrary(libName);
			if (deleteLib != null)
			{
				// library already exists, prompt for save
				if (FileMenu.preventLoss(deleteLib, 2)) return;
				WindowFrame.removeLibraryReferences(deleteLib);
			}
			new ImportLibrary(fileURL, type, deleteLib);
		}
	}

    public static void closeLibraryCommand(Library lib)
    {
        if (lib == null) return;
	    Set<Cell> found = Library.findReferenceInCell(lib);

		// if all references are from the clipboard, request that the clipboard be cleared, too
		boolean clearClipboard = false, nonClipboard = false;
	    for (Cell cell : found)
	    {
		   if (cell.getLibrary().isHidden()) clearClipboard = true; else
			   nonClipboard = true;
	    }

		// You can't close it because there are open cells that refer to library elements
		if (nonClipboard)
	    {
		    System.out.println("Cannot close " + lib + ":");
		    System.out.print("\t Cells ");

		    for (Cell cell : found)
		    {
			   System.out.print("'" + cell.getName() + "'(" + cell.getLibrary().getName() + ") ");
		    }
		    System.out.println("refer to it.");
		    return;
	    }
	    if (preventLoss(lib, 1)) return;

        WindowFrame.removeLibraryReferences(lib);
        CloseLibrary job = new CloseLibrary(lib, clearClipboard);
    }

	private static class CloseLibrary extends Job {
        private Library lib;
		private boolean clearClipboard;

        public CloseLibrary(Library lib, boolean clearClipboard) {
            super("Close "+lib, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.lib = lib;
            this.clearClipboard = clearClipboard;
            startJob();
        }

        public boolean doIt() throws JobException {
			if (clearClipboard)
			{
				Clipboard.clear();
			}
            if (lib.kill("delete"))
            {
                System.out.println("Library '" + lib.getName() + "' closed");
            }
            return true;
        }

        public void terminateOK()
        {
            WindowFrame.wantToRedoTitleNames();
            EditWindow.repaintAll();

            // Disable save icon if no more libraries are open
            ToolBar.setSaveLibraryButton();
        }
    }

    /**
     * This method implements the command to save a library.
     * It is interactive, and pops up a dialog box.
     * @param lib the Library to save.
     * @return true if library saved, false otherwise.
     */
    public static boolean saveLibraryCommand(Library lib) {
		return lib != null && saveLibraryCommand(lib, FileType.DEFAULTLIB, false, true, false);
	}

    /**
     * This method implements the command to save a library.
     * It is interactive, and pops up a dialog box.
     * @param lib the Library to save.
     * @param type the format of the library (OpenFile.Type.ELIB, OpenFile.Type.READABLEDUMP, or OpenFile.Type.JELIB).
     * @param compatibleWith6 true to write a library that is compatible with version 6 Electric.
     * @param forceToType
     * @param saveAs true if this is a "save as" and should always prompt for a file name.
     * @return true if library saved, false otherwise.
     */
    public static boolean saveLibraryCommand(Library lib, FileType type, boolean compatibleWith6, boolean forceToType, boolean saveAs)
    {
        // scan for Dummy Cells, warn user that they still exist
        List<String> dummyCells = new ArrayList<String>();
        dummyCells.add("WARNING: "+lib+" contains the following Dummy cells:");
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell c = it.next();
            if (c.getVar(LibraryFiles.IO_DUMMY_OBJECT) != null) {
                dummyCells.add("   "+c.noLibDescribe());
            }
        }
        if (dummyCells.size() > 1) {
            dummyCells.add("Do you really want to write this library?");
            String [] options = {"Continue Writing", "Cancel" };
            String message = dummyCells.toString();
            int val = Job.getUserInterface().askForChoice(message,
                    "Dummy Cells Found in "+lib, options, options[1]);
            if (val == 1) return false;
        }

        String [] extensions = type.getExtensions();
        String extension = extensions[0];
        String fileName = null;
        if (!saveAs && lib.isFromDisk())
        {
        	if (type == FileType.JELIB || type == FileType.DELIB || 
        		(type == FileType.ELIB && !compatibleWith6))
	        {
	            fileName = lib.getLibFile().getPath();
	            if (forceToType)
	            {
		            type = OpenFile.getOpenFileType(fileName, FileType.DEFAULTLIB);
	            }
	        }
            // check to see that file is writable
            if (fileName != null) {
                File file = new File(fileName);
                if (file.exists() && !file.canWrite()) fileName = null;
/*
                try {
                    if (!file.createNewFile()) fileName = null;
                } catch (java.io.IOException e) {
                    System.out.println(e.getMessage());
                    fileName = null;
                }
*/
            }
        }
        if (fileName == null)
        {
            fileName = OpenFile.chooseOutputFile(FileType.libraryTypes, null, lib.getName() + "." + extension);
            if (fileName == null) return false;
            type = getLibraryFormat(fileName, type);
//            // mark for saving, all libraries that depend on this
//            for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
//            {
//                Library oLib = it.next();
//                if (oLib.isHidden()) continue;
//                if (oLib == lib) continue;
//                if (oLib.isChangedMajor()) continue;
//
//                // see if any cells in this library reference the renamed one
//                if (oLib.referencesLib(lib))
//                    oLib.setChanged();
//            }
        }

        // save the library
        new SaveLibrary(lib, fileName, type, compatibleWith6);
        return true;
    }

    private static void saveOldJelib() {
        String currentDir = User.getWorkingDirectory();
        System.out.println("Saving libraries in oldJelib directory under " + currentDir); System.out.flush();
        File oldJelibDir = new File(currentDir, "oldJelib");
        if (!oldJelibDir.exists() && !oldJelibDir.mkdir()) {
            JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String [] {"Could not create oldJelib directory",
                 oldJelibDir.getAbsolutePath()}, "Error creating oldJelib directory", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Output.writePanicSnapshot(EDatabase.clientDatabase().backup(), oldJelibDir, true);
    }

//    public static boolean saveLibraryNoJob(String newName, Library lib, FileType type, boolean compatibleWith6)
//    {
//        return SaveLibrary.performTaskNoJob(newName, lib, type, compatibleWith6);
//    }

    /**
     * Class to save a library in a new thread.
     * For a non-interactive script, use SaveLibrary job = new SaveLibrary(filename).
     * Saves as an elib.
     */
    private static class SaveLibrary extends Job
    {
        private Library lib;
        private String newName;
        private FileType type;
        private boolean compatibleWith6;
        private IdMapper idMapper;

        public SaveLibrary(Library lib, String newName, FileType type, boolean compatibleWith6)
        {
            super("Write "+lib, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER); // CHANGE because of possible renaming
            this.lib = lib;
            this.newName = newName;
            this.type = type;
            this.compatibleWith6 = compatibleWith6;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            boolean success = false;
            try
            {
//                success = performTaskNoJob(newName, lib, type, compatibleWith6);
                // rename the library if requested
                if (newName != null) {
                    URL libURL = TextUtils.makeURLToFile(newName);
                    lib.setLibFile(libURL);
                    idMapper = lib.setName(TextUtils.getFileNameWithoutExtension(libURL));
                    if (idMapper != null)
                        lib = EDatabase.serverDatabase().getLib(idMapper.get(lib.getId()));
                }
                fieldVariableChanged("idMapper");
                
                success = !Output.writeLibrary(lib, type, compatibleWith6, false, false);
            } catch (Exception e)
            {
                e.printStackTrace(System.out);
            	throw new JobException("Exception caught when saving files: " +
                    e.getMessage() + "Please check your disk libraries");
            }
            if (!success)
            	throw new JobException("Error saving files.  Please check your disk libraries");
            return success;
        }

        public void terminateOK() {
            User.fixStaleCellReferences(idMapper);
        }
//        protected static boolean performTaskNoJob(String newName, Library lib, FileType type, boolean compatibleWith6)
//        {
//            // rename the library if requested
//            if (newName != null)
//            {
//                URL libURL = TextUtils.makeURLToFile(newName);
//                lib.setLibFile(libURL);
//                IdMapper idMapper = lib.setName(TextUtils.getFileNameWithoutExtension(libURL));
//            }
//
//            boolean error = Output.writeLibrary(lib, type, compatibleWith6, false);
//            if (error) return false;
//            return true;
//        }
    }

    /**
     * This method implements the command to save a library to a different file.
     * It is interactive, and pops up a dialog box.
     */
    public static void saveAsLibraryCommand(Library lib)
    {
        saveLibraryCommand(lib, FileType.DEFAULTLIB, false, true, true);
        WindowFrame.wantToRedoTitleNames();
    }

    /**
     * This method implements the command to save all libraries.
     */
    public static void saveAllLibrariesCommand()
    {
        saveAllLibrariesCommand(FileType.DEFAULTLIB, false, true);
    }

    public static void saveAllLibrariesCommand(FileType type, boolean compatibleWith6, boolean forceToType)
    {
		HashMap<Library,FileType> libsToSave = new HashMap<Library,FileType>();
        for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = it.next();
            if (lib.isHidden()) continue;
            if (!lib.isChanged()) continue;
            if (lib.getLibFile() != null)
                type = getLibraryFormat(lib.getLibFile().getFile(), type);
			libsToSave.put(lib, type);
        }
		boolean justSkip = false;
		for(Iterator<Library> it = libsToSave.keySet().iterator(); it.hasNext(); )
		{
			Library lib = it.next();
			type = libsToSave.get(lib);
            if (!saveLibraryCommand(lib, type, compatibleWith6, forceToType, false))
			{
				if (justSkip) continue;
				if (it.hasNext())
				{
					String [] options = {"Cancel", "Skip this Library"};
					int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
						"Cancel all library saving, or just skip saving this library?", "Save Cancelled",
						JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, "Cancel");
					if (ret == 1) { justSkip = true;   continue; }
				}
				break;
			}
		}
    }

    public static void saveAllLibrariesInFormatCommand() {
        Object[] formats = {FileType.JELIB, FileType.ELIB, FileType.READABLEDUMP, FileType.DELIB};
        Object format = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(),
                "Output file format for all libraries:", "Save All Libraries In Format...",
                JOptionPane.PLAIN_MESSAGE,
                null, formats, FileType.DEFAULTLIB);
        if (format == null) return; // cancel operation
        FileType outType = (FileType)format;
        SaveAllLibrariesInFormatJob job = new SaveAllLibrariesInFormatJob(outType);
    }

    public static class SaveAllLibrariesInFormatJob extends Job {
        private FileType outType;
        public SaveAllLibrariesInFormatJob(FileType outType) {
            super("Save All Libraries", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.outType = outType;
            startJob();
        }
        public boolean doIt() {
            for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
                Library lib = it.next();
                if (lib.isHidden()) continue;
                if (!lib.isFromDisk()) continue;
                if (lib.getLibFile() != null) {
                    // set library file to new format
                    String fullName = lib.getLibFile().getFile();
                    //if (fullName.endsWith("spiceparts.txt")) continue; // ignore spiceparts library
                    // match ".<word><endline>"
                    fullName = fullName.replaceAll("\\.\\w*?$", "."+outType.getExtensions()[0]);
                    lib.setLibFile(TextUtils.makeURLToFile(fullName));
                }
                lib.setChanged();
            }
            saveAllLibrariesCommand(outType, false, false);
            return true;
        }
    }

    /**
     * This method checks database invariants.
     * @return true if database is valid or user forced saving.
     */
	private static boolean checkInvariants()
	{
// 		// check database invariants
// 		if (!EDatabase.clientDatabase().checkInvariants())
// 		{
// 			String [] messages = {
// 				"Database invariants are not valid",
// 				"You may use \"Force Quit (and Save)\" to save in a panic directory",
// 				"Do you really want to write libraries into the working directory?"};
//             Object [] options = {"Continue Writing", "Cancel", "ForceQuit" };
//             int val = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(), messages,
// 				"Invalid database invariants", JOptionPane.DEFAULT_OPTION,
// 				JOptionPane.WARNING_MESSAGE, null, options, options[1]);
//             if (val == 1) return false;
// 			if (val == 2)
// 			{
// 				forceQuit();
// 				return false;
// 			}
// 		}
		return true;
	}

    /**
     * This method implements the export cell command for different export types.
     * It is interactive, and pops up a dialog box.
     */
    public static void exportCommand(FileType type, boolean isNetlist)
    {
		// synchronization of PostScript is done first because no window is needed
        if (type == FileType.POSTSCRIPT)
        {
            if (PostScript.syncAll()) return;
        }

	    WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
	    WindowContent wnd = (wf != null) ? wf.getContent() : null;

	    if (wnd == null)
        {
            System.out.println("No current window");
            return;
        }
        Cell cell = wnd.getCell();
        if (cell == null)
        {
            System.out.println("No cell in this window");
            return;
        }
        VarContext context = (wnd instanceof EditWindow) ? ((EditWindow)wnd).getVarContext() : null;

        List<PolyBase> override = null;
        if (type == FileType.POSTSCRIPT)
        {
            if (cell.getView() == View.DOC)
            {
                System.out.println("Document cells can't be exported as postscript.");
                JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), "Document cells can't be exported as postscript.\n" +
                        "Try \"Export -> Text Cell Contents\"",
                    "Exporting PS", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (IOTool.isPrintEncapsulated()) type = FileType.EPS;
			if (wnd instanceof WaveformWindow)
			{
				// waveform windows provide a list of polygons to print
				WaveformWindow ww = (WaveformWindow)wnd;
				override = ww.getPolysForPrinting();
			}
        }

		String [] extensions = type.getExtensions();
        String filePath = ((cell != null) ? cell.getName() : "") + "." + extensions[0];

        // special case for spice
        if (type == FileType.SPICE &&
			!Simulation.getSpiceRunChoice().equals(Simulation.spiceRunChoiceDontRun))
        {
            // check if user specified working dir
            if (Simulation.getSpiceUseRunDir())
                filePath = Simulation.getSpiceRunDir() + File.separator + filePath;
            else
                filePath = User.getWorkingDirectory() + File.separator + filePath;
            // check for automatic overwrite
            if (User.isShowFileSelectionForNetlists() && !Simulation.getSpiceOutputOverwrite()) {
                String saveDir = User.getWorkingDirectory();
                filePath = OpenFile.chooseOutputFile(type, null, filePath);
                User.setWorkingDirectory(saveDir);
                if (filePath == null) return;
            }

            Output.exportCellCommand(cell, context, filePath, type, override);
            return;
        }

        if (User.isShowFileSelectionForNetlists() || !isNetlist)
        {
            filePath = OpenFile.chooseOutputFile(type, null, filePath);
            if (filePath == null) return;
        } else
        {
            filePath = User.getWorkingDirectory() + File.separator + filePath;
        }

	    // Special case for PNG format
	    if (type == FileType.PNG)
	    {
		    String name = (cell != null) ? cell.toString() : filePath;
		    ExportImage job = new ExportImage(name, wnd, filePath);
			return;
	    }

        Output.exportCellCommand(cell, context, filePath, type, override);
    }

    private static class ExportImage extends Job
	{
    	private String filePath;
		private WindowContent wnd;

		public ExportImage(String description, WindowContent wnd, String filePath)
		{
			super("Export "+description+" ("+FileType.PNG+")", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.wnd = wnd;
			this.filePath = filePath;
			startJob();
		}
		public boolean doIt() throws JobException
        {
			PrinterJob pj = PrinterJob.getPrinterJob();
	        ElectricPrinter ep = getOutputPreferences(wnd, pj);
            // Export has to be done in same thread as offscreen raster (valid for 3D at least)
            wnd.writeImage(ep, filePath);
//            BufferedImage img = wnd.getOffScreenImage(ep);
//			PNG.writeImage(img, filePath);
            return true;
        }
	}

    private static PageFormat pageFormat = null;

    public static void pageSetupCommand() {
        PrinterJob pj = PrinterJob.getPrinterJob();
        if (pageFormat == null)
            pageFormat = pj.pageDialog(pj.defaultPage());
        else
            pageFormat = pj.pageDialog(pageFormat);
    }

	private static ElectricPrinter getOutputPreferences(WindowContent context, PrinterJob pj)
	{
 		if (pageFormat == null)
		 {
			pageFormat = pj.defaultPage();
			pageFormat.setOrientation(PageFormat.LANDSCAPE);
            pageFormat = pj.validatePage(pageFormat);
		 }

 		ElectricPrinter ep = new ElectricPrinter(context, pageFormat, pj);
		pj.setPrintable(ep, pageFormat);
		return (ep);
	}

    /**
     * This method implements the command to print the current window.
     */
    public static void printCommand()
    {
    	WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        Cell cell = WindowFrame.getCurrentCell();
    	if (wf == null || cell == null)
    	{
    		System.out.println("No current window to print");
    		return;
    	}
        if (cell.getView() == View.DOC)
        {
            String message = "Document cells can't be printed as postscript.";
            System.out.println(message);
            JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), message,
                    "Printing Cell", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setJobName(wf.getTitle());
	    ElectricPrinter ep = getOutputPreferences(wf.getContent(), pj);

        // see if a default printer should be mentioned
        String pName = IOTool.getPrinterName();
        PrintService [] printers = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService printerToUse = null;
        for(PrintService printer : printers)
        {
            if (pName.equals(printer.getName()))
            {
                printerToUse = printer;
                break;
            }
        }
        if (printerToUse != null)
        {
            try
            {
                pj.setPrintService(printerToUse);
            } catch (PrinterException e)
            {
                System.out.println("Printing error "+e);
            }
        }

		PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        if (pj.printDialog(aset))
        {
			// disable double-buffering so prints look better
			JPanel overall = wf.getContent().getPanel();
			RepaintManager currentManager = RepaintManager.currentManager(overall);
			currentManager.setDoubleBufferingEnabled(false);

			Dimension oldSize = overall.getSize();
			ep.setOldSize(oldSize);

			// initialize for content-specific printing
            if (!wf.getContent().initializePrinting(ep, pageFormat))
            {
                String message = "Problems initializing printers. Check printer setup.";
                System.out.println(message);
                JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), message,
                    "Printing Cell", JOptionPane.ERROR_MESSAGE);
                return;
            }


            printerToUse = pj.getPrintService();
            if (printerToUse != null)
 				IOTool.setPrinterName(printerToUse.getName());
			SwingUtilities.invokeLater(new PrintJobAWT(wf, pj, oldSize, aset));
        }
    }

	private static class PrintJobAWT implements Runnable
	{
		private WindowFrame wf;
		private PrinterJob pj;
		private Dimension oldSize;
		private PrintRequestAttributeSet aset;

		PrintJobAWT(WindowFrame wf, PrinterJob pj, Dimension oldSize, PrintRequestAttributeSet aset)
		{
			this.wf = wf;
			this.pj = pj;
			this.oldSize = oldSize;
			this.aset = aset;
		}

		public void run()
		{
			try {
				pj.print(aset);
			} catch (PrinterException pe)
			{
				System.out.println("Print aborted.");
			}

			JPanel overall = wf.getContent().getPanel();
			RepaintManager currentManager = RepaintManager.currentManager(overall);
			currentManager.setDoubleBufferingEnabled(true);

			if (oldSize != null)
			{
				overall.setSize(oldSize);
				overall.validate();
			}
		}
	}

    /**
     * This method implements the command to quit Electric.
     */
    public static boolean quitCommand()
    {
        if (preventLoss(null, 0)) return false;

	    try {
            new QuitJob();
	    } catch (java.lang.NoClassDefFoundError e)
	    {
		    // Ignoring this one
		    return true;
	    } catch (Exception e)
	    {
		    // Don't quit in this case.
		    return false;
	    }
        return true;
    }

    /**
     * Class to clear the date information on a Cell.
     * Used by regressions to reset date information so that files match.
     */
    public static class ClearCellDate extends Job
    {
    	private String cellName;
    	private Cell cell;

    	public ClearCellDate(String cellName)
        {
            super("Clear Cell Dates", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cellName = cellName;
            startJob();
        }

    	public ClearCellDate(Cell cell)
        {
            super("Clear Cell Dates", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = cell;
            startJob();
        }

        public boolean doIt() throws JobException
        {
        	if (cell == null && cellName != null)
        		cell = (Cell)Cell.findNodeProto(cellName);
        	if (cell != null)
        	{
	        	cell.lowLevelSetRevisionDate(new Date(0));
	        	cell.lowLevelSetCreationDate(new Date(0));
        	}
            return true;
        }
    }

    /**
     * Class to quit Electric in a Job.
     * The quit function is done in a Job so that it can force all other jobs to finish.
     */
    public static class QuitJob extends Job
    {
        public QuitJob()
        {
            super("Quitting", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt() throws JobException
        {
            return true;
        }

        public void terminateOK()
        {
            try {
                Library.saveExpandStatus();
            } catch (BackingStoreException e)
            {
                int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
		            "Cannot save cell expand status. Do you still want to quit?", "Cell Status Error",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response != JOptionPane.YES_OPTION)
                {
                	return;
                }
            }

			// save changes to layer visibility
			Layer.preserveVisibility();

			// save changes to waveform window signals
			WaveformWindow.preserveSignalOrder();

			ActivityLogger.finished();
            System.exit(0);
        }
    }

    /**
     * Method to ensure that one or more libraries are saved.
     * @param desiredLib the library to check for being saved.
     * If desiredLib is null, all libraries are checked.
     * @param action the type of action that will occur:
     * 0: quit;
     * 1: close a library;
     * 2: replace a library.
     * @return true if the operation should be aborted;
     * false to continue with the quit/close/replace.
     */
    public static boolean preventLoss(Library desiredLib, int action)
    {
		boolean checkedInvariants = false;
        boolean saveCancelled = false;
        for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = it.next();
            if (desiredLib != null && desiredLib != lib) continue;
            if (lib.isHidden()) continue;
            if (!lib.isChanged()) continue;

			// Abort if database invariants are not valid
			if (!checkedInvariants)
			{
				if (!checkInvariants()) return true;
				checkedInvariants = true;
			}

            // warn about this library
            String theAction = "Save before quitting?";
            if (action == 1) theAction = "Save before closing?"; else
                if (action == 2) theAction = "Save before replacing?";
            String [] options = {"Yes", "No", "Cancel", "No to All"};
            int ret = showFileMenuOptionDialog(TopLevel.getCurrentJFrame(),
                "Library '" + lib.getName() + "' has changed.  " + theAction,
                "Save Library?", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0], null);
            if (ret == 0)
            {
                // save the library
                if (!saveLibraryCommand(lib, FileType.DEFAULTLIB, false, true, false))
                    saveCancelled = true;
                continue;
            }
            if (ret == 1) continue;
            if (ret == 2 || ret == -1) return true;
            if (ret == 3) break;
        }
        if (saveCancelled) return true;
        return false;
    }

    /**
     * Based on JOptionPane but allows ToolTip text
     * @param parentComponent
     * @param message
     * @param title
     * @param optionType
     * @param messageType
     * @param icon
     * @param options
     * @param initialValue
     * @return the return value of the JOptionPane choice.  Returns -1 if aborted
     * @throws HeadlessException
     */
    public static int showFileMenuOptionDialog(Component parentComponent,
        Object message, String title, int optionType, int messageType,
        Icon icon, Object[] options, Object initialValue, String toolTipMessage)
        throws HeadlessException
    {
        JOptionPane pane = new JOptionPane(message, messageType, optionType, icon,
                options, initialValue);

        pane.setInitialValue(initialValue);
        pane.setComponentOrientation(((parentComponent == null) ?
	    JOptionPane.getRootFrame() : parentComponent).getComponentOrientation());

        pane.setMessageType(messageType);
        JDialog dialog = pane.createDialog(parentComponent, title);

        pane.selectInitialValue();
        pane.setToolTipText(toolTipMessage);
        dialog.setVisible(true);
        dialog.dispose();

        Object selectedValue = pane.getValue();

        if(selectedValue == null)
            return JOptionPane.CLOSED_OPTION;
        if(options == null) {
            if(selectedValue instanceof Integer)
                return ((Integer)selectedValue).intValue();
            return JOptionPane.CLOSED_OPTION;
        }
        for(int counter = 0, maxCounter = options.length;
            counter < maxCounter; counter++) {
            if(options[counter].equals(selectedValue))
                return counter;
        }
        return JOptionPane.CLOSED_OPTION;
    }

    static class CellMouseMotionAdapter extends MouseMotionAdapter {
        JOptionPane pane;
        CellMouseMotionAdapter(JOptionPane p) {
            pane = p;}
    public void mouseMoved(MouseEvent e) {
        System.out.println(" Point " + pane.getToolTipLocation(e));}
    }

    /**
     * Unsafe way to force Electric to quit.  If this method returns,
     * it obviously did not kill electric (because of user input or some other reason).
     */
    public static void forceQuit() {
        // check if libraries need to be saved
        boolean dirty = false;
        for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = it.next();
            if (lib.isHidden()) continue;
            if (!lib.isChanged()) continue;
            dirty = true;
            break;
        }
        if (dirty) {
            String [] options = { "Force Save and Quit", "Cancel", "Quit without Saving"};
            int i = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
                 new String [] {"Warning!  Libraries Changed!  Saving changes now may create bad libraries!"},
                    "Force Quit", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    options, options[1]);
            if (i == 0) {
                // force save
                if (!forceSave(false)) {
                    JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
                            "Error during forced library save, not quiting", "Saving Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                ActivityLogger.finished();
                System.exit(1);
            }
            if (i == 1) return;
            if (i == 2) {
                ActivityLogger.finished();
                System.exit(1);
            }
        }
        int i = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(), new String [] {"Warning! You are about to kill Electric!",
            "Do you really want to force quit?"}, "Force Quit", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (i == JOptionPane.YES_OPTION) {
			ActivityLogger.finished();
            System.exit(1);
        }
    }

    /**
     * Force saving of libraries. This does not run in a Job, and could generate corrupt libraries.
     * This saves all libraries to a new directory called "panic" in the current directory.
     * @param confirm true to pop up confirmation dialog, false to just try to save
     * @return true if libraries saved (if they needed saving), false otherwise
     */
    public static boolean forceSave(boolean confirm) {
        if (confirm) {
            String [] options = { "Cancel", "Force Save"};
            int i = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
                 new String [] {"Warning! Saving changes now may create bad libraries!",
                                "Libraries will be saved to \"Panic\" directory in current directory",
                                "Do you really want to force save?"},
                 "Force Save", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                 options, options[0]);
            if (i == 0) return false;
        }
        // try to create the panic directory
        String currentDir = User.getWorkingDirectory();
        System.out.println("Saving libraries in panic directory under " + currentDir); System.out.flush();
        File panicDir = new File(currentDir, "panic");
        if (!panicDir.exists() && !panicDir.mkdir()) {
            JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String [] {"Could not create panic directory",
                 panicDir.getAbsolutePath()}, "Error creating panic directory", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // set libraries to save to panic dir
        Snapshot panicSnapshot = JobManager.findValidSnapshot();
        boolean ok = !Output.writePanicSnapshot(panicSnapshot, panicDir, false);
        if (ok) {
            JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String [] {"Libraries are saved to panic directory",
                 panicDir.getAbsolutePath()}, "Libraries are saved", JOptionPane.INFORMATION_MESSAGE);
        }
        return ok;
    }
    
//    public static boolean forceSave(boolean confirm) {
//        boolean dirty = false;
//        for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
//        {
//            Library lib = it.next();
//            if (lib.isHidden()) continue;
//            if (!lib.isChanged()) continue;
//            dirty = true;
//            break;
//        }
//        if (!dirty) {
//            System.out.println("Libraries have not changed, not saving");
//            return true;
//        }
//        if (confirm) {
//            String [] options = { "Cancel", "Force Save"};
//            int i = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
//                 new String [] {"Warning! Saving changes now may create bad libraries!",
//                                "Libraries will be saved to \"Panic\" directory in current directory",
//                                "Do you really want to force save?"},
//                 "Force Save", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
//                 options, options[0]);
//            if (i == 0) return false;
//        }
//        // try to create the panic directory
//        String currentDir = User.getWorkingDirectory();
//        System.out.println("Saving libraries in panic directory under " + currentDir); System.out.flush();
//        File panicDir = new File(currentDir, "panic");
//        if (!panicDir.exists() && !panicDir.mkdir()) {
//            JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String [] {"Could not create panic directory",
//                 panicDir.getAbsolutePath()}, "Error creating panic directory", JOptionPane.ERROR_MESSAGE);
//            return false;
//        }
//        // set libraries to save to panic dir
//        boolean retValue = true;
//        FileType type = FileType.DEFAULTLIB;
//        for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
//        {
//            Library lib = it.next();
//            if (lib.isHidden()) continue;
//            System.out.print("."); System.out.flush();
//            URL libURL = lib.getLibFile();
//            File newLibFile = null;
//            if (libURL == null || libURL.getPath() == null) {
//                newLibFile = new File(panicDir.getAbsolutePath(), lib.getName()+type.getExtensions()[0]);
//            } else {
//                File libFile = new File(libURL.getPath());
//                String fileName = libFile.getName();
//                if (fileName == null) fileName = lib.getName() + type.getExtensions()[0];
//                newLibFile = new File(panicDir.getAbsolutePath(), fileName);
//            }
//            URL newLibURL = TextUtils.makeURLToFile(newLibFile.getAbsolutePath());
//            lib.setLibFile(newLibURL);
//            boolean error = Output.writeLibrary(lib, type, false, false);
//            if (error) {
//                System.out.println("Error saving "+lib);
//                retValue = false;
//            }
//        }
//        System.out.println(" Libraries saved");
//        return retValue;
//    }
}

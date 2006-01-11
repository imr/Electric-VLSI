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

import com.sun.electric.Main;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.input.GDSMap;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.output.PostScript;
import com.sun.electric.tool.project.Project;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Clipboard;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.ChangeCurrentLib;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ElectricPrinter;
import com.sun.electric.tool.user.ui.TextWindow;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.waveform.HorizRuler;
import com.sun.electric.tool.user.waveform.Panel;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import javax.swing.KeyStroke;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

/**
 * Class to handle the commands in the "File" pulldown menu.
 */
public class FileMenu {


	protected static void addFileMenu(MenuBar menuBar) {
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		/****************************** THE FILE MENU ******************************/

		// mnemonic keys available:    D     J         T  WXYZ
		MenuBar.Menu fileMenu = MenuBar.makeMenu("_File");
        menuBar.add(fileMenu);

		fileMenu.addMenuItem("_New Library...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { newLibraryCommand(); } });
		fileMenu.addMenuItem("_Open Library...", KeyStroke.getKeyStroke('O', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { openLibraryCommand(); } });

		// mnemonic keys available: A    F HIJK  NO QR   VW YZ
		MenuBar.Menu importSubMenu = MenuBar.makeMenu("_Import");
		fileMenu.add(importSubMenu);
		importSubMenu.addMenuItem("_CIF (Caltech Intermediate Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.CIF); } });
		importSubMenu.addMenuItem("_GDS II (Stream)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.GDS); } });
		importSubMenu.addMenuItem("GDS _Map File...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { GDSMap.importMapFile(); } });
		importSubMenu.addMenuItem("_EDIF (Electronic Design Interchange Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.EDIF); } });
		importSubMenu.addMenuItem("_LEF (Library Exchange Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.LEF); } });
		importSubMenu.addMenuItem("_DEF (Design Exchange Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.DEF); } });
		importSubMenu.addMenuItem("D_XF (AutoCAD)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.DXF); } });
		importSubMenu.addMenuItem("S_UE (Schematic User Environment)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.SUE); } });
		if (IOTool.hasDais())
		{
			importSubMenu.addMenuItem("Dais (_Sun CAD)...", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.DAIS); } });
        }
		importSubMenu.addSeparator();
		importSubMenu.addMenuItem("ELI_B...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.ELIB); } });
		importSubMenu.addMenuItem("_Readable Dump...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(FileType.READABLEDUMP); } });
		importSubMenu.addMenuItem("_Text Cell Contents...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { TextWindow.readTextCell(); }});
		importSubMenu.addMenuItem("_Preferences...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Main.getUserInterface().importPrefs(); }});

		fileMenu.addSeparator();

		fileMenu.addMenuItem("_Close Library", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { closeLibraryCommand(Library.getCurrent()); } });
		fileMenu.addMenuItem("Sa_ve Library", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { if (checkInvariants()) saveLibraryCommand(Library.getCurrent(), FileType.DEFAULTLIB, false, true); } });
		fileMenu.addMenuItem("Save Library _As...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { if (checkInvariants()) saveAsLibraryCommand(Library.getCurrent()); } });
		fileMenu.addMenuItem("_Save All Libraries", KeyStroke.getKeyStroke('S', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { if (checkInvariants()) saveAllLibrariesCommand(); } });
        fileMenu.addMenuItem("Save All Libraries in _Format...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { if (checkInvariants()) saveAllLibrariesInFormatCommand(); } });

		// mnemonic keys available:    D     J  M   Q   UVW YZ
		MenuBar.Menu exportSubMenu = MenuBar.makeMenu("_Export");
		fileMenu.add(exportSubMenu);
		exportSubMenu.addMenuItem("_CIF (Caltech Intermediate Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.CIF, false); } });
		exportSubMenu.addMenuItem("_GDS II (Stream)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.GDS, false); } });
		exportSubMenu.addMenuItem("ED_IF (Electronic Design Interchange Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.EDIF, false); } });
		exportSubMenu.addMenuItem("LE_F (Library Exchange Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.LEF, false); } });
		exportSubMenu.addMenuItem("_L...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.L, false); } });
		if (IOTool.hasSkill()) {
			exportSubMenu.addMenuItem("S_kill (Cadence Commands)...", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.SKILL, false); } });
            exportSubMenu.addMenuItem("Skill Exports _Only (Cadence Commands)...", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.SKILLEXPORTSONLY, false); } });
        }
		exportSubMenu.addSeparator();
		exportSubMenu.addMenuItem("_Eagle...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.EAGLE, false); } });
		exportSubMenu.addMenuItem("EC_AD...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.ECAD, false); } });
		exportSubMenu.addMenuItem("Pad_s...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.PADS, false); } });
		exportSubMenu.addSeparator();
		exportSubMenu.addMenuItem("_Text Cell Contents...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { TextWindow.writeTextCell(); }});
		exportSubMenu.addMenuItem("_PostScript...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.POSTSCRIPT, false); } });
	    exportSubMenu.addMenuItem("P_NG (Portable Network Graphics)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.PNG, false); } });
		exportSubMenu.addMenuItem("_HPGL...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.HPGL, false); } });
		exportSubMenu.addMenuItem("D_XF (AutoCAD)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCommand(FileType.DXF, false); } });
		exportSubMenu.addSeparator();
		exportSubMenu.addMenuItem("ELI_B (Version 6)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { if (checkInvariants()) saveLibraryCommand(Library.getCurrent(), FileType.ELIB, true, false); } });
		exportSubMenu.addMenuItem("P_references...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Main.getUserInterface().exportPrefs(); }});

		fileMenu.addSeparator();

		fileMenu.addMenuItem("Change Current _Library...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ChangeCurrentLib.showDialog(); } });
		fileMenu.addMenuItem("List Li_braries", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.listLibrariesCommand(); } });
		fileMenu.addMenuItem("Rena_me Library...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.renameLibrary(Library.getCurrent()); } });
		fileMenu.addMenuItem("Mar_k All Libraries for Saving", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChangeJobs.markAllLibrariesForSavingCommand(); } });

		// mnemonic keys available: AB DEFGHIJKLMNOPQ STUVWXYZ
		MenuBar.Menu checkSubMenu = MenuBar.makeMenu("C_heck Libraries");
		fileMenu.add(checkSubMenu);
		checkSubMenu.addMenuItem("_Check", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.checkAndRepairCommand(false); } });
		checkSubMenu.addMenuItem("_Repair", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.checkAndRepairCommand(true); } });

        fileMenu.addSeparator();

        // mnemonic keys available:   C EFG  JK MN PQ ST VWXYZ
		MenuBar.Menu projectSubMenu = MenuBar.makeMenu("Project Management");
		fileMenu.add(projectSubMenu);
		projectSubMenu.addMenuItem("_Update", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Project.updateProject(); } });
		projectSubMenu.addMenuItem("Check _Out This Cell", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Project.checkOutThisCell(); } });
		projectSubMenu.addMenuItem("Check _In This Cell...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Project.checkInThisCell(); } });
		projectSubMenu.addSeparator();
		projectSubMenu.addMenuItem("Roll_back and Release Check-Out", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Project.cancelCheckOutThisCell(); } });
		projectSubMenu.addMenuItem("_Add This Cell", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Project.addThisCell(); } });
		projectSubMenu.addMenuItem("_Remove This Cell", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Project.removeThisCell(); } });
		projectSubMenu.addMenuItem("Show _History of This Cell...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Project.examineThisHistory(); } });
		projectSubMenu.addSeparator();
		projectSubMenu.addMenuItem("Get Library From Repository...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Project.getALibrary(); } });
		projectSubMenu.addMenuItem("Add Current _Library To Repository", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Project.addThisLibrary(); } });
		projectSubMenu.addMenuItem("Ad_d All Libraries To Repository", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Project.addAllLibraries(); } });

        fileMenu.addSeparator();

        fileMenu.addMenuItem("Pa_ge Setup...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { pageSetupCommand(); } });
		fileMenu.addMenuItem("_Print...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { printCommand(); } });

		fileMenu.addSeparator();
		fileMenu.addMenuItem("P_references...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { PreferencesFrame.preferencesCommand(); } });

		if (TopLevel.getOperatingSystem() != TopLevel.OS.MACINTOSH)
		{
			fileMenu.addMenuItem("_Quit", KeyStroke.getKeyStroke('Q', buckyBit),
				new ActionListener() { public void actionPerformed(ActionEvent e) { quitCommand(); } });
		}
        fileMenu.addMenuItem("Force Q_uit (and Save)", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { forceQuit(); } });

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
            EditWindow.repaintAll();
            TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
            System.out.println("New "+lib+" created");
            return true;
        }
    }

    /**
     * This method implements the command to read a library.
     * It is interactive, and pops up a dialog box.
     */
    public static void openLibraryCommand()
    {
        String fileName = OpenFile.chooseInputFile(FileType.LIBRARYFORMATS, null);
        //String fileName = OpenFile.chooseInputFile(OpenFile.Type.DEFAULTLIB, null);
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
			ReadLibrary job = new ReadLibrary(fileURL, type, deleteLib);
        }
    }

    /** Get the type from the fileName, or if no valid Library type found, return defaultType.
     */
    public static FileType getLibraryFormat(String fileName, FileType defaultType) {
        if (fileName != null)
        {
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

		public ReadLibrary(URL fileURL, FileType type, Library deleteLib)
		{
			super("Read External Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
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
				if (Library.getVisibleLibraries().size() > 1)
				{
					if (!deleteLib.kill("replace")) return false;
					deleteLib = null;
				} else
				{
					// cannot delete last library: must delete it later
					// mangle the name so that the new one can be created
					deleteLib.setName("FORMERVERSIONOF" + deleteLib.getName());
				}
			}
			openALibrary(fileURL, type);
			if (deleteLib != null)
				deleteLib.kill("replace");
			return true;
		}
	}

	/**
	 * Class to read initial libraries into Electric.
	 */
	public static class ReadInitialELIBs extends Job
    {
        List<URL> fileURLs;

        public ReadInitialELIBs(List<URL> fileURLs) {
            super("Read Initial Libraries", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.fileURLs = fileURLs;
            startJob();
        }

        public boolean doIt() throws JobException {
            // open no name library first
            Library mainLib = Library.newInstance("noname", null);
            if (mainLib == null) return false;
            mainLib.setCurrent();

            // try to open initial libraries
            boolean success = false;
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
                    URL f = TextUtils.makeURLToFile(fileName + "." + FileType.READABLEDUMP.getExtensions()[0]);
                    if (TextUtils.URLExists(f, null)) {
                        defType = FileType.READABLEDUMP;
                        file = f;
                    }
                }
                if (defType == null) defType = FileType.DEFAULTLIB;
                User.setWorkingDirectory(TextUtils.getFilePath(file));
                if (openALibrary(file, defType))
                    success = true;
            }
            if (success) {
                // close no name library
                //mainLib.kill();
                // the calls to repaint actually cause the
                // EditWindow to come up BLANK in Linux SDI mode
                //EditWindow.repaintAll();
                //EditWindow.repaintAllContents();
            }
            return true;
        }
    }

    /** Opens a library */
    private static boolean openALibrary(URL fileURL, FileType type)
    {
    	Library lib = null;
    	if (type == FileType.ELIB || type == FileType.JELIB || type == FileType.READABLEDUMP)
        {
    		lib = LibraryFiles.readLibrary(fileURL, null, type, false);
        } else
        {
    		lib = Input.importLibrary(fileURL, type);
        }
        if (lib != null)
        {
            // new library open: check for default "noname" library and close if empty
            Library noname = Library.findLibrary("noname");
            if (noname != null)
            {
                if (!noname.getCells().hasNext())
                {
                    noname.kill("delete");
                }
            }
        }
        // Repair libraries in case default width changes due to foundry changes
        new Technology.ResetDefaultWidthJob(lib);
        Undo.noUndoAllowed();
        if (lib == null) return false;
        lib.setCurrent();
        Cell cell = lib.getCurCell();
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
                WindowFrame.wantToOpenCurrentLibrary(true);
            }});
        return true;
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
                    wf.setCellWindow(cell);
                    //wf.requestFocus();
                    TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
                    return;
                }
            }
            WindowFrame.createEditWindow(cell);

            // no clean for now.
            TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
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
			ReadLibrary job = new ReadLibrary(fileURL, type, deleteLib);
		}
	}

    public static void closeLibraryCommand(Library lib)
    {
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
	            WindowFrame.wantToRedoTitleNames();
	            EditWindow.repaintAll();

	            // Disable save icon if no more libraries are open
	            TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
            }
            return true;
        }
    }

    /**
     * This method implements the command to save a library.
     * It is interactive, and pops up a dialog box.
     * @param lib the Library to save.
     * @param type the format of the library (OpenFile.Type.ELIB, OpenFile.Type.READABLEDUMP, or OpenFile.Type.JELIB).
     * @param compatibleWith6 true to write a library that is compatible with version 6 Electric.
     * @return true if library saved, false otherwise.
     */
    public static boolean saveLibraryCommand(Library lib, FileType type, boolean compatibleWith6, boolean forceToType)
    {
        String [] extensions = type.getExtensions();
        String extension = extensions[0];
        String fileName = null;
        if (lib.isFromDisk())
        {
        	if (type == FileType.JELIB ||
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
            // mark for saving, all libraries that depend on this
            for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
            {
                Library oLib = it.next();
                if (oLib.isHidden()) continue;
                if (oLib == lib) continue;
                if (oLib.isChangedMajor()) continue;

                // see if any cells in this library reference the renamed one
                if (oLib.referencesLib(lib))
                    oLib.setChangedMajor();
            }
        }
        SaveLibrary job = new SaveLibrary(lib, fileName, type, compatibleWith6, false);
        return true;
    }

    /**
     * Class to save a library in a new thread.
     * For a non-interactive script, use SaveLibrary job = new SaveLibrary(filename).
     * Saves as an elib.
     */
    public static class SaveLibrary extends Job
    {
        Library lib;
        String newName;
        FileType type;
        boolean compatibleWith6;

        public SaveLibrary(Library lib, String newName, FileType type, boolean compatibleWith6, boolean batchJob)
        {
            super("Write "+lib, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.lib = lib;
            this.newName = newName;
            this.type = type;
            this.compatibleWith6 = compatibleWith6;
            if (!batchJob)
                startJob();
        }

        public boolean doIt() throws JobException
        {
            boolean retVal = false;
            try {
                retVal = performTask();
                if (!retVal) {
                    JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String [] {"Error saving files",
                         "Please check your disk libraries"}, "Saving Failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String [] {"Exception caught when saving files",
                     e.getMessage(),
                     "Please check your disk libraries"}, "Saving Failed", JOptionPane.ERROR_MESSAGE);
                ActivityLogger.logException(e);
            }
            return retVal;
        }

        public boolean performTask() {
            // rename the library if requested
            if (newName != null)
            {
                URL libURL = TextUtils.makeURLToFile(newName);
                lib.setLibFile(libURL);
                lib.setName(TextUtils.getFileNameWithoutExtension(libURL));
            }

            boolean error = Output.writeLibrary(lib, type, compatibleWith6, false);
            if (error) return false;
            return true;
        }
    }

    /**
     * This method implements the command to save a library to a different file.
     * It is interactive, and pops up a dialog box.
     */
    public static void saveAsLibraryCommand(Library lib)
    {
        lib.clearFromDisk();
        saveLibraryCommand(lib, FileType.DEFAULTLIB, false, true);
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
			type = (FileType)libsToSave.get(lib);
            if (!saveLibraryCommand(lib, type, compatibleWith6, forceToType))
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
        Object[] formats = {FileType.JELIB, FileType.ELIB, FileType.READABLEDUMP};
        Object format = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(),
                "Output file format for all libraries:", "Save All Libraries In Format...",
                JOptionPane.PLAIN_MESSAGE,
                null, formats, FileType.DEFAULTLIB);
        if (format == null) return; // cancel operation
        FileType outType = (FileType)format;
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
    }

    /**
     * This method checks database invariants.
     * @return true if database is valid or user forced saving.
     */
	private static boolean checkInvariants()
	{
		// check database invariants
		if (!Library.checkInvariants())
		{
			String [] messages = {
				"Database invariants are not valid",
				"You may use \"Force Quit (and Save)\" to save in a panic directory",
				"Do you really want to write libraries into the working directory?"};
            Object [] options = {"Continue Writing", "Cancel", "ForceQuit" };
            int val = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(), messages,
				"Invalid database invariants", JOptionPane.DEFAULT_OPTION,
				JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (val == 1) return false;
			if (val == 2)
			{
				forceQuit();
				return false;
			}
		}
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

            Output.exportCellCommand(cell, context, filePath, type, true, override);
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

        Output.exportCellCommand(cell, context, filePath, type, true, override);
    }

    private static class ExportImage extends Job
	{
		String filePath;
		WindowContent wnd;

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

 		ElectricPrinter ep = new ElectricPrinter(context, pageFormat);
		pj.setPrintable(ep, pageFormat);
		return (ep);
	}

    /**
     * This method implements the command to print the current window.
     */
    public static void printCommand()
    {
    	WindowFrame wf = WindowFrame.getCurrentWindowFrame();
    	if (wf == null)
    	{
    		System.out.println("No current window to print");
    		return;
    	}

        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setJobName(wf.getTitle());
	    ElectricPrinter ep = getOutputPreferences(wf.getContent(), pj);

        // see if a default printer should be mentioned
        String pName = IOTool.getPrinterName();
        PrintService [] printers = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService printerToUse = null;
        for(int i=0; i<printers.length; i++)
        {
            if (pName.equals(printers[i].getName()))
            {
                printerToUse = printers[i];
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

			// resize the window if this is a WaveformWindow
			Dimension oldSize = null;
			if (wf.getContent() instanceof WaveformWindow)
			{
				int iw = (int)pageFormat.getImageableWidth();
				int ih = (int)pageFormat.getImageableHeight();
				oldSize = overall.getSize();
				overall.setSize(iw, ih);
				overall.validate();
				overall.repaint();
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
            QuitJob job = new QuitJob();
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
     * Class to quit Electric in a new thread.
     */
	private static class QuitJob extends Job
    {
        public QuitJob()
        {
            super("Quitting", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt() throws JobException
        {
            try {
                Library.saveExpandStatus();
            } catch (BackingStoreException e) {
                int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
			            "Cannot save cell expand status. Do you still want to quit?", "Cell Status Error",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response != JOptionPane.YES_OPTION) return false;
            }

			// save changes to layer visibility
			Layer.preserveVisibility();

			// save changes to waveform window signals
			WaveformWindow.preserveSignalOrder();

			ActivityLogger.finished();
            System.exit(0);
            return true;
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
            String how = "significantly";
            String toolTipMessage = "Major changes include creation, deletion, or modification of circuit\n" +
                    "elements including variable and export renaming.";
            if (!lib.isChangedMajor())
            {
                how = "insignificantly";
                toolTipMessage = "Minor changes include DRC dates, expanded flag.";
            }

            String theAction = "Save before quitting?";
            if (action == 1) theAction = "Save before closing?"; else
                if (action == 2) theAction = "Save before replacing?";
            String [] options = {"Yes", "No", "Cancel", "No to All"};
            int ret = showFileMenuOptionDialog(TopLevel.getCurrentJFrame(),
                "Library '" + lib.getName() + "' has changed " + how + ".  " + theAction,
                "Save Library?", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0], toolTipMessage);
            if (ret == 0)
            {
                // save the library
                if (!saveLibraryCommand(lib, FileType.DEFAULTLIB, false, true))
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
                System.exit(1);
            }
            if (i == 1) return;
            if (i == 2) System.exit(1);
        }
        int i = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(), new String [] {"Warning! You are about to kill Electric!",
            "Do you really want to force quit?"}, "Force Quit", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (i == JOptionPane.YES_OPTION) {
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
        boolean dirty = false;
        for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = it.next();
            if (lib.isHidden()) continue;
            if (!lib.isChanged()) continue;
            dirty = true;
            break;
        }
        if (!dirty) {
            System.out.println("Libraries have not changed, not saving");
            return true;
        }
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
        boolean retValue = true;
        FileType type = FileType.DEFAULTLIB;
        for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = it.next();
            if (lib.isHidden()) continue;
            System.out.print("."); System.out.flush();
            URL libURL = lib.getLibFile();
            File newLibFile = null;
            if (libURL.getPath() == null) {
                newLibFile = new File(panicDir.getAbsolutePath(), lib.getName()+type.getExtensions()[0]);
            } else {
                File libFile = new File(libURL.getPath());
                String fileName = libFile.getName();
                if (fileName == null) fileName = lib.getName() + type.getExtensions()[0];
                newLibFile = new File(panicDir.getAbsolutePath(), fileName);
            }
            URL newLibURL = TextUtils.makeURLToFile(newLibFile.getAbsolutePath());
            lib.setLibFile(newLibURL);
            boolean error = Output.writeLibrary(lib, type, false, false);
            if (error) {
                System.out.println("Error saving "+lib);
                retValue = false;
            }
        }
        System.out.println(" Libraries saved");
        return retValue;
    }
}

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

import com.sun.electric.tool.user.ui.*;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.output.PostScript;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.variable.VarContext;

import javax.swing.*;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.*;
import java.awt.List;
import java.awt.print.PrinterJob;
import java.awt.print.PrinterException;
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.*;
import java.io.File;

/**
 * Class to handle the commands in the "File" pulldown menu.
 */
public class FileMenu {


    protected static void addFileMenu(MenuBar menuBar) {
        MenuBar.MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		/****************************** THE FILE MENU ******************************/

		MenuBar.Menu fileMenu = new MenuBar.Menu("File", 'F');
        menuBar.add(fileMenu);

		fileMenu.addMenuItem("New Library...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { newLibraryCommand(); } });
		fileMenu.addMenuItem("Open Library...", KeyStroke.getKeyStroke('O', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { openLibraryCommand(); } });

		MenuBar.Menu importSubMenu = new MenuBar.Menu("Import");
		fileMenu.add(importSubMenu);
		importSubMenu.addMenuItem("Version 7 ELIB...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(OpenFile.Type.ELIB); } });
		importSubMenu.addMenuItem("Readable Dump...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(OpenFile.Type.READABLEDUMP); } });
		importSubMenu.addMenuItem("Text Cell Contents...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { TextWindow.readTextCell(); }});

		fileMenu.addSeparator();

		fileMenu.addMenuItem("Close Library", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { closeLibraryCommand(Library.getCurrent()); } });
		fileMenu.addMenuItem("Save Library", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(Library.getCurrent(), OpenFile.Type.DEFAULTLIB, false, true); } });
		fileMenu.addMenuItem("Save Library as...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveAsLibraryCommand(Library.getCurrent()); } });
		fileMenu.addMenuItem("Save All Libraries", KeyStroke.getKeyStroke('S', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveAllLibrariesCommand(); } });

		MenuBar.Menu exportSubMenu = new MenuBar.Menu("Export");
		fileMenu.add(exportSubMenu);
		exportSubMenu.addMenuItem("CIF (Caltech Intermediate Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.CIF, false); } });
		exportSubMenu.addMenuItem("GDS II (Stream)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.GDS, false); } });
		exportSubMenu.addMenuItem("EDIF (Electronic Design Interchange Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.EDIF, false); } });
		exportSubMenu.addMenuItem("LEF (Library Exchange Format)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.LEF, false); } });
		if (IOTool.hasSkill())
			exportSubMenu.addMenuItem("Skill (Cadence Commands)...", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.SKILL, false); } });
		exportSubMenu.addSeparator();
		exportSubMenu.addMenuItem("Eagle...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.EAGLE, false); } });
		exportSubMenu.addMenuItem("ECAD...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.ECAD, false); } });
		exportSubMenu.addMenuItem("Pads...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.PADS, false); } });
		exportSubMenu.addSeparator();
		exportSubMenu.addMenuItem("Text Cell Contents...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { TextWindow.writeTextCell(); }});
		exportSubMenu.addMenuItem("PostScript...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.POSTSCRIPT, false); } });
		exportSubMenu.addMenuItem("DXF (AutoCAD)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.DXF, false); } });
		exportSubMenu.addMenuItem("L...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.L, false); } });
		exportSubMenu.addSeparator();
		exportSubMenu.addMenuItem("ELIB (Version 6)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(Library.getCurrent(), OpenFile.Type.ELIB, true, false); } });
		exportSubMenu.addMenuItem("ELIB (Version 7)...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(Library.getCurrent(), OpenFile.Type.ELIB, false, false); } });
		exportSubMenu.addMenuItem("Readable Dump...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(Library.getCurrent(), OpenFile.Type.READABLEDUMP, false, false); } });

		fileMenu.addSeparator();

		fileMenu.addMenuItem("Change Current Library...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.changeCurrentLibraryCommand(); } });
		fileMenu.addMenuItem("List Libraries", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.listLibrariesCommand(); } });
		fileMenu.addMenuItem("Rename Library...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.renameLibrary(Library.getCurrent()); } });
		fileMenu.addMenuItem("Mark All Libraries for Saving", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.markAllLibrariesForSavingCommand(); } });
		fileMenu.addMenuItem("Repair Libraries", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.checkAndRepairCommand(); } });

        fileMenu.addSeparator();

        fileMenu.addMenuItem("Page Setup...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { pageSetupCommand(); } });
		fileMenu.addMenuItem("Print...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { printCommand(); } });

		fileMenu.addSeparator();
		fileMenu.addMenuItem("Preferences...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { PreferencesFrame.preferencesCommand(); } });

		if (TopLevel.getOperatingSystem() != TopLevel.OS.MACINTOSH)
		{
			fileMenu.addMenuItem("Quit", KeyStroke.getKeyStroke('Q', buckyBit),
				new ActionListener() { public void actionPerformed(ActionEvent e) { quitCommand(); } });
		}
        fileMenu.addMenuItem("Force Quit (and Save)", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { forceQuit(); } });

    }

    // ------------------------------ File Menu -----------------------------------

    public static void newLibraryCommand()
    {
        String newLibName = JOptionPane.showInputDialog("New Library Name", "");
        if (newLibName == null) return;
        NewLibrary job = new NewLibrary(newLibName);
    }

    public static class NewLibrary extends Job {
        private String newLibName;

        public NewLibrary(String newLibName) {
            super("New Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.newLibName = newLibName;
            startJob();
        }

        public boolean doIt()
        {
            Library lib = Library.newInstance(newLibName, null);
            if (lib == null) return false;
            lib.setCurrent();
            //WindowFrame.wantToRedoLibraryTree();
            EditWindow.repaintAll();
            TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
            System.out.println("New Library '"+lib.getName()+"' created");
            return true;
        }
    }

    /**
     * This method implements the command to read a library.
     * It is interactive, and pops up a dialog box.
     */
    public static void openLibraryCommand()
    {
        String fileName = OpenFile.chooseInputFile(OpenFile.Type.DEFAULTLIB, null);
        if (fileName != null)
        {
            // start a job to do the input
            URL fileURL = TextUtils.makeURLToFile(fileName);
			ReadLibrary job = new ReadLibrary(fileURL, OpenFile.Type.DEFAULTLIB);
        }
    }

	/**
	 * Class to read a text library in a new thread.
	 * For a non-interactive script, use ReadLibrary job = new ReadLibrary(filename, format).
	 */
	public static class ReadLibrary extends Job
	{
		private URL fileURL;
		private OpenFile.Type type;

		public ReadLibrary(URL fileURL, OpenFile.Type type)
		{
			super("Read External Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fileURL = fileURL;
			this.type = type;
			startJob();
		}

		public boolean doIt()
		{
			openALibrary(fileURL, type);
			return true;
		}
	}

    public static class ReadInitialELIBs extends Job
    {
        java.util.List fileURLs;

        public ReadInitialELIBs(java.util.List fileURLs) {
            super("Read Initial Libraries", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.fileURLs = fileURLs;
            startJob();
        }

        public boolean doIt() {
            // open no name library first
            Library mainLib = Library.newInstance("noname", null);
            if (mainLib == null) return false;
            mainLib.setCurrent();

            // try to open initial libraries
            boolean success = false;
            for (Iterator it = fileURLs.iterator(); it.hasNext(); ) {
                URL file = (URL)it.next();
                if (openALibrary(file, OpenFile.Type.DEFAULTLIB)) success = true;
            }
            if (success) {
                // close no name library
                //mainLib.kill();
                //WindowFrame.wantToRedoLibraryTree();
                // the calls to repaint actually cause the
                // EditWindow to come up BLANK in Linux SDI mode
                //EditWindow.repaintAll();
                //EditWindow.repaintAllContents();
            }
            return true;
        }
    }

    /** Opens a library */
    private static boolean openALibrary(URL fileURL, OpenFile.Type type)
    {
        Library lib = Input.readLibrary(fileURL, type);
        if (lib != null)
        {
            // new library open: check for default "noname" library and close if empty
            Library noname = Library.findLibrary("noname");
            if (noname != null)
            {
                if (!noname.getCells().hasNext())
                {
                    noname.kill();
                }
            }
        }
        Undo.noUndoAllowed();
        if (lib == null) return false;
        lib.setCurrent();
        Cell cell = lib.getCurCell();
        if (cell == null) System.out.println("No current cell in this library"); else
        {
            CreateCellWindow creator = new CreateCellWindow(cell);
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(creator);
            } else {
                creator.run();
            }
        }
        return true;

    }

    public static class CreateCellWindow implements Runnable {
        private Cell cell;
        public CreateCellWindow(Cell cell) { this.cell = cell; }
        public void run() {
            // check if edit window open with null cell, use that one if exists
            for (Iterator it = WindowFrame.getWindows(); it.hasNext(); )
            {
                WindowFrame wf = (WindowFrame)it.next();
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
	public static void importLibraryCommand(OpenFile.Type type)
	{
		String fileName = OpenFile.chooseInputFile(type, null);
		if (fileName != null)
		{
			// start a job to do the input
			URL fileURL = TextUtils.makeURLToFile(fileName);
			ReadLibrary job = new ReadLibrary(fileURL, type);
		}
	}

    public static void closeLibraryCommand(Library lib)
    {
	    Set found = Library.findReferenceInCell(lib);
	    // You can't close it because there are open cells that refer to library elements
	    if (found.size() != 0)
	    {
		    System.out.println("Cannot close library '" + lib.getName() + "':");
		    System.out.print("\t Cells ");

		    for (Iterator i = found.iterator(); i.hasNext();)
		    {
			   Cell cell = (Cell)i.next();
			   System.out.print("'" + cell.getName() + "'(" + cell.getLibrary().getName() + ") ");
		    }
		    System.out.println("refer to it.");
		    return;
	    }

        int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(), "Are you sure you want to close library " + lib.getName() + "?");
        if (response != JOptionPane.YES_OPTION) return;
        String libName = lib.getName();
        WindowFrame.removeLibraryReferences(lib);
        CloseLibrary job = new CloseLibrary(lib);
    }

    public static class CloseLibrary extends Job {
        Library lib;

        public CloseLibrary(Library lib) {
            super("Close Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.lib = lib;
            startJob();
        }

        public boolean doIt() {
            if (lib.kill())
                System.out.println("Library " + lib.getName() + " closed");
            WindowFrame.wantToRedoTitleNames();
            //WindowFrame.wantToRedoLibraryTree();
            EditWindow.repaintAll();
            // Disable save icon if no more libraries are open
            TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
            return true;
        }
    }

    /**
     * This method implements the command to save a library.
     * It is interactive, and pops up a dialog box.
     * @param lib the Library to save.
     * @param type the format of the library (OpenFile.Type.ELIB for binary; OpenFile.Type.READABLEDUMP for text).
     * @param compatibleWith6 true to write a library that is compatible with version 6 Electric.
     * @return true if library saved, false otherwise.
     */
    public static boolean saveLibraryCommand(Library lib, OpenFile.Type type, boolean compatibleWith6, boolean forceToType)
    {
        String [] extensions = type.getExtensions();
        String extension = extensions[0];
        String fileName = null;
        if (lib.isFromDisk())
        {
        	if (type == OpenFile.Type.JELIB ||
        		(type == OpenFile.Type.ELIB && !compatibleWith6))
	        {
	            fileName = lib.getLibFile().getPath();
	            if (forceToType)
	            {
					if (fileName.endsWith(".elib")) type = OpenFile.Type.ELIB; else
					if (fileName.endsWith(".jelib")) type = OpenFile.Type.JELIB; else
					if (fileName.endsWith(".txt")) type = OpenFile.Type.READABLEDUMP;
	            }
	        }
        }
        if (fileName == null)
        {
            fileName = OpenFile.chooseOutputFile(type, null, lib.getName() + "." + extension);
            if (fileName == null) return false;

            int dotPos = fileName.lastIndexOf('.');
            if (dotPos < 0) fileName += "." + extension; else
            {
                if (!fileName.substring(dotPos+1).equals(extension))
                {
                    fileName = fileName.substring(0, dotPos) + "." + extension;
                }
            }
        }
        SaveLibrary job = new SaveLibrary(lib, fileName, type, compatibleWith6);
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
        OpenFile.Type type;
        boolean compatibleWith6;

        public SaveLibrary(Library lib, String newName, OpenFile.Type type, boolean compatibleWith6)
        {
            super("Write Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.lib = lib;
            this.newName = newName;
            this.type = type;
            this.compatibleWith6 = compatibleWith6;
            startJob();
        }

        public boolean doIt()
        {
            boolean retVal = false;
            try {
                retVal = _doIt();
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

        private boolean _doIt() {
            // rename the library if requested
            if (newName != null)
            {
                URL libURL = TextUtils.makeURLToFile(newName);
                lib.setLibFile(libURL);
                lib.setName(TextUtils.getFileNameWithoutExtension(libURL));
            }

            boolean error = Output.writeLibrary(lib, type, compatibleWith6);
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
        saveLibraryCommand(lib, OpenFile.Type.DEFAULTLIB, false, true);
        WindowFrame.wantToRedoTitleNames();
    }

    /**
     * This method implements the command to save all libraries.
     */
    public static void saveAllLibrariesCommand()
    {
        for(Iterator it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = (Library)it.next();
            if (lib.isHidden()) continue;
            if (!lib.isChangedMajor() && !lib.isChangedMinor()) continue;
            if (!saveLibraryCommand(lib, OpenFile.Type.DEFAULTLIB, false, true)) break;
        }
    }

    /**
     * This method implements the export cell command for different export types.
     * It is interactive, and pops up a dialog box.
     */
    public static void exportCellCommand(OpenFile.Type type, boolean isNetlist)
    {
        if (type == OpenFile.Type.POSTSCRIPT)
        {
            if (PostScript.syncAll()) return;
        }
        EditWindow wnd = EditWindow.needCurrent();

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
        VarContext context = wnd.getVarContext();

        String [] extensions = type.getExtensions();
        String filePath = cell.getName() + "." + extensions[0];

        // special case for spice
        if (type == OpenFile.Type.SPICE &&
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

            exportCellCommand(cell, context, filePath, type);
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

        exportCellCommand(cell, context, filePath, type);
    }

    /**
     * This is the non-interactive version of exportCellCommand
     */
    public static void exportCellCommand(Cell cell, VarContext context, String filePath, OpenFile.Type type)
    {
        ExportCell job = new ExportCell(cell, context, filePath, type);
    }

    /**
     * Class to export a cell in a new thread.
     * For a non-interactive script, use
     * ExportCell job = new ExportCell(Cell cell, String filename, Output.ExportType type).
     * Saves as an elib.
     */
    private static class ExportCell extends Job
    {
        Cell cell;
        VarContext context;
        String filePath;
        OpenFile.Type type;

        public ExportCell(Cell cell, VarContext context, String filePath, OpenFile.Type type)
        {
            super("Export "+cell.describe()+" ("+type+")", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.context = context;
            this.filePath = filePath;
            this.type = type;
            startJob();
        }

        public boolean doIt()
        {
            Output.writeCell(cell, context, filePath, type);
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
 		if (pageFormat == null)
			pageFormat = pj.defaultPage();

		ElectricPrinter ep = new ElectricPrinter();
		ep.setPrintWindow(wf);
        pj.setPrintable(ep, pageFormat);

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

        if (pj.printDialog())
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
			SwingUtilities.invokeLater(new PrintJobAWT(wf, pj, oldSize));
        }
    }

	private static class PrintJobAWT implements Runnable
	{
		private WindowFrame wf;
		private PrinterJob pj;
		private Dimension oldSize;

		PrintJobAWT(WindowFrame wf, PrinterJob pj, Dimension oldSize)
		{
			this.wf = wf;
			this.pj = pj;
			this.oldSize = oldSize;
		}

		public void run()
		{
			try {
				pj.print();
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

    private static class ElectricPrinter implements Printable
    {
    	private WindowFrame wf;
        private Image img = null;

        public void setPrintWindow(WindowFrame wf) {
            this.wf = wf;
        }

        public int print(Graphics g, PageFormat pageFormat, int page)
            throws java.awt.print.PrinterException
        {
            if (page != 0) return Printable.NO_SUCH_PAGE;

			// handle different window types
			if (wf.getContent() instanceof EditWindow)
			{
				EditWindow wnd = (EditWindow)wf.getContent();
				VarContext context = wnd.getVarContext();
				Cell printCell = wnd.getCell();
				if (printCell == null) return Printable.NO_SUCH_PAGE;

	            // create an EditWindow for rendering this cell
	            if (img == null)
	            {
	                EditWindow w = EditWindow.CreateElectricDoc(null, null);
	                int iw = (int)pageFormat.getImageableWidth();
	                int ih = (int)pageFormat.getImageableHeight();
	                w.setScreenSize(new Dimension(iw, ih));
	                w.setCell(printCell, context);
	                PixelDrawing offscreen = w.getOffscreen();
	                offscreen.setBackgroundColor(Color.WHITE);
	                offscreen.drawImage(null);
	                img = offscreen.getImage();
	            }

	            // copy the image to the page
	            int ix = (int)pageFormat.getImageableX();
	            int iy = (int)pageFormat.getImageableY();
	            g.drawImage(img, ix, iy, null);
	            return Printable.PAGE_EXISTS;
			} else if (wf.getContent() instanceof WaveformWindow)
			{
				WaveformWindow ww = (WaveformWindow)wf.getContent();
				Graphics2D g2d = (Graphics2D)g;
				g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
				JPanel printArea = wf.getContent().getPanel();
				printArea.paint(g);
				return Printable.PAGE_EXISTS;
			}

			// can't handle this type of window
			return Printable.NO_SUCH_PAGE;
        }
    }

    /**
     * This method implements the command to quit Electric.
     */
    public static boolean quitCommand()
    {
        if (preventLoss(null, 0)) return (false);
        QuitJob job = new QuitJob();
        return (true);
    }

    /**
     * Class to quit Electric in a new thread.
     */
    private static class QuitJob extends Job
    {
        public QuitJob()
        {
            super("Quitting", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt()
        {
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
        boolean saveCancelled = false;
        for(Iterator it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = (Library)it.next();
            if (desiredLib != null && desiredLib != lib) continue;
            if (lib.isHidden()) continue;
            if (!lib.isChangedMajor() && !lib.isChangedMinor()) continue;

            // warn about this library
            String how = "significantly";
            if (!lib.isChangedMajor()) how = "insignificantly";

            String theAction = "Save before quitting?";
            if (action == 1) theAction = "Save before closing?"; else
                if (action == 2) theAction = "Save before replacing?";
            String [] options = {"Yes", "No", "Cancel", "No to All"};
            int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
                "Library " + lib.getName() + " has changed " + how + ".  " + theAction,
                "Save Library?", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
            if (ret == 0)
            {
                // save the library
                if (!saveLibraryCommand(lib, OpenFile.Type.DEFAULTLIB, false, true))
                    saveCancelled = true;
                continue;
            }
            if (ret == 1) continue;
            if (ret == 2) return true;
            if (ret == 3) break;
        }
        if (saveCancelled) return true;
        return false;
    }

    /**
     * Unsafe way to force Electric to quit.  If this method returns,
     * it obviously did not kill electric (because of user input or some other reason).
     */
    public static void forceQuit() {
        // check if libraries need to be saved
        boolean dirty = false;
        for(Iterator it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = (Library)it.next();
            if (lib.isHidden()) continue;
            if (!lib.isChangedMajor() && !lib.isChangedMinor()) continue;
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
        for(Iterator it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = (Library)it.next();
            if (lib.isHidden()) continue;
            if (!lib.isChangedMajor() && !lib.isChangedMinor()) continue;
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
        File panicDir = new File(currentDir, "panic");
        if (!panicDir.exists() && !panicDir.mkdir()) {
            JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String [] {"Could not create panic directory",
                 panicDir.getAbsolutePath()}, "Error creating panic directory", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // set libraries to save to panic dir
        boolean retValue = true;
        OpenFile.Type type = OpenFile.Type.DEFAULTLIB;
        for(Iterator it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = (Library)it.next();
            if (lib.isHidden()) continue;
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
            boolean error = Output.writeLibrary(lib, type, false);
            if (error) {
                System.out.println("Error saving library "+lib.getName());
                retValue = false;
            }
        }
        return retValue;
    }
}

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
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Jun 23, 2004
 * Time: 11:36:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class FileMenu {


    protected static void addFileMenu(MenuBar menuBar) {
        MenuBar.MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		/****************************** THE FILE MENU ******************************/

		MenuBar.Menu fileMenu = new MenuBar.Menu("File", 'F');
        menuBar.add(fileMenu);

		fileMenu.addMenuItem("New Library", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { newLibraryCommand(); } });
		fileMenu.addMenuItem("Open Library", KeyStroke.getKeyStroke('O', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { openLibraryCommand(); } });

		MenuBar.Menu importSubMenu = new MenuBar.Menu("Import");
		fileMenu.add(importSubMenu);
		importSubMenu.addMenuItem("Readable Dump", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(); } });
		importSubMenu.addMenuItem("Text Cell Contents...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { TextWindow.readTextCell(); }});

		fileMenu.addSeparator();

		fileMenu.addMenuItem("Close Library", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { closeLibraryCommand(Library.getCurrent()); } });
		fileMenu.addMenuItem("Save Library", KeyStroke.getKeyStroke('S', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(Library.getCurrent(), OpenFile.Type.ELIB, false); } });
		fileMenu.addMenuItem("Save Library as...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveAsLibraryCommand(); } });
		fileMenu.addMenuItem("Save All Libraries",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveAllLibrariesCommand(); } });

		MenuBar.Menu exportSubMenu = new MenuBar.Menu("Export");
		fileMenu.add(exportSubMenu);
		exportSubMenu.addMenuItem("CIF (Caltech Intermediate Format)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.CIF, false); } });
		exportSubMenu.addMenuItem("GDS II (Stream)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.GDS, false); } });
		exportSubMenu.addMenuItem("PostScript", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.POSTSCRIPT, false); } });
		exportSubMenu.addMenuItem("Text Cell Contents...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { TextWindow.writeTextCell(); }});
		exportSubMenu.addMenuItem("Readable Dump", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(Library.getCurrent(), OpenFile.Type.READABLEDUMP, false); } });
		exportSubMenu.addMenuItem("Version 6 ELIB", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(Library.getCurrent(), OpenFile.Type.ELIB, true); } });

		fileMenu.addSeparator();

		fileMenu.addMenuItem("Change Current Library...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.changeCurrentLibraryCommand(); } });
		fileMenu.addMenuItem("List Libraries", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.listLibrariesCommand(); } });
		fileMenu.addMenuItem("Rename Library...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.renameLibraryCommand(); } });
		fileMenu.addMenuItem("Mark All Libraries for Saving", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.markAllLibrariesForSavingCommand(); } });
		fileMenu.addMenuItem("Repair Libraries", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.checkAndRepairCommand(); } });

        fileMenu.addSeparator();

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

    }

    // ------------------------------ File Menu -----------------------------------

    public static void newLibraryCommand()
    {
        String newLibName = JOptionPane.showInputDialog("New Library Name", "");
        if (newLibName == null) return;
        Library lib = Library.newInstance(newLibName, null);
        if (lib == null) return;
        lib.setCurrent();
        WindowFrame.wantToRedoLibraryTree();
        EditWindow.repaintAll();
        TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
    }

    /**
     * This method implements the command to read a library.
     * It is interactive, and pops up a dialog box.
     */
    public static void openLibraryCommand()
    {
        String fileName = OpenFile.chooseInputFile(OpenFile.Type.ELIB, null);
        if (fileName != null)
        {
            // start a job to do the input
            URL fileURL = TextUtils.makeURLToFile(fileName);
            ReadELIB job = new ReadELIB(fileURL);
        }
    }

    /**
     * Class to read a library in a new thread.
     * For a non-interactive script, use ReadELIB job = new ReadELIB(filename).
     */
    public static class ReadELIB extends Job
    {
        URL fileURL;

        public ReadELIB(URL fileURL)
        {
            super("Read Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.fileURL = fileURL;
            startJob();
        }

        public boolean doIt()
        {
            return openALibrary(fileURL);
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
            WindowFrame window1 = WindowFrame.createEditWindow(null);

            // try to open initial libraries
            boolean success = false;
            for (Iterator it = fileURLs.iterator(); it.hasNext(); ) {
                URL file = (URL)it.next();
                if (openALibrary(file)) success = true;
            }
            if (success) {
                // close no name library
                mainLib.kill();
                WindowFrame.wantToRedoLibraryTree();
                EditWindow.repaintAllContents();
            }
            return true;
        }
    }

    /** Opens a library */
    private static boolean openALibrary(URL fileURL) {
        Library lib = Input.readLibrary(fileURL, OpenFile.Type.ELIB);
        Undo.noUndoAllowed();
        if (lib == null) return false;
        lib.setCurrent();
        Cell cell = lib.getCurCell();
        if (cell == null)
            System.out.println("No current cell in this library");
        else
        {
            // check if edit window open with null cell, use that one if exists
            for (Iterator it = WindowFrame.getWindows(); it.hasNext(); )
            {
                WindowFrame wf = (WindowFrame)it.next();
                WindowContent content = wf.getContent();
                if (content.getCell() == null)
                {
                    wf.setCellWindow(cell);
                    WindowFrame.setCurrentWindowFrame(wf);
                    TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
                    return true;
                }
            }
            WindowFrame.createEditWindow(cell);
            // no clean for now.
            TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
        }
        return true;

    }

    /**
     * This method implements the command to import a library (Readable Dump format).
     * It is interactive, and pops up a dialog box.
     */
    public static void importLibraryCommand()
    {
        String fileName = OpenFile.chooseInputFile(OpenFile.Type.READABLEDUMP, null);
        if (fileName != null)
        {
            // start a job to do the input
            URL fileURL = TextUtils.makeURLToFile(fileName);
            ReadTextLibrary job = new ReadTextLibrary(fileURL);
        }
    }

    /**
     * Class to read a text library in a new thread.
     * For a non-interactive script, use ReadTextLibrary job = new ReadTextLibrary(filename).
     */
    private static class ReadTextLibrary extends Job
    {
        URL fileURL;
        protected ReadTextLibrary(URL fileURL)
        {
            super("Read Text Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.fileURL = fileURL;
            startJob();
        }

        public boolean doIt()
        {
            Library lib = Input.readLibrary(fileURL, OpenFile.Type.READABLEDUMP);
            Undo.noUndoAllowed();
            if (lib == null) return false;
            lib.setCurrent();
            Cell cell = lib.getCurCell();
            if (cell == null) System.out.println("No current cell in this library"); else
            {
                // check if edit window open with null cell, use that one if exists
                for (Iterator it = WindowFrame.getWindows(); it.hasNext(); )
                {
                    WindowFrame wf = (WindowFrame)it.next();
                    WindowContent content = wf.getContent();
                    if (content instanceof EditWindow)
                    {
                        if (content.getCell() == null)
                        {
                            content.setCell(cell, VarContext.globalContext);
                            return true;
                        }
                    }
                }
                WindowFrame.createEditWindow(cell);
            }
            return true;
        }
    }

    public static void closeLibraryCommand(Library lib)
    {
        int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(), "Are you sure you want to close library " + lib.getName() + "?");
        if (response != JOptionPane.YES_OPTION) return;
        String libName = lib.getName();
        WindowFrame.removeLibraryReferences(lib);
        if (lib.kill())
            System.out.println("Library " + libName + " closed");
        WindowFrame.wantToRedoTitleNames();
        WindowFrame.wantToRedoLibraryTree();
        EditWindow.repaintAll();
        // Disable save icon if no more libraries are open
        TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
    }

    /**
     * This method implements the command to save a library.
     * It is interactive, and pops up a dialog box.
     * @param lib the Library to save.
     * @param type the format of the library (OpenFile.Type.ELIB for binary; OpenFile.Type.READABLEDUMP for text).
     * @param compatibleWith6 true to write a library that is compatible with version 6 Electric.
     * @return true if library saved, false otherwise.
     */
    public static boolean saveLibraryCommand(Library lib, OpenFile.Type type, boolean compatibleWith6)
    {
        String [] extensions = type.getExtensions();
        String extension = extensions[0];
        String fileName;
        if (lib.isFromDisk() && type == OpenFile.Type.ELIB && !compatibleWith6)
        {
            fileName = lib.getLibFile().getPath();
        } else
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
            // rename the library if requested
            if (newName != null)
            {
                URL libURL = TextUtils.makeURLToFile(newName);
                lib.setLibFile(libURL);
                lib.setName(TextUtils.getFileNameWithoutExtension(libURL));
            }

            boolean error = Output.writeLibrary(lib, type, compatibleWith6);
            if (error)
            {
                System.out.println("Error writing the library file");
            }
            return true;
        }
    }

    /**
     * This method implements the command to save a library to a different file.
     * It is interactive, and pops up a dialog box.
     */
    public static void saveAsLibraryCommand()
    {
        Library lib = Library.getCurrent();
        lib.clearFromDisk();
        saveLibraryCommand(lib, OpenFile.Type.ELIB, false);
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
            if (!saveLibraryCommand(lib, OpenFile.Type.ELIB, false)) break;
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
                !Simulation.getSpiceRunChoice().equals(Simulation.spiceRunChoiceDontRun)) {

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

    /**
     * This method implements the command to print the current window.
     */
    public static void printCommand()
    {
        Cell printCell = WindowFrame.needCurCell();
        if (printCell == null) return;

        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setJobName("Cell "+printCell.describe());
        ElectricPrinter ep = new ElectricPrinter();
        ep.setPrintCell(printCell);
        pj.setPrintable(ep);

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
            printerToUse = pj.getPrintService();
            if (printerToUse != null)
				IOTool.setPrinterName(printerToUse.getName());
            PrintJob job = new PrintJob(printCell, pj);
        }
    }

    /**
     * Class to print a cell in a new thread.
     */
    private static class PrintJob extends Job
    {
        Cell cell;
        PrinterJob pj;

        public PrintJob(Cell cell, PrinterJob pj)
        {
            super("Print "+cell.describe(), User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.pj = pj;
            startJob();
        }

        public boolean doIt()
        {
            try {
                pj.print();
            } catch (PrinterException pe)
            {
                System.out.println("Print aborted.");
            }
            return true;
        }
    }

    private static class ElectricPrinter implements Printable
    {
        private Cell printCell;
        private Image img = null;

        public void setPrintCell(Cell printCell) { this.printCell = printCell; }

        public int print(Graphics g, PageFormat pageFormat, int page)
            throws java.awt.print.PrinterException
        {
            if (page != 0) return Printable.NO_SUCH_PAGE;

            // create an EditWindow for rendering this cell
            if (img == null)
            {
                EditWindow w = EditWindow.CreateElectricDoc(null, null);
                int iw = (int)pageFormat.getImageableWidth();
                int ih = (int)pageFormat.getImageableHeight();
                w.setScreenSize(new Dimension(iw, ih));
                w.setCell(printCell, VarContext.globalContext);
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
            super("Quitting", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt()
        {
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
                if (!saveLibraryCommand(lib, OpenFile.Type.ELIB, false))
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


}

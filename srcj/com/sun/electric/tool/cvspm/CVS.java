/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CVSMenu.java
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


package com.sun.electric.tool.cvspm;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.Pref;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Exec;
import com.sun.electric.tool.user.dialogs.ModalCommandDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.DELIB;
import com.sun.electric.tool.Job;

import javax.swing.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 10, 2006
 * Time: 11:47:01 AM
 * To change this template use File | Settings | File Templates.
 */

/**
 * Only one CVS command can be working at a time.  While CVS is running,
 * the GUI will be tied up.  This is to
 */
public class CVS {

    public static boolean isEnabled() { return false; }

    public static void checkoutFromRepository() {
        // get list of modules in repository
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        runModalCVSCommand("-n checkout -c", "Getting modules in repository...", User.getWorkingDirectory(), out);

        // this must come after the runModal command
        LineNumberReader result = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));

        List<String> modules = new ArrayList<String>();
        for (;;) {
            String line = null;
            try {
                line = result.readLine();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                return;
            }
            if (line == null) break;
            line = line.trim();
            if (line.equals("")) continue;

            String[] parts = line.split("\\s");
            modules.add(parts[0]);
        }
        if (modules.size() == 0) {
            System.out.println("No modules in CVS!");
            return;
        }
        Object ret = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Choose Module to Checkout",
                "Checkout Module...", JOptionPane.QUESTION_MESSAGE, null, modules.toArray(), modules.toArray()[0]);
        if (ret == null) {
            // user cancelled
            return;
        }
        String module = (String)ret;
        // choose directory to checkout to
        String directory = OpenFile.chooseDirectory("Choose directory in which to checkout module "+module);
        if (directory == null) {
            // user cancelled
            return;
        }
        // checkout module to current working directory
        String cmd = "checkout "+module;
        runModalCVSCommand(cmd, "Checking out '"+module+"' to "+directory, directory, System.out);
        System.out.println("Checked out '"+module+"' to '"+directory+"'");
    }

    // ------------------------------------------------------------------------

    /**
     * This will run a CVS command in-thread; i.e. the current thread will
     * block until the CVS command completes.
     * @param cmd the command to run
     * @param comment the message to display on the dialog
     * @param workingDir the directory in which to run the CVS command
     * (null for current working directory). I recommend you specify
     * this as the current library dir.
     * @param out where the result of the command gets printed. May be
     * a ByteArrayOutputStream for storing it, or just System.out for
     * printing it.
     */
    public static void runCVSCommand(String cmd, String comment, String workingDir, OutputStream out) {
        String run = getCVSProgram() + " -d"+getRepository()+" "+cmd;

        System.out.println(comment);
        Exec e = new Exec(run, null, new File(workingDir), out, out);
        e.run();
    }

    /**
     * This will run a CVS command in a separate Thread and block the GUI until the command
     * completes, or until the user hits 'cancel', which will try to
     * terminate the external command.  This method returns after
     * the cvs command has completed.
     * @param cmd the command to run
     * @param comment the message to display on the dialog
     * @param workingDir the directory in which to run the CVS command
     * (null for current working directory). I recommend you specify
     * this as the current library dir.
     * @param out where the result of the command gets printed. May be
     * a ByteArrayOutputStream for storing it, or just System.out for
     * printing it.
     */
    public static void runModalCVSCommand(String cmd, String comment, String workingDir, OutputStream out) {
        String run = getCVSProgram() + " -d"+getRepository()+" "+cmd;

        Exec e = new Exec(run, null, new File(workingDir), out, out);
        // add a listener to get rid of the modal dialog when the command finishes
        String message = "Running: "+run;
        JFrame frame = TopLevel.getCurrentJFrame();
        ModalCommandDialog dialog = new ModalCommandDialog(frame, true, e, message, comment);
        dialog.setVisible(true);
    }

    // -------------------------- Utility Commands --------------------------

    public static void testModal() {
        runModalCVSCommand("-n history -c -a", "testing command", User.getWorkingDirectory(), System.out);
    }

    /**
     * Get the file for the given Cell, assuming the library is
     * in DELIB format.  If not, returns null.
     * @param cell
     * @return
     */
    public static File getCellFile(Cell cell) {
        if (isDELIB(cell.getLibrary())) {
            String relativeFile = DELIB.getCellFile(cell.backup());
            URL libFile = cell.getLibrary().getLibFile();
            return new File(libFile.getFile(), relativeFile);
        }
        return null;
    }

    public static boolean isDELIB(Library lib) {
        URL libFile = lib.getLibFile();
        FileType type = OpenFile.getOpenFileType(libFile.getFile(), FileType.JELIB);
        return (type == FileType.DELIB);
    }

    /**
     * Returns true if this file has is being maintained from a
     * CVS respository, returns false otherwise.
     */
    public static boolean isFileInCVS(File fd) {
        // get directory file is in
        if (fd == null) return false;
        File parent = fd.getParentFile();
        File CVSDIR = new File(parent, "CVS");
        if (!CVSDIR.exists()) return false;
        File entries = new File(CVSDIR, "Entries");
        if (!entries.exists()) return false;
        // make sure file is mentioned in Entries file
        String filename = fd.getName();
        try {
            FileReader fr = new FileReader(entries);
            LineNumberReader reader = new LineNumberReader(fr);
            for (;;) {
                String line = reader.readLine();
                if (line == null) break;
                if (line.equals("")) continue;
                String parts[] = line.split("/");
                if (parts.length >= 2 && parts[1].equals(filename)) return true;
            }

        } catch (IOException e) {
        }
        return false;
    }

    /**
     * Used by commands that require the library to be in sync with the disk.
     * @param lib
     * @param dialog true to pop up a dialog to tell the user, false to not do so.
     * @return true if not modified, false if modified
     */
    public static boolean assertNotModified(Library lib, String cmd, boolean dialog) {
        if (lib.isChanged()) {
            if (dialog) {
                Job.getUserInterface().showErrorMessage("Library "+lib.getName()+" must be saved to run CVS "+cmd, "CVS "+cmd);
            } else {
                System.out.println("Library "+lib.getName()+" must be saved to run CVS "+cmd);
            }
            return false;
        }
        return true;
    }

    /**
     * Used by commands that require the library to be in sync with the disk.
     * @param cell
     * @param dialog true to pop up a dialog to tell the user, false to not do so.
     * @return true if not modified, false if modified
     */
    public static boolean assertNotModified(Cell cell, String cmd, boolean dialog) {
        if (cell.isModified(true)) {
            if (dialog) {
                Job.getUserInterface().showErrorMessage("Cell "+cell.getName()+" must be saved to run CVS "+cmd, "CVS "+cmd);
            } else {
                System.out.println("Cell "+cell.getName()+" must be saved to run CVS "+cmd);
            }
            return false;
        }
        return true;
    }

    // ------------------- Preferences --------------------

    /**
     * Get the repository.  In the future, there may be some
     * dialog to let the user choose between multiple respositories.
     * @return
     */
    private static String getRepository() {
        return getCVSRepository();
    }

    private static Pref cacheCVSProgram = Pref.makeStringPref("CVS Program", User.getUserTool().prefs, "cvspm");
    public static String getCVSProgram() { return cacheCVSProgram.getString(); }
    public static void setCVSProgram(String s) { cacheCVSProgram.setString(s); }

    private static Pref cacheCVSRepository = Pref.makeStringPref("CVS Repository", User.getUserTool().prefs, "");
    public static String getCVSRepository() { return cacheCVSRepository.getString(); }
    public static void setCVSRepository(String s) { cacheCVSRepository.setString(s); }
}

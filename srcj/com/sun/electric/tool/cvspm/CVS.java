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
import com.sun.electric.tool.user.ui.TopLevel;

import javax.swing.*;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

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

    public static void checkoutFromRepository() {

    }

    // -------------- Updating -----------------

    public static void updateAllLibraries() {

    }

    public static void updateLibrary(Library lib) {

    }

    public static void updateCell(Cell cell) {

    }

    // -------------- Adding ---------------------

    public static void addAllLibraries() {

    }

    public static void addLibrary(Library lib) {

    }

    public static void addCell(Cell cell) {
        // first make sure that library is in Repository

    }

    // -------------- Committing -----------------

    public static void commitAllLibraries() {

    }

    public static void commitLibrary(Library lib) {

    }

    public static void commitCell(Cell cell) {

    }

    public static String getCommitMessage() {

        return "";
    }

    // ---------------- Editing ------------------

    public static void editLibrary(Library lib) {

    }

    public static void editCell(Cell cell) {

    }

    public static void uneditLibrary(Library lib) {

    }

    public static void uneditCell(Cell cell) {

    }

    // -------------------------------------------

    /**
     * This will run a CVS command and block the GUI until the command
     * completes, or until the user hits 'cancel', which will try to
     * terminate the external command.
     * @param cmd
     * @param out where the result of the command gets printed. May be
     * a ByteArrayOutputStream for storing it, or just System.out for
     * printing it.
     */
    public static void runModalCVSCommand(String cmd, OutputStream out) {
        String run = getCVSProgram() + " -d"+getRepository()+" "+cmd;

        //run = "sleep 4";
        Exec e = new Exec(run, null, new File(User.getWorkingDirectory()), out, out);
        // add a listener to get rid of the modal dialog when the command finishes
        String message = "Running: "+run;
        JFrame frame = TopLevel.getCurrentJFrame();
        ModalCommandDialog dialog = new ModalCommandDialog(frame, true, e, message);
        dialog.setVisible(true);
    }

    public static void testModal() {
        runModalCVSCommand("-n history -c -a", System.out);
    }


    /**
     * Use this to capture the output of a CVS command
     */
    public abstract class CVSExecListener implements Exec.FinishedListener {
        ByteArrayOutputStream out;
        ByteArrayOutputStream err;
        public CVSExecListener() {
            out = new ByteArrayOutputStream();
            err = new ByteArrayOutputStream();
        }
        public ByteArrayOutputStream getOut() { return out; }
        public ByteArrayOutputStream getErr() { return err; }
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

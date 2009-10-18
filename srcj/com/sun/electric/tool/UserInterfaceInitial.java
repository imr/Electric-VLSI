/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UserInterfaceInitial.java
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.ErrorLogger;

/**
 *
 */
public class UserInterfaceInitial  implements UserInterface {
    private final EDatabase database;
    public UserInterfaceInitial(EDatabase database) {
        this.database = database;
    }
    public Job.Key getJobKey() { return new Job.Key(0, 0, false); }
    public EDatabase getDatabase() { return database; }
    public Technology getCurrentTechnology() { return null; }
    public Library getCurrentLibrary() { return null; }
    public EditWindow_ getCurrentEditWindow_() { throw new UnsupportedOperationException(); }
    public EditWindow_ needCurrentEditWindow_() { throw new UnsupportedOperationException(); }
    public Cell getCurrentCell() { return null; }
    public Cell needCurrentCell() { throw new UnsupportedOperationException(); }
    public void adjustReferencePoint(Cell cell, double cX, double cY) { throw new UnsupportedOperationException(); }
    public void repaintAllWindows() { throw new UnsupportedOperationException(); }
    public int getDefaultTextSize() { throw new UnsupportedOperationException(); }
    public EditWindow_ displayCell(Cell cell) { throw new UnsupportedOperationException(); }
    public void termLogging(final ErrorLogger logger, boolean explain, boolean terminate) { throw new UnsupportedOperationException(); }
    public String reportLog(ErrorLogger.MessageLog log, boolean showhigh, boolean separateWindow, int position) { throw new UnsupportedOperationException(); }
    public void showErrorMessage(String message, String title) { throw new UnsupportedOperationException(); }
    public void showInformationMessage(String message, String title) { throw new UnsupportedOperationException(); }
    public void printMessage(String message, boolean newLine) {
        if (Job.currentUI != null)
            Job.currentUI.printMessage(message, newLine);
        else if (newLine)
            System.err.println(message);
        else
            System.err.print(message);
    }
    public void saveMessages(String filePath) { throw new UnsupportedOperationException(); }
    public void beep() { throw new UnsupportedOperationException(); }
    public boolean confirmMessage(Object message) { throw new UnsupportedOperationException(); }
    public int askForChoice(String message, String title, String [] choices, String defaultChoice) { throw new UnsupportedOperationException(); }
    public String askForInput(Object message, String title, String def) { throw new UnsupportedOperationException(); }
    public void startProgressDialog(String msg, String filePath) { throw new UnsupportedOperationException(); }
    public void stopProgressDialog() { throw new UnsupportedOperationException(); }
    public void setProgressValue(int pct) { throw new UnsupportedOperationException(); }
    public void setProgressNote(String message) { throw new UnsupportedOperationException(); }
    public String getProgressNote() { throw new UnsupportedOperationException(); }
}

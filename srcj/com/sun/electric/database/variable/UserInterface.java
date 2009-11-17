/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UserInterface.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.variable;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger;

/**
 * This interface provides information from the user interface.
 */
public interface UserInterface {

    /**
     * Method to return Job Key of a currently executed Job.
     * Jobless context (Gui) is represented by a Job Key with jobId=0.
     */
    public Job.Key getJobKey();

    /**
     * Method to return the current database object.
     * @return the current database (null if none).
     */
    public EDatabase getDatabase();

    /**
     * Method to return the current Technology.
     * @return the current database (null if none).
     */
    public Technology getCurrentTechnology();

    /**
     * Method to return the current Library.
     * @return the current Library.
     */
    public Library getCurrentLibrary();

    /**
     * Method to return the current "EditWindow_" object.
     * @return the current "EditWindow_" object (null if none).
     */
    public EditWindow_ getCurrentEditWindow_();

    /**
     * Method to demand the current "EditWindow_" object.
     * If none exists, an error message is displayed.
     * @return the current "EditWindow_" object (null if none).
     */
    public EditWindow_ needCurrentEditWindow_();

    /**
     * Method to return the current Cell in the current Library.
     * @return the current Cell (null if none).
     */
    public Cell getCurrentCell();

    /**
     * Method to demand the current Cell.
     * If none exists, an error message is displayed.
     * @return the current Cell (null if none).
     */
    public Cell needCurrentCell();

    /**
     * Method to adjust reference point in WindowFrame containing the cell
     */
    public void adjustReferencePoint(Cell cell, double cX, double cY);

    /**
     * Method to request that all windows be redisplayed including palettes.
     */
    public void repaintAllWindows();

    /**
     * Method to return the height of default text (in points).
     * @return the height of default text (in points).
     */
    public int getDefaultTextSize();

    /**
     * Method to request that a Cell be displayed in a new window.
     * @param cell the Cell to be displayed.
     * @return the EditWindow_ object created to show the Cell.
     */
    public EditWindow_ displayCell(Cell cell);

    // ErrorLogger related functions
    public void termLogging(final ErrorLogger logger, boolean explain, boolean terminate);

    /**
     * Method to return the error message associated with the current error.
     * Highlights associated graphics if "showhigh" is nonzero.
     */
    public String reportLog(ErrorLogger.MessageLog log, boolean showhigh, boolean separateWindow, int position);

    /**
     * Method to show an error message.
     * @param message the error message to show.
     * @param title the title of a dialog with the error message.
     */
    public void showErrorMessage(String message, String title);

    /**
     * Method to show an informational message.
     * @param message the message to show.
     * @param title the title of a dialog with the message.
     */
    public void showInformationMessage(String message, String title);

    /**
     * Method print a message.
     * @param message the message to show.
     * @param newLine add new line after the message
     */
    public void printMessage(String message, boolean newLine);

    /**
     * Method to start saving messages.
     * @param filePath file to save
     */
    public void saveMessages(String filePath);

    /**
     * Method to beep.
     */
    public void beep();

    /**
     * Method to show a message and ask for confirmation.
     * @param message the message to show.
     * @return true if "yes" was selected, false if "no" was selected.
     */
    public boolean confirmMessage(Object message);

    /**
     * Method to ask for a choice among possibilities.
     * @param message the message to show.
     * @param title the title of the dialog with the query.
     * @param choices an array of choices to present, each in a button.
     * @param defaultChoice the default choice.
     * @return the index into the choices array that was selected.
     */
    public int askForChoice(String message, String title, String[] choices, String defaultChoice);

    /**
     * Method to ask for a line of text.
     * @param message the prompt message.
     * @param title the title of a dialog with the message.
     * @param def the default response.
     * @return the string (null if cancelled).
     */
    public String askForInput(Object message, String title, String def);

    /**
     * Method to start the display of a progress dialog.
     * @param msg the message to show in the progress dialog.
     * @param filePath the file being read (null if not reading a file).
     */
    public void startProgressDialog(String msg, String filePath);

    /**
     * Method to stop the progress bar
     */
    public void stopProgressDialog();

    /**
     * Method to update the progress bar
     * @param pct the percentage done (from 0 to 100).
     */
    public void setProgressValue(int pct);

    /**
     * Method to set a text message in the progress dialog.
     * @param message the new progress message.
     */
    public void setProgressNote(String message);

    /**
     * Method to get text message in the progress dialgo.
     * @return the current progress message.
     */
    public String getProgressNote();
}

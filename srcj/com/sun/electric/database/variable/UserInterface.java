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
package com.sun.electric.database.variable;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.Point2D;

/**
 * This interface provides information from the user interface.
 */
public interface UserInterface
{
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
	 * Method to get the current Cell in a given Library.
	 * @param lib the library to query.
	 * @return the current Cell in the Library.
	 * @return the current cell in the library; null if there is no current Cell.
	 */
	public Cell getCurrentCell(Library lib);

	/**
	 * Method to demand the current Cell.
	 * If none exists, an error message is displayed.
	 * @return the current Cell (null if none).
	 */
	public Cell needCurrentCell();

	/**
	 * Method to set the current Cell in a Library.
	 * @param lib the library in which to set a current cell.
	 * @param curCell the new current Cell in the Library (can be null).
	 */
	public void setCurrentCell(Library lib, Cell curCell);

    /**
     * Method to adjust reference point in WindowFrame containing the cell
     */
    public void adjustReferencePoint(Cell cell, double cX, double cY);

	/**
	 * Method to request that all windows be redisplayed.
	 */
	public void repaintAllEditWindows();

    /**
     * Method to request the refresh of palette and layers tabs
     * according to the new technology selected
     */
    public void loadComponentMenuForTechnology();

	/**
	 * Method to align a database coordinate with the current grid.
	 * @param pt the database coordinate.  It's value is adjusted.
	 */
	public void alignToGrid(Point2D pt);

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
     * Highlights associated graphics if "showhigh" is nonzero.  Fills "g1" and "g2"
     * with associated geometry modules (if nonzero).
     */
    public String reportLog(ErrorLogger.MessageLog log, boolean showhigh, Geometric [] gPair);

    /**
     * Method to show an error message.
     * @param message the error message to show.
     * @param title the title of a dialog with the error message.
     */
    public void showErrorMessage(Object message, String title);

    /**
     * Method to show an informational message.
     * @param message the message to show.
     * @param title the title of a dialog with the message.
     */
    public void showInformationMessage(Object message, String title);

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
    public int askForChoice(String message, String title, String [] choices, String defaultChoice);

    /**
     * Method to ask for a line of text.
     * @param message the prompt message.
     * @param title the title of a dialog with the message.
     * @param def the default response.
     * @return the string (null if cancelled).
     */
    public String askForInput(Object message, String title, String def);

    /** For Pref */
	/**
	 * Method to import the preferences from an XML file.
	 * Prompts the user and reads the file.
	 */
    public void importPrefs();

    /**
	 * Method to export the preferences to an XML file.
	 * Prompts the user and writes the file.
	 */
	public void exportPrefs();

    /**
     * Method to start progress bar
     */
    public void startProgressDialog(String type, String filePath);

    /**
     * Method to stop the progress bar
     */
    public void stopProgressDialog();

    /**
     * Method to update the progress bar
     */
    public void setProgressValue(long pct);

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

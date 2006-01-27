/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UserInterfaceMain.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.text.Pref;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.ui.*;
import com.sun.electric.tool.user.dialogs.OptionReconcile;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Class to build the UserInterface for the main GUI version of the user interface.
 */
public class UserInterfaceMain implements UserInterface
{
	public EditWindow_ getCurrentEditWindow_() { return EditWindow.getCurrent(); }

	public EditWindow_ needCurrentEditWindow_() { return EditWindow.needCurrent(); }

	public Cell getCurrentCell() { return WindowFrame.getCurrentCell(); }

	public Cell needCurrentCell() { return WindowFrame.needCurCell(); }

    /**
     * Method to adjust reference point in WindowFrame containing the cell
     */
    public void adjustReferencePoint(Cell theCell, double cX, double cY)
    {
        // adjust all windows showing this cell
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow_)) continue;
			Cell cell = content.getCell();
			if (cell != theCell) continue;
			EditWindow_ wnd = (EditWindow_)content;
			Point2D off = wnd.getOffset();
			off.setLocation(off.getX()-cX, off.getY()-cY);
			wnd.setOffset(off);
		}
    }

	public void repaintAllEditWindows() { EditWindow.repaintAllContents(); }

	public void alignToGrid(Point2D pt) { EditWindow.gridAlign(pt); }

	public int getDefaultTextSize() { return EditWindow.getDefaultFontSize(); }

    /** Related to ExplorerTree */
    public void wantToRedoErrorTree() { WindowFrame.wantToRedoErrorTree(); }

	public EditWindow_ displayCell(Cell cell)
	{
		WindowFrame wf = WindowFrame.createEditWindow(cell);
		if (wf.getContent() instanceof EditWindow_) return (EditWindow_)wf.getContent();
		return null;
	}

    // ErrorLogger functions
    public void termLogging(final ErrorLogger logger, boolean explain)
    {
        if (logger.getNumLogs() > 0 && explain)
        {
//            if (!alreadyExplained)
            {
//				alreadyExplained = true;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        // To print consistent message in message window
                        String extraMsg = "errors/warnings";
                        if (logger.getNumErrors() == 0) extraMsg = "warnings";
                        else  if (logger.getNumWarnings() == 0) extraMsg = "errors";
                        String msg = logger.getInfo();
                        System.out.println(msg);
                        if (logger.getNumLogs() > 0)
                        {
                            System.out.println("Type > and < to step through " + extraMsg + ", or open the ERRORS view in the explorer");
                        }
                        if (logger.getNumErrors() > 0 && !Job.BATCHMODE)
                        {
                            JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), msg,
                                logger.getSystem() + " finished with Errors", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                });
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {WindowFrame.wantToRedoErrorTree(); }
        });
    }

    /**
     * Method to return the error message associated with the current error.
     * Highlights associated graphics if "showhigh" is nonzero.  Fills "g1" and "g2"
     * with associated geometry modules (if nonzero).
     */
    public String reportLog(ErrorLogger.MessageLog log, boolean showhigh, Geometric [] gPair)
    {
        // if two highlights are requested, find them
        if (gPair != null)
        {
            Geometric geom1 = null, geom2 = null;
            for(Iterator<ErrorHighlight> it = log.getHighlights(); it.hasNext(); )
            {
                ErrorHighlight eh = it.next();
                    if (geom1 == null) geom1 = (Geometric)eh.getObject();
                    else if (geom2 == null) geom2 = (Geometric)eh.getObject();
            }

            // return geometry if requested
            if (geom1 != null) gPair[0] = geom1;
            if (geom2 != null) gPair[1] = geom2;
        }

        // show the error
        if (showhigh)
        {
            Highlighter highlighter = null;
            EditWindow wnd = null;

            // first show the geometry associated with this error
            for(Iterator<ErrorHighlight> it = log.getHighlights(); it.hasNext(); )
            {
                ErrorHighlight eh = it.next();

                Cell cell = eh.getCell();
                // validate the cell (it may have been deleted)
                if (cell != null)
                {
                    if (!cell.isLinked())
                    {
                        return "(cell deleted): " + log.getMessageString();
                    }

                    // make sure it is shown
                    boolean found = false;
                    for(Iterator<WindowFrame> it2 = WindowFrame.getWindows(); it2.hasNext(); )
                    {
                        WindowFrame wf = (WindowFrame)it2.next();
                        WindowContent content = wf.getContent();
                        if (!(content instanceof EditWindow)) continue;
                        wnd = (EditWindow)content;
                        if (wnd.getCell() == cell)
                        {
                            if (((eh.getVarContext() != null) && eh.getVarContext().equals(wnd.getVarContext())) ||
                                    (eh.getVarContext() == null)) {
                                // already displayed.  should force window "wf" to front? yes
                                wf.getFrame().toFront();
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found)
                    {
                        // make a new window for the cell
                        WindowFrame wf = WindowFrame.createEditWindow(cell);
                        wnd = (EditWindow)wf.getContent();
                        wnd.setCell(eh.getCell(), eh.getVarContext());
                    }
                    if (highlighter == null) {
                        highlighter = wnd.getHighlighter();
                        highlighter.clear();
                    }
                }

                if (highlighter == null) continue;

                eh.addToHighlighter(highlighter);
            }

            if (highlighter != null)
            {
                highlighter.ensureHighlightingSeen();
                highlighter.finished();

                // make sure the selection is visible
                Rectangle2D hBounds = highlighter.getHighlightedArea(wnd);
                Rectangle2D shown = wnd.getDisplayedBounds();
                if (!shown.intersects(hBounds))
                {
                    wnd.focusOnHighlighted();
                }
            }
        }

        // return the error message
        return log.getMessageString();
    }

    /**
     * Method to show an error message.
     * @param message the error message to show.
     * @param title the title of a dialog with the error message.
     */
    public void showErrorMessage(Object message, String title)
    {
		JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Method to show an informational message.
     * @param message the message to show.
     * @param title the title of a dialog with the message.
     */
    public void showInformationMessage(Object message, String title)
    {
		JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Method to show a message and ask for confirmation.
     * @param message the message to show.
     * @return true if "yes" was selected, false if "no" was selected.
     */
    public boolean confirmMessage(Object message)
    {
		int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(), message);
		return response == JOptionPane.YES_OPTION;
    }

    /**
     * Method to ask for a choice among possibilities.
     * @param message the message to show.
     * @param title the title of the dialog with the query.
     * @param choices an array of choices to present, each in a button.
     * @param defaultChoice the default choice.
     * @return the index into the choices array that was selected.
     */
    public int askForChoice(Object message, String title, String [] choices, String defaultChoice)
    {
	    int val = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(), message, title,
	    	JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, choices, defaultChoice);
	    return val;
    }

    /**
     * Method to ask for a line of text.
     * @param message the prompt message.
     * @param title the title of a dialog with the message.
     * @param def the default response.
     * @return the string (null if cancelled).
     */
    public String askForInput(Object message, String title, String def)
    {
    	return JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), message, title, JOptionPane.QUESTION_MESSAGE, null, null, def).toString();
    }

    /** For Pref */
    public void restoreSavedBindings(boolean initialCall)
    {
        TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
        top.getTheMenuBar().restoreSavedBindings(false); //trying to cache again
    }

    public void finishPrefReconcilation(String libName, List<Pref.Meaning> meaningsToReconcile)
    {
        OptionReconcile dialog = new OptionReconcile(TopLevel.getCurrentJFrame(), true, meaningsToReconcile, libName);
		dialog.setVisible(true);
    }

	/**
	 * Method to import the preferences from an XML file.
	 * Prompts the user and reads the file.
	 */
    public void importPrefs()
    {
		// prompt for the XML file
        String fileName = OpenFile.chooseInputFile(FileType.PREFS, "Saved Preferences");
        if (fileName == null) return;

        Pref.importPrefs(fileName);
    }

    /**
	 * Method to export the preferences to an XML file.
	 * Prompts the user and writes the file.
	 */
	public void exportPrefs()
	{
		// prompt for the XML file
        String fileName = OpenFile.chooseOutputFile(FileType.PREFS, "Saved Preferences", "electricPrefs.xml");
        if (fileName == null) return;

        Pref.exportPrefs(fileName);
    }

    /** For TextWindow */
    public String [] getEditedText(Cell cell) { return TextWindow.getEditedText(cell); }

    public void updateText(Cell cell, String [] strings) { TextWindow.updateText(cell, strings); }
}

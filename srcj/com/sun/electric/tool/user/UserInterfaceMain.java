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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.Job;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 * Class to build the UserInterface for the main GUI version of the user interface.
 */
public class UserInterfaceMain implements UserInterface
{
	public EditWindow_ getCurrentEditWindow_() { return EditWindow.getCurrent(); }

	public EditWindow_ needCurrentEditWindow_() { return EditWindow.needCurrent(); }

	public Cell getCurrentCell() { return WindowFrame.getCurrentCell(); }

	public Cell needCurrentCell() { return WindowFrame.needCurCell(); }

	public void repaintAllEditWindows() { EditWindow.repaintAllContents(); }

	public void alignToGrid(Point2D pt) { EditWindow.gridAlign(pt); }

	public int getDefaultTextSize() { return EditWindow.getDefaultFontSize(); }

    public void wantToRedoErrorTree() { WindowFrame.wantToRedoErrorTree(); }

//	public Highlighter getHighlighter();

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
            for(Iterator<ErrorLogger.ErrorHighlight> it = log.getHighlights(); it.hasNext(); )
            {
                ErrorLogger.ErrorHighlight eh = (ErrorLogger.ErrorHighlight)it.next();
                if (eh.getType() == ErrorLogger.ErrorLoggerType.ERRORTYPEGEOM)
                {
                    if (geom1 == null) geom1 = eh.getGeom(); else
                        if (geom2 == null) geom2 = eh.getGeom();
                }
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
            for(Iterator<ErrorLogger.ErrorHighlight> it = log.getHighlights(); it.hasNext(); )
            {
                ErrorLogger.ErrorHighlight eh = (ErrorLogger.ErrorHighlight)it.next();

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

                switch (eh.getType())
                {
                    case ERRORTYPEGEOM:
                        if (!eh.getShowGeom()) break;
                        highlighter.addElectricObject(eh.getGeom(), cell);
                        break;
                    case ERRORTYPEEXPORT:
                        highlighter.addText(eh.getExport(), cell, null, null);
//						if (havegeoms == 0) infstr = initinfstr(); else
//							addtoinfstr(infstr, '\n');
//						havegeoms++;
//						formatinfstr(infstr, x_("CELL=%s TEXT=0%lo;0%lo;-"),
//							describenodeproto(eh->pp->parent), (INTBIG)eh->pp->subnodeinst->geom,
//								(INTBIG)eh->pp);
                        break;
                    case ERRORTYPELINE:
                        highlighter.addLine(eh.getPoint(0, 0, 0), eh.getPoint(1, 0, 0), cell);
                        break;
                    case ERRORTYPETHICKLINE:
                        highlighter.addThickLine(eh.getPoint(0, 0, 0), eh.getPoint(1, 0, 0), eh.getCenterPoint(), cell);
                        break;
                    case ERRORTYPEPOINT:
                        double consize = 5;
                        highlighter.addLine(eh.getPoint(0, -consize, -consize), eh.getPoint(0, consize, consize), cell);
                        highlighter.addLine(eh.getPoint(0, -consize, consize), eh.getPoint(0, consize, -consize), cell);
                        break;
                }
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
}

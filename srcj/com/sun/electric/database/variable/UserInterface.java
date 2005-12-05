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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.Point2D;

/**
 * This interface provides information from the user interface.
 */
public interface UserInterface {

	public EditWindow_ getCurrentEditWindow_();
	public EditWindow_ needCurrentEditWindow_();
	public Cell getCurrentCell();
	public Cell needCurrentCell();
	public void repaintAllEditWindows();
	public void alignToGrid(Point2D pt);
	public int getDefaultTextSize();
	public EditWindow_ displayCell(Cell cell);

    /** Related to ExplorerTree */
    public void wantToRedoErrorTree();
    public void wantToRedoJobTree();

    /** ErrorLogger related functions */
    public void termLogging(final ErrorLogger logger, boolean explain);
    /**
     * Method to return the error message associated with the current error.
     * Highlights associated graphics if "showhigh" is nonzero.  Fills "g1" and "g2"
     * with associated geometry modules (if nonzero).
     */
    public String reportLog(ErrorLogger.MessageLog log, boolean showhigh, Geometric [] gPair);

    /** ActivityLogger related functions */
    public void logException(String[] msg);
    public void logFinished(String outputFile);

    /* Job related **/
    public void invokeLaterBusyCursor(boolean state);
    public void setBusyCursor(boolean state);
}

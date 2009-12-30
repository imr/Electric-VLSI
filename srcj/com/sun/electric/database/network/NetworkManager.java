/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetworkTool.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.network;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 *
 */
public class NetworkManager {

    /** The cell for logging network errors */
    private Cell currentErrorCell;
    /** buffer of highlights for next error */
    private ArrayList<ErrorHighlight> errorHighlights = new ArrayList<ErrorHighlight>();
    /** list of errors for current cell */
    private ArrayList<ErrorLogger.MessageLog> errors = new ArrayList<ErrorLogger.MessageLog>();

    /** Creates a new instance of NetworkManager */
    public NetworkManager() {
    }

    /****************************** CHANGE LISTENER ******************************/
    void startErrorLogging(Cell cell) {
        currentErrorCell = cell;
        errorHighlights.clear();
        errors.clear();
    }

    void pushHighlight(Export e) {
//        assert e.getParent() == currentErrorCell;
        errorHighlights.add(ErrorHighlight.newInstance(e));
    }

    void pushHighlight(Geometric geom) {
        assert geom.getParent() == currentErrorCell;
        errorHighlights.add(ErrorHighlight.newInstance(null, geom));
    }

    void pushHighlight(PortInst pi) {
        Poly poly = pi.getPoly();
        Point2D[] points = poly.getPoints();
        for (int i = 0; i < points.length; i++) {
            int prev = i - 1;
            if (i == 0) {
                prev = points.length - 1;
            }
            errorHighlights.add(ErrorHighlight.newInstance(currentErrorCell, points[prev], points[i]));
        }

    }

    void logError(String message, int sortKey) {
        errors.add(new ErrorLogger.MessageLog(message, currentErrorCell, sortKey, errorHighlights));
        errorHighlights.clear();
    }

    void logWarning(String message, int sortKey) {
        errors.add(new ErrorLogger.WarningLog(message, currentErrorCell, sortKey, errorHighlights));
        errorHighlights.clear();
    }

    void finishErrorLogging() {
        Job.updateNetworkErrors(currentErrorCell, errors);
        errorHighlights.clear();
        NetworkTool.totalNumErrors += errors.size();
        errors.clear();
    }
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayerCoverage.java
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
package com.sun.electric.tool.extract;

import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.technology.Technology;

import java.awt.geom.Rectangle2D;

/**
 * Class to describe coverage percentage for a layer.
 */
public class LayerCoverage extends Listener
{
    /** the LayerCoverage tool. */		protected static LayerCoverage tool = new LayerCoverage();
    /**
	 * The constructor sets up the DRC tool.
	 */
	private LayerCoverage()
	{
		super("coverage");
	}

    /****************************** OPTIONS ******************************/

    // Default value is in um to be technology independent
    private static final double defaultSize = 50000;
    private static Pref cacheDeltaX = Pref.makeDoublePref("DeltaX", tool.prefs, defaultSize);
    static { cacheDeltaX.attachToObject(tool, "Tools/Coverage tab", "Delta along X to sweep bounding box"); }
	/**
	 * Method to get user preference for deltaX.
	 * The default is 50 mm.
	 * @return double representing deltaX
	 */
	public static double getDeltaX(Technology tech)
    {
        return cacheDeltaX.getDouble()/tech.getScale();
    }
	/**
	 * Method to set user preference for deltaX.
	 * @param delta double representing new deltaX
	 */
	public static void setDeltaX(double delta, Technology tech)
    {
        cacheDeltaX.setDouble(delta*tech.getScale());
    }

    private static Pref cacheDeltaY = Pref.makeDoublePref("DeltaY", tool.prefs, defaultSize);
    static { cacheDeltaY.attachToObject(tool, "Tools/Coverage tab", "Delta along Y to sweep bounding box"); }
	/**
	 * Method to get user preference for deltaY.
	 * The default is 50 mm.
	 * @return double representing deltaY
	 */
	public static double getDeltaY(Technology tech)
    {
        return cacheDeltaY.getDouble()/tech.getScale();
    }
	/**
	 * Method to set user preference for deltaY.
	 * @param delta double representing new deltaY
	 */
	public static void setDeltaY(double delta, Technology tech)
    {
        cacheDeltaY.setDouble(delta*tech.getScale());
    }

    private static Pref cacheWidth = Pref.makeDoublePref("Width", tool.prefs, defaultSize);
    static { cacheWidth.attachToObject(tool, "Tools/Coverage tab", "Bounding box width"); }
	/**
	 * Method to get user preference for deltaY.
	 * The default is 50 mm.
	 * @return double representing deltaY
	 */
	public static double getWidth(Technology tech)
    {
        return cacheWidth.getDouble()/tech.getScale();
    }
	/**
	 * Method to set user preference for width of the bounding box.
	 * @param w double representing new width
	 */
	public static void setWidth(double w, Technology tech)
    {
        cacheWidth.setDouble(w*tech.getScale());
    }

    private static Pref cacheHeight = Pref.makeDoublePref("Height", tool.prefs, defaultSize);
    static { cacheHeight.attachToObject(tool, "Tools/Coverage tab", "Bounding box height"); }
	/**
	 * Method to get user preference for deltaY.
	 * The default is 50 mm.
	 * @return double representing deltaY
	 */
	public static double getHeight(Technology tech)
    {
        return cacheHeight.getDouble()/tech.getScale();
    }
	/**
	 * Method to set user preference for height of the bounding box.
	 * @param h double representing new width
	 */
	public static void setHeight(double h, Technology tech)
    {
        cacheHeight.setDouble(h*tech.getScale());
    }

    /**
     * Method to handle the "List Layer Coverage", "Coverage Implant Generator",  polygons merge
     * except "List Geometry on Network" commands.
     */

    public static class AreaCoverageJob extends Job
    {
        private Cell curCell;
        double deltaX, deltaY;
        double width, height;
        Highlighter highlighter;
        GeometryHandler.GHMode mode;
        boolean foundError = false;

    	public AreaCoverageJob() {}

        public AreaCoverageJob(Cell cell, Highlighter highlighter, GeometryHandler.GHMode mode, double width, double height, double deltaX, double deltaY)
        {
            super("Layer Coverage", User.getUserTool(), Type.EXAMINE, null, null, Priority.USER);
            this.curCell = cell;
            this.highlighter = highlighter;
            this.mode = mode;
            this.width = width;
            this.height = height;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            setReportExecutionFlag(true); // Want to report statistics
        }

        public boolean isOK() { return !foundError; }

        public boolean doIt() throws JobException
        {
            ErrorLogger errorLogger = ErrorLogger.newInstance("Area Coverage");
            Rectangle2D bBoxOrig = curCell.getBounds();
            double maxY = bBoxOrig.getMaxY();
            double maxX = bBoxOrig.getMaxX();

            // if negative or zero values -> only once
            if (deltaX <= 0) deltaX = bBoxOrig.getWidth();
            if (deltaY <= 0) deltaY = bBoxOrig.getHeight();
            if (width <= 0) width = bBoxOrig.getWidth();
            if (height <= 0) height = bBoxOrig.getHeight();

            for (double posY = bBoxOrig.getMinY(); posY < maxY; posY += deltaY)
            {
                for (double posX = bBoxOrig.getMinX(); posX < maxX; posX += deltaX)
                {
                    Rectangle2D box = new Rectangle2D.Double(posX, posY, width, height);
                    LayerCoverageJob.GeometryOnNetwork geoms = new LayerCoverageJob.GeometryOnNetwork(curCell, null, 1, true);
                    System.out.println("Calculating Coverage on cell '" + curCell.getName() + "' for area (" +
                            DBMath.round(posX) + "," + DBMath.round(posY) + ") (" +
                            DBMath.round(box.getMaxX()) + "," + DBMath.round(box.getMaxY()) + ")");
                    Job job = new LayerCoverageJob(this, Type.EXAMINE, curCell, LCMode.AREA, mode, highlighter, geoms, box);
                    job.doIt();
                    if (checkAbort() || job.checkAbort()) // aborted by user
                    {
                        foundError = true;
                        return false; // didn't finish
                    }
                    if (geoms.analyzeCoverage(box, errorLogger))
                        foundError = true;
                }
            }
            errorLogger.termLogging(true);
            return true;
        }
    }

    public enum LCMode // LC = LayerCoverage mode
    {
	    AREA,   // function Layer Coverage
	    MERGE,  // Generic merge polygons function
	    IMPLANT, // Coverage implants
	    NETWORK; // List Geometry on Network function
    }
}

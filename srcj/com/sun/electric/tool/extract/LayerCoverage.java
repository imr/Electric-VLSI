package com.sun.electric.tool.extract;

import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.technology.Technology;

import java.awt.geom.Rectangle2D;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Feb 10, 2005
 * Time: 3:58:21 PM
 * To change this template use File | Settings | File Templates.
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

    public static class AreaCoverage extends Job
    {
        private Cell curCell;
        double deltaX, deltaY;
        double width, height;
        Highlighter highlighter;
        int mode;
        boolean foundError = false;

        public AreaCoverage(Cell cell, Highlighter highlighter, int mode, double width, double height, double deltaX, double deltaY)
        {
            super("Layer Coverage", User.tool, Type.EXAMINE, null, null, Priority.USER);
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

        public boolean doIt()
        {
            ErrorLogger errorLogger = ErrorLogger.newInstance("Area Coverage");
            Rectangle2D bBoxOrig = curCell.getBounds();
            double maxY = bBoxOrig.getMaxY();
            double maxX = bBoxOrig.getMaxX();
            for (double posY = bBoxOrig.getMinY(); posY < maxY; posY += deltaY)
            {
                for (double posX = bBoxOrig.getMinX(); posX < maxX; posX += deltaX)
                {
                    Rectangle2D box = new Rectangle2D.Double(posX, posY, width, height);
                    LayerCoverageJob.GeometryOnNetwork geoms = new LayerCoverageJob.GeometryOnNetwork(curCell, null, 1, true);
                    System.out.println("Calculating Coverage on cell '" + curCell.getName() + "' for area (" +
                            posX + "," + posY + ") (" + box.getMaxX() + "," + box.getMaxY() + ")");
                    Job job = new LayerCoverageJob(Type.EXAMINE, curCell, LayerCoverageJob.AREA, mode, highlighter, geoms, box);
                    job.doIt();
                    if (geoms.analyzeCoverage(box, errorLogger))
                        foundError = true;
                }
            }
            errorLogger.termLogging(true);
            return true;
        }
    }
}

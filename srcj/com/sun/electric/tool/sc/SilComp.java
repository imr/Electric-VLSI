/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SilComp.java
 * Silicon compiler tool (QUISC): control
 * Written by Andrew R. Kostiuk, Queen's University.
 * Translated to Java by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.sc;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.Pref;
import com.sun.electric.tool.Tool;

import java.awt.geom.Rectangle2D;

/**
 * This is the Silicon Compiler tool.
 */
public class SilComp extends Tool
{
	/** the Silicon Compiler tool. */		private static SilComp tool = new SilComp();
	public static final String SCLIBNAME = "sclib";

	/****************************** TOOL INTERFACE ******************************/

	/**
	 * The constructor sets up the Silicon Compiler tool.
	 */
	private SilComp()
	{
		super("sc");
	}

	/**
	 * Method to initialize the Silicon Compiler tool.
	 */
	public void init()
	{
	}

    /**
     * Method to retrieve the singleton associated with the Silicon Compiler tool.
     * @return the SilComp tool.
     */
    public static SilComp getSilCompTool() { return tool; }

	static double leafCellXSize(Cell cell)
	{
		Rectangle2D bounds = cell.getBounds();
		return bounds.getWidth();
	}

	static double leafCellYSize(Cell cell)
	{
		Rectangle2D bounds = cell.getBounds();
		return bounds.getHeight();
	}

	/**
	 * Method to return the xpos of the indicated leaf port from the left side of it's parent leaf cell.
	 * @param port the leaf port.
	 * @return position from left side of cell.
	 */
	static double leafPortXPos(Export port)
	{
		if (port == null) return 0;
		Poly poly = port.getOriginalPort().getPoly();
		Rectangle2D bounds = ((Cell)port.getParent()).getBounds();
		return poly.getCenterX() - bounds.getMinX();
	}

	/**
	 * Method to return the xpos of the indicated leaf port from the bottom side of it's parent leaf cell.
	 * @param port the leaf port.
	 * @return position from bottom side of cell.
	 */
	static double leafPortYPos(Export port)
	{
		if (port == null) return 0;
		Poly poly = port.getOriginalPort().getPoly();
		Rectangle2D bounds = ((Cell)port.getParent()).getBounds();
		return poly.getCenterY() - bounds.getMinY();
	}

	/****************************** OPTIONS ******************************/

	private static Pref cacheNumberOfRows = Pref.makeIntPref("NumberOfRows", tool.prefs, 4);
	/**
	 * Method to return the number of rows of cells to make.
	 * The default is 4.
	 * @return the number of rows of cells to make.
	 */
	public static int getNumberOfRows() { return cacheNumberOfRows.getInt(); }
	/**
	 * Method to set the number of rows of cells to make.
	 * @param rows the new number of rows of cells to make.
	 */
	public static void setNumberOfRows(int rows) { cacheNumberOfRows.setInt(rows); }


	private static Pref cacheHorizRoutingArc = Pref.makeStringPref("HorizRoutingArc", tool.prefs, "Metal-1");
	/**
	 * Method to return the horizontal routing arc.
	 * The default is "Metal-1".
	 * @return the name of the horizontal routing arc.
	 */
	public static String getHorizRoutingArc() { return cacheHorizRoutingArc.getString(); }
	/**
	 * Method to set the horizontal routing arc.
	 * @param arcName name of new horizontal routing arc.
	 */
	public static void setHorizRoutingArc(String arcName) { cacheHorizRoutingArc.setString(arcName); }

	private static Pref cacheHorizRoutingWidth = Pref.makeDoublePref("HorizArcWidth", tool.prefs, 4);
	/**
	 * Method to return the width of the horizontal routing arc.
	 * The default is 4.
	 * @return the width of the horizontal routing arc.
	 */
	public static double getHorizArcWidth() { return cacheHorizRoutingWidth.getDouble(); }
	/**
	 * Method to set the width of the horizontal routing arc.
	 * @param wid the new width of the horizontal routing arc.
	 */
	public static void setHorizArcWidth(double wid) { cacheHorizRoutingWidth.setDouble(wid); }


	private static Pref cacheVertRoutingArc = Pref.makeStringPref("VertRoutingArc", tool.prefs, "Metal-2");
	/**
	 * Method to return the vertical routing arc.
	 * The default is "Metal-2".
	 * @return the name of the vertical routing arc.
	 */
	public static String getVertRoutingArc() { return cacheVertRoutingArc.getString(); }
	/**
	 * Method to set the vertical routing arc.
	 * @param arcName name of new vertical routing arc.
	 */
	public static void setVertRoutingArc(String arcName) { cacheVertRoutingArc.setString(arcName); }

	private static Pref cacheVertRoutingWidth = Pref.makeDoublePref("VertArcWidth", tool.prefs, 4);
	/**
	 * Method to return the width of the vertical routing arc.
	 * The default is 4.
	 * @return the width of the vertical routing arc.
	 */
	public static double getVertArcWidth() { return cacheVertRoutingWidth.getDouble(); }
	/**
	 * Method to set the width of the vertical routing arc.
	 * @param wid the new width of the vertical routing arc.
	 */
	public static void setVertArcWidth(double wid) { cacheVertRoutingWidth.setDouble(wid); }


	private static Pref cachePowerWireWidth = Pref.makeDoublePref("PowerWireWidth", tool.prefs, 5);
	/**
	 * Method to return the width of the power and ground arc.
	 * The default is 5.
	 * @return the width of the power and ground arc.
	 */
	public static double getPowerWireWidth() { return cachePowerWireWidth.getDouble(); }
	/**
	 * Method to set the width of the power and ground arc.
	 * @param wid the new width of the power and ground arc.
	 */
	public static void setPowerWireWidth(double wid) { cachePowerWireWidth.setDouble(wid); }

	private static Pref cacheMainPowerWireWidth = Pref.makeDoublePref("MainPowerWireWidth", tool.prefs, 8);
	/**
	 * Method to return the width of the main power and ground arc.
	 * The default is 8.
	 * @return the width of the main power and ground arc.
	 */
	public static double getMainPowerWireWidth() { return cacheMainPowerWireWidth.getDouble(); }
	/**
	 * Method to set the width of the main power and ground arc.
	 * @param wid the new width of the main power and ground arc.
	 */
	public static void setMainPowerWireWidth(double wid) { cacheMainPowerWireWidth.setDouble(wid); }

	private static Pref cacheMainPowerArc = Pref.makeStringPref("MainPowerArc", tool.prefs, "Horizontal Arc");
	/**
	 * Method to return the main power and ground arc.
	 * The default is "Horizontal Arc".
	 * @return the name of the main power and ground arc.
	 */
	public static String getMainPowerArc() { return cacheMainPowerArc.getString(); }
	/**
	 * Method to set the main power and ground arc.
	 * @param arcName name of new main power and ground arc.
	 */
	public static void setMainPowerArc(String arcName) { cacheMainPowerArc.setString(arcName); }

	private static Pref cachePWellHeight = Pref.makeDoublePref("PWellHeight", tool.prefs, 41);
	/**
	 * Method to return the height of the p-well.
	 * The default is 41.
	 * @return the height of the p-well.
	 */
	public static double getPWellHeight() { return cachePWellHeight.getDouble(); }
	/**
	 * Method to set the height of the p-well.
	 * @param hei the new height of the p-well.
	 */
	public static void setPWellHeight(double hei) { cachePWellHeight.setDouble(hei); }

	private static Pref cachePWellOffset = Pref.makeDoublePref("PWellOffset", tool.prefs, 0);
	/**
	 * Method to return the offset of the p-well.
	 * The default is 0.
	 * @return the offset of the p-well.
	 */
	public static double getPWellOffset() { return cachePWellOffset.getDouble(); }
	/**
	 * Method to set the offset of the p-well.
	 * @param off the new offset of the p-well.
	 */
	public static void setPWellOffset(double off) { cachePWellOffset.setDouble(off); }

	private static Pref cacheNWellHeight = Pref.makeDoublePref("NWellHeight", tool.prefs, 51);
	/**
	 * Method to return the height of the n-well.
	 * The default is 51.
	 * @return the height of the n-well.
	 */
	public static double getNWellHeight() { return cacheNWellHeight.getDouble(); }
	/**
	 * Method to set the height of the n-well.
	 * @param hei the new height of the n-well.
	 */
	public static void setNWellHeight(double hei) { cacheNWellHeight.setDouble(hei); }

	private static Pref cacheNWellOffset = Pref.makeDoublePref("NWellOffset", tool.prefs, 0);
	/**
	 * Method to return the offset of the n-well.
	 * The default is 0.
	 * @return the offset of the n-well.
	 */
	public static double getNWellOffset() { return cacheNWellOffset.getDouble(); }
	/**
	 * Method to set the offset of the n-well.
	 * @param off the new offset of the n-well.
	 */
	public static void setNWellOffset(double off) { cacheNWellOffset.setDouble(off); }


	private static Pref cacheViaSize = Pref.makeDoublePref("ViaSize", tool.prefs, 4);
	/**
	 * Method to return the size of vias.
	 * The default is 4.
	 * @return the size of vias.
	 */
	public static double getViaSize() { return cacheViaSize.getDouble(); }
	/**
	 * Method to set the size of vias.
	 * @param off the new size of vias.
	 */
	public static void setViaSize(double off) { cacheViaSize.setDouble(off); }

	private static Pref cacheMinMetalSpacing = Pref.makeDoublePref("MinMetalSpacing", tool.prefs, 6);
	/**
	 * Method to return the minimum metal spacing.
	 * The default is 6.
	 * @return the minimum metal spacing.
	 */
	public static double getMinMetalSpacing() { return cacheMinMetalSpacing.getDouble(); }
	/**
	 * Method to set the minimum metal spacing.
	 * @param off the new minimum metal spacing.
	 */
	public static void setMinMetalSpacing(double off) { cacheMinMetalSpacing.setDouble(off); }

	private static Pref cacheFeedThruSize = Pref.makeDoublePref("FeedThruSize", tool.prefs, 16);
	/**
	 * Method to return the size of feed-throughs.
	 * The default is 16.
	 * @return the size of feed-throughs.
	 */
	public static double getFeedThruSize() { return cacheFeedThruSize.getDouble(); }
	/**
	 * Method to set the size of feed-throughs.
	 * @param off the new size of feed-throughs.
	 */
	public static void setFeedThruSize(double off) { cacheFeedThruSize.setDouble(off); }

	private static Pref cacheMinPortDistance = Pref.makeDoublePref("MinPortDistance", tool.prefs, 8);
	/**
	 * Method to return the minimum port distance.
	 * The default is 8.
	 * @return the minimum port distance.
	 */
	public static double getMinPortDistance() { return cacheMinPortDistance.getDouble(); }
	/**
	 * Method to set the minimum port distance.
	 * @param off the new minimum port distance.
	 */
	public static void setMinPortDistance(double off) { cacheMinPortDistance.setDouble(off); }

	private static Pref cacheMinActiveDistance = Pref.makeDoublePref("MinActiveDistance", tool.prefs, 8);
	/**
	 * Method to return the minimum active distance.
	 * The default is 8.
	 * @return the minimum active distance.
	 */
	public static double getMinActiveDistance() { return cacheMinActiveDistance.getDouble(); }
	/**
	 * Method to set the minimum active distance.
	 * @param off the new minimum active distance.
	 */
	public static void setMinActiveDistance(double off) { cacheMinActiveDistance.setDouble(off); }

}

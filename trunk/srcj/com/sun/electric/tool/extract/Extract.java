/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Extract.java
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
package com.sun.electric.tool.extract;

import com.sun.electric.database.text.Pref;
import com.sun.electric.tool.Tool;

/**
 * This is the Extraction tool.
 */
public class Extract extends Tool
{
	/** the Extraction tool. */								private static Extract tool = new Extract();

	/****************************** TOOL CONTROL ******************************/

	/**
	 * The constructor sets up the DRC tool.
	 */
	private Extract()
	{
		super("extract");
	}

    /**
	 * Method to initialize the Extraction tool.
	 */
	public void init()
	{
	}

    /**
     * Method to retrieve the singleton associated with the Extract tool.
     * @return the Extract tool.
     */
    public static Extract getExtractTool() { return tool; }

	/****************************** OPTIONS ******************************/

	private static Pref cacheExactGridAlign = Pref.makeBooleanPref("GridAlignExtraction", Extract.tool.prefs, false);
	/**
	 * Method to tell whether the node extractor should grid-align geometry before extraction.
	 * This is useful if the input geometry has many small alignment errors.
	 * The default is "false".
	 * @return true if the node extractor should grid-align geometry before extraction.
	 */
	public static boolean isGridAlignExtraction() { return cacheExactGridAlign.getBoolean(); }
	/**
	 * Method to set whether the node extractor should grid-align geometry before extraction.
	 * This is useful if the input geometry has many small alignment errors.
	 * @param a true if the node extractor should grid-align geometry before extraction.
	 */
	public static void setGridAlignExtraction(boolean a) { cacheExactGridAlign.setBoolean(a); }
	/**
	 * Method to tell whether the node extractor should grid-align geometry before extraction, by default.
	 * This is useful if the input geometry has many small alignment errors.
	 * @return true if the node extractor should grid-align geometry before extraction, by default.
	 */
	public static boolean isFactoryGridAlignExtraction() { return cacheExactGridAlign.getBooleanFactoryValue(); }

	private static Pref cacheActiveHandling = Pref.makeIntPref("ActiveHandling", Extract.tool.prefs, 0);
	/**
	 * Method to tell how the node extractor should handle active layers.
	 * The values can be:
	 * 0: Insist on two different active layers (N and P) and also proper select/well surrounds (the default).
	 * 1: Ignore active distinctions and use select/well surrounds to distinguish N from P.
	 * 2: Insist on two different active layers (N and P) but ignore select/well surrounds.
	 * @return an integer indicating how to handle active layers.
	 */
	public static int getActiveHandling() { return cacheActiveHandling.getInt(); }
	/**
	 * Method to set how the node extractor should handle active layers.
	 * @param a an integer indicating how to handle active layers.
	 * The values can be:
	 * 0: Insist on two different active layers (N and P) and also proper select/well surrounds (the default).
	 * 1: Ignore active distinctions and use select/well surrounds to distinguish N from P.
	 * 2: Insist on two different active layers (N and P) but ignore select/well surrounds.
	 */
	public static void setActiveHandling(int a) { cacheActiveHandling.setInt(a); }
	/**
	 * Method to tell how the node extractor should handle active layers, by default.
	 * The values can be:
	 * 0: Insist on two different active layers (N and P) and also proper select/well surrounds.
	 * 1: Ignore active distinctions and use select/well surrounds to distinguish N from P.
	 * 2: Insist on two different active layers (N and P) but ignore select/well surrounds.
	 * @return an integer indicating how to handle active layers, by default.
	 */
	public static int getFactoryActiveHandling() { return cacheActiveHandling.getIntFactoryValue(); }

	private static Pref cacheApproximateCuts = Pref.makeBooleanPref("ApproximateCuts", Extract.tool.prefs, false);
	/**
	 * Method to tell whether the node extractor should approximate cut placement in multicut situations.
	 * When via layers in multicut situations do not exactly match Electric's spacing, this will allow
	 * a single large contact to be placed.
	 * The default is "false".
	 * @return true if the node extractor should approximate cut placement in multicut situations.
	 */
	public static boolean isApproximateCuts() { return cacheApproximateCuts.getBoolean(); }
	/**
	 * Method to set whether the node extractor should approximate cut placement in multicut situations.
	 * When via layers in multicut situations do not exactly match Electric's spacing, this will allow
	 * a single large contact to be placed.
	 * @param a true if the node extractor should approximate cut placement in multicut situations.
	 */
	public static void setApproximateCuts(boolean a) { cacheApproximateCuts.setBoolean(a); }
	/**
	 * Method to tell whether the node extractor should approximate cut placement in multicut situations, by default.
	 * When via layers in multicut situations do not exactly match Electric's spacing, this will allow
	 * a single large contact to be placed.
	 * @return true if the node extractor should approximate cut placement in multicut situations, by default.
	 */
	public static boolean isFactoryApproximateCuts() { return cacheApproximateCuts.getBooleanFactoryValue(); }

	private static Pref cacheIgnoreTinyPolygons = Pref.makeBooleanPref("IgnoreTinyPolygons", Extract.tool.prefs, false);
	/**
	 * Method to tell whether the node extractor should ignore tiny polygons.
	 * The default is "false".
	 * @return true if the node extractor should ignore tiny polygons.
	 */
	public static boolean isIgnoreTinyPolygons() { return cacheIgnoreTinyPolygons.getBoolean(); }
	/**
	 * Method to set whether the node extractor should ignore tiny polygons.
	 * @param a true if the node extractor should ignore tiny polygons.
	 */
	public static void setIgnoreTinyPolygons(boolean a) { cacheIgnoreTinyPolygons.setBoolean(a); }
	/**
	 * Method to tell whether the node extractor should ignore tiny polygons, by default.
	 * @return true if the node extractor should ignore tiny polygons, by default.
	 */
	public static boolean isFactoryIgnoreTinyPolygons() { return cacheIgnoreTinyPolygons.getBooleanFactoryValue(); }

	private static Pref cacheSmallestPolygonSize = Pref.makeDoublePref("SmallestPolygonSize", Extract.tool.prefs, 0.25);
	/**
	 * Method to return the size of the smallest polygon to extract.
	 * Any polygon smaller than this will be ignored.
	 * The default is 0.25 square grid units.
	 * @return the size of the smallest polygon to extract.
	 */
	public static double getSmallestPolygonSize() { return cacheSmallestPolygonSize.getDouble(); }
	/**
	 * Method to set the size of the smallest polygon to extract.
	 * Any polygon smaller than this will be ignored.
	 * @param a the size of the smallest polygon to extract.
	 */
	public static void setSmallestPolygonSize(double a) { cacheSmallestPolygonSize.setDouble(a); }
	/**
	 * Method to return the size of the smallest polygon to extract, by default.
	 * Any polygon smaller than this will be ignored.
	 * @return the size of the smallest polygon to extract, by default.
	 */
	public static double getFactorySmallestPolygonSize() { return cacheSmallestPolygonSize.getDoubleFactoryValue(); }

	private static Pref cacheCellExpandPattern = Pref.makeStringPref("CellExpandPattern", Extract.tool.prefs, ".*via.*");
	/**
	 * Method to return the cell expansion pattern for node extraction.
	 * All cells that match this string will be expanded before node extraction.
	 * The default is ".*via.*" (anything with the word "via" in it).
	 * @return the cell expansion pattern for node extraction.
	 */
	public static String getCellExpandPattern() { return cacheCellExpandPattern.getString(); }
	/**
	 * Method to set the cell expansion pattern for node extraction.
	 * All cells that match this string will be expanded before node extraction.
	 * @param a the cell expansion pattern for node extraction.
	 */
	public static void setCellExpandPattern(String a) { cacheCellExpandPattern.setString(a); }
	/**
	 * Method to return the cell expansion pattern for node extraction, by default.
	 * All cells that match this string will be expanded before node extraction.
	 * @return the cell expansion pattern for node extraction, by default.
	 */
	public static String getFactoryCellExpandPattern() { return cacheCellExpandPattern.getStringFactoryValue(); }

	private static Pref cacheFlattenPcells = Pref.makeBooleanPref("FlattenPcells", Extract.tool.prefs, true);
	/**
	 * Method to tell whether the node extractor should flatten Cadence Pcells.
	 * Cadence Pcells are cells whose names end with "$$" and a number.
	 * The default is "true".
	 * @return true if the node extractor should flatten Cadence Pcells.
	 */
	public static boolean isFlattenPcells() { return cacheFlattenPcells.getBoolean(); }
	/**
	 * Method to set whether the node extractor should flatten Cadence Pcells.
	 * Cadence Pcells are cells whose names end with "$$" and a number.
	 * @param a true if the node extractor should flatten Cadence Pcells.
	 */
	public static void setFlattenPcells(boolean a) { cacheFlattenPcells.setBoolean(a); }
	/**
	 * Method to tell whether the node extractor should flatten Cadence Pcells by default.
	 * Cadence Pcells are cells whose names end with "$$" and a number.
	 * @return true if the node extractor should flatten Cadence Pcells, by default.
	 */
	public static boolean isFactoryFlattenPcells() { return cacheFlattenPcells.getBooleanFactoryValue(); }

	private static Pref cacheUsePureLayerNodes = Pref.makeBooleanPref("UsePureLayerNodes", Extract.tool.prefs, false);
	/**
	 * Method to tell whether the node extractor should use pure-layer nodes for connectivity.
	 * The alternative is to use pins and arcs.
	 * The default is "false".
	 * @return true if the node extractor should use pure-layer nodes for connectivity.
	 */
	public static boolean isUsePureLayerNodes() { return cacheUsePureLayerNodes.getBoolean(); }
	/**
	 * Method to set whether the node extractor should use pure-layer nodes for connectivity.
	 * The alternative is to use pins and arcs.
	 * @param a true if the node extractor should flatten Cadence Pcells.
	 */
	public static void setUsePureLayerNodes(boolean a) { cacheUsePureLayerNodes.setBoolean(a); }
	/**
	 * Method to tell whether the node extractor should use pure-layer nodes for connectivity by default.
	 * The alternative is to use pins and arcs.
	 * @return true if the node extractor should use pure-layer nodes for connectivity, by default.
	 */
	public static boolean isFactoryUsePureLayerNodes() { return cacheUsePureLayerNodes.getBooleanFactoryValue(); }
}

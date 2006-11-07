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
}

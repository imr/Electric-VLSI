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

	private static Pref cacheExactCutExtraction = Pref.makeBooleanPref("ExactCutExtraction", Extract.tool.prefs, false);
	/**
	 * Method to tell whether the node extractor should preserve contact/via cut placement precisely.
	 * Since Electric automatically generates cuts, they may not land in the same place as those
	 * that come from other systems.
	 * The default is "false", meaning that the node extractor will create the largest contact
	 * possible given the layer overlaps.
	 * @return true if the node extractor should preserve contact/via cut placement precisely.
	 */
	public static boolean isExactCutExtraction() { return cacheExactCutExtraction.getBoolean(); }
	/**
	 * Method to set whether the node extractor should preserve contact/via cut placement precisely.
	 * Since Electric automatically generates cuts, they may not land in the same place as those
	 * that come from other systems.
	 * @param e true if the node extractor should preserve contact/via cut placement precisely.
	 */
	public static void setExactCutExtraction(boolean e) { cacheExactCutExtraction.setBoolean(e); }

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
}

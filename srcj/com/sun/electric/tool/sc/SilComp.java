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
package com.sun.electric.tool.sc;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.IconParameters;

import java.awt.geom.Rectangle2D;

/**
 * This is the Silicon Compiler tool.
 */
public class SilComp extends Tool
{
	/** the Silicon Compiler tool. */		private static SilComp tool = new SilComp();
	public static final String SCLIBNAME = "sclib";
    private static final String PREF_NODE = "tool/sc";

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
    @Override
	public void init()
	{
	}

    public static class SilCompPrefs extends PrefPackage
    {
        // from Settings
    	public String schematicTechnology;

        // icon prefereces
        public IconParameters iconParameters = IconParameters.makeInstance(true);

        // SilComp prefs

		// the layout information
        /** The number of rows of cells to make. The default is 4. */
        @IntegerPref(node = PREF_NODE, key = "NumberOfRows", factory = 4)
    	public int numRows;

		// the arc information
        /** The horizontal routing arc. The default is "Metal-1". */
        @StringPref(node = PREF_NODE, key = "HorizRoutingArc", factory = "Metal-1")
    	public String horizRoutingArc;

       /** The width of the horizontal routing arc. The default is 4. */
        @DoublePref(node = PREF_NODE, key = "HorizArcWidth", factory = 4)
    	public double horizArcWidth;

        /** The vertical routing arc. The default is "Metal-2". */
        @StringPref(node = PREF_NODE, key = "VertRoutingArc", factory = "Metal-2")
    	public String vertRoutingArc;

         /** The width of the vertical routing arc. The default is 4. */
        @DoublePref(node = PREF_NODE, key = "VertArcWidth", factory = 4)
    	public double vertArcWidth;

        /** The width of the power and ground arc. The default is 5. */
        @DoublePref(node = PREF_NODE, key = "PowerWireWidth", factory = 5)
    	public double powerWireWidth;

        /** The width of the main power and ground arc. The default is 8. */
        @DoublePref(node = PREF_NODE, key = "MainPowerWireWidth", factory = 8)
    	public double mainPowerWireWidth;

        /** The main power and ground arc. The default is "Horizontal Arc". */
         @StringPref(node = PREF_NODE, key = "MainPowerArc", factory = "Horizontal Arc")
        public String mainPowerArc;

		// the Well information
         /** The height of the p-well. The default is 41. */
        @DoublePref(node = PREF_NODE, key = "PWellHeight", factory = 41)
    	public double pWellHeight;

        /** The offset of the p-well. The default is 0. */
        @DoublePref(node = PREF_NODE, key = "PWellOffset", factory = 0)
    	public double pWellOffset;

        /** The height of the n-well. The default is 51. */
        @DoublePref(node = PREF_NODE, key = "NWellHeight", factory = 51)
    	public double nWellHeight;

        /** The offset of the n-well. The default is 0. */
        @DoublePref(node = PREF_NODE, key = "NWellOffset", factory = 0)
    	public double nWellOffset;

		// the Design Rules
         /** The size of vias. The default is 4. */
        @DoublePref(node = PREF_NODE, key = "ViaSize", factory = 4)
    	public double viaSize;

        /** The minimum metal spacing. The default is 6. */
        @DoublePref(node = PREF_NODE, key = "MinMetalSpacing", factory = 6)
    	public double minMetalSpacing;

        /** The size of feed-throughs. The default is 16. */
        @DoublePref(node = PREF_NODE, key = "FeedThruSize", factory = 16)
     	public double feedThruSize;

        /** The minimum port distance. The default is 8. */
        @DoublePref(node = PREF_NODE, key = "MinPortDistance", factory = 8)
    	public double minPortDistance;

        /** The minimum active distance. The default is 8. */
        @DoublePref(node = PREF_NODE, key = "MinActiveDistance", factory = 8)
    	public double minActiveDistance;

		public SilCompPrefs(boolean factory)
		{
            super(factory);
			schematicTechnology = User.getSchematicTechnology().getTechName();
        }
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
		Poly poly = port.getPoly();
		Rectangle2D bounds = port.getParent().getBounds();
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
		Poly poly = port.getPoly();
		Rectangle2D bounds = port.getParent().getBounds();
		return poly.getCenterY() - bounds.getMinY();
	}
}

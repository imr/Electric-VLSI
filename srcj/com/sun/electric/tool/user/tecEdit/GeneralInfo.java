/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GeneralInfo.java
 * Technology Editor, general factors information
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;

import java.awt.Color;
import java.util.Iterator;

/**
 * This class defines general information about a technology in the Technology Editor.
 */
public class GeneralInfo extends Info
{
	/** the full description of the technology */	String   description;
	/** the scale factor of the technology */		double   scale;
	/** minimum resistance/capacitance */			double   minRes, minCap;
	/** gate shrinkage for the technology */		double   gateShrinkage;
	/** true to include gates in resistance calc */	boolean  includeGateInResistance;
	/** true to include ground in parasitics */		boolean  includeGround;
	/** the transparent colors in the technology */	Color [] transparentColors;

	static SpecialTextDescr [] genTextTable =
	{
		new SpecialTextDescr(0,   3, TECHSCALE),
		new SpecialTextDescr(0,   0, TECHDESCRIPT),
		new SpecialTextDescr(0,  -3, TECHSPICEMINRES),
		new SpecialTextDescr(0,  -6, TECHSPICEMINCAP),
		new SpecialTextDescr(0,  -9, TECHGATESHRINK),
		new SpecialTextDescr(0, -12, TECHGATEINCLUDED),
		new SpecialTextDescr(0, -15, TECHGROUNDINCLUDED),
		new SpecialTextDescr(0, -18, TECHTRANSPCOLORS),
	};

	/**
	 * Method to build the appropriate descriptive information for a layer into
	 * cell "np".  The color is "colorindex"; the stipple array is in "stip"; the
	 * layer style is in "style", the CIF layer is in "ciflayer"; the function is
	 * in "functionindex"; the Calma GDS-II layer is in "gds"; the SPICE resistance is in "spires",
	 * the SPICE capacitance is in "spicap", the SPICE edge capacitance is in "spiecap",
	 * the 3D height is in "height3d", and the 3D thickness is in "thick3d".
	 */
	void generate(Cell np)
	{
		// load up the structure with the current values
		loadTableEntry(genTextTable, TECHSCALE, new Double(scale));
		loadTableEntry(genTextTable, TECHDESCRIPT, description);
		loadTableEntry(genTextTable, TECHSPICEMINRES, new Double(minRes));
		loadTableEntry(genTextTable, TECHSPICEMINCAP, new Double(minCap));
		loadTableEntry(genTextTable, TECHGATESHRINK, new Double(gateShrinkage));
		loadTableEntry(genTextTable, TECHGATEINCLUDED, new Boolean(includeGateInResistance));
		loadTableEntry(genTextTable, TECHGROUNDINCLUDED, new Boolean(includeGround));
		loadTableEntry(genTextTable, TECHTRANSPCOLORS, transparentColors);

		// now create those text objects
		createSpecialText(np, genTextTable);
	}

	/**
	 * Method to parse the miscellaneous-info cell in "np" and return a GeneralInfo object that describes it.
	 */
	static GeneralInfo parseCell(Cell np)
	{
		// create and initialize the GRAPHICS structure
		GeneralInfo gi = new GeneralInfo();

		for(Iterator<NodeInst> it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			int opt = Manipulate.getOptionOnNode(ni);
			String str = getValueOnNode(ni);
			switch (opt)
			{
				case TECHSCALE:
					gi.scale = TextUtils.atof(str);
					break;
				case TECHDESCRIPT:
					gi.description = str;
					break;
				case TECHSPICEMINRES:
					gi.minRes = TextUtils.atof(str);
					break;
				case TECHSPICEMINCAP:
					gi.minCap = TextUtils.atof(str);
					break;
				case TECHGATESHRINK:
					gi.gateShrinkage = TextUtils.atof(str);
					break;
				case TECHGATEINCLUDED:
					gi.includeGateInResistance = str.equalsIgnoreCase("yes");
					break;
				case TECHGROUNDINCLUDED:
					gi.includeGround = str.equalsIgnoreCase("yes");
					break;
				case TECHTRANSPCOLORS:
					Variable var = ni.getVar(TRANSLAYER_KEY);
					if (var != null)
					{
						Color [] colors = getTransparentColors((String)var.getObject());
						if (colors != null) gi.transparentColors = colors;
					}
					break;
				case CENTEROBJ:
					break;
				default:
					LibToTech.pointOutError(ni, np);
					System.out.println("Unknown object in miscellaneous-information cell (" + ni + ")");
					break;
			}
		}
		return gi;
	}

	static Color [] getTransparentColors(String str)
	{
		String [] colorNames = str.split("/");
		Color [] colors = new Color[colorNames.length];
		for(int i=0; i<colorNames.length; i++)
		{
			String colorName = colorNames[i].trim();
			String [] rgb = colorName.split(",");
			if (rgb.length != 3) return null;
			int r = TextUtils.atoi(rgb[0]);
			int g = TextUtils.atoi(rgb[1]);
			int b = TextUtils.atoi(rgb[2]);
			colors[i] = new Color(r, g, b);
		}
		return colors;
	}

	static String makeTransparentColorsLine(Color [] trans)
	{
		String str = "The Transparent Colors: ";
		for(int j=0; j<trans.length; j++)
		{
			if (j != 0) str += " /";
			str += " " + trans[j].getRed() + "," + trans[j].getGreen() + "," + trans[j].getBlue();
		}
		return str;
	}
}

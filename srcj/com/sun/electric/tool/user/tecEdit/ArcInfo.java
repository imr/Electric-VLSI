/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ArcInfo.java
 * Technology Editor, arc information
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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.technologies.Generic;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;

/**
 * This class defines information about arcs in the Technology Editor.
 */
public class ArcInfo extends Info
{
	static class LayerDetails
	{
		LayerInfo layer;
		double    width;
		Poly.Type style;
	}

	/** the name of the arc */					String name;
	/** the name of the arc in Java code */		String javaName;
	/** the arc Function */						ArcProto.Function func;
	/** true for fixed-angle arcs */			boolean fixAng;
	/** true if arcs wipe pins */				boolean wipes;
	/** true if arcs don't extend endpoints */	boolean noExtend;
	/** the arc angle increment */				int angInc;
	/** the maximum antenna ratio */			double antennaRatio;
	/** the ArcProto in the Technology */		ArcProto generated;
	/** layers in the arc */					LayerDetails [] arcDetails;
	/** the width offset to the highlight */	double widthOffset;
	double maxWidth;

	private static SpecialTextDescr [] arcTextTable =
	{
		new SpecialTextDescr(0, 18, ARCFUNCTION),
		new SpecialTextDescr(0, 15, ARCFIXANG),
		new SpecialTextDescr(0, 12, ARCWIPESPINS),
		new SpecialTextDescr(0,  9, ARCNOEXTEND),
		new SpecialTextDescr(0,  6, ARCINC),
		new SpecialTextDescr(0,  3, ARCANTENNARATIO)
	};

	ArcInfo()
	{
		func = ArcProto.Function.UNKNOWN;
	}

	/**
	 * Method to return an array of cells that comprise the arcs in a technology library.
	 * @param lib the technology library.
	 * @return an array of cells for each arc (in the proper order).
	 */
	public static Cell [] getArcCells(Library lib)
	{
		Library [] oneLib = new Library[1];
		oneLib[0] = lib;
		return findCellSequence(oneLib, "arc-", ARCSEQUENCE_KEY);
	}

	/**
	 * Method to build the appropriate descriptive information for an arc into
	 * cell "np".  The function is in "func"; the arc is fixed-angle if "fixang"
	 * is nonzero; the arc wipes pins if "wipes" is nonzero; and the arc does
	 * not extend its ends if "noextend" is nonzero.  The angle increment is
	 * in "anginc".
	 */
	void generate(Cell np)
	{
		// load up the structure with the current values
		loadTableEntry(arcTextTable, ARCFUNCTION, func);
		loadTableEntry(arcTextTable, ARCFIXANG, new Boolean(fixAng));
		loadTableEntry(arcTextTable, ARCWIPESPINS, new Boolean(wipes));
		loadTableEntry(arcTextTable, ARCNOEXTEND, new Boolean(noExtend));
		loadTableEntry(arcTextTable, ARCINC, new Integer(angInc));
		loadTableEntry(arcTextTable, ARCANTENNARATIO, new Double(antennaRatio));

		// now create those text objects
		createSpecialText(np, arcTextTable);
	}

	/**
	 * Method to parse the arc cell in "np" and return an ArcInfo object that describes it.
	 */
	static ArcInfo parseCell(Cell np)
	{
		// create and initialize the GRAPHICS structure
		ArcInfo aIn = new ArcInfo();
		aIn.name = np.getName().substring(4);

		// look at all nodes in the arc description cell
		for(Iterator<NodeInst> it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			Variable var = ni.getVar(OPTION_KEY);
			if (var == null) continue;
			String str = getValueOnNode(ni);

			switch (((Integer)var.getObject()).intValue())
			{
				case ARCFUNCTION:
					aIn.func = ArcProto.Function.UNKNOWN;
					List<ArcProto.Function> allFuncs = ArcProto.Function.getFunctions();
					for(ArcProto.Function fun : allFuncs)
					{
						if (fun.toString().equalsIgnoreCase(str))
						{
							aIn.func = fun;
							break;
						}
					}
					break;
				case ARCINC:
					aIn.angInc = TextUtils.atoi(str);
					break;
				case ARCFIXANG:
					aIn.fixAng = str.equalsIgnoreCase("yes");
					break;
				case ARCWIPESPINS:
					aIn.wipes = str.equalsIgnoreCase("yes");
					break;
				case ARCNOEXTEND:
					aIn.noExtend = str.equalsIgnoreCase("no");
					break;
				case ARCANTENNARATIO:
					aIn.antennaRatio = TextUtils.atof(str);
					break;
			}
		}
		return aIn;
	}

	/**
	 * Method to compact an Arc technology-edit cell
	 */
	static void compactCell(Cell cell)
	{
		// compute bounds of arc contents
		Rectangle2D nonSpecBounds = null;
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.getProto() == Generic.tech.cellCenterNode) continue;

			// ignore the special text nodes
			boolean special = false;
			for(int i=0; i<arcTextTable.length; i++)
				if (arcTextTable[i].ni == ni) special = true;
			if (special) continue;

			// compute overall bounds
			Rectangle2D bounds = ni.getBounds();
			if (nonSpecBounds == null) nonSpecBounds = bounds; else
				Rectangle2D.union(nonSpecBounds, bounds, nonSpecBounds);
		}

		// now rearrange the geometry
		if (nonSpecBounds != null)
		{
			double xOff = -nonSpecBounds.getCenterX();
			double yOff = -nonSpecBounds.getMaxY();
			if (xOff != 0 || yOff != 0)
			{
				for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					if (ni.getProto() == Generic.tech.cellCenterNode) continue;

					// ignore the special text nodes
					boolean special = false;
					for(int i=0; i<arcTextTable.length; i++)
						if (arcTextTable[i].ni == ni) special = true;
					if (special) continue;

					// center the geometry
					ni.move(xOff, yOff);
				}
			}
		}
	}
}


/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementSimple.java
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.placement;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple Placement algorithm to assign locations based on simple criteria.
 */
public class PlacementSimple extends PlacementFrame
{
	/**
	 * Method to return the name of this placement algorithm.
	 * @return the name of this placement algorithm.
	 */
	public String getAlgorithmName() { return "Simple"; }

	/**
	 * Method to do Simple Placement.
	 * @param nodesToPlace a list of all nodes that are to be placed.
	 * @param allNetworks a list of all networks that connect the nodes.
	 * @param cellName the name of the cell being placed.
	 */
	protected void runPlacement(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks, String cellName)
	{
		// gather lists of transistors, resistors, capacitors, and instances
		double pPos = 0, nPos = 0, iPos = 0;
		List<PlacementNode> resistors = new ArrayList<PlacementNode>();
		List<PlacementNode> capacitors = new ArrayList<PlacementNode>();
		for(PlacementNode plNode : nodesToPlace)
		{
			if (plNode.getType() == Schematics.tech().transistorNode ||
				plNode.getType() == Schematics.tech().transistor4Node)
			{
				PrimitiveNode.Function fun = Schematics.tech().getPrimitiveFunction((PrimitiveNode)plNode.getType(),
					plNode.getTechBits());
				if (fun.isPTypeTransistor())
				{
					plNode.setPlacement(pPos, 5);
					plNode.setOrientation(Orientation.R);
					pPos += 10;
				} else
				{
					plNode.setPlacement(nPos, -5);
					plNode.setOrientation(Orientation.R);
					nPos += 10;
				}
			} else if (plNode.getType() == Schematics.tech().resistorNode)
			{
				resistors.add(plNode);
			} else if (plNode.getType() == Schematics.tech().capacitorNode)
			{
				capacitors.add(plNode);
			} else
			{
				plNode.setPlacement(iPos, -10-plNode.getType().getDefHeight());
				iPos += 30;
			}
		}

		// place resistors and capacitors at the end of the transistor rows
		for(PlacementNode plNode : resistors)
		{
			plNode.setPlacement(pPos, 5);
			pPos += 10;
		}
		for(PlacementNode plNode : capacitors)
		{
			plNode.setPlacement(nPos, -5);
			nPos += 10;
		}
	}
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementRandom.java
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

import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;

import java.util.List;

/**
 * Random Placement algorithm to arbitrarily assign locations in a square grid.
 */
public class PlacementRandom extends PlacementFrame
{
	private static final int SPACING = 5;

	/**
	 * Method to return the name of this placement algorithm.
	 * @return the name of this placement algorithm.
	 */
	public String getAlgorithmName() { return "Random"; }

	/**
	 * Method to do Random Placement.
	 * @param nodesToPlace a list of all nodes that are to be placed.
	 * @param allNetworks a list of all networks that connect the nodes.
	 * @param cellName the name of the cell being placed.
	 */
	public void runPlacement(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks, String cellName)
	{
		int numRows = (int)Math.round(Math.sqrt(nodesToPlace.size()));
		double xPos = 0, yPos = 0;
		double maxHeight = 0;
		for(int i=0; i<nodesToPlace.size(); i++)
		{
			PlacementNode plNode = nodesToPlace.get(i);
			plNode.setPlacement(xPos, yPos);
			xPos += plNode.getWidth() + SPACING;
			maxHeight = Math.max(maxHeight, plNode.getHeight());
			if ((i%numRows) == numRows-1)
			{
				xPos = 0;
				yPos += maxHeight + SPACING;
				maxHeight = 0;
			}
		}
	}
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BBThread.java
 * Written by Team 4: Benedikt Mueller, Richard Fallert
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.placement.genetic2.metrics;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.PlacementFrame.PlacementPort;
import com.sun.electric.tool.placement.genetic2.Block;

import java.util.List;
import java.util.Map;


public class BBThread implements Runnable
{
	private double badness;
	private List<PlacementNetwork> allNetworks;
	private Map<PlacementNode, Integer> nodeBlocks;
	private Block[] allBlocks;
	private int left;
	private int right;
	
	BBThread(List<PlacementNetwork> w, Block[] blocks, Map<PlacementNode, Integer> nBlocks, int a, int b)
	{
		badness = 0.0;
		allNetworks = w;
		allBlocks = blocks;
		nodeBlocks = nBlocks;
		left = a;
		right = b;
	}
	
	public void run()
	{
		double completeLength = 0.0;
		// iterate over all networks and calculate the semiperimeter lengths
		//for(PlacementNetwork w : allNetworks)
		for(int index=left; index < right; index++)
		{
			PlacementNetwork w = allNetworks.get(index);
			
			List<PlacementPort> pp = w.getPortsOnNet();
			if(w.getPortsOnNet().size() == 0) System.out.println("HELP");
			
			
			double left = Double.POSITIVE_INFINITY,
			       right = Double.NEGATIVE_INFINITY, 
			       top = Double.NEGATIVE_INFINITY, 
			       bottom = Double.POSITIVE_INFINITY;
			
			// iterate over all PlacementPorts and calculate their bounding box
			for(PlacementPort p : pp)
			{
				int blockId = nodeBlocks.get(p.getPlacementNode()).intValue();
				
				// these functions give us non-rotated offsets
				// because we have not set a rotation for the nodes
				double offX = p.getRotatedOffX();
				double offY = p.getRotatedOffY();
				
				// manually rotate the port offsets according to the Block rotation
				if(allBlocks[blockId].getOrientation() == Orientation.R)
				{
					double t = offX;
					offX = -offY;
					offY = t;
				}
				else if(allBlocks[blockId].getOrientation() == Orientation.RR)
				{
					double t = offX;
					offX = offY;
					offY = -t;
				}
				else if(allBlocks[blockId].getOrientation() == Orientation.RRR)
				{
					offX = -offX;
					offY = -offY;
				}
				
				// the x,y coordinates of the PlacementPort according to the Individual
				double xpos = allBlocks[blockId].getX() + offX;
				double ypos = allBlocks[blockId].getY() + offY;
				
				// calculates the bounding box
				if(xpos < left)
				{
					left = xpos;
				}
				if(xpos > right)
				{
					right = xpos;
				}
				if(ypos > top)
				{
					top = ypos;
				}
				if(ypos < bottom)
				{
					bottom = ypos;
				}
			}
			
			completeLength += (right-left)+(top-bottom);
		}
		badness = completeLength;
	}
	
	public double getBadness(){
		return badness;
	}

}

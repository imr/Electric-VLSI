/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BBMetric.java
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

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Class for evaluating individuals in genetic algorithm and final placement solution.
 * This class uses bounding boxes of networks to estimate the wire length.
 */
public class BBMetric
{
	private final boolean PARALLEL = false;
	private List<PlacementNetwork> allNetworks;
	private Map<PlacementNode, Integer> nodeBlocks;
	
	public BBMetric(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks)
	{
		this.allNetworks = allNetworks;
		nodeBlocks = new HashMap<PlacementNode, Integer>();		
		Iterator<PlacementNode> it = nodesToPlace.iterator();
		for(int i = 0; i < nodesToPlace.size(); i++)
		{
			nodeBlocks.put(it.next(), new Integer(i));
		}
	}
	
	/**
	 * Method to evaluate a placement given by a blocks array.
	 * @param blocks the genome of an individual.
	 * @return the estimated wire length of the corresponding placement.
	 */
	public double compute(Block[] blocks)
	{
		double completeLength = 0.0;		
		if(PARALLEL)
		{
			BBThread bbThread1 = new BBThread(allNetworks, blocks, nodeBlocks, 0, allNetworks.size()>>1);
			Thread t1 = new Thread(bbThread1);
			t1.start();
			
			BBThread bbThread2 = new BBThread(allNetworks, blocks, nodeBlocks, (allNetworks.size()>>1)+1, allNetworks.size());
			Thread t2 = new Thread(bbThread2);
			t2.start();
			
			try {
				t1.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				t2.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			completeLength += bbThread1.getBadness();
			completeLength += bbThread2.getBadness();
		}
		else
		{
			// iterate over all networks and calculate the semiperimeter lengths
			for(PlacementNetwork w : allNetworks)
			{
				List<PlacementPort> pp = w.getPortsOnNet();
				if(w.getPortsOnNet().size() == 0) 
				{
					System.out.println("HELP");	
				}				
				double left = Double.POSITIVE_INFINITY,
				       right = Double.NEGATIVE_INFINITY, 
				       top = Double.NEGATIVE_INFINITY, 
				       bottom = Double.POSITIVE_INFINITY;
				
				// iterate over all PlacementPorts and calculate their bounding box
				for(PlacementPort p : pp)
				{
					int blockId = nodeBlocks.get(p.getPlacementNode()).intValue();
					
					// these functions give us non-rotated offsets
					double offX = p.getOffX();
					double offY = p.getOffY();
					
					Orientation o = blocks[blockId].getOrientation();
					
					if (o != Orientation.IDENT)
					{
						AffineTransform trans = o.pureRotate();

						Point2D offset = new Point2D.Double(offX, offY);
						trans.transform(offset, offset);
						offX = offset.getX();
						offY = offset.getY();
					}
					
					// the x,y coordinates of the PlacementPort according to the Individual
					double xpos = blocks[blockId].getX() + offX;
					double ypos = blocks[blockId].getY() + offY;
					
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
		}		
		return completeLength;
	}
	
	/**
	 * Method to evaluate a placement given by the nodesToPlace.
	 * @return the estimated wire length of the placement.
	 */
	public double compute()
	{
		double completeLength = 0.0;		

		// iterate over all networks and calculate the semiperimeter lengths
		List<PlacementPort> pp;
		double l,r,u,d;
		for(PlacementNetwork w : allNetworks)
		{
			pp = w.getPortsOnNet();
			if(w.getPortsOnNet().size() == 0) 
			{
				System.exit(-1337);
			}
			
			l = (pp.get(0).getPlacementNode().getPlacementX() + pp.get(0).getRotatedOffX());
			r = (pp.get(0).getPlacementNode().getPlacementX() + pp.get(0).getRotatedOffX());
			u = (pp.get(0).getPlacementNode().getPlacementY() + pp.get(0).getRotatedOffY());
			d = (pp.get(0).getPlacementNode().getPlacementY() + pp.get(0).getRotatedOffY());
			
			// iterate over all PlacementPorts and calculate their bounding box
			for(PlacementPort p : pp)
			{
				double xpos = p.getPlacementNode().getPlacementX() + p.getRotatedOffX();
				double ypos = p.getPlacementNode().getPlacementY() + p.getRotatedOffY();
				if(xpos < l)
				{
					l = xpos;
				}
				else
				{
					if(xpos > r)
					{
						r = xpos;
					}
				}
				if(ypos < d)
				{
					d = ypos;
				}
				else
				{
					if(ypos > u)
					{
						u = ypos;
					}
				}
			}
			completeLength += (r-l)+(u-d);
		}		
		return completeLength;
	}
}

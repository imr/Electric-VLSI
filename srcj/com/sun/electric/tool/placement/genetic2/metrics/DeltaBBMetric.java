/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DeltaBBMetric.java
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
import com.sun.electric.tool.placement.genetic2.Reference;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Class for evaluating delta-individuals in genetic algorithm and final placement solution.
 * This class uses bounding boxes of networks to estimate the wire length.
 */
public class DeltaBBMetric
{
	static List<PlacementNode> nodesToPlace;
	static List<PlacementNetwork> allNetworks;
	static Reference ref;
	static List<Integer>[] networksOfNode; 
	public static Map<PlacementNode, Integer> nodeBlocks;
	
	public static void init(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks)
	{
		DeltaBBMetric.allNetworks = allNetworks;
		DeltaBBMetric.nodesToPlace = nodesToPlace;
		
		nodeBlocks = new HashMap<PlacementNode, Integer>();		
		Iterator<PlacementNode> it = nodesToPlace.iterator();
		for(int i = 0; i < nodesToPlace.size(); i++)
		{
			nodeBlocks.put(it.next(), new Integer(i));
		}
		
		networksOfNode = new LinkedList[nodesToPlace.size()];
		for(int i = 0; i < networksOfNode.length; i++)
		{
			networksOfNode[i] = new LinkedList<Integer>();
		}
		
		PlacementNode n;
		for(int i = 0; i < allNetworks.size(); i++)
		{
			PlacementNetwork w = allNetworks.get(i);
			List<PlacementPort> pp = w.getPortsOnNet();
			
			for(PlacementPort p : pp)
			{	
				n = p.getPlacementNode();
				Integer ii = new Integer(i);
				if(!networksOfNode[nodeBlocks.get(n).intValue()].contains(ii))
					networksOfNode[nodeBlocks.get(n).intValue()].add(ii);
			}
		}
	}
	
	public static void setRef(Reference ref)
	{
		DeltaBBMetric.ref = ref;
	}
	
	/**
	 * Method to evaluate a placement given by a blocks array.
	 * @param blocks the genome of an individual.
	 * @return the estimated wire length of the corresponding placement.
	 */
	public static double compute(List<Block> blocks, double[] netLengths)
	{
		double completeLength = ref.netLength;		
	
		HashSet<Integer> changedNet = new HashSet<Integer>();

		
		for(Block b : blocks)
		{
			for(Integer j : networksOfNode[b.getNr()])
			{
				changedNet.add(j);
			}
		}

		for(Integer i : changedNet)
		{
			PlacementNetwork w = allNetworks.get(i.intValue());
			
			List<PlacementPort> pp = w.getPortsOnNet();
			
			double left = Double.POSITIVE_INFINITY,
			       right = Double.NEGATIVE_INFINITY, 
			       top = Double.NEGATIVE_INFINITY, 
			       bottom = Double.POSITIVE_INFINITY;
			
			// iterate over all PlacementPorts and calculate their bounding box
			for(PlacementPort p : pp)
			{
				
				int blockId = nodeBlocks.get(p.getPlacementNode()).intValue();//nodeBlocks.get(p.getPlacementNode());
				
				// these functions give us non-rotated offsets
				double offX = p.getOffX();
				double offY = p.getOffY();
				
				Block b = null;
				
				for(Block currBlock : blocks) if(currBlock.getNr() == blockId) b = currBlock;
				
				Orientation o;
				if(b != null) o = b.getOrientation();
				else o = p.getPlacementNode().getPlacementOrientation();
				
				if (o != Orientation.IDENT)
				{
					AffineTransform trans = o.pureRotate();

					Point2D offset = new Point2D.Double(offX, offY);
					trans.transform(offset, offset);
					offX = offset.getX();
					offY = offset.getY();
				}
				
				// the x,y coordinates of the PlacementPort according to the Individual
				double xpos;
				if(b != null) xpos = b.getX() + offX;
				else xpos = p.getPlacementNode().getPlacementX() + offX;
				
				double ypos;
				if(b != null) ypos = b.getY() + offY;
				else ypos = p.getPlacementNode().getPlacementY() + offY;

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
			
			netLengths[i.intValue()] = (right-left)+(top-bottom);
			//completeLength += (right-left)+(top-bottom);
			completeLength += netLengths[i.intValue()];
			completeLength -= ref.netLengths[i.intValue()];

		}
	
		return completeLength;
	}
	
	// non-accelerated version (deprecated)
	public static double old_compute(List<Block> blocks, double[] netLengths)
	{
		double completeLength = 0.0;		
		int n = 0;
	
		// iterate over all networks and calculate the semiperimeter lengths
		for(PlacementNetwork w : allNetworks)
		{
			List<PlacementPort> pp = w.getPortsOnNet();
	
			double left = Double.POSITIVE_INFINITY,
			       right = Double.NEGATIVE_INFINITY, 
			       top = Double.NEGATIVE_INFINITY, 
			       bottom = Double.POSITIVE_INFINITY;
			
			// iterate over all PlacementPorts and calculate their bounding box
			for(PlacementPort p : pp)
			{
				
				int blockId = nodeBlocks.get(p.getPlacementNode()).intValue();//nodeBlocks.get(p.getPlacementNode());
				
				// these functions give us non-rotated offsets
				double offX = p.getOffX();
				double offY = p.getOffY();
				
				Block b = null;
				
				for(Block currBlock : blocks) if(currBlock.getNr() == blockId) b = currBlock;
				
				Orientation o;
				if(b != null) o = b.getOrientation();
				else o = p.getPlacementNode().getPlacementOrientation();
				
				if (o != Orientation.IDENT)
				{
					AffineTransform trans = o.pureRotate();

					Point2D offset = new Point2D.Double(offX, offY);
					trans.transform(offset, offset);
					offX = offset.getX();
					offY = offset.getY();
				}
				
				// the x,y coordinates of the PlacementPort according to the Individual
				double xpos;
				if(b != null) xpos = b.getX() + offX;
				else xpos = p.getPlacementNode().getPlacementX() + offX;
				
				double ypos;
				if(b != null) ypos = b.getY() + offY;
				else ypos = p.getPlacementNode().getPlacementY() + offY;

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
			netLengths[n] = (right-left)+(top-bottom);
			n++;
		}
		
		return completeLength;
	}
	
	/**
	 * Method to evaluate a placement given by the nodesToPlace.
	 * @return the estimated wire length of the placement.
	 */
	public static double compute()
	{
		double completeLength = 0.0;		

		// iterate over all networks and calculate the semiperimeter lengths
		List<PlacementPort> pp;
		double l,r,u,d;
		int n = 0;
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
			ref.netLengths[n] = (r-l)+(u-d);
			n++;
		}		
		return completeLength;
	}
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UniformGrid.java
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
package com.sun.electric.tool.placement.genetic2;

import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This is a data structure for the acceleration of collision detection,
 * it speeds up the calculation of overlap area.
 * @see DeltaIndividual
 */
public class UniformGrid
{
	LinkedList<Integer>[][] grid;
	LinkedList<Integer>[] cellOfNode;
	
	// bounding box of the reference placement.
	double left;
	double right;
	double top;
	double bottom;
	
	double cellW; // maximum width OR height of all PlacementNodes in the reference placement.
	
	int w, h; // grid size
	
	UniformGrid(List<PlacementNode> nodesToPlace, double left, double right, double top, double bottom, double cellW)
	{
		cellOfNode = new LinkedList[nodesToPlace.size()];
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
		this.cellW = cellW;
		w = (int)Math.ceil((right-left)/cellW);
		h = (int)Math.ceil((top-bottom)/cellW);
		
		// System.out.println("Grid size w: " + w + " h: " + h);
		grid = new LinkedList[w][h];
	}
	
	public int getCellX(double xPos)
	{
		int x = (int)((xPos-left)/cellW);
		
		if(x < 0) x = 0;
		if(x >= w) x = w-1;
		return x;
	}
	
	public int getCellY(double yPos)
	{
		int y = (int)((yPos-bottom)/cellW);
		
		if(y < 0) y = 0;
		if(y >= h) y = h-1;
		return y;
	}
	
	/**
	 * Insert index for a PlacementNode.
	 * @param i index of the PlacementNode.
	 * @param xPos x-coordinate of the PlacementNode.
	 * @param yPos y-coordinate of the PlacementNode.
	 */
	public LinkedList<Integer> insert(int i, double xPos, double yPos)
	{
		
		int x = getCellX(xPos);
		int y = getCellY(yPos);

		if(grid[x][y] == null)
		{
			grid[x][y] = new LinkedList<Integer>();
		}
		
		grid[x][y].add(new Integer(i));
		cellOfNode[i] = grid[x][y];
		
		LinkedList<Integer> cellList = cellOfNode[i];

		return cellList;
	}
	
	public void remove(int nodeIndex)
	{
		LinkedList<Integer> cellList = cellOfNode[nodeIndex];
		//System.out.println("before: " + cellList);
		if(cellList == null) System.out.println("ARRAY PANIC in remove in UniformGrid");
		Iterator<Integer> it = cellList.iterator();
		
		while(it.hasNext())
		{
			if(it.next().intValue() == nodeIndex)
			{
				it.remove();
				//System.out.println("after: " + cellList);
				return;
			}
		}
	}
	
	/**
	 * Gets the change in collision area for the delta-position of the delta-block.
	 * ( Compared to the original position of the PlacementNode in the reference placement. )
	 * @param b Delta-block whose overlap is to be updated.
	 * @param deltas List of all delta-blocks.
	 * @param nodesToPlace All PlacementNodes of the reference placement.
	 * @param overlaps Partial overlaps for potential diversity comparison in the evolution.
	 */
	public double collide(Block b, List<Block> deltas, List<PlacementNode> nodesToPlace, double[] overlaps)
	{
		double overlap = 0.0;
	
		int x = getCellX(b.getX());
		int y = getCellY(b.getY());

		
		Block orig = new Block();
		orig.valuesFrom(nodesToPlace.get(b.getNr()));
		orig.number = b.getNr();
		
		int oldX = getCellX(orig.getX());
		int oldY = getCellY(orig.getY());
		
		
		// check the cell to which the Block belongs and its 8 neighbors for collision.
		// This is done first to calculate the new collision and a second time to subtract
		// the overlap of the original of the deltablock in the reference placement.
		for(int i = -1; i <= 1; i++)
		{
			for(int j = -1; j <= 1; j++)
			{
				// new overlap:
				overlap += collideCell(b, deltas, nodesToPlace, overlaps, x+i, y+j, false);
				
				// old overlap of original:
				overlap -= collideCell(orig, deltas, nodesToPlace, overlaps, oldX+i, oldY+j, true);
			}
		}
		
		return overlap;
	}
	
	/**
	 * Gets the change in collision area for the delta-block in a single grid cell.
	 * @param b Delta-block whose overlap is to be updated.
	 * @param deltas List of all delta-blocks.
	 * @param nodesToPlace All PlacementNodes of the reference placement.
	 * @param overlaps Partial overlaps for potential diversity comparison in the evolution.
	 * @param x x-coordinate of the grid cell to check for collisions.
	 * @param y y-coordinate of the grid cell.
	 */
	public double collideCell(Block b, List<Block> deltas, List<PlacementNode> nodesToPlace, 
			double[] overlaps, int x, int y, boolean oldBlock)
	{
		if(x < 0 || x >= w || y < 0 || y >= h) return 0.0;
		if(grid[x][y] == null) return 0.0;

		double overlap = 0.0;
		Block other = new Block();

		Iterator<Block> iB = deltas.iterator();
		Block deltaBlock = iB.next();
		
		for(Integer i : grid[x][y])
		{
			if(i.intValue() != b.getNr())
			{
				other.valuesFrom(nodesToPlace.get(i.intValue()));
				
				while(deltaBlock.getNr() < i.intValue() && iB.hasNext())
				{
					deltaBlock = iB.next();
				}
				
				if(deltaBlock.getNr() != i.intValue()) // only collide with the non-deltas. the deltas don't use this grid
				{
					//overlap -= orig.intersectionArea(other);
					overlap += b.intersectionArea(other);
					
					if(oldBlock)
					{
						overlaps[b.getNr()] -= b.intersectionArea(other);
					}
					else
					{
						overlaps[b.getNr()] += b.intersectionArea(other);
					}
				}
			}
		}
		
		return overlap;
	}
	
	/**
	 * Gets the collision area for the Block b with the previous nodes.
	 * ( Compared to the original position of the PlacementNode in the reference placement. )
	 * @param nodesToPlace All PlacementNodes of the reference placement.
	 * @param overlaps Partial overlaps for potential diversity comparison in the evolution.
	 */
	public double collide(int nodeId, List<PlacementNode> nodesToPlace, double[] overlaps)
	{
		double overlap = 0.0;
		
		PlacementNode n = nodesToPlace.get(nodeId);
		Block b = new Block();
		b.valuesFrom(n);
		b.number = nodeId;
		
		int x = getCellX(b.getX());
		int y = getCellY(b.getY());
		
		// check the cell to which the Block belongs and its 8 neighbors for collision.
		for(int i = -1; i <= 1; i++)
		{
			for(int j = -1; j <= 1; j++)
			{
//				double t = overlap;
				overlap += collideCell(b, nodesToPlace, overlaps, x+i, y+j);
				
			}
		}
		
		return overlap;
	}
	
	/**
	 * Gets the collision area for the Block b with the previous nodes.
	 * @param b The values of a PlacementNode, inserted into a block.
	 * @param nodesToPlace All PlacementNodes of the reference placement.
	 * @param overlaps Partial overlaps for potential diversity comparison in the evolution.
	 * @param x x-coordinate of the grid cell to check for collisions.
	 * @param y y-coordinate of the grid cell.
	 */
	public double collideCell(Block b, List<PlacementNode> nodesToPlace, 
			double[] overlaps, int x, int y)
	{
		if(x < 0 || x >= w || y < 0 || y >= h) return 0.0;
		if(grid[x][y] == null) return 0.0;

		double overlap = 0.0;
		Block other = new Block();
		
		for(Integer i : grid[x][y])
		{
			if(i.intValue() >= b.getNr()) return overlap;
			
			other.valuesFrom(nodesToPlace.get(i.intValue()));

			overlap += b.intersectionArea(other);

			overlaps[b.getNr()] += b.intersectionArea(other);
		}
		
		return overlap;
	}
	
	public void draw(Graphics g, double scale)
	{
		for(int i = 0; i < w; i++)
		{
			for(int j = 0; j < h; j++)
			{
				g.setColor(new Color(i/(float)w, j/(float)h, 0.3f, 0.7f));
				//g.fillRect((int)(left*scale+i*cellW*scale), (int)(bottom*scale+j*cellW*scale), (int)(cellW*scale), (int)(cellW*scale));
				g.drawRect((int)(left*scale+i*cellW*scale), (int)(bottom*scale+j*cellW*scale), (int)(cellW*scale), (int)(cellW*scale));
				g.drawString("("+i+","+j+")", (int)(left*scale+i*cellW*scale), (int)(bottom*scale+j*cellW*scale));
			}
		}
	}
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Block.java
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

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;

import java.util.Random;

/**
 * A Block is a simplified PlacementNode for the genome of an Individual in a genetic algorithm.
 * @see Individual
 */
public class Block implements Comparable<Block>
{
	Orientation o;
	private double width, height, halfWidth, halfHeight;
	private double xPos, yPos;
	
	int number = -1; // index of the PlacementNode to which this Block corresponds to

	public Block()
	{
	}
	
	public Block(Block b)
	{
		xPos = b.xPos;
		yPos = b.yPos;
		width = b.width;
		height = b.height;
		halfWidth = b.halfWidth;
		halfHeight = b.halfHeight;
		o = b.o;
		number = b.number;
	}
	
	public Block(double x, double y, double w, double h)
	{
		width = w;
		halfWidth = width/2.0;
		height = h;
		halfHeight = height/2.0;
		xPos = x;
		yPos = y;
		o = Orientation.IDENT;
	}
	
	public Block(double x, double y, double w, double h, Orientation o, int number)
	{
		width = w;
		halfWidth = width/2.0;
		height = h;
		halfHeight = height/2.0;
		xPos = x;
		yPos = y;
		this.o = o;
		this.number = number;
	}
	
	public void valuesFrom(PlacementNode n)
	{
		xPos = n.getPlacementX();
		yPos = n.getPlacementY();
		width = n.getWidth();
		halfWidth = width/2.0;
		height = n.getHeight();
		halfHeight = height/2.0;
		o = n.getPlacementOrientation();
	}
	
	/**
	 * Method to compare the number of blocks
	 * @param other The other Block
	 * @return -1 if other has a higher number, 1 if other has lower one, else 0.
	 */
	public int compareTo(Block other)
	{
		if(getNr() < other.getNr())
		{
			return -1;
		}
		else if(getNr() > other.getNr())
		{
			return 1;
		}
		else return 0;
	}
	
	/**
	 * Method to get the index of the node to which this Block corresponds.
	 * @return The index of the corresponding node in the nodesToPlace list.
	 */
	public int getNr() { return number; }
	
	public Orientation getOrientation()
	{
		return o; 
	}
	
	public void setOrientation(Orientation o) 
	{
		this.o = o; 
	}

	/**
	 * Method to return the width of this Block.
	 * @return the width of this Block.
	 */
	public double getWidth() 
	{
		return width;
	}

	/**
	 * Method to return the height of this Block.
	 * @return the height of this Block.
	 */
	public double getHeight()
	{
		return height;
	}
	
	/**
	 * Method to return the x-coordinate of this Block.
	 * @return the x-coordinate of this Block.
	 */
	public double getX() 
	{ 
		return xPos; 
	}
	
	/**
	 * Method to return the y-coordinate of this Block.
	 * @return the y-coordinate of this Block.
	 */
	public double getY() 
	{ 
		return yPos;
	}

	/**
	 * Method to get the left x-coordinate of this rotated Block.
	 * @return The x-coordinate of the left border of the rotated Block.
	 */
	public double getLeft()
	{
		if(o == Orientation.R || o == Orientation.RRR
				|| o == Orientation.XR || o == Orientation.XRRR
				|| o == Orientation.YR || o == Orientation.YRRR
				|| o == Orientation.XYR || o == Orientation.XYRRR)
		{
			return xPos - halfHeight;
		}
		else
		{
			return xPos - halfWidth;
		}
	}

	/**
	 * Method to get the top y-coordinate of this rotated Block.
	 * @return The y-coordinate of the top border of the rotated Block.
	 */
	public double getTop()
	{
		if(o == Orientation.R || o == Orientation.RRR
				|| o == Orientation.XR || o == Orientation.XRRR
				|| o == Orientation.YR || o == Orientation.YRRR
				|| o == Orientation.XYR || o == Orientation.XYRRR)
		{
			return yPos + halfWidth;
		}
		else
		{
			return yPos + halfHeight;
		}
	}

	/**
	 * Method to get the right x-coordinate of this rotated Block.
	 * @return The x-coordinate of the right border of the rotated Block.
	 */
	public double getRight()
	{
		if(o == Orientation.R || o == Orientation.RRR
				|| o == Orientation.XR || o == Orientation.XRRR
				|| o == Orientation.YR || o == Orientation.YRRR
				|| o == Orientation.XYR || o == Orientation.XYRRR)
		{
			return xPos + halfHeight;
		}
		else
		{
			return xPos + halfWidth;
		}
	}
	
	/**
	 * Method to get the bottom y-coordinate of this rotated Block.
	 * @return The y-coordinate of the bottom border of the rotated Block.
	 */
	public double getBottom()
	{
		if(o == Orientation.R || o == Orientation.RRR
				|| o == Orientation.XR || o == Orientation.XRRR
				|| o == Orientation.YR || o == Orientation.YRRR
				|| o == Orientation.XYR || o == Orientation.XYRRR)
		{
			return yPos - halfWidth;
		}
		else
		{
			return yPos - halfHeight;
		}
	}
	
	/**
	 * Method to check this Block for intersection with another.
	 * @param other The other Block to check this one for intersection with.
	 * @return Whether this Block intersects the other.
	 */
	public boolean intersects(Block other)
	{
		return ( other.getLeft() < getRight()
				&& other.getRight() > getLeft()
				&& other.getTop() > getBottom()
				&& other.getBottom() < getTop() );
	}

	/**
	 * Method to calculate the intersection area with another Block.
	 * @param other The other block which possibly intersects this Block.
	 * @return The intersection area - 0.0 if empty.
	 */
	public double intersectionArea(Block other)
	{
		if (intersects(other))
		{

			double left = Math.max(getLeft(), other.getLeft());
			double top = Math.min(getTop(), other.getTop());
			double right = Math.min(getRight(), other.getRight());
			double bottom = Math.max(getBottom(), other.getBottom());
			
			return (top-bottom)*(right-left);
		} 
		else return 0.0;
	}
	
	/**
	 * Method to set the location of this Block.
	 * @param x the X-coordinate of the center of this Block.
	 * @param y the Y-coordinate of the center of this Block.
	 */
	public void setPos(double x, double y) 
	{ 
		xPos = x;
		yPos = y; 
	}
	
	/**
	 * Method to mutate this Block.
	 * @param rand The random number generator of the calling thread.
	 */
	public void disturb(double avgW, Random rand)
	{
		// random (gaussian) mutation of position
		xPos += 1.0*avgW * rand.nextGaussian();
		yPos += 1.0*avgW * rand.nextGaussian();
	}
	
	public void disturbToCenter(double avgW, Random rand)
	{
		// random mutation of position in direction of center
		xPos = xPos *= rand.nextDouble();
		yPos = yPos *= rand.nextDouble();
	}

	
	public void disturbOrientation(Random rand)
	{
		switch(rand.nextInt(16))
		{
		case 0:
			o = Orientation.IDENT;
			break;
		case 1:
			o = Orientation.R;
			break;
		case 2:
			o = Orientation.RR;
			break;
		case 3:
			o = Orientation.RRR;
			break;
			
		case 4:
			o = Orientation.X;
			break;
		case 5:
			o = Orientation.XR;
			break;
		case 6:
			o = Orientation.XRR;
			break;
		case 7:
			o = Orientation.XRRR;
			break;
			
		case 8:
			o = Orientation.Y;
			break;
		case 9:
			o = Orientation.YR;
			break;
		case 10:
			o = Orientation.YRR;
			break;
		case 11:
			o = Orientation.YRRR;
			break;
			
		case 12:
			o = Orientation.XY;
			break;
		case 13:
			o = Orientation.XYR;
			break;
		case 14:
			o = Orientation.XYRR;
			break;
		case 15:
			o = Orientation.XYRRR;
			break;
		}
	}
}


/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Reference.java
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
import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.genetic2.metrics.DeltaBBMetric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Class representing the nodesToPlace and the reference placement for DeltaIndividuals.
 */
public class Reference
{
	double p = 0.0;
	public double[] badnessComponents;
	public double[] overlaps;
	public double[] netLengths;
	public double overlap;
	public double netLength;
	public double spread;
	public UniformGrid grid;
	public List<PlacementNode> nodesToPlace;
	public List<PlacementNetwork> allNetworks;
	Random rand;
	public double avgW;
	
	
	public Reference(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks, Random rand)
	{
		this.nodesToPlace = nodesToPlace;
		this.allNetworks = allNetworks;
		this.rand = rand;
		
		overlap = 0.0;
		overlaps = new double[nodesToPlace.size()];
		netLengths = new double[allNetworks.size()];
		badnessComponents = new double[3];
		
		init();
		
		createGrid();
		calculateRefOverlap();
		
		badnessComponents[0] = netLength;
		badnessComponents[1] = overlap;
	}
	
	public void init()
	{
		PlacementNode n;
		
		for(int i = 0; i < nodesToPlace.size(); i++)
		{
			n = nodesToPlace.get(i);
			avgW += (Math.max(n.getWidth(), n.getHeight()))/nodesToPlace.size();
		}
		//System.out.println("avgW: " + avgW);
		
		/*
		 // randomize first reference placement:
		for(int i = 0; i < nodesToPlace.size(); i++)
		{
			n = nodesToPlace.get(i);
			n.setPlacement(rand.nextGaussian()*2000.0, rand.nextGaussian()*2000.0);
			n.setOrientation(Orientation.IDENT);
		}
		*/
		/*
		for(int i = 0; i < nodesToPlace.size(); i++)
		{
			n = nodesToPlace.get(i);
			n.setPlacement((1.0-2.0*rand.nextDouble())*0.5*avgW*Math.sqrt(nodesToPlace.size()), 
					(1.0-2.0*rand.nextDouble())*0.5*avgW*Math.sqrt(nodesToPlace.size()));
			n.setOrientation(Orientation.IDENT);
		}
		*/
		
		// rasterize first reference placement:
		int l = (int) Math.sqrt(nodesToPlace.size());
		double s = 0.0;
		double m = 0.0;
		for(int i = 0; i < nodesToPlace.size(); i++)
		{
			m = Math.max(nodesToPlace.get(i).getHeight(),nodesToPlace.get(i).getWidth());
			s = Math.max(s,m);
		}
		
			
		for(int i = 0; i < nodesToPlace.size(); i++)
		{
			n = nodesToPlace.get(i);
			n.setPlacement( -l/2*s + (i%l) * s,
		        			-l/2*s + (int)((double)i/(double)l) * s );
			n.setOrientation(Orientation.IDENT);
		}
		
		
	}
	
	public double getSemiperimeterLength()
	{
		Block b = new Block();
		b.valuesFrom(nodesToPlace.get(0));
		double left = b.getLeft();
		double top = b.getTop();
		double right = b.getRight();
		double bottom = b.getBottom();
		
		for(int i = 0; i < nodesToPlace.size(); i++)
		{
			b.valuesFrom(nodesToPlace.get(i));
			if(b.getLeft() < left) left = b.getLeft();
			if(b.getTop() > top) top = b.getTop();
			if(b.getRight() > right) right = b.getRight();
			if(b.getBottom() < bottom) bottom = b.getBottom();
		}
		return (top-bottom)+(right-left);
	}
	
	public double calculateSpread()
	{
		spread = 0.0;
		Block b = new Block();
		
		for(int i = 0; i < nodesToPlace.size(); i++)
		{
			b.valuesFrom(nodesToPlace.get(i));
			spread+=Math.sqrt(b.getX()*b.getX() + b.getY()*b.getY())
					* b.getWidth()*b.getHeight();;
		}
		return spread;
	}
	
	public void setProgress(double p)
	{
		this.p = p;
	}
	
	public double getBadness()
	{
		double badness = 0.0;
		
		// wire length estimate:
		badness += badnessComponents[0];
		
		// overlap:
		badness += badnessComponents[1]*(1.0+300.0*p*p);
		
		// area:
		// badness += badnessComponents[2]*0.1;
		badness += 0.00001*spread;

		return badness;
	}
	
	public double[] getBadnessComponents()
	{
		return badnessComponents;
	}
	
	public void calculateFirstTime()
	{
		createGrid();
		calculateRefOverlap();
		calculateRefNetLength();
		calculateSpread();
		
		badnessComponents[0] = netLength; // update refNetLengths
		badnessComponents[1] = overlap;
	}
	
	public void calculateRefOverlap()
	{
		overlap = 0.0;
		
		for(int i = 0; i < nodesToPlace.size(); i++)
		{
			overlap+=grid.collide(i, nodesToPlace, overlaps);
		}
	}
	
	public double getNaiveOverlap()
	{
		Block a = new Block();
		Block b = new Block();
		PlacementNode n1;
		PlacementNode n2;
		double naiveOverlap = 0.0;
		
		for(int i = 0; i < nodesToPlace.size(); i++)
		{
			n1 = nodesToPlace.get(i);
			a.valuesFrom(n1);
			
			for(int j = 0; j < i; j++)
			{
				if(i!=j)
				{
					n2 = nodesToPlace.get(j);
					b.valuesFrom(n2);
					naiveOverlap += a.intersectionArea(b);
				}
			}
		}
		//System.out.println("naiveOverlap: " + naiveOverlap);
		return naiveOverlap;
	}
	
	public void calculateRefNetLength()
	{
		netLength = DeltaBBMetric.compute();
		//System.out.println("refNetLength: " + netLength);
	}
	public void update(DeltaIndividual indi)
	{
		netLength = indi.getBadnessComponents()[0];
		
		overlap = indi.getBadnessComponents()[1];
		
		netLengths = new double[indi.netLengths.length];
		for(int i = 0; i < indi.netLengths.length; i++) netLengths[i] = indi.netLengths[i];
		overlaps = new double[indi.overlaps.length];
		for(int i = 0; i < indi.overlaps.length; i++) overlaps[i] = indi.overlaps[i];
		
		spread = indi.getBadnessComponents()[2];

		for(Block b : indi.blocks)
		{
			grid.remove(b.getNr());
			
			Collections.sort(grid.insert(b.getNr(), b.getX(), b.getY()));
		
			nodesToPlace.get(b.getNr()).setPlacement(b.getX(), b.getY());
			nodesToPlace.get(b.getNr()).setOrientation(b.getOrientation());
		}

		
		badnessComponents[0] = netLength;
		badnessComponents[1] = overlap;
		badnessComponents[2] = spread;
	}
	/*
	public void update(DeltaIndividual indi)
	{
		netLength = indi.getBadnessComponents()[0];
		netLengths = Arrays.copyOf(indi.netLengths, indi.netLengths.length);
		
		overlap = indi.getBadnessComponents()[1];
		overlaps = Arrays.copyOf(indi.overlaps, indi.overlaps.length);
		
		spread = indi.getBadnessComponents()[2];

		for(Block b : indi.blocks)
		{
			grid.remove(b.getNr());
			
			Collections.sort(grid.insert(b.getNr(), b.getX(), b.getY()));
		
			nodesToPlace.get(b.getNr()).setPlacement(b.getX(), b.getY());
			nodesToPlace.get(b.getNr()).setOrientation(b.getOrientation());
		}

		
		badnessComponents[0] = netLength;
		badnessComponents[1] = overlap;
		badnessComponents[2] = spread;
		refVersion++;
	}
	*/
	/**
	 * Creates the uniform grid for fast overlap calculation.
	 */
	public void createGrid()
	{
		double left = Double.POSITIVE_INFINITY,
	       right = Double.NEGATIVE_INFINITY, 
	       top = Double.NEGATIVE_INFINITY, 
	       bottom = Double.POSITIVE_INFINITY;
		
		double maxW = 0.0;
		
		// create lists of x,y-Values to calculate the 0.1-quantiles
		ArrayList<Double> xPositions = new ArrayList<Double>();
		ArrayList<Double> yPositions = new ArrayList<Double>();
		
		Block b = new Block();
		for(PlacementNode n : nodesToPlace)
		{
			b.valuesFrom(n);
			xPositions.add(new Double(b.getX()));
			yPositions.add(new Double(b.getY()));
			
			if(b.getWidth() > maxW)
			{
				maxW = b.getWidth();
			}
			if(b.getHeight() > maxW)
			{
				maxW = b.getHeight();
			}
		}
		Collections.sort(xPositions);
		Collections.sort(yPositions);
		
		left = xPositions.get(xPositions.size()/10).doubleValue();
		right = xPositions.get(xPositions.size()-xPositions.size()/10-1).doubleValue();
		
		bottom = yPositions.get(yPositions.size()/10).doubleValue();
		top = yPositions.get(yPositions.size()-yPositions.size()/10-1).doubleValue();
		
		/*
		// version without quantiles (problematic because of outliers
		Block b = new Block();
		for(PlacementNode n : nodesToPlace)
		{
			b.valuesFrom(n);
			if(b.getLeft() < left)
			{
				left = b.getLeft();
			}
			if(b.getRight() > right)
			{
				right = b.getRight();
			}
			if(b.getTop() > top)
			{
				top = b.getTop();
			}
			if(b.getBottom() < bottom)
			{
				bottom = b.getBottom();
			}
			
			if(b.getWidth() > maxW)
			{
				maxW = b.getWidth();
			}
			if(b.getHeight() > maxW)
			{
				maxW = b.getHeight();
			}
		}
		*/
		
		grid = new UniformGrid(nodesToPlace, left, right, top, bottom, maxW);
		for(int i = 0; i < nodesToPlace.size(); i++)
		{
			grid.insert(i, nodesToPlace.get(i).getPlacementX(), nodesToPlace.get(i).getPlacementY());
		}
	}

}

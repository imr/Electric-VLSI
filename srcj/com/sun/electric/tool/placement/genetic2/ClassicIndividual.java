/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ClassicIndividual.java
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
import com.sun.electric.tool.placement.genetic2.metrics.BBMetric;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * A ClassicIndividual contains a complete copy of a placement. This uses a lot of
 * memory and evaluation is slow.
 * 
 * DeltaIndividual is recommended instead.
 * 
 * This class is only provided for comparison.
 * 
 * @see Population
 * @see DeltaIndividual
 */
public class ClassicIndividual extends Individual<ClassicIndividual>
{
	private Block[] blocks; // the genome, contains the positions and rotations of the PlacementNodes
	private double[] badnessComponents = new double[3]; // how bad our individual is
	private double[] hashes;
	Reference ref;

	double[] netLengths;

	private BBMetric m_bb;
	
	public ReadWriteLock rwLock;
	double p;
	
	/**
	 * Constructor to randomly initialize the genome (blocks).
	 * @param nodesToPlace The PlacementNode objects.
	 * @param allNetworks The sets of connected PlacementPort objects.
	 * @param rand The random number generator of the population's thread.
	 */
	ClassicIndividual(Reference ref, Random rand)
	{		
		super(ref);
		this.ref = ref;
		
		rwLock = new ReentrantReadWriteLock();

		m_bb = new BBMetric(nodesToPlace, allNetworks);
		blocks = new Block[nodesToPlace.size()];
		hashes = new double[3];

		
//		Iterator<PlacementNode> it = nodesToPlace.iterator();
		
//		PlacementNode n; 
		for(int i=0; i<nodesToPlace.size(); i++)
		{
			/*
			n = it.next();
			blocks[i] = new Block(
					n.getPlacementX(), n.getPlacementY(),
					n.getWidth(),
					n.getHeight());
			blocks[i].setOrientation(n.getPlacementOrientation());
			*/
			blocks[i] = new Block();
			blocks[i].valuesFrom(nodesToPlace.get(i));
		}
		evaluate();
	}

	public void setProgress(double p)
	{
		this.p = p;
	}
	
	public void reboot(Random rand)
	{
		
	}
	
	public void writeToPlacement(List<PlacementNode> nodesToPlace)
	{
		Block b;
//		Iterator<PlacementNode> it = nodesToPlace.iterator();	
		PlacementNode n; 

		for(int i=0; i<nodesToPlace.size(); i++)
		{
			b = blocks[i];
			//n = it.next();
			n = nodesToPlace.get(i);
			
			n.setPlacement(b.getX(), b.getY());
			n.setOrientation(b.getOrientation());
		}
	}
	
	/**
	 * Method to compare the badness with another individual.
	 * @param other The Individual to be used as a reference.
	 * @return -1 if other has a higher badness, else 1.
	 */
	public int compareTo(ClassicIndividual other)
	{
		if(getBadness() < other.getBadness())
		{
			return -1;
		}
		else 
		{
			return 1;
		}
	}
	
	/**
	 * Method to copy the genes (Blocks) and badness from another Individual.
	 * @param other The Individual to be cloned.
	 */
	public void copyFrom(ClassicIndividual other)
	{
		for(int i = 0; i < blocks.length; i++)
		{
			blocks[i].setPos(other.getBlockAt(i).getX(), other.getBlockAt(i).getY());	
			blocks[i].setOrientation(other.getBlockAt(i).getOrientation());
		}
		setBadness(other.badnessComponents);
		//evaluate();
	}

	/**
	 * Method to do a crossover of two genomes and write the result to the Individual's genome.
	 * Mutation and evaluation is also performed.
	 * @param mom One donor of genes.
	 * @param dad The other donor of genes.
	 */
	public void deriveFrom(ClassicIndividual mom, ClassicIndividual dad, Random rand)
	{
		if(rand.nextDouble() > 0.5)
		{
			// exchange randomly who donates the first part of the genome
			ClassicIndividual t = mom;
			mom = dad;
			dad = t;
		}
		
		// find a random crossover-Point (one point crossover)
//		int crossingPoint = rand.nextInt(blocks.length);
		
		for(int i = 0; i < blocks.length; i++)
		{
			//if(i < crossingPoint)
			if(rand.nextDouble() > 0.5)
			{
				blocks[i].setPos(mom.getBlockAt(i).getX(), mom.getBlockAt(i).getY());
				blocks[i].setOrientation(mom.getBlockAt(i).getOrientation());
			}
			else
			{
				blocks[i].setPos(dad.getBlockAt(i).getX(), dad.getBlockAt(i).getY());
				blocks[i].setOrientation(dad.getBlockAt(i).getOrientation());
			}
				
		}		
		mutate(rand);		
		// recalculate the badness
		evaluate();
	}
	
	public double distance(ClassicIndividual other)
	{
		double distance = 0.0;

		double[] h = other.getHashes();
		distance += (hashes[0]-h[0])*(hashes[0]-h[0]) + (hashes[1]-h[1])*(hashes[1]-h[1]) + (hashes[2]-h[2])*(hashes[2]-h[2]);

		return distance;
	}
	
	/**
	 * Method to get a Block at position i of the genome.
	 * @param i The position of the block to get.
	 * @return The Block at position i.
	 */
	public Block getBlockAt(int i)
	{
		return blocks[i];
	}
	
	/**
	 * Method to randomly swap the position of 2 blocks.
	 */
	public void swapBlocks(int a, int b)
	{
		
		double ax = blocks[a].getX();
		double ay = blocks[a].getY();
		blocks[a].setPos(blocks[b].getX(), blocks[b].getY());
		blocks[b].setPos(ax, ay);
	}
	
	/**
	 * Method to mutate a random amount of Blocks.
	 */
	public void mutate(Random rand)
	{
		int disturbedPositions = Math.abs((int)(rand.nextGaussian()*3.0));
		int disturbedOrientations = Math.abs((int)(rand.nextGaussian()*1.0));
		int swaps = Math.abs((int)(rand.nextGaussian()*1.0));
		
		for(int i = 0; i < disturbedPositions; i++)
		{
			blocks[rand.nextInt(blocks.length)].disturb(ref.avgW, rand); // move cell to a random position
		}
		for(int i = 0; i < disturbedOrientations; i++)
		{
			blocks[rand.nextInt(blocks.length)].disturbOrientation(rand); // change Orientation randomly
		}
		for(int i = 0; i < swaps; i++)
		{
			int pos1 = rand.nextInt(blocks.length);
			int pos2 = rand.nextInt(blocks.length);
			swapBlocks(pos1, pos2); // swap random blocks
		}
	}
	
	/**
	 * Method to mutate a random amount of Blocks.
	 */
	public void mutateAndEvaluate(ClassicIndividual original, Random rand)
	{
		HashSet<Integer> changedBlocks = new HashSet<Integer>();
		int disturbedPositions = Math.abs((int)(rand.nextGaussian()*3.0));
		int disturbedOrientations = Math.abs((int)(rand.nextGaussian()*1.0));
		int swaps = Math.abs((int)(rand.nextGaussian()*1.0));
		
		for(int i = 0; i < disturbedPositions; i++) // TODO +1 (at least one disturbed pos!)
		{
			int pos = rand.nextInt(blocks.length);
			blocks[rand.nextInt(blocks.length)].disturb(ref.avgW, rand); // move cell to a random position
			changedBlocks.add(new Integer(pos));
		}
		for(int i = 0; i < disturbedOrientations; i++)
		{
			int pos = rand.nextInt(blocks.length);
			blocks[rand.nextInt(blocks.length)].disturbOrientation(rand); // change Orientation randomly
			changedBlocks.add(new Integer(pos));
		}
		for(int i = 0; i < swaps; i++)
		{
			int pos1 = rand.nextInt(blocks.length);
			int pos2 = rand.nextInt(blocks.length);
			swapBlocks(pos1, pos2); // swap random blocks
			changedBlocks.add(new Integer(pos1));
			changedBlocks.add(new Integer(pos2));
		}
		
		if(changedBlocks.size()==0) return; // TODO
		
		badnessComponents[0] = m_bb.compute(blocks);
		badnessComponents[1] = calculateChangedOverlap(original, changedBlocks);
		badnessComponents[2] = getSemiperimeterLength();
	}
	
	public double calculateChangedOverlap(ClassicIndividual original, HashSet<Integer> changedBlocks)
	{
		double overlap = original.badnessComponents[1]; // refOverlap

		Block otherOrig = new Block();
		Block orig = new Block();

		Block otherBlock;
		Block b;
		
		Iterator<Integer> it = changedBlocks.iterator();
		
		for(int n = 0; n < changedBlocks.size(); n++)
		{
			b = blocks[it.next().intValue()];
			
			// UniformGrid for fast collision detection with reference blocks
			//overlap += ref.grid.collide(b, changedBlocks, original.blocks);
		
			
			orig.valuesFrom(nodesToPlace.get(b.getNr()));
			// collision detection for delta blocks
			for(int i = 0; i < n; i++)
			{
				otherBlock = blocks[i];
				otherOrig.valuesFrom(nodesToPlace.get(otherBlock.getNr()));
				overlap -= orig.intersectionArea(otherOrig);
				overlap += b.intersectionArea(otherBlock);
			}
		}
		return overlap;
	}
	
	/**
	 * Calculates the overlap of all Blocks (PlacementNodes).
	 * @return the sum of the overlap areas.
	 */
	public double calculateOverlap()
	{
		double overlap = 0.0;
		// sum up all the overlap area of all blocks
		
		for(int i = 0; i < blocks.length; i++)
		{
			for(int j = 0; j < i; j++)
			{
				overlap += blocks[i].intersectionArea(blocks[j]);
			}
		}
		return overlap;
	}
	
	/**
	 * Method to calculate the bounding box area of the Blocks.
	 * @return the bounding box area of the Blocks.
	 */
	public double getBoundingBoxArea()
	{
		double left = blocks[0].getLeft();
		double top = blocks[0].getTop();
		double right = blocks[0].getRight();
		double bottom = blocks[0].getBottom();
		
		for(Block b : blocks)
		{
			if(b.getLeft() < left) left = b.getLeft();
			if(b.getTop() > top) top = b.getTop();
			if(b.getRight() > right) right = b.getRight();
			if(b.getBottom() < bottom) bottom = b.getBottom();
		}
		return (top-bottom)*(right-left);
	}
	
	/**
	 * Method to calculate the semiperimeter of the bounding box of the Blocks.
	 * @return the semiperimeter of the bounding box of the Blocks.
	 */
	public double getSemiperimeterLength()
	{
		double left = blocks[0].getLeft();
		double top = blocks[0].getTop();
		double right = blocks[0].getRight();
		double bottom = blocks[0].getBottom();
		
		for(Block b : blocks)
		{
			if(b.getLeft() < left) left = b.getLeft();
			if(b.getTop() > top) top = b.getTop();
			if(b.getRight() > right) right = b.getRight();
			if(b.getBottom() < bottom) bottom = b.getBottom();
		}
		return (top-bottom)+(right-left);
	}
	
	public double getNetLength()
	{
		return badnessComponents[0];
	}
	
	/**
	 * Evaluate the individual by calculating an estimate of the network length
	 */
	public void evaluate()
	{
		badnessComponents[0] = m_bb.compute(blocks);
		//badness = m_mst.compute(blocks);
		badnessComponents[1] = calculateOverlap();
		
		badnessComponents[2] = getSemiperimeterLength();
		//hashes[0] = getXHash();
		//hashes[1] = getYHash();
		//hashes[2] = getRotHash();
		
		/*
		if(p < badnessFunction)
		{
			badnessComponents[2] = getSemiperimeterLength();
		}
		else
		{
			badnessComponents[2] = getBoundingBoxArea();
		}
		*/
	}
	

	public double sqr(double a)
	{
		return a*a;
	}
	
	/**
	 * Method to return the evaluated "un-fitness" of the Individual.
	 * @return the "un-fitness" of the placement solution.
	 */
	public double getBadness()
	{
		double badness = 0.0;
		
		// wire length estimate:
		badness += badnessComponents[0];
		
		// overlap:
		badness += badnessComponents[1]*0.5;//(0.1+(4*p)*(4*p));
		
		// semiperimeter length:
		badness += badnessComponents[2]*1.0;

		return badness;
	}
	
	public void setBadness(double[] otherComponents)
	{
		for(int i = 0; i < badnessComponents.length; i++)
		{
			badnessComponents[i] = otherComponents[i];
		}
	}
	
	public double getXHash()
	{
		double r = 0.0;
		for(Block b : blocks)
		{
			r+=b.getX();
		}
		r = Math.abs(r);
		while(r > 2.0) r/=10.0;
		return (Math.sin(r*Math.PI)+1.0)/2.0;
	}
	
	public double getYHash()
	{
		double r = 0.0;
		for(Block b : blocks)
		{
			r+=b.getY();
		}
		r = Math.abs(r);
		while(r > 2.0) r/=10.0;
		return (Math.sin(r*Math.PI)+1.0)/2.0;
	}
	
	public double getRotHash()
	{
		double r = 0.0;
		for(Block b : blocks)
		{
			Orientation o = b.getOrientation();
			if(o == Orientation.IDENT) r+=0.1;
			else if(o == Orientation.R) r+=0.2;
			else if(o == Orientation.RR) r+=0.3;
			else if(o == Orientation.RRR) r+=0.4;
			else if(o == Orientation.X) r+=0.5;
			else if(o == Orientation.XR) r+=0.6;
			else if(o == Orientation.XRR) r+=0.7;
			else if(o == Orientation.XRRR) r+=0.8;
			else if(o == Orientation.Y) r+=0.9;
			else if(o == Orientation.YR) r+=1.0;
			else if(o == Orientation.YRR) r+=1.1;
			else if(o == Orientation.YRRR) r+=1.2;
			else if(o == Orientation.XY) r+=1.3;
			else if(o == Orientation.XYR) r+=1.4;
			else if(o == Orientation.XYRR) r+=1.5;
			else if(o == Orientation.XYRRR) r+=1.6;

		}
		while(r > 2.0) r/=10.0;
		return (Math.sin(r*Math.PI)+1.0)/2.0;
	}
	
	public double[] getHashes()
	{
		return hashes;
	}
}

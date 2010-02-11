/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DeltaIndividual.java
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
import com.sun.electric.tool.placement.genetic2.metrics.DeltaBBMetric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


/**
 * This kind of individual only saves the deltas (differences) to a common
 * reference placement. This saves memory and speeds up calculations.
 * @see Population
 */
public class DeltaIndividual extends Individual<DeltaIndividual>
{
	public List<Block> blocks; // the genome, contains the positions and rotations of the PlacementNodes

	private double[] hashes;
	
	double[] overlaps;
	double[] netLengths;
	double spread;
	
	/**
	 * Constructor to randomly initialize the genome (blocks).
	 * @param nodesToPlace The PlacementNode objects.
	 * @param allNetworks The sets of connected PlacementPort objects.
	 * @param rand The random number generator of the population's thread.
	 */
	DeltaIndividual(Reference ref, Random rand)
	{		
		super(ref);
		blocks = new ArrayList<Block>();
		hashes = new double[3];
		
		netLengths = new double[ref.netLengths.length];
		for(int i = 0; i < ref.netLengths.length; i++) netLengths[i] = ref.netLengths[i];
		overlaps = new double[ref.overlaps.length];
		for(int i = 0; i < ref.overlaps.length; i++) overlaps[i] = ref.overlaps[i];

		mutate(rand);
		evaluate();
	}
	
	public void reboot(Random rand)
	{
		blocks.clear();
		
		mutate(rand);
		evaluate();
	}
	
	public void prepareForTest()
	{
		for(int i = 0; i < 10; i++)
		{
			int nodeNr = i;
			PlacementNode n = nodesToPlace.get(nodeNr);
			
			blocks.add(new Block(
					n.getPlacementX(),
					n.getPlacementY(),
					n.getWidth(),
					n.getHeight(),
					n.getPlacementOrientation(),
					nodeNr));
		}
		Collections.sort(blocks);
	}

	public void setProgress(double p)
	{
		this.p = p;
	}
	
	public void writeToPlacement(List<PlacementNode> nodesToPlace)
	{
		for(Block b : blocks)
		{
			nodesToPlace.get(b.getNr()).setPlacement(b.getX(), b.getY());
			nodesToPlace.get(b.getNr()).setOrientation(b.getOrientation());
		}
	}
	
	/**
	 * Method to compare the badness with another individual.
	 * @param other The Individual to be used as a reference.
	 * @return -1 if other has a higher badness, else 1.
	 */
	public int compareTo(DeltaIndividual other)
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
	public void copyFrom(DeltaIndividual other)
	{
		if(this == other) return;
		
		blocks.clear();
		for(Block b : other.blocks)
		{
			blocks.add(new Block(b));
		}
		
		netLengths = new double[ref.netLengths.length];
		for(int i = 0; i < ref.netLengths.length; i++) netLengths[i] = ref.netLengths[i];
		overlaps = new double[ref.overlaps.length];
		for(int i = 0; i < ref.overlaps.length; i++) overlaps[i] = ref.overlaps[i];
		
		setBadness(other.badnessComponents);
		//mutate(rand);
		//evaluate();
		//System.out.println("bsize: " + blocks.size());
	}

	/**
	 * Method to do a crossover of two genomes and write the result to the Individual's genome.
	 * Mutation and evaluation is also performed.
	 * @param mom One donor of genes.
	 * @param dad The other donor of genes.
	 */
	public void deriveFrom(DeltaIndividual mom, DeltaIndividual dad, Random rand)
	{
		if(this == mom || this == dad) return;
		
		blocks.clear();
		
		// add everything from both parents to blocks and sort
		for(Block b : mom.blocks)
		{
			blocks.add(new Block(b));
		}
		for(Block b : dad.blocks)
		{
			blocks.add(new Block(b));
		}
		Collections.sort(blocks);
		
		int pos = 0;
		int usedNr = -1; // usedNr helps avoid duplicate blocks
		int prevNr = -1;
		
		while(pos < blocks.size())
		{
			if((rand.nextDouble() > 0.5 && blocks.get(pos).getNr() != prevNr) || blocks.get(pos).getNr() == usedNr)
			{
				prevNr = blocks.get(pos).getNr();
				blocks.remove(pos);
			}
			else
			{
				prevNr = blocks.get(pos).getNr();
				usedNr = blocks.get(pos).getNr();
				pos++;
			}
		}
		
		mutate(rand);		
		// recalculate the badness
		evaluate();
	}
	/*
	public double distanceGenome(DeltaIndividual other)
	{
		Block orig = new Block();
		Iterator<Block> ib = other.blocks.iterator();
		Block otherBlock = ib.next();
		for(Block b : blocks)
		{
			while(b.getNr() > otherBlock.getNr() && ib.hasNext())
			{
				otherBlock = ib.next();
			}
			if(otherBlock.getNr() == b.getNr())
			{
				d += (b.getX()-otherBlock.getX())*(b.getX()-otherBlock.getX());
				d += (b.getY()-otherBlock.getY())*(b.getY()-otherBlock.getY());
			}
			else
			{
				orig.valuesFrom(nodesToPlace.get(b.getNr()));
				d += (b.getX()-orig.getX())*(b.getX()-orig.getX());
				d += (b.getY()-orig.getY())*(b.getY()-orig.getY());
			}
		}
		return Math.sqrt(Math.sqrt(d));
	}
	*/
	public double distance(DeltaIndividual other)
	{
		double d = 0.0;
		/*
		for(int i = 0; i < overlaps.length; i++)
		{
			d += (overlaps[i]-other.overlaps[i])*(overlaps[i]-other.overlaps[i]);
		}
		*/
		for(int i = 0; i < netLengths.length; i++)
		{
			d += Math.abs(netLengths[i]-other.netLengths[i]);

		}
		
		return d;
		
	}
	
	public double distance()
	{
		double d = 0.0;
		/*
		for(int i = 0; i < overlaps.length; i++)
		{
			d += (overlaps[i]-other.overlaps[i])*(overlaps[i]-other.overlaps[i]);
		}
		*/
		for(int i = 0; i < netLengths.length; i++)
		{
			d += Math.abs(netLengths[i]-ref.netLengths[i]);

		}
		
		return d;
		
	}
	
	/**
	 * Method to randomly swap the position of 2 blocks.
	 */
	public void swapBlocks(Random rand)
	{
		int a = rand.nextInt(nodesToPlace.size());
		int b = rand.nextInt(nodesToPlace.size());
		
		Block one = insertBlock(rand, a, false);
		Block two = insertBlock(rand, b, false);
		
		double ax = one.getX();
		double ay = one.getY();
		one.setPos(two.getX(), two.getY());
		two.setPos(ax, ay);
	}
	/*
	public void swapBlocks(Random rand)
	{
		int a = rand.nextInt(blocks.size());
		int b = rand.nextInt(blocks.size());
		
		double ax = blocks.get(a).getX();
		double ay = blocks.get(a).getY();
		blocks.get(a).setPos(blocks.get(b).getX(), blocks.get(b).getY());
		blocks.get(b).setPos(ax, ay);
	}
	*/
	
	/**
	 * Method to fetch a node from the reference and insert a corresponding
	 * block into the list of deltas.
	 */
	public Block insertBlock(Random rand, int nodeNr, boolean mutation)
	{
		PlacementNode n = nodesToPlace.get(nodeNr);
		
		double xmut = 0.0;
		double ymut = 0.0;
		
		if(mutation)
		{
			xmut = ref.avgW*rand.nextGaussian();
			ymut = ref.avgW*rand.nextGaussian();
		}
		
		Block b = new Block(
				n.getPlacementX() + xmut,
				n.getPlacementY() + ymut,
				n.getWidth(),
				n.getHeight(),
				n.getPlacementOrientation(),
				nodeNr);
		
		blocks.add(b);
		Collections.sort(blocks);
		for(int i = 1; i < blocks.size(); i++)
		{
			if(blocks.get(i).getNr() == blocks.get(i-1).getNr())
			{
				if(rand.nextDouble()>0.5)
				{
					b = blocks.get(i-1);
					blocks.remove(i);
				}
				else
				{
					b = blocks.get(i);
					blocks.remove(i-1);
				}
			}
		}
		return b;
	}
	
	/**
	 * Method to mutate a random amount of Blocks.
	 */
	public void mutate(Random rand)
	{
		if(blocks.size() < 50 && (blocks.size() == 0 || rand.nextDouble() > 0.5))
		{
			int nodeNr = rand.nextInt(nodesToPlace.size());
			insertBlock(rand, nodeNr, true);
		}
		/*else*/ if(rand.nextDouble() > 0.5 && blocks.size() > 1)
		{
			blocks.remove(rand.nextInt(blocks.size()));
		}
		
		int disturbedPositions = Math.abs((int)(rand.nextGaussian()*3.0));
		int disturbedOrientations = Math.abs((int)(rand.nextGaussian()*1.0));
		int swaps = Math.abs((int)(rand.nextGaussian()*2.0*(1-p)));
		
		for(int i = 0; i < disturbedPositions; i++)
		{
			blocks.get(rand.nextInt(blocks.size())).disturb(ref.avgW, rand); // move cell to a random position
		}
		/*
		for(int i = 0; i < disturbedPositions; i++)
		{
			blocks.get(rand.nextInt(blocks.size())).disturbToCenter(ref.avgW, rand); // move cell to a random position
		}
		*/
		
		for(int i = 0; i < disturbedOrientations; i++)
		{
			blocks.get(rand.nextInt(blocks.size())).disturbOrientation(rand); // change Orientation randomly
		}
		for(int i = 0; i < swaps; i++)
		{
			swapBlocks(rand); // swap random blocks
		}
	}
	
	/**
	 * Calculates the overlap of the complete placement.
	 * This function uses the overlap of the Reference and only calculates the
	 * changes (the changes are represented by blocks).
	 * @return the sum of the overlap areas.
	 */
	public double calculateOverlap()
	{
		double overlap = ref.badnessComponents[1]; // refOverlap
		for(int i = 0; i < nodesToPlace.size(); i++)
		{
			overlaps[i] = ref.overlaps[i];
		}

		Block otherOrig = new Block();
		Block orig = new Block();

		Block otherBlock;
		Block b;
		
		for(int n = 0; n < blocks.size(); n++)
		{
			b = blocks.get(n);
			
			// UniformGrid for fast collision detection with reference blocks
			overlap += ref.grid.collide(b, blocks, nodesToPlace, overlaps);
		
			
			orig.valuesFrom(nodesToPlace.get(b.getNr()));
			
			// collision detection for delta blocks
			for(int i = 0; i < n; i++)
			{
				otherBlock = blocks.get(i);
				otherOrig.valuesFrom(nodesToPlace.get(otherBlock.getNr()));
				overlap -= orig.intersectionArea(otherOrig);
				overlap += b.intersectionArea(otherBlock);
				
				overlaps[b.getNr()] -= orig.intersectionArea(otherOrig);
				overlaps[b.getNr()] += b.intersectionArea(otherBlock);
			}
			
			
		}
		return overlap;
	}
	
	// old calculation, not accelerated by UniformGrid
	public double old_calculateOverlap()
	{
		double overlap = ref.badnessComponents[1]; // refOverlap
		for(int i = 0; i < nodesToPlace.size(); i++)
		{
			overlaps[i] = ref.overlaps[i];
		}

		Block other = new Block();
		Block orig = new Block();
		Iterator<Block> ib = blocks.iterator();
		Block deltaBlock = ib.next();
		Block b;
		
		for(int n = 0; n < blocks.size(); n++)
		{
			b = blocks.get(n);
			orig.valuesFrom(nodesToPlace.get(b.getNr()));
			ib = blocks.iterator();
			deltaBlock = ib.next();
			
			for(int i = 0; i < nodesToPlace.size(); i++)
			{
				if(i > deltaBlock.getNr() && ib.hasNext())
				{
					deltaBlock = ib.next();
				}
				
				if(i != b.getNr()) // not myself
				{				
					if(i < b.getNr() && deltaBlock.getNr() == i) // for the predecessors, look also at the deltas
					{
						other.valuesFrom(nodesToPlace.get(i));
						overlap -= orig.intersectionArea(other);
						
						overlaps[b.getNr()] -= orig.intersectionArea(other);
						overlaps[b.getNr()] += b.intersectionArea(deltaBlock);
						
						
						overlap += b.intersectionArea(deltaBlock);
					}
					else if(deltaBlock.getNr() != i) // for the successors, look only at the non-deltas
					{
						other.valuesFrom(nodesToPlace.get(i));
						overlap -= orig.intersectionArea(other);
						
						overlaps[i] -= orig.intersectionArea(other);
						overlaps[i] += b.intersectionArea(other);

						overlap += b.intersectionArea(other);
					}
					else
					{
						// do nothing for the successors who are deltas (skip them),
						// they will calculate their overlap with this block
					}
				}
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
		Iterator<PlacementNode> it = nodesToPlace.iterator();
		Iterator<Block> ib = blocks.iterator();
		Block deltaBlock = ib.next();
		
		Block b = new Block();
		PlacementNode n = it.next();
		b.setPos(n.getPlacementX(), n.getPlacementY());
		b.setOrientation(n.getPlacementOrientation());
		

		double left = b.getLeft();
		double top = b.getTop();
		double right = b.getRight();
		double bottom = b.getBottom();
		
		
		for(int i = 1; i < nodesToPlace.size(); i++)
		{
			
			n = it.next();
			b.setPos(n.getPlacementX(), n.getPlacementY());
			b.setOrientation(n.getPlacementOrientation());
			
			if(i > deltaBlock.getNr() && ib.hasNext())
			{
				deltaBlock = ib.next();
			}
			if(deltaBlock.getNr() == i)
			{
				if(deltaBlock.getLeft() < left) left = deltaBlock.getLeft();
				if(deltaBlock.getTop() > top) top = deltaBlock.getTop();
				if(deltaBlock.getRight() > right) right = deltaBlock.getRight();
				if(deltaBlock.getBottom() < bottom) bottom = deltaBlock.getBottom();
			}
			else
			{
				if(b.getLeft() < left) left = b.getLeft();
				if(b.getTop() > top) top = b.getTop();
				if(b.getRight() > right) right = b.getRight();
				if(b.getBottom() < bottom) bottom = b.getBottom();
			}
		}
		
		return (top-bottom)*(right-left);
	}
	
	/**
	 * Method to calculate the semiperimeter of the bounding box of the Blocks.
	 * @return the semiperimeter of the bounding box of the Blocks.
	 */
	public double getSemiperimeterLength()
	{
		Iterator<PlacementNode> it = nodesToPlace.iterator();
		Iterator<Block> ib = blocks.iterator();
		Block deltaBlock = ib.next();
		
		Block b = new Block();
		PlacementNode n = it.next();
		b.valuesFrom(n);
		
		double left = b.getLeft();
		double top = b.getTop();
		double right = b.getRight();
		double bottom = b.getBottom();
		
		if(deltaBlock.getNr() == 0)
		{
			if(deltaBlock.getLeft() < left) left = deltaBlock.getLeft();
			if(deltaBlock.getTop() > top) top = deltaBlock.getTop();
			if(deltaBlock.getRight() > right) right = deltaBlock.getRight();
			if(deltaBlock.getBottom() < bottom) bottom = deltaBlock.getBottom();
		}
		else
		{
			if(b.getLeft() < left) left = b.getLeft();
			if(b.getTop() > top) top = b.getTop();
			if(b.getRight() > right) right = b.getRight();
			if(b.getBottom() < bottom) bottom = b.getBottom();
		}

		
		
		for(int i = 1; i < nodesToPlace.size(); i++)
		{
			
			n = it.next();
			b.valuesFrom(n);
			
			if(i > deltaBlock.getNr() && ib.hasNext())
			{
				deltaBlock = ib.next();
			}
			if(deltaBlock.getNr() == i)
			{
				if(deltaBlock.getLeft() < left) left = deltaBlock.getLeft();
				if(deltaBlock.getTop() > top) top = deltaBlock.getTop();
				if(deltaBlock.getRight() > right) right = deltaBlock.getRight();
				if(deltaBlock.getBottom() < bottom) bottom = deltaBlock.getBottom();
			}
			else
			{
				if(b.getLeft() < left) left = b.getLeft();
				if(b.getTop() > top) top = b.getTop();
				if(b.getRight() > right) right = b.getRight();
				if(b.getBottom() < bottom) bottom = b.getBottom();
			}
		}
		
		return (top-bottom)+(right-left);
	}
	
	public double getNetLength()
	{
		return badnessComponents[0];
	}
	
	/**
	 * Evaluate the individual by calculating an estimate of the network length and
	 * the overlap area between PlacementNodes or Delta-blocks.
	 */
	public void evaluate()
	{
		netLengths = new double[ref.netLengths.length];
		for(int i = 0; i < ref.netLengths.length; i++) netLengths[i] = ref.netLengths[i];
		overlaps = new double[ref.overlaps.length];
		for(int i = 0; i < ref.overlaps.length; i++) overlaps[i] = ref.overlaps[i];
		
		badnessComponents[0] = DeltaBBMetric.compute(blocks, netLengths);
		badnessComponents[1] = calculateOverlap();
		badnessComponents[2] = calculateSpread();
		//badnessComponents[2] = getSemiperimeterLength();
		
		hashes[0] = getXHash();
		hashes[1] = getYHash();
		hashes[2] = getRotHash();
	}

	
	public double getSize()
	{
		return blocks.size();
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
		badness += badnessComponents[1]*(1.0+300.0*p*p);
		
		// area:
		// badness += badnessComponents[2]*0.1;
		badness += 0.00001*badnessComponents[2];

		return badness;
	}
	
	public double calculateSpread()
	{
		spread = ref.spread;
		
		Block refBlock = new Block();
		
		for(Block b : blocks)
		{
			refBlock.valuesFrom(nodesToPlace.get(b.getNr()));
			
			spread-=Math.sqrt(refBlock.getX()*refBlock.getX() + refBlock.getY()*refBlock.getY())
					* refBlock.getWidth()*refBlock.getHeight();
			spread+=Math.sqrt(b.getX()*b.getX() + b.getY()*b.getY())
					* b.getWidth()*b.getHeight();
			
			//spread-=Math.abs(refBlock.getX())+Math.abs(refBlock.getY());
			//spread+=Math.abs(b.getX())+Math.abs(b.getY());
		}
		return spread;
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
			r+=b.getX()-nodesToPlace.get(b.getNr()).getPlacementX();
		}
		r = Math.abs(r);
		//while(r > 2.0) r/=10.0;
		r/=5000.0;
		return (Math.sin(r*Math.PI)+1.0)/2.0;
	}
	
	public double getYHash()
	{
		double r = 0.0;
		for(Block b : blocks)
		{
			r+=b.getY()-nodesToPlace.get(b.getNr()).getPlacementY();
		}
		r = Math.abs(r);
		//while(r > 2.0) r/=10.0;
		r/=5000.0;
		return (Math.sin(r*Math.PI)+1.0)/2.0;
	}
	
	public double getRotHash()
	{
		double r = 0.0;
		for(Block b : blocks)
		{
			Orientation o = b.getOrientation();
			if(o == nodesToPlace.get(b.getNr()).getPlacementOrientation()) r+=0.0;
			else if(o == Orientation.IDENT) r+=0.1;
			else if(o == Orientation.R) r-=0.2;
			else if(o == Orientation.RR) r+=0.3;
			else if(o == Orientation.RRR) r-=0.4;
			else if(o == Orientation.X) r+=0.5;
			else if(o == Orientation.XR) r-=0.6;
			else if(o == Orientation.XRR) r+=0.7;
			else if(o == Orientation.XRRR) r-=0.8;
			else if(o == Orientation.Y) r+=0.9;
			else if(o == Orientation.YR) r-=1.0;
			else if(o == Orientation.YRR) r+=1.1;
			else if(o == Orientation.YRRR) r-=1.2;
			else if(o == Orientation.XY) r+=1.3;
			else if(o == Orientation.XYR) r-=1.4;
			else if(o == Orientation.XYRR) r+=1.5;
			else if(o == Orientation.XYRRR) r-=1.6;

		}
		//while(r > 2.0) r/=10.0;
		r/=50.0;
		return (Math.sin(r*Math.PI)+1.0)/2.0;
	}
	
	public double getHash()
	{
		double b = getBadness();
		while(b > 2.0) b/=10.0;
		b = (Math.sin(b*Math.PI)+1.0)/2.0;
		double h = hashes[0]+hashes[1]+hashes[2]+b;
		return h;
	}
	
	public double[] getHashes()
	{
		return hashes;
	}
}

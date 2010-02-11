/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Individual.java
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


import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;

import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Abstract class representing an individual in a genetic algorithm.
 * @see DeltaIndividual
 */
public abstract class Individual <I extends Individual> implements Comparable<I>
{
	protected double[] badnessComponents = new double[3]; // how bad our individual is
	private double[] hashes;

	List<PlacementNode> nodesToPlace;
	List<PlacementNetwork> allNetworks;
	
	public ReadWriteLock rwLock;
	double p;
	public Reference ref;

	
	/**
	 * Constructor to randomly initialize the genome (blocks).
	 * @param ref The Reference contains all needed information about the placement
	 */
	Individual(Reference ref)
	{
		this.ref = ref;
		nodesToPlace = ref.nodesToPlace;
		allNetworks = ref.allNetworks;
		rwLock = new ReentrantReadWriteLock();
	}
	
	public abstract void evaluate();
	public abstract void reboot(Random rand);
	
	public double distance()
	{
		return 0.0;
	}
	
	public void setProgress(double p)
	{
		this.p = p;
	}
	
	public double getSize()
	{
		return 5.0;
	}
	
	public abstract double distance(I other);
	public abstract void writeToPlacement(List<PlacementNode> nodesToPlace);
	
	/**
	 * Method to compare the badness with another individual.
	 * @param other The Individual to be used as a reference.
	 * @return -1 if other has a higher badness, else 1.
	 */
	public int compareTo(I other)
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
	public abstract void copyFrom(I other);
	
	public abstract void mutate(Random rand);

	/**
	 * Method to do a crossover of two genomes and write the result to the Individual's genome.
	 * Mutation and evaluation is also performed.
	 * @param mom One donor of genes.
	 * @param dad The other donor of genes.
	 */
	public abstract void deriveFrom(I mom, I dad, Random rand);

	
	/**
	 * Calculates the overlap of all Blocks (PlacementNodes).
	 * @return the sum of the overlap areas.
	 */
	public abstract double calculateOverlap();
	
	/**
	 * Method to calculate the bounding box area of the Blocks.
	 * @return the bounding box area of the Blocks.
	 */
	public abstract double getBoundingBoxArea();
	/**
	 * Method to calculate the semiperimeter of the bounding box of the Blocks.
	 * @return the semiperimeter of the bounding box of the Blocks.
	 */
	public abstract double getSemiperimeterLength();

	public abstract double getNetLength();
	
	public double[] getBadnessComponents()
	{
		return badnessComponents;
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
		badness += badnessComponents[1]*20.0;
		
		// bounding box area:
		badness += badnessComponents[2]*20.0;
		
		return badness;// + rand.nextGaussian()*1000.0*(1.0-((double)evolutionStep)/((double)evolutionSteps));
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
		return 0.0;
	}
	
	public double getYHash()
	{
		return 0.0;
	}
	
	public double getRotHash()
	{
		return 0.0;
	}
	
	public double[] getHashes()
	{
		return hashes;
	}
	public double getHash()
	{
		return 0.0;
	}
}

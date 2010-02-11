/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UnifiedPopulation.java
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


/**
 * Class for running a genetic algorithm in a separate thread.
 * Different selection methods are implemented here.
 */
public class UnifiedPopulation<I extends Individual>
{
	private I[] indis;
	private Evolver[] evolvers;
	private Random rand;
	private long evolutionStepTime;
	double p;
	
	/**
	 * Constructor of a population.
	 * @param nodesToPlace
	 * @param allNetworks 
	 * @param maxIndividuals The number of individuals.
	 * @param evolutionSteps How many steps the evolve-function should iterate.
	 * @param rand An exclusive random number generator, not to be used by other threads.
	 */
	UnifiedPopulation(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks,
			I[] indis, long evoStepTime, int numThreads, Random rand)
	{
		evolutionStepTime = evoStepTime;

		this.rand = rand;
		
		this.indis = indis;
		evolvers = new Evolver[numThreads];


		Random[] rands = new Random[numThreads];
		
		int n = 0;
		for(int i = 0; i < numThreads; i++)
		{
			n+=rand.nextInt();
			rands[i] = new Random(n);
			evolvers[i] = new Evolver(indis, evolutionStepTime, rands[i]);
		}
	}
	
	public int getSize()
	{
		return indis.length;
	}
	
	public void setProgress(double p)
	{
		this.p = p;
		indis[0].ref.setProgress(p);
		for(int i=0;i<indis.length;i++)
		{
			indis[i].setProgress(p);
		}
	}
	
	public long getEvolverSteps()
	{
		long steps = 0;
		for(Evolver e : evolvers)
		{
			steps += e.getSteps();
		}
		return steps;
	}
	
	/**
	 * Method to overwrite the genome of the individual at the end of the array
	 * with an immigrant's genome.
	 * @param immigrant The Individual to be inserted.
	 */
	public void insert(Individual immigrant)
	{
		indis[indis.length-1].copyFrom(immigrant);
	}
	
	public void reboot()
	{
		for(I i : indis)
		{
			i.reboot(rand);
		}
	}
	
	public void evolveLocalMT(int numThreads)
	{
		Thread[] threads = new Thread[numThreads];
		for(int i = 0; i < numThreads; i++)
		{
			threads[i] = new Thread(evolvers[i]);
			threads[i].start();
		}
		for(int i = 0; i < numThreads; i++)
		{
			try 
			{
				threads[i].join();
			} 
			catch(InterruptedException e) 
			{ 
				System.exit(-3333);
			}
		}
	}
	
	public void reEvaluateAll()
	{
		for(I i : indis)
		{
			i.evaluate();
		}
	}
	
	/**
	 * Method to find the best individual and return it
	 * @return The best performing individual
	 */
	public Individual getChampion()
	{
		int pos = 0;
		int c = 0;
		double badness = indis[0].getBadness();
		
		for(Individual i : indis)
		{
			if(i.getBadness() < badness)
			{
				pos = c;
				badness = i.getBadness();
			}
			c++;
		}		
		return indis[pos];
	}
	
	public Individual getRandomOne()
	{
		return indis[rand.nextInt(indis.length)];
	}
	
	public Individual getAt(int pos)
	{
		return indis[pos];
	}
}

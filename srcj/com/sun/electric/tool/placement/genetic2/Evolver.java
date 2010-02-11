/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Evolver.java
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

import java.util.List;
import java.util.Random;



/**
 * Class for running a genetic algorithm in a separate thread.
 * Each Evolver iteratively picks individuals from the population, locks them
 * and uses them for evolution.
 * Different selection methods are implemented here.
 */
public class Evolver implements Runnable
{
	List<PlacementNetwork> allNetworks;
	
	private Individual[] indis;
	private Random rand;
	private long evolutionStepTime;
	double p;
	long steps;
	
	/**
	 * Constructor of an Evolver.
	 * @param indis All the individuals in the whole population.
	 * @param evolutionStepTime How many milliseconds a single step runs.
	 * @param evolutionSteps How many steps the evolve-function should iterate.
	 * @param rand An exclusive random number generator, not to be used by other threads.
	 */
	Evolver(Individual[] indis, long evolutionStepTime,  Random rand)
	{
		this.indis = indis;
		this.evolutionStepTime = evolutionStepTime;
		this.rand = rand;
	}
	
	/**
	 * Updates the information of how much of the runtime is already over.
	 */
	public void setProgress(double p)
	{
		this.p = p;		
	}
	
//	/**
//	 * Local steady-state evolution with diversity bonus.
//	 */
//	private void evoDiverseLocal()
//	{		
//		int comparison;
//		comparison = rand.nextInt(indis.length);
//		while(!indis[comparison].rwLock.writeLock().tryLock())
//		{
//			comparison = rand.nextInt(indis.length);
//		}
//		
//		int[] positions = new int[3];
//		positions[0] = rand.nextInt(indis.length);
//		while(!indis[positions[0]].rwLock.writeLock().tryLock())
//		{
//			positions[0] = rand.nextInt(indis.length);
//		}
//		
//		for(int i = 1; i < positions.length; i++)
//		{
//			positions[i] = (positions[0]+(int)(20.0*rand.nextGaussian()))%indis.length;
//			if(positions[i] < 0) positions[i] += indis.length;
//			while(!indis[positions[i]].rwLock.writeLock().tryLock())
//			{
//				positions[i] = (positions[0]+(int)(20.0*rand.nextGaussian()))%indis.length;
//				if(positions[i] < 0) positions[i] += indis.length;
//			}
//		}
//
//		//double d0 = indis[positions[0]].distance();
//		//double d1 = indis[positions[1]].distance();
//		//double d2 = indis[positions[2]].distance();
//		
//		// diversity bonus:
//		double d0 = indis[positions[0]].distance(indis[comparison]);
//		double d1 = indis[positions[1]].distance(indis[comparison]);
//		double d2 = indis[positions[2]].distance(indis[comparison]);
//		
//		/*
//		if(rand.nextInt(100000) == 0)
//		{
//			System.out.println("d0: " + d0 + " d1: " + d1 + " d2: " + d2);
//		}
//		*/
//		
//		// overwrite the loser with the crossover of the best two
//		if(indis[positions[0]].getBadness()-d0 >= indis[positions[1]].getBadness()-d1 &&
//				indis[positions[0]].getBadness()-d0 >= indis[positions[2]].getBadness()-d2)
//		{
//			indis[positions[0]].deriveFrom(indis[positions[1]], indis[positions[2]], rand);
//		}
//		else if(indis[positions[1]].getBadness()-d1 >= indis[positions[0]].getBadness()-d0 &&
//				indis[positions[1]].getBadness()-d1 >= indis[positions[2]].getBadness()-d2)
//		{
//			indis[positions[1]].deriveFrom(indis[positions[0]], indis[positions[2]], rand);
//		}	
//		else
//		{
//			indis[positions[2]].deriveFrom(indis[positions[0]], indis[positions[1]], rand);
//		}	
//		
//		for(int i = 0; i < positions.length; i++)
//		{
//			indis[positions[i]].rwLock.writeLock().unlock();
//		}
//		indis[comparison].rwLock.writeLock().unlock();
//	}
	
	/**
	 * Local steady-state evolution.
	 */
	private void evoLocal()
	{		
		int[] positions = new int[3];
		positions[0] = rand.nextInt(indis.length);
		while(!indis[positions[0]].rwLock.writeLock().tryLock())
		{
			positions[0] = rand.nextInt(indis.length);
		}
		
		for(int i = 1; i < positions.length; i++)
		{
			positions[i] = (positions[0]+(int)(20.0*rand.nextGaussian()))%indis.length;
			if(positions[i] < 0) positions[i] += indis.length;
			while(!indis[positions[i]].rwLock.writeLock().tryLock())
			{
				positions[i] = (positions[0]+(int)(20.0*rand.nextGaussian()))%indis.length;
				if(positions[i] < 0) positions[i] += indis.length;
			}
		}

		
		// overwrite the loser with the crossover of the best two
		if(indis[positions[0]].getBadness() >= indis[positions[1]].getBadness() &&
				indis[positions[0]].getBadness() >= indis[positions[2]].getBadness())
		{
			indis[positions[0]].deriveFrom(indis[positions[1]], indis[positions[2]], rand);
		}
		else if(indis[positions[1]].getBadness() >= indis[positions[0]].getBadness() &&
				indis[positions[1]].getBadness() >= indis[positions[2]].getBadness())
		{
			indis[positions[1]].deriveFrom(indis[positions[0]], indis[positions[2]], rand);
		}	
		else
		{
			indis[positions[2]].deriveFrom(indis[positions[0]], indis[positions[1]], rand);
		}	
		
		for(int i = 0; i < positions.length; i++)
		{
			indis[positions[i]].rwLock.writeLock().unlock();
		}
	}
	
	/**
	 * @return The number of evolution steps (generations) the evolver has iterated
	 */
	public long getSteps()
	{
		return steps;
	}
	
	public void run()
	{
		long t = System.currentTimeMillis();		

		while((System.currentTimeMillis()-t) < evolutionStepTime)
		{	
			//evoDiverseLocal();
			evoLocal();
			steps++;
		}
	}

}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Population.java
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

import java.util.Arrays;
import java.util.List;
import java.util.Random;


/**
 * Class for running a genetic algorithm in a separate thread.
 * Different selection methods are implemented here.
 * This class is deprecated, use UnifiedPopulation instead.
 * @see UnifiedPopulation
 */
public class Population<I extends Individual> implements Runnable
{
	private I[] indis;
	private int maxIndividuals;
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
	Population(List<PlacementNode> nodes, List<PlacementNetwork> allNets,
			I[] indis, long evolutionStepTime, Random r)
	{

		rand = r;
		
		this.indis = indis;
	}
	
	public void setProgress(double p)
	{
		this.p = p;
		for(int i=0;i<indis.length;i++)
		{
			indis[i].setProgress(p);
		}
	}
	
	/**
	 * Method to overwrite the genome of the individual at the end of the array
	 * with an immigrant's genome.
	 * @param immigrant The Individual to be inserted.
	 */
	public void insert(I immigrant)
	{
		indis[indis.length-1].copyFrom(immigrant);
	}
	
	/**
	 * Method to explore the solution space of individuals by means of repeated selection,
	 * recombination, mutation. The selection method is "best 50% survive and mate"
	 */
	private void evolveCompetition()
	{
		long t = System.currentTimeMillis();
		while((System.currentTimeMillis()-t) < evolutionStepTime)
		{		
			Arrays.sort(indis);
			
			// the worse half is overwritten
			int momPos, dadPos;
			for(int j = maxIndividuals/2; j < maxIndividuals; j++)
			{
				momPos = rand.nextInt(maxIndividuals/2);
				dadPos = rand.nextInt(maxIndividuals/2);
				
				indis[j].deriveFrom(indis[momPos], indis[dadPos], rand);
			}
		}
	}
	
//	/**
//	 * Method to explore the solution space of individuals by means of repeated selection,
//	 * recombination, mutation. A special form of tournament selection is applied.
//	 */
//	private void evolveTournament()
//	{
//		long t = System.currentTimeMillis();
//		
//		int currPos, otherPos, opponentPos;
//
//		while((System.currentTimeMillis()-t) < evolutionStepTime)
//		{		
//			currPos = rand.nextInt(maxIndividuals);
//			opponentPos = rand.nextInt(maxIndividuals);
//			
//			// overwrite the loser with the crossover of the winner and a random individual
//			if(indis[currPos].getBadness() > indis[opponentPos].getBadness())
//			{
//				otherPos = rand.nextInt(maxIndividuals);
//				indis[currPos].deriveFrom(indis[opponentPos], indis[otherPos], rand);
//			}
//		}
//	}
	
//	/**
//	 * Method to explore the solution space of individuals by means of repeated selection,
//	 * recombination, mutation. Local selection with 1D-neighborhoods is applied.
//	 */
//	private void evolveLocal()
//	{
//		long t = System.currentTimeMillis();
//		
//		int currPos, otherPos, opponentPos;
//
//		while((System.currentTimeMillis()-t) < evolutionStepTime)
//		{		
//			currPos = rand.nextInt(maxIndividuals);
//			
//			opponentPos = (currPos+(int)(10.0*rand.nextGaussian()))%indis.length;
//			if(opponentPos < 0) 
//			{
//				opponentPos += indis.length;
//			}
//			
//			// overwrite the loser with the crossover of the winner and a random individual
//			if(indis[currPos].getBadness() > indis[opponentPos].getBadness())
//			{
//				otherPos = (currPos+(int)(10.0*rand.nextGaussian()))%indis.length;
//				if(otherPos < 0) otherPos += indis.length;
//				
//				indis[currPos].deriveFrom(indis[opponentPos], indis[otherPos], rand);
//			}
//		}
//	}
	
	
	/**
	 * Method to find the best individual and return it
	 * @return The best performing individual
	 */
	public I getChampion()
	{
		int pos = 0;
		int c = 0;
		double badness = indis[0].getBadness();
		
		for(I i : indis)
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
	
	public I getRandomOne()
	{
		return indis[rand.nextInt(indis.length)];
	}
	
	public void run()
	{
		evolveCompetition();
		//evolveTournament();
		//evolveLocal();
	}

}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GeneticPlacer.java
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
import com.sun.electric.tool.placement.genetic2.metrics.DeltaBBMetric;

import java.util.List;
import java.util.Random;

/**
 * Genetic algorithm for placement.
 */
public class GeneticPlacer
{
//	private static final int MAX_INDIVIDUALS = 200; // for islands (deprecated)
	private static final int INDIVIDUALS = 1000; // population size for UnifiedPopulation

	private static int EVOLUTION_STEPS;
	
	// maximum runtime of the placement algorithm in seconds
	public int maxRuntime;
	// number of threads
	public int numThreads;
	// if false: NO system.out.println statements
	public boolean printDebugInformation;
	long startTime;

	UnifiedPopulation population = null;

	Reference ref;
	
	public String getAlgorithmName() { return "team4Genetic"; }
	
	public UnifiedPopulation  getPopulation() { return population; }
	
	GeneticPlacer(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks,
			int runtime, int threads, boolean debug)
	{
		maxRuntime = runtime*1000;
		EVOLUTION_STEPS = maxRuntime/10000;
		numThreads = threads;
		printDebugInformation = debug;
		
		startTime = System.currentTimeMillis();
		
		Random rand = new Random(System.currentTimeMillis());

		
		DeltaBBMetric.init(nodesToPlace, allNetworks);
		ref = new Reference(nodesToPlace, allNetworks, rand);
		DeltaBBMetric.setRef(ref);
		ref.calculateRefNetLength();
		

		// initialize population
		DeltaIndividual[] indis = new DeltaIndividual[INDIVIDUALS];
		for(int i = 0; i < INDIVIDUALS; i++)
		{
			indis[i] = new DeltaIndividual(ref, rand);
		}
		
		population = new UnifiedPopulation(nodesToPlace, allNetworks,
				indis, maxRuntime/EVOLUTION_STEPS, numThreads, rand);
		
	}
	

	private void placeUnified(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks, long partTime)
	{
		long localStartTime = System.currentTimeMillis();
		
		if(printDebugInformation)
		{
			System.out.println("Starting genetic algorithm...\n" 
					+ "Total running time is " + maxRuntime/1000 + " seconds (~" +maxRuntime/60000+ " minutes).\n"
					+ "There will be " + EVOLUTION_STEPS + " text messages while running,\n"
					+ "each will tell you the current round number and champion badness:");
		}

		int round = 0;
		long t = 0;
		long localT = 0;
		DeltaIndividual champion;
		ref.calculateFirstTime();
		
		while(t < partTime)
		{
			
			population.setProgress(((double)localT)/(double)(partTime-(localStartTime-startTime)));
		
			population.evolveLocalMT(numThreads); // this is where the evolution happens!
			
			champion = (DeltaIndividual)population.getChampion();

			if(printDebugInformation)
			{
				System.out.println("Round " + round + " of " + EVOLUTION_STEPS
						+ " | champion badness: " + population.getChampion().getBadness());
	
				
				System.out.println("champion overlap: " + champion.getBadnessComponents()[1]);
				System.out.println("champion netlength: " + champion.getBadnessComponents()[0]);
			}
		
			ref.update(champion);
			
			if(round%4==0)
			{
				// adapt the grid to the changed placement
				ref.createGrid();
			}
			
			if(printDebugInformation)
			{
				System.out.println("ref overlap: " + ref.getBadnessComponents()[1]);
				System.out.println("ref netlength: " + ref.getBadnessComponents()[0]);
				System.out.println();
			}
			
			population.reboot();
			
			round++;
			t = (System.currentTimeMillis()-startTime);
			localT = (System.currentTimeMillis()-localStartTime);
		}
		
		// find the champion
		champion = (DeltaIndividual)population.getChampion();	

		
		// write champion placement to nodesToPlace
		ref.update(champion);
		
		ref.calculateFirstTime();
		
		if(printDebugInformation)
		{
			System.out.println("final ref net length: " + ref.getBadnessComponents()[0]);
			System.out.println("final ref overlap: " + ref.getBadnessComponents()[1]);
			System.out.println("final ref naiveOverlap: " + ref.getNaiveOverlap());
			System.out.println("final net length (bb):  " + DeltaBBMetric.compute());
			//System.out.println("final evolverSteps: " + population.getEvolverSteps());
		}
	}
	
	
	
	
	private void placeAnnealing(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks, long partTime)
	{
		long localStartTime = System.currentTimeMillis();
		SimulatedAnnealing annealing = new SimulatedAnnealing(nodesToPlace, allNetworks, 
									maxRuntime/EVOLUTION_STEPS, numThreads, ref);
		
		
		if(printDebugInformation)
		{
			System.out.println("Starting simulated annealing...\n" 
					+ "Total running time is " + maxRuntime/1000 + " seconds (~" +maxRuntime/60000+ " minutes).\n"
					+ "There will be " + EVOLUTION_STEPS + " text messages while running,\n"
					+ "each will tell you the current round number and champion badness:");
		}
		int round = 0;
		long t = 0;
		long localT = 0;
		
		ref.calculateFirstTime();
		while(t < partTime)
		{
			
			annealing.setProgress(((double)localT)/(double)(partTime-(localStartTime-startTime)));

			annealing.go();

			if(printDebugInformation)
			{
				System.out.println("Round " + round + " of " + EVOLUTION_STEPS);
				System.out.println("ref netlength: " + ref.getBadnessComponents()[0]);
				System.out.println("ref overlap: " + ref.getBadnessComponents()[1]);
			}
			
			if(round%4==0)
			{
				// adapt the grid to the changed placement
				ref.createGrid();
			}
			
			// annealing.reboot();
			
			round++;
			t = (System.currentTimeMillis()-startTime);
			localT = (System.currentTimeMillis()-localStartTime);
		}

		
		if(printDebugInformation)
		{
			System.out.println("final ref net length: " + ref.getBadnessComponents()[0]);
			System.out.println("final ref overlap: " + ref.getBadnessComponents()[1]);
			System.out.println("final ref naiveOverlap: " + ref.getNaiveOverlap());
			System.out.println("final net length (bb):  " + DeltaBBMetric.compute());
			//System.out.println("final evolverSteps: " + population.getEvolverSteps());
		}
	}
	
//	/**
//	 * This function is deprecated, use placeUnified or placeAnnealing instead.
//	 * It is only provided for comparison and can be deleted at any time
//	 */
//	private void placeIslands(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks)
//	{
//		BBMetric m_bb = new BBMetric(nodesToPlace, allNetworks);
//		
//		// create root random number generator
//		Random rand = new Random(System.currentTimeMillis());
//		
//		// create one random number generator per thread
//		int[] seeds = new int[numThreads];
//		Random[] rands = new Random[numThreads];
//		
//		for(int i=0; i<numThreads; i++)
//		{
//			seeds[i] = rand.nextInt();
//			rands[i] = new Random(seeds[i]);
//		}
//
//		
//		// initialize populations (one per thread)
//		Population<ClassicIndividual>[] populations = new Population[numThreads];
//		Thread[] threads = new Thread[numThreads];
//		
//		
//		for(int i=0; i<numThreads; i++) {
//			
//			ClassicIndividual[] indis = new ClassicIndividual[MAX_INDIVIDUALS/numThreads];
//			for(int n = 0; n < MAX_INDIVIDUALS/numThreads; n++)
//				indis[n] = new ClassicIndividual(ref, rand);
//			
//			populations[i] = 
//				new Population<ClassicIndividual>(nodesToPlace, 
//					allNetworks, 
//					indis, 
//					(long)(maxRuntime/EVOLUTION_STEPS),   // Gesamtzeit geteilt durch Evolutionsschritte
//					rands[i]);
//		}
//		
//		if(printDebugInformation)
//		{
//			System.out.println("Starting genetic algorithm...\n" 
//					+ "Total running time is " + maxRuntime/1000 + " seconds (~" +maxRuntime/60000+ " minutes).\n"
//					+ "There will be " + EVOLUTION_STEPS + " text messages while running,\n"
//					+ "each will tell you the current round number and champion badness:");
//		}
//		
//		// periodically start threads and exchange champions between populations
//		int round = 0;
//		long t = 0;
//		
//		while(t < maxRuntime)
//		{
//			for(int i=0; i<numThreads; i++)
//			{
//				populations[i].setProgress(((double)t)/((double)maxRuntime));
//				threads[i] = new Thread(populations[i]);
//				threads[i].start();
//			}
//			
//			// wait until all populations have completed some rounds of evolution
//			for(int i=0; i<numThreads; i++)
//			{
//				try {
//					threads[i].join();
//				} catch (InterruptedException e) { e.printStackTrace(); }
//			}
//			
//			if(printDebugInformation)
//			{
//				System.out.println("(" + round + ") : "
//						+ populations[0].getChampion().getBadness());
//			}
//			
//			
//			// exchange champions so the populations can cooperate
//			for(int i=0; i<numThreads; i++)
//			{
//				//if(rand.nextDouble() > 0.5)
//				{
//					populations[i].insert(populations[(i+1)%numThreads].getChampion());
//					//populations[i].insert(populations[(i+1)%numThreads].getRandomOne());
//				}
//			}
//
//			// write champion placement to nodesToPlace
//			population.getChampion().writeToPlacement(nodesToPlace);
//			population.reEvaluateAll();
//			
//			round++;
//			t = (System.currentTimeMillis()-startTime);
//		}
//		
//		// find the best champion of all populations
//		Individual champion = populations[0].getChampion();	
//		
//		for(int p=1; p<numThreads; p++)
//		{
//			Individual challenger = populations[p].getChampion();
//			if(challenger.getBadness() < champion.getBadness())
//			{
//				champion = challenger;
//			}
//		}
//		
//		// write champion placement to nodesToPlace
//		population.getChampion().writeToPlacement(nodesToPlace);
//		if(printDebugInformation)
//		{
//			System.out.println("final champion badness: " + champion.getBadness());
//			System.out.println("final champion overlap: " + champion.calculateOverlap());
//			System.out.println("final champion semiperimeter length: " + champion.getSemiperimeterLength());
//			System.out.println("final champion bounding box area: " + champion.getBoundingBoxArea());
//			
//			System.out.println("final net length (bb):  " + m_bb.compute());
//		}
//	}
	
	/**
	 * Method to run the genetic algorithm to find a good placement.
	 * @param nodesToPlace a list of all nodes that are to be placed.
	 * @param allNetworks a list of all networks that connect the nodes.
	 */
	protected void runPlacement(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks)
	{
		// For a runtime of less than 20 seconds, do only simulated annealing
		if(maxRuntime >= 19900) placeUnified(nodesToPlace, allNetworks, maxRuntime/2);
		placeAnnealing(nodesToPlace, allNetworks, maxRuntime);
		//placeIslands(nodesToPlace, allNetworks); // deprecated
	}	
	
	
}

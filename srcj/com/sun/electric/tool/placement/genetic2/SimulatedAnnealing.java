/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SimulatedAnnealing.java
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * Class for running simulated annealing.
 * The parallelization concept is: parallel evaluation of multiple moves
 * This concept relies on the fact that the number of threads corresponds to the
 * number of physical cores.
 * Thread pools are used to accelerate thread construction.
 */
public class SimulatedAnnealing
{
	/**
	 * Helper class to find candidate solutions in parallel.
	 * When a solution has been accepted, the solutions of all other threads are
	 * thrown away because they were relative to the previous reference placement.
	 */
	class Annealer implements Runnable
	{
		DeltaIndividual dIndi;
		Random rand;
		double p;
		
		public Annealer (Random rand)
		{
			dIndi = new DeltaIndividual(ref, rand);
			this.rand = rand;
		}
		
		public void setProgress(double p)
		{
			this.p = p;
			dIndi.setProgress(p);
		}
		
		public void run()
		{
			dIndi.reboot(rand);
		}
	}
	Annealer[] annealers;
	private Random rand;
	private long evolutionStepTime;
	double p;
	int numThreads;
	ExecutorService threadExecutor;

	Reference ref;
	
	/**
	 * Constructor of the simulated annealing class.
	 * @param nodesToPlace
	 * @param allNetworks 
	 * @param evoStepTime Run for this time and then return to the loop in GeneticPlacer
	 * @param numThreads Number of threads
	 * @param ref The reference placement (nodesToPlace wrapper)
	 */
	SimulatedAnnealing(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks,
			long evoStepTime, int numThreads, Reference ref)
	{
		this.ref = ref;
		rand = new Random();
		evolutionStepTime = evoStepTime;
		this.numThreads = numThreads;
		
		p = 0.0;

		Random[] rands = new Random[numThreads];
		annealers = new Annealer[numThreads];
		for(int i = 0; i < numThreads; i++)
		{
			rands[i] = new Random();
			annealers[i] = new Annealer(rands[i]);
		}
		
		threadExecutor = Executors.newFixedThreadPool(numThreads);
	}

	public void setProgress(double p)
	{
		this.p = p;
		ref.setProgress(p);
		for(Annealer a : annealers) a.setProgress(p);
	}
	
	public void go()
	{
		Future[] futures = new Future[numThreads];
		
		long t = System.currentTimeMillis();		
		double r;
		double bDiff;
		while((System.currentTimeMillis()-t) < evolutionStepTime)
		{	
			for(int i = 0; i < numThreads; i++)
			{
				futures[i] = threadExecutor.submit(annealers[i]);
			}
			
			for(int i = 0; i < numThreads; i++)
			{
				try {
					futures[i].get(); // wait for each thread to complete execution
				} catch(Exception e) { }
			}
			
			// once the first annealer's candidate is accepted, the other annealer's
			// candidates are discarded.
			for(Annealer a : annealers)
			{
				bDiff = a.dIndi.getBadness()-ref.getBadness();
				r = Math.abs(rand.nextGaussian()*(0.9-p));
				
				
				if(bDiff < 0.0 || r > bDiff*0.005)
				{
					//if(bDiff > 0.0 && r > bDiff*0.005) System.out.println("step backward");
					ref.update(a.dIndi);
					//  The other annealer's results are now not usable any more so we throw them away
					break;
				}
			}
		}

	}
}










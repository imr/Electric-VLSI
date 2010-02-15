/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CycleCrossoverFavoringStrongParents.java
 * Written by Team 3: Christian Wittner
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
package com.sun.electric.tool.placement.genetic1.g1;

import com.sun.electric.tool.placement.genetic1.Chromosome;
import com.sun.electric.tool.placement.genetic1.Crossover;
import com.sun.electric.tool.placement.genetic1.Population;

import java.util.ArrayList;
import java.util.Random;

/**
 *         Implement Cycle Crossover.
 */
public class CycleCrossoverFavoringStrongParents implements Crossover {

	float crossOverRate;
	Random r;
	boolean isPositionUsed[];

	// mappings from genePosToNodeIndex
	int[] P1genePosToNodeIndex;
	int[] P2genePosToNodeIndex;

	/**
	 * 
	 * @param crossOverRate
	 */
	public CycleCrossoverFavoringStrongParents(float crossOverRate, Random r,
			int chromsomeSize) {
		this.crossOverRate = crossOverRate;
		this.r = r;
		isPositionUsed = new boolean[chromsomeSize];
		P1genePosToNodeIndex = new int[chromsomeSize];
		P2genePosToNodeIndex = new int[chromsomeSize];
	}

	public void crossover(Population population) {

		assert (r != null) : "set random seed value before first use";

		int indexOfProxy;
		int nbrOfCrossovers = Math.round(population.chromosomes.size() * crossOverRate);

		int nbrOfGenesPerChromsome = GeneticPlacement.nodeProxies.length;

		ArrayList<Chromosome> offsprings = new ArrayList<Chromosome>(
				nbrOfCrossovers);

		// chromosomes representing parents
		Chromosome[] P1 = new Chromosome[nbrOfCrossovers];
		Chromosome[] P2 = new Chromosome[nbrOfCrossovers];
		Chromosome swapChromosome, offspring;

		// select parents for offsprings favoring strong individuals
		double sumOfAllFitnessValues = 0.0;
		for (Chromosome c : population.chromosomes)
			sumOfAllFitnessValues += (1 / c.fitness.doubleValue());
		for (int i = 0; i < nbrOfCrossovers; i++) {

			// generate random value between 0 and sumOfAllFitnessValues
			double canidateSelector = r.nextDouble() * sumOfAllFitnessValues;

			// get P1
			double currentFitnessSum = 0.0;
			for (Chromosome c : population.chromosomes) {
				currentFitnessSum += (1 / c.fitness.doubleValue());
				if (currentFitnessSum >= canidateSelector) {
					P1[i] = c;
					break;
				}
			}

			// get P2
			canidateSelector = r.nextDouble() * sumOfAllFitnessValues;
			currentFitnessSum = 0.0;
			for (Chromosome c : population.chromosomes) {
				currentFitnessSum += (1 / c.fitness.doubleValue());
				if (currentFitnessSum >= canidateSelector) {
					// if we just picked P1 again get next
					if (P1[i] == c) {
						int indexOfC = population.chromosomes.indexOf(c);
						// if c is last in list get first element
						if (indexOfC == population.chromosomes.size() - 1) {
							P2[i] = population.chromosomes.get(0);
						} else {
							// just pick next in liste
							P2[i] = population.chromosomes.get(
									indexOfC + 1);
						}
					} else {
						P2[i] = c;
						break;
					}
				}
			}

		}

		int[] swapIndex;

		// while nbr of crossovers < crossover rate
		for (int i = 0; i < nbrOfCrossovers; i++) {

			assert (P1[i] != null);
			assert (P2[i] != null);
			assert (P1[i] != P2[i]);

			// create new chromsome to represent offspring
			// TODO:later get from object pool
			offspring = new Chromosome(nbrOfGenesPerChromsome);

			// calculate genePos2NodeIndexMap for P1 and P2
			// TODO: this proves that generateGenePos2IndexMapping method is not
			// well placed in GenePlacementLeftAlignedDrop
			// eventually we can computer this reverse mapping only if required
			// and
			// save it for further use.
			// computation vs memory resource!
			GenePlacementLeftRightAlignedDrop.generateGenePos2IndexMapping(
					P1[i].Index2GenePositionInChromosome, P1genePosToNodeIndex);
			GenePlacementLeftRightAlignedDrop.generateGenePos2IndexMapping(
					P2[i].Index2GenePositionInChromosome, P2genePosToNodeIndex);

			// fill list with all available indices
			for (int ind = 0; ind < isPositionUsed.length; ind++)
				isPositionUsed[ind] = false;

			int nbrOfPlacedNodes = 0;

			// for all nodes to place in offspring
			while (nbrOfPlacedNodes < isPositionUsed.length)
			// cycle crossover
			{
				// randomly select cell to be placed
				indexOfProxy = r.nextInt(isPositionUsed.length);
				while (isPositionUsed[indexOfProxy]) {
					indexOfProxy++;
					if (indexOfProxy == isPositionUsed.length)
						indexOfProxy = 0;
				}

				while (true) {
					// get gene position in P1
					// put it into P1's position in the offspring
					offspring.Index2GenePositionInChromosome[indexOfProxy] = P1[i].Index2GenePositionInChromosome[indexOfProxy];
					isPositionUsed[indexOfProxy] = true;
					nbrOfPlacedNodes++;

					offspring.GeneRotation[indexOfProxy] = P1[i].GeneRotation[indexOfProxy];
					offspring.GeneXPadding[indexOfProxy] = P1[i].GeneXPadding[indexOfProxy];
					offspring.GeneYPadding[indexOfProxy] = P1[i].GeneYPadding[indexOfProxy];

					// get node at this position from P2
					indexOfProxy = P2genePosToNodeIndex[P1[i].Index2GenePositionInChromosome[indexOfProxy]];

					// if it is already in the offspring the current cycle is
					// finished
					if (isPositionUsed[indexOfProxy])
						break;

				}// cycle ended

				// swap P1 and P2 to start the next cycle from the other
				// parent
				swapChromosome = P1[i];
				P1[i] = P2[i];
				P2[i] = swapChromosome;

				swapIndex = P1genePosToNodeIndex;
				P1genePosToNodeIndex = P2genePosToNodeIndex;
				P2genePosToNodeIndex = swapIndex;

			}

			assert (offspring.isIndex2GenePosValid());

			// create new chromosome from configuration calculated above
			offsprings.add(offspring);
		}

		for (Chromosome c : offsprings) {
			assert c.size() == GeneticPlacement.nodeProxies.length;
		}

		// add offsprings to the population
		population.chromosomes.addAll(offsprings);

	}
}
/*
 * every cell is in the same location one parent or the other
 */

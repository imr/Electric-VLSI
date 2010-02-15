/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SubPopulationProcessing.java
 * Written by Team 3: Christian Wittner, Ivan Dimitrov
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

import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.genetic1.Crossover;
import com.sun.electric.tool.placement.genetic1.GenePlacement;
import com.sun.electric.tool.placement.genetic1.Metric;
import com.sun.electric.tool.placement.genetic1.Population;
import com.sun.electric.tool.placement.genetic1.PopulationMutation;
import com.sun.electric.tool.placement.genetic1.Selection;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.logging.Level;

public class SubPopulationProcessing implements Callable<Population> {

	static int idcounter = 0;

	int id;

	Random randomGenerator;
	Population subPopulation;
	GenePlacement placement;
	Metric metric;
	Crossover crossover;
	PopulationMutation mutation;
	Selection selection;

	int epochLenght;

	public SubPopulationProcessing(int epochLenght, long randomSeed,
			int placementWidth, PlacementNodeProxy[] nodeProxies,
			List<PlacementNetwork> networks, int chromosomeSize) {
		this.epochLenght = epochLenght;
		randomGenerator = new Random(randomSeed);
		placement = new GenePlacementLeftRightAlignedDrop(placementWidth,
				chromosomeSize, nodeProxies);
		metric = new MetricBoundingBox3(networks, nodeProxies);
		crossover = new CycleCrossoverFavoringStrongParents(0.7f,
				randomGenerator, chromosomeSize);
		mutation = new PopulationMutation2(chromosomeSize);
		selection = new SelectionTournament();
		id = idcounter++;
	}

	public Population call() throws Exception {

//		assert (subPopulation != null && subPopulation.chromosomes.size() == GeneticPlacement
//				.current_population_size_per_thread);

		subPopulation.evaluate(metric, placement);
		// assign random generator of this cpu to subgeneration
		// any opertion (crossover, mutation) on this population can use it
		// but none of this operations needs a handle on this class
		subPopulation.setRandomGenerator(randomGenerator);

		for (int threadGeneration = 0; threadGeneration < epochLenght; threadGeneration++) {

			if (System.currentTimeMillis() > GeneticPlacement.MAX_RUNTIME)
				break;

//			assert (subPopulation.chromosomes.size() == GeneticPlacement
//					.current_population_size_per_thread);

			if (GeneticPlacement.IS_LOGGING_ENABLED)
				GeneticPlacement.logger.log(Level.FINE, "Thread Generation :"
						+ threadGeneration);

			if (GeneticPlacement.IS_LOGGING_ENABLED)
				GeneticPlacement.logger
						.log(Level.FINE, "Task " + id
								+ ":Start crossover in generation :"
								+ threadGeneration);
			// cross over
			crossover.crossover(subPopulation);

			if (GeneticPlacement.IS_LOGGING_ENABLED)
				GeneticPlacement.logger.log(Level.FINE, "Task " + id
						+ ":Done crossover in generation :" + threadGeneration);

			if (GeneticPlacement.IS_LOGGING_ENABLED)
				GeneticPlacement.logger.log(Level.FINE, "Task " + id
						+ ":Start mutation in generation :" + threadGeneration);
			// mutation
			mutation.mutate(subPopulation);
			if (GeneticPlacement.IS_LOGGING_ENABLED)
				GeneticPlacement.logger.log(Level.FINE, "Task " + id
						+ ":Done mutation in generation :" + threadGeneration);

			// evaluate chromosomes
			if (GeneticPlacement.IS_LOGGING_ENABLED)
				GeneticPlacement.logger.log(Level.FINE, "Task " + id
						+ ":Start evaluating subpopulation in generation :"
						+ threadGeneration);

			subPopulation.evaluate(metric, placement);
			if (GeneticPlacement.IS_LOGGING_ENABLED)
				GeneticPlacement.logger.log(Level.FINE, "Task " + id
						+ ":Done evaluating subpopulation in generation :"
						+ threadGeneration);
			// status output
			if (GeneticPlacement.IS_LOGGING_ENABLED)
				GeneticPlacement.logger.log(Level.FINE, threadGeneration
						+ " Generation best fitness"
						+ subPopulation.getBest_fitness());

			// selection
			if (GeneticPlacement.IS_LOGGING_ENABLED)
				GeneticPlacement.logger
						.log(Level.FINE, "Task " + id
								+ ":Start selection in generation :"
								+ threadGeneration);
			selection.selection(subPopulation);
			if (GeneticPlacement.IS_LOGGING_ENABLED)
				GeneticPlacement.logger.log(Level.FINE, "Task " + id
						+ ":Done selection in generation :" + threadGeneration);

			if (GeneticPlacement.IS_PROGRESS_LOGGING_ENABLED)
				logProgress(threadGeneration);
		}
//		assert (!subPopulation.chromosomes.isEmpty() && subPopulation
//				.chromosomes.size() == GeneticPlacement
//				.current_population_size_per_thread);

		return subPopulation;
	}

	// full telemetry output to log file
	void logProgress(int threadGeneration) {

		GeneticPlacement.PROGRESS_LOGGER
				.println((System.currentTimeMillis() - GeneticPlacement.START_TIME) / 1000
						+ ";"
						+ id
						+ ";"
						+ threadGeneration
						+ ";"
						+ subPopulation.getBest_fitness()
						+ ";"
						+ subPopulation.chromosomes.size()
						+ ";"
						+ GeneticPlacement.current_population_size_per_thread
						+ ";"
						+ PopulationMutation2.chromosomeAlterPaddingRate
						+ ";"
						+ PopulationMutation2.genePaddingChangeRate_current
						+ ";"
						+ PopulationMutation2.chrosomeMaxPaddingChangeStep
						+ ";"
						+ PopulationMutation2.chromosomeMoveRate
						+ ";"
						+ PopulationMutation2.geneMoveRate_current
						+ ";"
						+ PopulationMutation2.geneMoveDistance
						+ ";"
						+ PopulationMutation2.chromosomeSwapRate
						+ ";"
						+ PopulationMutation2.geneSwapRate_current
						+ ";"
						+ PopulationMutation2.chromsomeRotationRate
						+ ";"
						+ "NA");
		
	}

	public void setSubPolulation(Population population) {
		subPopulation = population;
	}
}

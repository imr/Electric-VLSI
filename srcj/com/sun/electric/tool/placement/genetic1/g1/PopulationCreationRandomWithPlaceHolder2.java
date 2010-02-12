/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PopulationCreationRandomWithPlaceHolder2.java
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
import com.sun.electric.tool.placement.genetic1.Chromosome;
import com.sun.electric.tool.placement.genetic1.Population;
import com.sun.electric.tool.placement.genetic1.PopulationCreation;

import java.util.List;
import java.util.Random;

public class PopulationCreationRandomWithPlaceHolder2 implements
		PopulationCreation {

	final static boolean DEBUG = false;

	Random random;

	public PopulationCreationRandomWithPlaceHolder2(Random random) {
		this.random = random;
	}

	/**
	 * Generates random population with added placeholer nodes.
	 */
	public Population generatePopulation(PlacementNodeProxy[] nodeProxies,
			List<PlacementNetwork> allNetworks, int populationSize) {


		boolean isPositionUsed[] = new boolean[nodeProxies.length];

		Population popululation = new Population(populationSize);

		Chromosome chrom;
		int index, pos;

		for (int i = 0; i < populationSize; i++) {

			chrom = new Chromosome(nodeProxies.length);
			popululation.chromosomes.add(chrom);
			index = 0;

			// fill list with all available indices
			for (int ind = 0; ind < isPositionUsed.length; ind++)
				isPositionUsed[ind] = false;

			// place placement node representing genes at random index
			for (PlacementNodeProxy proxy : nodeProxies) {

				// pick random position of new gene in chromosome
				pos = random.nextInt(nodeProxies.length);
				while (isPositionUsed[pos]) {
					pos++;
					if (pos == isPositionUsed.length)
						pos = 0;
				}
				isPositionUsed[pos] = true;

				chrom.Index2GenePositionInChromosome[index] = pos;
				chrom.GeneRotation[index++] = proxy.angle;
			}

			assert (chrom.isIndex2GenePosValid());
		}

		return popululation;
	}
}

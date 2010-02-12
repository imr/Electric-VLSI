/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ChromsomeMutationMoveCells.java
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

import com.sun.electric.tool.placement.genetic1.Chromosome;
import com.sun.electric.tool.placement.genetic1.ChromosomeMutation;

import java.util.Random;

public class ChromsomeMutationMoveCells implements ChromosomeMutation {

	static int nbrOfGenes2Move = -1;

	static int moveMax;

	public void setGeneMoveDitance(double geneMoveDistance, int chromosomesize) {
		moveMax = (int) (geneMoveDistance * chromosomesize);
	}

	public void mutate(Chromosome c, Random r) {

		assert (nbrOfGenes2Move > -1) : "set mutation rate before first usage";

		for (int i = 0; i < nbrOfGenes2Move; i++) {

			// select gene
			int index = r.nextInt(c.Index2GenePositionInChromosome.length);

			// random movement 0 to 10 = -5 to +5
			int move = r.nextInt(11);
			move -= 5;

			// calculate target index
			move += index;

			// cap at index bounds
			if (move < 0)
				move = 0;
			else if (move >= c.size())
				move = c.size() - 1;

			// move
			int swap = c.Index2GenePositionInChromosome[index];
			c.Index2GenePositionInChromosome[index] = c.Index2GenePositionInChromosome[move];
			c.Index2GenePositionInChromosome[move] = swap;

			assert (c.isIndex2GenePosValid());
		}

	}

	public void setMutationRate(double mutationRate) {
		nbrOfGenes2Move = (int) (mutationRate * GeneticPlacement.nodeProxies.length);
		if (nbrOfGenes2Move < 1)
			nbrOfGenes2Move = 1;
	}

}

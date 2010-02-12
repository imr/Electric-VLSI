/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ChromsomeMutationRotateCells.java
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

public class ChromsomeMutationRotateCells implements ChromosomeMutation {

	static int nbrOfGenes2Rotate = -1;

	public void mutate(Chromosome c, Random r) {
		assert (r != null);
		assert (nbrOfGenes2Rotate > -1) : "set mutation rate befor first usage";

		for (int i = 0; i < nbrOfGenes2Rotate; i++) {
			c.rotate(900, r.nextInt(c.Index2GenePositionInChromosome.length));
		}
	}

	public void setMutationRate(double mutationRate) {
		nbrOfGenes2Rotate = (int) (GeneticPlacement.nodeProxies.length * mutationRate);
		if (nbrOfGenes2Rotate < 1)
			nbrOfGenes2Rotate = 1;
	}
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ChromosomeMutationPadding.java
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

public class ChromosomeMutationPadding implements ChromosomeMutation {

	static int nbrOfPlaceHoldersToMutate = -1;

	static short maxChangePerStep;

	public ChromosomeMutationPadding(short maxChangePerStep) {
		ChromosomeMutationPadding.maxChangePerStep = maxChangePerStep;
	}

	public void mutate(Chromosome c, Random r) {

		assert (nbrOfPlaceHoldersToMutate > -1) : "set nbr of placeholders before using mutation";

		int indexOfPlaceholder;
		for (int i = 0; i < nbrOfPlaceHoldersToMutate; i++) {
			// resize placeholder
			indexOfPlaceholder = r.nextInt(c.GeneXPadding.length);

			// change x
			if (r.nextBoolean()) {
				c.GeneXPadding[indexOfPlaceholder] += r
						.nextInt(maxChangePerStep);
				if (c.GeneXPadding[indexOfPlaceholder] < 0)
					c.GeneXPadding[indexOfPlaceholder] = 0;
			}
			// or change y
			else {

				c.GeneYPadding[indexOfPlaceholder] += r
						.nextInt(maxChangePerStep);
				if (c.GeneYPadding[indexOfPlaceholder] < 0)
					c.GeneYPadding[indexOfPlaceholder] = 0;
			}
		}

	}

	public void setMutationRate(double mutationRate) {
		nbrOfPlaceHoldersToMutate = (int) (GeneticPlacement.nodeProxies.length * mutationRate);
		if (nbrOfPlaceHoldersToMutate < 1)
			nbrOfPlaceHoldersToMutate = 1;
	}

}

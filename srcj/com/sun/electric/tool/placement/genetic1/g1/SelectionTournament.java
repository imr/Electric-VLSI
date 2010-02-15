/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SelectionTournament.java
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
import com.sun.electric.tool.placement.genetic1.Population;
import com.sun.electric.tool.placement.genetic1.Selection;

import java.util.List;

/**
 * Imitate a tournament. Randomly pick to individuals to compete. The weaker one
 * gets sorted out.
 * 
 * This way a weak solution has a chance of survival and the strongest one never
 * gets dropped.
 */
public class SelectionTournament implements Selection {

	public void selection(Population population) {

		Chromosome a, b;
		List<Chromosome> chromsomes = population.chromosomes;

		// as long as we have two many individuals
		while (chromsomes.size() > GeneticPlacement.current_population_size_per_thread) {

			// select two individuals
			a = chromsomes.get(population.getRandomGenerator().nextInt(
					chromsomes.size()));
			b = a;
			while (a == b) {
				b = chromsomes.get(population.getRandomGenerator().nextInt(
						chromsomes.size()));
			}

			// remove the weaker one with the longer wire length
			if (a.fitness.doubleValue() > b.fitness.doubleValue()) {
				chromsomes.remove(a);
			} else {
				chromsomes.remove(b);
			}
		}

	}

}

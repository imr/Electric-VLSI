/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PopulationMutation2.java
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
import com.sun.electric.tool.placement.genetic1.ChromosomeMutation;
import com.sun.electric.tool.placement.genetic1.Population;
import com.sun.electric.tool.placement.genetic1.PopulationMutation;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutate by exchanging gene position in chromosome and randomly apply rotation
 * by 90 degree.
 */
public class PopulationMutation2 implements PopulationMutation {

	// define at witch percentage of progress mutation rate should be increased
	// calculated by comparing the best result of one epoch with the previous in
	// genetic placement
	public final static double MUTATION_RATE_LOWER = .004;
	public final static double MUTATION_RATE_INCREASE = .05;

	// percentage of chromosmes to swap
	static double chromosomeSwapRate = .05;
	// percentage of genes to be swapped in a chromosme
	final static double geneSwapRate_START = .001;
	final static double geneSwapRate_STEP = .5;
	static double geneSwapRate_current;
	// percentage of chromosmes to rotate
	static double chromsomeRotationRate = .05;
	// percentage of genes to rotate in a chromosme
	final static double geneRotationRate_START = .001;
	final static double geneRotationRate_STEP = .5;
	static double geneRotationRate_current;
	// percentage of chromosmes to move
	static double chromosomeMoveRate = .1;
	// percentage of genes to move in a chromosme
	static double geneMoveRate_START = .001;
	static double geneMoveRate_STEP = .5;
	static double geneMoveRate_current;
	// percentage of chromosome string length a gene is maximally moved
	static double geneMoveDistance = .001;
	// percentage of placeholders to move
	static double chromosomeAlterPaddingRate = .05;
	// percentage of chromosome to resize
	final static double genePaddingChangeRate_START = .001;
	final static double genePaddingChangeRate_STEP = .5;
	static double genePaddingChangeRate_current;
	// max stepwidth for padding changes
	static short chrosomeMaxPaddingChangeStep = 5;

	static ChromosomeMutation swapCellsMutation, rotateCellsMutation,
			moveCellsMutation, changePaddingMutation;

	static int chromosomeSize;

	public PopulationMutation2(int chromosomesize) {

		chromosomeSize = chromosomesize;
		chrosomeMaxPaddingChangeStep = 10;

		swapCellsMutation = new ChromsomeMutationSwapCells();
		rotateCellsMutation = new ChromsomeMutationRotateCells();
		moveCellsMutation = new ChromsomeMutationMoveCells();
		changePaddingMutation = new ChromosomeMutationPadding(
				chrosomeMaxPaddingChangeStep);

		resetMutationRates();

	}

	public static void resetMutationRates() {
		geneSwapRate_current = geneSwapRate_START;
		geneRotationRate_current = geneRotationRate_START;
		geneMoveRate_current = geneMoveRate_START;
		genePaddingChangeRate_current = genePaddingChangeRate_START;
		applyMutationRates();
	}

	public static void increaseMutationRate() {
		geneSwapRate_current /= geneSwapRate_STEP;
		geneRotationRate_current /= geneRotationRate_STEP;
		geneMoveRate_current /= geneMoveRate_STEP;
		genePaddingChangeRate_current /= genePaddingChangeRate_STEP;
		applyMutationRates();
	}

	public static void lowerMutationRate() {
		geneSwapRate_current *= geneSwapRate_STEP;
		geneRotationRate_current *= geneRotationRate_STEP;
		geneMoveRate_current *= geneMoveRate_STEP;
		genePaddingChangeRate_current *= genePaddingChangeRate_STEP;
		applyMutationRates();
	}

	static void applyMutationRates() {
		swapCellsMutation.setMutationRate(geneSwapRate_current);
		rotateCellsMutation.setMutationRate(geneRotationRate_current);
		moveCellsMutation.setMutationRate(geneMoveRate_current);
		((ChromsomeMutationMoveCells) moveCellsMutation).setGeneMoveDitance(
				geneMoveDistance, chromosomeSize);
		changePaddingMutation.setMutationRate(genePaddingChangeRate_current);
	}

	public void mutate(Population p) {

		// swap gene positions in a chromosome
		swapGene(p);

		// switch orientation of one gene
		rotateGenes(p);

		moveGenes(p);

		alterPadding(p);
	}

	private void alterPadding(Population p) {
		List<Chromosome> chromosome2Mutate = selectChromosome2Mutate(
				chromosomeAlterPaddingRate, p);

		for (Chromosome c : chromosome2Mutate) {
			changePaddingMutation.mutate(c, p.getRandomGenerator());
			c.altered = true;
			assert (c.isIndex2GenePosValid());
		}

		// add mutations to population
		p.chromosomes.addAll(chromosome2Mutate);

	}

	private void moveGenes(Population p) {
		List<Chromosome> chromosome2Mutate = selectChromosome2Mutate(
				chromosomeMoveRate, p);

		for (Chromosome c : chromosome2Mutate) {
			moveCellsMutation.mutate(c, p.getRandomGenerator());
			c.altered = true;
			assert (c.isIndex2GenePosValid());
		}

		// add mutations to population
		p.chromosomes.addAll(chromosome2Mutate);

	}

	private void rotateGenes(Population p) {
		List<Chromosome> chromosome2Mutate = selectChromosome2Mutate(
				chromsomeRotationRate, p);

		for (Chromosome c : chromosome2Mutate) {
			rotateCellsMutation.mutate(c, p.getRandomGenerator());
			c.altered = true;
			assert (c.isIndex2GenePosValid());
		}

		// add mutations to population
		p.chromosomes.addAll(chromosome2Mutate);
	}

	private void swapGene(Population p) {
		List<Chromosome> chromosome2Mutate = selectChromosome2Mutate(
				chromosomeSwapRate, p);

		for (Chromosome c : chromosome2Mutate) {

			swapCellsMutation.mutate(c, p.getRandomGenerator());
			c.altered = true;
			assert (c.isIndex2GenePosValid());
		}

		// add mutations to population
		p.chromosomes.addAll(chromosome2Mutate);
	}

	/**
	 * Selects chromosomes to mutate from population an returns a list of clones
	 * of the selected chromsomes.
	 * 
	 * @param mutationRate
	 *            Percentage of chromosomes that should be selected out of the
	 *            population.
	 * @param p
	 *            Population to select from.
	 * @return List of clones.
	 */
	private List<Chromosome> selectChromosome2Mutate(double mutationRate,
			Population p) {
		List<Chromosome> chromosmes2mutate = new ArrayList<Chromosome>(
				(int) (p.chromosomes.size() * mutationRate));

		// select chromosomes to mutate from population
		while (chromosmes2mutate.size() < p.chromosomes.size() * mutationRate) {
			Chromosome c = p.chromosomes.get(p.getRandomGenerator().nextInt(
					p.chromosomes.size()));
			if (!chromosmes2mutate.contains(c))
				chromosmes2mutate.add(c);
		}

		// replace chromosomes by clones
		for (int i = 0; i < chromosmes2mutate.size(); i++) {
			chromosmes2mutate.set(i, chromosmes2mutate.get(i).clone());
		}

		return chromosmes2mutate;
	}

}

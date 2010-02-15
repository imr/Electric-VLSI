/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Chromosome.java
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
package com.sun.electric.tool.placement.genetic1;

import com.sun.electric.tool.placement.PlacementFrame.PlacementPort;

import java.util.logging.Level;

/**
 * Chromosome representing Placement.
 */

//TODO: remove comment from root element if you want to use xmlstorage. not java 1.5 compliant. or 
//include jar with jaxb for java 1.5.
//@XmlRootElement
public class Chromosome implements Comparable<Chromosome> {

	final static Level LOG_LEVEL = Level.FINEST;

	// true if chromsome was altered
	// just created, mutated
	public boolean altered;

	public Double fitness;

	final static boolean DEBUG = false;

	// stores the position of a certain index in the gene
	// in the first entry the position of the gene representing the first node
	// is stored
	public int[] Index2GenePositionInChromosome;

	public int[] GeneXPos;
	public int[] GeneYPos;
	public short[] GeneRotation;

	public short[] GeneYPadding;
	public short[] GeneXPadding;

	public Chromosome() {

	}

	public Chromosome(int nbrOfGenes) {
		altered = true;

		GeneXPos = new int[nbrOfGenes];
		GeneYPos = new int[nbrOfGenes];
		GeneRotation = new short[nbrOfGenes];
		GeneYPadding = new short[nbrOfGenes];
		GeneXPadding = new short[nbrOfGenes];
		Index2GenePositionInChromosome = new int[nbrOfGenes];

		fitness = new Double(Double.MAX_VALUE);

	}

	/**
	 * Rotate by given angle in 10th degree.
	 * 
	 * @param angle
	 *            the angle of rotation (in tenth-degrees)
	 */
	public void rotate(int angle, int geneIndex) {

		GeneRotation[geneIndex] = (short) ((GeneRotation[geneIndex] + angle) % 3600);

		altered = true;

	}

	/**
	 * 
	 * @return Number of genes this chromosome consists of.
	 */
	public int size() {
		return Index2GenePositionInChromosome.length;
	}

	public int compareTo(Chromosome o) {
		return fitness.compareTo(o.fitness);
	}

	public Chromosome clone() {

		Chromosome newChromosome = new Chromosome(GeneXPos.length);

		newChromosome.fitness = fitness;

		newChromosome.Index2GenePositionInChromosome = Index2GenePositionInChromosome
				.clone();

		assert (newChromosome.isIndex2GenePosValid());

		newChromosome.GeneRotation = GeneRotation.clone();
		newChromosome.GeneXPos = GeneXPos.clone();
		newChromosome.GeneYPos = GeneYPos.clone();
		newChromosome.GeneXPadding = GeneXPadding.clone();
		newChromosome.GeneYPadding = GeneYPadding.clone();

		assert newChromosome.altered == true;

		return newChromosome;
	}

	/**
	 * Calculate port location with applied rotation angle of this gene.
	 * 
	 * @param port
	 *            Port of which location is to be computed.
	 * @return X coordinate offset relative to center of gene.
	 */
	public double getPortXOffset(PlacementPort port, int geneIndex) {
		switch (GeneRotation[geneIndex]) {
		case 0:
			return port.getOffX();
		case 900:
			return -port.getOffY();
		case 1800:
			return -port.getOffX();
		case 2700:
			return port.getOffY();
		default:
			System.err
					.println(this.getClass().getName()
							+ " unsupported rotation angle: "
							+ GeneRotation[geneIndex]);
			return -1;
		}

	}

	/**
	 * Calculate port location with applied rotation angle of this gene.
	 * 
	 * @param port
	 *            Port of which location is to be computed.
	 * @return Y coordinate offset relative to center of gene.
	 */
	public double getPortYOffset(PlacementPort port, int geneIndex) {
		switch (GeneRotation[geneIndex]) {
		case 0:
			return port.getOffY();
		case 900:
			return port.getOffX();
		case 1800:
			return -port.getOffY();
		case 2700:
			return -port.getOffX();
		default:
			System.err
					.println(this.getClass().getName()
							+ " unsupported rotation angle: "
							+ GeneRotation[geneIndex]);
			return -1;
		}

	}

	/**
	 * Use to assert that each index is represented exactly once in the
	 * index2gene map.
	 */
	public boolean isIndex2GenePosValid() {

		// System.err.println("assertion running");

		int matchCount;
		for (int i = 0; i < Index2GenePositionInChromosome.length; i++) {
			matchCount = 0;
			for (int z = 0; z < Index2GenePositionInChromosome.length; z++) {
				if (Index2GenePositionInChromosome[z] == i) {
					matchCount++;
				}
			}
			if (matchCount != 1)
				return false;
		}

		return true;
	}

}

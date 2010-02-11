/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StartUpStageWorker.java
 * Written by Team 7: Felix Schmidt
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
package com.sun.electric.tool.placement.forceDirected2.forceDirected.staged;

import com.sun.electric.database.geometry.GenMath.MutableInteger;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.forceDirected2.PlacementForceDirectedStaged;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.CheckboardingField;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.CheckboardingPattern;
import com.sun.electric.tool.placement.forceDirected2.metrics.AbstractMetric;
import com.sun.electric.tool.placement.forceDirected2.metrics.BBMetric;
import com.sun.electric.tool.placement.forceDirected2.utils.PlacementProperties;
import com.sun.electric.tool.placement.forceDirected2.utils.concurrent.Stage;
import com.sun.electric.tool.placement.forceDirected2.utils.concurrent.StageWorker;
import com.sun.electric.tool.placement.forceDirected2.utils.output.PNGOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class StartUpStageWorker extends StageWorker {

	protected static Map<PlacementNode, Map<PlacementNode, MutableInteger>> connectivityMap = null;
	private double velocityFactor;

	private List<PlacementNode> nodesToPlace;

	private List<PlacementNetwork> allNetworks;
	private int widthCheckBoarding;
	private int heightCheckBoarding;
	private double fieldSize;
	private CheckboardingPattern checkPattern;

	public StartUpStageWorker(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks) {
		this.nodesToPlace = nodesToPlace;
		this.allNetworks = allNetworks;
	}

	protected double bestSize() {

		double result = 0.0;

		for (PlacementNode node : this.nodesToPlace) {
			double cellMax = (node.getHeight() > node.getWidth()) ? node.getHeight() : node.getWidth();
			result = (cellMax > result) ? cellMax : result;
		}

		return result;
	}

	protected void calculateVelocityFactor() {
		this.velocityFactor = 1;
		double tmp = this.nodesToPlace.size();
		while (tmp > 1) {
			tmp /= 10;
			this.velocityFactor /= 10;
		}
	}

	/**
	 * Find the best CheckboardingPatternLayout
	 */
	protected void createCheckboardingPattern() {
		int findBestSizeMax = (int) Math.ceil(Math.sqrt(this.nodesToPlace.size()));
		int bestField = this.nodesToPlace.size() * 2;
		int bestIdx = 0;
		int divergence = PlacementProperties.getInstance().getDivergence();

		int start = ((findBestSizeMax - divergence) < 1) ? 1 : findBestSizeMax - divergence;
		int end = ((findBestSizeMax + divergence) > this.nodesToPlace.size()) ? this.nodesToPlace.size() : findBestSizeMax + divergence;

		for (int i = start; i < end; ++i) {
			// field = i * ceil(#nodes / i)
			int field = (i * (int) Math.ceil((double) this.nodesToPlace.size() / (double) i));
			if (field < bestField) {
				bestField = field;
				bestIdx = i;
			}
		}

		this.widthCheckBoarding = (bestIdx * 1);
		this.heightCheckBoarding = (int) (Math.ceil((double) this.nodesToPlace.size() / (double) bestIdx) * 1);

		this.checkPattern = new CheckboardingPattern(this.widthCheckBoarding, this.heightCheckBoarding, this.fieldSize, this.fieldSize);
	}

	protected void fillCheckboardingPattern() {
		Random rand = new Random(System.currentTimeMillis());
		List<Integer> places = new ArrayList<Integer>(this.heightCheckBoarding * this.widthCheckBoarding);

		for (int i = 0; i < (this.heightCheckBoarding * this.widthCheckBoarding); i++) {
			places.add(new Integer(i));
		}

		for (PlacementNode node : this.nodesToPlace) {

			int listPos = rand.nextInt(places.size());
			int nodePos = places.remove(listPos).intValue();

			int x = nodePos % this.widthCheckBoarding, y = nodePos / this.widthCheckBoarding;
			CheckboardingField field = this.checkPattern.getField(x, y);
			node.setPlacement(field.getLocation().getX(), field.getLocation().getY());
			field.setNode(node);
		}
	}

	public void run() {
		this.calculateVelocityFactor();

		this.fieldSize = this.bestSize();
		this.createCheckboardingPattern();
		this.fillCheckboardingPattern();

		PlacementForceDirectedStaged.setCheckboardingPattern(this.checkPattern);

		if (PlacementProperties.getInstance().getIterations() != 0) {

			PlacementProperties properties = PlacementProperties.getInstance();
			List<StageWorker> forces = new ArrayList<StageWorker>();
			List<StageWorker> move = new ArrayList<StageWorker>();
			List<StageWorker> overlap = new ArrayList<StageWorker>();
			List<StageWorker> endWorker = new ArrayList<StageWorker>();

			double threshold = PlacementProperties.getInstance().getOverlappingThreshold();

			for (int i = 0; i < properties.getNumOfThreads(); i++) {
				forces.add(new CalculateForcesStageWorker(StartUpStageWorker.connectivityMap, this.allNetworks));
				move.add(new PlaceNodesStageWorker(this.velocityFactor));
				overlap.add(new OverlapWorker(threshold, i));
			}

			long finalTimeStamp = System.currentTimeMillis() + PlacementProperties.getInstance().getTimeout() * 1000;

			AbstractMetric bb = new BBMetric(this.nodesToPlace, this.allNetworks);
			PNGOutput out = new PNGOutput(this.nodesToPlace, this.allNetworks);
			endWorker.add(new EndWorker(PlacementProperties.getInstance().getIterations(), (StartUpStage) this.stage, this.widthCheckBoarding,
					this.heightCheckBoarding, this.checkPattern, this.velocityFactor, out, bb, finalTimeStamp));

			Stage calculateForces = new Stage(forces);
			Stage moveNodes = new Stage(move);
			Stage resolveOverlap = new Stage(overlap);
			Stage endStage = new Stage(endWorker);

			calculateForces.getNextStages().add(moveNodes);
			moveNodes.getNextStages().add(resolveOverlap);
			resolveOverlap.getNextStages().add(endStage);
			endStage.getNextStages().add(calculateForces);

			((StartUpStage) this.stage).getStages().add(calculateForces);
			((StartUpStage) this.stage).getStages().add(moveNodes);
			((StartUpStage) this.stage).getStages().add(resolveOverlap);
			((StartUpStage) this.stage).getStages().add(endStage);

			calculateForces.start();
			moveNodes.start();
			resolveOverlap.start();
			endStage.start();

			int totalNumOfPorts = 0;
			for (PlacementNode node : this.nodesToPlace) {
				totalNumOfPorts += node.getPorts().size();
			}

			int stepWidth = 10;
			for (int i = 0; i < this.heightCheckBoarding; i += stepWidth) {
				for (int j = 0; j < this.widthCheckBoarding; j += stepWidth) {
					CheckboardingField[][] fields = this.checkPattern.getFields(j, i, stepWidth, stepWidth);
					calculateForces.getInput(this).add(new PlacementDTO(fields, i * this.widthCheckBoarding + j));
				}
			}

			try {
				calculateForces.join();
				moveNodes.join();
				resolveOverlap.join();
				endStage.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			this.stage.stop();
		}

	}

	public void setStage(StartUpStage stage) {
		this.stage = stage;
	}

}

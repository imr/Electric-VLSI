/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementForceDirectedStaged.java
 * Written by Team 7: Felix Schmidt, Daniel Lechner
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
package com.sun.electric.tool.placement.forceDirected2;

import com.sun.electric.database.geometry.GenMath.MutableInteger;
import com.sun.electric.tool.placement.PlacementFrame;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.staged.FinalizeWorker;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.staged.PlacementDTO;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.staged.StartUpStage;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.staged.StartUpStageWorker;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.CheckboardingField;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.CheckboardingPattern;
import com.sun.electric.tool.placement.forceDirected2.metrics.AbstractMetric;
import com.sun.electric.tool.placement.forceDirected2.metrics.BBMetric;
import com.sun.electric.tool.placement.forceDirected2.utils.GlobalVars;
import com.sun.electric.tool.placement.forceDirected2.utils.PlacementProperties;
import com.sun.electric.tool.placement.forceDirected2.utils.concurrent.Stage;
import com.sun.electric.tool.placement.forceDirected2.utils.concurrent.StageWorker;
import com.sun.electric.tool.placement.forceDirected2.utils.output.DebugMessageHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parallel Placement
 * 
 * Base class for placement algorithm This class starts the first stage of the
 * pipeline and provides several settings of the placement
 */
public class PlacementForceDirectedStaged extends PlacementFrame {

	private static final String ALGORITHM_NAME = "Force-Directed-2";

	protected static Map<PlacementNode, Map<PlacementNode, MutableInteger>> connectivityMap;
	private static volatile int globalCounter = 0;
	private static CheckboardingPattern pattern;
	private static int movementCounter = 0;
	private static Map<PlacementNode, AdditionalNodeData> nodeData;

	public static int getGlobalCounter() {
		return PlacementForceDirectedStaged.globalCounter;
	}

	public static synchronized int getMovementCounter() {
		return PlacementForceDirectedStaged.movementCounter;
	}

	public static Map<PlacementNode, AdditionalNodeData> getNodeData() {
		return nodeData;
	}

	public static synchronized void incMovementCounter() {
		PlacementForceDirectedStaged.movementCounter++;
	}

	public static void setCheckboardingPattern(CheckboardingPattern pattern) {
		PlacementForceDirectedStaged.pattern = pattern;
	}

	public static synchronized void setMovementCounter(int value) {
		PlacementForceDirectedStaged.movementCounter = value;
	}

	@Override
	public String getAlgorithmName() {
		return ALGORITHM_NAME;
	}

	public synchronized void incGlobalCounter() {
		PlacementForceDirectedStaged.globalCounter++;
	}

	@Override
	public void runPlacement(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks, String cellName) {

		nodeData = new HashMap<PlacementNode, AdditionalNodeData>();
		globalCounter = 0;
		movementCounter = 0;

		for (PlacementNode node : nodesToPlace) {

			AdditionalNodeData dataSet = new AdditionalNodeData(null);

			nodeData.put(node, dataSet);

		}

		GlobalVars.numOfNodes = new Integer(nodesToPlace.size());

		System.out.println("Algorithm: " + this.getAlgorithmName());

		long start = System.currentTimeMillis();
		int nThreads = PlacementProperties.getInstance().getNumOfThreads();

		// set up stages
		List<StageWorker> startUp = new ArrayList<StageWorker>();

		startUp.add(new StartUpStageWorker(nodesToPlace, allNetworks));

		StartUpStage startUpStage = new StartUpStage(startUp);
		startUpStage.start();

		try {
			startUpStage.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		CheckboardingField[][] fields = pattern.getAll();

		List<StageWorker> finalizeWorker = new ArrayList<StageWorker>();
		for (int i = 0; i < nThreads; i++) {
			finalizeWorker.add(new FinalizeWorker(fields[0].length));
		}

		Stage finalizeStage = new Stage(finalizeWorker);
		finalizeStage.start();

		for (int i = 0; i < fields[0].length; i++) {
			PlacementDTO dto = new PlacementDTO(fields, i);
			finalizeStage.getInput(null).add(dto);
		}

		try {
			finalizeStage.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		long end = System.currentTimeMillis();

		System.out.println("finished: " + (end - start) + " ms.");

		System.out.println("round timhe: " + ((end - start) / GlobalVars.rounds.doubleValue()) + " ms.");

		DebugMessageHandler.printOnStdOut();

		AbstractMetric bmetric = new BBMetric(nodesToPlace, allNetworks);
		System.out.println(bmetric.toString());

	}
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementSimulatedAnnealing.java
 * Written by Team 6: Sebastian Roether, Jochen Lutz
 *
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.placement.simulatedAnnealing2;

import com.sun.electric.tool.placement.PlacementFrame;
import com.sun.electric.tool.placement.simulatedAnnealing2.PositionIndex.AreaSnapshot;
import com.sun.electric.util.math.Orientation;

import java.awt.geom.Point2D;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Implementation of the simulated annealing placement algorithm
 *
 * Things to do:
 *     In column mode, flip alternating columns horizontally
 *     Implement row mode
 *     Handle overlap specification, and also routing channel specification
 */
public class PlacementSimulatedAnnealing extends PlacementFrame {
	public PlacementParameter numThreadsParam = new PlacementParameter("threads", "Number of threads:",
		Runtime.getRuntime().availableProcessors());
	public PlacementParameter maxRuntimeParam = new PlacementParameter("runtime", "Runtime (in seconds, 0 for no limit):", 240);
	public static final int PLACEMENTSTYLE_ANY = 0;
	public static final int PLACEMENTSTYLE_COLUMNS = 1;
	public static final int PLACEMENTSTYLE_ROWS = 2;
	public PlacementParameter placementStyleParam = new PlacementParameter("placementStyle", "Placement Style:", 0,
		new String[] {"Any Orientation", "Column-based Placement" /*, "Row-based Placement" */ });

	// Tweaking Parameter
	private final double OVERLAP_WEIGHT = 100;
	private final double MANHATTAN_WEIGHT = 0.1;
	private final double AREA_WEIGHT = 10000;

	// Temperature control
	private int iterationsPerTemperatureStep; // if maximum runtime is > 0 this is calculated each temperature step
	private double startingTemperature = 0;
	private double temperature;
	private int temperatureSteps = 0; // how often will the temperature be decreased
	private int temperatureStep;

	private int perturbations_tried;
	private long stepStartTime;
	private long timestampStart = 0;
	private double maxChipLength = 0;

	private double totalArea = 0;

	private List<ProxyNode> nodesToPlace;
	private List<PlacementNetwork> allNetworks;
	private BoundingBoxMetric metric = null;
	private Map<PlacementNode, ProxyNode> proxyMap;
	private Map<PlacementNetwork, Double> netLengths = null;
	private PositionIndex posIndex;

	// for column-based placement
	private List<Double> columnCoords;
	private double avgRowHeight;

	private int accepts = 0; // moves accepted
	private int conflicts = 0; // moves that were accepted but not made because of interference from other threads

	int stepsPerUpdate = 50;

	// Debugging performance
	private final boolean performance_log = false;
	private final String performance_log_filename = "placement.log";
	double[] lengthLog;
	double[] timestampLog;
	double[] temperatureLog;
	double[] areaLog;
	double[] stepsizeLog;
	double[] acceptLog;
	double[] conflictLog;
	double[] stepdurationLog;

	/**
	 * Method to return the name of this placement algorithm.
	 * @return the name of this placement algorithm.
	 */
	public String getAlgorithmName() {
		return "Simulated-Annealing-2";
	}

	/**
	 * Method to do placement by simulated annealing.
	 * @param nodesToPlace a list of all nodes that are to be placed.
	 * @param allNetworks a list of all networks that connect the nodes.
	 * @param cellName the name of the cell being placed.
	 */
	public void runPlacement(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks, String cellName) {
		this.setParamterValues(this.numThreadsParam.getIntValue(), this.maxRuntimeParam.getIntValue());

		if (performance_log) {
			lengthLog = new double[1000000];
			timestampLog = new double[1000000];
			temperatureLog = new double[1000000];
			areaLog = new double[1000000];
			stepsizeLog = new double[1000000];
			acceptLog = new double[1000000];
			conflictLog = new double[1000000];
			stepdurationLog = new double[1000000];
		}

		timestampStart = System.currentTimeMillis();

		this.allNetworks = allNetworks;
		metric = new BoundingBoxMetric();
		temperatureStep = 0;
		iterationsPerTemperatureStep = runtime > 0 ? 100 : getDesiredMoves(nodesToPlace);
		perturbations_tried = 0;
		accepts = 0;
		conflicts = 0;

		List<PlacementNetwork> ignoredNets = new ArrayList<PlacementNetwork>();
		for (PlacementNetwork net : allNetworks)
		{
			if (net.getPortsOnNet().size() >= nodesToPlace.size() * 0.4 && net.getPortsOnNet().size() > 100)
			{
				System.out.println("WARNING: Ignoring network " + net.toString() +
					" because it has too many ports (" + net.getPortsOnNet().size() + ")");
				ignoredNets.add(net);
			}
		}

		// create an initial layout to be improved by simulated annealing
		initLayout(nodesToPlace);

		netLengths = new HashMap<PlacementNetwork, Double>();

		// create proxies for placement nodes and insert in lists and maps
		this.nodesToPlace = new ArrayList<ProxyNode>(nodesToPlace.size());
		this.proxyMap = new HashMap<PlacementNode, ProxyNode>();
		for (PlacementNode p : nodesToPlace) {
			ProxyNode proxy = new ProxyNode(p, ignoredNets);
			this.nodesToPlace.add(proxy);
			proxyMap.put(p, proxy);
		}

		// Sum up the total node area. This is used as reference value for the area metric
		totalArea = 0;
		for (ProxyNode node : this.nodesToPlace)
			totalArea += node.width * node.height;

		// Calculate the working area for the process
		// nodes will not be placed outside the working area
		// This is because there is no use in moving nodes "miles" away ...
		maxChipLength = Math.sqrt(totalArea) * 4;
		posIndex = new PositionIndex(maxChipLength, this.nodesToPlace);

		// calculate the starting temperature
		startingTemperature = getStartingTemperature(nodesToPlace, maxChipLength, runtime);
		temperature = startingTemperature;
		temperatureSteps = countTemperatureSteps(startingTemperature);

		for (ProxyNode p : this.nodesToPlace)
			p.apply();

//if (placementStyleParam.getIntValue() != PLACEMENTSTYLE_COLUMNS) {
		// precalculate net lengths
		for (PlacementNetwork net : allNetworks)
			netLengths.put(net, new Double(metric.netLength(net, proxyMap)));

		long startingLength = Math.round(metric.netLength(allNetworks, proxyMap));
		stepStartTime = System.nanoTime();

		SimulatedAnnealing[] threads = new SimulatedAnnealing[numOfThreads];

		for (int n = 0; n < numOfThreads; n++) {
			threads[n] = new SimulatedAnnealing();
			threads[n].start();
		}

		for (int i = 0; i < numOfThreads; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
//}
		// cleanup overlap
		cleanup();

		// Apply the placement of the proxies to the actual nodes
		for (ProxyNode p : this.nodesToPlace)
			p.apply();
System.out.println("STARTING LENGTH=" + startingLength + ", ENDING LENGTH=" +
	Math.round(metric.netLength(allNetworks, proxyMap)));

			if (performance_log) {
			try {
				FileWriter f = new FileWriter(performance_log_filename);

				for (int i = 0; i < temperatureStep; i++)
					f.append((long) timestampLog[i] + "," + (int) lengthLog[i] + "," + (int) temperatureLog[i] + ","
							+ areaLog[i] + "," + stepsizeLog[i] + "," + acceptLog[i] + ","
							+ conflictLog[i] + "," + stepdurationLog[i] + "\n");
				f.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Method that counts how often the temperature will be decreased before
	 * going below 1.
	 */
	private int countTemperatureSteps(double startingTemperature) {
		double temperature = startingTemperature;
		int steps = 0;

		while (temperature > 1) {
			steps++;
			temperature = coolDown(temperature);
		}

		return steps;
	}

	/**
	 * Method to calculate the initial temperature. It uses the standard deviation
	 * of the net length for random placements to derive the starting temperature.
	 * @param length maximum length of the chip
	 * @return the initial temperature
	 */
	private double getStartingTemperature(List<PlacementNode> nodes, double length, double runtime) {
		double[] metrics = new double[1000]; // sample size
		double sigma = 0;
		double average = 0;
		Random r = new Random();

		double width = length;
		double height = length;

		// collect samples of the cost function
		for (int i = 0; i < metrics.length; i++) {
			for (PlacementNode p : nodes) {
				p.setPlacement(r.nextDouble() * width, r.nextDouble() * height);
			}

			metrics[i] = metric.netLength(allNetworks);
		}

		// calculate average
		for (int i = 0; i < metrics.length; i++) {
			average += metrics[i];
		}
		average /= metrics.length;

		// calculate standard deviation
		for (int i = 0; i < metrics.length; i++) {
			sigma += (metrics[i] - average) * (metrics[i] - average);
		}
		sigma = Math.sqrt(sigma / metrics.length);

		// set starting temperature so that the acceptance function
		// accepts worsening of -sigma with a probability of 0.3
		// e^(-sigma /startingTemperature) = 0.3 # solve for startingTemperature
		// TODO THIS IS TWEAKING DATA
		double startingTemperature = sigma * (-1 / (2 * Math.log(0.3)));

		// If a maximum runtime is set, only the last temperature steps are done
		if (runtime > 0) {
			double msPerMove = guessMillisecondsPerMove();
			int desired_moves = getDesiredMoves(nodes);
			int possibleSteps = Math.max((int) ((runtime * 1000) / (msPerMove * desired_moves)), 5);
			int stepsUntilStop = countTemperatureSteps(startingTemperature);

			for (int i = 0; i < stepsUntilStop - possibleSteps; i++)
				startingTemperature = coolDown(startingTemperature);
		}

		return startingTemperature;
	}

	/**
	 * Method that calculates how many moves per temperature step are necessary.
	 * @param nodes
	 * @return
	 */
	private int getDesiredMoves(List<PlacementNode> nodes) {
		// TODO THIS IS TWEAKING DATA
		return Math.max((int) (nodes.size() * Math.sqrt(nodes.size())), 8719);
	}

	/**
	 * Method that estimates how many moves can be evaluated with the current settings.
	 * @return
	 */
	private double guessMillisecondsPerMove() {
		double startTime = System.currentTimeMillis();
		double sampleSize = 20000;

		// start the threads
		Thread gatherers[] = new Thread[numOfThreads];
		for (int i = 0; i < numOfThreads; i++) {
			gatherers[i] = new SampleGatherer((int) (sampleSize / numOfThreads));
			gatherers[i].start();
		}

		// wait until they have finished
		for (int i = 0; i < numOfThreads; i++) {
			try {
				gatherers[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// A move actually takes more time than we measured
		return (System.currentTimeMillis() - startTime) * 2 / sampleSize;
	}

	/**
	 * This class is used to guess how many moves per second can be calculated
	 */
	class SampleGatherer extends Thread {
		int samplesCount = 0;

		public SampleGatherer(int samplesCount) {
			this.samplesCount = samplesCount;
		}

		public void run() {
			Random r = new Random();

			for (int i = 0; i < samplesCount; i++) {
				ProxyNode proxy = nodesToPlace.get(r.nextInt(nodesToPlace.size()));
				metric.netLength(proxy.getNets());
			}
		}
	}

	/**
	 * Method to calculate the cooling of the simulated annealing process.
	 * @param temp the current temperature
	 * @return the lowered temperature
	 */
	private double coolDown(double temp) {
		return temp * 0.99 - 0.1;
	}

	/**
	 * Synchronized method that does temperature and time control. Threads
	 * should call this periodically.
	 * @param tries how many perturbation the thread tried since last calling update
	 * @param acceptCount how many of the perturbations that thread tried since last
	 *            calling update were accepted
	 * @param conflictCount how many of the perturbations that were accepted since last
	 *            calling update were dropped due to conflicts with other threads
	 */
	public synchronized void update(int tries, int acceptCount, int conflictCount) {
		this.perturbations_tried += tries;
		this.accepts += acceptCount;
		this.conflicts += conflictCount;

		// decrease temperature if enough iterations were done
		if (this.perturbations_tried >= iterationsPerTemperatureStep) {
			long stepDuration = System.nanoTime() - stepStartTime;
			stepStartTime = System.nanoTime();

			// this will hopefully be useful when optimizing
			if (performance_log) {
				lengthLog[temperatureStep] = metric.netLength(allNetworks, proxyMap);
				timestampLog[temperatureStep] = System.currentTimeMillis() - timestampStart;
				temperatureLog[temperatureStep] = temperature;
				areaLog[temperatureStep] = posIndex.area.getArea() / totalArea;
				stepsizeLog[temperatureStep] = iterationsPerTemperatureStep;
				acceptLog[temperatureStep] = this.accepts;
				conflictLog[temperatureStep] = this.conflicts;
				stepdurationLog[temperatureStep] = stepDuration;
			}

			// adjust how many moves to try before the next temperature decrease
			if (runtime > 0) {
				// use the observed time from the last step to calculate
				// how many move to do
				long elapsedTime = System.currentTimeMillis() - timestampStart;
				long remainingTime = runtime * 1000 - elapsedTime;
				long remainingSteps = temperatureSteps - temperatureStep;
				double allowedTimePerStep = 1e6 * ((double) remainingTime / remainingSteps);

				iterationsPerTemperatureStep *= allowedTimePerStep / stepDuration;
				iterationsPerTemperatureStep = Math.max(stepsPerUpdate, iterationsPerTemperatureStep);
			}

			temperatureStep++;
			temperature = coolDown(temperature);
			this.perturbations_tried = 0;
			this.accepts = 0;
			this.conflicts = 0;
		}
	}

	/**
	 * Method that generates an initial node placement.
	 * @param nodesToPlace a list of nodes to place
	 */
	private void initLayout(List<PlacementNode> nodesToPlace) {
		double xPos = 0, yPos = 0;
		double maxHeight = 0, maxWidth = 0;

		// place the nodes in sqrt(n) rows * sqrt(n) nodes with no overlaps
		switch (placementStyleParam.getIntValue())
		{
			case PLACEMENTSTYLE_ANY:
			case PLACEMENTSTYLE_ROWS:
				int numRows = (int) Math.round(Math.sqrt(nodesToPlace.size()));
				for (int i = 0; i < nodesToPlace.size(); i++)
				{
					PlacementNode plNode = nodesToPlace.get(i);
					plNode.setPlacement(xPos+plNode.getWidth()/2, yPos);
					xPos += plNode.getWidth();
					maxHeight = Math.max(maxHeight, plNode.getHeight());
					if ((i % numRows) == numRows - 1)
					{
						yPos += maxHeight;
						maxHeight = 0;
						xPos = 0;
					}
				}
				break;
			case PLACEMENTSTYLE_COLUMNS:
				double totalHeight = 0;
				for(PlacementNode plNode : nodesToPlace) totalHeight += plNode.getHeight();
				double avgCellHeight = totalHeight / nodesToPlace.size();
				avgRowHeight = Math.sqrt(totalHeight * nodesToPlace.get(0).getWidth());
				int numCols = (int)Math.round(totalHeight / avgRowHeight);
				columnCoords = new ArrayList<Double>();

				xPos = -maxWidth;
				for (int i = 0; i < nodesToPlace.size(); i++)
				{
					if ((i % numCols) == 0)
					{
						xPos += maxWidth;
						maxWidth = 0;
						yPos = 0;
						columnCoords.add(new Double(xPos));
					}
					PlacementNode plNode = nodesToPlace.get(i);
					plNode.setPlacement(xPos, yPos);
					yPos += avgCellHeight;
					maxWidth = Math.max(maxWidth, plNode.getWidth());
				}
				break;
		}

		// for our metric it is desirable that the node are placed around (0,0)
		// so move the center to (0,0)
		double x = 0, y = 0;
		for (PlacementNode node : nodesToPlace) {
			x += node.getPlacementX();
			y += node.getPlacementY();
		}

		double x_m = x / nodesToPlace.size();
		double y_m = y / nodesToPlace.size();

		for (PlacementNode node : nodesToPlace) {
			node.setPlacement(node.getPlacementX() - x_m, node.getPlacementY() - y_m);
		}
	}

	/**
	 * Method that cleans up overlap. Beginning with the node closest to the
	 * origin nodes that overlap with nodes more closer are moved outwards.
	 */
	private void cleanup() {
		if (placementStyleParam.getIntValue() == PLACEMENTSTYLE_ANY) {
			ProxyNode nodes[] = new ProxyNode[nodesToPlace.size()];
			nodesToPlace.toArray(nodes);
			Arrays.sort(nodes);

			// For all nodes, beginning with the one closest to the origin
			for (int i = 0; i < nodes.length; i++) {
				// Get all nodes closer to the origin OR nodes that overlap but are already finalized
				Set<ProxyNode> co = posIndex.getPossibleOverlaps(nodes[i]);
				List<ProxyNode> ct = new ArrayList<ProxyNode>();

				for (ProxyNode node : co)
					if ((node.compareTo(nodes[i]) < 0 && node != nodes[i]) || node.finalized)
						ct.add(node);

				// move the node outwards depending on how big the overlap is until the overlap is gone
				double overlap = 0;
				while ((overlap = metric.overlap(nodes[i], ct)) != 0) {
					double d = Math.sqrt(nodes[i].getPlacementX() * nodes[i].getPlacementX() + nodes[i].getPlacementY()
							* nodes[i].getPlacementY());
					double x = nodes[i].getPlacementX() / d * Math.sqrt(overlap) * 0.1;
					double y = nodes[i].getPlacementY() / d * Math.sqrt(overlap) * 0.1;

					posIndex.move(nodes[i], nodes[i].getPlacementX() + x, nodes[i].getPlacementY() + y);

					// Overlapping with nodes that are finalized is never allowed
					Set<ProxyNode> co2 = posIndex.getPossibleOverlaps(nodes[i]);
					for (ProxyNode node : co2)
						if (node.finalized && ct.contains(node) == false)
							ct.add(node);
				}
				nodes[i].finalized = true;
			}
		} else if (placementStyleParam.getIntValue() == PLACEMENTSTYLE_COLUMNS) {
			// make lists of cells in each column
			Map<Double,List<ProxyNode>> columns = new HashMap<Double,List<ProxyNode>>();
			for(ProxyNode node : nodesToPlace)
			{
				Double xPos = new Double(node.getPlacementX());
				List<ProxyNode> colNodes = columns.get(xPos);
				if (colNodes == null) columns.put(xPos, colNodes = new ArrayList<ProxyNode>());
				colNodes.add(node);
			}

			// now spread the cells in each column so that they do not overlap
			for(Double xPos : columns.keySet())
			{
				List<ProxyNode> colNodes = columns.get(xPos);
				Collections.sort(colNodes, new ProxyNodesByY());

				ProxyNode closestToZero = null;
				for(ProxyNode node : colNodes)
				{
					if (closestToZero == null ||
						Math.abs(node.getPlacementY()) < Math.abs(closestToZero.getPlacementY()))
							closestToZero = node;
				}
				int index = colNodes.indexOf(closestToZero);
				for(int i=index-1; i>=0; i--)
				{
					ProxyNode node = colNodes.get(i);
					ProxyNode above = colNodes.get(i+1);
					posIndex.move(node, node.getPlacementX(), above.getPlacementY() - above.height/2 - node.height/2);
				}
				for(int i=index+1; i<colNodes.size(); i++)
				{
					ProxyNode node = colNodes.get(i);
					ProxyNode below = colNodes.get(i-1);
					posIndex.move(node, node.getPlacementX(), below.getPlacementY() + below.height/2 + node.height/2);
				}

				// shift so that it is zero-centered
				double lY = colNodes.get(0).getPlacementY();
				double hY = colNodes.get(colNodes.size()-1).getPlacementY();
				double offY = (lY + hY) / 2;
				for(ProxyNode node : colNodes)
				{
					posIndex.move(node, node.getPlacementX(), node.getPlacementY() - offY);
					node.finalized = true;
				}
			}
		}
	}

    /**
     * Comparator class for sorting ProxyNodes by their Y position.
     */
    public static class ProxyNodesByY implements Comparator<ProxyNode> {
        /**
         * Method to sort ProxyNodes by their Y position.
         */
        public int compare(ProxyNode n1, ProxyNode n2) {
            double y1 = n1.getPlacementY();
            double y2 = n2.getPlacementY();
            if (y1 < y2) return 1;
            if (y1 > y2) return -1;
            return 0;
        }
    }

	/**
	 * Class that does the actual simulated annealing. All instances of this
	 * class share the same data set. Simulated annealing goes like this:
	 * 
	 * - find a measure of how good a placement is (e.g. the total net length) -
	 * define a "starting temperature" - given a placement, do something that
	 * could change this measure (moving nodes around, rotating them...) - if
	 * the measure is better fine, if not the probability of accepting this
	 * depends on the temperature and on how much the placement is worse than
	 * before. The higher the temperature, the higher the probability of bad
	 * moves being accepted - decrease temperature - repeat until temperature
	 * reaches 0
	 * 
	 * To achieve a good balance of speedup, complexity and overhead we
	 * did...nothing ;-) Okay thats not the truth but we kept it very simple for
	 * now. Every perturbation is evaluated without any locking first and when
	 * it's accepted it is partially evaluated a second time in a synchronized
	 * block. This is because in the meantime the placement and the data used in
	 * the calculations may be outdated, rendering the result useless.
	 * 
	 * The effects of this implementations are: - The less likely it is that a
	 * move is accepted, the better the speedup - The more threads started the
	 * more likely it is moves are conflicting when accepted because they work
	 * with the same data - starting more threads than there are cores is most
	 * likely useless (?)
	 */
	class SimulatedAnnealing extends Thread {
		private static final int MUTATIONSWAP = 1;
		private static final int MUTATIONMOVE = 2;
		private static final int MUTATIONORIENTATE = 3;

		Orientation[] orientations = { Orientation.IDENT, Orientation.R, Orientation.RR, Orientation.RRR,
				Orientation.Y, Orientation.YR, Orientation.YRR, Orientation.YRRR};
		Orientation[] orientationsColumns = { Orientation.IDENT, Orientation.X};
		Orientation[] orientationsRows = { Orientation.IDENT, Orientation.Y};

		Random rand = new Random(); // TODO: thread-aware seed

		/**
		 * Method that finds a random node
		 * 
		 * @return a random node
		 */
		private ProxyNode getRandomNode() {
			return nodesToPlace.get(rand.nextInt(nodesToPlace.size()));
		}

		/**
		 * Method that calculates the net metric for a node in the placement
		 * using node information provided by a dummy. This is typically used
		 * with <original> being a node in the current placement and <dummy>
		 * being a clone of that node with another location or rotation.
		 * @param original a nodes in the current placement
		 * @param dummy a nodes not in the placement that replaces <original> for this calculation
		 * @param lengths the resulting individual net lengths
		 * @return the sum of the net lengths
		 */
		private double metricForDummy(ProxyNode original, ProxyNode dummy, Map<PlacementNetwork, Double> lengths) {
			double sum = 0;

			for (PlacementNetwork net : dummy.getNets()) {
				double length = metric
						.netLength(net, proxyMap, new ProxyNode[] { original }, new ProxyNode[] { dummy });
				lengths.put(dummy.getOriginalNet(net), new Double(length));
				sum += length;
			}

			return sum;
		}

		/**
		 * Method that calculates the net lengths for a placement in which some
		 * of the node are replaced by other nodes. This is typically used with
		 * <originals> being a list of nodes in the current placement and
		 * <dummies> being a list of clones of these nodes with another
		 * locations or rotations.
		 * @param originals a list of nodes in the current placement
		 * @param dummies a list of nodes not in the placement that replace the
		 *            nodes in <originals> for this calculation
		 * @param lengths the resulting individual net lengths
		 * @return the sum of the net lengths
		 */
		private double metricForDummies(ProxyNode[] originals, ProxyNode[] dummies,
				Map<PlacementNetwork, Double> lengths) {
			double sum = 0;

			List<PlacementNetwork> nets = new ArrayList<PlacementNetwork>();

			// create a set of nets connected to one of these nodes
			for (int i = 0; i < originals.length; i++)
				for (PlacementNetwork net : originals[i].getNets())
					if (!nets.contains(net))
						nets.add(net);

			// sum up the net lengths
			for (PlacementNetwork net : nets) {
				double length = metric.netLength(net, proxyMap, originals, dummies);
				lengths.put(net, new Double(length));
				sum += length;
			}

			return sum;
		}

		/**
		 * Method that calculates how much overlap there would be in the current
		 * placement if one node is replaced by another. This is typically used
		 * with <original> being a node in the current placement and <dummy>
		 * being a clone of that node with another location or rotation.
		 * @param original
		 * @param dummy replacement of <original>
		 * @return
		 */
		private double overlapForDummy(ProxyNode original, ProxyNode dummy) {
			// in the list of nodes in the proximity of the dummy,
			// we remove node that the dummy replaced an then calculate the
			Set<ProxyNode> candidates = posIndex.getPossibleOverlaps(dummy);
			while (candidates.remove(original))
				;
			return metric.overlap(dummy, candidates);
		}

		/**
		 * Method that calculates how much overlap there would be in the current
		 * placement if two nodes are replaced by another two nodes.
		 * @param original1
		 * @param original2
		 * @param dummy1 replacement of <original1>
		 * @param dummy2 replacement of <original2>
		 * @return
		 */
		private double overlapForDummy(ProxyNode original1, ProxyNode original2, ProxyNode dummy1, ProxyNode dummy2) {
			double overlap = 0;
			Set<ProxyNode> candidates1 = posIndex.getPossibleOverlaps(dummy1);
			Set<ProxyNode> candidates2 = posIndex.getPossibleOverlaps(dummy2);

			// in the list of nodes in the proximity of dummy1,
			// we remove the nodes that are replaced and add
			// the other dummy
			candidates1.add(dummy2);
			while (candidates1.remove(original1))
				;
			while (candidates1.remove(original2))
				;
			overlap += metric.overlap(dummy1, candidates1);

			// in the list of nodes in the proximity of dummy2,
			// we remove the nodes that are replaced and add
			// the other dummy
			candidates2.add(dummy1);
			while (candidates2.remove(original1))
				;
			while (candidates2.remove(original2))
				;
			overlap += metric.overlap(dummy2, candidates2);

			return overlap;
		}

		public void run() {
			// Thread wont stop until temperature is below 1
			while (temperature > 1) {
				int acceptCount = 0;
				int conflictCount = 0;

				// Try some perturbations before checking if temperature has to be lowered
				for (int i = 0; i < stepsPerUpdate; i++) {
					ProxyNode node1 = null;
					ProxyNode node2 = null;
					ProxyNode dummy1 = null;
					ProxyNode dummy2 = null;

					double networkMetricBefore = 0, networkMetricAfter = 0;
					double overlapMetricBefore = 0, overlapMetricAfter = 0;
					double areaMetricBefore = 0, areaMetricAfter = 0;
					double manhattanRadiusBefore = 0, manhattanRadiusAfter = 0;

					Map<PlacementNetwork, Double> newNetLengths = new HashMap<PlacementNetwork, Double>();

					int mutationType = randomPerturbationType();
					AreaSnapshot area = posIndex.area;

					areaMetricBefore = area.getArea();

					// given a perturbation type, find nodes to apply the perturbation to
					// and calculate the cost function for the altered layout
					switch (mutationType) {

					case MUTATIONSWAP:		// Find and swap two nodes
						node1 = getRandomNode();
						while ((node2 = getRandomNode()) == node1)
							;

						networkMetricBefore = metricForNodes(node1, node2);
						// TODO Overlap of 1 and 2 is counted twice
						overlapMetricBefore = metric.overlap(node1, posIndex.getPossibleOverlaps(node1))
								+ metric.overlap(node2, posIndex.getPossibleOverlaps(node2));

						// Create two dummies of the nodes that have the same size
						// Move the clone of node1 to the position of node2 and vice versa
						dummy1 = node1.clone();
						dummy2 = node2.clone();
						dummy1.setPlacement(node2.getPlacementX(), node2.getPlacementY());
						dummy2.setPlacement(node1.getPlacementX(), node1.getPlacementY());

						// calculate the metrics for a layout where
						// the two nodes are replaced by the dummies
						networkMetricAfter = metricForDummies(new ProxyNode[] { node1, node2 }, new ProxyNode[] {
								dummy1, dummy2 }, newNetLengths);
						overlapMetricAfter = overlapForDummy(node1, node2, dummy1, dummy2);
						areaMetricAfter = area.areaForDummy(node1, node2, dummy1, dummy2);
						break;

					case MUTATIONMOVE:

						node1 = getRandomNode();

						networkMetricBefore = metricForNode(node1);
						overlapMetricBefore = metric.overlap(node1, posIndex.getPossibleOverlaps(node1));
						manhattanRadiusBefore = Math.max(Math.abs(node1.getPlacementX()), Math.abs(node1.getPlacementY()));

						// add a random translation vector to the nodes location
						// but don't move it outside the "working area"
						Point2D position_t = randomMove(node1);
						double new_x = (node1.getPlacementX() + position_t.getX()
							* (0.1 + 0.1 * temperature / startingTemperature)) % maxChipLength;
						double new_y = (node1.getPlacementY() + position_t.getY()
							* (0.1 + 0.1 * temperature / startingTemperature)) % maxChipLength;
						switch (placementStyleParam.getIntValue())
						{
							case PLACEMENTSTYLE_ROWS:
								break;
							case PLACEMENTSTYLE_COLUMNS:
								int r1 = rand.nextInt(columnCoords.size());
								int r2 = rand.nextInt(columnCoords.size());
								if (r1 != r2)
								{
									double xCur = node1.getPlacementX();
									double xR1 = columnCoords.get(r1).doubleValue();
									double xR2 = columnCoords.get(r2).doubleValue();
									double curHeight = 0, alt1Height = 0, alt2Height = 0;
									for(ProxyNode n : nodesToPlace)
									{
										if (n.getPlacementX() == xCur) curHeight += n.height;
										if (n.getPlacementX() == xR1) alt1Height += n.height;
										if (n.getPlacementX() == xR2) alt2Height += n.height;
									}
									double diff1 = Math.abs(avgRowHeight - (alt1Height + (xCur == xR1 ? 0 : node1.height)));
									double diff2 = Math.abs(avgRowHeight - (alt2Height + (xCur == xR2 ? 0 : node1.height)));
									if (diff2 < diff1) r1 = r2;
								}
								new_x = columnCoords.get(r1).doubleValue();
								break;
						}

						dummy1 = node1.clone();
						dummy1.setPlacement(new_x, new_y);

						// calculate the metrics for a layout where
						// the node is moved to another location
						networkMetricAfter = metricForDummy(node1, dummy1, newNetLengths);
						overlapMetricAfter = overlapForDummy(node1, dummy1);
						areaMetricAfter = area.areaForDummy(node1, dummy1);
						manhattanRadiusAfter = Math.max(Math.abs(dummy1.getPlacementX()), Math.abs(dummy1.getPlacementY()));
						break;

					case MUTATIONORIENTATE:

						node1 = getRandomNode();
						networkMetricBefore = metricForNode(node1);
						overlapMetricBefore = metric.overlap(node1, posIndex.getPossibleOverlaps(node1));

						dummy1 = node1.clone();
						switch (placementStyleParam.getIntValue())
						{
							case PLACEMENTSTYLE_ANY:
								dummy1.setPlacementOrientation(orientations[rand.nextInt(orientations.length)], true);
								break;
							case PLACEMENTSTYLE_COLUMNS:
								dummy1.setPlacementOrientation(orientationsColumns[rand.nextInt(orientationsColumns.length)], true);
								break;
							case PLACEMENTSTYLE_ROWS:
								dummy1.setPlacementOrientation(orientationsRows[rand.nextInt(orientationsRows.length)], true);
								break;
						}

						// calculate the metrics for a layout where the node is rotated
						networkMetricAfter = metricForDummy(node1, dummy1, newNetLengths);
						overlapMetricAfter = overlapForDummy(node1, dummy1);
						areaMetricAfter = area.areaForDummy(node1, dummy1);
					}

					// TODO area metric is obsolete because of the Manhattan geometry metric
					// which give much more compact placements
					double networkGain = networkMetricBefore - networkMetricAfter;
					double overlapGain = overlapMetricBefore - overlapMetricAfter;
					double areaGain = (areaMetricBefore - areaMetricAfter) / totalArea;
					double manhattanRadiusGain = manhattanRadiusBefore - manhattanRadiusAfter;

					double gain = networkGain + overlapGain * OVERLAP_WEIGHT + areaGain * AREA_WEIGHT
							+ manhattanRadiusGain * MANHATTAN_WEIGHT;

					// the worse the gain of a perturbation, the lower the probability that this perturbation
					// is actually applied (positive gains are always accepted)
					if (Math.exp(gain / temperature) >= Math.random()) {
						acceptCount++;

						// Before we actually apply the perturbation, we have to check if the overlap
						// has changed. This is because we haven't locked the area we are occupying
						// and another thread could have placed a node that would now overlap.
						// The same goes for nets that could also be altered by another thread
						//
						// Note:
						// There is still a logical race condition because it is possible that
						// two moves are already accepted but after the first one has been written,
						// the second one would most likely be rejected. It is just not very likely
						// and with thousands of moves per seconds no big deal
						//
						// Performance:
						// The overlap has to be calculated twice (nets are hashed values).
						// The synchronized block is quite coarse as everything is locked.
						// It could be replaced by a smaller synchronized block that only
						// checks and locks altered parts of the placement (target areas, nets)
						synchronized (posIndex) {
							switch (mutationType) {
							case MUTATIONMOVE:
								if (overlapForDummy(node1, dummy1) == overlapMetricAfter
										&& metricForNode(node1) == networkMetricBefore) {
									posIndex.move(node1, dummy1.getPlacementX(), dummy1.getPlacementY());
									for (PlacementNetwork net : newNetLengths.keySet())
										netLengths.put(net, newNetLengths.get(net));
								} else
									conflictCount++;
								break;

							case MUTATIONSWAP:
								if (overlapForDummy(node1, node2, dummy1, dummy2) == overlapMetricAfter
										&& metricForNodes(node1, node2) == networkMetricBefore) {
									posIndex.swap(node1, node2);
									for (PlacementNetwork net : newNetLengths.keySet())
										netLengths.put(net, newNetLengths.get(net));
								} else
									conflictCount++;
								break;

							case MUTATIONORIENTATE:
								if (overlapForDummy(node1, dummy1) == overlapMetricAfter
										&& metricForNode(node1) == networkMetricBefore) {
									posIndex.rotate(node1, dummy1.getPlacementOrientation());
									for (PlacementNetwork net : newNetLengths.keySet())
										netLengths.put(net, newNetLengths.get(net));
								} else
									conflictCount++;
							}
						}
					}
				}

				update(stepsPerUpdate, acceptCount, conflictCount);
			}
		}

		/**
		 * Method that generates a random translation vector for a given node
		 * @param node
		 * @return a translation vector
		 */
		private Point2D randomMove(ProxyNode node) {
			double maxBoundingBox = 1;

			// the length of the vector is a function of the maximum bounding box
			// the further away connected nodes are, the further the node may move
			List<PlacementNetwork> nets = node.getNets();
			if (nets.size() > 0) {
				for (PlacementNetwork net : nets) {
					double metric = netLengths.get(net).doubleValue();
					if (metric > maxBoundingBox)
						maxBoundingBox = metric;
				}
			} else {
				maxBoundingBox = maxChipLength;
			}

			// shorter vectors are more likely
			// don't move nodes too far away if they are already good positioned
			double offsetX = Math.min(Math.max(rand.nextGaussian(), -1), 1) * maxBoundingBox;
			double offsetY = Math.min(Math.max(rand.nextGaussian(), -1), 1) * maxBoundingBox;

			return new Point2D.Double(offsetX, offsetY);
		}

		/**
		 * Returns a random perturbation type.
		 * @return
		 *   MUTATIONSWAP to swap two nodes (10% probability);
		 *   MUTATIONMOVE to move a node (80% probability);
		 *   MUTATIONORIENTATE to rotate a node (10% probability);
		 */
		//STARTING LENGTH=560366, ENDING LENGTH=175334	10% swap, 90% move
		//STARTING LENGTH=560366, ENDING LENGTH=179628	50% swap, 50% move
		//STARTING LENGTH=560366, ENDING LENGTH=168816	90% swap, 10% move
		private int randomPerturbationType() {
			if (placementStyleParam.getIntValue() == PLACEMENTSTYLE_ANY)
			{
				int r = rand.nextInt(100);
				if (r < 10) return MUTATIONSWAP;
				if (r < 90) return MUTATIONMOVE;
				return MUTATIONORIENTATE;
			} else
			{
				int r = rand.nextInt(100);
				if (r < 10) return MUTATIONMOVE;
				return MUTATIONSWAP;
			}
		}

		/**
		 * Method that estimates the net lengths of all nets connected to two nodes
		 * @param n1
		 * @param n2
		 * @return
		 */
		private double metricForNodes(ProxyNode n1, ProxyNode n2) {
			// TODO Change so that it matches the method used in metricForDummies
			// swaps check if the net has changed by comparing two doubles that
			// are calculated in different ways for the same data
			double metric = metricForNode(n1) + metricForNode(n2);
			for (PlacementNetwork p : n1.getNets())
				if (n2.getNets().contains(p))
					metric -= netLengths.get(p).doubleValue();

			return metric;
		}

		/**
		 * Method that estimates the net lengths of all nets connected to a given node
		 * @param node the node to evaluate
		 * @return a value for the current position. The lower the value, the better.
		 */
		private double metricForNode(ProxyNode node) {
			double metric = 0;
			for (PlacementNetwork net : node.getNets())
				metric += netLengths.get(net).doubleValue();

			return metric;
		}
	}
}

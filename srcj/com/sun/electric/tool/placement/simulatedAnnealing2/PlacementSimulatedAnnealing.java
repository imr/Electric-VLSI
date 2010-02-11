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
package com.sun.electric.tool.placement.simulatedAnnealing2;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.tool.placement.PlacementFrame;
import com.sun.electric.tool.placement.simulatedAnnealing2.PositionIndex.AreaSnapshot;

import java.awt.geom.Point2D;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *  Implementation of the simulated annealing placement algorithm
 */
public class PlacementSimulatedAnnealing extends PlacementFrame
{
	// Benchmarking properties
	String teamName = "Team 6";
	String studentName1 = "Sebastian";
	String studentName2 = "Jochen";
	String algorithmType = "simulated annealing";
	
	public int numThreads = 2;
	public int maxRuntime = 0; // in seconds, 0 means no time limit

	public boolean printDebugInformation = false;
	
	// Temperature control
	private int iterationsPerTemperatureStep;	// if maximum runtime is > 0 this is calculated each temperature step
	private double startingTemperature = 0;
	private double temperature;
	private int temperatureSteps = 0;				// how often will the temperature be decreased
	private int temperatureStep;

	private int perturbations_tried;
	private long stepStartTime;
	private long timestampStart = 0;
	private double maxChipLength = 0;
	
	private double minArea = 0;
	
	
	private ArrayList<ProxyNode> nodesToPlace;
	private List<PlacementNetwork> allNetworks;
	private BoundingBoxMetric metric = null;
	private Map<PlacementNode, ProxyNode> proxyMap;
	private Map<PlacementNetwork, Double> netLengths = null;
	private PositionIndex posIndex;
	
	
	// Debug and performance
	private final boolean performance_log = false;
	private final String  performance_log_filename = "placement.log";

	private int accepts = 0;	// moves accepted
	private int conflicts = 0;  // moves that were accepted but not made because of interference from other threads
	
	double[] lengthLog 		= new double[1000000]; // TODO: write a class that collects performance data
	double[] overlapLog 	= new double[1000000]; // TODO: allocate with appropriate size (temperatureSteps)
	double[] timestampLog   = new double[1000000];
	double[] temperatureLog = new double[1000000];
	double[] areaLog 		= new double[1000000];
	double[] stepsizeLog 	= new double[1000000];
	double[] acceptLog 		= new double[1000000];
	double[] conflictLog	= new double[1000000];
	double[] stepdurationLog= new double[1000000];
	int stepsPerUpdate = 50;
	
	// Tweaking Parameter
	private final double OVERLAP_WEIGHT = 100;
	private final double MANHATTAN_WEIGHT = 0.1;
	private final double AREA_WEIGHT = 10000;
	
	
	/**
	 * Method that creates a map that hashes a node to its proxy node
	 * This is mainly for working with the net topology because nodes belonging to
	 * a net are of type <Node> not its <ProxyNode>
	 * @param nodesToPlace a list of proxy nodes
	 * @return the map that maps a node to its proxy
	 */
	private HashMap<PlacementNode, ProxyNode> createProxyHashmap( List<ProxyNode>nodesToPlace ) {
		HashMap<PlacementNode, ProxyNode> proxyMap = new HashMap<PlacementNode, ProxyNode>();
		
		for(ProxyNode p : nodesToPlace) {
			proxyMap.put( p.getNode(), p );
		}
		return proxyMap;
	}

	/**
	 * Method to return the name of this placement algorithm.
	 * @return the name of this placement algorithm.
	 */
	public String getAlgorithmName() { return "Simulated-Annealing-2"; }

	/**
	 * Method that counts how often the temperature will be decreased before going below 1
	 */
	private int countTemperatureSteps(double startingTemperature) {
		double temperature = startingTemperature;
		int steps = 0;
		
		while(temperature > 1) {
			steps++;
			temperature = coolDown(temperature);
		}
		
		return steps;
	}
	
	/**
	 * Method to do placement by simulated annealing.
	 * @param nodesToPlace a list of all nodes that are to be placed.
	 * @param allNetworks a list of all networks that connect the nodes.
	 * @param cellName the name of the cell being placed.
	 */
	public void runPlacement( List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks, String cellName )
	{
		timestampStart = System.currentTimeMillis();

		this.allNetworks = allNetworks;
		metric = new BoundingBoxMetric();
		temperatureStep = 0;
		iterationsPerTemperatureStep = maxRuntime > 0 ? 100 : getDesiredMoves( nodesToPlace );
		perturbations_tried = 0;
		accepts = 0;
		conflicts = 0;
		minArea = 0;
		
		ArrayList<PlacementNetwork> ignoredNets = new ArrayList<PlacementNetwork>();
		for(PlacementNetwork net: allNetworks)
			if(net.getPortsOnNet().size() >= nodesToPlace.size() * 0.4 && net.getPortsOnNet().size() > 100)
				ignoredNets.add(net);
		
		// create an initial layout to be improved by simulated annealing
		initLayout(nodesToPlace);

		netLengths = new HashMap<PlacementNetwork, Double>();
		
		// create proxies for placement nodes
		this.nodesToPlace = new ArrayList<ProxyNode> ( nodesToPlace.size() );
		for(PlacementNode p : nodesToPlace) {
			ProxyNode proxy = new ProxyNode(p, ignoredNets);
			this.nodesToPlace.add( proxy );
		}
		proxyMap = createProxyHashmap(this.nodesToPlace);
		
		// Sum up the total node area. This is used as reference value for the area metric
		for ( ProxyNode node : this.nodesToPlace ) {
			minArea += node.width * node.height;
		}
				
		// Calculate the working area for the process (maximum chip area)
		// nodes will not be placed outside the working area
		maxChipLength = getMaxChipLength(nodesToPlace);
		posIndex = new PositionIndex( maxChipLength, this.nodesToPlace );
		
		// calculate the starting temperature
		startingTemperature = getStartingTemperature(nodesToPlace, maxChipLength, maxRuntime);
		temperature = startingTemperature;
		temperatureSteps = countTemperatureSteps(startingTemperature);

		for ( ProxyNode p : this.nodesToPlace )
			p.apply();

		// precalculate an hash net lengths
		for(PlacementNetwork net : allNetworks)
			netLengths.put(net, new Double(metric.netLength( net, proxyMap )));
			
		stepStartTime = System.nanoTime();
		
		SimulatedAnnealing[] threads = new SimulatedAnnealing[numThreads];

		for(int n = 0; n < numThreads; n++)
		{
			threads[n] = new SimulatedAnnealing();
			threads[n].start();
		}

		for (int i = 0; i < numThreads; i++) {
			try {
				threads[i].join();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// cleanup overlap
		cleanup();
		
		// Apply the placement of the proxies to the actual nodes
		for(ProxyNode p : this.nodesToPlace)
			p.apply();


		if(performance_log) {
			writeLog(performance_log_filename);
		}
	}

	/**
	 * Method to calculate the initial temperature.
	 * It uses the standard deviation of the net length
	 * for random placements to derive the starting temperature
	 * @param length maximum length of the chip
	 * @return the initial temperature
	 */
	private double getStartingTemperature(List<PlacementNode> nodes, double length, double runtime)
	{
		double[] metrics = new double[1000];	// sample size
		double sigma = 0;
		double average = 0;
		Random r = new Random();
		
		double width = length;
		double height = length;

		// collect samples of the cost function
		for(int i = 0; i < metrics.length; i++)
		{
			for(PlacementNode p : nodes)	{
				p.setPlacement(r.nextDouble() * width, r.nextDouble() * height);
			}
			
			metrics[i] = metric.netLength(allNetworks);
		}
		
		// calculate average
		for(int i = 0; i < metrics.length; i++)
		{
			average += metrics[i];
		}
		average /= metrics.length;
		
		// calculate standard deviation
		for(int i = 0; i < metrics.length; i++)
		{
			sigma += (metrics[i] - average) * (metrics[i] - average);
		}
		sigma = Math.sqrt(sigma / metrics.length);
		
		// set starting temperature so that the acceptance function
		// accepts worsening of -sigma with a probability of 0.3
		// e^(-sigma /startingTemperature) = 0.3 # solve for startingTemperature
		// TODO THIS IS TWEAKING DATA
		double startingTemperature = sigma * (-1 / (2 *Math.log(0.3)));
		
		// If a maximum runtime is set, only the last temperature steps are done
		if ( maxRuntime > 0 ) {		
			double msPerMove = guessMillisecondsPerMove();
			int desired_moves = getDesiredMoves(nodes);
			int possibleSteps = Math.max((int)((runtime * 1000) / (msPerMove * desired_moves)), 5);
			int stepsUntilStop = countTemperatureSteps(startingTemperature);
			
			for(int i = 0; i < stepsUntilStop - possibleSteps; i++)
				startingTemperature = coolDown(startingTemperature);
		}
		
		return startingTemperature;
	}

	/**
	 * Method that calculates how many moves per temperature step are necessary 
	 * @param nodes
	 * @return
	 */
	private int getDesiredMoves(List<PlacementNode> nodes) {
		// TODO THIS IS TWEAKING DATA
		return Math.max((int)(nodes.size() * Math.sqrt(nodes.size())), 8719);
	}
	
	/**
	 * Method that estimates how many moves can be evaluated with the current settings
	 * @return
	 */
	private double guessMillisecondsPerMove()
	{
		double time = System.currentTimeMillis();
		double sampleSize = 20000;
		Thread gatherers[] = new Thread[numThreads];
		
		// start the threads
		for(int i = 0; i < numThreads; i++)
		{
			gatherers[i] = new SampleGatherer((int)(sampleSize / numThreads));
			gatherers[i].start();
		}
		
		// wait until they have finished
		for(int i = 0; i < numThreads; i++)
		{
			try {
				gatherers[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// A move actually takes more time than we measured
		return (System.currentTimeMillis() - time) * 2 / sampleSize;
	}
	
	/**
	 * This class is used to guess how many moves per
	 * second can be calculated
	 * 
	 * @author Basti
	 *
	 */
	class SampleGatherer extends Thread {
		int samplesCount = 0;
		
		public SampleGatherer(int samplesCount)
		{
			this.samplesCount = samplesCount;
		}
		
		public void run()
		{
			Random r = new Random();
			
			for(int i = 0; i < samplesCount; i++)
			{
				ProxyNode proxy = nodesToPlace.get(r.nextInt(nodesToPlace.size()));
				metric.netLength(proxy.getNets());
			}
		}
	}
			
	/**
	 * Method to calculate the cooling of the simulated annealing process
	 * @param temp the current temperature
	 * @return the lowered temperature
	 */
	private double coolDown(double temp)
	{
		//if(temp < 100) return temp * 0.95 - 0.1;
		return temp * 0.99 - 0.1;
	}

	/**
	 * Synchronized method that does temperature and time control. Threads should call this periodically
	 * @param tries how many perturbation the thread tried since last calling update
	 * @param acceptCount how many of the perturbations that thread tried since last 
	 *        calling update were accepted
	 * @param conflictCount how many of the perturbations that were accepted since last calling
	 *        update were dropped due to conflicts with other threads
	 */
	public synchronized void update( int tries, int acceptCount, int conflictCount )
	{
		this.perturbations_tried += tries;
		this.accepts += acceptCount;
		this.conflicts += conflictCount;
		
		// decrease temperature if enough iterations were done
		if(this.perturbations_tried >= iterationsPerTemperatureStep)
		{
			long stepDuration = System.nanoTime() - stepStartTime;
			stepStartTime = System.nanoTime();
			
			// this will hopefully be useful when optimizing
			if(performance_log)
			{
				lengthLog[temperatureStep] 		= metric.netLength( allNetworks, proxyMap);
				//overlapLog[temperatureStep] 	= metric.overlap(nodesToPlace);
				timestampLog[temperatureStep] 	= System.currentTimeMillis() - timestampStart;
				temperatureLog[temperatureStep] = temperature;
				areaLog[temperatureStep] 		= posIndex.area.getArea() / minArea;
				stepsizeLog[temperatureStep] 	= iterationsPerTemperatureStep;
				acceptLog[temperatureStep]		= this.accepts;
				conflictLog[temperatureStep]	= this.conflicts;
				stepdurationLog[temperatureStep]= stepDuration;
			}
			
			// adjust how many moves to try before the next temperature decrease
			if ( maxRuntime > 0 )
			{
				// use the observed time from the last step to calculate
				// how many move to do
				long elapsedTime = System.currentTimeMillis() - timestampStart;
				long remainingTime = maxRuntime * 1000 - elapsedTime;
				long remainingSteps = temperatureSteps - temperatureStep;
				double allowedTimePerStep = 1e6 * ((double)remainingTime / remainingSteps);

				iterationsPerTemperatureStep *= allowedTimePerStep/stepDuration;
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
	private void initLayout( List<PlacementNode> nodesToPlace )
	{
		// TODO replace initial random node placement if useful
		int SPACING = 20;
		
		int numRows = (int)Math.round(Math.sqrt(nodesToPlace.size()));
		double xPos = 0, yPos = 0;
		double maxHeight = 0;
		
		// place the nodes in sqrt(n) rows * sqrt(n) nodes with no overlaps
		for(int i=0; i<nodesToPlace.size(); i++)
		{
			PlacementNode plNode = nodesToPlace.get(i);
			xPos += plNode.getWidth() / 2;
			plNode.setPlacement(xPos, yPos + plNode.getHeight() / 2);
			xPos += plNode.getWidth() / 2 + SPACING;
			maxHeight = Math.max(maxHeight, plNode.getHeight());
			if ((i%numRows) == numRows-1)
			{
				yPos += maxHeight + SPACING;
				maxHeight = 0;
				xPos = 0;
			}
		}
		
		// for our metric it is desirable that the node are placed around (0,0)
		// so move the center to (0,0)
		double x = 0, y = 0;
		for(PlacementNode node : nodesToPlace)
		{
			x += node.getPlacementX();
			y += node.getPlacementY();
		}
		
		double x_m = x / nodesToPlace.size();
		double y_m = y / nodesToPlace.size();
		
		for(PlacementNode node : nodesToPlace)
		{
			node.setPlacement(node.getPlacementX() - x_m,
			                  node.getPlacementY() - y_m);
		}
		
	}
	
	/**
	 * Method that cleans up overlap. Beginning with the node closes to the origin nodes that overlap
	 * with nodes mor closer are moved outwards.
	 */
	private void cleanup()
	{
		ProxyNode nodes[] = new ProxyNode[nodesToPlace.size()];
		nodesToPlace.toArray(nodes);
		Arrays.sort(nodes);
		
		// For all nodes, beginning with the one closes to the origin
		for(int i = 0; i < nodes.length; i++)
		{
			// Get all nodes closer to the origin OR nodes that overlap but are already finalized
			List<ProxyNode> co = posIndex.getPossibleOverlaps(nodes[i]);
			List<ProxyNode> ct = new ArrayList<ProxyNode>();
			
			for(ProxyNode node : co)
				if((node.compareTo(nodes[i]) < 0 && node != nodes[i]) || node.finalized)
					ct.add(node);
			
			// move the node outwards depending on how big the overlap is until the overlap is gone
			double overlap = 0;
			while((overlap = metric.overlap(nodes[i], ct)) != 0)
			{
				double d = Math.sqrt(nodes[i].getPlacementX() * nodes[i].getPlacementX() + nodes[i].getPlacementY() * nodes[i].getPlacementY());
				double x = nodes[i].getPlacementX() / d * Math.sqrt(overlap) * 0.1;
				double y = nodes[i].getPlacementY() / d * Math.sqrt(overlap) * 0.1;

				posIndex.move(nodes[i], nodes[i].getPlacementX() + x , nodes[i].getPlacementY() + y);
				
				// Overlapping with nodes that are finalized is never allowed
				List<ProxyNode> co2 = posIndex.getPossibleOverlaps(nodes[i]);
				for(ProxyNode node : co2)
					if(node.finalized && ct.contains(node) == false)
						ct.add(node);
				
			}
			nodes[i].finalized = true;
		}	
	}
	

	/**
	 * Method that calculates the maximum working area
	 * This is because there is no use in moving nodes
	 * "miles" away ...
	 * @param nodesToPlace
	 * @return
	 */
	private double getMaxChipLength(List<PlacementNode> nodesToPlace)
	{
		double totalArea = 0;
		
		for(PlacementNode p : nodesToPlace)	{
			totalArea += p.getHeight() * p.getWidth();
		}
		
		return Math.sqrt(totalArea) * 4;
	}
	
	/**
	 * writes collected performance data
	 * @param filename
	 */
	private void writeLog(String filename)
	{
		try {
			FileWriter f = new FileWriter(filename);

			for(int i = 0; i < temperatureStep; i++)
				f.append( (long)timestampLog[i] + ","
						+ (int)lengthLog[i] + ","
						+ (int)temperatureLog[i] + ","
						+ (int)overlapLog[i] + ","
						+ areaLog[i] + ","
						+ stepsizeLog[i] + ","
						+ acceptLog[i] + ","
						+ conflictLog[i] + ","
						+ stepdurationLog[i] +"\n");
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Class that does the actual simulated annealing
	 * All instances of this class share the same data set
	 * in short simulated annealing goes like this:
	 * 
	 * - find a measure of how good a placement is (eg. the total net length)
	 * - define a "starting temperature"
	 * - given a placement, do something that could change this measure
	 *   (moving nodes around, rotating them...)
	 * - if the measure is better fine, if not the probability of accepting this
	 *   depends on the temperature and on how much the placement is worse than before.
	 *   the higher the temperature the higher the probability of bad moves beeing accepted
	 * - decrease temperature
	 * - repeat until temperature reaches 0
	 *  
	 * To achieve a good balance of speedup, complexity and overhead we did...nothing ;-)
	 * Okay thats not the truth but we kept it very simple for now.
	 * Every perturbation is evaluated without any locking first and when its accepted
	 * its partially evaluated a second time in a synchronized block.
	 * This is because in the meantime the placement and the data used in the calculations
	 * may be outdated rendering the result useless
	 * 
	 * The effects of this implementations are:
	 * - The less likely it is that a move is accepted, the better the speedup
	 * - The more threads started the more likely it is moves are conflicting when accepted
	 *   because they work with the same data
	 * - starting more threads that there are cores is most likely useless (?)
	 * 
	 * @author Basti
	 *
	 */
	class SimulatedAnnealing extends Thread
	{
		private static final int MUTATIONSWAP = 1;
		private static final int MUTATIONMOVE = 2;
		private static final int MUTATIONORIENTATE = 3;

		Orientation[] orientations = { 
				Orientation.IDENT, Orientation.R, Orientation.RR, Orientation.RRR,
				Orientation.X, Orientation.XR, Orientation.XRR, Orientation.XRRR,
				Orientation.Y, Orientation.YR, Orientation.YRR, Orientation.YRRR,
				Orientation.XY, Orientation.XYR, Orientation.XYRR, Orientation.XYRRR };

		Random rand = new Random(); // TODO: thread-aware seed

		/**
		 * Method that finds a random node
		 * @return a random node
		 */
		private ProxyNode getRandomNode()
		{
			return nodesToPlace.get( rand.nextInt( nodesToPlace.size() ) );
		}
		
		/**
		 * Method that calculates the net metric for a node in the placement using node
		 * information provided by a dummy
		 * This is typically used with <original> being a node in the current placement
		 * and <dummy> being a clone of that node with another location or rotation
		 * @param original a nodes in the current placement
		 * @param dummy a nodes not in the placement that replaces <original> for this calculation
		 * @param lengths the resulting individual net lengths
		 * @return the sum of the net lengths
		 */
		private double metricForDummy(ProxyNode original, ProxyNode dummy, HashMap<PlacementNetwork, Double>lengths)
		{
			double sum = 0;
			
			for(PlacementNetwork net : dummy.getNets())
			{
				double length = metric.netLength( net, proxyMap, new ProxyNode[] {original}, new ProxyNode[] {dummy});
				lengths.put(dummy.getOriginalNet(net), new Double(length));
				sum += length;
			}
				
			return sum;
		}
		
		/**
		 * Method that calculates the net lengths for a placement in which some of the
		 * node are replaced by other nodes.
		 * This is typically used with <originals> being a list of nodes in the current placement
		 * and <dummies> being a list of clones of these nodes with another locations or rotations
		 * @param originals a list of nodes in the current placement
		 * @param dummies a list of nodes not in the placement that replace the nodes in <originals> for this calculation
		 * @param lengths the resulting individual net lengths
		 * @return the sum of the net lengths
		 */
		private double metricForDummies(ProxyNode[] originals, ProxyNode[] dummies, HashMap<PlacementNetwork, Double>lengths)
		{
			double sum = 0;
			
			ArrayList<PlacementNetwork> nets = new ArrayList<PlacementNetwork>();
			
			// create a set of nets connected to one of these nodes
			for(int i = 0; i < originals.length; i++)
				for(PlacementNetwork net : originals[i].getNets())
					if(!nets.contains(net))
						nets.add(net);
			
			// sum up the net lengths
			for(PlacementNetwork net : nets) {
				double length = metric.netLength(net, proxyMap, originals, dummies);
				lengths.put(net, new Double(length));
				sum += length;
			}
							
			return sum;
		}
		
		/**
		 * Method that calculates how much overlap there would be in the current placement
		 * if one node is replaced by another
		 * This is typically used with <original> being a node in the current placement
		 * and <dummy> being a clone of that node with another location or rotation
		 * @param original
		 * @param dummy replacement of <original>
		 * @return
		 */
		private double overlapForDummy(ProxyNode original, ProxyNode dummy)
		{
			// in the list of nodes in the proximity of the dummy,
			// we remove node that the dummy replaced an then calculate the
			List<ProxyNode> candidates = posIndex.getPossibleOverlaps( dummy );
			while(candidates.remove(original));
			return metric.overlap(dummy, candidates );
		}
		
		/**
		 * Method that calculates how much overlap there would be in the current placement
		 * if two nodes are replaced by another two nodes
		 * @param original1
		 * @param original2
		 * @param dummy1 replacement of <original1>
		 * @param dummy2 replacement of <original2>
		 * @return
		 */
		private double overlapForDummy(ProxyNode original1, ProxyNode original2, ProxyNode dummy1, ProxyNode dummy2)
		{
			double overlap = 0;
			List<ProxyNode> candidates1 = null;
			List<ProxyNode> candidates2 = null;
			
			candidates1 = posIndex.getPossibleOverlaps( dummy1 );
			candidates2 = posIndex.getPossibleOverlaps( dummy2 );
			
			// in the list of nodes in the proximity of dummy1,
			// we remove the nodes that are replaced and add
			// the other dummy
			candidates1.add( dummy2 );
			while(candidates1.remove(original1));
			while(candidates1.remove(original2));
			overlap += metric.overlap( dummy1, candidates1);
			
			// in the list of nodes in the proximity of dummy2,
			// we remove the nodes that are replaced and add
			// the other dummy
			candidates2.add( dummy1 );
			while(candidates2.remove(original1));
			while(candidates2.remove(original2));
			overlap += metric.overlap( dummy2, candidates2);
			
			return overlap;			
		}
		
		public void run()
		{
			// Thread wont stop until temperature is below 1
			while(temperature > 1)
			{
				int acceptCount = 0;
				int conflictCount = 0;
				
				// Try some perturbations before checking if temperature has to be lowered
				for ( int i = 0; i < stepsPerUpdate; i++ )
				{
					ProxyNode node1 = null;
					ProxyNode node2 = null;
					ProxyNode dummy1 = null;
					ProxyNode dummy2 = null;
					
					double networkMetricBefore   = 0, networkMetricAfter   = 0;
					double overlapMetricBefore   = 0, overlapMetricAfter   = 0;
					double areaMetricBefore      = 0, areaMetricAfter      = 0;
					double manhattanRadiusBefore = 0, manhattanRadiusAfter = 0;
					
					HashMap<PlacementNetwork, Double> newNetLengths = new HashMap<PlacementNetwork, Double>();

					int mutationType = randomPerturbationType();
					AreaSnapshot area = posIndex.area;
					
					areaMetricBefore    = area.getArea();
					
					// given a perturbation type, find nodes to apply the perturbation to and
					// calculate the cost function for the altered layout
					switch(mutationType)
					{
						// Find and swap two nodes
						case MUTATIONSWAP:
							node1 = getRandomNode();
							while((node2 = getRandomNode()) == node1);

							networkMetricBefore = metricForNodes( node1, node2 );
							overlapMetricBefore = metric.overlap(node1, posIndex.getPossibleOverlaps( node1 ) ) + metric.overlap( node2, posIndex.getPossibleOverlaps( node2 ) ); // TODO Overlap of 1 and 2 is counted twice
							
							// Create two dummies of the nodes that have the same size
							// Move the clone of node1 to the position of node2 and vice versa
							dummy1 = node1.clone();
							dummy2 = node2.clone();
							dummy1.setPlacement(node2.getPlacementX(), node2.getPlacementY());
							dummy2.setPlacement(node1.getPlacementX(), node1.getPlacementY());
	
							// calculate the metrics for a layout where
							// the two nodes are replaced by the dummies
							networkMetricAfter = metricForDummies ( new ProxyNode[] { node1, node2 }, new ProxyNode[] { dummy1, dummy2 }, newNetLengths);
							overlapMetricAfter = overlapForDummy(node1, node2, dummy1, dummy2);
							areaMetricAfter    = area.areaForDummy(node1, node2, dummy1, dummy2);
							break;
					
						case MUTATIONMOVE:
							
							node1 = getRandomNode();
	
							networkMetricBefore = metricForNode( node1 );
							overlapMetricBefore = metric.overlap(node1, posIndex.getPossibleOverlaps( node1 ) );
							manhattanRadiusBefore = Math.max( Math.abs(node1.getPlacementX() ), Math.abs( node1.getPlacementY() ) );
	
							// add a random translation vector to the nodes location
							// but don't move it outside the "working area"
							Point2D position_t = randomMove( node1 );
							double new_x = (node1.getPlacementX() + position_t.getX() * (0.1 + 0.1 * temperature / startingTemperature)) % maxChipLength;
							double new_y = (node1.getPlacementY() + position_t.getY() * (0.1 + 0.1 * temperature / startingTemperature)) % maxChipLength;

							dummy1 = node1.clone();
							dummy1.setPlacement(new_x, new_y);
							
							// calculate the metrics for a layout where
							// the node is moved to another location
							networkMetricAfter = metricForDummy( node1, dummy1, newNetLengths );
							overlapMetricAfter = overlapForDummy( node1, dummy1 );
							areaMetricAfter    = area.areaForDummy( node1, dummy1 );
							manhattanRadiusAfter = Math.max( Math.abs(dummy1.getPlacementX() ), Math.abs( dummy1.getPlacementY() ) );
							break;
						
						case MUTATIONORIENTATE:
						
							node1 = getRandomNode();
							networkMetricBefore = metricForNode( node1 );
							overlapMetricBefore = metric.overlap(node1, posIndex.getPossibleOverlaps( node1 ) );

							dummy1 = node1.clone();
							dummy1.setPlacementOrientation(orientations[rand.nextInt(orientations.length)], true);
							
							// calculate the metrics for a layout where
							// the node is rotated
							networkMetricAfter = metricForDummy( node1, dummy1, newNetLengths );
							overlapMetricAfter = overlapForDummy(node1, dummy1 );
							areaMetricAfter    = area.areaForDummy( node1, dummy1 );
					}

					// TODO area metric is obsolete because of the manhattan geometry metric
					// which give much more compact placements
					double networkGain = networkMetricBefore - networkMetricAfter;
					double overlapGain = overlapMetricBefore - overlapMetricAfter;
					double areaGain    = (areaMetricBefore    - areaMetricAfter) / minArea;
					double manhattanRadiusGain = manhattanRadiusBefore - manhattanRadiusAfter;

					double gain = networkGain
								+ overlapGain 		  * OVERLAP_WEIGHT
								+ areaGain 			  * AREA_WEIGHT
								+ manhattanRadiusGain * MANHATTAN_WEIGHT;
					
					// the worse the gain of a perturbation the lower the
					// probability of this perturbation to actually be applied
					// (positive gains are always accepted)
					if ( Math.exp(gain / temperature) >= Math.random() )
					{
						acceptCount++;

						// Before we actually apply the perturbation, we have to check if the overlap
						// has changed. This is because we haven't locked the area we are occupying
						// and another thread could have placed a node that would now overlap.
						// The same goes for nets that could also be altered by another thread
						//
						// Note:
						// There is still a logical race condition because it is possible that
						// two moves are already accepted but after the first one has been written,
						// the second one would be most likely be rejected. it just not very likely
						// and with thousands of moves per seconds no big deal
						//
						// Performance:
						// The overlap has to be calculated twice (nets are hashed values).
						// The synchronized block is quite coarse as everything is locked.
						// It could be replaced by a smaller synchronized block that only
						// checks and locks altered parts of the placement (target areas,
						// nets)
						synchronized(posIndex)
						{
							switch(mutationType)
							{
								case MUTATIONMOVE:
									if(overlapForDummy(node1, dummy1) == overlapMetricAfter &&
									   metricForNode(node1)           == networkMetricBefore )
									{
										posIndex.move( node1 , dummy1.getPlacementX() , dummy1.getPlacementY() );
										for(PlacementNetwork net : newNetLengths.keySet())
											netLengths.put(net, newNetLengths.get(net));
									}
									else
										conflictCount++;
									break;
								
								case MUTATIONSWAP:
									if(overlapForDummy(node1, node2, dummy1, dummy2) == overlapMetricAfter &&
									   metricForNodes(node1, node2)                  == networkMetricBefore )
									{
										posIndex.swap( node1, node2 );
										for(PlacementNetwork net : newNetLengths.keySet())
											netLengths.put(net, newNetLengths.get(net));
									}
									else
										conflictCount++;
									break;
								
								case MUTATIONORIENTATE:
									if(overlapForDummy(node1, dummy1) == overlapMetricAfter &&
									   metricForNode(node1)           == networkMetricBefore )
									{
										posIndex.rotate(node1, dummy1.getPlacementOrientation());
										for(PlacementNetwork net : newNetLengths.keySet())
											netLengths.put(net, newNetLengths.get(net));
									}
									else
										conflictCount++;
							}
						}
						//node1.apply(); // TODO REMOVE
						//if(node2 != null) node2.apply(); // TODO REMOVE
					}										
				}
				
				update( stepsPerUpdate, acceptCount, conflictCount );
			}
		}

		/**
		 * Method that generates a random translation vector for a given node
		 * @param node
		 * @return a translation vector
		 */
		private Point2D randomMove( ProxyNode node )
		{
			double maxBoundingBox = 1;
			
			// the length of the vector is a function of the maximum bounding box
			// the further away connected nodes are, the further the node may move
			ArrayList<PlacementNetwork> nets = node.getNets();
			if ( nets.size() > 0 ) {
				for ( PlacementNetwork net : nets )
				{
					double metric = netLengths.get(net).doubleValue();
					if ( metric > maxBoundingBox )
						maxBoundingBox = metric;
				}
			}
			else {
				maxBoundingBox = maxChipLength;
			}
			
			// shorter vectors are more likely
			// don't move nodes too far away if they are already good positioned
			double offsetX = Math.min( Math.max( rand.nextGaussian(), -1 ), 1) * maxBoundingBox;
			double offsetY = Math.min( Math.max( rand.nextGaussian(), -1 ), 1) * maxBoundingBox;

			return new Point2D.Double( offsetX, offsetY );
		}

		/**
		 * Returns a random perturbation type.
		 * @return
		 */
		private int randomPerturbationType()
		{
			int r = rand.nextInt(100);

			// TODO: replace with variables
			// THIS IS TWEAKING DATA
			if ( r < 10 ) return MUTATIONSWAP;
			if ( r < 90 ) return MUTATIONMOVE;

			return MUTATIONORIENTATE;
		}
		
		/**
		 * Method that estimates the net lengths of all nets connected to two nodes
		 * @param n1
		 * @param n2
		 * @return
		 */
		private double metricForNodes(ProxyNode n1, ProxyNode n2)
		{
			// TODO Change so that it matches the method used in metricForDummies
			// swaps check if the net has changed by comparing two doubles that
			// are calculated in different ways for the same data
			double metric = metricForNode(n1) + metricForNode(n2);
			for(PlacementNetwork p: n1.getNets())
				if(n2.getNets().contains(p))
					metric -= netLengths.get(p).doubleValue();
					
			return metric;
		}
		
		/**
		 * Method that estimates the net lengths of all nets connected to a given node
		 * @param node the node to evaluate
		 * @return a value for the current position. The lower the value, the better.
		 */
		private double metricForNode(ProxyNode node)
		{
			double metric = 0;
			for(PlacementNetwork net : node.getNets())
				metric += netLengths.get(net).doubleValue();
			
			return metric;
		}
	}
}


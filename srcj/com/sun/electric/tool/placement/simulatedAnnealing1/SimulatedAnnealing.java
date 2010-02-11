/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SimulatedAnnealing.java
 * Written by Team 2: Jan Barth, Iskandar Abudiab
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
package com.sun.electric.tool.placement.simulatedAnnealing1;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.GenMath.MutableInteger;
import com.sun.electric.tool.placement.PlacementFrame;
import com.sun.electric.tool.placement.simulatedAnnealing1.metrics.AreaOverlapMetric;
import com.sun.electric.tool.placement.simulatedAnnealing1.metrics.BoundingBoxMetric;
import com.sun.electric.tool.placement.simulatedAnnealing1.metrics.MSTMetric;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/** Parallel Placement
 **/
public class SimulatedAnnealing extends PlacementFrame {
	
	/**
	 * Number of threads.
	 */
	private final int NUM_THREADS = 2;
	/**
	 * Number of iteration for each thread to do per temperature change.
	 */
	public final int STEP_THREAD = 20;
	/**
	 * Total number of iterations for the inner loop.
	 */
	public final int INNER_LOOP_TOTAL = 800;
	
	private Temperature temp;
	private Random rand = new Random();	

	
	// Connectivity Map (transitive hull)
	private Map<PlacementNode,Map<PlacementNode,MutableInteger>> connectivityMap;
	
	/**
	 * @see PlacementFrame#getAlgorithmName()
	 */
	@Override
	public String getAlgorithmName() {		
		return "Simulated-Annealing-1";
	}
	
	/**
	 * @see PlacementFrame#runPlacement(List, List, String);
	 */
	@Override
	protected void runPlacement(List<PlacementNode> nodesToPlace,
			List<PlacementNetwork> allNetworks, String cellName) {
		
			
		System.out.println("Simulated annealing started.");
				
		//Step 1: Random Placement 
//		for (int i = 0; i <  nodesToPlace.size(); i++ ) {
//			PlacementNode n = nodesToPlace.get(i);
			//n.setPlacement(i * 1.0 % Math.sqrt(nodesToPlace.size()) * 200, Math.round(i * 1.0 / Math.sqrt(nodesToPlace.size()))* 200);
//		}			
		
		createAndFillConnectivityMap( nodesToPlace , allNetworks );
		
		//Step 2: Create a Temperature
		temp = new Temperature(nodesToPlace);
		System.out.println("Temperature = "  + temp.getTemperature());
		
		
		//step 3: Iteration
		int numInnerSteps = INNER_LOOP_TOTAL / (NUM_THREADS * STEP_THREAD); //nodesToPlace.size() / 7 + 100; //TODO: Exponential Growth Function
		System.out.println("Inner Steps = " + numInnerSteps);
		System.out.println("Running placement, please wait...");
		//step4: Initialize Thread Pool
		ExecutorService threadPool = Executors.newFixedThreadPool( NUM_THREADS );	
		
		//step5: create workers
		LinkedList<Callable<Double>> workforce = new LinkedList<Callable<Double>>();
		for (int i = 0; i < NUM_THREADS; i++) {
			PlacementThread worker = new PlacementThread(numInnerSteps, nodesToPlace, allNetworks);
			workforce.add(worker);
		}			
		
		while (temp.nextIteration()) {
			//outer loop
									
			//test
			/*
			double BeforeScore = incState.getScore();
			for (PlacementNode n: nodesToPlace) {
				double Bscore = incState.getScore(); 
				PlacementNodePosition p = new PlacementNodePosition(nodesToPlace, nodesToPlace.indexOf(n));
				p.setPlacementY(p.getPlacementY() + 1000);
				incState.addNode(nodesToPlace.indexOf(n), p);				
				double Ascore = incState.getScore();
				System.out.println("Bscore = " + Bscore + ", AScore = " + Ascore);				
			}
			
			
			
			incState.makeGlobal();
			
			if (true) return;
			*/
			
			System.out.println("Temperature=" + temp.getTemperature());			
			for (int middleStep = 0; middleStep < numInnerSteps; middleStep++) {
				//System.out.println("New Temperature: " + temp.getTemperature());
				
				List<Future<Double>> results = null;
				
				//reset the incremental state of all threads
				for(int i = 0; i < NUM_THREADS; i++) {				
					( ( PlacementThread ) workforce.get(i) ).reset();
				}
				
				
				//start the calculation and get the results
				double minScore = Double.MAX_VALUE;
				int bestThread = -1;
				
//				System.out.println("Before invokeAll");			
				
				try {
					results = threadPool.invokeAll( workforce );			
//					System.out.println("After invokeAll");			
					
					minScore = Double.MAX_VALUE;
					bestThread = -1;
					for (int i = 0; i < NUM_THREADS; i++) {
						//retrieve the score
						Double threadScore = results.get(i).get();
						
//						System.out.println("Thread score: " + threadScore);
						
						if (threadScore.doubleValue() < minScore) {
							minScore = threadScore.doubleValue();
							bestThread = i;
						}
					}
					
					//make the best result global
					( ( PlacementThread ) workforce.get(bestThread) ).getIncState().makeGlobal();
					
					System.out.println("C1Score = " + (( PlacementThread)workforce.get(bestThread) ).getIncState().getC1());
					
				} catch (Exception  e) {
					System.out.println("An error occured. Aborting. Message:" + e.getMessage());
					e.printStackTrace(System.out);
					return;
				}
				//System.out.println("Current score = " + minScore);
			}			
		}		
		
		IncrementalState finalState = new IncrementalState(nodesToPlace, allNetworks);		
		BoundingBoxMetric bbm = new BoundingBoxMetric(nodesToPlace, allNetworks, finalState);
		System.out.println("Final bounding box metric: " + bbm.init(allNetworks));
		System.out.println("Done");
		
	}
	
	/**
	 * Calculates the minimal chip area. This method simply calculates the sum 
	 * of all <code>PlacementNode</code>s.
	 * @param nodesToPlace a list containing all <code>PlacementNode</code>s.
	 * @return the minimal chip area.
	 */
	private double getMinChipArea(List<PlacementNode> nodesToPlace){
		double area = 0.0;
		
		for(PlacementNode node : nodesToPlace){
			area += node.getHeight()*node.getWidth();
		}
		return area;
	}	
	
		
	/**
	 * This method fills the connectivity by connecting every node to all nodes in its network
	 * @param nodesToPlace
	 * @param allNetworks
	 */
	private void createAndFillConnectivityMap(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks) {
		connectivityMap = new HashMap<PlacementNode,Map<PlacementNode,MutableInteger>>();
		for(PlacementNetwork plNet : allNetworks)
		{
			// add all combinations of the nodes on this net to the connectivity map
			List<PlacementPort> portsInNetwork = plNet.getPortsOnNet();
			for(int i=0; i<portsInNetwork.size(); i++)
			{
				PlacementPort plPort1 = portsInNetwork.get(i);
				PlacementNode plNode1 = plPort1.getPlacementNode();
				for(int j=i+1; j<portsInNetwork.size(); j++)
				{
					PlacementPort plPort2 = portsInNetwork.get(j);
					PlacementNode plNode2 = plPort2.getPlacementNode();
					
					incrementMap(plNode1, plNode2);
					incrementMap(plNode2, plNode1);
				}
			}
		}

	}
	
	 /** Method to build the connectivity map by adding a connection between two PlacementNodes.
	 * This method is usually called twice with the PlacementNodes in both orders because
	 * the mapping is not symmetric.
	 * @param plNode1 the first PlacementNode.
	 * @param plNode2 the second PlacementNode.
	 */
	private void incrementMap(PlacementNode plNode1, PlacementNode plNode2)
	{
		Map<PlacementNode,MutableInteger> destMap = connectivityMap.get(plNode1);
		if (destMap == null)
			connectivityMap.put(plNode1, destMap = new HashMap<PlacementNode,MutableInteger>());
		MutableInteger mi = destMap.get(plNode2);
		if (mi == null) destMap.put(plNode2, mi = new MutableInteger(0));
		mi.increment();
	}
	
	/**
	 * Method to return the number of connections between two PlacementNodes.
	 * @param plNode1 the first PlacementNode.
	 * @param plNode2 the second PlacementNode.
	 * @return the number of connections between the PlacementNodes.
	 */
	private int getConnectivity(PlacementNode plNode1, PlacementNode plNode2)
	{
		Map<PlacementNode,MutableInteger> destMap = connectivityMap.get(plNode1);
		if (destMap == null) return 0;
		MutableInteger mi = destMap.get(plNode2);
		if (mi == null) return 0;
		return mi.intValue();
	}
	
	/**
	 * Returns the highest connected node to the given node
	 * @param node the node for which the highest connected to be found
	 * @return the highest connected node to the given node
	 */
	public PlacementNode getHighestConnectedNode(PlacementNode node){
		PlacementNode highestConnected = null;
		MutableInteger highest = new MutableInteger( 0 );
		Map<PlacementNode, MutableInteger> conn = connectivityMap.get( node );
		if( conn == null ) return null;
		for ( PlacementNode n : conn.keySet() ){
			if( conn.get( n ).intValue() > highest.intValue() ){
				highest = conn.get( n );
				highestConnected = n;
			}
		}
		return highestConnected;
	}
	
	/**
	 * An implementation of a worker thread that does the moving and swapping 
	 * of the placement nodes in parallel.
	 */
	public class PlacementThread implements Callable<Double> {
		private IncrementalState incState = null;
//		private List<PlacementNode> allNodes;
//		private List<PlacementNetwork> allNetworks;
		private int numSteps = 0;
		
		/**
		 * Creates this.
		 * @param numSteps the number of iterations which this runnable will do.
 		 * @param allNodes a list of all <code>PlacementNode</code>s
		 * @param allNetworks a list of all <code>PlacementNetwork</code>s.
		 */
		public PlacementThread(int numSteps, List<PlacementNode> allNodes, List<PlacementNetwork> allNetworks) {
//			this.allNodes = allNodes;
//			this.allNetworks = allNetworks;			
			this.numSteps = numSteps;
			this.incState = new IncrementalState(allNodes, allNetworks);									
		}
		
		/**
		 * @see Callable#call();
		 */
		public Double call() throws Exception {					
			
			for (int i = 0; i < numSteps; i++) {
				//Swap or Move
				double r = rand.nextDouble();
				if (r < 0.2) {
					//we swap					
					if (!incState.chooseAndSwapNodes()) {
						//swap failed, do something else
					}					
				} else {
					//we displace (move)
					if (!incState.moveNode()) {
						//move failed, do something else
					}
				}				
			}				
								
			return new Double(incState.getScore());
		}
		
		/**
		 * Returns the <code>IncrementalState</code> object of this runnable
		 * @return the <code>IncrementalState</code> object of this runnable
		 */
		public IncrementalState getIncState() {
			return incState;
		}
		
		/**
		 * Resets the state.
		 */
		public void reset() {
			incState.reset();
		}				
				
	}
	
	/**
	 * A representation of temperature
	 */
	public class Temperature {
		private double temperature;
		private final double initialTemperature = 5000.0;
		private final double threshholdTemperature = 0.1;
		private Random rand = null;
		private double initialChipLength;
//		private List<PlacementNode> nodesToPlace;
		
		/**
		 * Creates this temeperature object
		 * @param nodesToPlace a list of all <code>PlacementNode</code>s to be placed.
		 */ 
		public Temperature (List<PlacementNode> nodesToPlace) {
			rand = new Random();
			rand.setSeed(System.currentTimeMillis());
			initialChipLength = Math.sqrt(getMinChipArea(nodesToPlace))*2;
			System.out.println("Initial Chip Length = " + initialChipLength);
//			this.nodesToPlace = nodesToPlace;
			temperature = initialTemperature;
		}
		
		/**
		 * Returns the current maximal swapping distance based on the current temperature.
		 * @return the current swapping distance.
		 */
		public double getCurrentSwapDistance(){
			double newSwapDisttance = initialChipLength*(Math.log(temperature)/Math.log(initialTemperature));			
			return newSwapDisttance;
		}
		
		/**
		 * Sets the temperature.
		 * @param newTemperature the new temperature.
		 */
		public void setTemperature(double newTemperature) {
			this.temperature = newTemperature;
		}
		
		/**
		 * Gets the temperature.
		 * @return the current temperature.
		 */
		public double getTemperature() {
			return temperature;
		}
		
		/**
		 * Resets the temperature.
		 */
		public void reset() {
			temperature = initialTemperature;
		}
		
		/**
		 * Decreases the temperature and checks for stop condition.
		 * @return true if current temperature hasn't reached threshold. false otherwise
		 */
		public boolean nextIteration () {
			decTempQuadratically();
			return (temperature > threshholdTemperature);
		}
		
		/**
		 * Decreases the temperature.
		 */
		private void decTempQuadratically() {
			//x > -1 -> 1+
			double x = 1 / Math.log(1+ temperature) * 2 - 1 ;			
			double xSquared = x * x;			
			double alpha = 0.95 -  xSquared * (0.95 - 0.80) ;
			//System.out.println("x = " + x);
			temperature = alpha * temperature;
		}
		
		/**
		 * Decides whether to accept a new state or not
		 * @param deltaE the difference in score between new and old states.
		 * @return true for accept. false otherwise.
		 */
		public boolean accept(double deltaE) {
			if (deltaE < 0) return true;
			return false;
			//double randomNumber = rand.nextDouble();
			//double threshold = Math.exp(-deltaE / temperature);
			//return (randomNumber < temperature / initialTemperature / 10) ;
		}	
	}
	
	/**
	 * A class for storing node positions and rotations
	 */
	public class PlacementNodePosition {
		private double x, y;
		private int index;
		private Orientation orientation = Orientation.IDENT;
		
		/**
		 * Creates this.
		 * @param allNodes a list of all <code>PlacementNode</code>s.
		 * @param index the index of the desired <code>PlacementNode</code> to store the
		 * coordinates for.
		 */
		public PlacementNodePosition (List<PlacementNode> allNodes, int index) {
			PlacementNode originalNode = allNodes.get(index);
			this.x = originalNode.getPlacementX();
			this.y = originalNode.getPlacementY();
			this.index = index;
			this.orientation = originalNode.getPlacementOrientation();
		}
		
		// Getters & Setters
		public double getPlacementX() {
			return x;			
		}
		
		public void setPlacementX(double x) {
			this.x = x;
		}
		
		public double getPlacementY() {
			return y;
		}
		public void setPlacementY(double y) {
			this.y =y;
		}
		
		public void setPlacement(double x, double y) {
			this.x = x;
			this.y = y;
		}
		
		public Orientation getPlacementOrientation() {
			return orientation;
		}
		
		public void setPlacementOrientation(Orientation o) {
			this.orientation = o;
		}
		
		public int getIndex() {
			return index;
		}
		
		public void setIndex(int index) {
			this.index = index;
		}
		
	}
	
	/**
	 * A representation of a state of the nodes on the chip.
	 * Every worker thread gets a local <code>IncrementalState</code> object
	 * on which it apply the changes. After {@link STEP_THREAD} iterations all 
	 * <code>IncrementalState</code>s of all worker threads are compared and the
	 * state with best score becomes the new state for all threads.
	 * @see IncrementalState#makeGlobal()
	 */
	public class IncrementalState {
		private HashMap<Integer, PlacementNodePosition> changedNodes; //index -> NewNode
		private List<PlacementNode> originalNodes;
		private List<PlacementNetwork> allNetworks;
		
		private double currentC1, currentC2;
		private MSTMetric C1Metric;
		private AreaOverlapMetric C2Metric;
		
		/**
		 * Creates this.
		 * @param allNodes a list of all <code>PlacementNode</code>s.
		 * @param allNetworks a list of all <code>PlacementNetwork</code>s.
		 */
		public IncrementalState (List<PlacementNode> allNodes, List<PlacementNetwork> allNetworks) {
			changedNodes = new HashMap<Integer, PlacementNodePosition>( STEP_THREAD );
			originalNodes = allNodes;
			this.allNetworks = allNetworks;			
			C1Metric = new MSTMetric(allNodes, allNetworks, this);
			C2Metric = new AreaOverlapMetric(allNodes, this);			
			currentC1 = C1Metric.init(allNetworks);			
			currentC2 = C2Metric.init(allNodes);
		}
		
		/**
		 * Adds a node to the list of changed nodes.
		 * @param index the index of the changed node.
		 * @param newNode a new <code>PlacementNodePosition</code> for the changed node.
		 * @return metric score for the current change.
		 */
		public double addNode(int index, PlacementNodePosition newNode) {			
			changedNodes.put(new Integer(index), newNode);
			currentC1 = C1Metric.update(index);
			currentC2 = C2Metric.update(index);
			return currentC1 + currentC2;
		}
		
		public double removeNode(int index) {
			changedNodes.remove(new Integer(index));
			currentC1 = C1Metric.update(index);
			currentC2 = C2Metric.update(index);
			return currentC1 + currentC2;
		}
		
		/**
		 * Moves a node.
		 * This method chooses a node at random and tries to place it near its highest 
		 * connected node.
		 * @return true if the move was accepted. false otherwise.
		 */
		public boolean moveNode() {
			int index = (int) Math.round(rand.nextDouble()*(originalNodes.size()-1));
			PlacementNodePosition theOne = getNodeFromState(index);
			double x = theOne.getPlacementX();
			double y = theOne.getPlacementY();
			double maxDistance = 100;//temp.getCurrentSwapDistance()/10;  
			
			double newX = x - rand.nextDouble() * maxDistance / 2 + rand.nextDouble() * maxDistance; 
			double newY = y - rand.nextDouble() * maxDistance / 2 + rand.nextDouble() * maxDistance;
			
			PlacementNode bestPartner = getHighestConnectedNode( originalNodes.get( index ) );
			if(bestPartner != null ){
				double pX = bestPartner.getPlacementX();
				double pY = bestPartner.getPlacementY();
				if( rand.nextBoolean() ) {
					newX = pX + ((rand.nextBoolean()? 1:-1)* bestPartner.getWidth());
					newY = pY;
				} else {
					newX = pX;
					newY = pY + ((rand.nextBoolean()? 1:-1)* bestPartner.getHeight());
				}
			}else{
				// Some other calculation
			}
			//newX  = newX % (temp.initialChipLength);
			//newY = newY % (temp.initialChipLength);
			
			theOne.setPlacement(newX, newY);
			
			//check metric
			double metricBeforeMove = getScore();
			
//			double networksScoreBeforeMove = C1Metric.getNetworkScoreForNode( originalNodes.get( index ) );
			
			addNode(index, theOne);
			
//			double networksScoreAfterMove = C1Metric.getNetworkScoreForNode( originalNodes.get( index ) );
							
			double metricAfterMove = getScore();
			double deltaE =  (metricAfterMove - metricBeforeMove) / metricAfterMove;
			
//			if( networksScoreAfterMove >= 2 * networksScoreBeforeMove ) {
//				//undo
//				theOne.setPlacement(x,y);
//				addNode(index, theOne);
//				return false;
//			}
			if (temp.accept(deltaE)) {
				return true;
			} else {
				//undo
				theOne.setPlacement(x,y);
				addNode(index, theOne);
				return false;
			}
		}
		
		/**
		 * Swaps two nodes.
		 * This method chooses two nodes at random and swaps them.
		 * @return true if the swap was accpeted. fase otherwise.
		 */
		public boolean chooseAndSwapNodes(){
			double maxDistance = temp.getCurrentSwapDistance()/10;
			double maxDistanceSquared = maxDistance * maxDistance;
			int numNodes = originalNodes.size();
			int index1 = (int) Math.round(rand.nextDouble()*(numNodes-1));
			
			//Find a partner to swap 
			int index2 = 0;
			boolean partnerFound = false;
			for (int i = 0; i < numNodes / 10; i++) {
				index2 = (int) Math.round(rand.nextDouble()*(numNodes-1));
				if (index2 == index1) continue;
				if( getConnectivity( originalNodes.get( index1 ) , originalNodes.get( index2 ) ) > 4 ) continue;
				
				double node1X = getNodeFromState(index1).getPlacementX();
				double node2X = getNodeFromState(index2).getPlacementX();
				double node1Y = getNodeFromState(index1).getPlacementY();
				double node2Y = getNodeFromState(index2).getPlacementY();			
				double distance = (node1X - node2X) * (node1X - node2X) + (node1Y - node2Y) * (node1Y - node2Y);
				double distanceSquared = distance * distance;
				if (distanceSquared < maxDistanceSquared) {
					partnerFound = true;
					break;
				}
			}
			if (!partnerFound) return false;
			
			PlacementNodePosition n1 = getNodeFromState(index1);
			PlacementNodePosition n2 = getNodeFromState(index2);				
			
			double tempX = n1.getPlacementX(), tempY = n1.getPlacementY();
			n1.setPlacementX(n2.getPlacementX());
			n1.setPlacementY(n2.getPlacementY());
			n2.setPlacementX(tempX);
			n2.setPlacementY(tempY);
			
			double metricBeforeSwap = getScore();
			
			addNode(index1, n1);
			addNode(index2, n2);
			
			double metricAfterSwap = getScore();
			
			double deltaE = (metricAfterSwap - metricBeforeSwap) / metricAfterSwap;
			if (temp.accept(deltaE)) {
				return true;
			} else {
				//unswap nodes				
				n2.setPlacementX(n1.getPlacementX());
				n2.setPlacementY(n1.getPlacementY());
				n1.setPlacementX(tempX);
				n1.setPlacementY(tempY);
				
				addNode(index1, n1);
				addNode(index2, n2);
				return false;
			}						
		}
		
		/**
		 * Returns the <code>PlacementNodePosition</code> for a node given the index.
		 * @param index the index of the node
		 * @return the <code>PlacementNodePosition</code> for this node.
		 */
		public PlacementNodePosition getNodeFromState(int index) {
			Integer ii = new Integer(index);
			if (changedNodes.containsKey(ii)) {
				return changedNodes.get(ii);
			} else {
				return new PlacementNodePosition(originalNodes, index);
			}			
		}
		
		public boolean isNodeChanged(int index) {
			return changedNodes.containsKey(new Integer(index));
		}
		
		public List<PlacementNode> getOriginalNodes() {
			return originalNodes;
		}
		
		public double getC1() {
			return currentC1;
		}
		
		public double getC2() {
			return currentC2;
		}
		
		/**
		 * Returns the score of the two metrics.
		 * @return the sum of both metric scores.
		 */
		public double getScore() {
			return currentC1 + 1000000 * currentC2;
		}
		
		/**
		 * Resets the state.
		 * @return the current metric score.
		 */
		public double reset() {
			changedNodes.clear();
			//Recalculate Netscores
			currentC1 = C1Metric.init(allNetworks);
			//Recalculate Overlaps
			currentC2 = C2Metric.init(originalNodes);
			//Recalculate Metrics
			return getScore();
			//return score
		}
			
		/**
		 * This methods makes this <code>IncrementalState</code> object a global state, i.e. the changes
		 * of this state will be applied to the nodes that are to be placed.
		 * @return the current metric score.
		 */
		public double makeGlobal() {
			for (Iterator<Entry<Integer, PlacementNodePosition>> it = changedNodes.entrySet().iterator(); it.hasNext(); ) {
				Entry<Integer, PlacementNodePosition> entry = it.next();
				int index = entry.getKey().intValue();
				PlacementNodePosition n = entry.getValue();
				PlacementNode originalPlacementNode = originalNodes.get(index);
				originalPlacementNode.setPlacement(n.getPlacementX(), n.getPlacementY());
				originalPlacementNode.setOrientation(n.getPlacementOrientation());				
			}
			
			changedNodes.clear();
			
			return getScore();
		}
		
		
	}
	
}
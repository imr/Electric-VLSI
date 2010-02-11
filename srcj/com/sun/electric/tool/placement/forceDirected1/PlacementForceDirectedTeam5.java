/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementForceDirectedTeam5.java
 * Written by Team 5: Andreas Wagner, Thomas Hauck, Philippe Bartscherer
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
package com.sun.electric.tool.placement.forceDirected1;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.GenMath.MutableInteger;
import com.sun.electric.tool.placement.PlacementFrame;
import com.sun.electric.tool.placement.forceDirected1.metric.BBMetric;
import com.sun.electric.tool.placement.forceDirected1.metrics.PAMetric;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class PlacementForceDirectedTeam5 extends PlacementFrame {
	//------ Parameters from the standalone Placementframe -------------
	// maximum runtime of the placement algorithm in seconds
	public int maxRuntime = 60;
	// number of threads
	public int numThreads = 4;
	// if false: NO system.out.println statements
	public boolean printDebugInformation = true;
	
	// parameters
	Heuristic p;	// all global parameter
	double overlapWeightingFactor;

	// algorithm variables
	private List<PlacementNode> nodesToPlace;
	List<PlacementNetwork> allNetworks;
	private Map<PlacementNode, Map<PlacementNode, MutableInteger>> connectivityMap;
	Point2D.Double centerOfMass;
	private double[] forceX;
	private double[] forceY;
	double cellCount[][];
	private double mirrorGain;	
	public BBMetric bb;
	public BBMetric bbPartial;
	
	// information for the benchmark framework
	String teamName = "team 5";
	String studentName1 = "Thomas Hauck";
	String studentName2 = "Andreas Wagner";
	String algorithmType = "force directed";
	

	/**
	 * Method to return the name of this placement algorithm.
	 * 
	 * @return the name of this placement algorithm.
	 */
	public String getAlgorithmName() {
		return "Force-Directed-1";
	}

	public void setBenchmarkValues(int runtime, int threads, boolean debug) {
		maxRuntime = runtime;
		numThreads = threads;
		printDebugInformation = debug;
	}
	
	// --- Force-Directed placement -------------------------------------------

	private class Heuristic {
		// --- parameter ---
		public int max_iterations = 2000;		// e.g. 1000
		public int max_resolve = 5000;			// e.g. 1000
		public int max_rows    = 15;			// e.g. 15

		public double donut_inner = 2500;		// e.g. 2500
		public double spread_length = 3750;		// e.g. 3750
		public double spread_force = 400;		// e.g. 400
		public double spread_slowdown = 20;		// e.g. 100

		// --- statistics ---
		public double W;						// minimum node width
		public double H;						// minimum node height
		public double totalArea;				// calculated in constructor
		public double areaPerCell;

		// --- public ---
		public Heuristic(List<PlacementNode> nodesToPlace) {
			calculateStatistics(nodesToPlace);
			heuristic1(nodesToPlace);
		}
		
		/**
		 * my parameter for placement results
		 */
		private void heuristic1(List<PlacementNode> nodesToPlace) {
			int nodes = nodesToPlace.size();
			// better dynamic heuristic
			this.max_rows = (int)(16+Math.sqrt(nodes/5.3));
			this.donut_inner = Math.sqrt(totalArea);
			this.spread_length = 1.5 * donut_inner;			
			this.spread_slowdown = 5;
			if(totalArea < 200) spread_force = 40;
			if(nodesToPlace.size() > 500) spread_force = 2500;
			if(nodesToPlace.size() > 2000) spread_force = 8000;
		
			if(printDebugInformation)	{
				System.out.println("Heuristic3: max_rows=" + max_rows);
				System.out.println("total Area: " + totalArea);
			}
		}

		// --- statistics ---
		private void calculateStatistics(List<PlacementNode> nodesToPlace) {
			getMinDimensions(nodesToPlace);
			this.totalArea = getTotalArea(nodesToPlace);
			this.areaPerCell = totalArea/(max_rows*max_rows);
		}
		
		private double getTotalArea(List<PlacementNode> nodesToPlace) {
			double area = 0;
			for(int i=0; i<nodesToPlace.size(); i++) {
				area += nodesToPlace.get(i).getWidth() * nodesToPlace.get(i).getHeight();
			}
			return area;
		}

		/**
		 * Get dimensions of the smallest node.
		 */
		private void getMinDimensions(List<PlacementNode> nodesToPlace) {
			// inputs: nodesToPlace
			// outputs: W, H
			this.W = Double.MAX_VALUE;		
			this.H = Double.MAX_VALUE;		
			for(PlacementNode node : nodesToPlace) {
				if(node.getWidth()>0)  W = Math.min(node.getWidth(),W);
				if(node.getHeight()>0) H = Math.min(node.getHeight(),H);
			}
		}
	}
	
	/**
	 * Method to do Force-Directed Placement.
	 * 
	 * @param nodesToPlace
	 *            a list of all nodes that are to be placed.
	 * @param allNetworks
	 *            a list of all networks that connect the nodes.
	 * @param cellName
	 *            the name of the cell being placed.
	 */
	public void runPlacement(List<PlacementNode> nodesToPlace, 
			List<PlacementNetwork> allNetworks, String cellName) {
		initTimeout(0);
		bb = new BBMetric();
		bbPartial = new BBMetric();
		// initialize algorithm
		dbg = new Debug(); 							// debug object
		if(printDebugInformation) {
			bb.setBenchmarkValues(nodesToPlace, allNetworks);
			dbg.tick();								// total time
			dbg.tick();								// initialization time
		}

		// parameter
		this.nodesToPlace = nodesToPlace;
		this.allNetworks = allNetworks;
		this.p = new Heuristic(nodesToPlace);		// calculate all default parameters
		overlapWeightingFactor = 0;
		// allocate variables
		centerOfMass = new Point2D.Double();
		forceX = new double[nodesToPlace.size()];
		forceY = new double[nodesToPlace.size()];
		cellCount = new double[p.max_rows][p.max_rows];

		// build connectivity map
		if(printDebugInformation) {
			dbg.println("=== FORCE-DIRECTED =====================");
		}
		buildConnectivityMap(allNetworks);
		randomPlaceDonut(p.donut_inner, p.donut_inner/4);	// e.g. 2500, 500
		if (printDebugInformation) {
			dbg.tack("Initialization");
		}
		swapNodes();

		// force directed placement
		if(printDebugInformation) {
			dbg.println("Threads",numThreads);
			dbg.println("Nodes",nodesToPlace.size());
			dbg.tick();
		}

		// phase1 : calculate forces and move nodes
		// --- parallel --- 
		initTimeout(1);
		startThreads();											// step1 & step2 (parallel)
		waitThreads();
		if(printDebugInformation) {
			dbg.tack("Phase1", true);
		}

		// phase2 : resolve overlapping nodes
		overlapWeightingFactor = 1;
		PAMetric chipArea = new PAMetric(nodesToPlace, allNetworks);
		boolean overlap = true;
		int i = 0;
		double whRatio=1;
		calculateCenterOfMass();								// step3
		initTimeout(2);
		for (i=0; overlap && i < p.max_resolve && checkTimeout(); i++) {	// step4
			chipArea.compute();
			whRatio = chipArea.getWidth() / chipArea.getHeight();
			if(nodesToPlace.size()>3000 ||((nodesToPlace.size() > 1000 && 
					nodesToPlace.size() < 3000 && maxRuntime < 120))) {
				overlap = resolveOverlapsFast(overlapWeightingFactor, whRatio);
			} else {
				overlap = resolveOverlaps(overlapWeightingFactor, whRatio);
			}
		}				
		if(printDebugInformation) {
			dbg.println("Number of resolve Steps: " + i);
			dbg.tack("Phase2");
			dbg.println("=== after resolve Overlap ==============================");
			dbg.println( bb.toString() );	// metric
		}
	
		// rotate nodes
		initTimeout(3);
		iterativeFindOrientations(nodesToPlace);	
		if (printDebugInformation) {
			dbg.println("=== after rotation/mirroring ======================");
			dbg.println( bb.toString() );	// metric
		}

		// fill empty bins
		initTimeout(4);
		fillEmptyBins();				
		if (printDebugInformation) {
			dbg.println("=== after fillEmptyBins ======================");
			dbg.println( bb.toString() );	// metric
		}
		
		// shake nodes
		initTimeout(5);
		shakeNodes();
		if (printDebugInformation) {
			dbg.println("=== after shakeNodes ======================");
			dbg.println( bb.toString() );	// metric
			dbg.println("=== FINAL placement ======================");
			dbg.tack("Total");				// total time
			dbg.println( bb.toString() );	// metric
			dbg.flush();
		}
		

	}

	//--- algorithm : phase1 --------------------------------------------------
	
	/**
	 *  Step1: method to calculate attracting and repulsing forces
	 *  
	 */
	private void calculateForces(int firstIndex, int lastIndex) {
		PlacementNode node1;								//!faster
		PlacementNode node2;								//!faster
		// attracting forces
		// inputs:  nodesToPlace, connectivityMap 
		// outputs: forceX[], forceY[]
		for (int i = firstIndex; i <= lastIndex && checkTimeout(); i++) {
			// clear output
			forceX[i] = 0;
			forceY[i] = 0;
			// loop cells
			node1 = nodesToPlace.get(i);
			Iterator<Entry<PlacementNode, MutableInteger>> destMapIterator;
			Map<PlacementNode, MutableInteger> destMap = connectivityMap.get(node1);
			if (destMap != null) {
				destMapIterator = destMap.entrySet().iterator();
				while (destMapIterator.hasNext()) {
					Entry<PlacementNode, MutableInteger> srcEntry = destMapIterator.next();
					node2 = srcEntry.getKey();
					forceX[i] += (node2.getPlacementX() - node1.getPlacementX()) * destMap.get(node2).intValue();
					forceY[i] += (node2.getPlacementY() - node1.getPlacementY()) * destMap.get(node2).intValue();
				}
			}
			
		}
		// repulsing forces for overlaps
		// inputs:  nodesToPlace, forceX[], forceY[]
		// outputs: forceX[], forceY[]
		for (int i = firstIndex; i <= lastIndex && checkTimeout(); i++) {
			node1 = nodesToPlace.get(i);
			int x = getCol(node1);
			int y = getRow(node1);
			if(cellCount[x][y] > p.areaPerCell) {
				try {
					if(cellCount[x-1][y] < p.areaPerCell) {
						forceX[i] -=Math.min(5*p.spread_force,
								(cellCount[x][y]/p.areaPerCell)*
								(-p.spread_force*cellCount[x-1][y]/p.areaPerCell 
										+ p.spread_force));
					}
				} catch (IndexOutOfBoundsException e) {
					
				}
				try {
					if(cellCount[x+1][y] < p.areaPerCell) {
						forceX[i] += Math.min(5*p.spread_force,
								(cellCount[x][y]/p.areaPerCell)*
								(-p.spread_force*cellCount[x+1][y]/p.areaPerCell 
										+ p.spread_force));
					}
				} catch (IndexOutOfBoundsException e) {
					
				}
				try {
					if(cellCount[x][y-1] < p.areaPerCell) {
						forceY[i] -= Math.min(5*p.spread_force,
								(cellCount[x][y]/p.areaPerCell)*
								(-p.spread_force*cellCount[x][y-1]/p.areaPerCell 
										+ p.spread_force));
					}
				} catch (IndexOutOfBoundsException e) {
					
				}
				try {
					if(cellCount[x][y+1] < p.areaPerCell) {
						forceY[i] += Math.min(5*p.spread_force,
								(cellCount[x][y]/p.areaPerCell)*
								(-p.spread_force*cellCount[x][y+1]/p.areaPerCell 
										+ p.spread_force));
					}
				} catch (IndexOutOfBoundsException e) {
					
				}
			}		
		}
	}

	
	private void count() {
		for(int i = 0; i < p.max_rows; i ++) {
			for (int j = 0; j < p.max_rows; j++) {
				cellCount[i][j] = 0;
			}
		}
		for(PlacementNode node : nodesToPlace) {							
			cellCount[getCol(node)][getRow(node)] += 
				Math.min(p.areaPerCell,node.getHeight()*node.getWidth());
		}
	}
	
	int getCol(PlacementNode node) {
		int x = (int)(node.getPlacementX()*p.max_rows/p.spread_length);
		if(x < 0) x = 0;
		else if(x >= p.max_rows) x = p.max_rows-1;
		return x;
	}
	
	int getRow(PlacementNode node) {
		int y = (int)(node.getPlacementY()*p.max_rows/p.spread_length);
		if(y < 0) y = 0;
		else if(y >= p.max_rows) y = p.max_rows-1;
		return y;
	}
	
	
	/**
	 * Step2 : move all nodes to new position 
	 * 
	 * @param weightingFactor
	 */
	private void moveCells(double weightingFactor) {
		// loop all nodes
		PlacementNode node;
		int size = nodesToPlace.size();
		for (int i = 0; i < size; i++) {
			node = nodesToPlace.get(i);
			node.setPlacement(node.getPlacementX() + forceX[i] * weightingFactor, 
							  node.getPlacementY() + forceY[i] * weightingFactor);
		}
	}
	
	// --- algorithm : phase2 ---------------------------------------------

	/**
	 * Step4 : calculate mass center of all nodes
	 *  
	 */
	private void calculateCenterOfMass() {
		// inputs:  nodesToPlace
		// outputs: centerOfMass
		double x = 0;
		double y = 0;
		double mass = 0;
		double totalMass = 0;
		PlacementNode node;
		// loop cells
		for (int i = 0; i < nodesToPlace.size(); i++) {
			node = nodesToPlace.get(i);
			mass = node.getHeight() * node.getWidth();
			x += node.getPlacementX() * mass;
			y += node.getPlacementY() * mass;
			totalMass += mass;
		}
		centerOfMass.setLocation(x / totalMass, y / totalMass);
	}

	/**
	 * Step5 : resolves overlapping cells
	 * 
	 * @param overlapWeightingFactor
	 * @param shapeRatio
	 * @return
	 */
	boolean resolveOverlaps(double overlapWeightingFactor, double shapeRatio) {
		// inputs : nodesToPlace, centerOfMass, W, H
		boolean isOverlapping = false;
		PlacementNode node1;
		PlacementNode node2;
		Point2D.Double overlap = new Point2D.Double();
		double distNode1;
		double distNode2;
		double sigma1;
		double sigma2;
		final double lowerBound = 0.6;
		final double upperBound = 1.8;
		int size = nodesToPlace.size();					// number of nodes to place
		for (int i = 0; i < size && checkTimeout(); i++) {
			node1 = nodesToPlace.get(i);
			for (int j = i + 1; j < size && checkTimeout(); j++) {
				node2 = nodesToPlace.get(j);
				if (getOverlap(node1, node2, overlap) != 0) {
					isOverlapping = true;
					distNode1 = Math.abs(centerOfMass.x - node1.getPlacementX())
							  + Math.abs(centerOfMass.y - node1.getPlacementY());
					distNode2 = Math.abs(centerOfMass.x - node2.getPlacementX())
							  + Math.abs(centerOfMass.y - node2.getPlacementY());				
					sigma1 = 0;
					sigma2 = 0;
					if (distNode1 > distNode2) { 		// move node1
						if(overlap.y/overlap.x > lowerBound)	
							sigma1 = (node1.getPlacementX() - centerOfMass.x) / distNode1;
						if(overlap.y/overlap.x < upperBound)	
							sigma2 = (node1.getPlacementY() - centerOfMass.y) / distNode1;

						if(shapeRatio>1)				//! new: consider square shape
							sigma1 *= 0.1;				//
						else if(shapeRatio<1)			//
							sigma2 *= 0.1;				//

						node1.setPlacement(node1.getPlacementX() + 
								overlapWeightingFactor * sigma1 * p.W,
								node1.getPlacementY() + sigma2 * p.H);
					} else {							// move node2
						if(overlap.y/overlap.x > lowerBound)
							sigma1 = (node2.getPlacementX() - centerOfMass.x) / distNode2;
						if(overlap.y/overlap.x < upperBound)
							sigma2 = (node2.getPlacementY() - centerOfMass.y) / distNode2;

						if(shapeRatio>1)				//! new: consider square shape
							sigma1 *= 0.1;				//
						else if(shapeRatio<1)			//
							sigma2 *= 0.1;				//

						node2.setPlacement(node2.getPlacementX() + overlapWeightingFactor * sigma1 * p.W,
										   node2.getPlacementY() + sigma2 * p.H);
					}
				}
			}
		}
		return isOverlapping;
	}
	
	/**
	 * Step5 : resolves overlapping cells
	 * 
	 * @param overlapWeightingFactor
	 * @param shapeRatio
	 * @return
	 */
	boolean resolveOverlapsFast(double overlapWeightingFactor, double shapeRatio) {
		// inputs : nodesToPlace, centerOfMass, W, H
		boolean isOverlapping = false;
		PlacementNode node1;
		PlacementNode node2;
		Point2D.Double overlap = new Point2D.Double();
		double distNode1;
		double distNode2;
		int size = nodesToPlace.size();					// number of nodes to place
		for (int i = 0; i < size && checkTimeout(); i++) {
			node1 = nodesToPlace.get(i);
			for (int j = i + 1; j < size && checkTimeout(); j++) {
				node2 = nodesToPlace.get(j);
				if (getOverlap(node1, node2, overlap) != 0) {
					isOverlapping = true;
					distNode1 = Math.abs(centerOfMass.x - node1.getPlacementX())
							  + Math.abs(centerOfMass.y - node1.getPlacementY());
					distNode2 = Math.abs(centerOfMass.x - node2.getPlacementX())
							  + Math.abs(centerOfMass.y - node2.getPlacementY());				
					if (distNode1 > distNode2) { 		// move node1
						if(Math.random()>= 0.5) {
							node1.setPlacement(node1.getPlacementX()+1.00001*(overlap.x) 
									*Math.signum(centerOfMass.x - node1.getPlacementX()),
									node1.getPlacementY());
						} else {
							node1.setPlacement(node1.getPlacementX(), node1.getPlacementY() + 
									1.00001*(overlap.y)*
									Math.signum(centerOfMass.y - node1.getPlacementY()));
						}
					} else {
						if(Math.random()>= 0.5) {
							node2.setPlacement(node2.getPlacementX()+1.00001*(overlap.x)  
									*Math.signum(centerOfMass.x - node2.getPlacementX()),
									node2.getPlacementY());
						} else {
							node2.setPlacement(node2.getPlacementX(), node2.getPlacementY() 
									+ 1.00001*(overlap.y)*
									Math.signum(centerOfMass.y - node2.getPlacementY()));
						}
					}
				}
			}
		}
		return isOverlapping;
	}
	

	// --- Algorithm : private methods ----------------------------------------

	/**
	 * Initial placement for all nodes. Place them randomly in form of a square donut.
	 * 
	 * @param innerSize
	 * @param border
	 */
	private void randomPlaceDonut(double innerSize, double border) {
		Random rand = new Random(1);						// pseudo random
		double posX;
		double posY;
		for(PlacementNode node : nodesToPlace) {
			do {
				posX = rand.nextDouble() * (2 * border + innerSize);
				posY = rand.nextDouble() * (2 * border + innerSize);
			} while (posX > border && posX < innerSize + border &&
					 posY > border && posY < innerSize + border);
			node.setPlacement(posX, posY);
		}
	}	

	/**
	 * Calculate the smallest step a node can move.
	 *
	 * @return smallest step
	 */
	private double getMobilityFactor() {
		// inputs: forceX[], forceY[]
		double avg = 0;
		double max = 0;
		double vectorLength = 0;
		int size = forceX.length;
		for(int i = 0; i < size; i++) {
			vectorLength = Math.abs(forceX[i]) + Math.abs(forceY[i]);
			avg += vectorLength;
			if(vectorLength > max) max = vectorLength;
		}
		avg /= forceX.length;
		if(max == 0) max = 1;
		return avg/(p.spread_slowdown * max);
	}

	/**
	 * Returns absolute value of Overlap between 2 nodes.
	 * Method optimized, 2 times faster.
	 * 
	 * @param node1
	 * @param node2
	 * @param out
	 * @return overlapping area
	 */
	private double getOverlap(PlacementNode node1, PlacementNode node2, Point2D.Double out) {
		double diffX = Math.abs(node1.getPlacementX() - node2.getPlacementX());
		double diffY = Math.abs(node1.getPlacementY() - node2.getPlacementY());
		double overlapX = Math.min(0, diffX - (node1.getWidth()  + node2.getWidth())  / 2);
		double overlapY = Math.min(0, diffY - (node1.getHeight() + node2.getHeight()) / 2);
		out.setLocation(overlapX, overlapY);
		return overlapX * overlapY;
	}

	/**
	 * Create a datastructure to efficiently find 
	 * the number of connection between two nodes.
	 * 
	 * @param allNetworks
	 */
	private void buildConnectivityMap(List<PlacementNetwork> allNetworks) {
		connectivityMap = new HashMap<PlacementNode, Map<PlacementNode, MutableInteger>>();
		for (int i = 0; i < allNetworks.size(); i++) {
			List<PlacementPort> currentPortList = allNetworks.get(i).getPortsOnNet();
			PlacementPort previousPlacementPort = null;
			for (int j = 0; j < currentPortList.size(); j++) {
				PlacementPort currentPlacementPort = currentPortList.get(j);
				if (previousPlacementPort != null) {
					incrementMap(previousPlacementPort.getPlacementNode(),
							currentPlacementPort.getPlacementNode());
					incrementMap(currentPlacementPort.getPlacementNode(),
							previousPlacementPort.getPlacementNode());
				}
				previousPlacementPort = currentPlacementPort;
			}
		}
	}	

	/**
	 * Method to build the connectivity map by adding a connection between two
	 * PlacementNodes. This method is usually called twice with the
	 * PlacementNodes in both orders because the mapping is not symmetric.
	 * 
	 * @param plNode1
	 *            the first PlacementNode.
	 * @param plNode2
	 *            the second PlacementNode.
	 */
	private void incrementMap(PlacementNode plNode1, PlacementNode plNode2) {
		Map<PlacementNode, MutableInteger> destMap = connectivityMap.get(plNode1);
		if (destMap == null)
			connectivityMap.put(plNode1,
					destMap = new HashMap<PlacementNode, MutableInteger>());
		MutableInteger mi = destMap.get(plNode2);
		if (mi == null)
			destMap.put(plNode2, mi = new MutableInteger(0));
		mi.increment();
	}

	// --- Threads : classes --------------------------------------------------	

	/**
	 * Classes to encapsulate multithreading, synchronization and 
	 * single steps of the Force-Directed algorithm.
	 *
	 *    class WorkerThread
	 *    class JoinThread
     */
	private Thread task[];
	private WorkerThread worker[];
	private CyclicBarrier barrier;

	private class WorkerThread implements Runnable {
		private int firstIndex;		// first index of node
		private int lastIndex;		// last index of node
		// debug
		public Debug dbg = new Debug();
		
		public WorkerThread(int firstNode, int lastNode) {
			this.firstIndex = firstNode;
			this.lastIndex = lastNode;
		}
		
		// algorithm
		public void run() {
			// phase1 : contract cells
			for (int i = 0; i < p.max_iterations && checkTimeout(); i++) {
				calculateForces(firstIndex, lastIndex);			// step1
				syncThreads();									// wait for all threads
			}
		}
	}

	private class JoinThread implements Runnable {
		// algorithm
		public void run() {
			count();
			moveCells( getMobilityFactor() );					// step2
		}
	
	}

	// --- Threads : private methods ------------------------------------------	

	/**
	 * Create and start all threads used in Force-Directed placement.
	 * 
	 */
	private void startThreads() {
		// inputs: nodesToPlace
		// outputs: task[], worker[]
		task = new Thread[numThreads];
		worker = new WorkerThread[numThreads];
		barrier = new CyclicBarrier(numThreads,new JoinThread());
		int first = 0;
		int delta = nodesToPlace.size()/numThreads;
		for (int i = 0; i < numThreads; i++) {
			if(i==numThreads-1) delta=nodesToPlace.size()-first;
			worker[i] = new WorkerThread(first,first+delta-1);
			task[i] = new Thread(worker[i]);
			task[i].start();
			first+=delta;
		}
	}

	/**
	 * Synchronize all threads used in Force-Directed placement.
	 * 
	 */	
	private void syncThreads() {
		try {
			barrier.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	}	

	/**
	 * Wait until all threads finished their work.
	 * 
	 */
	private void waitThreads() {
		for (Thread t : task)
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		if (printDebugInformation) {
			for (int i = 0; i < numThreads; i++) {
				worker[i].dbg.flush("=== THREAD-"+i+" ===========================");
				task[i] = null;
				worker[i] = null;
			}
			barrier = null;
		}
	}

	/**
	 * Timeout threads, if they work too long.
	 */
	private long timeoutStart;
	private long timeoutEnd;		// timeout for the complete algorithm, hard deadline
	private long timeoutLevel;		// timeout for current phase
	private void initTimeout(int phase) {
		switch(phase) {
			case 0: // swapNodes 
				timeoutStart = System.currentTimeMillis();
				timeoutEnd	  = timeoutStart + (maxRuntime*1000);
				timeoutLevel = timeoutStart + (long)(maxRuntime*1000*0.10); // phase1 10%
				break;
			case 1: // moveCells
				timeoutLevel = timeoutStart + (long)(maxRuntime*1000*0.50); // phase2 50%
				break;
			case 2: // resolveOverlap
				timeoutLevel = timeoutStart + (long)(maxRuntime*1000*0.95);
				break;
			case 3: // rotateNodes 
				timeoutLevel = timeoutStart + (long)(maxRuntime*1000*0.55);
				break;
			case 4: // fillEmptyBins
				timeoutLevel = timeoutStart + (long)(maxRuntime*1000*0.60);
				break;
			case 5: // shakeNodes
				timeoutLevel = timeoutEnd;
				break;
		}
	}
	/**
	 * Timeout threads in each phase, if they work too long.
	 * 
	 * @return true = go on, false = timed out
	 */
	private boolean checkTimeout() {
		return System.currentTimeMillis() < timeoutLevel;
	}

	//--- debugger ------------------------------------------------------------
	
	//globals
	private Debug dbg = null;

	//methods
	public void setDebugger(Debug dbg) {
		this.dbg = dbg;
	}
	
	private void iterativeFindOrientations(List<PlacementNode> nodesToRotate) {
		if( !checkTimeout() ) return;	// exit if no time is left
		do {							// else improve placement and rotate nodes
			mirrorGain = 0;
			findOrientations(nodesToRotate);
		} while( mirrorGain > 0 );			
	}

	/**
	 * Method to find a better orientation for all of the nodes at the bottom point.
	 * 4 options for Mirroring are considered for each PlacementNode including no change
	 * 
	 * @param allNodes a List of PlacementNodes that have location, but not ideal orientation.
	 * @return a Map assigning orientation to each of the PlacementNodes in the list.
	 */
	private void findOrientations(List<PlacementNode> nodesToRotate)
	{
		//check timeout
		if(!checkTimeout()) return;
		//
		double lengthNoMirror;
		double lengthXMirror;
		double lengthYMirror;
		double lengthXYMirror;
		for(int i = 0; i < nodesToRotate.size() && checkTimeout(); i++) {
			PlacementNode currentNode = nodesToRotate.get(i);
			
			List<PlacementNetwork> currentNetworks = getNetworks(currentNode);
			bbPartial.setBenchmarkValues(nodesToPlace, currentNetworks);
				
			LinkedList<PlacementNetwork> connectedNetworks = new LinkedList<PlacementNetwork>();
			LinkedList<Point2D.Double> srcPoints = new LinkedList<Point2D.Double>();
			for(int j = 0; j < currentNode.getPorts().size(); j++) {
				connectedNetworks.add(currentNode.getPorts().get(j).getPlacementNetwork());
				srcPoints.add(getAbsolutePositionOf(currentNode.getPorts().get(j)));
			}
			
			lengthNoMirror = bbPartial.compute();
			int currentRotation = currentNode.getPlacementOrientation().getAngle();
			boolean isXMirrored = currentNode.getPlacementOrientation().isXMirrored();
			boolean isYMirrored = currentNode.getPlacementOrientation().isYMirrored();
			currentNode.setOrientation(Orientation.fromJava(currentRotation, !isXMirrored, isYMirrored));
			lengthXMirror = bbPartial.compute();
			currentNode.setOrientation(Orientation.fromJava(currentRotation, isXMirrored, !isYMirrored));
			lengthYMirror = bbPartial.compute();
			currentNode.setOrientation(Orientation.fromJava(currentRotation, !isXMirrored, !isYMirrored));
			lengthXYMirror = bbPartial.compute();
			
			if((lengthNoMirror <= lengthXMirror) && (lengthNoMirror <= lengthYMirror) && (lengthNoMirror <= lengthXYMirror)) {
				currentNode.setOrientation(Orientation.fromJava(currentRotation, isXMirrored, isYMirrored));
			} else if ((lengthXMirror <= lengthNoMirror) && (lengthXMirror <= lengthYMirror) &&
					(lengthXMirror <= lengthXYMirror)) {
				currentNode.setOrientation(Orientation.fromJava(currentRotation, !isXMirrored, isYMirrored));
				mirrorGain += lengthNoMirror - lengthXMirror;
			} else if ((lengthYMirror <= lengthNoMirror) && (lengthYMirror <= lengthXMirror) &&
					(lengthYMirror <= lengthXYMirror)) {
				currentNode.setOrientation(Orientation.fromJava(currentRotation, isXMirrored, !isYMirrored));
				mirrorGain += lengthNoMirror - lengthYMirror;
			} else {
				mirrorGain += lengthNoMirror - lengthXYMirror;
			}
		}

	}
	
	private void fillEmptyBins(){
		//check timeout
		if(!checkTimeout()) return;
		//
		ArrayList<Point2D.Double> emptyBins = new ArrayList<Point2D.Double>();
		//find empty bins;
		for(int i = 0; i < cellCount.length; i++) {
			for(int j = 0; j < cellCount.length; j++){
				if(cellCount[i][j] == 0) {
					emptyBins.add(new Point2D.Double((i+0.5)*
							Math.sqrt(p.areaPerCell),(j+0.5)*Math.sqrt(p.areaPerCell)));
				}
			}
		}

		while(checkTimeout()) {
			int nodeNumber = (int)(Math.random()*nodesToPlace.size());
			int destNumber = (int)(Math.random()*emptyBins.size());
			
			List<PlacementNetwork> currentNetworks = getNetworks(nodesToPlace.get(nodeNumber));
			bbPartial.setBenchmarkValues(nodesToPlace, currentNetworks);
			
			double oldMetric = bbPartial.compute();
			PlacementNode currentNode = nodesToPlace.get(nodeNumber);
			
			//save old position
			double oldX = currentNode.getPlacementX();
			double oldY = currentNode.getPlacementY();
			
			currentNode.setPlacement(emptyBins.get(destNumber).x + Math.random()*Math.sqrt(p.areaPerCell),
					emptyBins.get(destNumber).y+ Math.random()*Math.sqrt(p.areaPerCell));
					
			double newMetric = bbPartial.compute();
			
			boolean overlap = false;
			for(int z = 0; z < nodesToPlace.size(); z++) {
				if(z!=nodeNumber && getOverlap(currentNode, nodesToPlace.get(z), new Point2D.Double(0,0))!= 0){
					overlap = true;
				}
			}			
			
			if(newMetric >= oldMetric || overlap) {
				currentNode.setPlacement(oldX, oldY);
			} 
		}
	}

	private void shakeNodes(){
		//check timeout
		if(!checkTimeout()) return;
		//
		double step;
		double oldX;
		double oldY;
		boolean badMove;
		double newMetric;
		double oldMetric;
		int randomDirection;
		int nodeNumber;
		boolean overlap;
		List<PlacementNetwork> currentNetworks;

		while(checkTimeout()) {
			nodeNumber = (int)(Math.random()*nodesToPlace.size());
			currentNetworks = getNetworks(nodesToPlace.get(nodeNumber));
			bbPartial.setBenchmarkValues(nodesToPlace, currentNetworks);
			oldMetric = bbPartial.compute();
			PlacementNode currentNode = nodesToPlace.get(nodeNumber);
			step = 1;
			badMove = false;
			randomDirection = (int)(Math.random()*8);
			
			while(step >= 1) {
			
				//save old position
				oldX = currentNode.getPlacementX();
				oldY = currentNode.getPlacementY();	
				
				switch(randomDirection) {
				case 0:
					currentNode.setPlacement(oldX + step, oldY);
				break;
				case 1:
					currentNode.setPlacement(oldX - step, oldY);
				break;
				case 2:
					currentNode.setPlacement(oldX, oldY + step);
				break;
				case 3:
					currentNode.setPlacement(oldX, oldY - step);
				break;
				case 4:
					currentNode.setPlacement(oldX + step, oldY + step);
				break;
				case 5:
					currentNode.setPlacement(oldX + step, oldY - step);
				break;
				case 6:
					currentNode.setPlacement(oldX - step, oldY + step);
				break;
				case 7:
					currentNode.setPlacement(oldX - step, oldY - step);
				break;
							
				}
				
				newMetric = bbPartial.compute();
				
				overlap = false;
				for(int z = 0; z < nodesToPlace.size(); z++) {
					if(z!=nodeNumber && getOverlap(currentNode,
							nodesToPlace.get(z), new Point2D.Double(0,0))!= 0){
						overlap = true;
					}
				}
				
				if(newMetric >= oldMetric || overlap) {
					badMove = true;
					currentNode.setPlacement(oldX, oldY);
					step = step/2;
				} else {
					if(!badMove) step = step * 2;
					oldMetric = newMetric;
				}
			}
		}
	}

	
	/**
	 * Method to find a better position for all of the nodes by swapping nodes.
	 */
	private void swapNodes() {
		//check timeout
		if(!checkTimeout()) return;
		//
		PlacementNode currentNode;
		PlacementNode swapNode;
		double oldMetric;
		double newMetric;
		double oldX1;
		double oldY1;
		double oldX2;
		double oldY2;
		List<PlacementNetwork> currentNetworks;
					
		for(int i=0; i<nodesToPlace.size() && checkTimeout(); i++) {
			currentNode = nodesToPlace.get(i);
			oldX1 = currentNode.getPlacementX();
			oldY1 = currentNode.getPlacementY();
			for(int j=i+1; j<nodesToPlace.size() && checkTimeout(); j++) {
				swapNode = nodesToPlace.get(j);
				currentNetworks = getNetworks(currentNode,swapNode);
				bbPartial.setBenchmarkValues(nodesToPlace, currentNetworks);
				
				
					oldMetric = bbPartial.compute();
					oldX2 = swapNode.getPlacementX();
					oldY2 = swapNode.getPlacementY();

					currentNode.setPlacement(oldX2, oldY2);	// swap
					swapNode.setPlacement(oldX1, oldY1);

					newMetric = bbPartial.compute();
					if(newMetric<oldMetric) continue;

					currentNode.setPlacement(oldX1, oldY1);	// restore old placement
					swapNode.setPlacement(oldX2, oldY2);						
			}
		}
	}

	//returns set of Networks that are connected to a node
	private List<PlacementNetwork> getNetworks(PlacementNode node) {
		//use of set to remove duplicates
		HashSet<PlacementNetwork> set = new HashSet<PlacementNetwork>();
		for(PlacementPort p :node.getPorts()) {
			if(p.getPlacementNetwork()!= null) {
				set.add(p.getPlacementNetwork());
			}
		}
		ArrayList<PlacementNetwork> result = new ArrayList<PlacementNetwork>();
		result.addAll(set);
		return result;
	}
	
	private List<PlacementNetwork> getNetworks(PlacementNode node1, PlacementNode node2) {
		//use of set to remove duplicates
		HashSet<PlacementNetwork> set = new HashSet<PlacementNetwork>();
		for(PlacementPort p :node1.getPorts()) {
			if(p.getPlacementNetwork()!= null) {
				set.add(p.getPlacementNetwork());
			}
		}
		for(PlacementPort p :node2.getPorts()) {
			if(p.getPlacementNetwork()!= null) {
				set.add(p.getPlacementNetwork());
			}
		}
		ArrayList<PlacementNetwork> result = new ArrayList<PlacementNetwork>();
		result.addAll(set);
		return result;
	}

	/**
	 * auxiliary function for getPositionOfPorts
	 * @param placementPort
	 * @return
	 */
	private Point2D.Double getAbsolutePositionOf(PlacementPort placementPort) {
		return new Point2D.Double(
				placementPort.getRotatedOffX() + placementPort.getPlacementNode().getPlacementX(),
				placementPort.getRotatedOffY() + placementPort.getPlacementNode().getPlacementY());
	}
}
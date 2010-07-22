/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarMaster.java
 * Written by: Christian Julg, Jonas Thedering (Team 1)
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
package com.sun.electric.tool.routing.experimentalAStar1;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.routing.RoutingFrame.RoutePoint;
import com.sun.electric.tool.routing.RoutingFrame.RouteWire;
import com.sun.electric.tool.routing.RoutingFrame.RoutingContact;
import com.sun.electric.tool.routing.RoutingFrame.RoutingGeometry;
import com.sun.electric.tool.routing.RoutingFrame.RoutingLayer;
import com.sun.electric.tool.routing.RoutingFrame.RoutingSegment;

/** 
 * Creates the jobs for the worker threads and processes the results
 * 
 * @author Jonas Thedering
 * @author Christian Jülg
 */
public class AStarMaster {

	private boolean DEBUG = false;

	// Displacement to map the given coordinates to the field array
	private final int dispX;
	private final int dispY;

	private ExecutorCompletionService<Net> netCompletionService;

	private List<Net> netList;
	private List<Net> unroutedNetList;
	private List<Net> activeNetList = new LinkedList<Net>();

	private List<ObjectPool<Node>> nodePools;
	private List<ObjectPool<Storage>> storagePools;

	private RoutingLayer[] metalLayers;
	private RoutingContact[] metalPins;

	private List<RoutingContact> allContacts;

	private final Map map;

	private int poolSize;

	private int totalNumPaths;
	
	private int threadCount;

	private long shutdownTime;

	private int unroutableNetCount = 0;
	private int unroutablePathCount = 0;

	public AStarMaster(ExecutorService service, Map map, List<ObjectPool<Node>> nodePools, List<ObjectPool<Storage>> storagePools, RoutingLayer[] metalLayers,
			RoutingContact[] metalPins, int threadCount, long shutDownTime) {

		netCompletionService = new ExecutorCompletionService<Net>(service);

		this.nodePools = nodePools;
		this.storagePools = storagePools;
		this.map = map;

		this.metalLayers = metalLayers;
		this.metalPins = metalPins;

		this.threadCount = threadCount;

		this.dispX = map.getDispX();
		this.dispY = map.getDispY();

		this.poolSize = nodePools.size();

		this.shutdownTime = shutDownTime;

		DEBUG &= AStarRoutingFrame.getInstance().isOutputEnabled();
	}

	private void run() {
		long startTime = 0;
		long masterTimeActiveStart = 0;
		long masterTimeWaitingStart = 0;
		long masterTimeActiveSum = 0;
		long masterTimeWaitingSum = 0;
		if (DEBUG) {
			long currentTime = System.currentTimeMillis();
			masterTimeActiveStart = currentTime;
			startTime = currentTime;
		}

		int invalidLength = 0;
		int invalidPathSum = 0;

		int completedNetCount = 0;
		int completedPathCount = 0;
		int invalidNetSum = 0;
		
		EndPointMarker marker = new EndPointMarker(map);
		marker.markStartAndFinish(netList);
		
		processUnroutables();

		// Schedule one net per thread at the start
		for (int i = 0; i < threadCount; ++i) {
			scheduleUnroutedNet();
		}
		
		boolean shutdown = false;

		// The "main loop", which waits for finishing workers and creates new jobs
		while (completedNetCount < netList.size() && !(activeNetList.isEmpty() && shutdown)) {
			try {
				if (DEBUG) {
					long currentTime = System.currentTimeMillis();
					masterTimeActiveSum += currentTime - masterTimeActiveStart;
					masterTimeWaitingStart = currentTime;
				}
				Future<Net> future = netCompletionService.take();

				Net net = future.get();

				if (DEBUG) {
					long currentTime = System.currentTimeMillis();
					masterTimeWaitingSum += currentTime - masterTimeWaitingStart; 
					masterTimeActiveStart = currentTime;
				}
				
				// if there are fewer Nets than threads left, 
				// remove a nodepool to free up memory
				while (netList.size() - completedNetCount < poolSize) {
					synchronized (nodePools) {
						if(DEBUG)
							System.out.println("Master: removed a nodepool!");
						nodePools.remove(0);
						storagePools.remove(0);
						poolSize--;
					}
				}
				
				List<Path> paths = net.getPaths();
				boolean invalidFound = false;
				boolean[] pathInvalid = new boolean[paths.size()];
				boolean unroutablePathFound = false;

				// Check all paths routed for validity
				for (int pathIdx = 0; pathIdx < paths.size(); pathIdx++) {
					Path path = paths.get(pathIdx);
					
					if (path.pathDone) {
						// this path was already entered into the map!
						continue;
					}
					
					// No route between the endpoints was found for this path
					if (path.nodesX == null || path.pathUnroutable) {
						if(DEBUG) {
							System.err.printf("Net \"%s\": Path routing failure! totalCost %d, Path will be left untouched.\n", path.segment.getNetName(), path.totalCost);
						}
						if (!unroutablePathFound){
							++unroutableNetCount;
							unroutablePathFound = true;
						}
						
						unroutablePathCount++;
						
						// ignore this path from now on!
						path.pathUnroutable = true;
						path.pathDone = true;
						net.pathDone[pathIdx] = true;

						continue;
					}

					for (int i = 0; i < path.nodesX.length; ++i) {
						int status = map.getStatus(path.nodesX[i], path.nodesY[i], path.nodesZ[i]);
						if (status != Map.CLEAR && status != net.getNetID()) {
							// Path is invalid because it's using blocked nodes
							invalidFound = true;
							pathInvalid[pathIdx] = true;
							break;
						}
					}
					// only invalid paths need to be recomputed
					if (!pathInvalid[pathIdx]) {
						net.pathDone[pathIdx] = true;
						path.pathDone = true;

						// The path is ok, so we set the new blockages
						map.setStatus(path.nodesX, path.nodesY, path.nodesZ, net.getNetID());
					}
				}
				
				shutdown = System.currentTimeMillis() > shutdownTime;
				
				if (invalidFound) {
					// There were invalid (but not unroutable) paths, so reroute them
					++invalidNetSum;
					
					if (DEBUG){
						int netInvalidLength =0;
						int invalidPathCount = 0;
						for (Path p: paths) {
							if (!p.pathDone && !p.pathUnroutable) {
								netInvalidLength += p.totalCost;
								invalidPathCount++;
							}
						}
						invalidPathSum += invalidPathCount;
						invalidLength += netInvalidLength;
						System.out.printf("AStarMaster: Net %3d contains %d invalid paths, rerouting it, invalid length: %d (net est.: %4d), invalid nets: %d, completed:%d/%d\n", net.getElectricNetID(), invalidPathCount, netInvalidLength, net.getLengthEstimate(), invalidNetSum,
								completedNetCount, netList.size());
					}
					
					if (!shutdown) {
						Goal goal = new Goal(net, map);

						AStarWorker worker = new AStarWorker(net, nodePools, storagePools, map, goal, shutdownTime);
						// Do it again
						netCompletionService.submit(worker);
					} else {
						// need to shutdown, so finish valid paths in this net
						for (Path path : paths) {
							if (path.pathDone && !path.pathUnroutable) {
								routeSegment(path);
							}
							++completedPathCount;
						}

						++completedNetCount;

						activeNetList.remove(net);

						if (DEBUG) {
							System.out.printf("AStarMaster: shutdown now, time after shutdown threshold:%dms (Net %3d is complete, completed nets:%3d/%d, completed paths:%3d/%d)\n", System.currentTimeMillis() - shutdownTime, net.getElectricNetID(), completedNetCount, netList.size(), completedPathCount, totalNumPaths);
						}
					}

				} else {
					// All paths were valid/unroutable, so finish this net and schedule the next
					for (Path path : paths) {
						if (path.pathDone && !path.pathUnroutable) {
							routeSegment(path);
						}
						++completedPathCount;
					}

					++completedNetCount;

					if (DEBUG)
						System.out.printf("AStarMaster: Net %3d is complete, completed nets:%3d/%d, completed paths:%3d/%d\n", net.getElectricNetID(), completedNetCount, netList.size(), completedPathCount, totalNumPaths);
					
					activeNetList.remove(net);
					
					if (!shutdown) {
						scheduleUnroutedNet();
					}
				}

			} catch (InterruptedException e) {
				System.err.printf("AStarMaster: Caught exception in Worker:\n%s\n", e.toString());
				e.getCause().printStackTrace();
				return;
			} catch (ExecutionException e) {
				System.err.printf("AStarMaster: Caught exception in Worker:\n%s\n", e.toString());
				e.getCause().printStackTrace();
				return;
			}
		}	
		
		assert activeNetList.isEmpty();

		if (DEBUG) {
			
			long currentTime = System.currentTimeMillis();
			System.out.printf("\nAStarMaster: Routing completed after %d ms, invalid/completed nets: %d/%d (%.1f %%), invalid/completed paths: %d/%d (%.1f %%)\n",
					currentTime - startTime, invalidNetSum, completedNetCount, (100f * invalidNetSum)
					/ (completedNetCount), invalidPathSum, completedPathCount, (100f * invalidPathSum)
					/ (completedPathCount));

			masterTimeActiveSum += currentTime - masterTimeActiveStart; 
			System.out.printf("AStarMaster: Master thread waiting time: %d, active time: %d (%.1f%%)\n", 
					masterTimeWaitingSum, masterTimeActiveSum, masterTimeActiveSum*100f / (currentTime - startTime));
		}
		
		if (DEBUG) {
			if (unroutableNetCount > 0)
				System.err.printf("Error: %d nets not routed, containing %d paths\n", unroutableNetCount, unroutablePathCount);
			evaluateRouting(invalidLength);
		}
	}
	
	/**
	 * looks at all Paths and marks all that were set pathUnroutable by EndpointMarker to pathDone, 
	 * so that they will be ignored by Worker
	 */
	private void processUnroutables() {
		for (Net net : netList) {

			List<Path> paths = net.getPaths();
			boolean unroutablePathFound = false;

			// Check all paths routed for validity
			for (int pathIdx = 0; pathIdx < paths.size(); pathIdx++) {
				Path path = paths.get(pathIdx);
					
				// DEBUG: this should not happen, pathDone should not be set before this loop
				 assert !path.pathDone : "processUnroutables(): pathDone was already set!";

				// No route between the endpoints was found for this path
				if (path.pathUnroutable) {
					if (DEBUG) {
						System.err.printf("Net \"%s\": Path unroutable, Path will be left untouched.\n", path.segment.getNetName());
					}
					if (!unroutablePathFound) {
						++unroutableNetCount;
						unroutablePathFound = true;
					}

					unroutablePathCount++;

					// ignore this path from now on!
					path.pathUnroutable = true;
					path.pathDone = true;
					net.pathDone[pathIdx] = true;
				}
			}
		}
	}

	/** Starts the execution of a net from the unrouted list, moving it to the active list */
	private void scheduleUnroutedNet() {
		if(unroutedNetList.isEmpty())
			return;
		
		long startTime = 0;
		if(DEBUG)
			startTime = System.nanoTime();
		
		// Determine the net with least overlap to the currently routed
		Net net = null;
		int minSum = Integer.MAX_VALUE;
		for(Net unroutedNet : unroutedNetList) {
			int sum = 0;
			for(Net activeNet : activeNetList) {
				sum += activeNet.getOverlapSum(unroutedNet);
			}
			if(sum < minSum) {
				minSum = sum;
				net = unroutedNet;
				
				if(minSum == 0)
					break;
			}
		}
		
		if(DEBUG) {
			if(minSum > 0)
				System.out.printf("AStarMaster: Net %d scheduled with overlap %d after %.3f ms\n", net.getElectricNetID(), minSum, (System.nanoTime()-startTime)/1e6f);
		}
		
		unroutedNetList.remove(net);
		activeNetList.add(net);
		
		Goal goal = new Goal(net, map);

		AStarWorker worker = new AStarWorker(net, nodePools, storagePools, map, goal, shutdownTime);
		netCompletionService.submit(worker);
	}

	/**
	 * Method to do routing
	 * 
	 * @param segmentsToRoute
	 *            a list of all routes that need to be made.
	 * @param allLayers
	 *            a list of all layers that can be used in routing.
	 * @param allNodes
	 *            a list of all nodes involved in the routing.
	 * @param allContacts
	 *            a list of all contacts that can be used in routing.
	 */
	protected void runRouting(Cell cell, List<RoutingSegment> segmentsToRoute, List<RoutingLayer> allLayers, List<RoutingContact> allContacts,
			List<RoutingGeometry> blockages) {

		netList = new ArrayList<Net>();

		HashMap<Integer, Net> netMap = new HashMap<Integer, Net>();
		
		this.totalNumPaths = segmentsToRoute.size();

		// Create nets to combine the given segments
		for (RoutingSegment rs : segmentsToRoute) {
			int netID = rs.getNetID();
			Net net = netMap.get(netID);

			if (net == null) {
				net = new Net(rs.getNetID());
				netMap.put(rs.getNetID(), net);

				netList.add(net);
			}

			net.getPaths().add(new Path(rs, dispX, dispY, map.getScalingFactor()));
		}

		sortNets();
		
		unroutedNetList = new LinkedList<Net>(netList);

		this.allContacts = allContacts;

		run();
	}

	/** Calculate some statistic information about the routing process */
	private void evaluateRouting(int invalidLength) {
		int sum = 0;
		int estimate = 0;
		int netSum, netEstimate;
		int diff; //difference between optimum and actual cost
		for (Net net: netList) {
			netSum = 0;
			for (Path p: net.getPaths()) {
				if (p.pathDone && !p.pathUnroutable) {
					netSum += p.totalCost;
				}
			}
			sum += netSum;
			netEstimate = net.getRoutableLengthEstimate();
			estimate += netEstimate;
		}
		diff = sum-estimate;
		System.out.printf("total routing cost: %d, estimated: %d, diff: %d (%.02f%%), invalid length: %d (%.02f%%)\n", sum, estimate, diff, diff*100f/estimate, invalidLength, invalidLength*100f/sum);
	}

	/** Sort the nets by length to improve invalid routing costs */
	private void sortNets() {
		long start = 0;
		if (DEBUG) {
			start = System.currentTimeMillis();
		}

		Collections.sort(netList, new Comparator<Net>() {
			public int compare(Net n1, Net n2) {
				int len1 = n1.getLengthEstimate();
				int len2 = n2.getLengthEstimate();

				return (len1 - len2);
			}
		});

		if (DEBUG) {
			System.out.printf("Master: sortNets() took %d ms for %d nets\n", System.currentTimeMillis() - start, netList.size());
		}
	}

	/** Finally insert the path into the Electric data structures */
	private void routeSegment(Path path) {
		/*
		long start;
		if (DEBUG) {
			start = System.currentTimeMillis();
		}
		*/

		RoutingSegment segment = path.segment;
		int[] nodesX = path.nodesX;
		int[] nodesY = path.nodesY;
		int[] nodesZ = path.nodesZ;

		Point2D startLocation = segment.getStartEnd().getLocation();

		Point2D finishLocation = segment.getFinishEnd().getLocation();

		// Now that we have the result, we convert the path to wires and
		// wire ends and add them to the segment

		// Special handling for the start point
		RoutePoint previous = new RoutePoint(RoutingContact.STARTPOINT, startLocation, 0);
		segment.addWireEnd(previous);

		RoutingLayer layer = metalLayers[nodesZ[0]];
		RoutingContact contact = metalPins[nodesZ[0]];

		double scalingFactor = map.getScalingFactor();
		
		// Determine quadrant of start / finish point
		boolean startRight = path.startRight;
		boolean startAbove = path.startAbove;
		boolean finishRight = path.finishRight;
		boolean finishAbove = path.finishAbove;
		
		// A complicated scheme follows that carefully handles routing at the endpoints
		// special handling for all Paths shorter than 4 needed to avoid Notch errors
		if(nodesX.length < 4) {
			// Short nets need special care to avoid notch errors
			assert nodesX.length >= 1 : nodesX.length;
			
			//0: no connection, 1: L, 2: horizontal Z, 3: vertical Z, 4: M
			int connectionMode;
			
			if(nodesX.length == 1) {
				// Diagonal?
				if(startRight != finishRight && startAbove != finishAbove) {
					// Make Z connection
					connectionMode = 2;
				}
				else {
					// Make L connection
					connectionMode = 1;
				}
			}
			else if(nodesX.length == 2) {
				boolean horizontal = nodesY[0] == nodesY[1];
				boolean vertical = nodesX[0] == nodesX[1];
				boolean startInside = (horizontal && ((nodesX[0] < nodesX[1]) == startRight))
					|| (vertical && ((nodesY[0] < nodesY[1]) == startAbove));
				boolean finishInside = (horizontal && ((nodesX[0] > nodesX[1]) == finishRight))
				|| (vertical && ((nodesY[0] > nodesY[1]) == finishAbove));
				boolean sameSide = (vertical && (startRight == finishRight))
					|| (horizontal && (startAbove == finishAbove));
				
				if(startInside && finishInside || sameSide) {
					// Make L connection
					connectionMode = 1;
				}
				else {
					// Make Z connection
					if(horizontal)
						connectionMode = 2;
					else
						connectionMode = 3;
				}
			}
			else {
				assert nodesX.length == 3 : nodesX.length;
				if(nodesX[0] == nodesX[2])
					// Make Z connection
					connectionMode = 3;
				else if(nodesY[0] == nodesY[2])
					// Make Z connection
					connectionMode = 2;
				else {
					// Not straight
					int quadrantStartX = (int)((startLocation.getX() + dispX) / (scalingFactor / 2));
					int quadrantStartY = (int)((startLocation.getY() + dispY) / (scalingFactor / 2));
					int quadrantFinishX = (int)((finishLocation.getX() + dispX) / (scalingFactor / 2));
					int quadrantFinishY = (int)((finishLocation.getY() + dispY) / (scalingFactor / 2));
					double middleX = getUnscaledCoordinate(nodesX[1], dispX, scalingFactor);
					double middleY = getUnscaledCoordinate(nodesY[1], dispY, scalingFactor);
					if(Math.abs(quadrantStartX - quadrantFinishX) <= 1
						|| Math.abs(quadrantStartY - quadrantFinishY) <= 1) {
						// Make L connection
						connectionMode = 1;
					}
					else if(Math.abs(startLocation.getX() - middleX) <= scalingFactor
						|| Math.abs(finishLocation.getX() - middleX) <= scalingFactor) {
						// Make Z connection
						connectionMode = 3;
					}
					else if(Math.abs(startLocation.getY() - middleY) <= scalingFactor
						|| Math.abs(finishLocation.getY() - middleY) <= scalingFactor) {
						// Make Z connection
						connectionMode = 2;
					}
					else {
						// Make M connection
						connectionMode = 4;
					}
				}
			}
			
			if(connectionMode == 1) {
				// Make L connection
				RoutePoint point = new RoutePoint(contact, new Point2D.Double(startLocation.getX(), finishLocation.getY()), 0);
				segment.addWireEnd(point);
	
				RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
				segment.addWire(wire);
	
				previous = point;
			}
			else if(connectionMode == 2) {
				// Make horizontal Z connection
				double pointY = getUnscaledCoordinate((nodesX.length == 3 ? nodesY[1] : nodesY[0]), dispY, scalingFactor);
				{
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(startLocation.getX(), pointY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
				{
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(finishLocation.getX(), pointY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
			}
			else if(connectionMode == 3) {
				// Make vertical Z connection
				double pointX = getUnscaledCoordinate((nodesX.length == 3 ? nodesX[1] : nodesX[0]), dispX, scalingFactor);
				{
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(pointX, startLocation.getY()), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
				{
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(pointX, finishLocation.getY()), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
			}
			else if(connectionMode == 4) {
				// Make M connection
				assert nodesX.length == 3;
				double pointX = getUnscaledCoordinate(nodesX[1], dispX, scalingFactor);
				double pointY = getUnscaledCoordinate(nodesY[1], dispY, scalingFactor);
				{
					RoutePoint point;
					if(nodesX[0] == nodesX[1])
						point = new RoutePoint(contact, new Point2D.Double(pointX, startLocation.getY()), 0);
					else
						point = new RoutePoint(contact, new Point2D.Double(startLocation.getX(), pointY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
				{
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(pointX, pointY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
				{
					RoutePoint point;
					if(nodesX[2] == nodesX[1])
						point = new RoutePoint(contact, new Point2D.Double(pointX, finishLocation.getY()), 0);
					else
						point = new RoutePoint(contact, new Point2D.Double(finishLocation.getX(), pointY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
			}
		}
		else { // nodesX.length >= 4
			int startOuterX = startRight ? nodesX[0]-1 : nodesX[0]+1;
			int startInnerX = startRight ? nodesX[0]+1 : nodesX[0]-1;
			int startOuterY = startAbove ? nodesY[0]-1 : nodesY[0]+1;
			int startInnerY = startAbove ? nodesY[0]+1 : nodesY[0]-1;
			int finishOuterX = finishRight ? nodesX[nodesX.length-1]-1 : nodesX[nodesX.length-1]+1;
			int finishInnerX = finishRight ? nodesX[nodesX.length-1]+1 : nodesX[nodesX.length-1]-1;
			int finishOuterY = finishAbove ? nodesY[nodesX.length-1]-1 : nodesY[nodesX.length-1]+1;
			int finishInnerY = finishAbove ? nodesY[nodesX.length-1]+1 : nodesY[nodesX.length-1]-1;
			
			double startGridX = getUnscaledCoordinate(nodesX[0], dispX, scalingFactor);
			double startGridY = getUnscaledCoordinate(nodesY[0], dispY, scalingFactor);
			double startInnerGridY = getUnscaledCoordinate(startInnerY, dispY, scalingFactor);
			double finishGridX = getUnscaledCoordinate(nodesX[nodesX.length-1], dispX, scalingFactor);
			double finishGridY = getUnscaledCoordinate(nodesY[nodesX.length-1], dispY, scalingFactor);
			double finishInnerGridY = getUnscaledCoordinate(finishInnerY, dispY, scalingFactor);
			
			if(nodesX[1] == startOuterX) {
				// Vertical or via?
				if(nodesX[2] == nodesX[1]) {
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(startLocation.getX(), startGridY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
			}
			else if(nodesY[1] == startOuterY) {
				{
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(startLocation.getX(), startGridY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
				
				// Horizontal or via?
				if(nodesY[2] == nodesY[1]) {
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(startGridX, startGridY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
			}
			else if(nodesX[1] == startInnerX) {
				if(nodesY[2] == startInnerY) {
					if(nodesY[3] != nodesY[2]) {
						RoutePoint point = new RoutePoint(contact, new Point2D.Double(startLocation.getX(), startInnerGridY), 0);
						segment.addWireEnd(point);
			
						RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
						segment.addWire(wire);
			
						previous = point;
					}
				}
				// Vertical or via?
				else if(nodesX[2] == nodesX[1]) {
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(startLocation.getX(), startGridY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
			}
			else if (nodesY[1] == startInnerY) {
				// Vertical or via?
				if(nodesX[2] == nodesX[1]) {
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(startLocation.getX(), startInnerGridY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
			}
			else {
				assert nodesZ[1] != nodesZ[0];
				{
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(startLocation.getX(), startGridY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
				{
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(startGridX, startGridY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
			}
			
			// Iterate over all intermediate positions
			for (int i = 1; i < nodesX.length - 1; ++i) {
				// look out for layer changes
				if (nodesZ[i - 1] == nodesZ[i]) {
	
					if (i > 1 && i < (nodesX.length - 2)) {
	
						// If this is part of a long line, don't create a redundant
						// route point
						int x = nodesX[i];
						if (x == nodesX[i - 1] && x == nodesX[i + 1])
							continue;
	
						int y = nodesY[i];
						if (y == nodesY[i - 1] && y == nodesY[i + 1])
							continue;
					}
	
					// connection is on same layer
					contact = metalPins[nodesZ[i]];
				} else {
					contact = getVia(metalLayers[nodesZ[i - 1]], metalLayers[nodesZ[i]]);
				}

				 // SCALE!
				double pointX = getUnscaledCoordinate(nodesX[i], dispX, scalingFactor);
				double pointY = getUnscaledCoordinate(nodesY[i], dispY, scalingFactor);
				if(i == 1) {
					if(nodesX[1] == startOuterX) {
						// Horizontal?
						if(nodesX[2] != nodesX[1]) {
							pointX = startLocation.getX();
						}
					}
					else if(nodesY[1] == startOuterY) {
						// Vertical?
						if(nodesY[2] != nodesY[1]) {
							pointY = startGridY;
						}
					}
					else if(nodesX[1] == startInnerX) {
						if(nodesY[2] == startInnerY) {
							// Vertical?
							if(nodesY[3] != nodesY[2]) {
								pointY = startInnerGridY;
							}
							else {
								pointX = startLocation.getX();
								pointY = startInnerGridY;
							}
						}
						// Horizontal?
						else if(nodesX[2] != nodesX[1]) {
							pointX = startLocation.getX();
						}
					}
					else if (nodesY[1] == startInnerY) {
						// Horizontal?
						if(nodesX[2] != nodesX[1]) {
							pointX = startLocation.getX();
						}
					}
				}
				else if(i == (nodesX.length - 2)) {
					if(nodesX[nodesX.length-2] == finishOuterX) {
						// Horizontal?
						if(nodesX[nodesX.length-3] != nodesX[nodesX.length-2]) {
							pointX = finishLocation.getX();
						}
					}
					else if(nodesY[nodesX.length-2] == finishOuterY) {
						// Vertical?
						if(nodesY[nodesX.length-3] != nodesY[nodesX.length-2]) {
							pointY = finishGridY;
						}
					}
					else if(nodesX[nodesX.length-2] == finishInnerX) {
						if(nodesY[nodesX.length-3] == finishInnerY) {
							// Vertical?
							if(nodesY[nodesX.length-4] != nodesY[nodesX.length-3]) {
								pointY = finishInnerGridY;
							}
							else {
								pointX = finishLocation.getX();
								pointY = finishInnerGridY;
							}
						}
						// Horizontal?
						else if(nodesX[nodesX.length-3] != nodesX[nodesX.length-2]) {
							pointX = finishLocation.getX();
						}
					}
					else if (nodesY[nodesX.length-2] == finishInnerY) {
						// Horizontal?
						if(nodesX[nodesX.length-3] != nodesX[nodesX.length-2]) {
							pointX = finishLocation.getX();
						}
					}
				}
				RoutePoint point = new RoutePoint(contact, new Point2D.Double(pointX, pointY), 0);
				segment.addWireEnd(point);
	
				RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
				segment.addWire(wire);
	
				layer = metalLayers[nodesZ[i]];
	
				previous = point;
			}
			
			layer = metalLayers[nodesZ[nodesX.length-1]];
			contact = metalPins[nodesZ[nodesX.length-1]];
			
			// Special handling for the finish point
			if(nodesX[nodesX.length-2] == finishOuterX) {
				// Vertical or via?
				if(nodesX[nodesX.length-3] == nodesX[nodesX.length-2]) {
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(finishLocation.getX(), finishGridY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
			}
			else if(nodesY[nodesX.length-2] == finishOuterY) {
				// Horizontal or via?
				if(nodesY[nodesX.length-3] == nodesY[nodesX.length-2]) {
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(finishGridX, finishGridY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
				
				{
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(finishLocation.getX(), finishGridY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
			}
			else if(nodesX[nodesX.length-2] == finishInnerX) {
				if(nodesY[nodesX.length-3] == finishInnerY) {
					if(nodesY[nodesX.length-4] != nodesY[nodesX.length-3]) {
						RoutePoint point = new RoutePoint(contact, new Point2D.Double(finishLocation.getX(), finishInnerGridY), 0);
						segment.addWireEnd(point);
			
						RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
						segment.addWire(wire);
			
						previous = point;
					}
				}
				// Vertical or via?
				else if(nodesX[nodesX.length-3] == nodesX[nodesX.length-2]) {
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(finishLocation.getX(), finishGridY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
			}
			else if (nodesY[nodesX.length-2] == finishInnerY) {
				// Vertical or via?
				if(nodesX[nodesX.length-3] == nodesX[nodesX.length-2]) {
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(finishLocation.getX(), finishInnerGridY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
			}
			else {
				assert nodesZ[nodesX.length-2] != nodesZ[nodesX.length-1];
				{
					RoutePoint point = new RoutePoint(getVia(metalLayers[nodesZ[nodesX.length-2]], metalLayers[nodesZ[nodesX.length-1]]), new Point2D.Double(finishGridX, finishGridY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(metalLayers[nodesZ[nodesX.length-2]], previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
				{
					RoutePoint point = new RoutePoint(contact, new Point2D.Double(finishLocation.getX(), finishGridY), 0);
					segment.addWireEnd(point);
		
					RouteWire wire = new RouteWire(layer, previous, point, layer.getMinWidth());
					segment.addWire(wire);
		
					previous = point;
				}
			}
		}

		RoutePoint finishPoint = new RoutePoint(RoutingContact.FINISHPOINT, finishLocation, 0);
		segment.addWireEnd(finishPoint);

		RouteWire wire = new RouteWire(layer, previous, finishPoint, layer.getMinWidth());
		segment.addWire(wire);

		/*
		if (DEBUG) {
			System.out.printf("Master: routeSegment(Path %s) took %d ms\n", path.segment.getNetName(), System.currentTimeMillis() - start);
		}
		*/
	}

	/**
	 * @return Coordinate inversely scaled, corrected by dispX or dispY and shifted to the middle of the "grid"
	 */
	private double getUnscaledCoordinate(int scaledCoord, int disp, double scalingFactor) {
		double shift = scalingFactor / 2d;

		return (scaledCoord*scalingFactor) - disp + shift;
	}

	/** @return The routing contact to route between the given layers */
	private RoutingContact getVia(RoutingLayer l1, RoutingLayer l2) {
		for (RoutingContact rc : allContacts) {
			if ((rc.getFirstLayer().equals(l1) && rc.getSecondLayer().equals(l2)) || (rc.getFirstLayer().equals(l2) && rc.getSecondLayer().equals(l1))) {
				return rc;
			}
		}
		return null;
	}
}

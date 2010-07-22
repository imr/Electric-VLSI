/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarRoutingFrame.java
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
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.electric.database.hierarchy.Cell;

/**
 * Integrates the routing algorithm into the Electric framework
 * 
 * @author Christian Jülg
 * @author Jonas Thedering
 */
public class AStarRoutingFrame extends BenchmarkRouter {
	
	private boolean DEBUG = false;
	private boolean PERFORMANCE = true;

	private boolean outputEnabled = false;

	// cleanup time between worker interruption and return from runRouting, in ms
	private static final long CLEANUP_TIME = 250;

	private static AStarRoutingFrame instance;
	
	// will be set in runRouting() according to Electric Preferences, 
	//  or config file if available
	private int threadCount = 1;
	private int timeout = Integer.MAX_VALUE;

	private ExecutorService service;

	private Map map;
	private List<ObjectPool<Node>> nodePools;
	private List<ObjectPool<Storage>> storagePools;

	private RoutingLayer[] metalLayers;
	private RoutingContact[] metalPins;
	
	//count and numMax could be different, if metalNumbers are not contiguous 1,2,...,n
	private int metalLayerCount;
	// use nummax instead count to be safe
	private int metalLayerNumMax;

	private double scalingFactor;
	
	public AStarRoutingFrame(){
		super();
		instance = this;
	}

	public String getAlgorithmName() {
		return "A* - 1";
	}

	/**
	 * Method to do Routing (overridden by actual Routing algorithms).
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
	protected void runRouting(Cell cell, List<RoutingSegment> segmentsToRoute, List<RoutingLayer> allLayers,
			List<RoutingContact> allContacts, List<RoutingGeometry> blockages) {
		
		if (segmentsToRoute.isEmpty()){
			return;
		}

		long startTime = 0;
		if (PERFORMANCE) {
			startTime = System.currentTimeMillis();
		}
		
		this.threadCount = numThreads.getIntValue();
		this.timeout = maxRuntime.getIntValue();
		this.outputEnabled = enableOutput.getBooleanValue();

		PERFORMANCE &= this.isOutputEnabled(); 
		DEBUG &= this.isOutputEnabled(); 

		long shutdownTime = System.currentTimeMillis() + (timeout * 1000) - CLEANUP_TIME; 

		service = Executors.newFixedThreadPool(threadCount);

		// 1 nodepool per thread
		nodePools = new ArrayList<ObjectPool<Node>>(threadCount); // fixed size
		storagePools = new ArrayList<ObjectPool<Storage>>(threadCount); // fixed size
		for (int i = 0; i < threadCount; i++) {
			nodePools.add(new ObjectPool<Node>(Node.class));
			storagePools.add(new ObjectPool<Storage>(Storage.class));
		}

		if (DEBUG) {
			CellPrinter.printContacts(allContacts);
			CellPrinter.printLayers(allLayers);
			CellPrinter.printChipStatistics(cell, segmentsToRoute, allLayers, allContacts, blockages);
		}

		processLayers(allLayers);
		processSpacing(allContacts);

		map = findBoundingBox(blockages, cell, segmentsToRoute);

		// do this in worker threads
		addBlockagesToMap(blockages);

		AStarMaster master = new AStarMaster(service, map, nodePools, storagePools, metalLayers, metalPins, threadCount, shutdownTime);
		master.runRouting(cell, segmentsToRoute, allLayers, allContacts, blockages);

		if (PERFORMANCE) {
			long totalTime = System.currentTimeMillis() - startTime;
			System.out.println("AStarRouting Settings: threadCount:" + threadCount + ", timeout:" + timeout);
			System.out.printf("AStarRouting total time: %d ms\n", totalTime);
		}
		
		//cleanup
		map = null;
		nodePools = null;
		storagePools = null;
		metalLayers = null;
		metalPins = null;
	}

	/** Calculates the grid granularity based on minimum wire widths and spacing rules */
	private void processSpacing(List<RoutingContact> allContacts) {
		scalingFactor = 0d;

		// check minWidths
		for (RoutingLayer rl : metalLayers) {				
				double layerMin = rl.getMinWidth();
				scalingFactor = Math.max(scalingFactor, layerMin);
				
				if (DEBUG) {
					double h = rl.getPin().getDefHeight();
					double w = rl.getPin().getDefWidth();
					double viaSpacing = rl.getPin().getViaSpacing();
					System.out.printf("Layer %d: minWireWidth is %f, defHeight=%f, defWidth=%f, viaSpacing=%f\n", rl.getMetalNumber(), rl.getMinWidth(), h, w, viaSpacing);
				} 
		}
		
		// Use double the minimum width to ensure the needed spacing between wires
		scalingFactor *= 2;
		
		//check viaSpacings
		for (RoutingContact rc : allContacts){
			double contactSpacing = rc.getViaSpacing();
			scalingFactor = Math.max(scalingFactor, contactSpacing);
			
			if (DEBUG) {
				double h = rc.getDefHeight();
				double w = rc.getDefWidth();
				double viaSpacing = rc.getViaSpacing();
				System.out.printf("Contact %d-%d: defHeight=%f, defWidth=%f, viaSpacing=%f\n", rc.getFirstLayer().getMetalNumber(), rc.getSecondLayer().getMetalNumber(), h, w, viaSpacing);
				
				for (RoutingGeometry geo : rc.getGeometry()){
					//TODO: what does this geometry mean? occurs at every via! 
					h = geo.getBounds().getHeight();
					w = geo.getBounds().getWidth();
					double minX = geo.getBounds().getMinX();
					double maxX = geo.getBounds().getMaxX();
					double minY = geo.getBounds().getMinY();
					double maxY = geo.getBounds().getMaxY();
					int id = geo.getNetID();
					System.out.printf("Contact %d-%d has RoutingGeometry on Layer %d, netID %d: defHeight=%f, defWidth=%f, minX=%f, maxX=%f, minY=%f, maxY=%f\n", rc.getFirstLayer().getMetalNumber(), rc.getSecondLayer().getMetalNumber(), geo.getLayer().getMetalNumber(), id, h, w, minX, maxX, minY, maxY);
				}
				
			}
		}
		
		//scaling factor should never be below 1, would be too memory expensive
		scalingFactor = Math.max(scalingFactor, 1d);
	}

	/** Collects the layers and pins needed to create RoutePoints/RouteWires */
	private void processLayers(List<RoutingLayer> allLayers) {
		metalLayerCount = 0;
		metalLayerNumMax = 0;
		
		int invalidLayerMin = Integer.MAX_VALUE;
		boolean invalidLayerFound = false;
		
		for (int i = 0; i < allLayers.size(); i++) {
			RoutingLayer rl = allLayers.get(i);

			if (rl.isMetal()) {
				if (rl.getMinWidth() < 0.001d) {
					// this layer is not valid!
					invalidLayerFound = true;
					invalidLayerMin = Math.min(invalidLayerMin, rl.getMetalNumber());
					continue;
				}
				if (invalidLayerFound && rl.getMetalNumber() > invalidLayerMin) {
					// assumption: when there are layers "beyond" invalid layers, they are also invalid
					continue;
				}
				 
				metalLayerCount++;
				metalLayerNumMax = Math.max(metalLayerNumMax, rl.getMetalNumber());
			}
		}

		if (DEBUG)
			System.out.println("metalLayers: " + metalLayerCount + ", maxMetalLayer: " + metalLayerNumMax + ", invalidLayerMin: "+invalidLayerMin);
		
		metalLayers = new RoutingLayer[metalLayerNumMax];
		metalPins = new RoutingContact[metalLayerNumMax];
		
		for (int i = 0; i < allLayers.size(); i++) {
			RoutingLayer rl = allLayers.get(i);

			if (rl.isMetal()) {
				int num = rl.getMetalNumber();
				
				if (invalidLayerFound && num >= invalidLayerMin) {
					//invalid Layer
					continue;
				}
				metalLayers[num-1] = rl;
				metalPins[num-1] = rl.getPin();
			}
		}
	}

	/**
	 * finds the bounding box and returns a Map with the parameters found
	 * 
	 * @param blockages
	 * @param cell
	 * @param segmentsToRoute
	 * @return
	 */
	private Map findBoundingBox(List<RoutingGeometry> blockages, Cell cell, List<RoutingSegment> segmentsToRoute) {

		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

		// temp, will be rounded downwards
		double dMinX = minX, dMinY = minY;
		// temp, will be rounded upwards
		double dMaxX = maxX, dMaxY = maxY;

		Rectangle2D rec;
		// find min/max x/y of blockages
		for (RoutingGeometry geo : blockages) {
			rec = geo.getBounds();
			dMinX = Math.min(rec.getMinX(), dMinX);
			dMinY = Math.min(rec.getMinX(), dMinY);
			dMaxX = Math.max(rec.getMaxX(), dMaxX);
			dMaxY = Math.max(rec.getMaxY(), dMaxY);
		}

		// find min/max x/y of cell elements
		// necessary to avoid arrayindexoutofboundsexceptions on repeated
		// routing calls on the same Cell
		rec = cell.getBounds();
		dMinX = Math.min(rec.getMinX(), dMinX);
		dMinY = Math.min(rec.getMinY(), dMinY);
		dMaxX = Math.max(rec.getMaxX(), dMaxX);
		dMaxY = Math.max(rec.getMaxY(), dMaxY);

		// find min/max x/y of segments start/finish
		Point2D endPoint;
		for (RoutingSegment rs : segmentsToRoute) {
			endPoint = rs.getStartEnd().getLocation();
			dMinX = Math.min(endPoint.getX(), dMinX);
			dMinY = Math.min(endPoint.getY(), dMinY);
			dMaxX = Math.max(endPoint.getX(), dMaxX);
			dMaxY = Math.max(endPoint.getY(), dMaxY);

			endPoint = rs.getFinishEnd().getLocation();
			dMinX = Math.min(endPoint.getX(), dMinX);
			dMinY = Math.min(endPoint.getY(), dMinY);
			dMaxX = Math.max(endPoint.getX(), dMaxX);
			dMaxY = Math.max(endPoint.getY(), dMaxY);
		}

		// Round to minWidth so that the grid in Electric matches ours
		minX = (int) (Math.floor(dMinX / scalingFactor) * scalingFactor);
		minY = (int) (Math.floor(dMinY / scalingFactor) * scalingFactor);
		maxX = (int) (Math.ceil(dMaxX / scalingFactor) * scalingFactor);
		maxY = (int) (Math.ceil(dMaxY / scalingFactor) * scalingFactor);

		int dispX, dispY;
		int width, height;

		// this margin allows bypassing blockages on the bounding box of the Cell
		//   could be calculated depending on the number of segments to route instead of fixed
		int numOfSafetyPoints = 5;
		int padding = (int) (numOfSafetyPoints*scalingFactor); 
		// +1 to account for field 0, times two because we add on both sides
		int margin = 2 * padding + 1;
		width = maxX - minX + margin;
		height = maxY - minY + margin;
		dispX = -minX + padding;
		dispY = -minY + padding;

		if (DEBUG) {
			System.out.printf("minx: %d, miny: %d, maxx: %d, maxy: %d\n", minX, minY, maxX, maxY);
			System.out.printf("dispx: %d, dispy: %d, width: %d, height: %d\n", dispX, dispY, width, height);
		}

		return new Map(scalingFactor, width, height, metalLayerNumMax, dispX, dispY);
	}

	public static AStarRoutingFrame getInstance(){
		return instance;
	}
	
	public boolean isOutputEnabled(){
		return outputEnabled;
	}

	/**
	 * add blockages to Map
	 * 
	 * NOTE: usually there are no blockages
	 * 
	 * @param blockages
	 */
	private void addBlockagesToMap(List<RoutingGeometry> blockages) {

		CountDownLatch latch = new CountDownLatch(blockages.size());

		for (RoutingGeometry block : blockages) {

			// each blockage is on exactly one layer
			AStarBlockageWorker worker = new AStarBlockageWorker(map, block, latch);
			service.execute(worker);
		}

		try {
			latch.await();
		} catch (InterruptedException e) {
			System.out.println("addBlockages(): Interrupted: " + e);
		}
	}
}

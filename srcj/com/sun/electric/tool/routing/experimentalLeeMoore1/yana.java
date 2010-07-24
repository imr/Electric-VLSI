/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Yana.java
 * Written by: Andreas Uebelhoer, Alexander Bieles, Emre Selegin (Team 6)
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
package com.sun.electric.tool.routing.experimentalLeeMoore1;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.Job;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.UserInterface;

import java.util.HashMap;
import java.util.List;

import com.sun.electric.tool.routing.experimentalLeeMoore1.LeeMoore.RoutingArray;
import com.sun.electric.tool.routing.experimentalLeeMoore1.LeeMoore.Tupel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;


public class yana extends BenchmarkRouter {

    protected final boolean output = enableOutput.getBooleanValue();
    
	/*
     * GUI
     */
    private static int progressMax = 0;
    private static int progress = 0;
    private static UserInterface gui;

    /*
     * Parameters
     */
//    public RoutingParameter maxThreadsParameter = new RoutingParameter("threads", "Number of Threads to use:", 4);
    public static int maxThreads;
    public RoutingParameter numPartitionsParameter = new RoutingParameter("partitions", "Number of partitions to use:", Math.max(26,Math.max(Runtime.getRuntime().availableProcessors(),numThreads.getIntValue())));
    //public RoutingParameter numPartitionsParameter = new RoutingParameter("partitions", "Number of partitions to use:", 5);
    public static int numPartitions;
    public RoutingParameter regionDivideMethodParameter = new RoutingParameter("region-generation-method", "Method used to devide the grid into regions:", 1);
    public static int regionDivideMethod;
    //1=optimal balanced in x and y direction
    //2=simple stripes
    //3=adapted so that regions have the same ratio than the grid dimensions
    public RoutingParameter maxLayerUseParameter=new RoutingParameter("maxLayerUse", "Restrict number of layers to use", 5);
    public static int maxLayerUse;
    public RoutingParameter minimumRegionBorderLengthParameter = new RoutingParameter("minimumRegionBorderLength", "Minumum length the regions must have:", 20);
    public static int minimumRegionBorderLength;
    //public RoutingParameter widthWhereJustOneWireIsPermittedParameter = new RoutingParameter("widthWhereJustOneWireIsPermitted", "Wire-thickness + 2*spacing-length to next wire", 9);
    public static int distanceBetweenWires; //should be = wirethickness + 2*requiredspacing
    //if there are to much regions, so that this constraint cant be held, than the numpartitions will be reduced until this constraint is fulfilled
    /*
     * Others
     */
    public static CyclicBarrier barrierRouting;
    public static CyclicBarrier barrierWiring;
    public static ConcurrentHashMap<Integer, Boolean> unroutedNets=new ConcurrentHashMap<Integer, Boolean>();

    /**
     * Method to return the name of this routing algorithm.
     * @return the name of this routing algorithm.
     */
    @Override
    public String getAlgorithmName() {
        return "Lee/Moore - 1";
    }

    /**
     * Method to return a list of parameters for this routing algorithm.
     * @return a list of parameters for this routing algorithm.
     */
    @Override
    public List<RoutingParameter> getParameters() {
//        allParameters.add(maxThreadsParameter);
        allParameters.add(numPartitionsParameter);
        allParameters.add(regionDivideMethodParameter);
        allParameters.add(minimumRegionBorderLengthParameter);
        //allParams.add(widthWhereJustOneWireIsPermittedParameter);
        allParameters.add(maxLayerUseParameter);
        return allParameters;
    }

    /**
     * Method to do Simple routing.
     */
    @Override
    protected void runRouting(Cell cell, List<RoutingSegment> segmentsToRoute, List<RoutingLayer> allLayers,
            List<RoutingContact> allContacts, List<RoutingGeometry> blockages) {

//        printChipStatistics(cell, segmentsToRoute, allLayers, allContacts, blockages);
        
        
        long start_time = System.currentTimeMillis();
        if(output)
        System.out.println("electric goes yana...");
        
        int maxRuntimeMs=this.maxRuntime.getIntValue()*1000;
        
        maxThreads = (numThreads.getIntValue() < 1) ? 1 : numThreads.getIntValue();
        numPartitions = (numPartitionsParameter.getIntValue() < 1) ? 1 : numPartitionsParameter.getIntValue();
        minimumRegionBorderLength = (minimumRegionBorderLengthParameter.getIntValue() < 2) ? 2 : minimumRegionBorderLengthParameter.getIntValue();
        //distanceBetweenWires = (widthWhereJustOneWireIsPermittedParameter.getIntValue() < 1) ? 1 : widthWhereJustOneWireIsPermittedParameter.getIntValue();
        regionDivideMethod = regionDivideMethodParameter.getIntValue();
        //regionDivideMethod can be any value, but if it is an unkown one, there will be just one region
        
        maxLayerUse=maxLayerUseParameter.getIntValue();
        if(maxLayerUse<=0 || maxLayerUse>countLayers(allLayers)){
        	maxLayerUse=countLayers(allLayers);
        }
        
        int dist= 0;
        int width = 0;
        int currentLayer = 0;
        for (RoutingLayer rl : allLayers) {
            if(rl.isMetal() && currentLayer<maxLayerUse){
	        	if (rl.getMinSpacing(rl)>dist) {
	                dist=(int)Math.round(rl.getMinSpacing(rl));
	            }
	            if(rl.getMinWidth()>width){
	                width = (int)Math.round(rl.getMinWidth());
	            }
	            currentLayer++;
            }
        }
        
        //distance: free space needed between two objects on the same layer
        //width: width of a wire
        //assumption: vias are width+1 thick (except vias from the last layer
        if(width<5){
        	width++;
        }
        distanceBetweenWires = dist + width;

        if(output)
        System.out.println("Running " + maxThreads + " Threads on " + numPartitions + " partitions each minimum " + minimumRegionBorderLength + " length and " + distanceBetweenWires + " wire spacing.");
        initProgress();

        initializeWiringClass(allLayers, allContacts);

        // for testing purposes: identify blocking cells -> will be deleted later
//        Iterator<NodeInst> iter = cell.getNodes();
//        while (iter.hasNext()) {
//            NodeInst node = iter.next();
//            if (node.getName().contains("blockage")) {
//                for (RoutingLayer rl : allLayers) {
//                    RoutingGeometry blockage = new RoutingGeometry(rl, node.getBounds(), 0);
//                    blockages.add(blockage);
//                }
//            }
//        }

        //create barriers
        barrierRouting = new CyclicBarrier(maxThreads, new Runnable() {

            public void run() {
                WorkPool.prepare();
            }
        });
        
        barrierWiring = new CyclicBarrier(maxThreads);

        //configure the Tupel-class static variables
        Tupel.setOffset(cell.getBounds().getLambdaX(), cell.getBounds().getLambdaY(), 10, output);

        //the global routing array, where the routing will be done from each thread
        RoutingArray ra = new RoutingArray(cell, maxLayerUse, blockages, 3);

        //DEBUG
//        System.out.println("Interessantes Tupel: " + Tupel.convertElectricToRoutingArrayCoordinate_X(208) + "," + Tupel.convertElectricToRoutingArrayCoordinate_Y(101));

        //set start and end points as blocked
        markStartEndAsBlocked(segmentsToRoute, ra);

        //create all workers
        WorkerThread[] workerObjects = new WorkerThread[maxThreads];
        Thread[] workerThreads = new Thread[maxThreads];
        for (int i = 0; i < maxThreads; i++) {
            workerObjects[i] = new WorkerThread(i);
            workerThreads[i] = new Thread(workerObjects[i]);
            //do not start the threads here, because they have to be initialized first
        }
        maxRuntimeMs=(int)(System.currentTimeMillis()-start_time)+maxRuntimeMs;		//calculate runtime without initialization
        WorkerThread.init(workerObjects, maxLayerUse, ra, segmentsToRoute,maxRuntimeMs, output);

        //start all threads
        for (int i = 0; i < maxThreads; i++) {
            workerThreads[i].start();
        }

        // wait until threads have finished their work. afterwards we can do the return
        try {
            for (int i = 0; i < maxThreads; i++) {
                workerThreads[i].join();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(yana.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(output)
        System.out.println("yana complete...");
        closeProgress();
        if(output)
        System.out.println("YANA-time: " + TextUtils.getElapsedTime(System.currentTimeMillis() - start_time));
        System.gc();
    }

    private void initializeWiringClass(List<RoutingLayer> allLayers, List<RoutingContact> allContacts) {
        //map metal layers which can be used for routing
		int metalLayers = 0;
		HashMap<String, Integer> metalLayerMap = new HashMap<String, Integer>();
		for (RoutingLayer rl : allLayers) {
			if (rl.isMetal()) {
				metalLayerMap.put(rl.getName(), metalLayers);
				metalLayers++;
			}
		}
		Wiring.init(allLayers, metalLayerMap, allContacts, output);
	}

	private int countLayers(List<RoutingLayer> allLayers) {
		int layer = 0;
		for (RoutingLayer l : allLayers) {
			if (l != null && l.isMetal()) {
				layer++;
			}
		}
		return layer;
	}

	/**
	 * print some statistics about the chip
	 *
	 * 
	 * @param cell
	 * @param segmentsToRoute
	 * @param allLayers
	 * @param allContacts
	 * @param blockages
	 */
	public void printChipStatistics(Cell cell, List<RoutingSegment> segmentsToRoute,
			List<RoutingLayer> allLayers, List<RoutingContact> allContacts, List<RoutingGeometry> blockages) {

		int metalLayers = 0;

		for (RoutingLayer rl : allLayers) {
			if (rl.isMetal()) {
				metalLayers++;
			}
		}

		int[] layerDistance = new int[metalLayers];
		int[] layerMembership = new int[metalLayers];
		int[][] crossLayerMembership = new int[metalLayers][metalLayers];
		int singleLayer = 0;
		int crossLayer = 0;

		int maxNetID = Integer.MIN_VALUE;

		for (RoutingSegment rs : segmentsToRoute) {
			maxNetID = Math.max(maxNetID, rs.getNetID());

			int minStart = Integer.MAX_VALUE;
			int maxStart = Integer.MIN_VALUE;
			for (RoutingLayer rl : rs.getStartLayers()) {
				minStart = Math.min(minStart, rl.getMetalNumber());
				maxStart = Math.max(maxStart, rl.getMetalNumber());
			}
			int minFinish = Integer.MAX_VALUE;
			int maxFinish = Integer.MIN_VALUE;
			for (RoutingLayer rl : rs.getFinishLayers()) {
				minFinish = Math.min(minFinish, rl.getMetalNumber());
				maxFinish = Math.max(maxFinish, rl.getMetalNumber());
			}

			if (minStart == maxStart && minFinish == maxFinish) {
				int distance = Math.abs(minStart - minFinish);
				layerDistance[distance]++;
				if (minStart == minFinish) {
					singleLayer++;
					layerMembership[minStart - 1]++;
				} else {
					int min = Math.min(minStart, minFinish);
					int max = Math.max(minStart, minFinish);
					crossLayerMembership[min - 1][max - 1]++;
					crossLayer++;
				}
			}
		}

		int[] nets = new int[maxNetID + 1];

		for (RoutingSegment rs : segmentsToRoute) {
			nets[rs.getNetID()]++;
		}

		int numberOfNets = 0;
		int maxSegments = Integer.MIN_VALUE;

		for (int i = 0; i < nets.length; i++) {
			if (nets[i] != 0) {
				numberOfNets++;
			}
			maxSegments = Math.max(maxSegments, nets[i]);
		}

		int[] numberOfSegments = new int[maxSegments];
		for (int i = 0; i < nets.length; i++) {
			if (nets[i] != 0) {
				numberOfSegments[nets[i] - 1]++;
			}
		}

		int[] blockagesOnLayer = new int[metalLayers];

		for (RoutingGeometry rg : blockages) {
			blockagesOnLayer[rg.getLayer().getMetalNumber() - 1]++;
		}

		System.out.println("################### Cell statistics  ########################");
		System.out.println();
		System.out.println();
		System.out.print("#Metal-Layers: " + metalLayers + " { ");
		for (RoutingLayer rl : allLayers) {
			if (rl.isMetal()) {
				System.out.print(rl.getMetalNumber() + ", ");
			}
		}
		System.out.println("}\n");

		System.out.println("#Segments: " + segmentsToRoute.size());
		System.out.println("\t on one layer:  " + singleLayer);
		for (int i = 0; i < layerMembership.length; i++) {
			if (layerMembership[i] != 0) {
				System.out.println("\t\t Metal-" + (i + 1) + ": " + layerMembership[i]);
			}
		}
		System.out.println("\t on two layers: " + crossLayer);
		for (int i = 0; i < crossLayerMembership.length; i++) {
			for (int j = 0; j < crossLayerMembership[0].length; j++) {
				if (crossLayerMembership[i][j] != 0) {
					System.out.println("\t\t Metal-" + (i + 1) + " - Metal-" + (j + 1) + ": "
							+ crossLayerMembership[i][j]);
				}
			}
		}
		System.out.println();

		System.out.println("layer distance distribution");
		for (int i = 0; i < layerDistance.length; i++) {
			if (layerDistance[i] != 0) {
				System.out.println("\t distance " + i + ": " + layerDistance[i]);
			}
		}
		System.out.println();

		System.out.println("#Blockages: " + blockages.size());
		for (int i = 0; i < blockagesOnLayer.length; i++) {
			if (blockagesOnLayer[i] != 0) {
				System.out.println("\t on Metal-" + (i + 1) + ": " + blockagesOnLayer[i]);
			}
		}
		System.out.println();

		int countNumSegments = 0;

		System.out.println("#Nets: " + numberOfNets);
		for (int i = 0; i < numberOfSegments.length; i++) {
			if (numberOfSegments[i] != 0) {
				countNumSegments += numberOfSegments[i] * (i + 1);
				System.out.print("\t " + (i + 1) + " segments: " + numberOfSegments[i] + "{ ");
				for (int j = 0; j < nets.length; j++) {
					if (nets[j] == (i + 1)) {
						System.out.print(j + ", ");
					}
				}
				System.out.println("}");
			}
		}
		System.out.println("\tsum: " + countNumSegments + " segments");

		System.out.println("############################################################");
		System.out.println();
	}

	/**
	 * Initializes progress bar
	 */
	public static void initProgress() {
		gui = Job.getUserInterface();
		gui.startProgressDialog("Yana is working for you", null);
		gui.setProgressNote("Running...");
		gui.setProgressValue(0);
	}

	/**
	 * Increases maximum value used in progress bar
	 * 
	 * @param size
	 *            value to add to current maximum value
	 */
	synchronized public static void setProgressMax(int size) {
		progressMax += size;
	}

	/**
	 * Update progress bar. Marks one job as done
	 */
	synchronized public static void updateProgress() {
		progress++;
		// System.out.println("Progress: "+(double)progress/progressMax*100);
		gui.setProgressNote("Routing segment " + progress + "/" + progressMax);
		gui.setProgressValue((int) ((double) progress / progressMax * 100));
	}

	/**
	 * Hides progess bar
	 */
	public static void closeProgress() {
		gui.stopProgressDialog();
	}

	/**
	 * Mark start and end points of a routing segment as blocked
	 * 
	 * @param segmentsToRoute
	 *            Routing Segments
	 * @param ra
	 *            Routing Array
	 */
	private void markStartEndAsBlocked(List<RoutingSegment> segmentsToRoute, RoutingArray ra) {
		Tupel[] startEndPoints = new Tupel[18];
		int i;
		for (RoutingSegment rs : segmentsToRoute) {
			// original points
			i=0;
			
			Tupel start = new Tupel(rs.getStartEnd().getLocation(), rs.getStartLayers().get(0)
					.getMetalNumber() - 1);
			Tupel end = new Tupel(rs.getFinishEnd().getLocation(), rs.getFinishLayers().get(0)
					.getMetalNumber() - 1);
			
			//DEBUG
//			if(start.getX_InsideRoutingArray()==93 && start.getY_InsideRoutingArray()==50){
//	        	System.out.println("GOT IT");
//	        }
//	    	if(end.getX_InsideRoutingArray()==93 && end.getY_InsideRoutingArray()==50){
//	        	System.out.println("GOT IT");
//	        }
			
			startEndPoints[i++] = start;
			startEndPoints[i++] = end;
			// points right and top of the original points
			startEndPoints[i++] = new Tupel(start.getX_InsideRoutingArray() + 1, start
					.getY_InsideRoutingArray(), start.getLayer(), false);
			startEndPoints[i++] = new Tupel(start.getX_InsideRoutingArray(),
					start.getY_InsideRoutingArray() + 1, start.getLayer(), false);
			startEndPoints[i++] = new Tupel(start.getX_InsideRoutingArray() + 1, start
					.getY_InsideRoutingArray() + 1, start.getLayer(), false);
			startEndPoints[i++] = new Tupel(start.getX_InsideRoutingArray() - 1, start
					.getY_InsideRoutingArray(), start.getLayer(), false);
			startEndPoints[i++] = new Tupel(start.getX_InsideRoutingArray(),
					start.getY_InsideRoutingArray() - 1, start.getLayer(), false);
			startEndPoints[i++] = new Tupel(start.getX_InsideRoutingArray() - 1, start
					.getY_InsideRoutingArray() - 1, start.getLayer(), false);
			startEndPoints[i++] = new Tupel(start.getX_InsideRoutingArray() + 1, start
					.getY_InsideRoutingArray() - 1, start.getLayer(), false);
			startEndPoints[i++] = new Tupel(start.getX_InsideRoutingArray() - 1, start
					.getY_InsideRoutingArray() + 1, start.getLayer(), false);

			startEndPoints[i++] = new Tupel(end.getX_InsideRoutingArray() + 1, end.getY_InsideRoutingArray(),
					end.getLayer(), false);
			startEndPoints[i++] = new Tupel(end.getX_InsideRoutingArray(), end.getY_InsideRoutingArray() + 1,
					end.getLayer(), false);
			startEndPoints[i++] = new Tupel(end.getX_InsideRoutingArray() + 1,
					end.getY_InsideRoutingArray() + 1, end.getLayer(), false);
			startEndPoints[i++] = new Tupel(end.getX_InsideRoutingArray() - 1, end.getY_InsideRoutingArray(),
					end.getLayer(), false);
			startEndPoints[i++] = new Tupel(end.getX_InsideRoutingArray(), end.getY_InsideRoutingArray() - 1,
					end.getLayer(), false);
			startEndPoints[i++] = new Tupel(end.getX_InsideRoutingArray() - 1,
					end.getY_InsideRoutingArray() - 1, end.getLayer(), false);
			startEndPoints[i++] = new Tupel(end.getX_InsideRoutingArray() + 1,
					end.getY_InsideRoutingArray() - 1, end.getLayer(), false);
			startEndPoints[i++] = new Tupel(end.getX_InsideRoutingArray() - 1,
					end.getY_InsideRoutingArray() + 1, end.getLayer(), false);
			
			ra.reserveForRouting(startEndPoints,rs.getNetID());
		}
		ra.markReserved();
	}

	public yana() {
		super();
	}

	public static void markSegmentAsUnroutable(RoutingSegment rs){
		unroutedNets.put(rs.hashCode(), false);
	}

	public static boolean isRouteable(RoutingSegment rs) {
		if(unroutedNets.get(rs.hashCode())==null){
			//segment can be routed
			return true;
		}else{
			return false;
		}
	}
}

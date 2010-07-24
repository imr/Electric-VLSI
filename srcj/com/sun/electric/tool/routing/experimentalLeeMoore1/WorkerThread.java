/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WorkerThread.java
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
import com.sun.electric.tool.routing.RoutingFrame.*;
import com.sun.electric.tool.routing.experimentalLeeMoore1.LeeMoore.Route;
import com.sun.electric.tool.routing.experimentalLeeMoore1.LeeMoore.RoutingArray;
import com.sun.electric.tool.routing.experimentalLeeMoore1.LeeMoore.Tupel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;

/**
 * This is one thread doing routing
 */
public class WorkerThread implements Runnable {

    // for all threads the same variables
    public static final boolean X_DIRECTION = true; // const
    public static final boolean Y_DIRECTION = false; // const
    //protected static final Object signal = new Object(); // will be used to wait
    // for
    // segmentCounter-changes
    private static long DEADLINE;
    private static int MAXLAYER; // the number of usable layers
    protected static boolean[][][] regionBoundaries;
    private static RoutingArray ra;
    protected static int size_x;
    protected static int size_y;
    private static FindVirtualEndInterface findVirtualEndAlgorithm;
    private static int NUMPARTITIONS;
//	private static boolean OUTPUT = false;
    // thread specific variables
    private int id;
    private boolean DEBUG = false;
    
    List<WiringJob> wj=new ArrayList<WiringJob>();

    /**
     *
     * @param workerObjects the WorkerObjects as an array
     * @param countLayers on how much layers will be routed
     * @param ra the RoutingArray
     * @param segmentsToRoute the segments to route, got from Electric
     *
     * This method has to be called before any other method!
     */
    public static void init(WorkerThread[] workerObjects, int countLayers,
            RoutingArray ra, List<RoutingSegment> segmentsToRoute, int maxRunTime, boolean output) {
    	
//    	WorkerThread.OUTPUT = output; 
    	
    	WorkerThread.DEADLINE = System.currentTimeMillis()+maxRunTime;
        WorkerThread.MAXLAYER = countLayers;
        WorkerThread.ra = ra;
        WorkerThread.NUMPARTITIONS = yana.numPartitions;
        WorkerThread.size_x = ra.size_x;
        WorkerThread.size_y = ra.size_y;

        // we have to initialize the workpool
        WorkPool.init(segmentsToRoute, NUMPARTITIONS, size_x, size_y, output);

        WorkPool.printPartitionBorders();

        // initialize the boundary data structure
        regionBoundaries = new boolean[size_x][size_y][MAXLAYER];
        for (int i = 0; i < size_x; i++) {
            for (int j = 0; j < size_y; j++) {
                for (int k = 0; k < MAXLAYER; k++) {
                    regionBoundaries[i][j][k] = true;
                }
            }
        }
        // set all blocked points also as blocked in the boundary structure
        ArrayList<Tupel> blocked = ra.getBlocked();
        for (Tupel t : blocked) {
            for (int i = 0; i < MAXLAYER; i++) {
                regionBoundaries[t.getX_InsideRoutingArray()][t.getY_InsideRoutingArray()][i] = false;
            }
        }
        findVirtualEndAlgorithm = new FindVirtualEnd_ProjectedAndRandomized(
                X_DIRECTION, Y_DIRECTION, MAXLAYER, output);
        // System.out.print("Init End.");
    }

    /*******************************************************************************************************************************************/
    /**
     *
     * @param id must be a unique number. the numbers 0 to n-1 must be given, when n threads shall work
     */
    public WorkerThread(int id) {
        this.id = id;
    }

    /**
     * The Worker will do the three following things:
     * 1.) Change the RoutingSegments to RoutingParts which have more information inside it for routing.
     * 2.) Get a RoutingPart out of the WorkPool. If it is completely belonging to a WorkPartition (start and end are located inside of it) assign it,
     * 	   else split the RoutingPart.
     * 3.) Grab one WorkPartition and route the routingPart inside. If a route was found, it will be transformed into the electric data structures
     */
    public void run() {

        if (DEBUG) System.out.println("Thread-" + id + " running...");
        changeRoutingSegmentToRoutingPart();

        if (DEBUG) System.out.println("Thread-" + id + " allocating work to partitions...");
        allocateRoutingPartsToWorkPartitions();

        try {
            yana.barrierRouting.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }

        System.gc();

        RoutingPart rp;
        Tupel start;
        Tupel end;
        Route r;

        // Now get a WorkPartition and do routing & wiring
        WorkPartition wp = WorkPool.getWorkFromPartition();
        while (wp != null) {
        	if (DEBUG) System.out.println("Thread-" + id + ": routing partition " + wp.id + " containing " + wp.routingParts.size() + " routingparts");
            rp = wp.routingParts.poll();
            
            while (rp != null) {
            	if(System.currentTimeMillis()>DEADLINE){
            		System.out.println("Timeout...");
            		return;
            	}
            	start = rp.start;
                end = rp.end;
                if (DEBUG) {
                    String sys_out = "THREAD " + id + ": ";
                    sys_out += "Routing netID " + rp.rs.getNetID() + ": ";
                    sys_out += "[" + rp.rs.getStartEnd().getLocation().getX() + ","
                            + rp.rs.getStartEnd().getLocation().getY() + "@"
                            + rp.rs.getStartLayers().get(0).getName() + "]";
                    sys_out += "->[" + rp.rs.getFinishEnd().getLocation().getX() + ","
                            + rp.rs.getFinishEnd().getLocation().getY() + "@"
                            + rp.rs.getFinishLayers().get(0).getName() + "]";
                    sys_out += "; part " + start + "->" + end;
                    System.out.println(sys_out);
                    System.out.println("Borders: " + WorkPool.getLowIndexIn_X(wp.id) + ","
                            + WorkPool.getHighIndexIn_X(wp.id) + "; "
                            + WorkPool.getLowIndexIn_Y(wp.id) + ","
                            + WorkPool.getHighIndexIn_Y(wp.id));
                }
                if (!start.isEqual(end)) {
                    r = ra.route(start, end, wp.tb, rp.rs.getNetID());
                    // route is found
                    if (r != null) {
//                        if (DEBUG) {
//                            System.out.println("Thread-" + id + " wiring");
//                        }
//                        Wiring.connect(rp, r.getEdgePoints(),r.isReversed());
                    	wj.add(new WiringJob(rp, r.getEdgePoints(), r.isReversed()));
                    }else{
                    	//no route could be found
                    	yana.markSegmentAsUnroutable(rp.rs);
                    }
                }
                yana.updateProgress();
                rp = wp.routingParts.poll();
            }
            wp = WorkPool.getWorkFromPartition();
        }
        
        if (DEBUG) System.out.println("Thread-" + id + " finished routing");
        
        //wait for other threads to finish to be able to start wiring
        try {
			yana.barrierWiring.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
        
        //wiring
		for (WiringJob wjob : wj) {
			if(yana.isRouteable(wjob.rp.rs)){
				//RoutingSegment was not marked as partly unrouted
				if(wjob.isConnectionPoints){
					Wiring.connect(wjob.rp.rs,wjob.rp1,wjob.rp2);
				}else{
					Wiring.connect(wjob.rp, wjob.route, wjob.isReversed);
				}
			}
		}
		
		if (DEBUG) System.out.println("Thread-" + id + " finished wiring");

    }

    /*******************************************************************************************************************************************/
    
    /**
     * Create RoutingParts out of RoutingSegments. They contain more information and allow splitting.
     */
    private void changeRoutingSegmentToRoutingPart() {
        RoutingSegment rs = WorkPool.getRoutingSegment();
        while (rs != null) {
            WorkPool.addPartToGlobalRoutingQueue(new RoutingPart(rs));
            rs = WorkPool.getRoutingSegment();
        }
    }

    /**
     * Allocate RoutingParts to WorkPartitions or split RoutingParts if they are located on multiple WorkPartitions
     */
    private void allocateRoutingPartsToWorkPartitions() {
        RoutingPart rp;
        Tupel start;
        Tupel end;
        RoutingPart prefixRoutingPart;
        RoutingPart suffixRoutingPart;
        int indexStart;
        int indexEnd;
        RoutePoint[] borderPoints;
        while (WorkPool.getSegmentCounter() != 0) {
            while (WorkPool.getGlobalRoutingQueueSize() != 0) {
                rp = WorkPool.getPartFromGlobalRoutingQueue();
                if (rp == null) {
                    Thread.yield();
                    break;
                    //make yield and break to return, because here we have just one segment left and just one thread can allocate the partitions from now
                }
                start = rp.start;
                end = rp.end;

                indexEnd = WorkPool.getResponsiblePartitionID_ForPoint(end.getX_InsideRoutingArray(), end.getY_InsideRoutingArray());
                indexStart = WorkPool.getResponsiblePartitionID_ForPoint(start.getX_InsideRoutingArray(), start.getY_InsideRoutingArray());
                if (indexEnd == indexStart) {
                    // both ends are in same partition -> add to work queue
                    // decrease segmentCounter
                    WorkPool.addWorkToPartition(rp, indexStart);
                    WorkPool.decreaseSegmentCounter();
                } else {
                    // if points are not in same partition => find
                    // region-connecting-point, append prefix RoutingPart to responsible partition and suffix  RoutingPart to
                    // RoutingPart queue; do NOT decrease segmentCounter
                    ConnectionPoints connectionPoints = findVirtualEndAlgorithm.findVirtualEnd(rp, indexStart);
                    // making the find-method synchronized, so we can
                    // delete the while loop => better performance?
                    while (connectionPoints.areValid() && blockConnectingPoint(connectionPoints.getInnerPoint(), connectionPoints.getOuterPoint()) == false) {
                        connectionPoints = findVirtualEndAlgorithm.findVirtualEnd(rp, indexStart);
                    }
                    if (!connectionPoints.areValid()) {
                        // a routing segment will not be routed, because a part
                        // is missing
                    	if (DEBUG) System.out.println("Part of a RoutingSegment could not be routed");
                    	yana.markSegmentAsUnroutable(rp.rs);
                        WorkPool.decreaseSegmentCounter();
                    } else {
                        // now we create a wire between the connectionPoints
//                        borderPoints=Wiring.connect(rp.rs, connectionPoints.getInnerPoint(),
//                                connectionPoints.getOuterPoint());
                    	borderPoints=Wiring.getRoutePoints(connectionPoints.getInnerPoint(),
                              connectionPoints.getOuterPoint());
                    	wj.add(new WiringJob(rp,borderPoints[0],borderPoints[1],false));
                        
                        // mark both tupels as blocked
                        ra.setBlocked(connectionPoints.getTupels());

                        // split the RoutingSegment into two RoutingSegments
                        prefixRoutingPart = rp.getPrefixPart(connectionPoints.getInnerPoint(),borderPoints[0]);
                        suffixRoutingPart = rp.getSuffixPart(connectionPoints.getOuterPoint(),borderPoints[1]);
                        
                        WorkPool.addPartToGlobalRoutingQueue(suffixRoutingPart);
                        WorkPool.addWorkToPartition(prefixRoutingPart, indexStart);
                        WorkPool.increaseAdditionalSegments();
                    }
                }
            }
        }
    }

    private synchronized boolean blockConnectingPoint(Tupel pointA, Tupel pointB) {
        int PAX = pointA.getX_InsideRoutingArray();
        int PBX = pointB.getX_InsideRoutingArray();
        int PAY = pointA.getY_InsideRoutingArray();
        int PBY = pointB.getY_InsideRoutingArray();
        if (PAX == -1 || PAY == -1 || PBX == -1 || PBY == -1) {
            System.out.println(" CRITICAL ERROR: we've got company! (a crash is approaching)");
        }

        if (Math.abs(PAX - PBX) == 1) { // check if points are neighbored in
            // x-direction
            if ((PAY - PBY) != 0
                    || (pointA.getLayer() - pointB.getLayer()) != 0) {
                return false;// because they are not neighbored
            }
            // neighbored points in x direction
            int minX = (PAX < PBX) ? PAX : PBX;
            if (regionBoundaries[minX][PAY][pointA.getLayer()] == true) {// free
                // point
                regionBoundaries[minX][PAY][pointA.getLayer()] = false;
                regionBoundaries[minX + 1][PAY][pointA.getLayer()] = false;
                return true;
            } else {
                return false;
            }
        } else if (Math.abs(PAY - PBY) == 1) {// check if points are neighbored
            // in y-direction
            if ((PAX - PBX) != 0
                    || (pointA.getLayer() - pointB.getLayer()) != 0) {
                return false;// because they are not neighbored
            }
            // neighbored in y direction
            int minY = (PAY < PBY) ? PAY : PBY;
            if (regionBoundaries[PAX][minY][pointA.getLayer()] == true) {// free
                // point
                regionBoundaries[PAX][minY][pointA.getLayer()] = false;
                regionBoundaries[PAX][minY + 1][pointA.getLayer()] = false;
                return true;
            } else {
                return false;
            }
        }
        return false; // because they are not neighbored

    }

    protected static Tupel getMiddlePoint(Tupel p) {
        int x = p.getX_InsideRoutingArray();
        int y = p.getY_InsideRoutingArray();
        int workerID = WorkPool.getResponsiblePartitionID_ForPoint(x, y);
        int lowX = WorkPool.getLowIndexIn_X(workerID);
        int hiX = WorkPool.getHighIndexIn_X(workerID);
        int lowY = WorkPool.getLowIndexIn_Y(workerID);
        int hiY = WorkPool.getHighIndexIn_Y(workerID);
        return new Tupel((int) ((hiX - lowX) / 2) + lowX,
                (int) ((hiY - lowY) / 2) + lowY, p.getLayer(), false);
    }

    class WiringJob{
    	RoutingPart rp;
    	List<Tupel> route;
    	RoutePoint rp1,rp2;
    	boolean isConnectionPoints;
    	boolean isReversed;
    	
		public WiringJob(RoutingPart rp, List<Tupel> route, boolean isReversed) {
			super();
			this.rp = rp;
			this.route = route;
			this.isConnectionPoints = false;
			this.isReversed = isReversed;
			this.rp1=null;
			this.rp2=null;
		}

		public WiringJob(RoutingPart rp, RoutePoint rp1,
				RoutePoint rp2, boolean isReversed) {
			super();
			this.rp = rp;
			this.route = null;
			this.rp1 = rp1;
			this.rp2 = rp2;
			this.isConnectionPoints = true;
			this.isReversed = isReversed;
		}
		
    }
}

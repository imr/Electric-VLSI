/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WorkPool.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import com.sun.electric.tool.routing.RoutingFrame.RoutingSegment;

/**
 * A workpool contains all the work partitions and manages them.
 */
public class WorkPool {

    private static ConcurrentLinkedQueue<RoutingSegment> inputList = new ConcurrentLinkedQueue<RoutingSegment>();
    private static BlockingQueue<RoutingPart> globalRoutingQueue;
    private static ArrayList<WorkPartition> workPartitions;
    private static ConcurrentLinkedQueue<WorkPartition> workPartitionsList;
    private static int segmentCounter; // CountDownLatch only decrease the segmentCounter with the help of the decrease function!
    private static int additionalSegments;
    private static int WorkDivideIn_X_Dir; // represents the number of regions
    // in x direction
    private static int WorkDivideIn_Y_Dir; // analogeous to above in y direction
    private static int minimumRegionBorderLength;
    private static int size_x;
    private static int size_y;
    private static int numPartitions;
    private static int[][] regionPartition;
    
    public static boolean output = false;

    /**
     * This method has to be called before any other!
     * @param segmentsToRoute the segments from Electric, which have to be routed
     * @param numPartitions in how many partitions should the grid be devided
     * @param size_x grid size in x direction
     * @param size_y grid size in y direction
     */
    public static void init(List<RoutingSegment> segmentsToRoute, int numPartitions, int size_x, int size_y, boolean output) {
        WorkPool.output = output;
    	//set variables
        WorkPool.size_x = size_x;
        WorkPool.size_y = size_y;
        WorkPool.numPartitions = numPartitions;
        minimumRegionBorderLength = yana.minimumRegionBorderLength;
        segmentCounter = segmentsToRoute.size();
        additionalSegments = segmentCounter;

        //initialize objects
        globalRoutingQueue = new ArrayBlockingQueue<RoutingPart>(segmentCounter);
        for (RoutingSegment rs : segmentsToRoute) {
            inputList.add(rs);
        }

        workPartitions = new ArrayList<WorkPartition>(numPartitions);

        //initialize working partitions
        for (int i = 0; i < numPartitions; i++) {
            workPartitions.add(new WorkPartition(i));
        }

        //calculate and create ThreadBorders for the partitions
        createThreadBorders(yana.regionDivideMethod);
        createRegionArray();
    }

   /**
    * This method creates the borders of regions. Therefore the "region_"-methods are called
    * @param method there are currently three methods how the grid can be devided into numPartition regions.
    */
    private static void createThreadBorders(int method) {

        //be sure, that the regions are not toooooo small
        do  {
            switch (method) {
                case 1:
                    regions_OptimalDivisionFactor();
                    break;
                case 2:
                    regions_simpleStripes();
                    break;
                case 3:
                    regions_AdaptedRegions();
                    break;
                default:
                    regions_OneRegion();
            }
            numPartitions--;
        }while (size_x / WorkDivideIn_X_Dir < minimumRegionBorderLength || size_y / WorkDivideIn_Y_Dir < minimumRegionBorderLength);
        //restore last usable numpartitions
        numPartitions++;
        if(output)
        System.out.println("size: " + size_x + "x" + size_y);
        if(output)
        System.out.println("region devision: " + WorkDivideIn_X_Dir + "x" + WorkDivideIn_Y_Dir + " for " + numPartitions +" regions");

        for (int i = 0; i < numPartitions; i++) {
            workPartitions.get(i).setThreadBorders(new ThreadBorders(WorkPool.getLowIndexIn_X(i),
                    WorkPool.getHighIndexIn_X(i), WorkPool.getLowIndexIn_Y(i),
                    WorkPool.getHighIndexIn_Y(i)));
        }

    }

    /**
     * We divide the whole grid into regions (as great and squared as possible)
     */
    private static void regions_OptimalDivisionFactor() {
        int smallerFactor = (int) Math.sqrt(numPartitions);
        aproximateXYdirection(smallerFactor);
    }

    /**
     * Divide the grid into stripes so that the longer direction is divided
     */
    private static void regions_simpleStripes() {
        aproximateXYdirection(1);
    }

    /**
     * Allocate the regions to the two dimensions, so that the ratio of the grid dimension sizes equals the number of the regions they get
     * so if the grid ratio is 2/3 its possible to allocate 4regions to the x-direction and 6 to the y-direction, because its the same ratio
     */
    private static void regions_AdaptedRegions() {
        /*
         * Following equations should be aproximated, to get an adapted region allocation:
         *
         * x=size_x, y=size_y, r=ratio of the grid, a=work in x direction, b = work in y direction, n=numwork
         *
         * x/y = r = a/b AND a*b=n
         *
         * therefore we get the following:
         * r = n/(b^2), because a=n/b
         * => b^2 = n/r
         *
         * so we can calculate a region division wich is nearly the same than the grid ratio
         */

        double r = (1.0 * size_x) / size_y;
        if (Math.abs(1.0 - r) < 0.05) {
            //squared grid
            regions_OptimalDivisionFactor();
            return;
        } else if (r < 1) {
            //let the ratio be always
            r = 1 / r;
        }
        int smallerSide = (int) Math.round(Math.sqrt(numPartitions / r));
        if (smallerSide == 0) {
            // grid to widely streched
            regions_simpleStripes();
            return;
        }
        aproximateXYdirection(smallerSide);
    }

    /**
     * just one region
     */
    private static void regions_OneRegion() {
        WorkDivideIn_X_Dir = 1;
        WorkDivideIn_Y_Dir = 1;
        numPartitions = 1;
    }

    /**
     * Try to find factor so that the regions allocated to the dimensions equal numPartitions if we multiply them.
     * @param smallerFactor 
     */
    private static void aproximateXYdirection(int smallerFactor) {
        if (smallerFactor <= 0) {
            regions_OneRegion();
            return;
        }
        while (numPartitions % smallerFactor != 0) {
            smallerFactor--;
        }
        if (size_x <= size_y) {
            WorkDivideIn_X_Dir = smallerFactor;
            WorkDivideIn_Y_Dir = numPartitions / smallerFactor;
        } else {
            WorkDivideIn_X_Dir = numPartitions / smallerFactor;
            WorkDivideIn_Y_Dir = smallerFactor;
        }
    }

    /**
     * Change data structure for real work phase. This method has to be called before threads are starting to do detailed routing.
     */
    public static void prepare() {
        verify();
        if(output)
        System.out.println("Preparing work partitions for real work...");
        workPartitionsList = new ConcurrentLinkedQueue<WorkPartition>(workPartitions);
        workPartitions = null;
        yana.setProgressMax(WorkPool.getAdditionalSegments());
    }

    /**
     * debug
     */
    private static void verify() {
        int numParts = 0;
        for (WorkPartition wp : workPartitions) {
            numParts += wp.routingParts.size();
        }
        if (numParts != additionalSegments && output) {
            System.out.println("ERROR: RoutingParts are missing!");
        }

    }

    /**
     * Gets a new routing segment for global routing.
     * @return RoutingSegment
     */
    public static RoutingSegment getRoutingSegment() {
        return inputList.poll();
    }

    /**
     * Get a routing part from the global routingPart-queue
     * @return RoutingPart
     */
    public static RoutingPart getPartFromGlobalRoutingQueue() {
        return globalRoutingQueue.poll();
    }

    /**
     * Puts a routingPart the global routingPart-queue
     * @param rp routingPart
     */
    public static void addPartToGlobalRoutingQueue(RoutingPart rp) {
        globalRoutingQueue.offer(rp);
    }

    /**
     * Return the current value of the segment counter
     * @return segment counter
     */
    synchronized public static int getSegmentCounter() {
        return segmentCounter;
    }

    /**
     * decreases the current value of the segment counter
     */
    synchronized static public void decreaseSegmentCounter() {
        segmentCounter--;
    }

    /**
     * This methods puts the routingPart in the queue of the given workPartition 
     * @param rp routingPart
     * @param partition workPartition
     */
    public static void addWorkToPartition(RoutingPart rp, int partition) {
        workPartitions.get(partition).addWork(rp);
    }

    /**
     * Get a new work partition for detailed routing.
     * @return WorkPartition
     */
    public static WorkPartition getWorkFromPartition() {
        return workPartitionsList.poll();
    }

    /**
     * Returns the size of the global routingPart queue
     * @return size of the global routingPart queue
     */
    public static int getGlobalRoutingQueueSize() {
        return globalRoutingQueue.size();
    }

    /**
     * Increase the value for additional segments
     */
    synchronized public static void increaseAdditionalSegments() {
        additionalSegments++;
    }

    /**
     * Returns the current value of how many additional segments were created
     * @return additional segments
     */
    synchronized public static int getAdditionalSegments() {
        return additionalSegments;
    }


    /*
     * this functions will return the "begin" and the "end" indexes of given
     * array. example: 15 long array (0...14), 4 threads (thread 0..3): each
     * thread id will get: 0-2, 3-6, 7-10, 11-14 depending on their ids. note:
     * the calculation is done via id*(ges/anz). this is because without
     * brackets an overflow at id*ges can occur!!!
     */
    private static int oneDimensionalGetHighIndex(int numberOfWork,
            int numWorkers, int rank) {
        rank++;
        if (numWorkers > numberOfWork) {
            if (rank > numberOfWork) {
                return -1;
            } else {
                return rank - 1;
            }
        }
        return ((int) (rank * (((double) numberOfWork) / ((double) numWorkers)))) - 1;
    }

    private static int oneDimensionalGetLowIndex(int numberOfWork,
            int numWorkers, int rank) {
        rank++;
        if (numWorkers > numberOfWork) {
            if (rank > numberOfWork) {
                return 0;
            } else {
                return rank - 1;
            }
        }
        return (((int) ((rank - 1) * (((double) numberOfWork) / ((double) numWorkers)))) + 1) - 1;
    }

    /*
     * these functions will return the boundary indexes of their region inside
     * the grid given their id
     */
    protected static int getHighIndexIn_X(int rank) {
        return oneDimensionalGetHighIndex(size_x, WorkDivideIn_X_Dir, rank
                % WorkDivideIn_X_Dir);
    }

    protected static int getHighIndexIn_Y(int rank) {
        return oneDimensionalGetHighIndex(size_y, WorkDivideIn_Y_Dir, rank
                / WorkDivideIn_X_Dir);
    }

    protected static int getLowIndexIn_X(int rank) {
        return oneDimensionalGetLowIndex(size_x, WorkDivideIn_X_Dir, rank
                % WorkDivideIn_X_Dir);
    }

    protected static int getLowIndexIn_Y(int rank) {
        return oneDimensionalGetLowIndex(size_y, WorkDivideIn_Y_Dir, rank
                / WorkDivideIn_X_Dir);
    }

    /**
     * just print some statics about which region has which borders
     */
    static void printPartitionBorders() {
        regionPartition = new int[size_x][size_y];
        for (int t = 0; t < numPartitions; t++) {
        	if(output)
            System.out.println("Partition-" + t + ": " + WorkPool.getLowIndexIn_X(t)
                    + "<x<" + WorkPool.getHighIndexIn_X(t) + ", " + WorkPool.getLowIndexIn_Y(t)
                    + "<y<" + WorkPool.getHighIndexIn_Y(t));
            for (int x = WorkPool.getLowIndexIn_X(t); x <= WorkPool.getHighIndexIn_X(t); x++) {
                for (int y = WorkPool.getLowIndexIn_Y(t); y <= WorkPool.getHighIndexIn_Y(t); y++) {
                    regionPartition[x][y] = t;
                }
            }
        }
    }

    /**
     * this data structure will store the responsible thread index for each grid-element
     */
    static void createRegionArray() {
        regionPartition = new int[size_x][size_y];
        for (int t = 0; t < numPartitions; t++) {
            for (int x = WorkPool.getLowIndexIn_X(t); x <= WorkPool.getHighIndexIn_X(t); x++) {
                for (int y = WorkPool.getLowIndexIn_Y(t); y <= WorkPool.getHighIndexIn_Y(t); y++) {
                    regionPartition[x][y] = t;
                }
            }
        }
    }

    /**
     * Get the index of the WorkPartition, which is responsible for this point
     * @param posX X-coordinate
     * @param posY Y-coordinate
     * @return index of the responsible WorkPartition
     */
    public static int getResponsiblePartitionID_ForPoint(int posX, int posY) {
        return regionPartition[posX][posY];
    }
}

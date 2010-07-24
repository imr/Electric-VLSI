/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FindVirtualEnd_ProjectedAndRandomized.java
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

import com.sun.electric.tool.routing.experimentalLeeMoore1.LeeMoore.Tupel;

import java.util.Random;

/**
 * This Class finds two points which are to overcome the region boundary of nighbored regions.
 */
public class FindVirtualEnd_ProjectedAndRandomized implements FindVirtualEndInterface {

    //some constants
    private static boolean X_DIRECTION;
    private static boolean Y_DIRECTION;
    private static byte LEFT = -1;
    private static byte RIGHT = 1;
    private static byte UP = 1;
    private static byte DOWN = -1;
    private static int MAXLAYER;
//    private static Random rand = new Random();
    private static boolean DEBUG = false;

    /**
     *
     * @param xdir just the boolean value of the x-directed routing direction
     * @param ydir just the boolean value of the y-directed routing direction
     * @param l the maximum layer on which we can find connectionpoints
     */
    public FindVirtualEnd_ProjectedAndRandomized(boolean xdir, boolean ydir, int l, boolean output) {
        X_DIRECTION = xdir;
        Y_DIRECTION = ydir;
        MAXLAYER = l;
        DEBUG = output;
    }

    /**
     *
     * @param rp the RoutingPart data structure
     * @param id the id of the region, where the routing part is inside
     * @return returns a connectionPoints object with valid tupels if it was possible to find a connection, else the areValid variable is false.
     *
     * This algorithms works as follows:
     * 1.) check if we can route in the direction of the current layer, direcly projected onto the boundary. we can save vias if we are able to.
     * 2.) check "left" and "right" from the projected location if there is a possibility. if not. check the one more left/right located points and so on. do this on all layers
     * 3.) check if there is any point to find at the boundary so we dont make the wire longer as the manhatten distance
     * 4.) find any point which brings us nearer to the end as we are.
     *
     * return as soon, as such two points are found. if not, then return an object, which areValid variable is false => no connectionPoint possible to find
     */
    public ConnectionPoints findVirtualEnd(RoutingPart rp, int id) {
        Tupel start = rp.start;
        Tupel end = rp.end;

        int deltaX = end.getX_InsideRoutingArray() - start.getX_InsideRoutingArray(); //these two variables will be put in the random generator, so they represent the maximum distance to the region boundary or the end.y-coordinate
        int deltaY = end.getY_InsideRoutingArray() - start.getY_InsideRoutingArray();
        boolean routingDirection;
        boolean layerRoutingDirection = getRoutingDirectionOnLayer(start.getLayer());
        boolean layerRoutingDirectionPossible;
        boolean routingInX_DirectionOK = ((getMiddlePoint(start).getX_InsideRoutingArray() - getMiddlePoint(end).getX_InsideRoutingArray()) != 0);
        boolean routingInY_DirectionOK = ((getMiddlePoint(start).getY_InsideRoutingArray() - getMiddlePoint(end).getY_InsideRoutingArray()) != 0);
        Tupel vEnd;
//        int randomCoord;

        if (routingInX_DirectionOK) {
            if (deltaX >= 0) {//go to the right
                deltaX = Math.min(deltaX, getHighIndexIn_X(id) - start.getX_InsideRoutingArray());
            } else {//go to the left
                deltaX = (-1) * Math.min(Math.abs(deltaX), Math.abs(getLowIndexIn_X(id) - start.getX_InsideRoutingArray()));
            }
        }

        if (routingInY_DirectionOK) {
            if (deltaY >= 0) {//go up
                deltaY = Math.min(deltaY, getHighIndexIn_Y(id) - start.getY_InsideRoutingArray());
            } else { //go down
                deltaY = (-1) * Math.min(Math.abs(deltaY), Math.abs(getLowIndexIn_Y(id) - start.getY_InsideRoutingArray()));
            }
        }

        if (layerRoutingDirection == X_DIRECTION && routingInX_DirectionOK) {
            layerRoutingDirectionPossible = true;
        } else if (layerRoutingDirection == Y_DIRECTION && routingInY_DirectionOK) {
            layerRoutingDirectionPossible = true;
        } else {
            layerRoutingDirectionPossible = false;
        }

        //Try the projection in the preferred directions, wich are the routing direction of the startlayer
        if (layerRoutingDirectionPossible) {
            if (layerRoutingDirection == X_DIRECTION) {

                if ((start.getX_InsideRoutingArray() <= end.getX_InsideRoutingArray())) {
                    //we have to go to the right, because the start is on the left side of the end
                    vEnd = new Tupel(getHighIndexIn_X(id), start.getY_InsideRoutingArray(), start.getLayer(), false);
                    if (isFree(vEnd, layerRoutingDirection, RIGHT)) {
                        return makeReturnArg(vEnd, layerRoutingDirection, RIGHT);
                    }
                } else { //((start.getX_InsideRoutingArray() > end.getX_InsideRoutingArray()) ) {
                    //we have to go to the left, because ... s.o.
                    vEnd = new Tupel(getLowIndexIn_X(id), start.getY_InsideRoutingArray(), start.getLayer(), false);
                    if (isFree(vEnd, layerRoutingDirection, LEFT)) {
                        return makeReturnArg(vEnd, layerRoutingDirection, LEFT);
                    }
                }
            } else { //layerRoutingDirection == Y_DIRECTION
                if ((start.getY_InsideRoutingArray() <= end.getY_InsideRoutingArray())) {
                    //we have to go to the top, because ... s.o.
                    vEnd = new Tupel(start.getX_InsideRoutingArray(), getHighIndexIn_Y(id), start.getLayer(), false);
                    if (isFree(vEnd, layerRoutingDirection, UP)) {
                        return makeReturnArg(vEnd, layerRoutingDirection, UP);
                    }
                } else { //((start.getY_InsideRoutingArray() > end.getY_InsideRoutingArray()) ) {
                    //we have to go down, because ... s.o.
                    vEnd = new Tupel(start.getX_InsideRoutingArray(), getLowIndexIn_Y(id), start.getLayer(), false);
                    if (isFree(vEnd, layerRoutingDirection, DOWN)) {
                        return makeReturnArg(vEnd, layerRoutingDirection, DOWN);
                    }
                }
            }
        }
        /*
         * prefered direction was not possible, so we try the other direction first, to minimize the use of vias
         * now try for each layer firstly to route in the layer routing routing direction and then in the other direction.
         * find the connection point randomized*/
        /*
         * Attention: the first two for loops are very tricky and difficult to understand. its easier to read the comments!
         */
        //some initialisations for the first loop
        int currentLayer = start.getLayer() + 1;
//        double factorToTryRandomMultipleTimes = 1; //more then one doesnt make a difference, less then one is ok too, but because its not the big waste of computation power 1 one is a good trade off.
        byte step = 1; //will guarantee, that the stop condition will be reached!
/*
        //TODO: this can be deleted?
        for (int q1 = 0; q1 < 2; q1++) {//just go two times through the inside-loop, once to search layers under the start point, and in the second run in upper layers
            for (; currentLayer < MAXLAYER && currentLayer >= 0; currentLayer += step) {
                layerRoutingDirection = getRoutingDirectionOnLayer(currentLayer);
                if (layerRoutingDirection == X_DIRECTION && routingInX_DirectionOK) {
                    //try to find a random point as often as possible cells exist
                    for (int r = 0; r <= Math.abs(deltaY) * factorToTryRandomMultipleTimes; r++) {
                        randomCoord = (int) (rand.nextInt(Math.abs(deltaY) + 1) * Math.signum(deltaY));
                        randomCoord += start.getY_InsideRoutingArray();
                        if ((start.getX_InsideRoutingArray() <= end.getX_InsideRoutingArray())) {//we have to go to the right, because the start is on the left side of the end
                            vEnd = new Tupel(getHighIndexIn_X(id), randomCoord, currentLayer, false);
                            if (isFree(vEnd, X_DIRECTION, RIGHT)) {
                                return makeReturnArg(vEnd, X_DIRECTION, RIGHT);
                            }
                        } else {//we have to go to the left, because ... s.o.
                            vEnd = new Tupel(getLowIndexIn_X(id), randomCoord, currentLayer, false);
                            if (isFree(vEnd, X_DIRECTION, LEFT)) {
                                return makeReturnArg(vEnd, X_DIRECTION, LEFT);
                            }
                        }
                    }
                }
                if (layerRoutingDirection == Y_DIRECTION && routingInY_DirectionOK) {//routing in x-dir, was not ok
                    //now try to find a connection point in the other direction...
                    for (int r = 0; r <= Math.abs(deltaX) * factorToTryRandomMultipleTimes; r++) {
                        randomCoord = (int) (rand.nextInt(Math.abs(deltaX) + 1) * Math.signum(deltaX));
                        randomCoord += start.getX_InsideRoutingArray();
                        if ((start.getY_InsideRoutingArray() <= end.getY_InsideRoutingArray())) {//we have to go to the top, because ... s.o.
                            vEnd = new Tupel(randomCoord, getHighIndexIn_Y(id), currentLayer, false);
                            if (isFree(vEnd, Y_DIRECTION, UP)) {
                                return makeReturnArg(vEnd, Y_DIRECTION, UP);
                            }
                        } else {//we have to go down, because ... s.o.
                            vEnd = new Tupel(randomCoord, getLowIndexIn_Y(id), currentLayer, false);
                            if (isFree(vEnd, Y_DIRECTION, DOWN)) {
                                return makeReturnArg(vEnd, Y_DIRECTION, DOWN);
                            }
                        }
                    }
                }
            }
            //now the first loop (going all alyers down is done, now try to find a connection point in the upper layers
            currentLayer = start.getLayer() - 1;
            step = -1;
        }

        System.out.println("WOW - it's difficult to find a RANDOM connection point for netID=" + rp.rs.getNetID() + " from " + start.toString() + " to " + end.toString());
*/
        /*******************************************************************************************************************************/
        /*
         *
         * the random search was not successful, so we search the same area step by step, bevore we make the simple search.
         *
         * Attention: the first two for loops are very tricky and difficult to understand. its easier to read the comments!
         *
         */
        //some initialisations for the first loop
        currentLayer = start.getLayer() + 1;
        int hop;
        int originalKoordinate;
        int tempKoordinate;
        step = 1; //will guarantee, that the stop condition will be reached!
        for (int q1 = 0; q1 < 2; q1++) {//just go two times through the inside-loop, once to search layers under the start point, and in the second run in upper layers
            for (; currentLayer < MAXLAYER && currentLayer >= 0; currentLayer += step) {
                layerRoutingDirection = getRoutingDirectionOnLayer(currentLayer);
                if (layerRoutingDirection == X_DIRECTION && routingInX_DirectionOK) {
                    //try to find a random point as often as possible cells exist
                    originalKoordinate = start.getY_InsideRoutingArray();
                    hop = 1;
                    tempKoordinate = originalKoordinate + hop;
                    while ((tempKoordinate > getLowIndexIn_Y(id) && tempKoordinate < getHighIndexIn_Y(id)-1)
                            || (originalKoordinate + getNewHop(hop) > getLowIndexIn_Y(id) && originalKoordinate + getNewHop(hop) < getHighIndexIn_Y(id)-1)) {
                        if ((tempKoordinate > getLowIndexIn_Y(id) && tempKoordinate < getHighIndexIn_Y(id)-1)) {
                            if ((start.getX_InsideRoutingArray() <= end.getX_InsideRoutingArray())) {//we have to go to the right, because the start is on the left side of the end
                                vEnd = new Tupel(getHighIndexIn_X(id), tempKoordinate, currentLayer, false);
                                if (isFree(vEnd, X_DIRECTION, RIGHT)) {
                                    return makeReturnArg(vEnd, X_DIRECTION, RIGHT);
                                }
                            } else {//we have to go to the left, because ... s.o.
                                vEnd = new Tupel(getLowIndexIn_X(id), tempKoordinate, currentLayer, false);
                                if (isFree(vEnd, X_DIRECTION, LEFT)) {
                                    return makeReturnArg(vEnd, X_DIRECTION, LEFT);
                                }
                            }
                        }
                        hop = getNewHop(hop);
                        tempKoordinate = originalKoordinate + hop;
                    }
                }
                if (layerRoutingDirection == Y_DIRECTION && routingInY_DirectionOK) {//routing in x-dir, was not ok
                    //now try to find a connection point in the other direction...
                    originalKoordinate = start.getX_InsideRoutingArray();
                    hop = 1;
                    tempKoordinate = originalKoordinate + hop;
                    while ((tempKoordinate > getLowIndexIn_X(id) && tempKoordinate < getHighIndexIn_X(id)-1)
                            || (originalKoordinate + getNewHop(hop) > getLowIndexIn_X(id) && originalKoordinate + getNewHop(hop) < getHighIndexIn_X(id)-1)) {
                        if ((tempKoordinate > getLowIndexIn_X(id) && tempKoordinate < getHighIndexIn_X(id)-1)) {
                            if ((start.getY_InsideRoutingArray() <= end.getY_InsideRoutingArray())) {//we have to go to the top, because ... s.o.
                                vEnd = new Tupel(tempKoordinate, getHighIndexIn_Y(id), currentLayer, false);
                                if (isFree(vEnd, Y_DIRECTION, UP)) {
                                    return makeReturnArg(vEnd, Y_DIRECTION, UP);
                                }
                            } else {//we have to go down, because ... s.o.
                                vEnd = new Tupel(tempKoordinate, getLowIndexIn_Y(id), currentLayer, false);
                                if (isFree(vEnd, Y_DIRECTION, DOWN)) {
                                    return makeReturnArg(vEnd, Y_DIRECTION, DOWN);
                                }
                            }
                        }
                        hop = getNewHop(hop);
                        tempKoordinate = originalKoordinate + hop;
                    }
                }
            }
            //now the first loop (going all alyers down is done, now try to find a connection point in the upper layers
            currentLayer = start.getLayer() - 1;
            step = -1;
        }

        if(DEBUG)
        System.out.println("OMG - it's difficult to find a SYSTEMATIC connection point for netID=" + rp.rs.getNetID() + " from " + start.toString() + " to " + end.toString());

        //TODO: i think its ok to let the borders be searched over the full range and not at both ends one smaller... right?
        if (Math.abs(getMiddlePoint(start).getX_InsideRoutingArray() - getMiddlePoint(end).getX_InsideRoutingArray())
                >= Math.abs(getMiddlePoint(start).getY_InsideRoutingArray() - getMiddlePoint(end).getY_InsideRoutingArray())) {
            routingDirection = X_DIRECTION;
            //route in Xdirection because its longer (on the x sided boundary)
            if ((start.getX_InsideRoutingArray() <= end.getX_InsideRoutingArray())) {
                //we have to go to the right, because the start is on the left side of the end
                for (int l = 0; l < MAXLAYER; l++) {
                    if (!isCorrectOddEvenLayerForRoutingDirection(l, routingDirection)) {
                        continue;
                    }
                    //Tupel vEnd = new Tupel(getHighIndexIn_X(this.id), 0, l, false);
                    for (int y = getLowIndexIn_Y(id) + 1; y < getHighIndexIn_Y(id)-1 ; y += 1) {
                        //try to find a free position on the right boundary
                        vEnd = new Tupel(getHighIndexIn_X(id), y, l, false);
                        if (isFree(vEnd, routingDirection, RIGHT)) {
                            return makeReturnArg(vEnd, routingDirection, RIGHT);
                        }
                    }
                }
            } else { //((start.getX_InsideRoutingArray() > end.getX_InsideRoutingArray()) ) {
                //we have to go to the left, because ... s.o.
                for (int l = 0; l < MAXLAYER; l++) {
                    if (!isCorrectOddEvenLayerForRoutingDirection(l, routingDirection)) {
                        continue;
                    }
                    //Tupel vEnd = new Tupel(getLowIndexIn_X(this.id), 0, l, false);
                    for (int y = getLowIndexIn_Y(id) + 1; y < getHighIndexIn_Y(id)-1 ; y += 1) {
                        //try to find a free position on the left boundary
                        vEnd = new Tupel(getLowIndexIn_X(id), y, l, false);
                        if (isFree(vEnd, routingDirection, LEFT)) {
                            return makeReturnArg(vEnd, routingDirection, LEFT);
                        }
                    }
                }
            }
        } else {
            routingDirection = Y_DIRECTION;
            //route in Ydirection, because its longer
            if ((start.getY_InsideRoutingArray() <= end.getY_InsideRoutingArray())) {
                //we have to go to the top, because ... s.o.
                for (int l = 0; l < MAXLAYER; l++) {
                    if (!isCorrectOddEvenLayerForRoutingDirection(l, routingDirection)) {
                        continue;
                    }
                    //Tupel vEnd = new Tupel(0, getHighIndexIn_Y(this.id), l, false);
                    for (int x = getLowIndexIn_X(id) + 1; x < getHighIndexIn_X(id)-1 ; x += 1) {
                        //try to find a free position on the top boundary
                        vEnd = new Tupel(x, getHighIndexIn_Y(id), l, false);
                        if (isFree(vEnd, routingDirection, UP)) {
                            return makeReturnArg(vEnd, routingDirection, UP);
                        }
                    }
                }
            } else { //((start.getY_InsideRoutingArray() > end.getY_InsideRoutingArray()) ) {
                //we have to go down, because ... s.o.
                for (int l = 0; l < MAXLAYER; l++) {
                    if (!isCorrectOddEvenLayerForRoutingDirection(l, routingDirection)) {
                        continue;
                    }
                    //Tupel vEnd = new Tupel(0, getLowIndexIn_Y(this.id), l, false);
                    for (int x = getLowIndexIn_X(id) + 1; x < getHighIndexIn_X(id)-1 ; x += 1) {
                        //try to find a free position on the bottom boundary
                        vEnd = new Tupel(x, getLowIndexIn_Y(id), l, false);
                        if (isFree(vEnd, routingDirection, DOWN)) {
                            return makeReturnArg(vEnd, routingDirection, DOWN);
                        }
                    }
                }
            }
        }

        if(DEBUG)
        System.out.println("W-T-F - it's difficult to find a SIMPLE connection point for netID=" + rp.rs.getNetID() + " from " + start.toString() + " to " + end.toString());

        routingDirection = !routingDirection;

        if (routingDirection == X_DIRECTION) {
            //route in Xdirection
            if ((start.getX_InsideRoutingArray() <= end.getX_InsideRoutingArray())) {
                //we have to go to the right, because the start is on the left side of the end
                for (int l = 0; l < MAXLAYER; l++) {
                    if (!isCorrectOddEvenLayerForRoutingDirection(l, routingDirection)) {
                        continue;
                    }
                    for (int y = getLowIndexIn_Y(id) + 1; y < getHighIndexIn_Y(id)-1; y += 1) {
                        //try to find a free position on the right boundary
                        vEnd = new Tupel(getHighIndexIn_X(id), y, l, false);
                        if (isFree(vEnd, routingDirection, RIGHT)) {
                            return makeReturnArg(vEnd, routingDirection, RIGHT);
                        }
                    }
                }
            } else { //((start.getX_InsideRoutingArray() > end.getX_InsideRoutingArray()) ) {
                //we have to go to the left, because ... s.o.
                for (int l = 0; l < MAXLAYER; l++) {
                    if (!isCorrectOddEvenLayerForRoutingDirection(l, routingDirection)) {
                        continue;
                    }
                    //Tupel vEnd = new Tupel(getLowIndexIn_X(this.id), 0, l, false);
                    for (int y = getLowIndexIn_Y(id) + 1; y < getHighIndexIn_Y(id)-1 ; y += 1) {
                        //try to find a free position on the left boundary
                        vEnd = new Tupel(getLowIndexIn_X(id), y, l, false);
                        if (isFree(vEnd, routingDirection, LEFT)) {
                            return makeReturnArg(vEnd, routingDirection, LEFT);
                        }
                    }
                }
            }
        } else {
            //routingDirection == Y_DIRECTION;
            //route in Ydirection
            if ((start.getY_InsideRoutingArray() <= end.getY_InsideRoutingArray())) {
                //we have to go to the top, because ... s.o.
                for (int l = 0; l < MAXLAYER; l++) {
                    if (!isCorrectOddEvenLayerForRoutingDirection(l, routingDirection)) {
                        continue;
                    }
                    //Tupel vEnd = new Tupel(0, getHighIndexIn_Y(this.id), l, false);
                    for (int x = getLowIndexIn_X(id) + 1; x < getHighIndexIn_X(id)-1 ; x += 1) {
                        //try to find a free position on the top boundary
                        vEnd = new Tupel(x, getHighIndexIn_Y(id), l, false);
                        if (isFree(vEnd, routingDirection, UP)) {
                            return makeReturnArg(vEnd, routingDirection, UP);
                        }
                    }
                }
            } else { //((start.getY_InsideRoutingArray() > end.getY_InsideRoutingArray()) ) {
                //we have to go down, because ... s.o.
                for (int l = 0; l < MAXLAYER; l++) {
                    if (!isCorrectOddEvenLayerForRoutingDirection(l, routingDirection)) {
                        continue;
                    }
                    //Tupel vEnd = new Tupel(0, getLowIndexIn_Y(this.id), l, false);
                    for (int x = getLowIndexIn_X(id) + 1; x < getHighIndexIn_X(id)-1 ; x += 1) {
                        //try to find a free position on the bottom boundary
                        vEnd = new Tupel(x, getLowIndexIn_Y(id), l, false);
                        if (isFree(vEnd, routingDirection, DOWN)) {
                            return makeReturnArg(vEnd, routingDirection, DOWN);
                        }
                    }
                }
            }
        }

        if(DEBUG)
        System.out.println("NO WAY!!!: no connection point for netID=" + rp.rs.getNetID() + " from " + start.toString() + " to " + end.toString() + " free!");

        return new ConnectionPoints();
    }

    /**
     *
     * @param end the tupel which lies inside the region, whose middlepoint is wanted
     * @return gets the tupel, which represents the middlepoint of the region where the tupel lies inside
     */
    private Tupel getMiddlePoint(Tupel end) {
        return WorkerThread.getMiddlePoint(end);
    }

    /**
     *
     * @param layer on wich layer should the routing direction be checked
     * @param direction which direction do you think it is?
     * @return returns true, if the givin direction is really the routing direction of the given layer. otherwise false
     */
    private boolean isCorrectOddEvenLayerForRoutingDirection(int layer, boolean direction) {
        //direction==true => X-Dir
        //layers are counted from 0, so we have add 1 to get the first layer index 1
        if (direction == X_DIRECTION && (layer + 1) % 2 == 1) {
            return true; //odd layers route in x-direction
        }
        if (direction == Y_DIRECTION && (layer + 1) % 2 == 0) {
            return true; //even layer shall route in y-direction
        }
        return false;
    }

    /**
     *
     * @param pkt the tupel which is one side of the conntection points, but inside(!) the own region
     * @param direction in which direction should the second connectingPoint be created
     * @param range +1 if we have to go further in given direction to overgome the regionboundaries, -1 if we have to go one step back.
     * @return returns a valid connectionPoint object
     */
    private ConnectionPoints makeReturnArg(Tupel pkt, boolean direction, int range) {
        if (direction == X_DIRECTION) {
            //X-dir
            return new ConnectionPoints(new Tupel(pkt.getX_InsideRoutingArray(), pkt.getY_InsideRoutingArray(), pkt.getLayer(), false), new Tupel(pkt.getX_InsideRoutingArray() + range, pkt.getY_InsideRoutingArray(), pkt.getLayer(), false));
        } else {
            //Y-direction
            return new ConnectionPoints(new Tupel(pkt.getX_InsideRoutingArray(), pkt.getY_InsideRoutingArray(), pkt.getLayer(), false), new Tupel(pkt.getX_InsideRoutingArray(), pkt.getY_InsideRoutingArray() + range, pkt.getLayer(), false));
        }
    }

    /**
     *
     * @param pkt the tupel which is one side of the conntection points, but inside(!) the own region
     * @param direction direction in which direction should the second connectingPoint be created
     * @param range +1 if we have to go further in given direction to overgome the regionboundaries, -1 if we have to go one step back.
     * @return true, when both points are free, else false
     */
    private boolean isFree(Tupel pkt, boolean direction, int range) {
        if ( !(pkt.getX_InsideRoutingArray() > 0 && pkt.getX_InsideRoutingArray() < WorkerThread.size_x -1
        && pkt.getY_InsideRoutingArray() > 0 && pkt.getY_InsideRoutingArray() < WorkerThread.size_y -1
        && pkt.getLayer() >= 0 && pkt.getLayer() <= MAXLAYER) ) {
        return false;
        }
        if (direction == X_DIRECTION) {
            //X-dir
            return WorkerThread.regionBoundaries[pkt.getX_InsideRoutingArray()][pkt.getY_InsideRoutingArray()][pkt.getLayer()]
                    && WorkerThread.regionBoundaries[pkt.getX_InsideRoutingArray() + range][pkt.getY_InsideRoutingArray()][pkt.getLayer()];
        } else {
            //Y-direction
            return WorkerThread.regionBoundaries[pkt.getX_InsideRoutingArray()][pkt.getY_InsideRoutingArray()][pkt.getLayer()]
                    && WorkerThread.regionBoundaries[pkt.getX_InsideRoutingArray()][pkt.getY_InsideRoutingArray() + range][pkt.getLayer()];
        }
    }

    private static int getHighIndexIn_X(int rank) {
        return WorkPool.getHighIndexIn_X(rank);
    }

    private static int getHighIndexIn_Y(int rank) {
        return WorkPool.getHighIndexIn_Y(rank);
    }

    private static int getLowIndexIn_X(int rank) {
        return WorkPool.getLowIndexIn_X(rank);
    }

    private static int getLowIndexIn_Y(int rank) {
        return WorkPool.getLowIndexIn_Y(rank);
    }

    /**
     *
     * @param l from wich layer should the routing direction be returned?
     * @return the routing directino of the given layer (X_DIRECION or Y_DIRECTION)
     */
    private boolean getRoutingDirectionOnLayer(int l) {
        return isCorrectOddEvenLayerForRoutingDirection(l, X_DIRECTION) ? X_DIRECTION : Y_DIRECTION;
    }

    /**
     *
     * @param hop the start point for the next hop
     * @return the next hop with the following rules: if the current hop is negative, then a positive one is returned but with an absolute size one greater then before. is the current hop positive make the next one just as its negative value. Therefore we can alternate around a point in one dimension with a slowly growing absolut value
     */
    private int getNewHop(int hop) {
        if (hop < 0) {
            return (-hop) + 1;
        } else {
            return -hop;
        }
    }
}

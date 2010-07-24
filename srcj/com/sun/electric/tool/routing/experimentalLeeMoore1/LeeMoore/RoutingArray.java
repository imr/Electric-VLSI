/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RoutingArray.java
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
package com.sun.electric.tool.routing.experimentalLeeMoore1.LeeMoore;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.*;

import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.routing.RoutingFrame.RoutingGeometry;
import com.sun.electric.tool.routing.experimentalLeeMoore1.ThreadBorders;
import com.sun.electric.tool.routing.experimentalLeeMoore1.yana;

/**
 * class representing the routing grid
 * array value
 * =0					free field
 * =Integer.MIN_VALUE	field blocked
 * >0					field used while Lee-More algorithm
 * =-1					starting field
 * =-2					ending field
 */
public class RoutingArray {

    public int size_x, size_y, numLayers;
    private int[][][] array;
    final int ARRAY_OVERSIZE = 80;	// make array bigger than cell so that wires can be routed around
    final int NET_ID_OFFSET = 10;		// change net id when written into the array to avoid conflicts with start and end
    final int START_POINT = -1;
    final int END_POINT = -2;
    final int EMPTY_POINT = 0;
    final int RESERVED_POINT = Integer.MIN_VALUE + 1;
    final int BLOCKED_POINT = Integer.MIN_VALUE;
    double minWidth;
    private final boolean DEBUG = false;
    private final boolean USE_MULTI_TERMINAL_ALGORITHM=false;	//multiterminal algorithm is not yet doing correct wires in electric

    /**
     * set size of array and blocked tupels
     * @param size_x size in x direction
     * @param size_y size in y direction
     * @param numLayers size in z direction
     * @param blocked blocked tupels
     */
    public RoutingArray(int size_x, int size_y, int numLayers, Tupel[] blocked) {
        this.size_x = size_x;
        this.size_y = size_y;
        this.numLayers = numLayers;
        array = new int[size_x][size_y][numLayers];
        setBlocked(blocked);
    }

    /**
     * set size of array and blocked rectangles
     * @param c Cell
     * @param numLayers count of layers
     * @param blocked blocked RoutingGeometries
     * @param minWidth width of wires
     */
    public RoutingArray(Cell c, int numLayers, List<RoutingGeometry> blocked, double minWidth) {
        ERectangle r = c.getBounds();
        int distanceBetweenWires = (yana.distanceBetweenWires == 0) ? 3 : yana.distanceBetweenWires;
        this.size_x = (int) (r.getLambdaWidth() + ARRAY_OVERSIZE) / distanceBetweenWires;
        this.size_y = (int) (r.getLambdaHeight() + ARRAY_OVERSIZE) / distanceBetweenWires;
        this.numLayers = numLayers;
        this.minWidth = minWidth;
        if (DEBUG) System.out.println("scaling grid from " + (r.getLambdaWidth() + ARRAY_OVERSIZE) + " x " + (r.getLambdaHeight() + ARRAY_OVERSIZE) + " -> " + size_x + " x " + size_y
                + "\n with scaling factor=" + distanceBetweenWires + " and oversize=" + ARRAY_OVERSIZE);

        array = new int[size_x][size_y][numLayers];

        setBlocked(blocked);
    }

    /**
     * set array element to given value
     * @param t Tupel representing location
     * @param val value
     */
    private void setValue(Tupel t, int val) {
        int x = t.getX_InsideRoutingArray();
        int y = t.getY_InsideRoutingArray();
        int l = t.getLayer();

        array[x][y][l] = val;
    }
    
    /**
     * Returns array value
     * @param t Tupel representing location
     * @return value
     */
    private int getValue(Tupel t){
    	return array[t.getX_InsideRoutingArray()][t.getY_InsideRoutingArray()][t.getLayer()];
    }
    
    /**
     * set array element to given value and check borders so that a thread can't set values that do not belong to its WorkPartition
     * @param t Tupel representing location
     * @param val
     * @param tb
     */
    private void setValue(Tupel t, int val, ThreadBorders tb) {
        int x = t.getX_InsideRoutingArray();
        int y = t.getY_InsideRoutingArray();
        int l = t.getLayer();
        if (DEBUG&&(!(tb.getLowIndexX() <= x && x <= tb.getHighIndexX() && tb.getLowIndexY() <= y && y <= tb.getHighIndexY()))) {
            System.out.println("Thread out of bounds!");
        }
        array[x][y][l] = val;
    }

    /**
     * mark tupels in array as blocked
     * @param blocked blocked tupels
     */
    public void setBlocked(Tupel[] blocked) {
        for (int i = 0; i < blocked.length; i++) {
            setValue(blocked[i], BLOCKED_POINT);
        }
    }

    /**
     * mark tupels as used using val
     * @param route tupels to mark
     * @param val value to use for marking
     */
    private void markUsed(Tupel[] route, int val) {
        if (DEBUG) {
            System.out.println("Marking route as used with " + val);
        }
        for (int i = 0; i < route.length; i++) {
            setValue(route[i], val);
        }
    }

    /**
     * Reserves Tupels for routing. Tupels reserved by two nets are marked as blocked.
     * @param tupels Tupels to reserve
     * @param netID segment which reserves tupels
     */
    public void reserveForRouting(Tupel[] tupels, int netID) {
        for (Tupel tupel : tupels) {
            int x = tupel.getX_InsideRoutingArray();
            int y = tupel.getY_InsideRoutingArray();
            int l = tupel.getLayer();
            if (array[x][y][l] == 0 || array[x][y][l] == netID) {
                setValue(tupel, netID);
            } else {
                setValue(tupel, BLOCKED_POINT);
            }
        }
    }

    /**
     *
     * @return a list of the blocked and reserved tupels
     */
    public ArrayList<Tupel> getBlocked() {
        ArrayList<Tupel> blocked = new ArrayList<Tupel>();
        for (int i = 0; i < size_x; i++) {
            for (int j = 0; j < size_y; j++) {
                for (int k = 0; k < numLayers; k++) {
                    if (array[i][j][k] < 0) {
                        blocked.add(new Tupel(i, j, k, false));
                    }
                }
            }
        }
        return blocked;
    }

    /**
     * Set RoutingGeometries as blocked
     * @param blockages routing geometries
     */
    public void setBlocked(List<RoutingGeometry> blockages) {
        for (RoutingGeometry rg : blockages) {
            setBlocked(grow(rg.getBounds(), minWidth), rg.getLayer().getMetalNumber() - 1);
        }
    }

   /**
    * Grows a rectangle by a given size.
    * @param r Rectangle
    * @param size size to grow
    * @return grown rectangle
    */
    private Rectangle2D grow(Rectangle2D r, double size) {
        double spacing = size / 2;
        int x = (int) (r.getX() - spacing);
        int y = (int) (r.getY() - spacing);
        int width = (int) Math.floor(r.getWidth() + size);
        int height = (int) Math.floor(r.getHeight() + size);
        return new Rectangle(x, y, width, height);
    }

    /**
     * Mark rectangle in array as blocked. Sets only the outer shape as blocked, as inner elements can't be reached
     * @param r Rectangle
     * @param layer Layer cotaining blockage
     */
    private void setBlocked(Rectangle2D r, int layer) {
        int x = (int) r.getX();
        int y = (int) r.getY();
        int width = (int) Math.floor(r.getWidth());
        int height = (int) Math.floor(r.getHeight());
        List<Tupel> blocked = new ArrayList<Tupel>();

        int h_border = x + width;
        int v_border = y + height;

        //horizontal borders
        for (int i = x; i < h_border; i++) {
            blocked.add(new Tupel(i, y, layer, true));
            blocked.add(new Tupel(i, v_border - 1, layer, true));
        }

        //vertical borders
        for (int i = y; i < v_border; i++) {
            blocked.add(new Tupel(x, i, layer, true));
            blocked.add(new Tupel(h_border - 1, i, layer, true));
        }
        setBlocked(blocked.toArray(new Tupel[0]));
    }

    /**
     * Mark a found route as blocked.
     * @param r found route
     * @param netID id of route
     */
    private void setBlocked(Route r, int netID) {
        if (r == null) {
            return;
        }
        List<Tupel> l = r.getRoutingList();
        markUsed(l.toArray(new Tupel[0]), -1 * (netID + NET_ID_OFFSET));
    }

    /**
     * Mark points in array around start and end as reserved
     * @param start start tupel
     * @param end end tupel
     * @param border ThreadBorders
     */
    private void createBlockingsAroundStartEnd(Tupel start,Tupel end,ThreadBorders border) {
        if(isStartEndTupel(start)){
        	if(getLeftNeighbour(start, border)==EMPTY_POINT){
	    		setValue(leftNeighbour(start),RESERVED_POINT,border);
	    	}
	    	if(getRightNeighbour(start, border)==EMPTY_POINT){
	    		setValue(rightNeighbour(start),RESERVED_POINT,border);
	    	}
	    	if(getTopNeighbour(start, border)==EMPTY_POINT){
	    		setValue(topNeighbour(start),RESERVED_POINT,border);
	    	}
			if(getBottomNeighbour(start, border)==EMPTY_POINT){
				setValue(bottomNeighbour(start),RESERVED_POINT,border);
			}
        }
        if (isStartEndTupel(end)) {
            if (getLeftNeighbour(end, border) == EMPTY_POINT) {
                setValue(leftNeighbour(end), RESERVED_POINT, border);
            }
            if (getRightNeighbour(end, border) == EMPTY_POINT) {
                setValue(rightNeighbour(end), RESERVED_POINT, border);
            }
            if (getTopNeighbour(end, border) == EMPTY_POINT) {
                setValue(topNeighbour(end), RESERVED_POINT, border);
            }
            if (getBottomNeighbour(end, border) == EMPTY_POINT) {
                setValue(bottomNeighbour(end), RESERVED_POINT, border);
            }
        }
    }


    /**
     *  delete all temporary created routing values
     * @param tb ThreadBorders
     */
    private void clearRouting(ThreadBorders tb) {
        for (int i = tb.getLowIndexX(); i <= tb.getHighIndexX(); i++) {
            for (int j = tb.getLowIndexY(); j <= tb.getHighIndexY(); j++) {
                for (int k = 0; k < numLayers; k++) {
                    if (array[i][j][k] > 0) {
                        array[i][j][k] = EMPTY_POINT;
                    } else if (array[i][j][k] < 0 && array[i][j][k] > -3) {
                        //start and end point may not be used
                        array[i][j][k] = BLOCKED_POINT;
                    }
                }
            }
        }
        //checkClearing(tb);
    }

//    //DEBUG
//    private void checkClearing(ThreadBorders tb) {
//        for (int i = tb.getLowIndexX(); i <= tb.getHighIndexX(); i++) {
//            for (int j = tb.getLowIndexY(); j <= tb.getHighIndexY(); j++) {
//                for (int k = 0; k < numLayers; k++) {
//                    if (array[i][j][k] == END_POINT || array[i][j][k] == START_POINT || array[i][j][k] > EMPTY_POINT) {
//                    	if(DEBUG)System.out.println("Clearing not successful!");
//                    }
//                }
//            }
//        }
//    }

    /**
     * this method is only used for compability with single threaded algorithm
     * @param start
     * @param end
     * @param netID
     * @return
     */
    @Deprecated
    public Route route(Tupel start, Tupel end, int netID) {
        ThreadBorders border = new ThreadBorders(0, array.length - 1, 0, array[0].length - 1);
        return route(start, end, border, netID);
    }

	/**
	 * Start routing between start and end in given borders
	 * @param start start point
	 * @param end end point
	 * @param border border to route in
	 * @param netID id of the route
	 * @return
	 */
    public Route route(Tupel start, Tupel end, ThreadBorders border, int netID) {
    	
        if(start.isEqualPosition(end)){
            //TODO: isEqualPosition doesn't check on same layer. return null too conservative?
        	if(DEBUG)System.out.println("START==END! => Length of wire==0...");
            return null;
        }

        boolean useMultiTerminalAlgorithm;
        if (USE_MULTI_TERMINAL_ALGORITHM) {
            if (getValue(start) > RESERVED_POINT) {
                //start point was already routed once, multiterminal algorithm can not use it as start point
                if (getValue(end) > RESERVED_POINT) {
                    //same for end point, so don't use multiterminal algorithm
                    useMultiTerminalAlgorithm = false;
                } else {
                    //switch start and end point
                    Tupel temp = start;
                    start = end;
                    end = temp;
                    useMultiTerminalAlgorithm = true;
                }
            } else {
                //none of the points was already routed once so don't use multiterminal algorithm to make sure that both will be connected
                useMultiTerminalAlgorithm = false;
            }
        } else {
            useMultiTerminalAlgorithm = false;
        }

        prepareStartEndPoint(start, end, border);

        Queue<Tupel> start_queue = new LinkedList<Tupel>();
        start_queue.offer(start);
        start_queue.offer(null);
        SearchResult res = search(start_queue, border, end, -1 * (netID + NET_ID_OFFSET), useMultiTerminalAlgorithm);
        int length = res.getLength();
        Tupel foundEnd = res.getFoundEnd();
        if (length >= 0) {
            Route r = new Route();
            r.setReversed(useMultiTerminalAlgorithm);
            //create a new Tupel out of routing array coordinates so that wiring algorithm can calculate rectangular wires to the end point
            r.addFieldInFront(new Tupel(foundEnd.getX_InsideRoutingArray(), foundEnd.getY_InsideRoutingArray(), foundEnd.getLayer(), false));
            r = trace(r, length, border);
            setBlocked(r, netID);
            if (DEBUG) {
                r.printRoute(r.getEdgePoints());
            }
            clearRouting(border);
            createBlockingsAroundStartEnd(start, end, border);	//this is only needed if route was found
            return r;
        } else {
            if (DEBUG) {
                System.out.println("Routing from " + start.toString() + " to " + end.toString() + " (netID=" + netID + ") in borders " + border.toString() + " has no solution!");
            }
            clearRouting(border);
            return null;
        }
    }
    /**
     * Mark points around start and end as free if they are reserved.
     * @param start start point
     * @param end end point
     * @param border border of routing
     */
    private void prepareStartEndPoint(Tupel start, Tupel end, ThreadBorders border) {
        setValue(start, START_POINT, border);
        //prepare neighbours (were marked as reserved) if start point is original start point of a routing segment
        if (isStartEndTupel(start)) {
            if (getLeftNeighbour(start, border) == RESERVED_POINT) {
                setValue(leftNeighbour(start), EMPTY_POINT, border);
            }
            if (getRightNeighbour(start, border) == RESERVED_POINT) {
                setValue(rightNeighbour(start), EMPTY_POINT, border);
            }
            if (getTopNeighbour(start, border) == RESERVED_POINT) {
                setValue(topNeighbour(start), EMPTY_POINT, border);
            }
            if (getBottomNeighbour(start, border) == RESERVED_POINT) {
                setValue(bottomNeighbour(start), EMPTY_POINT, border);
            }
        }
    	setValue(end, END_POINT, border);
    	//same for end point
    	if(isStartEndTupel(end)){
	    	if(getLeftNeighbour(end, border)==RESERVED_POINT){
	    		setValue(leftNeighbour(end),EMPTY_POINT,border);
	    	}
	    	if(getRightNeighbour(end, border)==RESERVED_POINT){
	    		setValue(rightNeighbour(end),EMPTY_POINT,border);
	    	}
	    	if(getTopNeighbour(end, border)==RESERVED_POINT){
	    		setValue(topNeighbour(end),EMPTY_POINT,border);
	    	}
			if(getBottomNeighbour(end, border)==RESERVED_POINT){
				setValue(bottomNeighbour(end),EMPTY_POINT,border);
			}
    	}
	}
    
    /**
     * Return whether the given tupel is a start or end point.
     * In this case the conversion between Electric coordinates to routing array coordinates and back again should give another result.
     * @param t Tupel to check
     * @return whether Tupel t is a start or end point of a routing segment
     */
    private boolean isStartEndTupel(Tupel t){
    	return (t.getX_InsideElectric()!=Tupel.convertRoutingArrayToElectricCoordinates_X(t.getX_InsideRoutingArray()))
			||t.getY_InsideElectric()!=Tupel.convertRoutingArrayToElectricCoordinates_Y(t.getY_InsideRoutingArray());
    }

	/**
	 * Trace up route marked by search
	 * @param r route to save the result in
	 * @param length length of the route to find
	 * @param b borders to trace in
	 * @return
	 */
    private Route trace(Route r, int length, ThreadBorders b) {
        if (DEBUG) {
            System.out.println("Length: " + length);
        }
        while (true) {
            Tupel t = r.getFirstTupel();
            // starting field is reached
            if (length == -2) {
                return r;
            }

            // adapt search value for search of starting element
            if (length == 0) {
                length = START_POINT;
            }

            if ((t.getLayer() % 2) == 0) {
                // check neighbours
                // left neighbour
                if (getLeftNeighbour(t, b) == length) {
                    r.addFieldInFront(leftNeighbour(t));
                    length--;
                    continue;
                }

                // right neighbour
                if (getRightNeighbour(t, b) == length) {
                    r.addFieldInFront(rightNeighbour(t));
                    length--;
                    continue;
                }
            } else {
                // top neighbour
                if (getTopNeighbour(t, b) == length) {
                    r.addFieldInFront(topNeighbour(t));
                    length--;
                    continue;
                }

                // lower neighbour
                if (getBottomNeighbour(t, b) == length) {
                    r.addFieldInFront(bottomNeighbour(t));
                    length--;
                    continue;
                }
            }
            // layer up neighbour
            if (getLayerUpNeighbour(t) == length) {
                r.addFieldInFront(layerUpNeighbour(t));
                length--;
                continue;
            }

            // layer down neighbour
            if (getLayerDownNeighbour(t) == length) {
                r.addFieldInFront(layerDownNeighbour(t));
                length--;
                continue;
            }

            //no neighbour was found
            if(DEBUG)System.out.println("ERROR IN TRACE: no neighbour was found!");
            printNeighbourhood(b, t);
            if(DEBUG)System.out.println("Route so far: ");
            if(DEBUG)r.printRoute();
            //printArrayNonZero(b,length);
            if(DEBUG)System.out.println();
        }
    }

    //DEBUG
    private void printNeighbourhood(ThreadBorders b, Tupel t) {
    	if(DEBUG) return;
        System.out.println("Neighbourhood around " + t);
        System.out.println("left: " + getLeftNeighbour(t, b));
        System.out.println("right: " + getRightNeighbour(t, b));
        System.out.println("top: " + getTopNeighbour(t, b));
        System.out.println("bottom: " + getBottomNeighbour(t, b));
        System.out.println("layerup: " + getLayerUpNeighbour(t));
        System.out.println("layerdown: " + getLayerDownNeighbour(t));
        System.out.println("self: " + array[t.getX_InsideRoutingArray()][t.getY_InsideRoutingArray()][t.getLayer()]);
    }

    /**
     * Search for a route to tupel end.
     * @param queue queue already containing start tupel
     * @param b borders in which it should search
     * @param end tupel to find
     * @param netID id oft the net
     * @param multiTerminal use multiterminal algorithm
     * @return SearchResult (length and found tupel)
     */
    private SearchResult search(Queue<Tupel> queue, ThreadBorders b, Tupel end, int netID, boolean multiTerminal) {
        int val = 1;
        Tupel foundEnd = null;
        boolean found = false;
        while (!found) {
            Tupel t = queue.poll();

            //null object means that value has to be incremented
            if (t == null) {
                if (queue.size() == 0) {
                    //System.out.println("No solution found!");
                    return new SearchResult(end, -1);
                }
                queue.offer(null);
                val++;
                //printArray();
                continue;
            }

            int neighbourValue;
            if ((t.getLayer() % 2) == 0) {
                //check neighbours
                //left neighbour
                neighbourValue = getLeftNeighbour(t, b);
                switch (neighbourValue) {
                    case START_POINT:
                        break;		//starting point: do nothing
                    case END_POINT:				//end point: route found
                        found = true;
                        foundEnd = leftNeighbour(t);
                        break;
                    case EMPTY_POINT:
                        Tupel neighbour = leftNeighbour(t);
                        queue.offer(neighbour);
                        setValue(neighbour, val, b);	//mark as visited
                        break;
                    default:
                        if (multiTerminal && neighbourValue == netID) {
                            found = true;
                            foundEnd = leftNeighbour(t);
                        }
                        break;		//already visited or blocked
                }
                //right neighbour
                neighbourValue = getRightNeighbour(t, b);
                switch (neighbourValue) {
                    case START_POINT:
                        break;		//starting point: do nothing
                    case END_POINT:					//end point: route found
                        found = true;
                        foundEnd = rightNeighbour(t);
                        break;
                    case EMPTY_POINT:
                        Tupel neighbour = rightNeighbour(t);
                        queue.offer(neighbour);
                        setValue(neighbour, val, b);	//mark as visited
                        break;
                    default:
                        if (multiTerminal && neighbourValue == netID) {
                            found = true;
                            foundEnd = rightNeighbour(t);
                        }
                        break;		//already visited or blocked
                }
            } else {
                //top neighbour
                neighbourValue = getTopNeighbour(t, b);
                switch (neighbourValue) {
                    case START_POINT:
                        break;		//starting point: do nothing
                    case END_POINT:					//end point: route found
                        found = true;
                        foundEnd = topNeighbour(t);
                        break;
                    case EMPTY_POINT:
                        Tupel neighbour = topNeighbour(t);
                        queue.offer(neighbour);
                        setValue(neighbour, val, b);	//mark as visited
                        break;
                    default:
                        if (multiTerminal && neighbourValue == netID) {
                            found = true;
                            foundEnd = topNeighbour(t);
                        }
                        break;		//already visited or blocked
                }
                //lower neighbour
                neighbourValue = getBottomNeighbour(t, b);
                switch (neighbourValue) {
                    case START_POINT:
                        break;		//starting point: do nothing
                    case END_POINT:					//end point: route found
                        found = true;
                        foundEnd = bottomNeighbour(t);
                        break;
                    case EMPTY_POINT:
                        Tupel neighbour = bottomNeighbour(t);
                        queue.offer(neighbour);
                        setValue(neighbour, val, b);	//mark as visited
                        break;
                    default:
                        if (multiTerminal && neighbourValue == netID) {
                            found = true;
                            foundEnd = bottomNeighbour(t);
                        }
                        break;		//already visited or blocked
                }
            }
            //layer up neighbour
            neighbourValue = getLayerUpNeighbour(t);
            switch (neighbourValue) {
                case START_POINT:
                    break;		//starting point: do nothing
                case END_POINT:					//end point: route found
                    found = true;
                    foundEnd = layerUpNeighbour(t);
                    break;
                case EMPTY_POINT:
                    Tupel neighbour = layerUpNeighbour(t);
                    queue.offer(neighbour);
                    setValue(neighbour, val, b);	//mark as visited
                    break;
                default:
                    if (multiTerminal && neighbourValue == netID) {
                        found = true;
                        foundEnd = layerUpNeighbour(t);
                    }
                    break;		//already visited or blocked
            }
            //layer down neighbour
            neighbourValue = getLayerDownNeighbour(t);
            switch (neighbourValue) {
                case START_POINT:
                    break;		//starting point: do nothing
                case END_POINT:					//end point: route found
                    found = true;
                    foundEnd = layerDownNeighbour(t);
                    break;
                case EMPTY_POINT:
                    Tupel neighbour = layerDownNeighbour(t);
                    queue.offer(neighbour);
                    setValue(neighbour, val, b);	//mark as visited
                    break;
                default:
                    if (multiTerminal && neighbourValue == netID) {
                        found = true;
                        foundEnd = layerDownNeighbour(t);
                    }
                    break;		//already visited or blocked
            }
            if (found) {
                if (!foundEnd.isEqual(end)) {
                	if(DEBUG)System.out.println("INFO: Multiterminal algorithm used.");
                }
                if (val == 1) {
                	if(DEBUG)System.out.println("ERROR: Length of route is 0!");
                }
                return new SearchResult(foundEnd, val - 1);
            }
        }
        if(DEBUG)System.out.println("ERROR: Reached anreachable statement!");
        //should never be reached
        return new SearchResult(end, -1);

    }

    /**
     * Return value of left neighbour
     * @param t tupel which neighbours to check
     * @param b borders
     * @return value of left neighbour or blocked if out of borders
     */
    private int getLeftNeighbour(Tupel t, ThreadBorders b) {
        int x = t.getX_InsideRoutingArray();
        int y = t.getY_InsideRoutingArray();
        int l = t.getLayer();

        if (x > b.getLowIndexX()) {
            return array[x - 1][y][l];
        } else {
            return BLOCKED_POINT;
        }
    }

    /**
     * Return value of right neighbour
     * @param t tupel which neighbours to check
     * @param b borders
     * @return value of right neighbour or blocked if out of borders
     */
    private int getRightNeighbour(Tupel t, ThreadBorders b) {
        int x = t.getX_InsideRoutingArray();
        int y = t.getY_InsideRoutingArray();
        int l = t.getLayer();

        if (x < b.getHighIndexX()) {
            return array[x + 1][y][l];
        } else {
            return BLOCKED_POINT;
        }
    }

    /**
     * Return value of bottom neighbour
     * @param t tupel which neighbours to check
     * @param b borders
     * @return value of bottom neighbour or blocked if out of borders
     */
    private int getBottomNeighbour(Tupel t, ThreadBorders b) {
        int x = t.getX_InsideRoutingArray();
        int y = t.getY_InsideRoutingArray();
        int l = t.getLayer();

        if (y > b.getLowIndexY()) {
            return array[x][y - 1][l];
        } else {
            return BLOCKED_POINT;
        }
    }

    /**
     * Return value of top neighbour
     * @param t tupel which neighbours to check
     * @param b borders
     * @return value of top neighbour or blocked if out of borders
     */
    private int getTopNeighbour(Tupel t, ThreadBorders b) {
        int x = t.getX_InsideRoutingArray();
        int y = t.getY_InsideRoutingArray();
        int l = t.getLayer();

        if (y < b.getHighIndexY()) {
            return array[x][y + 1][l];
        } else {
            return BLOCKED_POINT;
        }
    }

    /**
     * Return value of layer up neighbour
     * @param t tupel which neighbours to check
     * @param b borders
     * @return value of layer up neighbour or blocked if out of borders
     */
    private int getLayerUpNeighbour(Tupel t) {
        int x = t.getX_InsideRoutingArray();
        int y = t.getY_InsideRoutingArray();
        int l = t.getLayer();

        if (l < array[0][0].length - 1) {
            return array[x][y][l + 1];
        } else {
            return BLOCKED_POINT;
        }
    }

    /**
     * Return value of layer down neighbour
     * @param t tupel which neighbours to check
     * @param b borders
     * @return value of layer down neighbour or blocked if out of borders
     */
    private int getLayerDownNeighbour(Tupel t) {
        int x = t.getX_InsideRoutingArray();
        int y = t.getY_InsideRoutingArray();
        int l = t.getLayer();

        if (l > 0) {
            return array[x][y][l - 1];
        } else {
            return BLOCKED_POINT;
        }
    }

    /**
     * Return tupel left to the given one
     * @param t Tupel
     * @return left neighbour
     */
    private Tupel leftNeighbour(Tupel t) {
        return new Tupel(t.getX_InsideRoutingArray() - 1, t.getY_InsideRoutingArray(), t.getLayer(), false);
    }

    /**
     * Return tupel right to the given one
     * @param t Tupel
     * @return right neighbour
     */
    private Tupel rightNeighbour(Tupel t) {
        return new Tupel(t.getX_InsideRoutingArray() + 1, t.getY_InsideRoutingArray(), t.getLayer(), false);
    }

    /**
     * Return upper tupel to the given one
     * @param t Tupel
     * @return top neighbour
     */
    private Tupel topNeighbour(Tupel t) {
        return new Tupel(t.getX_InsideRoutingArray(), t.getY_InsideRoutingArray() + 1, t.getLayer(), false);
    }

    /**
     * Return lower tupel to the given one
     * @param t Tupel
     * @return bottom neighbour
     */
    private Tupel bottomNeighbour(Tupel t) {
        return new Tupel(t.getX_InsideRoutingArray(), t.getY_InsideRoutingArray() - 1, t.getLayer(), false);
    }

    /**
     * Return tupel a layer up to the given one
     * @param t Tupel
     * @return layer up neighbour
     */
    private Tupel layerUpNeighbour(Tupel t) {
        return new Tupel(t.getX_InsideRoutingArray(), t.getY_InsideRoutingArray(), t.getLayer() + 1, false);
    }

    /**
     * Return tupel a layer down to the given one
     * @param t Tupel
     * @return layer down neighbour
     */
    private Tupel layerDownNeighbour(Tupel t) {
        return new Tupel(t.getX_InsideRoutingArray(), t.getY_InsideRoutingArray(), t.getLayer() - 1, false);
    }
    
    /**
     * This class represents a result returned by search. It contains the length and the found tupel.
     * (This tupel is only different if multiterminal is true, which is not by default)
     * @author Andreas
     *
     */
    class SearchResult{
    	Tupel foundEnd;
    	int length;
		public Tupel getFoundEnd() {
			return foundEnd;
		}
		public int getLength() {
			return length;
		}
		public SearchResult(Tupel foundEnd, int length) {
			super();
			this.foundEnd = foundEnd;
			this.length = length;
		}
    }

    /**
     * Converts initial reservations of start and end points with their netID to the representation needed by algorithm.
     */
	public void markReserved() {
		 for (int i = 0; i < array.length; i++) {
	            for (int j = 0; j < array[0].length; j++) {
	                for (int k = 0; k < numLayers; k++) {
	                	if (array[i][j][k] > 0) {
	                        array[i][j][k] = RESERVED_POINT;
	                    }
	                }
	            }
	        }
	}
}

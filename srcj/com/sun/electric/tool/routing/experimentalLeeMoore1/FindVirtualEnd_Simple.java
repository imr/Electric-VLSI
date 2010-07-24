/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FindVirtualEnd_Simple.java
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

/**
 *
 * @author Sonny
 */
public class FindVirtualEnd_Simple implements FindVirtualEndInterface {

    private static boolean X_DIRECTION; //const
    private static boolean Y_DIRECTION; //const
    private static int MAXLAYER;

    public FindVirtualEnd_Simple(boolean xdir, boolean ydir, int l) {
        X_DIRECTION = xdir;
        Y_DIRECTION = ydir;
        MAXLAYER = l;
    }

    public synchronized ConnectionPoints findVirtualEnd(RoutingPart rp, int id) {
        Tupel start = rp.start;
        Tupel ende = rp.end;

        /*current algo:
         * go to the boundary in this direction, which is shorter to the end_point
         * at the boundary try to find a free point to use
         * try this for all layers
         */
        boolean routingDirection;
        if (Math.abs(getMiddlePoint(start).getX_InsideRoutingArray() - getMiddlePoint(ende).getX_InsideRoutingArray())
                >= Math.abs(getMiddlePoint(start).getY_InsideRoutingArray() - getMiddlePoint(ende).getY_InsideRoutingArray())) {
            routingDirection = X_DIRECTION;
            //route in Xdirection because its longer (on the x sided boundary)
            if ((start.getX_InsideRoutingArray() <= ende.getX_InsideRoutingArray())) {
                //we have to go to the right, because the start is on the left side of the end
                for (int l = 0; l < MAXLAYER; l++) {
                    if (!isCorrectOddEvenLayerForRoutingDirection(l, routingDirection)) {
                        continue;
                    }
                    //Tupel vEnd = new Tupel(getHighIndexIn_X(this.id), 0, l, false);
                    for (int y = getLowIndexIn_Y(id) + 1; y < getHighIndexIn_Y(id) - 1; y += 10) {
                        //try to find a free position on the right boundary
                        Tupel vEnd = new Tupel(getHighIndexIn_X(id), y, l, false);
                        if (isFree(vEnd)) {
                            return makeReturnArg(vEnd, routingDirection, 1);
                        }
                    }
                }
            } else { //((start.getX_InsideRoutingArray() > ende.getX_InsideRoutingArray()) ) {
                //we have to go to the left, because ... s.o.
                for (int l = 0; l < MAXLAYER; l++) {
                    if (!isCorrectOddEvenLayerForRoutingDirection(l, routingDirection)) {
                        continue;
                    }
                    //Tupel vEnd = new Tupel(getLowIndexIn_X(this.id), 0, l, false);
                    for (int y = getLowIndexIn_Y(id) + 1; y < getHighIndexIn_Y(id) - 1; y += 10) {
                        //try to find a free position on the left boundary
                        Tupel vEnd = new Tupel(getLowIndexIn_X(id), y, l, false);
                        if (isFree(vEnd)) {
                            return makeReturnArg(vEnd, routingDirection, -1);
                        }
                    }
                }
            }
        } else {
            routingDirection = Y_DIRECTION;
            //route in Ydirection, because its longer
            if ((start.getY_InsideRoutingArray() <= ende.getY_InsideRoutingArray())) {
                //we have to go to the top, because ... s.o.
                for (int l = 0; l < MAXLAYER; l++) {
                    if (!isCorrectOddEvenLayerForRoutingDirection(l, routingDirection)) {
                        continue;
                    }
                    //Tupel vEnd = new Tupel(0, getHighIndexIn_Y(this.id), l, false);
                    for (int x = getLowIndexIn_X(id) + 1; x < getHighIndexIn_X(id) - 1; x += 10) {
                        //try to find a free position on the top boundary
                        Tupel vEnd = new Tupel(x, getHighIndexIn_Y(id), l, false);
                        if (isFree(vEnd)) {
                            return makeReturnArg(vEnd, routingDirection, 1);
                        }
                    }
                }
            } else { //((start.getY_InsideRoutingArray() > ende.getY_InsideRoutingArray()) ) {
                //we have to go down, because ... s.o.
                for (int l = 0; l < MAXLAYER; l++) {
                    if (!isCorrectOddEvenLayerForRoutingDirection(l, routingDirection)) {
                        continue;
                    }
                    //Tupel vEnd = new Tupel(0, getLowIndexIn_Y(this.id), l, false);
                    for (int x = getLowIndexIn_X(id) + 1; x < getHighIndexIn_X(id) - 1; x += 10) {
                        //try to find a free position on the bottom boundary
                        Tupel vEnd = new Tupel(x, getLowIndexIn_Y(id), l, false);
                        if (isFree(vEnd)) {
                            return makeReturnArg(vEnd, routingDirection, -1);
                        }
                    }
                }
            }
        }
        //TODO: no virtual connecting point was found in the desired routing direction, so try to go another to another side of your region
        return new ConnectionPoints();
    }

    private Tupel getMiddlePoint(Tupel end) {
        return WorkerThread.getMiddlePoint(end);
    }

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

    private ConnectionPoints makeReturnArg(Tupel pkt, boolean direction, int range) {
        if (direction == X_DIRECTION) {
            //X-dir
            return new ConnectionPoints(new Tupel(pkt.getX_InsideRoutingArray(), pkt.getY_InsideRoutingArray(), pkt.getLayer(), false), new Tupel(pkt.getX_InsideRoutingArray() + range, pkt.getY_InsideRoutingArray(), pkt.getLayer(), false));
        } else {
            //Y-direction
            return new ConnectionPoints(new Tupel(pkt.getX_InsideRoutingArray(), pkt.getY_InsideRoutingArray(), pkt.getLayer(), false), new Tupel(pkt.getX_InsideRoutingArray(), pkt.getY_InsideRoutingArray() + range, pkt.getLayer(), false));
        }
    }

    private boolean isFree(Tupel t) {
        if (t.getX_InsideRoutingArray() >= 0 && t.getX_InsideRoutingArray() < WorkerThread.size_x
                && t.getY_InsideRoutingArray() >= 0 && t.getY_InsideRoutingArray() < WorkerThread.size_y
                && t.getLayer() >= 0 && t.getLayer() <= MAXLAYER) {
            return WorkerThread.regionBoundaries[t.getX_InsideRoutingArray()][t.getY_InsideRoutingArray()][t.getLayer()];
        } else {
            //point not inside the grid
            return false;
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
}

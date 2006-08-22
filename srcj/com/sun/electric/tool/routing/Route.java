/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Route.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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

package com.sun.electric.tool.routing;

import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.technology.PrimitiveNode;

import java.util.*;

/**
 * Specifies a route to be created.  Note that the order if items
 * in a route is meaningless.  The only thing that specifies order is the
 * start and end of the route.
 * <p>
 * Author: gainsley
 */

public class Route extends ArrayList<RouteElement> {

    private RouteElementPort routeStart;       // start of route
    private RouteElementPort routeEnd;         // end of route
    private boolean routeReversed;             // if the route has been reversed

    // ---------------------- Constructors ---------------------------

    /** Constructs an empty route */
    public Route() {
        super();
        routeStart = null;
        routeEnd = null;
        routeReversed = false;
    }

    /** Constructs a route containing the elements of the passed route,
     * in the order they are returned by the route iterator, and having
     * the same start and end RouteElement (if Collection is a Route).
     */
    public Route(Collection<RouteElement> c) {
        super(c);
        if (c instanceof Route) {
            Route r = (Route)c;
            routeStart = r.getStart();
            routeEnd = r.getEnd();
        } else {
            routeStart = null;
            routeEnd = null;
        }
    }

    // ------------------------------- Route Methods -----------------------------------

//    public RouteElement get(int index) {
//        // return get(index);
//        int count = 0;
//        for (Iterator<RouteElement> it = iterator(); it.hasNext();)
//        {
//            if (count == index) return it.next();
//            count++;
//        }
//        return null;
//    }

    /** Sets the start of the Route */
    public void setStart(RouteElementPort startRE) {
        if (!contains(startRE)) {
            add(startRE);
            //System.out.println("Route.setStart Error: argument not part of list");
            //return;
        }
        routeStart = startRE;
    }

    /** Get the start of the Route */
    public RouteElementPort getStart() { return routeStart; }

    /** Sets the end of the Route */
    public void setEnd(RouteElementPort endRE) {
        if (!contains(endRE)) {
            add(endRE);
            //System.out.println("Route.setEnd Error: argument not part of list");
            //return;
        }
        routeEnd = endRE;
    }

    /** Get the end of the Route */
    public RouteElementPort getEnd() { return routeEnd; }

    /**
     * Reverse the Route. This just swaps and the start and end
     * RouteElements, because the order of the list does not matter.
     */
    public void reverseRoute() {
        RouteElementPort re = routeStart;
        routeStart = routeEnd;
        routeEnd = re;
        routeReversed = !routeReversed;
    }

    /** True if the route is reversed, false if it is not reversed */
    public boolean isRouteReversed() { return routeReversed; }

    /**
     * Attempts to replace pin with replacement. See replaceBisectPin and
     * replaceExistingRedundantPin for details.
     * @param pin the pin to replace
     * @param replacement the replacement
     * @return true if any replacement done, false otherwise.
     */
    public boolean replacePin(RouteElementPort pin, RouteElementPort replacement) {
        if (replaceBisectPin(pin, replacement)) return true;
        if (replaceExistingRedundantPin(pin, replacement)) return true;
        return false;
    }

    /**
     * Attempts to replace the bisectPin by replacement. Returns true
     * if any replacements done, and bisect pin is no longer used.
     * otherwise returns false. This method currently requires both
     * bisectPin and replacement to be part of this Route when this
     * method is called.
     * @param bisectPin the port pin to replace
     * @param replacement the port pin to replace bisectPin with.
     * @return true if any replacements done and bisectPin no longer used,
     * false otherwise.
     */
    public boolean replaceBisectPin(RouteElementPort bisectPin, RouteElementPort replacement) {
        if (!bisectPin.isBisectArcPin()) return false;
        assert(contains(bisectPin));

        boolean success = true;
        for (Iterator<RouteElement> it = iterator(); it.hasNext(); ) {
            RouteElement re = it.next();
            if (re instanceof RouteElementArc) {
                RouteElementArc reArc = (RouteElementArc)re;
                if (!reArc.replaceArcEnd(bisectPin, replacement))
                    success = false;            // reArc still contains reference to bisectPin
            }
        }
        return success;
    }

    /**
     * Attempts to replace an existing pin that has been made redundant by
     * some node in the route, such as a contact cut.  If replacable, all
     * arcs that used to connect to the pin will connect to the replacement node.
     * Note that this does not remove pinRE from the route, nor does it add
     * replacementRE.
     * @param pinRE the pin to replace
     * @param replacementRE the replacement
     * @return true if replacement done, false otherwise.
     */
    public boolean replaceExistingRedundantPin(RouteElementPort pinRE, RouteElementPort replacementRE) {
        // only replace existing pins
        if (pinRE.getAction() != RouteElement.RouteElementAction.existingPortInst) return false;

        PortInst pi = pinRE.getPortInst();
        NodeInst ni = pi.getNodeInst();

        // only replace pins
        if (ni.getProto().getFunction() != PrimitiveNode.Function.PIN) return false;

        // if the pins is exported, do not replace
        if (pi.getExports().hasNext()) return false;

        List<RouteElementArc> newElements = new ArrayList<RouteElementArc>();
        Cell cell = replacementRE.getCell();
        boolean replace = true;

//        Iterator it2 = pi.getConnections();
        // if there are no connections, check if it's at the same location
        if (!pi.hasConnections()) {
//        if (!it2.hasNext()) {
            if (!ni.getTrueCenter().equals(replacementRE.getLocation()))
                return false;
        }

        // if any connection cannot be remade to replacement, abort
        for (Iterator<Connection> it = pi.getConnections(); it.hasNext(); ) {
            Connection conn = it.next();
            if (replacementRE.getPortProto().connectsTo(conn.getArc().getProto())) {
                // possible to connect, check location
                if (conn.getLocation().equals(replacementRE.getLocation())) {
                    // can reconnect
                    // get other end point connection
                    ArcInst ai = conn.getArc();
                    int otherEnd = 1 - conn.getEndIndex();
//                    Connection otherConn = ai.getHead();
//                    if (otherConn == conn) otherConn = ai.getTail();
                    RouteElementPort otherPort = RouteElementPort.existingPortInst(ai.getPortInst(otherEnd),
                            ai.getPortInst(otherEnd).getPoly());
                    // build new arc
                    RouteElementArc newArc = RouteElementArc.newArc(cell, ai.getProto(),
                            ai.getWidth(), otherPort, replacementRE,
                            ai.getLocation(otherEnd), conn.getLocation(), ai.getName(),
                            ai.getTextDescriptor(ArcInst.ARC_NAME), ai, true, null);
                    RouteElementArc delArc = RouteElementArc.deleteArc(ai);
                    newElements.add(newArc);
                    newElements.add(delArc);
                } else {
                    replace = false;
                    break;
                }
            } else {
                replace = false;
                break;
            }
        }

        if (replace) {
            //System.out.println("Replacing "+pinRE+" with "+replacementRE);
            RouteElementPort delPort = RouteElementPort.deleteNode(ni);
            add(delPort);
            for (RouteElement e : newElements) {
                add(e);
            }
        }
        return replace;
    }


}

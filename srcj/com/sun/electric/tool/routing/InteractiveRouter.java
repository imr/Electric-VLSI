/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: InteractiveRouter.java
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

import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.Generic;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * An Interactive Router has several methods that build on Router
 * methods to provide interactive control to user.  It also
 * provides methods for highlighting routes to provide visual
 * feedback to the user.  Finally, non-interactive routing is done only
 * from PortInst to PortInst, whereas interactive routing can start and
 * end on any arc, and can end in space.
 * <p>
 * Note: 'Interactive' is somewhat of a misnomer, as it would imply
 * the route can be incremently built or changed by the user.  In
 * reality, it is expected that the route simply be rebuilt whenever
 * the user input changes, until the user decides that the route is acceptable,
 * at which point the route can be made.
 * <p>
 * User: gainsley
 * Date: Feb 24, 2004
 * Time: 4:58:24 PM
 */
public abstract class InteractiveRouter extends Router {

    /** for highlighting the start of the route */  private List startRouteHighlights = new ArrayList();
    /** for highlighting the end of the route */    private RouteElement finalRE;

    public InteractiveRouter() {
        verbose = true;
    }

    // --------------------- Abstract Router Classes ------------------------------

    public abstract String toString();

    protected abstract boolean planRoute(List route, Cell cell, RouteElement startRE, RouteElement endRE, Point2D hint);

    // ----------------------- Interactive Route Control --------------------------

    /**
     * This stores the currently highlighted objects to highlight
     * in addition to route highlighting.  If routing it cancelled,
     * it also restores the original highlighting.
     */
    public void startInteractiveRoute() {
        // copy current highlights
        startRouteHighlights.clear();
        finalRE = null;
        for (Iterator it = Highlight.getHighlights(); it.hasNext(); ) {
            Highlight h = (Highlight)it.next();
            startRouteHighlights.add(h);
        }

    }

    /**
     * Cancels interative routing and restores original highlights
     */
    public void cancelInteractiveRoute() {
        // restore original highlights
        Highlight.clear();
        Highlight.setHighlightList(startRouteHighlights);
        Highlight.finished();
    }

    /**
     * Make a route between startObj and endObj in the EditWindow wnd.
     * Uses the point where the user clicked as a parameter to set the route.
     * @param wnd the EditWindow the user is editing
     * @param startObj a PortInst or ArcInst from which to start the route
     * @param endObj a PortInst or ArcInst to end the route on. May be null
     * if the user is drawing to empty space.
     * @param clicked the point where the user clicked
     */
    public void makeRoute(EditWindow wnd, ElectricObject startObj, ElectricObject endObj, Point2D clicked) {

        // plan the route
        List route = planRoute(wnd, startObj, endObj, clicked);
        // create route
        createRoute(route, wnd.getCell(), finalRE);
    }

    /**
     * Make a vertical route.  Will add in contacts in startPort's technology
     * to be able to connect to endPort.  The added contacts will be placed on
     * top of startPort.  However, the final arc will route to endPort, regardless
     * of where it is (this may result in non-orthogonal arcs at this point in time).
     * @param startPort the start of the route
     * @param endPort the end of the route
     * @return true on success, false otherwise
     */
    public boolean makeVerticalRoute(PortInst startPort, PortInst endPort) {
        List route = routeVerticallyToPort(startPort, endPort);
        if (route == null || route.size() == 0) return false;
        // last node is second to last in route
        RouteElement lastNode = (RouteElement)route.get(route.size()-2);
        createRoute(route, startPort.getNodeInst().getParent(), lastNode);
        return true;
    }

    /**
     * Make a vertical route.  Will add in contacts in startPort's technology
     * to be able to connect to endPort.  The added contacts will be placed on
     * top of startPort.  The final contact will be able to connect to <i>arc</i>.
     * @param startPort the start of the route
     * @param arc the arc type that the last contact will be able to connect to
     * @return true on sucess
     */
    public boolean makeVerticalRoute(PortInst startPort, ArcProto arc) {
        List route = routeVerticallyToArc(startPort, arc);
        if (route == null || route.size() == 0) return false;
        // last node is second to last in route
        RouteElement lastNode = (RouteElement)route.get(route.size()-2);
        createRoute(route, startPort.getNodeInst().getParent(), lastNode);
        return true;
    }

    // -------------------------- Highlight Route Methods -------------------------

    /**
     * Make a route and highlight it in the window.
     * @param wnd the EditWindow the user is editing
     * @param startObj a PortInst or ArcInst from which to start the route
     * @param endObj a PortInst or ArcInst to end the route on. May be null
     * if the user is drawing to empty space.
     * @param clicked the point where the user clicked
     */
    public void highlightRoute(EditWindow wnd, ElectricObject startObj, ElectricObject endObj, Point2D clicked) {
        // highlight route
        List route = planRoute(wnd, startObj, endObj, clicked);
        highlightRoute(wnd, route);
    }

    /**
     * Highlight a route in the window
     * @param wnd the EditWindow the user is editing
     * @param route the route to be highlighted
     */
    public void highlightRoute(EditWindow wnd, List route) {
        // highlight all objects in route
        Highlight.clear();
        Highlight.setHighlightList(startRouteHighlights);
        for (Iterator it = route.iterator(); it.hasNext(); ) {
            RouteElement e = (RouteElement)it.next();
            e.addHighlightArea();
        }
        Highlight.finished();
    }


    // -------------------- Internal Router Wrapper of Router ---------------------

    /**
     * Plan a route from startObj to endObj, taking into account
     * where the user clicked in the cell.
     * @param wnd the EditWindow the user is editing
     * @param startObj a PortInst or ArcInst from which to start the route
     * @param endObj a PortInst or ArcInst to end the route on. May be null
     * if the user is drawing to empty space.
     * @param clicked the point where the user clicked
     * @return a List of RouteElements denoting route
     */
    protected List planRoute(EditWindow wnd, ElectricObject startObj, ElectricObject endObj, Point2D clicked) {

        List route = new ArrayList();               // hold the route
        Cell cell = wnd.getCell();
        if (cell == null) return route;

        RouteElement startRE = null;                // denote start of route
        RouteElement endRE = null;                  // denote end of route

        // plan start of route
        if (startObj instanceof PortInst) {
            // portinst: just wrap in RouteElement
            startRE = RouteElement.existingPortInst((PortInst)startObj);
        }
        if (startObj instanceof ArcInst) {
            // arc: figure out where on arc to start
            startRE = findArcStartPoint(route, (ArcInst)startObj, clicked);
        }
        if (startObj instanceof NodeInst) {
            // find closest portinst to start from
            PortInst pi = ((NodeInst)startObj).findClosestPortInst(clicked);
            if (pi != null) {
                startRE = RouteElement.existingPortInst(pi);
            }
        }
        if (startRE == null) {
            System.out.println("  Can't route from "+startObj);
            return route;
        }

        // plan end of route
        if (endObj != null) {
            // we have somewhere to route to
            if (endObj instanceof PortInst) {
                // portinst: just wrap in RouteElement
                endRE = RouteElement.existingPortInst((PortInst)endObj);
            }
            if (endObj instanceof ArcInst) {
                // arc: figure out where on arc to end
                endRE = findArcStartPoint(route, (ArcInst)endObj, clicked);
            }
            if (endRE == null) {
                System.out.println("  Can't route to "+endObj);
                return route;
            }
        } else {
            // nowhere to route to, must make new pin to route to
            // first we need to determine what pin to make based on
            // start object
            ArcProto useArc = null;
            if (startObj instanceof PortInst) {
                PortInst startPort = (PortInst)startObj;
                useArc = getArcToUse(startPort.getPortProto(), null);
            }
            if (startObj instanceof ArcInst) {
                ArcInst startArc = (ArcInst)startObj;
                useArc = startArc.getProto();
            }
            if (startObj instanceof NodeInst) {
                PortInst startPort = ((NodeInst)startObj).findClosestPortInst(clicked);
                useArc = getArcToUse(startPort.getPortProto(), null);
            }
            if (!(useArc instanceof PrimitiveArc)) {
                System.out.println("  Don't know how to determine pin for arc "+useArc);
                return new ArrayList();
            }
            // make new pin to route to
            PrimitiveNode pn = ((PrimitiveArc)useArc).findPinProto();
            Point2D location = getClosestOrthogonalPoint(startRE.getLocation(), clicked);
            endRE = RouteElement.newNode(cell, pn, pn.getPort(0), location,
                    pn.getDefWidth(), pn.getDefHeight());
        }

        // add startRE and endRE to route
        route.add(startRE);
        route.add(endRE);
        finalRE = endRE;

        // Tell Router to route between startRE and endRE
        if (planRoute(route, cell, startRE, endRE, clicked))
            return route;
        else
            return new ArrayList();             // error, return empty route
    }

    // -------------------- Internal Router Utility Methods --------------------
    
    /**
     * If drawing to/from an ArcInst, we may connect to the some
     * point along the arc.  This may bisect the arc, in which case
     * we delete the current arc, add in a pin at the appropriate
     * place, and create 2 new arcs to the old arc head/tail points.
     * The bisection point depends on where the user clicked, but it
     * is always a point on the arc.
     * <p>
     * Note that this method adds the returned RouteElement to the
     * route, and updates the route if the arc is bisected.
     * This method should NOT add the returned RouteElement to the route.
     * @param route the route so far
     * @param arc the arc to draw from/to
     * @param clicked where the user clicked
     * @return a RouteElement holding the new pin at the bisection
     * point, or a RouteElement holding an existingPortInst if
     * drawing from either end of the ArcInst.
     */
    protected RouteElement findArcStartPoint(List route, ArcInst arc, Point2D clicked) {

        Point2D head = arc.getHead().getLocation();
        Point2D tail = arc.getTail().getLocation();
        RouteElement headRE = RouteElement.existingPortInst(arc.getHead().getPortInst());
        RouteElement tailRE = RouteElement.existingPortInst(arc.getTail().getPortInst());
        RouteElement startRE = null;
        // find extents of wire
        double minX, minY, maxX, maxY;
        Point2D minXpin = null, minYpin = null;
        if (head.getX() < tail.getX()) {
            minX = head.getX(); maxX = tail.getX(); minXpin = head;
        } else {
            minX = tail.getX(); maxX = head.getX(); minXpin = tail;
        }
        if (head.getY() < tail.getY()) {
            minY = head.getY(); maxY = tail.getY(); minYpin = head;
        } else {
            minY = tail.getY(); maxY = head.getY(); minYpin = tail;
        }
        // for efficiency purposes, we are going to assume the arc is
        // either vertical or horizontal for bisecting the arc
        if (head.getX() == tail.getX()) {
            // line is vertical, see if clicked point bisects
            if (clicked.getY() > minY && clicked.getY() < maxY) {
                Point2D location = new Point2D.Double(head.getX(), clicked.getY());
                startRE = bisectArc(route, arc, location);
            }
            // not within Y bounds, choose closest pin
            else if (clicked.getY() <= minY) {
                if (minYpin == head) startRE = headRE; else startRE = tailRE;
            } else {
                if (minYpin == head) startRE = tailRE; else startRE = headRE;
            }
        }
        // check if arc is horizontal
        else if (head.getY() == tail.getY()) {
            // line is horizontal, see if clicked bisects
            if (clicked.getX() > minX && clicked.getX() < maxX) {
                Point2D location = new Point2D.Double(clicked.getX(), head.getY());
                startRE = bisectArc(route, arc, location);
            }
            // not within X bounds, choose closest pin
            else if (clicked.getX() <= minX) {
                if (minXpin == head) startRE = headRE; else startRE = tailRE;
            } else {
                if (minXpin == head) startRE = tailRE; else startRE = headRE;
            }
        }
        // arc is not horizontal or vertical, draw from closest pin
        else {
            double headDist = clicked.distance(head);
            double tailDist = clicked.distance(tail);
            if (headDist < tailDist)
                startRE = headRE;
            else
                startRE = tailRE;
        }
        //route.add(startRE);           // DON'T ADD!!
        return startRE;
    }

    /**
     * Splits an arc at bisectPoint and updates the route to reflect the change.
     * This method should NOT add the returned RouteElement to the route.
     *
     * @param route the current route
     * @param arc the arc to split
     * @param bisectPoint point on arc from which to split it
     * @return the RouteElement from which to continue the route
     */
    protected RouteElement bisectArc(List route, ArcInst arc, Point2D bisectPoint) {

        Cell cell = arc.getParent();
        // determine pin type to use if bisecting arc
        PrimitiveNode pn = ((PrimitiveArc)arc.getProto()).findPinProto();
        // make new pin
        RouteElement newPinRE = RouteElement.newNode(cell, pn, pn.getPort(0),
                bisectPoint, pn.getDefWidth(), pn.getDefHeight());
        newPinRE.setIsBisectArcPin(true);
        // make dummy end pins
        RouteElement headRE = RouteElement.existingPortInst(arc.getHead().getPortInst());
        RouteElement tailRE = RouteElement.existingPortInst(arc.getTail().getPortInst());
        // add two arcs to rebuild old startArc
        RouteElement newHeadArcRE = RouteElement.newArc(cell, arc.getProto(), arc.getWidth(), headRE, newPinRE);
        RouteElement newTailArcRE = RouteElement.newArc(cell, arc.getProto(), arc.getWidth(), newPinRE, tailRE);
        // delete old arc
        RouteElement deleteArcRE = RouteElement.deleteArc(arc);
        // add new stuff to route
        route.add(deleteArcRE);
        //route.add(newPinRE);          // DON'T ADD!!
        route.add(headRE);
        route.add(tailRE);
        route.add(newHeadArcRE);
        route.add(newTailArcRE);
        return newPinRE;
    }

    /**
     * Gets the closest orthogonal point from the startPoint to the clicked point.
     * This is used when the user clicks in space and the router draws only a single
     * arc towards the clicked point in one dimension.
     * @param startPoint start point of the arc
     * @param clicked where the user clicked
     * @return an end point to draw to from start point to make a single arc segment.
     */
    protected Point2D getClosestOrthogonalPoint(Point2D startPoint, Point2D clicked) {
        Point2D newPoint;
        if (Math.abs(startPoint.getX() - clicked.getX()) < Math.abs(startPoint.getY() - clicked.getY())) {
            // draw horizontally
            newPoint = new Point2D.Double(startPoint.getX(), clicked.getY());
        } else {
            // draw vertically
            newPoint = new Point2D.Double(clicked.getX(), startPoint.getY());
        }
        return newPoint;
    }

    protected void replaceRouteElementArcPin(List route, RouteElement bisectPinRE, RouteElement newPinRE) {
        // go through route and update newArcs
        for (Iterator it = route.iterator(); it.hasNext(); ) {
            RouteElement e = (RouteElement)it.next();
            e.replaceArcEnd(bisectPinRE, newPinRE);
        }
    }

}

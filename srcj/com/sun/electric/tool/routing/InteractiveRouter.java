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
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.*;

import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
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
    /** if start has been called */                 private boolean started;

    public InteractiveRouter() {
        verbose = true;
        started = false;
    }

    // --------------------- Abstract Router Classes ------------------------------

    public abstract String toString();

    protected abstract boolean planRoute(Route route, Cell cell, RouteElement endRE, Point2D hint);

    // ----------------------- Interactive Route Control --------------------------

    /**
     * This stores the currently highlighted objects to highlight
     * in addition to route highlighting.  If routing it cancelled,
     * it also restores the original highlighting.
     */
    private void startInteractiveRoute() {
        // copy current highlights
        startRouteHighlights.clear();
        for (Iterator it = Highlight.getHighlights(); it.hasNext(); ) {
            Highlight h = (Highlight)it.next();
            startRouteHighlights.add(h);
        }
        Highlight.clear();
        started = true;
    }

    /**
     * Cancels interative routing and restores original highlights
     */
    public void cancelInteractiveRoute() {
        // restore original highlights
        Highlight.clear();
        Highlight.setHighlightList(startRouteHighlights);
        Highlight.finished();
        started = false;
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
        if (!started) startInteractiveRoute();
        // plan the route
        Route route = planRoute(wnd, startObj, endObj, clicked);
        // restore highlights at start of planning, so that
        // they will correctly show up if this job is undone.
        Highlight.clear();
        Highlight.setHighlightList(startRouteHighlights);
        // create route
        createRoute(route, wnd.getCell());
        started = false;
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
        if (!started) startInteractiveRoute();
        // do nothing if startPort can already connect to arc
        if (startPort.getPortProto().connectsTo(arc)) return true;

        RouteElement startRE = RouteElement.existingPortInst(startPort, null);
        Route route = new Route();
        route.add(startRE); route.setStart(startRE);
        route.setEnd(startRE);

        VerticalRoute vroute = new VerticalRoute(startRE, arc);
        if (!vroute.specifyRoute()) return false;
        vroute.buildRoute(route, startRE.getCell(), startRE.getLocation());
        // restore highlights at start of planning, so that
        // they will correctly show up if this job is undone.
        Highlight.clear();
        Highlight.setHighlightList(startRouteHighlights);
        createRoute(route, startPort.getNodeInst().getParent());
        started = false;
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
        if (!started) startInteractiveRoute();
        // highlight route
        Route route = planRoute(wnd, startObj, endObj, clicked);
        highlightRoute(wnd, route);
    }

    /**
     * Highlight a route in the window
     * @param wnd the EditWindow the user is editing
     * @param route the route to be highlighted
     */
    public void highlightRoute(EditWindow wnd, Route route) {
        if (!started) startInteractiveRoute();
        // highlight all objects in route
        Highlight.clear();
        //Highlight.setHighlightList(startRouteHighlights);
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
    protected Route planRoute(EditWindow wnd, ElectricObject startObj, ElectricObject endObj, Point2D clicked) {

        Route route = new Route();               // hold the route
        Cell cell = wnd.getCell();
        if (cell == null) return route;

        RouteElement startRE = null;                // denote start of route
        RouteElement endRE = null;                  // denote end of route

        // special case: if both objects are arc insts, check if they intersect
        // if they intersect, connect them there
        if ((startObj instanceof ArcInst) && endObj instanceof ArcInst) {
            if (connectIntersectingArcs(route, (ArcInst)startObj, (ArcInst)endObj)) {
                // arcs intersected, return new route
                return route;
            }
        }

        // to make connecting to and from an ArcInst <-> PortInst a symmetric operation
        Point2D endPoint = null;
        if (endObj instanceof PortInst) {
            PortInst endPort = (PortInst)endObj;
			endPoint = new Point2D.Double(endPort.getBounds().getCenterX(),
										  endPort.getBounds().getCenterY());
            endPoint = getExistingPortEndPoint(endPort, clicked);
        }
        if (endObj instanceof NodeInst) {
            // find closest portinst to start from
            PortInst endPort = ((NodeInst)endObj).findClosestPortInst(clicked);
            if (endPort != null) {
                endPoint = getExistingPortEndPoint(endPort, clicked);
            }
        }

        // contacts go at the end of the route: but we want contacts to go
        // on the arc, if it the other connection is a node
        boolean reverseRoute = false;

        // plan start of route
        if (startObj instanceof PortInst) {
            // portinst: just wrap in RouteElement
            Point2D point = getExistingPortEndPoint((PortInst)startObj, null);
            startRE = RouteElement.existingPortInst((PortInst)startObj, point);
        }
        if (startObj instanceof ArcInst) {
            // arc: figure out where on arc to start
            if (endPoint == null) endPoint = clicked;
            startRE = findArcConnectingPoint(route, (ArcInst)startObj, endPoint);
            reverseRoute = true;
        }
        if (startObj instanceof NodeInst) {
            // find closest portinst to start from
            PortInst pi = ((NodeInst)startObj).findClosestPortInst(clicked);
            if (pi != null) {
                Point2D point = getExistingPortEndPoint((PortInst)startObj, clicked);
                startRE = RouteElement.existingPortInst(pi, point);
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
                Point2D point = getExistingPortEndPoint((PortInst)endObj, clicked);
                endRE = RouteElement.existingPortInst((PortInst)endObj, point);
            }
            if (endObj instanceof ArcInst) {
                // arc: figure out where on arc to end
                // use startRE location when possible if connecting to arc
                Point2D startPoint = startRE.getLocation();
                if (startPoint == null) startPoint = clicked;
                endRE = findArcConnectingPoint(route, (ArcInst)endObj, startPoint);
                reverseRoute = false;
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
                return new Route();
            }
            // make new pin to route to
            PrimitiveNode pn = ((PrimitiveArc)useArc).findOverridablePinProto();
            Point2D location = getClosestOrthogonalPoint(startRE.getLocation(), clicked);
            endRE = RouteElement.newNode(cell, pn, pn.getPort(0), location,
                    pn.getDefWidth(), pn.getDefHeight());
            reverseRoute = false;
        }

        // favors arcs for location of contact cuts
        if (reverseRoute) {
            RouteElement re = startRE;
            startRE = endRE;
            endRE = re;
        }

        // add startRE and endRE to route
        route.add(startRE);
        route.setStart(startRE);
        route.setEnd(startRE);
        //route.add(endRE); route.setEnd(endRE);

        // Tell Router to route between startRE and endRE
        if (planRoute(route, cell, endRE, clicked))
            return route;
        else
            return new Route();             // error, return empty route
    }

    // -------------------- Internal Router Utility Methods --------------------


    /**
     * For existing port insts that may be multi-site ports (Electric schematic
     * gate primitives), find end point of a connecting arc based on 'clicked'
     * @param pi the port inst to connect to
     * @param clicked the point where the user clicked
     * @return a point on the port to connect an arc to
     */
    protected static Point2D getExistingPortEndPoint(PortInst pi, Point2D clicked) {
        NodeInst ni = pi.getNodeInst();
        PortProto pp = pi.getPortProto();
        Poly poly = ni.getShapeOfPort(pp, clicked);
        Rectangle2D bounds = poly.getBounds2D();
        return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
    }

    /**
     * If drawing to/from an ArcInst, we may connect to some
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
    protected RouteElement findArcConnectingPoint(Route route, ArcInst arc, Point2D clicked) {

        Point2D head = arc.getHead().getLocation();
        Point2D tail = arc.getTail().getLocation();
        RouteElement headRE = RouteElement.existingPortInst(arc.getHead().getPortInst(), null);
        RouteElement tailRE = RouteElement.existingPortInst(arc.getTail().getPortInst(), null);
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

    protected boolean connectIntersectingArcs(Route route, ArcInst startArc, ArcInst endArc) {
        // check if arcs intersect
        // equation for line 1
        // y = startSlope*x + startYint
        Point2D startHead = startArc.getHead().getLocation();
        Point2D startTail = startArc.getTail().getLocation();
        double startSlope, startYint;
        startSlope = (startTail.getY() - startHead.getY())/(startTail.getX() - startHead.getX());
        startYint = startTail.getY() - startSlope*startTail.getX();
        // equation for line 2
        Point2D endHead = endArc.getHead().getLocation();
        Point2D endTail = endArc.getTail().getLocation();
        double endSlope, endYint;
        endSlope = (endTail.getY() - endHead.getY())/(endTail.getX() - endHead.getX());
        endYint = endTail.getY() - endSlope*endTail.getX();

        // if slopes are the same, lines may be colinear or may not intersect
        if (startSlope == endSlope) {
            // ignore colinear case
            // if don't intersect, do not edit route, and return false
            return false;
        }
        // find intersecting point of lines
        Point2D point = null;
        if (Double.isInfinite(startSlope)) {
            // startArc is vertical
            // check if X coord startArc is within endArc
            if (withinBounds(startHead.getX(), endHead.getX(), endTail.getX())) {
                double y = endSlope*startHead.getX() + endYint;
                point = new Point2D.Double(startHead.getX(), y);
            }
        } else if (Double.isInfinite(endSlope)) {
            // endArc is vertical
            // check if X coord startArc is within startArc
            if (withinBounds(endHead.getX(), startHead.getX(), startTail.getX())) {
                double y = startSlope*endHead.getX() + startYint;
                point = new Point2D.Double(endHead.getX(), y);
            }
        } else {
            double x, y;
            x = (endYint - startYint)/(startSlope - endSlope);
            y = startSlope*x + startYint;
            // check if it is on both line segments
            Point2D tryPoint = new Point2D.Double(x, y);
            if (onSegment(tryPoint, new Line2D.Double(startHead, startTail)) &&
                onSegment(tryPoint, new Line2D.Double(endHead, endTail))) {
                point = tryPoint;
            }
        }
        if (point == null) return false;

        // lines intersect, connect them
        RouteElement startRE = bisectArc(route, startArc, point);
        RouteElement endRE = bisectArc(route, endArc, point);
        route.setStart(startRE);
        route.setEnd(startRE);

        // see if we can connect directly
        ArcProto useArc = getArcToUse(startRE.getPortProto(), endRE.getPortProto());
        if (useArc != null) {
            // replace one of the bisect pins with the other
            replaceRouteElementArcPin(route, endRE, startRE);
            // note that endRE was never added to the route, otherwise we'd remove it here
        } else {
            VerticalRoute vroute = new VerticalRoute(route.getEnd(), endRE);
            if (!vroute.specifyRoute()) {
                System.out.println("Can't route vertically between "+startArc+" and "+endArc);
                return false;
            }
            vroute.buildRoute(route, endRE.getCell(), point);
        }
        return true;
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
    protected RouteElement bisectArc(Route route, ArcInst arc, Point2D bisectPoint) {

        Cell cell = arc.getParent();
        // determine pin type to use if bisecting arc
        PrimitiveNode pn = ((PrimitiveArc)arc.getProto()).findOverridablePinProto();
        // make new pin
        RouteElement newPinRE = RouteElement.newNode(cell, pn, pn.getPort(0),
                bisectPoint, pn.getDefWidth(), pn.getDefHeight());
        newPinRE.setIsBisectArcPin(true);
        // make dummy end pins
        RouteElement headRE = RouteElement.existingPortInst(arc.getHead().getPortInst(), null);
        RouteElement tailRE = RouteElement.existingPortInst(arc.getTail().getPortInst(), null);
        headRE.setShowHighlight(false);
        tailRE.setShowHighlight(false);
        // add two arcs to rebuild old startArc
        String name = arc.getName();
        RouteElement newHeadArcRE = RouteElement.newArc(cell, arc.getProto(), arc.getWidth(), headRE, newPinRE, name);
        RouteElement newTailArcRE = RouteElement.newArc(cell, arc.getProto(), arc.getWidth(), newPinRE, tailRE, null);
        newHeadArcRE.setShowHighlight(false);
        newTailArcRE.setShowHighlight(false);
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

    // ----------------------------------------------------------------------

    protected boolean withinBounds(double point, double bound1, double bound2) {
        double min, max;
        if (bound1 < bound2) {
            min = bound1; max = bound2;
        } else {
            min = bound2; max = bound1;
        }
        return ((point >= min) && (point <= max));
    }


    /**
     * Returns true if point is on the line segment, false otherwise.
     */
    protected boolean onSegment(Point2D point, Line2D line) {
        double minX, minY, maxX, maxY;
        Point2D head = line.getP1();
        Point2D tail = line.getP2();
        if (head.getX() < tail.getX()) {
            minX = head.getX(); maxX = tail.getX();
        } else {
            minX = tail.getX(); maxX = head.getX();
        }
        if (head.getY() < tail.getY()) {
            minY = head.getY(); maxY = tail.getY();
        } else {
            minY = tail.getY(); maxY = head.getY();
        }
        if ((point.getX() >= minX) && (point.getX() <= maxX) &&
            (point.getY() >= minY) && (point.getY() <= maxY))
            return true;
        return false;
    }
}

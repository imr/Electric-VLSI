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
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.Job;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.DBMath;
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
    /** EditWindow we are routing in */             private EditWindow wnd;

    /** last bad object routed from: prevent too many error messages */ private ElectricObject badStartObject;
    /** last bad object routing to: prevent too many error messages */  private ElectricObject badEndObject;

    public InteractiveRouter() {
        verbose = true;
        started = false;
        badStartObject = badEndObject = null;
        wnd = null;
    }

    public String toString() { return "Interactive Router"; }

    protected abstract boolean planRoute(Route route, Cell cell, RouteElementPort endRE,
                                Point2D startLoc, Point2D endLoc, Point2D clicked, VerticalRoute vroute,
                                boolean contactsOnEndObject);

    // ----------------------- Interactive Route Control --------------------------

    /**
     * This stores the currently highlighted objects to highlight
     * in addition to route highlighting.  If routing it cancelled,
     * it also restores the original highlighting.
     */
    public void startInteractiveRoute(EditWindow wnd) {
        this.wnd = wnd;
        // copy current highlights
        startRouteHighlights.clear();
        for (Iterator it = wnd.getHighlighter().getHighlights().iterator(); it.hasNext(); ) {
            Highlight h = (Highlight)it.next();
            startRouteHighlights.add(h);
        }
        wnd.getHighlighter().clear();
        started = true;
    }

    /**
     * Cancels interative routing and restores original highlights
     */
    public void cancelInteractiveRoute() {
        // restore original highlights
        Highlighter highlighter = wnd.getHighlighter();
        highlighter.clear();
        highlighter.setHighlightList(startRouteHighlights);
        highlighter.finished();
        wnd = null;
        started = false;
    }

    /**
     * Make a route between startObj and endObj in the EditWindow wnd.
     * Uses the point where the user clicked as a parameter to set the route.
     * @param wnd the EditWindow the user is editing
     * @param cell the cell in which to create the route
     * @param startObj a PortInst or ArcInst from which to start the route
     * @param endObj a PortInst or ArcInst to end the route on. May be null
     * if the user is drawing to empty space.
     * @param clicked the point where the user clicked
     */
    public void makeRoute(EditWindow wnd, Cell cell, ElectricObject startObj, ElectricObject endObj, Point2D clicked) {
        if (!started) startInteractiveRoute(wnd);
        // plan the route
        Route route = planRoute(cell, startObj, endObj, clicked);
        // restore highlights at start of planning, so that
        // they will correctly show up if this job is undone.
        wnd.getHighlighter().clear();
        wnd.getHighlighter().setHighlightList(startRouteHighlights);
        // create route
        createRoute(route, cell);
        started = false;
    }

    /**
     * Make a vertical route.  Will add in contacts in startPort's technology
     * to be able to connect to endPort.  The added contacts will be placed on
     * top of startPort.  The final contact will be able to connect to <i>arc</i>.
     * @param wnd the EditWindow the user is editing
     * @param startPort the start of the route
     * @param arc the arc type that the last contact will be able to connect to
     * @return true on sucess
     */
    public boolean makeVerticalRoute(EditWindow wnd, PortInst startPort, ArcProto arc) {
        // do nothing if startPort can already connect to arc
        if (startPort.getPortProto().connectsTo(arc)) return true;

        Cell cell = startPort.getNodeInst().getParent();
        if (!started) startInteractiveRoute(wnd);

        Point2D startLoc = new Point2D.Double(startPort.getPoly().getCenterX(), startPort.getPoly().getCenterY());
        Poly poly = getConnectingSite(startPort, startLoc, -1);
        RouteElementPort startRE = RouteElementPort.existingPortInst(startPort, poly);
        Route route = new Route();
        route.add(startRE); route.setStart(startRE);
        //route.setEnd(startRE);

        PrimitiveNode pn = ((PrimitiveArc)arc).findOverridablePinProto();
        PortProto pp = pn.getPort(0);
        VerticalRoute vroute = VerticalRoute.newRoute(startPort.getPortProto(), arc);
        if (!vroute.isSpecificationSucceeded()) {
            cancelInteractiveRoute();
            return false;
        }
        vroute.buildRoute(route, startRE.getCell(), startRE, null, startLoc, startLoc, startLoc);
        // restore highlights at start of planning, so that
        // they will correctly show up if this job is undone.
        wnd.getHighlighter().clear();
        wnd.getHighlighter().setHighlightList(startRouteHighlights);
        MakeVerticalRouteJob job = new MakeVerticalRouteJob(this, route, startPort.getNodeInst().getParent(), true);
        started = false;
        return true;
    }

    public static class MakeVerticalRouteJob extends Router.CreateRouteJob {
        protected MakeVerticalRouteJob(Router router, Route route, Cell cell, boolean verbose) {
            super(router, route, cell, false);
        }

        /** Implemented doIt() method to perform Job */
        public boolean doIt() {
            if (!super.doIt()) return false;

            RouteElementPort startRE = route.getStart();
            if (startRE.getAction() == RouteElement.RouteElementAction.existingPortInst) {
                // if this is a pin, replace it with the first contact in vertical route
                PortInst pi = startRE.getPortInst();
                NodeInst ni = pi.getNodeInst();
                if (ni.getProto().getFunction() == NodeProto.Function.PIN) {
                    CircuitChanges.Reconnect re = CircuitChanges.Reconnect.erasePassThru(ni, false);
                    if (re != null) re.reconnectArcs();
                    ni.kill();
                }
            }
            return true;
       }
    }

    // -------------------------- Highlight Route Methods -------------------------

    /**
     * Make a route and highlight it in the window.
     * @param cell the cell in which to create the route
     * @param startObj a PortInst or ArcInst from which to start the route
     * @param endObj a PortInst or ArcInst to end the route on. May be null
     * if the user is drawing to empty space.
     * @param clicked the point where the user clicked
     */
    public void highlightRoute(EditWindow wnd, Cell cell, ElectricObject startObj, ElectricObject endObj, Point2D clicked) {
        if (!started) startInteractiveRoute(wnd);
        // highlight route
        Route route = planRoute(cell, startObj, endObj, clicked);
        highlightRoute(wnd, route, cell);
    }

    /**
     * Highlight a route in the window
     * @param route the route to be highlighted
     */
    public void highlightRoute(EditWindow wnd, Route route, Cell cell) {
        if (!started) startInteractiveRoute(wnd);
        wnd.getHighlighter().clear();
        //wnd.getHighlighter().setHighlightList(startRouteHighlights);
        for (Iterator it = route.iterator(); it.hasNext(); ) {
            RouteElement e = (RouteElement)it.next();
            e.addHighlightArea(wnd.getHighlighter());
        }
        wnd.getHighlighter().finished();
    }


    // -------------------- Internal Router Wrapper of Router ---------------------

    /**
     * Plan a route from startObj to endObj, taking into account
     * where the user clicked in the cell.
     * @param cell the cell in which to create the arc
     * @param startObj a PortInst or ArcInst from which to start the route
     * @param endObj a PortInst or ArcInst to end the route on. May be null
     * if the user is drawing to empty space.
     * @param clicked the point where the user clicked
     * @return a List of RouteElements denoting route
     */
    protected Route planRoute(Cell cell, ElectricObject startObj, ElectricObject endObj, Point2D clicked) {

        Route route = new Route();               // hold the route
        if (cell == null) return route;

        RouteElementPort startRE = null;                // denote start of route
        RouteElementPort endRE = null;                  // denote end of route

        // first, convert NodeInsts to PortInsts, if it is one.
        // Now we don't have to worry about NodeInsts.
        startObj = filterRouteObject(startObj, clicked);
        endObj = filterRouteObject(endObj, clicked);

        // get the port types at each end so we can build electrical route
        PortProto startPort = getRoutePort(startObj);
        PortProto endPort;
        if (endObj == null) {
            // end object is null, we are routing to a pin. Figure out what arc to use
            ArcProto useArc = getArcToUse(startPort, null);
            PrimitiveNode pn = ((PrimitiveArc)useArc).findOverridablePinProto();
            endPort = pn.getPort(0);
        } else {
            endPort = getRoutePort(endObj);
        }

        // now determine the electrical route. We need to know what
        // arcs will be used (and their widths, eventually) to determine
        // the Port Poly size for contacts
        VerticalRoute vroute = VerticalRoute.newRoute(startPort, endPort);
        if (!vroute.isSpecificationSucceeded()) return new Route();
        // arc width of arcs that will connect to startObj, endObj will determine
        // valid attachment points of arcs
        ArcProto startArc = vroute.getStartArc();
        ArcProto endArc = vroute.getEndArc();
        double startArcWidth = getArcWidthToUse(startObj, startArc);
        double endArcWidth = (endObj == null) ? startArcWidth : getArcWidthToUse(endObj, endArc);

        // get valid connecting sites for start and end objects based on the objects
        // themselves, the point the user clicked, and the width of the wire that will
        // attach to each
        Poly startPoly = getConnectingSite(startObj, clicked, startArcWidth);
        Poly endPoly = getConnectingSite(endObj, clicked, endArcWidth);
        //Poly startPoly = getConnectingSite(startObj, clicked, 3);
        //Poly endPoly = getConnectingSite(endObj, clicked, 3);

        // Now we can figure out where on the start and end objects the connecting
        // arc(s) should connect
        Point2D startPoint = new Point2D.Double(0, 0);
        Point2D endPoint = new Point2D.Double(0,0);
        getConnectingPoints(startObj, endObj, clicked, startPoint, endPoint, startPoly, endPoly);


        PortInst existingStartPort = null;
        PortInst existingEndPort = null;

        // favor contact cuts on arcs
        boolean contactsOnEndObject = true;

        // plan start of route
        if (startObj instanceof PortInst) {
            // portinst: just wrap in RouteElement
            existingStartPort = (PortInst)startObj;
            startRE = RouteElementPort.existingPortInst(existingStartPort, startPoly);
        }
        if (startObj instanceof ArcInst) {
            // arc: figure out where on arc to start
            startRE = findArcConnectingPoint(route, (ArcInst)startObj, startPoint);
            contactsOnEndObject = false;
        }
        if (startRE == null) {
            if (startObj != badStartObject)
                System.out.println("  Can't route from "+startObj+", no ports");
            badStartObject = startObj;
            return route;
        }

        // plan end of route
        if (endObj != null) {
            // we have somewhere to route to
            if (endObj instanceof PortInst) {
                // portinst: just wrap in RouteElement
                existingEndPort = (PortInst)endObj;
                endRE = RouteElementPort.existingPortInst(existingEndPort, endPoly);
            }
            if (endObj instanceof ArcInst) {
                // arc: figure out where on arc to end
                // use startRE location when possible if connecting to arc
                endRE = findArcConnectingPoint(route, (ArcInst)endObj, endPoint);
                contactsOnEndObject = true;
            }
            if (endRE == null) {
                if (endObj != badEndObject)
                    System.out.println("  Can't route to "+endObj+", no ports");
                badEndObject = endObj;
                endObj = null;
            }
        }
        if (endObj == null) {
            // nowhere to route to, must make new pin to route to
            // first we need to determine what pin to make based on
            // start object
            ArcProto useArc = null;
            if (startObj instanceof PortInst) {
                PortInst startPi = (PortInst)startObj;
                useArc = getArcToUse(startPi.getPortProto(), null);
            }
            if (startObj instanceof ArcInst) {
                ArcInst startAi = (ArcInst)startObj;
                useArc = startAi.getProto();
            }
            if (!(useArc instanceof PrimitiveArc)) {
                System.out.println("  Don't know how to determine pin for arc "+useArc);
                return new Route();
            }
            // make new pin to route to
            PrimitiveNode pn = ((PrimitiveArc)useArc).findOverridablePinProto();
            SizeOffset so = pn.getProtoSizeOffset();
            endRE = RouteElementPort.newNode(cell, pn, pn.getPort(0), endPoint,
                    pn.getDefWidth()-so.getHighXOffset()-so.getLowXOffset(),
                    pn.getDefHeight()-so.getHighYOffset()-so.getLowYOffset());
        }

        // special check: if both are existing port insts and are same port, do nothing
        if ((existingEndPort != null) && (existingEndPort == existingStartPort)) return new Route();

        // add startRE and endRE to route
        route.add(startRE);
        route.setStart(startRE);
        route.setEnd(startRE);
        //route.add(endRE); route.setEnd(endRE);

        // Tell Router to route between startRE and endRE
        if (planRoute(route, cell, endRE, startPoint, endPoint, clicked, vroute, contactsOnEndObject)) {
            return route;
        } else
            return new Route();             // error, return empty route
    }

    // -------------------- Internal Router Utility Methods --------------------

    /**
     * If routeObj is a NodeInst, first thing we do is get the nearest PortInst
     * to where the user clicked, and use that instead.
     * @param routeObj the route object (possibly a NodeInst).
     * @param clicked where the user clicked
     * @return the PortInst on the NodeInst closest to where the user clicked,
     * or just the routeObj back if it is not a NodeInst.
     */
    protected static ElectricObject filterRouteObject(ElectricObject routeObj, Point2D clicked) {
        if (routeObj instanceof NodeInst) {
            return ((NodeInst)routeObj).findClosestPortInst(clicked);
        }
        if (routeObj instanceof Export) {
            Export exp = (Export)routeObj;
            return exp.getOriginalPort();
        }
        return routeObj;
    }

    /**
     * Get the PortProto associated with routeObj (it should be either
     * a ArcInst or a PortInst, otherwise this will return null).
     * @param routeObj the route object
     * @return the PortProto for this route object
     */
    protected static PortProto getRoutePort(ElectricObject routeObj) {
        assert(!(routeObj instanceof NodeInst));
        if (routeObj instanceof ArcInst) {
            ArcInst ai = (ArcInst)routeObj;
            PrimitiveNode pn = ((PrimitiveArc)ai.getProto()).findOverridablePinProto();
            return (PortProto)pn.getPort(0);
        }
        if (routeObj instanceof PortInst) {
            PortInst pi = (PortInst)routeObj;
            return pi.getPortProto();
        }
        return null;
    }

    protected static double getArcWidthToUse(ElectricObject routeObj, ArcProto ap) {
        double width = -1;
        if (routeObj instanceof ArcInst) {
            ArcInst ai = (ArcInst)routeObj;
            if (ai.getProto() == ap)
                return ai.getWidth();
        }
        if (routeObj instanceof PortInst) {
            width = Router.getArcWidthToUse((PortInst)routeObj, ap);
        }
        return width;
    }

    /**
     * Get the connecting points for the start and end objects of the route. This fills in
     * the two Point2D's startPoint and endPoint. These will be the end points of an arc that
     * connects to either startObj or endObj.
     * @param startObj the start route object
     * @param endObj the end route object
     * @param clicked where the user clicked
     * @param startPoint point inside startPoly on startObj to connect arc to
     * @param endPoint point inside endPoly on endObj to connect arc to
     * @param startPoly valid port site on startObj
     * @param endPoly valid port site on endObj
     */
    protected static void getConnectingPoints(ElectricObject startObj, ElectricObject endObj, Point2D clicked,
                                              Point2D startPoint, Point2D endPoint, Poly startPoly, Poly endPoly) {

        // just go by bounds for now
        Rectangle2D startBounds = startPoly.getBounds2D();
        // default is center point
        startPoint.setLocation(startBounds.getCenterX(), startBounds.getCenterY());

        if (startObj instanceof ArcInst) {
            double x, y;
            // if nothing to connect to, clicked will determine connecting point on startPoly
            // endPoint will be location of new pin
            x = getClosestValue(startBounds.getMinX(), startBounds.getMaxX(), clicked.getX());
            y = getClosestValue(startBounds.getMinY(), startBounds.getMaxY(), clicked.getY());
            startPoint.setLocation(x, y);
        }

        if (endPoly == null) {
            // if arc, find place to connect to. Otherwise use the center point (default)
            endPoint.setLocation(getClosestOrthogonalPoint(startPoint, clicked));
            return;
        }

        Rectangle2D endBounds = endPoly.getBounds2D();
        endPoint.setLocation(endBounds.getCenterX(), endBounds.getCenterY());

        if (endObj instanceof ArcInst) {
            double x, y;
            // if nothing to connect to, clicked will determine connecting point on startPoly
            // endPoint will be location of new pin
            x = getClosestValue(endBounds.getMinX(), endBounds.getMaxX(), clicked.getX());
            y = getClosestValue(endBounds.getMinY(), endBounds.getMaxY(), clicked.getY());
            endPoint.setLocation(x, y);
        }

        // if bounds share x-space, use closest x within that space to clicked point
        double lowerBoundX = Math.max(startBounds.getMinX(), endBounds.getMinX());
        double upperBoundX = Math.min(startBounds.getMaxX(), endBounds.getMaxX());
        if (lowerBoundX <= upperBoundX) {
            double x = getClosestValue(lowerBoundX, upperBoundX, clicked.getX());
            startPoint.setLocation(x, startPoint.getY());
            endPoint.setLocation(x, endPoint.getY());
        } else {
            // otherwise, use closest point in bounds to the other port
            // see which one is higher in X...they don't overlap, so any X coord in bounds is comparable
            if (startBounds.getMinX() > endBounds.getMaxX()) {
                startPoint.setLocation(startBounds.getMinX(), startPoint.getY());
                endPoint.setLocation(endBounds.getMaxX(), endPoint.getY());
            } else {
                startPoint.setLocation(startBounds.getMaxX(), startPoint.getY());
                endPoint.setLocation(endBounds.getMinX(), endPoint.getY());
            }
        }
        // if bounds share y-space, use closest y within that space to clicked point
        double lowerBoundY = Math.max(startBounds.getMinY(), endBounds.getMinY());
        double upperBoundY = Math.min(startBounds.getMaxY(), endBounds.getMaxY());
        if (lowerBoundY <= upperBoundY) {
            double y = getClosestValue(lowerBoundY, upperBoundY, clicked.getY());
            startPoint.setLocation(startPoint.getX(), y);
            endPoint.setLocation(endPoint.getX(), y);
        } else {
            // otherwise, use closest point in bounds to the other port
            // see which one is higher in Y...they don't overlap, so any Y coord in bounds is comparable
            if (startBounds.getMinY() > endBounds.getMaxY()) {
                startPoint.setLocation(startPoint.getX(), startBounds.getMinY());
                endPoint.setLocation(endPoint.getX(), endBounds.getMaxY());
            } else {
                startPoint.setLocation(startPoint.getX(), startBounds.getMaxY());
                endPoint.setLocation(endPoint.getX(), endBounds.getMinY());
            }
        }
    }

    /**
     * Get the connecting site of the electric object.
     * <ul>
     * <li>For NodeInsts, this is the nearest portinst to "clicked", which is then subject to:
     * <li>For PortInsts, this is the nearest site of a multisite port, or just the entire port
     * <li>For ArcInsts, this is a poly composed of the head location and the tail location
     * </ul>
     * See NodeInst.getShapeOfPort() for more details.
     * @param obj the object to get a connection site for
     * @param clicked used to find the nearest portinst on a nodeinst, and nearest
     * site on a multisite port
     * @param arcWidth contacts port sites are restricted by the size of arcs connecting
     * to them, such that the arc width does extend beyond the contact edges.
     * @return a poly describing where something can connect to
     */
    protected static Poly getConnectingSite(ElectricObject obj, Point2D clicked, double arcWidth) {

        assert(clicked != null);

        if (obj instanceof NodeInst) {
            PortInst pi = ((NodeInst)obj).findClosestPortInst(clicked);
            if (pi == null) return null;
            obj = pi;
        }
        if (obj instanceof PortInst) {
            PortInst pi = (PortInst)obj;
            NodeInst ni = pi.getNodeInst();
            PortProto pp = pi.getPortProto();
            boolean compressPort = false;
            if (ni.getProto() instanceof PrimitiveNode) compressPort = true;
            Poly poly = ni.getShapeOfPort(pp, clicked, compressPort, arcWidth); // this is for multi-site ports
            return poly;
        }
        if (obj instanceof ArcInst) {
            // make poly out of possible connecting points on arc
            ArcInst arc = (ArcInst)obj;
            Point2D [] points = new Point2D[2];
            points[0] = arc.getHead().getLocation();
            points[1] = arc.getTail().getLocation();
            Poly poly = new Poly(points);
            return poly;
        }
        return null;
    }

    /**
     * If drawing to/from an ArcInst, we may connect to some
     * point along the arc.  This may bisect the arc, in which case
     * we delete the current arc, add in a pin at the appropriate
     * place, and create 2 new arcs to the old arc head/tail points.
     * The bisection point depends on where the user point, but it
     * is always a point on the arc.
     * <p>
     * Note that this method adds the returned RouteElement to the
     * route, and updates the route if the arc is bisected.
     * This method should NOT add the returned RouteElement to the route.
     * @param route the route so far
     * @param arc the arc to draw from/to
     * @param point point on or near arc
     * @return a RouteElement holding the new pin at the bisection
     * point, or a RouteElement holding an existingPortInst if
     * drawing from either end of the ArcInst.
     */
    protected RouteElementPort findArcConnectingPoint(Route route, ArcInst arc, Point2D point) {

        Point2D head = arc.getHead().getLocation();
        Point2D tail = arc.getTail().getLocation();
        RouteElementPort headRE = RouteElementPort.existingPortInst(arc.getHead().getPortInst(), head);
        RouteElementPort tailRE = RouteElementPort.existingPortInst(arc.getTail().getPortInst(), tail);
        RouteElementPort startRE = null;
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
            // line is vertical, see if point point bisects
            if (point.getY() > minY && point.getY() < maxY) {
                Point2D location = new Point2D.Double(head.getX(), point.getY());
                startRE = bisectArc(route, arc, location);
            }
            // not within Y bounds, choose closest pin
            else if (point.getY() <= minY) {
                if (minYpin == head) startRE = headRE; else startRE = tailRE;
            } else {
                if (minYpin == head) startRE = tailRE; else startRE = headRE;
            }
        }
        // check if arc is horizontal
        else if (head.getY() == tail.getY()) {
            // line is horizontal, see if point bisects
            if (point.getX() > minX && point.getX() < maxX) {
                Point2D location = new Point2D.Double(point.getX(), head.getY());
                startRE = bisectArc(route, arc, location);
            }
            // not within X bounds, choose closest pin
            else if (point.getX() <= minX) {
                if (minXpin == head) startRE = headRE; else startRE = tailRE;
            } else {
                if (minXpin == head) startRE = tailRE; else startRE = headRE;
            }
        }
        // arc is not horizontal or vertical, draw from closest pin
        else {
            double headDist = point.distance(head);
            double tailDist = point.distance(tail);
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
    protected RouteElementPort bisectArc(Route route, ArcInst arc, Point2D bisectPoint) {

        Cell cell = arc.getParent();
        Point2D head = arc.getHead().getLocation();
        Point2D tail = arc.getTail().getLocation();

        // determine pin type to use if bisecting arc
        PrimitiveNode pn = ((PrimitiveArc)arc.getProto()).findOverridablePinProto();
        SizeOffset so = pn.getProtoSizeOffset();
        double width = pn.getDefWidth()-so.getHighXOffset()-so.getLowXOffset();
        double height = pn.getDefHeight()-so.getHighYOffset()-so.getLowYOffset();

        // make new pin
        RouteElementPort newPinRE = RouteElementPort.newNode(cell, pn, pn.getPort(0),
                bisectPoint, width, height);
        newPinRE.setBisectArcPin(true);

        // make dummy end pins
        RouteElementPort headRE = RouteElementPort.existingPortInst(arc.getHead().getPortInst(), head);
        RouteElementPort tailRE = RouteElementPort.existingPortInst(arc.getTail().getPortInst(), tail);
        headRE.setShowHighlight(false);
        tailRE.setShowHighlight(false);
        // add two arcs to rebuild old startArc
        String name = arc.getName();
        RouteElement newHeadArcRE = RouteElementArc.newArc(cell, arc.getProto(), arc.getWidth(), headRE, newPinRE,
                head, bisectPoint, name, arc.getNameTextDescriptor());
        RouteElement newTailArcRE = RouteElementArc.newArc(cell, arc.getProto(), arc.getWidth(), newPinRE, tailRE,
                bisectPoint, tail, null, null);
        newHeadArcRE.setShowHighlight(false);
        newTailArcRE.setShowHighlight(false);
        // delete old arc
        RouteElement deleteArcRE = RouteElementArc.deleteArc(arc);
        // add new stuff to route
        route.add(deleteArcRE);
        //route.add(newPinRE);          // DON'T ADD!!
        route.add(headRE);
        route.add(tailRE);
        route.add(newHeadArcRE);
        route.add(newTailArcRE);
        return newPinRE;
    }

    // ------------------------- Spatial Dimension Calculations -------------------------

    /**
     * Get closest value to clicked within a range from min to max
     */
    protected static double getClosestValue(double min, double max, double clicked) {
        if (clicked >= max) {
            return max;
        } else if (clicked <= min) {
            return min;
        } else {
            return clicked;
        }
    }

    /**
     * Gets the closest orthogonal point from the startPoint to the clicked point.
     * This is used when the user clicks in space and the router draws only a single
     * arc towards the clicked point in one dimension.
     * @param startPoint start point of the arc
     * @param clicked where the user clicked
     * @return an end point to draw to from start point to make a single arc segment.
     */
    protected static Point2D getClosestOrthogonalPoint(Point2D startPoint, Point2D clicked) {
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

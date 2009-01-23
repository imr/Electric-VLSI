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

package com.sun.electric.tool.routing;

import com.sun.electric.database.geometry.*;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

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

    /** for highlighting the start of the route */  private List<Highlight2> startRouteHighlights = new ArrayList<Highlight2>();
    /** if start has been called */                 private boolean started;
    /** EditWindow we are routing in */             private EditWindow wnd;

    /** last bad object routed from: prevent too many error messages */ private ElectricObject badStartObject;
    /** last bad object routing to: prevent too many error messages */  private ElectricObject badEndObject;

    public InteractiveRouter() {
        verbose = true;
        started = false;
        badStartObject = badEndObject = null;
        wnd = null;
        tool = Routing.getRoutingTool();
    }

    public String toString() { return "Interactive Router"; }

    protected abstract boolean planRoute(Route route, Cell cell, RouteElementPort endRE,
                                Point2D startLoc, Point2D endLoc, Point2D clicked, PolyMerge stayInside, VerticalRoute vroute,
                                boolean contactsOnEndObject, boolean extendArcHead, boolean extendArcTail, Rectangle2D contactArea);

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
        for (Highlight2 h : wnd.getHighlighter().getHighlights()) {
            startRouteHighlights.add(h);
        }
        wnd.clearHighlighting();
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
     * Make a route between startObj and endObj in the EditWindow_ wnd.
     * Uses the point where the user clicked as a parameter to set the route.
     * @param wnd the EditWindow_ the user is editing
     * @param cell the cell in which to create the route
     * @param startObj a PortInst or ArcInst from which to start the route
     * @param endObj a PortInst or ArcInst to end the route on. May be null
     * if the user is drawing to empty space.
     * @param clicked the point where the user clicked
     */
    public void makeRoute(EditWindow wnd, Cell cell, ElectricObject startObj, ElectricObject endObj, Point2D clicked) {
        if (!started) startInteractiveRoute(wnd);
        // plan the route
        Route route = planRoute(cell, startObj, endObj, clicked, null, true, true);
        // restore highlights at start of planning, so that
        // they will correctly show up if this job is undone.
        wnd.clearHighlighting();
        wnd.getHighlighter().setHighlightList(startRouteHighlights);
        // create route
        createRoute(route, cell);
        started = false;
    }

    /**
     * Make a vertical route.  Will add in contacts in startPort's technology
     * to be able to connect to endPort.  The added contacts will be placed on
     * top of startPort.  The final contact will be able to connect to <i>arc</i>.
     * @param wnd the EditWindow_ the user is editing
     * @param startPort the start of the route
     * @param arc the arc type that the last contact will be able to connect to
     * @return true on sucess
     */
    public boolean makeVerticalRoute(EditWindow wnd, PortInst startPort, ArcProto arc) {
        // do nothing if startPort can already connect to arc
        if (startPort.getPortProto().connectsTo(arc)) return true;

        if (!started) startInteractiveRoute(wnd);

        Point2D startLoc = new Point2D.Double(startPort.getPoly().getCenterX(), startPort.getPoly().getCenterY());
        Poly poly = getConnectingSite(startPort, startLoc, -1);
        RouteElementPort startRE = RouteElementPort.existingPortInst(startPort, poly);
        Route route = new Route();
        route.add(startRE); route.setStart(startRE);

        VerticalRoute vroute = VerticalRoute.newRoute(startPort.getPortProto(), arc);
        if (!vroute.isSpecificationSucceeded()) {
            cancelInteractiveRoute();
            return false;
        }
        ArcProto startArc = vroute.getStartArc();
        ArcProto endArc = vroute.getEndArc();

        ContactSize sizer = new ContactSize(startPort, null, startLoc, startLoc, startLoc, startArc, endArc, false);
        Rectangle2D contactArea = sizer.getContactSize();
        Dimension2D contactSize = new Dimension2D.Double(contactArea.getWidth(), contactArea.getHeight());
        int startAngle = sizer.getStartAngle();
        int endAngle = sizer.getEndAngle();
        double startArcWidth = sizer.getStartWidth();
        double endArcWidth = sizer.getEndWidth();
        Cell cell = wnd.getCell();

        Route vertRoute = vroute.buildRoute(cell, startLoc, contactSize, startAngle, null);
        for (RouteElement re : vertRoute) {
            if (!route.contains(re)) route.add(re);
            route.setEnd(vertRoute.getEnd());
        }
        // create arc between startRE and start of vertical route
        if (route.replacePin(startRE, vertRoute.getStart(), null)) {
            route.remove(startRE);
            if (route.getStart() == startRE) route.setStart(vertRoute.getStart());
        } else {
            addConnectingArc(route, cell, startRE, vertRoute.getStart(), startLoc, startLoc, startArc,
                    startArcWidth, startAngle, true, true, null);
        }

        // restore highlights at start of planning, so that
        // they will correctly show up if this job is undone.
        wnd.finishedHighlighting();
        wnd.getHighlighter().setHighlightList(startRouteHighlights);
        new MakeVerticalRouteJob(this, route, cell, true);
        started = false;
        return true;
    }

    private static class MakeVerticalRouteJob extends Router.CreateRouteJob {
        protected MakeVerticalRouteJob(Router router, Route route, Cell cell, boolean verbose) {
            super(router.toString(), route, cell, verbose, Routing.getRoutingTool());
        }

        /** Implemented doIt() method to perform Job */
        public boolean doIt() throws JobException {
            if (!super.doIt()) return false;

            RouteElementPort startRE = route.getStart();
            if (startRE.getAction() == RouteElement.RouteElementAction.existingPortInst) {
                // if this is a pin, replace it with the first contact in vertical route
                PortInst pi = startRE.getPortInst();
                NodeInst ni = pi.getNodeInst();
                if (ni.getProto().getFunction() == PrimitiveNode.Function.PIN) {
                	CircuitChangeJobs.Reconnect re = CircuitChangeJobs.Reconnect.erasePassThru(ni, false, true);
                    if (re != null) re.reconnectArcs();
                    if (!ni.hasExports()) ni.kill();
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
        Route route = planRoute(cell, startObj, endObj, clicked, null, true, true);
        highlightRoute(wnd, route, cell);
    }

    /**
     * Highlight a route in the window
     * @param route the route to be highlighted
     */
    public void highlightRoute(EditWindow wnd, Route route, Cell cell) {
        if (!started) startInteractiveRoute(wnd);
        wnd.clearHighlighting();
        //wnd.getHighlighter().setHighlightList(startRouteHighlights);
        for (RouteElement e : route) {
            e.addHighlightArea(wnd.getHighlighter());
        }
        wnd.finishedHighlighting();
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
     * @param stayInside the area in which to route (null if not applicable).
     * @param extendArcHead true to use default arc extension; false to force no arc extension.
     * @param extendArcTail true to use default arc extension; false to force no arc extension.
     * @return a List of RouteElements denoting route
     */
    public Route planRoute(Cell cell, ElectricObject startObj, ElectricObject endObj, Point2D clicked, PolyMerge stayInside,
                           boolean extendArcHead, boolean extendArcTail) {
        return planRoute(cell, startObj, endObj, clicked, stayInside, extendArcHead, extendArcTail, null);
    }

    /**
     * Plan a route from startObj to endObj, taking into account
     * where the user clicked in the cell.
     * @param cell the cell in which to create the arc
     * @param startObj a PortInst or ArcInst from which to start the route
     * @param endObj a PortInst or ArcInst to end the route on. May be null
     * if the user is drawing to empty space.
     * @param clicked the point where the user clicked
     * @param stayInside the area in which to route (null if not applicable).
     * @param extendArcHead true to use default arc extension; false to force no arc extension.
     * @param extendArcTail true to use default arc extension; false to force no arc extension.
     * @return a List of RouteElements denoting route
     */
    public Route planRoute(Cell cell, ElectricObject startObj, ElectricObject endObj, Point2D clicked, PolyMerge stayInside,
                           boolean extendArcHead, boolean extendArcTail, Rectangle2D contactArea) {

        Route route = new Route();               // hold the route
        if (cell == null) return route;

        if (startObj == endObj) return route;           // can't route to yourself

        RouteElementPort startRE = null;                // denote start of route
        RouteElementPort endRE = null;                  // denote end of route

        // first, convert NodeInsts to PortInsts, if it is one.
        // Now we don't have to worry about NodeInsts.
        startObj = filterRouteObject(startObj, clicked);
        endObj = filterRouteObject(endObj, clicked);

        // get the port types at each end so we can build electrical route
        PortProto startPort = getRoutePort(startObj);
        if (startPort == null) return route;            // start cannot be null
        PortProto endPort;
        if (endObj == null) {
            // end object is null, we are routing to a pin. Figure out what arc to use
            ArcProto useArc = getArcToUse(startPort, null);
            if (useArc == null) return route;
            PrimitiveNode pn = useArc.findOverridablePinProto();
            endPort = pn.getPort(0);
        } else {
            endPort = getRoutePort(endObj);
        }

        // now determine the electrical route. We need to know what
        // arcs will be used (and their widths, eventually) to determine
        // the Port Poly size for contacts
        VerticalRoute vroute = VerticalRoute.newRoute(startPort, endPort);
        if (!vroute.isSpecificationSucceeded()) return new Route();

        boolean ignoreAngles = false;
        // arc width of arcs that will connect to startObj, endObj will determine
        // valid attachment points of arcs
        ArcProto startArc = vroute.getStartArc();
        ArcProto endArc = vroute.getEndArc();
        double startArcWidth = 0;
        double endArcWidth = 0;

        if (!ClickZoomWireListener.theOne.getUseFatWiringMode()) {
            // if not using fat wiring mode, determine arc sizes now, and
            // start and end points based off of arc sizes and startObj and endObj port sizes
            startArcWidth = getArcWidthToUse(startObj, startArc, 0, true);
            endArcWidth = (endObj == null) ? startArcWidth : getArcWidthToUse(endObj, endArc, 0, true);
            if (startArc == endArc) {
                if (startArcWidth > endArcWidth) endArcWidth = startArcWidth;
                if (endArcWidth > startArcWidth) startArcWidth = endArcWidth;
            }
        }

        // if extension not supressed, use defaults from arcs
        if (extendArcHead) extendArcHead = startArc.isExtended() || endArc.isExtended();
        if (extendArcTail) extendArcTail = startArc.isExtended() || endArc.isExtended();

        // get valid connecting sites for start and end objects based on the objects
        // themselves, the point the user clicked, and the width of the wire that will
        // attach to each
        Poly startPoly = getConnectingSite(startObj, clicked, startArcWidth);
        Poly endPoly = getConnectingSite(endObj, clicked, endArcWidth);
        Poly startPolyFull = getConnectingSite(startObj, clicked, -1);
        Poly endPolyFull = getConnectingSite(endObj, clicked, -1);

        // Now we can figure out where on the start and end objects the connecting
        // arc(s) should connect
        Point2D startPoint = new Point2D.Double(0, 0);
        Point2D endPoint = new Point2D.Double(0,0);
        getConnectingPoints(startObj, endObj, clicked, startPoint, endPoint, startPoly, endPoly, startArc, endArc);

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
            startRE = findArcConnectingPoint(route, (ArcInst)startObj, startPoint, stayInside);
            if (startRE.isBisectArcPin()) contactsOnEndObject = false;
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
                endRE = findArcConnectingPoint(route, (ArcInst)endObj, endPoint, stayInside);
                if (endRE.isBisectArcPin()) contactsOnEndObject = true;
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
            // make new pin to route to
            PrimitiveNode pn = useArc.findOverridablePinProto();
            SizeOffset so = pn.getProtoSizeOffset();
            endRE = RouteElementPort.newNode(cell, pn, pn.getPort(0), endPoint,
                    pn.getDefWidth()-so.getHighXOffset()-so.getLowXOffset(),
                    pn.getDefHeight()-so.getHighYOffset()-so.getLowYOffset());
        }

        // favor contact on bisected arc
        if (startRE.isBisectArcPin()) contactsOnEndObject = false;
        if (endRE != null && endRE.isBisectArcPin()) contactsOnEndObject = true;

        // special check: if both are existing port insts and are same port, do nothing
        if ((existingEndPort != null) && (existingEndPort == existingStartPort)) return new Route();

        Point2D cornerLoc = getCornerLocation(startPoint, endPoint, clicked, startArc, endArc,
                                              contactsOnEndObject, stayInside, contactArea, startPolyFull, endPolyFull);
        int startAngle = GenMath.figureAngle(startPoint, cornerLoc);
        int endAngle = GenMath.figureAngle(endPoint, cornerLoc);

        // figure out sizes to use
        ContactSize sizer = new ContactSize(startObj, endObj, startPoint, endPoint, cornerLoc, startArc, endArc, false);
        contactArea = sizer.getContactSize();
        startAngle = sizer.getStartAngle();
        endAngle = sizer.getEndAngle();
        startArcWidth = sizer.getStartWidth();
        endArcWidth = sizer.getEndWidth();
/*
        if (contactArea == null) {
            contactArea = getContactArea(startObj, endObj, startPoint, endPoint, cornerLoc, startArc, endArc);
            //contactArea = getContactArea(startRE, endRE, startPoint, endPoint, cornerLoc, startArc, endArc, startArcWidth, endArcWidth);
            contactArea = new Rectangle2D.Double(cornerLoc.getX(), cornerLoc.getY(), 0, 0);
            updateContactArea(contactArea, startRE, cornerLoc, startArcWidth, startAngle);
            updateContactArea(contactArea, endRE, cornerLoc, endArcWidth, endAngle);
        }
*/

        // add startRE and endRE to route
        route.add(startRE);
        route.setStart(startRE);
        route.add(endRE);
        route.setEnd(endRE);

        // create vertical route if needed
        if (startArc != endArc) {
            // build vertical route
            Dimension2D contactSize = new Dimension2D.Double(contactArea.getWidth(), contactArea.getHeight());
            Route vertRoute = vroute.buildRoute(cell, cornerLoc, contactSize, startAngle, stayInside);
            for (RouteElement re : vertRoute) {
                if (!route.contains(re)) route.add(re);
            }

            // replace endpoints if possible (remove redundant pins), startRE
            if (route.replacePin(startRE, vertRoute.getStart(), stayInside)) {
                route.remove(startRE);
                if (route.getStart() == startRE) route.setStart(vertRoute.getStart());
            } else {
                // create arc between startRE and start of vertical route
                addConnectingArc(route, cell, startRE, vertRoute.getStart(), startPoint, cornerLoc, startArc,
                        startArcWidth, startAngle, extendArcHead, extendArcTail, stayInside);
            }
            // replace endpoints if possible (remove redundant pins), endRE
            if (route.replacePin(endRE, vertRoute.getEnd(), stayInside)) {
                route.remove(endRE);
                if (route.getEnd() == endRE) route.setEnd(vertRoute.getEnd());
            } else {
                // create arc between endRE and end of vertical route
                addConnectingArc(route, cell, endRE, vertRoute.getEnd(), endPoint, cornerLoc, endArc,
                        endArcWidth, endAngle, extendArcHead, extendArcTail, stayInside);
            }
        }
        else {
            // startRE and endRE can be connected with an arc.  If one of them is a bisectArcPin,
            // and can be replaced by the other, just replace it and we're done.
            if (route.replaceBisectPin(startRE, endRE)) {
                route.remove(startRE);
                return route;
            } else if (route.replaceBisectPin(endRE, startRE)) {
                route.remove(endRE);
                route.setEnd(startRE);
                return route;
            }
            // check single arc case
            if (DBMath.areEquals(startPoint.getX(), endPoint.getX()) || DBMath.areEquals(startPoint.getY(), endPoint.getY())) {
                addConnectingArc(route, cell, startRE, endRE, startPoint, endPoint, startArc,
                        startArcWidth, startAngle, extendArcHead, extendArcTail, stayInside);
            } else {
                PrimitiveNode pn = startArc.findOverridablePinProto();
                SizeOffset so = pn.getProtoSizeOffset();
                double defwidth = pn.getDefWidth()-so.getHighXOffset()-so.getLowXOffset();
                double defheight = pn.getDefHeight()-so.getHighYOffset()-so.getLowYOffset();
                RouteElementPort pinRE = RouteElementPort.newNode(cell, pn, pn.getPort(0), cornerLoc,
                        defwidth, defheight);
                route.add(pinRE);
                addConnectingArc(route, cell, startRE, pinRE, startPoint, cornerLoc, startArc,
                        startArcWidth, startAngle, extendArcHead, extendArcTail, stayInside);
                addConnectingArc(route, cell, endRE, pinRE, endPoint, cornerLoc, endArc,
                        endArcWidth, endAngle, extendArcHead, extendArcTail, stayInside);
            }

        }
        return route;

        //route.add(endRE); route.setEnd(endRE);

        // Tell Router to route between startRE and endRE
/*
        if (planRoute(route, cell, endRE, startPoint, endPoint, clicked, stayInside, vroute, contactsOnEndObject,
                extendArcHead, extendArcTail, contactArea)) {
            return route;
        }
        return new Route();             // error, return empty route
*/
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
            PrimitiveNode pn = ai.getProto().findOverridablePinProto();
            return pn.getPort(0);
        }
        if (routeObj instanceof PortInst) {
            PortInst pi = (PortInst)routeObj;
            return pi.getPortProto();
        }
        return null;
    }

/*
    protected static double getArcWidthToUse(ElectricObject routeObj, ArcProto ap) {
        double width = -1;
        if (routeObj instanceof ArcInst) {
            ArcInst ai = (ArcInst)routeObj;
            if (ai.getProto() == ap)
                return ai.getLambdaBaseWidth();
        }
        if (routeObj instanceof PortInst) {
            width = Router.getArcWidthToUse((PortInst)routeObj, ap, 0);
        }
        return width;
    }
*/

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
     * @param startArc the arc type to connect to startObj
     * @param endArc the arc type to connect to endObj
     */
    protected static void getConnectingPoints(ElectricObject startObj, ElectricObject endObj, Point2D clicked,
                                              Point2D startPoint, Point2D endPoint, Poly startPoly, Poly endPoly,
                                              ArcProto startArc, ArcProto endArc) {

/*        Point2D[] points = startPoly.getPoints();
        System.out.print("StartPoly: ");
        for (int i=0; i<points.length; i++) {
            System.out.print(points[i]+", ");
        }
        System.out.println("");
        if (endPoly != null) {
            points = endPoly.getPoints();
            System.out.print("EndPoly: ");
            for (int i=0; i<points.length; i++) {
                System.out.print(points[i]+", ");
            }
            System.out.println("");
        }*/
        if ((startPoly.getBox() == null && startPoly.getPoints().length != 2) ||
            (endPoly != null && endPoly.getBox() == null && endPoly.getPoints().length != 2)) {
            // special case: one of the polys is not a rectangle
            startPoint.setLocation(startPoly.closestPoint(clicked));
            if (endPoly == null) {
                endPoint.setLocation(getClosestOrthogonalPoint(startPoint, clicked));
            } else {
                endPoint.setLocation(endPoly.closestPoint(clicked));
            }
            return;
        }

        // just go by bounds for now
        Rectangle2D startBounds = startPoly.getBounds2D();
        // default is center point
        startPoint.setLocation(startBounds.getCenterX(), startBounds.getCenterY());

        // if startObj is arc inst
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
            int angleIncrement = endArc.getAngleIncrement();
            endPoint.setLocation(getClosestAngledPoint(startPoint, clicked, angleIncrement));
            //endPoint.setLocation(getClosestOrthogonalPoint(startPoint, clicked));
            // however, if this is an Artwork technology, just put end point at mouse
            if (startArc.getTechnology() == Artwork.tech())
                endPoint.setLocation(clicked);
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

        double lowerBoundX = Math.max(startBounds.getMinX(), endBounds.getMinX());
        double upperBoundX = Math.min(startBounds.getMaxX(), endBounds.getMaxX());
        boolean overlapX = lowerBoundX <= upperBoundX;
        double lowerBoundY = Math.max(startBounds.getMinY(), endBounds.getMinY());
        double upperBoundY = Math.min(startBounds.getMaxY(), endBounds.getMaxY());
        boolean overlapY = lowerBoundY <= upperBoundY;

        if (ClickZoomWireListener.theOne.getUseFatWiringMode()) {
            Rectangle2D startObjBounds = getBounds(startObj);
            Rectangle2D endObjBounds = getBounds(endObj);
            boolean objsOverlap = false;
            if (startObjBounds != null && endObjBounds != null) {
                if (startArc == endArc && startObjBounds.intersects(endObjBounds))
                    objsOverlap = true;
            }
            // normally we route center to center (for PortInsts) in fat wiring mode,
            // but if the objects overlap (both ports and objects themselves, then we will route directly
            if (!objsOverlap || !overlapX) {
                // center X on both objects (if they are port insts)
                if (startObj instanceof PortInst) {
                    startBounds.setRect(startBounds.getCenterX(), startBounds.getY(), 0, startBounds.getHeight());
                }
                if (endObj instanceof PortInst) {
                    endBounds.setRect(endBounds.getCenterX(), endBounds.getY(), 0, endBounds.getHeight());
                }
            }
            if (!objsOverlap || !overlapY) {
                // center Y on both objects (if they are port insts)
                if (startObj instanceof PortInst) {
                    startBounds.setRect(startBounds.getX(), startBounds.getCenterY(), startBounds.getWidth(), 0);
                }
                if (endObj instanceof PortInst) {
                    endBounds.setRect(endBounds.getX(), endBounds.getCenterY(), endBounds.getWidth(), 0);
                }
            }
            // recalculate overlaps in case bounds have changed
            lowerBoundX = Math.max(startBounds.getMinX(), endBounds.getMinX());
            upperBoundX = Math.min(startBounds.getMaxX(), endBounds.getMaxX());
            overlapX = lowerBoundX <= upperBoundX;
            lowerBoundY = Math.max(startBounds.getMinY(), endBounds.getMinY());
            upperBoundY = Math.min(startBounds.getMaxY(), endBounds.getMaxY());
            overlapY = lowerBoundY <= upperBoundY;
        }

        // check if bounds share X and Y space
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
     * Get bounds of primitive instance. Returns null if object is not an instance of a primitive.
     * @param obj the object
     * @return the bounds of the instance
     */
    protected static Rectangle2D getBounds(ElectricObject obj) {
        if (obj instanceof ArcInst) {
            ArcInst ai = (ArcInst)obj;
            return ai.getBounds();
        }
        if (obj instanceof PortInst) {
            PortInst pi = (PortInst)obj;
            obj = pi.getNodeInst();
        }
        if (obj instanceof NodeInst) {
            NodeInst ni = (NodeInst)obj;
            NodeProto np = ni.getProto();
            if (np instanceof PrimitiveNode) {
                return ni.getBounds();
            }
        }
        return null;
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
            if (!ni.isCellInstance()) compressPort = true;
            Poly poly = ni.getShapeOfPort(pp, clicked, compressPort, arcWidth); // this is for multi-site ports
            return poly;
        }
        if (obj instanceof ArcInst) {
            // make poly out of possible connecting points on arc
            ArcInst arc = (ArcInst)obj;
            Point2D [] points = new Point2D[2];
            points[0] = arc.getHeadLocation();
            points[1] = arc.getTailLocation();
            Poly poly = new Poly(points);
            return poly;
        }
        return null;
    }

    protected static Rectangle2D getLayerArea(ElectricObject obj, Layer layer) {

        if (obj instanceof PortInst) {
            PortInst pi = (PortInst)obj;
            NodeInst ni = pi.getNodeInst();
            NodeProto np = ni.getProto();
            while (np instanceof Cell) {
                // TODO: add code to extract initial exported primitive object,
                // and create affinetransform to translate extracted poly back up to current level
                return null;
            }

            if (np instanceof PrimitiveNode) {
                PrimitiveNode pn = (PrimitiveNode)np;

                // if pin, use area of arcs connected to it
                if (pn.getFunction() == PrimitiveNode.Function.PIN) {
                    // determine size by arcs connected to it
                    Rectangle2D horiz = null;
                    Rectangle2D vert = null;
                    for (Iterator<Connection> it = pi.getConnections(); it.hasNext(); ) {
                        Connection conn = it.next();
                        ArcInst ai = conn.getArc();
                        if (ai.getProto().getLayerIterator().next() != layer) continue;
                        int angle = ai.getAngle();
                        if (angle % 1800 == 0) {
                            if (horiz == null)
                                horiz = ai.getBounds();
                            else
                                horiz = horiz.createUnion(ai.getBounds());
                        }
                        if ((angle + 900) % 1800 == 0) {
                            if (vert == null)
                                vert = ai.getBounds();
                            else
                                vert = vert.createUnion(ai.getBounds());
                        }
                    }
                    return horiz.createIntersection(vert);
                }

                // if contact, use size of contact
                if (pn.getFunction() == PrimitiveNode.Function.CONTACT) {
                    Poly p = ni.getBaseShape();
                    return p.getBounds2D();
                }
            }
        }
        if (obj instanceof ArcInst) {
            ArcInst ai = (ArcInst)obj;
            return ai.getBounds();
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
     * @param stayInside the area in which to route (null if not applicable).
     * @return a RouteElement holding the new pin at the bisection
     * point, or a RouteElement holding an existingPortInst if
     * drawing from either end of the ArcInst.
     */
    protected RouteElementPort findArcConnectingPoint(Route route, ArcInst arc, Point2D point, PolyMerge stayInside) {

        EPoint head = arc.getHeadLocation();
        EPoint tail = arc.getTailLocation();
        RouteElementPort headRE = RouteElementPort.existingPortInst(arc.getHeadPortInst(), head);
        RouteElementPort tailRE = RouteElementPort.existingPortInst(arc.getTailPortInst(), tail);
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
                startRE = bisectArc(route, arc, location, stayInside);
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
                startRE = bisectArc(route, arc, location, stayInside);
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
     * @param stayInside the area in which to route (null if not applicable).
     * @return the RouteElement from which to continue the route
     */
    protected RouteElementPort bisectArc(Route route, ArcInst arc, Point2D bisectPoint, PolyMerge stayInside) {

        Cell cell = arc.getParent();
        EPoint head = arc.getHeadLocation();
        EPoint tail = arc.getTailLocation();

        // determine pin type to use if bisecting arc
        PrimitiveNode pn = arc.getProto().findOverridablePinProto();
        SizeOffset so = pn.getProtoSizeOffset();
        double width = pn.getDefWidth()-so.getHighXOffset()-so.getLowXOffset();
        double height = pn.getDefHeight()-so.getHighYOffset()-so.getLowYOffset();

        // make new pin
        RouteElementPort newPinRE = RouteElementPort.newNode(cell, pn, pn.getPort(0),
                bisectPoint, width, height);
        newPinRE.setBisectArcPin(true);

        // make dummy end pins
        RouteElementPort headRE = RouteElementPort.existingPortInst(arc.getHeadPortInst(), head);
        RouteElementPort tailRE = RouteElementPort.existingPortInst(arc.getTailPortInst(), tail);
        headRE.setShowHighlight(false);
        tailRE.setShowHighlight(false);
        // put name on longer arc
        String name1 = null;
        String name2 = null;
        if (head.distance(bisectPoint) > tail.distance(bisectPoint))
            name1 = arc.getName();
        else
            name2 = arc.getName();
        // add two arcs to rebuild old startArc
        RouteElement newHeadArcRE = RouteElementArc.newArc(cell, arc.getProto(), arc.getLambdaBaseWidth(), headRE, newPinRE,
                head, bisectPoint, name1, arc.getTextDescriptor(ArcInst.ARC_NAME), arc, true, true, stayInside);
        RouteElement newTailArcRE = RouteElementArc.newArc(cell, arc.getProto(), arc.getLambdaBaseWidth(), newPinRE, tailRE,
                bisectPoint, tail, name2, arc.getTextDescriptor(ArcInst.ARC_NAME), arc, true, true, stayInside);
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

    protected static Point2D getCornerLocation(Point2D startLoc, Point2D endLoc, Point2D clicked, ArcProto startArc, ArcProto endArc,
                                               boolean contactsOnEndObj, PolyMerge stayInside, Rectangle2D contactArea,
                                               Poly startPolyFull, Poly endPolyFull) {
        // if startArc and endArc are the same, see if we can connect directly with whatever angle increment is on the arc
        boolean singleArc = false;
        if (startArc == endArc) {
            int inc = 10*startArc.getAngleIncrement();
            if (inc == 0) singleArc = true; else
            {
                int ang = GenMath.figureAngle(startLoc, endLoc);
                if ((ang % inc) == 0) singleArc = true;
            }
        }
        else {
            // see if start and end line up in X or Y
            if (startLoc.getX() == endLoc.getX() || startLoc.getY() == endLoc.getY()) singleArc = true;
        }

        // if single arc, corner loc is either start or end loc
        if (singleArc) {
            if (contactsOnEndObj)
                return new Point2D.Double(endLoc.getX(), endLoc.getY());
            else
                return new Point2D.Double(startLoc.getX(), startLoc.getY());
        }

        // connection point specified by contactArea
        if (contactArea != null) {
            return new Point2D.Double(contactArea.getCenterX(), contactArea.getCenterY());
        }

        Point2D cornerLoc = null;

        Point2D pin1 = new Point2D.Double(startLoc.getX(), endLoc.getY());
        Point2D pin2 = new Point2D.Double(endLoc.getX(), startLoc.getY());

        int clickedQuad = findQuadrant(endLoc, clicked);
        int pin1Quad = findQuadrant(endLoc, pin1);
        int pin2Quad = findQuadrant(endLoc, pin2);
        int oppositeQuad = (clickedQuad + 2) % 4;
        // presume pin1 by default
        cornerLoc = pin1;
        if (pin2Quad == clickedQuad)
        {
            cornerLoc = pin2;                // same quad as pin2, use pin2
        } else if (pin1Quad == clickedQuad)
        {
            cornerLoc = pin1;                // same quad as pin1, use pin1
        } else if (pin1Quad == oppositeQuad)
        {
            cornerLoc = pin2;                // near to pin2 quad, use pin2
        }

        // special case - if poly sites overlap and corner is within that area, use that corner
        if (startPolyFull.intersects(endPolyFull)) {
            boolean usepin1 = false, usepin2 = false;
            if (startPolyFull.contains(pin1) && endPolyFull.contains(pin1)) usepin1 = true;
            if (startPolyFull.contains(pin2) && endPolyFull.contains(pin2)) usepin2 = true;
            if (usepin1 && !usepin2) cornerLoc = pin1;
            if (usepin2 && !usepin1) cornerLoc = pin2;
        }

        ArcProto useArc = startArc;
        if (!contactsOnEndObj) useArc = endArc;
        
        if (stayInside != null && useArc != null)
        {
            // make sure the bend stays inside of the merge area
            double pinSize = useArc.getDefaultLambdaBaseWidth();
            Layer pinLayer = useArc.getLayerIterator().next();
            Rectangle2D pin1Rect = new Rectangle2D.Double(pin1.getX()-pinSize/2, pin1.getY()-pinSize/2, pinSize, pinSize);
            Rectangle2D pin2Rect = new Rectangle2D.Double(pin2.getX()-pinSize/2, pin2.getY()-pinSize/2, pinSize, pinSize);
            if (stayInside.contains(pinLayer, pin1Rect)) cornerLoc = pin1; else
                if (stayInside.contains(pinLayer, pin2Rect)) cornerLoc = pin2;
        }
        return cornerLoc;
    }

    protected static void updateContactArea(Rectangle2D contactArea, RouteElementPort re, Point2D cornerLoc, double arcWidth, int arcAngle) {
        if (arcAngle % 1800 == 0) {
            // horizontal arc
            if (contactArea.getHeight() < arcWidth) {
                contactArea.setRect(contactArea.getX(), contactArea.getCenterY()-arcWidth/2, contactArea.getWidth(), arcWidth);
            }
        }
        if ((arcAngle + 900) % 1800 == 0) {
            // vertical arc
            if (contactArea.getWidth() < arcWidth) {
                contactArea.setRect(contactArea.getCenterX()-arcWidth/2, contactArea.getY(), arcWidth, contactArea.getHeight());
            }
        }
    }

    protected static void addConnectingArc(Route route, Cell cell, RouteElementPort startRE, RouteElementPort endRE,
                                           Point2D startPoint, Point2D endPoint,
                                           ArcProto arc, double width, int arcAngle, boolean extendArcHead,
                                           boolean extendArcTail, PolyMerge stayInside) {

        if (extendArcHead) extendArcHead = getExtendArcEnd(startRE, width, arc, arcAngle, extendArcHead);
        if (extendArcTail) extendArcTail = getExtendArcEnd(endRE, width, arc, arcAngle, extendArcTail);
        RouteElementArc reArc = RouteElementArc.newArc(cell, arc, width, startRE, endRE, startPoint, endPoint,
                null, null, null, extendArcHead, extendArcTail, stayInside);
        reArc.setArcAngle(arcAngle);
        route.add(reArc);
    }

    protected static boolean getExtendArcEnd(RouteElementPort re, double arcWidth, ArcProto arc, int arcAngle, boolean defExtends) {
        NodeProto np = re.getNodeProto();
        if (np == null) return defExtends;

        boolean checkAttachedArcs = false;
        if (np instanceof Cell) {
            checkAttachedArcs = true;
        }
        if (np instanceof PrimitiveNode) {
            PrimitiveNode pn = (PrimitiveNode)np;
            if (pn.getFunction() == PrimitiveNode.Function.PIN) {
                checkAttachedArcs = true;
            }
            if (pn.getFunction() == PrimitiveNode.Function.CONTACT) {
                Dimension2D size = re.getNodeSize();
                if (arcAngle % 1800 == 0) {
                    if (arcWidth > size.getHeight()) return false;
                }
                if ((arcAngle+900) % 1800 == 0) {
                    if (arcWidth > size.getWidth()) return false;
                }
            }
        }

/*
        if (checkAttachedArcs && re.getPortInst() != null) {
            double attachedWidth = getArcWidthToUse(re.getPortInst(), arc, arcAngle);
            if (attachedWidth < arcWidth) return false;
        }
*/

        return defExtends;
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

    /**
     * Use to find the closest point to clicked to route a wire from startPoint.
     * This point will be at an angel from startPoint which is a multiple of
     * angleIncrement.  If stayInside and useArc are not null, then the two possible
     * closest points will be determined by if they reside inside stayInside,
     * rather than the closest point.
     * @param startPoint
     * @param clicked
     * @param angleIncrement
     * @return the closest point to clicked to route a wire.
     */
    protected static Point2D getClosestAngledPoint(Point2D startPoint, Point2D clicked,
                                                   int angleIncrement) {
        angleIncrement = Math.abs(angleIncrement);
        if (angleIncrement == 0) return clicked;
        // don't really have to call this for angleIncr of 90 as general purpose
        // code below does right thing, but it's a lot faster
        if (angleIncrement == 90) return getClosestOrthogonalPoint(startPoint, clicked);
        double angleInc = angleIncrement * Math.PI / 180;

        // calculate angle
        double x = clicked.getX()-startPoint.getX();
        double y = clicked.getY()-startPoint.getY();
        double angle = Math.atan2(y,x);
        //System.out.println("angle is "+angle);

        // find closest two angle increments
        double nearest1 = (int)(angle / angleInc) * angleInc;
        double nearest2 = (angle<0) ? nearest1 - angleInc : nearest1 + angleInc;
        //System.out.println("nearest1, nearest2 are "+nearest1+" and "+ nearest2);
        Point2D n1, n2;
        // two points on line are candidates: one uses clicked X, other uses clickedY
        double tan1 = Math.tan(nearest1);
        if (tan1 == 0) tan1 = 0.000001;
        //System.out.println("tan1 is "+tan1);
        Point2D n1_1 = new Point2D.Double(x, x*tan1);
        Point2D n1_2 = new Point2D.Double(y/tan1, y);
        //System.out.println("Point1's: "+n1_1+", "+n1_2);
        // take whichever is shorter
        if (n1_1.distance(x,y) < n1_2.distance(x,y))
            n1 = n1_1;
        else
            n1 = n1_2;
        // same for second angle
        double tan2 = Math.tan(nearest2);
        if (tan2 == 0) tan2 = 0.000001;
        //System.out.println("tan2 is "+tan2);
        Point2D n2_1 = new Point2D.Double(x, x*tan2);
        Point2D n2_2 = new Point2D.Double(y/tan2, y);
        //System.out.println("Point2's: "+n2_1+", "+n2_2);
        if (n2_1.distance(x,y) < n2_2.distance(x,y))
            n2 = n2_1;
        else
            n2 = n2_2;
        //System.out.println("Point1: "+n1);
        //System.out.println("Point2: "+n2);
        double xfinal, yfinal;
        // of the points on the two angles, take closest to clicked
        if (n2.distance(x,y) < n1.distance(x,y)) {
            xfinal = DBMath.round(n2.getX() + startPoint.getX());
            yfinal = DBMath.round(n2.getY() + startPoint.getY());
        } else {
            xfinal = DBMath.round(n1.getX() + startPoint.getX());
            yfinal = DBMath.round(n1.getY() + startPoint.getY());
        }
        return new Point2D.Double(xfinal, yfinal);
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

    /**
     * Determines what route quadrant pt is compared to refPoint.
     * A route can be drawn vertically or horizontally so this
     * method will return a number between 0 and 3, inclusive,
     * where quadrants are defined based on the angle relationship
     * of refPoint to pt.  Imagine a circle with <i>refPoint</i> as
     * the center and <i>pt</i> a point on the circumference of the
     * circle.  Then theta is the angle described by the arc refPoint->pt,
     * and quadrants are defined as:
     * <code>
     * <p>quadrant :     angle (theta)
     * <p>0 :            -45 degrees to 45 degrees
     * <p>1 :            45 degress to 135 degrees
     * <p>2 :            135 degrees to 225 degrees
     * <p>3 :            225 degrees to 315 degrees (-45 degrees)
     *
     * @param refPoint reference point
     * @param pt variable point
     * @return which quadrant <i>pt</i> is in.
     */
    protected static int findQuadrant(Point2D refPoint, Point2D pt) {
        // find angle
        double angle = Math.atan((pt.getY()-refPoint.getY())/(pt.getX()-refPoint.getX()));
        if (pt.getX() < refPoint.getX()) angle += Math.PI;
        if ((angle > -Math.PI/4) && (angle <= Math.PI/4))
            return 0;
        else if ((angle > Math.PI/4) && (angle <= Math.PI*3/4))
            return 1;
        else if ((angle > Math.PI*3/4) &&(angle <= Math.PI*5/4))
            return 2;
        else
            return 3;
    }

}

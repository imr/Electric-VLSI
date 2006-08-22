/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VerticalRoute.java
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

import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.User;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.geom.Point2D;

/**
 * Class to route vertically (in Z direction) between two RouteElements.
 * The class is used as following:
 * <p>After creating the object, call specifyRoute() to find a way to connect
 * between startRE and endRE RouteElement objects.  At this point you may wish to
 * use the information about the specified route before actually building the route.
 * Right now the only useful information that is exported is the start and end ArcProtos
 * used, if not already specified.
 * <p>Once satisfied with the specification, call buildRoute() to create all
 * the RouteElements that determine exactly what the route will look like. Note
 * that this does not actually create any objects, it just creates a Route, which
 * can then be used to create Electric database objects.
 * <p>There are two forms of build route, the first tries to figure out everything
 * for you (contact sizes, arc angles), and connects to startRE and endRE if given.
 * The second just creates RouteElements from the specification, and you need to give
 * it the contact size and arc angle, and it does not connect to startRE or endRE.
 */
public class VerticalRoute {
    /** start of the vertical route */          private PortProto startPort;
    /** end of the vertical route */            private PortProto endPort;
    /** list of arcs and nodes to make route */ private SpecifiedRoute specifiedRoute;
    /** list of all valid specified routes */   private List<SpecifiedRoute> allSpecifiedRoutes;
    /** first arc (from startRE) */             private ArcProto startArc;
    /** last arct (to endRE) */                 private ArcProto endArc;

    /** the possible start arcs */              private ArcProto [] startArcs;
    /** the possible end arcs */                private ArcProto [] endArcs;
    /** if route specification succeeded */     private boolean specificationSucceeded;

    private int searchNumber;
    private static final int SEARCHLIMIT = 3000;
    private static final boolean DEBUG = false;
    private static final boolean DEBUGSEARCH = false;
    private static final boolean DEBUGTERSE = false;

    private static class SpecifiedRoute extends ArrayList<Object> {
        ArcProto startArc;
        ArcProto endArc;

        void printRoute() {
            for (int k=0; k<size(); k++) {
                System.out.println("   "+k+": "+get(k));
            }
        }
    }

    /**
     * Private constructor. Any of start/endPort, or start/endArc may be null, however
     * startArcs and endArcs must not be null.  They are the possible arcs to connect between
     * startPort/Arc and endPort/Arc.
     * @param startPort the start port of the route
     * @param endPort the end port of the route
     * @param startArc the start arc of the route
     * @param endArc the end arc of the route
     * @param startArcs the possible starting arcs
     * @param endArcs the possible ending arcs
     */
    private VerticalRoute(PortProto startPort, PortProto endPort, ArcProto startArc, ArcProto endArc,
                          ArcProto [] startArcs, ArcProto [] endArcs) {
        this.startPort = startPort;
        this.endPort = endPort;
        // special case: if port is a universal port, limit arc lists, otherwise
        // searching entire space for best connection will take forever
        if (DEBUGTERSE) {
            System.out.println("Searching for way to connect "+startPort.getBasePort().getParent()+
                    " and "+endPort.getBasePort().getParent());
        }
        if ((startPort.getBasePort().getParent() == Generic.tech.universalPinNode &&
            endPort.getBasePort().getParent() == Generic.tech.universalPinNode) ||
            (startPort.getBasePort().getParent() == Generic.tech.invisiblePinNode &&
            endPort.getBasePort().getParent() == Generic.tech.invisiblePinNode)) {
            startArc = endArc = User.getUserTool().getCurrentArcProto();
            startArcs = endArcs = new ArcProto [] { startArc };
        }
        this.startArc = startArc;
        this.endArc = endArc;
        this.startArcs = copyArcArray(startArcs);
        this.endArcs = copyArcArray(endArcs);
        specifiedRoute = null;
        allSpecifiedRoutes = null;
        specificationSucceeded = false;
    }
    
    /**
     * Create new VerticalRoute object to route between startRE and endRE
     * @param startPort the start port of the route
     * @param endPort the end port of the route
     */
    public static VerticalRoute newRoute(PortProto startPort, PortProto endPort) {
        ArcProto [] startArcs = startPort.getBasePort().getConnections();
        ArcProto [] endArcs = endPort.getBasePort().getConnections();
        VerticalRoute vr = new VerticalRoute(startPort, endPort, null, null, startArcs, endArcs);
        vr.specificationSucceeded = vr.specifyRoute();
        return vr;
    }

    /**
     * Create new VerticalRoute object to route between startRE and endArc
     * @param startPort the start port of the route
     * @param endArc and arc the end of the route will be able to connect to
     */
    public static VerticalRoute newRoute(PortProto startPort, ArcProto endArc) {
        ArcProto [] startArcs = startPort.getBasePort().getConnections();
        ArcProto [] endArcs = {endArc};
        VerticalRoute vr = new VerticalRoute(startPort, null, null, endArc, startArcs, endArcs);
        vr.specificationSucceeded = vr.specifyRoute();
        return vr;
    }

    /**
     * Get the arc used to start the vertical route from startRE
     * @return the start arc, or null if route could not be found or not created
     */
    public ArcProto getStartArc() { return startArc; }

    /**
     * Get the arc used to end the vertical route to endRE
     * @return the end arc, or null if route could not be found or not created
     */
    public ArcProto getEndArc() { return endArc; }

    /**
     * See if specification succeeded and VerticalRoute contains a valid specification
     * @return true if succeeded, false otherwise.
     */
    public boolean isSpecificationSucceeded() { return specificationSucceeded; }

    // we need to copy the array, because we want to modify it
    private ArcProto [] copyArcArray(ArcProto [] arcs) {
        ArcProto [] copy = new ArcProto[arcs.length];
        for (int i=0; i<arcs.length; i++) {
            ArcProto arc = arcs[i];
            // get rid of arcs we won't route with
            if (arc == Generic.tech.universal_arc && User.getUserTool().getCurrentArcProto() != Generic.tech.universal_arc) arc = null;
            if (arc == Generic.tech.invisible_arc && User.getUserTool().getCurrentArcProto() != Generic.tech.invisible_arc) arc = null;
            if (arc == Generic.tech.unrouted_arc && User.getUserTool().getCurrentArcProto() != Generic.tech.unrouted_arc) arc = null;
            if ((arc != null) && (arc.isNotUsed())) arc = null;
            copy[i] = arc;
        }
        return copy;
    }

    /**
     * Specify a Route between startRE and endRE
     * @return true if a route was found, false otherwise
     */
    private boolean specifyRoute() {

        if (endArcs == null || startArcs == null) {
            System.out.println("VerticalRoute: invalid start or end point");
            return false;
        }

        return specifyRoute(startArcs, endArcs);
    }

    /**
     * Builds a Route using the specification from specifyRoute(). It connects
     * this route up to startRE and endRE if they were specified.
     * Note that this may create non-orthogonal
     * arcs if startRE and endRE are not orthogonal to location.  Also,
     * startRE and endRE must have valid ports (i.e. are existingPortInst or newNode
     * types) if they are non-null. This method automatically determines the contact size, and the
     * angle of all the zero length arcs.
     * @param route the route to append with the new RouteElements
     * @param cell the cell in which to create the vertical route
     * @param location where to create the route (database units)
     */
    public void buildRoute(Route route, Cell cell, RouteElementPort startRE, RouteElementPort endRE,
                           Point2D startLoc, Point2D endLoc, Point2D location) {

        if (specifiedRoute == null) {
            System.out.println("Error: Trying to build VerticalRoute without a call to specifyRoute() first");
            return;
        }
        if (specifiedRoute.size() == 0) return;

        if (startRE != null) if (!route.contains(startRE)) route.add(startRE);
        if (endRE != null) if (!route.contains(endRE)) route.add(endRE);

        // set angle by start arc if it is vertical, otherwise angle is zero
        int arcAngle = 0;
        if (startRE != null) {
//            if (startRE.getLocation().getX() == location.getX() &&
//                startRE.getLocation().getY() != location.getY()) arcAngle = 900;
            if (startLoc.getX() == location.getX() &&
                startLoc.getY() != location.getY()) arcAngle = 900;
        }

        // create Route, using default contact size
        Route vertRoute = buildRoute(cell, location, new Dimension2D.Double(-1,-1), arcAngle);

        // remove startRE and endRE if they are bisect arc pins and at same location,
        // otherwise, connect them to start and end of vertical route
        double width;
        if (startRE != null) {
            if (route.replacePin(startRE, vertRoute.getStart())) {
                route.remove(startRE);
                if (route.getStart() == startRE) route.setStart(vertRoute.getStart());
            } else {
                width = Router.getArcWidthToUse(startRE, startArc);
                RouteElement arc1 = RouteElementArc.newArc(cell, startArc, width, startRE, vertRoute.getStart(),
                        startLoc, location, null, null, null, true, null);
                route.add(arc1);
            }
        }
        if (endRE != null) {
            if (route.replacePin(endRE, vertRoute.getEnd())) {
                route.remove(endRE);
                if (route.getEnd() == endRE) route.setEnd(vertRoute.getEnd());
            } else {
                width = Router.getArcWidthToUse(endRE, endArc);
                RouteElement arc2 = RouteElementArc.newArc(cell, endArc, width, endRE, vertRoute.getEnd(),
                        endLoc, location, null, null, null, true, null);
                route.add(arc2);
            }
        } else {
            if (route.getEnd() == null) {
                // both endRE and end of route are null, use end of vertical route
                route.setEnd(vertRoute.getEnd());
            }
        }

        // resize contacts to right size, and add to route
        Dimension2D size = Router.getContactSize(vertRoute.getStart(), vertRoute.getEnd());
        for (RouteElement re : vertRoute) {
            if (re instanceof RouteElementPort)
                ((RouteElementPort)re).setNodeSize(size);
            if (!route.contains(re)) route.add(re);
        }

    }

    /**
     * Builds a Route using the specification from specifyRoute(), but without
     * connecting to startRE and endRE.  The start of the returned Route can connect
     * to startRE, and the end of the returned Route can connect to endRE.
     * The caller must handle the final connections.
     * @param cell the Cell in which to create the route
     * @param location where in the database the vertical route is to be created
     * @param contactSize the size of contacts
     * @param arcAngle angle of zero length arcs created between contacts (usually zero)
     * @return a Route whose start can connect to startRE and whose end
     * can connect to endRE. Returns null if no specification for the route exists.
     */
    public Route buildRoute(Cell cell, Point2D location, Dimension2D contactSize, int arcAngle) {
        if (specifiedRoute == null) {
            System.out.println("Error: Trying to build VerticalRoute without a call to specifyRoute() first");
            return null;
        }
        Route route = new Route();
        if (specifiedRoute.size() == 0) return route;
        if (DEBUG) {
            System.out.println("Building route: ");
            for (Object obj : specifiedRoute) {
                System.out.println("  "+obj);
            }
        }

        // pull off the first object, which will be a port, and create contact from that
        PrimitivePort pp = (PrimitivePort)specifiedRoute.remove(0);
        RouteElementPort node = RouteElementPort.newNode(cell, pp.getParent(), pp,
                location, contactSize.getWidth(), contactSize.getHeight());
        route.add(node);
        route.setStart(node);
        route.setEnd(node);

        // now iterate through rest of list and create arc,port route element pairs
        for (Iterator<Object> it = specifiedRoute.iterator(); it.hasNext(); ) {
            ArcProto ap = (ArcProto)it.next();
            PrimitivePort port = (PrimitivePort)it.next();

            // create node
            RouteElementPort newNode = RouteElementPort.newNode(cell, port.getParent(), port,
                    location, contactSize.getWidth(), contactSize.getHeight());
            route.add(newNode);
            route.setEnd(newNode);

            // create arc
            double arcWidth = Router.getArcWidthToUse(node, ap);
            RouteElementArc arc = RouteElementArc.newArc(cell, ap, arcWidth, node, newNode, location, location, null, null, null, true, null);
            arc.setArcAngle(arcAngle);
            route.add(arc);

            node = newNode;
        }

        return route;
    }

    /**
     * Specify the route
     */
    private boolean specifyRoute(ArcProto [] startArcs, ArcProto [] endArcs) {

        specifiedRoute = new SpecifiedRoute();
        allSpecifiedRoutes = new ArrayList<SpecifiedRoute>();
        this.startArc = null;
        this.endArc = null;

        // try to find a way to connect, do exhaustive search
        for (int i=0; i<startArcs.length; i++) {
            for (int j=0; j<endArcs.length; j++) {
                ArcProto startArc = startArcs[i];
                ArcProto endArc = endArcs[j];
                if (startArc == null || endArc == null) continue;

                specifiedRoute.clear();
                specifiedRoute.startArc = startArc;
                specifiedRoute.endArc = endArc;
                searchNumber = 0;
                if (DEBUGSEARCH || DEBUGTERSE) System.out.println("** Start search startArc="+startArc+", endArc="+endArc);
                findConnectingPorts(startArc, endArc, new StringBuffer());
                if (DEBUGSEARCH || DEBUGTERSE) System.out.println("   Search reached searchNumber "+searchNumber);
            }
        }

        if (allSpecifiedRoutes.size() == 0) return false;           // nothing found

        // choose shortest route
        specifiedRoute = allSpecifiedRoutes.get(0);
        List<SpecifiedRoute> zeroLengthRoutes = new ArrayList<SpecifiedRoute>();
        for (int i=0; i<allSpecifiedRoutes.size(); i++) {
            SpecifiedRoute r = allSpecifiedRoutes.get(i);
            if (r.size() < specifiedRoute.size()) specifiedRoute = r;
            if (r.size() == 0) zeroLengthRoutes.add(r);
        }
        // if multiple ways to connect that use only one wire, choose
        // the one that uses the current wire, if any.
        if (zeroLengthRoutes.size() > 0) {
            for (SpecifiedRoute r : zeroLengthRoutes) {
                if (r.startArc == User.getUserTool().getCurrentArcProto())
                    specifiedRoute = r;
            }
        }

        allSpecifiedRoutes.clear();
        startArc = specifiedRoute.startArc;
        endArc = specifiedRoute.endArc;
        if (DEBUGSEARCH || DEBUGTERSE) {
            System.out.println("*** Using Best Route: ");
            specifiedRoute.printRoute();
        }

        return true;
    }

    /**
     * Recursive method to create a specification list of ports and arcs
     * that connect startArc to endArc.  The list will be odd in length
     * (or zero if startArc and endArc are the same). It will consist
     * of a PortProto, and zero or more ArcProto,PortProto pairs in that order.
     * The first PortProto will be able to connect to the initial startArc,
     * and the last PortProto will be able to connect to the final endArc.
     * <p>PortProtos used are Ports from the current technology whose parents
     * (NodeProtos) have the function of CONTACT.
     * @param startArc connect from this arc
     * @param endArc connect to this arc
     * @param ds spacing for debug messages, if enabled
     */
    private void findConnectingPorts(ArcProto startArc, ArcProto endArc, StringBuffer ds) {

        // throw away route if it's longer than shortest good route
        if (specifiedRoute.size() > getShortestRouteLength())
            return;

        if (startArc == endArc) {
            saveRoute(specifiedRoute);
            if (DEBUGTERSE) System.out.println("  --Found good route of length "+specifiedRoute.size());
            return;    // don't need anything to connect between them
        }

        ds.append("  ");
        if (searchNumber > SEARCHLIMIT) { return; }
        if (searchNumber == SEARCHLIMIT) {
            System.out.println("Search limit reached in VerticalRoute");
            searchNumber++;
            return;
        }
        searchNumber++;
        Technology tech = startArc.getTechnology();

        // see if we can find a port in the current technology
        // that will connect the two arcs
		for (Iterator<PrimitiveNode> nodesIt = tech.getNodes(); nodesIt.hasNext(); ) {
			PrimitiveNode pn = nodesIt.next();
            // ignore anything that is noy CONTACT
            if (pn.getFunction() != PrimitiveNode.Function.CONTACT) continue;

			for (Iterator<PortProto> portsIt = pn.getPorts(); portsIt.hasNext(); ) {
				PrimitivePort pp = (PrimitivePort)portsIt.next();
				if (DEBUGSEARCH) System.out.println(ds+"Checking if "+pp+" connects between "+startArc+" and "+endArc);
				if (pp.connectsTo(startArc) && pp.connectsTo(endArc)) {
					specifiedRoute.add(pp);
					saveRoute(specifiedRoute);
					return;                                // this connects between both arcs
				}
			}
		}

        // try all contact ports as an intermediate
		for (Iterator<PrimitiveNode> nodesIt = tech.getNodes(); nodesIt.hasNext(); ) {
			PrimitiveNode pn = nodesIt.next();
            // ignore anything that is noy CONTACT
            if (pn.getFunction() != PrimitiveNode.Function.CONTACT) continue;

			for (Iterator<PortProto> portsIt = pn.getPorts(); portsIt.hasNext(); ) {
				PrimitivePort pp = (PrimitivePort)portsIt.next();
				if (DEBUGSEARCH) System.out.println(ds+"Checking if "+pp+" (parent is "+pp.getParent()+") connects to "+startArc);
				if (pp.connectsTo(startArc)) {
					if (pp == startPort) continue;                       // ignore start port
					if (pp == endPort) continue;                         // ignore end port
					if (specifiedRoute.contains(pp)) continue;          // ignore ones we've already hit
					// add to list
					int prePortSize = specifiedRoute.size();
					specifiedRoute.add(pp);

					// now try to connect through all arcs that can connect to the found pp
					int preArcSize = specifiedRoute.size();
					ArcProto [] arcs = pp.getConnections();
					for (int i=0; i<arcs.length; i++) {
						ArcProto tryarc = arcs[i];
						if (tryarc == Generic.tech.universal_arc) continue;
						if (tryarc == Generic.tech.invisible_arc) continue;
						if (tryarc == Generic.tech.unrouted_arc) continue;
						if (tryarc.isNotUsed()) continue;
						if (tryarc == startArc) continue;           // already connecting through startArc
						if (tryarc == this.startArc) continue;      // original arc connecting from
						if (specifiedRoute.contains(tryarc)) continue;       // already used this arc
						specifiedRoute.add(tryarc);
						if (DEBUGSEARCH) System.out.println(ds+"...found intermediate node "+pp+" through "+startArc+" to "+tryarc);
						// recurse
						findConnectingPorts(tryarc, endArc, ds);

						// remove added arcs and port and continue search
						while (specifiedRoute.size() > preArcSize) {
							specifiedRoute.remove(specifiedRoute.size()-1);
						}
					}

					// that port didn't get us anywhere, clear list back to last good point
					while (specifiedRoute.size() > prePortSize) {
						specifiedRoute.remove(specifiedRoute.size()-1);
					}
				}
			}
		}

        if (DEBUGSEARCH) System.out.println(ds+"--- Bad path ---");
        return;               // no valid path to endpp found
    }

    /**
     * Save a successful route
     * @param route the route to save
     */
    private void saveRoute(SpecifiedRoute route) {
        // create copy and store it
        if (DEBUGSEARCH) {
            System.out.println("** Found Route for: startArc="+route.startArc+", endArc="+route.endArc);
            route.printRoute();
        }
        int shortestLength = getShortestRouteLength();
        if (route.size() > shortestLength) {
            // ignore it
            return;
        }
        SpecifiedRoute loggedRoute = new SpecifiedRoute();
        loggedRoute.startArc = route.startArc;
        loggedRoute.endArc = route.endArc;
        loggedRoute.addAll(route);
        allSpecifiedRoutes.add(loggedRoute);
        boolean trim = true;
        while (trim) {
            // remove shorter routes
            Iterator<SpecifiedRoute> it = null;
            for (it = allSpecifiedRoutes.iterator(); it.hasNext(); ) {
                SpecifiedRoute r = it.next();
                if (r.size() > shortestLength) {
                    allSpecifiedRoutes.remove(r);
                    break;
                }
            }
            if (!it.hasNext()) {
                trim = false;           // done trimming
            }
        }
    }

    /**
     * Get the length of the shortest route.
     */
    private int getShortestRouteLength() {
        // Because all routes should be of the
        // shortest length, just return the length of the first route
        if (allSpecifiedRoutes.size() == 0) return Integer.MAX_VALUE;
        SpecifiedRoute r = allSpecifiedRoutes.get(0);
        return r.size();
    }
}

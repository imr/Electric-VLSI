/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Router.java
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

import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.Job;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.technologies.Generic;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.awt.geom.Point2D;
import java.awt.*;

/**
 * Parent Class for all Routers.  I really have no idea what this
 * should look like because I've never written a real router,
 * but I've started it off with a few basics.
 * <p>
 * A Route is a List of RouteElements.  See RouteElement for details
 * of the elements.
 * <p>
 * User: gainsley
 * Date: Mar 1, 2004
 * Time: 2:48:46 PM
 */
public abstract class Router {

    /** set to tell user short info on what was done */     protected boolean verbose = false;

    // ------------------ Protected Abstract Router Methods -----------------

    /**
     * Plan a route starting from startRE, and ending at endRE.
     * Note that this method does add startRE and endRE to the
     * returned list of RouteElements.
     * @param route the list of RouteElements describing route to be modified
     * @param cell the cell in which to create the route
     * @param endRE the RouteElement that will be the new end of the route
     * @param hint can be used as a hint to the router for determining route.
     *        Ignored if null
     * @return false on error, route should be ignored.
     */
    protected abstract boolean planRoute(Route route, Cell cell, RouteElement endRE, Point2D hint);


    // --------------------------- Public Methods ---------------------------

    /**
     * Plan a route starting from startPort, and ending at endPort
     * @param route the list of RouteElements describing route to be modified
     * @param cell the cell in which to create the route
     * @param startPort the start of the route
     * @param endPort the end of the route
     * @param hint can be used as a hint to the router for determining route.
     *        Ignored if null
     * @return false on error, route should be ignored.
     */
    public boolean planRoute(Route route, Cell cell, PortInst startPort, PortInst endPort, Point2D hint) {
        RouteElement startRE = RouteElement.existingPortInst(startPort);
        RouteElement endRE = RouteElement.existingPortInst(endPort);
        route.add(startRE);
        route.setRouteStart(startRE);
        route.setRouteEnd(startRE);
        return planRoute(route, cell, endRE, hint);
    }

    /**
     * Create the route.  If finalRE is not null, set it as highlighted.
     * @param route the route to create
     * @param cell the cell in which to create the route
     */
    public void createRoute(Route route, Cell cell) {
        CreateRouteJob job = new CreateRouteJob(this, route, cell, verbose);
    }

    /** Return a string describing the Router */
    public abstract String toString();


    // -------------------------- Job to build route ------------------------

    /**
     * Job to create the route.
     * Highlights the end of the Route after it creates it.
     */
    protected static class CreateRouteJob extends Job {

        /** route to build */                       private Route route;
        /** print message on what was done */       private boolean verbose;
        /** cell in which to build route */         private Cell cell;

        /** Constructor */
        protected CreateRouteJob(Router router, Route route, Cell cell, boolean verbose) {
            super(router.toString(), User.tool, Job.Type.CHANGE, cell, null, Job.Priority.USER);
            this.route = route;
            this.verbose = verbose;
            this.cell = cell;
            startJob();
        }

        /** Implemented doIt() method to perform Job */
        public boolean doIt() {
            int arcsCreated = 0;
            int nodesCreated = 0;
            // pass 1: build all newNodes
            for (Iterator it = route.iterator(); it.hasNext(); ) {
                RouteElement e = (RouteElement)it.next();
                if (e.getAction() == RouteElement.RouteElementAction.newNode) {
                    e.doAction();
                    nodesCreated++;
                }
            }
            // pass 2: do all other actions (deletes, newArcs)
            for (Iterator it = route.iterator(); it.hasNext(); ) {
                RouteElement e = (RouteElement)it.next();
                e.doAction();
                if (e.getAction() == RouteElement.RouteElementAction.newArc)
                    arcsCreated++;
            }
            if (verbose) {
                if (arcsCreated == 1)
                    System.out.print("1 arc, ");
                else
                    System.out.print(arcsCreated+" arcs, ");
                if (nodesCreated == 1)
                    System.out.println("1 node created");
                else
                    System.out.println(nodesCreated+" nodes created");
            }
            RouteElement finalRE = route.getRouteEnd();
            if (finalRE != null) {
                Highlight.clear();
                PortInst pi = finalRE.getConnectingPort();
                if (pi != null) {
                    Highlight.addElectricObject(pi, cell);
                    Highlight.finished();
                }
            }
			return true;
       }
    }


    // ------------------------ Protected Utility Methods ---------------------

    /**
     * Routes vertically (in Z) from the end of the route to newEndRE, and then
     * makes newEndRE the end of the route.  Asserts that newEndRE is at the
     * location as the end of the route.
     * <P>For simplicity, this method assumes the RouteElement newEndRE, and the
     * RouteElement that is the end of the 'route', both have valid PortProtos
     * (i.e. they are newNodes or existingPortInst types of RouteElements).
     * <p>They must also have the same location.
     * @param newEndRE The new end of the Route.  Must be of type newNode or existingPortInst.
     * @return true if successful, otherwise returns false and does not modify route.
     */
    public boolean routeVertically(Route route, RouteElement newEndRE, Point2D contactLocation) {

        // newEndRE should not be part of route
        assert (!route.contains(newEndRE));

        // make sure both start and end of route have ports
        PortProto endPort = newEndRE.getPortProto();
        PortProto startPort = route.getRouteEnd().getPortProto();
        assert ((endPort != null) && (startPort != null));

        RouteElement startRE = route.getRouteEnd();

        ArcProto [] endArcs = endPort.getBasePort().getConnections();
        if (endArcs == null) return false;

        // see what arcs endPort can connect to, and try to route to each
        for (int i = 0; i < endArcs.length; i++) {
            ArcProto endArc = endArcs[i];
            if (endArc == Generic.tech.universal_arc) continue;
            if (endArc == Generic.tech.invisible_arc) continue;
            if (endArc == Generic.tech.unrouted_arc) continue;
            if (endArc.isNotUsed()) continue;

            Dimension2D contactSize = getContactSize(startRE, newEndRE);
            if (!routeVertically(route, endArc, contactSize, contactLocation)) continue;

            // add final connection
            RouteElement lastNode = route.getRouteEnd();
            if (newEndRE.isBisectArcPin() && newEndRE.getLocation().equals(lastNode.getLocation())) {
                // replace endRE with lastNode
                replaceRouteElementArcPin(route, newEndRE, lastNode);
                // no need to create final arc
            } else {
                // find arc width to use
                Cell cell = startRE.getCell();
                double startArcWidth = getArcWidthToUse(startRE, endArc);
                double endArcWidth = getArcWidthToUse(newEndRE, endArc);
                if (endArcWidth > startArcWidth) startArcWidth = endArcWidth;
                RouteElement arc = RouteElement.newArc(cell, endArc, startArcWidth, lastNode, newEndRE, null);
                route.add(arc);
                route.add(newEndRE);
                route.setRouteEnd(newEndRE);
            }
            return true;
        }
        return false;
    }


    /**
     * Extend the route vertically (in Z) from the end of the route to a new
     * RouteElement node that can connect to endArc.
     * @param endArc arc final element should be able to connect to
     * @return true on success, otherwise returns false and does not modify the route
     */
    public boolean routeVertically(Route route, ArcProto endArc, Dimension2D contactSize,
                                    Point2D contactLocation) {
        // get start port
        RouteElement startRE = route.getRouteEnd();
        PortProto startPort = startRE.getPortProto();
        if (startPort == null) return false;

        // see if we can find way to connect to endArc
        List bestRoute = new ArrayList();
        if (!findConnectingPorts(bestRoute, startPort, endArc)) return false;
        if (bestRoute.size() == 0) return false;

        if (contactLocation == null) return false;
        Cell cell = startRE.getCell();

        RouteElement lastNode = startRE;
        for (Iterator it = bestRoute.iterator(); it.hasNext(); ) {

            // should always be arc proto, primitive port pair
            ArcProto ap = (ArcProto)it.next();
            PrimitivePort pp = (PrimitivePort)it.next();

            // create new contact node RouteElement
            double defHeight = pp.getParent().getDefHeight();
            double defWidth = pp.getParent().getDefWidth();
            double height = (contactSize.getHeight() < defHeight) ? defHeight : contactSize.getHeight();
            double width = (contactSize.getWidth() < defWidth) ? defWidth : contactSize.getWidth();
            RouteElement node = RouteElement.newNode(cell, pp.getParent(), pp,
                    contactLocation, width, height);
            route.add(node);

            // if startRE is a bisectArcPin, replace it with first pin in vertical route
            if (lastNode == startRE && startRE.isBisectArcPin() &&
                    startRE.getLocation().equals(node.getLocation())) {
                replaceRouteElementArcPin(route, startRE, node);
                route.remove(startRE);
            } else {
                double arcWidth = getArcWidthToUse(lastNode, ap);
                RouteElement arc = RouteElement.newArc(cell, ap, arcWidth, lastNode, node, null);
                route.add(arc);
            }
            lastNode = node;        // add start of route (existing port inst)
        }
        route.setRouteEnd(lastNode);
        return true;
    }

    private static int searchNumber;
    private static final int SEARCHLIMIT = 100;
    private static final boolean DEBUG = false;
    /**
     * Find a way to connect between PortProto <i>start</i> and ArcProto <i>ap</i>.
     * If a way is found, a list of arc and port pairs is returned.  The list is always
     * even in length, with each pair being an ArcProto followed by a PrimitivePort.
     * The smallest example is a list of two elements, the first being an arc that can
     * connect to both <i>start</i> and the PrimitivePort that is the second element in the
     * list.  The PrimitivePort can also connect to <i>ap</i>. Use PrimitivePort.getParent()
     * to find the PrimitiveNode on which the PrimitivePort resides.
     * @param portsList an empty list into which to store the route description
     * @param start the start of the route
     * @param ap the arc the last primitive port will be able to connect to
     * @return true on success, false on failure
     */
    private boolean findConnectingPorts(List portsList, PortProto start, ArcProto ap) {
        if (DEBUG) System.out.println("***Trying to connect from "+start+" to "+ap);
        searchNumber = 0;
        return findConnectingPorts("  ", portsList, start, ap);
    }
    /** See findConnectingPorts (public version) */
    private boolean findConnectingPorts(String ds, List portsList, PortProto start, ArcProto ap) {
        // list should not be null
        if (portsList == null) return false;
        ds += "  ";

        if (searchNumber > SEARCHLIMIT) return false;
        searchNumber++;

        // find what start can connect to
        PrimitivePort startpp = start.getBasePort();
        ArcProto [] startArcs = startpp.getConnections();
        // find all ports that can connect to what start can connect to
        for (int i = 0; i < startArcs.length; i++) {
            ArcProto startArc = startArcs[i];
            if (startArc == Generic.tech.universal_arc) continue;
            if (startArc == Generic.tech.invisible_arc) continue;
            if (startArc == Generic.tech.unrouted_arc) continue;
            if (startArc.isNotUsed()) continue;
            if (portsList.contains(startArc)) continue; // shouldn't have to traverse same arc twice
            Technology tech = startArc.getTechnology();
            if (DEBUG) System.out.println(ds+"Checking: Node="+startpp+", Arc="+startArc);
            // first check if we can connect to end
            if (startArc == ap) {
                // we're done
                //portsList.add(startArc);
                if (DEBUG) System.out.println(ds+"...successfully connected to "+startArc);
                return true;
            }
            // find all primitive ports in technology that can connect to
            // this arc, and that are not already in list (and are not startpp)
            Iterator portsIt = tech.getPorts();
            for (; portsIt.hasNext(); ) {
                PrimitivePort pp = (PrimitivePort)portsIt.next();
                // ignore anything whose parent is not a CONTACT
                if (pp.getParent().getFunction() != NodeProto.Function.CONTACT) continue;
                if (DEBUG) System.out.println(ds+"Checking "+pp+" (parent is "+pp.getParent()+")");
                if (pp.connectsTo(startArc)) {
                    if (portsList.contains(pp)) continue;           // ignore ones we've already hit
                    if (pp == startpp) continue;                    // ignore start port
                    // add to list
                    int lastSize = portsList.size();
                    portsList.add(startArc); portsList.add(pp);
                    if (DEBUG) System.out.println(ds+"...found intermediate node "+pp+" through "+startArc);
                    // recurse, but ignore results if failed
                    if (findConnectingPorts(ds, portsList, pp, ap)) {
                        // success
                        return true;
                    }
                    // else remove added junk and continue search
                    while (portsList.size() > lastSize) {
                        portsList.remove(portsList.size()-1);
                    }
                }
            }
        }
        if (DEBUG) System.out.println(ds+"--- Bad path ---");
        return false;               // no valid path to endpp found
    }

    /**
     * Determine which arc type to use to connect two ports
     * NOTE: for safety, will NOT return a Generic.tech.universal_arc,
     * Generic.tech.invisible_arc, or Generic.tech.unrouted_arc,
     * unless it is the currently selected arc.  Will instead return null
     * if no other arc can be found to work.
     * @param port1 one end point of arc (ignored if null)
     * @param port2 other end point of arc (ignored if null)
     * @return the arc type (an ArcProto). null if none or error.
     */
    protected static ArcProto getArcToUse(PortProto port1, PortProto port2) {
        // current user selected arc
        ArcProto curAp = User.tool.getCurrentArcProto();
        ArcProto uni = Generic.tech.universal_arc;
        ArcProto invis = Generic.tech.invisible_arc;
        ArcProto unr = Generic.tech.unrouted_arc;

        PortProto pp1 = null, pp2 = null;
        // Note: this makes it so either port1 or port2 can be null,
        // but only pp2 can be null down below
        if (port1 == null) pp1 = port2; else {
            pp1 = port1; pp2 = port2; }
        if (pp1 == null && pp2 == null) return null;
        
        // Ignore pp2 if it is null
        if (pp2 == null) {
            // see if current arcproto works
            if (pp1.connectsTo(curAp)) return curAp;
            // otherwise, find one that does
            Technology tech = pp1.getParent().getTechnology();
            for(Iterator it = tech.getArcs(); it.hasNext(); )
            {
                ArcProto ap = (ArcProto)it.next();
                if (pp1.connectsTo(ap) && ap != uni && ap != invis && ap != unr) return ap;
            }
            // none in current technology: try any technology
            for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
            {
                Technology anyTech = (Technology)it.next();
                for(Iterator aIt = anyTech.getArcs(); aIt.hasNext(); )
                {
                    PrimitiveArc ap = (PrimitiveArc)aIt.next();
                    if (pp1.connectsTo(ap) && ap != uni && ap != invis && ap != unr) return ap;
                }
            }
        } else {
            // pp2 is not null, include it in search

            // see if current arcproto workds
            if (pp1.connectsTo(curAp) && pp2.connectsTo(curAp)) return curAp;
            // find one that works if current doesn't
            Technology tech = pp1.getParent().getTechnology();
            for(Iterator it = tech.getArcs(); it.hasNext(); )
            {
                ArcProto ap = (ArcProto)it.next();
                if (pp1.connectsTo(ap) && pp2.connectsTo(ap) && ap != uni && ap != invis && ap != unr) return ap;
            }
            // none in current technology: try any technology
            for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
            {
                Technology anyTech = (Technology)it.next();
                for(Iterator aIt = anyTech.getArcs(); aIt.hasNext(); )
                {
                    PrimitiveArc ap = (PrimitiveArc)aIt.next();
                    if (pp1.connectsTo(ap) && pp2.connectsTo(ap) && ap != uni && ap != invis && ap != unr) return ap;
                }
            }
        }
        return null;
    }

    protected void replaceRouteElementArcPin(Route route, RouteElement bisectPinRE, RouteElement newPinRE) {
        // go through route and update newArcs
        for (Iterator it = route.iterator(); it.hasNext(); ) {
            RouteElement e = (RouteElement)it.next();
            e.replaceArcEnd(bisectPinRE, newPinRE);
        }
    }

    protected static void cleanRoute(Route route) {

        // first pass: remove extra pins
        for (Iterator it = route.iterator(); it.hasNext(); ) {

            RouteElement re = (RouteElement)it.next();
            Point2D location = re.getLocation();
            if (location != null) {
                // check if
            }
        }
    }

    /**
     * Convert all new arcs of type 'ap' in route to use width of widest
     * arc of that type.
     */
    protected static void useWidestWire(Route route, ArcProto ap) {
        // get widest wire connected to anything in route
        double width = getArcWidthToUse(route, ap);
        // set all new arcs of that type to use that width
        for (Iterator it = route.iterator(); it.hasNext(); ) {
            RouteElement re = (RouteElement)it.next();
            if (re.getArcProto() == ap)
                re.setArcWidth(width);
        }
    }

    /**
     * Get arc width to use by searching for largest arc of passed type
     * connected to any elements in the route.
     * @param route the route to be searched
     * @param ap the arc type
     * @return the largest width
     */
    protected static double getArcWidthToUse(Route route, ArcProto ap) {
        double widest = ap.getDefaultWidth();
        for (Iterator it = route.iterator(); it.hasNext(); ) {
            RouteElement re = (RouteElement)it.next();
            double width = getArcWidthToUse(re, ap);
            if (width > widest) widest = width;
        }
        return widest;
    }

    /**
     * Get arc width to use to connect to PortInst pi.  Arc type
     * is ap.  Uses the largest width of arc type ap already connected
     * to pi, or the default width of ap if none found.<p>
     * You may specify pi as null, in which case it just returns
     * ap.getDefaultWidth().
     * @param pi the PortInst to connect to
     * @param ap the Arc type to connect with
     * @return the width to use to connect
     */
    protected static double getArcWidthToUse(PortInst pi, ArcProto ap) {
        // if pi null, just return default width of ap
        if (pi == null) return ap.getDefaultWidth();

        // get all ArcInsts on pi, find largest
        double width = 0;
        boolean found = false;
        for (Iterator it = pi.getConnections(); it.hasNext(); ) {
            Connection c = (Connection)it.next();
            found = true;
            if (width < c.getArc().getWidth()) width = c.getArc().getWidth();
        }
        if (!found) return ap.getDefaultWidth();
        return width;
    }

    /**
     * Get arc width to use to connect to RouteElement re. Uses largest
     * width of arc already connected to re.
     * @param re the RouteElement to connect to
     * @param ap the arc type (for default width)
     * @return the width of the arc to use to connect
     */
    protected static double getArcWidthToUse(RouteElement re, ArcProto ap) {
        double width = ap.getDefaultWidth();
        double connectedWidth = re.getWidestConnectingArc(ap);
        if (width > connectedWidth) return width;
        else return connectedWidth;
    }

    /**
     * Get the dimensions of a contact that will connect between startRE and endRE.
     */
    protected static Dimension2D getContactSize(RouteElement startRE, RouteElement endRE) {
        Dimension2D start = getContactSize(startRE);
        Dimension2D end = getContactSize(endRE);

        System.out.println("start contact size: "+start);
        System.out.println("end contact size: "+end);

        // use the largest of the dimensions
        Dimension2D dim = new Dimension2D.Double(start);
        if (end.getWidth() > dim.getWidth()) dim.setSize(end.getWidth(), dim.getHeight());
        if (end.getHeight() > dim.getHeight()) dim.setSize(dim.getWidth(), end.getHeight());


        System.out.println("final contact size: "+dim);
        return dim;
    }

    protected static Dimension2D getContactSize(RouteElement re) {

        double width = -1, height = -1;

        // if RE is an arc, use its arc width
        if (re.getAction() == RouteElement.RouteElementAction.newArc) {
            if (re.isNewArcVertical()) {
                if (re.getArcWidth() > width) width = re.getArcWidth();
            }
            if (re.isNewArcHorizontal()) {
                if (re.getArcWidth() > height) height = re.getArcWidth();
            }
        }

        // special case: if this is a contact cut, use the size of the contact cut
        if (re.getPortProto() != null) {
            if (re.getPortProto().getParent().getFunction() == NodeProto.Function.CONTACT) {
                return re.getNodeSize();
            }
        }

        // if RE is an existingPortInst, use width of arcs connected to it.
        if (re.getAction() == RouteElement.RouteElementAction.existingPortInst) {
            PortInst pi = re.getConnectingPort();
            for (Iterator it = pi.getConnections(); it.hasNext(); ) {
                Connection conn = (Connection)it.next();
                ArcInst arc = conn.getArc();

                Point2D head = arc.getHead().getLocation();
                Point2D tail = arc.getTail().getLocation();

                // use width of widest arc
                if (head.getX() == tail.getX()) {
                    if (arc.getWidth() > width) width = arc.getWidth();
                }
                if (head.getY() == tail.getY()) {
                    if (arc.getWidth() > height) height = arc.getWidth();
                }
            }
        }

        // if RE is a new Node, check it's arcs
        if (re.getAction() == RouteElement.RouteElementAction.newNode) {
            Dimension2D dim = null;
            for (Iterator it = re.getNewArcs(); it.hasNext(); ) {
                RouteElement newArcRE = (RouteElement)it.next();
                Dimension2D d = getContactSize(newArcRE);
                if (dim == null) dim = d;

                // use LARGEST of all dimensions
                if (d.getWidth() > dim.getWidth()) dim.setSize(d.getWidth(), dim.getHeight());
                if (d.getHeight() > dim.getHeight()) dim.setSize(dim.getWidth(), d.getHeight());
            }
            return dim;
        }

        return new Dimension2D.Double(width, height);
    }

}

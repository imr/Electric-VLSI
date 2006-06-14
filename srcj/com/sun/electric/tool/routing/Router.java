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

import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.User;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
    /** the tool that is making routes */					protected Tool tool;

    // ------------------ Abstract Router Methods -----------------

    /**
     * Plan a route from startRE to endRE.
     * startRE in this case will be route.getEndRE().  This builds upon whatever
     * route is already in 'route'. 
     * @param route the list of RouteElements describing route to be modified
     * @param cell the cell in which to create the route
     * @param endRE the RouteElementPort that will be the new end of the route
     * @param startLoc the location to attach an arc to on startRE
     * @param endLoc the location to attach an arc to on endRE
     * @param hint can be used as a hint to the router for determining route.
     *        Ignored if null
     * @return false on error, route should be ignored.
     */
//    protected abstract boolean planRoute(Route route, Cell cell, RouteElementPort endRE,
//                                         Point2D startLoc, Point2D endLoc, Point2D hint);

    /** Return a string describing the Router */
    //public abstract String toString();

    // --------------------------- Public Methods ---------------------------

    /**
     * Create the route within a Job.
     * @param route the route to create
     * @param cell the cell in which to create the route
     */
    public void createRoute(Route route, Cell cell) {
        new CreateRouteJob(toString(), route, cell, verbose, tool);
    }

    /**
     * Method to create the route.
     * Does not wrap Job around it
     * (useful if already being called from a Job).  This still
     * must be called from within a Job context, however.
     * @param route the route to create
     * @param cell the cell in which to create the route
     * @param verbose if true, prints objects created
     * @param highlightRouteEnd highlights end of route (last object) if true, otherwise leaves
     * highlights alone.
     */
    public static PortInst createRouteNoJob(Route route, Cell cell, boolean verbose,
                                           boolean highlightRouteEnd)
    {
        EDatabase.serverDatabase().checkChanging();

        // check if we can edit this cell
        if (CircuitChangeJobs.cantEdit(cell, null, true) != 0) return null;

        HashMap<ArcProto,Integer> arcsCreatedMap = new HashMap<ArcProto,Integer>();
        HashMap<NodeProto,Integer> nodesCreatedMap = new HashMap<NodeProto,Integer>();

        // pass 1: build all newNodes
        for (RouteElement e : route)
        {
            if (e.getAction() == RouteElement.RouteElementAction.newNode)
            {
                if (e.isDone()) continue;
                e.doAction();
                RouteElementPort rep = (RouteElementPort)e;
                Integer i = nodesCreatedMap.get(rep.getPortProto().getParent());
                if (i == null) i = new Integer(0);
                i = new Integer(i.intValue() + 1);
                nodesCreatedMap.put(rep.getPortProto().getParent(), i);
            }
        }

        // pass 2: do all other actions (deletes, newArcs)
        for (RouteElement e : route)
        {
            e.doAction();
            if (e.getAction() == RouteElement.RouteElementAction.newArc) {
                RouteElementArc rea = (RouteElementArc)e;
                Integer i = arcsCreatedMap.get(rea.getArcProto());
                if (i == null) i = new Integer(0);
                i = new Integer(i.intValue() + 1);
                arcsCreatedMap.put(rea.getArcProto(), i);
            }
        }

        if (verbose)
        {
            List<ArcProto> arcEntries = new ArrayList<ArcProto>(arcsCreatedMap.keySet());
            List<NodeProto> nodeEntries = new ArrayList<NodeProto>(nodesCreatedMap.keySet());
            if (arcEntries.size() == 0 && nodeEntries.size() == 0) {
                System.out.println("Nothing wired");
            } else if (arcEntries.size() == 1 && nodeEntries.size() == 0) {
                ArcProto ap = arcEntries.iterator().next();
                Integer i = arcsCreatedMap.get(ap);
                System.out.println("Wiring added: "+i+" "+ap.describe()+" arcs");
            } else if (arcEntries.size() == 1 && nodeEntries.size() == 1) {
                ArcProto ap = arcEntries.iterator().next();
                NodeProto np = nodeEntries.iterator().next();
                Integer i = arcsCreatedMap.get(ap);
                Integer i2 = nodesCreatedMap.get(np);
                System.out.println("Wiring added: "+i+" "+ap.describe()+" arcs, "+i2+" "+np.describe(true)+" nodes");
            } else {
                System.out.println("Wiring added:");
                Collections.sort(arcEntries, new TextUtils.ObjectsByToString());
                Collections.sort(nodeEntries, new TextUtils.ObjectsByToString());
                for (ArcProto ap : arcEntries) {
                    Integer i = arcsCreatedMap.get(ap);
                    System.out.println("    "+i+" "+ap.describe()+" arcs");
                }
                for (NodeProto np : nodeEntries) {
                    Integer i = nodesCreatedMap.get(np);
                    System.out.println("    "+i+" "+np.describe(true)+" nodes");
                }
            }
			User.playSound();
        }

        PortInst created = null;
        if (highlightRouteEnd) {
            RouteElementPort finalRE = route.getEnd();
            if (finalRE != null)
            	created = finalRE.getPortInst();
        }
        return created;
    }

    /** Method to set the tool associated with this router */
    public void setTool(Tool tool) { this.tool = tool; }

    // -------------------------- Job to build route ------------------------

    /**
     * Job to create the route.
     * Highlights the end of the Route after it creates it.
     */
    protected static class CreateRouteJob extends Job {

        /** route to build */                       protected Route route;
        /** print message on what was done */       private boolean verbose;
        /** cell in which to build route */         private Cell cell;
        /** port to highlight */                    private PortInst portToHighlight;

        /** Constructor */
        protected CreateRouteJob(String what, Route route, Cell cell, boolean verbose, Tool tool) {
            super(what, tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.route = route;
            this.verbose = verbose;
            this.cell = cell;
            startJob();
        }

        public boolean doIt() throws JobException {
            if (CircuitChangeJobs.cantEdit(cell, null, true) != 0) return false;

            portToHighlight = createRouteNoJob(route, cell, verbose, true);
			fieldVariableChanged("portToHighlight");
            return true;
       }

        public void terminateOK()
        {
        	if (portToHighlight != null)
        	{
	            UserInterface ui = Job.getUserInterface();
	            EditWindow_ wnd = ui.getCurrentEditWindow_();
	            if (wnd != null)
	            {
	                wnd.clearHighlighting();
	                wnd.addElectricObject(portToHighlight, cell);
	                wnd.finishedHighlighting();
	            }
        	}
        }
    }


    // ------------------------ Protected Utility Methods ---------------------

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
    public static ArcProto getArcToUse(PortProto port1, PortProto port2) {
        // current user selected arc
        ArcProto curAp = User.getUserTool().getCurrentArcProto();
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
            for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
            {
                ArcProto ap = it.next();
                if (pp1.connectsTo(ap) && ap != uni && ap != invis && ap != unr) return ap;
            }
            // none in current technology: try any technology
            for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
            {
                Technology anyTech = it.next();
                for(Iterator<ArcProto> aIt = anyTech.getArcs(); aIt.hasNext(); )
                {
                    ArcProto ap = aIt.next();
                    if (pp1.connectsTo(ap) && ap != uni && ap != invis && ap != unr) return ap;
                }
            }
        } else {
            // pp2 is not null, include it in search

            // see if current arcproto workds
            if (pp1.connectsTo(curAp) && pp2.connectsTo(curAp)) return curAp;
            // find one that works if current doesn't
            Technology tech = pp1.getParent().getTechnology();
            for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
            {
                ArcProto ap = it.next();
                if (pp1.connectsTo(ap) && pp2.connectsTo(ap) && ap != uni && ap != invis && ap != unr) return ap;
            }
            // none in current technology: try any technology
            for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
            {
                Technology anyTech = it.next();
                for(Iterator<ArcProto> aIt = anyTech.getArcs(); aIt.hasNext(); )
                {
                    ArcProto ap = aIt.next();
                    if (pp1.connectsTo(ap) && pp2.connectsTo(ap) && ap != uni && ap != invis && ap != unr) return ap;
                }
            }
        }
        return null;
    }

    /**
     * Convert all new arcs of type 'ap' in route to use width of widest
     * arc of that type.
     */
    protected static void useWidestWire(Route route, ArcProto ap) {
        // get widest wire connected to anything in route
        double width = getArcWidthToUse(route, ap);
        // set all new arcs of that type to use that width
        for (RouteElement re : route) {
            if (re instanceof RouteElementArc) {
                RouteElementArc reArc = (RouteElementArc)re;
                if (reArc.getArcProto() == ap)
                    reArc.setArcWidth(width);
            }
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
        for (RouteElement re : route) {
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
    public static double getArcWidthToUse(PortInst pi, ArcProto ap) {
        // if pi null, just return default width of ap
        if (pi == null) return ap.getDefaultWidth();

        // get all ArcInsts on pi, find largest
        double width = ap.getDefaultWidth();
        for (Iterator<Connection> it = pi.getConnections(); it.hasNext(); ) {
            Connection c = it.next();
            ArcInst ai = c.getArc();
            if (ai.getProto() != ap) continue;
            double newWidth = c.getArc().getWidth() - c.getArc().getProto().getWidthOffset();
            if (width < newWidth) width = newWidth;
        }
        // check any wires that connect to the export of this portinst in the
        // prototype, if this is a cell instance
        NodeInst ni = pi.getNodeInst();
        if (ni.isCellInstance()) {
            Cell cell = (Cell)ni.getProto();
            Export export = cell.findExport(pi.getPortProto().getName());
            PortInst exportedInst = export.getOriginalPort();
            double width2 = getArcWidthToUse(exportedInst, ap);
            if (width2 > width) width = width2;
        }
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
        double connectedWidth = width;
        if (re instanceof RouteElementPort) {
            RouteElementPort rePort = (RouteElementPort)re;
            connectedWidth = rePort.getWidestConnectingArc(ap);
            if (rePort.getPortInst() != null) {
                // check if prototype connection to export has larger wire
                double width2 = getArcWidthToUse(rePort.getPortInst(), ap);
                if (width2 > connectedWidth) connectedWidth = width2;
            }
        }
        if (re instanceof RouteElementArc) {
            RouteElementArc reArc = (RouteElementArc)re;
            connectedWidth = reArc.getOffsetArcWidth();
        }
        if (width > connectedWidth) return width;
        else return connectedWidth;
    }

    /**
     * Get the dimensions of a contact that will connect between startRE and endRE.
     */
    protected static Dimension2D getContactSize(RouteElement startRE, RouteElement endRE) {
        Dimension2D start = getContactSize(startRE);
        Dimension2D end = getContactSize(endRE);

        // use the largest of the dimensions
        Dimension2D dim = new Dimension2D.Double(start);
        if (end.getWidth() > dim.getWidth()) dim.setSize(end.getWidth(), dim.getHeight());
        if (end.getHeight() > dim.getHeight()) dim.setSize(dim.getWidth(), end.getHeight());

        return dim;
    }

    protected static Dimension2D getContactSize(RouteElement re) {

        double width = -1, height = -1;

        // if RE is an arc, use its arc width
        if (re instanceof RouteElementArc) {
            RouteElementArc reArc = (RouteElementArc)re;
            if (reArc.isArcVertical()) {
                if (reArc.getOffsetArcWidth() > width) width = reArc.getOffsetArcWidth();
            }
            if (reArc.isArcHorizontal()) {
                if (reArc.getOffsetArcWidth() > height) height = reArc.getOffsetArcWidth();
            }
        }

        // special case: if this is a contact cut, use the size of the contact cut
/*
        if (re.getPortProto() != null) {
            if (re.getPortProto().getParent().getFunction() == PrimitiveNode.Function.CONTACT) {
                return re.getNodeSize();
            }
        }
*/

        // if RE is an existingPortInst, use width of arcs connected to it.
        if (re.getAction() == RouteElement.RouteElementAction.existingPortInst) {
            RouteElementPort rePort = (RouteElementPort)re;
            PortInst pi = rePort.getPortInst();
            for (Iterator<Connection> it = pi.getConnections(); it.hasNext(); ) {
                Connection conn = it.next();
                ArcInst arc = conn.getArc();

                Point2D head = arc.getHeadLocation();
                Point2D tail = arc.getTailLocation();

                // use width of widest arc
                double newWidth = arc.getWidth() - arc.getProto().getWidthOffset();
                if (head.getX() == tail.getX()) {
                    if (newWidth > width) width = newWidth;
                }
                if (head.getY() == tail.getY()) {
                    if (newWidth > height) height = newWidth;
                }
            }
        }

        // if RE is a new Node, check it's arcs
        if (re.getAction() == RouteElement.RouteElementAction.newNode) {
            RouteElementPort rePort = (RouteElementPort)re;
            Dimension2D dim = null;
            for (Iterator<RouteElement> it = rePort.getNewArcs(); it.hasNext(); ) {
                RouteElement newArcRE = it.next();
                Dimension2D d = getContactSize(newArcRE);
                if (dim == null) dim = d;

                // use LARGEST of all dimensions
                if (d.getWidth() > dim.getWidth()) dim.setSize(d.getWidth(), dim.getHeight());
                if (d.getHeight() > dim.getHeight()) dim.setSize(dim.getWidth(), d.getHeight());
            }
            if (dim == null) dim = new Dimension2D.Double(-1, -1);
            return dim;
        }

        return new Dimension2D.Double(width, height);
    }
}

package com.sun.electric.tool.routing;

import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.Job;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.technologies.Generic;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.awt.geom.Point2D;

/**
 * User: gainsley
 * Date: Mar 1, 2004
 * Time: 2:48:46 PM
 * <p>
 * Parent Class for all Routers.  I really have no idea what this
 * should look like because I've never written a real router,
 * but I've started it off with a few basics.
 * <p>
 * A Route is a List of RouteElements.  See RouteElement for details
 * of the elements.
 */
public abstract class Router {

    /** set to tell user short info on what was done */     private boolean verbose = true;

    // ------------------ Protected Abstract Router Methods -----------------

    /**
     * Plan a route starting from startRE, and ending at endRE.
     * Note that this method does not add startRE and endRE to the
     * returned list of RouteElements.
     * @param route the list of RouteElements describing route to be modified
     * @param cell the cell in which to create the route
     * @param startRE the RouteElement at the start of the route
     * @param endRE the RouteElement at the end of the route
     * @param hint can be used as a hint to the router for determining route.
     *        Ignored if null
     * @return false on error, route should be ignored.
     */
    protected abstract boolean planRoute(List route, Cell cell, RouteElement startRE, RouteElement endRE, Point2D hint);


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
    public boolean planRoute(List route, Cell cell, PortInst startPort, PortInst endPort, Point2D hint) {
        RouteElement startRE = RouteElement.existingPortInst(startPort);
        RouteElement endRE = RouteElement.existingPortInst(endPort);
        return planRoute(route, cell, startRE, endRE, hint);
    }

    /**
     * Create the route.  If finalRE is not null, set it as highlighted.
     * @param route the route to create
     * @param cell the cell in which to create the route
     * @param finalRE the final RouteElement of the route (i.e. where
     * to continue the route from if extending the route).
     */
    public void createRoute(List route, Cell cell, RouteElement finalRE) {
        CreateRouteJob job = new CreateRouteJob(this, route, cell, finalRE, verbose);
    }

    /** Return a string describing the Router */
    public abstract String toString();


    // -------------------------- Job to build route ------------------------

    /** Job to create the route */
    protected static class CreateRouteJob extends Job {

        /** route to build */                       private List route;
        /** print message on what was done */       private boolean verbose;
        /** final RouteElement in route */          private RouteElement finalRE;
        /** cell in which to build route */         private Cell cell;

        /** Constructor */
        protected CreateRouteJob(Router router, List route, Cell cell, RouteElement finalRE, boolean verbose) {
            super(router.toString(), User.tool, Job.Type.CHANGE, cell, null, Job.Priority.USER);
            this.route = route;
            this.verbose = verbose;
            this.finalRE = finalRE;
            this.cell = cell;
            this.startJob();
        }

        /** Implemented doIt() method to perform Job */
        public void doIt() {
            int arcsCreated = 0;
            // pass 1: build all newNodes
            for (Iterator it = route.iterator(); it.hasNext(); ) {
                RouteElement e = (RouteElement)it.next();
                if (e.getAction() == RouteElement.RouteElementAction.newNode) {
                    e.doAction();
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
                    System.out.println("1 arc created");
                else
                    System.out.println(arcsCreated+" arcs created");
            }
            if (finalRE != null) {
                Highlight.clear();
                PortInst pi = finalRE.getConnectingPort();
                if (pi != null) {
                    Highlight.addElectricObject(pi, cell);
                    Highlight.finished();
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
    protected static ArcProto getArcToUse(PortProto port1, PortProto port2) {
        // current user selected arc
        ArcProto curAp = User.tool.getCurrentArcProto();
        ArcProto uni = Generic.tech.universal_arc;
        ArcProto invis = Generic.tech.invisible_arc;
        ArcProto unr = Generic.tech.unrouted_arc;

        if (!(port1 instanceof PrimitivePort) && !(port2 instanceof PrimitivePort))
            return curAp;                               // non-primitives, use current

        PrimitivePort pp1 = null, pp2 = null;
        // Note: this group of if else junk lets the either of the two
        // port1, port2 args be null, but down below only pp2 can be null.
        if (port1 instanceof PrimitivePort) {
            pp1 = (PrimitivePort)port1;                 // port1 is a primitive port
            if (port2 instanceof PrimitivePort)
                pp2 = (PrimitivePort)port2;             // port2 is also a primitive port
        } else {
            if (port2 instanceof PrimitivePort)
                pp1 = (PrimitivePort)port2;             // port2 is a primitive port,
                                                        // assign it to pp1, leave pp2 null
        }

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

        // get all ArcInsts on pi, find largest of type ap
        double width = 0;
        boolean found = false;
        for (Iterator it = pi.getConnections(); it.hasNext(); ) {
            Connection c = (Connection)it.next();
            if (c.getArc().getProto() == ap) {
                found = true;
                if (width < c.getArc().getWidth()) width = c.getArc().getWidth();
            }
        }
        if (!found) return ap.getDefaultWidth();
        return width;
    }



}

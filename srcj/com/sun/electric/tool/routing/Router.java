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

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
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
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
     * @param arcsCreatedMap a map of arcs to integers which is updated to indicate the number of each arc type created.
     * @param nodesCreatedMap a map of nodes to integers which is updated to indicate the number of each node type created.
     * @return true on error.
     */
    public static boolean createRouteNoJob(Route route, Cell cell, Map<ArcProto,Integer> arcsCreatedMap,
    	Map<NodeProto,Integer> nodesCreatedMap)
    {
        EDatabase.serverDatabase().checkChanging();

        // check if we can edit this cell
        if (CircuitChangeJobs.cantEdit(cell, null, true, false, true) != 0) return true;

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
        	if (e.getAction() == RouteElement.RouteElementAction.newNode) continue;
            ElectricObject result = e.doAction();
            if (e.getAction() == RouteElement.RouteElementAction.newArc) {
            	if (result == null) return true;
                RouteElementArc rea = (RouteElementArc)e;
                Integer i = arcsCreatedMap.get(rea.getArcProto());
                if (i == null) i = new Integer(0);
                i = new Integer(i.intValue() + 1);
                arcsCreatedMap.put(rea.getArcProto(), i);
            }
        }

        if (arcsCreatedMap.get(Generic.tech().unrouted_arc) == null)
        {
            // update current unrouted arcs
        	RouteElementPort rep = route.getStart();
        	if (rep != null && rep.getPortInst() != null)
        	{
        		PortInst pi = rep.getPortInst();
	            for (Iterator<Connection> it = pi.getConnections(); it.hasNext(); )
	            {
	                Connection conn = it.next();
	                ArcInst ai = conn.getArc();
	                if (ai.getProto() == Generic.tech().unrouted_arc)
	                {
	                    Connection oconn = ai.getConnection(1-conn.getEndIndex());
	                    // make new unrouted arc from end of route to arc end point,
	                    // otherwise just get rid of it
	                    if (oconn.getPortInst() != route.getEnd().getPortInst())
	                    {
	                    	RouteElementPort newEnd = RouteElementPort.existingPortInst(oconn.getPortInst(), oconn.getLocation());
	                        RouteElementArc newArc = RouteElementArc.newArc(cell, Generic.tech().unrouted_arc,
                                Generic.tech().unrouted_arc.getDefaultLambdaBaseWidth(), route.getEnd(), newEnd,
                                route.getEnd().getLocation(), oconn.getLocation(), null,
                                ai.getTextDescriptor(ArcInst.ARC_NAME), ai, ai.isHeadExtended(), ai.isTailExtended(), null);
	                        newArc.doAction();
	                    }
	                    if (conn.getArc().isLinked())
	                        conn.getArc().kill();
	                }
	            }
        	}
        }
        return false;
    }

	public static void reportRoutingResults(String prefix, Map<ArcProto,Integer> arcsCreatedMap, Map<NodeProto,Integer> nodesCreatedMap, boolean beep)
	{
		List<ArcProto> arcEntries = new ArrayList<ArcProto>(arcsCreatedMap.keySet());
		List<NodeProto> nodeEntries = new ArrayList<NodeProto>(nodesCreatedMap.keySet());
		if (arcEntries.size() == 0 && nodeEntries.size() == 0)
		{
			System.out.println(prefix + ": nothing added");
		} else
		{
			System.out.print(prefix + " added: ");
			Collections.sort(arcEntries, new TextUtils.ObjectsByToString());
			Collections.sort(nodeEntries, new TextUtils.ObjectsByToString());
			int total = arcEntries.size() + nodeEntries.size();
			int sofar = 0;
			for (ArcProto ap : arcEntries)
			{
				Integer i = arcsCreatedMap.get(ap);
				sofar++;
				if (sofar > 1 && total > 1)
				{
					if (sofar < total) System.out.print(", "); else
						System.out.print(" and ");
				}
				System.out.print(i.intValue() + " " + ap.describe());
				if (i.intValue() > 1) System.out.print(" arcs"); else
					System.out.print(" arc");
			}
			for (NodeProto np : nodeEntries)
			{
				Integer i = nodesCreatedMap.get(np);
				sofar++;
				if (sofar > 1 && total > 1)
				{
					if (sofar < total) System.out.print(", "); else
						System.out.print(" and ");
				}
				System.out.print(i.intValue() + " " + np.describe(false));
				if (i.intValue() > 1) System.out.print(" nodes"); else
					System.out.print(" node");
			}
			System.out.println();
            if (beep)
                Job.getUserInterface().beep();
//			User.playSound();
		}
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
        /** true to beep */                         private boolean beep;

        /** Constructor */
        protected CreateRouteJob(String what, Route route, Cell cell, boolean verbose, Tool tool) {
            super(what, tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.route = route;
            this.verbose = verbose;
            this.cell = cell;
            beep = User.isPlayClickSoundsWhenCreatingArcs();
            startJob();
        }

        public boolean doIt() throws JobException {
            if (CircuitChangeJobs.cantEdit(cell, null, true, false, true) != 0) return false;

            Map<ArcProto,Integer> arcsCreatedMap = new HashMap<ArcProto,Integer>();
            Map<NodeProto,Integer> nodesCreatedMap = new HashMap<NodeProto,Integer>();
            createRouteNoJob(route, cell, arcsCreatedMap, nodesCreatedMap);
            portToHighlight = null;
            RouteElementPort finalRE = route.getEnd();
            if (finalRE != null)
            	portToHighlight = finalRE.getPortInst();
            reportRoutingResults("Wiring", arcsCreatedMap, nodesCreatedMap, beep);
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

        // if connecting two busses, force a bus arc
		if (curAp == Schematics.tech().wire_arc && port1 != null && port2 != null)
		{
			boolean bus1 = (port1.getParent() == Schematics.tech().busPinNode) || port1.getNameKey().isBus();
			boolean bus2 = (port2.getParent() == Schematics.tech().busPinNode) || port2.getNameKey().isBus();
			if (bus1 && bus2) return Schematics.tech().bus_arc;
		}

        PortProto pp1 = null, pp2 = null;
        // Note: this makes it so either port1 or port2 can be null,
        // but only pp2 can be null down below
        if (port1 == null) pp1 = port2; else
        	{ pp1 = port1; pp2 = port2; }
        if (pp1 == null && pp2 == null) return null;

        // see if current arcproto works
        if (pp2 == null)
        {
            if (pp1.connectsTo(curAp)) return curAp;
        } else
        {
            if (pp1.connectsTo(curAp) && pp2.connectsTo(curAp)) return curAp;
        }

        // otherwise, find one that does in the current technology
        Technology tech = pp1.getParent().getTechnology();
    	ArcProto ap = findArcThatConnects(tech, pp1, pp2);
    	if (ap != null) return ap;

        // none in current technology: try any technology but generic
        for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
        {
            Technology anyTech = it.next();
            if (anyTech == tech || anyTech == Generic.tech()) continue;
            ap = findArcThatConnects(anyTech, pp1, pp2);
            if (ap != null) return ap;
        }

        return null;
    }

    private static ArcProto findArcThatConnects(Technology tech, PortProto pp1, PortProto pp2)
    {
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
		    ArcProto ap = it.next();
		    if (pp1.connectsTo(ap))
		    {
		    	if (pp2 == null || pp2.connectsTo(ap))
		    		return ap;
		    }
		}
		return null;
    }

    /**
     * Convert all new arcs of type 'ap' in route to use width of widest
     * arc of that type.
     */
/*
    protected static void useWidestWire(Route route, ArcProto ap) {
        // get widest wire connected to anything in route
        double width = getArcWidthToUse(route, ap);
        // set all new arcs of that type to use that width
        for (RouteElement re : route) {
            if (re instanceof RouteElementArc) {
                RouteElementArc reArc = (RouteElementArc)re;
                if (reArc.getArcProto() == ap)
                    reArc.setArcBaseWidth(width);
            }
        }
    }
*/

    /**
     * Get arc width to use by searching for largest arc of passed type
     * connected to any elements in the route.
     * @param route the route to be searched
     * @param ap the arc type
     * @return the largest width
     */
/*
    protected static double getArcWidthToUse(Route route, ArcProto ap) {
        double widest = ap.getDefaultLambdaBaseWidth();
        for (RouteElement re : route) {
            double width = getArcWidthToUse(re, ap, 0);
            if (width > widest) widest = width;
        }
        return widest;
    }
*/

    /**
     * Get arc width to use to connect to PortInst pi.  Arc type
     * is ap.  Uses the largest width of arc type ap already connected
     * to pi, or the default width of ap if none found.<p>
     * You may specify pi as null, in which case it just returns
     * ap.getDefaultLambdaFullWidth().
     * <P>
     * If ignoreAngle is false, only arcs whose angles match arcAngle
     * will have their sizes used to determine the arc width.  180 degrees
     * out of phase also matches in this case.
     * <P>
     * If ignoreAngle is true, any arcs will have their sizes used to
     * determine the return arc size.
     *
     * @param obj the object to connect to, either a PortInst or an ArcInst
     * @param ap the Arc type to connect with
     * @param arcAngle of the arc that will be drawn (in tenth-degrees)
     * @param ignoreAngle to ignore the angle of arc to be drawn and existing arcs
     * @param fatWires true to make arcs as wide as connecting nodes.
     * @return the width to use to connect
     */
    public static double getArcWidthToUse(ElectricObject obj, ArcProto ap, int arcAngle, boolean ignoreAngle, boolean fatWires) {

        if (obj instanceof ArcInst) {
            ArcInst ai = (ArcInst)obj;
            if (ignoreAngle) return ai.getLambdaBaseWidth();
            if (ai.getAngle() % 1800 == arcAngle) return ai.getLambdaBaseWidth();
            return ap.getDefaultLambdaBaseWidth();
        }
        if (obj == null || !(obj instanceof PortInst)) return ap.getDefaultLambdaBaseWidth();
        PortInst pi = (PortInst)obj;

        boolean arcFound = false;
        // get all ArcInsts on pi, find largest
        double width = ap.getDefaultLambdaBaseWidth();
        for (Iterator<Connection> it = pi.getConnections(); it.hasNext(); ) {
            Connection c = it.next();
            ArcInst ai = c.getArc();
            if (ai.getProto() != ap) continue;
            if (!ignoreAngle) {
                if (ai.getAngle() % 1800 != arcAngle % 1800) continue;
            }
            double newWidth = c.getArc().getLambdaBaseWidth();
            if (width < newWidth) width = newWidth;
            arcFound = true;
        }
        if (arcFound) return width;

        NodeInst ni = pi.getNodeInst();
        // if still default width and node is a contact, use the width/height of the contact
        if (!arcFound && (ni.getProto() instanceof PrimitiveNode)) {
            PrimitiveNode pn = (PrimitiveNode)ni.getProto();
            if (fatWires) {
                if (pn.getFunction().isContact()) {
                    // size calls take into account rotation
                    double xsize = ni.getXSizeWithoutOffset();
                    double ysize = ni.getYSizeWithoutOffset();

                    // look for actual size of layer on contact
                    Iterator<Poly> pit = ni.getShape(Poly.newLambdaBuilder());
                    while (pit.hasNext()) {
                        Poly poly = pit.next();
                        if (poly.getLayer() == ap.getLayer(0)) {
                            xsize = poly.getBounds2D().getWidth();
                            ysize = poly.getBounds2D().getHeight();
                        }
                    }
                    
                    if (arcAngle % 1800 == 0) {
                        width = ysize;
                    }
                    if ((arcAngle - 900) % 1800 == 0) {
                        width = xsize;
                    }
                }
            }
            if (pn.getFunction().isPin()) {
                for (Iterator<Connection> it = pi.getConnections(); it.hasNext(); ) {
                    Connection c = it.next();
                    ArcInst ai = c.getArc();
                    if (ai.getProto() != ap) continue;
                    double newWidth = c.getArc().getLambdaBaseWidth();
                    if (width < newWidth) width = newWidth;
                }
            }
        }

        // check any wires that connect to the export of this portinst in the
        // prototype, if this is a cell instance
        if (ni.isCellInstance()) {
            Cell cell = (Cell)ni.getProto();
            Export export = cell.findExport(pi.getPortProto().getName());
            PortInst exportedInst = export.getOriginalPort();
            double width2 = getArcWidthToUse(exportedInst, ap, arcAngle, ignoreAngle, fatWires);
            if (width2 > width) width = width2;
        }

        return width;
    }

    /**
     * Get arc width to use to connect to RouteElement re. Uses largest
     * width of arc already connected to re.
     * @param re the RouteElement to connect to
     * @param ap the arc type (for default width)
     * @param arcAngle of the arc that will be drawn (in tenth-degrees)
     * @return the width of the arc to use to connect
     */
/*    protected static double getArcWidthToUse(RouteElement re, ArcProto ap, int arcAngle) {
        double width = ap.getDefaultLambdaBaseWidth();
        double connectedWidth = width;
        if (re instanceof RouteElementPort) {
            RouteElementPort rePort = (RouteElementPort)re;
            connectedWidth = rePort.getWidestConnectingArc(ap);
            if (rePort.getPortInst() != null) {
                // check if prototype connection to export has larger wire
                double width2 = getArcWidthToUse(rePort.getPortInst(), ap, arcAngle);
                if (width2 > connectedWidth) connectedWidth = width2;
            }
        }
        if (re instanceof RouteElementArc) {
            RouteElementArc reArc = (RouteElementArc)re;
            connectedWidth = reArc.getArcBaseWidth();
        }
        if (width > connectedWidth) return width;
        return connectedWidth;
    }
*/

    /**
     * ContactSize class to deterime the arc sizes and contact size between two
     * objects to be connected by the wirer.  This assumes manhatten wiring.
     */
    protected static class ContactSize {
        private Rectangle2D contactSize;
        private int startAngle;
        private int endAngle;
        private double startArcWidth;
        private double endArcWidth;

        public Rectangle2D getContactSize() { return contactSize; }
        public int getStartAngle() { return startAngle; }
        public int getEndAngle() { return endAngle; }
        public double getStartWidth() { return startArcWidth; }
        public double getEndWidth() { return endArcWidth; }

        /**
         * Determine the contact size, arc sizes, and arc angles based on the
         * ElectricObjects to be connected, and the start, end, and corner location.
         * @param startObj the object to route from
         * @param endObj the object to route to
         * @param startLoc the start location of the start arc
         * @param endLoc the end location of the end arc
         * @param cornerLoc the corner location (end of start arc and start of end arc)
         * @param startArc start arc type
         * @param endArc end arc type
         * @param ignoreAngles whether to ignore angles when determining sizes
         * @param fatWires true to make arcs as wide as connecting nodes.
         */
        public ContactSize(ElectricObject startObj, ElectricObject endObj, Point2D startLoc, Point2D endLoc,
                           Point2D cornerLoc, ArcProto startArc, ArcProto endArc, boolean ignoreAngles, boolean fatWires) {

            Dimension2D startDim = new Dimension2D.Double(0, 0);
            Dimension2D endDim = new Dimension2D.Double(0, 0);
            Dimension2D startPref = new Dimension2D.Double(0, 0);
            Dimension2D endPref = new Dimension2D.Double(0, 0);
            startAngle = getAngleAndDimension(startDim, startObj, startLoc, cornerLoc, startArc, endObj == null, ignoreAngles, fatWires, startPref);
            endAngle = getAngleAndDimension(endDim, endObj, endLoc, cornerLoc, endArc, startObj == null, ignoreAngles, fatWires, endPref);

            double startW = startDim.getWidth();
            double startH = startDim.getHeight();
            double endW = endDim.getWidth();
            double endH = endDim.getHeight();
            if (startW == 0) startW = endW;
            if (startH == 0) startH = endH;
            if (endW == 0) endW = startW;
            if (endH == 0) endH = startH;

            // put dims in start, prefer arc widths
            if (endPref.getWidth() > startPref.getWidth()) {
                startW = endW;
            } else if (endPref.getWidth() == startPref.getWidth()) {
                if (endW < startW) startW = endW;
            }
            if (endPref.getHeight() > startPref.getHeight()) {
                startH = endH;
            } else if (endPref.getHeight() == startPref.getHeight()) {
                if (endH < startH) startH = endH;
            }

            if (endObj == null && fatWires) {
                if (startW > startH) startH = startW;
                if (startH > startW) startW = startH;
            }

            contactSize = new Rectangle2D.Double(cornerLoc.getX()-startW/2.0, cornerLoc.getY()-startH/2.0, startW, startH);

            if (startAngle % 1800 == 0) startArcWidth = contactSize.getHeight();
            else if ((startAngle + 900) % 1800 == 0) startArcWidth = contactSize.getWidth();
            else startArcWidth = contactSize.getHeight(); // all non-manhatten angles
            if (endAngle % 1800 == 0) endArcWidth = contactSize.getHeight();
            else if ((endAngle + 900) % 1800 == 0) endArcWidth = contactSize.getWidth();
            else endArcWidth = contactSize.getWidth(); // all non-manhatten angles

        }

        private static int getAngleAndDimension(Dimension2D dim, ElectricObject obj, Point2D loc, Point2D cornerLoc,
                                                ArcProto arc, boolean otherObjNull, boolean ignoreAngles,
                                                boolean fatWires, Dimension2D pref) {
            double w = 0, h = 0;
            int angle = 0;

            if (obj instanceof ArcInst) {
                ArcInst ai = (ArcInst)obj;
                angle = ai.getAngle();
                double size = ai.getLambdaBaseWidth();

                if (loc.equals(cornerLoc)) {
                    if (angle % 1800 == 0) h = ai.getLambdaBaseWidth();
                    if ((angle + 900) % 1800 == 0) w = ai.getLambdaBaseWidth();
                } else {
                    angle = GenMath.figureAngle(loc, cornerLoc);
                    if (angle % 1800 == 0) h = ai.getLambdaBaseWidth();
                    if ((angle + 900) % 1800 == 0) w = ai.getLambdaBaseWidth();
                }

                if (angle % 1800 == 0) pref.setSize(0, 1);
                if ((angle + 900) % 1800 == 0) pref.setSize(1, 0);

                // special case
                if (otherObjNull) {
                    w = h = size;
                    pref.setSize(1, 1);
                }
            }

            if (obj instanceof PortInst) {
                PortInst pi = (PortInst)obj;
                if (otherObjNull) ignoreAngles = true;

                if (loc.equals(cornerLoc)) {
                    w = getArcWidthToUse(pi, arc, 900, ignoreAngles, fatWires);
                    h = getArcWidthToUse(pi, arc, 0, ignoreAngles, fatWires);
                } else {
                    angle = GenMath.figureAngle(loc, cornerLoc);
                    if (angle % 1800 == 0) h = getArcWidthToUse(pi, arc, angle, ignoreAngles, fatWires);
                    else if ((angle + 900) % 1800 == 0) w = getArcWidthToUse(pi, arc, angle, ignoreAngles, fatWires);
                    else h = w = getArcWidthToUse(pi, arc, angle, true, fatWires);
                }
            }

            dim.setSize(w, h);
            return angle;
        }

    }


    /**
     * Get the dimensions of a contact that will connect between startRE and endRE.
     */
/*
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
            int angle = reArc.getArcAngle();
            if (angle == 900 || angle == 2700) {    // vertical
                if (reArc.getArcBaseWidth() > width) width = reArc.getArcBaseWidth();
            }
            if (angle == 0 || angle == 1800 || angle == 3600) {  // horizontal
                if (reArc.getArcBaseWidth() > height) height = reArc.getArcBaseWidth();
            }
        }

        // special case: if this is a contact cut, use the size of the contact cut
*/
/*
        if (re.getPortProto() != null) {
            if (re.getPortProto().getParent().getFunction() == PrimitiveNode.Function.CONTACT) {
                return re.getNodeSize();
            }
        }
*/
/*

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
                double newWidth = arc.getLambdaBaseWidth();
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
*/

    //=====================================================================================
/*

    public static class ArcWidth {
        private int preferredAngle;
        private double preferredWidth;
        private double defaultWidth;
        public ArcWidth(int preferredAngle) {
            this.preferredAngle = preferredAngle % 1800;
            this.preferredWidth = -1;
            this.defaultWidth = -1;
        }

        public double getPreferredWidth() { return preferredWidth; }
        public double getDefaultWidth() { return defaultWidth; }
        public double getWidth() {
            if (preferredWidth > 0) return preferredWidth;
            return defaultWidth;
        }

        */
/**
         * Get arc width to use by searching for largest arc of passed type
         * connected to any elements in the route.
         * @param route the route to be searched
         * @param ap the arc type
         */
/*
        public void findArcWidthToUse(Route route, ArcProto ap) {
            double basewidth = ap.getDefaultLambdaBaseWidth();
            if (basewidth > defaultWidth) defaultWidth = basewidth;
            for (RouteElement re : route) {
                findArcWidthToUse(re, ap);
            }
        }

        public void findArcWidthToUse(ElectricObject routeObj, ArcProto ap) {
            double width = -1;
            if (routeObj instanceof ArcInst) {
                ArcInst ai = (ArcInst)routeObj;
                if (ai.getProto() == ap) {
                    width = ai.getLambdaBaseWidth();
                    if (width > defaultWidth) defaultWidth = width;
                    if (width > preferredWidth) preferredWidth = width;
                }
            }
            if (routeObj instanceof PortInst) {
                findArcWidthToUse((PortInst)routeObj, ap);
            }
        }

        */
/**
         * Get arc width to use to connect to PortInst pi.  Arc type
         * is ap.  Uses the largest width of arc type ap already connected
         * to pi, or the default width of ap if none found.<p>
         * You may specify pi as null, in which case it just returns
         * ap.getDefaultLambdaFullWidth().
         * @param pi the PortInst to connect to
         * @param ap the Arc type to connect with
         */
/*
        public void findArcWidthToUse(PortInst pi, ArcProto ap) {
            // if pi null, just return default width of ap
            if (pi == null) return;

            double basewidth = ap.getDefaultLambdaBaseWidth();
            if (basewidth > defaultWidth) defaultWidth = basewidth;

            // get all ArcInsts on pi, find largest
            for (Iterator<Connection> it = pi.getConnections(); it.hasNext(); ) {
                Connection c = it.next();
                ArcInst ai = c.getArc();
                if (ai.getProto() != ap) continue;
                double newWidth = c.getArc().getLambdaBaseWidth();
                int aiAngle = ai.getAngle() % 1800;
                if (defaultWidth < newWidth) defaultWidth = newWidth;
                if (preferredWidth < newWidth && aiAngle == preferredAngle)
                    preferredWidth = newWidth;
            }

            // check any wires that connect to the export of this portinst in the
            // prototype, if this is a cell instance
            NodeInst ni = pi.getNodeInst();
            if (ni.isCellInstance()) {
                Cell cell = (Cell)ni.getProto();
                Export export = cell.findExport(pi.getPortProto().getName());
                PortInst exportedInst = export.getOriginalPort();
                findArcWidthToUse(exportedInst, ap);
            }

            // if this is a contact cut that can connect to the arc and no other
            // arcs have been found, use the width/height of it.
*/
/*
            if ((ni.getProto() instanceof PrimitiveNode) && preferredAngle >= 0)
            {
                PrimitiveNode pn = (PrimitiveNode)ni.getProto();
                if (pn.getFunction() == PrimitiveNode.Function.CONTACT && pi.getPortProto().connectsTo(ap))
                {
                    double width = 0;
                    if (preferredAngle == 0)
                    {
                    	double ySize = ni.getLambdaBaseYSize();
                    	double ySizeBase = pn.getDefaultLambdaBaseHeight();
                    	width = basewidth + ySize - ySizeBase;
                    }
                    if (preferredAngle == 900)
                    {
                    	double xSize = ni.getLambdaBaseXSize();
                    	double xSizeBase = pn.getDefaultLambdaBaseWidth();
                    	width = basewidth + xSize - xSizeBase;
                    }

                    if (width > defaultWidth) defaultWidth = width;
                    if (width > preferredWidth) preferredWidth = width;
                }
            }
*/
/*
        }

        */
/**
         * Get arc width to use to connect to RouteElement re. Uses largest
         * width of arc already connected to re.
         * @param re the RouteElement to connect to
         * @param ap the arc type (for default width)
         */
/*
        public void findArcWidthToUse(RouteElement re, ArcProto ap) {
            double basewidth = ap.getDefaultLambdaBaseWidth();
            if (basewidth > defaultWidth) defaultWidth = basewidth;

            double connectedWidth = -1;
            if (re instanceof RouteElementPort) {
                RouteElementPort rePort = (RouteElementPort)re;
                findArcWidthToUse(rePort, ap);
                if (rePort.getPortInst() != null) {
                    // check if prototype connection to export has larger wire
                    findArcWidthToUse(rePort.getPortInst(), ap);
                }
            }
            if (re instanceof RouteElementArc) {
                RouteElementArc reArc = (RouteElementArc)re;
                connectedWidth = reArc.getArcBaseWidth();
                int arcAngle = reArc.getArcAngle() % 1800;
                if (connectedWidth > defaultWidth) defaultWidth = connectedWidth;
                if (connectedWidth > preferredWidth && arcAngle == preferredAngle)
                    preferredWidth = connectedWidth;
            }
        }

        */
/**
         * Get largest arc width of newArc RouteElements attached to this
         * RouteElement.  If none present returns -1.
         * <p>Note that these width values should have been pre-adjusted for
         * the arc width offset, so these values have had the offset subtracted away.
         */
/*
        public void findArcWidthToUse(RouteElementPort re, ArcProto ap) {
            double basewidth = ap.getDefaultLambdaBaseWidth();
            if (basewidth > defaultWidth) defaultWidth = basewidth;

            if (re.getAction() == RouteElement.RouteElementAction.existingPortInst) {
                // find all arcs of type ap connected to this
                findArcWidthToUse(re.getPortInst(), ap);
            }

            if (re.getAction() == RouteElement.RouteElementAction.newNode) {
                for (Iterator<RouteElement> it = re.getNewArcs(); it.hasNext(); ) {
                    RouteElementArc rearc = (RouteElementArc)it.next();
                    if (rearc.getArcProto() == ap) {
                        double newWidth = rearc.getArcBaseWidth();
                        int connAngle = rearc.getArcAngle() % 1800;
                        if (newWidth > preferredWidth && preferredAngle == connAngle) preferredWidth = newWidth;
                        if (newWidth > defaultWidth) defaultWidth = newWidth;
                    }
                }
            }
        }
    }
*/

}

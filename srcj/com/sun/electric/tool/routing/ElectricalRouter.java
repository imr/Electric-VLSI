/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ElectricalRouter.java
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

import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitivePort;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Jul 20, 2004
 * Time: 5:12:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class ElectricalRouter {

    /** Possible arcs to start from */          private ArcProto [] startArcs;
    /** Possible arcs to end on */              private ArcProto [] endArcs;
    /** current start arc */                    private ArcProto startArc;
    /** current end arc */                      private ArcProto endArc;
    /** current start port (ignored in search) */ private PortProto startPort;
    /** current end port (ignored in search) */ private PortProto endPort;
    /** best electrical route so far */         private SpecifiedRoute specifiedRoute;
    /** list of all valid specified routes */   private List allSpecifiedRoutes;

    private static class SpecifiedRoute extends ArrayList {
        ArcProto startArc;
        ArcProto endArc;

        void printRoute() {
            for (int k=0; k<size(); k++) {
                System.out.println("   "+k+": "+get(k));
            }
        }
    }

    private int searchNumber;
    private static final int SEARCHLIMIT = 100;
    private static final boolean DEBUG = false;
    private static final boolean DEBUGSEARCH = false;

    private ElectricalRouter(ArcProto [] startArcs, ArcProto [] endArcs, PortProto startPort,
                             PortProto endPort) {
        this.startArcs = startArcs;
        this.endArcs = endArcs;
        this.startPort = startPort;
        this.endPort = endPort;
        specifiedRoute = null;
    }

    // ------------------------ Planning Methods -------------------------

    /**
     * Create an electrical route from startPort to endPort.
     * @param startPort the port to start from
     * @param endPort the port to end on
     * @return an electrical route specification
     */
    public static ElecRoute planRoute(PortProto startPort, PortProto endPort) {
        ArcProto [] startArcs = startPort.getBasePort().getConnections();
        ArcProto [] endArcs = endPort.getBasePort().getConnections();

        ElectricalRouter router = new ElectricalRouter(startArcs, endArcs, startPort, endPort);
        return router.planRoute();
    }

    /**
     * Create an electrical route from startPort to endArc.
     * @param startPort the port to start from
     * @param endArc the arc to end with
     * @return an electrical route specification, does not include endArc
     */
    public static ElecRoute planRoute(PortProto startPort, ArcProto endArc) {
        ArcProto [] startArcs = startPort.getBasePort().getConnections();
        ArcProto [] endArcs = {endArc};

        ElectricalRouter router = new ElectricalRouter(startArcs, endArcs, startPort, null);
        return router.planRoute();
    }

    /**
     * Create an electrical route from startPort to endArc.
     * @param startArc the arc to start from
     * @param endArc the arc to end with
     * @return an electrical route specification, does not include either
     * startArc or endArc.
     */
    public static ElecRoute planRoute(ArcProto startArc, ArcProto endArc) {
        ArcProto [] startArcs  = {startArc};
        ArcProto [] endArcs = {endArc};

        ElectricalRouter router = new ElectricalRouter(startArcs, endArcs, null, null);
        return router.planRoute();
    }

    /**
     * The main entry method to create an electrical route
     * @return the electrical route
     */
    private ElecRoute planRoute() {
        // null out bad arcs
        startArcs = nullBadArcs(startArcs);
        endArcs = nullBadArcs(endArcs);

        if (endArcs == null || startArcs == null) {
            System.out.println("VerticalRoute: invalid start or end point");
            return null;
        }
        if (specifyRoute(startArcs, endArcs)) {
            // convert specified route to an ElecRoute

        }
        return null;
    }

    /** Get rid of unrouteable arcs */
    private static ArcProto [] nullBadArcs(ArcProto [] arcs) {
        // null out bad ars
        for (int i=0; i<arcs.length; i++) {
            if (arcs[i] == Generic.tech.universal_arc) arcs[i] = null;
            if (arcs[i] == Generic.tech.invisible_arc) arcs[i] = null;
            if (arcs[i] == Generic.tech.unrouted_arc) arcs[i] = null;
            if ((arcs[i] != null) && (arcs[i].isNotUsed())) arcs[i] = null;
        }
        return arcs;
    }

    /**
     * Specify the route
     */
    private boolean specifyRoute(ArcProto [] startArcs, ArcProto [] endArcs) {

        specifiedRoute = new SpecifiedRoute();
        allSpecifiedRoutes = new ArrayList();
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
                if (DEBUGSEARCH) System.out.println("** Start search startArc="+startArc+", endArc="+endArc);
                findConnectingPorts(startArc, endArc, "");
            }
        }

        if (allSpecifiedRoutes.size() == 0) return false;           // nothing found

        // choose shortest route
        specifiedRoute = (SpecifiedRoute)allSpecifiedRoutes.get(0);
        for (int i=0; i<allSpecifiedRoutes.size(); i++) {
            SpecifiedRoute r = (SpecifiedRoute)allSpecifiedRoutes.get(i);
            if (r.size() < specifiedRoute.size()) specifiedRoute = r;
        }
        allSpecifiedRoutes.clear();
        startArc = specifiedRoute.startArc;
        endArc = specifiedRoute.endArc;
        if (DEBUGSEARCH) {
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
    private void findConnectingPorts(ArcProto startArc, ArcProto endArc, String ds) {

        if (startArc == endArc) {
            saveRoute(specifiedRoute);
            return;    // don't need anything to connect between them
        }

        ds += "  ";
        if (searchNumber > SEARCHLIMIT) return;
        searchNumber++;
        Technology tech = startArc.getTechnology();

        // see if we can find a port in the current technology
        // that will connect the two arcs
        for (Iterator portsIt = tech.getPorts(); portsIt.hasNext(); ) {
            PrimitivePort pp = (PrimitivePort)portsIt.next();
            // ignore anything whose parent is not a CONTACT
            if (pp.getParent().getFunction() != NodeProto.Function.CONTACT) continue;
            if (DEBUGSEARCH) System.out.println(ds+"Checking if "+pp+" connects between "+startArc+" and "+endArc);
            if (pp.connectsTo(startArc) && pp.connectsTo(endArc)) {
                specifiedRoute.add(pp);
                saveRoute(specifiedRoute);
                return;                                // this connects between both arcs
            }
        }

        // try all contact ports as an intermediate
        for (Iterator portsIt = tech.getPorts(); portsIt.hasNext(); ) {
            PrimitivePort pp = (PrimitivePort)portsIt.next();
            // ignore anything whose parent is not a CONTACT
            if (pp.getParent().getFunction() != NodeProto.Function.CONTACT) continue;
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
        SpecifiedRoute loggedRoute = new SpecifiedRoute();
        loggedRoute.startArc = route.startArc;
        loggedRoute.endArc = route.endArc;
        loggedRoute.addAll(route);
        allSpecifiedRoutes.add(loggedRoute);
    }

}

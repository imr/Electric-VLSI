/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetworkHighlighter.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.technology.PrimitiveNode;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.geom.AffineTransform;
import java.awt.*;

/**
 * This class is used for hierarchical highlighting of networks
 */
public class NetworkHighlighter extends HierarchyEnumerator.Visitor {

    private Cell cell;
    private Netlist netlist;
    private Network net;
    private int netID;
    private int startDepth;
    private int endDepth;
    private int currentDepth;
    private Highlighter highlighter;

    private NetworkHighlighter(Cell cell, Netlist netlist, Network net, int startDepth, int endDepth) {
        this.cell = cell;
        this.netlist = netlist;
        this.net = net;
        this.startDepth = startDepth;
        this.endDepth = endDepth;
        currentDepth = 0;
        highlighter = new Highlighter(Highlighter.SELECT_HIGHLIGHTER, null);
    }

    /**
     * Returns a list of Highlight objects that draw lines and boxes over
     * instances that denote the location of objects in that instance that
     * are connected to net. The depth of the search is specified by startDepth
     * and endDepth, which start at 0. If both are set to zero, only objects in the
     * cell Cell are highlighted.
     * @param cell the cell in which to highlight objects
     * @param netlist the netlist for the cell
     * @param net objects connected to this network will be highlighted
     * @param startDepth to start depth of the hierarchical search
     * @return endDepth the end depth of the hierarchical search
     */
    public static synchronized List getHighlights(Cell cell, Netlist netlist, Network net, int startDepth, int endDepth) {
        NetworkHighlighter networkHighlighter = new NetworkHighlighter(cell, netlist, net, startDepth, endDepth);

        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, netlist, networkHighlighter);

        return networkHighlighter.highlighter.getHighlights();
    }



    public boolean enterCell(HierarchyEnumerator.CellInfo info) {
        if (currentDepth == 0) {
            // set the global net ID that will be used in sub cells
            netID = info.getNetID(net);
        }

        // do not go below depth 0 for schematics
        if (cell.isSchematic()) {
            if (currentDepth > 0) return false;
        }

        if (currentDepth >= startDepth) {
            if (currentDepth > endDepth) return false;      // stop here. No more traversal down.
            // highlight connected in current cell. Highlight actual objects if top level
            if (currentDepth == 0) {
                addNetworkObjects();
            } else {
                addNetworkPolys(info);
            }
        }

        // increment depth and continue
        currentDepth++;
        return true;
    }

    public void exitCell(HierarchyEnumerator.CellInfo info) {
        currentDepth--;
    }

    public boolean visitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {
        return true;
    }


    /**
     * Get objects connected to network 'net' in Cell 'cell' with Netlist 'netlist.
     * This will grab all arcs on net if 'arcs' is not null, it will grab all
     * ports if both 'arcs' and 'ports' are not null, and will grab all
     * exports if 'exports' is not null.
     * @param cell
     * @param netlist
     * @param net
     * @param arcs list of ArcInsts on network
     * @param ports list of PortsInsts on network
     * @param exports list of Exports on network
     */
    private static void getNetworkObjects(Cell cell, Netlist netlist, Network net,
                                       List arcs, List ports, List exports) {
        if (arcs != null) {
            List nodesAdded = new ArrayList();

            // all arcs on the network
            for(Iterator aIt = cell.getArcs(); aIt.hasNext(); )
            {
                ArcInst ai = (ArcInst)aIt.next();
                int width = netlist.getBusWidth(ai);
                for(int i=0; i<width; i++)
                {
                    Network oNet = netlist.getNetwork(ai, i);
                    if (oNet == net)
                    {
                        arcs.add(ai);
                        if (ports != null) {
                            // also highlight end nodes of arc, if they are primitive nodes
                            PortInst pi = ai.getHead().getPortInst();
                            if (pi.getNodeInst().getProto() instanceof PrimitiveNode) {
                                // ignore pins
                                if (pi.getNodeInst().getProto().getFunction() != PrimitiveNode.Function.PIN &&
                                    !nodesAdded.contains(pi)) {
                                    // prevent duplicates
                                    ports.add(pi);
                                    nodesAdded.add(pi);
                                }
                            }
                            pi = ai.getTail().getPortInst();
                            if (pi.getNodeInst().getProto() instanceof PrimitiveNode) {
                                if (pi.getNodeInst().getProto().getFunction() != PrimitiveNode.Function.PIN &&
                                    !nodesAdded.contains(pi)) {
                                    // prevent duplicates
                                    ports.add(pi);
                                    nodesAdded.add(pi);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (exports != null) {
            // show all exports on the network
            for(Iterator pIt = cell.getPorts(); pIt.hasNext(); )
            {
                Export pp = (Export)pIt.next();
                int width = netlist.getBusWidth(pp);
                for(int i=0; i<width; i++)
                {
                    Network oNet = netlist.getNetwork(pp, i);
                    if (oNet == net)
                    {
                        exports.add(pp);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Add all network objects (arcs, primitive nodes) to highlighter for current cell
     */
    private void addNetworkObjects() {
        // highlight objects in this cell
        List arcs = new ArrayList();
        List ports = new ArrayList();
        List exports = new ArrayList();

        getNetworkObjects(cell, netlist, net, arcs, ports, exports);

        for (Iterator it = arcs.iterator(); it.hasNext(); ) {
            ArcInst ai = (ArcInst)it.next();
            highlighter.addElectricObject(ai, cell, false);
        }
        for (Iterator it = ports.iterator(); it.hasNext(); ) {
            PortInst pi = (PortInst)it.next();
            highlighter.addElectricObject(pi, cell, false);
        }
        for (Iterator it = exports.iterator(); it.hasNext(); ) {
            Export ep = (Export)it.next();
            highlighter.addElectricObject(ep, cell, false);
        }
    }

    /**
     * Add all network objects (arcs, primitive nodes) as poly
     * objects, transformed by trans.
     */
    private void addNetworkPolys(HierarchyEnumerator.CellInfo info) {
        Netlist netlist = info.getNetlist();

        // find network in this cell that corresponds to global id
        Network localNet = null;
        for (Iterator it = netlist.getNetworks(); it.hasNext(); ) {
            Network aNet = (Network)it.next();
            if (info.getNetID(aNet) == netID) {
                localNet = aNet;
                break;
            }
        }

        // no local net that matches with global network
        if (localNet == null) return;

        Cell currentCell = info.getCell();
        List arcs = new ArrayList();
        List ports = new ArrayList();
        List exports = new ArrayList();
        getNetworkObjects(currentCell, netlist, localNet, arcs, ports, exports);

        // get polys for each object and transform them to root
        AffineTransform trans = info.getTransformToRoot();
        for (Iterator it = arcs.iterator(); it.hasNext(); ) {
            ArcInst ai = (ArcInst)it.next();
            Poly poly = ai.makePoly(ai.getLength(), ai.getWidth() - ai.getProto().getWidthOffset(), Poly.Type.CLOSED);
            poly.transform(trans);
            highlighter.addPoly(poly, cell, null);
        }
        for (Iterator it = ports.iterator(); it.hasNext(); ) {
            PortInst pi = (PortInst)it.next();
            NodeInst ni = pi.getNodeInst();
            Poly poly = Highlight.getNodeInstOutline(ni);
            poly.transform(trans);
            highlighter.addPoly(poly, cell, null);
        }
        for (Iterator it = exports.iterator(); it.hasNext(); ) {
            Export ep = (Export)it.next();
            PortInst pi = ep.getOriginalPort();
            NodeInst ni = pi.getNodeInst();
            Poly poly = ni.getShapeOfPort(pi.getPortProto());
            poly.transform(trans);
            highlighter.addPoly(poly, cell, Color.YELLOW);
        }
    }
}

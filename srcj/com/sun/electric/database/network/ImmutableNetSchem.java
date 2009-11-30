/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableNetSchem.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.network;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellRevision;
import com.sun.electric.database.CellTree;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.ExportId;
import com.sun.electric.database.id.NodeProtoId;
import com.sun.electric.database.id.PortProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.PrimitivePortId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author dn146861
 */
class ImmutableNetSchem {
    private final Snapshot snapshot;
    private final TechPool techPool;
    private final Schematics schem;
    private final PrimitivePortId busPinPortId;
    private final ArcProto busArc;
    private final CellTree cellTree;
    private final CellBackup cellBackup;
    private final CellBackup.Memoization m;
    private final CellRevision cellRevision;
    private final CellId cellId;
    private final ImmutableArrayList<ImmutableExport> exports;
    private final ImmutableArrayList<ImmutableNodeInst> nodes;
    private final ImmutableArrayList<ImmutableArcInst> arcs;
    private final int numExports;
    private final int numNodes;
    private final int numArcs;

    private final int[] ni_pi;
    private final int arcsOffset;
    private final int[] tailConn;
    private final int[] headConn;
    private final int[] drawns;

    private final ArrayList<PortInst> stack = new ArrayList<PortInst>();
    private int numDrawns;
    private int numExportedDrawns;
    private int numConnectedDrawns;



    ImmutableNetSchem(Snapshot snapshot, CellId cellId) {
        this.snapshot = snapshot;
        this.cellId = cellId;
        techPool = snapshot.techPool;
        schem = techPool.getSchematics();
        busPinPortId = schem != null ? schem.busPinNode.getPort(0).getId() : null;
        busArc = schem != null ? schem.bus_arc : null;
        cellTree = snapshot.getCellTree(cellId);
        cellBackup = cellTree.top;
        m = cellBackup.getMemoization();
        cellRevision = cellBackup.cellRevision;
        exports = cellRevision.exports;
        nodes = cellRevision.nodes;
        arcs = cellRevision.arcs;
        numExports = cellRevision.exports.size();
        numNodes = cellRevision.nodes.size();
        numArcs = cellRevision.arcs.size();

        // init connections
        ni_pi = new int[numNodes];
        int offset = numExports;
        for (int i = 0; i < numNodes; i++) {
            ImmutableNodeInst n = nodes.get(i);
            ni_pi[i] = offset;
            offset += getNumPorts(n.protoId);
        }
        arcsOffset = offset;
        offset += numArcs;

        headConn = new int[offset];
        tailConn = new int[offset];
        drawns = new int[offset];
        for (int i = numExports; i < arcsOffset; i++) {
            headConn[i] = i;
            tailConn[i] = i;
        }
        for (int i = 0; i < numExports; i++) {
            int portOffset = i;
            ImmutableExport export = exports.get(i);
            int orig = getPortInstOffset(export.originalNodeId, export.originalPortId);
            headConn[portOffset] = headConn[orig];
            headConn[orig] = portOffset;
            tailConn[portOffset] = -1;
        }
        for (int arcIndex = 0; arcIndex < numArcs; arcIndex++) {
            ImmutableArcInst a = arcs.get(arcIndex);
            int arcOffset = arcsOffset + arcIndex;
            int head = getPortInstOffset(a.headNodeId, a.headPortId);
            headConn[arcOffset] = headConn[head];
            headConn[head] = arcOffset;
            int tail = getPortInstOffset(a.tailNodeId, a.tailPortId);
            tailConn[arcOffset] = tailConn[tail];
            tailConn[tail] = arcOffset;
        }
    }

    private void addToDrawn1(PortInst pi) {
        int piOffset = pi.getPortInstOffset();
        if (drawns[piOffset] >= 0) {
            return;
        }
        if (pi.portId instanceof PrimitivePortId && techPool.getPrimitivePort((PrimitivePortId)pi.portId).isIsolated()) {
            return;
        }
        drawns[piOffset] = numDrawns;
        if (NetworkTool.debug) {
            System.out.println(numDrawns + ": " + pi);
        }

        for (int k = piOffset; headConn[k] != piOffset;) {
            k = headConn[k];
            if (drawns[k] >= 0) {
                continue;
            }
            if (k < arcsOffset) {
                // This is port
                drawns[k] = numDrawns;
                if (NetworkTool.debug) {
                    System.out.println(numDrawns + ": " + exports.get(k));
                }
                continue;
            }
            ImmutableArcInst a = arcs.get(k - arcsOffset);
            ArcProto ap = techPool.getArcProto(a.protoId);
            if (ap.getFunction() == ArcProto.Function.NONELEC) {
                continue;
            }
            if (pi.portId == busPinPortId && ap != busArc) {
                continue;
            }
            drawns[k] = numDrawns;
            if (NetworkTool.debug) {
                System.out.println(numDrawns + ": " + a.name);
            }
            PortInst tpi = new PortInst(a, ImmutableArcInst.TAILEND);
            if (tpi.portId == busPinPortId && ap != busArc) {
                continue;
            }
            stack.add(tpi);
        }
        for (int k = piOffset; tailConn[k] != piOffset;) {
            k = tailConn[k];
            if (drawns[k] >= 0) {
                continue;
            }
            ImmutableArcInst a = arcs.get(k - arcsOffset);
            ArcProto ap = techPool.getArcProto(a.protoId);
            if (ap.getFunction() == ArcProto.Function.NONELEC) {
                continue;
            }
            if (pi.portId == busPinPortId && ap != busArc) {
                continue;
            }
            drawns[k] = numDrawns;
            if (NetworkTool.debug) {
                System.out.println(numDrawns + ": " + a.name);
            }
            PortInst hpi = new PortInst(a, ImmutableArcInst.HEADEND);
            if (hpi.portId == busPinPortId && ap != busArc) {
                continue;
            }
            stack.add(hpi);
        }
    }

    private void addToDrawn(PortInst pi) {
        assert stack.isEmpty();
        stack.add(pi);
        while (!stack.isEmpty()) {
            pi = stack.remove(stack.size() - 1);
            PortProtoId ppId = pi.portId;
            int numPorts = getNumPorts(ppId.getParentId());
            if (numPorts == 1 || ppId instanceof ExportId) {
                addToDrawn1(pi);
                continue;
            }
            PrimitivePort pp = techPool.getPrimitivePort((PrimitivePortId) ppId);
            PrimitiveNode pn = pp.getParent();
            int topology = pp.getTopology();
            for (int i = 0; i < numPorts; i++) {
                PrimitivePort pp2 = pn.getPort(i);
                if (pp2.getTopology() != topology) {
                    continue;
                }
                addToDrawn1(new PortInst(pi.n.nodeId, pp2.getId()));
            }
        }
    }

    void makeDrawns() {
        Arrays.fill(drawns, -1);
        numDrawns = 0;
        for (int i = 0; i < numExports; i++) {
            if (drawns[i] >= 0) {
                continue;
            }
            drawns[i] = numDrawns;
            ImmutableExport export = exports.get(i);
            addToDrawn(new PortInst(export));
            numDrawns++;
        }
        numExportedDrawns = numDrawns;
        for (int i = 0; i < numArcs; i++) {
            if (drawns[arcsOffset + i] >= 0) {
                continue;
            }
            ImmutableArcInst a = arcs.get(i);
            ArcProto ap = techPool.getArcProto(a.protoId);
            if (ap.getFunction() == ArcProto.Function.NONELEC) {
                continue;
            }
            drawns[arcsOffset + i] = numDrawns;
            if (NetworkTool.debug) {
                System.out.println(numDrawns + ": " + ai);
            }
            PortInst hpi = new PortInst(a, ImmutableArcInst.HEADEND);
            if (hpi.portId != busPinPortId || ap == busArc) {
                addToDrawn(hpi);
            }
            PortInst tpi = new PortInst(a, ImmutableArcInst.TAILEND);
            if (tpi.portId != busPinPortId || ap == busArc) {
                addToDrawn(tpi);
            }
            numDrawns++;
        }
        numConnectedDrawns = numDrawns;
        for (int i = 0; i < numNodes; i++) {
//            ImmutableNodeInst n = nodes.get(i);
//            if (n.protoId instanceof CellId) {
//                if (ni.isIconOfParent())
//                    continue;
//            } else {
//                PrimitiveNode pn = techPool.getPrimitiveNode((PrimitiveNodeId) n.protoId);
//                if (pn.getFunction() == PrimitiveNode.Function.ART && pn != Generic.tech().simProbeNode
//                        || pn == Artwork.tech().pinNode
//                        || pn == Generic.tech().invisiblePinNode) {
//                    continue;
//                }
//            }
//            NodeProto np = ni.getProto();
//            int numPortInsts = np.getNumPorts();
//            for (int j = 0; j < numPortInsts; j++) {
//                PortInst pi = ni.getPortInst(j);
//                int piOffset = getPortInstOffset(pi);
//                if (drawns[piOffset] >= 0) {
//                    continue;
//                }
//                if (pi.getPortProto() instanceof PrimitivePort && ((PrimitivePort) pi.getPortProto()).isIsolated()) {
//                    continue;
//                }
//                addToDrawn(pi);
//                numDrawns++;
//            }
        }
    }

    private int getPortInstOffset(int nodeId, PortProtoId portId) {
        return ni_pi[m.getNodeIndexByNodeId(nodeId)] + getPortIndex(portId);
    }
    
    private class PortInst {
        private final ImmutableNodeInst n;
        private final int nodeIndex;
        private final PortProtoId portId;
        private final int portIndex;
        
        private PortInst(int nodeId, PortProtoId portId) {
            nodeIndex = m.getNodeIndexByNodeId(nodeId);
            n = nodes.get(nodeIndex);
            this.portId = portId;
            portIndex = getPortIndex(portId);
        }
        
        private PortInst(ImmutableExport e) {
            this(e.originalNodeId, e.originalPortId);
        }
        
        private PortInst(ImmutableArcInst a, int end) {
            this(end != 0 ? a.headNodeId : a.tailNodeId, end != 0 ? a.headPortId : a.tailPortId);
        }
        
        private int getPortInstOffset() {
            return ni_pi[nodeIndex] + portIndex;
        }
    }

    private int getNumPorts(NodeProtoId nodeProtoId) {
        if (nodeProtoId instanceof CellId) {
            return snapshot.getCell((CellId)nodeProtoId).cellRevision.exports.size();
        } else {
            return techPool.getPrimitiveNode((PrimitiveNodeId)nodeProtoId).getNumPorts();
        }

    }

    private int getPortIndex(PortProtoId portId) {
        if (portId instanceof ExportId) {
            ExportId exportId = (ExportId)portId;
            return snapshot.getCell(exportId.getParentId()).cellRevision.getExportIndexByExportId(exportId);
        } else {
            return techPool.getPrimitivePort((PrimitivePortId)portId).getPortIndex();
        }
    }
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableNetLayout.java
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
package com.sun.electric.database;

import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.CellUsage;
import com.sun.electric.database.id.NodeProtoId;
import com.sun.electric.database.id.PortProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.PrimitivePortId;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.technologies.Schematics;
import java.util.Arrays;
import java.util.IdentityHashMap;

/**
 *
 */
class ImmutableNetLayout {
    final int numExports;
    /**
     * Equivalence of ports.
     * equivPorts.size == ports.size.
     * equivPorts[i] contains minimal index among ports of its group.
     */
    final int[] equivPortsN;
    final int[] equivPortsP;
    final int[] equivPortsA;

    private CellBackup cellBackup;
    private CellRevision cellRevision;
    private IdentityHashMap<NodeProtoId,NodeProtoInfo> nodeProtoInfos;
    private IdentityHashMap<PortProtoId,Object> isolatedPorts;
    /** Node offsets. */
    int[] ni_pi;
    /** */
    int[] netMap;

    ImmutableNetLayout(CellTree cellTree) {
        cellBackup = cellTree.top;
        numExports = cellTree.top.cellRevision.exports.size();
        initConnections(cellTree);

        equivPortsN = computePortsN();
        equivPortsP = computePortsP();
        equivPortsA = computePortsA();

        cellBackup = null;
        cellRevision = null;
        nodeProtoInfos = null;
        isolatedPorts = null;
        ni_pi = null;
        netMap = null;
    }

    private int[] computePortsN() {
        for (ImmutableNodeInst n: cellRevision.nodes) {
            NodeProtoInfo npi = nodeProtoInfos.get(n.protoId);
            if (npi.numPorts == 1) {
                continue;
            }
            connectInternals(n, npi.equivPortsN);
        }
        return equivMap(null);
    }

    private int[] computePortsP() {
        TechPool techPool = cellBackup.techPool;
        Schematics schemTech = techPool.getSchematics();
        PrimitiveNodeId schemResistor = schemTech != null ? schemTech.resistorNode.getId() : null;

        boolean changed = false;
        for (ImmutableNodeInst n: cellRevision.nodes) {
            NodeProtoInfo npi = nodeProtoInfos.get(n.protoId);
            if (npi.equivPortsP == npi.equivPortsN || n.protoId == schemResistor && n.techBits != 0) {
                continue;
            }
            if (connectInternals(n, npi.equivPortsP)) {
                changed = true;
            }
        }
        return changed ? equivMap(equivPortsN) : equivPortsN;
   }

    private int[] computePortsA() {
        TechPool techPool = cellBackup.techPool;
        Schematics schemTech = techPool.getSchematics();

        PrimitiveNodeId schemResistor = schemTech != null ? schemTech.resistorNode.getId() : null;
        boolean changed = false;
        for (ImmutableNodeInst n: cellRevision.nodes) {
            NodeProtoInfo npi = nodeProtoInfos.get(n.protoId);
            if (npi.equivPortsA == npi.equivPortsP && (n.protoId != schemResistor || n.techBits == 0)) {
                continue;
            }
            if (connectInternals(n, npi.equivPortsA)) {
                changed = true;
            }
        }
        return changed ? equivMap(equivPortsP) :  equivPortsP;
    }

    private boolean connectInternals(ImmutableNodeInst n, int[] equivPorts) {
        boolean changed = false;
        int nodeBase = ni_pi[n.nodeId];
        for (int i = 1; i < equivPorts.length; i++) {
            int eq = equivPorts[i];
            if (eq != i && connectMap(netMap, nodeBase + i, nodeBase + eq)) {
                changed = true;
            }
        }
        return changed;
    }

    private static final int[] EQUIV_PORTS_1 = { 0 };

    private static class NodeProtoInfo {
        final int numPorts;
        final int[] equivPortsN;
        final int[] equivPortsP;
        final int[] equivPortsA;
        final int[] portIndexByChron;
        final EquivPorts subNet;

        private NodeProtoInfo(CellTree cellTree, NodeProtoId nodeProtoId) {
            if (nodeProtoId instanceof CellId) {
                CellUsage cu = cellTree.top.cellRevision.d.cellId.getUsageIn((CellId)nodeProtoId);
                CellTree subTree = cellTree.subTrees[cu.indexInParent];
                subNet = subTree.getEquivPorts();
                numPorts = subNet.numExports;
                equivPortsN = subNet.equivPortsN;
                equivPortsP = subNet.equivPortsP;
                equivPortsA = subNet.equivPortsA;
                portIndexByChron = subTree.top.cellRevision.exportIndex;
            } else {
                subNet = null;
                PrimitiveNode pn = cellTree.techPool.getPrimitiveNode((PrimitiveNodeId)nodeProtoId);
                numPorts = pn.getNumPorts();
                int maxChronId = -1;
                for (int portIndex = 0; portIndex < pn.getNumPorts(); portIndex++) {
                    PrimitivePort pp = pn.getPort(portIndex);
                    maxChronId = Math.max(maxChronId, pp.getId().chronIndex);
                }
                portIndexByChron = new int[maxChronId + 1];
                Arrays.fill(portIndexByChron, -1);
                for (int portIndex = 0; portIndex < pn.getNumPorts(); portIndex++) {
                    PrimitivePort pp = pn.getPort(portIndex);
                    portIndexByChron[pp.getId().chronIndex] = portIndex;
                }
                if (pn.getNumPorts() == 1) {
                    equivPortsN = equivPortsP = equivPortsA = EQUIV_PORTS_1;
                } else {
                    equivPortsN = initMap(pn.getNumPorts());
                    for (int i = 0; i < pn.getNumPorts(); i++) {
                        for (int j = i + 1; j < pn.getNumPorts(); j++) {
                            if (pn.getPort(i).getTopology() == pn.getPort(j).getTopology()) {
                                connectMap(equivPortsN, i, j);
                            }
                        }
                    }
                    closureMap(equivPortsN);
                    PrimitiveNode.Function fun = pn.getFunction();
                    if (fun == PrimitiveNode.Function.RESIST) {
                        assert equivPortsN.length == 2;
                        equivPortsP = equivPortsA = new int[] { 0, 0 };
                    } else if (fun.isComplexResistor()) {
                        equivPortsP = equivPortsN;
                        equivPortsA = new int[] { 0, 0 };
                    } else {
                        equivPortsP = equivPortsA = equivPortsN;
                    }
                }
            }
        }
    }

    private void initConnections(CellTree cellTree) {
        cellRevision = cellBackup.cellRevision;
        int maxNodeId = -1;
        for (ImmutableNodeInst n: cellRevision.nodes) {
            maxNodeId = Math.max(maxNodeId, n.nodeId);
        }
        ni_pi = new int[maxNodeId + 1];
        nodeProtoInfos = new IdentityHashMap<NodeProtoId,NodeProtoInfo>();
        initNetMap(cellTree);
        connectExports();
        connectArcs();
    }

    private void initNetMap(CellTree cellTree) {
        TechPool techPool = cellBackup.techPool;
        Schematics schemTech = techPool.getSchematics();
        int nodeBase = numExports;
        for (ImmutableNodeInst n: cellRevision.nodes) {
            ni_pi[n.nodeId] = nodeBase;
            NodeProtoInfo npi = nodeProtoInfos.get(n.protoId);
            if (npi == null) {
                npi = new NodeProtoInfo(cellTree, n.protoId);
                nodeProtoInfos.put(n.protoId, npi);
                if (n.protoId instanceof PrimitiveNodeId) {
                    PrimitiveNode pn = techPool.getPrimitiveNode((PrimitiveNodeId)n.protoId);
                    if (pn.getTechnology() == schemTech) {
                        for (PrimitivePort pp: ArrayIterator.i2i(pn.getPrimitivePorts())) {
                            if (pp.isIsolated()) {
                                if (isolatedPorts == null) {
                                    isolatedPorts = new IdentityHashMap<PortProtoId,Object>();
                                }
                                isolatedPorts.put(pp.getId(), null);
                            }
                        }
                    }
                }
            }
            nodeBase += npi.numPorts;
        }
        netMap = initMap(nodeBase);
    }

    private void connectExports() {
        for (int exportIndex = 0; exportIndex < cellRevision.exports.size(); exportIndex++) {
            ImmutableExport e = cellRevision.exports.get(exportIndex);
            connectMap(netMap, exportIndex, mapIndex(e.originalNodeId, e.originalPortId));
        }
    }

    private void connectArcs() {
        TechPool techPool = cellBackup.techPool;
        Schematics schemTech = techPool.getSchematics();
        PrimitivePortId busPinPortId = schemTech != null ? schemTech.busPinNode.getPort(0).getId() : null;
        ArcProto busArc = schemTech != null ? schemTech.bus_arc : null;
        for (ImmutableArcInst a: cellRevision.arcs) {
            ArcProto ap = techPool.getArcProto(a.protoId);
            if (ap.getFunction() == ArcProto.Function.NONELEC) {
                continue;
            }
            if (isolatedPorts != null && (isolatedPorts.containsKey(a.tailPortId) || isolatedPorts.containsKey(a.headPortId))) {
                continue;
            }
            if ((a.tailPortId == busPinPortId || a.headPortId == busPinPortId) && ap != busArc) {
                continue;
            }
            connectMap(netMap, mapIndex(a.tailNodeId, a.tailPortId), mapIndex(a.headNodeId, a.headPortId));
        }
    }

    private int mapIndex(int nodeId, PortProtoId portId) {
        NodeProtoInfo npi = nodeProtoInfos.get(portId.parentId);
        return ni_pi[nodeId] + npi.portIndexByChron[portId.chronIndex];
    }

    private int[] equivMap(int[] oldMap) {
        closureMap(netMap);

        if (oldMap != null && oldMap.length == numExports) {
            boolean eq = true;
            for (int i = 0; i < numExports; i++) {
                if (netMap[i] != oldMap[i]) {
                    eq = false;
                    break;
                }
            }
            if (eq) {
                return oldMap;
            }
        }
        int[] map = new int[numExports];
        System.arraycopy(netMap, 0, map, 0, numExports);
        return map;
    }

    /**
     * Init equivalence map.
     * @param size numeber of elements in equivalence map
     * @return integer array representing equivalence map consisting of disjoint elements.
     */
    static int[] initMap(int size) {
        int[] map = new int[size];
        for (int i = 0; i < map.length; i++) {
            map[i] = i;
        }
        return map;
    }

    /**
     * Merge classes of equivalence map to which elements a1 and a2 belong.
     * Returns true if the classes were different
     */
    static boolean connectMap(int[] map, int a1, int a2) {
        int m1, m2, m;

        for (m1 = a1; map[m1] != m1; m1 = map[m1]);
        for (m2 = a2; map[m2] != m2; m2 = map[m2]);
        boolean changed = m1 != m2;
        m = m1 < m2 ? m1 : m2;

        for (;;) {
            int k = map[a1];
            map[a1] = m;
            if (a1 == k) {
                break;
            }
            a1 = k;
        }
        for (;;) {
            int k = map[a2];
            map[a2] = m;
            if (a2 == k) {
                break;
            }
            a2 = k;
        }
        return changed;
    }

    /**
     * Obtain canonical representation of equivalence map.
     */
    static void closureMap(int[] map) {
        for (int i = 0; i < map.length; i++) {
            map[i] = map[map[i]];
        }
    }
}

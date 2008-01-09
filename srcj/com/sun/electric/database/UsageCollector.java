/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UsageCollector.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
import com.sun.electric.database.id.ExportId;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.PortProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.technology.PrimitivePort;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Package-private class to collect usage of libraries/cells/exports in CellBackup or in Library variables.
 * Export usages of subcells are represented by BitSet of ExportId's chronological indices.
 * If a subcell is used and no its ExportIds are used, it is represented by empty BitSet.
 */
class UsageCollector {
    static final BitSet EMPTY_BITSET = new BitSet();

    private ImmutableCell d;
    private HashMap<CellId, CellUsageInfoBuilder> cellIndices = new HashMap<CellId,CellUsageInfoBuilder>(16, 0.5f);
    private HashSet<TechId> techUsed = new HashSet<TechId> (16, 0.5f);
    
    /**
     * Collect usages in lists of nodes/arcs/exports together with Cell's variables. 
     */
    UsageCollector(ImmutableCell d,
            ImmutableArrayList<ImmutableNodeInst> nodes,
            ImmutableArrayList<ImmutableArcInst> arcs,
            ImmutableArrayList<ImmutableExport> exports) {
        this.d = d;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            ImmutableNodeInst n = nodes.get(nodeIndex);
            if (n.protoId instanceof CellId)
                add((CellId)n.protoId, true);
            else
                techUsed.add(((PrimitiveNodeId)n.protoId).techId);
            for (int chronIndex = 0; chronIndex < n.ports.length; chronIndex++) {
                ImmutablePortInst pi = n.ports[chronIndex];
                if (pi == ImmutablePortInst.EMPTY) continue;
                PortProtoId pp = n.protoId.getPortId(chronIndex);
                add(pp);
            }
        }
        for (int arcIndex = 0; arcIndex < arcs.size(); arcIndex++) {
            ImmutableArcInst a = arcs.get(arcIndex);
            techUsed.add(a.protoId.techId);
            add(a.tailPortId);
            add(a.headPortId);
        }
        for (int portIndex = 0; portIndex < exports.size(); portIndex++) {
            ImmutableExport e = exports.get(portIndex);
            add(e.originalPortId);
        }
    }
    
    private void add(PortProtoId portId) {
        if (portId instanceof PrimitivePort) return;
        ExportId eId = (ExportId)portId;
        add(eId.parentId, false).set(eId.chronIndex);
    }
    
    private BitSet add(CellId cellId, boolean isInstance) {
        CellUsageInfoBuilder cellCount = cellIndices.get(cellId);
        if (cellCount == null) {
            cellCount = new CellUsageInfoBuilder();
            cellIndices.put(cellId, cellCount);
        }
        if (isInstance)
            cellCount.instCount++;
        return cellCount.usedExports;
    }
    
    /**
     * Return TechId usages for CellBackup.
     */
    BitSet getTechUsages(BitSet oldTechUsages) {
        IdManager idManager = d.cellId.idManager;
        BitSet techUsages = new BitSet();
        techUsages.set(d.techId.techIndex);
        for (TechId techId: techUsed)
            techUsages.set(techId.techIndex);
        return bitSetWith(oldTechUsages, techUsages); 
    }
    
    /**
     * Return usages for CellRevision.
     */
    CellRevision.CellUsageInfo[] getCellUsages(CellRevision.CellUsageInfo[] oldCellUsages) {
        if (cellIndices.isEmpty()) return CellRevision.NULL_CELL_USAGE_INFO_ARRAY;
        CellId parentId = d.cellId;
        int length = 0;
        for (CellId cellId: cellIndices.keySet())
            length = Math.max(length, parentId.getUsageIn(cellId).indexInParent + 1);
        CellRevision.CellUsageInfo[] newCellUsages = new CellRevision.CellUsageInfo[length];
        for (CellId cellId: cellIndices.keySet()) {
            CellUsage u = parentId.getUsageIn(cellId);
            int indexInParent = u.indexInParent;
            CellRevision.CellUsageInfo newC = null;
            CellUsageInfoBuilder cellCount = cellIndices.get(cellId);
            if (cellCount != null) {
                CellRevision.CellUsageInfo oldC = indexInParent < oldCellUsages.length ? oldCellUsages[indexInParent] : null;
                if (oldC != null)
                    newC = oldC.with(cellCount.instCount, cellCount.usedExports);
                else
                    newC = new CellRevision.CellUsageInfo(cellCount.instCount, cellCount.usedExports);
            }
            newCellUsages[indexInParent] = newC;
        }
        return Arrays.equals(newCellUsages, oldCellUsages) ? oldCellUsages : newCellUsages;
    }

    static BitSet bitSetWith(BitSet oldBitSet, BitSet newBitSet) {
        if (newBitSet.isEmpty())
            return EMPTY_BITSET;
        return newBitSet.equals(oldBitSet) ? oldBitSet : newBitSet;
    }
    
    private static class CellUsageInfoBuilder {
        int instCount;
        BitSet usedExports = new BitSet();
    }
}
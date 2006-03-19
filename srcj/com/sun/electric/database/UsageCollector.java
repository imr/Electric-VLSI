/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UsageCollector.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.database;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitivePort;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;

/**
 * Package-private class to collect usage of libraries/cells/exports in CellBackup or in Library variables.
 * Export usages of subcells are represented by BitSet of ExportId's chronological indices.
 * If a subcell is used and no its ExportIds are used, it is represented by empty BitSet.
 */
class UsageCollector {
    static final BitSet EMPTY_BITSET = new BitSet();

    private HashMap<CellId, CellUsageInfoBuilder> cellIndices = new HashMap<CellId,CellUsageInfoBuilder>(16, 0.5f);
    private BitSet libUsages = new BitSet();
    
    /**
     * Collect usages in lists of nodes/arcs/exports together with Cell's variables. 
     */
    UsageCollector(ImmutableCell d,
            ImmutableArrayList<ImmutableNodeInst> nodes,
            ImmutableArrayList<ImmutableArcInst> arcs,
            ImmutableArrayList<ImmutableExport> exports) {
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            ImmutableNodeInst n = nodes.get(nodeIndex);
            if (n.protoId instanceof CellId)
                add((CellId)n.protoId, true);
            addVars(n);
            for (int chronIndex = 0; chronIndex < n.ports.length; chronIndex++) {
                ImmutablePortInst pi = n.ports[chronIndex];
                if (pi == ImmutablePortInst.EMPTY) continue;
                PortProtoId pp = n.protoId.getPortId(chronIndex);
                add(pp);
                addVars(pi);
            }
        }
        for (int arcIndex = 0; arcIndex < arcs.size(); arcIndex++) {
            ImmutableArcInst a = arcs.get(arcIndex);
            add(a.tailPortId);
            add(a.headPortId);
            addVars(a);
        }
        for (int portIndex = 0; portIndex < exports.size(); portIndex++) {
            ImmutableExport e = exports.get(portIndex);
            add(e.originalPortId);
            addVars(e);
        }
        addVars(d);
    }
    
    /**
     * Collect usages in ImmutableLibrary variables.
     */
    UsageCollector(ImmutableLibrary d) {
        addVars(d);
    }
    
    private void addVars(ImmutableElectricObject d) {
        for (Variable var: d.getVars()) {
            if (!var.hasReferences()) continue;
            if (var.isArray()) {
                for (int i = 0, n = var.getLength(); i < n; i++)
                    addVarObj(var.getObject(i));
            } else {
                addVarObj(var.getObject());
            }
        }
    }
    
    private void addVarObj(Object o) {
        if (o instanceof LibId)
            add((LibId)o);
        else if (o instanceof CellId)
            add((CellId)o, false);
        else if (o instanceof ExportId)
            add((ExportId)o);
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
    
    private void add(LibId libId) {
        libUsages.set(libId.libIndex);
    }
    
    /**
     * Return usages for CellBackup.
     */
    CellBackup.CellUsageInfo[] getCellUsages(CellId parentId, CellBackup.CellUsageInfo[] oldCellUsages) {
        if (cellIndices.isEmpty()) return CellBackup.NULL_CELL_USAGE_INFO_ARRAY;
        int length = 0;
        for (CellId cellId: cellIndices.keySet())
            length = Math.max(length, parentId.getUsageIn(cellId).indexInParent + 1);
        CellBackup.CellUsageInfo[] newCellUsages = new CellBackup.CellUsageInfo[length];
        for (CellId cellId: cellIndices.keySet()) {
            CellUsage u = parentId.getUsageIn(cellId);
            int indexInParent = u.indexInParent;
            CellBackup.CellUsageInfo newC = null;
            CellUsageInfoBuilder cellCount = cellIndices.get(cellId);
            if (cellCount != null) {
                CellBackup.CellUsageInfo oldC = indexInParent < oldCellUsages.length ? oldCellUsages[indexInParent] : null;
                if (oldC != null)
                    newC = oldC.with(cellCount.instCount, cellCount.usedExports);
                else
                    newC = new CellBackup.CellUsageInfo(cellCount.instCount, cellCount.usedExports);
            }
            newCellUsages[indexInParent] = newC;
        }
        return Arrays.equals(newCellUsages, oldCellUsages) ? oldCellUsages : newCellUsages;
    }

    /**
     * Return usages for LibraryBackup.
     */
    HashMap<CellId,BitSet> getExportUsages(HashMap<CellId,BitSet> oldExportUsages) {
        if (cellIndices == null) return null;
        HashMap<CellId,BitSet> newExportUsages = new HashMap<CellId,BitSet>();
        for (CellId cellId: cellIndices.keySet()) {
            BitSet oldSet = oldExportUsages != null ? oldExportUsages.get(cellId) : null;
            BitSet newSet = bitSetWith(oldSet, cellIndices.get(cellId).usedExports);
            newExportUsages.put(cellId, newSet);
        }
        return oldExportUsages != null && oldExportUsages.equals(newExportUsages) ? oldExportUsages : newExportUsages;
    }
    
    BitSet getLibUsages(BitSet oldLibUsages) { return bitSetWith(oldLibUsages, libUsages); }
    
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
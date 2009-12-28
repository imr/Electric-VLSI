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
import com.sun.electric.database.id.PortProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.PrimitivePortId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * Package-private class to collect usage of libraries/cells/exports in CellBackup or in Library variables.
 * Export usages of subcells are represented by BitSet of ExportId's chronological indices.
 * If a subcell is used and no its ExportIds are used, it is represented by empty BitSet.
 */
class UsageCollector {

    static final BitSet EMPTY_BITSET = new BitSet();
    static final TreeMap<Variable.AttrKey, TextDescriptor.Unit> EMPTY_ATTRIBUTES = new TreeMap<Variable.AttrKey, TextDescriptor.Unit>();
    private final ImmutableCell d;
    private final HashMap<CellId, CellUsageInfoBuilder> cellIndices = new HashMap<CellId, CellUsageInfoBuilder>(16, 0.5f);
    private final HashSet<TechId> techUsed = new HashSet<TechId>(16, 0.5f);

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
            if (n.protoId instanceof CellId) {
                CellUsageInfoBuilder cellCount = add((CellId) n.protoId, true);
                if (cellCount.isIcon) {
                    for (Variable param : n.getDefinedParams()) {
                        cellCount.addAttribute((Variable.AttrKey) param.getKey(), param.getUnit());
                    }
                    for (int varIndex = 0; varIndex < n.getNumVariables(); varIndex++) {
                        Variable.Key varKey = n.getVar(varIndex).getKey();
                        if (varKey.isAttribute()) {
                            cellCount.addAttribute((Variable.AttrKey) varKey, null);
                        }
                    }
                }
            } else {
                techUsed.add(((PrimitiveNodeId) n.protoId).techId);
            }
            for (int chronIndex = 0; chronIndex < n.ports.length; chronIndex++) {
                ImmutablePortInst pi = n.ports[chronIndex];
                if (pi == ImmutablePortInst.EMPTY) {
                    continue;
                }
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
        if (portId instanceof PrimitivePortId) {
            return;
        }
        ExportId eId = (ExportId) portId;
        add(eId.getParentId(), false).usedExports.set(eId.chronIndex);
    }

    private CellUsageInfoBuilder add(CellId cellId, boolean isInstance) {
        CellUsageInfoBuilder cellCount = cellIndices.get(cellId);
        if (cellCount == null) {
            cellCount = new CellUsageInfoBuilder(d.cellId.getUsageIn(cellId));
            cellIndices.put(cellId, cellCount);
        }
        if (isInstance) {
            cellCount.instCount++;
        }
        return cellCount;
    }

    /**
     * Return TechId usages for CellBackup.
     */
    BitSet getTechUsages(BitSet oldTechUsages) {
        BitSet techUsages = new BitSet();
        techUsages.set(d.techId.techIndex);
        for (TechId techId : techUsed) {
            techUsages.set(techId.techIndex);
        }
        return bitSetWith(oldTechUsages, techUsages);
    }

    /**
     * Return usages for CellRevision.
     */
    CellRevision.CellUsageInfo[] getCellUsages(CellRevision.CellUsageInfo[] oldCellUsages) {
        if (cellIndices.isEmpty()) {
            return CellRevision.NULL_CELL_USAGE_INFO_ARRAY;
        }
        CellId parentId = d.cellId;
        int length = 0;
        for (CellId cellId : cellIndices.keySet()) {
            length = Math.max(length, parentId.getUsageIn(cellId).indexInParent + 1);
        }
        CellRevision.CellUsageInfo[] newCellUsages = new CellRevision.CellUsageInfo[length];
        for (CellId cellId : cellIndices.keySet()) {
            CellUsage u = parentId.getUsageIn(cellId);
            int indexInParent = u.indexInParent;
            CellUsageInfoBuilder cellCount = cellIndices.get(cellId);
            if (cellCount != null) {
                CellRevision.CellUsageInfo oldC = indexInParent < oldCellUsages.length ? oldCellUsages[indexInParent] : null;
                newCellUsages[indexInParent] = cellCount.getCellUsageInfo(oldC);
            }
        }
        return Arrays.equals(newCellUsages, oldCellUsages) ? oldCellUsages : newCellUsages;
    }

    static BitSet bitSetWith(BitSet oldBitSet, BitSet newBitSet) {
        if (newBitSet.isEmpty()) {
            return EMPTY_BITSET;
        }
        return newBitSet.equals(oldBitSet) ? oldBitSet : newBitSet;
    }

    static TreeMap<Variable.AttrKey, TextDescriptor.Unit> usedAttributesWith(TreeMap<Variable.AttrKey, TextDescriptor.Unit> oldAttributes,
            TreeMap<Variable.AttrKey, TextDescriptor.Unit> newAttributes) {
        if (newAttributes == null) {
            return null;
        }
        if (newAttributes.isEmpty()) {
            return EMPTY_ATTRIBUTES;
        }
        return newAttributes.equals(oldAttributes) ? oldAttributes : newAttributes;
    }

    private static class CellUsageInfoBuilder {

        private final CellUsage cellUsage;
        private final boolean isIcon;
        private int instCount;
        private final BitSet usedExports = new BitSet();
        private final TreeMap<Variable.AttrKey, TextDescriptor.Unit> usedAttributes;

        private CellUsageInfoBuilder(CellUsage cellUsage) {
            this.cellUsage = cellUsage;
            isIcon = cellUsage.protoId.isIcon();
            usedAttributes = isIcon ? new TreeMap<Variable.AttrKey, TextDescriptor.Unit>() : null;
        }

        private void addAttribute(Variable.AttrKey attrKey, TextDescriptor.Unit unit) {
            TextDescriptor.Unit oldUnit = usedAttributes.get(attrKey);
            if (oldUnit != null) {
                if (unit != oldUnit) {
                    throw new IllegalArgumentException(attrKey + " " + unit);
                }
            } else {
                usedAttributes.put(attrKey, unit);
            }
        }

        private CellRevision.CellUsageInfo getCellUsageInfo(CellRevision.CellUsageInfo oldCellUsageInfo) {
            if (oldCellUsageInfo != null) {
                return oldCellUsageInfo.with(instCount, usedExports, usedAttributes);
            } else {
                return new CellRevision.CellUsageInfo(instCount, usedExports, usedAttributes);
            }
        }
    }
}

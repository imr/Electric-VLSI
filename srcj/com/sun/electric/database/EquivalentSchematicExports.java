/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EquivalentSchematicsExports.java
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
import com.sun.electric.database.id.ExportId;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.text.ImmutableArrayList;

import com.sun.electric.database.text.Name;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 *
 */
public class EquivalentSchematicExports {

    public final CellId cellId;
    public final EquivalentSchematicExports implementation;
    public final int[] portImplementation;
    public final Global.Set globals;
    public final int numExports;
    public final int[] portOffsets;
    public final int numExpandedExports;
    final ImmutableArrayList<ImmutableExport> exports;
    private final HashMap<ExportId, Global.Set[]> globalPartitions;
    private IdentityHashMap<Name, Integer> exportNameMapOffsets;
    /**
     * Equivalence of ports.
     * equivPorts.size == ports.size.
     * equivPorts[i] contains minimal index among ports of its group.
     */
    final int[] equivPortsN;
    final int[] equivPortsP;
    final int[] equivPortsA;

    EquivalentSchematicExports(ImmutableNetSchem netSchem) {
        cellId = netSchem.cellTree.top.cellRevision.d.cellId;
        implementation = cellId == netSchem.implementationCellId ? this : netSchem.snapshot.equivSchemExports[netSchem.implementationCellId.cellIndex];
        portImplementation = netSchem.portImplementation;
        exports = netSchem.cellTree.top.cellRevision.exports;
        numExports = exports.size();
        numExpandedExports = netSchem.equivPortsN.length;
        portOffsets = netSchem.portOffsets;
        globals = netSchem.globals;
        globalPartitions = netSchem.globalPartitions;
        equivPortsN = netSchem.equivPortsN;
        equivPortsP = netSchem.equivPortsP;
        equivPortsA = netSchem.equivPortsA;
    }

    public static EquivalentSchematicExports computeEquivExports(Snapshot snapshot, CellId top) {
        ImmutableNetSchem netSchem = new ImmutableNetSchem(snapshot, top);
        return new EquivalentSchematicExports(netSchem);
    }

    public CellId getCellId() {
        return cellId;
    }

    public Global.Set getGlobals() {
        return globals;
    }

    public int getNumExports() {
        return numExports;
    }

    public Name getExportName(int exportIndex) {
        return exports.get(exportIndex).name;
    }

    public int getExportNameMapOffset(Name exportName) {
        assert !exportName.isBus();
        if (exportNameMapOffsets == null) {
            buildExportNameMapOffsets();
        }
        Integer objResult = exportNameMapOffsets.get(exportName);
        return objResult != null ? objResult.intValue() : -1;
    }

    public int getNumExpandedExports() {
        return numExports;
    }

    public int[] getEquivPortsN() {
        return equivPortsN.clone();
    }

    public int[] getEquivPortsP() {
        return equivPortsP.clone();
    }

    public int[] getEquivPortsA() {
        return equivPortsA.clone();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(equivPortsN);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EquivalentSchematicExports)) {
            return false;
        }
        EquivalentSchematicExports that = (EquivalentSchematicExports) o;
        if (this.cellId != that.cellId || this.implementation.cellId != that.implementation.cellId) {
            return false;
        }
        if (cellId != implementation.cellId && !this.implementation.equals(that.implementation)) {
            return false;
        }
        if (!Arrays.equals(this.portImplementation, that.portImplementation)) {
            return false;
        }
        if (this.globals != that.globals) {
            return false;
        }
        if (this.exports != that.exports) {
            if (this.exports.size() != that.exports.size()) {
                return false;
            }
            for (int exportIndex = 0; exportIndex < this.exports.size(); exportIndex++) {
                ImmutableExport e1 = this.exports.get(exportIndex);
                ImmutableExport e2 = that.exports.get(exportIndex);
                if (e1.exportId != e2.exportId || e1.name != e2.name) {
                    return false;
                }
            }
        }
        assert this.numExpandedExports == that.numExpandedExports;
        assert Arrays.equals(this.portOffsets, that.portOffsets);
        if (this.globalPartitions == null || that.globalPartitions == null) {
            if (this.globalPartitions != null || that.globalPartitions != null) {
                return false;
            }
        } else {
            if (this.globalPartitions.size() != that.globalPartitions.size()) {
                return false;
            }
            for (Map.Entry<ExportId, Global.Set[]> e : this.globalPartitions.entrySet()) {
                ExportId eId = e.getKey();
                Global.Set[] thisG = e.getValue();
                Global.Set[] thatG = that.globalPartitions.get(eId);
                if (!Arrays.equals(thisG, thatG)) {
                    return false;
                }
            }
        }
        return Arrays.equals(this.equivPortsN, that.equivPortsN)
                && Arrays.equals(this.equivPortsP, that.equivPortsP)
                && Arrays.equals(this.equivPortsA, that.equivPortsA);
    }

    private void buildExportNameMapOffsets() {
        IdentityHashMap<Name, Integer> map = new IdentityHashMap<Name, Integer>();
        for (int exportIndex = 0; exportIndex < exports.size(); exportIndex++) {
            ImmutableExport e = exports.get(exportIndex);
            for (int busIndex = 0; busIndex < e.name.busWidth(); busIndex++) {
                Name exportName = e.name.subname(busIndex);
                if (map.containsKey(exportName)) {
                    continue;
                }
                Integer mapOffset;
                if (implementation == this) {
                    mapOffset = Integer.valueOf(portOffsets[exportIndex] + busIndex);
                } else {
                    mapOffset = implementation.getExportNameMapOffset(exportName);
                    if (mapOffset == null) {
                        continue;
                    }
                }
                map.put(exportName, mapOffset);
            }
        }
        exportNameMapOffsets = map;
    }
}

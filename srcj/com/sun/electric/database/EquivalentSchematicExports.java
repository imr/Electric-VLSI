/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EquivPorts.java
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

/**
 *
 */
public class EquivalentSchematicExports {
    private final CellId cellId;
    private final Global.Set globals;
    final int numExports;
    private final int numExpandedExports;
    private final ImmutableArrayList<ImmutableExport> exports;
    private final HashMap<ExportId,Global.Set[]> globalPartitions;
    /**
     * Equivalence of ports.
     * equivPorts.size == ports.size.
     * equivPorts[i] contains minimal index among ports of its group.
     */
    final int[] equivPortsN;
    final int[] equivPortsP;
    final int[] equivPortsA;

    EquivalentSchematicExports(CellTree cellTree) {
        cellId = cellTree.top.cellRevision.d.cellId;
        exports = cellTree.top.cellRevision.exports;
        numExpandedExports = numExports = exports.size();
        globals = Global.Set.empty;
        globalPartitions = null;
        ImmutableNetLayout netCell = new ImmutableNetLayout(cellTree);
        equivPortsN = netCell.equivPortsN;
        equivPortsP = netCell.equivPortsP;
        equivPortsA = netCell.equivPortsA;
    }

    public CellId getCellId() {
        return cellId;
    }

    public Global.Set getGlobals() {
        return Global.Set.empty;
    }

    public int getNumExports() {
        return numExports;
    }

    public Name getExportName(int exportIndex) {
        return exports.get(exportIndex).name;
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
        if (!(o instanceof EquivPorts)) {
            return false;
        }
        EquivPorts that = (EquivPorts)o;
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
        return Arrays.equals(this.equivPortsN, that.equivPortsN)
                && Arrays.equals(this.equivPortsP, that.equivPortsP)
                && Arrays.equals(this.equivPortsA, that.equivPortsA);
    }
}

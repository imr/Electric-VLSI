/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetlistImpl.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
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
package com.sun.electric.database.network;

import com.sun.electric.database.CellTree;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author dn146861
 */
public class NetlistImpl extends Netlist {

    private static final String[] NULL_STRING_ARRAY = {};
    /**
     * Arrays of names for each net.
     * First names are exported names in STRING_NUMBER_ORDER,
     * Then internal user-defined names in STRING_NUMBER_ORDER.
     * If this net has no exported or user-defined names, then this
     * list contains one of temporary names.
     * Hence the first name in the list are most appropriate.
     **/
    private String[][] names;
    /** Nets which have has user-defiend names */
    private BitSet isUsernamed = new BitSet();
    /** Number of export names for each net */
    private int[] exportedNamesCount;
    private int[] equivPortIndexByNetIndex;

    NetlistImpl(NetCell netCell, int numExternals, int[] map) {
        super(netCell, Netlist.ShortResistors.NO, numExternals, map);
        if (netCell instanceof NetSchem) {
            expectedSnapshot = new WeakReference<Snapshot>(netCell.database.backup());
        } else {
            expectedCellTree = new WeakReference<CellTree>(netCell.cell.tree());
        }
        exportedNamesCount = new int[numExternalNets];
        equivPortIndexByNetIndex = new int[numExternalNets];
        Arrays.fill(equivPortIndexByNetIndex, -1);
        names = new String[getNumNetworks()][];
        Arrays.fill(names, NULL_STRING_ARRAY);
    }

    void setEquivPortIndexByNetIndex(int equivIndex, int netIndex) {
        equivPortIndexByNetIndex[netIndex] = equivIndex;
    }

    @Override
    Iterator<String> getNames(int netIndex) {
        return ArrayIterator.iterator(names[netIndex]);
    }

    @Override
    Iterator<String> getExportedNames(int netIndex) {
        int exportedNamesCount = netIndex < numExternalNets ? this.exportedNamesCount[netIndex] : 0;
        return ArrayIterator.iterator(names[netIndex], 0, exportedNamesCount);
    }

    @Override
    String getName(int netIndex) {
        return names[netIndex][0];
    }

    @Override
    boolean hasName(int netIndex, String nm) {
        String[] theseNames = names[netIndex];
        for (int i = 0; i < theseNames.length; i++) {
            if (theseNames[i].equals(nm)) {
                return true;
            }
        }
        return false;
    }

    @Override
    void fillNames(int netIndex, Collection<String> exportedNames, Collection<String> privateNames) {
        if (!isUsernamed(netIndex)) {
            return;
        }
        String[] names = this.names[netIndex];
        int exportedNamesCount = netIndex < numExternalNets ? this.exportedNamesCount[netIndex] : 0;
        for (int i = 0; i < exportedNamesCount; i++) {
            exportedNames.add(names[i]);
        }
        if (privateNames != null) {
            for (int i = exportedNamesCount; i < names.length; i++) {
                privateNames.add(names[i]);
            }
        }
    }

    @Override
    boolean isUsernamed(int netIndex) {
        return isUsernamed.get(netIndex);
    }

    boolean hasNames(int netIndex) {
        return names[netIndex].length > 0;
    }

    @Override
    int getEquivPortIndexByNetIndex(int netIndex) {
        return equivPortIndexByNetIndex[netIndex];
    }

    /**
     * Add user name to list of names of this Network.
     * @param netIndex index of Network
     * @param nameKey name key to add.
     * @param exported true if name is exported.
     */
    void addUserName(int netIndex, Name nameKey, boolean exported) {
        assert !nameKey.isTempname();
        String name = nameKey.toString();
        String[] theseNames = names[netIndex];
        int exportedCount = netIndex < numExternalNets ? exportedNamesCount[netIndex] : 0;
        if (exported) {
            assert exportedCount == theseNames.length;
        }
        int i = 0;
        for (; i < theseNames.length; i++) {
            String n = names[netIndex][i];
            int cmp = TextUtils.STRING_NUMBER_ORDER.compare(name, n);
            if (cmp == 0) {
                return;
            }
            if (cmp < 0 && (exported || i >= exportedCount)) {
                break;
            }
        }
        if (theseNames.length == 0) {
            names[netIndex] = new String[]{name};
        } else {
            String[] newNames = new String[theseNames.length + 1];
            System.arraycopy(theseNames, 0, newNames, 0, i);
            newNames[i] = name;
            System.arraycopy(theseNames, i, newNames, i + 1, theseNames.length - i);
            names[netIndex] = newNames;
        }
        if (exported) {
            exportedNamesCount[netIndex]++;
        }
        isUsernamed.set(netIndex);
    }

    void addTempName(int netIndex, String name) {
        assert names[netIndex].length == 0;
        names[netIndex] = new String[]{name};
    }
}

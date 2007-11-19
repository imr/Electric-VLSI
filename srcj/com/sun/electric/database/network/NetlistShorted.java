/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetlistShorted.java
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

import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.TextUtils;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * This class represents Netlist obtained from base Netlist by shortening some Networks.
 */
public class NetlistShorted extends Netlist {

    private Netlist baseNetlist;
    private int[] thisNetHead;
    private int[] baseNetNext;
    private BitSet isUsernamed = new BitSet();
    private BitSet isExported = new BitSet();
    private String[] firstNames;

    NetlistShorted(NetCell netCell, boolean shortResistors, Netlist other, int numExternals, int[] netMap) {
        super(netCell, shortResistors, other, numExternals, netMap);
        baseNetlist = other;
        
        assert nm_net.length == baseNetlist.nm_net.length;
        int[] baseNetToThisNet = new int[baseNetlist.getNumNetworks()];
        Arrays.fill(baseNetToThisNet, -1);
        for (int mapOffset = 0; mapOffset < nm_net.length; mapOffset++) {
            int baseNetIndex = baseNetlist.nm_net[mapOffset];
            int thisNetIndex = nm_net[mapOffset];
            if (baseNetToThisNet[baseNetIndex] < 0) {
                baseNetToThisNet[baseNetIndex] = thisNetIndex;
            } else {
                assert baseNetToThisNet[baseNetIndex] == thisNetIndex;
            }
        }
        for (int thisNet : baseNetToThisNet) {
            assert thisNet >= 0;
        }
        thisNetHead = new int[getNumNetworks()];
        Arrays.fill(thisNetHead, -1);
        baseNetNext = new int[baseNetlist.getNumNetworks()];
        Arrays.fill(baseNetNext, -1);
        for (int baseNetIndex = baseNetlist.getNumNetworks() - 1; baseNetIndex >= 0; baseNetIndex--) {
            int thisNetIndex = baseNetToThisNet[baseNetIndex];
            baseNetNext[baseNetIndex] = thisNetHead[thisNetIndex];
            thisNetHead[thisNetIndex] = baseNetIndex;
        }
        for (int thisHead : thisNetHead) {
            assert thisHead >= 0;
        }
        
        FirstStringCollection exportedNames = new FirstStringCollection();
        FirstStringCollection privateNames = new FirstStringCollection();
        firstNames = new String[getNumNetworks()];
        for (int thisNetIndex = 0; thisNetIndex < getNumNetworks(); thisNetIndex++) {
            exportedNames.clear();
            privateNames.clear();
            for (int baseNetIndex = thisNetHead[thisNetIndex]; baseNetIndex >= 0; baseNetIndex = baseNetNext[baseNetIndex]) {
                baseNetlist.fillNames(baseNetIndex, exportedNames, privateNames);
            }
            String firstName;
            boolean isUsernamed = false;
            boolean isExported = false;
            if (exportedNames.value != null) {
                firstName = exportedNames.value;
                isExported = isUsernamed = true;
            } else if (privateNames.value != null) {
                firstName = privateNames.value;
                isUsernamed = true;
            } else {
                firstName = baseNetlist.getName(thisNetHead[thisNetIndex]);
            }
            firstNames[thisNetIndex] = firstName;
            if (isExported)
                this.isExported.set(thisNetIndex);
            if (isUsernamed)
                this.isUsernamed.set(thisNetIndex);
        }
    }

    @Override
    Iterator<String> getNames( int netIndex) {
        if (isUsernamed(netIndex)) {
            TreeSet<String> exportedNames = new TreeSet<String>(TextUtils.STRING_NUMBER_ORDER);
            TreeSet<String> privateNames = new TreeSet<String>(TextUtils.STRING_NUMBER_ORDER);
            fillNames(netIndex, exportedNames, privateNames);
            ArrayList<String> allNames = new ArrayList<String>(exportedNames);
            for ( String name : privateNames) {
                if (exportedNames.contains(name)) {
                    continue;
                }
                allNames.add(name);
            }
            return allNames.iterator();
        } else {
            return Collections.singleton(firstNames[netIndex]).iterator();
        }
    }

    @Override
    Iterator<String> getExportedNames( int netIndex) {
        if (isExported(netIndex)) {
            TreeSet<String> exportedNames = new TreeSet<String>(TextUtils.STRING_NUMBER_ORDER);
            fillNames(netIndex, exportedNames, null);
            return exportedNames.iterator();
        } else {
            return ArrayIterator.<String>emptyIterator();
        }
    }

    /**
     * Returns most appropriate name of the net.
     * Intitialized net has at least one name - user-defiend or temporary.
     */
    @Override
    String getName( int netIndex) {
        return firstNames[netIndex];
    }

    /** Returns true if nm is one of Network's names */
    @Override
    boolean hasName(int netIndex, String nm) {
        if (isUsernamed(netIndex)) {
            HasStringCollection hasString = new HasStringCollection(nm);
            fillNames(netIndex, hasString, hasString);
            return hasString.hasString;
        } else {
            return firstNames[netIndex].equals(nm);
        }
    }

    /**
     * Add names of this net to two Collections. One for exported, and other for unexported names.
     * @param exportedNames Collection for exported names.
     * @param privateNames Collection for unexported names.
     */
    @Override
    void fillNames( int netIndex,  Collection<String> exportedNames,  Collection<String> privateNames) {
        if (!isUsernamed(netIndex))
            return;
        for ( int baseNetIndex = thisNetHead[netIndex]; baseNetIndex >= 0; baseNetIndex = baseNetNext[baseNetIndex])
            baseNetlist.fillNames(baseNetIndex, exportedNames, privateNames);
    }

    /**
     * Method to tell whether this network has any exports on it.
     * @return true if there are exports on this Network.
     */
    @Override
    boolean isExported(int netIndex) {
        return isExported.get(netIndex);
    }

    /**
     * Method to tell whether this network has user-defined name.
     * @return true if this Network has user-defined name.
     */
    @Override
    boolean isUsernamed(int netIndex) {
        return isUsernamed.get(netIndex);
    }

    void checkNames() {
        TreeSet<String> exportedNames = new TreeSet<String>(TextUtils.STRING_NUMBER_ORDER);
        TreeSet<String> privateNames = new TreeSet<String>(TextUtils.STRING_NUMBER_ORDER);
        for (int thisNetIndex = 0; thisNetIndex < getNumNetworks(); thisNetIndex++) {
            exportedNames.clear();
            privateNames.clear();
            for (int baseNetIndex = thisNetHead[thisNetIndex]; baseNetIndex >= 0; baseNetIndex = baseNetNext[baseNetIndex]) {
                baseNetlist.fillNames(baseNetIndex, exportedNames, privateNames);
            }
            if (exportedNames.size() > 0 || privateNames.size() > 0) {
                assert isUsernamed(thisNetIndex);
                assert getName(thisNetIndex) == (exportedNames.size() > 0 ? exportedNames : privateNames).iterator().next();
                assert isExported(thisNetIndex) == (exportedNames.size() > 0);
                Iterator<String> eIt = getExportedNames(thisNetIndex);
                Iterator<String> pIt = getNames(thisNetIndex);
                for (String name : exportedNames) {
                    assert eIt.next().equals(name);
                    assert pIt.next().equals(name);
                }
                assert !eIt.hasNext();
                for (String name : privateNames) {
                    if (exportedNames.contains(name)) {
                        continue;
                    }
                    String pName = pIt.next();
                    if (!pName.equals(name)) {
                        int x = 0;
                    }
                    assert pName.equals(name);
                }
                assert !pIt.hasNext();
            } else {
                assert !isUsernamed(thisNetIndex);
                assert !isExported(thisNetIndex);
                boolean hasTempName = false;
                String tempName = getName(thisNetIndex);
                for (int baseNetIndex = thisNetHead[thisNetIndex]; baseNetIndex >= 0; baseNetIndex = baseNetNext[baseNetIndex]) {
                    assert !baseNetlist.isUsernamed(baseNetIndex);
                    if (baseNetlist.getName(baseNetIndex).equals(tempName)) {
                        hasTempName = true;
                    }
                }
                assert hasTempName;
            }
        }
    }
    
    private static class FirstStringCollection extends AbstractCollection<String> {

        private String value;

        @Override
        public int size() {
            return value != null ? 1 : 0;
        }

        @Override
        public void clear() {
            value = null;
        }

        @Override
        public Iterator<String> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(String e) {
            if (value != null && TextUtils.STRING_NUMBER_ORDER.compare(e, value) >= 0) {
                return false;
            }
            value = e;
            return true;
        }
    }

    private static class HasStringCollection extends AbstractCollection<String> {

        private final String value;
        boolean hasString;

        HasStringCollection(String value) {
            this.value = value;
        }

        @Override
        public int size() {
            return hasString ? 1 : 0;
        }

        @Override
        public Iterator<String> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(String e) {
            if (hasString || !value.equals(e)) {
                return false;
            }
            hasString = true;
            return true;
        }
    }
}

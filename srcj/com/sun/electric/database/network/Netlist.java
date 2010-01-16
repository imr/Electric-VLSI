/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Netlist.java
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
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.IconNodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ActivityLogger;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is the Netlist class. It contains information about electric
 * networks of a cell for a given set of options. Networks are 0-based
 * indices. First positions occupies indices of networks connected to
 * global signals, then networks connected to exports, and then local
 * networks.
 */
public abstract class Netlist {

    /** Enumaration defines mode of short resistors in Netlist. */
    public enum ShortResistors {

        /** No resistors are shortened */
        NO,
        /** Resistors are shortened except poly resistors */
        PARASITIC,
        /** All resistors are shortened */
        ALL
    };
    // -------------------------- private data ---------------------------------
    /** NetCell which owns this Netlist. */
    final NetCell netCell;
    final ShortResistors shortResistors;
//    HashMap/*<Cell,Netlist>*/ subNetlists;
    /**
     * The modCount value that the netlist believes that the backing
     * netCell should have.  If this expectation is violated, the netlist
     * has detected concurrent modification.
     */
    WeakReference<Snapshot> expectedSnapshot; // for Schem netlists
    WeakReference<CellTree> expectedCellTree; // for Layout netlisits
//    int expectedModCount;
    /** An equivalence map of PortInsts and NetNames. */
    final int[] netMap;
    final int[] nm_net;
    /** An array of Networks in this Cell. */
    private Network[] networks;
    int numExternalEntries;
    int numExternalNets;

    // ---------------------- package methods -----------------
    /**
     * The constructor of Netlist object..
     */
    Netlist(NetCell netCell, ShortResistors shortResistors, int numExternals, int[] map) {
        this.netCell = netCell;
        this.shortResistors = shortResistors;
//        this.subNetlists = subNetlists;
//        expectedModCount = netCell.modCount;
        netMap = map;
        nm_net = new int[netMap.length];

        closureMap(netMap);
        int k = 0;
        for (int i = 0; i < netMap.length; i++) {
            if (netMap[i] == i) {
                k++;
            }
        }
        networks = new Network[k];

        numExternalEntries = numExternals;
        k = 0;
        for (int i = 0; i < netMap.length; i++) {
            if (netMap[i] == i) {
                nm_net[i] = k;
                k++;
                if (i < numExternals) {
                    numExternalNets++;
                }
            } else {
                nm_net[i] = nm_net[netMap[i]];
            }
        }
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
     */
    static void connectMap(int[] map, int a1, int a2) {
        int m1, m2, m;

        for (m1 = a1; map[m1] != m1; m1 = map[m1]);
        for (m2 = a2; map[m2] != m2; m2 = map[m2]);
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
    }

    /**
     * Obtain canonical representation of equivalence map.
     */
    static void closureMap(int[] map) {
        for (int i = 0; i < map.length; i++) {
            map[i] = map[map[i]];
        }
    }

    /**
     * Obtain canonical representation of equivalence map.
     */
//	private static boolean equalMaps(int[] map1, int[] map2)
//	{
//		if (map1.length != map2.length) return false;
//		for (int i = 0; i < map1.length; i++)
//		{
//			if (map1[i] != map2[i]) return false;
//		}
//		return true;
//	}
    /** A cell of this netlist. */
    public Cell getCell() {
        return netCell.cell;
    }

    /** A net can have multiple names. Return alphabetized list of names. */
    abstract Iterator<String> getNames(int netIndex);

    /** A net can have multiple names. Return alphabetized list of names. */
    abstract Iterator<String> getExportedNames(int netIndex);

    /**
     * Returns most appropriate name of the net.
     * Intitialized net has at least one name - user-defiend or temporary.
     */
    abstract String getName(int netIndex);

    /** Returns true if nm is one of Network's names */
    abstract boolean hasName(int netIndex, String nm);

    /**
     * Add names of this net to two Collections. One for exported, and other for unexported names.
     * @param exportedNames Collection for exported names.
     * @param privateNames Collection for unexported names.
     */
    abstract void fillNames(int netIndex, Collection<String> exportedNames, Collection<String> privateNames);

    /**
     * Method to tell whether this network has any exports or globals on it.
     * @return true if there are exports or globals on this Network.
     */
    boolean isExported(int netIndex) {
        return netIndex < numExternalNets;
    }

    /**
     * Method to tell whether this network has user-defined name.
     * @return true if this Network has user-defined name.
     */
    abstract boolean isUsernamed(int netIndex);

    int getNetIndexByMap(int mapOffset) {
        return nm_net[mapOffset];
    }

    abstract int getEquivPortIndexByNetIndex(int netIndex);

    private final void checkForModification() {
        if (expectedCellTree != null) {
            if (netCell.cell.tree() != expectedCellTree.get() && netCell.obsolete(this)) {
                throw new ConcurrentModificationException();
            }
        } else {
            if (netCell.database.backup() != expectedSnapshot.get() && netCell.obsolete(this)) {
                throw new ConcurrentModificationException();
            }
        }
//        if (expectedModCount != netCell.modCount) {
//            throw new ConcurrentModificationException();
//        }
        netCell.database.checkExamine();
    }

    // ---------------------- public methods -----------------
    // JKG: trying this out
    /**
     * Returns Nodable for given NodeInst and array index.
     * The Nodable is NodeInst itself for primitives and layout subcells.
     * The Nodable is a proxy nodable for icon instances.
     * @param ni node instance
     * @param arrayIndex array index for arrayed icons or zero.
     * @return Nodable for given NodeInst and array index.
     */
    public static Nodable getNodableFor(NodeInst ni, int arrayIndex) {
        Cell parent = ni.getParent();
        if (ni instanceof IconNodeInst) {
            if (ni.isIconOfParent() || arrayIndex < 0 || arrayIndex >= ni.getNameKey().busWidth()) {
                return null;
            }
            return ((IconNodeInst) ni).getNodable(arrayIndex);
        } else {
            return ni;
        }
    }

    /**
     * Get an iterator over all of the Nodables of this Cell.
     * <p> Warning: before getNodables() is called, Networks must be
     * build by calling Cell.rebuildNetworks()
     */
    public Iterator<Nodable> getNodables() {
        checkForModification();
        return netCell.getNodables();
    }

    /**
     * Returns subnetlist for a given Nodable.
     * @param no Nadable in this Netlist
     * @return subnetlist for a given Nidable.
     */
    public Netlist getNetlist(Nodable no) {
        if (!no.isCellInstance()) {
            return null;
        }
        return ((Cell) no.getProto()).getNetlist(shortResistors);
//		return subNetlists.get(no.getProto());
    }

    /**
     * Returns set of global signals in this Netlist.
     * @return set of global signals in this Netlist.
     */
    public Global.Set getGlobals() {
        checkForModification();
        return netCell.getGlobals();
    }

    /**
     * Returns number of drawn networks in this Netlist.
     * Drawn networks are computed from this Cell only.
     * They doesn't consider buses or shortcuts in subcells.
     * @return set of number of drawn networks in this Netlist.
     */
    public int getNumDrawns() {
        return netCell.numDrawns;
    }

    /**
     * Get number of networks in this Netlist.
     * @return number of networks in this Netlist.
     */
    public int getNumNetworks() {
        checkForModification();
        return networks.length;
    }

    /**
     * Get number of networks in this Netlist, which are
     * connected to exports or globals.
     * @return number of external networks in this Netlist.
     */
    public int getNumExternalNetworks() {
        checkForModification();
        return numExternalNets;
    }

    /**
     * Get Network with specified index.
     * @param netIndex index of Network
     * @return Network with specified index.
     */
    public Network getNetwork(int netIndex) {
        checkForModification();
        return getNetworkRaw(netIndex);
    }

    /**
     * Get Network with specified index.
     * If Network was not allocated yet, allocate it.
     * @param netIndex index of Network
     * @return Network with specified index.
     */
    private Network getNetworkRaw(int netIndex) {
        if (netIndex < 0) {
            return null;
        }
        Network network = networks[netIndex];
        if (network != null) {
            return network;
        }
        network = new Network(this, netIndex);

        // Check if concurrent thread allocated this network also
        // This code is not properly synchronized !!!!!!
        if (networks[netIndex] != null) {
            return networks[netIndex];
        }
        networks[netIndex] = network;
        return network;
    }

    /**
     * Get an iterator over all of the Networks of this Netlist.
     */
    public Iterator<Network> getNetworks() {
        checkForModification();
        for (int netIndex = 0; netIndex < networks.length; netIndex++) {
            if (networks[netIndex] == null) {
                getNetworkRaw(netIndex);
            }
        }
        return Arrays.asList(networks).iterator();
    }

    /**
     * Get net index of a global signal.
     * @param global global signal.
     * @return net index of a gloabal signal.
     */
    int getNetIndex(Global global) {
        checkForModification();
        int netMapIndex = netCell.getNetMapOffset(global);
        if (netMapIndex < 0) {
            return -1;
        }
        return nm_net[netMapIndex];
    }

    /**
     * Get net index of a global signal of nodable.
     * @param no nodable.
     * @param global global signal.
     * @return net index of a global signal of nodable.
     */
    int getNetIndex(Nodable no, Global global) {
        checkForModification();
        int netMapIndex = netCell.getNetMapOffset(no, global);
        if (netMapIndex < 0) {
            return -1;
        }
        return nm_net[netMapIndex];
    }

    /**
     * Get net index of signal connected to specified external network of nodable.
     * Nodable must be subcell
     * @param no nodable (subcell)
     * @param subNetwork a network
     * @return network.
     */
    int getNetIndex(Nodable no, Network subNetwork) {
        checkForModification();
        Netlist subNetlist = subNetwork.getNetlist();
        assert subNetlist.shortResistors == shortResistors;
        if (no.getParent() == netCell.cell && subNetlist.getCell() == no.getProto() && subNetwork.isExported()) {
            int equivPortIndex = subNetlist.getEquivPortIndexByNetIndex(subNetwork.getNetIndex());
            assert equivPortIndex >= 0 && equivPortIndex < subNetlist.netCell.equivPortsN.length;
            int netMapIndex = netCell.getNetMapOffset(no, equivPortIndex);
            if (netMapIndex >= 0) {
                return nm_net[netMapIndex];
            }
        }
        return -1;
    }

    /**
     * Get net index of signal in a port instance of nodable.
     * @param no nodable
     * @param portProto port of nodable
     * @param busIndex index of signal in a bus or zero.
     * @return net index
     */
    int getNetIndex(Nodable no, PortProto portProto, int busIndex) {
        checkForModification();
//        if (no instanceof IconNodeInst) {
//            Nodable no1 = ((IconNodeInst)no).getNodable(0);
//            if (Job.getDebug()) {
//                System.out.println("IconNodeInst " + no + " is passed to getNodeIndex. Replaced by IconNodeable " + no1);
//            }
//            if (no1.getProto() != no.getProto()) {
//                portProto = ((Export)portProto).getEquivalent();
//                if (portProto == null) {
//                    return -1;
//                }
//            }
//            no = no1;
//        }
        if (no.getParent() != netCell.cell) {
            return -1;
        }
        if (portProto.getParent() != no.getProto()) {
            System.out.println("Netlist.getNetwork: invalid argument portProto");
            return -1;
        }
        if (busIndex < 0 || busIndex >= netCell.getBusWidth(no, portProto)) {
// 			System.out.println("Nodable.getNetwork: invalid arguments busIndex="+busIndex+" portProto="+portProto);
            return -1;
        }
        int netMapIndex = netCell.getNetMapOffset(no, portProto, busIndex);
        if (netMapIndex < 0) {
            return -1;
        }
        return nm_net[netMapIndex];
    }

    /**
     * Get net index of signal in export.
     * @param export given Export.
     * @param busIndex index of signal in a bus or zero.
     * @return net index.
     */
    int getNetIndex(Export export, int busIndex) {
        checkForModification();
        if (export.getParent() != netCell.cell) {
            return -1;
        }
        if (busIndex < 0 || busIndex >= export.getNameKey().busWidth()) {
            System.out.println("Nodable.getNetwork: invalid arguments busIndex=" + busIndex + " export=" + export);
            return -1;
        }
        int netMapIndex = netCell.getNetMapOffset(export, busIndex);
        int netIndex = netMapIndex >= 0 ? nm_net[netMapIndex] : -1;
        if (Job.getDebug()) {
            Name exportName = export.getNameKey().subname(busIndex);
            if (netIndex != getNetIndex(exportName)) {
                String msg = "Export Name network mismatch in Cell " + netCell.cell.libDescribe()+
                        ": getNetIndex("+export+","+busIndex+")="+netIndex+
                        " getNetIndex("+exportName+")"+"="+getNetIndex(exportName);
                System.out.println(msg);
                ActivityLogger.logException(new AssertionError(msg));
            }
        }
        return netIndex;
    }

    /**
     * Get net index of signal in export name.
     * @param exportName given Export name.
     * @return net index.
     */
    int getNetIndex(Name exportName) {
        checkForModification();
        if (exportName.isBus()) {
            throw new IllegalArgumentException("Scalar export name expected");
        }
        int netMapIndex = netCell.getNetMapOffset(exportName);
        if (netMapIndex < 0) {
            return -1;
        }
        return nm_net[netMapIndex];
    }

    /**
     * Get net index of signal on arc.
     * @param ai arc instance
     * @param busIndex index of signal in a bus or zero.
     * @return net index.
     */
    int getNetIndex(ArcInst ai, int busIndex) {
        checkForModification();
        if (ai.getParent() != netCell.cell) {
            return -1;
        }
        int netMapIndex = netCell.getNetMapOffset(ai, busIndex);
        if (netMapIndex < 0) {
            return -1;
        }
        return nm_net[netMapIndex];
    }

    /**
     * Get network of a global signal.
     * @param global global signal.
     * @return net index of a gloabal signal.
     */
    public Network getNetwork(Global global) {
        return getNetworkRaw(getNetIndex(global));
    }

    /**
     * Get network of a global signal of nodable.
     * @param no nodable.
     * @param global global signal.
     * @return net index of a gloabal signal of nodable.
     */
    public Network getNetwork(Nodable no, Global global) {
        return getNetworkRaw(getNetIndex(no, global));
    }

    /**
     * Get network of signal connected to specified external network of nodable.
     * Nodable must be subcell
     * @param no nodable (subcell)
     * @param subNetwork a network
     * @return network.
     * @throws IllegalArgumentException if nodable is not subcell
     */
    public Network getNetwork(Nodable no, Network subNetwork) {
        return getNetworkRaw(getNetIndex(no, subNetwork));
    }

    /**
     * Get network of signal in a port instance of nodable.
     * @param no nodable
     * @param portProto port of nodable
     * @param busIndex index of signal in a bus or zero.
     * @return network.
     */
    public Network getNetwork(Nodable no, PortProto portProto, int busIndex) {
        if (no == null || portProto == null) {
            return null;
        }
        if (no instanceof NodeInst && !((NodeInst) no).isLinked()) {
            return null;
        }
        if (portProto instanceof Export && !((Export) portProto).isLinked()) {
            return null;
        }
        return getNetworkRaw(getNetIndex(no, portProto, busIndex));
    }

    /**
     * Method to tell whether two PortProtos are electrically connected.
     * @param no the Nodable on which the PortProtos reside.
     * @param port1 the first PortProto.
     * @param port2 the second PortProto.
     * @return true if the two PortProtos are electrically connected.
     */
    public boolean portsConnected(Nodable no, PortProto port1, PortProto port2) {
        if (no == null || port1 == null || port2 == null) {
            return false;
        }
        if (no instanceof NodeInst && !((NodeInst) no).isLinked()) {
            return false;
        }
        if (port1 instanceof Export && !((Export) port1).isLinked()) {
            return false;
        }
        if (port2 instanceof Export && !((Export) port2).isLinked()) {
            return false;
        }
        int busWidth = netCell.getBusWidth(no, port1);
        if (netCell.getBusWidth(no, port2) != busWidth) {
            return false;
        }
        for (int i = 0; i < busWidth; i++) {
            if (getNetIndex(no, port1, i) != getNetIndex(no, port2, i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get network of port instance.
     * @param pi port instance.
     * @return signal on port index or null.
     */
    public Network getNetwork(PortInst pi) {
        if (!pi.isLinked()) {
            return null;
        }
        PortProto portProto = pi.getPortProto();
        if (portProto.getNameKey().isBus()) {
            System.out.println("PortInst.getNetwork() was called for instance of bus port " + portProto.getName());
            return null;
        }
        return getNetwork(pi.getNodeInst(), portProto, 0);
    }

    /**
     * Return a map from Networks to array of their PortInsts.
     * Works only for layout cells.
     * @return a map from Networks to array of their PortInsts.
     * @throws IllegalArgumentException for schematic Netlists
     */
    public Map<Network, PortInst[]> getPortInstsByNetwork() {
        if (netCell instanceof NetSchem) {
            throw new IllegalArgumentException();
        }
        Cell cell = netCell.cell;
        int[] networkCounts = new int[getNumNetworks()];
        for (Iterator<NodeInst> nit = cell.getNodes(); nit.hasNext();) {
            NodeInst ni = nit.next();
            for (Iterator<PortInst> pit = ni.getPortInsts(); pit.hasNext();) {
                PortInst pi = pit.next();
                Network net = getNetwork(pi);
                if (net == null) {
                    continue;
                }
                networkCounts[net.getNetIndex()]++;
            }
        }
        LinkedHashMap<Network, PortInst[]> map = new LinkedHashMap<Network, PortInst[]>();
        PortInst[][] portInsts = new PortInst[getNumNetworks()][];
        for (int netIndex = 0; netIndex < getNumNetworks(); netIndex++) {
            map.put(getNetwork(netIndex), portInsts[netIndex] = new PortInst[networkCounts[netIndex]]);
        }
        Arrays.fill(networkCounts, 0);
        for (Iterator<NodeInst> nit = cell.getNodes(); nit.hasNext();) {
            NodeInst ni = nit.next();
            for (Iterator<PortInst> pit = ni.getPortInsts(); pit.hasNext();) {
                PortInst pi = pit.next();
                Network net = getNetwork(pi);
                if (net == null) {
                    continue;
                }
                int netIndex = net.getNetIndex();
                portInsts[netIndex][networkCounts[netIndex]++] = pi;
            }
        }
        return map;
    }

    /**
     * Return a map from Networks to array of their ArcInsts.
     * Works only for layout cells.
     * @return a map from Networks to array of their ArcInsts.
     * @throws IllegalArgumentException for schematic Netlists
     */
    public Map<Network, ArcInst[]> getArcInstsByNetwork() {
        if (netCell instanceof NetSchem) {
            throw new IllegalArgumentException();
        }
        Cell cell = netCell.cell;
        int[] networkCounts = new int[getNumNetworks()];
        for (Iterator<ArcInst> ait = cell.getArcs(); ait.hasNext();) {
            ArcInst ai = ait.next();
            Network net = getNetwork(ai, 0);
            if (net == null) {
                continue;
            }
            networkCounts[net.getNetIndex()]++;
        }
        LinkedHashMap<Network, ArcInst[]> map = new LinkedHashMap<Network, ArcInst[]>();
        ArcInst[][] arcInsts = new ArcInst[getNumNetworks()][];
        for (int netIndex = 0; netIndex < getNumNetworks(); netIndex++) {
            int numArcs = networkCounts[netIndex];
            ArcInst[] arcs = numArcs > 0 ? new ArcInst[numArcs] : ArcInst.NULL_ARRAY;
            map.put(getNetwork(netIndex), arcs);
            arcInsts[netIndex] = arcs;
        }
        Arrays.fill(networkCounts, 0);
        for (Iterator<ArcInst> ait = cell.getArcs(); ait.hasNext();) {
            ArcInst ai = ait.next();
            Network net = getNetwork(ai, 0);
            if (net == null) {
                continue;
            }
            int netIndex = net.getNetIndex();
            arcInsts[netIndex][networkCounts[netIndex]++] = ai;
        }
        return map;
    }

    /**
     * Get network of signal in export.
     * @param export given Export.
     * @param busIndex index of signal in a bus or zero.
     * @return network.
     */
    public Network getNetwork(Export export, int busIndex) {
        if (!export.isLinked()) {
            return null;
        }
        return getNetworkRaw(getNetIndex(export, busIndex));
    }

    /**
     * Get network of signal in export name.
     * @param exportName given Export name.
     * @return network.
     */
    public Network getNetwork(Name exportName) {
        return getNetworkRaw(getNetIndex(exportName));
    }

    /**
     * Get network of signal on arc.
     * @param ai arc instance
     * @param busIndex index of signal in a bus or zero.
     * @return network.
     */
    public Network getNetwork(ArcInst ai, int busIndex) {
        if (!ai.isLinked()) {
            return null;
        }
        return getNetworkRaw(getNetIndex(ai, busIndex));
    }

    /**
     * Method to tell whether two ArcInsts are electrically equivalent.
     * This considers individual elements of busses.
     * @param ai1 the first ArcInst.
     * @param ai2 the second ArcInst.
     * @return true if the arcs are electrically connected.
     */
    public boolean sameNetwork(ArcInst ai1, ArcInst ai2) {
        if (ai1 == null || ai2 == null) {
            return false;
        }
        if (!ai1.isLinked()) {
            return false;
        }
        if (!ai2.isLinked()) {
            return false;
        }

        checkForModification();
        int busWidth1 = netCell.getBusWidth(ai1);
        int busWidth2 = netCell.getBusWidth(ai2);
        if (busWidth1 != busWidth2) {
            return false;
        }
        for (int i = 0; i < busWidth1; i++) {
            if (getNetIndex(ai1, i) != getNetIndex(ai2, i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method to tell whether a PortProto on a Nodable is electrically equivalent to an ArcInst.
     * This considers individual elements of busses.
     * @param no the Nodable.
     * @param pp the PortProto on the Nodable.
     * @param ai the ArcInst.
     * @return true if they are electrically connected.
     */
    public boolean sameNetwork(Nodable no, PortProto pp, ArcInst ai) {
        if (no == null || pp == null || ai == null) {
            return false;
        }
        if (no instanceof NodeInst && !((NodeInst) no).isLinked()) {
            return false;
        }
        if (pp instanceof Export && !((Export) pp).isLinked()) {
            return false;
        }
        if (!ai.isLinked()) {
            return false;
        }
        checkForModification();
        int busWidth1 = netCell.getBusWidth(no, pp);
        int busWidth2 = netCell.getBusWidth(ai);
        if (busWidth1 != busWidth2) {
            return false;
        }
        for (int i = 0; i < busWidth1; i++) {
            if (getNetIndex(no, pp, i) != getNetIndex(ai, i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method to tell whether two PortProto / Nodable pairs are electrically equivalent.
     * This considers individual elements of busses.
     * @param no1 the first Nodable.
     * @param pp1 the PortProto on the first Nodable.
     * @param no2 the second Nodable.
     * @param pp2 the PortProto on the second Nodable.
     * @return true if they are electrically connected.
     */
    public boolean sameNetwork(Nodable no1, PortProto pp1, Nodable no2, PortProto pp2) {
        if (no1 == null || pp1 == null || no2 == null || pp2 == null) {
            return false;
        }
        if (no1 instanceof NodeInst && !((NodeInst) no1).isLinked()) {
            return false;
        }
        if (pp1 instanceof Export && !((Export) pp1).isLinked()) {
            return false;
        }
        if (no2 instanceof NodeInst && !((NodeInst) no2).isLinked()) {
            return false;
        }
        if (pp2 instanceof Export && !((Export) pp2).isLinked()) {
            return false;
        }
        int busWidth1 = netCell.getBusWidth(no1, pp1);
        int busWidth2 = netCell.getBusWidth(no2, pp2);
        if (busWidth1 != busWidth2) {
            return false;
        }
        for (int i = 0; i < busWidth1; i++) {
            if (getNetIndex(no1, pp1, i) != getNetIndex(no2, pp2, i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method to return either the network name or the bus name on this ArcInst.
     * @return the either the network name or the bus name on this ArcInst.
     */
    public String getNetworkName(ArcInst ai) {
        if (ai == null || !ai.isLinked()) {
            return null;
        }
        checkForModification();
        if (ai.getParent() != netCell.cell) {
            return null;
        }
        int busWidth = netCell.getBusWidth(ai);
        if (busWidth > 1) {
            return netCell.getBusName(ai).toString();
        }
        Network network = getNetwork(ai, 0);
        if (network == null) {
            return null;
        }
        return network.describe(false);
    }

    /**
     * Method to return the name of the bus on this ArcInst.
     * @return the name of the bus on this ArcInst.
     */
    public Name getBusName(ArcInst ai) {
        if (ai == null || !ai.isLinked()) {
            return null;
        }
        checkForModification();
        if (ai.getParent() != netCell.cell) {
            return null;
        }
        int busWidth = netCell.getBusWidth(ai);
        if (busWidth <= 1) {
            return null;
        }
        return netCell.getBusName(ai);
    }

    /**
     * Method to return the bus width on an Export.
     * @param e the Export to examine.
     * @return the bus width of the Export.
     */
    public int getBusWidth(Export e) {
        if (e == null || !e.isLinked()) {
            return 0;
        }
        return e.getNameKey().busWidth();
    }

    /**
     * Method to return the bus width on this ArcInst.
     * @return the either the bus width on this ArcInst.
     */
    public int getBusWidth(ArcInst ai) {
        if (ai == null || !ai.isLinked()) {
            return 0;
        }
        checkForModification();
        if (ai.getParent() != netCell.cell) {
            return 0;
        }
        return netCell.getBusWidth(ai);
    }

    public ShortResistors getShortResistors() {
        return shortResistors;
    }

    /**
     * Returns a printable version of this Netlist.
     * @return a printable version of this Netlist.
     */
    @Override
    public String toString() {
        return "Netlist of " + netCell.cell;
    }
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Netlist.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.network;

import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * This is the Netlist class. It contains information about electric
 * networks of a cell for a given set of options. Networks are 0-based
 * indices. First positions occupies indices of networks connected to
 * global signals, then networks connected to exports, and then local
 * networks.
 */
public class Netlist
{

	// -------------------------- private data ---------------------------------

	/** NetCell which owns this Netlist. */
	NetCell netCell;

	/**
	 * The modCount value that the netlist believes that the backing
	 * netCell should have.  If this expectation is violated, the netlist
	 * has detected concurrent modification.
	 */
	int expectedModCount;

	/** An equivalence map of PortInsts and NetNames. */
	int[] netMap;
	int[] nm_net;

	/** An array of Networks in this Cell. */
	private Network[] networks;

	// ---------------------- package methods -----------------

	/**
	 * The constructor of Netlist object..
	 */
	Netlist(NetCell netCell) {
		this.netCell = netCell;
		expectedModCount = netCell.modCount;
	}

	void initNetMap(int size) {
		if (netMap == null || netMap.length != size) {
			netMap = new int[size];
			nm_net = new int[size];
		}
		for (int i = 0; i < netMap.length; i++) netMap[i] = i;
	}

	void initNetworks() {
		closureMap(netMap);
		int k = 0;
		for (int i = 0; i < netMap.length; i++) {
			if (netMap[i] == i) k++;
		}
		if (networks == null || networks.length != k) {
			networks = new Network[k];
		}
		Arrays.fill(networks, null);
		
		k = 0;
		for (int i = 0; i < netMap.length; i++) {
			if (netMap[i] == i) {
				networks[k] = new Network(this, k);
				nm_net[i] = k;
				k++;
			} else {
				nm_net[i] = nm_net[netMap[i]];
			}
		}
	}

	/**
	 * Merge classes of equivalence map to which elements a1 and a2 belong.
	 */
	final void connectMap(int a1, int a2) {
		//System.out.println("connectMap "+a1+" "+a2);
		Netlist.connectMap(netMap, a1, a2);
	}

	/**
	 * Merge classes of equivalence map to which elements a1 and a2 belong.
	 */
	private static void connectMap(int[] map, int a1, int a2)
	{
		int m1, m2, m;

		for (m1 = a1; map[m1] != m1; m1 = map[m1]);
		for (m2 = a2; map[m2] != m2; m2 = map[m2]);
		m = m1 < m2 ? m1 : m2;

		for (;;)
		{
			int k = map[a1];
			map[a1] = m;
			if (a1 == k) break;
			a1 = k;
		}
		for (;;)
		{
			int k = map[a2];
			map[a2] = m;
			if (a2 == k) break;
			a2 = k;
		}
	}

	/**
	 * Obtain canonical representation of equivalence map.
	 */
	private static void closureMap(int[] map)
	{
		for (int i = 0; i < map.length; i++)
		{
			map[i] = map[map[i]];
		}
	}

	/**
	 * Obtain canonical representation of equivalence map.
	 */
	private static boolean equalMaps(int[] map1, int[] map2)
	{
		if (map1.length != map2.length) return false;
		for (int i = 0; i < map1.length; i++)
		{
			if (map1[i] != map2[i]) return false;
		}
		return true;
	}

	Network getNetworkByMap(int mapOffset) { return networks[nm_net[mapOffset]]; }

	private final void checkForModification() {
		if (expectedModCount != netCell.modCount)
			throw new ConcurrentModificationException();
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
        Netlist netlist = NetworkTool.getUserNetlist(parent);
        for (Iterator it = netlist.getNodables(); it.hasNext(); ) {
            Nodable no = (Nodable)it.next();
            if (no.contains(ni, arrayIndex)) return no;
        }
        return null;
    }

	/**
	 * Get an iterator over all of the Nodables of this Cell.
	 * <p> Warning: before getNodables() is called, Networks must be
	 * build by calling Cell.rebuildNetworks()
	 */
	public Iterator getNodables() {
		checkForModification();
		return netCell.getNodables();
	}

	/**
	 * Returns subnetlist for a given Nodable.
	 * @param no Nadable in this Netlist
	 * @return subnetlist for a given Nidable.
	 */
	public Netlist getNetlist(Nodable no) {
		NodeProto np = no.getProto();
		if (!(np instanceof Cell)) return null;
		return NetworkTool.getUserNetlist((Cell)np);
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
	 * Get number of networks in this Netlist.
	 * @return number of networks in this Netlist.
	 */
	public int getNumNetworks() {
		checkForModification();
		return networks.length;
	}

	/**
	 * Get Network with specified index.
	 * @param netIndex index of Network
	 * @return Network with specified index.
	 */
	public Network getNetwork(int netIndex) {
		checkForModification();
		return networks[netIndex];
	}

	/**
	 * Get an iterator over all of the Networks of this Netlist.
	 */
	public Iterator getNetworks() {
		checkForModification();
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
		if (netMapIndex < 0) return -1;
		return nm_net[netMapIndex];
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
		if (no.getParent() != netCell.cell)
			return -1;
		if (portProto.getParent() != no.getProto())
		{
			System.out.println("Netlist.getNetwork: invalid argument portProto");
			return -1;
		}
// 		if (busIndex < 0 || busIndex >= portProto.getNameKey().busWidth())
// 		{
// 			System.out.println("Nodable.getNetwork: invalid arguments busIndex="+busIndex+" portProto="+portProto);
// 			return -1;
// 		}
		int netMapIndex = netCell.getNetMapOffset(no, portProto, busIndex);
		if (netMapIndex < 0) return -1;
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
		if (export.getParent() != netCell.cell) return -1;
		if (busIndex < 0 || busIndex >= export.getNameKey().busWidth())
		{
			System.out.println("Nodable.getNetwork: invalid arguments busIndex="+busIndex+" export="+export);
			return -1;
		}
		int netMapIndex = netCell.getNetMapOffset(export, busIndex);
		if (netMapIndex < 0) return -1;
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
		if (ai.getParent() != netCell.cell) return -1;
		int netMapIndex = netCell.getNetMapOffset(ai, busIndex);
		if (netMapIndex < 0) return -1;
		return nm_net[netMapIndex];
	}

	/**
	 * Get network of a global signal.
	 * @param global global signal.
	 * @return net index of a gloabal signal.
	 */
	public Network getNetwork(Global global) {
		int netIndex = getNetIndex(global);
		if (netIndex < 0) return null;
		return networks[netIndex];
	}

	/**
	 * Get network of signal in a port instance of nodable.
	 * @param no nodable 
	 * @param portProto port of nodable
	 * @param busIndex index of signal in a bus or zero.
	 * @return network.
	 */
	public Network getNetwork(Nodable no, PortProto portProto, int busIndex) {
		int netIndex = getNetIndex(no, portProto, busIndex);
		if (netIndex < 0) return null;
		return networks[netIndex];
	}

	/**
	 * Method to tell whether two PortProtos are electrically connected.
	 * @param no the Nodable on which the PortProtos reside.
	 * @param port1 the first PortProto.
	 * @param port2 the second PortProto.
	 * @return true if the two PortProtos are electrically connected.
	 */
	public boolean portsConnected(Nodable no, PortProto port1, PortProto port2)
	{
		int busWidth = port1.getNameKey().busWidth();
		if (port2.getNameKey().busWidth() != busWidth) return false;
		for (int i = 0; i < busWidth; i++) {
			if (getNetIndex(no, port1, i) != getNetIndex(no, port2, i))
				return false;
		}
		return true;
	}

	/**
	 * Get network of port instance.
	 * @param pi port instance.
	 * @return signal on port index or null.
	 */
	public Network getNetwork(PortInst pi) {
		PortProto portProto = pi.getPortProto();
		if (portProto.getNameKey().isBus())
		{
			System.out.println("PortInst.getNetwork() was called for instance of bus port "+portProto.getName());
			return null;
		}
		return getNetwork(pi.getNodeInst(), portProto, 0);
	}

	/**
	 * Get network of signal in export.
	 * @param export given Export.
	 * @param busIndex index of signal in a bus or zero.
	 * @return network.
	 */
	public Network getNetwork(Export export, int busIndex) {
		int netIndex = getNetIndex(export, busIndex);
		if (netIndex < 0) return null;
		return networks[netIndex];
	}

	/**
	 * Get network of signal on arc.
	 * @param ai arc instance
	 * @param busIndex index of signal in a bus or zero.
	 * @return network.
	 */
	public Network getNetwork(ArcInst ai, int busIndex) {
		int netIndex = getNetIndex(ai, busIndex);
		if (netIndex < 0) return null;
		return networks[netIndex];
	}

	/**
	 * Method to tell whether two ArcInsts are electrically equivalent.
	 * This considers individual elements of busses.
	 * @param ai1 the first ArcInst.
	 * @param ai2 the second ArcInst.
	 * @return true if the arcs are electrically connected.
	 */
	public boolean sameNetwork(ArcInst ai1, ArcInst ai2)
	{
		if (ai1 == null || ai2 == null) return false;
		int busWidth1 = netCell.getBusWidth(ai1);
		int busWidth2 = netCell.getBusWidth(ai2);
		if (busWidth1 != busWidth2) return false;
		for(int i=0; i<busWidth1; i++)
		{
			if (getNetIndex(ai1, i) != getNetIndex(ai2, i))
				return false;
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
	public boolean sameNetwork(Nodable no, PortProto pp, ArcInst ai)
	{
		if (no == null || pp == null || ai == null) return false;
		int busWidth1 = pp.getNameKey().busWidth();
		int busWidth2 = netCell.getBusWidth(ai);
		if (busWidth1 != busWidth2) return false;
		for(int i=0; i<busWidth1; i++)
		{
			if (getNetIndex(no, pp, i) != getNetIndex(ai, i))
				return false;
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
	public boolean sameNetwork(Nodable no1, PortProto pp1, Nodable no2, PortProto pp2)
	{
		if (no1 == null || pp1 == null || no2 == null || pp2 == null) return false;
		int busWidth1 = pp1.getNameKey().busWidth();
		int busWidth2 = pp2.getNameKey().busWidth();
		if (busWidth1 != busWidth2) return false;
		for(int i=0; i<busWidth1; i++)
		{
			if (getNetIndex(no1, pp1, i) != getNetIndex(no2, pp2, i))
				return false;
		}
		return true;
	}

	/**
	 * Method to return either the network name or the bus name on this ArcInst.
	 * @return the either the network name or the bus name on this ArcInst.
	 */
	public String getNetworkName(ArcInst ai) {
		checkForModification();
		if (ai.getParent() != netCell.cell) return null;
		int busWidth = netCell.getBusWidth(ai);
		if (busWidth > 1) {
			return netCell.getBusName(ai).toString();
		}
		Network network = getNetwork(ai, 0);
		if (network == null) return null;
		return network.describe();
	}

	/**
	 * Method to return the name of the bus on this ArcInst.
	 * @return the name of the bus on this ArcInst.
	 */
	public Name getBusName(ArcInst ai) {
		checkForModification();
		if (ai.getParent() != netCell.cell) return null;
		int busWidth = netCell.getBusWidth(ai);
		if (busWidth <= 1) return null;
		return netCell.getBusName(ai);
	}

	/**
	 * Method to return the bus width on an Export.
	 * @param e the Export to examine.
	 * @return the bus width of the Export.
	 */
	public int getBusWidth(Export e)
	{
		return e.getNameKey().busWidth();
	}

	/**
	 * Method to return the bus width on this ArcInst.
	 * @return the either the bus width on this ArcInst.
	 */
	public int getBusWidth(ArcInst ai) {
		checkForModification();
		if (ai.getParent() != netCell.cell) return 0;
		return netCell.getBusWidth(ai);
	}

	/**
	 * Returns a printable version of this Netlist.
	 * @return a printable version of this Netlist.
	 */
	public String toString()
	{
		return "Netlist of " + netCell.cell;
	}

}

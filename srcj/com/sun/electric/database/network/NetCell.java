/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetCell.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This is the Cell mirror in Network tool.
 */
class NetCell
{
	/**
	 * An NetName class represents possible net name in a cell.
	 * NetName is obtainsed either from Export name or ArcInst name.
	 */
	static class NetName
	{
		Name name;
		int index;
		NetName() { index = -1; }
	}

	/** If bit set, netlist is valid for cell tree.*/				static final int VALID = 1;
	/** If bit set, netlist is valid with current  equivPorts of subcells.*/static final int LOCALVALID = 2;

	/** Cell from database. */										final Cell cell;
	/** Flags of this NetCell. */									int flags;

	/** Exports of original cell. */								Export[] dbExports;
	/**
     * Equivalence of ports. equivPorts.size == ports.size.
	 * equivPorts[i] contains minimal index among ports of its group.
     */																int[] equivPorts;
	/** Node offsets. */											private int[] ni_pi;
	/** */															int arcsOffset;
	/** */															private int[] headConn;
	/** */															private int[] tailConn;
	/** */															int[] drawns;

	/** A map from Name to NetName. */								Map netNames = new HashMap();
	/** Counter for enumerating NetNames. */						private int netNameCount;
	
	/** An equivalence map of PortInsts and NetNames. */			int[] netMap;
	/** An array of JNetworks in this Cell. */						JNetwork[] networks;

	/** */															private static PortProto busPinPort = Schematics.tech.busPinNode.getPort(0);
	/** */															private static ArcProto busArc = Schematics.tech.bus_arc;
	/**
	 * The constructor is not used.
	 */
	NetCell() { cell = null; }

	NetCell(Cell cell)
	{
		this.cell = cell;
		if (cell != null) {
			Network.setCell(cell, this, 0);
		} else {
			dbExports = new Export[0];
			equivPorts = new int[0];
		}
	}

	final void setNetworksDirty()
	{
		if ((flags & LOCALVALID) != 0)
			setInvalid(true);
	}

	/**
	 * Get an iterator over all of the Nodables of this Cell.
	 */
	Iterator getNodables()
	{
		if ((flags & VALID) == 0) redoNetworks();
		return cell.getNodes();
	}

	/**
	 * Get an iterator over all of the JNetworks of this Cell.
	 */
	Iterator getNetworks()
	{
		if ((flags & VALID) == 0) redoNetworks();
		ArrayList nets = new ArrayList();
		for (int i = 0; i < networks.length; i++)
		{
			if (networks[i] != null)
				nets.add(networks[i]);
		}
		return nets.iterator();
	}

	void setInvalid(boolean strong)
	{
		if (strong) flags &= ~LOCALVALID;
		if ((flags & VALID) == 0) return;
		flags &= ~VALID;
		invalidateUsagesOf(false);
	}

	void invalidateUsagesOf(boolean strong) { invalidateUsagesOf(cell, strong); }

	static void invalidateUsagesOf(Cell cell, boolean strong) {
		for (Iterator it = cell.getUsagesOf(); it.hasNext();) {
			NodeUsage nu = (NodeUsage)it.next();
			if (nu.isIconOfParent()) continue;
			Network.getNetCell(nu.getParent()).setInvalid(strong);
		}
	}

	/**
	 * Redo subcells of this cell. Ifmap of equivalent ports of some subcell has
     * been updated since last network renumbering, then retunr true.
	 */
// 	private void redoDescendents()
// 	{
// 		for (Iterator it = cell.getUsagesIn(); it.hasNext();)
// 		{
// 			NodeUsage nu = (NodeUsage) it.next();
// 			if (nu.isIconOfParent()) continue;

// 			NodeProto np = nu.getProto();
// 			int npInd = np.getIndex();
// 			if (npInd < 0) continue;
// 			NetCell netCell = Network.cells[npInd];
// 			if ((netCell.flags & VALID) != 0)
// 				netCell.redoNetworks();
// 		}
// 	}

	/*
	 * Get network by index in networks maps.
	 */
	JNetwork getNetwork(Nodable no, int arrayIndex, PortProto portProto, int busIndex) {
		if ((flags & VALID) == 0) redoNetworks();
		NodeInst ni = (NodeInst)no;
		int drawn = drawns[ni_pi[ni.getNodeIndex()] + portProto.getPortIndex()];
		if (drawn < 0) return null;
		return networks[netMap[drawn]];
	}

	/*
	 * Get network of export.
	 */
	JNetwork getNetwork(Export export, int busIndex) {
		if ((flags & VALID) == 0) redoNetworks();
		int drawn = drawns[export.getPortIndex()];
		if (drawn < 0) return null;
		return networks[netMap[drawn]];
	}

	/*
	 * Get network of arc.
	 */
	JNetwork getNetwork(ArcInst ai, int busIndex) {
		if ((flags & VALID) == 0) redoNetworks();
		int drawn = drawns[arcsOffset + ai.getArcIndex()];
		if (drawn < 0) return null;
		return networks[netMap[drawn]];
	}

	/**
	 * Routine to return either the network name or the bus name on this ArcInst.
	 * @return the either the network name or the bus name on this ArcInst.
	 */
	String getNetworkName(ArcInst ai) {
		if ((flags & VALID) == 0) redoNetworks();
		int drawn = drawns[arcsOffset + ai.getArcIndex()];
		if (drawn < 0) return null;
		return networks[netMap[drawn]].describe();
	}

	/**
	 * Routine to return the bus width on this ArcInst.
	 * @return the either the bus width on this ArcInst.
	 */
	public int getBusWidth(ArcInst ai)
	{
		if ((flags & VALID) == 0) redoNetworks();
		int drawn = drawns[arcsOffset + ai.getArcIndex()];
		if (drawn < 0) return 0;
		return 1;
	}

	/**
	 * Check if cell has changed exports.
	 * @return true if this cell has changed exports.
	 */
	boolean updateExports() {
		boolean changed = false;
		if (dbExports == null || dbExports.length != cell.getNumPorts()) {
			changed = true;
			dbExports = new Export[cell.getNumPorts()];
			}
		for (Iterator it = cell.getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			int portIndex = e.getPortIndex();
			if (dbExports[portIndex] != e) {
				changed = true;
				dbExports[portIndex] = e;
			}
		}
		return changed;
	}          

	private void checkLayoutCell() {
		int numNodes = cell.getNumNodes();
		for (int i = 0; i < numNodes; i++) {
			NodeInst ni = (NodeInst)cell.getNode(i);
			if (ni.getNameKey().isBus())
				System.out.println("Network: Layout cell " + cell.describe() + " has arrayed node " + ni.describe());
		}
		for (Iterator it = cell.getUsagesIn(); it.hasNext();) {
			NodeUsage nu = (NodeUsage)it.next();
			NodeProto np = nu.getProto();
			boolean err = false;
			if (isSchem(np)) {
				System.out.println("Network: Layout cell " + cell.describe() + " has " + nu.getNumInsts() +
					" " + np.describe() + " nodes");
				err = true;
			}
			if (np == Generic.tech.universalPinNode) {
				System.out.println("Network: Layout cell " + cell.describe() + " has " + nu.getNumInsts() +
					" " + np.describe() + " nodes");
				err = true;
			}
			if (np instanceof PrimitiveNode) {
				if (np.getTechnology() == Schematics.tech) {
					System.out.println("Network: Layout cell " + cell.describe() + " has " + nu.getNumInsts() +
						" " + np.describe() + " nodes");
					err = true;
				}
			}
		}

		int numArcs = cell.getNumArcs();
		ArcProto universalArc = Generic.tech.universal_arc;
		int numUniversalArcs = 0;
		ArcProto unroutedArc = Generic.tech.unrouted_arc;
		int numUnroutedArcs = 0;
		for (int i = 0; i < numArcs; i++) {
			ArcInst ai = cell.getArc(i);
			ArcProto ap = ai.getProto();
			if (ap == universalArc) numUniversalArcs++;
			if (ap == unroutedArc) numUnroutedArcs++;
		}
		if (numUniversalArcs > 0)
			System.out.println("Network: Layout cell " + cell.describe() + " has " + numUniversalArcs + " " + universalArc.describe() + " arcs");
		if (numUnroutedArcs > 0)
			System.out.println("Network: Layout cell " + cell.describe() + " has " + numUnroutedArcs + " " + unroutedArc.describe() + " arcs");
	}

	private void initConnections() {
		int numPorts = cell.getNumPorts();
		int numNodes = cell.getNumNodes();
		int numArcs = cell.getNumArcs();
		if (ni_pi == null || ni_pi.length != numNodes)
			ni_pi = new int[numNodes];
		int offset = numPorts;
		for (int i = 0; i < numNodes; i++) {
			NodeInst ni = cell.getNode(i);
			ni_pi[i] = offset;
			offset += ni.getProto().getNumPorts();
		}
		arcsOffset = offset;
		offset += numArcs;
		if (headConn == null || headConn.length != offset) {
			headConn = new int[offset];
			tailConn = new int[offset];
			drawns = new int[offset];
		}
		for (int i = numPorts; i < arcsOffset; i++) {
			headConn[i] = i;
			tailConn[i] = i;
		}
		for (int i = 0; i < dbExports.length; i++) {
			int portOffset = i;
			int orig = getPortInstOffset(dbExports[i].getOriginalPort());
			headConn[portOffset] = headConn[orig];
			headConn[orig] = portOffset;
			tailConn[portOffset] = -1;
		}
		for (int i = 0; i < cell.getNumArcs(); i++) {
			ArcInst ai = cell.getArc(i);
			int arcOffset = arcsOffset + i;
			int head = getPortInstOffset(ai.getHead().getPortInst());
			headConn[arcOffset] = headConn[head];
			headConn[head] = arcOffset;
			int tail = getPortInstOffset(ai.getTail().getPortInst());
			tailConn[arcOffset] = tailConn[tail];
			tailConn[tail] = arcOffset;
		}
		//showConnections();
	}

	private void showConnections() {
		int numNodes = cell.getNumNodes();
		for (int i = 0; i < numNodes; i++) {
			NodeInst ni = cell.getNode(i);
			int numPortInsts = ni.getProto().getNumPorts();
			for (int j = 0; j < numPortInsts; j++) {
				PortInst pi = ni.getPortInst(j);
				System.out.println("Connections of " + pi);
				int piOffset = getPortInstOffset(pi);
				for (int k = piOffset; headConn[k] != piOffset; ) {
					k = headConn[k];
					if (k >= arcsOffset)
						System.out.println("\thead_arc\t" + cell.getArc(k - arcsOffset).describe());
					else
						System.out.println("\tport\t" + cell.getPort(k));
				}
				for (int k = piOffset; tailConn[k] != piOffset; ) {
					k = tailConn[k];
					System.out.println("\ttail_arc\t" + cell.getArc(k - arcsOffset).describe());
				}
			}
		}
	}

	private void addToDrawn1(PortInst pi, int drawn) {
		int piOffset = getPortInstOffset(pi);
		if (drawns[piOffset] >= 0) return;
		PortProto pp = pi.getPortProto();
		if (pp.isIsolated()) return;
		drawns[piOffset] = drawn;
		if (Network.debug) System.out.println(drawn + ": " + pi);

		for (int k = piOffset; headConn[k] != piOffset; ) {
			k = headConn[k];
			if (drawns[k] >= 0) continue;
			if (k < dbExports.length) {
				drawns[k] = drawn;
				if (Network.debug) System.out.println(drawn + ": " + cell.getPort(k));
				continue;
			}
			ArcInst ai = cell.getArc(k - arcsOffset);
			ArcProto ap = ai.getProto();
			if (ap.getFunction() == ArcProto.Function.NONELEC) continue;
			if (pp == busPinPort && ap != busArc) continue;
			drawns[k] = drawn;
			if (Network.debug) System.out.println(drawn + ": " + ai.describe());
			PortInst tpi = ai.getTail().getPortInst();
			if (tpi.getPortProto() == busPinPort && ap != busArc) continue;
			addToDrawn(tpi, drawn);
		}
		for (int k = piOffset; tailConn[k] != piOffset; ) {
			k = tailConn[k];
			if (drawns[k] >= 0) continue;
			ArcInst ai = cell.getArc(k - arcsOffset);
			ArcProto ap = ai.getProto();
			if (ap.getFunction() == ArcProto.Function.NONELEC) continue;
			if (pp == busPinPort && ap != busArc) continue;
			drawns[k] = drawn;
			if (Network.debug) System.out.println(drawn + ": " + ai.describe());
			PortInst hpi = ai.getHead().getPortInst();
			if (hpi.getPortProto() == busPinPort && ap != busArc) continue;
			addToDrawn(hpi, drawn);
		}
	}

	private void addToDrawn(PortInst pi, int drawn) {
		PortProto pp = pi.getPortProto();
		NodeProto np = pp.getParent();
		int numPorts = np.getNumPorts();
		if (numPorts == 1 || np instanceof Cell) {
			addToDrawn1(pi, drawn);
			return;
		}
		int topology = pp.getTopology();
		NodeInst ni = pi.getNodeInst();
		for (int i = 0; i < numPorts; i++) {
			if (np.getPort(i).getTopology() != topology) continue;
			addToDrawn1(ni.getPortInst(i), drawn);
		}
	}

	int makeDrawns() {
		initConnections();
		Arrays.fill(drawns, -1);
		int drawn = 0;
		int numPorts = dbExports.length;
		int numNodes = cell.getNumNodes();
		int numArcs = cell.getNumArcs();
		for (int i = 0; i < numPorts; i++) {
			if (drawns[i] >= 0) continue;
			drawns[i] = drawn;
			addToDrawn(dbExports[i].getOriginalPort(), drawn);
			drawn++;
		}
		for (int i = 0; i < numArcs; i++) {
			if (drawns[arcsOffset + i] >= 0) continue;
			ArcInst ai = cell.getArc(i);
			ArcProto ap = ai.getProto();
			if (ap.getFunction() == ArcProto.Function.NONELEC) continue;
			drawns[arcsOffset + i] = drawn;
			if (Network.debug) System.out.println(drawn + ": " + ai.describe());
			PortInst hpi = ai.getHead().getPortInst();
			if (hpi.getPortProto() != busPinPort || ap == busArc)
				addToDrawn(hpi, drawn);
			PortInst tpi = ai.getTail().getPortInst();
			if (tpi.getPortProto() != busPinPort || ap == busArc)
				addToDrawn(tpi, drawn);
			drawn++;
		}
		for (int i = 0; i < numNodes; i++) {
			NodeInst ni = cell.getNode(i);
			NodeProto np = ni.getProto();
			int numPortInsts = np.getNumPorts();
			for (int j = 0; j < numPortInsts; j++) {
				PortInst pi = ni.getPortInst(j);
				int piOffset = getPortInstOffset(pi);
				if (ni.isIconOfParent() ||
					np.getFunction() == NodeProto.Function.ART ||
					np == Artwork.tech.pinNode ||
					np == Generic.tech.invisiblePinNode) {
					if (drawns[piOffset] >= 0)
						System.out.println("Network: " + cell + " has connections on " + pi);
					continue;
				}
				if (drawns[piOffset] >= 0) continue;
				if (pi.getPortProto().isIsolated()) continue;
				if (np.getFunction() == NodeProto.Function.PIN)
					System.out.println("Network: " + cell + " has unconnected pin " + pi.describe());
				addToDrawn(pi, drawn);
				drawn++;
			}
		}
		return drawn;
//  		System.out.println(cell + " has " + cell.getNumPorts() + " ports, " + cell.getNumNodes() + " nodes, " +
//  			cell.getNumArcs() + " arcs, " + (arcsOffset - cell.getNumPorts()) + " portinsts, " + netMap.length + "(" + piDrawns + ") drawns");
	}

	int initNetnames() {
		for (Iterator it = netNames.values().iterator(); it.hasNext(); ) {
			NetName nn = (NetName)it.next();
			nn.name = null;
			nn.index = -1;
		}
		netNameCount = 0;
		for (Iterator it = cell.getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			addNetNames(e.getProtoNameKey());
		}
		for (Iterator it = cell.getArcs(); it.hasNext(); ) {
			ArcInst ai = (ArcInst) it.next();
			if (ai.getProto().getFunction() == ArcProto.Function.NONELEC) continue;
			if (ai.getNameKey().isBus() && ai.getProto() != busArc)
				System.out.println("Network: " + cell + " has bus name <"+ai.getNameKey()+"> not on to bus arc");
			if (ai.isUsernamed())
				addNetNames(ai.getNameKey());
		}
		for (Iterator it = netNames.values().iterator(); it.hasNext(); ) {
			NetName nn = (NetName)it.next();
			if (nn.name == null)
				it.remove();
			else if (Network.debug)
				System.out.println("NetName "+nn.name+" "+nn.index);
		}
		return netNameCount;
	}

	void addNetNames(Name name) {
		if (name.isBus())
			System.out.println("Network: Layout cell " + cell.describe() + " has bus port/arc " + name);
		addNetName(name);
	}

	void addNetName(Name name) {
		NetName nn = (NetName)netNames.get(name);
		if (nn == null) {
			nn = new NetName();
			netNames.put(name.lowerCase(), nn);
		}
		if (nn.index < 0) {
			nn.name = name;
			nn.index = netNameCount++;
		}
	}

	private void mergeInternallyConnected()
	{
		for (int i = 0; i < netMap.length; i++) netMap[i] = i;
		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (!(np instanceof Cell)) continue;
			if (Network.getNetCell((Cell)np) instanceof NetSchem) continue;
			int[] eq = Network.getEquivPorts(np);
			int nodeOffset = ni_pi[ni.getNodeIndex()];
			if (np instanceof Cell && !isSchem(np)) {
				for (int i = 0; i < eq.length; i++)
				{
					if (eq[i] == i) continue;
					connectMap(drawns[nodeOffset + i], drawns[nodeOffset + eq[i]]);
				}
			}
		}
		closureMap();
	}

	final int getPortInstOffset(PortInst pi) {
		return ni_pi[pi.getNodeInst().getNodeIndex()] + pi.getPortProto().getPortIndex();
	}

	private void buildNetworkList()
	{
		if (networks == null || networks.length != netMap.length)
			networks = new JNetwork[netMap.length];
		int k = 0;
		for (int i = 0; i < netMap.length; i++) {
			if (netMap[i] == i)
				networks[i] = new JNetwork(cell);
			else 
				networks[i] = null;
		}
		JNetwork[] netNameToNet = new JNetwork[netNames.size()];
		int numPorts = dbExports.length;
		for (int i = 0; i < numPorts; i++) {
			Export e = dbExports[i];
			setNetName(netNameToNet, drawns[i], e.getProtoNameKey());
		}
		int numArcs = cell.getNumArcs();
		for (int i = 0; i < numArcs; i++) {
			ArcInst ai = cell.getArc(i);
			if (!ai.isUsernamed()) continue;
			int ind = drawns[arcsOffset + i];
			if (ind < 0) continue;
			setNetName(netNameToNet, ind, ai.getNameKey());
		}
		for (int i = 0; i < numArcs; i++) {
			ArcInst ai = cell.getArc(i);
			int ind = drawns[arcsOffset + i];
			if (ind < 0) continue;
			JNetwork network = networks[netMap[ind]];
			if (network.hasNames()) continue;
			network.addName(ai.getName());
		}
		/*
		// debug info
		System.out.println("BuildNetworkList "+this);
		int i = 0;
		for (Iterator nit = getNetworks(); nit.hasNext(); )
		{
			JNetwork network = (JNetwork)nit.next();
			String s = "";
			for (Iterator sit = network.getNames(); sit.hasNext(); )
			{
				String n = (String)sit.next();
				s += "/"+ n;
			}
			for (Iterator pit = network.getPorts(); pit.hasNext(); )
			{
				PortInst pi = (PortInst)pit.next();
				s += "|"+pi.getNodeInst().getProto()+"&"+pi.getPortProto().getProtoName();
			}
			System.out.println("    "+i+"    "+s);
			i++;
		}
		*/
	}

	private void setNetName(JNetwork[] netNamesToNet, int netIndex, Name name) {
		JNetwork network = networks[netMap[netIndex]];
		NetName nn = (NetName)netNames.get(name);
		if (netNamesToNet[nn.index] != null)
			System.out.println("Network: Layout cell " + cell.describe() + " has nets with same name " + name);
		else
			netNamesToNet[nn.index] = network;
		network.addName(name.toString());
	}

	/**
	 * Update map of equivalent ports newEquivPort.
	 * @param currentTime time stamp of current network reevaluation
	 * or will be kept untouched if not.
	 */
	private boolean updateInterface() {
		boolean changed = false;
		int numPorts = dbExports.length;
		int[] netToPort = new int[numPorts];
		Arrays.fill(netToPort, -1);
		for (int i = 0; i < numPorts; i++) {
			int net = netMap[drawns[i]];
			if (netToPort[net] < 0)
				netToPort[net] = i;
			if (equivPorts[i] != netToPort[net]) {
				changed = true;
				equivPorts[i] = netToPort[net];
			}
		}
		return changed;
	}

	/**
	 * Show map of equivalent ports newEquivPort.
	 * @param userEquivMap HashMap (PortProto -> JNetwork) of user-specified equivalent ports
	 * @param currentTime.time stamp of current network reevaluation
	 */
	private void showEquivPorts()
	{
		System.out.println("Equivalent ports of "+cell);
		String s = "\t";
		Export[] ports = new Export[cell.getNumPorts()];
		int i = 0;
		for (Iterator it = cell.getPorts(); it.hasNext(); i++)
			ports[i] = (Export)it.next();
		for (i = 0; i < equivPorts.length; i++)
		{
			Export pi = ports[i];
			if (equivPorts[i] != i) continue;
			boolean found = false;
			for (int j = i+1; j < equivPorts.length; j++)
			{
				if (equivPorts[i] != equivPorts[j]) continue;
				Export pj = ports[j];
				if (!found) s = s+" ( "+pi.getProtoName();
				found = true;
				s = s+" "+pj.getProtoName();
			}
			if (found)
				s = s+")";
			else
				s = s+" "+pi.getProtoName();
		}
		System.out.println(s);
	}

	void redoNetworks()
	{
		if ((flags & VALID) != 0) return;

		// redo descendents
		for (Iterator it = cell.getUsagesIn(); it.hasNext();)
		{
			NodeUsage nu = (NodeUsage) it.next();
			if (nu.isIconOfParent()) continue;

			NodeProto np = nu.getProto();
			if (!(np instanceof Cell)) continue;
			NetCell netCell = Network.getNetCell((Cell)np);
			if ((netCell.flags & VALID) == 0)
				netCell.redoNetworks();
		}

		if ((flags & LOCALVALID) != 0)
		{
			flags |= VALID;
			return;
		}

		boolean changed = false;
		if (updateExports()) changed = true;
		if (equivPorts == null || equivPorts.length != dbExports.length)
			equivPorts = new int[dbExports.length];

		/* Set index of NodeInsts */
		checkLayoutCell();
		initConnections();
		int numDrawns = makeDrawns();
		if (netMap == null || netMap.length != numDrawns) {
			netMap = new int[numDrawns];
		}

		// Gather port and arc names
		int netNameSize = initNetnames();

		mergeInternallyConnected();
		buildNetworkList();
		if (updateInterface()) changed = true;
		if (changed)
			invalidateUsagesOf(true);
		flags |= (LOCALVALID|VALID);
	}

	/**
	 * Merge classes of equivalence map to which elements a1 and a2 belong.
	 */
	final void connectMap(int a1, int a2) {
		//System.out.println("connectMap "+a1+" "+a2);
		NetCell.connectMap(netMap, a1, a2);
	}

	/**
	 * Obtain canonical representation of equivalence map.
	 */
	final void closureMap() { NetCell.closureMap(netMap); }

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

	static boolean isSchem(NodeProto np) {
		return np instanceof Cell && Network.getNetCell((Cell)np) instanceof NetSchem;
	}
}


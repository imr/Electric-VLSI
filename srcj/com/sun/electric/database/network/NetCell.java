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
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.user.ErrorLogger;

import java.util.Arrays;
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
    /** The number of times this NetCell has been <i>structurally modified</i>.
     * Structural modifications are those that change the number of networks in
	 * its netlists, or otherwise perturb them in such a fashion that iterations in
     * progress may yield incorrect results.<p>
     *
     * This field is used by the netlist implementation returned by the
	 * <tt>getNetlist</tt> methods. If the value of this field changes unexpectedly,
	 * the netlist will throw a <tt>ConcurrentModificationException</tt> in
     * response to the <tt>getNetwork</tt> and other operations.  This provides
     * <i>fail-fast</i> behavior, rather than non-deterministic behavior in
     * the face of concurrent modification during netlist examination.<p>
     */																int modCount = 0;

	/**
     * Equivalence of ports. equivPorts.size == ports.size.
	 * equivPorts[i] contains minimal index among ports of its group.
     */																private int[] equivPorts;
	/** Node offsets. */											int[] ni_pi;
	/** */															int arcsOffset;
	/** */															private int[] headConn;
	/** */															private int[] tailConn;
	/** */															int[] drawns;
	/** */															int numDrawns;

	/** A map from Name to NetName. */								Map netNames = new HashMap();
	/** Counter for enumerating NetNames. */						private int netNameCount;
	/** Counter for enumerating NetNames. */						int exportedNetNameCount;
	
	/** Netlist. */													private Netlist netlist;

	/** */															private static PortProto busPinPort = Schematics.tech.busPinNode.getPort(0);
	/** */															private static ArcProto busArc = Schematics.tech.bus_arc;
	/**
	 * The constructor is not used.
	 */
	NetCell() { cell = null; }

	NetCell(Cell cell)
	{
		this.cell = cell;
		NetworkTool.setCell(cell, this);
	}

	final void setNetworksDirty()
	{
		setInvalid(true, false);
	}

	void exportsChanged()
	{
		setInvalid(true, true);
	}

	void setInvalid(boolean strongMe, boolean strongUsages)
	{
//		System.out.println("setInvalid " + cell + " " + strongMe + " " + strongUsages);
		if (strongMe) flags &= ~LOCALVALID;
		if ((flags & VALID) == 0 && !strongUsages) return;
		flags &= ~VALID;
		invalidateUsagesOf(strongUsages);
	}

	void invalidateUsagesOf(boolean strong)
	{
//		System.out.println("NetSchem.invalidateUsagesOf " + cell + " " + strong);
		for (Iterator it = cell.getUsagesOf(); it.hasNext();) {
			NodeUsage nu = (NodeUsage)it.next();
			if (nu.isIconOfParent()) continue;
			NetCell netCell = NetworkTool.getNetCell(nu.getParent());
			if (netCell != null) netCell.setInvalid(strong, false);
		}
	}

	Netlist getNetlist(boolean shortResistros) {
		if ((flags & VALID) == 0) redoNetworks();
		return netlist;
	}

	/**
	 * Get an iterator over all of the Nodables of this Cell.
	 */
	Iterator getNodables() { return cell.getNodes(); }

	/**
	 * Get a set of global signal in this Cell and its descendants.
	 */
	Global.Set getGlobals() { return Global.Set.empty; }

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
// 			NetCell netCell = NetworkTool.cells[npInd];
// 			if ((netCell.flags & VALID) != 0)
// 				netCell.redoNetworks();
// 		}
// 	}

	/*
	 * Get offset in networks map for given global signal.
	 */
	int getNetMapOffset(Global global) { return -1; }

	/*
	 * Get offset in networks map for given port instance.
	 */
	int getNetMapOffset(Nodable no, PortProto portProto, int busIndex) {
		NodeInst ni = (NodeInst)no;
		return drawns[ni_pi[ni.getNodeIndex()] + portProto.getPortIndex()];
	}

	/*
	 * Get offset in networks map for given export..
	 */
	int getNetMapOffset(Export export, int busIndex) {
		return drawns[export.getPortIndex()];
	}

	/*
	 * Get offset in networks map for given arc.
	 */
	int getNetMapOffset(ArcInst ai, int busIndex) {
		return drawns[arcsOffset + ai.getArcIndex()];
	}

	/**
	 * Method to return either the network name or the bus name on this ArcInst.
	 * @return the either the network name or the bus name on this ArcInst.
	 */
	Name getBusName(ArcInst ai) { return null; }

	/**
	 * Method to return the bus width on this ArcInst.
	 * @return the either the bus width on this ArcInst.
	 */
	public int getBusWidth(ArcInst ai)
	{
		int drawn = drawns[arcsOffset + ai.getArcIndex()];
		if (drawn < 0) return 0;
		return 1;
	}

	private void checkLayoutCell() {
		int numNodes = cell.getNumNodes();
		for (int i = 0; i < numNodes; i++) {
			NodeInst ni = (NodeInst)cell.getNode(i);
			if (ni.getNameKey().isBus()) {
				String msg = "Network: Layout cell " + cell.describe() + " has arrayed node " + ni.describe();
                System.out.println(msg);
                ErrorLogger.MessageLog log = NetworkTool.errorLogger.logError(msg, cell, NetworkTool.errorSortNodes);
                log.addGeom(ni, true, cell, null);
            }
		}
		for (Iterator it = cell.getUsagesIn(); it.hasNext();) {
			NodeUsage nu = (NodeUsage)it.next();
			NodeProto np = nu.getProto();
			boolean err = false;
			if (np instanceof Cell) {
				if (NetworkTool.getNetCell((Cell)np) instanceof NetSchem) {
					String msg = "Network: Layout cell " + cell.describe() + " has " + nu.getNumInsts() +
						" " + np.describe() + " nodes";
                    System.out.println(msg);
                    ErrorLogger.MessageLog log = NetworkTool.errorLogger.logError(msg, cell, NetworkTool.errorSortNodes);
                    for (Iterator nit = nu.getInsts(); nit.hasNext(); ) {
                        NodeInst ni = (NodeInst)nit.next();
                        log.addGeom(ni, true, cell, null);
                    }
					err = true;
				}
			}
			if (np == Generic.tech.universalPinNode) {
				String msg = "Network: Layout cell " + cell.describe() + " has " + nu.getNumInsts() +
					" " + np.describe() + " nodes";
                System.out.println(msg);
                ErrorLogger.MessageLog log = NetworkTool.errorLogger.logError(msg, cell, NetworkTool.errorSortNodes);
                for (Iterator nit = nu.getInsts(); nit.hasNext(); ) {
                    NodeInst ni = (NodeInst)nit.next();
                    log.addGeom(ni, true, cell, null);
                }
				err = true;
			}
			if (np instanceof PrimitiveNode) {
				if (np.getTechnology() == Schematics.tech) {
					String msg = "Network: Layout cell " + cell.describe() + " has " + nu.getNumInsts() +
						" " + np.describe() + " nodes";
                    System.out.println(msg);
                    ErrorLogger.MessageLog log = NetworkTool.errorLogger.logError(msg, cell, NetworkTool.errorSortNodes);
                    for (Iterator nit = nu.getInsts(); nit.hasNext(); ) {
                        NodeInst ni = (NodeInst)nit.next();
                        log.addGeom(ni, true, cell, null);
                    }
					err = true;
				}
			}
		}
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
		for (int i = 0; i < numPorts; i++) {
			int portOffset = i;
			Export export = (Export)cell.getPort(i);
			int orig = getPortInstOffset(export.getOriginalPort());
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

	private void addToDrawn1(PortInst pi) {
		int piOffset = getPortInstOffset(pi);
		if (drawns[piOffset] >= 0) return;
		PortProto pp = pi.getPortProto();
		if (pp instanceof PrimitivePort && ((PrimitivePort)pp).isIsolated()) return;
		drawns[piOffset] = numDrawns;
		if (NetworkTool.debug) System.out.println(numDrawns + ": " + pi);

		for (int k = piOffset; headConn[k] != piOffset; ) {
			k = headConn[k];
			if (drawns[k] >= 0) continue;
			if (k < arcsOffset) {
				// This is port
				drawns[k] = numDrawns;
				if (NetworkTool.debug) System.out.println(numDrawns + ": " + cell.getPort(k));
				continue;
			}
			ArcInst ai = cell.getArc(k - arcsOffset);
			ArcProto ap = ai.getProto();
			if (ap.getFunction() == ArcProto.Function.NONELEC) continue;
			if (pp == busPinPort && ap != busArc) continue;
			drawns[k] = numDrawns;
			if (NetworkTool.debug) System.out.println(numDrawns + ": " + ai.describe());
			PortInst tpi = ai.getTail().getPortInst();
			if (tpi.getPortProto() == busPinPort && ap != busArc) continue;
			addToDrawn(tpi);
		}
		for (int k = piOffset; tailConn[k] != piOffset; ) {
			k = tailConn[k];
			if (drawns[k] >= 0) continue;
			ArcInst ai = cell.getArc(k - arcsOffset);
			ArcProto ap = ai.getProto();
			if (ap.getFunction() == ArcProto.Function.NONELEC) continue;
			if (pp == busPinPort && ap != busArc) continue;
			drawns[k] = numDrawns;
			if (NetworkTool.debug) System.out.println(numDrawns + ": " + ai.describe());
			PortInst hpi = ai.getHead().getPortInst();
			if (hpi.getPortProto() == busPinPort && ap != busArc) continue;
			addToDrawn(hpi);
		}
	}

	private void addToDrawn(PortInst pi) {
		PortProto pp = pi.getPortProto();
		NodeProto np = pp.getParent();
		int numPorts = np.getNumPorts();
		if (numPorts == 1 || np instanceof Cell) {
			addToDrawn1(pi);
			return;
		}
		NodeInst ni = pi.getNodeInst();
// 		if (np == Schematics.tech.resistorNode && NetworkTool.shortResistors) {
//			
// 			addToDrawn1(ni.getPortInst(0));
// 			addToDrawn1(ni.getPortInst(1));
// 			return;
// 		}
		int topology = ((PrimitivePort)pp).getTopology();
		for (int i = 0; i < numPorts; i++) {
			if (((PrimitivePort)np.getPort(i)).getTopology() != topology) continue;
			addToDrawn1(ni.getPortInst(i));
		}
	}

	void makeDrawns() {
		initConnections();
		Arrays.fill(drawns, -1);
		numDrawns = 0;
		int numPorts = cell.getNumPorts();
		int numNodes = cell.getNumNodes();
		int numArcs = cell.getNumArcs();
		for (int i = 0; i < numPorts; i++) {
			if (drawns[i] >= 0) continue;
			drawns[i] = numDrawns;
			Export export = (Export)cell.getPort(i);
			addToDrawn(export.getOriginalPort());
			numDrawns++;
		}
		for (int i = 0; i < numArcs; i++) {
			if (drawns[arcsOffset + i] >= 0) continue;
			ArcInst ai = cell.getArc(i);
			ArcProto ap = ai.getProto();
			if (ap.getFunction() == ArcProto.Function.NONELEC) continue;
			drawns[arcsOffset + i] = numDrawns;
			if (NetworkTool.debug) System.out.println(numDrawns + ": " + ai.describe());
			PortInst hpi = ai.getHead().getPortInst();
			if (hpi.getPortProto() != busPinPort || ap == busArc)
				addToDrawn(hpi);
			PortInst tpi = ai.getTail().getPortInst();
			if (tpi.getPortProto() != busPinPort || ap == busArc)
				addToDrawn(tpi);
			numDrawns++;
		}
		for (int i = 0; i < numNodes; i++) {
			NodeInst ni = cell.getNode(i);
			NodeProto np = ni.getProto();
			int numPortInsts = np.getNumPorts();
			for (int j = 0; j < numPortInsts; j++) {
				PortInst pi = ni.getPortInst(j);
				int piOffset = getPortInstOffset(pi);
				if (ni.isIconOfParent() ||
					np.getFunction() == PrimitiveNode.Function.ART && np != Generic.tech.simProbeNode ||
					np == Artwork.tech.pinNode ||
					np == Generic.tech.invisiblePinNode) {
					if (drawns[piOffset] >= 0 && !cell.isIcon()) {
						String msg = "Network: " + cell + " has connections on " + pi;
                        System.out.println(msg);
                        ErrorLogger.MessageLog log = NetworkTool.errorLogger.logError(msg, cell, NetworkTool.errorSortNodes);
                        log.addPoly(pi.getPoly(), true, cell);
                    }
					continue;
				}
				if (drawns[piOffset] >= 0) continue;
				if (pi.getPortProto() instanceof PrimitivePort && ((PrimitivePort)pi.getPortProto()).isIsolated()) continue;
				if (np.getFunction() == PrimitiveNode.Function.PIN && !cell.isIcon()) {
					String msg = "Network: " + cell + " has unconnected pin " + pi.describe();
                    System.out.println(msg);
                    ErrorLogger.MessageLog log = NetworkTool.errorLogger.logWarning(msg, cell, NetworkTool.errorSortNodes);
                    log.addGeom(ni, true, cell, null);
                }
				addToDrawn(pi);
				numDrawns++;
			}
		}
		// showDrawns();
//  		System.out.println(cell + " has " + cell.getNumPorts() + " ports, " + cell.getNumNodes() + " nodes, " +
//  			cell.getNumArcs() + " arcs, " + (arcsOffset - cell.getNumPorts()) + " portinsts, " + netMap.length + "(" + piDrawns + ") drawns");
	}

	void showDrawns() {
		java.io.PrintWriter out;
		String filePath = "tttt";
		try
		{
            out = new java.io.PrintWriter(new java.io.BufferedWriter(new java.io.FileWriter(filePath, true)));
        } catch (java.io.IOException e)
		{
            System.out.println("Error opening " + filePath);
			return;
        }

		out.println("Drawns " + cell);
		int numPorts = cell.getNumPorts();
		for (int drawn = 0; drawn < numDrawns; drawn++) {
			for (int i = 0; i < drawns.length; i++) {
				if (drawns[i] != drawn) continue;
				if (i < numPorts) {
					out.println(drawn + ": " + cell.getPort(i));
				} else if (i >= arcsOffset) {
					out.println(drawn + ": " + cell.getArc(i - arcsOffset));
				} else {
					int k = 1;
					for (; k < cell.getNumNodes() && ni_pi[k] <= i; k++) ;
					k--;
					NodeInst ni = cell.getNode(k);
					PortInst pi = ni.getPortInst(i - ni_pi[k]);
					out.println(drawn + ": " + pi);
				}
			}
		}
		out.close(); 
	}

	void initNetnames() {
		for (Iterator it = netNames.values().iterator(); it.hasNext(); ) {
			NetName nn = (NetName)it.next();
			nn.name = null;
			nn.index = -1;
		}
		netNameCount = 0;
		for (Iterator it = cell.getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			addNetNames(e.getNameKey());
		}
		exportedNetNameCount = netNameCount;
		for (Iterator it = cell.getArcs(); it.hasNext(); ) {
			ArcInst ai = (ArcInst) it.next();
			if (ai.getProto().getFunction() == ArcProto.Function.NONELEC) continue;
			if (ai.getNameKey().isBus() && ai.getProto() != busArc) {
				String msg = "Network: " + cell + " has bus name <"+ai.getNameKey()+"> on arc that is not a bus";
                System.out.println(msg);
                ErrorLogger.MessageLog log = NetworkTool.errorLogger.logError(msg, cell, NetworkTool.errorSortNetworks);
                log.addGeom(ai, true, cell, null);
            }
			if (ai.isUsernamed())
				addNetNames(ai.getNameKey());
		}
		for (Iterator it = netNames.values().iterator(); it.hasNext(); ) {
			NetName nn = (NetName)it.next();
			if (nn.name == null)
				it.remove();
			else if (NetworkTool.debug)
				System.out.println("NetName "+nn.name+" "+nn.index);
		}
		if (netNameCount != netNames.size())
			System.out.println("Error netNameCount in NetCell.initNetnames");
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

	private void internalConnections()
	{
		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (!(np instanceof Cell)) continue;
			NetCell netCell = NetworkTool.getNetCell((Cell)np);
			if (netCell instanceof NetSchem) continue;
			int[] eq = netCell.equivPorts;
			int nodeOffset = ni_pi[ni.getNodeIndex()];
			for (int i = 0; i < eq.length; i++)
			{
				if (eq[i] == i) continue;
				netlist.connectMap(drawns[nodeOffset + i], drawns[nodeOffset + eq[i]]);
			}
		}
	}

	final int getPortInstOffset(PortInst pi) {
		return ni_pi[pi.getNodeInst().getNodeIndex()] + pi.getPortProto().getPortIndex();
	}

	int getPortOffset(int portIndex, int busIndex) { return busIndex == 0 ? portIndex : -1; }

	NetSchem getSchem() { return null; }

	private void buildNetworkList()
	{
		netlist.initNetworks();
		Network[] netNameToNet = new Network[netNames.size()];
		int numPorts = cell.getNumPorts();
		for (int i = 0; i < numPorts; i++) {
			Export e = (Export)cell.getPort(i);
			setNetName(netNameToNet, drawns[i], e.getNameKey(), true);
		}
		int numArcs = cell.getNumArcs();
		for (int i = 0; i < numArcs; i++) {
			ArcInst ai = cell.getArc(i);
			if (!ai.isUsernamed()) continue;
			int drawn = drawns[arcsOffset + i];
			if (drawn < 0) continue;
			setNetName(netNameToNet, drawn, ai.getNameKey(), false);
		}
		for (int i = 0; i < numArcs; i++) {
			ArcInst ai = cell.getArc(i);
			int drawn = drawns[arcsOffset + i];
			if (drawn < 0) continue;
			Network network = netlist.getNetworkByMap(drawn);
			if (network.hasNames()) continue;
			network.addName(ai.getName(), false);
		}
		for (int i = 0; i < cell.getNumNodes(); i++) {
			NodeInst ni = cell.getNode(i);
			for (int j = 0; j < ni.getProto().getNumPorts(); j++) {
				int drawn = drawns[ni_pi[i] + j];
				if (drawn < 0) continue;
				Network network  = netlist.getNetworkByMap(drawn);
				if (network.hasNames()) continue;
				network.addName(ni.getName() + "@" + ni.getProto().getPort(j).getName(), false);
			}
		}
		/*
		// debug info
		System.out.println("BuildNetworkList "+this);
		int i = 0;
		for (Iterator nit = getNetworks(); nit.hasNext(); )
		{
			Network network = (Network)nit.next();
			String s = "";
			for (Iterator sit = network.getNames(); sit.hasNext(); )
			{
				String n = (String)sit.next();
				s += "/"+ n;
			}
			for (Iterator pit = network.getPorts(); pit.hasNext(); )
			{
				PortInst pi = (PortInst)pit.next();
				s += "|"+pi.getNodeInst().getProto()+"&"+pi.getPortProto().getName();
			}
			System.out.println("    "+i+"    "+s);
			i++;
		}
		*/
	}

	private void setNetName(Network[] netNamesToNet, int drawn, Name name, boolean exported) {
		Network network = netlist.getNetworkByMap(drawn);
		NetName nn = (NetName)netNames.get(name);
		if (netNamesToNet[nn.index] != null) {
			if (netNamesToNet[nn.index] == network) return;
			String msg = "Network: Layout cell " + cell.describe() + " has nets with same name " + name;
            System.out.println(msg);
            ErrorLogger.MessageLog log = NetworkTool.errorLogger.logError(msg, cell, NetworkTool.errorSortNetworks);
            // because this should be an infrequent event that the user will fix, let's
            // put all the work here
            int numPorts = cell.getNumPorts();
            for (int i = 0; i < numPorts; i++) {
                Export e = (Export)cell.getPort(i);
                if (e.getName().equals(name.toString())) log.addExport(e, true, cell, null);
            }
            int numArcs = cell.getNumArcs();
            for (int i = 0; i < numArcs; i++) {
                ArcInst ai = cell.getArc(i);
                if (!ai.isUsernamed()) continue;
                if (ai.getName().equals(name.toString())) log.addGeom(ai, true, cell, null);
            }
        }
		else
			netNamesToNet[nn.index] = network;
		network.addName(name.toString(), exported);
	}

	/**
	 * Update map of equivalent ports newEquivPort.
	 */
	private boolean updateInterface() {
		boolean changed = false;
		int numPorts = cell.getNumPorts();
		if (equivPorts == null || equivPorts.length != numPorts) {
			changed = true;
			equivPorts = new int[numPorts];
		}

		int[] netToPort = new int[numPorts];
		Arrays.fill(netToPort, -1);
		for (int i = 0; i < numPorts; i++) {
			int net = netlist.netMap[drawns[i]];
			if (netToPort[net] < 0)
				netToPort[net] = i;
			if (equivPorts[i] != netToPort[net]) {
				changed = true;
				equivPorts[i] = netToPort[net];
			}
		}
		return changed;
	}


	int getNumEquivPorts() { return equivPorts.length; }
	int getEquivPorts(int portIndex) { return equivPorts[portIndex]; }

	/**
	 * Show map of equivalent ports newEquivPort.
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
				if (!found) s = s+" ( "+pi.getName();
				found = true;
				s = s+" "+pj.getName();
			}
			if (found)
				s = s+")";
			else
				s = s+" "+pi.getName();
		}
		System.out.println(s);
	}

	void redoNetworks()
	{
		if ((flags & VALID) != 0) return;

        // clear errors for cell
        NetworkTool.errorLogger.clearLogs(cell);

		// redo descendents
		for (Iterator it = cell.getUsagesIn(); it.hasNext();)
		{
			NodeUsage nu = (NodeUsage) it.next();
			if (nu.isIconOfParent()) continue;

			NodeProto np = nu.getProto();
			if (!(np instanceof Cell)) continue;
			NetCell netCell = NetworkTool.getNetCell((Cell)np);
			if ((netCell.flags & VALID) == 0)
				netCell.redoNetworks();
		}

		// redo implementation
		NetSchem schem = getSchem();
		if (schem != null && schem != this)
			schem.redoNetworks();

		if ((flags & LOCALVALID) != 0)
		{
			flags |= VALID;
			return;
		}

		// Mark this netcell changed
		modCount++;

		makeDrawns();
		// Gather port and arc names
		initNetnames();

		if (redoNetworks1())
			setInvalid(false, true);
		flags |= (LOCALVALID|VALID);
	}

	boolean redoNetworks1() {
		/* Set index of NodeInsts */
		checkLayoutCell();
		netlist = new Netlist(this, numDrawns);

		internalConnections();
		buildNetworkList();
		return updateInterface();
	}

}

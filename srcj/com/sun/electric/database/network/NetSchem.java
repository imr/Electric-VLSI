/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetSchem.java
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
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Schematics;

import java.util.Arrays;
import java.util.ArrayList;
// import java.util.HashMap;
import java.util.Iterator;
// import java.util.Map;

/**
 * This is the mirror of group of Icon and Schematic cells in Network tool.
 */
class NetSchem extends NetCell {
	class Icon {
		Cell iconCell;
		Export[] dbIconExports;
		int[] iconToSchem;

		Icon(Cell iconCell) {
			this.iconCell = iconCell;
		}

		void invalidateUsagesOf(boolean strong) { NetCell.invalidateUsagesOf(iconCell, strong); }

		private void updateInterface() {
			boolean changed = false;
			if (dbIconExports == null || dbIconExports.length != iconCell.getNumPorts()) {
				changed = true;
				dbIconExports = new Export[iconCell.getNumPorts()];
				iconToSchem = new int[iconCell.getNumPorts()];
			}
			for (Iterator it = iconCell.getPorts(); it.hasNext();) {
				Export e = (Export)it.next();
				int portIndex = e.getPortIndex();
				if (dbIconExports[portIndex] != e) {
					changed = true;
					dbIconExports[portIndex] = e;
				}
				int equivIndex = -1;
				if (cell != null) {
					Export equiv = e.getEquivalentPort(cell);
					if (equiv != null) equivIndex = equiv.getPortIndex();
				}
				if (iconToSchem[portIndex] != equivIndex) {
					changed = true;
					iconToSchem[portIndex] = equivIndex;
				}
			}
			if (changed)
				invalidateUsagesOf(true);
		}
	}

	private class Proxy implements Nodable {
		NodeInst nodeInst;
		int arrayIndex;
		int nodeOffset;

		Proxy(NodeInst nodeInst, int arrayIndex) {
			this.nodeInst = nodeInst;
			this.arrayIndex = arrayIndex;
		}
	
		/**
		 * Routine to return the prototype of this Nodable.
		 * @return the prototype of this Nodable.
		 */
		public NodeProto getProto() {
			NodeProto np = nodeInst.getProto();
			if (np instanceof Cell) {
				np = Network.getNetCell((Cell)np).cell;
			}
			return np;
		}

		/**
		 * Routine to return the Cell that contains this Nodable.
		 * @return the Cell that contains this Nodable.
		 */
		public Cell getParent() { return cell; }

		/**
		 * Routine to return the name of this Nodable.
		 * @return the name of this Nodable.
		 */
		public String getName() { return getNameKey().toString(); }

		/**
		 * Routine to return the name key of this Nodable.
		 * @return the name key of this Nodable.
		 */
		public Name getNameKey() { return nodeInst.getNameKey().subname(arrayIndex); }

		/**
		 * Routine to return the Variable on this Nodable with a given name.
		 * @param name the name of the Variable.
		 * @return the Variable with that name, or null if there is no such Variable.
		 */
		public Variable getVar(String name) { return nodeInst.getVar(name); }

		/**
		 * Routine to get network by PortProto and bus index.
		 * @param portProto PortProto in protoType.
		 * @param busIndex index in bus.
		 */
		public JNetwork getNetwork(PortProto portProto, int busIndex) {
			return Network.getNetwork(this, 0, portProto, busIndex);
		}

		/**
		 * Returns a printable version of this Nodable.
		 * @return a printable version of this Nodable.
		 */
		public String toString() { return "NetSchem.Proxy " + getName(); }

	}

	/** */															private Icon[] icons;
	/** Node offsets. */											int[] nodeOffsets;
	/** Node offsets. */											Proxy[] nodeProxies;
	/** */															Global.Set globals = Global.Set.empty;
	/** */															int[] portOffsets = new int[1];
	/** */															int netNamesOffset;

	/** */															Name[] drawnNames;
	/** */															int[] drawnWidths;

	NetSchem(Cell cell) {
		super(cell.isIcon() ? cell.getCellGroup().getMainSchematics() : cell);
		if (cell.isIcon() || cell.getCellGroup().getMainSchematics() == cell)
			initIcons(cell.getCellGroup());
		else
			icons = new Icon[0];
	}

	NetSchem(Cell.CellGroup cellGroup) {
		super(cellGroup.getMainSchematics());
		initIcons(cellGroup);
	}

	private void initIcons(Cell.CellGroup cellGroup) {
		int iconCount = 0;
		for (Iterator it = cellGroup.getCells(); it.hasNext();) {
			Cell c = (Cell)it.next();
			if (c.isIcon()) {
				iconCount++;
				for (Iterator vit = c.getVersions(); vit.hasNext();) {
					Cell verCell = (Cell)vit.next();
					if (verCell == c) continue;
					iconCount++;
				}
			}
		}
		icons = new Icon[iconCount];
		iconCount = 0;
		for (Iterator it = cellGroup.getCells(); it.hasNext();) {
			Cell c = (Cell)it.next();
			if (c.isIcon()) {
				icons[iconCount] = new Icon(c);
				Network.setCell(c, this, ~iconCount);
				iconCount++;
				for (Iterator vit = c.getVersions(); vit.hasNext();) {
					Cell verCell = (Cell)vit.next();
					if (verCell == c) continue;
					icons[iconCount] = new Icon(verCell);
					Network.setCell(verCell, this, ~iconCount);
					iconCount++;
				}
			}
		}
	}


// 		int ind = cellsStart + c.getIndex();
// 		int numPorts = c.getNumPorts();
// 		int[] beg = new int[numPorts + 1];
// 		for (int i = 0; i < beg.length; i++) beg[i] = 0;
// 		for (Iterator pit = c.getPorts(); pit.hasNext(); )
// 		{
// 			Export pp = (Export)pit.next();
// 			beg[pp.getIndex()] = pp.getProtoNameLow().busWidth();
// 		}
// 		int b = 0;
// 		for (int i = 0; i < numPorts; i++)
// 		{
// 			int w = beg[i];
// 			beg[i] = b;
// 			b = b + w;
// 		}
// 		beg[numPorts] = b;
//		protoPortBeg[ind] = beg;

	int getPortOffset(int iconIndex, int portIndex, int busIndex) {
		if (iconIndex < 0)
			portIndex = icons[~iconIndex].iconToSchem[portIndex];
		if (portIndex < 0) return -1;
		int portOffset = portOffsets[portIndex] + busIndex;
		if (busIndex < 0 || portOffset >= portOffsets[portIndex+1]) return -1;
		return portOffset - portOffsets[0];
	}

	/**
	 * Get an iterator over all of the Nodables of this Cell.
	 */
	Iterator getNodables()
	{
		if ((flags & VALID) == 0) redoNetworks();
		ArrayList nodables = new ArrayList();
		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst)it.next();
			if (nodeOffsets[ni.getNodeIndex()] < 0) continue;
			nodables.add(ni);
		}
		for (int i = 0; i < nodeProxies.length; i++) {
			if (nodeProxies[i] != null)
				nodables.add(nodeProxies[i]);
		}
		return nodables.iterator();
	}

	/*
	 * Get network by index in networks maps.
	 */
	JNetwork getNetwork(Nodable no, int arrayIndex, PortProto portProto, int busIndex) {
		if ((flags & VALID) == 0) redoNetworks();
		int portOffset = Network.getPortOffset(portProto, busIndex);
		if (portOffset < 0) return null;
		int nodeOffset;
		if (no instanceof Proxy) {
			Proxy proxy = (Proxy)no;
			nodeOffset = proxy.nodeOffset;
		} else {
			NodeInst ni = (NodeInst)no;
			nodeOffset = nodeOffsets[ni.getNodeIndex()];
			if (nodeOffset < 0) {
				if (arrayIndex < 0 || arrayIndex >= ni.getNameKey().busWidth())
					return null;
				Proxy proxy = nodeProxies[~nodeOffset + arrayIndex];
				if (proxy == null) return null;
				nodeOffset = proxy.nodeOffset;
			}
		}
		return networks[netMap[nodeOffset + portOffset]];
	}

	/*
	 * Get network of export.
	 */
	JNetwork getNetwork(Export export, int busIndex) {
		if (export.getParent() != cell) return null;
		PortInst originalPort = export.getOriginalPort();
		return getNetwork(originalPort.getNodeInst(), 0, originalPort.getPortProto(), busIndex);
	}

	/*
	 * Get network of arc.
	 */
	JNetwork getNetwork(ArcInst ai, int busIndex) {
		if (ai.getParent() != cell) return null;
		if (ai.getProto().getFunction() == ArcProto.Function.NONELEC) return null;
		PortInst headPort = ai.getHead().getPortInst();
		return getNetwork(headPort.getNodeInst(), 0, headPort.getPortProto(), busIndex);
	}

	/**
	 * Routine to return either the network name or the bus name on this ArcInst.
	 * @return the either the network name or the bus n1ame on this ArcInst.
	 */
	String getNetworkName(ArcInst ai) {
		if (ai.getParent() != cell) return null;
		if ((flags & VALID) == 0) redoNetworks();
		int drawn = drawns[arcsOffset + ai.getArcIndex()];
		if (drawn < 0) return null;
		if (drawnWidths[drawn] == 1) {
			JNetwork network = getNetwork(ai, 0);
			if (network == null) return null;
			return network.describe();
		}
		if (drawnNames[drawn] == null) return null;
		return drawnNames[drawn].toString();
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
		return drawnWidths[drawn];
	}

	void invalidateUsagesOf(boolean strong)
	{
		if (cell != null)
			super.invalidateUsagesOf(strong);	
		for (int i = 0; i < icons.length; i++)
			icons[i].invalidateUsagesOf(strong);
	}

	private boolean initNodables() {
		if (nodeOffsets == null || nodeOffsets.length != cell.getNumNodes())
			nodeOffsets = new int[cell.getNumNodes()];
		int nodeOffset = 0;
		Global.clearBuf();
		int nodeProxiesOffset = 0;
		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst)it.next();
			int nodeIndex = ni.getNodeIndex();
			NodeProto np = ni.getProto();
			if (np.isIcon()) {
				if (ni.getNameKey().hasDuplicates())
					System.out.println(cell + ": Node name <"+ni.getNameKey()+"> has duplicate subnames");
				nodeOffsets[nodeIndex] = ~nodeProxiesOffset;
				nodeProxiesOffset += ni.getNameKey().busWidth();
			} else {
				if (ni.getNameKey().isBus())
					System.out.println(cell + ": Array name <"+ni.getNameKey()+"> can be assigned only to icon nodes");
				nodeOffsets[nodeIndex] = nodeOffset;
				nodeOffset += portsSize(np);
			}
			if (isSchem(np)) {
				NetSchem sch = (NetSchem)Network.getNetCell((Cell)np);
				Global.addToBuf(sch.globals);
			} else {
				Global g = globalInst(ni);
				if (g != null) Global.addToBuf(g);
			}
		}
		Global.Set newGlobals = Global.getBuf();
		boolean changed = false;
		if (globals != newGlobals) {
			changed = true;
			globals = newGlobals;
			if (Network.debug) System.out.println(cell+" has "+globals);
		}
		int portsEnd = portOffsets[0] = globals.size();
		for (int i = 1; i <= dbExports.length; i++) {
			if (Network.debug) System.out.println(dbExports[i-1]+" "+portOffsets[i-1]);
			portsEnd += dbExports[i - 1].getProtoNameKey().busWidth();
			if (portOffsets[i] != portsEnd) {
				changed = true;
				portOffsets[i] = portsEnd;
			}
		}
		if (equivPorts == null || equivPorts.length != portsEnd)
			equivPorts = new int[portsEnd];
		for (int i = 0; i < nodeOffsets.length; i++) {
			if (nodeOffsets[i] >= 0)
				nodeOffsets[i] += portsEnd;
		}
		nodeOffset += portsEnd;
		if (nodeProxies == null || nodeProxies.length != nodeProxiesOffset)
			nodeProxies = new Proxy[nodeProxiesOffset];
		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst)it.next();
			int proxyOffset = nodeOffsets[ni.getNodeIndex()];
			if (Network.debug) System.out.println(ni+" "+proxyOffset);
			if (proxyOffset >= 0) continue;
			NetCell netCell = Network.getNetCell((Cell)ni.getProto());
			for (int i = 0; i < ni.getNameKey().busWidth(); i++) {
				Proxy proxy = null;
				if (netCell != this && netCell.cell != null) {
					proxy = new Proxy(ni, i);
					if (Network.debug) System.out.println(proxy+" "+nodeOffset);
					proxy.nodeOffset = nodeOffset;
					nodeOffset += portsSize(ni.getProto());
				}
				nodeProxies[~proxyOffset + i] = proxy;
			}
		}
		netNamesOffset = nodeOffset;
		if (Network.debug) System.out.println("netNamesOffset="+netNamesOffset);
		return changed;
	}

	private static int portsSize(NodeProto np) {
		if (!isSchem(np)) return np.getNumPorts();
		NetSchem sch = (NetSchem)Network.getNetCell((Cell)np);
		return sch.equivPorts.length - sch.globals.size();
	}

	private static Global globalInst(NodeInst ni) {
		NodeProto np = ni.getProto();
		if (np == Schematics.tech.groundNode) return Global.ground;
		if (np == Schematics.tech.powerNode) return Global.power;
		if (np == Schematics.tech.globalNode) {
			Variable var = ni.getVar(Schematics.SCHEM_GLOBAL_NAME, String.class);
			if (var != null) return Global.newGlobal((String)var.getObject());
		}
		return null;
	}

	void calcDrawnWidths() {
		Arrays.fill(drawnNames, null);
		Arrays.fill(drawnWidths, -1);
		int numPorts = cell.getNumPorts();
		int numNodes = cell.getNumNodes();
		int numArcs = cell.getNumArcs();

		for (int i = 0; i < numPorts; i++) {
			int drawn = drawns[i];
			PortProto pp = dbExports[i];
			Name name = pp.getProtoNameKey();
			int newWidth = name.busWidth();
			int oldWidth = drawnWidths[drawn];
			if (oldWidth < 0) {
				drawnNames[drawn] = name;
				drawnWidths[drawn] = name.busWidth();
				continue;
			}
			if (oldWidth != newWidth)
				System.out.println("Network: Schematic cell " + cell.describe() + " has net with conflict name width <" +
								   drawnNames[drawn] + "> and <" + name);
		}
		for (int i = 0; i < numArcs; i++) {
			int drawn = drawns[arcsOffset + i];
			if (drawn < 0) continue;
			ArcInst ai = cell.getArc(i);
			Name name = ai.getNameKey();
			if (name.isTempname()) continue;
			int newWidth = name.busWidth();
			int oldWidth = drawnWidths[drawn];
			if (oldWidth < 0) {
				drawnNames[drawn] = name;
				drawnWidths[drawn] = newWidth;
				continue;
			}
			if (oldWidth != newWidth)
				System.out.println("Network: Schematic cell " + cell.describe() + " has net with conflict width of names <" +
								   drawnNames[drawn] + "> and <" + name);
		}
		for (int i = 0; i < numArcs; i++) {
			int drawn = drawns[arcsOffset + i];
			if (drawn < 0) continue;
			ArcInst ai = cell.getArc(i);
			Name name = ai.getNameKey();
			if (!name.isTempname()) continue;
			int oldWidth = drawnWidths[drawn];
			if (oldWidth < 0)
				drawnNames[drawn] = name;
		}
		for (int i = 0; i < numNodes; i++) {
			NodeInst ni = cell.getNode(i);
			NodeProto np = ni.getProto();
			if (np instanceof PrimitiveNode) {
				if (np.getFunction() == NodeProto.Function.PIN) continue;
				if (np == Schematics.tech.offpageNode) continue;
			}
			int numPortInsts = np.getNumPorts();
			for (int j = 0; j < numPortInsts; j++) {
				PortInst pi = ni.getPortInst(j);
				int drawn = drawns[getPortInstOffset(pi)];
				if (drawn < 0) continue;
				int oldWidth = drawnWidths[drawn];
				int newWidth = 1;
				if (isSchem(np)) {
					NetSchem sch = (NetSchem)Network.getNetCell((Cell)np);
					int arraySize = np.isIcon() ? ni.getNameKey().busWidth() : 1;
					int portWidth = pi.getPortProto().getProtoNameKey().busWidth();
					if (oldWidth == portWidth) continue;
					newWidth = arraySize*portWidth;
				}
				if (oldWidth < 0) {
					drawnWidths[drawn] = newWidth;
					continue;
				}
				if (oldWidth != newWidth) 
					System.out.println("Network: Schematic cell " + cell.describe() + " has net <" +
						drawnNames[drawn] + "> with width conflict in connection " + pi.describe());
			}
		}
		for (int i = 0; i < drawnWidths.length; i++) {
			if (drawnWidths[i] < 1)
				drawnWidths[i] = 1;
			if (Network.debug ) System.out.println("Drawn "+i+" "+(drawnNames[i] != null ? drawnNames[i].toString() : "") +" has width " + drawnWidths[i]);
		}
		//showConnections();
		
	}

	void addNetNames(Name name) {
 		for (int i = 0; i < name.busWidth(); i++)
 			addNetName(name.subname(i));
	}

	private void mergeInternallyConnected()
	{
		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst)it.next();
			int nodeOffset = nodeOffsets[ni.getNodeIndex()];
			if (nodeOffset < 0) continue;
			mergeInternallyConnected(nodeOffset, ni);

			// Connect global primitives
			Global g = globalInst(ni);
			if (g != null)
				connectMap(globals.indexOf(g), nodeOffset);
		}
		for (int i = 0; i < nodeProxies.length; i++) {
			Proxy proxy = nodeProxies[i];
			if (proxy == null) continue;
			mergeInternallyConnected(proxy.nodeOffset, proxy.nodeInst);
		}
	}

	private void mergeInternallyConnected(int nodeOffset, NodeInst ni) {
		int[] eq = Network.getEquivPorts(ni.getProto());

		NetSchem sch = null;
		int numGlobals = 0;
		NodeProto np = ni.getProto();
		if (isSchem(np)) {
			sch = (NetSchem)Network.getNetCell((Cell)np);
			numGlobals = sch.portOffsets[0];
		}
		for (int i = 0; i < eq.length; i++) {
			int j = eq[i];
			if (i == j) continue;
			i = (i >= numGlobals ? nodeOffset + i : this.globals.indexOf(sch.globals.get(i)));
			j = (j >= numGlobals ? nodeOffset + j : this.globals.indexOf(sch.globals.get(j)));
			connectMap(i, j);
		}
	}

	private void mergeNetsConnectedByArcs()
	{
		for (Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst) it.next();
			if (ai.getProto().getFunction() == ArcProto.Function.NONELEC) continue;

			int ind = -1;
			if (ai.isUsernamed()) {
				Name arcNm = ai.getNameKey();
				for (int i = 0; i < arcNm.busWidth(); i++) {
					NetName nn = (NetName)netNames.get(arcNm.subname(i));
					int newInd = netNamesOffset + nn.index;
					if (ind < 0)
						ind = newInd;
					else
						connectMap(ind, newInd);
				}
			}
			ind = connectPortInst(ind, ai.getHead().getPortInst());
			ind = connectPortInst(ind, ai.getTail().getPortInst());
		}
	}

	private void addExportNamesToNets()
	{
		for (Iterator it = cell.getPorts(); it.hasNext();)
		{
			Export e = (Export) it.next();
			int portIndex = portOffsets[e.getPortIndex()];
			Name expNm = e.getProtoNameKey();
			for (int i = 0; i < expNm.busWidth(); i++) {
				NetName nn = (NetName)netNames.get(expNm.subname(i));
				connectMap(portIndex + i, netNamesOffset + nn.index);
			}
			connectPortInst(portIndex, e.getOriginalPort());
		}
	}

	private int connectPortInst(int oldInd, PortInst pi) {
		NodeInst ni = pi.getNodeInst();
		PortProto pp = pi.getPortProto();
		if (Network.debug) System.out.println("connectPortInst "+pp+" of "+ni);
		int busWidth = isSchem(ni.getProto()) ? pp.getProtoNameKey().busWidth() : 1;
		for (int busIndex = 0; busIndex < busWidth; busIndex++) {
			int portOffset = Network.getPortOffset(pp, busIndex);
			if (portOffset < 0) continue;
			int nodeOffset = nodeOffsets[ni.getNodeIndex()];
			if (nodeOffset >= 0) {
				int ind = nodeOffset + portOffset;
				if (oldInd < 0)
					oldInd = ind;
				else
					connectMap(oldInd, ind);
			} else {
				int proxyOffset = ~nodeOffset;
				for (int i = 0; i < ni.getNameKey().busWidth(); i++) {
					Proxy proxy = nodeProxies[proxyOffset + i];
					if (proxy == null) continue;
					nodeOffset = proxy.nodeOffset;
					int ind = nodeOffset + portOffset;
					if (oldInd < 0)
						oldInd = ind;
					else
						connectMap(oldInd, ind);
				}
			}
		}
		return oldInd;
	}

	private void buildNetworkList()
	{
		if (networks == null || networks.length != netMap.length)
			networks = new JNetwork[netMap.length];
		for (int i = 0; i < netMap.length; i++)
		{
			networks[i] = (netMap[i] == i ? new JNetwork(cell) : null);
		}
		for (int i = 0; i < globals.size(); i++) {
			networks[netMap[i]].addName(globals.get(i).getName());
		}
		for (Iterator it = netNames.values().iterator(); it.hasNext(); )
		{
			NetName nn = (NetName)it.next();
			if (nn.index < 0) continue;
			networks[netMap[netNamesOffset + nn.index]].addName(nn.name.toString());
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

	/**
	 * Update map of equivalent ports newEquivPort.
	 * @param currentTime time stamp of current network reevaluation
	 * or will be kept untouched if not.
	 */
	private boolean updateInterface() {
		boolean changed = false;
		for (int i = 0; i < equivPorts.length; i++) {
			if (equivPorts[i] != netMap[i]) {
				changed = true;
				equivPorts[i] = netMap[i];
			}
		}
		return changed;
	}

	void redoNetworks()
	{
		if ((flags & VALID) != 0) return;

		if (cell == null) {
			for (int i = 0; i < icons.length; i++)
				icons[i].updateInterface();
			flags |= (LOCALVALID|VALID);
			return;
		}

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
		if (portOffsets.length != dbExports.length + 1)
			portOffsets = new int[dbExports.length + 1];

		/* Set index of NodeInsts */
		int numDrawns = makeDrawns();
		if (drawnNames == null || drawnNames.length != numDrawns) {
			drawnNames = new Name[numDrawns];
			drawnWidths = new int[numDrawns];
		}
		calcDrawnWidths();

		if (initNodables()) changed = true;

		// Gather port and arc names
		int mapSize = netNamesOffset + initNetnames();

		if (netMap == null || netMap.length != mapSize)
			netMap = new int[mapSize];
		for (int i = 0; i < netMap.length; i++) netMap[i] = i;

		if (Network.debug) System.out.println("mergeInternallyConnected");
		mergeInternallyConnected();
		if (Network.debug) System.out.println("mergeNetsConnectedByArcs");
		mergeNetsConnectedByArcs();
		if (Network.debug) System.out.println("addExportNamesToNets");
		addExportNamesToNets();
		closureMap();
		buildNetworkList();
		if (updateInterface()) changed = true;
		if (changed)
			invalidateUsagesOf(true);
		for (int i = 0; i < icons.length; i++)
			icons[i].updateInterface();
		flags |= (LOCALVALID|VALID);
	}
}


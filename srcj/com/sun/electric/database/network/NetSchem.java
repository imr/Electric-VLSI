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
import com.sun.electric.database.topology.Connection;
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

	static class Icon extends NetCell {
		NetSchem schem;
		int[] iconToSchem;

		Icon(Cell iconCell) {
			super(iconCell);
			updateCellGroup(iconCell.getCellGroup());
		}

		void setSchem(NetSchem schem) {
			if (this.schem == schem) return;
			this.schem = schem;
			updateInterface();
		}

		int getPortOffset(int portIndex, int busIndex) {
			portIndex = iconToSchem[portIndex];
			if (portIndex < 0) return -1;
			if (schem == null) return -1;
			return schem.getPortOffset(portIndex, busIndex);
		}

		NetSchem getSchem() { return schem; }

		void redoNetworks() {
			if ((flags & VALID) != 0) return;

			if (schem != null && (schem.flags & VALID) == 0)
				schem.redoNetworks();

			if ((flags & LOCALVALID) != 0)
			{
				flags |= VALID;
				return;
			}

			if (updateInterface())
				super.invalidateUsagesOf(true);
			flags |= (LOCALVALID|VALID);
		}

		private boolean updateInterface() {
			boolean changed = false;
			int numPorts = cell.getNumPorts();
			if (iconToSchem == null || iconToSchem.length != numPorts) {
				changed = true;
				iconToSchem = new int[numPorts];
			}
			Cell c = schem != null ? schem.cell : null;
			for (int i = 0; i < numPorts; i++) {
				Export e = (Export)cell.getPort(i);
				int equivIndex = -1;
				if (c != null) {
					Export equiv = e.getEquivalentPort(c);
					if (equiv != null) equivIndex = equiv.getPortIndex();
				}
				if (iconToSchem[i] != equivIndex) {
					changed = true;
					iconToSchem[i] = equivIndex;
				}
			}
			return changed;
		}

		/**
		 * Get an iterator over all of the Nodables of this Cell.
		 */
		Iterator getNodables()
		{
			if ((flags & VALID) == 0) redoNetworks();
			return (new ArrayList()).iterator();
		}

		/**
		 * Get an iterator over all of the JNetworks of this Cell.
		 */
		Iterator getNetworks()
		{
			if ((flags & VALID) == 0) redoNetworks();
			return (new ArrayList()).iterator();
		}

		/*
		 * Get network by index in networks maps.
		 */
		JNetwork getNetwork(Nodable no, int arrayIndex, PortProto portProto, int busIndex) {
			if ((flags & VALID) == 0) redoNetworks();
			return null;
		}

		/*
		 * Get network of export.
		 */
		JNetwork getNetwork(Export export, int busIndex) {
			if ((flags & VALID) == 0) redoNetworks();
			return null;
		}

		/*
		 * Get network of arc.
		 */
		JNetwork getNetwork(ArcInst ai, int busIndex) {
			if ((flags & VALID) == 0) redoNetworks();
			return null;
		}

		/**
		 * Method to return either the network name or the bus name on this ArcInst.
		 * @return the either the network name or the bus name on this ArcInst.
		 */
		String getNetworkName(ArcInst ai) {
			if ((flags & VALID) == 0) redoNetworks();
			return null;
		}

		/**
		 * Method to return the bus width on this ArcInst.
		 * @return the either the bus width on this ArcInst.
		 */
		public int getBusWidth(ArcInst ai)
		{
			if ((flags & VALID) == 0) redoNetworks();
			return 0;
		}
	}

	static void updateCellGroup(Cell.CellGroup cellGroup) {
		Cell mainSchematics = cellGroup.getMainSchematics();
		NetSchem mainSchem = null;
		if (mainSchematics != null) mainSchem = (NetSchem)Network.getNetCell(mainSchematics);
		for (Iterator it = cellGroup.getCells(); it.hasNext();) {
			Cell cell = (Cell)it.next();
			if (cell.isIcon()) {
				NetSchem.Icon icon = (NetSchem.Icon)Network.getNetCell(cell);
				if (icon == null) continue;
				icon.setSchem(mainSchem);
				for (Iterator vit = cell.getVersions(); vit.hasNext();) {
					Cell verCell = (Cell)vit.next();
					if (verCell == cell) continue;
					icon = (NetSchem.Icon)Network.getNetCell(verCell);
					if (icon == null) continue;
					icon.setSchem(mainSchem);
				}
			}
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
		 * Method to return the prototype of this Nodable.
		 * @return the prototype of this Nodable.
		 */
		public NodeProto getProto() {
			NetSchem schem = Network.getNetCell((Cell)nodeInst.getProto()).getSchem();
			return schem.cell;
		}

		/**
		 * Method to return the Cell that contains this Nodable.
		 * @return the Cell that contains this Nodable.
		 */
		public Cell getParent() { return cell; }

		/**
		 * Method to return the name of this Nodable.
		 * @return the name of this Nodable.
		 */
		public String getName() { return getNameKey().toString(); }

		/**
		 * Method to return the name key of this Nodable.
		 * @return the name key of this Nodable.
		 */
		public Name getNameKey() { return nodeInst.getNameKey().subname(arrayIndex); }

		/**
		 * Method to return the Variable on this Nodable with a given name.
		 * @param name the name of the Variable.
		 * @return the Variable with that name, or null if there is no such Variable.
		 */
		public Variable getVar(String name) { return nodeInst.getVar(name); }

		/**
		 * Method to get network by PortProto and bus index.
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

	/** Node offsets. */											int[] nodeOffsets;
	/** Node offsets. */											int[] drawnOffsets;
	/** Node offsets. */											Proxy[] nodeProxies;
	/** */															Global.Set globals = Global.Set.empty;
	/** */															int[] portOffsets = new int[1];
	/** */															int netNamesOffset;

	/** */															Name[] drawnNames;
	/** */															int[] drawnWidths;

	NetSchem(Cell cell) {
		super(cell);
		updateCellGroup(cell.getCellGroup());
	}

	NetSchem(Cell.CellGroup cellGroup) {
		super(cellGroup.getMainSchematics());
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

	int getPortOffset(int portIndex, int busIndex) {
		int portOffset = portOffsets[portIndex] + busIndex;
		if (busIndex < 0 || portOffset >= portOffsets[portIndex+1]) return -1;
		return portOffset - portOffsets[0];
	}

	NetSchem getSchem() { return this; }

	static int getPortOffset(PortProto pp, int busIndex) {
		int portIndex = pp.getPortIndex();
		NodeProto np = pp.getParent();
		if (!(np instanceof Cell))
			return busIndex == 0 ? portIndex : -1;
		NetCell netCell = Network.getNetCell((Cell)np);
		return netCell.getPortOffset(portIndex, busIndex);
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
		Proxy proxy;
		if (no instanceof NodeInst) {
			NodeInst ni = (NodeInst)no;
			int nodeIndex = ni.getNodeIndex();
			int proxyOffset = nodeOffsets[nodeIndex];
			if (proxyOffset >= 0) {
				int drawn = drawns[ni_pi[nodeIndex] + portProto.getPortIndex()];
				if (drawn < 0) return null;
				if (busIndex < 0 || busIndex >= drawnWidths[drawn]) return null;
				return networks[netMap[drawnOffsets[drawn] + busIndex]];
			}
			proxy = nodeProxies[~proxyOffset + arrayIndex];
			NetCell netCell = Network.getNetCell((Cell)ni.getProto());
		} else {
			proxy = (Proxy)no;
		}
		if (proxy == null) return null;
		int portOffset = NetSchem.getPortOffset(portProto, busIndex);
		if (portOffset < 0) return null;
		return networks[netMap[proxy.nodeOffset + portOffset]];
	}

	/*
	 * Get network of export.
	 */
	JNetwork getNetwork(Export export, int busIndex) {
		if ((flags & VALID) == 0) redoNetworks();
		int drawn = drawns[export.getPortIndex()];
		if (drawn < 0) return null;
		if (busIndex < 0 || busIndex >= drawnWidths[drawn]) return null;
		return networks[netMap[drawnOffsets[drawn] + busIndex]];
	}

	/*
	 * Get network of arc.
	 */
	JNetwork getNetwork(ArcInst ai, int busIndex) {
		if ((flags & VALID) == 0) redoNetworks();
		int drawn = drawns[arcsOffset + ai.getArcIndex()];
		if (drawn < 0) return null;
		if (busIndex < 0 || busIndex >= drawnWidths[drawn]) return null;
		return networks[netMap[drawnOffsets[drawn] + busIndex]];
	}

	/**
	 * Method to return either the network name or the bus name on this ArcInst.
	 * @return the either the network name or the bus n1ame on this ArcInst.
	 */
	String getNetworkName(ArcInst ai) {
		if ((flags & VALID) == 0) redoNetworks();
		int drawn = drawns[arcsOffset + ai.getArcIndex()];
		if (drawn < 0) return null;
		if (drawnWidths[drawn] == 1) {
			JNetwork network = networks[netMap[drawnOffsets[drawn]]];
			if (network == null) return null;
			return network.describe();
		}
		if (drawnNames[drawn] == null) return null;
		return drawnNames[drawn].toString();
	}

	/**
	 * Method to return the bus width on this ArcInst.
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
		super.invalidateUsagesOf(strong);
		for (Iterator it = cell.getCellGroup().getCells(); it.hasNext();) {
			Cell c = (Cell)it.next();
			if (!c.isIcon()) continue;
			Network.getNetCell(c).invalidateUsagesOf(strong);
			for (Iterator vit = c.getVersions(); vit.hasNext();) {
				Cell verCell = (Cell)vit.next();
				if (verCell == c) continue;
				Network.getNetCell(verCell).invalidateUsagesOf(strong);
			}
		}
	}

	private boolean initNodables() {
		if (nodeOffsets == null || nodeOffsets.length != cell.getNumNodes())
			nodeOffsets = new int[cell.getNumNodes()];
		int numNodes = cell.getNumNodes();
		Global.clearBuf();
		int nodeProxiesOffset = 0;
		for (int i = 0; i < numNodes; i++) {
			NodeInst ni = cell.getNode(i);
			NodeProto np = ni.getProto();
			NetCell netCell = null;
			if (np instanceof Cell)
				netCell = Network.getNetCell((Cell)np);
			if (netCell != null && (netCell instanceof NetSchem.Icon || netCell instanceof NetSchem)) {
				if (ni.getNameKey().hasDuplicates())
					System.out.println(cell + ": Node name <"+ni.getNameKey()+"> has duplicate subnames");
				nodeOffsets[i] = ~nodeProxiesOffset;
				nodeProxiesOffset += ni.getNameKey().busWidth();
			} else {
				if (ni.getNameKey().isBus())
					System.out.println(cell + ": Array name <"+ni.getNameKey()+"> can be assigned only to icon nodes");
				nodeOffsets[i] = 0;
			}
			if (netCell != null) {
				NetSchem sch = Network.getNetCell((Cell)np).getSchem();
				if (sch != null) Global.addToBuf(sch.globals);
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
		int mapOffset = portOffsets[0] = globals.size();
		int numPorts = cell.getNumPorts();
		for (int i = 1; i <= numPorts; i++) {
			Export export = (Export)cell.getPort(i - 1);
			if (Network.debug) System.out.println(export+" "+portOffsets[i-1]);
			mapOffset += export.getProtoNameKey().busWidth();
			if (portOffsets[i] != mapOffset) {
				changed = true;
				portOffsets[i] = mapOffset;
			}
		}
		if (equivPorts == null || equivPorts.length != mapOffset)
			equivPorts = new int[mapOffset];

		for (int i = 0; i < numDrawns; i++) {
			drawnOffsets[i] = mapOffset;
			mapOffset += drawnWidths[i];
		}
		if (nodeProxies == null || nodeProxies.length != nodeProxiesOffset)
			nodeProxies = new Proxy[nodeProxiesOffset];
		for (int n = 0; n < numNodes; n++) {
			NodeInst ni = (NodeInst)cell.getNode(n);
			int proxyOffset = nodeOffsets[n];
			if (Network.debug) System.out.println(ni+" "+proxyOffset);
			if (proxyOffset >= 0) continue;
			NetSchem netSchem = Network.getNetCell((Cell)ni.getProto()).getSchem();
			if (ni.isIconOfParent()) netSchem = null;
			for (int i = 0; i < ni.getNameKey().busWidth(); i++) {
				Proxy proxy = null;
				if (netSchem != null) {
					proxy = new Proxy(ni, i);
					if (Network.debug) System.out.println(proxy+" "+mapOffset+" "+netSchem.equivPorts.length);
					proxy.nodeOffset = mapOffset;
					mapOffset += (netSchem.equivPorts.length - netSchem.globals.size())/*portsSize(ni.getProto())*/;
				}
				nodeProxies[~proxyOffset + i] = proxy;
			}
		}
		netNamesOffset = mapOffset;
		if (Network.debug) System.out.println("netNamesOffset="+netNamesOffset);
		return changed;
	}

// 	private static int portsSize(NodeProto np) {
// 		if (np instanceof PrimitiveNode) return np.getNumPorts();
// 		NetCell netCell = (NetCell) Network.getNetCell((Cell)np);
// 		NetSchem sch = netCell.getSchem();
// 		if (sch == null) return np.getNumPorts();
// 		return sch.equivPorts.length - sch.globals.size();
// 	}

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
			Name name = cell.getPort(i).getProtoNameKey();
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
				if (np instanceof Cell) {
					NetCell netCell = Network.getNetCell((Cell)np);
					if (netCell instanceof NetSchem.Icon || netCell instanceof NetSchem) {
						int arraySize = np.isIcon() ? ni.getNameKey().busWidth() : 1;
						int portWidth = pi.getPortProto().getProtoNameKey().busWidth();
						if (oldWidth == arraySize*portWidth) continue;
						newWidth = portWidth;
					}
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
			if (Network.debug) System.out.println("Drawn "+i+" "+(drawnNames[i] != null ? drawnNames[i].toString() : "") +" has width " + drawnWidths[i]);
		}
	}

	void addNetNames(Name name) {
 		for (int i = 0; i < name.busWidth(); i++)
 			addNetName(name.subname(i));
	}

	private void localConnections() {

		// Exports
		int numExports = cell.getNumPorts();
		for (int k = 0; k < numExports; k++) {
			Export e = (Export) cell.getPort(k);
			int portOffset = portOffsets[k];
			Name expNm = e.getProtoNameKey();
			int busWidth = expNm.busWidth();
			int drawn = drawns[k];
			int drawnOffset = drawnOffsets[drawn];
			if (busWidth != drawnWidths[drawn]) continue;
			for (int i = 0; i < busWidth; i++) {
				connectMap(portOffset + i, drawnOffset + i);
				NetName nn = (NetName)netNames.get(expNm.subname(i));
				connectMap(portOffset + i, netNamesOffset + nn.index);
			}
		}

		// PortInsts
		int numNodes = cell.getNumNodes();
		for (int k = 0; k < numNodes; k++) {
			NodeInst ni = cell.getNode(k);
			NodeProto np = ni.getProto();
			if (np instanceof PrimitiveNode) {
				// Connect global primitives
				Global g = globalInst(ni);
				if (g != null) {
					int drawn = drawns[ni_pi[k]];
					connectMap(globals.indexOf(g), drawnOffsets[drawn]);
				}
				if (np == Schematics.tech.wireConNode)
					connectWireCon(ni);
				continue;
			}
			if (nodeOffsets[k] >= 0) continue;
			NetCell netCell = Network.getNetCell((Cell)np);
			NetSchem schem = netCell.getSchem();
			if (schem == null) continue;
			Name nodeName = ni.getNameKey();
			int arraySize = nodeName.busWidth();
			int proxyOffset = nodeOffsets[k];
			int numPorts = np.getNumPorts();
			for (int m = 0; m < numPorts; m++) {
				Export e = (Export) np.getPort(m);
				int portIndex = m;
				if (netCell instanceof NetSchem.Icon)
					portIndex = ((NetSchem.Icon)netCell).iconToSchem[portIndex];
				int portOffset = schem.portOffsets[portIndex] - schem.portOffsets[0];
				int busWidth = e.getProtoNameKey().busWidth();
				int drawn = drawns[ni_pi[k] + m];
				if (drawn < 0) continue;
				int width = drawnWidths[drawn];
				if (width != busWidth && width != busWidth*arraySize) continue;
				for (int i = 0; i < arraySize; i++) {
					Proxy proxy = nodeProxies[~proxyOffset + i];
					if (proxy == null) continue;
					int nodeOffset = proxy.nodeOffset + portOffset;
					int busOffset = drawnOffsets[drawn];
					if (width != busWidth) busOffset += busWidth*i;
					for (int j = 0; j < busWidth; j++)
						connectMap(busOffset + j, nodeOffset + j);
				}
			}
		}

		// Arcs
		int numArcs = cell.getNumArcs();
		for (int k = 0; k < numArcs; k++) {
			ArcInst ai = (ArcInst) cell.getArc(k);
			int drawn = drawns[arcsOffset + k];
			if (drawn < 0) continue;
			if (!ai.isUsernamed()) continue;
			int busWidth = drawnWidths[drawn];
			Name arcNm = ai.getNameKey();
			if (arcNm.busWidth() != busWidth) continue;
			int drawnOffset = drawnOffsets[drawn];
			for (int i = 0; i < busWidth; i++) {
				NetName nn = (NetName)netNames.get(arcNm.subname(i));
				connectMap(drawnOffset + i, netNamesOffset + nn.index);
			}
		}
	}

	private void connectWireCon (NodeInst ni) {
		ArcInst ai1 = null;
		ArcInst ai2 = null;
		for (Iterator it = ni.getConnections(); it.hasNext();) {
			Connection con = (Connection)it.next();
			ArcInst ai = con.getArc();
			if (ai1 == null) {
				ai1 = ai;
			} else if (ai2 == null) {
				ai2 = ai;
			} else {
				System.out.println("Network: Schematic cell " + cell.describe() + " has connector " + ni.describe() + 
					" which merges more than two arcs");
				return;
			}
		}
		if (ai2 == null || ai1 == ai2) return;
		int large = drawns[arcsOffset + ai1.getArcIndex()];
		int small = drawns[arcsOffset + ai2.getArcIndex()];
		if (large < 0 || small < 0) return;
		if (drawnWidths[small] > drawnWidths[large]) {
			int temp = small;
			small = large;
			large = temp;
		}
		for (int i = 0; i < drawnWidths[large]; i++)
			connectMap(drawnOffsets[large] + i, drawnOffsets[small] + (i % drawnWidths[small]));
	}

	private void internalConnections()
	{
		int numNodes = cell.getNumNodes();
		for (int k = 0; k < numNodes; k++) {
			NodeInst ni = cell.getNode(k);
			int nodeOffset = ni_pi[k];
			NodeProto np = ni.getProto();
			if (np instanceof PrimitiveNode) {
				if (np == Schematics.tech.resistorNode && Network.shortResistors)
					connectMap(drawns[nodeOffset], drawns[nodeOffset + 1]);
				continue;
			}
			NetCell netCell = Network.getNetCell((Cell)np);
			if (nodeOffsets[k] < 0) continue;
			int[] eq = netCell.equivPorts;
			for (int i = 0; i < eq.length; i++) {
				int j = eq[i];
				if (i == j) continue;
				int di = drawns[nodeOffset + i];
				int dj = drawns[nodeOffset + j];
				if (di < 0 || dj < 0) continue;
				connectMap(di, dj);
			}
		}
		for (int k = 0; k < nodeProxies.length; k++) {
			Proxy proxy = nodeProxies[k];
			if (proxy == null) continue;
			NodeProto np = proxy.getProto();
			NetSchem schem = (NetSchem)Network.getNetCell((Cell)np);
			int numGlobals = schem.portOffsets[0];
			int nodeOffset = proxy.nodeOffset - numGlobals;
			int[] eq = schem.equivPorts;
			for (int i = 0; i < eq.length; i++) {
				int j = eq[i];
				if (i == j) continue;
				int io = (i >= numGlobals ? nodeOffset + i : this.globals.indexOf(schem.globals.get(i)));
				int jo = (j >= numGlobals ? nodeOffset + j : this.globals.indexOf(schem.globals.get(j)));
				connectMap(io, jo);
			}
		}
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
		System.out.println("BuildNetworkList "+cell);
		int i = 0;
		for (int l = 0; l < networks.length; l++) {
			JNetwork network = networks[l];
			if (network == null) continue;
			String s = "";
			for (Iterator sit = network.getNames(); sit.hasNext(); )
			{
				String n = (String)sit.next();
				s += "/"+ n;
			}
			System.out.println("    "+i+"    "+s);
			i++;
			for (int k = 0; k < globals.size(); k++) {
				if (networks[netMap[k]] != network) continue;
				System.out.println("\t" + globals.get(k));
			}
			int numPorts = cell.getNumPorts();
			for (int k = 0; k < numPorts; k++) {
				Export e = (Export) cell.getPort(k);
				for (int j = 0; j < e.getProtoNameKey().busWidth(); j++) {
					if (networks[netMap[portOffsets[k] + j]] != network) continue;
					System.out.println("\t" + e + " [" + j + "]");
				}
			}
			for (int k = 0; k < numDrawns; k++) {
				for (int j = 0; j < drawnWidths[k]; j++) {
					if (networks[netMap[drawnOffsets[k] + j]] != network) continue;
					System.out.println("\tDrawn " + k + " [" + j + "]");
				}
			}
			for (Iterator it = netNames.values().iterator(); it.hasNext();) {
				NetName nn = (NetName)it.next();
				if (networks[netMap[netNamesOffset + nn.index]] != network) continue;
				System.out.println("\tNetName " + nn.name);
			}
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

	boolean redoNetworks1()
	{
		int numPorts = cell.getNumPorts();
		if (portOffsets.length != numPorts + 1)
			portOffsets = new int[numPorts + 1];

		/* Set index of NodeInsts */
		if (drawnNames == null || drawnNames.length != numDrawns) {
			drawnNames = new Name[numDrawns];
			drawnWidths = new int[numDrawns];
			drawnOffsets = new int[numDrawns];
		}
		calcDrawnWidths();

		boolean changed = initNodables();
		// Gather port and arc names
		int mapSize = netNamesOffset + netNames.size();

		if (netMap == null || netMap.length != mapSize)
			netMap = new int[mapSize];
		for (int i = 0; i < netMap.length; i++) netMap[i] = i;

		localConnections();
		internalConnections();
		closureMap();
		buildNetworkList();
		if (updateInterface()) changed = true;
		return changed;
	}
}


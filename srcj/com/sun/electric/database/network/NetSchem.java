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
// import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
// import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;

// import java.util.Arrays;
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
				if (dbIconExports[e.getIndex()] != e) {
					changed = true;
					dbIconExports[e.getIndex()] = e;
				}
				int portInd = -1;
				if (cell != null) {
					Export equiv = e.getEquivalentPort(cell);
					if (equiv != null) portInd = equiv.getIndex();
				}
				if (iconToSchem[e.getIndex()] != portInd) {
					changed = true;
					iconToSchem[e.getIndex()] = portInd;
				}
			}
			if (changed)
				invalidateUsagesOf(true);
		}
	}

	private class Proxy {
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
			return Network.getNetwork(nodeInst, arrayIndex, portProto, busIndex);
		}

		/**
		 * Returns a printable version of this Nodable.
		 * @return a printable version of this Nodable.
		 */
		public String toString() { return "NetSchem.Proxy " + getName(); }

	}

	private Icon[] icons;
	/** Node offsets. */											Proxy[] nodeProxies;

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
			if (c.isIcon())
				iconCount++;
		}
		icons = new Icon[iconCount];
		iconCount = 0;
		for (Iterator it = cellGroup.getCells(); it.hasNext();) {
			Cell c = (Cell)it.next();
			if (c.isIcon()) {
				icons[iconCount] = new Icon(c);
				Network.setCell(c, this, ~iconCount);
				iconCount++;
			}
		}
	}

	void mergeInternallyConnected()
	{
		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst)it.next();
			int nodeOffset = nodeOffsets[ni.getIndex()];
			if (nodeOffset < 0) continue;
			mergeInternallyConnected(nodeOffset, ni);
		}
		for (int i = 0; i < nodeProxies.length; i++) {
			Proxy proxy = nodeProxies[i];
			if (proxy == null) continue;
			mergeInternallyConnected(proxy.nodeOffset, proxy.nodeInst);
		}
	}

	private void mergeInternallyConnected(int nodeOffset, NodeInst ni) {
		int[] eq = Network.getEquivPorts(ni.getProto());
		for (int i = 0; i < eq.length; i++) {
			if (eq[i] == i) continue;
			connectMap(netMap, nodeOffset + i, nodeOffset + eq[i]);
		}
	}

	int getPortOffset(int iconIndex, int portIndex, int busIndex) {
		int portInd = icons[~iconIndex].iconToSchem[portIndex];
		if (portInd < 0) return -1;
		return portInd + busIndex;
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
			if (nodeOffsets[ni.getIndex()] < 0) continue;
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
			nodeOffset = nodeOffsets[ni.getIndex()];
			if (nodeOffset < 0) {
				Proxy proxy = nodeProxies[~nodeOffset];
				if (proxy == null) return null;
				nodeOffset = proxy.nodeOffset;
			}
		}
		return networks[netMap[nodeOffset + portOffset]];
	}

	void invalidateUsagesOf(boolean strong)
	{
		if (cell != null)
			super.invalidateUsagesOf(strong);	
		for (int i = 0; i < icons.length; i++)
			icons[i].invalidateUsagesOf(strong);
	}

	int initNodables() {
		if (nodeOffsets == null || nodeOffsets.length != cell.getNumNodes())
			nodeOffsets = new int[cell.getNumNodes()];
		int nodeOffset = 0;
		int nodeProxiesOffset = 0;
		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst)it.next();
			int nodeIndex = ni.getIndex();
			NodeProto np = ni.getProto();
			if (np.isIcon()) {
				nodeOffsets[nodeIndex] = ~nodeProxiesOffset;
				nodeProxiesOffset += 1;
			} else {
				nodeOffsets[nodeIndex] = nodeOffset;
				nodeOffset += np.getNumPorts();
			}
		}
		if (nodeProxies == null || nodeProxies.length != nodeProxiesOffset)
			nodeProxies = new Proxy[nodeProxiesOffset];
		for (Iterator it = cell.getNodes(); it.hasNext();) {
			NodeInst ni = (NodeInst)it.next();
			int proxyOffset = nodeOffsets[ni.getIndex()];
			if (proxyOffset >= 0) continue;
			NetCell netCell = Network.getNetCell((Cell)ni.getProto());
			if (netCell != this && netCell.cell != null) {
				Proxy proxy = new Proxy(ni, 0);
				nodeProxies[~proxyOffset] = proxy;
				proxy.nodeOffset = nodeOffset;
				nodeOffset += proxy.getProto().getNumPorts();
			} else {
				nodeProxies[~proxyOffset] = null;
			}
		}
		return nodeOffset;
	}

	int connectPortInst(int oldInd, PortInst pi) {
		int portOffset = Network.getPortOffset(pi.getPortProto(), 0);
		if (portOffset < 0) return oldInd;
		int nodeOffset = nodeOffsets[pi.getNodeInst().getIndex()];
		if (nodeOffset < 0) {
			Proxy proxy = nodeProxies[~nodeOffset];
			if (proxy == null) return oldInd;
			nodeOffset = proxy.nodeOffset;
		}
		int ind = nodeOffset + portOffset;
		if (oldInd < 0) return ind;
		connectMap(netMap, oldInd, ind);
		return oldInd;
	}

	void updateInterface() {
		if (cell != null)
			super.updateInterface();
		for (int i = 0; i < icons.length; i++) {
			icons[i].updateInterface();
		}
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
			int npInd = np.getIndex();
			if (npInd < 0) continue;
			NetCell netCell = Network.getNetCell((Cell)np);
			if ((netCell.flags & VALID) == 0)
				netCell.redoNetworks();
		}

		if ((flags & LOCALVALID) != 0)
		{
			flags |= VALID;
			return;
		}

		if (cell == null) {
			updateInterface();
			flags |= (LOCALVALID|VALID);
			return;
		}

		/* Set index of NodeInsts */
		int mapOffset = initNodables();

		// Gather port and arc names
		mapOffset = initNetnames(mapOffset);

		if (netMap == null || netMap.length != mapOffset)
			netMap = new int[mapOffset];
		for (int i = 0; i < netMap.length; i++) netMap[i] = i;

		mergeInternallyConnected();
		mergeNetsConnectedByArcs();
		addExportNamesToNets();
		closureMap(netMap);
		buildNetworkList();
		updateInterface();
		flags |= (LOCALVALID|VALID);
	}
}


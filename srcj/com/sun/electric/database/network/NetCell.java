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
import com.sun.electric.database.hierarchy.NodeInstProxy;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;

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

	/** If bit set, netlist is valid for cell tree.*/				private static final int VALID = 1;
	/** If bit set, netlist is valid with current  equivPorts of subcells.*/private static final int LOCALVALID = 2;

	/** Cell from database. */										final Cell cell;
	/** Flags of this NetCell. */									int flags;

	/**
     * Equivalence of ports. equivPorts.size == ports.size.
	 * equivPorts[i] contains minimal index among ports of its group.
     */																int[] equivPorts;
	/** A map from name key to NodeInstProxy. */					private Map proxies;
	/** A map from Name to NetName. */								private Map netNames = new HashMap();
	/** An equivalence map of PortInsts and NetNames. */			private int[] netMap;
	/** An array of JNetworks in this Cell. */						private JNetwork[] networks;

	NetCell(Cell cell)
	{
		this.cell = cell;

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
	}

	final void setNetworksDirty()
	{
		if ((flags & LOCALVALID) != 0)
			setInvalid(true);
	}

	/**
	 * Get an iterator over all of the JNetworks of this Cell.
	 * <p> Warning: before getNetworks() is called, JNetworks must be
	 * build by calling Cell.rebuildNetworks()
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

	/*
	 * Get network by index in networks maps.
	 */
	JNetwork getNetwork(int index)
	{
		if ((flags & VALID) == 0) redoNetworks();
		return networks[netMap[index]];
	}

	void setInvalid(boolean strong)
	{
		if (strong) flags &= ~LOCALVALID;
		if ((flags & VALID) == 0) return;
		flags &= ~VALID;
		for (Iterator it = cell.getUsagesOf(); it.hasNext(); )
		{
			NodeUsage nu = (NodeUsage)it.next();
			Network.cells[nu.getParent().getIndex()].setInvalid(false);
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

	private void placeEachPortInstOnItsOwnNet()
	{
		for (Iterator uit = cell.getUsagesIn(); uit.hasNext();)
		{
			NodeUsage nu = (NodeUsage) uit.next();
			if (nu.isIcon()) continue;
			if (nu.getProto().getFunction() == NodeProto.Function.ART) continue;

			int[] eq = Network.getEquivPorts(nu.getProto());
			for (Iterator iit = nu.getInsts(); iit.hasNext();)
			{
				NodeInst ni = (NodeInst)iit.next();
				int ind = ni.getIndex();
				for (int i = 0; i < eq.length; i++)
				{
					if (eq[i] == i) continue;
					connectMap(netMap, ind + i, ind + eq[i]);
				}
			}
			for (Iterator iit = nu.getProxies(); iit.hasNext();)
			{
				NodeInstProxy nip = (NodeInstProxy)iit.next();
				int ind = nip.getIndex();
				for (int i = 0; i < eq.length; i++)
				{
					if (eq[i] == i) continue;
					connectMap(netMap, ind + i, ind + eq[i]);
				}
			}
		}
	}

	private void mergeNetsConnectedByArcs()
	{
		for (Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst) it.next();
			if (ai.getProto().getFunction() == ArcProto.Function.NONELEC) continue;

			PortInst pi0 = ai.getConnection(false).getPortInst();
			NodeInst ni0 = pi0.getNodeInst();
			int ind0 = ni0.getIndex();
			if (ind0 >= 0)
			{
				ind0 += pi0.getPortProto().getIndex();
			} else if (ni0.getProto().isIcon())
			{
				NodeInstProxy nip = ni0.getSubinst(0).getProxy();
				PortProto pp = pi0.getPortProto().getEquivalent();
				if (nip != null && pp != null)
					ind0 = nip.getIndex() + pp.getIndex();
			}

			PortInst pi1 = ai.getConnection(true).getPortInst();
			NodeInst ni1 = pi1.getNodeInst();
			int ind1 = ni1.getIndex();
			if (ind1 >= 0)
			{
				ind1 += pi1.getPortProto().getIndex();
			} else if (ni1.getProto().isIcon())
			{
				NodeInstProxy nip = ni1.getSubinst(0).getProxy();
				PortProto pp = pi1.getPortProto().getEquivalent();
				if (nip != null && pp != null)
					ind1 = nip.getIndex() + pp.getIndex();
			}

			if (ind0 >= 0 && ind1 >= 0) connectMap(netMap, ind0, ind1);

			Name arcNm = ai.getNameKey();
			if (!ai.isUsernamed()) continue;
			NetName nn = (NetName)netNames.get(arcNm);
			if (ind0 >= 0) connectMap(netMap, nn.index, ind0);
			if (ind1 >= 0) connectMap(netMap, nn.index, ind1);
		}
	}

	private void addExportNamesToNets()
	{
		for (Iterator it = cell.getPorts(); it.hasNext();)
		{
			Export e = (Export) it.next();
			Name expNm = e.getProtoNameKey();
			NetName nn = (NetName)netNames.get(expNm);
			PortInst pi = e.getOriginalPort();
			NodeInst ni = pi.getNodeInst();
			int ind = ni.getIndex();
			if (ind >= 0)
			{
				ind += pi.getPortProto().getIndex();
			} else if (ni.getProto().isIcon())
			{
				NodeInstProxy nip = ni.getSubinst(0).getProxy();
				PortProto pp = pi.getPortProto().getEquivalent();
				if (nip != null && pp != null)
					ind = nip.getIndex() + pp.getIndex();
			}
			if (ind >= 0)
				connectMap(netMap, nn.index, ind);
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
		for (Iterator it = netNames.values().iterator(); it.hasNext(); )
		{
			NetName nn = (NetName)it.next();
			if (nn.index < 0) continue;
			networks[netMap[nn.index]].addName(nn.name.toString());
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
	private void updateEquivPorts()
	{
		int[] newEquivPorts = new int[cell.getNumPorts()];
		for (int i = 0; i < newEquivPorts.length; i++) newEquivPorts[i] = i;

		/* Connect ports connected by node proto subnets */
		int[] netToPort = new int[networks.length];
		for (int i = 0; i < netToPort.length; i++) netToPort[i] = -1;
		int i = 0;
		for (Iterator it = cell.getPorts(); it.hasNext(); i++)
		{
			Export e = (Export) it.next();
			NetName nn = (NetName)netNames.get(e.getProtoNameKey());
			int netNum = netMap[nn.index];
			int iOld = netToPort[netNum];
			if (iOld >= 0)
			{
				connectMap(newEquivPorts, iOld, i);
			} else
			{
				netToPort[netNum] = i;
			}
		}
		closureMap(newEquivPorts);

		/* if equivalent ports changed, invalidate user cells */
		if (equivPorts == null || !equalMaps(equivPorts, newEquivPorts))
		{
			equivPorts = newEquivPorts;
			for (Iterator it = cell.getUsagesOf(); it.hasNext(); )
			{
				NodeUsage nu = (NodeUsage)it.next();
				Network.cells[nu.getParent().getIndex()].flags &= ~LOCALVALID;
			}
		}
		//showEquivPorts();
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
			int npInd = np.getIndex();
			if (npInd < 0) continue;
			NetCell netCell = Network.cells[npInd];
			if ((netCell.flags & VALID) == 0)
				netCell.redoNetworks();
		}

		if ((flags & LOCALVALID) != 0)
		{
			flags |= VALID;
			return;
		}

		/* Set index of NodeInsts */
		int index = 0;
		for (Iterator uit = cell.getUsagesIn(); uit.hasNext();)
		{
			NodeUsage nu = (NodeUsage) uit.next();
			if (nu.isIcon()) continue;
			if (nu.getProto().getFunction() == NodeProto.Function.ART) continue;

			NodeProto np = nu.getProto();
			for (Iterator iit = nu.getInsts(); iit.hasNext();)
			{
				NodeInst ni = (NodeInst)iit.next();
				ni.lowLevelSetIndex(index);
				index += np.getNumPorts();
			}
			for (Iterator iit = nu.getProxies(); iit.hasNext();)
			{
				NodeInstProxy nip = (NodeInstProxy)iit.next();
				nip.setIndex(index);
				index += np.getNumPorts();
			}
		}

		// Gather names
		for (Iterator it = netNames.values().iterator(); it.hasNext(); )
		{
			NetName nn = (NetName)it.next();
			nn.name = null;
			nn.index = -1;
		}
		for (Iterator it = cell.getPorts(); it.hasNext();)
		{
			Export e = (Export) it.next();
			Name expNm = e.getProtoNameKey();
			NetName nn = (NetName)netNames.get(expNm);
			if (nn == null)
			{
				nn = new NetName();
				netNames.put(expNm.lowerCase(), nn);
			}
			if (nn.index < 0)
			{
				nn.name = expNm;
				nn.index = index++;
			}
		}
		for (Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst) it.next();
			if (!ai.isUsernamed()) continue;
			Name arcNm = ai.getNameKey();
			NetName nn = (NetName)netNames.get(arcNm);
			if (nn == null)
			{
				nn = new NetName();
				netNames.put(arcNm.lowerCase(), nn);
			}
			if (nn.index < 0)
			{
				nn.name = arcNm;
				nn.index = index++;
			}
		}

		if (netMap == null || netMap.length != index)
			netMap = new int[index];
		for (int i = 0; i < netMap.length; i++) netMap[i] = i;
		placeEachPortInstOnItsOwnNet();
		mergeNetsConnectedByArcs();
		addExportNamesToNets();
		closureMap(netMap);
		buildNetworkList();
		updateEquivPorts();
		flags |= (LOCALVALID|VALID);
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
}


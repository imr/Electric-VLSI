/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OutputTopology.java
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
package com.sun.electric.tool.io;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.io.Output;

import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;

/**
 * This is the Simulation Interface tool.
 */
public abstract class OutputTopology extends Output
{
	/** number of unique cells processed */				protected int numVisited;
	/** number of unique cells to process */			protected int numCells;
	/** top-level cell being processed */				protected Cell topCell;

	/** HashMap of all CellTopologies */				protected HashMap cellTopos;

	/** Creates a new instance of OutputTopology */
	OutputTopology() 
	{
	}

	/** 
	 * Write cell to file
	 * @return true on error
	 */
	public boolean writeCell(Cell cell) 
	{
		writeCell(cell, new Visitor(this));
		return false;
	}

	/** 
	 * Write cell to file
	 * @return true on error
	 */
	public boolean writeCell(Cell cell, Visitor visitor) 
	{
		// see how many cells we have to write, for progress indication
		numVisited = 0;
		numCells = HierarchyEnumerator.getNumUniqueChildCells(cell) + 1;
		topCell = cell;
		cellTopos = new HashMap();

		makeCellNameMap();

		// write out cells
		start();
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, null, visitor);
		done();
		return false;
	}


	/** Abstract method called before hierarchy traversal */
	protected abstract void start();

	/** Abstract method called after traversal */
	protected abstract void done();

	/** Abstract method to write CellGeom to disk */
	protected abstract void writeCellTopology(Cell cell, CellNets cn);

	/** Abstract method to convert a network name to one that is safe for this format */
	protected abstract String getSafeNetName(String name);

	/** Abstract method to convert a cell name to one that is safe for this format */
	protected abstract String getSafeCellName(String name);

	/** Abstract method to convert a cell name to one that is safe for this format */
	protected abstract Netlist getNetlistForCell(Cell cell);

	protected CellNets getCellNets(Cell cell)
	{
		CellNets cn = (CellNets)cellTopos.get(cell);
		return cn;
	}
	private HashMap cellNameMap;

	/*
	 * determine whether any cells have name clashes in other libraries
	 */
	private void makeCellNameMap()
	{
		cellNameMap = new HashMap();
		for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = (Library)lIt.next();
			if (lib.isHidden()) continue;
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				boolean duplicate = false;
				for(Iterator oLIt = Library.getLibraries(); oLIt.hasNext(); )
				{
					Library oLib = (Library)oLIt.next();
					if (oLib.isHidden()) continue;
					if (oLib == lib) continue;
					for(Iterator oCIt = oLib.getCells(); oCIt.hasNext(); )
					{
						Cell oCell = (Cell)oCIt.next();
						if (cell.getProtoName().equalsIgnoreCase(oCell.getProtoName()))
						{
							duplicate = true;
							break;
						}
					}
					if (duplicate) break;
				}

				if (duplicate) cellNameMap.put(cell, cell.getLibrary().getLibName() + "__" + cell.getProtoName()); else
					cellNameMap.put(cell, cell.getProtoName());
			}
		}
	}

	/*
	 * Method to return the name of cell "c", given that it may be ambiguously used in multiple
	 * libraries.
	 */
	protected String getUniqueCellName(Cell cell)
	{
		String name = (String)cellNameMap.get(cell);
		return name;
	}

	//------------------HierarchyEnumerator.Visitor Implementation----------------------

	public class Visitor extends HierarchyEnumerator.Visitor
	{
		/** OutputTopology object this Visitor is enumerating for */	private OutputTopology outGeom;

		public Visitor(OutputTopology outGeom)
		{
			this.outGeom = outGeom;
		}

		public boolean enterCell(HierarchyEnumerator.CellInfo info) 
		{
			Cell cell = info.getCell();
			if (cellTopos.containsKey(cell)) return false;	// already processed this Cell

			Netlist netList = getNetlistForCell(cell);
			CellNets cn = sim_vergetnetworks(cell, false);
//printWriter.print("********Decomposition of cell " + cell.describe() + "\n");
//printWriter.print(" Power is " + cn.pwrNet + "\n");
//printWriter.print(" Ground is " + cn.gndNet + "\n");
//printWriter.print(" Have " + cn.wireLists.size() + " networks:\n");
//for(Iterator it = cn.wireLists.values().iterator(); it.hasNext(); )
//{
//	OutputTopology.WireList wl = (OutputTopology.WireList)it.next();
//	printWriter.print("   Temp="+wl.temp1+", port="+wl.pp+", name="+wl.name+", net="+wl.net+"\n");
//}
//printWriter.print(" Have " + cn.portInfo.size() + " signals:\n");
//for(Iterator it = cn.portInfo.iterator(); it.hasNext(); )
//{
//	OutputTopology.NetInfo nin = (OutputTopology.NetInfo)it.next();
//	printWriter.print("   Temp="+nin.temp1+", port="+nin.pp+", name="+nin.name+", low="+nin.low+", high="+nin.high+"\n");
//}
			cellTopos.put(cell, cn);
			return true;
		}

		public void exitCell(HierarchyEnumerator.CellInfo info) 
		{
			// write cell
			Cell cell = info.getCell();
			CellNets cn = (CellNets)cellTopos.get(cell);
			outGeom.writeCellTopology(cell, cn);
		}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) 
		{
			NodeProto np = no.getProto();
			if (np instanceof PrimitiveNode) return false;
//	protected String parameterizedName(NodeInst ni, String cellname)

			// else just a cell
			return true;
		}
	}

	//---------------------------- Utility Methods --------------------------------------

	static class WireList
	{
		int temp1;
		JNetwork net;
		Export pp;
		/** name to use for this network. */	String name;
		/** true if part of a descending bus */	boolean descending;
		/** true if from a global signal */		boolean isGlobal;
		/** scratch word for the subclass */	int flags;
	}

	static class NetInfo
	{
		int temp1;
		WireList [] wires;
		Export pp;
		String name;
		int low;
		int high;
	}

	static class CellNets
	{
		HashMap wireLists;
		List portInfo;
		JNetwork pwrNet;
		JNetwork gndNet;
	}

	/*
	 * Method to scan networks in cell "cell".  The "temp1" field is filled in with
	 *    0: internal network (ascending order when in a bus)
	 *    1: internal network (descending order when in a bus)
	 *    2: exported input network (ascending order when in a bus)
	 *    3: exported input network (descending order when in a bus)
	 *    4: exported output network (ascending order when in a bus)
	 *    5: exported output network (descending order when in a bus)
	 *    6: power or ground network
	 * All networks are sorted by name within "temp1" and the common array entries are
	 * reduced to a list of names (in "namelist") and their low/high range of indices
	 * (in "lowindex" and "highindex", with high < low if no indices apply).  The value
	 * of "temp1" is returned in the array "tempval".  The list of "netcount" networks
	 * found (uncombined by index) is returned in "wirelist".  The power and ground nets
	 * are stored in "pwrNet" and "gndNet".  The total number of names is returned.
	 */
	private CellNets sim_vergetnetworks(Cell cell, boolean quiet)
	{
		// get network information about this cell
		Netlist netList = getNetlistForCell(cell);

		// create a map of all nets in the cell
		HashMap wireListMap = new HashMap();
		for(Iterator it = netList.getNetworks(); it.hasNext(); )
		{
			JNetwork net = (JNetwork)it.next();
			WireList wl = new WireList();
			wl.net = net;
			wl.temp1 = 0;
			wl.flags = 0;
			wireListMap.put(net, wl);
		}

		// determine default direction of busses
		int dirBit = 1;
		if (Network.isBusAscending()) dirBit = 0;

		// mark exported networks
		for(Iterator it = cell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			int j = dirBit + 2;
			if (pp.getCharacteristic() == PortProto.Characteristic.OUT) j = dirBit + 4;

			// mark every network on the bus (or just 1 network if not a bus)
			int portWidth = netList.getBusWidth(pp);
			for(int i=0; i<portWidth; i++)
			{
				JNetwork net = netList.getNetwork(pp, i);
				WireList wl = (WireList)wireListMap.get(net);
				wl.temp1 = j;
			}
		}

//		// postprocess to ensure the directionality is correct
//		for(Iterator it = netList.getNetworks(); it.hasNext(); )
//		{
//			JNetwork net = (JNetwork)it.next();
//			WireList wl = (WireList)wireListMap.get(net);
//			if (wl == null) continue;
//			for(Iterator nIt = net.getNames(); nIt.hasNext(); )
//			{
//				String netName = (String)nIt.next();
//				for(Iterator eIt = cell.getPorts(); eIt.hasNext(); )
//				{
//					Export pp = (Export)eIt.next();
//					int portWidth = netList.getBusWidth(pp);
//					if (portWidth > 1) continue;
//					if (!pp.getProtoName().equals(netName)) continue;
//					if (pp.getCharacteristic() == PortProto.Characteristic.OUT)
//						wl.temp1 = dirBit + 4; else
//							wl.temp1 = dirBit + 2;
//				}
//			}
//		}

		// make sure all busses go in the same direction
		for(Iterator eIt = cell.getPorts(); eIt.hasNext(); )
		{
			Export pp = (Export)eIt.next();
			int portWidth = netList.getBusWidth(pp);
			if (portWidth <= 1) continue;
			int j = 0;
			boolean sameDir = true;
			for(int i=0; i<portWidth; i++)
			{
				JNetwork subNet = netList.getNetwork(pp, i);
				WireList wl = (WireList)wireListMap.get(subNet);
				if (j == 0) j = wl.temp1;
				if (wl.temp1 != j) { sameDir = false;   break; }
			}
			if (sameDir) continue;

			// mixed directionality: make it all nonoutput
			for(int i=0; i<portWidth; i++)
			{
				JNetwork subNet = netList.getNetwork(pp, i);
				WireList wl = (WireList)wireListMap.get(subNet);
				wl.temp1 = 2+dirBit;
			}
		}

		// scan all networks for those that go in descending order
		for(Iterator eIt = cell.getPorts(); eIt.hasNext(); )
		{
			Export pp = (Export)eIt.next();
			int portWidth = netList.getBusWidth(pp);
			if (portWidth <= 1) continue;
			boolean upDir = false, downDir = false, randomDir = false;
			int last = 0;
			for(int i=0; i<portWidth; i++)
			{
				JNetwork subNet = netList.getNetwork(pp, i);
				if (!subNet.hasNames()) break;
				String firstName = (String)subNet.getNames().next();
				int openSquare = firstName.indexOf('[');
				if (openSquare < 0) break;
				if (!Character.isDigit(firstName.charAt(openSquare+1))) break;
				int index = TextUtils.atoi(firstName.substring(openSquare+1));
				if (i != 0)
				{
					if (index == last-1) downDir = true; else
						if (index == last+1) upDir = true; else
							randomDir = true;
				}
				last = index;
			}
			if (randomDir) continue;
			if (upDir && downDir) continue;
			if (!upDir && !downDir) continue;
			if (downDir) dirBit = 1; else
				dirBit = 0;
			for(int i=0; i<portWidth; i++)
			{
				JNetwork subNet = netList.getNetwork(pp, i);
				WireList wl = (WireList)wireListMap.get(subNet);
				wl.temp1 = (wl.temp1 & ~1) | dirBit;
			}
		}

		// find power and ground
		JNetwork pwrNet = null, gndNet = null;
		boolean multiPwr = false, multiGnd = false;
		for(Iterator eIt = cell.getPorts(); eIt.hasNext(); )
		{
			Export pp = (Export)eIt.next();
			int portWidth = netList.getBusWidth(pp);
			if (portWidth > 1) continue;
			JNetwork subNet = netList.getNetwork(pp, 0);
			if (pp.isPower())
			{
				if (pwrNet != null && pwrNet != subNet && !multiPwr)
				{
					if (!quiet)
						System.out.println("Warning: multiple power networks in cell " + cell.describe());
					multiPwr = true;
				}
				pwrNet = subNet;
			}
			if (pp.isGround())
			{
				if (gndNet != null && gndNet != subNet && !multiGnd)
				{
					if (!quiet)
						System.out.println("Warning: multiple ground networks in cell " + cell.describe());
					multiGnd = true;
				}
				gndNet = subNet;
			}
		}
		Global.Set globals = netList.getGlobals();
		int globalSize = globals.size();
//		for(net = cell->firstnetwork; net != NONETWORK; net = net->nextnetwork)
//		{
//			if (net->globalnet >= 0 && net->globalnet < cell->globalnetcount)
//			{
//				characteristics = cell->globalnetchar[net->globalnet];
//				if (characteristics == PWRPORT)
//				{
//					if (*pwrNet != NONETWORK && *pwrNet != net && !multiPwr)
//					{
//						if (!quiet)
//							ttyputmsg("Warning: multiple power networks in cell %s",
//								describenodeproto(cell));
//						multiPwr = TRUE;
//					}
//					*pwrNet = net;
//				} else if (characteristics == GNDPORT)
//				{
//					if (*gndNet != NONETWORK && *gndNet != net && !multiGnd)
//					{
//						if (!quiet)
//							ttyputmsg("Warning: multiple ground networks in cell %s",
//								describenodeproto(cell));
//						multiGnd = TRUE;
//					}
//					*gndNet = net;
//				}
//			}
//		}
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto.Function fun = ni.getFunction();
			if (fun == NodeProto.Function.CONPOWER || fun == NodeProto.Function.CONGROUND)
			{
				for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
				{
					Connection con = (Connection)cIt.next();
					ArcInst ai = con.getArc();
					JNetwork subNet = netList.getNetwork(ai, 0);
					if (fun == NodeProto.Function.CONPOWER)
					{
						if (pwrNet != null && pwrNet != subNet && !multiPwr)
						{
							if (!quiet)
								System.out.println("Warning: multiple power networks in cell " + cell.describe());
							multiPwr = true;
						}
						pwrNet = subNet;
					} else
					{
						if (gndNet != null && gndNet != subNet && !multiGnd)
						{
							if (!quiet)
								System.out.println("Warning: multiple ground networks in cell " + cell.describe());
							multiGnd = true;
						}
						gndNet = subNet;
					}
				}
			}
		}
		if (pwrNet != null)
		{
			WireList wl = (WireList)wireListMap.get(pwrNet);
			wl.temp1 = 6;
		}
		if (gndNet != null)
		{
			WireList wl = (WireList)wireListMap.get(gndNet);
			wl.temp1 = 6;
		}

		// find the widest export associated with each network
		for(Iterator it = netList.getNetworks(); it.hasNext(); )
		{
			JNetwork net = (JNetwork)it.next();
			WireList wl = (WireList)wireListMap.get(net);
			if (wl.temp1 == 0) continue;

			// find the widest export that touches this network
			int widestFound = -1;
			Export widestPp = null;
			for(Iterator eIt = cell.getPorts(); eIt.hasNext(); )
			{
				Export pp = (Export)eIt.next();
				int portWidth = netList.getBusWidth(pp);
				boolean found = false;
				for(int j=0; j<portWidth; j++)
				{
					JNetwork subNet = netList.getNetwork(pp, j);
					if (subNet == net) found = true;
				}
				if (found)
				{
					if (portWidth > widestFound)
					{
						widestFound = portWidth;
						widestPp = pp;
					}
				}
			}
			if (widestPp != null) wl.pp = widestPp;
		}

		// make an array of the WireList objects
		List wireListArray = new ArrayList();
		for(Iterator it = wireListMap.values().iterator(); it.hasNext(); )
			wireListArray.add(it.next());

		// sort the networks by name
		Collections.sort(wireListArray, new SortNetsByName());

		// organize by name and index order
		List netInfoList = new ArrayList();
		int total = wireListArray.size();
		int emptyName = 1;
		for(int i=0; i<total; i++)
		{
			WireList wl = (WireList)wireListArray.get(i);
			JNetwork net = wl.net;

			// see if it is global
			int netIndex = net.getNetIndex();
			Global globalObj = null;
			for(int j=0; j<globalSize; j++)
			{
				Global global = (Global)globals.get(j);
				if (netList.getNetIndex(global) == netIndex) { globalObj = global;   break; }
			}

			// add this name to the list
			String name = "";
			if (globalObj != null)
			{
				wl.isGlobal = true;
				name = globalObj.getName();
			} else
			{
				if (net.hasNames()) name = (String)net.getNames().next(); else
				{
					name = net.describe();
				}

				// if exported, be sure to get the export name
//				if (wl.temp1 > 1 && wl.temp1 < 6)
//					name = wl.pp.getProtoName();
			}

			NetInfo nin = new NetInfo();
			nin.name = sim_verstartofindex(name);
			nin.temp1 = wl.temp1;
			nin.pp = wl.pp;
			if (nin.name.equals(name))
			{
				// single wire: set range to show that
				nin.low = 1;
				nin.high = 0;
				nin.wires = new WireList[1];
				nin.wires[0] = wl;
			} else
			{
				nin.high = nin.low = TextUtils.atoi(name.substring(nin.name.length()+1));
				int start = i;
				for(int j=i+1; j<total; j++)
				{
					WireList wlEnd = (WireList)wireListArray.get(j);
					JNetwork endNet = wlEnd.net;
					if (endNet == null) break;
					if (wlEnd.temp1 != wl.temp1) break;
					if (wlEnd.pp != wl.pp) break;
					if (wlEnd.isGlobal != wl.isGlobal) break;
					if (!endNet.hasNames()) break;
					String endName = (String)endNet.getNames().next();
					String ept = sim_verstartofindex(endName);
					if (ept.equals(endName)) break;
					int index = TextUtils.atoi(endName.substring(ept.length()+1));

					// make sure export indices go in order
					if (index != nin.high+1) break;
					if (index > nin.high) nin.high = index;
					i = j;
				}
//				nin.name = name;
				nin.wires = new WireList[i-start+1];
				for(int j=start; j<=i; j++)
				{
					WireList wlEnd = (WireList)wireListArray.get(j);
					nin.wires[j-start] = wlEnd;
				}
			}
			netInfoList.add(nin);
		}

		// for single signals, give them a verilog name
		for(Iterator it = netInfoList.iterator(); it.hasNext(); )
		{
			NetInfo nin = (NetInfo)it.next();
			nin.name =  getSafeNetName(nin.name);
//			if (nin.low == nin.high) nin.name = nin.name + "_" + nin.low + "_";
		}

		// make sure all names are unique
		int numNameList = netInfoList.size();
		for(int i=1; i<numNameList; i++)
		{
			// single signal: give it that name
			NetInfo nin = (NetInfo)netInfoList.get(i);

			// see if the name clashes
			for(int k=0; k<1000; k++)
			{
				String ninName = nin.name;
				if (k > 0) ninName = ninName + "_" + k;
				boolean found = false;
				for(int j=0; j<i; j++)
				{
					NetInfo oNin = (NetInfo)netInfoList.get(j);
					if (!ninName.equals(oNin.name)) { found = true;   break; }
				}
				if (!found)
				{
					if (k > 0) nin.name = ninName;
					break;
				}
			}
		}

		for(Iterator it = netInfoList.iterator(); it.hasNext(); )
		{
			NetInfo nin = (NetInfo)it.next();
			if (nin.low > nin.high)
			{
				WireList wl = nin.wires[0];
				wl.name = nin.name;
			} else
			{
				boolean descending = false;
				if ((nin.temp1&1) != 0) descending = true; 
				for(int k=nin.low; k<=nin.high; k++)
				{
					WireList wl = nin.wires[k-nin.low];
					wl.name = nin.name + "[" + k + "]";
					wl.descending = descending;
				}
			}
		}

		CellNets cn = new CellNets();
		cn.wireLists = wireListMap;
		cn.portInfo = netInfoList;
		cn.pwrNet = pwrNet;
		cn.gndNet = gndNet;
		return cn;
	}

	/*
	 * Method to return the character position in network name "name" that is the start of indexing.
	 * If there is no indexing ("clock"), this will point to the end of the string.
	 * If there is simple indexing ("dog[12]"), this will point to the "[".
	 * If the index is nonnumeric ("dog[cat]"), this will point to the end of the string.
	 * If there are multiple indices, ("dog[12][45]") this will point to the last "[" (unless it is nonnumeric).
	 */
	protected static String sim_verstartofindex(String name)
	{
		int len = name.length();
		if (len == 0) return name;
		if (name.charAt(len-1) != ']') return name;
		int i = len - 2;
		for( ; i > 0; i--)
		{
			char theChr = name.charAt(i);
			if (theChr == '[') break;
			if (theChr == ':' || theChr == ',') continue;
			if (!Character.isDigit(theChr)) break;
		}
		if (name.charAt(i) != '[') return name;
		return name.substring(0, i);
	}

	static class SortNetsByName implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			WireList wl1 = (WireList)o1;
			WireList wl2 = (WireList)o2;
			if (wl1.temp1 != wl2.temp1) return wl1.temp1 - wl2.temp1;
			String s1 = "";
			if (wl1.net.hasNames()) s1 = (String)wl1.net.getNames().next();
			String s2 = "";
			if (wl2.net.hasNames()) s2 = (String)wl2.net.getNames().next();
			return TextUtils.nameSameNumeric(s1, s2);
		}
	}

	/*
	 * Method to create a parameterized name for node instance "ni".
	 * If the node is not parameterized, returns zero.
	 * If it returns a name, that name must be deallocated when done.
	 */
	protected String parameterizedName(NodeInst ni, String cellname)
	{
		if (!(ni.getProto() instanceof Cell)) return null;

		// must have parameter variables on it
		boolean hasParameters = false;
		for(Iterator it = ni.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.getTextDescriptor().isParam()) { hasParameters = true;   break; }
		}
		if (!hasParameters) return null;

		// generate the parameterized name
		StringBuffer sb = new StringBuffer();
		sb.append(cellname);
		for(Iterator it = ni.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.getTextDescriptor().isParam()) continue;

//            Object eval = context.evalVar(var, ni);
//
//			sb.append("-");
//			sb.append(var.getTrueName());
//			sb.append("-");
//			sb.append(eval.toString());
		}
		return sb.toString();
	}

}

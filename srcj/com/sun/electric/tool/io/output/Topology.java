/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Topology.java
 * Input/output tool: superclass for output modules that write connectivity.
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.PrimitiveNode;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;

/**
 * This is the Simulation Interface tool.
 */
public abstract class Topology extends Output
{
	/** top-level cell being processed */				protected Cell topCell;

	/** HashMap of all CellTopologies */				private HashMap cellTopos;
	/** HashMap of all Cell names */					private HashMap cellNameMap;

	/** Creates a new instance of Topology */
	public Topology() 
	{
	}

	/** 
	 * Write cell to file
	 * @return true on error
	 */
	public boolean writeCell(Cell cell, VarContext context)
	{
		writeCell(cell, context, new Visitor(this));
		return false;
	}

	/** 
	 * Write cell to file
	 * @return true on error
	 */
	public boolean writeCell(Cell cell, VarContext context, Visitor visitor)
	{
		// remember the top-level cell
		topCell = cell;

		// clear the map of CellNetInfo for each processed cell
		cellTopos = new HashMap();

		// make a map of cell names to use (unique across libraries)
		makeCellNameMap();

		// write out cells
		start();
		HierarchyEnumerator.enumerateCell(cell, context, null, visitor);
		done();
		return false;
	}


	/** Abstract method called before hierarchy traversal */
	protected abstract void start();

	/** Abstract method called after traversal */
	protected abstract void done();

	/** Abstract method to write CellGeom to disk */
	protected abstract void writeCellTopology(Cell cell, CellNetInfo cni, VarContext context);

	/** Abstract method to convert a network name to one that is safe for this format */
	protected abstract String getSafeNetName(String name);

	/** Abstract method to convert a cell name to one that is safe for this format */
	protected abstract String getSafeCellName(String name);

	/** Abstract method to return the proper name of Power (or null to use existing name) */
	protected abstract String getPowerName(Network net);

	/** Abstract method to return the proper name of Ground (or null to use existing name) */
	protected abstract String getGroundName(Network net);

	/** Abstract method to return the proper name of a Global signal */
	protected abstract String getGlobalName(Global glob);

	/** Abstract method to decide whether export names take precedence over
	 * arc names when determining the name of the network. */
	protected abstract boolean isNetworksUseExportedNames();

	/** Abstract method to decide whether library names are always prepended to cell names. */
	protected abstract boolean isLibraryNameAlwaysAddedToCellName();

	/** Abstract method to decide whether aggregate names (busses) are used. */
	protected abstract boolean isAggregateNamesSupported();

    /** If the netlister has requirments not to netlist certain cells and their
     * subcells, override this method. */
    protected boolean skipCellAndSubcells(Cell cell) { return false; }

	/**
	 * Method to tell whether the topological analysis should mangle cell names that are parameterized.
	 */
	protected boolean canParameterizeNames() { return false; }

	/**
	 * Method to tell set a limit on the number of characters in a name.
	 * @return the limit to name size (0 if no limit). 
	 */
	protected int maxNameLength() { return 0; }

	/** Abstract method to convert a cell name to one that is safe for this format */
	protected abstract Netlist getNetlistForCell(Cell cell);

	protected CellNetInfo getCellNetInfo(String cellName)
	{
		CellNetInfo cni = (CellNetInfo)cellTopos.get(cellName);
		return cni;
	}

	//------------------ override for HierarchyEnumerator.Visitor ----------------------

	public class MyCellInfo extends HierarchyEnumerator.CellInfo
	{
		String currentInstanceParametizedName;
	}

	public class Visitor extends HierarchyEnumerator.Visitor
	{
		/** Topology object this Visitor is enumerating for */	private Topology outGeom;

		public Visitor(Topology outGeom)
		{
			this.outGeom = outGeom;
		}

		public boolean enterCell(HierarchyEnumerator.CellInfo info) 
		{
            if (skipCellAndSubcells(info.getCell())) return false;
			return true;
		}

		public void exitCell(HierarchyEnumerator.CellInfo info) 
		{
			// write cell
			Cell cell = info.getCell();
			CellNetInfo cni = null;
			if (info.isRootCell())
			{
				cni = getNetworkInformation(cell, false, cell.getName(), isNetworksUseExportedNames());
			} else
			{
				MyCellInfo mci = (MyCellInfo)info;
				mci = (MyCellInfo)mci.getParentInfo();
				cni = getCellNetInfo(mci.currentInstanceParametizedName);
			}
			outGeom.writeCellTopology(cell, cni, info.getContext());
		}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) 
		{
			NodeProto np = no.getProto();
			if (np instanceof PrimitiveNode) return false;

			MyCellInfo mci = (MyCellInfo)info;
			VarContext context = info.getContext();
			mci.currentInstanceParametizedName = parameterizedName(no, context);

			if (cellTopos.containsKey(mci.currentInstanceParametizedName)) return false;	// already processed this Cell

			Cell cell = (Cell)np;
			Netlist netList = getNetlistForCell(cell);
			CellNetInfo cni = getNetworkInformation(cell, false, mci.currentInstanceParametizedName, isNetworksUseExportedNames());
			cellTopos.put(mci.currentInstanceParametizedName, cni);

			// else just a cell
			return true;
		}

		public HierarchyEnumerator.CellInfo newCellInfo()
		{
			return new MyCellInfo();
		}
	}

	//---------------------------- Utility Methods --------------------------------------

	/**
	 * Class to describe a single signal in a Cell.
	 */
	protected static class CellSignal
	{
		/** name to use for this network. */		private String name;
		/** Network for this signal. */				private Network net;
		/** CellAggregate that this is part of. */	private CellAggregateSignal aggregateSignal;
		/** export that this is part of. */			private Export pp;
        /** if export is bussed, index of signal */ private int ppIndex;
		/** true if part of a descending bus */		private boolean descending;
		/** true if a power signal */				private boolean power;
		/** true if a ground signal */				private boolean ground;
		/** true if from a global signal */			private Global globalSignal;

		protected String getName() { return name; }
		protected Network getNetwork() { return net; }
		protected Export getExport() { return pp; }
        protected int getExportIndex() { return ppIndex; }
		protected CellAggregateSignal getAggregateSignal() { return aggregateSignal; }
		protected boolean isDescending() { return descending; }
		protected boolean isGlobal() { return globalSignal != null; }
		protected boolean isPower() { return power; }
		protected boolean isGround() { return ground; }
		protected boolean isExported() { return pp != null; }
	}

	/**
	 * Class to describe an aggregate signal (a bus) in a Cell.
	 * The difference between aggregate signals and busses is that aggregate signals
	 * are more sensitive to variations.  For example, a bus may be X[1,2,4,5] but
	 * there must be two aggregate signals describing it because of the noncontinuity
	 * of the indices (thus, the aggregate signals will be X[1:2] and X_1[4:5]).
	 * Other factors that cause busses to be broken into multiple aggregate signals are:
	 * (1) differences in port direction (mixes of input and output, for example)
	 * (2) differences in port type (power, ground, global, etc.)
	 */
	protected static class CellAggregateSignal
	{
		private String name;
		private Export pp;
        private int ppIndex;
		private int low;
		private int high;
		private boolean supply;
		private boolean descending;
		private CellSignal [] signals;
		/** scratch word for the subclass */	private int flags;

		protected String getName() { return name; }
		protected String getNameWithIndices()
		{
			if (low > high) return name;
			int lowIndex = low, highIndex = high;
			if (descending)
			{
				lowIndex = high;   highIndex = low;
			}
			return name + "[" + lowIndex + ":" + highIndex + "]";
		}
		protected CellSignal getSignal(int index) { return signals[index]; }
		protected boolean isDescending() { return descending; }
		protected boolean isSupply() { return supply; }
		protected Export getExport() { return pp; }
        protected int getExportIndex() { return ppIndex; }
		protected int getLowIndex() { return low; }
		protected int getHighIndex() { return high; }
		protected int getFlags() { return flags; }
		protected void setFlags(int flags) { this.flags = flags; }
		protected boolean isGlobal()
		{
			int numGlobal = 0;
			for(int i=0; i<signals.length; i++)
			{
				CellSignal cs = signals[i];
				if (cs.isGlobal()) numGlobal++;
			}
			if (numGlobal > 0 && numGlobal == signals.length) return true;
			return false;
		}
	}

	/**
	 * Class to describe the networks in a Cell.
	 * This contains all of the CellSignal and CellAggregateSignal information.
	 */
	protected static class CellNetInfo
	{
		private String paramName;
		private HashMap cellSignals;
		private List cellSignalsSorted;
		private List cellAggretateSignals;
		private Network pwrNet;
		private Network gndNet;
		private Netlist netList;

		protected CellSignal getCellSignal(Network net) { return (CellSignal)cellSignals.get(net); }
		protected Iterator getCellSignals() { return cellSignalsSorted.iterator(); }
		protected Iterator getCellAggregateSignals() { return cellAggretateSignals.iterator(); }
		protected String getParameterizedName() { return paramName; }
		protected Network getPowerNet() { return pwrNet; }
		protected Network getGroundNet() { return gndNet; }
		protected Netlist getNetList() { return netList; }
	}

	private CellNetInfo getNetworkInformation(Cell cell, boolean quiet, String paramName, boolean useExportedName)
	{
		CellNetInfo cni = doGetNetworks(cell, quiet, paramName, useExportedName);
//printWriter.print("********Decomposition of cell " + cell.describe() + "\n");
//printWriter.print("** Have " + cni.cellSignalsSorted.size() + " signals:\n");
//for(Iterator it = cni.getCellSignals(); it.hasNext(); )
//{
//	CellSignal cs = (CellSignal)it.next();
//	printWriter.print("**   Name="+cs.name+" export="+cs.pp+" index="+cs.ppIndex+" descending="+cs.descending+" power="+cs.power+" ground="+cs.ground+" global="+cs.globalSignal+"\n");
//}
//if (isAggregateNamesSupported())
//{
//	printWriter.print("** Have " + cni.cellAggretateSignals.size() + " aggregate signals:\n");
//	for(Iterator it = cni.cellAggretateSignals.iterator(); it.hasNext(); )
//	{
//		CellAggregateSignal cas = (CellAggregateSignal)it.next();
//		printWriter.print("**   Name="+cas.name+", export="+cas.pp+" descending="+cas.descending+", low="+cas.low+", high="+cas.high+"\n");
//	}
//}
//printWriter.print("********DONE WITH CELL " + cell.describe() + "\n");
		return cni;
	}

	private CellNetInfo doGetNetworks(Cell cell, boolean quiet, String paramName, boolean useExportedName)
	{
		// create the object with cell net information
		CellNetInfo cni = new CellNetInfo();
		cni.paramName = paramName;

		// get network information about this cell
		cni.netList = getNetlistForCell(cell);
		Global.Set globals = cni.netList.getGlobals();
		int globalSize = globals.size();

		// create a map of all nets in the cell
		cni.cellSignals = new HashMap();
		int nullNameCount = 1;
		for(Iterator it = cni.netList.getNetworks(); it.hasNext(); )
		{
			Network net = (Network)it.next();
			CellSignal cs = new CellSignal();
			cs.pp = null;
			cs.descending = !NetworkTool.isBusAscending();
			cs.power = false;
			cs.ground = false;
			cs.net = net;

			// see if it is global
			cs.globalSignal = null;
			for(int j=0; j<globalSize; j++)
			{
				Global global = globals.get(j);
				if (cni.netList.getNetwork(global) == net) { cs.globalSignal = global;   break; }
			}

			// name the signal
			if (cs.globalSignal != null)
			{
				cs.name = getGlobalName(cs.globalSignal);
			} else
			{
				if (net.hasNames())
                {
                    if (useExportedName && net.getExportedNames().hasNext())
					{
						cs.name = (String)net.getExportedNames().next();
                    } else
                    {
						cs.name = (String)net.getNames().next();
                    }
                } else
				{
					cs.name = net.describe();
					if (cs.name.equals(""))
						cs.name = "UNCONNECTED" + (nullNameCount++);
				}
			}

			// save it in the map of signals
 			cni.cellSignals.put(net, cs);
		}

		// mark exported networks
		for(Iterator it = cell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();

			// mark every network on the bus (or just 1 network if not a bus)
			int portWidth = cni.netList.getBusWidth(pp);
			for(int i=0; i<portWidth; i++)
			{
				Network net = cni.netList.getNetwork(pp, i);
				CellSignal cs = (CellSignal)cni.cellSignals.get(net);
				if (cs == null) continue;

				// if there is already an export on this signal, make sure that it is wider
				if (cs.pp != null)
				{
					int oldPortWidth = cni.netList.getBusWidth(cs.pp);
					if (isAggregateNamesSupported())
					{
						// with aggregate names, the widest bus is the best, so that long runs can be emitted
						if (oldPortWidth >= portWidth) continue;
					} else
					{
						// without aggregate names, individual signal names are more important
						if (oldPortWidth == 1) continue;
						if (portWidth != 1 && oldPortWidth >= portWidth) continue;
					}
				}

				// save this export's information
				cs.pp = pp;
				cs.ppIndex = i;
				if (useExportedName)
				{
					String rootName = pp.getName();
					if (portWidth == 1) cs.name = rootName;
					int openPos = rootName.indexOf('[');
					if (openPos > 0)
					{
						rootName = rootName.substring(0, openPos);
						for(Iterator nIt = net.getExportedNames(); nIt.hasNext(); )
						{
							String exportNetName = (String)nIt.next();
							String exportRootName = exportNetName;
							openPos = exportRootName.indexOf('[');
							if (openPos > 0)
							{
								exportRootName = exportRootName.substring(0, openPos);
								if (rootName.equals(exportRootName))
								{
									cs.name = rootName + exportNetName.substring(openPos);
								}
							}
						}
					}
				}
			}

			if (portWidth <= 1) continue;
			Network [] nets = new Network[portWidth];
			for(int i=0; i<portWidth; i++)
				nets[i] = cni.netList.getNetwork(pp, i);
			setBusDirectionality(nets, cni);
		}

		// look at all busses and mark directionality
		for(Iterator aIt = cell.getArcs(); aIt.hasNext(); )
		{
			ArcInst ai = (ArcInst)aIt.next();
			int width = cni.netList.getBusWidth(ai);
			if (width < 2) continue;
			Network [] nets = new Network[width];
			for(int i=0; i<width; i++)
				nets[i] = cni.netList.getNetwork(ai, i);
			setBusDirectionality(nets, cni);
		}

		// find power and ground
		cni.pwrNet = cni.gndNet = null;
		boolean multiPwr = false, multiGnd = false;
		for(Iterator eIt = cell.getPorts(); eIt.hasNext(); )
		{
			Export pp = (Export)eIt.next();
			int portWidth = cni.netList.getBusWidth(pp);
			if (portWidth > 1) continue;
			Network subNet = cni.netList.getNetwork(pp, 0);
			if (pp.isPower())
			{
				if (cni.pwrNet != null && cni.pwrNet != subNet && !multiPwr)
				{
					if (!quiet)
						System.out.println("Warning: multiple power networks in cell " + cell.describe());
					multiPwr = true;
				}
				cni.pwrNet = subNet;
			}
			if (pp.isGround())
			{
				if (cni.gndNet != null && cni.gndNet != subNet && !multiGnd)
				{
					if (!quiet)
						System.out.println("Warning: multiple ground networks in cell " + cell.describe());
					multiGnd = true;
				}
				cni.gndNet = subNet;
			}
		}
		for(Iterator it = cni.netList.getNetworks(); it.hasNext(); )
		{
			Network net = (Network)it.next();
			CellSignal cs = (CellSignal)cni.cellSignals.get(net);
			if (cs.globalSignal != null)
			{
				if (cs.globalSignal == Global.power)
				{
					if (cni.pwrNet != null && cni.pwrNet != net && !multiPwr)
					{
						if (!quiet)
							System.out.println("Warning: multiple power networks in cell " + cell.describe());
						multiPwr = true;
					}
					cni.pwrNet = net;
				}
				if (cs.globalSignal == Global.ground)
				{
					if (cni.gndNet != null && cni.gndNet != net && !multiGnd)
					{
						if (!quiet)
							System.out.println("Warning: multiple ground networks in cell " + cell.describe());
						multiGnd = true;
					}
					cni.gndNet = net;
				}
			}
		}
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			PrimitiveNode.Function fun = ni.getFunction();
			if (fun == PrimitiveNode.Function.CONPOWER || fun == PrimitiveNode.Function.CONGROUND)
			{
				for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
				{
					Connection con = (Connection)cIt.next();
					ArcInst ai = con.getArc();
					Network subNet = cni.netList.getNetwork(ai, 0);
					if (fun == PrimitiveNode.Function.CONPOWER)
					{
						if (cni.pwrNet != null && cni.pwrNet != subNet && !multiPwr)
						{
							if (!quiet)
								System.out.println("Warning: multiple power networks in cell " + cell.describe());
							multiPwr = true;
						}
						cni.pwrNet = subNet;
					} else
					{
						if (cni.gndNet != null && cni.gndNet != subNet && !multiGnd)
						{
							if (!quiet)
								System.out.println("Warning: multiple ground networks in cell " + cell.describe());
							multiGnd = true;
						}
						cni.gndNet = subNet;
					}
				}
			}
		}
		if (cni.pwrNet != null)
		{
			CellSignal cs = (CellSignal)cni.cellSignals.get(cni.pwrNet);
			String powerName = getPowerName(cni.pwrNet);
			if (powerName != null) cs.name = powerName;
			cs.power = true;
		}
		if (cni.gndNet != null)
		{
			CellSignal cs = (CellSignal)cni.cellSignals.get(cni.gndNet);
			String groundName = getGroundName(cni.gndNet);
			if (groundName != null) cs.name = groundName;
			cs.ground = true;
		}

//		// find the widest export associated with each network
//		for(Iterator it = cni.netList.getNetworks(); it.hasNext(); )
//		{
//			Network net = (Network)it.next();
//			CellSignal cs = (CellSignal)cni.cellSignals.get(net);
//			if (cs.pp == null) continue;
//
//			// find the widest export that touches this network
//			int widestFound = cni.netList.getBusWidth(cs.pp);
//			Export widestPp = null;
//			for(Iterator eIt = cell.getPorts(); eIt.hasNext(); )
//			{
//				Export pp = (Export)eIt.next();
//				int portWidth = cni.netList.getBusWidth(pp);
//				boolean found = false;
//				for(int j=0; j<portWidth; j++)
//				{
//					Network subNet = cni.netList.getNetwork(pp, j);
//					if (subNet == net) found = true;
//				}
//				if (found)
//				{
//					if (portWidth > widestFound)
//					{
//						widestFound = portWidth;
//						widestPp = pp;
//					}
//				}
//			}
//			if (widestPp != null) cs.pp = widestPp;
//		}

		// make an array of the CellSignals
		cni.cellSignalsSorted = new ArrayList();
		for(Iterator it = cni.cellSignals.values().iterator(); it.hasNext(); )
		cni.cellSignalsSorted.add(it.next());

		// sort the networks by characteristic and name
		Collections.sort(cni.cellSignalsSorted, new SortNetsByName());

		// create aggregate signals if needed
		if (isAggregateNamesSupported())
		{
			// organize by name and index order
			cni.cellAggretateSignals = new ArrayList();
			int total = cni.cellSignalsSorted.size();
			int emptyName = 1;
			for(int i=0; i<total; i++)
			{
				CellSignal cs = (CellSignal)cni.cellSignalsSorted.get(i);

				CellAggregateSignal cas = new CellAggregateSignal();
				cas.name = unIndexedName(cs.name);
				cas.pp = cs.pp;
	            cas.ppIndex = cs.ppIndex;
				cas.supply = cs.power | cs.ground;
				cas.descending = cs.descending;
				cas.flags = 0;
				cs.aggregateSignal = cas;
				if (cs.name.equals(cas.name))
				{
					// single wire: set range to show that
					cas.low = 1;
					cas.high = 0;
					cas.signals = new CellSignal[1];
					cas.signals[0] = cs;
				} else
				{
					cas.high = cas.low = TextUtils.atoi(cs.name.substring(cas.name.length()+1));
					int start = i;
					for(int j=i+1; j<total; j++)
					{
						CellSignal csEnd = (CellSignal)cni.cellSignalsSorted.get(j);
						if (csEnd.descending != cs.descending) break;
						if (csEnd.pp != cs.pp) break;
						if (csEnd.globalSignal != cs.globalSignal) break;
						String endName = csEnd.name;
						String ept = unIndexedName(endName);
						if (ept.equals(endName)) break;
						int index = TextUtils.atoi(endName.substring(ept.length()+1));

						// make sure export indices go in order
						if (index != cas.high+1) break;
						if (index > cas.high) cas.high = index;
						i = j;
						csEnd.aggregateSignal = cas;
					}
					cas.signals = new CellSignal[i-start+1];
					for(int j=start; j<=i; j++)
					{
						CellSignal csEnd = (CellSignal)cni.cellSignalsSorted.get(j);
						cas.signals[j-start] = csEnd;
					}
				}
				cni.cellAggretateSignals.add(cas);
			}

			// give all signals a safe name
			for(Iterator it = cni.cellAggretateSignals.iterator(); it.hasNext(); )
			{
				CellAggregateSignal cas = (CellAggregateSignal)it.next();
				cas.name = getSafeNetName(cas.name);
//				if (cas.low == cas.high) cas.name = cas.name + "_" + cas.low + "_";
			}

			// make sure all names are unique
			int numNameList = cni.cellAggretateSignals.size();
			for(int i=1; i<numNameList; i++)
			{
				// single signal: give it that name
				CellAggregateSignal cas = (CellAggregateSignal)cni.cellAggretateSignals.get(i);

				// see if the name clashes
				for(int k=0; k<1000; k++)
				{
					String ninName = cas.name;
					if (k > 0) ninName = ninName + "_" + k;
					boolean found = false;
					for(int j=0; j<i; j++)
					{
						CellAggregateSignal oCas = (CellAggregateSignal)cni.cellAggretateSignals.get(j);
						if (ninName.equals(oCas.name)) { found = true;   break; }
					}
					if (!found)
					{
						if (k > 0) cas.name = ninName;
						break;
					}
				}
			}

			// put names back onto the signals
			for(Iterator it = cni.cellAggretateSignals.iterator(); it.hasNext(); )
			{
				CellAggregateSignal cas = (CellAggregateSignal)it.next();
				if (cas.low > cas.high)
				{
					CellSignal cs = cas.signals[0];
					cs.name = cas.name;
				} else
				{
					for(int k=cas.low; k<=cas.high; k++)
					{
						CellSignal cs = cas.signals[k-cas.low];
						cs.name = getSafeNetName(cas.name + "[" + k + "]");
					}
				}
			}
		} else {
            // just get safe net names for all cell signals
            for (Iterator it = cni.cellSignalsSorted.iterator(); it.hasNext(); ) {
                CellSignal cs = (CellSignal)it.next();
                cs.name = getSafeNetName(cs.name);
            }
        }

		return cni;
	}

	private void setBusDirectionality(Network [] nets, CellNetInfo cni)
	{
		boolean upDir = false, downDir = false, randomDir = false;
		int last = 0;
		int width = nets.length;
		for(int i=0; i<width; i++)
		{
			Network subNet = nets[i];
			if (subNet == null) continue;
			if (!subNet.hasNames()) break;
			String firstName = (String)subNet.getNames().next();
			int index = -1;
			int charPos = 0;
			for(;;)
			{
				charPos = firstName.indexOf('[', charPos);
				if (charPos < 0) break;
				charPos++;
				if (!TextUtils.isDigit(firstName.charAt(charPos))) continue;
				index = TextUtils.atoi(firstName.substring(charPos));
				break;
			}
			if (index < 0) break;
			if (i != 0)
			{
				if (index == last-1) downDir = true; else
					if (index == last+1) upDir = true; else
						randomDir = true;
			}
			last = index;
		}
		if (randomDir) return;
		if (upDir && downDir) return;
		if (!upDir && !downDir) return;

		// valid direction found: set it on all entries of the bus
		for(int i=0; i<width; i++)
		{
			Network subNet = nets[i];
			CellSignal cs = (CellSignal)cni.cellSignals.get(subNet);
			cs.descending = downDir;
		}
	}

	/**
	 * Method to return the character position in network name "name" that is the start of indexing.
	 * If there is no indexing ("clock"), this will point to the end of the string.
	 * If there is simple indexing ("dog[12]"), this will point to the "[".
	 * If the index is nonnumeric ("dog[cat]"), this will point to the end of the string.
	 * If there are multiple indices, ("dog[12][45]") this will point to the last "[" (unless it is nonnumeric).
	 */
	protected static String unIndexedName(String name)
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
			if (!TextUtils.isDigit(theChr)) break;
		}
		if (name.charAt(i) != '[') return name;
		return name.substring(0, i);
	}

	private static class SortNetsByName implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			CellSignal cs1 = (CellSignal)o1;
			CellSignal cs2 = (CellSignal)o2;
			if ((cs1.pp == null) != (cs2.pp == null))
			{
				// one is exported and the other isn't...sort accordingly
				return cs1.pp == null ? 1 : -1;
			}
			if (cs1.pp != null && cs2.pp != null)
			{
				// both are exported: sort by characteristics (if different)
				PortCharacteristic ch1 = cs1.pp.getCharacteristic();
				PortCharacteristic ch2 = cs2.pp.getCharacteristic();
				if (ch1 != ch2) return ch1.getOrder() - ch2.getOrder();
			}
			if (cs1.descending != cs2.descending)
			{
				// one is descending and the other isn't...sort accordingly
				return cs1.descending ? 1 : -1;
			}
			return TextUtils.nameSameNumeric(cs1.name, cs2.name);
		}
	}

	/**
	 * Method to create a parameterized name for node instance "ni".
	 * If the node is not parameterized, returns zero.
	 * If it returns a name, that name must be deallocated when done.
	 */
	protected String parameterizedName(Nodable no, VarContext context)
	{
		Cell cell = (Cell)no.getProto();
		String uniqueCellName = getUniqueCellName(cell);
		if (canParameterizeNames() &&
			no.getProto() instanceof Cell)
		{
			// if there are parameters, append them to this name
			HashMap paramValues = new HashMap();
			for(Iterator it = no.getVariables(); it.hasNext(); )
			{
				Variable var = (Variable)it.next();
				if (!var.getTextDescriptor().isParam()) continue;
				paramValues.put(var.getKey(), var);
			}
			for(Iterator it = paramValues.keySet().iterator(); it.hasNext(); )
			{
				Variable.Key key = (Variable.Key)it.next();
				Variable var = no.getVar(key.getName());
                String eval = var.describe(context, no.getNodeInst());
				//Object eval = context.evalVar(var, no);
				if (eval == null) continue;
                //uniqueCellName += "-" + var.getTrueName() + "-" + eval.toString();
                uniqueCellName += "-" + eval.toString();
			}
		}

		// if it is over the length limit, truncate it
		int limit = maxNameLength();
		if (limit > 0 && uniqueCellName.length() > limit)
		{
			int ckSum = 0;
			for(int i=0; i<uniqueCellName.length(); i++)
				ckSum += (int)uniqueCellName.charAt(i);
			ckSum = (ckSum % 9999);
			uniqueCellName = uniqueCellName.substring(0, limit-10) + "-TRUNC"+ckSum;
		}

		// make it safe
		return getSafeCellName(uniqueCellName);
	}

	/**
	 * Method to return the name of cell "c", given that it may be ambiguously used in multiple
	 * libraries.
	 */
	protected String getUniqueCellName(Cell cell)
	{
		String name = (String)cellNameMap.get(cell);
		return name;
	}

	/**
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
				if (isLibraryNameAlwaysAddedToCellName())
				{
					// always prepend library name: just do it
					cellNameMap.put(cell, lib.getName() + "__" + cell.getName());
					continue;
				}

				// only prepend library name when there is a conflict of cell names
				boolean duplicate = false;
				for(Iterator oLIt = Library.getLibraries(); oLIt.hasNext(); )
				{
					Library oLib = (Library)oLIt.next();
					if (oLib.isHidden()) continue;
					if (oLib == lib) continue;
					for(Iterator oCIt = oLib.getCells(); oCIt.hasNext(); )
					{
						Cell oCell = (Cell)oCIt.next();
						if (cell.getName().equalsIgnoreCase(oCell.getName()))
						{
							duplicate = true;
							break;
						}
					}
					if (duplicate) break;
				}

				if (duplicate) cellNameMap.put(cell, lib.getName() + "__" + cell.getName()); else
					cellNameMap.put(cell, cell.getName());
			}
		}
	}

}

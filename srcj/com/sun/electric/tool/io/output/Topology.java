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

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
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

	/** HashMap of all CellTopologies */				private HashMap<String,CellNetInfo> cellTopos;
	/** HashMap of all Cell names */					private HashMap<Cell,String> cellNameMap;
                                                        private HierarchyEnumerator.CellInfo lastInfo;

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
		cellTopos = new HashMap<String,CellNetInfo>();

		// make a map of cell names to use (unique across libraries)
        cellNameMap = makeCellNameMap(topCell);

		// write out cells
		start();
        boolean shortPolyResistors = isShortExplicitResistors();
        HierarchyEnumerator.enumerateCell(cell, context, visitor,
                isShortResistors(), shortPolyResistors, isShortResistors(), false);
		done();
		return false;
	}


	/** Abstract method called before hierarchy traversal */
	protected abstract void start();

	/** Abstract method called after traversal */
	protected abstract void done();

    /** Called at the end of the enter cell phase of hierarchy enumeration */
    protected void enterCell(HierarchyEnumerator.CellInfo info) { }

	/** Abstract method to write CellGeom to disk */
	protected abstract void writeCellTopology(Cell cell, CellNetInfo cni, VarContext context, Topology.MyCellInfo info);

	/** Abstract method to convert a network name to one that is safe for this format */
	protected abstract String getSafeNetName(String name, boolean bus);

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

    /** Method to decide whether to choose best export name among exports connected to signal. */
    protected boolean isChooseBestExportName() { return true; } 
    
    /** Abstract method to decide whether aggregate names (busses) are used. */
	protected abstract boolean isSeparateInputAndOutput();

    /** If the netlister has requirments not to netlist certain cells and their
     * subcells, override this method. */
    protected boolean skipCellAndSubcells(Cell cell) { return false; }

    /** If a cell is skipped, this method can perform any checking to
     * validate that no error occurs */
    protected void validateSkippedCell(HierarchyEnumerator.CellInfo info) { }

    /**
	 * Method to tell whether the topological analysis should mangle cell names that are parameterized.
	 */
	protected boolean canParameterizeNames() { return false; }

    /** Tell the Hierarchy enumerator whether or not to short parasitic resistors */
    protected boolean isShortResistors() { return false; }

    /** Tell the Hierarchy enumerator whether or not to short explicit (poly) resistors */
    protected boolean isShortExplicitResistors() { return false; }

	/**
	 * Method to tell set a limit on the number of characters in a name.
	 * @return the limit to name size (0 if no limit). 
	 */
	protected int maxNameLength() { return 0; }

	/** Abstract method to convert a cell name to one that is safe for this format */
	//protected abstract Netlist getNetlistForCell(Cell cell);

	protected CellNetInfo getCellNetInfo(String cellName)
	{
		CellNetInfo cni = cellTopos.get(cellName);
		return cni;
	}

    /** Used to switch from schematic enumeration to layout enumeration */
    protected boolean enumerateLayoutView(Cell cell) { return false; }

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
            if (skipCellAndSubcells(info.getCell()))
			{
				if (!info.isRootCell())
				{
	                // save subcell topology, even though the cell isn't being written
	                HierarchyEnumerator.CellInfo parentInfo = info.getParentInfo();
	                Nodable no = info.getParentInst();
	                String parameterizedName = parameterizedName(no, parentInfo.getContext());
					CellNetInfo cni = getNetworkInformation(info.getCell(), false, parameterizedName,
	                        isNetworksUseExportedNames(), info);
	                cellTopos.put(parameterizedName, cni);
				}
                validateSkippedCell(info);
                return false;
			}
            outGeom.enterCell(info);
			return true;
		}

		public void exitCell(HierarchyEnumerator.CellInfo info)
		{
			// write cell
			Cell cell = info.getCell();
			CellNetInfo cni = null;
			if (info.isRootCell())
			{
				cni = getNetworkInformation(cell, false, cell.getName(),
                        isNetworksUseExportedNames(), info);
                cellTopos.put(cell.getName(), cni);
			} else
			{
                // derived parameterized name of instance of this cell in parent
                HierarchyEnumerator.CellInfo parentInfo = info.getParentInfo();
                Nodable no = info.getParentInst();
                String parameterizedName = parameterizedName(no, parentInfo.getContext());
                cni = getNetworkInformation(info.getCell(), false, parameterizedName,
                        isNetworksUseExportedNames(), info);
                cellTopos.put(parameterizedName, cni);
/*
				MyCellInfo mci = (MyCellInfo)info;
				mci = (MyCellInfo)mci.getParentInfo();
				cni = getCellNetInfo(mci.currentInstanceParametizedName);
*/
			}
			outGeom.writeCellTopology(cell, cni, info.getContext(), (MyCellInfo)info);
            lastInfo = info;
		}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) 
		{
			if (!no.isCellInstance()) return false;

			VarContext context = info.getContext();
			String parameterizedName = parameterizedName(no, context);

			if (cellTopos.containsKey(parameterizedName)) return false;	// already processed this Cell

            Cell cell = (Cell)no.getProto();
            Cell schcell = cell.contentsView();
            if (schcell == null) schcell = cell;
            if (cell.getView() == View.SCHEMATIC && enumerateLayoutView(schcell)) {
                Cell layCell = null;
                for (Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); ) {
                    Cell c = it.next();
                    if (c.getView() == View.LAYOUT) {
                        layCell = c;
                        break;
                    }
                }
                if (layCell != null) {
                    HierarchyEnumerator.enumerateCell(layCell, context, this);
                    // save subcell topology, even though the cell isn't being written
                    CellNetInfo cni = getNetworkInformation(layCell, false, layCell.getName(),
                            isNetworksUseExportedNames(), lastInfo);
                    cellTopos.put(parameterizedName, cni);
                    return false;
                }
            }

/*
			Cell cell = (Cell)np;
			Netlist netList = getNetlistForCell(cell);
			CellNetInfo cni = getNetworkInformation(cell, false, mci.currentInstanceParametizedName, isNetworksUseExportedNames());
			cellTopos.put(mci.currentInstanceParametizedName, cni);
*/

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
		protected Global getGlobal() { return globalSignal; }
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
        private Cell cell;
		private String paramName;
		private HashMap<Network,CellSignal> cellSignals;
		private List<CellSignal> cellSignalsSorted;
		private List<CellAggregateSignal> cellAggretateSignals;
		private Network pwrNet;
		private Network gndNet;
		private Netlist netList;

		protected CellSignal getCellSignal(Network net) { return cellSignals.get(net); }
		protected Iterator<CellSignal> getCellSignals() { return cellSignalsSorted.iterator(); }
		protected Iterator<CellAggregateSignal> getCellAggregateSignals() { return cellAggretateSignals.iterator(); }
		protected String getParameterizedName() { return paramName; }
		protected Network getPowerNet() { return pwrNet; }
		protected Network getGroundNet() { return gndNet; }
		protected Netlist getNetList() { return netList; }
        protected Cell getCell() { return cell; }
	}

	private CellNetInfo getNetworkInformation(Cell cell, boolean quiet, String paramName, boolean useExportedName, 
                                              HierarchyEnumerator.CellInfo info)
	{
		CellNetInfo cni = doGetNetworks(cell, quiet, paramName, useExportedName, info);
//printWriter.print("********Decomposition of cell " + cell + "\n");
//printWriter.print("** Have " + cni.cellSignalsSorted.size() + " signals:\n");
//for(Iterator it = cni.getCellSignals(); it.hasNext(); )
//{
//	CellSignal cs = it.next();
//	printWriter.print("**   Name="+cs.name+" export="+cs.pp+" index="+cs.ppIndex+" descending="+cs.descending+" power="+cs.power+" ground="+cs.ground+" global="+cs.globalSignal+"\n");
//}
//if (isAggregateNamesSupported())
//{
//	printWriter.print("** Have " + cni.cellAggretateSignals.size() + " aggregate signals:\n");
//	for(CellAggregateSignal cas : cni.cellAggretateSignals)
//	{
//		printWriter.print("**   Name="+cas.name+", export="+cas.pp+" descending="+cas.descending+", low="+cas.low+", high="+cas.high+"\n");
//	}
//}
//printWriter.print("********DONE WITH CELL " + cell + "\n");
		return cni;
	}

	private CellNetInfo doGetNetworks(Cell cell, boolean quiet, String paramName, boolean useExportedName,
                                      HierarchyEnumerator.CellInfo info)
	{
		// create the object with cell net information
		CellNetInfo cni = new CellNetInfo();
        cni.cell = cell;
		cni.paramName = paramName;

		// get network information about this cell
		//cni.netList = getNetlistForCell(cell);
		cni.netList = info.getNetlist();
		Global.Set globals = cni.netList.getGlobals();
		int globalSize = globals.size();

		// create a map of all nets in the cell
		cni.cellSignals = new HashMap<Network,CellSignal>();
//		int nullNameCount = 1;
		for(Iterator<Network> it = cni.netList.getNetworks(); it.hasNext(); )
		{
			Network net = it.next();
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
                cs.name = net.getName();
//				if (net.hasNames())
//                {
//                    if (useExportedName && net.getExportedNames().hasNext())
//					{
//						cs.name = net.getExportedNames().next();
//                    } else
//                    {
//						cs.name = net.getNames().next();
//                    }
//                } else
//				{
//					cs.name = net.describe(false);
//					if (cs.name.equals(""))
//						cs.name = "UNCONNECTED" + (nullNameCount++);
//				}
			}

//            if (cs.getName().equals("gnd") && !cs.isGlobal()) {
//                System.out.println("cell "+cell.describe(false)+" has a global name "+cs.getName()+" but is not a global");
//                continue;
//            }
			// save it in the map of signals
 			cni.cellSignals.put(net, cs);
		}

		// mark exported networks
		for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();

			// mark every network on the bus (or just 1 network if not a bus)
			int portWidth = cni.netList.getBusWidth(pp);
			for(int i=0; i<portWidth; i++)
			{
				Network net = cni.netList.getNetwork(pp, i);
				CellSignal cs = cni.cellSignals.get(net);
				if (cs == null) continue;

				// if there is already an export on this signal, make sure that it is wider
				if (cs.pp != null)
                {
                    if (isChooseBestExportName())
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
					} else
                    {
                        if (TextUtils.STRING_NUMBER_ORDER.compare(cs.pp.getName(), pp.getName()) <= 0) continue;
                    }
				}

				// save this export's information
				cs.pp = pp;
				cs.ppIndex = i;
				if (useExportedName)
				{
					String rootName = pp.getName();
					if (portWidth == 1)
					{
						cs.name = rootName;
					}
					int openPos = rootName.indexOf('[');
					if (openPos > 0)
					{
						rootName = rootName.substring(0, openPos);
						for(Iterator<String> nIt = net.getExportedNames(); nIt.hasNext(); )
						{
							String exportNetName = nIt.next();
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
		for(Iterator<ArcInst> aIt = cell.getArcs(); aIt.hasNext(); )
		{
			ArcInst ai = aIt.next();
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
		for(Iterator<PortProto> eIt = cell.getPorts(); eIt.hasNext(); )
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
						System.out.println("Warning: multiple power networks in " + cell);
					multiPwr = true;
				}
				cni.pwrNet = subNet;
			}
			if (pp.isGround())
			{
				if (cni.gndNet != null && cni.gndNet != subNet && !multiGnd)
				{
					if (!quiet)
						System.out.println("Warning: multiple ground networks in " + cell);
					multiGnd = true;
				}
				cni.gndNet = subNet;
			}
		}
		for(Iterator<Network> it = cni.netList.getNetworks(); it.hasNext(); )
		{
			Network net = it.next();
			CellSignal cs = cni.cellSignals.get(net);
			if (cs.globalSignal != null)
			{
				if (cs.globalSignal == Global.power)
				{
					if (cni.pwrNet != null && cni.pwrNet != net && !multiPwr)
					{
						if (!quiet)
							System.out.println("Warning: multiple power networks in " + cell);
						multiPwr = true;
					}
					cni.pwrNet = net;
				}
				if (cs.globalSignal == Global.ground)
				{
					if (cni.gndNet != null && cni.gndNet != net && !multiGnd)
					{
						if (!quiet)
							System.out.println("Warning: multiple ground networks in " + cell);
						multiGnd = true;
					}
					cni.gndNet = net;
				}
			}
		}
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			PrimitiveNode.Function fun = ni.getFunction();
			if (fun == PrimitiveNode.Function.CONPOWER || fun == PrimitiveNode.Function.CONGROUND)
			{
				for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
				{
					Connection con = cIt.next();
					ArcInst ai = con.getArc();
					Network subNet = cni.netList.getNetwork(ai, 0);
					if (fun == PrimitiveNode.Function.CONPOWER)
					{
						if (cni.pwrNet != null && cni.pwrNet != subNet && !multiPwr)
						{
							if (!quiet)
								System.out.println("Warning: multiple power networks in " + cell);
							multiPwr = true;
						}
						cni.pwrNet = subNet;
					} else
					{
						if (cni.gndNet != null && cni.gndNet != subNet && !multiGnd)
						{
							if (!quiet)
								System.out.println("Warning: multiple ground networks in " + cell);
							multiGnd = true;
						}
						cni.gndNet = subNet;
					}
				}
			}
		}
		if (cni.pwrNet != null)
		{
			CellSignal cs = cni.cellSignals.get(cni.pwrNet);
			String powerName = getPowerName(cni.pwrNet);
			if (powerName != null) cs.name = powerName;
			cs.power = true;
		}
		if (cni.gndNet != null)
		{
			CellSignal cs = cni.cellSignals.get(cni.gndNet);
			String groundName = getGroundName(cni.gndNet);
			if (groundName != null) cs.name = groundName;
			cs.ground = true;
		}

//		// find the widest export associated with each network
//		for(Iterator it = cni.netList.getNetworks(); it.hasNext(); )
//		{
//			Network net = it.next();
//			CellSignal cs = cni.cellSignals.get(net);
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
		cni.cellSignalsSorted = new ArrayList<CellSignal>();
		for(CellSignal cs : cni.cellSignals.values())
			cni.cellSignalsSorted.add(cs);

		// sort the networks by characteristic and name
		Collections.sort(cni.cellSignalsSorted, new SortNetsByName(isSeparateInputAndOutput(), isAggregateNamesSupported()));

		// create aggregate signals if needed
		if (isAggregateNamesSupported())
		{
			// organize by name and index order
			cni.cellAggretateSignals = new ArrayList<CellAggregateSignal>();
			int total = cni.cellSignalsSorted.size();
//			int emptyName = 1;
			for(int i=0; i<total; i++)
			{
				CellSignal cs = cni.cellSignalsSorted.get(i);

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
						CellSignal csEnd = cni.cellSignalsSorted.get(j);
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
						CellSignal csEnd = cni.cellSignalsSorted.get(j);
						cas.signals[j-start] = csEnd;
					}
				}
				cni.cellAggretateSignals.add(cas);
			}

			// give all signals a safe name
			for(CellAggregateSignal cas : cni.cellAggretateSignals)
			{
				cas.name = getSafeNetName(cas.name, true);
//				if (cas.low == cas.high) cas.name = cas.name + "_" + cas.low + "_";
			}

			// make sure all names are unique
			int numNameList = cni.cellAggretateSignals.size();
			for(int i=1; i<numNameList; i++)
			{
				// single signal: give it that name
				CellAggregateSignal cas = cni.cellAggretateSignals.get(i);

				// see if the name clashes
				for(int k=0; k<1000; k++)
				{
					String ninName = cas.name;
					if (k > 0) ninName = ninName + "_" + k;
					boolean found = false;
					for(int j=0; j<i; j++)
					{
						CellAggregateSignal oCas = cni.cellAggretateSignals.get(j);
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
			for(CellAggregateSignal cas : cni.cellAggretateSignals)
			{
				if (cas.low > cas.high)
				{
					CellSignal cs = cas.signals[0];
					cs.name = cas.name;
				} else
				{
					for(int k=cas.low; k<=cas.high; k++)
					{
						CellSignal cs = cas.signals[k-cas.low];
						cs.name = getSafeNetName(cas.name + "[" + k + "]", false);
					}
				}
			}

            Collections.sort(cni.cellAggretateSignals, new SortAggregateNetsByName(isSeparateInputAndOutput()));

		} else {
            // just get safe net names for all cell signals
            for (CellSignal cs : cni.cellSignalsSorted)
			{
                cs.name = getSafeNetName(cs.name, false);
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
//			if (!subNet.hasNames()) break;
			String firstName = subNet.getNames().next();
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
			CellSignal cs = cni.cellSignals.get(subNet);
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

	private static class SortNetsByName implements Comparator<CellSignal>
	{
		private boolean separateInputAndOutput, aggregateNamesSupported;
		SortNetsByName(boolean separateInputAndOutput, boolean aggregateNamesSupported)
		{
			this.separateInputAndOutput = separateInputAndOutput;
			this.aggregateNamesSupported = aggregateNamesSupported;
		}

		public int compare(CellSignal cs1, CellSignal cs2)
		{
			if ((aggregateNamesSupported || separateInputAndOutput) && (cs1.pp == null) != (cs2.pp == null))
			{
				// one is exported and the other isn't...sort accordingly
				return cs1.pp == null ? 1 : -1;
			}
			if (cs1.pp != null && cs2.pp != null)
			{
				if (separateInputAndOutput)
				{
					// both are exported: sort by characteristics (if different)
					PortCharacteristic ch1 = cs1.pp.getCharacteristic();
					PortCharacteristic ch2 = cs2.pp.getCharacteristic();
					if (ch1 != ch2) return ch1.getOrder() - ch2.getOrder();
				}
			}
			if (aggregateNamesSupported && cs1.descending != cs2.descending)
			{
				// one is descending and the other isn't...sort accordingly
				return cs1.descending ? 1 : -1;
			}
            if (aggregateNamesSupported)
			    return TextUtils.STRING_NUMBER_ORDER.compare(cs1.name, cs2.name);
            else {
                // Sort by simple string comparison, otherwise it is impossible
                // to stitch this together with netlists from other tools
                return cs1.name.compareTo(cs2.name);
            }
		}
	}

    private static class SortAggregateNetsByName implements Comparator<CellAggregateSignal>
    {
        private boolean separateInputAndOutput;
        SortAggregateNetsByName(boolean separateInputAndOutput)
        {
            this.separateInputAndOutput = separateInputAndOutput;
        }

        public int compare(CellAggregateSignal cs1, CellAggregateSignal cs2)
        {
            if ((separateInputAndOutput) && (cs1.pp == null) != (cs2.pp == null))
            {
                // one is exported and the other isn't...sort accordingly
                return cs1.pp == null ? 1 : -1;
            }
            if (cs1.pp != null && cs2.pp != null)
            {
                if (separateInputAndOutput)
                {
                    // both are exported: sort by characteristics (if different)
                    PortCharacteristic ch1 = cs1.pp.getCharacteristic();
                    PortCharacteristic ch2 = cs2.pp.getCharacteristic();
                    if (ch1 != ch2) return ch1.getOrder() - ch2.getOrder();
                }
            }
            // Sort by simple string comparison, otherwise it is impossible
            // to stitch this together with netlists from other tools
            return cs1.name.compareTo(cs2.name);
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
		if (canParameterizeNames() && no.isCellInstance())
		{
			// if there are parameters, append them to this name
			HashMap<Variable.Key,Variable> paramValues = new HashMap<Variable.Key,Variable>();
			for(Iterator<Variable> it = no.getVariables(); it.hasNext(); )
			{
				Variable var = it.next();
				if (!no.getNodeInst().isParam(var.getKey())) continue;
//				if (!var.isParam()) continue;
				paramValues.put(var.getKey(), var);
			}
			for(Variable.Key key : paramValues.keySet())
			{
				Variable var = no.getVar(key);
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
        Cell contents = cell.contentsView();
        if (contents != null) cell = contents;
		String name = cellNameMap.get(cell);
		return name;
	}

    private static class NameMapGenerator extends HierarchyEnumerator.Visitor {

        private HashMap<Cell,String> cellNameMap;
        private HashMap<String,List<Cell>> cellNameMapReverse;
        private boolean alwaysUseLibName;
        private Topology topology;
        private Cell topCell;

        public NameMapGenerator(boolean alwaysUseLibName, Topology topology) {
            cellNameMap = new HashMap<Cell,String>();
            cellNameMapReverse = new HashMap<String,List<Cell>>();
            this.alwaysUseLibName = alwaysUseLibName;
            this.topology = topology;
            topCell = null;
        }

        public boolean enterCell(HierarchyEnumerator.CellInfo info) {
            Cell cell = info.getCell();
            if (topCell == null) topCell = cell;

            if (cellNameMap.containsKey(cell)) return false;
            // add name for this cell
            String name = getDefaultName(cell);
            cellNameMap.put(cell, name);
            getConflictList(name).add(cell);
            //System.out.println("Mapped "+cell.describe(false) + " --> " + name);
            return true;
        }

        private List<Cell> getConflictList(String cellname) {
            List<Cell> conflictList = cellNameMapReverse.get(cellname.toLowerCase());
            if (conflictList == null) {
                conflictList = new ArrayList<Cell>();
                cellNameMapReverse.put(cellname.toLowerCase(), conflictList);
            }
            return conflictList;
        }

        public void exitCell(HierarchyEnumerator.CellInfo info) {
            Cell cell = info.getCell();
            if (cell == topCell) {
                // resolve conflicts
                if (!alwaysUseLibName)
                    resolveConflicts(1);
                resolveConflicts(2);
                resolveConflicts(3);
            }
        }

        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) {
            if (!no.isCellInstance()) return false;

            VarContext context = info.getContext();

            Cell cell = (Cell)no.getProto();
            Cell schcell = cell.contentsView();
            if (schcell == null) schcell = cell;
            if (cell.getView() == View.SCHEMATIC && topology.enumerateLayoutView(schcell)) {
                Cell layCell = null;
                for (Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); ) {
                    Cell c = it.next();
                    if (c.getView() == View.LAYOUT) {
                        layCell = c;
                        break;
                    }
                }
                if (layCell != null) {
                    String name = getDefaultName(schcell);
                    cellNameMap.put(schcell, name);
                    getConflictList(name).add(schcell);
                    //System.out.println("Mapped "+schcell.describe(false) + " --> " + name);
                    HierarchyEnumerator.enumerateCell(layCell, context, this);
                    return false;
                }
            } else {
                // special case for cells with only icons
                schcell = cell.contentsView();
                if (cell.getView() == View.ICON && schcell == null && !cellNameMap.containsKey(cell)) {
                    String name = getDefaultName(cell);
                    cellNameMap.put(cell, name);
                    getConflictList(name).add(cell);
                    return false;
                }
            }


            return true;
        }

        private String getDefaultName(Cell cell) {
            if (alwaysUseLibName) {
                return cell.getLibrary().getName() + "__" + cell.getName();
            } else
                return cell.getName();
        }

        private void resolveConflicts(int whichPass) {
            List<List<Cell>> conflictLists = new ArrayList<List<Cell>>();
            for (List<Cell> conflictList : cellNameMapReverse.values()) {
                if (conflictList.size() <= 1) continue; // no conflict
                conflictLists.add(conflictList);
            }
            for (List<Cell> conflictList : conflictLists) {
                // on pass 1, only cell names. If any conflicts, prepend libname
                if (whichPass == 1) {
                    // replace name with lib + name
                    for (Cell cell : conflictList) {
                        String name = cell.getLibrary().getName() + "__" + cell.getName();
                        cellNameMap.put(cell, name);
                        getConflictList(name).add(cell);
                    }
                    conflictList.clear();
                }
                if (whichPass == 2) {
                    // lib + name conflicts, append view
                    for (Cell cell : conflictList) {
                        String name = cell.getLibrary().getName() + "__" + cell.getName() + "__" + cell.getView().getAbbreviation();
                        cellNameMap.put(cell, name);
                        getConflictList(name).add(cell);
                    }
                    conflictList.clear();
                }
                if (whichPass == 3) {
                    // must be an error
                    Cell cell = conflictList.get(0);
                    System.out.print("Error: Unable to make unique cell name for "+cell.describe(false)+
                            ", it conflicts with: ");
                    for (int i=1; i<conflictList.size(); i++) {
                        System.out.print(conflictList.get(i).describe(false)+" ");
                    }
                    System.out.println();
                }
            }
        }
    }

	/**
	 * determine whether any cells have name clashes in other libraries
	 */
	private HashMap<Cell,String> makeCellNameMap(Cell cell)
	{
        NameMapGenerator gen = new NameMapGenerator(isLibraryNameAlwaysAddedToCellName(), this);
        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, gen);
        return gen.cellNameMap;
	}

}

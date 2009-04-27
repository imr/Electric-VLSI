/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SpiceSegmentedNets.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.io.output.Topology.CellNetInfo;
import com.sun.electric.tool.io.output.Topology.CellSignal;
import com.sun.electric.tool.simulation.Simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Class to take care of added networks in cell due to
 * extracting resistance of arcs.  Takes care of naming,
 * addings caps at portinst locations (due to PI model end caps),
 * and storing resistance of arcs.
 * <P>
 * A network is broken into segments at all PortInsts along the network.
 * Each PortInst is given a new net segment name.  These names are used
 * to write out caps and resistors.  Each Arc writes out a PI model
 * (a cap on each end and a resistor in the middle).  Sometimes, an arc
 * has very little or zero resistance, or we want to ignore it.  Then
 * we must short two portinsts together into the same net segment.
 * However, we do not discard the capacitance, but continue to add it up.
 */
class SpiceSegmentedNets
{
    private final Spice.SpicePreferences sp;
	private Map<PortInst,NetInfo> segmentedNets;          // key: portinst, obj: PortInstInfo
	private Map<ArcInst,Double> arcRes;                 // key: arcinst, obj: Double (arc resistance)
	private boolean verboseNames = false;           // true to give renamed nets verbose names
	private CellNetInfo cni;                // the Cell's net info
	private Map<Network,Integer> netCounters;            // key: net, obj: Integer - for naming segments
	private Cell cell;
	private List<List<String>> shortedExports; // list of lists of export names shorted together
	private Map<ArcInst,Double> longArcCaps;            // for arcs to be broken up into multiple PI models, need to record cap
	private Map<Network,Network> extractedNets;         // keep track of extracted nets

	public SpiceSegmentedNets(Cell cell, boolean verboseNames, CellNetInfo cni, Spice.SpicePreferences sp)
	{
		segmentedNets = new HashMap<PortInst,NetInfo>();
		arcRes = new HashMap<ArcInst,Double>();
		this.verboseNames = verboseNames;
		this.cni = cni;
        this.sp = sp;
		netCounters = new HashMap<Network,Integer>();
		this.cell = cell;
		shortedExports = new ArrayList<List<String>>();
		longArcCaps = new HashMap<ArcInst,Double>();
		extractedNets = new HashMap<Network,Network>();
	}

	// don't call this method outside of SpiceSegmentedNets
	// Add a new PortInst net segment
	public NetInfo putSegment(PortInst pi, double cap)
	{
		// create new info for PortInst
		NetInfo info = segmentedNets.get(pi);
		if (info == null)
		{
			info = new NetInfo();
			info.netName = getNewName(pi, info);
			info.cap += cap;
			if (isPowerGround(pi)) info.cap = 0;        // note if you remove this line,
                                                        // you have to explicity short all
                                                        // power portinsts together, or you can get duplicate caps
			info.joinedPorts.add(pi);
			segmentedNets.put(pi, info);
		} else
		{
			info.cap += cap;
			//assert(info.joinedPorts.contains(pi));  // should already contain pi if info already exists
		}
		return info;
	}

	// don't call this method outside of SpiceSegmentedNets
	// Get a new name for the net segment associated with the portinst
	private String getNewName(PortInst pi, NetInfo info)
	{
		Network net = cni.getNetList().getNetwork(pi);
		CellSignal cs = cni.getCellSignal(net);
		if (sp.parasiticsLevel == Simulation.SpiceParasitics.SIMPLE || (!sp.parasiticsExtractPowerGround &&
			isPowerGround(pi))) return cs.getName();

		Integer i = netCounters.get(net);
		if (i == null)
		{
			i = new Integer(0);
			netCounters.put(net, i);
		}

		// get new name
		String name = info.netName;
		Export ex = pi.getExports().hasNext() ? pi.getExports().next() : null;
		//if (ex != null && ex.getName().equals(cs.getName())) {
		if (ex != null)
		{
			name = ex.getName();
		} else
		{
			if (i.intValue() == 0 && !cs.isExported())	  // get rid of #0 if net not exported
				name = cs.getName();
			else
			{
				if (verboseNames)
					name = cs.getName() + "#" + i.intValue() + pi.getNodeInst().getName() + "_" + pi.getPortProto().getName();
				else
					name = cs.getName() + "#" + i.intValue();
			}
			i = new Integer(i.intValue() + 1);
			netCounters.put(net, i);
		}
		//System.out.println("Created new segmented net name "+name+" for port "+pi.getPortProto().getName()+
		//        " on node "+pi.getNodeInst().getName()+" on net "+net.getName());
		return name;
	}

	// short two net segments together by their portinsts
	public void shortSegments(PortInst p1, PortInst p2)
	{
		if (!segmentedNets.containsKey(p1))
			putSegment(p1, 0);
		if (!segmentedNets.containsKey(p2));
			putSegment(p2, 0);
		NetInfo info1 = segmentedNets.get(p1);
		NetInfo info2 = segmentedNets.get(p2);
		if (info1 == info2) return;                     // already joined
		// short
		//System.out.println("Shorted together "+info1.netName+ " and "+info2.netName);
		info1.joinedPorts.addAll(info2.joinedPorts);
		info1.cap += info2.cap;
		if (TextUtils.STRING_NUMBER_ORDER.compare(info2.netName, info1.netName) < 0)
		{
//			if (info2.netName.compareTo(info1.netName) < 0) {
			info1.netName = info2.netName;
		}
		//info1.netName += info2.netName;
		// replace info2 with info1, info2 is no longer used
		// need to do for every portinst in merged segment
		for (PortInst pi : info1.joinedPorts)
			segmentedNets.put(pi, info1);
	}

	// get the segment name for the portinst.
	// if no parasitics, this is just the CellSignal name.
	public String getNetName(PortInst pi)
	{
		if (sp.parasiticsLevel == Simulation.SpiceParasitics.SIMPLE || (isPowerGround(pi) &&
			!sp.parasiticsExtractPowerGround))
		{
			CellSignal cs = cni.getCellSignal(cni.getNetList().getNetwork(pi));
			if (cs == null) return null;
			//System.out.println("CellSignal name for "+pi.getNodeInst().getName()+"."+pi.getPortProto().getName()+" is "+cs.getName());
//System.out.println("NETWORK NAMED "+cs.getName());
			return cs.getName();
		}
		NetInfo info = segmentedNets.get(pi);
		if (info == null)
		{
			CellSignal cs = cni.getCellSignal(cni.getNetList().getNetwork(pi));
			if (cs == null) return null;
			return cs.getName();
			//info = putSegment(pi, 0);
		}
//System.out.println("NETWORK INAMED "+info.netName);
		return info.netName;
	}

	public void addArcRes(ArcInst ai, double res)
	{
		// short out if both conns are power/ground
		if (isPowerGround(ai.getHeadPortInst()) && isPowerGround(ai.getTailPortInst()) &&
			!Simulation.isParasiticsExtractPowerGround())
		{
			shortSegments(ai.getHeadPortInst(), ai.getTailPortInst());
			return;
		}
		arcRes.put(ai, new Double(res));
	}

	public Double getRes(ArcInst ai) { return arcRes.get(ai); }

	public boolean isPowerGround(PortInst pi)
	{
		Network net = cni.getNetList().getNetwork(pi);
		CellSignal cs = cni.getCellSignal(net);
		if (cs == null) return false;
		if (cs.isPower() || cs.isGround()) return true;
		if (cs.getName().startsWith("vdd")) return true;
		if (cs.getName().startsWith("gnd")) return true;
		return false;
	}

	/**
	 * Return list of NetInfos for unique segments
	 * @return a list of al NetInfos
	 */
	public TreeSet<NetInfo> getUniqueSegments()
	{
		return new TreeSet<NetInfo>(segmentedNets.values());
	}

	public Simulation.SpiceParasitics getParasiticsLevel()
	{
		return sp.parasiticsLevel;
	}

	// list of export names (Strings)
	public void addShortedExports(List<String> exports)
	{
		shortedExports.add(exports);
	}

	// list of lists of export names (Strings)
	public Iterator<List<String>> getShortedExports() { return shortedExports.iterator(); }

	public static int getNumPISegments(double res, double maxSeriesResistance)
	{
		if (res <= 0) return 1;
		int arcPImodels = 1;
		arcPImodels = (int)(res/maxSeriesResistance);            // need preference here
		if ((res % maxSeriesResistance) != 0) arcPImodels++;
		return arcPImodels;
	}

	// for arcs of larger than max series resistance, we need to break it up into
	// multiple PI models.  So, we need to store the cap associated with the arc
	public void addArcCap(ArcInst ai, double cap)
	{
		longArcCaps.put(ai, new Double(cap));
	}

	public double getArcCap(ArcInst ai)
	{
		Double d = longArcCaps.get(ai);
		return d.doubleValue();
	}

	public void addExtractedNet(Network net)
	{
		extractedNets.put(net, net);
	}

	public boolean isExtractedNet(Network net)
	{
		return extractedNets.containsKey(net);
	}

	public Cell getCell() { return cell; }

	private static Comparator<PortInst> PORT_INST_COMPARATOR = new Comparator<PortInst>()
	{
		public int compare(PortInst p1, PortInst p2)
		{
			if (p1 == p2) return 0;
			int cmp = p1.getNodeInst().compareTo(p2.getNodeInst());
			if (cmp != 0) return cmp;
			if (p1.getPortIndex() < p2.getPortIndex()) return -1;
			return 1;
		}
	};

	public static class NetInfo implements Comparable
	{
		private String netName = "unassigned";
		private double cap = 0;
		private TreeSet<PortInst> joinedPorts = new TreeSet<PortInst>(PORT_INST_COMPARATOR);     // list of portInsts on this new net

		/**
		 * Compares NetInfos by thier first PortInst.
		 * @param obj the other NetInfo.
		 * @return a comparison between the NetInfos.
		 */
		public int compareTo(Object obj)
		{
			NetInfo that = (NetInfo)obj;
			if (this.joinedPorts.isEmpty()) return that.joinedPorts.isEmpty() ? 0 : -1;
			if (that.joinedPorts.isEmpty()) return 1;
			return PORT_INST_COMPARATOR.compare(this.joinedPorts.first(), that.joinedPorts.first());
		}

		public Iterator<PortInst> getPortIterator() { return joinedPorts.iterator(); }

		public double getCap() { return cap; }

		public String getName() { return netName; }
	}
}

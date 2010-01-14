/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SpiceRCSimple.java
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.output.Topology.CellNetInfo;
import com.sun.electric.tool.io.output.Topology.CellSignal;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the simple-RC parasitics extractor for the Spice netlist writer.
 */
public class SpiceParasitic extends SpiceParasiticsGeneral
{
	/** List of networks analyzed. */  								private List<Network> networkList;
	/** List of arcs analyzed. */        							private List<ArcInst> arcList;
	/** Parasitic component count. */    							int tLineCount = 0;
	/** The head port of the parasitic component */     			String n0= "";
	/** The tail port of the parasitic component */    				String n1= "";
	/** The layer of the previous parasitic component */ 		    String preLayer = "";
	/** The previous arc analyzed */ 								ArcInst preAi = null;
	/** The current arc being analyzed */							ArcInst currAi = null;
	/** Whether or not the subckt spice code is already printed  */ boolean alreadyPrinted = false;
//	private List<Connection> conList;

	SpiceParasitic(Spice.SpicePreferences localPrefs)
	{
        super(localPrefs);
		segmentedParasiticInfo = new ArrayList<SpiceSegmentedNets>();
	}

	/**
	 * Method to initialize cell being analyzed for RC parasitics.
	 * @param cell the Cell being analyzed.
	 * @param cni hierarchical traversal information for the Cell, including netlists and other connectivity data.
	 * @param layoutTechnology the Technology to use for the Cell (may be different
	 * from the Cell's actual Technology if the Cell is a schematic...this is the
	 * layout technology to use instead).
	 * @param exemptedNets as set of networks that should be exempted from the analysis
	 * @param info data from the hierarchy traverser that gives global network information.
	 * @return a SpiceSegmentedNets object for the Cell.
	 */
	public SpiceSegmentedNets initializeSegments(Cell cell, CellNetInfo cni, Technology layoutTechnology,
		SpiceExemptedNets exemptedNets, Topology.MyCellInfo info)
	{
		// first create a set of segmentedNets for the Cell
        boolean verboseSegmentNames = localPrefs.parasiticsUseVerboseNaming;
        Simulation.SpiceParasitics spLevel = localPrefs.parasiticsLevel;
        SpiceSegmentedNets segmentedNets = new SpiceSegmentedNets(cell, verboseSegmentNames, cni, localPrefs);
        segmentedParasiticInfo.add(segmentedNets);
        curSegmentedNets = segmentedNets;

        // look at every arc in the Cell
        Netlist netList = cni.getNetList();
		double scale = layoutTechnology.getScale(); // scale to convert units to nanometers
		Map<Network,Network> exemptedNetsFound = new HashMap<Network,Network>();
		for (Iterator<ArcInst> ait = cell.getArcs(); ait.hasNext(); )
		{
			ArcInst ai = ait.next();

			// see if the network is being extracted
			boolean extractNet = true;
			if (segmentedNets.isPowerGround(ai.getHeadPortInst()))
				extractNet = false;
			if (ai.getProto().getFunction() == ArcProto.Function.NONELEC)
				extractNet = false;

			Network net = netList.getNetwork(ai, 0);
			double cap = 0;
			double res = 0;
			if (extractNet && localPrefs.parasiticsUseExemptedNetsFile)
			{
				// ignore nets in exempted nets file
				if (localPrefs.parasiticsIgnoreExemptedNets)
				{
					// check if this net is exempted
					if (exemptedNets.isExempted(info.getNetID(net)))
					{
						extractNet = false;
						cap = 0;
						if (!exemptedNetsFound.containsKey(net))
						{
							System.out.println("Not extracting net "+cell.describe(false)+" "+net.getName());
							exemptedNetsFound.put(net, net);
							cap = exemptedNets.getReplacementCap(cell, net);
						}
					}
					// extract only nets in exempted nets file
				} else
				{
					if (exemptedNets.isExempted(info.getNetID(net)))
					{
						if (!exemptedNetsFound.containsKey(net))
						{
							System.out.println("Extracting net "+cell.describe(false)+" "+net.getName());
							exemptedNetsFound.put(net, net);
							extractNet = true;
						}
					} else
					{
						extractNet = false;
					}
				}
			}
			if (!extractNet)
			{
				// don't need to short arcs on networks that aren't extracted, since it is
				// guaranteed that both ends of the arc are named the same.
				//segmentedNets.shortSegments(ai.getHeadPortInst(), ai.getTailPortInst());
				continue;
			}

			// figure out res and cap, see if we should ignore it
			double length = ai.getLambdaLength() * scale / 1000;      // length in microns
			double width = ai.getLambdaBaseWidth() * scale / 1000;        // width in microns
			double area = length * width;
			double fringe = length*2;

			Technology tech = ai.getProto().getTechnology();
			Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
			int tot = arcInstPolyList.length;
			for(int j=0; j<tot; j++)
			{
				Poly poly = arcInstPolyList[j];
				if (poly.getStyle().isText()) continue;
				if (poly.isPseudoLayer()) continue;
				Layer layer = poly.getLayer();
				if (layer.getTechnology() != layoutTechnology) continue;

				if (!layer.isDiffusionLayer())
				{
					if (localPrefs.parasiticsExtractsC)
					{
						double areacap = area * layer.getCapacitance();
						double fringecap = fringe * layer.getEdgeCapacitance();
						cap = areacap + fringecap;
					}
					if (localPrefs.parasiticsExtractsR)
					{
						res = length/width * layer.getResistance();
					}
				}
			}

			int arcPImodels = SpiceSegmentedNets.getNumPISegments(res, layoutTechnology.getMaxSeriesResistance());

			// add caps
			segmentedNets.putSegment(ai.getHeadPortInst(), cap/(arcPImodels+1));
			segmentedNets.putSegment(ai.getTailPortInst(), cap/(arcPImodels+1));

			//system.out.println("Using resistance of "+res+" for arc "+ai.getName());
			segmentedNets.addArcRes(ai, res);

			segmentedNets.addArcCap(ai, cap);       // need to store cap later to break it up

			segmentedNets.addExtractedNet(net);
		}

		// Don't take into account gate resistance: so we need to short two PortInsts
		// of gate together if this is layout
		for(Iterator<NodeInst> aIt = cell.getNodes(); aIt.hasNext(); )
		{
			NodeInst ni = aIt.next();
			if (!ni.isCellInstance())
			{
				if (((PrimitiveNode)ni.getProto()).getGroupFunction() == PrimitiveNode.Function.TRANS)
				{
					// System.out.println("--Processing gate "+ni.getName());
					PortInst gate0 = ni.getTransistorGatePort();
					PortInst gate1 = ni.getTransistorAltGatePort();
					Network gateNet0 = netList.getNetwork(gate0);
					if ((gate0 != gate1) && segmentedNets.isExtractedNet(gateNet0))
					{
						//System.out.println("Shorting gate "+ni.getName()+" ports "+gate0.getPortProto().getName()+" and "+gate1.getPortProto().getName());
						segmentedNets.shortSegments(gate0, gate1);
					}
				}
				// merge wells
			} else
			{
				// System.out.println("--Processing subcell "+ni.getName());
				// short together pins if shorted by subcell
				Cell subCell = (Cell)ni.getProto();
				SpiceSegmentedNets subNets = getSegmentedNets(subCell);

				// list of lists of shorted exports
				if (subNets != null)
				{
					// subnets may be null if mixing schematics with layout technologies
					for (Iterator<List<String>> it = subNets.getShortedExports(); it.hasNext(); )
					{
						List<String> exports = it.next();
						PortInst pi1 = null;

						// list of exports shorted together
						for (String exportName : exports)
						{
							// get portinst on node
							PortInst pi = ni.findPortInst(exportName);
							if (pi1 == null)
							{
								pi1 = pi;
								continue;
							}
							Network net = netList.getNetwork(pi);
							if (segmentedNets.isExtractedNet(net))
								segmentedNets.shortSegments(pi1, pi);
						}
					}
				}
			}
		}
		return segmentedNets;
	}

	/**
	 * Method to emit the proper subcircuit header for a signal.
	 * @param cs the signal to emit
	 * @param infstr the string buffer to fill with the emitted signal information.
	 */
	public void writeSubcircuitHeader(CellSignal cs, StringBuffer infstr)
	{
		Network net = cs.getNetwork();
		Map<String,List<String>> shortedExportsMap = new HashMap<String,List<String>>();

		// For a single logical network, we need to:
		// 1) treat certain exports as separate so as not to short resistors on arcs between the exports
		// 2) join certain exports that do not have resistors on arcs between them, and record this information
		//   so that the next level up knows to short networks connection to those exports.
		for (Iterator<Export> it = net.getExports(); it.hasNext(); )
		{
			Export e = it.next();
			PortInst pi = e.getOriginalPort();
			String name = curSegmentedNets.getNetName(pi);

			// exports are shorted if their segmented net names are the same (case (2))
			List<String> shortedExports = shortedExportsMap.get(name);
			if (shortedExports == null)
			{
				shortedExports = new ArrayList<String>();
				shortedExportsMap.put(name, shortedExports);

				// this is the first occurance of this segmented network, use the name as the export (1)
				infstr.append(" " + name);
			}
			shortedExports.add(e.getName());
		}

		// record shorted exports
		for (List<String> shortedExports : shortedExportsMap.values())
		{
			if (shortedExports.size() > 1)
				curSegmentedNets.addShortedExports(shortedExports);
		}
	}

	/**
	 * Method to emit the name of a signal on an instance call (the "X" statement).
	 * @param no the Nodable for the cell instance being examined.
	 * @param subNet the Network in the cell attached to that Nodable.
	 * @param subSegmentedNets the SpiceSegmentedNets object for the Nodable's Cell.
	 * @param infstr the string buffer in which to emit the name(s).
	 */
	public void getParasiticName(Nodable no, Network subNet, SpiceSegmentedNets subSegmentedNets, StringBuffer infstr)
	{
		// connect to all exports (except power and ground of subcell net)
		List<String> exportNames = new ArrayList<String>();
		for (Iterator<Export> it = subNet.getExports(); it.hasNext(); )
		{
			// get subcell export, unless collapsed due to less than min R
			Export e = it.next();
			PortInst pi = e.getOriginalPort();
			String name = subSegmentedNets.getNetName(pi);
			if (exportNames.contains(name)) continue;
			exportNames.add(name);

			// ok, there is a port on the subckt on this subcell for this export,
			// now get the appropriate network in this cell
			pi = no.getNodeInst().findPortInstFromProto(no.getProto().findPortProto(e.getNameKey()));
			name = curSegmentedNets.getNetName(pi);
			infstr.append(" " + name);
		}
	}

	/**
	 * Method to find the SpiceSegmentedNets object that corresponds to a given Cell.
	 * @param cell the Cell to find.
	 * @return the SpiceSegmentedNets object associated with that cell (null if none found).
	 */
	public SpiceSegmentedNets getSegmentedNets(Cell cell)
	{
		for (SpiceSegmentedNets seg : segmentedParasiticInfo)
			if (seg.getCell() == cell) return seg;
		return null;
	}

	/**
	 * Method called at the end of netlist writing to deal with back-annotation.
	 */
	public void backAnnotate()
	{
    	Set<Cell>      cellsToClear = new HashSet<Cell>();
    	List<PortInst> capsOnPorts  = new ArrayList<PortInst>();
    	List<String>   valsOnPorts  = new ArrayList<String>();
    	List<ArcInst>  resOnArcs    = new ArrayList<ArcInst>();
    	List<Double>   valsOnArcs   = new ArrayList<Double>();
        for (SpiceSegmentedNets segmentedNets : segmentedParasiticInfo)
        {
            Cell cell = segmentedNets.getCell();
            if (cell.getView() != View.LAYOUT) continue;

            // gather cells to clear capacitor values
            cellsToClear.add(cell);

            // gather capacitor updates
            for (SpiceSegmentedNets.NetInfo info : segmentedNets.getUniqueSegments())
            {
                PortInst pi = info.getPortIterator().next();
                if (info.getCap() > cell.getTechnology().getMinCapacitance())
                {
                	capsOnPorts.add(pi);
                	valsOnPorts.add(TextUtils.formatDouble(info.getCap(), 2) + "fF");
                }
            }

            // gather resistor updates
            for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
            {
                ArcInst ai = it.next();
                Double res = segmentedNets.getRes(ai);

                resOnArcs.add(ai);
                valsOnArcs.add(res);
            }
        }
        new BackAnnotateJob(cellsToClear, capsOnPorts, valsOnPorts, resOnArcs, valsOnArcs);
	}

	/**
	 * Class to run back-annotation in a Job.
	 */
	private static class BackAnnotateJob extends Job
    {
    	private Set<Cell> cellsToClear;
    	private List<PortInst> capsOnPorts;
    	private List<String> valsOnPorts;
    	private List<ArcInst> resOnArcs;
    	private List<Double> valsOnArcs;

        private BackAnnotateJob(Set<Cell> cellsToClear, List<PortInst> capsOnPorts, List<String> valsOnPorts,
        	List<ArcInst> resOnArcs, List<Double> valsOnArcs)
    	{
            super("Spice Layout Back Annotate", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.capsOnPorts = capsOnPorts;
            this.valsOnPorts = valsOnPorts;
            this.resOnArcs = resOnArcs;
            this.valsOnArcs = valsOnArcs;
            this.cellsToClear = cellsToClear;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            TextDescriptor ctd = TextDescriptor.getPortInstTextDescriptor().withDispPart(TextDescriptor.DispPos.NAMEVALUE);
            TextDescriptor rtd = TextDescriptor.getArcTextDescriptor().withDispPart(TextDescriptor.DispPos.NAMEVALUE);
            int capCount = 0;
            int resCount = 0;

            // clear caps on layout
            for(Cell cell : cellsToClear)
            {
                // delete all C's already on layout
                for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
                {
                    NodeInst ni = it.next();
                    for (Iterator<PortInst> pit = ni.getPortInsts(); pit.hasNext(); )
                    {
                        PortInst pi = pit.next();
                        Variable var = pi.getVar(ATTR_C);
                        if (var != null) pi.delVar(var.getKey());
                    }
                }
            }

            // add new C's
            for(int i=0; i<capsOnPorts.size(); i++)
            {
            	PortInst pi = capsOnPorts.get(i);
            	String str = valsOnPorts.get(i);
                pi.newVar(ATTR_C, str, ctd);
                resCount++;
            }

            // add new R's
            for(int i=0; i<resOnArcs.size(); i++)
            {
            	ArcInst ai = resOnArcs.get(i);
            	Double res = valsOnArcs.get(i);

                // delete R if no new one
                Variable var = ai.getVar(ATTR_R);
                if (res == null && var != null)
                    ai.delVar(ATTR_R);

                // change R if new one
                if (res != null)
                {
                    ai.newVar(ATTR_R, res, rtd);
                    resCount++;
                }
            }
            System.out.println("Back-annotated "+resCount+" Resistors and "+capCount+" Capacitors");
            return true;
        }
    }

	/**
	 * Method to print the netlist considering the metal lines as distribute RC(transmission lines)
	 */
	public void writeNewSpiceCode(Cell cell, CellNetInfo cni, Technology layoutTechnology, Spice out)
	{
		double scale = layoutTechnology.getScale();

		networkList = new ArrayList<Network>();
		arcList = new ArrayList<ArcInst>();

		for (SpiceSegmentedNets segmentedNets : segmentedParasiticInfo)
        {
			if (segmentedNets.getCell() != cell) continue;
			for( Iterator<Network> itNet = cni.getNetList().getNetworks();itNet.hasNext();)
			{
				Network net = itNet.next();
				Iterator<ArcInst> itArc = net.getArcs();
				ArcInst FirstAi = itArc.next();

            	double sqrs =0;
        		double cap=0;
        		double res=0;
        		boolean startAgain=false;

                ArcInst MainAi = FirstAi;
                ArcInst CurrAi = MainAi;

                // Start with the head port instance of the first arc
                Iterator<Connection> ConIT = MainAi.getHeadPortInst().getConnections();
                PortInst MainPI = MainAi.getHeadPortInst();
                n0 = segmentedNets.getNetName(MainAi.getHeadPortInst());

                // If this network is not already analyzed
                if (networkList == null || !networkList.contains(net))
                {
                	networkList.add(net);
                   	while(ConIT.hasNext())
                	{
                   		Connection conn = ConIT.next();
                		CurrAi = conn.getArc();

                		// If this arc is not already analyzed.
                		if (!arcList.contains(CurrAi))
                		{
                			double length = CurrAi.getLambdaLength() * scale / 1000;          // length in microns
                    		double width = CurrAi.getLambdaBaseWidth() * scale / 1000;        // width in microns
                			Poly[] polya = layoutTechnology.getShapeOfArc(CurrAi);
                    		Poly poly = polya[0];
                    		if (poly.isPseudoLayer()) continue;
                    		String curLayer = poly.getLayer().getName();

                    		// If both the arcs are of the same layer ,add resistance,
                    		// capacitance and no. of squares and continue traversing.
                    		// Else print the existing data and start afresh.
                			if((preLayer == curLayer) || preLayer == "") {
                				preLayer = curLayer;
                				arcList.add(CurrAi);
                				sqrs += length / width;
                    	   		res += segmentedNets.getRes(CurrAi).doubleValue();
                    	   		cap += segmentedNets.getArcCap(CurrAi);
                			} else {
                				preLayer = curLayer;
                				arcList.add(null);
                				arcList.add(CurrAi);
                				if(sqrs > 3)
                				{
                					out.multiLinePrint(false, "XP" + tLineCount + " " + n0 + " " + n1 +" RCLINE R=" + TextUtils.formatDouble(res/sqrs, 2) + " C=" + TextUtils.formatDouble(cap/sqrs, 2) + "fF len=" + TextUtils.formatDouble(sqrs, 2) + "\n");
                					tLineCount++;
                					n0=n1;
                				}
                        		sqrs = 0;
                        		res = 0.0;
                        		cap = 0;
                        		sqrs += length / width;
                    	   		res += segmentedNets.getRes(CurrAi).doubleValue();
                    	   		cap += segmentedNets.getArcCap(CurrAi);
                			}

                			// Decide which port of the current arc to use to continue traversing the network
                			if (MainPI == CurrAi.getHeadPortInst()) {
                				MainAi = CurrAi;
                				MainPI = MainAi.getTailPortInst();
                				ConIT = MainAi.getTailPortInst().getConnections();
                				n1 = segmentedNets.getNetName(MainAi.getTailPortInst());
                			} else {
                				MainAi = CurrAi;
                				MainPI = MainAi.getHeadPortInst();
                				ConIT = MainAi.getHeadPortInst().getConnections();
                				n1 = segmentedNets.getNetName(MainAi.getHeadPortInst());
                			}
                		}

                		// Once, one end of the network is reached, start traversing from the
                		// head port instance of the first arc in the other direction now.
                		if (!ConIT.hasNext() && !startAgain)
        				{
        					ConIT = FirstAi.getHeadPortInst().getConnections();
        					MainPI = FirstAi.getHeadPortInst();
        					startAgain = true;

        					if(sqrs > 3) {
        						out.multiLinePrint(false, "XP" + tLineCount + " " + n0 + " " + n1 +" RCLINE R=" + TextUtils.formatDouble(res/sqrs, 2) + " C=" + TextUtils.formatDouble(cap/sqrs, 2) + "fF len=" + TextUtils.formatDouble(sqrs, 2) + "\n");
        						tLineCount++;
        					}
        					preLayer = "";
        					sqrs = 0;
                    		res = 0.0;
                    		cap = 0;
                    		n0=segmentedNets.getNetName(FirstAi.getHeadPortInst());
        				}
                	}
                   	if(sqrs > 3 && res > 0 && cap >0 ) {
                   		out.multiLinePrint(false, "XP" + tLineCount + " " + n0 + " " + n1 +" RCLINE R=" + TextUtils.formatDouble(res/sqrs, 2) + " C=" + TextUtils.formatDouble(cap/sqrs, 2) + "fF len=" + TextUtils.formatDouble(sqrs, 2) + "\n");
                   	    tLineCount++;
                   	}
                }
			}
        }

		// Print the subckt Spice code
        if (!alreadyPrinted){
        	out.multiLinePrint(false, ".subckt RCLINE n1 n2 \n");
        	out.multiLinePrint(false, "o1 n1 0 n2 0 TRC \n");
        	out.multiLinePrint(false, ".model TRC ltra R={R} C={C} len={len} \n");
        	out.multiLinePrint(false, ".ends RCLINE \n");
        	alreadyPrinted = true;
        }
	}
}

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
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.io.output.Topology.CellSignal;
import com.sun.electric.tool.simulation.Simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This is the simple-RC parasitics extractor for the Spice netlist writer.
 */
public class SpiceRCSimple
{
	/** List of segmented nets and parasitics */	private List<SpiceSegmentedNets> segmentedParasiticInfo;
	/** current segmented nets and parasitics */	private SpiceSegmentedNets curSegmentedNets;

	SpiceRCSimple()
	{
		segmentedParasiticInfo = new ArrayList<SpiceSegmentedNets>();
	}

	public void addSegmentedNets(SpiceSegmentedNets sn) { segmentedParasiticInfo.add(sn); }

	public List<SpiceSegmentedNets> getSegmentedNets() { return segmentedParasiticInfo; }

	public void setCurrentSegmentedNets(SpiceSegmentedNets sn) { curSegmentedNets = sn; }

	public SpiceSegmentedNets getSegmentedNets(Cell cell)
	{
		for (SpiceSegmentedNets seg : segmentedParasiticInfo)
		{
			if (seg.getCell() == cell) return seg;
		}
		return null;
	}

	public void initializeSegments(Cell cell, Netlist netList, Technology layoutTechnology,
		SpiceSegmentedNets segmentedNets, SpiceExemptedNets exemptedNets, Topology.MyCellInfo info)
	{
		double scale = layoutTechnology.getScale(); // scale to convert units to nanometers
		//System.out.println("\n	 Finding parasitics for cell "+cell.describe(false));
		HashMap<Network,Network> exemptedNetsFound = new HashMap<Network,Network>();
		for (Iterator<ArcInst> ait = cell.getArcs(); ait.hasNext(); )
		{
			ArcInst ai = ait.next();

			double cap = 0;
			double res = 0;

			//System.out.println("--Processing arc "+ai.getName());
			boolean extractNet = true;

			if (segmentedNets.isPowerGround(ai.getHeadPortInst()))
				extractNet = false;
			if (ai.getProto().getFunction() == ArcProto.Function.NONELEC)
				extractNet = false;

			Network net = netList.getNetwork(ai, 0);
			if (extractNet && Simulation.isParasiticsUseExemptedNetsFile())
			{
				// ignore nets in exempted nets file
				if (Simulation.isParasiticsIgnoreExemptedNets())
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

			if (extractNet)
			{
				// figure out res and cap, see if we should ignore it
				double length = ai.getLambdaLength() * scale / 1000;      // length in microns
				double width = ai.getLambdaBaseWidth() * scale / 1000;        // width in microns
//				double width = ai.getLambdaFullWidth() * scale / 1000;        // width in microns
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
//					if (layer.isPseudoLayer()) continue;

					if (!layer.isDiffusionLayer())
					{
						if (Simulation.isParasiticsExtractsC())
						{
							double areacap = area * layer.getCapacitance();
							double fringecap = fringe * layer.getEdgeCapacitance();
							cap = areacap + fringecap;
						}
						if (Simulation.isParasiticsExtractsR())
						{
							res = length/width * layer.getResistance();
						}
					}
				}

				int arcPImodels = SpiceSegmentedNets.getNumPISegments(res, layoutTechnology.getMaxSeriesResistance());

				// add caps
				segmentedNets.putSegment(ai.getHeadPortInst(), cap/(arcPImodels+1));
				segmentedNets.putSegment(ai.getTailPortInst(), cap/(arcPImodels+1));

				if (res <= cell.getTechnology().getMinResistance())
				{
					// short arc
					segmentedNets.shortSegments(ai.getHeadPortInst(), ai.getTailPortInst());
				} else
				{
					//System.out.println("Using resistance of "+res+" for arc "+ai.getName());
					segmentedNets.addArcRes(ai, res);
					if (arcPImodels > 1)
						segmentedNets.addArcCap(ai, cap);       // need to store cap later to break it up
				}
				segmentedNets.addExtractedNet(net);
			} else
			{
				//System.out.println("  not extracting arc "+ai.getName());
				// don't need to short arcs on networks that aren't extracted, since it is
				// guaranteed that both ends of the arc are named the same.
				//segmentedNets.shortSegments(ai.getHeadPortInst(), ai.getTailPortInst());
			}
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
					//System.out.println("--Processing gate "+ni.getName());
					PortInst gate0 = ni.getTransistorGatePort();
					PortInst gate1 = ni.getTransistorAltGatePort();
					Network gateNet0 = netList.getNetwork(gate0);
					if ((gate0 != gate1) && segmentedNets.isExtractedNet(gateNet0))
					{
						//System.out.println("Shorting gate "+ni.getName()+" ports "
						//        +gate0.getPortProto().getName()+" and "
						//        +gate1.getPortProto().getName());
						segmentedNets.shortSegments(gate0, gate1);
					}
				}
				// merge wells
			} else
			{
				//System.out.println("--Processing subcell "+ni.getName());
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
								pi1 = pi; continue;
							}
							Network net = netList.getNetwork(pi);
							if (segmentedNets.isExtractedNet(net))
								segmentedNets.shortSegments(pi1, pi);
						}
					}
				}
			}
		} // for (cell.getNodes())
	}

	public void getParasiticName(Nodable no, CellSignal subCS, StringBuffer infstr)
	{
		// connect to all exports (except power and ground of subcell net)
		SpiceSegmentedNets subSegmentedNets = getSegmentedNets((Cell)no.getProto());
		Network subNet = subCS.getNetwork();
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

	public void writeSubcircuitHeader(CellSignal cs, StringBuffer infstr)
	{
		Network net = cs.getNetwork();
		HashMap<String,List<String>> shortedExportsMap = new HashMap<String,List<String>>();
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
				// this is the first occurance of this segmented network,
				// use the name as the export (1)
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
}

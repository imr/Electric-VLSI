/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Sim.java
 * Input/output tool: ESIM, RSIM, RNL, and COSMOS output
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.io.FileType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This is the netlister for Sim.
 */
public class Sim extends Output
{
	private HashMap<Integer,String> globalNetNames;
	private int globalNetVDD, globalNetGND, globalNetPhi1H, globalNetPhi1L, globalNetPhi2H, globalNetPhi2L;
	/** key of Variable holding COSMOS attributes. */	private static final Variable.Key COSMOS_ATTRIBUTE_KEY = Variable.newKey("SIM_cosmos_attribute");

	/**
	 * The main entry point for Sim deck writing.
     * @param cell the top-level cell to write.
     * @param context the hierarchical context to the cell.
	 * @param filePath the disk file to create with Sim.
	 * @param type the type of deck being written.
	 */
	public static void writeSimFile(Cell cell, VarContext context, String filePath, FileType type)
	{
		Sim out = new Sim();
		if (out.openTextOutputStream(filePath)) return;

		out.init(cell, filePath, type);
		HierarchyEnumerator.enumerateCell(cell, context, new Visitor(out, type), true);
//		HierarchyEnumerator.enumerateCell(cellJob.cell, cellJob.context, null, new Visitor(out, cellJob.type));

		if (out.closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/**
	 * Creates a new instance of the Sim netlister.
	 */
	Sim()
	{
	}

	private static class Visitor extends HierarchyEnumerator.Visitor
	{
		private Sim generator;
		private FileType type;

		public Visitor(Sim generator, FileType type)
		{
			this.generator = generator;
			this.type = type;
		}

		public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
 			generator.writeCellContents(info, type);
			return true;
		}

		public void exitCell(HierarchyEnumerator.CellInfo info) {}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) { return true; }
	}

	private void init(Cell cell, String netfile, FileType format)
	{
		globalNetNames = new HashMap<Integer,String>();

		printWriter.println("| " + netfile);
		emitCopyright("| ", "");
		if (User.isIncludeDateAndVersionInOutput())
		{
			printWriter.println("| Cell created " + TextUtils.formatDate(cell.getCreationDate()));
			printWriter.println("| Version " + cell.getVersion() + " last revised " + TextUtils.formatDate(cell.getRevisionDate()));
		}
		
		if (format == FileType.COSMOS)
		{
			printWriter.println("| [e | d | p | n] gate source drain length width xpos ypos {[gsd]=attrs}");
			printWriter.println("| N node D-area D-perim P-area P-perim M-area M-perim");
			printWriter.println("| A node attrs");
			printWriter.println("|  attrs = [Sim:[In | Out | 1 | 2 | 3 | Z | U]]");
		} else
		{
			printWriter.println("| [epd] gate source drain length width r xpos ypos area");
			printWriter.println("| N node xpos ypos M-area P-area D-area D-perim");
		}
	}

	private void writeCellContents(HierarchyEnumerator.CellInfo ci, FileType format)
	{
		Cell cell = ci.getCell();
		Technology tech = cell.getTechnology();
		boolean top = ci.isRootCell();
		Netlist netList = ci.getNetlist();
		if (format == FileType.COSMOS) printWriter.println("| cell " + cell.getName());

		// if top level, cache all export names
		if (top)
		{
			globalNetVDD = globalNetGND = globalNetPhi1H = globalNetPhi1L = globalNetPhi2H = globalNetPhi2L = -1;
			for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
			{
				Export e = (Export)it.next();
				Network net = netList.getNetwork(e, 0);
				int globalNetNum = ci.getNetID(net);
				globalNetNames.put(new Integer(globalNetNum), e.getName());

				if (e.isPower()) globalNetVDD = globalNetNum;
				if (e.isGround()) globalNetGND = globalNetNum;
				if (e.getCharacteristic() == PortCharacteristic.C1 ||
					e.getName().startsWith("clk1") || e.getName().startsWith("phi1h")) globalNetPhi1H = globalNetNum;
				if (e.getCharacteristic() == PortCharacteristic.C2 ||
					e.getName().startsWith("phi1l")) globalNetPhi1L = globalNetNum;
				if (e.getCharacteristic() == PortCharacteristic.C3 ||
					e.getName().startsWith("clk2") || e.getName().startsWith("phi2h")) globalNetPhi2H = globalNetNum;
				if (e.getCharacteristic() == PortCharacteristic.C4 ||
					e.getName().startsWith("phi2l")) globalNetPhi2L = globalNetNum;
			}

			if (globalNetVDD < 0) System.out.println("Warning: no power export in this cell");
			if (globalNetGND < 0) System.out.println("Warning: no ground export in this cell");
		}

		// reset the arcinst node values
		Set<Network> netsSeen = new HashSet<Network>();
	
		// set every arcinst to a global node number (from inside or outside)
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			Network net = netList.getNetwork(ai, 0);
			if (netsSeen.contains(net)) continue;
			netsSeen.add(net);
			int globalNetNum = ci.getNetID(net);

			// calculate parasitics for this network
			double darea = 0, dperim = 0, parea = 0, pperim = 0, marea = 0, mperim = 0;
			for(Iterator<ArcInst> oIt = cell.getArcs(); oIt.hasNext(); )
			{
				ArcInst oAi = (ArcInst)oIt.next();
				Network oNet = netList.getNetwork(oAi, 0);
				if (oNet != net) continue;
		
				// calculate true length and width of arc
				double width = oAi.getWidth() - oAi.getProto().getWidthOffset();
				double length = oAi.getLength();
				if (oAi.isHeadExtended()) length += width/2;
				if (oAi.isTailExtended()) length += width/2;
				if (format != FileType.COSMOS)
				{
					width = TextUtils.convertDistance(width, tech, TextUtils.UnitScale.MICRO);
					length = TextUtils.convertDistance(length, tech, TextUtils.UnitScale.MICRO);
				}

				// sum up area and perimeter to total
				ArcProto.Function fun = oAi.getProto().getFunction();
				if (fun.isMetal())
				{
					marea += length * width;
					mperim += 2 * (length + width);
				} else if (fun.isPoly())
				{
					parea += length * width;
					pperim += 2 * (length + width);
				} else if (fun.isDiffusion())
				{
					darea += length * width;
					dperim += 2 * (length + width);
				}
			}
			if (globalNetNum != globalNetVDD && globalNetNum != globalNetGND &&
				(marea != 0 || parea != 0 || darea != 0))
			{
				if (format == FileType.COSMOS)
				{
					printWriter.println("N " + makeNodeName(globalNetNum, format) + " " +
						TextUtils.formatDouble(darea) + " " + TextUtils.formatDouble(dperim) + " " +
						TextUtils.formatDouble(parea) + " " + TextUtils.formatDouble(pperim) + " " +
						TextUtils.formatDouble(marea) + " " + TextUtils.formatDouble(mperim));
				} else
				{
					printWriter.println("N " + makeNodeName(globalNetNum, format) + " 0 0 " +
						TextUtils.formatDouble(marea) + " " + TextUtils.formatDouble(parea) + " " +
						TextUtils.formatDouble(darea) + " " + TextUtils.formatDouble(dperim));
				}
			}
		}

		if (format == FileType.COSMOS)
		{
			// Test each arc for attributes
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				Variable var = ai.getVar(COSMOS_ATTRIBUTE_KEY);
				if (var != null)
				{
					Network net = netList.getNetwork(ai, 0);
					int globalNetNum = ci.getNetID(net);
					printWriter.println("A " + makeNodeName(globalNetNum, format) + " Sim:" + var.getPureValue(-1));
				}
			}
		}

		// look at every nodeinst in the cell
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			PrimitiveNode.Function fun = ni.getFunction();

			// if it is a transistor, write the information
			if (fun == PrimitiveNode.Function.TRANMOS || fun == PrimitiveNode.Function.TRADMOS || fun == PrimitiveNode.Function.TRAPMOS)
			{
				Network gateNet = netList.getNetwork(ni.getTransistorGatePort());
				int gate = ci.getNetID(gateNet);

				Network sourceNet = netList.getNetwork(ni.getTransistorSourcePort());
				int source = ci.getNetID(sourceNet);

				Network drainNet = netList.getNetwork(ni.getTransistorDrainPort());
				int drain = ci.getNetID(drainNet);

				String tType = "U";
				if (fun == PrimitiveNode.Function.TRANMOS) tType = "e"; else
				if (fun == PrimitiveNode.Function.TRADMOS) tType = "d"; else
				if (fun == PrimitiveNode.Function.TRAPMOS) tType = "p";

				// determine size of transistor
				TransistorSize size = ni.getTransistorSize(ci.getContext());
				double length = 0, width = 0;
				if (size.getDoubleWidth() > 0) width = size.getDoubleWidth();
				if (size.getDoubleLength() > 0) length = size.getDoubleLength();

				if (format == FileType.COSMOS)
				{
					// see if there is an attribute mentioned on this node
					String extra = "";
					Variable var = ni.getVar(COSMOS_ATTRIBUTE_KEY);
					if (var != null) extra = " g=Sim:" + var.getPureValue(-1);
					extra += " " + TextUtils.formatDouble(ni.getAnchorCenterX()) + " " + TextUtils.formatDouble(ni.getAnchorCenterY());

					printWriter.println(tType + " " + makeNodeName(gate, format) + " " + makeNodeName(source, format) +
						" " + makeNodeName(drain, format) + " " + TextUtils.formatDouble(length) + " " +
						TextUtils.formatDouble(width) + extra);
					
					// approximate source and drain diffusion capacitances
					printWriter.println("N " + makeNodeName(source, format) + " " +
						TextUtils.formatDouble(length * width) + " " + TextUtils.formatDouble(width) + " 0 0 0 0");
	
					printWriter.println("N " + makeNodeName(drain, format) + " " +
						TextUtils.formatDouble(length * width) + " " + TextUtils.formatDouble(width) + " 0 0 0 0");
				} else
				{
					width = TextUtils.convertDistance(width, tech, TextUtils.UnitScale.MICRO);
					length = TextUtils.convertDistance(length, tech, TextUtils.UnitScale.MICRO);

					printWriter.println(tType + " " + makeNodeName(gate, format) + " " + makeNodeName(source, format) +
						" " + makeNodeName(drain, format) + " " + TextUtils.formatDouble(length) + " " +
						TextUtils.formatDouble(width) + " r 0 0 " + TextUtils.formatDouble(length * width));
		
					// approximate source and drain diffusion capacitances
					printWriter.println("N " + makeNodeName(source, format) + " 0 0 0 0 " +
						TextUtils.formatDouble(length * width) + " " + TextUtils.formatDouble(width));
	
					printWriter.println("N " + makeNodeName(drain, format) + " 0 0 0 0 " +
						TextUtils.formatDouble(length * width) + " " + TextUtils.formatDouble(width));
				}
			}
		}
	}

	/**
	 * Method to generate the name of a simulation node.
	 * Generates a string name (either a numeric node number if internal, an export
	 * name if at the top level, or "power", "ground", etc. if special).  The
	 * "format" is the particular simuator being used.
	 */
	private String makeNodeName(int globalNetNum, FileType format)
	{
		// test for special names
		if (globalNetNum == globalNetVDD) return "vdd";
		if (globalNetNum == globalNetGND) return "gnd";
		if (globalNetNum == globalNetPhi1H)
		{
			if (format == FileType.RSIM) return "phi1h";
			return "clk1";
		}
		if (globalNetNum == globalNetPhi1L) return "phi1l";
		if (globalNetNum == globalNetPhi2H)
		{
			if (format == FileType.RSIM) return "phi2h";
			return "clk2";
		}
		if (globalNetNum == globalNetPhi2L) return "phi2l";

		String name = (String)globalNetNames.get(new Integer(globalNetNum));
		if (name == null) name = Integer.toString(globalNetNum);
		return(name);
	}

}

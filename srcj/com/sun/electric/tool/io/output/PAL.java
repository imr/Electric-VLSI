/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PAL.java
 * Input/output tool: PAL Netlist output
 * Written by Steven M. Rubin, Sun Microsystems.
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
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.PrimitiveNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * This is the netlister for PAL.
 */
public class PAL extends Output
{
	private Cell topCell;
	private List<String> equations;
	private Set<String> internalSymbols;
	private Set<String> externalSymbols;
//	private PALPreferences localPrefs;

	public static class PALPreferences extends OutputPreferences
    {
		PALPreferences() {}

        public Output doOutput(Cell cell, VarContext context, String filePath)
        {
    		PAL out = new PAL(this);
    		if (out.openTextOutputStream(filePath)) return out.finishWrite();
    		out.initialize(cell);
    		PALNetlister netlister = new PALNetlister(out);
    		HierarchyEnumerator.enumerateCell(cell, context, netlister, Netlist.ShortResistors.ALL);
    		out.terminate(cell);
    		if (out.closeTextOutputStream()) return out.finishWrite();
    		System.out.println(filePath + " written");
            return out.finishWrite();
        }
    }

	/**
	 * Creates a new instance of the PAL netlister.
	 */
	PAL(PALPreferences pp) { /* localPrefs = pp; */ }

	private void initialize(Cell cell)
	{
		topCell = cell;
		equations = new ArrayList<String>();
		internalSymbols = new TreeSet<String>();
		externalSymbols = new TreeSet<String>();
	}

	private void terminate(Cell cell)
	{
		// initialize the deck
		printWriter.println("module " + cell.getName());
		printWriter.println("title 'generated by Electric'");

		// write the external and internal symbols
		int pinNumber = 1;
		for(String symbol : externalSymbols)
		{
			printWriter.println("    " + symbol + " pin " + pinNumber + ";");
			pinNumber++;
		}
		for(String symbol : internalSymbols)
			printWriter.println("    " + symbol + " = 0,1;");

		// write the equations
		printWriter.println("");
		printWriter.println("equations");
		for(String eq : equations)
			printWriter.println("    " + eq + ";");

		// end of deck
		printWriter.println("");
		printWriter.println("end " + cell.describe(false));
	}

	/** PAL Netlister */
	private static class PALNetlister extends HierarchyEnumerator.Visitor
	{
		private PAL pal;

		PALNetlister(PAL pal)
		{
			super();
			this.pal = pal;
		}

		public boolean enterCell(HierarchyEnumerator.CellInfo info) { return true; }

		public void exitCell(HierarchyEnumerator.CellInfo info) {}   

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			// if this is a cell instance, keep recursing down
			if (no.isCellInstance()) return true;

			// Nodable is NodeInst because it is primitive node
			NodeInst ni = (NodeInst)no;

			// must be a logic gate
			PrimitiveNode.Function fun = ni.getFunction();
			if (fun != PrimitiveNode.Function.GATEAND && fun != PrimitiveNode.Function.GATEOR &&
				fun != PrimitiveNode.Function.GATEXOR && fun != PrimitiveNode.Function.BUFFER) return false;

			String funName = "";
			if (fun == PrimitiveNode.Function.GATEAND) funName = "&"; else
			if (fun == PrimitiveNode.Function.GATEOR) funName = "#"; else
				if (fun == PrimitiveNode.Function.GATEXOR) funName = "$";

			// find output
			Connection outputCon = null;
			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = it.next();
				PortInst pi = con.getPortInst();
				if (pi.getPortProto().getName().equals("y")) { outputCon = con;   break; }
			}
			if (outputCon == null)
			{
				pal.reportError("ERROR: output port is not connected on " + ni + " in " + ni.getParent());
				return false;
			}

			Netlist netlist = info.getNetlist();
			StringBuffer sb = new StringBuffer();
			if (outputCon.isNegated()) sb.append("!");
			Network oNet = netlist.getNetwork(outputCon.getPortInst());
			sb.append(getNetName(oNet, info) + " =");
			int count = 0;
			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = it.next();
				PortInst pi = con.getPortInst();
				if (!pi.getPortProto().getName().equals("a")) continue;
				if (count == 1) sb.append(" " + funName);
				count++;
				sb.append(" ");
				ArcInst ai = con.getArc();
				if (con.isNegated()) sb.append("!");
				Network net = netlist.getNetwork(ai, 0);
				if (net == null) continue;
				sb.append(getNetName(net, info));
			}
			pal.equations.add(sb.toString());
			return false;
		}

		private String getNetName(Network net, HierarchyEnumerator.CellInfo info)
		{
			for(;;)
			{
				Network higher = info.getNetworkInParent(net);
				if (higher == null) break;
				net = higher;
				info = info.getParentInfo();
			}
			if (net.isExported() && info.getCell() == pal.topCell)
			{
				String exportName = net.describe(false);
				pal.externalSymbols.add(exportName);
				return exportName;
			}

			String internalName = info.getUniqueNetName(net, ".");
			pal.internalSymbols.add(internalName);
			return internalName;
		}
	}
}

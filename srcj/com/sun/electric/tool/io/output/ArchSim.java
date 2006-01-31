/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ArchSim.java
 * Input/output tool: ArchSim Netlist output
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.user.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * This is the ArchSim writer tool.
 */
public class ArchSim extends Output
{
	/**
	 * The main entry point for ArchSim deck writing.
     * @param cell the top-level cell to write.
	 * @param filePath the disk file to create with ArchSim.
	 */
	public static void writeArchSimFile(Cell cell, String filePath)
	{
		ArchSim out = new ArchSim();
		if (out.openTextOutputStream(filePath)) return;
		out.writeFlatCell(cell);
		if (out.closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/**
	 * Creates a new instance of ArchSim.
	 */
	ArchSim()
	{
	}

	private void writeFlatCell(Cell cell)
	{
		// write header information
		printWriter.println("<?xml version='1.0' encoding='utf-8'?>");
		printWriter.println();
		printWriter.println("<!DOCTYPE model SYSTEM \"ArchSimModel.dtd\">");
		printWriter.println();
		printWriter.println("<!-- Cell: " + cell.describe(true) + " -->");
		emitCopyright("<!-- ", " -->");
		if (User.isIncludeDateAndVersionInOutput())
		{
			printWriter.println("<!-- Created on " + TextUtils.formatDate(cell.getCreationDate()) + " -->");
			printWriter.println("<!-- Last revised on " + TextUtils.formatDate(cell.getRevisionDate()) + " -->");
			printWriter.println("<!-- Written on " + TextUtils.formatDate(new Date()) +
				" by Electric VLSI Design System, version " + Version.getVersion() + " -->");
		} else
		{
			printWriter.println("<!-- Written by Electric VLSI Design System -->");
		}
		printWriter.println();
		printWriter.println("<model name= \"" + cell.getName() + "\">");
		printWriter.println();

		// write all components
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (!ni.isCellInstance()) continue;
			if (ni.isIconOfParent()) continue;
			Cell subCell = (Cell)ni.getProto();
			printWriter.println("<component name= \"" + ni.getName() + "\" type= \"" + subCell.getName() + "\" />");
		}
		printWriter.println();

		// write all connections
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted netlisting (network information unavailable).  Please try again");
			return;
		}
		for(Iterator<Network> it = netlist.getNetworks(); it.hasNext(); )
		{
			Network net = it.next();
			List<PortInst> inputs = new ArrayList<PortInst>();
			List<PortInst> outputs = new ArrayList<PortInst>();
			for(Iterator<NodeInst> oIt = cell.getNodes(); oIt.hasNext(); )
			{
				NodeInst ni = oIt.next();
				if (!ni.isCellInstance()) continue;
				if (ni.isIconOfParent()) continue;
				for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext();)
				{
					Connection con = cIt.next();
					PortInst pi = con.getPortInst();
					Network nodeNet = netlist.getNetwork(pi);
					if (nodeNet != net) continue;
					PortCharacteristic pc = pi.getPortProto().getCharacteristic(); 
					if (pc == PortCharacteristic.IN) inputs.add(pi); else
						if (pc == PortCharacteristic.OUT) outputs.add(pi); else
					{
						System.out.println("Export " + pi.getPortProto().getName() + " of " + ni.getProto() +
							" is neither input or output (it is " + pc.getFullName() + ")");
					}
				}
			}

			// ignore if no inputs or outputs
			if (inputs.size() == 0 && outputs.size() == 0) continue;

			// get the network name
			String netName = null;
			Iterator<String> nIt = net.getNames();
			if (nIt.hasNext()) netName = nIt.next(); else
				netName = net.describe(true);
			printWriter.println();
			printWriter.println("<connection name= \"" + netName + "\">");

			// write the output connections ("from")
			for(PortInst pi : outputs)
			{
				printWriter.println("\t<from component= \"" + pi.getNodeInst().getName() + "\" terminal=\"" +
					pi.getPortProto().getName() + "\" />");
			}

			// write the input connections ("to")
			for(PortInst pi : inputs)
			{
				printWriter.println("\t<to component= \"" + pi.getNodeInst().getName() + "\" terminal=\"" +
					pi.getPortProto().getName() + "\" />");
			}
			printWriter.println("</connection>");
		}
		printWriter.println();
		printWriter.println("</model>");
	}
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ECAD.java
 * Input/output tool: ECAD netlist output
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
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class to write ECAD netlists.
 * <BR>
 * Format:<BR>
 * .cmp (t=pkg_type) ref_des (x=0, y=0)<BR>
 * <BR>
 * .cmp (t=LED) LED1<BR>
 * .cmp (t=RES100) R1<BR>
 * <BR>
 * Net_name : ref_des pad; \<BR>
 *            ref_des pad;<BR>
 * <BR>
 * A0 : LED1 1; R1 2;<BR>
 * A1 : LED1 2; R1 1;<BR>
 */
public class ECAD extends Output
{
	/** key of Variable holding node name. */				public static final Variable.Key REF_DES_KEY = Variable.newKey("ATTR_ref_des");
	/** key of Variable holding package type. */			public static final Variable.Key PKG_TYPE_KEY = Variable.newKey("ATTR_pkg_type");
	/** key of Variable holding pin information. */			public static final Variable.Key PIN_KEY = Variable.newKey("ATTR_pin");

	private List<NetNames> networks;

	/**
	 * Creates a new instance of ECAD netlister.
	 */
	private ECAD()
	{
	}

	/**
	 * The main entry point for ECAD deck writing.
     * @param cell the top-level cell to write.
     * @param context the hierarchical context to the cell.
	 * @param filePath the disk file to create.
	 */
	public static void writeECADFile(Cell cell, VarContext context, String filePath)
	{
		ECAD out = new ECAD();
		out.writeNetlist(cell, context, filePath);
	}

	private void writeNetlist(Cell cell, VarContext context, String filePath)
	{
		if (openTextOutputStream(filePath)) return;

		networks = new ArrayList<NetNames>();
		ECADNetlister netlister = new ECADNetlister();
		HierarchyEnumerator.enumerateCell(cell, context, netlister, true);
//		Netlist netlist = cell.getNetlist(true);
//		HierarchyEnumerator.enumerateCell(cell, context, netlist, netlister);
		printWriter.println("| end of part list");

		// warn the user if nets not found
		if (networks.size() == 0)
		{
			System.out.println("ERROR: no output produced.  Packages need attribute 'ref_des' and ports need attribute 'pin'");
		}

		// add all network pairs
		Collections.sort(networks, new NetNamesSort());
		for(int i=0; i<networks.size(); i++)
		{
			NetNames nn = networks.get(i);
			String baseName = nn.netName;
			int endPos = i;
			for(int j=i+1; j<networks.size(); j++)
			{
				NetNames oNn = networks.get(j);
				if (!oNn.netName.equals(baseName)) break;
				endPos = j;
			}
			if (endPos == i) continue;
			for(int j=i; j<=endPos; j++)
			{
				NetNames oNn = networks.get(j);
				if (j == i) printWriter.print(baseName + " :");
				printWriter.println("     " + oNn.nodeName + "    " + oNn.portName + " \\");
			}
			printWriter.println(";");
		}
		printWriter.println("|end of net section.");

		if (closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/** ECAD Netlister */
	private class ECADNetlister extends HierarchyEnumerator.Visitor
	{
		public boolean enterCell(HierarchyEnumerator.CellInfo info) { return true; }

		public void exitCell(HierarchyEnumerator.CellInfo info) {}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			if (!no.isCellInstance()) return false;

			// if node doesn't have "ref_des" on it, recurse down the hierarchy
			Variable var = no.getVar(REF_DES_KEY);
			if (var == null) return true;

			// found symbol name: emit it
			String context = "";
			Nodable pNo = info.getParentInst();
			HierarchyEnumerator.CellInfo parentInfo = info.getParentInfo();
			if (parentInfo != null && pNo != null) context = parentInfo.getUniqueNodableName(pNo, ".") + ".";
			String nodeName = context + var.getPureValue(-1);
			String pkgType = no.getProto().getName();
			Variable pkgName = no.getVar(PKG_TYPE_KEY);
			if (pkgName != null) pkgType = pkgName.getPureValue(-1);
			printWriter.println(".cmp (t=" + pkgType + ") " + nodeName + " (x=0, y=0)");

			// save all networks on this node for later
			for(Iterator<PortProto> it = no.getProto().getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				String pName = null;
				PortInst pi = no.getNodeInst().findPortInstFromProto(pp);
				Variable pVar = pi.getVar(PIN_KEY);
				if (pVar != null) pName = pVar.getPureValue(-1); else
				{
					pVar = pp.getVar(PIN_KEY);
					if (pVar != null) pName = pVar.getPureValue(-1);
				}
				if (pName == null) continue;
				int [] ids = info.getPortNetIDs(no, pp);
				for(int i=0; i<ids.length; i++)
				{
					NetNames nn = new NetNames();
					nn.netName = info.getUniqueNetName(ids[i], ".");
					nn.nodeName = nodeName;
					nn.portName = pName;
					networks.add(nn);
				}
			}
			return false;
		}
	}
}

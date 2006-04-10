/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Pads.java
 * Input/output tool: Pads netlist output
 * Original C Code written by Neil Levine, NAL Consulting for Intel Corp.
 * Translated to Java by Steven M. Rubin, Sun Microsystems.
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
 * Class to write Pads netlists.
 * <BR>
 * Format:<BR>
 *   !PADS-POWERPCB-V2<BR>
 * <BR>
 *   *PART*<BR>
 *   LED1 LED@LED<BR>
 *   R1 RES100@RES100<BR>
 * <BR>
 *   *NET*<BR>
 *   *SIGNAL* A0 12,00 0 0 0 -2<BR>
 *   LED1.1 R1.2<BR>
 *   *SIGNAL* A1 12.00 0 0 0 -2<BR>
 *   LED1.2 R1.1<BR>
 * <BR>
 *   *END*
 */
public class Pads extends Output
{
	/** key of Variable holding node name. */				public static final Variable.Key REF_DES_KEY = Variable.newKey("ATTR_ref_des");
	/** key of Variable holding package type. */			public static final Variable.Key PKG_TYPE_KEY = Variable.newKey("ATTR_pkg_type");
	/** key of Variable holding pin information. */			public static final Variable.Key PIN_KEY = Variable.newKey("ATTR_pin");

	private List<NetNames> networks;

	/**
	 * Creates a new instance of Pads netlister.
	 */
	private Pads()
	{
	}

	/**
	 * The main entry point for Pads deck writing.
     * @param cell the top-level cell to write.
     * @param context the hierarchical context to the cell.
	 * @param filePath the disk file to create.
	 */
	public static void writePadsFile(Cell cell, VarContext context, String filePath)
	{
		Pads out = new Pads();
		out.writeNetlist(cell, context, filePath);
	}

	private void writeNetlist(Cell cell, VarContext context, String filePath)
	{
		if (openTextOutputStream(filePath)) return;

		printWriter.println("!PADS-POWERPCB-V2");
		printWriter.println("");
		printWriter.println("*CLUSTER* ITEM");
		printWriter.println("");
		printWriter.println("*PART*");
		networks = new ArrayList<NetNames>();
		PadsNetlister netlister = new PadsNetlister();
		HierarchyEnumerator.enumerateCell(cell, context, netlister, true);
//		Netlist netlist = cell.getNetlist(true);
//		HierarchyEnumerator.enumerateCell(cell, context, netlist, netlister);
		printWriter.println("");
		printWriter.println("*NET*");

		// warn the user if nets not found
		if (networks.size() == 0)
		{
			System.out.println("ERROR: no output produced.  Packages need attribute 'ref_des' and ports need attribute 'pin'");
		}

		// add all network pairs
		Collections.sort(networks, new NetNamesSort());
		for(int i=0; i<networks.size(); i++)
		{
			NetNames nn = (NetNames)networks.get(i);
			String baseName = nn.netName;
			int endPos = i;
			for(int j=i+1; j<networks.size(); j++)
			{
				NetNames oNn = (NetNames)networks.get(j);
				if (!oNn.netName.equals(baseName)) break;
				endPos = j;
			}
			if (endPos == i) continue;
			printWriter.println("*SIGNAL* "+ baseName);
			for(int j=i; j<=endPos; j++)
			{
				NetNames oNn = (NetNames)networks.get(j);
				printWriter.println(oNn.nodeName + "." + oNn.portName);
			}
			printWriter.println("");
		}
		printWriter.println("*END*");

		if (closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/** Pads Netlister */
	private class PadsNetlister extends HierarchyEnumerator.Visitor
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
			printWriter.println(nodeName + "  " + pkgType + "@" + pkgType);

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

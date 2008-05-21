/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Spice.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * These are nets that are either extracted when nothing else is extracted,
 * or not extracted during extraction.  They are specified via the top level
 * net cell + name, any traversal of that net down the hierarchy is also not extracted.
 */
class SpiceExemptedNets
{
	private Map<Cell,List<Net>> netsByCell;         // key: cell, value: List of ExemptedNets.Net objects
	private Set<Integer> exemptedNetIDs;

	private static class Net
	{
		private String name;
		private double replacementCap;
	}

	public SpiceExemptedNets(File file)
	{
		netsByCell = new HashMap<Cell,List<Net>>();
		exemptedNetIDs = new TreeSet<Integer>();

		try
		{
			FileReader reader = new FileReader(file);
			BufferedReader br = new BufferedReader(reader);
			String line;
			int lineno = 1;
			System.out.println("Using exempted nets file "+file.getAbsolutePath());
			while ((line = br.readLine()) != null)
			{
				processLine(line, lineno);
				lineno++;
			}
		} catch (IOException e)
		{
			System.out.println(e.getMessage());
			return;
		}
	}

	private void processLine(String line, int lineno)
	{
		if (line == null) return;
		if (line.trim().equals("")) return;
		if (line.startsWith("#")) return; // comment
		String parts[] = line.trim().split("\\s+");
		if (parts.length < 3)
		{
			System.out.println("Error on line "+lineno+": Expected 'LibraryName CellName NetName', but was "+line);
			return;
		}
		Cell cell = getCell(parts[0], parts[1]);
		if (cell == null) return;
		double cap = 0;
		if (parts.length > 3)
		{
			try
			{
				cap = Double.parseDouble(parts[3]);
			} catch (NumberFormatException e) {
				System.out.println("Error on line "+lineno+" "+e.getMessage());
			}
		}

		List<Net> list = netsByCell.get(cell);
		if (list == null)
		{
			list = new ArrayList<Net>();
			netsByCell.put(cell, list);
		}
		Net n = new Net();
		n.name = parts[2];
		n.replacementCap = cap;
		list.add(n);
	}

	private Cell getCell(String library, String cell)
	{
		Library lib = Library.findLibrary(library);
		if (lib == null)
		{
			System.out.println("Could not find library "+library);
			return null;
		}
		Cell c = lib.findNodeProto(cell);
		if (c == null)
		{
			System.out.println("Could not find cell "+cell+" in library "+library);
			return null;
		}
		return c;
	}

	/**
	 * Get the netIDs for all exempted nets in the cell specified by the CellInfo
	 * @param info
	 */
	public void setExemptedNets(HierarchyEnumerator.CellInfo info)
	{
		Cell cell = info.getCell();
		List<Net> netNames = netsByCell.get(cell);
		if (netNames == null) return;   // nothing for this cell
		for (Net n : netNames)
		{
			String netName = n.name;
			Network net = findNetwork(info, netName);
			if (net == null)
			{
				System.out.println("Cannot find network "+netName+" in cell "+cell.describe(true));
				continue;
			}

			// get the global ID
			System.out.println("exemptedNets: specified net "+cell.describe(false)+" "+netName);
			int netID = info.getNetID(net);
			exemptedNetIDs.add(new Integer(netID));
		}
	}

	private Network findNetwork(HierarchyEnumerator.CellInfo info, String name)
	{
		for (Iterator<Network> it = info.getNetlist().getNetworks(); it.hasNext(); )
		{
			Network net = it.next();
			if (net.hasName(name)) return net;
		}
		return null;
	}

	public boolean isExempted(int netID)
	{
		return exemptedNetIDs.contains(new Integer(netID));
	}

	public double getReplacementCap(Cell cell, Network net)
	{
		List<Net> netNames = netsByCell.get(cell);
		if (netNames == null) return 0;      // nothing for this cell
		for (Net n : netNames)
		{
			if (net.hasName(n.name)) return n.replacementCap;
		}
		return 0;
	}
}

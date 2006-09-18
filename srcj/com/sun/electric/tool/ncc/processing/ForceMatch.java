/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExportChecker.java
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
package com.sun.electric.tool.ncc.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.netlist.Wire;
import com.sun.electric.tool.ncc.strategy.Strategy;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

public class ForceMatch {
	private static class PartitionEquivRecord extends Strategy {
		private Map<NetObject, Integer> netObjToIndex;
		
		@Override
		public Integer doFor(NetObject n) {
			Integer index = netObjToIndex.get(n);
			// add 1000 to index to avoid collision with 0 which means "no change"
			return index==null ? 0 : (1000+index.intValue());
		}
		
		public PartitionEquivRecord(EquivRecord er, 
				                    Map<NetObject, Integer> netObjToIndex, 
				                    NccGlobals globals) {
			super(globals);
			this.netObjToIndex = netObjToIndex;
			doFor(er);
		}
	}
	private NccGlobals globals;
	
	private void getNamesOfPartsAndWiresToForceMatch(List<String> partNames, 
			                                         List<String> wireNames) {
		// eliminate duplicates in case of user error
		Set<String> forcePartNames = new HashSet<String>();
		Set<String> forceWireNames = new HashSet<String>();

		Cell[] cells = globals.getRootCells();
		for (int i=0; i<cells.length; i++) {
			Cell c = cells[i];
			NccCellAnnotations ann = NccCellAnnotations.getAnnotations(c);
			if (ann==null) continue;
			forcePartNames.addAll(ann.getForcePartMatches());
			forceWireNames.addAll(ann.getForceWireMatches());
		}
		partNames.addAll(forcePartNames);
		wireNames.addAll(forceWireNames);
	}
	
	private boolean nameMatches(String name, NetObject nObj) {
		if (nObj instanceof Wire) {
			return ((Wire)nObj).getNameProxy().getNet().hasName(name); 
		} else {
			globals.error(!(nObj instanceof Part), "not Part or Wire");
			return ((Part)nObj).getNameProxy().leafName().equals(name);
		}
	}
	private int indexOfMatchingName(List<String> forceNames, NetObject nObj) {
		for (int i=0; i<forceNames.size(); i++) {
			if (nameMatches(forceNames.get(i), nObj)) return i;
		}
		return -1;
	}
	/** Make sure that for each name we have one NetObject for each root Cell.
	 * Print messages saying we're forcing matches.
	 * Print messages if we can't find names. 
	 * clear Netobject if name match is incomplete.
	 * @param forceNames
	 * @param forceNetObjs NetObject[cktIndex][nameIndex] */
	private Map<NetObject, Integer> buildForceNetObjMap(List<String> forceNames, 
			                                            NetObject[][] forceNetObjs,
			                                            String netObjType) {
		Map<NetObject, Integer> netObjToNdx = new HashMap<NetObject, Integer>();
		
		for (int nameNdx=0; nameNdx<forceNames.size(); nameNdx++) {
			String forceName = forceNames.get(nameNdx);
			boolean haveNetObjForEachRootCell = true;
			Cell[] rootCells = globals.getRootCells();
			for (int desNdx=0; desNdx<forceNetObjs.length; desNdx++) {
				if (forceNetObjs[desNdx][nameNdx]==null) {
					globals.prln("  forceMatch: Can't find "+netObjType+
							     " named: "+forceName+
							     " in Cell: "+rootCells[desNdx].describe(false));
					haveNetObjForEachRootCell = false;
				}
			}
			if (haveNetObjForEachRootCell) {
				globals.pr("  Forcing match of "+netObjType+
						   " named: "+forceName+" in Cells: ");
				for (int desNdx=0; desNdx<rootCells.length; desNdx++) {
					globals.pr(rootCells[desNdx].describe(false)+" ");
				}
				globals.prln("");
				for (int desNdx=0; desNdx<forceNetObjs.length; desNdx++) {
					netObjToNdx.put(forceNetObjs[desNdx][nameNdx], nameNdx);
				}
			}
		}
		
		return netObjToNdx;
	}
	
	private Cell getParentCell(NetObject nObj) {
		if (nObj instanceof Wire) {
			return ((Wire)nObj).getNameProxy().leafCell();
		} else {
			globals.error(!(nObj instanceof Part), "not a Part or a Wire?");
			return ((Part)nObj).getNameProxy().leafCell();
		}
	}
	/** @return NetObject[circuitIndex][nameIndex] */
	private NetObject[][] findNetObjsToForceMatch(EquivRecord equivRec,
			                                      List<String> forceNames) {
		NetObject[][] forceNetObjs = new NetObject[globals.getRootCells().length][];
		                               
		Cell[] rootCells = globals.getRootCells();
		int cktNdx = 0;
		for (Iterator<Circuit> cit=equivRec.getCircuits(); cit.hasNext(); cktNdx++) {
			Circuit ckt = cit.next();
			Cell rootCell = rootCells[cktNdx];
			forceNetObjs[cktNdx] = new NetObject[forceNames.size()];
			for (Iterator<NetObject> nit=ckt.getNetObjs(); nit.hasNext();) {
				NetObject nObj = nit.next();
				if (getParentCell(nObj)==rootCell) {
					int ndx = indexOfMatchingName(forceNames, nObj);
					if (ndx!=-1) {
						forceNetObjs[cktNdx][ndx] = nObj;
					}
				}
			}
		}
		return forceNetObjs;
	}
	
	private void forceWireMatches(Set<Part> forcedMatchParts, 
			                      Set<Wire> forcedMatchWires) {
		List<String> forcePartNames = new ArrayList<String>(); 
		List<String> forceWireNames = new ArrayList<String>(); 
		getNamesOfPartsAndWiresToForceMatch(forcePartNames, forceWireNames);

		// Cells may have no Parts. Cells with no Parts and no Exports have no Wires
		if (globals.getWires()!=null) {
			NetObject[][] forceWires = 
				findNetObjsToForceMatch(globals.getWires(), forceWireNames);
			Map<NetObject, Integer> wireToIndex = 
				buildForceNetObjMap(forceWireNames, forceWires, "Wire");
			new PartitionEquivRecord(globals.getWires(), wireToIndex, globals);
			for (NetObject nObj : wireToIndex.keySet()) 
				forcedMatchWires.add((Wire)nObj);
		}
		if (globals.getParts()!=null) {
			NetObject[][] forceParts = 
				findNetObjsToForceMatch(globals.getParts(), forcePartNames);
			Map<NetObject, Integer> partToIndex = 
				buildForceNetObjMap(forcePartNames, forceParts, "Part");
			new PartitionEquivRecord(globals.getParts(), partToIndex, globals);
			for (NetObject nObj : partToIndex.keySet())
				forcedMatchParts.add((Part)nObj);
		}
	}
	
	private ForceMatch(Set<Part> forcedMatchParts, 
			           Set<Wire> forcedMatchWires, NccGlobals g) {
		globals = g;
		forceWireMatches(forcedMatchParts, forcedMatchWires);
	}

	/** Must be called before any other partitioning occurs. It assumes that
	 * all the Wires are in a single, root partition; and that all Parts are
	 * in a single, root partition.
	 * @param forcedMatchParts Parts that were matched
	 * @param forcedMatchWires Wires that were matched
	 * @param g Ncc Globals */
	public static void doYourJob(Set<Part> forcedMatchParts, 
			                     Set<Wire> forcedMatchWires, NccGlobals g) {
		new ForceMatch(forcedMatchParts, forcedMatchWires, g);
	}
}

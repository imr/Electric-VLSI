/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratPortName.java
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
package com.sun.electric.tool.ncc.strategy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.lists.RecordList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Port;
import com.sun.electric.tool.ncc.netlist.Wire;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/**
 * StratPortName sorts all Wire EquivRecords on the frontier by
 * size. Starting with the smallest first, it finds the first
 * EquivRecord that splits due to Export names. It divides that
 * EquivRecord and returns the offspring EquivRecords.
 */
public class StratPortName extends Strategy {
	private int numWiresProcessed;
	private int numEquivProcessed;
	private Map<Wire,Integer> theMap;
	private boolean doneOne;
	
	private StratPortName(NccGlobals globals) {super(globals);}
	
	public static LeafList doYourJob(NccGlobals globals) {
		// if no Wires suppress all StratPortName messages
		if (globals.getWires()==null) return new LeafList(); 
		
		StratPortName wn = new StratPortName(globals);
		wn.preamble();
        //LeafList front = StratFrontier.doYourJob(globals.getWires(), globals);
		Iterator<EquivRecord> frontier = globals.getWireLeafEquivRecs().getNotMatched();
		LeafList ll = new LeafList();
		while (frontier.hasNext()) ll.add(frontier.next());

		LeafList el = wn.doFor(ll);
		wn.summary(el);
		return el;
	}
		
	private void preamble() {
		startTime("StratPortName", "all active Wires");
	}
	
	private void summary(LeafList offspring){
		globals.status2("StratPortName processed " +
							numWiresProcessed + " Wires from " +
							numEquivProcessed + " EquivRecords");
		globals.status2(offspringStats(offspring));

		globals.status2(offspring.sizeInfoString());
		elapsedTime();
	}
	
	/** 
	 * printTheMap is a debug routine that exhibits the map.
	 * @param m the Map to exhibit
	 */
	private void printTheMap(Map<Wire,Integer> m){
		globals.status2("  printing an EquivRecord map of size= " + m.size());
		if(m.size() == 0)return;
		for (Wire w : m.keySet()) {
			Object oo= m.get(w);
			if(oo == null){
				globals.status2(" "+w.instanceDescription() + " maps to null");
			} else {
				Integer i= (Integer)oo;
				globals.status2(" "+w.instanceDescription() + " maps to " + i.intValue());
			}
		}
	}
	private Map<String,Wire> getMapFromExportNamesToWires(Circuit wires){
		Map<String,Wire> out = new HashMap<String,Wire>();
		for (Iterator<NetObject> it=wires.getNetObjs(); it.hasNext();) {
			NetObject n= it.next();
			error(!(n instanceof Wire), "getExportMap expects only Wires");
			Wire w= (Wire)n;
			Port p = w.getPort();
			if (p!=null && !p.getToBeRenamed()) {
				for (Iterator<String> ni=p.getExportNames(); ni.hasNext();) {
					String exportNm = ni.next();
					error(out.containsKey(exportNm),
						  "different wires have the same export name?");
					out.put(exportNm, w);
				}
			}
		}
		return out;
	}

	/** 
	 * getWireExportMap produces a map of Wires to arbitrary Integers
	 * based on matching export names.
	 * @return a map of Wires to Integers
	 */
	private Map<Wire,Integer> getWireExportMap(EquivRecord er){
		//step 1 - get the string maps from the circuits
		List<Map<String,Wire>> mapPerCkt = new ArrayList<Map<String,Wire>>(); //to hold the circuit's maps
		Set<String> keys = new HashSet<String>();
		for (Iterator<Circuit> ci=er.getCircuits(); ci.hasNext();) {
			Map<String,Wire> exportToWire = getMapFromExportNamesToWires(ci.next());
			mapPerCkt.add(exportToWire);
			keys.addAll(exportToWire.keySet());
		}
		//keys now holds all possible Strings that are names
		if (keys.size()==0)  return new HashMap<Wire,Integer>(); //no ports
		HashMap<Wire,Integer> out = new HashMap<Wire,Integer>();
		int i= 0;
		for (String key : keys) {
			//check that all maps have this key
			List<Wire> wires = new ArrayList<Wire>();
			for (Map<String,Wire> map : mapPerCkt) {
				if(map.containsKey(key)){
					Wire w= map.get(key);
					wires.add(w);
				}
			}
			//does wires contain enough records?
			if(wires.size() == mapPerCkt.size()){
				//yes it does
				i++;
				for (Wire w : wires) {
					out.put(w, new Integer(i));
				}
			}
		}
		printTheMap(out);
		return out;
	}

    public LeafList doFor(RecordList g){
		LeafList gg= (LeafList)g;
		gg.sortByIncreasingSize();
		return super.doFor(gg);
    }
	
    public LeafList doFor(EquivRecord g){
		LeafList out;
		if(g.isLeaf()){
			EquivRecord er= (EquivRecord)g;
			if (doneOne) return new LeafList();
			numEquivProcessed++;
			theMap = getWireExportMap(er);
			if (theMap.size()==0) return new LeafList();
			doneOne = true; 
			
			out = super.doFor(g);
			globals.status2(" processed "+g.nameString()+
							" with map size= "+theMap.size()+" yields " 
							+out.size()+" offspring ");
		} else {
			out = super.doFor(g);
		}
		return out;
    }
    
	public Integer doFor(NetObject n){
		error(!(n instanceof Wire), "StratPortName expects only Wires");
		numWiresProcessed++;
		Wire w= (Wire)n;
		Integer i= theMap.get(w);
		return i!=null ? i : new Integer(0);
	}
	
}

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
import com.sun.electric.tool.ncc.*;
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

import java.util.Map;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * StratPortName sorts all Wire EquivRecords on the frontier by
 * size. Starting with the smallest first, it finds the first
 * EquivRecord that splits due to Export names. It divides that
 * EquivRecord and returns the offspring EquivRecords.
 */
public class StratPortName extends Strategy {
	private int numWiresProcessed;
	private int numEquivProcessed;
	private Map theMap;
	private boolean doneOne;
	
	private StratPortName(NccGlobals globals) {super(globals);}
	
	public static LeafList doYourJob(NccGlobals globals) {
		// if no Wires suppress all StratPortName messages
		if (globals.getWires()==null) return new LeafList(); 
		
		StratPortName wn = new StratPortName(globals);
		wn.preamble();
        LeafList front = StratFrontier.doYourJob(globals.getWires(), globals);
		LeafList el = wn.doFor(front);
		wn.summary(el);
		return el;
	}
		
	//do something before starting
	private void preamble() {
		startTime("StratPortName", "all active Wires");
	}
	
	//summarize at the end
	private void summary(LeafList offspring){
		//RecordList out= EquivRecord.tryToRetire(cc);
		globals.println("StratPortName processed " +
							numWiresProcessed + " Wires from " +
							numEquivProcessed + " EquivRecords");
		globals.println(offspringStats(offspring));

		globals.println(offspring.sizeInfoString());
		elapsedTime();
	}
	
	/** 
	 * printTheMap is a debug routine that exhibits the map.
	 * @param the Map to exhibit
	 */
	private void printTheMap(Map m){
		globals.println("  printing an EquivRecord map of size= " + m.size());
		if(m.size() == 0)return;
		for (Iterator it=m.keySet().iterator(); it.hasNext();) {
			Wire w= (Wire)it.next();
			Object oo= m.get(w);
			if(oo == null){
				globals.println(" "+w.nameString() + " maps to null");
			} else {
				Integer i= (Integer)oo;
				globals.println(" "+w.nameString() + " maps to " + i.intValue());
			}
		}
	}
	private Map getMapFromExportNamesToWires(Circuit wires){
		Map out = new HashMap();
		for (Iterator it=wires.getNetObjs(); it.hasNext();) {
			NetObject n= (NetObject)it.next();
			error(!(n instanceof Wire), "getExportMap expects only Wires");
			Wire w= (Wire)n;
			Port p = w.getPort();
			if (p!=null && !p.getToBeRenamed()) {
				for (Iterator ni=p.getExportNames(); ni.hasNext();) {
					String exportNm = (String) ni.next();
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
	private Map getWireExportMap(EquivRecord er){
		//step 1 - get the string maps from the circuits
		List mapPerCkt = new ArrayList(); //to hold the circuit's maps
		Set keys = new HashSet();
		for (Iterator ci=er.getCircuits(); ci.hasNext();) {
			Map exportToWire = getMapFromExportNamesToWires((Circuit)ci.next());
			mapPerCkt.add(exportToWire);
			keys.addAll(exportToWire.keySet());
		}
		//keys now holds all possible Strings that are names
		if (keys.size()==0)  return new HashMap(); //no ports
		HashMap out = new HashMap();
		int i= 0;
		for (Iterator ki=keys.iterator(); ki.hasNext();) {
			String key = (String)ki.next();
			//check that all maps have this key
			List wires = new ArrayList();
			for (Iterator hi=mapPerCkt.iterator(); hi.hasNext();) {
				Map map = (Map)hi.next();
				if(map.containsKey(key)){
					Wire w= (Wire)map.get(key);
					wires.add(w);
				}
			}
			//does wires contain enough records?
			if(wires.size() == mapPerCkt.size()){
				//yes it does
				i++;
				for (Iterator hi= wires.iterator(); hi.hasNext();) {
					Wire w= (Wire)hi.next();
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
			globals.println(" processed "+g.nameString()+
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
		Integer i= (Integer)theMap.get(w);
		return i!=null ? i : new Integer(0);
	}
	
}

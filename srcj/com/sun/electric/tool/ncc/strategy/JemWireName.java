/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemWireName.java
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

/**
 *  Splits Wire JemCircuit according to matching names
 */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.basicA.Messenger;
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
 * JemWireName sorts all Wire JemEquivRecords on the frontier by
 * size. Starting with the smallest first, it finds the first
 * JemEquivRecord that splits due to Export names. It divides that
 * JemEquivRecord and returns the offspring JemEquivRecords.
 */
public class JemWireName extends JemStrat {
	private int numWiresProcessed;
	private int numEquivProcessed;
	private Map theMap;
	private boolean doneOne;
	
	private JemWireName(){}
	
	public static JemEquivList doYourJob(JemSets jss){
        JemEquivList frontier= JemStratFrontier.doYourJob(jss.wires);
		return doYourJob(frontier);
	}

	private static JemEquivList doYourJob(JemRecordList l) {
		JemWireName wn = new JemWireName();
		wn.preamble(l);
		JemEquivList el = wn.doFor(l);
		wn.summary(el);
		return el;
	}
		
	//do something before starting
	private void preamble(JemRecordList j){
		startTime("JemWireName", " a list of "+j.size());
	}
	
	//summarize at the end
	private void summary(JemEquivList offspring){
		//JemRecordList out= JemEquivRecord.tryToRetire(cc);
		Messenger.line("JemWireName processed " +
							numWiresProcessed + " Wires from " +
							numEquivProcessed + " JemEquivRecords");
		Messenger.line(offspringStats(offspring));

		Messenger.line(offspring.sizeInfoString());
		elapsedTime(numWiresProcessed);
	}
	
	/** 
	 * printTheMap is a debug routine that exhibits the map.
	 * @param the Map to exhibit
	 */
	private static void printTheMap(Map m){
		Messenger.line("printing an EquivRecord map of size= " + m.size());
		if(m.size() == 0)return;
		for (Iterator it=m.keySet().iterator(); it.hasNext();) {
			Wire w= (Wire)it.next();
			Object oo= m.get(w);
			if(oo == null){
				Messenger.line(w.nameString() + " maps to null");
			} else {
				Integer i= (Integer)oo;
				Messenger.line(w.nameString() + " maps to " + i.intValue());
			}
		}
	}
	
	/** 
	 * getWireExportMap produces a map of Wires to arbitrary Integers
	 * based on matching export names.
	 * @return a map of Wires to Integers
	 */
	private Map getWireExportMap(JemEquivRecord er){
		//step 1 - get the string maps from the circuits
		List mapPerCkt = new ArrayList(); //to hold the circuit's maps
		Set keys = new HashSet();
		for (Iterator ci=er.getCircuits(); ci.hasNext();) {
			JemCircuit jc= (JemCircuit)ci.next();
			Map map= jc.getExportMap();
			mapPerCkt.add(map);
			keys.addAll(map.keySet());
		}
		//keys now holds all possible Strings that are names
		if (keys.size()==0)  return new HashMap(); //no ports
		HashMap out = new HashMap();
		int i= 0;
		for (Iterator ki=keys.iterator(); ki.hasNext();) {
			String key = (String)ki.next();
			//check that all maps have this key
			List wires = new ArrayList(2);
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

	// ---------- for JemRecordList -------------
	
    public JemEquivList doFor(JemRecordList g){
		JemEquivList gg= (JemEquivList)g;
		gg.sortByIncreasingSize();
		return super.doFor(gg);
    }
	
	// ---------- for JemRecord -------------
	
    public JemEquivList doFor(JemRecord g){
		JemEquivList out;
		if(g instanceof JemHistoryRecord){
			out = super.doFor(g);
		} else {
			error(!(g instanceof JemEquivRecord), "unrecognized JemRecord");
			JemEquivRecord er= (JemEquivRecord)g;
			if (doneOne) return new JemEquivList();
			numEquivProcessed++;
			theMap = getWireExportMap(er);
			if (theMap.size()==0) return new JemEquivList();
			doneOne = true; 
			
			out = super.doFor(g);
			Messenger.line("processed "+g.nameString()+
								" with map size= "+theMap.size()+" yields " 
							    +out.size()+" offspring ");
		}
		return out;
    }
    

	
	//------------- for NetObject ------------
	public Integer doFor(NetObject n){
		error(!(n instanceof Wire), "JemWireName expects only Wires");
		numWiresProcessed++;
		Wire w= (Wire)n;
		Integer i= (Integer)theMap.get(w);
		return i!=null ? i : new Integer(0);
	}
	
}

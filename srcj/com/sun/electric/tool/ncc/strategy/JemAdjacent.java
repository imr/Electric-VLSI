/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemAdjacent.java
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
 * JemAdjacent returns a list of the leaf JemEquivRecords adjacent to the
 * input list or record
 */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.trees.*;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

public class JemAdjacent extends JemStrat {
	private int numEquivProcessed;
	private int numAdjacentUnique;
	private Set adjacent = new HashSet();

	private JemAdjacent(NccGlobals globals) {super(globals);}
	
	public static JemLeafList doYourJob(JemRecordList l, 
									     NccGlobals globals) {
		JemAdjacent ja = new JemAdjacent(globals);
		return ja.doFor(l);
	}
	
	//do something before starting
	private void preamble(JemRecordList j){
//		startTime("JemAdjacent", "a list of " + j.size());
	}
	
    //summarize at the end
    private JemLeafList summary(){
		JemLeafList offspring = new JemLeafList();
		for (Iterator it=adjacent.iterator(); it.hasNext();) {
			offspring.add((JemEquivRecord)it.next());
		}

//		globals.println("JemAdjacent done - processed " +
//							numEquivProcessed + " JemEquivRecords");

		globals.println(" JemAdjacent"+offspringStats(offspring));

//		elapsedTime();
		return offspring.selectActive(globals);
    }
	
	// ---------- for JemRecordList -------------
	
    public JemLeafList doFor(JemRecordList g){
		preamble(g);
		super.doFor(g);
        return summary();
    }
	
	// ---------- for JemEquivRecord -------------
	
	/** 
	 * add JemEquivRecords that are adjacent to the contents of er. A
	 * JemEquivRecord is adjacent if it can be reached in one step
	 * from any of er's circuits and it's live and not retired nor
	 * mismatched
	 * @return Set of adjacent JemEquivRecords
	 */
	private void addAdjacentEquivRecs(JemEquivRecord er){
		for (Iterator ci=er.getCircuits(); ci.hasNext();) {
			JemCircuit jc= (JemCircuit) ci.next();
			for (Iterator ni=jc.getNetObjs(); ni.hasNext();) {
				NetObject netObj= (NetObject)ni.next();
				for (Iterator it=netObj.getConnected(); it.hasNext();) {
					//for each adjacent NetObject
					netObj = (NetObject)it.next();
					JemEquivRecord sg= netObj.getParent().getParent();
					if(sg.isActive()) adjacent.add(sg);
				}
			}
		}
	}
	
	/** The real output of this method is what it adds to adjacent */
    public JemLeafList doFor(JemEquivRecord g){
		if(g.isLeaf()){
			numEquivProcessed++;
			addAdjacentEquivRecs(g);
//			String s= ("processed " + g.nameString());
//			globals.println(s + " to find " + out.size() + " adjacent");
		} else {
			globals.println("processing " + g.nameString());
			super.doFor(g);
		}
		return new JemLeafList(); // return value is ignored
	}
	
}

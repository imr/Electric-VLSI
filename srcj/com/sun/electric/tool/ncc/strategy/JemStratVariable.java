/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratVariable.java
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
 * JemManager keeps lists of active JemEquivRecords.  It knows what
 * the strategies do and puts newly-created JemEquivRecord on the
 * right lists.
 */
package com.sun.electric.tool.ncc.strategy;

import com.sun.electric.tool.ncc.*;
import com.sun.electric.tool.ncc.NccEngine;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.trees.JemEquivRecord;
import com.sun.electric.tool.ncc.trees.JemCircuit;
import com.sun.electric.tool.ncc.trees.NetObject;
import com.sun.electric.tool.ncc.lists.JemRecordList;
import com.sun.electric.tool.ncc.lists.JemLeafList;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Wire;

import java.util.Iterator;

public class JemStratVariable {
	NccGlobals globals;

	private void error(boolean pred, String msg) {globals.error(msg);}

	private JemLeafList hashFrontierParts(){
		if (globals.getParts()==null)  return new JemLeafList();
		globals.println("----- hash all Parts on frontier");
		JemLeafList frontier = JemStratFrontier.doYourJob(globals.getParts(), globals);
		JemLeafList offspring = JemHashParts.doYourJob(frontier, globals);
		return offspring;
	}
	
	private JemLeafList hashFrontierWires(){
		if (globals.getParts()==null)  return new JemLeafList();
		globals.println("----- hash all Wires on frontier");
		JemLeafList frontier = JemStratFrontier.doYourJob(globals.getWires(), globals);
		JemLeafList offspring = JemHashWires.doYourJob(frontier, globals);
		return offspring;
	}
	
	private boolean isPartsList(JemLeafList el) {
		JemEquivRecord er = (JemEquivRecord) el.get(0);
		return er.getNetObjType()==NetObject.Type.PART;
	}

	/**
	 * Takes a list of newly divided Part/Wire JemEquivRecords. If any
	 * of them retire then find the retirees' neighbors and perform a
	 * hash step on them to get newly divided Wire/Part
	 * JemEquivRecords. Repeat until the hash step yields no newly
	 * divided JemEquivRecords.
	 * @param newDivided newly divided JemEquivRecords
	 */
	private void chaseRetired(JemLeafList newDivided) {
		// if nothing divided then suppress chaseRetired messages
		if (newDivided.size()==0) return;
		
		globals.println("------ starting chaseRetired");
		int i=0;
		while (newDivided.size()!=0) {
			globals.println("------ chaseRetired pass: " + i++);
			JemLeafList newRetired = newDivided.selectRetired(globals);
			if (newRetired.size()==0) break;
			JemLeafList adjacent = JemAdjacent.doYourJob(newRetired, globals);
			if (adjacent.size()==0)  break;
			boolean doParts = isPartsList(adjacent);
			newDivided = doParts ? JemHashParts.doYourJob(adjacent, globals) :
								   JemHashWires.doYourJob(adjacent, globals);
		}
		globals.println("------ done  chaseRetired after "+i+" passes");
		//		JemStratCheck.doYourJob(myJemSets.starter);
		globals.println();
	}
	
//	/**
//	 * Takes a list of newly divided Part/Wire JemEquivRecords. Finds
//	 * their neighbors and performs a hash step on them to get newly
//	 * divided Wire/Part JemEquivRecords. Repeat until the hash step
//	 * yields no newly divided JemEquivRecords.
//	 * @param newDivided newly divided JemEquivRecords
//	 */
//	private void chaseDivided(JemLeafList newDivided) {
//		// if nothing divided then suppress chaseDivided messages
//		if (newDivided.size()==0) return;
//
//		globals.println("------ starting chaseDivided");
//		int i=0;
//		while (newDivided.size()!=0) {
//			globals.println("------ chaseDivided pass: " + i++);
//			JemLeafList adjacent = JemAdjacent.doYourJob(newDivided, globals);
//			if (adjacent.size()==0)  break;
//			boolean doParts = isPartsList(adjacent);
//			newDivided = doParts ? JemHashParts.doYourJob(adjacent, globals) :
//								   JemHashWires.doYourJob(adjacent, globals);
//		}
//		globals.println("------ done  chaseDivided after "+i+" passes");
//		//		JemStratCheck.doYourJob(myJemSets.starter);
//		globals.println();
//	}
	
	private void hashFrontier() {
		globals.println("----- hash all NetObjects on frontier");
		while (true) {
			JemLeafList partOffspring = hashFrontierParts();
			chaseRetired(partOffspring);
		
			JemLeafList wireOffspring = hashFrontierWires();
			chaseRetired(wireOffspring);
			if (partOffspring.size()==0 && wireOffspring.size()==0) break;
		}
	}
	private boolean done() {
		JemLeafList p = JemStratFrontier.doYourJob(globals.getParts(),
		                                           globals);
		JemLeafList w = JemStratFrontier.doYourJob(globals.getWires(),
												   globals);
		return p.size()==0 && w.size()==0;
	}
	
	private void useExportNames() {
		globals.println("----- use Export Names");
		while (true) {
			JemStratCount.doYourJob(globals.getRoot(), globals);
			JemLeafList offspring = JemWireName.doYourJob(globals);
			if (offspring.size()==0) break;
			chaseRetired(offspring);
			hashFrontier();
		}
	}
	
	private JemEquivRecord findSmallestActive(JemEquivRecord root) {
		JemLeafList frontier = JemStratFrontier.doYourJob(root, globals);
		int minSz = Integer.MAX_VALUE;
		JemEquivRecord minRec = null;
		for (Iterator ri=frontier.iterator(); ri.hasNext();) {
			JemEquivRecord r = (JemEquivRecord) ri.next();
			if (r.isMismatched())  continue;
			int sz  = r.maxSize();
			if (sz<minSz) {
				minSz = sz;
				minRec = r;
			}
		}
		return minRec;
	}
	
	private JemEquivRecord findSmallestActive() {
		JemEquivRecord w = findSmallestActive(globals.getWires());
		JemEquivRecord p = findSmallestActive(globals.getParts());
		if (p==null) return w;
		if (p==null) return p;
		return p.maxSize()<w.maxSize() ? p : w; 
	}
	
	private void randomMatch() {
		globals.println("----- random matching");
		while (true) {
			JemEquivRecord smallest = findSmallestActive();
			if (smallest==null) return; 
			JemLeafList el = new JemLeafList();
			el.add(smallest);
			JemLeafList offspring = JemStratRandomMatch.doYourJob(el, globals);
			if (offspring.size()!=0) chaseRetired(offspring);
		}
	}
	
	// contructor does all the work
	private JemStratVariable(NccGlobals globals){
		this.globals = globals;

		globals.println("----- starting JemStratVariable");

		if (!done()) hashFrontier();
		if (!done()) useExportNames();
		if (!done()) randomMatch();
		
		globals.println("----- done JemStratVariable");
	}

	// ------------------------ public methods --------------------------------
	public static boolean doYourJob(NccGlobals globals) {
		new JemStratVariable(globals);
		return JemStratResult.doYourJob(globals);
	}
}

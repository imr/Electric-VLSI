/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HashCodePartitioning.java
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

import com.sun.electric.tool.ncc.*;
import com.sun.electric.tool.ncc.NccEngine;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.strategy.StratAdjacent;
import com.sun.electric.tool.ncc.strategy.StratCount;
import com.sun.electric.tool.ncc.strategy.StratFrontier;
import com.sun.electric.tool.ncc.strategy.StratHashParts;
import com.sun.electric.tool.ncc.strategy.StratHashWires;
import com.sun.electric.tool.ncc.strategy.StratPortName;
import com.sun.electric.tool.ncc.strategy.StratRandomMatch;
import com.sun.electric.tool.ncc.strategy.StratResult;
import com.sun.electric.tool.ncc.trees.EquivRecord;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.NetObject;
import com.sun.electric.tool.ncc.lists.RecordList;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Wire;

import java.util.Iterator;

public class HashCodePartitioning {
	NccGlobals globals;

	private void error(boolean pred, String msg) {globals.error(msg);}

	private LeafList hashFrontierParts(){
		if (globals.getParts()==null)  return new LeafList();
		globals.println("----- hash all Parts on frontier");
		LeafList frontier = StratFrontier.doYourJob(globals.getParts(), globals);
		LeafList offspring = StratHashParts.doYourJob(frontier, globals);
		return offspring;
	}
	
	private LeafList hashFrontierWires(){
		if (globals.getParts()==null)  return new LeafList();
		globals.println("----- hash all Wires on frontier");
		LeafList frontier = StratFrontier.doYourJob(globals.getWires(), globals);
		LeafList offspring = StratHashWires.doYourJob(frontier, globals);
		return offspring;
	}
	
	private boolean isPartsList(LeafList el) {
		EquivRecord er = (EquivRecord) el.get(0);
		return er.getNetObjType()==NetObject.Type.PART;
	}

	/**
	 * Takes a list of newly divided Part/Wire EquivRecords. If any
	 * of them retire then find the retirees' neighbors and perform a
	 * hash step on them to get newly divided Wire/Part
	 * EquivRecords. Repeat until the hash step yields no newly
	 * divided EquivRecords.
	 * @param newDivided newly divided EquivRecords
	 */
	private void chaseRetired(LeafList newDivided) {
		// if nothing divided then suppress chaseRetired messages
		if (newDivided.size()==0) return;
		
		globals.println("------ starting chaseRetired");
		int i=0;
		while (newDivided.size()!=0) {
			globals.println("------ chaseRetired pass: " + i++);
			LeafList newRetired = newDivided.selectRetired(globals);
			if (newRetired.size()==0) break;
			LeafList adjacent = StratAdjacent.doYourJob(newRetired, globals);
			if (adjacent.size()==0)  break;
			boolean doParts = isPartsList(adjacent);
			newDivided = doParts ? StratHashParts.doYourJob(adjacent, globals) :
								   StratHashWires.doYourJob(adjacent, globals);
		}
		globals.println("------ done  chaseRetired after "+i+" passes");
		//		StratCheck.doYourJob(globals.getRoot, globals);
		globals.println();
	}
	
//	/**
//	 * Takes a list of newly divided Part/Wire EquivRecords. Finds
//	 * their neighbors and performs a hash step on them to get newly
//	 * divided Wire/Part EquivRecords. Repeat until the hash step
//	 * yields no newly divided EquivRecords.
//	 * @param newDivided newly divided EquivRecords
//	 */
//	private void chaseDivided(LeafList newDivided) {
//		// if nothing divided then suppress chaseDivided messages
//		if (newDivided.size()==0) return;
//
//		globals.println("------ starting chaseDivided");
//		int i=0;
//		while (newDivided.size()!=0) {
//			globals.println("------ chaseDivided pass: " + i++);
//			LeafList adjacent = StratAdjacent.doYourJob(newDivided, globals);
//			if (adjacent.size()==0)  break;
//			boolean doParts = isPartsList(adjacent);
//			newDivided = doParts ? StratHashParts.doYourJob(adjacent, globals) :
//								   StratHashWires.doYourJob(adjacent, globals);
//		}
//		globals.println("------ done  chaseDivided after "+i+" passes");
//		//		StratCheck.doYourJob(globals.getRoot(), globals);
//		globals.println();
//	}
	
	private void hashFrontier() {
		globals.println("----- hash all NetObjects on frontier");
		while (true) {
			LeafList partOffspring = hashFrontierParts();
			chaseRetired(partOffspring);
		
			LeafList wireOffspring = hashFrontierWires();
			chaseRetired(wireOffspring);
			if (partOffspring.size()==0 && wireOffspring.size()==0) break;
		}
	}
	private boolean done() {
		LeafList p = StratFrontier.doYourJob(globals.getParts(),
		                                           globals);
		LeafList w = StratFrontier.doYourJob(globals.getWires(),
												   globals);
		return p.size()==0 && w.size()==0;
	}
	
	private void useExportNames() {
		globals.println("----- use Export Names");
		while (true) {
			StratCount.doYourJob(globals.getRoot(), globals);
			LeafList offspring = StratPortName.doYourJob(globals);
			if (offspring.size()==0) break;
			chaseRetired(offspring);
			hashFrontier();
		}
	}
	
	private EquivRecord findSmallestActive(EquivRecord root) {
		LeafList frontier = StratFrontier.doYourJob(root, globals);
		int minSz = Integer.MAX_VALUE;
		EquivRecord minRec = null;
		for (Iterator ri=frontier.iterator(); ri.hasNext();) {
			EquivRecord r = (EquivRecord) ri.next();
			if (r.isMismatched())  continue;
			int sz  = r.maxSize();
			if (sz<minSz) {
				minSz = sz;
				minRec = r;
			}
		}
		return minRec;
	}
	
	private EquivRecord findSmallestActive() {
		EquivRecord w = findSmallestActive(globals.getWires());
		EquivRecord p = findSmallestActive(globals.getParts());
		if (p==null) return w;
		if (w==null) return p;
		return p.maxSize()<w.maxSize() ? p : w; 
	}
	
	private void randomMatch() {
		globals.println("----- random matching");
		while (true) {
			EquivRecord smallest = findSmallestActive();
			if (smallest==null) return; 
			LeafList el = new LeafList();
			el.add(smallest);
			LeafList offspring = StratRandomMatch.doYourJob(el, globals);
			if (offspring.size()!=0) chaseRetired(offspring);
		}
	}
	
	// contructor does all the work
	private HashCodePartitioning(NccGlobals globals){
		this.globals = globals;

		globals.println("----- starting HashCodePartitioning");

		if (!done()) hashFrontier();
		if (!done()) useExportNames();
		NccOptions options = globals.getOptions();
		StratCount.doYourJob(globals.getRoot(), globals);
		if (!done()) randomMatch();
		
		globals.println("----- done HashCodePartitioning");
	}

	// ------------------------ public methods --------------------------------
	public static boolean doYourJob(NccGlobals globals) {
		new HashCodePartitioning(globals);
		return StratResult.doYourJob(globals);
	}
}

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
//package com.sun.electric.tool.ncc.processing;
//
//import java.util.Date;
//import java.util.Iterator;
//
//import com.sun.electric.tool.ncc.NccGlobals;
//import com.sun.electric.tool.ncc.basic.NccUtils;
//import com.sun.electric.tool.ncc.lists.LeafList;
//import com.sun.electric.tool.ncc.netlist.NetObject;
//import com.sun.electric.tool.ncc.strategy.StratAdjacent;
//import com.sun.electric.tool.ncc.strategy.StratCount;
//import com.sun.electric.tool.ncc.strategy.StratHashParts;
//import com.sun.electric.tool.ncc.strategy.StratHashWires;
//import com.sun.electric.tool.ncc.strategy.StratPortName;
//import com.sun.electric.tool.ncc.strategy.StratRandomMatch;
//import com.sun.electric.tool.ncc.strategy.StratSizes;
//import com.sun.electric.tool.ncc.trees.EquivRecord;
//
//public class HashCodePartitioning {
//	NccGlobals globals;
//	
//	private LeafList hashFrontierParts(){
//		if (globals.getParts()==null)  return new LeafList();
//		globals.status2("----- hash all Parts on frontier");
////		LeafList frontier = StratFrontier.doYourJob(globals.getParts(), globals);
//		Iterator<EquivRecord> frontier = globals.getPartLeafEquivRecs().getNotMatched();
//		LeafList offspring = StratHashParts.doYourJob(frontier, globals);
//		return offspring;
//	}
//	
//	private LeafList hashFrontierWires(){
//		if (globals.getParts()==null)  return new LeafList();
//		globals.status2("----- hash all Wires on frontier");
////		LeafList frontier = StratFrontier.doYourJob(globals.getWires(), globals);
//		Iterator<EquivRecord> frontier = globals.getWireLeafEquivRecs().getNotMatched();
//		LeafList offspring = StratHashWires.doYourJob(frontier, globals);
//		return offspring;
//	}
//	
//	private boolean isPartsList(LeafList el) {
//		EquivRecord er = (EquivRecord) el.get(0);
//		return er.getNetObjType()==NetObject.Type.PART;
//	}
//
//	/** Takes a list of newly divided Part/Wire EquivRecords. If any
//	 * of them match then find the match's neighbors and perform a
//	 * hash step on them to get newly divided Wire/Part
//	 * EquivRecords. Repeat until the hash step yields no newly
//	 * divided EquivRecords.
//	 * @param newDivided newly divided EquivRecords */
//	private void chaseMatched(LeafList newDivided) {
//		// if nothing divided then suppress chaseMatched messages
//		if (newDivided.size()==0) return;
//		
//		globals.status2("------ starting chaseMatched");
//		int i=0;
//		while (newDivided.size()!=0) {
//			globals.status2("------ chaseMatched pass: " + i++);
//			LeafList newMatches = newDivided.selectMatched(globals);
//			if (newMatches.size()==0) break;
//			LeafList adjacent = StratAdjacent.doYourJob(newMatches, globals);
//			if (adjacent.size()==0)  break;
//			boolean doParts = isPartsList(adjacent);
//			newDivided = 
//				doParts ? StratHashParts.doYourJob(adjacent.iterator(), globals) :
//					      StratHashWires.doYourJob(adjacent.iterator(), globals);
//		}
//		globals.status2("------ done  chaseMatched after "+i+" passes");
//		//		StratCheck.doYourJob(globals.getRoot, globals);
//		globals.status2("");
//	}
//	
////	/**
////	 * Takes a list of newly divided Part/Wire EquivRecords. Finds
////	 * their neighbors and performs a hash step on them to get newly
////	 * divided Wire/Part EquivRecords. Repeat until the hash step
////	 * yields no newly divided EquivRecords.
////	 * @param newDivided newly divided EquivRecords
////	 */
////	private void chaseDivided(LeafList newDivided) {
////		// if nothing divided then suppress chaseDivided messages
////		if (newDivided.size()==0) return;
////
////		globals.println("------ starting chaseDivided");
////		int i=0;
////		while (newDivided.size()!=0) {
////			globals.println("------ chaseDivided pass: " + i++);
////			LeafList adjacent = StratAdjacent.doYourJob(newDivided, globals);
////			if (adjacent.size()==0)  break;
////			boolean doParts = isPartsList(adjacent);
////			newDivided = doParts ? StratHashParts.doYourJob(adjacent, globals) :
////								   StratHashWires.doYourJob(adjacent, globals);
////		}
////		globals.println("------ done  chaseDivided after "+i+" passes");
////		//		StratCheck.doYourJob(globals.getRoot(), globals);
////		globals.println();
////	}
//	
//	private void hashFrontier() {
//		globals.status2("----- hash all NetObjects on frontier");
//		while (true) {
//			LeafList partOffspring = hashFrontierParts();
//			chaseMatched(partOffspring);
//		
//			LeafList wireOffspring = hashFrontierWires();
//			chaseMatched(wireOffspring);
//			if (partOffspring.size()==0 && wireOffspring.size()==0) break;
//		}
//	}
//	private boolean done() {
////		LeafList p = StratFrontier.doYourJob(globals.getParts(),
////		                                           globals);
////		LeafList w = StratFrontier.doYourJob(globals.getWires(),
////												   globals);
//		return globals.getWireLeafEquivRecs().numNotMatched()==0 &&
//			   globals.getPartLeafEquivRecs().numNotMatched()==0;
//	}
//	
//	private void useExportNames() {
//		globals.status2("----- use Export Names");
//		while (true) {
//			StratCount.doYourJob(globals);
//			LeafList offspring = StratPortName.doYourJob(globals);
//			if (offspring.size()==0) break;
//			chaseMatched(offspring);
//			hashFrontier();
//		}
//	}
//	private void useTransistorSizes() {
//		globals.status2("----- use transistor sizes");
//		while (true) {
//			LeafList offspring = StratSizes.doYourJob(globals);
//			if (offspring.size()==0) break;
//			chaseMatched(offspring);
//			hashFrontier();
//		}
//	}
//	
//	private void randomMatch() {
//		globals.status2("----- random matching");
//		while (true) {
//			LeafList offspring = StratRandomMatch.doYourJob(globals);
//			if (offspring.size()==0) break; 
//			chaseMatched(offspring);
//			hashFrontier();
//		}
//	}
//	
//	private void doWork() {
//		if (done()) return;
//		Date d1 = new Date();
//		
//		hashFrontier();
//		Date d2 = new Date();
//		globals.status1("  Hashing frontier took: "+NccUtils.hourMinSec(d1, d2));
//
//		if (done() || globals.userWantsToAbort()) return;
//		useExportNames();
//		Date d3 = new Date();
//		globals.status1("  Using export names took: "+NccUtils.hourMinSec(d2, d3));
//
//		if (done() || globals.userWantsToAbort()) return;
//		useTransistorSizes();
//		Date d4 = new Date();
//		globals.status1("  Using transistor sizes took: "+NccUtils.hourMinSec(d3, d4));
//
//		if (done() || globals.userWantsToAbort()) return;
//		randomMatch();
//		Date d5 = new Date();
//		globals.status1("  Random match took: "+NccUtils.hourMinSec(d4, d5));
//	}
//	
//	// contructor does all the work
//	private HashCodePartitioning(NccGlobals globals){
//		this.globals = globals;
//		globals.status2("----- starting HashCodePartitioning");
//		doWork();
//		globals.status2("----- done HashCodePartitioning");
//	}
//	
//	private boolean allPartsWiresMatch() {
//		return globals.getPartLeafEquivRecs().numNotMatched()==0 &&
//		       globals.getWireLeafEquivRecs().numNotMatched()==0;
//	}
//
//	// ------------------------ public methods --------------------------------
//	public static boolean doYourJob(NccGlobals globals) {
//		HashCodePartitioning p = new HashCodePartitioning(globals);
//		return p.allPartsWiresMatch();
//	}
//}

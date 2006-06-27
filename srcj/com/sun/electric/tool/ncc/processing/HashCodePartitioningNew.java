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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.strategy.StratCount;
import com.sun.electric.tool.ncc.strategy.StratHashParts;
import com.sun.electric.tool.ncc.strategy.StratHashWires;
import com.sun.electric.tool.ncc.strategy.StratPortName;
import com.sun.electric.tool.ncc.strategy.StratRandomMatch;
import com.sun.electric.tool.ncc.strategy.StratSizes;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

public class HashCodePartitioningNew {
	NccGlobals globals;
	
	/* The NetObjects contained by newly born EquivRecords
	 * have new hash codes. Therefore it's only necessary to 
	 * check for the consequences of these new hash codes
	 * in the EquivRecords containing NEIGHBORS of the NetObjects
	 * with new hash codes.
	 * 
	 * I will do this using three queues. The first queue
	 * contains newly divided EquivRecords that are matched. The second
	 * queue contains newly divided EquivRecords that are not matched
	 * but not mismatched. The third queue contains newly divided
	 * EquivRecords that are mismatched.
	 * 
	 * The first queue has the highest priority while the third queue
	 * has the lowest priority. From the three queues I select
	 * the highest priority new born
	 * EquivRecord, X. I find all the neighbors of the 
	 * NetObjects inside X. I recompute the hash codes  of the
	 * EquivRecords containing those neighbors. 
	 * 
	 * When all three queues are empty I have fully explored
	 * the consequences of the original EquivRecord partition.
	 * In particular, I no longer need to rehash everything on
	 * the frontier.	 */
	private static class HashCodePropagator {
		NccGlobals globals;
		
		// maximum number of EquivRecords for which we will save birthdays
		private final int MAX_REC_BIRTHDAYS = 1000;
		
		public static class ChildAndBirthday {
			EquivRecord child;
			int birthday;
			ChildAndBirthday(EquivRecord child, int birthday) {
				this.child = child;
				this.birthday = birthday;
			}
		}
		private int todaysDate = 1;
		private Map<EquivRecord, Integer> recToRehashDate = 
			new HashMap<EquivRecord, Integer>();

		private LinkedList<ChildAndBirthday> matchedNewBorns = new LinkedList<ChildAndBirthday>();
		private LinkedList<ChildAndBirthday> activeNewBorns = new  LinkedList<ChildAndBirthday>();
		private LinkedList<ChildAndBirthday> mismatchedNewBorns = new  LinkedList<ChildAndBirthday>();

		private ChildAndBirthday selectHighPriorityNewBorn2() {
			if (!matchedNewBorns.isEmpty()) {
				return matchedNewBorns.removeFirst();
			} else if (!activeNewBorns.isEmpty()) {
				return activeNewBorns.removeFirst();
			} else if (!mismatchedNewBorns.isEmpty()) {
				return mismatchedNewBorns.removeFirst();
			} else {
				return null;
			}
		}
		private ChildAndBirthday selectHighPriorityNewBorn() {
			// Discard EquivRecords that have been partitioned since the time 
			// they were put on the lists.
			while (true) {
				ChildAndBirthday cf = selectHighPriorityNewBorn2();
				if (cf==null) return null;
				if (cf.child.isLeaf()) return cf;
			}
		}
		// Presume that these newborns were born today
		private void addAll(LeafList newBorns) {
			// Increment todays date twice so we never rehash on a birthday.
			// This prevents the confusing case: rehash day == birthday.
			todaysDate++;
			if (recToRehashDate.size()>MAX_REC_BIRTHDAYS) {
				// bound hash table size by periodically clearing hash table
				System.out.println("  NCC: Reached MAX_REC_BIRTHDAYS: "+
						           MAX_REC_BIRTHDAYS);
				recToRehashDate.clear();
			}
			for (Iterator<EquivRecord> erIt=newBorns.iterator(); erIt.hasNext();) {
				EquivRecord er = erIt.next();
				LayoutLib.error(er==null, "null not allowed");
				if (er.isMatched()) {
					matchedNewBorns.add(new ChildAndBirthday(er, todaysDate));
				} else if (er.isMismatched()) {
					mismatchedNewBorns.add(new ChildAndBirthday(er, todaysDate));
				} else {
					// must be active
					activeNewBorns.add(new ChildAndBirthday(er, todaysDate));
				}
			}
			todaysDate++;
		}
		// Find child's neighbors that haven't been rehashed since child 
		// was born.
		private List<EquivRecord> findStaleAdjacentTo(ChildAndBirthday cb) {
			int childsBirthday = cb.birthday;
			EquivRecord child = cb.child;
			
			List<EquivRecord> adjacent = new ArrayList<EquivRecord>();
			for (Iterator<Circuit> ci=child.getCircuits(); ci.hasNext();) {
				Circuit jc= ci.next();
				for (Iterator<NetObject> ni=jc.getNetObjs(); ni.hasNext();) {
					NetObject netObj= ni.next();
					for (Iterator<NetObject> it=netObj.getConnected(); it.hasNext();) {
						//for each adjacent NetObject
						EquivRecord neighbor = it.next().getParent().getParent();
						if(neighbor.isActive()) {
							Integer rehashDate = recToRehashDate.get(neighbor);
							if (rehashDate==null || rehashDate<childsBirthday) {
								adjacent.add(neighbor);
								recToRehashDate.put(neighbor, todaysDate);
							}
						} else {
							// remove entries from recToRehashDate whenever possible
							recToRehashDate.remove(neighbor);
						}
					}
				}
			}
			return adjacent;
		}
		
		private List<EquivRecord> findStaleAdjacentToHighestPriorityNewBorn() {
			while (true) {
				ChildAndBirthday cb = selectHighPriorityNewBorn();
				if (cb==null) return new ArrayList<EquivRecord>();
				
				List<EquivRecord> adjacent = findStaleAdjacentTo(cb);
				if (!adjacent.isEmpty()) return adjacent;
			}
		}
		
		public HashCodePropagator(NccGlobals glob) {globals=glob;}
		
		public void propagateFromNewBorns(LeafList newBornList) {
			addAll(newBornList);

			while (true) {
				List<EquivRecord> adjacent = findStaleAdjacentToHighestPriorityNewBorn();
				if (adjacent.isEmpty()) break;
				
				for (EquivRecord er : adjacent) {
					if (!er.isLeaf()) continue;
					if (er.getNetObjType()==NetObject.Type.PART) {
						addAll(StratHashParts.doYourJob(er, globals));
					} else {
						addAll(StratHashWires.doYourJob(er, globals));
					}
				}
			}
		}
	}

	private void hashFrontierParts(HashCodePropagator hashProp) {
		if (globals.getParts()==null)  return;
		globals.status2("----- hash all Parts on frontier");
		for (Iterator<EquivRecord> eIt=globals.getPartLeafEquivRecs().getNotMatched(); eIt.hasNext();) {
			EquivRecord er = eIt.next();
			if (!er.isLeaf()) continue;
			if (er.isMismatched()) continue;
			// must be active
			
			LeafList newBornList = StratHashParts.doYourJob(er, globals);
			hashProp.propagateFromNewBorns(newBornList);
		}
	}
	
	private void hashFrontierWires(HashCodePropagator hashProp){
		if (globals.getWires()==null)  return;
		globals.status2("----- hash all Wires on frontier");
		for (Iterator<EquivRecord> eIt=globals.getWireLeafEquivRecs().getNotMatched(); eIt.hasNext();) {
			EquivRecord er = eIt.next();
			if (!er.isLeaf()) continue;
			if (er.isMismatched()) continue;
			// must be active
			
			LeafList newBornList = StratHashWires.doYourJob(er, globals);
			hashProp.propagateFromNewBorns(newBornList);
		}
	}
	
	private void hashFrontier(HashCodePropagator hashProp) {
		globals.status2("----- hash all NetObjects on frontier");
		hashFrontierParts(hashProp);
		hashFrontierWires(hashProp);
	}
	
	private boolean done() {
		return globals.getWireLeafEquivRecs().numNotMatched()==0 &&
			   globals.getPartLeafEquivRecs().numNotMatched()==0;
	}
	
	private void useExportNames(HashCodePropagator hashProp) {
		globals.status2("----- use Export Names");
		while (true) {
			//StratCount.doYourJob(globals);
			LeafList offspring = StratPortName.doYourJob(globals);
			if (offspring.size()==0) break;
			hashProp.propagateFromNewBorns(offspring);
		}
	}
	private void useTransistorSizes(HashCodePropagator hashProp) {
		globals.status2("----- use transistor sizes");
		while (true) {
			LeafList offspring = StratSizes.doYourJob(globals);
			if (offspring.size()==0) break;
			hashProp.propagateFromNewBorns(offspring);
		}
	}
	
	private void randomMatch(HashCodePropagator hashProp) {
		globals.status2("----- random matching");
		while (true) {
			LeafList offspring = StratRandomMatch.doYourJob(globals);
			if (offspring.size()==0) break; 
			hashProp.propagateFromNewBorns(offspring);
		}
	}
	
	private void doWork() {
		if (done()) return;
		Date d1 = new Date();

		HashCodePropagator hashProp = new HashCodePropagator(globals);
		hashFrontier(hashProp);
		Date d2 = new Date();
		globals.status1("  Hashing frontier took: "+NccUtils.hourMinSec(d1, d2));

		if (done() || globals.userWantsToAbort()) return;
		useExportNames(hashProp);
		Date d3 = new Date();
		globals.status1("  Using export names took: "+NccUtils.hourMinSec(d2, d3));

		if (done() || globals.userWantsToAbort()) return;
		useTransistorSizes(hashProp);
		Date d4 = new Date();
		globals.status1("  Using transistor sizes took: "+NccUtils.hourMinSec(d3, d4));

		if (done() || globals.userWantsToAbort()) return;
		randomMatch(hashProp);
		Date d5 = new Date();
		globals.status1("  Random match took: "+NccUtils.hourMinSec(d4, d5));
	}
	
	private boolean allPartsWiresMatch() {
		return globals.getPartLeafEquivRecs().numNotMatched()==0 &&
		       globals.getWireLeafEquivRecs().numNotMatched()==0;
	}

	// contructor does all the work
	private HashCodePartitioningNew(NccGlobals globals){
		this.globals = globals;
		globals.status2("----- starting HashCodePartitioningNew");
		doWork();
		globals.status2("----- done HashCodePartitioningNew");
	}
	
	// ------------------------ public methods --------------------------------
	public static boolean doYourJob(NccGlobals globals) {
		HashCodePartitioningNew p = new HashCodePartitioningNew(globals);
		return p.allPartsWiresMatch();
	}
}

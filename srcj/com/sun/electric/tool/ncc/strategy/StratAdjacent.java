/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratAdjacent.java
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
//package com.sun.electric.tool.ncc.strategy;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.Set;
//
//import com.sun.electric.tool.ncc.NccGlobals;
//import com.sun.electric.tool.ncc.lists.LeafList;
//import com.sun.electric.tool.ncc.lists.RecordList;
//import com.sun.electric.tool.ncc.netlist.NetObject;
//import com.sun.electric.tool.ncc.trees.Circuit;
//import com.sun.electric.tool.ncc.trees.EquivRecord;
//
///** StratAdjacent returns a list of the leaf EquivRecords adjacent to the
// * input list or record */
//public class StratAdjacent extends Strategy {
//	private int numEquivProcessed;
//	private Set<EquivRecord> adjacent = new HashSet<EquivRecord>();
//
//	private StratAdjacent(NccGlobals globals) {super(globals);}
//	
//	public static LeafList doYourJob(RecordList l, 
//									     NccGlobals globals) {
//		StratAdjacent ja = new StratAdjacent(globals);
//		return ja.doFor(l);
//	}
//	
//	//do something before starting
//	private void preamble(RecordList j){
////		startTime("StratAdjacent", "a list of " + j.size());
//	}
//	
//    //summarize at the end
//    private LeafList summary(){
//		LeafList offspring = new LeafList();
//		for (EquivRecord er : adjacent) {
//			offspring.add(er);
//		}
//
////		globals.println("StratAdjacent done - processed " +
////							numEquivProcessed + " EquivRecords");
//
//		globals.status2(" StratAdjacent"+offspringStats(offspring));
//
////		elapsedTime();
//		return offspring.selectActive(globals);
//    }
//	
//    public LeafList doFor(RecordList g){
//		preamble(g);
//		super.doFor(g);
//        return summary();
//    }
//	
//	/** add EquivRecords that are adjacent to the contents of er. A
//	 * EquivRecord is adjacent if it can be reached in one step
//	 * from any of er's circuits and it's live and not matched nor
//	 * mismatched
//	 * @return Set of adjacent EquivRecords */
//	private void addAdjacentEquivRecs(EquivRecord er){
//		for (Iterator<Circuit> ci=er.getCircuits(); ci.hasNext();) {
//			Circuit jc= ci.next();
//			for (Iterator<NetObject> ni=jc.getNetObjs(); ni.hasNext();) {
//				NetObject netObj= ni.next();
//				for (Iterator<NetObject> it=netObj.getConnected(); it.hasNext();) {
//					//for each adjacent NetObject
//					netObj = it.next();
//					EquivRecord sg = netObj.getParent().getParent();
//					if(sg.isActive()) adjacent.add(sg);
//				}
//			}
//		}
//	}
//	
//	/** The real output of this method is what it adds to adjacent */
//    public LeafList doFor(EquivRecord g){
//		if(g.isLeaf()) {
//			numEquivProcessed++;
//			addAdjacentEquivRecs(g);
////			String s= ("processed " + g.nameString());
////			globals.println(s + " to find " + out.size() + " adjacent");
//		} else {
//			globals.status2("processing " + g.nameString());
//			super.doFor(g);
//		}
//		return new LeafList(); // return value is ignored
//	}
//	
//}

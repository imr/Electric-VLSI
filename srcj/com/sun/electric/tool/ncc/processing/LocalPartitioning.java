/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LocalPartitioning.java
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
import java.util.List;
import java.util.Set;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.netlist.Wire;
import com.sun.electric.tool.ncc.strategy.StratHashParts;
import com.sun.electric.tool.ncc.strategy.StratHashWires;
import com.sun.electric.tool.ncc.strategy.StratPartPopularity;
import com.sun.electric.tool.ncc.strategy.StratPartType;
import com.sun.electric.tool.ncc.strategy.Strategy;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/** LocalPartitioning partitions the Part and Wire equivalence classes based
 * on purely local characteristics. In principle, these partitions are
 * redundant with the normal hash code process. However it seems useful
 * to perform these first because:
 * 1) There is no hash code computation. Therefore errors in one part of
 * the circuit do not cause mismatches in other parts of the circuit.
 * 2) They can be easily explained to the user. */
public class LocalPartitioning {
	private static class GetLeaves extends Strategy {
		List<EquivRecord> matched = new ArrayList<EquivRecord>();
		List<EquivRecord> notMatched = new ArrayList<EquivRecord>();
		GetLeaves(EquivRecord er, NccGlobals glob) {
			super(glob);
			if (er!=null)  doFor(er);
		}
		@Override
		public LeafList doFor(EquivRecord er) {
			if (er.isLeaf()) {
				if (er.isMatched()) matched.add(er);
				else notMatched.add(er);
			} else {
				return super.doFor(er);
			}
			return new LeafList();
		}
		public List<EquivRecord> getMatched() {return matched;}
		public List<EquivRecord> getNotMatched() {return notMatched;}
	}
	
    NccGlobals globals;
    /** return a Set of all the types of Pins we might encounter */
	private void partitionPartsUsingLocalInformation(Set<Part> forcedMatchParts) {
		globals.status2("Partition Parts using local information");
		if (globals.getParts()==null) return;
		LeafList offspring = StratPartType.doYourJob(forcedMatchParts, globals);
		if (offspring.size()!=0) {
			//StratCheck.doYourJob(globals.getRoot(), globals);
			//StratCount.doYourJob(globals.getRoot(), globals);
		}
		
		offspring = StratPartPopularity.doYourJob(forcedMatchParts, globals);
		if (offspring.size()!=0) {
			//StratCheck.doYourJob(globals.getRoot(), globals);
			//StratCount.doYourJob(globals.getRoot(), globals);
		}
	}

    private LocalPartitioning(NccGlobals globals) {this.globals = globals;}
    
    private void localPartitionPartsAndWires(Set<Part> forcedMatchParts, 
    		                                 Set<Wire> forcedMatchWires) {
        globals.status2("Begin partitioning based on local characteristics \n");

		partitionPartsUsingLocalInformation(forcedMatchParts);
		NewLocalPartitionWires.doYourJob(forcedMatchWires, globals);

		/* Count EquivRecords after Local Partitioning */
		/*
		int numMatchedPartRecs = globals.getPartLeafEquivRecs().numMatched();
		int numNotMatchedPartRecs = globals.getPartLeafEquivRecs().numNotMatched(); 
		int numMatchedWireRecs = globals.getWireLeafEquivRecs().numMatched();
		int numNotMatchedWireRecs = globals.getWireLeafEquivRecs().numNotMatched(); 
		System.out.println("Local Partitioning Results: #matchedPartRecs="+numMatchedPartRecs+
				           " #notMatchedPartRecs="+numNotMatchedPartRecs+
						   " #matchedWireRecs="+numMatchedWireRecs+
						   " #notMatchedWireRecs="+numNotMatchedWireRecs);
		*/		
		//StratReportLocalPartitionFailure.doYourJob(globals);

		globals.status2("End partitioning based on local characteristics ");
    }
    
    /** Subtle!!!: If user has forced the match of Parts or Wires then
     * we MUST check, via hashing, all Parts and Wires that were matched 
     * by Local Partitioning. Otherwise we may fail to report mismatches. 
     * Report Parts and Wires matched by LocalPartitioning but subsequently 
     * mismatched by hashing as "hash code" errors. */
    private LocalPartitionResult 
    	hashMatchedPartsWires(Set<Part> forcedMatchParts, 
                              Set<Wire> forcedMatchWires) {
    	GetLeaves partLeaves = new GetLeaves(globals.getParts(), globals);
    	GetLeaves wireLeaves = new GetLeaves(globals.getWires(), globals);
    	LocalPartitionResult lpr = 
    		new LocalPartitionResult(partLeaves.getNotMatched(), 
    				                 wireLeaves.getNotMatched(), globals);

    	List<EquivRecord> partsToCheck = new ArrayList<EquivRecord>();
    	for (EquivRecord er : partLeaves.getMatched()) {
    		NetObject no = er.getCircuits().next().getNetObjs().next();
    		if (!forcedMatchParts.contains(no))  partsToCheck.add(er);
    	}

    	List<EquivRecord> wiresToCheck = new ArrayList<EquivRecord>();
    	for (EquivRecord er : wireLeaves.getMatched()) {
    		NetObject no = er.getCircuits().next().getNetObjs().next();
    		if (!forcedMatchWires.contains(no))  wiresToCheck.add(er);
    	}

    	StratHashParts.doYourJob(partsToCheck.iterator(), globals);
    	StratHashWires.doYourJob(wiresToCheck.iterator(), globals);
    	return lpr;
    }
    
	
	// ------------------------ public method ---------------------------------
	public static LocalPartitionResult doYourJob(Set<Part> forcedMatchParts,
			                     		         Set<Wire> forcedMatchWires, 
			                                     NccGlobals globals) {
		LocalPartitioning jsl = new LocalPartitioning(globals);
		jsl.localPartitionPartsAndWires(forcedMatchParts, forcedMatchWires);
		return jsl.hashMatchedPartsWires(forcedMatchParts, forcedMatchWires);
	}
}

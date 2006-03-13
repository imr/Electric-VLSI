/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MergeSerialParallel.java
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.netlist.Mos;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.netlist.Wire;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/**
 * Merge parallel Transistors into one.  Merge stacks of Transistors into
 * one stacked Transistor. <p>
 * 
 * Tricky: The process of merging requires the deletion of Parts and 
 * Wires. My first attempt to handle deletion efficiently was to use Sets to
 * hold Parts and Wires. However, this was bad because it used too much 
 * storage. My new approach is to mark Parts and Wires "deleted" and later
 * in a single pass remove "deleted" objects from lists after all merging
 * is complete. Note that "deleted" Parts and Wires should never exist before
 * or after serial parallel merge.<p>
 * 
 * More Tricky:
 * This phase gets executed before Wires get put in cannonical form. That 
 * means we must be prepared for a Part to occur more than once on a Wire's
 * parts list. We must also be prepared to enounter "deleted" parts. One 
 * implication is that I can't check the number of parts on a wire by 
 * simply calling Wire.getNumParts() because I need to add extra code to
 * exclude deleted and duplicate Parts! 
 */
public class SerialParallelMerge {
    private NccGlobals globals;

    private SerialParallelMerge(NccGlobals globals) {this.globals=globals;}

	private boolean serialMerge() {
		int numMerged = 0;
		EquivRecord wires = globals.getWires();
		if (wires!=null) {
			// don't blow up if no wires
			for (Iterator<Circuit> it=wires.getCircuits(); it.hasNext();) {
				Circuit ckt = it.next();
				for (Iterator<NetObject> ni=ckt.getNetObjs(); ni.hasNext();) {
					Wire w = (Wire) ni.next();
					if (Mos.joinOnWire(w)) numMerged++;
				}
			}
		}
		globals.status2("    Serial merged "+numMerged+" Transistors");

		return numMerged>0;
	}

	/** parallelMergeAllCandidatesInList attempts to parallel merge the Parts in
	 * parts. Because there is a possibility of hash code collisions,
	 * I examine all n^2 combinations to guarantee that all possible
	 * parallel merges are performed.
	 * @param parts Collection of Parts to merge in parallel
	 * @return the count of Parts actually merged */
	private int parallelMergeAllCandidatesInSet(Collection<Part> parts) {
		int numMerged = 0;
		// Linked list allows O(1) remove()
		LinkedList<Part> pts = new LinkedList<Part>(parts);
		while (true) {
			Iterator<Part> it= pts.iterator();
			if (!it.hasNext()) break;
			Part first= it.next();
			it.remove();			
			while (it.hasNext()) {
				Part p = it.next();
				if (first.parallelMerge(p)) {
					it.remove();
					numMerged++;
				} 
			}
		}
		return numMerged;
	}
	
	private int parallelMergeEachSetInMap(Map<Integer,Set<Part>> map) {
		int numMerged = 0;
		for(Integer i : map.keySet()){
			Set<Part> j= map.get(i);
			numMerged += parallelMergeAllCandidatesInSet(j);
		}
		return numMerged;
	}

    // process candidate transistors from one Wire
	// Tricky: Set removes duplicate Parts on Wire.parts list
    private int parallelMergePartsOnWire(Wire w) {
    	if (w.isDeleted()) return 0;
		HashMap<Integer,Set<Part>>  map = new HashMap<Integer,Set<Part>>();
		
		for (Iterator<Part> it=w.getParts(); it.hasNext();) {
			Part p = it.next();
			if (p.isDeleted()) continue;
			Integer code = p.hashCodeForParallelMerge();
			Set<Part> set = map.get(code);
			if (set==null) {
				set= new HashSet<Part>();
				map.put(code,set);
			}
			set.add(p);
		}
		return parallelMergeEachSetInMap(map);
    }

	private boolean parallelMerge() {
		int numMerged = 0;
		EquivRecord er = globals.getWires();
		if (er!=null) {
			// don't blow up if no wires
			for (Iterator<Circuit> it=er.getCircuits(); it.hasNext();) {
				Circuit ckt = it.next();
				for (Iterator<NetObject> ni=ckt.getNetObjs(); ni.hasNext();) {
					Wire w = (Wire)ni.next();
					numMerged += parallelMergePartsOnWire(w);
				}
			}
		}
		globals.status2("    Parallel merged " + numMerged + " Parts");
		return numMerged>0;
	}
	
	private int countUndeletedParts(EquivRecord parts) {
		int numParts = 0;
		for (Iterator<Circuit> it=parts.getCircuits(); it.hasNext();) {
			Circuit ckt = it.next();
			numParts += ckt.numUndeletedNetObjs();
		}
		return numParts;
	}

	// merge the various parts using series and parallel lists
	private void serialParallelMerge() {
		EquivRecord parts = globals.getParts();

		if (parts==null)  return; // No Cell has Parts

		int numParts = countUndeletedParts(parts);
		globals.status2("--- NCC starting merge process with " +
						numParts + " Parts");
		// Tricky: Don't give up if the first parallel merge attempt fails because 
		// the following first serial merge may succeed!
		boolean first = true;
		for (int tripNumber=1; ; tripNumber++) {
			if (globals.userWantsToAbort()) break; 
			globals.status2("  parallel and series merge trip " + tripNumber);
			boolean progress = parallelMerge();
			if (!first && !progress) break;
			first = false;

			if (globals.userWantsToAbort()) break; 
			progress = serialMerge();
			if (!progress) break;
		}
		numParts = countUndeletedParts(parts);
		globals.status2("--- NCC finishing merge process with " +
						numParts + " Parts");
		globals.status2("");
	}

	private static void putInFinalForm(EquivRecord er) {
		if (er==null) return;
		for (Iterator<Circuit> it=er.getCircuits(); it.hasNext();) {
			it.next().putInFinalForm();
		}
	}
	public static void doYourJob(NccGlobals globals){
		SerialParallelMerge sp = new SerialParallelMerge(globals);
		sp.serialParallelMerge();
		putInFinalForm(globals.getParts());
		putInFinalForm(globals.getWires());
	}
}

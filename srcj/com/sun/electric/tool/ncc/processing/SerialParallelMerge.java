/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MergeParallel.java
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
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.jemNets.Transistor;
import com.sun.electric.tool.ncc.strategy.Strategy;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * MergeParallel works by seeking a wire with many gates on it it then
 * sorts the transistors on that wire by hash of their names seeking
 * any that lie in the same hash bin it produces exactly one offspring
 * identical to its target
*/
public class SerialParallelMerge {
    private NccGlobals globals;

    private SerialParallelMerge(NccGlobals globals) {this.globals=globals;}

	private int processSerialMergeCandidates(List candidates) {
		int numMerged = 0;
		for (Iterator it=candidates.iterator(); it.hasNext();) {
			Wire w = (Wire) it.next();
			if (Transistor.joinOnWire(w)) numMerged++;
		}
		return numMerged;
	}
	
	private void findSerialMergeCandidates(List candidates, Wire w){
		// this a quick and dirty test for candidacy. 
		// Transistor.joinOnWire does the complete test.
		if (w.numParts()==2) candidates.add(w);
	}

	private boolean serialMerge() {
		ArrayList candidates = new ArrayList();
		
		EquivRecord wires = globals.getWires();
		for (Iterator it=wires.getCircuits(); it.hasNext();) {
			Circuit ckt = (Circuit) it.next();
			for (Iterator ni=ckt.getNetObjs(); ni.hasNext();) {
				Wire w = (Wire) ni.next();
				findSerialMergeCandidates(candidates, w);
			}
		}
		int numMerged = processSerialMergeCandidates(candidates);
		globals.println("    Serial merged "+numMerged+" Parts");

		return numMerged>0;
	}

	/** parallelMergeAllCandidatesInList attempts to parallel merge the Parts in
	 * parts. Because there is a possibility of hash code collisions,
	 * I examine all n^2 combinations to guarantee that all possible
	 * parallel merges are performed.
	 * @param parts Collection of Parts to merge in parallel
	 * @return the count of Parts actually merged */
	private int parallelMergeAllCandidatesInList(Collection parts) {
		int numMerged = 0;
		// Clone the list so we don't surprise the caller by changing it
		LinkedList pts = new LinkedList(parts);
		while (true) {
			Iterator it= pts.iterator();
			if (!it.hasNext()) break;
			Part first= (Part)it.next();
			it.remove();			
			while (it.hasNext()) {
				Part p = (Part)it.next();
				if (first.parallelMerge(p)) {
					it.remove();
					numMerged++;
				} 
			}
		}
		return numMerged;
	}
	
	private int parallelMergeEachListInMap(Map map) {
		int numMerged = 0;
		for(Iterator it=map.keySet().iterator(); it.hasNext();){
			ArrayList j= (ArrayList) map.get(it.next());
			numMerged += parallelMergeAllCandidatesInList(j);
		}
		return numMerged;
	}

    // process candidate transistors from one Wire
    private int parallelMergePartsOnWire(Wire w){
		HashMap  map = new HashMap();
		
		for (Iterator wi=w.getParts(); wi.hasNext();) {
			Part p = (Part)wi.next();
			Integer code = p.hashCodeForParallelMerge();
			ArrayList list = (ArrayList) map.get(code);
			if (list==null) {
				list= new ArrayList();
				map.put(code,list);
			}
			list.add(p);
		}
		return parallelMergeEachListInMap(map);
    }

	private boolean parallelMerge() {
		int numMerged = 0;
		EquivRecord er = globals.getWires();
		for (Iterator it=er.getCircuits(); it.hasNext();) {
			Circuit ckt = (Circuit) it.next();
			for (Iterator ni=ckt.getNetObjs(); ni.hasNext();) {
				Wire w = (Wire) ni.next();
				numMerged += parallelMergePartsOnWire(w);
			}
		}
		globals.println("    Parallel merged " + numMerged + " Parts");
		return numMerged>0;
	}
	
	private int countParts(EquivRecord parts) {
		int numParts = 0;
		for (Iterator it=parts.getCircuits(); it.hasNext();) {
			Circuit ckt = (Circuit) it.next();
			numParts += ckt.numNetObjs();
		}
		return numParts;
	}

	// merge the various parts using series and parallel lists
	private void serialParallelMerge() {
		EquivRecord parts = globals.getParts();

		if (parts==null)  return; // No Cell has Parts

		int numParts = countParts(parts);
		globals.println("--- NCC starting merge process with " +
						numParts + " Parts");
		// Tricky: Don't give up if the first parallel merge attempt fails because 
		// the following first serial merge may succeed!
		boolean first = true;
		for (int tripNumber=1; ; tripNumber++) {
			globals.println("  parallel and series merge trip " + tripNumber);
			boolean progress = parallelMerge();
			if (!first && !progress) break;
			first = false;
			progress = serialMerge();
			if (!progress) break;
		}
		numParts = countParts(parts);
		globals.println("--- NCC finishing merge process with " +
						numParts + " Parts");
		globals.println();
	}

	
	public static void doYourJob(NccGlobals globals){
		SerialParallelMerge sp = new SerialParallelMerge(globals);
		sp.serialParallelMerge();
	}
}

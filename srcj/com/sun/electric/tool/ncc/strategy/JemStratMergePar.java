/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratMergePar.java
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
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.*;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.jemNets.Transistor;
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
public class JemStratMergePar extends JemStrat {
    private int numMerged = 0;

    private JemStratMergePar(NccGlobals globals) {super(globals);}

	private boolean doYourJob2(NccGlobals globals){
		preamble(globals.getWires());
		doFor(globals.getWires());
		summary();
		return numMerged>0;
	}
	
    //do something before starting
    private void preamble(JemEquivRecord j){
		startTime("JemStratMergePar" , j.nameString());
    }

    //do at the end
    private void summary(){
		globals.println(" Parallel merged " + numMerged + " Parts");
		elapsedTime();
    }

    public Integer doFor(NetObject n){
		error(!(n instanceof Wire), "JemStratMergePar expects only Wires");
		return doWire((Wire)n);
    }

	/** processAllCandidatesInList attempts to parallel merge the Parts in
	 * parts. Because there is a possibility of hash code collisions,
	 * I examine all n^2 combinations to guarantee that all possible
	 * parallel merges are performed.
	 * @param parts Collection of Parts to merge in parallel
	 * @return the count of Parts actually merged */
	private void processAllCandidatesInList(Collection parts){
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
	}
	
	private void processEachListInMap(Map map) {
		for(Iterator it=map.keySet().iterator(); it.hasNext();){
			ArrayList j= (ArrayList) map.get(it.next());
			processAllCandidatesInList(j);
		}
	}

    // process candidate transistors from one Wire
    private Integer doWire(Wire w){
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
		processEachListInMap(map);
		return CODE_NO_CHANGE;
    }

	// ------------------ intended interface -----------------------
	public static boolean doYourJob(NccGlobals globals){
		JemStratMergePar jsmp = new JemStratMergePar(globals);
		return jsmp.doYourJob2(globals);
	}
}

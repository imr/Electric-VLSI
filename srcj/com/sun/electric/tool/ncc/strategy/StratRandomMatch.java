/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratRandomMatch.java
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

import java.util.Iterator;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/** StratRandomMatch finds balanced EquivRecords and arbitrarily matches the 
 * NetObjects that are first in each Circuit. */
public class StratRandomMatch extends Strategy {
	private static final Integer CODE_FIRST = new Integer(1);
	private static final Integer CODE_REST = new Integer(2);
    private StratRandomMatch(NccGlobals globals){super(globals);}

	private EquivRecord findSmallestBalanced(Iterator<EquivRecord> frontier) {
		//LeafList frontier = StratFrontier.doYourJob(root, globals);
		int minSz = Integer.MAX_VALUE;
		EquivRecord minRec = null;
		while (frontier.hasNext()) {
			EquivRecord r = (EquivRecord) frontier.next();
			if (!r.isBalanced())  continue;
			int sz  = r.maxSize();
			if (sz<minSz) {
				minSz = sz;
				minRec = r;
			}
		}
		return minRec;
	}
	
	private EquivRecord findSmallestActive() {
		EquivRecord w = 
			findSmallestBalanced(globals.getWireLeafEquivRecs().getNotMatched());
		EquivRecord p = 
			findSmallestBalanced(globals.getPartLeafEquivRecs().getNotMatched());
		if (p==null) return w;
		if (w==null) return p;
		return p.maxSize()<w.maxSize() ? p : w; 
	}

	private LeafList doYourJob() {
		EquivRecord smallest = findSmallestActive();
		if (smallest==null) return new LeafList();
		return doFor(smallest); 
	}
	
	public Integer doFor(NetObject n){
		Circuit ckt = n.getParent();
		Iterator<NetObject> ni = ckt.getNetObjs();
		Object first = ni.next();
		return n==first ? CODE_FIRST : CODE_REST;
	}


    //----------------------------- intended interface ------------------------
	/** Find the smallest active equivalence class. Partition it by matching
	 * the first NetObject in each Circuit.
	 * @return the offspring resulting from the partition */
	public static LeafList doYourJob(NccGlobals globals){
		StratRandomMatch rm = new StratRandomMatch(globals);
		return rm.doYourJob();
	}
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Leaves.java
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
package com.sun.electric.tool.ncc.trees;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.strategy.Strategy;
/** Object to keep track of the leaves of the EquivRecord tree.
 * <p>
 * Profiling has demonstrated that it is too expensive to repeatedly locate 
 * the leaves of the EquivRecord tree by recursive descent from the root. A 
 * flat NCC of qFourP2 (no size checking) spends fully 80% of its time doing 
 * just this. Therefore I'm building a data structure to keep track of the 
 * leaves. Because this data structure updates itself incrementally it never 
 * has to scan the tree. 
 * <p>
 * A separate list of matched EquivRecords is kept. Most records will be 
 * matched. Most of the time we're interested in active records, not matched.
 * Separating out the matched records speeds up the scan for active records. */
public class LeafEquivRecords {
    private static final LeafList EMPTY_LIST = new LeafList();

    // ----------------------------- private data -----------------------------
	private NccGlobals globals;
	private List matched = new ArrayList();
	// Contains mismatched and active EquivRecords.
	// This needs to be a linked list because we delete elements from the
	// middle of the list.
	private LinkedList unmatched = new LinkedList();

	private static class FindLeaves extends Strategy {
		private List matched;
		private List unmatched;
		public LeafList doFor(EquivRecord j){
			if (j.isLeaf()) {
				EquivRecord er = (EquivRecord)j;
				// add to the front of notMatched since it's useless to 
				// encounter and process it again.
				if (er.isMatched()) matched.add(er); else unmatched.add(er);
			} else {
				super.doFor(j);
			}
			return EMPTY_LIST;
		}

		// ------------------- intended interface ------------------------
		public FindLeaves(List newMatched, List newUnmatched, EquivRecord er, 
				          NccGlobals globals) {
			super(globals);
			matched = newMatched;
			unmatched = newUnmatched;
			doFor(er);
		}
	}
	
	// Normally, unmatched should contain only leaf EquivRecords. However
	// partitioning might turn a leaf into an internal node. When that happens
	// we need to remove that internal node and find the descendents that are
	// leaves and add them to the appropriate lists.
	private void processInternalEquivRecords() {
		List newMatched = new ArrayList();
		List newUnmatched = new ArrayList();
		for (ListIterator it=unmatched.listIterator(); it.hasNext();) {
			EquivRecord er = (EquivRecord) it.next();
			if (er.isLeaf()) {
				LayoutLib.error(er.isMatched(), "unmatched list has matched");
			} else {
				// a leaf EquivRecord was partitioned and therefore isn't a 
				// leaf anymore.  Find the descendents of this node that are
				// leaves.
				it.remove();
				new FindLeaves(newMatched, newUnmatched, er, globals);
			}
		}
		matched.addAll(newMatched);
		unmatched.addAll(newUnmatched);
	}
	// ----------------------------- public methods ---------------------------
	public LeafEquivRecords(EquivRecord root, NccGlobals globals) {
		this.globals = globals;
		if (root==null) return; // sometimes there are no parts or no wires.
		if (root.isLeaf() && root.isMatched()) {
			matched.add(root);
		} else {
			unmatched.add(root);
		}
	}

	/** @return all active and mismatched leaf EquivRecords */
	public Iterator getUnmatched() {
		processInternalEquivRecords();
		return Collections.unmodifiableList(unmatched).iterator();
	}
	public int numUnmatched() {return unmatched.size();}
	/** @return all matched leaf EquivRecords */
	public Iterator getMatched() {
		processInternalEquivRecords();
		return Collections.unmodifiableList(matched).iterator();
	}
	public int numMatched() {return matched.size();}
}

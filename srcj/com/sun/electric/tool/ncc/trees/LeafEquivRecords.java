/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LeafEquivRecords.java
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
import java.util.Collection;
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
 * matched. Most of the time we're interested in records not matched.
 * Separating out the matched records speeds up the scan for active records. 
 * <p>
 * Tricky: LeafEquivRecords takes advantage of the fact that the Gemini hash 
 * code algorithm only subdivides "notMatched" EquivRecords. 
 * This allows me to keep a separate list for the "notMatched" and scan only
 * that list to see which EquivRecords have been subdivided. What's tricky
 * is that the series-parallel reduction can change an EquivRecord from
 * notMatched to matched. Also Local Partitioning can change subdivide
 * a matched EquivRecord. Therefore I must initialize the LeafEquivRecords
 * object <i>after</i> series-parallel reduction and Local Partitioning. 
 * */
public class LeafEquivRecords {
    private static final LeafList EMPTY_LIST = new LeafList();

    // ----------------------------- private data -----------------------------
	private NccGlobals globals;
	private List<EquivRecord> matched = new ArrayList<EquivRecord>();
	// Contains all EquivRecords that haven't been matched
	// This needs to be a linked list because we delete elements from the
	// middle of the list.
	private LinkedList<EquivRecord> notMatched = new LinkedList<EquivRecord>();

	private static class FindLeaves extends Strategy {
		private List<EquivRecord> matched;
		private List<EquivRecord> notMatched;
		public LeafList doFor(EquivRecord j){
			if (j.isLeaf()) {
				EquivRecord er = (EquivRecord)j;
				// add to the front of notMatched since it's useless to 
				// encounter and process it again.
				if (er.isMatched()) matched.add(er); else notMatched.add(er);
			} else {
				super.doFor(j);
			}
			return EMPTY_LIST;
		}

		// ------------------- intended interface ------------------------
		public FindLeaves(List<EquivRecord> newMatched, List<EquivRecord> newNotMatched, EquivRecord er, 
				          NccGlobals globals) {
			super(globals);
			matched = newMatched;
			notMatched = newNotMatched;
			doFor(er);
		}
	}
	
	// Normally, notMatched should contain only leaf EquivRecords. However
	// partitioning might turn a leaf into an internal node. When that happens
	// we need to remove that internal node and find the descendents that are
	// leaves and add them to the appropriate lists.
	private void processInternalEquivRecords() {
		List<EquivRecord> newMatched = new ArrayList<EquivRecord>();
		List<EquivRecord> newNotMatched = new ArrayList<EquivRecord>();
		for (ListIterator<EquivRecord> it=notMatched.listIterator(); it.hasNext();) {
			EquivRecord er = it.next();
			if (er.isLeaf()) {
				LayoutLib.error(er.isMatched(), "notMatched list has matched");
			} else {
				// a leaf EquivRecord was partitioned and therefore isn't a 
				// leaf anymore.  Find the descendents of this node that are
				// leaves.
				it.remove();
				new FindLeaves(newMatched, newNotMatched, er, globals);
			}
		}
		matched.addAll(newMatched);
		notMatched.addAll(newNotMatched);
	}

	// ----------------------------- public methods ---------------------------
	public LeafEquivRecords(EquivRecord root, NccGlobals globals) {
		this.globals = globals;
		if (root==null) return; // sometimes there are no parts or no wires.
		if (root.isLeaf() && root.isMatched()) {
			matched.add(root);
		} else {
			notMatched.add(root);
		}
	}

	/** @return all leaf EquivRecords that haven't been matched */
	public Iterator<EquivRecord> getNotMatched() {
		processInternalEquivRecords();
		return Collections.unmodifiableList(notMatched).iterator();
	}
	public int numNotMatched() {
		processInternalEquivRecords();
		return notMatched.size();
	}
	/** @return all matched leaf EquivRecords */
	public Iterator<EquivRecord> getMatched() {
		processInternalEquivRecords();
		return Collections.unmodifiableList(matched).iterator();
	}
	public int numMatched() {
		processInternalEquivRecords();
		return matched.size();
	}
}

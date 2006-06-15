/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EquivRecReport.java
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
package com.sun.electric.tool.ncc.result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.netlist.Wire;
import com.sun.electric.tool.ncc.strategy.Strategy;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/** Unlike EquivRecord, EquivRecReport can only have Parts or Wires, never
 * Ports. */
public class EquivRecReport implements Serializable {
	static final long serialVersionUID = 0;

	// -------------------------- utility class -------------------------------
	// Return reports for all matched and unmatched netobjects belonging to this
	// or descendents of this EquivRecord
	private static class GetNetObjs extends Strategy {
		private int numDesigns;
		private NetObject.Type  type = null;
		private final List<List<NetObjReport>> matches; 
		private final List<List<NetObjReport>> notMatches; 
		
		private void checkType(NetObject n) {
			NetObject.Type t = n.getNetObjType();
			if (type==null) type=t;
			LayoutLib.error(type!=t, "different types in same EquivRecord");
			LayoutLib.error(type!=NetObject.Type.PART &&
					        type!=NetObject.Type.WIRE,
					        "expecting only Parts or Wires");
		}
		
		private void appendNetObjsFromCircuit(List<List<NetObjReport>> lists, 
				                              EquivRecord er) {
			int i=0;
			for (Iterator<Circuit> itC=er.getCircuits(); itC.hasNext(); i++) {
				Circuit ckt = (Circuit) itC.next();
				for (Iterator<NetObject> itN=ckt.getNetObjs(); itN.hasNext();) {
					NetObject n = itN.next();
					checkType(n);
					NetObjReport nor = (n instanceof Part) ? 
							new PartReport((Part)n) : new WireReport((Wire)n);
					
					lists.get(i).add(nor);
				}
			}
			LayoutLib.error(i!=numDesigns, "wrong number of circuits");
		}
		
		public LeafList doFor(EquivRecord er) {
			if (er.isLeaf()) {
				appendNetObjsFromCircuit(er.isMatched() ? matches : notMatches, er);
				return new LeafList();
			} else {
				return super.doFor(er);
			}
		}
		
		// ------------------------- intended interface -----------------------
		public GetNetObjs(EquivRecord er, NccGlobals globals) {
			super(globals);
			numDesigns = globals.getNumNetlistsBeingCompared();
			matches = new ArrayList<List<NetObjReport>>();
			notMatches = new ArrayList<List<NetObjReport>>();
			for (int i=0; i<numDesigns; i++) {
				matches.add(new ArrayList<NetObjReport>());
				notMatches.add(new ArrayList<NetObjReport>());
			}
			doFor(er);
		}
		public List<List<NetObjReport>> getMatchedNetObjs() {
			return matches;
		}
		public List<List<NetObjReport>> getNotMatchedNetObjs() {
			return notMatches;
		}
		public boolean hasParts() {return type==NetObject.Type.PART;}
	}
	
	// ------------------------------- data -----------------------------------
	// A ResEquivRec is either due to a local partitioning mismatch
	// or a hash code mismatch.
	private final boolean hashMismatch;
	private final boolean hasParts;
	
	// matched exists only for bad local partition records. matched
	// holds those ResNetObjs belonging to bad local partition
	// records that were matched by hash coding.
	private final List<List<NetObjReport>> matched, notMatched; 
	private final List<String> reasons;
	
	public EquivRecReport(EquivRecord er, boolean hashMismatch, 
			              NccGlobals globals) {
		this.hashMismatch = hashMismatch;
		reasons = er.getPartitionReasonsFromRootToMe();
		GetNetObjs gno = new GetNetObjs(er, globals);
		matched = gno.getMatchedNetObjs();
		notMatched = gno.getNotMatchedNetObjs();
		hasParts = gno.hasParts();
	}
	public int maxSize() {
		return Math.max(matched.get(0).size() + notMatched.get(0).size(),
			            matched.get(1).size() + notMatched.get(1).size());
	}
	public boolean hashMismatch() {return hashMismatch;}
	public List<String> getReasons() {return reasons;}
	public boolean hasParts() {return hasParts;}
	/** Only bad local partition EquivRecords have matched net objects. */
	public List<List<NetObjReport>> getMatchedNetObjs() {return matched;}
	public List<List<NetObjReport>> getNotMatchedNetObjs() {return notMatched;}
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ReportHashCodeFailure.java
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
import java.util.Iterator;
import java.util.List;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.EquivRecReport;
import com.sun.electric.tool.ncc.result.NetObjReport;
import com.sun.electric.tool.ncc.strategy.StratCount;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/** Try to print useful information to explain to the user why
 * the Hash Code Partitioning phase failed. */
public class ReportHashCodeFailure {
	private NccGlobals globals;
	private List<EquivRecReport> badParts = new ArrayList<EquivRecReport>();
	private List<EquivRecReport> badWires = new ArrayList<EquivRecReport>();
	
	private static void pr(String s) {System.out.print(s);}
	private static void prln(String s) {System.out.println(s);}

//	private static class GetNotMatched extends Strategy {
//		private final List<EquivRecReport> notMatches;
//		
//		// ---------- the tree walking code ---------
//		public LeafList doFor(EquivRecord er) {
//			if (er.isLeaf()) {
//				EquivRecReport err = new EquivRecReport(er, er.isMismatched(), globals);
//				if (!er.isMatched()) notMatches.add(err);
//			} else {
//				super.doFor(er);
//			}
//			return new LeafList();
//		}
//    
//		// Constructor does it all
//		private GetNotMatched(List<EquivRecReport> notMatched, 
//				                   EquivRecord er, NccGlobals globals) {
//			super(globals);
//			notMatches = notMatched;
//			doFor(er);
//		}
//		
//		// ----------------------- intended interface -------------------------
//		public static List<EquivRecReport> doIt(EquivRecord er, 
//				                                         NccGlobals globals) {
//			List<EquivRecReport> notMatched = new ArrayList<EquivRecReport>();
//			new GetNotMatched(notMatched, er, globals);
//			return notMatched;
//		}
//	}

	private void printNotMatched(List<EquivRecReport> notMatched) {
		if (notMatched.size()==0)  return;
		String type = notMatched.get(0).hasParts() ? "Part" : "Wire";
		int num = notMatched.size();
		prln("  "+num+" not-matched "+type+" equivalence classes:");
		int maxNotMatches = globals.getOptions().maxMismatchedEquivRecsToPrint;
		if (num>maxNotMatches) {
			prln("    Too many, I'm only printing "+maxNotMatches+".");
		}
		int numToPrint = Math.min(num, maxNotMatches);
		for (int rptNdx=0; rptNdx<numToPrint; rptNdx++) {
			prln("    not-matched "+type+" equivalence class "+rptNdx);
			EquivRecReport rpt = notMatched.get(rptNdx);
			List<List<NetObjReport>> netObjs = rpt.getNotMatchedNetObjs();
			for (int desNdx=0; desNdx<netObjs.size(); desNdx++) {
				List<NetObjReport> noD = netObjs.get(desNdx);
				prln("      Cell "+globals.getRootCellNames()[desNdx]+
						 " has "+noD.size()+" mismatched objects");
				for (NetObjReport no : noD) {
					prln("        "+no.fullDescription());
				}
			}
		}
	}
	
	// force StratCount to print
	private void printCounts() {
		NccOptions options = globals.getOptions();
		int saveHowMuchStatus = options.howMuchStatus;
		options.howMuchStatus = 10;
		StratCount.doYourJob(globals);
		options.howMuchStatus = saveHowMuchStatus;
	}

	// ------------------------ intended interface ----------------------------
	/** constructor does it all */
	public ReportHashCodeFailure(NccGlobals globals) {
		this.globals = globals;
		prln("Hash Code Partitioning Failed!!!");
		printCounts();
		for (Iterator<EquivRecord> 
		     it=globals.getPartLeafEquivRecs().getNotMatched(); it.hasNext();)
			badParts.add(new EquivRecReport(it.next(), true));
		for (Iterator<EquivRecord> 
		     it=globals.getWireLeafEquivRecs().getNotMatched(); it.hasNext();)
			badWires.add(new EquivRecReport(it.next(), true));
		
//		badParts = GetNotMatched.doIt(globals.getParts(), globals);
//		badWires = GetNotMatched.doIt(globals.getWires(), globals);
		printNotMatched(badParts);
		printNotMatched(badWires);
	}

	public List<EquivRecReport> getWireRecReports() {return badWires;}
	public List<EquivRecReport> getPartRecReports() {return badParts;}
}


//private static class StratPrintNotMatched extends Strategy {
//private int cktNdx;
//private int classNum;
//private String netObjDescr;
//private int maxNotMatches;
//private void printEquivRecs(String type, EquivRecord partsOrWires, 
//		                    int num) {
//	classNum = 1;
//	netObjDescr = type;
//	prln("  "+num+" not-matched "+type+" equivalence classes:");
//	maxNotMatches = globals.getOptions().maxMismatchedEquivRecsToPrint;
//	if (num>maxNotMatches) {
//		prln("    Too many, I'm only printing "+maxNotMatches+".");
//	}
//	if (partsOrWires==null) {
//		// The designs being compared have no Parts or no Wires.
//		// There's nothing to print.  Do a quick sanity check and 
//		// return.
//		error(num!=0, "non-zero mismatched objects but no objects!");
//		return;
//	}
//	doFor(partsOrWires);
//}
//// Constructor does everything
//private StratPrintNotMatched(StratCount.Counts counts, 
//		                     NccGlobals globals) {
//	super(globals);
//	printEquivRecs("Part", globals.getParts(), 
//			       counts.numNotMatchedPartEquivRecs());
//	printEquivRecs("Wire", globals.getWires(), 
//			       counts.numNotMatchedWireEquivRecs());
//}
//
//// ---------- the tree walking code ---------
//public LeafList doFor(EquivRecord er) {
//	if (classNum > maxNotMatches) {
//		// we've already printed too much. Do nothing.
//	} else if (er.isLeaf()) {
//		if (!er.isMatched()) {
//			prln("    not-matched "+netObjDescr+" equivalence class "+classNum++);
//			cktNdx = 0;
//			super.doFor(er);
//		}
//	} else {
//		super.doFor(er);
//	}
//	return new LeafList();
//}
//
//public HashMap<Integer,List<NetObject>> doFor(Circuit c) {
//	prln("      Cell "+globals.getRootCellNames()[cktNdx]+
//		 " has "+c.numNetObjs()+" mismatched objects");
//	cktNdx++;
//	return super.doFor(c);
//}
//
//public Integer doFor(NetObject n){
//	prln("        "+n.fullDescription());
//	return CODE_NO_CHANGE;
//}
//
//// ----------------------- intended interface -------------------------
//public static void doYourJob(StratCount.Counts counts, NccGlobals globals) {
//	new StratPrintNotMatched(counts, globals);
//}
//}


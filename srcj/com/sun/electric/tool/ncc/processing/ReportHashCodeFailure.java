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

import java.util.HashMap;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.jemNets.NetObject;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.strategy.StratCount;
import com.sun.electric.tool.ncc.strategy.Strategy;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/** Try to print useful information to explain to the user why
 * the Hash Code Partitioning phase failed. */
public class ReportHashCodeFailure {
	private NccGlobals globals;
	
	private static void pr(String s) {System.out.print(s);}
	private static void prln(String s) {System.out.println(s);}

	private static class StratPrintMismatched extends Strategy {
		private int cktNdx;
		private int classNum;
		private String netObjDescr;
		private int maxMismatches;
		private void printEquivRecs(String type, EquivRecord partsOrWires, 
				                    int num) {
			classNum = 1;
			netObjDescr = "Part";
			prln("  "+num+" mismatched "+type+" equivalence classes:");
			maxMismatches = globals.getOptions().maxMismatchedEquivRecsToPrint;
			if (num>maxMismatches) {
				prln("    Too many, I'm only printing "+maxMismatches+".");
			}
			if (partsOrWires==null) {
				// The designs being compared have no Parts or no Wires.
				// There's nothing to print.  Do a quick sanity check and 
				// return.
				error(num!=0, "non-zero mismatched objects but no objects!");
				return;
			}
			doFor(partsOrWires);
		}
		// Constructor does everything
		private StratPrintMismatched(StratCount.Counts counts, 
				                     NccGlobals globals) {
			super(globals);
			printEquivRecs("Part", globals.getParts(), 
					       counts.numMismatchedPartEquivRecs);
			printEquivRecs("Wire", globals.getWires(), 
					       counts.numMismatchedWireEquivRecs);
		}
	
		// ---------- the tree walking code ---------
		public LeafList doFor(EquivRecord er) {
			if (classNum > maxMismatches) {
				// we've already printed too much. Do nothing.
			} else if (er.isLeaf()) {
				if (er.isMismatched()) {
					prln("    Mismatched "+netObjDescr+" equivalence class "+classNum++);
					cktNdx = 0;
					super.doFor(er);
				}
			} else {
				super.doFor(er);
			}
			return new LeafList();
		}
	
		public HashMap doFor(Circuit c) {
			prln("      Cell "+globals.getRootCellNames()[cktNdx]+
				 " has "+c.numNetObjs()+" mismatched objects");
			cktNdx++;
			return super.doFor(c);
		}

		public Integer doFor(NetObject n){
			prln("        "+n.fullDescription());
			return CODE_NO_CHANGE;
		}
    
		// ----------------------- intended interface -------------------------
		public static void doYourJob(StratCount.Counts counts, NccGlobals globals) {
			new StratPrintMismatched(counts, globals);
		}
	}

	private static class StratPrintMatched extends Strategy {
		private int classNum=1;
		private String netObjDescr;
		private int maxMatches;
		
		private void printEquivRecs(String type, EquivRecord partsOrWires, 
				                    int num) {
			classNum = 1;
			netObjDescr = "Part";
			prln("  "+num+" matched "+type+" equivalence classes:");
			maxMatches = globals.getOptions().maxMatchedEquivRecsToPrint;
			if (num>maxMatches) {
				prln("    Too many, I'm only printing "+maxMatches+".");
			}
			if (partsOrWires==null) {
				// The designs being compared have no Parts or no Wires.
				// There's nothing to print.  Do a quick sanity check and 
				// return.
				error(num!=0, "non-zero matched objects but no objects!");
				return;
			}
			
			doFor(partsOrWires);
		}
		// Constructor does everything
		private StratPrintMatched(StratCount.Counts counts, NccGlobals globals) {
			super(globals);
			printEquivRecs("Part", globals.getParts(), 
					       counts.numMatchedPartEquivRecs);
			printEquivRecs("Wire", globals.getWires(), 
					       counts.numMatchedWireEquivRecs);
		}
	
		public LeafList doFor(EquivRecord er) {
			if (classNum > maxMatches) {
				// do nothing because we've already printed too much
			} else if (er.isLeaf()) {
				if (er.isMatched()) {
					pr("    "+netObjDescr+" match "+(classNum++)+" between: ");
					int numDesigns = globals.getNumNetlistsBeingCompared();
					for (int i=0; i<numDesigns; i++) {
						if (i!=0) pr(" and ");
						pr(globals.getRootCellNames()[i]);
					}
					prln("");
					super.doFor(er);
				}
			} else {
				super.doFor(er);
			}
			return new LeafList();
		}
	
		public HashMap doFor(Circuit c) {return super.doFor(c);}

		public Integer doFor(NetObject n) {
			prln("      "+n.fullDescription());
			return CODE_NO_CHANGE;
		}
    
		// ------------------------ intended interface ------------------------
		public static void doYourJob(StratCount.Counts counts, NccGlobals globals) {
			new StratPrintMatched(counts, globals);
		}
	}
	// force StratCount to print
	private StratCount.Counts printCounts() {
		NccOptions options = globals.getOptions();
		int saveHowMuchStatus = options.howMuchStatus;
		options.howMuchStatus = 10;
		StratCount.Counts counts = StratCount.doYourJob(globals);
		options.howMuchStatus = saveHowMuchStatus;
		return counts;
	}
	/** constructor does it all */
	private ReportHashCodeFailure(NccGlobals globals) {
		this.globals = globals;
		prln("Hash Code Partitioning Failed!!!");
		StratCount.Counts counts = printCounts();
		StratPrintMismatched.doYourJob(counts, globals);
		StratPrintMatched.doYourJob(counts, globals);
	}

	public static void reportHashCodeFailure(NccGlobals globals) {
		new ReportHashCodeFailure(globals);
	}
}

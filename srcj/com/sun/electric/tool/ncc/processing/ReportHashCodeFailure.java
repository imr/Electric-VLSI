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

import com.sun.electric.tool.ncc.strategy.Strategy;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.trees.EquivRecord;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.NetObject;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Wire;

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
		// Constructor does everything
		private StratPrintMismatched(NccGlobals globals) {
			super(globals);
			int maxMismatched = globals.getOptions().numMismatchedEquivClassesToPrint;

			netObjDescr = "part";
			classNum = 1;
			prln("Dumping a maximum of "+maxMismatched+" mismatched part equivalence classes");
			doFor(globals.getParts());

			netObjDescr = "wire";
			classNum = 1;
			prln("Dumping a maximum of "+maxMismatched+" mismatched wire equivalence classes");
			doFor(globals.getWires());
		}
	
		// ---------- the tree walking code ---------
		public LeafList doFor(EquivRecord er) {
			if (classNum > globals.getOptions().numMismatchedEquivClassesToPrint) {
				// we've already printed too much. Do nothing.
			} else if (er.isLeaf()) {
				if (er.isMismatched()) {
					prln("  Mismatched "+netObjDescr+" equivalence class "+classNum++);
					cktNdx = 0;
					super.doFor(er);
				}
			} else {
				super.doFor(er);
			}
			return new LeafList();
		}
	
		public HashMap doFor(Circuit c) {
			prln("    Cell "+globals.getRootCellNames()[cktNdx]+" has mismatched objects");
			cktNdx++;
			return super.doFor(c);
		}

		public Integer doFor(NetObject n){
			prln("      "+n.toString());
			return CODE_NO_CHANGE;
		}
    
		// -------------------------- public method -------------------------------
		public static void doYourJob(NccGlobals globals) {
			new StratPrintMismatched(globals);
		}
	}

	private static class StratPrintMatched extends Strategy {
		private int classNum=1;
		private String netObjDescr;
		// Constructor does everything
		private StratPrintMatched(NccGlobals globals) {
			super(globals);
			int maxMatches = globals.getOptions().numMatchedEquivClassesToPrint;
			classNum = 1;
			netObjDescr = "Part";
			prln("Dumping a maximum of "+maxMatches+" matched part equivalence classes");
			doFor(globals.getParts());

			classNum = 1;
			netObjDescr = "Wire";
			prln("Dumping a maximum of "+maxMatches+" matched wire equivalence classes");
			doFor(globals.getWires());
		}
	
		// ---------- the tree walking code ---------
		public LeafList doFor(EquivRecord er) {
			if (classNum > globals.getOptions().numMatchedEquivClassesToPrint) {
				// do nothing because we've already printend too much
			} else if (er.isLeaf()) {
				if (er.isRetired()) {
					pr("  "+netObjDescr+" match "+(classNum++)+" between: ");
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
	
		public HashMap doFor(Circuit c) {
			return super.doFor(c);
		}

		public Integer doFor(NetObject n){
			prln("      "+n.toString());
			return CODE_NO_CHANGE;
		}
    
		// -------------------------- public method -------------------------------
		public static void doYourJob(NccGlobals globals) {
			new StratPrintMatched(globals);
		}
	}
	/** constructor does it all */
	private ReportHashCodeFailure(NccGlobals globals) {
		this.globals = globals;
		prln("Hash Code Partitioning Failed!!!");
		StratPrintMismatched.doYourJob(globals);
		StratPrintMatched.doYourJob(globals);
	}

	public static void reportHashCodeFailure(NccGlobals globals) {
		new ReportHashCodeFailure(globals);
	}
}

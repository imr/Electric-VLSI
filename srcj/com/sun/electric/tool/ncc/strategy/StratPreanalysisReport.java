/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratPreanalysisReport.java
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.jemNets.NetObject;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

public class StratPreanalysisReport extends Strategy {
	private static final NetObject.Type PART = NetObject.Type.PART; 
	private static final NetObject.Type PORT = NetObject.Type.PORT; 

	List mismatched = new ArrayList();

	private StratPreanalysisReport(NccGlobals globals) {super(globals);}

	private boolean doYourJob2(NccGlobals globals) {
		doFor(globals.getRoot());
		summary();
		return mismatched.size()==0;
	}
	
	private void prln(String s) {System.out.println(s);}
	
	private void printCircuitContents(Circuit ckt, int cktNdx, String t) {
		String cktName = globals.getRootCellNames()[cktNdx];
		int numNetObjs = ckt.numNetObjs();
		prln("      "+cktName+" has "+numNetObjs+" of these "+t+":");
		int maxPrint = globals.getOptions().maxEquivRecMembersToPrint;
		if (ckt.numNetObjs()>maxPrint) {
			prln("        Too many "+t+"! I'll only print the first "+maxPrint);
		}
		int numPrint = 0;
		for (Iterator it=ckt.getNetObjs(); it.hasNext(); numPrint++) {
			if (numPrint>maxPrint)  break;
			NetObject o = (NetObject) it.next();
			prln("        "+o.fullDescription());
		}
	}
	
	private void printMismatchedRecord(EquivRecord r) {
		String t = r.getNetObjType()==PART ? "parts" : "wires";
    			 
		prln("    The "+t+" in this equivalence class share the following characteristics:");
		List reasons = r.getPartitionReasonsFromRootToMe();
		for (Iterator it=reasons.iterator(); it.hasNext();) {
			prln("      "+it.next());
		}
		int cktNdx = 0;
		for (Iterator it=r.getCircuits(); it.hasNext(); cktNdx++) {
			Circuit ckt = (Circuit) it.next();
			printCircuitContents(ckt, cktNdx, t);
		}
	}

    private void summary() {
    	if (mismatched.size()!=0) 
    		prln("\n  Mismatches found during local partitioning:\n");
    		
    	for (Iterator it=mismatched.iterator(); it.hasNext();) {
    		EquivRecord r = (EquivRecord) it.next();
			printMismatchedRecord(r);			
    	}
    }

    public LeafList doFor(EquivRecord g){
		if (g.isLeaf()) {
			if (g.isMismatched() && g.getNetObjType()!=PORT)  mismatched.add(g);
			return new LeafList();
		} else {
			return super.doFor(g);
		}
	}

	// -------------------- intended interface ------------------------
	public static boolean doYourJob(NccGlobals globals) {
		StratPreanalysisReport wp = new StratPreanalysisReport(globals);
		return wp.doYourJob2(globals);
	}
}

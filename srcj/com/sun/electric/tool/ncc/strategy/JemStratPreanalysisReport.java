/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratPreanalysisReport.java
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
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class JemStratPreanalysisReport extends JemStrat {
	private static final NetObject.Type PART = NetObject.Type.PART; 
	private static final NetObject.Type PORT = NetObject.Type.PORT; 

	List mismatched = new ArrayList();

	private JemStratPreanalysisReport(NccGlobals globals) {super(globals);}

	private boolean doYourJob2(NccGlobals globals) {
		doFor(globals.getRoot());
		summary();
		return mismatched.size()==0;
	}
	
	private void prln(String s) {globals.println(s);}
	
	private void printCircuitContents(JemCircuit ckt, int cktNdx, String t) {
		String cktName = globals.getRootCellNames()[cktNdx];
		int numNetObjs = ckt.numNetObjs();
		prln("  In "+cktName+" "+numNetObjs+" "+t+
             " have these characteristics: ");
		for (Iterator it=ckt.getNetObjs(); it.hasNext();) {
			NetObject o = (NetObject) it.next();
			prln("    "+o.toString());
		}
	}
	
	private void printMismatchedRecord(JemEquivRecord r) {
		String t = r.getNetObjType()==PART ? "parts" : "wires";
    			 
		prln("The "+t+" in this set share the following characteristics:");
		for (JemEquivRecord i=r; i!=null; i=i.getParent()) {
			String reason = i.getPartitionReason();
			if (reason==null) continue;
			prln("    "+reason);
		}
		int cktNdx = 0;
		for (Iterator it=r.getCircuits(); it.hasNext(); cktNdx++) {
			JemCircuit ckt = (JemCircuit) it.next();
			printCircuitContents(ckt, cktNdx, t);
		}
	}

    private void summary() {
    	NccOptions options = globals.getOptions();
    	boolean savedVerbose = options.verbose;
    	options.verbose = true;
    	
    	if (mismatched.size()!=0) 
    		globals.println("\nMismatches found during local processing:\n");
    		
    	for (Iterator it=mismatched.iterator(); it.hasNext();) {
    		JemEquivRecord r = (JemEquivRecord) it.next();
			printMismatchedRecord(r);			
    	}
    	
    	options.verbose = savedVerbose;
    }

    public JemLeafList doFor(JemEquivRecord g){
		if (g.isLeaf()) {
			if (g.isMismatched() && g.getNetObjType()!=PORT)  mismatched.add(g);
			return new JemLeafList();
		} else {
			return super.doFor(g);
		}
	}

	// -------------------- intended interface ------------------------
	public static boolean doYourJob(NccGlobals globals) {
		JemStratPreanalysisReport wp = new JemStratPreanalysisReport(globals);
		return wp.doYourJob2(globals);
	}
}

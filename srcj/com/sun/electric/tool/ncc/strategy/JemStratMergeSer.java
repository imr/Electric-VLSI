/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratMergeSer.java
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

/** JemStratMergeSer merges series transistors
 * It first creates a list of wires with two parts attached.
 * It then tries to perform a serial merge on the wires in that list.
 * 
 * <p>Subtle. I must do this in two passes. Series merge requires the
 * deletion of a wire. However, I can't remove a wire from a list over
 * which I'm currently Iterating. If I do I get a Concurrent Modification
 * Exception.  */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.*;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.Transistor;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

//import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;

public class JemStratMergeSer extends JemStrat {
    private ArrayList candidates = new ArrayList();

    private JemStratMergeSer(NccGlobals globals) {super(globals);}

	private int processCandidates() {
		int numMerged = 0;
		for (Iterator it=candidates.iterator(); it.hasNext();) {
			Wire w = (Wire) it.next();
			if (Transistor.joinOnWire(w)) numMerged++;
		}
		return numMerged;
	}
	
	private boolean doYourJob() {
		JemEquivRecord wires = globals.getWires();
		preamble(wires);
		doFor(wires);
		int numMerged = processCandidates();
		summary(numMerged);

		return numMerged>0;
	}
	
    private void preamble(JemEquivRecord j){
		startTime("JemStratMergeSer" , j.nameString());
    }

    private void summary(int numMerged){
		globals.println("JemStratMergeSer performed " +
					    numMerged + " series merges");		elapsedTime();
    }

    public Integer doFor(NetObject n){
		error(!(n instanceof Wire), "expecting only wires");
		return doWire((Wire)n);
    }

    private Integer doWire(Wire w){
		// this a quick and dirty test for candidacy. 
		// Transistor.joinOnWire does the complete test.
		if (w.numParts()==2) candidates.add(w);
		return CODE_NO_CHANGE;
    }

	// --------------- intended interface --------------------
	public static boolean doYourJob(NccGlobals globals){
		JemStratMergeSer ms = new JemStratMergeSer(globals);
		return ms.doYourJob();
	}
}

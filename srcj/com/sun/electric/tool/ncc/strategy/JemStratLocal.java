/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratLocal.java
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
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.JemLeafList;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.*;
import com.sun.electric.tool.ncc.NccEngine;
import com.sun.electric.tool.ncc.NccOptions;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/** JemStratLocal partitions the Part and Wire equivalence classes based
 * on purely local characteristics. In principle, these partitions are
 * redundant with the normal hash code process. However it seems useful
 * to perform these first because:
 * 1) There is no hash code computation. Therefore errors in one part of
 * the circuit do not cause mismatches in other parts of the circuit.
 * 2) They can be easily explained to the user. */
public class JemStratLocal {
    NccGlobals globals;
    
	private void partitionWiresUsingLocalInformation() {
		globals.println("Partition Wires using local information");
		JemEquivRecord root = globals.getRoot();
		List pinTypes = new ArrayList();
		pinTypes.addAll(Transistor.PIN_TYPES);
		for (Iterator it=pinTypes.iterator(); it.hasNext();) {
			JemPinType pinType = (JemPinType) it.next();
			JemLeafList offspring = 
				JemStratCountPartPinsOnWires.doYourJob(globals, pinType);
			if (offspring.size()!=0) {
				JemStratCheck.doYourJob(root, globals);
				JemStratCount.doYourJob(root, globals);
			}
		}
	}
	
	private void partitionPartsUsingLocalInformation() {
		globals.println("Partition Parts using local information");
		if (globals.getParts()==null) return;
		JemLeafList offspring = JemStratPartType.doYourJob(globals);
		if (offspring.size()!=0) {
			JemStratCheck.doYourJob(globals.getRoot(), globals);
			JemStratCount.doYourJob(globals.getRoot(), globals);
		}
		
		offspring = JemStratPartPopularity.doYourJob(globals);
		if (offspring.size()!=0) {
			JemStratCheck.doYourJob(globals.getRoot(), globals);
			JemStratCount.doYourJob(globals.getRoot(), globals);
		}
	}

    private JemStratLocal(NccGlobals globals) {this.globals = globals;}
    
    private boolean doYourJob2() {
		JemEquivRecord root = globals.getRoot();
        globals.println("Begin partitioning based on local characteristics " +
                        root.nameString());
        globals.println();
		JemStratCheck.doYourJob(root, globals);
		JemStratCount.doYourJob(root, globals);

		mergeParts();
		JemStratCheck.doYourJob(root, globals);
		JemStratCount.doYourJob(root, globals);

		partitionWiresUsingLocalInformation();
		partitionPartsUsingLocalInformation();

		boolean match = JemStratPreanalysisReport.doYourJob(globals);

		globals.println("End partitioning based on local characteristics ");
		return match;

//		JemStratNPsplit.doYourJob(globals);
//		
//		root= globals.getRoot();
//		JemStratCheck.doYourJob(root, globals);
//        JemStratCount.doYourJob(root, globals);
//		
//		//JemWirePopularity uses myJemSets.wires as input
//		JemWirePopularity.doYourJob(globals);
//		
//        JemStratCount.doYourJob(root, globals);
//		JemStratCheck.doYourJob(root, globals);
//		
//		//JemWireStepUp uses myJemSets.wires as input
//		JemWireStepUp.doYourJob(globals);
//		
//		root = globals.getRoot();
//        JemStratCount.doYourJob(root, globals);
//		JemStratCheck.doYourJob(root, globals);
    }
	
	//this routine merges the various parts using series and parallel lists
	private void mergeParts() {
		JemEquivRecord parts = globals.getParts();

		if (parts==null)  return; // No Cell has Parts

		int numParts = parts.maxSize();
		globals.println("--- Jemini starting merge process with " +
					    numParts + " Parts");
		globals.println();
		// Tricky: Don't give up if the first parallel merge attempt fails because 
		// the following first serial merge may succeed!
		boolean first = true;
		for (int tripNumber=1; ; tripNumber++) {
			globals.println("parallel and series merge trip " + tripNumber);
			boolean progress = JemStratMergePar.doYourJob(globals);
			if (!first && !progress) break;
			first = false;
			progress = JemStratMergeSer.doYourJob(globals);
			if (!progress) break;
		}
		numParts = parts.maxSize();
		globals.println("--- Jemini finishing merge process with " +
					    numParts + " Parts");
		globals.println();
	}
	
	// ------------------------ public method ---------------------------------
	public static boolean doYourJob(NccGlobals globals) {
		JemStratLocal jsl = new JemStratLocal(globals);
		return jsl.doYourJob2();
	}
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratFixed.java
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

/**
 * JemStratFixed holds the fixed break-up methods.
 * It's constructor requires a NccGlobals into which it puts its results.
 * The method "doAllFixed" processes an initial JemEquivRecord through:
 *	partWireSplit();
 *	popularWires();
 *	mergeParts();
 *	initialPartsSplit();
 * leaving the NccGlobals ready for the hash code methods.
 */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.JemLeafList;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.*;
import com.sun.electric.tool.ncc.NccEngine;
import com.sun.electric.tool.ncc.NccOptions;

import java.util.ArrayList;
import java.util.Iterator;

public class JemStratFixed {
    NccGlobals globals;
    NccOptions options;

	/** 
	 * constructor does the whole fixed process.
	 * @param globals global data for Ncc task
	 */
    private JemStratFixed(NccGlobals globals){
    	this.globals = globals;

		JemEquivRecord root = globals.getRoot();
        globals.println("Starting fixed methods on " + root.nameString());
        globals.println();

        JemStratCheck.doYourJob(root, globals);
		JemStratCount.doYourJob(root, globals);

		JemStratWiresWithWithoutGates.doYourJob(globals);
		
		JemStratCheck.doYourJob(root, globals);
        JemStratCount.doYourJob(root, globals);

		mergeParts();

		JemStratCheck.doYourJob(root, globals);
        JemStratCount.doYourJob(root, globals);
		
		JemStratNPsplit.doYourJob(globals);
		
		root= globals.getRoot();
		JemStratCheck.doYourJob(root, globals);
        JemStratCount.doYourJob(root, globals);
		
		//JemWirePopularity uses myJemSets.wires as input
		JemWirePopularity.doYourJob(globals);
		
        JemStratCount.doYourJob(root, globals);
		JemStratCheck.doYourJob(root, globals);
		
		//JemWireStepUp uses myJemSets.wires as input
		JemWireStepUp.doYourJob(globals);
		
		root= globals.getRoot();
        JemStratCount.doYourJob(root, globals);
		JemStratCheck.doYourJob(root, globals);
		
		globals.println("**** doAllFixed is finished ****");
    }
	
	//this routine merges the various parts using series and parallel lists
	private void mergeParts() {
		JemEquivRecord theParts = globals.getParts();

		if (theParts==null)  return; // No Cell has Parts

		int numParts= theParts.maxSize();
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
		globals.deleteEmptyWires();
		numParts = theParts.maxSize();
		globals.println("--- Jemini finishing merge process with " +
					  numParts + " Parts");
		globals.println();
	}
	
	// ------------------------ public method ---------------------------------
	public static void doYourJob(NccGlobals globals){
		new JemStratFixed(globals);
	}
}

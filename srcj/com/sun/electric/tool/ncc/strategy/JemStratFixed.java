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
 * It's constructor requires a JemSets into which it puts its results.
 * The method "doAllFixed" processes an initial JemEquivRecord through:
 *	partWireSplit();
 *	popularWires();
 *	mergeParts();
 *	initialPartsSplit();
 * leaving the JemSets ready for the hash code methods.
 */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.JemEquivList;
import com.sun.electric.tool.ncc.basicA.Messenger;

import java.util.ArrayList;
import java.util.Iterator;

public class JemStratFixed {
    JemSets myJemSets;

    private JemStratFixed(JemSets js){myJemSets= js;}

	public static void doYourJob(JemSets js, JemEquivRecord g){
		JemStratFixed jsf = new JemStratFixed(js);
		jsf.doAllFixed(g);
	}

    /** 
	 * doAllFixed does the whole fixed process.
	 * @param the initial JemEquivRecord to process
	 */
    private void doAllFixed(JemEquivRecord g){
        if(g == null){
            Messenger.error("doAllFixed called with null JemEquivRecord");
        }
        myJemSets.starter= g;
        Messenger.line("Starting fixed methods on " + g.nameString());
        Messenger.freshLine();

        JemRecord e= myJemSets.starter;
        JemStratCheck.doYourJob(e);
		JemStratPrint.doYourJob(e);

        //JemStratPartsPortsWireSplit uses myJemSets.starter for input,
        //and puts answers in myJemSets.parts, myJemSets.wires, myJemSets.ports
		JemStratPartWirePort.doYourJob(myJemSets);
        e= myJemSets.starter;
		//JemStratPrint.doYourJob(e);
        JemStratCheck.doYourJob(e);
		JemStratCount.doYourJob(e);

		// JemStratPopularWires uses myJemSets.wires as input,
		// and puts its output in the Lists myJemSets.parallel and myJemSets.series
		JemStratWireSplit.doYourJob(myJemSets);

		e= myJemSets.starter;
		//JemStratPrint.doYourJob(e);
		JemStratCheck.doYourJob(e);
        JemStratCount.doYourJob(e);

		//mergeParts is a local routine.
		//it uses myJemSets.parallel and myJemSets.series as input
		//and may retire some JemEquivRecord,  but it doesn't change these lists
		mergeParts();

		e= myJemSets.starter;
		JemStratCheck.doYourJob(e);
		//JemStratPrint.doYourJob(e);
        JemStratCount.doYourJob(e);
		
		//JemStratNPsplit starts with parts and splits into
		// N-type, P-type and Not transistor
		JemStratNPsplit.doYourJob(myJemSets);
		
		e= myJemSets.starter;
		JemStratCheck.doYourJob(e);
		//JemStratPrint.doYourJob(e);
        JemStratCount.doYourJob(e);
		
		//JemWirePopularity uses myJemSets.wires as input
		JemWirePopularity.doYourJob(myJemSets);
		
		e= myJemSets.starter;
		//JemStratPrint.doYourJob(e);
        JemStratCount.doYourJob(e);
		JemStratCheck.doYourJob(e);
		
		//JemWireStepUp uses myJemSets.wires as input
		JemWireStepUp.doYourJob(myJemSets);
		
		e= myJemSets.starter;
		//JemStratPrint.doYourJob(e);
        JemStratCount.doYourJob(e);
		JemStratCheck.doYourJob(e);
		
		Messenger.line("**** doAllFixed is finished ****");
    }
	
	//this routine merges the various parts using series and parallel lists
	private void mergeParts(){
		JemEquivRecord theParts= (JemEquivRecord)myJemSets.parts;
		int numParts= theParts.maxSize();
		Messenger.line("--- Jemini starting merge process with " +
					  numParts + " Parts");
		Messenger.freshLine();
		int tripNumber= 1;
		for(boolean progress= true; progress == true; tripNumber++){
			Messenger.line("parallel and series merge trip " + tripNumber);
			if(progress)progress= JemStratMergePar.doYourJob(myJemSets);
			if(progress)progress= JemStratMergeSer.doYourJob(myJemSets);
		} //end of for
		JemEquivRecord xx= (JemEquivRecord)myJemSets.noGates;
		if(xx.maxSize()==0){
			myJemSets.noGates= null;
			Messenger.line("   No Wires without gates remain");
		} else {
			Messenger.line("  At most " +
					   xx.maxSize() + " Wires without gates remain");
		} //end of not empty
		numParts= theParts.maxSize();
		Messenger.line("--- Jemini finishing merge process with " +
					  numParts + " Parts");
		Messenger.freshLine();
	}

}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratNPsplit.java
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

/** JemStratNPsplit divides N-transistors, P-transistors and Ports: */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.*;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.jemNets.Port;
import com.sun.electric.tool.ncc.jemNets.Transistor;

//import java.util.Collection;
import java.util.Iterator;

public class JemStratNPsplit extends JemStrat {
    private int numPtype;
    private int numNtype;
    private int numOther;

    private static final int NUM_CODES= 3;
    protected static final Integer CODE_P_TYPE= new Integer(0);
    protected static final Integer CODE_N_TYPE= new Integer(1);
    protected static final Integer CODE_NOT_TRANSISTOR= new Integer(2);

    private JemStratNPsplit(NccGlobals globals) {super(globals);}

    //do something before starting
    private void preamble(JemEquivRecord j){
		startTime("JemStratNPsplit" , j.nameString());
    }

    //summarize at the end
    private void summary(JemLeafList offspring){
        globals.println(" JemStratNPsplit found " +
							numPtype + " P-type, and " +
							numNtype + " N-type, and " +
                            numOther + " Other Parts");
        globals.println(offspring.sizeInfoString());

		globals.println(" JemStratNPsplit offspring: ");
		pickAnOffspring(CODE_P_TYPE, offspring, "  P-type");
		pickAnOffspring(CODE_N_TYPE, offspring, "  N-type");
		pickAnOffspring(CODE_NOT_TRANSISTOR, offspring, "  Other Parts");

		globals.println(offspringStats(offspring));
        elapsedTime();
	}

	//------------- for NetObject ------------

    public Integer doFor(NetObject n){
    	error(n instanceof Wire, "shouldn't have wires here");
		if(n instanceof Transistor){
			Transistor t= (Transistor)n;
			if(t.isNtype()){
				numNtype++;
				return CODE_N_TYPE;
			}
			if(t.isPtype()){
				numPtype++;
				return CODE_P_TYPE;
			}
		}
		numOther++;
		return CODE_NOT_TRANSISTOR;
    }

	// ---------- to do the job -------------

	public static void doYourJob(NccGlobals globals) {
		JemStratNPsplit nps = new JemStratNPsplit(globals);
		nps.doYourJob2(globals);
	}

	public void doYourJob2(NccGlobals globals){
		JemEquivRecord parts = globals.getParts();

		if (parts==null) return; // No cell has Parts

		preamble(parts);
		JemLeafList offspring= doFor(parts);
		summary(offspring);

	}
	
}

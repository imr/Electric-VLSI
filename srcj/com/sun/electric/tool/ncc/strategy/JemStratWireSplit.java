/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratWireSplit.java
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

/* 
 * JemStratWireSplit divides wires into those with gates and those
 * without gates doYourJob(NccGlobals) uses wires as the seed and leaves
 * the two classes in locations called "wiresWithGates" and
 * "wiresWithoutGates".  it also fixes up the "wires" location to be
 * the new JemHistoryRecord.
 */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.*;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.jemNets.*;

import java.util.Iterator;

public class JemStratWireSplit extends JemStrat {
	private int numWithGates;
	private int numNoGates;

	private static final int NUM_CODES= 2;
	protected static final Integer CODE_NO_GATES= new Integer(0);
	protected static final Integer CODE_WITH_GATES= new Integer(1);

    private JemStratWireSplit(NccGlobals globals) {super(globals);}

    // ---------- to do the job -------------

	public static void doYourJob(NccGlobals globals){
		JemStratWireSplit jsws = new JemStratWireSplit(globals);
		jsws.doYourJob2();
	}

	private void doYourJob2(){
        JemEquivRecord wires = globals.getWires();

		preamble(wires);
		JemLeafList offspring= doFor(wires);
		summary(offspring);
	}

	//do something before starting
    private void preamble(JemEquivRecord j){
        startTime("JemStratWireSplit" , j.nameString());
    }

	//summarize at the end
    private void summary(JemLeafList offspring){
        globals.println(" JemStratWireSplit found " +
                       numNoGates + " Wires without gates and " +
                       numWithGates + " Wires with gates");
        globals.println(offspring.sizeInfoString());
		globals.println(" JemStratWireSplit offspring: ");

		JemEquivRecord withGates=null, noGates=null;
		if (offspring.size()==0) {
			// special case. If all wires have gates or if all wires have 
			// no gates then we don't split the EquivRec
			error(numWithGates!=0 && numNoGates!=0, "can't happen");
			if (numWithGates!=0) withGates = globals.getWires();
			if (numNoGates!=0) noGates = globals.getWires();
		} else {
			noGates = pickAnOffspring(CODE_NO_GATES, offspring, 
											  "  Wires without gates");
			withGates = pickAnOffspring(CODE_WITH_GATES, offspring, 
											  "  Wires with gates");
		}
		globals.setWithWithoutGates(withGates, noGates);

		globals.println(offspringStats(offspring));
        elapsedTime();
    }

    //------------- for NetObject ------------

    public Integer doFor(NetObject n){
    	error(!(n instanceof Wire), "JemStratWireSplit expects only Wires");
		Wire w= (Wire)n;
		int nbGates= w.numPartsWithGateAttached();
		if(nbGates==0){
			numNoGates++;
			return CODE_NO_GATES;
		} else {
			numWithGates++;
			return CODE_WITH_GATES;
		}
    }

}

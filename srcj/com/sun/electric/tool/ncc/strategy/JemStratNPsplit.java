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
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.jemNets.Port;
import com.sun.electric.tool.ncc.jemNets.Transistor;
import com.sun.electric.tool.ncc.strategy.JemSets;

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

    private JemStratNPsplit(){}

	// ---------- to do the job -------------

	public static JemRecord doYourJob(JemSets jss) {
		JemStratNPsplit nps = new JemStratNPsplit();
		return nps.doYourJob2(jss);
	}

	public JemRecord doYourJob2(JemSets jss){
		JemEquivRecord ss= (JemEquivRecord)jss.parts;
		preamble(ss);
        JemEquivList offspring= doFor(ss);
        summary(offspring);

		jss.parts = getOffspringParent(offspring);

        Messenger.line("Jemini proceeds with these maximum counts: ");
		pickAnOffspring(CODE_P_TYPE, offspring, "N-type and");
        pickAnOffspring(CODE_N_TYPE, offspring, "N-type and");
        pickAnOffspring(CODE_NOT_TRANSISTOR, offspring, "Other Parts");

		Messenger.line(offspringStats(offspring));
        Messenger.freshLine();
        return jss.parts;
	}
	
    //do something before starting
    private void preamble(JemRecord j){
		startTime("JemStratNPsplit" , j.nameString());
    }

    //summarize at the end
    private void summary(JemEquivList cc){
        Messenger.line("JemStratPWsplit separated " +
							numPtype + " P-type and " +
							numNtype + " N-type into " +
                            numOther + " Other Parts into " +
                            NUM_CODES + " distinct hash groups");
        //	+ numPorts + " Ports.");
        Messenger.line(cc.sizeInfoString());
        elapsedTime(numPtype + numNtype + numOther);
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

}

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
 * without gates doYourJob(JemSets) uses wires as the seed and leaves
 * the two classes in locations called "wiresWithGates" and
 * "wiresWithoutGates".  it also fixes up the "wires" location to be
 * the new JemHistoryRecord.
 */
package com.sun.electric.tool.ncc.strategy;
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

    private JemStratWireSplit(){}

    // ---------- to do the job -------------

	public static JemRecord doYourJob(JemSets jss){
		JemStratWireSplit jsws = new JemStratWireSplit();
		return jsws.doYourJob2(jss);
	}

	private JemRecord doYourJob2(JemSets jss){
        JemEquivRecord ss= (JemEquivRecord) jss.wires;

		preamble(ss);
		JemEquivList offspring= doFor(ss);
		summary(offspring);

        Messenger.line("Jemini proceeds with these maximum counts: ");

		jss.wires = getOffspringParent(offspring);
		jss.noGates= pickAnOffspring(CODE_NO_GATES, offspring, 
									 "Wires without gates and");
        jss.withGates= pickAnOffspring(CODE_WITH_GATES, offspring, 
									   "Wires with gates");

		Messenger.line("JemStratWireSplit: ");
		Messenger.line(offspringStats(offspring));
		Messenger.freshLine();

		for(Iterator it=offspring.iterator(); it.hasNext();){
			JemEquivRecord er= (JemEquivRecord) it.next();
			error(er.getParent() != jss.wires, "got problem");
		}
        return jss.wires;
	}

	//do something before starting
    private void preamble(JemRecord j){
        startTime("JemStratWireSplit" , j.nameString());
    }

	//summarize at the end
    private void summary(JemEquivList cc){
        Messenger.line("JemStratWireSplit separated " +
                            numNoGates + " Wires without gates and " +
                            numWithGates + " Wires with gates into " +
                            NUM_CODES + " distinct hash groups");
        Messenger.line(cc.sizeInfoString());
        elapsedTime(numNoGates + numWithGates);
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

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemWireNamePopularity.java
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
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

import java.util.Iterator;

public class JemWirePopularity extends JemStrat {
	int numWiresProcessed;
	int numEquivProcessed;

	private JemWirePopularity(){}

	public static JemEquivList doYourJob(JemSets jss){
		JemEquivList frontier= JemStratFrontier.doYourJob(jss.wires);
		JemWirePopularity wp = new JemWirePopularity();
		wp.preamble(frontier);
		JemEquivList offspring= wp.doFor(frontier);
		wp.summary(offspring);
		return offspring;
	}

	//do something before starting
	private void preamble(JemEquivList el){
		startTime("JemWirePopularity", " JemEquivList of size: "+el.size());
	}

    //summarize at the end
    private void summary(JemEquivList offspring){
		Messenger.line("JemWirePopularity done - examined " +
							numWiresProcessed + " Wires from " +
							numEquivProcessed + " JemEquivRecords");
		Messenger.line(offspringStats(offspring));
        elapsedTime(numWiresProcessed);
    }

    // ---------- for JemRecord -------------
	
    public JemEquivList doFor(JemRecord g){
		if (g instanceof JemEquivRecord)  numEquivProcessed++;  
		return super.doFor(g);
	}

	//------------- for NetObject ------------
	public Integer doFor(NetObject n){
		error(!(n instanceof Wire), "JemWirePopularity expects wires only");
		Wire w= (Wire)n;
		numWiresProcessed++;
		return new Integer(w.numParts());
	}

}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemHashPartAll.java
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
// JemHashPartAll hashes Parts by all Wires.

package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

public class JemHashParts extends JemStrat {
	private int numPartsProcessed;
	private int numEquivProcessed;

    private JemHashParts(NccGlobals globals){super(globals);}
	
    //do something before starting
    private void preamble(int nbParts){
		startTime("JemHashParts", nbParts+" Parts");
    }
	
    //summarize at the end
    private void summary(JemLeafList offspring){
		globals.println(" processed " +
					   numPartsProcessed + " Parts from " +
					   numEquivProcessed + " leaf Records");
		globals.println(offspringStats(offspring));
		globals.println(offspring.sizeInfoString());
		elapsedTime();
    }
	
    public JemLeafList doFor(JemEquivRecord g){
		if (g.isLeaf())  numEquivProcessed++;
		return super.doFor(g);
    }
	
    public Integer doFor(NetObject n){
		error(!(n instanceof Part), "JemHashPartAll expects only Parts");
		numPartsProcessed++;
		Part p= (Part)n;
		return p.computeHashCode();
    }
	
	// ------------------ intended interface -----------------
	public static JemLeafList doYourJob(JemRecordList l,
										NccGlobals globals){
		JemHashParts jhpa = new JemHashParts(globals);

		// If no Parts suppress all JemHashParts messages
		if (l.size()==0) return new JemLeafList();

		jhpa.preamble(l.size());
		JemLeafList offspring= jhpa.doFor(l);
		jhpa.summary(offspring);
		return offspring;
	}
}

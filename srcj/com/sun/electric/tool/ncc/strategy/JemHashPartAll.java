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
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

public class JemHashPartAll extends JemStrat {
	private int numPartsProcessed;
	private int numEquivProcessed;

    private JemHashPartAll(){}
	
	public static JemEquivList doYourJob(JemRecordList l){
		JemHashPartAll jhpa = new JemHashPartAll();
		jhpa.preamble(l);
		JemEquivList offspring= jhpa.doFor(l);
		jhpa.summary(offspring);
		return offspring;
	}
	
    //do something before starting
    private void preamble(JemRecordList j){
		startTime("JemHashPartAll", "a list of size: "+j.size());
    }
	
	//do something before starting
    private void preamble(JemRecord j){
		startTime("JemHashPartAll", j.nameString());
    }

    //summarize at the end
    private void summary(JemEquivList offspring){
		//JemEquivList cc= JemEquivRecord.tryToRetire(offsp);
		Messenger.line("JemHashPartAll processed " +
							numPartsProcessed + " Parts from " +
							numEquivProcessed + " JemEquivRecords");
		Messenger.line(offspringStats(offspring));
		Messenger.line(offspring.sizeInfoString());
		elapsedTime(numPartsProcessed);
    }
	
	// ---------- for JemRecord -------------
	
    public JemEquivList doFor(JemRecord g){
		if (g instanceof JemEquivRecord)  numEquivProcessed++;
		return super.doFor(g);
    }
	
    //------------- for NetObject ------------
	
    public Integer doFor(NetObject n){
		error(!(n instanceof Part), "JemHashPartAll expects only Parts");
		numPartsProcessed++;
		Part p= (Part)n;
		return p.computeHashCode();
    }
	
}

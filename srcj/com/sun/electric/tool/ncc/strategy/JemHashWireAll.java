/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemHashWireAll.java
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
/** JemHashWireAll hashes Wires by all Parts. */

package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

public class JemHashWireAll extends JemStrat {
	
	private int numWiresProcessed;
	private int numEquivProcessed;
	
	public String nameString(){return "JemHashWireAll";}
	
	private JemHashWireAll(){}
	
	public static JemEquivList doYourJob(JemRecordList l){
		JemHashWireAll hwa = new JemHashWireAll();
		hwa.preamble(l);
		JemEquivList el = hwa.doFor(l);
		hwa.summary(el);
		return el;
	}
	
	//do something before starting
	private void preamble(JemRecordList j){
		startTime("JemHashWireAll", "a list of size: " + j.size());
	}
	
	//summarize at the end
	private void summary(JemEquivList offspring){
		Messenger.line("JemHashWireAll processed " +
							numWiresProcessed + " Wires from " +
							numEquivProcessed + " JemEquivRecords");
		Messenger.line(offspringStats(offspring));
		Messenger.line(offspring.sizeInfoString());
		elapsedTime(numWiresProcessed);
	}
	
	// ---------- for JemRecord -------------
	
    public JemEquivList doFor(JemRecord g){
		JemEquivList out;
		if(g instanceof JemHistoryRecord){
			out = super.doFor(g);
		} else {
			error(!(g instanceof JemEquivRecord), "unrecognized JemRecord");
			numEquivProcessed++;
			out = super.doFor(g);
//			String s= ("processed " + g.nameString());
//			Messenger.line(s + " to get " + out.size() + " offspring ");
		}
		return out;
    }
	
	//------------- for NetObject ------------
	public Integer doFor(NetObject n){
		error(!(n instanceof Wire), "JemHashWireAll expects wires only");
		numWiresProcessed++;
		Wire w= (Wire)n;
		return w.computeHashCode();
	}
	
}

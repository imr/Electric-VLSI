/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemHashWires.java
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
/** JemHashWires hashes Wires by all Parts. */

package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

public class JemHashWires extends JemStrat {
	private int numWiresProcessed;
	private int numEquivProcessed;
	
	private JemHashWires(NccGlobals globals){super(globals);}
	
	private void preamble(int nbWires){
		startTime("JemHashWires", nbWires+" Wires");
	}
	
	private void summary(JemLeafList offspring){
		globals.println(" processed " +
					   numWiresProcessed + " Wires from " +
					   numEquivProcessed + " leaf records");
		globals.println(offspringStats(offspring));
		globals.println(offspring.sizeInfoString());
		elapsedTime();
	}
	
    public JemLeafList doFor(JemEquivRecord g){
		JemLeafList out;
		if(g.isLeaf()){
			numEquivProcessed++;
			out = super.doFor(g);
//			String s= ("processed " + g.nameString());
//			globals.println(s + " to get " + out.size() + " offspring ");
		} else {
			out = super.doFor(g);
		}
		return out;
    }
	
	public Integer doFor(NetObject n){
		error(!(n instanceof Wire), "JemHashWires expects wires only");
		numWiresProcessed++;
		Wire w= (Wire)n;
		return w.computeHashCode();
	}

	// --------------- intended interface ------------------	
	public static JemLeafList doYourJob(JemRecordList l,
										NccGlobals globals){
		// if no Wires suppress all JemHashWires messages
		if (l.size()==0) return new JemLeafList();											
											
		JemHashWires hwa = new JemHashWires(globals);
		hwa.preamble(l.size());
		JemLeafList el = hwa.doFor(l);
		hwa.summary(el);
		return el;
	}
}

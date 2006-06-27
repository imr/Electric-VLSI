/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratHashWires.java
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
/** StratHashWires hashes Wires by all Parts. */

package com.sun.electric.tool.ncc.strategy;
import java.util.Iterator;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Wire;
import com.sun.electric.tool.ncc.trees.EquivRecord;

public class StratHashWires extends Strategy {
	private int numWiresProcessed;
	private int numEquivProcessed;
	
	private StratHashWires(NccGlobals globals){super(globals);}
	
	private void preamble(){
		startTime("StratHashWires", " Wires");
	}
	
	private void summary(LeafList offspring){
		globals.status2(" processed " +
					   numWiresProcessed + " Wires from " +
					   numEquivProcessed + " leaf records");
		globals.status2(offspringStats(offspring));
		globals.status2(offspring.sizeInfoString());
		elapsedTime();
	}
	
    public LeafList doFor(EquivRecord g){
		LeafList out;
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
		error(!(n instanceof Wire), "StratHashWires expects wires only");
		numWiresProcessed++;
		Wire w= (Wire)n;
		return w.computeHashCode();
	}

	// --------------- intended interface ------------------	
	public static LeafList doYourJob(Iterator<EquivRecord> it, NccGlobals globals){
		// if no Wires suppress all StratHashWires messages
		if (!it.hasNext()) return new LeafList();											
											
		StratHashWires hwa = new StratHashWires(globals);
		hwa.preamble();
		LeafList el = hwa.doFor(it);
		hwa.summary(el);
		return el;
	}
	public static LeafList doYourJob(EquivRecord er, NccGlobals globals) {
		StratHashWires hwa = new StratHashWires(globals);
		return hwa.doFor(er);
	}
}

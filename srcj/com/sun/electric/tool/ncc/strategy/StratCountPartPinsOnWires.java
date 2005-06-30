/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratCountPartPinsOnWires.java
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
import java.util.Iterator;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.netlist.PinType;
import com.sun.electric.tool.ncc.netlist.Wire;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/* StratCountPartPinsOnWires partitions Wire equivalence classes
 * based upon how many part pins of a certain type (for example the number
 * of NMOS transistor gates) are on the net.*/
public class StratCountPartPinsOnWires extends Strategy {
	private PinType pinType;
	
    private StratCountPartPinsOnWires(NccGlobals globals, PinType pinType) {
    	super(globals);
    	this.pinType = pinType;
    }

	private LeafList doYourJob2() {
        EquivRecord wires = globals.getWires();

		// don't blow up if no wires
		LeafList offspring = wires!=null ? doFor(wires) : new LeafList();
		
		setReasons(offspring);
		summary(offspring);
		return offspring;
	}
	
	private void setReasons(LeafList offspring) {
		for (Iterator it=offspring.iterator(); it.hasNext();) {
			EquivRecord r = (EquivRecord) it.next();
			int value = r.getValue();
			String reason = value+" = number of "+pinType.description()+" pins.";
			globals.status2(reason);
			r.setPartitionReason(reason);
		}
	}

    private void summary(LeafList offspring) {
        globals.status2(" StratCountPartPinsOnWires produced " + offspring.size() +
                        " offspring when counting " + pinType.description() +
                        " pins");
        if (offspring.size()!=0) {
			globals.status2(offspring.sizeInfoString());
			globals.status2(offspringStats(offspring));
        }
    }

    public Integer doFor(NetObject n){
    	error(!(n instanceof Wire), "StratCountPartPinsOnWires expects only Wires");
		Wire w = (Wire) n;
		int count = 0;
		for (Iterator it=w.getParts(); it.hasNext();) {
			Part p = (Part) it.next();
			count += pinType.numConnectionsToPinOfThisType(p, w);
		}
		return new Integer(count);
    }

	// ------------------------------- intended inteface -----------------------------------
	public static LeafList doYourJob(NccGlobals globals, PinType pinTester){
		StratCountPartPinsOnWires pow = 
			new StratCountPartPinsOnWires(globals, pinTester);
		return pow.doYourJob2();
	}
}

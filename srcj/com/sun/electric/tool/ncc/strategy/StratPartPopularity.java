/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratPartPopularity.java
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.jemNets.NetObject;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/* StratPartPopularity partitions Part equivalence classes
 * based upon how many unique Wires are connected. */
public class StratPartPopularity extends Strategy {
	private Map typeCodeToTypeName = new HashMap();
	
    private StratPartPopularity(NccGlobals globals) {super(globals);}

	private LeafList doYourJob2() {
        EquivRecord parts = globals.getParts();

		LeafList offspring = doFor(parts);
		setReasons(offspring);
		summary(offspring);
		return offspring;
	}
	
	private void setReasons(LeafList offspring) {
		for (Iterator it=offspring.iterator(); it.hasNext();) {
			EquivRecord r = (EquivRecord) it.next();
			int value = r.getValue();
			String reason = "part has "+value+" different Wires attached";
			globals.println(reason);
			r.setPartitionReason(reason);
		}
	}

    private void summary(LeafList offspring) {
        globals.println("StratPartPopularity produced " + offspring.size() +
                        " offspring");
        if (offspring.size()!=0) {
			globals.println(offspring.sizeInfoString());
			globals.println(offspringStats(offspring));
        }
    }

    public Integer doFor(NetObject n){
    	error(!(n instanceof Part), "StratPartPopularity expects only Parts");
		Part p = (Part) n;
		return new Integer(p.numDistinctWires());
    }

	// ------------------------- intended inteface ----------------------------
	public static LeafList doYourJob(NccGlobals globals){
		StratPartPopularity pow = new StratPartPopularity(globals);
		return pow.doYourJob2();
	}
}

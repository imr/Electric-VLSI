/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratPartPopularity.java
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
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.trees.JemEquivRecord;
import com.sun.electric.tool.ncc.trees.NetObject;
import com.sun.electric.tool.ncc.lists.JemLeafList;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Part;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/* JemStratPartPopularity partitions Part equivalence classes
 * based upon how many unique Wires are connected. */
public class JemStratPartPopularity extends JemStrat {
	private Map typeCodeToTypeName = new HashMap();
	
    private JemStratPartPopularity(NccGlobals globals) {super(globals);}

	private JemLeafList doYourJob2() {
        JemEquivRecord parts = globals.getParts();

		JemLeafList offspring = doFor(parts);
		setReasons(offspring);
		summary(offspring);
		return offspring;
	}
	
	private void setReasons(JemLeafList offspring) {
		for (Iterator it=offspring.iterator(); it.hasNext();) {
			JemEquivRecord r = (JemEquivRecord) it.next();
			int value = r.getValue();
			String reason = "part has "+value+" different Wires attached";
			globals.println(reason);
			r.setPartitionReason(reason);
		}
	}

    private void summary(JemLeafList offspring) {
        globals.println("JemStratPartPopularity produced " + offspring.size() +
                        " offspring");
        if (offspring.size()!=0) {
			globals.println(offspring.sizeInfoString());
			globals.println(offspringStats(offspring));
        }
    }

    public Integer doFor(NetObject n){
    	error(!(n instanceof Part), "JemStratPartPopularity expects only Parts");
		Part p = (Part) n;
		return new Integer(p.numDistinctWires());
    }

	// ------------------------- intended inteface ----------------------------
	public static JemLeafList doYourJob(NccGlobals globals){
		JemStratPartPopularity pow = new JemStratPartPopularity(globals);
		return pow.doYourJob2();
	}
}

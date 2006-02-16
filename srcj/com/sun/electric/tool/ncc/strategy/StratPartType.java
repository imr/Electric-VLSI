/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratPartType.java
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
import java.util.Set;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.netlist.PinType;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/* StratPartType partitions Part equivalence classes
 * based upon the Part's type. */
public class StratPartType extends Strategy {
	private Map<Integer,String> typeCodeToTypeName = new HashMap<Integer,String>();
	private Set<PinType> pinTypes;
	
    private StratPartType(Set<PinType> pinTypes, NccGlobals globals) {
    	super(globals);
    	this.pinTypes = pinTypes;
    }

	private LeafList doYourJob2() {
        EquivRecord parts = globals.getParts();

		LeafList offspring = doFor(parts);
		setReasons(offspring);
		summary(offspring);
		return offspring;
	}
	
	private void setReasons(LeafList offspring) {
		for (Iterator<EquivRecord> it=offspring.iterator(); it.hasNext();) {
			EquivRecord r = it.next();
			int value = r.getValue();
			String reason = "part type is "+
			                typeCodeToTypeName.get(new Integer(value));
			globals.status2(reason);
			r.setPartitionReason(reason);
		}
	}

    private void summary(LeafList offspring) {
        globals.status2("StratPartType produced " + offspring.size() +
                        " offspring");
        if (offspring.size()!=0) {
			globals.status2(offspring.sizeInfoString());
			globals.status2(offspringStats(offspring));
        }
    }
    
    public Integer doFor(NetObject n){
    	error(!(n instanceof Part), "StratPartType expects only Parts");
		Part p = (Part) n;
		Integer typeCode = new Integer(p.typeCode());
		String typeName = p.typeString();
		
		String oldTypeName = typeCodeToTypeName.get(typeCode);
		if (oldTypeName!=null) {
			globals.error(!typeName.equals(oldTypeName), 
						  "type code maps to multiple type names");
		} else {
			typeCodeToTypeName.put(typeCode, typeName);
			Set<PinType> partPinTypes = p.getPinTypes();
			pinTypes.addAll(partPinTypes);
		}
		return typeCode;
    }

	// ------------------------- intended inteface ----------------------------
	public static LeafList doYourJob(Set<PinType> pinTypes, NccGlobals globals){
		StratPartType pow = new StratPartType(pinTypes, globals);
		return pow.doYourJob2();
	}
}

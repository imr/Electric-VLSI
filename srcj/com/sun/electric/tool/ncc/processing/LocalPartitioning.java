/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LocalPartitioning.java
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

package com.sun.electric.tool.ncc.processing;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.processing.*;
import com.sun.electric.tool.ncc.strategy.StratCheck;
import com.sun.electric.tool.ncc.strategy.StratCount;
import com.sun.electric.tool.ncc.strategy.StratCountPartPinsOnWires;
import com.sun.electric.tool.ncc.strategy.StratPartPopularity;
import com.sun.electric.tool.ncc.strategy.StratPartType;
import com.sun.electric.tool.ncc.strategy.StratPreanalysisReport;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.*;
import com.sun.electric.tool.ncc.NccEngine;
import com.sun.electric.tool.ncc.NccOptions;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

/** LocalPartitioning partitions the Part and Wire equivalence classes based
 * on purely local characteristics. In principle, these partitions are
 * redundant with the normal hash code process. However it seems useful
 * to perform these first because:
 * 1) There is no hash code computation. Therefore errors in one part of
 * the circuit do not cause mismatches in other parts of the circuit.
 * 2) They can be easily explained to the user. */
public class LocalPartitioning {
    NccGlobals globals;
    /** return a Set of all the types of Pins we might encounter */
	private Set partitionPartsUsingLocalInformation() {
		globals.println("Partition Parts using local information");
		Set pinTypes = new HashSet();
		if (globals.getParts()==null) return pinTypes;
		LeafList offspring = StratPartType.doYourJob(pinTypes, globals);
		if (offspring.size()!=0) {
			StratCheck.doYourJob(globals.getRoot(), globals);
			StratCount.doYourJob(globals.getRoot(), globals);
		}
		
		offspring = StratPartPopularity.doYourJob(globals);
		if (offspring.size()!=0) {
			StratCheck.doYourJob(globals.getRoot(), globals);
			StratCount.doYourJob(globals.getRoot(), globals);
		}
		return pinTypes;
	}

	private void partitionWiresUsingLocalInformation(Set pinTypes) {
		globals.println("Partition Wires using local information");
		EquivRecord root = globals.getRoot();
		for (Iterator it=pinTypes.iterator(); it.hasNext();) {
			PinType pinType = (PinType) it.next();
			LeafList offspring = 
				StratCountPartPinsOnWires.doYourJob(globals, pinType);
			if (offspring.size()!=0) {
				StratCheck.doYourJob(root, globals);
				StratCount.doYourJob(root, globals);
			}
		}
	}
	
    private LocalPartitioning(NccGlobals globals) {this.globals = globals;}
    
    private boolean doYourJob2() {
        globals.println("Begin partitioning based on local characteristics \n");

		Set pinTypes = partitionPartsUsingLocalInformation();
		partitionWiresUsingLocalInformation(pinTypes);

		boolean match = StratPreanalysisReport.doYourJob(globals);

		globals.println("End partitioning based on local characteristics ");
		return match;
    }
	
	// ------------------------ public method ---------------------------------
	public static boolean doYourJob(NccGlobals globals) {
		LocalPartitioning jsl = new LocalPartitioning(globals);
		return jsl.doYourJob2();
	}
}

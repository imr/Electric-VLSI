/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratPrint.java
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

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/** 
 * StratPrint gives a limited print out of the tree
 * It prints not more than maxLines stuff for each list
 * and not more than maxPins in any one Wire
 */
public class StratPrint extends Strategy {

    // ---------- private data -------------

    // these integers set limits to the amount of output printing
    private static final int MAX_LINES= 4; //max number Parts or Wires to print

    public String nameString(){return "StratPrint";}

    private StratPrint(NccGlobals globals) {super(globals);}

	public static LeafList doYourJob(EquivRecord j,
	                                     NccGlobals globals){
		NccOptions options = globals.getOptions();
		int saveHowMuchStatus = options.howMuchStatus;
		options.howMuchStatus = 10;

		StratPrint jsp = new StratPrint(globals);
		jsp.preamble(j);
		LeafList el= jsp.doFor(j); //prints content and offspring if any
		jsp.summary();

		options.howMuchStatus = saveHowMuchStatus;
		return el;
	}

    // ---------- the tree walking code ---------
    private void preamble(EquivRecord j) {
    	startTime("StratPrint", j.nameString());
    }

	private void summary(){elapsedTime();}

	public LeafList doFor(EquivRecord j){
		globals.status2("**  Depth= " + getDepth() + " "+j.nameString());
		return super.doFor(j);
	}

	// has extra stuff to limit the amount of printing
	public HashMap doFor(Circuit j){
		globals.status2("**  Depth= " + getDepth() +
					    " " + j.nameString()+
						(j.numNetObjs()<MAX_LINES ? "" : " starts with"));

		int count= 0;
		HashMap<Integer,NetObject> codeToNetObjs = new HashMap<Integer,NetObject>();
		for (Iterator<NetObject> it=j.getNetObjs(); it.hasNext();) {
			NetObject n= (NetObject) it.next();
			codeToNetObjs.put(CODE_NO_CHANGE, n);
			/*if (count<MAX_LINES) */globals.status2(n.fullDescription());
			count++;
		}
		return codeToNetObjs;
	}

	public Integer doFor(NetObject n){
		error(true, "should never get here");
		return CODE_ERROR;
	}
}

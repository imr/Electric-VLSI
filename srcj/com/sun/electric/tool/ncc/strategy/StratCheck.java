/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratCheck.java
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
import java.util.List;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

public class StratCheck extends Strategy {

    //these are variables that pass between levels of the tree
    private EquivRecord recordParent;
    private Circuit circuitParent;

    private StratCheck(NccGlobals globals) {super(globals);}

	public static LeafList doYourJob(EquivRecord j, 
									     NccGlobals globals) {
		StratCheck jsc = new StratCheck(globals);
		jsc.preamble(j);
		LeafList el = jsc.doFor(j);    	
		jsc.summary(j);
		return el;
	}

    // ---------- the tree walking code ---------

    //do something before starting
    private void preamble(EquivRecord j){
//		startTime("StratCheck", j.nameString());
    }

    //summarize at the end
    private void summary(EquivRecord x){
//        elapsedTime();
    }

    public LeafList doFor(EquivRecord j){
    	j.checkMe(recordParent);
        EquivRecord oldParent= recordParent; //save the old one
        recordParent= j;
		LeafList el= super.doFor(j);
        recordParent= oldParent;
        return el;
    }
    
    public HashMap<Integer,List<NetObject>> doFor(Circuit j){
        j.checkMe((EquivRecord)recordParent);
        circuitParent= j;
        return super.doFor(j);
    }

    public Integer doFor(NetObject n){
    	n.checkMe(circuitParent);
        return CODE_NO_CHANGE;
    }

}

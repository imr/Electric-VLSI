/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratFrontier.java
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

/** StratFrontier finds all non-retired EquivRecords */

package com.sun.electric.tool.ncc.strategy;

import java.util.HashMap;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

public class StratFrontier extends Strategy {
    private StratFrontier(NccGlobals globals) {super(globals);}

    private void summary(LeafList x){
		globals.println(" StratFrontier ");
		globals.println(offspringStats(x));
    }

	public LeafList doFor(EquivRecord j){
		LeafList frontier = new LeafList();
		if(j.isLeaf()){
			EquivRecord er= (EquivRecord)j;
			if (!er.isRetired())  frontier.add(j);
		} else {
			frontier = super.doFor(j);
		}
		return frontier;
	}

	// ------------------- intended interface ------------------------
	public static LeafList doYourJob(RecordList r,
										NccGlobals globals) {
		StratFrontier jsf = new StratFrontier(globals);
		LeafList el= jsf.doFor(r);
		jsf.summary(el);
        return el;
    }
    
    public static LeafList doYourJob(EquivRecord r,
    									NccGlobals globals) {
    	if (r==null)  return new LeafList();
    	
    	StratFrontier jsf = new StratFrontier(globals);
    	LeafList el = jsf.doFor(r);
    	jsf.summary(el);
    	return el;
    }
}

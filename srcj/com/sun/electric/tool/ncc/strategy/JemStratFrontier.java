/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratFrontier.java
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

/** JemStratFrontier finds all non-retired JemEquiRecords */

package com.sun.electric.tool.ncc.strategy;

import java.util.HashMap;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

public class JemStratFrontier extends JemStrat {
	private int numHistoryRecords;

    private JemStratFrontier(NccGlobals globals) {super(globals);}

    // ---------- the tree walking code ---------

    private void preamble(){
//		startTime("JemStratFrontier", "JemLeafList");
    }
	
    private void preamble(JemEquivRecord j){
//		startTime("JemStratFrontier", j.nameString());
    }
	
    //summarize at the end
    private void summary(JemLeafList x){
//        globals.println(" JemStratFrontier done - used ");
//        globals.println(numHistoryRecords + " HistoryRecords");
		globals.println(" JemStratFrontier ");
		globals.println(offspringStats(x));
//        elapsedTime();
    }

	public static JemLeafList doYourJob(JemRecordList r,
										NccGlobals globals) {
		JemStratFrontier jsf = new JemStratFrontier(globals);
        jsf.preamble();
		JemLeafList el= jsf.doFor(r);
		jsf.summary(el);
        return el;
    }
    
    public static JemLeafList doYourJob(JemEquivRecord r,
    									NccGlobals globals) {
    	if (r==null)  return new JemLeafList();
    	
    	JemStratFrontier jsf = new JemStratFrontier(globals);
    	jsf.preamble(r);
    	JemLeafList el = jsf.doFor(r);
    	jsf.summary(el);
    	return el;
    }
	
    // ---------- for JemEquivRecord -------------

    public JemLeafList doFor(JemEquivRecord j){
		JemLeafList frontier = new JemLeafList();
		if(j.isLeaf()){
			JemEquivRecord er= (JemEquivRecord)j;
			if (!er.isRetired())  frontier.add(j);
		} else {
			numHistoryRecords++;
			frontier = super.doFor(j);
		}
		return frontier;
    }
}

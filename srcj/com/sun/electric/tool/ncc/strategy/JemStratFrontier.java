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

import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

public class JemStratFrontier extends JemStrat {
	int numHistoryRecords;

    private JemStratFrontier(){}

    // ---------- the tree walking code ---------

    //do something before starting
    private void preamble(){
		startTime("JemStratFrontier", "starting on a JemEquivList");
    }
	
    //do something before starting
    private void preamble(JemRecord j){
		startTime("JemStratFrontier", j.nameString());
    }
	
    //summarize at the end
    private void summary(JemEquivList x){
        Messenger.say("JemStratFrontier done - used ");
        Messenger.line(numHistoryRecords + " HistoryRecords");
		Messenger.line(offspringStats(x));
        elapsedTime();
    }

	public static JemEquivList doYourJob(JemRecordList r) {
		JemStratFrontier jsf = new JemStratFrontier();
        jsf.preamble();
		JemEquivList el= jsf.doFor(r);
		jsf.summary(el);
        return el;
    }
    
    public static JemEquivList doYourJob(JemRecord r) {
    	JemStratFrontier jsf = new JemStratFrontier();
    	jsf.preamble(r);
    	JemEquivList el = jsf.doFor(r);
    	jsf.summary(el);
    	return el;
    }
	
    // ---------- for JemRecord -------------

    public JemEquivList doFor(JemRecord j){
		JemEquivList frontier = new JemEquivList();
		if(j instanceof JemHistoryRecord){
			numHistoryRecords++;
			frontier = super.doFor(j);
		} else {
			error(!(j instanceof JemEquivRecord), "unrecognized JemRecord");
			JemEquivRecord er= (JemEquivRecord)j;
			if (!er.isRetired())  frontier.add(j);
		}
		return frontier;
    }

    // ---------- for JemCircuit -------------

    public HashMap doFor(JemCircuit j){
    	error(true, "shouldn't call doFor(JemCircuit)");
		return null;
    }

    // ---------- for NetObject -------------

    public Integer doFor(NetObject n){
    	error(true, "shouldn't call doFor(NetObject)");
        return CODE_ERROR;
    }

}

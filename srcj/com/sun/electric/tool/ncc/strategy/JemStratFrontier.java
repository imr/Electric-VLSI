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

/** JemStratFrontier finds and returns the frontier */

package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

public class JemStratFrontier extends JemStratNone{
    public String nameString(){return "JemStratFrontier";}

    //these are variables that pass between levels of the tree
	
	int numHistoryRecords;
	int numEquivRecords;
	JemEquivList frontier;

    private JemStratFrontier(){
        super();
		numHistoryRecords= 0;
		numEquivRecords= 0;
		frontier= new JemEquivList();
    }

    public static JemStratFrontier please(){return new JemStratFrontier();}

    // ---------- the tree walking code ---------

    //do something before starting
    private void preamble(JemRecordList j){
        if(getDepth() != 0)return;
        //depth is zero
		numHistoryRecords= 0;
		numEquivRecords= 0;
		frontier= new JemEquivList();
		startTime("JemStratFrontier", "starting on a JemEquivList");
    } //end of preamble
	
    //do something before starting
    private void preamble(JemParent j){
        if(getDepth() != 0)return;
        //depth is zero
		numHistoryRecords= 0;
		numEquivRecords= 0;
		frontier= new JemEquivList();
		startTime("JemStratFrontier", j.nameString());
    } //end of preamble
	
    //summarize at the end
    private void summary(JemEquivList x){
        if(getDepth() != 0)return;
        //depth is zero
            getMessenger().say("JemStratFrontier done - used ");
            getMessenger().line(numHistoryRecords + " HistoryRecords to find " +
								numEquivRecords + " EquivRecords");
        elapsedTime();
    } //end of summary

	public JemEquivList doFor(JemRecordList r){
		startTime(nameString(), "JemStratFrontier");
		//getMessenger().line(
		depth= 0;
        preamble(r);
		JemEquivList out= super.doFor(r);
		//		elapsedTime();
		summary(out);
        return out;
    } //end of doFor(JemRecordList)
	
    // ---------- for JemRecord -------------

    public JemEquivList doFor(JemRecord j){
        preamble(j);
		JemEquivList frontier= new JemEquivList();
		if(j instanceof JemHistoryRecord){
			numHistoryRecords++;
			JemEquivList xx= super.doFor(j);
			frontier.addAll(xx);
		} else if(j instanceof JemEquivRecord){
			JemEquivRecord er= (JemEquivRecord)j;
			numEquivRecords++;
			if( ! er.canRetire())frontier.add(j);
		} //end of else
		summary(frontier);
		return frontier;
    } //end of doFor

    // ---------- for JemCircuit -------------

    public JemCircuitMap doFor(JemCircuit j){
		getMessenger().line(nameString() + "shouldn't call doFor(JemCircuit)");
		return null;
    } //end of doFor

    // ---------- for NetObject -------------

    public Integer doFor(NetObject n){
		getMessenger().line(nameString() + "shouldn't call doFor(NetObject)");
        return null;
    } //end of doFor

}

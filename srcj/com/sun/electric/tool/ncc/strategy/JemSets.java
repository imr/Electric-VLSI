/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemSets.java
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

/* 
 * JemSets holds the StrategyGroups whose position must be known.
 * A sort of "Common" area in the Fortran sense.
 */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.trees.JemEquivRecord;
import com.sun.electric.tool.ncc.trees.JemHistoryRecord;
import com.sun.electric.tool.ncc.trees.JemRecord;
import com.sun.electric.tool.ncc.lists.JemRecordList;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.strategy.JemStrat;
import com.sun.electric.tool.ncc.strategy.JemStratCount;
import com.sun.electric.tool.ncc.strategy.JemStratCheck;

//import java.util.Set;
//import java.util.List;
//import java.util.Collection;
//import java.util.Iterator;
//import java.util.ArrayList;
//import java.util.HashSet;

public class JemSets {
    protected JemRecord starter= null;
    protected JemRecord parts= null;
    protected JemRecord wires= null;
    protected JemRecord ports= null;
	protected JemRecord noGates= null;
	protected JemRecord withGates= null;
	protected JemRecordList workingParts;
    protected JemRecordList workingWires;

/*
	protected JemRecord ptype= null;
	protected JemRecord ntype= null;

    protected JemRecordList transistors= new JemRecordList(2);
    protected JemRecordList active= new JemRecordList(2);

    protected JemRecordList series= new JemRecordList(2);
    protected JemRecordList parallel= new JemRecordList(4);
    protected JemRecordList other= new JemRecordList(4);

    //    protected TwinList workingPorts;
*/
	
    public JemSets(){
        workingWires= null;
        workingParts= null;
    }

    public void sizeReport(){
        Messenger.say("There are " + workingParts.size() + " working Parts");
        Messenger.line(" and " + workingWires.size() + " working Wires");
    } //end of sizeReport

/*    public int setPartFrontier(){
        JemRecordList cc= JemHistoryRecord.findTheLeaves(parts);
        workingParts= cc;
        return workingParts.size();
    } //end of setWireFrontier

    public int setWireFrontier(){
        JemRecordList cc= JemHistoryRecord.findTheLeaves(wires);
        workingWires= cc;
        return workingWires.size();
    } //end of setWireFrontier

    public void showTheFrontier(JemRecord g){
        JemRecordList front;
        if(g == null)front= JemHistoryRecord.findTheLeaves(starter);
        else front= JemHistoryRecord.findTheLeaves(g);
        Messenger.line("Printing the frontier:");
        JemEquivRecord.print("the frontier from starter", front);
    } //end of showTheFrontier
*/
} //end of JemSets

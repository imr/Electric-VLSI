/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemAdjacent.java
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

/**
 * JemAdjacent returns a list of the JemEquivRecords adjacent to the
 * input list or record
 */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.trees.*;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

public class JemAdjacent extends JemStratSome {
	int numEquivProcessed;
	int numAdjacentFound;
	int numAdjacentUnique;

	public String nameString(){return "JemAdjacent";}

	private JemAdjacent(){super();}
	
	public static JemAdjacent please(){return new JemAdjacent();}
	
	//do something before starting
	private void preamble(JemRecordList j){
		if(getDepth() != 0)return;
		String s= "a list of " + j.size();
		startTime("JemAdjacent", s);
		numEquivProcessed= 0;
		numAdjacentFound= 0;
		numAdjacentUnique= 0;
		return;
	} //end of preamble
	
    //summarize at the end
    private void summary(JemEquivList offspring){
        if(getDepth() != 0)return;
		JemRecordList cc= JemEquivRecord.tryToRetire(offspring);
		getMessenger().line("JemAdjacent done - processed " +
							numEquivProcessed + " JemEquivRecords");
		getMessenger().line(" to find " + numAdjacentFound + 
							" adjacent hash groups, of which " +
							cc.size() + " are unique");
		elapsedTime(numEquivProcessed);
        return;
    } //end of summary
	
	// ---------- for JemRecordList -------------
	
    public JemEquivList doFor(JemRecordList g){
		depth= 0;
		preamble(g);
		Set adjacent= new HashSet(5);
		JemEquivList mm= super.doFor(g);
		//cull the list to remove duplicates
		Iterator it= mm.iterator();
		while(it.hasNext()){
			JemEquivRecord er= (JemEquivRecord)it.next();
			adjacent.add(er);
		} //end of while
		JemEquivList out= new JemEquivList();
		it= adjacent.iterator();
		while(it.hasNext()){
			JemEquivRecord er= (JemEquivRecord)it.next();
			out.add(er);
			numAdjacentUnique++;
		} //end of while
		summary(out);
        return out;
    } //end of doFor(JemRecordList)
	
	// ---------- for JemRecord -------------
	
    public JemEquivList doFor(JemRecord g){
		JemEquivList out= new JemEquivList();
		if(g instanceof JemHistoryRecord){
			getMessenger().line("processing " + g.nameString());
			JemEquivList these= super.doFor(g);
			out.addAll(these);
		} else if(g instanceof JemEquivRecord){
			numEquivProcessed++;
			String s= ("processed " + g.nameString());
			JemEquivRecord er= (JemEquivRecord)g;
			out= er.adjacentGroups();
//			getMessenger().line(s + " to find " + out.size() + " adjacent");
			numAdjacentFound += out.size();
		} //end of else
		return out;
	} //end of doFor
	
	//------------- for NetObject ------------
	public Integer doFor(NetObject n){
		getMessenger().error("doFor(NetObject) called in " + nameString());
		return null;
	} //end of doFor
} //end of JemAdjacent

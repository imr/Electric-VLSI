/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemWireName.java
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

//this one splits a wire circuit according to matching names

package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

import java.util.Map;
import java.util.Collections;

public class JemWireName extends JemStratSome {
	private int numWiresProcessed;
	private int numEquivProcessed;
	private int numOffspringProduced;
	private Map theMap;
	private boolean doneOne;
	private static JemStratFrontier myFrontier;
	
	public String nameString(){return "JemWireName";}

	private JemWireName(){
		super(); //numCodes
		theMap= null;
		myFrontier= JemStratFrontier.please();
	} //end of constructor
	
	public static JemWireName please(){return new JemWireName();}
	
	public JemEquivList doYourJob(JemSets jss){
        JemRecord w= jss.wires;
        JemEquivList frontier= myFrontier.doFor(w);
		frontier.sort();
		JemEquivList offspring= doFor(frontier);
		return offspring;
	} //end of doYourJob
		
	//do something before starting
	private void preamble(JemRecordList j){
		if(getDepth() != 0)return;
		String s= " a list of " + j.size();
		startTime("JemWireName", s);
		numWiresProcessed= 0;
		numEquivProcessed= 0;
		numOffspringProduced= 0;
		return;
	} //end of preamble
	
	//summarize at the end
	private void summary(JemEquivList cc){
		if(getDepth() != 0)return;
		//JemRecordList out= JemEquivRecord.tryToRetire(cc);
		getMessenger().line("JemWireName processed " +
							numWiresProcessed + " Wires from " +
							numEquivProcessed + " JemEquivRecords");
		getMessenger().line(" to make " + numOffspringProduced + 
							" distinct hash groups, of which " +
							"some" + " remain active");
		//out.size() + " remain active");
		getMessenger().line(cc.sizeInfoString());
		elapsedTime(numWiresProcessed);
		return;
	} //end of summary
	
	// ---------- for JemRecordList -------------
	
    public JemEquivList doFor(JemRecordList g){
		JemEquivList gg= (JemEquivList)g;
		gg.sort();
		preamble(gg);
		doneOne= false;
		JemEquivList mm= super.doFor(gg);
		summary(mm);
		return mm;
    } //end of doFor
	
	// ---------- for JemRecord -------------
	
    public JemEquivList doFor(JemRecord g){
		JemEquivList out= new JemEquivList();
		if(g instanceof JemHistoryRecord){
			JemEquivList these= super.doFor(g);
			out.addAll(these);
		} else if(g instanceof JemEquivRecord){
			if(doneOne)return out;
			numEquivProcessed++;
			String s= ("processed " + g.nameString());
			JemEquivRecord er= (JemEquivRecord)g;
			theMap= er.getWireExportMap();
		//	JemEquivRecord.printTheMap(theMap);
			if(theMap.size() == 0)return out; //nothing to map
			doneOne= true;
			String m= " with map size= " + theMap.size();
			JemEquivList these= super.doFor(g);
			getMessenger().line(s + m + " to get " + these.size() +
								" offspring ");
			out.addAll(these);
			numOffspringProduced += these.size();
		} //end of else
		return out;
    } //end of doFor
	
	// ---------- for JemCircuit -------------
	
    public JemCircuitMap doFor(JemCircuit g){
		JemCircuitMap mm= super.doFor(g);
        return mm;
    } //end of doFor
	
	//------------- for NetObject ------------
	public Integer doFor(NetObject n){
		if(n instanceof Wire){
			numWiresProcessed++;
			Wire w= (Wire)n;
			Integer i= (Integer)theMap.get(w);
			if(i == null)return new Integer(0);
			return i;
		} //end of Parts
		else return null;
	} //end of doFor
	
} //end of class JemWireName

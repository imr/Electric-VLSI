/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemHashPartAll.java
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
// JemHashPartAll hashes Parts by all Wires.

package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

public class JemHashPartAll extends JemStratSome{
	private int numPartsProcessed;
	private int numEquivProcessed;
	private int numOffspringProduced;
	private static JemStrat myFront= JemStratFrontier.please();

    public String nameString(){return "JemHashPartAll";}
	
    private JemHashPartAll(){
		super(); 
    } //end of constructor
	
    public static JemHashPartAll please(){return new JemHashPartAll();}
	
	public JemEquivList doYourJob(JemSets jss){
		JemRecord p= jss.parts;
		JemEquivList frontier= myFront.doFor(p);
		depth= 0;
		preamble(p);
		JemEquivList offspring= super.doFor(frontier);
		summary(offspring);
		return offspring;
	} //end of doYourJob
	
    //do something before starting
    private void preamble(JemRecordList j){
		if(getDepth() != 0)return;
		String s= "a list of " + j.size();
		startTime("JemHashPartAll", s);
		numPartsProcessed= 0;
		numEquivProcessed= 0;
		numOffspringProduced= 0;
		return;
    } //end of preamble
	
	  //do something before starting
    private void preamble(JemRecord j){
		if(getDepth() != 0)return;
		startTime("JemHashPartAll", j.nameString());
		numPartsProcessed= 0;
		numEquivProcessed= 0;
		numOffspringProduced= 0;
		return;
    } //end of preamble

    //summarize at the end
    private void summary(JemEquivList offsp){
		if(getDepth() != 0)return;
		//JemEquivList cc= JemEquivRecord.tryToRetire(offsp);
		getMessenger().line("JemHashPartAll processed " +
							numPartsProcessed + " Parts from " +
							numEquivProcessed + " JemEquivRecords");
		getMessenger().line(" to make " + numOffspringProduced + 
							" distinct hash groups, of which " +
							"some" + " remain active");
		getMessenger().line(offsp.sizeInfoString());
		elapsedTime(numPartsProcessed);
		return;
    } //end of summary
	
	// ---------- for JemRecordList -------------
	
    public JemEquivList doFor(JemRecordList g){
		preamble(g);
		JemEquivList mm= super.doFor(g);
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
			numEquivProcessed++;
			String s= ("processed " + g.nameString());
			JemEquivList these= super.doFor(g);
			
			//returns null pointer in some cases
			
	//		getMessenger().line(s + " to get " + these.size() + " offspring ");
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
		if(n instanceof Part){
			if(n instanceof Port){
				int i= 0; //for debug
			}
			numPartsProcessed++;
			Part p= (Part)n;
			Integer i=p.computeCode(3);
			return i;
		} //end of Parts
		else return null;
    } //end of doFor
	
} //end of class JemHashPartAll

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemHashWireAll.java
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
/** JemHashWireAll hashes Wires by all Parts. */

package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

public class JemHashWireAll extends JemStratSome{
	
	private int numWiresProcessed;
	private int numEquivProcessed;
	private int numOffspringProduced;
	
	public String nameString(){return "JemHashWireAll";}
	
	private JemHashWireAll(){
		super(); //numCodes
	} //end of constructor
	
	public static JemHashWireAll please(){return new JemHashWireAll();}
	
	//do something before starting
	private void preamble(JemRecordList j){
		if(getDepth() != 0)return;
		String s= "a list of " + j.size();
		startTime("JemHashWireAll", s);
		numWiresProcessed= 0;
		numEquivProcessed= 0;
		numOffspringProduced= 0;
		return;
	} //end of preamble
	
	//summarize at the end
	private void summary(JemEquivList cc){
		if(getDepth() != 0)return;
		//JemRecordList out= JemEquivRecord.tryToRetire(cc);
		getMessenger().line("JemHashWireAll processed " +
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
//			getMessenger().line(s + " to get " + these.size() + " offspring ");
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
			Integer i= w.computeCode(3);
			return i;
		} //end of Parts
		else return null;
	} //end of doFor
	
} //end of class JemHashWireAll

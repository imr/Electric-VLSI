/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemWireStepUp.java
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
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.basicA.JemHistogram;

import java.util.Iterator;

public class JemWireStepUp extends JemStratSome{
	int numWiresProcessed;
	int numEquivProcessed;
	int numOffspringProduced;
	private static JemStrat myFront= JemStratFrontier.please();

	public String nameString(){return "JemWireStepUp";}
	private JemWireStepUp(){}

	public static JemEquivList doYourJob(JemSets jss){
		JemWireStepUp jws = new JemWireStepUp();
		return jws.doYourJob2(jss);
	}
	private JemEquivList doYourJob2(JemSets jss){
		JemRecord w= jss.wires;
		JemEquivList frontier= myFront.doFor(w);
		depth= 0;
		preamble(w);
		JemEquivList offspring= doFor(frontier);
		summary(offspring);
		return offspring;
	}

	//do something before starting
	private void preamble(JemRecord j){
		if(getDepth() != 0)return;
		startTime("JemWireStepUp", j.nameString());
		numWiresProcessed= 0;
		numEquivProcessed= 0;
		numOffspringProduced= 0;
		return;
	}

    //summarize at the end
    private void summary(JemEquivList offspring){
        if(getDepth() != 0)return;
		JemRecordList cc= JemEquivRecord.tryToRetire(offspring);
		getMessenger().line("JemWireStepUp done - processed " +
							numWiresProcessed + " Wires from " +
							numEquivProcessed + " JemEquivRecords");
		getMessenger().line(" to make " + numOffspringProduced + 
							" distinct hash groups, of which " +
							cc.size() + " remain active");
		JemHistogram jh= new JemHistogram(3);
		Iterator it= offspring.iterator();
		while(it.hasNext()){
			JemEquivRecord er= (JemEquivRecord)it.next();
			jh.incrementEntry(er.maxSize());
		}
		jh.printMe(getMessenger());
		elapsedTime(numWiresProcessed);
        return;
    } //end of summary

	// ---------- for JemRecordList -------------
	
    public JemEquivList doFor(JemRecordList g){
		JemEquivList mm= super.doFor(g);
        return mm;
    } //end of doFor

	// ---------- for JemRecord -------------
	
    public JemEquivList doFor(JemRecord g){
		preamble(g);
		JemEquivList out= new JemEquivList();
		if(g instanceof JemHistoryRecord){
			getMessenger().line("processing " + g.nameString());
			JemEquivList these= super.doFor(g);
			out.addAll(these);
		} else {
			if(!(g instanceof JemEquivRecord))
				getMessenger().error("unrecognized JemRecord");
			numEquivProcessed++;
			String s= ("processed " + g.nameString());
			JemEquivList these= super.doFor(g);
	//		getMessenger().line(s + " to get " + these.size() + " offspring");
			out.addAll(these);
			numOffspringProduced += these.size();
		} //end of else
		summary(out);
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
			int pp= w.stepUp();
			Integer i= new Integer(pp);
			return i;
		} //end of Parts
		else return null;
	} //end of doFor

} //end of class JemWireStepUp

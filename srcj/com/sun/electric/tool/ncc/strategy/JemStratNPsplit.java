/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratNPsplit.java
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

/** JemStratNPsplit divides N-transistors, P-transistors and Ports: */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.jemNets.Port;
import com.sun.electric.tool.ncc.jemNets.Transistor;
import com.sun.electric.tool.ncc.strategy.JemSets;

//import java.util.Collection;
import java.util.Iterator;

public class JemStratNPsplit extends JemStratSome{
    private int numPtype= 0;
    private int numNtype= 0;
    private int numOther= 0;

    private static int numCodes= 3;
    protected static final Integer CODE_P_TYPE= new Integer(0);
    protected static final Integer CODE_N_TYPE= new Integer(1);
    protected static final Integer CODE_NOT_TRANSISTOR= new Integer(2);

    public String nameString(){return "JemStratNPsplit";}

    private JemStratNPsplit(){}

    public static JemStratNPsplit please(){return new JemStratNPsplit();}

	// ---------- to do the job -------------

	public JemRecord doYourJob(JemSets jss){
		JemRecord s= jss.parts;
		JemEquivRecord ss= (JemEquivRecord)s;
        JemEquivList offspring= doFor(ss);

		JemEquivRecord ee;
        getMessenger().line("Jemini proceeds with these maximum counts: ");
		ee= pickAnOffspring(CODE_P_TYPE, offspring, "N-type and");
        if(ee != null){
			jss.parts= (JemRecord)ee.getParent();
		} //end of if
        ee= pickAnOffspring(CODE_N_TYPE, offspring, "N-type and");
		if(ee != null){
			jss.parts= (JemRecord)ee.getParent();
		} //end of if
        ee= pickAnOffspring(CODE_NOT_TRANSISTOR, offspring, "Other Parts");
        if(ee != null){
			jss.parts= (JemRecord)ee.getParent();
		} //end of if
		getMessenger().line("NB3 there are " + offspring.size() + " offspring");
		JemEquivRecord.tryToRetire(offspring);
        getMessenger().freshLine();
        return jss.parts;
	} //end of doYourJob
	
    //do something before starting
    private void preamble(JemRecord j){
		if(getDepth() != 0)return;
		startTime("JemStratNPsplit" , j.nameString());
        numPtype= 0;
        numNtype= 0;
		numOther= 0;
    } //end of preamble

    //summarize at the end
    private void summary(JemEquivList cc){
        if(getDepth() != 0)return;
        getMessenger().line("JemStratPWsplit separated " +
							numPtype + " P-type and " +
							numNtype + " N-type into " +
                            numOther + " Other Parts into " +
                            numCodes + " distinct hash groups");
        //	+ numPorts + " Ports.");
        getMessenger().line(cc.sizeInfoString());
        elapsedTime(numPtype + numNtype + numOther);
	} //end of summary

    // ---------- for JemRecord -------------
	
    public JemEquivList doFor(JemRecord j){
        preamble(j);
        JemEquivList rl= super.doFor(j);
        summary(rl);
        return rl;
    } //end of doFor
	
	// ---------- for JemCircuit -------------
	
    public JemCircuitMap doFor(JemCircuit c){
		JemCircuitMap m= super.doFor(c);
		return m;
	} //end of doFor(JemCircuit)
	
	  //------------- for NetObject ------------

    public Integer doFor(NetObject n){
		if(n instanceof Wire){
			getMessenger().error("shouldn't have wires here");
			return null;
		} //end of wires
		if(n instanceof Transistor){
			Transistor t= (Transistor)n;
			if(t.isNtype()){
				numNtype++;
				return CODE_N_TYPE;
			} //end of if
			if(t.isPtype()){
				numPtype++;
				return CODE_P_TYPE;
			} //end of if
		} //end of Parts
		numOther++;
		return CODE_NOT_TRANSISTOR;
    } //end of doFor

} //end of class JemStratNPsplit

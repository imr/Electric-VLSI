/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratMergeSer.java
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
 * JemStratMergeSer merges series transistors
 * it starts with all wires, separating them into two groups:
 * 0) uninteristing wires and
 * 1) "short" Wires with exactly two diffusions
 * before releasing a short wire JemStratMergeSer attempts
 * to merge the transistors that it links.
 */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.jemNets.TransistorOne;
import com.sun.electric.tool.ncc.jemNets.TransistorTwo;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.strategy.JemSets;

//import java.util.Collection;
import java.util.Iterator;

public class JemStratMergeSer extends JemStratSome {

    private int numWires= 0;
    private int numShort= 0;
    private int numMerged= 0;
    private int numLeft= 0;

    private static int numCodes= 1;
    protected final static Integer CODE_WIRE= new Integer(0);

    public String nameString(){return "JemStratMergeSer";}

    private JemStratMergeSer(){}

	// ---------- to do the job -------------

	public static boolean doYourJob(JemSets jss){
		JemStratMergeSer jsms = new JemStratMergeSer();
		return jsms.doYourJob2(jss);
	}
	
	private boolean doYourJob2(JemSets jss){
		JemEquivRecord s= (JemEquivRecord)jss.noGates;
		JemEquivRecord pt= (JemEquivRecord)jss.parts;
        int tot= pt.maxSize();
		if(s == null) return false;
		doFor(s);
		if(pt.maxSize() < tot)return true;
		return false;
	}
	
    //do something before starting
    private void preamble(JemRecord j){
		if(getDepth() != 0)return;
		startTime("JemStratMergeSer" , j.nameString());
        numShort= 0;
        numWires= 0;
		numLeft= 0;
		numMerged= 0;
		return;
    } //end of preamble

    //summarize at the end
    private void summary(){
		if(getDepth() != 0)return;
		getMessenger().line("JemStratMergeSer formed " +
					  numMerged + " TransTwo, processing " +
					  numWires + " Wires = " +
					  numShort + " two-diff Wires, of which " +
					  numLeft + " remain.");
		elapsedTime(numWires);
		return;
    } //end of summary

    // ---------- for JemRecord -------------

    public JemEquivList doFor(JemRecord j){
		preamble(j);
		JemEquivList el= super.doFor(j);
		summary();
		//to indicate no change to this symmetry group
        return el;
    } //end of doFor

    // ---------- for JemCircuit -------------
	
    public JemCircuitMap doFor(JemCircuit g){
		JemCircuitMap mm= super.doFor(g);
        return mm;
    } //end of doFor
	
    //------------- for NetObject ------------

    public Integer doFor(NetObject n){
		if(n instanceof Wire){
			Wire w= (Wire)n;
			return doFor(w);
		} //end of wires
		else return null;
    } //end of doFor

    private Integer doFor(Wire n){
		//does it have exactly two diffusions?
		numWires++;
		int count= 0;
		Iterator it= n.iterator();
		while(it.hasNext()){
			Object oo= it.next();
			Part p= (Part)oo;
			if( ! (p instanceof TransistorOne))continue;
			TransistorOne t= (TransistorOne)p;
			if(t.touchesAtDiffusion(n))count++;
			else return CODE_WIRE; //must touch gate
			if(count > 2)return CODE_WIRE;
		} //end of while
		if(count != 2) return CODE_WIRE;
		//we've got a candidate
		numShort++;
		if(TransistorTwo.joinOnWire(n) != null){
			numMerged++;
			return null; //drop this wire
		} //end of made one
		numLeft++;
		return CODE_WIRE;
    } //end of doFor Wire

} //end of JemStratMergeSer

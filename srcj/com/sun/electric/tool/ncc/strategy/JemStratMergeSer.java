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
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.jemNets.TransistorOne;
import com.sun.electric.tool.ncc.jemNets.TransistorTwo;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.strategy.JemSets;

//import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;

public class JemStratMergeSer extends JemStrat {

    private int numWires;
    private int numShort;
    private int numLeft;
    private ArrayList toBeDeleted = new ArrayList();

    private static final int NUM_CODES= 1;

    private JemStratMergeSer(){}

	// ---------- to do the job -------------

	public static boolean doYourJob(JemSets jss){
		JemEquivRecord noGates= (JemEquivRecord)jss.noGates;
		if(noGates == null) return false;
		JemEquivRecord pt= (JemEquivRecord)jss.parts;
		int nmPartsBefore = pt.maxSize();
		JemStratMergeSer ms = new JemStratMergeSer();
		ms.preamble(noGates);
		ms.doFor(noGates);
		ms.summary();
		return pt.maxSize() < nmPartsBefore;
	}
	
    //do something before starting
    private void preamble(JemRecord j){
		startTime("JemStratMergeSer" , j.nameString());
    }

    //summarize at the end
    private void summary(){
    	for (int i=0; i<toBeDeleted.size(); i++) {
    		Wire w = (Wire) toBeDeleted.get(i); 
    		w.killMe();
    	}
		Messenger.line("JemStratMergeSer formed " +
					  toBeDeleted.size() + " TransTwo, processing " +
					  numWires + " Wires = " +
					  numShort + " two-diff Wires, of which " +
					  numLeft + " remain.");
		elapsedTime(numWires);
    }

    //------------- for NetObject ------------

    public Integer doFor(NetObject n){
		error(!(n instanceof Wire), "expecting only wires");
		return doFor((Wire)n);
    }

    private Integer doFor(Wire n){
		//does it have exactly two diffusions?
		numWires++;

		int count= 0;
		for(Iterator it=n.getParts(); it.hasNext();){
			Part p= (Part)it.next();
			if(!(p instanceof TransistorOne)) return CODE_NO_CHANGE;
			TransistorOne t= (TransistorOne)p;
			if(!t.touchesAtDiffusion(n)) return CODE_NO_CHANGE;
			count++;
			if(count > 2)return CODE_NO_CHANGE;
		}
		if(count != 2) return CODE_NO_CHANGE;
		//we've got a candidate
		numShort++;
		if(TransistorTwo.joinOnWire(n)){
			toBeDeleted.add(n);
			return CODE_NO_CHANGE; //drop this wire
		}
		numLeft++;
		return CODE_NO_CHANGE;
    }

}

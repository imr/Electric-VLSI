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
 * 0) uninteresting wires and
 * 1) "short" Wires with exactly two diffusions
 * before releasing a short wire JemStratMergeSer attempts
 * to merge the transistors that it links.
 */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.*;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.Transistor;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

//import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;

public class JemStratMergeSer extends JemStrat {

    private int numWires;
    private int numShort;
    private int numLeft;
    private ArrayList toBeDeleted = new ArrayList();

    private static final int NUM_CODES= 1;

    private JemStratMergeSer(NccGlobals globals) {super(globals);}

	// ---------- to do the job -------------

	public static boolean doYourJob(NccGlobals globals){
		JemEquivRecord noGates= globals.getWiresWithoutGates();
		if(noGates == null) return false;
		JemEquivRecord pt= globals.getParts();
		int nmPartsBefore = pt.maxSize();
		JemStratMergeSer ms = new JemStratMergeSer(globals);
		ms.preamble(noGates);
		ms.doFor(noGates);
		ms.summary();
		return pt.maxSize() < nmPartsBefore;
	}
	
    //do something before starting
    private void preamble(JemEquivRecord j){
		startTime("JemStratMergeSer" , j.nameString());
    }

    //summarize at the end
    private void summary(){
    	for (int i=0; i<toBeDeleted.size(); i++) {
    		Wire w = (Wire) toBeDeleted.get(i); 
    		w.killMe();
    	}
		globals.println("JemStratMergeSer performed " +
					  toBeDeleted.size() + " series merges, processing " +
					  numWires + " Wires = " +
					  numShort + " two-diff Wires, of which " +
					  numLeft + " remain.");
		elapsedTime();
    }

    //------------- for NetObject ------------

    public Integer doFor(NetObject n){
		error(!(n instanceof Wire), "expecting only wires");
		return doFor((Wire)n);
    }

    private Integer doFor(Wire w){
		//does it have exactly two diffusions?
		numWires++;

		// make sure there are no ports on wire
		if (w.getPort()!=null)  return CODE_NO_CHANGE;
		
		// wires declared GLOBAL in Electric aren't internal nodes of MOS stacks
		if (w.isGlobal()) return CODE_NO_CHANGE;
		
		int count= 0;
		for(Iterator it=w.getParts(); it.hasNext();){
			Part p= (Part)it.next();
			if(!(p instanceof Transistor)) return CODE_NO_CHANGE;
			Transistor t= (Transistor)p;
			if(!t.touchesAtDiffusion(w)) return CODE_NO_CHANGE;
			count++;
			if(count > 2)return CODE_NO_CHANGE;
		}
		if(count != 2) return CODE_NO_CHANGE;
		//we've got a candidate
		numShort++;
		if(Transistor.joinOnWire(w)){
			toBeDeleted.add(w);
			return CODE_NO_CHANGE;
		}
		numLeft++;
		return CODE_NO_CHANGE;
    }

}

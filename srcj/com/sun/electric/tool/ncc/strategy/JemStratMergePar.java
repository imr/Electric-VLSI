/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratMergePar.java
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
import com.sun.electric.tool.ncc.*;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.jemNets.Transistor;
import com.sun.electric.tool.ncc.jemNets.TransistorZero;
import com.sun.electric.tool.ncc.jemNets.TransistorOne;
import com.sun.electric.tool.ncc.jemNets.TransistorTwo;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * MergeParallel works by seeking a wire with many gates on it it then
 * sorts the transistors on that wire by hash of their names seeking
 * any that lie in the same hash bin it produces exactly one offspring
 * identical to its target
*/
public class JemStratMergePar extends JemStrat {

    //statistics to gather
    private int numTransCandidates= 0;
    private int numCapCandidates= 0;
    private int numTransOneDone= 0;
    private int numTransTwoDone= 0;
    private int numCapDone= 0;

    private JemStratMergePar(NccGlobals globals) {super(globals);}

	// ---------- to do the job -------------

	public static boolean doYourJob(NccGlobals globals){
		JemStratMergePar jsmp = new JemStratMergePar(globals);
		return jsmp.doYourJob2(globals);
	}
	private boolean doYourJob2(NccGlobals globals){
		JemEquivRecord pt = globals.getParts();
        int befSz= pt.maxSize();
		preamble(globals.getWiresWithGates());
		doFor(globals.getWiresWithGates());
		summary();
		return pt.maxSize() < befSz;
	}
	
    //do something before starting
    private void preamble(JemEquivRecord j){
		startTime("JemStratMergePar" , j.nameString());
    }

    //do at the end
    private void summary(){
		globals.println(" Parallel merged " +
					  numTransOneDone + " TransOne and " +
					  numTransTwoDone + " TransTwo of " +
					  numTransCandidates + " candidates and " +
					  numCapDone + " Capacitors of " +
					  numCapCandidates + " candidates");
		elapsedTime();
    }

	//------------- for NetObject ------------

    //Given a JemEquivRecord of Wires there won't be any Parts.
    public Integer doFor(NetObject n){
		error(!(n instanceof Wire), "JemStratMergePar expects only Wires");
		return doFor((Wire)n);
    }

    //process candidate transistors from one Wire
    private Integer doFor(Wire w){
        List transOne= new ArrayList(3);
        List transTwo= new ArrayList(3);
        List capacitors= new ArrayList(3);
		
		//the connections loop
		for(Iterator wi=w.getParts(); wi.hasNext();){
			Part p= (Part)wi.next();
			if(p instanceof Transistor){
				Transistor t= (Transistor)p;
				if(t.touchesAtGate(w)){
					if(t instanceof TransistorZero){
						capacitors.add(t);
						numCapCandidates++;
					}else if(t instanceof TransistorOne){
						if(((TransistorOne)t).isCapacitor()){
							capacitors.add(t);
							numCapCandidates++;
						}else{
							transOne.add(t);
							numTransCandidates++;
						}
					}else if(t instanceof TransistorTwo){
						transTwo.add(t);
						numTransCandidates++;
					}
				}
			}
		}
		//convert the capacitors
		capacitors= convert(capacitors);
		numCapDone+= processTransList(capacitors);
		numTransOneDone+= processTransList(transOne);
		numTransTwoDone+= processTransList(transTwo);
		return CODE_NO_CHANGE; // not enough to merge
    }

	//this converts TransistorOne capacitors into real capacitors
    private List convert(List s){
		List out= new ArrayList();
		for(Iterator it=s.iterator(); it.hasNext();){
			Object oo= it.next();
			if(oo instanceof TransistorOne){
				TransistorOne t1= (TransistorOne)oo;
				TransistorZero t0= TransistorZero.please(t1);
				oo= t0;
			}
			out.add(oo);
		}
		return out;
    }

    //process one candidate transistor list
    private int processTransList(List ttt){
		//ttt is a list of possible transistors
		HashMap h= new HashMap();
		
		for(Iterator it=ttt.iterator(); it.hasNext();){
			Transistor t= (Transistor)it.next();
			Integer code= t.computeNameCode();
			ArrayList list= (ArrayList) h.get(code);
			if (list==null) {
				list= new ArrayList();
				h.put(code,list);
			}
			list.add(t);
		}

		//hashmap contains the lists of candidates
		int numDone= 0;
		for(Iterator it=h.keySet().iterator(); it.hasNext();){
			ArrayList j= (ArrayList) h.get(it.next());
			numDone += Part.parallelMerge(j);
		}
		return numDone;
    }

}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratPrint.java
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
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Wire;

import java.util.Iterator;

/** 
 * JemStratPrint gives a limited print out of the tree
 * It prints not more than maxLines stuff for each list
 * and not more than maxPins in any one Wire
 */
public class JemStratPrint extends JemStratNone {

    // ---------- private data -------------

    // these integers set limits to the amount of output printing
    private static final int MAX_LINES= 2; //max number Parts or Wires to print
    private static final int MAX_PINS=3; //max number Pins printed for Wires

    public String nameString(){return "JemStratPrint";}

    private JemStratPrint(){}

	public static JemEquivList doYourJob(JemRecord j){
		JemStratPrint jsp = new JemStratPrint();
		jsp.preamble(j);
		JemEquivList el= jsp.doFor(j); //prints content and offspring if any
		jsp.summary();
		return el;
	}

    // ---------- the tree walking code ---------
    private void preamble(JemParent j){
    	startTime("JemStratPrint", j.nameString());
    }

	private void summary(){elapsedTime();}

	// ---------- for JemRecord -------------
	public JemEquivList doFor(JemRecord j){
		Messenger mm= getMessenger();
		mm.say("**  Depth= " + depth + " ");
		j.printMe(mm);
		return super.doFor(j);
	}

	// ---------- for JemCircuit -------------

	// has extra stuff to limit the amount of printing
	public JemCircuitMap doFor(JemCircuit j){
		JemCircuitMap cm;
		getMessenger().say("**  Depth= " + depth +
					 " " + j.nameString() +
					 " of " + j.size() +
					 " and code " + j.getCode());
		if(j.size() < MAX_LINES){
			getMessenger().line("");
			cm= super.doFor(j);
		}else{
			depth++;
			getMessenger().line(" starts with");
			int count= 0;
			Iterator it= j.iterator();
			while(it.hasNext() && (count < MAX_LINES)){
				Object x= it.next();
				NetObject n= (NetObject)x;
				doFor(n);
				count++;
			} //end of loop
			cm= JemCircuitMap.mapPlease(1);
			depth--;
		} //end of else
		return cm;
	} //end of doFor

	// ---------- for NetObject -------------

	public Integer doFor(NetObject n){
		n.printMe(MAX_PINS);
		return null;
	} //end of doFor NetObject

} //end of JemStratPrint

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
import java.util.HashMap;

/** 
 * JemStratPrint gives a limited print out of the tree
 * It prints not more than maxLines stuff for each list
 * and not more than maxPins in any one Wire
 */
public class JemStratPrint extends JemStrat {

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
    private void preamble(JemRecord j){
    	startTime("JemStratPrint", j.nameString());
    }

	private void summary(){elapsedTime();}

	// ---------- for JemRecord -------------
	public JemEquivList doFor(JemRecord j){
		Messenger.say("**  Depth= " + getDepth() + " ");
		j.printMe();
		return super.doFor(j);
	}

	// ---------- for JemCircuit -------------

	// has extra stuff to limit the amount of printing
	public HashMap doFor(JemCircuit j){
		Messenger.say("**  Depth= " + getDepth() +
					 " " + j.nameString() +
					 " of " + j.numNetObjs() +
					 " and code " + j.getCode());

		Messenger.line(j.numNetObjs()<MAX_LINES ? "" : " starts with");

		int count= 0;
		HashMap codeToNetObjs = new HashMap();
		for (Iterator it=j.getNetObjs(); it.hasNext();) {
			NetObject n= (NetObject) it.next();
			codeToNetObjs.put(CODE_NO_CHANGE, n);
			if (count<MAX_LINES) n.printMe(MAX_PINS);
			count++;
		}
		return codeToNetObjs;
	}

	// ---------- for NetObject -------------

	public Integer doFor(NetObject n){
		error(true, "should never get here");
		return CODE_ERROR;
	}

}

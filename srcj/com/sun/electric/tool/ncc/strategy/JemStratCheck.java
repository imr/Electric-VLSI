/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratCheck.java
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

import java.util.Iterator;
import java.util.HashMap;

public class JemStratCheck extends JemStrat {

    //these are variables that pass between levels of the tree
    private JemRecord recordParent;
    private JemCircuit circuitParent;

    private JemStratCheck(){}

	public static JemEquivList doYourJob(JemRecord j) {
		JemStratCheck jsc = new JemStratCheck();
		jsc.preamble(j);
		JemEquivList el = jsc.doFor(j);    	
		jsc.summary(j);
		return el;
	}

    // ---------- the tree walking code ---------

    //do something before starting
    private void preamble(JemRecord j){
		startTime("JemStratCheck", j.nameString());
    }

    //summarize at the end
    private void summary(JemRecord x){
        elapsedTime();
    }

    // ---------- for JemRecord -------------

    public JemEquivList doFor(JemRecord j){
    	j.checkMe(recordParent);
        JemRecord oldParent= recordParent; //save the old one
        recordParent= j;
		JemEquivList el= super.doFor(j);
        recordParent= oldParent;
        return el;
    }
    
    // ---------- for JemCircuit -------------

    public HashMap doFor(JemCircuit j){
        j.checkMe((JemEquivRecord)recordParent);
        circuitParent= j;
        return super.doFor(j);
    }

    // ---------- for NetObject -------------

    public Integer doFor(NetObject n){
    	n.checkMe(circuitParent);
        return CODE_NO_CHANGE;
    }

}

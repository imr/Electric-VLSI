/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemHashPartAll.java
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

import java.util.Iterator;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

/* JemStratRandomMatch arbitrarily matches the NetObjects that are first in
 * each JemCircuit. */
public class JemStratRandomMatch extends JemStrat {
	private static final Integer CODE_FIRST = new Integer(1);
	private static final Integer CODE_REST = new Integer(2);
    private JemStratRandomMatch(NccGlobals globals){super(globals);}
	
	public static JemLeafList doYourJob(JemRecordList l,
	                                    NccGlobals globals){
		JemStratRandomMatch rm = new JemStratRandomMatch(globals);

		JemLeafList offspring = rm.doFor(l);
		return offspring;
	}
	
    //------------- for NetObject ------------
	
    public Integer doFor(NetObject n){
		JemCircuit ckt = n.getParent();
		Iterator ni = ckt.getNetObjs();
		Object first = ni.next();
		return n==first ? CODE_FIRST : CODE_REST;
    }
}

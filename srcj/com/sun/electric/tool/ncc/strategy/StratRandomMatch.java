/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratRandomMatch.java
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
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

/** StratRandomMatch arbitrarily matches the NetObjects that are first in
 * each Circuit. */
public class StratRandomMatch extends Strategy {
	private static final Integer CODE_FIRST = new Integer(1);
	private static final Integer CODE_REST = new Integer(2);
    private StratRandomMatch(NccGlobals globals){super(globals);}
	
	public static LeafList doYourJob(RecordList l,
	                                    NccGlobals globals){
		StratRandomMatch rm = new StratRandomMatch(globals);

		LeafList offspring = rm.doFor(l);
		return offspring;
	}
	
    //------------- for NetObject ------------
	
    public Integer doFor(NetObject n){
		Circuit ckt = n.getParent();
		Iterator ni = ckt.getNetObjs();
		Object first = ni.next();
		return n==first ? CODE_FIRST : CODE_REST;
    }
}

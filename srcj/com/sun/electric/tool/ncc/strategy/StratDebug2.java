/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratDebug2.java
 *
 * Copyright (c) 2003 Sun Microsystems and Free Software
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

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.jemNets.*;

/**
 * StratDebug performs the debugging function of the day.
*/
public class StratDebug2 extends Strategy {
	// Constructor does everything
	private StratDebug2(NccGlobals globals) {
		super(globals);
		NccOptions options = globals.getOptions();
		boolean savedVerbose = options.verbose;
		options.verbose = true;
		
		globals.println("begin search for RK stuff");
		doFor(globals.getRoot());
		globals.println("end search for RK stuff");
		
		options.verbose = savedVerbose;
	}
	
	private boolean hasRKstuff(Circuit ckt) {
		for (Iterator it=ckt.getNetObjs(); it.hasNext();) {
			NetObject no = (NetObject) it.next();
			String name = no.getName();
			if (name.indexOf("/rks_")!=-1) return true;
			if (name.indexOf("/rkl_")!=-1) return true;
		}
		return false;
	}
	
	private boolean hasRKstuff(EquivRecord er) {
		for (Iterator it=er.getCircuits(); it.hasNext();) {
			Circuit ckt = (Circuit) it.next();
			if (hasRKstuff(ckt)) return true;			
		}
		return false;
	}

    // ---------- the tree walking code ---------
	public LeafList doFor(EquivRecord er) {
		if (er.isLeaf()) {
			if (hasRKstuff(er)) {
				globals.println(er.nameString());
				List reasons = er.getPartitionReasonsFromRootToMe();
				for (Iterator it=reasons.iterator(); it.hasNext();) {
					globals.println("   "+it.next());
				}
				super.doFor(er);
			}
		} else {
			super.doFor(er);
		}
		return new LeafList();
	}
	
	public HashMap doFor(Circuit c) {
		globals.println(" "+c.nameString());
		return super.doFor(c);
	}

    public Integer doFor(NetObject n){
		globals.println("  "+n.toString());
        return CODE_NO_CHANGE;
    }
    
    // -------------------------- public method -------------------------------
	public static void doYourJob(NccGlobals globals) {
		new StratDebug2(globals);
	}
}

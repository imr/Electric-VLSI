/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratResult.java
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

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.jemNets.NetObject;
import com.sun.electric.tool.ncc.trees.EquivRecord;
import com.sun.electric.tool.ncc.lists.LeafList;

public class StratResult extends Strategy {
	// ------------------------ private data and methods ----------------------
	private boolean match = true;

	private StratResult(NccGlobals globals) {super(globals);}
	
	private boolean match() {
		doFor(globals.getRoot());
		return match;
	}
	
	// -------------------------- walk tree -----------------------------------
	public LeafList doFor(EquivRecord r) {
		if (r==globals.getPorts()) {
			return new LeafList();
		} else if (r.isLeaf()) {
			if (r.isActive() || r.isMismatched())  match = false;
			return new LeafList();
		} else {
			return super.doFor(r);
		}
	}

	public Integer doFor(NetObject n) {return CODE_NO_CHANGE;}

	// --------------------------- real public method -------------------------	
	/**
	 * Walk EquivRecord tree to find active and mismatched leaf records
	 * @param globals NccGlobals
	 * @return true if matched (all leaf records retired)
	 */
	public static boolean doYourJob(NccGlobals globals) {
		StratResult jsr = new StratResult(globals);
		return jsr.match();		
	}
}

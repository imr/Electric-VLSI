/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratDebug.java
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
import java.util.List;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/**
 * StratDebug performs the debugging function of the day.
*/
public class StratDebug extends Strategy {
	// Constructor does everything
	private StratDebug(NccGlobals globals) {
		super(globals);
		NccOptions options = globals.getOptions();
		int saveHowMuchStatus = options.howMuchStatus;
		options.howMuchStatus = 10;
		
		globals.status2("begin StratDebug");
		globals.status2("dumping mismatched EquivRecords");
		doFor(globals.getRoot());
		globals.status2("end StratDebug");
		
		options.howMuchStatus = saveHowMuchStatus;
	}
	
    // ---------- the tree walking code ---------
	public LeafList doFor(EquivRecord er) {
		if (er.isLeaf()) {
			if (!er.isBalanced() && er.getNetObjType()!=Part.Type.PORT) {
				globals.status2(er.nameString());
				List<String> reasons = er.getPartitionReasonsFromRootToMe();
				for (String s : reasons) {
					globals.status2("   "+s);
				}
				super.doFor(er);
			}
		} else {
			super.doFor(er);
		}
		return new LeafList();
	}
	
	public HashMap<Integer,List<NetObject>> doFor(Circuit c) {
		globals.status2(" "+c.nameString());
		return super.doFor(c);
	}

    /** 
	 * 
	 */
    public Integer doFor(NetObject n){
		globals.status2("  "+n.fullDescription());
        return CODE_NO_CHANGE;
    }
    
    // -------------------------- public method -------------------------------
	public static void doYourJob(NccGlobals globals) {
		new StratDebug(globals);
	}
}

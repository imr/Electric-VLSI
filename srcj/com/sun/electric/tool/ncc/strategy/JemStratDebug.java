
package com.sun.electric.tool.ncc.strategy;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.jemNets.*;

/**
 * JemStratDebug performs the debugging function of the day.
*/
public class JemStratDebug extends JemStrat {
	// Constructor does everything
	private JemStratDebug(NccGlobals globals) {
		super(globals);
		globals.println("begin JemStratDebug");
		globals.println("dumping all leaf JemEquivRecords of size 2");
		doFor(globals.getRoot());
		globals.println("end JemStratDebug");
	}

    // ---------- the tree walking code ---------
	public JemLeafList doFor(JemEquivRecord er) {
		if (er.isLeaf()) {
			if (er.isActive()) {
				globals.println("Print JemEquivRecord:");
				super.doFor(er);
			}
		} else {
			super.doFor(er);
		}
		return new JemLeafList();
	}
	
	public HashMap doFor(JemCircuit c) {
		globals.println(" Print JemCircuit");
		return super.doFor(c);
	}

    /** 
	 * 
	 */
    public Integer doFor(NetObject n){
		globals.println("  "+n.toString());
        return CODE_NO_CHANGE;
    }
    
    // -------------------------- public method -------------------------------
	public static void doYourJob(NccGlobals globals) {
		new JemStratDebug(globals);
	}
}

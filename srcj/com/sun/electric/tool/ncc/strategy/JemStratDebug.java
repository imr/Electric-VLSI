
package com.sun.electric.tool.ncc.strategy;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.NccOptions;
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
		NccOptions options = globals.getOptions();
		boolean savedVerbose = options.verbose;
		options.verbose = true;
		
		globals.println("begin JemStratDebug");
		globals.println("dumping active and mismatched JemEquivRecords");
		doFor(globals.getRoot());
		globals.println("end JemStratDebug");
		
		options.verbose = savedVerbose;
	}

    // ---------- the tree walking code ---------
	public JemLeafList doFor(JemEquivRecord er) {
		if (er.isLeaf()) {
			if (er.isActive() || er.isMismatched()) {
				globals.println(er.nameString());
				super.doFor(er);
			}
		} else {
			super.doFor(er);
		}
		return new JemLeafList();
	}
	
	public HashMap doFor(JemCircuit c) {
		globals.println(" "+c.nameString());
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

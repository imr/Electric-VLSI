package com.sun.electric.tool.ncc.strategy;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.trees.NetObject;
import com.sun.electric.tool.ncc.trees.JemEquivRecord;
import com.sun.electric.tool.ncc.lists.JemLeafList;

public class JemStratResult extends JemStrat {
	// ------------------------ private data and methods ----------------------
	private JemLeafList mismatched, active;

	// constructor does all the work
	private JemStratResult(JemLeafList mismatched, JemLeafList active,
						   NccGlobals globals) {
		super(globals);
		this.mismatched = mismatched;
		this.active = active;
		mismatched.clear();
		active.clear();
		doFor(globals.getRoot());
	}
	
	// -------------------------- walk tree -----------------------------------
	public JemLeafList doFor(JemEquivRecord r) {
		if (r==globals.getPorts()) {
			return new JemLeafList();
		} else if (r.isLeaf()) {
			if (r.isActive()) {
				active.add(r);	
			} else if (r.isMismatched()) {
				mismatched.add(r);
			}
			return new JemLeafList();
		} else {
			return super.doFor(r);
		}
	}

	public Integer doFor(NetObject n) {return CODE_NO_CHANGE;}

	// --------------------------- real public method -------------------------	
	/**
	 * Walk JemEquivRecord tree to find active and mismatched leaf records
	 * @param mismatched returned ist of mismatched leaf records 
	 * @param active returned list of active leaf records 
	 * @param globals NccGlobals
	 * @return true if matched (all leaf records retired)
	 */
	public static boolean doYourJob(JemLeafList mismatched, JemLeafList active,
						  			NccGlobals globals) {
		new JemStratResult(mismatched, active, globals);
		return mismatched.size()==0 && active.size()==0;		
	}
}
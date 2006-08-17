/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Strategy.java
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
//  Updated 31 October to attach to a SymmetryGroup
//updated 25 January 2004 (and preceeding week) to remove iteration.
//the iteration is now all in the tree structure

package com.sun.electric.tool.ncc.strategy;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.lists.RecordList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/** 
 * Strategy is the superclass for all strategies.
 * The Strategy classes implement a doFor method for each
 * level of the EquivRecord tree: 
 *     Integer doFor(NetObject)
 * Each of these does a call-back to the apply(Strategy) method of its class
 *    which actually computes the appropriate answer, perhaps by doFor calls,
 *    for each of its children, accumulating the answers.
 * Strategy keeps track of the depth in the tree, 
 *    giving reports when depth == 0.  
 * Because of that, calls to Strategy.doFor(Record) 
 *    produce a single combined report, but
 *    calls to Record.apply(Strategy) produce multiple reports.
 */
public abstract class Strategy {
	// ---------------------------- constants ---------------------------------
	public static final Integer CODE_ERROR = null;
    public static final Integer CODE_NO_CHANGE = new Integer(0);

    // --------------- local variables -------------------
    protected int depth; //depth in the tree
    protected int getDepth(){return depth;}
	public NccGlobals globals;	
    private Date theStartDate= null;
	
    /** Die if error occurs
     * @param pred true if error occurs
     * @param msg message to print if error occurs */
    public void error(boolean pred, String msg) {
    	if (globals==null) {
    		LayoutLib.error(pred, msg);
    	} else {
    		globals.error(pred, msg);
    	}
    }
	
    /** Simple stratgies may pass in null for globals. But they can't
     * call startTime, elapsedTime, or pickAnOffspring */
    protected Strategy(NccGlobals globals) {this.globals=globals;}

	private LeafList apply(Iterator<EquivRecord> it){
		LeafList out= new LeafList();
		while (it.hasNext()) {
			EquivRecord jr= it.next();
			out.addAll(doFor(jr));
		}
		return out;
	}
	
	private LeafList apply(RecordList r) {
		return apply(r.iterator());
	}
    
	/** Apply this Strategy to a list of leaf and internal records.
	 * @param r a RecordList of EquivRecords to process
	 * @return a LeafList of the new leaf EquivRecords */
    public LeafList doFor(RecordList r) {return doFor(r.iterator());}
	
    public LeafList doFor(Iterator<EquivRecord> it) {
    	depth++;
    	LeafList out = apply(it);
    	depth--;
    	return out;
    }
	
	/** Method doFor(EquivRecord) processes a single EquivRecord.
	 * @param rr the EquivRecord to process
	 * @return a LeafList of the new leaf EquivRecords */	
    public LeafList doFor(EquivRecord rr){
		depth++;
		LeafList out = rr.apply(this);
		depth--;
        return out;
    }

    /** Method doFor(Circuit) process a single Circuit,
	 * dividing the circuit according to this strategy, and 
	 * placing the NetObjects of the Circuit into new Circuits 
	 * mapped in the return according to the separation Integer. 
	 * @param c the Circuit to process.
	 * @return a CircuitMap of offspring Circuits.
	 * Returns an empty map if no offspring intended, and
	 * returns the input input Circuit if method fails to split. */
    public HashMap<Integer,List<NetObject>> doFor(Circuit c){
		depth++;
		HashMap<Integer,List<NetObject>> codeToNetObjs = c.apply(this);
		depth--;
		return codeToNetObjs;
	}

    /**  doFor(NetObject) tests the NetObject to decide its catagory.
	 * The default method generates no offspring.
	 * @param n the NetObject to catagorize
	 * @return an Integer for the choice. */
    public Integer doFor(NetObject n) {return CODE_NO_CHANGE;}

	//comments on the "code"th offspring of g
	EquivRecord pickAnOffspring(Integer code, LeafList g, String label) {
		int value = code.intValue();
		for(Iterator<EquivRecord> it=g.iterator(); it.hasNext();){
			EquivRecord er = it.next();
			if(er.getValue()==value){
				globals.status2(label+": "+ er.nameString());
				return er;
			}
		}
		//falls out if not found
		globals.status2(label+": none");
		return null;
	}

	protected String offspringStats(LeafList el) {
		int matched=0, mismatched=0, active=0;
		for (Iterator<EquivRecord> it=el.iterator(); it.hasNext();) {
			EquivRecord er = it.next();
			if (er.isMismatched())  mismatched++;
			else if (er.isMatched())  matched++;
			else active++;
		}
		String msg = "  offspring counts: #matched="+matched+
			         " #mismatched="+mismatched+" #active="+active;
//		if (mismatched!=0) {
//			globals.status1(msg);
//			//StrategyPrint.doYourJob(globals.getRoot(), globals);
//			StratDebug.doYourJob(globals);
//			globals.error("mismatched!");
//			return null;		        		
//		}
		return msg;
	}
	
    protected void startTime(String strat, String target){
        theStartDate= new Date();
        globals.status2((globals.passNumber++)+" "+
					   strat + " doing " + target);
    }

    protected long elapsedTime(){
        Date d= new Date();
        long time= d.getTime() - theStartDate.getTime();
        globals.status2(" took " + time + " miliseconds\n");
        return time;
    }

}

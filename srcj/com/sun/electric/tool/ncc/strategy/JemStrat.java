/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStrat.java
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
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.basicA.Messenger;

import java.util.Iterator;
import java.util.Date;
import java.util.HashMap;

/** 
 * JemStrat is the superclass for all strategies.
 * The JemStrat strategy classes implement a doFor method for each
 * level of the JemTree hierarchy: 
 *     JemEquivList doFor(JemRecordList)
 *     JemEquivList doFor(JemRecord)
 *     JemCircuitMap doFor(JemCircuit)
 *     Integer doFor(NetObject)
 * Each of these does a call-back to the apply(JemStrat) method of its class
 *    which actually computes the appropriate answer, perhaps by doFor calls,
 *    for each of its children, accumulating the answers.
 * JemStrat keeps track of the depth in the tree, 
 *    giving reports when depth == 0.  
 * Because of that, calls to JemStrat.doFor(Record) 
 *    produce a single combined report, but
 *    calls to Record.apply(JemStrat) produce multiple reports.
 */
public abstract class JemStrat {
	// --------------- local variables -------------------
	
    protected int depth; //depth in the tree
    protected int getDepth(){return depth;}
	
    private Date theStartDate= null;
	
    private static int passNumber= 1; //an index of strategy steps

	public static final Integer CODE_ERROR = null;
    public static final Integer CODE_NO_CHANGE = new Integer(0);
	
    /**
     * Die if error occurs
     * @param pred true if error occurs
     * @param msg message to print if error occurs
     */
    public static void error(boolean pred, String msg) {
    	if (pred) Messenger.error(msg);
    }
	
    protected JemStrat(){}

	/** 
	 * Method doFor(JemRecordList) processes a list of JemRecords.
	 * @param  a JemRecordList of JemRecords to process
	 * @return a JemEquivList of the new frontier JemEquivRecords
	 */
    public JemEquivList doFor(JemRecordList r){
		depth++;
		JemEquivList out= r.apply(this);
		depth--;
        return out;
    }
	
	
	/** 
	 * Method doFor(JemRecord) processes a single JemRecord.
	 * @param  the JemRecords to process
	 * @return a JemEquivList of the new frontier JemEquivRecords
	 */	
    public JemEquivList doFor(JemRecord rr){
		depth++;
		JemEquivList out= rr.apply(this);
		depth--;
        return out;
    }

    /** 
	 * Method doFor(JemCircuit) process a single JemCircuit,
	 * dividing the circuit according to this strategy, and 
	 * placing the NetObjects of the JemCircuit into new JemCircuits 
	 * mapped in the return according to the separation Integer. 
	 * @param  the JemCircuit to process.
	 * @return a JemCircuitMap of offspring JemCircuits.
	 * Returns an empty map if no offspring intended, and
	 * returns the input input JemCircuit if method fails to split.
	 */
    public HashMap doFor(JemCircuit c){
		depth++;
		HashMap codeToNetObjs = c.apply(this);
		depth--;
		return codeToNetObjs;
	}

    /** 
	 * doFor(NetObject) tests the NetObject to decide its catagory.
	 * @param  The NetObject to catagorize
	 * @return an Integer for the choice, or null to drop this NetObject.
	 */
    public abstract Integer doFor(NetObject n);

	//comments on the "code"th offspring of g
	protected JemEquivRecord pickAnOffspring(Integer code, JemEquivList g,
											 String ss){
		int value= code.intValue();
		for(Iterator it=g.iterator(); it.hasNext();){
			JemEquivRecord ch= (JemEquivRecord)it.next();
			if(ch.getValue() == value){
				Messenger.line(ch.nameString() + " of "
									+ ch.maxSize() + " " + ss);
				return ch;
			}
		}
		//falls out if not found
		Messenger.line("without " + ss);
		return null;
	}

	/**
	 * Get the parent of the 0th JemEquivRecord
	 * @param el list of JemEquivRecords
	 * @return parent of 0th JemEquivRecord or null if el is empty
	 */
	protected JemRecord getOffspringParent(JemEquivList el){
		if (el.size()==0) return null;
		JemEquivRecord er = (JemEquivRecord) el.get(0);
		return (JemRecord) er.getParent();
	}
	
	protected String offspringStats(JemEquivList el) {
		int retired=0, mismatched=0, active=0;
		for (Iterator it=el.iterator(); it.hasNext();) {
			JemEquivRecord er = (JemEquivRecord) it.next();
			if (er.isMismatched())  mismatched++;
			else if (er.isRetired())  retired++;
			else active++;
		}
		return " yielding "+mismatched+" mismatched "+retired+" retired "+
		       active+" active JemEquivRecords";
	}
	
    protected void startTime(String strat, String target){
        theStartDate= new Date();
        passNumber++;
        String sn;
        sn= passNumber + " ";
        String sss= sn + strat + " doing " + target;
        Messenger.line(sss);
    }

    protected long elapsedTime(){
        Date d= new Date();
        long time= d.getTime() - theStartDate.getTime();
        Messenger.line(" took " + time + " miliseconds");
        Messenger.freshLine();
        return time;
    }

    protected long elapsedTime(int actions){
        Date d= new Date();
        long time= d.getTime() - theStartDate.getTime();
        float average= 0;
        if(actions > 0)average= (float)time * 1000 / (float)actions;
        Messenger.say(" took " +
                        time + " miliseconds to do " +
                        actions + " actions: average action time is ");
        if(actions > 0) Messenger.line(average + " microseconds");
        else Messenger.line("uncertain");
        Messenger.freshLine();
        return time;
    }


}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemHistoryRecord.java
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

/** 
 * The JemHistory class records the past history of processing as a
 * tree.  The leaf nodes of the tree are JemEquivRecords.  The root of
 * the tree is a JemHistroyRecord with null parent.  The root of the
 * tree is sometimes called the "starter."
 */
package com.sun.electric.tool.ncc.trees;
import com.sun.electric.tool.ncc.strategy.JemStrat;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.basicA.Messenger;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public class JemHistoryRecord extends JemRecord {
	private List content = new ArrayList();

	/** private constructor for a JemhistoryRecord */
    private JemHistoryRecord(JemEquivRecord lameDuck,
    						 JemEquivList offspring) {
		error(offspring.size()<=1, "don't need History Record");
    	setParent(lameDuck.getParent());
    	if (getParent()!=null) {
    		getParent().replaceEquivRecord(lameDuck, this);
    	}
    	nominalCode = lameDuck.getCode();
		workDone = lameDuck.workDone;
		value = lameDuck.value;

		for (Iterator it=offspring.iterator(); it.hasNext();) {
			adopt((JemEquivRecord)it.next());
		}
    }

	/**
	 * A factory method to make a JemHistoryRecord holding several
	 * JemEquivRecords
	 * @param lameDuck the JemEquivRecord that is about to be 
	 * replaced by the new JemHistoryRecord
	 * @param offspring the children of the new JemHistoryRecord
	 * @return null if no offspring or a JemHistoryRecord with those
	 * offspring
	 */
    public static JemHistoryRecord please(JemEquivRecord lameDuck,
    									  JemEquivList offspring){
        return new JemHistoryRecord(lameDuck, offspring);
    }
    
    private void replaceEquivRecord(JemEquivRecord oldRec, JemHistoryRecord newRec) {
    	for (int i=0; i<content.size(); i++) {
    		 JemRecord r = (JemRecord) content.get(i);
    		 if (r==oldRec) {
    		 	content.set(i, newRec);
    		 	return;
    		 }
    	}
    	error(true, "can't find old JemEquivRecord");
    }

	public Iterator getChildRecs() {return content.iterator();}
	public int numChildRecs() {return content.size();}
	public void adopt(JemRecord r) {
		content.add(r);
		r.setParent(this);
	}

	/** 
	 * The apply method applies a JemStrat to this JemHistoryRecord.
	 * @param js the JemStrat to apply
	 * @return a JemEquivList of the resulting offspring
	 */
	public JemEquivList apply(JemStrat js){
		JemEquivList out= new JemEquivList();
		for (Iterator it=getChildRecs(); it.hasNext();) {
			JemRecord jr= (JemRecord) it.next();
			out.addAll(js.doFor(jr));
		}
		return out;		
	}
	
	/** 
	 * findSmallestOffspring searches recursively for the
	 * JemEquivRecord with fewest NetOjbects in its circuits.
	 * @return the JemEquivRecord with the fewest NetObjects
	 */
    public JemEquivRecord findSmallestOffspringIn(){
        int bestSize= 0;
        JemEquivRecord theBest= null;
        for (Iterator it=getChildRecs(); it.hasNext();) {
            JemEquivRecord good;
            Object oo= it.next();
            if(oo instanceof JemHistoryRecord){
                JemHistoryRecord hr= (JemHistoryRecord)oo;
                good= hr.findSmallestOffspringIn(); //recursive call
            } else if(oo instanceof JemEquivRecord){
                good = (JemEquivRecord)oo;
            } else {
                return null; //should never happen
            } //end of else
            int goodSize= good.maxSize();
            if((theBest == null) || (goodSize < bestSize)){
                bestSize= goodSize;
                theBest= good;
            } //end of if better
        } //end of while
        return theBest;
    }

	/**
	 * findSmallest searches recursively for the JemEquivRecord with
	 * fewest NetObjects in its circuits.
	 * @param c the JemRecordList to search
	 * @return the JemEquivRecord with fewest NetObjects in its circuits
	 */
    public static JemEquivRecord findSmallestIn(JemRecordList c){
        int bestSize= 0;
        JemEquivRecord theBest= null;
        Iterator it= c.iterator();
        while(it.hasNext()){
            JemEquivRecord good;
            Object oo= it.next();
            if(oo instanceof JemHistoryRecord){
                JemHistoryRecord hr= (JemHistoryRecord)oo;
                good= hr.findSmallestOffspringIn();
            } else {
            	error(!(oo instanceof JemEquivRecord), "bad JemRecord");
                good = (JemEquivRecord)oo;
            }
            int goodSize= good.maxSize();
            if((theBest == null) || (goodSize < bestSize)){
                bestSize= goodSize;
                theBest= good;
            }
        }
        return theBest;
    }

/*
    public static JemRecordList findTheLeaves(JemRecord x){
		if(x == null)return null;
        JemRecordList out= new JemRecordList(10);
        if(x instanceof JemEquivRecord){
            out.add(x);
        } else {
            JemHistoryRecord h= (JemHistoryRecord)x;
            out.addAll(h.findTheLeaves());
        } //end of else
        return out;
    } //end of findTheLeaves

	/**
	* @param
	 * @return
    public JemRecordList findTheLeaves(){
        JemRecordList out= new JemRecordList(10);
        Iterator it= iterator();
        while(it.hasNext()){
            Object oo= it.next();
            if(oo instanceof JemEquivRecord){
                out.add((JemEquivRecord)oo);
            } else if(oo instanceof JemHistoryRecord){
                JemHistoryRecord h= (JemHistoryRecord)oo;
                out.addAll(h.findTheLeaves());
            } //end of else
        } //end of while
        return out;
    } //end of findTheLeaves
	 */

	/**
	 * nameString returns a String of type and name for this JemParent.
 	 * @return a String identifying this JemTree object.
 	 */
	public String nameString(){
        return "JemHistoryRecord " + nominalCode +
			   " value= " + value;
    }
	
	/** 
	 * The printMe method prints this JemJistoryRecord on a given
	 * Messenger
	 */
	public void printMe(){
		Messenger.line(nameString() + 		
				" with " + numChildRecs() + " offspring");
	}
	
}

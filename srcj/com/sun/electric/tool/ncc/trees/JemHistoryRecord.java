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

public class JemHistoryRecord extends JemRecord {

	/** private constructor for a JemhistoryRecord */
    private JemHistoryRecord(){}

	/** 
	 *Please is a factory JemHistoryRecords taking over a previous
	 * JemEquivRecord.
	 * @param e the JemEquivRecord whose place is to be taken
	 * @return the fresh JemHistoryRecord
	 */
	public static JemHistoryRecord please(JemEquivRecord e){
		JemHistoryRecord h= new JemHistoryRecord();
		return h.historize(e);
	}

	/**
	 * A factory method to make a JemHistoryRecord holding several
	 * JemEquivRecords
	 * @param r a JemEquivList containing the offspring
	 * JemEquivRecords
	 * @return null if no offspring or a JemHistoryRecord with those
	 * offspring
	 */
    public static JemHistoryRecord please(JemEquivList r){
        if((r == null)||(r.size() == 0))return null;
        JemHistoryRecord h= new JemHistoryRecord();
        Iterator it= r.iterator();
        while(it.hasNext()){
            Object oo= it.next();
            JemEquivRecord e= (JemEquivRecord)oo;
            h.adopt(e);
        } //end of while
        return h;
    } //end of please

	/**
	 * The historize method converts a JemEquivRecord into this
	 * JemHistory record
	 * @param the JemEquivRecord to historize
	 * @return this JemHistoryRecord
	 */
    public JemHistoryRecord historize(JemEquivRecord e){
        JemHistoryRecord p= (JemHistoryRecord)e.getParent();
        if(p != null){
            p.remove(e);
            p.adopt(this);
        } //end of not null
		nominalCode= e.getCode();
		copyAndKill(e);
		return this;
    } //end of absorb

	/** 
	 * checkChild checks that a proposed JemChild is a JemRecord.
	 * @param the JemChild to test
	 * @return true if the JemChild is an OK class, false otherwise
	 */
	public boolean checkChild(JemChild c){
		if(c instanceof JemRecord)return true;
		else{
			getMessenger().error("bad child class in " + nameString());
			return false;
		} //end of else
	} //end of checkChild

	/** 
	 * The apply method applies a JemStrat to this JemHistoryRecord.
	 * @param the JemStrat to apply
	 * @return a JemEquivList of the resulting offspring
	 */
	public JemEquivList apply(JemStrat js){
		JemEquivList out= new JemEquivList();
		Iterator it= iterator();
		while(it.hasNext()){
			Object oo= it.next();
			JemRecord jr= (JemRecord)oo;
			JemEquivList xx= js.doFor(jr);
			out.addAll(xx);
		} //end of while
		return out;		
	} //end of apply
	
	/** 
	 * findSmallestOffspring searches recursively for the
	 * JemEquivRecord with fewest NetOjbects in its circuits.
	 * @return the JemEquivRecord with the fewest NetObjects
	 */
    public JemEquivRecord findSmallestOffspringIn(){
        int bestSize= 0;
        JemEquivRecord theBest= null;
        Iterator it= iterator();
        while(it.hasNext()){
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
    } //end of findSmallestOffspring

	/**
	 * findSmallest searches recursively for the JemEquivRecord with
	 * fewest NetObjects in its circuits.
	 * @param the JemRecordList to search
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
    } //end of findSmallest

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

	/** nameString returns a String of type and name for this JemParent.
 * @return a String identifying this JemTree object.
 */
	public String nameString(){
        String s= "JemHistoryRecord " + nominalCode +
			" value= " + value;
        return s;
    } //end of nameString
	
	/** 
	 * The printMe method prints this JemJistoryRecord on a given
	 * Messenger
	 * @param the Messenger to use
	 */
	public void printMe(Messenger mm){
		mm.line(nameString() + 		
				" with " + size() + " offspring");
		return;
	} //end of printMe
	
} //end of JemHistoryRecord

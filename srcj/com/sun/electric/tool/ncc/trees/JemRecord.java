/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemRecord.java
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
// updated to new view of trees, 16 Jan 2004
// Annotated by Ivan Sutherland, 30 January 2004

package com.sun.electric.tool.ncc.trees;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.strategy.JemStrat;
import com.sun.electric.tool.ncc.basicA.Messenger;

import java.util.List;
import java.util.Iterator;

/** 
 * JemRecord is the super class for JemEquivRecord and
 * JemHistoryRecord.  JemRecords were formerly called SymmetryGroups.
 * JemRecord is both a JemParent and a JemChild.  Every JemRecord is
 * assigned a pseudo random code at birth which it retains for life.
 */
public abstract class JemRecord extends JemParent implements JemChild{
	/** points toward the tree root */ private JemHistoryRecord myParent= null;

	/** the fixed randoom code */ protected int nominalCode;
    /** the number of processing passes invested */ //protected int passes;
    /** an estimate of the work invested */ protected int workDone;
	/** the int that distinguished this Record */ protected int value;

	/** constructor for JemRecord */
	protected JemRecord(){
        myParent= null;
        workDone= 0;
    }
	
	// ------ The JemParent methods
	
   	/**
	 * nameString returns a String of type and name for this
	 * JemRecord.
	 * @return a String identifying this JemTree object.
	 */
	public abstract String nameString();
	
	/** 
	 * getCode returns the fixed int hash code for this object.
	 * @return the int fixed hash code for this object.
	 */
	public int getCode(){return nominalCode;}

	/** 
	 * checkChild checks that a proposed JemChild is of the proper class.
	 * @param the JemChild to test
	 * @return true if the JemChild is an OK class, false otherwise
	 */
	protected abstract boolean checkChild(JemChild c);
	
	// ----- the JemChild interface
	
	/** 
	 * getParent fetches the parent JemParent towards the tree's root.
	 * @return the parent of this JemChild, or null for the root.
	 */
    public JemParent getParent(){return myParent;}

	/** 
	 * setParent checks the proposed parent's class before writing it.
	 * @param the JemParent proposed
	 * @return true if parent was accepted, false otherwise
	 */
	public boolean setParent(JemParent x){
		if(checkParent(x)){
			myParent= (JemHistoryRecord)x;
			return true;
		} else {
		getMessenger().error("wrong class parent in " + nameString());
			return false;
		} //end of else
	} //end of setParent
	
	/** checkParent tests a proposed JemParent for inclusion in this.
		* @param the JemParent to test
		* @return true if the JemParent is acceptable, false otherwise
		*/
	public boolean checkParent(JemParent p){
		if(p instanceof JemRecord)return true;
		else {
			getMessenger().error("wrong class parent in " + nameString());
			return false;
		} //end of if
	} //end of checkParent

	/** 
	 * The apply method does a JemStrat strategy in this JemRecord
	 * @param the JemStrat to apply to this JemRecord
	 * @return the JemEquivList of offspring that result, empty if none
	 */
	public abstract JemEquivList apply(JemStrat s);
	
	/** 
	 *  a routine to print this JemRecord on a Messenger.
	 * @param the Messenger to use for output
	 */
	public abstract void printMe(Messenger mm);
	
	/** 
	 * access method for workDone.
	 * @return an estimate of the work done on this JemRecord
	 */	
	public int getWorkDone(){return workDone;}
	
	/** 
	 * getValue fetches the value that a strategy used to distinguish
	 * this JemRecord.
	 * @return the int value that distinguished this JemRecord
	 */
	public int getValue(){return value;}

	/** 
	 * copyAndDump transfers the value parameters from another record
	 * to this one.
	 * @param the source of the values
	 */
	protected void copyAndKill(JemRecord e){
		workDone= e.workDone;
		value= e.value;
		e.killMe();
	} //end of copyValue

	/** killMe destroys a JemRecord by nulling all its pointers */
	protected void killMe(){
		if(myParent != null){
			myParent.remove(this);
		} //end of if
		myParent= null;
		nominalCode= 0;
		super.killMe(); //kill the content
	} //end of killMe
	
} //end of JemRecord

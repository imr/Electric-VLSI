/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemEquivRecord.java
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

// takes the place of SymmetryGroup with circuits

package com.sun.electric.tool.ncc.trees;
import com.sun.electric.tool.ncc.strategy.JemStrat;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.Wire;

import java.util.Random;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/** 
 * JemEquivRecords hold groups of circuits whose NetObjects are still
 * equivalent.  JemEquivRecords are the leaf nodes of the JemRecord
 * Tree.  Each JemEquivRecord holds a few JemCircuits, typically two,
 * that are to be compared.  The parent of a JemEquivRecord is a
 * JemHistoryRecord.  JemEquivRecords may be listed in JemEquivLists.
 * The JemEquivRecord class was formerly called "SymmetryGroup".
*/
public class JemEquivRecord extends JemRecord{
	/** ourRandom is a random number generator for assigning random
	 * codes to JemEquivRecords as they are constructed.  */
    private static Random ourRandom= new Random(204);

	/** The retired list holds JemEquivRecords whose NetObjects have
	 * been matched. JemEquivRecords "retire" when they have only one
	 * NetObject from each of the circuits being compared. */
	public static JemRecordList retired= new JemRecordList();

	/** The mismatched JemRecordList holds JemEquivRecords one of
	 * whose circuits holds only a single NetObject. */
    private static JemRecordList mismatched= new JemRecordList();

	/** Here is the constructor */
    private JemEquivRecord(){
        nominalCode= ourRandom.nextInt();
    }

	/** Here is a factory method for the JemEquivRecord class.
	 * @return a fresh JemEquivRecord */
    public static JemEquivRecord please(){
        JemEquivRecord e= new JemEquivRecord();
        return e;
    } //end of please

	/** Here is another factory method for JemEquivRecord class.
	 * @param a JemCircuitHolder with the circuits for the JemEquivRecord 
	 * @return a fresh JemEquivRecord with the circuits attached */
    public static JemEquivRecord please(JemCircuitHolder h){
        if(h == null)return null;
		if(h.maxSize() < 1)return null;
        JemEquivRecord e= JemEquivRecord.please();
        Iterator it= h.iterator();
        while(it.hasNext()){
            Object oo= it.next();
            JemCircuit x= (JemCircuit)oo;
            e.adopt(x);
        } //end of while
        return e;
    } //end of please

	/**
	 * A routine to convert a JemCircuitHolderMap into a
	 * corresponding JemEquivList of fresh JemEquivRecords.  The
	 * new records "value" field is marked according to the keys of
	 * the map.
	 * @param a JemCircuitHolderMap of JemCircuitHolders that contain
	 * the subdivided JemCircuits
	 * @return a corresponding JemEquivList of JemEquivRecords
	 */
	public static JemEquivList makeEquivs(JemCircuitHolderMap in){
		if(in == null) return null;
		JemEquivList out= new JemEquivList();
		Iterator it;
		it= in.keyIterator();
		while(it.hasNext()){
			Integer ii= (Integer)it.next();
			JemCircuitHolder ch= (JemCircuitHolder)in.get(ii);
			JemEquivRecord er= JemEquivRecord.please(ch);
			if(er != null){
				out.add(er);
				er.value= ii.intValue();
			} //end of if
		} //end of while
		return out;
	} //end of makeEquivs

	/** The killMe method destroys all pointers in this JemEquivRecord. */
	protected void killMe(){
//		getMessenger().line("killing " + nameString());
		if(getCode() == -597136633){
			getMessenger().error("killing " + nameString());
		} //end of if
		super.killMe(); //sets content to empty
		return;
	} //end of killMe
	
	//left over abstract methods

   	/**
	 * nameString returns a String of type and name for this JemEquivRecord.
	 * @return a String identifying this JemTree object.
	 */
    public String nameString(){
        String s= "JemEquivRecord " + getCode();
        return s;
    } //end of nameString

	/**
	 * checkParent tests a proposed JemParent as parent for this JemEquivRecord.
	 * @param the JemParent to test
	 * @return true if the JemParent is a JemHistoryRecord, false otherwise
	 */
	public boolean checkParent(JemParent p){
		if(p instanceof JemHistoryRecord)return true;
		else{
			getMessenger().error("bad parent class in " + nameString());
			return false;
		} //end of else
	} //end of checkParent

	/**
	 * checkChild checks that a proposed JemChild is of the proper class.
	 * @param the JemChild to test
	 * @return true if the JemChild is a JemCircuit, false otherwise
	 */
	public boolean checkChild(JemChild c){
		if(c instanceof JemCircuit)return true;
		else{
			getMessenger().error("bad child class in " + nameString());
			return false;
		} //end of else
	} //end of checkChild

	/** 
	 * The apply method applies a JemStrat to this JemEquivRecord,
	 * returning a list, possibly empty, of offspring.
	 * @param the strategy to apply
	 * @return the JemEquivList of offspring
	 */
	public JemEquivList apply(JemStrat js){
		JemCircuitMapHolder hh= new JemCircuitMapHolder();
		Iterator it= iterator();
		while(it.hasNext()){
			Object oo= it.next();
			JemCircuit cc= (JemCircuit)oo;
			JemCircuitMap mm= js.doFor(cc);
			hh.add(mm);
		} //end of loop
		JemCircuitHolderMap mm= JemCircuitHolderMap.please(hh);
		JemEquivList offspring= makeEquivs(mm);
		offspring= makeHistory(offspring);
		return offspring;
	} //end of apply

	/**
	 * makeHistory collects a list of JemEquivRecord proposed
	 * offspring under a JemHistoryRecord parent and returns the
	 * list.  If there is only one entry in the input list,
	 * makeHistory uses gobbleUp to put its information into this
	 * JemEquivRecord.
	 * @param the JemEquivList of offspring
	 * @return the JemEquivList of offspring, possibly empty
	 */
    public JemEquivList makeHistory(JemEquivList offsp){
		//should never get null input - this needs to be checked
        if(offsp == null)return null; //no split, and circuits have been fixed.
    //    JemRecordList rrl= reduce(rl);
        if(offsp.size() > 1){
            JemHistoryRecord h= JemHistoryRecord.please(offsp);
            h.historize(this);
			killMe();
            return offsp;
        }else if(offsp.size() == 1){
            //it's a singleton offspring
            // so adopt its content
            JemEquivRecord er= (JemEquivRecord)offsp.get(0);
			if(er.maxSize() == 0){
				int i= 0; //for debug - maybe return an empty list
			} //end of if
			gobbleUp(er);
			JemEquivList out= new JemEquivList();
			//avoid returning the record itself - couldn't split
//			out.add(this);
            return out;
        } //end of else
		return offsp;
    } //end of makeHistory

	/** 
	 * gobbleUp encorporates the information from another
	 * JemEquivRecord into this one and destroys the other.
	 * @param the JemEquivRecord to encorporate
	 */
	private void gobbleUp(JemEquivRecord e){
		clear();
		Iterator it= e.iterator();
		while(it.hasNext()){
			Object oo= it.next();
			JemCircuit c= (JemCircuit)oo;
			adopt(c);
		} //end of while
		copyAndKill(e);
        return;
    } //end of absorb
	
	/** 
	 * getWireExportMap produces a map of Wires to arbitrary Integers
	 * based on matching export names.
	 * @return a map of Wires to Integers
	 */
	public Map getWireExportMap(){
		//step 1 - get the string maps from the circuits
		List holder= new ArrayList(2); //to hold the circuit's maps
		Map out= new HashMap(2);
		Set keys= new HashSet(4);
		int i= 0;
		Iterator ci= iterator();
		while(ci.hasNext()){
			JemCircuit jc= (JemCircuit)ci.next();
			Map mm= jc.getExportMap();
			holder.add(mm);
			if(mm.isEmpty())continue;
			keys.addAll(mm.keySet());
		} //end of while ci
		  //keys now holds all possible Strings that are names
		if(keys.size() == 0)return out; //no ports
		Iterator ki= keys.iterator();
		while(ki.hasNext()){
			String theKey= (String)ki.next();
			//check that all maps have this key
			Iterator hi= holder.iterator();
			List wires= new ArrayList(2);
			while(hi.hasNext()){
				Map mm= (Map)hi.next();
				if(mm.containsKey(theKey)){
					Wire w= (Wire)mm.get(theKey);
					wires.add(w);
				} //end of if
			} //end of hi loop
			//does wires contain enough records?
			if(wires.size() == holder.size()){
				//yes it does
				i++;
				hi= wires.iterator();
				while(hi.hasNext()){
					Wire w= (Wire)hi.next();
					out.put(w, new Integer(i));
				} //end of output loop
			} //end of if
		} //end of key loop
		printTheMap(out);
		return out;
	} //end of getWireExportMap

	/** 
	 * printTheMap is a debug routine that exhibits the map.
	 * @param the Map to exhibit
	 */
	private static void printTheMap(Map m){
		Messenger mm= getMessenger();
		mm.line("printing an EquivRecord map of size= " + m.size());
		if(m.size() == 0)return;
		Iterator it= m.keySet().iterator();
		while(it.hasNext()){
			Wire w= (Wire)it.next();
			Object oo= m.get(w);
			if(oo == null){
				mm.line(w.nameString() + " maps to null");
			} else {
				Integer i= (Integer)oo;
				mm.line(w.nameString() + " maps to " + i.intValue());
			} //end of else
		} //end of loop
		return;
	}

	/**
	 * Here is the comparison method for Comparable interface
	 * @param an Object to test to compare to this JemEquivRecord
	 * @return a positive, negative or zero integer that indicates sorting 
	 * order
	 */
	public int compareTo(Object oo){
		JemEquivRecord s= (JemEquivRecord)oo;
		int sizeDiff= maxSize() - s.maxSize();
		if(sizeDiff != 0)return sizeDiff;
		if(s == this) return 0;
		else return 1;
	} //end of compareTo

	/** 
	 * sizeString generates a String indicating the size of the
	 * JemCircuits in this JemEquivRecord
	 * @return a String indicative of the size of this JemEquivRecord's 
	 * JemCircuits
	 */
    public String sizeString(){
        if(size() == 0) return "0";
        Iterator it= iterator();
        String s= "";
        while(it.hasNext()){
            Object oo= it.next();
            JemCircuit jc= (JemCircuit)oo;
            s= s + " " + jc.size() ;
        } //end of while
        return s;
    } //end of sizeString
	
	/** 
	 * maxSizeDiff computes the difference in the number of
	 * NetObjects in the JemCircuits of this JemEquivRecord.
	 * @return an int with the difference, zero is good
	 */
    public int maxSizeDiff(){
        int out= 0;
        int max= maxSize();
        Iterator it= iterator();
        while(it.hasNext()){
            Object oo= it.next();
            JemCircuit j= (JemCircuit)oo;
            int diff= max-j.size();
            if(diff > out)out= diff;
        } //end of loop
        return out;
    } // end of maxSizeDiff

	/** 
	 * maxSize returns the number of NetObjects in the most populous
	 * JemCircuit.
	 * @return an int with the maximum size of any JemCircuit in this
	 * JemEquivRecord
	 */
	public int maxSize(){
        int out= 0;
        Iterator it= iterator();
        while(it.hasNext()){
            Object oo= it.next();
            JemCircuit j= (JemCircuit)oo;
            if(j.size() > out)out= j.size();
        } //end of while
        return out;
    } //end of maxSize

	/** 
	 * isLive indicates that this JemEquivRecord is neither retired
	 * nor mismatched.
	 * @return true if this JemEquivRecord is still in play, false otherwise
	 */
	public boolean isLive(){
        if(size() == 0)return false;
        int largest= 0;
        Iterator it= iterator();
        while(it.hasNext()){
            Object oo= it.next();
            JemCircuit j= (JemCircuit)oo;
            if(j.size() == 0)return false;
            if(j.size() > largest)largest= j.size();
        } //end of while
        if(largest > 1)return true;
        return false;
    } //end of isLive

	/** 
	 * canRetire indicates whether this JemEquivRecord can or has
	 * retired.
	 * @return true if this JemEquivRecord can or has retired
	 */
    public boolean canRetire(){
        if(maxSize() > 1) return false;
        if(maxSizeDiff() > 0)return false;
        return true;
    } //end of canRetire

	/** 
	 * isMismatched indicates whether some JemCircuits in this
	 * JemEquivRecord differ in population.
	 * @return true if the circuits differ in population, false
	 * otherwise
	 */
    public boolean isMismatched(){
        if(maxSize() == 0)return false;
        Iterator it= iterator();
        while(it.hasNext()){
            Object oo= it.next();
            JemCircuit j= (JemCircuit)oo;
            if(j.size() == 0)return true;
        } //end of while
        return false;
    } //end of isMismatched

	/** 
	 * deleteMeIfEmpty makes an attempt to delete this JemEquivRecord
	 * if it's empty.
	 * @return true if deletion is possible
	 */
    public boolean deleteMeIfEmpty(){
        if(maxSize() > 0)return false;
		killMe();
        return true;
    } //end of deleteMeIfEmpty
		
	/** 
	 * adjacentGroups finds the JemEquivRecords whose content is
	 * adjacent to this one's.  a JemEquivRecord is adjacent if it can
	 * be reached in one step from any of g's circuits and it's live
	 * and not retired nor mismatched
	 * @return
	 */
    public JemEquivList adjacentGroups(){
		JemEquivList out= new JemEquivList();
        if(maxSize() == 0)return out; //there are none
        Iterator ci= iterator();
        while(ci.hasNext()){
            Object oo= ci.next();
            JemCircuit jc= (JemCircuit)oo;
            if(jc.size() == 0)continue;
            //get first NetObject in circuit
            Iterator it= jc.iterator();
            NetObject netObj= (NetObject)it.next();
            it= netObj.iterator();
            while(it.hasNext()){
                //for each adjacent NetObject
                netObj= (NetObject)it.next();
                //get it's grandparent JemEquivRecord
                jc= (JemCircuit)netObj.getParent();
                JemEquivRecord sg= (JemEquivRecord)jc.getParent();
                if(sg.canRetire())continue;
                if(sg.isLive())out.add(sg);
            } //end of loop
        } //end of circuit loop
		return out;
    } //end of adjacentGroups

	/** 
	 * tryToRetire tries to retire or mismatch the JemEquivRecords in
	 * a JemEquivList, and returns a JemEquivList of those that fail
	 * to retire.
	 * @param in the input JemEquivList
	 * @return a JemEquivList, possibly empty, of those that do retire.
	 */
    public static JemEquivList tryToRetire(JemEquivList in){
        if(in == null)return null;
		int count= 0;
        JemEquivList out= new JemEquivList();
        Iterator it= in.iterator();
        while(it.hasNext()){
            Object oo= it.next();
            if(oo != null){
                JemEquivRecord sg= (JemEquivRecord)oo;
                if(sg.canRetire()){
                    sg.retireMe();
					count++;
                    continue;
                } //end of if
                if(sg.isMismatched()){
                    sg.mismatchMe();
                    continue;
                } //end of if
                out.add(sg);
            } //end of if not null
        } //end of loop
		getMessenger().line("tryToRetire retired " +
							count + " JemEquivRecords");
        return out;
    } // end of tryToRetire

	/** 
	 * findNewlyRetired tries to retire or mismatch the
	 * JemEquivRecords in a JemEquivList, and returns a JemEquivList
	 * of those that do retire.
	 * @param in the input JemEquivList
	 * @return a JemEquivList, possibly empty, of those that do retire.
	 */
	public static JemEquivList findNewlyRetired(JemEquivList in){
		if(in == null)return null;
		int count= 0;
		JemEquivList out= new JemEquivList();
        Iterator it= in.iterator();
        while(it.hasNext()){
            Object oo= it.next();
            if(oo != null){
                JemEquivRecord sg= (JemEquivRecord)oo;
                if(sg.canRetire()){
                    sg.retireMe();
					out.add(sg);
					count++;
                } //end of if
                if(sg.isMismatched()){
                    sg.mismatchMe();
                } //end of if
            } //end of if not null
        } //end of loop
		getMessenger().line("findNewlyRetired retired " +
							count + " JemEquivRecords");
		return out;
	} // end of newlyRetired

	/** retireMe adds this JemEquivRecord to the retired list.
	 */
    private void retireMe(){
        retired.add(this);
//        getMessenger().line("retired JemEquivRecord #" +
//                            retired.size() +
//                            " has code " + getCode() +
//                            " sizes are " + sizeString() );
        // myJemSets.print(g);
        return;
    } //end of retireMe

	/** mismatchMe adds this JemEquivRecord to the mismatched list.
		*/
    private void mismatchMe(){
        mismatched.add(this);
        getMessenger().line("mismatched JemEquivRecord #" +
                            mismatched.size() +
                            " has code " + getCode() +
                            " sizes are " + sizeString() );
        //myJemSets.print(g);
        return;
    } //end of mismatchMe

	/** printTheLists prints the retired and mismatched lists.
		* @param (implicit) the retired and mismatched lists
	 */
	public static void printTheLists(){
		getMessenger().freshLine();
        print("retired ", retired);
		getMessenger().freshLine();
        print("mismatched ", mismatched);
    } //end of printTheLists

	/** 
	 * print is a helper method for printTheLists
	 * @param s a herald string
	 * @param c the JemRecordList to print
	 */
    private static void print(String s, JemRecordList c){
		Messenger mm= getMessenger();
        mm.line(s + " is a JemRecordList of " + c.size());
        int count= 0;
        JemStrat.passFractionOn();
        Iterator it= c.iterator();
        while(it.hasNext() && (count < 200)){
            Object oo= it.next();
            JemEquivRecord g= (JemEquivRecord)oo;
            count++;
            //getMessenger().say(count + " ");
            g.printMe(mm);
        } //end of loop
        JemStrat.passFractionOff();
        return;
    } //end of print
	
	/** 
	 * printMe prints this JemEquivRecord on a given Messenger.
	 * @param the Messenger to use for output
	 */
	public void printMe(Messenger mm){
		mm.line(nameString() + " value= " + value +
				" maxSize= " + maxSize());
	}

}

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
	private List circuits = new ArrayList(); 
	
	/** ourRandom is a random number generator for assigning random
	 * codes to JemEquivRecords as they are constructed.  */
    private static Random ourRandom= new Random(204);
    private static HashSet randoms = new HashSet();

	/** Here is the constructor */
    private JemEquivRecord(int key){
        Integer nc;
		// I've seen different nextInt() calls return the same value
		// Don't allow two JemEquivRecords to have the same nominal code.
        do {
			nominalCode = ourRandom.nextInt();
			nc = new Integer(nominalCode);
        } while(randoms.contains(nc));
        
        randoms.add(nc);
        value = key;
    }

	private ArrayList getOneMapPerCircuit(JemStrat js) {
		ArrayList mapPerCkt = new ArrayList();
		for (Iterator it=getCircuits(); it.hasNext();) {
			JemCircuit ckt = (JemCircuit)it.next();
			HashMap codeToNetObjs = js.doFor(ckt);
			mapPerCkt.add(codeToNetObjs);
		}
		return mapPerCkt;
	}

	/**
	 * Get all the keys of all the maps.
	 * @param maps list of maps
	 * @return the set of all keys from all the maps
	 */
	private Set getKeysFromAllMaps(ArrayList mapPerCkt) {
		Set keys = new HashSet();
		for (Iterator it=mapPerCkt.iterator(); it.hasNext();) {
			HashMap map = (HashMap) it.next();
			keys.addAll(map.keySet());
		}
		return keys;    	
	}
	/**
	 * Create a JemEquivRec for all the JemCircuits corresponding to a
	 * given key. If a map has a list of NetObjects for the given key
	 * then create a JemCircuit containing those NetObjects.
	 * Otherwise, add an empty JemCircuit to the JemEquivRec.
	 * @param mapPerCkt ArrayList of maps.  Each map maps: 
	 * (Integer -> ArrayList of NetObjects)
	 * @param key check each map for a List of NetObjects at this key 
	 * @return a JemEquivRec
	 */
	private JemEquivRecord makeEquivRecForKey(ArrayList mapPerCkt,
											  Integer key) {
		JemEquivRecord er = JemEquivRecord.please(key.intValue());
		for (Iterator it=mapPerCkt.iterator(); it.hasNext();) {
			HashMap map = (HashMap) it.next();
			ArrayList netObjs = (ArrayList) map.get(key);
			if (netObjs==null)  netObjs = new ArrayList();
			JemCircuit ckt = JemCircuit.please(netObjs);
			er.addCircuit(ckt); 
		}
		return er;												 	
	}

	/** Here is a factory method for the JemEquivRecord class.
	 * @return a fresh JemEquivRecord */
    public static JemEquivRecord please(int key){
    	return new JemEquivRecord(key);
    }

	//left over abstract methods

   	/**
	 * nameString returns a String of type and name for this JemEquivRecord.
	 * @return a String identifying this JemTree object.
	 */
    public String nameString(){return "JemEquivRecord " + getCode();}

	public Iterator getCircuits() {return circuits.iterator();}
	public int numCircuits() {return circuits.size();}
	public void addCircuit(JemCircuit c) {
		circuits.add(c);
		c.setParent(this);
	}

	/**
	 * say whether this JemEquivRecord contains Parts, Wires, or Ports.
	 * Assumes that JemEquivRecord only holds one kind of NetObject. The
	 * assumption is true after JemStratPartWirePort has run.
	 * @return PART, WIRE, or PORT
	 */
	public NetObject.Type getNetObjType() {
		for (Iterator ci=getCircuits(); ci.hasNext();) {
			JemCircuit c = (JemCircuit) ci.next();
			Iterator ni = c.getNetObjs();
			if (ni.hasNext()) {
				NetObject no = (NetObject) ni.next();
				return no.getNetObjType();
			}
		}
		error(true, "no NetObjects in a JemEquivRecord?");
		return null; 
	}
	
	/**
	 * Get total number of NetObjects in all Circuits
	 * @return number of NetObjects
	 */
	public int numNetObjs() {
		int sum = 0;
		for (Iterator ci=getCircuits(); ci.hasNext();) {
			JemCircuit c = (JemCircuit) ci.next();
			sum += c.numNetObjs();
		}
		return sum;
	}

	/** 
	 * The apply method applies a JemStrat to this JemEquivRecord,
	 * returning a list, possibly empty, of offspring.
	 * @param js the strategy to apply
	 * @return the JemEquivList of offspring
	 */
	public JemEquivList apply(JemStrat js) {
		ArrayList mapPerCkt = getOneMapPerCircuit(js);
		
		Set keys = getKeysFromAllMaps(mapPerCkt);
		
		error(keys.size()==0, "must have at least one key");
		
		// If everything maps to one hash code then no offspring
		if (keys.size()==1) return new JemEquivList();
		
		JemEquivList offspring = new JemEquivList();
		for (Iterator it=keys.iterator(); it.hasNext();) {
			Integer key = (Integer) it.next();
			JemEquivRecord er = makeEquivRecForKey(mapPerCkt, key); 
			offspring.add(er);
		}
		JemHistoryRecord.please(this, offspring);
		
		return offspring;
	}
	
	/** 
	 * sizeString generates a String indicating the size of the
	 * JemCircuits in this JemEquivRecord
	 * @return a String indicative of the size of this JemEquivRecord's 
	 * JemCircuits
	 */
    public String sizeString(){
        if(numCircuits() == 0) return "0";
        String s= "";
        for (Iterator it=getCircuits(); it.hasNext();) {
            JemCircuit jc= (JemCircuit) it.next();
            s= s + " " + jc.numNetObjs() ;
        }
        return s;
    }
	
	/** 
	 * maxSizeDiff computes the difference in the number of
	 * NetObjects in the JemCircuits of this JemEquivRecord.
	 * @return an int with the difference, zero is good
	 */
    public int maxSizeDiff(){
        int out= 0;
        int max= maxSize();
        for (Iterator it=getCircuits(); it.hasNext();) {
            JemCircuit j= (JemCircuit) it.next();
            int diff= max-j.numNetObjs();
            if(diff > out)out= diff;
        }
        return out;
    }

	/** 
	 * maxSize returns the number of NetObjects in the most populous
	 * JemCircuit.
	 * @return an int with the maximum size of any JemCircuit in this
	 * JemEquivRecord
	 */
	public int maxSize(){
        int out= 0;
        for (Iterator it=getCircuits(); it.hasNext();) {
            JemCircuit j= (JemCircuit)it.next();;
            out = Math.max(out, j.numNetObjs());
        }
        return out;
    }

	/** 
	 * isActive indicates that this JemEquivRecord is neither retired
	 * nor mismatched.
	 * @return true if this JemEquivRecord is still in play, false otherwise
	 */
	public boolean isActive(){
        error(numCircuits()==0, "JemEquivRecord with no circuits?");
        for (Iterator it=getCircuits(); it.hasNext();) {
            JemCircuit c = (JemCircuit) it.next();
            if (c.numNetObjs()==0) return false; // mismatched
            if (c.numNetObjs()>1)  return true;  // live
        }
        return false;
    }

	/** 
	 * canRetire indicates whether this JemEquivRecord can or has
	 * retired.
	 * @return true if this JemEquivRecord can or has retired
	 */
    public boolean isRetired(){
		for (Iterator it=getCircuits(); it.hasNext();) {
			JemCircuit c = (JemCircuit) it.next();
			if (c.numNetObjs()!=1) return false;
		}
		return true;
    }

	/** 
	 * isMismatched indicates whether some JemCircuits in this
	 * JemEquivRecord differ in population.
	 * @return true if the circuits differ in population, false
	 * otherwise
	 */
    public boolean isMismatched(){
        for (Iterator it=getCircuits(); it.hasNext();) {
            JemCircuit c = (JemCircuit) it.next();
            if (c.numNetObjs()==0) return true;
        }
        return false;
    }

	/** 
	 * printMe prints this JemEquivRecord on a given Messenger.
	 */
	public void printMe(){
		Messenger.line(nameString() + " value= " + value +
				" maxSize= " + maxSize());
	}

}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EquivRecord.java
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.jemNets.NetObject;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.lists.RecordList;
import com.sun.electric.tool.ncc.strategy.Strategy;

/** Leaf EquivRecords hold Circuits. Internal EquivRecords hold offspring.
 * Every EquivRecord is assigned a pseudo random code at birth which it 
 * retains for life. */
public class EquivRecord {
	/** points toward root */ 	             private EquivRecord parent;
	/** the immutable random code */         private int randCode;
	/** int that distinguished this Record */private int value;
	/** comment describing the metric used to
	 * partition my parent into my siblings 
	 * and me */							 private String partitionReason;

	// At any given time only one of this lists is non-null
	private RecordList offspring;
	private List circuits;

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	/** For a leaf record, apply strategy to build one Map for each Circuit
	 * @param js
	 * @return list of Maps */
	private ArrayList getOneMapPerCircuit(Strategy js) {
		ArrayList mapPerCkt = new ArrayList();
		for (Iterator it=getCircuits(); it.hasNext();) {
			Circuit ckt = (Circuit)it.next();
			HashMap codeToNetObjs = js.doFor(ckt);
			mapPerCkt.add(codeToNetObjs);
		}
		return mapPerCkt;
	}

	/** Get all the keys of all the maps.
	 * @param maps list of maps
	 * @return the set of all keys from all the maps */
	private Set getKeysFromAllMaps(ArrayList mapPerCkt) {
		Set keys = new HashSet();
		for (Iterator it=mapPerCkt.iterator(); it.hasNext();) {
			HashMap map = (HashMap) it.next();
			keys.addAll(map.keySet());
		}
		return keys;    	
	}

	/** Create a leaf record for all the Circuits corresponding to a
	 * given key. If a map has a list of NetObjects for the given key
	 * then create a Circuit containing those NetObjects.
	 * Otherwise, add an empty Circuit to the EquivRecord.
	 * @param mapPerCkt ArrayList of maps.  Each map maps: 
	 * (Integer -> ArrayList of NetObjects)
	 * @param key check each map for a List of NetObjects at this key 
	 * @return a EquivRecord */
	private EquivRecord makeEquivRecForKey(ArrayList mapPerCkt, Integer key, NccGlobals globals) {
		List ckts = new ArrayList();
		for (Iterator it=mapPerCkt.iterator(); it.hasNext();) {
			HashMap map = (HashMap) it.next();
			ArrayList netObjs = (ArrayList) map.get(key);
			if (netObjs==null)  netObjs = new ArrayList();
			ckts.add(Circuit.please(netObjs));
		}
		return EquivRecord.newLeafRecord(key.intValue(), ckts, globals);												 	
	}

	/** constructor*/
	private EquivRecord(){}
	
	private void addOffspring(EquivRecord r) {
		offspring.add(r);
		r.setParent(this);
	}

	private LeafList applyToLeaf(Strategy js) {
		ArrayList mapPerCkt = getOneMapPerCircuit(js);
		
		Set keys = getKeysFromAllMaps(mapPerCkt);
		
		error(keys.size()==0, "must have at least one key");
		
		// If everything maps to one hash code then no offspring
		if (keys.size()==1) return new LeafList();
		
		// Change this record from leaf to internal
		circuits = null;
		offspring = new RecordList();
		
		for (Iterator it=keys.iterator(); it.hasNext();) {
			Integer key = (Integer) it.next();
			EquivRecord er = makeEquivRecForKey(mapPerCkt, key, js.globals); 
			addOffspring(er);
		}
		

		LeafList el = new LeafList();
		el.addAll(offspring);
		return el;
	}

	private LeafList applyToInternal(Strategy js) {
		LeafList offspring = new LeafList();
		for (Iterator it=getOffspring(); it.hasNext();) {
			EquivRecord jr= (EquivRecord) it.next();
			offspring.addAll(js.doFor(jr));
		}
		return offspring;
	}

	// --------------------------- public methods -----------------------------
	/** @return internal EquivRecord that contains me */
	public EquivRecord getParent() {return parent;}

	/** getCode returns the fixed hash code for this object.
	 * @return the int fixed hash code for this object. */
	public int getCode(){return randCode;}

    public void checkMe(EquivRecord parent) {
    	error(getParent()!=parent, "wrong parent");
    	error(!(offspring==null ^ circuits==null), "bad lists");
    }

	public void setParent(EquivRecord x) {parent=x;}
	
	/** get the value that a strategy used to distinguish
	 * this EquivRecord.
	 * @return the int value that distinguished this EquivRecord */
	public int getValue(){return value;}

	public Iterator getCircuits() {return circuits.iterator();}

	public int numCircuits() {return circuits.size();}

	public void addCircuit(Circuit c) {
		circuits.add(c);
		c.setParent(this);
	}

	/** say whether this leaf record contains Parts, Wires, or Ports.
	 * A leaf record can only hold one kind of NetObject. 
	 * @return PART, WIRE, or PORT */
	public NetObject.Type getNetObjType() {
		for (Iterator ci=getCircuits(); ci.hasNext();) {
			Circuit c = (Circuit) ci.next();
			Iterator ni = c.getNetObjs();
			if (ni.hasNext()) {
				NetObject no = (NetObject) ni.next();
				return no.getNetObjType();
			}
		}
		error(true, "no NetObjects in a leaf EquivRecord?");
		return null; 
	}

	/** Get total number of NetObjects in all Circuits of a leaf record
	 * @return number of NetObjects */
	public int numNetObjs() {
		int sum = 0;
		for (Iterator ci=getCircuits(); ci.hasNext();) {
			Circuit c = (Circuit) ci.next();
			sum += c.numNetObjs();
		}
		return sum;
	}

	/** generates a String indicating the size of the
	 * Circuits in this leaf record
	 * @return the String */
	public String sizeString() {
	    if(numCircuits() == 0) return "0";
	    String s= "";
	    for (Iterator it=getCircuits(); it.hasNext();) {
	        Circuit jc= (Circuit) it.next();
	        s= s + " " + jc.numNetObjs() ;
	    }
	    return s;
	}

	/** maxSizeDiff computes the difference in the number of
	 * NetObjects in the Circuits of this leaf record.
	 * @return an int with the difference, zero is good */
	public int maxSizeDiff() {
	    int out= 0;
	    int max= maxSize();
	    for (Iterator it=getCircuits(); it.hasNext();) {
	        Circuit j= (Circuit) it.next();
	        int diff= max-j.numNetObjs();
	        if(diff > out)out= diff;
	    }
	    return out;
	}

	/** maxSize returns the number of NetObjects in the most populous
	 * Circuit.
	 * @return an int with the maximum size of any Circuit in this
	 * leaf record */
	public int maxSize() {
	    int out= 0;
	    for (Iterator it=getCircuits(); it.hasNext();) {
	        Circuit j= (Circuit)it.next();;
	        out = Math.max(out, j.numNetObjs());
	    }
	    return out;
	}

	/** isActive indicates that this leaf record is neither matched
	 * nor mismatched.
	 * @return true if this leaf record is still in play, false otherwise */
	public boolean isActive() {
	    error(numCircuits()==0, "leaf record with no circuits?");
	    for (Iterator it=getCircuits(); it.hasNext();) {
	        Circuit c = (Circuit) it.next();
	        if (c.numNetObjs()==0) return false; // mismatched
	        if (c.numNetObjs()>1)  return true;  // live
	    }
	    return false;
	}

	/** @return true if matched */
	public boolean isMatched() {
		for (Iterator it=getCircuits(); it.hasNext();) {
			Circuit c = (Circuit) it.next();
			if (c.numNetObjs()!=1) return false;
		}
		return true;
	}

	/** isMismatched indicates whether some Circuits in this
	 * leaf record differ in population.
	 * @return true if the circuits differ in population, false
	 * otherwise */
	public boolean isMismatched() {
		boolean first = true;
		int sz = 0;
	    for (Iterator it=getCircuits(); it.hasNext();) {
	        Circuit c = (Circuit) it.next();
	        if (first) {
	        	sz = c.numNetObjs();
	        	first = false;
	        } else {
				if (c.numNetObjs()!=sz) return true;
	        }
	    }
	    return false;
	}

	/** get offspring of internal record */
	public Iterator getOffspring() {return offspring.iterator();}

	public int numOffspring() {return offspring.size();}

	/** The apply method applies a Strategy to this leaf EquivRecord.  If the 
	 * divides this Record then this leaf record becomes an internal record.
	 * @param js the Strategy to apply
	 * @return a LeafList of the resulting offspring */
	public LeafList apply(Strategy js) {
		return isLeaf() ? applyToLeaf(js) : applyToInternal(js);
	}

	/** nameString returns a String of type and name for this parent.
 	 * @return a String identifying this EquivRecord. */
	public String nameString() {
		String name = "";
		if (isLeaf()) {
			name = isMatched() ? "Matched" : isMismatched() ? "Mismatched" : "Active";
			name += " leaf";
		} else {
			name = "Internal";
		}
		name += " Record randCode="+randCode+" value="+value;
		name += isLeaf() ? (
			" maxSize="+maxSize()
		) : (
			" #offspring="+numOffspring()
		);
		return name;
	}

	public boolean isLeaf() {return offspring==null;} 

	/** The fixed strategies annotate EquivRecords with comments
	 * describing what characteristic made this EquivRecord unique.
	 * This information is useful for providing pre-analysis information
	 * to the user. */
	public void setPartitionReason(String s) {partitionReason=s;}
	public String getPartitionReason() {return partitionReason;}
	public List getPartitionReasonsFromRootToMe() {
		LinkedList reasons = new LinkedList();
		for (EquivRecord r=this; r!=null; r=r.getParent()) {
			String reason = r.getPartitionReason();
			if (reason!=null) reasons.addFirst(reason);
		}
		return reasons;
	}

	/** Construct a leaf EquivRecord that holds circuits
	 * @param ckts Circuits belonging to Equivalence Record
	 * @param globals used for generating random numbers
	 * @return the new EquivRecord */
	public static EquivRecord newLeafRecord(int key, List ckts, NccGlobals globals) {
		EquivRecord r = new EquivRecord();
		r.circuits = new ArrayList();
		r.value = key;
		r.randCode = globals.getRandom();
		for (Iterator it=ckts.iterator(); it.hasNext();) {
			r.addCircuit((Circuit)it.next());
		}
		error(r.maxSize()==0, 
			  "invalid leaf EquivRecord: all Circuits are empty");
		return r;
	}
	/** Construct an internal EquivRecord that will serve as the root of the 
	 * EquivRecord tree
	 * @param offspring 
	 * @return the new EquivRecord or null if there are no offspring */
	public static EquivRecord newRootRecord(List offspring) {
		if (offspring.size()==0) return null;
		EquivRecord r = new EquivRecord();
		r.offspring = new RecordList();
		for (Iterator it=offspring.iterator(); it.hasNext();) {
			r.addOffspring((EquivRecord) it.next());
		}
		return r;		
	}
}
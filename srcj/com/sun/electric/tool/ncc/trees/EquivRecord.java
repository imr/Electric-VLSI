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
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.lists.RecordList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.processing.NewLocalPartitionWires;
import com.sun.electric.tool.ncc.result.EquivRecReport.EquivRecReportable;
import com.sun.electric.tool.ncc.result.NetObjReport.NetObjReportable;
import com.sun.electric.tool.ncc.strategy.Strategy;

/** Leaf EquivRecords hold Circuits. Internal EquivRecords hold offspring.
 * Every EquivRecord is assigned a pseudo random code at birth which it 
 * retains for life. 
 * <p>
 * A Leaf EquivRecord is "balanced" if all Circuits have the same number of 
 * NetObjects. "Matched" means balanced with each Circuit having one 
 * NetObject. "Mismatched" means unbalanced with some some Circuit having 
 * no NetObject. "Active" means not matched and not mismatched. */
public class EquivRecord implements EquivRecReportable {
	/** Get all NetObjects contained by an EquivRecord subtree. 
	 * The NetObjects found in matched EquivRecords are collected 
	 * separately from NetObjects found in not-matched EquivRecords. */
	private static class GetNetObjs extends Strategy {
		private int numDesigns = -1;
//		private NetObject.Type  type = null;
		private final List<List<NetObject>> matches = 
									new ArrayList<List<NetObject>>();
		private final List<List<NetObject>> notMatches = 
									new ArrayList<List<NetObject>>();
		
//		private void checkType(NetObject n) {
//			NetObject.Type t = n.getNetObjType();
//			if (type==null) type=t;
//			LayoutLib.error(type!=t, "different types in same EquivRecord");
//			LayoutLib.error(type!=NetObject.Type.PART &&
//					        type!=NetObject.Type.WIRE,
//					        "expecting only Parts or Wires");
//		}
		
		private void appendNetObjsFromCircuit(List<List<NetObject>> lists, 
				                              EquivRecord er) {
			int i=0;
			for (Iterator<Circuit> itC=er.getCircuits(); itC.hasNext(); i++) {
				Circuit ckt = (Circuit) itC.next();
				for (Iterator<NetObject> itN=ckt.getNetObjs(); itN.hasNext();) {
					lists.get(i).add(itN.next());
				}
			}
			LayoutLib.error(i!=numDesigns, "wrong number of circuits");
		}
		
		@Override public LeafList doFor(EquivRecord er) {
			if (er.isLeaf()) {
				if (numDesigns==-1) {
					// We've encountered the first leaf EquivRec. Now we
					// can initialize the Lists.
					numDesigns = er.numCircuits();
					for (int i=0; i<numDesigns; i++) {
						matches.add(new ArrayList<NetObject>());
						notMatches.add(new ArrayList<NetObject>());
					}
				}
				appendNetObjsFromCircuit(er.isMatched() ? matches : notMatches, er);
				return new LeafList();
			} else {
				return super.doFor(er);
			}
		}
		
		// ------------------------- intended interface -----------------------
		public GetNetObjs(EquivRecord er) {
			super(null);
			doFor(er);
		}
		
		/**  @return one list per Circuit. Each list contains all the matched
		 * NetObjects in that circuit. For all these lists, elements at the
		 * same index matched. */
		public List<List<NetObject>> getMatchedNetObjs() {return matches;}
		
		/** @return one list per Circuit. Each list contains all not-matched
		 * NetObjects in that circuit. */
		public List<List<NetObject>> getNotMatchedNetObjs() {
			return notMatches;
		}
		//public boolean hasParts() {return type==NetObject.Type.PART;}
	}

	// points toward root 	             
	private EquivRecord parent;
	
	// the immutable random code
	private int randCode;
	
	// int that distinguished this Record
	private int value;
	
	// comment describing the metric used to partition my parent into my 
	// siblings and me
	private String partitionReason;
	
	// description of Wire PortInst connections
	private NewLocalPartitionWires.Signature wireSignature; 
	 
	// At any given time only one of this lists is non-null
	private RecordList offspring;
	private List<Circuit> circuits;

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	/** For a leaf record, apply strategy to build one Map for each Circuit
	 * @param js
	 * @return list of Maps */
	private ArrayList<HashMap<Integer,List<NetObject>>> getOneMapPerCircuit(Strategy js) {
		ArrayList<HashMap<Integer,List<NetObject>>> mapPerCkt = new ArrayList<HashMap<Integer,List<NetObject>>>();
		for (Iterator<Circuit> it=getCircuits(); it.hasNext();) {
			Circuit ckt = it.next();
			HashMap<Integer,List<NetObject>> codeToNetObjs = js.doFor(ckt);
			mapPerCkt.add(codeToNetObjs);
		}
		return mapPerCkt;
	}

	/** Get all the keys of all the maps.
	 * @param mapPerCkt list of maps
	 * @return the set of all keys from all the maps */
	private Set<Integer> getKeysFromAllMaps(ArrayList<HashMap<Integer,List<NetObject>>> mapPerCkt) {
		Set<Integer> keys = new HashSet<Integer>();
		for (HashMap<Integer,List<NetObject>> map : mapPerCkt) {
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
	private EquivRecord makeEquivRecForKey(ArrayList<HashMap<Integer,List<NetObject>>> mapPerCkt, Integer key, NccGlobals globals) {
		List<Circuit> ckts = new ArrayList<Circuit>();
		for (HashMap<Integer,List<NetObject>> map : mapPerCkt) {
			ArrayList<NetObject> netObjs = (ArrayList<NetObject>) map.get(key);
			if (netObjs==null)  netObjs = new ArrayList<NetObject>();
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
		ArrayList<HashMap<Integer,List<NetObject>>> mapPerCkt = getOneMapPerCircuit(js);
		
		Set<Integer> keys = getKeysFromAllMaps(mapPerCkt);
		
		error(keys.size()==0, "must have at least one key");
		
		// If everything maps to one hash code then no offspring
		if (keys.size()==1) return new LeafList();
		
		// Change this record from leaf to internal
		circuits = null;
		offspring = new RecordList();
		
		for (Integer key : keys) {
			EquivRecord er = makeEquivRecForKey(mapPerCkt, key, js.globals); 
			addOffspring(er);
		}
		

		LeafList el = new LeafList();
		el.addAll(offspring);
		return el;
	}

	private LeafList applyToInternal(Strategy js) {
		LeafList offspring = new LeafList();
		for (Iterator<EquivRecord> it=getOffspring(); it.hasNext();) {
			EquivRecord jr= it.next();
			offspring.addAll(js.doFor(jr));
		}
		return offspring;
	}

	// --------------------------- public methods -----------------------------
	/** @return internal EquivRecord that contains me */
	public EquivRecord getParent() {return parent;}

	/** getCode returns the fixed hash code for this object.
	 * @return the int fixed hash code for this object. */
	public int getCode(){return isMismatched()? 0 : randCode;}

    public void checkMe(EquivRecord parent) {
    	error(getParent()!=parent, "wrong parent");
    	error(!(offspring==null ^ circuits==null), "bad lists");
    }

	public void setParent(EquivRecord x) {parent=x;}
	
	/** get the value that a strategy used to distinguish
	 * this EquivRecord.
	 * @return the int value that distinguished this EquivRecord */
	public int getValue(){return value;}

	public Iterator<Circuit> getCircuits() {return circuits.iterator();}

	public int numCircuits() {return circuits.size();}

	public void addCircuit(Circuit c) {
		circuits.add(c);
		c.setParent(this);
	}

	/** say whether this leaf record contains Parts, Wires, or Ports.
	 * A leaf record can only hold one kind of NetObject. 
	 * @return PART, WIRE, or PORT */
	public NetObject.Type getNetObjType() {
		for (Iterator<Circuit> ci=getCircuits(); ci.hasNext();) {
			Circuit c = ci.next();
			Iterator<NetObject> ni = c.getNetObjs();
			if (ni.hasNext()) {
				NetObject no = ni.next();
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
		for (Iterator<Circuit> ci=getCircuits(); ci.hasNext();) {
			Circuit c = ci.next();
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
	    for (Iterator<Circuit> it=getCircuits(); it.hasNext();) {
	        Circuit jc= it.next();
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
	    for (Iterator<Circuit> it=getCircuits(); it.hasNext();) {
	        Circuit j= it.next();
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
	    for (Iterator<Circuit> it=getCircuits(); it.hasNext();) {
	        Circuit j= it.next();;
	        out = Math.max(out, j.numNetObjs());
	    }
	    return out;
	}

	/** isActive indicates that this leaf record is neither matched
	 * nor mismatched.
	 * @return true if this leaf record is still in play, false otherwise */
	public boolean isActive() {
	    error(numCircuits()==0, "leaf record with no circuits?");
	    for (Iterator<Circuit> it=getCircuits(); it.hasNext();) {
	        Circuit c = it.next();
	        if (c.numNetObjs()==0) return false; // mismatched
	        if (c.numNetObjs()>1)  return true;  // live
	    }
	    return false;
	}

	/** @return true if all Circuits have same number of NetObjects. */
	public boolean isBalanced() {
		boolean first = true;
		int sz = 0;
	    for (Iterator<Circuit> it=getCircuits(); it.hasNext();) {
	        Circuit c = it.next();
	        if (first) {
	        	sz = c.numNetObjs();
	        	first = false;
	        } else {
				if (c.numNetObjs()!=sz) return false;
	        }
	    }
	    return true;
	}

	/** isMatched is a special case of balanced.
	 * @return true if every Circuit has one NetObject */
	public boolean isMatched() {
		for (Iterator<Circuit> it=getCircuits(); it.hasNext();) {
			Circuit c = it.next();
			if (c.numNetObjs()!=1) return false;
		}
		return true;
	}

	/** isMismatched is a special case of unbalanced. 
	 * @return true if some Circuit has no NetObject */
	public boolean isMismatched() {
		// It's impossible for all Circuits to be zero sized so we only
		// need to find the first zero sized.
	    for (Iterator<Circuit> it=getCircuits(); it.hasNext();) {
	        Circuit c = it.next();
			if (c.numNetObjs()==0) return true;
	    }
	    return false;
	}

	/** get offspring of internal record */
	public Iterator<EquivRecord> getOffspring() {return offspring.iterator();}

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
	public List<String> getPartitionReasonsFromRootToMe() {
		if (wireSignature!=null) {
			return wireSignature.getReasons();
		} else {
			LinkedList<String> reasons = new LinkedList<String>();
			for (EquivRecord r=this; r!=null; r=r.getParent()) {
				String reason = r.getPartitionReason();
				if (reason!=null) reasons.addFirst(reason);
			}
			return reasons;
		}
	}
	public void setWireSignature(NewLocalPartitionWires.Signature sig) {
		wireSignature = sig;
	}

	/** Construct a leaf EquivRecord that holds circuits
	 * @param ckts Circuits belonging to Equivalence Record
	 * @param globals used for generating random numbers
	 * @return the new EquivRecord */
	public static EquivRecord newLeafRecord(int key, List<Circuit> ckts, NccGlobals globals) {
		EquivRecord r = new EquivRecord();
		r.circuits = new ArrayList<Circuit>();
		r.value = key;
		r.randCode = globals.getRandom();
		for (Circuit ckt : ckts) {
			r.addCircuit(ckt);
		}
		error(r.maxSize()==0, 
			  "invalid leaf EquivRecord: all Circuits are empty");
		return r;
	}
	/** Construct an internal EquivRecord that will serve as the root of the 
	 * EquivRecord tree
	 * @param offspring 
	 * @return the new EquivRecord or null if there are no offspring */
	public static EquivRecord newRootRecord(List<EquivRecord> offspring) {
		if (offspring.size()==0) return null;
		EquivRecord r = new EquivRecord();
		r.offspring = new RecordList();
		for (EquivRecord er : offspring) {
			r.addOffspring(er);
		}
		return r;		
	}
	/** Get all NetObjects contained by an EquivRecord subtree.
	 * @param matched list of list of NetObjects from matched EquivRecords
	 * indexed as: [circuitIndex][netObjectIndex]. NetObjects at the same
	 * index in each list match.
	 * @param notMatched list of list of NetObjects from not matched
	 * EquivRecords indexed as [circuitIndex][netObjectIndex] */
	public void getNetObjsFromEntireTree(List<List<NetObject>> matched,
			                             List<List<NetObject>> notMatched) {
		GetNetObjs gno = new GetNetObjs(this);
		matched.clear();
		matched.addAll(gno.getMatchedNetObjs());
		notMatched.clear();
		notMatched.addAll(gno.getNotMatchedNetObjs());
	}
	private List<List<NetObjReportable>> coerceToReportable(List<List<NetObject>> no) {
		List<List<NetObjReportable>> nor = new ArrayList<List<NetObjReportable>>();
		for (List<NetObject> i : no) {
			List<NetObjReportable> j = new ArrayList<NetObjReportable>();
			j.addAll(i);
			nor.add(j);
		}
		return nor;
	}
	public void getNetObjReportablesFromEntireTree(
									List<List<NetObjReportable>> matched,
						            List<List<NetObjReportable>> notMatched) {
		List<List<NetObject>> m = new ArrayList<List<NetObject>>();
		List<List<NetObject>> nm = new ArrayList<List<NetObject>>();
		getNetObjsFromEntireTree(m, nm);

		matched.clear();
		matched.addAll(coerceToReportable(m));
		
		notMatched.clear();
		notMatched.addAll(coerceToReportable(nm));
	}
}
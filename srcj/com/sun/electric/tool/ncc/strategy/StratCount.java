/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratCount.java
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
package com.sun.electric.tool.ncc.strategy;

import java.util.TreeMap;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.netlist.Port;
import com.sun.electric.tool.ncc.netlist.Wire;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/** StratCount counts and prints a tree's content. StratCount
 * implements a doFor method for each level in the EquivRecord tree,
 * keeping counters of what it finds. */
public class StratCount extends Strategy {
	// ------------------------------ types -----------------------------------
	/** Object to maintain a statistical datum (an int) for each type of
	 * NetObject. */
	private static class NetObjStats {
		private static final int NUM_TYPES = 3;
		private int[] data = new int[NUM_TYPES];
		NetObjStats(int initialVal) {
			for (int i=0; i<NUM_TYPES; i++)  data[i] = initialVal;
		}
		public void incr(NetObject.Type type, int delta) {
			data[type.ordinal()] += delta;
		}
		public int get(NetObject.Type type) {return data[type.ordinal()];}
		public int getSumForAllTypes() {
			int sum = 0;
			for (int i=0; i<NUM_TYPES; i++)  sum+=data[i];
			return sum;
		}
		public String toString() {
			String out = "";
			for (int i=0; i<NUM_TYPES; i++) {
				String v = String.valueOf(data[i]);
				out += rightJustifyInField(v, FIELD_WIDTH);
			}
			return out;
		}
	}
	
	private class SizeHistogram {
		TreeMap<Integer,NetObjStats> sizeToStats = new TreeMap<Integer,NetObjStats>();
		void incr(NetObject.Type type, int size) {
			Integer sz = new Integer(size);
			NetObjStats stats = sizeToStats.get(sz);
			if (stats==null) {
				stats = new NetObjStats(0);
				sizeToStats.put(sz, stats);
			}
			stats.incr(type, 1);
		}
		void print() {
			globals.status2(
				spaces(INDENT_WIDTH)+
				leftJustifyInField("LeafRec size", LABEL_WIDTH)+
				rightJustifyInField("#Part_Recs", FIELD_WIDTH)+
				rightJustifyInField("#Wire_Recs", FIELD_WIDTH)+
				rightJustifyInField("#Port_Recs", FIELD_WIDTH));
			for (Integer key : sizeToStats.keySet()) {
				NetObjStats stats = sizeToStats.get(key);
				printLine(rightJustifyInField(key.toString(), 7),
						  stats);
			}
		}
	}

	public static class Counts {
		public final int numMatchedPartEquivRecs;
		public final int numMatchedWireEquivRecs;
		public final int numMismatchedPartEquivRecs;
		public final int numMismatchedWireEquivRecs;
		public final int numActivePartEquivRecs;
		public final int numActiveWireEquivRecs;
		public Counts(int matchPartER, int matchWireER, 
					  int mismatchPartER, int mismatchWireER,
					  int activePartER, int activeWireER) {
			numMatchedPartEquivRecs = matchPartER;
			numMatchedWireEquivRecs = matchWireER;
			numMismatchedPartEquivRecs = mismatchPartER;
			numMismatchedWireEquivRecs = mismatchWireER;
			numActivePartEquivRecs = activePartER;
			numActiveWireEquivRecs = activeWireER;
		}
		public int numNotMatchedPartEquivRecs() {
			return numMismatchedPartEquivRecs + numActivePartEquivRecs;
		}
		public int numNotMatchedWireEquivRecs() {
			return numMismatchedWireEquivRecs + numActiveWireEquivRecs;
		}
	}

	// -------------------------- private data --------------------------------
	private static final int INDENT_WIDTH = 4;
	private static final int LABEL_WIDTH = 30;
	private static final int FIELD_WIDTH = 15; 

    private int maxDepth; //depth in the tree
    private int numInternalRecs;
	private NetObjStats numMismatchedNetObjs = new NetObjStats(0);
	private NetObjStats numMatchedNetObjs = new NetObjStats(0);
	private NetObjStats numActiveNetObjs = new NetObjStats(0);
	private NetObjStats numMismatchedLeafRecs = new NetObjStats(0);
	private NetObjStats numMatchedLeafRecs = new NetObjStats(0);
	private NetObjStats numActiveLeafRecs = new NetObjStats(0);
	private int numCircuits;
    private int totalEqGrpSize;
    private int numberOfWireConnections;
    private long numberOfWireConnectionsSquared;
    private int numberOfPartConnections;
	private String workingOnString;
	private NetObject.Type netObjType;
	private SizeHistogram sizeHistogram = new SizeHistogram();

    private StratCount(NccGlobals globals) {super(globals);}
    
    private static String spaces(int num) {
    	StringBuffer b = new StringBuffer();
    	for (int i=0; i<num; i++)  b.append(" ");
    	return b.toString();
    }
    private static String rightJustifyInField(String s, int fieldWidth) {
    	return spaces(fieldWidth - s.length()) + s;
    }
    
    private static String leftJustifyInField(String s, int fieldWidth) {
		return s + spaces(fieldWidth - s.length());
    }
	private void printLine(String label, NetObjStats stats) {
		globals.status2(spaces(INDENT_WIDTH) +
					    leftJustifyInField(label, LABEL_WIDTH) +
					    stats.toString());
	}
    private void printLine(String label, int data) {
    	globals.status2(spaces(INDENT_WIDTH) +
    				    leftJustifyInField(label, LABEL_WIDTH) +
    				    rightJustifyInField(String.valueOf(data), FIELD_WIDTH));
    }
	private void printLine(String label, double data) {
		data = Math.rint(data * 10)/10;
		globals.status2(spaces(INDENT_WIDTH) +
					    leftJustifyInField(label, LABEL_WIDTH) +
					    rightJustifyInField(String.valueOf(data), FIELD_WIDTH));
	}

    // ---------- the tree walking code ---------

    //Get setup to start, initializing the counters.
    private void preamble(EquivRecord j){
		startTime("StratCount", "statistics for entire equivalence class tree");
	}
	
    // Print the results when finished.
    private Counts summary(){
    	// print a label
    	globals.status2(
    		spaces(INDENT_WIDTH + LABEL_WIDTH) + 
		   	rightJustifyInField("Parts", FIELD_WIDTH) +
		   	rightJustifyInField("Wires", FIELD_WIDTH) +
		   	rightJustifyInField("Ports", FIELD_WIDTH));
		printLine("# mismatched equiv classes", numMismatchedLeafRecs);
		printLine("# matched equiv classes", numMatchedLeafRecs);
		printLine("# active equiv classes", numActiveLeafRecs);
		printLine("# mismatched net objects", numMismatchedNetObjs);
		printLine("# matched net objects", numMatchedNetObjs);
		printLine("# active net objects", numActiveNetObjs);

    	int numEquivRecs = numMismatchedLeafRecs.getSumForAllTypes() +
    					   numMatchedLeafRecs.getSumForAllTypes() +
    					   numActiveLeafRecs.getSumForAllTypes();
		globals.status2("");
		sizeHistogram.print();
		
		globals.status2("");
		//printLine("# InternalRecords", numInternalRecs);
		//printLine("# Circuits", numCircuits);
		printLine("max tree depth", maxDepth);
		//if (maxDepth>1000)  System.out.println("Tell Russell that max tree depth = "+maxDepth);

//        float average= (float)totalEqGrpSize/(float)numEquivRecs;
//		printLine("average size EquivRec", average);
//        printLine("# pins", numberOfWireConnections);
//
//		// # Wire pins must = # Part pins unless we're checking just the Wire 
//		// sub-tree or just the Part sub-tree.
//		int diffPins = numberOfPartConnections - numberOfWireConnections;
//		int numWires =  numMismatchedNetObjs.get(NetObject.Type.WIRE) + 
//						numRetiredNetObjs.get(NetObject.Type.WIRE) +
//						numActiveNetObjs.get(NetObject.Type.WIRE); 
//		int numParts =  numMismatchedNetObjs.get(NetObject.Type.PART) + 
//						numRetiredNetObjs.get(NetObject.Type.PART) +
//						numActiveNetObjs.get(NetObject.Type.PART); 
//		error(numWires!=0 && numParts!=0 && diffPins!=0, 
//			  "#wirePins != #partPins: "+
//			  numberOfWireConnections+" != "+numberOfPartConnections);
//		
//        average= (float)numberOfWireConnections/(float)numWires;
//        double rms= (float)numberOfWireConnectionsSquared/(float)numWires;
//        rms= Math.sqrt(rms);
//        printLine("average wire pins", average);
//        printLine("rms wire pins", rms);
        elapsedTime();
        return new Counts(numMatchedLeafRecs.get(NetObject.Type.PART),
        		          numMatchedLeafRecs.get(NetObject.Type.WIRE),
						  numMismatchedLeafRecs.get(NetObject.Type.PART),
        		          numMismatchedLeafRecs.get(NetObject.Type.WIRE),
        		          numActiveLeafRecs.get(NetObject.Type.PART),
        		          numActiveLeafRecs.get(NetObject.Type.WIRE));
    }

	
    public LeafList doFor(EquivRecord j){
    	if (j.isLeaf()) {
			doEquivRec((EquivRecord)j);
	    } else {
			numInternalRecs++; 
	    }
		return super.doFor(j);
    }
	
	private void doEquivRec(EquivRecord er){
		int erSize = er.maxSize();
		totalEqGrpSize += erSize;

		netObjType = er.getNetObjType();
		int numNetObjs = er.numNetObjs();
		if (er.isMismatched()) {
			numMismatchedLeafRecs.incr(netObjType, 1);
			numMismatchedNetObjs.incr(netObjType, numNetObjs);
			sizeHistogram.incr(netObjType, erSize);
		} else if (er.isMatched()) {
			numMatchedLeafRecs.incr(netObjType, 1);
			numMatchedNetObjs.incr(netObjType, numNetObjs);
			sizeHistogram.incr(netObjType, erSize);
		} else {
			numActiveLeafRecs.incr(netObjType, 1);
			numActiveNetObjs.incr(netObjType, numNetObjs);
			sizeHistogram.incr(netObjType, erSize);
		}
		numCircuits += er.numCircuits();
	}
	
    /** 
	 * doFor(NetObject) counts Wires and Parts.
	 * It computes the total and the average number of connections on a Wire
	 * and the root mean square (RMS) average number of connections.
	 * It sums the total number of connections on Parts.
	 * @return null.
	 */
    public Integer doFor(NetObject n){
    	error(n.getNetObjType()!=netObjType, "mixed type leaf record");
        maxDepth = Math.max(maxDepth, getDepth());
        if (n instanceof Wire) {
        	doFor((Wire)n); 
       	} else if (n instanceof Part){
       		doFor((Part)n); 
       	} else {
			error(!(n instanceof Port), "expecting Port");
       	}

       	return CODE_NO_CHANGE;
    }

    private void doFor(Wire w){
        int n= w.numParts();
        numberOfWireConnections += n;
        float nf= n;
        numberOfWireConnectionsSquared += nf*nf;
    }

    private void doFor(Part p){
        numberOfPartConnections += p.getNumWiresConnected();
    }
    
    // ----------------------- intended interface -----------------------------
	public static Counts doYourJob(NccGlobals globals) {
		EquivRecord root = globals.getRoot();
		StratCount jsc = new StratCount(globals);
		jsc.preamble(root);
		jsc.doFor(root);
		return jsc.summary();
	}
    
//	public static LeafList doYourJob(RecordList g,
//										 NccGlobals globals){
//		StratCount jsc = new StratCount(globals);
//		jsc.preamble(g);
//		if (g.size()!=0) {
//			jsc.doFor(g);
//			jsc.summary();
//		}
//		return new LeafList();
//	}

}

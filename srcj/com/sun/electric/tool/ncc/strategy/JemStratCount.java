/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratCount.java
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
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.jemNets.*;

/**
 * JemStratCount counts and prints a tree's content.  JemStratCount is
 * a JemStratNone strategy, producing no offspring.  JemStratCount
 * implements a doFor method for each level in the JemTree structure,
 * keeping counters of what it finds.  JemStratCount uses the
 * tree-traversal methods of JemStratNone.  JemStratCount serves as
 * the model for other JemStratNone classes.
*/
public class JemStratCount extends JemStratNone {
    private int maxDepth; //depth in the tree
    private int numberOfHistoryRecords;
    private int numberOfEquivRecords;
    private int largestEqRec;
    private int smallestEqRec= Integer.MAX_VALUE;
    private int totalEqGrpSize;
    private int numberOfCircuits;
    private int numberOfWires;
    private int numberOfParts;
    private int numberOfWireConnections;
    private long numberOfWireConnectionsSquared;
    private int numberOfPartConnections;
	private int totalWorkDone;
	private String workingOnString;
	private JemRecord portsRecord;

    private JemStratCount(JemRecord portsRecord){}

	/**
	 * Count things in the tree rooted at j.
	 * @param j root of the tree 
	 * @param portsRecordToSkip the Ports JemRecord. Don't count items in this tree. 
	 * @return an empty list
	 */
    public static JemEquivList doYourJob(JemRecord j, JemRecord portsRecordToSkip) {
    	JemStratCount jsc = new JemStratCount(portsRecordToSkip);
    	jsc.preamble(j);
    	JemEquivList el = jsc.doFor(j);
    	jsc.summary();
    	return el;
    }
    
    public static JemEquivList doYourJob(JemRecordList g, 
    									 JemRecord portsRecordToSkip){
	    JemStratCount jsc = new JemStratCount(portsRecordToSkip);
		jsc.preamble(g);
		JemEquivList el= jsc.doFor(g);
		jsc.summary();
		return el;
	}

    // ---------- the tree walking code ---------

    //Get setup to start, initializing the counters.
    private void preamble(JemParent j){
		workingOnString= j.nameString();
		startTime("JemStratCount", workingOnString);
	}
	
    //Get setup to start, initializing the counters.
    private void preamble(JemRecordList j){
		workingOnString= "a list of " + j.size();
		startTime("JemStratCount", workingOnString);
	}
	
    //Print the results when finished.
    private void summary(){
        if(getDepth() != 0)return;
        String s =
		  "   "+numberOfHistoryRecords+" JemHistoryRecords\n"+
		  "   "+numberOfEquivRecords+  " JemEquivRecords\n"+ 
		  "   "+numberOfCircuits+" JemCircuits\n"+
		  "   "+totalWorkDone+" work done";
        getMessenger().line(workingOnString +
                            " with max depth= " + maxDepth + " has");
		getMessenger().line(s);
        float average= (float)totalEqGrpSize/(float)numberOfEquivRecords;
		getMessenger().line(
		  "   "+smallestEqRec+" size( >1 )smallest EquivRec\n"+
		  "   "+largestEqRec+" size largest EquivRec\n"+
		  "   "+average+" average size EquivRec");
        getMessenger().line(
		  "   "+numberOfWires+" Wires\n"+
		  "   "+numberOfWireConnections+" Wire pins\n" +
		  "   "+numberOfParts+" Parts\n"+
		  "   "+numberOfPartConnections+" Part pins");
		int diffPins = numberOfPartConnections - numberOfWireConnections;
		if (diffPins!=0) getMessenger().error("#wirePins != #partPins");
		
        average= (float)numberOfWireConnections/(float)numberOfWires;
        double rms= (float)numberOfWireConnectionsSquared/(float)numberOfWires;
        rms= Math.sqrt(rms);
        getMessenger().say("   average wire pins= " + average);
        getMessenger().line("; rms wire pins= " + rms);
        average= (float)totalWorkDone/(float)numberOfHistoryRecords;
        getMessenger().line("   average actions per offspring= " + average);
        elapsedTime();
    } //end of summary

	
    /** 
	 * doFor(JemHistoryRecord) walks the tree starting at a JemHistoryRecord.
	 * It depends on the tree traversal code in JemStratNone.
	 * It counts whatever it finds.
	 * @return null.
	 */
    public JemEquivList doFor(JemRecord j){
    	if (j==portsRecord) {
			// Don't count things in the Ports subtree
    		return new JemEquivList(); 
    	} else if(j instanceof JemHistoryRecord) {
			numberOfHistoryRecords++; 
	    } else {
	    	if (!(j instanceof JemEquivRecord)) 
	    	    getMessenger().error("unrecognized JemRecord");
	    	doJemEquiv((JemEquivRecord)j);
	    }
		return super.doFor(j);
    }
	
	private void doJemEquiv(JemEquivRecord er){
		numberOfEquivRecords++;
		totalWorkDone += er.getWorkDone();
		largestEqRec = Math.max(largestEqRec, er.maxSize());
		totalEqGrpSize += er.maxSize();
		if(er.maxSize() > 1){
			smallestEqRec = Math.min(smallestEqRec, er.maxSize());
		}
	}
	
    /** 
	 * doFor(JemCircuit) walks the tree starting at a JemCircuit.
	 * It depends on the tree traversal code in JemStratNone.
	 * It counts whatever it finds.
	 * @return null.
	 */
    public JemCircuitMap doFor(JemCircuit j){
        numberOfCircuits++;
		return super.doFor(j);
    }

    /** 
	 * doFor(NetObject) counts Wires and Parts.
	 * It computes the total and the average number of connections on a Wire
	 * and the root mean square (RMS) average number of connections.
	 * It sums the total number of connections on Parts.
	 * @return null.
	 */
    public Integer doFor(NetObject n){
        if(getDepth()>maxDepth)maxDepth= getDepth();
        if(n instanceof Wire)return doFor((Wire)n);
        if(n instanceof Part)return doFor((Part)n);
        return null;
    }

    // ---------- for Wire -------------

    private Integer doFor(Wire w){
        numberOfWires++;
        int n= w.size();
        numberOfWireConnections += n;
        float nf= n;
        numberOfWireConnectionsSquared += nf*nf;
        return null;
    }

    // ---------- for Part -------------

    private Integer doFor(Part p){
        numberOfParts++;
        int n= p.size();
        numberOfPartConnections += n;
        return null;
    }

}

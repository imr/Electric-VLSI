/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccGlobals.java
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

/* 
 * NccGlobals holds all the global state for an Ncc run.  All class
 * members that were previously static have been moved to this
 * class. This allows an Ncc run to be completely thread safe.
 */
package com.sun.electric.tool.ncc;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NodableNameProxy;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.basic.CellContext;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.netlist.NccNetlist;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.netlist.Wire;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;
import com.sun.electric.tool.ncc.trees.LeafEquivRecords;
import com.sun.electric.tool.user.ncc.NccGuiInfo;

/**
 * Generate non-recurring random integers
 */
class NccRandom {
	private Random randGen = new Random(204);
	private HashSet<Integer> randoms = new HashSet<Integer>();

	public int next() {
		while (true) {
			Integer r = new Integer(randGen.nextInt());
			if (!randoms.contains(r)) {
				randoms.add(r);
				return r.intValue();
			}
		}
	}
}

public class NccGlobals implements NccGlobalsReportable {
	// ---------------------------- private data ------------------------------
	private static final int CODE_PART= 0;
	private static final int CODE_WIRE= 1;
	private static final int CODE_PORT= 2;
	/** used to assign new hash code values to EquivRecords */ 
	private NccRandom randGen = new NccRandom();
	/** all options controlling an Ncc run */ private final NccOptions options;
	/** object says when user wants abort */  private final Aborter aborter;

    /** root of the EquivRecord tree */       private EquivRecord root;
    /** subtree holding parts */              private EquivRecord parts;
    /** subtree holding wires */              private EquivRecord wires;
    /** subtree holding ports */              private EquivRecord ports;
	/** root Cell of each netlist */  		  private Cell[] rootCells;
	/** VarContext of root of each netlist */ private VarContext[] rootContexts;
	/** pass number shared by strategies */   public int passNumber;
	/** leaf nodes of parts tree */           private LeafEquivRecords partLeafRecs;
	/** leaf nodes of wires tree */           private LeafEquivRecords wireLeafRecs;
	/** leaf nodes of ports tree */			  private LeafEquivRecords portLeafRecs;
    /** can't build netlist? */               private boolean[] cantBuildNetlist;
    /** mismatches displayed by GUI */        private NccGuiInfo nccGuiInfo;

    
	// ----------------------------- private methods --------------------------
	private List getNetObjs(int code, NccNetlist nets) {
		switch (code) {
		  case CODE_PART:  return nets.getPartArray();
		  case CODE_WIRE:  return nets.getWireArray(); 
		  case CODE_PORT:  return nets.getPortArray();
		}
		error("invalid code");
		return null;
	}	
	
	private void countNetObjs(int[] counts, Iterator<EquivRecord> it) {
		while (it.hasNext()) {
			EquivRecord er = it.next();
			error(!er.isLeaf(), "Must be leaf");
			int numCkts = er.numCircuits();
			error(counts.length!=numCkts, "different number of Circuits");
			Iterator<Circuit> it2 = er.getCircuits();
			for (int i=0; i<numCkts; i++) {
				counts[i] += it2.next().numNetObjs(); 
			}
		}
	}

	private int[] getNetObjCounts(LeafEquivRecords leaves) {
    	int[] counts = new int[getNumNetlistsBeingCompared()];
    	countNetObjs(counts, leaves.getMatched());
    	countNetObjs(counts, leaves.getNotMatched());
    	return counts;
	}
	
	private EquivRecord buildEquivRec(int code, List<NccNetlist> nccNets) {
		boolean atLeastOneNetObj = false;
		List<Circuit> ckts = new ArrayList<Circuit>();
		for (NccNetlist nets : nccNets) {
			List<NetObject> netObjs = getNetObjs(code, nets);
			if (netObjs.size()!=0)  atLeastOneNetObj = true;
			ckts.add(Circuit.please(netObjs));
		}
		if (!atLeastOneNetObj) return null;
		return EquivRecord.newLeafRecord(code, ckts, this);
	}
	
	// ----------------------------- public methods --------------------------
	/**
	 * The constructor initializes global root, parts, wires, and ports from 
	 * net lists. 
	 * param list of NccNetlists, one per Cell to be compared
	 * @param options 
	 */
	public NccGlobals(NccOptions options, Aborter aborter) {
		this.options = options;
		this.aborter = aborter;
        nccGuiInfo = new NccGuiInfo();
	}
	public void prln(String s) {System.out.println(s); System.out.flush();}
	public void pr(String s) {System.out.print(s); System.out.flush();}

	public void setInitialNetlists(List<NccNetlist> nccNets) {
		parts = buildEquivRec(CODE_PART, nccNets);
		wires = buildEquivRec(CODE_WIRE, nccNets);
		ports = buildEquivRec(CODE_PORT, nccNets);

		List<EquivRecord> el = new ArrayList<EquivRecord>();
		if (parts!=null) el.add(parts); 
		if (wires!=null) el.add(wires); 
		if (ports!=null) el.add(ports);
		root = EquivRecord.newRootRecord(el);
		
		rootCells = new Cell[nccNets.size()];
		rootContexts = new VarContext[nccNets.size()];
		cantBuildNetlist = new boolean[nccNets.size()];
		int i=0;
		for (Iterator<NccNetlist> it=nccNets.iterator(); it.hasNext(); i++) {
			NccNetlist nl = it.next();
			rootCells[i] = nl.getRootCell();
			rootContexts[i] = nl.getRootContext();
            cantBuildNetlist[i] = nl.cantBuildNetlist();
		}
	}
	/** Initialization.
	 * Tricky! initLeafLists() must be called AFTER series/parallel merging.
	 * and AFTER Local Partitioning!!! Both of those processes violate 
	 * invariants assumed by LeafEquivRecords. */
	public void initLeafLists() {
		partLeafRecs = new LeafEquivRecords(parts, this);
		wireLeafRecs = new LeafEquivRecords(wires, this);
		portLeafRecs = new LeafEquivRecords(ports, this);
	}
	
	public EquivRecord getRoot() {return root;}
	public EquivRecord getParts() {return parts;}
	public EquivRecord getWires() {return wires;}
	public EquivRecord getPorts() {return ports;}
	public int getNumNetlistsBeingCompared() {return rootCells.length;}
	public Cell[] getRootCells() {return rootCells;}
	public VarContext[] getRootContexts() {return rootContexts;}
	public String[] getRootCellNames() {
		String[] rootCellNames = new String[rootCells.length];
		for (int i=0; i<rootCells.length; i++) {
			rootCellNames[i] = NccUtils.fullName(rootCells[i]);
		}
		return rootCellNames;
	}
	public void status1(String msg) {
		if (options.howMuchStatus>=1) prln(msg);
	}
	public void status2(String msg) {
		if (options.howMuchStatus>=2) prln(msg); 
	}
	public void flush() {System.out.flush();}
	public void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	public void error(String msg) {LayoutLib.error(true, msg);}
	
	public NccOptions getOptions() {return options;}
	
	/** Generate non-recurring pseudo-random integers */
	public int getRandom() {return randGen.next();}
	
	public LeafEquivRecords getPartLeafEquivRecs() {return partLeafRecs;}
	public LeafEquivRecords getWireLeafEquivRecs() {return wireLeafRecs;}
	public LeafEquivRecords getPortLeafEquivRecs() {return portLeafRecs;}

	/** @return an NetNameProxy[][]. NetNameProxy[d][n] gives the nth net of
	 * the dth design.  NetNameProxy[a][n] is NCC equivalent to NetNameProxy[b][n]
	 * for all a and b.*/
	public NetNameProxy[][] getEquivalentNets() {
		int numDes = getNumNetlistsBeingCompared();
		NetNameProxy[][] equivNets = new NetNameProxy[numDes][];
		int numMatched = wireLeafRecs.numMatched();
		for (int i=0; i<numDes; i++) {
			equivNets[i] = new NetNameProxy[numMatched];
		}
		int wireNdx = 0;
		for (Iterator<EquivRecord> it=wireLeafRecs.getMatched(); it.hasNext(); wireNdx++) {
			EquivRecord er = it.next();
			int cktNdx = 0;
			for (Iterator<Circuit> cit=er.getCircuits(); cit.hasNext(); cktNdx++) {
				Circuit ckt = cit.next();
				LayoutLib.error(ckt.numNetObjs()!=1, "not matched?");
				Wire w = (Wire) ckt.getNetObjs().next();
				equivNets[cktNdx][wireNdx] = w.getNameProxy().getNetNameProxy();
			}
		}
		return equivNets;
	}
    
	/** @return an NodableNameProxy[][]. NodableNameProxy[d][n] gives the nth net of
	 * the dth design.  NetNameProxy[a][n] is NCC equivalent to NetNameProxy[b][n]
	 * for all a and b.*/
	public NodableNameProxy[][] getEquivalentNodes() {
		int numDes = getNumNetlistsBeingCompared();
		NodableNameProxy[][] equivParts = new NodableNameProxy[numDes][];
		int numMatched = partLeafRecs.numMatched();
		for (int i=0; i<numDes; i++) {
			equivParts[i] = new NodableNameProxy[numMatched];
		}
		int partNdx = 0;
		for (Iterator<EquivRecord> it=partLeafRecs.getMatched(); it.hasNext(); partNdx++) {
			EquivRecord er = it.next();
			int cktNdx = 0;
			for (Iterator<Circuit> cit=er.getCircuits(); cit.hasNext(); cktNdx++) {
				Circuit ckt = cit.next();
				LayoutLib.error(ckt.numNetObjs()!=1, "not matched?");
				Part p = (Part) ckt.getNetObjs().next();
				equivParts[cktNdx][partNdx] = p.getNameProxy().getNodableNameProxy();
			}
		}
		return equivParts;
	}
    
    /** Get mismatches to be displayed in the GUI
     * @return an object with mismatches to be displayed in GUI */
    public NccGuiInfo getNccGuiInfo() {return nccGuiInfo;}
    
    /** @return true if some netlist can't be built */
    public boolean cantBuildNetlist() {
    	for (int i=0; i<cantBuildNetlist.length; i++) 
    		if (cantBuildNetlist[i]) return true;
    	return false; 
    }
    /** @return true if ith netlist can't be built */
    public boolean[] cantBuildNetlistBits() {return cantBuildNetlist;}
    
	/** @return array of Part counts. One array element per Circuit */
    public int[] getPartCounts() { return getNetObjCounts(partLeafRecs); }
	/** @return array of Wire counts. One array element per Circuit */
    public int[] getWireCounts() { return getNetObjCounts(wireLeafRecs); }
	/** @return array of Port counts. One array element per Circuit */
    public int[] getPortCounts() { return getNetObjCounts(portLeafRecs); }
    /** @return true if user wants to abort */
    public boolean userWantsToAbort() {return aborter.userWantsToAbort();}
}

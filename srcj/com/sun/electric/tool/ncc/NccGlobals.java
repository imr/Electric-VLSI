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
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.jemNets.NccNetlist;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/**
 * Generate non-recurring random integers
 */
class NccRandom {
	private Random randGen = new Random(204);
	private HashSet randoms = new HashSet();

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

public class NccGlobals {
	// ---------------------------- private data ------------------------------
	private static final int CODE_PART= 0;
	private static final int CODE_WIRE= 1;
	private static final int CODE_PORT= 2;
	/** used to assign new hash code values to EquivRecords */ 
	private NccRandom randGen = new NccRandom();
	/** all options controlling an Ncc run */ private final NccOptions options;
	/** printing object */                    private final Messenger messenger;

    /** root of the EquivRecord tree */    private EquivRecord root;
    /** subtree holding parts */              private EquivRecord parts;
    /** subtree holding wires */              private EquivRecord wires;
    /** subtree holding ports */              private EquivRecord ports;
	/** root Cell of each netlist */  		  private Cell[] rootCells;
	/** pass number shared by strategies */   public int passNumber;
	
	
	private List getNetObjs(int code, NccNetlist nets) {
		switch (code) {
		  case CODE_PART:  return nets.getPartArray();
		  case CODE_WIRE:  return nets.getWireArray(); 
		  case CODE_PORT:  return nets.getPortArray();
		}
		messenger.error(true, "invalid code");
		return null;
	}
	
	// ----------------------------- private methods --------------------------
	private EquivRecord buildEquivRec(int code, List nccNets) {
		boolean atLeastOneNetObj = false;
		List ckts = new ArrayList();
		for (Iterator it=nccNets.iterator(); it.hasNext();) {
			NccNetlist nets = (NccNetlist) it.next();
			List netObjs = getNetObjs(code, nets);
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
	 * @param list of NccNetlists, one per Cell to be compared
	 * @param options 
	 */
	public NccGlobals(NccOptions options) {
		this.options = options;
		this.messenger = new Messenger(false);
	}
	public void setInitialNetlists(List nccNets) {
		parts = buildEquivRec(CODE_PART, nccNets);
		wires = buildEquivRec(CODE_WIRE, nccNets);
		ports = buildEquivRec(CODE_PORT, nccNets);

		List el = new ArrayList();
		if (parts!=null) el.add(parts);
		if (wires!=null) el.add(wires);
		if (ports!=null) el.add(ports);

		root = EquivRecord.newRootRecord(el);
		
		rootCells = new Cell[nccNets.size()];
		int i=0;
		for (Iterator it=nccNets.iterator(); it.hasNext(); i++) {
			NccNetlist nl = (NccNetlist) it.next();
			rootCells[i] = nl.getRootCell();
		}
	}
	
	public EquivRecord getRoot() {return root;}
	public EquivRecord getParts() {return parts;}
	public EquivRecord getWires() {return wires;}
	public EquivRecord getPorts() {return ports;}
	public int getNumNetlistsBeingCompared() {return rootCells.length;}
	public Cell[] getRootCells() {return rootCells;}
	public String[] getRootCellNames() {
		String[] rootCellNames = new String[rootCells.length];
		for (int i=0; i<rootCells.length; i++) {
			rootCellNames[i] = NccUtils.fullName(rootCells[i]);
		}
		return rootCellNames;
	}
	
	public void println(String msg) {
		if (options.verbose) messenger.println(msg);
	}
	public void println() {
		if (options.verbose) messenger.println();
	}
	public void print(String msg) {
		if (options.verbose) messenger.print(msg);
	}
	public void flush() {messenger.flush();}
	public void error(boolean pred, String msg) {
		messenger.error(pred, msg);
	}
	public void error(String msg) {messenger.error(true, msg);}
	
	public NccOptions getOptions() {return options;}
	
	/** Generate non-recurring pseudo-random integers */
	public int getRandom() {return randGen.next();}
	public Messenger getMessenger() {return messenger;}
}

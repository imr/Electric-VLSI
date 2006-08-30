/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccResult.java
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
package com.sun.electric.tool.ncc.result;

import java.io.Serializable;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NodableNameProxy;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.NccGlobalsReportable;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.equivalence.Equivalence;
import com.sun.electric.tool.user.ncc.NccGuiInfo;

/** The result of running a netlist comparison on a single pair of Cells. 
 * Stores all information we need to save for tools using NCC and for GUI. */
public class NccResult implements Serializable {
    public static class CellSummary implements Serializable {
    	static final long serialVersionUID = 0;

    	/* arrays of Part, Port, Wire counts. One array element per Circuit */
	    public final int[] numParts, numPorts, numWires;
	    public final boolean[] cantBuildNetlist;
	    public CellSummary(int[] parts, int[] ports, int[] wires, 
	    		           boolean[] cantBuildNetlistBits) {
	        numParts = parts;
	        numPorts = ports;
	        numWires = wires;
	        cantBuildNetlist = cantBuildNetlistBits;
	    }
	}

	static final long serialVersionUID = 0;

	private final boolean exportMatch, topologyMatch, sizeMatch, userAbort;
	private Cell[] rootCells;
	private String[] rootCellNames;
	private VarContext[] rootContexts; 
	private Equivalence equivalence;
	private NccGuiInfo nccGuiInfo;
	private NccOptions options;
	private CellSummary summary;
	
	private Equivalence buildNetEquivalence(NccGlobalsReportable globalData) {
		NetNameProxy[][] equivNets;
		NodableNameProxy[][] equivNodes;
		if (globalData==null) {
			// For severe netlist errors, NCC doesn't even construct globalData.
			// Create a NetEquivalence that has no matching nets.
			equivNets = new NetNameProxy[2][];
			equivNets[0] = new NetNameProxy[0];
			equivNets[1] = new NetNameProxy[0];
			equivNodes = new NodableNameProxy[2][];
			equivNodes[0] = new NodableNameProxy[0];
			equivNodes[1] = new NodableNameProxy[0];
		} else {
			equivNets = globalData.getEquivalentNets();
			equivNodes = globalData.getEquivalentNodes();
		}
		return new Equivalence(equivNets, equivNodes, rootCells, rootContexts);
	}
	
	private NccResult(boolean exportNameMatch, boolean topologyMatch, 
			          boolean sizeMatch, boolean userAbort, NccGlobalsReportable globalData) {
		this.exportMatch = exportNameMatch;
		this.topologyMatch = topologyMatch;
		this.sizeMatch = sizeMatch;
		this.userAbort = userAbort; 
		if (userAbort) return;

		rootCells = globalData.getRootCells();
		rootCellNames = globalData.getRootCellNames();
		rootContexts = globalData.getRootContexts();
		equivalence = buildNetEquivalence(globalData);
		nccGuiInfo = globalData.getNccGuiInfo();
		options = globalData.getOptions();
        summary = new CellSummary(globalData.getPartCounts(),
                				  globalData.getPortCounts(),
                				  globalData.getWireCounts(),
                				  globalData.cantBuildNetlistBits());
	}
	public static NccResult newResult(boolean exportNameMatch, 
            boolean topologyMatch, 
            boolean sizeMatch, 
            NccGlobalsReportable globalData) {
			return new NccResult(exportNameMatch, topologyMatch, sizeMatch, false, globalData);
	}

//	private Equivalence buildNetEquivalence(com.sun.electric.plugins.pie.NccGlobals globalData) {
//		NetNameProxy[][] equivNets;
//		NodableNameProxy[][] equivNodes;
//		if (globalData==null) {
//			// For severe netlist errors, NCC doesn't even construct globalData.
//			// Create a NetEquivalence that has no matching nets.
//			equivNets = new NetNameProxy[2][];
//			equivNets[0] = new NetNameProxy[0];
//			equivNets[1] = new NetNameProxy[0];
//			equivNodes = new NodableNameProxy[2][];
//			equivNodes[0] = new NodableNameProxy[0];
//			equivNodes[1] = new NodableNameProxy[0];
//		} else {
//			equivNets = globalData.getEquivalentNets();
//			equivNodes = globalData.getEquivalentNodes();
//		}
//		return new Equivalence(equivNets, equivNodes, rootCells, rootContexts);
//	}
	
//	private NccResult(boolean exportNameMatch, boolean topologyMatch, 
//	          		  boolean sizeMatch, boolean userAbort, 
//	          		  com.sun.electric.plugins.pie.NccGlobals globalData) {
//		this.exportMatch = exportNameMatch;
//		this.topologyMatch = topologyMatch;
//		this.sizeMatch = sizeMatch;
//		this.userAbort = userAbort; 
//		if (userAbort) return;
//
//		rootCells = globalData.getRootCells();
//		rootCellNames = globalData.getRootCellNames();
//		rootContexts = globalData.getRootContexts();
//		equivalence = buildNetEquivalence(globalData);
//		nccGuiInfo = globalData.getNccGuiInfo();
//		options = globalData.getOptions();
//		summary = new CellSummary(globalData.getPartCounts(),
//      				  			  globalData.getPortCounts(),
//      				  			  globalData.getWireCounts(),
//      				  			  globalData.cantBuildNetlistBits());
//	}
//	public static NccResult newResult(boolean exportNameMatch, 
//									  boolean topologyMatch,
//									  boolean sizeMatch,
//									  com.sun.electric.plugins.pie.NccGlobals globalData) {
//        return new NccResult(exportNameMatch, topologyMatch, sizeMatch, false, globalData);
//	}

	public static NccResult newUserAbortResult() {
		return new NccResult(false, false, false, true, (NccGlobals)null);
	}
	/** return array of the top-level Cells being compared */
	public Cell[] getRootCells() {return rootCells;}
	
	public String[] getRootCellNames() {return rootCellNames;}
	
	/** return array of the VarContexts for the top-level Cells begin 
	 * compared */
	public VarContext[] getRootContexts() {return rootContexts;}
	
	public NccOptions getOptions() {return options;}
	
	public CellSummary getCellSummary() {return summary;}

	/** No problem was found with Exports */ 
	public boolean exportMatch() {return exportMatch;}
	
	/** No problem was found with the network topology */
	public boolean topologyMatch() {return topologyMatch;}

	/** No problem was found with transistor sizes */
	public boolean sizeMatch() {return sizeMatch;}
	
	/** User aborted this comparison. No other information is saved. */
	public boolean userAbort() {return userAbort;}
	
	/** No problem was found */
	public boolean match() {return exportMatch && topologyMatch && sizeMatch;}
	
	/** return object that maps between Nodes and Networks in the two designs */
	public Equivalence getEquivalence() {return equivalence;}
	
	public String summary(boolean checkSizes) {
		String s;
		if (exportMatch) {
			s = "  exports match, ";
		} else {
			s = "exports mismatch, ";
		}
		if (topologyMatch) {
			s += "topologies match, ";
		} else {
			s += "topologies mismatch, ";
		}
		if (!checkSizes) {
			s += "sizes not checked";
		} else if (sizeMatch) {
			s += "sizes match";
		} else {
			s += "sizes mismatch";
		}
		return s;
	}
	
	/** The GUI needs to display this result to the user */
	public boolean guiNeedsToReport() {
		if (userAbort) return false;
		return !exportMatch || !topologyMatch || !sizeMatch;
	}

	/** return information saved specifically for the GUI */
    public NccGuiInfo getNccGuiInfo() {return nccGuiInfo;}
}

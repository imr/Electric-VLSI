/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: NccComparisonMismatches.java
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
package com.sun.electric.tool.user.ncc;
   
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.processing.LocalPartitionResult;
import com.sun.electric.tool.ncc.trees.EquivRecord;
import com.sun.electric.tool.ncc.trees.LeafEquivRecords;

/**
 * This class is a container for all NCC mismatch data produced by 
 * a comparison of two cells. This data is required by NCC GUI.
 */
public class NccComparisonMismatches {
    /** True if exports match      */   private boolean exportMatch;
    /** True if topologies match   */   private boolean topologyMatch;
    /** True if sizes match        */   private boolean sizeMatch;
    /** True if sizes were checked */   private boolean sizeChecked;
    
    /** True if top-level cells are swapped. Schematic cell is always 
     *  attempted to be displayed on the laft and the layout cell - on the right.
     *  This variable should only be used for data in EquivRecords and for size 
     *  data. Exports and hashcodes have to compare their cell names to the cell
     *  names stored in their object of this class. Objects of this class have 
     *  cell names, cells, and contexts in the correct order. */    
    private boolean swapCells;  
    
    /** Names of the two cells     */   private String[] cellNames;
    /** The two cells              */   private Cell[] cells;
    /** Contexts of the two cells  */   private VarContext[] contexts;
    /** Summary of the two cells   */   private CellSummary summary;
    
    /** Export mismatches          */   private List exportMismatches;
    /** Part/Wire mismatches (local partitioning) */
                                        private LocalPartitionResult lpResult;
    
    /** Part/Wire mismatches (hashcode partitioning) */                                        
                                        private EquivRecord[] hashMismEqvRecrds;    
    /** Transistor size mismatches */   private List sizeMismatches;
    /** Some export mismatches are duplicated when suggestions are added.
     *  Suggestions are given only when topology match. 
     *  This variable holds the total number of mismatches without suggestions
     *  which were duplicated by their corresponding suggestions */
                                        private int numExportsValidOnlyWhenTopologyMismatch;
                                        
    /** Export Assertion Failures */    private List exportAssertionFailures;
    /** Network Export Conflicts  */    private List networkExportConflicts;
    /** Charact Export Conflicts  */    private List charactExportConflicts;
    /** Unrecognized MOSes        */    private List unrecognizedParts;
    /** print hash code errors? */      private boolean printHashFailures;  
    
    public NccComparisonMismatches() {
        exportMatch = true;
        topologyMatch = true;
        sizeMatch = true;
        sizeChecked = false;
        swapCells = false;
        numExportsValidOnlyWhenTopologyMismatch = 0;
        lpResult = null;
        
        exportMismatches = new LinkedList();
        sizeMismatches = new LinkedList();
        exportAssertionFailures = new LinkedList();
        networkExportConflicts = new LinkedList();
        charactExportConflicts = new LinkedList();
        unrecognizedParts = new LinkedList();
    }
    
    /** 
     * This method should be called at the end of an NCC job, when all results 
     * are available. This method performs cell swaps attempting to place 
     * a schematic cell at index 0, and a layout cell at index 1. 
     */
    public void setGlobalData(NccGlobals globals) {
        cellNames = globals.getRootCellNames();
        cells = globals.getRootCells();
        contexts = globals.getRootContexts();        
        if (cellNames[0].indexOf("{sch}") == -1
         && cellNames[1].indexOf("{sch}") != -1) {
            String     s=cellNames[0];cellNames[0]=cellNames[1];cellNames[1]=s;
            Cell       c=cells[0];    cells[0]=cells[1];        cells[1]=c;
            VarContext vc=contexts[0];contexts[0]=contexts[1];  contexts[1]=vc;
            swapCells = true;
        } else
            swapCells = false;
        
        sizeChecked = globals.getOptions().checkSizes;
        printHashFailures = globals.isPrintHashFailures();
        
        if (lpResult == null || lpResult.matches()) {  // if no LP mismatches
            LeafEquivRecords parts = globals.getPartLeafEquivRecs();
            LeafEquivRecords wires = globals.getWireLeafEquivRecs();
            hashMismEqvRecrds = new EquivRecord[parts.numNotMatched() 
                                              + wires.numNotMatched()];
            int i=0;
            for (Iterator it=parts.getNotMatched(); it.hasNext(); i++)
                hashMismEqvRecrds[i] = (EquivRecord)it.next();
            for (Iterator it=wires.getNotMatched(); it.hasNext(); i++)
                hashMismEqvRecrds[i] = (EquivRecord)it.next();
        }
        
        summary = new CellSummary(globals.getPartCounts(),
                                  globals.getPortCounts(),
                                  globals.getWireCounts(),
								  globals.cantBuildNetlistBits());
    }
    
    /**
     * This method returns the total number of valid mismatches stored in the object:
     * export mismatches + size mismatches +
     * local partitioning mismatches (or hascode mismaches)
     * @return the total number of valid mismatches stored in the object
     */
    public int getTotalMismatchCount() {
        int eqvRecCount = 0;
        if (lpResult != null && !lpResult.matches()) 
            eqvRecCount += lpResult.size();
        else if (hashMismEqvRecrds != null && printHashFailures) 
            eqvRecCount += hashMismEqvRecrds.length;
        
        return getValidExportMismatchCount() 
               + eqvRecCount   
               + sizeMismatches.size()
               + exportAssertionFailures.size()
               + networkExportConflicts.size()
               + charactExportConflicts.size()
               + unrecognizedParts.size();
    }
    
    /**
     * This method returns true if two cells stored in this object were 
     * swapped and, therefore, all internal data (exports, partitions, sizes) 
     * have to be swapped as well
     * @return true if two cells stored in this object were swapped
     */
    public boolean isSwapCells() {
        return swapCells;
    }
    
    /**
     * This method returns an array of two Strings representing the names of the 
     * compared cells 
     * @return an array of cell names
     */
    public String[] getNames() {
        return cellNames;
    }
    
    /**
     * This method returns an array of two compared cells 
     * @return an array of cell
     */    
    public Cell[] getCells() {
        return cells;
    }
    
    /**
     * This method returns an array of two cell contexts 
     * @return an array cell contexts
     */    
    public VarContext[] getContexts() {
        return contexts;
    }

    /**
     * This method returns a CellSummary object holding number of parts, 
     * wires, and ports in each cell. 
     * @return a CellSummary with summary of the compared cells
     */    
    public CellSummary getCellSummary() {
        return summary;
    }    
    
    /**
     * This method adds the provided ExportMismatch object to the list 
     * of export mismatches
     * @param em export mismatch to add to the list of export mismatches 
     */
    public void addExportMismatch(ExportMismatch em) {
        exportMismatches.add(em);
        if (em.isValidOnlyWhenTopologyMismatch())
            numExportsValidOnlyWhenTopologyMismatch++;
    }
    
    /**
     * This method returns the list of export mismatches
     * @return list of export mismatches 
     */
    public List getExportMismatches() {
        return exportMismatches;
    }
    
    public void setLocalPartitionResult(LocalPartitionResult lpr) {
        lpResult = lpr;
    }
    public LocalPartitionResult getLocalPartitionResult() {
        return lpResult;
    }
    public EquivRecord[] getHashMismatchedEquivRecords() {
        return hashMismEqvRecrds;
    }
    
    public boolean isHashFailuresPrinted() {
        return printHashFailures; 
    }
    
    public void setMatchFlags(boolean em, boolean tm, boolean sm) {
        exportMatch = em;
        topologyMatch = tm;
        sizeMatch = sm;
    }
    
    public boolean isExportMatch() {
        return exportMatch;
    }

    public boolean isSizeMatch() {
        return sizeMatch;
    }

    public boolean isTopologyMatch() {
        return topologyMatch;
    }

    public boolean isSizeChecked() {
        return sizeChecked;
    }

    public int getValidExportMismatchCount() {
        if (topologyMatch)
            return exportMismatches.size() - numExportsValidOnlyWhenTopologyMismatch;
        return exportMismatches.size();
    }

    public List getSizeMismatches() {
        return sizeMismatches;
    }
    public void setSizeMismatches(List sizeMismatches) {
        this.sizeMismatches = sizeMismatches;
    }
    
    public void addExportAssertionFailure(Cell cell, VarContext context, 
                                          Object[][] items, String[][] names) {
        exportAssertionFailures.add(
                new ExportAssertionFailures(cell, context, items, names));
    }
    public List getExportAssertionFailures() {
        return exportAssertionFailures;
    }
    
    public void addNetworkExportConflict(ExportConflict.NetworkConflict conf) {
        networkExportConflicts.add(conf);
    }
    public List getNetworkExportConflicts() {
        return networkExportConflicts;
    }
    public void addCharactExportConflict(ExportConflict.CharactConflict conf) {
        charactExportConflicts.add(conf);
    }
    public List getCharactExportConflicts() {
        return charactExportConflicts;
    }
    
    public void addUnrecognizedPart(UnrecognizedPart mos) {
        unrecognizedParts.add(mos);
    }
    public List getUnrecognizedParts() {
        return unrecognizedParts;
    }
    
    static class CellSummary {
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
}

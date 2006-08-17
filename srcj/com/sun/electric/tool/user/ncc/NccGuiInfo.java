/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: NccGuiInfo.java
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
   
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.ncc.result.EquivRecReport;
import com.sun.electric.tool.ncc.result.NccResult;
import com.sun.electric.tool.ncc.result.NccResult.CellSummary;
import com.sun.electric.tool.ncc.result.SizeMismatch.Mismatch;
import com.sun.electric.tool.ncc.strategy.StratCheckSizes;

/**
 * This class is a container for all NCC mismatch data produced by 
 * a comparison of two cells. This data is required by NCC GUI.
 */
public class NccGuiInfo  implements Serializable {
	static final long serialVersionUID = 0;

	/** Results from Cell pair
	 *  comparison */					private NccResult nccResult;
    
    /** True if top-level cells are swapped. Schematic cell is always 
     *  attempted to be displayed on the laft and the layout cell - on the right.
     *  This variable should only be used for data in EquivRecords and for size 
     *  data. Exports and hashcodes have to compare their cell names to the cell
     *  names stored in their object of this class. Objects of this class have 
     *  cell names, cells, and contexts in the correct order. */    
    private boolean swapCells;  
    
    /** Export mismatches          */   private List<ExportMismatch> exportMismatches;
    
    /** Transistor size mismatches */   private List<Mismatch> sizeMismatches;
    /** Some export mismatches are duplicated when suggestions are added.
     *  Suggestions are given only when topology match. 
     *  This variable holds the total number of mismatches without suggestions
     *  which were duplicated by their corresponding suggestions */
                                        private int numExportsValidOnlyWhenTopologyMismatch;
                                        
    /** Export Assertion Failures */    private List<ExportAssertionFailures> exportAssertionFailures;
    /** Network Export Conflicts  */    private List<ExportConflict.NetworkConflict> networkExportConflicts;
    /** Charact Export Conflicts  */    private List<ExportConflict.CharactConflict> charactExportConflicts;
    /** Unrecognized MOSes        */    private List<UnrecognizedPart> unrecognizedParts;
    /** Mismatched EquivRecords */		private List<EquivRecReport> partRecReports, wireRecReports;
    
    public NccGuiInfo() {
        swapCells = false;
        numExportsValidOnlyWhenTopologyMismatch = 0;
        
        exportMismatches = new LinkedList<ExportMismatch>();
        sizeMismatches = new LinkedList<Mismatch>();
        exportAssertionFailures = new LinkedList<ExportAssertionFailures>();
        networkExportConflicts = new LinkedList<ExportConflict.NetworkConflict>();
        charactExportConflicts = new LinkedList<ExportConflict.CharactConflict>();
        unrecognizedParts = new LinkedList<UnrecognizedPart>();
        partRecReports = new ArrayList<EquivRecReport>();
        wireRecReports = new ArrayList<EquivRecReport>();
    }

    /** setNccResult should be called after NCC has returned a result */
    public void setNccResult(NccResult r) {nccResult=r;}
    
    /**
     * This method returns the total number of valid mismatches stored in the object:
     * export mismatches + size mismatches +
     * local partitioning mismatches (or hascode mismaches)
     * @return the total number of valid mismatches stored in the object
     */
    public int getTotalMismatchCount() {
        int eqvRecCount = partRecReports.size() + wireRecReports.size();
        
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
    
    /** This method returns an array of two Strings representing the names of the 
     * compared cells 
     * @return an array of cell names*/
    public String[] getNames() {return nccResult.getRootCellNames();}
    
    /** This method returns an array of two compared cells 
     * @return an array of cell */    
    public Cell[] getCells() {return nccResult.getRootCells();}
    
    /** This method returns an array of two cell contexts 
     * @return an array cell contexts */    
    public VarContext[] getContexts() {return nccResult.getRootContexts();}

    /**
     * This method returns a CellSummary object holding number of parts, 
     * wires, and ports in each cell. 
     * @return a CellSummary with summary of the compared cells
     */    
    public CellSummary getCellSummary() {return nccResult.getCellSummary();}    
    
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
    
    /** This method returns the list of export mismatches
     * @return list of export mismatches */
    public List<ExportMismatch> getExportMismatches(){return exportMismatches;}
    public void setPartRecReports(List<EquivRecReport> badParts) {
    	partRecReports = badParts;
    }
    public List<EquivRecReport> getPartRecReports() {return partRecReports;}
    public void setWireRecReports(List<EquivRecReport> badWires) {
    	wireRecReports = badWires;
    }
    public List<EquivRecReport> getWireRecReports() {return wireRecReports;}
    
    /** has hash coding mismatches */
    public boolean isHashFailuresPrinted() {
    	// the reports, if any, are either all local partition or all 
    	// hash code partition 
    	if (partRecReports.size()!=0)  return partRecReports.get(0).hashMismatch();
    	if (wireRecReports.size()!=0)  return wireRecReports.get(0).hashMismatch();

    	// no partition errors at all
    	return false;
    }
    
    public boolean hasLocalPartitionMismatches() {
    	if (partRecReports.size()!=0)  return !partRecReports.get(0).hashMismatch();
    	if (wireRecReports.size()!=0)  return !wireRecReports.get(0).hashMismatch();

    	// no partition errors at all
    	return false;
    }
    
    public boolean isExportMatch() {return nccResult.exportMatch();}

    public boolean isSizeMatch() {return nccResult.sizeMatch();}

    public boolean isTopologyMatch() {return nccResult.topologyMatch();}

    public boolean isSizeChecked() {return nccResult.getOptions().checkSizes;}

    public int getValidExportMismatchCount() {
        if (isTopologyMatch())
            return exportMismatches.size() - numExportsValidOnlyWhenTopologyMismatch;
        return exportMismatches.size();
    }

    public List<Mismatch> getSizeMismatches() {
        return sizeMismatches;
    }
    public void setSizeMismatches(List<Mismatch> sizeMismatches) {
        this.sizeMismatches = sizeMismatches;
    }
    
    public void addExportAssertionFailure(Cell cell, VarContext context, 
                                          Object[][] items, String[][] names) {
        exportAssertionFailures.add(
                new ExportAssertionFailures(cell, context, items, names));
    }
    public List<ExportAssertionFailures> getExportAssertionFailures() {
        return exportAssertionFailures;
    }
    
    public void addNetworkExportConflict(ExportConflict.NetworkConflict conf) {
        networkExportConflicts.add(conf);
    }
    public List<ExportConflict.NetworkConflict> getNetworkExportConflicts() {
        return networkExportConflicts;
    }
    public void addCharactExportConflict(ExportConflict.CharactConflict conf) {
        charactExportConflicts.add(conf);
    }
    public List<ExportConflict.CharactConflict> getCharactExportConflicts() {
        return charactExportConflicts;
    }
    
    public void addUnrecognizedPart(UnrecognizedPart mos) {
        unrecognizedParts.add(mos);
    }
    public List<UnrecognizedPart> getUnrecognizedParts() {
    	return unrecognizedParts;
    } 
}

/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: ExportMismatch.java
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
import java.util.List;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.ncc.result.NetObjReport;
import com.sun.electric.tool.ncc.result.PortReport;
import com.sun.electric.tool.ncc.result.NetObjReport.NetObjReportable;
import com.sun.electric.tool.ncc.result.PortReport.PortReportable;

/**
 * This class is an abstract superclass for Export mismatches
 */ 
public abstract class ExportMismatch implements Serializable {
   
    /** Cell names     */ protected String desingNames[] = new String[2];
    /** Compared Cells */ protected Cell[] cells = new Cell[2];
    /** Conetexts      */ protected VarContext[] contexts = new VarContext[2];
    
    /** is this a topology mismatch? */ protected boolean topologyMatch;
    /** is this a name mismatch?     */ protected boolean nameMatch;
    
    /** is this mismatch valid only when topology mismatch? 
     *  Used to avoid duplication with suggested matches which are 
     *  given when topology matches */
    protected boolean validOnlyWhenTopologyMismatch;

    public ExportMismatch() { init(); }
    
    public ExportMismatch(String name1, String name2) {
        desingNames[0] = name1;
        desingNames[1] = name2;
        init();
    }
    
    private void init() {
        topologyMatch = true;
        nameMatch = true; 
        validOnlyWhenTopologyMismatch = false;        
    }
    
    /**
     * Get Cell name
     * @param index  Cell index: 0 or 1
     * @return Cell name for the given index or null if index is invalid
     */
    public String getName(int index) {
        if (index != 0 && index != 1) return null;
        return desingNames[index];
    }
    
    /**
     * Set names of the compared Cels (designs). Has no effect on the names 
     * stored in the Cells themselves.
     * @param name1  new name for the first design 
     * @param name2  new name for the second design
     */
    public void setNames(String name1, String name2) {
        desingNames[0] = name1;
        desingNames[1] = name2;
    }    
    
    /**
     * Get Cell with the given design index.
     * @param index  design index: 0 or 1
     * @return  Cell with the given index or null if index is invalid
     */
    public Cell getCell(int index) {
        if (index != 0 && index != 1) return null;
        return cells[index];
    }
    
    /**
     * Set compared Cells (designs). 
     * @param cell1  new first design Cell 
     * @param cell2  new second design Cell
     */
    public void setCells(Cell cell1, Cell cell2) {
        cells[0] = cell1;
        cells[1] = cell2;
    }    

    /**
     * Get Context with the given design index.
     * @param index  design index: 0 or 1
     * @return  Context with the given index or null if index is invalid
     */
    public VarContext getContext(int index) {
        if (index != 0 && index != 1) return null;        
        return contexts[index];
    }

    /**
     * Set Contexts of the compared Cells (designs). 
     * @param cnxt1  new Context for the first Cell 
     * @param cnxt2  new Context for the second Cell
     */
    public void setContexts(VarContext cnxt1, VarContext cnxt2) {
        contexts[0] = cnxt1;
        contexts[1] = cnxt2;
    }
    
    public boolean isTopologyMatch() { return topologyMatch; }
    public void setTopologyMatch(boolean topologyMatch) {
        this.topologyMatch = topologyMatch;
    }

    public boolean isNameMatch()     { return nameMatch; }
    public void setNameMatch(boolean nameMatch) {
        this.nameMatch = nameMatch;
    }
    
    public boolean isValidOnlyWhenTopologyMismatch() {
        return validOnlyWhenTopologyMismatch;
    }

    public void setValidOnlyWhenTopologyMismatch(boolean valid) {
        validOnlyWhenTopologyMismatch = valid;
    }
    
    /**
     * This class implements a zero-to-one, zero-to-many, one-to-many, and
     * many-to-many Export mismatch. 
     */
    public static class MultiMatch extends ExportMismatch{
    	static final long serialVersionUID = 0;

        /** Lists of mismatched exports in each Cells.
         * The stored objects are Ports on which mismatched Exports are */
        private final List<PortReport> ports[] = new ArrayList[2];
        
        public MultiMatch() {
        	ports[0] = new ArrayList<PortReport>();
        	ports[1] = new ArrayList<PortReport>();
        }
        /**
         * Add a mismatched Port. 
         * @param listIndex  Cell index
         * @param port  Port to add
         */
        public void add(int listIndex, PortReportable port) {
            ports[listIndex].add(new PortReport(port));
        }
        
        /**
         * Add all mismatched Ports in the proviede set 
         * @param listIndex  Cell index
         * @param portSet  Ports to add
         */
        public void add(int listIndex, Set<PortReportable> portSet) {
        	for (PortReportable p : portSet) ports[listIndex].add(new PortReport(p));
        }
        
        /**
         * Get all Ports for a given Cell 
         * @param index  Cell index
         * @return the list with all Posrt for the Cell with the given index 
         */
        public List<PortReport> getAll(int index) {return ports[index];}
    }
    
    
    /**
     * This class is a container for a suggested Export match.   
     */
    public static class NameMismatch extends ExportMismatch {
    	static final long serialVersionUID = 0;

        /** Mismatched Export in the first design */ private PortReport exp1;
        /** Suggested match in the second design  */ private NetObjReport exp2;
                                                     // exp2 is Port or Wire
        
        public NameMismatch() {
            nameMatch = false;
            topologyMatch = true;
        }
        public NameMismatch(String name1, String name2) {
            desingNames[0] = name1;
            desingNames[1] = name2;
            nameMatch = false;
            topologyMatch = true;
        }
        public PortReport   getFirstExport() { return exp1; }
        public NetObjReport getSuggestion()  { return exp2; }
        
        public void setFirstExport(PortReportable exp1)     { 
        	this.exp1 = new PortReport(exp1); 
        }
        public void setSuggestion(NetObjReportable exp2) { 
        	this.exp2 = NetObjReport.newNetObjReport(exp2); 
        }
    }


    /**
     * This class is a container for a topological Export mismatch. 
     * It also might have a suggested Export match.   
     */
    public static class TopologyMismatch extends ExportMismatch{
    	static final long serialVersionUID = 0;

        /** Mismatched Exports           */ private PortReport exp1, exp2;
        /** Suggestion in the 2nd design */ private NetObjReport sug = null;
                                            // sug is Port or Wire
        
        public TopologyMismatch() {
            nameMatch = true;
            topologyMatch = false;
        }
        public TopologyMismatch(String name1, String name2) {
            desingNames[0] = name1;
            desingNames[1] = name2;
            nameMatch = true;
            topologyMatch = false;
        }
        public PortReport   getFirstExport()  { return exp1; }
        public PortReport   getSecondExport() { return exp2; }
        public NetObjReport getSuggestion()   { return sug;  }
        
        public void setFirstExport(PortReportable exp1){
        	this.exp1 = new PortReport(exp1); 
        }        
        public void setSecondExport(PortReportable exp2) {
        	this.exp2 = new PortReport(exp2); 
        }
        public void setSuggestion(NetObjReportable sug) { 
        	this.sug = NetObjReport.newNetObjReport(sug); 
        }
    }    
}

/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: Ncc.java
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

import java.util.ArrayList;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Port;

public abstract class ExportMismatch {
    public final static int FIRST = 0;
    public final static int SECOND = 1;
    
    protected String desingNames[] = new String[2];
    protected Cell[] cells = new Cell[2];
    protected VarContext[] contexts = new VarContext[2];
    protected boolean topologyMatch;
    protected boolean nameMatch;
    protected boolean validOnlyWhenTopologyMismatch;

    public ExportMismatch() {
        init();
    }
    
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
    
    public String getName(int index) {
        return desingNames[index];
    }

    public void setName(int index, String name) {
        desingNames[index] = name;
    }

    public void setNames(String name1, String name2) {
        desingNames[0] = name1;
        desingNames[1] = name2;
    }    
    
    public Cell getCell(int index) {
        return cells[index];
    }

    public void setCell(int index, Cell cell) {
        cells[index] = cell;
    }

    public void setCells(Cell cell1, Cell cell2) {
        cells[0] = cell1;
        cells[1] = cell2;
    }    
    
    public VarContext getContext(int index) {
        return contexts[index];
    }

    public void setContext1(int index, VarContext cnxt) {
        contexts[index] = cnxt;
    }

    public void setContexts(VarContext cnxt1, VarContext cnxt2) {
        contexts[0] = cnxt1;
        contexts[1] = cnxt2;
    }
    
    public boolean isTopologyMatch() {
        return topologyMatch;
    }

    public void setTopologyMatch(boolean topologyMatch) {
        this.topologyMatch = topologyMatch;
    }
    
    public boolean isNameMatch() {
        return nameMatch;
    }

    public void setNameMatch(boolean nameMatch) {
        this.nameMatch = nameMatch;
    }
    
    public boolean isValidOnlyWhenTopologyMismatch() {
        return validOnlyWhenTopologyMismatch;
    }

    public void setValidOnlyWhenTopologyMismatch(boolean valid) {
        validOnlyWhenTopologyMismatch = valid;
    }
    
    
    public static class MultiMatch extends ExportMismatch{
        private ArrayList ports[] = {new ArrayList(), new ArrayList()};  // set of ports

        public void add(int listIndex, Port port) {
            ports[listIndex].add(port);
        }
        public void add(int listIndex, Set portSet) {
            ports[listIndex].addAll(portSet);
        }
        public ArrayList getAll(int index) {
            return ports[index];
        }
    }
    
    
    public static class NameMismatch extends ExportMismatch {
        public static final int EXPORTEXPORT = 0;
        public static final int EXPORTWIRE = 1;
        public static final int WIREEXPORT = 2;

        private int type;
        private Port exp1;
        private NetObject exp2;  // Port or Wire
        
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
        public int       getType()        { return type; }
        public Port      getFirstExport() { return exp1; }
        public NetObject getSuggestion()  { return exp2; }
        
        public void setType(int type)             { this.type = type; }
        public void setFirstExport(Port exp1)     { this.exp1 = exp1; }
        public void setSuggestion(NetObject exp2) { this.exp2 = exp2; }
    }

    
    public static class TopologyMismatch extends ExportMismatch{
        private Port exp1, exp2;
        private NetObject sug = null;  // Port or Wire
        
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
        public Port      getFirstExport()  { return exp1; }
        public Port      getSecondExport() { return exp2; }
        public NetObject getSuggestion()   { return sug;  }
        
        public void setFirstExport(Port exp1)    { this.exp1 = exp1; }        
        public void setSecondExport(Port exp2)   { this.exp2 = exp2; }
        public void setSuggestion(NetObject sug) { this.sug = sug; }
    }    
}

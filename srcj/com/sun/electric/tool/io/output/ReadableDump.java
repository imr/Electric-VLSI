/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ReadableDump.java
 * Input/output tool: "Readable-Dump" Library output
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.ExportId;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.LibId;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.Version;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class to write a library to disk in Readable Dump format.
 */
public class ReadableDump extends ELIB {
    private int portProtoError;
    private LinkedHashMap<CellId,Integer> cellOrdering = new LinkedHashMap<CellId,Integer>();
//    private HashMap<CellId,Integer> cellGrouping = new HashMap<CellId,Integer>();
    private HashMap<ExportId,Integer> portMap;
    
    ReadableDump() {
        write6Compatible();
    }
    
    /**
     * Method to write the .elib file.
     * Returns true on error.
     */
    @Override
    boolean writeTheLibrary() throws IOException {
        // clear error counters
        portProtoError = 0;
        
        // determine proper library order
        for (CellBackup cellBackup: externalCells)
            cellOrdering.put(cellBackup.d.cellId, null);
        for (CellBackup cellBackup: localCells) {
            CellId cellId = cellBackup.d.cellId;
            if (!cellOrdering.containsKey(cellId)) textRecurse(theLibId, cellId);
        }
        int cellNumber = 0;
        for (Map.Entry<CellId,Integer> e : cellOrdering.entrySet()) {
            e.setValue(new Integer(cellNumber++));
            objInfo.put(e.getKey(), e.getValue());
        }
        
        // write header information
        printWriter.println("****library: \"" + theLibId.libName + "\"");
        printWriter.println("version: " + Version.getVersion());
        printWriter.println("aids: " + toolCount);
        for(Iterator<Tool> it = Tool.getTools(); it.hasNext(); ) {
            Tool tool = it.next();
            if (!objInfo.containsKey(tool)) continue;
            printWriter.println("aidname: " + tool.getName());
            writeMeaningPrefs(tool);
        }
//		printWriter.println("userbits: " + lib.lowLevelGetUserBits());
        printWriter.println("techcount: " + techCount);
        for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); ) {
            Technology tech = it.next();
            if (!objInfo.containsKey(tech)) continue;
            printWriter.println("techname: " + tech.getTechName() + " lambda: " + gridCoordToElib(tech, DBMath.GRID));
//            printWriter.println("techname: " + tech.getTechName() + " lambda: " + (int)(tech.getScale()*2));
            writeMeaningPrefs(tech);
        }
        for(Iterator<View> it = View.getViews(); it.hasNext(); ) {
            View v = it.next();
            if (!objInfo.containsKey(v)) continue;
            printWriter.println("view: " + v.getFullName() + "{" + v.getAbbreviation() + "}");
        }
        printWriter.println("cellcount: " + cellNumber);
        
        // write variables on the library
        writeVariables(snapshot.getLib(theLibId).d);
        
        // write the rest of the database
        writeExternalCells();
        for (Map.Entry<CellId,Integer> entry : cellOrdering.entrySet()) {
            CellId cellId = entry.getKey();
            if (cellId.libId != theLibId) continue;
            CellBackup cellBackup = snapshot.getCell(cellId);
            startCell(cellBackup, 0);
            int groupIndex = groupRenumber[snapshot.cellGroups[cellId.cellIndex]];
            printWriter.println("***cell: " + ((Integer)entry.getValue()).intValue() + "/" + groupIndex);
            writeCellInfo(cellBackup);
            
            // write tool information
            printWriter.println("userbits: " + (cellBackup.d.flags & ELIBConstants.CELL_BITS));
            
            // count and number the ports
            portMap = new HashMap<ExportId,Integer>();
            int portCount = 0;
            for(int exportIndex = 0; exportIndex < cellBackup.exports.size(); exportIndex++) {
                ImmutableExport e = cellBackup.exports.get(exportIndex);
                portMap.put(e.exportId, new Integer(portCount++));
            }
            printWriter.println("nodes: " + cellBackup.nodes.size() + " arcs: " + cellBackup.arcs.size() +
                    " porttypes: " + cellBackup.exports.size());
            
            // write variables on the cell
            writeVariables(cellBackup.d);
            
            // write the nodes in this cell
            writeNodes(cellBackup, 0);
            
            // write the portprotos in this cell
            writeExports(cellBackup);
            
            // write the arcs in this cell
            writeArcs(cellBackup);
            printWriter.println("celldone: " + cellId.cellName.getName());
        }
        
        // print any variable-related error messages
        if (portProtoError != 0)
            System.out.println("Warning: " + portProtoError + " export pointers point outside cell: not saved");
        
        return false;
    }
    
    @Override
    void writeOrientation(int angle, int transpose) throws IOException {
        printWriter.println("rotation: " + angle + " transpose: " + transpose);
    }
    
    @Override
    void writeConnection(PortProtoId portId, int arcIndex, int connIndex) throws IOException {
        printWriter.println("*port: " + portId.getName(snapshot) + " arc: " + arcIndex);
    }
    
    @Override
    void writeReExport(ImmutableExport e) throws IOException {
        Integer pIndex = portMap.get(e.exportId);
        if (pIndex == null) pIndex = new Integer(-1);
        printWriter.println("*port: " + e.originalPortId.getName(snapshot) + " exported: " + pIndex.intValue());
    }

    /**
     * Method to help order the library for proper nonforward references
     * in the outout
     */
    private void textRecurse(LibId libId, CellId cellId) {
        for (ImmutableNodeInst n: snapshot.getCell(cellId).nodes) {
            if (!(n.protoId instanceof CellId)) continue;
            CellId subCellId = (CellId)n.protoId;
            if (subCellId.libId != libId) continue;
            if (!cellOrdering.containsKey(subCellId)) textRecurse(libId, subCellId);
        }
        
        // add this cell to the list
        cellOrdering.put(cellId, null);
    }
    
    /**
     * Method to write the value of variable.
     * @param obj value of variable.
     */
    @Override
    void writeVarValue(Object varObj) {
        StringBuffer infstr = new StringBuffer();
        if (varObj instanceof Object[]) {
            Object [] objArray = (Object [])varObj;
            int len = objArray.length;
            for(int i=0; i<len; i++) {
                Object oneObj = objArray[i];
                if (i == 0) infstr.append("["); else
                    infstr.append(",");
                if (oneObj != null)
                    makeStringVar(infstr, oneObj);
            }
            infstr.append("]");
        } else makeStringVar(infstr, varObj);
        printWriter.println(infstr);
    }
    
    /**
     * Method to make a string from the value in "addr" which has a type in
     * "type".
     */
    private void makeStringVar(StringBuffer infstr, Object obj) {
        if (obj instanceof String) {
            infstr.append("\"");
            infstr.append(convertString((String)obj));
            infstr.append("\"");
            return;
        }
        if (obj instanceof Double) {
            infstr.append(((Double)obj).floatValue());
            return;
        }
        if (obj instanceof Float) {
            infstr.append(((Float)obj).floatValue());
            return;
        }
        if (obj instanceof Long) {
            infstr.append(((Long)obj).intValue());
            return;
        }
        if (obj instanceof Integer) {
            infstr.append(((Integer)obj).intValue());
            return;
        }
        if (obj instanceof Short) {
            infstr.append(((Short)obj).shortValue());
            return;
        }
        if (obj instanceof Byte) {
            infstr.append(((Byte)obj).byteValue());
            return;
        }
        if (obj instanceof Boolean) {
            infstr.append(((Boolean)obj).booleanValue() ? 1 : 0);
            return;
        }
        if (obj instanceof Tool) {
            Tool tool = (Tool)obj;
            infstr.append(tool.getName());
            return;
        }
        if (obj instanceof Technology) {
            Technology tech = (Technology)obj;
            infstr.append(tech.getTechName());
            return;
        }
        if (obj instanceof PrimitiveNode) {
            PrimitiveNode np = (PrimitiveNode)obj;
            infstr.append(np.getTechnology().getTechName() + ":" + np.getName());
            return;
        }
        if (obj instanceof ArcProto) {
            ArcProto ap = (ArcProto)obj;
            infstr.append(ap.getTechnology().getTechName() + ":" + ap.getName());
            return;
        }
        if (obj instanceof LibId) {
            LibId libId = (LibId)obj;
            infstr.append("\"" + libId.libName + "\"");
            return;
        }
        if (obj instanceof CellId) {
            CellId cellId = (CellId)obj;
            Integer mi = cellOrdering.get(cellId);
            int cIndex = -1;
            if (mi != null) cIndex = mi.intValue();
            infstr.append(Integer.toString(cIndex));
            return;
        }
        if (obj instanceof ExportId) {
            ExportId exportId = (ExportId)obj;
            Integer portIndex = portMap.get(exportId);
            int cIndex = -1;
            if (portIndex == null) portProtoError++; else
                cIndex = portIndex.intValue();
            infstr.append(Integer.toString(cIndex));
            return;
        }
        assert false;
    }
    
    /**
     * Method to write a text descriptor (possibly with variable bits).
     * Face of text descriptor is mapped according to "faceMap".
     * @param varBits variableBits or -1.
     * @param td0 first word of TextDescriptor to write.
     * @param td1 first word of TextDescriptor to write.
     */
    @Override
    void writeTextDescriptor(int varBits, int td0, int td1) throws IOException {
        if (varBits == -1) {
            printWriter.println("descript: " + td0 + "/" + td1);
            return;
        }
        if ((varBits & ELIBConstants.VISARRAY) != 0)
            printWriter.print("(" + ((varBits & ELIBConstants.VLENGTH) >> ELIBConstants.VLENGTHSH) + ")");
        printWriter.print("[0" + Integer.toOctalString(varBits) + ",");
        printWriter.print(td0 != 0 ? "0" + Integer.toOctalString(td0) : "0");
        printWriter.print("/");
        printWriter.print(td1 != 0 ? "0" + Integer.toOctalString(td1) : "0");
        printWriter.print("]: ");
    }
    
    /**
     * Method to write a disk index of Object.
     * Index is obtained fron objInfo map.
     * @param obj Object to write
     */
    @Override
    void writeObj(Object obj) throws IOException {}
    
    /**
     * Method to write an integer (4 bytes) to the ouput stream.
     * @param keyword keywork fro ReadableDump
     * @parma i integer value.
     */
    @Override
    void writeInt(String keyword, int i) throws IOException {
        if (keyword == null) return;
        printWriter.println(keyword + i);
    }
    
    /**
     * Method to write a text into ReadableDump stream.
     * @param txt a text to write into ReadableDump stream.
     */
    @Override
    void writeTxt(String txt) throws IOException {
        printWriter.println(txt);
    }
    
    @Override
	void writeBigInteger(int i) throws IOException {}

    /**
     * Method to write a disk index of variable name.
     * Index is obtained from the nameSpace map.
     * @param name Variable Key to write
     */
    @Override
    void writeVariableName(String name) throws IOException {
        printName(name);
    }
    
    /**
     * Method to add the string "str" to the infinite string and to quote the
     * special characters '[', ']', '"', and '^'.
     */
    private String convertString(String str) {
        StringBuffer infstr = new StringBuffer();
        int len = str.length();
        for(int i=0; i<len; i++) {
            char ch = str.charAt(i);
            if (ch == '[' || ch == ']' || ch == '"' || ch == '^')
                infstr.append('^');
            infstr.append(ch);
        }
        return infstr.toString();
    }
    
    /**
     * Method to print the variable name in "name" on file "file".  The
     * conversion performed is to quote with a backslash any of the characters
     * '(', '[', or '^'.
     */
    private void printName(String name) {
        int len = name.length();
        for(int i=0; i<len; i++) {
            char pt = name.charAt(i);
            if (pt == '^' || pt == '[' || pt == '(' || pt == ':') printWriter.print("^");
            printWriter.print(pt);
        }
    }
}

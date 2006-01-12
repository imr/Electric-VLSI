/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EObjectOutputStream.java
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

package com.sun.electric.database;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Tool;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * ObjectOutputStream which repalaces Electric objects by serializable key objects.
 * On reading key objects are resolved to Electric objects again.
 */
public class EObjectOutputStream extends ObjectOutputStream {
    
    public EObjectOutputStream(OutputStream out) throws IOException {
        super(out);
        enableReplaceObject(true);
    }
    
    /** 
     * This method will allow trusted subclasses of ObjectOutputStream to
     * substitute one object for another during serialization. Replacing
     * objects is disabled until enableReplaceObject is called. The
     * enableReplaceObject method checks that the stream requesting to do
     * replacement can be trusted.  The first occurrence of each object written
     * into the serialization stream is passed to replaceObject.  Subsequent
     * references to the object are replaced by the object returned by the
     * original call to replaceObject.  To ensure that the private state of
     * objects is not unintentionally exposed, only trusted streams may use
     * replaceObject.
     * 
     * <p>The ObjectOutputStream.writeObject method takes a parameter of type
     * Object (as opposed to type Serializable) to allow for cases where
     * non-serializable objects are replaced by serializable ones.
     * 
     * <p>When a subclass is replacing objects it must insure that either a
     * complementary substitution must be made during deserialization or that
     * the substituted object is compatible with every field where the
     * reference will be stored.  Objects whose type is not a subclass of the
     * type of the field or array element abort the serialization by raising an
     * exception and the object is not be stored.
     *
     * <p>This method is called only once when each object is first
     * encountered.  All subsequent references to the object will be redirected
     * to the new object. This method should return the object to be
     * substituted or the original object.
     *
     * <p>Null can be returned as the object to be substituted, but may cause
     * NullReferenceException in classes that contain references to the
     * original object since they may be expecting an object instead of
     * null.
     *
     * @param	obj the object to be replaced
     * @return	the alternate object that replaced the specified one
     * @throws	IOException Any exception thrown by the underlying
     * 		OutputStream.
     */
    protected Object replaceObject(Object obj) throws IOException {
        if (obj instanceof NodeInst) return new ENodeInst((NodeInst)obj);
        if (obj instanceof ArcInst) return new EArcInst((ArcInst)obj);
        if (obj instanceof PortInst) return new EPortInst((PortInst)obj);
        if (obj instanceof Export) return new EExport((Export)obj);
        if (obj instanceof Cell) return new ECell((Cell)obj);
        if (obj instanceof Technology) return new ETechnology((Technology)obj);
        if (obj instanceof PrimitiveNode) return new EPrimitiveNode((PrimitiveNode)obj);
        if (obj instanceof PrimitivePort) return new EPrimitivePort((PrimitivePort)obj);
        if (obj instanceof ArcProto) return new EArcProto((ArcProto)obj);
        if (obj instanceof Tool) return new ETool((Tool)obj);
        if (obj instanceof Variable.Key) return new EVariableKey((Variable.Key)obj);
        return obj;
    }

    private static class ENodeInst implements Serializable {
        int cellIndex, nodeId;
        
        private ENodeInst(NodeInst ni) {
            assert ni.isLinked();
            cellIndex = ni.getParent().getCellIndex();
            nodeId = ni.getD().nodeId;
        }
        
        private Object readResolve() throws ObjectStreamException {
            Cell cell = (Cell)CellId.getByIndex(cellIndex).inCurrentThread();
            NodeInst ni = cell.getNodeById(nodeId);
            if (ni == null) throw new InvalidObjectException("NodeInst");
            return ni;
        }
    }
    
    private static class EArcInst implements Serializable {
        int cellIndex, arcId;
        
        private EArcInst(ArcInst ai) {
            assert ai.isLinked();
            cellIndex = ai.getParent().getCellIndex();
            arcId = ai.getD().arcId;
        }
        
        private Object readResolve() throws ObjectStreamException {
            Cell cell = (Cell)CellId.getByIndex(cellIndex).inCurrentThread();
            ArcInst ai = cell.getArcById(arcId);
            if (ai == null) throw new InvalidObjectException("ArcInst");
            return ai;
        }
    }
    
    private static class EPortInst implements Serializable {
        int cellIndex, nodeId, portChronIndex;
        
        private EPortInst(PortInst pi) {
            assert pi.isLinked();
            cellIndex = pi.getNodeInst().getParent().getCellIndex();
            nodeId = pi.getNodeInst().getD().nodeId;
            portChronIndex = pi.getPortProto().getId().getChronIndex();
        }
        
        private Object readResolve() throws ObjectStreamException {
            Cell cell = (Cell)CellId.getByIndex(cellIndex).inCurrentThread();
            NodeInst ni = cell.getNodeById(nodeId);
            NodeProto np = ni.getProto();
            int portIndex;
            if (np instanceof Cell) {
                Export e = ((Cell)np).getExportChron(portChronIndex);
                portIndex = e.getPortIndex();
            } else {
                portIndex = portChronIndex;
            }
            PortInst pi = ni.getPortInst(portIndex);
            if (pi == null) throw new InvalidObjectException("PortInst");
            return pi;
        }
    }
    
    private static class EExport implements Serializable {
        int cellIndex, exportChronIndex;
        
        private EExport(Export e) {
            assert e.isLinked();
            cellIndex = ((Cell)e.getParent()).getCellIndex();
            exportChronIndex = e.getD().exportId.chronIndex;
        }
        
        private Object readResolve() throws ObjectStreamException {
            Cell cell = (Cell)CellId.getByIndex(cellIndex).inCurrentThread();
            Export e = cell.getExportChron(exportChronIndex);
            if (e == null) throw new InvalidObjectException("Export");
            return e;
        }
    }
    
    private static class ECell implements Serializable {
        int cellIndex;
        
        private ECell(Cell cell) {
            assert cell.isLinked();
            cellIndex = cell.getCellIndex();
        }
        
        private Object readResolve() throws ObjectStreamException {
            Cell cell = (Cell)CellId.getByIndex(cellIndex).inCurrentThread();
            if (cell == null) throw new InvalidObjectException("Cell");
            return cell;
        }
    }
    
    private static class ETechnology implements Serializable {
        String techName;
        
        private ETechnology(Technology tech) {
            techName = tech.getTechName();
        }
        
        private Object readResolve() throws ObjectStreamException {
            Technology tech = Technology.findTechnology(techName);
            if (tech == null) throw new InvalidObjectException("Technology");
            return tech;
        }
    }
    
    private static class EPrimitiveNode implements Serializable {
        String techName, primName;
        
        private EPrimitiveNode(PrimitiveNode pn) {
            techName = pn.getTechnology().getTechName();
            primName = pn.getName();
        }
        
        private Object readResolve() throws ObjectStreamException {
            Technology tech = Technology.findTechnology(techName);
            PrimitiveNode pn = tech.findNodeProto(primName);
            if (pn == null) throw new InvalidObjectException("PrimitiveNode");
            return pn;
        }
    }
    
    private static class EPrimitivePort implements Serializable {
        String techName, primName;
        int portIndex;
        
        private EPrimitivePort(PrimitivePort pp) {
            PrimitiveNode pn = (PrimitiveNode)pp.getParent();
            techName = pn.getTechnology().getTechName();
            primName = pn.getName();
            portIndex = pp.getPortIndex();
        }
        
        private Object readResolve() throws ObjectStreamException {
            Technology tech = Technology.findTechnology(techName);
            PrimitiveNode pn = tech.findNodeProto(primName);
            PrimitivePort pp = (PrimitivePort)pn.getPort(portIndex);
            if (pp == null) throw new InvalidObjectException("PrimitivePort");
            return pp;
        }
    }
    
    private static class EArcProto implements Serializable {
        String techName, arcName;
        
        private EArcProto(ArcProto ap) {
            techName = ap.getTechnology().getTechName();
            arcName = ap.getName();
        }
        
        private Object readResolve() throws ObjectStreamException {
            Technology tech = Technology.findTechnology(techName);
            ArcProto ap = tech.findArcProto(arcName);
            if (ap == null) throw new InvalidObjectException("ArcProto");
            return ap;
        }
    }
    
    private static class ETool implements Serializable {
        String toolName;
        
        private ETool(Tool tool) {
            toolName = tool.getName();
        }
        
        private Object readResolve() throws ObjectStreamException {
            Tool tool = Tool.findTool(toolName);
            if (tool == null) throw new InvalidObjectException("Tool");
            return tool;
        }
    }
    
    private static class EVariableKey implements Serializable {
        String varName;
        
        private EVariableKey(Variable.Key varKey) {
            varName = varKey.toString();
        }
        
        private Object readResolve() throws ObjectStreamException {
            Variable.Key varKey = Variable.findKey(varName);
            if (varKey == null) throw new InvalidObjectException("Variable.Key");
            return varKey;
        }
    }
    
}

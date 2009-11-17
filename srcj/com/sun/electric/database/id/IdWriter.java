/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IdWriter.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.database.id;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Tool;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Class to write trace of Snapshots to DataOutput byte sequence.
 */
public class IdWriter {

    public final IdManager idManager;
    private final DataOutputStream out;
    private TechCounts[] techCounts = {}; // One entry is for Generic technology
    private int libCount;
    private int[] exportCounts = {};
    private HashMap<Variable.Key, Integer> varKeys = new HashMap<Variable.Key, Integer>();
    private HashMap<TextDescriptor, Integer> textDescriptors = new HashMap<TextDescriptor, Integer>();
    private HashMap<Tool, Integer> tools = new HashMap<Tool, Integer>();
    private HashMap<Orientation, Integer> orients = new HashMap<Orientation, Integer>();

    private static class TechCounts {

        int modCount;
        int layerCount;
        int arcCount;
        int[] portCounts = {};
    }

    /** Creates a new instance of SnapshotWriter */
    public IdWriter(IdManager idManager, DataOutputStream out) {
        this.idManager = idManager;
        this.out = out;
    }

    public void writeDiffs() throws IOException {
        TechId[] techIdsArray;
        LibId[] libIdsArray;
        CellId[] cellIdsArray;
        synchronized (idManager) {
            techIdsArray = idManager.techIds.toArray(TechId.NULL_ARRAY);
            libIdsArray = idManager.libIds.toArray(LibId.NULL_ARRAY);
            cellIdsArray = idManager.cellIds.toArray(CellId.NULL_ARRAY);
        }

        writeInt(techIdsArray.length);
        if (techIdsArray.length != techCounts.length) {
            TechCounts[] newTechCounts = new TechCounts[techIdsArray.length];
            System.arraycopy(techCounts, 0, newTechCounts, 0, techCounts.length);
            for (int techIndex = techCounts.length; techIndex < techIdsArray.length; techIndex++) {
                TechId techId = techIdsArray[techIndex];
                writeString(techId.techName);
                newTechCounts[techIndex] = new TechCounts();
            }
            techCounts = newTechCounts;
        }

        writeInt(libIdsArray.length);
        for (int libIndex = libCount; libIndex < libIdsArray.length; libIndex++) {
            LibId libId = libIdsArray[libIndex];
            writeString(libId.libName);
        }
        libCount = libIdsArray.length;

        writeInt(cellIdsArray.length);
        if (cellIdsArray.length != exportCounts.length) {
            int[] newExportCounts = new int[cellIdsArray.length];
            System.arraycopy(exportCounts, 0, newExportCounts, 0, exportCounts.length);
            for (int cellIndex = exportCounts.length; cellIndex < cellIdsArray.length; cellIndex++) {
                CellId cellId = cellIdsArray[cellIndex];
                writeLibId(cellId.libId);
                writeString(cellId.cellName.toString());
            }
            exportCounts = newExportCounts;
        }

        for (int techIndex = 0; techIndex < techIdsArray.length; techIndex++) {
            TechId techId = techIdsArray[techIndex];
            TechCounts techCount = techCounts[techIndex];
            int modCount = techId.modCount;
            if (modCount == techCount.modCount) {
                continue;
            }

            writeInt(techIndex);

            int numLayerIds = techId.numLayerIds();
            int numNewLayerIds = numLayerIds - techCount.layerCount;
            assert numNewLayerIds >= 0;
            writeInt(numNewLayerIds);
            for (int i = 0; i < numNewLayerIds; i++) {
                writeString(techId.getLayerId(techCount.layerCount + i).name);
            }
            techCount.layerCount = numLayerIds;

            int numArcProtoIds = techId.numArcProtoIds();
            int numNewArcProtoIds = numArcProtoIds - techCount.arcCount;
            assert numNewArcProtoIds >= 0;
            writeInt(numNewArcProtoIds);
            for (int i = 0; i < numNewArcProtoIds; i++) {
                writeString(techId.getArcProtoId(techCount.arcCount + i).name);
            }
            techCount.arcCount = numArcProtoIds;

            int numPrimitiveNodeIds = techId.numPrimitiveNodeIds();
            int numNewPrimitiveNodeIds = numPrimitiveNodeIds - techCount.portCounts.length;
            assert numNewPrimitiveNodeIds >= 0;
            writeInt(numNewPrimitiveNodeIds);
            if (numNewPrimitiveNodeIds > 0) {
                for (int i = 0; i < numNewPrimitiveNodeIds; i++) {
                    writeString(techId.getPrimitiveNodeId(techCount.portCounts.length + i).name);
                }
                int[] newPortCounts = new int[numPrimitiveNodeIds];
                System.arraycopy(techCount.portCounts, 0, newPortCounts, 0, techCount.portCounts.length);
                techCount.portCounts = newPortCounts;
            }

            for (int primIndex = 0; primIndex < numPrimitiveNodeIds; primIndex++) {
                PrimitiveNodeId primitiveNodeId = techId.getPrimitiveNodeId(primIndex);
                int numPrimitivePortIds = primitiveNodeId.numPrimitivePortIds();
                int numNewPrimitivePortIds = numPrimitivePortIds - techCount.portCounts[primIndex];
                assert numNewPrimitivePortIds >= 0;
                if (numNewPrimitivePortIds == 0) {
                    continue;
                }

                writeInt(primIndex);
                writeInt(numNewPrimitivePortIds);
                for (int i = 0; i < numNewPrimitivePortIds; i++) {
                    writeString(primitiveNodeId.getPortId(techCount.portCounts[primIndex] + i).externalId);
                }
                techCount.portCounts[primIndex] = numPrimitivePortIds;
            }
            writeInt(-1);

            techCount.modCount = modCount;
        }
        writeInt(-1);

        for (int cellIndex = 0; cellIndex < cellIdsArray.length; cellIndex++) {
            CellId cellId = cellIdsArray[cellIndex];
            int numExportIds = cellId.numExportIds();
            int exportCount = exportCounts[cellIndex];
            if (numExportIds != exportCount) {
                writeInt(cellIndex);
                int numNewExportIds = numExportIds - exportCount;
                assert numNewExportIds > 0;
                writeInt(numNewExportIds);
                for (int i = 0; i < numNewExportIds; i++) {
                    writeString(cellId.getPortId(exportCount + i).externalId);
                }
                exportCounts[cellIndex] = numExportIds;
            }
        }
        writeInt(-1);
    }

    /** Flushes this IdWriter */
    public void flush() throws IOException {
        out.flush();
    }

    /** Flushes this IdWriter */
    public void close() throws IOException {
        out.close();
    }

    /**
     * Writes boolean.
     * @param v boolean to write.
     */
    public void writeBoolean(boolean v) throws IOException {
        out.writeBoolean(v);
    }

    /**
     * Writes byte.
     * @param v byte to write.
     */
    public void writeByte(byte v) throws IOException {
        out.writeByte(v);
    }

    /**
     * Writes short.
     * @param v short to write.
     */
    public void writeShort(short v) throws IOException {
        out.writeShort(v);
    }

    /**
     * Writes integer.
     * @param v integer to write.
     */
    public void writeInt(int v) throws IOException {
        out.writeInt(v);
    }

    /**
     * Writes long.
     * @param v long to write.
     */
    public void writeLong(long v) throws IOException {
        out.writeLong(v);
    }

    /**
     * Writes float.
     * @param v float to write.
     */
    public void writeFloat(float v) throws IOException {
        out.writeFloat(v);
    }

    /**
     * Writes double.
     * @param v double to write.
     */
    public void writeDouble(double v) throws IOException {
        out.writeDouble(v);
    }

    /**
     * Writes bytes.
     * @param v bytes to write.
     */
    public void writeBytes(byte[] v) throws IOException {
        out.writeInt(v.length);
        out.write(v);
    }

    /**
     * Writes string.
     * @param s string to write.
     */
    public void writeString(String s) throws IOException {
        out.writeUTF(s);
    }

    /**
     * Writes variable key.
     * @param key variable key to write.
     */
    public void writeVariableKey(Variable.Key key) throws IOException {
        Integer i = varKeys.get(key);
        if (i != null) {
            out.writeInt(i.intValue());
        } else {
            i = new Integer(varKeys.size());
            varKeys.put(key, i);
            out.writeInt(i.intValue());

            out.writeUTF((key.toString()));
        }
    }

    /**
     * Writes TextDescriptor.
     * @param td TextDescriptor to write.
     */
    public void writeTextDescriptor(TextDescriptor td) throws IOException {
        if (td == null) {
            out.writeInt(-1);
            return;
        }
        Integer i = textDescriptors.get(td);
        if (i != null) {
            out.writeInt(i.intValue());
        } else {
            i = new Integer(textDescriptors.size());
            textDescriptors.put(td, i);
            out.writeInt(i.intValue());

            out.writeLong(td.lowLevelGet());
            out.writeInt(td.getColorIndex());
            out.writeBoolean(td.isDisplay());
            int face = td.getFace();
            String fontName = face != 0 ? TextDescriptor.ActiveFont.findActiveFont(face).getName() : "";
            out.writeUTF(fontName);
        }
    }

    /**
     * Writes Tool.
     * @param tool Tool to write.
     */
    public void writeTool(Tool tool) throws IOException {
        Integer i = tools.get(tool);
        if (i != null) {
            out.writeInt(i.intValue());
        } else {
            i = new Integer(tools.size());
            tools.put(tool, i);
            out.writeInt(i.intValue());
            out.writeUTF(tool.getName());
        }
    }

    /**
     * Writes TechId.
     * @param techId TechId to write.
     */
    public void writeTechId(TechId techId) throws IOException {
        assert techId.idManager == idManager;
        out.writeInt(techId.techIndex);
    }

    /**
     * Writes ArcProtoId.
     * @param arcProtoId ArcProtoId to write.
     */
    public void writeArcProtoId(ArcProtoId arcProtoId) throws IOException {
        writeTechId(arcProtoId.techId);
        writeInt(arcProtoId.chronIndex);
    }

    /**
     * Writes LibId.
     * @param libId LibId to write.
     */
    public void writeLibId(LibId libId) throws IOException {
        out.writeInt(libId.libIndex);
    }

    /**
     * Writes NodeProtoId.
     * @param nodeProtoId NodeProtoId to write.
     */
    public void writeNodeProtoId(NodeProtoId nodeProtoId) throws IOException {
        if (nodeProtoId instanceof CellId) {
            CellId cellId = (CellId) nodeProtoId;
            assert cellId.idManager == idManager;
            out.writeInt(cellId.cellIndex);
            return;
        } else {
            PrimitiveNodeId pnId = (PrimitiveNodeId) nodeProtoId;
            out.writeInt(~pnId.chronIndex);
            writeTechId(pnId.techId);
        }
    }

    /**
     * Writes PortProtoId.
     * @param portProtoId PortProtoId to write.
     */
    public void writePortProtoId(PortProtoId portProtoId) throws IOException {
        writeNodeProtoId(portProtoId.getParentId());
        out.writeInt(portProtoId.getChronIndex());
    }

    /**
     * Writes node id.
     * @param nodeId node id to write.
     */
    public void writeNodeId(int nodeId) throws IOException {
        out.writeInt(nodeId);
    }

    /**
     * Writes arc id.
     * @param arcId arc id to write.
     */
    public void writeArcId(int arcId) throws IOException {
        out.writeInt(arcId);
    }

    /**
     * Writes Name key.
     * @param nameKey name key to write.
     */
    public void writeNameKey(Name nameKey) throws IOException {
        out.writeUTF(nameKey.toString());
    }

    /**
     * Writes Orientation.
     * @param orient Orientation.
     */
    public void writeOrientation(Orientation orient) throws IOException {
        Integer i = orients.get(orient);
        if (i != null) {
            out.writeInt(i.intValue());
        } else {
            i = new Integer(orients.size());
            orients.put(orient, i);
            out.writeInt(i.intValue());

            out.writeShort(orient.getAngle());
            out.writeBoolean(orient.isXMirrored());
            out.writeBoolean(orient.isYMirrored());
        }
    }

    /**
     * Writes coordiante.
     * @param v gridCooridnate.
     */
    public void writeCoord(long v) throws IOException {
        out.writeLong(v);
    }

    /**
     * Writes EPoint.
     * @param p EPoint.
     */
    public void writePoint(EPoint p) throws IOException {
        writeCoord(p.getGridX());
        writeCoord(p.getGridY());
    }

    /**
     * Writes ERectangle.
     * @param r ERectangle.
     */
    public void writeRectangle(ERectangle r) throws IOException {
        writeCoord(r.getGridX());
        writeCoord(r.getGridY());
        writeCoord(r.getGridWidth());
        writeCoord(r.getGridHeight());
    }
}

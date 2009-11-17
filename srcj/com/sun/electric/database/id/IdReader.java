/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IdReader.java
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
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Tool;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 */
public class IdReader {

    public final IdManager idManager;
    private final DataInputStream in;
    private final ArrayList<Variable.Key> varKeys = new ArrayList<Variable.Key>();
    private final ArrayList<TextDescriptor> textDescriptors = new ArrayList<TextDescriptor>();
    private final ArrayList<Tool> tools = new ArrayList<Tool>();
    private final ArrayList<Orientation> orients = new ArrayList<Orientation>();

    /** Creates a new instance of SnapshotWriter */
    public IdReader(DataInputStream in, IdManager idManager) {
        if (in == null || idManager == null) {
            throw new NullPointerException();
        }
        this.in = in;
        this.idManager = idManager;
    }

    public void readDiffs() throws IOException {
        int oldTechIdsCount, oldLibIdsCount, oldCellIdsCount;
        synchronized (idManager) {
            oldTechIdsCount = idManager.techIds.size();
            oldLibIdsCount = idManager.libIds.size();
            oldCellIdsCount = idManager.cellIds.size();
        }
        int techIdsCount = readInt();
        for (int techIndex = oldTechIdsCount; techIndex < techIdsCount; techIndex++) {
            idManager.newTechIdInternal(readString());
        }
        int libIdsCount = readInt();
        for (int libIndex = oldLibIdsCount; libIndex < libIdsCount; libIndex++) {
            idManager.newLibIdInternal(readString());
        }
        int cellIdsCount = readInt();
        for (int cellIndex = oldCellIdsCount; cellIndex < cellIdsCount; cellIndex++) {
            LibId libId = readLibId();
            idManager.newCellIdInternal(libId, CellName.parseName(readString()));
        }
        synchronized (idManager) {
            assert techIdsCount == idManager.techIds.size();
            assert libIdsCount == idManager.libIds.size();
            assert cellIdsCount == idManager.cellIds.size();
        }
        for (;;) {
            int techIndex = readInt();
            if (techIndex == -1) {
                break;
            }
            TechId techId = idManager.getTechId(techIndex);

            int numNewLayerIds = readInt();
            for (int i = 0; i < numNewLayerIds; i++) {
                String layerName = readString();
                techId.newLayerIdInternal(layerName);
            }

            int numNewArcProtoIds = readInt();
            for (int i = 0; i < numNewArcProtoIds; i++) {
                String arcProtoName = readString();
                techId.newArcProtoIdInternal(arcProtoName);
            }

            int numNewPrimitiveNodeIds = readInt();
            for (int i = 0; i < numNewPrimitiveNodeIds; i++) {
                String primitiveNodeName = readString();
                techId.newPrimitiveNodeIdInternal(primitiveNodeName);
            }

            for (;;) {
                int primIndex = readInt();
                if (primIndex == -1) {
                    break;
                }
                PrimitiveNodeId primitiveNodeId = techId.getPrimitiveNodeId(primIndex);
                int numNewPrimitivePortIds = readInt();
                for (int i = 0; i < numNewPrimitivePortIds; i++) {
                    String primitivePortName = readString();
                    primitiveNodeId.newPrimitivePortIdInternal(primitivePortName);
                }
            }
        }
        for (;;) {
            int cellIndex = readInt();
            if (cellIndex == -1) {
                break;
            }
            CellId cellId = idManager.getCellId(cellIndex);
            int numNewExportIds = readInt();
            for (int i = 0; i < numNewExportIds; i++) {
                String exportIdString = readString();
                cellId.newPortId(exportIdString);
            }
        }
    }

    /**
     * Reads boolean.
     * @return boolean.
     */
    public boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    /**
     * Reads byte.
     * @return byte.
     */
    public byte readByte() throws IOException {
        return in.readByte();
    }

    /**
     * Reads short.
     * @return short.
     */
    public short readShort() throws IOException {
        return in.readShort();
    }

    /**
     * Reads integer.
     * @return integer.
     */
    public int readInt() throws IOException {
        return in.readInt();
    }

    /**
     * Reads long.
     * @return long.
     */
    public long readLong() throws IOException {
        return in.readLong();
    }

    /**
     * Reads float.
     * @return float.
     */
    public float readFloat() throws IOException {
        return in.readFloat();
    }

    /**
     * Reads double.
     * @return double.
     */
    public double readDouble() throws IOException {
        return in.readDouble();
    }

    /**
     * Reads bytes.
     * @return bytes.
     */
    public byte[] readBytes() throws IOException {
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return bytes;
    }

    /**
     * Reads string.
     * @return string.
     */
    public String readString() throws IOException {
        return in.readUTF();
    }

    /**
     * Reads variable key.
     * @return variable key.
     */
    public Variable.Key readVariableKey() throws IOException {
        int i = in.readInt();
        if (i == varKeys.size()) {
            String s = in.readUTF();
            Variable.Key varKey = Variable.newKey(s);
            varKeys.add(varKey);
        }
        return varKeys.get(i);
    }

    /**
     * Reads TextDescriptor.
     * @return TextDescriptor.
     */
    public TextDescriptor readTextDescriptor() throws IOException {
        int i = in.readInt();
        if (i == -1) {
            return null;
        }
        if (i == textDescriptors.size()) {
            long bits = in.readLong();
            int colorIndex = in.readInt();
            boolean isDisplay = in.readBoolean();
            String fontName = in.readUTF();

            MutableTextDescriptor mtd = new MutableTextDescriptor(bits, colorIndex, isDisplay);
            int face = 0;
            if (fontName.length() != 0) {
                TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(fontName);
                face = af.getIndex();
            }
            mtd.setFace(face);
            TextDescriptor td = TextDescriptor.newTextDescriptor(mtd);
            textDescriptors.add(td);
        }
        return textDescriptors.get(i);
    }

    /**
     * Reads Tool.
     * @return Tool.
     */
    public Tool readTool() throws IOException {
        int i = in.readInt();
        if (i == tools.size()) {
            String toolName = in.readUTF();
            Tool tool = Tool.findTool(toolName);
            tools.add(tool);
        }
        return tools.get(i);
    }

    /**
     * Reads TechId.
     * @return TechId.
     */
    public TechId readTechId() throws IOException {
        int techIndex = in.readInt();
        return idManager.getTechId(techIndex);
    }

    /**
     * Reads ArcProtoId.
     * @return ArcProtoId.
     */
    public ArcProtoId readArcProtoId() throws IOException {
        TechId techId = readTechId();
        int chronIndex = readInt();
        return techId.getArcProtoId(chronIndex);
    }

    /**
     * Reads LibId.
     * @return LibId.
     */
    public LibId readLibId() throws IOException {
        int libIndex = in.readInt();
        return idManager.getLibId(libIndex);
    }

    /**
     * Reads NodeProtoId.
     * @return NodeProtoId.
     */
    public NodeProtoId readNodeProtoId() throws IOException {
        int i = in.readInt();
        if (i >= 0) {
            return idManager.getCellId(i);
        } else {
            int chronIndex = ~i;
            TechId techId = readTechId();
            return techId.getPrimitiveNodeId(chronIndex);
        }
    }

    /**
     * Reads PortProtoId.
     * @return PortProtoId.
     */
    public PortProtoId readPortProtoId() throws IOException {
        NodeProtoId nodeProtoId = readNodeProtoId();
        int chronIndex = in.readInt();
        return nodeProtoId.getPortId(chronIndex);
    }

    /**
     * Reads node id.
     * @return node id.
     */
    public int readNodeId() throws IOException {
        return in.readInt();
    }

    /**
     * Reads arc id.
     * @return arc id.
     */
    public int readArcId() throws IOException {
        return in.readInt();
    }

    /**
     * Reads Name key.
     * @return name key.
     */
    public Name readNameKey() throws IOException {
        String name = in.readUTF();
        return Name.findName(name);
    }

    /**
     * Reads Orientation.
     * @return Orientation.
     */
    public Orientation readOrientation() throws IOException {
        int i = in.readInt();
        if (i == orients.size()) {
            short angle = in.readShort();
            boolean isXMirrored = in.readBoolean();
            boolean isYMirrored = in.readBoolean();
            Orientation orient = Orientation.fromJava(angle, isXMirrored, isYMirrored);
            orients.add(orient);
        }
        return orients.get(i);
    }

    /**
     * Reads grid coordiante.
     * @return coordinate.
     */
    public long readCoord() throws IOException {
        return in.readLong();
    }

    /**
     * Reads EPoint.
     * @return EPoint.
     */
    public EPoint readPoint() throws IOException {
        long x = readCoord();
        long y = readCoord();
        return EPoint.fromGrid(x, y);
    }

    /**
     * Reads ERectangle.
     * @return ERectangle.
     */
    public ERectangle readRectangle() throws IOException {
        long x = readCoord();
        long y = readCoord();
        long w = readCoord();
        long h = readCoord();
        return ERectangle.fromGrid(x, y, w, h);
    }
}

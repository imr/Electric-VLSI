/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SnapshotReader.java
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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.prototype.NodeProtoId;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Tool;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 */
public class SnapshotReader {
    
    private final DataInputStream in;
    private final IdManager idManager;
    private final ArrayList<Variable.Key> varKeys = new ArrayList<Variable.Key>();
    private final ArrayList<TextDescriptor> textDescriptors = new ArrayList<TextDescriptor>();
    private final ArrayList<Tool> tools = new ArrayList<Tool>();
    private final ArrayList<Technology> techs = new ArrayList<Technology>();
    private final ArrayList<ArcProto> arcProtos = new ArrayList<ArcProto>();
    private final ArrayList<PrimitiveNode> primNodes = new ArrayList<PrimitiveNode>();
    private final ArrayList<Orientation> orients = new ArrayList<Orientation>();
   
    /** Creates a new instance of SnapshotWriter */
    public SnapshotReader(DataInputStream in, IdManager idManager) {
        if (in == null || idManager == null) throw new NullPointerException();
        this.in = in;
        this.idManager = idManager;
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
        if (i == -1) return null;
        if (i == textDescriptors.size()) {
            long bits = in.readLong();
            int colorIndex = in.readInt();
            boolean isDisplay = in.readBoolean();
            boolean isJava = in.readBoolean();
            String fontName = in.readUTF();

            TextDescriptor.Code code = isJava ? TextDescriptor.Code.JAVA : TextDescriptor.Code.NONE;
            MutableTextDescriptor mtd = new MutableTextDescriptor(bits, colorIndex, isDisplay, code);
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
     * Reads Technology.
     * @return Technology.
     */
    public Technology readTechnology() throws IOException {
        int i = in.readInt();
        if (i == techs.size()) {
            String techName = in.readUTF();
            Technology tech = Technology.findTechnology(techName);
            techs.add(tech);
        }
        return techs.get(i);
    }
    
    /**
     * Reads ArcProto.
     * @return ArcProto.
     */
    public ArcProto readArcProto() throws IOException {
        int i = in.readInt();
        if (i == arcProtos.size()) {
            Technology tech = readTechnology();
            String arcName = in.readUTF();
            ArcProto ap = tech.findArcProto(arcName);
            arcProtos.add(ap);
        }
        return arcProtos.get(i);
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
        if (i >= 0)
            return idManager.getCellId(i);
        i = ~i;
        if (i == primNodes.size()) {
            Technology tech = readTechnology();
            String primName = in.readUTF();
            PrimitiveNode pn = tech.findNodeProto(primName);
            primNodes.add(pn);
        }
        return primNodes.get(i);
    }
    
    /**
     * Reads PortProtoId.
     * @return PortProtoId.
     */
    public PortProtoId readPortProtoId() throws IOException {
        NodeProtoId nodeProtoId = readNodeProtoId();
        int chronIndex = in.readInt();
        if (nodeProtoId instanceof CellId) {
            CellId cellId = (CellId)nodeProtoId;
            return cellId.getPortId(chronIndex);
        } else {
            PrimitiveNode pn = (PrimitiveNode)nodeProtoId;
            return (PrimitivePort)pn.getPort(chronIndex);
        }
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
     * Reads double coordiante.
     * @return coordinate.
     */
    public double readCoord() throws IOException {
        return in.readDouble();
    }
    
    /**
     * Reads EPoint.
     * @return EPoint.
     */
    public EPoint readPoint() throws IOException {
        double x = readCoord();
        double y = readCoord();
        return x != 0 || y != 0 ? new EPoint(x, y) : EPoint.ORIGIN;
    }
}

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
    
    public DataInputStream in;
    private ArrayList<Variable.Key> varKeys = new ArrayList<Variable.Key>();
    private ArrayList<TextDescriptor> textDescriptors = new ArrayList<TextDescriptor>();
    private ArrayList<Tool> tools = new ArrayList<Tool>();
    private ArrayList<Technology> techs = new ArrayList<Technology>();
    private ArrayList<ArcProto> arcProtos = new ArrayList<ArcProto>();
    private ArrayList<PrimitiveNode> primNodes = new ArrayList<PrimitiveNode>();
    private ArrayList<Orientation> orients = new ArrayList<Orientation>();
   
    /** Creates a new instance of SnapshotWriter */
    SnapshotReader(DataInputStream in) {
        this.in = in;
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
        int i = in.readInt();
        return LibId.getByIndex(i);
    }
    
    /**
     * Reads NodeProtoId.
     * @return NodeProtoId.
     */
    public NodeProtoId readNodeProtoId() throws IOException {
        int i = in.readInt();
        if (i >= 0)
            return CellId.getByIndex(i);
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
            while (chronIndex >= cellId.numExportIds())
               cellId.newExportId();
            return cellId.getExportId(chronIndex);
        } else {
            PrimitiveNode pn = (PrimitiveNode)nodeProtoId;
            return (PrimitivePort)pn.getPort(chronIndex);
        }
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
            Orientation orient = Orientation.fromJava(angle,isXMirrored, isYMirrored);
            orients.add(orient);
        }
        return orients.get(i);
    }
    
    /**
     * Reads EPoint.
     * @return EPoint.
     */
    public EPoint readPoint() throws IOException {
        double x = in.readDouble();
        double y = in.readDouble();
        return x != 0 || y != 0 ? new EPoint(x, y) : EPoint.ORIGIN;
    }
}

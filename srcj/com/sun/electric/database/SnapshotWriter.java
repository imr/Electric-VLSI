/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SnapshotWriter.java
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

import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;

import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;

/**
 * Class to write trace of Snapshots to DataOutput byte sequence.
 */
public class SnapshotWriter {
    
    public static DataOutput out;
    private static HashMap<Variable.Key,Integer> varKeys = new HashMap<Variable.Key,Integer>();
    private static HashMap<TextDescriptor,Integer> textDescriptors = new HashMap<TextDescriptor,Integer>();
    
    /** Creates a new instance of SnapshotWriter */
    private SnapshotWriter() {
    }

    /**
     * Writes variable key.
     * @param key variable key to write.
     */
    public static void write(Variable.Key key) throws IOException {
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
    public static void write(TextDescriptor td) throws IOException {
        Integer i = textDescriptors.get(td);
        if (i != null) {
            out.writeInt(i.intValue());
        } else {
            i = new Integer(varKeys.size());
            textDescriptors.put(td, i);
            out.writeInt(i.intValue());
            
            out.writeLong(td.lowLevelGet());
            out.writeInt(td.getColorIndex());
            out.writeBoolean(td.isDisplay());
            out.writeBoolean(td.isJava());
            int face = td.getFace();
            String fontName = face != 0 ? TextDescriptor.ActiveFont.findActiveFont(face).getName() : "";
            out.writeUTF(fontName);
        }
    }
}

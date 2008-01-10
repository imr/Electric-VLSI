/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PrimitivePortId.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.text.Name;
import com.sun.electric.technology.PrimitivePort;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;

/**
 * The PrimitivePortId immutable class identifies primitive port proto independently of threads.
 * It differs from PrimitivePort objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method.
 */
public class PrimitivePortId implements PortProtoId, Serializable {
    /** TechId of this PrimitiveNodeId. */
    public final PrimitiveNodeId parentId;
    /** PrimitiveNode name */
    public final Name name;
    /** PrimitiveNode full name */
    public final String fullName;
    /** Unique index of this PrimtiveNodeId in TechId. */
    public final int chronIndex;
    
    /**
     * PrimitivePortId constructor.
     */
    PrimitivePortId(PrimitiveNodeId parentId, String name, int chronIndex) {
        assert parentId != null;
        if (name.length() == 0 || !TechId.jelibSafeName(name))
            throw new IllegalArgumentException("PrimtiveNodeId.name");
        this.parentId = parentId;
        this.name = Name.findName(name);
        fullName = parentId.fullName + ":" + name;
        this.chronIndex = chronIndex;
    }
    
    private Object writeReplace() { return new PrimitivePortIdKey(this); }
    
    private static class PrimitivePortIdKey extends EObjectInputStream.Key<PrimitivePortId> {
        public PrimitivePortIdKey() {}
        private PrimitivePortIdKey(PrimitivePortId primitivePortId) { super(primitivePortId); }
        
        @Override
        public void writeExternal(EObjectOutputStream out, PrimitivePortId primitivePortId) throws IOException {
            PrimitiveNodeId parentId = primitivePortId.parentId;
            TechId techId = parentId.techId;
            if (techId.idManager != out.getIdManager())
                throw new NotSerializableException(primitivePortId + " from other IdManager");
            out.writeInt(techId.techIndex);
            out.writeInt(parentId.chronIndex);
            out.writeInt(primitivePortId.chronIndex);
        }
        
        @Override
        public PrimitivePortId readExternal(EObjectInputStream in) throws IOException, ClassNotFoundException {
            int techIndex = in.readInt();
            int nodeChronIndex = in.readInt();
            int portChronIndex = in.readInt();
            return in.getIdManager().getTechId(techIndex).getPrimitiveNodeId(nodeChronIndex).getPrimitivePortId(portChronIndex);
        }
    }
    
	/**
	 * Method to return the parent NodeProtoId of this PortProtoId.
	 * @return the parent NodeProtoId of this PortProtoId.
	 */
	public PrimitiveNodeId getParentId() { return parentId; }

    /**
     * Method to return chronological index of this PortProtoId in parent.
     * @return chronological index of this PortProtoId in parent.
     */
    public int getChronIndex() { return chronIndex; }
    
	/**
	 * Method to return the name key of this PortProtoId in a specified Snapshot.
     * @param snapshot snapshot for name search.
	 * @return the Name key of this PortProtoId.
	 */
	public Name getNameKey(Snapshot snapshot) { return name; }

	/**
	 * Method to return the name of this PortProtoId in a specified Snapshot.
     * @param snapshot snapshot for name search.
	 * @return the name of this PortProtoId.
	 */
	public String getName(Snapshot snapshot) { return name.toString(); }

   /**
     * Method to return the PortProto representing PortProtoId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the PortProto representing PortProtoId in the specified database.
     * This method is not properly synchronized.
     */
    public PrimitivePort inDatabase(EDatabase database) {
        return database.getTechPool().getPrimitivePort(this);
    }
    
	/**
	 * Returns a printable version of this PrimitivePortId.
	 * @return a printable version of this PrimitivePortId.
	 */
    @Override
    public String toString() { return fullName; }
    
	/**
	 * Checks invariants in this PrimitivePortId.
     * @exception AssertionError if invariants are not valid
	 */
    void check() {
        assert this == parentId.getPrimitivePortId(chronIndex);
        assert name.toString().length() > 0 && TechId.jelibSafeName(name.toString());
    }
}

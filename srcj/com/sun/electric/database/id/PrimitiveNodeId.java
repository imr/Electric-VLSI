/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PrimitiveNodeId.java
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
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.technology.PrimitiveNode;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The PrimitiveNodeId immutable class identifies primitive node proto independently of threads.
 * It differs from PrimitiveNode objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method.
 */
public class PrimitiveNodeId implements NodeProtoId, Serializable {
    /** TechId of this PrimitiveNodeId. */
    public final TechId techId;
    /** PrimitiveNode name */
    public final String name;
    /** PrimitiveNode full name */
    public final String fullName;
    /** Unique index of this PrimtiveNodeId in TechId. */
    public final int chronIndex;
    /** List of PrimitivePortIds created so far. */
    final ArrayList<PrimitivePortId> primitivePortIds = new ArrayList<PrimitivePortId>();
    /** HashMap of PrimitivePortIds by their name. */
    private final HashMap<String,PrimitivePortId> primitivePortIdsByName = new HashMap<String,PrimitivePortId>();

    /**
     * PrimtiveNodeId constructor.
     */
    PrimitiveNodeId(TechId techId, String name, int chronIndex) {
        assert techId != null;
        if (name.length() == 0 || !TechId.jelibSafeName(name))
            throw new IllegalArgumentException("PrimtiveNodeId.name");
        this.techId = techId;
        this.name = name;
        fullName = techId.techName + ":" + name;
        this.chronIndex = chronIndex;
    }
    
    private Object writeReplace() { return new PrimitiveNodeIdKey(this); }
    
    private static class PrimitiveNodeIdKey extends EObjectInputStream.Key<PrimitiveNodeId> {
        public PrimitiveNodeIdKey() {}
        private PrimitiveNodeIdKey(PrimitiveNodeId primitiveNodeId) { super(primitiveNodeId); }
        
        @Override
        public void writeExternal(EObjectOutputStream out, PrimitiveNodeId primitiveNodeId) throws IOException {
            TechId techId = primitiveNodeId.techId;
            if (techId.idManager != out.getIdManager())
                throw new NotSerializableException(primitiveNodeId + " from other IdManager");
            out.writeInt(techId.techIndex);
            out.writeInt(primitiveNodeId.chronIndex);
        }
        
        @Override
        public PrimitiveNodeId readExternal(EObjectInputStream in) throws IOException, ClassNotFoundException {
            int techIndex = in.readInt();
            int chronIndex = in.readInt();
            return in.getIdManager().getTechId(techIndex).getPrimitiveNodeId(chronIndex);
        }
    }
    
    /**
     * Returns a number PrimitivePortIds in this PrimitiveNodeId.
     * This number may grow in time.
     * @return a number of PrimitivePortIds.
     */
    synchronized int numPrimitivePortIds() {
        return primitivePortIds.size();
    }
    
    /**
     * Returns PrimitivePortId in this PrimitiveNodeId with specified chronological index.
     * @param chronIndex chronological index of PrimitivePortId.
     * @return PrimitivePortId with specified chronological index.
     * @throws ArrayIndexOutOfBoundsException if no such PrimitivePortId.
     */
    synchronized PrimitivePortId getPrimitivePortId(int chronIndex) {
        return primitivePortIds.get(chronIndex);
    }
    
    /**
     * Returns PrimitivePortId with specified primitivePortName.
     * @param primitivePortName primitive port name.
     * @return PrimitiveNodeId with specified primitivePortName.
     */
    public synchronized PrimitivePortId newPrimitivePortId(String primitivePortName) {
        PrimitivePortId primitivePortId = primitivePortIdsByName.get(primitivePortName);
        return primitivePortId != null ? primitivePortId : newPrimitivePortIdInternal(primitivePortName);
    }
    
    private PrimitivePortId newPrimitivePortIdInternal(String primitivePortName) {
        int chronIndex = primitivePortIds.size();
        PrimitivePortId primitivePortId = new PrimitivePortId(this, primitivePortName, primitivePortIds.size());
        primitivePortIds.add(primitivePortId);
        primitivePortIdsByName.put(primitivePortId.name.toString(), primitivePortId);
        assert primitivePortIds.size() == primitivePortIdsByName.size();
        synchronized (techId) {
            techId.modCount++;
        }
        return primitivePortId;
    }
    
    /**
     * Returns PortProtoId in this node proto with specified chronological index.
     * @param chronIndex chronological index of ExportId.
     * @return PortProtoId whith specified chronological index.
     * @throws ArrayIndexOutOfBoundsException if no such ExportId.
     */
    public PrimitivePortId getPortId(int chronIndex) {
        return primitivePortIds.get(chronIndex);
    }
    
   /**
     * Method to return the NodeProto representing NodeProtoId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the NodeProto representing NodeProtoId in the specified database.
     * This method is not properly synchronized.
     */
    public PrimitiveNode inDatabase(EDatabase database) {
        return database.getTechPool().getPrimitiveNode(this);
    }
    
	/**
	 * Returns a printable version of this ArcProtoId.
	 * @return a printable version of this ArcProtoId.
	 */
    @Override
    public String toString() { return fullName; }
    
	/**
	 * Checks invariants in this ArcProtoId.
     * @exception AssertionError if invariants are not valid
	 */
    void check() {
        assert this == techId.getPrimitiveNodeId(chronIndex);
        assert name.length() > 0 && TechId.jelibSafeName(name);
        for (PrimitivePortId primitivePortId: primitivePortIds)
            primitivePortId.check();
        for (Map.Entry<String,PrimitivePortId> e: primitivePortIdsByName.entrySet()) {
            PrimitivePortId primitivePortId = e.getValue();
            assert primitivePortId.parentId == this;
            assert primitivePortId.name.toString() == e.getKey();
            primitivePortId.check();
        }
        for (int portChronIndex = 0; portChronIndex < primitivePortIds.size(); portChronIndex++) {
            PrimitivePortId primitivePortId = primitivePortIds.get(portChronIndex);
            primitivePortId.check();
            assert primitivePortIdsByName.get(primitivePortId.name.toString()) == primitivePortId;
        }
    }
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PortProtoId.java
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

import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.prototype.PortProto;

import java.io.IOException;
import java.io.Serializable;

/**
 * The PortProtoId class identifies a type of PortInst .
 * It can be implemented as PrimitiveNodeId (for primitives from Technologies)
 * or as ExportId (for cells in Libraries).
 * <P>
 * The PortProtoId is immutable and identifies PortProto independently of threads. It differs from PortProto objects,
 * which are owned by threads in transactional database.
 */
public abstract class PortProtoId implements Serializable {

    /** Parent NodeProtoId of this PortProtoId */
    public final NodeProtoId parentId;
    /** chronological index of this PortProtoId in parent. */
    public final int chronIndex;
    /** representation of PortProtoId in disk files.
     * This name isn't chaged when Export is renamed.
     */
    public final String externalId;

    /**
     * PortPortId constructor.
     */
    PortProtoId(NodeProtoId parentId, String externalId, int chronIndex) {
        assert parentId != null;
        this.parentId = parentId;
        this.chronIndex = chronIndex;
        this.externalId = externalId;
    }

    Object writeReplace() {
        return new PortProtoIdKey(this);
    }

    private static class PortProtoIdKey extends EObjectInputStream.Key<PortProtoId> {

        public PortProtoIdKey() {
        }

        private PortProtoIdKey(PortProtoId portProtoId) {
            super(portProtoId);
        }

        @Override
        public void writeExternal(EObjectOutputStream out, PortProtoId portProtoId) throws IOException {
            out.writeObject(portProtoId.parentId);
            out.writeInt(portProtoId.chronIndex);
        }

        @Override
        public PortProtoId readExternal(EObjectInputStream in) throws IOException, ClassNotFoundException {
            NodeProtoId parentId = (NodeProtoId) in.readObject();
            int chronIndex = in.readInt();
            return parentId.getPortId(chronIndex);
        }
    }

    /**
     * Method to return the parent NodeProtoId of this PortProtoId.
     * @return the parent NodeProtoId of this PortProtoId.
     */
    public NodeProtoId getParentId() {
        return parentId;
    }

    /**
     * Method to return chronological index of this PortProtoId in parent.
     * @return chronological index of this PortProtoId in parent.
     */
    public int getChronIndex() {
        return chronIndex;
    }

    /**
     * Method to return the representation of this PortProtoId in disk files.
     */
    public String getExternalId() {
        return externalId;
    }

    /**
     * Method to return the name of this PortProtoId in a specified Snapshot.
     * @param snapshot snapshot for name search.
     * @return the name of this PortProtoId.
     */
    public abstract String getName(Snapshot snapshot);

    /**
     * Method to return the PortProto representing PortProtoId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the PortProto representing PortProtoId in the specified database.
     * This method is not properly synchronized.
     */
    public abstract PortProto inDatabase(EDatabase database);

    @Override
    public int hashCode() {
        return externalId.hashCode();
    }

    @Override
    public String toString() {
        return parentId + ":" + externalId;
    }

    /**
     * Check invariants of this ExportId.
     * @throws AssertionError if this ExportId is not valid.
     */
    void check() {
        assert this == parentId.getPortId(chronIndex);
        assert externalId != null;
    }
}

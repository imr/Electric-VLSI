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

import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.technology.PrimitivePort;

/**
 * The PrimitivePortId immutable class identifies primitive port proto independently of threads.
 * It differs from PrimitivePort objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method.
 */
public class PrimitivePortId extends PortProtoId {

    /**
     * PrimitivePortId constructor.
     */
    PrimitivePortId(PrimitiveNodeId parentId, String externalId, int chronIndex) {
        super(parentId, externalId, chronIndex);
        if (!TechId.jelibSafeName(externalId)) {
            throw new IllegalArgumentException("PortProtoId.name");
        }
    }

    /**
     * Method to return the parent NodeProtoId of this PortProtoId.
     * @return the parent NodeProtoId of this PortProtoId.
     */
    @Override
    public PrimitiveNodeId getParentId() {
        return (PrimitiveNodeId) parentId;
    }

    /**
     * Method to return the name of this PortProtoId in a specified Snapshot.
     * @param snapshot snapshot for name search.
     * @return the name of this PortProtoId.
     */
    @Override
    public String getName(Snapshot snapshot) {
        PrimitivePort pp = inSnapshot(snapshot);
        return pp != null ? pp.getName() : null;
    }

    /**
     * Method to return the PrimitivePort representing PrimitivePortId in the specified Snapshot.
     * @param snapshot Snapshot where to get from.
     * @return the PrimitivePort representing PrimitivePortId in the specified snapshot.
     */
    public PrimitivePort inSnapshot(Snapshot snapshot) {
        return snapshot.getTechPool().getPrimitivePort(this);
    }

    /**
     * Method to return the PortProto representing PortProtoId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the PortProto representing PortProtoId in the specified database.
     * This method is not properly synchronized.
     */
    @Override
    public PrimitivePort inDatabase(EDatabase database) {
        return database.getTechPool().getPrimitivePort(this);
    }

    /**
     * Checks invariants in this PrimitivePortId.
     * @exception AssertionError if invariants are not valid
     */
    @Override
    void check() {
        super.check();
        assert TechId.jelibSafeName(externalId);
    }
}

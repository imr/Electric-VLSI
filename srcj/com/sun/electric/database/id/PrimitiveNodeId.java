/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PrimitiveNodeId.java
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

import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.prototype.NodeProto;

/**
 * The PrimitiveNodeId immutable class identifies primitive node proto independently of threads.
 * It differs from PrimitiveNode objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method.
 */
public abstract class PrimitiveNodeId implements NodeProtoId {
    /** TechId of this PrimitiveNodeId. */
    public final TechId techId;
    /** PrimitiveNode name */
    public final String name;
    /** PrimitiveNode full name */
    public final String fullName;
    /** Unique index of this PrimtiveNodeId in TechId. */
    public final int chronIndex;

    /**
     * PrimtiveNodeId constructor.
     */
    protected PrimitiveNodeId(TechId techId, String name, int chronIndex) {
        assert techId != null;
        if (name.length() == 0 || !TechId.jelibSafeName(name))
            throw new IllegalArgumentException("PrimtiveNodeId.name");
        this.techId = techId;
        this.name = name;
        fullName = techId.techName + ":" + name;
        this.chronIndex = chronIndex;
    }
    
    /**
     * Returns PortProtoId in this node proto with specified chronological index.
     * @param chronIndex chronological index of ExportId.
     * @return PortProtoId whith specified chronological index.
     * @throws ArrayIndexOutOfBoundsException if no such ExportId.
     */
    public abstract PortProtoId getPortId(int chronIndex);
    
   /**
     * Method to return the NodeProto representing NodeProtoId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the NodeProto representing NodeProtoId in the specified database.
     * This method is not properly synchronized.
     */
    public abstract NodeProto inDatabase(EDatabase database);
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PortProtoId.java
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
package com.sun.electric.database.prototype;

import com.sun.electric.database.hierarchy.EDatabase;

/**
 * The PortProtoId interface identifies a type of PortInst .
 * It can be implemented as PrimitiveNode (for primitives from Technologies)
 * or as ExportId (for cells in Libraries).
 * <P>
 * The PortProtoId is immutable and identifies PortProto independently of threads. It differs from PortProto objects,
 * some of them (Exports) will be owned by threads in transactional database. PrimitivePorts will
 * be shared too, so they are both PortProtoId and PortProto.
 */
public interface PortProtoId
{
	/**
	 * Method to return the parent NodeProtoId of this PortProtoId.
	 * @return the parent NodeProtoId of this PortProtoId.
	 */
	public NodeProtoId getParentId();

    /**
     * Method to return chronological index of this PortProtoId in parent.
     * @return chronological index of this PortProtoId in parent.
     */
    public int getChronIndex();
    
   /**
     * Method to return the PortProto representing PortProtoId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the PortProto representing PortProtoId in the specified database.
     * This method is not properly synchronized.
     */
    public PortProto inDatabase(EDatabase database);
}

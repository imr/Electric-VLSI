/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NodeProtoId.java
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

/**
 * The NodeProtoId interface identifies a type of NodeInst .
 * It can be implemented as PrimitiveNode (for primitives from Technologies)
 * or as CellId (for cells in Libraries).
 * <P>
 * The NodeProtoId is immutable and identifies NodeProto independently of threads. It differs from NodeProto objects,
 * some of them (Cells) will be owned by threads  in transactional database. PrimitiveNodes will
 * be shared too, so they are both NodeProtoId and NodeProto.
 */
public interface NodeProtoId
{
	/**
	 * Method to return the NodeProto representiong NodeProtoId in the current thread.
	 * @return the NodeProto representing NodeProtoId in the current thread.
	 */
    NodeProto inThisThread();
}

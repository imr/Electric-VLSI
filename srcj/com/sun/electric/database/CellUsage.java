/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellUsage.java
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

import com.sun.electric.database.hierarchy.Cell;

/**
 * Immutable class to represent a possible edge in a Cell call graph.
 * Vertices of the edge are CellIds of parent cell and proto subcell.
 * This possible edge can be an edge in one thread, and not an edge in another thread.
 */
public final class CellUsage
{
    /** Empty CellUsage array for initialization. */
    public static final CellUsage[] NULL_ARRAY = {};

    /** CellId of the parent Cell */
    public final CellId parentId;
	/** CellId of the (prototype) subCell */
    public final CellId protoId;
    
	// ---------------------- private data ----------------------------------
    public final int indexInParent;

	// --------------------- private and protected methods ---------------------

	/**
	 * The constructor.
	 */
	CellUsage(CellId parentId, CellId protoId, int indexInParent)
	{
		// initialize this object
        this.parentId = parentId;
		this.protoId = protoId;
        this.indexInParent = indexInParent;
	}
    
	// ------------------------ public methods -------------------------------

	/**
	 * Method to return the prototype of this NodeUsage.
	 * @return the prototype of this NodeUsage in this thread.
	 */
	public Cell getProto() { return Cell.inCurrentThread(protoId); }

	/**
	 * Method to return the Cell that contains this Geometric object.
	 * @return the Cell that contains this Geometric object in this thread.
	 */
	public Cell getParent() { return Cell.inCurrentThread(parentId); }

	/**
	 * Returns a printable version of this CellUsage.
	 * @return a printable version of this CellUsage.
	 */
	public String toString()
	{
		return "CellUsage of " + protoId + " in " + parentId;
	}

	/**
	 * Checks invariants in this CellUsage.
	 * @exception AssertionError if invariants are not valid
	 */
    void check() {
        // Check that this CellUsage is properly owned by its parentId.
        assert this == parentId.getUsageIn(indexInParent);
        
        // Check that this CellUsage is properly inserted in the hash.
        // If this is true for all CellUsages in parentId.usagesId then
        // all CellUsages in parentId.usagesIn have distinct protoIds.
        assert this == parentId.getUsageIn(protoId, false);
        
        // Check that protoId.usagesOf contains exactly one CellUsage with my protoId,
        // and it's me.
        int count = 0;
        for (int i = 0, protoNumOf = protoId.numUsagesOf(); i < protoNumOf; i++) {
            CellUsage u = protoId.getUsageOf(i);
            if (u.parentId == parentId) {
                assert u == this;
                count++;
            }
        }
        assert count == 1;
    }
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibId.java
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

import com.sun.electric.database.hierarchy.Library;


/**
 * The LibId immutable class identifies library independently of threads.
 * It differs from Library objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method.
 */
public final class LibId
{
    /** Unique index of this cell in the database. */
    public final int libIndex = numLibIds++;
    
    /** Number of LibraryIds created so far. */
    private static volatile int numLibIds = 0;
    
    /**
     * LibId constructor.
     * Creates LibId with unique libIndex.
     */
    public LibId() {}
    
    /**
     * Method to return the Library representiong LibId in the current thread.
     * @return the Library representing LibId in the current thread.
     * This method is not properly synchronized.
     */
    public Library inCurrentThread() { return Library.inCurrentThread(this); }
    
	/**
	 * Returns a printable version of this CellId.
	 * @return a printable version of this CellId.
	 */
    public String toString() {
        String s = "LibraryId#" + libIndex;
        Library lib = Library.inCurrentThread(this);
        if (lib != null) s += "(" + lib.getName() + ")";
        return s;
    }
}

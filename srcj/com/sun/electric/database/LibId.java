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

import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * The LibId immutable class identifies library independently of threads.
 * It differs from Library objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method.
 */
public final class LibId implements Serializable {
    
    /** IdManager which owns this LibId. */
    public final IdManager idManager;
    /** Unique index of this lib in the database. */
    public final int libIndex;
    
    /**
     * LibId constructor.
     */
    LibId(IdManager idManager, int libIndex) {
        this.idManager = idManager;
        this.libIndex = libIndex;
    }
    
    private Object writeReplace() throws ObjectStreamException { return new LibIdKey(this); }
    private Object readResolve() throws ObjectStreamException { throw new InvalidObjectException("LibId"); }
    
    private static class LibIdKey extends EObjectInputStream.Key {
        private final int libIndex;
        
        private LibIdKey(LibId libId) {
            libIndex = libId.libIndex;
        }
        
        protected Object readResolveInDatabase(EDatabase database) throws InvalidObjectException {
            return database.getIdManager().getLibId(libIndex);
        }
    }
         
    /**
     * Method to return the Library representing LibId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the Library representing LibId in the specified database.
     * This method is not properly synchronized.
     */
    public Library inDatabase(EDatabase database) { return database.getLib(this); }
    
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

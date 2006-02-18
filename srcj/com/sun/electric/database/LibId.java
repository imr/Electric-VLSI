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
import com.sun.electric.tool.Job;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;


/**
 * The LibId immutable class identifies library independently of threads.
 * It differs from Library objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method.
 */
public final class LibId implements Serializable
{
    /** Unique index of this lib in the database. */
    public final int libIndex;
    
     /** List of LibIds created so far. */
    private static final ArrayList<LibId> libIds = new ArrayList<LibId>();
    
    /**
     * LibId constructor.
     * Creates LibId with unique libIndex.
     */
    public LibId() {
        synchronized(libIds) {
            libIndex = libIds.size();
            libIds.add(this);
        }
    }
    
    /*
     * Resolve method for deserialization.
     */
    private Object readResolve() throws ObjectStreamException {
        return getByIndex(libIndex);
    }
    
    /**
     * Returns LibId by given index.
     * @param libIndex given index.
     * @return LibId with given index.
     */
    public static LibId getByIndex(int libIndex) {
        synchronized(libIds) {
           while (libIndex >= libIds.size())
               new LibId();
           return libIds.get(libIndex);
        }
    }
    
    /**
     * Method to return the Library representing LibId in the server EDatabase.
     * @return the Library representing LibId in the server database.
     * This method is not properly synchronized.
     */
    public Library inServerDatabase() { return inDatabase(EDatabase.serverDatabase()); }
    
    /**
     * Method to return the Library representing LibId in the client EDatabase.
     * @param database EDatabase where to get from.
     * @return the Library representing LibId in the cleint database.
     * This method is not properly synchronized.
     */
    public Library inClientDatabase() { return inDatabase(EDatabase.clientDatabase()); }
    
    /**
     * Method to return the Library representing LibId in the database of current thread.
     * @param database EDatabase where to get from.
     * @return the Library representing LibId in database of current thread.
     * This method is not properly synchronized.
     */
    public Library inThreadDatabase() { return inDatabase(Job.threadDatabase()); }
    
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

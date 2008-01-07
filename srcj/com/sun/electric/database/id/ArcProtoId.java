/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ArcProtoId.java
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
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;

/**
 * The ArcProtoId immutable class identifies arc proto independently of threads.
 * It differs from ArcProto objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method.
 */
public class ArcProtoId {
    /** TechId of this ArcProtoId. */
    public final TechId techId;
    /** ArcProto name */
    public final String name;
    /** ArcProto full name */
    public final String fullName;
    /** Unique index of this ArcProtoId in TechId. */
    public final int chronIndex;
    
    /**
     * ArcProtoId constructor.
     */
    ArcProtoId(TechId techId, String name, int chronIndex) {
        assert techId != null;
        if (name.length() == 0 || !TechId.jelibSafeName(name))
            throw new IllegalArgumentException("ArcProtoId.name");
        this.techId = techId;
        this.name = name;
        fullName = techId.techName + ":" + name;
        this.chronIndex = chronIndex;
     }
    
    private Object writeReplace() { return new ArcProtoIdKey(this); }
    private Object readResolve() throws ObjectStreamException { throw new InvalidObjectException("ArcProtoId"); }
    
    private static class ArcProtoIdKey extends EObjectInputStream.Key {
        private final int techIndex;
        private final int chronIndex;
       
        private ArcProtoIdKey(ArcProtoId arcProtoId) {
            techIndex = arcProtoId.techId.techIndex;
            chronIndex = arcProtoId.chronIndex;
        }
        
        protected Object readResolveInDatabase(EDatabase database) throws InvalidObjectException {
            return database.getIdManager().getTechId(techIndex).getArcProtoId(chronIndex);
        }
    }
         
    /**
     * Method to return the ArcProto representing ArcProtoId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the ArcProto representing ArcProtoId in the specified database.
     * This method is not properly synchronized.
     */
    public ArcProto inDatabase(EDatabase database) {
        Technology tech = techId.inDatabase(database);
        return tech != null ? tech.findArcProto(name) : null;
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
        assert this == techId.getArcProtoId(chronIndex);
        assert name.length() > 0 && TechId.jelibSafeName(name);
    }
}

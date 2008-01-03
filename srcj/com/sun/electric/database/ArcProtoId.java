/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ArcProtoId.java
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
package com.sun.electric.database;

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
    /** Unique index of this ArcProtoId in TechId. */
    public final int chronIndex;
    
    /**
     * ArcProtoId constructor.
     */
    ArcProtoId(TechId techId, int chronIndex, String name) {
        assert techId != null;
        if (name.length() == 0)
            throw new IllegalArgumentException("ArcProtoId.name");
        this.techId = techId;
        this.chronIndex = chronIndex;
        this.name = name;
    }
}

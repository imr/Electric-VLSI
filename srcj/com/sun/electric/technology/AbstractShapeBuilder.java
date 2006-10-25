/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AbstractShapeBuilder.java
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
package com.sun.electric.technology;

import com.sun.electric.database.CellBackup;

/**
 * A support class to build shapes of arcs and nodes.
 */
public abstract class AbstractShapeBuilder {
    private long[] points = new long[10];
    private CellBackup.Memoization m;
    
    /** Creates a new instance of AbstractShapeBuilder */
    public AbstractShapeBuilder() {
    }
    
    public long[] getPoints(int count) {
        if (count*2 > points.length)
            points = new long[Math.max(count*2, points.length*2)];
        return points;
    }
    
    public CellBackup.Memoization getMemoization() {
        return m;
    }
}

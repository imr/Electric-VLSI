/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ManhattanOrientation.java
 *
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.api.minarea;

import java.awt.geom.AffineTransform;

/**
 * Enumeration to specify Manhattan orientation.
 * The constants are standard EDIF orientations.
 */
public enum ManhattanOrientation {
    
    R0,
    R90,
    R180,
    R270,
    MY,
    MX,
    MYR90,
    MXR90;
    
    public void transformPoints(long[] coords, int offset, int count) {
        
    }
    
    public void transformRects(long[] coors, int offset, int coubt) {
        
    }
    
    public ManhattanOrientation concatenate(ManhattanOrientation other) {
        return null;
    }
    
    public AffineTransform affineTrasnform() {
        return null;
    }
}

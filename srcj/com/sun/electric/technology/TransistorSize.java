/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TransistorSize.java
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

import com.sun.electric.database.variable.VarContext;

/**
 * Holds the Width and Length of a PrimitiveNode that is a transistor.
 * This holds the width and length as objects, because the width and length
 * may be specified as strings if they are java code, or just numbers.
 */
public class TransistorSize {

    private Object width;
    private Object length;

    public TransistorSize(Object width, Object length) {
        this.width = width;
        this.length = length;
    }

    public Object getWidth() { return width; }

    public Object getLength() { return length; }

    /**
     * Gets the width *ONLY IF* the width can be converted to a double,
     * i.e. it is a Number or a parsable String. If it is some other type,
     * this method returns zero.
     * @return the width
     */
    public double getDoubleWidth() {
        return VarContext.objectToDouble(width, 0);
    }

    /**
     * Gets the length *ONLY IF* the length can be converted to a double,
     * i.e. it is a Number or a parsable String. If it is some other type,
     * this method returns zero.
     * @return the length
     */
    public double getDoubleLength() {
        return VarContext.objectToDouble(length, 0);
    }

}

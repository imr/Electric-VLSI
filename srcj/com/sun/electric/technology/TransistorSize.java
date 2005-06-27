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
 * This holds the width, length, and area as objects, because the width and length,
 * may be specified as strings if they are java code, or just numbers.
 */
public class TransistorSize {
    private final Object width;
    private final Object length;
    private final Object activeLength; // extension of OD on PO for parasitic calculation

	/**
	 * Constructor creates a TransistorSize with a given size.
	 * @param width the width of the TransistorSize.
	 * @param length the length of the TransistorSize.
	 * @param activeLen the length of the active area of the TransistorSize.
	 */
    public TransistorSize(Object width, Object length, Object activeLen) {
        this.width = width;
        this.length = length;
        this.activeLength = activeLen;
    }

	/**
	 * Method to return the width of this TransistorSize.
	 * @return the width of this TransistorSize.
	 */
    public Object getWidth() {return width;}

	/**
	 * Method to return the length of this TransistorSize.
	 * @return the length of this TransistorSize.
	 */
    public Object getLength() {return length; }

	/**
	 * Method to return the length of the active area of this TransistorSize.
	 * @return the length of the active area of this TransistorSize.
	 */
    public Object getActiveLength() {return activeLength; }

    /**
     * Gets the width *ONLY IF* the width can be converted to a double.
     * i.e. it is a Number or a parsable String. If it is some other type,
     * this method returns zero.
     * @return the width.
     */
    public double getDoubleWidth() {
    	return VarContext.objectToDouble(width, 0);
    }

    /**
     * Gets the length *ONLY IF* the length can be converted to a double.
     * i.e. it is a Number or a parsable String. If it is some other type,
     * this method returns zero.
     * @return the length.
     */
    public double getDoubleLength() {
        return VarContext.objectToDouble(length, 0);
    }

    /**
     * Gets the area *ONLY IF* the width and length can be converted to a double.
     * i.e. they are Numbers or a parsable Strings. If they are some other type,
     * this method returns zero.
     * @return the area.
     */
    public double getDoubleArea() {
    	return getDoubleWidth() * getDoubleLength();
    }

    /**
     * Gets the active length *ONLY IF* the active length can be converted to a double.
     * i.e. they are Numbers or a parsable Strings. If they are some other type,
     * this method returns zero.
     * @return the active length.
     */
    public double getDoubleActiveLength() {
    	return VarContext.objectToDouble(activeLength, 0);
    }
}

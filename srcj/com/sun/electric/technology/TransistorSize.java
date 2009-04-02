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
package com.sun.electric.technology;

import com.sun.electric.database.variable.VarContext;

/**
 * Holds the Width and Length of a PrimitiveNode that is a transistor.
 * This holds the width, length, and area as objects, because the width and length,
 * may be specified as strings if they are java code, or just numbers.
 */
public class TransistorSize extends PrimitiveNodeSize {
    private final Object activeLength; // extension of OD on PO for parasitic calculation
    private final Object mFactor; // m multiplier factor.

    /**
	 * Constructor creates a TransistorSize with a given size.
	 * @param width the width of the TransistorSize.
     * @param length the length of the TransistorSize.
     * @param activeLen the length of the active area of the TransistorSize.
     * @param polyAlignX
     * @param mFactor the m multiplier factor.  This is only populated for schematics.
     * 
     */
    public TransistorSize(Object width, Object length, Object activeLen, Object mFactor, boolean polyAlignX) {
        super(width, length, polyAlignX);
        this.activeLength = activeLen;
        this.mFactor = mFactor;
    }

	/**
	 * Method to return the length of the active area of this TransistorSize.
	 * @return the length of the active area of this TransistorSize.
	 */
    public Object getActiveLength() {return activeLength; }


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
    
    /**
     * Gets the m multiplier factor *ONLY IF* the m factor can be converted to a double.
     * Otherwise, returns 1.
     * @return the m factor.
     */
    public double getMFactor() {
    	return VarContext.objectToDouble(mFactor, 1);
    }
}

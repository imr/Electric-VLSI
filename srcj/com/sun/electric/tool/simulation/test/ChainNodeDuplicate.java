/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ChainNodeDuplicate.java
 * Written by Jonathan Gainsley, Sun Microsystems.
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
package com.sun.electric.tool.simulation.test;

/**
 * Wraps an existing ChainNode with this class to create a duplicate chain node that
 * is backed with the same data as the original ChainNode.
 * <P>
 * This specifically handles the two chains ExTest and SamplePreload which have different
 * names and opcodes, but access the same chain on chip. Thus the data structures
 * that track the bits on chip must also be the same in software.
 * <P>
 * The parts that are unique to this chain are only the name and the opcode.
 */
public class ChainNodeDuplicate extends ChainNode {

    private ChainNode original;

    public ChainNodeDuplicate(String name, String opcode, ChainNode original) {
        super(name, opcode, original.getLength(), original.getComment());
        this.original = original;
        for (int i=0; i<original.getChildCount(); i++) {
            this.addChild(original.getChildAt(i));
        }
        createBitVectors();
    }

    protected void createBitVectors() {
        if (original != null) {
            inBits = original.getInBits();
            outBitsExpected = original.getOutBitsExpected();
            oldOutBitsExpected = original.getOldOutBitsExpected();
            outBits = original.getOutBits();
            shadowState = original.getShadowState();
        }
    }

}

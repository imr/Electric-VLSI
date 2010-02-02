/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ChipNode.java
 * Written by Eric Kim, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
 * represent chip node in tree.
 */
class ChipNode extends TestNode {

    /** Length of the JTAG controller's instruction register */
    private int lengthIR;

    public ChipNode(String name, int lengthIR, String comment) {
        super(name, comment);
        this.lengthIR = lengthIR;
    }

    public String toString() {
        return super.toString() + " (IR=" + lengthIR + ")";
    }

    /**
     * Returns length of the JTAG controller's instruction register
     * 
     * @return Length in bits
     */
    int getLengthIR() {
        return lengthIR;
    }

    /** Helper for CompareXML */
    void compare(ChipNode that, String thisFile, String thatFile) {
        System.out.println("Differences for chip " + this);
        super.compare(that, thisFile, thatFile);
        if (this.getLengthIR() != that.getLengthIR()) {
            System.out.println("**** Chip " + getName() + " has IR length "
                    + getLengthIR() + " in " + thisFile + " but "
                    + that.getLengthIR() + " in " + thatFile);
        }

        int nchainThis = getChildCount();
        int nchainThat = that.getChildCount();
        if (nchainThis != nchainThat) {
            System.out.println("**** Chip " + thisFile + ":" + getPathString(1)
                    + " contains " + nchainThis + " chains, but " + thatFile
                    + ":" + that.getPathString(1) + " contains " + nchainThat
                    + ".  Comparison aborted.");
            Infrastructure.exit(1);
        }
        for (int ichain = 0; ichain < nchainThis; ichain++) {
            ChainNode thisChain = (ChainNode) getChildAt(ichain);
            ChainNode thatChain = (ChainNode) that.getChildAt(ichain);
            thisChain.compare(thatChain, thisFile, thatFile);
        }
    }

}

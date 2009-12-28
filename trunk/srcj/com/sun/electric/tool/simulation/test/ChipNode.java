package com.sun.electric.tool.simulation.test;

/**
 * represent chip node in tree.
 * 
 * @author Eric Kim
 * @version 1.0 9/3/03
 *
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
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

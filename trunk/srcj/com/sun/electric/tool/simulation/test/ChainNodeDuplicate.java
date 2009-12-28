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
 * <P>
 *
 * User: gainsley
 * Date: Mar 31, 2008
 * Time: 10:49:44 AM
 * To change this template use File | Settings | File Templates.
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

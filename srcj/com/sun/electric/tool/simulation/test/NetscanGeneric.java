/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetscanGeneric.java
 * Written by Eric Kim and Tom O'Neill, Sun Microsystems.
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
 * Generic initialization, configuration, and connection API for Corelis
 * boundary scan controllers (JTAG testers). Currently the one-port NET-1149.1/E
 * and the four-port NETUSB-1149.1/E are supported. Abstract methods
 * <code>hw_*</code> must be overridden to provide hardware-specific control
 * of the JTAG tester.
 * <p>
 * At most one Corelis JTAG tester is supported per program, because a) the
 * vendor-supplied interface methods are all static access and b) the one- and
 * four-port libraries have naming conflicts and so cannot be loaded at the same
 * time.
 * <p>
 * Shifting data in and out should instead be performed using
 * {@link ChainControl}. All of the methods could have been static, but we made
 * them non-static to allow device-independent JTAG control.
 */
public abstract class NetscanGeneric extends JtagTester {

    /** Default stop state for scan. Value equals 1: Run-Test/Idle. */
    public static final short DEFAULT_STOP_STATE = 1;

    /** Number of Netscan objects created */
    private static int numInstances = 0;

    /**
     * Whether the two most signifcant bits of the instruction register encode
     * <code>readEnable</code> and <code>writeEnable</code>.
     */
    public static final boolean newInstructionRegister = true;

    /**
     * Shift data in chain.inBits into the selected scan chain on the chip. The
     * previous data on the chip is shifted out into chain.outBits.
     * 
     * @param chain
     *            Root scan chain to shift data into
     * @param readEnable
     *            whether to set opcode's read-enable bit
     * @param writeEnable
     *            whether to set opcode's write-enable bit
     * @param irBadSeverity
     *            action when bits scanned out of IR are wrong
     * @see Infrastructure#SEVERITY_NOMESSAGE
     * @see Infrastructure#SEVERITY_WARNING
     * @see Infrastructure#SEVERITY_NONFATAL
     * @see Infrastructure#SEVERITY_FATAL
     */
    void shift(ChainNode chain, boolean readEnable, boolean writeEnable,
            int irBadSeverity) {

        // Tell JTAG controller what scan chain, and what I/O privileges
        int result = netScan_IR(chain, readEnable, writeEnable, irBadSeverity);

        // Shift chain.inBits into chip, and old chip scan chain info
        // into chain.outBits
        if (result == 0)
            netScan_DR(chain);

        if (isBypassChain(chain) && !chain.getOutBits().isEmpty()) {
            Infrastructure.fatal("Bypass register returned non-zero value");
        }
    }

    /**
     * Write the bits <code>scanIn</code> to the JTAG controller's instruction
     * register. The first bit scanned in to the chip is the LSB of
     * <code>scanIn[0]</code>, and the first bit scanned out from the chip is
     * the LSB of <code>scanOut[0]</code>.
     * 
     * @param numBits
     *            The number of bits to shift
     * @param scanIn
     *            Bit sequence to write to instruction register
     * @param scanOut
     *            Bits scanned out of instruction register
     * @param drBits
     *            Number of bits in the selected chain
     * @return 0x00 (success), 0x11 (transmit error), 0x33 (receive error)
     */
    protected abstract int hw_net_scan_ir(int numBits, short[] scanIn, short[] scanOut, int drBits);

    /**
     * Write the bits <code>scanIn</code> to the JTAG controller's data
     * register, and read back the bits <code>scanOut</code>. The first bit
     * scanned in to the chip is the LSB of <code>scanIn[0]</code>, and the
     * first bit scanned out from the chip is the LSB of <code>scanOut[0]</code>.
     * <p>
     * Extracted from netScan_DR to simplify overriding for different hardware,
     * e.g., in class <code>Netscan4</code>.
     * 
     * @param numBits
     *            The number of bits to shift
     * @param scanIn
     *            Bit sequence to write to data register
     * @param scanOut
     *            Bits scanned out of data register
     * @return 0x00 (success), 0x11 (transmit error), 0x33 (receive error)
     */
    protected abstract int hw_net_scan_dr(int numBits, short[] scanIn,
            short[] scanOut);

    /**
     * Should be called by any Netscan subclass constructors. Only one
     * {@link Netscan}&nbsp;or {@link Netscan4}&nbsp;instance is allowed,
     * because the Netscan library only supports one JTAG controller at a time.
     */
    protected static void incrementNumTesters() {
        numInstances++;
        if (numInstances > 1) {
            Infrastructure.fatal("The Netscan SFL libraries can only be used"
                    + " with one Corelis JTAG tester at a time.  Thus only"
                    + " one NetscanGeneric (i.e., Netscan or Netscan4) object"
                    + " is allowed in a given JVM.");
        }
    }

    /**
     * Debugging method prints array of shorts in reverse order. This way the
     * sequence of bits scanned in or out can be read from right to left.
     * <p>
     * In other words, the left-to-right order of the bits is the same as in
     * <code>BitVector</code> and in the XML file--but the user may need to
     * ignore some initial zero bits that do not correspond to any chain
     * elements.
     */
    protected static String shortsToString(short[] shorts) {
        StringBuffer buffer = new StringBuffer();

        for (int iShort = shorts.length - 1; iShort >= 0; iShort--) {
            for (int iBit = 15; iBit >= 0; iBit--) {
                int bitValue = (shorts[iShort] >> iBit) & 1;
                buffer.append(Integer.toString(bitValue));
            }
            buffer.append(" ");
        }
        return buffer.toString();
    }

    /**
     * Opcode for bypass register is all 1's, except for the two high-order
     * (read and write enable) bits
     */
    private static boolean isBypassChain(ChainNode chain) {
        int numEnableBits = (newInstructionRegister ? 2 : 0);

        StringBuffer bypassCode = new StringBuffer();
        for (int i=0; i<(chain.getParentChip().getLengthIR() - numEnableBits); i++) {
            bypassCode.append('1');
        }
        for (int i=0; i<numEnableBits; i++) {
            bypassCode.append('0');
        }
        String opcode = padOpcode(chain.getOpcode(), chain.getParentChip().getLengthIR(), false, false);
        return opcode.equals(bypassCode.toString());
    }

    /**
     * Write the appropriate opcode to the JTAG controller's instruction
     * register. The opcode is in constructed of the address of the scan chain
     * to be accessed in little endian bit order, with the two high-order bits
     * set according to the values of readEnable and writeEnable.
     * 
     * @param chain
     *            Root scan chain to address
     * @param readEnable
     *            whether to set opcode's read-enable bit
     * @param writeEnable
     *            whether to set opcode's write-enable bit
     * @param irBadSeverity
     *            action when bits scanned out of IR are wrong
     * @return 0x00: success; 0x11: error on command header transmit; 0x33:
     *         error on receiving reply header
     */
    private int netScan_IR(ChainNode chain, boolean readEnable,
            boolean writeEnable, int irBadSeverity) {

        // Construct, and optionally report, the bit sequence to write
        String irbits = getInstructionRegister(chain, readEnable, writeEnable);
        short[] scanIn = instructionRegisterToShorts(irbits);

        // Form array of shorts to contain data returned from JTAG controller.
        // This data is ignored.
        int numShorts = irbits.length() / 16 + 1;
        short[] scanOut = new short[numShorts];

        MyTreeNode root = chain.getParentChip().getParent();
        int numPrebits = 0;
        int numPostbits = 0;
        boolean foundChain = false;
        for (int i=0; i<root.getChildCount(); i++) {
            MyTreeNode child = root.getChildAt(i);
            if (child instanceof ChipNode) {
                ChipNode chip = (ChipNode)child;
                if (chip == chain.getParentChip()) {
                    foundChain = true; continue;
                }
                if (foundChain)
                    numPostbits++;
                else
                    numPrebits++;
            }
        }

        // Scan bits scanIn into the instruction register, and bits scanOut out
        int result = hw_net_scan_ir(irbits.length(), scanIn, scanOut,
                                    chain.getInBits().getNumBits()+numPrebits+numPostbits);

        if (result != 0) {
            Infrastructure.fatal("net_scan_ir returned error code 0x"
                    + Integer.toHexString(result));
        } else {
            String outirbits = shortsToString(scanOut);
            outirbits = outirbits.replaceAll("\\s", "");
            outirbits = outirbits.substring(outirbits.length() - irbits.length());

            boolean badValue = false;
            char zero = '0';
            char one = '1';
            if (isScanOutInverted()) {
                zero = '1';
                one = '0';
            }

            root = chain.getParentChip().getParent();
            int ind = 0;
            for (int i=0; i<root.getChildCount(); i++) {
                MyTreeNode child = root.getChildAt(i);
                if (child instanceof ChipNode) {
                    ChipNode chip = (ChipNode)child;
                    int j=0;
                    for (j=0; j<chip.getLengthIR()-1; j++) {
                        if (outirbits.charAt(ind+j) != zero)
                            badValue = true;
                    }
                    if (outirbits.charAt(ind+j) != one)
                        badValue = true;
                    ind += chip.getLengthIR();
                }
            }
            if (badValue) {
                Infrastructure.error(irBadSeverity, "Bad IR scan out "
                        + shortsToString(scanOut) + ", expected 1. IR scan in was "+irbits+". Possible "
                        + "causes: bad or too-long JTAG cable, "
                        + "bad jtagVolts or jtagKhz values in "
                        + "ChainControl constructor, bad Vdd to chip, "
                        + "broken JTAG controller or JTAG tester, "
                        + "ill-tempered gremlins.");
            }
        }

        return result;
    }

    /**
     * Get the instruction register to be shifted in to access the given chain.
     * The IR is the concatenation of all jtag controller's IRs in the system.
     * Only the portion for the given chain will have a valid opcode,
     * all others will be in bypass mode.
     *
     * @param chain
     *            The chain to be shifted
     * @param readEnable
     *            whether to set opcode's read-enable bit
     * @param writeEnable
     *            whether to set opcode's write-enable bit
     * @return the bits to be written to the instruction register
     */
    static String getInstructionRegister(ChainNode chain, boolean readEnable, boolean writeEnable) {
        // get opcodes for all controllers. There is one controller per "chip".
        MyTreeNode root = chain.getParentChip().getParent();
        StringBuffer ir = new StringBuffer();

        // concatenate instruction registers commands for all controllers.
        // the target controller uses the chain's IR, other controllers are
        // set to bypass mode
        for (int i=0; i<root.getChildCount(); i++) {
            MyTreeNode child = root.getChildAt(i);
            if (child instanceof ChipNode) {
                ChipNode chip = (ChipNode)child;

                if (chip == chain.getParentChip()) {
                    ir.append(padOpcode(chain.getOpcode(), chip.getLengthIR(), readEnable, writeEnable));
                } else {
                    // bypass IR is all 1's
                    for (int j=0; j<chip.getLengthIR(); j++) {
                        ir.append('1');
                    }
                }
            }
        }
        return ir.toString();
    }

    /**
     * Add leading zeros to the opcode if it is shorter than the instruction
     * register length. This also overrides the
     * two high-order bits according to the readEnable and writeEnable call
     * parameters.
     *
     * @param opcode
     *            scan chain address (e.g., "010010")
     * @param lengthIR
     *            length of the instruction register
     * @param readEnable
     *            whether to set opcode's read-enable bit
     * @param writeEnable
     *            whether to set opcode's write-enable bit
     * @return the opcode with any leading zeros added
     */
    private static String padOpcode(String opcode, int lengthIR,
                                    boolean readEnable, boolean writeEnable) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<(lengthIR-opcode.length()); i++) {
            buf.append("0");
        }
        buf.append(opcode);
        if (newInstructionRegister) {
            buf.setCharAt(1, writeEnable ? '1' : '0');
            buf.setCharAt(0, readEnable  ? '1' : '0');
        }
        return buf.toString();
    }

    /**
     * Convert the character string (e.g., "010010") containing the address of
     * the scan chain to an array of shorts for writing to the IR. Note the
     * string is already in the desired little endian bit order.
     *
     * @param instructionRegister
     *            string containing instruction register(s) bits
     * @return short array to be written to the instruction register
     */
    static short[] instructionRegisterToShorts(String instructionRegister) {
        return opcodeToShorts(instructionRegister, instructionRegister.length());
    }

    /**
     * Convert opcode string to a short array, with last character in the string
     * being assigned to the low-order bit of the first entry in the array.
     * I.e., string is little-endian, so we cannot use BitVector string
     * routines.
     * 
     * @param opcode
     *            scan chain address (e.g., "010010")
     * @return short array suitable for writing to instruction register
     */
    private static short[] opcodeToShorts(String opcode, int lengthIR) {
        int numBits = opcode.length();
        if (numBits <= 0 || numBits > lengthIR) {
            Infrastructure.fatal("opcode.length()=" + numBits
                    + " is outside allowed range 1.." + lengthIR);
        }
        short[] shorts = new short[(numBits - 1) / 16 + 1];

        for (int ind = 0; ind < opcode.length(); ind++) {
            char ch = opcode.charAt(opcode.length() - ind - 1);
            if (ch == '1') {
                shorts[ind / 16] |= 1 << (ind % 16);
            } else if (ch != '0') {
                Infrastructure.fatal("Bad character " + ch
                        + " in bit string, only 0 and 1 allowed");
            }
        }
        return shorts;
    }

    /**
     * Shift data in chain.inBits into the selected scan chain on the chip. The
     * previous data on the chip is shifted out into chain.outBits. WARNING:
     * Must call netScan_IR() for the same <code>ChainNode</code> before
     * calling this routine, in order to set the on-chip scan chain address.
     * 
     * @param chain
     *            Root scan chain to shift data into
     * @return 0x00: success; 0x11: error on command header transmit; 0x33:
     *         error on receiving reply header
     */
    private int netScan_DR(ChainNode chain) {

        // Add an extra bit for any jtag controller in bypass mode
        MyTreeNode root = chain.getParentChip().getParent();

        int numPrebits = 0;
        int numPostbits = 0;
        boolean foundChain = false;
        for (int i=0; i<root.getChildCount(); i++) {
            MyTreeNode child = root.getChildAt(i);
            if (child instanceof ChipNode) {
                ChipNode chip = (ChipNode)child;
                if (chip == chain.getParentChip()) {
                    foundChain = true; continue;
                }
                if (foundChain)
                    numPostbits++;
                else
                    numPrebits++;
            }
        }

        // Construct, and optionally report, the bit sequence to write
        BitVector scanInBits = padBitVector(chain.getInBits(), numPrebits, numPostbits);
        short[] scanIn = bitsToDataRegister(scanInBits);

        // Form array of shorts to contain data returned from JTAG controller.
        int numShorts = scanInBits.getNumBits() / 16 + 1;
        short[] scanOut = new short[numShorts];

        // Shift bits scanIn into the data register and bits scanOut out
        int result = hw_net_scan_dr(scanInBits.getNumBits(), scanIn, scanOut);
        if (result != 0) {
            Infrastructure.fatal("net_scan_dr returned error code 0x"
                    + Integer.toHexString(result));
        }

        // Convert scanOut shorts to BitVector, then copy into
        // chain.outBits
        BitVector outBits = dataRegisterToBits(scanOut, scanInBits.getNumBits());
        if (isScanOutInverted()) {
            outBits.flip(0, outBits.getNumBits());
        }
        outBits = stripBitVector(outBits, numPrebits, numPostbits);
        chain.setOutBits(outBits);

        return result;
    }

    /**
     * Set or clear bit at index index in short array according to value. Short
     * array assumed little endian
     */
    private static void setShortsBit(short[] shorts, int index, boolean value) {
        if (value) {
            shorts[index / 16] |= (1 << (index % 16));
        } else {
            shorts[index / 16] &= ~(1 << (index % 16));
        }
    }

    /**
     * Pad a bit vector with a number of 0 bits at the beginning and end
     * of the vector.  Post bits are placed after the LAST element of the bit vector,
     * Pre bits are placed before the FIRST element.
     * @param bits
     *            the original bit vector
     * @param numPre
     *            the number of 0 bits to prepend
     * @param numPost
     *            the number of 0 bits to postpend
     * @return a new bit vector object with beginning and end padding
     */
    public static BitVector padBitVector(BitVector bits, int numPre, int numPost) {
        int numNewBits = numPre + numPost + bits.getNumBits();
        BitVector newBits = new BitVector(numNewBits, bits.getName());
        for (int j=0; j<numNewBits; j++)
            newBits.clear(j);

        newBits.put(numPre, bits);
        return newBits;
    }

    /**
     * Strip bits from the start and end of the bit vector.
     * @param bits
     *            the original bit vector
     * @param numPre
     *            the number of 0 bits to prepend
     * @param numPost
     *            the number of 0 bits to postpend
     * @return a new bit vector object with beginning and end bits stripped
     */
    private static BitVector stripBitVector(BitVector bits, int numPre, int numPost) {
        int numBits = bits.getNumBits() - numPre - numPost;
        return bits.get(numPre, numBits);
    }

    /**
     * Convert a big-endian bit vector into a little endian short array. The
     * LAST element of the bit vector is put into the low-order bit of the FIRST
     * element of the short array.
     * 
     * @param bits
     *            input bit vector
     * @return converted short array
     */
    private static short[] bitsToDataRegister(BitVector bits) {
        int numBits = bits.getNumBits();
        if (numBits < 0) {
            Infrastructure.fatal("bad numBits = " + numBits);
        }
        short[] shorts = new short[(numBits - 1) / 16 + 1];

        for (int ind = 0; ind < numBits; ind++) {
            if (bits.get(numBits - ind - 1)) {
                shorts[ind / 16] |= 1 << (ind % 16);
            }
        }
        return shorts;
    }

    /**
     * Convert a little endian short array to a big endian bit vector of length
     * numBits. The LSB of the short array's first member is stored in the LAST
     * element of the bit vector.
     * 
     * @param shorts
     *            input short array
     * @param numBits
     *            length of the bit vector, must not exceed 16*shorts.length
     * @return bit vector corresponding to the shorts
     */
    private static BitVector dataRegisterToBits(short[] shorts, int numBits) {
        if (numBits < 0 || numBits > (shorts.length * 16)) {
            Infrastructure.fatal("BAD NUMBITS = " + numBits);
        }
        BitVector bits = new BitVector(numBits, "dataRegisterToBits.bits");

        for (int ind = 0; ind < numBits; ind++) {
            boolean value = (shorts[ind / 16] & 1 << (ind % 16)) != 0;
            bits.set(numBits - ind - 1, value);
        }
        return bits;
    }

    /** Unit test. */
    public static void main(String args[]) {
        short[] inp = new short[] { 0x03, 0xac, (short) 0xff00 };
        BitVector bits = new BitVector(0, "main().bits");

        // Test dataRegisterToBits()
        System.out.print("Shorts in (descending): " + shortsToString(inp));
        for (int numBits = 0; numBits <= inp.length * 16; numBits += 4) {
            bits = NetscanGeneric.dataRegisterToBits(inp, numBits);
            System.out.println(numBits + ":" + bits);
        }

        // Test bitsToDataRegister()
        System.out.println("BitVector in: " + bits);
        short[] out = NetscanGeneric.bitsToDataRegister(bits);
        System.out.print("DataRegister:" + shortsToString(out));

        // Test opcodeToInstructionRegister()
        String opcode;
        if (args.length > 0) {
            opcode = args[0];
        } else {
            opcode = "1011";
        }
        out = NetscanGeneric.instructionRegisterToShorts(padOpcode(opcode, 8, true, true));
        System.out.println("IR: " + shortsToString(out));
        out = NetscanGeneric.instructionRegisterToShorts(padOpcode(opcode, 8, false, false));
        System.out.println("IR: " + shortsToString(out));
    }
}

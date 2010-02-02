/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BitVector.java
 * Written by Eric Kim and JTom O'Neill, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import java.math.BigInteger;
import java.util.BitSet;

/**
 * Provides a fixed-length bit vector and methods for acting on it. Developed to
 * represent scan chain state variables.
 * <p>
 * Bits can be in the <tt>invalid</tt>, &nbsp; <tt>true</tt>, or
 * <tt>false</tt> state. The string representations for these values are "
 * <tt>-</tt>", "<tt>1</tt>", and "<tt>0</tt>", respectively. To help
 * catch bugs, bits are <tt>invalid</tt> (i.e., undefined or unknown) until
 * they have been explicitly set and <code>BitVector</code> methods crash if
 * you attempt to read an <tt>invalid</tt> bit. Here are some examples to
 * indicate how BitVector values are represented and used:
 * <p>
 * <TABLE border="1">
 * <TR>
 * <TH ROWSPAN=2>Bit representation
 * <TH COLSPAN=3>BitVector example
 * <TR>
 * <TH>example 1
 * <TH>example 2
 * <TH>example 3
 * <TR>
 * <TH>String
 * <TD>011
 * <TD>1-01
 * <TD>1000000000
 * <TR>
 * <TH>BigInteger
 * <TD>3
 * <TD>undefined
 * <TD>512
 * <TR>
 * <TH>"LittleInteger"
 * <TD>6
 * <TD>undefined
 * <TD>1
 * <TR>
 * <TH>BitVector state at index 0,1,2,...
 * <TD>false,true,true
 * <TD>true,invalid,false,true
 * <TD>true,false,false,...
 * <TR>
 * <TH>bits scanned into scan chain
 * <TD>1,1,0
 * <TD>undefined
 * <TD>0,0,0,0,0,0,0,0,0,1
 * <TR>
 * <TH>chain element states along s_in
 * <TD>0,1,1
 * <TD>undefined
 * <TD>1,0,0,0,0,0,0,0,0,0 </TABLE>
 * <p>
 * Bit zero is the LAST bit scanned into the scan chain. Thus the bit index
 * matches the position of the corresponding scan chain element along the s_in
 * chain. <code>BitVector</code> uses the big endian bit order convention for
 * the String representation. Strings should be read from left to right,
 * starting at bit zero. Thus the strings match the order in which scan chain
 * elements appear in most schematics. The MSB of a <code>BigInteger</code> or
 * <code>int</code> is stored in bit 0 of the <code>BitVector</code>. That
 * is, the bit order is opposite to that of the integer.
 * <p>
 * Key differences from <code>BitSet</code>: 1) addition of the <tt>invalid
 * </tt>
 * state; 2) the bit vector is fixed length; 3) ranges are specified by
 * (startIndex, numBits) rather than (startIndex, endIndex); 4) different
 * toString() method; addition of BigInteger I/O methods.
 * <p>
 */

public class BitVector {

    /** Number of bits in the bit vector */
    final private int numBits;

    /** Bit vector */
    private BitSet bitSet;

    /** Whether state of each bit is accurately known */
    private BitSet valid;

    /** Identifying name of bit vector, for errors etc. */
    private String name;

    /**
     * Severity of action when an unnamed BitVector is created. Default is
     * {@link Infrastructure#SEVERITY_WARNING}
     */
    public static int noNameSeverity = Infrastructure.SEVERITY_WARNING;

    /**
     * Deprecated. Constructor creates an unnamed vector of <code>numBits</code>
     * bits, set to the <tt>invalid</tt> state.
     * 
     * @param numBits
     *            number of bits in the vector
     * @deprecated
     */
    public BitVector(int numBits) {
        this(numBits, "unnamed");
        Infrastructure.error(noNameSeverity,
                "Warning: creating unnamed length-" + numBits
                        + " bit vector, use "
                        + "two-parameter BitVector constructor instead");
    }

    /**
     * Main constructor creates a vector of <code>numBits</code> bits, set to
     * the <tt>invalid</tt> state.
     * 
     * @param numBits
     *            number of bits in the vector
     * @param name
     *            identifying name of the bit vector, e.g. for errors
     */
    public BitVector(int numBits, String name) {
        if (numBits < 0) {
            Infrastructure.fatal("Bad BitVector length " + numBits
                    + ", must be non-negative");
        }
        this.numBits = numBits;
        this.bitSet = new BitSet(numBits);
        this.valid = new BitSet(numBits);
        this.name = name;
    }

    /**
     * Deprecated. Convenience constructor creates a bit vector from the input
     * string. The length of the string is used for the length of the bit
     * vector.
     * 
     * @param bitString
     *            bit sequence to initialize vector to
     * @deprecated
     */
    public BitVector(String bitString) {
        this(bitString.length());
        this.put(0, bitString);
    }

    /**
     * Convenience constructor creates a bit vector from the input string. The
     * length of the string is used for the length of the bit vector.
     * 
     * @param bitString
     *            bit sequence to initialize vector to
     * @param name
     *            identifying name of the bit vector, e.g. for errors
     */
    public BitVector(String bitString, String name) {
        this(bitString.length(), name);
        this.put(0, bitString);
    }
    /**
     * Convenience constructor copies contents of existing BitVector
     * 
     *  @param b BitVector to copy
     */
    public BitVector(BitVector b) {
    	this(b.getState(), b.getName());
    }

    /**
     * Convenience constructor creates a bit vector from the input string. The
     * length of the string is used for the length of the bit vector.
     *
     * @param bitArray
     *            bit sequence to initialize vector to
     * @param name
     *            identifying name of the bit vector, e.g. for errors
     */
    public BitVector(int[] bitArray, String name) {
        this(bitArray.length, name);
        this.put(0, bitArray);
    }

    /**
     * Returns number of bits in the bit vector
     * 
     * @return Number of bits in the bit vector
     */
    public int getNumBits() {
        return numBits;
    }

    /**
     * Returns the name of the bit vector.
     * 
     * @return Returns the name of the bit vector.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the bit vector.
     * 
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns a string representation of the entire bit sequence, starting with
     * the lowest-index bit.
     * 
     * @return string representation of bit sequence
     */
    public String getState() {
        StringBuffer buf = new StringBuffer(numBits);
        for (int ind = 0; ind < numBits; ind++) {
            if (this.valid.get(ind) == false) {
                buf.append("-");
            } else if (this.get(ind)) {
                buf.append("1");
            } else {
                buf.append("0");
            }
        }
        return buf.toString();
    }

    /**
     * Returns the name and the bit vector and a string representing the entire
     * bit sequence, starting with the lowest-index bit.
     * 
     * @return string representation of bit sequence
     */
    public String toString() {
        return getName() + ": " + getState();
    }

    /**
     * Returns a <code>BigInteger</code> representation of the entire bit
     * vector, with bit 0 in the MSB.
     * 
     * @return <code>BigInteger</code> representation of bit sequence
     */
    public BigInteger toBigInteger() {
        checkRange(0, numBits, true);
        BigInteger out = BigInteger.ZERO;

        for (int ind = 0; ind < numBits; ind++) {
            if (this.get(ind) == true) {
                out = out.or(BigInteger.ONE.shiftLeft(numBits - ind - 1));
            }
        }

        return out;
    }

    /**
     * Returns a <code>BigInteger</code> representation of the entire bit
     * vector, with bit 0 in the LSB. Compare to toBigInteger().
     * 
     * @return <code>BigInteger</code> representation of bit sequence
     * @see #toBigInteger
     */
    public BigInteger toLittleInteger() {
        checkRange(0, numBits, true);
        BigInteger out = BigInteger.ZERO;

        for (int ind = 0; ind < numBits; ind++) {
            if (this.get(ind) == true) {
                out = out.or(BigInteger.ONE.shiftLeft(ind));
            }
        }

        return out;
    }

    /**
     * Returns an <code>int</code> array representation of the entire bit
     * vector. Values in the array are either 0, 1, or -1 for false, true, and
     * undefined, respectively.
     * @return an integer array representation of the bit vector.
     */
    public int[] toIntArray() {
        int [] array = new int[numBits];
        for (int ind = 0; ind < numBits; ind++) {
            if (this.valid.get(ind) == false) {
                array[ind] = -1;
            } else if (this.get(ind)) {
                array[ind] = 1;
            } else {
                array[ind] = 0;
            }
        }
        return array;
    }

    /**
     * Returns <tt>true</tt> if specified bit has a known state.
     * 
     * @param bitIndex
     *            index of bit to be checked
     * @return whether bit has known state
     */
    public boolean isValid(int bitIndex) {
        checkIndex(bitIndex, false);
        return valid.get(bitIndex);
    }

    /**
     * Sets the bit specified by the index to the <tt>invalid</tt> state.
     * 
     * @param bitIndex
     *            the index of the bit to be invalidated
     */
    public void invalidate(int bitIndex) {
        checkIndex(bitIndex, false);
        valid.clear(bitIndex);
    }

    /**
     * Sets the entire bit vector to the <tt>invalid</tt> state.
     */
    public void invalidate() {
        for (int ind = 0; ind < numBits; ind++) {
            invalidate(ind);
        }
    }

    /**
     * Returns the value of the bit with the specified index. The value is
     * <tt>true</tt> if the bit with the index <code>bitIndex</code> is
     * currently set in this <code>BitVector</code>; otherwise, the result is
     * <tt>false</tt>.
     * 
     * @param bitIndex
     *            the bit index
     * @return the value of the bit with the specified index
     */
    public boolean get(int bitIndex) {
        checkIndex(bitIndex, true);
        return bitSet.get(bitIndex);
    }

    /**
     * Sets the bit at the specified index to to the complement of its current
     * value.
     * 
     * @param bitIndex
     *            the index of the bit to flip
     */
    public void flip(int bitIndex) {
        checkIndex(bitIndex, true);
        bitSet.flip(bitIndex);
    }

    /**
     * Sets the bits in the specified index range to to the complements of their
     * current values.
     * 
     * @param fromIndex
     *            the starting index of the bits to flip
     * @param nbits
     *            the number of bits to flip
     */
    public void flip(int fromIndex, int nbits) {
        checkRange(fromIndex, nbits, true);
        for (int bitIndex = fromIndex; bitIndex < fromIndex + nbits; bitIndex++) {
            this.flip(bitIndex);
        }
    }

    /**
     * Sets the bit specified by the index to <tt>false</tt>.
     * 
     * @param bitIndex
     *            the index of the bit to be cleared
     */
    public void clear(int bitIndex) {
        checkIndex(bitIndex, false);
        bitSet.clear(bitIndex);
        valid.set(bitIndex);
    }

    /**
     * Sets the bit at the specified index to <tt>true</tt>.
     * 
     * @param bitIndex
     *            a bit index
     */
    public void set(int bitIndex) {
        checkIndex(bitIndex, false);
        bitSet.set(bitIndex);
        valid.set(bitIndex);
    }

    /**
     * Sets the bit at the specified index to the specified value.
     * 
     * @param bitIndex
     *            a bit index.
     * @param value
     *            a boolean value to set
     */
    public void set(int bitIndex, boolean value) {
        checkIndex(bitIndex, false);
        bitSet.set(bitIndex, value);
        valid.set(bitIndex);
    }

    /**
     * Sets the <code>nbits</code> bits starting at <code>fromIndex</code>
     * (inclusive) to the specified value.
     * 
     * @param fromIndex
     *            index of the first bit to be set
     * @param nbits
     *            number of bits to set
     * @param value
     *            value to set the selected bits to
     */
    public void set(int fromIndex, int nbits, boolean value) {
        checkRange(fromIndex, nbits, false);
        bitSet.set(fromIndex, fromIndex + nbits, value);
        valid.set(fromIndex, fromIndex + nbits);
    }

    /**
     * Returns a new <code>BitVector</code> composed of a subset of
     * <code>numBits</code> bits from this <code>BitVector</code>. Range is
     * from <code>fromIndex</code> (inclusive) to
     * <code>(toIndex + nbits)</code> (exclusive). Note this interface is
     * different from that of <code>BitSet.get()</code>. The range is not
     * allowed to include any invalid bits.
     * 
     * @param fromIndex
     *            index of the first bit to include
     * @param nbits
     *            number of bits to include
     * @return a new BitSet from a range of this BitVector
     */
    public BitVector get(int fromIndex, int nbits) {
        checkRange(fromIndex, nbits, true);
        return getIndiscriminate(fromIndex, nbits);
    }

    /**
     * Like {@link #get(int, int)}, but the range is allowed to include invalid
     * bits.
     * 
     * @param fromIndex
     *            index of the first bit to include
     * @param nbits
     *            number of bits to include
     * @return a new BitSet from a range of this BitVector
     */
    public BitVector getIndiscriminate(int fromIndex, int nbits) {
        checkRange(fromIndex, nbits, false);
        BitVector result = new BitVector(nbits, "bits [" + fromIndex + ":"
                + (fromIndex + nbits - 1) + "] of " + getName());
        result.bitSet = bitSet.get(fromIndex, fromIndex + nbits);
        result.valid = valid.get(fromIndex, fromIndex + nbits);
        return result;
    }

    /**
     * Copies the source bit vector into the receiver, starting at index
     * <code>fromIndex</code>. If the source bit vector does not extend to
     * the end of the receiver, the high-index bits will not be modified. The
     * source bit vector is required to contain no invalid bits.
     * 
     * @param fromIndex
     *            starting index
     * @param source
     *            source bits
     */
    public void put(int fromIndex, BitVector source) {
        int nbits = source.getNumBits();
        checkRange(fromIndex, nbits, false);
        source.checkRange(0, nbits, true);

        putIndiscriminate(fromIndex, source);
    }

    /**
     * Like {@link #put(int, BitVector)}, but the source bit vector is allowed
     * to have invalid bits.
     * 
     * @param fromIndex
     *            starting index
     * @param source
     *            source bits
     */
    public void putIndiscriminate(int fromIndex, BitVector source) {
        int nbits = source.getNumBits();
        checkRange(fromIndex, nbits, false);
        source.checkRange(0, nbits, false);

        for (int ind = 0; ind < nbits; ind++) {
            if (source.isValid(ind) == false) {
                invalidate(fromIndex + ind);
            } else if (source.get(ind)) {
                set(fromIndex + ind);
            } else {
                clear(fromIndex + ind);
            }
        }
    }

    /**
     * Sets bits in bit vector according to a big endian input
     * <code>String</code>, starting at bit index <code>fromIndex</code>.
     * If the string does not extend to the end of the bit vector, the
     * high-index bits will not be modified.
     * 
     * @param fromIndex
     *            index of the first bit to be set
     * @param inp
     *            new bit values (e.g., "<tt>10101</tt>")
     */
    public void put(int fromIndex, String inp) {
        int length = inp.length();
        if (length == 0) return;
        checkRange(fromIndex, length, false);

        for (int ind = 0; ind < length; ind++) {
            char character = inp.charAt(ind);
            if (character == '1') {
                set(fromIndex + ind);
            } else if (character == '0') {
                clear(fromIndex + ind);
            } else {
                Infrastructure.fatal("Bad character " + character
                        + " in bit string " + inp
                        + ", only '0' and '1' are allowed for put method.");
            }
        }
    }

    /**
     * Sets bits in bit vector according to an
     * <code>int[]</code>, starting at bit index <code>fromIndex</code>.
     * If the array does not extend to the end of the bit vector, the
     * high-index bits will not be modified. The array must
     * be an array of 1's and 0's (1 for true, 0 for false).
     *
     * @param fromIndex
     *            index of the first bit to be set
     * @param array
     *            new bit values
     */
    public void put(int fromIndex, int[] array) {
        int length = array.length;
        if (length == 0) return;
        checkRange(fromIndex, length, false);

        for (int ind = 0; ind < length; ind++) {
            int i = array[ind];
            if (i == 1) {
                set(fromIndex + ind);
            } else if (i == 0) {
                clear(fromIndex + ind);
            } else {
                Infrastructure.fatal("Bad value " + i
                        + " in int[] " + array.toString()
                        + ", only 0 and 1 are allowed for put method.");
            }
        }
    }


    /**
     * Sets <code>nbits</code> bits in bit vector according to the bit
     * sequence in a <code>BigInteger</code>, starting at bit index
     * <code>fromIndex</code>. The integer's bit sequence is reversed upon
     * storage.
     * 
     * @param fromIndex
     *            index of the first bit to be set
     * @param nbits
     *            number of bits to set
     * @param inp
     *            number containing new bit values in reverse order
     */
    public void put(int fromIndex, int nbits, BigInteger inp) {
        checkRange(fromIndex, nbits, false);

        for (int ind = 0; ind < nbits; ind++) {
            BigInteger bit = inp.shiftRight(nbits - ind - 1);
            bit = bit.and(BigInteger.ONE);

            if (bit.equals(BigInteger.ONE)) {
                set(fromIndex + ind);
            } else if (bit.equals(BigInteger.ZERO)) {
                clear(fromIndex + ind);
            } else {
                Infrastructure.fatal("Programming error: bad bit value " + bit
                        + " in BigInteger " + inp);
            }
        }
    }

    /**
     * Sets <code>nbits</code> bits in bit vector according to the bit
     * sequence in a <code>BigInteger</code>, starting at bit index
     * <code>fromIndex</code>.
     * 
     * @param fromIndex
     *            index of the first bit to be set
     * @param nbits
     *            number of bits to set
     * @param inp
     *            number containing new bit values in reverse order
     */
    public void putLittle(int fromIndex, int nbits, BigInteger inp) {
        checkRange(fromIndex, nbits, false);

        for (int ind = 0; ind < nbits; ind++) {
            BigInteger bit = inp.shiftRight(ind);
            bit = bit.and(BigInteger.ONE);

            if (bit.equals(BigInteger.ONE)) {
                set(fromIndex + ind);
            } else if (bit.equals(BigInteger.ZERO)) {
                clear(fromIndex + ind);
            } else {
                Infrastructure.fatal("Programming error: bad bit value " + bit
                        + " in BigInteger " + inp);
            }
        }
    }

    /**
     * Returns the number of bits set to <tt>true</tt> in this
     * <code>BitVector</code>.
     * 
     * @return the number of bits set to <tt>true</tt> in this
     *         <code>BitVector</code>.
     */
    public int cardinality() {
        int numGoodBits = valid.cardinality();
        if (numGoodBits < numBits) {
            Infrastructure.fatal("Only " + numGoodBits + " out of " + numBits
                    + " bits valid in BitVector '" + getName()
                    + "'.  Can only count cardinality of"
                    + " a fully-valid (i.e. initialized) BitVector.");
        }
        return bitSet.cardinality();
    }

    /**
     * Returns <tt>true</tt> if this <code>BitSet</code> contains only bits
     * that are <tt>false</tt>.
     * 
     * @return whether this <code>BitSet</code> is empty.
     */
    public boolean isEmpty() {
        checkRange(0, getNumBits(), true);
        return bitSet.isEmpty();
    }

    /**
     * Returns <tt>true</tt> if all of the bits in this <code>BitSet</code>
     * are in the <tt>invalid</tt> state.
     * 
     * @return whether this <code>BitSet</code> is completely invalid.
     */
    public boolean isInvalid() {
        checkRange(0, getNumBits(), false);
        return valid.isEmpty();
    }
    
    //-------------------------------------------------------------------------
    /** I'm adding a bunch of methods that will make BitVector convenient for
     * writing test code.  This code will behave as if BitVector is a big
     * endian integer. RKao
     */
    private void fatalIfAnyBitInvalid(String op) {
    	for (int i=0; i<getNumBits(); i++) {
    		if (!isValid(i)) {
    			Infrastructure.fatal(op+" operand contains invalid bits");
    		}
    	}
    }
    /** Compare BitVectors. number of bits must match. */
    public boolean equals(Object o) {
    	if (!(o instanceof BitVector)) return false;
    	String s1 = getState();
    	String s2 = ((BitVector) o).getState();
    	return s1.equals(s2);
    }
    
    public int hashCode() {return getState().hashCode();}
    
    /** Generate concatenation of bit vectors. Put bits from b on the right */
    public BitVector cat(BitVector b) {
    	String s = getState() + b.getState();
    	return new BitVector(s, "cat");
    }
    /** Generate the complement all bits */
    public BitVector not() {
    	BitVector r = new BitVector(this);
    	r.flip(0, r.getNumBits());
    	return r;
    }
    /**
     * Logical AND this BitVector with b. Pad the shorter
     * of the two operands with zeros on the left.
     * @param b second operand
     * @return a new BitVector that is this AND b
     */
    public BitVector and(BitVector b) {
    	BitVector a = this;
    	int lenA = a.getNumBits();
    	int lenB = b.getNumBits();
    	int l = Math.max(lenA, lenB);
    	BitVector ans = new BitVector(l, "and");
    	int aNdx = lenA-1;
    	int bNdx = lenB-1;
    	for (int i=l-1; i>=0; i--) {
    		if (!a.isValid(aNdx) || !b.isValid(bNdx)) {
    			ans.invalidate(i);
    		} else {
    			boolean av = a.get(aNdx);
    			boolean bv = b.get(bNdx);
    			boolean ansv = av && bv;
    			ans.set(i, ansv);
    		}
    		if (aNdx>0) aNdx--;
    		if (bNdx>0) bNdx--;
    	}
    	return ans;
    }
    /**
     * Return a new BitVector that is the bit reverse of the
     * bits in this BitVector. That is the MSB
     * moves to the LSB and all other bits are shifted to the left.
     * @return the new rotated BitVector
     */
    public BitVector bitReverse() {
    	int l = getNumBits();
    	BitVector ans = new BitVector(l, "bitReverse");
    	int j=l-1;
    	for (int i=0; i<l; i++, j--) {
    		if (!isValid(i)) continue;
    		ans.set(j, get(i));
    	}
    	return ans;
    }
    /** Set the value of BitVector from an long value.  Right justify. 
     * Sign extend if longValue is too short.  Truncate MSBs if longValue
     *  is too long */
    public void setFromLong(long longValue) {
    	for (int i=getNumBits()-1; i>=0; i--) {
    		set(i, (longValue & 1)==1);
    		longValue = longValue >> 1;
    	}
    }
    /** Set the value of BitVector from a BigInteger.  Right justify. 
     * Sign extend if bigValue is too short.  Truncate MSBs if bigValue
     *  is too large */
    public void setFromBigInteger(BigInteger bigValue) {
    	BigInteger ONE = BigInteger.ONE;
    	for (int i=getNumBits()-1; i>=0; i--) {
    		set(i, (bigValue.and(ONE)).equals(ONE));
    		bigValue = bigValue.shiftRight(1);
    	}
    }
    /** Sign extend the shorter operand to the length of the longer
     * longer operand.
     * Add the two numbers. Truncate the result to the length of the
     * longer operand. */
    public BitVector add(BitVector bv) {
    	fatalIfAnyBitInvalid("add");
    	bv.fatalIfAnyBitInvalid("add");
    	
    	int len = Math.max(getNumBits(), bv.getNumBits());
    	BitVector ans = new BitVector(len, "add");
    	BigInteger a = this.toBigInteger();
    	BigInteger b = bv.toBigInteger();
    	ans.setFromBigInteger(a.add(b));
    	return ans;
    }
    /** Sign extend the shorter operand to the length of the longer
     * longer operand.
     * Subtract (this - bv). Truncate the result to the length of the
     * longer operand. */
    public BitVector subtract(BitVector bv) {
    	fatalIfAnyBitInvalid("subtract");
    	bv.fatalIfAnyBitInvalid("subtract");
    	
    	int len = Math.max(getNumBits(), bv.getNumBits());
    	BitVector ans = new BitVector(len, "subtract");
    	BigInteger a = this.toBigInteger();
    	BigInteger b = bv.toBigInteger();
    	ans.setFromBigInteger(a.subtract(b));
    	return ans;
    }
    /** Shift right, filling with sign bit */
    public BitVector shiftRight(int n) {
    	fatalIfAnyBitInvalid("shiftRight");
    	int len = getNumBits();
    	BitVector ans = new BitVector(len, "shiftRight");
    	for (int i=0; i<len; i++) {
    		int j = Math.max(0, i-n);
    		if (!isValid(j)) continue;
    		ans.set(i, get(j));
    	}
    	return ans;
    }
    /** convert to long. Truncate MSBs if BitVector is too large */
    public long toLong() {
    	fatalIfAnyBitInvalid("toLong");
    	BigInteger big = toBigInteger();
    	return big.longValue();
    }
    /** sign extend shorter operand to length of longer operand.
     * Then test for equality */
    public boolean equalsLong(long v) {
    	fatalIfAnyBitInvalid("equalsLong");
    	int len = Math.max(64, getNumBits());
    	BitVector a = new BitVector(len, "equalsLong a");
    	BitVector b = new BitVector (len, "equalsLong b");
    	a.setFromBigInteger(toBigInteger());
    	b.setFromLong(v);
    	return a.equals(b);
    }
    
    /**
     * Return a new BitVector that consists of the
     * bits in this BitVector rotated left. That is the MSB
     * moves to the LSB and all other bits are shifted to the 
     * left.
     * @param amountToRotate number of bit positions to rotate 
     * @return the new rotated BitVector
     */
    public BitVector rotateLeft(int amountToRotate) {
    	int len = getNumBits();
    	BitVector ans = new BitVector(len, "rotateLeft");
    	for (int i=0; i<len; i++) {
    		int j = (i+len-amountToRotate) % len;
    		if (this.isValid(i)) {
    			ans.set(j, this.get(i));
    		}
    	}
    	return ans;
    }

    /**
     * Return a new BitVector that consists of the
     * bits in this BitVector rotated right. That is the LSB
     * moves to the MSB and all other bits are shifted to the 
     * right.
     * @param amountToRotate number of bit positions to rotate 
     * @return the new rotated BitVector
     */
    public BitVector rotateRight(int amountToRotate) {
    	int len = getNumBits();
    	BitVector ans = new BitVector(len, "rotateLeft");
    	for (int i=0; i<len; i++) {
    		int j = (i+len+amountToRotate) % len;
    		if (this.isValid(i)) {
    			ans.set(j, this.get(i));
    		}
    	}
    	return ans;
    }
    //-------------------------------------------------------------------------
    

    /**
     * Checks if <code>BitVector</code> includes a bit at index
     * <code>bitIndex</code> and, optionally, if the bit has a known state. If
     * not, prints an error message and exits the JVM.
     * 
     * @param bitIndex
     *            bit index
     * @param checkValidity
     *            whether to insist bit is valid
     */
    private void checkIndex(int bitIndex, boolean checkValidity) {
        if (bitIndex < 0 || bitIndex > numBits) {
            Infrastructure.fatal("Bit index " + bitIndex
                    + " outside allowed range of 0.." + numBits
                    + " in BitVector " + toString());
        }
        if (checkValidity && this.valid.get(bitIndex) == false) {
            printInvalidError(bitIndex);
        }
    }

    /**
     * @param bitIndex
     *            index of invalid bit being accessed
     */
    private void printInvalidError(int bitIndex) {
        Infrastructure.fatal("Attempt to access the bit at position "
                + bitIndex + " in BitVector '" + getName()
                + "', which is in the 'invalid' state.  "
                + "Bits must be explicitly set before being read.  "
                + "This error probably indicates incorrect "
                + "initialization of the BitVector.  "
                + "The BitVector state is " + getState());
    }

    /**
     * Checks if bit range is consistent with this <code>BitVector</code>. If
     * not, prints an error message and exits the JVM.
     * 
     * @param fromIndex
     *            starting index
     * @param nbits
     *            number of bits in range
     * @param checkValidity
     *            whether to insist bit is valid
     */
    private void checkRange(int fromIndex, int nbits, boolean checkValidity) {
        if (nbits <= 0) {
            Infrastructure.fatal("Attempt to read " + nbits
                    + " bits, number must be positive");
        }
        if ((fromIndex + nbits) > numBits) {
            Infrastructure
                    .fatal("Attempt to read past the end" + " of BitVector '"
                            + getName() + "', which has length " + numBits
                            + ": fromIndex=" + fromIndex + ", nbits=" + nbits);
        }
        if (checkValidity) {
            for (int ind = fromIndex; ind < fromIndex + nbits; ind++) {
                if (this.valid.get(ind) == false) {
                    printInvalidError(ind);
                }
            }
        }
    }

    /**
     * Unit test, sets and prints some bit vectors. If args[0] is present, it is
     * the string (e.g., "01110") to set <code>BitVector b1</code> to. If
     * args[1] is prsent, it is the string to set <code>BitVector b2</code>
     * to. Otherwise default values of "1101" and "1001" are used.
     */
    public static void main(String[] args) {
        String b2String;

        // Test put(string), toString().
        BitVector b1 = new BitVector(7, "b1");
        if (args.length > 0) {
            b1.put(0, args[0]);
        } else {
            b1.put(0, "1101");
        }
        System.out.print(b1 + ": bitSet=" + b1.bitSet);
        System.out.println(", length=" + b1.numBits + ", size="
                + b1.bitSet.size());

        // Test get(), set(), clear()
        System.out.println("3 bits starting at 1: " + b1.get(1, 3));
        b1.set(2);
        b1.clear(1);
        System.out.println("After setting 2, clearing 1: " + b1);

        // Test put()
        if (args.length > 1) {
            b2String = args[1];
        } else {
            b2String = "1001";
        }
        BitVector b2 = new BitVector(b2String.length(), "b2");
        b2.put(0, b2String);
        b1.put(2, b2);
        System.out.println("inserted " + b2 + " at position 2: " + b1);
        b1.put(2, "010");
        System.out.println("inserted 3 bits 010 at position 2: " + b1);

        BitVector b3 = new BitVector(40, "b3");
        BigInteger biggy = new BigInteger("3");
        b3.set(0, b3.getNumBits(), false);
        b3.put(0, 2, biggy);
        System.out.println("biggy = " + biggy + ", b3 = " + b3.getState()
                + ", biggy2 = " + b3.toBigInteger() + " ("
                + b3.toLittleInteger() + ")");
        biggy = new BigInteger("536870912");
        b3.put(3, 30, biggy);
        System.out.println("biggy = " + biggy + ", b3 = " + b3.getState()
                + ", biggy2 = " + b3.toBigInteger() + " ("
                + b3.toLittleInteger() + ")");

        BitVector b4 = new BitVector(10, "b4");
        biggy = new BigInteger("512");
        b4.put(0, 10, biggy);
        System.out.println("biggy = " + biggy + ", b4 = " + b4.getState()
                + ", biggy2 = " + b4.toBigInteger() + " ("
                + b4.toLittleInteger() + ")");

        biggy = new BigInteger("11");
        b4.putLittle(2, 5, biggy);
        System.out.println("biggy = " + biggy + ", b4 = " + b4.getState()
                + ", biggy2 = " + b4.toBigInteger() + " ("
                + b4.toLittleInteger() + ")");

        System.out.println("Convenience constructor: "
                + new BitVector("101", "convenience"));
    }

}

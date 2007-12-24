/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BigBinary.java
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.interval;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Immutable, arbitrary-precision signed floating numbers.  A
 * <tt>BigBinary</tt> consists of an arbitrary precision integer
 * <i>unscaled value</i> and a 32-bit integer <i>scale</i>.  If zero
 * or positive, the scale is the number of binary digits to the right of the
 * decimal point.  If negative, the unscaled value of the number is
 * multiplied by two to the power of the negation of the scale.  The
 * value of the number represented by the <tt>BigBinary</tt> is
 * therefore <tt>(unscaledValue &times; 2<sup>-scale</sup>)</tt>.
 * 
 * @see     BigInteger
 * @author  Dmitry Nadezhin
 */
public class BigBinary  extends Number implements Comparable<BigBinary> {
    /**
     * The unscaled value of this BigDecimal, as returned by {@link
     * #unscaledValue}.
     *
     * @serial
     * @see #unscaledValue
     */
    private volatile BigInteger intVal;

    /**
     * The scale of this BigDecimal, as returned by {@link #scale}.
     *
     * @serial
     * @see #scale
     */
    private int scale = 0;  // Note: this may have any value, so
                            // calculations must be done in longs

    // Constants
    /**
     * The value 0, with a scale of 0.
     */
    public static final BigBinary ZERO = new BigBinary(BigInteger.ZERO, 0);

    /**
     * The value 1, with a scale of 0.
     */
    public static final BigBinary ONE = new BigBinary(BigInteger.ONE, 0);

    /**
     * The value 2, with a scale of 0.
     */
    public static final BigBinary TWO = new BigBinary(BigInteger.valueOf(2), 0);

    /**
     * The value 10, with a scale of 0.
     */
    public static final BigBinary TEN = new BigBinary(BigInteger.TEN, 0);

    /**
     * Translates a <tt>double</tt> into a <tt>BigBinary</tt> which
     * is the exact decimal representation of the <tt>double</tt>'s
     * binary floating-point value.  The scale of the returned
     * <tt>BigBinary</tt> is the smallest value such that
     * <tt>(2<sup>scale</sup> &times; val)</tt> is an integer.
     * <p>
     * <b>Notes:</b
     * <ol>
     * <li>
     * The results of this constructor can be somewhat unpredictable.
     * One might assume that writing <tt>new BigBinary(0.1)</tt> in
     * Java creates a <tt>BigBinary</tt> which is exactly equal to
     * 0.1 (an unscaled value of 1, with a scale of 1), but it is
     * actually equal to
     * 0.1000000000000000055511151231257827021181583404541015625.
     * This is because 0.1 cannot be represented exactly as a
     * <tt>double</tt> (or, for that matter, as a binary fraction of
     * any finite length).  Thus, the value that is being passed
     * <i>in</i> to the constructor is not exactly equal to 0.1,
     * appearances notwithstanding.
     *
     * <li>
     * When a <tt>double</tt> must be used as a source for a
     * <tt>BigBinary</tt>, note that this constructor provides an
     * exact conversion; it does not give the same result as
     * converting the <tt>double</tt> to a <tt>String</tt> using the
     * {@link Double#toString(double)} method and then using the
     * {@link #BigBinary(String)} constructor.  To get that result,
     * use the <tt>static</tt> {@link #valueOf(double)} method.
     * </ol>
     *
     * @param val <tt>double</tt> value to be converted to 
     *        <tt>BigBinary</tt>.
     * @throws NumberFormatException if <tt>val</tt> is infinite or NaN.
     */
    public BigBinary(double val) {
        // Translate the double into sign, exponent and significand, according
        // to the formulae in JLS, Section 20.10.22.
        long valBits = Double.doubleToLongBits(val);
        boolean sign = (valBits >> 63) != 0;
        int exponent = (int) ((valBits >> 52) & 0x7ffL);
        if (exponent == 0x7FFL)
            throw new NumberFormatException("Infinite or NaN");
        long significand = (valBits & ((1L << 52) - 1)) << 1;
        if (exponent != 0)
            valBits |= 1L << 52;
        if (sign)
            significand = -significand;
        exponent -= 1075;
        // At this point, val == significand * 2**exponent.

        /*
         * Special case zero to supress nonterminating normalization
         * and bogus scale calculation.
         */
        if (significand == 0) {
            intVal = BigInteger.ZERO;
            return;
        }

        // Normalize
        int numTrailingZeros = Long.numberOfTrailingZeros(valBits);
        significand >>= numTrailingZeros;
        exponent += numTrailingZeros;

        // Calculate intVal and scale
        intVal = BigInteger.valueOf(significand);
        scale = -exponent;
    }
    
    /**
     * Translates a <tt>BigInteger</tt> unscaled value and an
     * <tt>int</tt> scale into a <tt>BigBinary</tt>.  The value of
     * the <tt>BigBinary</tt> is
     * <tt>(unscaledVal &times; 2<sup>-scale</sup>)</tt>.
     *
     * @param unscaledVal unscaled value of the <tt>BigBinary</tt>.
     * @param scale scale of the <tt>BigBinary</tt>.
     */
    public BigBinary(BigInteger unscaledVal, int scale) {
        intVal = unscaledVal;
        this.scale = scale;
    }

    // Static Factory Methods

    /**
     * Translates a <tt>long</tt> unscaled value and an
     * <tt>int</tt> scale into a <tt>BigBinary</tt>.  This
     * &quot;static factory method&quot; is provided in preference to
     * a (<tt>long</tt>, <tt>int</tt>) constructor because it
     * allows for reuse of frequently used <tt>BigBinary</tt> values..
     *
     * @param unscaledVal unscaled value of the <tt>BigBinary</tt>.
     * @param scale scale of the <tt>BigBinary</tt>.
     * @return a <tt>BigBinary</tt> whose value is
     *	       <tt>(unscaledVal &times; 2<sup>-scale</sup>)</tt>.
     */
    public static BigBinary valueOf(long unscaledVal, int scale) {
        return new BigBinary(BigInteger.valueOf(unscaledVal), scale);
    }

    /**
     * Translates a <tt>long</tt> value into a <tt>BigBinary</tt>
     * with a scale of zero.  This &quot;static factory method&quot;
     * is provided in preference to a (<tt>long</tt>) constructor
     * because it allows for reuse of frequently used
     * <tt>BigBinary</tt> values.
     *
     * @param val value of the <tt>BigBinary</tt>.
     * @return a <tt>BigBinary</tt> whose value is <tt>val</tt>.
     */
    public static BigBinary valueOf(long val) {
        return valueOf(val, 0);
    }

    /**
     * Translates a <tt>double</tt> into a <tt>BigBinary</tt>.
     * @param  val <tt>double</tt> to convert to a <tt>BigBinary</tt>.
     * @return a <tt>BigBinary</tt> whose value is equal to the value of <tt>val</tt>.
     * @throws NumberFormatException if <tt>val</tt> is infinite or NaN.
     */
    public static BigBinary valueOf(double val) {
        // Reminder: a zero double returns '0.0', so we cannot fastpath
        // to use the constant ZERO.  This might be important enough to
        // justify a factory approach, a cache, or a few private
        // constants, later.
        return new BigBinary(val);
    }

    /**
     * Returns the signum function of this <tt>BigBinary</tt>.
     *
     * @return -1, 0, or 1 as the value of this <tt>BigBinary</tt> 
     *         is negative, zero, or positive.
     */
    public int signum() {
        return intVal.signum();
    }

    /**
     * Returns the <i>scale</i> of this <tt>BigBinary</tt>.  If zero
     * or positive, the scale is the number of binary digits to the right of
     * the binary point.  If negative, the unscaled value of the
     * number is multiplied by two to the power of the negation of the
     * scale.  For example, a scale of <tt>-3</tt> means the unscaled
     * value is multiplied by 8.
     *
     * @return the scale of this <tt>BigBinary</tt>.
     */
    public int scale() {
        return scale;
    }

    /**
     * Returns a <tt>BigInteger</tt> whose value is the <i>unscaled
     * value</i> of this <tt>BigBinary</tt>.  (Computes <tt>(this *
     * 2<sup>this.scale()</sup>)</tt>.)
     *
     * @return the unscaled value of this <tt>BigBinary</tt>.
     */
    public BigInteger unscaledValue() {
        return intVal;
    }

    // Comparison Operations

    /**
     * Compares this <tt>BigBinary</tt> with the specified
     * <tt>BigBinary</tt>.  Two <tt>BigBinary</tt> objects that are
     * equal in value but have a different scale (like 2.0 and 2.00)
     * are considered equal by this method.  This method is provided
     * in preference to individual methods for each of the six boolean
     * comparison operators (&lt;, ==, &gt;, &gt;=, !=, &lt;=).  The
     * suggested idiom for performing these comparisons is:
     * <tt>(x.compareTo(y)</tt> &lt;<i>op</i>&gt; <tt>0)</tt>, where
     * &lt;<i>op</i>&gt; is one of the six comparison operators.
     *
     * @param  val <tt>BigBinary</tt> to which this <tt>BigBinary</tt> is 
     *         to be compared.
     * @return -1, 0, or 1 as this <tt>BigBinary</tt> is numerically 
     *          less than, equal to, or greater than <tt>val</tt>.
     */
    public int compareTo(BigBinary val) {
        return toBigDecimal().compareTo(val.toBigDecimal());
    }

    /**
     * Compares this <tt>BigBinary</tt> with the specified
     * <tt>Object</tt> for equality.  Unlike {@link
     * #compareTo(BigBinary) compareTo}, this method considers two
     * <tt>BigBinary</tt> objects equal only if they are equal in
     * value and scale (thus 2.0 is not equal to 2.00 when compared by
     * this method).
     *
     * @param  x <tt>Object</tt> to which this <tt>BigBinary</tt> is 
     *         to be compared.
     * @return <tt>true</tt> if and only if the specified <tt>Object</tt> is a
     *         <tt>BigBinary</tt> whose value and scale are equal to this 
     *         <tt>BigBinary</tt>'s.
     * @see    #compareTo(com.sun.electric.tool.simulation.interval.BigBinary)
     * @see    #hashCode
     */
    public boolean equals(Object x) {
        if (!(x instanceof BigBinary))
            return false;
        BigBinary xBin = (BigBinary) x;
        if (scale != xBin.scale)
            return false;
        return this.intVal.equals(xBin.intVal);
    }

    /**
     * Returns the minimum of this <tt>BigBinary</tt> and
     * <tt>val</tt>.
     *
     * @param  val value with which the minimum is to be computed.
     * @return the <tt>BigBinary</tt> whose value is the lesser of this 
     *         <tt>BigBinary</tt> and <tt>val</tt>.  If they are equal, 
     *         as defined by the {@link #compareTo(BigBinary) compareTo}  
     *         method, <tt>this</tt> is returned.
     * @see    #compareTo(com.sun.electric.tool.simulation.interval.BigBinary)
     */
    public BigBinary min(BigBinary val) {
        return (compareTo(val) <= 0 ? this : val);
    }

    /**
     * Returns the maximum of this <tt>BigBinary</tt> and <tt>val</tt>.
     *
     * @param  val value with which the maximum is to be computed.
     * @return the <tt>BigBinary</tt> whose value is the greater of this 
     *         <tt>BigBinary</tt> and <tt>val</tt>.  If they are equal, 
     *         as defined by the {@link #compareTo(BigBinary) compareTo} 
     *         method, <tt>this</tt> is returned.
     * @see    #compareTo(com.sun.electric.tool.simulation.interval.BigBinary)
     */
    public BigBinary max(BigBinary val) {
        return (compareTo(val) >= 0 ? this : val);
    }

    // Hash Function

    /**
     * Returns the hash code for this <tt>BigBinary</tt>.  Note that
     * two <tt>BigBinary</tt> objects that are numerically equal but
     * differ in scale (like 2.0 and 2.00) will generally <i>not</i>
     * have the same hash code.
     *
     * @return hash code for this <tt>BigBinary</tt>.
     * @see #equals(Object)
     */
    public int hashCode() {
	    return 31*intVal.hashCode() + scale;
    }

    /**
     * Converts this <tt>BigBinary</tt> to a <tt>BigDecimal</tt>.
     * @return this <tt>BigBinary</tt> converted to a <tt>BigDecimal</tt>.
     */
    public BigDecimal toBigDecimal() {
        return scale > 0 ?
            new BigDecimal(intVal).divide(BigDecimal.valueOf(2).pow(scale)) :
            new BigDecimal(intVal.shiftLeft(-scale));
    }

    /**
     * Converts this <tt>BigBinary</tt> to a <tt>BigInteger</tt>.
     * This conversion is analogous to a <a
     * href="http://java.sun.com/docs/books/jls/second_edition/html/conversions.doc.html#25363"><i>narrowing
     * primitive conversion</i></a> from <tt>double</tt> to
     * <tt>long</tt> as defined in the <a
     * href="http://java.sun.com/docs/books/jls/html/">Java Language
     * Specification</a>: any fractional part of this
     * <tt>BigBinary</tt> will be discarded.  Note that this
     * conversion can lose information about the precision of the
     * <tt>BigBinary</tt> value.
     * <p>
     * To have an exception thrown if the conversion is inexact (in
     * other words if a nonzero fractional part is discarded), use the
     * {@link #toBigIntegerExact()} method.
     *
     * @return this <tt>BigNinary</tt> converted to a <tt>BigInteger</tt>.
     */
    public BigInteger toBigInteger() {
        // force to an integer, quietly
        return this.toBigDecimal().toBigInteger();
    }

    /**
     * Converts this <tt>BigBinary</tt> to a <tt>BigInteger</tt>,
     * checking for lost information.  An exception is thrown if this
     * <tt>BigBinary</tt> has a nonzero fractional part.
     *
     * @return this <tt>BigBinary</tt> converted to a <tt>BigInteger</tt>.
     * @throws ArithmeticException if <tt>this</tt> has a nonzero
     *         fractional part.
     */
    public BigInteger toBigIntegerExact() {
        // round to an integer, with Exception if decimal part non-0
        return this.toBigDecimal().toBigIntegerExact();
    }

    /**
     * Converts this <tt>BigBinary</tt> to a <tt>long</tt>.  This
     * conversion is analogous to a <a
     * href="http://java.sun.com/docs/books/jls/second_edition/html/conversions.doc.html#25363"><i>narrowing
     * primitive conversion</i></a> from <tt>double</tt> to
     * <tt>short</tt> as defined in the <a
     * href="http://java.sun.com/docs/books/jls/html/">Java Language
     * Specification</a>: any fractional part of this
     * <tt>BigBinary</tt> will be discarded, and if the resulting
     * &quot;<tt>BigInteger</tt>&quot; is too big to fit in a
     * <tt>long</tt>, only the low-order 64 bits are returned.
     * Note that this conversion can lose information about the
     * overall magnitude and precision of this <tt>BigBinary</tt> value as well
     * as return a result with the opposite sign.
     * 
     * @return this <tt>BigBinary</tt> converted to a <tt>long</tt>.
     */
    public long longValue(){
        return toBigInteger().longValue();
    }

    /**
     * Converts this <tt>BigBinary</tt> to a <tt>long</tt>, checking
     * for lost information.  If this <tt>BigBinary</tt> has a
     * nonzero fractional part or is out of the possible range for a
     * <tt>long</tt> result then an <tt>ArithmeticException</tt> is
     * thrown.
     *
     * @return this <tt>BigBinary</tt> converted to a <tt>long</tt>.
     * @throws ArithmeticException if <tt>this</tt> has a nonzero
     *         fractional part, or will not fit in a <tt>long</tt>.
     */
    public long longValueExact() {
        return toBigDecimal().longValueExact();
    }

    /**
     * Converts this <tt>BigBinary</tt> to an <tt>int</tt>.  This
     * conversion is analogous to a <a
     * href="http://java.sun.com/docs/books/jls/second_edition/html/conversions.doc.html#25363"><i>narrowing
     * primitive conversion</i></a> from <tt>double</tt> to
     * <tt>short</tt> as defined in the <a
     * href="http://java.sun.com/docs/books/jls/html/">Java Language
     * Specification</a>: any fractional part of this
     * <tt>BigBinary</tt> will be discarded, and if the resulting
     * &quot;<tt>BigBinary</tt>&quot; is too big to fit in an
     * <tt>int</tt>, only the low-order 32 bits are returned.
     * Note that this conversion can lose information about the
     * overall magnitude and precision of this <tt>BigBinary</tt>
     * value as well as return a result with the opposite sign.
     * 
     * @return this <tt>BigBinary</tt> converted to an <tt>int</tt>.
     */
    public int intValue() {
        return toBigInteger().intValue();
    }

    /**
     * Converts this <tt>BigBinary</tt> to an <tt>int</tt>, checking
     * for lost information.  If this <tt>BigBinary</tt> has a
     * nonzero fractional part or is out of the possible range for an
     * <tt>int</tt> result then an <tt>ArithmeticException</tt> is
     * thrown.
     *
     * @return this <tt>BigBinary</tt> converted to an <tt>int</tt>.
     * @throws ArithmeticException if <tt>this</tt> has a nonzero
     *         fractional part, or will not fit in an <tt>int</tt>.
     */
    public int intValueExact() {
       long num;
       num = this.longValueExact();     // will check binary part
       if ((int)num != num)
           throw new java.lang.ArithmeticException("Overflow");
       return (int)num;
    }

    /**
     * Converts this <tt>BigBinary</tt> to a <tt>short</tt>, checking
     * for lost information.  If this <tt>BigBinary</tt> has a
     * nonzero fractional part or is out of the possible range for a
     * <tt>short</tt> result then an <tt>ArithmeticException</tt> is
     * thrown.
     *
     * @return this <tt>BigBinary</tt> converted to a <tt>short</tt>.
     * @throws ArithmeticException if <tt>this</tt> has a nonzero
     *         fractional part, or will not fit in a <tt>short</tt>.
     */
    public short shortValueExact() {
       long num;
       num = this.longValueExact();     // will check binary part
       if ((short)num != num)
           throw new java.lang.ArithmeticException("Overflow");
       return (short)num;
    }

    /**
     * Converts this <tt>BigBinary</tt> to a <tt>byte</tt>, checking
     * for lost information.  If this <tt>BigBinary</tt> has a
     * nonzero fractional part or is out of the possible range for a
     * <tt>byte</tt> result then an <tt>ArithmeticException</tt> is
     * thrown.
     *
     * @return this <tt>BigBinary</tt> converted to a <tt>byte</tt>.
     * @throws ArithmeticException if <tt>this</tt> has a nonzero
     *         fractional part, or will not fit in a <tt>byte</tt>.
     */
    public byte byteValueExact() {
       long num;
       num = this.longValueExact();     // will check binary part
       if ((byte)num != num)
           throw new java.lang.ArithmeticException("Overflow");
       return (byte)num;
    }

    /**
     * Converts this <tt>BigBinary</tt> to a <tt>float</tt>.
     * This conversion is similar to the <a
     * href="http://java.sun.com/docs/books/jls/second_edition/html/conversions.doc.html#25363"><i>narrowing
     * primitive conversion</i></a> from <tt>double</tt> to
     * <tt>float</tt> defined in the <a
     * href="http://java.sun.com/docs/books/jls/html/">Java Language
     * Specification</a>: if this <tt>BigBinary</tt> has too great a
     * magnitude to represent as a <tt>float</tt>, it will be
     * converted to {@link Float#NEGATIVE_INFINITY} or {@link
     * Float#POSITIVE_INFINITY} as appropriate.  Note that even when
     * the return value is finite, this conversion can lose
     * information about the precision of the <tt>BigBinary</tt>
     * value.
     * 
     * @return this <tt>BigBinary</tt> converted to a <tt>float</tt>.
     */
    public float floatValue(){
        // Somewhat inefficient, but guaranteed to work.
        return Float.parseFloat(this.toString());
    }

    /**
     * Converts this <tt>BigBinary</tt> to a <tt>double</tt>.
     * This conversion is similar to the <a
     * href="http://java.sun.com/docs/books/jls/second_edition/html/conversions.doc.html#25363"><i>narrowing
     * primitive conversion</i></a> from <tt>double</tt> to
     * <tt>float</tt> as defined in the <a
     * href="http://java.sun.com/docs/books/jls/html/">Java Language
     * Specification</a>: if this <tt>BigBinary</tt> has too great a
     * magnitude represent as a <tt>double</tt>, it will be
     * converted to {@link Double#NEGATIVE_INFINITY} or {@link
     * Double#POSITIVE_INFINITY} as appropriate.  Note that even when
     * the return value is finite, this conversion can lose
     * information about the precision of the <tt>BigBinary</tt>
     * value.
     * 
     * @return this <tt>BigBinary</tt> converted to a <tt>double</tt>.
     */
    public double doubleValue(){
        // Somewhat inefficient, but guaranteed to work.
        return Double.parseDouble(this.toString());
    }

    /**
     * Returns the size of an ulp, a unit in the last place, of this
     * <tt>BigBinary</tt>.  An ulp of a nonzero <tt>BigBinary</tt>
     * value is the positive distance between this value and the
     * <tt>BigBinary</tt> value next larger in magnitude with the
     * same number of digits.  An ulp of a zero value is numerically
     * equal to 1 with the scale of <tt>this</tt>.  The result is
     * stored with the same scale as <code>this</code> so the result
     * for zero and nonzero values is equal to <code>[1,
     * this.scale()]</code>.
     *
     * @return the size of an ulp of <tt>this</tt>
     */
    public BigBinary ulp() {
	    return BigBinary.valueOf(1, this.scale());
    }
}

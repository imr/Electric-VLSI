/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ???????????.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
/*
 * @(#)FloatingDecimal.java	1.24 02/02/06
 *
 * Copyright 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//package java.lang;

import java.math.BigDecimal;
import java.math.BigInteger;

class FloatingDecimal {
    int exponent;
    int digits[];

	/**
     * Translates the String representation of a BigDecimal into a
     * BigDecimal.  The String representation consists of an optional
     * sign, <tt>'+'</tt> (<tt>'&#92;u002B'</tt>) or <tt>'-'</tt>
     * (<tt>'&#92;u002D'</tt>), followed by a sequence of zero or more
     * decimal digits ("the integer"), optionally followed by a
     * fraction, optionally followed by an exponent.
     *
     * <p>The fraction consists of of a decimal point followed by zero or more
     * decimal digits.  The string must contain at least one digit in either
     * the integer or the fraction.  The number formed by the sign, the
     * integer and the fraction is referred to as the <i>significand</i>.
     *
     * <p>The exponent consists of the character <tt>'e'</tt>
     * (<tt>'&#92;u0075'</tt>) or <tt>'E'</tt> (<tt>'&#92;u0045'</tt>)
     * followed by one or more decimal digits.  The value of the
     * exponent must lie between -{@link Integer#MAX_VALUE} ({@link
     * Integer#MIN_VALUE}+1) and {@link Integer.MAX_VALUE}, inclusive.
     *
     * @param val String representation of BigDecimal.
     * @throws NumberFormatException <tt>val</tt> is not a valid representation
     *	       of a BigDecimal.
     */
    FloatingDecimal(String val) {
        // This is the primary string to BigDecimal constructor; all
        // incoming strings end up here; it uses explicit (inline)
        // parsing for speed and generates at most one intermediate
        // (temporary) object (a char[] array).

        // use array bounds checking to handle too-long, len == 0,
        // bad offset, etc.
        try {
			int exponent = 0;
			// If exponent is present, break into exponent and significand
			int ePos = val.indexOf('e');
			if (ePos == -1)
				ePos = val.indexOf('E');
			if (ePos != -1) {
				String exp = val.substring(ePos+1);
				if (exp.length() == 0)              /* "1.2e" illegal! */
					throw new NumberFormatException();
				if (exp.charAt(0) == '+') {
					exp = exp.substring(1);         /* Discard leading '+' */
					if (exp.length() == 0 ||	    /* "123.456e+" illegal! */
						exp.charAt(0) == '-')       /* "123.456e+-7" illegal! */
						throw new NumberFormatException();
				}
				exponent = Integer.parseInt(exp);
				val = val.substring(0, ePos);
			}
			BigDecimal mantissa = new BigDecimal(val);
			BigInteger unscaledValue = mantissa.unscaledValue();
			int signum = unscaledValue.signum();
			unscaledValue = unscaledValue.abs();
			int bitLength = unscaledValue.bitLength();
			int pow = bitLength % 32;
			if (pow != 0)
				unscaledValue = unscaledValue.shiftLeft(32 - pow);
			byte[] byteArray = unscaledValue.toByteArray();
			digits = new int[byteArray.length/4];
			assert digits.length == (bitLength + 31) % 32;
			assert byteArray[0] == 0;
			for (int i = 0; i < digits.length; i++) {
				digits[i] =
					((byteArray[i*4+1]&0xff) << 24) |
					((byteArray[i*4+2]&0xff) << 16) |
					((byteArray[i*4+3]&0xff) <<  8) |
					((byteArray[i*4+4]&0xff)      ) ;
				//				System.out.println("digits["+i+"]="+Integer.toHexString(digits[i]));
			}

			// Combine exponent into significand
			assert (mantissa.scale() >= 0);  // && scale <= Integer.MAX_VALUE
			long longScale = (long)mantissa.scale() - (long)exponent; 	// Avoid errors 
			                                                // in calculating scale
			if(longScale > Integer.MAX_VALUE)
				throw new NumberFormatException("Final scale out of range");
			int scale = (int)longScale;
			assert (scale == longScale && // conversion should be exact
				Math.abs(longScale) <= Integer.MAX_VALUE)  // exponent range 
				// check
				:longScale;
			//			System.out.println("scale="+scale+" pow="+(-bitLength));
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new NumberFormatException();
        } catch (NegativeArraySizeException e) {
            throw new NumberFormatException();
        }
    }

	public static void main(String[] args) {
		String s = "0.0123456789e-308";
		
		for (int i = 0; i < 1000000; i++) {
			Double.parseDouble(s);
			new FloatingDecimal(s);
		}
		System.out.println("Warm end");

		long startTime1 = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++)
			Double.parseDouble(s);
		long endTime1 = System.currentTimeMillis();
		System.out.println("**** parseDouble took " + (endTime1-startTime1) + " mSec");

		long startTime2 = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++)
			new FloatingDecimal(s);
		long endTime2 = System.currentTimeMillis();
		System.out.println("**** FloatingDecimal took " + (endTime2-startTime2) + " mSec");
	}

}

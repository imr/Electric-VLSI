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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import sun.misc.FpUtils;

class Floating {
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
    Floating(String val) {
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

	static Field isExceptionalField;
	static Field isNegativeField;
	static Field decExponentField;
	static Field digitsField;
	static Field nDigitsField;
	static Field bigIntExpField;
	static Field bigIntNBitsField;
	static Field mustSetRoundDirField;
	static Field fromHexField;
	static Field roundDirField;

	static void initFields() {
		Class cls;
		try {
			cls = Class.forName("sun.misc.FloatingDecimal");
		} catch (ClassNotFoundException e) {
			System.out.println(e.toString());
			return;
		}
		try {
			(isExceptionalField = cls.getDeclaredField("isExceptional")).setAccessible(true);
			(isNegativeField = cls.getDeclaredField("isNegative")).setAccessible(true);
			(decExponentField = cls.getDeclaredField("decExponent")).setAccessible(true);
			(digitsField = cls.getDeclaredField("digits")).setAccessible(true);
			(nDigitsField = cls.getDeclaredField("nDigits")).setAccessible(true);
			(bigIntExpField = cls.getDeclaredField("bigIntExp")).setAccessible(true);
			(bigIntNBitsField = cls.getDeclaredField("bigIntNBits")).setAccessible(true);
			(mustSetRoundDirField = cls.getDeclaredField("mustSetRoundDir")).setAccessible(true);
			(fromHexField = cls.getDeclaredField("fromHex")).setAccessible(true);
			(roundDirField = cls.getDeclaredField("roundDir")).setAccessible(true);

		} catch (NoSuchFieldException e) {
			System.out.println(e.toString());
			return;
		} catch (SecurityException e) {
			System.out.println(e.toString());
			return;
		}
	}

	static void showFloatingDecimal(sun.misc.FloatingDecimal fd) {
		try {
			System.out.println("isExceptional=" + isExceptionalField.getBoolean(fd));
			System.out.println("isNegative=" + isNegativeField.getBoolean(fd));
			System.out.println("decExponent=" + decExponentField.getInt(fd));
			System.out.println("digits=" + Arrays.toString((char[])digitsField.get(fd)));
			System.out.println("nDigits=" + nDigitsField.getInt(fd));
			System.out.println("bigIntExp=" + bigIntExpField.getInt(fd));
			System.out.println("bigIntNBits=" + bigIntNBitsField.getInt(fd));
			System.out.println("mustSetRoundDir=" + mustSetRoundDirField.getBoolean(fd));
			System.out.println("fromHex=" + fromHexField.getBoolean(fd));
			System.out.println("roundDir=" + roundDirField.getInt(fd));
		} catch (IllegalAccessException e) {
			System.out.println(e.toString());
		}
	}

	static void testFloatingDecimal() {
		showFloatingDecimal(sun.misc.FloatingDecimal.readJavaFormatString("0.10000000"));
	}

	static void testReflection(String className) {
		Class cls;
		try {
			cls = Class.forName(className);
		} catch (ClassNotFoundException e) {
			System.out.println(e.toString());
			return;
		}

		Field[] fieldsAll, fields;
		Constructor[] constructorsAll, constructors;
		Method[] methodsAll, methods;
		try {
			fieldsAll = cls.getDeclaredFields();
			fields = cls.getFields();
			constructorsAll = cls.getDeclaredConstructors();
			constructors = cls.getConstructors();
			methodsAll = cls.getDeclaredMethods();
			methods = cls.getMethods();
		} catch (SecurityException e) {
			System.out.println(e.toString());
			return;
		}
		System.out.println("------ " + Modifier.toString(cls.getModifiers()) + " " + cls);
		for (int i = 0; i < fieldsAll.length; i++)
			System.out.println(fieldsAll[i].toString());
		for (int i = 0; i < constructorsAll.length; i++)
			System.out.println(constructorsAll[i].toString());
		for (int i = 0; i < methodsAll.length; i++) {
			Method method = methodsAll[i];
			System.out.println(method.toString());
		}
		System.out.println("------");
		for (int i = 0; i < fields.length; i++)
			System.out.println(fields[i].toString());
		for (int i = 0; i < constructors.length; i++)
			System.out.println(constructors[i].toString());
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			System.out.println(method.toString());
		}
		System.out.println("------");
	}

	static void testFpUtils() {
		double x = 0;
		double y = FpUtils.nextUp(x);
		System.out.println(Double.toHexString(x) + " --> " + Double.toHexString(y));
	}

	static void findError() {
// 		long m;
// 		long b = 0;
// 		do {
// 			m = (1L << 56) + b*(1L << 34) + (1L << 32);
// 			b++;
// 		} while (m % 100 <= 92);
// 		long a = m / 100 + 1;
// 		System.out.println("b=" + (b-1));
// 		System.out.println("a=" + a + " " + Long.toHexString(a));
// 		System.out.println("a*100=" + a*100 + " " + Long.toHexString(a*100));
// 		double x = (double)a;
// 		double y = x*100;
// 		float z = (float)y;
// 		float z1 = sun.misc.FpUtils.nextUp(z);
// 		long c = (long)z;
// 		long c1 = (long)z1;

// 		System.out.println("x=" + x + " " + Double.toHexString(x));
// 		System.out.println("y=" + y + " " + Double.toHexString(y));
// 		System.out.println("z=" + z + " " + Float.toHexString(z));
// 		System.out.println("z1=" + z1 + " " + Float.toHexString(z1));
// 		System.out.println("cl=" + c + " " + Long.toHexString(c));
// 		System.out.println("ch=" + c1 + " " + Long.toHexString(c1));

		long l0 = 720579591101481l;
		long l = l0*100;
		String s = l0 + "e2";
		float v1 = sun.misc.FloatingDecimal.readJavaFormatString(s).floatValue();
		float v2 = sun.misc.FpUtils.nextUp(v1);


		System.out.println("l=" + l);
		System.out.println("s=" + s);
		System.out.println("v1=" + v1 + " " + Float.toHexString(v1) + " |l-v1|=" + Math.abs(l-(long)v1));
		System.out.println("v2=" + v2 + " " + Float.toHexString(v2) + " |l-v2|=" + Math.abs(l-(long)v2));
// 		double x = 0x1.9999A1999999Ap0;
// 		double y = x*10;
// 		float yf = (float)y;
// 		System.out.println("x=" + x + " " + Double.toHexString(x));
// 		System.out.println("y=" + y + " " + Double.toHexString(y));
// 		System.out.println("yf=" + yf + " " + Float.toHexString(yf));
	}

	static void testRem() {
		int x = 0x80000000;
		System.out.println("x=" + x + " x%5=" + x%5 );
	}

	public static void main(String[] args) {
//		findError();
 		testReflection("sun.misc.FormattedFloatingDecimal");
 		testReflection("sun.misc.FloatingDecimal");
		testReflection("sun.misc.FDBigInt");
 		testReflection("sun.misc.FpUtils");
// 		initFields();
// 		testFloatingDecimal();
//		testRem();

// 		String s = "0.0123456789e-308";
		
// 		for (int i = 0; i < 1000000; i++) {
// 			Double.parseDouble(s);
// 			new FloatingDecimal(s);
// 		}
// 		System.out.println("Warm end");

// 		long startTime1 = System.currentTimeMillis();
// 		for (int i = 0; i < 1000000; i++)
// 			Double.parseDouble(s);
// 		long endTime1 = System.currentTimeMillis();
// 		System.out.println("**** parseDouble took " + (endTime1-startTime1) + " mSec");

// 		long startTime2 = System.currentTimeMillis();
// 		for (int i = 0; i < 1000000; i++)
// 			new FloatingDecimal(s);
// 		long endTime2 = System.currentTimeMillis();
// 		System.out.println("**** FloatingDecimal took " + (endTime2-startTime2) + " mSec");
	}

}

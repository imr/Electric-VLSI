/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DirectedFloatingDecimal.java
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

package com.sun.electric.tool.simulation.interval;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;

/**
 * Utility class for conversion between double values and decimal strings
 * with directed rounding.
 */
public class DirectedFloatingDecimal
{
	// Implementations

    /**
	 * Value indicates that implementation hasn't been detected yet.
     */
	private static final int IMPL_UNDETECTED = 0;

	/*
	 * Default implementation which doesn't use reflection.
	 */
	private static final int IMPL_DEFAULT = 1;

	/*
	 * Implementation that rounds using MathContext parameter.
	 */
	private static final int IMPL_REFLECTED = 2;

    /**
	 * Selects implementation of <i>toString</i> method.
	 */
	private static int toStringImplementation = IMPL_UNDETECTED;

	private static Constructor bigDecimalConstructor;
	private static Object[] bigDecimalConstructorArgs;
	private static Object[] mcFloor;
	private static Object[] mcCeiling;

	private static int parseDoubleImplementation = IMPL_UNDETECTED;

	private static Method parseDoubleMethod;
	private static Object[] parseDoubleMethodArgs;
	private static Object modeFloor;
	private static Object modeCeiling;

    /**
     * Don't let anyone instantiate this class.
     */
	private DirectedFloatingDecimal() {
	}

	// parseDouble staff

	public static double parseDoubleInf(String s) {
		return parseDouble(s, BigDecimal.ROUND_FLOOR);
	}

	public static double parseDoubleSup(String s) {
		return parseDouble(s, BigDecimal.ROUND_CEILING);
	}

	private static double parseDouble(String s, int roundingMode) {
		switch (parseDoubleImplementation) {
			case IMPL_UNDETECTED:
				parseDoubleImplementation = IMPL_DEFAULT;
				if (initParseDoubleReflected())
					parseDoubleImplementation = IMPL_REFLECTED;
				return parseDouble(s, roundingMode);
			case IMPL_REFLECTED:
				try {
					return parseDoubleReflected(s, roundingMode);
				} catch (Exception e) {
					toStringImplementation = IMPL_DEFAULT;
				}
				return parseDouble(s, roundingMode);
			case IMPL_DEFAULT:
			default:
				return parseDoubleDefault(s, roundingMode);
		}
	}

	private static double parseDoubleDefault(String s, int roundingMode) {
		String ts = s.trim();
		if (ts.length() >= 2) {
			switch (ts.charAt(0)) {
				case 'N':
					if (ts.equals("NaN"))
						return Double.NaN;
					break;
				case 'I':
					if (ts.equals("Infinity"))
						return returnInfinity(s, roundingMode);
					break;
				case '-':
				case '+':
					if (ts.charAt(1) == 'I') {
						if (ts.substring(1).equals("Infinity"))
							return returnInfinity(s, roundingMode);
					}
					break;
			}
		}
		BigDecimal exact = new BigDecimal(ts);
		double d = exact.doubleValue();
		if (Double.isInfinite(d))
			return returnInfinity(s, roundingMode);
		int diff = exact.compareTo(new BigDecimal(d));
		if (roundingMode == BigDecimal.ROUND_CEILING) {
			if (diff > 0) d = Interval.next(d);
		} else {
			if (diff < 0) d = Interval.prev(d);
		}
		return d;
	}

	private static double returnInfinity(String ts, int roundingMode) {
		if (ts.charAt(0) == '-')
			return (roundingMode == BigDecimal.ROUND_FLOOR ? Double.NEGATIVE_INFINITY : -Double.MAX_VALUE);
		else
			return (roundingMode == BigDecimal.ROUND_FLOOR ? Double.MAX_VALUE : Double.POSITIVE_INFINITY);
			
	}

	private static double parseDoubleReflected(String ts, int roundingMode) throws Exception {
		parseDoubleMethodArgs[0] = ts;
		parseDoubleMethodArgs[1] = roundingMode == BigDecimal.ROUND_FLOOR ? modeFloor : modeCeiling;
		return ((Double)parseDoubleMethod.invoke(null, parseDoubleMethodArgs)).doubleValue();
	}

	private static boolean initParseDoubleReflected() {
		try {
			Class roundingModeClass = Class.forName("java.math.RoundingMode");
			parseDoubleMethod = Double.class.getMethod("parseDouble", new Class[] { String.class, roundingModeClass });
			parseDoubleMethodArgs = new Object[2];
			modeFloor = roundingModeClass.getField("ROUND_FLOOR");
			modeCeiling = roundingModeClass.getField("ROUND_FLOOR");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// toString staff

	public static String toStringInf(double d, int precision) {
		return toString(d, precision, BigDecimal.ROUND_FLOOR);
	}

	public static String toStringSup(double d, int precision) {
		return toString(d, precision, BigDecimal.ROUND_CEILING);
	}

	private static String toString(double d, int precision, int roundingMode) {
		if (precision < 1)
			throw new IllegalArgumentException("precision < 1");
		if (Double.isNaN(d) || Double.isInfinite(d) || d == 0)
			return Double.toString(d);

		switch (toStringImplementation) {
			case IMPL_UNDETECTED:
				toStringImplementation = IMPL_DEFAULT;
				if (initToStringReflected())
					toStringImplementation = IMPL_REFLECTED;
				return toString(d, precision, roundingMode);
			case IMPL_REFLECTED:
				try {
					return toStringReflected(d, precision, roundingMode);
				} catch (Exception e) {
					toStringImplementation = IMPL_DEFAULT;
				}
				return toString(d, precision, roundingMode);
			case IMPL_DEFAULT:
			default:
				return toStringDefault(d, precision, roundingMode);
		}
	}

	private static String toStringDefault(double d, int precision, int roundingMode) {
		BigDecimal bd = new BigDecimal(d);
		String s = bd.unscaledValue().abs().toString();
		int drop = 0;
		if (s.length() > precision) {
			drop = s.length() - precision;
			bd = bd.setScale(bd.scale() - drop, roundingMode);
		}
		s = bd.unscaledValue().abs().toString();
		if (s.length() > precision) {
			assert s.length() == precision + 1 && s.charAt(s.length() - 1) == '0';
			drop++;
			bd = bd.setScale(bd.scale() - 1, BigDecimal.ROUND_UNNECESSARY);
			s = bd.unscaledValue().abs().toString();
		}
		
		StringBuffer buf = new StringBuffer(s.length()+14);
		if (bd.signum() < 0)
			buf.append('-');
		buf.append(s.charAt(0));
		if (s.length() > 1) {
			buf.append('.');
			buf.append(s, 1, s.length()-1);
		}
		buf.append('E');
		buf.append(-(long)bd.scale() + (s.length()-1));
		return buf.toString();
	}

	private static boolean initToStringReflected() {
		try {
			Class mathContextClass = Class.forName("java.math.MathContext");
			bigDecimalConstructor = BigDecimal.class.getConstructor(new Class[] { Double.TYPE, mathContextClass });
			bigDecimalConstructorArgs = new Object[2];
			int maxPrecision = 20;
			mcFloor = new Object[maxPrecision];
			mcCeiling = new Object[maxPrecision];
			for (int precision = 0; precision < maxPrecision; precision++) {
				mcFloor[precision] = newMathContext(precision, BigDecimal.ROUND_FLOOR);
				mcCeiling[precision] = newMathContext(precision, BigDecimal.ROUND_CEILING);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static String toStringReflected(double d, int precision, int roundingMode) throws Exception {
		bigDecimalConstructorArgs[0] = new Double(d);
		Object[] mathContexts = (roundingMode == BigDecimal.ROUND_FLOOR ? mcFloor : mcCeiling);
		bigDecimalConstructorArgs[1] = (precision < mathContexts.length ?
			mathContexts[precision] :
			newMathContext(precision, roundingMode));
		BigDecimal bd = (BigDecimal)bigDecimalConstructor.newInstance(bigDecimalConstructorArgs);
		return bd.toString();
	}

	private static Object newMathContext(int precision, int roundingMode) throws Exception {
		Class roundingModeClass = Class.forName("java.math.RoundingMode");
		Method roundingModeValueOf = roundingModeClass.getMethod("valueOf", new Class[] { Integer.TYPE });
		Object roundingModeEnum = roundingModeValueOf.invoke(null, new Object[] { new Integer(roundingMode) });
		Class mathContextClass = Class.forName("java.math.MathContext");
		Constructor mathContextConstructor = mathContextClass.getConstructor(new Class[] { Integer.TYPE, roundingModeClass });
		return mathContextConstructor.newInstance(new Object[] {new Integer(precision), roundingModeEnum});
	}
}

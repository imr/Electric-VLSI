/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TextUtils.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.text;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * This class is a collection of text utilities.
 */
public class TextUtils
{
    /**
     * Determines if the specified character is a ISO-LATIN-1 digit
	 * (<code>'0'</code> through <code>'9'</code>).
     * <p>
	 * This can be method instead of Character, if we are not ready
	 * to handle Arabi-Indic, Devanagaru and other digits.
	 *
     * @param   ch   the character to be tested.
     * @return  <code>true</code> if the character is a ISO-LATIN-1 digit;
     *          <code>false</code> otherwise.
     * @see     java.lang.Character#isDigit(char)
     */
    public static boolean isDigit(char ch) {
        return '0' <= ch && ch <= '9';
    }

    /**
     * Determines if the specified character is a letter or digit.
     * <p>
     * A character is considered to be a letter or digit if either
     * <code>Character.isLetter(char ch)</code> or
     * <code>TextUtils.isDigit(char ch)</code> returns
     * <code>true</code> for the character.
     *
     * @param   ch   the character to be tested.
     * @return  <code>true</code> if the character is a letter or digit;
     *          <code>false</code> otherwise.
     * @see     TextUtils#isDigit(char)
     * @see     java.lang.Character#isJavaLetterOrDigit(char)
     * @see     java.lang.Character#isLetter(char)
     */
    public static boolean isLetterOrDigit(char ch) {
        return isDigit(ch) || Character.isLetter(ch);
    }

    /**
     * Returns canonic char for ignore-case comparison .
     * This is the same as Character.toLowerCase(Character.toUpperCase(ch)).
     * @param ch given char.
     * @return canonic char fo rthe given char.
     */
    public static char canonicChar(char ch)
    {
        if (ch <= 'Z') {
            if (ch >= 'A')
                ch += 'a' - 'A';
        } else {
            if (ch >= '\u0080')
                ch = Character.toLowerCase(Character.toUpperCase(ch));
        }
        return ch;
    }
    
    /**
     * Returns canonic string for ignore-case comparision .
     * FORALL String s1, s2: s1.equalsIgnoreCase(s2) == canonicString(s1).equals(canonicString(s2)
     * FORALL String s: canonicString(canonicString(s)).equals(canonicString(s))
     * @param s given String
     * @return canonic String
     * Simple "toLowerCase" is not sufficent.
     * For example ("\u0131").equalsIgnoreCase("i") , but Character.toLowerCase('\u0131') == '\u0131' .
     */
    public static String canonicString(String s) {
        int i = 0;
        for (; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (canonicChar(ch) != ch) break;
        }
        if (i == s.length()) return s;
        
        char[] chars = s.toCharArray();
        for (; i < s.length(); i++)
            chars[i] = canonicChar(chars[i]);
        return new String(chars);
    }
   
//    static {
//        for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++) {
//            char ch = (char)i;
//            char toLower = Character.toLowerCase(ch);
//            char toUpper = Character.toUpperCase(ch);
//            char canonic = canonicChar(toUpper);
//            if (canonic != toLower) {
//                System.out.println(ch + " " + Integer.toHexString(ch) +
//                        " lower " + toLower + " " + Integer.toHexString(toLower) +
//                        " upper " + toUpper + " " + Integer.toHexString(toUpper) +
//                        " canonic " + canonic + " " + Integer.toHexString(canonic));
//                assert Character.toLowerCase(Character.toUpperCase(canonic)) == canonic;
//            }
//        }
//    }
    
	/**
	 * Method to determine if one string is a subset of another, but case-insensitive.
	 * @param main the main string.
	 * @param with the substring.
	 * @return true if the main string starts with the substring, ignoring case.
	 */
	public static boolean startsWithIgnoreCase(String main, String with)
	{
		int mainLen = main.length();
		int withLen = with.length();
		if (withLen > mainLen) return false;
		for(int i=0; i<withLen; i++)
		{
			char mainChr = canonicChar(main.charAt(i));
			char withChr = canonicChar(with.charAt(i));
			if (mainChr != withChr) return false;
		}
		return true;
	}

	/**
	 * Method to parse the floating-point number in a string.
	 * <P>
	 * There is one reason to use this method instead of Double.parseDouble...
	 * <UL>
	 * <LI>This method does not throw an exception if the number is invalid (or blank).
	 * </UL>
	 * @param text the string with a number in it.
	 * @return the numeric value.
	 */
    public static double atof(String text)
    {
		try
		{
			return Double.parseDouble(text);
		} catch (NumberFormatException e)
		{
			return atof(text, null);
		}
    }

    /**
     * This is the same as TextUtils.atof, except upon failure to convert
     * the passed text to a number, it returns the value in 'defaultVal'.
     * If 'defaultVal' is null and the text cannot be converted to a number,
     * the method returns 0.
     * @param text the string to convert to a double
     * @param defaultVal the value to return if the string cannot be converted to a double
     * @return the numeric value
     */
	public static double atof(String text, Double defaultVal)
	{
        // remove commas that denote 1000's separators
        text = text.replaceAll(",", "");

		double v = 0;
		try
		{
            Number n = parsePostFixNumber(text);
			v = n.doubleValue();
		} catch (NumberFormatException ex)
		{
			int start = 0;
			while (start < text.length() && text.charAt(start) == ' ') start++;
			int end = start;

			// allow initial + or -
			if (end < text.length() && (text.charAt(end) == '-' || text.charAt(end) == '+')) end++;

			// allow digits
			while (end < text.length() && TextUtils.isDigit(text.charAt(end))) end++;

			// allow decimal point and digits beyond it
			if (end < text.length() && text.charAt(end) == '.')
			{
				end++;
				while (end < text.length() && TextUtils.isDigit(text.charAt(end))) end++;
			}

			// allow exponent
			if (end < text.length() && (text.charAt(end) == 'e' || text.charAt(end) == 'E'))
			{
				end++;
				if (end < text.length() && (text.charAt(end) == '-' || text.charAt(end) == '+')) end++;
				while (end < text.length() && TextUtils.isDigit(text.charAt(end))) end++;
			}

			if (end <= start) {
                if (defaultVal != null) return defaultVal.doubleValue();
                return 0;
            }
			try
			{
				v = Double.parseDouble(text.substring(start, end-start));
			} catch (NumberFormatException e)
			{
				v = 0;
			}
		}
		return v;
	}

	/**
	 * Method to parse the number in a string.
	 * <P>
	 * There are many reasons to use this method instead of Integer.parseInt...
	 * <UL>
	 * <LI>This method can handle any radix.
	 *     If the number begins with "0", presume base 8.
	 *     If the number begins with "0b", presume base 2.
	 *     If the number begins with "0x", presume base 16.
	 *     Otherwise presume base 10.
	 * <LI>This method can handle numbers that affect the sign bit.
	 *     If you give 0xFFFFFFFF to Integer.parseInt, you get a numberFormatPostFix exception.
	 *     This method properly returns -1.
	 * <LI>This method does not require that the entire string be part of the number.
	 *     If there is extra text after the end, Integer.parseInt fails (for example "123xx").
	 * <LI>This method does not throw an exception if the number is invalid (or blank).
	 * </UL>
	 * @param s the string with a number in it.
	 * @return the numeric value.
	 */
	public static int atoi(String s)
	{
		return atoi(s, 0, 0);
	}

	/**
	 * Method to parse the number in a string.
	 * See the comments for "atoi(String s)" for reasons why this method exists.
	 * @param s the string with a number in it.
	 * @param pos the starting position in the string to find the number.
	 * @return the numeric value.
	 */
	public static int atoi(String s, int pos)
	{
		return atoi(s, pos, 0);
	}

	/**
	 * Method to parse the number in a string.
	 * See the comments for "atoi(String s)" for reasons why this method exists.
	 * @param s the string with a number in it.
	 * @param pos the starting position in the string to find the number.
	 * @param base the forced base of the number (0 to determine it automatically).
	 * @return the numeric value.
	 */
	public static int atoi(String s, int pos, int base)
	{
		int num = 0;
		int sign = 1;
		int len = s.length();
		if (pos < len && s.charAt(pos) == '-')
		{
			pos++;
			sign = -1;
		}
		if (base == 0)
		{
			base = 10;
			if (pos < len && s.charAt(pos) == '0')
			{
				pos++;
				base = 8;
				if (pos < len && (s.charAt(pos) == 'x' || s.charAt(pos) == 'X'))
				{
					pos++;
					base = 16;
				} else if (pos < len && (s.charAt(pos) == 'b' || s.charAt(pos) == 'B'))
				{
					pos++;
					base = 2;
				}
			}
		}
		for(; pos < len; pos++)
		{
			char cat = s.charAt(pos);
			int digit = Character.digit(cat, base);
			if (digit < 0) break;
			num = num * base + digit;
// 			if ((cat >= 'a' && cat <= 'f') || (cat >= 'A' && cat <= 'F'))
// 			{
// 				if (base != 16) break;
// 				num = num * 16;
// 				if (cat >= 'a' && cat <= 'f') num += cat - 'a' + 10; else
// 					num += cat - 'A' + 10;
// 				continue;
// 			}
//			if (!TextUtils.isDigit(cat)) break;
//			if (cat >= '8' && base == 8) break;
//			num = num * base + cat - '0';
		}
		return(num * sign);
	}

	/**
	 * Method to get the numeric value of a string that may be an expression.
	 * @param expression the string that may be an expression.
	 * @return the numeric value of the expression.
	 * This method uses the Bean Shell to evaluate non-numeric strings.
	 */
	public static double getValueOfExpression(String expression)
	{
		if (isANumber(expression))
		{
			double res = atof(expression);
			return res;
		}
		Object o = EvalJavaBsh.evalJavaBsh.doEvalLine(expression);
		if (o == null) return 0;
		if (o instanceof Double) return ((Double)o).doubleValue();
		if (o instanceof Integer) return ((Integer)o).intValue();
		return 0;
	}

	private static NumberFormat numberFormatPostFix = null;

	/**
	 * Method to convert a double to a string.
     * Also scales number and appends appropriate postfix UnitScale string.
	 * @param v the double value to format.
	 * @return the string representation of the number.
	 */
	public static String formatDoublePostFix(double v)
	{
		if (numberFormatPostFix == null) {
            numberFormatPostFix = NumberFormat.getInstance(Locale.US);
            try {
                DecimalFormat d = (DecimalFormat)numberFormatPostFix;
                d.setDecimalSeparatorAlwaysShown(false);
                d.setGroupingSize(300);     // make it so comma (1000's separator) is never used
            } catch (Exception e) {}
        }
        numberFormatPostFix.setMaximumFractionDigits(3);
        int unitScaleIndex = 0;
        if (v != 0) {
            while ((Math.abs(v) >= 1000000) && (unitScaleIndex > UnitScale.UNIT_BASE)) {
                v /= 1000;
                unitScaleIndex--;
            }
            while ((Math.abs(v) < 0.1) && (unitScaleIndex < UnitScale.UNIT_END)) {
                v *= 1000;
                unitScaleIndex++;
            }

            // if number still out of range, adjust decimal formatting
            if (Math.abs(v) < 0.1) {
                int maxDecimals = 3;
                double v2 = Math.abs(v);
                while (v2 < 0.1) {
                    maxDecimals++;
                    v2 *= 10;
                }
                numberFormatPostFix.setMaximumFractionDigits(maxDecimals);
            }
        }
        UnitScale u = UnitScale.findFromIndex(unitScaleIndex);
		String result = numberFormatPostFix.format(v);
		return result + u.getPostFix();
	}

    private static NumberFormat numberFormatSpecific = null;

    /**
     * Method to convert a double to a string.
     * If the double has no precision past the decimal, none will be shown.
     * @param v the double value to format.
     * @return the string representation of the number.
     */
    public static String formatDouble(double v)
    {
        return formatDouble(v, 3);
    }

	/**
	 * Method to convert a double to a string.
     * It will show up to 'numFractions' digits past the decimal point if numFractions is greater
     * than zero. If numFractions is 0, it will show infinite (as far as doubles go) precision.
     * If the double has no precision past the decimal, none will be shown.
     * This method is now thread safe.
	 * @param v the double value to format.
	 * @param numFractions the number of digits to the right of the decimal point.
	 * @return the string representation of the number.
	 */
	public static synchronized String formatDouble(double v, int numFractions)
	{
		if (numberFormatSpecific == null) {
            numberFormatSpecific = NumberFormat.getInstance(Locale.US);
            if (numberFormatSpecific != null) numberFormatSpecific.setGroupingUsed(false);
            try {
                DecimalFormat d = (DecimalFormat)numberFormatSpecific;
//                DecimalFormat d = (DecimalFormat)numberFormatPostFix;
                d.setDecimalSeparatorAlwaysShown(false);
            } catch (Exception e) {}

        }
        if (numFractions == 0) {
            numberFormatSpecific.setMaximumFractionDigits(340);
        } else {
            numberFormatSpecific.setMaximumFractionDigits(numFractions);
        }
		return numberFormatSpecific.format(v);
	}

	private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd, yyyy HH:mm:ss");

	/**
	 * Method to convert a Date to a String using local TimeZone.
	 * @param date the date to format.
	 * @return the string representation of the date.
	 */
	public static String formatDate(Date date)
	{
		return simpleDateFormat.format(date);
	}

	// SMR removed this method because the initialization of the "PST" timezone only works in the USA

//	private static SimpleDateFormat simpleDateFormatGMT = new SimpleDateFormat("EEE MMMM dd, yyyy HH:mm:ss zzz");
//	static { simpleDateFormatGMT.setTimeZone(TimeZone.getTimeZone("GMT")); }
//	/**
//	 * Method to convert a Date to a String using GMT TimeZone.
//	 * @param date the date to format.
//	 * @return the string representation of the date.
//	 */
//	public static String formatDateGMT(Date date)
//	{
//		return simpleDateFormatGMT.format(date);
//	}

	/**
	 * Method to converts a floating point number into engineering units such as pico, micro, milli, etc.
	 * @param value floating point value to be converted to engineering notation.
	 */
	public static String convertToEngineeringNotation(double value)
	{
		return convertToEngineeringNotation(value, "", 9999);
	}

	/**
	 * Method to converts a floating point number into engineering units such as pico, micro, milli, etc.
	 * @param value floating point value to be converted to engineering notation.
	 * @param unit a unit string to append to the result (null for none).
	 */
	public static String convertToEngineeringNotation(double value, String unit)
	{
		return convertToEngineeringNotation(value, unit, 9999);
	}

	/**
	 * Method to converts a floating point number into engineering units such as pico, micro, milli, etc.
	 * @param time floating point value to be converted to engineering notation.
	 * @param unit a unit string to append to the result (null for none).
	 * @param precpower decimal power of necessary time precision.
	 * Use a very large number to ignore this factor (9999).
	 */
	private static class ConversionRange
	{
		String postfix;
		int power;
		double scale;
		ConversionRange(String p, int pow, double s) { postfix = p;   power = pow;   scale = s; }
	}
	private static ConversionRange [] allRanges = new ConversionRange[]
    {
		// Although the extremes (yocto and yotta) are defined in the literature,
		// they aren't common in circuits (at this time) and so they are commented out.
		// Add them and more as their use in circuitry becomes common.
//		new ConversionRange("y", -24, 1.0E26),		// yocto
		new ConversionRange("z", -21, 1.0E23),		// zepto
		new ConversionRange("a", -18, 1.0E20),		// atto
		new ConversionRange("f", -15, 1.0E17),		// femto
		new ConversionRange("p", -12, 1.0E14),		// pico
		new ConversionRange("n",  -9, 1.0E11),		// nano
		new ConversionRange("u",  -6, 1.0E8),		// micro
		new ConversionRange("m",  -3, 1.0E5),		// milli
		new ConversionRange("",    0, 1.0E2),		// no scale
		new ConversionRange("k",   3, 1.0E-1),		// kilo
		new ConversionRange("M",   6, 1.0E-4),		// mega
		new ConversionRange("G",   9, 1.0E-7),		// giga
		new ConversionRange("T",  12, 1.0E-10),		// tera
		new ConversionRange("P",  15, 1.0E-13),		// peta
		new ConversionRange("E",  18, 1.0E-16),		// exa
		new ConversionRange("Z",  21, 1.0E-19)		// zetta
//		new ConversionRange("Y",  24, 1.0E-22)		// yotta
    };
	private static final double LOOKS_LIKE_ZERO = 1.0 / (allRanges[0].scale * 1.0E5);
	private static final double SMALLEST_JUST_PRINT = 1.0 / (allRanges[0].scale * 10.0);
	private static final double LARGEST_JUST_PRINT = 1.0 / allRanges[allRanges.length-1].scale * 1.0E5;

	public static String convertToEngineeringNotation(double time, String unit, int precpower)
	{
		String negative = "";
		if (time < 0.0)
		{
			negative = "-";
			time = -time;
		}
		String unitPostfix = unit;
		if (unitPostfix == null) unitPostfix = "";

		// if the value is too tiny, just call it zero
		if (time < LOOKS_LIKE_ZERO) return "0" + unitPostfix;

		// if the value is out of range, use normal formatting for it
		if (time < SMALLEST_JUST_PRINT || time >= LARGEST_JUST_PRINT)
			return negative + TextUtils.formatDouble(time) + unitPostfix;

		// get proper time unit to use
		String secType = "";
		int scalePower = 0;
		long intTime = 0;
		double scaled = 0;
		for(int i=0; i<allRanges.length; i++)
		{
			scaled = time * allRanges[i].scale;
			intTime = Math.round(scaled);
			if (i == allRanges.length-1 || (scaled < 200000.0 && intTime < 100000))
			{
				if (unit == null)
				{
					if (allRanges[i].power != 0)
						secType += "e" + allRanges[i].power;
				} else
					secType += allRanges[i].postfix + unitPostfix;
				scalePower = allRanges[i].power;
				break;
			}			
		}

		if (precpower >= scalePower)
		{
			long timeleft = intTime / 100;
			long timeright = intTime % 100;
			if (timeright == 0)
			{
				return negative + timeleft + secType;
			} else
			{
				if ((timeright%10) == 0)
				{
					return negative + timeleft + "." + timeright/10 + secType;
				} else
				{
					String tensDigit = "";
					if (timeright < 10) tensDigit = "0";
					return negative + timeleft + "." + tensDigit + timeright + secType;
				}
			}
		}
		scaled /= 1.0E2;
		String numPart = TextUtils.formatDouble(scaled, scalePower - precpower);
		if (numPart.indexOf('.') >= 0)
		{
			while (numPart.endsWith("0")) numPart = numPart.substring(0, numPart.length()-1);
			if (numPart.endsWith(".")) numPart = numPart.substring(0, numPart.length()-1);
		}
		return negative + numPart + secType;
	}

	/**
	 * Method to convert an integer to a string that is left-padded with spaces
	 * @param value the integer value.
	 * @param width the minimum field width.
	 * If the result is less than this, extra spaces are added to the beginning.
	 * @return a string describing the integer.
	 */
	public static String toBlankPaddedString(int value, int width)
	{
		String msg = Integer.toString(value);
		while (msg.length() < width) msg = " " + msg;
		return msg;
	}

	/**
	 * Method to convert a double to a string that is left-padded with spaces
	 * @param value the double value.
	 * @param width the minimum field width.
	 * If the result is less than this, extra spaces are added to the beginning.
	 * @return a string describing the double.
	 */
	public static String toBlankPaddedString(double value, int width)
	{
		String msg = Double.toString(value);
		while (msg.length() < width) msg = " " + msg;
		return msg;
	}

	/**
	 * Method to determine whether or not a string is a number.
	 * This method allows hexadecimal numbers as well as those with exponents.
	 * @param pp the string to test.
	 * @return true if it is a number.
	 */
	public static boolean isANumber(String pp)
	{
		// ignore the minus sign
		int i = 0;
		int len = pp.length();
		if (i < len && (pp.charAt(i) == '+' || pp.charAt(i) == '-')) i++;

		// special case for hexadecimal prefix
		boolean xflag = false;
		if (i < len-1 && pp.charAt(i) == '0' && (pp.charAt(i+1) == 'x' || pp.charAt(i+1) == 'X'))
		{
			i += 2;
			xflag = true;
		}

		boolean founddigits = false;
		if (xflag)
		{
			while (i < len && (TextUtils.isDigit(pp.charAt(i)) ||
				pp.charAt(i) == 'a' || pp.charAt(i) == 'A' ||
				pp.charAt(i) == 'b' || pp.charAt(i) == 'B' ||
				pp.charAt(i) == 'c' || pp.charAt(i) == 'C' ||
				pp.charAt(i) == 'd' || pp.charAt(i) == 'D' ||
				pp.charAt(i) == 'e' || pp.charAt(i) == 'E' ||
				pp.charAt(i) == 'f' || pp.charAt(i) == 'F'))
			{
				i++;
				founddigits = true;
			}
		} else
		{
			while (i < len && (TextUtils.isDigit(pp.charAt(i)) || pp.charAt(i) == '.'))
			{
				if (pp.charAt(i) != '.') founddigits = true;
				i++;
			}
		}
		if (!founddigits) return false;
		if (i == len) return true;

		// handle exponent of floating point numbers
		if (xflag) return false;
		if (pp.charAt(i) != 'e' && pp.charAt(i) != 'E') return false;
		i++;
		if (i == len) return false;
		if (pp.charAt(i) == '+' || pp.charAt(i) == '-') i++;
		if (i == len) return false;
		while (i < len && TextUtils.isDigit(pp.charAt(i))) i++;
		if (i == len) return true;

		return false;
	}

	/**
	 * Method to describe a time value as a String.
	 * @param milliseconds the time span in milli-seconds.
	 * @return a String describing the time span with the
	 * format: days : hours : minutes : seconds
	 */
	public static String getElapsedTime(long milliseconds)
	{
		if (milliseconds < 60000)
		{
			// less than a minute: show fractions of a second
			return (milliseconds / 1000.0) + " secs";
		}
		StringBuffer buf = new StringBuffer();
		int seconds = (int)milliseconds/1000;
		if (seconds < 0) seconds = 0;
		int days = seconds/86400;
		if (days > 0) buf.append(days + " days, ");
		seconds = seconds - (days*86400);
		int hours = seconds/3600;
		if (hours > 0) buf.append(hours + " hrs, ");
		seconds = seconds - (hours*3600);
		int minutes = seconds/60;
		if (minutes > 0) buf.append(minutes + " mins, ");
		seconds = seconds - (minutes*60);
		buf.append(seconds + " secs");
		return buf.toString();
	}

	/**
	 * Method to find a string inside of another string.
	 * @param string the main string being searched.
	 * @param search the string being located in the main string.
	 * @param startingPos the starting position in the main string to look (0 to search the whole string).
	 * @param caseSensitive true to do a case-sensitive search.
	 * @param reverse true to search from the back of the string.
	 * @return the position of the search string.  Returns negative if the string is not found.
	 */
	public static int findStringInString(String string, String search, int startingPos, boolean caseSensitive, boolean reverse)
	{
		if (caseSensitive)
		{
			// case-sensitive search
			int i = 0;
			if (reverse) i = string.lastIndexOf(search, startingPos); else
				i = string.indexOf(search, startingPos);
			return i;
		}

		// case-insensitive search
		if (startingPos > 0) string = string.substring(startingPos);
		String stringLC = canonicString(string);
		String searchLC = canonicString(search);
		int i = 0;
		if (reverse) i = stringLC.lastIndexOf(searchLC); else
			i = stringLC.indexOf(searchLC);
		if (i >= 0) i += startingPos;
		return i;
	}

	/**
	 * Method to break a line into keywords, separated by white space or comma
	 * @param line the string to tokenize.
     * @param delim the delimiters.
	 * @return an array of Strings for each keyword on the line.
	 */
	public static String [] parseString(String line, String delim)
	{
		StringTokenizer st = new StringTokenizer(line, delim);
		int total = st.countTokens();
		String [] strings = new String[total];
		for(int i=0; i<total; i++)
			strings[i] = st.nextToken().trim();
		return strings;
	}

	/**
	 * Unit is a typesafe enum class that describes a unit scale (metric factors of 10).
	 */
	public static class UnitScale
	{
		private final String name;
//		private final String description;
		private final int index;
        private final String postFix;
        private final Number multiplier;

		private UnitScale(String name, String description, int index, String postFix, Number multiplier)
		{
			this.name = name;
//			this.description = description;
			this.index = index;
            this.postFix = postFix;
            this.multiplier = multiplier;
		}

		/**
		 * Method to return the name of this UnitScale.
		 * The name can be prepended to a type, for example the name "Milli" can be put in front of "Meter".
		 * @return the name of this UnitScale.
		 */
		public String getName() { return name; }

		/**
		 * Method to convert this UnitScale to an integer.
		 * Used when storing these as preferences.
		 * @return the index of this UnitScale.
		 */
		public int getIndex() { return index; }

        /**
         * Get the string representing the postfix associated with this unit scale
         * @return the post fix string
         */
        public String getPostFix() { return postFix; }

        /**
         * Get the multiplier value associated with this unit scale.
         * @return the multiplier. May be an Integer (values >= 1) or a Double (values <= 1)
         */
        public Number getMultiplier() { return multiplier; }

		/**
		 * Method to convert the index value to a UnitScale.
		 * Used when storing these as preferences.
		 * @param index the index of the UnitScale.
		 * @return the indexed UnitScale.
		 */
		public static UnitScale findFromIndex(int index) { return allUnits[index - UNIT_BASE]; }

		/**
		 * Method to return a list of all scales.
		 * @return an array of all scales.
		 */
		public static UnitScale [] getUnitScales() { return allUnits; }

		/**
		 * Returns a printable version of this Unit.
		 * @return a printable version of this Unit.
		 */
		public String toString() { return name; }

		/** The largest unit value. */					private static final int UNIT_BASE =  -3;
        /** The smallest unit value. */                 private static final int UNIT_END = 5;
		/** Describes giga scale (1 billion). */		public static final UnitScale GIGA =  new UnitScale("Giga",  "giga:  x 1000000000", -3, "G", new Integer(1000000000));
		/** Describes mega scale (1 million). */		public static final UnitScale MEGA =  new UnitScale("Mega",  "mega:  x 1000000",    -2, "meg", new Integer(1000000));
		/** Describes kilo scale (1 thousand). */		public static final UnitScale KILO =  new UnitScale("Kilo",  "kilo:  x 1000",       -1, "k", new Integer(1000));
		/** Describes unit scale (1). */				public static final UnitScale NONE =  new UnitScale("",      "-:     x 1",           0, "", new Integer(1));
		/** Describes milli scale (1 thousandth). */	public static final UnitScale MILLI = new UnitScale("Milli", "milli: x 10 ^ -3",     1, "m", new Double(0.001));
		/** Describes micro scale (1 millionth). */		public static final UnitScale MICRO = new UnitScale("Micro", "micro: x 10 ^ -6",     2, "u", new Double(0.000001));
		/** Describes nano scale (1 billionth). */		public static final UnitScale NANO =  new UnitScale("Nano",  "nano:  x 10 ^ -9",     3, "n", new Double(0.000000001));
		/** Describes pico scale (10 to the -12th). */	public static final UnitScale PICO =  new UnitScale("Pico",  "pico:  x 10 ^ -12",    4, "p", new Double(0.000000000001));
		/** Describes femto scale (10 to the -15th). */	public static final UnitScale FEMTO = new UnitScale("Femto", "femto: x 10 ^ -15",    5, "f", new Double(0.000000000000001));
		/** Describes atto scale (10 to the -18th). */	public static final UnitScale ATTO  = new UnitScale("Atto",  "atto:  x 10 ^ -18",    6, "a", new Double(0.000000000000000001));
		/** Describes zepto scale (10 to the -21st). */	public static final UnitScale ZEPTO = new UnitScale("Zepto", "zepto: x 10 ^ -21",    7, "z", new Double(0.000000000000000000001));
		/** Describes yocto scale (10 to the -24th). */	public static final UnitScale YOCTO = new UnitScale("Yocto", "yocto: x 10 ^ -24",    8, "y", new Double(0.000000000000000000000001));

		private final static UnitScale [] allUnits =
		{
			GIGA, MEGA, KILO, NONE, MILLI, MICRO, NANO, PICO, FEMTO, ATTO, ZEPTO, YOCTO
		};
	}

	/**
	 * Method to convert a database coordinate into real spacing.
	 * @param value the database coordinate to convert.
	 * @param tech the technology to use for conversion (provides a real scaling).
	 * @param unitScale the type of unit desired.
	 * @return the database coordinate in the desired units.
	 * For example, if the given technology has a scale of 200 nanometers per unit,
	 * and the value 7 is given, then that is 1.4 microns (1400 nanometers).
	 * If the desired units are UnitScale.MICRO, then the returned value will be 1.4.
	 */
	public static double convertDistance(double value, Technology tech, UnitScale unitScale)
	{
		double scale = tech.getScale();
		double distanceScale = 0.000000001 / unitScale.getMultiplier().doubleValue() * scale;
		return value * distanceScale;
	}

	/**
	 * Method to convert real spacing into a database coordinate.
	 * @param value the real distance to convert.
	 * @param tech the technology to use for conversion (provides a real scaling).
	 * @param unitScale the type of unit desired.
	 * @return the real spacing in the database units.
	 * For example, if the given technology has a scale of 200 nanometers per unit,
	 * and the value 1.6 is given with the scale UnitScale.MICRO, then that is 1.6 microns (1600 nanometers).
	 * Since the technology has 200 nanometers per unit, this converts to 8 units.
	 */
	public static double convertFromDistance(double value, Technology tech, UnitScale unitScale)
	{
		double scale = tech.getScale();
		double distanceScale = 0.000000001 / unitScale.getMultiplier().doubleValue() * scale;
		return value / distanceScale;
	}

	/**
	 * Method to express "value" as a string in "unittype" electrical units.
	 * The scale of the units is in "unitscale".
	 */
//	public static String displayedUnits(double value, TextDescriptor.Unit unitType, UnitScale unitScale)
//	{
//        String postFix = "";
//		if (unitScale == UnitScale.GIGA)
//		{
//			value /= 1000000000.0f;
//			postFix = "g";
//		} else if (unitScale == UnitScale.MEGA)
//		{
//			value /= 1000000.0f;
//			postFix = "meg";		// SPICE wants "x"
//		} else if (unitScale == UnitScale.KILO)
//		{
//			value /= 1000.0f;
//			postFix = "k";
//		} else if (unitScale == UnitScale.MILLI)
//		{
//			value *= 1000.0f;
//			postFix = "m";
//		} else if (unitScale == UnitScale.MICRO)
//		{
//			value *= 1000000.0f;
//			postFix = "u";
//		} else if (unitScale == UnitScale.NANO)
//		{
//			value *= 1000000000.0f;
//			postFix = "n";
//		} else if (unitScale == UnitScale.PICO)
//		{
//			value *= 1000000000000.0f;
//			postFix = "p";
//		} else if (unitScale == UnitScale.FEMTO)
//		{
//			value *= 1000000000000000.0f;
//			postFix = "f";
//		}
//		return value + postFix;
////        return formatDoublePostFix(value);
//	}

	/**
	 * Method to convert a floating point value to a string, given that it is a particular type of unit.
	 * Each unit has a default scale.  For example, if Capacitance value 0.0000012 is being converted, and
	 * Capacitance is currently using microFarads, then the result will be "1.2m".
	 * If, however, capacitance is currently using milliFarads, the result will be 0.0012u".
	 * @param value the floating point value.
	 * @param units the type of unit.
	 * @return a string describing that value in the current unit.
	 */
	public static String makeUnits(double value, TextDescriptor.Unit units)
	{
		if (units == TextDescriptor.Unit.NONE)
		{
			// SMR removed the 2nd parameter to show only 3 digits
//			return formatDouble(value, 0);
			return formatDouble(value);
		}
//		if (units == TextDescriptor.Unit.RESISTANCE)
//			return displayedUnits(value, units, User.getResistanceUnits());
//		if (units == TextDescriptor.Unit.CAPACITANCE)
//			return displayedUnits(value, units, User.getCapacitanceUnits());
//		if (units == TextDescriptor.Unit.INDUCTANCE)
//			return displayedUnits(value, units, User.getInductanceUnits());
//		if (units == TextDescriptor.Unit.CURRENT)
//			return displayedUnits(value, units, User.getAmperageUnits());
//		if (units == TextDescriptor.Unit.VOLTAGE)
//			return displayedUnits(value, units, User.getVoltageUnits());
//		if (units == TextDescriptor.Unit.TIME)
//			return displayedUnits(value, units, User.getTimeUnits());
        return (formatDoublePostFix(value));
//		// shouldn't get here
//		return "?";
	}

    /**
     * Try to parse the user input String s into a Number. Conversion into the following formats
     * is tried in order. If a conversion is successful, that object is returned.
     * If no conversions are successful, this throws a NumberFormatException.
     * This method removes any UnitScale postfix, and scales the number accordingly.
     * No characters in the string are ignored - the string in its entirety (sans removed postfix) must be
     * able to be parsed into the Number by the usual Integer.parseInt(), Double.parseDouble() methods.
     * <P>Formats: Integer, Long, Double
     * @param s the string to parse
     * @return a Number that represents the string in its entirety
     * @throws NumberFormatException if the String is not a parsable Number.
     */
    public static Number parsePostFixNumber(String s) throws NumberFormatException {
        // remove character denoting multiplier at end, if any

        // remove commas that denote 1000's separators
        s = s.replaceAll(",", "");

        Number n = null;                                    // the number
        Number m = null;                                    // the multiplier

        for (int i=0; i<UnitScale.allUnits.length; i++) {
            UnitScale u = UnitScale.allUnits[i];

            String postfix = u.getPostFix();
            if (postfix.equals("")) continue;               // ignore the NONE suffix case
            if (postfix.length() >= s.length()) continue;   // postfix is same length or longer than string

            String sSuffix = s.substring(s.length()-postfix.length(), s.length());

            if (sSuffix.equalsIgnoreCase(postfix)) {
                m = u.getMultiplier();
                String sub = s.substring(0, s.length()-postfix.length());
                // try to converst substring to a number
                try {
                    n = parseNumber(sub);
                    break;
                } catch (NumberFormatException e) {
                    m = null;                           // try again
                }
            }
        }

        // if no valid postfix found, just parse number
        if (n == null) n = parseNumber(s);

        if (m != null) {
            if ((m instanceof Integer) && (m.intValue() == 1)) return n;

            if ((n instanceof Integer) && (m instanceof Integer)) {
                return new Integer(n.intValue() * m.intValue());
            }
            if ((n instanceof Long) && (m instanceof Integer)) {
                return new Long(n.longValue() * m.longValue());
            }
            return new Double(n.doubleValue() * m.doubleValue());
        }
        return n;
    }

    /**
     * Try to parse the user input String s into a Number. Conversion into the following formats
     * is tried in order. If a conversion is successful, that object is returned.
     * If no conversions are successful, this throws a NumberFormatException.
     * No characters in the string are ignored - the string in its entirety must be
     * able to be parsed into the Number by the usual Integer.parseInt(), Double.parseDouble() methods.
     * <P>Formats: Integer, Long, Double
     * @param s the string to parse
     * @return a Number that represents the string in its entirety
     * @throws NumberFormatException if the String is not a parsable Number.
     */
    private static Number parseNumber(String s) throws NumberFormatException {
        // try to do the conversion
        if (s.equals("1")) {
            Number nn = new Integer(1);
        }

        Number n = null;
        try {
            n = new Integer(s);
        } catch (NumberFormatException e) {
            // elib format does not know what a Long is
            //try {
            //    n = new Long(s);
            //} catch (NumberFormatException ee) {
                try {
                    n = new Double(s);
                } catch (NumberFormatException eee) {}
            //}
        }
        if (n == null) {
            NumberFormatException e = new NumberFormatException(s + "cannot be parsed into a Number");
            throw e;
        }
        return n;
    }

	/**
	 * Method to print a very long string.
	 * The string is broken sensibly.
	 */
	public static void printLongString(String str)
	{
		String prefix = "";
		while (str.length() > 80)
		{
			int i = 80;
			for( ; i > 0; i--) if (str.charAt(i) == ' ' || str.charAt(i) == ',') break;
			if (i <= 0) i = 80;
			if (str.charAt(i) == ',') i++;
			System.out.println(prefix + str.substring(0, i));
			if (str.charAt(i) == ' ') i++;
			str = str.substring(i);
			prefix = "   ";
		}
		System.out.println(prefix + str);
	}

//	/**
//	 * Method to compare two names and give a sort order.
//	 * The comparison considers numbers in numeric order so that the
//	 * string "in10" comes after the string "in9".
//	 *
//	 * Formal definition of order.
//	 * Lets insert in string's character sequence number at start of digit sequences.
//	 * Consider that numbers in the sequence are less than chars.
//	 * 
//	 * Examples below are in increasing order:
//	 *   ""           { }
//	 *   "0"          {  0, '0' }
//	 *   "9"          {  9, '9' }
//	 *   "10"         { 10, '1', '0' }
//	 *   "2147483648" { 2147483648, '2', '1', '4', '7', '4', '8', '3', '6', '4', '8' }
//	 *   " "          { ' ' }
//	 *   "-"          { '-' }
//	 *   "-1"         { '-', 1, '1' }
//	 *   "-2"         { '-', 2, '2' }
//	 *   "a"          { 'a' }
//	 *   "a0"         { 'a',  0, '0' }
//	 *   "a0-0"       { 'a',  0, '0', '-', 0, '0' }
//	 *   "a00"        { 'a',  0, '0', '0' }
//	 *   "a0a"        { 'a',  0, '0', 'a' }
//	 *   "a01"        { 'a',  1, '0', '1' }
//	 *   "a1"         { 'a',  1, '1' }
//	 *   "in"         { 'i', 'n' }
//	 *   "in1"        { 'i', 'n',  1, '1' }
//	 *   "in1a"       { 'i', 'n',  1, '1', 'a' }
//	 *   "in9"        { 'i', 'n',  9, '9' }
//	 *   "in10"       { 'i', 'n', 10, '1', '0' }
//	 *   "in!"        { 'i', 'n', '!' }
//	 *   "ina"        { 'i , 'n', 'a' }
//	 *   
//	 * @param name1 the first string.
//	 * @param name2 the second string.
//	 * @return 0 if they are equal, nonzero according to order.
//	 */
//	public static int nameSameNumeric(String name1, String name2) {
//        return STRING_NUMBER_ORDER.compare(name1, name2);
//    }
//
	/**
	 * Method to convert a file path to a URL.
	 * @param fileName the path to the file.
	 * @return the URL to that file (null on error).
	 */
	public static URL makeURLToFile(String fileName)
	{
		if (fileName.startsWith("file://")) fileName = fileName.substring(6);
		if (fileName.startsWith("file:/")) fileName = fileName.substring(5);
		File file = new File(fileName);
		try
		{
			return file.toURL();
		} catch (java.net.MalformedURLException e)
		{
			System.out.println("Cannot find file " + fileName);
		}
		return null;
	}

    /**
     * Get the file for the URL. The code
     * <code>
     * new File(url.getPath())
     * </code>
     * returns an illegal leading slash on windows,
     * and has forward slashes instead of back slashes.
     * This method generates the correct File using
     * <code>
     * new File(url.toURI())
     * </code>
     * <P>
     * use <code>getPath()</code> on the returned File
     * to get the correct String file path.
     * <P>
     * This should only be needed when running an external process under
     * windows with command line arguments containing file paths. Otherwise,
     * the Java IO code does the correct conversion.
     *
     * @param url the URL to convert to a File.
     * @return the File.  Will return null if
     * URL does not point to a file.
     */
    public static File getFile(URL url) {
        try {
            return new File(url.toURI());
        } catch (java.net.URISyntaxException e) {
            System.out.println("URL -> File conversion error: "+e.getMessage());
            return new File(url.getPath());
        } catch (java.lang.IllegalArgumentException e) {
            System.out.println("URL -> File conversion error: "+e.getMessage());
            return null;
        }
    }

	/**
	 * Method to return the directory path part of a URL (excluding the file name).
	 * For example, the URL "file:/users/strubin/gates.elib" has the directory part "/users/strubin/".
	 * @param url the URL to the file.
	 * @return the directory path part (including the trailing "/", ":", or "\").
	 * If there is no directory part, returns "".
	 */
	public static String getFilePath(URL url)
	{
		if (url == null) return "";
		String filePath = url.getFile();
        // special case of .delib files, which are directories, but we want them to appear as files
        File file = new File(filePath);
        if (file.getName().toLowerCase().endsWith(".delib")) {
            filePath = file.getPath();
        }
        //if (filePath.toLowerCase().endsWith(".delib"+File.separator))
        //    filePath = filePath.substring(0, filePath.length()-1);  // remove trailing '/'
		int backSlashPos = filePath.lastIndexOf('\\');
		int colonPos = filePath.lastIndexOf(':');
		int slashPos = filePath.lastIndexOf('/');
		int charPos = Math.max(backSlashPos, Math.max(colonPos, slashPos));
		if (charPos < 0) return "";
		return filePath.substring(0, charPos+1);
	}

	/**
	 * Method to return the pure file name of a URL.
	 * The pure file name excludes the directory path and the extension.
	 * It is used to find the library name from a URL.
	 * For example, the URL "file:/users/strubin/gates.elib" has the pure file name "gates".
	 * @param url the URL to the file.
	 * @return the pure file name.
	 */
	public static String getFileNameWithoutExtension(URL url)
	{
		String fileName = url.getFile();

		// special case if the library path came from a different computer system and still has separators
		while (fileName.endsWith("\\") || fileName.endsWith(":") || fileName.endsWith("/"))
			fileName = fileName.substring(0, fileName.length()-1);
		int backSlashPos = fileName.lastIndexOf('\\');
		int colonPos = fileName.lastIndexOf(':');
		int slashPos = fileName.lastIndexOf('/');
		int charPos = Math.max(backSlashPos, Math.max(colonPos, slashPos));
		if (charPos >= 0)
			fileName = fileName.substring(charPos+1);

		int dotPos = fileName.lastIndexOf('.');
		if (dotPos >= 0) fileName = fileName.substring(0, dotPos);

		// make sure the file name is legal
		StringBuffer buf = null;
		for (int i = 0; i < fileName.length(); i++)
		{
			char ch = fileName.charAt(i);
			if (ch == '\n' || ch == '|' || ch == ':' || ch == ' ' || ch == '{' || ch == '}' || ch == ';')
			{
				if (buf == null)
				{
					buf = new StringBuffer();
					buf.append(fileName.substring(0, i));
				}
				buf.append('-');
				continue;
			} else if (buf != null)
			{
				buf.append(ch);
			}
		}
		if (buf != null)
		{
			String newS = buf.toString();
			System.out.println("File name " + fileName + " was converted to " + newS);
			return newS;
		}
		return fileName;
	}

	/**
	 * Method to return the extension of the file in a URL.
	 * The extension is the part after the last dot.
	 * For example, the URL "file:/users/strubin/gates.elib" has the extension "elib".
	 * @param url the URL to the file.
	 * @return the extension of the file ("" if none).
	 */
	public static String getExtension(URL url)
	{
		if (url == null) return "";
		String fileName = url.getFile();
		int dotPos = fileName.lastIndexOf('.');
		if (dotPos < 0) return "";
		return fileName.substring(dotPos+1);
	}

	/**
	 * Method to open an input stream to a URL.
	 * @param url the URL to the file.
	 * @return the InputStream, or null if the file cannot be found.
	 */
	public static InputStream getURLStream(URL url)
	{
		return getURLStream(url, null);
	}

	/**
	 * Method to open an input stream to a URL.
	 * @param url the URL to the file.
	 * @param errorMsg a string buffer in which to print any error message. If null,
	 * any error message is printed to System.out
	 * @return the InputStream, or null if the file cannot be found.
	 */
	public static InputStream getURLStream(URL url, StringBuffer errorMsg)
	{
		if (url != null)
		{
			try
			{
				return url.openStream();
			} catch (IOException e) {
				if (errorMsg != null)
					errorMsg.append("Error: cannot open " + e.getMessage() + "\n");
				else
					System.out.println("Error: cannot open " + e.getMessage());
			}
		}
		return null;
	}

	/**
	 * Method to tell whether a given URL exists.
	 * @param url the URL in question.
	 * @return true if the file exists.
	 */
	public static boolean URLExists(URL url)
	{
		return URLExists(url, null);
	}

	/**
	 * Method to tell whether a given URL exists.
	 * @param url the URL in question.
	 * @param errorMsg a string buffer in which to print any error message.
	 * If null, errors are not printed.
	 * @return true if the file exists.
	 */
	public static boolean URLExists(URL url, StringBuffer errorMsg)
	{
		if (url == null) return false;

		try
		{
			URLConnection con = url.openConnection();
			con.connect();
			if (con.getContentLength() < 0) return false;
		} catch (Exception e)
		{
			if (errorMsg != null)
				errorMsg.append("Error: cannot open " + e.getMessage() + "\n");
			return false;
		}
		return true;
	}

	/****************************** FOR SORTING OBJECTS ******************************/

    /**
     * A comparator object for sorting Strings that may have numbers in them.
     * Created once because it is used often.
     */
    public static final Comparator<String> STRING_NUMBER_ORDER = new Comparator<String>() {
        /**
         * Method to compare two names and give a sort order.
         * The comparison considers numbers in numeric order so that the
         * string "in10" comes after the string "in9".
         *
         * Formal definition of order.
         * Lets insert in string's character sequence number at start of digit sequences.
         * Consider that numbers in the sequence are less than chars.
         *
         * Examples below are in increasing order:
         *   ""           { }
         *   "0"          {  0, '0' }
         *   "9"          {  9, '9' }
         *   "10"         { 10, '1', '0' }
         *   "2147483648" { 2147483648, '2', '1', '4', '7', '4', '8', '3', '6', '4', '8' }
         *   " "          { ' ' }
         *   "-"          { '-' }
         *   "-1"         { '-', 1, '1' }
         *   "-2"         { '-', 2, '2' }
         *   "a"          { 'a' }
         *   "a0"         { 'a',  0, '0' }
         *   "a0-0"       { 'a',  0, '0', '-', 0, '0' }
         *   "a00"        { 'a',  0, '0', '0' }
         *   "a0a"        { 'a',  0, '0', 'a' }
         *   "a01"        { 'a',  1, '0', '1' }
         *   "a1"         { 'a',  1, '1' }
         *   "a[1]"       { 'a', '[', 1, '1', ']' }
         *   "a[10]"      { 'a', '[', 10, '1', '0', ']' }
         *   "in"         { 'i', 'n' }
         *   "in1"        { 'i', 'n',  1, '1' }
         *   "in1a"       { 'i', 'n',  1, '1', 'a' }
         *   "in9"        { 'i', 'n',  9, '9' }
         *   "in10"       { 'i', 'n', 10, '1', '0' }
         *   "in!"        { 'i', 'n', '!' }
         *   "ina"        { 'i , 'n', 'a' }
         *
         * @param o1 the first string.
         * @param o2 the second string.
         * @return 0 if they are equal, nonzero according to order.
         */
    	public int compare(String name1, String name2) { 
            int len1 = name1.length();
            int len2 = name2.length();
            int extent = Math.min(len1, len2);
            for(int pos = 0; pos < extent; pos++) {
                char ch1 = name1.charAt(pos);
                char ch2 = name2.charAt(pos);
                if (ch1 != ch2) {
                    int digit1 = digit(ch1);
                    int digit2 = digit(ch2);
                    if (digit1 >= 0 || digit2 >= 0) {
                        int pos1 = pos + 1, pos2 = pos + 1; // Positions in string to compare
                        
                        // One char is digit, another is not. Is previous digit ?
                        int digit = pos > 0 ? digit(name1.charAt(--pos)) : -1;
                        if (digit < 0 && (digit1 < 0 || digit2 < 0)) {
                            // Previos is not digit. Number is less than non-number.
                            return digit2 - digit1;
                        }
                        // Are previus digits all zeros ?
                        while (digit == 0)
                            digit = pos > 0 ? digit(name1.charAt(--pos)) : -1;
                        if (digit < 0) {
                            // All previos digits are zeros. Skip zeros further.
                            while (digit1 == 0)
                                digit1 = pos1 < len1 ? digit(name1.charAt(pos1++)) : -1;
                            while (digit2 == 0)
                                digit2 = pos2 < len2 ? digit(name2.charAt(pos2++)) : -1;
                        }
                        
                        // skip matching digits
                        while (digit1 == digit2 && digit1 >= 0) {
                            digit1 = pos1 < len1 ? digit(name1.charAt(pos1++)) : -1;
                            digit2 = pos2 < len2 ? digit(name2.charAt(pos2++)) : -1;
                        }
                        
                        boolean dig1 = digit1 >= 0;
                        boolean dig2 = digit2 >= 0;
                        for (int i = 0; dig1 && dig2; i++) {
                            dig1 = pos1 + i < len1 && digit(name1.charAt(pos1 + i)) >= 0;
                            dig2 = pos2 + i < len2 && digit(name2.charAt(pos2 + i)) >= 0;
                        }
                        if (dig1 != dig2) return dig1 ? 1 : -1;
                        if (digit1 != digit2) return digit1 - digit2;
                    }
                    return ch1 - ch2;
                }
            }
            return len1 - len2;
        }
	};

    private static int digit(char ch) {
        if (ch < '\u0080')
            return ch >= '0' && ch <= '9' ? ch - '0' : -1;
        else
            return Character.digit((int)ch, 10);
    }

//	/**
//	 * Test of STRING_NUMBER_ORDER.
//	 */
// 	private static String[] numericStrings = {
// 		"",           // { }
// 		"0",          // {  0, '0' }
// 		"0-0",        // {  0, '0', '-', 0, '0' }
// 		"00",         // {  0, '0', '0' }
// 		"0a",         // {  0, '0', 'a' }
// 		"01",         // {  1, '0', '1' }
// 		"1",          // {  1, '1' }
// 		"9",          // {  9, '9' }
// 		"10",         // { 10, '1', '0' }
// 		"12",         // { 12, '1', '2' }
// 		"102",        // { 102, '1', '0', '2' }
// 		"2147483648", // { 2147483648, '2', '1', '4', '7', '4', '8', '3', '6', '4', '8' }
// 		" ",          // { ' ' }
// 		"-",          // { '-' }
// 		"-1",         // { '-', 1, '1' }
// 		"-2",         // { '-', 2, '2' }
// 		"a",          // { 'a' }
// 		"a0",         // { 'a',  0, '0' }
// 		"a0-0",       // { 'a',  0, '0', '-', 0, '0' }
// 		"a00",        // { 'a',  0, '0', '0' }
// 		"a0a",        // { 'a',  0, '0', 'a' }
// 		"a01",        // { 'a',  1, '0', '1' }
// 		"a1",         // { 'a',  1, '1' }
//        "a[1]",       // { 'a', '[', 1, '1', ']' }
//        "a[10]",      // { 'a', '[', 10, '1', '0', ']' }
// 		"in",         // { 'i', 'n' }
// 		"in1",        // { 'i', 'n',  1, '1' }
// 		"in1a",       // { 'i', 'n',  1, '1', 'a' }
// 		"in9",        // { 'i', 'n',  9, '9' }
// 		"in10",       // { 'i', 'n', 10, '1', '0' }
// 		"in!",        // { 'i', 'n', '!' }
// 		"ina"         // { 'i , 'n', 'a' }
// 	};
//
// 	static {
// 		for (int i = 0; i < numericStrings.length; i++)
// 		{
// 			for (int j = 0; j < numericStrings.length; j++)
// 			{
// 				String s1 = numericStrings[i];
// 				String s2 = numericStrings[j];
// 				int cmp = STRING_NUMBER_ORDER.compare(s1, s2);
// 				if (i == j && cmp != 0 || i < j && cmp >= 0 || i > j && cmp <= 0)
// 					System.out.println("Error in TextUtils.nameSameNumeric(\"" +
// 						s1 + "\", \"" + s2 + "\") = " + cmp);
// 			}
// 		}
// 	}

	/**
	 * Comparator class for sorting Objects by their string name.
	 */
    public static class ObjectsByToString implements Comparator<Object>
    {
		/**
		 * Method to sort Objects by their string name.
		 */
        public int compare(Object o1, Object o2)
        {
            String s1 = o1.toString();
            String s2 = o2.toString();
            return s1.compareToIgnoreCase(s2);
        }
    }

	/**
	 * Comparator class for sorting Cells by their view order.
	 */
	public static class CellsByView implements Comparator<Cell>
	{
		/**
		 * Method to sort Cells by their view order.
		 */
		public int compare(Cell c1, Cell c2)
        {
			View v1 = c1.getView();
			View v2 = c2.getView();
			return v1.getOrder() - v2.getOrder();
		}
	}

	/**
	 * Comparator class for sorting Cells by their version number.
	 */
	public static class CellsByVersion implements Comparator<Cell>
	{
		/**
		 * Method to sort Cells by their version number.
		 */
		public int compare(Cell c1, Cell c2)
		{
			return c2.getVersion() - c1.getVersion();
		}
	}

	/**
	 * Comparator class for sorting Cells by their date.
	 */
	public static class CellsByDate implements Comparator<Cell>
	{
		/**
		 * Method to sort Cells by their date.
		 */
		public int compare(Cell c1, Cell c2)
		{
			Date r1 = c1.getRevisionDate();
			Date r2 = c2.getRevisionDate();
			return r1.compareTo(r2);
		}
	}

	/**
	 * Comparator class for sorting Preferences by their name.
	 */
	public static class PrefsByName implements Comparator<Pref>
	{
		/**
		 * Method to sort Preferences by their name.
		 */
		public int compare(Pref p1, Pref p2)
		{
			String s1 = p1.getPrefName();
			String s2 = p2.getPrefName();
			return s1.compareToIgnoreCase(s2);
		}
	}

	/**
	 * Comparator class for sorting Networks by their name.
	 */
    public static class NetworksByName implements Comparator<Network>
    {
		/**
		 * Method to sort Networks by their name.
		 */
    	public int compare(Network n1, Network n2)
        {
            String s1 = n1.describe(false);
            String s2 = n2.describe(false);
            return s1.compareToIgnoreCase(s2);
        }
    }

    public static final Comparator<Connection> CONNECTIONS_ORDER = new Comparator<Connection>() {
        public int compare(Connection c1, Connection c2) {
			int i1 = c1.getPortInst().getPortProto().getPortIndex();
			int i2 = c2.getPortInst().getPortProto().getPortIndex();
			int cmp = i1 - i2;
			if (cmp != 0) return cmp;
			cmp = c1.getArc().getArcIndex() - c2.getArc().getArcIndex();
			if (cmp != 0) return cmp;
			return c1.getEndIndex() - c2.getEndIndex();
		}
    };

    /**
	 * Class to define the kind of text string to search
	 */
	public enum WhatToSearch {
		ARC_NAME("Arc Name"),
		ARC_VAR("Arc Variable"),
		NODE_NAME("Node Name"),
		NODE_VAR("Node Variable"),
		EXPORT_NAME("Export Name"),
		EXPORT_VAR("Export Variable"),
		CELL_VAR("Cell Name"),
		TEMP_NAMES(null);

		private String descriptionOfObjectFound;

		private WhatToSearch(String descriptionOfObjectFound) {
			this.descriptionOfObjectFound = descriptionOfObjectFound;
		}
		public String toString() {return descriptionOfObjectFound;}
	}
}

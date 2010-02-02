/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TextUtils.java
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

import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.Locale;

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
			char mainChr = Character.toLowerCase(main.charAt(i));
			char withChr = Character.toLowerCase(with.charAt(i));
			if (mainChr != withChr) return false;
		}
		return true;
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

}

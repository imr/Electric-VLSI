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

/**
 * This class is a collection of text utilities.
 */
public class TextUtils
{
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
		double v = 0;
		try
		{
			v = Double.parseDouble(text);
		} catch (NumberFormatException ex)
		{
			int start = 0;
			while (start < text.length() && text.charAt(start) == ' ') start++;
			int end = start;

			// allow initial + or -
			if (end < text.length() && (text.charAt(end) == '-' || text.charAt(end) == '+')) end++;

			// allow digits
			while (end < text.length() && Character.isDigit(text.charAt(end))) end++;

			// allow decimal point and digits beyond it
			if (end < text.length() && text.charAt(end) == '.')
			{
				end++;
				while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
			}

			// allow exponent
			if (end < text.length() && (text.charAt(end) == 'e' || text.charAt(end) == 'E'))
			{
				end++;
				if (end < text.length() && (text.charAt(end) == '-' || text.charAt(end) == '+')) end++;
				while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
			}

			if (end <= start) return 0;
			v = Double.parseDouble(text.substring(start, end-start));
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
	 *     If the number begins with "0x", presume base 16.
	 *     Otherwise presume base 10.
	 * <LI>This method can handle numbers that affect the sign bit.
	 *     If you give 0xFFFFFFFF to Integer.parseInt, you get a numberFormat exception.
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
		return atoi(s, 0);
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
		int base = 10;
		int num = 0;
		int sign = 1;
		int len = s.length();
		if (pos < len && s.charAt(pos) == '-')
		{
			pos++;
			sign = -1;
		}
		if (pos < len && s.charAt(pos) == '0')
		{
			pos++;
			base = 8;
			if (pos < len && (s.charAt(pos) == 'x' || s.charAt(pos) == 'X'))
			{
				pos++;
				base = 16;
			}
		}
		for(; pos < len; pos++)
		{
			char cat = s.charAt(pos);
			if ((cat >= 'a' && cat <= 'f') || (cat >= 'A' && cat <= 'F'))
			{
				if (base != 16) break;
				num = num * 16;
				if (cat >= 'a' && cat <= 'f') num += cat - 'a' + 10; else
					num += cat - 'A' + 10;
				continue;
			}
			if (!Character.isDigit(cat)) break;
			if (cat >= '8' && base == 8) break;
			num = num * base + cat - '0';
		}
		return(num * sign);
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
			while (i < len && (Character.isDigit(pp.charAt(i)) ||
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
			while (i < len && (Character.isDigit(pp.charAt(i)) || pp.charAt(i) == '.'))
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
		while (i < len && Character.isDigit(pp.charAt(i))) i++;
		if (i == len) return true;

		return false;
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

	/*
	 * Routine to compare two names "name1" and "name2" and return an
	 * integer giving their sorting order (0 if equal, nonzero according
	 * to order) in the same manner as any other string compare EXCEPT
	 * that it considers numbers properly, so that the string "in10" comes
	 * after the string "in9".
	 */
	public static int nameSameNumeric(String name1, String name2)
	{
		int len1 = name1.length();
		int len2 = name2.length();
		int extent = Math.min(len1, len2);
		for(int pos = 0; pos < extent; pos++)
		{
			char ch1 = name1.charAt(pos);
			char ch2 = name2.charAt(pos);
			if (Character.isDigit(ch1) && Character.isDigit(ch2))
			{
				// found a number: compare them numerically
				int value1 = TextUtils.atoi(name1.substring(pos));
				int value2 = TextUtils.atoi(name2.substring(pos));
				if (value1 != value2) return value1 - value2;
				while (pos < extent-1)
				{
					char nextCh1 = name1.charAt(pos+1);
					char nextCh2 = name2.charAt(pos+1);
					if (nextCh1 != nextCh2 || !Character.isDigit(nextCh1)) break;
					pos++;
				}					
				continue;
			}
			if (ch1 != ch2) return ch1 - ch2;
		}
		return len1 - len2;
	}

}

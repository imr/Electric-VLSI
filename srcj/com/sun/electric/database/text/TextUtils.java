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

import com.sun.electric.database.variable.TextDescriptor;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.net.URL;
import java.net.URLConnection;

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
				}
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

	private static NumberFormat numberFormat = null;

	/**
	 * Method to convert a double to a string, with a guaranteed number of digits to the right of the decimal point.
	 * @param v the double value to format.
	 * @param numFractions the number of digits to the right of the decimal point.
	 * @return the string representation of the number.
	 */
	public static String formatDouble(double v, int numFractions)
	{
		if (numberFormat == null) numberFormat = NumberFormat.getInstance();
		numberFormat.setMaximumFractionDigits(numFractions);
		numberFormat.setMinimumFractionDigits(numFractions);
		return numberFormat.format(v);
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

	/** Get a string representing elapsed time.
	 * format: days : hours : minutes : seconds */
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
	 * Unit is a typesafe enum class that describes a unit scale (metric factors of 10).
	 */
	public static class UnitScale
	{
		private final String name;

		private UnitScale(String name) { this.name = name; }

		/**
		 * Returns a printable version of this Unit.
		 * @return a printable version of this Unit.
		 */
		public String toString() { return name; }

		/** Describes no units. */				public static final UnitScale GIGA =  new UnitScale("giga:  x 1000000000");
		/** Describes resistance units. */		public static final UnitScale MEGA =  new UnitScale("mega:  x 1000000");
		/** Describes capacitance units. */		public static final UnitScale KILO =  new UnitScale("kilo:  x 1000");
		/** Describes inductance units. */		public static final UnitScale NONE =  new UnitScale("-:     x 1");
		/** Describes current units. */			public static final UnitScale MILLI = new UnitScale("milli: / 1000");
		/** Describes voltage units. */			public static final UnitScale MICRO = new UnitScale("micro: / 1000000");
		/** Describes distance units. */		public static final UnitScale NANO =  new UnitScale("nano:  / 1000000000");
		/** Describes time units. */			public static final UnitScale PICO =  new UnitScale("pico:  / 1000000000000");
		/** Describes time units. */			public static final UnitScale FEMTO = new UnitScale("femto: / 1000000000000000");
	}

	/**
	 * Method to express "value" as a string in "unittype" electrical units.
	 * The scale of the units is in "unitscale".
	 */
	public static String displayedUnits(double value, TextDescriptor.Unit unitType, UnitScale unitScale)
	{
		String postFix = "";
		if (unitScale == UnitScale.GIGA)
		{
			value /= 1000000000.0f;
			postFix = "g";
		} else if (unitScale == UnitScale.MEGA)
		{
			value /= 1000000.0f;
			postFix = "meg";		/* SPICE wants "x" */
		} else if (unitScale == UnitScale.KILO)
		{
			value /= 1000.0f;
			postFix = "k";
		} else if (unitScale == UnitScale.MILLI)
		{
			value *= 1000.0f;
			postFix = "m";
		} else if (unitScale == UnitScale.MICRO)
		{
			value *= 1000000.0f;
			postFix = "u";
		} else if (unitScale == UnitScale.NANO)
		{
			value *= 1000000000.0f;
			postFix = "n";
		} else if (unitScale == UnitScale.PICO)
		{
			value *= 1000000000000.0f;
			postFix = "p";
		} else if (unitScale == UnitScale.FEMTO)
		{
			value *= 1000000000000000.0f;
			postFix = "f";
		}
		return value + postFix;
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

	/**
	 * Method to convert a file path to a URL.
	 * @param fileName the path to the file.
	 * @return the URL to that file (null on error).
	 */
	public static URL makeURLToFile(String fileName)
	{
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
	 * Method to return the directory path part of a URL (excluding the file name).
	 * For example, the URL "file:/users/strubin/gates.elib" has the directory part "/users/strubin/".
	 * @param url the URL to the file.
	 * @return the directory path part (including the trailing "/").
	 * If there is no directory part, returns "".
	 */
	public static String getFilePath(URL url)
	{
		String filePath = url.getFile();
		int slashPos = filePath.lastIndexOf('/');
		if (slashPos < 0) return "";
		return filePath.substring(0, slashPos+1);
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
		int slashPos = fileName.lastIndexOf('/');
		if (slashPos >= 0)
		{
			fileName = fileName.substring(slashPos+1);
		}
		int dotPos = fileName.lastIndexOf('.');
		if (dotPos >= 0) fileName = fileName.substring(0, dotPos);
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
		if (url != null)
		{
			try
			{
				return url.openStream();
			} catch (IOException e) {}
		}
		return null;
	}
}

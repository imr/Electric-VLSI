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
import com.sun.electric.tool.user.User;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Iterator;

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
        return atof(text, null);
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
		double v = 0;
		try
		{
            Number n = parseUserInput(text);
			v = n.doubleValue();

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

			if (end <= start) {
                if (defaultVal != null) return defaultVal.doubleValue();
                return 0;
            }
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
	 * Method to convert a double to a string.
     * Also scales number and appends appropriate postfix UnitScale string.
	 * @param v the double value to format.
	 * @return the string representation of the number.
	 */
	public static String formatDouble(double v)
	{
		if (numberFormat == null) {
            numberFormat = NumberFormat.getInstance();
            try {
                DecimalFormat d = (DecimalFormat)numberFormat;
                d.setDecimalSeparatorAlwaysShown(false);
            } catch (Exception e) {}
        }
        numberFormat.setMaximumFractionDigits(3);
        int unitScaleIndex = 0;
        if (v != 0) {
            while ((Math.abs(v) > 1000000) && (unitScaleIndex > UnitScale.UNIT_BASE)) {
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
                numberFormat.setMaximumFractionDigits(maxDecimals);
            }
        }
        UnitScale u = UnitScale.findFromIndex(unitScaleIndex);
		String result = numberFormat.format(v);
		return result + u.getPostFix();
	}

	private static NumberFormat numberFormatSpecific = null;

	/**
	 * Method to convert a double to a string, with a guaranteed number of digits to the right of the decimal point.
	 * @param v the double value to format.
	 * @param numFractions the number of digits to the right of the decimal point.
	 * @return the string representation of the number.
	 */
	public static String formatDouble(double v, int numFractions)
	{
		if (numberFormatSpecific == null) numberFormatSpecific = NumberFormat.getInstance();
		numberFormatSpecific.setMaximumFractionDigits(numFractions);
		numberFormatSpecific.setMinimumFractionDigits(numFractions);
		return numberFormatSpecific.format(v);
	}

	private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMMM dd, yyyy HH:mm:ss");

	/**
	 * Method to convert a Date to a String.
	 * @param date the date to format.
	 * @return the string representation of the date.
	 */
	public static String formatDate(Date date)
	{
		return simpleDateFormat.format(date);
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
		String stringLC = string.toLowerCase();
		String searchLC = search.toLowerCase();
		int i = 0;
		if (reverse) i = stringLC.lastIndexOf(searchLC); else
			i = stringLC.indexOf(searchLC);
		if (i >= 0) i += startingPos;
		return i;
	}

	/**
	 * Unit is a typesafe enum class that describes a unit scale (metric factors of 10).
	 */
	public static class UnitScale
	{
		private final String name;
		private final int index;
        private final String postFix;
        private final Number multiplier;

		private UnitScale(String name, int index, String postFix, Number multiplier)
		{
			this.name = name;
			this.index = index;
            this.postFix = postFix;
            this.multiplier = multiplier;
		}

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
		 * Returns a printable version of this Unit.
		 * @return a printable version of this Unit.
		 */
		public String toString() { return name; }

		/** The largest unit value. */					private static final int UNIT_BASE =  -3;
        /** The smallest unit value. */                 private static final int UNIT_END = 5;
		/** Describes giga scale (1 billion). */		public static final UnitScale GIGA =  new UnitScale("giga:  x 1000000000",      -3, "G", new Integer(1000000000));
		/** Describes mega scale (1 million). */		public static final UnitScale MEGA =  new UnitScale("mega:  x 1000000",         -2, "meg", new Integer(1000000));
		/** Describes kilo scale (1 thousand). */		public static final UnitScale KILO =  new UnitScale("kilo:  x 1000",            -1, "k", new Integer(1000));
		/** Describes unit scale (1). */				public static final UnitScale NONE =  new UnitScale("-:     x 1",                0, "", new Integer(1));
		/** Describes milli scale (1 thousandth). */	public static final UnitScale MILLI = new UnitScale("milli: / 1000",             1, "m", new Double(0.001));
		/** Describes micro scale (1 millionth). */		public static final UnitScale MICRO = new UnitScale("micro: / 1000000",          2, "u", new Double(0.000001));
		/** Describes nano scale (1 billionth). */		public static final UnitScale NANO =  new UnitScale("nano:  / 1000000000",       3, "n", new Double(0.000000001));
		/** Describes pico scale (1 quadrillionth). */	public static final UnitScale PICO =  new UnitScale("pico:  / 1000000000000",    4, "p", new Double(0.000000000001));
		/** Describes femto scale (1 quintillionth). */	public static final UnitScale FEMTO = new UnitScale("femto: / 1000000000000000", 5, "f", new Double(0.000000000000001));

		private final static UnitScale [] allUnits =
		{
			GIGA, MEGA, KILO, NONE, MILLI, MICRO, NANO, PICO, FEMTO
		};
	}

	/**
	 * Method to express "value" as a string in "unittype" electrical units.
	 * The scale of the units is in "unitscale".
	 */
	public static String displayedUnits(double value, TextDescriptor.Unit unitType, UnitScale unitScale)
	{
/*		String postFix = "";
		if (unitScale == UnitScale.GIGA)
		{
			value /= 1000000000.0f;
			postFix = "g";
		} else if (unitScale == UnitScale.MEGA)
		{
			value /= 1000000.0f;
			postFix = "meg";		// SPICE wants "x"
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
        */
        return formatDouble(value);
	}

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
		if (units == TextDescriptor.Unit.RESISTANCE)
		{
			return displayedUnits(value, units, User.getResistanceUnits());
		} else if (units == TextDescriptor.Unit.CAPACITANCE)
		{
			return displayedUnits(value, units, User.getCapacitanceUnits());
		} else if (units == TextDescriptor.Unit.INDUCTANCE)
		{
			return displayedUnits(value, units, User.getInductanceUnits());
		} else if (units == TextDescriptor.Unit.CURRENT)
		{
			return displayedUnits(value, units, User.getAmperageUnits());
		} else if (units == TextDescriptor.Unit.VOLTAGE)
		{
			return displayedUnits(value, units, User.getVoltageUnits());
		} else if (units == TextDescriptor.Unit.TIME)
		{
			return displayedUnits(value, units, User.getTimeUnits());
		}
		return formatDouble(value);
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
    public static Number parseUserInput(String s) throws NumberFormatException {
        // remove character denoting multiplier at end, if any

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
        Number n = null;
        try {
            n = new Integer(s);
        } catch (NumberFormatException e) {}
        try {
            n = new Long(s);
        } catch (NumberFormatException e) {}
        try {
            n = new Double(s);
        } catch (NumberFormatException e) {}

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

	/**
	 * Method to compare two names and give a sort order.
	 * The comparison considers numbers in numeric order so that the
	 * string "in10" comes after the string "in9".
	 * @param name1 the first string.
	 * @param name2 the second string.
	 * @return 0 if they are equal, nonzero according to order.
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
		if (url == null) return "";
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
		} catch (IOException e)
		{
			if (errorMsg != null)
				errorMsg.append("Error: cannot open " + e.getMessage() + "\n");
			return false;
		}
		return true;
	}
}

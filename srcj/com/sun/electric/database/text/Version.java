/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Version.java
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

import com.sun.electric.tool.user.ActivityLogger;

import java.io.File;
import java.util.Date;
import java.util.StringTokenizer;
import java.text.DateFormat;

/**
 * A Version is a text-parsing object for Electric's version number.
 * Electric's current version has the form:<BR>
 * <CENTER>Major.Minor[Details]</CENTER><BR>
 * where <I>Major</I> is the major release number,
 * <I>Minor</I> is the minor release number within the major release,
 * and <I>Details</I> are sub-release values which can be letters or numbers.
 * <P>
 * If Details are omitted, then this is a full release of Electric.
 * If Details are present, then this is an interim release.
 * The Details can take two forms: letters or numbers with a dot in front.
 * <P>
 * Examples:
 *  6.03a
 *  7.04.1
 *  8.00
 */
public class Version
{
	/**
	 * This is the current version of Electric
	 */
	private static final String CURRENT = "8.01ac";
    private static final String ROOTARNAME = "electric";

	private int major;
	private int minor;
	private int details;

	/**
	 * Constructs a <CODE>Version</CODE> (cannot be called).
	 */
	private Version() {}

	/**
	 * Method to return author information
	 * @return Steve M. Rubin
	 */
	public static String getAuthorInformation() { return "Written by Steven M. Rubin"; }

	/**
	 * Method to return official name of Electric
	 * @return the official name
	 */
	public static String getApplicationInformation() { return "The Electric VLSI Design System"; }

	/**
	 * Method to return copyright information
	 * @return  Sun Microsystems and Static Free Software
	 */
	public static String getCopyrightInformation() { return "Copyright (c) 2004 Sun Microsystems and Static Free Software"; }

	/**
	 * Method to return a short description of warranty
	 * @return short description of warranty
	 */
	public static String getWarrantyInformation() { return "Electric comes with ABSOLUTELY NO WARRANTY"; }

	/**
	 * Method to return version and compilation date if available
	 * @return string containing version number and date of jar file
	 */
	public static String getVersionInformation()
	{
		String versionText =  "Version " + getVersion();
        String buildText = getBuildDate();
        if (buildText != null)
           versionText += " (built on " + buildText + ")";
		return (versionText);
	}

	/**
	 * Method to return the current Electric version.
	 * @return the current Electric version.
	 */
	public static String getVersion() { return CURRENT; }

    /**
     * Method to return build date of main jar file
     * @return string containing the date in short format
     */
    public static String getBuildDate()
    {
        // Might need to adjust token delim depending on operating system
        StringTokenizer parse = new StringTokenizer(System.getProperty("java.class.path"));
        String delim = System.getProperty("path.separator");
        try
        {
            while (parse.hasMoreElements())
            {
                String val = parse.nextToken(delim);
                // Find path for main jar
	            String filename = ROOTARNAME+"-"+getVersion()+".jar";
                if (val.lastIndexOf(filename) != -1)
                {
                    File electricJar = new File(val);
                    long date = electricJar.lastModified();
                    Date d = new Date(date);
                    return (DateFormat.getDateInstance().format(d));
                }
            }
        }
        catch (Exception e)
        {
            ActivityLogger.logException(e);
        }
        return (null);
    }

	/**
	 * Method to return the major part of a parsed Version number.
	 * @return the major part of a parsed Version number.
	 */
	public int getMajor() { return major; }

	/**
	 * Method to return the minor part of a parsed Version number.
	 * @return the minor part of a parsed Version number.
	 */
	public int getMinor() { return minor; }

	/**
	 * Method to return the details part of a parsed Version number.
	 * @return the details part of a parsed Version number.
	 */
	public int getDetail() { return details; }

	/**
	 * Method to parse the specified Version number and return a Version object.
	 * @param version the version of Electric.
	 * @return a Version object with the fields parsed.
	 */
	public static Version parseVersion(String version)
	{
		// create an object to hold the version results
		Version v = new Version();

		/* parse the version fields */
		int dot = version.indexOf('.');
		if (dot == -1)
		{
			// no "." in the string: version should be a pure number
			v.major = Integer.parseInt(version);
			v.minor = v.details = 0;
			return v;
		}

		// found the "." so parse the major version number (to the left of the ".")
		String majorString = version.substring(0, dot);
		String restOfString = version.substring(dot+1);
		v.major = Integer.parseInt(majorString);

		int letters;
		for(letters = 0; letters < restOfString.length(); letters++)
			if (!Character.isDigit(restOfString.charAt(letters))) break;
		String minorString = restOfString.substring(0, letters);
		v.minor = Integer.parseInt(minorString);
		restOfString = restOfString.substring(letters);
		
		// see what else follows the minor version number
		if (restOfString.length() == 0)
		{
			v.details = 0;
			return v;
		}
		if (restOfString.charAt(0) == '.')
		{
			v.details = Integer.parseInt(restOfString.substring(1));
		} else
		{
			v.details = 0;
			while (restOfString.length() > 0 &&
				Character.isLetter(restOfString.charAt(0)))
			{
				v.details = (v.details * 26) + Character.toLowerCase(restOfString.charAt(0)) - 'a' + 1;
				restOfString = restOfString.substring(1);
			}
		}
		return v;
	}
}

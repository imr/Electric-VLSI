package com.sun.electric.database.text;

import java.lang.Character;

public class Version
{
	int major;
	int minor;
	int details;

	private Version() {}
	
	public int getMajor() { return major; }
	public int getMinor() { return minor; }
	public int getDetail() { return details; }

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

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Users.java
 * Project management tool: user management
 * Written by: Steven M. Rubin
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.project;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This is the user management part of the Project Management tool.
 */
public class Users
{
	/**
	 * There are two levels of security: low and medium.
	 * Medium security manages a list of user names/passwords and requires logging-in.
	 * It is only "medium" becuase the passwords are badly encrypted, and it is easy to add user names.
	 * Low security simply uses the user's name without questioning it.
	 */
	public static final boolean LOWSECURITY    = true;
    private static final String PUSERFILE      = "projectusers";

	/** the users */							private static HashMap<String,String> usersMap;

	/**
	 * Method to return the number of users in the user database.
	 * @return the number of users in the user database.
	 */
	public static int getNumUsers()
	{
		ensureUserList();
		return usersMap.size();
	}

	/**
	 * Method to return an Iterator over the users in the user database.
	 * @return an Iterator over the users in the user database.
	 */
	public static Iterator<String> getUsers()
	{
		ensureUserList();
		return usersMap.keySet().iterator();
	}

	/**
	 * Method to tell whether a user name is in the user database.
	 * @param user the user name.
	 * @return true if the user name is in the user database.
	 */
	public static boolean isExistingUser(String user)
	{
		ensureUserList();
		return usersMap.get(user) != null;
	}

	/**
	 * Method to remove a user name from the user database.
	 * @param user the user name to remove from the user database.
	 */
	public static void deleteUser(String user)
	{
		usersMap.remove(user);
		saveUserList();
	}

	/**
	 * Method to add a user to the user database.
	 * @param user the user name to add.
	 * @param encryptedPassword the encrypted password for the user.
	 */
	public static void addUser(String user, String encryptedPassword)
	{
		usersMap.put(user, encryptedPassword);
		saveUserList();
	}

	/**
	 * Method to return the encrypted password associated with a given user.
	 * @param user the user name.
	 * @return the user's encrypted password (null if not found).
	 */
	public static String getEncryptedPassword(String user)
	{
		return usersMap.get(user);
	}

	/**
	 * Method to change a user's encrypted password.
	 * @param user the user name.
	 * @param newEncryptedPassword the new encrypted password for the user.
	 */
	public static void changeEncryptedPassword(String user, String newEncryptedPassword)
	{
		usersMap.put(user, newEncryptedPassword);
		saveUserList();
	}

	/************************ USER SUPPORT ***********************/

	/**
	 * Method to ensuer that there is a valid user name.
	 * @return true if there is NO valid user name (also displays error message).
	 */
	static boolean needUserName()
	{
		Project.pmActive = true;
		if (Project.getCurrentUserName().length() == 0)
		{
			if (LOWSECURITY)
			{
				Project.setCurrentUserName(System.getProperty("user.name"));
				return false;
			}
			Job.getUserInterface().showErrorMessage(
				"You must select a user first (in the 'Project Management' panel of the Preferences dialog)",
				"No Valid User Name");
			return true;
		}
		return false;
	}

	private static void ensureUserList()
	{
		if (usersMap == null)
		{
			usersMap = new HashMap<String,String>();
			String userFile = Project.getRepositoryLocation() + File.separator + PUSERFILE;
			URL url = TextUtils.makeURLToFile(userFile);
			try
			{
				URLConnection urlCon = url.openConnection();
				InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
				LineNumberReader lnr = new LineNumberReader(is);

				for(;;)
				{
					String userLine = lnr.readLine();
					if (userLine == null) break;
					int colonPos = userLine.indexOf(':');
					if (colonPos < 0)
					{
						System.out.println("Missing ':' in user file: " + userLine);
						break;
					}
					String userName = userLine.substring(0, colonPos);
					String encryptedPassword = userLine.substring(colonPos+1);
					usersMap.put(userName, encryptedPassword);
				}

				lnr.close();
			} catch (IOException e)
			{
				System.out.println("Creating new user database");
			}
		}
	}

	private static void saveUserList()
	{
		// write the file back
		String userFile = Project.getRepositoryLocation() + File.separator + PUSERFILE;
		try
		{
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(userFile)));

			for(String userName : usersMap.keySet())
			{
				String encryptedPassword = usersMap.get(userName);
				printWriter.println(userName + ":" + encryptedPassword);
			}

			printWriter.close();
			System.out.println("Wrote " + userFile);
		} catch (IOException e)
		{
			System.out.println("Error writing " + userFile);
			return;
		}
	}

	private static final int ROTORSZ = 256;		/* a power of two */
	private static final int MASK =   (ROTORSZ-1);
	/**
	 * Method to encrypt a string in the most simple of ways.
	 * A one-rotor machine designed along the lines of Enigma but considerably trivialized.
	 * @param text the text to encrypt.
	 * @return an encrypted version of the text.
	 */
	public static String encryptPassword(String text)
	{
		// first setup the machine
		String key = "BicIsSchediwy";
		String readable = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+-";
		int seed = 123;
		int keyLen = key.length();
		for (int i=0; i<keyLen; i++) seed = seed*key.charAt(i) + i;
		char [] t1 = new char[ROTORSZ];
		char [] t2 = new char[ROTORSZ];
		char [] t3 = new char[ROTORSZ];
		char [] deck = new char[ROTORSZ];
		for(int i=0; i<ROTORSZ; i++)
		{
			t1[i] = (char)i;
			t3[i] = 0;
			deck[i] = (char)i;
		}
		for(int i=0; i<ROTORSZ; i++)
		{
			seed = 5*seed + key.charAt(i%keyLen);
			int random = seed % 65521;
			int k = ROTORSZ-1 - i;
			int ic = (random&MASK) % (k+1);
			random >>= 8;
			int temp = t1[k];
			t1[k] = t1[ic];
			t1[ic] = (char)temp;
			if (t3[k] != 0) continue;
			ic = (random&MASK) % k;
			while (t3[ic] != 0) ic = (ic+1) % k;
			t3[k] = (char)ic;
			t3[ic] = (char)k;
		}
		for(int i=0; i<ROTORSZ; i++) t2[t1[i]&MASK] = (char)i;

		// now run the machine
		int n1 = 0;
		int n2 = 0;
		int nr2 = 0;
		StringBuffer result = new StringBuffer();
		for(int pt=0; pt<text.length(); pt++)
		{
			int nr1 = deck[n1]&MASK;
			nr2 = deck[nr1]&MASK;
			int i = t2[(t3[(t1[(text.charAt(pt)+nr1)&MASK]+nr2)&MASK]-nr2)&MASK]-nr1;
			result.append(readable.charAt(i&63));
			n1++;
			if (n1 == ROTORSZ)
			{
				n1 = 0;
				n2++;
				if (n2 == ROTORSZ) n2 = 0;
				shuffle(deck, key);
			}
		}
		String res = result.toString();
		return res;
	}

	private static void shuffle(char [] deck, String key)
	{
		int seed = 123;
		int keyLen = key.length();
		for(int i=0; i<ROTORSZ; i++)
		{
			seed = 5*seed + key.charAt(i%keyLen);
			int random = seed % 65521;
			int k = ROTORSZ-1 - i;
			int ic = (random&MASK) % (k+1);
			int temp = deck[k];
			deck[k] = deck[ic];
			deck[ic] = (char)temp;
		}
	}

}

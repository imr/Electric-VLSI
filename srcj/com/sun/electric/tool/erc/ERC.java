/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ERC.java
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
package com.sun.electric.tool.erc;

import com.sun.electric.database.text.Pref;
import com.sun.electric.tool.Tool;

/**
 * This is the Electrical Rule Checker tool.
 */
public class ERC extends Tool
{

	/** the ERC tool. */		public static ERC tool = new ERC();

	/**
	 * The constructor sets up the ERC tool.
	 */
	private ERC()
	{
		super("ERC");
	}

	/**
	 * Method to initialize the ERC tool.
	 */
	public void init()
	{
	}

	/****************************** OPTIONS ******************************/

	private static Pref cachePWellCheck = Pref.makeIntPref("PWellCheck", ERC.tool.prefs, 0);
	/**
	 * Method to tell how much P-Well contact checking the ERC should do.
	 * The values are:
	 * <UL>
	 * <LI>0: must have a contact in every well area.</LI>
	 * <LI>1: must have at least one contact.</LI>
	 * <LI>2: do not check for contact presence.</LI>
	 * </UL>
	 * The default is "0".
	 * @return how much P-Well contact checking the ERC should do.
	 */
	public static int getPWellCheck() { return cachePWellCheck.getInt(); }
	/**
	 * Method to set how much P-Well contact checking the ERC should do.
	 * @param c how much P-Well contact checking the ERC should do:
	 * <UL>
	 * <LI>0: must have a contact in every well area.</LI>
	 * <LI>1: must have at least one contact.</LI>
	 * <LI>2: do not check for contact presence.</LI>
	 * </UL>
	 */
	public static void setPWellCheck(int c) { cachePWellCheck.setInt(c); }

	private static Pref cacheMustConnectPWellToGround = Pref.makeBooleanPref("MustConnectPWellToGround", ERC.tool.prefs, true);
	/**
	 * Method to tell whether ERC should check that all P-Well contacts connect to ground.
	 * The default is "true".
	 * @return true if ERC should check that all P-Well contacts connect to ground.
	 */
	public static boolean isMustConnectPWellToGround() { return cacheMustConnectPWellToGround.getBoolean(); }
	/**
	 * Method to set whether ERC should check that all P-Well contacts connect to ground.
	 * @param on true if ERC should check that all P-Well contacts connect to ground.
	 */
	public static void setMustConnectPWellToGround(boolean on) { cacheMustConnectPWellToGround.setBoolean(on); }

	private static Pref cacheNWellCheck = Pref.makeIntPref("NWellCheck", ERC.tool.prefs, 0);
	/**
	 * Method to tell how much N-Well contact checking the ERC should do.
	 * The values are:
	 * <UL>
	 * <LI>0: must have a contact in every well area.</LI>
	 * <LI>1: must have at least one contact.</LI>
	 * <LI>2: do not check for contact presence.</LI>
	 * </UL>
	 * The default is "0".
	 * @return how much N-Well contact checking the ERC should do.
	 */
	public static int getNWellCheck() { return cacheNWellCheck.getInt(); }
	/**
	 * Method to set how much N-Well contact checking the ERC should do.
	 * @param c how much N-Well contact checking the ERC should do:
	 * <UL>
	 * <LI>0: must have a contact in every well area.</LI>
	 * <LI>1: must have at least one contact.</LI>
	 * <LI>2: do not check for contact presence.</LI>
	 * </UL>
	 */
	public static void setNWellCheck(int c) { cacheNWellCheck.setInt(c); }

	private static Pref cacheMustConnectNWellToPower = Pref.makeBooleanPref("MustConnectNWellToPower", ERC.tool.prefs, true);
	/**
	 * Method to tell whether ERC should check that all N-Well contacts connect to power.
	 * The default is "true".
	 * @return true if ERC should check that all N-Well contacts connect to power.
	 */
	public static boolean isMustConnectNWellToPower() { return cacheMustConnectNWellToPower.getBoolean(); }
	/**
	 * Method to set whether ERC should check that all N-Well contacts connect to power.
	 * @param on true if ERC should check that all N-Well contacts connect to power.
	 */
	public static void setMustConnectNWellToPower(boolean on) { cacheMustConnectNWellToPower.setBoolean(on); }

	private static Pref cacheFindWorstCaseWellContact = Pref.makeBooleanPref("FindWorstCaseWell", ERC.tool.prefs, false);
	/**
	 * Method to tell whether ERC should find the contact that is farthest from the well edge.
	 * The default is "false".
	 * @return true if ERC should find the contact that is farthest from the well edge.
	 */
	public static boolean isFindWorstCaseWell() { return cacheFindWorstCaseWellContact.getBoolean(); }
	/**
	 * Method to set whether ERC should find the contact that is farthest from the well edge.
	 * @param on true if ERC should find the contact that is farthest from the well edge.
	 */
	public static void setFindWorstCaseWell(boolean on) { cacheFindWorstCaseWellContact.setBoolean(on); }
}

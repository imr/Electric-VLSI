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
import com.sun.electric.technology.ArcProto;
import com.sun.electric.tool.Tool;

import java.util.HashMap;

/**
 * This is the Electrical Rule Checker tool.
 */
public class ERC extends Tool
{

	/** the ERC tool. */		protected static ERC tool = new ERC();
	/** Pref map for arc antenna ratio. */		private static HashMap<ArcProto,Pref> defaultAntennaRatioPrefs = new HashMap<ArcProto,Pref>();

	/**
	 * The constructor sets up the ERC tool.
	 */
	private ERC()
	{
		super("erc");
	}

	/**
	 * Method to initialize the ERC tool.
	 */
	public void init()
	{
	}

    /**
     * Method to retrieve singleton associated to ERC tool
     * @return the ERC tool.
     */
    public static ERC getERCTool() { return tool; }

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

	private static Pref cacheDRCCheck = Pref.makeBooleanPref("DRCCheckInERC", ERC.tool.prefs, false);
	/**
	 * Method to tell whether ERC should check DRC Spacing condition
	 * The default is "false".
	 * @return true if ERC should check DRC Spacing condition
	 */
	public static boolean isDRCCheck() { return cacheDRCCheck.getBoolean(); }
	/**
	 * Method to tell whether ERC should check DRC Spacing condition
	 * @param on true if ERC should check DRC Spacing condition
	 */
	public static void setDRCCheck(boolean on) { cacheDRCCheck.setBoolean(on); }

    /**** ANTENNA Preferences ***/
	private Pref getArcProtoAntennaPref(ArcProto ap)
	{
		Pref pref = defaultAntennaRatioPrefs.get(ap);
		if (pref == null)
		{
			double factory = ERCAntenna.DEFPOLYRATIO;
			if (ap.getFunction().isMetal()) factory = ERCAntenna.DEFMETALRATIO;
			pref = Pref.makeDoublePref("DefaultAntennaRatioFor" + ap.getName() + "IN" + ap.getTechnology().getTechName(), ERC.tool.prefs, factory);
			defaultAntennaRatioPrefs.put(ap, pref);
		}
		return pref;
	}

    /**
	 * Method to set the antenna ratio of this ArcProto.
	 * Antenna ratios are used in antenna checks that make sure the ratio of the area of a layer is correct.
	 * @param ratio the antenna ratio of this ArcProto.
	 */
	public void setAntennaRatio(ArcProto ap, double ratio) { getArcProtoAntennaPref(ap).setDouble(ratio); }

    /**
	 * Method to tell the antenna ratio of this ArcProto.
	 * Antenna ratios are used in antenna checks that make sure the ratio of the area of a layer is correct.
	 * @return the antenna ratio of this ArcProto.
	 */
	public double getAntennaRatio(ArcProto ap) { return getArcProtoAntennaPref(ap).getDouble(); }
}

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
package com.sun.electric.tool.erc;

import com.sun.electric.database.text.Pref;
import com.sun.electric.tool.Tool;

/**
 * This is the Electrical Rule Checker tool.
 */
public class ERC extends Tool
{
	/** the ERC tool. */					protected static ERC tool = new ERC();

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

	private static Pref cachePWellCheck = Pref.makeIntServerPref("PWellCheck", ERC.tool.prefs, 0);
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
	/**
	 * Method to tell how much P-Well contact checking the ERC should do, by default.
	 * The values are:
	 * <UL>
	 * <LI>0: must have a contact in every well area.</LI>
	 * <LI>1: must have at least one contact.</LI>
	 * <LI>2: do not check for contact presence.</LI>
	 * </UL>
	 * @return how much P-Well contact checking the ERC should do, by default.
	 */
	public static int getFactoryPWellCheck() { return cachePWellCheck.getIntFactoryValue(); }

	private static Pref cacheMustConnectPWellToGround = Pref.makeBooleanServerPref("MustConnectPWellToGround", ERC.tool.prefs, true);
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
	/**
	 * Method to tell whether ERC should check that all P-Well contacts connect to ground, by default.
	 * @return true if ERC should check that all P-Well contacts connect to ground, by default.
	 */
	public static boolean isFactoryMustConnectPWellToGround() { return cacheMustConnectPWellToGround.getBooleanFactoryValue(); }

	private static Pref cacheParallelWellAnalysis = Pref.makeBooleanServerPref("ParallelWellAnalysis", ERC.tool.prefs, true);
	/**
	 * Method to tell whether ERC should do well analysis using multiple processors.
	 * The default is "true".
	 * @return true if ERC should do well analysis using multiple processors.
	 */
	public static boolean isParallelWellAnalysis() { return cacheParallelWellAnalysis.getBoolean(); }
	/**
	 * Method to set whether ERC should do well analysis using multiple processors.
	 * @param on true if ERC should do well analysis using multiple processors.
	 */
	public static void setParallelWellAnalysis(boolean on) { cacheParallelWellAnalysis.setBoolean(on); }
	/**
	 * Method to tell whether ERC should do well analysis using multiple processors, by default.
	 * @return true if ERC should do well analysis using multiple processors, by default.
	 */
	public static boolean isFactoryParallelWellAnalysis() { return cacheParallelWellAnalysis.getBooleanFactoryValue(); }

	private static Pref cacheWellAnalysisNumProc = Pref.makeIntServerPref("WellAnalysisNumProc", ERC.tool.prefs, 0);
	/**
	 * Method to tell the number of processors to use in ERC well analysis.
	 * The default is "0" (as many as there are).
	 * @return the number of processors to use in ERC well analysis.
	 */
	public static int getWellAnalysisNumProc() { return cacheWellAnalysisNumProc.getInt(); }
	/**
	 * Method to set the number of processors to use in ERC well analysis.
	 * @param p the number of processors to use in ERC well analysis.
	 */
	public static void setWellAnalysisNumProc(int p) { cacheWellAnalysisNumProc.setInt(p); }
	/**
	 * Method to tell the number of processors to use in ERC well analysis, by default.
	 * @return the number of processors to use in ERC well analysis, by default.
	 */
	public static int getFactoryWellAnalysisNumProc() { return cacheWellAnalysisNumProc.getIntFactoryValue(); }

	private static Pref cacheNWellCheck = Pref.makeIntServerPref("NWellCheck", ERC.tool.prefs, 0);
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
	/**
	 * Method to tell how much N-Well contact checking the ERC should do by default.
	 * The values are:
	 * <UL>
	 * <LI>0: must have a contact in every well area.</LI>
	 * <LI>1: must have at least one contact.</LI>
	 * <LI>2: do not check for contact presence.</LI>
	 * </UL>
	 * @return how much N-Well contact checking the ERC should do by default.
	 */
	public static int getFactoryNWellCheck() { return cacheNWellCheck.getIntFactoryValue(); }

	private static Pref cacheMustConnectNWellToPower = Pref.makeBooleanServerPref("MustConnectNWellToPower", ERC.tool.prefs, true);
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
	/**
	 * Method to tell whether ERC should check that all N-Well contacts connect to power by default.
	 * @return true if ERC should check that all N-Well contacts connect to power by default.
	 */
	public static boolean isFactoryMustConnectNWellToPower() { return cacheMustConnectNWellToPower.getBooleanFactoryValue(); }

	private static Pref cacheFindWorstCaseWellContact = Pref.makeBooleanServerPref("FindWorstCaseWell", ERC.tool.prefs, false);
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
	/**
	 * Method to tell whether ERC should find the contact that is farthest from the well edge, by default.
	 * @return true if ERC should find the contact that is farthest from the well edge, by default.
	 */
	public static boolean isFactoryFindWorstCaseWell() { return cacheFindWorstCaseWellContact.getBooleanFactoryValue(); }

	private static Pref cacheDRCCheck = Pref.makeBooleanServerPref("DRCCheckInERC", ERC.tool.prefs, false);
	/**
	 * Method to tell whether ERC should check DRC Spacing condition.
	 * The default is "false".
	 * @return true if ERC should check DRC Spacing condition.
	 */
	public static boolean isDRCCheck() { return cacheDRCCheck.getBoolean(); }
	/**
	 * Method to tell whether ERC should check DRC Spacing condition.
	 * @param on true if ERC should check DRC Spacing condition.
	 */
	public static void setDRCCheck(boolean on) { cacheDRCCheck.setBoolean(on); }
	/**
	 * Method to tell whether ERC should check DRC Spacing condition by default.
	 * @return true if ERC should check DRC Spacing condition by default.
	 */
	public static boolean isFactoryDRCCheck() { return cacheDRCCheck.getBooleanFactoryValue(); }
}

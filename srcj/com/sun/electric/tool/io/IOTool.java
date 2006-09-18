/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IOTool.java
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
package com.sun.electric.tool.io;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Tool;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * This class manages reading files in different formats.
 * The class is subclassed by the different file readers.
 */
public class IOTool extends Tool
{
	/** the IO tool. */										private static IOTool tool = new IOTool();

	/** Varible key for true library of fake cell. */		public static final Variable.Key IO_TRUE_LIBRARY = Variable.newKey("IO_true_library");

	// ---------------------- private and protected methods -----------------

	/**
	 * The constructor sets up the I/O tool.
	 */
	protected IOTool()
	{
		super("io");
	}

    /**
     * Method to retrieve the singleton associated with the IOTool tool.
     * @return the IOTool tool.
     */
    public static IOTool getIOTool() { return tool; }

	/****************************** SKILL FORMAT INTERFACE ******************************/

    private static boolean skillChecked = false;
	private static Class skillClass = null;
	private static Method skillOutputMethod;

	/**
	 * Method to tell whether Skill output is available.
	 * Skill is a proprietary format of Cadence, and only valid licensees are given this module.
	 * This method dynamically figures out whether the Skill module is present by using reflection.
	 * @return true if the Skill output module is available.
	 */
	public static boolean hasSkill()
	{
		if (!skillChecked)
		{
			skillChecked = true;

			// find the Skill class
			try
			{
				skillClass = Class.forName("com.sun.electric.plugins.skill.Skill");
			} catch (ClassNotFoundException e)
			{
				skillClass = null;
				return false;
			}

			// find the necessary method on the Skill class
			try
			{
				skillOutputMethod = skillClass.getMethod("writeSkillFile", new Class[] {Cell.class, String.class, Boolean.class});
			} catch (NoSuchMethodException e)
			{
				skillClass = null;
				return false;
			}
		}

		// if already initialized, return
		if (skillClass == null) return false;
	 	return true;
	}

	/**
	 * Method to invoke the Skill output module via reflection.
	 * @param cell the Cell to write in Skill.
	 * @param fileName the name of the file to write.
	 */
	public static void writeSkill(Cell cell, String fileName, boolean exportsOnly)
	{
		if (!hasSkill()) return;
		try
		{
			skillOutputMethod.invoke(skillClass, new Object[] {cell, fileName, new Boolean(exportsOnly)});
		} catch (Exception e)
		{
			System.out.println("Unable to run the Skill output module");
            e.printStackTrace(System.out);
		}
	}

	/****************************** DAIS FORMAT INTERFACE ******************************/

    private static boolean daisChecked = false;
	private static Class daisClass = null;
	private static Method daisInputMethod;

	/**
	 * Method to tell whether Dais input is available.
	 * Dais is a proprietary format of Sun Microsystems.
	 * This method dynamically figures out whether the Dais module is present by using reflection.
	 * @return true if the Dais input module is available.
	 */
	public static boolean hasDais()
	{
		if (!daisChecked)
		{
			daisChecked = true;

			// find the Dais class
			try
			{
				daisClass = Class.forName("com.sun.electric.plugins.dais.Dais");
			} catch (ClassNotFoundException e)
			{
				daisClass = null;
				return false;
			}

			// find the necessary method on the Dais class
			try
			{
				daisInputMethod = daisClass.getMethod("readDaisFile", new Class[] {Library.class});
			} catch (NoSuchMethodException e)
			{
				daisClass = null;
				return false;
			}
		}

		// if already initialized, return
		if (daisClass == null) return false;
	 	return true;
	}

	/**
	 * Method to invoke the Dais input module via reflection.
	 * @param lib the Library to read.
	 */
	public static void readDais(Library lib)
	{
		if (!hasDais()) return;
		try
		{
			daisInputMethod.invoke(daisClass, new Object[] {lib});
		} catch (Exception e)
		{
			System.out.println("Unable to run the Dais input module (" + e.getClass() + ")");
            e.printStackTrace(System.out);
		}
	}

	/****************************** GENERAL IO PREFERENCES ******************************/

	private static Pref cacheBackupRedundancy = Pref.makeIntPref("OutputBackupRedundancy", IOTool.tool.prefs, 0);
	/**
	 * Method to tell what kind of redundancy to apply when writing library files.
	 * The value is:
	 * 0 for no backup (just overwrite the old file) [the default];
	 * 1 for 1-level backup (rename the old file to have a "~" at the end);
	 * 2 for full history backup (rename the old file to have date information in it).
	 * @return the level of redundancy to apply when writing library files.
	 */
	public static int getBackupRedundancy() { return cacheBackupRedundancy.getInt(); }
	/**
	 * Method to set the level of redundancy to apply when writing library files.
	 * The value is:
	 * 0 for no backup (just overwrite the old file);
	 * 1 for 1-level backup (rename the old file to have a "~" at the end);
	 * 2 for full history backup (rename the old file to have date information in it).
	 * @param r the level of redundancy to apply when writing library files.
	 */
	public static void setBackupRedundancy(int r) { cacheBackupRedundancy.setInt(r); }

	/****************************** GENERAL OUTPUT PREFERENCES ******************************/

    private static Pref cacheUseCopyrightMessage = Pref.makeBooleanSetting("UseCopyrightMessage", IOTool.tool.prefs, IOTool.tool,
            tool.getProjectSettings(), null,
            "Netlists tab", "Use copyright message", false);
	/**
	 * Method to tell whether to add the copyright message to output decks.
	 * The default is "false".
	 * @return true to add the copyright message to output decks.
	 */
	public static boolean isUseCopyrightMessage() { return cacheUseCopyrightMessage.getBoolean(); }
	/**
	 * Method to set whether to add the copyright message to output decks.
	 * @param u true to add the copyright message to output decks.
	 */
	public static void setUseCopyrightMessage(boolean u) { cacheUseCopyrightMessage.setBoolean(u); }

	private static Pref cacheCopyrightMessage = Pref.makeStringSetting("CopyrightMessage", IOTool.tool.prefs, IOTool.tool,
            tool.getProjectSettings(), null,
            "Netlists tab", "Copyright message", "");
	/**
	 * Method to tell the copyright message that will be added to output decks.
	 * The default is "".
	 * @return the copyright message that will be added to output decks.
	 */
	public static String getCopyrightMessage() { return cacheCopyrightMessage.getString(); }
	/**
	 * Method to set the copyright message that will be added to output decks.
	 * @param m the copyright message that will be added to output decks.
	 */
	public static void setCopyrightMessage(String m) { cacheCopyrightMessage.setString(m); }

	private static Pref cachePlotArea = Pref.makeIntPref("PlotArea", IOTool.tool.prefs, 0);
	/**
	 * Method to tell the area of the screen to plot for printing/PostScript/HPGL.
	 * @return the area of the screen to plot for printing/PostScript/HPGL:
	 * 0=plot the entire cell (the default);
	 * 1=plot only the highlighted area;
	 * 2=plot only the displayed window.
	 */
	public static int getPlotArea() { return cachePlotArea.getInt(); }
	/**
	 * Method to set the area of the screen to plot for printing/PostScript/HPGL.
	 * @param pa the area of the screen to plot for printing/PostScript/HPGL.
	 * 0=plot the entire cell;
	 * 1=plot only the highlighted area;
	 * 2=plot only the displayed window.
	 */
	public static void setPlotArea(int pa) { cachePlotArea.setInt(pa); }

	private static Pref cachePlotDate = Pref.makeBooleanPref("PlotDate", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether to plot the date in PostScript/HPGL output.
	 * The default is "false".
	 * @return whether to plot the date in PostScript/HPGL output.
	 */
	public static boolean isPlotDate() { return cachePlotDate.getBoolean(); }
	/**
	 * Method to set whether to plot the date in PostScript/HPGL output.
	 * @param pd true to plot the date in PostScript/HPGL output.
	 */
	public static void setPlotDate(boolean pd) { cachePlotDate.setBoolean(pd); }

	private static Pref cachePrinterName = null;

	private static Pref getCachePrinterName()
	{
		if (cachePrinterName == null)
		{
			cachePrinterName = Pref.makeStringPref("PrinterName", IOTool.tool.prefs, "");
//			PrintService defPrintService = PrintServiceLookup.lookupDefaultPrintService();
//			if (defPrintService == null) cachePrinterName = Pref.makeStringPref("PrinterName", IOTool.tool.prefs, ""); else
//				cachePrinterName = Pref.makeStringPref("PrinterName", IOTool.tool.prefs, defPrintService.getName());
		}
		return cachePrinterName;
	}

	/**
	 * Method to tell the default printer name to use.
	 * The default is "".
	 * @return the default printer name to use.
	 */
	public static String getPrinterName() { return getCachePrinterName().getString(); }
	/**
	 * Method to set the default printer name to use.
	 * @param pName the default printer name to use.
	 */
	public static void setPrinterName(String pName) { getCachePrinterName().setString(pName); }

	/****************************** CIF PREFERENCES ******************************/

	private static Pref cacheCIFOutMimicsDisplay = Pref.makeBooleanSetting("CIFMimicsDisplay", IOTool.tool.prefs, IOTool.tool,
            tool.getProjectSettings(), null,
            "CIF tab", "CIF output mimics display", false);
	/**
	 * Method to tell whether CIF Output mimics the display.
	 * To mimic the display, unexpanded cell instances are described as black boxes,
	 * instead of calls to their contents.
	 * The default is "false".
	 * @return true if CIF Output mimics the display.
	 */
	public static boolean isCIFOutMimicsDisplay() { return cacheCIFOutMimicsDisplay.getBoolean(); }
	/**
	 * Method to set whether CIF Output mimics the display.
	 * To mimic the display, unexpanded cell instances are described as black boxes,
	 * instead of calls to their contents.
	 * @param on true if CIF Output mimics the display.
	 */
	public static void setCIFOutMimicsDisplay(boolean on) { cacheCIFOutMimicsDisplay.setBoolean(on); }

	private static Pref cacheCIFOutMergesBoxes = Pref.makeBooleanSetting("CIFMergesBoxes", IOTool.tool.prefs, IOTool.tool,
            tool.getProjectSettings(), null,
            "CIF tab", "CIF output merges boxes", false);
	/**
	 * Method to tell whether CIF Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * The default is "false".
	 * @return true if CIF Output merges boxes into complex polygons.
	 */
	public static boolean isCIFOutMergesBoxes() { return cacheCIFOutMergesBoxes.getBoolean(); }
	/**
	 * Method to set whether CIF Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * @param on true if CIF Output merges boxes into complex polygons.
	 */
	public static void setCIFOutMergesBoxes(boolean on) { cacheCIFOutMergesBoxes.setBoolean(on); }

	private static Pref cacheCIFOutInstantiatesTopLevel = Pref.makeBooleanSetting("CIFInstantiatesTopLevel", IOTool.tool.prefs, IOTool.tool, 
            tool.getProjectSettings(), null,
            "CIF tab", "CIF output instantiates top level", true);
	/**
	 * Method to tell whether CIF Output instantiates the top-level.
	 * When this happens, a CIF "call" to the top cell is emitted.
	 * The default is "true".
	 * @return true if CIF Output merges boxes into complex polygons.
	 */
	public static boolean isCIFOutInstantiatesTopLevel() { return cacheCIFOutInstantiatesTopLevel.getBoolean(); }
	/**
	 * Method to set whether CIF Output merges boxes into complex polygons.
	 * When this happens, a CIF "call" to the top cell is emitted.
	 * @param on true if CIF Output merges boxes into complex polygons.
	 */
	public static void setCIFOutInstantiatesTopLevel(boolean on) { cacheCIFOutInstantiatesTopLevel.setBoolean(on); }

//	private static Pref cacheCIFOutResolution = Pref.makeDoublePref("CIFResolution", IOTool.tool.prefs, 0);
//	/**
//	 * Method to tell the minimum CIF Output resolution.
//	 * This is the smallest feature size that can be safely generated.
//	 * The default is "0" (no resolution check).
//	 * @return the minimum CIF Output resolution.
//	 */
//	public static double getCIFOutResolution() { return cacheCIFOutResolution.getDouble(); }
//	/**
//	 * Method to set the minimum CIF Output resolution.
//	 * This is the smallest feature size that can be safely generated.
//	 * @param r the minimum CIF Output resolution.
//	 */
//	public static void setCIFOutResolution(double r) { cacheCIFOutResolution.setDouble(r); }

	private static Pref cacheCIFInSquaresWires = Pref.makeBooleanPref("CIFInSquaresWires", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether CIF input makes wire ends square or round.
	 * The default is "true" (square).
	 * @return true if CIF input makes wire ends square.
	 */
	public static boolean isCIFInSquaresWires() { return cacheCIFInSquaresWires.getBoolean(); }
	/**
	 * Method to set whether CIF input makes wire ends square or round.
	 * @param s true if CIF input makes wire ends square.
	 */
	public static void setCIFInSquaresWires(boolean s) { cacheCIFInSquaresWires.setBoolean(s); }

	/****************************** DEF PREFERENCES ******************************/

	private static Pref cacheDEFLogicalPlacement = Pref.makeBooleanPref("DEFLogicalPlacement", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether DEF Input makes logical placement.
	 * The default is "true" (do the logical placement).
	 * @return true if  DEF Input makes logical placement.
	 */
	public static boolean isDEFLogicalPlacement() { return cacheDEFLogicalPlacement.getBoolean(); }
	/**
	 * Method to set whether  DEF Input makes logical placement.
	 * @param on true if  DEF Input makes logical placement.
	 */
	public static void setDEFLogicalPlacement(boolean on) { cacheDEFLogicalPlacement.setBoolean(on); }

	private static Pref cacheDEFPhysicalPlacement = Pref.makeBooleanPref("DEFPhysicalPlacement", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether DEF Input makes physical placement.
	 * The default is "true" (do the physical placement).
	 * @return true if  DEF Input makes physical placement.
	 */
	public static boolean isDEFPhysicalPlacement() { return cacheDEFPhysicalPlacement.getBoolean(); }
	/**
	 * Method to set whether  DEF Input makes physical placement.
	 * @param on true if  DEF Input makes physical placement.
	 */
	public static void setDEFPhysicalPlacement(boolean on) { cacheDEFPhysicalPlacement.setBoolean(on); }

	/****************************** GDS PREFERENCES ******************************/

	private static Pref cacheGDSOutMergesBoxes = Pref.makeBooleanSetting("GDSMergesBoxes", IOTool.tool.prefs, IOTool.tool,
            tool.getProjectSettings(), null,
            "GDS tab", "GDS output merges boxes", false);
	/**
	 * Method to tell whether GDS Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * The default is "false".
	 * @return true if GDS Output merges boxes into complex polygons.
	 */
	public static boolean isGDSOutMergesBoxes() { return cacheGDSOutMergesBoxes.getBoolean(); }
	/**
	 * Method to set whether GDS Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * @param on true if GDS Output merges boxes into complex polygons.
	 */
	public static void setGDSOutMergesBoxes(boolean on) { cacheGDSOutMergesBoxes.setBoolean(on); }

	private static Pref cacheGDSOutWritesExportPins = Pref.makeBooleanSetting("GDSWritesExportPins", IOTool.tool.prefs, IOTool.tool,
            tool.getProjectSettings(), null,
            "GDS tab", "GDS output writes export pins", false);
	/**
	 * Method to tell whether GDS Output writes pins at Export locations.
	 * Some systems can use this information to reconstruct export locations.
	 * The default is "false".
	 * @return true if GDS Output writes pins at Export locations.
	 */
	public static boolean isGDSOutWritesExportPins() { return cacheGDSOutWritesExportPins.getBoolean(); }
	/**
	 * Method to set whether GDS Output writes pins at Export locations.
	 * Some systems can use this information to reconstruct export locations.
	 * @param on true if GDS Output writes pins at Export locations.
	 */
	public static void setGDSOutWritesExportPins(boolean on) { cacheGDSOutWritesExportPins.setBoolean(on); }

	private static Pref cacheGDSOutUpperCase = Pref.makeBooleanSetting("GDSOutputUpperCase", IOTool.tool.prefs, IOTool.tool,
            tool.getProjectSettings(), null,
            "GDS tab", "GDS output all upper-case", false);
	/**
	 * Method to tell whether GDS Output makes all text upper-case.
	 * Some systems insist on this.
	 * The default is "false".
	 * @return true if GDS Output makes all text upper-case.
	 */
	public static boolean isGDSOutUpperCase() { return cacheGDSOutUpperCase.getBoolean(); }
	/**
	 * Method to set whether GDS Output makes all text upper-case.
	 * Some systems insist on this.
	 * @param on true if GDS Output makes all text upper-case.
	 */
	public static void setGDSOutUpperCase(boolean on) { cacheGDSOutUpperCase.setBoolean(on); }

	private static Pref cacheGDSOutDefaultTextLayer = Pref.makeIntSetting("GDSDefaultTextLayer", IOTool.tool.prefs, IOTool.tool,
            tool.getProjectSettings(), null,
            "GDS tab", "GDS output default text layer", 230);
	/**
	 * Method to tell the default GDS layer to use for the text of Export pins.
	 * Export pins are annotated with text objects on this layer.
	 * If this is negative, do not write Export pins.
	 * The default is "230".
	 * @return the default GDS layer to use for the text of Export pins.
	 */
	public static int getGDSOutDefaultTextLayer() { return cacheGDSOutDefaultTextLayer.getInt(); }
	/**
	 * Method to set the default GDS layer to use for the text of Export pins.
	 * Export pins are annotated with text objects on this layer.
	 * @param num the default GDS layer to use for the text of Export pins.
	 * If this is negative, do not write Export pins.
	 */
	public static void setGDSOutDefaultTextLayer(int num) { cacheGDSOutDefaultTextLayer.setInt(num); }

    private static Pref cacheGDSOutputConvertsBracketsInExports = Pref.makeBooleanSetting("GDSOutputConvertsBracketsInExports", IOTool.tool.prefs, IOTool.tool,
            tool.getProjectSettings(), null,
            "GDS tab", "GDS output converts brackets in exports", true);
    /**
     * Method to get the state of whether the GDS writer converts brackets
     * to underscores in export names.
     */
    public static boolean getGDSOutputConvertsBracketsInExports() { return cacheGDSOutputConvertsBracketsInExports.getBoolean(); }
    /**
     * Method to set the state of whether the GDS writer converts brackets
     * to underscores in export names.
     */
    public static void setGDSOutputConvertsBracketsInExports(boolean b) { cacheGDSOutputConvertsBracketsInExports.setBoolean(b); }

	private static Pref cacheGDSInMergesBoxes = Pref.makeBooleanPref("GDSInMergesBoxes", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether GDS Input merges boxes into complex polygons.
	 * This takes more time but produces a smaller database.
	 * The default is "false".
	 * @return true if GDS Input merges boxes into complex polygons.
	 */
	public static boolean isGDSInMergesBoxes() { return cacheGDSInMergesBoxes.getBoolean(); }
	/**
	 * Method to set whether GDS Input merges boxes into complex polygons.
	 * This takes more time but produces a smaller database.
	 * @param on true if GDS Input merges boxes into complex polygons.
	 */
	public static void setGDSInMergesBoxes(boolean on) { cacheGDSInMergesBoxes.setBoolean(on); }

	private static Pref cacheGDSInIncludesText = Pref.makeBooleanPref("GDSInIncludesText", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether GDS Input ignores text.
	 * Text can clutter the display, so some users don't want to read it.
	 * The default is "false".
	 * @return true if GDS Input ignores text.
	 */
	public static boolean isGDSInIncludesText() { return cacheGDSInIncludesText.getBoolean(); }
	/**
	 * Method to set whether GDS Input ignores text.
	 * Text can clutter the display, so some users don't want to read it.
	 * @param on true if GDS Input ignores text.
	 */
	public static void setGDSInIncludesText(boolean on) { cacheGDSInIncludesText.setBoolean(on); }

	private static Pref cacheGDSInExpandsCells = Pref.makeBooleanPref("GDSInExpandsCells", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether GDS Input expands cells.
	 * The default is "false".
	 * @return true if GDS Input expands cells.
	 */
	public static boolean isGDSInExpandsCells() { return cacheGDSInExpandsCells.getBoolean(); }
	/**
	 * Method to set whether GDS Input expands cells.
	 * @param on true if GDS Input expands cells.
	 */
	public static void setGDSInExpandsCells(boolean on) { cacheGDSInExpandsCells.setBoolean(on); }

	private static Pref cacheGDSInInstantiatesArrays = Pref.makeBooleanPref("GDSInInstantiatesArrays", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether GDS Input instantiates arrays.
	 * The default is "true".
	 * When false, only the edges of arrays are instantiated, not those entries in the center.
	 * @return true if GDS Input instantiates arrays.
	 */
	public static boolean isGDSInInstantiatesArrays() { return cacheGDSInInstantiatesArrays.getBoolean(); }
	/**
	 * Method to set whether GDS Input instantiates arrays.
	 * When false, only the edges of arrays are instantiated, not those entries in the center.
	 * @param on true if GDS Input instantiates arrays.
	 */
	public static void setGDSInInstantiatesArrays(boolean on) { cacheGDSInInstantiatesArrays.setBoolean(on); }

	private static Pref cacheGDSInIgnoresUnknownLayers = Pref.makeBooleanPref("GDSInIgnoresUnknownLayers", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether GDS Input ignores unknown layers
	 * The default is "false".
	 * Unknown layers are replaced with "DRC exclusion" nodes if not ignored.
	 * @return true if GDS Input ignores unknown layers
	 */
	public static boolean isGDSInIgnoresUnknownLayers() { return cacheGDSInIgnoresUnknownLayers.getBoolean(); }
	/**
	 * Method to set whether GDS Input ignores unknown layers
	 * Unknown layers are replaced with "DRC exclusion" nodes if not ignored.
	 * @param on true if GDS Input ignores unknown layers
	 */
	public static void setGDSInIgnoresUnknownLayers(boolean on) { cacheGDSInIgnoresUnknownLayers.setBoolean(on); }

    private static Pref cacheGDSCellNameLenMax = Pref.makeIntSetting("GDSCellNameLenMax", IOTool.tool.prefs, IOTool.tool,
            tool.getProjectSettings(), null,
    	"GDS tab", "GDS name length limit", 32);
    /**
     * Get the maximum length (number of chars) for Cell names in the GDS output file
     * @return the number of chars
     */
    public static int getGDSCellNameLenMax() { return cacheGDSCellNameLenMax.getInt(); }
    /**
     * Set the maximum length (number of chars) for Cell names in the GDS output file
     */
    public static void setGDSCellNameLenMax(int len) { cacheGDSCellNameLenMax.setInt(len); }

    private static Pref cacheGDSConvertNCCExportsConnectedByParentPins = Pref.makeBooleanPref("GDSConvertNCCEconnectedByParentPins", IOTool.tool.prefs, false);
    /**
     * True to convert pin names to name:name for pins that are specified in the
     * NCC annotation, "exportsConnectedByParent".  This allows external LVS tools to
     * perform the analogous operation of virtual connection of networks.
     * For example, 'exportsConnectedByParent vdd /vdd_[0-9]+/' will rename all
     * pins that match the assertion to vdd:vdd.
     */
    public static boolean getGDSConvertNCCExportsConnectedByParentPins() { return cacheGDSConvertNCCExportsConnectedByParentPins.getBoolean(); }
    /**
     * True to convert pin names to name:name for pins that are specified in the
     * NCC annotation, "exportsConnectedByParent".  This allows external LVS tools to
     * perform the analogous operation of virtual connection of networks.
     * For example, 'exportsConnectedByParent vdd /vdd_[0-9]+/' will rename all
     * pins that match the assertion to vdd:vdd.
     */
    public static void setGDSConvertNCCExportsConnectedByParentPins(boolean b) { cacheGDSConvertNCCExportsConnectedByParentPins.setBoolean(b); }

    private static Pref cacheGDSInSimplifyCells = Pref.makeBooleanPref("GDSInSimplifyCells", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether GDS Input simplifies contact vias.
	 * The default is "false".
	 * @return true if GDS Input simplifies contact vias.
	 */
	public static boolean isGDSInSimplifyCells() { return cacheGDSInSimplifyCells.getBoolean(); }
	/**
	 * Method to set whether GDS Input simplifies contact vias.
	 * @param on true if GDS Input simplifies contact vias.
	 */
	public static void setGDSInSimplifyCells(boolean on) { cacheGDSInSimplifyCells.setBoolean(on); }

    private static Pref cacheGDSColapseVddGndPinNames = Pref.makeBooleanPref("cacheGDSColapseVddGndPinNames", IOTool.tool.prefs, false);
    /**
     * Method to tell whether Vdd_* and Gnd_* export pins must be colapsed. This is for extraction in Fire/Ice.
     * @return true if GDS Input colapses vdd/gnd names.
     */
    public static boolean isGDSColapseVddGndPinNames() { return cacheGDSColapseVddGndPinNames.getBoolean(); }
    /**
     * Method to set whether Vdd_* and Gnd_* export pins must be colapsed. This is for extraction in Fire/Ice.
     * @param on true if GDS Input colapses vdd/gnd names.
     */
    public static void setGDSColapseVddGndPinNames(boolean on) { cacheGDSColapseVddGndPinNames.setBoolean(on); }

	/****************************** POSTSCRIPT OUTPUT PREFERENCES ******************************/

	private static Pref cachePrintEncapsulated = Pref.makeBooleanPref("PostScriptEncapsulated", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether PostScript Output is Encapsulated.
	 * Encapsulated PostScript can be inserted into other documents.
	 * The default is "false".
	 * @return true if PostScript Output is Encapsulated.
	 */
	public static boolean isPrintEncapsulated() { return cachePrintEncapsulated.getBoolean(); }
	/**
	 * Method to set whether PostScript Output is Encapsulated.
	 * Encapsulated PostScript can be inserted into other documents.
	 * @param on true if PostScript Output is Encapsulated.
	 */
	public static void setPrintEncapsulated(boolean on) { cachePrintEncapsulated.setBoolean(on); }

	private static Pref cachePrintResolution = Pref.makeIntPref("PrintResolution", IOTool.tool.prefs, 300);
	/**
	 * Method to tell the default printing resolution.
	 * Java printing assumes 72 DPI, this is an override.
	 * The default is "300".
	 * @return the default printing resolution.
	 */
	public static int getPrintResolution() { return cachePrintResolution.getInt(); }
	/**
	 * Method to set the default printing resolution.
	 * Java printing assumes 72 DPI, this is an override.
	 * @param r the default printing resolution.
	 */
	public static void setPrintResolution(int r) { cachePrintResolution.setInt(r); }

	private static Pref cachePrintForPlotter = Pref.makeBooleanPref("PostScriptForPlotter", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether PostScript Output is for a plotter.
	 * Plotters have width, but no height, since they are continuous feed.
	 * The default is "false".
	 * @return true if PostScript Output is for a plotter.
	 */
	public static boolean isPrintForPlotter() { return cachePrintForPlotter.getBoolean(); }
	/**
	 * Method to set whether PostScript Output is for a plotter.
	 * Plotters have width, but no height, since they are continuous feed.
	 * @param on true if PostScript Output is for a plotter.
	 */
	public static void setPrintForPlotter(boolean on) { cachePrintForPlotter.setBoolean(on); }

	private static Pref cachePrintWidth = Pref.makeDoublePref("PostScriptWidth", IOTool.tool.prefs, 8.5);
	/**
	 * Method to tell the width of PostScript Output.
	 * The width is in inches.
	 * The default is "8.5".
	 * @return the width of PostScript Output.
	 */
	public static double getPrintWidth() { return cachePrintWidth.getDouble(); }
	/**
	 * Method to set the width of PostScript Output.
	 * The width is in inches.
	 * @param wid the width of PostScript Output.
	 */
	public static void setPrintWidth(double wid) { cachePrintWidth.setDouble(wid); }

	private static Pref cachePrintHeight = Pref.makeDoublePref("PostScriptHeight", IOTool.tool.prefs, 11);
	/**
	 * Method to tell the height of PostScript Output.
	 * The height is in inches, and only applies if printing (not plotting).
	 * The default is "11".
	 * @return the height of PostScript Output.
	 */
	public static double getPrintHeight() { return cachePrintHeight.getDouble(); }
	/**
	 * Method to set the height of PostScript Output.
	 * The height is in inches, and only applies if printing (not plotting).
	 * @param hei the height of PostScript Output.
	 */
	public static void setPrintHeight(double hei) { cachePrintHeight.setDouble(hei); }

	private static Pref cachePrintMargin = Pref.makeDoublePref("PostScriptMargin", IOTool.tool.prefs, 0.75);
	/**
	 * Method to tell the margin of PostScript Output.
	 * The margin is in inches and insets from all sides.
	 * The default is "0.75".
	 * @return the margin of PostScript Output.
	 */
	public static double getPrintMargin() { return cachePrintMargin.getDouble(); }
	/**
	 * Method to set the margin of PostScript Output.
	 * The margin is in inches and insets from all sides.
	 * @param mar the margin of PostScript Output.
	 */
	public static void setPrintMargin(double mar) { cachePrintMargin.setDouble(mar); }

	private static Pref cachePrintRotation = Pref.makeIntPref("PostScriptRotation", IOTool.tool.prefs, 0);
	/**
	 * Method to tell the rotation of PostScript Output.
	 * The plot can be normal or rotated 90 degrees to better fit the paper.
	 * @return the rotation of PostScript Output:
	 * 0=no rotation (the default);
	 * 1=rotate 90 degrees;
	 * 2=rotate automatically to fit best.
	 */
	public static int getPrintRotation() { return cachePrintRotation.getInt(); }
	/**
	 * Method to set the rotation of PostScript Output.
	 * The plot can be normal or rotated 90 degrees to better fit the paper.
	 * @param rot the rotation of PostScript Output.
	 * 0=no rotation;
	 * 1=rotate 90 degrees;
	 * 2=rotate automatically to fit best.
	 */
	public static void setPrintRotation(int rot) { cachePrintRotation.setInt(rot); }

	private static Pref cachePrintColorMethod = Pref.makeIntPref("PostScriptColorMethod", IOTool.tool.prefs, 0);
	/**
	 * Method to tell the color method of PostScript Output.
	 * @return the color method of PostScript Output:
	 * 0=Black & White (the default);
	 * 1=Color (solid);
	 * 2=Color (stippled);
	 * 3=Color (merged).
	 */
	public static int getPrintColorMethod() { return cachePrintColorMethod.getInt(); }
	/**
	 * Method to set the color method of PostScript Output.
	 * @param cm the color method of PostScript Output.
	 * 0=Black & White;
	 * 1=Color (solid);
	 * 2=Color (stippled);
	 * 3=Color (merged).
	 */
	public static void setPrintColorMethod(int cm) { cachePrintColorMethod.setInt(cm); }

	public static final Variable.Key POSTSCRIPT_EPS_SCALE = Variable.newKey("IO_postscript_EPS_scale");
	/**
	 * Method to tell the EPS scale of a given Cell.
	 * @param cell the cell to query.
	 * @return the EPS scale of that Cell.
	 */
	public static double getPrintEPSScale(Cell cell)
	{
		Variable var = cell.getVar(POSTSCRIPT_EPS_SCALE);
		if (var != null)
		{
			Object obj = var.getObject();
			String desc = obj.toString();
			double epsScale = TextUtils.atof(desc);
			return epsScale;
		}
		return 1;
	}
	/**
	 * Method to set the EPS scale of a given Cell.
	 * @param cell the cell to modify.
	 * @param scale the EPS scale of that Cell.
	 */
	public static void setPrintEPSScale(Cell cell, double scale)
	{
		tool.setVarInJob(cell, POSTSCRIPT_EPS_SCALE, new Double(scale));
	}

	public static final Variable.Key POSTSCRIPT_FILENAME = Variable.newKey("IO_postscript_filename");
	/**
	 * Method to tell the EPS synchronization file of a given Cell.
	 * During automatic synchronization of PostScript, any cell changed more
	 * recently than the date on this file will cause that file to be generated
	 * from the Cell.
	 * @param cell the cell to query.
	 * @return the EPS synchronization file of that Cell.
	 */
	public static String getPrintEPSSynchronizeFile(Cell cell)
	{
		Variable var = cell.getVar(POSTSCRIPT_FILENAME);
		if (var != null)
		{
			Object obj = var.getObject();
			String desc = obj.toString();
			return desc;
		}
		return "";
	}
	/**
	 * Method to set the EPS synchronization file of a given Cell.
	 * During automatic synchronization of PostScript, any cell changed more
	 * recently than the date on this file will cause that file to be generated
	 * from the Cell.
	 * @param cell the cell to modify.
	 * @param syncFile the EPS synchronization file to associate with that Cell.
	 */
	public static void setPrintEPSSynchronizeFile(Cell cell, String syncFile)
	{
		tool.setVarInJob(cell, POSTSCRIPT_FILENAME, syncFile);
	}

	public static final Variable.Key POSTSCRIPT_FILEDATE = Variable.newKey("IO_postscript_filedate");
	/**
	 * Method to tell the EPS synchronization file of a given Cell.
	 * During automatic synchronization of PostScript, any cell changed more
	 * recently than the date on this file will cause that file to be generated
	 * from the Cell.
	 * @param cell the cell to query.
	 * @return the EPS synchronization file of that Cell.
	 */
	public static Date getPrintEPSSavedDate(Cell cell)
	{
		Variable varDate = cell.getVar(POSTSCRIPT_FILEDATE, Integer[].class);
		if (varDate == null) return null;
		Integer [] lastSavedDateAsInts = (Integer [])varDate.getObject();
		long lastSavedDateInSeconds = ((long)lastSavedDateAsInts[0].intValue() << 32) |
			(lastSavedDateAsInts[1].intValue() & 0xFFFFFFFF);
		Date lastSavedDate = new Date(lastSavedDateInSeconds);
		return lastSavedDate;
	}
	/**
	 * Method to set the EPS synchronization file of a given Cell.
	 * During automatic synchronization of PostScript, any cell changed more
	 * recently than the date on this file will cause that file to be generated
	 * from the Cell.
	 * @param cell the cell to modify.
	 * @param date the EPS synchronization date to associate with that Cell.
	 */
	public static void setPrintEPSSavedDate(Cell cell, Date date)
	{
		long iVal = date.getTime();
		Integer [] dateArray = new Integer[2];
		dateArray[0] = new Integer((int)(iVal >> 32));
		dateArray[1] = new Integer((int)(iVal & 0xFFFFFFFF));
		tool.setVarInJob(cell, POSTSCRIPT_FILEDATE, dateArray);
	}

	private static Pref cachePrintPSLineWidth = Pref.makeDoublePref("PostScriptLineWidth", IOTool.tool.prefs, 1);
	/**
	 * Method to tell the width of PostScript lines.
	 * Lines have their width scaled by this amount, so the default (1) means normal lines.
	 * @return the width of PostScript lines.
	 */
	public static double getPrintPSLineWidth() { return cachePrintPSLineWidth.getDouble(); }
	/**
	 * Method to set the width of PostScript lines.
	 * Lines have their width scaled by this amount, so the default (1) means normal lines.
	 * @param mar the width of PostScript lines.
	 */
	public static void setPrintPSLineWidth(double mar) { cachePrintPSLineWidth.setDouble(mar); }

	/****************************** EDIF PREFERENCES ******************************/

	private static Pref cacheEDIFUseSchematicView = Pref.makeBooleanPref("EDIFUseSchematicView", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether EDIF uses the schematic view.
	 * The default is "false".
	 * @return true if EDIF uses the schematic view.
	 */
	public static boolean isEDIFUseSchematicView() { return cacheEDIFUseSchematicView.getBoolean(); }
	/**
	 * Method to set whether EDIF uses the schematic view.
	 * @param f true if EDIF uses the schematic view.
	 */
	public static void setEDIFUseSchematicView(boolean f) { cacheEDIFUseSchematicView.setBoolean(f); }

	private static Pref cacheEDIFCadenceCompatibility = Pref.makeBooleanPref("EDIFCadenceCompatibility", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether EDIF I/O is compatible with Cadence.
	 * The default is "true".
	 * @return true if EDIF I/O is compatible with Cadence.
	 */
	public static boolean isEDIFCadenceCompatibility() { return cacheEDIFCadenceCompatibility.getBoolean(); }
	/**
	 * Method to set whether EDIF I/O is compatible with Cadence.
	 * @param c true if EDIF I/O is compatible with Cadence.
	 */
	public static void setEDIFCadenceCompatibility(boolean c) { cacheEDIFCadenceCompatibility.setBoolean(c); }

	private static Pref cacheEDIFInputScale = Pref.makeDoublePref("EDIFInputScale", IOTool.tool.prefs, 0.05);
	/**
	 * Method to return the EDIF input scale.
	 * The default is "1".
	 * @return the EDIF input scale.
	 */
	public static double getEDIFInputScale() { return cacheEDIFInputScale.getDouble(); }
	/**
	 * Method to set the EDIF input scale.
	 * @param f the EDIF input scale.
	 */
	public static void setEDIFInputScale(double f) { cacheEDIFInputScale.setDouble(f); }

	private static Pref cacheEDIFConfigurationFile = Pref.makeStringPref("EDIFConfigurationFile", IOTool.tool.prefs, "");
	/**
	 * Method to tell the configuration file to use.
	 * The default is "" (no configuration file).
	 * @return the configuration file to use.
	 */
	public static String getEDIFConfigurationFile() { return cacheEDIFConfigurationFile.getString(); }
	/**
	 * Method to set the configuration file to use.
	 * @param cFile the configuration file to use.
	 */
	public static void setEDIFConfigurationFile(String cFile) { cacheEDIFConfigurationFile.setString(cFile); }

	/****************************** DXF PREFERENCES ******************************/

	private static Pref cacheDXFInputFlattensHierarchy = Pref.makeBooleanPref("DXFInputFlattensHierarchy", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether DXF Input flattens the hierarchy.
	 * Flattened DXF appears in a single cell.
	 * The default is "true".
	 * @return true if DXF Input flattens the hierarchy.
	 */
	public static boolean isDXFInputFlattensHierarchy() { return cacheDXFInputFlattensHierarchy.getBoolean(); }
	/**
	 * Method to set whether DXF Input flattens the hierarchy.
	 * Flattened DXF appears in a single cell.
	 * @param f true if DXF Input flattens the hierarchy.
	 */
	public static void setDXFInputFlattensHierarchy(boolean f) { cacheDXFInputFlattensHierarchy.setBoolean(f); }

	private static Pref cacheDXFInputReadsAllLayers = Pref.makeBooleanPref("DXFInputReadsAllLayers", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether DXF input reads all layers.
	 * When a DXF layer in the file is unknown, it is ignored if all layers are NOT being read;
	 * it is converted to another layer if all layers ARE being read.
	 * The default is "false".
	 * @return true if DXF input reads all layers.
	 */
	public static boolean isDXFInputReadsAllLayers() { return cacheDXFInputReadsAllLayers.getBoolean(); }
	/**
	 * Method to set whether DXF input reads all layers.
	 * When a DXF layer in the file is unknown, it is ignored if all layers are NOT being read;
	 * it is converted to another layer if all layers ARE being read.
	 * @param a true if DXF input reads all layers.
	 */
	public static void setDXFInputReadsAllLayers(boolean a) { cacheDXFInputReadsAllLayers.setBoolean(a); }

	private static Pref cacheDXFScale = Pref.makeIntSetting("DXFScale", IOTool.tool.prefs, IOTool.tool,
            tool.getProjectSettings(), null,
		"DXF tab", "DXF scale factor", 2);
	/**
	 * Method to tell the DXF scale.
	 * The DXF scale is:
	 * <UL>
	 * <LI>-3: GigaMeters
	 * <LI>-2: MegaMeters
	 * <LI>-1: KiloMeters
	 * <LI>0: Meters
	 * <LI>1: MilliMeters
	 * <LI>2: MicroMeters
	 * <LI>3: NanoMeters
	 * <LI>4: PicoMeters
	 * <LI>5: FemtoMeters
	 * </UL>
	 * The default is "2" (MicroMeters).
	 * @return the DXF scale.
	 */
	public static int getDXFScale() { return cacheDXFScale.getInt(); }
	/**
	 * Method to set the DXF scale.
	 * The DXF scale is:
	 * <UL>
	 * <LI>-3: GigaMeters
	 * <LI>-2: MegaMeters
	 * <LI>-1: KiloMeters
	 * <LI>0: Meters
	 * <LI>1: MilliMeters
	 * <LI>2: MicroMeters
	 * <LI>3: NanoMeters
	 * <LI>4: PicoMeters
	 * <LI>5: FemtoMeters
	 * </UL>
	 * @param s the DXF scale.
	 */
	public static void setDXFScale(int s) { cacheDXFScale.setInt(s); }

	/****************************** SUE OUTPUT PREFERENCES ******************************/

	private static Pref cacheSueUses4PortTransistors = Pref.makeBooleanPref("SueUses4PortTransistors", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether Sue input creates 4-port transistors.
	 * Without this, standard 3-port transistors are created.
	 * The default is "false".
	 * @return true if Sue input creates 4-port transistors.
	 */
	public static boolean isSueUses4PortTransistors() { return cacheSueUses4PortTransistors.getBoolean(); }
	/**
	 * Method to set whether Sue input creates 4-port transistors.
	 * Without this, standard 3-port transistors are created.
	 * @param on true if Sue input creates 4-port transistors.
	 */
	public static void setSueUses4PortTransistors(boolean on) { cacheSueUses4PortTransistors.setBoolean(on); }

	/****************************** SKILL OUTPUT PREFERENCES ******************************/

	private static Pref cacheSkillExcludesSubcells = Pref.makeBooleanPref("SkillExcludesSubcells", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether Skill Output excludes subcells.
	 * If subcells are included, a Skill output files have multiple cell definitions in them.
	 * The default is "false".
	 * @return true if Skill Output excludes subcells.
	 */
	public static boolean isSkillExcludesSubcells() { return cacheSkillExcludesSubcells.getBoolean(); }
	/**
	 * Method to set whether Skill Output excludes subcells.
	 * If subcells are included, a Skill output files have multiple cell definitions in them.
	 * @param on true if Skill Output excludes subcells.
	 */
	public static void setSkillExcludesSubcells(boolean on) { cacheSkillExcludesSubcells.setBoolean(on); }

	private static Pref cacheSkillFlattensHierarchy = Pref.makeBooleanPref("SkillFlattensHierarchy", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether Skill Output flattens the hierarchy.
	 * Flattened files are larger, but have no hierarchical structure.
	 * The default is "false".
	 * @return true if Skill Output flattens the hierarchy.
	 */
	public static boolean isSkillFlattensHierarchy() { return cacheSkillFlattensHierarchy.getBoolean(); }
	/**
	 * Method to set whether Skill Output flattens the hierarchy.
	 * Flattened files are larger, but have no hierarchical structure.
	 * @param on true if Skill Output flattens the hierarchy.
	 */
	public static void setSkillFlattensHierarchy(boolean on) { cacheSkillFlattensHierarchy.setBoolean(on); }

    private static Pref cacheSkillGDSNameLimit = Pref.makeBooleanPref("SkillGDSNameLimit", IOTool.tool.prefs, false);
    /**
     * Method to tell whether Skill Output flattens the hierarchy.
     * Flattened files are larger, but have no hierarchical structure.
     * The default is "false".
     * @return true if Skill Output flattens the hierarchy.
     */
    public static boolean isSkillGDSNameLimit() { return cacheSkillGDSNameLimit.getBoolean(); }
    /**
     * Method to set whether Skill Output flattens the hierarchy.
     * Flattened files are larger, but have no hierarchical structure.
     * @param on true if Skill Output flattens the hierarchy.
     */
    public static void setSkillGDSNameLimit(boolean on) { cacheSkillGDSNameLimit.setBoolean(on); }

	/****************************** DAIS OUTPUT PREFERENCES ******************************/

	private static Pref cacheDaisDisplayOnly = Pref.makeBooleanPref("DaisDisplayOnly", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether Dais Input creates real geometry.
	 * When real geometry is created, it takes more time and memory, but the circuitry can be edited.
	 * When false, Dais input is read directly into the display system for rapid viewing.
	 * The default is "false".
	 * @return true if Dais Input creates real geometry.
	 */
	public static boolean isDaisDisplayOnly() { return cacheDaisDisplayOnly.getBoolean(); }
	/**
	 * Method to set whether Dais Input creates real geometry.
	 * When real geometry is created, it takes more time and memory, but the circuitry can be edited.
	 * When false, Dais input is read directly into the display system for rapid viewing.
	 * @param on true if Dais Input creates real geometry.
	 */
	public static void setDaisDisplayOnly(boolean on) { cacheDaisDisplayOnly.setBoolean(on); }

	private static Pref cacheDaisReadCellInstances = Pref.makeBooleanPref("DaisReadCellInstances", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether Dais Input creates real geometry.
	 * When real geometry is created, it takes more time and memory, but the circuitry can be edited.
	 * When false, Dais input is read directly into the display system for rapid viewing.
	 * The default is "false".
	 * @return true if Dais Input creates real geometry.
	 */
	public static boolean isDaisReadCellInstances() { return cacheDaisReadCellInstances.getBoolean(); }
	/**
	 * Method to set whether Dais Input creates real geometry.
	 * When real geometry is created, it takes more time and memory, but the circuitry can be edited.
	 * When false, Dais input is read directly into the display system for rapid viewing.
	 * @param on true if Dais Input creates real geometry.
	 */
	public static void setDaisReadCellInstances(boolean on) { cacheDaisReadCellInstances.setBoolean(on); }

	private static Pref cacheDaisReadDetailWires = Pref.makeBooleanPref("DaisReadDetailWires", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether Dais Input creates real geometry.
	 * When real geometry is created, it takes more time and memory, but the circuitry can be edited.
	 * When false, Dais input is read directly into the display system for rapid viewing.
	 * The default is "false".
	 * @return true if Dais Input creates real geometry.
	 */
	public static boolean isDaisReadDetailWires() { return cacheDaisReadDetailWires.getBoolean(); }
	/**
	 * Method to set whether Dais Input creates real geometry.
	 * When real geometry is created, it takes more time and memory, but the circuitry can be edited.
	 * When false, Dais input is read directly into the display system for rapid viewing.
	 * @param on true if Dais Input creates real geometry.
	 */
	public static void setDaisReadDetailWires(boolean on) { cacheDaisReadDetailWires.setBoolean(on); }

	private static Pref cacheDaisReadGlobalWires = Pref.makeBooleanPref("DaisReadGlobalWires", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether Dais Input creates real geometry.
	 * When real geometry is created, it takes more time and memory, but the circuitry can be edited.
	 * When false, Dais input is read directly into the display system for rapid viewing.
	 * The default is "false".
	 * @return true if Dais Input creates real geometry.
	 */
	public static boolean isDaisReadGlobalWires() { return cacheDaisReadGlobalWires.getBoolean(); }
	/**
	 * Method to set whether Dais Input creates real geometry.
	 * When real geometry is created, it takes more time and memory, but the circuitry can be edited.
	 * When false, Dais input is read directly into the display system for rapid viewing.
	 * @param on true if Dais Input creates real geometry.
	 */
	public static void setDaisReadGlobalWires(boolean on) { cacheDaisReadGlobalWires.setBoolean(on); }

	private static Pref cacheDaisReadPowerAndGround = Pref.makeBooleanPref("DaisReadPowerAndGround", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether Dais Input creates real geometry.
	 * When real geometry is created, it takes more time and memory, but the circuitry can be edited.
	 * When false, Dais input is read directly into the display system for rapid viewing.
	 * The default is "false".
	 * @return true if Dais Input creates real geometry.
	 */
	public static boolean isDaisReadPowerAndGround() { return cacheDaisReadPowerAndGround.getBoolean(); }
	/**
	 * Method to set whether Dais Input creates real geometry.
	 * When real geometry is created, it takes more time and memory, but the circuitry can be edited.
	 * When false, Dais input is read directly into the display system for rapid viewing.
	 * @param on true if Dais Input creates real geometry.
	 */
	public static void setDaisReadPowerAndGround(boolean on) { cacheDaisReadPowerAndGround.setBoolean(on); }
}

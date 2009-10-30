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
package com.sun.electric.tool.io;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.ToolSettings;
import com.sun.electric.tool.io.input.Input.InputPreferences;
import com.sun.electric.tool.io.output.Output;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.BitSet;
import java.util.Date;
import java.util.Map;

/**
 * This class manages reading files in different formats.
 * The class is subclassed by the different file readers.
 */
public class IOTool extends Tool
{
	/** the IO tool. */										private static IOTool tool = new IOTool();
    static { FileType.getFileTypeGroups(); } // Preallocate preferences

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
	private static Class<?> skillClass = null;
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
				skillOutputMethod = skillClass.getMethod("writeSkillFile", new Class[] {Cell.class, String.class, SkillPreferences.class});
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

	public static class SkillPreferences extends Output.OutputPreferences
    {
		public boolean exportsOnly;
		public String libName;
		public boolean gdsNameLimit;
		public int gdsCellNameLenMax;
		public boolean gdsOutUpperCase;
		public boolean flattenHierarchy;
		public boolean excludeSubcells;

		public SkillPreferences(boolean factory, boolean exportsOnly, Cell cell)
		{
			super(factory);
			this.exportsOnly = exportsOnly;
			gdsNameLimit = IOTool.isSkillGDSNameLimit();
			gdsCellNameLenMax = IOTool.getGDSCellNameLenMax();
			gdsOutUpperCase = IOTool.isGDSOutUpperCase();
			flattenHierarchy = IOTool.isSkillFlattensHierarchy();
			excludeSubcells = IOTool.isSkillExcludesSubcells();

			// get SKILL library name
			libName = Job.getUserInterface().askForInput("Library name to use in SKILL file:",
				"Name Selection", cell.getLibrary().getName());
		}

        public Output doOutput(Cell cell, VarContext context, String filePath)
        {
    		if (!hasSkill()) return null;
            Output out = null;
            try
    		{
    			out = (Output)skillOutputMethod.invoke(skillClass, new Object[] {cell, filePath, this});
    		} catch (Exception e)
    		{
                String msg = "Unable to run the Skill output module: " + e.getMessage();
                if (out != null)
                    out.reportError(msg);
                else
                    System.out.println(msg);
    		}
            return out;
        }
    }

	/****************************** DAIS FORMAT INTERFACE ******************************/

	private static boolean daisChecked = false;
	private static Class<?> daisClass = null;
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
				daisInputMethod = daisClass.getMethod("readDaisFile", new Class[] {URL.class, Library.class, boolean.class, DaisPreferences.class});
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
	 * Class to invoke the Dais input module via reflection.
	 */
	public static class DaisPreferences extends InputPreferences
    {
		private boolean newLib;
		public boolean displayOnly;
		public boolean readCellInstances;
		public boolean readGlobalWires;
		public boolean readPowerAndGround;
		public boolean readDetailWires;
		public boolean readConnectivity;

		public DaisPreferences(boolean factory, boolean newLib)
		{
			super(factory);
			this.newLib = newLib;
		}

		public void initFromUserDefaults()
		{
			displayOnly = IOTool.isDaisDisplayOnly();
			readCellInstances = IOTool.isDaisReadCellInstances();
			readGlobalWires = IOTool.isDaisReadGlobalWires();
			readPowerAndGround = IOTool.isDaisReadPowerAndGround();
			readDetailWires = IOTool.isDaisReadDetailWires();
			readConnectivity = IOTool.isDaisReadConnectivity();
		}

        @Override
        public Library doInput(URL fileURL, Library lib, Technology tech, Map<Library,Cell> currentCells, Map<CellId,BitSet> nodesToExpand, Job job)
        {
    		if (!hasDais()) return null;
    		try
    		{
    			long startTime = System.currentTimeMillis();
    			long startMemory = com.sun.electric.Main.getMemoryUsage();
    			daisInputMethod.invoke(daisClass, new Object[] {fileURL, lib, Boolean.valueOf(newLib), this});
				long endTime = System.currentTimeMillis();
				float finalTime = (endTime - startTime) / 1000F;
	    		long end = com.sun.electric.Main.getMemoryUsage();
	    		long amt = (end-startMemory)/1024/1024;
	    		System.out.println("*** DAIS INPUT TOOK " + finalTime + " seconds, " + amt + " megabytes");
    		} catch (Exception e)
    		{
    			System.out.println("Unable to run the Dais input module (" + e.getClass() + ")");
    			e.printStackTrace(System.out);
    		}
			return lib;
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
	/**
	 * Method to tell what kind of redundancy to apply when writing library files, by default.
	 * The value is:
	 * 0 for no backup (just overwrite the old file) [the default];
	 * 1 for 1-level backup (rename the old file to have a "~" at the end);
	 * 2 for full history backup (rename the old file to have date information in it).
	 * @return the level of redundancy to apply when writing library files, by default.
	 */
	public static int getFactoryBackupRedundancy() { return cacheBackupRedundancy.getIntFactoryValue(); }

	/****************************** GENERAL OUTPUT PREFERENCES ******************************/

	/**
	 * Method to tell whether to add the copyright message to output decks.
	 * The default is "false".
	 * @return true to add the copyright message to output decks.
	 */
	public static boolean isUseCopyrightMessage() { return getUseCopyrightMessageSetting().getBoolean(); }
	/**
	 * Returns project preferences to tell whether to add the copyright message to output decks.
	 * @return project preferences to tell whether to add the copyright message to output decks.
	 */
	public static Setting getUseCopyrightMessageSetting() { return ToolSettings.getUseCopyrightMessageSetting(); }

	/**
	 * Method to tell the copyright message that will be added to output decks.
	 * The default is "".
	 * @return the copyright message that will be added to output decks.
	 */
	public static String getCopyrightMessage() { return getCopyrightMessageSetting().getString(); }
	/**
	 * Returns project preferences to tell the copyright message that will be added to output decks.
	 * @return project preferences to tell the copyright message that will be added to output decks.
	 */
	public static Setting getCopyrightMessageSetting() { return ToolSettings.getCopyrightMessageSetting(); }

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
	/**
	 * Method to tell the area of the screen to plot for printing/PostScript/HPGL, by default.
	 * @return the area of the screen to plot for printing/PostScript/HPGL, by default:
	 * 0=plot the entire cell;
	 * 1=plot only the highlighted area;
	 * 2=plot only the displayed window.
	 */
	public static int getFactoryPlotArea() { return cachePlotArea.getIntFactoryValue(); }

	private static Pref cachePlotDate = Pref.makeBooleanPref("PlotDate", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether to plot the date in PostScript output.
	 * The default is "false".
	 * @return whether to plot the date in PostScript output.
	 */
	public static boolean isPlotDate() { return cachePlotDate.getBoolean(); }
	/**
	 * Method to set whether to plot the date in PostScript output.
	 * @param pd true to plot the date in PostScript output.
	 */
	public static void setPlotDate(boolean pd) { cachePlotDate.setBoolean(pd); }
	/**
	 * Method to tell whether to plot the date in PostScript output by default.
	 * @return whether to plot the date in PostScript output by default.
	 */
	public static boolean isFactoryPlotDate() { return cachePlotDate.getBooleanFactoryValue(); }

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

	/**
	 * Method to tell whether CIF Output mimics the display.
	 * To mimic the display, unexpanded cell instances are described as black boxes,
	 * instead of calls to their contents.
	 * The default is "false".
	 * @return true if CIF Output mimics the display.
	 */
	public static boolean isCIFOutMimicsDisplay() { return getCIFOutMimicsDisplaySetting().getBoolean(); }
	/**
	 * Returns Setting to tell whether CIF Output mimics the display.
	 * To mimic the display, unexpanded cell instances are described as black boxes,
	 * instead of calls to their contents.
	 * @return Setting to tell whether CIF Output mimics the display.
	 */
	public static Setting getCIFOutMimicsDisplaySetting() { return ToolSettings.getCIFOutMimicsDisplaySetting(); }

	/**
	 * Method to tell whether CIF Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * The default is "false".
	 * @return true if CIF Output merges boxes into complex polygons.
	 */
	public static boolean isCIFOutMergesBoxes() { return getCIFOutMergesBoxesSetting().getBoolean(); }
	/**
	 * Returns Setting to tell whether CIF Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * @return Setting to tell whether CIF Output merges boxes into complex polygons.
	 */
	public static Setting getCIFOutMergesBoxesSetting() { return ToolSettings.getCIFOutMergesBoxesSetting(); }

	/**
	 * Method to tell whether CIF Output instantiates the top-level.
	 * When this happens, a CIF "call" to the top cell is emitted.
	 * The default is "true".
	 * @return true if CIF Output merges boxes into complex polygons.
	 */
	public static boolean isCIFOutInstantiatesTopLevel() { return getCIFOutInstantiatesTopLevelSetting().getBoolean(); }
	/**
	 * Returns Setting to tell whether CIF Output merges boxes into complex polygons.
	 * When this happens, a CIF "call" to the top cell is emitted.
	 * @return Setting to tell whether CIF Output merges boxes into complex polygons.
	 */
	public static Setting getCIFOutInstantiatesTopLevelSetting() { return ToolSettings.getCIFOutInstantiatesTopLevelSetting(); }

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
	/**
	 * Method to tell whether CIF input makes wire ends square or round by default.
	 * The default is "true" (square).
	 * @return true if CIF input makes wire ends square by default.
	 */
	public static boolean isFactoryCIFInSquaresWires() { return cacheCIFInSquaresWires.getBooleanFactoryValue(); }

	/**
	 * Method to tell what scale factor to use for CIF Output.
	 * The scale factor is used in cell headers to avoid precision errors.
	 * The default is "1".
	 * @return the scale factor to use for CIF Output.
	 */
	public static int getCIFOutScaleFactor() { return getCIFOutScaleFactorSetting().getInt(); }
	/**
	 * Returns Setting to tell the scale factor to use for CIF Output.
	 * The scale factor is used in cell headers to avoid precision errors.
	 * @return Setting to tell the scale factor to use for CIF Output.
	 */
	public static Setting getCIFOutScaleFactorSetting() { return ToolSettings.getCIFOutScaleFactor(); }

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
	/**
	 * Method to tell whether DEF Input makes logical placement by default.
	 * The default is "true" (do the logical placement).
	 * @return true if  DEF Input makes logical placement by default.
	 */
	public static boolean isFactoryDEFLogicalPlacement() { return cacheDEFLogicalPlacement.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell whether DEF Input makes physical placement by default.
	 * The default is "true" (do the physical placement).
	 * @return true if  DEF Input makes physical placement by default.
	 */
	public static boolean isFactoryDEFPhysicalPlacement() { return cacheDEFPhysicalPlacement.getBooleanFactoryValue(); }

	/****************************** GDS PREFERENCES ******************************/

	/**
	 * Method to tell whether GDS Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * The default is "false".
	 * @return true if GDS Output merges boxes into complex polygons.
	 */
	public static boolean isGDSOutMergesBoxes() { return getGDSOutMergesBoxesSetting().getBoolean(); }
	/**
	 * Returns Setting to tell whether GDS Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * @return Setting to tell if GDS Output merges boxes into complex polygons.
	 */
	public static Setting getGDSOutMergesBoxesSetting() { return ToolSettings.getGDSOutMergesBoxesSetting(); }

	/**
	 * Method to tell whether GDS Output writes pins at Export locations.
	 * Some systems can use this information to reconstruct export locations.
	 * The default is "false".
	 * @return true if GDS Output writes pins at Export locations.
	 */
	public static boolean isGDSOutWritesExportPins() { return getGDSOutWritesExportPinsSetting().getBoolean(); }
	/**
	 * Returns Setting to tell whether GDS Output writes pins at Export locations.
	 * Some systems can use this information to reconstruct export locations.
	 * @return Setting to tell whether GDS Output writes pins at Export locations.
	 */
	public static Setting getGDSOutWritesExportPinsSetting() { return ToolSettings.getGDSOutWritesExportPinsSetting(); }

	/**
	 * Method to tell whether GDS Output makes all text upper-case.
	 * Some systems insist on this.
	 * The default is "false".
	 * @return true if GDS Output makes all text upper-case.
	 */
	public static boolean isGDSOutUpperCase() { return getGDSOutUpperCaseSetting().getBoolean(); }
	/**
	 * Returns Setting to tell whether GDS Output makes all text upper-case.
	 * Some systems insist on this.
	 * @return Setting to tell whether GDS Output makes all text upper-case.
	 */
	public static Setting getGDSOutUpperCaseSetting() { return ToolSettings.getGDSOutUpperCaseSetting(); }

	/**
	 * Method to tell the default GDS layer to use for the text of Export pins.
	 * Export pins are annotated with text objects on this layer.
	 * If this is negative, do not write Export pins.
	 * The default is "230".
	 * @return the default GDS layer to use for the text of Export pins.
	 */
	public static int getGDSOutDefaultTextLayer() { return getGDSOutDefaultTextLayerSetting().getInt(); }
	/**
	 * Returns Setting to tell the default GDS layer to use for the text of Export pins.
	 * Export pins are annotated with text objects on this layer.
	 * If this is negative, do not write Export pins.
	 * @return Setting to tell to set the default GDS layer to use for the text of Export pins.
	 */
	public static Setting getGDSOutDefaultTextLayerSetting() { return ToolSettings.getGDSOutDefaultTextLayerSetting(); }

	/**
	 * Method to get the state of whether the GDS writer converts brackets
	 * to underscores in export names.
	 */
	public static boolean getGDSOutputConvertsBracketsInExports() { return getGDSOutputConvertsBracketsInExportsSetting().getBoolean(); }
	/**
	 * Returns Setting to tell the state of whether the GDS writer converts brackets
	 * to underscores in export names.
	 * @return Setting to tell the state of whether the GDS writer converts brackets
	 */
	public static Setting getGDSOutputConvertsBracketsInExportsSetting() { return ToolSettings.getGDSOutputConvertsBracketsInExportsSetting(); }

	/**
	 * Get the maximum length (number of chars) for Cell names in the GDS output file
	 * @return the number of chars
	 */
	public static int getGDSCellNameLenMax() { return getGDSCellNameLenMaxSetting().getInt(); }
	/**
	 * Returns Setting to tell the maximum length (number of chars) for Cell names in the GDS output file
	 * @return Setting to tell the maximum length (number of chars) for Cell names in the GDS output file
	 */
	public static Setting getGDSCellNameLenMaxSetting() { return ToolSettings.getGDSCellNameLenMaxSetting(); }

	/**
	 * Method to tell the scale to be applied when reading GDS.
	 * The default is 1 (no scaling).
	 * @return the scale to be applied when reading GDS.
	 */
	public static double getGDSInputScale() { return getGDSInputScaleSetting().getDouble(); }
	/**
	 * Method to set the scale to be applied when reading GDS.
	 * @return the scale to be applied when reading GDS.
	 */
	public static Setting getGDSInputScaleSetting() { return ToolSettings.getGDSInputScaleSetting(); }

    /**
	 * Method to tell the scale to be applied when writing GDS.
	 * The default is 1 (no scaling).
	 * @return the scale to be applied when writing GDS.
	 */
	public static double getGDSOutputScale() { return getGDSOutputScaleSetting().getDouble(); }
	/**
	 * Method to set the scale to be applied when writing GDS.
	 * @return the scale to be applied when writing GDS.
	 */
	public static Setting getGDSOutputScaleSetting() { return ToolSettings.getGDSOutputScaleSetting(); }

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
	/**
	 * Method to tell whether GDS Input merges boxes into complex polygons, by default.
	 * This takes more time but produces a smaller database.
	 * @return true if GDS Input merges boxes into complex polygons, by default.
	 */
	public static boolean isFactoryGDSInMergesBoxes() { return cacheGDSInMergesBoxes.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell whether GDS Input ignores text, by default.
	 * Text can clutter the display, so some users don't want to read it.
	 * @return true if GDS Input ignores text, by default.
	 */
	public static boolean isFactoryGDSInIncludesText() { return cacheGDSInIncludesText.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell whether GDS Input expands cells, by default.
	 * @return true if GDS Input expands cells, by default.
	 */
	public static boolean isFactoryGDSInExpandsCells() { return cacheGDSInExpandsCells.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell whether GDS Input instantiates arrays, by default.
	 * When false, only the edges of arrays are instantiated, not those entries in the center.
	 * @return true if GDS Input instantiates arrays, by default.
	 */
	public static boolean isFactoryGDSInInstantiatesArrays() { return cacheGDSInInstantiatesArrays.getBooleanFactoryValue(); }

	public static final int GDSUNKNOWNLAYERIGNORE    = 0;
	public static final int GDSUNKNOWNLAYERUSEDRC    = 1;
	public static final int GDSUNKNOWNLAYERUSERANDOM = 2;
	private static Pref cacheGDSInUnknownLayerHandling = Pref.makeIntPref("GDSInUnknownLayerHandling", IOTool.tool.prefs, GDSUNKNOWNLAYERUSEDRC);
	/**
	 * Method to tell how GDS Input handles unknown layers.
	 * The choices are:<BR>
	 * 0: ignore anything on the layer.<BR>
	 * 1: convert that layer to the "DRC exclusion" layer (the default).<BR>
	 * 2: convert that layer to an unused layer.<BR>
	 * @return how GDS Input handles unknown layers.
	 */
	public static int getGDSInUnknownLayerHandling() { return cacheGDSInUnknownLayerHandling.getInt(); }
	/**
	 * Method to set how GDS Input handles unknown layers.
	 * The choices are:<BR>
	 * 0: ignore anything on the layer.<BR>
	 * 1: convert that layer to the "DRC exclusion" layer.<BR>
	 * 2: convert that layer to an unused layer.<BR>
	 * @param h how GDS Input handles unknown layers.
	 */
	public static void setGDSInUnknownLayerHandling(int h) { cacheGDSInUnknownLayerHandling.setInt(h); }
	/**
	 * Method to tell how GDS Input handles unknown layers, by default.
	 * The choices are:<BR>
	 * 0: ignore anything on the layer.<BR>
	 * 1: convert that layer to the "DRC exclusion" layer.<BR>
	 * 2: convert that layer to an unused layer.<BR>
	 * @return how GDS Input handles unknown layers, by default.
	 */
	public static int getFactoryGDSInUnknownLayerHandling() { return cacheGDSInUnknownLayerHandling.getIntFactoryValue(); }

	private static Pref cacheGDSConvertNCCExportsConnectedByParentPins = Pref.makeBooleanPref("GDSConvertNCCEconnectedByParentPins", IOTool.tool.prefs, false);
	/**
	 * True to convert pin names to name:name for pins that are specified in the
	 * NCC annotation, "exportsConnectedByParent".  This allows external LVS tools to
	 * perform the analogous operation of virtual connection of networks.
	 * For example, 'exportsConnectedByParent vdd /vdd_[0-9]+/' will rename all
	 * pins that match the assertion to vdd:vdd.
     * @return True if pin names should be converted.
	 */
	public static boolean getGDSConvertNCCExportsConnectedByParentPins() { return cacheGDSConvertNCCExportsConnectedByParentPins.getBoolean(); }
	/**
	 * True to convert pin names to name:name for pins that are specified in the
	 * NCC annotation, "exportsConnectedByParent".  This allows external LVS tools to
	 * perform the analogous operation of virtual connection of networks.
	 * For example, 'exportsConnectedByParent vdd /vdd_[0-9]+/' will rename all
	 * pins that match the assertion to vdd:vdd.
     * @param b true if pin maes should be converted.
	 */
	public static void setGDSConvertNCCExportsConnectedByParentPins(boolean b) { cacheGDSConvertNCCExportsConnectedByParentPins.setBoolean(b); }
	/**
	 * True to convert pin names to name:name for pins that are specified in the
	 * NCC annotation, "exportsConnectedByParent" (by default).  This allows external LVS tools to
	 * perform the analogous operation of virtual connection of networks.
	 * For example, 'exportsConnectedByParent vdd /vdd_[0-9]+/' will rename all
	 * pins that match the assertion to vdd:vdd.
     * @return True if the factory value requires to convert the pin names.
	 */
	public static boolean getFactoryGDSConvertNCCExportsConnectedByParentPins() { return cacheGDSConvertNCCExportsConnectedByParentPins.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell whether GDS Input simplifies contact vias, by default.
	 * @return true if GDS Input simplifies contact vias, by default.
	 */
	public static boolean isFactoryGDSInSimplifyCells() { return cacheGDSInSimplifyCells.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell whether Vdd_* and Gnd_* export pins must be colapsed, by default. This is for extraction in Fire/Ice.
	 * @return true if GDS Input colapses vdd/gnd names, by default.
	 */
	public static boolean isFactoryGDSColapseVddGndPinNames() { return cacheGDSColapseVddGndPinNames.getBooleanFactoryValue(); }

	private static Pref cacheGDSArraySimplification = Pref.makeIntPref("cacheGDSArraySimplification", IOTool.tool.prefs, 0);
	/**
	 * Method to tell how GDS input should simplify array references (AREFs).
	 * Choices are: 0=none (default), 1=merge each AREF, 2=merge all AREFs in a cell
	 * @return how GDS input should simplify array references (AREFs).
	 */
	public static int getGDSArraySimplification() { return cacheGDSArraySimplification.getInt(); }
	/**
	 * Method to set how GDS input should simplify array references (AREFs).
	 * Choices are: 0=none (default), 1=merge each AREF, 2=merge all AREFs in a cell
	 * @param on how GDS input should simplify array references (AREFs).
	 */
	public static void setGDSArraySimplification(int on) { cacheGDSArraySimplification.setInt(on); }
	/**
	 * Method to tell how GDS input should simplify array references (AREFs), by default.
	 * Choices are: 0=none (default), 1=merge each AREF, 2=merge all AREFs in a cell
	 * @return how GDS input should simplify array references (AREFs), by default.
	 */
	public static int getFactoryGDSArraySimplification() { return cacheGDSArraySimplification.getIntFactoryValue(); }

	private static Pref cacheGDSCadenceCompatibility = Pref.makeBooleanPref("GDSCadenceCompatibility", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether GDS input is compatible with Cadence.
	 * Cadence compatibility adjusts export locations to be centered in the geometry.
	 * The default is "true".
	 * @return true if GDS input is compatible with Cadence.
	 */
	public static boolean isGDSCadenceCompatibility() { return cacheGDSCadenceCompatibility.getBoolean(); }
	/**
	 * Method to set whether GDS input is compatible with Cadence.
	 * Cadence compatibility adjusts export locations to be centered in the geometry.
	 * @param c true if GDS input is compatible with Cadence.
	 */
	public static void setGDSCadenceCompatibility(boolean c) { cacheGDSCadenceCompatibility.setBoolean(c); }
	/**
	 * Method to tell whether GDS input is compatible with Cadence, by default.
	 * Cadence compatibility adjusts export locations to be centered in the geometry.
	 * @return true if GDS input is compatible with Cadence, by default.
	 */
	public static boolean isFactoryGDSCadenceCompatibility() { return cacheGDSCadenceCompatibility.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell whether PostScript Output is Encapsulated by default.
	 * Encapsulated PostScript can be inserted into other documents.
	 * @return true if PostScript Output is Encapsulated by default.
	 */
	public static boolean isFactoryPrintEncapsulated() { return cachePrintEncapsulated.getBooleanFactoryValue(); }

	private static Pref cachePrintResolution = Pref.makeIntPref("PrintResolution", IOTool.tool.prefs, 300);
	/**
	 * Method to tell the default printing resolution.
	 * Java printing assumes 72 DPI, this is an override.
	 * The factory default is "300".
	 * @return the default printing resolution.
	 */
	public static int getPrintResolution() { return cachePrintResolution.getInt(); }
	/**
	 * Method to set the default printing resolution.
	 * Java printing assumes 72 DPI, this is an override.
	 * @param r the default printing resolution.
	 */
	public static void setPrintResolution(int r) { cachePrintResolution.setInt(r); }
	/**
	 * Method to tell the factory default printing resolution.
	 * Java printing assumes 72 DPI, this is an override.
	 * @return the factory default printing resolution.
	 */
	public static int getFactoryPrintResolution() { return cachePrintResolution.getIntFactoryValue(); }

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
	/**
	 * Method to tell whether PostScript Output is for a plotter, by default.
	 * Plotters have width, but no height, since they are continuous feed.
	 * @return true if PostScript Output is for a plotter, by default.
	 */
	public static boolean isFactoryPrintForPlotter() { return cachePrintForPlotter.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell the width of PostScript Output, by default.
	 * The width is in inches.
	 * @return the width of PostScript Output, by default.
	 */
	public static double getFactoryPrintWidth() { return cachePrintWidth.getDoubleFactoryValue(); }

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
	/**
	 * Method to tell the height of PostScript Output, by default.
	 * The height is in inches, and only applies if printing (not plotting).
	 * @return the height of PostScript Output, by default.
	 */
	public static double getFactoryPrintHeight() { return cachePrintHeight.getDoubleFactoryValue(); }

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
	/**
	 * Method to tell the margin of PostScript Output, by default.
	 * The margin is in inches and insets from all sides.
	 * @return the margin of PostScript Output, by default.
	 */
	public static double getFactoryPrintMargin() { return cachePrintMargin.getDoubleFactoryValue(); }

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
	/**
	 * Method to tell the rotation of PostScript Output, by default.
	 * The plot can be normal or rotated 90 degrees to better fit the paper.
	 * @return the rotation of PostScript Output, by default:
	 * 0=no rotation;
	 * 1=rotate 90 degrees;
	 * 2=rotate automatically to fit best.
	 */
	public static int getFactoryPrintRotation() { return cachePrintRotation.getIntFactoryValue(); }

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
	/**
	 * Method to tell the color method of PostScript Output, by default.
	 * @return the color method of PostScript Output, by default:
	 * 0=Black & White;
	 * 1=Color (solid);
	 * 2=Color (stippled);
	 * 3=Color (merged).
	 */
	public static int getFactoryPrintColorMethod() { return cachePrintColorMethod.getIntFactoryValue(); }

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
		Integer [] lastSavedDateAsInts = cell.getVarValue(POSTSCRIPT_FILEDATE, Integer[].class);
		if (lastSavedDateAsInts == null) return null;
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
	/**
	 * Method to tell the width of PostScript lines, by default.
	 * Lines have their width scaled by this amount, so 1 means normal lines.
	 * @return the width of PostScript lines, by default.
	 */
	public static double getFactoryPrintPSLineWidth() { return cachePrintPSLineWidth.getDoubleFactoryValue(); }

	/****************************** EDIF PREFERENCES ******************************/

	private static Pref cacheEDIFUseSchematicView = Pref.makeBooleanPref("EDIFUseSchematicView", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether EDIF uses the schematic view.
	 * The default is "true".
	 * @return true if EDIF uses the schematic view.
	 */
	public static boolean isEDIFUseSchematicView() { return cacheEDIFUseSchematicView.getBoolean(); }
	/**
	 * Method to set whether EDIF uses the schematic view.
	 * @param f true if EDIF uses the schematic view.
	 */
	public static void setEDIFUseSchematicView(boolean f) { cacheEDIFUseSchematicView.setBoolean(f); }
	/**
	 * Method to tell whether EDIF uses the schematic view, by default.
	 * @return true if EDIF uses the schematic view, by default.
	 */
	public static boolean isFactoryEDIFUseSchematicView() { return cacheEDIFUseSchematicView.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell whether EDIF I/O is compatible with Cadence, by default.
	 * @return true if EDIF I/O is compatible with Cadence, by default.
	 */
	public static boolean isFactoryEDIFCadenceCompatibility() { return cacheEDIFCadenceCompatibility.getBooleanFactoryValue(); }

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
	/**
	 * Method to return the EDIF input scale, by default.
	 * @return the EDIF input scale, by default.
	 */
	public static double getFactoryEDIFInputScale() { return cacheEDIFInputScale.getDoubleFactoryValue(); }

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
	/**
	 * Method to tell the configuration file to use, by default.
	 * @return the configuration file to use, by default.
	 */
	public static String getFactoryEDIFConfigurationFile() { return cacheEDIFConfigurationFile.getStringFactoryValue(); }

	private static Pref cacheEDIFAcceptedParameters = Pref.makeStringPref("EDIFAcceptedParameters", IOTool.tool.prefs, "");
	/**
	 * Method to return a string with accepted parameter names.
	 * These parameter names will be placed on cells when reading EDIF.
	 * The string lists the names, separated by slashes.
	 * The default is "" (no parameters accepted).
	 * @return a string with accepted parameter names.
	 */
	public static String getEDIFAcceptedParameters() { return cacheEDIFAcceptedParameters.getString(); }
	/**
	 * Method to set a string with accepted parameter names.
	 * These parameter names will be placed on cells when reading EDIF.
	 * The string lists the names, separated by slashes.
	 * @param ap the accepted parameter names.
	 */
	public static void setEDIFAcceptedParameters(String ap) { cacheEDIFAcceptedParameters.setString(ap); }
	/**
	 * Method to return a string with accepted parameter names, by default.
	 * These parameter names will be placed on cells when reading EDIF.
	 * The string lists the names, separated by slashes.
	 * @return a string with accepted parameter names, by default.
	 */
	public static String getFactoryEDIFAcceptedParameters() { return cacheEDIFAcceptedParameters.getStringFactoryValue(); }

	/****************************** DXF PREFERENCES ******************************/

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
	public static int getDXFScale() { return getDXFScaleSetting().getInt(); }
	/**
	 * Returns project preferences to tell the DXF scale.
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
	 * @return project preferences to tell the DXF scale.
	 */
	public static Setting getDXFScaleSetting() { return ToolSettings.getDXFScaleSetting(); }

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
	/**
	 * Method to tell whether DXF Input flattens the hierarchy by default.
	 * Flattened DXF appears in a single cell.
	 * @return true if DXF Input flattens the hierarchy by default.
	 */
	public static boolean isFactoryDXFInputFlattensHierarchy() { return cacheDXFInputFlattensHierarchy.getBooleanFactoryValue(); }

	private static Pref cacheDXFInputReadsAllLayers = Pref.makeBooleanPref("DXFInputReadsAllLayers", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether DXF input reads all layers.
	 * When a DXF layer in the file is unknown, it is ignored if all layers are NOT being read;
	 * it is converted to another layer if all layers ARE being read.
	 * The default is "true".
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
	/**
	 * Method to tell whether DXF input reads all layers by default.
	 * When a DXF layer in the file is unknown, it is ignored if all layers are NOT being read;
	 * it is converted to another layer if all layers ARE being read.
	 * @return true if DXF input reads all layers by default.
	 */
	public static boolean isFactoryDXFInputReadsAllLayers() { return cacheDXFInputReadsAllLayers.getBooleanFactoryValue(); }

	/****************************** SUE INPUT PREFERENCES ******************************/

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
	/**
	 * Method to tell whether Sue input creates 4-port transistors by default.
	 * Without this, standard 3-port transistors are created.
	 * @return true if Sue input creates 4-port transistors by default.
	 */
	public static boolean isFactorySueUses4PortTransistors() { return cacheSueUses4PortTransistors.getBooleanFactoryValue(); }

	private static Pref cacheSueConvertsExpressions = Pref.makeBooleanPref("SueConvertsExpressions", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether Sue input converts Sue expressions to Electric form.
	 * Electric expressions have "@" in front of variables.
	 * The default is "false".
	 * @return true if Sue input converts Sue expressions to Electric form.
	 */
	public static boolean isSueConvertsExpressions() { return cacheSueConvertsExpressions.getBoolean(); }
	/**
	 * Method to set whether Sue input converts Sue expressions to Electric form.
	 * Electric expressions have "@" in front of variables.
	 * @param on true if Sue input converts Sue expressions to Electric form.
	 */
	public static void setSueConvertsExpressions(boolean on) { cacheSueConvertsExpressions.setBoolean(on); }
	/**
	 * Method to tell whether Sue input converts Sue expressions to Electric form by default.
	 * Electric expressions have "@" in front of variables.
	 * @return true if Sue input converts Sue expressions to Electric form by default.
	 */
	public static boolean isFactorySueConvertsExpressions() { return cacheSueConvertsExpressions.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell whether Skill Output excludes subcells by default.
	 * If subcells are included, a Skill output files have multiple cell definitions in them.
	 * @return true if Skill Output excludes subcells by default.
	 */
	public static boolean isFactorySkillExcludesSubcells() { return cacheSkillExcludesSubcells.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell whether Skill Output flattens the hierarchy by default.
	 * Flattened files are larger, but have no hierarchical structure.
	 * @return true if Skill Output flattens the hierarchy by default.
	 */
	public static boolean isFactorySkillFlattensHierarchy() { return cacheSkillFlattensHierarchy.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell whether Skill Output flattens the hierarchy by default.
	 * Flattened files are larger, but have no hierarchical structure.
	 * @return true if Skill Output flattens the hierarchy by default.
	 */
	public static boolean isFactorySkillGDSNameLimit() { return cacheSkillGDSNameLimit.getBooleanFactoryValue(); }

	/****************************** DAIS INPUT PREFERENCES ******************************/

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
	/**
	 * Method to tell whether Dais Input creates real geometry, by default.
	 * When real geometry is created, it takes more time and memory, but the circuitry can be edited.
	 * When false, Dais input is read directly into the display system for rapid viewing.
	 * @return true if Dais Input creates real geometry, by default.
	 */
	public static boolean isFactoryDaisDisplayOnly() { return cacheDaisDisplayOnly.getBooleanFactoryValue(); }

	private static Pref cacheDaisReadCellInstances = Pref.makeBooleanPref("DaisReadCellInstances", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether Dais Input places cell instances.
	 * The default is "true".
	 * @return true if Dais Input places cell instances.
	 */
	public static boolean isDaisReadCellInstances() { return cacheDaisReadCellInstances.getBoolean(); }
	/**
	 * Method to set whether Dais Input places cell instances.
	 * @param on true if Dais Input places cell instances.
	 */
	public static void setDaisReadCellInstances(boolean on) { cacheDaisReadCellInstances.setBoolean(on); }
	/**
	 * Method to tell whether Dais Input places cell instances, by default.
	 * @return true if Dais Input places cell instances, by default.
	 */
	public static boolean isFactoryDaisReadCellInstances() { return cacheDaisReadCellInstances.getBooleanFactoryValue(); }

	private static Pref cacheDaisReadDetailWires = Pref.makeBooleanPref("DaisReadDetailWires", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether Dais Input reads the "detail" wires.
	 * The default is "true".
	 * @return true if Dais Input reads the "detail" wires.
	 */
	public static boolean isDaisReadDetailWires() { return cacheDaisReadDetailWires.getBoolean(); }
	/**
	 * Method to set whether Dais Input reads the "detail" wires.
	 * @param on true if Dais Input reads the "detail" wires.
	 */
	public static void setDaisReadDetailWires(boolean on) { cacheDaisReadDetailWires.setBoolean(on); }
	/**
	 * Method to tell whether Dais Input reads the "detail" wires, by default.
	 * @return true if Dais Input reads the "detail" wires, by default.
	 */
	public static boolean isFactoryDaisReadDetailWires() { return cacheDaisReadDetailWires.getBooleanFactoryValue(); }

	private static Pref cacheDaisReadGlobalWires = Pref.makeBooleanPref("DaisReadGlobalWires", IOTool.tool.prefs, false);
	/**
	 * Method to tell whether Dais Input reads the "global" wires.
	 * The default is "false".
	 * @return true if Dais Input reads the "global" wires.
	 */
	public static boolean isDaisReadGlobalWires() { return cacheDaisReadGlobalWires.getBoolean(); }
	/**
	 * Method to set whether Dais Input reads the "global" wires.
	 * @param on true if Dais Input reads the "global" wires.
	 */
	public static void setDaisReadGlobalWires(boolean on) { cacheDaisReadGlobalWires.setBoolean(on); }
	/**
	 * Method to tell whether Dais Input reads the "global" wires, by default.
	 * @return true if Dais Input reads the "global" wires, by default.
	 */
	public static boolean isFactoryDaisReadGlobalWires() { return cacheDaisReadGlobalWires.getBooleanFactoryValue(); }

	private static Pref cacheDaisReadPowerAndGround = Pref.makeBooleanPref("DaisReadPowerAndGround", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether Dais Input reads power and ground wires.
	 * The default is "true".
	 * @return true if Dais Input reads power and ground wires.
	 */
	public static boolean isDaisReadPowerAndGround() { return cacheDaisReadPowerAndGround.getBoolean(); }
	/**
	 * Method to set whether Dais Input reads power and ground wires.
	 * @param on true if Dais Input reads power and ground wires.
	 */
	public static void setDaisReadPowerAndGround(boolean on) { cacheDaisReadPowerAndGround.setBoolean(on); }
	/**
	 * Method to tell whether Dais Input reads power and ground wires, by default.
	 * @return true if Dais Input reads power and ground wires, by default.
	 */
	public static boolean isFactoryDaisReadPowerAndGround() { return cacheDaisReadPowerAndGround.getBooleanFactoryValue(); }

	private static Pref cacheDaisReadConnectivity = Pref.makeBooleanPref("DaisReadConnectivity", IOTool.tool.prefs, true);
	/**
	 * Method to tell whether Dais Input reads connectivity.
	 * Connectivity is represented by "unrouted" arcs from the Generic technology,
	 * which appear in a "rats nest" on the circuit.
	 * The default is "true".
	 * @return true if Dais Input reads connectivity.
	 */
	public static boolean isDaisReadConnectivity() { return cacheDaisReadConnectivity.getBoolean(); }
	/**
	 * Method to set whether Dais Input reads connectivity.
	 * Connectivity is represented by "unrouted" arcs from the Generic technology,
	 * which appear in a "rats nest" on the circuit.
	 * @param on true if Dais Input reads connectivity.
	 */
	public static void setDaisReadConnectivity(boolean on) { cacheDaisReadConnectivity.setBoolean(on); }
	/**
	 * Method to tell whether Dais Input reads connectivity, by default.
	 * Connectivity is represented by "unrouted" arcs from the Generic technology,
	 * which appear in a "rats nest" on the circuit.
	 * @return true if Dais Input reads connectivity, by default.
	 */
	public static boolean isFactoryDaisReadConnectivity() { return cacheDaisReadConnectivity.getBooleanFactoryValue(); }
}

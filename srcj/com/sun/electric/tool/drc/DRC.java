/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DRC.java
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
package com.sun.electric.tool.drc;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.utils.MOSRules;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.plugins.tsmc90.TSMCRules;
import com.sun.electric.Main;

import java.util.*;
import java.util.prefs.Preferences;
import java.awt.geom.Rectangle2D;

/**
 * This is the Design Rule Checker tool.
 */
public class DRC extends Listener
{
	/** the DRC tool. */								public static DRC tool = new DRC();
	/** overrides of rules for each technology. */		private static HashMap prefDRCOverride = new HashMap();

	/****************************** DESIGN RULES ******************************/

    public static class NodeSizeRule
	{
		public double sizeX, sizeY;
		public String rule;

		NodeSizeRule(double sizeX, double sizeY, String rule)
		{
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.rule = rule;
		}
	}

	/****************************** TOOL CONTROL ******************************/

	/**
	 * The constructor sets up the DRC tool.
	 */
	private DRC()
	{
		super("drc");
	}

    /**
	 * Method to initialize the DRC tool.
	 */
	public void init()
	{
		setOn();
	}

	/** map of cells and their objects to DRC */		private static HashMap cellsToCheck = new HashMap();
	private static boolean incrementalRunning = false;

	private static void includeGeometric(Geometric geom)
	{
		if (!isIncrementalDRCOn()) return;
		Cell cell = geom.getParent();
		synchronized (cellsToCheck)
		{
			HashSet cellSet = (HashSet)cellsToCheck.get(cell);
			if (cellSet == null)
			{
				cellSet = new HashSet();
				cellsToCheck.put(cell, cellSet);
			}
			cellSet.add(geom);
		}
	}

	private static void removeGeometric(Geometric geom)
	{
		if (!isIncrementalDRCOn()) return;
		Cell cell = geom.getParent();
		synchronized (cellsToCheck)
		{
			HashSet cellSet = (HashSet)cellsToCheck.get(cell);
			if (cellSet != null) cellSet.remove(geom);
		}
	}

	private static void doIncrementalDRCTask()
	{
		if (!isIncrementalDRCOn()) return;
		if (incrementalRunning) return;

		Library curLib = Library.getCurrent();
		if (curLib == null) return;
		Cell cellToCheck = curLib.getCurCell();
		HashSet cellSet = null;

		// get a cell to check
		synchronized (cellsToCheck)
		{
			if (cellToCheck != null)
				cellSet = (HashSet)cellsToCheck.get(cellToCheck);
			if (cellSet == null && cellsToCheck.size() > 0)
			{
				cellToCheck = (Cell)cellsToCheck.keySet().iterator().next();
				cellSet = (HashSet)cellsToCheck.get(cellToCheck);
			}
			if (cellSet != null)
				cellsToCheck.remove(cellToCheck);
		}

        // don't check if cell not in database anymore
        if (cellToCheck != null && !cellToCheck.isLinked()) return;

		// if there is a cell to check, do it
		if (cellSet != null)
		{
			Geometric [] objectsToCheck = new Geometric[cellSet.size()];
			int i = 0;
			for(Iterator it = cellSet.iterator(); it.hasNext(); )
				objectsToCheck[i++] = (Geometric)it.next();
			CheckLayoutIncrementally job = new CheckLayoutIncrementally(cellToCheck, objectsToCheck);
		}
	}

	private static class CheckLayoutIncrementally extends Job
	{
		Cell cell;
		Geometric [] objectsToCheck;

		protected CheckLayoutIncrementally(Cell cell, Geometric [] objectsToCheck)
		{
			super("DRC in cell " + cell.describe(), tool, Job.Type.EXAMINE, null, null, Job.Priority.ANALYSIS);
			this.cell = cell;
			this.objectsToCheck = objectsToCheck;
			startJob();
		}

		public boolean doIt()
		{
			incrementalRunning = true;
			long startTime = System.currentTimeMillis();
			int errorsFound = Quick.checkDesignRules(cell, objectsToCheck.length, objectsToCheck, null, null);
			long endTime = System.currentTimeMillis();
			if (errorsFound > 0)
			{
				System.out.println("Incremental DRC found " + errorsFound + " errors in cell "+cell.describe());
			}
			incrementalRunning = false;
			doIncrementalDRCTask();
			return true;
		}
	}

	/**
	 * Method to announce the end of a batch of changes.
	 */
	public void endBatch()
	{
		doIncrementalDRCTask();
	}

	/**
	 * Method to announce a change to a NodeInst.
	 * @param ni the NodeInst that was changed.
	 * @param oCX the old X center of the NodeInst.
	 * @param oCY the old Y center of the NodeInst.
	 * @param oSX the old X size of the NodeInst.
	 * @param oSY the old Y size of the NodeInst.
	 * @param oRot the old rotation of the NodeInst.
	 */
	public void modifyNodeInst(NodeInst ni, double oCX, double oCY, double oSX, double oSY, int oRot)
	{
		includeGeometric(ni);
	}

	/**
	 * Method to announce a change to many NodeInsts at once.
	 * @param nis the NodeInsts that were changed.
	 * @param oCX the old X centers of the NodeInsts.
	 * @param oCY the old Y centers of the NodeInsts.
	 * @param oSX the old X sizes of the NodeInsts.
	 * @param oSY the old Y sizes of the NodeInsts.
	 * @param oRot the old rotations of the NodeInsts.
	 */
	public void modifyNodeInsts(NodeInst [] nis, double [] oCX, double [] oCY, double [] oSX, double [] oSY, int [] oRot)
	{
		for(int i=0; i<nis.length; i++)
			includeGeometric(nis[i]);
	}

	/**
	 * Method to announce a change to an ArcInst.
	 * @param ai the ArcInst that changed.
	 * @param oHX the old X coordinate of the ArcInst head end.
	 * @param oHY the old Y coordinate of the ArcInst head end.
	 * @param oTX the old X coordinate of the ArcInst tail end.
	 * @param oTY the old Y coordinate of the ArcInst tail end.
	 * @param oWid the old width of the ArcInst.
	 */
	public void modifyArcInst(ArcInst ai, double oHX, double oHY, double oTX, double oTY, double oWid)
	{
		includeGeometric(ai);
	}

	/**
	 * Method to announce the creation of a new ElectricObject.
	 * @param obj the ElectricObject that was just created.
	 */
	public void newObject(ElectricObject obj)
	{
		if (obj instanceof Geometric)
		{
			includeGeometric((Geometric)obj);
		}
	}

	/**
	 * Method to announce the deletion of an ElectricObject.
	 * @param obj the ElectricObject that was just deleted.
	 */
	public void killObject(ElectricObject obj)
	{
		if (obj instanceof Geometric)
		{
			removeGeometric((Geometric)obj);
		}
	}

	/****************************** DRC INTERFACE ******************************/

	/**
	 * Method to check the current cell hierarchically.
	 */
	public static void checkHierarchically()
	{
		Cell curCell = WindowFrame.needCurCell();

		if (curCell == null) return;
		if (curCell.getView() == View.SCHEMATIC || curCell.getTechnology() == Schematics.tech ||
			curCell.getView() == View.ICON || curCell.getTechnology() == Artwork.tech)
		{
			// hierarchical check of schematics
			CheckSchematicHierarchically job = new CheckSchematicHierarchically(curCell, null);
		} else
		{
			// hierarchical check of layout
			CheckLayoutHierarchically job = new CheckLayoutHierarchically(curCell, null);
		}
	}

	/**
	 * Method to check the selected area of the current cell hierarchically.
	 */
	public static void checkAreaHierarchically()
	{
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
        Highlighter h = wnd.getHighlighter();
        Rectangle2D bounds = h.getHighlightedArea(wnd);

        Cell curCell = wnd.getCell();
		if (curCell == null) return;
		if (curCell.getView() == View.SCHEMATIC || curCell.getTechnology() == Schematics.tech ||
			curCell.getView() == View.ICON || curCell.getTechnology() == Artwork.tech)
		{
			// hierarchical check of schematics
			CheckSchematicHierarchically job = new CheckSchematicHierarchically(curCell, bounds);
		} else
		{
			// hierarchical check of layout
			CheckLayoutHierarchically job = new CheckLayoutHierarchically(curCell, bounds);
		}
	}

	private static class CheckLayoutHierarchically extends Job
	{
		Cell cell;
		Rectangle2D bounds;

        /**
         * Check bounds within cell. If bounds is null, check entire cell.
         * @param cell
         * @param bounds
         */
		protected CheckLayoutHierarchically(Cell cell, Rectangle2D bounds)
		{
			super("Design-Rule Check", tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.bounds = bounds;
			startJob();
		}

		public boolean doIt()
		{
			long startTime = System.currentTimeMillis();
			int errorsFound = Quick.checkDesignRules(cell, 0, null, null, bounds);
			long endTime = System.currentTimeMillis();
			System.out.println(errorsFound + " errors found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
			return true;
		}
	}

	private static class CheckSchematicHierarchically extends Job
	{
		Cell cell;
		Rectangle2D bounds;

        /**
         * Check bounds within Cell.  If bounds is null, check entire cell.
         * @param cell
         * @param bounds
         */
		protected CheckSchematicHierarchically(Cell cell, Rectangle2D bounds)
		{
			super("Design-Rule Check", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.bounds = bounds;
			startJob();
		}

		public boolean doIt()
		{
			long startTime = System.currentTimeMillis();
			Schematic.doCheck(cell);
			long endTime = System.currentTimeMillis();
			int errorcount = ErrorLogger.getCurrent().numErrors();
			System.out.println(errorcount + " errors found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
			return true;
		}
	}

	/**
	 * Method to delete all cached date information on all cells.
	 */
	public static void resetDRCDates()
	{
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				Variable var = cell.getVar(DRCRules.LAST_GOOD_DRC);
				if (var == null) continue;
				cell.delVar(DRCRules.LAST_GOOD_DRC);
			}
		}
	}

	/****************************** DESIGN RULE CONTROL ******************************/

	/** Cached rules for a specific technology. */		private static DRCRules currentRules = null;
	/** The Technology whose rules are cached. */		private static Technology currentTechnology = null;

	/**
	 * Method to build a Rules object that contains the current design rules for a Technology.
	 * The DRC dialogs use this to hold the values while editing them.
	 * It also provides a cache for the design rule checker.
	 * @param tech the Technology to examine.
	 * @return a new Rules object with the design rules for the given Technology.
	 */
	public static DRCRules getRules(Technology tech)
	{
		if (currentRules != null && tech == currentTechnology) return currentRules;

		// constructing design rules: start with factory rules
		currentRules = tech.getFactoryDesignRules();
		if (currentRules != null)
		{
			// add overrides
			StringBuffer override = getDRCOverrides(tech);
			currentRules.applyDRCOverrides(override.toString(), tech);
		}

		// remember technology whose rules are cached
		currentTechnology = tech;
		return currentRules;
	}

	/**
	 * Method to load a full set of design rules for a Technology.
	 * @param tech the Technology to load.
	 * @param newRules a complete design rules object.
	 */
	public static void setRules(Technology tech, DRCRules newRules)
	{
		// get factory design rules
		DRCRules factoryRules = tech.getFactoryDesignRules();

		// determine override differences from the factory rules
		StringBuffer changes = tech.getRuleDifferences(factoryRules, newRules);

		// get current overrides of factory rules
		StringBuffer override = getDRCOverrides(tech);

		// if the differences are the same as before, stop
		if (changes.toString().equals(override.toString())) return;

		// update the preference for the rule overrides
		setDRCOverrides(changes, tech);

		// update variables on the technology
		tech.setRuleVariables(newRules);

		// flush the cache of rules
		if (currentTechnology == tech) currentTechnology = null;
	}

	/**
	 * Method to create a set of Design Rules from some simple spacing arrays.
	 * Used by simpler technologies that do not have full-sets of design rules.
	 * @param tech the Technology to load.
	 * @param conDist an upper-diagonal array of layer-to-layer distances (when connected).
	 * @param unConDist an upper-diagonal array of layer-to-layer distances (when unconnected).
	 * @return a set of design rules for the Technology.
	 */
	public static DRCRules makeSimpleRulesOld(Technology tech, double [] conDist, double [] unConDist)
	{
		/*
		Rules rules = new Rules(tech);
		if (conDist != null)
		{
			for(int i=0; i<conDist.length; i++)
			{
				rules.conList[i] = new Double(conDist[i]);
			}
		}
		if (unConDist != null)
		{
			for(int i=0; i<unConDist.length; i++)
			{
				rules.unConList[i] = new Double(unConDist[i]);
			}
		}
		return rules;
		*/
		return (null);
	}

	/****************************** INDIVIDUAL DESIGN RULES ******************************/

	/**
	 * Method to find the worst spacing distance in the design rules.
	 * Finds the largest spacing rule in the Technology.
	 * @param tech the Technology to examine.
	 * @return the largest spacing distance in the Technology. Zero if nothing found
	 */
	public static double getWorstSpacingDistance(Technology tech)
	{
		DRCRules rules = getRules(tech);
		if (rules == null)
        {
            if (Main.getDebug()) System.out.println("Is -1 a valid number? in DRC::getWorstSpacingDistance");
            return -1;
        }
		return (rules.getWorstSpacingDistance());
	}

	/**
	 * Method to find the maximum design-rule distance around a layer.
	 * @param layer the Layer to examine.
	 * @return the maximum design-rule distance around the layer. -1 if nothing found.
	 */
	public static double getMaxSurround(Layer layer, double maxSize)
	{
		Technology tech = layer.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return -1;

        return (rules.getMaxSurround(tech, layer, maxSize));
	}

	/**
	 * Method to find the edge spacing rule between two layer.
	 * @param layer1 the first layer.
	 * @param layer2 the second layer.
	 * @return the edge rule distance between the layers.
	 * Returns null if there is no edge spacing rule.
	 */
	public static DRCRules.DRCRule getEdgeRule(Layer layer1, Layer layer2)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;

		return (rules.getEdgeRule(tech, layer1, layer2));
	}

	/**
	 * Method to return the "wide" limit for a Technology.
	 * There are different design rules, depending on whether the geometry is wide or not.
	 * @param tech the Technology in question.
	 * @return the "wide" limit.  Anything this size or larger must use "wide" design rules.
	 */
	/*
	public static double getWideLimit(Technology tech)
	{
		Rules rules = getRules(tech);
		if (rules == null) return -1;
		return rules.wideLimit.doubleValue();
	}
	*/

	/**
	 * Method to find the spacing rule between two layer.
	 * @param layer1 the first layer.
	 * @param layer2 the second layer.
	 * @param connected true to find the distance when the layers are connected.
	 * @param multiCut true to find the distance when this is part of a multicut contact.
	 * @return the spacing rule between the layers.
	 * Returns null if there is no spacing rule.
	 */
	public static DRCRules.DRCRule getSpacingRule(Layer layer1, Layer layer2, boolean connected,
	                                  boolean multiCut, double wideS)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;

        /*
		int type = (connected) ? RuleTemplate.CONSPA : RuleTemplate.UCONSPA;
		RRule r = rules.getRule(pIndex, type, wideS);

		if (r != null)
		{
			bestDist = r.value;
			rule = r.ruleName;
		}
        */
        return (rules.getSpacingRule(tech, layer1, layer2, connected, multiCut, wideS));
	}

	/**
	 * Method to tell whether there are any design rules between two layers.
	 * @param layer1 the first Layer to check.
	 * @param layer2 the second Layer to check.
	 * @return true if there are design rules between the layers.
	 */
	public static boolean isAnyRule(Layer layer1, Layer layer2)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return false;
        return (rules.isAnyRule(tech, layer1, layer2));
	}

	/**
	 * Method to get the minimum <type> rule for a Layer
	 * where <type> is the rule type. E.g. MinWidth or Area
	 * @param layer the Layer to examine.
	 * @param type rule type
	 * @return the minimum width rule for the layer.
	 * Returns null if there is no minimum width rule.
	 */
	public static DRCRules.DRCRule getMinValue(Layer layer, int type)
	{
		Technology tech = layer.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;
        return (rules.getMinValue(layer, type));
	}

	/**
	 * Method to get the minimum size rule for a NodeProto.
	 * @param np the NodeProto to examine.
	 * @return the minimum size rule for the NodeProto.
	 * Returns null if there is no minimum size rule.
	 */
	public static NodeSizeRule getMinSize(NodeProto np)
	{
		if (np instanceof Cell) return null;
		PrimitiveNode pnp = (PrimitiveNode)np;
		if (pnp.getMinWidth() < 0 && pnp.getMinHeight() < 0) return null;
		return new NodeSizeRule(pnp.getMinWidth(), pnp.getMinHeight(), pnp.getMinSizeRule());
	}



	/****************************** SUPPORT FOR DESIGN RULES ******************************/

	/**
	 * Method to get the DRC overrides from the preferences for a given technology.
	 * @param tech the Technology on which to get overrides.
	 * @return a Pref describing DRC overrides for the Technology.
	 */
	private static StringBuffer getDRCOverrides(Technology tech)
	{
		Pref pref = (Pref)prefDRCOverride.get(tech);
		if (pref == null)
		{
			pref = Pref.makeStringPref("DRCOverridesFor" + tech.getTechName(), DRC.tool.prefs, "");
			prefDRCOverride.put(tech, pref);
		}
		StringBuffer sb = new StringBuffer();
		sb.append(pref.getString());
		return sb;
	}

	/**
	 * Method to set the DRC overrides for a given technology.
	 * @param sb the overrides (a StringBuffer).
	 * @param tech the Technology on which to get overrides.
	 */
	private static void setDRCOverrides(StringBuffer sb, Technology tech)
	{
		if (sb.length() >= Preferences.MAX_VALUE_LENGTH)
		{
			System.out.println("Warning: Design rule overrides are too complex to be saved (are " +
				sb.length() + " long which is more than the limit of " + Preferences.MAX_VALUE_LENGTH + ")");
		}
		Pref pref = (Pref)prefDRCOverride.get(tech);
		if (pref == null)
		{
			pref = Pref.makeStringPref("DRCOverridesFor" + tech.getTechName(), DRC.tool.prefs, "");
			prefDRCOverride.put(tech, pref);
		}
		pref.setString(sb.toString());
	}

    /****************************** OPTIONS ******************************/

	private static Pref cacheIncrementalDRCOn = Pref.makeBooleanPref("IncrementalDRCOn", DRC.tool.prefs, true);
	/**
	 * Method to tell whether DRC should be done incrementally.
	 * The default is "true".
	 * @return true if DRC should be done incrementally.
	 */
	public static boolean isIncrementalDRCOn() { return cacheIncrementalDRCOn.getBoolean(); }
	/**
	 * Method to set whether DRC should be done incrementally.
	 * @param on true if DRC should be done incrementally.
	 */
	public static void setIncrementalDRCOn(boolean on) { cacheIncrementalDRCOn.setBoolean(on); }

	private static Pref cacheOneErrorPerCell = Pref.makeBooleanPref("OneErrorPerCell", DRC.tool.prefs, false);
	/**
	 * Method to tell whether DRC should report only one error per Cell.
	 * The default is "false".
	 * @return true if DRC should report only one error per Cell.
	 */
	public static boolean isOneErrorPerCell() { return cacheOneErrorPerCell.getBoolean(); }
	/**
	 * Method to set whether DRC should report only one error per Cell.
	 * @param on true if DRC should report only one error per Cell.
	 */
	public static void setOneErrorPerCell(boolean on) { cacheOneErrorPerCell.setBoolean(on); }

	private static Pref cacheUseMultipleThreads = Pref.makeBooleanPref("UseMultipleThreads", DRC.tool.prefs, false);
	/**
	 * Method to tell whether DRC should use multiple threads.
	 * The default is "false".
	 * @return true if DRC should use multiple threads.
	 */
	public static boolean isUseMultipleThreads() { return cacheUseMultipleThreads.getBoolean(); }
	/**
	 * Method to set whether DRC should use multiple threads.
	 * @param on true if DRC should use multiple threads.
	 */
	public static void setUseMultipleThreads(boolean on) { cacheUseMultipleThreads.setBoolean(on); }

	private static Pref cacheNumberOfThreads = Pref.makeIntPref("NumberOfThreads", DRC.tool.prefs, 2);
	/**
	 * Method to return the number of threads to use when running DRC with multiple threads.
	 * The default is 2.
	 * @return the number of threads to use when running DRC with multiple threads.
	 */
	public static int getNumberOfThreads() { return cacheNumberOfThreads.getInt(); }
	/**
	 * Method to set the number of threads to use when running DRC with multiple threads.
	 * @param th the number of threads to use when running DRC with multiple threads.
	 */
	public static void setNumberOfThreads(int th) { cacheNumberOfThreads.setInt(th); }

	private static Pref cacheIgnoreCenterCuts = Pref.makeBooleanPref("IgnoreCenterCuts", DRC.tool.prefs, false);
    static { cacheIgnoreCenterCuts.attachToObject(DRC.tool, "Tools/DRC tab", "DRC ignores center cuts in large contacts"); }
	/**
	 * Method to tell whether DRC should ignore center cuts in large contacts.
	 * Only the perimeter of cuts will be checked.
	 * The default is "false".
	 * @return true if DRC should ignore center cuts in large contacts.
	 */
	public static boolean isIgnoreCenterCuts() { return cacheIgnoreCenterCuts.getBoolean(); }
	/**
	 * Method to set whether DRC should ignore center cuts in large contacts.
	 * Only the perimeter of cuts will be checked.
	 * @param on true if DRC should ignore center cuts in large contacts.
	 */
	public static void setIgnoreCenterCuts(boolean on) { cacheIgnoreCenterCuts.setBoolean(on); }

	public static final Variable.Key POSTSCRIPT_FILEDATE = ElectricObject.newKey("IO_postscript_filedate");
	/**
	 * Method to tell the date of the last successful DRC of a given Cell.
	 * @param cell the cell to query.
	 * @return the date of the last successful DRC of that Cell.
	 */
	public static Date getLastDRCDate(Cell cell)
	{
		Variable varDate = cell.getVar(DRCRules.LAST_GOOD_DRC, Integer[].class);
		if (varDate == null) return null;
		Integer [] lastDRCDateAsInts = (Integer [])varDate.getObject();
		long lastDRCDateInSecondsHigh = lastDRCDateAsInts[0].intValue();
		long lastDRCDateInSecondsLow = lastDRCDateAsInts[1].intValue();
		long lastDRCDateInSeconds = (lastDRCDateInSecondsHigh << 32) | (lastDRCDateInSecondsLow & 0xFFFFFFFFL);
		Date lastDRCDate = new Date(lastDRCDateInSeconds);
		return lastDRCDate;
	}
	/**
	 * Method to set the date of the last successful DRC of a given Cell.
	 * @param cell the cell to modify.
	 * @param date the date of the last successful DRC of that Cell.
	 */
	public static void setLastDRCDate(Cell cell, Date date)
	{
		long iVal = date.getTime();
		Integer [] dateArray = new Integer[2];
		dateArray[0] = new Integer((int)(iVal >> 32));
		dateArray[1] = new Integer((int)(iVal & 0xFFFFFFFF));
		cell.newVar(DRCRules.LAST_GOOD_DRC, dateArray);
	}
}

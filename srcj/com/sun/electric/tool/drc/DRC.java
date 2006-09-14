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

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * This is the Design Rule Checker tool.
 */
public class DRC extends Listener
{
	/** the DRC tool. */								protected static DRC tool = new DRC();
	/** overrides of rules for each technology. */		private static HashMap<Technology,Pref> prefDRCOverride = new HashMap<Technology,Pref>();
	/** map of cells and their objects to DRC */		private static HashMap<Cell,HashSet<Geometric>> cellsToCheck = new HashMap<Cell,HashSet<Geometric>>();
    /** to temporary store DRC dates */                 private static HashMap<Cell,StoreDRCInfo> storedDRCDate = new HashMap<Cell,StoreDRCInfo>();
	/** for logging incremental errors */                       private static ErrorLogger errorLoggerIncremental = null;

    private static class StoreDRCInfo
    {
        long date;
        int bits;
        StoreDRCInfo(long d, int b)
        {
            date = d;
            bits = b;
        }
    }

    private static boolean incrementalRunning = false;
    /** key of Variable for last valid DRC date on a Cell. */
    public static final Variable.Key DRC_LAST_GOOD_DATE = Variable.newKey("DRC_last_good_drc_date");
    /** key of Variable for last valid DRC bit on a Cell. */
    public static final Variable.Key DRC_LAST_GOOD_BIT = Variable.newKey("DRC_last_good_drc_bit");
    private static final int DRC_BIT_AREA = 01; /* Min area condition */
    private static final int DRC_BIT_COVERAGE = 02;   /* Coverage DRC condition */
    private static final int DRC_BIT_ST_FOUNDRY = 04; /* For ST foundry selection */
    private static final int DRC_BIT_TSMC_FOUNDRY = 010; /* For TSMC foundry selection */
    private static final int DRC_BIT_MOSIS_FOUNDRY = 020; /* For Mosis foundry selection */

    /** Control different level of error checking */
    public enum DRCCheckMode
    {
	    ERROR_CHECK_DEFAULT (0),    /** DRC stops after first error between 2 nodes is found (default) */
        ERROR_CHECK_CELL (1),       /** DRC stops after first error per cell is found */
        ERROR_CHECK_EXHAUSTIVE (2);  /** DRC checks all combinations */
        private final int mode;   // mode
        DRCCheckMode(int mode) {
            this.mode = mode;
        }
        public int mode() { return this.mode; }
        public String toString() {return name();}
    }

    /****************************** DESIGN RULES ******************************/

	/**
	 * Class to define a single rule on a node.
	 */
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

    /**
     * Method to retrieve the singleton associated with the DRC tool.
     * @return the DRC tool.
     */
    public static DRC getDRCTool() { return tool; }

	private static void includeGeometric(Geometric geom)
	{
		if (!isIncrementalDRCOn()) return;
        Cell cell = geom.getParent();

        synchronized (cellsToCheck)
		{
			HashSet<Geometric> cellSet = cellsToCheck.get(cell);
			if (cellSet == null)
			{
				cellSet = new HashSet<Geometric>();
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
			HashSet<Geometric> cellSet = cellsToCheck.get(cell);
			if (cellSet != null) cellSet.remove(geom);
		}
	}

	private static void doIncrementalDRCTask()
	{
		if (!isIncrementalDRCOn()) return;
		if (incrementalRunning) return;

		Library curLib = Library.getCurrent();
		if (curLib == null) return;
		Cell cellToCheck = Job.getUserInterface().getCurrentCell(curLib);
		HashSet<Geometric> cellSet = null;

		// get a cell to check
		synchronized (cellsToCheck)
		{
			if (cellToCheck != null)
				cellSet = cellsToCheck.get(cellToCheck);
			if (cellSet == null && cellsToCheck.size() > 0)
			{
				cellToCheck = cellsToCheck.keySet().iterator().next();
				cellSet = cellsToCheck.get(cellToCheck);
			}
			if (cellSet != null)
				cellsToCheck.remove(cellToCheck);
		}

		if (cellToCheck == null) return; // nothing to do

		// don't check if cell not in database anymore
		if (!cellToCheck.isLinked()) return;
		// Handling clipboard case (one type of hidden libraries)
		if (cellToCheck.getLibrary().isHidden()) return;

		// if there is a cell to check, do it
		if (cellSet != null)
		{
			Geometric [] objectsToCheck = new Geometric[cellSet.size()];
			int i = 0;
            for(Geometric geom : cellSet)
				objectsToCheck[i++] = geom;

            new CheckDRCIncrementally(cellToCheck, objectsToCheck, cellToCheck.getTechnology().isScaleRelevant());
		}
	}

   /**
     * Handles database changes of a Job.
     * @param oldSnapshot database snapshot before Job.
     * @param newSnapshot database snapshot after Job and constraint propagation.
     * @param undoRedo true if Job was Undo/Redo job.
     */
    public void endBatch(Snapshot oldSnapshot, Snapshot newSnapshot, boolean undoRedo)
	{
        for (CellId cellId: newSnapshot.getChangedCells(oldSnapshot)) {
            Cell cell = Cell.inCurrentThread(cellId);
            if (cell == null) continue;
            CellBackup oldBackup = oldSnapshot.getCell(cellId);
            for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
                NodeInst ni = it.next();
                ImmutableNodeInst d = ni.getD();
                if (oldBackup == null || oldBackup.getNode(d.nodeId) != d)
                    includeGeometric(ni);
            }
            for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); ) {
                ArcInst ai = it.next();
                ImmutableArcInst d = ai.getD();
                if (oldBackup == null || oldBackup.getArc(d.arcId) != d)
                    includeGeometric(ai);
            }
        }
		doIncrementalDRCTask();
	}

//	/**
//	 * Method to announce a change to a NodeInst.
//	 * @param ni the NodeInst that was changed.
//	 * @param oD the old contents of the NodeInst.
//	 */
//	public void modifyNodeInst(NodeInst ni, ImmutableNodeInst oD)
//	{
//		includeGeometric(ni);
//	}
//
//	/**
//	 * Method to announce a change to an ArcInst.
//	 * @param ai the ArcInst that changed.
//     * @param oD the old contents of the ArcInst.
//	 */
//	public void modifyArcInst(ArcInst ai, ImmutableArcInst oD)
//	{
//		includeGeometric(ai);
//	}
//
//	/**
//	 * Method to announce the creation of a new ElectricObject.
//	 * @param obj the ElectricObject that was just created.
//	 */
//	public void newObject(ElectricObject obj)
//	{
//		if (obj instanceof Geometric)
//		{
//			includeGeometric((Geometric)obj);
//		}
//	}
//
//	/**
//	 * Method to announce the deletion of an ElectricObject.
//	 * @param obj the ElectricObject that was just deleted.
//	 */
//	public void killObject(ElectricObject obj)
//	{
//		if (obj instanceof Geometric)
//		{
//			removeGeometric((Geometric)obj);
//		}
//	}

	/****************************** DRC INTERFACE ******************************/
    public static ErrorLogger getDRCErrorLogger(boolean layout, boolean incremental)
    {
        ErrorLogger errorLogger = null;
        String title = (layout) ? "Layout " : "Schematic ";

        if (incremental)
        {
            if (errorLoggerIncremental == null) errorLoggerIncremental = ErrorLogger.newInstance("DRC (incremental)"/*, true*/);
            errorLogger = errorLoggerIncremental;
        }
        else
            errorLogger = ErrorLogger.newInstance(title + "DRC (full)");
        return errorLogger;
    }

    /**
     * This method generates a DRC job from the GUI or for a bash script.
     */
    public static void checkDRCHierarchically(Cell cell, Rectangle2D bounds, GeometryHandler.GHMode mode)
    {
        if (cell == null) return;
        boolean isLayout = true; // hierarchical check of layout by default
		if (cell.isSchematic() || cell.getTechnology() == Schematics.tech ||
			cell.isIcon() || cell.getTechnology() == Artwork.tech)
			// hierarchical check of schematics
			isLayout = false;

        if (mode == null) mode = GeometryHandler.GHMode.ALGO_SWEEP;
        new CheckDRCHierarchically(cell, isLayout, bounds, mode);
    }

	/**
	 * Base class for checking design rules.
	 *
	 */
	public static class CheckDRCJob extends Job
	{
		Cell cell;
        boolean isLayout; // to check layout

        private static String getJobName(Cell cell) { return "Design-Rule Check " + cell; }
		protected CheckDRCJob(Cell cell, Listener tool, Priority priority, boolean layout)
		{
			super(getJobName(cell), tool, Job.Type.EXAMINE, null, null, priority);
			this.cell = cell;
            this.isLayout = layout;

		}
		// never used
		public boolean doIt() { return (false);}
	}

    /**
     * Class for hierarchical DRC for layout and schematics
     */
	private static class CheckDRCHierarchically extends CheckDRCJob
	{
		Rectangle2D bounds;
        private GeometryHandler.GHMode mergeMode; // to select the merge algorithm

        /**
         * Check bounds within cell. If bounds is null, check entire cell.
         * @param cell
         * @param layout
         * @param bounds
         */
		protected CheckDRCHierarchically(Cell cell, boolean layout, Rectangle2D bounds, GeometryHandler.GHMode mode)
		{
			super(cell, tool, Job.Priority.USER, layout);
			this.bounds = bounds;
            this.mergeMode = mode;
			startJob();
		}

		public boolean doIt()
		{
			long startTime = System.currentTimeMillis();
            ErrorLogger errorLog = getDRCErrorLogger(isLayout, false);
            if (isLayout)
                Quick.checkDesignRules(errorLog, cell, null, null, bounds, this, mergeMode);
            else
                Schematic.doCheck(errorLog, cell, null);
            long endTime = System.currentTimeMillis();
            int errorCount = errorLog.getNumErrors();
            int warnCount = errorLog.getNumWarnings();
            System.out.println(errorCount + " errors and " + warnCount + " warnings found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");

            return true;
		}
	}

	private static class CheckDRCIncrementally extends CheckDRCJob
	{
		Geometric [] objectsToCheck;

		protected CheckDRCIncrementally(Cell cell, Geometric[] objectsToCheck, boolean layout)
		{
			super(cell, tool, Job.Priority.ANALYSIS, layout);
			this.objectsToCheck = objectsToCheck;
			startJob();
		}

		public boolean doIt()
		{
			incrementalRunning = true;
            ErrorLogger errorLog = getDRCErrorLogger(isLayout, true);
            if (isLayout)
                errorLog = Quick.checkDesignRules(errorLog, cell, objectsToCheck, null, null);
            else
                errorLog = Schematic.doCheck(errorLog, cell, objectsToCheck);
            int errorsFound = errorLog.getNumErrors();
			if (errorsFound > 0)
				System.out.println("Incremental DRC found " + errorsFound + " errors/warnings in "+ cell);
			incrementalRunning = false;
			doIncrementalDRCTask();
			return true;
		}
	}

	/****************************** DESIGN RULE CONTROL ******************************/

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
        DRCRules currentRules = tech.getCachedRules();
		if (currentRules != null && tech == currentTechnology) return currentRules;

		// constructing design rules: start with factory rules
		currentRules = tech.getFactoryDesignRules(true);
		if (currentRules != null)
		{
			// add overrides
			StringBuffer override = getDRCOverrides(tech);
			currentRules.applyDRCOverrides(override.toString(), tech);
		}

		// remember technology whose rules are cached
		currentTechnology = tech;
        tech.setCachedRules(currentRules);
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
		DRCRules factoryRules = tech.getFactoryDesignRules(true);

		// determine override differences from the factory rules
		StringBuffer changes = Technology.getRuleDifferences(factoryRules, newRules);

        if (Job.LOCALDEBUGFLAG)
            System.out.println("This function needs attention");

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

	/****************************** INDIVIDUAL DESIGN RULES ******************************/

	/**
	 * Method to find the worst spacing distance in the design rules.
	 * Finds the largest spacing rule in the Technology.
	 * @param tech the Technology to examine.
     * @param lastMetal
     * @return the largest spacing distance in the Technology. Zero if nothing found
	 */
	public static double getWorstSpacingDistance(Technology tech, int lastMetal)
	{
		DRCRules rules = getRules(tech);
		if (rules == null)
            return 0;
		return (rules.getWorstSpacingDistance(lastMetal));
	}

    /**
	 * Method to find the maximum design-rule distance around a layer.
	 * @param layer the Layer to examine.
	 * @return the maximum design-rule distance around the layer. -1 if nothing found.
	 */
	public static double getMaxSurround(Layer layer, double maxSize)
	{
		Technology tech = layer.getTechnology();
        if (tech == null) return -1; // case when layer is a Graphics
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
	public static DRCTemplate getEdgeRule(Layer layer1, Layer layer2)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;

		return (rules.getEdgeRule(layer1, layer2));
	}

	/**
	 * Method to find the spacing rule between two layer.
	 * @param layer1 the first layer.
     * @param geo1
	 * @param layer2 the second layer.
     * @param geo2
	 * @param connected true to find the distance when the layers are connected.
	 * @param multiCut true to find the distance when this is part of a multicut contact.
     * @param wideS widest polygon
     * @param length length of the intersection
	 * @return the spacing rule between the layers.
	 * Returns null if there is no spacing rule.
	 */
	public static DRCTemplate getSpacingRule(Layer layer1, Geometric geo1, Layer layer2, Geometric geo2,
                                             boolean connected, int multiCut, double wideS, double length)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;
        return (rules.getSpacingRule(layer1, geo1, layer2, geo2, connected, multiCut, wideS, length));
	}

	/**
	 * Method to find the extension rule between two layer.
	 * @param layer1 the first layer.
	 * @param layer2 the second layer.
     * @param isGateExtension to decide between the rule EXTENSIONGATE or EXTENSION
	 * @return the extension rule between the layers.
	 * Returns null if there is no extension rule.
	 */
	public static DRCTemplate getExtensionRule(Layer layer1, Layer layer2, boolean isGateExtension)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;
        return (rules.getExtensionRule(layer1, layer2, isGateExtension));
	}

	/**
	 * Method to tell whether there are any design rules between two layers.
	 * @param layer1 the first Layer to check.
	 * @param layer2 the second Layer to check.
	 * @return true if there are design rules between the layers.
	 */
	public static boolean isAnySpacingRule(Layer layer1, Layer layer2)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return false;
        return (rules.isAnySpacingRule(layer1, layer2));
	}

	/**
	 * Method to get the minimum <type> rule for a Layer
	 * where <type> is the rule type. E.g. MinWidth or Area
	 * @param layer the Layer to examine.
	 * @param type rule type
	 * @return the minimum width rule for the layer.
	 * Returns null if there is no minimum width rule.
	 */
	public static DRCTemplate getMinValue(Layer layer, DRCTemplate.DRCRuleType type)
	{
		Technology tech = layer.getTechnology();
		if (tech == null) return null;
		DRCRules rules = getRules(tech);
		if (rules == null) return null;
        return (rules.getMinValue(layer, type));
	}

    /**
     * Determine if node represented by index in DRC mapping table is forbidden under
     * this foundry.
     */
    public static boolean isForbiddenNode(int index1, int index2, DRCTemplate.DRCRuleType type, Technology tech)
    {
        DRCRules rules = getRules(tech);
        if (rules == null) return false;
        int index = index1; // In case of primitive nodes
        if (index2 != -1 ) index = rules.getRuleIndex(index1, index2);
        if (type == DRCTemplate.DRCRuleType.FORBIDDEN) index += tech.getNumLayers();
        return (rules.isForbiddenNode(index, type));
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
		Pref pref = prefDRCOverride.get(tech);
		if (pref == null)
		{
			pref = Pref.makeStringPref("DRCOverridesFor" + tech.getTechName(), tool.prefs, "");
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
		Pref pref = prefDRCOverride.get(tech);
		if (pref == null)
		{
			pref = Pref.makeStringPref("DRCOverridesFor" + tech.getTechName(), tool.prefs, "");
			prefDRCOverride.put(tech, pref);
		}
		pref.setString(sb.toString());
	}

    /**
     * Method to clean those cells that were marked with a valid date due to
     * changes in the DRC rules.
     * @param f
     */
    public static void cleanCellsDueToFoundryChanges(Technology tech, Foundry f)
    {
        // Need to clean cells using this foundry because the rules might have changed.
        System.out.println("Cleaning good DRC dates in cells using '" + f.getType().name() +
                "' in '" + tech.getTechName() + "'");
        HashMap<Cell,Cell> cleanDRCDate = new HashMap<Cell,Cell>();
        int bit = 0;
        switch(f.getType())
        {
            case MOSIS:
                bit = DRC_BIT_MOSIS_FOUNDRY;
                break;
            case TSMC:
                bit = DRC_BIT_TSMC_FOUNDRY;
                break;
            case ST:
                bit = DRC_BIT_ST_FOUNDRY;
                break;
        }

        boolean inMemory = isDatesStoredInMemory();

        for (Iterator<Library> it = Library.getLibraries(); it.hasNext();)
        {
            Library lib = it.next();
            for (Iterator<Cell> itC = lib.getCells(); itC.hasNext();)
            {
                Cell cell = itC.next();
                if (cell.getTechnology() != tech) continue;

                StoreDRCInfo data = getCellGoodDRCDateAndBits(cell, !inMemory);

                if (data == null) continue; // no data

                // It was marked as valid with previous set of rules
                if ((data.bits & bit) != 0)
                    cleanDRCDate.put(cell, cell);
            }
        }
        addDRCUpdate(0, null, cleanDRCDate, null);
    }

    /**
     * Method to extract the corresponding DRC bits stored in disk(database) or
     * in memory for a given cell for further analysis
     * @param cell
     * @param fromDisk
     * @return temporary class containing date and bits if available. Null if nothing is found
     */
    private static StoreDRCInfo getCellGoodDRCDateAndBits(Cell cell, boolean fromDisk)
    {
        StoreDRCInfo data = storedDRCDate.get(cell);
        boolean firstTime = false;

        if (data == null)
        {
            boolean validVersion = true;
            Version version = cell.getLibrary().getVersion();
            if (version != null) validVersion = version.compareTo(Version.getVersion()) >=0;
            data = new StoreDRCInfo(-1, -1);
            storedDRCDate.put(cell, data);
            firstTime = true; // to load Variable date from disk in case of inMemory case.
            if (!validVersion)
                return null; // only the first the data is access the version is considered
        }
        if (fromDisk || (!fromDisk && firstTime))
        {
            int thisByte = 0;
            long lastDRCDateInMilliseconds = 0;
            // disk version
            Variable varDate = cell.getVar(DRC_LAST_GOOD_DATE, Long.class); // new strategy
            if (varDate == null)
                varDate = cell.getVar(DRC_LAST_GOOD_DATE, Integer[].class);
            if (varDate == null) return null;
            Object lastDRCDateObject = varDate.getObject();
            if (lastDRCDateObject instanceof Integer[]) {
                Integer[] lastDRCDateAsInts = (Integer [])lastDRCDateObject;
                long lastDRCDateInSecondsHigh = lastDRCDateAsInts[0].intValue();
                long lastDRCDateInSecondsLow = lastDRCDateAsInts[1].intValue();
                lastDRCDateInMilliseconds = (lastDRCDateInSecondsHigh << 32) | (lastDRCDateInSecondsLow & 0xFFFFFFFFL);
            } else {
                lastDRCDateInMilliseconds = ((Long)lastDRCDateObject).longValue();
            }
            Variable varBits = cell.getVar(DRC_LAST_GOOD_BIT, Integer.class);
            if (varBits == null) // old Byte class
            {
                varBits = cell.getVar(DRC_LAST_GOOD_BIT, Byte.class);
                if (varBits != null)
                    thisByte =((Byte)varBits.getObject()).byteValue();
            }
            else
                thisByte = ((Integer)varBits.getObject()).intValue();
            data.bits = thisByte;
            data.date = lastDRCDateInMilliseconds;
//            data = new StoreDRCInfo(lastDRCDateInMilliseconds, thisByte);
        }
        else
        {
            data = storedDRCDate.get(cell);
        }
        return data;
    }

    /**
     * Method to check if current date is later than cell revision
     * @param cell
     * @param date
     * @return true if DRC date in cell is valid
     */
    public static boolean isCellDRCDateGood(Cell cell, Date date)
    {
        if (date != null)
        {
            Date lastChangeDate = cell.getRevisionDate();
            if (date.after(lastChangeDate)) return true;
        }
        return false;
    }

    /**
     * Method to tell the date of the last successful DRC of a given Cell.
     * @param cell the cell to query.
     * @param fromDisk
     * @return the date of the last successful DRC of that Cell.
     */
    public static Date getLastDRCDateBasedOnBits(Cell cell, int activeBits, boolean fromDisk)
    {
        StoreDRCInfo data = getCellGoodDRCDateAndBits(cell, fromDisk);

        // if data is null -> nothing found
        if (data == null)
            return null;

        int thisByte = data.bits;
        if (fromDisk)
            assert(thisByte!=0);
        boolean area = (thisByte & DRC_BIT_AREA) == (activeBits & DRC_BIT_AREA);
        boolean coverage = (thisByte & DRC_BIT_COVERAGE) == (activeBits & DRC_BIT_COVERAGE);
        // DRC date is invalid if conditions were checked for another foundry
        boolean sameManufacturer = (thisByte & DRC_BIT_TSMC_FOUNDRY) == (activeBits & DRC_BIT_TSMC_FOUNDRY) &&
                (thisByte & DRC_BIT_ST_FOUNDRY) == (activeBits & DRC_BIT_ST_FOUNDRY) &&
                (thisByte & DRC_BIT_MOSIS_FOUNDRY) == (activeBits & DRC_BIT_MOSIS_FOUNDRY);
        if (activeBits != 0 && (!area || !coverage || !sameManufacturer))
            return null;

        // If in memory, date doesn't matter
        Date revisionDate = cell.getRevisionDate();
        Date lastDRCDate = lastDRCDate = new Date(data.date);
        return (lastDRCDate.after(revisionDate)) ? lastDRCDate : null;
    }

    /**
     * Method to clean any DRC date stored previously
     * @param cell the cell to clean
     */
    private static void cleanDRCDateAndBits(Cell cell)
    {
        cell.delVar(DRC_LAST_GOOD_DATE);
        cell.delVar(DRC_LAST_GOOD_BIT);
    }

    public static String explainBits(int bits)
    {
        boolean on = (bits & DRC_BIT_AREA) != 0;
        String msg = "area bit ";
        msg += on ? "on" : "off";

        on = (bits & DRC_BIT_COVERAGE) != 0;
        msg += ", coverage bit ";
        msg += on ? "on" : "off";

        if ((bits & DRC_BIT_TSMC_FOUNDRY) != 0)
            msg += ", TSMC bit";
        else if ((bits & DRC_BIT_ST_FOUNDRY) != 0)
            msg += ", ST bit";
        else if ((bits & DRC_BIT_MOSIS_FOUNDRY) != 0)
            msg += ", Mosis bit";
        return msg;
    }

    public static int getActiveBits(Technology tech)
    {
        int bits = 0;
        if (!isIgnoreAreaChecking()) bits |= DRC_BIT_AREA;
        if (!isIgnoreExtensionRuleChecking()) bits |= DRC_BIT_COVERAGE;
        // Adding foundry to bits set
        Foundry foundry = tech.getSelectedFoundry();
        if (foundry != null)
        {
	        switch(foundry.getType())
	        {
	            case MOSIS:
	                bits |= DRC_BIT_MOSIS_FOUNDRY;
	                break;
	            case TSMC:
	                bits |= DRC_BIT_TSMC_FOUNDRY;
	                break;
	            case ST:
	                bits |= DRC_BIT_ST_FOUNDRY;
	                break;
	        }
        }
        return bits;
    }


    /****************************** OPTIONS ******************************/

	private static Pref cacheIncrementalDRCOn = Pref.makeBooleanPref("IncrementalDRCOn", tool.prefs, false);
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

	private static Pref cacheErrorCheckLevel = Pref.makeIntPref("ErrorCheckLevel", tool.prefs,
            DRCCheckMode.ERROR_CHECK_DEFAULT.mode());
	/**
	 * Method to retrieve checking level in DRC
	 * The default is "ERROR_CHECK_DEFAULT".
	 * @return integer representing error type
	 */
	public static DRC.DRCCheckMode getErrorType()
    {
        int val = cacheErrorCheckLevel.getInt();
        for (DRCCheckMode p : DRCCheckMode.values())
        {
            if (p.mode() == val) return p;
        }
        return null;
    }

	/**
	 * Method to set DRC error type.
	 * @param type representing error level
	 */
	public static void setErrorType(DRC.DRCCheckMode type) { cacheErrorCheckLevel.setInt(type.mode()); }

	private static Pref cacheUseMultipleThreads = Pref.makeBooleanPref("UseMultipleThreads", tool.prefs, false);
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

	private static Pref cacheNumberOfThreads = Pref.makeIntPref("NumberOfThreads", tool.prefs, 2);
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

	private static Pref cacheIgnoreCenterCuts = Pref.makeBooleanPref("IgnoreCenterCuts", tool.prefs, false);
//    static { cacheIgnoreCenterCuts.attachToObject(tool, "Tools/DRC tab", "DRC ignores center cuts in large contacts"); }
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

    private static Pref cacheIgnoreAreaChecking = Pref.makeBooleanPref("IgnoreAreaCheck", tool.prefs, false);
//    static { cacheIgnoreAreaChecking.attachToObject(tool, "Tools/DRC tab", "DRC ignores area checking"); }
	/**
	 * Method to tell whether DRC should ignore minimum/enclosed area checking.
	 * The default is "false".
	 * @return true if DRC should ignore minimum/enclosed area checking.
	 */
	public static boolean isIgnoreAreaChecking() { return cacheIgnoreAreaChecking.getBoolean(); }
	/**
	 * Method to set whether DRC should ignore minimum/enclosed area checking.
	 * @param on true if DRC should ignore minimum/enclosed area checking.
	 */
	public static void setIgnoreAreaChecking(boolean on) { cacheIgnoreAreaChecking.setBoolean(on); }

    private static Pref cacheIgnoreExtensionRuleChecking = Pref.makeBooleanPref("IgnoreExtensionRuleCheck", tool.prefs, false);
//    static { cacheIgnoreExtensionRuleChecking.attachToObject(tool, "Tools/DRC tab", "DRC extension rule checking"); }
	/**
	 * Method to tell whether DRC should check extension rules.
	 * The default is "false".
	 * @return true if DRC should check extension rules.
	 */
	public static boolean isIgnoreExtensionRuleChecking() { return cacheIgnoreExtensionRuleChecking.getBoolean(); }
	/**
	 * Method to set whether DRC should check extension rules.
	 * @param on true if DRC should check extension rules.
	 */
	public static void setIgnoreExtensionRuleChecking(boolean on) { cacheIgnoreExtensionRuleChecking.setBoolean(on); }

    private static Pref cacheStoreDatesInMemory = Pref.makeBooleanPref("StoreDatesInMemory", tool.prefs, false);
    /**
     * Method to tell whether DRC dates should be stored in memory or not.
     * The default is "false".
     * @return true if DRC dates should be stored in memory or not.
     */
    public static boolean isDatesStoredInMemory() { return cacheStoreDatesInMemory.getBoolean(); }
    /**
     * Method to set whether DRC dates should be stored in memory or not.
     * @param on true if DRC dates should be stored in memory or not.
     */
    public static void setDatesStoredInMemory(boolean on) { cacheStoreDatesInMemory.setBoolean(on); }

    private static Pref cacheInteractiveLog = Pref.makeBooleanPref("InteractiveLog", tool.prefs, false);
    /**
     * Method to tell whether DRC loggers should be displayed in Explorer immediately
     * The default is "false".
     * @return true if DRC loggers should be displayed in Explorer immediately or not.
     */
    public static boolean isInteractiveLoggingOn() { return cacheInteractiveLog.getBoolean(); }
    /**
     * Method to set whether DRC loggers should be displayed in Explorer immediately or not
     * @param on true if DRC loggers should be displayed in Explorer immediately.
     */
    public static void setInteractiveLogging(boolean on) { cacheInteractiveLog.setBoolean(on); }

    public static void addDRCUpdate(int bits, HashMap<Cell, Date> goodDRCDate, HashMap<Cell, Cell> cleanDRCDate,
                                    HashMap<Geometric, List<Variable>> newVariables)
    {
        boolean good = (goodDRCDate != null && goodDRCDate.size() > 0);
        boolean clean = (cleanDRCDate != null && cleanDRCDate.size() > 0);
        boolean vars = (newVariables != null && newVariables.size() > 0);
        if (!good && !clean && !vars) return; // nothing to do
        new DRCUpdate(bits, goodDRCDate, cleanDRCDate, newVariables);
    }

	/**
	 * Method to delete all cached date information on all cells.
     * @param startJob
     */
	public static void resetDRCDates(boolean startJob)
	{
        new DRCReset(startJob);
	}

    private static class DRCReset extends Job
    {
        DRCReset(boolean startJob)
        {
            super("Resetting DRC Dates", User.getUserTool(), Job.Type.CHANGE, null, null, Priority.USER);
            if (startJob)
                startJob();
            else
                doIt();
        }

        public boolean doIt()
        {
            storedDRCDate.clear();
            if (isDatesStoredInMemory())
            {
                // delete any date stored
            }
            else // stored in disk
            {
                for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
                {
                    Library lib = (Library)it.next();
                    for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
                    {
                        Cell cell = cIt.next();
                        cleanDRCDateAndBits(cell);
                    }
                }
            }
            return true;
        }
    }

    /**
	 * Class to save good Layout DRC dates in a new thread or add new variables in Schematic DRC
	 */
	private static class DRCUpdate extends Job
	{
		HashMap<Cell,Date> goodDRCDate;
		HashMap<Cell,Cell> cleanDRCDate;
        HashMap<Geometric,List<Variable>> newVariables;
        int activeBits;

		public DRCUpdate(int bits, HashMap<Cell, Date> goodDRCDate, HashMap<Cell, Cell> cleanDRCDate, HashMap<Geometric, List<Variable>> newVariables)
		{
			super("Update DRC data", tool, Type.CHANGE, null, null, Priority.USER);
            this.goodDRCDate = goodDRCDate;
			this.cleanDRCDate = cleanDRCDate;
            this.newVariables = newVariables;
            this.activeBits = bits;
            // Only works for layout with in memory dates -> no need of adding the job into the queue
            if (isDatesStoredInMemory() && (newVariables == null || newVariables.isEmpty()))
            {
                try {doIt();} catch (Exception e) {e.printStackTrace();};
            }
            else // put it into the queue
			    startJob();
		}

		public boolean doIt() throws JobException
		{
            HashSet<Cell> goodDRCCells = new HashSet<Cell>();
            boolean inMemory = isDatesStoredInMemory();

            if (goodDRCDate != null)
            {
                for (Map.Entry<Cell,Date> e : goodDRCDate.entrySet())
                {
                    Cell cell = e.getKey();

                    if (!cell.isLinked())
                        throw new JobException("Cell '" + cell + "' is invalid to update DRC date");
                    else
                    {
                        if (inMemory)
                            storedDRCDate.put(cell, new StoreDRCInfo(e.getValue().getTime(), activeBits));
                        else
                            goodDRCCells.add(cell);
                    }
                }
            }
            if (!goodDRCCells.isEmpty())
                Layout.setGoodDRCCells(goodDRCCells, activeBits, inMemory);

            if (cleanDRCDate != null)
            {
                for (Cell cell : cleanDRCDate.keySet())
                {
                    if (!cell.isLinked())
                        new JobException("Cell '" + cell + "' is invalid to clean DRC date");
                    else
                    {
                        StoreDRCInfo data = storedDRCDate.get(cell);
                        assert(data != null);
                        data.date = -1;
                        data.bits = -1; // I can't put null because of the version
                        if (!inMemory)
                            cleanDRCDateAndBits(cell);
                    }
                }
            }

            // Update variables in Schematics DRC
            if (newVariables != null)
            {
                assert(!inMemory);
                for (Map.Entry<Geometric,List<Variable>> e : newVariables.entrySet())
                {
                    Geometric ni = e.getKey();
                    for (Variable var : e.getValue())
                        ni.addVar(var);
                }
            }
			return true;
		}
	}

    /***********************************
     * JUnit interface
     ***********************************/
    public static boolean testAll()
    {
        return true;
    }
}

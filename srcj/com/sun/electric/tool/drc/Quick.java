/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Quick.java
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
package com.sun.electric.tool.drc;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortOriginal;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.Shape;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

/**
 * This is the "quick" DRC which does full hierarchical examination of the circuit.
 * <P>
 * The "quick" DRC works as follows:
 *    It first examines every primitive node and arc in the cell
 *        For each layer on these objects, it examines everything surrounding it, even in subcells
 *            R-trees are used to limit search.
 *            Where cell instances are found, the contents are examined recursively
 *    It next examines every cell instance in the cell
 *        All other instances within the surrounding area are considered
 *            When another instance is found, the two instances are examined for interaction
 *                A cache is kept of instance pairs in specified configurations to speed-up arrays
 *            All objects in the other instance that are inside the bounds of the first are considered
 *                The other instance is hierarchically examined to locate primitives in the area of consideration
 *            For each layer on each primitive object found in the other instance,
 *                Examine the contents of the first instance for interactions about that layer
 * <P>
 * Since Electric understands connectivity, it uses this information to determine whether two layers
 * are connected or not.  However, if such global connectivity is propagated in the standard Electric
 * way (placing numbers on exports, descending into the cell, and pulling the numbers onto local networks)
 * then it is not possible to decompose the DRC for multiple processors, since two different processors
 * may want to write global network information on the same local networks at once.
 * <P>
 * To solve this problem, the "quick" DRC determines how many instances of each cell exist.  For every
 * network in every cell, an array is built that is as large as the number of instances of that cell.
 * This array contains the global network number for that each instance of the cell.  The algorithm for
 * building these arrays is quick (1 second for a million-transistor chip) and the memory requirement
 * is not excessive (8 megabytes for a million-transistor chip).  It uses the CheckInst and CheckProto
 * objects.
 * @author  Steve Rubin, Gilda Garreton
 */

public class Quick
{
	private static final double TINYDELTA = DBMath.getEpsilon()*1.1;
    /** key of Variable holding DRC Cell annotations. */	private static final Variable.Key DRC_ANNOTATION_KEY = Variable.newKey("ATTR_DRC");

    private static enum DRCErrorType
    {
	    // the different types of errors
        SPACINGERROR, MINWIDTHERROR, NOTCHERROR, MINSIZEERROR, BADLAYERERROR, LAYERSURROUNDERROR,
        MINAREAERROR, ENCLOSEDAREAERROR, SURROUNDERROR, FORBIDDEN, RESOLUTION, CUTERROR, SLOTSIZEERROR,
	    // Different types of warnings
        ZEROLENGTHARCWARN, TECHMIXWARN
    }

	/**
	 * The CheckInst object is associated with every cell instance in the library.
	 * It helps determine network information on a global scale.
	 * It takes a "global-index" parameter, inherited from above (intially zero).
	 * It then computes its own index number as follows:
	 *   thisindex = global-index * multiplier + localIndex + offset
	 * This individual index is used to lookup an entry on each network in the cell
	 * (an array is stored on each network, giving its global net number).
	 */
	private static class CheckInst
	{
		int localIndex;
		int multiplier;
		int offset;
	}
	private HashMap<NodeInst,CheckInst> checkInsts = null;


	/**
	 * The CheckProto object is placed on every cell and is used only temporarily
	 * to number the instances.
	 */
	private static class CheckProto
	{
		/** time stamp for counting within a particular parent */		int timeStamp;
		/** number of instances of this cell in a particular parent */	int instanceCount;
		/** total number of instances of this cell, hierarchically */	int hierInstanceCount;
		/** number of instances of this cell in a particular parent */	int totalPerCell;
		/** true if this cell has been checked */						boolean cellChecked;
		/** true if this cell has parameters */							boolean cellParameterized;
		/** true if this cell or subcell has parameters */				boolean treeParameterized;
		/** list of instances in a particular parent */					List<CheckInst> nodesInCell;
		/** netlist of this cell */										Netlist netlist;
	}

    private HashMap<Cell,CheckProto> checkProtos = null;
	private HashMap<Network,Integer[]> networkLists = null;
	private HashMap<Layer,DRCTemplate> minAreaLayerMap = new HashMap<Layer,DRCTemplate>();    // For minimum area checking
	private HashMap<Layer,DRCTemplate> enclosedAreaLayerMap = new HashMap<Layer,DRCTemplate>();    // For enclosed area checking
	private HashMap<Layer,DRCTemplate> slotSizeLayerMap = new HashMap<Layer,DRCTemplate>();    // For max length checking
    private DRC.CheckDRCJob job; // Reference to running job
	private HashMap<Cell,Cell> cellsMap = new HashMap<Cell,Cell>(); // for cell caching
    private HashMap<Geometric,Geometric> nodesMap = new HashMap<Geometric,Geometric>(); // for node caching
    private int activeBits = 0; // to cache current extra bits
    private boolean inMemory = DRC.isDatesStoredInMemory();
    private GeometryHandler.GHMode mergeMode = GeometryHandler.GHMode.ALGO_SWEEP; // .ALGO_QTREE;
    private Map<Layer,NodeInst> od2Layers = new HashMap<Layer,NodeInst>(3);  /** to control OD2 combination in the same die according to foundries */

	public Quick(DRC.CheckDRCJob job, GeometryHandler.GHMode mode)
	{
		this.job = job;
        this.mergeMode = mode;
	}

	/**
	 * The InstanceInter object records interactions between two cell instances and prevents checking
	 * them multiple times.
	 */
	private static class InstanceInter
	{
		/** the two cell instances being compared */	Cell cell1, cell2;
        /** orientation of cell instance 1 */           Orientation or1;
        /** orientation of cell instance 2 */           Orientation or2;
		/** distance from instance 1 to instance 2 */	double dx, dy;
        /** the two NodeInst parents */                 NodeInst n1Parent, n2Parent, triggerNi;
	};
	private List<InstanceInter> instanceInteractionList = new ArrayList<InstanceInter>();


	/**
	 * The DRCExclusion object lists areas where Generic:DRC-Nodes exist to ignore errors.
	 */
//	private static class DRCExclusion
//	{
//		Cell cell;
//		PolyBase poly;
//		NodeInst ni;
//	};
//	private List<DRCExclusion> exclusionList = new ArrayList<DRCExclusion>();
    private Map<Cell,Area> exclusionMap = new HashMap<Cell,Area>();


	/** number of processes for doing DRC */					private int numberOfThreads;
	/** error type search */				                    private DRC.DRCCheckMode errorTypeSearch;
    /** minimum output grid resolution */				        private double minAllowedResolution;
	/** true to ignore center cuts in large contacts. */		private boolean ignoreCenterCuts;
	/** maximum area to examine (the worst spacing rule). */	private double worstInteractionDistance;
	/** time stamp for numbering networks. */					private int checkTimeStamp;
	/** for numbering networks. */								private int checkNetNumber;
	/** total errors found in all threads. */					private int totalMsgFound;
	/** a NodeInst that is too tiny for its connection. */		private NodeInst tinyNodeInst;
	/** the other Geometric in "tiny" errors. */				private Geometric tinyGeometric;
	/** for tracking the time of good DRC. */					private HashMap<Cell,Date> goodDRCDate = new HashMap<Cell,Date>();
	/** for tracking cells that need to clean good DRC vars */	private HashMap<Cell,Cell> cleanDRCDate = new HashMap<Cell,Cell>();
	/** for logging errors */                                   private ErrorLogger errorLogger;
    /** for interactive error logging */                        private boolean interactiveLogger = false;
	/** Top cell for DRC */                                     private Cell topCell;

	/* for figuring out which layers are valid for DRC */
	private Technology layersValidTech = null;
	private boolean [] layersValid;

	/* for tracking which layers interact with which nodes */
	private Technology layerInterTech = null;
	private HashMap<PrimitiveNode, boolean[]> layersInterNodes = null;
	private HashMap<ArcProto, boolean[]> layersInterArcs = null;

    public static ErrorLogger checkDesignRules(ErrorLogger errorLog, Cell cell, Geometric[] geomsToCheck, boolean[] validity,
                                               Rectangle2D bounds)
    {
        if (errorLog == null) errorLog = DRC.getDRCErrorLogger(true, false);
        return checkDesignRules(errorLog, cell, geomsToCheck, validity, bounds, null, GeometryHandler.GHMode.ALGO_SWEEP);
    }

	/**
	 * This is the entry point for DRC.
	 *
	 * Method to do a hierarchical DRC check on cell "cell".
	 * If "count" is zero, check the entire cell.
	 * If "count" is nonzero, only check that many instances (in "nodesToCheck") and set the
	 * entry in "validity" TRUE if it is DRC clean.
	 * @param bounds if null, check entire cell. If not null, only check area in bounds.
     * @param drcJob
	 * @return ErrorLogger containing the information
	 */
	public static ErrorLogger checkDesignRules(ErrorLogger errorLog, Cell cell, Geometric[] geomsToCheck, boolean[] validity,
                                               Rectangle2D bounds, DRC.CheckDRCJob drcJob, GeometryHandler.GHMode mode)
	{
		Quick q = new Quick(drcJob, mode);
        return q.doCheck(errorLog, cell, geomsToCheck, validity, bounds);
	}

    // returns the number of errors found
	private ErrorLogger doCheck(ErrorLogger errorLog, Cell cell, Geometric [] geomsToCheck, boolean [] validity, Rectangle2D bounds)
	{
		// Check if there are DRC rules for particular tech
        Technology tech = cell.getTechnology();
		DRCRules rules = DRC.getRules(tech);

        // caching bits
        activeBits = DRC.getActiveBits(tech);
        System.out.println("Running DRC with " + DRC.explainBits(activeBits));

        // caching memory setting
        inMemory = DRC.isDatesStoredInMemory();
        interactiveLogger = DRC.isInteractiveLoggingOn();

        // minimim resolution different from zero if flag is on otherwise stays at zero (default)
        minAllowedResolution = tech.getResolution();

		// Nothing to check for this particular technology
		if (rules == null || rules.getNumberOfRules() == 0) return errorLogger;

		// get the current DRC options
		errorTypeSearch = DRC.getErrorType();
		ignoreCenterCuts = DRC.isIgnoreCenterCuts();
		numberOfThreads = DRC.getNumberOfThreads();
	    topCell = cell; /* Especially important for minArea checking */

		// if checking specific instances, adjust options and processor count
        int count = (geomsToCheck != null) ? geomsToCheck.length : 0;
		if (count > 0)
		{
			errorTypeSearch = DRC.DRCCheckMode.ERROR_CHECK_CELL;
			numberOfThreads = 1;
		}

		// cache valid layers for this technology
		cacheValidLayers(tech);
		buildLayerInteractions(tech);

		// clean out the cache of instances
	    instanceInteractionList.clear();

		// determine maximum DRC interaction distance
		worstInteractionDistance = DRC.getWorstSpacingDistance(tech, -1);

	    // determine if min area must be checked (if any layer got valid data)
	    minAreaLayerMap.clear();
	    enclosedAreaLayerMap.clear();
        slotSizeLayerMap.clear();
	    cellsMap.clear();
	    nodesMap.clear();

	    // No incremental neither per Cell
	    if (!DRC.isIgnoreAreaChecking() && errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_CELL)
	    {
		    for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
			{
				Layer layer = it.next();

				// Storing min areas
				DRCTemplate minAreaRule = DRC.getMinValue(layer, DRCTemplate.DRCRuleType.MINAREA);
				if (minAreaRule != null)
					minAreaLayerMap.put(layer, minAreaRule);

				// Storing enclosed areas
				DRCTemplate enclosedAreaRule = DRC.getMinValue(layer, DRCTemplate.DRCRuleType.MINENCLOSEDAREA);
				if (enclosedAreaRule != null)
					enclosedAreaLayerMap.put(layer, enclosedAreaRule);

                // Storing slot sizes
				DRCTemplate slotRule = DRC.getMinValue(layer, DRCTemplate.DRCRuleType.SLOTSIZE);
				if (slotRule != null)
					slotSizeLayerMap.put(layer, slotRule);
			}
	    }

		// initialize all cells for hierarchical network numbering
		checkProtos = new HashMap<Cell,CheckProto>();
		checkInsts = new HashMap<NodeInst,CheckInst>();

		// initialize cells in tree for hierarchical network numbering
		Netlist netlist = cell.getNetlist(false);
		CheckProto cp = checkEnumerateProtos(cell, netlist);

		// see if any parameters are used below this cell
		if (cp.treeParameterized && numberOfThreads > 1)
		{
			// parameters found: cannot use multiple processors
			System.out.println("Parameterized layout being used: multiprocessor decomposition disabled");
			numberOfThreads = 1;
		}

		// now recursively examine, setting information on all instances
		cp.hierInstanceCount = 1;
		checkTimeStamp = 0;
		checkEnumerateInstances(cell);

		// now allocate space for hierarchical network arrays
		//int totalNetworks = 0;
		networkLists = new HashMap<Network,Integer[]>();
        for (Map.Entry<Cell,CheckProto> e : checkProtos.entrySet())
		{
			Cell libCell = e.getKey();
			CheckProto subCP = (CheckProto)e.getValue();
			if (subCP.hierInstanceCount > 0)
			{
				// allocate net number lists for every net in the cell
				for(Iterator<Network> nIt = subCP.netlist.getNetworks(); nIt.hasNext(); )
				{
					Network net = nIt.next();
					Integer [] netNumbers = new Integer[subCP.hierInstanceCount];
					for(int i=0; i<subCP.hierInstanceCount; i++) netNumbers[i] = new Integer(0);
					networkLists.put(net, netNumbers);
					//totalNetworks += subCP.hierInstanceCount;
				}
			}
			for(Iterator<NodeInst> nIt = libCell.getNodes(); nIt.hasNext(); )
			{
				NodeInst ni = nIt.next();
				NodeProto np = ni.getProto();
				if (!ni.isCellInstance()) continue;

				// ignore documentation icons
				if (ni.isIconOfParent()) continue;

				CheckInst ci = checkInsts.get(ni);
				CheckProto ocp = getCheckProto((Cell)np);
				ci.offset = ocp.totalPerCell;
			}
			checkTimeStamp++;
			for(Iterator<NodeInst> nIt = libCell.getNodes(); nIt.hasNext(); )
			{
				NodeInst ni = nIt.next();
				NodeProto np = ni.getProto();
				if (!ni.isCellInstance()) continue;

				// ignore documentation icons
				if (ni.isIconOfParent()) continue;

				CheckProto ocp = getCheckProto((Cell)np);
				if (ocp.timeStamp != checkTimeStamp)
				{
					CheckInst ci = checkInsts.get(ni);
					ocp.timeStamp = checkTimeStamp;
					ocp.totalPerCell += subCP.hierInstanceCount * ci.multiplier;
				}
			}
		}

		// now fill in the hierarchical network arrays
		checkTimeStamp = 0;
		checkNetNumber = 1;

		HashMap<Network,Integer> enumeratedNets = new HashMap<Network,Integer>();
		for(Iterator<Network> nIt = cp.netlist.getNetworks(); nIt.hasNext(); )
		{
			Network net = nIt.next();
			enumeratedNets.put(net, new Integer(checkNetNumber));
			checkNetNumber++;
		}
		checkEnumerateNetworks(cell, cp, 0, enumeratedNets);

		if (count <= 0)
			System.out.println("Found " + checkNetNumber + " networks");

		// now search for DRC exclusion areas
        exclusionMap.clear();
		accumulateExclusion(cell);

		// now do the DRC
		errorLogger = errorLog;
        int logsFound = 0;
        int totalErrors = 0;

		if (count == 0)
		{
			// just do full DRC here
			totalErrors = checkThisCell(cell, 0, bounds);
			// sort the errors by layer
			errorLogger.sortLogs();
		} else
		{
			// check only these "count" instances (either an incremental DRC or a quiet one...from Array command)
			if (validity == null)
			{
				// not a quiet DRC, so it must be incremental
                logsFound = errorLogger.getNumLogs();
			}

            // @TODO missing counting this number of errors.
			checkTheseGeometrics(cell, count, geomsToCheck, validity);
		}

		if (errorLogger != null) {
            errorLogger.termLogging(true);
            logsFound = errorLogger.getNumLogs() - logsFound;
        }

        // -2 if cells and subcells are ok
        // Commentted out on May 2, 2006. Not sure why this condition is valid
        // If goodDRCDate contains information of DRC clean cells, it destroys that information -> wrong
//		if (totalErrors != 0 && totalErrors != -2) goodDRCDate.clear();

		// some cells were sucessfully checked: save that information in the database
	    // some cells don't have valid DRC date anymore and therefore they should be clean
        // This is only going to happen if job was not aborted.
	    if ((job == null || !job.checkAbort()))
	    {
            DRC.addDRCUpdate(activeBits, goodDRCDate, cleanDRCDate, null);
	    }

        return errorLogger;
	}

    /*************************** QUICK DRC CELL EXAMINATION ***************************/

	/**
	 * Method to check the contents of cell "cell" with global network index "globalIndex".
	 * Returns positive if errors are found, zero if no errors are found, negative on internal error.
     * @param cell
     * @param globalIndex
     * @param bounds
     * @return positive number if errors are found, zero if no errors are found and check the cell, -1 if job was
     * aborted and -2 if cell is OK with DRC date stored.
     */
	private int checkThisCell(Cell cell, int globalIndex, Rectangle2D bounds)
	{
		// Job aborted or scheduled for abort
		if (job != null && job.checkAbort()) return -1;

        // Cell already checked
		if (cellsMap.get(cell) != null)
			return (0);

		// Previous # of errors/warnings
		int prevErrors = 0;
		int prevWarns = 0;
		if (errorLogger != null)
		{
			prevErrors = errorLogger.getNumErrors();
			prevWarns = errorLogger.getNumWarnings();
		}

		cellsMap.put(cell, cell);

        // Check if cell doesn't have special annotation
        Variable drcVar = cell.getVar(DRC_ANNOTATION_KEY);
        if (drcVar != null && drcVar.getObject().toString().startsWith("black"))
        {
            // Skipping this one
            return totalMsgFound;
        }

		// first check all subcells
		boolean allSubCellsStillOK = true;
        Area area = exclusionMap.get(cell);

		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			if (!ni.isCellInstance()) continue;

			// ignore documentation icons
			if (ni.isIconOfParent()) continue;

            // Check DRC exclusion regions
            if (area != null && area.contains(ni.getBounds()))
                continue; // excluded

			// ignore if not in the area
			Rectangle2D subBounds = bounds; // sept30
			if (subBounds != null)
			{
				if (!ni.getBounds().intersects(bounds)) continue;
				AffineTransform trans = ni.rotateIn();
				AffineTransform xTrnI = ni.translateIn();
				trans.preConcatenate(xTrnI);
				subBounds = new Rectangle2D.Double();
				subBounds.setRect(bounds);
				DBMath.transformRect(subBounds, trans);
			}

			CheckProto cp = getCheckProto((Cell)np);
			if (cp.cellChecked && !cp.cellParameterized) continue;

			// recursively check the subcell
			CheckInst ci = checkInsts.get(ni);
			int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;
			int retval = checkThisCell((Cell)np, localIndex, subBounds);
			if (retval < 0)
				return retval;
            // if cell is in goodDRCDate it means it changes its date for some reason.
            // This happen when a subcell was reloaded and DRC again. The cell containing
            // the subcell instance must be re-DRC to make sure changes in subCell don't affect
            // the parent one.
			if (retval > 0 || goodDRCDate.get(np) != null)
                allSubCellsStillOK = false;
//            if (retval > 0)
//                allSubCellsStillOK = false;
		}

		// prepare to check cell
		CheckProto cp = getCheckProto(cell);
		cp.cellChecked = true;

		// if the cell hasn't changed since the last good check, stop now
        Date lastGoodDate = DRC.getLastDRCDateBasedOnBits(cell, activeBits, !inMemory);
		if (allSubCellsStillOK && DRC.isCellDRCDateGood(cell, lastGoodDate))
		{
            return 0;
		}

		// announce progress
        long startTime = System.currentTimeMillis();
		System.out.println("Checking " + cell);

		// now look at every node and arc here
		totalMsgFound = 0;

		// Check the area first but only when is not incremental
		// Only for the most top cell
		if (cell == topCell && !DRC.isIgnoreAreaChecking() && errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_CELL)
			totalMsgFound = checkMinArea(cell);

		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (bounds != null)
			{
				if (!ni.getBounds().intersects(bounds)) continue;
			}
            if (area != null)
            {
                if (area.contains(ni.getBounds()))
                {
                    continue;
                }
            }
			boolean ret = (ni.isCellInstance()) ?
			        checkCellInst(ni, globalIndex) :
			        checkNodeInst(ni, globalIndex);
			if (ret)
			{
				totalMsgFound++;
				if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) break;
			}
		}
		Technology cellTech = cell.getTechnology();
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			Technology tech = ai.getProto().getTechnology();
			if (tech != cellTech)
			{
				reportError(DRCErrorType.TECHMIXWARN, " belongs to " + tech.getTechName(), cell, 0, 0, null, null, ai, null, null, null, null);
				continue;
			}
			if (bounds != null)
			{
				if (!ai.getBounds().intersects(bounds)) continue;
			}
			if (checkArcInst(cp, ai, globalIndex))
			{
				totalMsgFound++;
				if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) break;
			}
		}

		// If message founds, then remove any possible good date
        // !allSubCellsStillOK disconnected on April 18, 2006. totalMsgFound should
        // dictate if this cell is re-marked.
		if (totalMsgFound > 0) //  || !allSubCellsStillOK)
		{
			cleanDRCDate.put(cell, cell);
		}
		else
		{
            // Only mark the cell when it passes with a new version of DRC or didn't have
            // the DRC bit on
            // If lastGoodDate == null, wrong bits stored or no date available.
            if (lastGoodDate == null)
			    goodDRCDate.put(cell, new Date());
		}

		// if there were no errors, remember that
		if (errorLogger != null)
		{
			int localErrors = errorLogger.getNumErrors() - prevErrors;
			int localWarnings = errorLogger.getNumWarnings() - prevWarns;
            long endTime = System.currentTimeMillis();
			if (localErrors == 0 &&  localWarnings == 0)
			{
				System.out.println("\tNo errors/warnings found");
			} else
			{
				if (localErrors > 0)
					System.out.println("\tFOUND " + localErrors + " ERRORS");
				if (localWarnings > 0)
					System.out.println("\tFOUND " + localWarnings + " WARNINGS");
			}
            if (Job.getDebug())
                System.out.println("\t(took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
		}

		return totalMsgFound;
	}

    /**
     * Check Poly for CIF Resolution Errors
     * @param poly
     * @param cell
     * @param geom
     * @return true if an error was found.
     */
    private boolean checkResolution(PolyBase poly, Cell cell, Geometric geom)
	{
		if (minAllowedResolution == 0) return false;
        int count = 0;
        String resolutionError = "";
		Point2D [] points = poly.getPoints();
		for (int i=0; i<points.length; i++)
		{
            if (DBMath.hasRemainder(points[i].getX(), minAllowedResolution))
            {
                count++;
                resolutionError = TextUtils.formatDouble(Math.abs(((points[i].getX()/minAllowedResolution) % 1) * minAllowedResolution)) +
                        "(X=" + points[i].getX() + ")";
//                DBMath.hasRemainder(points[i].getX(), minAllowedResolution);
                break; // with one error is enough
            }
            else if (DBMath.hasRemainder(points[i].getY(), minAllowedResolution))
            {
                count++;
                resolutionError = TextUtils.formatDouble(Math.abs(((points[i].getY()/minAllowedResolution) % 1) * minAllowedResolution)) +
                        "(Y=" + points[i].getY() + ")";
//                DBMath.hasRemainder(points[i].getY(), minAllowedResolution);
                break;
            }
		}
		if (count == 0) return false; // no error

        // there was an error, for now print error
        Layer layer = poly.getLayer();
        reportError(DRCErrorType.RESOLUTION, " resolution of " + resolutionError + " less than " + minAllowedResolution +
                " on layer " + layer.getName(), cell, 0, 0, null, null, geom, null, null, null, null);
        return true;
	}

    /**
     * Private method to check if geometry is covered 100% by exclusion region
     * @param geo
     * @return true if geometry is covered by exclusion
     */
    private boolean coverByExclusion(Geometric geo)
    {
        Cell cell = geo.getParent();
        Area area = exclusionMap.get(cell);
        if (area != null && area.contains(geo.getBounds()))
        {
            System.out.println("DRC Exclusion found");
            return true;
        }
        return false;
    }

    /**
     * Method to check the design rules about nodeinst "ni".
     * @param ni
     * @param globalIndex
     * @return true if an error was found.
     */
	private boolean checkNodeInst(NodeInst ni, int globalIndex)
	{
		Cell cell = ni.getParent();
		Netlist netlist = getCheckProto(cell).netlist;
		NodeProto np = ni.getProto();
		Technology tech = np.getTechnology();
		AffineTransform trans = ni.rotateOut();
		boolean errorsFound = false;

		// Skipping special nodes
		if (NodeInst.isSpecialNode(ni)) return false; // Oct 5;

        if (np.getFunction() == PrimitiveNode.Function.PIN) return false; // Sept 30

        if (coverByExclusion(ni)) return false; // no errors

        // Already done
		if (nodesMap.get(ni) != null)
			return (false);
		nodesMap.put(ni, ni);

        if (np instanceof PrimitiveNode && DRC.isForbiddenNode(((PrimitiveNode)np).getPrimNodeIndexInTech(), -1,
                DRCTemplate.DRCRuleType.FORBIDDEN, tech))
        {
            reportError(DRCErrorType.FORBIDDEN, " is not allowed by selected foundry", cell, -1, -1, null, null, ni, null, null, null, null);
            if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
            errorsFound = true;
        }

		// get all of the polygons on this node
		Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, null, null, true, ignoreCenterCuts, null);
		convertPseudoLayers(ni, nodeInstPolyList);
		int tot = nodeInstPolyList.length;
        boolean isTransistor =  np.getFunction().isTransistor();

        // examine the polygons on this node
		for(int j=0; j<tot; j++)
		{
			Poly poly = nodeInstPolyList[j];
			Layer layer = poly.getLayer();
			if (layer == null) continue;

            // Checking combination
            boolean ret = checkOD2Combination(tech, ni, layer);
            if (ret)
            {
                // panic errors -> return regarless errorTypeSearch
                return true;
            }

			poly.transform(trans);

            // Checking resolution only after transformation
            ret = checkResolution(poly, cell, ni);
            if (ret)
			{
				if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}

			// determine network for this polygon
			int netNumber = getDRCNetNumber(netlist, poly.getPort(), ni, globalIndex);

			ret = badBox(poly, layer, netNumber, tech, ni, trans, cell, globalIndex);
			if (ret)
			{
				if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
			ret = checkMinWidth(ni, layer, poly, tot==1);
			if (ret)
			{
				if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
			// Check select over transistor poly
			// Assumes polys on transistors fulfill condition by construction
			ret = !isTransistor && checkSelectOverPolysilicon(ni, layer, poly, cell);
			if (ret)
			{
				if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
            ret = checkExtensionRule(ni, layer, poly, cell);
            if (ret)
			{
				if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
			if (tech == layersValidTech && !layersValid[layer.getIndex()])
			{
				reportError(DRCErrorType.BADLAYERERROR, null, cell, 0, 0, null,
					poly, ni, layer, null, null, null);
				if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
		}

		// check node for minimum size
		DRC.NodeSizeRule sizeRule = DRC.getMinSize(np);
		if (sizeRule != null)
		{
			if (DBMath.isGreaterThan(sizeRule.sizeX, ni.getXSize()) ||
                DBMath.isGreaterThan(sizeRule.sizeY, ni.getYSize()))
			{
				SizeOffset so = ni.getSizeOffset();
				double minSize = 0, actual = 0;
                String msg = "X axis";
				if (sizeRule.sizeX - ni.getXSize() > sizeRule.sizeY - ni.getYSize())
				{
					minSize = sizeRule.sizeX - so.getLowXOffset() - so.getHighXOffset();
					actual = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
				} else
				{
                    msg = "Y axis";
					minSize = sizeRule.sizeY - so.getLowYOffset() - so.getHighYOffset();
					actual = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
				}
				reportError(DRCErrorType.MINSIZEERROR, msg, cell, minSize, actual, sizeRule.rule,
					null, ni, null, null, null, null);
                errorsFound = true;
			}
		}
		return errorsFound;
	}

    /**
     * Method to check which combination of OD2 layers are allowed
     * @param layer
     * @return
     */
    private boolean checkOD2Combination(Technology tech, NodeInst ni, Layer layer)
    {
        int funExtras = layer.getFunctionExtras();
        boolean notOk = false;

        if (layer.getFunction().isImplant() && (funExtras&Layer.Function.THICK) != 0)
        {
            // Only stores first node found
            od2Layers.put(layer, ni);

            // More than one type used.
            if (od2Layers.size() != 1)
            {
                for (Map.Entry<Layer,NodeInst> e : od2Layers.entrySet())
                {
                    Layer lay1 = e.getKey();
                    if (lay1 == layer) continue;
                    if (DRC.isForbiddenNode(lay1.getIndex(), layer.getIndex(), DRCTemplate.DRCRuleType.COMBINATION, tech))
                    {
                        NodeInst node = e.getValue(); // od2Layers.get(lay1);
                        String message = "- combination of layers '" + layer.getName() + "' and '" + lay1.getName() + "' (in '" +
                                node.getParent().getName() + ":" + node.getName() +"') not allowed by selected foundry";
                        reportError(DRCErrorType.FORBIDDEN, message, ni.getParent(), -1, -1, null, null, ni, null, null, node, null);

                        return true;
                    }
                }
            }
        }
        return notOk;
    }

	/**
	 * Method to check the design rules about arcinst "ai".
	 * Returns true if errors were found.
	 */
	private boolean checkArcInst(CheckProto cp, ArcInst ai, int globalIndex)
	{
		// ignore arcs with no topology
		Network net = cp.netlist.getNetwork(ai, 0);
		if (net == null) return false;
		Integer [] netNumbers = networkLists.get(net);

		if (nodesMap.get(ai) != null)
		{
			if (Job.LOCALDEBUGFLAG) System.out.println("Done already arc " + ai.getName());
			return (false);
		}
		nodesMap.put(ai, ai);

        // Check if arc is contained in exclusion region
//        if (coverByExclusion(ai))
//            return false; // no error

		// get all of the polygons on this arc
		Technology tech = ai.getProto().getTechnology();
		Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
		boolean errorsFound = false;

        // Check resolution before cropping the
        for(int j=0; j<arcInstPolyList.length; j++)
		{
			Poly poly = arcInstPolyList[j];
			Layer layer = poly.getLayer();
			if (layer == null) continue;
			if (layer.isNonElectrical()) continue;
            // Checking resolution
            boolean ret = checkResolution(poly, ai.getParent(), ai);
            if (ret)
			{
				if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
        }

		cropActiveArc(ai, arcInstPolyList);
		int tot = arcInstPolyList.length;
		// examine the polygons on this arc
		for(int j=0; j<tot; j++)
		{
			Poly poly = arcInstPolyList[j];
			Layer layer = poly.getLayer();
			if (layer == null) continue;
			if (layer.isNonElectrical()) continue;

			int layerNum = layer.getIndex();
			int netNumber = netNumbers[globalIndex].intValue();
			boolean ret = badBox(poly, layer, netNumber, tech, ai, DBMath.MATID, ai.getParent(), globalIndex);
			if (ret)
			{
				if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
			ret = checkMinWidth(ai, layer, poly, tot==1);
			if (ret)
			{
				if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
			// Check select over transistor poly
			ret = checkSelectOverPolysilicon(ai, layer, poly, ai.getParent());
			if (ret)
			{
				if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
			if (tech == layersValidTech && !layersValid[layerNum])
			{
				reportError(DRCErrorType.BADLAYERERROR, null, ai.getParent(), 0, 0, null,
					(tot==1)?null:poly, ai, layer, null, null, null);
				if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
		}
		return errorsFound;
	}

	/**
	 * Method to check the design rules about cell instance "ni".  Only check other
	 * instances, and only check the parts of each that are within striking range.
	 * Returns true if an error was found.
	 */
	private boolean checkCellInst(NodeInst ni, int globalIndex)
	{
		// get transformation out of the instance
        AffineTransform upTrans = ni.translateOut(ni.rotateOut());

		// get network numbering for the instance
		CheckInst ci = checkInsts.get(ni);
		int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;
        boolean errorFound = false;

		// look for other instances surrounding this one
		Rectangle2D nodeBounds = ni.getBounds();
		Rectangle2D searchBounds = new Rectangle2D.Double(
			nodeBounds.getMinX()-worstInteractionDistance,
			nodeBounds.getMinY()-worstInteractionDistance,
			nodeBounds.getWidth() + worstInteractionDistance*2,
			nodeBounds.getHeight() + worstInteractionDistance*2);

        instanceInteractionList.clear(); // part3
		for(Iterator<Geometric> it = ni.getParent().searchIterator(searchBounds); it.hasNext(); )
		{
			Geometric geom = it.next();

			if ( geom == ni ) continue; // covered by checkInteraction?

			if (!(geom instanceof NodeInst)) continue;
			NodeInst oNi = (NodeInst)geom;

			// only check other nodes that are numerically higher (so each pair is only checked once)
			if (oNi.getNodeIndex() <= ni.getNodeIndex()) continue;
			if (!oNi.isCellInstance()) continue;

			// see if this configuration of instances has already been done
			if (checkInteraction(ni, null, oNi, null, null)) continue;

			// found other instance "oNi", look for everything in "ni" that is near it
			Rectangle2D nearNodeBounds = oNi.getBounds();
			Rectangle2D subBounds = new Rectangle2D.Double(
				nearNodeBounds.getMinX()-worstInteractionDistance,
				nearNodeBounds.getMinY()-worstInteractionDistance,
				nearNodeBounds.getWidth() + worstInteractionDistance*2,
				nearNodeBounds.getHeight() + worstInteractionDistance*2);

			// recursively search instance "ni" in the vicinity of "oNi"
			boolean ret = checkCellInstContents(subBounds, ni, upTrans, localIndex, oNi, null, globalIndex, null);
			if (ret) errorFound = true;
		}
		return errorFound;
	}

	/**
	 * Method to recursively examine the area "bounds" in cell "cell" with global index "globalIndex".
	 * The objects that are found are transformed by "uptrans" to be in the space of a top-level cell.
	 * They are then compared with objects in "oNi" (which is in that top-level cell),
	 * which has global index "topGlobalIndex".
	 */
	private boolean checkCellInstContents(Rectangle2D bounds, NodeInst thisNi, AffineTransform upTrans, int globalIndex,
                                          NodeInst oNi, NodeInst oNiParent, int topGlobalIndex, NodeInst triggerNi)
	{
        // Job aborted or scheduled for abort
		if (job != null && job.checkAbort()) return true;

		Cell cell = (Cell)thisNi.getProto();
		boolean logsFound = false;
		Netlist netlist = getCheckProto(cell).netlist;
		Technology cellTech = cell.getTechnology();

		// Need to transform bounds coordinates first otherwise it won't
		// never overlap
		Rectangle2D bb = (Rectangle2D)bounds.clone();
		AffineTransform downTrans = thisNi.transformIn();
		DBMath.transformRect(bb, downTrans);

		for(Iterator<Geometric> it = cell.searchIterator(bb); it.hasNext(); )
		{
			Geometric geom = it.next();

            // Checking if element is covered by exclusion region
//            if (coverByExclusion(geom))
//                continue; // skips this element

			if (geom instanceof NodeInst)
			{
			    NodeInst ni = (NodeInst)geom;
				NodeProto np = ni.getProto();

                if (NodeInst.isSpecialNode(ni)) continue; // Oct 5'04

				if (ni.isCellInstance())
				{
                    // see if this configuration of instances has already been done
                    if (checkInteraction(ni, thisNi, oNi, oNiParent, triggerNi)) continue;  // Jan 27'05. Removed on May'05
                    // You can't discard by interaction becuase two cells could be visited many times
                    // during this type of checking

					AffineTransform subUpTrans = ni.translateOut(ni.rotateOut());
					subUpTrans.preConcatenate(upTrans);

					CheckInst ci = checkInsts.get(ni);

					int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;

					// changes Sept04: subBound by bb
					boolean ret = checkCellInstContents(bb, ni, subUpTrans, localIndex, oNi, oNiParent, topGlobalIndex,
                            (triggerNi==null)?ni:triggerNi);
					if (ret)
					{
						if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
						logsFound = true;
					}
				} else
				{
					AffineTransform rTrans = ni.rotateOut();
					rTrans.preConcatenate(upTrans);
					Technology tech = np.getTechnology();
					Poly [] primPolyList = tech.getShapeOfNode(ni, null, null, true, ignoreCenterCuts, null);
					convertPseudoLayers(ni, primPolyList);
					int tot = primPolyList.length;
					for(int j=0; j<tot; j++)
					{
						Poly poly = primPolyList[j];
						Layer layer = poly.getLayer();
						if (layer == null)
                        {
                            if (Job.LOCALDEBUGFLAG) System.out.println("When is this case?");
                            continue;
                        }

						poly.transform(rTrans);

						// determine network for this polygon
						int net = getDRCNetNumber(netlist, poly.getPort(), ni, globalIndex);
						boolean ret = badSubBox(poly, layer, net, tech, ni, rTrans,
							globalIndex, oNi, topGlobalIndex);
						if (ret)
						{
							if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
							logsFound = true;
						}
					}
				}
			} else
			{
				ArcInst ai = (ArcInst)geom;
				Technology tech = ai.getProto().getTechnology();

				if (tech != cellTech)
				{
					reportError(DRCErrorType.TECHMIXWARN, " belongs to " + tech.getTechName(), cell, 0, 0, null, null, ai, null, null, null, null);
					continue;
				}

				Poly [] arcPolyList = tech.getShapeOfArc(ai);
				int tot = arcPolyList.length;
				for(int j=0; j<tot; j++)
					arcPolyList[j].transform(upTrans);
				cropActiveArc(ai, arcPolyList);
				for(int j=0; j<tot; j++)
				{
					Poly poly = arcPolyList[j];
					Layer layer = poly.getLayer();
					if (layer == null) continue;
					if (layer.isNonElectrical()) continue;
					Network jNet = netlist.getNetwork(ai, 0);
					int net = -1;
					if (jNet != null)
					{
						Integer [] netList = networkLists.get(jNet);
						net = netList[globalIndex].intValue();
					}
					boolean ret = badSubBox(poly, layer, net, tech, ai, upTrans,
						globalIndex, oNi, topGlobalIndex);
					if (ret)
					{
						if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
						logsFound = true;
					}
				}
			}
		}
		return logsFound;
	}

	/**
	 * Method to examine polygon "poly" layer "layer" network "net" technology "tech" geometry "geom"
	 * which is in cell "cell" and has global index "globalIndex".
	 * The polygon is compared against things inside node "oNi", and that node's parent has global index "topGlobalIndex".
	 */
	private boolean badSubBox(Poly poly, Layer layer, int net, Technology tech, Geometric geom, AffineTransform trans,
                              int globalIndex, NodeInst oNi, int topGlobalIndex)
	{
		// see how far around the box it is necessary to search
		double maxSize = poly.getMaxSize();
		double bound = DRC.getMaxSurround(layer, maxSize);
		if (bound < 0) return false;

		// get bounds
		Rectangle2D bounds = new Rectangle2D.Double();
		bounds.setRect(poly.getBounds2D());
		AffineTransform downTrans = oNi.rotateIn();
		AffineTransform tTransI = oNi.translateIn();
		downTrans.preConcatenate(tTransI);
		DBMath.transformRect(bounds, downTrans);

		AffineTransform upTrans = oNi.translateOut(oNi.rotateOut());

		CheckInst ci = checkInsts.get(oNi);
		int localIndex = topGlobalIndex * ci.multiplier + ci.localIndex + ci.offset;

		// determine if original object has multiple contact cuts
		boolean baseMulti = false;
		if (geom instanceof NodeInst)
		{
			baseMulti = tech.isMultiCutCase((NodeInst)geom);
		}

		// search in the area surrounding the box
		bounds.setRect(bounds.getMinX()-bound, bounds.getMinY()-bound, bounds.getWidth()+bound*2, bounds.getHeight()+bound*2);
		return (badBoxInArea(poly, layer, tech, net, geom, trans, globalIndex, bounds, (Cell)oNi.getProto(), localIndex,
                oNi.getParent(), topGlobalIndex, upTrans, baseMulti, false));
        // true in this case you don't want to check Geometric.objectsTouch(nGeom, geom) if nGeom and geom are in the
        // same cell because they could come from different cell instances.
	}

	/**
	 * Method to examine a polygon to see if it has any errors with its surrounding area.
	 * The polygon is "poly" on layer "layer" on network "net" from technology "tech" from object "geom".
	 * Checking looks in cell "cell" global index "globalIndex".
	 * Object "geom" can be transformed to the space of this cell with "trans".
	 * Returns TRUE if a spacing error is found relative to anything surrounding it at or below
	 * this hierarchical level.
	 */
	private boolean badBox(Poly poly, Layer layer, int net, Technology tech, Geometric geom,
                           AffineTransform trans, Cell cell, int globalIndex)
	{
		// see how far around the box it is necessary to search
		double maxSize = poly.getMaxSize();
		double bound = DRC.getMaxSurround(layer, maxSize);
		if (bound < 0) return false;

		// get bounds
		Rectangle2D bounds = new Rectangle2D.Double();
		bounds.setRect(poly.getBounds2D());

		// determine if original object has multiple contact cuts
		boolean baseMulti = false;
		if (geom instanceof NodeInst)
			baseMulti = tech.isMultiCutCase((NodeInst)geom);

		// search in the area surrounding the box
		bounds.setRect(bounds.getMinX()-bound, bounds.getMinY()-bound, bounds.getWidth()+bound*2, bounds.getHeight()+bound*2);
		return badBoxInArea(poly, layer, tech, net, geom, trans, globalIndex, bounds, cell, globalIndex,
			    cell, globalIndex, DBMath.MATID, baseMulti, true);
	}

	/**
	 * Method to recursively examine a polygon to see if it has any errors with its surrounding area.
	 * The polygon is "poly" on layer "layer" from technology "tech" on network "net" from object "geom"
	 * which is associated with global index "globalIndex".
	 * Checking looks in the area (lxbound-hxbound, lybound-hybound) in cell "cell" global index "cellGlobalIndex".
	 * The polygon coordinates are in the space of cell "topCell", global index "topGlobalIndex",
	 * and objects in "cell" can be transformed by "upTrans" to get to this space.
	 * The base object, in "geom" can be transformed by "trans" to get to this space.
	 * The minimum size of this polygon is "minSize" and "baseMulti" is TRUE if it comes from a multicut contact.
	 * If the two objects are in the same cell instance (nonhierarchical DRC), then "sameInstance" is TRUE.
	 * If they are from different instances, then "sameInstance" is FALSE.
	 *
	 * Returns TRUE if errors are found.
	 */
	private boolean badBoxInArea(Poly poly, Layer layer, Technology tech, int net, Geometric geom, AffineTransform trans,
                                 int globalIndex, Rectangle2D bounds, Cell cell, int cellGlobalIndex,
                                 Cell topCell, int topGlobalIndex, AffineTransform upTrans, boolean baseMulti,
                                 boolean sameInstance)
	{
		Rectangle2D rBound = new Rectangle2D.Double();
		rBound.setRect(bounds);
		DBMath.transformRect(rBound, upTrans); // Step 1
		Netlist netlist = getCheckProto(cell).netlist;
        Rectangle2D subBound = new Rectangle2D.Double(); //Sept 30
        boolean foundError = false;

		// These nodes won't generate any DRC errors. Most of them are pins
		if (geom instanceof NodeInst && NodeInst.isSpecialNode(((NodeInst)geom)))
			return false;

		// Sept04 changes: bounds by rBound
		for(Iterator<Geometric> it = cell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric nGeom = it.next();
            // I have to check if they are the same instance otherwise I check geometry against itself
            if (nGeom == geom && (sameInstance))// || nGeom.getParent() == cell))
                continue;

            // Checking if element is covered by exclusion region
            //if (coverByExclusion(nGeom)) continue; // skips this element

			if (nGeom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)nGeom;
				NodeProto np = ni.getProto();

				if (NodeInst.isSpecialNode(ni)) continue; // Oct 5;

				// ignore nodes that are not primitive
				if (ni.isCellInstance())
				{
					// instance found: look inside it for offending geometry
					AffineTransform rTransI = ni.rotateIn();
					AffineTransform tTransI = ni.translateIn();
					rTransI.preConcatenate(tTransI);
					subBound.setRect(bounds);
					DBMath.transformRect(subBound, rTransI);

					CheckInst ci = checkInsts.get(ni);
					int localIndex = cellGlobalIndex * ci.multiplier + ci.localIndex + ci.offset;

					AffineTransform subTrans = ni.translateOut(ni.rotateOut());
					subTrans.preConcatenate(upTrans);        //Sept 15 04

					// compute localIndex
					if (badBoxInArea(poly, layer, tech, net, geom, trans, globalIndex, subBound, (Cell)np, localIndex,
						             topCell, topGlobalIndex, subTrans, baseMulti, sameInstance))
                    {
                        foundError = true;
                        if (errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                    }
				} else
				{
					// don't check between technologies
					if (np.getTechnology() != tech) continue;

					// see if this type of node can interact with this layer
					if (!checkLayerWithNode(layer, np)) continue;

//                    System.out.println("Checking Node " + ni.getName() + " " + layer.getName() + " " +
//                            geom);

                    // see if the objects directly touch but they are not
                    // coming from different NodeInst (not from checkCellInstContents
                    // because they might below to the same cell but in different instances
					boolean touch = sameInstance && Geometric.objectsTouch(nGeom, geom);

					// prepare to examine every layer in this nodeinst
					AffineTransform rTrans = ni.rotateOut();
					rTrans.preConcatenate(upTrans);

					// get the shape of each nodeinst layer
					Poly [] subPolyList = tech.getShapeOfNode(ni, null, null, true, ignoreCenterCuts, null);
					convertPseudoLayers(ni, subPolyList);
					int tot = subPolyList.length;
					for(int i=0; i<tot; i++)
						subPolyList[i].transform(rTrans);
					    /* Step 1 */
					boolean multi = baseMulti;
					if (!multi) multi = tech.isMultiCutCase(ni);
                    int multiInt = (multi) ? 1 : 0;
					for(int j=0; j<tot; j++)
					{
						Poly npoly = subPolyList[j];
						Layer nLayer = npoly.getLayer();
						if (nLayer == null) continue;
                        //npoly.transform(rTrans);

                        Rectangle2D nPolyRect = npoly.getBounds2D();

						// can't do this because "lxbound..." is local but the poly bounds are global
                        // On the corners?
                        if (DBMath.isGreaterThan(nPolyRect.getMinX(), rBound.getMaxX()) ||
							DBMath.isGreaterThan(rBound.getMinX(), nPolyRect.getMaxX())  ||
							DBMath.isGreaterThan(nPolyRect.getMinY(), rBound.getMaxY()) ||
							DBMath.isGreaterThan(rBound.getMinY(), nPolyRect.getMaxY()))
                            continue;

						// determine network for this polygon
						int nNet = getDRCNetNumber(netlist, npoly.getPort(), ni, cellGlobalIndex);

						// see whether the two objects are electrically connected
						boolean con = false;
						if (nNet >= 0 && nNet == net) con = true;

                        // Checking extension, it could be slow
                        boolean ret = checkExtensionGateRule(geom, layer, poly, nLayer, npoly, netlist);
                        if (ret)
                        {
                            foundError = true;
                            if (errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                        }

                        // check if both polys are cut and if the combine area doesn't excess cut sizes
                        // regardless if they are connected or not
                        ret = checkCutSizes(np, geom, layer, poly, nGeom, nLayer, npoly, topCell);
                        if (ret)
                        {
                            foundError = true;
                            if (errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                        }

						// if they connect electrically and adjoin, don't check
						if (con && touch)
                        {
                            // Check if there are minimum size defects
                            boolean maytouch = mayTouch(tech, con, layer, nLayer);
                            Rectangle2D trueBox1 = poly.getBox();
                            if (trueBox1 == null) trueBox1 = poly.getBounds2D();
                            Rectangle2D trueBox2 = npoly.getBox();
                            if (trueBox2 == null) trueBox1 = npoly.getBounds2D();
                            ret = checkMinDefects(cell,maytouch, geom, poly, trueBox1, layer,
                                    nGeom, npoly, trueBox2, nLayer, topCell);
                            if (ret)
                            {
                                foundError = true;
                                if (errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                            }
                            continue;
                        }

						boolean edge = false;
                        // @TODO check if previous spacing rule was composed of same layers!
                        DRCTemplate theRule = getSpacingRule(layer, poly, geom, nLayer, npoly, ni, con, multiInt);
                        if (theRule == null)
                        {
						    theRule = DRC.getEdgeRule(layer, nLayer);
                            edge = true;
                        }

						if (theRule != null)
                        {
                            ret = checkDist(tech, topCell, topGlobalIndex,
                                poly, layer, net, geom, trans, globalIndex,
                                npoly, nLayer, nNet, nGeom, rTrans, cellGlobalIndex,
                                con, theRule, edge);
                            if (ret)
                            {
                                foundError = true;
                                if (errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                            }
                        }
					}
				}
			} else
			{
				ArcInst ai = (ArcInst)nGeom;
				ArcProto ap = ai.getProto();

				// don't check between technologies
				if (ap.getTechnology() != tech) continue;

				// see if this type of arc can interact with this layer
				if (!checkLayerWithArc(layer, ap)) continue;

//                System.out.println("Checking Arc " + ai.getName() + " " + layer.getName() + " " +
//                            geom);

                // see if the objects directly touch
				boolean touch = sameInstance && Geometric.objectsTouch(nGeom, geom);

				// see whether the two objects are electrically connected
                Network jNet = netlist.getNetwork(ai, 0);
                Integer [] netNumbers = networkLists.get(jNet);
                int nNet = netNumbers[cellGlobalIndex].intValue();
				boolean con = false;
				if (net >= 0 && nNet == net) con = true;

				// if they connect electrically and adjoin, don't check
//				if (con && touch) continue;

				// get the shape of each arcinst layer
				Poly [] subPolyList = tech.getShapeOfArc(ai);
				int tot = subPolyList.length;
				for(int i=0; i<tot; i++)
					subPolyList[i].transform(upTrans);
				cropActiveArc(ai, subPolyList);
				boolean multi = baseMulti;
                int multiInt = (multi) ? 1 : 0;

				for(int j=0; j<tot; j++)
				{
					Poly nPoly = subPolyList[j];
					Layer nLayer = nPoly.getLayer();
					if (nLayer == null) continue;
					Rectangle2D nPolyRect = nPoly.getBounds2D();

					// can't do this because "lxbound..." is local but the poly bounds are global
					if (nPolyRect.getMinX() > rBound.getMaxX() ||
						nPolyRect.getMaxX() < rBound.getMinX() ||
						nPolyRect.getMinY() > rBound.getMaxY() ||
						nPolyRect.getMaxY() < rBound.getMinY()) continue;

                    boolean ret = false;
				    // if they connect electrically and adjoin, don't check
                    // We must check if there are minor defects if they overlap regardless if they are connected or not
				    if (con && touch)
                    {
                        // Check if there are minimum size defects
                        boolean maytouch = mayTouch(tech, con, layer, nLayer);
                        Rectangle2D trueBox1 = poly.getBox();
                        if (trueBox1 == null) trueBox1 = poly.getBounds2D();
                        Rectangle2D trueBox2 = nPoly.getBox();
                        if (trueBox2 == null) trueBox1 = nPoly.getBounds2D();
                        ret = checkMinDefects(cell, maytouch, geom, poly, trueBox1, layer,
                                nGeom, nPoly, trueBox2, nLayer, topCell);
                        if (ret)
                        {
                            foundError = true;
                            if (errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                        }
                        continue;
                    }

					// see how close they can get
                    boolean edge = false;
                    DRCTemplate theRule = getSpacingRule(layer, poly, geom, nLayer, nPoly, ai, con, multiInt);
                    if (theRule == null)
                    {
                        theRule = DRC.getEdgeRule(layer, nLayer);
                        edge = true;
                    }
                    if (theRule == null) continue;

					// check the distance
				    ret = checkDist(tech, topCell, topGlobalIndex, poly, layer, net, geom, trans, globalIndex,
                            nPoly, nLayer, nNet, nGeom, upTrans, cellGlobalIndex, con, theRule, edge);
					if (ret)
                    {
                        foundError = true;
                        if (errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                    }

                    // Checking extension, it could be slow
                    ret = checkExtensionGateRule(geom, layer, poly, nLayer, nPoly, netlist);
                    if (ret)
                    {
                        foundError = true;
                        if (errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                    }
				}
			}
		}
		return foundError;
	}

    /**
     * Function to detect small edges due to overlapping polygons of the same material. It is valid only
     * for manhattan shapes
     * @return true if error was found
     */
    private boolean checkMinDefects(Cell cell, boolean maytouch, Geometric geom1, Poly poly1, Rectangle2D trueBox1, Layer layer1,
                                    Geometric geom2, Poly poly2, Rectangle2D trueBox2, Layer layer2, Cell topCell)
    {
        if (trueBox1 == null || trueBox2 == null) return false;
        if (!maytouch) return false;
        // manhattan
        double pdx = Math.max(trueBox2.getMinX()-trueBox1.getMaxX(), trueBox1.getMinX()-trueBox2.getMaxX());
        double pdy = Math.max(trueBox2.getMinY()-trueBox1.getMaxY(), trueBox1.getMinY()-trueBox2.getMaxY());
        double pd = Math.max(pdx, pdy);
        if (pdx == 0 && pdy == 0) pd = 0; // touching
        boolean foundError = false;

        // They have to overlap
		if (pd > 0) return false;

        // they are electrically connected and they overlap: look for minimum size errors
        // of the overlapping region.
        DRCTemplate wRule = DRC.getMinValue(layer1, DRCTemplate.DRCRuleType.MINWID);
        if (wRule == null) return false; // no rule

        double minWidth = wRule.value1;
        double lxb = Math.max(trueBox1.getMinX(), trueBox2.getMinX());
        double hxb = Math.min(trueBox1.getMaxX(), trueBox2.getMaxX());
        double lyb = Math.max(trueBox1.getMinY(), trueBox2.getMinY());
        double hyb = Math.min(trueBox1.getMaxY(), trueBox2.getMaxY());
        Point2D lowB = new Point2D.Double(lxb, lyb);
        Point2D highB = new Point2D.Double(hxb, hyb);
        Rectangle2D bounds = new Rectangle2D.Double(lxb, lyb, hxb-lxb, hyb-lyb);

        // If resulting bounding box is identical to one of the original polygons
        // then one polygon is contained in the other one
        if (bounds.equals(trueBox1) || bounds.equals(trueBox2))
        {
            return false;
        }

        //+-------------------+F
        //|                   |
        //|                   |
        //|                   |B        E
        //|           +-------+---------+
        //|           |       |         |
        //|C         A|       |         |
        //+-----------+-------+         |
        //            |                 |
        //           D+-----------------+

        // Checking A-B distance
        double actual = lowB.distance(highB);
        if (actual != 0 && DBMath.isGreaterThan(minWidth, actual) &&
            foundSmallSizeDefect(cell, geom1, poly1, layer1, geom2, poly2, pd, lxb, lyb, hxb, hyb))
        {
            reportError(DRCErrorType.MINWIDTHERROR, null, topCell, minWidth, actual, wRule.ruleName, new Poly(bounds),
                            geom1, layer1, null, geom2, layer2);
            foundError = true;
        }
        return foundError;
    }

    private boolean foundSmallSizeDefect(Cell cell, Geometric geom1, Poly poly1, Layer layer1,
                                         Geometric geom2, Poly poly2,
                                         double pd, double lxb, double lyb, double hxb, double hyb)
    {
        boolean foundError = false;
        boolean [] pointsFound = new boolean[2];
        pointsFound[0] = pointsFound[1] = false;
        Point2D lowB = new Point2D.Double(lxb, lyb);
        Point2D highB = new Point2D.Double(hxb, hyb);
        Rectangle2D bounds = new Rectangle2D.Double(lxb, lyb, hxb-lxb, hyb-lyb);
        Poly rebuild = new Poly(bounds);
        Point2D pt1 = new Point2D.Double(lxb-TINYDELTA, lyb-TINYDELTA);
        Point2D pt2 = new Point2D.Double(lxb-TINYDELTA, hyb+TINYDELTA);
        Point2D pt1d = (Point2D)pt1.clone();
        Point2D pt2d = (Point2D)pt2.clone();

        // Search area should be bigger than bounding box otherwise it might not get the cells due to
        // rounding errors.
        Rectangle2D search = new Rectangle2D.Double(DBMath.round(lxb-TINYDELTA), DBMath.round(lyb-TINYDELTA),
                DBMath.round(hxb-lxb+2*TINYDELTA), DBMath.round(hyb-lyb+2*TINYDELTA));

        // Looking for two corners not inside bounding boxes A and B
        if (pd == 0) // flat bounding box
        {
            pt1 = lowB;
            pt2 = highB;
            pt1d = lowB;
            pt2d = highB;
        }
        else
        {
            Point2D[] points = rebuild.getPoints();
            int[] cornerPoints = new int[2];
            int corners = 0;  // if more than 3 corners are found -> bounding box is flat
            for (int i = 0; i < points.length && corners < 2; i++)
            {
                if (!DBMath.pointInsideRect(points[i], poly1.getBounds2D()) &&
                    !DBMath.pointInsideRect(points[i], poly2.getBounds2D()))
                {
                    cornerPoints[corners++] = i;
                }
            }
            if (corners != 2)
                throw new Error("Wrong corners in Quick.foundSmallSizeDefect()");

            pt1 = points[cornerPoints[0]];
            pt2 = points[cornerPoints[1]];
            double delta = ((pt2.getY() - pt1.getY())/(pt2.getX() - pt1.getX()) > 0) ? 1 : -1;
            // getting point lightly out of the elements
            if (pt1.getX() < pt2.getX())
            {
                // (y2-y1)/(x2-x1)(x1-tinydelta-x1) + y1
                pt1d = new Point2D.Double(pt1.getX()-TINYDELTA, -delta*TINYDELTA+pt1.getY());
                // (y2-y1)/(x2-x1)(x2+tinydelta-x2) + y2
                pt2d = new Point2D.Double(pt2.getX()+TINYDELTA, delta*TINYDELTA+pt2.getY());
            }
            else
            {
                pt1d = new Point2D.Double(pt2.getX()-TINYDELTA, -delta*TINYDELTA+pt2.getY());
                pt2d = new Point2D.Double(pt1.getX()+TINYDELTA, delta*TINYDELTA+pt1.getY());
            }
        }
        if (DBMath.areEquals(pt1.getX(), pt2.getX()) || DBMath.areEquals(pt1.getY(), pt2.getY()))
        {
//            if (Main.LOCALDEBUGFLAG) System.out.println("Escaping this case as the points are not at opposite corners");
            return false;
        }
        // looking if points around the overlapping area are inside another region
        // to avoid the error
        lookForLayerNew(geom1, poly1, geom2, poly2, cell, layer1, DBMath.MATID, search,
                pt1d, pt2d, null, pointsFound, false);
        // Nothing found
        if (!pointsFound[0] && !pointsFound[1])
        {
            foundError = true;
        }
        return (foundError);
    }

    /**
     * Method to determine if it is allowed to have both layers touching.
     * special rule for allowing touching:
     *   the layers are the same and either:
     *     they connect and are *NOT* contact layers
	 *   or:
	 *     they don't connect and are implant layers (substrate/well)
     * @param tech
     * @param con
     * @param layer1
     * @param layer2
     * @return true if the layer may touch
     */
    private boolean mayTouch(Technology tech, boolean con, Layer layer1, Layer layer2)
    {
        boolean maytouch = false;
		if (tech.sameLayer(layer1, layer2))
		{
			Layer.Function fun = layer1.getFunction();
			if (con)
			{
				if (!fun.isContact()) maytouch = true;
			} else
			{
				if (fun.isSubstrate()) maytouch = true;
				// Special cases for thick actives
				else
				{
					// Searching for THICK bit
					int funExtras = layer1.getFunctionExtras();
					if (fun.isDiff() && (funExtras&Layer.Function.THICK) != 0)
					{
						if (Job.LOCALDEBUGFLAG) System.out.println("Thick active found in Quick.checkDist");
						maytouch = true;
					}
				}
			}
		}
        return maytouch;
    }

	/**
	 * Method to compare:
	 *    polygon "poly1" layer "layer1" network "net1" object "geom1"
	 * with:
	 *    polygon "poly2" layer "layer2" network "net2" object "geom2"
	 * The polygons are both in technology "tech" and are in the space of cell "cell"
	 * which has global index "globalIndex".
	 * Note that to transform object "geom1" to this space, use "trans1" and to transform
	 * object "geom2" to this space, use "trans2".
	 * They are connected if "con" is nonzero.
	 * They cannot be less than "dist" apart (if "edge" is nonzero, check edges only)
	 * and the rule for this is "rule".
	 *
	 * Returns TRUE if an error has been found.
	 */
	private boolean checkDist(Technology tech, Cell cell, int globalIndex,
                              Poly poly1, Layer layer1, int net1, Geometric geom1, AffineTransform trans1, int globalIndex1,
                              Poly poly2, Layer layer2, int net2, Geometric geom2, AffineTransform trans2, int globalIndex2,
                              boolean con, DRCTemplate theRule, boolean edge)
	{
		// turn off flag that the nodeinst may be undersized
		tinyNodeInst = null;

		Poly origPoly1 = poly1;
		Poly origPoly2 = poly2;
		Rectangle2D isBox1 = poly1.getBox();
		Rectangle2D trueBox1 = isBox1;
		if (trueBox1 == null) trueBox1 = poly1.getBounds2D();
		Rectangle2D isBox2 = poly2.getBox();
		Rectangle2D trueBox2 = isBox2;
		if (trueBox2 == null) trueBox2 = poly2.getBounds2D();
        boolean errorFound = false;  // remember if there was a min defect error
		boolean maytouch = mayTouch(tech, con, layer1, layer2);

		// special code if both polygons are manhattan
		double pd = 0;
		boolean overlap = false;
		if (isBox1 != null && isBox2 != null)
		{
			// manhattan
			double pdx = Math.max(trueBox2.getMinX()-trueBox1.getMaxX(), trueBox1.getMinX()-trueBox2.getMaxX());
			double pdy = Math.max(trueBox2.getMinY()-trueBox1.getMaxY(), trueBox1.getMinY()-trueBox2.getMaxY());
			pd = Math.max(pdx, pdy);
			if (pdx == 0 && pdy == 0) pd = 0; // touching
			overlap = (pd < 0);
			if (maytouch)
			{
				// they are electrically connected: see if they touch
                if (checkMinDefects(cell, maytouch, geom1, poly1, trueBox1, layer2, geom2, poly2, trueBox2, layer2, cell))
                {
                    if (errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                    errorFound = true;
                }
			}
			// crop out parts of any arc that is covered by an adjoining node
			trueBox1 = new Rectangle2D.Double(trueBox1.getMinX(), trueBox1.getMinY(), trueBox1.getWidth(), trueBox1.getHeight());
			trueBox2 = new Rectangle2D.Double(trueBox2.getMinX(), trueBox2.getMinY(), trueBox2.getWidth(), trueBox2.getHeight());

			if (geom1 instanceof NodeInst)
			{
				if (cropNodeInst((NodeInst)geom1, globalIndex1, trans1,
				        layer2, net2, geom2, trueBox2))
					return errorFound;
			} else
			{
				if (cropArcInst((ArcInst)geom1, layer1, trans1, trueBox1, overlap))
					return errorFound;
			}
			if (geom2 instanceof NodeInst)
			{
				if (cropNodeInst((NodeInst)geom2, globalIndex2, trans2,
				        layer1, net1, geom1, trueBox1))
					return errorFound;
			} else
			{
				if (cropArcInst((ArcInst)geom2, layer2, trans2, trueBox2, overlap))
					return errorFound;
			}
			poly1 = new Poly(trueBox1);
			poly1.setStyle(Poly.Type.FILLED);
			poly2 = new Poly(trueBox2);
			poly2.setStyle(Poly.Type.FILLED);

			// compute the distance
			double lX1 = trueBox1.getMinX();   double hX1 = trueBox1.getMaxX();
			double lY1 = trueBox1.getMinY();   double hY1 = trueBox1.getMaxY();
			double lX2 = trueBox2.getMinX();   double hX2 = trueBox2.getMaxX();
			double lY2 = trueBox2.getMinY();   double hY2 = trueBox2.getMaxY();
			if (edge)
			{
				// calculate the spacing between the box edges
				double pdedge = Math.min(
					Math.min(Math.min(Math.abs(lX1-lX2), Math.abs(lX1-hX2)), Math.min(Math.abs(hX1-lX2), Math.abs(hX1-hX2))),
					Math.min(Math.min(Math.abs(lY1-lY2), Math.abs(lY1-hY2)), Math.min(Math.abs(hY1-lY2), Math.abs(hY1-hY2))));
				pd = Math.max(pd, pdedge);
			} else
			{
				pdx = Math.max(lX2-hX1, lX1-hX2);
				pdy = Math.max(lY2-hY1, lY1-hY2);

				if (pdx == 0 && pdy == 0)
					pd = 0; // they are touching!!
				else
				{
					// They are overlapping if pdx < 0 && pdy < 0
					pd = DBMath.round(Math.max(pdx, pdy));

					if (pd < theRule.value1 && pd > 0)
					{
						pd = poly1.separation(poly2);
					}
				}
			}
		} else
		{
			// nonmanhattan
			Poly.Type style = poly1.getStyle();
			if (style != Poly.Type.FILLED && style != Poly.Type.CLOSED &&
				style != Poly.Type.CROSSED && style != Poly.Type.OPENED &&
				style != Poly.Type.OPENEDT1 && style != Poly.Type.OPENEDT2 &&
				style != Poly.Type.OPENEDT3 && style != Poly.Type.VECTORS) return errorFound;

			style = poly2.getStyle();
			if (style != Poly.Type.FILLED && style != Poly.Type.CLOSED &&
				style != Poly.Type.CROSSED && style != Poly.Type.OPENED &&
				style != Poly.Type.OPENEDT1 && style != Poly.Type.OPENEDT2 &&
				style != Poly.Type.OPENEDT3 && style != Poly.Type.VECTORS) return errorFound;

			// make sure polygons don't intersect
            //@TODO combine this calculation otherwise Poly.intersects is called twice!
            double pd1 = poly1.separation(poly2);
			if (poly1.intersects(poly2)) pd = 0;
            else
			{
				// find distance between polygons
				pd = poly1.separation(poly2);
			}
            if (pd1 != pd)
                System.out.println("Wrong case in non-nonmanhattan, Quick.");
		}

		DRCErrorType errorType = DRCErrorType.SPACINGERROR;
        if (theRule.ruleType == DRCTemplate.DRCRuleType.SURROUND)
        {
            if (pd > 0) // layers don't overlap -> no condition to check
                return errorFound;
            pd = Math.abs(pd);
            errorType = DRCErrorType.SURROUNDERROR;
        }
		// see if the design rule is met
		if (!DBMath.isGreaterThan(theRule.value1, pd)) // default case: SPACING   pd >= theRule.value1
		{
			return errorFound;
		}
        if (theRule.ruleType != DRCTemplate.DRCRuleType.SURROUND)
        {
            /*
             * special case: ignore errors between two active layers connected
             * to either side of a field-effect transistor that is inside of
             * the error area.
             */
            if (activeOnTransistor(poly1, layer1, net1, poly2, layer2, net2, cell, globalIndex))
                return errorFound;

            /**
             * Special case: ignore spacing errors generated by VT layers if poly is already
             * part of a VTH-transistors. Not very elegant solution but should work for now.
             */
            if (tech.polyCoverByAnyVTLayer(cell, theRule, tech, new Poly[]{poly1, poly2}, new Layer[]{layer1, layer2},
                    new Geometric[]{geom1, geom2}, ignoreCenterCuts))
                return errorFound;

            // special cases if the layers are the same
            if (tech.sameLayer(layer1, layer2))
            {
                // special case: check for "notch"
                if (maytouch)
                {
                    // if they touch, it is acceptable
                    if (pd <= 0) return errorFound;

                    // see if the notch is filled
                    boolean newR = lookForCrossPolygons(geom1, poly1, geom2, poly2, layer1, cell, overlap);
                    if (Job.LOCALDEBUGFLAG)
                    {
                        Point2D pt1 = new Point2D.Double();
                        Point2D pt2 = new Point2D.Double();
                        int intervening = findInterveningPoints(poly1, poly2, pt1, pt2, false);
                        if (intervening == 0)
                        {
                            if (!newR)
                            {
                                System.out.println("DIfferent");
                                lookForCrossPolygons(geom1, poly1, geom2, poly2, layer1, cell, overlap);

                            }
                            //return false;
                        }
                        boolean needBoth = true;
                        if (intervening == 1) needBoth = false;

                        if (lookForPoints(pt1, pt2, layer1, cell, needBoth))
                        {
                            if (!newR)
                            {
                                System.out.println("DIfferent");
                                lookForPoints(pt1, pt2, layer1, cell, needBoth);
                                lookForCrossPolygons(geom1, poly1, geom2, poly2, layer1, cell, overlap);
                            //return false;
                            }
                        }

                        boolean oldR = lookForPoints(pt1, pt2, layer1, cell, needBoth);
                        if (oldR != newR)
                                System.out.println("DIfferent 2");
                    }
                    if (newR) return errorFound;

                    // look further if on the same net and diagonally separate (1 intervening point)
                    //if (net1 == net2 && intervening == 1) return false;
                    errorType = DRCErrorType.NOTCHERROR;
                }
            }
        }

		String msg = null;
		if (tinyNodeInst != null)
		{
			// see if the node/arc that failed was involved in the error
			if ((tinyNodeInst == geom1 || tinyNodeInst == geom2) &&
				(tinyGeometric == geom1 || tinyGeometric == geom2))
			{
				msg = tinyNodeInst + " is too small for the " + tinyGeometric;
			}
		}

		reportError(errorType, msg, cell, theRule.value1, pd, theRule.ruleName, origPoly1, geom1, layer1, origPoly2, geom2, layer2);
		return true;
	}

	/*************************** QUICK DRC SEE IF GEOMETRICS CAUSE ERRORS ***************************/

	/**
	 * Method to examine, in cell "cell", the "count" instances in "geomsToCheck".
	 * If they are DRC clean, set the associated entry in "validity" to TRUE.
	 */
	private void checkTheseGeometrics(Cell cell, int count, Geometric [] geomsToCheck, boolean [] validity)
	{
		CheckProto cp = getCheckProto(cell);

		// loop through all of the objects to be checked
		for(int i=0; i<count; i++)
		{
			Geometric geomToCheck = geomsToCheck[i];
			boolean errors = false;
			if (geomToCheck instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geomToCheck;
				if (ni.isCellInstance())
				{
					errors = checkThisCellPlease(ni);
				} else
				{
					errors = checkNodeInst(ni, 0);
				}
			} else
			{
				ArcInst ai = (ArcInst)geomToCheck;
				errors = checkArcInst(cp, ai, 0);
			}
			if (validity != null) validity[i] = !errors;
		}
	}

	private boolean checkThisCellPlease(NodeInst ni)
	{
		Cell cell = ni.getParent();
		Netlist netlist = getCheckProto(cell).netlist;
		int globalIndex = 0;

		// get transformation out of this instance
		AffineTransform upTrans = ni.translateOut(ni.rotateOut());

		// get network numbering information for this instance
		CheckInst ci = checkInsts.get(ni);
		if (ci == null) return false;
		int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;

		// look for other objects surrounding this one
		Rectangle2D nodeBounds = ni.getBounds();
		Rectangle2D searchBounds = new Rectangle2D.Double(
			nodeBounds.getMinX() - worstInteractionDistance,
			nodeBounds.getMinY() - worstInteractionDistance,
			nodeBounds.getWidth() + worstInteractionDistance*2,
			nodeBounds.getHeight() + worstInteractionDistance*2);

		for(Iterator<Geometric> it = cell.searchIterator(searchBounds); it.hasNext(); )
		{
			Geometric geom = it.next();

			if ( geom == ni ) continue;

			if (geom instanceof ArcInst)
			{
				if (checkGeomAgainstInstance(netlist, geom, ni)) return true;
				continue;
			}
			NodeInst oNi = (NodeInst)geom;

			if (oNi.getProto() instanceof PrimitiveNode)
			{
				// found a primitive node: check it against the instance contents
				if (checkGeomAgainstInstance(netlist, geom, ni)) return true;
				continue;
			}

			// found other instance "oNi", look for everything in "ni" that is near it
			Rectangle2D subNodeBounds = oNi.getBounds();
			Rectangle2D subBounds = new Rectangle2D.Double(
				subNodeBounds.getMinX() - worstInteractionDistance,
				subNodeBounds.getMinY() - worstInteractionDistance,
				subNodeBounds.getWidth() + worstInteractionDistance*2,
				subNodeBounds.getHeight() + worstInteractionDistance*2);

			// recursively search instance "ni" in the vicinity of "oNi"
			if (checkCellInstContents(subBounds, ni, upTrans, localIndex, oNi, null, globalIndex, null)) return true;
		}
		return false;
	}

	/**
	 * Method to check primitive object "geom" (an arcinst or primitive nodeinst) against cell instance "ni".
	 * Returns TRUE if there are design-rule violations in their interaction.
	 */
	private boolean checkGeomAgainstInstance(Netlist netlist, Geometric geom, NodeInst ni)
	{
		NodeProto np = ni.getProto();
		int globalIndex = 0;
		Cell subCell = geom.getParent();
		boolean baseMulti = false;
		Technology tech = null;
		Poly [] nodeInstPolyList = null;
		AffineTransform trans = DBMath.MATID;

		if (geom instanceof NodeInst)
		{
			// get all of the polygons on this node
			NodeInst oNi = (NodeInst)geom;

            if (NodeInst.isSpecialNode(oNi))
                return false; // Nov 16, no need for checking pins or other special nodes;

			tech = oNi.getProto().getTechnology();
			trans = oNi.rotateOut();
			nodeInstPolyList = tech.getShapeOfNode(oNi, null, null, true, ignoreCenterCuts, null);
			convertPseudoLayers(oNi, nodeInstPolyList);
			baseMulti = tech.isMultiCutCase(oNi);
		} else
		{
			ArcInst oAi = (ArcInst)geom;
			tech = oAi.getProto().getTechnology();
			nodeInstPolyList = tech.getShapeOfArc(oAi);
		}
		if (nodeInstPolyList == null) return false;
		int tot = nodeInstPolyList.length;

		CheckInst ci = checkInsts.get(ni);
		if (ci == null) return false;
		int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;

		// examine the polygons on this node
		for(int j=0; j<tot; j++)
		{
			Poly poly = nodeInstPolyList[j];
			Layer polyLayer = poly.getLayer();
			if (polyLayer == null) continue;

			// see how far around the box it is necessary to search
			double maxSize = poly.getMaxSize();
			double bound = DRC.getMaxSurround(polyLayer, maxSize);
			if (bound < 0) continue;

			// determine network for this polygon
			int net;
			if (geom instanceof NodeInst)
			{
				net = getDRCNetNumber(netlist, poly.getPort(), (NodeInst)geom, globalIndex);
			} else
			{
				ArcInst oAi = (ArcInst)geom;
				Network jNet = netlist.getNetwork(oAi, 0);
				Integer [] netNumbers = networkLists.get(jNet);
				net = netNumbers[globalIndex].intValue();
			}

			// determine area to search inside of cell to check this layer
			Rectangle2D polyBounds = poly.getBounds2D();
			Rectangle2D subBounds = new Rectangle2D.Double(polyBounds.getMinX() - bound,
				polyBounds.getMinY()-bound, polyBounds.getWidth()+bound*2, polyBounds.getHeight()+bound*2);
			AffineTransform tempTrans = ni.rotateIn();
			AffineTransform tTransI = ni.translateIn();
			tempTrans.preConcatenate(tTransI);
			DBMath.transformRect(subBounds, tempTrans);

			AffineTransform subTrans = ni.translateOut(ni.rotateOut());

			// see if this polygon has errors in the cell
			if (badBoxInArea(poly, polyLayer, tech, net, geom, trans, globalIndex, subBounds, (Cell)np, localIndex,
                    subCell, globalIndex, subTrans, baseMulti, false)) return true;
		}
		return false;
	}

	/*************************** QUICK DRC CACHE OF INSTANCE INTERACTIONS ***************************/

	/**
	 * Method to look for an interaction between instances "ni1" and "ni2".  If it is found,
	 * return TRUE.  If not found, add to the list and return FALSE.
	 */
	private boolean checkInteraction(NodeInst ni1, NodeInst n1Parent,
                                     NodeInst ni2, NodeInst n2Parent, NodeInst triggerNi)
	{
        if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return false;

		// must recheck parameterized instances always
		CheckProto cp = getCheckProto((Cell)ni1.getProto());
		if (cp.cellParameterized) return false;
		cp = getCheckProto((Cell)ni2.getProto());
		if (cp.cellParameterized) return false;

		// keep the instances in proper numeric order
		InstanceInter dii = new InstanceInter();
		if (ni1.getNodeIndex() < ni2.getNodeIndex())
		{
			NodeInst swapni = ni1;   ni1 = ni2;   ni2 = swapni;
		} else if (ni1 == ni2)
		{
			int node1Orientation = ni1.getAngle();
			if (ni1.isMirroredAboutXAxis()) node1Orientation += 3600;
			if (ni1.isMirroredAboutYAxis()) node1Orientation += 7200;
			int node2Orientation = ni2.getAngle();
			if (ni2.isMirroredAboutXAxis()) node2Orientation += 3600;
			if (ni2.isMirroredAboutYAxis()) node2Orientation += 7200;
			if (node1Orientation < node2Orientation)
			{
				NodeInst swapNI = ni1;   ni1 = ni2;   ni2 = swapNI;
                System.out.println("Check this case in Quick.checkInteraction");
			}
		}

		// get essential information about their interaction
		dii.cell1 = (Cell)ni1.getProto();
		dii.or1 = ni1.getOrient();

		dii.cell2 = (Cell)ni2.getProto();
		dii.or2 = ni2.getOrient();

        // This has to be calculated before the swap
		dii.dx = ni2.getAnchorCenterX() - ni1.getAnchorCenterX();
		dii.dy = ni2.getAnchorCenterY() - ni1.getAnchorCenterY();
        dii.n1Parent = n1Parent;
        dii.n2Parent = n2Parent;
        dii.triggerNi = triggerNi;

		// if found, stop now
		if (findInteraction(dii)) return true;

		// insert it now
		instanceInteractionList.add(dii);

		return false;
	}

	/**
	 * Method to look for the instance-interaction in "dii" in the global list of instances interactions
	 * that have already been checked.  Returns the entry if it is found, NOINSTINTER if not.
	 */
	private boolean findInteraction(InstanceInter dii)
	{
//		for(Iterator it = instanceInteractionList.iterator(); it.hasNext(); )
        for (InstanceInter thisII : instanceInteractionList)
		{
//			InstanceInter thisII = (InstanceInter)it.next();
			if (thisII.cell1 == dii.cell1 && thisII.cell2 == dii.cell2 &&
				thisII.or1.equals(dii.or1) && thisII.or2.equals(dii.or2) &&
				thisII.dx == dii.dx && thisII.dy == dii.dy &&
                thisII.n1Parent == dii.n1Parent && thisII.n2Parent == dii.n2Parent)
            {
                if (dii.triggerNi == thisII.triggerNi)
                {
                    return true;
                }
            }
		}
		return false;
	}

	/************************* QUICK DRC HIERARCHICAL NETWORK BUILDING *************************/

	/**
	 * Method to recursively examine the hierarchy below cell "cell" and create
	 * CheckProto and CheckInst objects on every cell instance.
	 */
	private CheckProto checkEnumerateProtos(Cell cell, Netlist netlist)
	{
		CheckProto cp = getCheckProto(cell);
		if (cp != null) return cp;

		cp = new CheckProto();
		cp.instanceCount = 0;
		cp.timeStamp = 0;
		cp.hierInstanceCount = 0;
		cp.totalPerCell = 0;
		cp.cellChecked = false;
		cp.cellParameterized = false;
		cp.treeParameterized = false;
		cp.netlist = netlist;
        if (cell.getParameters().hasNext()) {
            cp.cellParameterized = true;
            cp.treeParameterized = true;
        }
//		for(Iterator vIt = cell.getVariables(); vIt.hasNext(); )
//		{
//			Variable var = (Variable)vIt.next();
//			if (var.isParam())
//			{
//				cp.cellParameterized = true;
//				cp.treeParameterized = true;
//				break;
//			}
//		}
		checkProtos.put(cell, cp);

		for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
		{
			NodeInst ni = nIt.next();
			if (!ni.isCellInstance()) continue;
			// ignore documentation icons
			if (ni.isIconOfParent()) continue;
			Cell subCell = (Cell)ni.getProto();

			CheckInst ci = new CheckInst();
			checkInsts.put(ni, ci);

			CheckProto subCP = checkEnumerateProtos(subCell, netlist.getNetlist(ni));
			if (subCP.treeParameterized)
				cp.treeParameterized = true;
		}
		return cp;
	}

	/**
	 * Method to return CheckProto of a Cell.
	 * @param cell Cell to get its CheckProto.
	 * @return CheckProto of a Cell.
	 */
	private final CheckProto getCheckProto(Cell cell)
	{
		return checkProtos.get(cell);
	}

	/**
	 * Method to recursively examine the hierarchy below cell "cell" and fill in the
	 * CheckInst objects on every cell instance.  Uses the CheckProto objects
	 * to keep track of cell usage.
	 */
	private void checkEnumerateInstances(Cell cell)
	{
		if (job != null && job.checkAbort()) return;

		// number all of the instances in this cell
		checkTimeStamp++;
		List<CheckProto> subCheckProtos = new ArrayList<CheckProto>();
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			if (!ni.isCellInstance()) continue;

			// ignore documentation icons
			if (ni.isIconOfParent()) continue;

			CheckProto cp = getCheckProto((Cell)np);
			if (cp.timeStamp != checkTimeStamp)
			{
				cp.timeStamp = checkTimeStamp;
				cp.instanceCount = 0;
				cp.nodesInCell = new ArrayList<CheckInst>();
				subCheckProtos.add(cp);
			}

			CheckInst ci = checkInsts.get(ni);
			ci.localIndex = cp.instanceCount++;
			cp.nodesInCell.add(ci);
		}

		// update the counts for this cell
//		for(Iterator it = subCheckProtos.iterator(); it.hasNext(); )
        for (CheckProto cp : subCheckProtos)
		{
//			CheckProto cp = (CheckProto)it.next();
			cp.hierInstanceCount += cp.instanceCount;
//			for(Iterator nIt = cp.nodesInCell.iterator(); nIt.hasNext(); )
            for (CheckInst ci : cp.nodesInCell)
			{
//				CheckInst ci = (CheckInst)nIt.next();
				ci.multiplier = cp.instanceCount;
			}
		}

		// now recurse
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			if (!ni.isCellInstance()) continue;

			// ignore documentation icons
			if (ni.isIconOfParent()) continue;

			checkEnumerateInstances((Cell)np);
		}
	}

	private void checkEnumerateNetworks(Cell cell, CheckProto cp, int globalIndex, HashMap<Network,Integer> enumeratedNets)
	{
		// store all network information in the appropriate place
		for(Iterator<Network> nIt = cp.netlist.getNetworks(); nIt.hasNext(); )
		{
			Network net = nIt.next();
			Integer netNumber = enumeratedNets.get(net);
			Integer [] netNumbers = networkLists.get(net);
			netNumbers[globalIndex] = netNumber;
		}

		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			if (!ni.isCellInstance()) continue;

			// ignore documentation icons
			if (ni.isIconOfParent()) continue;

			// compute the index of this instance
			CheckInst ci = checkInsts.get(ni);
			int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;

			// propagate down the hierarchy
			Cell subCell = (Cell)np;
			CheckProto subCP = getCheckProto(subCell);

			HashMap<Network,Integer> subEnumeratedNets = new HashMap<Network,Integer>();
			for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
			{
				PortInst pi = pIt.next();
				Export subPP = (Export)pi.getPortProto();
				Network net = cp.netlist.getNetwork(ni, subPP, 0);
				if (net == null) continue;
				Integer netNumber = enumeratedNets.get(net);
				Network subNet = subCP.netlist.getNetwork(subPP, 0);
				subEnumeratedNets.put(subNet, netNumber);
			}
			for(Iterator<Network> nIt = subCP.netlist.getNetworks(); nIt.hasNext(); )
			{
				Network net = nIt.next();
				if (subEnumeratedNets.get(net) == null)
					subEnumeratedNets.put(net, new Integer(checkNetNumber++));
			}
			checkEnumerateNetworks(subCell, subCP, localIndex, subEnumeratedNets);
		}
	}

	/*********************************** QUICK DRC SUPPORT ***********************************/

    /**
     * Method to determine if neighbor would help to cover the minimum conditions
     * @return true if error was found (not warning)
     */
    private boolean checkExtensionWithNeighbors(Cell cell, Geometric geom, Poly poly, Layer layer, Rectangle2D bounds,
                                                DRCTemplate minWidthRule, int dir, boolean onlyOne)
    {
        double actual = 0;
        Point2D left1, left2, left3, right1, right2, right3;
        //if (bounds.getWidth() < minWidthRule.value)
        String msg = "";

        // potential problem along X
        if (dir == 0)
        {
            actual = bounds.getWidth();
            msg = "(X axis)";
            double leftW = bounds.getMinX() - TINYDELTA;
            left1 = new Point2D.Double(leftW, bounds.getMinY());
            left2 = new Point2D.Double(leftW, bounds.getMaxY());
            left3 = new Point2D.Double(leftW, bounds.getCenterY());
            double rightW = bounds.getMaxX() + TINYDELTA;
            right1 = new Point2D.Double(rightW, bounds.getMinY());
            right2 = new Point2D.Double(rightW, bounds.getMaxY());
            right3 = new Point2D.Double(rightW, bounds.getCenterY());
        } else
        {
            actual = bounds.getHeight();
            msg = "(Y axis)";
            double leftH = bounds.getMinY() - TINYDELTA;
            left1 = new Point2D.Double(bounds.getMinX(), leftH);
            left2 = new Point2D.Double(bounds.getMaxX(), leftH);
            left3 = new Point2D.Double(bounds.getCenterX(), leftH);
            double rightH = bounds.getMaxY() + TINYDELTA;
            right1 = new Point2D.Double(bounds.getMinX(), rightH);
            right2 = new Point2D.Double(bounds.getMaxX(), rightH);
            right3 = new Point2D.Double(bounds.getCenterX(), rightH);
        }
        // see if there is more of this layer adjoining on either side
        boolean [] pointsFound = new boolean[3];
        pointsFound[0] = pointsFound[1] = pointsFound[2] = false;
        Rectangle2D newBounds = new Rectangle2D.Double(bounds.getMinX()-TINYDELTA, bounds.getMinY()-TINYDELTA,
                bounds.getWidth()+TINYDELTA*2, bounds.getHeight()+TINYDELTA*2);
        boolean zeroWide = (bounds.getWidth() == 0 || bounds.getHeight() == 0);

        boolean overlapLayer = lookForLayer(poly, cell, layer, DBMath.MATID, newBounds,
                left1, left2, left3, pointsFound); //) return false;
        if (overlapLayer && !zeroWide) return false;

        // Try the other corner
        pointsFound[0] = pointsFound[1] = pointsFound[2] = false;
        overlapLayer = lookForLayer(poly, cell, layer, DBMath.MATID, newBounds,
                right1, right2, right3, pointsFound); //) return false;
        if (overlapLayer && !zeroWide) return false;

        DRCErrorType errorType = DRCErrorType.MINWIDTHERROR;
        String extraMsg = msg;
        String rule = minWidthRule.ruleName;
        if (zeroWide)
        {
            if (overlapLayer) extraMsg = " but covered by other layer";
            errorType = DRCErrorType.ZEROLENGTHARCWARN;
            rule = null;
        }

        reportError(errorType, extraMsg, cell, minWidthRule.value1, actual, rule,
                (onlyOne) ? null : poly, geom, layer, null, null, null);
        return !overlapLayer;
    }

	/**
	 * Method to ensure that polygon "poly" on layer "layer" from object "geom" in
	 * technology "tech" meets minimum width rules.  If it is too narrow, other layers
	 * in the vicinity are checked to be sure it is indeed an error.  Returns true
	 * if an error is found.
	 */
	private boolean checkMinWidth(Geometric geom, Layer layer, Poly poly, boolean onlyOne)
	{
		Cell cell = geom.getParent();
		DRCTemplate minWidthRule = DRC.getMinValue(layer, DRCTemplate.DRCRuleType.MINWID);
		if (minWidthRule == null) return false;

		// simpler analysis if manhattan
		Rectangle2D bounds = poly.getBox();
		if (bounds != null)
		{
			boolean tooSmallWidth = DBMath.isGreaterThan(minWidthRule.value1, bounds.getWidth());
			boolean tooSmallHeight = DBMath.isGreaterThan(minWidthRule.value1, bounds.getHeight());
			if (!tooSmallWidth && !tooSmallHeight) return false;

            boolean foundError = false;
            if (tooSmallWidth && checkExtensionWithNeighbors(cell, geom, poly, layer, bounds, minWidthRule, 0, onlyOne))
                foundError = true;
            if (tooSmallHeight && checkExtensionWithNeighbors(cell, geom, poly, layer, bounds, minWidthRule, 1, onlyOne))
                foundError = true;
            return foundError;
		}

		// nonmanhattan polygon: stop now if it has no size
		Poly.Type style = poly.getStyle();
		if (style != Poly.Type.FILLED && style != Poly.Type.CLOSED && style != Poly.Type.CROSSED &&
			style != Poly.Type.OPENED && style != Poly.Type.OPENEDT1 && style != Poly.Type.OPENEDT2 &&
			style != Poly.Type.OPENEDT3 && style != Poly.Type.VECTORS) return false;

		// simple check of nonmanhattan polygon for minimum width
		bounds = poly.getBounds2D();
		double actual = Math.min(bounds.getWidth(), bounds.getHeight());
		if (actual < minWidthRule.value1)
		{
			reportError(DRCErrorType.MINWIDTHERROR, null, cell, minWidthRule.value1, actual, minWidthRule.ruleName,
				(onlyOne) ? null : poly, geom, layer, null, null, null);
			return true;
		}

		// check distance of each line's midpoint to perpendicular opposite point
		Point2D [] points = poly.getPoints();
		int count = points.length;
		for(int i=0; i<count; i++)
		{
			Point2D from;
			if (i == 0) from = points[count-1]; else
				from = points[i-1];
			Point2D to = points[i];
			if (from.equals(to)) continue;

			double ang = DBMath.figureAngleRadians(from, to);
			Point2D center = new Point2D.Double((from.getX() + to.getX()) / 2, (from.getY() + to.getY()) / 2);
			double perpang = ang + Math.PI / 2;
			for(int j=0; j<count; j++)
			{
				if (j == i) continue;
				Point2D oFrom;
				if (j == 0) oFrom = points[count-1]; else
					oFrom = points[j-1];
				Point2D oTo = points[j];
				if (oFrom.equals(oTo)) continue;
				double oAng = DBMath.figureAngleRadians(oFrom, oTo);
				double rAng = ang;   while (rAng > Math.PI) rAng -= Math.PI;
				double rOAng = oAng;   while (rOAng > Math.PI) rOAng -= Math.PI;
				if (DBMath.doublesEqual(oAng, rOAng))
				{
					// lines are parallel: see if they are colinear
					if (DBMath.isOnLine(from, to, oFrom)) continue;
					if (DBMath.isOnLine(from, to, oTo)) continue;
					if (DBMath.isOnLine(oFrom, oTo, from)) continue;
					if (DBMath.isOnLine(oFrom, oTo, to)) continue;
				}
				Point2D inter = DBMath.intersectRadians(center, perpang, oFrom, oAng);
				if (inter == null) continue;
				if (inter.getX() < Math.min(oFrom.getX(), oTo.getX()) || inter.getX() > Math.max(oFrom.getX(), oTo.getX())) continue;
				if (inter.getY() < Math.min(oFrom.getY(), oTo.getY()) || inter.getY() > Math.max(oFrom.getY(), oTo.getY())) continue;
				double fdx = center.getX() - inter.getX();
				double fdy = center.getY() - inter.getY();
				actual = DBMath.round(Math.sqrt(fdx*fdx + fdy*fdy));

				// becuase this is done in integer, accuracy may suffer
//				actual += 2;

				if (actual < minWidthRule.value1)
				{
					// look between the points to see if it is minimum width or notch
					if (poly.isInside(new Point2D.Double((center.getX()+inter.getX())/2, (center.getY()+inter.getY())/2)))
					{
						reportError(DRCErrorType.MINWIDTHERROR, null, cell, minWidthRule.value1,
							actual, minWidthRule.ruleName, (onlyOne) ? null : poly, geom, layer, null, null, null);
					} else
					{
						reportError(DRCErrorType.NOTCHERROR, null, cell, minWidthRule.value1,
							actual, minWidthRule.ruleName, (onlyOne) ? null : poly, geom, layer, poly, geom, layer);
					}
					return true;
				}
			}
		}
		return false;
	}

	private int checkMinAreaLayer(GeometryHandler merge, Cell cell, Layer layer, boolean addError,
                                  HashMap<Layer, Layer> minAreaLayerMapDone, HashMap<Layer, Layer> enclosedAreaLayerMapDone)
	{
		int errorFound = 0;
		DRCTemplate minAreaRule = minAreaLayerMap.get(layer);
		DRCTemplate encloseAreaRule = enclosedAreaLayerMap.get(layer);
        DRCTemplate slotSizeRule = slotSizeLayerMap.get(layer);

		// Layer doesn't have min area nor slot size
		if (minAreaRule == null && encloseAreaRule == null && slotSizeRule == null) return 0;

		Collection<PolyBase> set = merge.getObjects(layer, false, true);

        if (set == null) return 0; // layer is not present in this cell

        boolean minAreaDone = true;
        boolean enclosedAreaDone = true;

//		for(Iterator<PolyBase> pIt = set.iterator(); pIt.hasNext(); )
        for (PolyBase obj : set)
		{
//            PolyBase obj = pIt.next();
			if (obj == null) throw new Error("wrong condition in Quick.checkMinArea()");

			List<PolyBase> list = obj.getSortedLoops();

			// Order depends on area comparison done. First element is the smallest.
			// and depending on # polygons it could be minArea or encloseArea
			DRCTemplate evenRule = (list.size()%2==0) ? encloseAreaRule : minAreaRule;
			DRCTemplate oddRule = (evenRule == minAreaRule) ? encloseAreaRule : minAreaRule;
			// Looping over simple polygons. Possible problems with disconnected elements
			// polyArray.length = Maximum number of distintic loops
            int i = 0;
            for (PolyBase listObj : list)
//			for (int i = 0; i < list.size(); i++)
			{
//                PolyBase listObj = list.get(i);
				double area = ((PolyBase)listObj).getArea();
				DRCTemplate minRule = (i%2 == 0) ? evenRule : oddRule;
                PolyBase simplePn = listObj;
                i++;

                // Check slot size when area is checked
                if (minRule == minAreaRule && slotSizeRule != null)
                {
                    double length = listObj.getMaxLength();

                    if (!DBMath.isGreaterThan(length, slotSizeRule.value1)) continue;
                    if (addError)
                    reportError(DRCErrorType.SLOTSIZEERROR, null, cell, slotSizeRule.value1, length, slotSizeRule.ruleName,
						simplePn, null, layer, null, null, null);
				    errorFound++;
                }

				if (minRule == null) continue;

				// isGreaterThan doesn't consider equals condition therefore negate condition is used
				if (!DBMath.isGreaterThan(minRule.value1, area)) continue;

				errorFound++;
				DRCErrorType errorType = (minRule == minAreaRule) ? DRCErrorType.MINAREAERROR : DRCErrorType.ENCLOSEDAREAERROR;
                if (errorType == DRCErrorType.MINAREAERROR) minAreaDone = false;
                else if (errorType == DRCErrorType.ENCLOSEDAREAERROR) enclosedAreaDone = false;
                if (addError)
				reportError(errorType, null, cell, minRule.value1, area, minRule.ruleName,
						simplePn, null, layer, null, null, null);
			}
		}
        if (minAreaDone && minAreaLayerMapDone != null)
            minAreaLayerMapDone.put(layer, layer);
        if (enclosedAreaDone && enclosedAreaLayerMapDone != null)
            enclosedAreaLayerMapDone.put(layer, layer);
		return errorFound;

	}

    /**
	 * Method to ensure that polygon "poly" on layer "layer" from object "geom" in
	 * technology "tech" meets minimum area rules. Returns true
	 * if an error is found.
	 */
	private int checkMinAreaNew(Cell cell)
	{
		CheckProto cp = getCheckProto(cell);
		int errorFound = 0;

		// Nothing to check
		if (minAreaLayerMap.isEmpty() && enclosedAreaLayerMap.isEmpty())
			return 0;

		// Select/well regions
        HashMap<Cell,GeometryHandler> selectMergeMap = new HashMap<Cell,GeometryHandler>();

        CheckAreaEnumerator quickArea = new CheckAreaEnumerator(selectMergeMap, mergeMode);
        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, quickArea);
        GeometryHandler geom = quickArea.mainMergeMap.get(cell);

		// Get merged areas. Only valid for layers that have connections (metals/polys). No valid for NP/PP rules
        for(Iterator<Layer> layerIt = cell.getTechnology().getLayers(); layerIt.hasNext(); )
		{
			Layer layer = layerIt.next();
            if (minAreaLayerMap.get(layer) == null && enclosedAreaLayerMap.get(layer) == null)
					continue;

			// Job aborted
			if (job != null && job.checkAbort()) return 0;

            errorFound += checkMinAreaLayer(geom, cell, layer, true, null, null);
		}

		// Checking nodes not exported down in the hierarchy. Probably good enought not to collect networks first
//		quickArea = new CheckAreaEnumerator(notExportedNodes, checkedNodes, job.mergeMode);
//		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, cp.netlist, quickArea);
		// Non exported nodes
//		for(Iterator it = quickArea.mainMerge.getKeyIterator(); it.hasNext(); )
//		{
//			Layer layer = (Layer)it.next();
//			boolean localError = checkMinAreaLayer(quickArea.mainMerge, cell, layer);
//			if (!errorFound) errorFound = localError;
//		}

		// Special cases for select areas. You can't evaluate based on networks
        GeometryHandler selectMerge = selectMergeMap.get(cell);
        for (Layer layer : selectMerge.getKeySet())
//		for(Iterator<Layer> it = selectMerge.getKeyIterator(); it.hasNext(); )
		{
//			Layer layer = it.next();
			errorFound += checkMinAreaLayer(selectMerge, cell, layer, true, null, null);
		}

		return errorFound;
	}

	/**
	 * Method to ensure that polygon "poly" on layer "layer" from object "geom" in
	 * technology "tech" meets minimum area rules. Returns true
	 * if an error is found.
	 */
	private int checkMinArea(Cell cell)
	{
		CheckProto cp = getCheckProto(cell);
		int errorFound = 0;

		// Nothing to check
		if (minAreaLayerMap.isEmpty() && enclosedAreaLayerMap.isEmpty())
			return 0;

		// Select/well regions
		GeometryHandler	selectMerge = GeometryHandler.createGeometryHandler(mergeMode, 0); //new PolyQTree(cell.getBounds());
		HashMap<NodeInst,NodeInst> notExportedNodes = new HashMap<NodeInst,NodeInst>();
		HashMap<NodeInst,NodeInst> checkedNodes = new HashMap<NodeInst,NodeInst>();

        // remember number of errors before the min area checking
        errorFound = errorLogger.getNumErrors();

		// Get merged areas. Only valid for layers that have connections (metals/polys). No valid for NP/PP rule
        QuickAreaEnumerator quickArea = new QuickAreaEnumerator(cp.netlist, selectMerge, notExportedNodes, checkedNodes,
                    mergeMode);
        quickArea.setPreProcessFlag(false);
        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, quickArea);

        // Job aborted
        if (job != null && job.checkAbort()) return 0;

        errorFound = errorLogger.getNumErrors() - errorFound;

		// Checking nodes not exported down in the hierarchy. Probably good enough not to collect networks first
        // Maybe it would be better to check notExportNodes with respect to local cells. No need of checking
        // the entire hierarchy again!
		quickArea = new QuickAreaEnumerator(notExportedNodes, checkedNodes, mergeMode);
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, quickArea);
		// Non exported nodes
//		for(Iterator it = quickArea.mainMerge.getKeyIterator(); it.hasNext(); )
//		{
//			Layer layer = (Layer)it.next();
//			boolean localError = checkMinAreaLayer(quickArea.mainMerge, cell, layer);
//			if (!errorFound) errorFound = localError;
//		}
        selectMerge.postProcess(true);

		// Special cases for select areas. You can't evaluate based on networks
        for (Layer layer : selectMerge.getKeySet())
//		for(Iterator<Layer> it = selectMerge.getKeyIterator(); it.hasNext(); )
		{
//			Layer layer = it.next();
			errorFound +=  checkMinAreaLayer(selectMerge, cell, layer, true, null, null);
		}

		return errorFound;
	}

	/**
	 * Method to find two points between polygons "poly1" and "poly2" that can be used to test
	 * for notches.  The points are returned in (xf1,yf1) and (xf2,yf2).  Returns zero if no
	 * points exist in the gap between the polygons (becuase they don't have common facing edges).
	 * Returns 1 if only one of the reported points needs to be filled (because the polygons meet
	 * at a point).  Returns 2 if both reported points need to be filled.
	 */
	private int findInterveningPoints(Poly poly1, Poly poly2, Point2D pt1, Point2D pt2, boolean newFix)
	{
		Rectangle2D isBox1 = poly1.getBox();
		Rectangle2D isBox2 = poly2.getBox();

		// Better to treat serpentine cases with bouding box
		if (newFix)
		{
			if (isBox1 == null)
				isBox1 = poly1.getBounds2D();
			if (isBox2 == null)
				isBox2 = poly2.getBounds2D();
		}

		if (isBox1 != null && isBox2 != null)
		{
			// handle vertical gap between polygons
			if (isBox1.getMinX() > isBox2.getMaxX() || isBox2.getMinX() > isBox1.getMaxX())
			{
				// see if the polygons are horizontally aligned
				if (isBox1.getMinY() <= isBox2.getMaxY() && isBox2.getMinY() <= isBox1.getMaxY())
				{
					double yf1 = Math.max(isBox1.getMinY(), isBox2.getMinY());
					double yf2 = Math.min(isBox1.getMaxY(), isBox2.getMaxY());
					if (isBox1.getMinX() > isBox2.getMaxX())
					{
						double x = (isBox1.getMinX() + isBox2.getMaxX()) / 2;
						pt1.setLocation(x, yf1);
						pt2.setLocation(x, yf2);
					} else
					{
						double x = (isBox2.getMinX() + isBox1.getMaxX()) / 2;
						pt1.setLocation(x, yf1);
						pt2.setLocation(x, yf2);
					}
					return 2;
				}
			} else if (isBox1.getMinY() > isBox2.getMaxY() || isBox2.getMinY() > isBox1.getMaxY())
			{
				// see if the polygons are horizontally aligned
				if (isBox1.getMinX() <= isBox2.getMaxX() && isBox2.getMinX() <= isBox1.getMaxX())
				{
					double xf1 = Math.max(isBox1.getMinX(), isBox2.getMinX());
					double xf2 = Math.min(isBox1.getMaxX(), isBox2.getMaxX());
					if (isBox1.getMinY() > isBox2.getMaxY())
					{
						double y = (isBox1.getMinY() + isBox2.getMaxY()) / 2;
						pt1.setLocation(xf1, y);
						pt2.setLocation(xf2, y);
					} else
					{
						double y = (isBox2.getMinY() + isBox1.getMaxY()) / 2;
						pt1.setLocation(xf1, y);
						pt2.setLocation(xf2, y);
					}
					return 2;
				}
			} else if ((isBox1.getMinX() == isBox2.getMaxX() || isBox2.getMinX() == isBox1.getMaxX()) || (isBox1.getMinY() == isBox2.getMaxY() || isBox2.getMinY() == isBox2.getMaxY()))
			{
				// handle touching at a point
				double xc = isBox2.getMinX();
				if (isBox1.getMinX() == isBox2.getMaxX()) xc = isBox1.getMinX();
				double yc = isBox2.getMinY();
				if (isBox1.getMinY() == isBox2.getMaxY()) yc = isBox1.getMinY();
				double xPlus = xc + TINYDELTA;
				double yPlus = yc + TINYDELTA;
				pt1.setLocation(xPlus, yPlus);
				pt2.setLocation(xPlus, yPlus);
				if ((xPlus < isBox1.getMinX() || xPlus > isBox1.getMaxX() || yPlus < isBox1.getMinY() || yPlus > isBox1.getMaxY()) &&
					(xPlus < isBox2.getMinX() || xPlus > isBox2.getMaxX() || yPlus < isBox2.getMinY() || yPlus > isBox2.getMaxY())) return 1;
				pt1.setLocation(xc + TINYDELTA, yc - TINYDELTA);
				pt2.setLocation(xc - TINYDELTA, yc + TINYDELTA);
				return 1;
			}

			// handle manhattan objects that are on a diagonal
			if (isBox1.getMinX() > isBox2.getMaxX())
			{
				if (isBox1.getMinY() > isBox2.getMaxY())
				{
					pt1.setLocation(isBox1.getMinX(), isBox1.getMinY());
					pt2.setLocation(isBox2.getMaxX(), isBox2.getMaxY());
					return 1;
				}
				if (isBox2.getMinY() > isBox1.getMaxY())
				{
					pt1.setLocation(isBox1.getMinX(), isBox1.getMaxY());
					pt2.setLocation(isBox2.getMaxX(), isBox2.getMinY());
					return 1;
				}
			}
			if (isBox2.getMinX() > isBox1.getMaxX())
			{
				if (isBox1.getMinY() > isBox2.getMaxY())
				{
					pt1.setLocation(isBox1.getMaxX(), isBox1.getMinY());
					pt2.setLocation(isBox2.getMinX(), isBox2.getMaxY());
					return 1;
				}
				if (isBox2.getMinY() > isBox1.getMaxY())
				{
					pt1.setLocation(isBox1.getMaxX(), isBox1.getMaxY());
					pt2.setLocation(isBox2.getMinX(), isBox2.getMinY());
					return 1;
				}
			}
		}

		// boxes don't line up or this is a nonmanhattan situation
		isBox1 = poly1.getBounds2D();
		isBox2 = poly2.getBounds2D();
		pt1.setLocation((isBox1.getMinX() + isBox1.getMaxX()) / 2, (isBox2.getMinY() + isBox2.getMaxY()) / 2);
		pt2.setLocation((isBox2.getMinX() + isBox2.getMaxX()) / 2, (isBox1.getMinY() + isBox1.getMaxY()) / 2);
		return 1;
	}

	/**
	 * Method to explore the points (xf1,yf1) and (xf2,yf2) to see if there is
	 * geometry on layer "layer" (in or below cell "cell").  Returns true if there is.
	 * If "needBoth" is true, both points must have geometry, otherwise only 1 needs it.
	 */
	private boolean lookForPoints(Point2D pt1, Point2D pt2, Layer layer, Cell cell, boolean needBoth)
	{
		Point2D pt3 = new Point2D.Double((pt1.getX()+pt2.getX()) / 2, (pt1.getY()+pt2.getY()) / 2);

		// compute bounds for searching inside cells
		double flx = Math.min(pt1.getX(), pt2.getX());   double fhx = Math.max(pt1.getX(), pt2.getX());
		double fly = Math.min(pt1.getY(), pt2.getY());   double fhy = Math.max(pt1.getY(), pt2.getY());
		Rectangle2D bounds = new Rectangle2D.Double(flx, fly, fhx-flx, fhy-fly);

		// search the cell for geometry that fills the notch
		boolean [] pointsFound = new boolean[3];
		pointsFound[0] = pointsFound[1] = pointsFound[2] = false;
		boolean allFound = lookForLayer(null, cell, layer, DBMath.MATID, bounds,
			pt1, pt2, pt3, pointsFound);
		if (needBoth)
		{
			if (allFound) return true;
		} else
		{
			if (pointsFound[0] || pointsFound[1]) return true;
		}
		return false;
	}

		/**
	 * Method to explore the points (xf1,yf1) and (xf2,yf2) to see if there is
	 * geometry on layer "layer" (in or below cell "cell").  Returns true if there is.
	 * If "needBoth" is true, both points must have geometry, otherwise only 1 needs it.
	 */
	private boolean lookForCrossPolygons(Geometric geo1, Poly poly1, Geometric geo2, Poly poly2, Layer layer,
	                                     Cell cell, boolean overlap)
	{
		Point2D pt1 = new Point2D.Double();
		Point2D pt2 = new Point2D.Double();
		findInterveningPoints(poly1, poly2, pt1, pt2, true);

		// compute bounds for searching inside cells
		double flx = Math.min(pt1.getX(), pt2.getX());   double fhx = Math.max(pt1.getX(), pt2.getX());
		double fly = Math.min(pt1.getY(), pt2.getY());   double fhy = Math.max(pt1.getY(), pt2.getY());
		Rectangle2D bounds = new Rectangle2D.Double(DBMath.round(flx-TINYDELTA), DBMath.round(fly-TINYDELTA),
                DBMath.round(fhx-flx+2*TINYDELTA), DBMath.round(fhy-fly+2*TINYDELTA));
            // Adding delta otherwise it won't consider points along edges.
        // Mind bounding boxes could have zero width or height

		// search the cell for geometry that fills the notch
		boolean [] pointsFound = new boolean[2];
		pointsFound[0] = pointsFound[1] = false;
		boolean allFound = lookForLayerNew(geo1, poly1, geo2, poly2, cell, layer, DBMath.MATID, bounds,
		        pt1, pt2, null, pointsFound, overlap);

		return allFound;
	}

	/**
	 * Method to examine cell "cell" in the area (lx<=X<=hx, ly<=Y<=hy) for objects
	 * on layer "layer".  Apply transformation "moreTrans" to the objects.  If polygons are
	 * found at (xf1,yf1) or (xf2,yf2) or (xf3,yf3) then sets "p1found/p2found/p3found" to 1.
	 * If all locations are found, returns true.
	 */
	private boolean lookForLayer(Poly thisPoly, Cell cell, Layer layer, AffineTransform moreTrans,
		Rectangle2D bounds, Point2D pt1, Point2D pt2, Point2D pt3, boolean [] pointsFound)
	{
		int j;
        boolean skip = false;
        Rectangle2D newBounds = new Rectangle2D.Double();  // sept 30

		for(Iterator<Geometric> it = cell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric g = it.next();
			if (g instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)g;
				if (NodeInst.isSpecialNode(ni)) continue; // Nov 16, no need for checking pins or other special nodes;
				if (ni.isCellInstance())
				{
					// compute bounding area inside of sub-cell
					AffineTransform rotI = ni.rotateIn();
					AffineTransform transI = ni.translateIn();
					rotI.preConcatenate(transI);
					newBounds.setRect(bounds);
					DBMath.transformRect(newBounds, rotI);

					// compute new matrix for sub-cell examination
					AffineTransform trans = ni.translateOut(ni.rotateOut());
					trans.preConcatenate(moreTrans);
					if (lookForLayer(thisPoly, (Cell)ni.getProto(), layer, trans, newBounds,
						pt1, pt2, pt3, pointsFound))
							return true;
					continue;
				}
				AffineTransform bound = ni.rotateOut();
				bound.preConcatenate(moreTrans);
				Technology tech = ni.getProto().getTechnology();
				Poly [] layerLookPolyList = tech.getShapeOfNode(ni, null, null, false, ignoreCenterCuts, null);
				int tot = layerLookPolyList.length;
				for(int i=0; i<tot; i++)
				{
					Poly poly = layerLookPolyList[i];

					if (!tech.sameLayer(poly.getLayer(), layer)) continue;

					if (thisPoly != null && poly.polySame(thisPoly)) continue;
					poly.transform(bound);
					if (poly.isInside(pt1)) pointsFound[0] = true;
					if (poly.isInside(pt2)) pointsFound[1] = true;
					if (pt3 != null && poly.isInside(pt3)) pointsFound[2] = true;
					for (j = 0; j < pointsFound.length && pointsFound[j]; j++);
					boolean newR = (j == pointsFound.length);
					if (newR)
                    {
						return true;
                    }
                    // No need of checking rest of the layers?
                    //break;
				}
			} else
			{
				ArcInst ai = (ArcInst)g;
				Technology tech = ai.getProto().getTechnology();
				Poly [] layerLookPolyList = tech.getShapeOfArc(ai);
				int tot = layerLookPolyList.length;
				for(int i=0; i<tot; i++)
				{
					Poly poly = layerLookPolyList[i];
					if (!tech.sameLayer(poly.getLayer(), layer)) continue;
					poly.transform(moreTrans);
					if (poly.isInside(pt1)) pointsFound[0] = true;
					if (poly.isInside(pt2)) pointsFound[1] = true;
					if (pt3 != null && poly.isInside(pt3)) pointsFound[2] = true;
					for (j = 0; j < pointsFound.length && pointsFound[j]; j++);
					boolean newR = (j == pointsFound.length);
					if (newR)
						return true;
                    // No need of checking rest of the layers
                    //break;
				}
			}

			for (j = 0; j < pointsFound.length && pointsFound[j]; j++);
            if (j == pointsFound.length)
            {
                System.out.println("When?");
                return true;
            }
		}
        if (skip) System.out.println("This case in lookForLayerNew antes");

		return false;
	}

	/**
	 * Method to examine cell "cell" in the area (lx<=X<=hx, ly<=Y<=hy) for objects
	 * on layer "layer".  Apply transformation "moreTrans" to the objects.  If polygons are
	 * found at (xf1,yf1) or (xf2,yf2) or (xf3,yf3) then sets "p1found/p2found/p3found" to 1.
	 * If all locations are found, returns true.
	 */
	private boolean lookForLayerNew(Geometric geo1, Poly poly1, Geometric geo2, Poly poly2, Cell cell,
	                                Layer layer, AffineTransform moreTrans, Rectangle2D bounds,
	                                Point2D pt1, Point2D pt2, Point2D pt3, boolean[] pointsFound,
                                    boolean overlap)
	{
		int j;
        Rectangle2D newBounds = new Rectangle2D.Double();  // Sept 30

		for(Iterator<Geometric> it = cell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric g = it.next();

            // Skipping the same geometry only when looking for notches in min distance
//			if (ignoreSameGeometry && (g == geo1 || g == geo2)) //not valid condition
//                continue;
			// I can't skip geometries to exclude from the search
			if (g instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)g;
				if (NodeInst.isSpecialNode(ni)) continue; // Nov 16, no need for checking pins or other special nodes;
				if (ni.isCellInstance())
				{
					// compute bounding area inside of sub-cell
					AffineTransform rotI = ni.rotateIn();
					AffineTransform transI = ni.translateIn();
					rotI.preConcatenate(transI);
					newBounds.setRect(bounds);
					DBMath.transformRect(newBounds, rotI);

					// compute new matrix for sub-cell examination
					AffineTransform trans = ni.translateOut(ni.rotateOut());
					trans.preConcatenate(moreTrans);
					if (lookForLayerNew(geo1, poly1, geo2, poly2, (Cell)ni.getProto(), layer, trans, newBounds,
						pt1, pt2, pt3, pointsFound, overlap))
							return true;
					continue;
				}
				AffineTransform bound = ni.rotateOut();
				bound.preConcatenate(moreTrans);
				Technology tech = ni.getProto().getTechnology();
                // I have to ask for electrical layers otherwise it will retrieve one polygon for polysilicon
                // and poly.polySame(poly1) will never be true.
				Poly [] layerLookPolyList = tech.getShapeOfNode(ni, null, null, false, ignoreCenterCuts, null);
				int tot = layerLookPolyList.length;
				for(int i=0; i<tot; i++)
				{
					Poly poly = layerLookPolyList[i];
					if (!tech.sameLayer(poly.getLayer(), layer)) continue;

                    // Should be the transform before?
					poly.transform(bound);

					if (poly1 != null && !overlap && poly.polySame(poly1))
						continue;
					if (poly2 != null && !overlap && poly.polySame(poly2))
						continue;

					if (poly.isInside(pt1)) pointsFound[0] = true; // @TODO Should still evaluate isInside if pointsFound[i] is already valid?
					if (poly.isInside(pt2)) pointsFound[1] = true;
					if (pt3 != null && poly.isInside(pt3)) pointsFound[2] = true;
					for (j = 0; j < pointsFound.length && pointsFound[j]; j++);
					if (j == pointsFound.length) return true;
                    // No need of checking rest of the layers
                    break;
				}
			} else
			{
				ArcInst ai = (ArcInst)g;
				Technology tech = ai.getProto().getTechnology();
				Poly [] layerLookPolyList = tech.getShapeOfArc(ai);
				int tot = layerLookPolyList.length;
				for(int i=0; i<tot; i++)
				{
					Poly poly = layerLookPolyList[i];
					if (!tech.sameLayer(poly.getLayer(), layer)) continue;
					poly.transform(moreTrans);  // @TODO Should still evaluate isInside if pointsFound[i] is already valid?
					if (poly.isInside(pt1)) pointsFound[0] = true;
					if (poly.isInside(pt2)) pointsFound[1] = true;
					if (pt3 != null && poly.isInside(pt3)) pointsFound[2] = true;
					for (j = 0; j < pointsFound.length && pointsFound[j]; j++);
					if (j == pointsFound.length) return true;
                    // No need of checking rest of the layers
                    break;
				}
			}

			for (j = 0; j < pointsFound.length && pointsFound[j]; j++);
			if (j == pointsFound.length)
            {
                System.out.println("When?");
                return true;
            }
		}
		return false;
	}
//    checkCutSizes

    /****************************** Cut Rule Functions ***************************/
    private boolean checkCutSizes(NodeProto np, Geometric geom, Layer layer, Poly poly,
                                  Geometric nGeom, Layer nLayer, Poly nPoly, Cell topCell)
    {
        // None of them is cut
        if (!(np instanceof PrimitiveNode) || layer != nLayer ||
                !layer.getFunction().isContact() || !nLayer.getFunction().isContact())
            return false;

        PrimitiveNode nty = (PrimitiveNode)np;
        double[] specValues = nty.getSpecialValues();
        // 0 and 1 hold CUTSIZE
        if (specValues == null) return false; // no values

        Rectangle2D box1 = poly.getBounds2D();
        Rectangle2D box2 = nPoly.getBounds2D();

        double pdx = Math.max(box2.getMinX()-box1.getMaxX(), box1.getMinX()-box2.getMaxX());
        double pdy = Math.max(box2.getMinY()-box1.getMaxY(), box1.getMinY()-box2.getMaxY());
        double pd = Math.max(pdx, pdy);
        if (pdx == 0 && pdy == 0) pd = 0; // touching

        // They have to overlap
		if (pd > 0) return false;
        boolean foundError = false;
        double minX = Math.min(box1.getMinX(), box2.getMinX());
        double minY = Math.min(box1.getMinY(), box2.getMinY());
        double maxX = Math.max(box1.getMaxX(), box2.getMaxX());
        double maxY = Math.max(box1.getMaxY(), box2.getMaxY());
        Rectangle2D rect = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
        DRCTemplate rule = DRC.getRules(nty.getTechnology()).getRule(layer.getIndex(),
                DRCTemplate.DRCRuleType.MINWID);
//        DRCTemplate rule = DRC.getRules(nty.getTechnology()).getRule(nty.getPrimNodeIndexInTech(),
//                DRCTemplate.DRCRuleType.CUTSIZE);
        String ruleName = (rule != null) ? rule.ruleName : "for contacts";
        if (DBMath.isGreaterThan(rect.getWidth(), specValues[0]))
        {
            reportError(DRCErrorType.CUTERROR, "along X", topCell, specValues[0], rect.getWidth(),
                    ruleName, new Poly(rect), null, layer, null, null, nLayer);
            foundError = true;

        }
        if (DBMath.isGreaterThan(rect.getHeight(), specValues[1]))
        {
            reportError(DRCErrorType.CUTERROR, "along Y", topCell, specValues[1], rect.getHeight(),
                    ruleName, new Poly(rect), null, layer, null, null, layer);
            foundError = true;

        }
        return foundError;
    }

    /****************************** Extension Rule Functions ***************************/
    private boolean checkExtensionGateRule(Geometric geom, Layer layer, Poly poly, Layer nLayer,
                                           Poly nPoly, Netlist netlist)
	{
        return false;
//        if(DRC.isIgnoreExtensionRuleChecking()) return false;
//
//        DRCTemplate extensionRule = DRC.getExtensionRule(layer, nLayer, techMode, true);
//        // Checking extension, it could be slow
//        if (extensionRule == null) return false;
//
//        List list = extensionRule.getNodesInRule();
//        // Layers order is revelant, the first element is the one to check for the extension in the second one
//        if (list != null && list.size() == 2)
//        {
//            // Not the correct order
//            if (list.get(0) != layer || list.get(1) != nLayer) return false;
//        }
//
//        Area firstArea = new Area(poly);
//        Area nPolyArea = new Area(nPoly);
//        Area inter = (Area)nPolyArea.clone();
//        inter.intersect(firstArea);
//        nPolyArea.subtract(inter); // get original nPoly without the intersection
//        boolean found = true; // areas that don't insert are not subject to this test.
//        if (!nPolyArea.isEmpty())
//        {
//            // Searching from the corresponding net on geom
//            Poly portPoly = null;
//            Poly[] primPolyList = getShapeOfGeometric(geom, nLayer);
//            for (int i = 0; i < primPolyList.length; i++)
//            {
//                // Look for Poly that overlaps with original nPoly to get the port
//                if (nPoly.intersects(primPolyList[i]))
//                {
//                    portPoly = primPolyList[i];
//                    break;
//                }
//            }
//            assert portPoly != null;
//            Network net = getDRCNetNumber(netlist, geom, portPoly);
//            found = searchTransistor(net, geom);
//        }
//
//        if (!found)
//            reportError(DRCErrorType.FORBIDDEN, " does not comply with rule '" + extensionRule.ruleName + "'.", geom.getParent(), -1, -1, null, null, geom, null, null, null, null);
//        return false;
    }

    /**
     * Method to retrieve Poly objects from Geometric object (NodeInst, ArcInst)
     * @param geom
     * @param layer
     * @return
     */
    private Poly[] getShapeOfGeometric(Geometric geom, Layer layer)
    {
        Poly [] primPolyList = null;
        List<Layer.Function> drcLayers = new ArrayList<Layer.Function>(1);
        drcLayers.add(layer.getFunction());

        if (geom instanceof NodeInst)
        {
            NodeInst ni = (NodeInst)geom;
            NodeProto np = ni.getProto();
            Technology tech = np.getTechnology();
            primPolyList = tech.getShapeOfNode(ni, null, null, true, ignoreCenterCuts, drcLayers);
        }
        else if (geom instanceof ArcInst)
        {
            ArcInst ai = (ArcInst)geom;
            primPolyList = ai.getProto().getTechnology().getShapeOfArc(ai, null, null, drcLayers);
        }
        return primPolyList;
    }

    /**
     * Method to search for a transistor connected to this network that is not geom. This is to implement
     * special rule in 90nm technology. MOVE TO TECHNOLOGY class!! to be cleaner
     * @param network
     * @param geom
     * @return v
     */
//    private boolean searchTransistor(Network network, Geometric geom)
//    {
//        // checking if any port in that network belongs to a transistor
//        // No need of checking arcs?
//        for (Iterator it = network.getPorts(); it.hasNext();)
//        {
//            PortInst pi = (PortInst)it.next();
//            NodeInst ni = pi.getNodeInst();
//            if (ni != geom && ni.getProto() instanceof PrimitiveNode)
//            {
//                PrimitiveNode pn = (PrimitiveNode)ni.getProto();
//                if (pn.getFunction().isTransistor())
//                    return true; // found a transistor!
//            }
//        }
//        return false;
//    }

    /**
	 * Method to check extension rules in general
	 */
	private boolean checkExtensionRule(Geometric geom, Layer layer, Poly poly, Cell cell)
	{
        return false;
//        if(DRC.isIgnoreExtensionRuleChecking()) return false;
//
//		Rectangle2D polyBnd = poly.getBounds2D();
//        Set layerSet = new HashSet(); // to avoid repeated elements
//
//        // Search among all possible neighbors and to get the layers
//        for(Iterator sIt = cell.searchIterator(polyBnd); sIt.hasNext();)
//        {
//            Geometric g = (Geometric)sIt.next();
//	        if (g == geom) continue;
//			if ((g instanceof ArcInst))
//            {
//                ArcProto ap = ((ArcInst)g).getProto();
//                for (int i = 0; i < ap.getLayers().length; i++)
//                    layerSet.add(pa.getLayers()[i].getLayer());
//            }
//            else
//            {
//                NodeInst ni = (NodeInst)g;
//                NodeProto np = ni.getProto();
//                if (NodeInst.isSkipSizeInPalette(ni)) continue; // Nov 4;
//                if (np instanceof Cell)
//                {
//                    System.out.println("Not implemented in checkExtensionRule");
//                }
//                else
//                {
//                    if (np instanceof PrimitiveNode)
//                    {
//                        PrimitiveNode pn = (PrimitiveNode)np;
//                        for (int i = 0; i < pn.getLayers().length; i++)
//                            layerSet.add(pn.getLayers()[i].getLayer());
//                    }
//                    else
//                        System.out.println("When do we have this case");
//                }
//            }
//        }
//        boolean error = false;
//
//        Object[] layerArray = layerSet.toArray();
//        for (int i = 0; i < layerArray.length; i++)
//        {
//            Layer nLayer = (Layer)layerArray[i];
//            DRCTemplate extensionRule = DRC.getExtensionRule(layer, nLayer, techMode, false);
//            // Checking extension, it could be slow
//            if (extensionRule == null) continue;
//
//            List list = extensionRule.getNodesInRule();
//            // Layers order is revelant, the first element is the one to check for the extension in the second one
//            if (list != null && list.size() == 2)
//            {
//                // Not the correct order
//                if (list.get(0) != layer || list.get(1) != nLayer) continue;
//            }
//
//            Area baseArea = new Area(poly);
//            Area overlapArea = new Area(); // start empty
//            Area extensionArea = new Area(); // start empty
//
//            checkExtensionOverlapRule(geom, nLayer, cell, baseArea, extensionArea, overlapArea, polyBnd);
//
//            // get the area outside nLayer
//            if (!extensionArea.isEmpty())
//            {
//                List polyList = PolyBase.getPointsInArea(extensionArea, layer, true, true, null);
//
//                for (Iterator it = polyList.iterator(); it.hasNext(); )
//                {
//                    PolyBase nPoly = (PolyBase)it.next();
//                    Rectangle2D rect = nPoly.getBounds2D();
//                    int dir = 0; // X  assume the intersection edge is Y oriented
//                    // Determine how they touch. This is to get the perpendicular direction to the common edge.
//                    if ((rect.getMaxY() == poly.getBounds2D().getMinY()) ||
//                        (rect.getMinY() == poly.getBounds2D().getMaxY()) )
//                        dir = 1; // Y
//
//                    // not easy to determine along which axis they are touching
//                    if ((dir == 1 && rect.getHeight() < extensionRule.value) || (dir == 0 && rect.getWidth() < extensionRule.value))
//                    {
//                        reportError(LAYERDRCErrorType.SURROUNDERROR, "No enough extension, ", cell, extensionRule.value, -1, extensionRule.ruleName,
//                                nPoly, geom, layer, null, null, nLayer);
//                        error = true;
//                    }
//                }
//            }
//
//            // There is overlap
//            if (!overlapArea.isEmpty())
//            {
//                List polyList = PolyBase.getPointsInArea(overlapArea, layer, true, true, null);
//
//                for (Iterator it = polyList.iterator(); it.hasNext(); )
//                {
//                    PolyBase nPoly = (PolyBase)it.next();
//                    Rectangle2D rect = nPoly.getBounds2D();
//                    // not easy to determine along which axis they are touching
//                    if (rect.getHeight() < extensionRule.value || rect.getWidth() < extensionRule.value)
//                    {
//                        reportError(LAYERDRCErrorType.SURROUNDERROR, "No enough overlap, ", cell, extensionRule.value, -1, extensionRule.ruleName,
//                                nPoly, geom, layer, null, null, nLayer);
//                        error = true;
//                    }
//                }
//            }
//        }
//		return (error);
	}

    /**
     *
     * @param geom
     * @param cell
     * @param extensionArea contains information about poly area outside of nLayer
     * @param overlapArea
     * @param polyBnd
     */
    private void checkExtensionOverlapRule(Geometric geom, Layer layer, Cell cell, Area baseArea,
                                              Area extensionArea, Area overlapArea,
                                              Rectangle2D polyBnd)
    {
        for(Iterator<Geometric> sIt = cell.searchIterator(polyBnd); sIt.hasNext(); )
		{
			Geometric g = sIt.next();
	        if (g == geom) continue;
			if ((g instanceof NodeInst))
            {
                NodeInst ni = (NodeInst)g;
                NodeProto np = ni.getProto();
                if (NodeInst.isSpecialNode(ni)) continue; // Nov 4;
                if (ni.isCellInstance())
                {
                    AffineTransform cellDownTrans = ni.transformIn();
                    AffineTransform	cellUpTrans = ni.transformOut();
                    DBMath.transformRect(polyBnd, cellDownTrans);
                    extensionArea.transform(cellDownTrans);
                    overlapArea.transform(cellDownTrans);
                    checkExtensionOverlapRule(geom, layer, (Cell)np, baseArea, extensionArea, overlapArea, polyBnd);
                    DBMath.transformRect(polyBnd, cellUpTrans);
                    extensionArea.transform(cellUpTrans);
                    overlapArea.transform(cellUpTrans);
                    continue;
                }
            }

            Poly [] primPolyList = getShapeOfGeometric(g, layer);
            int tot = primPolyList.length;
            for(int j=0; j<tot; j++)
            {
                Poly nPoly = primPolyList[j];

                // Checking if are covered by select is surrounded by minOverlapRule
                Area nPolyArea = new Area(nPoly);
                Area interPolyArea = (Area)nPolyArea.clone();
                interPolyArea.intersect(baseArea);
                overlapArea.add(interPolyArea);
                Area extenPolyArea = (Area)nPolyArea.clone();
                extenPolyArea.subtract(interPolyArea);
                extensionArea.add(extenPolyArea);
            }
		}
    }

    /****************************** Select Over Polysilicon Functions ***************************/
	/**
	 * Method to check if non-transistor polysilicons are completed covered by N++/P++ regions
	 */
	private boolean checkSelectOverPolysilicon(Geometric geom, Layer layer, Poly poly, Cell cell)
	{
        if(DRC.isIgnoreExtensionRuleChecking()) return false;

		if (!layer.getFunction().isPoly()) return false;
		// One layer must be select and other polysilicon. They are not connected

		DRCTemplate minOverlapRule = DRC.getMinValue(layer, DRCTemplate.DRCRuleType.EXTENSION);
		if (minOverlapRule == null) return false;

		Rectangle2D polyBnd = poly.getBounds2D();
        Area polyArea = new Area(poly);
        boolean found = polyArea.isEmpty();
		List<Point2D> checkExtraPoints = new ArrayList<Point2D>();

        found = checkThisCellExtensionRule(geom, layer, poly, null, cell, polyArea, polyBnd, minOverlapRule,
                found, checkExtraPoints);

		// error if the merged area doesn't contain 100% the search area.
		if (!found)
		{
            List<PolyBase> polyList = PolyBase.getPointsInArea(polyArea, layer, true, true, null);

//            for (Iterator it = polyList.iterator(); it.hasNext(); )
            for (PolyBase nPoly : polyList)
            {
//                PolyBase nPoly = (PolyBase)it.next();
                reportError(DRCErrorType.LAYERSURROUNDERROR, "Polysilicon not covered, ", cell, minOverlapRule.value1, -1, minOverlapRule.ruleName,
                            nPoly, geom, layer, null, null, null);
            }
		}
		else
		{
			// Checking if not enough coverage
			Point2D[] extraPoints = new Point2D[checkExtraPoints.size()];
			checkExtraPoints.toArray(extraPoints);
			boolean[] founds = new boolean[checkExtraPoints.size()];
			Arrays.fill(founds, false);
			Rectangle2D ruleBnd = new Rectangle2D.Double(polyBnd.getMinX()-minOverlapRule.value1,
								polyBnd.getMinY()-minOverlapRule.value1,
								polyBnd.getWidth() + minOverlapRule.value1*2,
								polyBnd.getHeight() + minOverlapRule.value1*2);
			boolean foundAll = allPointsContainedInLayer(geom, cell, ruleBnd, null, extraPoints, founds);

			if (!foundAll)
				reportError(DRCErrorType.LAYERSURROUNDERROR, "No enough surround, ", geom.getParent(), minOverlapRule.value1, -1, minOverlapRule.ruleName,
									 poly, geom, layer, null, null, null);
		}
		return (!found);
	}

    private boolean checkThisCellExtensionRule(Geometric geom, Layer layer, Poly poly, List<Layer.Function> drcLayers, Cell cell, Area polyArea,
                                               Rectangle2D polyBnd, DRCTemplate minOverlapRule, boolean found,
                                               List<Point2D> checkExtraPoints)
    {
        boolean[] founds = new boolean[4];

        for(Iterator<Geometric> sIt = cell.searchIterator(polyBnd); !found && sIt.hasNext(); )
		{
			Geometric g = sIt.next();
	        if (g == geom) continue;
			if (!(g instanceof NodeInst))
            {
                if (Job.LOCALDEBUGFLAG && !DRC.isIgnoreExtensionRuleChecking())
                    System.out.println("Skipping arcs!");
                continue; // Skipping arcs!!!!
            }
			NodeInst ni = (NodeInst)g;
			NodeProto np = ni.getProto();
			if (NodeInst.isSpecialNode(ni)) continue; // Nov 4;
			if (ni.isCellInstance())
			{
				AffineTransform cellDownTrans = ni.transformIn();
				AffineTransform	cellUpTrans = ni.transformOut();
				DBMath.transformRect(polyBnd, cellDownTrans);
				polyArea.transform(cellDownTrans);
				poly.transform(cellDownTrans);
				List<Point2D> newExtraPoints = new ArrayList<Point2D>();
				found = checkThisCellExtensionRule(geom, layer, poly, drcLayers, (Cell)np, polyArea, polyBnd, minOverlapRule,
				        found, newExtraPoints);
                for (Point2D point : newExtraPoints)
//				for (Iterator<Point2D> it = newExtraPoints.iterator(); it.hasNext();)
				{
//					Point2D point = it.next();
					cellUpTrans.transform(point, point);
					checkExtraPoints.add(point);
				}
				DBMath.transformRect(polyBnd, cellUpTrans);
				polyArea.transform(cellUpTrans);
				poly.transform(cellUpTrans);
			}
			else
			{
				Technology tech = np.getTechnology();
				Poly [] primPolyList = tech.getShapeOfNode(ni, null, null, true, ignoreCenterCuts, drcLayers);
				int tot = primPolyList.length;
				for(int j=0; j<tot; j++)
				{
					Poly nPoly = primPolyList[j];
                    if (drcLayers == null)
                        if (!nPoly.getLayer().getFunction().isImplant()) continue;

                    // Checking if are covered by select is surrounded by minOverlapRule
                    Area distPolyArea = (Area)polyArea.clone();
                    Area nPolyArea = new Area(nPoly);
                    polyArea.subtract(nPolyArea);

                    distPolyArea.subtract(polyArea);
					if (distPolyArea.isEmpty())
						continue;  // no intersection
                    Rectangle2D interRect = distPolyArea.getBounds2D();
                    Rectangle2D ruleBnd = new Rectangle2D.Double(interRect.getMinX()-minOverlapRule.value1,
                            interRect.getMinY()-minOverlapRule.value1,
                            interRect.getWidth() + minOverlapRule.value1*2,
                            interRect.getHeight() + minOverlapRule.value1*2);
                    PolyBase extPoly = new PolyBase(ruleBnd);
					PolyBase distPoly = new PolyBase(interRect);
                    Arrays.fill(founds, false);
					// Removing points on original polygon. No very efficient though
					Point2D[] points = extPoly.getPoints();
					Point2D[] distPoints = distPoly.getPoints();
					// Only valid for 4-point polygons!!
					if (distPoints.length != points.length)
						System.out.println("This case is not valid in Quick.checkThisCellExtensionRule");
					for (int i = 0; i < points.length; i++)
					{
						// Check if point is corner
						found = poly.isPointOnCorner(distPoints[i]);
						// Point along edge
						if (!found)
							founds[i] = true;
					}

                    boolean foundAll = allPointsContainedInLayer(geom, cell, ruleBnd, drcLayers, points, founds);
                    if (!foundAll)
                    {
	                    // Points that should be checked from geom parent
	                    for (int i = 0; i < founds.length; i++)
	                    {
		                    if (!founds[i]) checkExtraPoints.add(points[i]);
	                    }
                    }
				}
			}
			found = polyArea.isEmpty();
		}
        return (found);
    }

    /**
     * Method to check if certain poly rectangle is fully covered by any select regoin
     */
    private boolean allPointsContainedInLayer(Geometric geom, Cell cell, Rectangle2D ruleBnd, List<Layer.Function> drcLayers, Point2D[] points, boolean[] founds)
    {
        for(Iterator<Geometric> sIt = cell.searchIterator(ruleBnd); sIt.hasNext(); )
        {
            Geometric g = sIt.next();
	        if (g == geom) continue;
            if (!(g instanceof NodeInst)) continue;
            NodeInst ni = (NodeInst)g;
            NodeProto np = ni.getProto();
	        if (NodeInst.isSpecialNode(ni)) continue; // Nov 4;
            if (ni.isCellInstance())
            {
				AffineTransform cellDownTrans = ni.transformIn();
				AffineTransform	cellUpTrans = ni.transformOut();
				DBMath.transformRect(ruleBnd, cellDownTrans);
	            cellDownTrans.transform(points, 0, points, 0, points.length);
                boolean allFound = allPointsContainedInLayer(geom, (Cell)np, ruleBnd, drcLayers, points, founds);
				DBMath.transformRect(ruleBnd, cellUpTrans);
	            cellUpTrans.transform(points, 0, points, 0, points.length);
	            if (allFound)
	                return true;
            }
            else
            {
                Technology tech = np.getTechnology();
                Poly [] primPolyList = tech.getShapeOfNode(ni, null, null, true, ignoreCenterCuts, drcLayers);
                int tot = primPolyList.length;
                for(int j=0; j<tot; j++)
                {
                    Poly nPoly = primPolyList[j];
                    if (!nPoly.getLayer().getFunction().isImplant()) continue;
                    boolean allFound = true;
	                // No need of looping if one of them is already out
                    for (int i = 0; i < points.length; i++)
                    {
                        if (!founds[i]) founds[i] = nPoly.contains(points[i]);
	                    if (!founds[i]) allFound = false;
                    }
                    if (allFound) return true;
                }
            }
        }

        return (false);
    }
    /***************************END of Select Over Polysilicn Functions ************************************/

	/**
	 * Method to see if the two boxes are active elements, connected to opposite
	 * sides of a field-effect transistor that resides inside of the box area.
	 * Returns true if so.
	 */
	private boolean activeOnTransistor(Poly poly1, Layer layer1, int net1,
	                                   Poly poly2, Layer layer2, int net2, Cell cell, int globalIndex)
	{
		// networks must be different
		if (net1 == net2) return false;

		// layers must be active or active contact
		Layer.Function fun = layer1.getFunction();
		int funExtras = layer1.getFunctionExtras();

		if (!fun.isDiff())
		{
			if (!fun.isContact() || (funExtras&Layer.Function.CONDIFF) == 0) return false;
		}

		funExtras = layer2.getFunctionExtras();
		fun = layer2.getFunction();
		if (!fun.isDiff())
		{
			if (!fun.isContact() || (funExtras&Layer.Function.CONDIFF) == 0) return false;
		}

		// search for intervening transistor in the cell
		Rectangle2D bounds1 = poly1.getBounds2D();
		Rectangle2D bounds2 = poly2.getBounds2D();
		Rectangle2D.union(bounds1, bounds2, bounds1);
		return activeOnTransistorRecurse(bounds1, net1, net2, cell, globalIndex, DBMath.MATID);
	}

	private boolean activeOnTransistorRecurse(Rectangle2D bounds,
		int net1, int net2, Cell cell, int globalIndex, AffineTransform trans)
	{
		Netlist netlist = getCheckProto(cell).netlist;
		Rectangle2D subBounds = new Rectangle2D.Double();
		for(Iterator<Geometric> sIt = cell.searchIterator(bounds); sIt.hasNext(); )
		{
			Geometric g = sIt.next();
			if (!(g instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)g;
			NodeProto np = ni.getProto();
			if (ni.isCellInstance())
			{
				AffineTransform rTransI = ni.rotateIn();
				AffineTransform tTransI = ni.translateIn();
				rTransI.preConcatenate(tTransI);
				subBounds.setRect(bounds);
				DBMath.transformRect(subBounds, rTransI);

				CheckInst ci = checkInsts.get(ni);
				int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;

				boolean ret = activeOnTransistorRecurse(subBounds,
					net1, net2, (Cell)np, localIndex, trans);
				if (ret) return true;
				continue;
			}

			// must be a transistor
			if (!ni.isFET()) continue;

			// must be inside of the bounding box of the desired layers
			Rectangle2D nodeBounds = ni.getBounds();
			double cX = nodeBounds.getCenterX();
			double cY = nodeBounds.getCenterY();

			if (cX < bounds.getMinX() || cX > bounds.getMaxX() ||
				cY < bounds.getMinY() || cY > bounds.getMaxY()) continue;

			// determine the poly port  (MUST BE BETTER WAY!!!!)
			PortProto badport = np.getPort(0);
			Network badNet = netlist.getNetwork(ni, badport, 0);

			boolean on1 = false, on2 = false;
			for(Iterator it = ni.getPortInsts(); it.hasNext(); )
			{
				PortInst pi = (PortInst)it.next();
				PortProto po = pi.getPortProto();
				String name = po.getName();
				boolean found = false;
                boolean oldFound = false;
				Network piNet = netlist.getNetwork(pi);

				// ignore connections on poly/gate
				if (name.indexOf("poly") != -1)
				{
					found = true;
					//continue;
				}

				if (piNet == badNet)
					oldFound = true;
				if (Job.LOCALDEBUGFLAG && oldFound != found)
						System.out.println("Here is different in activeOnTransistorRecurse");
                if (found)
                    continue;
				Integer [] netNumbers = networkLists.get(piNet);
				int net = netNumbers[globalIndex].intValue();
				if (net < 0) continue;
				if (net == net1) on1 = true;
				if (net == net2) on2 = true;
			}

			// if either side is not connected, ignore this
			if (!on1 || !on2) continue;

			// transistor found that connects to both actives
			return true;
		}
		return false;
	}

	/**
	 * Method to crop the box on layer "nLayer", electrical index "nNet"
	 * and bounds (lx-hx, ly-hy) against the nodeinst "ni".  The geometry in nodeinst "ni"
	 * that is being checked runs from (nlx-nhx, nly-nhy).  Only those layers
	 * in the nodeinst that are the same layer and the same electrical network
	 * are checked.  Returns true if the bounds are reduced
	 * to nothing.
	 */
	private boolean cropNodeInst(NodeInst ni, int globalIndex, AffineTransform trans,
	                             Layer nLayer, int nNet, Geometric nGeom, Rectangle2D bound)
	{
		Technology tech = ni.getProto().getTechnology();
		Poly [] cropNodePolyList = tech.getShapeOfNode(ni, null, null, true, ignoreCenterCuts, null);
		convertPseudoLayers(ni, cropNodePolyList);
		int tot = cropNodePolyList.length;
		if (tot < 0) return false;
        // Change #1
//		for(int j=0; j<tot; j++)
//			cropNodePolyList[j].transform(trans);
        boolean [] rotated = new boolean[tot];
        Arrays.fill(rotated, false);
		boolean isConnected = false;
		Netlist netlist = getCheckProto(ni.getParent()).netlist;
		for(int j=0; j<tot; j++)
		{
			Poly poly = cropNodePolyList[j];
			if (!tech.sameLayer(poly.getLayer(), nLayer)) continue;
            //only transform when poly is valid
            poly.transform(trans); // change 1
            rotated[j] = true;
			if (nNet >= 0)
			{
				if (poly.getPort() == null) continue;

				// determine network for this polygon
				int net = getDRCNetNumber(netlist, poly.getPort(), ni, globalIndex);
				if (net >= 0 && net != nNet) continue;
			}
			isConnected = true;
			break;
		}
		if (!isConnected) return false;

		// get the description of the nodeinst layers
		boolean allgone = false;
        for(int j=0; j<tot; j++)
        {
            Poly poly = cropNodePolyList[j];
            if (!tech.sameLayer(poly.getLayer(), nLayer)) continue;

            if (!rotated[j]) poly.transform(trans); // change 1

            // warning: does not handle arbitrary polygons, only boxes
            Rectangle2D polyBox = poly.getBox();
            if (polyBox == null) continue;
            int temp = Poly.cropBox(bound, polyBox);
            if (temp > 0) { allgone = true;   break; }
            if (temp < 0)
            {
                tinyNodeInst = ni;
                tinyGeometric = nGeom;
            }
        }
		return allgone;
	}

	/**
	 * Method to crop away any part of layer "lay" of arcinst "ai" that coincides
	 * with a similar layer on a connecting nodeinst.  The bounds of the arcinst
	 * are in the reference parameters (lx-hx, ly-hy).  Returns false
	 * normally, 1 if the arcinst is cropped into oblivion.
	 */
	private boolean cropArcInst(ArcInst ai, Layer lay, AffineTransform inTrans, Rectangle2D bounds, boolean overlap)
	{
		for(int i=0; i<2; i++)
		{
			// find the primitive nodeinst at the true end of the portinst
			PortInst pi = ai.getPortInst(i);

			PortOriginal fp = new PortOriginal(pi, inTrans);
			NodeInst ni = fp.getBottomNodeInst();
			NodeProto np = ni.getProto();
			AffineTransform trans = fp.getTransformToTop();

			Technology tech = np.getTechnology();
            Poly [] cropArcPolyList = null;
            // No overlap means arc should be cropped to get away from other element
//            if (!overlap && np.getFunction() == PrimitiveNode.Function.PIN)
//            {
//                // Pins don't generate polygons
//                // Search for another arc and try to crop it with that geometry
//                List<Layer.Function> drcLayers = new ArrayList<Layer.Function>(1);
//                drcLayers.add(lay.getFunction());
//                List<Poly[]> arcPolys = new ArrayList<Poly[]>(1);
//                int totalPolys = 0;
//                for (Iterator it = ni.getConnections(); it.hasNext(); )
//                {
//                    Connection con = (Connection)it.next();
//                    ArcInst arc = con.getArc();
//                    if (arc == ai) continue;
//                    Poly[] polys = tech.getShapeOfArc(arc, null, null, drcLayers);
//                    arcPolys.add(polys);
//                    totalPolys += polys.length;
//                }
//                cropArcPolyList = new Poly[totalPolys];
//                int destPos = 0;
//                for (Poly[] arcs : arcPolys)
////                for (int j = 0; j < arcPolys.size(); j++)
//                {
////                    Poly[] arcs = arcPolys.get(j);
//                    System.arraycopy(arcs, 0, cropArcPolyList, destPos, arcs.length);
//                    destPos += arcs.length;
//                }
//            }
//            else
                cropArcPolyList = tech.getShapeOfNode(ni, null, null, false, ignoreCenterCuts, null);
			int tot = cropArcPolyList.length;
			for(int j=0; j<tot; j++)
			{
				Poly poly = cropArcPolyList[j];
				if (!tech.sameLayer(poly.getLayer(), lay)) continue;
				poly.transform(trans);

				// warning: does not handle arbitrary polygons, only boxes
				Rectangle2D polyBox = poly.getBox();
				if (polyBox == null) continue;
				int temp = Poly.halfCropBox(bounds, polyBox);
				if (temp > 0) return true;
				if (temp < 0)
				{
					tinyNodeInst = ni;
					tinyGeometric = ai;
				}
			}
		}
		return false;
	}

	/**
	 * Method to see if polygons in "pList" (describing arc "ai") should be cropped against a
	 * connecting transistor.  Crops the polygon if so.
	 */
	private void cropActiveArc(ArcInst ai, Poly [] pList)
	{
		// look for an active layer in this arc
		int tot = pList.length;
		int diffPoly = -1;
		for(int j=0; j<tot; j++)
		{
			Poly poly = pList[j];
			Layer layer = poly.getLayer();
			if (layer == null) continue;
			Layer.Function fun = layer.getFunction();
			if (fun.isDiff()) { diffPoly = j;   break; }
		}
		if (diffPoly < 0) return;
		Poly poly = pList[diffPoly];

		// must be manhattan
		Rectangle2D polyBounds = poly.getBox();
		if (polyBounds == null) return;
		polyBounds = new Rectangle2D.Double(polyBounds.getMinX(), polyBounds.getMinY(), polyBounds.getWidth(), polyBounds.getHeight());

		// search for adjoining transistor in the cell
		boolean cropped = false;
		boolean halved = false;
		for(int i=0; i<2; i++)
		{
			PortInst pi = ai.getPortInst(i);
			NodeInst ni = pi.getNodeInst();
			if (!ni.isFET()) continue;

			// crop the arc against this transistor
			AffineTransform trans = ni.rotateOut();
			Technology tech = ni.getProto().getTechnology();
			Poly [] activeCropPolyList = tech.getShapeOfNode(ni, null, null, false, ignoreCenterCuts, null);
			int nTot = activeCropPolyList.length;
			for(int k=0; k<nTot; k++)
			{
				Poly nPoly = activeCropPolyList[k];
				if (nPoly.getLayer() != poly.getLayer()) continue;
				nPoly.transform(trans);
				Rectangle2D nPolyBounds = nPoly.getBox();
				if (nPolyBounds == null) continue;
				// @TODO Why only one half is half crop?
				// Should I change cropBox by cropBoxComplete?
				int result = (halved) ?
						Poly.cropBox(polyBounds, nPolyBounds) :
				        Poly.halfCropBox(polyBounds, nPolyBounds);

				if (result == 1)
				{
					// remove this polygon from consideration
					poly.setLayer(null);
					return;
				}
				cropped = true;
				halved = true;
			}
		}
		if (cropped)
		{
			Poly.Type style = poly.getStyle();
			Layer layer = poly.getLayer();
			poly = new Poly(polyBounds);
			poly.setStyle(style);
			poly.setLayer(layer);
			pList[diffPoly] = poly;
		}
	}

	/**
	 * Method to convert all Layer information in the Polys to be non-pseudo.
	 * This is only done for pins that have exports but no arcs.
	 * @param ni the NodeInst being converted.
	 * @param pList an array of Polys with Layer information for the NodeInst.
	 */
	private void convertPseudoLayers(NodeInst ni, Poly [] pList)
	{
		if (ni.getProto().getFunction() != PrimitiveNode.Function.PIN) return;
		if (ni.hasConnections()) return;
//		if (ni.getNumConnections() != 0) return;
		if (!ni.hasExports()) return;
//		if (ni.getNumExports() == 0) return;

		// for pins that are unconnected but have exports, convert them to real layers
		int tot = pList.length;
		for(int i=0; i<tot; i++)
		{
			Poly poly = pList[i];
			Layer layer = poly.getLayer();
			if (layer != null)
				poly.setLayer(layer.getNonPseudoLayer());
		}
	}

	/**
	 * Method to determine which layers in a Technology are valid.
	 */
	private void cacheValidLayers(Technology tech)
	{
		if (tech == null) return;
		if (layersValidTech == tech) return;

		layersValidTech = tech;

		// determine the layers that are being used
		int numLayers = tech.getNumLayers();
		layersValid = new boolean[numLayers];
		for(int i=0; i < numLayers; i++)
			layersValid[i] = false;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			if (np.isNotUsed()) continue;
			Technology.NodeLayer [] layers = np.getLayers();
			for(int i=0; i<layers.length; i++)
			{
				Layer layer = layers[i].getLayer();
				layersValid[layer.getIndex()] = true;
			}
		}
		for(Iterator it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = (ArcProto)it.next();
			if (ap.isNotUsed()) continue;
			Technology.ArcLayer [] layers = ap.getLayers();
			for(int i=0; i<layers.length; i++)
			{
				Layer layer = layers[i].getLayer();
				layersValid[layer.getIndex()] = true;
			}
		}
	}

	/**
	 * Method to determine the minimum distance between "layer1" and "layer2" in technology
	 * "tech" and library "lib".  If "con" is true, the layers are connected.  Also forces
	 * connectivity for same-implant layers.
	 */
	private DRCTemplate getSpacingRule(Layer layer1, Poly poly1, Geometric geo1,
                                       Layer layer2, Poly poly2, Geometric geo2,
                                       boolean con, int multi)
	{
		// if they are implant on the same layer, they connect
		if (!con && layer1 == layer2)
		{
			Layer.Function fun = layer1.getFunction();
			// treat all wells as connected
            con = fun.isSubstrate();
		}

        double[] values = layer1.getTechnology().getSpacingDistances(poly1, poly2);

		return (DRC.getSpacingRule(layer1, geo1, layer2, geo2, con, multi, values[0], values[1]));
	}

    /**
     * Method to retrieve network from a Geometric object (NodeInst, ArcInst)
     * @param netlist
     * @param geom
     * @param poly
     * @return
     */
//    private Network getDRCNetNumber(Netlist netlist, Geometric geom, Poly poly)
//    {
//        Network jNet = null;
//        if (geom instanceof ArcInst)
//        {
//            ArcInst ai = (ArcInst)geom;
//            jNet = netlist.getNetwork(ai, 0);
//        } else if (geom instanceof NodeInst)
//            jNet = netlist.getNetwork((NodeInst)geom, poly.getPort(), 0);
//        return jNet;
//    }

	/**
	 * Method to return the network number for port "pp" on node "ni", given that the node is
	 * in a cell with global index "globalIndex".
	 */
	private int getDRCNetNumber(Netlist netlist, PortProto pp, NodeInst ni, int globalIndex)
	{
		if (pp == null) return -1;

       //@TODO REPLACE BY getNetwork(Nodable no, PortProto portProto, int busIndex)
        // or getNetIndex(Nodable no, PortProto portProto, int busIndex)

		// see if there is an arc connected
		Network net = netlist.getNetwork(ni, pp, 0);
		Integer [] nets =networkLists.get(net);
		if (nets == null) return -1;
		return nets[globalIndex].intValue();
	}

	/***************** LAYER INTERACTIONS ******************/

	/**
	 * Method to build the internal data structures that tell which layers interact with
	 * which primitive nodes in technology "tech".
	 */
	private void buildLayerInteractions(Technology tech)
	{
		Technology old = layerInterTech;
		if (layerInterTech == tech) return;

		layerInterTech = tech;
		int numLayers = tech.getNumLayers();

		// build the node table
        if (layersInterNodes != null && old != null && job != null)
        {
	        errorLogger.logWarning("Switching from '" + old.getTechName() +
	                "' to '" +  tech.getTechName() + "' in DRC process. Check for non desired nodes in ",
	                job.cell, -1);
        }

		layersInterNodes = new HashMap<PrimitiveNode, boolean[]>();
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
            if (np.isNotUsed()) continue;
			boolean [] layersInNode = new boolean[numLayers];
            Arrays.fill(layersInNode, false);

			Technology.NodeLayer [] layers = np.getLayers();
			Technology.NodeLayer [] eLayers = np.getElectricalLayers();
			if (eLayers != null) layers = eLayers;
			for(Technology.NodeLayer l : layers)
			{
				Layer layer = l.getLayer();
                if (layer.isNonElectrical())
                    continue; // such as pseudo
                for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
				{
					Layer oLayer = lIt.next();
                    if (oLayer.isNonElectrical())
                        continue; // such as pseudo
                    if (DRC.isAnySpacingRule(layer, oLayer))
                        layersInNode[oLayer.getIndex()] = true;
				}
			}
			layersInterNodes.put(np, layersInNode);
		}

		// build the arc table
		layersInterArcs = new HashMap<ArcProto, boolean[]>();
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			boolean [] layersInArc = new boolean[numLayers];
            Arrays.fill(layersInArc, false);

			Technology.ArcLayer [] layers = ap.getLayers();
			for(Technology.ArcLayer l : layers)
			{
				Layer layer = l.getLayer();
				for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
				{
					Layer oLayer = lIt.next();
					if (DRC.isAnySpacingRule(layer, oLayer))
						layersInArc[oLayer.getIndex()] = true;
				}
			}
			layersInterArcs.put(ap, layersInArc);
		}
	}

	/**
	 * Method to determine whether layer "layer" interacts in any way with a node of type "np".
	 * If not, returns FALSE.
	 */
	private boolean checkLayerWithNode(Layer layer, NodeProto np)
	{
		buildLayerInteractions(np.getTechnology());

		// find this node in the table
		boolean [] validLayers = layersInterNodes.get(np);
		if (validLayers == null) return false;
		return validLayers[layer.getIndex()];
	}

	/**
	 * Method to determine whether layer "layer" interacts in any way with an arc of type "ap".
	 * If not, returns FALSE.
	 */
	private boolean checkLayerWithArc(Layer layer, ArcProto ap)
	{
		buildLayerInteractions(ap.getTechnology());

		// find this node in the table
		boolean [] validLayers = layersInterArcs.get(ap);
		if (validLayers == null) return false;
		return validLayers[layer.getIndex()];
	}

	/**
	 * Method to recursively scan cell "cell" (transformed with "trans") searching
	 * for DRC Exclusion nodes.  Each node is added to the global list "exclusionList".
	 */
	private void accumulateExclusion(Cell cell)
	{
        Area area = null;

		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			if (np == Generic.tech.drcNode)
			{
                // Must get polygon from getNodeShape otherwise it will miss
                // rings
                Poly [] list = cell.getTechnology().getShapeOfNode(ni, null, null, true, true, null);
                Area thisArea = new Area(list[0]);
                if (area == null)
                    area = thisArea;
                else
                    area.add(thisArea);
				continue;
			}

			if (ni.isCellInstance())
			{
				// examine contents
				accumulateExclusion((Cell)np);
			}
		};
        exclusionMap.put(cell, area);
	}

	/*********************************** QUICK DRC ERROR REPORTING ***********************************/
    private boolean checkExclusionMap(Cell cell, List<PolyBase> polyList, List<Geometric> geomList, StringBuffer DRCexclusionMsg)
    {
        Area area = exclusionMap.get(cell);
        if (area == null) return false;

        int count = 0, i = -1;

        for (PolyBase thisPoly : polyList)
        {
            i++;
            if (thisPoly == null)
                continue; // MinNode case
            boolean found = area.contains(thisPoly.getBounds2D());

            if (found) count++;
            else DRCexclusionMsg.append("\n\t(DRC Exclusion in '" + cell.getName() + "' does not completely contain " +
                        geomList.get(i) + ")");
        }
        // At least one DRC exclusion that contains both
//        if (count == polyList.size())
        if (count >= 1) // at one element is inside the DRC exclusion
            return true;
        return false;
    }

	/* Adds details about an error to the error list */
	private void reportError(DRCErrorType errorType, String msg,
	                         Cell cell, double limit, double actual, String rule,
	                         PolyBase poly1, Geometric geom1, Layer layer1,
	                         PolyBase poly2, Geometric geom2, Layer layer2)
	{
		if (errorLogger == null) return;

		// if this error is in an ignored area, don't record it
		StringBuffer DRCexclusionMsg = new StringBuffer();
        if (exclusionMap.get(cell) != null)
		{
			// determine the bounding box of the error
			List<PolyBase> polyList = new ArrayList<PolyBase>(2);
			List<Geometric> geomList = new ArrayList<Geometric>(2);
			polyList.add(poly1); geomList.add(geom1);
			if (poly2 != null)
			{
				polyList.add(poly2);
				geomList.add(geom2);
			}
            boolean found = checkExclusionMap(cell, polyList, geomList, DRCexclusionMsg);

            // At least one DRC exclusion that contains both
            if (found) return;
		}

		// describe the error
		Cell np1 = (geom1 != null) ? geom1.getParent() : null;
		Cell np2 = (geom2 != null) ? geom2.getParent() : null;

		// Message already logged
        boolean onlyWarning = (errorType == DRCErrorType.ZEROLENGTHARCWARN || errorType == DRCErrorType.TECHMIXWARN);
        // Until a decent algorithm is in place for detecting repeated errors, ERROR_CHECK_EXHAUSTIVE might report duplicate errros
		if ( geom2 != null && errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE && errorLogger.findMessage(cell, geom1, geom2.getParent(), geom2, !onlyWarning))
            return;

		StringBuffer errorMessage = new StringBuffer();
		int sortLayer = cell.hashCode(); // 0;
		if (errorType == DRCErrorType.SPACINGERROR || errorType == DRCErrorType.NOTCHERROR || errorType == DRCErrorType.SURROUNDERROR)
		{
			// describe spacing width error
			if (errorType == DRCErrorType.SPACINGERROR)
				errorMessage.append("Spacing");
			else if (errorType == DRCErrorType.SURROUNDERROR)
				errorMessage.append("Surround");
			else
				errorMessage.append("Notch");
			if (layer1 == layer2)
				errorMessage.append(" (layer '" + layer1.getName() + "')");
			errorMessage.append(": ");

			if (np1 != np2)
			{
				errorMessage.append(np1 + ", ");
			} else if (np1 != cell)
			{
				errorMessage.append("[in " + np1 + "] ");
			}

            errorMessage.append(geom1);
			if (layer1 != layer2)
				errorMessage.append(", layer '" + layer1.getName() + "'");

			if (actual < 0) errorMessage.append(" OVERLAPS (BY " + TextUtils.formatDouble(limit-actual) + ") ");
			else if (actual == 0) errorMessage.append(" TOUCHES ");
			else errorMessage.append(" LESS (BY " + TextUtils.formatDouble(limit-actual) + ") THAN " + TextUtils.formatDouble(limit) + " TO ");

			if (np1 != np2)
				errorMessage.append(np2 + ", ");

			errorMessage.append(geom2);
			if (layer1 != layer2)
				errorMessage.append(", layer '" + layer2.getName() + "'");
			if (msg != null)
				errorMessage.append("; " + msg);
		} else
		{
			// describe minimum width/size or layer error
			StringBuffer errorMessagePart2 = null;
			switch (errorType)
			{
                case RESOLUTION:
                    errorMessage.append("Resolution error:");
					errorMessagePart2 = new StringBuffer(msg);
                    break;
                case FORBIDDEN:
                    errorMessage.append("Forbidden error:");
					errorMessagePart2 = new StringBuffer(msg);
                    break;
                case SLOTSIZEERROR:
                    errorMessage.append("Slot size error:");
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
					errorMessagePart2.append(" BIGGER THAN " + TextUtils.formatDouble(limit) + " IN LENGTH (IS " + TextUtils.formatDouble(actual) + ")");
                    break;
				case MINAREAERROR:
					errorMessage.append("Minimum area error:");
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
					errorMessagePart2.append(" LESS THAN " + TextUtils.formatDouble(limit) + " IN AREA (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case ENCLOSEDAREAERROR:
					errorMessage.append("Enclosed area error:");
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
					errorMessagePart2.append(" LESS THAN " + TextUtils.formatDouble(limit) + " IN AREA (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case TECHMIXWARN:
					errorMessage.append("Technology mixture warning:");
					errorMessagePart2 = new StringBuffer(msg);
					break;
				case ZEROLENGTHARCWARN:
					errorMessage.append("Zero width warning:");
					errorMessagePart2 = new StringBuffer(msg); break;
				case CUTERROR:
                    errorMessage.append("Maximum cut error" + ((msg != null) ? ("(" + msg + "):") : ""));
                    errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
                    errorMessagePart2.append(" BIGGER THAN " + TextUtils.formatDouble(limit) + " WIDE (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case MINWIDTHERROR:
                    errorMessage.append("Minimum width/heigh error" + ((msg != null) ? ("(" + msg + "):") : ""));
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
					errorMessagePart2.append(" LESS THAN " + TextUtils.formatDouble(limit) + " WIDE (IS " + TextUtils.formatDouble(actual) + ")");
                    break;
				case MINSIZEERROR:
					errorMessage.append("Minimum size error on " + msg + ":");
					errorMessagePart2 = new StringBuffer(" LESS THAN " + TextUtils.formatDouble(limit) + " IN SIZE (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case BADLAYERERROR:
					errorMessage.append("Invalid layer ('" + layer1.getName() + "'):");
					break;
				case LAYERSURROUNDERROR:
					errorMessage.append("Layer surround error: " + msg);
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
                    String layerName = (layer2 != null) ? layer2.getName() : "Select";
					errorMessagePart2.append(" NEEDS SURROUND OF LAYER '" + layerName + "' BY " + limit);
                    break;
			}

			errorMessage.append(" " + cell + " ");
			if (geom1 != null)
			{
				errorMessage.append(geom1);
			}
			if (layer1 != null) sortLayer = layer1.getIndex();
			errorMessage.append(errorMessagePart2);
		}
		if (rule != null && rule.length() > 0) errorMessage.append(" [rule '" + rule + "']");
		errorMessage.append(DRCexclusionMsg);

		List<Geometric> geomList = new ArrayList<Geometric>();
		List<PolyBase> polyList = new ArrayList<PolyBase>();
		if (poly1 != null) polyList.add(poly1); else
			if (geom1 != null) geomList.add(geom1);
		if (poly2 != null) polyList.add(poly2); else
			if (geom2 != null) geomList.add(geom2);
		if (onlyWarning)
            errorLogger.logWarning(errorMessage.toString(), geomList, null, null, null, polyList, cell, sortLayer);
        else
		    errorLogger.logError(errorMessage.toString(), geomList, null, null, null, polyList, cell, sortLayer);
        // Temporary display of errors.
        if (interactiveLogger)
            Job.getUserInterface().termLogging(errorLogger, false, false);
	}

	/**************************************************************************************************************
	 *  QuickAreaEnumerator class
	 **************************************************************************************************************/
	// Extra functions to check area
	private class QuickAreaEnumerator extends HierarchyEnumerator.Visitor
    {
		private GeometryHandler otherTypeMerge;
		private Layer polyLayer;
		private HashMap<NodeInst,NodeInst> notExportedNodes;
		private HashMap<NodeInst,NodeInst> checkedNodes;
        private GeometryHandler.GHMode mode; // geometrical merge algorithm
        private boolean preProcess = false;
        private boolean doneAll = false;
        private boolean doingNonExport = false;
        private List<QuickAreaBucket> buckets = new ArrayList<QuickAreaBucket>();

        /**
         * Case 2 & Case 3: original checking of hierarchy per network
         * Case 2: MapDone contains all layers with rules regardless if they are present in the layout
         * Case 3: Attempt to collect all layers in cells (preprocess) before running the final checking -> very slow
         * Case 4 and Case 5: Only 1 hierarchy down containing buckets. MapDone contains all layers with rules
         * regardless if they are present in the layout (same as Case2). They use more memory than Case2&Case3
         * Case 4: Checking (QuickAreaBucket bucket : buckets) in exitCell in all cells -> best result?
         * Case 5: Checking (QuickAreaBucket bucket : buckets) in exitCell only on Top Cell -> too slow
         * Case 6: Skip if addElement() adds element bigger than minArea -> doesn't work because I skip disconnected
         * Case 9: Prunning network buckets to check by checking network in the cells. Do process in enterCell and cache
         * the data
         * areas in a given network
         */
        private class QuickAreaBucket
        {
            boolean merged = false;
            GeometryHandler mainMerge;
            Network jNet;
            HashMap<Layer,Layer> minAreaLayerMapDone = new HashMap<Layer,Layer>();
            HashMap<Layer,Layer> enclosedAreaLayerMapDone = new HashMap<Layer,Layer>();

            QuickAreaBucket(Network net)
            {
                jNet = net;
                for (Layer layer : minAreaLayerMap.keySet()) minAreaLayerMapDone.put(layer, null);
                for (Layer layer : enclosedAreaLayerMap.keySet()) enclosedAreaLayerMapDone.put(layer, null);
            }

            /**
             * Method to check whether the layer must be checked or not. It might be done by this layer on that network
             * @param layer
             * @return true is the layer is should be skipped
             */
            private boolean skipLayer(Layer layer)
            {
                boolean noMinArea = minAreaLayerMap.get(layer) == null;
                boolean noMinEnc = enclosedAreaLayerMap.get(layer) == null;

                if (noMinArea && noMinEnc)
                    return true;
                if (preProcess)
                {
                    minAreaLayerMapDone.put(layer, null);
                    enclosedAreaLayerMapDone.put(layer, null);
                    return true;
                }
                else
                {
                    // Done means either doesn't have rule or it is reached.
                    boolean minADone = (noMinArea || (minAreaLayerMapDone.get(layer) != null));
                    boolean minEDone = (noMinEnc || (enclosedAreaLayerMapDone.get(layer) != null));
                    if (minADone && minEDone)
                        return true;
                }
                return false;
            }
        }

        class QuickAreaCellInfo extends HierarchyEnumerator.CellInfo
        {
            List<QuickAreaBucket> theseBuckets = null;

            List<QuickAreaBucket> getValidNetworks()
            {
                if (theseBuckets == null)
                {
                    theseBuckets = new ArrayList<QuickAreaBucket>();

                    for (QuickAreaBucket bucket : buckets)
                    {
                        boolean found = false;
                        for (Iterator<Network> it = getNetlist().getNetworks(); it.hasNext(); )
                        {
                            Network net = it.next();
                            found = HierarchyEnumerator.searchNetworkInParent(net, this, bucket.jNet);
                            if (found) break;
                        }
                        if (found) theseBuckets.add(bucket);
                    }
                }
                return theseBuckets;
            }
        }

        public HierarchyEnumerator.CellInfo newCellInfo() { return new QuickAreaCellInfo(); }

		public QuickAreaEnumerator(Netlist netlist, GeometryHandler selectMerge, HashMap<NodeInst,NodeInst> notExportedNodes,
		                           HashMap<NodeInst,NodeInst> checkedNodes, GeometryHandler.GHMode mode)
		{
			this.otherTypeMerge = selectMerge;
			this.notExportedNodes = notExportedNodes;
			this.checkedNodes = checkedNodes;
            this.mode = mode;

            for(Iterator<Network> netIt = netlist.getNetworks(); netIt.hasNext(); )
            {
                Network net = netIt.next();
                buckets.add(new QuickAreaBucket(net));
            }
		}

        public void setPreProcessFlag(boolean f) { preProcess = f; }

		public QuickAreaEnumerator(HashMap<NodeInst,NodeInst> notExportedNodes, HashMap<NodeInst,NodeInst> checkedNodes, GeometryHandler.GHMode mode)
		{
			this.notExportedNodes = notExportedNodes;
			this.checkedNodes = checkedNodes;
            this.mode = mode;
            this.doingNonExport = true;

            for (NodeInst ni : notExportedNodes.keySet())
            {
                Cell cell = ni.getParent();
                CheckProto cp = getCheckProto(cell);
                for (Iterator<PortInst> itP = ni.getPortInsts(); itP.hasNext();)
                {
                    PortInst pi = itP.next();
                    Network net = cp.netlist.getNetwork(pi);
                    buckets.add(new QuickAreaBucket(net));
                }
            }
		}

        /**
         * Method to check if all layers have been checked and are OK with the rule for a particular
         * network. This is under the assumption that connectivity gives you all the nodes connected so
         * there is no point of checking more once the minimum has been reached.
         * @return true if all layers are checked.
         */
        private boolean areAllLayersDone()
        {
            if (!doneAll)
            {
                boolean done = true;
                for (QuickAreaBucket bucket : buckets)
                {
                    for (Layer layer : bucket.minAreaLayerMapDone.keySet())
                    {
                        if (bucket.minAreaLayerMapDone.get(layer) == null) // not done
                        {
                            done = false;
                            break;
                        }
                    }
                    if (done)
                    {
                        for (Layer layer : bucket.enclosedAreaLayerMapDone.keySet())
                        {
                            if (bucket.enclosedAreaLayerMapDone.get(layer) == null) // not done
                            {
                                done = false;
                                break;
                            }
                        }
                    }
                    if (!done) break; // not sure what would be faster
                }
                doneAll = done;
            }
            return doneAll;
        }

		/**
		 */
		public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
			if (job != null && job.checkAbort()) return false;

            if (!preProcess && (doneAll || areAllLayersDone())) return false;
            // Create them only on top cell
            if (info.getCell() == topCell)
            {
                for (QuickAreaBucket bucket : buckets)
                {
                    bucket.mainMerge = GeometryHandler.createGeometryHandler(mode, info.getCell().getTechnology().getNumLayers());
                }
            }

            AffineTransform rTrans = info.getTransformToRoot();
            QuickAreaCellInfo areaInfo = (QuickAreaCellInfo)info;
            List<QuickAreaBucket> theseBuckets = areaInfo.getValidNetworks();

//            if (theseBuckets.size() == 0)
//                System.out.println("Should this be empty?");

			// Check Arcs too if network is not null. They can belong to an inner network (not exported up in the
            // hierarchy
//			if (theseBuckets.size() > 0)
			{
                List<QuickAreaBucket> onlyTheseBuckets = new ArrayList<QuickAreaBucket>();

				for(Iterator<ArcInst> it = info.getCell().getArcs(); it.hasNext(); )
				{
					ArcInst ai = it.next();
					Technology tech = ai.getProto().getTechnology();
					Network aNet = info.getNetlist().getNetwork(ai, 0);

                    // Only check those network buckets that are found as exports
                    onlyTheseBuckets.clear();
//                    for (QuickAreaBucket bucket : buckets)
                    for (QuickAreaBucket bucket : theseBuckets)
                    {
					    boolean found = false;
                        // aNet is null if ArcProto is Artwork
                        if (aNet != null)
                        {
                            for (Iterator<Export> arcIt = aNet.getExports(); !found && arcIt.hasNext();)
                            {
                                Export exp = arcIt.next();
                                Network net = info.getNetlist().getNetwork(exp, 0);
                                found = HierarchyEnumerator.searchNetworkInParent(net, info, bucket.jNet);
                            }
                        }

                        if (!found && aNet != bucket.jNet)
                            continue; // no same net
                        onlyTheseBuckets.add(bucket);
                    }

//                    if (!theseBuckets.containsAll(onlyTheseBuckets))
//                            System.out.println("Error here?");

                    if (onlyTheseBuckets.size() == 0)
                    {
                        // These nets should be checked locally to this particular cell!!
//                        System.out.println(ai.getName() + " not found in " + ai.getProto().getName() + " " + info.getCell().getName());
                        continue;
                    }

					Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
					int tot = arcInstPolyList.length;
					for(int i=0; i<tot; i++)
					{
						Poly poly = arcInstPolyList[i];
						Layer layer = poly.getLayer();

						// it has to take into account poly and transistor poly as one layer
						if (layer.getFunction().isPoly())
						{
							if (polyLayer == null)
								polyLayer = layer;
							layer = polyLayer;
						}

                        for (QuickAreaBucket bucket : onlyTheseBuckets)
                        {
                            // No area associated or already done
                            if (bucket.skipLayer(layer))
                                continue;

                            // only here when is really merging
                            assert(!preProcess);
                            poly.transform(rTrans);
                            addElement(bucket, poly, layer, false);
                        }
					}
				}
			}
			return true;
		}

		/**
		 */
		public void exitCell(HierarchyEnumerator.CellInfo info)
        {
            // If abort, it should not process minArea.
            if (preProcess || doneAll || (job != null && job.checkAbort())) return;
            boolean isTopCell = info.getCell() == topCell;

//            if (!isTopCell) return;     Case 5 too slow!. Case 4 is good for now

            for (QuickAreaBucket bucket : buckets)
            {
                if (!isTopCell && bucket.merged)
                    continue;
                bucket.mainMerge.postProcess(true);
                bucket.merged = true;
                for (Layer layer : bucket.mainMerge.getKeySet())
//                for(Iterator<Layer> it = bucket.mainMerge.getKeyIterator(); it.hasNext(); )
                {
//                    Layer layer = it.next();
                    if (bucket.skipLayer(layer)) continue; // done!
                    checkMinAreaLayer(bucket.mainMerge, info.getCell(), layer, isTopCell, bucket.minAreaLayerMapDone,
                            bucket.enclosedAreaLayerMapDone);
                    if (areAllLayersDone())
                        return; // done
                }
            }
        }

		/**
		 */
		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			if (job != null && job.checkAbort()) return false;

            if (doneAll) return false; // done!

            // Facet or special elements
			NodeInst ni = no.getNodeInst();
            if (NodeInst.isSpecialNode(ni)) return (false);

            if (doingNonExport && !notExportedNodes.containsKey(ni))
            {
//                System.out.println("Skipping this one");
                return false;
            }

			Cell cell = info.getCell();
			AffineTransform trans = ni.rotateOut();
			NodeProto np = ni.getProto();
			AffineTransform root = info.getTransformToRoot();
			if (root.getType() != AffineTransform.TYPE_IDENTITY)
				trans.preConcatenate(root);

			// Cells
			if (!(np instanceof PrimitiveNode)) return (true);

			PrimitiveNode pNp = (PrimitiveNode)np;
			boolean forceChecking = false;
            List<QuickAreaBucket> onlyTheseBuckets = new ArrayList<QuickAreaBucket>();
            QuickAreaCellInfo areaInfo = (QuickAreaCellInfo)info;
            List<QuickAreaBucket> theseBuckets = areaInfo.getValidNetworks();

			if (theseBuckets.size() > 0) //jNet != null)
			{
				boolean pureNode = (pNp.getFunction() == PrimitiveNode.Function.NODE);
				if (np instanceof PrimitiveNode)
				{
					if (NodeInst.isSpecialNode(ni)) return (false);
					forceChecking = pNp.isPureSubstrateNode(); // forcing the checking
				}

				boolean notExported = false;
				Network thisNet = null;
                boolean found = false;

                for(Iterator<PortInst> pIt = ni.getPortInsts(); !found && pIt.hasNext(); )
                {
                    PortInst pi = pIt.next();
                    PortProto pp = pi.getPortProto();
                    boolean isExported = (pp instanceof Export) && ((Export)pp).getParent() == cell;
                    thisNet = info.getNetlist().getNetwork(pi);
                    QuickAreaBucket thisBucket = null;
//                        for (QuickAreaBucket bucket : buckets)
                    for (QuickAreaBucket bucket : theseBuckets)
                    {
                        found = HierarchyEnumerator.searchNetworkInParent(thisNet, info, bucket.jNet);
                        if (found)
                        {
                            thisBucket = bucket;
                            break;
                        }
                    }
                    if (thisBucket != null) onlyTheseBuckets.add(thisBucket);
                    if (!isExported) notExported = true;
                }

                // Substrate layers are special so we must check node regardless network
                notExported = notExported && !forceChecking && !pureNode && info.getParentInfo() != null;
                if (!found && !forceChecking)
                {
                    if (notExportedNodes != null && notExported)
                    {
                        assert(!doingNonExport);
                        //addNodeInst(ni, pNp, forceChecking, null, trans, nonExportedMerge, nonExportedMerge);
                        notExportedNodes.put(ni, ni);
                    }
                    //else
//                        continue;
                        return (false);
                }

				// Removing node if it was found included in a network
				notExportedNodes.remove(ni);
				checkedNodes.put(ni, ni);
			}
			else
			{
                if (doingNonExport) return false;

                if (notExportedNodes.containsKey(ni))
                    System.out.println("NI " + ni + " in " + cell);
                notExportedNodes.put(ni, ni);
				if (checkedNodes.containsKey(ni))
					return (false);
                return false;
			}

            if (onlyTheseBuckets.size() == 0)
            {
                //These are candidates for extra checking internally!
//                System.out.println(ni.getName() + " not found NODE in " + " " + ni.getProto().getName() + " " + info.getCell().getName());
                return false;
            }

			Technology tech = pNp.getTechnology();
			// electrical should not be null due to ports but causes
			// problems with poly and transistor-poly
			Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, null, null, true, true, null);
			int tot = nodeInstPolyList.length;
			for(int i=0; i<tot; i++)
			{
				Poly poly = nodeInstPolyList[i];
				Layer layer = poly.getLayer();

				// it has to take into account poly and transistor poly as one layer
				if (layer.getFunction().isPoly())
				{
					if (polyLayer == null)
						polyLayer = layer;
					layer = polyLayer;
				}

                for (QuickAreaBucket bucket : onlyTheseBuckets)
                {
                    // No area associated or already done
                    if (bucket.skipLayer(layer))
                        continue;

                    // make sure this layer connects electrically to the desired port
                    // but only when checking networks, no nonExported.
                    if (bucket.jNet != null)
                    {
                        PortProto pp = poly.getPort();
                        boolean found = forceChecking && layer.getFunction().isSubstrate();
                        if (!found && pp != null)
                        {
                            Network net = info.getNetlist().getNetwork(ni, pp, 0);
                            found = HierarchyEnumerator.searchNetworkInParent(net, info, bucket.jNet);
                        }
                        //	if (!forceChecking && !found) continue;
                        if (!found) continue;
                    }
                    assert(!preProcess);
                    poly.roundPoints(); // Trying to avoid mismatches while joining areas.
                    poly.transform(trans);
                    addElement(bucket, poly, layer, true);
                }
			}

            return true;
		}

        private void addElement(QuickAreaBucket bucket, Poly poly, Layer layer, boolean isNode)
        {
            Shape obj = poly;
            // otherTypeMerge is null if nonExported nodes are analyzed
            if ((isNode && otherTypeMerge == null) || layer.getFunction().isMetal() || layer.getFunction().isPoly())
            {
                bucket.merged = false;
//                assert(bucket.minAreaLayerMapDone.get(layer) == null); // otherwise bucket.skipLayer should have detected
//                // Case 6
//                double area = poly.getArea();
//                DRCTemplate minAreaRule = minAreaLayerMap.get(layer);
//                if (!DBMath.isGreaterThan(minAreaRule.value1, area))
//                {
//                    bucket.minAreaLayerMapDone.put(layer, layer);
//                }
//                else
                    bucket.mainMerge.add(layer, obj);
            }
            else
                otherTypeMerge.add(layer, obj);
        }
    }

    /**************************************************************************************************************
	 *  QuickAreaEnumerator class
	 **************************************************************************************************************/
	// Extra functions to check area
	private class CheckAreaEnumerator extends HierarchyEnumerator.Visitor
    {
        HashMap<Cell,GeometryHandler> mainMergeMap = new HashMap<Cell,GeometryHandler>();
        private HashMap<Cell,GeometryHandler> otherTypeMergeMap;
        private HashMap<Cell,Cell> doneCells = new HashMap<Cell,Cell>(); // Mark if cells are done already.
		private Layer polyLayer;
        private GeometryHandler.GHMode mode; // geometrical merge algorithm

		public CheckAreaEnumerator(HashMap<Cell,GeometryHandler> selectMergeMap, GeometryHandler.GHMode mode)
		{
            this.otherTypeMergeMap = selectMergeMap;
            this.mode = mode;
		}

		public CheckAreaEnumerator(GeometryHandler.GHMode mode)
		{
            this.mode = mode;
		}

		/**
		 */
		public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
			if (job != null && job.checkAbort()) return false;

            Cell cell = info.getCell();
	        GeometryHandler thisMerge = mainMergeMap.get(cell);
            GeometryHandler thisOtherMerge = otherTypeMergeMap.get(cell);
            boolean firstTime = false;

            if (thisMerge == null)
            {
                thisMerge = GeometryHandler.createGeometryHandler(mode, cell.getTechnology().getNumLayers());
                thisOtherMerge = GeometryHandler.createGeometryHandler(mode, 4);
                mainMergeMap.put(cell, thisMerge);
                otherTypeMergeMap.put(cell, thisOtherMerge);
                firstTime = true;
            }

			// Check Arcs too if network is not null
			if (firstTime)
			{
				for(Iterator it = cell.getArcs(); it.hasNext(); )
				{
					ArcInst ai = (ArcInst)it.next();
					Technology tech = ai.getProto().getTechnology();
					Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
					int tot = arcInstPolyList.length;
					for(int i=0; i<tot; i++)
					{
						Poly poly = arcInstPolyList[i];
						Layer layer = poly.getLayer();

						// No area associated
						if (minAreaLayerMap.get(layer) == null && enclosedAreaLayerMap.get(layer) == null)
							continue;

						// it has to take into account poly and transistor poly as one layer
						if (layer.getFunction().isPoly())
						{
							if (polyLayer == null)
								polyLayer = layer;
							layer = polyLayer;
						}

                        addElement(thisMerge, thisOtherMerge, poly, layer, false);
					}
				}
			}
			return true;
		}

		/**
		 */
		public void exitCell(HierarchyEnumerator.CellInfo info)
        {
            Cell cell = info.getCell();
            boolean done = doneCells.get(cell) != null;

            if (!done)
            {
                GeometryHandler thisMerge = mainMergeMap.get(info.getCell());
                GeometryHandler thisOtherMerge = otherTypeMergeMap.get(info.getCell());

                thisMerge.postProcess(true);
                thisOtherMerge.postProcess(true);

                // merge everything sub trees
                for(Iterator it = cell.getNodes(); it.hasNext(); )
                {
                    NodeInst ni = (NodeInst)it.next();
                    NodeProto subNp = ni.getProto();
                    if (subNp instanceof PrimitiveNode) continue;
                    // get sub-merge information for the cell instance
                    GeometryHandler subMerge = (GeometryHandler)mainMergeMap.get(subNp);
                    if (subMerge != null)
                    {
                        AffineTransform tTrans = ni.translateOut(ni.rotateOut());
                        thisMerge.addAll(subMerge, tTrans);
                    }
                    GeometryHandler subOtherMerge = (GeometryHandler)otherTypeMergeMap.get(subNp);
                    if (subOtherMerge != null)
                    {
                        AffineTransform tTrans = ni.translateOut(ni.rotateOut());
                        thisOtherMerge.addAll(subOtherMerge, tTrans);
                    }
                }
            }

            // To mark if cell is already done
			doneCells.put(cell, cell);
        }

		/**
		 */
		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			if (job != null && job.checkAbort()) return false;

			Cell cell = info.getCell();
			NodeInst ni = no.getNodeInst();
			AffineTransform trans = ni.rotateOut();
			NodeProto np = ni.getProto();

			// Cells
			if (!(np instanceof PrimitiveNode)) return (true);

            // No done yet
	        if (doneCells.get(cell) == null)
            {
                GeometryHandler thisMerge = mainMergeMap.get(cell);
                GeometryHandler thisOtherMerge = otherTypeMergeMap.get(cell);

                PrimitiveNode pNp = (PrimitiveNode)np;
				if (np instanceof PrimitiveNode)
				{
					if (NodeInst.isSpecialNode(ni)) return (false);
				}

                Technology tech = pNp.getTechnology();
                // electrical should not be null due to ports but causes
                // problems with poly and transistor-poly
                Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, null, null, true, true, null);
                int tot = nodeInstPolyList.length;
                for(int i=0; i<tot; i++)
                {
                    Poly poly = nodeInstPolyList[i];
                    Layer layer = poly.getLayer();

                    // No area rule associated
                    if (minAreaLayerMap.get(layer) == null && enclosedAreaLayerMap.get(layer) == null)
                        continue;

                    // it has to take into account poly and transistor poly as one layer
                    if (layer.getFunction().isPoly())
                    {
                        if (polyLayer == null)
                            polyLayer = layer;
                        layer = polyLayer;
                    }
                    poly.roundPoints(); // Trying to avoid mismatches while joining areas.
                    poly.transform(trans);
                    addElement(thisMerge, thisOtherMerge, poly, layer, false);
                }
            }
            return true;
		}

        private void addElement(GeometryHandler mainMerge, GeometryHandler otherTypeMerge, Poly poly,
                                Layer layer, boolean isNode)
        {
            Shape obj = poly;
            // otherTypeMerge is null if nonExported nodes are analyzed
            if ((isNode && otherTypeMerge == null) || layer.getFunction().isMetal() || layer.getFunction().isPoly())
                mainMerge.add(layer, obj);
            else
                otherTypeMerge.add(layer, obj);
        }
    }
}

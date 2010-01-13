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
package com.sun.electric.tool.drc;

import com.sun.electric.database.geometry.*;
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
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger;

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
	private HashMap<NodeInst,CheckInst> checkInsts = null;
    private HashMap<Cell,CheckProto> checkProtos = null;
	private HashMap<Network,Integer[]> networkLists = null;
	private HashMap<Layer,DRCTemplate> minAreaLayerMap = new HashMap<Layer,DRCTemplate>();    // For minimum area checking
	private HashMap<Layer,DRCTemplate> enclosedAreaLayerMap = new HashMap<Layer,DRCTemplate>();    // For enclosed area checking
    private HashMap<Layer,DRCTemplate> spacingLayerMap = new HashMap<Layer,DRCTemplate>();    // to detect holes using the area function
    private HashMap<Layer,DRCTemplate> slotSizeLayerMap = new HashMap<Layer,DRCTemplate>();    // For max length checking
    private DRC.CheckDRCJob job; // Reference to running job
    private DRC.DRCPreferences dp;
	private HashMap<Cell,Cell> cellsMap = new HashMap<Cell,Cell>(); // for cell caching
    private HashMap<Geometric,Geometric> nodesMap = new HashMap<Geometric,Geometric>(); // for node caching
    private GeometryHandler.GHMode mergeMode = GeometryHandler.GHMode.ALGO_SWEEP; // .ALGO_QTREE;
    private Map<Layer,NodeInst> od2Layers = new HashMap<Layer,NodeInst>(3);  /** to control OD2 combination in the same die according to foundries */

	public Quick(DRC.CheckDRCJob j, DRC.DRCPreferences dp, GeometryHandler.GHMode mode)
	{
		this.job = j;
        this.dp = dp;
        this.mergeMode = mode;
	}

    private List<InstanceInter> instanceInteractionList = new ArrayList<InstanceInter>();

	/** a NodeInst that is too tiny for its connection. */		private NodeInst tinyNodeInst;
	/** the other Geometric in "tiny" errors. */				private Geometric tinyGeometric;
	/** for tracking the time of good DRC. */					private HashSet<Cell> goodSpacingDRCDate = new HashSet<Cell>();
	/** for tracking cells that need to clean good DRC vars */	private HashSet<Cell> cleanSpacingDRCDate = new HashSet<Cell>();
	/** for tracking the time of good DRC. */					private HashSet<Cell> goodAreaDRCDate = new HashSet<Cell>();
	/** for tracking cells that need to clean good DRC vars */	private HashSet<Cell> cleanAreaDRCDate = new HashSet<Cell>();
	/** Top cell for DRC */                                     private Cell topCell;
    /** Miscellanous data for DRC */                            private DRC.ReportInfo reportInfo;

//    /* for figuring out which layers are valid for DRC */
    // To speed up the layer process
    private ValidationLayers validLayers;

    public static ErrorLogger checkDesignRules(DRC.DRCPreferences dp, Cell cell, Geometric[] geomsToCheck, boolean[] validity)
    {
        ErrorLogger errorLog = DRC.getDRCErrorLogger(true, null);
        checkDesignRules(errorLog, cell, geomsToCheck, validity, null, null, dp,
                GeometryHandler.GHMode.ALGO_SWEEP, false);
        return errorLog;
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
	 * @param onlyArea
	 */
	public static void checkDesignRules(ErrorLogger errorLog, Cell cell, Geometric[] geomsToCheck, boolean[] validity,
                                               Rectangle2D bounds, DRC.CheckDRCJob drcJob, DRC.DRCPreferences dp, GeometryHandler.GHMode mode, boolean onlyArea)
	{
		Quick q = new Quick(drcJob, dp, mode);
        q.doCheck(errorLog, cell, geomsToCheck, validity, bounds, onlyArea);
	}

    // returns the number of errors found
	private void doCheck(ErrorLogger errorLog, Cell cell, Geometric[] geomsToCheck, boolean[] validity,
                                Rectangle2D bounds, boolean onlyArea)
	{
		// Check if there are DRC rules for particular tech
        Technology tech = cell.getTechnology();
		DRCRules rules = DRC.getRules(tech);

        // if checking specific instances, adjust options and processor count
        int count = (geomsToCheck != null) ? geomsToCheck.length : 0;

        reportInfo = new DRC.ReportInfo(errorLog, tech, dp, (count > 0));
        ErrorLogger errorLogger = errorLog;

        // caching bits
        System.out.println("Running DRC with " + DRC.explainBits(reportInfo.activeSpacingBits, dp));

		// Nothing to check for this particular technology
		if (rules == null || rules.getNumberOfRules() == 0) return /*errorLogger*/;

	    topCell = cell; /* Especially important for minArea checking */

		// cache valid layers for this technology
        validLayers = new ValidationLayers(reportInfo.errorLogger, topCell, rules);

		// clean out the cache of instances
	    instanceInteractionList.clear();

		// determine maximum DRC interaction distance
//		worstInteractionDistance = DRC.getWorstSpacingDistance(tech, -1);

	    // determine if min area must be checked (if any layer got valid data)
	    minAreaLayerMap.clear();
	    enclosedAreaLayerMap.clear();
        spacingLayerMap.clear();
        slotSizeLayerMap.clear();
	    cellsMap.clear();
	    nodesMap.clear();

	    // No incremental neither per Cell
	    if (!dp.ignoreAreaCheck && reportInfo.errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_CELL)
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

                // Storing spacing rules
                DRCTemplate spaceRule = DRC.getSpacingRule(layer, null, layer, null, true, -1, -1.0, -1.0); // UCONSPA, CONSPA or SPACING
                if (spaceRule != null)
                    spacingLayerMap.put(layer, spaceRule);

                // Storing slot sizes
				DRCTemplate slotRule = DRC.getMinValue(layer, DRCTemplate.DRCRuleType.SLOTSIZE);
				if (slotRule != null)
					slotSizeLayerMap.put(layer, slotRule);
			}
            if (onlyArea)
            {
                int numErrors = checkMinAreaSlow(cell);
                if (numErrors == 0)
                    goodAreaDRCDate.add(cell);
                else
                    cleanAreaDRCDate.add(cell);
                System.out.println("Missing update in dates");
                return /*errorLogger*/;
            }
        }

		// initialize all cells for hierarchical network numbering
		checkProtos = new HashMap<Cell,CheckProto>();
		checkInsts = new HashMap<NodeInst,CheckInst>();

		// initialize cells in tree for hierarchical network numbering
		Netlist netlist = cell.getNetlist();
		CheckProto cp = checkEnumerateProtos(cell, netlist);

		// now recursively examine, setting information on all instances
		cp.hierInstanceCount = 1;
		reportInfo.checkTimeStamp = 0;
		checkEnumerateInstances(cell);

		// now allocate space for hierarchical network arrays
		//int totalNetworks = 0;
		networkLists = new HashMap<Network,Integer[]>();
        for (Map.Entry<Cell,CheckProto> e : checkProtos.entrySet())
		{
			Cell libCell = e.getKey();
			CheckProto subCP = e.getValue();
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
			reportInfo.checkTimeStamp++;
			for(Iterator<NodeInst> nIt = libCell.getNodes(); nIt.hasNext(); )
			{
				NodeInst ni = nIt.next();
				NodeProto np = ni.getProto();
				if (!ni.isCellInstance()) continue;

				// ignore documentation icons
				if (ni.isIconOfParent()) continue;

				CheckProto ocp = getCheckProto((Cell)np);
				if (ocp.timeStamp != reportInfo.checkTimeStamp)
				{
					CheckInst ci = checkInsts.get(ni);
					ocp.timeStamp = reportInfo.checkTimeStamp;
					ocp.totalPerCell += subCP.hierInstanceCount * ci.multiplier;
				}
			}
		}

		// now fill in the hierarchical network arrays
		reportInfo.checkTimeStamp = 0;
		int checkNetNumber = 1;

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
        reportInfo.exclusionMap.clear();
		accumulateExclusion(cell);

		// now do the DRC
        int logsFound = 0;
//        int totalErrors = 0;

		if (count == 0)
		{
			// just do full DRC here
			/*totalErrors =*/ checkThisCell(cell, 0, bounds);
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

//		if (errorLogger != null) {
//            errorLogger.termLogging(true);
//            logsFound = errorLogger.getNumLogs() - logsFound;
//        }

        // -2 if cells and subcells are ok
        // Commentted out on May 2, 2006. Not sure why this condition is valid
        // If goodDRCDate contains information of DRC clean cells, it destroys that information -> wrong
//		if (totalErrors != 0 && totalErrors != -2) goodDRCDate.clear();

		// some cells were sucessfully checked: save that information in the database
	    // some cells don't have valid DRC date anymore and therefore they should be clean
        // This is only going to happen if job was not aborted.
	    if ((job == null || !job.checkAbort()))
	    {
            DRC.addDRCUpdate(reportInfo.activeSpacingBits, goodSpacingDRCDate, cleanSpacingDRCDate,
                goodAreaDRCDate, cleanAreaDRCDate, null, dp);
	    }
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
		if (reportInfo.errorLogger != null)
		{
			prevErrors = reportInfo.errorLogger.getNumErrors();
			prevWarns = reportInfo.errorLogger.getNumWarnings();
		}

		cellsMap.put(cell, cell);

        // Check if cell doesn't have special annotation
        Variable drcVar = cell.getVar(DRC.DRC_ANNOTATION_KEY);
        if (drcVar != null)
        {
            assert(drcVar.getLength() == 1 && drcVar.getObject(0) instanceof String);
            String val = (String)drcVar.getObject(0);
            if (val.toUpperCase().equals("BLACK"))
            {
                // Skipping this one
                assert(reportInfo.totalSpacingMsgFound == 0); // get rid of this variable.
                return reportInfo.totalSpacingMsgFound;
            }
        }

        // first check all subcells
		boolean allSubCellsStillOK = true;
        Area area = reportInfo.exclusionMap.get(cell);

		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
            if (job != null && job.checkAbort()) return -1;

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
			if (retval > 0 || goodSpacingDRCDate.contains(np))
                allSubCellsStillOK = false;
//            if (retval > 0)
//                allSubCellsStillOK = false;
		}

		// prepare to check cell
		CheckProto cp = getCheckProto(cell);
		cp.cellChecked = true;
        boolean checkArea = (cell == topCell && !dp.ignoreAreaCheck && reportInfo.errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_CELL);

        // if the cell hasn't changed since the last good check, stop now
        Date lastSpacingGoodDate = DRC.getLastDRCDateBasedOnBits(cell, true, reportInfo.activeSpacingBits, !reportInfo.inMemory);
        Date lastAreaGoodDate = DRC.getLastDRCDateBasedOnBits(cell, false, -1, !reportInfo.inMemory);
        if (allSubCellsStillOK && DRC.isCellDRCDateGood(cell, lastSpacingGoodDate) &&
            (!checkArea || DRC.isCellDRCDateGood(cell, lastAreaGoodDate)))
		{
            return 0;
		}

		// announce progress
        long startTime = System.currentTimeMillis();
		System.out.println("Checking " + cell);

		// now look at every node and arc here
		reportInfo.totalSpacingMsgFound = 0;

		// Check the area first but only when is not incremental
		// Only for the most top cell
		if (checkArea)
        {
//            totalMsgFound = checkMinArea(cell);
            assert(reportInfo.totalSpacingMsgFound == 0);
            int totalAreaMsgFound = checkMinAreaSlow(cell);
            if (totalAreaMsgFound == 0)
                goodAreaDRCDate.add(cell);
            else
                cleanAreaDRCDate.add(cell);
        }

        for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
            if (job != null && job.checkAbort()) return -1;

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
				reportInfo.totalSpacingMsgFound++;
				if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) break;
			}
		}
		Technology cellTech = cell.getTechnology();
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
            if (job != null && job.checkAbort()) return -1;

            ArcInst ai = it.next();
			Technology tech = ai.getProto().getTechnology();
			if (tech != cellTech)
			{
				DRC.createDRCErrorLogger(reportInfo, DRC.DRCErrorType.TECHMIXWARN, " belongs to " + tech.getTechName(),
                    cell, 0, 0, null, null, ai, null, null, null, null);
				continue;
			}
			if (bounds != null)
			{
				if (!ai.getBounds().intersects(bounds)) continue;
			}
			if (checkArcInst(cp, ai, globalIndex))
			{
				reportInfo.totalSpacingMsgFound++;
				if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) break;
			}
		}

		// If message founds, then remove any possible good date
        // !allSubCellsStillOK disconnected on April 18, 2006. totalMsgFound should
        // dictate if this cell is re-marked.
		if (reportInfo.totalSpacingMsgFound > 0) //  || !allSubCellsStillOK)
		{
			cleanSpacingDRCDate.add(cell);
		}
		else
		{
            // Only mark the cell when it passes with a new version of DRC or didn't have
            // the DRC bit on
            // If lastGoodDate == null, wrong bits stored or no date available.
            if (lastSpacingGoodDate == null)
			    goodSpacingDRCDate.add(cell);
		}

		// if there were no errors, remember that
		if (reportInfo.errorLogger != null)
		{
			int localErrors = reportInfo.errorLogger.getNumErrors() - prevErrors;
			int localWarnings = reportInfo.errorLogger.getNumWarnings() - prevWarns;
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

		return reportInfo.totalSpacingMsgFound;
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
        double minAllowedResolution = reportInfo.minAllowedResolution;
        if (minAllowedResolution == 0) return false;
        int count = 0;
        String resolutionError = "";
		Point2D [] points = poly.getPoints();
        for(Point2D point : points)
		{
            if (DBMath.hasRemainder(point.getX(), minAllowedResolution))
            {
                count++;
                resolutionError = TextUtils.formatDistance(Math.abs(((point.getX()/minAllowedResolution) % 1) * minAllowedResolution)) +
                        "(X=" + point.getX() + ")";
                break; // with one error is enough
            }
            else if (DBMath.hasRemainder(point.getY(), minAllowedResolution))
            {
                count++;
                resolutionError = TextUtils.formatDistance(Math.abs(((point.getY()/minAllowedResolution) % 1) * minAllowedResolution)) +
                        "(Y=" + point.getY() + ")";
                break;
            }
		}
		if (count == 0) return false; // no error

        // there was an error, for now print error
        Layer layer = poly.getLayer();
        DRC.createDRCErrorLogger(reportInfo,DRC.DRCErrorType.RESOLUTION, " resolution of " + resolutionError + " less than " + minAllowedResolution +
                " on layer " + layer.getName(), cell, 0, 0, "Resolution", null, geom, null, null, null, null);
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
        Area area = reportInfo.exclusionMap.get(cell);
        if (area != null && area.contains(geo.getBounds()))
        {
//            System.out.println("DRC Exclusion found");
            return true;
        }
        return false;
    }

    /**
     * Private method to check if geometry given by Poly object is inside an exclusion region
     * @param poly
     * @param parent
     * @return true if Poly is inside an exclusion region
     */
    private boolean coverByExclusion(Poly poly, Cell parent)
    {
        Area area = reportInfo.exclusionMap.get(parent);
        if (area == null) return false;

        Point2D[] pts = poly.getPoints();
        for (Point2D point : pts)
        {
            if (!area.contains(point))
            {
                return false;
            }
        }
        return true;
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

        if (!np.getTechnology().isLayout())
            return (false); // only layout nodes

        if (np.getFunction().isPin()) return false; // Sept 30

        if (coverByExclusion(ni)) return false; // no errors

        // Already done
		if (nodesMap.get(ni) != null)
			return (false);
		nodesMap.put(ni, ni);

        if (DRC.checkNodeAgainstCombinationRules(ni, reportInfo))
        {
            if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
            errorsFound = true;
        }
//        if (np instanceof PrimitiveNode)
//        {
//            DRCTemplate forbidRule =
//            DRC.isForbiddenNode(((PrimitiveNode)np).getPrimNodeIndexInTech(), -1,
//                DRCTemplate.DRCRuleType.FORBIDDEN, tech);
//            if (forbidRule != null)
//            {
//                DRC.createDRCErrorLogger(reportInfo, DRC.DRCErrorType.FORBIDDEN, " is not allowed by selected foundry", cell,
//                    -1, -1, forbidRule.nodeName, null, ni, null, null, null, null);
//                if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
//                errorsFound = true;
//            }
//        }

        // get all of the polygons on this node
		Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, true, reportInfo.ignoreCenterCuts, null);
		convertPseudoLayers(ni, nodeInstPolyList);
		int tot = nodeInstPolyList.length;
        boolean isTransistor =  np.getFunction().isTransistor();
        Technology.MultiCutData multiCutData = tech.getMultiCutData(ni);

        // examine the polygons on this node
		for(int j=0; j<tot; j++)
		{
			Poly poly = nodeInstPolyList[j];
			Layer layer = poly.getLayer();
			if (layer == null) continue;

            if (coverByExclusion(poly, cell))
                continue;

            // Checking combination
            boolean ret = DRC.checkOD2Combination(tech, ni, layer, od2Layers, reportInfo);
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
				if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}

			// determine network for this polygon
			int netNumber = getDRCNetNumber(netlist, poly.getPort(), ni, globalIndex);

			ret = badBox(poly, layer, netNumber, tech, ni, trans, cell, globalIndex, multiCutData);
			if (ret)
			{
				if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
			ret = checkMinWidth(ni, layer, poly, tot==1);
			if (ret)
			{
				if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
			// Check select over transistor poly
			// Assumes polys on transistors fulfill condition by construction
//			ret = !isTransistor && checkSelectOverPolysilicon(ni, layer, poly, cell);
            ret = !isTransistor && checkExtensionRules(ni, layer, poly, cell);
            if (ret)
			{
				if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
//            ret = checkExtensionRule(ni, layer, poly, cell);
//            if (ret)
//			{
//				if (errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
//				errorsFound = true;
//			}
            if (validLayers.isABadLayer(tech, layer.getIndex()))
			{
				DRC.createDRCErrorLogger(reportInfo, DRC.DRCErrorType.BADLAYERERROR, null, cell, 0, 0, null,
					poly, ni, layer, null, null, null);
				if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
		}

		// check node for minimum size
        if (DRC.checkNodeSize(ni, cell, reportInfo))
            errorsFound = true;

		return errorsFound;
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

		boolean errorsFound = false;
        // Checking if the arc is horizontal or vertical
        Point2D from = ai.getHeadLocation();
        Point2D to = ai.getTailLocation();

        if (!DBMath.areEquals(from.getX(), to.getX()) && !DBMath.areEquals(from.getY(), to.getY()))
        {
            DRC.createDRCErrorLogger(reportInfo, DRC.DRCErrorType.CROOKEDERROR, null, ai.getParent(),
                -1, -1, null, null, ai, null, null, null, null);
            if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
        }

        // get all of the polygons on this arc
		Technology tech = ai.getProto().getTechnology();
		Poly [] arcInstPolyList = tech.getShapeOfArc(ai);

        // Check resolution before cropping the
        for(Poly poly : arcInstPolyList)
		{
			Layer layer = poly.getLayer();
			if (layer == null) continue;
			if (layer.isNonElectrical()) continue;
            // Checking resolution
            boolean ret = checkResolution(poly, ai.getParent(), ai);
            if (ret)
			{
				if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
        }

		DRC.cropActiveArc(ai, reportInfo.ignoreCenterCuts, arcInstPolyList);
		int tot = arcInstPolyList.length;
		// examine the polygons on this arc
		for(int j=0; j<tot; j++)
		{
			Poly poly = arcInstPolyList[j];
			Layer layer = poly.getLayer();
			if (layer == null) continue;
			if (layer.isNonElectrical()) continue;

			int layerNum = layer.getIndex();
			int netNumber = netNumbers[globalIndex]; // autoboxing
			boolean ret = badBox(poly, layer, netNumber, tech, ai, DBMath.MATID, ai.getParent(), globalIndex, null);
			if (ret)
			{
				if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
			ret = checkMinWidth(ai, layer, poly, tot==1);
			if (ret)
			{
				if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
			// Check select over transistor poly
//			ret = checkSelectOverPolysilicon(ai, layer, poly, ai.getParent());
            ret = checkExtensionRules(ai, layer, poly, ai.getParent());
            if (ret)
			{
				if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
				errorsFound = true;
			}
            if (validLayers.isABadLayer(tech, layerNum))
			{
				DRC.createDRCErrorLogger(reportInfo, DRC.DRCErrorType.BADLAYERERROR, null, ai.getParent(), 0, 0, null,
					(tot==1)?null:poly, ai, layer, null, null, null);
				if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
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
        Cell thisCell = (Cell)ni.getProto();
        if (!thisCell.isLayout())
            return false; // skips non-layout cells.

        // get transformation out of the instance
        AffineTransform upTrans = ni.translateOut(ni.rotateOut());

		// get network numbering for the instance
		CheckInst ci = checkInsts.get(ni);
		int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;
        boolean errorFound = false;

        // look for other instances surrounding this one
		Rectangle2D nodeBounds = ni.getBounds();
        double worstInteractionDistance = reportInfo.worstInteractionDistance;
        Rectangle2D searchBounds = new Rectangle2D.Double(
			nodeBounds.getMinX()-worstInteractionDistance,
			nodeBounds.getMinY()-worstInteractionDistance,
			nodeBounds.getWidth() + worstInteractionDistance*2,
			nodeBounds.getHeight() + worstInteractionDistance*2);

        instanceInteractionList.clear(); // part3
		for(Iterator<RTBounds> it = ni.getParent().searchIterator(searchBounds); it.hasNext(); )
		{
			RTBounds geom = it.next();

			if ( geom == ni ) continue; // covered by checkInteraction?

			if (!(geom instanceof NodeInst)) continue;
			NodeInst oNi = (NodeInst)geom;

			// only check other nodes that are numerically higher (so each pair is only checked once)
			if (oNi.getNodeIndex() <= ni.getNodeIndex()) continue;
			if (!oNi.isCellInstance()) continue;

			// see if this configuration of instances has already been done
			if (checkInteraction(ni, ni, oNi, oNi, ni, searchBounds)) continue;

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

		for(Iterator<RTBounds> it = cell.searchIterator(bb); it.hasNext(); )
		{
			RTBounds geom = it.next();

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
                    if (checkInteraction(ni, thisNi, oNi, oNiParent, triggerNi, bb)) continue;  // Jan 27'05. Removed on May'05
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
						if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
						logsFound = true;
					}
				} else
				{
					AffineTransform rTrans = ni.rotateOut();
					rTrans.preConcatenate(upTrans);
					Technology tech = np.getTechnology();
					Poly [] primPolyList = tech.getShapeOfNode(ni, true, reportInfo.ignoreCenterCuts, null);
					convertPseudoLayers(ni, primPolyList);
					int tot = primPolyList.length;
                    Technology.MultiCutData multiCutData = tech.getMultiCutData(ni);

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
							globalIndex, oNi, topGlobalIndex, multiCutData);
						if (ret)
						{
							if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
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
					DRC.createDRCErrorLogger(reportInfo, DRC.DRCErrorType.TECHMIXWARN, " belongs to " + tech.getTechName(),
                        cell, 0, 0, null, null, ai, null, null, null, null);
					continue;
				}

				Poly [] arcPolyList = tech.getShapeOfArc(ai);
				int tot = arcPolyList.length;
				for(int j=0; j<tot; j++)
					arcPolyList[j].transform(upTrans);
				DRC.cropActiveArc(ai, reportInfo.ignoreCenterCuts, arcPolyList);
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
						globalIndex, oNi, topGlobalIndex, null);
					if (ret)
					{
						if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_CELL) return true;
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
                              int globalIndex, NodeInst oNi, int topGlobalIndex, Technology.MultiCutData multiCutData)
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
//        Technology.MultiCutData multiCutData = null;
//		if (geom instanceof NodeInst)
//        {
//            multiCutData = tech.getMultiCutData((NodeInst)geom);
//        }
//        boolean baseMulti = tech.isMultiCutInTechnology(multiCutData);

		// search in the area surrounding the box
		bounds.setRect(bounds.getMinX()-bound, bounds.getMinY()-bound, bounds.getWidth()+bound*2, bounds.getHeight()+bound*2);
		return (badBoxInArea(poly, layer, tech, net, geom, trans, globalIndex, bounds, (Cell)oNi.getProto(), localIndex,
                oNi.getParent(), topGlobalIndex, upTrans, multiCutData, false));
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
                           AffineTransform trans, Cell cell, int globalIndex, Technology.MultiCutData multiCutData)
	{
		// see how far around the box it is necessary to search
		double maxSize = poly.getMaxSize();
		double bound = DRC.getMaxSurround(layer, maxSize);
		if (bound < 0) return false;

		// get bounds
		Rectangle2D bounds = new Rectangle2D.Double();
		bounds.setRect(poly.getBounds2D());

        // search in the area surrounding the box
		bounds.setRect(bounds.getMinX()-bound, bounds.getMinY()-bound, bounds.getWidth()+bound*2, bounds.getHeight()+bound*2);
		return badBoxInArea(poly, layer, tech, net, geom, trans, globalIndex, bounds, cell, globalIndex,
			    cell, globalIndex, DBMath.MATID, multiCutData, true);
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
                                 Cell topCell, int topGlobalIndex, AffineTransform upTrans, Technology.MultiCutData multiCutData,
                                 boolean sameInstance)
	{
		Rectangle2D rBound = new Rectangle2D.Double();
		rBound.setRect(bounds);
		DBMath.transformRect(rBound, upTrans); // Step 1
		Netlist netlist = getCheckProto(cell).netlist;
        Rectangle2D subBound = new Rectangle2D.Double(); //Sept 30
        boolean foundError = false;
        boolean isLayerAContact = layer.getFunction().isContact();
        boolean baseMulti = tech.isMultiCutInTechnology(multiCutData);

        // These nodes won't generate any DRC errors. Most of them are pins
		if (geom instanceof NodeInst && NodeInst.isSpecialNode(((NodeInst)geom)))
			return false;

		// Sept04 changes: bounds by rBound
		for(Iterator<RTBounds> it = cell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric nGeom = (Geometric)it.next();
            // I have to check if they are the same instance otherwise I check geometry against itself
            if (nGeom == geom && (sameInstance))// || nGeom.getParent() == cell))
                continue;

            // Checking if element is covered by exclusion region
            if (coverByExclusion(nGeom)) continue; // skips this element

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
						             topCell, topGlobalIndex, subTrans, multiCutData, sameInstance))
                    {
                        foundError = true;
                        if (reportInfo.errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                    }
				} else
				{
					// don't check between technologies
					if (np.getTechnology() != tech) continue;

					// see if this type of node can interact with this layer
					if (!validLayers.checkLayerWithNode(layer, np)) continue;

                    // see if the objects directly touch but they are not
                    // coming from different NodeInst (not from checkCellInstContents
                    // because they might below to the same cell but in different instances
					boolean touch = sameInstance && nGeom.isConnected(geom);

					// prepare to examine every layer in this nodeinst
					AffineTransform rTrans = ni.rotateOut();
					rTrans.preConcatenate(upTrans);

					// get the shape of each nodeinst layer
					Poly [] subPolyList = tech.getShapeOfNode(ni, true, reportInfo.ignoreCenterCuts, null);
					convertPseudoLayers(ni, subPolyList);
					int tot = subPolyList.length;
					for(int i=0; i<tot; i++)
						subPolyList[i].transform(rTrans);
					    /* Step 1 */
					boolean multi = baseMulti;
                    Technology.MultiCutData niMCD = tech.getMultiCutData(ni);
                    // in case it is one via against many from another contact (3-contact configuration)
                    if (!multi && isLayerAContact && niMCD != null)
                    {
                        multi = tech.isMultiCutInTechnology(niMCD);
                        if (!multi)
                        {
                            // geom must be NodeInst
                            NodeInst gNi = (NodeInst)geom;
                            // in this case, both possible contacts are either 1xn or mx1 with n,m>=1
                            if (multiCutData == null || multiCutData.numCutsX() == 1) // it is 1xn
                            {
                                // Checking if they match at the Y axis. If yes, they are considered as a long 1xn array
                                // so multi=false. The other element must be 1xM
                                if (niMCD.numCutsY() != 1 && ni.getAnchorCenterX() != gNi.getAnchorCenterX())
                                    multi = true;
                            }
                            else
                            {
                                // Checking if they match at the Y axis. If yes, they are considered as a long 1xn array
                                // so multi=false
                                if (niMCD.numCutsX() != 1 && ni.getAnchorCenterY() != gNi.getAnchorCenterY())
                                    multi = true;
                            }
                        }
                    }
                    int multiInt = (multi) ? 1 : 0;
					for(int j=0; j<tot; j++)
					{
						Poly npoly = subPolyList[j];
						Layer nLayer = npoly.getLayer();
						if (nLayer == null) continue;

                        if (coverByExclusion(npoly, nGeom.getParent()))
                            continue;
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
                            if (reportInfo.errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                        }

                        // check if both polys are cut and if the combine area doesn't excess cut sizes
                        // regardless if they are connected or not
                        ret = checkCutSizes(np, geom, layer, poly, nGeom, nLayer, npoly, topCell);
                        if (ret)
                        {
                            foundError = true;
                            if (reportInfo.errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                        }

						// if they connect electrically and adjoin, don't check
						if (con && touch)
                        {
                            // Check if there are minimum size defects
                            boolean maytouch = DRC.mayTouch(tech, con, layer, nLayer);
                            Rectangle2D trueBox1 = poly.getBox();
                            if (trueBox1 == null) trueBox1 = poly.getBounds2D();
                            Rectangle2D trueBox2 = npoly.getBox();
                            if (trueBox2 == null) trueBox1 = npoly.getBounds2D();
                            ret = checkMinDefects(cell,maytouch, geom, poly, trueBox1, layer,
                                    nGeom, npoly, trueBox2, nLayer, topCell);
                            if (ret)
                            {
                                foundError = true;
                                if (reportInfo.errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                            }
                            continue;
                        }

						boolean edge = false;
                        // @TODO check if previous spacing rule was composed of same layers!
                        DRCTemplate theRule = getSpacingRule(layer, poly, geom, nLayer, npoly, ni, con, multiInt);

                        // Try edge rules
                        if (theRule == null)
                        {
						    theRule = DRC.getEdgeRule(layer, nLayer);
                            edge = true;
                        }

                        if (theRule == null) continue;

                        ret = checkDist(tech, topCell, topGlobalIndex,
                            poly, layer, net, geom, trans, globalIndex,
                            npoly, nLayer, nNet, nGeom, rTrans, cellGlobalIndex,
                            con, theRule, edge);
                        if (ret)
                        {
                            foundError = true;
                            if (reportInfo.errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
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
				if (!validLayers.checkLayerWithArc(layer, ap)) continue;

                // see if the objects directly touch
				boolean touch = sameInstance && nGeom.isConnected(geom);

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
				DRC.cropActiveArc(ai, reportInfo.ignoreCenterCuts, subPolyList);
				boolean multi = baseMulti; // this condition is not very relevant for arcs because no cut/via is used in arcs
                int multiInt = (multi) ? 1 : 0;

				for(int j=0; j<tot; j++)
				{
					Poly nPoly = subPolyList[j];
					Layer nLayer = nPoly.getLayer();
					if (nLayer == null) continue;

                    if (coverByExclusion(nPoly, nGeom.getParent()))
                            continue;

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
                        boolean maytouch = DRC.mayTouch(tech, con, layer, nLayer);
                        Rectangle2D trueBox1 = poly.getBox();
                        if (trueBox1 == null) trueBox1 = poly.getBounds2D();
                        Rectangle2D trueBox2 = nPoly.getBox();
                        if (trueBox2 == null) trueBox1 = nPoly.getBounds2D();
                        ret = checkMinDefects(cell, maytouch, geom, poly, trueBox1, layer,
                                nGeom, nPoly, trueBox2, nLayer, topCell);
                        if (ret)
                        {
                            foundError = true;
                            if (reportInfo.errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
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
                        if (reportInfo.errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                    }

                    // Checking extension, it could be slow
                    ret = checkExtensionGateRule(geom, layer, poly, nLayer, nPoly, netlist);
                    if (ret)
                    {
                        foundError = true;
                        if (reportInfo.errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
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
        double pdx = DBMath.round(Math.max(trueBox2.getMinX()-trueBox1.getMaxX(), trueBox1.getMinX()-trueBox2.getMaxX()));
        double pdy = DBMath.round(Math.max(trueBox2.getMinY()-trueBox1.getMaxY(), trueBox1.getMinY()-trueBox2.getMaxY()));
        double pd = Math.max(pdx, pdy);

        if (DBMath.areEquals(pdx, 0) && DBMath.areEquals(pdy, 0))
            pd = 0; // touching
        boolean foundError = false;

        // They have to overlap
        if (DBMath.isGreaterThan(pd, 0)) return false;

        // they are electrically connected and they overlap: look for minimum size errors
        // of the overlapping region.
        DRCTemplate wRule = DRC.getMinValue(layer1, DRCTemplate.DRCRuleType.MINWID);
        if (wRule == null) return false; // no rule

        double minWidth = wRule.getValue(0);
        double lxb = DBMath.round(Math.max(trueBox1.getMinX(), trueBox2.getMinX()));
        double hxb = DBMath.round(Math.min(trueBox1.getMaxX(), trueBox2.getMaxX()));
        double lyb = DBMath.round(Math.max(trueBox1.getMinY(), trueBox2.getMinY()));
        double hyb = DBMath.round(Math.min(trueBox1.getMaxY(), trueBox2.getMaxY()));
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

        // !DBMath.areEquals(actual, 0) is on, it skips cases where A==B.
        // Condition off as of July 2nd 2008. Bugzilla 1745
        if (/*!DBMath.areEquals(actual, 0) &&*/ DBMath.isGreaterThan(minWidth, actual) &&
            foundSmallSizeDefect(cell, geom1, poly1, layer1, geom2, poly2, pd, lxb, lyb, hxb, hyb))
        {
            // you can't pass geom1 or geom2 becuase they could be in different cells and therefore the message
            // could mislead
            DRC.createDRCErrorLogger(reportInfo, DRC.DRCErrorType.MINWIDTHERROR, null, topCell, minWidth, actual, wRule.ruleName, new Poly(bounds),
                            null, layer1, null, null, layer2);
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
        Point2D pt1 = null; //new Point2D.Double(lxb-TINYDELTA, lyb-TINYDELTA);
        Point2D pt2 = null; //new Point2D.Double(lxb-TINYDELTA, hyb+TINYDELTA);
        Point2D pt1d = null;//(Point2D)pt1.clone();
        Point2D pt2d = null; //(Point2D)pt2.clone();

        // Search area should be bigger than bounding box otherwise it might not get the cells due to
        // rounding errors.
        Rectangle2D search = new Rectangle2D.Double(DBMath.round(lxb- DRC.TINYDELTA), DBMath.round(lyb- DRC.TINYDELTA),
                DBMath.round(hxb-lxb+2* DRC.TINYDELTA), DBMath.round(hyb-lyb+2* DRC.TINYDELTA));

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
                pt1d = new Point2D.Double(pt1.getX()- DRC.TINYDELTA, -delta* DRC.TINYDELTA+pt1.getY());
                // (y2-y1)/(x2-x1)(x2+tinydelta-x2) + y2
                pt2d = new Point2D.Double(pt2.getX()+ DRC.TINYDELTA, delta* DRC.TINYDELTA+pt2.getY());
            }
            else
            {
                pt1d = new Point2D.Double(pt2.getX()- DRC.TINYDELTA, -delta* DRC.TINYDELTA+pt2.getY());
                pt2d = new Point2D.Double(pt1.getX()+ DRC.TINYDELTA, delta* DRC.TINYDELTA+pt1.getY());
            }
            if (DBMath.areEquals(pt1.getX(), pt2.getX()) || DBMath.areEquals(pt1.getY(), pt2.getY()))
            {
                return false;
            }
        }
        // looking if points around the overlapping area are inside another region
        // to avoid the error
        DRC.lookForLayerCoverage(geom1, poly1, geom2, poly2, cell, layer1, DBMath.MATID, search,
                pt1d, pt2d, null, pointsFound, false, null, false, reportInfo.ignoreCenterCuts);
        // Nothing found
        if (!pointsFound[0] && !pointsFound[1])
        {
            foundError = true;
        }
        return (foundError);
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
		boolean maytouch = DRC.mayTouch(tech, con, layer1, layer2);

		// special code if both polygons are manhattan
		double pd = 0;
		boolean overlap = false;
		if (isBox1 != null && isBox2 != null)
		{
			// manhattan
			double pdx = DBMath.round(Math.max(trueBox2.getMinX()-trueBox1.getMaxX(), trueBox1.getMinX()-trueBox2.getMaxX()));
			double pdy = DBMath.round(Math.max(trueBox2.getMinY()-trueBox1.getMaxY(), trueBox1.getMinY()-trueBox2.getMaxY()));
			pd = Math.max(pdx, pdy);
			if (DBMath.areEquals(pdx, 0) && DBMath.areEquals(pdy,0))
                pd = 0; // touching
			overlap = DBMath.isLessThan(pd, 0);
            if (maytouch)
			{
				// they are electrically connected: see if they touch
                if (checkMinDefects(cell, maytouch, geom1, poly1, trueBox1, layer2, geom2, poly2, trueBox2, layer2, cell))
                {
                    if (reportInfo.errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true;
                    errorFound = true;
                }
			}
			// crop out parts of any arc that is covered by an adjoining node
			trueBox1 = new Rectangle2D.Double(trueBox1.getMinX(), trueBox1.getMinY(), trueBox1.getWidth(), trueBox1.getHeight());
			trueBox2 = new Rectangle2D.Double(trueBox2.getMinX(), trueBox2.getMinY(), trueBox2.getWidth(), trueBox2.getHeight());
            // first to crop arcs since node cropping crops arcs too!
            boolean geom1IsNode = (geom1 instanceof NodeInst);
            boolean geom2IsNode = (geom2 instanceof NodeInst);

            if (!geom1IsNode) // arc -> crop it first
            {
                if (cropArcInst((ArcInst)geom1, layer1, trans1, trueBox1, overlap))
					return errorFound;
            }
            if (!geom2IsNode)
            {
                if (cropArcInst((ArcInst)geom2, layer2, trans2, trueBox2, overlap))
					return errorFound;
            }
            if (geom1IsNode)
			{
				if (cropNodeInst((NodeInst)geom1, globalIndex1, trans1,
				        layer2, net2, geom2, trueBox2))
					return errorFound;
//			} else
//			{
//				if (cropArcInst((ArcInst)geom1, layer1, trans1, trueBox1, overlap))
//					return errorFound;
			}
			if (geom2 instanceof NodeInst)
			{
				if (cropNodeInst((NodeInst)geom2, globalIndex2, trans2,
				        layer1, net1, geom1, trueBox1))
					return errorFound;
//			} else
//			{
//				if (cropArcInst((ArcInst)geom2, layer2, trans2, trueBox2, overlap))
//					return errorFound;
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
				double pdedge = DBMath.round(Math.min(
					Math.min(Math.min(Math.abs(lX1-lX2), Math.abs(lX1-hX2)), Math.min(Math.abs(hX1-lX2), Math.abs(hX1-hX2))),
					Math.min(Math.min(Math.abs(lY1-lY2), Math.abs(lY1-hY2)), Math.min(Math.abs(hY1-lY2), Math.abs(hY1-hY2)))));
				pd = Math.max(pd, pdedge);
			} else
			{
				pdx = DBMath.round(Math.max(lX2-hX1, lX1-hX2));
				pdy = DBMath.round(Math.max(lY2-hY1, lY1-hY2));

                if (DBMath.areEquals(pdx, 0) && DBMath.areEquals(pdy, 0))
					pd = 0; // they are touching!!
				else
				{
					// They are overlapping if pdx < 0 && pdy < 0
					pd = (Math.max(pdx, pdy));

                    if (pd < theRule.getValue(0) && DBMath.isGreaterThan(pd, 0))
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
			if (poly1.intersects(poly2))
                pd = 0;
            else
			{
				// find distance between polygons
				pd = poly1.separation(poly2);
			}
//            if (pd1 != pd)
            if (!DBMath.areEquals(pd1, pd))
                System.out.println("Wrong case in non-nonmanhattan, Quick.");
		}

		DRC.DRCErrorType errorType = DRC.DRCErrorType.SPACINGERROR;
        if (theRule.ruleType == DRCTemplate.DRCRuleType.SURROUND)
        {
            if (DBMath.isGreaterThan(pd, 0)) // layers don't overlap -> no condition to check
                return errorFound;
            pd = Math.abs(pd);
            errorType = DRC.DRCErrorType.SURROUNDERROR;
        }
		// see if the design rule is met
		if (!DBMath.isGreaterThan(theRule.getValue(0), pd)) // default case: SPACING   pd >= theRule.value1
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
            if (polyCoverByAnyVTLayer(cell, theRule, tech, new Poly[]{poly1, poly2}, new Layer[]{layer1, layer2},
                    new Geometric[]{geom1, geom2}, reportInfo.ignoreCenterCuts))
                return errorFound;

            // special cases if the layers are the same
            if (tech.sameLayer(layer1, layer2))
            {
                // special case: check for "notch"
                if (maytouch)
                {
                    // if they touch, it is acceptable
                    if (DBMath.isLessThanOrEqualTo(pd, 0))
                        return errorFound;

                    // see if the notch is filled
                    boolean newR = lookForCrossPolygons(geom1, poly1, geom2, poly2, layer1, cell, overlap);
//                    if (Job.LOCALDEBUGFLAG)
//                    {
//                        Point2D pt1 = new Point2D.Double();
//                        Point2D pt2 = new Point2D.Double();
//                        int intervening = findInterveningPoints(poly1, poly2, pt1, pt2, false);
//                        if (intervening == 0)
//                        {
//                            if (!newR)
//                            {
//                                System.out.println("DIfferent");
//                                lookForCrossPolygons(geom1, poly1, geom2, poly2, layer1, cell, overlap);
//
//                            }
//                            //return false;
//                        }
//                        boolean needBoth = true;
//                        if (intervening == 1) needBoth = false;
//
//                        if (lookForPoints(pt1, pt2, layer1, cell, needBoth))
//                        {
//                            if (!newR)
//                            {
//                                System.out.println("DIfferent");
//                                lookForPoints(pt1, pt2, layer1, cell, needBoth);
//                                lookForCrossPolygons(geom1, poly1, geom2, poly2, layer1, cell, overlap);
//                            //return false;
//                            }
//                        }
//
//                        boolean oldR = lookForPoints(pt1, pt2, layer1, cell, needBoth);
//                        if (oldR != newR)
//                                System.out.println("DIfferent 2");
//                    }
                    if (newR) return errorFound;

                    // look further if on the same net and diagonally separate (1 intervening point)
                    //if (net1 == net2 && intervening == 1) return false;
                    errorType = DRC.DRCErrorType.NOTCHERROR;
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

		DRC.createDRCErrorLogger(reportInfo, errorType, msg, cell, theRule.getValue(0), pd, theRule.ruleName,
            origPoly1, geom1, layer1, origPoly2, geom2, layer2);
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
        double worstInteractionDistance = reportInfo.worstInteractionDistance;
        Rectangle2D searchBounds = new Rectangle2D.Double(
			nodeBounds.getMinX() - worstInteractionDistance,
			nodeBounds.getMinY() - worstInteractionDistance,
			nodeBounds.getWidth() + worstInteractionDistance*2,
			nodeBounds.getHeight() + worstInteractionDistance*2);

		for(Iterator<RTBounds> it = cell.searchIterator(searchBounds); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();

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
//		boolean baseMulti = false;
        Technology.MultiCutData multiCutData = null;
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
			nodeInstPolyList = tech.getShapeOfNode(oNi, true, reportInfo.ignoreCenterCuts, null);
			convertPseudoLayers(oNi, nodeInstPolyList);
            multiCutData = tech.getMultiCutData(oNi);
//            baseMulti = tech.isMultiCutCase(oNi);
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
                    subCell, globalIndex, subTrans, multiCutData, false)) return true;
		}
		return false;
	}

	/*************************** QUICK DRC CACHE OF INSTANCE INTERACTIONS ***************************/

	/**
	 * Method to look for an interaction between instances "ni1" and "ni2".  If it is found,
	 * return TRUE.  If not found, add to the list and return FALSE.
	 */
	private boolean checkInteraction(NodeInst ni1, NodeInst n1Parent, NodeInst ni2, NodeInst n2Parent,
                                     NodeInst triggerNi, Rectangle2D searchBnd)
	{
        if (reportInfo.errorTypeSearch == DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return false;

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
//        dii.bnd = searchBnd;

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
//                thisII.bnd == dii.bnd)
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
		cp.netlist = netlist;
        if (cell.hasParameters()) {
            cp.cellParameterized = true;
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
		reportInfo.checkTimeStamp++;
		List<CheckProto> subCheckProtos = new ArrayList<CheckProto>();
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			if (!ni.isCellInstance()) continue;

			// ignore documentation icons
			if (ni.isIconOfParent()) continue;

			CheckProto cp = getCheckProto((Cell)np);
			if (cp.timeStamp != reportInfo.checkTimeStamp)
			{
				cp.timeStamp = reportInfo.checkTimeStamp;
				cp.instanceCount = 0;
				cp.nodesInCell = new ArrayList<CheckInst>();
				subCheckProtos.add(cp);
			}

			CheckInst ci = checkInsts.get(ni);
			ci.localIndex = cp.instanceCount++;
			cp.nodesInCell.add(ci);
		}

		// update the counts for this cell
        for (CheckProto cp : subCheckProtos)
		{
			cp.hierInstanceCount += cp.instanceCount;
            for (CheckInst ci : cp.nodesInCell)
			{
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
					subEnumeratedNets.put(net, new Integer(reportInfo.checkNetNumber++));
			}
			checkEnumerateNetworks(subCell, subCP, localIndex, subEnumeratedNets);
		}
	}

	/*********************************** QUICK DRC SUPPORT ***********************************/

    /**
     * Function to look for conditional layers that will 100% overlap with the given region.
     * @param poly
     * @param layer
     * @param cell
     * @param ignoreCenterCuts
     * @return true if conditional layers are found for the test points
     */
    private boolean searchForCondLayer(Geometric geom, Poly poly, Layer layer, Cell cell,
                                       boolean ignoreCenterCuts)
    {
        Rectangle2D polyBnd = poly.getBounds2D();
        double midPointX = (polyBnd.getMinX() + polyBnd.getMaxX())/2;
        double midPointY = (polyBnd.getMinY()+polyBnd.getMaxY())/2;
        Point2D pt1 = new Point2D.Double(midPointX, polyBnd.getMinY());
        Point2D pt2 = new Point2D.Double(midPointX, polyBnd.getMaxY());
        Point2D pt3 = new Point2D.Double(midPointX, midPointY);
        // compute bounds for searching inside the given polygon
		Rectangle2D bnd = new Rectangle2D.Double(DBMath.round(polyBnd.getMinX()- DRC.TINYDELTA),
                DBMath.round(polyBnd.getMinY()- DRC.TINYDELTA),
                DBMath.round(polyBnd.getWidth()+2* DRC.TINYDELTA), DBMath.round(polyBnd.getHeight()+2* DRC.TINYDELTA));
        // looking if points around the overlapping area are inside another region
        // to avoid the error
        boolean [] pointsFound = new boolean[3];
		pointsFound[0] = pointsFound[1] = pointsFound[2] = false;
        // Test first with a vertical line
        boolean found = DRC.lookForLayerCoverage(geom, poly, null, null, cell, layer, DBMath.MATID, bnd, pt1, pt2, pt3,
                pointsFound, true, null, false, ignoreCenterCuts);
        if (!found)
            return found; // no need of checking horizontal line if the vertical test was not positive
        pt1.setLocation(polyBnd.getMinX(), midPointY);
        pt2.setLocation(polyBnd.getMaxX(), midPointY);
        pointsFound[0] = pointsFound[1] = false;
        pointsFound[2] = true; // no testing pt3 again
        found = DRC.lookForLayerCoverage(geom, poly, null, null, cell, layer, DBMath.MATID, bnd, pt1, pt2, null,
                pointsFound, true, null, false, ignoreCenterCuts);
        return found;
    }

    /**
	 * Method to ensure that polygon "poly" on layer "layer" from object "geom" in
	 * technology "tech" meets minimum width rules.  If it is too narrow, other layers
	 * in the vicinity are checked to be sure it is indeed an error.  Returns true
	 * if an error is found.
	 */
    private boolean checkMinWidth(Geometric geom, Layer layer, Poly poly, boolean onlyOne)
    {
        DRCTemplate minWidthRule = DRC.getMinValue(layer, DRCTemplate.DRCRuleType.MINWID);
        boolean errorDefault = false;

        // Only if there is one default size
        if (minWidthRule != null)
        {
            errorDefault = DRC.checkMinWidthInternal(geom, layer, poly, onlyOne, minWidthRule, false, null, reportInfo);
            if (!errorDefault) return false; // the default condition is the valid one.
        }

        DRCTemplate minWidthRuleCond = DRC.getMinValue(layer, DRCTemplate.DRCRuleType.MINWIDCOND);
        // No appropiate overlapping condition
        if (minWidthRuleCond == null || !minWidthRuleCond.condition.startsWith("overlap("))
        {
            // Now the error is reporte. Not very efficient
            if (errorDefault)
                DRC.checkMinWidthInternal(geom, layer, poly, onlyOne, minWidthRule, true, null, reportInfo);
            return errorDefault;
        }

        // checking if geometry complains with overlap condition
        // Skipping "overlap"
        String[] layers = TextUtils.parseString(minWidthRuleCond.condition.substring(7), "(,)");
        boolean found = false;

        for (String la : layers)
        {
            Layer l = layer.getTechnology().findLayer(la);
            found = searchForCondLayer(geom, poly, l, geom.getParent(), reportInfo.ignoreCenterCuts);
            if (!found)
                break; // no need to check the next layer
        }
        // If condition is met then the new rule applied.
        if (found)
           errorDefault = DRC.checkMinWidthInternal(geom, layer, poly, onlyOne, minWidthRuleCond, true, null, reportInfo);
        else if (errorDefault) // report the errors here in case of default values
            DRC.checkMinWidthInternal(geom, layer, poly, onlyOne, minWidthRule, true, null, reportInfo);
        return errorDefault;
    }

    //	private int checkMinAreaLayer(GeometryHandler merge, Cell cell, Layer layer, boolean addError,
//                                  HashMap<Layer, Layer> minAreaLayerMapDone, HashMap<Layer, Layer> enclosedAreaLayerMapDone)
//	{
//		int errorFound = 0;
//		DRCTemplate minAreaRule = minAreaLayerMap.get(layer);
//		DRCTemplate encloseAreaRule = enclosedAreaLayerMap.get(layer);
//        DRCTemplate slotSizeRule = slotSizeLayerMap.get(layer);
//
//		// Layer doesn't have min area nor slot size
//		if (minAreaRule == null && encloseAreaRule == null && slotSizeRule == null) return 0;
//
//		Collection<PolyBase> set = merge.getObjects(layer, false, true);
//
//        if (set == null) return 0; // layer is not present in this cell
//
//        boolean minAreaDone = true;
//        boolean enclosedAreaDone = true;
//
//        for (PolyBase obj : set)
//		{
//			if (obj == null) throw new Error("wrong condition in Quick.checkMinArea()");
//
//			List<PolyBase> list = obj.getSortedLoops();
//
//			// Order depends on area comparison done. First element is the smallest.
//			// and depending on # polygons it could be minArea or encloseArea
//			DRCTemplate evenRule = (list.size()%2==0) ? encloseAreaRule : minAreaRule;
//			DRCTemplate oddRule = (evenRule == minAreaRule) ? encloseAreaRule : minAreaRule;
//			// Looping over simple polygons. Possible problems with disconnected elements
//			// polyArray.length = Maximum number of distintic loops
//            int i = 0;
//            for (PolyBase listObj : list)
//			{
//				double area = listObj.getArea();
//				DRCTemplate minRule = (i%2 == 0) ? evenRule : oddRule;
//                PolyBase simplePn = listObj;
//                i++;
//
//                // Check slot size when area is checked
//                if (minRule == minAreaRule && slotSizeRule != null)
//                {
//                    double length = listObj.getMaxLength();
//
//                    if (!DBMath.isGreaterThan(length, slotSizeRule.getValue(0))) continue;
//                    if (addError)
//                    reportError(DRC.DRCErrorType.SLOTSIZEERROR, null, cell, slotSizeRule.getValue(0), length, slotSizeRule.ruleName,
//						simplePn, null, layer, null, null, null);
//				    errorFound++;
//                }
//
//				if (minRule == null) continue;
//
//				// isGreaterThan doesn't consider equals condition therefore negate condition is used
//				if (!DBMath.isGreaterThan(minRule.getValue(0), area)) continue;
//
//				errorFound++;
//				DRC.DRCErrorType errorType = (minRule == minAreaRule) ? DRC.DRCErrorType.MINAREAERROR : DRC.DRCErrorType.ENCLOSEDAREAERROR;
//                if (errorType == DRC.DRCErrorType.MINAREAERROR) minAreaDone = false;
//                else if (errorType == DRC.DRCErrorType.ENCLOSEDAREAERROR) enclosedAreaDone = false;
//                if (addError)
//				reportError(errorType, null, cell, minRule.getValue(0), area, minRule.ruleName,
//						simplePn, null, layer, null, null, null);
//			}
//		}
//        if (minAreaDone && minAreaLayerMapDone != null)
//            minAreaLayerMapDone.put(layer, layer);
//        if (enclosedAreaDone && enclosedAreaLayerMapDone != null)
//            enclosedAreaLayerMapDone.put(layer, layer);
//		return errorFound;
//
//	}


    private void traversePolyTree(Layer layer, PolyBase.PolyBaseTree obj, int level, DRCTemplate minAreaRule,
                                         DRCTemplate encloseAreaRule, DRCTemplate spacingRule, Cell cell, GenMath.MutableInteger count)
    {
        List<PolyBase.PolyBaseTree> sons = obj.getSons();
        for (PolyBase.PolyBaseTree son : sons)
        {
            traversePolyTree(layer, son, level+1, minAreaRule, encloseAreaRule, spacingRule, cell, count);
        }
        boolean minAreaCheck = level%2 == 0;
        boolean checkMin = false, checkNotch = false;
        DRC.DRCErrorType errorType = DRC.DRCErrorType.MINAREAERROR;
        double minVal = 0;
        String ruleName = "";

        if (minAreaCheck)
        {
            if (minAreaRule == null) return; // no rule
            minVal = minAreaRule.getValue(0);
            ruleName = minAreaRule.ruleName;
            checkMin = true;
        }
        else
        {
            // odd level checks enclose area and holes (spacing rule)
            errorType = DRC.DRCErrorType.ENCLOSEDAREAERROR;
            if (encloseAreaRule != null)
            {
                minVal = encloseAreaRule.getValue(0);
                ruleName = encloseAreaRule.ruleName;
                checkMin = true;
            }
            checkNotch = (spacingRule != null);
        }
        PolyBase poly = obj.getPoly();

        if (checkMin)
        {
            double area = poly.getArea();
            // isGreaterThan doesn't consider equals condition therefore negate condition is used
            if (!DBMath.isGreaterThan(minVal, area)) return; // larger than the min value
            count.increment();
            DRC.createDRCErrorLogger(reportInfo, errorType, null, cell, minVal, area, ruleName,
                        poly, null, layer, null, null, null);
        }
        if (checkNotch)
        {
            // Notches are calculated using the bounding box of the polygon -> this is an approximation
            Rectangle2D bnd = poly.getBounds2D();
            if (bnd.getWidth() < spacingRule.getValue(0))
            {
                count.increment();
                DRC.createDRCErrorLogger(reportInfo, DRC.DRCErrorType.NOTCHERROR, "(X axis)", cell,
                    spacingRule.getValue(0), bnd.getWidth(), spacingRule.ruleName, poly, null, layer, null, null, layer);
            }
            if (bnd.getHeight() < spacingRule.getValue(1))
            {
                count.increment();
                DRC.createDRCErrorLogger(reportInfo, DRC.DRCErrorType.NOTCHERROR, "(Y axis)", cell,
                    spacingRule.getValue(1), bnd.getHeight(), spacingRule.ruleName, poly, null, layer, null, null, layer);
            }
        }
    }

    private int checkMinAreaLayerWithTree(GeometryHandler merge, Cell cell, Layer layer)
	{
		DRCTemplate minAreaRule = minAreaLayerMap.get(layer);
		DRCTemplate encloseAreaRule = enclosedAreaLayerMap.get(layer);
        DRCTemplate spacingRule = spacingLayerMap.get(layer);

        // Layer doesn't have min areae
		if (minAreaRule == null && encloseAreaRule == null && spacingRule == null) return 0;

        Collection<PolyBase.PolyBaseTree> trees = merge.getTreeObjects(layer);

        GenMath.MutableInteger errorFound = new GenMath.MutableInteger(0);

        for (PolyBase.PolyBaseTree obj : trees)
		{
            traversePolyTree(layer, obj, 0, minAreaRule, encloseAreaRule, spacingRule, cell, errorFound);
        }
		return errorFound.intValue();
	}

    private int checkMinAreaSlow(Cell cell)
	{
		// Nothing to check
		if (minAreaLayerMap.isEmpty() && enclosedAreaLayerMap.isEmpty() && spacingLayerMap.isEmpty())
			return 0;

        // remember number of errors before the min area checking
        int errorFound = reportInfo.errorLogger.getNumErrors();

		// Get merged areas.
        DRC.DRCCheckMinArea algoType = dp.minAreaAlgoOption;
        HierarchyEnumerator.Visitor quickArea = null;

        switch(algoType)
        {
            case AREA_BASIC:
                    quickArea = new QuickAreaEnumeratorSlow(mergeMode);
                break;
            case AREA_LOCAL:
                    quickArea = new QuickAreaEnumeratorLocal(mergeMode);
                break;
        }
        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, quickArea);

        errorFound = reportInfo.errorLogger.getNumErrors() - errorFound;

        return errorFound;
	}

    /**
	 * Method to ensure that polygon "poly" on layer "layer" from object "geom" in
	 * technology "tech" meets minimum area rules. Returns true
	 * if an error is found.
	 */
//	private int checkMinArea(Cell cell)
//	{
//		CheckProto cp = getCheckProto(cell);
//		int errorFound = 0;
//
//		// Nothing to check
//		if (minAreaLayerMap.isEmpty() && enclosedAreaLayerMap.isEmpty() && spacingLayerMap.isEmpty())
//			return 0;
//
//		// Select/well regions
//		GeometryHandler	selectMerge = GeometryHandler.createGeometryHandler(mergeMode, 0);
//		HashMap<NodeInst,NodeInst> notExportedNodes = new HashMap<NodeInst,NodeInst>();
//		HashMap<NodeInst,NodeInst> checkedNodes = new HashMap<NodeInst,NodeInst>();
//
//        // remember number of errors before the min area checking
//        errorFound = errorLogger.getNumErrors();
//
//		// Get merged areas. Only valid for layers that have connections (metals/polys). No valid for NP/PP rule
//        QuickAreaEnumerator quickArea = new QuickAreaEnumerator(cp.netlist, selectMerge, notExportedNodes, checkedNodes,
//                    mergeMode);
//        quickArea.setPreProcessFlag(false);
//        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, quickArea);
//
//        // Job aborted
//        if (job != null && job.checkAbort()) return 0;
//
//        errorFound = errorLogger.getNumErrors() - errorFound;
//
//		// Checking nodes not exported down in the hierarchy. Probably good enough not to collect networks first
//        // Maybe it would be better to check notExportNodes with respect to local cells. No need of checking
//        // the entire hierarchy again!
//		quickArea = new QuickAreaEnumerator(selectMerge, notExportedNodes, checkedNodes, mergeMode);
//		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, quickArea);
//		// Non exported nodes
////		for(Iterator it = quickArea.mainMerge.getKeyIterator(); it.hasNext(); )
////		{
////			Layer layer = (Layer)it.next();
////			boolean localError = checkMinAreaLayer(quickArea.mainMerge, cell, layer);
////			if (!errorFound) errorFound = localError;
////		}
//        selectMerge.postProcess(true);
//
//		// Special cases for select areas. You can't evaluate based on networks
//        for (Layer layer : selectMerge.getKeySet())
//		{
////			errorFound +=  checkMinAreaLayer(selectMerge, cell, layer, true, null, null);
//            errorFound += checkMinAreaLayerWithTree(selectMerge, cell, layer, true, null, null);
//        }
//
//		return errorFound;
//	}
//    private int checkMinAreaAgain(Cell cell)
//	{
//		CheckProto cp = getCheckProto(cell);
//		int errorFound = 0;
//
//		// Nothing to check
//		if (minAreaLayerMap.isEmpty() && enclosedAreaLayerMap.isEmpty())
//			return 0;
//
//		// Select/well regions
//		GeometryHandler	selectMerge = GeometryHandler.createGeometryHandler(mergeMode, 0); //new PolyQTree(cell.getBounds());
//		HashMap<NodeInst,Geometric> notExportedNodes = new HashMap<NodeInst,Geometric>();
//		HashMap<NodeInst,NodeInst> checkedNodes = new HashMap<NodeInst,NodeInst>();
//
//        // remember number of errors before the min area checking
//        errorFound = errorLogger.getNumErrors();
//
//		// Get merged areas. Only valid for layers that have connections (metals/polys). No valid for NP/PP rule
//        QuickAreaEnumeratorAgain quickArea = new QuickAreaEnumeratorAgain(cp.netlist, selectMerge, notExportedNodes, checkedNodes,
//                    mergeMode);
//        quickArea.setPreProcessFlag(false);
//        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, quickArea);
//
//        // Job aborted
//        if (job != null && job.checkAbort()) return 0;
//
//        errorFound = errorLogger.getNumErrors() - errorFound;
//
//		// Checking nodes not exported down in the hierarchy. Probably good enough not to collect networks first
//        // Maybe it would be better to check notExportNodes with respect to local cells. No need of checking
//        // the entire hierarchy again!
//		quickArea = new
// (selectMerge, notExportedNodes, checkedNodes, mergeMode);
//		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, quickArea);
//        selectMerge.postProcess(true);
//
//		// Special cases for select areas. You can't evaluate based on networks
//        for (Layer layer : selectMerge.getKeySet())
//		{
//			errorFound +=  checkMinAreaLayer(selectMerge, cell, layer, true, null, null);
//		}
//
//		return errorFound;
//	}

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
				double xPlus = xc + DRC.TINYDELTA;
				double yPlus = yc + DRC.TINYDELTA;
				pt1.setLocation(xPlus, yPlus);
				pt2.setLocation(xPlus, yPlus);
				if ((xPlus < isBox1.getMinX() || xPlus > isBox1.getMaxX() || yPlus < isBox1.getMinY() || yPlus > isBox1.getMaxY()) &&
					(xPlus < isBox2.getMinX() || xPlus > isBox2.getMaxX() || yPlus < isBox2.getMinY() || yPlus > isBox2.getMaxY())) return 1;
				pt1.setLocation(xc + DRC.TINYDELTA, yc - DRC.TINYDELTA);
				pt2.setLocation(xc - DRC.TINYDELTA, yc + DRC.TINYDELTA);
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
	private boolean lookForCrossPolygons(Geometric geo1, Poly poly1, Geometric geo2, Poly poly2, Layer layer,
	                                     Cell cell, boolean overlap)
	{
		Point2D pt1 = new Point2D.Double();
		Point2D pt2 = new Point2D.Double();
		findInterveningPoints(poly1, poly2, pt1, pt2, true);

		// compute bounds for searching inside cells
		double flx = Math.min(pt1.getX(), pt2.getX());   double fhx = Math.max(pt1.getX(), pt2.getX());
		double fly = Math.min(pt1.getY(), pt2.getY());   double fhy = Math.max(pt1.getY(), pt2.getY());
		Rectangle2D bounds = new Rectangle2D.Double(DBMath.round(flx- DRC.TINYDELTA), DBMath.round(fly- DRC.TINYDELTA),
                DBMath.round(fhx-flx+2* DRC.TINYDELTA), DBMath.round(fhy-fly+2* DRC.TINYDELTA));
            // Adding delta otherwise it won't consider points along edges.
        // Mind bounding boxes could have zero width or height

		// search the cell for geometry that fills the notch
		boolean [] pointsFound = new boolean[2];
		pointsFound[0] = pointsFound[1] = false;
		boolean allFound = DRC.lookForLayerCoverage(geo1, poly1, geo2, poly2, cell, layer, DBMath.MATID, bounds,
		        pt1, pt2, null, pointsFound, overlap, null, false, reportInfo.ignoreCenterCuts);

		return allFound;
	}

    /**
	 * Method to examine cell "cell" in the area (lx<=X<=hx, ly<=Y<=hy) for objects
	 * on layer "layer".  Apply transformation "moreTrans" to the objects.  If polygons are
	 * found at (xf1,yf1) or (xf2,yf2) or (xf3,yf3) then sets "p1found/p2found/p3found" to 1.
	 * If all locations are found, returns true.
	 */
	private boolean lookForLayerWithPoints(Geometric geo1, Poly poly1, Geometric geo2, Poly poly2, Cell cell,
                                           Layer layer, AffineTransform moreTrans, Rectangle2D bounds,
                                           Point2D[] pts, boolean[] pointsFound, int length,
                                           boolean overlap)
	{
        assert (pts.length == pointsFound.length);

        int j;
        Rectangle2D newBounds = new Rectangle2D.Double();  // Sept 30

		for(Iterator<RTBounds> it = cell.searchIterator(bounds); it.hasNext(); )
		{
			RTBounds g = it.next();

            // You can't skip the same geometry otherwise layers in the same Geometric won't
            // be tested.
//			if (ignoreSameGeometry && (g == geo1 || g == geo2))
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
					if (lookForLayerWithPoints(geo1, poly1, geo2, poly2, (Cell)ni.getProto(), layer, trans, newBounds,
                            pts, pointsFound, length, overlap))
							return true;
					continue;
				}
				AffineTransform bound = ni.rotateOut();
				bound.preConcatenate(moreTrans);
				Technology tech = ni.getProto().getTechnology();
                // I have to ask for electrical layers otherwise it will retrieve one polygon for polysilicon
                // and poly.polySame(poly1) will never be true. CONTRADICTION!
				Poly [] layerLookPolyList = tech.getShapeOfNode(ni, false, reportInfo.ignoreCenterCuts, null);
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

                    for (j = 0; j < length; j++)
                    {
                        if (pointsFound[j]) continue; // point covered
                        if (poly.isInside(pts[j])) pointsFound[j] = true;
                    }
					for (j = 0; j < length && pointsFound[j]; j++);
					if (j == length) return true;
                    // No need of checking rest of the layers
                    break; // assuming only 1 polygon per layer (non-electrical)
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

                    for (j = 0; j < length; j++)
                    {
                        if (pointsFound[j]) continue; // point covered
                        if (poly.isInside(pts[j])) pointsFound[j] = true;
                    }
					for (j = 0; j < length && pointsFound[j]; j++);
					if (j == length) return true;
                    // No need of checking rest of the layers
                    break;
				}
			}
		}
		return false;
	}

    /****************************** Cut Rule Functions ***************************/
    private boolean checkCutSizes(NodeProto np, Geometric geom, Layer layer, Poly poly,
                                  Geometric nGeom, Layer nLayer, Poly nPoly, Cell topCell)
    {
        // None of them is cut
        if (!(np instanceof PrimitiveNode) || layer != nLayer ||
                !layer.getFunction().isContact() || !nLayer.getFunction().isContact())
            return false;

        PrimitiveNode nty = (PrimitiveNode)np;
        Technology.NodeLayer cutLayer = nty.findMulticut();
        if (cutLayer == null) return false; // no cut layer
        double cutSizeX = cutLayer.getMulticutSizeX();
        double cutSizeY = cutLayer.getMulticutSizeY();
//        double[] specValues = nty.getSpecialValues();
//        // 0 and 1 hold CUTSIZE
//        if (specValues == null) return false; // no values

        Rectangle2D box1 = poly.getBounds2D();
        Rectangle2D box2 = nPoly.getBounds2D();

        double pdx = DBMath.round(Math.max(box2.getMinX()-box1.getMaxX(), box1.getMinX()-box2.getMaxX()));
        double pdy = DBMath.round(Math.max(box2.getMinY()-box1.getMaxY(), box1.getMinY()-box2.getMaxY()));
        double pd = Math.max(pdx, pdy);

        if (DBMath.areEquals(pdx, 0) && DBMath.areEquals(pdy, 0))
            pd = 0; // touching

        // They have to overlap
        if (DBMath.isGreaterThan(pd, 0))
            return false;
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
        if (DBMath.isGreaterThan(rect.getWidth(), cutSizeX))
        {
            DRC.createDRCErrorLogger(reportInfo, DRC.DRCErrorType.CUTERROR, "along X", topCell, cutSizeX, rect.getWidth(),
                    ruleName, new Poly(rect), null, layer, null, null, nLayer);
            foundError = true;

        }
        if (DBMath.isGreaterThan(rect.getHeight(), cutSizeY))
        {
            DRC.createDRCErrorLogger(reportInfo, DRC.DRCErrorType.CUTERROR, "along Y", topCell, cutSizeY, rect.getHeight(),
                    ruleName, new Poly(rect), null, layer, null, null, layer);
            foundError = true;

        }
        return foundError;
    }

    /****************************** Extension Rule Functions ***************************/
    private boolean checkExtensionGateRule(Geometric geom, Layer layer, Poly poly, Layer nLayer,
                                           Poly nPoly, Netlist netlist)
	{
        if(dp.ignoreExtensionRuleChecking) return false;

        return false;
//        DRCTemplate extensionRule = DRC.getExtensionRule(layer, nLayer, true);
        // Checking extension, it could be slow
//        if (extensionRule == null) return false;
//
//        if (!layer.getName().equals(extensionRule.name1) ||
//            !nLayer.getName().equals(extensionRule.name2))
//            return false; // no match in names


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
     * @return Array of Geometric objects
     */
//    private Poly[] getShapeOfGeometric(Geometric geom, Layer layer)
//    {
//        Poly [] primPolyList = null;
//        Layer.Function.Set drcLayers = new Layer.Function.Set(layer.getFunction());
//
//        if (geom instanceof NodeInst)
//        {
//            NodeInst ni = (NodeInst)geom;
//            NodeProto np = ni.getProto();
//            Technology tech = np.getTechnology();
//            primPolyList = tech.getShapeOfNode(ni, true, ignoreCenterCuts, drcLayers);
//        }
//        else if (geom instanceof ArcInst)
//        {
//            ArcInst ai = (ArcInst)geom;
//            primPolyList = ai.getProto().getTechnology().getShapeOfArc(ai, drcLayers);
//        }
//        return primPolyList;
//    }

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
//	private boolean checkExtensionRule(Geometric geom, Layer layer, Poly poly, Cell cell)
//	{
//        return false;
////        if(DRC.isIgnoreExtensionRuleChecking()) return false;
////
////		Rectangle2D polyBnd = poly.getBounds2D();
////        Set layerSet = new HashSet(); // to avoid repeated elements
////
////        // Search among all possible neighbors and to get the layers
////        for(Iterator sIt = cell.searchIterator(polyBnd); sIt.hasNext();)
////        {
////            Geometric g = (Geometric)sIt.next();
////	        if (g == geom) continue;
////			if ((g instanceof ArcInst))
////            {
////                ArcProto ap = ((ArcInst)g).getProto();
////                for (int i = 0; i < ap.getLayers().length; i++)
////                    layerSet.add(pa.getLayers()[i].getLayer());
////            }
////            else
////            {
////                NodeInst ni = (NodeInst)g;
////                NodeProto np = ni.getProto();
////                if (NodeInst.isSkipSizeInPalette(ni)) continue; // Nov 4;
////                if (np instanceof Cell)
////                {
////                    System.out.println("Not implemented in checkExtensionRule");
////                }
////                else
////                {
////                    if (np instanceof PrimitiveNode)
////                    {
////                        PrimitiveNode pn = (PrimitiveNode)np;
////                        for (int i = 0; i < pn.getLayers().length; i++)
////                            layerSet.add(pn.getLayers()[i].getLayer());
////                    }
////                    else
////                        System.out.println("When do we have this case");
////                }
////            }
////        }
////        boolean error = false;
////
////        Object[] layerArray = layerSet.toArray();
////        for (int i = 0; i < layerArray.length; i++)
////        {
////            Layer nLayer = (Layer)layerArray[i];
////            DRCTemplate extensionRule = DRC.getExtensionRule(layer, nLayer, techMode, false);
////            // Checking extension, it could be slow
////            if (extensionRule == null) continue;
////
////            List list = extensionRule.getNodesInRule();
////            // Layers order is revelant, the first element is the one to check for the extension in the second one
////            if (list != null && list.size() == 2)
////            {
////                // Not the correct order
////                if (list.get(0) != layer || list.get(1) != nLayer) continue;
////            }
////
////            Area baseArea = new Area(poly);
////            Area overlapArea = new Area(); // start empty
////            Area extensionArea = new Area(); // start empty
////
////            checkExtensionOverlapRule(geom, nLayer, cell, baseArea, extensionArea, overlapArea, polyBnd);
////
////            // get the area outside nLayer
////            if (!extensionArea.isEmpty())
////            {
////                List polyList = PolyBase.getPointsInArea(extensionArea, layer, true, true, null);
////
////                for (Iterator it = polyList.iterator(); it.hasNext(); )
////                {
////                    PolyBase nPoly = (PolyBase)it.next();
////                    Rectangle2D rect = nPoly.getBounds2D();
////                    int dir = 0; // X  assume the intersection edge is Y oriented
////                    // Determine how they touch. This is to get the perpendicular direction to the common edge.
////                    if ((rect.getMaxY() == poly.getBounds2D().getMinY()) ||
////                        (rect.getMinY() == poly.getBounds2D().getMaxY()) )
////                        dir = 1; // Y
////
////                    // not easy to determine along which axis they are touching
////                    if ((dir == 1 && rect.getHeight() < extensionRule.value) || (dir == 0 && rect.getWidth() < extensionRule.value))
////                    {
////                        reportError(LAYERDRCErrorType.SURROUNDERROR, "No enough extension, ", cell, extensionRule.value, -1, extensionRule.ruleName,
////                                nPoly, geom, layer, null, null, nLayer);
////                        error = true;
////                    }
////                }
////            }
////
////            // There is overlap
////            if (!overlapArea.isEmpty())
////            {
////                List polyList = PolyBase.getPointsInArea(overlapArea, layer, true, true, null);
////
////                for (Iterator it = polyList.iterator(); it.hasNext(); )
////                {
////                    PolyBase nPoly = (PolyBase)it.next();
////                    Rectangle2D rect = nPoly.getBounds2D();
////                    // not easy to determine along which axis they are touching
////                    if (rect.getHeight() < extensionRule.value || rect.getWidth() < extensionRule.value)
////                    {
////                        reportError(LAYERDRCErrorType.SURROUNDERROR, "No enough overlap, ", cell, extensionRule.value, -1, extensionRule.ruleName,
////                                nPoly, geom, layer, null, null, nLayer);
////                        error = true;
////                    }
////                }
////            }
////        }
////		return (error);
//	}

    /**
     *
     * @param geom
     * @param cell
     * @param extensionArea contains information about poly area outside of nLayer
     * @param overlapArea
     * @param polyBnd
     */
//    private void checkExtensionOverlapRule(Geometric geom, Layer layer, Cell cell, Area baseArea,
//                                              Area extensionArea, Area overlapArea,
//                                              Rectangle2D polyBnd)
//    {
//        for(Iterator<RTBounds> sIt = cell.searchIterator(polyBnd); sIt.hasNext(); )
//		{
//			Geometric g = (Geometric)sIt.next();
//	        if (g == geom) continue;
//			if ((g instanceof NodeInst))
//            {
//                NodeInst ni = (NodeInst)g;
//                NodeProto np = ni.getProto();
//                if (NodeInst.isSpecialNode(ni)) continue; // Nov 4;
//                if (ni.isCellInstance())
//                {
//                    AffineTransform cellDownTrans = ni.transformIn();
//                    AffineTransform	cellUpTrans = ni.transformOut();
//                    DBMath.transformRect(polyBnd, cellDownTrans);
//                    extensionArea.transform(cellDownTrans);
//                    overlapArea.transform(cellDownTrans);
//                    checkExtensionOverlapRule(geom, layer, (Cell)np, baseArea, extensionArea, overlapArea, polyBnd);
//                    DBMath.transformRect(polyBnd, cellUpTrans);
//                    extensionArea.transform(cellUpTrans);
//                    overlapArea.transform(cellUpTrans);
//                    continue;
//                }
//            }
//
//            Poly [] primPolyList = getShapeOfGeometric(g, layer);
//            int tot = primPolyList.length;
//            for(int j=0; j<tot; j++)
//            {
//                Poly nPoly = primPolyList[j];
//
//                // Checking if are covered by select is surrounded by minOverlapRule
//                Area nPolyArea = new Area(nPoly);
//                Area interPolyArea = (Area)nPolyArea.clone();
//                interPolyArea.intersect(baseArea);
//                overlapArea.add(interPolyArea);
//                Area extenPolyArea = (Area)nPolyArea.clone();
//                extenPolyArea.subtract(interPolyArea);
//                extensionArea.add(extenPolyArea);
//            }
//		}
//    }

    /**
     * Method to check if extension rules are met
     * @param geom
     * @param layer
     * @param poly
     * @param cell
     * @return  true if there are not met
     */
    private boolean checkExtensionRules(Geometric geom, Layer layer, Poly poly, Cell cell)
    {
        if(dp.ignoreExtensionRuleChecking) return false;

        List<DRCTemplate> rules = DRC.getRules(layer, DRCTemplate.DRCRuleType.SURROUND);

        //A              AB               B
        // +----------------------------+
        // |                            |
        //A|                            | B
        //D|                            | C
        // |                            |
        // |                            |
        // +----------------------------+
        //D              DC               C
        Point2D[] basePts = new Point2D[4];
//        boolean[] baseFound = new boolean[4];  // all false at the beginning
        // NOTE: Enough to find a point matching 1 rule to be OK.
        Rectangle2D polyBnd = poly.getBounds2D();
        basePts[0] = new Point2D.Double(polyBnd.getMinX(), polyBnd.getMinY());
        basePts[1] = new Point2D.Double(polyBnd.getMinX(), polyBnd.getMaxY());
        basePts[2] = new Point2D.Double(polyBnd.getMaxX(), polyBnd.getMaxY());
        basePts[3] = new Point2D.Double(polyBnd.getMaxX(), polyBnd.getMinY());

        for (DRCTemplate rule : rules)
        {
            if (rule.nodeName != null) continue; // ignore these ones, only use to resize primitives
            if (rule.condition == null) continue; // must be conditional
            if (!rule.name1.equals(layer.getName())) continue; // it has to be the first name
            String[] layersS = TextUtils.parseString(rule.condition.substring(2), "(,)"); // 2 to skip "or"
            Layer[] layers = new Layer[layersS.length];
            for (int i = 0; i < layersS.length; i++)
            {
                layers[i] = layer.getTechnology().findLayer(layersS[i]);
            }

            for (int j = 0; j < 4; j++)
            {
                boolean[] basicFound = new boolean[3];
                Point2D[] basicPts = new Point2D[3];
                basicFound[0] = false;
                basicPts[0] = basePts[j];

                Rectangle2D basicBnd = new Rectangle2D.Double(DBMath.round(basicPts[0].getX()- DRC.TINYDELTA),
                                DBMath.round(basicPts[0].getY()- DRC.TINYDELTA),
                                2*(DRC.TINYDELTA), 2*(DRC.TINYDELTA));
                boolean f = false;
                for (int i = 0; i < layers.length; i++)
                {
                    f = lookForLayerWithPoints(geom, poly, null, null, cell, layers[i], DBMath.MATID, basicBnd,
                            basicPts, basicFound, 1, true);
                    if (f)
                        break; // found
                }
                if (!f)
                    continue; // no point in layers found

                // now testing the rule
                basicFound[0] = basicFound[1] = basicFound[2] = false;
                double cos45 = 1.0/Math.sqrt(2);
                double xValue = rule.getValue(0);  // on the diagonal, 45 degrees
                double yValue = rule.getValue(1);
                double x45Value = xValue * cos45;  // on the diagonal, 45 degrees
                double y45Value = yValue * cos45;
                Point2D basicBndPoint = null;

                // Sample 3 points per vertex to detect corners.
                switch (j)
                {
                    case 0:
                        // Diagonal
                        basicPts[0] = new Point2D.Double(polyBnd.getMinX()-x45Value, polyBnd.getMinY()-y45Value);
                        // left
                        basicPts[1] = new Point2D.Double(polyBnd.getMinX()-xValue, polyBnd.getMinY());
                        // bottom
                        basicPts[2] = new Point2D.Double(polyBnd.getMinX(), polyBnd.getMinY()-yValue);
                        basicBndPoint = new Point2D.Double(polyBnd.getMinX()-xValue, polyBnd.getMinY()-yValue);
                        break;
                    case 1:
                        basicPts[0] = new Point2D.Double(polyBnd.getMinX()-x45Value, polyBnd.getMaxY()+y45Value);
                        basicPts[1] = new Point2D.Double(polyBnd.getMinX()-xValue, polyBnd.getMaxY());
                        basicPts[2] = new Point2D.Double(polyBnd.getMinX(), polyBnd.getMaxY()+yValue);
                        basicBndPoint = basicPts[1];
                        break;
                    case 2:
                        basicPts[0] = new Point2D.Double(polyBnd.getMaxX()+x45Value, polyBnd.getMaxY()+y45Value);
                        basicPts[1] = new Point2D.Double(polyBnd.getMaxX()+xValue, polyBnd.getMaxY());
                        basicPts[2] = new Point2D.Double(polyBnd.getMaxX(), polyBnd.getMaxY()+yValue);
                        basicBndPoint = new Point2D.Double(polyBnd.getMaxX(), polyBnd.getMaxY());
                        break;
                    case 3:
                        basicPts[0] = new Point2D.Double(polyBnd.getMaxX()+x45Value, polyBnd.getMinY()-y45Value);
                        basicPts[1] = new Point2D.Double(polyBnd.getMaxX()+xValue, polyBnd.getMinY());
                        basicPts[2] = new Point2D.Double(polyBnd.getMaxX(), polyBnd.getMinY()-yValue);
                        basicBndPoint = basicPts[2];
                        break;
                }
                basicBnd = new Rectangle2D.Double(DBMath.round(basicBndPoint.getX()- DRC.TINYDELTA),
                                DBMath.round(basicBndPoint.getY()- DRC.TINYDELTA),
                                DBMath.round(xValue+2*(DRC.TINYDELTA)), DBMath.round(yValue+2*(DRC.TINYDELTA)));

                for (int i = 0; i < layers.length; i++)
                {
                    f = lookForLayerWithPoints(geom, poly, null, null, cell, layers[i], DBMath.MATID, basicBnd,
                            basicPts, basicFound, 3, true);
                    if (f)
                        break; // found
                }

                if (!f)
                {
                    DRC.createDRCErrorLogger(reportInfo, DRC.DRCErrorType.LAYERSURROUNDERROR,
                        "Not enough surround of " + rule.condition + ", ",
                        geom.getParent(), rule.getValue(0), -1, rule.ruleName, poly, geom, layer, null, null, null);
                    if (reportInfo.errorTypeSearch != DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE) return true; // no need of checking other combinations
                    break; // with next rule
                }
            }
        }
        return false;
    }

    /****************************** Select Over Polysilicon Functions ***************************/
	/**
	 * Method to check if non-transistor polysilicons are completed covered by N++/P++ regions
	 */
//	private boolean checkSelectOverPolysilicon(Geometric geom, Layer layer, Poly poly, Cell cell)
//	{
//        if(DRC.isIgnoreExtensionRuleChecking()) return false;
//
//		if (!layer.getFunction().isPoly()) return false;
//		// One layer must be select and other polysilicon. They are not connected
//
//		DRCTemplate minOverlapRule = DRC.getMinValue(layer, DRCTemplate.DRCRuleType.EXTENSION);
//		if (minOverlapRule == null) return false;
//        double minOverlapValue = minOverlapRule.getValue(0);
//		Rectangle2D polyBnd = poly.getBounds2D();
//        Area polyArea = new Area(poly);
//        boolean found = polyArea.isEmpty();
//		List<Point2D> checkExtraPoints = new ArrayList<Point2D>();
//
//        found = checkThisCellExtensionRule(geom, layer, poly, null, cell, polyArea, polyBnd, minOverlapRule,
//                found, checkExtraPoints);
//
//		// error if the merged area doesn't contain 100% the search area.
//		if (!found)
//		{
//            List<PolyBase> polyList = PolyBase.getPointsInArea(polyArea, layer, true, true, null);
//
//            for (PolyBase nPoly : polyList)
//            {
//                reportError(DRCErrorType.LAYERSURROUNDERROR, "Polysilicon not covered, ", cell, minOverlapValue, -1,
//                        minOverlapRule.ruleName, nPoly, geom, layer, null, null, null);
//            }
//		}
//		else
//		{
//			// Checking if not enough coverage
//			Point2D[] extraPoints = new Point2D[checkExtraPoints.size()];
//			checkExtraPoints.toArray(extraPoints);
//			boolean[] founds = new boolean[checkExtraPoints.size()];
//			Arrays.fill(founds, false);
//			Rectangle2D ruleBnd = new Rectangle2D.Double(polyBnd.getMinX()-minOverlapValue,
//								polyBnd.getMinY()-minOverlapValue,
//								polyBnd.getWidth() + minOverlapValue*2,
//								polyBnd.getHeight() + minOverlapValue*2);
//			boolean foundAll = allPointsContainedInLayer(geom, cell, ruleBnd, null, extraPoints, founds);
//
//			if (!foundAll)
//				reportError(DRCErrorType.LAYERSURROUNDERROR, "Not enough surround, ", geom.getParent(), minOverlapValue,
//                        -1, minOverlapRule.ruleName, poly, geom, layer, null, null, null);
//		}
//		return (!found);
//	}

//    private boolean checkThisCellExtensionRule(Geometric geom, Layer layer, Poly poly, Layer.Function.Set drcLayers, Cell cell, Area polyArea,
//                                               Rectangle2D polyBnd, DRCTemplate minOverlapRule, boolean found,
//                                               List<Point2D> checkExtraPoints)
//    {
//        boolean[] founds = new boolean[4];
//
//        for(Iterator<RTBounds> sIt = cell.searchIterator(polyBnd); !found && sIt.hasNext(); )
//		{
//        	RTBounds g = sIt.next();
//	        if (g == geom) continue;
//			if (!(g instanceof NodeInst))
//            {
//                if (Job.LOCALDEBUGFLAG && !DRC.isIgnoreExtensionRuleChecking())
//                    System.out.println("Skipping arcs!");
//                continue; // Skipping arcs!!!!
//            }
//			NodeInst ni = (NodeInst)g;
//			NodeProto np = ni.getProto();
//			if (NodeInst.isSpecialNode(ni)) continue; // Nov 4;
//			if (ni.isCellInstance())
//			{
//				AffineTransform cellDownTrans = ni.transformIn();
//				AffineTransform	cellUpTrans = ni.transformOut();
//				DBMath.transformRect(polyBnd, cellDownTrans);
//				polyArea.transform(cellDownTrans);
//				poly.transform(cellDownTrans);
//				List<Point2D> newExtraPoints = new ArrayList<Point2D>();
//				found = checkThisCellExtensionRule(geom, layer, poly, drcLayers, (Cell)np, polyArea, polyBnd, minOverlapRule,
//				        found, newExtraPoints);
//                for (Point2D point : newExtraPoints)
////				for (Iterator<Point2D> it = newExtraPoints.iterator(); it.hasNext();)
//				{
////					Point2D point = it.next();
//					cellUpTrans.transform(point, point);
//					checkExtraPoints.add(point);
//				}
//				DBMath.transformRect(polyBnd, cellUpTrans);
//				polyArea.transform(cellUpTrans);
//				poly.transform(cellUpTrans);
//			}
//			else
//			{
//				Technology tech = np.getTechnology();
//				Poly [] primPolyList = tech.getShapeOfNode(ni, true, ignoreCenterCuts, drcLayers);
//				int tot = primPolyList.length;
//				for(int j=0; j<tot; j++)
//				{
//					Poly nPoly = primPolyList[j];
//                    if (drcLayers == null)
//                        if (!nPoly.getLayer().getFunction().isImplant()) continue;
//
//                    // Checking if are covered by select is surrounded by minOverlapRule
//                    Area distPolyArea = (Area)polyArea.clone();
//                    Area nPolyArea = new Area(nPoly);
//                    polyArea.subtract(nPolyArea);
//
//                    distPolyArea.subtract(polyArea);
//					if (distPolyArea.isEmpty())
//						continue;  // no intersection
//                    Rectangle2D interRect = distPolyArea.getBounds2D();
//                    Rectangle2D ruleBnd = new Rectangle2D.Double(interRect.getMinX()-minOverlapRule.getValue(0),
//                            interRect.getMinY()-minOverlapRule.getValue(0),
//                            interRect.getWidth() + minOverlapRule.getValue(0)*2,
//                            interRect.getHeight() + minOverlapRule.getValue(0)*2);
//                    PolyBase extPoly = new PolyBase(ruleBnd);
//					PolyBase distPoly = new PolyBase(interRect);
//                    Arrays.fill(founds, false);
//					// Removing points on original polygon. No very efficient though
//					Point2D[] points = extPoly.getPoints();
//					Point2D[] distPoints = distPoly.getPoints();
//					// Only valid for 4-point polygons!!
//					if (distPoints.length != points.length)
//						System.out.println("This case is not valid in Quick.checkThisCellExtensionRule");
//					for (int i = 0; i < points.length; i++)
//					{
//						// Check if point is corner
//						found = poly.isPointOnCorner(distPoints[i]);
//						// Point along edge
//						if (!found)
//							founds[i] = true;
//					}
//
//                    boolean foundAll = allPointsContainedInLayer(geom, cell, ruleBnd, drcLayers, points, founds);
//                    if (!foundAll)
//                    {
//	                    // Points that should be checked from geom parent
//	                    for (int i = 0; i < founds.length; i++)
//	                    {
//		                    if (!founds[i]) checkExtraPoints.add(points[i]);
//	                    }
//                    }
//				}
//			}
//			found = polyArea.isEmpty();
//		}
//        return (found);
//    }

    /**
     * Method to check if certain poly rectangle is fully covered by any select regoin
     */
//    private boolean allPointsContainedInLayer(Geometric geom, Cell cell, Rectangle2D ruleBnd, Layer.Function.Set drcLayers, Point2D[] points, boolean[] founds)
//    {
//        for(Iterator<RTBounds> sIt = cell.searchIterator(ruleBnd); sIt.hasNext(); )
//        {
//        	RTBounds g = sIt.next();
//	        if (g == geom) continue;
//            if (!(g instanceof NodeInst)) continue;
//            NodeInst ni = (NodeInst)g;
//            NodeProto np = ni.getProto();
//	        if (NodeInst.isSpecialNode(ni)) continue; // Nov 4;
//            if (ni.isCellInstance())
//            {
//				AffineTransform cellDownTrans = ni.transformIn();
//				AffineTransform	cellUpTrans = ni.transformOut();
//				DBMath.transformRect(ruleBnd, cellDownTrans);
//	            cellDownTrans.transform(points, 0, points, 0, points.length);
//                boolean allFound = allPointsContainedInLayer(geom, (Cell)np, ruleBnd, drcLayers, points, founds);
//				DBMath.transformRect(ruleBnd, cellUpTrans);
//	            cellUpTrans.transform(points, 0, points, 0, points.length);
//	            if (allFound)
//	                return true;
//            }
//            else
//            {
//                Technology tech = np.getTechnology();
//                Poly [] primPolyList = tech.getShapeOfNode(ni, true, ignoreCenterCuts, drcLayers);
//                int tot = primPolyList.length;
//                for(int j=0; j<tot; j++)
//                {
//                    Poly nPoly = primPolyList[j];
//                    if (!nPoly.getLayer().getFunction().isImplant()) continue;
//                    boolean allFound = true;
//	                // No need of looping if one of them is already out
//                    for (int i = 0; i < points.length; i++)
//                    {
//                        if (!founds[i]) founds[i] = nPoly.contains(points[i]);
//	                    if (!founds[i]) allFound = false;
//                    }
//                    if (allFound) return true;
//                }
//            }
//        }
//
//        return (false);
//    }
    /***************************END of Select Over Polysilicn Functions ************************************/

    /***************************START of Poly Cover By Any VT Layer Functions ************************************/
    // Special functions not available for all technologies.
    private static final Layer.Function.Set vtLayers = new Layer.Function.Set(Layer.Function.IMPLANTP, Layer.Function.IMPLANTN);
     /**
     * This method determines if one of the polysilicon polygons is covered by a vth layer. If yes, VT{H/L}_{P/N}.S.2
     * doesn't apply
     * @param polys
     * @param layers
     * @param geoms
     * @param ignoreCenterCuts
     * @return true if geometry is covered by a VTH layer
     */
    public boolean polyCoverByAnyVTLayer(Cell cell, DRCTemplate theRule, Technology tech, Poly[] polys, Layer[] layers,
                                         Geometric[] geoms, boolean ignoreCenterCuts)
     {
         if (!tech.isValidVTPolyRule(theRule))
            return false;

         int polyIndex = -1, vtIndex = -1;

         for (int i = 0; i < polys.length; i++)
         {
             Layer.Function fun = layers[i].getFunction();;
             if (fun.isGatePoly())
                 polyIndex = i;
             else if (layers[i].isVTImplantLayer())
                 vtIndex = i;
         }
         // Not the correct layers
         if (polyIndex == -1 || vtIndex == -1)
             return false;

         if (geoms[polyIndex] instanceof NodeInst)
         {
             NodeProto np = ((NodeInst)geoms[polyIndex]).getProto();
             if (np instanceof PrimitiveNode)
             {
                 PrimitiveNode pn = (PrimitiveNode)np;
                 if (pn.isNodeBitOn(PrimitiveNode.LOWVTBIT) || pn.isNodeBitOn(PrimitiveNode.HIGHVTBIT))
                     return true; // condition valid by construction
             }
         }

         Rectangle2D polyBounds = polys[polyIndex].getBounds2D(); // should I search with delta?

         boolean[] basicFound = new boolean[4];
         Point2D[] basePts = new Point2D[4];
         basePts[0] = new Point2D.Double(polyBounds.getMinX(), polyBounds.getMinY());
         basePts[1] = new Point2D.Double(polyBounds.getMinX(), polyBounds.getMaxY());
         basePts[2] = new Point2D.Double(polyBounds.getMaxX(), polyBounds.getMaxY());
         basePts[3] = new Point2D.Double(polyBounds.getMaxX(), polyBounds.getMinY());
         Rectangle2D basicBnd = new Rectangle2D.Double(DBMath.round(basePts[0].getX()- DRC.TINYDELTA),
                 DBMath.round(basePts[0].getY()- DRC.TINYDELTA),
                 DBMath.round(2*(DRC.TINYDELTA)+polyBounds.getWidth()),
                 DBMath.round(2*(DRC.TINYDELTA)+polyBounds.getHeight()));
         // Searches for any posible VTH/VTH that fully covers the polysilicon.
         // It has to be recursive and could be expensive
         boolean found = lookForLayerWithPoints(null, polys[polyIndex], null, null, cell, layers[vtIndex],
                 DBMath.MATID, basicBnd, basePts, basicFound, 1, true);
         return found;
     }

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
        Rectangle2D result = new Rectangle2D.Double();
        Rectangle2D.union(bounds1, bounds2, result);  // Rectangle2D.union(bounds1, bounds2, bounds1)
        Area area = new Area(result);
        area.subtract(new Area(bounds1));
        area.subtract(new Area(bounds2));
        result = area.getBounds2D();
//        Rectangle2D.intersect(bounds1, result, result);
//        Rectangle2D.intersect(bounds2, result, result);
        return activeOnTransistorRecurse(result, net1, net2, cell, globalIndex, DBMath.MATID);
	}

	private boolean activeOnTransistorRecurse(Rectangle2D bounds,
		int net1, int net2, Cell cell, int globalIndex, AffineTransform trans)
	{
		Netlist netlist = getCheckProto(cell).netlist;
		Rectangle2D subBounds = new Rectangle2D.Double();
		for(Iterator<RTBounds> sIt = cell.searchIterator(bounds); sIt.hasNext(); )
		{
			RTBounds g = sIt.next();
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
			if (!ni.getFunction().isFET()) continue;

			// must be inside of the bounding box of the desired layers
			Rectangle2D nodeBounds = ni.getBounds();

            // Enough with checking if the transistor active covers 100% the union region
            boolean newStrategy = nodeBounds.contains(bounds);

//            if (Job.getDebug())
//            {
//                double cX = nodeBounds.getCenterX();
//                double cY = nodeBounds.getCenterY();
//                boolean oldS = !((cX < bounds.getMinX() || cX > bounds.getMaxX() ||
//                    cY < bounds.getMinY() || cY > bounds.getMaxY()));
//
//                if (newStrategy != oldS)
//                    System.out.println("Error in transistor coverage");
//            }

            if (!newStrategy)
                continue;
//            if (cX < bounds.getMinX() || cX > bounds.getMaxX() ||
//				cY < bounds.getMinY() || cY > bounds.getMaxY()) continue;

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
        assert(!ni.getProto().getFunction().isPin());
        Poly [] cropNodePolyList = tech.getShapeOfNode(ni, true, reportInfo.ignoreCenterCuts, null);
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

            if (NodeInst.isSpecialNode(ni))
                continue; // Dec 08 -> is a pin so ignore it

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
                cropArcPolyList = tech.getShapeOfNode(ni, false, reportInfo.ignoreCenterCuts, null);
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
	 * Method to convert all Layer information in the Polys to be non-pseudo.
	 * This is only done for pins that have exports but no arcs.
	 * @param ni the NodeInst being converted.
	 * @param pList an array of Polys with Layer information for the NodeInst.
	 */
	private void convertPseudoLayers(NodeInst ni, Poly [] pList)
	{
		if (!ni.getProto().getFunction().isPin()) return;
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
	 * Method to determine the minimum distance between "layer1" and "layer2" in technology
	 * "tech" and library "lib".  If "con" is true, the layers are connected.  Also forces
	 * connectivity for same-implant layers. Returns conditional rules if not standard rules were found
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

        // try conditional or standard
        DRCTemplate theRule = (DRC.getSpacingRule(layer1, geo1, layer2, geo2, con, multi, values[0], values[1]));

        if (theRule != null && theRule.condition != null) // conditional rules
        {
            String[] conds = TextUtils.parseString(theRule.condition, "{,}");
            assert(conds.length == 2); // {layerName1, cond(layerName2)}
            String layerName1 = conds[0];
            String[] parameters = TextUtils.parseString(conds[1], "()");
            assert(parameters.length == 2); // cond(layerName2)
            assert(parameters[0].equals("no_overlap")); // only function implemented
            String layerName2 = parameters[1];
            if (!layer1.getName().equals(layerName1))
                theRule = null; // doesn't apply
            else
            {
                Layer l = layer1.getTechnology().findLayer(layerName2);
                boolean found = searchForCondLayer(geo1, poly1, l, geo1.getParent(), reportInfo.ignoreCenterCuts);
                if (found)
                    theRule = null; // doesn't apply
            }
        }

        return theRule;
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
			if (np == Generic.tech().drcNode)
			{
                // Must get polygon from getNodeShape otherwise it will miss
                // rings
                AffineTransform trans = ni.rotateOut();
                Poly [] list = cell.getTechnology().getShapeOfNode(ni, true, true, null);
                assert(list.length == 1);
                list[0].transform(trans);
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
        reportInfo.exclusionMap.put(cell, area);
	}

    /**************************************************************************************************************
	 *  QuickAreaEnumerator abstract class
	 **************************************************************************************************************/
    private abstract class QuickAreaEnumerator extends HierarchyEnumerator.Visitor
    {
        Layer polyLayer;
        Layer.Function.Set activeLayers = new Layer.Function.Set(Layer.Function.DIFFP, Layer.Function.DIFFN);

        /**
         * Method to check whether the layer must be checked or not. It might be done by this layer on that network
         * @param layer
         * @return true is the layer is should be skipped
         */
        boolean skipLayer(Layer layer)
        {
            boolean noMinArea = minAreaLayerMap.get(layer) == null;
            boolean noMinEnc = enclosedAreaLayerMap.get(layer) == null && spacingLayerMap.get(layer) == null;

            if (noMinArea && noMinEnc)
                return true;
            if (layer.getFunction().isContact())  // via*, polyCut, activeCut)
                return true;
            return false;
        }
    }

    /**************************************************************************************************************
	 *  QuickAreaEnumeratorSlow class
	 **************************************************************************************************************/
    private class QuickAreaEnumeratorSlow extends QuickAreaEnumerator
    {
        private GeometryHandler merge;

        QuickAreaEnumeratorSlow(GeometryHandler.GHMode mode)
        {
            // Merge is used by the brute force algorithm
            merge = GeometryHandler.createGeometryHandler(mode, topCell.getTechnology().getNumLayers());
        }

        public boolean enterCell(HierarchyEnumerator.CellInfo info)
        {
            if (job != null && job.checkAbort()) return false;
            AffineTransform rTrans = info.getTransformToRoot();
            Cell cell = info.getCell();

            for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
            {
                ArcInst ai = it.next();
                Network aNet = info.getNetlist().getNetwork(ai, 0);

                // aNet is null if ArcProto is Artwork
                if (aNet == null)
                {
                    continue;
                }
                Technology tech = ai.getProto().getTechnology();
                Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
                int tot = arcInstPolyList.length;
                for(int i=0; i<tot; i++)
                {
                    Poly poly = arcInstPolyList[i];
                    Layer layer = poly.getLayer();

                    // No area associated or already done
                    if (skipLayer(layer))
                        continue;

                    // it has to take into account poly and transistor poly as one layer
                    if (layer.getFunction().isPoly())
                    {
                        if (polyLayer == null)
                            polyLayer = layer;
                        layer = polyLayer;
                    }
                    poly.transform(rTrans);
                    addElement(poly, layer);
                }
            }

            return true;
        }

        private void addElement(Poly poly, Layer layer)
        {
            merge.add(layer, poly);
        }

        public void exitCell(HierarchyEnumerator.CellInfo info)
        {
            Cell cell = info.getCell();
            boolean isTopCell = cell == topCell;

            if (isTopCell)
            {
                this.merge.postProcess(true);
                for (Layer layer : merge.getKeySet())
                {
                    checkMinAreaLayerWithTree(merge, info.getCell(), layer);
                }
            }
        }

        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
        {

            if (job != null && job.checkAbort()) return false;
            // Facet or special elements
			NodeInst ni = no.getNodeInst();
            if (NodeInst.isSpecialNode(ni)) return (false);
            NodeProto np = ni.getProto();
            if (!np.getTechnology().isLayout())
                return (false); // only layout nodes

            // Cells
            if (ni.isCellInstance()) return (true);

			AffineTransform trans = ni.rotateOut();
            AffineTransform root = info.getTransformToRoot();
			if (root.getType() != AffineTransform.TYPE_IDENTITY)
				trans.preConcatenate(root);

            PrimitiveNode pNp = (PrimitiveNode)np;
			Technology tech = pNp.getTechnology();
			// electrical should not be null due to ports but causes
			// problems with poly and transistor-poly
            boolean isTransistor = pNp.getFunction().isTransistor();
            Poly activePoly = null;
            boolean addedActive = false;

            if (isTransistor)
            {
                Poly [] list = tech.getShapeOfNode(ni, false, true, activeLayers);
                assert(list.length == 1);
                activePoly = list[0];
            }
            // Don't get electric layers in case of transistors otherwise it is hard to detect ports
            Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, true, true, null);
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
                // Special case for transistors
                if (activePoly != null && layer.getFunction().isDiff())
                {
                    poly = activePoly; // overlapping is allowed
                    if (!addedActive)
                        addedActive = true;
                    else
                        continue; // no need of adding next active
                }

                // No area associated or already done
                if (skipLayer(layer))
                    continue;

                poly.roundPoints(); // Trying to avoid mismatches while joining areas.
                poly.transform(trans);
                addElement(poly, layer);
            }

            return true;
        }
    }

    /**************************************************************************************************************
	 *  QuickAreaEnumeratorLocal class
	 **************************************************************************************************************/
    /**
     * Class that uses local GeometryHandler to calculate the area per cell
     */
    private class QuickAreaEnumeratorLocal extends QuickAreaEnumerator
    {
        private Map<Cell, MTDRCAreaTool.GeometryHandlerLayerBucket> cellsMap;
        private GeometryHandler.GHMode mode;

        QuickAreaEnumeratorLocal(GeometryHandler.GHMode mode)
        {
            this.mode = mode;
            cellsMap = new HashMap<Cell, MTDRCAreaTool.GeometryHandlerLayerBucket>();
        }

        public boolean enterCell(HierarchyEnumerator.CellInfo info)
        {
            if (job != null && job.checkAbort()) return false;
            Cell cell = info.getCell();
            MTDRCAreaTool.GeometryHandlerLayerBucket bucket = cellsMap.get(cell);
            if (bucket == null)
            {
                bucket = new MTDRCAreaTool.GeometryHandlerLayerBucket(mode);
                cellsMap.put(cell, bucket);
            }
            else
            {
                assert(bucket.merged);
                return false; // done with this cell
            }

            for(Iterator<ArcInst> it = info.getCell().getArcs(); it.hasNext(); )
            {
                ArcInst ai = it.next();
                Network aNet = info.getNetlist().getNetwork(ai, 0);

                // aNet is null if ArcProto is Artwork
                if (aNet == null)
                {
                    continue;
                }
                Technology tech = ai.getProto().getTechnology();
                Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
                int tot = arcInstPolyList.length;
                for(int i=0; i<tot; i++)
                {
                    Poly poly = arcInstPolyList[i];
                    Layer layer = poly.getLayer();

                    // No area associated or already done
                    if (skipLayer(layer))
                        continue;

                    // it has to take into account poly and transistor poly as one layer
                    if (layer.getFunction().isPoly())
                    {
                        if (polyLayer == null)
                            polyLayer = layer;
                        layer = polyLayer;
                    }
                    bucket.addElementLocal(poly, layer);
                }
            }

            return true;
        }

        public void exitCell(HierarchyEnumerator.CellInfo info)
        {
            Cell cell = info.getCell();
            boolean isTopCell = cell == topCell;
            MTDRCAreaTool.GeometryHandlerLayerBucket bucket = cellsMap.get(cell);

            bucket.mergeGeometry(cell, cellsMap);

            if (isTopCell)
            {
                for (Layer layer : bucket.local.getKeySet())
                {
                    checkMinAreaLayerWithTree(bucket.local, info.getCell(), layer);
                }
            }
        }

        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
        {
            if (job != null && job.checkAbort()) return false;
            // Facet or special elements
			NodeInst ni = no.getNodeInst();
            if (NodeInst.isSpecialNode(ni)) return (false);
            NodeProto np = ni.getProto();
            if (!np.getTechnology().isLayout())
                return (false); // only layout nodes
            MTDRCAreaTool.GeometryHandlerLayerBucket bucket = cellsMap.get(info.getCell());

            // Cells
            if (ni.isCellInstance()) return (true);

			AffineTransform trans = ni.rotateOut();
//            AffineTransform root = info.getTransformToRoot();
//			if (root.getType() != AffineTransform.TYPE_IDENTITY)
//				trans.preConcatenate(root);

            PrimitiveNode pNp = (PrimitiveNode)np;
			Technology tech = pNp.getTechnology();
			// electrical should not be null due to ports but causes
			// problems with poly and transistor-poly
            boolean isTransistor = pNp.getFunction().isTransistor();
            Poly activePoly = null;
            boolean addedActive = false;

            if (isTransistor)
            {
                Poly [] list = tech.getShapeOfNode(ni, false, true, activeLayers);
                assert(list.length == 1);
                activePoly = list[0];
            }
            // Don't get electric layers in case of transistors otherwise it is hard to detect ports
            Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, true, true, null);
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
                // Special case for transistors
                if (activePoly != null && layer.getFunction().isDiff())
                {
                    poly = activePoly; // overlapping is allowed
                    if (!addedActive)
                        addedActive = true;
                    else
                        continue; // no need of adding next active
                }

                // No area associated or already done
                if (skipLayer(layer))
                    continue;

                poly.roundPoints(); // Trying to avoid mismatches while joining areas.
                poly.transform(trans);
                bucket.addElementLocal(poly, layer);

//                poly.transform(root);   // if transform is identity, it does nothing
//                addElement(poly, layer);
            }

            return true;
        }
    }
}


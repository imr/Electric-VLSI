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
import com.sun.electric.database.geometry.*;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortOriginal;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.Main;

import java.awt.geom.*;
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
 */
public class Quick
{
	private static final double TINYDELTA = DBMath.getEpsilon()*1.1;

	// the different types of errors
	private static final int SPACINGERROR       = 1;
	private static final int MINWIDTHERROR      = 2;
	private static final int NOTCHERROR         = 3;
	private static final int MINSIZEERROR       = 4;
	private static final int BADLAYERERROR      = 5;
	private static final int LAYERSURROUNDERROR = 6;
	private static final int MINAREAERROR       = 7;
	private static final int ENCLOSEDAREAERROR  = 8;
	private static final int POLYSELECTERROR   = 9;
	// Different types of warnings
	private static final int ZEROLENGTHARCWARN  = 10;

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
	};
	private HashMap checkInsts = null;


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
		/** list of instances in a particular parent */					List nodesInCell;
		/** netlist of this cell */										Netlist netlist;
	};
	private HashMap checkProtos = null;
	private HashMap networkLists = null;
	private HashMap minAreaLayerMap = new HashMap();    // For minimum area checking
	private HashMap enclosedAreaLayerMap = new HashMap();    // For enclosed area checking
	private DRC.CheckDRCLayoutJob job; // Reference to running job
	private HashMap cellsMap = new HashMap(); // for cell caching
    private HashMap nodesMap = new HashMap(); // for node caching

	public Quick(DRC.CheckDRCLayoutJob job)
	{
		this.job = job;
	}

	/**
	 * The InstanceInter object records interactions between two cell instances and prevents checking
	 * them multiple times.
	 */
	private static class InstanceInter
	{
		/** the two cell instances being compared */	Cell cell1, cell2;
		/** rotation of cell instance 1 */				int rot1;
		/** mirroring of cell instance 1 */				boolean mirrorX1, mirrorY1;
		/** rotation of cell instance 2 */				int rot2;
		/** mirroring of cell instance 2 */				boolean mirrorX2, mirrorY2;
		/** distance from instance 1 to instance 2 */	double dx, dy;
	};
	private List instanceInteractionList = new ArrayList();


	/**
	 * The DRCExclusion object lists areas where Generic:DRC-Nodes exist to ignore errors.
	 */
	private static class DRCExclusion
	{
		Cell cell;
		Poly poly;
		NodeInst ni;
	};
	private List exclusionList = new ArrayList();


	/** number of processes for doing DRC */					private int numberOfThreads;
	/** true to report only 1 error per Cell. */				private boolean onlyFirstError;
	/** true to ignore center cuts in large contacts. */		private boolean ignoreCenterCuts;
	/** maximum area to examine (the worst spacing rule). */	private double worstInteractionDistance;
	/** time stamp for numbering networks. */					private int checkTimeStamp;
	/** for numbering networks. */								private int checkNetNumber;
	/** total errors found in all threads. */					private int totalMsgFound;
	/** a NodeInst that is too tiny for its connection. */		private NodeInst tinyNodeInst;
	/** the other Geometric in "tiny" errors. */				private Geometric tinyGeometric;
	/** for tracking the time of good DRC. */					//private HashMap goodDRCDate = new HashMap();
	/** for tracking the time of good DRC. */					//private boolean haveGoodDRCDate;
	/** for logging errors */                                   private ErrorLogger errorLogger;
	/** for logging incremental errors */                       private static ErrorLogger errorLoggerIncremental = null;
	/** Top cell for DRC */                                     private Cell topCell;

	/* for figuring out which layers are valid for DRC */
	private Technology layersValidTech = null;
	private boolean [] layersValid;

	/* for tracking which layers interact with which nodes */
	private Technology layerInterTech = null;
	private HashMap layersInterNodes = null;
	private HashMap layersInterArcs = null;

	/**
	 * This is the entry point for DRC.
	 *
	 * Method to do a hierarchical DRC check on cell "cell".
	 * If "count" is zero, check the entire cell.
	 * If "count" is nonzero, only check that many instances (in "nodesToCheck") and set the
	 * entry in "validity" TRUE if it is DRC clean.
	 * @param bounds if null, check entire cell. If not null, only check area in bounds.
     * @param drcJob
	 * @return the number of errors found
	 */
	public static int checkDesignRules(Cell cell, int count, Geometric[] geomsToCheck, boolean[] validity, Rectangle2D bounds, DRC.CheckDRCLayoutJob drcJob)
	{
		Quick q = new Quick(drcJob);
		return q.doCheck(cell, count, geomsToCheck, validity, bounds);
	}


    // returns the number of errors found
	private int doCheck(Cell cell, int count, Geometric [] geomsToCheck, boolean [] validity, Rectangle2D bounds)
	{

		// Check if there are DRC rules for particular tech
		DRCRules rules = DRC.getRules(cell.getTechnology());

		// Nothing to check for this particular technology
		if (rules == null || rules.getNumberOfRules() == 0) return 0;

		// get the current DRC options
		onlyFirstError = DRC.isOneErrorPerCell();
		ignoreCenterCuts = DRC.isIgnoreCenterCuts();
		numberOfThreads = DRC.getNumberOfThreads();
	    topCell = cell; /* Especially important for minArea checking */

		// if checking specific instances, adjust options and processor count
		if (count > 0)
		{
			onlyFirstError = true;
			numberOfThreads = 1;
		}

		// get the proper technology
		Technology tech = cell.getTechnology();

		// cache valid layers for this technology
		cacheValidLayers(tech);
		buildLayerInteractions(tech);

		// clean out the cache of instances
		clearInstanceCache();

		// determine maximum DRC interaction distance
		worstInteractionDistance = DRC.getWorstSpacingDistance(tech);

	    // determine if min area must be checked (if any layer got valid data)
	    minAreaLayerMap.clear();
	    enclosedAreaLayerMap.clear();
	    cellsMap.clear();
	    nodesMap.clear();

	    // No incremental neither per Cell
	    if (!DRC.isIgnoreAreaChecking() && !onlyFirstError)
	    {
		    for(Iterator it = tech.getLayers(); it.hasNext(); )
			{
				Layer layer = (Layer)it.next();

				// Storing min areas
				DRCRules.DRCRule minAreaRule = DRC.getMinValue(layer, DRCTemplate.AREA);
				if (minAreaRule != null)
					minAreaLayerMap.put(layer, minAreaRule);

				// Storing enclosed areas
				DRCRules.DRCRule enclosedAreaRule = DRC.getMinValue(layer, DRCTemplate.ENCLOSEDAREA);
				if (enclosedAreaRule != null)
					enclosedAreaLayerMap.put(layer, enclosedAreaRule);
			}
	    }

		// initialize all cells for hierarchical network numbering
		checkProtos = new HashMap();
		checkInsts = new HashMap();

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
		int totalNetworks = 0;
		networkLists = new HashMap();
		for(Iterator it = checkProtos.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry e = (Map.Entry)it.next();
			Cell libCell = (Cell)e.getKey();
			CheckProto subCP = (CheckProto)e.getValue();
			if (subCP.hierInstanceCount > 0)
			{
				// allocate net number lists for every net in the cell
				for(Iterator nIt = subCP.netlist.getNetworks(); nIt.hasNext(); )
				{
					Network net = (Network)nIt.next();
					Integer [] netNumbers = new Integer[subCP.hierInstanceCount];
					for(int i=0; i<subCP.hierInstanceCount; i++) netNumbers[i] = new Integer(0);
					networkLists.put(net, netNumbers);
					totalNetworks += subCP.hierInstanceCount;
				}
			}
			for(Iterator nIt = libCell.getNodes(); nIt.hasNext(); )
			{
				NodeInst ni = (NodeInst)nIt.next();
				NodeProto np = ni.getProto();
				if (!(np instanceof Cell)) continue;

				// ignore documentation icons
				if (ni.isIconOfParent()) continue;

				CheckInst ci = (CheckInst)checkInsts.get(ni);
				CheckProto ocp = getCheckProto((Cell)np);
				ci.offset = ocp.totalPerCell;
			}
			checkTimeStamp++;
			for(Iterator nIt = libCell.getNodes(); nIt.hasNext(); )
			{
				NodeInst ni = (NodeInst)nIt.next();
				NodeProto np = ni.getProto();
				if (!(np instanceof Cell)) continue;

				// ignore documentation icons
				if (ni.isIconOfParent()) continue;

				CheckProto ocp = getCheckProto((Cell)np);
				if (ocp.timeStamp != checkTimeStamp)
				{
					CheckInst ci = (CheckInst)checkInsts.get(ni);
					ocp.timeStamp = checkTimeStamp;
					ocp.totalPerCell += subCP.hierInstanceCount * ci.multiplier;
				}
			}
		}

		// now fill in the hierarchical network arrays
		checkTimeStamp = 0;
		checkNetNumber = 1;

		HashMap enumeratedNets = new HashMap();
		for(Iterator nIt = cp.netlist.getNetworks(); nIt.hasNext(); )
		{
			Network net = (Network)nIt.next();
			enumeratedNets.put(net, new Integer(checkNetNumber));
			checkNetNumber++;
		}
		checkEnumerateNetworks(cell, cp, 0, enumeratedNets);

		if (count <= 0)
			System.out.println("Found " + checkNetNumber + " networks");

		// now search for DRC exclusion areas
		exclusionList.clear();
		accumulateExclusion(cell, DBMath.MATID);

		// now do the DRC
		//haveGoodDRCDate = false;
		errorLogger = null;
        int logsFound = 0;
		if (count == 0)
		{
			// just do full DRC here
			errorLogger = ErrorLogger.newInstance("DRC (full)");
//			if (!dr_quickparalleldrc) begintraversehierarchy();
			checkThisCell(cell, 0, bounds);
//			if (!dr_quickparalleldrc) endtraversehierarchy();

			// sort the errors by layer
			errorLogger.sortLogs();
		} else
		{
			// check only these "count" instances (either an incremental DRC or a quiet one...from Array command)
			if (validity == null)
			{
				// not a quiet DRC, so it must be incremental
				if (errorLoggerIncremental == null) errorLoggerIncremental = ErrorLogger.newInstance("DRC (incremental)", true);
				errorLoggerIncremental.clearLogs(cell);
				errorLogger = errorLoggerIncremental;
                logsFound = errorLoggerIncremental.getNumLogs();
			}

			checkTheseGeometrics(cell, count, geomsToCheck, validity);
		}

		if (errorLogger != null) {
            errorLogger.termLogging(true);
            logsFound = errorLogger.getNumLogs() - logsFound;
        }

//		if (!Main.getDebug() && haveGoodDRCDate && count == 0)
//		{
//			// some cells were sucessfully checked: save that information in the database
//			SaveDRCDates job = new SaveDRCDates(goodDRCDate);
//		}
        return logsFound;
	}

	/**
	 * Class to save good DRC dates in a new thread.
	 */
//	private static class SaveDRCDates extends Job
//	{
//		HashMap goodDRCDate;
//
//		protected SaveDRCDates(HashMap goodDRCDate)
//		{
//			super("Remember DRC Successes", DRC.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
//			this.goodDRCDate = goodDRCDate;
//			startJob();
//		}
//
//		public boolean doIt()
//		{
//			for(Iterator it = goodDRCDate.keySet().iterator(); it.hasNext(); )
//			{
//				Cell cell = (Cell)it.next();
//				Date now = (Date)goodDRCDate.get(cell);
//				DRC.setLastDRCDate(cell, now);
//			}
//			return true;
//		}
//	}

	/*************************** QUICK DRC CELL EXAMINATION ***************************/

	/**
	 * Method to check the contents of cell "cell" with global network index "globalIndex".
	 * Returns positive if errors are found, zero if no errors are found, negative on internal error.
	 */
	private int checkThisCell(Cell cell, int globalIndex, Rectangle2D bounds)
	{
		// Job aborted or scheduled for abort
		if (job != null && job.checkAbort()) return -1;

		if (Main.LOCALDEBUGFLAG)
		{
		if (cellsMap.get(cell) != null)
		{
			System.out.println("Done already cell " + cell.getName());
			return (0);
		}
		}

		// Previous # of errors/warnings
		int prevErrors = 0;
		int prevWarns = 0;
		if (errorLogger != null)
		{
			prevErrors = errorLogger.getNumErrors();
			prevWarns = errorLogger.getNumWarnings();
		}

		cellsMap.put(cell, cell);
		// first check all subcells
		boolean allSubCellsStillOK = true;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (!(np instanceof Cell)) continue;

			// ignore documentation icons
			if (ni.isIconOfParent()) continue;

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
			CheckInst ci = (CheckInst)checkInsts.get(ni);
			int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;
			int retval = checkThisCell((Cell)np, localIndex, subBounds);
			if (retval < 0)
				return -1;
			if (retval > 0) allSubCellsStillOK = false;
		}

		// prepare to check cell
		CheckProto cp = getCheckProto(cell);
		cp.cellChecked = true;

		// if the cell hasn't changed since the last good check, stop now
//		if (allSubCellsStillOK)
//		{
//			Date lastGoodDate = DRC.getLastDRCDate(cell);
//			if (lastGoodDate != null)
//			{
//				Date lastChangeDate = cell.getRevisionDate();
//				if (lastGoodDate.after(lastChangeDate)) return 0;
//			}
//		}

		// announce progress
		System.out.println("Checking cell " + cell.describe());

		// Check the area first but only when is not incremental
		// Only for the most top cell
		if (cell == topCell && !DRC.isIgnoreAreaChecking() && !onlyFirstError)
			checkMinArea(cell);

		// now look at every node and arc here
		totalMsgFound = 0;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (bounds != null)
			{
				if (!ni.getBounds().intersects(bounds)) continue;
			}
			boolean ret = (ni.getProto() instanceof Cell) ?
			        checkCellInst(ni, globalIndex) :
			        checkNodeInst(ni, globalIndex);
			if (ret)
			{
				totalMsgFound++;
				if (onlyFirstError) break;
			}
		}
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			if (bounds != null)
			{
				if (!ai.getBounds().intersects(bounds)) continue;
			}
			if (checkArcInst(cp, ai, globalIndex))
			{
				totalMsgFound++;
				if (onlyFirstError) break;
			}
		}

		// if there were no errors, remember that
		if (errorLogger != null)
		{
			int localErrors = errorLogger.getNumErrors() - prevErrors;
			int localWarnings = errorLogger.getNumWarnings() - prevWarns;
			if (localErrors == 0 &&  localWarnings == 0)
			{
				//goodDRCDate.put(cell, new Date());
				//haveGoodDRCDate = true;
				System.out.println("   No errors/warnings found");
			} else
			{
				if (localErrors > 0)
					System.out.println("   FOUND " + localErrors + " ERRORS");
				if (localWarnings > 0)
					System.out.println("   FOUND " + localWarnings + " WARNINGS");
			}
		}

		return totalMsgFound;
	}

	/**
	 * Method to check the design rules about nodeinst "ni".
	 * Returns true if an error was found.
	 */
	private boolean checkNodeInst(NodeInst ni, int globalIndex)
	{
		Cell cell = ni.getParent();
		Netlist netlist = getCheckProto(cell).netlist;
		NodeProto np = ni.getProto();
		Technology tech = np.getTechnology();
		AffineTransform trans = ni.rotateOut();

		if (Main.LOCALDEBUGFLAG)
		{
		if (nodesMap.get(ni) != null)
		{
			System.out.println("Done already node " + ni.getName());
			return (false);
		}

		nodesMap.put(ni, ni);
		}

		// Skipping special nodes
		if (NodeInst.isSpecialNode(ni)) return false; // Oct 5;

        if (np.getFunction() == PrimitiveNode.Function.PIN) return false; // Sept 30

		// get all of the polygons on this node
		Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, null, true, ignoreCenterCuts);
		convertPseudoLayers(ni, nodeInstPolyList);
		int tot = nodeInstPolyList.length;
        boolean isTransistor =  np.getFunction().isTransistor();
		// examine the polygons on this node
		boolean errorsFound = false;
		for(int j=0; j<tot; j++)
		{
			Poly poly = nodeInstPolyList[j];
			Layer layer = poly.getLayer();
			if (layer == null) continue;

			poly.transform(trans);

			// determine network for this polygon
			int netNumber = getDRCNetNumber(netlist, poly.getPort(), ni, globalIndex);

			boolean ret = badBox(poly, layer, netNumber, tech, ni, trans, cell, globalIndex);
			if (ret)
			{
				if (onlyFirstError) return true;
				errorsFound = true;
			}
			ret = checkMinWidth(ni, layer, poly, tot==1);
			if (ret)
			{
				if (onlyFirstError) return true;
				errorsFound = true;
			}
			// Check PP.R.1 select over transistor poly
			// Assumes polys on transistors fulfill condition by construction
			ret = !isTransistor && checkSelectOverPolysilicon(ni, layer, poly, cell);
			if (ret)
			{
				if (onlyFirstError) return true;
				errorsFound = true;
			}
			if (tech == layersValidTech && !layersValid[layer.getIndex()])
			{
				reportError(BADLAYERERROR, null, cell, 0, 0, null,
					poly, ni, layer, null, null, null);
				if (onlyFirstError) return true;
				errorsFound = true;
			}
		}

		// check node for minimum size
		DRC.NodeSizeRule sizeRule = DRC.getMinSize(np);
		if (sizeRule != null)
		{
			if (ni.getXSize() < sizeRule.sizeX || ni.getYSize() < sizeRule.sizeY)
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
				reportError(MINSIZEERROR, msg, cell, minSize, actual, sizeRule.rule,
					null, ni, null, null, null, null);
			}
		}
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
		Integer [] netNumbers = (Integer [])networkLists.get(net);

		if (Main.LOCALDEBUGFLAG)
		{
			if (nodesMap.get(ai) != null)
			{
				System.out.println("Done already arc " + ai.getName());
				return (false);
			}

			nodesMap.put(ai, ai);
		}

		// get all of the polygons on this arc
		Technology tech = ai.getProto().getTechnology();
		Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
		cropActiveArc(ai, arcInstPolyList);
		int tot = arcInstPolyList.length;

		// examine the polygons on this arc
		boolean errorsFound = false;
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
				if (onlyFirstError) return true;
				errorsFound = true;
			}
			ret = checkMinWidth(ai, layer, poly, tot==1);
			if (ret)
			{
				if (onlyFirstError) return true;
				errorsFound = true;
			}
			// Check PP.R.1 select over transistor poly
			ret = checkSelectOverPolysilicon(ai, layer, poly, ai.getParent());
			if (ret)
			{
				if (onlyFirstError) return true;
				errorsFound = true;
			}
			if (tech == layersValidTech && !layersValid[layerNum])
			{
				reportError(BADLAYERERROR, null, ai.getParent(), 0, 0, null,
					(tot==1)?null:poly, ai, layer, null, null, null);
				if (onlyFirstError) return true;
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
		CheckInst ci = (CheckInst)checkInsts.get(ni);
		int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;

		// look for other instances surrounding this one
		Rectangle2D nodeBounds = ni.getBounds();
		Rectangle2D searchBounds = new Rectangle2D.Double(
			nodeBounds.getMinX()-worstInteractionDistance,
			nodeBounds.getMinY()-worstInteractionDistance,
			nodeBounds.getWidth() + worstInteractionDistance*2,
			nodeBounds.getHeight() + worstInteractionDistance*2);
		for(Iterator it = ni.getParent().searchIterator(searchBounds); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();

			if ( geom == ni ) continue; // covered by checkInteraction?

			if (!(geom instanceof NodeInst)) continue;
			NodeInst oNi = (NodeInst)geom;

			// only check other nodes that are numerically higher (so each pair is only checked once)
			if (oNi.getNodeIndex() <= ni.getNodeIndex()) continue;
			if (!(oNi.getProto() instanceof Cell)) continue;

			// see if this configuration of instances has already been done
			if (checkInteraction(ni, oNi)) continue;

			// found other instance "oNi", look for everything in "ni" that is near it
			Rectangle2D nearNodeBounds = oNi.getBounds();
			Rectangle2D subBounds = new Rectangle2D.Double(
				nearNodeBounds.getMinX()-worstInteractionDistance,
				nearNodeBounds.getMinY()-worstInteractionDistance,
				nearNodeBounds.getWidth() + worstInteractionDistance*2,
				nearNodeBounds.getHeight() + worstInteractionDistance*2);

			// recursively search instance "ni" in the vicinity of "oNi"
//			if (!dr_quickparalleldrc) downhierarchy(ni, ni->proto, 0);
			checkCellInstContents(subBounds, ni, upTrans,
				localIndex, oNi, globalIndex);
//			if (!dr_quickparalleldrc) uphierarchy();
		}
		return false;
	}

	/**
	 * Method to recursively examine the area "bounds" in cell "cell" with global index "globalIndex".
	 * The objects that are found are transformed by "uptrans" to be in the space of a top-level cell.
	 * They are then compared with objects in "oNi" (which is in that top-level cell),
	 * which has global index "topGlobalIndex".
	 */
	private boolean checkCellInstContents(Rectangle2D bounds, NodeInst thisNi,
		AffineTransform upTrans, int globalIndex, NodeInst oNi, int topGlobalIndex)
	{
        // Job aborted or scheduled for abort
		if (job != null && job.checkAbort()) return true;

		Cell cell = (Cell)thisNi.getProto();
		boolean logsFound = false;
		Netlist netlist = getCheckProto(cell).netlist;

		// Need to transform bounds coordinates first otherwise it won't
		// never overlap
		Rectangle2D bb = (Rectangle2D)bounds.clone();
		AffineTransform downTrans = thisNi.transformIn();
		DBMath.transformRect(bb, downTrans);

		for(Iterator it = cell.searchIterator(bb); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (Main.getDebug() && geom == thisNi )
			{
				System.out.println("Should I skip it 2?");
				//continue;
			}

			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				NodeProto np = ni.getProto();

                if (NodeInst.isSpecialNode(ni)) continue; // Oct 5;

				if (np instanceof Cell)
				{
					AffineTransform subUpTrans = ni.translateOut(ni.rotateOut());
					subUpTrans.preConcatenate(upTrans);

					CheckInst ci = (CheckInst)checkInsts.get(ni);

					int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;

//					if (!dr_quickparalleldrc) downhierarchy(ni, ni->proto, 0);
					// changes Sept04: subBound by bb
					checkCellInstContents(bb, ni, subUpTrans, localIndex, oNi, topGlobalIndex);
//					if (!dr_quickparalleldrc) uphierarchy();
				} else
				{
					AffineTransform rTrans = ni.rotateOut();
					rTrans.preConcatenate(upTrans);
                    if (np.getFunction() == PrimitiveNode.Function.PIN)
                    {
	                    System.out.println("This should not happend in Quick.checkCellInstContents()");
	                    continue; // Sept 30
                    }
					Technology tech = np.getTechnology();
					Poly [] primPolyList = tech.getShapeOfNode(ni, null, true, ignoreCenterCuts);
					convertPseudoLayers(ni, primPolyList);
					int tot = primPolyList.length;
					for(int j=0; j<tot; j++)
					{
						Poly poly = primPolyList[j];
						Layer layer = poly.getLayer();
						if (layer == null)
                        {
                            if (Main.LOCALDEBUGFLAG) System.out.println("When is this case?");
                            continue;
                        }

						poly.transform(rTrans);

						// determine network for this polygon
						int net = getDRCNetNumber(netlist, poly.getPort(), ni, globalIndex);
						boolean ret = badSubBox(poly, layer, net, tech, ni, rTrans,
							globalIndex, oNi, topGlobalIndex);
						if (ret)
						{
							if (onlyFirstError) return true;
							logsFound = true;
						}
					}
				}
			} else
			{
				ArcInst ai = (ArcInst)geom;
				Technology tech = ai.getProto().getTechnology();
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
						Integer [] netList = (Integer [])networkLists.get(jNet);
						net = netList[globalIndex].intValue();
					}
					boolean ret = badSubBox(poly, layer, net, tech, ai, upTrans,
						globalIndex, oNi, topGlobalIndex);
					if (ret)
					{
						if (onlyFirstError) return true;
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
		double minSize = poly.getMinSize();

		AffineTransform upTrans = oNi.translateOut(oNi.rotateOut());

		CheckInst ci = (CheckInst)checkInsts.get(oNi);
		int localIndex = topGlobalIndex * ci.multiplier + ci.localIndex + ci.offset;

		// determine if original object has multiple contact cuts
		boolean baseMulti = false;
		if (geom instanceof NodeInst)
		{
			baseMulti = isMultiCut((NodeInst)geom);
		}

		// remember the current position in the hierarchy traversal and set the base one
//		gethierarchicaltraversal(state->hierarchytempsnapshot);
//		sethierarchicaltraversal(state->hierarchybasesnapshot);

		// search in the area surrounding the box
		bounds.setRect(bounds.getMinX()-bound, bounds.getMinY()-bound, bounds.getWidth()+bound*2, bounds.getHeight()+bound*2);
		boolean retval = badBoxInArea(poly, layer, tech, net, geom, trans, globalIndex,
			bounds, (Cell)oNi.getProto(), localIndex,
			oNi.getParent(), topGlobalIndex, upTrans, minSize, baseMulti, false);

		// restore the proper hierarchy traversal position
//		sethierarchicaltraversal(state->hierarchytempsnapshot);
		return retval;
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
		double minSize = poly.getMinSize();

		// determine if original object has multiple contact cuts
		boolean baseMulti = false;
		if (geom instanceof NodeInst)
			baseMulti = isMultiCut((NodeInst)geom);

		// search in the area surrounding the box
		bounds.setRect(bounds.getMinX()-bound, bounds.getMinY()-bound, bounds.getWidth()+bound*2, bounds.getHeight()+bound*2);
		return badBoxInArea(poly, layer, tech, net, geom, trans, globalIndex,
			bounds, cell, globalIndex,
			cell, globalIndex, DBMath.MATID, minSize, baseMulti, true);
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
		Cell topCell, int topGlobalIndex, AffineTransform upTrans, double minSize, boolean baseMulti,
		boolean sameInstance)
	{
		Rectangle2D rBound = new Rectangle2D.Double();
		rBound.setRect(bounds);
		DBMath.transformRect(rBound, upTrans); // Step 1
		Netlist netlist = getCheckProto(cell).netlist;
        Rectangle2D subBound = new Rectangle2D.Double(); //Sept 30

		// These nodes won't generate any DRC errors. Most of them are pins
		if (geom instanceof NodeInst && NodeInst.isSpecialNode(((NodeInst)geom)))
			return false;

		// Sept04 changes: bounds by rBound
		for(Iterator it = cell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric nGeom = (Geometric)it.next();
			if (sameInstance && (nGeom == geom)) continue;
			if (nGeom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)nGeom;
				NodeProto np = ni.getProto();

				if (NodeInst.isSpecialNode(ni)) continue; // Oct 5;

				// ignore nodes that are not primitive
				if (np instanceof Cell)
				{
					// instance found: look inside it for offending geometry
					AffineTransform rTransI = ni.rotateIn();
					AffineTransform tTransI = ni.translateIn();
					rTransI.preConcatenate(tTransI);
					subBound.setRect(bounds);
					DBMath.transformRect(subBound, rTransI);

					CheckInst ci = (CheckInst)checkInsts.get(ni);
					int localIndex = cellGlobalIndex * ci.multiplier + ci.localIndex + ci.offset;

					AffineTransform subTrans = ni.translateOut(ni.rotateOut());
					subTrans.preConcatenate(upTrans);        //Sept 15 04

					// compute localIndex
//					if (!dr_quickparalleldrc) downhierarchy(ni, np, 0);
					badBoxInArea(poly, layer, tech, net, geom, trans, globalIndex,
						subBound, (Cell)np, localIndex,
						topCell, topGlobalIndex, subTrans, minSize, baseMulti, sameInstance);
//					if (!dr_quickparalleldrc) uphierarchy();
				} else
				{
					// don't check between technologies
					if (np.getTechnology() != tech) continue;

					// see if this type of node can interact with this layer
					if (!checkLayerWithNode(layer, np)) continue;

                    if (np.getFunction() == PrimitiveNode.Function.PIN)
                    {
	                    System.out.println("This should not happen in Quick.badBoxInArea");
	                    continue; // Sept 30
                    }

					// see if the objects directly touch
					boolean touch = Geometric.objectsTouch(nGeom, geom);

					// prepare to examine every layer in this nodeinst
					AffineTransform rTrans = ni.rotateOut();
					rTrans.preConcatenate(upTrans);

					// get the shape of each nodeinst layer
					Poly [] subPolyList = tech.getShapeOfNode(ni, null, true, ignoreCenterCuts);
					convertPseudoLayers(ni, subPolyList);
					int tot = subPolyList.length;
					for(int i=0; i<tot; i++)
						subPolyList[i].transform(rTrans);
					    /* Step 1 */
					boolean multi = baseMulti;
					if (!multi) multi = isMultiCut(ni);
					for(int j=0; j<tot; j++)
					{
						Poly npoly = subPolyList[j];
						Layer nLayer = npoly.getLayer();
						if (nLayer == null) continue;
						//if (nLayer.isNonElectrical()) continue; // covered by isSpecialNode
                        //npoly.transform(rTrans);

						Rectangle2D nPolyRect = npoly.getBounds2D();

						// can't do this because "lxbound..." is local but the poly bounds are global
						if (nPolyRect.getMinX() > rBound.getMaxX() ||
							nPolyRect.getMaxX() < rBound.getMinX() ||
							nPolyRect.getMinY() > rBound.getMaxY() ||
							nPolyRect.getMaxY() < rBound.getMinY()) continue;

						// determine network for this polygon
						int nNet = getDRCNetNumber(netlist, npoly.getPort(), ni, cellGlobalIndex);

						// see whether the two objects are electrically connected
						boolean con = false;
						if (nNet >= 0 && nNet == net) con = true;

						// if they connect electrically and adjoin, don't check
						if (con && touch) continue;

						double nMinSize = npoly.getMinSize();
						DRCRules.DRCRule dRule = getAdjustedMinDist(tech, layer, minSize,
                                nLayer, nMinSize, con, multi);
						DRCRules.DRCRule eRule = DRC.getEdgeRule(layer, nLayer);
						if (dRule == null && eRule == null) continue;
						double dist = -1;
						boolean edge = false;
						String rule;
						if (dRule != null)
						{
							dist = dRule.value;
							rule = dRule.rule;
						} else
						{
							dist = eRule.value;
							rule = eRule.rule;
							edge = true;
						}

						// check the distance
						boolean ret = checkDist(tech, topCell, topGlobalIndex,
							poly, layer, net, geom, trans, globalIndex,
							npoly, nLayer, nNet, nGeom, rTrans, cellGlobalIndex,
							con, dist, edge, rule);
						if (ret) return true;
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

				// see if the objects directly touch
				boolean touch = Geometric.objectsTouch(nGeom, geom);

				// see whether the two objects are electrically connected
				Network jNet = netlist.getNetwork(ai, 0);
				Integer [] netNumbers = (Integer [])networkLists.get(jNet);
				int nNet = netNumbers[cellGlobalIndex].intValue();
				boolean con = false;
				if (net >= 0 && nNet == net) con = true;

				// if they connect electrically and adjoin, don't check
				if (con && touch) continue;

				// get the shape of each arcinst layer
				Poly [] subPolyList = tech.getShapeOfArc(ai);
				int tot = subPolyList.length;
				for(int i=0; i<tot; i++)
					subPolyList[i].transform(upTrans);
				cropActiveArc(ai, subPolyList);
				boolean multi = baseMulti;
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

					// see how close they can get
					double nMinSize = nPoly.getMinSize();
					DRCRules.DRCRule dRule = getAdjustedMinDist(tech, layer, minSize,
						nLayer, nMinSize, con, multi);
					DRCRules.DRCRule eRule = DRC.getEdgeRule(layer, nLayer);
					if (dRule == null && eRule == null) continue;
					double dist = -1;
					boolean edge = false;
					String rule;
					if (dRule != null)
					{
						dist = dRule.value;
						rule = dRule.rule;
					} else
					{
						dist = eRule.value;
						rule = eRule.rule;
						edge = true;
					}

					// check the distance
					boolean ret = checkDist(tech, topCell, topGlobalIndex,
						poly, layer, net, geom, trans, globalIndex,
						nPoly, nLayer, nNet, nGeom, upTrans, cellGlobalIndex,
						con, dist, edge, rule);
					if (ret) return true;
				}
			}
		}
		return false;
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
		boolean con, double dist, boolean edge, String rule)
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

		/*
		 * special rule for allowing touching:
		 *   the layers are the same and either:
		 *     they connect and are *NOT* contact layers
		 *   or:
		 *     they don't connect and are implant layers (substrate/well)
		 */
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
			}
		}

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
			if (maytouch)
			{
				// they are electrically connected: see if they touch
				overlap = (pd < 0);
                //code decomissioned Sept 04
				if (Main.LOCALDEBUGFLAG && pd <= 0)
				{
					// they are electrically connected and they touch: look for minimum size errors
					DRCRules.DRCRule wRule = DRC.getMinValue(layer1, DRCTemplate.MINWID);
					if (wRule != null)
					{
						double minWidth = wRule.value;
						double lxb = Math.max(trueBox1.getMinX(), trueBox2.getMinX());
						double hxb = Math.min(trueBox1.getMaxX(), trueBox2.getMaxX());
						double lyb = Math.max(trueBox1.getMinY(), trueBox2.getMinY());
						double hyb = Math.min(trueBox1.getMaxY(), trueBox2.getMaxY());
						Point2D lowB = new Point2D.Double(lxb, lyb);
						Point2D highB = new Point2D.Double(hxb, hyb);
						double actual = lowB.distance(highB);
						if (actual != 0 && actual < minWidth)
						{
							if (hxb-lxb > hyb-lyb)
							{
								// horizontal abutment: check for minimum width
								Point2D pt1 = new Point2D.Double(lxb-TINYDELTA, lyb-TINYDELTA);
								Point2D pt2 = new Point2D.Double(lxb-TINYDELTA, hyb+TINYDELTA);
								Point2D pt3 = new Point2D.Double(hxb+TINYDELTA, lyb-TINYDELTA);
								Point2D pt4 = new Point2D.Double(hxb+TINYDELTA, hyb+TINYDELTA);
								if (!lookForPoints(pt1, pt2, layer1, cell, true) &&
									!lookForPoints(pt3, pt4, layer1, cell, true))
								{
									lyb -= minWidth/2;   hyb += minWidth/2;
									Poly rebuild = new Poly((lxb+hxb)/2, (lyb+hyb)/2, hxb-lxb, hyb-lyb);
									rebuild.setStyle(Poly.Type.FILLED);
                                    /*
									reportError(MINWIDTHERROR, null, cell, minWidth,
                                            actual, sizeRule, rebuild,
											geom1, layer1, null, null, null);
									return true;
                                    */
                                    System.out.println("Quick.checkDist code decomissioned");
								}
							} else
							{
								// vertical abutment: check for minimum width
								Point2D pt1 = new Point2D.Double(lxb-TINYDELTA, lyb-TINYDELTA);
								Point2D pt2 = new Point2D.Double(hxb+TINYDELTA, lyb-TINYDELTA);
								Point2D pt3 = new Point2D.Double(lxb-TINYDELTA, hyb+TINYDELTA);
								Point2D pt4 = new Point2D.Double(hxb+TINYDELTA, hyb+TINYDELTA);
								if (!lookForPoints(pt1, pt2, layer1, cell, true) &&
									!lookForPoints(pt3, pt4, layer1, cell, true))
								{
									lxb -= minWidth/2;   hxb += minWidth/2;
									Poly rebuild = new Poly((lxb+hxb)/2, (lyb+hyb)/2, hxb-lxb, hyb-lyb);
									rebuild.setStyle(Poly.Type.FILLED);
									/*reportError(MINWIDTHERROR, null, cell, minWidth,
                                            actual, sizeRule, rebuild,
											geom1, layer1, null, null, null);
									return true;
                                   */
                                   System.out.println("Quick.checkDist code decomissioned");
								}
							}
						}
					}
				}
			}
			// crop out parts of any arc that is covered by an adjoining node
			trueBox1 = new Rectangle2D.Double(trueBox1.getMinX(), trueBox1.getMinY(), trueBox1.getWidth(), trueBox1.getHeight());
			trueBox2 = new Rectangle2D.Double(trueBox2.getMinX(), trueBox2.getMinY(), trueBox2.getWidth(), trueBox2.getHeight());

			if (geom1 instanceof NodeInst)
			{
				if (cropNodeInst((NodeInst)geom1, globalIndex1, trans1,
				        layer2, net2, geom2, trueBox2))
						return false;
			} else
			{
				if (cropArcInst((ArcInst)geom1, layer1, trans1, trueBox1))
					return false;
			}
			if (geom2 instanceof NodeInst)
			{
				if (cropNodeInst((NodeInst)geom2, globalIndex2, trans2,
				        layer1, net1, geom1, trueBox1))
						return false;
			} else
			{
				if (cropArcInst((ArcInst)geom2, layer2, trans2, trueBox2))
					return false;
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

					if (pd < dist && pd > 0)
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
				style != Poly.Type.OPENEDT3 && style != Poly.Type.VECTORS) return false;

			style = poly2.getStyle();
			if (style != Poly.Type.FILLED && style != Poly.Type.CLOSED &&
				style != Poly.Type.CROSSED && style != Poly.Type.OPENED &&
				style != Poly.Type.OPENEDT1 && style != Poly.Type.OPENEDT2 &&
				style != Poly.Type.OPENEDT3 && style != Poly.Type.VECTORS) return false;

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

		// see if the design rule is met
		if (pd >= dist)
		{
			return false;
		}
		int errorType = SPACINGERROR;

		/*
		 * special case: ignore errors between two active layers connected
		 * to either side of a field-effect transistor that is inside of
		 * the error area.
		 */
		if (activeOnTransistor(poly1, layer1, net1, poly2, layer2, net2, cell, globalIndex))
			return false;

		// special cases if the layers are the same
		if (tech.sameLayer(layer1, layer2))
		{
			// special case: check for "notch"
			if (maytouch)
			{
				// if they touch, it is acceptable
				if (pd <= 0) return false;

				/*
				if (!Main.LOCALDEBUGFLAG && net1 == net2)
				{
					return (false);
				}
				*/

				// see if the notch is filled
				boolean newR = lookForCrossPolygons(geom1, poly1, geom2, poly2, layer1, cell, overlap);
				if (Main.LOCALDEBUGFLAG)
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
				if (newR) return false;

				/*
				if (net1 == net2)
				{
					if (Main.LOCALDEBUGFLAG)
						System.out.println("Should I return false before?");
					return (false);
				}
				*/

				// look further if on the same net and diagonally separate (1 intervening point)
				//if (net1 == net2 && intervening == 1) return false;
				errorType = NOTCHERROR;
			}
		}
//if (debug) System.out.println("   ERROR FOUND");

		String msg = null;
		if (tinyNodeInst != null)
		{
			// see if the node/arc that failed was involved in the error
			if ((tinyNodeInst == geom1 || tinyNodeInst == geom2) &&
				(tinyGeometric == geom1 || tinyGeometric == geom2))
			{
				msg = tinyNodeInst.describe() + " is too small for the " + tinyGeometric.describe();
			}
		}

		reportError(errorType, msg, cell, dist, pd, rule,
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
				if (ni.getProto() instanceof Cell)
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
		//AffineTransform rTrans = ni.rotateOut();
		//upTrans.preConcatenate(rTrans);

		// get network numbering information for this instance
		CheckInst ci = (CheckInst)checkInsts.get(ni);
		if (ci == null) return false;
		int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;

		// look for other objects surrounding this one
		Rectangle2D nodeBounds = ni.getBounds();
		Rectangle2D searchBounds = new Rectangle2D.Double(
			nodeBounds.getMinX() - worstInteractionDistance,
			nodeBounds.getMinY() - worstInteractionDistance,
			nodeBounds.getWidth() + worstInteractionDistance*2,
			nodeBounds.getHeight() + worstInteractionDistance*2);
		for(Iterator it = cell.searchIterator(searchBounds); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();

			if ( geom == ni )
			{
				//System.out.println("Should I skip it?");
				continue;
			}

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

//			// ignore if it is one of the geometrics in the list
//			boolean found = false;
//			for(int j=0; j<count; j++)
//				if (oNi == geomsToCheck[j]) { found = true;   break; }
//			if (found) continue;

			// found other instance "oNi", look for everything in "ni" that is near it
			Rectangle2D subNodeBounds = oNi.getBounds();
			Rectangle2D subBounds = new Rectangle2D.Double(
				subNodeBounds.getMinX() - worstInteractionDistance,
				subNodeBounds.getMinY() - worstInteractionDistance,
				subNodeBounds.getWidth() + worstInteractionDistance*2,
				subNodeBounds.getHeight() + worstInteractionDistance*2);

			// recursively search instance "ni" in the vicinity of "oNi"
			if (checkCellInstContents(subBounds, ni, upTrans,
				localIndex, oNi, globalIndex)) return true;
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
			tech = oNi.getProto().getTechnology();
			trans = oNi.rotateOut();
			nodeInstPolyList = tech.getShapeOfNode(oNi, null, true, ignoreCenterCuts);
			convertPseudoLayers(oNi, nodeInstPolyList);
			baseMulti = isMultiCut(oNi);
		} else
		{
			ArcInst oAi = (ArcInst)geom;
			tech = oAi.getProto().getTechnology();
			nodeInstPolyList = tech.getShapeOfArc(oAi);
		}
		if (nodeInstPolyList == null) return false;
		int tot = nodeInstPolyList.length;

		CheckInst ci = (CheckInst)checkInsts.get(ni);
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
				Integer [] netNumbers = (Integer [])networkLists.get(jNet);
				net = netNumbers[globalIndex].intValue();
			}

			// determine if original object has multiple contact cuts
			double minSize = poly.getMinSize();

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
			if (badBoxInArea(poly, polyLayer, tech, net, geom, trans, globalIndex,
				subBounds, (Cell)np, localIndex,
				subCell, globalIndex, subTrans, minSize, baseMulti, false)) return true;
		}
		return false;
	}

	/*************************** QUICK DRC CACHE OF INSTANCE INTERACTIONS ***************************/

	/**
	 * Method to look for an interaction between instances "ni1" and "ni2".  If it is found,
	 * return TRUE.  If not found, add to the list and return FALSE.
	 */
	private boolean checkInteraction(NodeInst ni1, NodeInst ni2)
	{
		// must recheck parameterized instances always
		CheckProto cp = getCheckProto((Cell)ni1.getProto());
		if (cp.cellParameterized) return false;
		cp = getCheckProto((Cell)ni2.getProto());
		if (cp.cellParameterized) return false;

		// keep the instances in proper numeric order
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
		InstanceInter dii = new InstanceInter();
		dii.cell1 = (Cell)ni1.getProto();
		dii.rot1 = ni1.getAngle();
		dii.mirrorX1 = ni1.isMirroredAboutXAxis();
		dii.mirrorY1 = ni1.isMirroredAboutYAxis();

		dii.cell2 = (Cell)ni2.getProto();
		dii.rot2 = ni2.getAngle();
		dii.mirrorX2 = ni2.isMirroredAboutXAxis();
		dii.mirrorY2 = ni2.isMirroredAboutYAxis();

		dii.dx = ni2.getAnchorCenterX() - ni1.getAnchorCenterX();
		dii.dy = ni2.getAnchorCenterY() - ni1.getAnchorCenterY();

		// if found, stop now
		if (findInteraction(dii)) return true;

		// insert it now
		instanceInteractionList.add(dii);

		return false;
	}

	/**
	 * Method to remove all instance interaction information.
	 */
	private void clearInstanceCache()
	{
		instanceInteractionList.clear();
	}

	/**
	 * Method to look for the instance-interaction in "dii" in the global list of instances interactions
	 * that have already been checked.  Returns the entry if it is found, NOINSTINTER if not.
	 */
	private boolean findInteraction(InstanceInter dii)
	{
			for(Iterator it = instanceInteractionList.iterator(); it.hasNext(); )
		{
			InstanceInter thisII = (InstanceInter)it.next();
			if (thisII.cell1 == dii.cell1 && thisII.cell2 == dii.cell2 &&
				thisII.rot1 == dii.rot1 && thisII.rot2 == dii.rot2 &&
				thisII.mirrorX1 == dii.mirrorX1 && thisII.mirrorX2 == dii.mirrorX2 &&
				thisII.mirrorY1 == dii.mirrorY1 && thisII.mirrorY2 == dii.mirrorY2 &&
				thisII.dx == dii.dx && thisII.dy == dii.dy) return true;
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
		for(Iterator vIt = cell.getVariables(); vIt.hasNext(); )
		{
			Variable var = (Variable)vIt.next();
			if (var.getTextDescriptor().isParam())
			{
				cp.cellParameterized = true;
				cp.treeParameterized = true;
				break;
			}
		}
		checkProtos.put(cell, cp);

		for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
		{
			NodeInst ni = (NodeInst)nIt.next();
			if (!(ni.getProto() instanceof Cell)) continue;
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
		return (CheckProto) checkProtos.get(cell);
	}

	/**
	 * Method to recursively examine the hierarchy below cell "cell" and fill in the
	 * CheckInst objects on every cell instance.  Uses the CheckProto objects
	 * to keep track of cell usage.
	 */
	private void checkEnumerateInstances(Cell cell)
	{
		// number all of the instances in this cell
		checkTimeStamp++;
		List subCheckProtos = new ArrayList();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (!(np instanceof Cell)) continue;

			// ignore documentation icons
			if (ni.isIconOfParent()) continue;

			CheckProto cp = getCheckProto((Cell)np);
			if (cp.timeStamp != checkTimeStamp)
			{
				cp.timeStamp = checkTimeStamp;
				cp.instanceCount = 0;
				cp.nodesInCell = new ArrayList();
				subCheckProtos.add(cp);
			}

			CheckInst ci = (CheckInst)checkInsts.get(ni);
			ci.localIndex = cp.instanceCount++;
			cp.nodesInCell.add(ci);
		}

		// update the counts for this cell
		for(Iterator it = subCheckProtos.iterator(); it.hasNext(); )
		{
			CheckProto cp = (CheckProto)it.next();
			cp.hierInstanceCount += cp.instanceCount;
			for(Iterator nIt = cp.nodesInCell.iterator(); nIt.hasNext(); )
			{
				CheckInst ci = (CheckInst)nIt.next();
				ci.multiplier = cp.instanceCount;
			}
		}

		// now recurse
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (!(np instanceof Cell)) continue;

			// ignore documentation icons
			if (ni.isIconOfParent()) continue;

			checkEnumerateInstances((Cell)np);
		}
	}

	private void checkEnumerateNetworks(Cell cell, CheckProto cp, int globalIndex, HashMap enumeratedNets)
	{
		// store all network information in the appropriate place
		for(Iterator nIt = cp.netlist.getNetworks(); nIt.hasNext(); )
		{
			Network net = (Network)nIt.next();
			Integer netNumber = (Integer)enumeratedNets.get(net);
			Integer [] netNumbers = (Integer [])networkLists.get(net);
			netNumbers[globalIndex] = netNumber;
		}

		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (!(np instanceof Cell)) continue;

			// ignore documentation icons
			if (ni.isIconOfParent()) continue;

			// compute the index of this instance
			CheckInst ci = (CheckInst)checkInsts.get(ni);
			int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;

			// propagate down the hierarchy
			Cell subCell = (Cell)np;
			CheckProto subCP = getCheckProto(subCell);

			HashMap subEnumeratedNets = new HashMap();
			for(Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
			{
				PortInst pi = (PortInst)pIt.next();
				Export subPP = (Export)pi.getPortProto();
				Network net = cp.netlist.getNetwork(ni, subPP, 0);
				if (net == null) continue;
				Integer netNumber = (Integer)enumeratedNets.get(net);
				Network subNet = subCP.netlist.getNetwork(subPP, 0);
				subEnumeratedNets.put(subNet, netNumber);
			}
			for(Iterator nIt = subCP.netlist.getNetworks(); nIt.hasNext(); )
			{
				Network net = (Network)nIt.next();
				if (subEnumeratedNets.get(net) == null)
					subEnumeratedNets.put(net, new Integer(checkNetNumber++));
			}
			checkEnumerateNetworks(subCell, subCP, localIndex, subEnumeratedNets);
		}
	}

	/*********************************** QUICK DRC SUPPORT ***********************************/

	/**
	 * Method to ensure that polygon "poly" on layer "layer" from object "geom" in
	 * technology "tech" meets minimum width rules.  If it is too narrow, other layers
	 * in the vicinity are checked to be sure it is indeed an error.  Returns true
	 * if an error is found.
	 * @param geom
	 * @param layer
	 * @param poly
	 * @param onlyOne
     * @return
	 */
	private boolean checkMinWidth(Geometric geom, Layer layer, Poly poly, boolean onlyOne)
	{
		Cell cell = geom.getParent();
		DRCRules.DRCRule minWidthRule = DRC.getMinValue(layer, DRCTemplate.MINWID);
		if (minWidthRule == null) return false;

		// simpler analysis if manhattan
		Rectangle2D bounds = poly.getBox();
		if (bounds != null)
		{
			boolean tooSmallWidth = DBMath.isGreaterThan(minWidthRule.value, bounds.getWidth());
			boolean tooSmallHeight = DBMath.isGreaterThan(minWidthRule.value, bounds.getHeight());
			if (!tooSmallWidth && !tooSmallHeight) return false;

			double actual = 0;
			Point2D left1, left2, left3, right1, right2, right3;
			//if (bounds.getWidth() < minWidthRule.value)
            String msg = "(X axis)";
            if (tooSmallWidth)
			{
				actual = bounds.getWidth();
				left1 = new Point2D.Double(bounds.getMinX() - TINYDELTA, bounds.getMinY());
				left2 = new Point2D.Double(bounds.getMinX() - TINYDELTA, bounds.getMaxY());
				left3 = new Point2D.Double(bounds.getMinX() - TINYDELTA, bounds.getCenterY());
				right1 = new Point2D.Double(bounds.getMaxX() + TINYDELTA, bounds.getMinY());
				right2 = new Point2D.Double(bounds.getMaxX() + TINYDELTA, bounds.getMaxY());
				right3 = new Point2D.Double(bounds.getMaxX() + TINYDELTA, bounds.getCenterY());
			} else
			{
				actual = bounds.getHeight();
                msg = "(Y axis)";
				left1 = new Point2D.Double(bounds.getMinX(), bounds.getMinY() - TINYDELTA);
				left2 = new Point2D.Double(bounds.getMaxX(), bounds.getMinY() - TINYDELTA);
				left3 = new Point2D.Double(bounds.getCenterX(), bounds.getMinY() - TINYDELTA);
				right1 = new Point2D.Double(bounds.getMinX(), bounds.getMaxY() + TINYDELTA);
				right2 = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY() + TINYDELTA);
				right3 = new Point2D.Double(bounds.getCenterX(), bounds.getMaxY() + TINYDELTA);
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

            int errorType = MINWIDTHERROR;
			String extraMsg = msg;
			String rule = minWidthRule.rule;
			if (zeroWide)
			{
				if (overlapLayer) extraMsg = " but covered by other layer";
				errorType = ZEROLENGTHARCWARN;
				rule = null;
			}

			reportError(errorType, extraMsg, cell, minWidthRule.value, actual, rule,
			        (onlyOne) ? null : poly, geom, layer, null, null, null);
			return !overlapLayer;
		}

		// nonmanhattan polygon: stop now if it has no size
		Poly.Type style = poly.getStyle();
		if (style != Poly.Type.FILLED && style != Poly.Type.CLOSED && style != Poly.Type.CROSSED &&
			style != Poly.Type.OPENED && style != Poly.Type.OPENEDT1 && style != Poly.Type.OPENEDT2 &&
			style != Poly.Type.OPENEDT3 && style != Poly.Type.VECTORS) return false;

		// simple check of nonmanhattan polygon for minimum width
		bounds = poly.getBounds2D();
		double actual = Math.min(bounds.getWidth(), bounds.getHeight());
		if (actual < minWidthRule.value)
		{
			reportError(MINWIDTHERROR, null, cell, minWidthRule.value, actual, minWidthRule.rule,
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

				if (actual < minWidthRule.value)
				{
					// look between the points to see if it is minimum width or notch
					if (poly.isInside(new Point2D.Double((center.getX()+inter.getX())/2, (center.getY()+inter.getY())/2)))
					{
						reportError(MINWIDTHERROR, null, cell, minWidthRule.value,
							actual, minWidthRule.rule, (onlyOne) ? null : poly, geom, layer, null, null, null);
					} else
					{
						reportError(NOTCHERROR, null, cell, minWidthRule.value,
							actual, minWidthRule.rule, (onlyOne) ? null : poly, geom, layer, poly, geom, layer);
					}
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkMinAreaLayer(GeometryHandler merge, Cell cell, Layer layer)
	{
		boolean errorFound = false;
		DRCRules.DRCRule minAreaRule = (DRCRules.DRCRule)minAreaLayerMap.get(layer);
		DRCRules.DRCRule encloseAreaRule = (DRCRules.DRCRule)enclosedAreaLayerMap.get(layer);

		// Layer doesn't have min area
		if (minAreaRule == null && encloseAreaRule == null) return (false);

		Collection set = merge.getObjects(layer, false, true);

		for(Iterator pIt = set.iterator(); pIt.hasNext(); )
		{
			PolyQTree.PolyNode pn = (PolyQTree.PolyNode)pIt.next();
			if (pn == null) throw new Error("wrong condition in Quick.checkMinArea()");
			List list = pn.getSortedLoops();
			// Order depends on area comparison done. First element is the smallest.
			// and depending on # polygons it could be minArea or encloseArea
			DRCRules.DRCRule evenRule = (list.size()%2==0) ? encloseAreaRule : minAreaRule;
			DRCRules.DRCRule oddRule = (evenRule == minAreaRule) ? encloseAreaRule : minAreaRule;
			// Looping over simple polygons. Possible problems with disconnected elements
			// polyArray.length = Maximum number of distintic loops
			for (int i = 0; i < list.size(); i++)
			{
				PolyQTree.PolyNode simplePn = (PolyQTree.PolyNode)list.get(i);
				double area = simplePn.getArea();
				DRCRules.DRCRule minRule = (i%2 == 0) ? evenRule : oddRule;

				if (minRule == null) continue;

				// isGreaterThan doesn't consider equals condition therefore negate condition is used
				if (!DBMath.isGreaterThan(minRule.value, area)) continue;

				errorFound = true;
				int errorType = (minRule == minAreaRule) ? MINAREAERROR : ENCLOSEDAREAERROR;
				reportError(errorType, null, cell, minRule.value, area, minRule.rule,
						new Poly(simplePn.getPoints()), null /*ni*/, layer, null, null, null);
			}
		}
		return errorFound;

	}

	/**
	 * Method to ensure that polygon "poly" on layer "layer" from object "geom" in
	 * technology "tech" meets minimum area rules. Returns true
	 * if an error is found.
	 * @param cell
	 * @return
	 */
	private boolean checkMinArea(Cell cell)
	{
		CheckProto cp = getCheckProto(cell);
		boolean errorFound = false;

		// Nothing to check
		if (minAreaLayerMap.isEmpty() && enclosedAreaLayerMap.isEmpty())
			return false;

		// Select/well regions
		GeometryHandler	selectMerge = new PolyQTree(cell.getBounds());
		GeometryHandler	notExportedMerge = new PolyQTree(cell.getBounds());
		HashMap notExportedNodes = new HashMap();
		HashMap checkedNodes = new HashMap();

		// Get merged areas. Only valid for layers that have connections (metals/polys). No valid for NP/PP.A.1 rule
		for(Iterator netIt = cp.netlist.getNetworks(); netIt.hasNext(); )
		{
			Network net = (Network)netIt.next();
			QuickAreaEnumerator quickArea = new QuickAreaEnumerator(net, selectMerge, notExportedNodes, checkedNodes);
			HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, cp.netlist, quickArea);

			// Job aborted
			if (job != null && job.checkAbort()) return true;

			for(Iterator it = quickArea.mainMerge.getKeyIterator(); it.hasNext(); )
			{
				Layer layer = (Layer)it.next();
				boolean localError = checkMinAreaLayer(quickArea.mainMerge, cell, layer);
				if (!errorFound) errorFound = localError;
			}
		}

		// Checking nodes not exported down in the hierarchy. Probably good enought not to collect networks first
		QuickAreaEnumerator quickArea = new QuickAreaEnumerator(notExportedNodes, checkedNodes);
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, cp.netlist, quickArea);
		// Non exported nodes
		for(Iterator it = quickArea.mainMerge.getKeyIterator(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			boolean localError = checkMinAreaLayer(quickArea.mainMerge, cell, layer);
			if (!errorFound) errorFound = localError;
		}

		// Special cases for select areas. You can't evaluate based on networks
		for(Iterator it = selectMerge.getKeyIterator(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			boolean localError = checkMinAreaLayer(selectMerge, cell, layer);
			if (!errorFound) errorFound = localError;
		}

		return errorFound;
	}

	/**
	 * Method to determine whether node "ni" is a multiple cut contact.
	 * @param ni
	 * @return
	 */
	private boolean isMultiCut(NodeInst ni)
	{
		NodeProto np = ni.getProto();
		if (np instanceof Cell) return false;
		PrimitiveNode pnp = (PrimitiveNode)np;
		if (pnp.getSpecialType() != PrimitiveNode.MULTICUT) return false;
		double [] specialValues = pnp.getSpecialValues();
		Technology.MultiCutData mcd = new Technology.MultiCutData(ni, specialValues);
		if (mcd.numCuts() > 1) return true;
		return false;
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
		int intervening = findInterveningPoints(poly1, poly2, pt1, pt2, true);

		// compute bounds for searching inside cells
		double flx = Math.min(pt1.getX(), pt2.getX());   double fhx = Math.max(pt1.getX(), pt2.getX());
		double fly = Math.min(pt1.getY(), pt2.getY());   double fhy = Math.max(pt1.getY(), pt2.getY());
		Rectangle2D bounds = new Rectangle2D.Double(flx, fly, DBMath.round(fhx-flx), DBMath.round(fhy-fly));

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

		for(Iterator it = cell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric g = (Geometric)it.next();
			if (g instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)g;
				if (NodeInst.isSpecialNode(ni)) continue; // Nov 16, no need for checking pins or other special nodes;
				if (ni.getProto() instanceof Cell)
				{
					// compute bounding area inside of sub-cell
					AffineTransform rotI = ni.rotateIn();
					AffineTransform transI = ni.translateIn();
					rotI.preConcatenate(transI);
					//Rectangle2D newBounds = new Rectangle2D.Double();  // sept 30
					newBounds.setRect(bounds);
					DBMath.transformRect(newBounds, rotI);

					// compute new matrix for sub-cell examination
					AffineTransform trans = ni.translateOut(ni.rotateOut());
					//AffineTransform rot = ni.rotateOut();
					//trans.preConcatenate(rot);
					trans.preConcatenate(moreTrans);
					if (lookForLayer(thisPoly, (Cell)ni.getProto(), layer, trans, newBounds,
						pt1, pt2, pt3, pointsFound))
							return true;
					continue;
				}
				AffineTransform bound = ni.rotateOut();
				bound.preConcatenate(moreTrans);
				Technology tech = ni.getProto().getTechnology();
				Poly [] layerLookPolyList = tech.getShapeOfNode(ni, null, false, ignoreCenterCuts);
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
                    {
						return true;
                    }
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
	                                Point2D pt1, Point2D pt2, Point2D pt3, boolean[] pointsFound, boolean overlap)
	{
		int j;
        Rectangle2D newBounds = new Rectangle2D.Double();  // Sept 30

		for(Iterator it = cell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric g = (Geometric)it.next();

			if (g == geo1 || g == geo2)
			{
				continue;
				//System.out.println("Skip in lookForLayerNew");
			}

			// I can't skip geometries to exclude from the search
			if (g instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)g;
				if (NodeInst.isSpecialNode(ni)) continue; // Nov 16, no need for checking pins or other special nodes;
				if (ni.getProto() instanceof Cell)
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
				Poly [] layerLookPolyList = tech.getShapeOfNode(ni, null, false, ignoreCenterCuts);
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

					if (poly.isInside(pt1)) pointsFound[0] = true;
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

	/**
	 * Method to check if non-transistor polysilicons are completed covered by N++/P++ regions
	 * @param geom
	 * @param layer
	 * @param poly
	 * @param cell
	 * @return
	 */
	private boolean checkSelectOverPolysilicon(Geometric geom, Layer layer, Poly poly, Cell cell)
	{
        if(DRC.isIgnorePolySelectChecking()) return false;

		if (!layer.getFunction().isPoly()) return false;
		// One layer must be select and other polysilicon. They are not connected

		DRCRules.DRCRule minOverlapRule = DRC.getMinValue(layer, DRCTemplate.POLYSELECT);
		if (minOverlapRule == null) return false;
		double value = minOverlapRule.value;
        boolean[] founds = new boolean[4];


		Rectangle2D polyBnd = poly.getBounds2D();
        Area polyArea = new Area(poly);
        boolean found = polyArea.isEmpty();

		for(Iterator sIt = cell.searchIterator(polyBnd); !found && sIt.hasNext(); )
		{
			Geometric g = (Geometric)sIt.next();
			if (!(g instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)g;
			NodeProto np = ni.getProto();
			if (NodeInst.isSpecialNode(ni)) continue; // Nov 4;
			if (np instanceof Cell)
			{
				System.out.println("Skipping this case for now");
			}
			else
			{
				Technology tech = np.getTechnology();
				Poly [] primPolyList = tech.getShapeOfNode(ni, null, true, ignoreCenterCuts);
				int tot = primPolyList.length;
				for(int j=0; j<tot; j++)
				{
					Poly nPoly = primPolyList[j];
                    if (!nPoly.getLayer().getFunction().isImplant()) continue;

                    // Checking if are covered by select is surrounded by minOverlapRule
                    Area distPolyArea = (Area)polyArea.clone();
                    Area nPolyArea = new Area(nPoly);
                    polyArea.subtract(nPolyArea);

                    distPolyArea.subtract(polyArea);
					if (distPolyArea.isEmpty())
						continue;  // no intersection
                    Rectangle2D interRect = distPolyArea.getBounds2D();
                    Rectangle2D ruleBnd = new Rectangle2D.Double(interRect.getMinX()-value, interRect.getMinY()-value,
                            interRect.getWidth() + value*2, interRect.getHeight() + value*2);
                    PolyBase extPoly = new PolyBase(ruleBnd);
					PolyBase distPoly = new PolyBase(interRect);
                    Arrays.fill(founds, false);
					// Removing points on original polygon. No very efficient though
					Point2D[] points = extPoly.getPoints();
					Point2D[] distPoints = distPoly.getPoints();
					// Only valid for 4-point polygons!!
					if (distPoints.length != points.length)
						System.out.println("This case is not valid in Quick.checkSelectOverPolysilicon");
					for (int i = 0; i < points.length; i++)
					{
						// Check if point is corner
						found = poly.isPointOnCorner(distPoints[i]);
						// Point along edge
						if (!found)
							founds[i] = true;
					}
                    boolean foundAll = allPointsContainedInLayer(geom, cell, ruleBnd, points, founds);
                    if (!foundAll)
                    {
                         reportError(POLYSELECTERROR, "No enough surround, ", cell, minOverlapRule.value, -1, minOverlapRule.rule,
                            distPoly/*new Poly(distPolyArea.getBounds2D())*/, geom, layer, null, null, null);
                    }
				}
			}
			found = polyArea.isEmpty();
		}

		// error if the merged area doesn't contain 100% the search area.
		if (!found)
		{
            List polyList = PolyBase.getPointsInArea(polyArea, layer, true);

            for (Iterator it = polyList.iterator(); it.hasNext(); )
            {
                PolyBase nPoly = (PolyBase)it.next();
                reportError(POLYSELECTERROR, "Polysilicon not covered, ", cell, minOverlapRule.value, -1, minOverlapRule.rule,
                            nPoly, geom, layer, null, null, null);
            }
		}
		return (!found);
	}

    /**
     * Method to check if certain poly rectangle is fully covered by any select regoin
     * @param cell
     * @param ruleBnd
     * @param points
     * @param founds
     * @return
     */
    private boolean allPointsContainedInLayer(Geometric geom, Cell cell, Rectangle2D ruleBnd, Point2D[] points, boolean[] founds)
    {
        for(Iterator sIt = cell.searchIterator(ruleBnd); sIt.hasNext(); )
        {
            Geometric g = (Geometric)sIt.next();
	        if (g == geom) continue;
            if (!(g instanceof NodeInst)) continue;
            NodeInst ni = (NodeInst)g;
            NodeProto np = ni.getProto();
	        if (NodeInst.isSpecialNode(ni)) continue; // Nov 4;
            if (np instanceof Cell)
            {
                return (allPointsContainedInLayer(geom, (Cell)np, ruleBnd, points, founds));
            }
            else
            {
                Technology tech = np.getTechnology();
                Poly [] primPolyList = tech.getShapeOfNode(ni, null, true, ignoreCenterCuts);
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
		for(Iterator sIt = cell.searchIterator(bounds); sIt.hasNext(); )
		{
			Geometric g = (Geometric)sIt.next();
			if (!(g instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)g;
			NodeProto np = ni.getProto();
			if (np instanceof Cell)
			{
				AffineTransform rTransI = ni.rotateIn();
				AffineTransform tTransI = ni.translateIn();
				rTransI.preConcatenate(tTransI);
//				transmult(rTransI, tTransI, temptrans);
				subBounds.setRect(bounds);
				DBMath.transformRect(subBounds, rTransI);

				CheckInst ci = (CheckInst)checkInsts.get(ni);
				int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;

//				if (!dr_quickparalleldrc) downhierarchy(ni, np, 0);
				boolean ret = activeOnTransistorRecurse(subBounds,
					net1, net2, (Cell)np, localIndex, trans);
//				if (!dr_quickparalleldrc) uphierarchy();
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

				// ignore connections on poly/gate
				Network piNet = netlist.getNetwork(pi);
				if (piNet == badNet) continue;

				Integer [] netNumbers = (Integer [])networkLists.get(piNet);
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
		Poly [] cropNodePolyList = tech.getShapeOfNode(ni, null, true, ignoreCenterCuts);
		convertPseudoLayers(ni, cropNodePolyList);
		int tot = cropNodePolyList.length;
		if (tot < 0) return false;
		for(int j=0; j<tot; j++)
			cropNodePolyList[j].transform(trans);
		boolean isConnected = false;
		Netlist netlist = getCheckProto(ni.getParent()).netlist;
		for(int j=0; j<tot; j++)
		{
			Poly poly = cropNodePolyList[j];
			if (!tech.sameLayer(poly.getLayer(), nLayer)) continue;
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
//		if (gettrace(ni) != NOVARIABLE)
//		{
//			// node is defined with an outline: use advanced methods to crop
//			void *merge;
//
//			merge = mergenew(dr_tool->cluster);
//			makerectpoly(*lx, *hx, *ly, *hy, state->checkdistpoly1rebuild);
//			mergeaddpolygon(merge, nLayer, el_curtech, state->checkdistpoly1rebuild);
//			for(j=0; j<tot; j++)
//			{
//				poly = state->cropnodepolylist->polygons[j];
//				if (!samelayer(poly->tech, poly->layer, nLayer)) continue;
//				mergesubpolygon(merge, nLayer, el_curtech, poly);
//			}
//			if (!mergebbox(merge, lx, hx, ly, hy))
//				allgone = TRUE;
//			mergedelete(merge);
//		} else
		{
			for(int j=0; j<tot; j++)
			{
				Poly poly = cropNodePolyList[j];
				if (!tech.sameLayer(poly.getLayer(), nLayer)) continue;

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
		}
		return allgone;
	}

	/**
	 * Method to crop away any part of layer "lay" of arcinst "ai" that coincides
	 * with a similar layer on a connecting nodeinst.  The bounds of the arcinst
	 * are in the reference parameters (lx-hx, ly-hy).  Returns false
	 * normally, 1 if the arcinst is cropped into oblivion.
	 */
	private boolean cropArcInst(ArcInst ai, Layer lay, AffineTransform inTrans, Rectangle2D bounds)
	{
		for(int i=0; i<2; i++)
		{
			// find the primitive nodeinst at the true end of the portinst
			Connection con = ai.getConnection(i);
			PortInst pi = con.getPortInst();

			PortOriginal fp = new PortOriginal(pi, inTrans);
			NodeInst ni = fp.getBottomNodeInst();
			NodeProto np = ni.getProto();
			AffineTransform trans = fp.getTransformToTop();

			Technology tech = np.getTechnology();
			Poly [] cropArcPolyList = tech.getShapeOfNode(ni, null, false, ignoreCenterCuts);
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
		int count = 0;
		for(int i=0; i<2; i++)
		{
			Connection con = ai.getConnection(i);
			PortInst pi = con.getPortInst();
			NodeInst ni = pi.getNodeInst();
			if (!ni.isFET()) continue;

			// crop the arc against this transistor
			AffineTransform trans = ni.rotateOut();
			Technology tech = ni.getProto().getTechnology();
			Poly [] activeCropPolyList = tech.getShapeOfNode(ni, null, false, ignoreCenterCuts);
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
		if (ni.getNumConnections() != 0) return;
		if (ni.getNumExports() == 0) return;

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
			PrimitiveArc ap = (PrimitiveArc)it.next();
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
	private DRCRules.DRCRule getAdjustedMinDist(Technology tech, Layer layer1, double size1,
		Layer layer2, double size2, boolean con, boolean multi)
	{
		// if they are implant on the same layer, they connect
		if (!con && layer1 == layer2)
		{
			Layer.Function fun = layer1.getFunction();

			// treat all wells as connected
			if (!con)
			{
				if (fun.isSubstrate()) con = true;
			}
		}

		// see how close they can get.
		double wideS = (size1 > size2) ? size1 : size2;

		return (DRC.getSpacingRule(layer1, layer2, con, multi, wideS));
	}

	/**
	 * Method to return the network number for port "pp" on node "ni", given that the node is
	 * in a cell with global index "globalIndex".
	 */
	private int getDRCNetNumber(Netlist netlist, PortProto pp, NodeInst ni, int globalIndex)
	{
		if (pp == null) return -1;

		// see if there is an arc connected
		Network net = netlist.getNetwork(ni, pp, 0);
		Integer [] nets = (Integer [])networkLists.get(net);
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
	        ErrorLogger.MessageLog err =  errorLogger.logWarning("Switching from '" + old.getTechName() +
	                "' to '" +  tech.getTechName() + "' in DRC process. Check for non desired nodes in cell '" +
	                job.cell.describe() + "'", null, -1);
        }

		layersInterNodes = new HashMap();
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			boolean [] layersInNode = new boolean[numLayers];
			//for(int i=0; i<numLayers; i++) layersInNode[i] = false;   // Sept 30
            Arrays.fill(layersInNode, false);

			Technology.NodeLayer [] layers = np.getLayers();
			Technology.NodeLayer [] eLayers = np.getElectricalLayers();
			if (eLayers != null) layers = eLayers;
			for(int i=0; i<layers.length; i++)
			{
				Layer layer = layers[i].getLayer();
				for(Iterator lIt = tech.getLayers(); lIt.hasNext(); )
				{
					Layer oLayer = (Layer)lIt.next();
					if (DRC.isAnyRule(layer, oLayer))
						layersInNode[oLayer.getIndex()] = true;
				}
			}
			layersInterNodes.put(np, layersInNode);
		}

		// build the arc table
		layersInterArcs = new HashMap();
		for(Iterator it = tech.getArcs(); it.hasNext(); )
		{
			PrimitiveArc ap = (PrimitiveArc)it.next();
			boolean [] layersInArc = new boolean[numLayers];
			for(int i=0; i<numLayers; i++) layersInArc[i] = false;

			Technology.ArcLayer [] layers = ap.getLayers();
			for(int i=0; i<layers.length; i++)
			{
				Layer layer = layers[i].getLayer();
				for(Iterator lIt = tech.getLayers(); lIt.hasNext(); )
				{
					Layer oLayer = (Layer)lIt.next();
					if (DRC.isAnyRule(layer, oLayer))
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
		boolean [] validLayers = (boolean [])layersInterNodes.get(np);
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
		boolean [] validLayers = (boolean [])layersInterArcs.get(ap);
		if (validLayers == null) return false;
		return validLayers[layer.getIndex()];
	}

	/**
	 * Method to recursively scan cell "cell" (transformed with "trans") searching
	 * for DRC Exclusion nodes.  Each node is added to the global list "exclusionList".
	 */
	private void accumulateExclusion(Cell cell, AffineTransform upTrans)
	{
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (np == Generic.tech.drcNode)
			{
				DRCExclusion dex = new DRCExclusion();
				dex.cell = cell;
				// extract the information about this DRC exclusion node
				dex.poly = new Poly(ni.getBounds());
				/*
				AffineTransform subUpTrans = ni.rotateOut();
				subUpTrans.preConcatenate(upTrans);
				dex.poly.transform(subUpTrans);
				*/
				dex.poly.setStyle(Poly.Type.FILLED);
				dex.ni = ni;

				// see if it is already in the list
				boolean found = false;
				for(Iterator dIt = exclusionList.iterator(); dIt.hasNext(); )
				{
					DRCExclusion oDex = (DRCExclusion)dIt.next();
					if (oDex.cell != cell) continue;
					if (oDex.poly.polySame(dex.poly))
					{
						found = true;
						break;
					}
				}
				if (!found) exclusionList.add(dex);
				continue;
			}

			if (np instanceof Cell)
			{
				// examine contents
				AffineTransform tTrans = ni.translateOut(ni.rotateOut());
				accumulateExclusion((Cell)np, tTrans);
			}
		}
	}

	/*********************************** QUICK DRC ERROR REPORTING ***********************************/

	/* Adds details about an error to the error list */
	private void reportError(int errorType, String msg,
	                         Cell cell, double limit, double actual, String rule,
	                         PolyBase poly1, Geometric geom1, Layer layer1,
	                         PolyBase poly2, Geometric geom2, Layer layer2)
	{
		if (errorLogger == null) return;

		// if this error is in an ignored area, don't record it
		StringBuffer DRCexclusionMsg = new StringBuffer();
		if (exclusionList.size() > 0)
		{
			// determine the bounding box of the error
			List polyList = new ArrayList(2);
			List geomList = new ArrayList(2);
			polyList.add(poly1); geomList.add(geom1);
			if (poly2 != null)
			{
				polyList.add(poly2);
				geomList.add(geom2);
			}
			for(Iterator it = exclusionList.iterator(); it.hasNext(); )
			{
				DRCExclusion dex = (DRCExclusion)it.next();
				if (cell != dex.cell) continue;
				Poly poly = dex.poly;
				int count = 0;

				for (int i = 0; i < polyList.size(); i++)
				{
					Poly thisPoly = (Poly)polyList.get(i);
					if (thisPoly == null)
						continue; // MinNode case
					boolean found = poly.contains(thisPoly.getBounds2D());

					if (found) count++;
					else DRCexclusionMsg.append("\n\t(DRC Exclusion '" + dex.ni.getName() + "' does not completely contain '" +
						        ((Geometric)geomList.get(i)).describe() + "')");
				}
				// At least one DRC exclusion that contains both
				if (count == polyList.size())
					return;
			}
		}

		// describe the error
		Cell np1 = null;
		StringBuffer errorMessage = new StringBuffer();
		int sortLayer = cell.hashCode(); // 0;
		if (errorType == SPACINGERROR || errorType == NOTCHERROR)
		{
			np1 = geom1.getParent();
			// describe spacing width error
			if (errorType == SPACINGERROR)
				errorMessage.append("Spacing");
			else
				errorMessage.append("Notch");
			if (layer1 == layer2)
				errorMessage.append(" (layer " + layer1.getName() + ")");
			errorMessage.append(": ");
			Cell np2 = geom2.getParent();

			// Message already logged
			if ( errorLogger.findMessage(cell, geom1, geom2.getParent(), geom2))
				return;

			if (np1 != np2)
			{
				errorMessage.append("cell " + np1.describe() + ", ");
			} else if (np1 != cell)
			{
				errorMessage.append("[in cell " + np1.describe() + "] ");
			}
			if (geom1 instanceof NodeInst)
				errorMessage.append("node " + geom1.describe());
			else
				errorMessage.append("arc " + geom1.describe());
			if (layer1 != layer2)
				errorMessage.append(", layer " + layer1.getName());

			if (actual < 0) errorMessage.append(" OVERLAPS ");
			else if (actual == 0) errorMessage.append(" TOUCHES ");
			else errorMessage.append(" LESS (BY " + TextUtils.formatDouble(limit-actual) + ") THAN " + TextUtils.formatDouble(limit) + " TO ");

			if (np1 != np2)
				errorMessage.append("cell " + np2.describe() + ", ");
			if (geom2 instanceof NodeInst)
				errorMessage.append("node " + geom2.describe());
			else
				errorMessage.append("arc " + geom2.describe());
			if (layer1 != layer2)
				errorMessage.append(", layer " + layer2.getName());
			if (msg != null)
				errorMessage.append("; " + msg);
		} else
		{
			// describe minimum width/size or layer error
			StringBuffer errorMessagePart2 = null;
			switch (errorType)
			{
				case MINAREAERROR:
					errorMessage.append("Minimum area error:");
					errorMessagePart2 = new StringBuffer(", layer " + layer1.getName());
					errorMessagePart2.append(" LESS THAN " + TextUtils.formatDouble(limit) + " IN AREA (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case ENCLOSEDAREAERROR:
					errorMessage.append("Enclosed area error:");
					errorMessagePart2 = new StringBuffer(", layer " + layer1.getName());
					errorMessagePart2.append(" LESS THAN " + TextUtils.formatDouble(limit) + " IN AREA (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case ZEROLENGTHARCWARN:
					errorMessage.append("Zero width warning:");
					errorMessagePart2 = new StringBuffer(msg);
					break;
				case MINWIDTHERROR:
                    errorMessage.append("Minimum width/heigh error (" + msg + "):");
					errorMessagePart2 = new StringBuffer(", layer " + layer1.getName());
					errorMessagePart2.append(" LESS THAN " + TextUtils.formatDouble(limit) + " WIDE (IS " + TextUtils.formatDouble(actual) + ")");
                    break;
				case MINSIZEERROR:
					errorMessage.append("Minimum size error on " + msg + ":");
					errorMessagePart2 = new StringBuffer(" LESS THAN " + TextUtils.formatDouble(limit) + " IN SIZE (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case BADLAYERERROR:
					errorMessage.append("Invalid layer (" + layer1.getName() + "):");
					break;
				case POLYSELECTERROR:
					errorMessage.append("Layer surround error: " + msg);
					errorMessagePart2 = new StringBuffer(", layer " + layer1.getName());
					errorMessagePart2.append(" NEEDS SURROUND OF LAYER Select BY " + limit);
                    break;
				case LAYERSURROUNDERROR:
					errorMessage.append("Layer surround error:");
					errorMessagePart2 = new StringBuffer(", layer " + layer1.getName());
					errorMessagePart2.append(" NEEDS SURROUND OF LAYER " + layer2.getName() + " BY " + limit);
					break;
			}

			errorMessage.append(" cell '" + cell.describe() + "'");
			if (geom1 != null)
			{
				errorMessage.append((geom1 instanceof NodeInst) ?
				        ", node '" + geom1.describe() + "'" :
				        ", arc '" + geom1.describe() + "'");
			}
			if (layer1 != null) sortLayer = layer1.getIndex();
			errorMessage.append(errorMessagePart2);
		}
		if (rule != null && rule.length() > 0) errorMessage.append(" [rule " + rule + "]");
		errorMessage.append(DRCexclusionMsg);

		ErrorLogger.MessageLog err = (errorType == ZEROLENGTHARCWARN) ?
		        errorLogger.logWarning(errorMessage.toString(), cell, sortLayer) :
		        errorLogger.logError(errorMessage.toString(), cell, sortLayer);

		boolean showGeom = true;
		if (poly1 != null) { showGeom = false;   err.addPoly(poly1, true, cell); }
		if (poly2 != null) { showGeom = false;   err.addPoly(poly2, true, cell); }
		if (geom1 != null) err.addGeom(geom1, showGeom, cell, null);
		if (geom2 != null) err.addGeom(geom2, showGeom, cell, null);
	}

	/**************************************************************************************************************
	 *  QuickAreaEnumerator class
	 **************************************************************************************************************/
	// Extra functions to check area
	private class QuickAreaEnumerator extends HierarchyEnumerator.Visitor
    {
		private Network jNet;
		private GeometryHandler mainMerge;
		private GeometryHandler otherTypeMerge;
		private Layer polyLayer;
		private HashMap notExportedNodes;
		private HashMap checkedNodes;

		public QuickAreaEnumerator(Network jNet, GeometryHandler selectMerge, HashMap notExportedNodes,
		                           HashMap checkedNodes)
		{
			this.jNet = jNet;
			this.otherTypeMerge = selectMerge;
			this.notExportedNodes = notExportedNodes;
			this.checkedNodes = checkedNodes;
		}

		public QuickAreaEnumerator(HashMap notExportedNodes, HashMap checkedNodes)
		{
			this.notExportedNodes = notExportedNodes;
			this.checkedNodes = checkedNodes;
		}

		/**
		 * Method to search if child network is connected to visitor network.
		 * @param net
		 * @param info
		 * @return
		 */
		private boolean searchNetworkInParent(Network net, HierarchyEnumerator.CellInfo info)
		{
			if (jNet == net) return true;
			HierarchyEnumerator.CellInfo cinfo = info;
			while (net != null && cinfo.getParentInst() != null) {
				net = cinfo.getNetworkInParent(net);
				if (jNet == net) return true;
				cinfo = cinfo.getParentInfo();
			}
			return false;
		}

		/**
		 *
		 * @param info
		 * @return
		 */
		public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
			if (job != null && job.checkAbort()) return false;

			if (mainMerge == null)
				mainMerge = new PolyQTree(info.getCell().getBounds());
            AffineTransform rTrans = info.getTransformToRoot();

			// Check Arcs too if network is not null
			if (jNet != null)
			{
				for(Iterator it = info.getCell().getArcs(); it.hasNext(); )
				{
					ArcInst ai = (ArcInst)it.next();
					Technology tech = ai.getProto().getTechnology();
					Network aNet = info.getNetlist().getNetwork(ai, 0);
					boolean found = false;

					// aNet is null if ArcProto is Artwork
					if (aNet != null)
					{
						for (Iterator arcIt = aNet.getExports(); !found && arcIt.hasNext();)
						{
							Export exp = (Export)arcIt.next();
							Network net = info.getNetlist().getNetwork(exp, 0);
							found = searchNetworkInParent(net, info);
						}
					}

					if (!found && aNet != jNet)
						continue; // no same net

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
						poly.transform(rTrans);
						if (layer.getFunction().isMetal() || layer.getFunction().isPoly())
							mainMerge.add(layer, new PolyQTree.PolyNode(poly.getBounds2D()), false);
						else
							otherTypeMerge.add(layer, new PolyQTree.PolyNode(poly.getBounds2D()), false);
					}
				}
			}
			return true;
		}

		/**
		 *
		 * @param info
		 */
		public void exitCell(HierarchyEnumerator.CellInfo info) {}

		/**
		 *
		 * @param no
		 * @param info
		 * @return
		 */
		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			if (job != null && job.checkAbort()) return false;

			Cell cell = info.getCell();
			NodeInst ni = no.getNodeInst();
			AffineTransform trans = ni.rotateOut();
			NodeProto np = ni.getProto();
			AffineTransform root = info.getTransformToRoot();
			if (root.getType() != AffineTransform.TYPE_IDENTITY)
				trans.preConcatenate(root);

			// Cells
			if (!(np instanceof PrimitiveNode)) return (true);

			PrimitiveNode pNp = (PrimitiveNode)np;
			boolean found = false;
			boolean forceChecking = false;

			if (jNet != null)
			{
				boolean pureNode = (pNp.getFunction() == PrimitiveNode.Function.NODE);
				if (np instanceof PrimitiveNode)
				{
					if (NodeInst.isSpecialNode(ni)) return (false);
					forceChecking = pNp.isPureImplantNode(); // forcing the checking
				}

				boolean notExported = false;
				Network thisNet = null;
				for(Iterator pIt = ni.getPortInsts(); !found && pIt.hasNext(); )
				{
					PortInst pi = (PortInst)pIt.next();
					boolean isExported = cell.findPortProto(pi.getPortProto());
					thisNet = info.getNetlist().getNetwork(pi);
					found = searchNetworkInParent(thisNet, info);
					if (!isExported) notExported = true;
				}
				// Substrate layers are special so we must check node regardless network
				notExported = notExported && !forceChecking && !pureNode && info.getParentInfo() != null;
				if (!found)
				{
					if (notExportedNodes != null && notExported)
					{
						//addNodeInst(ni, pNp, forceChecking, null, trans, nonExportedMerge, nonExportedMerge);
						notExportedNodes.put(ni, ni);
					}
					//else
						return (false);
				}
				// Removing node if it was found included in a network
				notExportedNodes.remove(ni);
				checkedNodes.put(ni, ni);
			}
			else
			{
				if (!notExportedNodes.containsKey(ni))
					return (false); // not to consider
				if (checkedNodes.containsKey(ni))
					return (false);
			}

			Technology tech = pNp.getTechnology();
			// electrical should not be null due to ports but causes
			// problems with poly and transistor-poly
			Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, null, true, true);
			int tot = nodeInstPolyList.length;
			for(int i=0; i<tot; i++)
			{
				Poly poly = nodeInstPolyList[i];
				Layer layer = poly.getLayer();

				// No area rule associated
				if (minAreaLayerMap.get(layer) == null && enclosedAreaLayerMap.get(layer) == null)
					continue;

				// make sure this layer connects electrically to the desired port
				// but only when checking networks, no nonExported.
				if (jNet != null)
				{
					PortProto pp = poly.getPort();
					found = forceChecking && layer.getFunction().isImplant();
					if (!found && pp != null)
					{
						Network net = info.getNetlist().getNetwork(ni, pp, 0);
						found = searchNetworkInParent(net, info);
					}
					//	if (!forceChecking && !found) continue;
					if (!found) continue;
				}
				// it has to take into account poly and transistor poly as one layer
				if (layer.getFunction().isPoly())
				{
					if (polyLayer == null)
						polyLayer = layer;
					layer = polyLayer;
				}
				poly.roundPoints(); // Trying to avoid mismatches while joining areas.
				poly.transform(trans);
				// otherTypeMerge is null if nonExported nodes are analyzed
				if (otherTypeMerge == null || layer.getFunction().isMetal() || layer.getFunction().isPoly())
					mainMerge.add(layer, new PolyQTree.PolyNode(poly.getBounds2D()), false);
				else
					otherTypeMerge.add(layer, new PolyQTree.PolyNode(poly.getBounds2D()), false);
			}

            return true;
		}
		
		/**
		 * Method to add node after network was checked
		 * @param ni
		 * @param pNp
		 * @param forceChecking
		 * @param info
		 * @param merge1
		 * @param merge2
		 */ 
		private void addNodeInst(NodeInst ni, PrimitiveNode pNp, boolean forceChecking,
		                           HierarchyEnumerator.CellInfo info, AffineTransform trans,
		                           GeometryHandler merge1, GeometryHandler merge2)
		{
			Technology tech = pNp.getTechnology();
			
			// electrical should not be null due to ports but causes
			// problems with poly and transistor-poly
			Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, null, true, true);
			int tot = nodeInstPolyList.length;
			for(int i=0; i<tot; i++)
			{
				Poly poly = nodeInstPolyList[i];
				Layer layer = poly.getLayer();

				// No area rule associated
				if (minAreaLayerMap.get(layer) == null && enclosedAreaLayerMap.get(layer) == null)
					continue;

				if (info != null)
				{
					// make sure this layer connects electrically to the desired port
					PortProto pp = poly.getPort();
					boolean found = forceChecking && layer.getFunction().isImplant();
					if (!found && pp != null)
					{
						Network net = info.getNetlist().getNetwork(ni, pp, 0);
						found = searchNetworkInParent(net, info);
					}
					//	if (!forceChecking && !found) continue;
					if (!found) continue;   
				}

				// it has to take into account poly and transistor poly as one layer
				if (layer.getFunction().isPoly())
				{
					if (polyLayer == null)
						polyLayer = layer;
					layer = polyLayer;
				}
				poly.roundPoints(); // Trying to avoid mismatches while joining areas.
				poly.transform(trans);
				if (layer.getFunction().isMetal() || layer.getFunction().isPoly())
					merge1.add(layer, new PolyQTree.PolyNode(poly.getBounds2D()), false);
				else
					merge2.add(layer, new PolyQTree.PolyNode(poly.getBounds2D()), false);
			}
		}
    }
}

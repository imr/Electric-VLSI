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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLog;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

/*
 * This is the "quick" DRC which does full hierarchical examination of the circuit.
 *
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
 *
 * Since Electric understands connectivity, it uses this information to determine whether two layers
 * are connected or not.  However, if such global connectivity is propagated in the standard Electric
 * way (placing numbers on exports, descending into the cell, and pulling the numbers onto local networks)
 * then it is not possible to decompose the DRC for multiple processors, since two different processors
 * may want to write global network information on the same local networks at once.
 *
 * To solve this problem, the "quick" DRC determines how many instances of each cell exist.  For every
 * network in every cell, an array is built that is as large as the number of instances of that cell.
 * This array contains the global network number for that each instance of the cell.  The algorithm for
 * building these arrays is quick (1 second for a million-transistor chip) and the memory requirement
 * is not excessive (8 megabytes for a million-transistor chip).  It uses the CheckInst and CheckProto
 * objects.
 */
public class Quick
{
	private static final double TINYDELTA = 0.001;

	// the different types of errors
	private static final int SPACINGERROR       = 1;
	private static final int MINWIDTHERROR      = 2;
	private static final int NOTCHERROR         = 3;
	private static final int MINSIZEERROR       = 4;
	private static final int BADLAYERERROR      = 5;
	private static final int LAYERSURROUNDERROR = 6;

	/*
	 * The CheckInst object is associated with every cell instance in the library.
	 * It helps determine network information on a global scale.
	 * It takes a "global-index" parameter, inherited from above (intially zero).
	 * It then computes its own index number as follows:
	 *   thisindex = global-index * multiplier + localIndex + offset
	 * This individual index is used to lookup an entry on each network in the cell
	 * (an array is stored on each network, giving its global net number).
	 */
	static class CheckInst
	{
		int localIndex;
		int multiplier;
		int offset;
	};
	private static HashMap checkInsts = null;


	/*
	 * The CheckProto object is placed on every cell and is used only temporarily
	 * to number the instances.
	 */
	static class CheckProto
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
	private static HashMap checkProtos = null;
	private static HashMap networkLists = null;


	/*
	 * The CHECKSTATE object exists for each processor and has global information for that thread.
	 */
//	typedef struct
//	{
//		POLYLIST  *nodeinstpolylist;
//		POLYLIST  *arcinstpolylist;
//		POLYLIST  *subpolylist;
//		POLYLIST  *cropnodepolylist;
//		POLYLIST  *croparcpolylist;
//		POLYLIST  *layerlookpolylist;
//		POLYLIST  *activecroppolylist;
//		POLYGON   *checkdistpoly1rebuild;
//		POLYGON   *checkdistpoly2rebuild;
//		INTBIG     globalindex;
//		NODEINST  *tinynodeinst;
//		GEOM      *tinyobj;
//		void      *hierarchybasesnapshot;		/* traversal path at start of instance interaction check */
//		void      *hierarchytempsnapshot;		/* traversal path when at bottom of one interaction check */
//	} CHECKSTATE;


	/**
	 * The InstanceInter object records interactions between two cell instances and prevents checking
	 * them multiple times.
	 */
	static class InstanceInter
	{
		/** the two cell instances being compared */	Cell cell1, cell2;
		/** rotation of cell instance 1 */				int rot1;
		/** mirroring of cell instance 1 */				boolean mirrorX1, mirrorY1;
		/** rotation of cell instance 2 */				int rot2;
		/** mirroring of cell instance 2 */				boolean mirrorX2, mirrorY2;
		/** distance from instance 1 to instance 2 */	double dx, dy;	
	};
	private static List instanceInteractionList = new ArrayList();


	/**
	 * The DRCExclusion object lists areas where Generic:DRC-Nodes exist to ignore errors.
	 */
	static class DRCExclusion
	{
		Cell cell;
		Poly poly;
	};
	private static List exclusionList = new ArrayList();


	/** number of processes for doing DRC */					private static int numberOfThreads;
	/** true to report only 1 error per Cell. */				private static boolean onlyFirstError;
	/** true to ignore center cuts in large contacts. */		private static boolean ignoreCenterCuts;
	/** maximum area to examine (the worst spacing rule). */	private static double worstInteractionDistance;
	/** time stamp for numbering networks. */					private static int checkTimeStamp;
	/** for numbering networks. */								private static int checkNetNumber;
	/** total errors found in all threads. */					private static int totalErrorsFound;
	/** a NodeInst that is too tiny for its connection. */		private static NodeInst tinyNodeInst;
	/** the other Geometric in "tiny" errors. */				private static Geometric tinyGeometric;
	/** for tracking the time of good DRC. */					private static HashMap goodDRCDate = new HashMap();
	/** for tracking the time of good DRC. */					private static boolean haveGoodDRCDate;

	/* for figuring out which layers are valid for DRC */
	private static Technology layersValidTech = null;
	private static boolean [] layersValid;

	/* for tracking which layers interact with which nodes */
	private static Technology layerInterTech = null;
	private static HashMap layersInterNodes = null;
	private static HashMap layersInterArcs = null;

	/*
	 * This is the entry point for DRC.
	 *
	 * Method to do a hierarchical DRC check on cell "cell".
	 * If "count" is zero, check the entire cell.
	 * If "count" is nonzero, only check that many instances (in "nodesToCheck") and set the
	 * entry in "validity" TRUE if it is DRC clean.
	 * If "justArea" is TRUE, only check in the selected area.
	 */
	public static void doCheck(Cell cell, int count, NodeInst [] nodesToCheck, boolean [] validity, boolean justArea)
	{
		// get the current DRC options
		onlyFirstError = DRC.isOneErrorPerCell();
		ignoreCenterCuts = DRC.isIgnoreCenterCuts();
		numberOfThreads = DRC.getNumberOfThreads();

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

		// initialize all cells for hierarchical network numbering
		checkProtos = new HashMap();
		checkInsts = new HashMap();
		// initialize cells in tree for hierarchical network numbering
		Netlist netlist = cell.getNetlist(false);
		CheckProto cp = checkEnumerateProtos(cell, netlist);

		// see if any parameters are used below this cell
		if (cp.treeParameterized)
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
					JNetwork net = (JNetwork)nIt.next();
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
			JNetwork net = (JNetwork)nIt.next();
			enumeratedNets.put(net, new Integer(checkNetNumber));
			checkNetNumber++;
		}
		checkEnumerateNetworks(cell, cp, 0, enumeratedNets);

		if (count <= 0)
			System.out.println("Found " + checkNetNumber + " networks");

		// now search for DRC exclusion areas
		exclusionList.clear();
		accumulateExclusion(cell, EMath.MATID);

 		Rectangle2D bounds = null;
		if (justArea)
		{
			EditWindow wnd = EditWindow.getCurrent();
			bounds = Highlight.getHighlightedArea(wnd);
		}

		// now do the DRC
		haveGoodDRCDate = false;
		ErrorLog.initLogging("DRC");
		if (count == 0)
		{
			// just do standard DRC here
//			if (!dr_quickparalleldrc) begintraversehierarchy();
			checkThisCell(cell, 0, bounds, justArea);
//			if (!dr_quickparalleldrc) endtraversehierarchy();

			// sort the errors by layer
			ErrorLog.sortErrors();
		} else
		{
			// check only these "count" instances
			checkTheseInstances(cell, count, nodesToCheck, validity);
		}

		ErrorLog.termLogging(true);

		if (haveGoodDRCDate)
		{
			// some cells were sucessfully checked: save that information in the database
			SaveDRCDates job = new SaveDRCDates(goodDRCDate);
		}
	}

	/**
	 * Class to save good DRC dates in a new thread.
	 */
	protected static class SaveDRCDates extends Job
	{
		HashMap goodDRCDate;

		protected SaveDRCDates(HashMap goodDRCDate)
		{
			super("Remember DRC Successes", DRC.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.goodDRCDate = goodDRCDate;
			this.startJob();
		}

		public void doIt()
		{
			for(Iterator it = goodDRCDate.keySet().iterator(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				Long DRCDate = (Long)goodDRCDate.get(cell);
				DRC.setLastDRCDate(cell, new Date(DRCDate.longValue()));
			}
		}
	}

	/*************************** QUICK DRC CELL EXAMINATION ***************************/

	/*
	 * Method to check the contents of cell "cell" with global network index "globalIndex".
	 * Returns positive if errors are found, zero if no errors are found, negative on internal error.
	 */
	private static int checkThisCell(Cell cell, int globalIndex, Rectangle2D bounds, boolean justArea)
	{
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
			Rectangle2D subBounds = bounds;
			if (justArea)
			{
				if (!ni.getBounds().intersects(bounds)) continue;

				AffineTransform trans = ni.rotateIn();
				AffineTransform xTrnI = ni.translateIn();
				trans.preConcatenate(xTrnI);
//				transmult(xrot, xtrn, trans);
				subBounds = new Rectangle2D.Double();
				subBounds.setRect(bounds);
				EMath.transformRect(subBounds, trans);
			}

			CheckProto cp = getCheckProto((Cell)np);
			if (cp.cellChecked && !cp.cellParameterized) continue;

			// recursively check the subcell
			CheckInst ci = (CheckInst)checkInsts.get(ni);
			int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;
//			if (!dr_quickparalleldrc) downhierarchy(ni, np, 0);
			int retval = checkThisCell((Cell)np, localIndex, subBounds, justArea);
//			if (!dr_quickparalleldrc) uphierarchy();
			if (retval < 0) return(-1);
			if (retval > 0) allSubCellsStillOK = false;
		}

		// prepare to check cell
		CheckProto cp = getCheckProto(cell);
		cp.cellChecked = true;

		// if the cell hasn't changed since the last good check, stop now
		if (allSubCellsStillOK)
		{
			Date lastGoodDate = DRC.getLastDRCDate(cell);
			if (lastGoodDate != null)
			{
				Date lastChangeDate = cell.getRevisionDate();
				if (lastGoodDate.after(lastChangeDate)) return 0;
			}
		}

		// announce progress
		System.out.println("Checking cell " + cell.describe());

		// remember how many errors there are on entry
		int errCount = ErrorLog.numErrors();

		// now look at every primitive node and arc here
		totalErrorsFound = 0;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			boolean ret = false;
			if (ni.getProto() instanceof Cell)
			{
				ret = checkCellInst(ni, globalIndex);
			} else
			{
				ret = checkNodeInst(ni, globalIndex);
			}
			if (ret)
			{
				totalErrorsFound++;
				if (onlyFirstError) break;
			}
		}
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			if (checkArcInst(cp, ai, globalIndex))
			{
				totalErrorsFound++;
				if (onlyFirstError) break;
			}
		}

		// if there were no errors, remember that
		int localErrors = ErrorLog.numErrors() - errCount;
		if (localErrors == 0)
		{
			Long now = new Long(new Date().getTime());
			goodDRCDate.put(cell, now);
			haveGoodDRCDate = true;
			System.out.println("   No errors found");
		} else
		{
			System.out.println("   FOUND " + localErrors + " ERRORS");
		}

		return totalErrorsFound;
	}

	/*
	 * Method to check the design rules about nodeinst "ni".  Only check those
	 * other objects whose geom pointers are greater than this one (i.e. only
	 * check any pair of objects once).
	 * Returns true if an error was found.
	 */
	private static boolean checkNodeInst(NodeInst ni, int globalIndex)
	{
		Cell cell = ni.getParent();
		Netlist netlist = getCheckProto(cell).netlist;
		NodeProto np = ni.getProto();
		Technology tech = np.getTechnology();
		AffineTransform trans = ni.rotateOut();

		// get all of the polygons on this node
		NodeProto.Function fun = ni.getFunction();
		Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, null, true, ignoreCenterCuts);
		convertPseudoLayers(ni, nodeInstPolyList);
		int tot = nodeInstPolyList.length;

		// examine the polygons on this node
		boolean errorsFound = false;
		for(int j=0; j<tot; j++)
		{
			Poly poly = nodeInstPolyList[j];
			Layer layer = poly.getLayer();
			if (layer == null) continue;
			if (layer.isNonElectrical()) continue;
			poly.transform(trans);

			// determine network for this polygon
			int netNumber = getDRCNetNumber(netlist, poly.getPort(), ni, globalIndex);
			boolean ret = badBox(poly, layer, netNumber, tech, ni, trans, cell, globalIndex);
			if (ret)
			{
				if (onlyFirstError) return true;
				errorsFound = true;
			}
			ret = checkMinWidth(ni, layer, poly, tech);
			if (ret)
			{
				if (onlyFirstError) return true;
				errorsFound = true;
			}
			if (tech == layersValidTech && !layersValid[layer.getIndex()])
			{
				reportError(BADLAYERERROR, layersValidTech, null, cell, 0, 0, null,
					poly, ni, layer, -1, null, null, null, -1);
				if (onlyFirstError) return true;
				errorsFound = true;
			}

//	#ifdef SURROUNDRULES
//			// check surround if this layer is from a pure-layer node
//			if (fun == NPNODE)
//			{
//				count = drcsurroundrules(poly->tech, poly->layer, &surlayers, &surdist, &surrules);
//				for(i=0; i<count; i++)
//				{
//					if (dr_quickfindsurround(poly, surlayers[i], surdist[i], cell, state)) continue;
//					reportError(LAYERSURROUNDERROR, layersValidTech, 0, cell, surdist[i], 0,
//						surrules[i], poly, ni->geom, poly->layer, NONET,
//							NOPOLYGON, NOGEOM, surlayers[i], NONET);
//					if (onlyFirstError) return(TRUE);
//					errorsFound = TRUE;
//				}
//			}
//	#endif
		}

		// check node for minimum size
		DRC.NodeSizeRule sizeRule = DRC.getMinSize(np);
		if (sizeRule != null)
		{
			if (ni.getXSize() < sizeRule.sizeX || ni.getYSize() < sizeRule.sizeY)
			{
				SizeOffset so = tech.getSizeOffset(ni);
				double minSize = 0, actual = 0;
				if (sizeRule.sizeX - ni.getXSize() > sizeRule.sizeY - ni.getYSize())
				{
					minSize = sizeRule.sizeX - so.getLowXOffset() - so.getHighXOffset();
					actual = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
				} else
				{
					minSize = sizeRule.sizeY - so.getLowYOffset() - so.getHighYOffset();
					actual = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
				}
				reportError(MINSIZEERROR, tech, null, cell, minSize, actual, sizeRule.rule,
					null, ni, null, -1, null, null, null, -1);
			}
		}
		return errorsFound;
	}

	/*
	 * Method to check the design rules about arcinst "ai".
	 * Returns true if errors were found.
	 */
	private static boolean checkArcInst(CheckProto cp, ArcInst ai, int globalIndex)
	{
		// ignore arcs with no topology
		JNetwork net = cp.netlist.getNetwork(ai, 0);
		if (net == null) return false;
		Integer [] netNumbers = (Integer [])networkLists.get(net);

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
			boolean ret = badBox(poly, layer, netNumber, tech, ai, EMath.MATID, ai.getParent(), globalIndex);
			if (ret)
			{
				if (onlyFirstError) return true;
				errorsFound = true;
			}
			ret = checkMinWidth(ai, layer, poly, tech);
			if (ret)
			{
				if (onlyFirstError) return true;
				errorsFound = true;
			}
			if (tech == layersValidTech && !layersValid[layerNum])
			{
				reportError(BADLAYERERROR, tech, null, ai.getParent(), 0, 0, null,
					poly, ai, layer, 0, null, null, null, 0);
				if (onlyFirstError) return true;
				errorsFound = true;
			}
		}
		return errorsFound;
	}

	/*
	 * Method to check the design rules about cell instance "ni".  Only check other
	 * instances, and only check the parts of each that are within striking range.
	 * Returns true if an error was found.
	 */
	private static boolean checkCellInst(NodeInst ni, int globalIndex)
	{
		// get current position in traversal hierarchy
//		gethierarchicaltraversal(state->hierarchybasesnapshot);

		// look for other instances surrounding this one
		Rectangle2D nodeBounds = ni.getBounds();
		Rectangle2D searchBounds = new Rectangle2D.Double(
			nodeBounds.getMinX()-worstInteractionDistance,
			nodeBounds.getMinY()-worstInteractionDistance,
			nodeBounds.getWidth() + worstInteractionDistance*2,
			nodeBounds.getHeight() + worstInteractionDistance*2);
		Geometric.Search sea = new Geometric.Search(searchBounds, ni.getParent());
		for(;;)
		{
			Geometric geom = sea.nextObject();
			if (geom == null) break;
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
			AffineTransform downTrans = ni.rotateIn();
			AffineTransform tTransI = ni.translateIn();
			downTrans.preConcatenate(tTransI);
//			transmult(rTransI, tTransI, downTrans);
			EMath.transformRect(subBounds, downTrans);

			AffineTransform upTrans = ni.translateOut();
			AffineTransform rTrans = ni.rotateOut();
			upTrans.preConcatenate(rTrans);
//			transmult(tTrans, rTrans, upTrans);

			CheckInst ci = (CheckInst)checkInsts.get(ni);
			int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;

			// recursively search instance "ni" in the vicinity of "oNi"
//			if (!dr_quickparalleldrc) downhierarchy(ni, ni->proto, 0);
			checkCellInstContents(subBounds, (Cell)ni.getProto(), upTrans,
				localIndex, oNi, globalIndex);
//			if (!dr_quickparalleldrc) uphierarchy();
		}
		return false;
	}

	/*
	 * Method to recursively examine the area "bounds" in cell "cell" with global index "globalIndex".
	 * The objects that are found are transformed by "uptrans" to be in the space of a top-level cell.
	 * They are then compared with objects in "oNi" (which is in that top-level cell),
	 * which has global index "topGlobalIndex".
	 */
	private static boolean checkCellInstContents(Rectangle2D bounds, Cell cell,
		AffineTransform upTrans, int globalIndex, NodeInst oNi, int topGlobalIndex)
	{
		boolean errorsFound = false;
		Netlist netlist = getCheckProto(cell).netlist;
		Geometric.Search subsearch = new Geometric.Search(bounds, cell);
		for(;;)
		{
			Geometric geom = subsearch.nextObject();
			if (geom == null) break;
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				NodeProto np = ni.getProto();
				if (np instanceof Cell)
				{
					Rectangle2D subBounds = new Rectangle2D.Double();
					subBounds.setRect(bounds);
					AffineTransform rTransI = ni.rotateIn();
					AffineTransform tTransI = ni.translateIn();
					rTransI.preConcatenate(tTransI);
//					transmult(rTransI, tTransI, trans);
					EMath.transformRect(subBounds, rTransI);

					AffineTransform subUpTrans = ni.translateOut();
					AffineTransform rTrans = ni.rotateOut();
					subUpTrans.preConcatenate(rTrans);
					subUpTrans.preConcatenate(upTrans);
//					transmult(tTrans, rTrans, trans);
//					transmult(trans, upTrans, subUpTrans);

					CheckInst ci = (CheckInst)checkInsts.get(ni);
					int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;

//					if (!dr_quickparalleldrc) downhierarchy(ni, ni->proto, 0);
					checkCellInstContents(subBounds, (Cell)np,
						subUpTrans, localIndex, oNi, topGlobalIndex);
//					if (!dr_quickparalleldrc) uphierarchy();
				} else
				{
					AffineTransform rTrans = ni.rotateOut();
					rTrans.preConcatenate(upTrans);
//					transmult(rtrans, uptrans, trans);
					Technology tech = np.getTechnology();
					Poly [] primPolyList = tech.getShapeOfNode(ni, null, true, ignoreCenterCuts);
					convertPseudoLayers(ni, primPolyList);
					int tot = primPolyList.length;
					for(int j=0; j<tot; j++)
					{
						Poly poly = primPolyList[j];
						Layer layer = poly.getLayer();
						if (layer == null) continue;
						if (layer.isNonElectrical()) continue;
						poly.transform(rTrans);

						// determine network for this polygon
						int net = getDRCNetNumber(netlist, poly.getPort(), ni, globalIndex);
						boolean ret = badSubBox(poly, layer, net, tech, ni, rTrans,
							globalIndex, cell, oNi, topGlobalIndex);
						if (ret)
						{
							if (onlyFirstError) return true;
							errorsFound = true;
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
					JNetwork jNet = netlist.getNetwork(ai, 0);
					int net = -1;
					if (jNet != null)
					{
						Integer [] netList = (Integer [])networkLists.get(jNet);
						net = netList[globalIndex].intValue();
					}
					boolean ret = badSubBox(poly, layer, net, tech, ai, upTrans,
						globalIndex, cell, oNi, topGlobalIndex);
					if (ret)
					{
						if (onlyFirstError) return true;
						errorsFound = true;
					}
				}
			}
		}
		return errorsFound;
	}

	/*
	 * Method to examine polygon "poly" layer "layer" network "net" technology "tech" geometry "geom"
	 * which is in cell "cell" and has global index "globalIndex".
	 * The polygon is compared against things inside node "oNi", and that node's parent has global index "topGlobalIndex". 
	 */
	private static boolean badSubBox(Poly poly, Layer layer, int net, Technology tech, Geometric geom, AffineTransform trans,
		int globalIndex, Cell cell, NodeInst oNi, int topGlobalIndex)
	{
		// see how far around the box it is necessary to search
		double bound = DRC.getMaxSurround(layer);
		if (bound < 0) return false;

		// get bounds
		Rectangle2D bounds = new Rectangle2D.Double();
		bounds.setRect(poly.getBounds2D());
		AffineTransform downTrans = oNi.rotateIn();
		AffineTransform tTransI = oNi.translateIn();
		downTrans.preConcatenate(tTransI);
//		transmult(rTransI, tTransI, downtrans);
		EMath.transformRect(bounds, downTrans);
		double minSize = poly.getMinSize();

		AffineTransform upTrans = oNi.translateOut();
		AffineTransform rTrans = oNi.rotateOut();
		upTrans.preConcatenate(rTrans);
//		transmult(tTrans, rtrans, upTrans);

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

	/*
	 * Method to examine a polygon to see if it has any errors with its surrounding area.
	 * The polygon is "poly" on layer "layer" on network "net" from technology "tech" from object "geom".
	 * Checking looks in cell "cell" global index "globalIndex".
	 * Object "geom" can be transformed to the space of this cell with "trans".
	 * Returns TRUE if a spacing error is found relative to anything surrounding it at or below
	 * this hierarchical level.
	 */
	private static boolean badBox(Poly poly, Layer layer, int net, Technology tech, Geometric geom,
		AffineTransform trans, Cell cell, int globalIndex)
	{
		// see how far around the box it is necessary to search
		double bound = DRC.getMaxSurround(layer);
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
			cell, globalIndex, EMath.MATID, minSize, baseMulti, true);
	}

	/*
	 * Method to recursively examine a polygon to see if it has any errors with its surrounding area.
	 * The polygon is "poly" on layer "layer" from technology "tech" on network "net" from object "geom"
	 * which is associated with global index "globalIndex".
	 * Checking looks in the area (lxbound-hxbound, lybound-hybound) in cell "cell" global index "cellGlobalIndex".
	 * The polygon coordinates are in the space of cell "topCell", global index "topGlobalIndex",
	 * and objects in "cell" can be transformed by "topTrans" to get to this space.
	 * The base object, in "geom" can be transformed by "trans" to get to this space.
	 * The minimum size of this polygon is "minSize" and "baseMulti" is TRUE if it comes from a multicut contact.
	 * If the two objects are in the same cell instance (nonhierarchical DRC), then "sameInstance" is TRUE.
	 * If they are from different instances, then "sameInstance" is FALSE.
	 *
	 * Returns TRUE if errors are found.
	 */
	private static boolean badBoxInArea(Poly poly, Layer layer, Technology tech, int net, Geometric geom, AffineTransform trans,
		int globalIndex,
		Rectangle2D bounds, Cell cell, int cellGlobalIndex,
		Cell topCell, int topGlobalIndex, AffineTransform topTrans, double minSize, boolean baseMulti,
		boolean sameInstance)
	{
		Rectangle2D rBound = new Rectangle2D.Double();
		rBound.setRect(bounds);
		EMath.transformRect(rBound, topTrans);
		Netlist netlist = getCheckProto(cell).netlist;
		Geometric.Search sea = new Geometric.Search(bounds, cell);
		int count = 0;
		for(;;)
		{
			Geometric nGeom = sea.nextObject();
			if (nGeom == null) break;
			if (sameInstance && (nGeom == geom)) continue;
			if (nGeom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)nGeom;
				NodeProto np = ni.getProto();

				// ignore nodes that are not primitive
				if (np instanceof Cell)
				{
					// instance found: look inside it for offending geometry
					AffineTransform rTransI = ni.rotateIn();
					AffineTransform tTransI = ni.translateIn();
					rTransI.preConcatenate(tTransI);
//					transmult(rTransI, ttrans, temptrans);
					Rectangle2D subBound = new Rectangle2D.Double();
					subBound.setRect(bounds);
					EMath.transformRect(subBound, rTransI);

					CheckInst ci = (CheckInst)checkInsts.get(ni);
					int localIndex = cellGlobalIndex * ci.multiplier + ci.localIndex + ci.offset;

					AffineTransform subTrans = ni.translateOut();
					AffineTransform rTrans = ni.rotateOut();
					subTrans.preConcatenate(rTrans);
					subTrans.preConcatenate(topTrans);
//					transmult(ttrans, rtrans, temptrans);
//					transmult(temptrans, topTrans, subtrans);

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

					// see if the objects directly touch
					boolean touch = objectsTouch(nGeom, geom);

					// prepare to examine every layer in this nodeinst
					AffineTransform rTrans = ni.rotateOut();
					rTrans.preConcatenate(topTrans);
//					transmult(rtrans, topTrans, subtrans);

					// get the shape of each nodeinst layer
					Poly [] subPolyList = tech.getShapeOfNode(ni, null, true, ignoreCenterCuts);
					convertPseudoLayers(ni, subPolyList);
					int tot = subPolyList.length;
					for(int i=0; i<tot; i++)
						subPolyList[i].transform(rTrans);
					boolean multi = baseMulti;
					if (!multi) multi = isMultiCut(ni);
					for(int j=0; j<tot; j++)
					{
						Poly npoly = subPolyList[j];
						Layer nLayer = npoly.getLayer();
						if (nLayer == null) continue;
						if (nLayer.isNonElectrical()) continue;

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
						DRC.Rule dRule = getAdjustedMinDist(tech, layer, minSize,
							nLayer, nMinSize, con, multi);
						DRC.Rule eRule = DRC.getEdgeRule(layer, nLayer);
						if (dRule == null && eRule == null) continue;
						double dist = -1;
						boolean edge = false;
						String rule;
						if (dRule != null)
						{
							dist = dRule.distance;
							rule = dRule.rule;
						} else
						{
							dist = eRule.distance;
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
				boolean touch = objectsTouch(nGeom, geom);

				// see whether the two objects are electrically connected
				JNetwork jNet = netlist.getNetwork(ai, 0);
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
					subPolyList[i].transform(topTrans);
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
					DRC.Rule dRule = getAdjustedMinDist(tech, layer, minSize,
						nLayer, nMinSize, con, multi);
					DRC.Rule eRule = DRC.getEdgeRule(layer, nLayer);
					if (dRule == null && eRule == null) continue;
					double dist = -1;
					boolean edge = false;
					String rule;
					if (dRule != null)
					{
						dist = dRule.distance;
						rule = dRule.rule;
					} else
					{
						dist = eRule.distance;
						rule = eRule.rule;
						edge = true;
					}

					// check the distance
					boolean ret = checkDist(tech, topCell, topGlobalIndex,
						poly, layer, net, geom, trans, globalIndex,
						nPoly, nLayer, nNet, nGeom, topTrans, cellGlobalIndex,
						con, dist, edge, rule);
					if (ret) return true;
				}
			}
		}
		return false;
	}

	/*
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
	private static boolean checkDist(Technology tech, Cell cell, int globalIndex,
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
//boolean debug = false;
//if (layer1.getName().equalsIgnoreCase("N-Select") && layer2.getName().equalsIgnoreCase("N-Select")) debug = true;
//if (layer2.getName().equalsIgnoreCase("N-Select") && layer1.getName().equalsIgnoreCase("N-Select")) debug = true;
//if (debug)
//{
// System.out.println("Comparing layer "+layer1.getName()+" is "+trueBox1.getMinX()+"<=X<="+trueBox1.getMaxX()+" and "+trueBox1.getMinY()+"<=Y<="+trueBox1.getMaxY());
// System.out.println("     with layer "+layer2.getName()+" is "+trueBox2.getMinX()+"<=X<="+trueBox2.getMaxX()+" and "+trueBox2.getMinY()+"<=Y<="+trueBox2.getMaxY());
//}

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
		if (isBox1 != null && isBox2 != null)
		{
			// manhattan
			double pdx = Math.max(trueBox2.getMinX()-trueBox1.getMaxX(), trueBox1.getMinX()-trueBox2.getMaxX());
			double pdy = Math.max(trueBox2.getMinY()-trueBox1.getMaxY(), trueBox1.getMinY()-trueBox2.getMaxY());
			pd = Math.max(pdx, pdy);
			if (pdx == 0 && pdy == 0) pd = 1;
			if (maytouch)
			{
				// they are electrically connected: see if they touch
				if (pd <= 0)
				{
					// they are electrically connected and they touch: look for minimum size errors
					DRC.Rule wRule = DRC.getMinWidth(layer1);
					if (wRule != null)
					{
						double minWidth = wRule.distance;
						String sizeRule = wRule.rule;
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
									reportError(MINWIDTHERROR, tech, null, cell, minWidth,
										actual, sizeRule, rebuild,
											geom1, layer1, -1, null, null, null, -1);
									return true;
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
									reportError(MINWIDTHERROR, tech, null, cell, minWidth,
										actual, sizeRule, rebuild,
											geom1, layer1, -1, null, null, null, -1);
									return true;
								}
							}
						}
					}
				}
			}

			// crop out parts of any arc that is covered by an adjoining node
			if (geom1 instanceof NodeInst)
			{
				if (cropNodeInst((NodeInst)geom1, globalIndex1, trans1,
					trueBox1, layer2, net2, geom2, trueBox2))
						return false;
			} else
			{
				if (cropArcInst((ArcInst)geom1, layer1, trueBox1))
					return false;
			}
			if (geom2 instanceof NodeInst)
			{
				if (cropNodeInst((NodeInst)geom2, globalIndex2, trans2,
					trueBox2, layer1, net1, geom1, trueBox1))
						return false;
			} else
			{
				if (cropArcInst((ArcInst)geom2, layer2, trueBox2))
					return false;
			}
//if (debug)
//{
// System.out.println("   Cropped layer "+layer1.getName()+" is "+trueBox1.getMinX()+"<=X<="+trueBox1.getMaxX()+" and "+trueBox1.getMinY()+"<=Y<="+trueBox1.getMaxY());
// System.out.println("       and layer "+layer2.getName()+" is "+trueBox2.getMinX()+"<=X<="+trueBox2.getMaxX()+" and "+trueBox2.getMinY()+"<=Y<="+trueBox2.getMaxY());
//}
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
				if (pdx == 0 && pdy == 0) pd = 1; else
				{
					pd = Math.max(pdx, pdy);

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
			if (poly1.intersects(poly2)) pd = 0; else
			{
				// find distance between polygons
				pd = poly1.separation(poly2);
			}
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
		if (activeOnTransistor(poly1, layer1, net1,
			poly2, layer2, net2, tech, cell, globalIndex)) return false;

		// special cases if the layers are the same
		if (tech.sameLayer(layer1, layer2))
		{
			// special case: check for "notch"
			if (maytouch)
			{
				// if they touch, it is acceptable
				if (pd <= 0) return false;

				// see if the notch is filled
				Point2D pt1 = new Point2D.Double();
				Point2D pt2 = new Point2D.Double();
				int intervening = findInterveningPoints(poly1, poly2, pt1, pt2);
				if (intervening == 0) return false;
				boolean needBoth = true;
				if (intervening == 1) needBoth = false;
				if (lookForPoints(pt1, pt2, layer1, cell, needBoth))
				{
					return false;
				}

				// look further if on the same net and diagonally separate (1 intervening point)
				if (net1 == net2 && intervening == 1) return false;
				errorType = NOTCHERROR;
			}
		}

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

		reportError(errorType, tech, msg, cell, dist, pd, rule,
			origPoly1, geom1, layer1, net1,
			origPoly2, geom2, layer2, net2);
		return true;
	}

	/*************************** QUICK DRC SEE IF INSTANCES CAUSE ERRORS ***************************/

	/*
	 * Method to examine, in cell "cell", the "count" instances in "nodesToCheck".
	 * If they are DRC clean, set the associated entry in "validity" to TRUE.
	 */
	private static void checkTheseInstances(Cell cell, int count, NodeInst [] nodesToCheck, boolean [] validity)
	{
		int globalIndex = 0;
		Netlist netlist = getCheckProto(cell).netlist;

		// loop through all of the instances to be checked
		for(int i=0; i<count; i++)
		{
			NodeInst ni = nodesToCheck[i];
			validity[i] = true;

			// look for other instances surrounding this one
			Rectangle2D nodeBounds = ni.getBounds();
			Rectangle2D searchBounds = new Rectangle2D.Double(
				nodeBounds.getMinX()-worstInteractionDistance,
				nodeBounds.getMinY()-worstInteractionDistance,
				nodeBounds.getWidth() + worstInteractionDistance*2,
				nodeBounds.getHeight() + worstInteractionDistance*2);
			Geometric.Search search = new Geometric.Search(searchBounds, cell);
			for(;;)
			{
				Geometric geom = search.nextObject();
				if (geom == null) break;
				if (geom instanceof ArcInst)
				{
					if (checkGeomAgainstInstance(netlist, geom, ni))
					{
						validity[i] = false;
						break;
					}
					continue;
				}
				NodeInst oNi = (NodeInst)geom;
				if (oNi.getProto() instanceof PrimitiveNode)
				{
					// found a primitive node: check it against the instance contents
					if (checkGeomAgainstInstance(netlist, geom, ni))
					{
						validity[i] = false;
						break;
					}
					continue;
				}

				// ignore if it is one of the instances in the list
				boolean found = false;
				for(int j=0; j<count; j++)
					if (oNi == nodesToCheck[j]) { found = true;   break; }
				if (found) continue;

				// found other instance "oNi", look for everything in "ni" that is near it
				Rectangle2D subNodeBounds = oNi.getBounds();
				Rectangle2D subBounds = new Rectangle2D.Double(
					subNodeBounds.getMinX()-worstInteractionDistance,
					subNodeBounds.getMinY()-worstInteractionDistance,
					subNodeBounds.getWidth() + worstInteractionDistance*2,
					subNodeBounds.getHeight() + worstInteractionDistance*2);
				AffineTransform downTrans = ni.rotateIn();
				AffineTransform tTransI = ni.translateIn();
				downTrans.preConcatenate(tTransI);
//				transmult(rtrans, ttrans, downTrans);
				EMath.transformRect(subBounds, downTrans);

				AffineTransform upTrans = ni.translateOut();
				AffineTransform rTrans = ni.rotateOut();
				upTrans.preConcatenate(rTrans);
//				transmult(ttrans, rtrans, upTrans);

				CheckInst ci = (CheckInst)checkInsts.get(ni);
				int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;

				// recursively search instance "ni" in the vicinity of "oNi"
				if (checkCellInstContents(subBounds, (Cell)ni.getProto(), upTrans,
					localIndex, oNi, globalIndex))
				{
					// errors were found: bail
					validity[i] = false;
					break;
				}
			}
		}
	}

	/*
	 * Method to check primitive object "geom" (an arcinst or primitive nodeinst) against cell instance "ni".
	 * Returns TRUE if there are design-rule violations in their interaction.
	 */
	private static boolean checkGeomAgainstInstance(Netlist netlist, Geometric geom, NodeInst ni)
	{
		NodeProto np = ni.getProto();
		int globalIndex = 0;
		Cell subCell = geom.getParent();
		boolean baseMulti = false;
		Technology tech = null;
		Poly [] nodeInstPolyList;
		AffineTransform trans = EMath.MATID;
		if (geom instanceof NodeInst)
		{
			// get all of the polygons on this node
			NodeInst oNi = (NodeInst)geom;
			tech = oNi.getProto().getTechnology();
			trans = oNi.rotateOut();
			nodeInstPolyList = tech.getShapeOfNode(ni, null, true, ignoreCenterCuts);
			convertPseudoLayers(ni, nodeInstPolyList);
			baseMulti = isMultiCut(oNi);
		} else
		{
			ArcInst oAi = (ArcInst)geom;
			tech = oAi.getProto().getTechnology();
			nodeInstPolyList = tech.getShapeOfArc(oAi);
		}
		int tot = nodeInstPolyList.length;

		CheckInst ci = (CheckInst)checkInsts.get(ni);
		int localIndex = globalIndex * ci.multiplier + ci.localIndex + ci.offset;

		// examine the polygons on this node
		for(int j=0; j<tot; j++)
		{
			Poly poly = nodeInstPolyList[j];
			Layer polyLayer = poly.getLayer();
			if (polyLayer == null) continue;

			// see how far around the box it is necessary to search
			double bound = DRC.getMaxSurround(polyLayer);
			if (bound < 0) continue;

			// determine network for this polygon
			int net;
			if (geom instanceof NodeInst)
			{
				net = getDRCNetNumber(netlist, poly.getPort(), (NodeInst)geom, globalIndex);
			} else
			{
				ArcInst oAi = (ArcInst)geom;
				JNetwork jNet = netlist.getNetwork(oAi, 0);
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
//			transmult(rtrans, ttrans, temptrans);
			EMath.transformRect(subBounds, tempTrans);

			AffineTransform subTrans = ni.translateOut();
			AffineTransform rTrans = ni.rotateOut();
			subTrans.preConcatenate(rTrans);
//			transmult(ttrans, rtrans, subTrans);

			// see if this polygon has errors in the cell
			if (badBoxInArea(poly, polyLayer, tech, net, geom, trans, globalIndex,
				subBounds, (Cell)np, localIndex,
				subCell, globalIndex, subTrans, minSize, baseMulti, false)) return true;
		}
		return false;
	}

	/*************************** QUICK DRC CACHE OF INSTANCE INTERACTIONS ***************************/

	/*
	 * Method to look for an interaction between instances "ni1" and "ni2".  If it is found,
	 * return TRUE.  If not found, add to the list and return FALSE.
	 */
	private static boolean checkInteraction(NodeInst ni1, NodeInst ni2)
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

		dii.dx = ni2.getGrabCenterX() - ni1.getGrabCenterX();
		dii.dy = ni2.getGrabCenterY() - ni1.getGrabCenterY();

		// if found, stop now
		if (findInteraction(dii)) return true;

		// insert it now
		instanceInteractionList.add(dii);

		return false;
	}

	/*
	 * Method to remove all instance interaction information.
	 */
	private static void clearInstanceCache()
	{
		instanceInteractionList.clear();
	}

	/*
	 * Method to look for the instance-interaction in "dii" in the global list of instances interactions
	 * that have already been checked.  Returns the entry if it is found, NOINSTINTER if not.
	 */ 
	private static boolean findInteraction(InstanceInter dii)
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

	/*
	 * Method to recursively examine the hierarchy below cell "cell" and create
	 * CheckProto and CheckInst objects on every cell instance.
	 */
	private static CheckProto checkEnumerateProtos(Cell cell, Netlist netlist)
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

	/*
	 * Method to return CheckProto of a Cell.
	 * @param cell Cell to get its CheckProto.
	 * @return CheckProto of a Cell.
	 */
	private static final CheckProto getCheckProto(Cell cell)
	{
		return (CheckProto) checkProtos.get(cell);
	}

	/*
	 * Method to recursively examine the hierarchy below cell "cell" and fill in the
	 * CheckInst objects on every cell instance.  Uses the CheckProto objects
	 * to keep track of cell usage.
	 */
	private static void checkEnumerateInstances(Cell cell)
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

	private static void checkEnumerateNetworks(Cell cell, CheckProto cp, int globalIndex, HashMap enumeratedNets)
	{
		// store all network information in the appropriate place
		for(Iterator nIt = cp.netlist.getNetworks(); nIt.hasNext(); )
		{
			JNetwork net = (JNetwork)nIt.next();
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
				JNetwork net = cp.netlist.getNetwork(ni, subPP, 0);
				if (net == null) continue;
				Integer netNumber = (Integer)enumeratedNets.get(net);
				JNetwork subNet = subCP.netlist.getNetwork(subPP, 0);
				subEnumeratedNets.put(subNet, netNumber);
			}
			for(Iterator nIt = subCP.netlist.getNetworks(); nIt.hasNext(); )
			{
				JNetwork net = (JNetwork)nIt.next();
				if (subEnumeratedNets.get(net) == null)
					subEnumeratedNets.put(net, new Integer(checkNetNumber++));
			}
			checkEnumerateNetworks(subCell, subCP, localIndex, subEnumeratedNets);
		}
	}

	/*********************************** QUICK DRC SUPPORT ***********************************/

	/*
	 * Method to ensure that polygon "poly" on layer "layer" from object "geom" in
	 * technology "tech" meets minimum width rules.  If it is too narrow, other layers
	 * in the vicinity are checked to be sure it is indeed an error.  Returns true
	 * if an error is found.
	 */
	private static boolean checkMinWidth(Geometric geom, Layer layer, Poly poly, Technology tech)
	{
		Cell cell = geom.getParent();
		DRC.Rule minWidthRule = DRC.getMinWidth(layer);
		if (minWidthRule == null) return false;
		double minWidth = minWidthRule.distance;

		// simpler analysis if manhattan
		Rectangle2D bounds = poly.getBox();
		if (bounds != null)
		{
			if (bounds.getWidth() >= minWidth && bounds.getHeight() >= minWidth) return false;
			double actual = 0;
			Point2D left1, left2, left3, right1, right2, right3;
			if (bounds.getWidth() < minWidth)
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
			if (lookForLayer(cell, layer, EMath.MATID, newBounds,
				left1, left2, left3, pointsFound)) return false;

			pointsFound[0] = pointsFound[1] = pointsFound[2] = false;
			if (lookForLayer(cell, layer, EMath.MATID, newBounds,
				right1, right2, right3, pointsFound)) return false;

			reportError(MINWIDTHERROR, tech, null, cell, minWidth, actual, minWidthRule.rule,
				poly, geom, layer, -1, null, null, null, -1);
			return true;
		}

		// nonmanhattan polygon: stop now if it has no size
		Poly.Type style = poly.getStyle();
		if (style != Poly.Type.FILLED && style != Poly.Type.CLOSED && style != Poly.Type.CROSSED &&
			style != Poly.Type.OPENED && style != Poly.Type.OPENEDT1 && style != Poly.Type.OPENEDT2 &&
			style != Poly.Type.OPENEDT3 && style != Poly.Type.VECTORS) return false;

		// simple check of nonmanhattan polygon for minimum width
		bounds = poly.getBounds2D();
		double actual = Math.min(bounds.getWidth(), bounds.getHeight());
		if (actual < minWidth)
		{
			reportError(MINWIDTHERROR, tech, null, cell, minWidth, actual, minWidthRule.rule,
				poly, geom, layer, -1, null, null, null, -1);
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
			
			double ang = EMath.figureAngleRadians(from, to);
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
				double oAng = EMath.figureAngleRadians(oFrom, oTo);
				double rAng = ang;   while (rAng > Math.PI) rAng -= Math.PI;
				double rOAng = oAng;   while (rOAng > Math.PI) rOAng -= Math.PI;
				if (EMath.doublesEqual(oAng, rOAng))
				{
					// lines are parallel: see if they are colinear
					if (EMath.isOnLine(from, to, oFrom)) continue;
					if (EMath.isOnLine(from, to, oTo)) continue;
					if (EMath.isOnLine(oFrom, oTo, from)) continue;
					if (EMath.isOnLine(oFrom, oTo, to)) continue;
				}
				Point2D inter = EMath.intersectRadians(center, perpang, oFrom, oAng);
				if (inter == null) continue;
				if (inter.getX() < Math.min(oFrom.getX(), oTo.getX()) || inter.getX() > Math.max(oFrom.getX(), oTo.getX())) continue;
				if (inter.getY() < Math.min(oFrom.getY(), oTo.getY()) || inter.getY() > Math.max(oFrom.getY(), oTo.getY())) continue;
				double fdx = center.getX() - inter.getX();
				double fdy = center.getY() - inter.getY();
				actual = EMath.smooth(Math.sqrt(fdx*fdx + fdy*fdy));

				// becuase this is done in integer, accuracy may suffer
//				actual += 2;

				if (actual < minWidth)
				{
					// look between the points to see if it is minimum width or notch
					if (poly.isInside(new Point2D.Double((center.getX()+inter.getX())/2, (center.getY()+inter.getY())/2)))
					{
						reportError(MINWIDTHERROR, tech, null, cell, minWidth,
							actual, minWidthRule.rule, poly, geom, layer, -1, null, null, null, -1);
					} else
					{
						reportError(NOTCHERROR, tech, null, cell, minWidth,
							actual, minWidthRule.rule, poly, geom, layer, -1, poly, geom, layer, -1);
					}
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * Method to determine whether node "ni" is a multiple cut contact.
	 */
	private static boolean isMultiCut(NodeInst ni)
	{
		NodeProto np = ni.getProto();
		if (np instanceof Cell) return false;
		PrimitiveNode pnp = (PrimitiveNode)np;
		Technology tech = pnp.getTechnology();
		if (pnp.getSpecialType() != PrimitiveNode.MULTICUT) return false;
		double [] specialValues = pnp.getSpecialValues();
		Technology.MultiCutData mcd = new Technology.MultiCutData(ni, specialValues);
		if (mcd.numCuts() > 1) return true;
		return false;
	}

	/*
	 * Method to tell whether the objects at geometry modules "geom1" and "geom2"
	 * touch directly (that is, an arcinst connected to a nodeinst).  The method
	 * returns true if they touch
	 */
	private static boolean objectsTouch(Geometric geom1, Geometric geom2)
	{
		if (geom1 instanceof NodeInst)
		{
			if (geom2 instanceof NodeInst) return false;
			Geometric temp = geom1;   geom1 = geom2;   geom2 = temp;
		}
		if (!(geom2 instanceof NodeInst))
			return false;

		// see if the arcinst at "geom1" touches the nodeinst at "geom2"
		NodeInst ni = (NodeInst)geom2;
		ArcInst ai = (ArcInst)geom1;
		for(int i=0; i<2; i++)
		{
			Connection con = ai.getConnection(i);
			if (con.getPortInst().getNodeInst() == ni) return true;
		}
		return false;
	}

	/*
	 * Method to find two points between polygons "poly1" and "poly2" that can be used to test
	 * for notches.  The points are returned in (xf1,yf1) and (xf2,yf2).  Returns zero if no
	 * points exist in the gap between the polygons (becuase they don't have common facing edges).
	 * Returns 1 if only one of the reported points needs to be filled (because the polygons meet
	 * at a point).  Returns 2 if both reported points need to be filled.
	 */
	private static int findInterveningPoints(Poly poly1, Poly poly2, Point2D pt1, Point2D pt2)
	{
		Rectangle2D isBox1 = poly1.getBox();
		Rectangle2D isBox2 = poly2.getBox();
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
					pt1.setLocation(isBox1.getMaxX(), isBox1.getMaxY());
					pt2.setLocation(isBox2.getMinX(), isBox2.getMinY());
					return 1;
				}
				if (isBox2.getMinY() > isBox1.getMaxY())
				{
					pt1.setLocation(isBox1.getMaxX(), isBox1.getMinY());
					pt2.setLocation(isBox2.getMinX(), isBox2.getMaxY());
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

	/*
	 * Method to explore the points (xf1,yf1) and (xf2,yf2) to see if there is
	 * geometry on layer "layer" (in or below cell "cell").  Returns true if there is.
	 * If "needBoth" is true, both points must have geometry, otherwise only 1 needs it.
	 */
	private static boolean lookForPoints(Point2D pt1, Point2D pt2, Layer layer, Cell cell, boolean needBoth)
	{
		Point2D pt3 = new Point2D.Double((pt1.getX()+pt2.getX()) / 2, (pt1.getY()+pt2.getY()) / 2);

		// compute bounds for searching inside cells
		double flx = Math.min(pt1.getX(), pt2.getX());   double fhx = Math.max(pt1.getX(), pt2.getX());
		double fly = Math.min(pt1.getY(), pt2.getY());   double fhy = Math.max(pt1.getY(), pt2.getY());
		Rectangle2D bounds = new Rectangle2D.Double(flx, fly, fhx-flx, fhy-fly);

		// search the cell for geometry that fills the notch
		boolean [] pointsFound = new boolean[3];
		pointsFound[0] = pointsFound[1] = pointsFound[2] = false;
		boolean allFound = lookForLayer(cell, layer, EMath.MATID, bounds,
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

	/*
	 * Method to examine cell "cell" in the area (lx<=X<=hx, ly<=Y<=hy) for objects
	 * on layer "layer".  Apply transformation "moreTrans" to the objects.  If polygons are
	 * found at (xf1,yf1) or (xf2,yf2) or (xf3,yf3) then sets "p1found/p2found/p3found" to 1.
	 * If all locations are found, returns true.
	 */
	private static boolean lookForLayer(Cell cell, Layer layer, AffineTransform moreTrans,
		Rectangle2D bounds, Point2D pt1, Point2D pt2, Point2D pt3, boolean [] pointsFound)
	{
		Geometric.Search sea = new Geometric.Search(bounds, cell);
		for(;;)
		{
			Geometric g = sea.nextObject();
			if (g == null) break;
			if (g instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)g;
				if (ni.getProto() instanceof Cell)
				{
					// compute bounding area inside of sub-cell
					AffineTransform rotI = ni.rotateIn();
					AffineTransform transI = ni.translateIn();
					rotI.preConcatenate(transI);
//					transmult(rotI, transI, bound);
					Rectangle2D newBounds = new Rectangle2D.Double();
					newBounds.setRect(bounds);
					EMath.transformRect(newBounds, rotI);

					// compute new matrix for sub-cell examination
					AffineTransform trans = ni.translateOut();
					AffineTransform rot = ni.rotateOut();
					trans.preConcatenate(rot);
					trans.preConcatenate(moreTrans);
//					transmult(trans, rot, bound);
//					transmult(bound, moreTrans, trans);
					if (lookForLayer((Cell)ni.getProto(), layer, trans, newBounds,
						pt1, pt2, pt3, pointsFound))
							return true;
					continue;
				}
				AffineTransform bound = ni.rotateOut();
				bound.preConcatenate(moreTrans);
//				transmult(rot, moreTrans, bound);
				Technology tech = ni.getProto().getTechnology();
				Poly [] layerLookPolyList = tech.getShapeOfNode(ni, null, false, ignoreCenterCuts);
				int tot = layerLookPolyList.length;
				for(int i=0; i<tot; i++)
				{
					Poly poly = layerLookPolyList[i];
					if (!tech.sameLayer(poly.getLayer(), layer)) continue;
					poly.transform(bound);
					if (poly.isInside(pt1)) pointsFound[0] = true;
					if (poly.isInside(pt2)) pointsFound[1] = true;
					if (poly.isInside(pt3)) pointsFound[2] = true;
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
					if (poly.isInside(pt3)) pointsFound[2] = true;
				}
			}
			if (pointsFound[0] && pointsFound[1] && pointsFound[2])
				return true;
		}
		return false;
	}

	/*
	 * Method to see if the two boxes are active elements, connected to opposite
	 * sides of a field-effect transistor that resides inside of the box area.
	 * Returns true if so.
	 */
	private static boolean activeOnTransistor(Poly poly1, Layer layer1, int net1,
		Poly poly2, Layer layer2, int net2, Technology tech, Cell cell, int globalIndex)
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
		fun = layer2.getFunction();
		if (!fun.isDiff())
		{
			if (!fun.isContact() || (funExtras&Layer.Function.CONDIFF) == 0) return false;
		}

		// search for intervening transistor in the cell
		Rectangle2D bounds1 = poly1.getBounds2D();
		Rectangle2D bounds2 = poly2.getBounds2D();
		Rectangle2D.union(bounds1, bounds2, bounds1);
		return activeOnTransistorRecurse(bounds1, net1, net2, cell, globalIndex, EMath.MATID);
	}

	private static boolean activeOnTransistorRecurse(Rectangle2D bounds,
		int net1, int net2, Cell cell, int globalIndex, AffineTransform trans)
	{
		Netlist netlist = getCheckProto(cell).netlist;
		Geometric.Search sea = new Geometric.Search(bounds, cell);
		for(;;)
		{
			Geometric g = sea.nextObject();
			if (g == null) break;
			if (!(g instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)g;
			NodeProto np = ni.getProto();
			if (np instanceof Cell)
			{
				AffineTransform rTransI = ni.rotateIn();
				AffineTransform tTransI = ni.translateIn();
				rTransI.preConcatenate(tTransI);
//				transmult(rTransI, tTransI, temptrans);
				Rectangle2D subBounds = new Rectangle2D.Double();
				subBounds.setRect(bounds);
				EMath.transformRect(subBounds, rTransI);

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
			JNetwork badNet = netlist.getNetwork(ni, badport, 0);

			boolean on1 = false, on2 = false;
			for(Iterator it = ni.getPortInsts(); it.hasNext(); )
			{
				PortInst pi = (PortInst)it.next();

				// ignore connections on poly/gate
				JNetwork piNet = netlist.getNetwork(pi);
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

	/*
	 * Method to crop the box in the reference parameters (lx-hx, ly-hy)
	 * against the box in (bx-ux, by-uy).  If the box is cropped into oblivion,
	 * returns 1.  If the boxes overlap but cannot be cleanly cropped,
	 * returns -1.  Otherwise the box is cropped and zero is returned
	 */
	private static int cropBox(Rectangle2D bounds, Rectangle2D PUBox, Rectangle2D nBounds)
	{
		// if the two boxes don't touch, just return
		double lX = bounds.getMinX();   double hX = bounds.getMaxX();
		double lY = bounds.getMinY();   double hY = bounds.getMaxY();
		if (PUBox.getMinX() >= hX || PUBox.getMinY() >= hY || PUBox.getMaxX() <= lX || PUBox.getMaxY() <= lY) return(0);

		// if the box to be cropped is within the other, say so
		if (PUBox.getMinX() <= lX && PUBox.getMaxX() >= hX && PUBox.getMinY() <= lY && PUBox.getMaxY() >= hY) return(1);

		// see which direction is being cropped
		double xoverlap = Math.min(hX, PUBox.getMaxX()) - Math.max(lX, PUBox.getMinX());
		double yoverlap = Math.min(hY, PUBox.getMaxY()) - Math.max(lY, PUBox.getMinY());
		if (xoverlap > yoverlap)
		{
			// one above the other: crop in Y
			if (PUBox.getMinX() <= lX && PUBox.getMaxX() >= hX)
			{
				// it covers in X...do the crop
				if (PUBox.getMaxY() >= hY) hY = PUBox.getMinY();
				if (PUBox.getMinY() <= lY) lY = PUBox.getMaxY();
				if (hY <= lY) return(1);
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return(0);
			}
		} else
		{
			// one next to the other: crop in X
			if (PUBox.getMinY() <= lY && PUBox.getMaxY() >= hY)
			{
				// it covers in Y...crop in X
				if (PUBox.getMaxX() >= hX) hX = PUBox.getMinX();
				if (PUBox.getMinX() <= lX) lX = PUBox.getMaxX();
				if (hX <= lX) return(1);
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return(0);
			}
		}
		return -1;
	}

	/*
	 * Method to crop the box in the reference parameters (lx-hx, ly-hy)
	 * against the box in (bx-ux, by-uy).  If the box is cropped into oblivion,
	 * returns 1.  If the boxes overlap but cannot be cleanly cropped,
	 * returns -1.  Otherwise the box is cropped and zero is returned
	 */
	private static int halfCropBox(Rectangle2D bounds, Rectangle2D limit)
	{
		double bX = limit.getMinX();    double uX = limit.getMaxX();
		double bY = limit.getMinY();    double uY = limit.getMaxY();
		double lX = bounds.getMinX();   double hX = bounds.getMaxX();
		double lY = bounds.getMinY();   double hY = bounds.getMaxY();

		// if the two boxes don't touch, just return
		if (bX >= hX || bY >= hY || uX <= lX || uY <= lY) return(0);

		// if the box to be cropped is within the other, figure out which half to remove
		if (bX <= lX && uX >= hX && bY <= lY && uY >= hY)
		{
			double lxe = lX - bX;   double hxe = uX - hX;
			double lye = lY - bY;   double hye = uY - hY;
			double biggestExt = Math.max(Math.max(lxe, hxe), Math.max(lye, hye));
			if (lxe == biggestExt)
			{
				lX = (lX + uX) / 2;
				if (lX >= hX) return 1;
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return 0;
			}
			if (hxe == biggestExt)
			{
				hX = (hX + bX) / 2;
				if (hX <= lX) return 1;
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return 0;
			}
			if (lye == biggestExt)
			{
				lY = (lY + uY) / 2;
				if (lY >= hY) return 1;
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return 0;
			}
			if (hye == biggestExt)
			{
				hY = (hY + bY) / 2;
				if (hY <= lY) return 1;
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return 0;
			}
		}

		// reduce (lx-hx,lY-hy) bY (bX-uX,bY-uY)
		boolean crops = false;
		if (bX <= lX && uX >= hX)
		{
			// it covers in X...crop in Y
			if (uY >= hY) hY = (hY + bY) / 2;
			if (bY <= lY) lY = (lY + uY) / 2;
			bounds.setRect(lX, lY, hX-lX, hY-lY);
			crops = true;
		}
		if (bY <= lY && uY >= hY)
		{
			// it covers in Y...crop in X
			if (uX >= hX) hX = (hX + bX) / 2;
			if (bX <= lX) lX = (lX + uX) / 2;
			bounds.setRect(lX, lY, hX-lX, hY-lY);
			crops = true;
		}
		if (!crops) return -1;
		return 0;
	}

	/*
	 * Method to crop the box on layer "nLayer", electrical index "nNet"
	 * and bounds (lx-hx, ly-hy) against the nodeinst "ni".  The geometry in nodeinst "ni"
	 * that is being checked runs from (nlx-nhx, nly-nhy).  Only those layers
	 * in the nodeinst that are the same layer and the same electrical network
	 * are checked.  Returns true if the bounds are reduced
	 * to nothing.
	 */
	private static boolean cropNodeInst(NodeInst ni, int globalIndex,
		AffineTransform trans, Rectangle2D nBound,
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
				int temp = cropBox(bound, polyBox, nBound);
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

	/*
	 * Method to crop away any part of layer "lay" of arcinst "ai" that coincides
	 * with a similar layer on a connecting nodeinst.  The bounds of the arcinst
	 * are in the reference parameters (lx-hx, ly-hy).  Returns false
	 * normally, 1 if the arcinst is cropped into oblivion.
	 */
	private static boolean cropArcInst(ArcInst ai, Layer lay, Rectangle2D bounds)
	{
		for(int i=0; i<2; i++)
		{
			// find the primitive nodeinst at the true end of the portinst
			Connection con = ai.getConnection(i);
			PortInst pi = con.getPortInst();
			NodeInst ni = pi.getNodeInst();
			NodeProto np = ni.getProto();
			PortProto pp = pi.getPortProto();
			while (np instanceof Cell)
			{
				PortInst subPi = ((Export)pp).getOriginalPort();
				ni = subPi.getNodeInst();
				np = ni.getProto();
				pp = subPi.getPortProto();
			}
			AffineTransform trans = ni.rotateOut();
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
				int temp = halfCropBox(bounds, polyBox);
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

	/*
	 * Method to see if polygons in "pList" (describing arc "ai") should be cropped against a
	 * connecting transistor.  Crops the polygon if so.
	 */
	private static void cropActiveArc(ArcInst ai, Poly [] pList)
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
				if (halfCropBox(polyBounds, nPolyBounds) == 1)
				{
					// remove this polygon from consideration
					poly.setLayer(null);
					return;
				}
				cropped = true;
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
	private static void convertPseudoLayers(NodeInst ni, Poly [] pList)
	{
		if (ni.getProto().getFunction() != NodeProto.Function.PIN) return;
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
	private static void cacheValidLayers(Technology tech)
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

	/*
	 * Method to determine the minimum distance between "layer1" and "layer2" in technology
	 * "tech" and library "lib".  If "con" is true, the layers are connected.  Also forces
	 * connectivity for same-implant layers.
	 */
	private static DRC.Rule getAdjustedMinDist(Technology tech, Layer layer1, double size1,
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

		// see how close they can get
		double wideLimit = DRC.getWideLimit(tech);
		boolean wide = false;
		if (size1 >= wideLimit || size2 >= wideLimit) wide = true;

		DRC.Rule rule = DRC.getSpacingRule(layer1, layer2, con, wide, multi);
		return rule;
	}

	/*
	 * Method to return the network number for port "pp" on node "ni", given that the node is
	 * in a cell with global index "globalIndex".
	 */
	private static int getDRCNetNumber(Netlist netlist, PortProto pp, NodeInst ni, int globalIndex)
	{
		if (pp == null) return -1;

		// see if there is an arc connected
//		PortInst pi = ni.findPortInstFromProto(pp);
		JNetwork net = netlist.getNetwork(ni, pp, 0);
		Integer [] nets = (Integer [])networkLists.get(net);
		return nets[globalIndex].intValue();
	}

	/***************** LAYER INTERACTIONS ******************/

	/*
	 * Method to build the internal data structures that tell which layers interact with
	 * which primitive nodes in technology "tech".
	 */
	private static void buildLayerInteractions(Technology tech)
	{
		if (layerInterTech == tech) return;

		layerInterTech = tech;
		int numLayers = tech.getNumLayers();

		// build the node table
		layersInterNodes = new HashMap();
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			boolean [] layersInNode = new boolean[numLayers];
			for(int i=0; i<numLayers; i++) layersInNode[i] = false;

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

	/*
	 * Method to determine whether layer "layer" interacts in any way with a node of type "np".
	 * If not, returns FALSE.
	 */
	private static boolean checkLayerWithNode(Layer layer, NodeProto np)
	{
		buildLayerInteractions(np.getTechnology());

		// find this node in the table
		boolean [] validLayers = (boolean [])layersInterNodes.get(np);
		if (validLayers == null) return false;
		return validLayers[layer.getIndex()];
	}

	/*
	 * Method to determine whether layer "layer" interacts in any way with an arc of type "ap".
	 * If not, returns FALSE.
	 */
	private static boolean checkLayerWithArc(Layer layer, ArcProto ap)
	{
		buildLayerInteractions(ap.getTechnology());

		// find this node in the table
		boolean [] validLayers = (boolean [])layersInterArcs.get(ap);
		if (validLayers == null) return false;
		return validLayers[layer.getIndex()];
	}

	/*
	 * Method to recursively scan cell "cell" (transformed with "trans") searching
	 * for DRC Exclusion nodes.  Each node is added to the global list "exclusionList".
	 */
	private static void accumulateExclusion(Cell cell, AffineTransform trans)
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
				dex.poly.setStyle(Poly.Type.FILLED);

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
				AffineTransform tTrans = ni.translateOut();
				AffineTransform rTrans = ni.rotateOut();
				tTrans.preConcatenate(rTrans);
//				transmult(transt, transr, transtemp);
				accumulateExclusion((Cell)np, tTrans);
			}
		}
	}

	/*********************************** QUICK DRC ERROR REPORTING ***********************************/

	/* Adds details about an error to the error list */
	private static void reportError(int errorType, Technology tech, String msg,
		Cell cell, double limit, double actual, String rule,
		Poly poly1, Geometric geom1, Layer layer1, int net1,
		Poly poly2, Geometric geom2, Layer layer2, int net2)
	{
//		// if this error is being ignored, don't record it
//		var = getvalkey((INTBIG)cell, VNODEPROTO, VGEOM|VISARRAY, dr_ignore_listkey);
//		if (var != NOVARIABLE)
//		{
//			len = getlength(var);
//			for(i=0; i<len; i += 2)
//			{
//				p1 = ((GEOM **)var->addr)[i];
//				p2 = ((GEOM **)var->addr)[i+1];
//				if (p1 == geom1 && p2 == geom2) return;
//				if (p1 == geom2 && p2 == geom1) return;
//			}
//		}

		// if this error is in an ignored area, don't record it
		if (exclusionList.size() > 0)
		{
			// determine the bounding box of the error
			Rectangle2D polyBounds = poly1.getBounds2D();
			double lX = polyBounds.getMinX();   double hX = polyBounds.getMaxX();
			double lY = polyBounds.getMinY();   double hY = polyBounds.getMaxY();
			if (poly2 != null)
			{
				polyBounds = poly2.getBounds2D();
				if (polyBounds.getMinX() < lX) lX = polyBounds.getMinX();
				if (polyBounds.getMaxX() > hX) hX = polyBounds.getMaxX();
				if (polyBounds.getMinY() < lY) lY = polyBounds.getMinY();
				if (polyBounds.getMaxY() > hY) hY = polyBounds.getMaxY();
			}
			for(Iterator it = exclusionList.iterator(); it.hasNext(); )
			{
				DRCExclusion dex = (DRCExclusion)it.next();
				if (cell != dex.cell) continue;
				Poly poly = dex.poly;
				if (poly.isInside(new Point2D.Double(lX, lY)) &&
					poly.isInside(new Point2D.Double(lX, hY)) &&
					poly.isInside(new Point2D.Double(hX, lY)) &&
					poly.isInside(new Point2D.Double(hX, hY))) return;
			}
		}

		// describe the error
		String errorMessage = "";
		Cell np1 = geom1.getParent();
		int sortLayer = 0;
		if (errorType == SPACINGERROR || errorType == NOTCHERROR)
		{
			// describe spacing width error
			if (errorType == SPACINGERROR) errorMessage += "Spacing"; else
				errorMessage += "Notch";
			if (layer1 == layer2)
				errorMessage += " (layer " + layer1.getName() + ")";
			errorMessage += ": ";
			Cell np2 = geom2.getParent();
			if (np1 != np2)
			{
				errorMessage += "cell " + np1.describe() + ", ";
			} else if (np1 != cell)
			{
				errorMessage += "[in cell " + np1.describe() + "] ";
			}
			if (geom1 instanceof NodeInst)
				errorMessage += "node " + geom1.describe(); else
					errorMessage += "arc " + geom1.describe();
			if (layer1 != layer2)
				errorMessage += ", layer " + layer1.getName();

			if (actual < 0) errorMessage += " OVERLAPS "; else
				if (actual == 0) errorMessage += " TOUCHES "; else
					errorMessage += " LESS (BY " + (limit-actual) + ") THAN " + limit + " TO ";

			if (np1 != np2)
				errorMessage += "cell " + np2.describe() + ", ";
			if (geom2 instanceof NodeInst)
				errorMessage += "node " + geom2.describe(); else
					errorMessage += "arc " + geom2.describe();
			if (layer1 != layer2)
				errorMessage += ", layer " + layer2.getName();
			if (msg != null)
				errorMessage += "; " + msg;
			sortLayer = Math.min(layer1.getIndex(), layer2.getIndex());
		} else
		{
			// describe minimum width/size or layer error
			switch (errorType)
			{
				case MINWIDTHERROR:
					errorMessage += "Minimum width error:";
					break;
				case MINSIZEERROR:
					errorMessage += "Minimum size error:";
					break;
				case BADLAYERERROR:
					errorMessage += "Invalid layer (" + layer1.getName() + "):";
					break;
				case LAYERSURROUNDERROR:
					errorMessage += "Layer surround error:";
					break;
			}
			errorMessage += " cell " + np1.describe();
			if (geom1 instanceof NodeInst)
			{
				errorMessage += ", node " + geom1.describe();
			} else
			{
				errorMessage += ", arc " + geom1.describe();
			}
			if (errorType == MINWIDTHERROR)
			{
				errorMessage += ", layer " + layer1.getName();
				errorMessage += " LESS THAN " + limit + " WIDE (IS " + actual + ")";
			} else if (errorType == MINSIZEERROR)
			{
				errorMessage += " LESS THAN " + limit + " IN SIZE (IS " + actual + ")";
			} else if (errorType == LAYERSURROUNDERROR)
			{
				errorMessage += ", layer %" + layer1.getName();
				errorMessage += " NEEDS SURROUND OF LAYER " + layer2.getName() + " BY " + limit;
			}
			if (layer1 != null) sortLayer = layer1.getIndex();
		}
		if (rule != null) errorMessage += " [rule " + rule + "]";

		ErrorLog err = ErrorLog.logError(errorMessage, cell, sortLayer);
		boolean showGeom = true;
		if (poly1 != null) { showGeom = false;   err.addPoly(poly1); }
		if (poly2 != null) { showGeom = false;   err.addPoly(poly2); }
		err.addGeom(geom1, showGeom, 0, null);
		if (geom2 != null) err.addGeom(geom2, showGeom, 0, null);
	}

}

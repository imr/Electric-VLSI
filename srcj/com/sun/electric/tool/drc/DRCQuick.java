/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DRCQuick.java
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
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.JNetwork;
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
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.ErrorLog;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

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
 * is not excessive (8 megabytes for a million-transistor chip).  It uses the CHECKINST and CHECKPROTO
 * objects.
 */
public class DRCQuick
{
	private static final int MAXCHECKOBJECTS   = 50;			/* number to hand-out to each processor */

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
	 *   thisindex = global-index * multiplier + localindex + offset
	 * This individual index is used to lookup an entry on each network in the cell
	 * (an array is stored on each network, giving its global net number).
	 */
	static class CheckInst
	{
		int localindex;
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
		/** time stamp for counting within a particular parent */		int timestamp;
		/** number of instances of this cell in a particular parent */	int instancecount;
		/** total number of instances of this cell, hierarchically */	int hierinstancecount;
		/** number of instances of this cell in a particular parent */	int totalpercell;
		/** true if this cell has been checked */						boolean cellchecked;
		/** true if this cell has parameters */							boolean cellparameterized;
		/** list of instances in a particular parent */					List nodesInCell;
	};
	private static HashMap checkProtos = null;
	private static HashMap networkLists = null;

	/*
	 * The CHECKSTATE object exists for each processor and has global information for that thread.
	 */
//	typedef struct
//	{
//		POLYLIST  *cellinstpolylist;
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

//	typedef struct
//	{
//		NODEPROTO *cell;
//		POLYGON   *poly;
//		INTBIG     lx, hx, ly, hy;
//	} DRCEXCLUSION;

//	static INTBIG        dr_quickexclusioncount;		/* number of areas being excluded */
//	static INTBIG        dr_quickexclusiontotal = 0;	/* number of polygons allocated in exclusion list */
//	static DRCEXCLUSION *dr_quickexclusionlist;			/* list of excluded polygons */
//	static BOOLEAN       dr_quickexclusionwarned;		/* true if user warned about overflow */

//	static BOOLEAN      dr_quickparalleldrc;			/* true if doing DRC on multiple processes */
	/** number of processes for doing DRC */					private static int numberOfThreads;
	/** true to report only 1 error per Cell. */				private static boolean onlyFirstError;
	/** true to ignore center cuts in large contacts. */		private static boolean ignoreCenterCuts;
	/** maximum area to examine (the worst spacing rule). */	private static double worstInteractionDistance;
	/** time stamp for numbering networks. */					private static int dr_quickchecktimestamp;
	/** for numbering networks. */								private static int dr_quickchecknetnumber;
	/** for numbering networks. */								private static int dr_quickcheckunconnetnumber;
	/** the current design rules. */							private static DRC.Rules currentDesignRules;
	/** total errors found in all threads. */					private static int totalErrorsFound;

//	static INTBIG       dr_quickmainthread;				/* the main thread */
//	static CHECKSTATE **dr_quickstate;					/* one for each process */
//	static void        *dr_quickmutexnode = 0;			/* for locking node distribution */
//	static void        *dr_quickmutexio = 0;			/* for locking I/O */
//	static void        *dr_quickmutexinteract = 0;		/* for locking interaction checks */
//	static void       **dr_quickprocessdone;			/* lock to tell when process is done */
//	static INTBIG       dr_quickstatecount = 0;			/* number of per-process state blocks */

//	static NODEINST    *dr_quickparallelcellinst;		/* next cell instance to be checked */
//	static NODEINST    *dr_quickparallelnodeinst;		/* next primitive node to be checked */
//	static ARCINST     *dr_quickparallelarcinst;		/* next arc to be checked */

//	static INTBIG       dr_quickoptions;				/* DRC options for this run */
//	static INTBIG       dr_quickarealx;					/* low X of area being checked (if checking in area) */
//	static INTBIG       dr_quickareahx;					/* high X of area being checked (if checking in area) */
//	static INTBIG       dr_quickarealy;					/* low Y of area being checked (if checking in area) */
//	static INTBIG       dr_quickareahy;					/* high Y of area being checked (if checking in area) */
//	static BOOLEAN      dr_quickjustarea;				/* true if checking in an area only */

	/* for figuring out which layers are valid for DRC */
//		   TECHNOLOGY  *dr_curtech = NOTECHNOLOGY;		/* currently technology with cached layer info */
//	static INTBIG       dr_layertotal = 0;				/* number of layers in cached technology */
//		   BOOLEAN     *dr_layersvalid;					/* list of valid layers in cached technology */

	/* for tracking which layers interact with which nodes */
//	static TECHNOLOGY  *dr_quicklayerintertech = NOTECHNOLOGY;
//	static INTBIG       dr_quicklayerinternodehashsize = 0;
//	static NODEPROTO  **dr_quicklayerinternodehash;
//	static BOOLEAN    **dr_quicklayerinternodetable;
//	static INTBIG       dr_quicklayerinterarchashsize = 0;
//	static ARCPROTO   **dr_quicklayerinterarchash;
//	static BOOLEAN    **dr_quicklayerinterarctable;

	private static final int MAXHIERARCHYDEPTH = 100;

	/*
	 * This is the entry point for DRC.
	 *
	 * Method to do a hierarchical DRC check on cell "cell".
	 * If "count" is zero, check the entire cell.
	 * If "count" is nonzero, only check that many instances (in "nodestocheck") and set the
	 * entry in "validity" TRUE if it is DRC clean.
	 * If "justarea" is TRUE, only check in the selected area.
	 */
	public static void dr_quickcheck(Cell cell, int count, NodeInst [] nodestocheck, Boolean validity, boolean justarea)
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

		// get the current design rules for this technology
		currentDesignRules = tech.getDRCRules();

		// cache valid layers for this technology
		cacheValidLayers(tech);
		buildLayerInteractions(tech);

		// clean out the cache of instances
		clearInstanceCache();

		// determine maximum DRC interaction distance
		worstInteractionDistance = 0;
		for(int i = 0; i < currentDesignRules.uTSize; i++)
		{
			double dist = currentDesignRules.unConList[i].doubleValue();
			if (dist > worstInteractionDistance) worstInteractionDistance = dist;
			dist = currentDesignRules.unConListWide[i].doubleValue();
			if (dist > worstInteractionDistance) worstInteractionDistance = dist;
			dist = currentDesignRules.unConListMulti[i].doubleValue();
			if (dist > worstInteractionDistance) worstInteractionDistance = dist;
		}

		// initialize all cells for hierarchical network numbering
		checkProtos = new HashMap();
		checkInsts = new HashMap();
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (lib.isHidden()) continue;
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell libCell = (Cell)cIt.next();
				libCell.rebuildNetworks(null, false);
				CheckProto cp = new CheckProto();
				cp.instancecount = 0;
				cp.timestamp = 0;
				cp.hierinstancecount = 0;
				cp.totalpercell = 0;
				cp.cellchecked = false;
				cp.cellparameterized = false;
				for(Iterator vIt = libCell.getVariables(); vIt.hasNext(); )
				{
					Variable var = (Variable)vIt.next();
					if (var.getTextDescriptor().isParam())
					{
						cp.cellparameterized = true;
						break;
					}
				}
				checkProtos.put(libCell, cp);

				for(Iterator nIt = libCell.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = (NodeInst)nIt.next();
					if (!(ni.getProto() instanceof Cell)) continue;
					Cell subCell = (Cell)ni.getProto();

					// ignore documentation icons
					if (subCell.isIconOf(libCell)) continue;

					CheckInst ci = new CheckInst();
					checkInsts.put(ni, ci);
				}
			}
		}

		// see if any parameters are used below this cell
		if (findParameters(cell))
		{
			// parameters found: cannot use multiple processors
			System.out.println("Parameterized layout being used: multiprocessor decomposition disabled");
			numberOfThreads = 1;
		}

		// now recursively examine, setting information on all instances
		CheckProto cp = (CheckProto)checkProtos.get(cell);
		cp.hierinstancecount = 1;
		dr_quickchecktimestamp = 0;
		checkEnumerateInstances(cell);

		// now allocate space for hierarchical network arrays
		int totalNetworks = 0;
		networkLists = new HashMap();
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (lib.isHidden()) continue;
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell libCell = (Cell)cIt.next();
				CheckProto subCP = (CheckProto)checkProtos.get(libCell);
				if (subCP.hierinstancecount > 0)
				{
					// allocate net number lists for every net in the cell
					for(Iterator nIt = libCell.getNetworks(); nIt.hasNext(); )
					{
						JNetwork net = (JNetwork)nIt.next();
						Integer [] netNumbers = new Integer[subCP.hierinstancecount];
						for(int i=0; i<subCP.hierinstancecount; i++) netNumbers[i] = new Integer(0);
						networkLists.put(net, netNumbers);
						totalNetworks += subCP.hierinstancecount;
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
					CheckProto ocp = (CheckProto)checkProtos.get(np);
					ci.offset = ocp.totalpercell;
				}
				dr_quickchecktimestamp++;
				for(Iterator nIt = libCell.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = (NodeInst)nIt.next();
					NodeProto np = ni.getProto();
					if (!(np instanceof Cell)) continue;

					// ignore documentation icons
					if (ni.isIconOfParent()) continue;

					CheckProto ocp = (CheckProto)checkProtos.get(np);
					if (ocp.timestamp != dr_quickchecktimestamp)
					{
						CheckInst ci = (CheckInst)checkInsts.get(ni);
						ocp.timestamp = dr_quickchecktimestamp;
						ocp.totalpercell += subCP.hierinstancecount * ci.multiplier;
					}
				}
			}
		}

		// now fill in the hierarchical network arrays
		dr_quickchecktimestamp = 0;
		dr_quickchecknetnumber = 1;

		HashMap enumeratedNets = new HashMap();
		for(Iterator nIt = cell.getNetworks(); nIt.hasNext(); )
		{
			JNetwork net = (JNetwork)nIt.next();
			enumeratedNets.put(net, new Integer(dr_quickchecknetnumber));
			dr_quickchecknetnumber++;
		}
		checkEnumerateNetworks(cell, 0, enumeratedNets);
		dr_quickcheckunconnetnumber = dr_quickchecknetnumber;

		if (count <= 0)
		{
			System.out.println("Found " + dr_quickchecknetnumber + " networks");
		}

		// now search for DRC exclusion areas
//		dr_quickexclusioncount = 0;
//		cellarray[0] = cell;
//		dr_quickexclusionwarned = false;
//		transid(xarrayarray[0]);
//		dr_quickaccumulateexclusion(1, cellarray, xarrayarray);

 		Rectangle2D bounds = null;
		if (justarea)
		{
			EditWindow wnd = EditWindow.getCurrent();
			bounds = Highlight.getHighlightedArea(wnd);
		}

//		// initialize for multiple processors
//		if (dr_quickmakestateblocks(numberOfThreads)) return;
//		if (ensurevalidmutex(&dr_quickmutexnode, TRUE)) return;
//		if (ensurevalidmutex(&dr_quickmutexio,   TRUE)) return;
//		if (ensurevalidmutex(&dr_quickmutexinteract,   TRUE)) return;
//		if (numberOfThreads <= 1) dr_quickparalleldrc = FALSE; else
//		{
//			dr_quickparalleldrc = TRUE;
//			dr_quickmainthread = numberOfThreads - 1;
//		}

		// now do the DRC
		ErrorLog.initLogging("DRC");
		if (count == 0)
		{
			// just do standard DRC here
//			if (!dr_quickparalleldrc) begintraversehierarchy();
			checkThisCell(cell, 0, bounds, justarea);
//			if (!dr_quickparalleldrc) endtraversehierarchy();
		} else
		{
			// check only these "count" instances
//			dr_quickchecktheseinstances(cell, count, nodestocheck, validity);
		}

		// sort the errors by layer
		if (count <= 0)
		{
			ErrorLog.sortErrors();
			int errorcount = ErrorLog.numErrors();
			System.out.println(errorcount + " errors found");
		}
		ErrorLog.termLogging(true);
	}

	/*************************** QUICK DRC CELL EXAMINATION ***************************/

	/*
	 * Method to check the contents of cell "cell" with global network index "globalindex".
	 * Returns positive if errors are found, zero if no errors are found, negative on internal error.
	 */
	private static int checkThisCell(Cell cell, int globalindex, Rectangle2D bounds, boolean justarea)
	{
		// first check all subcells
		boolean allsubcellsstillok = true;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (!(np instanceof Cell)) continue;

			// ignore documentation icons
			if (ni.isIconOfParent()) continue;

			// ignore if not in the area
//			if (justarea)
//			{
//				if (ni->geom->lowx >= hx || ni->geom->highx <= lx ||
//					ni->geom->lowy >= hy || ni->geom->highy <= ly) continue;
//			}

			CheckProto cp = (CheckProto)checkProtos.get(np);
			if (cp.cellchecked && !cp.cellparameterized) continue;

			// recursively check the subcell
			CheckInst ci = (CheckInst)checkInsts.get(ni);
			int localindex = globalindex * ci.multiplier + ci.localindex + ci.offset;
//			if (!dr_quickparalleldrc) downhierarchy(ni, np, 0);
			int retval = checkThisCell((Cell)np, localindex, bounds, false);
//			if (!dr_quickparalleldrc) uphierarchy();
			if (retval < 0) return(-1);
			if (retval > 0) allsubcellsstillok = false;
		}

		// prepare to check cell
		CheckProto cp = (CheckProto)checkProtos.get(cell);
		cp.cellchecked = true;

		// if the cell hasn't changed since the last good check, stop now
		if (allsubcellsstillok)
		{
			Variable var = cell.getVar(DRC.LAST_GOOD_DRC);
			if (var != null)
			{
//				lastgooddate = (UINTBIG)var->addr;
//				lastchangedate = cell->revisiondate;
//				if (lastchangedate <= lastgooddate) return(0);
			}
		}

		// announce progress
		System.out.println("Checking cell " + cell.describe());

		// remember how many errors there are on entry
		int errcount = ErrorLog.numErrors();

		// now look at every primitive node and arc here
		totalErrorsFound = 0;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			boolean ret = false;
			if (ni.getProto() instanceof Cell)
			{
//				ret = dr_quickcheckcellinst(ni, globalindex);
			} else
			{
//				ret = dr_quickchecknodeinst(ni, globalindex);
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
			if (dr_quickcheckarcinst(ai, globalindex))
			{
				totalErrorsFound++;
				if (onlyFirstError) break;
			}
		}

		// if there were no errors, remember that
		int localerrors = ErrorLog.numErrors() - errcount;
		if (localerrors == 0)
		{
//			(void)setvalkey((INTBIG)cell, VNODEPROTO, dr_lastgooddrckey,
//				(INTBIG)getcurrenttime(), VINTEGER);
			System.out.println("   No errors found");
		} else
		{
			System.out.println("   FOUND " + localerrors + " ERRORS");
		}

		return totalErrorsFound;
	}

	/*
	 * Method to check the design rules about nodeinst "ni".  Only check those
	 * other objects whose geom pointers are greater than this one (i.e. only
	 * check any pair of objects once).
	 * Returns true if an error was found.
	 */
	boolean dr_quickchecknodeinst(NodeInst ni, int globalIndex)
	{
		Cell cell = ni.getParent();
		AffineTransform trans = ni.rotateOut();

		// get all of the polygons on this node
		NodeProto.Function fun = ni.getFunction();
		Technology tech = ni.getProto().getTechnology();
		Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, null);
		int tot = nodeInstPolyList.length;

		// examine the polygons on this node
		boolean errorsFound = false;
		for(int j=0; j<tot; j++)
		{
			Poly poly = nodeInstPolyList[j];
			Layer layer = poly.getLayer();
			if (layer == null) continue;

			// determine network for this polygon
			int netNumber = dr_quickgetnetnumber(poly.getPort(), ni, globalIndex);
			boolean ret = dr_quickbadbox(poly, layer, netNumber, tech, ni, trans, cell, globalIndex);
			if (ret)
			{
				if (onlyFirstError) return true;
				errorsFound = true;
			}
			ret = dr_quickcheckminwidth(ni, layer, poly, tech);
			if (ret)
			{
				if (onlyFirstError) return true;
				errorsFound = true;
			}
			if (tech == dr_curtech && !dr_layersvalid[layer.getIndex()])
			{
				reportError(BADLAYERERROR, dr_curtech, null, cell, 0, 0, null,
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
//					reportError(LAYERSURROUNDERROR, dr_curtech, 0, cell, surdist[i], 0,
//						surrules[i], poly, ni->geom, poly->layer, NONET,
//							NOPOLYGON, NOGEOM, surlayers[i], NONET);
//					if (onlyFirstError) return(TRUE);
//					errorsFound = TRUE;
//				}
//			}
//	#endif
		}

		// check node for minimum size
//		drcminnodesize(ni->proto, cell->lib, &minx, &miny, &rule);
//		if (minx > 0 && miny > 0)
//		{
//			if (ni->highx-ni->lowx < minx || ni->highy-ni->lowy < miny)
//			{
//				nodesizeoffset(ni, &lx, &ly, &hx, &hy);
//				if (minx - (ni->highx-ni->lowx) > miny - (ni->highy-ni->lowy))
//				{
//					minsize = minx - lx - hx;
//					actual = ni->highx - ni->lowx - lx - hx;
//				} else
//				{
//					minsize = miny - ly - hy;
//					actual = ni->highy - ni->lowy - ly - hy;
//				}
//				reportError(MINSIZEERROR, ni->proto->tech, 0, cell, minsize, actual, rule,
//					NOPOLYGON, ni->geom, 0, NONET, NOPOLYGON, NOGEOM, 0, NONET);
//			}
//		}
		return errorsFound;
	}

	/*
	 * Method to check the design rules about arcinst "ai".
	 * Returns true if errors were found.
	 */
	private static boolean dr_quickcheckarcinst(ArcInst ai, int globalIndex)
	{
		// ignore arcs with no topology
		JNetwork net = ai.getNetwork(0);
		if (net == null) return false;
		Integer [] netNumbers = (Integer [])networkLists.get(net);

		// get all of the polygons on this arc
		Technology tech = ai.getProto().getTechnology();
		Poly [] arcinstpolylist = tech.getShapeOfArc(ai, null);
//		dr_quickcropactivearc(ai, state->arcinstpolylist, state);
		int tot = arcinstpolylist.length;

		// examine the polygons on this arc
		boolean errorsFound = false;
		for(int j=0; j<tot; j++)
		{
			Poly poly = arcinstpolylist[j];
			if (poly.getLayer() == null) continue;
			Layer layer = poly.getLayer();
			int layerNum = layer.getIndex();
			int netNumber = netNumbers[globalIndex].intValue();
			AffineTransform matID = new AffineTransform();
			boolean ret = dr_quickbadbox(poly, layer, netNumber, tech, ai, matID, ai.getParent(), globalIndex);
			if (ret)
			{
				if (onlyFirstError) return true;
				errorsFound = true;
			}
			ret = dr_quickcheckminwidth(ai, layer, poly, tech);
			if (ret)
			{
				if (onlyFirstError) return true;
				errorsFound = true;
			}
			if (tech == dr_curtech && !dr_layersvalid[layerNum])
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
//	BOOLEAN dr_quickcheckcellinst(NODEINST *ni, INTBIG globalIndex, CHECKSTATE *state)
//	{
//		// get current position in traversal hierarchy
//		gethierarchicaltraversal(state->hierarchybasesnapshot);
//
//		// look for other instances surrounding this one
//		lx = ni->geom->lowx - worstInteractionDistance;
//		hx = ni->geom->highx + worstInteractionDistance;
//		ly = ni->geom->lowy - worstInteractionDistance;
//		hy = ni->geom->highy + worstInteractionDistance;
//		search = initsearch(lx, hx, ly, hy, ni->parent);
//		for(;;)
//		{
//			geom = nextobject(search);
//			if (geom == NOGEOM) break;
//			if (!geom->entryisnode) continue;
//			oni = geom->entryaddr.ni;
//
//			// only check other nodes that are numerically higher (so each pair is only checked once)
//			if ((INTBIG)oni <= (INTBIG)ni) continue;
//			if (oni->proto->primindex != 0) continue;
//
//			// see if this configuration of instances has already been done
//			if (dr_quickcheckinteraction(ni, oni)) continue;
//
//			// found other instance "oni", look for everything in "ni" that is near it
//			sublx = oni->geom->lowx - worstInteractionDistance;
//			subhx = oni->geom->highx + worstInteractionDistance;
//			subly = oni->geom->lowy - worstInteractionDistance;
//			subhy = oni->geom->highy + worstInteractionDistance;
//			makerotI(ni, rtrans);
//			maketransI(ni, ttrans);
//			transmult(rtrans, ttrans, downtrans);
//			xformbox(&sublx, &subhx, &subly, &subhy, downtrans);
//
//			maketrans(ni, ttrans);
//			makerot(ni, rtrans);
//			transmult(ttrans, rtrans, uptrans);
//
//			ci = (CHECKINST *)ni->temp1;
//			localindex = globalIndex * ci->multiplier + ci->localindex + ci->offset;
//
//			// recursively search instance "ni" in the vicinity of "oni"
//			if (!dr_quickparalleldrc) downhierarchy(ni, ni->proto, 0);
//			(void)dr_quickcheckcellinstcontents(sublx, subhx, subly, subhy, ni->proto, uptrans,
//				localindex, oni, globalIndex, state);
//			if (!dr_quickparalleldrc) uphierarchy();
//		}
//		return(FALSE);
//	}

	/*
	 * Method to recursively examine the area (lx-hx, ly-hy) in cell "np" with global index "globalIndex".
	 * The objects that are found are transformed by "uptrans" to be in the space of a top-level cell.
	 * They are then compared with objects in "oni" (which is in that top-level cell),
	 * which has global index "topglobalIndex".
	 */
//	BOOLEAN dr_quickcheckcellinstcontents(INTBIG lx, INTBIG hx, INTBIG ly, INTBIG hy, NODEPROTO *np,
//		XARRAY uptrans, INTBIG globalIndex, NODEINST *oni, INTBIG topglobalIndex, CHECKSTATE *state)
//	{
//		REGISTER INTBIG subsearch, tot, j, net, localindex;
//		INTBIG sublx, subhx, subly, subhy;
//		REGISTER GEOM *geom;
//		REGISTER NODEINST *ni;
//		REGISTER ARCINST *ai;
//		REGISTER BOOLEAN errorsfound, ret;
//		XARRAY rtrans, ttrans, trans, subuptrans;
//		REGISTER CHECKINST *ci;
//		REGISTER POLYGON *poly;
//
//		errorsfound = FALSE;
//		subsearch = initsearch(lx, hx, ly, hy, np);
//		for(;;)
//		{
//			geom = nextobject(subsearch);
//			if (geom == NOGEOM) break;
//			if (geom->entryisnode)
//			{
//				ni = geom->entryaddr.ni;
//				if (ni->proto->primindex == 0)
//				{
//					sublx = lx;   subhx = hx;
//					subly = ly;   subhy = hy;
//					makerotI(ni, rtrans);
//					maketransI(ni, ttrans);
//					transmult(rtrans, ttrans, trans);
//					xformbox(&sublx, &subhx, &subly, &subhy, trans);
//
//					maketrans(ni, ttrans);
//					makerot(ni, rtrans);
//					transmult(ttrans, rtrans, trans);
//					transmult(trans, uptrans, subuptrans);
//
//					ci = (CHECKINST *)ni->temp1;
//					localindex = globalIndex * ci->multiplier + ci->localindex + ci->offset;
//
//					if (!dr_quickparalleldrc) downhierarchy(ni, ni->proto, 0);
//					dr_quickcheckcellinstcontents(sublx, subhx, subly, subhy, ni->proto,
//						subuptrans, localindex, oni, topglobalIndex, state);
//					if (!dr_quickparalleldrc) uphierarchy();
//				} else
//				{
//					makerot(ni, rtrans);
//					transmult(rtrans, uptrans, trans);
//					dr_quickgetnodeEpolys(ni, state->cellinstpolylist, trans);
//					tot = state->cellinstpolylist->polylistcount;
//					for(j=0; j<tot; j++)
//					{
//						poly = state->cellinstpolylist->polygons[j];
//						if (poly->layer < 0) continue;
//
//						// determine network for this polygon
//						net = dr_quickgetnetnumber(poly->portproto, ni, globalIndex);
//						ret = dr_quickbadsubbox(poly, poly->layer, net, ni->proto->tech, ni->geom, trans,
//							globalIndex, np, oni, topglobalIndex, state);
//						if (ret)
//						{
//							if (onlyFirstError)
//							{
//								termsearch(subsearch);
//								return(TRUE);
//							}
//							errorsfound = TRUE;
//						}
//					}
//				}
//			} else
//			{
//				ai = geom->entryaddr.ai;
//				dr_quickgetarcpolys(ai, state->cellinstpolylist, uptrans);
//				dr_quickcropactivearc(ai, state->cellinstpolylist, state);
//				tot = state->cellinstpolylist->polylistcount;
//				for(j=0; j<tot; j++)
//				{
//					poly = state->cellinstpolylist->polygons[j];
//					if (poly->layer < 0) continue;
//					if (ai->network == NONETWORK) net = NONET; else
//						net = ((INTBIG *)ai->network->temp1)[globalIndex];
//					ret = dr_quickbadsubbox(poly, poly->layer, net, ai->proto->tech, ai->geom, uptrans,
//						globalIndex, np, oni, topglobalIndex, state);
//					if (ret)
//					{
//						if (onlyFirstError)
//						{
//							termsearch(subsearch);
//							return(TRUE);
//						}
//						errorsfound = TRUE;
//					}
//				}
//			}
//		}
//		return(errorsfound);
//	}

	/*
	 * Method to examine polygon "poly" layer "layer" network "net" technology "tech" geometry "geom"
	 * which is in cell "cell" and has global index "globalIndex".
	 * The polygon is compared against things inside node "oni", and that node's parent has global index "topglobalIndex". 
	 */
//	BOOLEAN dr_quickbadsubbox(POLYGON *poly, INTBIG layer, INTBIG net, TECHNOLOGY *tech, GEOM *geom, XARRAY trans,
//		INTBIG globalIndex, NODEPROTO *cell, NODEINST *oni, INTBIG topglobalIndex, CHECKSTATE *state)
//	{
//		REGISTER BOOLEAN basemulti, retval;
//		REGISTER INTBIG minsize, bound, lxbound, hxbound, lybound, hybound, localindex;
//		INTBIG lx, hx, ly, hy;
//		XARRAY rtrans, ttrans, downtrans, uptrans;
//		REGISTER CHECKINST *ci;
//
//		// see how far around the box it is necessary to search
//		bound = maxdrcsurround(tech, cell->lib, layer);
//		if (bound < 0) return(FALSE);
//
//		// get bounds
//		getbbox(poly, &lx, &hx, &ly, &hy);
//		makerotI(oni, rtrans);
//		maketransI(oni, ttrans);
//		transmult(rtrans, ttrans, downtrans);
//		xformbox(&lx, &hx, &ly, &hy, downtrans);
//		minsize = polyminsize(poly);
//
//		maketrans(oni, ttrans);
//		makerot(oni, rtrans);
//		transmult(ttrans, rtrans, uptrans);
//
//		ci = (CHECKINST *)oni->temp1;
//		localindex = topglobalIndex * ci->multiplier + ci->localindex + ci->offset;
//
//		// determine if original object has multiple contact cuts
//		if (geom->entryisnode) basemulti = dr_quickismulticut(geom->entryaddr.ni); else
//			basemulti = FALSE;
//
//		// remember the current position in the hierarchy traversal and set the base one
//		gethierarchicaltraversal(state->hierarchytempsnapshot);
//		sethierarchicaltraversal(state->hierarchybasesnapshot);
//
//		// search in the area surrounding the box
//		lxbound = lx-bound;   hxbound = hx+bound;
//		lybound = ly-bound;   hybound = hy+bound;
//		retval = dr_quickbadboxinarea(poly, layer, tech, net, geom, trans, globalIndex,
//			lxbound, hxbound, lybound, hybound, oni->proto, localindex,
//			oni->parent, topglobalIndex, uptrans, minsize, basemulti, state, FALSE);
//
//		// restore the proper hierarchy traversal position
//		sethierarchicaltraversal(state->hierarchytempsnapshot);
//		return(retval);
//	}

	/*
	 * Method to examine a polygon to see if it has any errors with its surrounding area.
	 * The polygon is "poly" on layer "layer" on network "net" from technology "tech" from object "geom".
	 * Checking looks in cell "cell" global index "globalIndex".
	 * Object "geom" can be transformed to the space of this cell with "trans".
	 * Returns TRUE if a spacing error is found relative to anything surrounding it at or below
	 * this hierarchical level.
	 */
	private static boolean dr_quickbadbox(Poly poly, Layer layer, int net, Technology tech, Geometric geom,
		AffineTransform trans, Cell cell, int globalIndex)
	{
//		REGISTER BOOLEAN basemulti;
//		REGISTER INTBIG minsize, bound, lxbound, hxbound, lybound, hybound;
//		INTBIG lx, hx, ly, hy;
//
//		// see how far around the box it is necessary to search
//		bound = maxdrcsurround(tech, cell->lib, layer);
//		if (bound < 0) return(FALSE);
//
//		// get bounds
//		getbbox(poly, &lx, &hx, &ly, &hy);
//		minsize = polyminsize(poly);
//
//		// determine if original object has multiple contact cuts
//		if (geom->entryisnode) basemulti = dr_quickismulticut(geom->entryaddr.ni); else
//			basemulti = FALSE;
//
//		// search in the area surrounding the box
//		lxbound = lx-bound;   hxbound = hx+bound;
//		lybound = ly-bound;   hybound = hy+bound;
//		return(dr_quickbadboxinarea(poly, layer, tech, net, geom, trans, globalIndex,
//			lxbound, hxbound, lybound, hybound, cell, globalIndex,
//			cell, globalIndex, el_matid, minsize, basemulti, state, TRUE));
		return false;
	}

	/*
	 * Method to recursively examine a polygon to see if it has any errors with its surrounding area.
	 * The polygon is "poly" on layer "layer" from technology "tech" on network "net" from object "geom"
	 * which is associated with global index "globalIndex".
	 * Checking looks in the area (lxbound-hxbound, lybound-hybound) in cell "cell" global index "cellglobalIndex".
	 * The polygon coordinates are in the space of cell "topcell", global index "topglobalIndex",
	 * and objects in "cell" can be transformed by "toptrans" to get to this space.
	 * The base object, in "geom" can be transformed by "trans" to get to this space.
	 * The minimum size of this polygon is "minsize" and "basemulti" is TRUE if it comes from a multicut contact.
	 * If the two objects are in the same cell instance (nonhierarchical DRC), then "sameinstance" is TRUE.
	 * If they are from different instances, then "sameinstance" is FALSE.
	 *
	 * Returns TRUE if errors are found.
	 */
//	BOOLEAN dr_quickbadboxinarea(POLYGON *poly, INTBIG layer, TECHNOLOGY *tech, INTBIG net, GEOM *geom, XARRAY trans,
//		INTBIG globalIndex,
//		INTBIG lxbound, INTBIG hxbound, INTBIG lybound, INTBIG hybound, NODEPROTO *cell, INTBIG cellglobalIndex,
//		NODEPROTO *topcell, INTBIG topglobalIndex, XARRAY toptrans, INTBIG minsize, BOOLEAN basemulti,
//		CHECKSTATE *state, BOOLEAN sameinstance)
//	{
//		REGISTER GEOM *ngeom;
//		REGISTER NODEPROTO *np;
//		REGISTER ARCINST *ai;
//		REGISTER NODEINST *ni;
//		REGISTER ARCPROTO *ap;
//		REGISTER BOOLEAN touch, ret, multi;
//		REGISTER INTBIG nnet, search, dist, j, tot, nminsize,
//			localindex, count;
//		REGISTER POLYGON *npoly;
//		XARRAY rtrans, ttrans, subtrans, temptrans;
//		CHAR *rule;
//		REGISTER BOOLEAN con;
//		INTBIG edge, slx, shx, sly, shy, rlxbound, rhxbound, rlybound, rhybound;
//		REGISTER CHECKINST *ci;
//
//		rlxbound = lxbound;   rhxbound = hxbound;
//		rlybound = lybound;   rhybound = hybound;
//		xformbox(&rlxbound, &rhxbound, &rlybound, &rhybound, toptrans);
//
//		search = initsearch(lxbound, hxbound, lybound, hybound, cell);
//		count = 0;
//		for(;;)
//		{
//			ngeom = nextobject(search);
//			if (ngeom == NOGEOM) break;
//			if (sameinstance && (ngeom == geom)) continue;
//			if (ngeom->entryisnode)
//			{
//				ni = ngeom->entryaddr.ni;
//				np = ni->proto;
//
//				// ignore nodes that are not primitive
//				if (np->primindex == 0)
//				{
//					// instance found: look inside it for offending geometry
//					makerotI(ni, rtrans);
//					maketransI(ni, ttrans);
//					transmult(rtrans, ttrans, temptrans);
//					slx = lxbound;   shx = hxbound;
//					sly = lybound;   shy = hybound;
//					xformbox(&slx, &shx, &sly, &shy, temptrans);
//
//					ci = (CHECKINST *)ni->temp1;
//					localindex = cellglobalIndex * ci->multiplier + ci->localindex + ci->offset;
//
//					maketrans(ni, ttrans);
//					makerot(ni, rtrans);
//					transmult(ttrans, rtrans, temptrans);
//					transmult(temptrans, toptrans, subtrans);
//
//					// compute localindex
//					if (!dr_quickparalleldrc) downhierarchy(ni, np, 0);
//					dr_quickbadboxinarea(poly, layer, tech, net, geom, trans, globalIndex,
//						slx, shx, sly, shy, np, localindex,
//						topcell, topglobalIndex, subtrans, minsize, basemulti, state, sameinstance);
//					if (!dr_quickparalleldrc) uphierarchy();
//				} else
//				{
//					// don't check between technologies
//					if (np->tech != tech) continue;
//
//					// see if this type of node can interact with this layer
//					if (!dr_quickchecklayerwithnode(layer, np)) continue;
//
//					// see if the objects directly touch
//					touch = dr_quickobjtouch(ngeom, geom);
//
//					// prepare to examine every layer in this nodeinst
//					makerot(ni, rtrans);
//					transmult(rtrans, toptrans, subtrans);
//
//					// get the shape of each nodeinst layer
//					dr_quickgetnodeEpolys(ni, state->subpolylist, subtrans);
//					tot = state->subpolylist->polylistcount;
//					multi = basemulti;
//					if (!multi) multi = dr_quickismulticut(ni);
//					for(j=0; j<tot; j++)
//					{
//						npoly = state->subpolylist->polygons[j];
//
//						// can't do this because "lxbound..." is local but the poly bounds are global
//						if (state->subpolylist->lx[j] > rhxbound ||
//							state->subpolylist->hx[j] < rlxbound ||
//							state->subpolylist->ly[j] > rhybound ||
//							state->subpolylist->hy[j] < rlybound) continue;
//						if (npoly->layer < 0) continue;
//
//						// determine network for this polygon
//						nnet = dr_quickgetnetnumber(npoly->portproto, ni, cellglobalIndex);
//
//						// see whether the two objects are electrically connected
//						if (nnet != NONET && nnet == net) con = TRUE; else con = FALSE;
//
//						// if they connect electrically and adjoin, don't check
//						if (con && touch) continue;
//
//						nminsize = polyminsize(npoly);
//						dist = dr_adjustedmindist(tech, cell->lib, layer, minsize,
//							npoly->layer, nminsize, con, multi, &edge, &rule);
//						if (dist < 0) continue;
//
//						// check the distance
//						ret = dr_quickcheckdist(tech, topcell, topglobalIndex,
//							poly, layer, net, geom, trans, globalIndex,
//							npoly, npoly->layer, nnet, ngeom, subtrans, cellglobalIndex,
//							con, dist, edge, rule, state);
//						if (ret)
//						{
//							termsearch(search);
//							return(TRUE);
//						}
//					}
//				}
//			} else
//			{
//				ai = ngeom->entryaddr.ai;   ap = ai->proto;
//
//				// don't check between technologies
//				if (ap->tech != tech) continue;
//
//				// see if this type of arc can interact with this layer
//				if (!dr_quickchecklayerwitharc(layer, ap)) continue;
//
//				// see if the objects directly touch
//				touch = dr_quickobjtouch(ngeom, geom);
//
//				// see whether the two objects are electrically connected
//				nnet = ((INTBIG *)ai->network->temp1)[cellglobalIndex];
//				if (net != NONET && nnet == net) con = TRUE; else con = FALSE;
//
//				// if they connect electrically and adjoin, don't check
//				if (con && touch) continue;
//
//				// get the shape of each arcinst layer
//				dr_quickgetarcpolys(ai, state->subpolylist, toptrans);
//				dr_quickcropactivearc(ai, state->subpolylist, state);
//				tot = state->subpolylist->polylistcount;
//				multi = basemulti;
//				for(j=0; j<tot; j++)
//				{
//					npoly = state->subpolylist->polygons[j];
//
//					// can't do this because "lxbound..." is local but the poly bounds are global
//					if (state->subpolylist->lx[j] > rhxbound ||
//						state->subpolylist->hx[j] < rlxbound ||
//						state->subpolylist->ly[j] > rhybound ||
//						state->subpolylist->hy[j] < rlybound) continue;
//					if (npoly->layer < 0) continue;
//
//					// see how close they can get
//					nminsize = polyminsize(npoly);
//					dist = dr_adjustedmindist(tech, cell->lib, layer,
//						minsize, npoly->layer, nminsize, con, multi, &edge, &rule);
//					if (dist < 0) continue;
//
//					// check the distance
//					ret = dr_quickcheckdist(tech, topcell, topglobalIndex,
//						poly, layer, net, geom, trans, globalIndex,
//						npoly, npoly->layer, nnet, ngeom, toptrans, cellglobalIndex,
//						con, dist, edge, rule, state);
//					if (ret)
//					{
//						termsearch(search);
//						return(TRUE);
//					}
//				}
//			}
//		}
//		return(FALSE);
//	}

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
//	BOOLEAN dr_quickcheckdist(TECHNOLOGY *tech, NODEPROTO *cell, INTBIG globalIndex,
//		POLYGON *poly1, INTBIG layer1, INTBIG net1, GEOM *geom1, XARRAY trans1, INTBIG globalIndex1,
//		POLYGON *poly2, INTBIG layer2, INTBIG net2, GEOM *geom2, XARRAY trans2, INTBIG globalIndex2,
//		BOOLEAN con, INTBIG dist, INTBIG edge, CHAR *rule, CHECKSTATE *state)
//	{
//		REGISTER BOOLEAN isbox1, isbox2, needboth, maytouch;
//		INTBIG lx1, hx1, ly1, hy1, lx2, hx2, ly2, hy2, xf1, yf1, xf2, yf2;
//		CHAR *msg, *sizerule;
//		void *infstr;
//		REGISTER POLYGON *origpoly1, *origpoly2;
//		REGISTER INTBIG pdx, pdy, pd, pdedge, fun, errtype, minwidth,
//			lxb, hxb, lyb, hyb, actual, intervening;
//
//		// turn off flag that the nodeinst may be undersized
//		state->tinynodeinst = NONODEINST;
//
//		origpoly1 = poly1;
//		origpoly2 = poly2;
//		isbox1 = isbox(poly1, &lx1, &hx1, &ly1, &hy1);
//		if (!isbox1) getbbox(poly1, &lx1, &hx1, &ly1, &hy1);
//		isbox2 = isbox(poly2, &lx2, &hx2, &ly2, &hy2);
//		if (!isbox2) getbbox(poly2, &lx2, &hx2, &ly2, &hy2);
//
//		/*
//		 * special rule for allowing touching:
//		 *   the layers are the same and either:
//		 *     they connect and are *NOT* contact layers
//		 *   or:
//		 *     they don't connect and are implant layers (substrate/well)
//		 */
//		maytouch = FALSE;
//		if (samelayer(tech, layer1, layer2))
//		{
//			fun = layerfunction(tech, layer1) & LFTYPE;
//			if (con)
//			{
//				if (!layeriscontact(fun)) maytouch = TRUE;
//			} else
//			{
//				if (fun == LFSUBSTRATE || fun == LFWELL || fun == LFIMPLANT) maytouch = TRUE;
//			}
//		}
//
//		// special code if both polygons are manhattan
//		if (isbox1 && isbox2)
//		{
//			// manhattan
//			pdx = maxi(lx2-hx1, lx1-hx2);
//			pdy = maxi(ly2-hy1, ly1-hy2);
//			if (pdx == 0 && pdy == 0) pd = 1; else
//				pd = maxi(pdx, pdy);
//			if (maytouch)
//			{
//				// they are electrically connected: see if they touch
//				if (pd <= 0)
//				{
//					// they are electrically connected and they touch: look for minimum size errors
//					minwidth = drcminwidth(tech, cell->lib, layer1, &sizerule);
//					lxb = maxi(lx1, lx2);
//					hxb = mini(hx1, hx2);
//					lyb = maxi(ly1, ly2);
//					hyb = mini(hy1, hy2);
//					actual = computedistance(lxb, lyb, hxb, hyb);
//					if (actual != 0 && actual < minwidth)
//					{
//						if (hxb-lxb > hyb-lyb)
//						{
//							// horizontal abutment: check for minimum width
//							if (!dr_quicklookforpoints(lxb-1, lyb-1, lxb-1, hyb+1, layer1, cell, TRUE, state) &&
//									!dr_quicklookforpoints(hxb+1, lyb-1, hxb+1, hyb+1, layer1, cell, TRUE, state))
//							{
//								lyb -= minwidth/2;   hyb += minwidth/2;
//								state->checkdistpoly1rebuild->xv[0] = lxb;
//								state->checkdistpoly1rebuild->yv[0] = lyb;
//								state->checkdistpoly1rebuild->xv[1] = hxb;
//								state->checkdistpoly1rebuild->yv[1] = hyb;
//								state->checkdistpoly1rebuild->count = 2;
//								state->checkdistpoly1rebuild->style = FILLEDRECT;
//								reportError(MINWIDTHERROR, tech, 0, cell, minwidth,
//									actual, sizerule, state->checkdistpoly1rebuild,
//										geom1, layer1, NONET, NOPOLYGON, NOGEOM, 0, NONET);
//								return(TRUE);
//							}
//						} else
//						{
//							// vertical abutment: check for minimum width
//							if (!dr_quicklookforpoints(lxb-1, lyb-1, hxb+1, lyb-1, layer1, cell, TRUE, state) &&
//									!dr_quicklookforpoints(lxb-1, hyb+1, hxb+1, hyb+1, layer1, cell, TRUE, state))
//							{
//								lxb -= minwidth/2;   hxb += minwidth/2;
//								state->checkdistpoly1rebuild->xv[0] = lxb;
//								state->checkdistpoly1rebuild->yv[0] = lyb;
//								state->checkdistpoly1rebuild->xv[1] = hxb;
//								state->checkdistpoly1rebuild->yv[1] = hyb;
//								state->checkdistpoly1rebuild->count = 2;
//								state->checkdistpoly1rebuild->style = FILLEDRECT;
//								reportError(MINWIDTHERROR, tech, 0, cell, minwidth,
//									actual, sizerule, state->checkdistpoly1rebuild,
//										geom1, layer1, NONET, NOPOLYGON, NOGEOM, 0, NONET);
//								return(TRUE);
//							}
//						}
//					}
//				}
//			}
//
//			// crop out parts of any arc that is covered by an adjoining node
//			if (geom1->entryisnode)
//			{
//				if (dr_quickcropnodeinst(geom1->entryaddr.ni, globalIndex1, state, trans1, net1,
//					lx1, hx1, ly1, hy1, layer2, net2, geom2, &lx2, &hx2, &ly2, &hy2))
//				{
//					return(FALSE);
//				}
//			} else
//			{
//				if (dr_quickcroparcinst(geom1->entryaddr.ai, layer1, &lx1, &hx1, &ly1, &hy1, state))
//				{
//					return(FALSE);
//				}
//			}
//			if (geom2->entryisnode)
//			{
//				if (dr_quickcropnodeinst(geom2->entryaddr.ni, globalIndex2, state, trans2, net2,
//					lx2, hx2, ly2, hy2, layer1, net1, geom1, &lx1, &hx1, &ly1, &hy1))
//				{
//					return(FALSE);
//				}
//			} else
//			{
//				if (dr_quickcroparcinst(geom2->entryaddr.ai, layer2, &lx2, &hx2, &ly2, &hy2, state))
//				{
//					return(FALSE);
//				}
//			}
//
//			makerectpoly(lx1, hx1, ly1, hy1, state->checkdistpoly1rebuild);
//			state->checkdistpoly1rebuild->style = FILLED;
//			makerectpoly(lx2, hx2, ly2, hy2, state->checkdistpoly2rebuild);
//			state->checkdistpoly2rebuild->style = FILLED;
//			poly1 = state->checkdistpoly1rebuild;
//			poly2 = state->checkdistpoly2rebuild;
//
//			// compute the distance
//			if (edge != 0)
//			{
//				// calculate the spacing between the box edges
//				pdedge = mini(
//					mini(mini(abs(lx1-lx2), abs(lx1-hx2)), mini(abs(hx1-lx2), abs(hx1-hx2))),
//					mini(mini(abs(ly1-ly2), abs(ly1-hy2)), mini(abs(hy1-ly2), abs(hy1-hy2))));
//				pd = maxi(pd, pdedge);
//			} else
//			{
//				pdx = maxi(lx2-hx1, lx1-hx2);
//				pdy = maxi(ly2-hy1, ly1-hy2);
//				if (pdx == 0 && pdy == 0) pd = 1; else
//				{
//					pd = maxi(pdx, pdy);
//					if (pd < dist && pd > 0) pd = polyseparation(poly1, poly2);
//				}
//			}
//		} else
//		{
//			// nonmanhattan
//			switch (poly1->style)
//			{
//				case FILLEDRECT:
//				case CLOSEDRECT:
//					maketruerect(poly1);
//					break;
//				case FILLED:
//				case CLOSED:
//				case CROSSED:
//				case OPENED:
//				case OPENEDT1:
//				case OPENEDT2:
//				case OPENEDT3:
//				case VECTORS:
//					break;
//				default:
//					return(FALSE);
//			}
//
//			switch (poly2->style)
//			{
//				case FILLEDRECT:
//				case CLOSEDRECT:
//					maketruerect(poly2);
//					break;
//				case FILLED:
//				case CLOSED:
//				case CROSSED:
//				case OPENED:
//				case OPENEDT1:
//				case OPENEDT2:
//				case OPENEDT3:
//				case VECTORS:
//					break;
//				default:
//					return(FALSE);
//			}
//
//			// make sure polygons don't intersect
//			if (polyintersect(poly1, poly2)) pd = 0; else
//			{
//				// find distance between polygons
//				pd = polyseparation(poly1, poly2);
//			}
//		}
//
//		// see if the design rule is met
//		if (pd >= dist)
//		{ 
//			return(FALSE);
//		}
//		errtype = SPACINGERROR;
//
//		/*
//		 * special case: ignore errors between two active layers connected
//		 * to either side of a field-effect transistor that is inside of
//		 * the error area.
//		 */
//		if (dr_quickactiveontransistor(poly1, layer1, net1,
//			poly2, layer2, net2, tech, cell, globalIndex))
//		{
//			return(FALSE);
//		}
//
//		// special cases if the layers are the same
//		if (samelayer(tech, layer1, layer2))
//		{
//			// special case: check for "notch"
//			if (maytouch)
//			{
//				// if they touch, it is acceptable
//				if (pd <= 0) 
//				{
//					return(FALSE);
//				}
//
//				// see if the notch is filled
//				intervening = dr_quickfindinterveningpoints(poly1, poly2, &xf1, &yf1, &xf2, &yf2);
//				if (intervening == 0)
//				{
//					return(FALSE);
//				}
//				if (intervening == 1) needboth = FALSE; else
//					needboth = TRUE;
//				if (dr_quicklookforpoints(xf1, yf1, xf2, yf2, layer1, cell, needboth, state))
//				{
//					return(FALSE);
//				}
//
//				// look further if on the same net and diagonally separate (1 intervening point)
//				if (net1 == net2 && intervening == 1)
//				{
//					return(FALSE);
//				}
//				errtype = NOTCHERROR;
//			}
//		}
//
//		msg = 0;
//		if (state->tinynodeinst != NONODEINST)
//		{
//			// see if the node/arc that failed was involved in the error
//			if ((state->tinynodeinst->geom == geom1 || state->tinynodeinst->geom == geom2) &&
//				(state->tinyobj == geom1 || state->tinyobj == geom2))
//			{
//				infstr = initinfstr();
//				if (dr_quickparalleldrc)
//					emutexlock(dr_quickmutexio);	// BEGIN critical section
//				formatinfstr(infstr, _("%s is too small for the %s"),
//					describenodeinst(state->tinynodeinst), geomname(state->tinyobj));
//				if (dr_quickparalleldrc)
//					emutexunlock(dr_quickmutexio);	// END critical section
//				(void)allocstring(&msg, returninfstr(infstr), dr_tool->cluster);
//			}
//		}
//
//		reportError(errtype, tech, msg, cell, dist, pd, rule,
//			origpoly1, geom1, layer1, net1,
//			origpoly2, geom2, layer2, net2);
//		return(TRUE);
//	}

	/*************************** QUICK DRC SEE IF INSTANCES CAUSE ERRORS ***************************/

	/*
	 * Method to examine, in cell "cell", the "count" instances in "nodestocheck".
	 * If they are DRC clean, set the associated entry in "validity" to TRUE.
	 */
//	void dr_quickchecktheseinstances(NODEPROTO *cell, INTBIG count, NODEINST **nodestocheck, BOOLEAN *validity)
//	{
//		REGISTER INTBIG lx, hx, ly, hy, search, globalIndex, localindex, i, j;
//		INTBIG sublx, subhx, subly, subhy;
//		REGISTER GEOM *geom;
//		REGISTER NODEINST *ni, *oni;
//		XARRAY rtrans, ttrans, downtrans, uptrans;
//		REGISTER CHECKINST *ci;
//		REGISTER CHECKSTATE *state;
//
//		globalIndex = 0;
//		state = dr_quickstate[0];
//		state->globalIndex = globalIndex;
//
//		// loop through all of the instances to be checked
//		for(i=0; i<count; i++)
//		{
//			ni = nodestocheck[i];
//			validity[i] = TRUE;
//
//			// look for other instances surrounding this one
//			lx = ni->geom->lowx - worstInteractionDistance;
//			hx = ni->geom->highx + worstInteractionDistance;
//			ly = ni->geom->lowy - worstInteractionDistance;
//			hy = ni->geom->highy + worstInteractionDistance;
//			search = initsearch(lx, hx, ly, hy, ni->parent);
//			for(;;)
//			{
//				geom = nextobject(search);
//				if (geom == NOGEOM) break;
//				if (!geom->entryisnode)
//				{
//					if (dr_quickcheckgeomagainstinstance(geom, ni, state))
//					{
//						validity[i] = FALSE;
//						termsearch(search);
//						break;
//					}
//					continue;
//				}
//				oni = geom->entryaddr.ni;
//				if (oni->proto->primindex != 0)
//				{
//					// found a primitive node: check it against the instance contents
//					if (dr_quickcheckgeomagainstinstance(geom, ni, state))
//					{
//						validity[i] = FALSE;
//						termsearch(search);
//						break;
//					}
//					continue;
//				}
//
//				// ignore if it is one of the instances in the list
//				for(j=0; j<count; j++)
//					if (oni == nodestocheck[j]) break;
//				if (j < count) continue;
//
//				// found other instance "oni", look for everything in "ni" that is near it
//				sublx = oni->geom->lowx - worstInteractionDistance;
//				subhx = oni->geom->highx + worstInteractionDistance;
//				subly = oni->geom->lowy - worstInteractionDistance;
//				subhy = oni->geom->highy + worstInteractionDistance;
//				makerotI(ni, rtrans);
//				maketransI(ni, ttrans);
//				transmult(rtrans, ttrans, downtrans);
//				xformbox(&sublx, &subhx, &subly, &subhy, downtrans);
//
//				maketrans(ni, ttrans);
//				makerot(ni, rtrans);
//				transmult(ttrans, rtrans, uptrans);
//
//				ci = (CHECKINST *)ni->temp1;
//				localindex = globalIndex * ci->multiplier + ci->localindex + ci->offset;
//
//				// recursively search instance "ni" in the vicinity of "oni"
//				if (dr_quickcheckcellinstcontents(sublx, subhx, subly, subhy, ni->proto, uptrans,
//					localindex, oni, globalIndex, dr_quickstate[0]))
//				{
//					// errors were found: bail
//					validity[i] = FALSE;
//					termsearch(search);
//					break;
//				}
//			}
//		}
//	}

	/*
	 * Method to check primitive object "geom" (an arcinst or primitive nodeinst) against cell instance "ni".
	 * Returns TRUE if there are design-rule violations in their interaction.
	 */
//	BOOLEAN dr_quickcheckgeomagainstinstance(GEOM *geom, NODEINST *ni, CHECKSTATE *state)
//	{
//		REGISTER NODEPROTO *np, *subcell;
//		REGISTER ARCINST *oai;
//		REGISTER NODEINST *oni;
//		REGISTER TECHNOLOGY *tech;
//		REGISTER BOOLEAN basemulti;
//		REGISTER INTBIG tot, j, bound, net, minsize, localindex, globalIndex;
//		INTBIG lx, hx, ly, hy, slx, shx, sly, shy;
//		XARRAY rtrans, ttrans, temptrans, subtrans, trans;
//		REGISTER POLYGON *poly;
//		REGISTER CHECKINST *ci;
//
//		np = ni->proto;
//		globalIndex = 0;
//		subcell = geomparent(geom);
//		if (geom->entryisnode)
//		{
//			// get all of the polygons on this node
//			oni = geom->entryaddr.ni;
//			makerot(oni, trans);
//			dr_quickgetnodeEpolys(oni, state->nodeinstpolylist, trans);
//			basemulti = dr_quickismulticut(oni);
//			tech = oni->proto->tech;
//		} else
//		{
//			oai = geom->entryaddr.ai;
//			transid(trans);
//			dr_quickgetarcpolys(oai, state->nodeinstpolylist, trans);
//			basemulti = FALSE;
//			tech = oai->proto->tech;
//		}
//		tot = state->nodeinstpolylist->polylistcount;
//
//		ci = (CHECKINST *)ni->temp1;
//		localindex = globalIndex * ci->multiplier + ci->localindex + ci->offset;
//
//		// examine the polygons on this node
//		for(j=0; j<tot; j++)
//		{
//			poly = state->nodeinstpolylist->polygons[j];
//			if (poly->layer < 0) continue;
//
//			// see how far around the box it is necessary to search
//			bound = maxdrcsurround(tech, subcell->lib, poly->layer);
//			if (bound < 0) continue;
//
//			// determine network for this polygon
//			if (geom->entryisnode)
//			{
//				net = dr_quickgetnetnumber(poly->portproto, oni, globalIndex);
//			} else
//			{
//				net = ((INTBIG *)oai->network->temp1)[globalIndex];
//			}
//
//			// determine if original object has multiple contact cuts
//			minsize = polyminsize(poly);
//
//			// determine area to search inside of cell to check this layer
//			getbbox(poly, &lx, &hx, &ly, &hy);
//			slx = lx-bound;   shx = hx+bound;
//			sly = ly-bound;   shy = hy+bound;
//			makerotI(ni, rtrans);
//			maketransI(ni, ttrans);
//			transmult(rtrans, ttrans, temptrans);
//			xformbox(&slx, &shx, &sly, &shy, temptrans);
//
//			maketrans(ni, ttrans);
//			makerot(ni, rtrans);
//			transmult(ttrans, rtrans, subtrans);
//
//			// see if this polygon has errors in the cell
//			if (dr_quickbadboxinarea(poly, poly->layer, tech, net, geom, trans, globalIndex,
//				slx, shx, sly, shy, np, localindex,
//				subcell, globalIndex, subtrans, minsize, basemulti, state, FALSE)) return(TRUE);
//		}
//		return(FALSE);
//	}

	/*************************** QUICK DRC CACHE OF INSTANCE INTERACTIONS ***************************/

	/*
	 * Method to look for an interaction between instances "ni1" and "ni2".  If it is found,
	 * return TRUE.  If not found, add to the list and return FALSE.
	 */
	boolean dr_quickcheckinteraction(NodeInst ni1, NodeInst ni2)
	{
		// must recheck parameterized instances always
//		cp = (CHECKPROTO *)ni1->proto->temp1;
//		if (cp->cellparameterized) return false;
//		cp = (CHECKPROTO *)ni2->proto->temp1;
//		if (cp->cellparameterized) return false;

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
				NodeInst swapni = ni1;   ni1 = ni2;   ni2 = swapni;
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
		if (dr_quickfindinteraction(dii)) return true;

		// BEGIN critical section
//		if (dr_quickparalleldrc) emutexlock(dr_quickmutexinteract);

		// insert it now
		instanceInteractionList.add(dii);

		// END critical section
//		if (dr_quickparalleldrc) emutexunlock(dr_quickmutexinteract);
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
	boolean dr_quickfindinteraction(InstanceInter dii)
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

	/*************************** QUICK DRC MULTIPROCESSOR SUPPORT ***************************/

	/*
	 * Method to initialize the various state blocks and interlocks to prepare for multiprocessor
	 * DRC using "numstates" processors.  Returns TRUE on error.
	 */
//	BOOLEAN dr_quickmakestateblocks(INTBIG numstates)
//	{
//		REGISTER INTBIG i;
//		REGISTER CHECKSTATE **newstates;
//		REGISTER void **newlocks;
//
//		// code cannot be called by multiple procesors: uses globals
//		NOT_REENTRANT;
//
//		if (numstates <= dr_quickstatecount) return(FALSE);
//		newstates = (CHECKSTATE **)emalloc(numstates * (sizeof (CHECKSTATE *)), dr_tool->cluster);
//		if (newstates == 0) return(TRUE);
//		newlocks = (void **)emalloc(numstates * (sizeof (void *)), dr_tool->cluster);
//		if (newlocks == 0) return(TRUE);
//		for(i=0; i<dr_quickstatecount; i++)
//		{
//			newstates[i] = dr_quickstate[i];
//			newlocks[i] = dr_quickprocessdone[i];
//		}
//		for(i=dr_quickstatecount; i<numstates; i++)
//		{
//			newstates[i] = (CHECKSTATE *)emalloc(sizeof(CHECKSTATE), dr_tool->cluster);
//			if (newstates[i] == 0) return(TRUE);
//
//			newstates[i]->cellinstpolylist = allocpolylist(dr_tool->cluster);
//			if (newstates[i]->cellinstpolylist == 0) return(TRUE);
//
//			newstates[i]->nodeinstpolylist = allocpolylist(dr_tool->cluster);
//			if (newstates[i]->nodeinstpolylist == 0) return(TRUE);
//
//			newstates[i]->arcinstpolylist = allocpolylist(dr_tool->cluster);
//			if (newstates[i]->arcinstpolylist == 0) return(TRUE);
//
//			newstates[i]->subpolylist = allocpolylist(dr_tool->cluster);
//			if (newstates[i]->subpolylist == 0) return(TRUE);
//
//			newstates[i]->cropnodepolylist = allocpolylist(dr_tool->cluster);
//			if (newstates[i]->cropnodepolylist == 0) return(TRUE);
//
//			newstates[i]->croparcpolylist = allocpolylist(dr_tool->cluster);
//			if (newstates[i]->croparcpolylist == 0) return(TRUE);
//
//			newstates[i]->layerlookpolylist = allocpolylist(dr_tool->cluster);
//			if (newstates[i]->layerlookpolylist == 0) return(TRUE);
//
//			newstates[i]->activecroppolylist = allocpolylist(dr_tool->cluster);
//			if (newstates[i]->activecroppolylist == 0) return(TRUE);
//
//			newstates[i]->checkdistpoly1rebuild = NOPOLYGON;
//			(void)needstaticpolygon(&newstates[i]->checkdistpoly1rebuild, 4, dr_tool->cluster);
//			newstates[i]->checkdistpoly2rebuild = NOPOLYGON;
//			(void)needstaticpolygon(&newstates[i]->checkdistpoly2rebuild, 4, dr_tool->cluster);
//
//			newstates[i]->hierarchybasesnapshot = newhierarchicaltraversal();
//			newstates[i]->hierarchytempsnapshot = newhierarchicaltraversal();
//
//			newlocks[i] = 0;
//			if (ensurevalidmutex(&newlocks[i], TRUE)) return(TRUE);
//		}
//		if (dr_quickstatecount > 0)
//		{
//			efree((CHAR *)dr_quickstate);
//			efree((CHAR *)dr_quickprocessdone);
//		}
//		dr_quickstate = newstates;
//		dr_quickprocessdone = newlocks;
//		dr_quickstatecount = numstates;
//		return(FALSE);
//	}

	/************************* QUICK DRC HIERARCHICAL NETWORK BUILDING *************************/

	/*
	 * Method to recursively examine the hierarchy below cell "cell" and fill in the
	 * CheckInst objects on every cell instance.  Uses the CheckProto objects
	 * to keep track of cell usage.
	 */
	private static void checkEnumerateInstances(Cell cell)
	{
		// number all of the instances in this cell
		dr_quickchecktimestamp++;
		List subCheckProtos = new ArrayList();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (!(np instanceof Cell)) continue;

			// ignore documentation icons
			if (ni.isIconOfParent()) continue;

			CheckProto cp = (CheckProto)checkProtos.get(np);
			if (cp.timestamp != dr_quickchecktimestamp)
			{
				cp.timestamp = dr_quickchecktimestamp;
				cp.instancecount = 0;
				cp.nodesInCell = new ArrayList();
				subCheckProtos.add(cp);
			}

			CheckInst ci = (CheckInst)checkInsts.get(ni);
			ci.localindex = cp.instancecount++;
			cp.nodesInCell.add(ci);
		}

		// update the counts for this cell
		for(Iterator it = subCheckProtos.iterator(); it.hasNext(); )
		{
			CheckProto cp = (CheckProto)it.next();
			cp.hierinstancecount += cp.instancecount;
			for(Iterator nIt = cp.nodesInCell.iterator(); nIt.hasNext(); )
			{
				CheckInst ci = (CheckInst)nIt.next();
				ci.multiplier = cp.instancecount;
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

	private static void checkEnumerateNetworks(Cell cell, int globalIndex, HashMap enumeratedNets)
	{
		// store all network information in the appropriate place
		for(Iterator nIt = cell.getNetworks(); nIt.hasNext(); )
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
			int localindex = globalIndex * ci.multiplier + ci.localindex + ci.offset;

			// propagate down the hierarchy
			Cell subCell = (Cell)np;

			HashMap subEnumeratedNets = new HashMap();
			for(Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
			{
				PortInst pi = (PortInst)pIt.next();
				JNetwork net = pi.getNetwork();
				if (net == null) continue;
				Integer netNumber = (Integer)enumeratedNets.get(net);
				Export subPP = (Export)pi.getPortProto();
				subEnumeratedNets.put(subPP.getNetwork(), netNumber);
			}
			for(Iterator nIt = subCell.getNetworks(); nIt.hasNext(); )
			{
				JNetwork net = (JNetwork)nIt.next();
				if (subEnumeratedNets.get(net) == null)
					subEnumeratedNets.put(net, new Integer(dr_quickchecknetnumber++));
			}
			checkEnumerateNetworks(subCell, localindex, subEnumeratedNets);
		}
	}

	/*********************************** QUICK DRC SUPPORT ***********************************/

	/*
	 * Method to ensure that polygon "poly" on layer "layer" from object "geom" in
	 * technology "tech" meets minimum width rules.  If it is too narrow, other layers
	 * in the vicinity are checked to be sure it is indeed an error.  Returns true
	 * if an error is found.
	 */
	private static boolean dr_quickcheckminwidth(Geometric geom, Layer layer, Poly poly, Technology tech)
	{
//		cell = geomparent(geom);
//		minwidth = drcminwidth(tech, cell->lib, layer, &rule);
//		if (minwidth < 0) return(FALSE);
//
//		// simpler analysis if manhattan
//		if (isbox(poly, &lx, &hx, &ly, &hy))
//		{
//			if (hx - lx >= minwidth && hy - ly >= minwidth) return(FALSE);
//			if (hx - lx < minwidth)
//			{
//				actual = hx - lx;
//				xl1 = xl2 = xl3 = lx - 1;
//				xr1 = xr2 = xr3 = hx + 1;
//				yl1 = yr1 = ly;
//				yl2 = yr2 = hy;
//				yl3 = yr3 = (yl1+yl2)/2;
//			} else
//			{
//				actual = hy - ly;
//				xl1 = xr1 = lx;
//				xl2 = xr2 = hx;
//				xl3 = xr3 = (xl1+xl2)/2;
//				yl1 = yl2 = yl3 = ly - 1;
//				yr1 = yr2 = yr3 = hy + 1;
//			}
//
//			// see if there is more of this layer adjoining on either side
//			p1found = p2found = p3found = FALSE;
//			if (dr_quicklookforlayer(cell, layer, el_matid, state, lx-1, hx+1, ly-1, hy+1,
//				xl1, yl1, &p1found, xl2, yl2, &p2found, xl3, yl3, &p3found)) return(FALSE);
//
//			p1found = p2found = p3found = FALSE;
//			if (dr_quicklookforlayer(cell, layer, el_matid, state, lx-1, hx+1, ly-1, hy+1,
//				xr1, yr1, &p1found, xr2, yr2, &p2found, xr3, yr3, &p3found)) return(FALSE);
//
//			reportError(MINWIDTHERROR, tech, 0, cell, minwidth, actual, rule,
//				poly, geom, layer, NONET, NOPOLYGON, NOGEOM, 0, NONET);
//			return(TRUE);
//		}
//
//		// nonmanhattan polygon: stop now if it has no size
//		switch (poly->style)
//		{
//			case FILLED:
//			case CLOSED:
//			case CROSSED:
//			case OPENED:
//			case OPENEDT1:
//			case OPENEDT2:
//			case OPENEDT3:
//			case VECTORS:
//				break;
//			default:
//				return(FALSE);
//		}
//
//		// simple check of nonmanhattan polygon for minimum width
//		getbbox(poly, &lx, &hx, &ly, &hy);
//		actual = mini(hx-lx, hy-ly);
//		if (actual < minwidth)
//		{
//			reportError(MINWIDTHERROR, tech, 0, cell, minwidth, actual, rule,
//				poly, geom, layer, NONET, NOPOLYGON, NOGEOM, 0, NONET);
//			return(TRUE);
//		}
//
//		// check distance of each line's midpoint to perpendicular opposite point
//		for(i=0; i<poly->count; i++)
//		{
//			if (i == 0)
//			{
//				fx = poly->xv[poly->count-1];   fy = poly->yv[poly->count-1];
//			} else
//			{
//				fx = poly->xv[i-1];   fy = poly->yv[i-1];
//			}
//			tx = poly->xv[i];   ty = poly->yv[i];
//			if (fx == tx && fy == ty) continue;
//			ang = ffigureangle(fx, fy, tx, ty);
//			cx = (fx + tx) / 2;
//			cy = (fy + ty) / 2;
//			perpang = ang + EPI/2;
//			for(j=0; j<poly->count; j++)
//			{
//				if (j == i) continue;
//				if (j == 0)
//				{
//					ofx = poly->xv[poly->count-1];   ofy = poly->yv[poly->count-1];
//				} else
//				{
//					ofx = poly->xv[j-1];   ofy = poly->yv[j-1];
//				}
//				otx = poly->xv[j];   oty = poly->yv[j];
//				if (ofx == otx && ofy == oty) continue;
//				oang = ffigureangle(ofx, ofy, otx, oty);
//				rang = ang;   while (rang > EPI) rang -= EPI;
//				roang = oang;   while (roang > EPI) roang -= EPI;
//				if (doublesequal(oang, roang))
//				{
//					// lines are parallel: see if they are colinear
//					if (isonline(fx, fy, tx, ty, ofx, ofy)) continue;
//					if (isonline(fx, fy, tx, ty, otx, oty)) continue;
//					if (isonline(ofx, ofy, otx, oty, fx, fy)) continue;
//					if (isonline(ofx, ofy, otx, oty, tx, ty)) continue;
//				}
//				if (fintersect(cx, cy, perpang, ofx, ofy, oang, &ix, &iy) < 0) continue;
//				if (ix < mini(ofx, otx) || ix > maxi(ofx, otx)) continue;
//				if (iy < mini(ofy, oty) || iy > maxi(ofy, oty)) continue;
//				fdx = cx - ix;   fdy = cy - iy;
//				actual = rounddouble(sqrt(fdx*fdx + fdy*fdy));
//
//				// becuase this is done in integer, accuracy may suffer
//				actual += 2;
//
//				if (actual < minwidth)
//				{
//					// look between the points to see if it is minimum width or notch
//					if (isinside((cx+ix)/2, (cy+iy)/2, poly))
//					{
//						reportError(MINWIDTHERROR, tech, 0, cell, minwidth,
//							actual, rule, poly, geom, layer, NONET, NOPOLYGON, NOGEOM, 0, NONET);
//					} else
//					{
//						reportError(NOTCHERROR, tech, 0, cell, minwidth,
//							actual, rule, poly, geom, layer, NONET, poly, geom, layer, NONET);
//					}
//					return(TRUE);
//				}
//			}
//		}
		return false;
	}

	/*
	 * Method to examine the hierarchy below cell "cell" and return TRUE if any of it is
	 * parameterized.
	 */
	private static boolean findParameters(Cell cell)
	{
		CheckProto cp = (CheckProto)checkProtos.get(cell);
		if (cp.cellparameterized) return true;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (!(np instanceof Cell)) continue;
			if (ni.isIconOfParent()) continue;
			if (findParameters((Cell)np)) return true;
		}
		return false;
	}

	/*
	 * Method to determine whether node "ni" is a multiple cut contact.
	 */
//	BOOLEAN dr_quickismulticut(NODEINST *ni)
//	{
//		REGISTER NODEPROTO *np;
//		REGISTER TECHNOLOGY *tech;
//		TECH_NODES *thistn;
//		INTBIG fewer, cutcount;
//		POLYLOOP mypl;
//
//		np = ni->proto;
//		if (np->primindex == 0) return(FALSE);
//		tech = np->tech;
//		thistn = tech->nodeprotos[np->primindex-1];
//		if (thistn->special != MULTICUT) return(FALSE);
//		cutcount = tech_moscutcount(ni, thistn->f1, thistn->f2, thistn->f3, thistn->f4,
//			&fewer, &mypl);
//		if (cutcount > 1) return(TRUE);
//		return(FALSE);
//	}

	/*
	 * Method to tell whether the objects at geometry modules "geom1" and "geom2"
	 * touch directly (that is, an arcinst connected to a nodeinst).  The method
	 * returns true if they touch
	 */
//	BOOLEAN dr_quickobjtouch(GEOM *geom1, GEOM *geom2)
//	{
//		REGISTER GEOM *temp;
//		REGISTER NODEINST *ni;
//		REGISTER ARCINST *ai;
//		REGISTER INTBIG i;
//
//		if (geom1->entryisnode)
//		{
//			if (geom2->entryisnode) return(FALSE);
//			temp = geom1;   geom1 = geom2;   geom2 = temp;
//		}
//		if (!geom2->entryisnode)
//			return(FALSE);
//
//		// see if the arcinst at "geom1" touches the nodeinst at "geom2"
//		ni = geom2->entryaddr.ni;
//		ai = geom1->entryaddr.ai;
//		for(i=0; i<2; i++)
//			if (ai->end[i].nodeinst == ni)
//				return(TRUE);
//		return(FALSE);
//	}

	/*
	 * Method to find two points between polygons "poly1" and "poly2" that can be used to test
	 * for notches.  The points are returned in (xf1,yf1) and (xf2,yf2).  Returns zero if no
	 * points exist in the gap between the polygons (becuase they don't have common facing edges).
	 * Returns 1 if only one of the reported points needs to be filled (because the polygons meet
	 * at a point).  Returns 2 if both reported points need to be filled.
	 */
//	INTBIG dr_quickfindinterveningpoints(POLYGON *poly1, POLYGON *poly2, INTBIG *xf1, INTBIG *yf1,
//		INTBIG *xf2, INTBIG *yf2)
//	{
//		REGISTER BOOLEAN isbox1, isbox2;
//		INTBIG lx1, hx1, ly1, hy1, lx2, hx2, ly2, hy2;
//		REGISTER INTBIG xc, yc;
//
//		isbox1 = isbox(poly1, &lx1, &hx1, &ly1, &hy1);
//		isbox2 = isbox(poly2, &lx2, &hx2, &ly2, &hy2);
//		if (isbox1 && isbox2)
//		{
//			// handle vertical gap between polygons
//			if (lx1 > hx2 || lx2 > hx1)
//			{
//				// see if the polygons are horizontally aligned
//				if (ly1 <= hy2 && ly2 <= hy1)
//				{
//					if (lx1 > hx2) *xf1 = *xf2 = (lx1 + hx2) / 2; else
//						*xf1 = *xf2 = (lx2 + hx1) / 2;
//					*yf1 = maxi(ly1, ly2);
//					*yf2 = mini(hy1, hy2);
//					return(2);
//				}
//			} else if (ly1 > hy2 || ly2 > hy1)
//			{
//				// see if the polygons are horizontally aligned
//				if (lx1 <= hx2 && lx2 <= hx1)
//				{
//					if (ly1 > hy2) *yf1 = *yf2 = (ly1 + hy2) / 2; else
//						*yf1 = *yf2 = (ly2 + hy1) / 2;
//					*xf1 = maxi(lx1, lx2);
//					*xf2 = mini(hx1, hx2);
//					return(2);
//				}
//			} else if ((lx1 == hx2 || lx2 == hx1) || (ly1 == hy2 || ly2 == hy2))
//			{
//				// handle touching at a point
//				if (lx1 == hx2) xc = lx1; else
//					xc = lx2;
//				if (ly1 == hy2) yc = ly1; else
//					yc = ly2;
//				*xf1 = xc + 1;   *yf1 = yc + 1;
//				*xf2 = xc - 1;   *yf2 = yc - 1;
//				if ((*xf1 < lx1 || *xf1 > hx1 || *yf1 < ly1 || *yf1 > hy1) &&
//					(*xf1 < lx2 || *xf1 > hx2 || *yf1 < ly2 || *yf1 > hy2)) return(1);
//				*xf1 = xc + 1;   *yf1 = yc - 1;
//				*xf2 = xc - 1;   *yf2 = yc + 1;
//				return(1);
//			}
//
//			// handle manhattan objects that are on a diagonal
//			if (lx1 > hx2)
//			{
//				if (ly1 > hy2)
//				{
//					*xf1 = lx1;   *yf1 = ly1;
//					*xf2 = hx2;   *yf2 = hy2;
//					return(1);
//				}
//				if (ly2 > hy1)
//				{
//					*xf1 = lx1;   *yf1 = hy1;
//					*xf2 = hx2;   *yf2 = ly2;
//					return(1);
//				}
//			}
//			if (lx2 > hx1)
//			{
//				if (ly1 > hy2)
//				{
//					*xf1 = hx1;   *yf1 = hy1;
//					*xf2 = lx2;   *yf2 = ly2;
//					return(1);
//				}
//				if (ly2 > hy1)
//				{
//					*xf1 = hx1;   *yf1 = ly1;
//					*xf2 = lx2;   *yf2 = hy2;
//					return(1);
//				}
//			}
//		}
//
//		// boxes don't line up or this is a nonmanhattan situation
//		*xf1 = (lx1 + hx1) / 2;   *yf1 = (ly2 + hy2) / 2;
//		*xf2 = (lx2 + hx2) / 2;   *yf2 = (ly1 + hy1) / 2;
//		return(1);
//	}

	/*
	 * Method to explore the points (xf1,yf1) and (xf2,yf2) to see if there is
	 * geometry on layer "layer" (in or below cell "cell").  Returns true if there is.
	 * If "needboth" is true, both points must have geometry, otherwise only 1 needs it.
	 */
//	BOOLEAN dr_quicklookforpoints(INTBIG xf1, INTBIG yf1, INTBIG xf2, INTBIG yf2,
//		INTBIG layer, NODEPROTO *cell, BOOLEAN needboth, CHECKSTATE *state)
//	{
//		INTBIG xf3, yf3, flx, fhx, fly, fhy;
//		BOOLEAN p1found, p2found, p3found, allfound;
//
//		xf3 = (xf1+xf2) / 2;
//		yf3 = (yf1+yf2) / 2;
//
//		// compute bounds for searching inside cells
//		flx = mini(xf1, xf2);   fhx = maxi(xf1, xf2);
//		fly = mini(yf1, yf2);   fhy = maxi(yf1, yf2);
//
//		// search the cell for geometry that fills the notch
//		p1found = p2found = p3found = FALSE;
//		allfound = dr_quicklookforlayer(cell, layer, el_matid, state, flx, fhx, fly, fhy,
//			xf1, yf1, &p1found, xf2, yf2, &p2found, xf3, yf3, &p3found);
//		if (needboth)
//		{
//			if (allfound) return(TRUE);
//		} else
//		{
//			if (p1found || p2found) return(TRUE);
//		}
//
//		return(FALSE);
//	}

	/*
	 * Method to examine cell "cell" in the area (lx<=X<=hx, ly<=Y<=hy) for objects
	 * on layer "layer".  Apply transformation "moretrans" to the objects.  If polygons are
	 * found at (xf1,yf1) or (xf2,yf2) or (xf3,yf3) then sets "p1found/p2found/p3found" to 1.
	 * If all locations are found, returns true.
	 */
//	BOOLEAN dr_quicklookforlayer(NODEPROTO *cell, INTBIG layer, XARRAY moretrans, CHECKSTATE *state,
//		INTBIG lx, INTBIG hx, INTBIG ly, INTBIG hy, INTBIG xf1, INTBIG yf1, BOOLEAN *p1found,
//			INTBIG xf2, INTBIG yf2, BOOLEAN *p2found, INTBIG xf3, INTBIG yf3, BOOLEAN *p3found)
//	{
//		REGISTER INTBIG sea, i, tot;
//		REGISTER GEOM *g;
//		REGISTER NODEINST *ni;
//		REGISTER ARCINST *ai;
//		REGISTER BOOLEAN reasonable;
//		INTBIG newlx, newhx, newly, newhy;
//		XARRAY trans, rot, bound;
//		REGISTER POLYGON *poly;
//
//		sea = initsearch(lx, hx, ly, hy, cell);
//		for(;;)
//		{
//			g = nextobject(sea);
//			if (g == NOGEOM) break;
//			if (g->entryisnode)
//			{
//				ni = g->entryaddr.ni;
//				if (ni->proto->primindex == 0)
//				{
//					// compute bounding area inside of sub-cell
//					makerotI(ni, rot);
//					maketransI(ni, trans);
//					transmult(rot, trans, bound);
//					newlx = lx;   newly = ly;   newhx = hx;   newhy = hy;
//					xformbox(&newlx, &newhx, &newly, &newhy, bound);
//
//					// compute new matrix for sub-cell examination
//					maketrans(ni, trans);
//					makerot(ni, rot);
//					transmult(trans, rot, bound);
//					transmult(bound, moretrans, trans);
//					if (dr_quicklookforlayer(ni->proto, layer, trans, state, newlx, newhx, newly, newhy,
//						xf1, yf1, p1found, xf2, yf2, p2found, xf3, yf3, p3found))
//					{
//						termsearch(sea);
//						return(TRUE);
//					}
//					continue;
//				}
//				makerot(ni, rot);
//				transmult(rot, moretrans, bound);
//				if (ignoreCenterCuts) reasonable = TRUE; else reasonable = FALSE;
//				tot = allnodepolys(ni, state->layerlookpolylist, NOWINDOWPART, reasonable);
//				for(i=0; i<tot; i++)
//				{
//					poly = state->layerlookpolylist->polygons[i];
//					if (!samelayer(poly->tech, poly->layer, layer)) continue;
//					xformpoly(poly, bound);
//					if (isinside(xf1, yf1, poly)) *p1found = TRUE;
//					if (isinside(xf2, yf2, poly)) *p2found = TRUE;
//					if (isinside(xf3, yf3, poly)) *p3found = TRUE;
//				}
//			} else
//			{
//				ai = g->entryaddr.ai;
//				tot = allarcpolys(ai, state->layerlookpolylist, NOWINDOWPART);
//				for(i=0; i<tot; i++)
//				{
//					poly = state->layerlookpolylist->polygons[i];
//					if (!samelayer(poly->tech, poly->layer, layer)) continue;
//					xformpoly(poly, moretrans);
//					if (isinside(xf1, yf1, poly)) *p1found = TRUE;
//					if (isinside(xf2, yf2, poly)) *p2found = TRUE;
//					if (isinside(xf3, yf3, poly)) *p3found = TRUE;
//				}
//			}
//			if (*p1found && *p2found && *p3found)
//			{
//				termsearch(sea);
//				return(TRUE);
//			}
//		}
//		return(FALSE);
//	}

	/*
	 * Method to see if the two boxes are active elements, connected to opposite
	 * sides of a field-effect transistor that resides inside of the box area.
	 * Returns true if so.
	 */
//	BOOLEAN dr_quickactiveontransistor(POLYGON *poly1, INTBIG layer1, INTBIG net1,
//		POLYGON *poly2, INTBIG layer2, INTBIG net2, TECHNOLOGY *tech, NODEPROTO *cell, INTBIG globalIndex)
//	{
//		REGISTER INTBIG blx, bhx, bly, bhy, fun;
//		INTBIG lx1, hx1, ly1, hy1, lx2, hx2, ly2, hy2;
//
//		// networks must be different
//		if (net1 == net2) return(FALSE);
//
//		// layers must be active or active contact
//		fun = layerfunction(tech, layer1);
//		if ((fun&LFTYPE) != LFDIFF)
//		{
//			if (!layeriscontact(fun) || (fun&LFCONDIFF) == 0) return(FALSE);
//		}
//		fun = layerfunction(tech, layer2);
//		if ((fun&LFTYPE) != LFDIFF)
//		{
//			if (!layeriscontact(fun) || (fun&LFCONDIFF) == 0) return(FALSE);
//		}
//
//		// search for intervening transistor in the cell
//		getbbox(poly1, &lx1, &hx1, &ly1, &hy1);
//		getbbox(poly2, &lx2, &hx2, &ly2, &hy2);
//		blx = mini(lx1,lx2);   bhx = maxi(hx1,hx2);
//		bly = mini(ly1,ly2);   bhy = maxi(hy1,hy2);
//		return(dr_quickactiveontransistorrecurse(blx, bhx, bly, bhy, net1, net2, cell, globalIndex, el_matid));
//	}

//	BOOLEAN dr_quickactiveontransistorrecurse(INTBIG blx, INTBIG bhx, INTBIG bly, INTBIG bhy,
//		INTBIG net1, INTBIG net2, NODEPROTO *cell, INTBIG globalIndex, XARRAY trans)
//	{
//		REGISTER INTBIG sea, net, cx, cy, localindex;
//		REGISTER BOOLEAN on1, on2, ret;
//		REGISTER GEOM *g;
//		REGISTER NODEINST *ni;
//		REGISTER NODEPROTO *np;
//		REGISTER PORTPROTO *badport;
//		REGISTER PORTARCINST *pi;
//		INTBIG slx, shx, sly, shy;
//		XARRAY rtrans, ttrans, temptrans;
//		REGISTER CHECKINST *ci;
//
//		sea = initsearch(blx, bhx, bly, bhy, cell);
//		if (sea < 0) return(FALSE);
//		for(;;)
//		{
//			g = nextobject(sea);
//			if (g == NOGEOM) break;
//			if (!g->entryisnode) continue;
//			ni = g->entryaddr.ni;
//			np = ni->proto;
//			if (np->primindex == 0)
//			{
//				makerotI(ni, rtrans);
//				maketransI(ni, ttrans);
//				transmult(rtrans, ttrans, temptrans);
//				slx = blx;   shx = bhx;
//				sly = bly;   shy = bhy;
//				xformbox(&slx, &shx, &sly, &shy, temptrans);
//
//				ci = (CHECKINST *)ni->temp1;
//				localindex = globalIndex * ci->multiplier + ci->localindex + ci->offset;
//
//				if (!dr_quickparalleldrc) downhierarchy(ni, np, 0);
//				ret = dr_quickactiveontransistorrecurse(slx, shx, sly, shy,
//					net1, net2, np, localindex, trans);
//				if (!dr_quickparalleldrc) uphierarchy();
//				if (ret)
//				{
//					termsearch(sea);
//					return(TRUE);
//				}
//				continue;
//			}
//
//			// must be a transistor
//			if (!isfet(g)) continue;
//
//			// must be inside of the bounding box of the desired layers
//			cx = (ni->geom->lowx + ni->geom->highx) / 2;
//			cy = (ni->geom->lowy + ni->geom->highy) / 2;
//			if (cx < blx || cx > bhx || cy < bly || cy > bhy) continue;
//
//			// determine the poly port
//			badport = ni->proto->firstportproto;
//
//			on1 = on2 = FALSE;
//			for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//			{
//				// ignore connections on poly/gate
//				if (pi->proto->network == badport->network) continue;
//
//				net = ((INTBIG *)pi->conarcinst->network->temp1)[globalIndex];
//				if (net == NONET) continue;
//				if (net == net1) on1 = TRUE;
//				if (net == net2) on2 = TRUE;
//			}
//
//			// if either side is not connected, ignore this
//			if (!on1 || !on2) continue;
//
//			// transistor found that connects to both actives
//			termsearch(sea);
//			return(TRUE);
//		}
//		return(FALSE);
//	}

	/*
	 * Method to crop the box in the reference parameters (lx-hx, ly-hy)
	 * against the box in (bx-ux, by-uy).  If the box is cropped into oblivion,
	 * returns 1.  If the boxes overlap but cannot be cleanly cropped,
	 * returns -1.  Otherwise the box is cropped and zero is returned
	 */
//	INTBIG dr_quickcropbox(INTBIG *lx, INTBIG *hx, INTBIG *ly, INTBIG *hy, INTBIG bx, INTBIG ux, INTBIG by,
//		INTBIG uy, INTBIG nlx, INTBIG nhx, INTBIG nly, INTBIG nhy)
//	{
//		REGISTER INTBIG xoverlap, yoverlap;
//
//		// if the two boxes don't touch, just return
//		if (bx >= *hx || by >= *hy || ux <= *lx || uy <= *ly) return(0);
//
//		// if the box to be cropped is within the other, say so
//		if (bx <= *lx && ux >= *hx && by <= *ly && uy >= *hy) return(1);
//
//		// see which direction is being cropped
//		xoverlap = mini(*hx, ux) - maxi(*lx, bx);
//		yoverlap = mini(*hy, uy) - maxi(*ly, by);
//		if (xoverlap > yoverlap)
//		{
//			// one above the other: crop in Y
//			if (bx <= *lx && ux >= *hx)
//			{
//				// it covers in X...do the crop
//				if (uy >= *hy) *hy = by;
//				if (by <= *ly) *ly = uy;
//				if (*hy <= *ly) return(1);
//				return(0);
//			}
//		} else
//		{
//			// one next to the other: crop in X
//			if (by <= *ly && uy >= *hy)
//			{
//				// it covers in Y...crop in X
//				if (ux >= *hx) *hx = bx;
//				if (bx <= *lx) *lx = ux;
//				if (*hx <= *lx) return(1);
//				return(0);
//			}
//		}
//		return(-1);
//	}

	/*
	 * Method to crop the box in the reference parameters (lx-hx, ly-hy)
	 * against the box in (bx-ux, by-uy).  If the box is cropped into oblivion,
	 * returns 1.  If the boxes overlap but cannot be cleanly cropped,
	 * returns -1.  Otherwise the box is cropped and zero is returned
	 */
//	INTBIG dr_quickhalfcropbox(INTBIG *lx, INTBIG *hx, INTBIG *ly, INTBIG *hy,
//		INTBIG bx, INTBIG ux, INTBIG by, INTBIG uy)
//	{
//		REGISTER BOOLEAN crops;
//		REGISTER INTBIG lxe, hxe, lye, hye, biggestext;
//
//		// if the two boxes don't touch, just return
//		if (bx >= *hx || by >= *hy || ux <= *lx || uy <= *ly) return(0);
//
//		// if the box to be cropped is within the other, figure out which half to remove
//		if (bx <= *lx && ux >= *hx && by <= *ly && uy >= *hy)
//		{
//			lxe = *lx - bx;   hxe = ux - *hx;
//			lye = *ly - by;   hye = uy - *hy;
//			biggestext = maxi(maxi(lxe, hxe), maxi(lye, hye));
//			if (lxe == biggestext)
//			{
//				*lx = (*lx + ux) / 2;
//				if (*lx >= *hx) return(1);
//				return(0);
//			}
//			if (hxe == biggestext)
//			{
//				*hx = (*hx + bx) / 2;
//				if (*hx <= *lx) return(1);
//				return(0);
//			}
//			if (lye == biggestext)
//			{
//				*ly = (*ly + uy) / 2;
//				if (*ly >= *hy) return(1);
//				return(0);
//			}
//			if (hye == biggestext)
//			{
//				*hy = (*hy + by) / 2;
//				if (*hy <= *ly) return(1);
//				return(0);
//			}
//		}
//
//		// reduce (lx-hx,ly-hy) by (bx-ux,by-uy)
//		crops = FALSE;
//		if (bx <= *lx && ux >= *hx)
//		{
//			// it covers in X...crop in Y
//			if (uy >= *hy) *hy = (*hy + by) / 2;
//			if (by <= *ly) *ly = (*ly + uy) / 2;
//			crops = TRUE;
//		}
//		if (by <= *ly && uy >= *hy)
//		{
//			// it covers in Y...crop in X
//			if (ux >= *hx) *hx = (*hx + bx) / 2;
//			if (bx <= *lx) *lx = (*lx + ux) / 2;
//			crops = TRUE;
//		}
//		if (!crops) return(-1);
//		return(0);
//	}

	/*
	 * Method to crop the box on layer "nlayer", electrical index "nnet"
	 * and bounds (lx-hx, ly-hy) against the nodeinst "ni".  The geometry in nodeinst "ni"
	 * that is being checked runs from (nlx-nhx, nly-nhy).  Only those layers
	 * in the nodeinst that are the same layer and the same electrical network
	 * are checked.  Returns true if the bounds are reduced
	 * to nothing.
	 */
//	BOOLEAN dr_quickcropnodeinst(NODEINST *ni, INTBIG globalIndex, CHECKSTATE *state,
//		XARRAY trans, INTBIG ninet, INTBIG nlx, INTBIG nhx, INTBIG nly, INTBIG nhy,
//		INTBIG nlayer, INTBIG nnet, GEOM *ngeom, INTBIG *lx, INTBIG *hx, INTBIG *ly, INTBIG *hy)
//	{
//		INTBIG xl, xh, yl, yh;
//		REGISTER INTBIG tot, j, isconnected, net;
//		REGISTER BOOLEAN allgone;
//		REGISTER INTBIG temp;
//		REGISTER POLYGON *poly;
//
//		dr_quickgetnodeEpolys(ni, state->cropnodepolylist, trans);
//		tot = state->cropnodepolylist->polylistcount;
//		if (tot < 0) return(FALSE);
//		isconnected = 0;
//		for(j=0; j<tot; j++)
//		{
//			poly = state->cropnodepolylist->polygons[j];
//			if (!samelayer(poly->tech, poly->layer, nlayer)) continue;
//			if (nnet != NONET)
//			{
//				if (poly->portproto == NOPORTPROTO) continue;
//
//				// determine network for this polygon
//				net = dr_quickgetnetnumber(poly->portproto, ni, globalIndex);
//				if (net != NONET && net != nnet) continue;
//			}
//			isconnected++;
//			break;
//		}
//		if (isconnected == 0) return(FALSE);
//
//		// get the description of the nodeinst layers
//		allgone = FALSE;
//		dr_quickgetnodepolys(ni, state->cropnodepolylist, trans);
//		tot = state->cropnodepolylist->polylistcount;
//		if (tot < 0) return(FALSE);
//
//		if (gettrace(ni) != NOVARIABLE)
//		{
//			// node is defined with an outline: use advanced methods to crop
//			void *merge;
//
//			merge = mergenew(dr_tool->cluster);
//			makerectpoly(*lx, *hx, *ly, *hy, state->checkdistpoly1rebuild);
//			mergeaddpolygon(merge, nlayer, el_curtech, state->checkdistpoly1rebuild);
//			for(j=0; j<tot; j++)
//			{
//				poly = state->cropnodepolylist->polygons[j];
//				if (!samelayer(poly->tech, poly->layer, nlayer)) continue;
//				mergesubpolygon(merge, nlayer, el_curtech, poly);
//			}
//			if (!mergebbox(merge, lx, hx, ly, hy))
//				allgone = TRUE;
//			mergedelete(merge);
//		} else
//		{
//			for(j=0; j<tot; j++)
//			{
//				poly = state->cropnodepolylist->polygons[j];
//				if (!samelayer(poly->tech, poly->layer, nlayer)) continue;
//
//				// warning: does not handle arbitrary polygons, only boxes
//				if (!isbox(poly, &xl, &xh, &yl, &yh)) continue;
//				temp = dr_quickcropbox(lx, hx, ly, hy, xl, xh, yl, yh, nlx, nhx, nly, nhy);
//				if (temp > 0) { allgone = TRUE; break; }
//				if (temp < 0)
//				{
//					state->tinynodeinst = ni;
//					state->tinyobj = ngeom;
//				}
//			}
//		}
//		return(allgone);
//	}

	/*
	 * Method to crop away any part of layer "lay" of arcinst "ai" that coincides
	 * with a similar layer on a connecting nodeinst.  The bounds of the arcinst
	 * are in the reference parameters (lx-hx, ly-hy).  Returns false
	 * normally, 1 if the arcinst is cropped into oblivion.
	 */
//	BOOLEAN dr_quickcroparcinst(ARCINST *ai, INTBIG lay, INTBIG *lx, INTBIG *hx, INTBIG *ly, INTBIG *hy, CHECKSTATE *state)
//	{
//		INTBIG xl, xh, yl, yh;
//		XARRAY trans;
//		REGISTER INTBIG i, j, tot;
//		REGISTER INTBIG temp;
//		REGISTER NODEINST *ni;
//		REGISTER NODEPROTO *np;
//		REGISTER PORTPROTO *pp;
//		REGISTER POLYGON *poly;
//
//		for(i=0; i<2; i++)
//		{
//			// find the primitive nodeinst at the true end of the portinst
//			ni = ai->end[i].nodeinst;   np = ni->proto;
//			pp = ai->end[i].portarcinst->proto;
//			while (np->primindex == 0)
//			{
//				ni = pp->subnodeinst;   np = ni->proto;
//				pp = pp->subportproto;
//			}
//			makerot(ni, trans);
//			dr_quickgetnodepolys(ni, state->croparcpolylist, trans);
//			tot = state->croparcpolylist->polylistcount;
//			for(j=0; j<tot; j++)
//			{
//				poly = state->croparcpolylist->polygons[j];
//				if (!samelayer(poly->tech, poly->layer, lay)) continue;
//
//				// warning: does not handle arbitrary polygons, only boxes
//				if (!isbox(poly, &xl, &xh, &yl, &yh)) continue;
//				temp = dr_quickhalfcropbox(lx, hx, ly, hy, xl, xh, yl, yh);
//				if (temp > 0) return(TRUE);
//				if (temp < 0)
//				{
//					state->tinynodeinst = ni;
//					state->tinyobj = ai->geom;
//				}
//			}
//		}
//		return(FALSE);
//	}

	/*
	 * Method to see if polygons in "plist" (describing arc "ai") should be cropped against a
	 * connecting transistor.  Crops the polygon if so.
	 */
//	void dr_quickcropactivearc(ARCINST *ai, POLYLIST *plist, CHECKSTATE *state)
//	{
//		REGISTER INTBIG i, j, k, fun, tot, ntot;
//		INTBIG lx, hx, ly, hy, nlx, nhx, nly, nhy;
//		REGISTER NODEINST *ni;
//		REGISTER POLYGON *poly, *npoly, *swappoly;
//		REGISTER BOOLEAN cropped;
//		XARRAY trans;
//
//		// look for an active layer in this arc
//		tot = plist->polylistcount;
//		for(j=0; j<tot; j++)
//		{
//			poly = plist->polygons[j];
//			fun = layerfunction(poly->tech, poly->layer);
//			if ((fun&LFTYPE) == LFDIFF) break;
//		}
//		if (j >= tot) return;
//
//		// must be manhattan
//		if (!isbox(poly, &lx, &hx, &ly, &hy)) return;
//
//		// search for adjoining transistor in the cell
//		cropped = FALSE;
//		for(i=0; i<2; i++)
//		{
//			ni = ai->end[i].nodeinst;
//			if (!isfet(ni->geom)) continue;
//
//			// crop the arc against this transistor
//			makerot(ni, trans);
//			dr_quickgetnodepolys(ni, state->activecroppolylist, trans);
//			ntot = state->activecroppolylist->polylistcount;
//			for(k=0; k<ntot; k++)
//			{
//				npoly = state->activecroppolylist->polygons[k];
//				if (npoly->tech != poly->tech || npoly->layer != poly->layer) continue;
//				if (!isbox(npoly, &nlx, &nhx, &nly, &nhy)) continue;
//				if (dr_quickhalfcropbox(&lx, &hx, &ly, &hy, nlx, nhx, nly, nhy) == 1)
//				{
//					// remove this polygon from consideration
//					swappoly = plist->polygons[j];
//					plist->polygons[j] = plist->polygons[tot-1];
//					plist->polygons[tot-1] = swappoly;
//					plist->polylistcount--;
//					return;
//				}
//				cropped = TRUE;
//			}
//		}
//		if (cropped) makerectpoly(lx, hx, ly, hy, poly);
//	}

	/*
	 * These methods get all the polygons in a primitive instance, and
	 * store them in the POLYLIST structure.
	 */
//	void dr_quickgetnodeEpolys(NODEINST *ni, POLYLIST *plist, XARRAY trans)
//	{
//		REGISTER INTBIG j;
//		BOOLEAN convertpseudo, onlyreasonable;
//		REGISTER POLYGON *poly;
//
//		convertpseudo = FALSE;
//		if (((ni->proto->userbits&NFUNCTION) >> NFUNCTIONSH) == NPPIN)
//		{
//			if (ni->firstportarcinst == NOPORTARCINST &&
//				ni->firstportexpinst != NOPORTEXPINST)
//					convertpseudo = TRUE;
//		}
//		if (ignoreCenterCuts) onlyreasonable = TRUE; else
//			onlyreasonable = FALSE;
//		plist->polylistcount = allnodeEpolys(ni, plist, NOWINDOWPART, onlyreasonable);
//		for(j = 0; j < plist->polylistcount; j++)
//		{
//			poly = plist->polygons[j];
//			xformpoly(poly, trans);
//			getbbox(poly, &plist->lx[j], &plist->hx[j], &plist->ly[j], &plist->hy[j]);
//			if (convertpseudo)
//				poly->layer = nonpseudolayer(poly->layer, poly->tech);
//		}
//	}

//	void dr_quickgetnodepolys(NODEINST *ni, POLYLIST *plist, XARRAY trans)
//	{
//		REGISTER INTBIG j;
//		REGISTER POLYGON *poly;
//		BOOLEAN onlyreasonable;
//
//		if (ignoreCenterCuts) onlyreasonable = TRUE; else
//			onlyreasonable = FALSE;
//		plist->polylistcount = allnodepolys(ni, plist, NOWINDOWPART, onlyreasonable);
//		for(j = 0; j < plist->polylistcount; j++)
//		{
//			poly = plist->polygons[j];
//			xformpoly(poly, trans);
//			getbbox(poly, &plist->lx[j], &plist->hx[j], &plist->ly[j], &plist->hy[j]);
//		}
//	}

//	void dr_quickgetarcpolys(ARCINST *ai, POLYLIST *plist, XARRAY trans)
//	{
//		REGISTER INTBIG j;
//		REGISTER POLYGON *poly;
//
//		plist->polylistcount = allarcpolys(ai, plist, NOWINDOWPART);
//		for(j = 0; j < plist->polylistcount; j++)
//		{
//			poly = plist->polygons[j];
//			xformpoly(poly, trans);
//			getbbox(poly, &plist->lx[j], &plist->hx[j], &plist->ly[j], &plist->hy[j]);
//		}
//	}

	private static Technology dr_curtech = null;
	private static boolean [] dr_layersvalid;

	/**
	 * Method to determine which layers in a Technology are valid.
	 */
	private static void cacheValidLayers(Technology tech)
	{
		if (tech == null) return;
		if (dr_curtech == tech) return;

		// code cannot be called by multiple procesors: uses globals
//		NOT_REENTRANT;

		dr_curtech = tech;

		// determine the layers that are being used
		int numLayers = tech.getNumLayers();
		dr_layersvalid = new boolean[numLayers];
		for(int i=0; i < numLayers; i++)
			dr_layersvalid[i] = false;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			if (np.isNotUsed()) continue;
			Technology.NodeLayer [] layers = np.getLayers();
			for(int i=0; i<layers.length; i++)
			{
				Layer layer = layers[i].getLayer();
				dr_layersvalid[layer.getIndex()] = true;
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
				dr_layersvalid[layer.getIndex()] = true;
			}
		}
	}

	/*
	 * Method to determine the minimum distance between "layer1" and "layer2" in technology
	 * "tech" and library "lib".  If "con" is true, the layers are connected.  Also forces
	 * connectivity for same-implant layers.
	 */
//	INTBIG dr_adjustedmindist(TECHNOLOGY *tech, LIBRARY *lib, INTBIG layer1, INTBIG size1,
//		INTBIG layer2, INTBIG size2, BOOLEAN con, BOOLEAN multi, INTBIG *edge, CHAR **rule)
//	{
//		INTBIG dist, fun;
//
//		// if they are implant on the same layer, they connect
//		if (!con && layer1 == layer2)
//		{
//			fun = layerfunction(tech, layer1) & LFTYPE;
//			// treat all wells as connected
//			if (!con)
//			{
//				if (fun == LFSUBSTRATE || fun == LFWELL || fun == LFIMPLANT) con = TRUE;
//			}
//		}
//
//		// see how close they can get
//		dist = drcmindistance(tech, lib, layer1, size1, layer2, size2, con, multi, edge, rule);
//		return(dist);
//	}

	/*
	 * Method to return the network number for port "pp" on node "ni", given that the node is
	 * in a cell with global index "globalIndex".
	 */
	int dr_quickgetnetnumber(PortProto pp, NodeInst ni, int globalIndex)
	{
		if (pp == null) return -1;

		// see if there is an arc connected
		PortInst pi = ni.findPortInstFromProto(pp);
		JNetwork net = pi.getNetwork();
		Integer [] nets = (Integer [])networkLists.get(net);
		return nets[globalIndex].intValue();
//
//		// generate a new unique network number
//		netnumber = dr_quickcheckunconnetnumber++;
//		if (dr_quickcheckunconnetnumber < dr_quickchecknetnumber)
//			dr_quickcheckunconnetnumber = dr_quickchecknetnumber;
//		return netnumber;
	}

	/***************** LAYER INTERACTIONS ******************/

	private static Technology dr_quicklayerintertech = null;
	private static HashMap dr_quicklayersInNodeTable = null;
	private static HashMap dr_quicklayersInArcTable = null;

	/*
	 * Method to build the internal data structures that tell which layers interact with
	 * which primitive nodes in technology "tech".
	 */
	private static void buildLayerInteractions(Technology tech)
	{
		if (dr_quicklayerintertech == tech) return;

		// code cannot be called by multiple procesors: uses globals
//		NOT_REENTRANT;

		dr_quicklayerintertech = tech;
		int numLayers = tech.getNumLayers();

		// build the node table
		dr_quicklayersInNodeTable = new HashMap();
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			boolean [] layersInNode = new boolean[numLayers];
			for(int i=0; i<numLayers; i++) layersInNode[i] = false;

			Technology.NodeLayer [] layers = np.getLayers();
			for(int i=0; i<layers.length; i++)
			{
				Layer layer = layers[i].getLayer();
				layersInNode[layer.getIndex()] = true;
			}
			dr_quicklayersInNodeTable.put(np, layersInNode);
		}

		// build the arc table
		dr_quicklayersInArcTable = new HashMap();
		for(Iterator it = tech.getArcs(); it.hasNext(); )
		{
			PrimitiveArc ap = (PrimitiveArc)it.next();
			boolean [] layersInArc = new boolean[numLayers];
			for(int i=0; i<numLayers; i++) layersInArc[i] = false;

			Technology.ArcLayer [] layers = ap.getLayers();
			for(int i=0; i<layers.length; i++)
			{
				Layer layer = layers[i].getLayer();
				layersInArc[layer.getIndex()] = true;
			}
			dr_quicklayersInArcTable.put(ap, layersInArc);
		}
	}

	/*
	 * Method to determine whether layer "layer" interacts in any way with a node of type "np".
	 * If not, returns FALSE.
	 */
	boolean dr_quickchecklayerwithnode(int layer, NodeProto np)
	{
		buildLayerInteractions(np.getTechnology());

		// find this node in the table
		boolean [] validLayers = (boolean [])dr_quicklayersInNodeTable.get(np);
		if (validLayers == null) return false;
		return validLayers[layer];
	}

	/*
	 * Method to determine whether layer "layer" interacts in any way with an arc of type "ap".
	 * If not, returns FALSE.
	 */
	boolean dr_quickchecklayerwitharc(int layer, ArcProto ap)
	{
		buildLayerInteractions(ap.getTechnology());

		// find this node in the table
		boolean [] validLayers = (boolean [])dr_quicklayersInArcTable.get(ap);
		if (validLayers == null) return false;
		return validLayers[layer];
	}

	/*
	 * Method to recursively scan cell "cell" (transformed with "trans") searching
	 * for DRC Exclusion nodes.  Each node is added to the global list "dr_quickexclusionlist".
	 */
//	void dr_quickaccumulateexclusion(INTBIG depth, NODEPROTO **celllist, XARRAY *translist)
//	{
//		REGISTER NODEINST *ni;
//		REGISTER NODEPROTO *cell;
//		REGISTER INTBIG i, j, k, newtotal;
//		REGISTER POLYGON *poly;
//		REGISTER DRCEXCLUSION *newlist;
//		XARRAY transr, transt, transtemp;
//
//		cell = celllist[depth-1];
//		for(ni = cell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			if (ni->proto == gen_drcprim)
//			{
//				// create a DRC exclusion for every cell up the hierarchy
//				for(j=0; j<depth; j++)
//				{
//					if (j != depth-1) continue;  // exlusion should only be for the current hierarchy
//
//					// make sure there is a place to put the polygon
//					if (dr_quickexclusioncount >= dr_quickexclusiontotal)
//					{
//						newtotal = dr_quickexclusiontotal * 2;
//						if (newtotal <= dr_quickexclusioncount) newtotal = dr_quickexclusioncount + 10;
//						newlist = (DRCEXCLUSION *)emalloc(newtotal * (sizeof (DRCEXCLUSION)), dr_tool->cluster);
//						if (newlist == 0) break;
//						for(i=0; i<dr_quickexclusiontotal; i++)
//							newlist[i] = dr_quickexclusionlist[i];
//						for(i=dr_quickexclusiontotal; i<newtotal; i++)
//							newlist[i].poly = allocpolygon(4, dr_tool->cluster);
//						if (dr_quickexclusiontotal > 0) efree((CHAR *)dr_quickexclusionlist);
//						dr_quickexclusionlist = newlist;
//						dr_quickexclusiontotal = newtotal;
//					}
//
//					// extract the information about this DRC exclusion node
//					poly = dr_quickexclusionlist[dr_quickexclusioncount].poly;
//					dr_quickexclusionlist[dr_quickexclusioncount].cell = celllist[j];
//					(void)nodepolys(ni, 0, NOWINDOWPART);
//					shapenodepoly(ni, 0, poly);
//					makerot(ni, transr);
//					xformpoly(poly, transr);
//					for(k=depth-1; k>j; k--)
//						xformpoly(poly, translist[k]);
//					getbbox(poly, &dr_quickexclusionlist[dr_quickexclusioncount].lx,
//						&dr_quickexclusionlist[dr_quickexclusioncount].hx,
//						&dr_quickexclusionlist[dr_quickexclusioncount].ly,
//						&dr_quickexclusionlist[dr_quickexclusioncount].hy);
//
//					// see if it is already in the list
//					for(i=0; i<dr_quickexclusioncount; i++)
//					{
//						if (dr_quickexclusionlist[i].cell != celllist[j]) continue;
//						if (dr_quickexclusionlist[i].lx != dr_quickexclusionlist[dr_quickexclusioncount].lx ||
//							dr_quickexclusionlist[i].hx != dr_quickexclusionlist[dr_quickexclusioncount].hx ||
//							dr_quickexclusionlist[i].ly != dr_quickexclusionlist[dr_quickexclusioncount].ly ||
//							dr_quickexclusionlist[i].hy != dr_quickexclusionlist[dr_quickexclusioncount].hy)
//								continue;
//						break;
//					}
//					if (i >= dr_quickexclusioncount)
//						dr_quickexclusioncount++;
//				}
//				continue;
//			}
//
//			if (ni->proto->primindex == 0)
//			{
//				// examine contents
//				maketrans(ni, transt);
//				makerot(ni, transr);
//				transmult(transt, transr, transtemp);
//
//				if (depth >= MAXHIERARCHYDEPTH)
//				{
//					if (!dr_quickexclusionwarned)
//						ttyputerr(_("Depth of circuit exceeds %ld: unable to locate all DRC exclusion areas"),
//							MAXHIERARCHYDEPTH);
//					dr_quickexclusionwarned = TRUE;
//					continue;
//				}
//				celllist[depth] = ni->proto;
//				transcpy(transtemp, translist[depth]);
//				dr_quickaccumulateexclusion(depth+1, celllist, translist);
//			}
//		}
//	}

	/*********************************** QUICK DRC ERROR REPORTING ***********************************/

	/* Adds details about an error to the error list */
	private static void reportError(int errtype, Technology tech, String msg,
		Cell cell, int limit, int actual, String rule,
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
//
//		// if this error is in an ignored area, don't record it
//		if (dr_quickexclusioncount > 0)
//		{
//			// determine the bounding box of the error
//			getbbox(poly1, &lx, &hx, &ly, &hy);
//			if (poly2 != NOPOLYGON)
//			{
//				getbbox(poly2, &lx2, &hx2, &ly2, &hy2);
//				if (lx2 < lx) lx = lx2;
//				if (hx2 > hx) hx = hx2;
//				if (ly2 < ly) ly = ly2;
//				if (hy2 > hy) hy = hy2;
//			}
//			for(i=0; i<dr_quickexclusioncount; i++)
//			{
//				if (cell != dr_quickexclusionlist[i].cell) continue;
//				poly = dr_quickexclusionlist[i].poly;
//				if (isinside(lx, ly, poly) && isinside(lx, hy, poly) &&
//					isinside(hx, ly, poly) && isinside(hx, hy, poly)) return;
//			}
//		}

		// describe the error
		String errorMessage = "";
		Cell np1 = geom1.getParent();
		int sortLayer = 0;
		if (errtype == SPACINGERROR || errtype == NOTCHERROR)
		{
			// describe spacing width error
			if (errtype == SPACINGERROR) errorMessage += "Spacing"; else
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
			switch (errtype)
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
			if (errtype == MINWIDTHERROR)
			{
				errorMessage += ", layer " + layer1.getName();
				errorMessage += " LESS THAN " + limit + " WIDE (IS " + actual + ")";
			} else if (errtype == MINSIZEERROR)
			{
				errorMessage += " LESS THAN " + limit + " IN SIZE (IS " + actual + ")";
			} else if (errtype == LAYERSURROUNDERROR)
			{
				errorMessage += ", layer %" + layer1.getName();
				errorMessage += " NEEDS SURROUND OF LAYER " + layer2.getName() + " BY " + limit;
			}
			sortLayer = layer1.getIndex();
		}
		if (rule != null) errorMessage += " [rule " + rule + "]";
//		if (dr_logerrors)
		{
			ErrorLog err = ErrorLog.logError(errorMessage, cell, sortLayer);
			boolean showGeom = true;
			if (poly1 != null) { showGeom = false;   err.addPoly(poly1); }
			if (poly2 != null) { showGeom = false;   err.addPoly(poly2); }
			err.addGeom(geom1, showGeom, 0, null);
			if (geom2 != null) err.addGeom(geom2, showGeom, 0, null);
//		} else
//		{
			System.out.println(errorMessage);
		}
	}

}

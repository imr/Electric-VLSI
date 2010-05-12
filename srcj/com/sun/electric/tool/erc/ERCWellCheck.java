/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ERCWellCheck.java
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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.GenMath.MutableBoolean;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.database.topology.RTNode;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Technology.NodeLayer;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.dialogs.EModelessDialog;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.dialogs.EModelessDialog;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;

/**
 * This is the Electrical Rule Checker tool.
 * 
 * @author Steve Rubin, Gilda Garreton
 */
public class ERCWellCheck {
	private Cell cell;
	private Set<Object> possiblePrimitives;
	private List<WellCon> wellCons = new ArrayList<WellCon>();
	private Iterator<WellCon>[] wellConIterator;
	private List<WellCon>[] wellConLists;
	private RTNode pWellRoot, nWellRoot;
	private Layer pWellLayer, nWellLayer;
	private ErrorLogger errorLogger;
	private WellCheckJob job;
	private double worstPWellDist;
	private Point2D worstPWellCon;
	private Point2D worstPWellEdge;
	private double worstNWellDist;
	private Point2D worstNWellCon;
	private Point2D worstNWellEdge;
	private WellCheckPreferences wellPrefs;
	private List<Transistor> transistors;
	private Set<Integer> networkExportAvailable;
	private Netlist netList;
	
	// TODO [felix] remove this flag
	private boolean useFelixCode = false;

	private static final boolean GATHERSTATISTICS = false;
	private static final boolean DISTANTSEEDS = true;
	private static final boolean INCREMENTALGROWTH = false;

	public static class WellCheckPreferences extends PrefPackage {
		private static final String PREF_NODE = "tool/erc";

		/**
		 * Whether ERC should do well analysis using multiple processors. The
		 * default is "true".
		 */
		@BooleanPref(node = PREF_NODE, key = "ParallelWellAnalysis", factory = true)
		public boolean parallelWellAnalysis;

		/**
		 * The number of processors to use in ERC well analysis. The default is
		 * "0" (as many as there are).
		 */
		@IntegerPref(node = PREF_NODE, key = "WellAnalysisNumProc", factory = 0)
		public int maxProc;

		/**
		 * Whether ERC should check that all P-Well contacts connect to ground.
		 * The default is "true".
		 */
		@BooleanPref(node = PREF_NODE, key = "MustConnectPWellToGround", factory = true)
		public boolean mustConnectPWellToGround;

		/**
		 * Whether ERC should check that all N-Well contacts connect to power.
		 * The default is "true".
		 */
		@BooleanPref(node = PREF_NODE, key = "MustConnectNWellToPower", factory = true)
		public boolean mustConnectNWellToPower;

		/**
		 * How much P-Well contact checking the ERC should do. The values are:
		 * <UL>
		 * <LI>0: must have a contact in every well area.</LI>
		 * <LI>1: must have at least one contact.</LI>
		 * <LI>2: do not check for contact presence.</LI>
		 * </UL>
		 * The default is "0".
		 */
		@IntegerPref(node = PREF_NODE, key = "PWellCheck", factory = 0)
		public int pWellCheck;

		/**
		 * How much N-Well contact checking the ERC should do. The values are:
		 * <UL>
		 * <LI>0: must have a contact in every well area.</LI>
		 * <LI>1: must have at least one contact.</LI>
		 * <LI>2: do not check for contact presence.</LI>
		 * </UL>
		 * The default is "0".
		 */
		@IntegerPref(node = PREF_NODE, key = "NWellCheck", factory = 0)
		public int nWellCheck;

		/**
		 * Whether ERC should check DRC Spacing condition. The default is
		 * "false".
		 */
		@BooleanPref(node = PREF_NODE, key = "DRCCheckInERC", factory = false)
		public boolean drcCheck;

		/**
		 * Whether ERC should find the contact that is farthest from the well
		 * edge. The default is "false".
		 */
		@BooleanPref(node = PREF_NODE, key = "FindWorstCaseWell", factory = false)
		public boolean findWorstCaseWell;

		public WellCheckPreferences(boolean factory) {
			super(factory);
		}
	}

	/**
	 * Method to analyze the current Cell for well errors.
	 */
	public static void analyzeCurCell(GeometryHandler.GHMode newAlgorithm) {
		UserInterface ui = Job.getUserInterface();
		Cell curCell = ui.needCurrentCell();
		if (curCell == null)
			return;

		View view = curCell.getView();
		if (view.isTextView() || view == View.SCHEMATIC || view == View.ICON) {
			System.out.println("Sorry, Well checking runs only on layout cells");
			return;
		}
		new WellCheckJob(curCell, newAlgorithm, new WellCheckPreferences(false));
	}

	/**
	 * Method used by the regressions.
	 * 
	 * @param cell
	 *            the Cell to well-check.
	 * @param newAlgorithm
	 *            the geometry algorithm to use.
	 * @return the success of running well-check.
	 */
	public static int checkERCWell(Cell cell, GeometryHandler.GHMode newAlgorithm,
			WellCheckPreferences wellPrefs) {
		ERCWellCheck check = new ERCWellCheck(cell, null, newAlgorithm, wellPrefs);
		return check.runNow();
	}

	private ERCWellCheck(Cell cell, WellCheckJob job, GeometryHandler.GHMode newAlgorithm,
			WellCheckPreferences wellPrefs) {
		this.job = job;
		this.mode = newAlgorithm;
		this.cell = cell;
		this.wellPrefs = wellPrefs;
		this.transistors = new ArrayList<Transistor>(1000000);
	}

	private static class WellCheckJob extends Job {
		private Cell cell;
		private GeometryHandler.GHMode newAlgorithm;
		private double worstPWellDist, worstNWellDist;
		private EPoint worstPWellCon, worstPWellEdge;
		private EPoint worstNWellCon, worstNWellEdge;
		private WellCheckPreferences wellPrefs;

		private WellCheckJob(Cell cell, GeometryHandler.GHMode newAlgorithm, WellCheckPreferences wellPrefs) {
			super("ERC Well Check on " + cell, ERC.tool, Job.Type.SERVER_EXAMINE, null, null,
					Job.Priority.USER);
			this.cell = cell;
			this.newAlgorithm = newAlgorithm;
			this.wellPrefs = wellPrefs;
			startJob();
		}

		public boolean doIt() throws JobException {
			ERCWellCheck check = new ERCWellCheck(cell, this, newAlgorithm, wellPrefs);
			check.runNow();
			worstPWellDist = check.worstPWellDist;
			fieldVariableChanged("worstPWellDist");
			worstNWellDist = check.worstNWellDist;
			fieldVariableChanged("worstNWellDist");
			if (check.worstPWellCon != null) {
				worstPWellCon = new EPoint(check.worstPWellCon.getX(), check.worstPWellCon.getY());
				fieldVariableChanged("worstPWellCon");
			}
			if (check.worstPWellEdge != null) {
				worstPWellEdge = new EPoint(check.worstPWellEdge.getX(), check.worstPWellEdge.getY());
				fieldVariableChanged("worstPWellEdge");
			}
			if (check.worstNWellCon != null) {
				worstNWellCon = new EPoint(check.worstNWellCon.getX(), check.worstNWellCon.getY());
				fieldVariableChanged("worstNWellCon");
			}
			if (check.worstNWellEdge != null) {
				worstNWellEdge = new EPoint(check.worstNWellEdge.getX(), check.worstNWellEdge.getY());
				fieldVariableChanged("worstNWellEdge");
			}
			return true;
		}

		public void terminateOK() {
			// show the farthest distance from a well contact
			UserInterface ui = Job.getUserInterface();
			EditWindow_ wnd = ui.getCurrentEditWindow_();
			if (wnd != null && (worstPWellDist > 0 || worstNWellDist > 0)) {
				wnd.clearHighlighting();
				if (worstPWellDist > 0) {
					wnd.addHighlightLine(worstPWellCon, worstPWellEdge, cell, false, false);
					System.out.println("Farthest distance from a P-Well contact is " + worstPWellDist);
				}
				if (worstNWellDist > 0) {
					wnd.addHighlightLine(worstNWellCon, worstNWellEdge, cell, false, false);
					System.out.println("Farthest distance from an N-Well contact is " + worstNWellDist);
				}
				wnd.finishedHighlighting();
			}
		}
	}

	// XXX [felix] Why to iterate over all technologies? why do I need
	private int runNow() {
		System.out.println("Checking Wells and Substrates in '" + cell.libDescribe() + "' ...");
		long startTime = System.currentTimeMillis();
		errorLogger = ErrorLogger.newInstance("ERC Well Check ");
		initStatistics();

		// make a list of primtivies that need to be examined
		possiblePrimitives = new HashSet<Object>();
		for (Iterator<Technology> it = Technology.getTechnologies(); it.hasNext();) {
			Technology tech = it.next();
			for (Iterator<PrimitiveNode> pIt = tech.getNodes(); pIt.hasNext();) {
				PrimitiveNode pn = pIt.next();
				NodeLayer[] nl = pn.getNodeLayers();
				for (int i = 0; i < nl.length; i++) {
					Layer lay = nl[i].getLayer();
					if (lay.getFunction().isSubstrate()) {
						possiblePrimitives.add(pn);
						break;
					}
				}
			}
			for (Iterator<ArcProto> pIt = tech.getArcs(); pIt.hasNext();) {
				ArcProto ap = pIt.next();
				for (int i = 0; i < ap.getNumArcLayers(); i++) {
					Layer lay = ap.getLayer(i);
					if (lay.getFunction().isSubstrate()) {
						if (lay.getFunction().isWell())
							pWellLayer = lay;
						else
							nWellLayer = lay;
						possiblePrimitives.add(ap);
						break;
					}
				}
			}
		}
		// int errorCount = doOldWay();
		int errorCount = doNewWay();
		showStatistics();

		// report the number of errors found
		long endTime = System.currentTimeMillis();
		if (errorCount == 0) {
			System.out.println("No Well errors found (took " + TextUtils.getElapsedTime(endTime - startTime)
					+ ")");
		} else {
			System.out.println("FOUND " + errorCount + " WELL ERRORS (took "
					+ TextUtils.getElapsedTime(endTime - startTime) + ")");

		}
		return errorCount;
	}

	// XXX [felix] ugly style use concurrent data structure
	private WellCon getNextWellCon(int threadIndex) {
		synchronized (wellConIterator[threadIndex]) {
			while (wellConIterator[threadIndex].hasNext()) {
				WellCon wc = wellConIterator[threadIndex].next();
				if (wc.wellNum == null)
					return wc;
			}
		}

		// not found in this list: try the others
		int numLists = wellConIterator.length;
		for (int i = 1; i < numLists; i++) {
			int otherList = (threadIndex + i) % numLists;
			synchronized (wellConIterator[otherList]) {
				while (wellConIterator[otherList].hasNext()) {
					WellCon wc = wellConIterator[otherList].next();
					if (wc.wellNum == null)
						return wc;
				}
			}
		}
		return null;
	}

	private void spreadSeeds(int threadIndex) {
		for (;;) {
			WellCon wc = getNextWellCon(threadIndex);
			if (wc == null)
				break;

			// see if this contact is in a marked well
			Rectangle2D searchArea = new Rectangle2D.Double(wc.ctr.getX(), wc.ctr.getY(), 0, 0);
			RTNode topSearch = nWellRoot;
			if (canBeSubstrateTap(wc.fun))
				topSearch = pWellRoot;
			boolean geomFound = false;
			for (RTNode.Search sea = new RTNode.Search(searchArea, topSearch, true); sea.hasNext();) {
				WellBound wb = (WellBound) sea.next();
				geomFound = true;
				wc.wellNum = wb.netID;
				if (wc.wellNum != null)
					break;
			}

			if (wc.wellNum != null)
				continue;

			// mark it and spread the value
			wc.wellNum = new NetValues();

			// if nothing to spread, give an error
			if (!geomFound) {
				String errorMsg = "N-Well contact is floating";
				if (canBeSubstrateTap(wc.fun))
					errorMsg = "P-Well contact is floating";
				errorLogger.logError(errorMsg, new EPoint(wc.ctr.getX(), wc.ctr.getY()), cell, 0);
				continue;
			}

			// spread out through the well area
			spreadWellSeed(wc.ctr.getX(), wc.ctr.getY(), wc.wellNum, topSearch, threadIndex);
		}
	}

	private class SpreadInThread extends Thread {
		private Semaphore whenDone;
		private int threadIndex;

		public SpreadInThread(String name, Semaphore whenDone, int index) {
			super(name);
			this.whenDone = whenDone;
			threadIndex = index;
			start();
		}

		public void run() {
			spreadSeeds(threadIndex);
			whenDone.release();
		}
	}

	// XXX [felix] define each analyzing step in each own module and use a
	// strategy pattern
	// XXX [felix] do analyzing parallel (task parallel) - easy to parallize
	// after rework
	private int doNewWay() {
		pWellRoot = RTNode.makeTopLevel();
		nWellRoot = RTNode.makeTopLevel();

		// enumerate the hierarchy below here
		long startTime = System.currentTimeMillis();
		NewWellCheckVisitor wcVisitor = new NewWellCheckVisitor();
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, wcVisitor);
		int numPRects = getTreeSize(pWellRoot);
		int numNRects = getTreeSize(nWellRoot);
		long endTime = System.currentTimeMillis();
		System.out.println("   Geometry collection found " + (numPRects + numNRects) + " well pieces, took "
				+ TextUtils.getElapsedTime(endTime - startTime));
		startTime = endTime;

		// determine the number of threads to use
		// XXX [felix] not here in the algorithm try to determine this in the
		// preferences
		int numberOfThreads = 1;
		if (wellPrefs.parallelWellAnalysis)
			numberOfThreads = Runtime.getRuntime().availableProcessors();
		if (numberOfThreads > 1) {
			int maxProc = wellPrefs.maxProc;
			if (maxProc > 0)
				numberOfThreads = maxProc;
		}

		// make arrays of well contacts clustdered for each processor
		assignWellContacts(numberOfThreads);

		// analyze the contacts
		NetValues.reset();
		if (numberOfThreads <= 1)
			spreadSeeds(0);
		else {
			// XXX [felix] do not use semaphores here, the Thread object
			// provides you with an join method.
			Semaphore outSem = new Semaphore(0);
			for (int i = 0; i < numberOfThreads; i++)
				new SpreadInThread("WellCheck propagate #" + (i + 1), outSem, i);

			// now wait for spread threads to finish
			outSem.acquireUninterruptibly(numberOfThreads);
		}

		endTime = System.currentTimeMillis();
		String msg = "   Geometry analysis ";
		if (numberOfThreads > 1)
			msg += "used " + numberOfThreads + " threads and ";
		msg += "took ";
		System.out.println(msg + TextUtils.getElapsedTime(endTime - startTime));
		startTime = endTime;

		// xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
		// look for short-circuits
		Map<Integer, WellCon> wellContacts = new HashMap<Integer, WellCon>();
		Map<Integer, Set<Integer>> wellShorts = new HashMap<Integer, Set<Integer>>();
		for (WellCon wc : wellCons) {
			Integer wellIndex = new Integer(wc.wellNum.getIndex());
			WellCon other = wellContacts.get(wellIndex);
			if (other == null)
				wellContacts.put(wellIndex, wc);
			else {
				if (wc.netNum != other.netNum && wc.ni != other.ni) {
					Integer wcNetNum = new Integer(wc.netNum);
					Set<Integer> shortsInWC = wellShorts.get(wcNetNum);
					if (shortsInWC == null) {
						shortsInWC = new HashSet<Integer>();
						wellShorts.put(wcNetNum, shortsInWC);
					}

					Integer otherNetNum = new Integer(other.netNum);
					Set<Integer> shortsInOther = wellShorts.get(otherNetNum);
					if (shortsInOther == null) {
						shortsInOther = new HashSet<Integer>();
						wellShorts.put(otherNetNum, shortsInOther);
					}

					// give error if not seen before
					if (!shortsInWC.contains(otherNetNum)) {
						List<EPoint> pointList = new ArrayList<EPoint>();
						pointList.add(new EPoint(wc.ctr.getX(), wc.ctr.getY()));
						pointList.add(new EPoint(other.ctr.getX(), other.ctr.getY()));
						errorLogger.logMessage("Short circuit between well contacts", pointList, cell, 0,
								true);
						shortsInWC.add(otherNetNum);
						shortsInOther.add(wcNetNum);
					}
				}
			}
		}

		// xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

		// xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
		// more analysis
		Set<Set<Integer>> paths = new HashSet<Set<Integer>>();

		while (transistors.size() > 0 && useFelixCode) {
			Transistor startTrans = transistors.get(0);
			Set<Integer> startPath = new HashSet<Integer>();
			if (createTransistorChain(startPath, startTrans, false)) {
				if (startPath.size() > 0) {
					paths.add(startPath);

//					for (Integer p : startPath) {
//						System.out.print(p + "; ");
//					}
//					System.out.println();
				}
			}
		}

		boolean hasPCon = false, hasNCon = false;
		for (WellCon wc : wellCons) {
			if (canBeSubstrateTap(wc.fun))
				hasPCon = true;
			else
				hasNCon = true;

			if (!wc.onRail && useFelixCode) {
				if (networkExportAvailable.contains(wc.netNum))
					wc.onRail = true;
				else
					for (Set<Integer> path : paths)
						if (path.contains(wc.netNum))
							wc.onRail = true;
			}

			if (!(wc.onRail || wc.onProperRail)) {
				if (canBeSubstrateTap(wc.fun)) {
					if (wellPrefs.mustConnectPWellToGround) {
						errorLogger.logError("P-Well contact '" + wc.ni.getName()
								+ "' not connected to ground", new EPoint(wc.ctr.getX(), wc.ctr.getY()),
								cell, 0);
					}
				} else {
					if (wellPrefs.mustConnectNWellToPower) {
						errorLogger.logError("N-Well contact '" + wc.ni.getName()
								+ "' not connected to power", new EPoint(wc.ctr.getX(), wc.ctr.getY()), cell,
								0);
					}
				}
			}
		}

		// xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

		// xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
		// look for unconnected well areas
		if (wellPrefs.pWellCheck != 2)
			findUnconnected(pWellRoot, pWellRoot, "P");
		if (wellPrefs.nWellCheck != 2)
			findUnconnected(nWellRoot, nWellRoot, "N");
		if (wellPrefs.pWellCheck == 1 && !hasPCon) {
			errorLogger.logError("No P-Well contact found in this cell", cell, 0);
		}
		if (wellPrefs.nWellCheck == 1 && !hasNCon) {
			errorLogger.logError("No N-Well contact found in this cell", cell, 0);
		}
		// xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

		endTime = System.currentTimeMillis();
		System.out.println("   Additional analysis took " + TextUtils.getElapsedTime(endTime - startTime));
		startTime = endTime;

		// make sure the wells are separated properly
		// Emany
		// times
		// THIS IS A DRC JOB .. not efficient if done here.

		// xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
		if (wellPrefs.drcCheck) {
			DRCTemplate pRule = DRC.getSpacingRule(pWellLayer, null, pWellLayer, null, false, -1, 0, 0);
			DRCTemplate nRule = DRC.getSpacingRule(nWellLayer, null, nWellLayer, null, false, -1, 0, 0);
			if (pRule != null)
				findDRCViolations(pWellRoot, pRule.getValue(0));
			if (nRule != null)
				findDRCViolations(nWellRoot, nRule.getValue(0));

			endTime = System.currentTimeMillis();
			System.out.println("   Design rule check took " + TextUtils.getElapsedTime(endTime - startTime));
			startTime = endTime;
		}

		// xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

		// xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

		// compute edge distance if requested
		if (wellPrefs.findWorstCaseWell) {
			worstPWellDist = 0;
			worstPWellCon = null;
			worstPWellEdge = null;
			worstNWellDist = 0;
			worstNWellCon = null;
			worstNWellEdge = null;

			Map<Integer, WellNet> wellNets = new HashMap<Integer, WellNet>();
			for (WellCon wc : wellCons) {
				if (wc.wellNum == null)
					continue;
				Integer netNUM = new Integer(wc.wellNum.getIndex());
				WellNet wn = wellNets.get(netNUM);
				if (wn == null) {
					wn = new WellNet();
					wn.pointsOnNet = new ArrayList<Point2D>();
					wn.contactsOnNet = new ArrayList<WellCon>();
					wn.fun = wc.fun;
					wellNets.put(netNUM, wn);
				}
				wn.contactsOnNet.add(wc);
			}

			findWellNetPoints(pWellRoot, wellNets);
			findWellNetPoints(nWellRoot, wellNets);

			for (Integer netNUM : wellNets.keySet()) {
				WellNet wn = wellNets.get(netNUM);
				for (Point2D pt : wn.pointsOnNet) {
					// find contact closest to this point
					double closestDist = Double.MAX_VALUE;
					Point2D closestCon = null;
					for (WellCon wc : wn.contactsOnNet) {
						double dist = wc.ctr.distance(pt);
						if (dist < closestDist) {
							closestDist = dist;
							closestCon = wc.ctr;
						}
					}

					// see if this distance is worst for the well type
					if (canBeSubstrateTap(wn.fun)) {
						// pWell
						if (closestDist > worstPWellDist) {
							worstPWellDist = closestDist;
							worstPWellCon = closestCon;
							worstPWellEdge = pt;
						}
					} else {
						// nWell
						if (closestDist > worstNWellDist) {
							worstNWellDist = closestDist;
							worstNWellCon = closestCon;
							worstNWellEdge = pt;
						}
					}
				}
			}
			endTime = System.currentTimeMillis();
			System.out.println("   Worst-case distance analysis took "
					+ TextUtils.getElapsedTime(endTime - startTime));
			startTime = endTime;
		}
		// xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

		errorLogger.termLogging(true);
		int errorCount = errorLogger.getNumErrors();
		return errorCount;
	}

	private boolean createTransistorChain(Set<Integer> path, Transistor node, boolean result) {

		result |= createTransistorRec(path, node, result, node.drainNet.get());
		result |= createTransistorRec(path, node, result, node.sourceNet.get());

		return result;

	}

	private boolean createTransistorRec(Set<Integer> path, Transistor node, boolean result, int num) {

		Transistor transis = node;
		Transistor transis_old = null;
		while (transis != null) {
			transis_old = transis;
			transistors.remove(transis);

			Integer neighbor = null;

			if (transis.drainNet.get() == num) {
				neighbor = transis.sourceNet.get();
			} else {
				neighbor = transis.drainNet.get();
			}

			path.add(neighbor);
			num = neighbor;

			result |= networkExportAvailable.contains(neighbor);

			transis = null;
			for (Transistor trans : transistors) {
				if (!trans.equals(transis_old)
						&& (trans.sourceNet.get() == neighbor || trans.drainNet.get() == neighbor)) {
					transis = trans;
					break;
				}
			}
		}
		return result;
	}

	// XXX [felix] Use Concurrent Lists instead of ArrayList for task pools -
	// comment above: creates ugly style
	// XXX [felix] Use ArrayLists instead of arrays
	// XXX [felix] why creating the iterators here?
	private void assignWellContacts(int numberOfThreads) {
		wellConIterator = new Iterator[numberOfThreads];
		wellConLists = new List[numberOfThreads];

		// load the lists
		if (numberOfThreads == 1) {
			wellConLists[0] = wellCons;
		} else {
			for (int i = 0; i < numberOfThreads; i++) {
				wellConLists[i] = new ArrayList<WellCon>();
			}
			if (DISTANTSEEDS) {
				// the new way: cluster the well contacts together
				Rectangle2D cellBounds = cell.getBounds();
				Point2D ctr = new Point2D.Double(cellBounds.getCenterX(), cellBounds.getCenterY());
				Point2D[] farPoints = new Point2D[numberOfThreads];
				for (int i = 0; i < numberOfThreads; i++) {
					double farthest = 0;
					farPoints[i] = new Point2D.Double(0, 0);
					for (WellCon wc : wellCons) {
						double dist = 0;
						if (i == 0) // XXX [felix] distance from the center of
							// the cell to the center of WellCon
							dist += wc.ctr.distance(ctr);
						else {
							for (int j = 0; j < i; j++)
								dist += wc.ctr.distance(farPoints[j]);
						}
						if (dist > farthest) {
							farthest = dist;
							farPoints[i].setLocation(wc.ctr);
						}
					}
				}

				// find the center of the cell
				for (WellCon wc : wellCons) {
					double minDist = Double.MAX_VALUE;
					int threadNum = 0;
					for (int j = 0; j < numberOfThreads; j++) {
						double dist = wc.ctr.distance(farPoints[j]);
						if (dist < minDist) {
							minDist = dist;
							threadNum = j;
						}
					}
					wellConLists[threadNum].add(wc);
				}
				// for(int i=0; i<numberOfThreads; i++)
				// Collections.sort(wellConLists[i], new
				// SortWellCons(farPoints[i]));
			} else {
				// old way where well contacts are analyzed in random order
				for (int i = 0; i < wellCons.size(); i++)
					wellConLists[i % numberOfThreads].add(wellCons.get(i));
			}
		}

		// create iterators over the lists
		for (int i = 0; i < numberOfThreads; i++)
			wellConIterator[i] = wellConLists[i].iterator();
	}

	// /**
	// * Comparator class for sorting Rectangle2D objects by their size.
	// */
	// private static class SortWellCons implements Comparator<WellCon>
	// {
	// private Point2D clusterPt;
	//
	// public SortWellCons(Point2D clusterPt)
	// {
	// super();
	// this.clusterPt = clusterPt;
	// }
	//
	// /**
	// * Method to sort Rectangle2D objects by their size.
	// */
	// public int compare(WellCon wc1, WellCon wc2)
	// {
	// double dist1 = wc1.ctr.distance(clusterPt);
	// double dist2 = wc2.ctr.distance(clusterPt);
	// if (dist1 > dist2) return 1;
	// if (dist1 < dist2) return -1;
	// return 0;
	// }
	// }

	private void findDRCViolations(RTNode rtree, double minDist) {
		for (int j = 0; j < rtree.getTotal(); j++) {
			if (rtree.getFlag()) {
				WellBound child = (WellBound) rtree.getChild(j);
				if (child.netID == null)
					continue;

				// look all around this geometry for others in the well area
				Rectangle2D searchArea = new Rectangle2D.Double(child.bound.getMinX() - minDist, child.bound
						.getMinY()
						- minDist, child.bound.getWidth() + minDist * 2, child.bound.getHeight() + minDist
						* 2);
				for (RTNode.Search sea = new RTNode.Search(searchArea, rtree, true); sea.hasNext();) {
					WellBound other = (WellBound) sea.next();
					if (other.netID.getIndex() <= child.netID.getIndex())
						continue;
					if (child.bound.getMinX() > other.bound.getMaxX() + minDist
							|| other.bound.getMinX() > child.bound.getMaxX() + minDist
							|| child.bound.getMinY() > other.bound.getMaxY() + minDist
							|| other.bound.getMinY() > child.bound.getMaxY() + minDist)
						continue;

					PolyBase pb = new PolyBase(child.bound);
					double trueDist = pb.polyDistance(other.bound);
					if (trueDist < minDist) {
						List<PolyBase> polyList = new ArrayList<PolyBase>();
						polyList.add(new PolyBase(child.bound));
						polyList.add(new PolyBase(other.bound));
						errorLogger.logMessage("Well areas too close (are "
								+ TextUtils.formatDistance(trueDist) + " but should be "
								+ TextUtils.formatDistance(minDist) + " apart)", polyList, cell, 0, true);
					}
				}
			} else {
				RTNode child = (RTNode) rtree.getChild(j);
				findDRCViolations(child, minDist);
			}
		}
	}

	// provide this information in the rtree structure
	private int getTreeSize(RTNode rtree) {
		int total = 0;
		if (rtree.getFlag())
			total += rtree.getTotal(); // FIXME [felix] ugly style of
		// assignment, why doesn't the RTree
		// provide this information
		else {
			for (int j = 0; j < rtree.getTotal(); j++) {
				RTNode child = (RTNode) rtree.getChild(j);
				total += getTreeSize(child);
			}
		}
		return total;
	}

	private void findWellNetPoints(RTNode rtree, Map<Integer, WellNet> wellNets) {
		for (int j = 0; j < rtree.getTotal(); j++) {
			if (rtree.getFlag()) {
				WellBound child = (WellBound) rtree.getChild(j);
				if (child.netID == null)
					continue;
				Integer netNUM = new Integer(child.netID.getIndex());
				WellNet wn = wellNets.get(netNUM);
				if (wn == null)
					continue;
				wn.pointsOnNet.add(new Point2D.Double(child.bound.getMinX(), child.bound.getMinY()));
				wn.pointsOnNet.add(new Point2D.Double(child.bound.getMaxX(), child.bound.getMinY()));
				wn.pointsOnNet.add(new Point2D.Double(child.bound.getMaxX(), child.bound.getMaxY()));
				wn.pointsOnNet.add(new Point2D.Double(child.bound.getMinX(), child.bound.getMaxY()));
			} else {
				RTNode child = (RTNode) rtree.getChild(j);
				findWellNetPoints(child, wellNets);
			}
		}
	}

	// XXX Use enum instead of string for title
	private void findUnconnected(RTNode rtree, RTNode current, String title) {
		for (int j = 0; j < current.getTotal(); j++) {
			if (current.getFlag()) {
				WellBound child = (WellBound) current.getChild(j);
				if (child.netID == null) {
					spreadWellSeed(child.bound.getCenterX(), child.bound.getCenterY(), new NetValues(),
							rtree, 0);
					errorLogger.logError("No " + title + "-Well contact in this area", new EPoint(child.bound
							.getCenterX(), child.bound.getCenterY()), cell, 0);
				}
			} else {
				RTNode child = (RTNode) current.getChild(j);
				findUnconnected(rtree, child, title);
			}
		}
	}

	private void spreadWellSeed(double cX, double cY, NetValues wellNum, RTNode rtree, int threadIndex) {
		RTNode allFound = null;
		Point2D ctr = new Point2D.Double(cX, cY);
		Rectangle2D searchArea = new Rectangle2D.Double(cX, cY, 0, 0);
		// XXX [felix] Use a atomic reference - jdk gives you exactly this what
		// you want
		MutableBoolean keepSearching = new MutableBoolean(true);
		Rectangle2D[] sides = new Rectangle2D[4];
		for (int i = 0; i < 4; i++)
			sides[i] = new Rectangle2D.Double(0, 0, 0, 0);
		int numSides = 1;
		sides[0].setRect(searchArea);
		while (keepSearching.booleanValue()) {
			if (INCREMENTALGROWTH) {
				// grow the search area incrementally
				double lX = sides[0].getMinX(), hX = sides[0].getMaxX();
				double lY = sides[0].getMinY(), hY = sides[0].getMaxY();
				for (int i = 1; i < numSides; i++) {
					lX = Math.min(lX, sides[i].getMinX());
					hX = Math.max(hX, sides[i].getMinX());
					lY = Math.min(lY, sides[i].getMinY());
					hY = Math.max(hY, sides[i].getMinY());
				}
				double newLX = lX, newHX = hX;
				double newLY = lY, newHY = hY;
				boolean anySearchesGood = false;
				for (int i = 0; i < numSides; i++) {
					allFound = searchInArea(sides[i], wellNum, rtree, allFound, ctr, keepSearching,
							threadIndex);
					if (keepSearching.booleanValue())
						anySearchesGood = true;
					newLX = Math.min(newLX, sides[i].getMinX());
					newHX = Math.max(newHX, sides[i].getMinX());
					newLY = Math.min(newLY, sides[i].getMinY());
					newHY = Math.max(newHY, sides[i].getMinY());
				}
				keepSearching.setValue(anySearchesGood);

				// compute new bounds
				numSides = 0;
				if (newLX < lX)
					sides[numSides++].setRect(newLX, newLY, lX - newLX, newHY - newLY);
				if (newHX > hX)
					sides[numSides++].setRect(hX, newLY, newHX - hX, newHY - newLY);
				if (newLY < lY)
					sides[numSides++].setRect(newLX, newLY, newHX - newLX, lY - newLY);
				if (newHY > hY)
					sides[numSides++].setRect(newLX, hY, newHX - newLX, newHY - hY);
			} else {
				// just keep growing the search area
				allFound = searchInArea(searchArea, wellNum, rtree, allFound, ctr, keepSearching, threadIndex);
			}
		}
	}

	private RTNode searchInArea(Rectangle2D searchArea, NetValues wellNum, RTNode rtree, RTNode allFound,
			Point2D ctr, MutableBoolean keepSearching, int threadIndex) {
		keepSearching.setValue(false);
		for (RTNode.Search sea = new RTNode.Search(searchArea, rtree, true); sea.hasNext();) {
			WellBound wb = (WellBound) sea.next();
			if (GATHERSTATISTICS)
				numObjSearches++;
			// ignore if this object is already properly connected
			if (wb.netID != null && wb.netID.getIndex() == wellNum.getIndex())
				continue;

			// see if this object actually touches something in the set
			if (allFound == null) {
				// start from center of contact
				if (!wb.bound.contains(ctr))
					continue;
			} else {
				boolean touches = false;
				for (RTNode.Search subSea = new RTNode.Search(wb.bound, allFound, true); subSea.hasNext();) {
					WellBound subWB = (WellBound) subSea.next();
					if (DBMath.rectsIntersect(subWB.bound, wb.bound)) {
						touches = true;
						break;
					}
				}
				if (!touches)
					continue;
			}

			// this touches what is gathered so far: add to it
			synchronized (wb) {
				if (wb.netID != null)
					wellNum.merge(wb.netID);
				else
					wb.netID = wellNum;
			}
			if (GATHERSTATISTICS)
				wellBoundSearchOrder.add(new WellBoundRecord(wb, threadIndex));
			// expand the next search area by this added bound
			Rectangle2D.union(searchArea, wb.bound, searchArea);
			if (allFound == null)
				allFound = RTNode.makeTopLevel();
			allFound = RTNode.linkGeom(null, allFound, wb);
			keepSearching.setValue(true);
		}
		return allFound;
	}

	// XXX [felix] whats the main difference between index and indexValues
	// XXX [felix] create a static factory method - you could use this method
	// atomically
	// XXX [felix] create such a way also for thread ids - could also be useful
	// on other places
	private static class NetValues {
		private int index;

		private static int indexValues;

		static void reset() {
			indexValues = 0;
		}

		static synchronized int getFreeIndex() {
			return indexValues++;
		}

		public int getIndex() {
			return index;
		}

		public NetValues() {
			index = getFreeIndex();
		}

		public synchronized void merge(NetValues other) {
			if (this.index == other.index)
				return;
			other.index = this.index;
		}
	}

	// well contacts
	private static class WellCon {
		Point2D ctr;
		int netNum;
		NetValues wellNum;
		boolean onProperRail;
		boolean onRail;
		PrimitiveNode.Function fun;
		NodeInst ni;
	}

	private static class Transistor {
		AtomicInteger drainNet = new AtomicInteger();
		AtomicInteger sourceNet = new AtomicInteger();
	}

	/**
	 * Class to define an R-Tree leaf node for geometry in the blockage data
	 * structure.
	 */
	private static class WellBound implements RTBounds {
		private Rectangle2D bound;
		private NetValues netID;

		WellBound(Rectangle2D bound) {
			this.bound = bound;
			this.netID = null;
		}

		public Rectangle2D getBounds() {
			return bound;
		}

		public NetValues getNetID() {
			return netID;
		}

		public String toString() {
			return "Well Bound on net " + netID.getIndex();
		}
	}

	private static class NetRails {
		boolean onGround;
		boolean onPower;
	}

	private static class WellNet {
		List<Point2D> pointsOnNet;
		List<WellCon> contactsOnNet;
		PrimitiveNode.Function fun;
	}

	/**
	 * Comparator class for sorting Rectangle2D objects by their size.
	 */
	private static class RectangleBySize implements Comparator<Rectangle2D> {
		/**
		 * Method to sort Rectangle2D objects by their size.
		 */
		public int compare(Rectangle2D b1, Rectangle2D b2) {
			double s1 = b1.getWidth() * b1.getHeight();
			double s2 = b2.getWidth() * b2.getHeight();
			if (s1 > s2)
				return -1;
			if (s1 < s2)
				return 1;
			return 0;
		}
	}

	private class NewWellCheckVisitor extends HierarchyEnumerator.Visitor {
		private Map<Cell, List<Rectangle2D>> essentialPWell;
		private Map<Cell, List<Rectangle2D>> essentialNWell;
		private Map<Network, NetRails> networkCache;
		private Map<Integer, Transistor> neighborCache;

		public NewWellCheckVisitor() {
			essentialPWell = new HashMap<Cell, List<Rectangle2D>>();
			essentialNWell = new HashMap<Cell, List<Rectangle2D>>();
			networkCache = new HashMap<Network, NetRails>();
			networkExportAvailable = new HashSet<Integer>();
			neighborCache = new HashMap<Integer, Transistor>();
		}

		public boolean enterCell(HierarchyEnumerator.CellInfo info) {
			// Checking if job is scheduled for abort or already aborted
			if (job != null && job.checkAbort())
				return false;

			// make sure essential well areas are gathered
			Cell cell = info.getCell();
			ensureCellCached(cell);
			return true;
		}

		public void exitCell(HierarchyEnumerator.CellInfo info) {
			// Checking if job is scheduled for abort or already aborted
			if (job != null && job.checkAbort())
				return;

			// get the object for merging all of the wells in this cell
			Cell cell = info.getCell();
			List<Rectangle2D> pWellsInCell = essentialPWell.get(cell);
			List<Rectangle2D> nWellsInCell = essentialNWell.get(cell);
			for (Rectangle2D b : pWellsInCell) {
				Rectangle2D bounds = new Rectangle2D.Double(b.getMinX(), b.getMinY(), b.getWidth(), b
						.getHeight());
				DBMath.transformRect(bounds, info.getTransformToRoot());
				pWellRoot = RTNode.linkGeom(null, pWellRoot, new WellBound(bounds));
			}
			for (Rectangle2D b : nWellsInCell) {
				Rectangle2D bounds = new Rectangle2D.Double(b.getMinX(), b.getMinY(), b.getWidth(), b
						.getHeight());
				DBMath.transformRect(bounds, info.getTransformToRoot());
				nWellRoot = RTNode.linkGeom(null, nWellRoot, new WellBound(bounds));
			}
		}

		public void addNetwork(Network net, AtomicInteger netNum, HierarchyEnumerator.CellInfo cinfo) {
			if (net != null) {
				netNum.set(cinfo.getNetID(net));
			}
		}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) {
			NodeInst ni = no.getNodeInst();

			// look for well and substrate contacts
			PrimitiveNode.Function fun = ni.getFunction();
			Netlist netList = info.getNetlist();

			if ((fun.isNTypeTransistor() || fun.isPTypeTransistor()) && useFelixCode) {
				Transistor trans = new Transistor();
				addNetwork(netList.getNetwork(ni.getTransistorDrainPort()), trans.drainNet, info);
				addNetwork(netList.getNetwork(ni.getTransistorSourcePort()), trans.sourceNet, info);
				transistors.add(trans);

			} else if (fun.isResistor() && useFelixCode) {
				Transistor trans = new Transistor();
				int i = 0;
				for (Iterator<PortInst> portit = ni.getPortInsts(); portit.hasNext();) {
					if (i == 0)
						addNetwork(netList.getNetwork(portit.next()), trans.drainNet, info);
					else
						addNetwork(netList.getNetwork(portit.next()), trans.sourceNet, info);
				}
				transistors.add(trans);
			} else if (canBeSubstrateTap(fun) || canBeWellTap(fun)) {
				// Allowing more than one port for well resistors.
				for (Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext();) {
					// PortInst pi = ni.getOnlyPortInst();
					PortInst pi = pIt.next();
					WellCon wc = new WellCon();
					wc.ctr = ni.getTrueCenter();
					AffineTransform trans = ni.rotateOut();
					trans.transform(wc.ctr, wc.ctr);
					info.getTransformToRoot().transform(wc.ctr, wc.ctr);
					wc.fun = fun;
					Network net = netList.getNetwork(pi);
					wc.onProperRail = false;
					wc.onRail = false;
					wc.wellNum = null;
					wc.ni = ni;
					if (net == null)
						wc.netNum = -1;
					else {
						wc.netNum = info.getNetID(net);

						// PWell: must be on ground or NWell: must be on power
						Network parentNet = net;
						HierarchyEnumerator.CellInfo cinfo = info;
						while (cinfo.getParentInst() != null) {
							parentNet = cinfo.getNetworkInParent(parentNet);
							cinfo = cinfo.getParentInfo();
						}
						if (parentNet != null) {
							NetRails nr = networkCache.get(parentNet);
							if (nr == null) {
								nr = new NetRails();
								networkCache.put(parentNet, nr);
								Iterator<Export> it = parentNet.getExports();
								while (it.hasNext()) {
									Export exp = it.next();

									networkExportAvailable.add(parentNet.getNetIndex());

									if (exp.isGround())
										nr.onGround = true;
									if (exp.isPower())
										nr.onPower = true;
								}
							}
							boolean searchWell = (canBeSubstrateTap(fun));
							if ((searchWell && nr.onGround) || (!searchWell && nr.onPower))
								wc.onProperRail = true;
							if (nr.onGround || nr.onPower)
								wc.onRail = true;
						}
					}
					wellCons.add(wc);
				}
			}
			return true;
		}

		private void ensureCellCached(Cell cell) {
			List<Rectangle2D> pWellsInCell = essentialPWell.get(cell);
			List<Rectangle2D> nWellsInCell = essentialNWell.get(cell);
			if (pWellsInCell == null) {
				// gather all wells in the cell
				pWellsInCell = new ArrayList<Rectangle2D>();
				nWellsInCell = new ArrayList<Rectangle2D>();
				for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();) {
					NodeInst ni = it.next();
					if (ni.isCellInstance())
						continue;
					PrimitiveNode pn = (PrimitiveNode) ni.getProto();
					if (!possiblePrimitives.contains(pn))
						continue;

					// Getting only ercLayers
					Poly[] nodeInstPolyList = pn.getTechnology().getShapeOfNode(ni, true, true, ercLayers);
					int tot = nodeInstPolyList.length;
					for (int i = 0; i < tot; i++) {
						Poly poly = nodeInstPolyList[i];
						Layer layer = poly.getLayer();
						AffineTransform trans = ni.rotateOut();
						poly.transform(trans);
						Rectangle2D bound = poly.getBounds2D();
						int wellType = getWellLayerType(layer);
						if (wellType == ERCPWell)
							pWellsInCell.add(bound);
						else if (wellType == ERCNWell)
							nWellsInCell.add(bound);
					}
				}
				for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext();) {
					ArcInst ai = it.next();
					ArcProto ap = ai.getProto();
					if (!possiblePrimitives.contains(ap))
						continue;

					// Getting only ercLayers
					Poly[] arcInstPolyList = ap.getTechnology().getShapeOfArc(ai, ercLayers);
					int tot = arcInstPolyList.length;
					for (int i = 0; i < tot; i++) {
						Poly poly = arcInstPolyList[i];
						Layer layer = poly.getLayer();
						Rectangle2D bound = poly.getBounds2D();
						int wellType = getWellLayerType(layer);
						if (wellType == ERCPWell)
							pWellsInCell.add(bound);
						else if (wellType == ERCNWell)
							nWellsInCell.add(bound);
					}
				}

				// eliminate duplicates
				eliminateDuplicates(pWellsInCell);
				eliminateDuplicates(nWellsInCell);

				// save results
				essentialPWell.put(cell, pWellsInCell);
				essentialNWell.put(cell, nWellsInCell);
			}
		}

		private void eliminateDuplicates(List<Rectangle2D> wbList) {
			// first sort by size
			Collections.sort(wbList, new RectangleBySize());
			for (int i = 0; i < wbList.size(); i++) {
				Rectangle2D b = wbList.get(i);
				for (int j = 0; j < i; j++) {
					Rectangle2D prevB = wbList.get(j);
					if (prevB.contains(b)) {
						wbList.remove(i);
						i--;
						break;
					}
				}
			}
		}
	}

	// XXX [felix] use a enum
	private static final int ERCPWell = 1;
	private static final int ERCNWell = 2;
	private static final int ERCPSEUDO = 0;

	private static final Layer.Function[] ercLayersArray = { Layer.Function.WELLP, Layer.Function.WELL,
			Layer.Function.WELLN, Layer.Function.SUBSTRATE, Layer.Function.IMPLANTP, Layer.Function.IMPLANT,
			Layer.Function.IMPLANTN };
	private static final Layer.Function.Set ercLayers = new Layer.Function.Set(ercLayersArray);

	/**
	 * Method to return nonzero if layer "layer" is a well/select layer.
	 * 
	 * @return 1: P-Well, 2: N-Well
	 */
	private static int getWellLayerType(Layer layer) {
		Layer.Function fun = layer.getFunction();
		if (layer.isPseudoLayer())
			return ERCPSEUDO;
		if (fun == Layer.Function.WELLP)
			return ERCPWell;
		if (fun == Layer.Function.WELL || fun == Layer.Function.WELLN)
			return ERCNWell;
		return ERCPSEUDO;
	}

	// variables for the "old way"
	private GeometryHandler.GHMode mode;
	private Set<Cell> doneCells = new HashSet<Cell>(); // Mark if cells are done
	// already.
	private Map<Cell, GeometryHandler> cellMerges = new HashMap<Cell, GeometryHandler>(); // make

	// a
	// map
	// of
	// merge
	// information
	// in
	// each
	// cell

	// well areas
	private static class WellArea {
		PolyBase poly;
		int netNum;
		int index;
	}

	private int doOldWay() {
		// enumerate the hierarchy below here
		WellCheckVisitor wcVisitor = new WellCheckVisitor();
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, wcVisitor);

		// Checking if job is scheduled for abort or already aborted
		if (job != null && job.checkAbort())
			return 0;

		// make a list of well and substrate areas
		List<WellArea> wellAreas = new ArrayList<WellArea>();
		int wellIndex = 0;
		GeometryHandler topMerge = cellMerges.get(cell);
		for (Layer layer : topMerge.getKeySet()) {
			// Not sure if null goes here
			Collection<PolyBase> set = topMerge.getObjects(layer, false, true);
			for (PolyBase poly : set) {
				WellArea wa = new WellArea();
				wa.poly = poly;
				wa.poly.setLayer(layer);
				wa.poly.setStyle(Poly.Type.FILLED);
				wa.index = wellIndex++;
				wellAreas.add(wa);
			}
		}

		boolean foundPWell = false;
		boolean foundNWell = false;

		for (WellArea wa : wellAreas) {
			int wellType = getWellLayerType(wa.poly.getLayer());
			if (wellType != ERCPWell && wellType != ERCNWell)
				continue;

			// presume N-well
			PrimitiveNode.Function desiredContact = PrimitiveNode.Function.SUBSTRATE;
			String noContactError = "No N-Well contact found in this area";
			int contactAction = wellPrefs.nWellCheck;

			if (wellType == ERCPWell) {
				// P-well
				desiredContact = PrimitiveNode.Function.WELL;
				contactAction = wellPrefs.pWellCheck;
				noContactError = "No P-Well contact found in this area";
				foundPWell = true;
			} else
				foundNWell = true;

			// find a contact in the area
			boolean found = false;
			for (WellCon wc : wellCons) {
				if (wc.fun != desiredContact)
					continue;
				if (!wa.poly.getBounds2D().contains(wc.ctr))
					continue;
				if (!wa.poly.contains(wc.ctr))
					continue;
				wa.netNum = wc.netNum;
				found = true;
				break;
			}

			// if no contact, issue appropriate errors
			if (!found) {
				if (contactAction == 0) {
					errorLogger.logError(noContactError, wa.poly, cell, 0);
				}
			}
		}

		// make sure all of the contacts are on the same net
		for (WellCon wc : wellCons) {
			// -1 means not connected in hierarchyEnumerator::numberNets()
			if (wc.netNum == -1) {
				String errorMsg = "N-Well contact is floating";
				if (canBeSubstrateTap(wc.fun))
					errorMsg = "P-Well contact is floating";
				errorLogger.logError(errorMsg, new EPoint(wc.ctr.getX(), wc.ctr.getY()), cell, 0);
				continue;
			}
			if (!wc.onProperRail) {
				if (canBeSubstrateTap(wc.fun)) {
					if (wellPrefs.mustConnectPWellToGround) {
						errorLogger.logError("P-Well contact not connected to ground", new EPoint(wc.ctr
								.getX(), wc.ctr.getY()), cell, 0);
					}
				} else {
					if (wellPrefs.mustConnectNWellToPower) {
						errorLogger.logError("N-Well contact not connected to power", new EPoint(wc.ctr
								.getX(), wc.ctr.getY()), cell, 0);
					}
				}
			}
		}

		// if just 1 N-Well contact is needed, see if it is there
		if (wellPrefs.nWellCheck == 1 && foundNWell) {
			boolean found = false;
			for (WellCon wc : wellCons) {
				if (canBeWellTap(wc.fun)) {
					found = true;
					break;
				}
			}
			if (!found) {
				errorLogger.logError("No N-Well contact found in this cell", cell, 0);
			}
		}

		// if just 1 P-Well contact is needed, see if it is there
		if (wellPrefs.pWellCheck == 1 && foundPWell) {
			boolean found = false;
			for (WellCon wc : wellCons) {
				if (canBeSubstrateTap(wc.fun)) {
					found = true;
					break;
				}
			}
			if (!found) {
				errorLogger.logError("No P-Well contact found in this cell", cell, 0);
			}
		}

		// make sure the wells are separated properly
		// Local storage of rules.. otherwise getSpacingRule is called too many
		// times
		// THIS IS A DRC JOB .. not efficient if done here.
		if (wellPrefs.drcCheck) {
			Map<Layer, DRCTemplate> rulesCon = new HashMap<Layer, DRCTemplate>();
			Map<Layer, DRCTemplate> rulesNonCon = new HashMap<Layer, DRCTemplate>();

			for (WellArea wa : wellAreas) {
				for (WellArea oWa : wellAreas) {
					// Checking if job is scheduled for abort or already aborted
					if (job != null && job.checkAbort())
						return (0);

					if (wa.index <= oWa.index)
						continue;
					Layer waLayer = wa.poly.getLayer();
					if (waLayer != oWa.poly.getLayer())
						continue;
					boolean con = false;
					if (wa.netNum == oWa.netNum && wa.netNum >= 0)
						con = true;
					DRCTemplate rule = (con) ? rulesCon.get(waLayer) : rulesNonCon.get(waLayer);
					// @TODO Might still return NULL the first time!!. Need
					// another array or aux class?
					if (rule == null) {
						rule = DRC.getSpacingRule(waLayer, null, waLayer, null, con, -1, 0, 0);
						if (rule != null) {
							if (con)
								rulesCon.put(waLayer, rule);
							else
								rulesNonCon.put(waLayer, rule);
						}
					}
					double ruleValue = -1;
					if (rule != null)
						ruleValue = rule.getValue(0);
					if (ruleValue < 0)
						continue;
					if (wa.poly.getBounds2D().getMinX() > oWa.poly.getBounds2D().getMaxX() + ruleValue
							|| oWa.poly.getBounds2D().getMinX() > wa.poly.getBounds2D().getMaxX() + ruleValue
							|| wa.poly.getBounds2D().getMinY() > oWa.poly.getBounds2D().getMaxY() + ruleValue
							|| oWa.poly.getBounds2D().getMinY() > wa.poly.getBounds2D().getMaxY() + ruleValue)
						continue;
					double dist = wa.poly.separation(oWa.poly); // dist == 0 ->
					// intersect or
					// inner loops
					if (dist > 0 && dist < ruleValue) {
						int layertype = getWellLayerType(waLayer);
						if (layertype == ERCPSEUDO)
							continue;

						List<PolyBase> polyList = new ArrayList<PolyBase>();
						polyList.add(wa.poly);
						polyList.add(oWa.poly);
						errorLogger.logMessage(waLayer.getName() + " areas too close (are "
								+ TextUtils.formatDistance(dist) + ", should be "
								+ TextUtils.formatDistance(ruleValue) + ")", polyList, cell, 0, true);
					}
				}
			}
		}

		// compute edge distance if requested
		if (wellPrefs.findWorstCaseWell) {
			worstPWellDist = 0;
			worstPWellCon = null;
			worstPWellEdge = null;
			worstNWellDist = 0;
			worstNWellCon = null;
			worstNWellEdge = null;

			for (WellArea wa : wellAreas) {
				int wellType = getWellLayerType(wa.poly.getLayer());
				if (wellType == ERCPSEUDO)
					continue;
				PrimitiveNode.Function desiredContact = PrimitiveNode.Function.SUBSTRATE;
				if (wellType == ERCPWell)
					desiredContact = PrimitiveNode.Function.WELL;

				// find the worst distance to the edge of the area
				Point2D[] points = wa.poly.getPoints();
				int count = points.length;
				for (int i = 0; i < count * 2; i++) {
					// figure out which point is being analyzed
					Point2D testPoint = null;
					if (i < count) {
						int prev = i - 1;
						if (i == 0)
							prev = count - 1;
						testPoint = new Point2D.Double((points[prev].getX() + points[i].getX()) / 2,
								(points[prev].getY() + points[i].getY()) / 2);
					} else {
						testPoint = points[i - count];
					}

					// find the closest contact to this point
					boolean first = true;
					double bestDist = 0;
					WellCon bestWc = null;
					for (WellCon wc : wellCons) {
						if (wc.fun != desiredContact)
							continue;
						if (!wa.poly.getBounds2D().contains(wc.ctr))
							continue;
						if (!wa.poly.contains(wc.ctr))
							continue;
						double dist = testPoint.distance(wc.ctr);
						if (first || dist < bestDist) {
							bestDist = dist;
							bestWc = wc;
						}
						first = false;
					}
					if (first)
						continue;

					// accumulate worst distances to edges
					if (wellType == 1) {
						if (bestDist > worstPWellDist) {
							worstPWellDist = bestDist;
							worstPWellCon = bestWc.ctr;
							worstPWellEdge = testPoint;
						}
					} else {
						if (bestDist > worstNWellDist) {
							worstNWellDist = bestDist;
							worstNWellCon = bestWc.ctr;
							worstNWellEdge = testPoint;
						}
					}
				}
			}
		}

		// clean up
		wellCons.clear();
		doneCells.clear();
		cellMerges.clear();
		errorLogger.termLogging(true);
		int errorCount = errorLogger.getNumErrors();
		return errorCount;
	}

	private class WellCheckVisitor extends HierarchyEnumerator.Visitor {
		public WellCheckVisitor() {
		}

		public boolean enterCell(HierarchyEnumerator.CellInfo info) {
			// Checking if job is scheduled for abort or already aborted
			if (job != null && job.checkAbort())
				return false;

			// make an object for merging all of the wells in this cell
			Cell cell = info.getCell();
			if (cellMerges.get(cell) == null) {
				GeometryHandler thisMerge = GeometryHandler
						.createGeometryHandler(mode, ercLayersArray.length);
				cellMerges.put(cell, thisMerge);
			}

			return true;
		}

		public void exitCell(HierarchyEnumerator.CellInfo info) {
			// Checking if job is scheduled for abort or already aborted
			if (job != null && job.checkAbort())
				return;

			// get the object for merging all of the wells in this cell
			Cell cell = info.getCell();
			GeometryHandler thisMerge = cellMerges.get(cell);
			if (thisMerge == null)
				throw new Error("wrong condition in ERCWellCheck.enterCell()");

			if (!doneCells.contains(cell)) {
				for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext();) {
					ArcInst ai = it.next();
					Technology tech = ai.getProto().getTechnology();

					// Getting only ercLayers
					Poly[] arcInstPolyList = tech.getShapeOfArc(ai, ercLayers);
					int tot = arcInstPolyList.length;
					for (int i = 0; i < tot; i++) {
						Poly poly = arcInstPolyList[i];
						Layer layer = poly.getLayer();

						// Only interested in well/select
						Shape newElem = poly;
						thisMerge.add(layer, newElem);
					}
				}

				thisMerge.postProcess(true);

				// merge everything sub trees
				for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();) {
					NodeInst ni = it.next();
					if (!ni.isCellInstance())
						continue;

					// get sub-merge information for the cell instance
					GeometryHandler subMerge = cellMerges.get(ni.getProto());
					if (subMerge != null) {
						AffineTransform tTrans = ni.translateOut(ni.rotateOut());
						thisMerge.addAll(subMerge, tTrans);
					}
				}

				// To mark if cell is already done
				doneCells.add(cell);
			}
		}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) {
			NodeInst ni = no.getNodeInst();
			if (NodeInst.isSpecialNode(ni))
				return false; // Nothing to do, Dec 9;

			// merge everything
			AffineTransform trans = null;

			// Not done yet
			Cell cell = info.getCell();
			if (!doneCells.contains(cell)) {
				if (!ni.isCellInstance()) {
					GeometryHandler thisMerge = cellMerges.get(cell);
					PrimitiveNode pNp = (PrimitiveNode) ni.getProto();
					Technology tech = pNp.getTechnology();

					// Getting only ercLayers
					Poly[] nodeInstPolyList = tech.getShapeOfNode(ni, true, true, ercLayers);
					int tot = nodeInstPolyList.length;

					for (int i = 0; i < tot; i++) {
						Poly poly = nodeInstPolyList[i];
						Layer layer = poly.getLayer();

						// Only interested in well/select regions
						if (trans == null)
							trans = ni.rotateOut(); // transformation only
						// calculated when required.
						poly.transform(trans);
						Shape newElem = poly;

						thisMerge.add(layer, newElem);
					}
				}
			}

			// look for well and substrate contacts
			PrimitiveNode.Function fun = ni.getFunction();
			if (canBeSubstrateTap(fun) || canBeWellTap(fun)) {
				WellCon wc = new WellCon();
				wc.ctr = ni.getTrueCenter();
				if (trans == null)
					trans = ni.rotateOut(); // transformation only calculated
				// when required.
				trans.transform(wc.ctr, wc.ctr);
				info.getTransformToRoot().transform(wc.ctr, wc.ctr);
				wc.fun = fun;
				PortInst pi = ni.getOnlyPortInst();
				Netlist netList = info.getNetlist();
				Network net = netList.getNetwork(pi);
				wc.netNum = info.getNetID(net);
				wc.onProperRail = false;
				if (net != null) {
					boolean searchWell = canBeSubstrateTap(fun);

					// PWell: must be on ground or NWell: must be on power
					Network parentNet = net;
					HierarchyEnumerator.CellInfo cinfo = info;
					while (cinfo.getParentInst() != null) {
						parentNet = cinfo.getNetworkInParent(parentNet);
						cinfo = cinfo.getParentInfo();
					}
					if (parentNet != null) {
						for (Iterator<Export> it = parentNet.getExports(); !wc.onProperRail && it.hasNext();) {
							Export exp = it.next();
							if ((searchWell && exp.isGround()) || (!searchWell && exp.isPower()))
								wc.onProperRail = true;
						}
					}
				}
				wellCons.add(wc);
			}
			return true;
		}
	}

	// **************************************** STATISTICS
	// ****************************************

	private List<WellBoundRecord> wellBoundSearchOrder;
	private int numObjSearches;

	private void initStatistics() {
		if (GATHERSTATISTICS) {
			numObjSearches = 0;
			wellBoundSearchOrder = Collections.synchronizedList(new ArrayList<WellBoundRecord>());
		}
	}

	private void showStatistics() {
		if (GATHERSTATISTICS) {
			System.out.println("SEARCHED " + numObjSearches + " OBJECTS");
			new ShowWellBoundOrder();
		}
	}

	private static class WellBoundRecord {
		WellBound wb;
		int processor;

		private WellBoundRecord(WellBound w, int p) {
			wb = w;
			processor = p;
		}
	}

	private class ShowWellBoundOrder extends EModelessDialog {
		private Timer vcrTimer;
		private long vcrLastAdvance;
		private int wbIndex;
		private int speed;
		private JTextField tf;
		private Highlighter h;
		private Color[] hColors = new Color[] { Color.WHITE, Color.RED, Color.GREEN, Color.BLUE };

		public ShowWellBoundOrder() {
			super(TopLevel.isMDIMode() ? TopLevel.getCurrentJFrame() : null, false);
			initComponents();
			finishInitialization();
			setVisible(true);
			wbIndex = 0;
			EditWindow wnd = EditWindow.getCurrent();
			h = wnd.getHighlighter();
			h.clear();
		}

		private void initComponents() {
			getContentPane().setLayout(new GridBagLayout());
			setTitle("Show ERC Progress");
			setName("");
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent evt) {
					closeDialog();
				}
			});
			GridBagConstraints gridBagConstraints;

			JButton go = new JButton("Go");
			go.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					goNow();
				}
			});
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(go, gridBagConstraints);

			JButton stop = new JButton("Stop");
			stop.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					stopNow();
				}
			});
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(stop, gridBagConstraints);

			JLabel lab = new JLabel("Speed:");
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 1;
			gridBagConstraints.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(lab, gridBagConstraints);

			speed = 1;
			tf = new JTextField(Integer.toString(speed));
			tf.getDocument().addDocumentListener(new BoundsPlayerDocumentListener());
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = 1;
			gridBagConstraints.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(tf, gridBagConstraints);

			pack();
		}

		private void updateSpeed() {
			speed = TextUtils.atoi(tf.getText());
			if (vcrTimer != null)
				vcrTimer.setDelay(speed);
		}

		private void goNow() {
			if (vcrTimer == null) {
				ActionListener taskPerformer = new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						tick();
					}
				};
				vcrTimer = new Timer(speed, taskPerformer);
				vcrLastAdvance = System.currentTimeMillis();
				vcrTimer.start();
			}
		}

		private void stopNow() {
			if (vcrTimer == null)
				return;
			vcrTimer.stop();
			vcrTimer = null;
		}

		private void tick() {
			// see if it is time to advance the VCR
			long curtime = System.currentTimeMillis();
			if (curtime - vcrLastAdvance < speed)
				return;
			vcrLastAdvance = curtime;

			if (wbIndex >= wellBoundSearchOrder.size())
				stopNow();
			else {
				WellBoundRecord wbr = wellBoundSearchOrder.get(wbIndex++);
				h.addPoly(new Poly(wbr.wb.bound), cell, hColors[wbr.processor]);
				h.finished();
			}
		}

		private class BoundsPlayerDocumentListener implements DocumentListener {
			BoundsPlayerDocumentListener() {
			}

			public void changedUpdate(DocumentEvent e) {
				updateSpeed();
			}

			public void insertUpdate(DocumentEvent e) {
				updateSpeed();
			}

			public void removeUpdate(DocumentEvent e) {
				updateSpeed();
			}
		}
	}

	/**
	 * Method to tell whether this function describes an element which can be
	 * used as substrate tap (ERC)
	 * 
	 * @return
	 */
	private static boolean canBeSubstrateTap(PrimitiveNode.Function fun) {
		return fun == PrimitiveNode.Function.SUBSTRATE || fun == PrimitiveNode.Function.RESPWELL;
	}

	/**
	 * Method to tell whether this function describes an element which can be
	 * used as well tap (ERC)
	 * 
	 * @return
	 */
	private static boolean canBeWellTap(PrimitiveNode.Function fun) {
		return fun == PrimitiveNode.Function.WELL || fun == PrimitiveNode.Function.RESNWELL;
	}
}

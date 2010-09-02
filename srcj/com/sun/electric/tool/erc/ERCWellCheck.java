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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.sun.electric.database.geometry.*;
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
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode.Function;
import com.sun.electric.technology.Technology.NodeLayer;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.erc.wellcheck.ConnectionCheck;
import com.sun.electric.tool.erc.wellcheck.DRCCheck;
import com.sun.electric.tool.erc.wellcheck.DistanceCheck;
import com.sun.electric.tool.erc.wellcheck.NetValues;
import com.sun.electric.tool.erc.wellcheck.OnRailCheck;
import com.sun.electric.tool.erc.wellcheck.ShortCircuitCheck;
import com.sun.electric.tool.erc.wellcheck.Utils;
import com.sun.electric.tool.erc.wellcheck.WellCheckAnalysisStrategy;
import com.sun.electric.tool.erc.wellcheck.WellCon;
import com.sun.electric.tool.erc.wellcheck.Utils.WorkDistributionStrategy;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.dialogs.EModelessDialog;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.util.concurrent.Parallel;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PJob;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange1D;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.PForTask;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool;
import com.sun.electric.util.CollectionFactory;

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
	private Map<Integer, List<Transistor>> transistors;
	private Set<Integer> networkExportAvailable;
	private boolean hasPCon;
	private boolean hasNCon;

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

	public enum WellType {
		none("none"), nwell("N"), pwell("P");

		private String name;

		private WellType(String name) {
			this.name = name;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return this.name;
		}
	}

	/**
	 * Method to analyze the current Cell for well errors.
	 */
	@Deprecated
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

	public static void analyzeCurCell() {
		UserInterface ui = Job.getUserInterface();
		Cell curCell = ui.needCurrentCell();
		if (curCell == null)
			return;

		View view = curCell.getView();
		if (view.isTextView() || view == View.SCHEMATIC || view == View.ICON) {
			System.out.println("Sorry, Well checking runs only on layout cells");
			return;
		}
		new WellCheckJob(curCell, new WellCheckPreferences(false));
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
	@Deprecated
	public static int checkERCWell(Cell cell, GeometryHandler.GHMode newAlgorithm,
			WellCheckPreferences wellPrefs) {
		ERCWellCheck check = new ERCWellCheck(cell, null, newAlgorithm, wellPrefs);
		return check.runNow();
	}

	public static int checkERCWell(Cell cell, WellCheckPreferences wellPrefs) {
		ERCWellCheck check = new ERCWellCheck(cell, null, wellPrefs);
		return check.runNow();
	}

	@Deprecated
	private ERCWellCheck(Cell cell, WellCheckJob job, GeometryHandler.GHMode newAlgorithm,
			WellCheckPreferences wellPrefs) {
		this.job = job;
		this.cell = cell;
		this.wellPrefs = wellPrefs;
		this.transistors = new HashMap<Integer, List<Transistor>>();
	}

	private ERCWellCheck(Cell cell, WellCheckJob job, WellCheckPreferences wellPrefs) {
		this.job = job;
		this.cell = cell;
		this.wellPrefs = wellPrefs;
		this.transistors = new HashMap<Integer, List<Transistor>>();
	}

	private static class WellCheckJob extends Job {
		private Cell cell;
		private double worstPWellDist, worstNWellDist;
		private EPoint worstPWellCon, worstPWellEdge;
		private EPoint worstNWellCon, worstNWellEdge;
		private WellCheckPreferences wellPrefs;

		@Deprecated
		private WellCheckJob(Cell cell, GeometryHandler.GHMode newAlgorithm,
				WellCheckPreferences wellPrefs) {
			super("ERC Well Check on " + cell, ERC.tool, Job.Type.SERVER_EXAMINE, null, null,
					Job.Priority.USER);
			this.cell = cell;
			this.wellPrefs = wellPrefs;
			startJob();
		}

		private WellCheckJob(Cell cell, WellCheckPreferences wellPrefs) {
			super("ERC Well Check on " + cell, ERC.tool, Job.Type.SERVER_EXAMINE, null, null,
					Job.Priority.USER);
			this.cell = cell;
			this.wellPrefs = wellPrefs;
			startJob();
		}

		public boolean doIt() throws JobException {
			ERCWellCheck check = new ERCWellCheck(cell, this, wellPrefs);
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
				worstPWellEdge = new EPoint(check.worstPWellEdge.getX(), check.worstPWellEdge
						.getY());
				fieldVariableChanged("worstPWellEdge");
			}
			if (check.worstNWellCon != null) {
				worstNWellCon = new EPoint(check.worstNWellCon.getX(), check.worstNWellCon.getY());
				fieldVariableChanged("worstNWellCon");
			}
			if (check.worstNWellEdge != null) {
				worstNWellEdge = new EPoint(check.worstNWellEdge.getX(), check.worstNWellEdge
						.getY());
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
					System.out.println("Farthest distance from a P-Well contact is "
							+ worstPWellDist);
				}
				if (worstNWellDist > 0) {
					wnd.addHighlightLine(worstNWellCon, worstNWellEdge, cell, false, false);
					System.out.println("Farthest distance from an N-Well contact is "
							+ worstNWellDist);
				}
				wnd.finishedHighlighting();
			}
		}
	}

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
			System.out.println("No Well errors found (took "
					+ TextUtils.getElapsedTime(endTime - startTime) + ")");
		} else {
			System.out.println("FOUND " + errorCount + " WELL ERRORS (took "
					+ TextUtils.getElapsedTime(endTime - startTime) + ")");

		}
		return errorCount;
	}

	private WellCon getNextWellCon(int threadIndex) {
		synchronized (wellConLists[threadIndex]) {
			while (wellConIterator[threadIndex].hasNext()) {
				WellCon wc = wellConIterator[threadIndex].next();
				if (wc.getWellNum() == null)
					return wc;
			}
		}

		// not found in this list: try the others
		int numLists = wellConIterator.length;
		for (int i = 1; i < numLists; i++) {
			int otherList = (threadIndex + i) % numLists;
			synchronized (wellConLists[otherList]) {
				while (wellConIterator[otherList].hasNext()) {
					WellCon wc = wellConIterator[otherList].next();
					if (wc.getWellNum() == null)
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

			// see if this contact is a marked well
			Rectangle2D searchArea = new Rectangle2D.Double(wc.getCtr().getX(), wc.getCtr().getY(),
					0, 0);
			RTNode topSearch = nWellRoot;
			if (Utils.canBeSubstrateTap(wc.getFun()))
				topSearch = pWellRoot;
			boolean geomFound = false;
			for (RTNode.Search sea = new RTNode.Search(searchArea, topSearch, true); sea.hasNext();) {
				WellBound wb = (WellBound) sea.next();
				geomFound = true;
				wc.setWellNum(wb.netID);
				if (wc.getWellNum() != null)
					break;
			}

			if (wc.getWellNum() != null)
				continue;

			// mark it and spread the value
			wc.setWellNum(new NetValues());

			// if nothing to spread, give an error
			if (!geomFound) {
				String errorMsg = "N-Well contact is floating";
				if (Utils.canBeSubstrateTap(wc.getFun()))
					errorMsg = "P-Well contact is floating";
				errorLogger.logError(errorMsg, new EPoint(wc.getCtr().getX(), wc.getCtr().getY()),
						cell, 0);
				continue;
			}

			// spread out through the well area
			Utils.spreadWellSeed(wc.getCtr().getX(), wc.getCtr().getY(), wc.getWellNum(),
					topSearch, threadIndex);
		}
	}

	public class SpreadInThread extends PTask {
		private int threadIndex;

		public SpreadInThread(PJob job, int index) {
			super(job);
			threadIndex = index;
		}

		public void execute() {
			spreadSeeds(threadIndex);
		}
	}

	private int doNewWay() {

		int numberOfThreads = 1;
		if (wellPrefs.parallelWellAnalysis)
			numberOfThreads = Runtime.getRuntime().availableProcessors();
		if (numberOfThreads > 1) {
			int maxProc = wellPrefs.maxProc;
			if (maxProc > 0)
				numberOfThreads = maxProc;
		}

		hasNCon = false;
		hasPCon = false;
		NetValues.numberOfMerges = 0;

		pWellRoot = RTNode.makeTopLevel();
		nWellRoot = RTNode.makeTopLevel();

		// enumerate the hierarchy below here
		long startTime = System.currentTimeMillis();
		WellCheckVisitor wcVisitor = new WellCheckVisitor();
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, wcVisitor);
		int numPRects = getTreeSize(pWellRoot);
		int numNRects = getTreeSize(nWellRoot);
		long endTime = System.currentTimeMillis();
		System.out.println("   Geometry collection found " + (numPRects + numNRects)
				+ " well pieces, took " + TextUtils.getElapsedTime(endTime - startTime));
		startTime = endTime;

		wcVisitor.clear();
		wcVisitor = null;

		try {
			ThreadPool.initialize(numberOfThreads);
		} catch (PoolExistsException e) {
			e.printStackTrace();
		}

		startTime = System.currentTimeMillis();

		// make arrays of well contacts clustdered for each processor
		assignWellContacts(numberOfThreads);

		if (Job.getDebug()) {
			endTime = System.currentTimeMillis();

			System.out.println("   Assign well contacts took: "
					+ TextUtils.getElapsedTime(endTime - startTime));

			startTime = endTime;
		}

		// analyze the contacts
		NetValues.reset();
		if (numberOfThreads <= 1)
			spreadSeeds(0);
		else {
			PJob spreadJob = new PJob();
			for (int i = 0; i < numberOfThreads; i++)
				spreadJob.add(new SpreadInThread(spreadJob, i));
			spreadJob.execute();
		}

		endTime = System.currentTimeMillis();
		String msg = "   Geometry analysis ";
		if (numberOfThreads > 1)
			msg += "used " + numberOfThreads + " threads and ";
		msg += "took ";
		System.out.println(msg + TextUtils.getElapsedTime(endTime - startTime));
		startTime = endTime;

		try {
			ThreadPool.getThreadPool().shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		startTime = endTime;

		if (Job.getDebug()) {
			System.out.println("   Amount of merges: " + NetValues.numberOfMerges);
		}

		StrategyParameter parameter = new StrategyParameter(wellCons, wellPrefs, cell, errorLogger);

		// create analysis steps
		List<WellCheckAnalysisStrategy> analysisParts = CollectionFactory.createArrayList();
		analysisParts.add(new ShortCircuitCheck(parameter));
		analysisParts.add(new OnRailCheck(parameter, networkExportAvailable, transistors));
		analysisParts.add(new ConnectionCheck(parameter, hasPCon, hasNCon, pWellRoot, nWellRoot));
		analysisParts.add(new DRCCheck(parameter, pWellLayer, nWellLayer, pWellRoot, nWellRoot));
		analysisParts
				.add(new DistanceCheck(parameter, worstPWellDist, worstPWellCon, worstPWellEdge,
						worstNWellDist, worstNWellCon, worstNWellEdge, pWellRoot, nWellRoot));

		// execute analysis steps
		for (WellCheckAnalysisStrategy strategy : analysisParts) {
			strategy.execute();
		}

		endTime = System.currentTimeMillis();
		System.out.println("   Additional analysis took "
				+ TextUtils.getElapsedTime(endTime - startTime));
		startTime = endTime;

		errorLogger.termLogging(true);
		int errorCount = errorLogger.getNumErrors();

		return errorCount;
	}

	public static class StrategyParameter {
		private final List<WellCon> wellCons;
		private final WellCheckPreferences wellPrefs;
		private final Cell cell;
		private final ErrorLogger errorLogger;

		/**
		 * @param wellCons
		 * @param wellPrefs
		 * @param cell
		 */
		public StrategyParameter(List<WellCon> wellCons, WellCheckPreferences wellPrefs, Cell cell,
				ErrorLogger errorLogger) {
			super();
			this.wellCons = wellCons;
			this.wellPrefs = wellPrefs;
			this.cell = cell;
			this.errorLogger = errorLogger;
		}

		public List<WellCon> getWellCons() {
			return wellCons;
		}

		public WellCheckPreferences getWellPrefs() {
			return wellPrefs;
		}

		public Cell getCell() {
			return cell;
		}

//		public ErrorLogger getErrorLogger() {
//			return errorLogger;
//		}

        // Apply translation to place the bounding box of the well contact
        // in the correct location on the top cell.
        private Rectangle2D placedWellCon(WellCon wc)
        {
            Rectangle2D orig = wc.getNi().getBounds();
            return new Rectangle2D.Double(wc.getCtr().getX()-orig.getWidth()/2,
                wc.getCtr().getY()-orig.getHeight()/2,
                orig.getWidth(), orig.getHeight());
        }

        public void logError(String message)
        {
        	errorLogger.logError(message, cell, 0);
        }
        
        public void logError(String message, Object... wblist)
        {					
            List<Object> list = new ArrayList<Object>();
        	for (Object w : wblist)
        	{
        		if (w instanceof WellBound)
        		{
        			list.add(((WellBound)w).getBounds());
        		}
        		else if (w instanceof WellCon)
        		{
        			// calculate the correct location of wc bnd with respect to the top cell
        			list.add(placedWellCon(((WellCon)w)));
        		}
        	}
        	errorLogger.logMessage(message, list, cell, 0, true);
        }
    }

	@SuppressWarnings("unchecked")
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
			if (Utils.WORKDISTRIBUTION == WorkDistributionStrategy.cluster) {
				// the new way: cluster the well contacts together
				Rectangle2D cellBounds = cell.getBounds();
				Point2D ctr = new Point2D.Double(cellBounds.getCenterX(), cellBounds.getCenterY());
				Point2D[] farPoints = new Point2D[numberOfThreads];

				for (int i = 0; i < numberOfThreads; i++) {
					double farthest = 0;
					farPoints[i] = new Point2D.Double(0, 0);
					for (WellCon wc : wellCons) {
						double dist = 0;
						if (i == 0)
							dist += wc.getCtr().distance(ctr);
						else {
							for (int j = 0; j < i; j++)
								dist += wc.getCtr().distance(farPoints[j]);
						}
						if (dist > farthest) {
							farthest = dist;
							farPoints[i].setLocation(wc.getCtr());
						}
					}
				}

				// find the center of the cell
				for (WellCon wc : wellCons) {
					double minDist = Double.MAX_VALUE;
					int threadNum = 0;
					for (int j = 0; j < numberOfThreads; j++) {
						double dist = wc.getCtr().distance(farPoints[j]);
						if (dist < minDist) {
							minDist = dist;
							threadNum = j;
						}
					}
					wellConLists[threadNum].add(wc);
				}
			} else if (Utils.WORKDISTRIBUTION == WorkDistributionStrategy.random) {

				for (int i = 0; i < wellCons.size(); i++)
					wellConLists[i % numberOfThreads].add(wellCons.get(i));

			} else if (Utils.WORKDISTRIBUTION == WorkDistributionStrategy.bucket) {

				GridDim dim = calculateGridDim(numberOfThreads);

				double sizeCellX = cell.getDefWidth() / (double) dim.xDim;
				double sizeCellY = cell.getDefHeight() / (double) dim.yDim;

				Parallel.For(new BlockedRange1D(0, wellCons.size(), wellCons.size()
						/ numberOfThreads), new WorkDistributionTask(sizeCellX, sizeCellY, dim));
			}
		}

		// create iterators over the lists
		for (int i = 0; i < numberOfThreads; i++)
			wellConIterator[i] = wellConLists[i].iterator();
	}

	public class WorkDistributionTask extends PForTask {

		private double sizeX;
		private double sizeY;
		private GridDim dim;

		public WorkDistributionTask(double sizeX, double sizeY, GridDim dim) {
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.dim = dim;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sun.electric.tool.util.concurrent.patterns.PForJob.PForTask#execute
		 * (com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange)
		 */
		@Override
		public void execute(BlockedRange range) {
			BlockedRange1D range1D = (BlockedRange1D) range;
			for (int i = range1D.start(); i < range1D.end(); i++) {
				WellCon con = wellCons.get(i);
				GridDim tmpDim = calculateBucket(con, sizeX, sizeY);
				int threadId = tmpDim.xDim + tmpDim.yDim * dim.xDim;
				CollectionFactory.threadSafeListAdd(con, wellConLists[threadId]);
			}
		}

	}

	private GridDim calculateBucket(WellCon con, double sizeX, double sizeY) {
		GridDim result = new GridDim();

		result.xDim = (int) ((con.getCtr().getX() - cell.getBounds().getMinX()) / sizeX);
		result.yDim = (int) ((con.getCtr().getY() - cell.getBounds().getMinY()) / sizeY);

		return result;
	}

	private GridDim calculateGridDim(int numberOfThreads) {
		GridDim result = new GridDim();

		for (int i = (int) Math.sqrt(numberOfThreads); i >= 1; i--) {
			if ((i * (int) (numberOfThreads / i)) == numberOfThreads) {
				result.xDim = i;
				result.yDim = (int) (numberOfThreads / i);
				return result;
			}
		}

		return null;
	}

	private static class GridDim {
		private int xDim;
		private int yDim;
	}

	// provide this information in the rtree structure
	private int getTreeSize(RTNode rtree) {
		int total = 0;
		if (rtree.getFlag())
			total += rtree.getTotal();
		else {
			for (int j = 0; j < rtree.getTotal(); j++) {
				RTNode child = (RTNode) rtree.getChild(j);
				total += getTreeSize(child);
			}
		}
		return total;
	}

	public static class Transistor {
		private AtomicInteger drainNet = new AtomicInteger();
		private AtomicInteger sourceNet = new AtomicInteger();

		public AtomicInteger getDrainNet() {
			return drainNet;
		}

		public void setDrainNet(AtomicInteger drainNet) {
			this.drainNet = drainNet;
		}

		public AtomicInteger getSourceNet() {
			return sourceNet;
		}

		public void setSourceNet(AtomicInteger sourceNet) {
			this.sourceNet = sourceNet;
		}
	}

	/**
	 * Class to define an R-Tree leaf node for geometry in the blockage data
	 * structure.
	 */
	public static class WellBound implements RTBounds {
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

		public void setNetID(NetValues netNum) {
			this.netID = netNum;
		}

		public String toString() {
			return "Well Bound on net " + netID.getIndex();
		}
	}

	private static class NetRails {
		boolean onGround;
		boolean onPower;
		boolean onExport;
	}

	public static class WellNet {
		private List<Point2D> pointsOnNet;
		private List<WellCon> contactsOnNet;
		private PrimitiveNode.Function fun;

		/**
		 * @param pointsOnNet
		 * @param contactsOnNet
		 * @param fun
		 */
		public WellNet(List<Point2D> pointsOnNet, List<WellCon> contactsOnNet, Function fun) {
			super();
			this.pointsOnNet = pointsOnNet;
			this.contactsOnNet = contactsOnNet;
			this.fun = fun;
		}

		public List<Point2D> getPointsOnNet() {
			return pointsOnNet;
		}

		public void setPointsOnNet(List<Point2D> pointsOnNet) {
			this.pointsOnNet = pointsOnNet;
		}

		public List<WellCon> getContactsOnNet() {
			return contactsOnNet;
		}

		public void setContactsOnNet(List<WellCon> contactsOnNet) {
			this.contactsOnNet = contactsOnNet;
		}

		public PrimitiveNode.Function getFun() {
			return fun;
		}

		public void setFun(PrimitiveNode.Function fun) {
			this.fun = fun;
		}
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

	private class WellCheckVisitor extends HierarchyEnumerator.Visitor {
		private Map<Cell, List<Rectangle2D>> essentialPWell;
		private Map<Cell, List<Rectangle2D>> essentialNWell;
		private Map<Network, NetRails> networkCache;
		private Map<Integer, Transistor> neighborCache;

		public WellCheckVisitor() {
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
				Rectangle2D bounds = new Rectangle2D.Double(b.getMinX(), b.getMinY(), b.getWidth(),
						b.getHeight());
				DBMath.transformRect(bounds, info.getTransformToRoot());
				pWellRoot = RTNode.linkGeom(null, pWellRoot, new WellBound(bounds));
			}
			for (Rectangle2D b : nWellsInCell) {
				Rectangle2D bounds = new Rectangle2D.Double(b.getMinX(), b.getMinY(), b.getWidth(),
						b.getHeight());
				DBMath.transformRect(bounds, info.getTransformToRoot());
				nWellRoot = RTNode.linkGeom(null, nWellRoot, new WellBound(bounds));
			}
		}

		private void addNetwork(Network net, AtomicInteger netNum,
				HierarchyEnumerator.CellInfo cinfo, Transistor trans) {
			if (net != null) {
				Integer num = cinfo.getNetID(net);
				netNum.set(num);

				if (!transistors.containsKey(num)) {
					List<Transistor> tmpList = new LinkedList<Transistor>();
					transistors.put(num, tmpList);
				}
				transistors.get(num).add(trans);
			}
		}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) {
			NodeInst ni = no.getNodeInst();

			// look for well and substrate contacts
			PrimitiveNode.Function fun = ni.getFunction();
			Netlist netList = info.getNetlist();

			if ((fun.isNTypeTransistor() || fun.isPTypeTransistor())) {
				Transistor trans = new Transistor();
				addNetwork(netList.getNetwork(ni.getTransistorDrainPort()), trans.drainNet, info,
						trans);
				addNetwork(netList.getNetwork(ni.getTransistorSourcePort()), trans.sourceNet, info,
						trans);
			} else if (Utils.canBeSubstrateTap(fun) || Utils.canBeWellTap(fun)) {
				// Allowing more than one port for well resistors.
				for (Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext();) {
					// PortInst pi = ni.getOnlyPortInst();
					PortInst pi = pIt.next();

					Network net = netList.getNetwork(pi);
					int tmpNetNum = 0;
					if (net == null)
						tmpNetNum = -1;
					else
						tmpNetNum = info.getNetID(net);

					WellCon wc = new WellCon(ni.getTrueCenter(), tmpNetNum, null, false, false,
							fun, ni);
					AffineTransform trans = ni.rotateOut();
					trans.transform(wc.getCtr(), wc.getCtr());
					info.getTransformToRoot().transform(wc.getCtr(), wc.getCtr());

					if (net != null) {
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
									nr.onExport = true;
								}
							}
							boolean searchWell = (Utils.canBeSubstrateTap(fun));

							if (Utils.canBeSubstrateTap(wc.getFun()))
								hasPCon = true;
							else
								hasNCon = true;

							if (searchWell)
								hasPCon = true;
							else
								hasNCon = true;

							if ((searchWell && nr.onGround) || (!searchWell && nr.onPower))
								wc.setOnProperRail(true);
							if (nr.onExport)
								wc.setOnRail(true);
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
					Poly[] nodeInstPolyList = pn.getTechnology().getShapeOfNode(ni, true, true,
							ercLayers);
					int tot = nodeInstPolyList.length;
					for (int i = 0; i < tot; i++) {
						Poly poly = nodeInstPolyList[i];
						Layer layer = poly.getLayer();
						AffineTransform trans = ni.rotateOut();
						poly.transform(trans);
						Rectangle2D bound = poly.getBounds2D();
						WellType wellType = getWellLayerType(layer);
						if (wellType == WellType.pwell)
							pWellsInCell.add(bound);
						else if (wellType == WellType.nwell)
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
						WellType wellType = getWellLayerType(layer);
						if (wellType == WellType.pwell)
							pWellsInCell.add(bound);
						else if (wellType == WellType.nwell)
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

		public void clear() {
			neighborCache.clear();
			networkCache.clear();

			neighborCache = null;
			networkCache = null;
		}
	}

	private static final Layer.Function[] ercLayersArray = { Layer.Function.WELLP,
			Layer.Function.WELL, Layer.Function.WELLN, Layer.Function.SUBSTRATE,
			Layer.Function.IMPLANTP, Layer.Function.IMPLANT, Layer.Function.IMPLANTN };
	private static final Layer.Function.Set ercLayers = new Layer.Function.Set(ercLayersArray);

	/**
	 * Method to return nonzero if layer "layer" is a well/select layer.
	 * 
	 * @return 1: P-Well, 2: N-Well
	 */
	private static WellType getWellLayerType(Layer layer) {
		Layer.Function fun = layer.getFunction();
		if (layer.isPseudoLayer())
			return WellType.none;
		if (fun == Layer.Function.WELLP)
			return WellType.pwell;
		if (fun == Layer.Function.WELL || fun == Layer.Function.WELLN)
			return WellType.nwell;
		return WellType.none;
	}

	// **************************************** STATISTICS
	// ****************************************

	private void initStatistics() {
		if (Utils.GATHERSTATISTICS) {
			Utils.numObjSearches = 0;
			Utils.wellBoundSearchOrder = Collections
					.synchronizedList(new ArrayList<WellBoundRecord>());
		}
	}

	private void showStatistics() {
		if (Utils.GATHERSTATISTICS) {
			System.out.println("SEARCHED " + Utils.numObjSearches + " OBJECTS");
			new ShowWellBoundOrder();
		}
	}

	public static class WellBoundRecord {
		private WellBound wb;
		private int processor;

		public WellBoundRecord(WellBound w, int p) {
			wb = w;
			processor = p;
		}

		public WellBound getWb() {
			return wb;
		}

		public void setWb(WellBound wb) {
			this.wb = wb;
		}

		public int getProcessor() {
			return processor;
		}

		public void setProcessor(int processor) {
			this.processor = processor;
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
			super(TopLevel.isMDIMode() ? TopLevel.getCurrentJFrame() : null);
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

			if (wbIndex >= Utils.wellBoundSearchOrder.size())
				stopNow();
			else {
				WellBoundRecord wbr = Utils.wellBoundSearchOrder.get(wbIndex++);
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
}

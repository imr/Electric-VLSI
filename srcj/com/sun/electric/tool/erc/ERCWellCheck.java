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
package com.sun.electric.tool.erc;

import com.sun.electric.Main;
import com.sun.electric.database.geometry.*;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.DRCRules;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.ErrorLogger.MessageLog;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This is the Electrical Rule Checker tool.
 * @author  Steve Rubin, Gilda Garreton
 */
public class ERCWellCheck
{
    Cell cell;
	int mode;
	ErrorLogger errorLogger;
	Highlighter highlighter;
	List wellCons = new ArrayList();
	List wellAreas = new ArrayList();
	HashMap cellMerges = new HashMap(); // make a map of merge information in each cell
	HashMap doneCells = new HashMap(); // Mark if cells are done already.
	WellCheckJob job;

	// well areas
	static class WellArea
	{
		PolyBase        poly;
		int         netNum;
		int         index;
	};
	// well contacts
	static class WellCon
	{
		Point2D            ctr;
		int                netNum;
		boolean            onProperRail;
		PrimitiveNode.Function fun;
	};

	public ERCWellCheck(Cell cell, WellCheckJob job, int newAlgorithm, Highlighter highlighter)
	{
		this.job = job;
		this.mode = newAlgorithm;
		this.cell = cell;
		this.highlighter = highlighter;
	}

	/*
	 * Method to analyze the current Cell for well errors.
	 */
	public static void analyzeCurCell(int newAlgorithm)
	{
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

		Cell curCell = wnd.getCell();
		if (curCell == null) return;

        new WellCheckJob(curCell, newAlgorithm, highlighter);
	}

	/**
	 * Static function to call the ERC Well functionality
	 * @return number of errors
	 */
	public static int checkERCWell(Cell cell, WellCheckJob job, int newAlgorithm, Highlighter highlighter)
	{
		ERCWellCheck check = new ERCWellCheck(cell, job, newAlgorithm, highlighter);
		return check.doIt();
	}

	public int doIt()
	{
		long startTime = System.currentTimeMillis();
		errorLogger = ErrorLogger.newInstance("ERC Well Check ");
        long initialMemory = 0;

		System.out.println("Checking Wells and Substrates in '" + cell.libDescribe() + "' ...");
		// announce start of analysis
		if (Main.getDebug())
		{
            initialMemory = Runtime.getRuntime().freeMemory();
			System.out.println("Free v/s Total Memory " +
					initialMemory + " / " + Runtime.getRuntime().totalMemory());
		}

		// enumerate the hierarchy below here
		Visitor wcVisitor = new Visitor(this);
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, null, wcVisitor);

		// Checking if job is scheduled for abort or already aborted
		if (job != null && job.checkAbort()) return (0);

		// make a list of well and substrate areas
		int wellIndex = 0;
        GeometryHandler topMerge = (GeometryHandler)cellMerges.get(cell);

		for(Iterator it = topMerge.getKeyIterator(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();

			// Not sure if null goes here
			Collection set = topMerge.getObjects(layer, false, true);

		    if (Main.getDebug())
                System.out.println("Layer " + layer.getName() + " " + set.size());

			for(Iterator pIt = set.iterator(); pIt.hasNext(); )
			{
				WellArea wa = new WellArea();
				PolyBase poly = null;

                if (mode == GeometryHandler.ALGO_QTREE)
                {
                    PolyQTree.PolyNode pn = (PolyQTree.PolyNode)pIt.next();
                    poly = new PolyBase(pn.getPoints(true));

                } else {
                    poly = (PolyBase)pIt.next();
                }

				wa.poly = poly;
				wa.poly.setLayer(layer);
				wa.poly.setStyle(Poly.Type.FILLED);
				wa.index = wellIndex++;
				wellAreas.add(wa);
			}
		}

		if (Main.getDebug())
		{
			System.out.println("Found " + wellAreas.size() + " well/select areas and " + wellCons.size() +
					" contact regions (took " + TextUtils.getElapsedTime(System.currentTimeMillis() - startTime) + ")");
			System.out.println("Free v/s Total Memory Intermediate step: " +
					Runtime.getRuntime().freeMemory() + " / " + Runtime.getRuntime().totalMemory());
            System.out.println("Memory Intermediate consumption: " + (initialMemory - Runtime.getRuntime().freeMemory()));
		}

		boolean foundPWell = false;
		boolean foundNWell = false;

		for(Iterator it = wellAreas.iterator(); it.hasNext(); )
		{
			WellArea wa = (WellArea)it.next();

			int wellType = getWellLayerType(wa.poly.getLayer()); // wa.layer);
			if (wellType != ERCPWell && wellType != ERCNWell) continue;

			// presume N-well
			PrimitiveNode.Function desiredContact = PrimitiveNode.Function.SUBSTRATE;
			String noContactError = "No N-Well contact found in this area";
			int contactAction = ERC.getNWellCheck();

			if (wellType == ERCPWell)
			{
				// P-well
				desiredContact = PrimitiveNode.Function.WELL;
				contactAction = ERC.getPWellCheck();
				noContactError = "No P-Well contact found in this area";
				foundPWell = true;
			}
			else
				foundNWell = true;

			//@TODO: contactAction != 0 -> do nothing
			// find a contact in the area
			boolean found = false;
			for(Iterator cIt = wellCons.iterator(); cIt.hasNext(); )
			{
				WellCon wc = (WellCon)cIt.next();
				if (wc.fun != desiredContact) continue;
				if (!wa.poly.getBounds2D().contains(wc.ctr)) continue;
				if (!wa.poly.contains(wc.ctr)) continue;
				wa.netNum = wc.netNum;
				found = true;
				break;
			}

			// if no contact, issue appropriate errors
			if (!found)
			{
				if (contactAction == 0)
				{
					MessageLog err = errorLogger.logError(noContactError, cell, 0);
					err.addPoly(wa.poly, true, cell);
				}
			}
		}

		// make sure all of the contacts are on the same net
		for(Iterator it = wellCons.iterator(); it.hasNext(); )
		{
			WellCon wc = (WellCon)it.next();
			// -1 means not connected in hierarchyEnumerator::numberNets()
			if (wc.netNum == -1)
			{
				String errorMsg = "N-Well contact is floating";
				if (wc.fun == PrimitiveNode.Function.WELL) errorMsg = "P-Well contact is floating";
				MessageLog err = errorLogger.logError(errorMsg, cell, 0);
				err.addPoint(wc.ctr.getX(), wc.ctr.getY(), cell);
				continue;
			}
			if (!wc.onProperRail)
			{
				if (wc.fun == PrimitiveNode.Function.WELL)
				{
					if (ERC.isMustConnectPWellToGround())
					{
						MessageLog err = errorLogger.logError("P-Well contact not connected to ground", cell, 0);
						err.addPoint(wc.ctr.getX(), wc.ctr.getY(), cell);
					}
				} else
				{
					if (ERC.isMustConnectNWellToPower())
					{
						MessageLog err = errorLogger.logError("N-Well contact not connected to power", cell, 0);
						err.addPoint(wc.ctr.getX(), wc.ctr.getY(), cell);
					}
				}
			}
			// NCC will detect if a ground or power network is incorrectly split into
			// two or more pieces. So this check is not needed here.
/*				for(Iterator oIt = wellCons.iterator(); oIt.hasNext(); )
			{
				WellCon oWc = (WellCon)oIt.next();
				if (oWc.index <= wc.index) continue;

				if (oWc.netNum == -1) continue;   // before ==0
				if (oWc.fun != wc.fun) continue;
				if (oWc.netNum == wc.netNum) continue;
				if (wc.onProperRail && oWc.onProperRail)
					continue; // Both proper connected to gdn/power exports
				String errorMsg = "N-Well contacts are not connected";
				if (wc.fun == PrimitiveNode.Function.WELL) errorMsg = "P-Well contacts are not connected";
				MessageLog err = errorLogger.logError(errorMsg, cell, 0);
				err.addPoint(wc.ctr.getX(), wc.ctr.getY(), cell);
				err.addPoint(oWc.ctr.getX(), oWc.ctr.getY(), cell);
				break;
			}*/
		}

		// if just 1 N-Well contact is needed, see if it is there
		if (ERC.getNWellCheck() == 1 && foundNWell)
		{
			boolean found = false;
			for(Iterator it = wellCons.iterator(); it.hasNext(); )
			{
				WellCon wc = (WellCon)it.next();
				if (wc.fun == PrimitiveNode.Function.SUBSTRATE) { found = true;   break; }
			}
			if (!found)
			{
				errorLogger.logError("No N-Well contact found in this cell", cell, 0);
			}
		}

		// if just 1 P-Well contact is needed, see if it is there
		if (ERC.getPWellCheck() == 1 && foundPWell)
		{
			boolean found = false;
			for(Iterator it = wellCons.iterator(); it.hasNext(); )
			{
				WellCon wc = (WellCon)it.next();
				if (wc.fun == PrimitiveNode.Function.WELL) { found = true;   break; }
			}
			if (!found)
			{
				errorLogger.logError("No P-Well contact found in this cell", cell, 0);
			}
		}

		// make sure the wells are separated properly
		// Local storage of rules.. otherwise getSpacingRule is called too many times
		// THIS IS A DRC JOB .. not efficient if done here.
		if (ERC.isDRCCheck())
		{
			HashMap rulesCon = new HashMap();
			HashMap rulesNonCon = new HashMap();
            int techMode = DRC.getFoundry();
			for(Iterator it = wellAreas.iterator(); it.hasNext(); )
			{
				WellArea wa = (WellArea)it.next();
				for(Iterator oIt = wellAreas.iterator(); oIt.hasNext(); )
				{
					// Checking if job is scheduled for abort or already aborted
					if (job != null && job.checkAbort()) return (0);

					WellArea oWa = (WellArea)oIt.next();
					if (wa.index <= oWa.index) continue;
					Layer waLayer = wa.poly.getLayer();
					if (waLayer != oWa.poly.getLayer()) continue;
					//if (wa.layer != oWa.layer) continue;
					boolean con = false;
					if (wa.netNum == oWa.netNum && wa.netNum >= 0) con = true;
					DRCRules.DRCRule rule = (con)?
							(DRCRules.DRCRule)rulesCon.get(waLayer):
							(DRCRules.DRCRule)rulesNonCon.get(waLayer);
					// @TODO Might still return NULL the first time!!. Need another array or aux class?
					if (rule == null)
					{
						rule = DRC.getSpacingRule(waLayer, waLayer, con, false, 0, techMode);
						if (rule == null)
							System.out.println("Replace this");
						if (con)
							rulesCon.put(waLayer, rule);
						else
							rulesNonCon.put(waLayer, rule);
					}
					//DRCRules.DRCRule rule = DRC.getSpacingRule(wa.layer, wa.layer, con, false, 0);
					if (rule == null || rule.value < 0) continue;
					if (wa.poly.getBounds2D().getMinX() > oWa.poly.getBounds2D().getMaxX()+rule.value ||
						oWa.poly.getBounds2D().getMinX() > wa.poly.getBounds2D().getMaxX()+rule.value ||
						wa.poly.getBounds2D().getMinY() > oWa.poly.getBounds2D().getMaxY()+rule.value ||
						oWa.poly.getBounds2D().getMinY() > wa.poly.getBounds2D().getMaxY()+rule.value) continue;
					double dist = wa.poly.separation(oWa.poly); // dist == 0 -> intersect or inner loops
					if (dist > 0 && dist < rule.value)
					{
						int layertype = getWellLayerType(waLayer);
						if (layertype == ERCPSEUDO) continue;

						MessageLog err = errorLogger.logError(waLayer.getName() + " areas too close (are "
								+ TextUtils.formatDouble(dist, 1) + ", should be "
								+ TextUtils.formatDouble(rule.value, 1) + ")", cell, 0);
						err.addPoly(wa.poly, true, cell);
						err.addPoly(oWa.poly, true, cell);
					}
				}
			}
		}
		if (Main.getDebug())
			System.out.println("Free v/s Total Memory Intermediate step 2: " +
				Runtime.getRuntime().freeMemory() + " / " + Runtime.getRuntime().totalMemory());

		// compute edge distance if requested
		if (ERC.isFindWorstCaseWell())
		{
			double worstPWellDist = 0;
			Point2D worstPWellCon = null;
			Point2D worstPWellEdge = null;
			double worstNWellDist = 0;
			Point2D worstNWellCon = null;
			Point2D worstNWellEdge = null;

			for(Iterator it = wellAreas.iterator(); it.hasNext(); )
			{
				WellArea wa = (WellArea)it.next();

				int wellType = getWellLayerType(wa.poly.getLayer());
				if (!isERCLayerRelated(wa.poly.getLayer())) continue;
				PrimitiveNode.Function desiredContact = PrimitiveNode.Function.SUBSTRATE;
				if (wellType == ERCPWell) desiredContact = PrimitiveNode.Function.WELL;

				// find the worst distance to the edge of the area
				Point2D [] points = wa.poly.getPoints();
				int count = points.length;
				for(int i=0; i<count*2; i++)
				{
					// figure out which point is being analyzed
					Point2D testPoint = null;
					if (i < count)
					{
						int prev = i-1;
						if (i == 0) prev = count-1;
						testPoint = new Point2D.Double((points[prev].getX() + points[i].getX()) / 2, (points[prev].getY() + points[i].getY()) / 2);
					} else
					{
						testPoint = points[i-count];
					}

					// find the closest contact to this point
					boolean first = true;
					double bestDist = 0;
					WellCon bestWc = null;
					for(Iterator cIt = wellCons.iterator(); cIt.hasNext(); )
					{
						WellCon wc = (WellCon)cIt.next();
						if (wc.fun != desiredContact) continue;
						if (!wa.poly.getBounds2D().contains(wc.ctr)) continue;
						if (!wa.poly.contains(wc.ctr)) continue;
						double dist = testPoint.distance(wc.ctr);
						if (first || dist < bestDist)
						{
							bestDist = dist;
							bestWc = wc;
						}
						first = false;
					}
					if (first) continue;

					// accumulate worst distances to edges
					if (wellType == 1)
					{
						if (bestDist > worstPWellDist)
						{
							worstPWellDist = bestDist;
							worstPWellCon = bestWc.ctr;
							worstPWellEdge = testPoint;
						}
					} else
					{
						if (bestDist > worstNWellDist)
						{
							worstNWellDist = bestDist;
							worstNWellCon = bestWc.ctr;
							worstNWellEdge = testPoint;
						}
					}
				}
			}

			// show the farthest distance from a well contact
			if (highlighter != null && (worstPWellDist > 0 || worstNWellDist > 0))
			{
				highlighter.clear();
				if (worstPWellDist > 0)
				{
					highlighter.addLine(worstPWellCon, worstPWellEdge, cell);
					System.out.println("Farthest distance from a P-Well contact is " + worstPWellDist);
				}
				if (worstNWellDist > 0)
				{
					highlighter.addLine(worstNWellCon, worstNWellEdge, cell);
					System.out.println("Farthest distance from an N-Well contact is " + worstNWellDist);
				}
				highlighter.finished();
			}
		}

		// report the number of errors found
		long endTime = System.currentTimeMillis();
		int errorCount = errorLogger.getNumErrors();
		if (errorCount == 0)
		{
			System.out.println("No Well errors found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
		} else
		{
			System.out.println("FOUND " + errorCount + " WELL ERRORS (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
		}

		if (Main.getDebug())
		{
			System.out.println("Free v/s Total Memory Final Step: " +
					Runtime.getRuntime().freeMemory() + " / " + Runtime.getRuntime().totalMemory());
		}
		wellAreas.clear();
		wellCons.clear();
		doneCells.clear();
		cellMerges.clear();
		errorLogger.termLogging(true);
		return (errorCount);
	}

	private static class WellCheckJob extends Job
	{
		Cell cell;
		int newAlgorithm;
		Highlighter highlighter;

		protected WellCheckJob(Cell cell, int newAlgorithm, Highlighter highlighter)
		{
			super("ERC Well Check", ERC.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.newAlgorithm = newAlgorithm;
			this.highlighter = highlighter;
			startJob();
		}

		public boolean doIt()
		{
			return(checkERCWell(cell, this, newAlgorithm, highlighter) == 0);
		}
    }

	private static class Visitor extends HierarchyEnumerator.Visitor
    {
		ERCWellCheck check;

        public Visitor(ERCWellCheck check)
        {
	        this.check = check;
        }

        public boolean enterCell(HierarchyEnumerator.CellInfo info)
        {
	        // Checking if job is scheduled for abort or already aborted
	        if (check.job != null && check.job.checkAbort()) return (false);

			// make an object for merging all of the wells in this cell
			Cell cell = info.getCell();
	        GeometryHandler thisMerge = (GeometryHandler)(check.cellMerges.get(cell));

			if (thisMerge == null)
			{
                thisMerge = GeometryHandler.createGeometryHandler(check.mode,
                        ercLayers.size(), cell.getBounds());
				check.cellMerges.put(cell, thisMerge);
			}

            return true;
        }

        public void exitCell(HierarchyEnumerator.CellInfo info)
        {
	        // Checking if job is scheduled for abort or already aborted
	        if (check.job != null && check.job.checkAbort()) return;

			// make an object for merging all of the wells in this cell
			Cell cell = info.getCell();
	        GeometryHandler thisMerge = (GeometryHandler)check.cellMerges.get(info.getCell());
			if (thisMerge == null) throw new Error("wrong condition in ERCWellCheck.enterCell()");
            boolean done = check.doneCells.get(cell) != null;

	        if (!done)
	        {
                boolean qTreeAlgo = check.mode == GeometryHandler.ALGO_QTREE;

				for(Iterator it = cell.getArcs(); it.hasNext(); )
				{
					ArcInst ai = (ArcInst)it.next();

					Technology tech = ai.getProto().getTechnology();
					// Getting only ercLayers
					Poly [] arcInstPolyList = tech.getShapeOfArc(ai, null, null, ercLayers);
					int tot = arcInstPolyList.length;
					for(int i=0; i<tot; i++)
					{
						Poly poly = arcInstPolyList[i];
						Layer layer = poly.getLayer();
						// Only interested in well/select
						Object newElem = poly;

                        if (qTreeAlgo)
                            newElem = new PolyQTree.PolyNode(poly);
						thisMerge.add(layer, newElem, qTreeAlgo);
					}
				}

                thisMerge.postProcess(true);

                // merge everything sub trees
                for(Iterator it = cell.getNodes(); it.hasNext(); )
                {
                    NodeInst ni = (NodeInst)it.next();
                    NodeProto subNp = ni.getProto();
                    if (subNp instanceof PrimitiveNode) continue;
                    // get sub-merge information for the cell instance
                    GeometryHandler subMerge = (GeometryHandler)check.cellMerges.get(subNp);
                    if (subMerge != null)
                    {
                        AffineTransform tTrans = ni.translateOut(ni.rotateOut());
                        thisMerge.addAll(subMerge, tTrans);
                    }
                }
	        }

	        // To mark if cell is already done
			check.doneCells.put(cell, cell);
       }
        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
        {
	        // Checking if job is scheduled for abort or already aborted
	        //if (check.job!= null && check.job.checkForAbort()) return (false);

			NodeInst ni = no.getNodeInst();
	        if (NodeInst.isSpecialNode(ni))
		        return false; // Nothing to do, Dec 9;

			// merge everything
	        Cell cell = info.getCell();
	        GeometryHandler thisMerge = (GeometryHandler)check.cellMerges.get(cell);
            AffineTransform trans = null;
            PrimitiveNode.Function fun = ni.getFunction();
	        boolean wellSubsContact = (fun == PrimitiveNode.Function.WELL || fun == PrimitiveNode.Function.SUBSTRATE);

	        // No done yet
	        if (check.doneCells.get(cell) == null)
	        {
                boolean qTreeAlgo = check.mode == GeometryHandler.ALGO_QTREE;
				NodeProto subNp = ni.getProto();
				if (subNp instanceof PrimitiveNode)
				{
					PrimitiveNode pNp = (PrimitiveNode)subNp;
					Technology tech = pNp.getTechnology();
					// Getting only ercLayers
					Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, null, null, true, true, ercLayers);
					int tot = nodeInstPolyList.length;

					for(int i=0; i<tot; i++)
					{
						Poly poly = nodeInstPolyList[i];
						Layer layer = poly.getLayer();
						// Only interested in well/select regions

                        if (trans == null) trans = ni.rotateOut();  // transformation only calculated when required.
						poly.transform(trans);
						Object newElem = poly;

						if (qTreeAlgo)
                            newElem = new PolyQTree.PolyNode(poly);
						thisMerge.add(layer, newElem, qTreeAlgo);
					}
				}
	        }

			// look for well and substrate contacts
			if (wellSubsContact)
			{
				WellCon wc = new WellCon();
				wc.ctr = ni.getTrueCenter();
                if (trans == null) trans = ni.rotateOut();  // transformation only calculated when required.
				trans.transform(wc.ctr, wc.ctr);
				info.getTransformToRoot().transform(wc.ctr, wc.ctr);
				wc.fun = fun;
				PortInst pi = ni.getOnlyPortInst();
				Netlist netList = info.getNetlist();
				Network net = netList.getNetwork(pi);
				wc.netNum = info.getNetID(net);
				wc.onProperRail = false;
				if (net != null)
				{
					boolean searchWell = (fun == PrimitiveNode.Function.WELL);
					// PWell: must be on ground or  NWell: must be on power
					Network parentNet = net;
					HierarchyEnumerator.CellInfo cinfo = info;
					while (cinfo.getParentInst() != null) {
						parentNet = cinfo.getNetworkInParent(parentNet);
						cinfo = cinfo.getParentInfo();
						if (parentNet == null && Main.LOCALDEBUGFLAG)
							System.out.println("parentNet null in ERC. Stop loop?");
					}
					if (parentNet != null)
					{
						for (Iterator it = parentNet.getExports(); !wc.onProperRail && it.hasNext();)
						{
							Export exp = (Export)it.next();
							if ((searchWell && exp.isGround()) || (!searchWell && exp.isPower()))
								wc.onProperRail = true;
						}
					}
				}
				check.wellCons.add(wc);
			}
            return true;
        }
    }

	/*
	 * Method to return nonzero if layer "layer" is a well/select layer.
	 * Returns:
	 *   1: P-Well
	 *   2: N-Well
	 *   3: P-Select
	 *   4: N-Select
	 */
	private static final int ERCPWell = 1;
	private static final int ERCNWell = 2;
	private static final int ERCPSelect = 3;
	private static final int ERCNSelect = 4;
	private static final int ERCPSEUDO = 0;

	private static final List ercLayers = new ArrayList(7);

    static {
	    ercLayers.add(Layer.Function.WELLP);
	    ercLayers.add(Layer.Function.WELL);
	    ercLayers.add(Layer.Function.WELLN);
		ercLayers.add(Layer.Function.SUBSTRATE);
	    ercLayers.add(Layer.Function.IMPLANTP);
	    ercLayers.add(Layer.Function.IMPLANT);
	    ercLayers.add(Layer.Function.IMPLANTN);
    };

	/**
	 * Determine if layer is relevant for ERC process to
	 * speed up calculation
	 * @param layer
	 * @return true for wells and selects
	 */
	private static boolean isERCLayerRelated(Layer layer)
	{
		int type = getWellLayerType(layer);
		return (type == ERCPWell || type == ERCNWell ||
		        type == ERCPSelect || type == ERCNSelect);
	}

	private static int getWellLayerType(Layer layer)
	{
		Layer.Function fun = layer.getFunction();
		int extra = layer.getFunctionExtras();
		if ((extra&Layer.Function.PSEUDO) != 0) return ERCPSEUDO;
		if (fun == Layer.Function.WELLP) return ERCPWell;
		if (fun == Layer.Function.WELL || fun == Layer.Function.WELLN) return ERCNWell;
		if (fun == Layer.Function.IMPLANTP)
			return ERCPSelect;
		if (fun == Layer.Function.IMPLANT || fun == Layer.Function.IMPLANTN)
			return ERCNSelect;
		if (fun == Layer.Function.SUBSTRATE)
		{
			if ((extra&Layer.Function.PTYPE) != 0) return ERCPSelect;
			return ERCNSelect;
		}
		return ERCPSEUDO;
	}

}

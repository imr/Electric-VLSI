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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.erc.ERC;
import com.sun.electric.tool.user.ErrorLog;
import com.sun.electric.tool.user.Highlight;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This is the Electrical Rule Checker tool.
 */
public class ERCWellCheck
{
	// well contacts
	static class WellCon
	{
		Point2D            ctr;
		int                netNum;
		boolean            onProperRail;
		NodeProto.Function fun;
		NodeProto          np;
		int                index;
	};
	private static List wellCons;
	private static int wellConIndex;

	// well areas
	static class WellArea
	{
		Rectangle2D bounds;
		Poly        poly;
		Layer       layer;
		int         netNum;
		int         index;
	};
	private static List wellAreas;

	private static HashMap cellMerges;

	/*
	 * Method to analyze the current Cell for well errors.
	 */
	public static void analyzeCurCell()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;
        WellCheck job = new WellCheck(curCell);
	}

	protected static class WellCheck extends Job
	{
		Cell cell;

		protected WellCheck(Cell cell)
		{
			super("ERC Well Check", ERC.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public void doIt()
		{
			long startTime = System.currentTimeMillis();
			ErrorLog.initLogging("ERC Well Check");

			// announce start of analysis
			System.out.println("Checking Wells and Substrates...");

			// make a list of well and substrate contacts
			wellCons = new ArrayList();
			wellConIndex = 0;

			// make a map of merge information in each cell
			cellMerges = new HashMap();

			// enumerate the hierarchy below here
			Visitor wcVisitor = new Visitor();
			HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, null, wcVisitor);

			// make a list of well and substrate areas
			PolyMerge topMerge = (PolyMerge)cellMerges.get(cell);
			wellAreas = new ArrayList();
			int wellIndex = 0;
			for(Iterator it = topMerge.getLayersUsed(); it.hasNext(); )
			{
				Layer layer = (Layer)it.next();
				List polyList = topMerge.getMergedPoints(layer);
				if (polyList != null)
				{
					for(Iterator pIt = polyList.iterator(); pIt.hasNext(); )
					{
						WellArea wa = new WellArea();
						wa.poly = (Poly)pIt.next();
						wa.bounds = wa.poly.getBounds2D();
						wa.layer = layer;
						wa.index = wellIndex++;
						wellAreas.add(wa);
					}
				}
			}

			// number the well areas according to topology of contacts in them
			int largestNetNum = 0;
			for(Iterator it = wellCons.iterator(); it.hasNext(); )
			{
				WellCon wc = (WellCon)it.next();
				if (wc.netNum > largestNetNum)
					largestNetNum = wc.netNum;
			}
			for(Iterator it = wellAreas.iterator(); it.hasNext(); )
			{
				WellArea wa = (WellArea)it.next();
				int wellType = getWellLayerType(wa.layer);
				if (wellType != 1 && wellType != 2) continue;

				// presume N-well
				NodeProto.Function desiredContact = NodeProto.Function.SUBSTRATE;
				String noContactError = "No N-Well contact found in this area";
				int contactAction = ERC.getNWellCheck();
				if (wellType == 1)
				{
					// P-well
					desiredContact = NodeProto.Function.WELL;
					contactAction = ERC.getPWellCheck();
					noContactError = "No P-Well contact found in this area";
				}

				// find a contact in the area
				boolean found = false;
				for(Iterator cIt = wellCons.iterator(); cIt.hasNext(); )
				{
					WellCon wc = (WellCon)cIt.next();
					if (wc.fun != desiredContact) continue;
					if (!wa.bounds.contains(wc.ctr)) continue;
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
						ErrorLog err = ErrorLog.logError(noContactError, cell, 0);
						err.addPoly(wa.poly, true);
					}
				}
			}

			// make sure all of the contacts are on the same net
			for(Iterator it = wellCons.iterator(); it.hasNext(); )
			{
				WellCon wc = (WellCon)it.next();
				if (wc.netNum == 0)
				{
					String errorMsg = "N-Well contact is floating";
					if (wc.fun == NodeProto.Function.WELL) errorMsg = "P-Well contact is floating";
					ErrorLog err = ErrorLog.logError(errorMsg, cell, 0);
					err.addPoint(wc.ctr.getX(), wc.ctr.getY());
					continue;
				}
				if (!wc.onProperRail)
				{
					if (wc.fun == NodeProto.Function.WELL)
					{
						if (ERC.isMustConnectPWellToGround())
						{
							ErrorLog err = ErrorLog.logError("P-Well contact not connected to ground", cell, 0);
							err.addPoint(wc.ctr.getX(), wc.ctr.getY());
						}
					} else
					{
						if (ERC.isMustConnectNWellToPower())
						{
							ErrorLog err = ErrorLog.logError("N-Well contact not connected to power", cell, 0);
							err.addPoint(wc.ctr.getX(), wc.ctr.getY());
						}
					}
				}
				for(Iterator oIt = wellCons.iterator(); oIt.hasNext(); )
				{
					WellCon oWc = (WellCon)oIt.next();
					if (oWc.index <= wc.index) continue;

					if (oWc.netNum == 0) continue;
					if (oWc.fun != wc.fun) continue;
					if (oWc.netNum == wc.netNum) continue;
					String errorMsg = "N-Well contacts are not connected";
					if (wc.fun == NodeProto.Function.WELL) errorMsg = "P-Well contacts are not connected";
					ErrorLog err = ErrorLog.logError(errorMsg, cell, 0);
					err.addPoint(wc.ctr.getX(), wc.ctr.getY());
					err.addPoint(oWc.ctr.getX(), oWc.ctr.getY());
					break;
				}
			}

			// if just 1 N-Well contact is needed, see if it is there
			if (ERC.getNWellCheck() == 1)
			{
				boolean found = false;
				for(Iterator it = wellCons.iterator(); it.hasNext(); )
				{
					WellCon wc = (WellCon)it.next();
					if (wc.fun == NodeProto.Function.SUBSTRATE) { found = true;   break; }
				}
				if (!found)
				{
					ErrorLog err = ErrorLog.logError("No N-Well contact found in this cell", cell, 0);
				}
			}

			// if just 1 P-Well contact is needed, see if it is there
			if (ERC.getPWellCheck() == 1)
			{
				boolean found = false;
				for(Iterator it = wellCons.iterator(); it.hasNext(); )
				{
					WellCon wc = (WellCon)it.next();
					if (wc.fun == NodeProto.Function.WELL) { found = true;   break; }
				}
				if (!found)
				{
					ErrorLog err = ErrorLog.logError("No P-Well contact found in this cell", cell, 0);
				}
			}

			// make sure the wells are separated properly
			for(Iterator it = wellAreas.iterator(); it.hasNext(); )
			{
				WellArea wa = (WellArea)it.next();
				for(Iterator oIt = wellAreas.iterator(); oIt.hasNext(); )
				{
					WellArea oWa = (WellArea)oIt.next();
					if (wa.index <= oWa.index) continue;
					if (wa.layer != oWa.layer) continue;
					boolean con = false;
					if (wa.netNum == oWa.netNum && wa.netNum >= 0) con = true;
					DRC.Rule rule = DRC.getSpacingRule(wa.layer, wa.layer, con, false, false);
					if (rule.distance < 0) continue;
					if (wa.bounds.getMinX() > oWa.bounds.getMaxX()+rule.distance ||
						oWa.bounds.getMinX() > wa.bounds.getMaxX()+rule.distance ||
						wa.bounds.getMinY() > oWa.bounds.getMaxY()+rule.distance ||
						oWa.bounds.getMinY() > wa.bounds.getMaxY()+rule.distance) continue;
					double dist = wa.poly.separation(oWa.poly);
					if (dist < rule.distance)
					{
						int layertype = getWellLayerType(wa.layer);
						if (layertype == 0) continue;
						String areaType = null;
						switch (layertype)
						{
							case 1: areaType = "P-Well";    break;
							case 2: areaType = "N-Well";    break;
							case 3: areaType = "P-Select";  break;
							case 4: areaType = "N-Select";  break;
						}
						ErrorLog err = ErrorLog.logError(areaType + " areas too close (are " + dist + ", should be " + rule.distance + ")", cell, 0);
						err.addPoly(wa.poly, true);
						err.addPoly(oWa.poly, true);
					}
				}
			}

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

					int wellType = getWellLayerType(wa.layer);
					if (wellType != 1 && wellType != 2) continue;
					NodeProto.Function desiredContact = NodeProto.Function.SUBSTRATE;
					if (wellType == 1) desiredContact = NodeProto.Function.WELL;

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
							if (!wa.bounds.contains(wc.ctr)) continue;
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
				if (worstPWellDist > 0 || worstNWellDist > 0)
				{
					Highlight.clear();
					if (worstPWellDist > 0)
					{
						Highlight.addLine(worstPWellCon, worstPWellEdge, cell);
						System.out.println("Farthest distance from a P-Well contact is " + worstPWellDist);
					}
					if (worstNWellDist > 0)
					{
						Highlight.addLine(worstNWellCon, worstNWellEdge, cell);
						System.out.println("Farthest distance from an N-Well contact is " + worstNWellDist);
					}
					Highlight.finished();
				}
			}

			// report the number of errors found
			long endTime = System.currentTimeMillis();
			int errorCount = ErrorLog.numErrors();
			if (errorCount == 0)
			{
				System.out.println("No Well errors found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
			} else
			{
				System.out.println("FOUND " + errorCount + " WELL ERRORS (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
			}
			ErrorLog.termLogging(true);
		}
	}

    public static class Visitor extends HierarchyEnumerator.Visitor
    {
        public Visitor()
        {
        }

        public boolean enterCell(HierarchyEnumerator.CellInfo info) 
        {
            return true;
        }

        public void exitCell(HierarchyEnumerator.CellInfo info) 
        {
			// make an object for merging all of the wells in this cell
			Cell cell = info.getCell();
			PolyMerge merge = (PolyMerge)cellMerges.get(cell);
			if (merge == null)
			{
				merge = new PolyMerge();
				cellMerges.put(cell, merge);

				// merge everything
				for(Iterator it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					AffineTransform trans = ni.rotateOut();
					NodeProto subNp = ni.getProto();
					if (subNp instanceof PrimitiveNode)
					{
						PrimitiveNode pNp = (PrimitiveNode)subNp;
						Technology tech = pNp.getTechnology();
						Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, null, true, true);
						int tot = nodeInstPolyList.length;
						for(int i=0; i<tot; i++)
						{
							Poly poly = nodeInstPolyList[i];
							Layer layer = poly.getLayer();
							if (getWellLayerType(layer) == 0) continue;
							poly.transform(trans);
							merge.addPolygon(layer, poly);
						}
					} else
					{
						// get sub-merge information for the cell instance
						PolyMerge subMerge = (PolyMerge)cellMerges.get(subNp);
						if (subMerge != null)
						{
							AffineTransform tTrans = ni.translateOut();
							tTrans.concatenate(trans);
							merge.addMerge(subMerge, tTrans);
						}
					}
				}
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
						if (getWellLayerType(layer) == 0) continue;
						merge.addPolygon(layer, poly);
					}
				}
			}

			// look for well and substrate contacts
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				AffineTransform trans = ni.rotateOut();
				NodeProto.Function fun = ni.getFunction();
				if (fun == NodeProto.Function.WELL || fun == NodeProto.Function.SUBSTRATE)
				{
					WellCon wc = new WellCon();
					wc.ctr = ni.getTrueCenter();
					trans.transform(wc.ctr, wc.ctr);
					info.getTransformToRoot().transform(wc.ctr, wc.ctr);
					wc.np = ni.getProto();
					wc.fun = fun;
					wc.index = wellConIndex++;
					PortInst pi = ni.getOnlyPortInst();
					Netlist netList = info.getNetlist();
					JNetwork net = netList.getNetwork(pi);
					wc.netNum = info.getNetID(net);
					wc.onProperRail = false;
					if (net != null)
					{
						if (fun == NodeProto.Function.WELL)
						{
							// PWell: must be on ground
//							for(pp = np->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//							{
//								if ((pp->userbits&STATEBITS) != GNDPORT) continue;
//								if (pp->network == net) break;
//							}
//							if (pp != null)
								wc.onProperRail = true;
						} else
						{
							// NWell: must be on power
//							for(pp = np->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//							{
//								if ((pp->userbits&STATEBITS) != PWRPORT) continue;
//								if (pp->network == net) break;
//							}
//							if (pp != null)
								wc.onProperRail = true;
						}
					}
					wellCons.add(wc);
				}
			}
       }

        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) 
        {
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
	private static int getWellLayerType(Layer layer)
	{
		Layer.Function fun = layer.getFunction();
		int extra = layer.getFunctionExtras();
		if ((extra&Layer.Function.PSEUDO) != 0) return 0;
		if (fun == Layer.Function.WELLP) return 1;
		if (fun == Layer.Function.WELL || fun == Layer.Function.WELLN) return 2;
		if (fun == Layer.Function.IMPLANTP) return 3;
		if (fun == Layer.Function.IMPLANT || fun == Layer.Function.IMPLANTN) return 4;
		if (fun == Layer.Function.SUBSTRATE)
		{
			if ((extra&Layer.Function.PTYPE) != 0) return 3;
			return 4;
		}
		return 0;
	}

}

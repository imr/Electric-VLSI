/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MimicStitch.java
 * Routing tool: Mimic Stitcher (duplicates user's routes elsewhere in the cell).
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.routing;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WiringListener;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JOptionPane;

/**
 * This is the Mimic Stitching tool.
 */
public class MimicStitch
{
    /** router to use */            static InteractiveRouter router = new SimpleWirer();

	/**
	 * Entry point for mimic router.
	 * @param forced true if this mimic operation was explicitly requested.
	 */
	public static void mimicStitch(boolean forced)
	{
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

		Routing.Activity lastActivity = Routing.tool.getLastActivity();
		if (lastActivity == null)
		{
			System.out.println("No wiring activity to mimic");
			return;
		}

		// if a single arc was deleted, and that is being mimiced, do it
		if (lastActivity.numDeletedArcs == 1 && Routing.isMimicStitchCanUnstitch())
		{
			ArcProto ap = lastActivity.deletedArcs[0].getProto();
			ro_mimicdelete(ap, lastActivity);
			lastActivity.numDeletedArcs = 0;
			return;
		}

		// if a single arc was just created, mimic that
		if (lastActivity.numCreatedArcs == 1)
		{
			ArcInst ai = lastActivity.createdArcs[0];
			MimicStitchJob job = new MimicStitchJob(ai.getHead(), ai.getTail(), ai.getWidth(), ai.getProto(), 0, 0, forced, highlighter);
			lastActivity.numCreatedArcs = 0;
			return;
		}

		// if multiple arcs were just created, find the true end and mimic that
		if (lastActivity.numCreatedArcs > 1 && lastActivity.numCreatedNodes > 0)
		{
			// find the ends of arcs that do not attach to the intermediate pins
			FlagSet nodeGotOne = NodeInst.getFlagSet(1);
			FlagSet nodeGotMany = NodeInst.getFlagSet(1);
			ArcProto proto = lastActivity.createdArcs[0].getProto();
			Cell parent = lastActivity.createdArcs[0].getParent();
			for(Iterator it = parent.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.clearBit(nodeGotOne);
				ni.clearBit(nodeGotMany);
			}
			for(int i=0; i<lastActivity.numCreatedArcs; i++)
			{
				ArcInst ai = lastActivity.createdArcs[i];
				for(int e=0; e<2; e++)
				{
					NodeInst ni = ai.getConnection(e).getPortInst().getNodeInst();
					if (!ni.isBit(nodeGotMany))
					{
						if (!ni.isBit(nodeGotOne)) ni.setBit(nodeGotOne); else
						{
							ni.clearBit(nodeGotOne);
							ni.setBit(nodeGotMany);
						}
					}
				}
			}
			int foundends = 0;
			Connection [] ends = new Connection[2];
			double width = 0;
			for(int i=0; i<lastActivity.numCreatedArcs; i++)
			{
				ArcInst ai = lastActivity.createdArcs[i];
				for(int e=0; e<2; e++)
				{
					NodeInst ni = ai.getConnection(e).getPortInst().getNodeInst();
					if (!ni.isBit(nodeGotOne)) continue;
					if (foundends < 2)
					{
						ends[foundends] = ai.getConnection(e);
						if (ai.getWidth() > width)
							width = ai.getWidth();
					}
					foundends++;
				}
			}
			nodeGotOne.freeFlagSet();
			nodeGotMany.freeFlagSet();

			// if exactly two ends are found, mimic that connection
			if (foundends == 2)
			{
				double prefx = 0, prefy = 0;
				if (lastActivity.numCreatedNodes == 1)
				{
					Poly portPoly0 = ends[0].getPortInst().getPoly();
				double x0 = portPoly0.getCenterX();
					double y0 = portPoly0.getCenterY();
					Poly portPoly1 = ends[1].getPortInst().getPoly();
					double x1 = portPoly1.getCenterX();
					double y1 = portPoly1.getCenterY();
					prefx = lastActivity.createdNodes[0].getAnchorCenterX() - (x0+x1) / 2;
					prefy = lastActivity.createdNodes[0].getAnchorCenterY() - (y0+y1) / 2;
				} else if (lastActivity.numCreatedNodes == 2)
				{
					Poly portPoly0 = ends[0].getPortInst().getPoly();
					double x0 = portPoly0.getCenterX();
					double y0 = portPoly0.getCenterY();
					Poly portPoly1 = ends[1].getPortInst().getPoly();
					double x1 = portPoly1.getCenterX();
					double y1 = portPoly1.getCenterY();
					prefx = (lastActivity.createdNodes[0].getAnchorCenterX() +
						lastActivity.createdNodes[1].getAnchorCenterX()) / 2 -
							(x0+x1) / 2;
					prefy = (lastActivity.createdNodes[0].getAnchorCenterY() +
						lastActivity.createdNodes[1].getAnchorCenterY()) / 2 -
							(y0+y1) / 2;
				}
				MimicStitchJob job = new MimicStitchJob(ends[0], ends[1], width, proto, prefx, prefy, forced, highlighter);
			}
			lastActivity.numCreatedArcs = 0;
			return;
		}
	}

	/**
	 * Method to mimic the unrouting of an arc that ran from nodes[0]/ports[0] to
	 * nodes[1]/ports[1] with type "typ".
	 */
	private static void ro_mimicdelete(ArcProto typ, Routing.Activity activity)
	{
		// determine length of deleted arc
		Point2D pt0 = activity.deletedArcs[0].getHead().getLocation();
		Point2D pt1 = activity.deletedArcs[0].getTail().getLocation();
		double dist = pt0.distance(pt1);
		int angle = 0;
		if (dist != 0) angle = DBMath.figureAngle(pt0, pt1);

		// look for a similar situation to delete
		Cell cell = activity.deletedNodes[0].getParent();
		List arcKills = new ArrayList();
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			// arc must be of the same type
			if (ai.getProto() != typ) continue;

			// arc must connect to the same type of node/port
			int match = 0;
			if (ai.getHead().getPortInst().getNodeInst().getProto() == activity.deletedNodes[0].getProto() &&
				ai.getTail().getPortInst().getNodeInst().getProto() == activity.deletedNodes[1].getProto() &&
				ai.getHead().getPortInst().getPortProto() == activity.deletedPorts[0] &&
				ai.getTail().getPortInst().getPortProto() == activity.deletedPorts[1]) match = 1;
			if (ai.getHead().getPortInst().getNodeInst().getProto() == activity.deletedNodes[1].getProto() &&
				ai.getTail().getPortInst().getNodeInst().getProto() == activity.deletedNodes[0].getProto() &&
				ai.getHead().getPortInst().getPortProto() == activity.deletedPorts[1] &&
				ai.getTail().getPortInst().getPortProto() == activity.deletedPorts[0]) match = -1;
			if (match == 0) continue;

			// must be the same length and angle
			Point2D end0 = ai.getHead().getLocation();
			Point2D end1 = ai.getTail().getLocation();
			double thisdist = end0.distance(end1);
			if (dist != thisdist) continue;
			if (dist != 0)
			{
				int thisangle = DBMath.figureAngle(end0, end1);
				if ((angle%1800) != (thisangle%1800)) continue;
			}

			// the same! queue it for deletion
			arcKills.add(ai);
		}
		MimicUnstitchJob job = new MimicUnstitchJob(arcKills);
	}

	/**
	 * Class to change the node/arc type in a new thread.
	 */
	private static class MimicUnstitchJob extends Job
	{
		List arcKills;

		protected MimicUnstitchJob(List arcKills)
		{
			super("Mimic-Unstitch", Routing.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.arcKills = arcKills;
            setReportExecutionFlag(true);
			startJob();
		}

		public boolean doIt()
		{
			// now delete those arcs
			int deleted = 0;
			for(Iterator it = arcKills.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();

				ai.kill();
				deleted++;
			}
			if (deleted != 0)
				System.out.println("MIMIC ROUTING: deleted " + deleted + "wires");
			return true;
		}
	}

	/**
	 * Class to change the node/arc type in a new thread.
	 */
	private static class MimicStitchJob extends Job
	{
		private Connection conn1, conn2;
		private double oWidth;
		private ArcProto oProto;
		private double prefX, prefY;
		private boolean forced;
		HashSet portMark;
        private List allRoutes;                         // all routes to be created
        private Highlighter highlighter;

		static class PossibleArc
		{
			int               situation;
			NodeInst          ni1, ni2;
			PortProto         pp1, pp2;
			Point2D           pt1, pt2;
		};

		private static final int LIKELYDIFFPORT     =  1;
		private static final int LIKELYDIFFARCCOUNT =  2;
		private static final int LIKELYDIFFNODETYPE =  4;
		private static final int LIKELYDIFFNODESIZE =  8;
		private static final int LIKELYARCSSAMEDIR  = 16;

		private static int situations[] = {
			0,
			LIKELYARCSSAMEDIR,
							  LIKELYDIFFNODESIZE,
												 LIKELYDIFFARCCOUNT,
																	LIKELYDIFFPORT,
																				   LIKELYDIFFNODETYPE,

			LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE,
			LIKELYARCSSAMEDIR|                   LIKELYDIFFARCCOUNT,
			LIKELYARCSSAMEDIR|                                      LIKELYDIFFPORT,
			LIKELYARCSSAMEDIR|                                                     LIKELYDIFFNODETYPE,
							  LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT,
							  LIKELYDIFFNODESIZE|                   LIKELYDIFFPORT,
							  LIKELYDIFFNODESIZE|                                  LIKELYDIFFNODETYPE,
												 LIKELYDIFFARCCOUNT|LIKELYDIFFPORT,
												 LIKELYDIFFARCCOUNT|               LIKELYDIFFNODETYPE,
																	LIKELYDIFFPORT|LIKELYDIFFNODETYPE,

			LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT,
			LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|                   LIKELYDIFFPORT,
			LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|                                  LIKELYDIFFNODETYPE,
			LIKELYARCSSAMEDIR|                   LIKELYDIFFARCCOUNT|LIKELYDIFFPORT,
			LIKELYARCSSAMEDIR|                   LIKELYDIFFARCCOUNT|               LIKELYDIFFNODETYPE,
			LIKELYARCSSAMEDIR|                                      LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
							  LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|LIKELYDIFFPORT,
							  LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|               LIKELYDIFFNODETYPE,
							  LIKELYDIFFNODESIZE|                   LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
												 LIKELYDIFFARCCOUNT|LIKELYDIFFPORT|LIKELYDIFFNODETYPE,

			LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|LIKELYDIFFPORT,
			LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|               LIKELYDIFFNODETYPE,
			LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|                   LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
			LIKELYARCSSAMEDIR|                   LIKELYDIFFARCCOUNT|LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
							  LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|LIKELYDIFFPORT|LIKELYDIFFNODETYPE,

			LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|LIKELYDIFFPORT|LIKELYDIFFNODETYPE
		};

		protected MimicStitchJob(Connection conn1, Connection conn2, double oWidth, ArcProto oProto,
                                 double prefX, double prefY, boolean forced, Highlighter highlighter)
		{
			super("Mimic-Stitch", Routing.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.conn1 = conn1;
			this.conn2 = conn2;
			this.oWidth = oWidth;
			this.oProto = oProto;
			this.prefX = prefX;
			this.prefY = prefY;
			this.forced = forced;
            allRoutes = new ArrayList();
            this.highlighter = highlighter;
            setReportExecutionFlag(true);
			startJob();
		}

		public boolean doIt()
		{
			portMark = new HashSet();
			int result = mimic();
			return true;
		}

		private int mimic()
		{
			if (forced) System.out.println("Mimicing last arc...");

			PortInst [] endPi = new PortInst[2];
			endPi[0] = conn1.getPortInst();   endPi[1] = conn2.getPortInst();
			Point2D [] endPts = new Point2D[2];
			endPts[0] = conn1.getLocation();   endPts[1] = conn2.getLocation();

			Cell cell = endPi[0].getNodeInst().getParent();
			Netlist netlist = cell.getUserNetlist();

			// get options
			boolean mimicInteractive = Routing.isMimicStitchInteractive();
			boolean matchPorts = Routing.isMimicStitchMatchPorts();
			boolean matchArcCount = Routing.isMimicStitchMatchNumArcs();
			boolean matchNodeType = Routing.isMimicStitchMatchNodeType();
			boolean matchNodeSize = Routing.isMimicStitchMatchNodeSize();
			boolean noOtherArcsThisDir = Routing.isMimicStitchNoOtherArcsSameDir();

			// initialize total of arcs placed
			int count = 0;

			// make list of possible arc connections
			List possibleArcs = new ArrayList();

			// count the number of other arcs on the ends
			int con1 = endPi[0].getNodeInst().getNumConnections() + endPi[1].getNodeInst().getNumConnections() - 2;

			// precompute information about every port in the cell
			int total = 0;
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				for(Iterator pIt = ni.getProto().getPorts(); pIt.hasNext(); )
				{
					PortProto pp = (PortProto)pIt.next();
					if (!pp.connectsTo(oProto)) continue;
					portMark.add(pp);
					total++;
				}
			}
			if (total == 0) return count;

			Poly [] ro_mimicportpolys = new Poly[total];
			int i = 0;
			HashMap nodeIndex = new HashMap();
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				nodeIndex.put(ni, new Integer(i));
				for(Iterator pIt = ni.getProto().getPorts(); pIt.hasNext(); )
				{
					PortProto pp = (PortProto)pIt.next();
					if (!portMark.contains(pp)) continue;
					ro_mimicportpolys[i] = ni.getShapeOfPort(pp);
					i++;
				}
			}

			// search from both ends
			for(int end=0; end<2; end++)
			{
				PortInst pi0 = endPi[end];
				PortInst pi1 = endPi[1-end];
				NodeInst node0 = pi0.getNodeInst();
				NodeInst node1 = pi1.getNodeInst();
				PortProto port0 = pi0.getPortProto();
				PortProto port1 = pi1.getPortProto();
				Point2D pt0 = endPts[end];
				Point2D pt1 = endPts[1-end];
				double dist = pt0.distance(pt1);
				int angle = 0;
				if (dist != 0) angle = DBMath.figureAngle(pt0, pt1);
				boolean useFAngle = false;
				double angleRadians = 0;
				if ((angle%900) != 0)
				{
					angleRadians = DBMath.figureAngleRadians(pt0, pt1);
					useFAngle = true;
				}
				Poly port0Poly = pi0.getPoly();   // node0.getShapeOfPort(port0);
				double end0offx = pt0.getX() - port0Poly.getCenterX();
				double end0offy = pt0.getY() - port0Poly.getCenterY();

				SizeOffset so0 = node0.getSizeOffset();
				double node0Wid = node0.getXSize() - so0.getLowXOffset() - so0.getHighXOffset();
				double node0Hei = node0.getYSize() - so0.getLowYOffset() - so0.getHighYOffset();

				SizeOffset so1 = node1.getSizeOffset();
				double node1Wid = node1.getXSize() - so1.getLowXOffset() - so1.getHighXOffset();
				double node1Hei = node1.getYSize() - so1.getLowYOffset() - so1.getHighYOffset();

				// now search every node in the cell
				for(Iterator it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					Rectangle2D bounds = ni.getBounds();

					// now look for another node that matches the situation
					for(Iterator oIt = cell.getNodes(); oIt.hasNext(); )
					{
						NodeInst oNi = (NodeInst)oIt.next();
						Rectangle2D oBounds = ni.getBounds();

						// ensure that intra-node wirings stay that way
						if (node0 == node1)
						{
							if (ni != oNi) continue;
						} else
						{
							if (ni == oNi) continue;
						}

						// make sure the distances are sensible
						if (bounds.getMinX() - oBounds.getMaxX() > dist) continue;
						if (oBounds.getMinX() - bounds.getMaxX() > dist) continue;
						if (bounds.getMinY() - oBounds.getMaxY() > dist) continue;
						if (oBounds.getMinY() - bounds.getMaxY() > dist) continue;

						// compare each port
						int portPos = ((Integer)nodeIndex.get(ni)).intValue();
						for(Iterator pIt = ni.getProto().getPorts(); pIt.hasNext(); )
						{
							PortProto pp = (PortProto)pIt.next();

							// make sure the arc can connect
							if (!portMark.contains(pp)) continue;

							Poly poly = ro_mimicportpolys[portPos++];
							double x0 = poly.getCenterX();
							double y0 = poly.getCenterY();
							x0 += end0offx;   y0 += end0offy;
							double wantX1 = x0;
							double wantY1 = y0;
							if (dist != 0)
							{
								if (useFAngle)
								{
									wantX1 = x0 + Math.cos(angleRadians) * dist;
									wantY1 = y0 + Math.sin(angleRadians) * dist;
								} else
								{
									wantX1 = x0 + DBMath.cos(angle) * dist;
									wantY1 = y0 + DBMath.sin(angle) * dist;
								}
							}
							Point2D xy0 = new Point2D.Double(x0, y0);
							Point2D want1 = new Point2D.Double(wantX1, wantY1);

							int oPortPos = ((Integer)nodeIndex.get(oNi)).intValue();
							for(Iterator oPIt = oNi.getProto().getPorts(); oPIt.hasNext(); )
							{
								PortProto opp = (PortProto)oPIt.next();

								// make sure the arc can connect
								if (!portMark.contains(pp)) continue;
								Poly thisPoly = ro_mimicportpolys[oPortPos++];

								// don't replicate what is already done
								if (ni == node0 && pp == port0 && oNi == node1 && opp == port1) continue;

								// see if they are the same distance apart
								boolean ptInPoly = thisPoly.isInside(want1);
								if (!ptInPoly) continue;

								// figure out the wiring situation here
								int situation = 0;

								// see if there are already wires going in this direction
								int desiredAngle = -1;
								if (x0 != wantX1 || y0 != wantY1)
									desiredAngle = DBMath.figureAngle(xy0, want1);
								PortInst piNet0 = null;
								for(Iterator pII = ni.getConnections(); pII.hasNext(); )
								{
									Connection con = (Connection)pII.next();
									PortInst pi = con.getPortInst();
									if (pi.getPortProto() != pp) continue;
									ArcInst oai = con.getArc();
									piNet0 = pi;
									if (desiredAngle < 0)
									{
										if (oai.getHead().getLocation().getX() == oai.getTail().getLocation().getX() &&
											oai.getHead().getLocation().getY() == oai.getTail().getLocation().getY())
										{
											situation |= LIKELYARCSSAMEDIR;
											break;
										}
									} else
									{
										if (oai.getHead().getLocation().getX() == oai.getTail().getLocation().getX() &&
											oai.getHead().getLocation().getY() == oai.getTail().getLocation().getY())
												continue;
										int thisend = 0;
										if (oai.getTail().getPortInst() == pi) thisend = 1;
										int existingAngle = DBMath.figureAngle(oai.getConnection(thisend).getLocation(),
											oai.getConnection(1-thisend).getLocation());
										if (existingAngle == desiredAngle)
										{
											situation |= LIKELYARCSSAMEDIR;
											break;
										}
									}
								}

								desiredAngle = -1;
								if (x0 != wantX1 || y0 != wantY1)
									desiredAngle = DBMath.figureAngle(want1, xy0);
								PortInst piNet1 = null;
								for(Iterator pII = oNi.getConnections(); pII.hasNext(); )
								{
									Connection con = (Connection)pII.next();
									PortInst pi = con.getPortInst();
									if (pi.getPortProto() != opp) continue;
									ArcInst oai = con.getArc();
									piNet1 = pi;
									if (desiredAngle < 0)
									{
										if (oai.getHead().getLocation().getX() == oai.getTail().getLocation().getX() &&
											oai.getHead().getLocation().getY() == oai.getTail().getLocation().getY())
										{
											situation |= LIKELYARCSSAMEDIR;
											break;
										}
									} else
									{
										if (oai.getHead().getLocation().getX() == oai.getTail().getLocation().getX() &&
											oai.getHead().getLocation().getY() == oai.getTail().getLocation().getY())
												continue;
										int thisend = 0;
										if (oai.getTail().getPortInst() == pi) thisend = 1;
										int existingAngle = DBMath.figureAngle(oai.getConnection(thisend).getLocation(),
											oai.getConnection(1-thisend).getLocation());
										if (existingAngle == desiredAngle)
										{
											situation |= LIKELYARCSSAMEDIR;
											break;
										}
									}
								}

								// if there is a network that already connects these, ignore
								if (piNet0 != null && piNet1 != null)
								{
									if (netlist.sameNetwork(piNet0.getNodeInst(), piNet0.getPortProto(),
										piNet1.getNodeInst(), piNet1.getPortProto())) continue;
								}

								if (pp != port0 || opp != port1)
									situation |= LIKELYDIFFPORT;
								int con2 = ni.getNumConnections() + oNi.getNumConnections();
								if (con1 != con2) situation |= LIKELYDIFFARCCOUNT;
								if (ni.getProto() != node0.getProto() || oNi.getProto() != node1.getProto())
									situation |= LIKELYDIFFNODETYPE;

								SizeOffset so = ni.getSizeOffset();
								double wid = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
								double hei = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
								if (wid != node0Wid || hei != node0Hei) situation |= LIKELYDIFFNODESIZE;
								so = oNi.getSizeOffset();
								wid = oNi.getXSize() - so.getLowXOffset() - so.getHighXOffset();
								hei = oNi.getYSize() - so.getLowYOffset() - so.getHighYOffset();
								if (wid != node1Wid || hei != node1Hei) situation |= LIKELYDIFFNODESIZE;

								// see if this combination has already been considered
								PossibleArc found = null;
								for(Iterator paIt = possibleArcs.iterator(); paIt.hasNext(); )
								{
									PossibleArc pa = (PossibleArc)paIt.next();
									if (pa.ni1 == ni && pa.pp1 == pp && pa.ni2 == oNi && pa.pp2 == opp)
									{ found = pa;   break; }
									if (pa.ni2 == ni && pa.pp2 == pp && pa.ni1 == oNi && pa.pp1 == opp)
									{ found = pa;   break; }
								}
								if (found != null)
								{
									if (found.situation == situation) continue;
									int foundIndex = -1;
									for(int k=0; k<situations.length; k++)
									{
										if (found.situation == situations[k]) break;
										if (situation == situations[k]) { foundIndex = k;   break; }
									}
									if (foundIndex >= 0 && found.situation == situations[foundIndex])
										continue;
								}
								if (found == null)
								{
									found = new PossibleArc();
									possibleArcs.add(found);
								}
								found.ni1 = ni;      found.pp1 = pp;    found.pt1 = xy0;
								found.ni2 = oNi;     found.pp2 = opp;   found.pt2 = want1;
								found.situation = situation;
							}
						}
					}
				}
			}

			// now create the mimiced arcs
			int ifIgnorePorts = 0, ifIgnoreArcCount = 0, ifIgnoreNodeType = 0,
				ifIgnoreNodeSize = 0, ifIgnoreOtherSameDir = 0;
			boolean flushStructureChanges = false;
			for(int j=0; j<situations.length; j++)
			{
				// see if this situation is possible
				total = 0;
				for(Iterator it = possibleArcs.iterator(); it.hasNext(); )
				{
					PossibleArc pa = (PossibleArc)it.next();
					if (pa.situation == situations[j]) total++;
				}
				if (total == 0) continue;

				// see if this situation is desired
				boolean stopWhenDone = false;
				if (!mimicInteractive)
				{
					// make sure this situation is the desired one
					if (matchPorts && (situations[j]&LIKELYDIFFPORT) != 0)
					{
						ifIgnorePorts += total;
						continue;
					}
					if (matchArcCount && (situations[j]&LIKELYDIFFARCCOUNT) != 0)
					{
						ifIgnoreArcCount += total;
						continue;
					}
					if (matchNodeType && (situations[j]&LIKELYDIFFNODETYPE) != 0)
					{
						ifIgnoreNodeType += total;
						continue;
					}
					if (matchNodeSize && (situations[j]&LIKELYDIFFNODESIZE) != 0)
					{
						ifIgnoreNodeSize += total;
						continue;
					}
					if (noOtherArcsThisDir && (situations[j]&LIKELYARCSSAMEDIR) != 0)
					{
						ifIgnoreOtherSameDir += total;
						continue;
					}
				} else
				{
					// save what is highlighted
					List saveHighlights = new ArrayList();
                    for(Iterator it = highlighter.getHighlights().iterator(); it.hasNext(); )
                        saveHighlights.add(it.next());

                    // show the wires to be created
                    highlighter.clear();
					for(Iterator it = possibleArcs.iterator(); it.hasNext(); )
					{
						PossibleArc pa = (PossibleArc)it.next();
						if (pa.situation != situations[j]) continue;
						if (pa.pt1.getX() == pa.pt2.getX() && pa.pt1.getY() == pa.pt2.getY())
						{
							Rectangle2D pointRect = new Rectangle2D.Double(pa.pt1.getX()-1, pa.pt1.getY()-1, 2, 2);
							highlighter.addArea(pointRect, cell);
						} else
						{
							highlighter.addLine(pa.pt1, pa.pt2, cell);
						}
					}
					highlighter.finished();
					if (flushStructureChanges)
					{
						EditWindow.repaintAllContents();
					}
					String [] options = {"Yes", "No", "No, and stop", "Yes, then stop"};
					int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
						"Create " + total + " wires shown here?",
						"Create wires", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
						null, options, options[1]);

					// restore highlighting
					highlighter.clear();
					highlighter.setHighlightList(saveHighlights);
					highlighter.finished();
					if (ret == 1) continue;
					if (ret == 2) break;
					if (ret == 3) stopWhenDone = true;
				}

				// make the wires
                allRoutes.clear();
				for(Iterator it = possibleArcs.iterator(); it.hasNext(); )
				{
					PossibleArc pa = (PossibleArc)it.next();
					if (pa.situation != situations[j]) continue;

					Poly portPoly1 = pa.ni1.getShapeOfPort(pa.pp1);
					Poly portPoly2 = pa.ni2.getShapeOfPort(pa.pp2);
					Point2D bend = new Point2D.Double((portPoly1.getCenterX() + portPoly2.getCenterX()) / 2 + prefX,
						(portPoly1.getCenterY() + portPoly2.getCenterY()) / 2 + prefY);
					//List added = WiringListener.makeConnection(pa.ni1, pa.pp1, pa.ni2, pa.pp2, bend, false, false);
                    PortInst pi1 = pa.ni1.findPortInstFromProto(pa.pp1);
                    PortInst pi2 = pa.ni2.findPortInstFromProto(pa.pp2);
                    Route route = router.planRoute(pa.ni1.getParent(), pi1, pi2, bend);
                    if (route.size() == 0)
					{
						System.out.println("Problem creating arc");
						continue;
					}
                    allRoutes.add(route);
                    //Router.createRouteNoJob(route, pa.ni1.getParent(), false);
					flushStructureChanges = true;
					count++;
				}

                // create the routes
                for (Iterator it = allRoutes.iterator(); it.hasNext(); ) {
                    Route route = (Route)it.next();
                    RouteElement re = (RouteElement)route.get(0);
                    Cell c = re.getCell();
                    Router.createRouteNoJob(route, c, false, false, highlighter);
                }

				// stop now if requested
				if (stopWhenDone) break;
			}

			if (count != 0)
			{
				System.out.println("MIMIC ROUTING: Created " + count + " arcs");
			} else
			{
				if (forced)
				{
					String msg = "No wires added";
					if (ifIgnorePorts != 0)
						msg += ", might have added " + ifIgnorePorts + " wires if 'ports must match' were off";
					if (ifIgnoreArcCount != 0)
						msg += ", might have added " + ifIgnoreArcCount + " wires if 'number of existing arcs must match' were off";
					if (ifIgnoreNodeType != 0)
						msg += ", might have added " + ifIgnoreNodeType + " wires if 'node types must match' were off";
					if (ifIgnoreNodeSize != 0)
						msg += ", might have added " + ifIgnoreNodeSize + " wires if 'nodes sizes must match' were off";
					if (ifIgnoreOtherSameDir != 0)
						msg += ", might have added " + ifIgnoreOtherSameDir + " wires if 'cannot have other arcs in the same direction' were off";
					System.out.println(msg);
					if (ifIgnorePorts + ifIgnoreArcCount + ifIgnoreNodeType + ifIgnoreNodeSize + ifIgnoreOtherSameDir != 0)
						System.out.println(" (settings are in the Tools / Routing tab of the Preferences)");
				}
			}
			return count;
		}

	}

}

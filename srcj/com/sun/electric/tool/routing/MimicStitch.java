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
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * This is the Mimic Stitching tool.
 */
public class MimicStitch
{
	/** router to use */            private static InteractiveRouter router = new SimpleWirer();

	private static final int LIKELYDIFFPORT     =  1;
	private static final int LIKELYDIFFARCCOUNT =  2;
	private static final int LIKELYDIFFNODETYPE =  4;
	private static final int LIKELYDIFFNODESIZE =  8;
	private static final int LIKELYARCSSAMEDIR  = 16;

	private static final int situations[] = {
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

	private static class PossibleArc
	{
		private int       situation;
		private NodeInst  ni1, ni2;
		private PortProto pp1, pp2;
		private Point2D   pt1, pt2;
	};

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
			ArcProto proto = lastActivity.createdArcs[0].getProto();
			Cell parent = lastActivity.createdArcs[0].getParent();
			HashSet gotOne = new HashSet();
			HashSet gotMany = new HashSet();
			for(int i=0; i<lastActivity.numCreatedArcs; i++)
			{
				ArcInst ai = lastActivity.createdArcs[i];
				for(int e=0; e<2; e++)
				{
					NodeInst ni = ai.getConnection(e).getPortInst().getNodeInst();
					if (!gotMany.contains(ni))
					{
						if (!gotOne.contains(ni)) gotOne.add(ni); else
						{
							gotOne.remove(ni);
							gotMany.add(ni);
						}
					}
				}
			}
			int foundEnds = 0;
			Connection [] ends = new Connection[2];
			double width = 0;
			for(int i=0; i<lastActivity.numCreatedArcs; i++)
			{
				ArcInst ai = lastActivity.createdArcs[i];
				for(int e=0; e<2; e++)
				{
					NodeInst ni = ai.getConnection(e).getPortInst().getNodeInst();
					if (!gotOne.contains(ni)) continue;
					if (foundEnds < 2)
					{
						ends[foundEnds] = ai.getConnection(e);
						if (ai.getWidth() > width)
							width = ai.getWidth();
					}
					foundEnds++;
				}
			}

			// if exactly two ends are found, mimic that connection
			if (foundEnds == 2)
			{
				double prefX = 0, prefY = 0;
				if (lastActivity.numCreatedNodes == 1)
				{
					Poly portPoly0 = ends[0].getPortInst().getPoly();
					double x0 = portPoly0.getCenterX();
					double y0 = portPoly0.getCenterY();
					Poly portPoly1 = ends[1].getPortInst().getPoly();
					double x1 = portPoly1.getCenterX();
					double y1 = portPoly1.getCenterY();
					prefX = lastActivity.createdNodes[0].getAnchorCenterX() - (x0+x1) / 2;
					prefY = lastActivity.createdNodes[0].getAnchorCenterY() - (y0+y1) / 2;
				} else if (lastActivity.numCreatedNodes == 2)
				{
					Poly portPoly0 = ends[0].getPortInst().getPoly();
					double x0 = portPoly0.getCenterX();
					double y0 = portPoly0.getCenterY();
					Poly portPoly1 = ends[1].getPortInst().getPoly();
					double x1 = portPoly1.getCenterX();
					double y1 = portPoly1.getCenterY();
					prefX = (lastActivity.createdNodes[0].getAnchorCenterX() +
						lastActivity.createdNodes[1].getAnchorCenterX()) / 2 - (x0+x1) / 2;
					prefY = (lastActivity.createdNodes[0].getAnchorCenterY() +
						lastActivity.createdNodes[1].getAnchorCenterY()) / 2 - (y0+y1) / 2;
				}
				MimicStitchJob job = new MimicStitchJob(ends[0], ends[1], width, proto, prefX, prefY, forced, highlighter);
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
			double thisDist = end0.distance(end1);
			if (dist != thisDist) continue;
			if (dist != 0)
			{
				int thisAngle = DBMath.figureAngle(end0, end1);
				if ((angle%1800) != (thisAngle%1800)) continue;
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
		private List arcKills;

		private MimicUnstitchJob(List arcKills)
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
	 * Class to examine a circuit and find mimic opportunities in a new thread.
	 */
	private static class MimicStitchJob extends Job
	{
		private Connection conn1, conn2;
		private double oWidth;
		private ArcProto oProto;
		private double prefX, prefY;
		private boolean forced;
		private Highlighter highlighter;

		private MimicStitchJob(Connection conn1, Connection conn2, double oWidth, ArcProto oProto,
								double prefX, double prefY, boolean forced, Highlighter highlighter)
		{
			super("Mimic-Stitch", Routing.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.conn1 = conn1;
			this.conn2 = conn2;
			this.oWidth = oWidth;
			this.oProto = oProto;
			this.prefX = prefX;
			this.prefY = prefY;
			this.forced = forced;
			this.highlighter = highlighter;
			setReportExecutionFlag(true);
			startJob();
		}

		public boolean doIt()
		{
			// get options
			boolean mimicInteractive = Routing.isMimicStitchInteractive();
			boolean matchPorts = Routing.isMimicStitchMatchPorts();
			boolean matchArcCount = Routing.isMimicStitchMatchNumArcs();
			boolean matchNodeType = Routing.isMimicStitchMatchNodeType();
			boolean matchNodeSize = Routing.isMimicStitchMatchNodeSize();
			boolean noOtherArcsThisDir = Routing.isMimicStitchNoOtherArcsSameDir();

			mimicOneArc(conn1, conn2, oWidth, oProto, prefX, prefY, forced, highlighter, Job.Type.EXAMINE,
				mimicInteractive, matchPorts, matchArcCount, matchNodeType, matchNodeSize, noOtherArcsThisDir);
			return true;
		}
	}

	/**
	 * Method to do mimic stitching.
	 * It can be used during batch processing to mimic directly.
	 * @param conn1 the connection at one end of the mimic.
	 * @param conn2 the connection at the other end of the mimic.
	 * @param oWidth the width of the arc to run.
	 * @param oProto the type of arc to run.
	 * @param prefX the preferred X position of the mimic (if there is a choice).
	 * @param prefY the preferred Y position of the mimic (if there is a choice).
	 * @param forced true if this was an explicitly requested mimic.
	 * @param highlighter the highlighter to use for highlighting the results.
	 * @param method the type of job that is running (CHANGE or EXAMINE).
	 * @param mimicInteractive true to run interactively.
	 * @param matchPorts true to require port types to match.
	 * @param matchArcCount true to require the number of arcs to match.
	 * @param matchNodeType true to require the node types to match.
	 * @param matchNodeSize true to require the node sizes to match.
	 * @param noOtherArcsThisDir true to require that no other arcs exist in the same direction.
	 */
	public static void mimicOneArc(Connection conn1, Connection conn2, double oWidth, ArcProto oProto, double prefX, double prefY,
			boolean forced, Highlighter highlighter, Job.Type method,
			boolean mimicInteractive, boolean matchPorts, boolean matchArcCount, boolean matchNodeType, boolean matchNodeSize, boolean noOtherArcsThisDir)
	{
		if (forced) System.out.println("Mimicing last arc...");

		PortInst [] endPi = new PortInst[2];
		endPi[0] = conn1.getPortInst();   endPi[1] = conn2.getPortInst();
		Point2D [] endPts = new Point2D[2];
		endPts[0] = conn1.getLocation();   endPts[1] = conn2.getLocation();

		Cell cell = endPi[0].getNodeInst().getParent();
//		Netlist netlist = cell.getUserNetlist();
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted mimic-routing (network information unavailable).  Please try again");
			return;
		}

		// make list of possible arc connections
		List possibleArcs = new ArrayList();

		// count the number of other arcs on the ends
		int con1 = endPi[0].getNodeInst().getNumConnections() + endPi[1].getNodeInst().getNumConnections() - 2;

		// precompute polygon information about every port in the cell
		HashMap cachedPortPoly = new HashMap();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			for(Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
			{
				PortInst pi = (PortInst)pIt.next();
				if (!pi.getPortProto().connectsTo(oProto)) continue;
				cachedPortPoly.put(pi, pi.getPoly());
			}
		}
		if (cachedPortPoly.size() == 0) return;

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
			double distX = pt1.getX() - pt0.getX();
			double distY = pt1.getY() - pt0.getY();
			int angle = 0;
			if (dist != 0) angle = DBMath.figureAngle(pt0, pt1);
			boolean useFAngle = false;
			double angleRadians = 0;
			if ((angle%900) != 0)
			{
				angleRadians = DBMath.figureAngleRadians(pt0, pt1);
				useFAngle = true;
			}
			Poly port0Poly = pi0.getPoly();
			double end0Offx = pt0.getX() - port0Poly.getCenterX();
			double end0Offy = pt0.getY() - port0Poly.getCenterY();

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
					Rectangle2D oBounds = oNi.getBounds();

					// ensure that intra-node wirings stay that way
					if (node0 == node1)
					{
						if (ni != oNi) continue;
					} else
					{
						if (ni == oNi) continue;
					}

					// make sure the distances are sensible
					if (distX > 0)
					{
						if (bounds.getMaxX() + distX < oBounds.getMinX()) continue;
						if (bounds.getMinX() + distX > oBounds.getMaxX()) continue;
					} else
					{
						if (bounds.getMinX() + distX > oBounds.getMaxX()) continue;
						if (bounds.getMaxX() + distX < oBounds.getMinX()) continue;
					}
					if (distY > 0)
					{
						if (bounds.getMaxY() + distY < oBounds.getMinY()) continue;
						if (bounds.getMinY() + distY > oBounds.getMaxY()) continue;
					} else
					{
						if (bounds.getMinY() + distY > oBounds.getMaxY()) continue;
						if (bounds.getMaxY() + distY < oBounds.getMinY()) continue;
					}

					// compare each port
					for(Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
					{
						PortInst pi = (PortInst)pIt.next();
						PortProto pp = pi.getPortProto();

						// if this port is not cached, it cannot connect, so ignore it
						Poly poly = (Poly)cachedPortPoly.get(pi);
						if (poly == null) continue;

						double x0 = poly.getCenterX();
						double y0 = poly.getCenterY();
						x0 += end0Offx;   y0 += end0Offy;
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

						for(Iterator oPIt = oNi.getPortInsts(); oPIt.hasNext(); )
						{
							PortInst oPi = (PortInst)oPIt.next();
							PortProto oPp = oPi.getPortProto();

							// if this port is not cached, it cannot connect, so ignore it
							Poly thisPoly = (Poly)cachedPortPoly.get(oPi);
							if (thisPoly == null) continue;

							// don't replicate what is already done
							if (pi == pi0 && oPi == pi1) continue;

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
								PortInst aPi = con.getPortInst();
								if (aPi.getPortProto() != pp) continue;
								ArcInst oAi = con.getArc();
								piNet0 = aPi;
								if (desiredAngle < 0)
								{
									if (oAi.getHead().getLocation().getX() == oAi.getTail().getLocation().getX() &&
										oAi.getHead().getLocation().getY() == oAi.getTail().getLocation().getY())
									{
										situation |= LIKELYARCSSAMEDIR;
										break;
									}
								} else
								{
									if (oAi.getHead().getLocation().getX() == oAi.getTail().getLocation().getX() &&
										oAi.getHead().getLocation().getY() == oAi.getTail().getLocation().getY())
											continue;
									int thisend = 0;
									if (oAi.getTail().getPortInst() == aPi) thisend = 1;
									int existingAngle = DBMath.figureAngle(oAi.getConnection(thisend).getLocation(),
										oAi.getConnection(1-thisend).getLocation());
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
								PortInst aPi = con.getPortInst();
								if (aPi.getPortProto() != oPp) continue;
								ArcInst oAi = con.getArc();
								piNet1 = aPi;
								if (desiredAngle < 0)
								{
									if (oAi.getHead().getLocation().getX() == oAi.getTail().getLocation().getX() &&
										oAi.getHead().getLocation().getY() == oAi.getTail().getLocation().getY())
									{
										situation |= LIKELYARCSSAMEDIR;
										break;
									}
								} else
								{
									if (oAi.getHead().getLocation().getX() == oAi.getTail().getLocation().getX() &&
										oAi.getHead().getLocation().getY() == oAi.getTail().getLocation().getY())
											continue;
									int thisend = 0;
									if (oAi.getTail().getPortInst() == aPi) thisend = 1;
									int existingAngle = DBMath.figureAngle(oAi.getConnection(thisend).getLocation(),
											oAi.getConnection(1-thisend).getLocation());
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

							if (pp != port0 || oPp != port1)
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
								if (pa.ni1 == ni && pa.pp1 == pp && pa.ni2 == oNi && pa.pp2 == oPp)
								{ found = pa;   break; }
								if (pa.ni2 == ni && pa.pp2 == pp && pa.ni1 == oNi && pa.pp1 == oPp)
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
							found.ni2 = oNi;     found.pp2 = oPp;   found.pt2 = want1;
							found.situation = situation;
						}
					}
				}
			}
		}

		// now create the mimiced arcs
		if (mimicInteractive)
		{
			// do this in a separate thread so that this examine job can finish
			MimicInteractive task = new MimicInteractive(cell, possibleArcs, prefX, prefY, highlighter);
			SwingUtilities.invokeLater(task);
		} else
		{
			// not interactive: follow rules in the Preferences
			int ifIgnorePorts = 0, ifIgnoreArcCount = 0, ifIgnoreNodeType = 0,
				ifIgnoreNodeSize = 0, ifIgnoreOtherSameDir = 0;
			int count = 0;
			for(int j=0; j<situations.length; j++)
			{
				// see if this situation is possible
				List allRoutes = new ArrayList();
				for(Iterator it = possibleArcs.iterator(); it.hasNext(); )
				{
					PossibleArc pa = (PossibleArc)it.next();
					if (pa.situation != situations[j]) continue;

					Poly portPoly1 = pa.ni1.getShapeOfPort(pa.pp1);
					Poly portPoly2 = pa.ni2.getShapeOfPort(pa.pp2);
					Point2D bend = new Point2D.Double((portPoly1.getCenterX() + portPoly2.getCenterX()) / 2 + prefX,
						(portPoly1.getCenterY() + portPoly2.getCenterY()) / 2 + prefY);
					PortInst pi1 = pa.ni1.findPortInstFromProto(pa.pp1);
					PortInst pi2 = pa.ni2.findPortInstFromProto(pa.pp2);
					Route route = router.planRoute(pa.ni1.getParent(), pi1, pi2, bend, null);
					if (route.size() == 0)
					{
						System.out.println("Problem creating arc");
						continue;
					}
					allRoutes.add(route);
				}
				int total = allRoutes.size();
				if (total == 0) continue;

				// make sure this situation is the desired one
				if (matchPorts && (situations[j]&LIKELYDIFFPORT) != 0) { ifIgnorePorts += total;   continue; }
				if (matchArcCount && (situations[j]&LIKELYDIFFARCCOUNT) != 0) { ifIgnoreArcCount += total;   continue; }
				if (matchNodeType && (situations[j]&LIKELYDIFFNODETYPE) != 0) { ifIgnoreNodeType += total;   continue; }
				if (matchNodeSize && (situations[j]&LIKELYDIFFNODESIZE) != 0) { ifIgnoreNodeSize += total;   continue; }
				if (noOtherArcsThisDir && (situations[j]&LIKELYARCSSAMEDIR) != 0) { ifIgnoreOtherSameDir += total;   continue; }

				// create the routes
				if (method == Job.Type.EXAMINE)
				{
					// since this is an examine job, queue a change job to make the wires
					MimicWireJob job = new MimicWireJob(allRoutes, highlighter, false);
				} else
				{
					// since this is a change job, do the wires now
					runTheWires(allRoutes, highlighter, false);
				}
				count += total;
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
		}
	}

	private static void runTheWires(List allRoutes, Highlighter highlighter, boolean redisplay)
	{
		// create the routes
		for (Iterator it = allRoutes.iterator(); it.hasNext(); )
		{
			Route route = (Route)it.next();
			RouteElement re = (RouteElement)route.get(0);
			Cell c = re.getCell();
			Router.createRouteNoJob(route, c, false, false, highlighter);
		}
		if (redisplay) EditWindow.repaintAllContents();
	}

	/**
	 * Class to implement actual wire creation in a new thread.
	 */
	private static class MimicWireJob extends Job
	{
		private List allRoutes;
		private Highlighter highlighter;
		private boolean redisplay;

		private MimicWireJob(List allRoutes, Highlighter highlighter, boolean redisplay)
		{
			super("Mimic-Stitch", Routing.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.allRoutes = allRoutes;
			this.highlighter = highlighter;
			this.redisplay = redisplay;
			startJob();
		}

		public boolean doIt()
		{
			runTheWires(allRoutes, highlighter, redisplay);
			return true;
		}
	}

	/****************************** INTERACTIVE MIMIC SELECTION ******************************/

	/**
	 * Class to present the next mimic opportunity and let the user choose whether to do it.
	 * This class runs in the Swing thread.
	 */
	private static class MimicInteractive implements Runnable
	{
		private Cell cell;
		private List possibleArcs;
		private double prefX, prefY;
		private Highlighter highlighter;

		private MimicInteractive(Cell cell, List possibleArcs, double prefX, double prefY, Highlighter highlighter)
	    {
	    	this.cell = cell;
	    	this.possibleArcs = possibleArcs;
	    	this.prefX = prefX;
	    	this.prefY = prefY;
	    	this.highlighter = highlighter;
	    }

	    public void run()
	    {
			// interactive mode: show paths before creating arcs
	    	presentNextSituation(0, 0, possibleArcs, cell, highlighter, prefX, prefY);
	    }
	}

	/**
	 * Method to interactively present a mimicing situation to the user.
	 * @param count the number of arcs created so far.
	 * @param situationNumber the starting "situation" number (class of mimics that are allowed).
	 * @param possibleArcs a list of possible arcs to route
	 * @param cell the Cell where routing is going on.
	 * @param highlighter the highlighter for the window.
	 * @param prefX preferred X coordinate when arcs bend.
	 * @param prefY preferred Y coordinate when arcs bend.
	 */
	private static void presentNextSituation(int count, int situationNumber, List possibleArcs, Cell cell, Highlighter highlighter, double prefX, double prefY)
	{
		// find the next situation
 		for(int j=situationNumber; j<situations.length; j++)
		{
			// make a list of mimics that match the situation
			List allRoutes = new ArrayList();
			for(Iterator it = possibleArcs.iterator(); it.hasNext(); )
			{
				PossibleArc pa = (PossibleArc)it.next();
				if (pa.situation != situations[j]) continue;

				Poly portPoly1 = pa.ni1.getShapeOfPort(pa.pp1);
				Poly portPoly2 = pa.ni2.getShapeOfPort(pa.pp2);
				Point2D bend = new Point2D.Double((portPoly1.getCenterX() + portPoly2.getCenterX()) / 2 + prefX,
					(portPoly1.getCenterY() + portPoly2.getCenterY()) / 2 + prefY);
				PortInst pi1 = pa.ni1.findPortInstFromProto(pa.pp1);
				PortInst pi2 = pa.ni2.findPortInstFromProto(pa.pp2);
				Route route = router.planRoute(pa.ni1.getParent(), pi1, pi2, bend, null);
				if (route.size() == 0)
				{
					System.out.println("Problem creating arc");
					continue;
				}
				allRoutes.add(route);
			}
			if (allRoutes.size() == 0) continue;

			// save what is highlighted
			List saveHighlights = new ArrayList();
			for(Iterator it = highlighter.getHighlights().iterator(); it.hasNext(); )
				saveHighlights.add(it.next());

			// show the wires to be created
			highlighter.clear();
			for(Iterator it = allRoutes.iterator(); it.hasNext(); )
			{
				Route route = (Route)it.next();
				double fX = route.getStart().getLocation().getX();
				double fY = route.getStart().getLocation().getY();
				double tX = route.getEnd().getLocation().getX();
				double tY = route.getEnd().getLocation().getY();
				if (fX == tX && fY == tY)
				{
					Rectangle2D pointRect = new Rectangle2D.Double(fX-1, fY-1, 2, 2);
					highlighter.addArea(pointRect, cell);
				} else
				{
					highlighter.addLine(route.getStart().getLocation(), route.getEnd().getLocation(), cell);
				}
			}
			highlighter.finished();

			// ask if the user wants to do it
			MimicDialog md = new MimicDialog(TopLevel.getCurrentJFrame(), count, allRoutes, saveHighlights, highlighter, j+1, possibleArcs, cell, prefX, prefY);
			return;
		}
 
 		// done with all situations: report any arcs created
		if (count != 0)
			System.out.println("MIMIC ROUTING: Created " + count + " arcs");
	}

	/**
	 * Class to handle the "Interactive Mimic" dialog.
	 */
	private static class MimicDialog extends EDialog
	{
		private int count;
		private List allRoutes;
		private List saveHighlights;
		private Highlighter highlighter;
		private int nextSituationNumber;
		private List possibleArcs;
		private Cell cell;
		private double prefX, prefY;

		private MimicDialog(Frame parent, int count, List allRoutes, List saveHighlights, Highlighter highlighter, int nextSituationNumber,
			List possibleArcs, Cell cell, double prefX, double prefY)
		{
			super(parent, false);
			this.count = count;
			this.allRoutes = allRoutes;
			this.saveHighlights = saveHighlights;
			this.highlighter = highlighter;
			this.nextSituationNumber = nextSituationNumber;
			this.possibleArcs = possibleArcs;
			this.cell = cell;
			this.prefX = prefX;
			this.prefY = prefY;

	        getContentPane().setLayout(new GridBagLayout());
	        setTitle("Create wires?");
	        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

	        JLabel question = new JLabel("Create " + allRoutes.size() + " wires shown here?");
		    GridBagConstraints gbc = new GridBagConstraints();
		    gbc.gridx = 0;   gbc.gridy = 0;
		    gbc.gridwidth = 4;
		    gbc.fill = GridBagConstraints.HORIZONTAL;
		    gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(question, gbc);

	        JButton yes = new JButton("Yes");
	        yes.addActionListener(new ActionListener()
	        {
	            public void actionPerformed(ActionEvent evt) { yes(); }
	        });
	        gbc = new GridBagConstraints();
	        gbc.gridx = 0;   gbc.gridy = 1;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(yes, gbc);
	        getRootPane().setDefaultButton(yes);

	        JButton no = new JButton("No");
	        no.addActionListener(new ActionListener()
	        {
	            public void actionPerformed(ActionEvent evt) { no(); }
	        });
	        gbc = new GridBagConstraints();
	        gbc.gridx = 1;   gbc.gridy = 1;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(no, gbc);

	        JButton noAndStop = new JButton("No, and stop");
	        noAndStop.addActionListener(new ActionListener()
	        {
	            public void actionPerformed(ActionEvent evt) { noAndStopActionPerformed(); }
	        });
	        gbc = new GridBagConstraints();
	        gbc.gridx = 2;   gbc.gridy = 1;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(noAndStop, gbc);

	        JButton yesAndStop = new JButton("Yes, then stop");
	        yesAndStop.addActionListener(new ActionListener()
	        {
	            public void actionPerformed(ActionEvent evt) { yesAndStopActionPerformed(); }
	        });
	        gbc = new GridBagConstraints();
	        gbc.gridx = 3;   gbc.gridy = 1;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(yesAndStop, gbc);

	        pack();
	        finishInitialization();
	        setVisible(true);
		}

		protected void escapePressed() { no(); }

		private void yesAndStopActionPerformed()
		{
			highlighter.clear();
			highlighter.setHighlightList(saveHighlights);
			highlighter.finished();

			MimicWireJob job = new MimicWireJob(allRoutes, highlighter, true);
			count += allRoutes.size();
			presentNextSituation(count, situations.length, possibleArcs, cell, highlighter, prefX, prefY);

			setVisible(false);
			dispose();
		}

		private void noAndStopActionPerformed()
		{
			highlighter.clear();
			highlighter.setHighlightList(saveHighlights);
			highlighter.finished();

			presentNextSituation(count, situations.length, possibleArcs, cell, highlighter, prefX, prefY);

			setVisible(false);
			dispose();
		}

		private void yes()
		{
			highlighter.clear();
			highlighter.setHighlightList(saveHighlights);
			highlighter.finished();

			MimicWireJob job = new MimicWireJob(allRoutes, highlighter, true);
			count += allRoutes.size();
			presentNextSituation(count, nextSituationNumber, possibleArcs, cell, highlighter, prefX, prefY);

			setVisible(false);
			dispose();
		}

		private void no()
		{
			highlighter.clear();
			highlighter.setHighlightList(saveHighlights);
			highlighter.finished();

			presentNextSituation(count, nextSituationNumber, possibleArcs, cell, highlighter, prefX, prefY);

			setVisible(false);
			dispose();
		}
	}
}

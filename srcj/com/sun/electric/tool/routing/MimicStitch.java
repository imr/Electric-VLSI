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

import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.user.dialogs.EDialog;
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

	private static final int LIKELYDIFFPORT      =  1;
	private static final int LIKELYDIFFPORTWIDTH =  2;
	private static final int LIKELYDIFFARCCOUNT  =  4;
	private static final int LIKELYDIFFNODETYPE  =  8;
	private static final int LIKELYDIFFNODESIZE  = 16;
	private static final int LIKELYARCSSAMEDIR   = 32;

	private static final int situations[] = {
		0,
		// any single situation
		LIKELYARCSSAMEDIR,
						  LIKELYDIFFNODESIZE,
											 LIKELYDIFFARCCOUNT,
											 					LIKELYDIFFPORTWIDTH,
											 										LIKELYDIFFPORT,
											 													   LIKELYDIFFNODETYPE,

		// any two situations
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE,
		LIKELYARCSSAMEDIR|                   LIKELYDIFFARCCOUNT,
		LIKELYARCSSAMEDIR|                                      LIKELYDIFFPORTWIDTH,
		LIKELYARCSSAMEDIR|                                                          LIKELYDIFFPORT,
		LIKELYARCSSAMEDIR|                                                                         LIKELYDIFFNODETYPE,
						  LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT,
						  LIKELYDIFFNODESIZE|                   LIKELYDIFFPORTWIDTH,
						  LIKELYDIFFNODESIZE|                                       LIKELYDIFFPORT,
						  LIKELYDIFFNODESIZE|                                                      LIKELYDIFFNODETYPE,
							 				 LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH,
											 LIKELYDIFFARCCOUNT|                    LIKELYDIFFPORT,
											 LIKELYDIFFARCCOUNT|                                   LIKELYDIFFNODETYPE,
											                    LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT,
											                    LIKELYDIFFPORTWIDTH|               LIKELYDIFFNODETYPE,
																                    LIKELYDIFFPORT|LIKELYDIFFNODETYPE,

		// any three situations
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT,
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|                   LIKELYDIFFPORTWIDTH,
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|                                       LIKELYDIFFPORT,
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|                                                      LIKELYDIFFNODETYPE,
		LIKELYARCSSAMEDIR|                   LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH,
		LIKELYARCSSAMEDIR|                   LIKELYDIFFARCCOUNT|                    LIKELYDIFFPORT,
		LIKELYARCSSAMEDIR|                   LIKELYDIFFARCCOUNT|                                   LIKELYDIFFNODETYPE,
		LIKELYARCSSAMEDIR|                                      LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT,
		LIKELYARCSSAMEDIR|                                      LIKELYDIFFPORTWIDTH|               LIKELYDIFFNODETYPE,
		LIKELYARCSSAMEDIR|                                                          LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
		                  LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH,
						  LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|                    LIKELYDIFFPORT,
						  LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|                                   LIKELYDIFFNODETYPE,
						  LIKELYDIFFNODESIZE|                   LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT,
						  LIKELYDIFFNODESIZE|                   LIKELYDIFFPORTWIDTH|               LIKELYDIFFNODETYPE,
						  LIKELYDIFFNODESIZE|                                       LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
											 LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT,
											 LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH|               LIKELYDIFFNODETYPE,
											                    LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT|LIKELYDIFFNODETYPE,

		// any four situations
		                                     LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
		                  LIKELYDIFFNODESIZE|                   LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
		                  LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|                    LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
		                  LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH|               LIKELYDIFFNODETYPE,
		                  LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT,
		LIKELYARCSSAMEDIR|                                      LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
		LIKELYARCSSAMEDIR|                   LIKELYDIFFARCCOUNT|                    LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
		LIKELYARCSSAMEDIR|                   LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH|               LIKELYDIFFNODETYPE,
		LIKELYARCSSAMEDIR|                   LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT,
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|                                       LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|                   LIKELYDIFFPORTWIDTH|               LIKELYDIFFNODETYPE,
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|                   LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT,
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|                                   LIKELYDIFFNODETYPE,
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|                    LIKELYDIFFPORT,
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH,

		// any five situations
		                  LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
		LIKELYARCSSAMEDIR|                   LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|                   LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|                    LIKELYDIFFPORT|LIKELYDIFFNODETYPE,
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH|               LIKELYDIFFNODETYPE,
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT,

		// all six situations
		LIKELYARCSSAMEDIR|LIKELYDIFFNODESIZE|LIKELYDIFFARCCOUNT|LIKELYDIFFPORTWIDTH|LIKELYDIFFPORT|LIKELYDIFFNODETYPE
	};

	private static class PossibleArc
	{
		private int       situation;
		private ArcInst   ai;
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
		UserInterface ui = Job.getUserInterface();
		EditWindow_ wnd = ui.needCurrentEditWindow_();
		if (wnd == null) return;

		Routing.Activity lastActivity = Routing.getRoutingTool().getLastActivity();
		if (lastActivity == null)
		{
			System.out.println("No wiring activity to mimic");
			return;
		}

		// if a single arc was deleted, mimiced it
		if (lastActivity.numDeletedArcs == 1 && lastActivity.numCreatedArcs == 0)
		{
			mimicdelete(lastActivity);
			lastActivity.numDeletedArcs = 0;
			return;
		}

		// if a single arc was just created, mimic that
		if (lastActivity.numCreatedArcs == 1)
		{
			ArcInst ai = lastActivity.createdArcs[0];
			new MimicStitchJob(ai, 0, ai, 1, ai.getWidth(), ai.getProto(), 0, 0, forced);
			lastActivity.numCreatedArcs = 0;
			return;
		}

		// if multiple arcs were just created, find the true end and mimic that
		if (lastActivity.numCreatedArcs > 1 && lastActivity.numCreatedNodes > 0)
		{
			// find the ends of arcs that do not attach to the intermediate pins
			ArcProto proto = lastActivity.createdArcs[0].getProto();
			Cell parent = lastActivity.createdArcs[0].getParent();
			HashSet<NodeInst> gotOne = new HashSet<NodeInst>();
			HashSet<NodeInst> gotMany = new HashSet<NodeInst>();
			for(int i=0; i<lastActivity.numCreatedArcs; i++)
			{
				ArcInst ai = lastActivity.createdArcs[i];
				for(int e=0; e<2; e++)
				{
					NodeInst ni = ai.getPortInst(e).getNodeInst();
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
					NodeInst ni = ai.getPortInst(e).getNodeInst();
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
				new MimicStitchJob(ends[0].getArc(), ends[0].getEndIndex(), ends[1].getArc(), ends[1].getEndIndex(),
					width, null, prefX, prefY, forced);
			}
			lastActivity.numCreatedArcs = 0;
			return;
		}
	}

	/**
	 * Method to mimic the deletion of an arc.
	 */
	private static void mimicdelete(Routing.Activity activity)
	{
		UserInterface ui = Job.getUserInterface();
		EditWindow_ wnd = ui.needCurrentEditWindow_();
		if (wnd == null) return;

		// determine information about deleted arc
		ImmutableArcInst mimicAi = activity.deletedArc;
        Cell cell = Cell.inCurrentThread(activity.deletedArcParent);
        if (cell == null) return; // cell killed
		NodeInst mimicNiHead = cell.getNodeById(mimicAi.headNodeId);
		NodeInst mimicNiTail = cell.getNodeById(mimicAi.tailNodeId);
        if (mimicNiHead == null || mimicNiTail == null) return; // arc end killed
		ArcProto typ = mimicAi.protoType;
		Point2D pt0 = mimicAi.headLocation;
		Point2D pt1 = mimicAi.tailLocation;
		double dist = pt0.distance(pt1);
		int angle = 0;
		if (dist != 0) angle = DBMath.figureAngle(pt0, pt1);

		// look for a similar situation to delete
		List<PossibleArc> arcKills = new ArrayList<PossibleArc>();
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();

			// arc must be of the same type
			if (ai.getProto() != typ) continue;

			// must be the same length and angle
			Point2D end0 = ai.getHeadLocation();
			Point2D end1 = ai.getTailLocation();
			double thisDist = end0.distance(end1);
			if (dist != thisDist) continue;
			if (dist != 0)
			{
				int thisAngle = DBMath.figureAngle(end0, end1);
				if ((angle%1800) != (thisAngle%1800)) continue;
			}
			
			PossibleArc pa = new PossibleArc();
			pa.ai = ai;
			pa.situation = 0;

			// arc must connect to the same type of port
			boolean matchPort = false;
			if (ai.getHeadPortInst().getPortProto().getId() == activity.deletedPorts[0] &&
				ai.getTailPortInst().getPortProto().getId() == activity.deletedPorts[1]) matchPort = true;
			if (ai.getHeadPortInst().getPortProto().getId() == activity.deletedPorts[1] &&
				ai.getTailPortInst().getPortProto().getId() == activity.deletedPorts[0]) matchPort = true;
			if (!matchPort) pa.situation |= LIKELYDIFFPORT;

			// arcs must have the same bus width
			
			NodeInst niHead = ai.getHeadPortInst().getNodeInst();
			NodeInst niTail = ai.getTailPortInst().getNodeInst();
			int con1 = mimicNiHead.getNumConnections() + mimicNiTail.getNumConnections() + 2;
			int con2 = niHead.getNumConnections() + niTail.getNumConnections();
			if (con1 != con2) pa.situation |= LIKELYDIFFARCCOUNT;

			// arc must connect to the same type of node
			boolean matchNode = false;
			if (niHead.getProto() == mimicNiHead.getProto() && niTail.getProto() == mimicNiTail.getProto())
				matchNode = true;
			if (niHead.getProto() == mimicNiTail.getProto() && niTail.getProto() == mimicNiHead.getProto())
				matchNode = true;
			if (!matchNode) pa.situation |= LIKELYDIFFNODETYPE;

			// determine size of nodes on mimic arc
			SizeOffset so = mimicNiHead.getSizeOffset();
			double mimicWidHead = mimicNiHead.getXSize() - so.getLowXOffset() - so.getHighXOffset();
			double mimicHeiHead = mimicNiHead.getYSize() - so.getLowYOffset() - so.getHighYOffset();
			so = mimicNiTail.getSizeOffset();
			double mimicWidTail = mimicNiTail.getXSize() - so.getLowXOffset() - so.getHighXOffset();
			double mimicHeiTail = mimicNiTail.getYSize() - so.getLowYOffset() - so.getHighYOffset();

			// determine size of nodes on possible deleted arc
			so = niHead.getSizeOffset();
			double widHead = niHead.getXSize() - so.getLowXOffset() - so.getHighXOffset();
			double heiHead = niHead.getYSize() - so.getLowYOffset() - so.getHighYOffset();
			so = niTail.getSizeOffset();
			double widTail = niTail.getXSize() - so.getLowXOffset() - so.getHighXOffset();
			double heiTail = niTail.getYSize() - so.getLowYOffset() - so.getHighYOffset();

			// flag if the sizes differ
			if (widHead != mimicWidHead || heiHead != mimicHeiHead) pa.situation |= LIKELYDIFFNODESIZE;
			if (widTail != mimicWidTail || heiTail != mimicHeiTail) pa.situation |= LIKELYDIFFNODESIZE;

			// the same! queue it for deletion
			arcKills.add(pa);
		}

		boolean mimicInteractive = Routing.isMimicStitchInteractive();
		boolean matchPorts = Routing.isMimicStitchMatchPorts();
		boolean matchPortWidth = Routing.isMimicStitchMatchPortWidth();
		boolean matchArcCount = Routing.isMimicStitchMatchNumArcs();
		boolean matchNodeType = Routing.isMimicStitchMatchNodeType();
		boolean matchNodeSize = Routing.isMimicStitchMatchNodeSize();
		boolean noOtherArcsThisDir = Routing.isMimicStitchNoOtherArcsSameDir();
		processPossibilities(cell, arcKills, 0, 0, Job.Type.EXAMINE, true,
			mimicInteractive, matchPorts, matchPortWidth, matchArcCount, matchNodeType, matchNodeSize, noOtherArcsThisDir);
	}

	/**
	 * Class to examine a circuit and find mimic opportunities in a new thread.
	 */
	private static class MimicStitchJob extends Job
	{
		private ArcInst ai1, ai2;
		private int end1, end2;
		private double oWidth;
		private ArcProto oProto;
		private double prefX, prefY;
		private boolean forced;

		private MimicStitchJob(ArcInst ai1, int end1, ArcInst ai2, int end2, double oWidth, ArcProto oProto,
								double prefX, double prefY, boolean forced)
		{
			super("Mimic-Stitch", Routing.getRoutingTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.ai1 = ai1;
			this.end1 = end1;
			this.ai2 = ai2;
			this.end2 = end2;
			this.oWidth = oWidth;
			this.oProto = oProto;
			this.prefX = prefX;
			this.prefY = prefY;
			this.forced = forced;
			setReportExecutionFlag(true);
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// get options
			boolean mimicInteractive = Routing.isMimicStitchInteractive();
			boolean matchPorts = Routing.isMimicStitchMatchPorts();
			boolean matchPortWidth = Routing.isMimicStitchMatchPortWidth();
			boolean matchArcCount = Routing.isMimicStitchMatchNumArcs();
			boolean matchNodeType = Routing.isMimicStitchMatchNodeType();
			boolean matchNodeSize = Routing.isMimicStitchMatchNodeSize();
			boolean noOtherArcsThisDir = Routing.isMimicStitchNoOtherArcsSameDir();

			mimicOneArc(ai1, end1, ai2, end2, oWidth, oProto, prefX, prefY, forced, Job.Type.EXAMINE,
				mimicInteractive, matchPorts, matchPortWidth, matchArcCount, matchNodeType, matchNodeSize, noOtherArcsThisDir, this);
			return true;
		}
	}

	/**
	 * Method to do mimic stitching.
	 * It can be used during batch processing to mimic directly.
	 * @param oWidth the width of the arc to run.
     * @param oProto the type of arc to run.
     * @param prefX the preferred X position of the mimic (if there is a choice).
     * @param prefY the preferred Y position of the mimic (if there is a choice).
     * @param forced true if this was an explicitly requested mimic.
     * @param method the type of job that is running (CHANGE or EXAMINE).
     * @param mimicInteractive true to run interactively.
     * @param matchPorts true to require port types to match.
     * @param matchPortWidth true to require port widths to match.
     * @param matchArcCount true to require the number of arcs to match.
     * @param matchNodeType true to require the node types to match.
     * @param matchNodeSize true to require the node sizes to match.
     * @param noOtherArcsThisDir true to require that no other arcs exist in the same direction.
     * @param theJob
     */
	public static void mimicOneArc(ArcInst ai1, int end1, ArcInst ai2, int end2, double oWidth, ArcProto oProto,
       double prefX, double prefY, boolean forced, Job.Type method, boolean mimicInteractive, boolean matchPorts,
       boolean matchPortWidth, boolean matchArcCount, boolean matchNodeType, boolean matchNodeSize, boolean noOtherArcsThisDir,
       Job theJob)
	{
		if (forced) System.out.println("Mimicing last arc...");

		PortInst [] endPi = new PortInst[2];
		Connection conn1 = ai1.getConnection(end1);
		Connection conn2 = ai2.getConnection(end2);
		endPi[0] = conn1.getPortInst();   endPi[1] = conn2.getPortInst();
		Point2D [] endPts = new Point2D[2];
		endPts[0] = conn1.getLocation();   endPts[1] = conn2.getLocation();
		Cell cell = endPi[0].getNodeInst().getParent();
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted mimic-routing (network information unavailable).  Please try again");
			return;
		}
		int busWidth = netlist.getBusWidth(ai1);

		// make list of possible arc connections
		List<PossibleArc> possibleArcs = new ArrayList<PossibleArc>();

		// count the number of other arcs on the ends
		int con1 = endPi[0].getNodeInst().getNumConnections() + endPi[1].getNodeInst().getNumConnections() - 2;

		// precompute polygon information about every port in the cell
		HashMap<PortInst,Poly> cachedPortPoly = new HashMap<PortInst,Poly>();
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
			{
				PortInst pi = pIt.next();
				if (oProto != null)
				{
					if (!pi.getPortProto().connectsTo(oProto)) continue;
				}
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
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				Rectangle2D bounds = ni.getBounds();

                if (theJob != null && theJob.checkAbort())
                {
                    System.out.println("Mimic Arc Job aborted");
                    return;
                }

				// now look for another node that matches the situation
				for(Iterator<NodeInst> oIt = cell.getNodes(); oIt.hasNext(); )
				{
					NodeInst oNi = oIt.next();
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
					for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
					{
						PortInst pi = pIt.next();
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

						for(Iterator<PortInst> oPIt = oNi.getPortInsts(); oPIt.hasNext(); )
						{
							PortInst oPi = oPIt.next();
							PortProto oPp = oPi.getPortProto();

							// if this port is not cached, it cannot connect, so ignore it
							Poly thisPoly = (Poly)cachedPortPoly.get(oPi);
							if (thisPoly == null) continue;

							// don't replicate what is already done
							if (pi == pi0 && oPi == pi1) continue;

							// see if they are the same distance apart
							boolean ptInPoly = thisPoly.isInside(want1);
							if (!ptInPoly) continue;

							// if there is a network that already connects these, ignore
							if (netlist.sameNetwork(ni, pp, oNi, oPp)) continue;

							// figure out the wiring situation here
							int situation = 0;

							// see if there are already wires going in this direction
							int desiredAngle = -1;
							if (x0 != wantX1 || y0 != wantY1)
								desiredAngle = DBMath.figureAngle(xy0, want1);
							PortInst piNet0 = null;
							for(Iterator<Connection> pII = ni.getConnections(); pII.hasNext(); )
							{
								Connection con = pII.next();
								PortInst aPi = con.getPortInst();
								if (aPi.getPortProto() != pp) continue;
								ArcInst oAi = con.getArc();
								piNet0 = aPi;
								if (desiredAngle < 0)
								{
									if (oAi.getHeadLocation().getX() == oAi.getTailLocation().getX() &&
										oAi.getHeadLocation().getY() == oAi.getTailLocation().getY())
									{
										situation |= LIKELYARCSSAMEDIR;
										break;
									}
								} else
								{
									if (oAi.getHeadLocation().getX() == oAi.getTailLocation().getX() &&
										oAi.getHeadLocation().getY() == oAi.getTailLocation().getY())
											continue;
									int thisend = 0;
									if (oAi.getTailPortInst() == aPi) thisend = 1;
									int existingAngle = DBMath.figureAngle(oAi.getLocation(thisend),
										oAi.getLocation(1-thisend));
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
							for(Iterator<Connection> pII = oNi.getConnections(); pII.hasNext(); )
							{
								Connection con = pII.next();
								PortInst aPi = con.getPortInst();
								if (aPi.getPortProto() != oPp) continue;
								ArcInst oAi = con.getArc();
								piNet1 = aPi;
								if (desiredAngle < 0)
								{
									if (oAi.getHeadLocation().getX() == oAi.getTailLocation().getX() &&
										oAi.getHeadLocation().getY() == oAi.getTailLocation().getY())
									{
										situation |= LIKELYARCSSAMEDIR;
										break;
									}
								} else
								{
									if (oAi.getHeadLocation().getX() == oAi.getTailLocation().getX() &&
										oAi.getHeadLocation().getY() == oAi.getTailLocation().getY())
											continue;
									int thisend = 0;
									if (oAi.getTailPortInst() == aPi) thisend = 1;
									int existingAngle = DBMath.figureAngle(oAi.getLocation(thisend),
											oAi.getLocation(1-thisend));
									if (existingAngle == desiredAngle)
									{
										situation |= LIKELYARCSSAMEDIR;
										break;
									}
								}
							}

							if (pp != port0 || oPp != port1)
								situation |= LIKELYDIFFPORT;
							if (pp instanceof Export && oPp instanceof Export)
							{
								int e0Wid = netlist.getBusWidth((Export)pp);
								int e1Wid = netlist.getBusWidth((Export)oPp);
								if (e0Wid != busWidth || e1Wid != busWidth)
									situation |= LIKELYDIFFPORTWIDTH;
							} else
							{
								if (busWidth != 1) situation |= LIKELYDIFFPORTWIDTH;
							}
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
							for(PossibleArc pa : possibleArcs)
							{
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
		processPossibilities(cell, possibleArcs, prefX, prefY, method, forced,
			mimicInteractive, matchPorts, matchPortWidth, matchArcCount, matchNodeType, matchNodeSize, noOtherArcsThisDir);
	}
	
	private static void processPossibilities(Cell cell, List<PossibleArc> possibleArcs, double prefX, double prefY,
		Job.Type method, boolean forced, boolean mimicInteractive,
		boolean matchPorts, boolean matchPortWidth, boolean matchArcCount, boolean matchNodeType, boolean matchNodeSize, boolean noOtherArcsThisDir)
	{
		if (mimicInteractive)
		{
			// do this in a separate thread so that this examine job can finish
			MimicInteractive task = new MimicInteractive(cell, possibleArcs, prefX, prefY);
			SwingUtilities.invokeLater(task);
			return;
		}

		// not interactive: follow rules in the Preferences
		int ifIgnorePorts = 0, ifIgnorePortWidth = 0, ifIgnoreArcCount = 0, ifIgnoreNodeType = 0, ifIgnoreNodeSize = 0, ifIgnoreOtherSameDir = 0;
		int count = 0;
		boolean deletion = false;
		for(int j=0; j<situations.length; j++)
		{
			// see if this situation is possible
			List<Route> allRoutes = new ArrayList<Route>();
			List<ArcInst> allKills = new ArrayList<ArcInst>();
			int total = 0;
			for(PossibleArc pa : possibleArcs)
			{
				if (pa.ai != null) deletion = true;
				if (pa.situation != situations[j]) continue;
				total++;

				if (pa.ai != null)
				{
					// plan an arc deletion
					allKills.add(pa.ai);
				} else
				{
					// plan an arc creation
					Poly portPoly1 = pa.ni1.getShapeOfPort(pa.pp1);
					Poly portPoly2 = pa.ni2.getShapeOfPort(pa.pp2);
					Point2D bend = new Point2D.Double((portPoly1.getCenterX() + portPoly2.getCenterX()) / 2 + prefX,
						(portPoly1.getCenterY() + portPoly2.getCenterY()) / 2 + prefY);
					PortInst pi1 = pa.ni1.findPortInstFromProto(pa.pp1);
					PortInst pi2 = pa.ni2.findPortInstFromProto(pa.pp2);
					Route route = router.planRoute(pa.ni1.getParent(), pi1, pi2, bend, null, true);
					if (route.size() == 0)
					{
						System.out.println("Problem creating arc");
						continue;
					}
					allRoutes.add(route);
				}
			}
			if (total == 0) continue;

			// make sure this situation is the desired one
			if (matchPorts && (situations[j]&LIKELYDIFFPORT) != 0) { ifIgnorePorts += total;   continue; }
			if (matchPortWidth && (situations[j]&LIKELYDIFFPORTWIDTH) != 0) { ifIgnorePortWidth += total;   continue; }
			if (matchArcCount && (situations[j]&LIKELYDIFFARCCOUNT) != 0) { ifIgnoreArcCount += total;   continue; }
			if (matchNodeType && (situations[j]&LIKELYDIFFNODETYPE) != 0) { ifIgnoreNodeType += total;   continue; }
			if (matchNodeSize && (situations[j]&LIKELYDIFFNODESIZE) != 0) { ifIgnoreNodeSize += total;   continue; }
			if (noOtherArcsThisDir && (situations[j]&LIKELYARCSSAMEDIR) != 0) { ifIgnoreOtherSameDir += total;   continue; }

			// create the routes
			if (method == Job.Type.EXAMINE)
			{
				// since this is an examine job, queue a change job to make the wires
				new MimicWireJob(allRoutes, allKills, false);
			} else
			{
				// since this is a change job, do the wires now
				runTheWires(allRoutes);
			}
			count += total;
		}

		if (count != 0)
		{
			System.out.println("MIMIC ROUTING: " +  (deletion ? "Deleted " : "Created ") + count + " arcs");
		} else
		{
			if (forced)
			{
				String activity = deletion ? "deleted" : "added";
				String msg = "No wires " + activity;
				if (ifIgnorePorts != 0)
					msg += ", might have " + activity + " " + ifIgnorePorts + " wires if 'ports must match' were off";
				if (ifIgnorePortWidth != 0)
					msg += ", might have " + activity + " " + ifIgnorePortWidth + " wires if 'ports must match width' were off";
				if (ifIgnoreArcCount != 0)
					msg += ", might have " + activity + " " + ifIgnoreArcCount + " wires if 'number of existing arcs must match' were off";
				if (ifIgnoreNodeType != 0)
					msg += ", might have " + activity + " " + ifIgnoreNodeType + " wires if 'node types must match' were off";
				if (ifIgnoreNodeSize != 0)
					msg += ", might have " + activity + " " + ifIgnoreNodeSize + " wires if 'nodes sizes must match' were off";
				if (ifIgnoreOtherSameDir != 0)
					msg += ", might have " + activity + " " + ifIgnoreOtherSameDir + " wires if 'cannot have other arcs in the same direction' were off";
				System.out.println(msg);
				if (ifIgnorePorts + ifIgnoreArcCount + ifIgnoreNodeType + ifIgnoreNodeSize + ifIgnoreOtherSameDir != 0)
					System.out.println(" (settings are in the Tools / Routing tab of the Preferences)");
			}
		}
	}

	private static List<PortInst> runTheWires(List<Route> allRoutes)
	{
		// create the routes
		List<PortInst> portsToHighlight = new ArrayList<PortInst>();
		for (Route route : allRoutes)
		{
			RouteElement re = (RouteElement)route.get(0);
			Cell c = re.getCell();
			PortInst showThis = Router.createRouteNoJob(route, c, false, false);
			if (showThis != null) portsToHighlight.add(showThis);
		}
		return portsToHighlight;
	}

	/**
	 * Class to implement actual wire creation/deletion in a new thread.
	 */
	private static class MimicWireJob extends Job
	{
		private List<Route> allRoutes;
		private List<ArcInst> allKills;
		private boolean redisplay;
		private List<PortInst> portsToHighlight;

		private MimicWireJob(List<Route> allRoutes, List<ArcInst> allKills, boolean redisplay)
		{
			super("Mimic-Stitch", Routing.getRoutingTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.allRoutes = allRoutes;
			this.allKills = allKills;
			this.redisplay = redisplay;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			if (allRoutes.size() > 0)
			{
				// create the routes
				portsToHighlight = runTheWires(allRoutes);
				fieldVariableChanged("portsToHighlight");
			} else
			{
                boolean keepPins = Routing.isMimicStitchPinsKept();
				// delete the arcs
				for (ArcInst ai : allKills)
				{
					NodeInst h = ai.getHeadPortInst().getNodeInst();
					NodeInst t = ai.getTailPortInst().getNodeInst();
					ai.kill();

					// also delete freed pin nodes
					if (h.getProto().getFunction() == PrimitiveNode.Function.PIN &&
						!h.hasConnections() && !h.hasExports() && !keepPins)
//						h.getNumConnections() == 0 && h.getNumExports() == 0 && !keepPins)
					{
						h.kill();
					}
					if (t.getProto().getFunction() == PrimitiveNode.Function.PIN &&
						!t.hasConnections() && !t.hasExports() && !keepPins)
//						t.getNumConnections() == 0 && t.getNumExports() == 0 && !keepPins)
					{
						t.kill();
					}
				}
			}
			return true;
		}

		public void terminateOK()
		{
			if (redisplay)
			{
				UserInterface ui = Job.getUserInterface();
				EditWindow_ wnd = ui.getCurrentEditWindow_();
				if (wnd != null)
				{
	                wnd.clearHighlighting();
					for(PortInst pi : portsToHighlight)
					{
		                wnd.addElectricObject(pi, pi.getNodeInst().getParent());
					}
	                wnd.finishedHighlighting();
				}				
				ui.repaintAllEditWindows();
			}
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
		private List<PossibleArc> possibleArcs;
		private double prefX, prefY;

		private MimicInteractive(Cell cell, List<PossibleArc> possibleArcs, double prefX, double prefY)
	    {
	    	this.cell = cell;
	    	this.possibleArcs = possibleArcs;
	    	this.prefX = prefX;
	    	this.prefY = prefY;
	    }

	    public void run()
	    {
			// interactive mode: show paths before creating arcs
	    	presentNextSituation(0, 0, possibleArcs, cell, prefX, prefY);
	    }
	}

	/**
	 * Method to interactively present a mimicing situation to the user.
	 * @param count the number of arcs created so far.
	 * @param situationNumber the starting "situation" number (class of mimics that are allowed).
	 * @param possibleArcs a list of possible arcs to route
	 * @param cell the Cell where routing is going on.
	 * @param wnd the highlighter window.
	 * @param prefX preferred X coordinate when arcs bend.
	 * @param prefY preferred Y coordinate when arcs bend.
	 */
	private static void presentNextSituation(int count, int situationNumber, List<PossibleArc> possibleArcs,
		Cell cell, double prefX, double prefY)
	{
		// find the next situation
 		for(int j=situationNumber; j<situations.length; j++)
		{
			// make a list of mimics that match the situation
			List<Route> allRoutes = new ArrayList<Route>();
			List<ArcInst> allKills = new ArrayList<ArcInst>();
			int total = 0;
			for(PossibleArc pa : possibleArcs)
			{
				if (pa.situation != situations[j]) continue;
				total++;

				if (pa.ai != null)
				{
					// consider a deletion
					allKills.add(pa.ai);
				} else
				{
					// consider a creation
					Poly portPoly1 = pa.ni1.getShapeOfPort(pa.pp1);
					Poly portPoly2 = pa.ni2.getShapeOfPort(pa.pp2);
					Point2D bend = new Point2D.Double((portPoly1.getCenterX() + portPoly2.getCenterX()) / 2 + prefX,
						(portPoly1.getCenterY() + portPoly2.getCenterY()) / 2 + prefY);
					PortInst pi1 = pa.ni1.findPortInstFromProto(pa.pp1);
					PortInst pi2 = pa.ni2.findPortInstFromProto(pa.pp2);
					Route route = router.planRoute(pa.ni1.getParent(), pi1, pi2, bend, null, true);
					if (route.size() == 0)
					{
						System.out.println("Problem creating arc");
						continue;
					}
					allRoutes.add(route);
				}
			}
			if (total == 0) continue;

			// save what is highlighted
			UserInterface ui = Job.getUserInterface();
			EditWindow_ wnd = ui.getCurrentEditWindow_();
			List<Highlight2> saveHighlights = wnd.saveHighlightList();

			// show the wires to be created/deleted
			wnd.clearHighlighting();
			for(Route route : allRoutes)
			{
				// determine the actual endpoints of the route
				Poly sPi = route.getStart().getPortInst().getPoly();
				Poly ePi = route.getEnd().getPortInst().getPoly();
				double fX = sPi.getCenterX();   double fY = sPi.getCenterY();
				double tX = ePi.getCenterX();   double tY = ePi.getCenterY();
				if (fX == tX && fY == tY)
				{
					Rectangle2D pointRect = new Rectangle2D.Double(fX-1, fY-1, 2, 2);
					wnd.addHighlightArea(pointRect, cell);
				} else
				{
					wnd.addHighlightLine(new Point2D.Double(fX, fY), new Point2D.Double(tX, tY), cell, false);
				}
			}
			for(ArcInst ai : allKills)
			{
				wnd.addHighlightLine(ai.getHeadLocation().mutable(), ai.getTailLocation().mutable(), cell, false);
			}
			wnd.finishedHighlighting();

			// ask if the user wants to do it
			new MimicDialog(TopLevel.getCurrentJFrame(), count, allRoutes, allKills, saveHighlights, wnd, j+1, possibleArcs, cell, prefX, prefY);
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
		private List<Route> allRoutes;
		private List<ArcInst> allKills;
		private List<Highlight2> saveHighlights;
		private EditWindow_ wnd;
		private int nextSituationNumber;
		private List<PossibleArc> possibleArcs;
		private Cell cell;
		private double prefX, prefY;

		private MimicDialog(Frame parent, int count, List<Route> allRoutes, List<ArcInst> allKills, List<Highlight2> saveHighlights,
			EditWindow_ wnd, int nextSituationNumber, List<PossibleArc> possibleArcs, Cell cell, double prefX, double prefY)
		{
			super(parent, false);
			this.count = count;
			this.allRoutes = allRoutes;
			this.allKills = allKills;
			this.saveHighlights = saveHighlights;
			this.wnd = wnd;
			this.nextSituationNumber = nextSituationNumber;
			this.possibleArcs = possibleArcs;
			this.cell = cell;
			this.prefX = prefX;
			this.prefY = prefY;

			String activity = (allKills.size() > 0 ? "Delete" : "Create");
	        getContentPane().setLayout(new GridBagLayout());
	        setTitle(activity + " wires?");
	        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

	        JLabel question = new JLabel(activity + " " + (allRoutes.size()+allKills.size()) + " wires shown here?");
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
			wnd.clearHighlighting();
			wnd.restoreHighlightList(saveHighlights);
			wnd.finishedHighlighting();

			new MimicWireJob(allRoutes, allKills, false);
			count += allRoutes.size() + allKills.size();
			presentNextSituation(count, situations.length, possibleArcs, cell, prefX, prefY);

			setVisible(false);
			dispose();
		}

		private void noAndStopActionPerformed()
		{
			wnd.clearHighlighting();
			wnd.restoreHighlightList(saveHighlights);
			wnd.finishedHighlighting();

			presentNextSituation(count, situations.length, possibleArcs, cell, prefX, prefY);

			setVisible(false);
			dispose();
		}

		private void yes()
		{
			wnd.clearHighlighting();
			wnd.restoreHighlightList(saveHighlights);
			wnd.finishedHighlighting();

			new MimicWireJob(allRoutes, allKills, false);
			count += allRoutes.size() + allKills.size();
			presentNextSituation(count, nextSituationNumber, possibleArcs, cell, prefX, prefY);

			setVisible(false);
			dispose();
		}

		private void no()
		{
			wnd.clearHighlighting();
			wnd.restoreHighlightList(saveHighlights);
			wnd.finishedHighlighting();

			presentNextSituation(count, nextSituationNumber, possibleArcs, cell, prefX, prefY);

			setVisible(false);
			dispose();
		}
	}
}

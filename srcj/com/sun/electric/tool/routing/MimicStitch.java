/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MimicStitch.java
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

import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.user.ui.WiringListener;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * This is the Mimic Stitching tool.
 */
public class MimicStitch
{
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

	static class PossibleArc
	{
		int               situation;
		NodeInst          ni1, ni2;
		PortProto         pp1, pp2;
	};

	/*
	 * Entry point for mimic router.  Called each "slice".  If "forced" is true,
	 * this mimic operation was explicitly requested.
	 */
	public static void mimicStitch(boolean forced)
	{
		Routing.Activity lastActivity = Routing.tool.getLastActivity();
		if (lastActivity == null)
		{
			System.out.println("No wiring activity to mimic");
			return;
		}

		// if a single arc was deleted, and that is being mimiced, do it
//		if (lastActivity.numDeletedArcs == 1 && Routing.isMimicStitchCanUnstitch())
//		{
//			for(i=0; i<2; i++)
//			{
//				endnode[i] = ro_deletednodes[i];
//				endport[i] = ro_deletedports[i];
//			}
//			ap = lastActivity.deletedarcs[0]->proto;
//			ro_mimicdelete(ap, endnode, endport);
//			ro_lastactivity.numdeletedarcs = 0;
//			return;
//		}

		// if a single arc was just created, mimic that
		if (lastActivity.numCreatedArcs == 1)
		{
			ArcInst ai = lastActivity.createdArcs[0];
			if (ro_mimiccreatedarc(ai) == 0)
			{
				// nothing was wired
				if (forced) System.out.println("No wires mimiced");
			}
			lastActivity.numCreatedArcs = 0;
			return;
		}

//		// if multiple arcs were just created, find the true end and mimic that
//		if (ro_lastactivity.numcreatedarcs > 1 && ro_lastactivity.numcreatednodes > 0)
//		{
//			// find the ends of arcs that do not attach to the intermediate pins
//			parent = ro_lastactivity.createdarcs[0]->parent;
//			for(ni = parent->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//				ni->temp1 = 0;
//			for(i=0; i<ro_lastactivity.numcreatednodes; i++)
//				ro_lastactivity.creatednodes[i]->temp1 = 0;
//			for(i=0; i<ro_lastactivity.numcreatedarcs; i++)
//			{
//				ro_lastactivity.createdarcs[i]->end[0].nodeinst->temp1++;
//				ro_lastactivity.createdarcs[i]->end[1].nodeinst->temp1++;
//			}
//			foundends = 0;
//			width = 0;
//			for(i=0; i<ro_lastactivity.numcreatedarcs; i++)
//			{
//				for(e=0; e<2; e++)
//				{
//					if (ro_lastactivity.createdarcs[i]->end[e].nodeinst->temp1 != 1) continue;
//					if (foundends < 2)
//					{
//						endnode[foundends] = ro_lastactivity.createdarcs[i]->end[e].nodeinst;
//						endport[foundends] = ro_lastactivity.createdarcs[i]->end[e].portarcinst->proto;
//						endx[foundends] = ro_lastactivity.createdarcs[i]->end[e].xpos;
//						endy[foundends] = ro_lastactivity.createdarcs[i]->end[e].ypos;
//						if (ro_lastactivity.createdarcs[i]->width > width)
//							width = ro_lastactivity.createdarcs[i]->width;
//						proto = ro_lastactivity.createdarcs[i]->proto;
//					}
//					foundends++;
//				}
//			}
//
//			// if exactly two ends are found, mimic that connection
//			if (foundends == 2)
//			{
//				prefx = prefy = 0;
//				if (ro_lastactivity.numcreatednodes == 1)
//				{
//					portposition(endnode[0], endport[0], &x0, &y0);
//					portposition(endnode[1], endport[1], &x1, &y1);
//					prefx = (ro_lastactivity.creatednodes[0]->lowx + ro_lastactivity.creatednodes[0]->highx) / 2 -
//						(x0+x1) / 2;
//					prefy = (ro_lastactivity.creatednodes[0]->lowy + ro_lastactivity.creatednodes[0]->highy) / 2 -
//						(y0+y1) / 2;
//				} else if (ro_lastactivity.numcreatednodes == 2)
//				{
//					portposition(endnode[0], endport[0], &x0, &y0);
//					portposition(endnode[1], endport[1], &x1, &y1);
//					prefx = (ro_lastactivity.creatednodes[0]->lowx + ro_lastactivity.creatednodes[0]->highx +
//						ro_lastactivity.creatednodes[1]->lowx + ro_lastactivity.creatednodes[1]->highx) / 4 -
//							(x0+x1) / 2;
//					prefy = (ro_lastactivity.creatednodes[0]->lowy + ro_lastactivity.creatednodes[0]->highy +
//						ro_lastactivity.creatednodes[1]->lowy + ro_lastactivity.creatednodes[1]->highy) / 4 -
//							(y0+y1) / 2;
//				}
//				if (ro_mimiccreatedany(endnode[0], endport[0], endx[0], endy[0],
//					endnode[1], endport[1], endx[1], endy[1],
//					width, proto, prefx, prefy) == 0)
//				{
//					// nothing was wired
//					if (forced) System.out.println("No wires mimiced");
//				}
//			}
//			ro_lastactivity.numcreatedarcs = 0;
//			return;
//		}
	}

	/*
	 * Routine to mimic the unrouting of an arc that ran from nodes[0]/ports[0] to
	 * nodes[1]/ports[1] with type "typ".
	 */
//	void ro_mimicdelete(ARCPROTO *typ, NODEINST **nodes, PORTPROTO **ports)
//	{
//		REGISTER NODEPROTO *cell;
//		REGISTER ARCINST *ai, *nextai;
//		REGISTER INTBIG match, dist, thisdist, deleted, angle, thisangle;
//		INTBIG x0, y0, x1, y1;
//
//		// determine length of deleted arc
//		portposition(nodes[0], ports[0], &x0, &y0);
//		portposition(nodes[1], ports[1], &x1, &y1);
//		dist = computedistance(x0, y0, x1, y1);
//		angle = (dist != 0 ? figureangle(x0, y0, x1, y1) : 0);
//
//		// look for a similar situation to delete
//		deleted = 0;
//		cell = nodes[0]->parent;
//		for(ai = cell->firstarcinst; ai != NOARCINST; ai = nextai)
//		{
//			nextai = ai->nextarcinst;
//
//			// arc must be of the same type
//			if (ai->proto != typ) continue;
//
//			// arc must connect to the same type of node/port
//			match = 0;
//			if (ai->end[0].nodeinst->proto == nodes[0]->proto &&
//				ai->end[1].nodeinst->proto == nodes[1]->proto &&
//				ai->end[0].portarcinst->proto == ports[0] &&
//				ai->end[1].portarcinst->proto == ports[1]) match = 1;
//			if (ai->end[0].nodeinst->proto == nodes[1]->proto &&
//				ai->end[1].nodeinst->proto == nodes[0]->proto &&
//				ai->end[0].portarcinst->proto == ports[1] &&
//				ai->end[1].portarcinst->proto == ports[0]) match = -1;
//			if (match == 0) continue;
//
//			// must be the same length and angle
//			portposition(ai->end[0].nodeinst, ai->end[0].portarcinst->proto, &x0, &y0);
//			portposition(ai->end[1].nodeinst, ai->end[1].portarcinst->proto, &x1, &y1);
//			thisdist = computedistance(x0, y0, x1, y1);
//			if (dist != thisdist) continue;
//			if (dist != 0)
//			{
//				thisangle = figureangle(x0, y0, x1, y1);
//				if ((angle%1800) != (thisangle%1800)) continue;
//			}
//
//			// the same! delete it
//			startobjectchange((INTBIG)ai, VARCINST);
//			(void)killarcinst(ai);
//			deleted++;
//		}
//		if (deleted != 0)
//			System.out.println("MIMIC ROUTING: deleted %ld %s"), deleted, makeplural(_("wire"), deleted));
//	}

	/*
	 * Routine to mimic the creation of arc "arc".  Returns the number of mimics made.
	 */
//	private static int ro_mimiccreatedany(NodeInst ni1, PortProto pp1, NodeInst ni2, PortProto pp2,
//		double width, ArcProto proto, double prefx, double prefy)
//	{
//		return ro_mimicthis(ni1, pp1, ni2, pp2, width, proto, prefx, prefy);
//	}

	/*
	 * Routine to mimic the creation of arc "arc".  Returns the number of mimics made.
	 */
	private static int ro_mimiccreatedarc(ArcInst ai)
	{
		return ro_mimicthis(ai.getHead(), ai.getTail(), ai.getWidth(), ai.getProto(), 0, 0);
	}

//	// Prompt: Yes/No with "stop asking" */
//	static DIALOGITEM ro_yesnostopdialogitems[] =
//	{
//	 /*  1 */ {0, {64,156,88,228}, BUTTON, N_("Yes")},
//	 /*  2 */ {0, {64,68,88,140}, BUTTON, N_("No")},
//	 /*  3 */ {0, {6,15,54,279}, MESSAGE, x_("")},
//	 /*  4 */ {0, {96,156,120,276}, BUTTON, N_("Yes, then stop")},
//	 /*  5 */ {0, {96,20,120,140}, BUTTON, N_("No, and stop")}
//	};
//	static DIALOG ro_yesnostopdialog = {{50,75,179,363}, N_("Warning"), 0, 5, ro_yesnostopdialogitems, 0, 0};
//
//	#define DYNS_YES     1		/* "Yes" (button) */
//	#define DYNS_NO      2		/* "No" (button) */
//	#define DYNS_MESSAGE 3		/* routing message (stat text) */
//	#define DYNS_YESSTOP 4		/* "Yes, then stop" (button) */
//	#define DYNS_NOSTOP  5		/* "No, and stop" (button) */

//	INTBIG ro_mimicthis(NODEINST *oni1, PORTPROTO *opp1, INTBIG ox1, INTBIG oy1,
//		NODEINST *oni2, PORTPROTO *opp2, INTBIG ox2, INTBIG oy2,
//		INTBIG owidth, ARCPROTO *oproto, INTBIG prefx, INTBIG prefy)
	private static int ro_mimicthis(Connection conn1, Connection conn2, double oWidth, ArcProto oProto, double prefX, double prefY)
	{
		System.out.println("Mimicing last arc...");

		PortInst pi1 = conn1.getPortInst();
		NodeInst oni1 = pi1.getNodeInst();
		PortProto opp1 = pi1.getPortProto();
		Point2D opt1 = conn1.getLocation();

		PortInst pi2 = conn2.getPortInst();
		NodeInst oni2 = pi2.getNodeInst();
		PortProto opp2 = pi2.getPortProto();
		Point2D opt2 = conn2.getLocation();

		NodeInst [] endni = new NodeInst[2];
		endni[0] = oni1;   endni[1] = oni2;
		PortProto [] endpp = new PortProto[2];
		endpp[0] = opp1;   endpp[1] = opp2;
		Point2D [] endPts = new Point2D[2];
		endPts[0] = opt1;   endPts[1] = opt2;

		Cell cell = oni1.getParent();
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
		int con1 = oni1.getNumConnections() + oni2.getNumConnections() - 2;
		FlagSet portMark = PortProto.getFlagSet(1);

		// precompute information about every port in the cell
		int total = 0;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			for(Iterator pIt = ni.getProto().getPorts(); pIt.hasNext(); )
			{
				PortProto pp = (PortProto)pIt.next();
				pp.clearBit(portMark);
				if (!pp.connectsTo(oProto)) continue;
				pp.setBit(portMark);
				total++;
			}
		}
		if (total == 0)
		{
			portMark.freeFlagSet();
			return count;
		}

		Poly [] ro_mimicportpolys = new Poly[total];
		int i = 0;
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			ni.setTempInt(i);
			for(Iterator pIt = ni.getProto().getPorts(); pIt.hasNext(); )
			{
				PortProto pp = (PortProto)pIt.next();
				if (!pp.isBit(portMark)) continue;
				ro_mimicportpolys[i] = ni.getShapeOfPort(pp);
				i++;
			}
		}

		// search from both ends
		for(int end=0; end<2; end++)
		{
			NodeInst node0 = endni[end];
			NodeInst node1 = endni[1-end];
			NodeProto proto0 = node0.getProto();
			NodeProto proto1 = node1.getProto();
			PortProto port0 = endpp[end];
			PortProto port1 = endpp[1-end];
			Point2D pt0 = endPts[end];
			Point2D pt1 = endPts[1-end];
			double dist = pt0.distance(pt1);
			int angle = 0;
			if (dist != 0) angle = EMath.figureAngle(pt0, pt1);
//			if ((angle%900) == 0) usefangle = FALSE; else
//			{
//				fangle = ffigureangle(x0, y1, x1, y1);
//				usefangle = TRUE;
//			}
			Poly port0Poly = node0.getShapeOfPort(port0);
			double end0offx = pt0.getX() - port0Poly.getCenterX();
			double end0offy = pt0.getY() - port0Poly.getCenterY();

			SizeOffset so0 = node0.getProto().getSizeOffset();
			double node0wid = node0.getXSize() - so0.getLowXOffset() - so0.getHighXOffset();
			double node0hei = node0.getYSize() - so0.getLowYOffset() - so0.getHighYOffset();

			SizeOffset so1 = node1.getProto().getSizeOffset();
			double node1wid = node1.getXSize() - so1.getLowXOffset() - so1.getHighXOffset();
			double node1hei = node1.getYSize() - so1.getLowYOffset() - so1.getHighYOffset();

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
					int portpos = ni.getTempInt();
					for(Iterator pIt = ni.getProto().getPorts(); pIt.hasNext(); )
					{
						PortProto pp = (PortProto)pIt.next();

						// make sure the arc can connect
						if (!pp.isBit(portMark)) continue;

						Poly poly = ro_mimicportpolys[portpos++];
						double x0 = poly.getCenterX();
						double y0 = poly.getCenterY();
						x0 += end0offx;   y0 += end0offy;
						double wantx1 = x0;
						double wanty1 = y0;
						if (dist != 0)
						{
//							if (usefangle)
//							{
//								wantx1 = x0 + rounddouble(dist * cos(fangle));
//								wanty1 = y0 + rounddouble(dist * sin(fangle));
//							} else
							{
								wantx1 = x0 + EMath.cos(angle) * dist;
								wanty1 = y0 + EMath.sin(angle) * dist;
							}
						}
						Point2D xy0 = new Point2D.Double(x0, y0);
						Point2D want1 = new Point2D.Double(wantx1, wanty1);

						int oportpos = oNi.getTempInt();
						for(Iterator oPIt = oNi.getProto().getPorts(); oPIt.hasNext(); )
						{
							PortProto opp = (PortProto)oPIt.next();

							// make sure the arc can connect
							if (!opp.isBit(portMark)) continue;
							Poly thispoly = ro_mimicportpolys[oportpos++];

							// don't replicate what is already done
							if (ni == node0 && pp == port0 && oNi == node1 && opp == port1) continue;

							// see if they are the same distance apart
							if (!thispoly.isInside(want1)) continue;

							// figure out the wiring situation here
							int situation = 0;

							// see if there are already wires going in this direction
							int desiredangle = -1;
							if (x0 != wantx1 || y0 != wanty1)
								desiredangle = EMath.figureAngle(xy0, want1);
							JNetwork net0 = null;
							for(Iterator pII = ni.getConnections(); pII.hasNext(); )
							{
								Connection con = (Connection)pII.next();
								PortInst pi = con.getPortInst();
								if (pi.getPortProto() != pp) continue;
								ArcInst oai = con.getArc();
								net0 = netlist.getNetwork(pi);
								if (desiredangle < 0)
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
									int existingangle = EMath.figureAngle(oai.getConnection(thisend).getLocation(),
										oai.getConnection(1-thisend).getLocation());
									if (existingangle == desiredangle)
									{
										situation |= LIKELYARCSSAMEDIR;
										break;
									}
								}
							}

							desiredangle = -1;
							if (x0 != wantx1 || y0 != wanty1)
								desiredangle = EMath.figureAngle(want1, xy0);
							JNetwork net1 = null;
							for(Iterator pII = oNi.getConnections(); pII.hasNext(); )
							{
								Connection con = (Connection)pII.next();
								PortInst pi = con.getPortInst();
								if (pi.getPortProto() != opp) continue;
								ArcInst oai = con.getArc();
								net1 = netlist.getNetwork(pi);
								if (desiredangle < 0)
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
									int existingangle = EMath.figureAngle(oai.getConnection(thisend).getLocation(),
										oai.getConnection(1-thisend).getLocation());
									if (existingangle == desiredangle)
									{
										situation |= LIKELYARCSSAMEDIR;
										break;
									}
								}
							}

							// if there is a network that already connects these, ignore
							if (net1 == net0 && net0 != null) continue;

							if (pp != port0 || opp != port1)
								situation |= LIKELYDIFFPORT;
							int con2 = ni.getNumConnections() + oNi.getNumConnections();
							if (con1 != con2) situation |= LIKELYDIFFARCCOUNT;
							if (ni.getProto() != proto0 || oNi.getProto() != proto1)
								situation |= LIKELYDIFFNODETYPE;

							SizeOffset so = ni.getProto().getSizeOffset();
							double wid = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
							double hei = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
							if (wid != node0wid || hei != node0hei) situation |= LIKELYDIFFNODESIZE;
							so = oNi.getProto().getSizeOffset();
							wid = oNi.getXSize() - so.getLowXOffset() - so.getHighXOffset();
							hei = oNi.getYSize() - so.getLowYOffset() - so.getHighYOffset();
							if (wid != node1wid || hei != node1hei) situation |= LIKELYDIFFNODESIZE;

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
							found.ni1 = ni;      found.pp1 = pp;
							found.ni2 = oNi;     found.pp2 = opp;
							found.situation = situation;
						}
					}
				}
			}
		}

		// now create the mimiced arcs
		int ifIgnorePorts = 0, ifIgnoreArcCount = 0, ifIgnoreNodeType = 0,
			ifIgnoreNodeSize = 0, ifIgnoreOtherSameDir = 0;
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
//				// show the wires to be created
//				(void)asktool(us_tool, x_("down-stack"));
//				(void)asktool(us_tool, x_("clear"));
//				for(pa = firstpossiblearc; pa != NOPOSSIBLEARC; pa = pa->nextpossiblearc)
//				{
//					if (pa->situation != situations[j]) continue;
//					if (pa->x1 == pa->x2 && pa->y1 == pa->y2)
//					{
//						dist = el_curlib->lambda[el_curtech->techindex];
//						(void)asktool(us_tool, x_("show-area"), pa->x1-dist, pa->x1+dist, pa->y1-dist, pa->y1+dist,
//							(INTBIG)cell);
//					} else
//					{
//						(void)asktool(us_tool, x_("show-line"), pa->x1, pa->y1, pa->x2, pa->y2, (INTBIG)cell);
//					}
//				}
//				esnprintf(question, 200, _("Create %ld %s shown here?"),
//					total, makeplural(_("wire"), total));
//				dia = DiaInitDialog(&ro_yesnostopdialog);
//				if (dia == 0)
//				{
//					portMark.freeFlagSet();
//					return(0);
//				}
//				DiaSetText(dia, DYNS_MESSAGE, question);
//				for(;;)
//				{
//					itemHit = DiaNextHit(dia);
//					if (itemHit == DYNS_YES || itemHit == DYNS_YESSTOP ||
//						itemHit == DYNS_NO || itemHit == DYNS_NOSTOP) break;
//				}
//				DiaDoneDialog(dia);
//				(void)asktool(us_tool, x_("up-stack"));
//				if (itemHit == DYNS_NO) continue;
//				if (itemHit == DYNS_NOSTOP) break;
			}

			// make the wires
			for(Iterator it = possibleArcs.iterator(); it.hasNext(); )
			{
				PossibleArc pa = (PossibleArc)it.next();
				if (pa.situation != situations[j]) continue;

				Poly portPoly1 = pa.ni1.getShapeOfPort(pa.pp1);
				Poly portPoly2 = pa.ni2.getShapeOfPort(pa.pp2);
				Point2D bend = new Point2D.Double((portPoly1.getCenterX() + portPoly2.getCenterX()) / 2 + prefX,
					(portPoly1.getCenterY() + portPoly2.getCenterY()) / 2 + prefY);
				List added = WiringListener.makeConnection(pa.ni1, pa.pp1, pa.ni2, pa.pp2, bend, false, false);
				if (added == null)
				{
					System.out.println("Problem creating arc");
					portMark.freeFlagSet();
					return count;
				}
				count++;
			}

			// stop now if requested
//			if (mimicInteractive && itemHit == DYNS_YESSTOP) break;
		}

		if (count != 0)
		{
//			us_reportarcscreated(_("MIMIC ROUTING"), count, 0, 0, 0);
			System.out.println("MIMIC ROUTING: Created " + count + " arcs");
		} else
		{
			String msg = "No wires added";
			if (ifIgnorePorts != 0)
				msg += ", would add " + ifIgnorePorts + " wires if 'ports must match' were off";
			if (ifIgnoreArcCount != 0)
				msg += ", would add " + ifIgnoreArcCount + " wires if 'number of existing arcs must match' were off";
			if (ifIgnoreNodeType != 0)
				msg += ", would add " + ifIgnoreNodeType + " wires if 'node types must match' were off";
			if (ifIgnoreNodeSize != 0)
				msg += ", would add " + ifIgnoreNodeSize + " wires if 'nodes sizes must match' were off";
			if (ifIgnoreOtherSameDir != 0)
				msg += ", would add " + ifIgnoreOtherSameDir + " wires if 'cannot have other arcs in the same direction' were off";
			System.out.println(msg);
		}
		portMark.freeFlagSet();
		return count;
	}

}

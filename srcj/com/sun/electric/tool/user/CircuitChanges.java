/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CircuitChanges.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Highlight;

import java.util.Iterator;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.awt.geom.Point2D;

/**
 * Class for user-level changes to the circuit.
 */
public class CircuitChanges
{
	// constructor, never used
	CircuitChanges() {}

	/**
	 * Routine to delete all selected objects.
	 */
	public static void deleteSelected()
	{
        DeleteSelected job = new DeleteSelected();
	}

	protected static class DeleteSelected extends Job
	{
		protected DeleteSelected()
		{
			super("Delete", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.startJob();
		}

		public void doIt()
		{
			if (Highlight.getNumHighlights() == 0) return;
			List deleteList = new ArrayList();
			int i = 0;
			Cell cell = null;
			boolean warned = false;
			for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.GEOM) continue;
				Geometric geom = h.getGeom();
				if (cell == null) cell = geom.getParent(); else
				{
					if (!warned && cell != geom.getParent())
					{
						System.out.println("Warning: Not all objects being deleted are in the same cell");
						warned = true;
					}
				}
				deleteList.add(geom);
			}
			Highlight.clear();
			Highlight.finished();
			eraseObjectsInList(cell, deleteList);
		}
	}


	/**
	 * routine to move the arcs in the GEOM module list "list" (terminated by
	 * NOGEOM) and the "total" nodes in the list "nodelist" by (dx, dy).
	 */
	public static void manyMove(double dx, double dy)
	{
        ManyMove job = new ManyMove(dx, dy);
	}

	protected static class ManyMove extends Job
	{
		double dx, dy;
		protected ManyMove(double dx, double dy)
		{
			super("Move", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dx = dx;   this.dy = dy;
			this.startJob();
		}

		public void doIt()
		{
			// get information about what is highlighted
			int total = Highlight.getNumHighlights();
			if (total <= 0) return;
			Iterator oit = Highlight.getHighlights();
			Highlight firstH = (Highlight)oit.next();
			Geometric firstGeom = firstH.getGeom();
			Cell cell = firstGeom.getParent();

			// special case if moving only one node
			if (total == 1 && firstGeom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)firstGeom;
				ni.modifyInstance(dx, dy, 0, 0, 0);
				return;
			}

			// special case if moving diagonal fixed-angle arcs connected to single manhattan arcs
			boolean found = true;
			for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.GEOM) continue;
				Geometric geom = h.getGeom();
				if (geom instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)geom;
					if (ai.getHead().getLocation().getX() == ai.getTail().getLocation().getX() ||
						ai.getHead().getLocation().getY() == ai.getTail().getLocation().getY()) { found = false;   break; }
					if (!ai.isFixedAngle()) { found = false;   break; }
					if (ai.isRigid()) { found = false;   break; }
					int j;
					for(j=0; j<2; j++)
					{
						NodeInst ni = ai.getConnection(j).getPortInst().getNodeInst();
						ArcInst oai = null;
						for(Iterator pIt = ni.getConnections(); pIt.hasNext(); )
						{
							Connection con = (Connection)pIt.next();
							if (con.getArc() == ai) continue;
							if (oai == null) oai = con.getArc(); else
							{
								oai = null;
								break;
							}
						}
						if (oai == null) break;
						if (oai.getHead().getLocation().getX() != oai.getTail().getLocation().getX() &&
							oai.getHead().getLocation().getY() != oai.getTail().getLocation().getY()) break;
					}
					if (j < 2) { found = false;   break; }
				} else found = false;
			}
			if (found)
			{
				// meets the test: make the special move to slide other orthogonal arcs
				for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
				{
					Highlight h = (Highlight)it.next();
					if (h.getType() != Highlight.Type.GEOM) continue;
					Geometric geom = h.getGeom();
					if (!(geom instanceof ArcInst)) continue;
					ArcInst ai = (ArcInst)geom;

					double [] deltaXs = new double[2];
					double [] deltaYs = new double[2];
					double [] deltaNulls = new double[2];
					int [] deltaRots = new int[2];
					NodeInst [] niList = new NodeInst[2];
					deltaNulls[0] = deltaNulls[1] = 0;
					deltaXs[0] = deltaYs[0] = deltaXs[1] = deltaYs[1] = 0;
					int arcangle = ai.getAngle();
					int j;
					for(j=0; j<2; j++)
					{
						NodeInst ni = ai.getConnection(j).getPortInst().getNodeInst();
						niList[j] = ni;
						ArcInst oai = null;
						for(Iterator pIt = ni.getConnections(); pIt.hasNext(); )
						{
							Connection con = (Connection)pIt.next();
							if (con.getArc() != ai) { oai = con.getArc();   break; }
						}
						if (oai == null) break;
						if (EMath.doublesEqual(oai.getHead().getLocation().getX(), oai.getTail().getLocation().getX()))
						{
							Point2D iPt = EMath.intersect(oai.getHead().getLocation(), 900,
								new Point2D.Double(ai.getHead().getLocation().getX()+dx, ai.getHead().getLocation().getY()+dy), arcangle);
							deltaXs[j] = iPt.getX() - ai.getConnection(j).getLocation().getX();
							deltaYs[j] = iPt.getY() - ai.getConnection(j).getLocation().getY();
						} else if (EMath.doublesEqual(oai.getHead().getLocation().getY(), oai.getTail().getLocation().getY()))
						{
							Point2D iPt = EMath.intersect(oai.getHead().getLocation(), 0,
								new Point2D.Double(ai.getHead().getLocation().getX()+dx, ai.getHead().getLocation().getY()+dy), arcangle);
							deltaXs[j] = iPt.getX() - ai.getConnection(j).getLocation().getX();
							deltaYs[j] = iPt.getY() - ai.getConnection(j).getLocation().getY();
						}
					}
					if (j < 2) continue;
					deltaRots[0] = deltaRots[1] = 0;
					NodeInst.modifyInstances(niList, deltaXs, deltaYs, deltaNulls, deltaNulls, deltaRots);
				}
				return;
			}

			// special case if moving only arcs and they slide
			boolean onlySlidable = true;
			for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.GEOM) continue;
				Geometric geom = h.getGeom();
				if (geom instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)geom;
					// see if the arc moves in its ports
					if (ai.isSlidable())
					{
						Connection head = ai.getHead();
						Connection tail = ai.getTail();
						Point2D newHead = new Point2D.Double(head.getLocation().getX()+dx, head.getLocation().getY()+dy);
						Point2D newTail = new Point2D.Double(tail.getLocation().getX()+dx, tail.getLocation().getY()+dy);
						if (ai.stillInPort(head, newHead, true) && ai.stillInPort(tail, newTail, true)) continue;
					}
				}
				onlySlidable = false;
			}
			if (onlySlidable)
			{
				for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
				{
					Highlight h = (Highlight)it.next();
					if (h.getType() != Highlight.Type.GEOM) continue;
					Geometric geom = h.getGeom();
					if (geom instanceof ArcInst)
					{
						ArcInst ai = (ArcInst)geom;
						ai.modify(0, dx, dy, dx, dy);
					}
				}
				return;
			}

			// make flag to track the nodes that move
			FlagSet flag = Geometric.getFlagSet(1);

			// remember the location of every node and arc
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.clearBit(flag);
				ni.setTempObj(new Point2D.Double(ni.getCenterX(), ni.getCenterY()));
			}
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ai.setTempObj(new Point2D.Double(ai.getCenterX(), ai.getCenterY()));
			}

			int numNodes = 0;
			for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.GEOM) continue;
				Geometric geom = h.getGeom();
				if (geom instanceof NodeInst)
				{
					NodeInst ni = (NodeInst)geom;
					if (!ni.isBit(flag)) numNodes++;
					ni.setBit(flag);
				} else
				{
					ArcInst ai = (ArcInst)geom;
					NodeInst ni1 = ai.getHead().getPortInst().getNodeInst();
					NodeInst ni2 = ai.getTail().getPortInst().getNodeInst();
					if (!ni1.isBit(flag)) numNodes++;
					if (!ni2.isBit(flag)) numNodes++;
					ni1.setBit(flag);
					ni2.setBit(flag);
					Layout.setTempRigid(ai, true);
				}
			}

			// look at all nodes and move them appropriately
			if (numNodes > 0)
			{
				NodeInst [] nis = new NodeInst[numNodes];
				double [] dX = new double[numNodes];
				double [] dY = new double[numNodes];
				double [] dSize = new double[numNodes];
				int [] dRot = new int[numNodes];
				boolean [] dTrn = new boolean[numNodes];
				numNodes = 0;
				for(Iterator it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					if (!ni.isBit(flag)) continue;
					nis[numNodes] = ni;
					dX[numNodes] = dx;
					dY[numNodes] = dy;
					dSize[numNodes] = 0;
					dRot[numNodes] = 0;
					dTrn[numNodes] = false;
					numNodes++;
				}
				NodeInst.modifyInstances(nis, dX, dY, dSize, dSize, dRot);
			}

			// look at all arcs and move them appropriately
			for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.GEOM) continue;
				Geometric geom = h.getGeom();
				if (geom instanceof NodeInst) continue;
				ArcInst ai = (ArcInst)geom;
				Point2D pt = (Point2D)ai.getTempObj();
				if (pt.getX() != ai.getCenterX() ||
					pt.getY() != ai.getCenterY()) continue;

				// see if the arc moves in its ports
				boolean headInPort = false, tailInPort = false;
				if (!ai.isRigid() && ai.isSlidable())
				{
					headInPort = ai.stillInPort(ai.getHead(),
						new Point2D.Double(ai.getHead().getLocation().getX()+dx, ai.getHead().getLocation().getY()+dy), true);
					tailInPort = ai.stillInPort(ai.getTail(),
						new Point2D.Double(ai.getTail().getLocation().getX()+dx, ai.getTail().getLocation().getY()+dy), true);
				}

				// if both ends slide in their port, move the arc
				if (headInPort && tailInPort)
				{
					ai.modify(0, dx, dy, dx, dy);
					continue;
				}

				// if neither end can slide in its port, move the nodes
				if (!headInPort && !tailInPort)
				{
					for(int k=0; k<2; k++)
					{
						NodeInst ni;
						if (k == 0) ni = ai.getHead().getPortInst().getNodeInst(); else
							ni = ai.getTail().getPortInst().getNodeInst();
						Point2D nPt = (Point2D)ni.getTempObj();
						if (ni.getCenterX() != nPt.getX() || ni.getCenterY() != nPt.getY()) continue;

						// fix all arcs that aren't sliding
						for(Iterator oIt = Highlight.getHighlights(); oIt.hasNext(); )
						{
							Highlight oH = (Highlight)oIt.next();
							if (oH.getType() != Highlight.Type.GEOM) continue;
							Geometric oGeom = oH.getGeom();
							if (oGeom instanceof ArcInst)
							{
								ArcInst oai = (ArcInst)oGeom;
								Point2D aPt = (Point2D)oai.getTempObj();
								if (aPt.getX() != oai.getCenterX() ||
									aPt.getY() != oai.getCenterY()) continue;
								if (oai.stillInPort(oai.getHead(),
										new Point2D.Double(ai.getHead().getLocation().getX()+dx, ai.getHead().getLocation().getY()+dy), true) ||
									oai.stillInPort(oai.getTail(),
										new Point2D.Double(ai.getTail().getLocation().getX()+dx, ai.getTail().getLocation().getY()+dy), true))
											continue;
								Layout.setTempRigid(oai, true);
							}
						}
						ni.modifyInstance(dx - (ni.getCenterX() - nPt.getX()), dy - (ni.getCenterY() - nPt.getY()), 0, 0, 0);
					}
					continue;
				}

//				// only one end is slidable: move other node and the arc
//				for(int k=0; k<2; k++)
//				{
//					if (e[k] != 0) continue;
//					ni = ai->end[k].nodeinst;
//					if (ni->lowx == ni->temp1 && ni->lowy == ni->temp2)
//					{
//						// node "ni" hasn't moved yet but must because arc motion forces it
//						for(j=0; list[j] != NOGEOM; j++)
//						{
//							if (list[j]->entryisnode) continue;
//							oai = list[j]->entryaddr.ai;
//							if (oai->temp1 != (oai->end[0].xpos + oai->end[1].xpos) / 2 ||
//								oai->temp2 != (oai->end[0].ypos + oai->end[1].ypos) / 2) continue;
//							if (oai->end[0].nodeinst == ni) otherend = 1; else otherend = 0;
//							if (db_stillinport(oai, otherend, ai->end[otherend].xpos+dx,
//								ai->end[otherend].ypos+dy)) continue;
//							(void)(*el_curconstraint->setobject)((INTBIG)oai,
//								VARCINST, CHANGETYPETEMPRIGID, 0);
//						}
//						startobjectchange((INTBIG)ni, VNODEINST);
//						modifynodeinst(ni, dx-(ni->lowx-ni->temp1), dy-(ni->lowy-ni->temp2),
//							dx-(ni->lowx-ni->temp1), dy-(ni->lowy-ni->temp2), 0, 0);
//						endobjectchange((INTBIG)ni, VNODEINST);
//
//						if (ai->temp1 != (ai->end[0].xpos + ai->end[1].xpos) / 2 ||
//							ai->temp2 != (ai->end[0].ypos + ai->end[1].ypos) / 2) continue;
//						startobjectchange((INTBIG)ai, VARCINST);
//						(void)modifyarcinst(ai, 0, dx, dy, dx, dy);
//						endobjectchange((INTBIG)ai, VARCINST);
//					}
//				}
			}

			// remove coordinate objects on nodes and arcs
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.setTempObj(null);
			}
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ai.setTempObj(null);
			}
		}
	}

	/**
	 * Routine to delete all of the Geometrics in a list.
	 * @param cell the cell with the objects to be deleted.
	 * @param list a List of Geometric objects to be deleted.
	 */
	public static void eraseObjectsInList(Cell cell, List list)
	{
		FlagSet deleteFlag = Geometric.getFlagSet(2);

		// mark all nodes touching arcs that are killed
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			ni.setFlagValue(deleteFlag, 0);
		}
		for(Iterator it=list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof NodeInst) continue;
			ArcInst ai = (ArcInst)geom;
			ai.getHead().getPortInst().getNodeInst().setFlagValue(deleteFlag, 1);
			ai.getTail().getPortInst().getNodeInst().setFlagValue(deleteFlag, 1);
		}

		// also mark all nodes on arcs that will be erased
		for(Iterator it=list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				if (ni.getFlagValue(deleteFlag) != 0)
					ni.setFlagValue(deleteFlag, 2);
			}
		}

		// also mark all nodes on the other end of arcs connected to erased nodes
		for(Iterator it=list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				for(Iterator sit = ni.getConnections(); sit.hasNext(); )
				{
					Connection con = (Connection)sit.next();
					ArcInst ai = con.getArc();
					Connection otherEnd = ai.getHead();
					if (ai.getHead() == con) otherEnd = ai.getTail();
					if (otherEnd.getPortInst().getNodeInst().getFlagValue(deleteFlag) == 0)
						otherEnd.getPortInst().getNodeInst().setFlagValue(deleteFlag, 1);
				}
			}
		}

		// now kill all of the arcs
		for(Iterator it=list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)geom;

				ai.kill();
			}
		}

		// next kill all of the nodes
		for(Iterator it=list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				eraseNodeInst(ni);
			}
		}

		// kill all pin nodes that touched an arc and no longer do
		List nodesToDelete = new ArrayList();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getFlagValue(deleteFlag) == 0) continue;
			if (ni.getProto() instanceof PrimitiveNode)
			{
				if (ni.getProto().getFunction() != NodeProto.Function.PIN) continue;
				if (ni.getNumConnections() != 0 || ni.getNumExports() != 0) continue;
				nodesToDelete.add(ni);
			}
		}
		for(Iterator it = nodesToDelete.iterator(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			eraseNodeInst(ni);
		}

		// kill all unexported pin or bus nodes left in the middle of arcs
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getFlagValue(deleteFlag) == 0) continue;
			if (ni.getProto() instanceof PrimitiveNode)
			{
				if (ni.getProto().getFunction() != NodeProto.Function.PIN) continue;
				if (ni.getNumExports() != 0) continue;
//				erasePassThru(ni, FALSE, &ai);
			}
		}

		deleteFlag.freeFlagSet();
	}

	/*
	 * Routine to erase node "ni" and all associated arcs, exports, etc.
	 */
	private static void eraseNodeInst(NodeInst ni)
	{
		// erase all connecting ArcInsts on this NodeInst
		int numConnectedArcs = ni.getNumConnections();
		if (numConnectedArcs > 0)
		{
			ArcInst [] arcsToDelete = new ArcInst[numConnectedArcs];
			int i = 0;
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				arcsToDelete[i++] = con.getArc();
			}
			for(int j=0; j<numConnectedArcs; j++)
			{
				ArcInst ai = arcsToDelete[j];

				// delete the ArcInst
				ai.kill();
			}
		}

		// if this NodeInst has Exports, delete them
		undoExport(ni, null);

		// now erase the NodeInst
		ni.kill();
	}

	/*
	 * routine to recursively delete ports at nodeinst "ni" and all arcs connected
	 * to them anywhere.  If "spt" is not NOPORTPROTO, delete only that portproto
	 * on this nodeinst (and its hierarchically related ports).  Otherwise delete
	 * all portprotos on this nodeinst.
	 */
	private static void undoExport(NodeInst ni, Export spt)
	{
		int numExports = ni.getNumExports();
		if (numExports == 0) return;
		Export exportsToDelete [] = new Export[numExports];
		int i = 0;		
		for(Iterator it = ni.getExports(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			exportsToDelete[i++] = pp;
		}
		for(int j=0; j<numExports; j++)
		{
			Export pp = exportsToDelete[j];
			if (spt != null && spt != pp) continue;
			pp.kill();
		}
	}

//	typedef struct Ireconnect
//	{
//		NETWORK *net;					/* network for this reconnection */
//		INTBIG arcsfound;				/* number of arcs found on this reconnection */
//		INTBIG reconx[2], recony[2];	/* coordinate at other end of arc */
//		INTBIG origx[2], origy[2];		/* coordinate where arc hits deleted node */
//		INTBIG dx[2], dy[2];			/* distance between ends */
//		NODEINST *reconno[2];			/* node at other end of arc */
//		PORTPROTO *reconpt[2];			/* port at other end of arc */
//		ARCINST *reconar[2];			/* arcinst being reconnected */
//		ARCPROTO *ap;					/* prototype of new arc */
//		INTBIG wid;						/* width of new arc */
//		INTBIG bits;					/* user bits of new arc */
//		struct Ireconnect *nextreconnect;
//	} RECONNECT;
//
//	/**
//	 * routine to kill a node between two arcs and join the arc as one.  Returns an error
//	 * code according to its success.  If it worked, the new arc is placed in "newai".
//	 */
//	int erasePassThru(NodeInst ni, boolean allowdiffs, ArcInst **newai)
//	{
//		// disallow erasing if lock is on
//		Cell cell = ni.getParent();
//		if (us_cantedit(cell, ni, TRUE)) return(-1);
//
//		// look for pairs arcs that will get reconnected
//		firstrecon = NORECONNECT;
//		for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//		{
//			// ignore arcs that connect from the node to itself
//			ai = pi->conarcinst;
//			if (ai->end[0].nodeinst == ni && ai->end[1].nodeinst == ni) continue;
//
//			// find a "reconnect" object with this network
//			for(re = firstrecon; re != NORECONNECT; re = re->nextreconnect)
//				if (re->net == ai->network) break;
//			if (re == NORECONNECT)
//			{
//				re = (RECONNECT *)emalloc(sizeof (RECONNECT), us_tool->cluster);
//				if (re == 0) return(-1);
//				re->net = ai->network;
//				re->arcsfound = 0;
//				re->nextreconnect = firstrecon;
//				firstrecon = re;
//			}
//			j = re->arcsfound;
//			re->arcsfound++;
//			if (re->arcsfound > 2) continue;
//			re->reconar[j] = ai;
//			for(i=0; i<2; i++) if (ai->end[i].nodeinst != ni)
//			{
//				re->reconno[j] = ai->end[i].nodeinst;
//				re->reconpt[j] = ai->end[i].portarcinst->proto;
//				re->reconx[j] = ai->end[i].xpos;
//				re->origx[j] = ai->end[1-i].xpos;
//				re->dx[j] = re->reconx[j] - re->origx[j];
//				re->recony[j] = ai->end[i].ypos;
//				re->origy[j] = ai->end[1-i].ypos;
//				re->dy[j] = re->recony[j] - re->origy[j];
//			}
//		}
//
//		// examine all of the reconnection situations
//		for(re = firstrecon; re != NORECONNECT; re = re->nextreconnect)
//		{
//			if (re->arcsfound != 2) continue;
//
//			// verify that the two arcs to merge have the same type
//			if (re->reconar[0]->proto != re->reconar[1]->proto) { re->arcsfound = -1; continue; }
//			re->ap = re->reconar[0]->proto;
//
//			if (!allowdiffs)
//			{
//				// verify that the two arcs to merge have the same width
//				if (re->reconar[0]->width != re->reconar[1]->width) { re->arcsfound = -2; continue; }
//
//				// verify that the two arcs have the same slope
//				if ((re->dx[1]*re->dy[0]) != (re->dx[0]*re->dy[1])) { re->arcsfound = -3; continue; }
//				if (re->origx[0] != re->origx[1] || re->origy[0] != re->origy[1])
//				{
//					// did not connect at the same location: be sure that angle is consistent
//					if (re->dx[0] != 0 || re->dy[0] != 0)
//					{
//						if (((re->origx[0]-re->origx[1])*re->dy[0]) !=
//							(re->dx[0]*(re->origy[0]-re->origy[1]))) { re->arcsfound = -3; continue; }
//					} else if (re->dx[1] != 0 || re->dy[1] != 0)
//					{
//						if (((re->origx[0]-re->origx[1])*re->dy[1]) !=
//							(re->dx[1]*(re->origy[0]-re->origy[1]))) { re->arcsfound = -3; continue; }
//					} else { re->arcsfound = -3; continue; }
//				}
//			}
//
//			// remember facts about the new arcinst
//			re->wid = re->reconar[0]->width;
//			re->bits = re->reconar[0]->userbits | re->reconar[1]->userbits;
//
//			// special code to handle directionality
//			if ((re->bits&(ISDIRECTIONAL|ISNEGATED|NOTEND0|NOTEND1|REVERSEEND)) != 0)
//			{
//				// reverse ends if the arcs point the wrong way
//				for(i=0; i<2; i++)
//				{
//					if (re->reconar[i]->end[i].nodeinst == ni)
//					{
//						if ((re->reconar[i]->userbits&REVERSEEND) == 0)
//							re->reconar[i]->userbits |= REVERSEEND; else
//								re->reconar[i]->userbits &= ~REVERSEEND;
//					}
//				}
//				re->bits = re->reconar[0]->userbits | re->reconar[1]->userbits;
//
//				// two negations make a positive
//				if ((re->reconar[0]->userbits&ISNEGATED) != 0 &&
//					(re->reconar[1]->userbits&ISNEGATED) != 0) re->bits &= ~ISNEGATED;
//			}
//		}
//
//		// see if any reconnection will be done
//		for(re = firstrecon; re != NORECONNECT; re = re->nextreconnect)
//		{
//			retval = re->arcsfound;
//			if (retval == 2) break;
//		}
//
//		// erase the nodeinst if reconnection will be done (this will erase connecting arcs)
//		if (retval == 2) us_erasenodeinst(ni);
//
//		// reconnect the arcs
//		for(re = firstrecon; re != NORECONNECT; re = re->nextreconnect)
//		{
//			if (re->arcsfound != 2) continue;
//
//			// make the new arcinst
//			*newai = newarcinst(re->ap, re->wid, re->bits, re->reconno[0], re->reconpt[0],
//				re->reconx[0], re->recony[0], re->reconno[1], re->reconpt[1], re->reconx[1], re->recony[1],
//					cell);
//			if (*newai == NOARCINST) { re->arcsfound = -5; continue; }
//
//			(void)copyvars((INTBIG)re->reconar[0], VARCINST, (INTBIG)*newai, VARCINST, FALSE);
//			(void)copyvars((INTBIG)re->reconar[1], VARCINST, (INTBIG)*newai, VARCINST, FALSE);
//			endobjectchange((INTBIG)*newai, VARCINST);
//			(*newai)->changed = 0;
//		}
//
//		// deallocate
//		for(re = firstrecon; re != NORECONNECT; re = nextre)
//		{
//			nextre = re->nextreconnect;
//			efree((CHAR *)re);
//		}
//		return(retval);
//	}

	/**
	 * recursive helper routine for "us_copycell" which copies cell "fromnp"
	 * to a new cell called "toname" in library "tolib" with the new view type
	 * "toview".  All needed subcells are copied (unless "nosubcells" is true).
	 * All shared view cells referenced by variables are copied too
	 * (unless "norelatedviews" is true).  If "useexisting" is TRUE, any subcells
	 * that already exist in the destination library will be used there instead of
	 * creating a cross-library reference.
	 * If "move" is nonzero, delete the original after copying, and update all
	 * references to point to the new cell.  If "subdescript" is empty, the operation
	 * is a top-level request.  Otherwise, this is for a subcell, so only create a
	 * new cell if one with the same name and date doesn't already exists.
	 */
	public static Cell copyRecursively(Cell fromnp, String toname, Library tolib,
		View toview, boolean verbose, boolean move, String subdescript, boolean norelatedviews,
		boolean nosubcells, boolean useexisting)
	{
		Date fromnp_creationdate = fromnp.getCreationDate();
		Date fromnp_revisiondate = fromnp.getRevisionDate();

		// see if the cell is already there
		for(Iterator it = tolib.getCells(); it.hasNext(); )
		{
			Cell newfromnp = (Cell)it.next();
			if (!newfromnp.getProtoName().equalsIgnoreCase(toname)) continue;
			if (newfromnp.getView() != toview) continue;
			if (!newfromnp.getCreationDate().equals(fromnp.getCreationDate())) continue;
			if (!newfromnp.getRevisionDate().equals(fromnp.getRevisionDate())) continue;
			if (subdescript != null) return newfromnp;
			break;
		}

		// copy subcells
		if (!nosubcells)
		{
			boolean found = true;
			while (found)
			{
				found = false;
				for(Iterator it = fromnp.getNodes(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					NodeProto np = ni.getProto();
					if (!(np instanceof Cell)) continue;
					Cell cell = (Cell)np;

					// allow cross-library references to stay
					if (cell.getLibrary() != fromnp.getLibrary()) continue;
					if (cell.getLibrary() == tolib) continue;

					// see if the cell is already there
					if (us_indestlib(cell, tolib)) continue;

					// copy subcell if not already there
					Cell onp = copyRecursively(cell, cell.getProtoName(), tolib, cell.getView(),
						verbose, move, "subcell ", norelatedviews, nosubcells, useexisting);
					if (onp == null)
					{
						if (move) System.out.println("Move of subcell " + cell.describe() + " failed"); else
							System.out.println("Copy of subcell " + cell.describe() + " failed");
						return null;
					}
					found = true;
					break;
				}
			}
		}

		// also copy equivalent views
		if (!norelatedviews)
		{
			// first copy the icons
			boolean found = true;
			Cell fromnpwalk = fromnp;
			while (found)
			{
				found = false;
				for(Iterator it = fromnpwalk.getCellGroup().getCells(); it.hasNext(); )
				{
					Cell np = (Cell)it.next();
					if (np.getView() != View.ICON) continue;

					// see if the cell is already there
					if (us_indestlib(np, tolib)) continue;

					// copy equivalent view if not already there
//					if (move) fromnpwalk = np->nextcellgrp; // if np is moved (i.e. deleted), circular linked list is broken
					Cell onp = copyRecursively(np, np.getProtoName(), tolib, np.getView(),
						verbose, move, "alternate view ", true, nosubcells, useexisting);
					if (onp == null)
					{
						if (move) System.out.println("Move of alternate view " + np.describe() + " failed"); else
							System.out.println("Copy of alternate view " + np.describe() + " failed");
						return null;
					}
					found = true;
					break;
				}
			}

			// now copy the rest
			found = true;
			while (found)
			{
				found = false;
				for(Iterator it = fromnpwalk.getCellGroup().getCells(); it.hasNext(); )
				{
					Cell np = (Cell)it.next();
					if (np.getView() == View.ICON) continue;

					// see if the cell is already there
					if (us_indestlib(np, tolib)) continue;

					// copy equivalent view if not already there
//					if (move) fromnpwalk = np->nextcellgrp; // if np is moved (i.e. deleted), circular linked list is broken
					Cell onp = copyRecursively(np, np.getProtoName(), tolib, np.getView(),
						verbose, move, "alternate view ", true, nosubcells, useexisting);
					if (onp == null)
					{
						if (move) System.out.println("Move of alternate view " + np.describe() + " failed"); else
							System.out.println("Copy of alternate view " + np.describe() + " failed");
						return null;
					}
					found = true;
					break;
				}
			}
		}

		// see if the cell is NOW there
		Cell beforenewfromnp = null;
		Cell newfromnp = null;
		for(Iterator it = tolib.getCells(); it.hasNext(); )
		{
			Cell thisCell = (Cell)it.next();
			if (!thisCell.getProtoName().equalsIgnoreCase(toname)) continue;
			if (thisCell.getView() != toview) continue;
			if (thisCell.getCreationDate() != fromnp_creationdate) continue;
			if (!move && thisCell.getRevisionDate() != fromnp_revisiondate) continue; // moving icon of schematic changes schematic's revision date
			if (subdescript != null) return thisCell;
			newfromnp = thisCell;
			break;
		}
		if (beforenewfromnp == newfromnp || newfromnp == null)
		{
			// copy the cell
			String newname = toname;
			if (toview.getAbbreviation().length() > 0)
			{
				newname = toname + "{" + toview.getAbbreviation() + "}";
			}
			newfromnp = Cell.copynodeproto(fromnp, tolib, newname, useexisting);
			if (newfromnp == null)
			{
				if (move)
				{
					System.out.println("Move of " + subdescript + fromnp.describe() + " failed");
				} else
				{
					System.out.println("Copy of " + subdescript + fromnp.describe() + " failed");
				}
				return null;
			}

			// ensure that the copied cell is the right size
//			(*el_curconstraint->solve)(newfromnp);

			// if moving, adjust pointers and kill original cell
			if (move)
			{
				// ensure that the copied cell is the right size
//				(*el_curconstraint->solve)(newfromnp);

				// clear highlighting if the current node is being replaced
//				list = us_gethighlighted(WANTNODEINST, 0, 0);
//				for(i=0; list[i] != NOGEOM; i++)
//				{
//					if (!list[i]->entryisnode) continue;
//					ni = list[i]->entryaddr.ni;
//					if (ni->proto == fromnp) break;
//				}
//				if (list[i] != NOGEOM) us_clearhighlightcount();

				// now replace old instances with the moved one
				for(Iterator it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = (Library)it.next();
					for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell np = (Cell)cIt.next();
						boolean found = true;
						while (found)
						{
							found = false;
							for(Iterator nIt = np.getNodes(); nIt.hasNext(); )
							{
								NodeInst ni = (NodeInst)nIt.next();
								if (ni.getProto() == fromnp)
								{
									NodeInst replacedNi = ni.replace(newfromnp, false, false);
									if (replacedNi == null)
										System.out.println("Error moving node " + ni.describe() + " in cell " + np.describe());
									found = true;
									break;
								}
							}
						}
					}
				}
//				toolturnoff(net_tool, FALSE);
				us_dokillcell(fromnp);
//				toolturnon(net_tool);
				fromnp = null;
			}

			if (verbose)
			{
				if (Library.getCurrent() != tolib)
				{
					String msg = "";
					if (move) msg += "Moved "; else
						 msg += "Copied ";
					msg += subdescript + Library.getCurrent().getLibName() + ":" + newfromnp.noLibDescribe() +
						" to library " + tolib.getLibName();
					System.out.println(msg);
				} else
				{
					System.out.println("Copied " + subdescript + newfromnp.describe());
				}
			}
		}
		return newfromnp;
	}

	/*
	 * Routine to delete cell "np".  Validity checks are assumed to be made (i.e. the
	 * cell is not used and is not locked).
	 */
	private static void us_dokillcell(Cell np)
	{
//		// delete random references to this cell
//		curcell = getcurcell();
//		if (np == curcell)
//		{
//			(void)setval((INTBIG)el_curlib, VLIBRARY, x_("curnodeproto"), (INTBIG)NONODEPROTO, VNODEPROTO);
//			us_clearhighlightcount();
//		}
//
//		// close windows that reference this cell
//		for(w = el_topwindowpart; w != NOWINDOWPART; w = nextw)
//		{
//			nextw = w->nextwindowpart;
//			if (w->curnodeproto != np) continue;
//			if (w == el_curwindowpart) iscurrent = TRUE; else iscurrent = FALSE;
//			if (iscurrent)
//				(void)setvalkey((INTBIG)us_tool, VTOOL, us_current_window_key, (INTBIG)NOWINDOWPART,
//					VWINDOWPART|VDONTSAVE);
//			startobjectchange((INTBIG)us_tool, VTOOL);
//			neww = newwindowpart(w->location, w);
//			if (neww == NOWINDOWPART) return;
//
//			// adjust the new window to account for borders in the old one
//			if ((w->state&WINDOWTYPE) != DISPWINDOW)
//			{
//				neww->usehx -= DISPLAYSLIDERSIZE;
//				neww->usely += DISPLAYSLIDERSIZE;
//			}
//			if ((w->state&WINDOWTYPE) == WAVEFORMWINDOW)
//			{
//				neww->uselx -= DISPLAYSLIDERSIZE;
//				neww->usely -= DISPLAYSLIDERSIZE;
//			}
//			if ((w->state&WINDOWMODE) != 0)
//			{
//				neww->uselx -= WINDOWMODEBORDERSIZE;   neww->usehx += WINDOWMODEBORDERSIZE;
//				neww->usely -= WINDOWMODEBORDERSIZE;   neww->usehy += WINDOWMODEBORDERSIZE;
//			}
//
//			neww->curnodeproto = NONODEPROTO;
//			neww->buttonhandler = DEFAULTBUTTONHANDLER;
//			neww->charhandler = DEFAULTCHARHANDLER;
//			neww->changehandler = DEFAULTCHANGEHANDLER;
//			neww->termhandler = DEFAULTTERMHANDLER;
//			neww->redisphandler = DEFAULTREDISPHANDLER;
//			neww->state = (neww->state & ~(WINDOWTYPE|WINDOWMODE)) | DISPWINDOW;
//			killwindowpart(w);
//			endobjectchange((INTBIG)us_tool, VTOOL);
//			if (iscurrent)
//				(void)setvalkey((INTBIG)us_tool, VTOOL, us_current_window_key, (INTBIG)neww,
//					VWINDOWPART|VDONTSAVE);
//		}

//		prevversion = np->prevversion;
//		toolturnoff(net_tool, FALSE);
		np.kill();
//		toolturnon(net_tool);

//		// see if this was the latest version of a cell
//		if (prevversion != NONODEPROTO)
//		{
//			// newest version was deleted: rename next older version
//			for(ni = prevversion->firstinst; ni != NONODEINST; ni = ni->nextinst)
//			{
//				if ((ni->userbits&NEXPAND) != 0) continue;
//				startobjectchange((INTBIG)ni, VNODEINST);
//				endobjectchange((INTBIG)ni, VNODEINST);
//			}
//		}
//
//		// update status display if necessary
//		if (us_curnodeproto != NONODEPROTO && us_curnodeproto->primindex == 0)
//		{
//			if (np == us_curnodeproto)
//			{
//				if ((us_state&NONPERSISTENTCURNODE) != 0) us_setnodeproto(NONODEPROTO); else
//					us_setnodeproto(el_curtech->firstnodeproto);
//			}
//		}
	}

	/*
	 * Routine to return true if a cell like "np" exists in library "lib".
	 */
	private static boolean us_indestlib(Cell np, Library lib)
	{
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell onp = (Cell)it.next();
			if (!onp.getProtoName().equalsIgnoreCase(np.getProtoName())) continue;
			if (onp.getView() != np.getView()) continue;
			if (onp.getCreationDate() != np.getCreationDate()) continue;
			if (onp.getRevisionDate() != np.getRevisionDate()) continue;
			return true;
		}
		return false;
	}

}

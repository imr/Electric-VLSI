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
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.tool.user.ui.UIEdit;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/*
 * Class for user-level changes to the circuit.
 */

public class CircuitChanges
{
	// constructor
	CircuitChanges()
	{
	}

	/**
	 * Routine to delete all selected objects.
	 */
	public static void deleteSelected()
	{
		if (UIEdit.getNumHighlights() == 0) return;
		Geometric [] deleteList = new Geometric[UIEdit.getNumHighlights()];
		int i = 0;
		Cell cell = null;
		boolean warned = false;
		for(Iterator it = UIEdit.getHighlights(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (cell == null) cell = geom.getParent(); else
			{
				if (!warned && cell != geom.getParent())
				{
					System.out.println("Warning: Not all objects being deleted are in the same cell");
					warned = true;
				}
			}
			deleteList[i++] = geom;
		}
		UIEdit.clearHighlighting();
		Undo.startChanges(User.tool, "Delete");
		eraseObjectsInList(cell, deleteList);
		Undo.endChanges();
	}

	private static void eraseObjectsInList(Cell cell, Geometric [] list)
	{
		FlagSet deleteFlag = Geometric.getFlagSet(2);

		// mark all nodes touching arcs that are killed
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			ni.setFlagValue(deleteFlag, 0);
		}
		for(int i=0; i < list.length; i++)
		{
			Geometric geom = list[i];
			if (geom instanceof NodeInst) continue;
			ArcInst ai = (ArcInst)geom;
			ai.getHead().getPortInst().getNodeInst().setFlagValue(deleteFlag, 1);
			ai.getTail().getPortInst().getNodeInst().setFlagValue(deleteFlag, 1);
		}

		// also mark all nodes on arcs that will be erased
		for(int i=0; i < list.length; i++)
		{
			Geometric geom = list[i];
			if (geom instanceof ArcInst) continue;
			NodeInst ni = (NodeInst)geom;
			if (ni.getFlagValue(deleteFlag) != 0)
				ni.setFlagValue(deleteFlag, 2);
		}

		// also mark all nodes on the other end of arcs connected to erased nodes
		for(int i=0; i < list.length; i++)
		{
			Geometric geom = list[i];
			if (geom instanceof ArcInst) continue;
			NodeInst ni = (NodeInst)geom;
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				ArcInst ai = con.getArc();
				Connection otherEnd = ai.getHead();
				if (ai.getHead() == con) otherEnd = ai.getTail();
				if (otherEnd.getPortInst().getNodeInst().getFlagValue(deleteFlag) == 0)
					otherEnd.getPortInst().getNodeInst().setFlagValue(deleteFlag, 1);
			}
		}

		// now kill all of the arcs
		for(int i=0; i < list.length; i++)
		{
			Geometric geom = list[i];
			if (geom instanceof NodeInst) continue;
			ArcInst ai = (ArcInst)geom;

			// see if nodes need to be undrawn to account for "Steiner Point" changes
			NodeInst niH = ai.getHead().getPortInst().getNodeInst();
			NodeInst niT = ai.getTail().getPortInst().getNodeInst();
			if (niH.getFlagValue(deleteFlag) == 1 && niH.getProto().isWipeOn1or2())
				niH.startChange();
			if (niT.getFlagValue(deleteFlag) == 1 && niT.getProto().isWipeOn1or2())
				niT.startChange();

			ai.startChange();
			ai.kill();

			// see if nodes need to be redrawn to account for "Steiner Point" changes
			if (niH.getFlagValue(deleteFlag) == 1 && niH.getProto().isWipeOn1or2())
				niH.endChange();
			if (niT.getFlagValue(deleteFlag) == 1 && niT.getProto().isWipeOn1or2())
				niT.endChange();
		}

		// next kill all of the nodes
		for(int i=0; i < list.length; i++)
		{
			Geometric geom = list[i];
			if (geom instanceof ArcInst) continue;
			NodeInst ni = (NodeInst)geom;
			eraseNodeInst(ni);
		}

		// kill all pin nodes that touched an arc and no longer do
		List nodesToDelete = new ArrayList();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getFlagValue(deleteFlag) == 0) continue;
			if (ni.getProto() instanceof Cell) continue;
			if (ni.getProto().getFunction() != NodeProto.Function.PIN) continue;
			if (ni.getNumConnections() != 0 || ni.getNumExports() != 0) continue;
			nodesToDelete.add(ni);
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
			if (ni.getProto() instanceof Cell) continue;
			if (ni.getProto().getFunction() != NodeProto.Function.PIN) continue;
			if (ni.getNumExports() != 0) continue;
//			erasePassThru(ni, FALSE, &ai);
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

				// see if nodes need to be undrawn to account for "Steiner Point" changes
				NodeInst niH = ai.getHead().getPortInst().getNodeInst();
				NodeInst niT = ai.getTail().getPortInst().getNodeInst();
				if (niH.getProto().isWipeOn1or2()) niH.startChange();
				if (niT.getProto().isWipeOn1or2()) niT.startChange();

				// delete the ArcInst
				ai.startChange();
				ai.kill();

				// see if nodes need to be redrawn to account for "Steiner Point" changes
				if (niH.getProto().isWipeOn1or2()) niH.endChange();
				if (niT.getProto().isWipeOn1or2()) niT.endChange();
			}
		}

		// if this NodeInst has Exports, delete them
		ni.startChange();
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
			pp.startChange();
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

}


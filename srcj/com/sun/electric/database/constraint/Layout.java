/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Layout.java
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
package com.sun.electric.database.constraint;

import com.sun.electric.database.constraint.Constraint;
import com.sun.electric.database.change.Change;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.tool.Tool;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class Layout extends Constraint
{
	private static final Layout layoutConstraint = new Layout();

	/**
	 * The meaning of changeClock for object modification:
	 *
	 * ai.getChangeClock() <  changeClock-2  unmodified         arcs
	 * ai.getChangeClock() == changeClock-2  unmodified rigid   arcs
	 * ai.getChangeClock() == changeClock-1  unmodified unrigid arcs
	 * ai.getChangeClock() == changeClock      modified rigid   arcs
	 * ai.getChangeClock() == changeClock+1    modified unrigid arcs
	 * ni.getChangeClock() <  changeClock-1  unmodified         nodes
	 * ni.getChangeClock() == changeClock-1  size-changed       nodes
	 * ni.getChangeClock() == changeClock    position-changed   nodes
	 */
	private static int changeClock = 10;

	private Layout() {}
	
	public static Layout getConstraint() { return layoutConstraint; }

	/*
	 * routine to do hierarchical update on any cells that changed
	 */
	public void endBatch()
	{
		// if only one cell is requested, solve that
		Undo.ChangeBatch curBatch = Undo.getCurrentBatch();
//		if (np != null)
//		{
//			cla_computecell(np, false);
//		} else
//		{
//			// solve all cells that changed
//			if (curBatch != null)
//				for(cc = curBatch->firstchangecell; cc != NOCHANGECELL; cc = cc->nextchangecell)
//					cla_computecell(cc->changecell, cc->forcedlook);
//		}

		if (curBatch == null) return;
		for(Iterator it = curBatch.getChanges(); it.hasNext(); )
		{
			Undo.Change c = (Undo.Change)it.next();
			if (c.getType() == Undo.Type.NODEINSTNEW || c.getType() == Undo.Type.NODEINSTKILL ||
				c.getType() == Undo.Type.NODEINSTMOD)
			{
				NodeInst ni = (NodeInst)c.getObject();
				ni.setChange(null);
			} else if (c.getType() == Undo.Type.ARCINSTNEW || c.getType() == Undo.Type.ARCINSTKILL ||
				c.getType() == Undo.Type.ARCINSTMOD)
			{
				ArcInst ai = (ArcInst)c.getObject();
				ai.setChange(null);
			} else if (c.getType() == Undo.Type.EXPORTNEW || c.getType() == Undo.Type.EXPORTKILL ||
				c.getType() == Undo.Type.EXPORTMOD)
			{
//				((PORTPROTO *)c->entryaddr)->changeaddr = null;
			} else if (c.getType() == Undo.Type.CELLNEW || c.getType() == Undo.Type.CELLKILL ||
				c.getType() == Undo.Type.CELLMOD)
			{
//				((NODEPROTO *)c->entryaddr)->changeaddr = null;
			}
		}
	}

	/*
	 * If an export is created, touch all instances of the cell
	 */
	public void newObject(ElectricObject obj)
	{
		if (obj instanceof Export)
		{
			Export pp = (Export)obj;
			Cell np = (Cell)pp.getParent();
//			for(ni = np->firstinst; ni != NONODEINST; ni = ni->nextinst)
//				(void)db_change((int)ni, OBJECTSTART, VNODEINST, 0, 0, 0, 0, 0);
//			for(ni = np->firstinst; ni != NONODEINST; ni = ni->nextinst)
//				(void)db_change((int)ni, NODEINSTMOD, ni->lowx, ni->lowy,
//					ni->highx, ni->highy, ni->rotation, ni->transpose);
//			for(ni = np->firstinst; ni != NONODEINST; ni = ni->nextinst)
//				(void)db_change((int)ni, OBJECTEND, VNODEINST, 0, 0, 0, 0, 0);
		}
	}

	/*
	 * If an export is deleted, touch all instances of the cell
	 */
	public void killObject(ElectricObject obj)
	{
		if (obj instanceof Export)
		{
			Export pp = (Export)obj;
			Cell np = (Cell)pp.getParent();
//			for(ni = np->firstinst; ni != NONODEINST; ni = ni->nextinst)
//			{
//				(void)db_change((int)ni, OBJECTSTART, VNODEINST, 0, 0, 0, 0, 0);
//				(void)db_change((int)ni, NODEINSTMOD, ni->lowx, ni->lowy,
//					ni->highx, ni->highy, ni->rotation, ni->transpose);
//				(void)db_change((int)ni, OBJECTEND, VNODEINST, 0, 0, 0, 0, 0);
//			}
		}
	}

	/*
	 * If an export is renamed, touch all instances of the cell
	 */
	public void newVariable(ElectricObject obj, Variable.Name key, int type)
	{
//		if (type == VPORTPROTO)
//		{
//			if ((stype&VCREF) != 0)
//			{
//				name = changedvariablename(type, skey, stype);
//				if (estrcmp(name, x_("protoname")) == 0)
//				{
//					pp = (PORTPROTO *)addr;
//					np = pp->parent;
//					for(ni = np->firstinst; ni != NONODEINST; ni = ni->nextinst)
//					{
//						(void)db_change((int)ni, OBJECTSTART, VNODEINST, 0, 0, 0, 0, 0);
//						(void)db_change((int)ni, NODEINSTMOD, ni->lowx, ni->lowy,
//							ni->highx, ni->highy, ni->rotation, ni->transpose);
//						(void)db_change((int)ni, OBJECTEND, VNODEINST, 0, 0, 0, 0, 0);
//					}
//				}
//			}
//		}
	}

	/*
	 * set layout constraints on arc instance "ai" according to "changetype".
	 * the routine returns true if the arc change is not successful (or already
	 * done)
	 */
	boolean cla_layconsetobject(int addr, int type, int changetype, int changedata)
	{
//		if ((type&VTYPE) != VARCINST) return(TRUE);
//		ai = (ARCINST *)addr;
//		if (ai == NOARCINST) return(TRUE);
//		switch (changetype)
//		{
//			case CHANGETYPERIGID:				// arc rigid
//				if ((ai->userbits & FIXED) != 0) return(TRUE);
//				(void)setval((int)ai, VARCINST, x_("userbits"), ai->userbits|FIXED, VINTEGER);
//				break;
//			case CHANGETYPEUNRIGID:				// arc un-rigid
//				if ((ai->userbits & FIXED) == 0) return(TRUE);
//				(void)setval((int)ai, VARCINST, x_("userbits"), ai->userbits & ~FIXED, VINTEGER);
//				break;
//			case CHANGETYPEFIXEDANGLE:			// arc fixed-angle
//				if ((ai->userbits & FIXANG) != 0) return(TRUE);
//				(void)setval((int)ai, VARCINST, x_("userbits"), ai->userbits|FIXANG, VINTEGER);
//				break;
//			case CHANGETYPENOTFIXEDANGLE:		// arc not fixed-angle
//				if ((ai->userbits & FIXANG) == 0) return(TRUE);
//				(void)setval((int)ai, VARCINST, x_("userbits"), ai->userbits & ~FIXANG, VINTEGER);
//				break;
//			case CHANGETYPESLIDABLE:			// arc slidable
//				if ((ai->userbits & CANTSLIDE) == 0) return(TRUE);
//				(void)setval((int)ai, VARCINST, x_("userbits"), ai->userbits & ~CANTSLIDE, VINTEGER);
//				break;
//			case CHANGETYPENOTSLIDABLE:			// arc nonslidable
//				if ((ai->userbits & CANTSLIDE) != 0) return(TRUE);
//				(void)setval((int)ai, VARCINST, x_("userbits"), ai->userbits|CANTSLIDE, VINTEGER);
//				break;
//			case CHANGETYPETEMPRIGID:			// arc temporarily rigid
//				if (ai.getChangeClock() == changeClock + 2) return(TRUE);
//				ai.getChangeClock() = changeClock + 2;
//				break;
//			case CHANGETYPETEMPUNRIGID:			// arc temporarily un-rigid
//				if (ai.getChangeClock() == changeClock + 3) return(TRUE);
//				ai.getChangeClock() = changeClock + 3;
//				break;
//			case CHANGETYPEREMOVETEMP:			// remove temporarily state
//				if (ai.getChangeClock() != changeClock + 3 && ai.getChangeClock() != changeClock + 2) return(TRUE);
//				ai.getChangeClock() = changeClock - 3;
//				break;
//		}
		return false;
	}

	public void modifyNodeInst(NodeInst ni, double dCX, double dCY, double dSX, double dSY, int dRot)
	{
		// advance the change clock
		changeClock += 4;

		// change the nodeinst
		if (alterNodeInst(ni, dCX, dCY, dSX, dSY, dRot, false))
			Undo.ChangeCell.forceHierarchicalAnalysis(ni.getParent());

		// change the arcs on the nodeinst
		if (modNodeArcs(ni, dRot, dSX, dSY))
			Undo.ChangeCell.forceHierarchicalAnalysis(ni.getParent());
	}

	public void modifyNodeInsts(NodeInst [] nis, double [] dCX, double [] dCY, double [] dSX, double [] dSY, int [] dRot)
	{
		// advance the change clock
		changeClock += 4;

		// change the nodeinst
		Cell parent = null;
		for(int i=0; i<nis.length; i++)
		{
			if (alterNodeInst(nis[i], dCX[i], dCY[i], dSX[i], dSY[i], dRot[i], false))
				parent = (Cell)nis[i].getParent();
		}

		// change the arcs on the nodeinst
		for(int i=0; i<nis.length; i++)
		{
			if (modNodeArcs(nis[i], dRot[i], dSX[i], dSY[i]))
				parent = nis[i].getParent();
		}
		if (parent != null)
			Undo.ChangeCell.forceHierarchicalAnalysis(parent);
	}

	/******************** NODE MODIFICATION CODE *************************/

	/**
	 * routine to modify nodeinst "ni" by "deltalx" in low X, "deltaly" in low Y,
	 * "deltahx" in high X, "deltahy" in high Y, and "dangle" tenth-degrees.  If
	 * "announce" is true, report "start" and "end" changes on the node.
	 * If the nodeinst is a portproto of the current cell and has any arcs
	 * connected to it, the routine returns nonzero to indicate that the outer
	 * cell has ports that moved (the nodeinst has exports).
	 */
	private static boolean alterNodeInst(NodeInst ni, double deltaCX, double deltaCY, double deltaSX,
		double deltaSY, int dAngle, boolean announce)
	{
		// determine whether this is a position or size change
		int change = -1;
		if (deltaSX == 0 && deltaSY == 0)
		{
			if (deltaCX != 0 || deltaCY != 0 || dAngle != 0) change = 0;
		}

		// reject if this change has already been done
		if (ni.getChangeClock() >= changeClock+change) return false;

//		// if simple rotation on transposed nodeinst, reverse rotation
//		if (ni->transpose != 0 && dtrans == 0) dAngle = (3600 - dAngle) % 3600;

		if (ni.getChangeClock() < changeClock-1 && announce)
			Undo.newChange(ni, Undo.Type.OBJECTSTART);

		// make changes to the nodeinst
		int oldang = ni.getAngle();
		double oldCX = ni.getCenterX();
		double oldCY = ni.getCenterY();
		double oldSX = ni.getXSize();
		double oldSY = ni.getYSize();
		ni.adjustInstance(deltaCX, deltaCY, deltaSX, deltaSY, dAngle);

		// mark that this nodeinst has changed
		if (ni.getChangeClock() < changeClock-1)
		{
			Undo.Change c = Undo.newChange(ni, Undo.Type.NODEINSTMOD);
			if (c != null)
			{
				c.setDoubles(oldCX, oldCY, oldSX, oldSY, 0);
				c.setInts(oldang, 0);
			}
			ni.setChange(c);
			if (announce)
				Undo.newChange(ni, Undo.Type.OBJECTEND);
		}

		ni.setChangeClock(changeClock + change);

		// see if this nodeinst is a port of the current cell
		if (ni.getNumExports() > 0) return false;
		return true;
	}

	/**
	 * Routine to modify all of the arcs connected to a NodeInst.
	 * @param ni the NodeInst being examined.
	 * @param dAngle the change in the nodes rotation (in tenth-degrees).
	 * @param dSX the change in the node's X size (negative if mirrored).
	 * @param dSY the change in the node's Y size (negative if mirrored).
	 * @return true if some exports on the current cell have moved.
	 * This indicates that the cell must be re-examined for export locations.
	 */
	private static boolean modNodeArcs(NodeInst ni, int dangle, double dSX, double dSY)
	{
		// assume cell needs no further looks
		boolean examineCell = false;

		// next look at arcs that run within this nodeinst
		cla_modwithin(ni, dangle, dSX, dSY);

		// next look at the rest of the rigid arcs on this nodeinst
		if (cla_modrigid(ni, dangle, dSX, dSY)) examineCell = true;

		// finally, look at rest of the flexible arcs on this nodeinst
		if (modFlex(ni, dangle, dSX, dSY)) examineCell = true;

		return examineCell;
	}

	/*
	 * routine to modify the arcs that run within nodeinst "ni"
	 */
	private static void cla_modwithin(NodeInst ni, int dAngle, double dSX, double dSY)
	{
		// ignore all this stuff if the node just got created
		Undo.Change change = ni.getChange();
		if (change != null && change.getType() == Undo.Type.NODEINSTNEW) return;

		// build a list of the arcs with both ends on this nodeinst
		List interiorArcs = new ArrayList();
		for(Iterator it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			ArcInst ai = con.getArc();

			// ignore if arcinst is not within the node
			if (ai.getHead().getPortInst().getNodeInst() != ai.getTail().getPortInst().getNodeInst()) continue;
			if (ai.getChangeClock() == changeClock) continue;

			// include in the list to be considered here
			interiorArcs.add(ai);
		}

		// look for arcs with both ends on this nodeinst
		for(Iterator it = interiorArcs.iterator(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
//			if ((ai->userbits&DEADA) != 0) continue;

			// prepare transformation matrix
			AffineTransform trans = NodeInst.pureRotate(dAngle, dSX, dSY);

			// compute old center of nodeinst
			double ox = change.getA1();
			double oy = change.getA2();

			// determine the new ends of the arcinst
			adjustMatrix(ni, ai.getHead().getPortInst().getPortProto(), trans);
			Point2D.Double newHead = new Point2D.Double();
			Point2D.Double src = new Point2D.Double(ai.getHead().getLocation().getX()-ox, ai.getHead().getLocation().getY()-oy);
			trans.transform(src, newHead);

			adjustMatrix(ni, ai.getTail().getPortInst().getPortProto(), trans);
			Point2D.Double newTail = new Point2D.Double();
			src.setLocation(ai.getTail().getLocation().getX()-ox, ai.getTail().getLocation().getY()-oy);
			trans.transform(src, newTail);

			// move the arcinst
			doMoveArcInst(ai, newHead, newTail, 0);
		}
	}

	/**
	 * Routine to modify the rigid arcs connected to a NodeInst.
	 * @param ni the NodeInst being examined.
	 * @param dAngle the change in the nodes rotation (in tenth-degrees).
	 * @param dSX the change in the node's X size (negative if mirrored).
	 * @param dSY the change in the node's Y size (negative if mirrored).
	 * @return true if any nodes that have exports move.
	 * This indicates that instances of the current cell must be examined for ArcInst motion.
	 */
	private static boolean cla_modrigid(NodeInst ni, int dAngle, double dSX, double dSY)
	{
		// build a list of the rigid arcs on this nodeinst
		List rigidArcs = new ArrayList();
		for(Iterator it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			ArcInst ai = con.getArc();

			// ignore if arcinst is not flexible
			if (ai.getChangeClock() == changeClock-1 || ai.getChangeClock() == changeClock+1) continue;
			if (ai.getChangeClock() != changeClock-2 && !ai.isRigid()) continue;

			// ignore arcs that connect two ports on the same node
			if (ai.getHead().getPortInst().getNodeInst() == ai.getTail().getPortInst().getNodeInst()) continue;

			// include in the list to be considered here
			rigidArcs.add(ai);
		}

		// if simple rotation on transposed nodeinst, reverse rotation
		int nextAngle = dAngle;
//		if (((CHANGE *)ni->changeaddr)->changetype != NODEINSTNEW &&
//			((CHANGE *)ni->changeaddr)->p6 != 0 && dtrans != 0)
//				nextAngle = (3600 - dAngle) % 3600;

		// prepare transformation matrix and angle/transposition information
		AffineTransform trans = NodeInst.pureRotate(nextAngle, dSX, dSY);

		// look for rigid arcs on this nodeinst
		boolean examineCell = false;
		for(Iterator it = rigidArcs.iterator(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
//			if ((ai->userbits&DEADA) != 0) continue;
			ai.clearRigidModified();

			// if rigid arcinst has already been changed check its connectivity
			if (ai.getChangeClock() == changeClock)
			{
				ensureArcInst(ai, 0);
				continue;
			}

			// find out which end of the arcinst is where, ignore internal arcs
			Connection thisEnd = ai.getHead();   int thisEndIndex = 0;
			Connection thatEnd = ai.getTail();   int thatEndIndex = 1;
			if (thatEnd.getPortInst().getNodeInst() == ni)
			{
				thisEnd = ai.getTail();   thisEndIndex = 1;
				thatEnd = ai.getHead();   thatEndIndex = 0;
			}

			Undo.Change change = ni.getChange();
			double ox = 0, oy = 0;
			if (change.getType() != Undo.Type.NODEINSTNEW)
			{
				ox = change.getA1();
				oy = change.getA2();
				adjustMatrix(ni, thisEnd.getPortInst().getPortProto(), trans);
			}

			// create the two points that will be the new ends of this arc
			Point2D.Double [] newPts = new Point2D.Double[2];
			newPts[0] = new Point2D.Double();
			newPts[1] = new Point2D.Double();

			// figure out the new location of this arcinst connection
			Point2D.Double src = new Point2D.Double(thisEnd.getLocation().getX()-ox, thisEnd.getLocation().getY()-oy);
			trans.transform(src, newPts[thisEndIndex]);

			NodeInst ono = thatEnd.getPortInst().getNodeInst();
			PortProto opt = thatEnd.getPortInst().getPortProto();

			// figure out the new location of that arcinst connection
			src.setLocation(thatEnd.getLocation().getX()-ox, thatEnd.getLocation().getY()-oy);
			trans.transform(src, newPts[thatEndIndex]);

			// see if other nodeinst has changed
			boolean locked = false;
			if (ono.getChangeClock() == changeClock) locked = true; else
			{
				if (ono.isLocked()) locked = true; else
				{
					if (ono.getProto() instanceof Cell)
					{
						if (ono.getParent().isInstancesLocked()) locked = true;
					} else
					{
//						if ((us_useroptions&NOPRIMCHANGES) != 0 &&
//							(ono.getProto().isLockedPrim()) locked = true;
					}
				}
			}
			if (!locked)
			{
				// compute port motion within the other nodeinst (is this right? !!!)
				Point2D.Double onoPt = oldPortPosition(ono, opt);
//				oldPortPosition(ono, opt, &onox, &onoy);
				Poly oPoly = thatEnd.getPortInst().getPoly();
				double dx = oPoly.getCenterX();   double dy = oPoly.getCenterY();
				double othX = dx - onoPt.getX();
				double othY = dy - onoPt.getY();

				// figure out the new location of the other nodeinst
				src.setLocation(ono.getCenterX(), ono.getCenterY());
				Point2D.Double ptD = new Point2D.Double();
				trans.transform(src, ptD);
				dx = ptD.getX();   dy = ptD.getY();
				dx = dx - onoPt.getX() - othX;
				dy = dy - onoPt.getY() - othY;

				// move the other nodeinst
				nextAngle = dAngle;
//				if (dtrans != 0 && ono->transpose != ((CHANGE *)ni->changeaddr)->p6)
//					nextAngle = (3600 - nextAngle) % 3600;

				// ignore null motion on nodes that have already been examined
				if (dx != 0 || dy != 0 || nextAngle != 0 || ono.getChangeClock() != changeClock-1)
				{
					ai.setRigidModified();
					if (alterNodeInst(ono, dx, dy, 0, 0, nextAngle, true))
						examineCell = true;
				}
			}

			// move the arcinst
			doMoveArcInst(ai, newPts[0], newPts[1], 0);
		}

		// re-scan rigid arcs and recursively modify arcs on other nodes
		for(Iterator it = rigidArcs.iterator(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
//			if ((ai->userbits&DEADA) != 0) continue;

			// only want arcinst that was just explored
			if (!ai.isRigidModified()) continue;

			// get the other nodeinst
			Connection thisEnd = ai.getHead();   int thisEndIndex = 0;
			Connection thatEnd = ai.getTail();   int thatEndIndex = 1;
			NodeInst ono;
			if (ai.getTail().getPortInst().getNodeInst() == ni) ono = ai.getHead().getPortInst().getNodeInst(); else
				ono = ai.getTail().getPortInst().getNodeInst();

			nextAngle = dAngle;
//			if (dtrans != 0 && ((CHANGE *)ono->changeaddr)->p6 != ((CHANGE *)ni->changeaddr)->p6)
//				nextAngle = (3600 - nextAngle) % 3600;
			if (modNodeArcs(ono, nextAngle, 0, 0)) examineCell = true;
		}
		return examineCell;
	}

	/**
	 * Routine to modify the flexible arcs connected to a NodeInst.
	 * @param ni the NodeInst being examined.
	 * @param dAngle the change in the nodes rotation (in tenth-degrees).
	 * @param dSX the change in the node's X size (negative if mirrored).
	 * @param dSY the change in the node's Y size (negative if mirrored).
	 * @return true if any nodes that have exports move.
	 * This indicates that instances of the current cell must be examined for ArcInst motion.
	 */
	private static boolean modFlex(NodeInst ni, int dAngle, double dSX, double dSY)
	{
		// build a list of the flexible arcs on this nodeinst
		List flexArcs = new ArrayList();
		for(Iterator it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			ArcInst ai = con.getArc();

			// ignore if arcinst is not flexible
			if (ai.getChangeClock() == changeClock-2 || ai.getChangeClock() == changeClock) continue;
			if (ai.getChangeClock() != changeClock-1 && ai.isRigid()) continue;

			// ignore arcs that connect two ports on the same node
			if (ai.getHead().getPortInst().getNodeInst() == ai.getTail().getPortInst().getNodeInst()) continue;

			// include in the list to be considered here
			flexArcs.add(ai);
		}

		// if simple rotation on transposed nodeinst, reverse rotation
		int nextAngle = dAngle;
//		if (((CHANGE *)ni->changeaddr)->changetype != NODEINSTNEW &&
//			((CHANGE *)ni->changeaddr)->p6 != 0 && dtrans != 0)
//				nextAngle = (3600 - dAngle) % 3600;

		// prepare transformation matrix and angle/transposition information
		AffineTransform trans = NodeInst.pureRotate(nextAngle, dSX, dSY);

		// look at all of the flexible arcs on this nodeinst
		boolean examineCell = false;
		for(Iterator it = flexArcs.iterator(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
//			if ((ai->userbits&DEADA) != 0) continue;

			// if flexible arcinst has been changed, verify its connectivity
			if (ai.getChangeClock() >= changeClock+1)
			{
				ensureArcInst(ai, 1);
				continue;
			}

			// figure where each end of the arcinst is, ignore internal arcs
			Connection thisEnd = ai.getHead();   int thisEndIndex = 0;
			Connection thatEnd = ai.getTail();   int thatEndIndex = 1;
			if (thatEnd.getPortInst().getNodeInst() == ni)
			{
				thisEnd = ai.getTail();   thisEndIndex = 1;
				thatEnd = ai.getHead();   thatEndIndex = 0;
			}

			// if nodeinst motion stays within port area, ignore the arcinst
			if (ai.isSlidable() && ai.stillInPort(thisEnd, thisEnd.getLocation()))
				continue;

			Undo.Change change = ni.getChange();
			double ox = 0, oy = 0;
			if (change.getType() != Undo.Type.NODEINSTNEW)
			{
				ox = change.getA1();
				oy = change.getA2();
				adjustMatrix(ni, thisEnd.getPortInst().getPortProto(), trans);
			}

			// create the two points that will be the new ends of this arc
			Point2D.Double [] newPts = new Point2D.Double[2];
			newPts[0] = new Point2D.Double();
			newPts[1] = new Point2D.Double();

			// figure out the new location of this arcinst connection
			Point2D.Double src = new Point2D.Double(thisEnd.getLocation().getX()-ox, thisEnd.getLocation().getY()-oy);
			trans.transform(src, newPts[thisEndIndex]);

			// make sure the arc end is still in the port
			Poly poly = thisEnd.getPortInst().getPoly();
			if (poly.isInside(newPts[thisEndIndex]))
			{
				Rectangle2D.Double bbox = poly.getBox();
				if (newPts[thisEndIndex].getY() >= bbox.getMinY() && newPts[thisEndIndex].getY() <= bbox.getMaxY())
				{
					// extend arc horizontally to fit in port
					if (newPts[thisEndIndex].getX() < bbox.getMinX()) newPts[thisEndIndex].x = bbox.getMinX(); else
						if (newPts[thisEndIndex].getX() > bbox.getMaxX()) newPts[thisEndIndex].x = bbox.getMaxX();
				} else if (newPts[thisEndIndex].getX() >= bbox.getMinX() && newPts[thisEndIndex].getX() <= bbox.getMaxX())
				{
					// extend arc vertically to fit in port
					if (newPts[thisEndIndex].getY() < bbox.getMinY()) newPts[thisEndIndex].y = bbox.getMinY(); else
						if (newPts[thisEndIndex].getY() > bbox.getMaxY()) newPts[thisEndIndex].y = bbox.getMaxY();
				} else
				{
					// extend arc arbitrarily to fit in port
					Point2D.Double pt = poly.closestPoint(newPts[thisEndIndex]);
					newPts[thisEndIndex].setLocation(pt);
				}
			}

			// get other end of arcinst and its position
			NodeInst ono = thatEnd.getPortInst().getNodeInst();
			newPts[thatEndIndex].setLocation(thatEnd.getLocation());

			// see if other nodeinst has changed
			boolean mangle = true;
			if (!ai.isFixedAngle()) mangle = false; else
			{
				if (ono.isLocked()) mangle = false; else
				{
					if (ono.getProto() instanceof Cell)
					{
						if (ono.getParent().isInstancesLocked()) mangle = false;
					} else
					{
//						if ((us_useroptions&NOPRIMCHANGES) != 0 &&
//							ono.getProto().isLockedPrim()) mangle = false;
					}
				}
			}
			if (mangle)
			{
				// other nodeinst untouched, mangle it
				double dx = newPts[thisEndIndex].getX() - thisEnd.getLocation().getX();
				double dy = newPts[thisEndIndex].getY() - thisEnd.getLocation().getY();
				double odx = newPts[thatEndIndex].getX() - thatEnd.getLocation().getX();
				double ody = newPts[thatEndIndex].getY() - thatEnd.getLocation().getY();
				if (EMath.doublesEqual(thisEnd.getLocation().getX(), thatEnd.getLocation().getX()))
				{
					// null arcinst must not be explicitly horizontal
					if (!EMath.doublesEqual(thisEnd.getLocation().getY(), thatEnd.getLocation().getY()) ||
						ai.getAngle() == 900 || ai.getAngle() == 2700)
					{
						// vertical arcinst: see if it really moved in X
						if (dx == odx) dx = odx = 0;

						// move horizontal, shrink vertical
						newPts[thatEndIndex].x += dx-odx;

						// see if next nodeinst need not be moved
						if (!EMath.doublesEqual(dx, odx) && ai.isSlidable() && ai.stillInPort(thatEnd, newPts[thatEndIndex]))
							dx = odx = 0;

						// if other node already moved, don't move it any more
						if (ono.getChangeClock() >= changeClock) dx = odx = 0;

						if (dx != odx)
						{
							if (alterNodeInst(ono, dx-odx, 0, 0, 0, 0, true))
								examineCell = true;
						}
						doMoveArcInst(ai, newPts[0], newPts[1], 1);
						if (!EMath.doublesEqual(dx, odx))
							if (modNodeArcs(ono, 0, 0, 0)) examineCell = true;
						continue;
					}
				}
				if (EMath.doublesEqual(thisEnd.getLocation().getY(), thatEnd.getLocation().getY()))
				{
					// horizontal arcinst: see if it really moved in Y
					if (EMath.doublesEqual(dy, ody)) dy = ody = 0;

					// shrink horizontal, move vertical
					newPts[thatEndIndex].y += dy-ody;

					// see if next nodeinst need not be moved
					if (!EMath.doublesEqual(dy, ody) && ai.isSlidable() &&
						ai.stillInPort(thatEnd, newPts[thatEndIndex]))
							dy = ody = 0;

					// if other node already moved, don't move it any more
					if (ono.getChangeClock() >= changeClock) dx = odx = 0;

					if (!EMath.doublesEqual(dy, ody))
					{
						if (alterNodeInst(ono, 0, dy-ody, 0, 0, 0, true))
							examineCell = true;
					}
					doMoveArcInst(ai, newPts[0], newPts[1], 1);
					if (!EMath.doublesEqual(dy, ody))
						if (modNodeArcs(ono, 0, 0, 0)) examineCell = true;
					continue;
				}

				/***** THIS CODE HANDLES ALL-ANGLE RIGIDITY WITH THE FIXED-ANGLE CONSTRAINT *****/

				// special code to handle nonorthogonal fixed-angles
				nonOrthogFixAng(ai, thisEnd, thisEndIndex, thatEnd, thatEndIndex, ono, newPts);
				dx = newPts[thatEndIndex].getX() - thatEnd.getLocation().getX();
				dy = newPts[thatEndIndex].getY() - thatEnd.getLocation().getY();

				// change the arc
				updateArc(ai, newPts[0], newPts[1], 1);

				// if other node already moved, don't move it any more
				if (ono.getChangeClock() >= changeClock) dx = dy = 0;

				if (dx != 0 || dy != 0)
				{
					if (alterNodeInst(ono, dx, dy, 0, 0, 0, true))
						examineCell = true;
					if (modNodeArcs(ono, 0, 0, 0)) examineCell = true;
				}
				continue;
			}

			// other node has changed or arc is funny, just use its position
			doMoveArcInst(ai, newPts[0], newPts[1], 1);
		}

		return examineCell;
	}

	/**
	 * Routine to determine the motion of a nonorthogonal ArcInst given that one end has moved.
	 * The end that is "thisEnd" has moved to (ax[thisEndIndex],ay[thisEndIndex]), so this routine
	 * must determine the coordinates of the other end and set (ax[thatEndIndex],ay[thatEndIndex]).
	 * @param ai the nonorthogonal ArcInst that is adjusting.
	 * @param thisEnd the Connection at one end of the ArcInst.
	 * @param thisEndIndex the index (0 or 1) of "thisEnd" of the ArcInst.
	 * @param thatEnd the Connection at the other end of the ArcInst.
	 * @param thatEndIndex the index (0 or 1) of "thatEnd" of the ArcInst.
	 * @param ono the node at the other end ("thatEnd").
	 * @param newPts an array of 2 points that defines the coordinates of the two ends (0: head, 1: tail).
	 */
	private static void nonOrthogFixAng(ArcInst ai, Connection thisEnd, int thisEndIndex, Connection thatEnd, int thatEndIndex,
		NodeInst ono, Point2D.Double [] newPts)
	{
		// look for longest other arc on "ono" to determine proper end position
		double bestDist = Double.MIN_VALUE;
		ArcInst bestAI = null;
		for(Iterator it = ono.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			ArcInst oai = con.getArc();
			if (oai == ai) continue;
			if (oai.getXSize() < bestDist) continue;
			bestDist = oai.getXSize();
			bestAI = oai;
		}

		// if no other arcs, allow that end to move the same as this end
		if (bestAI == null)
		{
			newPts[thatEndIndex].x += newPts[thisEndIndex].getX() - thisEnd.getLocation().getX();
			newPts[thatEndIndex].y += newPts[thisEndIndex].getY() - thisEnd.getLocation().getY();
			return;
		}

		// compute intersection of arc "bestai" with new moved arc "ai"
		Point2D.Double inter = EMath.intersect(newPts[thisEndIndex], ai.getAngle(),
			bestAI.getHead().getLocation(), bestAI.getAngle());
		if (inter == null)
		{
			newPts[thatEndIndex].x += newPts[thisEndIndex].getX() - thisEnd.getLocation().getX();
			newPts[thatEndIndex].y += newPts[thisEndIndex].getY() - thisEnd.getLocation().getY();
			return;
		}
		newPts[thatEndIndex].setLocation(inter);
	}

	/**
	 * Routine to ensure that an ArcInst is still connected properly at each end.
	 * If it is not, the ArcInst must be jogged or adjusted.
	 * @param ai the ArcInst to check.
	 * @param arctyp the nature of the arc: 0 for rigid, 1 for flexible.
	 */
	private static void ensureArcInst(ArcInst ai, int arctyp)
	{
		// if nothing is outside port, quit
		Connection head = ai.getHead();
		Point2D.Double headPoint = head.getLocation();
		boolean inside0 = ai.stillInPort(head, headPoint);
		Connection tail = ai.getHead();
		Point2D.Double tailPoint = tail.getLocation();
		boolean inside1 = ai.stillInPort(tail, tailPoint);
		if (inside0 && inside1) return;

		// get area of the ports
		Poly headPoly = head.getPortInst().getPoly();
		Poly tailPoly = tail.getPortInst().getPoly();

		// if arcinst is not fixed-angle, run it directly to the port centers
		if (!ai.isFixedAngle())
		{
			double fx = headPoly.getCenterX();   double fy = headPoly.getCenterY();
			double tx = tailPoly.getCenterX();   double ty = tailPoly.getCenterY();
			doMoveArcInst(ai, new Point2D.Double(fx, fy), new Point2D.Double(tx, ty), arctyp);
			return;
		}

		// get bounding boxes of polygons
		Rectangle2D.Double headBounds = headPoly.getBounds2DDouble();
		Rectangle2D.Double tailBounds = tailPoly.getBounds2DDouble();
		double lx0 = headBounds.getMinX();   double hx0 = headBounds.getMaxX();
		double ly0 = headBounds.getMinY();   double hy0 = headBounds.getMaxY();
		double lx1 = tailBounds.getMinX();   double hx1 = tailBounds.getMaxX();
		double ly1 = tailBounds.getMinY();   double hy1 = tailBounds.getMaxY();

		// if manhattan path runs between the ports, adjust the arcinst
		if (lx0 <= hx1 && lx1 <= hx0)
		{
			// arcinst runs vertically
			double tx = (Math.max(lx0,lx1) + Math.min(hx0,hx1)) / 2;
			double fx = tx;
			double fy = (ly0+hy0) / 2;   double ty = (ly1+hy1) / 2;
			Point2D.Double fPt = headPoly.closestPoint(new Point2D.Double(fx, fy));
			Point2D.Double tPt = tailPoly.closestPoint(new Point2D.Double(tx, ty));
			doMoveArcInst(ai, fPt, tPt, arctyp);
			return;
		}
		if (ly0 <= hy1 && ly1 <= hy0)
		{
			// arcinst runs horizontally
			double ty = (Math.max(ly0,ly1) + Math.min(hy0,hy1)) / 2;
			double fy = ty;
			double fx = (lx0+hx0) / 2;   double tx = (lx1+hx1) / 2;
			Point2D.Double fPt = headPoly.closestPoint(new Point2D.Double(fx, fy));
			Point2D.Double tPt = tailPoly.closestPoint(new Point2D.Double(tx, ty));
			doMoveArcInst(ai, fPt, tPt, arctyp);
			return;
		}

		// give up and jog the arcinst
		double fx = headPoly.getCenterX();   double fy = headPoly.getCenterY();
		double tx = tailPoly.getCenterX();   double ty = tailPoly.getCenterY();
		doMoveArcInst(ai, new Point2D.Double(fx, fy), new Point2D.Double(tx, ty), arctyp);
	}

	/**
	 * Routine to update the coordinates of the ends of an ArcInst.
	 * @param ai the ArcInst to adjust
	 * @param headPt the new coordinates of the head of the ArcInst.
	 * @param tailPt the new coordinates of the tail of the ArcInst.
	 * @param arctyp the nature of the arc: 0 for rigid, 1 for flexible.
	 */
	private static void updateArc(ArcInst ai, Point2D.Double headPt, Point2D.Double tailPt, int arctyp)
	{
		// start changes on this arc
		Undo.newChange(ai, Undo.Type.OBJECTSTART);

		// set the proper arcinst position
		Point2D.Double oldHeadPt = ai.getHead().getLocation();
		Point2D.Double oldTailPt = ai.getTail().getLocation();
		double oldHeadX = oldHeadPt.getX();   double oldHeadY = oldHeadPt.getY();
		double oldTailX = oldTailPt.getX();   double oldTailY = oldTailPt.getY();
//System.out.println("modify arc "+ai.describe()+" was ("+oldHeadX+","+oldHeadY+")-("+oldTailPt.getX()+","+oldTailPt.getY()+
//	") is ("+headPt.getX()+","+headPt.getY()+")-("+tailPt.getX()+","+tailPt.getY()+")");
		ai.lowLevelMove(0, headPt.getX() - oldHeadX, headPt.getY() - oldHeadY, tailPt.getX() - oldTailX, tailPt.getY() - oldTailY);

		// if the arc hasn't changed yet, record this change
		if (ai.getChange() == null)
		{
			Undo.Change change = Undo.newChange(ai, Undo.Type.ARCINSTMOD);
			change.setDoubles(oldHeadX, oldHeadY, oldTailX, oldTailY, ai.getWidth());
			ai.setChange(change);
			ai.setChangeClock(changeClock + arctyp);
		}

		// end changes on this arc
		Undo.newChange(ai, Undo.Type.OBJECTEND);
	}

	/**
	 * Routine to move the coordinates of the ends of an ArcInst.
	 * If the arc cannot be moved in this way, it will be broken up into 3 jogged arcs.
	 * @param ai the ArcInst to adjust
	 * @param headPt the new coordinates of the head of the ArcInst.
	 * @param tailPt the new coordinates of the tail of the ArcInst.
	 * @param arctyp the nature of the arc: 0 for rigid, 1 for flexible.
	 */
	private static void doMoveArcInst(ArcInst ai, Point2D.Double headPt, Point2D.Double tailPt, int arctyp)
	{
		// check for null arcinst motion
		Connection head = ai.getHead();
		Connection tail = ai.getTail();
		if (headPt.equals(head.getLocation()) && tailPt.equals(tail.getLocation()))
		{
			// only ignore null motion on fixed-angle requests
			if (arctyp != 0) return;
		}

		// if the angle is the same or doesn't need to be, simply make the change
		if (!ai.isRigid() ||
			(ai.isRigid() && ai.getChangeClock() != changeClock-1) ||
			ai.getChangeClock() == changeClock-2 ||
			headPt.equals(tailPt) ||
			(ai.getAngle() % 1800) == (EMath.figureAngle(headPt, tailPt) % 1800))
		{
			updateArc(ai, headPt, tailPt, arctyp);
			return;
		}

		// manhattan arcinst becomes nonmanhattan: remember facts about it
		PortInst fpi = head.getPortInst();
		NodeInst fno = fpi.getNodeInst();   PortProto fpt = fpi.getPortProto();
		PortInst tpi = tail.getPortInst();
		NodeInst tno = tpi.getNodeInst();   PortProto tpt = tpi.getPortProto();

		ArcProto ap = ai.getProto();   Cell pnt = ai.getParent();   double wid = ai.getWidth();

		// figure out what nodeinst proto connects these arcs
		PrimitiveNode np = ((PrimitiveArc)ap).findPinProto();
		double psx = np.getDefWidth();
		double psy = np.getDefHeight();

		// replace it with three arcs and two nodes
		NodeInst no1 = null, no2 = null;
		if (EMath.doublesEqual(head.getLocation().getX(), tail.getLocation().getX()))
		{
			// arcinst was vertical
			double oldyA = (tailPt.getY()+headPt.getY()) / 2;
			double oldyB = oldyA;
			double oldxA = headPt.getX();   double oldxB = tailPt.getX();
			no1 = NodeInst.newInstance(np, new Point2D.Double(oldxB, oldyB),psx, psy, 0, pnt);
			no2 = NodeInst.newInstance(np, new Point2D.Double(oldxA, oldyA),psx, psy, 0, pnt);
		} else
		{
			// assume horizontal arcinst
			double oldyA = headPt.getY();   double oldyB = tailPt.getY();
			double oldxA = (tailPt.getX()+headPt.getX()) / 2;
			double oldxB = oldxA;
			no1 = NodeInst.newInstance(np, new Point2D.Double(oldxB, oldyB),psx, psy, 0, pnt);
			no2 = NodeInst.newInstance(np, new Point2D.Double(oldxA, oldyA),psx, psy, 0, pnt);
		}
		if (no1 == null || no2 == null)
		{
			System.out.println("Problem creating jog pins");
			return;
		}
		Undo.newChange(no1, Undo.Type.OBJECTEND);
		Undo.newChange(no2, Undo.Type.OBJECTEND);

		Iterator it = np.getPorts();
		PortProto pp = (PortProto)it.next();
		PortInst no1pi = no1.getOnlyPortInst();
		Rectangle2D.Double no1Bounds = no1pi.getPoly().getBounds2DDouble();
		Point2D.Double no1Pt = new Point2D.Double(no1Bounds.getCenterX(), no1Bounds.getCenterY());

		PortInst no2pi = no2.getOnlyPortInst();
		Rectangle2D.Double no2Bounds = no2pi.getPoly().getBounds2DDouble();
		Point2D.Double no2Pt = new Point2D.Double(no2Bounds.getCenterX(), no2Bounds.getCenterY());

		ArcInst ar1 = ArcInst.newInstance(ap, wid, fpi, head.getLocation(), no2pi, no2Pt);
		ar1.copyStateBits(ai);
		ArcInst ar2 = ArcInst.newInstance(ap, wid, no2pi, no2Pt, no1pi, no1Pt);
		ar2.copyStateBits(ai);   ar2.clearNegated();
		ArcInst ar3 = ArcInst.newInstance(ap, wid, no1pi, no1Pt, tpi, tail.getLocation());
		ar3.copyStateBits(ai);   ar3.clearNegated();
		if (ar1 == null || ar2 == null || ar3 == null)
		{
			System.out.println("Problem creating jog arcs");
			return;
		}
//		(void)copyvars((INTBIG)ai, VARCINST, (INTBIG)ar2, VARCINST, FALSE);
		Undo.newChange(ar1, Undo.Type.OBJECTEND);
		Undo.newChange(ar2, Undo.Type.OBJECTEND);
		Undo.newChange(ar3, Undo.Type.OBJECTEND);
		ar1.setChangeClock(changeClock + arctyp);
		ar2.setChangeClock(changeClock + arctyp);
		ar3.setChangeClock(changeClock + arctyp);

		// now kill the arcinst
		Undo.newChange(ai, Undo.Type.OBJECTSTART);
//		if ((CHANGE *)ai->changeaddr != NOCHANGE)
//		{
//			ai->end[0].xpos = ((CHANGE *)ai->changeaddr)->p1;
//			ai->end[0].ypos = ((CHANGE *)ai->changeaddr)->p2;
//			ai->end[1].xpos = ((CHANGE *)ai->changeaddr)->p3;
//			ai->end[1].ypos = ((CHANGE *)ai->changeaddr)->p4;
//			ai->length = computedistance(ai->end[0].xpos, ai->end[0].ypos,
//				ai->end[1].xpos, ai->end[1].ypos);
//			ai->width = ((CHANGE *)ai->changeaddr)->p5;
//			determineangle(ai);
//		}
		ai.kill();
	}

	/*
	 * routine to adjust the transformation matrix "trans" by placing translation
	 * information for nodeinst "ni", port "pp".
	 *
	 * there are only two types of nodeinst changes: internal and external.
	 * The internal changes are scaling and port motion changes that
	 * are usually caused by other changes within the cell.  The external
	 * changes are rotation and transposition.  These two changes never
	 * occur at the same time.  There is also translation change that
	 * can occur at any time and is of no importance here.  What is
	 * important is that the transformation matrix "trans" handles
	 * the external changes and internal changes.  External changes are already
	 * set by the "makeangle" subroutine and internal changes are
	 * built into the matrix here.
	 */
	private static void adjustMatrix(NodeInst ni, PortProto pp, AffineTransform trans)
	{
		double m00 = trans.getScaleX();
		double m01 = trans.getShearX();
		double m11 = trans.getScaleY();
		double m10 = trans.getShearY();
		double m02 = ni.getCenterX();
		double m12 = ni.getCenterY();
		Undo.Change change = ni.getChange();
		if (change.getA3() < 0 || change.getA4() < 0 || change.getA5() != 0)
		{
			// nodeinst did not rotate: adjust for port motion
			Point2D.Double ono = oldPortPosition(ni, pp);
			Poly curPoly = ni.getShapeOfPort(pp);
			double dx = curPoly.getCenterX();
			double dy = curPoly.getCenterY();
			double ox = change.getA1();
			double oy = change.getA2();
			m02 = dx - ono.getX() + ox;   m12 = dy - ono.getY() + oy;
		}
		trans.setTransform(m00, m10, m01, m11, m02, m12);
	}

	/*
	 * routine to compute the position of portproto "pp" on nodeinst "ni" and
	 * place the center of the area in the parameters "x" and "y".  The position
	 * is the "old" position, as determined by any changes that may have occured
	 * to the nodeinst (and any sub-nodes).
	 */
	private static Point2D.Double oldPortPosition(NodeInst ni, PortProto pp)
	{
		// descend to the primitive node
		AffineTransform subrot = makeOldRot(ni);

		NodeInst bottomNi = ni;
		PortProto bottomPP = pp;
		while (bottomNi.getProto() instanceof Cell)
		{
			AffineTransform localtran = makeOldTrans(bottomNi);
			subrot.concatenate(localtran);

//			Undo.Change change = pp.getChange();
//			if (change != null && change.getType() == Undo.Type.PORTPROTOMOD)
//			{
//				bottomNi = (NODEINST *)((CHANGE *)pp->changeaddr)->p1;
//				bottomPP = (PORTPROTO *)((CHANGE *)pp->changeaddr)->p2;
//			} else
			{
				bottomNi = ((Export)bottomPP).getOriginalPort().getNodeInst();
				bottomPP = ((Export)bottomPP).getOriginalPort().getPortProto();
			}
			localtran = makeOldRot(bottomNi);
			subrot.concatenate(localtran);
		}

		// if the node hasn't changed, use its current values
		Undo.Change change = bottomNi.getChange();
		if (change != null && change.getType() == Undo.Type.NODEINSTMOD)
		{
			// get the old values
			double cX = change.getA1();
			double cY = change.getA2();
			double sX = change.getA3();
			double sY = change.getA4();
			int angle = change.getI1();

			// create a fake node with these values
			NodeInst oldNi = NodeInst.lowLevelAllocate();
			oldNi.lowLevelPopulate(bottomNi.getProto(), new Point2D.Double(cX, cY), sX, sY, angle, bottomNi.getParent());
			bottomNi = oldNi;
		}
		PrimitiveNode np = (PrimitiveNode)bottomNi.getProto();
		Technology tech = np.getTechnology();
		Poly poly = tech.getShapeOfPort(ni, (PrimitivePort)bottomPP);
		poly.transform(subrot);
		double x = poly.getCenterX();
		double y = poly.getCenterY();
		return new Point2D.Double(x, y);
	}

	private static AffineTransform makeOldRot(NodeInst ni)
	{
		// if the node has not been modified, just use the current transformation
		Undo.Change change = ni.getChange();
		if (change == null || change.getType() != Undo.Type.NODEINSTMOD)
			return ni.rotateOut();

		// get the old values
		double cX = change.getA1();
		double cY = change.getA2();
		double sX = change.getA3();
		double sY = change.getA4();
		int angle = change.getI1();

		// create a fake node with these values
		NodeInst oldNi = NodeInst.lowLevelAllocate();
		oldNi.lowLevelPopulate(ni.getProto(), new Point2D.Double(cX, cY), sX, sY, angle, ni.getParent());

		// use the fake node to determine the former transformation matrix
		return oldNi.rotateOut();
	}

	private static AffineTransform makeOldTrans(NodeInst ni)
	{
		// get current values
		Cell np = (Cell)ni.getProto();
		double cX = ni.getCenterX();   double cY = ni.getCenterY();
		Rectangle2D.Double cellBounds = np.getBounds();
		double pCX = cellBounds.getCenterX();   double pCY = cellBounds.getCenterY();

		// set to previous values if they changed
		Undo.Change change = ni.getChange();
		if (change != null && change.getType() == Undo.Type.NODEINSTMOD)
		{
			cX = change.getA1();
			cY = change.getA2();
		}
		change = np.getChange();
		if (change != null && change.getType() == Undo.Type.CELLMOD)
		{
			pCX = change.getA1();
			pCY = change.getA2();
		}

		// create the former translation matrix (this hack is stolen from NodeInst.translateOut())
		double dx = cX - pCX;
		double dy = cY - pCY;
		AffineTransform transform = new AffineTransform();
		transform.translate(dx, dy);
		return transform;
	}

	/*
	 * routine to re-compute the bounds of the cell "cell" (because an object
	 * has been added or removed from it) and store these bounds in the nominal
	 * size and the size of each instantiation of the cell.  It is also necessary
	 * to re-position each instantiation of the cell in its proper position list.
	 * If "forcedlook" is true, the cell is re-examined regardless of
	 * whether its size changed.
	 */
//	void cla_computecell(NODEPROTO *cell, BOOLEAN forcedlook)
//	{
//		REGISTER NODEINST *ni, *lni;
//		REGISTER NODEPROTO *np, *oneparent;
//		INTBIG nlx, nhx, nly, nhy, offx, offy;
//		XARRAY trans;
//		REGISTER INTBIG dlx, dly, dhx, dhy, flx, fhx, fly, fhy;
//		REGISTER BOOLEAN mixed;
//		REGISTER CHANGE *c;
//		REGISTER LIBRARY *lib;
//
//		// get current boundary of cell
//		db_boundcell(cell, &nlx,&nhx, &nly,&nhy);
//
//		// quit if it has not changed
//		if (cell->lowx == nlx && cell->highx == nhx && cell->lowy == nly &&
//			cell->highy == nhy && !forcedlook) return;
//
//		// advance the change clock
//		changeClock += 4;
//
//		// get former size of cell from change information
//		flx = cell->lowx;   fhx = cell->highx;
//		fly = cell->lowy;   fhy = cell->highy;
//		c = (CHANGE *)cell->changeaddr;
//		if (c != NOCHANGE && c->changetype == NODEPROTOMOD)
//		{
//			// modification changes carry original size
//			flx = c->p1;   fhx = c->p2;
//			fly = c->p3;   fhy = c->p4;
//		}
//
//		// update the cell size
//		cell->lowx = nlx;   cell->highx = nhx;
//		cell->lowy = nly;   cell->highy = nhy;
//		cell->changeaddr = (CHAR *)db_change((INTBIG)cell, NODEPROTOMOD, flx, fhx, fly, fhy, 0, 0);
//
//		// see if all instances of this cell are in the same location
//		mixed = FALSE;
//		oneparent = NONODEPROTO;
//		lni = NONODEINST;
//		for(ni = cell->firstinst; ni != NONODEINST; ni = ni->nextinst)
//		{
//			oneparent = ni->parent;
//			if (lni != NONODEINST) if (ni->parent != lni->parent) mixed = TRUE;
//			lni = ni;
//		}
//
//		// if there are no constrained instances of the cell, no change
//		if (oneparent == NONODEPROTO) return;
//
//		// if all parent cells the same, make changes to the instances
//		if (!mixed && !forcedlook)
//		{
//			dlx = cell->lowx - flx;   dhx = cell->highx - fhx;
//			dly = cell->lowy - fly;   dhy = cell->highy - fhy;
//			for(ni = cell->firstinst; ni != NONODEINST; ni = ni->nextinst)
//			{
//				makeangle(ni->rotation, ni->transpose, trans);
//				xform(dhx+dlx, dhy+dly, &offx, &offy, trans);
//				nlx = (dlx-dhx+offx)/2;  nhx = offx - nlx;
//				nly = (dly-dhy+offy)/2;  nhy = offy - nly;
//				if (alterNodeInst(ni, nlx, nly, nhx, nhy, 0, 0, TRUE)) forcedlook = TRUE;
//			}
//			for(ni = cell->firstinst; ni != NONODEINST; ni = ni->nextinst)
//				if (modNodeArcs(ni, 0, 0)) forcedlook = TRUE;
//			cla_computecell(oneparent, forcedlook);
//			return;
//		}
//
//		/*
//		 * if instances are scattered or port motion has occured, examine
//		 * entire database in proper recursive order and adjust cell sizes
//		 */
//		for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//			for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//				np->userbits &= ~(CELLMOD|CELLNOMOD);
//		cell->userbits |= CELLMOD;
//		for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//			for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//		{
//			// only want cells with no instances as roots of trees
//			if (np->firstinst != NONODEINST) continue;
//
//			// now look recursively at the nodes in this cell
//			(void)cla_lookdown(np);
//		}
//	}

//	BOOLEAN cla_lookdown(NODEPROTO *start)
//	{
//		REGISTER NODEINST *ni;
//		REGISTER NODEPROTO *np;
//		REGISTER INTBIG dlx, dhx, dly, dhy, flx, fhx, fly, fhy;
//		REGISTER BOOLEAN forcedlook, foundone;
//		INTBIG nlx, nhx, nly, nhy, offx, offy;
//		XARRAY trans;
//
//		// first look recursively to the bottom to see if this cell changed
//		for(ni = start->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//			if (ni->proto->primindex == 0) ni->userbits |= MARKN; else
//				ni->userbits &= ~MARKN;
//
//		foundone = TRUE;
//		while (foundone)
//		{
//			foundone = FALSE;
//			for(ni = start->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//			{
//				if ((ni->userbits & MARKN) == 0) continue;
//				ni->userbits &= ~MARKN;
//				np = ni->proto;
//
//				// ignore recursive references (showing icon in contents)
//				if (isiconof(np, start)) continue;
//
//				// if this nodeinst is to change, mark the parent cell also
//				if ((np->userbits & CELLMOD) != 0) start->userbits |= CELLMOD;
//
//				// don't look inside if the cell is certified
//				if ((np->userbits & (CELLNOMOD|CELLMOD)) != 0) continue;
//
//				// look inside nodeinst to see if it changed
//				if (cla_lookdown(np)) start->userbits |= CELLMOD;
//				foundone = TRUE;
//			}
//		}
//
//		// if this cell did not change, certify so and quit
//		if ((start->userbits & CELLMOD) == 0)
//		{
//			start->userbits |= CELLNOMOD;
//			return(FALSE);
//		}
//
//		// mark those nodes that must change
//		for(ni = start->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			np = ni->proto;
//			ni->userbits &= ~(MARKN|TOUCHN);
//			if (np->primindex != 0) continue;
//			if (isiconof(np, start)) continue;
//			if ((np->userbits & CELLMOD) == 0) continue;
//			ni->userbits |= MARKN | TOUCHN;
//		}
//
//		// modify the nodes in this cell that changed
//		forcedlook = FALSE;
//		foundone = TRUE;
//		while (foundone)
//		{
//			foundone = FALSE;
//			for(ni = start->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//			{
//				if ((ni->userbits & MARKN) == 0) continue;
//				ni->userbits &= ~MARKN;
//				np = ni->proto;
//
//				// determine original size of cell
//				if ((CHANGE *)np->changeaddr != NOCHANGE &&
//					((CHANGE *)np->changeaddr)->changetype == NODEPROTOMOD)
//				{
//					// modification changes carry original size
//					flx = ((CHANGE *)np->changeaddr)->p1;
//					fhx = ((CHANGE *)np->changeaddr)->p2;
//					fly = ((CHANGE *)np->changeaddr)->p3;
//					fhy = ((CHANGE *)np->changeaddr)->p4;
//				} else
//				{
//					// creation changes have no original size: use current size
//					flx = np->lowx;   fhx = np->highx;
//					fly = np->lowy;   fhy = np->highy;
//				}
//				if (ni->highx-ni->lowx == np->highx-np->lowx && ni->highy-ni->lowy == np->highy-np->lowy)
//					nlx = nhx = nly = nhy = 0; else
//				{
//					dlx = np->lowx - flx;   dhx = np->highx - fhx;
//					dly = np->lowy - fly;   dhy = np->highy - fhy;
//					makeangle(ni->rotation, ni->transpose, trans);
//					xform(dhx+dlx, dhy+dly, &offx, &offy, trans);
//					nlx = (dlx-dhx+offx)/2;  nhx = offx - nlx;
//					nly = (dly-dhy+offy)/2;  nhy = offy - nly;
//				}
//				if (alterNodeInst(ni, nlx, nly, nhx, nhy, 0, 0, TRUE)) forcedlook = TRUE;
//				foundone = TRUE;
//			}
//		}
//
//		// now change the arcs in the nodes in this cell that changed
//		foundone = TRUE;
//		while (foundone)
//		{
//			foundone = FALSE;
//			for(ni = start->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//			{
//				if ((ni->userbits & TOUCHN) == 0) continue;
//				ni->userbits &= ~TOUCHN;
//				if (modNodeArcs(ni, 0, 0)) forcedlook = TRUE;
//				foundone = TRUE;
//			}
//		}
//
//		// now change the size of this cell
//		db_boundcell(start, &nlx,&nhx, &nly,&nhy);
//
//		// quit if it has not changed
//		if (start->lowx == nlx && start->highx == nhx && start->lowy == nly &&
//			start->highy == nhy && !forcedlook)
//		{
//			start->userbits |= CELLNOMOD;
//			return(FALSE);
//		}
//
//		// update the cell size
//		start->changeaddr = (CHAR *)db_change((INTBIG)start, NODEPROTOMOD,
//			start->lowx, start->highx, start->lowy, start->highy, 0, 0);
//		start->lowx = nlx;  start->highx = nhx;
//		start->lowy = nly;  start->highy = nhy;
//		return(TRUE);
//	}
}

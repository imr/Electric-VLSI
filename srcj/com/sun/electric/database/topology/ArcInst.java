/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ArcInst.java
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
package com.sun.electric.database.topology;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.util.Iterator;

/**
 * An ArcInst is an instance of an ArcProto (a wire type)
 * An ArcInst points to its prototype, the Cell on which it has been
 * instantiated, and the connection at either end of the wire.
 * The geometry of the wire (width and length) is captured in the
 * bounds of the Geometric portion of this object.
 * <P>
 * ArcInst objects have properties that constrain them.  Here is the notion of "Fixed angle":
 * <P>
 * <CENTER><IMG SRC="doc-files/ArcInst-1.gif"></CENTER>
 * <P>
 * Here is the notion of rigid arcs:
 * <P>
 * <CENTER><IMG SRC="doc-files/ArcInst-2.gif"></CENTER>
 * <P>
 * Here is the notion of slidable arcs:
 * <P>
 * <CENTER><IMG SRC="doc-files/ArcInst-3.gif"></CENTER>
 * <P>
 * Constraints propagate hierarchically:
 * <P>
 * <CENTER><IMG SRC="doc-files/ArcInst-4.gif"></CENTER>
 */
public class ArcInst extends Geometric
{
	/** The index of the head of this ArcInst. */		public static final int HEADEND = 0;
	/** The index of the tail of this ArcInst. */		public static final int TAILEND = 1;

	// -------------------------- private data ----------------------------------

	/** fixed-length arc */								private static final int FIXED =                     01;
	/** fixed-angle arc */								private static final int FIXANG =                    02;
//	/** arc has text that is far away */				private static final int AHASFARTEXT =               04;
//	/** arc is not in use */							private static final int DEADA =                    020;
	/** angle of arc from end 0 to end 1 */				private static final int AANGLE =                037740;
	/** bits of right shift for AANGLE field */			private static final int AANGLESH =                   5;
//	/** set if arc is to be drawn shortened */			private static final int ASHORT =                040000;
	/** set if ends do not extend by half width */		private static final int NOEXTEND =             0400000;
	/** set if ends are negated */						private static final int ISNEGATED =           01000000;
	/** set if arc aims from end 0 to end 1 */			private static final int ISDIRECTIONAL =       02000000;
	/** no extension/negation/arrows on end 0 */		private static final int NOTEND0 =             04000000;
	/** no extension/negation/arrows on end 1 */		private static final int NOTEND1 =            010000000;
	/** reverse extension/negation/arrow ends */		private static final int REVERSEEND =         020000000;
	/** set if arc can't slide around in ports */		private static final int CANTSLIDE =          040000000;
	/** set if afixed arc was changed */				private static final int FIXEDMOD =          0100000000;
//	/** if on, this arcinst is marked for death */		private static final int KILLA =             0200000000;
//	/** arcinst re-drawing is scheduled */				private static final int REWANTA =           0400000000;
//	/** only local arcinst re-drawing desired */		private static final int RELOCLA =          01000000000;
//	/**transparent arcinst re-draw is done */			private static final int RETDONA =          02000000000;
//	/** opaque arcinst re-draw is done */				private static final int REODONA =          04000000000;
//	/** general flag for spreading and highlighting */	private static final int ARCFLAGBIT =      010000000000;
	/** set if hard to select */						private static final int HARDSELECTA =     020000000000;

	// Name of the variable holding the ArcInst's name.
	public static final String VAR_ARC_NAME = "ARC_name";

	/** width of this arc instance */					private double arcWidth;
	/** prototype of this arc instance */				private ArcProto protoType;
	/** end connections of this arc instance */			private Connection [] ends;
	/** true if this ArcInst is linked to parent */		private boolean linked;

	/**
	 * The constructor is never called.  Use the factory "newInstance" instead.
	 */
	private ArcInst() {}

	/****************************** CREATE, DELETE, MODIFY ******************************/

	/**
	 * Routine to create a new ArcInst with appropriate defaults, connecting two PortInsts.
	 * Since no coordinates are given, the ArcInst connects to the center of the PortInsts.
	 * @param type the prototype of the new ArcInst.
	 * @param width the width of the new ArcInst.  The width must be > 0.
	 * @param head the head end PortInst.
	 * @param tail the tail end PortInst.
	 * @param name the name of the new ArcInst
	 * @return the newly created ArcInst, or null if there is an error.
	 */
	public static ArcInst makeInstance(ArcProto type, double width, PortInst head, PortInst tail, String name)
	{
		ArcInst ai = newInstance(type, width, head, tail, name);
		if (ai != null)
		{
			// set default information from the prototype
			if (type.isRigid()) ai.setRigid();
			if (type.isFixedAngle()) ai.setFixedAngle();
			if (type.isSlidable()) ai.setSlidable();
			if (type.isExtended()) ai.setExtended();
			if (type.isNegated()) ai.setNegated();
			if (type.isDirectional()) ai.setDirectional();
		}
		return ai;
	}

	/**
	 * Routine to create a new ArcInst with appropriate defaults, connecting two PortInsts at specified locations.
	 * This is more general than the version that does not take coordinates.
	 * @param type the prototype of the new ArcInst.
	 * @param width the width of the new ArcInst.  The width must be > 0.
	 * @param head the head end PortInst.
	 * @param headPt the coordinate of the head end PortInst.
	 * @param tail the tail end PortInst.
	 * @param tailPt the coordinate of the tail end PortInst.
	 * @param name the name of the new ArcInst
	 * @return the newly created ArcInst, or null if there is an error.
	 */
	public static ArcInst makeInstance(ArcProto type, double width,
		PortInst head, Point2D headPt, PortInst tail, Point2D tailPt, String name)
	{
		ArcInst ai = newInstance(type, width, head, headPt, tail, tailPt, name);
		if (ai != null)
		{
			// set default information from the prototype
			if (type.isRigid()) ai.setRigid();
			if (type.isFixedAngle()) ai.setFixedAngle();
			if (type.isSlidable()) ai.setSlidable();
			if (type.isExtended()) ai.setExtended();
			if (type.isNegated()) ai.setNegated();
			if (type.isDirectional()) ai.setDirectional();
		}
		return ai;
	}

	/**
	 * Routine to create a new ArcInst connecting two PortInsts.
	 * Since no coordinates are given, the ArcInst connects to the center of the PortInsts.
	 * @param type the prototype of the new ArcInst.
	 * @param width the width of the new ArcInst.  The width must be > 0.
	 * @param head the head end PortInst.
	 * @param tail the tail end PortInst.
	 * @param name the name of the new ArcInst
	 * @return the newly created ArcInst, or null if there is an error.
	 */
	public static ArcInst newInstance(ArcProto type, double width, PortInst head, PortInst tail, String name)
	{
		Rectangle2D headBounds = head.getBounds();
		double headX = headBounds.getCenterX();
		double headY = headBounds.getCenterY();
		Rectangle2D tailBounds = tail.getBounds();
		double tailX = tailBounds.getCenterX();
		double tailY = tailBounds.getCenterY();
		return newInstance(type, width, head, new Point2D.Double(headX, headY), tail, new Point2D.Double(tailX, tailY), name);
	}

	/**
	 * Routine to create a new ArcInst connecting two PortInsts at specified locations.
	 * This is more general than the version that does not take coordinates.
	 * @param type the prototype of the new ArcInst.
	 * @param width the width of the new ArcInst.  The width must be > 0.
	 * @param head the head end PortInst.
	 * @param headPt the coordinate of the head end PortInst.
	 * @param tail the tail end PortInst.
	 * @param tailPt the coordinate of the tail end PortInst.
	 * @param name the name of the new ArcInst
	 * @return the newly created ArcInst, or null if there is an error.
	 */
	public static ArcInst newInstance(ArcProto type, double width,
		PortInst head, Point2D headPt, PortInst tail, Point2D tailPt, String name)
	{
		ArcInst ai = lowLevelAllocate();
		if (ai.lowLevelPopulate(type, width, head, headPt, tail, tailPt)) return null;
		if (!ai.stillInPort(ai.getHead(), headPt, false))
		{
			Cell parent = head.getNodeInst().getParent();
			System.out.println("Error in cell " + parent.describe() + ": head of " + type.getProtoName() +
				" arc at (" + headPt.getX() + "," + headPt.getY() + ") does not fit in port");
			return null;
		}
		if (!ai.stillInPort(ai.getTail(), tailPt, false))
		{
			Cell parent = tail.getNodeInst().getParent();
			System.out.println("Error in cell " + parent.describe() + ": tail of " + type.getProtoName() +
				" arc at (" + tailPt.getX() + "," + tailPt.getY() + ") does not fit in port");
			return null;
		}
		if (name != null) ai.setName(name);
		if (ai.lowLevelLink()) return null;

		// handle change control, constraint, and broadcast
		if (Undo.recordChange())
		{
			// tell all tools about this ArcInst
			Undo.Change ch = Undo.newChange(ai, Undo.Type.ARCINSTNEW);

			// tell constraint system about new ArcInst
//			(*el_curconstraint->newobject)((INTBIG)ni, VARCINST);
		}
		return ai;
	}

	/**
	 * Routine to delete this ArcInst.
	 */
	public void kill()
	{
		// remove the arc
		lowLevelUnlink();

		// handle change control, constraint, and broadcast
		if (Undo.recordChange())
		{
			// tell all tools about this ArcInst
			Undo.Change ch = Undo.newChange(this, Undo.Type.ARCINSTKILL);

			// tell constraint system about killed ArcInst
//			(*el_curconstraint->killobject)((INTBIG)ni, VARCINST);
		}
	}

	/**
	 * Routine to change the width and end locations of this ArcInst.
	 * @param dWidth the change to the ArcInst width.
	 * @param dHeadX the change to the X coordinate of the head of this ArcInst.
	 * @param dHeadY the change to the Y coordinate of the head of this ArcInst.
	 * @param dTailX the change to the X coordinate of the tail of this ArcInst.
	 * @param dTailY the change to the Y coordinate of the tail of this ArcInst.
	 */
	public void modify(double dWidth, double dHeadX, double dHeadY, double dTailX, double dTailY)
	{
		// save old arc state
		double oldxA = ends[HEADEND].getLocation().getX();
		double oldyA = ends[HEADEND].getLocation().getY();
		double oldxB = ends[TAILEND].getLocation().getX();
		double oldyB = ends[TAILEND].getLocation().getY();
		double oldWidth = getWidth();

		// change the arc
		lowLevelModify(dWidth, dHeadX, dHeadY, dTailX, dTailY);

		// track the change
		if (Undo.recordChange())
		{
			Undo.Change change = Undo.newChange(this, Undo.Type.ARCINSTMOD);
			change.setDoubles(oldxA, oldyA, oldxB, oldyB, oldWidth);
			setChange(change);
		}
		parent.setDirty();
	}

	/**
	 * Routine to replace this ArcInst with one of another type.
	 * @param ap the new type of arc.
	 * @return the new ArcInst (null on error).
	 */
	public ArcInst replace(ArcProto ap)
	{
		// check for connection allowance
		Connection head = getHead();
		Connection tail = getTail();
		PortInst piH = head.getPortInst();
		PortInst piT = tail.getPortInst();
		if (!piH.getPortProto().connectsTo(ap) || !piT.getPortProto().connectsTo(ap))
		{
			System.out.println("Cannot replace arc " + describe() + " with one of type " + ap.getProtoName() +
				" because the nodes cannot connect to it");
			return null;
		}

		// compute the new width
		double newwid = getWidth() - getProto().getWidthOffset() + ap.getWidthOffset();

		// first create the new nodeinst in place
		ArcInst newar = ArcInst.newInstance(ap, newwid, piH, head.getLocation(), piT, tail.getLocation(), null);
		if (newar == null)
		{
			System.out.println("Cannot replace arc " + describe() + " with one of type " + ap.getProtoName() +
				" because the new arc failed to create");
			return null;
		}

		// copy all variables on the arcinst
		newar.copyVars(this);
		newar.setNameTextDescriptor(getNameTextDescriptor());

		// now delete the original nodeinst
		kill();
		newar.setName(getName());
		return newar;
	}

	/****************************** LOW-LEVEL IMPLEMENTATION ******************************/

	/**
	 * Low-level access routine to create a ArcInst.
	 * @return the newly created ArcInst.
	 */
	public static ArcInst lowLevelAllocate()
	{
		ArcInst ai = new ArcInst();
		ai.ends = new Connection[2];
		return ai;
	}

	/**
	 * Low-level routine to fill-in the ArcInst information.
	 * @param protoType the ArcProto of this ArcInst.
	 * @param arcWidth the width of this ArcInst.
	 * @param headPort the head end PortInst.
	 * @param headPt the coordinate of the head end PortInst.
	 * @param tailPort the tail end PortInst.
	 * @param tailPt the coordinate of the tail end PortInst.
	 * @return true on error.
	 */
	public boolean lowLevelPopulate(ArcProto protoType, double arcWidth,
		PortInst headPort, Point2D headPt, PortInst tailPort, Point2D tailPt)
	{
		// initialize this object
		this.protoType = protoType;

		if (arcWidth <= 0)
			arcWidth = protoType.getWidth();
		this.arcWidth = arcWidth;

		Cell parent = headPort.getNodeInst().getParent();
		if (parent != tailPort.getNodeInst().getParent())
		{
			System.out.println("ArcProto.newInst: the 2 PortInsts are in different Cells!");
			return true;
		}
		this.parent = parent;

		// make sure the arc can connect to these ports
		PortProto headProto = headPort.getPortProto();
		PrimitivePort headPrimPort = headProto.getBasePort();
		if (!headPrimPort.connectsTo(protoType))
		{
			System.out.println("Cannot create " + protoType.describe() + " arc in cell " + parent.describe() +
				" because it cannot connect to port " + headProto.getProtoName());
			return true;
		}
		PortProto tailProto = tailPort.getPortProto();
		PrimitivePort tailPrimPort = tailProto.getBasePort();
		if (!tailPrimPort.connectsTo(protoType))
		{
			System.out.println("Cannot create " + protoType.describe() + " arc in cell " + parent.describe() +
				" because it cannot connect to port " + tailProto.getProtoName());
			return true;
		}

		// create node/arc connections and place them properly
		ends[HEADEND] = new Connection(this, headPort, headPt);
		ends[TAILEND] = new Connection(this, tailPort, tailPt);
		
		// fill in the geometry
		updateGeometric();

		return false;
	}

	/**
	 * Low-level routine to link the ArcInst into its Cell.
	 * @return true on error.
	 */
	public boolean lowLevelLink()
	{
		if (!isUsernamed())
		{
			if (getName() == null || !parent.isUniqueName(name, getClass(), this))
				if (setNameLow(parent.getAutoname(Name.findName("net@")))) return true;
		}

		// attach this arc to the two nodes it connects
		ends[HEADEND].getPortInst().getNodeInst().addConnection(ends[HEADEND]);
		ends[TAILEND].getPortInst().getNodeInst().addConnection(ends[TAILEND]);

		// add this arc to the cell
		linkGeom(parent);
		parent.addArc(this);
		linked = true;

		// update end shrinkage information
		for(int k=0; k<2; k++)
			updateShrinkage(ends[k].getPortInst().getNodeInst());
		return false;
	}

	/**
	 * Low-level routine to unlink the ArcInst from its Cell.
	 */
	public void lowLevelUnlink()
	{
		// remove this arc from the two nodes it connects
		ends[HEADEND].getPortInst().getNodeInst().removeConnection(ends[HEADEND]);
		ends[TAILEND].getPortInst().getNodeInst().removeConnection(ends[TAILEND]);

		// add this arc to the cell
		unLinkGeom(parent);
		parent.removeArc(this);

		// update end shrinkage information
		for(int k=0; k<2; k++)
			updateShrinkage(ends[k].getPortInst().getNodeInst());

		linked = false;
	}

	/**
	 * Low-level routine to change the width and end locations of this ArcInst.
	 * @param dWidth the change to the ArcInst width.
	 * @param dHeadX the change to the X coordinate of the head of this ArcInst.
	 * @param dHeadY the change to the Y coordinate of the head of this ArcInst.
	 * @param dTailX the change to the X coordinate of the tail of this ArcInst.
	 * @param dTailY the change to the Y coordinate of the tail of this ArcInst.
	 */
	public void lowLevelModify(double dWidth, double dHeadX, double dHeadY, double dTailX, double dTailY)
	{
		// first remove from the R-Tree structure
		unLinkGeom(parent);

		// now make the change
		arcWidth = EMath.smooth(arcWidth + dWidth);
		if (dHeadX != 0 || dHeadY != 0)
		{
			Point2D pt = ends[HEADEND].getLocation();
			ends[HEADEND].setLocation(new Point2D.Double(EMath.smooth(dHeadX+pt.getX()), EMath.smooth(pt.getY()+dHeadY)));
		}
		if (dTailX != 0 || dTailY != 0)
		{
			Point2D pt = ends[TAILEND].getLocation();
			ends[TAILEND].setLocation(new Point2D.Double(EMath.smooth(dTailX+pt.getX()), EMath.smooth(pt.getY()+dTailY)));
		}
		updateGeometric();

		// update end shrinkage information
		for(int k=0; k<2; k++)
			updateShrinkage(ends[k].getPortInst().getNodeInst());

		// reinsert in the R-Tree structure
		linkGeom(parent);
	}

	/****************************** GRAPHICS ******************************/

	/**
	 * Routine to return the width of this ArcInst.
	 * @return the width of this ArcInst.
	 */
	public double getWidth() { return arcWidth; }

	/**
	 * Routine to create a Poly object that describes an ArcInst.
	 * The ArcInst is described by its length, width and style.
	 * @param length the length of the ArcInst.
	 * @param width the width of the ArcInst.
	 * @param style the style of the ArcInst.
	 * @return a Poly that describes the ArcInst.
	 */
	public Poly makePoly(double length, double width, Poly.Type style)
	{
		Point2D endH = ends[HEADEND].getLocation();
		Point2D endT = ends[TAILEND].getLocation();

		// zero-width polygons are simply lines
		if (width == 0)
		{
			Poly poly = new Poly(new Point2D.Double[]{new Point2D.Double(endH.getX(), endH.getY()), new Point2D.Double(endT.getX(), endT.getY())});
			if (style == Poly.Type.FILLED) style = Poly.Type.OPENED;
			poly.setStyle(style);
			return poly;
		}

		// determine the end extension on each end
		double extendH = width/2;
		if (getHead().getEndShrink() != 0)
			extendH = getExtendFactor(width, getHead().getEndShrink());
		double extendT = width/2;
		if (getTail().getEndShrink() != 0)
			extendT = getExtendFactor(width, getTail().getEndShrink());
		if (!isExtended())
		{
			// nonextension arc: set extension to zero for all included ends
			if (!isSkipTail()) extendH = 0;
			if (!isSkipHead()) extendT = 0;
		}

		// make the polygon
		Poly poly = makeEndPointPoly(length, width, getAngle(), endH, extendH, endT, extendT);
		if (poly != null) poly.setStyle(style);
		return poly;
	}

	/**
	 * Routine to return a list of Polys that describes all text on this ArcInst.
	 * @param hardToSelect is true if considering hard-to-select text.
	 * @param wnd the window in which the text will be drawn.
	 * @return an array of Polys that describes the text.
	 */
	public Poly [] getAllText(boolean hardToSelect, EditWindow wnd)
	{
		int dispVars = numDisplayableVariables(false);
		int totalText = dispVars;
		if (totalText == 0) return null;
		Poly [] polys = new Poly[totalText];
		addDisplayableVariables(getBounds(), polys, 0, wnd, false);
		return polys;
	}

	private Poly makeEndPointPoly(double len, double wid, int angle, Point2D endH, double extendH,
		Point2D endT, double extendT)
	{
		double w2 = wid / 2;
		double x1 = endH.getX();   double y1 = endH.getY();
		double x2 = endT.getX();   double y2 = endT.getY();

		// somewhat simpler if rectangle is manhattan
		if (angle == 900 || angle == 2700)
		{
			if (y1 > y2)
			{
				double temp = y1;   y1 = y2;   y2 = temp;
				temp = extendH;   extendH = extendT;   extendT = temp;
			}
			return new Poly(new Point2D.Double[] {
				new Point2D.Double(EMath.smooth(x1 - w2), EMath.smooth(y1 - extendH)),
				new Point2D.Double(EMath.smooth(x1 + w2), EMath.smooth(y1 - extendH)),
				new Point2D.Double(EMath.smooth(x2 + w2), EMath.smooth(y2 + extendT)),
				new Point2D.Double(EMath.smooth(x2 - w2), EMath.smooth(y2 + extendT))});
		}
		if (angle == 0 || angle == 1800)
		{
			if (x1 > x2)
			{
				double temp = x1;   x1 = x2;   x2 = temp;
				temp = extendH;   extendH = extendT;   extendT = temp;
			}
			return new Poly(new Point2D.Double[] {
				new Point2D.Double(EMath.smooth(x1 - extendH), EMath.smooth(y1 - w2)),
				new Point2D.Double(EMath.smooth(x1 - extendH), EMath.smooth(y1 + w2)),
				new Point2D.Double(EMath.smooth(x2 + extendT), EMath.smooth(y2 + w2)),
				new Point2D.Double(EMath.smooth(x2 + extendT), EMath.smooth(y2 - w2))});
		}

		// nonmanhattan arcs cannot have zero length so re-compute it
		if (len == 0) len = endH.distance(endT);
		double xextra, yextra, xe1, ye1, xe2, ye2;
		if (len == 0)
		{
			double sa = EMath.sin(angle);
			double ca = EMath.cos(angle);
			xe1 = x1 - ca * extendH;
			ye1 = y1 - sa * extendH;
			xe2 = x2 + ca * extendT;
			ye2 = y2 + sa * extendT;
			xextra = ca * w2;
			yextra = sa * w2;
		} else
		{
			// work out all the math for nonmanhattan arcs
			xe1 = x1 - extendH * (x2-x1) / len;
			ye1 = y1 - extendH * (y2-y1) / len;
			xe2 = x2 + extendT * (x2-x1) / len;
			ye2 = y2 + extendT * (y2-y1) / len;

			// now compute the corners
			xextra = w2 * (x2-x1) / len;
			yextra = w2 * (y2-y1) / len;
		}
		return new Poly(new Point2D.Double[] {
			new Point2D.Double(EMath.smooth(yextra + xe1), EMath.smooth(ye1 - xextra)),
			new Point2D.Double(EMath.smooth(xe1 - yextra), EMath.smooth(xextra + ye1)),
			new Point2D.Double(EMath.smooth(xe2 - yextra), EMath.smooth(xextra + ye2)),
			new Point2D.Double(EMath.smooth(yextra + xe2), EMath.smooth(ye2 - xextra))});
	}

	private static int [] extendFactor = {0,
		11459, 5729, 3819, 2864, 2290, 1908, 1635, 1430, 1271, 1143,
		 1039,  951,  878,  814,  760,  712,  669,  631,  598,  567,
		  540,  514,  492,  470,  451,  433,  417,  401,  387,  373,
		  361,  349,  338,  327,  317,  308,  299,  290,  282,  275,
		  267,  261,  254,  248,  241,  236,  230,  225,  219,  214,
		  210,  205,  201,  196,  192,  188,  184,  180,  177,  173,
		  170,  166,  163,  160,  157,  154,  151,  148,  146,  143,
		  140,  138,  135,  133,  130,  128,  126,  123,  121,  119,
		  117,  115,  113,  111,  109,  107,  105,  104,  102,  100};

	/**
	 * Routine to return the amount that an arc end should extend, given its width and extension factor.
	 * @param width the width of the arc.
	 * @param extend the extension factor (from 0 to 90).
	 * @return the extension (from 0 to half of the width).
	 */
	private double getExtendFactor(double width, short extend)
	{
		// compute the amount of extension (from 0 to wid/2)
		if (extend <= 0) return width/2;

		// values should be from 0 to 90, but check anyway
		if (extend > 90) return width/2;

		// return correct extension
		return width * 50 / extendFactor[extend];
	}

	/**
	 * Routine to update the "end shrink" factors on all arcs on a NodeInst.
	 * @param ni the node to update.
	 */
	private void updateShrinkage(NodeInst ni)
	{
		ni.clearShortened();
		for(Iterator it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			short shrink = checkShortening(ni, con.getPortInst().getPortProto());
			if (shrink != 0) ni.setShortened();
			con.setEndShrink(shrink);
		}
	}

	private static final int MAXANGLES = 3;
	private static int [] shortAngles = new int[MAXANGLES];

	/**
	 * Routine to return the shortening factor for the arc connected to a port on a node.
	 * @param ni the node
	 * @param pp the port.
	 * @return the shortening factor.  This is a number from 0 to 90, where
	 * 0 indicates no shortening (extend the arc by half its width) and greater values
	 * indicate that the end should be shortened to account for this angle of connection.
	 * Small values are shortened almost to nothing, whereas large values are shortened
	 * very little (and a value of 90 indicates no shortening at all).
	 */
	private short checkShortening(NodeInst ni, PortProto pp)
	{
		// quit now if we don't have to worry about this kind of nodeinst
		NodeProto np = ni.getProto();
		if (!np.canShrink() && !np.isArcsShrink()) return 0;

		// gather the angles of the nodes/arcs
		int total = 0, off90 = 0;
		for(Iterator it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			ArcInst ai = con.getArc();

			// ignore zero-size arcs
			if (ai.getWidth() == 0) continue;

			// ignore this arcinst if it is not on the desired port
			if (!np.isArcsShrink() && con.getPortInst().getPortProto() != pp) continue;

			// compute the angle
			int ang = ai.getAngle() / 10;
			if (ai.getHead() == con) ang += 180;
			ang %= 360;
			if ((ang%90) != 0) off90++;
			if (total < MAXANGLES) shortAngles[total++] = ang; else
				break;
		}

		// throw in the nodeinst rotation factor if it is important
		if (np.canShrink())
		{
			PrimitivePort pRp = (PrimitivePort)pp;
			int ang = pRp.getAngle();
			ang += (ni.getAngle()+5) / 10;
//			if (ni->transpose != 0) { ang = 270 - ang; if (ang < 0) ang += 360; }
			ang = (ang+180)%360;
			if ((ang%90) != 0) off90++;
			if (total < MAXANGLES) shortAngles[total++] = ang;
		}

		// all fine if all manhattan angles involved
		if (off90 == 0) return 0;

		// give up if too many arcinst angles
		if (total != 2) return 0;

		// compute and return factor
		int ang = Math.abs(shortAngles[1]-shortAngles[0]);
		if (ang > 180) ang = 360 - ang;
		if (ang > 90) ang = 180 - ang;
		return (short)ang;
	}

	/**
	 * Routine to recompute the Geometric information on this ArcInst.
	 */
	private void updateGeometric()
	{
		Point2D p1 = ends[HEADEND].getLocation();
		Point2D p2 = ends[TAILEND].getLocation();
		double dx = p2.getX() - p1.getX();
		double dy = p2.getY() - p1.getY();
		sX = Math.sqrt(dx * dx + dy * dy);
		sY = arcWidth;
		center.setLocation(EMath.smooth((p1.getX() + p2.getX()) / 2), EMath.smooth((p1.getY() + p2.getY()) / 2));
		if (p1.equals(p2)) angle = 0; else
			angle = EMath.figureAngle(p1, p2);

		// compute the bounds
		Poly poly = makePoly(sX, arcWidth, Poly.Type.FILLED);
		visBounds.setRect(poly.getBounds2D());
	}

	/****************************** CONNECTIONS ******************************/

	/**
	 * Routine to return the Connection on the head end of this ArcInst.
	 * @return the Connection on the head end of this ArcInst.
	 */
	public Connection getHead() { return ends[HEADEND]; }

	/**
	 * Routine to return the Connection on the tail end of this ArcInst.
	 * @return the Connection on the tail end of this ArcInst.
	 */
	public Connection getTail() { return ends[TAILEND]; }

	/**
	 * Routine to return the connection at an end of this ArcInst.
	 * @param onHead true to get get the connection the head of this ArcInst.
	 * false to get get the connection the tail of this ArcInst.
	 */
	public Connection getConnection(boolean onHead)
	{
		return onHead ? ends[HEADEND] : ends[TAILEND];
	}

	/**
	 * Routine to return the connection at an end of this ArcInst.
	 * @param index 0 for the head of this ArcInst, 1 for the tail.
	 */
	public Connection getConnection(int index)
	{
		return ends[index];
	}

	/**
	 * Routine to tell whether a connection on this ArcInst contains a point.
	 * @param con the connection on this ArcInst.
	 * @param pt the point in question.
	 * @return true if the point is inside of the port.
	 */
	public boolean stillInPort(Connection con, Point2D pt, boolean reduceForArc)
	{
		// determine the area of the nodeinst
		PortInst pi = con.getPortInst();
		Poly poly = pi.getPoly();
		if (reduceForArc)
		{
			ArcInst ai = con.getArc();
			double wid = ai.getWidth() - ai.getProto().getWidthOffset();
			poly.reducePortPoly(pi, wid, ai.getAngle());
		}
		if (poly.isInside(pt)) return true;

		// no good
		return false;
	}

//	/**
//	 * Routine to remove the Connection from an end of this ArcInst.
//	 * @param c the Connection to remove.
//	 * @param onHead true if the Connection is on the head of this ArcInst.
//	 */
//	private void removeConnection(Connection c, boolean onHead)
//	{
//		// safety check
//		if ((onHead ? ends[HEADEND] : ends[TAILEND]) != c)
//		{
//			System.out.println("Tried to remove the wrong connection from a wire end: "
//				+ c + " on " + this);
//		}
//		if (onHead) ends[HEADEND] = null; else
//			ends[TAILEND] = null;
//	}

	/****************************** TEXT ******************************/

	/**
	 * Routine to determine whether a variable name on ArcInst is deprecated.
	 * Deprecated variable names are those that were used in old versions of Electric,
	 * but are no longer valid.
	 * @param name the name of the variable.
	 * @return true if the variable name is deprecated.
	 */
	public boolean isDeprecatedVariable(String name) { return name.equals(VAR_ARC_NAME); }

	/*
	 * Routine to write a description of this ArcInst.
	 * Displays the description in the Messages Window.
	 */
	public void getInfo()
	{
		System.out.println("-------------- ARC INSTANCE " + describe() + ": --------------");
		Point2D loc = ends[HEADEND].getLocation();
		System.out.println(" Head on " + ends[HEADEND].getPortInst().getNodeInst().describe() +
			" at (" + loc.getX() + "," + loc.getY() + ")");

		loc = ends[TAILEND].getLocation();
		System.out.println(" Tail on " + ends[TAILEND].getPortInst().getNodeInst().describe() +
			" at (" + loc.getX() + "," + loc.getY() + ")");
		System.out.println(" Center: (" + getCenterX() + "," + getCenterY() + "), width: " + getXSize() + ", length:" + getYSize() + ", angle " + angle/10.0);
		super.getInfo();
	}

	/**
	 * Routine to set the name of this ArcInst.
	 * The name is a local string that can be set by the user.
	 * @param name the new name of this ArcInst.
	 */
	public boolean setNameLow(Name name)
	{
		if (name == getNameLow()) return false;
		if (checkArcName(name)) return true;

		if (linked && getNameLow() != null)
		{
			parent.removeArcName(this);
		}
		super.setNameLow(name);
		if (linked)
		{
			parent.addArcName(this);
		}
		return false;
	}

	/**
	 * Routine to check the name of this NodeInst.
	 * @param name the new name of this NodeInst.
	 */
	private boolean checkArcName(Name name)
	{
		if (!name.isValid())
		{
			System.out.println("Invalid arc name <"+name+"> :"+Name.checkName(name.toString()));
			return true;
		}
		if (name.isTempname() && name.isBus())
		{
			System.out.println("Temporary bus name <"+name+">");
			return true;
		}
		if (name.hasEmptySubnames())
		{
			System.out.println("Name <"+name+"> has empty subnames");
			return true;
		}
		if (name.isBus() && protoType.getFunction() != ArcProto.Function.BUS)
		{
			System.out.println("Bus name <"+name+"> can be assigned only to bus arcs");
			return true;
		}
		if (name.isTempname() && linked && !parent.isUniqueName(name, NodeInst.class, this))
		{
			System.out.println("Arc name <"+name+"> is duplicated in "+parent);
			return true;
		}
		return false;
	}

	/**
	 * Routine to describe this ArcInst as a string.
	 * @return a description of this ArcInst.
	 */
	public String describe()
	{
		String description = protoType.describe();
		String name = getName();
		if (name != null) description += "[" + name + "]";
		return description;
	}

	/**
	 * Returns a printable version of this ArcInst.
	 * @return a printable version of this ArcInst.
	 */
	public String toString()
	{
		return "ArcInst " + protoType.getProtoName();
	}

	/****************************** MISCELLANEOUS ******************************/

	/**
	 * Routine to check and repair data structure errors in this ArcInst.
	 */
	public int checkAndRepair()
	{
		int errorCount = 0;

		// see if the ends are in their ports
		Point2D headPt = getHead().getLocation();
		if (!stillInPort(getHead(), headPt, false))
		{
			Poly poly = getHead().getPortInst().getPoly();
			System.out.println("Cell " + parent.describe() + ", arc " + describe() +
				": head not in port, is at (" + headPt.getX() + "," + headPt.getY() + ") but port center is (" +
				poly.getCenterX() + "," + poly.getCenterY() + ")");
			errorCount++;
		}
		Point2D tailPt = getTail().getLocation();
		if (!stillInPort(getTail(), tailPt, false))
		{
			Poly poly = getTail().getPortInst().getPoly();
			System.out.println("Cell " + parent.describe() + ", arc " + describe() +
				": tail not in port, is at (" + tailPt.getX() + "," + tailPt.getY() + ") but port center is (" +
				poly.getCenterX() + "," + poly.getCenterY() + ")");
			errorCount++;
		}

		// make sure width is not negative
		if (getWidth() < 0)
		{
			System.out.println("Cell " + parent.describe() + ", arc " + describe() +
				": has negative width (" + getWidth() + ")");
			errorCount++;
		}
		return errorCount;
	}

	/**
	 * Routine to return the prototype of this ArcInst.
	 * @return the prototype of this ArcInst.
	 */
	public ArcProto getProto() { return protoType; }

	/**
	 * Routine to set this ArcInst to be rigid.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
	 */
	public void setRigid() { userBits |= FIXED; }

	/**
	 * Routine to set this ArcInst to be not rigid.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
	 */
	public void clearRigid() { userBits &= ~FIXED; }

	/**
	 * Routine to tell whether this ArcInst is rigid.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
	 * @return true if this ArcInst is rigid.
	 */
	public boolean isRigid() { return (userBits & FIXED) != 0; }

	/**
	 * Routine to set this ArcInst to be fixed-angle.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
	 */
	public void setFixedAngle() { userBits |= FIXANG; }

	/**
	 * Routine to set this ArcInst to be not fixed-angle.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
	 */
	public void clearFixedAngle() { userBits &= ~FIXANG; }

	/**
	 * Routine to tell whether this ArcInst is fixed-angle.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
	 * @return true if this ArcInst is fixed-angle.
	 */
	public boolean isFixedAngle() { return (userBits & FIXANG) != 0; }

	/**
	 * Routine to set this ArcInst to be slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
	 */
	public void setSlidable() { userBits &= ~CANTSLIDE; }

	/**
	 * Routine to set this ArcInst to be not slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
	 */
	public void clearSlidable() { userBits |= CANTSLIDE; }

	/**
	 * Routine to tell whether this ArcInst is slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
	 * @return true if this ArcInst is slidable.
	 */
	public boolean isSlidable() { return (userBits & CANTSLIDE) == 0; }

	/**
	 * Routine to set that this rigid ArcInst was modified.
	 * This is used during constraint processing only and should not be used elsewhere.
	 */
	public void setRigidModified() { userBits &= ~FIXEDMOD; }

	/**
	 * Routine to set that this rigid ArcInst was not modified.
	 * This is used during constraint processing only and should not be used elsewhere.
	 */
	public void clearRigidModified() { userBits |= FIXEDMOD; }

	/**
	 * Routine to tell whether this rigid ArcInst was modified.
	 * This is used during constraint processing only and should not be used elsewhere.
	 * @return true if this rigid ArcInst was modified.
	 */
	public boolean isRigidModified() { return (userBits & FIXEDMOD) == 0; }

	/**
	 * Low-level routine to set the ArcInst angle in the "user bits".
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it this way.
	 * This should not normally be called by any other part of the system.
	 * @param angle the angle of the ArcInst (in degrees).
	 */
	public void lowLevelSetArcAngle(int angle) { userBits = (userBits & ~AANGLE) | (angle << AANGLESH); }

	/**
	 * Low-level routine to get the ArcInst angle from the "user bits".
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it this way.
	 * This should not normally be called by any other part of the system.
	 * @return the arc angle (in degrees).
	 */
	public int lowLevelGetArcAngle() { return (userBits & AANGLE) >> AANGLESH; }

	/**
	 * Routine to set this ArcInst to have its ends extended.
	 * End-extension causes an arc to extend past its endpoint by half of its width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 */
	public void setExtended() { userBits &= ~NOEXTEND; }

	/**
	 * Routine to set this ArcInst to have its ends not extended.
	 * End-extension causes an arc to extend past its endpoint by half of its width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 */
	public void clearExtended() { userBits |= NOEXTEND; }

	/**
	 * Routine to tell whether this ArcInst has its ends extended.
	 * End-extension causes an arc to extend past its endpoint by half of its width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @return true if this ArcInst has its ends extended.
	 */
	public boolean isExtended() { return (userBits & NOEXTEND) == 0; }

	/**
	 * Routine to set this ArcInst to be negated.
	 * Negated arcs have a bubble drawn on their tail end to indicate negation.
	 * If the arc is reversed, then the bubble appears on the head.
	 * This is used only in Schematics technologies to place negating bubbles on any node.
	 * @see ArcInst#setReverseEnds
	 */
	public void setNegated() { userBits |= ISNEGATED; }

	/**
	 * Routine to set this ArcInst to be not negated.
	 * Negated arcs have a bubble drawn on their tail end to indicate negation.
	 * If the arc is reversed, then the bubble appears on the head.
	 * This is used only in Schematics technologies to place negating bubbles on any node.
	 * @see ArcInst#setReverseEnds
	 */
	public void clearNegated() { userBits &= ~ISNEGATED; }

	/**
	 * Routine to tell whether this ArcInst is negated.
	 * Negated arcs have a bubble drawn on their tail end to indicate negation.
	 * If the arc is reversed, then the bubble appears on the head.
	 * This is used only in Schematics technologies to place negating bubbles on any node.
	 * @return true if this ArcInst is negated.
	 * @see ArcInst#setReverseEnds
	 */
	public boolean isNegated() { return (userBits & ISNEGATED) != 0; }

	/**
	 * Routine to set this ArcInst to be directional.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * The arrow head is on the arc's head end, unless the arc is reversed.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @see ArcInst#setReverseEnds
	 */
	public void setDirectional() { userBits |= ISDIRECTIONAL; }

	/**
	 * Routine to set this ArcInst to be not directional.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * The arrow head is on the arc's head end, unless the arc is reversed.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @see ArcInst#setReverseEnds
	 */
	public void clearDirectional() { userBits &= ~ISDIRECTIONAL; }

	/**
	 * Routine to tell whether this ArcInst is directional.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * The arrow head is on the arc's head end, unless the arc is reversed.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @return true if this ArcInst is directional.
	 * @see ArcInst#setReverseEnds
	 */
	public boolean isDirectional() { return (userBits & ISDIRECTIONAL) != 0; }

	/**
	 * Routine to set this ArcInst to have its head skipped.
	 * Skipping the head causes any special actions that are normally applied to the
	 * head to be ignored.  For example, the directional arrow is on the arc head,
	 * so skipping the head will remove the arrow-head, but not the body of the arrow.
	 */
	public void setSkipHead() { userBits |= NOTEND0; }

	/**
	 * Routine to set this ArcInst to have its head not skipped.
	 * Skipping the head causes any special actions that are normally applied to the
	 * head to be ignored.  For example, the directional arrow is on the arc head,
	 * so skipping the head will remove the arrow-head, but not the body of the arrow.
	 */
	public void clearSkipHead() { userBits &= ~NOTEND0; }

	/**
	 * Routine to tell whether this ArcInst has its head skipped.
	 * Skipping the head causes any special actions that are normally applied to the
	 * head to be ignored.  For example, the directional arrow is on the arc head,
	 * so skipping the head will remove the arrow-head, but not the body of the arrow.
	 * @return true if this ArcInst has its head skipped.
	 */
	public boolean isSkipHead() { return (userBits & NOTEND0) != 0; }

	/**
	 * Routine to set this ArcInst to have its tail skipped.
	 * Skipping the tail causes any special actions that are normally applied to the
	 * tail to be ignored.  For example, the negating bubble is on the arc tail,
	 * so skipping the tail will remove the bubble.
	 */
	public void setSkipTail() { userBits |= NOTEND1; }

	/**
	 * Routine to set this ArcInst to have its tail not skipped.
	 * Skipping the tail causes any special actions that are normally applied to the
	 * tail to be ignored.  For example, the negating bubble is on the arc tail,
	 * so skipping the tail will remove the bubble.
	 */
	public void clearSkipTail() { userBits &= ~NOTEND1; }

	/**
	 * Routine to tell whether this ArcInst has its tail skipped.
	 * Skipping the tail causes any special actions that are normally applied to the
	 * tail to be ignored.  For example, the negating bubble is on the arc tail,
	 * so skipping the tail will remove the bubble.
	 * @return true if this ArcInst has its tail skipped.
	 */
	public boolean isSkipTail() { return (userBits & NOTEND1) != 0; }

	/**
	 * Routine to reverse the ends of this ArcInst.
	 * A reversed arc switches its head and tail.
	 * This is useful if the negating bubble appears on the wrong end.
	 */
	public void setReverseEnds() { userBits |= REVERSEEND; }

	/**
	 * Routine to restore the proper ends of this ArcInst.
	 * A reversed arc switches its head and tail.
	 * This is useful if the negating bubble appears on the wrong end.
	 */
	public void clearReverseEnds() { userBits &= ~REVERSEEND; }

	/**
	 * Routine to tell whether this ArcInst has been reversed.
	 * A reversed arc switches its head and tail.
	 * This is useful if the negating bubble appears on the wrong end.
	 * @return true if this ArcInst has been reversed.
	 */
	public boolean isReverseEnds() { return (userBits & REVERSEEND) != 0; }

	/**
	 * Routine to set this ArcInst to be hard-to-select.
	 * Hard-to-select ArcInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 */
	public void setHardSelect() { userBits |= HARDSELECTA; }

	/**
	 * Routine to set this ArcInst to be easy-to-select.
	 * Hard-to-select ArcInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 */
	public void clearHardSelect() { userBits &= ~HARDSELECTA; }

	/**
	 * Routine to tell whether this ArcInst is hard-to-select.
	 * Hard-to-select ArcInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 * @return true if this ArcInst is hard-to-select.
	 */
	public boolean isHardSelect() { return (userBits & HARDSELECTA) != 0; }

}

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
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

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
	/** Key of the obsolete variable holding arc name.*/public static final Variable.Key ARC_NAME = ElectricObject.newKey("ARC_name");
	/** Key of Varible holding arc curvature. */		public static final Variable.Key ARC_RADIUS = ElectricObject.newKey("ARC_radius");

	// -------------------------- private data ----------------------------------

	/** fixed-length arc */								private static final int FIXED =                     01;
	/** fixed-angle arc */								private static final int FIXANG =                    02;
//	/** arc has text that is far away */				private static final int AHASFARTEXT =               04;
//	/** arc is not in use */							private static final int DEADA =                    020;
	/** angle of arc from end 0 to end 1 */				private static final int AANGLE =                037740;
	/** bits of right shift for AANGLE field */			private static final int AANGLESH =                   5;
//	/** set if arc is to be drawn shortened */			private static final int ASHORT =                040000;
	/** set if ends do not extend by half width */		private static final int NOEXTEND =             0400000;
//	/** set if ends are negated */						private static final int ISNEGATED =           01000000;
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

	/** prefix for autonameing. */						private static final Name BASENAME = Name.findName("net@");

	/** width of this ArcInst. */						private double width;
	/** length of this ArcInst. */						private double length;
	/** prototype of this arc instance */				private ArcProto protoType;
	/** end connections of this arc instance */			private Connection [] ends;
	/** 0-based index of this ArcInst in cell. */		private int arcIndex;
	/** angle of this ArcInst (in tenth-degrees). */	private int angle;

	/**
	 * The constructor is never called.  Use the factory "newInstance" instead.
	 */
	private ArcInst()
	{
		arcIndex = -1;
	}

	/****************************** CREATE, DELETE, MODIFY ******************************/

	/**
	 * Method to create a new ArcInst with appropriate defaults, connecting two PortInsts.
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
			ai.setDefaultConstraints();
		}
		return ai;
	}

	/**
	 * Method to create a new ArcInst with appropriate defaults, connecting two PortInsts at specified locations.
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
		ArcInst ai = newInstance(type, width, head, headPt, tail, tailPt, name, 0);
		if (ai != null)
		{
			ai.setDefaultConstraints();
		}
		return ai;
	}

	/**
	 * Method to create a "dummy" ArcInst for use outside of the database.
	 * @param ap the prototype of the ArcInst.
	 * @param arcLength the length of the ArcInst.
	 * @return the dummy ArcInst.
	 */
	public static ArcInst makeDummyInstance(PrimitiveArc ap, double arcLength)
	{
		PrimitiveNode npEnd = ap.findPinProto();
		if (npEnd == null)
		{
			System.out.println("Cannot find pin for arc " + ap.describe());
			return null;
		}

		// create the head node
		NodeInst niH = NodeInst.lowLevelAllocate();
		niH.lowLevelPopulate(npEnd, new Point2D.Double(-arcLength/2,0), npEnd.getDefWidth(), npEnd.getDefHeight(), 0, null);
		PortInst piH = niH.getOnlyPortInst();
		Rectangle2D boundsH = piH.getBounds();
		double xH = boundsH.getCenterX();
		double yH = boundsH.getCenterY();

		// create the tail node
		NodeInst niT = NodeInst.lowLevelAllocate();
		niT.lowLevelPopulate(npEnd, new Point2D.Double(arcLength/2,0), npEnd.getDefWidth(), npEnd.getDefHeight(), 0, null);
		PortInst piT = niT.getOnlyPortInst();
		Rectangle2D boundsT = piT.getBounds();
		double xT = boundsT.getCenterX();
		double yT = boundsT.getCenterY();

		// create the arc that connects them
		ArcInst ai = ArcInst.lowLevelAllocate();
		ai.lowLevelPopulate(ap, ap.getDefaultWidth(), piH, new Point2D.Double(xH, yH), piT, new Point2D.Double(xT, yT), 0);
		return ai;
	}

	/**
	 * Method to create a new ArcInst connecting two PortInsts.
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
		return newInstance(type, width, head, null, tail, null, name, 0);
	}

	/**
	 * Method to create a new ArcInst connecting two PortInsts at specified locations.
	 * This is more general than the version that does not take coordinates.
	 * @param type the prototype of the new ArcInst.
	 * @param width the width of the new ArcInst.  The width must be > 0.
	 * @param head the head end PortInst.
	 * @param headPt the coordinate of the head end PortInst.
	 * @param tail the tail end PortInst.
	 * @param tailPt the coordinate of the tail end PortInst.
	 * @param name the name of the new ArcInst
	 * @param defAngle default angle in case port points coincide
     * @return the newly created ArcInst, or null if there is an error.
	 */
	public static ArcInst newInstance(ArcProto type, double width,
                                      PortInst head, Point2D headPt, PortInst tail, Point2D tailPt, String name, int defAngle)
	{
        // if points are null, create them as would newInstance
        if (headPt == null) {
            Rectangle2D headBounds = head.getBounds();
            headPt = new Point2D.Double(headBounds.getCenterX(), headBounds.getCenterY());
        }
        if (tailPt == null) {
            Rectangle2D tailBounds = tail.getBounds();
            tailPt = new Point2D.Double(tailBounds.getCenterX(), tailBounds.getCenterY());
        }

		// make sure fields are valid
		if (type == null || head == null || tail == null) return null;

		ArcInst ai = lowLevelAllocate();
		if (ai.lowLevelPopulate(type, width, head, headPt, tail, tailPt, defAngle)) return null;
		if (!ai.stillInPort(ai.getHead(), headPt, false))
		{
			Cell parent = head.getNodeInst().getParent();
			System.out.println("Error in cell " + parent.describe() + ": head of " + type.getName() +
				" arc at (" + headPt.getX() + "," + headPt.getY() + ") does not fit in port " +
				ai.getHead().getPortInst().describe());
			return null;
		}
		if (!ai.stillInPort(ai.getTail(), tailPt, false))
		{
			Cell parent = tail.getNodeInst().getParent();
			System.out.println("Error in cell " + parent.describe() + ": tail of " + type.getName() +
				" arc at (" + tailPt.getX() + "," + tailPt.getY() + ") does not fit in port " +
				ai.getTail().getPortInst().describe());
			return null;
		}
		if (name != null) ai.setName(name);
		if (ai.lowLevelLink()) return null;

		// handle change control, constraint, and broadcast
		Undo.newObject(ai);
		return ai;
	}

	/**
	 * Method to delete this ArcInst.
	 */
	public void kill()
	{
		// remove the arc
		lowLevelUnlink();

		// handle change control, constraint, and broadcast
		Undo.killObject(this);
	}

	/**
	 * Method to change the width and end locations of this ArcInst.
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
		Undo.modifyArcInst(this, oldxA, oldyA, oldxB, oldyB, oldWidth);
	}

	/**
	 * Method to replace this ArcInst with one of another type.
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
			System.out.println("Cannot replace arc " + describe() + " with one of type " + ap.getName() +
				" because the nodes cannot connect to it");
			return null;
		}

		// compute the new width
		double newwid = getWidth() - getProto().getWidthOffset() + ap.getWidthOffset();

		// first create the new nodeinst in place
		ArcInst newar = ArcInst.newInstance(ap, newwid, piH, head.getLocation(), piT, tail.getLocation(), null, 0);
		if (newar == null)
		{
			System.out.println("Cannot replace arc " + describe() + " with one of type " + ap.getName() +
				" because the new arc failed to create");
			return null;
		}

		// copy all variables on the arcinst
		newar.copyVarsFrom(this);
		newar.setNameTextDescriptor(getNameTextDescriptor());

		// now delete the original nodeinst
		kill();
		newar.setName(getName());
		return newar;
	}

	/****************************** LOW-LEVEL IMPLEMENTATION ******************************/

	/**
	 * Low-level access method to create a ArcInst.
	 * @return the newly created ArcInst.
	 */
	public static ArcInst lowLevelAllocate()
	{
		ArcInst ai = new ArcInst();
		ai.ends = new Connection[2];
		return ai;
	}

	/**
	 * Low-level method to fill-in the ArcInst information.
	 * @param protoType the ArcProto of this ArcInst.
	 * @param width the width of this ArcInst.
	 * @param headPort the head end PortInst.
	 * @param headPt the coordinate of the head end PortInst.
	 * @param tailPort the tail end PortInst.
	 * @param tailPt the coordinate of the tail end PortInst.
	 * @param defAngle the default angle of this arc (if the endpoints are coincident).
	 * @return true on error.
	 */
	public boolean lowLevelPopulate(ArcProto protoType, double width,
		PortInst headPort, Point2D headPt, PortInst tailPort, Point2D tailPt, int defAngle)
	{
		// initialize this object
		this.protoType = protoType;

		if (width < 0)
			width = protoType.getWidth();
		this.width = width;

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
				" because it cannot connect to port " + headProto.getName());
			return true;
		}
		PortProto tailProto = tailPort.getPortProto();
		PrimitivePort tailPrimPort = tailProto.getBasePort();
		if (!tailPrimPort.connectsTo(protoType))
		{
			System.out.println("Cannot create " + protoType.describe() + " arc in cell " + parent.describe() +
				" because it cannot connect to port " + tailProto.getName());
			return true;
		}

		// create node/arc connections and place them properly
		ends[HEADEND] = new Connection(this, headPort, headPt);
		ends[TAILEND] = new Connection(this, tailPort, tailPt);
		
		// fill in the geometry
		updateGeometric(defAngle);

		return false;
	}

	/**
	 * Low-level method to link the ArcInst into its Cell.
	 * @return true on error.
	 */
	public boolean lowLevelLink()
	{
		if (!isUsernamed())
		{
			if (getName() == null || !parent.isUniqueName(name, getClass(), this))
				if (setNameKey(parent.getAutoname(BASENAME))) return true;
		}

		// attach this arc to the two nodes it connects
		if (ends[HEADEND] != null) ends[HEADEND].getPortInst().getNodeInst().addConnection(ends[HEADEND]);
		if (ends[TAILEND] != null) ends[TAILEND].getPortInst().getNodeInst().addConnection(ends[TAILEND]);

		// add this arc to the cell
		linkGeom(parent);
		parent.addArc(this);

		// update end shrinkage information
		for(int k=0; k<2; k++)
			updateShrinkage(ends[k].getPortInst().getNodeInst());
		return false;
	}

	/**
	 * Low-level method to unlink the ArcInst from its Cell.
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
	}

	/**
	 * Low-level method to change the width and end locations of this ArcInst.
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
		width = DBMath.round(width + dWidth);

		if (dHeadX != 0 || dHeadY != 0)
		{
			Point2D pt = ends[HEADEND].getLocation();
			ends[HEADEND].setLocation(new Point2D.Double(DBMath.round(dHeadX+pt.getX()), DBMath.round(pt.getY()+dHeadY)));
		}
		if (dTailX != 0 || dTailY != 0)
		{
			Point2D pt = ends[TAILEND].getLocation();
			ends[TAILEND].setLocation(new Point2D.Double(DBMath.round(dTailX+pt.getX()), DBMath.round(pt.getY()+dTailY)));
		}
		updateGeometric(getAngle());

		// update end shrinkage information
		for(int k=0; k<2; k++)
			updateShrinkage(ends[k].getPortInst().getNodeInst());

		// reinsert in the R-Tree structure
		linkGeom(parent);
	}

	/****************************** GRAPHICS ******************************/

	/**
	 * Method to return the width of this ArcInst.
	 * @return the width of this ArcInst.
	 */
	public double getWidth() { return width; }

	/**
	 * Method to return the length of this ArcInst.
	 * @return the length of this ArcInst.
	 */
	public double getLength() { return length; }

	/**
	 * Method to return the rotation angle of this ArcInst.
	 * @return the rotation angle of this ArcInst (in tenth-degrees).
	 */
	public int getAngle() { return angle; }

	/**
	 * Method to set the rotation angle of this ArcInst.
	 * @param angle the rotation angle of this ArcInst (in tenth-degrees).
	 * In general, you should not call this method because the
	 * constructors and modification methods update this correctly.
	 * If, however, you have a zero-length arc and want to explicitly set
	 * its angle, then use this method.
	 */
	public void setAngle(int angle) { this.angle = angle; }

	/**
	 * Method to create a Poly object that describes an ArcInst.
	 * The ArcInst is described by its length, width and style.
	 * @param length the length of the ArcInst.
	 * @param width the width of the ArcInst.
	 * @param style the style of the ArcInst.
	 * @return a Poly that describes the ArcInst.
	 */
	public Poly makePoly(double length, double width, Poly.Type style)
	{
		if (protoType.isCurvable())
		{
			Poly curvedPoly = curvedArcOutline(style, width);
			if (curvedPoly != null) return curvedPoly;
		}

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
		int headShrink = getHead().getEndShrink();
		if (headShrink != 0)
			extendH = getExtendFactor(width, (short)headShrink);
		double extendT = width/2;
		int tailShrink = getTail().getEndShrink();
		if (tailShrink != 0)
			extendT = getExtendFactor(width, (short)tailShrink);
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
	 * Method to get the curvature radius on this ArcInst.
	 * The curvature (used in artwork and round-cmos technologies) lets an arc
	 * curve.
	 * @return the curvature radius on this ArcInst.
	 * Returns negative if there is no curvature information.
	 */
	public Double getRadius()
	{
		Variable var = getVar(ARC_RADIUS);
		if (var == null) return null;

		// get the radius of the circle, check for validity
		Object obj = var.getObject();

		if (obj instanceof Integer)
		{
			return new Double(((Integer)obj).intValue() / 2000.0);
		}
		if (obj instanceof Double)
		{
			return new Double(((Double)obj).doubleValue());
		}
		return null;
	}

	/**
	 * when arcs are curved, the number of line segments will be
	 * between this value, and half of this value.
	 */
	private static final int MAXARCPIECES = 16;

	/**
	 * Method to fill polygon "poly" with the outline of the curved arc in
	 * "ai" whose width is "wid".  The style of the polygon is set to "style".
	 * If there is no curvature information in the arc, the routine returns null,
	 * otherwise it returns the curved polygon.
	 */
	public Poly curvedArcOutline(Poly.Type style, double wid)
	{
		// get the radius information on the arc
		Double radiusDouble = getRadius();
		if (radiusDouble == null) return null;

		// get information about the curved arc
		double radius = radiusDouble.doubleValue();
		double pureRadius = Math.abs(radius);
		Point2D headPt = getTail().getLocation();
		Point2D tailPt = getHead().getLocation();
		double length = headPt.distance(tailPt);

		// see if the radius can work with these arc ends
		if (pureRadius*2 < length) return null;

		// determine the center of the circle
		Point2D [] centers = DBMath.findCenters(pureRadius, headPt, tailPt, length);
		if (centers == null) return null;

		Point2D centerPt = centers[1];
		if (radius < 0)
		{
			radius = -radius;
			centerPt = centers[0];
		}

		// determine the base and range of angles
		int angleBase = DBMath.figureAngle(centerPt, headPt);
		int angleRange = DBMath.figureAngle(centerPt, tailPt);
		if (isReverseEnds())
		{
			int i = angleBase;
			angleBase = angleRange;
			angleRange = i;
		}
		angleRange -= angleBase;
		if (angleRange < 0) angleRange += 3600;

		// determine the number of intervals to use for the arc
		int pieces = angleRange;
		while (pieces > MAXARCPIECES) pieces /= 2;

		// initialize the polygon
		int points = (pieces+1) * 2;
		Point2D [] pointArray = new Point2D[points];

		// get the inner and outer radii of the arc
		double outerRadius = radius + wid / 2;
		double innerRadius = outerRadius - wid;

		// fill the polygon
		for(int i=0; i<=pieces; i++)
		{
			int a = (angleBase + i * angleRange / pieces) % 3600;
			double sin = DBMath.sin(a);   double cos = DBMath.cos(a);
			pointArray[i] = new Point2D.Double(cos * innerRadius + centerPt.getX(), sin * innerRadius + centerPt.getY());
			pointArray[points-1-i] = new Point2D.Double(cos * outerRadius + centerPt.getX(), sin * outerRadius + centerPt.getY());
		}
		Poly poly = new Poly(pointArray);
		poly.setStyle(style);
		return poly;
	}

	/**
	 * Method to return a list of Polys that describes all text on this ArcInst.
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
			new Poly(new Point2D.Double[] {
				new Point2D.Double(x1 - w2, y1 - extendH),
				new Point2D.Double(x1 + w2, y1 - extendH),
				new Point2D.Double(x2 + w2, y2 + extendT),
				new Point2D.Double(x2 - w2, y2 + extendT)});
		}
		if (angle == 0 || angle == 1800)
		{
			if (x1 > x2)
			{
				double temp = x1;   x1 = x2;   x2 = temp;
				temp = extendH;   extendH = extendT;   extendT = temp;
			}
			return new Poly(new Point2D.Double[] {
				new Point2D.Double(x1 - extendH, y1 - w2),
				new Point2D.Double(x1 - extendH, y1 + w2),
				new Point2D.Double(x2 + extendT, y2 + w2),
				new Point2D.Double(x2 + extendT, y2 - w2)});
		}

		// nonmanhattan arcs cannot have zero length so re-compute it
		if (len == 0) len = endH.distance(endT);
		double xextra, yextra, xe1, ye1, xe2, ye2;
		if (len == 0)
		{
			double sa = DBMath.sin(angle);
			double ca = DBMath.cos(angle);
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
			new Point2D.Double(yextra + xe1, ye1 - xextra),
			new Point2D.Double(xe1 - yextra, xextra + ye1),
			new Point2D.Double(xe2 - yextra, xextra + ye2),
			new Point2D.Double(yextra + xe2, ye2 - xextra)});
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
	 * Method to return the amount that an arc end should extend, given its width and extension factor.
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
	 * Method to update the "end shrink" factors on all arcs on a NodeInst.
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
	 * Method to return the shortening factor for the arc connected to a port on a node.
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
	 * Method to recompute the Geometric information on this ArcInst.
	 */
	private void updateGeometric(int defAngle)
	{
		Point2D p1 = ends[TAILEND].getLocation();
		Point2D p2 = ends[HEADEND].getLocation();
		double dx = p2.getX() - p1.getX();
		double dy = p2.getY() - p1.getY();
		length = Math.sqrt(dx * dx + dy * dy);
		if (p1.equals(p2)) angle = defAngle;
        else angle = DBMath.figureAngle(p1, p2);

		// compute the bounds
		Poly poly = makePoly(length, width, Poly.Type.FILLED);
		visBounds.setRect(poly.getBounds2D());

		// the cell must recompute its bounds
		if (parent != null) parent.setDirty();
	}

	/****************************** CONNECTIONS ******************************/

	/**
	 * Method to return the Connection on the head end of this ArcInst.
	 * @return the Connection on the head end of this ArcInst.
	 */
	public Connection getHead() { return ends[HEADEND]; }

	/**
	 * Method to return the Connection on the tail end of this ArcInst.
	 * @return the Connection on the tail end of this ArcInst.
	 */
	public Connection getTail() { return ends[TAILEND]; }

	/**
	 * Method to return the connection at an end of this ArcInst.
	 * @param onHead true to get get the connection the head of this ArcInst.
	 * false to get get the connection the tail of this ArcInst.
	 */
	public Connection getConnection(boolean onHead)
	{
		return onHead ? ends[HEADEND] : ends[TAILEND];
	}

	/**
	 * Method to return the connection at an end of this ArcInst.
	 * @param index 0 for the head of this ArcInst, 1 for the tail.
	 */
	public Connection getConnection(int index)
	{
		return ends[index];
	}

	/**
	 * Method to tell whether a connection on this ArcInst contains a port location.
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
//System.out.println("NOT STILL IN PORT BECAUSE pt="+pt+" reduce="+reduceForArc+" poly ctr=("+poly.getCenterX()+","+poly.getCenterY()+")");
		return false;
	}

	/****************************** TEXT ******************************/

	/**
	 * Method to determine whether a variable key on ArcInst is deprecated.
	 * Deprecated variable keys are those that were used in old versions of Electric,
	 * but are no longer valid.
	 * @param key the key of the variable.
	 * @return true if the variable key is deprecated.
	 */
	public boolean isDeprecatedVariable(Variable.Key key)
	{
		if (key == ARC_NAME) return true;
		return super.isDeprecatedVariable(key);
	}

	/**
	 * Method to write a description of this ArcInst.
	 * Displays the description in the Messages Window.
	 */
//	public void getInfo()
//	{
//		System.out.println("-------------- ARC INSTANCE " + describe() + ": --------------");
//		Point2D loc = ends[HEADEND].getLocation();
//		System.out.println(" Head on " + ends[HEADEND].getPortInst().getNodeInst().describe() +
//			" at (" + loc.getX() + "," + loc.getY() + ")");
//
//		loc = ends[TAILEND].getLocation();
//		System.out.println(" Tail on " + ends[TAILEND].getPortInst().getNodeInst().describe() +
//			" at (" + loc.getX() + "," + loc.getY() + ")");
//		System.out.println(" Center: (" + getCenterX() + "," + getCenterY() + "), width: " + getXSize() + ", length:" + getYSize() + ", angle " + angle/10.0);
//		super.getInfo();
//	}

	/**
	 * Method to describe this ArcInst as a string.
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
        if (protoType == null) return "ArcInst null protoType";
		return "ArcInst " + protoType.getName();
	}

	/****************************** MISCELLANEOUS ******************************/

	/**
	 * Method to check and repair data structure errors in this ArcInst.
	 */
	public int checkAndRepair()
	{
		int errorCount = 0;

		// see if the ends are in their ports
		Point2D headPt = getHead().getLocation();
		if (!stillInPort(getHead(), headPt, false))
		{
			// allow for round-off error
			headPt.setLocation(DBMath.round(headPt.getX()), DBMath.round(headPt.getY()));

			if (!stillInPort(getHead(), headPt, false))
			{
				Poly poly = getHead().getPortInst().getPoly();
				System.out.println("Cell " + parent.describe() + ", arc " + describe() +
					": head not in port, is at (" + headPt.getX() + "," + headPt.getY() + ") but port center is (" +
					poly.getCenterX() + "," + poly.getCenterY() + ")");
				getHead().setLocation(new Point2D.Double(poly.getCenterX(), poly.getCenterY()));
				errorCount++;
			}
		}
		Point2D tailPt = getTail().getLocation();
		if (!stillInPort(getTail(), tailPt, false))
		{
			// allow for round-off error
			tailPt.setLocation(DBMath.round(tailPt.getX()), DBMath.round(tailPt.getY()));
			//tailPt.setLocation(DBMath.smooth(tailPt.getX()), DBMath.smooth(tailPt.getY()));
			if (!stillInPort(getTail(), tailPt, false))
			{
				Poly poly = getTail().getPortInst().getPoly();
				System.out.println("Cell " + parent.describe() + ", arc " + describe() +
					": tail not in port, is at (" + tailPt.getX() + "," + tailPt.getY() + ") but port center is (" +
					poly.getCenterX() + "," + poly.getCenterY() + ")");
				getTail().setLocation(new Point2D.Double(poly.getCenterX(), poly.getCenterY()));
				errorCount++;
			}
		}

		// make sure width is not negative
		if (getWidth() < 0)
		{
			System.out.println("Cell " + parent.describe() + ", arc " + describe() +
				": has negative width (" + getWidth() + ")");
			width = Math.abs(width);
			errorCount++;
		}
		return errorCount;
	}

	/**
	 * Method to set an index of this ArcInst in Cell arcs.
	 * This is a zero-based index of arcs on the Cell.
	 * @param arcIndex an index of this ArcInst in Cell nodes.
	 */
	public void setArcIndex(int arcIndex) { this.arcIndex = arcIndex; }

	/**
	 * Method to get the index of this ArcInst.
	 * This is a zero-based index of arcs on the Cell.
	 * @return the index of this ArcInst.
	 */
	public final int getArcIndex() { return arcIndex; }

	/**
	 * Method tells if this ArcInst is linked to parent Cell.
	 * @return true if this ArcInst is linked to parent Cell.
	 */
	public boolean isLinked() { return arcIndex >= 0; }

	/**
	 * Returns the basename for autonaming.
	 * @return the basename for autonaming.
	 */
	public Name getBasename() { return BASENAME; }

	/**
	 * Method to return the prototype of this ArcInst.
	 * @return the prototype of this ArcInst.
	 */
	public ArcProto getProto() { return protoType; }

    /**
     * Copies all properties (variables, constraints, and textdescriptor)
     * from 'fraomAi' to this arcinst. This is basically the same as calling
     * copyVarsFrom(), copyConstraintsFrom(), and setTextDescriptor().
     * @param fromAi the arc from which to copy all arc properties
     */
    public void copyPropertiesFrom(ArcInst fromAi) {
        if (fromAi == null) return;
        copyVarsFrom(fromAi);
		copyConstraintsFrom(fromAi);
        setNameTextDescriptor(fromAi.getNameTextDescriptor());
    }

    /**
     * Copies constraints (Rigid, Ends Extended, etc) from another arcinst to this arcinst
     * @param fromAi the arcinst from which to copy constraints
     */
    public void copyConstraintsFrom(ArcInst fromAi) {
        if (fromAi == null) return;
        /*
        setRigid(fromAi.isRigid());
        setFixedAngle(fromAi.isFixedAngle());
        setSlidable(fromAi.isSlidable());
        setExtended(fromAi.isExtended());
        setDirectional(fromAi.isDirectional());
        setSkipHead(fromAi.isSkipHead());
        setSkipTail(fromAi.isSkipTail());
        setReverseEnds(fromAi.isReverseEnds());
        setHardSelect(fromAi.isHardSelect());
        */
        // equivalent:
        lowLevelSetUserbits(fromAi.lowLevelGetUserbits());
		getHead().setNegated(fromAi.getHead().isNegated());
		getTail().setNegated(fromAi.getTail().isNegated());
    }

	/**
	 * Method to set this ArcInst to be rigid.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
     * @param state
     */
	public void setRigid(boolean state) {
        if (state)
            userBits |= FIXED;
        else
            userBits &= ~FIXED;
   }

	/**
	 * Method to tell whether this ArcInst is rigid.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
	 * @return true if this ArcInst is rigid.
	 */
	public boolean isRigid() { return (userBits & FIXED) != 0; }

	/**
	 * Method to set this ArcInst to be fixed-angle.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
     * @param state
     */
	public void setFixedAngle(boolean state) {
        if (state)
            userBits |= FIXANG;
        else
            userBits &= ~FIXANG;
    }

	/**
	 * Method to tell whether this ArcInst is fixed-angle.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
	 * @return true if this ArcInst is fixed-angle.
	 */
	public boolean isFixedAngle() { return (userBits & FIXANG) != 0; }

	/**
	 * Method to set this ArcInst to be slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
     * @param state
     */
	public void setSlidable(boolean state) {
        if (state)
            userBits &= ~CANTSLIDE;
        else
            userBits |= CANTSLIDE;
    }

	/**
	 * Method to tell whether this ArcInst is slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
	 * @return true if this ArcInst is slidable.
	 */
	public boolean isSlidable() { return (userBits & CANTSLIDE) == 0; }

	/**
	 * Method to set that this rigid ArcInst was modified.
	 * This is used during constraint processing only and should not be used elsewhere.
	 */
	public void setRigidModified() { userBits &= ~FIXEDMOD; }

	/**
	 * Method to set that this rigid ArcInst was not modified.
	 * This is used during constraint processing only and should not be used elsewhere.
	 */
	public void clearRigidModified() { userBits |= FIXEDMOD; }

	/**
	 * Method to tell whether this rigid ArcInst was modified.
	 * This is used during constraint processing only and should not be used elsewhere.
	 * @return true if this rigid ArcInst was modified.
	 */
	public boolean isRigidModified() { return (userBits & FIXEDMOD) == 0; }

	/**
	 * Method to set default constraint information on this ArcInst.
	 */
	private void setDefaultConstraints()
	{
        setRigid(protoType.isRigid());
        setFixedAngle(protoType.isFixedAngle());
        setSlidable(protoType.isSlidable());
        setExtended(protoType.isExtended());
        setDirectional(protoType.isDirectional());
	}

	/**
	 * Low-level method to set the ArcInst angle in the "user bits".
	 * This general access to the bits is required because the ELIB
	 * file format stores it this way.
	 * This should not normally be called by any other part of the system.
	 * If you need to set the angle of an arc, use "setAngle".
	 * @param angle the angle of the ArcInst (in degrees).
	 */
	public void lowLevelSetArcAngle(int angle) { userBits = (userBits & ~AANGLE) | (angle << AANGLESH); }

	/**
	 * Low-level method to get the ArcInst angle from the "user bits".
	 * This general access to the bits is required because the ELIB
	 * file format stores it this way.
	 * This should not normally be called by any other part of the system.
	 * @return the arc angle (in degrees).
	 */
	public int lowLevelGetArcAngle() { return (userBits & AANGLE) >> AANGLESH; }

	/**
	 * Method to set this ArcInst to have its ends extended.
	 * End-extension causes an arc to extend past its endpoint by half of its width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
     * @param state
     */
	public void setExtended(boolean state)
	{
		if (state)
            userBits &= ~NOEXTEND;
        else
            userBits |= NOEXTEND;
		updateGeometric(getAngle());
	}

	/**
	 * Method to tell whether this ArcInst has its ends extended.
	 * End-extension causes an arc to extend past its endpoint by half of its width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @return true if this ArcInst has its ends extended.
	 */
	public boolean isExtended() { return (userBits & NOEXTEND) == 0; }

	/**
	 * Method to set this ArcInst to be directional.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * The arrow head is on the arc's head end, unless the arc is reversed.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @see ArcInst#setReverseEnds
     * @param state
     */
	public void setDirectional(boolean state) {
        if (state)
            userBits |= ISDIRECTIONAL;
        else
            userBits &= ~ISDIRECTIONAL;
    }

	/**
	 * Method to tell whether this ArcInst is directional.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * The arrow head is on the arc's head end, unless the arc is reversed.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @return true if this ArcInst is directional.
	 * @see ArcInst#setReverseEnds
	 */
	public boolean isDirectional() { return (userBits & ISDIRECTIONAL) != 0; }

	/**
	 * Method to set this ArcInst to have its head skipped.
	 * Skipping the head causes any special actions that are normally applied to the
	 * head to be ignored.  For example, the directional arrow is on the arc head,
	 * so skipping the head will remove the arrow-head, but not the body of the arrow.
     * @param state
     */
	public void setSkipHead(boolean state)
	{
		if (state)
            userBits |= NOTEND0;
        else
            userBits &= ~NOTEND0;
		updateGeometric(getAngle());
	}

	/**
	 * Method to tell whether this ArcInst has its head skipped.
	 * Skipping the head causes any special actions that are normally applied to the
	 * head to be ignored.  For example, the directional arrow is on the arc head,
	 * so skipping the head will remove the arrow-head, but not the body of the arrow.
	 * @return true if this ArcInst has its head skipped.
	 */
	public boolean isSkipHead() { return (userBits & NOTEND0) != 0; }

	/**
	 * Method to set this ArcInst to have its tail skipped.
	 * Skipping the tail causes any special actions that are normally applied to the
	 * tail to be ignored.  For example, the negating bubble is on the arc tail,
	 * so skipping the tail will remove the bubble.
     * @param state
     */
	public void setSkipTail(boolean state)
	{
		if (state)
            userBits |= NOTEND1;
        else
            userBits &= ~NOTEND1;
		updateGeometric(getAngle());
	}

	/**
	 * Method to tell whether this ArcInst has its tail skipped.
	 * Skipping the tail causes any special actions that are normally applied to the
	 * tail to be ignored.  For example, the negating bubble is on the arc tail,
	 * so skipping the tail will remove the bubble.
	 * @return true if this ArcInst has its tail skipped.
	 */
	public boolean isSkipTail() { return (userBits & NOTEND1) != 0; }

	/**
	 * Method to reverse the ends of this ArcInst.
	 * A reversed arc switches its head and tail.
	 * This is useful if the negating bubble appears on the wrong end.
     * @param state
     */
	public void setReverseEnds(boolean state)
	{
		if (state)
            userBits |= REVERSEEND;
        else
            userBits &= ~REVERSEEND;
		updateGeometric(getAngle());
	}

	/**
	 * Method to tell whether this ArcInst has been reversed.
	 * A reversed arc switches its head and tail.
	 * This is useful if the negating bubble appears on the wrong end.
	 * @return true if this ArcInst has been reversed.
	 */
	public boolean isReverseEnds() { return (userBits & REVERSEEND) != 0; }

	/**
	 * Method to set this ArcInst to be hard-to-select.
	 * Hard-to-select ArcInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
     * @param state
     */
	public void setHardSelect(boolean state) {
        if (state)
            userBits |= HARDSELECTA;
        else
            userBits &= ~HARDSELECTA;
    }

	/**
	 * Method to tell whether this ArcInst is hard-to-select.
	 * Hard-to-select ArcInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 * @return true if this ArcInst is hard-to-select.
	 */
	public boolean isHardSelect() { return (userBits & HARDSELECTA) != 0; }

    /**
     * This function is to compare NodeInst elements. Initiative CrossLibCopy
     * @param obj Object to compare to
     * @param buffer To store comparison messages in case of failure
     * @return True if objects represent same ArcInst
     */
    public boolean compare(Object obj, StringBuffer buffer)
	{
		if (this == obj) return (true);

        // Better if compare classes? but it will crash with obj=null
        if (obj == null || getClass() != obj.getClass())
            return (false);

        ArcInst a = (ArcInst)obj;
         if (protoType.getClass() != a.getProto().getClass())
            return (false);

        // Not sure if I should defina myEquals for Geometric
        ArcProto arcType = a.getProto();
		Technology tech = arcType.getTechnology();
        if (getProto().getTechnology() != tech)
        {
	        if (buffer != null)
		        buffer.append("No same technology for arcs " + getName() + " and " + a.getName() + "\n");
            return (false);
        }

		Poly[] polyList = getProto().getTechnology().getShapeOfArc(this);
        Poly[] aPolyList = tech.getShapeOfArc(a);

         if (polyList.length != aPolyList.length)
         {
	         if (buffer != null)   
		         buffer.append("No same number of geometries in " + getName() + " and " + a.getName() + "\n");
	         return (false);
         }

        // Remove noCheckList if equals is implemented
        // Sort them out by a key so comparison won't be O(n2)
        List noCheckAgain = new ArrayList();
        for (int i = 0; i < polyList.length; i++)
        {
            boolean found = false;
            for (int j = 0; j < aPolyList.length; j++)
            {
                // Already found
                if (noCheckAgain.contains(aPolyList[j])) continue;
                if (polyList[i].compare(aPolyList[j], buffer))
                {
                    found = true;
                    noCheckAgain.add(aPolyList[j]);
                    break;
                }
            }
            // polyList[i] doesn't match any elem in noPolyList
            if (!found)
            {
                // No message otherwise all comparisons are found in buffer
                /*
	            if (buffer != null)
		            buffer.append("No corresponding geometry in " + getName() + " found in " + a.getName() + "\n");
                    */
	            return (false);
            }
        }
        return (true);
    }
}

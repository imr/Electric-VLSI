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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

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
public class ArcInst extends Geometric /*implements Networkable*/
{
	// -------------------------- private data ----------------------------------

	/** fixed-length arc */								private static final int FIXED =                     01;
	/** fixed-angle arc */								private static final int FIXANG =                    02;
	/** arc has text that is far away */				private static final int AHASFARTEXT =               04;
//	/** arc is not in use */							private static final int DEADA =                    020;
	/** angle of arc from end 0 to end 1 */				private static final int AANGLE =                037740;
	/** bits of right shift for AANGLE field */			private static final int AANGLESH =                   5;
	/** set if arc is to be drawn shortened */			private static final int ASHORT =                040000;
	/** set if ends do not extend by half width */		private static final int NOEXTEND =             0400000;
	/** set if ends are negated */						private static final int ISNEGATED =           01000000;
	/** set if arc aims from end 0 to end 1 */			private static final int ISDIRECTIONAL =       02000000;
	/** no extension/negation/arrows on end 0 */		private static final int NOTEND0 =             04000000;
	/** no extension/negation/arrows on end 1 */		private static final int NOTEND1 =            010000000;
	/** reverse extension/negation/arrow ends */		private static final int REVERSEEND =         020000000;
	/** set if arc can't slide around in ports */		private static final int CANTSLIDE =          040000000;
//	/** if on, this arcinst is marked for death */		private static final int KILLA =             0200000000;
//	/** arcinst re-drawing is scheduled */				private static final int REWANTA =           0400000000;
//	/** only local arcinst re-drawing desired */		private static final int RELOCLA =          01000000000;
//	/**transparent arcinst re-draw is done */			private static final int RETDONA =          02000000000;
//	/** opaque arcinst re-draw is done */				private static final int REODONA =          04000000000;
	/** general flag for spreading and highlighting */	private static final int ARCFLAGBIT =      010000000000;
	/** set if hard to select */						private static final int HARDSELECTA =     020000000000;

	// Name of the variable holding the ArcInst's name.
	private static final String VAR_ARC_NAME = "ARC_name";

	/** flags for this arc instance */					private int userBits;
	/** width of this arc instance */					private double arcWidth;
	/** prototype of this arc instance */				private ArcProto protoType;
	/** end connections of this arc instance */			private Connection head, tail;

	// -------------------- private and protected methods ------------------------
	
	private ArcInst()
	{
		this.userBits = 0;
	}

	/**
	 * recalculate the transform on this arc to get the endpoints
	 * where they should be.
	 */
	void updateGeometric()
	{
		Point2D.Double p1 = head.getLocation();
		Point2D.Double p2 = tail.getLocation();
		double dx = p2.x - p1.x;
		double dy = p2.y - p1.y;
		double len = Math.sqrt(dx * dx + dy * dy);
		this.sY = arcWidth;
		this.sX = len;
		this.cX = (p1.x + p2.x) / 2;
		this.cY = (p1.y + p2.y) / 2;
		this.angle = Math.atan2(dy, dx);
		updateGeometricBounds();
	}

	/** set the connection on a particular end */
	void setConnection(Connection c, boolean onHead)
	{
		if (onHead) head = c; else
			tail = c;
	}

	/** get the connection on a particular end */
	public Connection getConnection(boolean onHead)
	{
		return onHead ? head : tail;
	}

	/** get the Head connection */
	public Connection getHead() { return head; }

	/** get the Tail connection */
	public Connection getTail() { return tail; }

	// Remove this ArcInst.  Will also remove the connections on either side.
//	public void remove()
//	{
//		head.remove();
//		tail.remove();
//		getParent().removeArc(this);
//		super.remove();
//	}

	/** remove the connection from a particular end */
	void removeConnection(Connection c, boolean onHead)
	{
		/* safety check */
		if ((onHead ? head : tail) != c)
		{
			System.out.println("Tried to remove the wrong connection from a wire end: "
				+ c + " on " + this);
		}
		if (onHead) head = null; else
			tail = null;
	}

	/** Reports information about this ArcInst */
	public void getInfo()
	{
		System.out.println("--------- ARC INSTANCE: ---------");
		System.out.println(" ArcProto: " + protoType.describe());
		Point2D loc = head.getLocation();
		System.out.println(" Head on " + head.getPortInst().getNodeInst().getProto().describe() +
			" at (" + loc.getX() + "," + loc.getY() + ")");

		loc = tail.getLocation();
		System.out.println(" Tail on " + tail.getPortInst().getNodeInst().getProto().describe() +
			" at (" + loc.getX() + "," + loc.getY() + ")");
		super.getInfo();
	}

	// -------------------------- public methods -----------------------------

	/**
	 * Low-level access routine to create a ArcInst.
	 */
	public static ArcInst lowLevelAllocate()
	{
		ArcInst ai = new ArcInst();
		return ai;
	}

	/**
	 * Low-level routine to fill-in the ArcInst information.
	 * Returns true on error.
	 */
	public boolean lowLevelPopulate(ArcProto protoType, double arcWidth,
		PortInst a, double aX, double aY, PortInst b, double bX, double bY)
	{
		// initialize this object
		this.protoType = protoType;

		if (arcWidth <= 0)
			arcWidth = protoType.getWidth();
		this.arcWidth = arcWidth;

		Cell parent = a.getNodeInst().getParent();
		if (parent != b.getNodeInst().getParent())
		{
			System.out.println("ArcProto.newInst: the 2 PortInsts are in different Cells!");
			return true;
		}
		this.parent = parent;

		// make sure the arc can connect to these ports
//		PortProto pa = a.getPortProto();
//		PrimitivePort ppa = (PrimitivePort)(pa.getBasePort());
//		if (!ppa.connectsTo(protoType))
//		{
//			System.out.println("Cannot create " + protoType.describe() + " arc in cell " + parent.describe() +
//				" because it cannot connect to port " + pa.getProtoName());
//			return true;
//		}
//		PortProto pb = b.getPortProto();
//		PrimitivePort ppb = (PrimitivePort)(pb.getBasePort());
//		if (!ppb.connectsTo(protoType))
//		{
//			System.out.println("Cannot create " + protoType.describe() + " arc in cell " + parent.describe() +
//				" because it cannot connect to port " + pb.getProtoName());
//			return true;
//		}

		// create node/arc connections and place them properly
		head = new Connection(this, a, aX, aY);
		tail = new Connection(this, b, bX, bY);
		
		// fill in the geometry
		updateGeometric();
		linkGeom(parent);

		return false;
	}

	/**
	 * Low-level access routine to link the ArcInst into its Cell.
	 * Returns true on error.
	 */
	public boolean lowLevelLink()
	{
		// attach this arc to the two nodes it connects
		head.getPortInst().getNodeInst().addConnection(head);
		tail.getPortInst().getNodeInst().addConnection(tail);

		// add this arc to the cell
		Cell parent = getParent();
		parent.addArc(this);
		return false;
	}

	/** Create a new ArcInst connecting two PortInsts.
	 *
	 * <p> Arcs are connected to the <i>center</i> of the PortInsts. If
	 * the two PortInst centers don't line up vertically or
	 * horizontally, then generate 'L' shaped wires by first routing
	 * horizontally from PortInst a and then vertically to PortInst b.
	 * @param width the width of this material.  Width
	 * must be >0. A zero width means "use the default width".
	 * @param a start PortInst
	 * @param b end PortInst */
	public static ArcInst newInstance(ArcProto type, double width, PortInst a, PortInst b)
	{
		Rectangle2D aBounds = a.getBounds();
		double aX = aBounds.getCenterX();
		double aY = aBounds.getCenterY();
		Rectangle2D bBounds = b.getBounds();
		double bX = bBounds.getCenterX();
		double bY = bBounds.getCenterY();
		return newInstance(type, width, a, aX, aY, b, bX, bY);
	}

	/** Create an ArcInst connecting two PortInsts at the specified
	 * locations.
	 *
	 * <p> The locations must lie within their respective ports.
	 *
	 * <p> This routine presents the full generality of Electric's
	 * database.  However, I've never found it to be useful. I recommend
	 * using the other newInst method. */
	public static ArcInst newInstance(ArcProto type, double arcWidth,
		PortInst a, double aX, double aY, PortInst b, double bX, double bY)
	{
		ArcInst ai = lowLevelAllocate();
		if (ai.lowLevelPopulate(type, arcWidth, a, aX, aY, b, bX, bY)) return null;
		if (ai.lowLevelLink()) return null;
		return ai;
	}

	/** Set the Rigid bit */
	public void setRigid() { userBits |= FIXED; }
	/** Clear the Rigid bit */
	public void clearRigid() { userBits &= ~FIXED; }
	/** Get the Rigid bit */
	public boolean isRigid() { return (userBits & FIXED) != 0; }

	/** Set the Fixed-angle bit */
	public void setFixedAngle() { userBits |= FIXANG; }
	/** Clear the Fixed-angle bit */
	public void clearFixedAngle() { userBits &= ~FIXANG; }
	/** Get the Fixed-angle bit */
	public boolean isFixedAngle() { return (userBits & FIXANG) != 0; }

	/** Set the Slidable bit */
	public void setSlidable() { userBits &= ~CANTSLIDE; }
	/** Clear the Slidable bit */
	public void clearSlidable() { userBits |= CANTSLIDE; }
	/** Get the Slidable bit */
	public boolean isSlidable() { return (userBits & CANTSLIDE) == 0; }

	/** Set the Far text bit */
	public void setFarText() { userBits |= AHASFARTEXT; }
	/** Clear the Far text bit */
	public void clearFarText() { userBits &= ~AHASFARTEXT; }
	/** Get the Far text bit */
	public boolean isFarText() { return (userBits & AHASFARTEXT) != 0; }

	/** Low-level routine to set the Arc angle value.  Should not normally be called. */
	public void lowLevelSetArcAngle(int angle) { userBits = (userBits & ~AANGLE) | (angle << AANGLESH); }
	/** Low-level routine to get the Arc angle value.  Should not normally be called. */
	public int lowLevelGetArcAngle() { return (userBits & AANGLE) >> AANGLESH; }

	/** Set the Shorten bit */
	public void setShortened() { userBits |= ASHORT; }
	/** Clear the Shorten bit */
	public void clearShortened() { userBits &= ~ASHORT; }
	/** Get the Shorten bit */
	public boolean isShortened() { return (userBits & ASHORT) != 0; }

	/** Set the ends-extend bit */
	public void setExtended() { userBits &= ~NOEXTEND; }
	/** Clear the ends-extend bit */
	public void clearExtended() { userBits |= NOEXTEND; }
	/** Get the ends-extend bit */
	public boolean isExtended() { return (userBits & NOEXTEND) == 0; }

	/** Set the Negated bit */
	public void setNegated() { userBits |= ISNEGATED; }
	/** Clear the Negated bit */
	public void clearNegated() { userBits &= ~ISNEGATED; }
	/** Get the Negated bit */
	public boolean isNegated() { return (userBits & ISNEGATED) != 0; }

	/** Set the Directional bit */
	public void setDirectional() { userBits |= ISDIRECTIONAL; }
	/** Clear the Directional bit */
	public void clearDirectional() { userBits &= ~ISDIRECTIONAL; }
	/** Get the Directional bit */
	public boolean isDirectional() { return (userBits & ISDIRECTIONAL) != 0; }

	/** Set the Skip-Head bit */
	public void setSkipHead() { userBits |= NOTEND0; }
	/** Clear the Skip-Head bit */
	public void clearSkipHead() { userBits &= ~NOTEND0; }
	/** Get the Skip-Head bit */
	public boolean isSkipHead() { return (userBits & NOTEND0) != 0; }

	/** Set the Skip-Tail bit */
	public void setSkipTail() { userBits |= NOTEND1; }
	/** Clear the Skip-Tail bit */
	public void clearSkipTail() { userBits &= ~NOTEND1; }
	/** Get the Skip-Tail bit */
	public boolean isSkipTail() { return (userBits & NOTEND1) != 0; }

	/** Set the Reverse-ends bit */
	public void setReverseEnds() { userBits |= REVERSEEND; }
	/** Clear the Reverse-ends bit */
	public void clearReverseEnds() { userBits &= ~REVERSEEND; }
	/** Get the Reverse-ends bit */
	public boolean isReverseEnds() { return (userBits & REVERSEEND) != 0; }

	/** Set the general flag bit */
	public void setFlagged() { userBits |= ARCFLAGBIT; }
	/** Clear the general flag bit */
	public void clearFlagged() { userBits &= ~ARCFLAGBIT; }
	/** Get the general flag bit */
	public boolean isFlagged() { return (userBits & ARCFLAGBIT) != 0; }

	/** Set the Hard-to-Select bit */
	public void setHardSelect() { userBits |= HARDSELECTA; }
	/** Clear the Hard-to-Select bit */
	public void clearHardSelect() { userBits &= ~HARDSELECTA; }
	/** Get the Hard-to-Select bit */
	public boolean isHardSelect() { return (userBits & HARDSELECTA) != 0; }

	/** Low-level routine to get the flag bits.  Should not normally be called. */
	public int lowLevelGetUserbits() { return userBits; }
	/** Low-level routine to set the flag bits.  Should not normally be called. */
	public void lowLevelSetUserbits(int userBits) { this.userBits = userBits; }

	/** Get the width of this ArcInst.
	 *
	 * <p> Note that this call excludes material surrounding this
	 * ArcInst. For example, if this is a diffusion ArcInst then return
	 * the width of the diffusion and ignore the width of well and
	 * select. */
	public double getWidth()
	{
		return arcWidth - ((PrimitiveArc)protoType).getWidthOffset();
	}

	/** Change the width of this ArcInst.
	 * @param dWidth the change to the arc width.
	 * means "use the default width". */
	public void modify(double dWidth)
	{
		arcWidth += dWidth;
		updateGeometric();
//		Electric.modifyArcInst(getAddr(), dWidth);
	}

	/** Get the ArcProto of this ArcInst. */
	public ArcProto getProto()
	{
		return protoType;
	}

	/**
	 * Set the name of this ArcInst.
	 */
	public Variable setName(String name)
	{
		Variable var = setVal(VAR_ARC_NAME, name);
		if (var != null) var.setDisplay();
		return var;
	}

	/**
	 * Get the name of this ArcInst.
	 */
	public String getName()
	{
		Variable var = getVal(VAR_ARC_NAME, String.class);
		if (var == null) return null;
		return (String) var.getObject();
	}

	public Poly makearcpoly(double len, double wid, Poly.Type style)
	{
		Point2D.Double end1 = head.getLocation();
		Point2D.Double end2 = tail.getLocation();

		// zero-width polygons are simply lines
		if (wid == 0)
		{
			Poly poly = new Poly(new Point2D.Double[]{end1, end2});
			if (style == Poly.Type.FILLED) style = Poly.Type.OPENED;
			poly.setStyle(style);
			return poly;
		}

		// determine the end extension on each end
		double e1 = wid/2;
		double e2 = wid/2;
		if (!isExtended())
		{
			// nonextension arc: set extension to zero for all included ends
			if (!isSkipTail()) e1 = 0;
			if (!isSkipHead()) e2 = 0;
//		} else if (isShortened())
//		{
//			// shortened arc: compute variable extension
//			e1 = tech_getextendfactor(wid, ai->endshrink&0xFFFF);
//			e2 = tech_getextendfactor(wid, (ai->endshrink>>16)&0xFFFF);
		}

		// make the polygon
		Poly poly = makeEndPointPoly(len, wid, getAngle(), end1, e1, end2, e2);
		if (poly != null) poly.setStyle(style);
		return poly;
	}

	Poly makeEndPointPoly(double len, double wid, double angle, Point2D end1, double e1,
		Point2D end2, double e2)
	{
		double temp, xextra, yextra, xe1, ye1, xe2, ye2, w2, sa, ca;

		w2 = wid / 2;
		double x1 = end1.getX();   double y1 = end1.getY();
		double x2 = end2.getX();   double y2 = end2.getY();

		/* somewhat simpler if rectangle is manhattan */
		if (angle < 0) angle += Math.PI * 2;
		if (angle == Math.PI/2 || angle == Math.PI/2*3)
		{
			if (y1 > y2)
			{
				temp = y1;   y1 = y2;   y2 = temp;
				temp = e1;   e1 = e2;   e2 = temp;
			}
			Poly poly = new Poly(new Point2D.Double[] {
				new Point2D.Double(x1 - w2, y1 - e1),
				new Point2D.Double(x1 + w2, y1 - e1),
				new Point2D.Double(x2 + w2, y2 + e2),
				new Point2D.Double(x2 - w2, y2 + e2)});
			return poly;
		}
		if (angle == 0 || angle == Math.PI)
		{
			if (x1 > x2)
			{
				temp = x1;   x1 = x2;   x2 = temp;
				temp = e1;   e1 = e2;   e2 = temp;
			}
			Poly poly = new Poly(new Point2D.Double[] {
				new Point2D.Double(x1 - e1, y1 - w2),
				new Point2D.Double(x1 - e1, y1 + w2),
				new Point2D.Double(x2 + e2, y2 + w2),
				new Point2D.Double(x2 + e2, y2 - w2)});
			return poly;
		}

		/* nonmanhattan arcs cannot have zero length so re-compute it */
//		if (len == 0) len = computedistance(x1,y1, x2,y2);
//		if (len == 0)
//		{
//			sa = sine(angle);
//			ca = cosine(angle);
//			xe1 = x1 - mult(ca, e1);
//			ye1 = y1 - mult(sa, e1);
//			xe2 = x2 + mult(ca, e2);
//			ye2 = y2 + mult(sa, e2);
//			xextra = mult(ca, w2);
//			yextra = mult(sa, w2);
//		} else
//		{
//			/* work out all the math for nonmanhattan arcs */
//			xe1 = x1 - muldiv(e1, (x2-x1), len);
//			ye1 = y1 - muldiv(e1, (y2-y1), len);
//			xe2 = x2 + muldiv(e2, (x2-x1), len);
//			ye2 = y2 + muldiv(e2, (y2-y1), len);
//
//			/* now compute the corners */
//			xextra = muldiv(w2, (x2-x1), len);
//			yextra = muldiv(w2, (y2-y1), len);
//		}
//		Poly poly = new Poly(new Point2D.Double[] {
//			new Point2D.Double(yextra + xe1, ye1 - xextra),
//			new Point2D.Double(xe1 - yextra, xextra + ye1),
//			new Point2D.Double(xe2 - yextra, xextra + ye2),
//			new Point2D.Double(yextra + xe2, ye2 - xextra)});
//		return poly;
		return null;
	}

	/**
	 * Routine to describe this ArcInst as a string.
	 */
	public String describe()
	{
		return protoType.getProtoName();
	}

	/** Printable version of this ArcInst */
	public String toString()
	{
		return "ArcInst " + protoType.getProtoName();
	}

}

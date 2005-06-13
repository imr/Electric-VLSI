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
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.ImmutableTextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
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
public class ArcInst extends Geometric implements Comparable
{
	/** The index of the tail of this ArcInst. */		public static final int TAILEND = 0;
	/** The index of the head of this ArcInst. */		public static final int HEADEND = 1;

	/** special name for text descriptor of arc name */	public static final String ARC_NAME_TD = new String("ARC_name");
	/** Key of the obsolete variable holding arc name.*/public static final Variable.Key ARC_NAME = ElectricObject.newKey("ARC_name");
	/** Key of Varible holding arc curvature. */		public static final Variable.Key ARC_RADIUS = ElectricObject.newKey("ARC_radius");

	/** Minimal distance of arc end to port polygon. */	static final double MINPORTDISTANCE = DBMath.getEpsilon()*0.71; // sqrt(0.5)

	// -------------------------- private data ----------------------------------

	/** fixed-length arc */                                 private static final int FIXED =                     01;
	/** fixed-angle arc */                                  private static final int FIXANG =                    02;
//	/** arc has text that is far away */                    private static final int AHASFARTEXT =               04;
//	/** arc is not in use */                                private static final int DEADA =                    020;
    /** angle of arc from end 0 to end 1 in 10th degrees */ private static final int AANGLE =                037774;
    /** bits of right shift for AANGLE shift */             private static final int AANGLESH =                   2;
	/** DISK: angle of arc from end 0 to end 1 */           private static final int DISK_AANGLE =           037740;
	/** DISK: bits of right shift for DISK_AANGLE field */  private static final int DISK_AANGLESH =              5;
//	/** set if arc is to be drawn shortened */              private static final int ASHORT =                040000;
	/** set if head end of ArcInst is negated */            private static final int ISHEADNEGATED =        0200000;
	/** DISK: set if ends do not extend by half width */    private static final int DISK_NOEXTEND =        0400000;
	/** set if tail end of ArcInst is negated */            private static final int ISTAILNEGATED =       01000000;
//	/** set if ends are negated */                          private static final int ISNEGATED =           01000000;
	/** set if arc has arrow on head end */                 private static final int HEADARROW =           02000000;
    /** DISK: set if arc aims from end 0 to end 1 */        private static final int DISK_ISDIRECTIONAL =  02000000; 
	/** no extension on tail */                             private static final int TAILNOEXTEND =        04000000;
    /** DISK: no extension/negation/arrows on end 0 */      private static final int DISK_NOTEND0 =        04000000;
	/** no extension on head */                             private static final int HEADNOEXTEND =       010000000;
    /** DISK: no extension/negation/arrows on end 1 */      private static final int DISK_NOTEND1 =       010000000;
	/** DISK: reverse extension/negation/arrow ends */      private static final int DISK_REVERSEEND =    020000000;
	/** set if arc can't slide around in ports */           private static final int CANTSLIDE =          040000000;
//	/** set if afixed arc was changed */                    private static final int FIXEDMOD =          0100000000;
	/** set if arc has arrow on tail end */                 private static final int TAILARROW =         0200000000;
	/** set if arc has arrow line along body */             private static final int BODYARROW =         0400000000;
//	/** only local arcinst re-drawing desired */            private static final int RELOCLA =          01000000000;
//	/**transparent arcinst re-draw is done */               private static final int RETDONA =          02000000000;
//	/** opaque arcinst re-draw is done */                   private static final int REODONA =          04000000000;
//	/** general flag for spreading and highlighting */      private static final int ARCFLAGBIT =      010000000000;
	/** set if hard to select */                            private static final int HARDSELECTA =     020000000000;
    
    /** bits with common meaniong in disk and database */   private static int COMMON_BITS = FIXED | FIXANG | CANTSLIDE | HARDSELECTA;  
    /** bits used in database */                            private static final int DATABASE_BITS = COMMON_BITS | AANGLE | BODYARROW |
            ISTAILNEGATED | TAILNOEXTEND | TAILARROW | ISHEADNEGATED | HEADNOEXTEND | HEADARROW; 

	/** prefix for autonameing. */						private static final Name BASENAME = Name.findName("net@");

	/** name of this ArcInst. */						private Name name;
	/** duplicate index of this ArcInst in the Cell */  private int duplicate = -1;
	/** The text descriptor of name of ArcInst. */		private ImmutableTextDescriptor nameDescriptor;
	/** bounds after transformation. */					private Rectangle2D visBounds;
	/** Flag bits for this ArcInst. */					private int userBits;
	/** The timestamp for changes. */					private int changeClock;
	/** The Change object. */							private Undo.Change change;

	/** width of this ArcInst. */						private double width;
	/** length of this ArcInst. */						private double length;
	/** prototype of this arc instance */				private ArcProto protoType;

	/** PortInst on tail end of this arc instance */	/*package*/PortInst tailPortInst;
	/** Location of tail end of this arc instance */	/*package*/EPoint tailLocation;
	/** the tail shrinkage is from 0 to 90 */			/*package*/byte tailShrink;
	/** tail connection of this arc instance */			private TailConnection tailEnd;

	/** PortInst on head end of this arc instance */	/*package*/PortInst headPortInst;
	/** Location of head end of this arc instance */	/*package*/EPoint headLocation;
	/** the head shrinkage is from 0 to 90 */			/*package*/byte headShrink;
	/** head connection of this arc instance */			private HeadConnection headEnd;

	/** 0-based index of this ArcInst in cell. */		private int arcIndex = -1;

	/**
	 * Private constructor of ArcInst.
     * @param parent the parent Cell of this ArcInst
	 * @param protoType the ArcProto of this ArcInst.
	 * @param name the name of this ArcInst
	 * @param duplicate duplicate index of this ArcInst
     * @param nameDescriptor text descriptor of name of this ArcInst
	 * @param headPort the head end PortInst.
	 * @param tailPort the tail end PortInst.
	 * @param headPt the coordinate of the head end PortInst.
	 * @param tailPt the coordinate of the tail end PortInst.
	 * @param width the width of this ArcInst.
     * @param userBits flag bits
	 */
	private ArcInst(Cell parent, ArcProto protoType, String name, int duplicate, ImmutableTextDescriptor nameDescriptor,
        PortInst headPort, PortInst tailPort, EPoint headPt, EPoint tailPt, double width, int userBits)
	{
		// initialize this object
		assert parent == headPort.getNodeInst().getParent();
        assert parent == tailPort.getNodeInst().getParent();
		this.parent = parent;
		this.protoType = protoType;

		if (width < 0)
			width = protoType.getWidth();
		this.width = DBMath.round(width);

		this.name = Name.findName(name);
		this.duplicate = duplicate;
        if (nameDescriptor == null) nameDescriptor = ImmutableTextDescriptor.getArcTextDescriptor();
        this.nameDescriptor = nameDescriptor;

		// create node/arc connections and place them properly
		tailPortInst = tailPort;
		tailLocation = tailPt;
		tailEnd = new TailConnection(this);

		headPortInst = headPort;
		headLocation = headPt;
		headEnd = new HeadConnection(this);
		
        this.userBits = userBits & DATABASE_BITS;
//		// fill in the geometry
//		updateGeometric(defAngle);
        
		this.visBounds = new Rectangle2D.Double(0, 0, 0, 0);
	}

	/****************************** CREATE, DELETE, MODIFY ******************************/

	/**
	 * Method to create a new ArcInst with appropriate defaults, connecting two PortInsts.
	 * Since no coordinates are given, the ArcInst connects to the center of the PortInsts.
	 * @param type the prototype of the new ArcInst.
	 * @param width the width of the new ArcInst.  The width must be > 0.
	 * @param head the head end PortInst.
	 * @param tail the tail end PortInst.
	 * @return the newly created ArcInst, or null if there is an error.
	 */
	public static ArcInst makeInstance(ArcProto type, double width, PortInst head, PortInst tail)
	{
		ArcInst ai = newInstance(type, width, head, tail);
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
	 * @param tail the tail end PortInst.
	 * @param headPt the coordinate of the head end PortInst.
	 * @param tailPt the coordinate of the tail end PortInst.
	 * @param name the name of the new ArcInst
	 * @return the newly created ArcInst, or null if there is an error.
	 */
	public static ArcInst makeInstance(ArcProto type, double width, PortInst head, PortInst tail,
	                                   Point2D headPt, Point2D tailPt, String name)
	{
		ArcInst ai = newInstance(type, width, head, tail, headPt, tailPt, name, 0);
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
	public static ArcInst makeDummyInstance(ArcProto ap, double arcLength)
	{
		PrimitiveNode npEnd = ap.findPinProto();
		if (npEnd == null)
		{
			System.out.println("Cannot find pin for arc " + ap.describe());
			return null;
		}

		// create the head node
		NodeInst niH = NodeInst.makeDummyInstance(npEnd, new EPoint(-arcLength/2,0), npEnd.getDefWidth(), npEnd.getDefHeight(), 0);
		PortInst piH = niH.getOnlyPortInst();
		Rectangle2D boundsH = piH.getBounds();
		double xH = boundsH.getCenterX();
		double yH = boundsH.getCenterY();

		// create the tail node
		NodeInst niT = NodeInst.makeDummyInstance(npEnd, new EPoint(arcLength/2,0), npEnd.getDefWidth(), npEnd.getDefHeight(), 0);
		PortInst piT = niT.getOnlyPortInst();
		Rectangle2D boundsT = piT.getBounds();
		double xT = boundsT.getCenterX();
		double yT = boundsT.getCenterY();

		// create the arc that connects them
		ArcInst ai = new ArcInst(null, ap, "", 0, null, piH, piT, new EPoint(xH, yH), new EPoint(xT, yT), ap.getDefaultWidth(), 0);

        ai.updateGeometric();
		return ai;
	}

	/**
	 * Method to create a new ArcInst connecting two PortInsts.
	 * Since no coordinates are given, the ArcInst connects to the center of the PortInsts.
	 * @param type the prototype of the new ArcInst.
	 * @param width the width of the new ArcInst.  The width must be > 0.
	 * @param head the head end PortInst.
	 * @param tail the tail end PortInst.
	 * @return the newly created ArcInst, or null if there is an error.
	 */
	public static ArcInst newInstance(ArcProto type, double width, PortInst head, PortInst tail)
	{
		return newInstance(type, width, head, tail, null, null, null, 0);
	}

	/**
	 * Method to create a new ArcInst connecting two PortInsts at specified locations.
	 * This is more general than the version that does not take coordinates.
	 * @param type the prototype of the new ArcInst.
	 * @param width the width of the new ArcInst.  The width must be > 0.
	 * @param head the head end PortInst.
	 * @param tail the tail end PortInst.
	 * @param headPt the coordinate of the head end PortInst.
	 * @param tailPt the coordinate of the tail end PortInst.
	 * @param name the name of the new ArcInst
	 * @param defAngle default angle in case port points coincide
     * @return the newly created ArcInst, or null if there is an error.
	 */
	public static ArcInst newInstance(ArcProto type, double width, PortInst head, PortInst tail,
	                                  Point2D headPt, Point2D tailPt, String name, int defAngle)
	{
        // if points are null, create them as would newInstance
		EPoint headP;
        if (headPt == null)
        {
            Rectangle2D headBounds = head.getBounds();
            headP = new EPoint(headBounds.getCenterX(), headBounds.getCenterY());
        } else
		{
			headP = EPoint.snap(headPt);
		}
		EPoint tailP;
        if (tailPt == null)
        {
            Rectangle2D tailBounds = tail.getBounds();
            tailP = new EPoint(tailBounds.getCenterX(), tailBounds.getCenterY());
        } else
		{
			tailP = EPoint.snap(tailPt);
		}

		// make sure points are valid
        Cell parent = head.getNodeInst().getParent();
        Poly headPoly = head.getPoly();
        if (!stillInPoly(headP, headPoly)) {
			System.out.println("Error in cell " + parent.describe() + ": head of " + type.getName() +
				" arc at (" + headP.getX() + "," + headP.getY() + ") does not fit in port " +
				head.describe() + " which is centered at (" + headPoly.getCenterX() + "," + headPoly.getCenterY() + ")");
			return null;
		}
        Poly tailPoly = tail.getPoly();
		if (!stillInPoly(tailP, tailPoly))
		{
			System.out.println("Error in cell " + parent.describe() + ": tail of " + type.getName() +
				" arc at (" + tailP.getX() + "," + tailP.getY() + ") does not fit in port " +
				tail.describe() + " which is centered at (" + tailPoly.getCenterX() + "," + tailPoly.getCenterY() + ")");
			return null;
		}
        
        defAngle = defAngle % 3600;
        if (defAngle < 0) defAngle += 3600;
        
        return newInstance(parent, type, name, -1, null, head, tail, headP, tailP, width, defAngle << AANGLESH);
	}

	/**
	 * Method to create a new ArcInst connecting two PortInsts at specified locations.
	 * This is more general than the version that does not take coordinates.
     * @param parent the parent Cell of this ArcInst
	 * @param protoType the ArcProto of this ArcInst.
	 * @param name the name of this ArcInst
	 * @param duplicate duplicate index of this ArcInst
     * @param nameDescriptor text descriptor of name of this ArcInst
	 * @param headPort the head end PortInst.
	 * @param tailPort the tail end PortInst.
	 * @param headPt the coordinate of the head end PortInst.
	 * @param tailPt the coordinate of the tail end PortInst.
	 * @param width the width of this ArcInst.
     * @param userBits flag bits.
     * @return the newly created ArcInst, or null if there is an error.
	 */
	public static ArcInst newInstance(Cell parent, ArcProto protoType, String name, int duplicate, ImmutableTextDescriptor nameDescriptor,
        PortInst headPort, PortInst tailPort, EPoint headPt, EPoint tailPt, double width, int userBits)
	{
		// make sure fields are valid
		if (protoType == null || headPort == null || tailPort == null || !headPort.isLinked() || !tailPort.isLinked()) return null;
        if (headPt == null || tailPt == null) return null;

        if (parent != headPort.getNodeInst().getParent() || parent != tailPort.getNodeInst().getParent())
		{
			System.out.println("ArcProto.newInst: the 2 PortInsts are in different Cells!");
			return null;
		}

        // make sure the arc can connect to these ports
        PortProto headProto = headPort.getPortProto();
		PrimitivePort headPrimPort = headProto.getBasePort();
		if (!headPrimPort.connectsTo(protoType))
		{
			System.out.println("Cannot create " + protoType.describe() + " arc in cell " + parent.describe() +
				" because it cannot connect to port " + headProto.getName());
			return null;
		}
		PortProto tailProto = tailPort.getPortProto();
		PrimitivePort tailPrimPort = tailProto.getBasePort();
		if (!tailPrimPort.connectsTo(protoType))
		{
			System.out.println("Cannot create " + protoType.describe() + " arc in cell " + parent.describe() +
				" because it cannot connect to port " + tailProto.getName());
			return null;
		}
        
        ArcInst ai = new ArcInst(parent, protoType, name, duplicate, nameDescriptor, headPort, tailPort, headPt, tailPt, width, userBits);
        if (ai == null) return null;
		ai.lowLevelLink();

		// handle change control, constraint, and broadcast
		Undo.newObject(ai);
		return ai;
	}

	/**
	 * Method to delete this ArcInst.
	 */
	public void kill()
	{
		if (!isLinked())
		{
			System.out.println("ArcInst already killed");
			return;
		}
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
		double oldxA = headLocation.getX();
		double oldyA = headLocation.getY();
		double oldxB = tailLocation.getX();
		double oldyB = tailLocation.getY();
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
		if (!headPortInst.getPortProto().connectsTo(ap) || !tailPortInst.getPortProto().connectsTo(ap))
		{
			System.out.println("Cannot replace arc " + describe() + " with one of type " + ap.getName() +
				" because the nodes cannot connect to it");
			return null;
		}

		// compute the new width
		double newwid = getWidth() - getProto().getWidthOffset() + ap.getWidthOffset();

		// first create the new nodeinst in place
		ArcInst newar = ArcInst.newInstance(ap, newwid, headPortInst, tailPortInst, headLocation, tailLocation, null, 0);
		if (newar == null)
		{
			System.out.println("Cannot replace arc " + describe() + " with one of type " + ap.getName() +
				" because the new arc failed to create");
			return null;
		}

		// copy all variables on the arcinst
		newar.copyPropertiesFrom(this);

		// now delete the original nodeinst
		kill();
		newar.setName(getName());
		return newar;
	}

	/****************************** LOW-LEVEL IMPLEMENTATION ******************************/

	/**
	 * Low-level method to link the ArcInst into its Cell.
	 */
	public void lowLevelLink()
	{
		if (!isUsernamed() && (name == null || !parent.isUniqueName(name, getClass(), this)) || checkNameKey(name, parent, false))
		{
			name = parent.getAutoname(BASENAME);
			duplicate = 0;
		}

		// attach this arc to the two nodes it connects
		headPortInst.getNodeInst().addConnection(headEnd);
		tailPortInst.getNodeInst().addConnection(tailEnd);

//		// add this arc to the cell
//		this.duplicate = parent.addArc(this);
//		parent.linkArc(this);

		// update end shrinkage information
		updateShrinkage(headPortInst.getNodeInst());
		updateShrinkage(tailPortInst.getNodeInst());		
		
		// fill in the geometry
		updateGeometric();

		// add this arc to the cell
		this.duplicate = parent.addArc(this);
		parent.linkArc(this);
	}

	/**
	 * Low-level method to unlink the ArcInst from its Cell.
	 */
	public void lowLevelUnlink()
	{
		// remove this arc from the two nodes it connects
		headPortInst.getNodeInst().removeConnection(headEnd);
		tailPortInst.getNodeInst().removeConnection(tailEnd);

		// remove this arc from the cell
		parent.removeArc(this);
		parent.unLinkArc(this);

		// update end shrinkage information
		updateShrinkage(headPortInst.getNodeInst());
		updateShrinkage(tailPortInst.getNodeInst());
		parent.checkInvariants();
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
		parent.unLinkArc(this);

		// now make the change
		width = DBMath.round(width + dWidth);

		if (dHeadX != 0 || dHeadY != 0)
			headLocation = new EPoint(headLocation.getX() + dHeadX, headLocation.getY() + dHeadY);
		if (dTailX != 0 || dTailY != 0)
			tailLocation = new EPoint(tailLocation.getX() + dTailX, tailLocation.getY() + dTailY);
		updateGeometric();

		// update end shrinkage information
		updateShrinkage(headPortInst.getNodeInst());
		updateShrinkage(tailPortInst.getNodeInst());

		// reinsert in the R-Tree structure
		parent.linkArc(this);
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
	public int getAngle() { return (userBits & AANGLE) >> AANGLESH; }

	/**
	 * Method to set the rotation angle of this ArcInst.
	 * @param angle the rotation angle of this ArcInst (in tenth-degrees).
	 * In general, you should not call this method because the
	 * constructors and modification methods update this correctly.
	 * If, however, you have a zero-length arc and want to explicitly set
	 * its angle, then use this method.
	 */
	public void setAngle(int angle) {
        checkChanging();
        if (!tailLocation.equals(headLocation)) return;
        lowLevelSetAngle(angle);
        Undo.otherChange(this);        
    }

    private void lowLevelSetAngle(int angle) {
        angle = angle % 3600;
        if (angle < 0) angle += 3600;
        assert 0 <= angle && angle < 3600;
        userBits = (userBits & ~AANGLE) | (angle << AANGLESH);
    }
    
	/**
	 * Method to return the bounds of this ArcInst.
	 * TODO: dangerous to give a pointer to our internal field; should make a copy of visBounds
	 * @return the bounds of this ArcInst.
	 */
	public Rectangle2D getBounds() { return visBounds; }

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

		// zero-width polygons are simply lines
		if (width == 0)
		{
			Poly poly = new Poly(new Point2D.Double[]{headLocation.mutable(), tailLocation.mutable()});
			if (style == Poly.Type.FILLED) style = Poly.Type.OPENED;
			poly.setStyle(style);
			return poly;
		}

		// determine the end extension on each end
		double extendH = 0;
		if (isHeadExtended())
		{
			extendH = width/2;
			if (headShrink != 0)
				extendH = getExtendFactor(width, headShrink);
		}
		double extendT = 0;
		if (isTailExtended())
		{
			extendT = width/2;
			if (tailShrink != 0)
				extendT = getExtendFactor(width, tailShrink);
		}

		// make the polygon
		Poly poly = Poly.makeEndPointPoly(length, width, getAngle(), headLocation, extendH, tailLocation, extendT);
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
		double length = tailLocation.distance(headLocation);

		// see if the radius can work with these arc ends
		if (pureRadius*2 < length) return null;

		// determine the center of the circle
		Point2D [] centers = DBMath.findCenters(pureRadius, tailLocation, headLocation, length);
		if (centers == null) return null;

		Point2D centerPt = centers[1];
		if (radius < 0)
		{
			radius = -radius;
			centerPt = centers[0];
		}

		// determine the base and range of angles
		int angleBase = DBMath.figureAngle(centerPt, tailLocation);
		int angleRange = DBMath.figureAngle(centerPt, headLocation);
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
	public static double getExtendFactor(double width, int extend)
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
	private static void updateShrinkage(NodeInst ni)
	{
		//		ni.clearShortened();
		for(Iterator it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			byte shrink = checkShortening(ni, con.getPortInst().getPortProto());
			//			if (shrink != 0) ni.setShortened();
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
	private static byte checkShortening(NodeInst ni, PortProto pp)
	{
		// quit now if we don't have to worry about this kind of nodeinst
		NodeProto np = ni.getProto();
		if (!(np instanceof PrimitiveNode)) return 0;
		PrimitiveNode pn = (PrimitiveNode)np;
		if (!pn.canShrink() && !pn.isArcsShrink()) return 0;

		// gather the angles of the nodes/arcs
		int total = 0, off90 = 0;
		for(Iterator it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			ArcInst ai = con.getArc();

			// ignore zero-size arcs
			if (ai.getWidth() == 0) continue;

			// ignore this arcinst if it is not on the desired port
			if (!pn.isArcsShrink() && con.getPortInst().getPortProto() != pp) continue;

			// compute the angle
			int ang = ai.getAngle() / 10;
			if (con.getEndIndex() == ArcInst.HEADEND) ang += 180;
			ang %= 360;
			if ((ang%90) != 0) off90++;
			if (total < MAXANGLES) shortAngles[total++] = ang; else
				break;
		}

		// throw in the nodeinst rotation factor if it is important
		if (pn.canShrink())
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
		return (byte)ang;
	}

	/**
	 * Method to recompute the Geometric information on this ArcInst.
	 */
	private void updateGeometric()
	{
		checkChanging();
		if (tailLocation.equals(headLocation))
		{
			length = 0;
		} else
		{
			length = tailLocation.distance(headLocation);
			lowLevelSetAngle(DBMath.figureAngle(tailLocation, headLocation));
		}

		// compute the bounds
		Poly poly = makePoly(length, width, Poly.Type.FILLED);
		visBounds.setRect(poly.getBounds2D());

		// the cell must recompute its bounds
		if (parent != null) parent.setDirty();
	}

	/****************************** CONNECTIONS ******************************/

	/**
	 * Method to return the Connection on the tail end of this ArcInst.
	 * @return the Connection on the tail end of this ArcInst.
	 */
	public TailConnection getTail() { return tailEnd; }

	/**
	 * Method to return the Connection on the head end of this ArcInst.
	 * @return the Connection on the head end of this ArcInst.
	 */
	public HeadConnection getHead() { return headEnd; }

	/**
	 * Method to return the connection at an end of this ArcInst.
	 * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
	 */
	public Connection getConnection(int connIndex)
	{
		switch (connIndex)
		{
			case TAILEND: return tailEnd;
			case HEADEND: return headEnd;
			default: throw new IllegalArgumentException("Bad end " + connIndex);
		}
	}

	/**
	 * Method to return the PortInst on tail of this ArcInst.
	 * @return the PortInst on tail.
	 */
	public PortInst getTailPortInst() { return tailPortInst; }

	/**
	 * Method to return the PortInst on head of this ArcInst.
	 * @return the PortInst on head.
	 */
	public PortInst getHeadPortInst() { return headPortInst; }

	/**
	 * Method to return the PortInst on an end of this ArcInst.
	 * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
	 * @return the PortInst at an end.
	 */
	public PortInst getPortInst(int connIndex)
	{
		switch (connIndex)
		{
			case TAILEND: return tailPortInst;
			case HEADEND: return headPortInst;
			default: throw new IllegalArgumentException("Bad end " + connIndex);
		}
	}

	/**
	 * Method to return the Location on tail of this ArcInst.
	 * @return the Location on tail.
	 */
	public EPoint getTailLocation() { return tailLocation; }

	/**
	 * Method to return the Location on head of this ArcInst.
	 * @return the Location on head.
	 */
	public EPoint getHeadLocation() { return headLocation; }

	/**
	 * Method to return the Location on an end of this ArcInst.
	 * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
	 * @return the Location on an end.
	 */
	public EPoint getLocation(int connIndex)
	{
		switch (connIndex)
		{
			case TAILEND: return tailLocation;
			case HEADEND: return headLocation;
			default: throw new IllegalArgumentException("Bad end " + connIndex);
		}
	}

	/**
	 * Method to tell whether a tail connection on this ArcInst contains a port location.
	 * @param pt the point in question.
	 * @param reduceForArc if true reduce width by width offset of it proto.
	 * @return true if the point is inside of the port.
	 */
	public boolean tailStillInPort(Point2D pt, boolean reduceForArc)
	{
		return stillInPort(TAILEND, pt, reduceForArc);
	}

	/**
	 * Method to tell whether a head connection on this ArcInst contains a port location.
	 * @param pt the point in question.
	 * @param reduceForArc if true reduce width by width offset of it proto.
	 * @return true if the point is inside of the port.
	 */
	public boolean headStillInPort(Point2D pt, boolean reduceForArc)
	{
		return stillInPort(HEADEND, pt, reduceForArc);
	}

	/**
	 * Method to tell whether a connection on this ArcInst contains a port location.
	 * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
	 * @param pt the point in question.
	 * @param reduceForArc if true reduce width by width offset of it proto.
	 * @return true if the point is inside of the port.
	 */
	public boolean stillInPort(int connIndex, Point2D pt, boolean reduceForArc)
	{
		// determine the area of the nodeinst
		PortInst pi = getPortInst(connIndex);
		Poly poly = pi.getPoly();
		if (reduceForArc)
		{
			double wid = getWidth() - getProto().getWidthOffset();
			poly.reducePortPoly(pi, wid, getAngle());
		}
        return stillInPoly(pt, poly);
//		if (poly.isInside(pt)) return true;
//		if (poly.polyDistance(pt.getX(), pt.getY()) < MINPORTDISTANCE) return true;
//
//		// no good
//		return false;
	}
    
    static private boolean stillInPoly(Point2D pt, Poly poly) { return poly.isInside(pt) || poly.polyDistance(pt.getX(), pt.getY()) < MINPORTDISTANCE; }
    
    /****************************** TEXT ******************************/

	/**
	 * Method to return the name key of this ArcInst.
	 * @return the name key of this ArcInst, null if there is no name.
	 */
	public Name getNameKey()
	{
		return name;
	}

	/**
	 * Low-level access method to change name of this ArcInst.
	 * @param name new name of this ArcInst.
	 * @param duplicate new duplicate number of this ArcInst or negative value.
	 */
	public void lowLevelRename(Name name, int duplicate)
	{
		parent.removeArc(this);
		this.name = name;
		this.duplicate = duplicate;
		this.duplicate = parent.addArc(this);
		parent.checkInvariants();
	}

	/**
	 * Method to return the duplicate index of this ArcInst.
	 * @return the duplicate index of this ArcInst.
	 */
	public int getDuplicate()
	{
		return duplicate;
	}

	/**
	 * Returns the TextDescriptor on this ArcInst selected by name.
	 * This name may be a name of variable on this ArcInst or
	 * the special name <code>ArcInst.ARC_NAME_TD</code>.
	 * Other strings are not considered special, even they are equal to the
	 * special name. In other words, special name is compared by "==" other than
	 * by "equals".
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param varName name of variable or special name.
	 * @return the TextDescriptor on this ArcInst.
	 */
	public ImmutableTextDescriptor getTextDescriptor(String varName)
	{
		if (varName == ARC_NAME_TD) return nameDescriptor;
		return super.getTextDescriptor(varName);
	}

	/**
	 * Updates the TextDescriptor on this ArcInst selected by varName.
	 * The varName may be a name of variable on this ArcInst or
	 * the special name <code>ArcInst.EXPORT_NAME_TD</codeOC>.
	 * If varName doesn't select any text descriptor, no action is performed.
	 * Other strings are not considered special, even they are equal to the
	 * special name. In other words, special name is compared by "==" other than
	 * by "equals".
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param varName name of variable or special name.
	 * @param td new value TextDescriptor
     * @return old text descriptor
     * @throws IllegalArgumentException if TextDescriptor with specified name not found on this ArcInst.
	 */
	public ImmutableTextDescriptor lowLevelSetTextDescriptor(String varName, ImmutableTextDescriptor td)
	{
		if (varName == ARC_NAME_TD)
        {
            ImmutableTextDescriptor oldDescriptor = nameDescriptor;
			nameDescriptor = td.withDisplayWithoutParamAndCode();
            return oldDescriptor;
        }
		return super.lowLevelSetTextDescriptor(varName, td);
	}

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
     * Compares ArcInsts by their Cells and names.
     * @param obj the other ArcInst.
     * @return a comparison between the ArcInsts.
     */
	public int compareTo(Object obj)
	{
		ArcInst that = (ArcInst)obj;
		int cmp;
		if (this.parent != that.parent)
		{
			cmp = this.parent.compareTo(that.parent);
			if (cmp != 0) return cmp;
		}
		cmp = this.getName().compareTo(that.getName());
		if (cmp != 0) return cmp;
		return this.duplicate - that.duplicate;
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

	/****************************** CONSTRAINTS ******************************/

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
        Undo.otherChange(this);
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
        Undo.otherChange(this);
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
        Undo.otherChange(this);
    }

	/**
	 * Method to tell whether this ArcInst is slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
	 * @return true if this ArcInst is slidable.
	 */
	public boolean isSlidable() { return (userBits & CANTSLIDE) == 0; }

	/****************************** PROPERTIES ******************************/

	/**
	 * Method to determine whether this ArcInst is directional, with an arrow on one end.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
	 * @return true if that end has a directional arrow on it.
     */
	public boolean isArrowed(int connIndex)
	{
		switch (connIndex)
		{
			case TAILEND: return isTailArrowed();
			case HEADEND: return isHeadArrowed();
			default: throw new IllegalArgumentException("Bad end " + connIndex);
		}
	}

	/**
	 * Method to determine whether this ArcInst is directional, with an arrow on the tail.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @return true if the arc's tail has a directional arrow on it.
     */
	public boolean isTailArrowed()
	{
		return (userBits & TAILARROW) != 0;
	}

	/**
	 * Method to determine whether this ArcInst is directional, with an arrow on the head.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @return true if the arc's head has a directional arrow on it.
     */
	public boolean isHeadArrowed()
	{
		return (userBits & HEADARROW) != 0;
	}

	/**
	 * Method to determine whether this ArcInst is directional, with an arrow line drawn down the center.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * The body is typically drawn when one of the ends has an arrow on it, but it may be
	 * drawin without an arrow head in order to continue an attached arc that has an arrow.
	 * @return true if the arc's tail has an arrow line on it.
     */
	public boolean isBodyArrowed()
	{
		return (userBits & BODYARROW) != 0;
	}
	
	/**
	 * Method to set this ArcInst to be directional, with an arrow on one end.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
     * @param state true to show a directional arrow on the specified end.
     */
	public void setArrowed(int connIndex, boolean state)
	{
		switch (connIndex)
		{
			case TAILEND: setTailArrowed(state); break;
			case HEADEND: setHeadArrowed(state); break;
			default: throw new IllegalArgumentException("Bad end " + connIndex);
		}
	}

	/**
	 * Method to set this ArcInst to be directional, with an arrow on the tail.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
     * @param state true to show a directional arrow on the tail.
     */
	public void setTailArrowed(boolean state)
	{
        checkChanging();
        if (state) userBits |= TAILARROW; else
            userBits &= ~TAILARROW;
        Undo.otherChange(this);
    }

	/**
	 * Method to set this ArcInst to be directional, with an arrow on the head.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
     * @param state true to show a directional arrow on the head.
     */
	public void setHeadArrowed(boolean state)
	{
        checkChanging();
        if (state) userBits |= HEADARROW; else
            userBits &= ~HEADARROW;
        Undo.otherChange(this);
	}

	/**
	 * Method to set this ArcInst to be directional, with an arrow line drawn down the center.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * The body is typically drawn when one of the ends has an arrow on it, but it may be
	 * drawin without an arrow head in order to continue an attached arc that has an arrow.
     * @param state true to show a directional line on this arc.
     */
	public void setBodyArrowed(boolean state)
	{
        checkChanging();
        if (state) userBits |= BODYARROW; else
            userBits &= ~BODYARROW;
        Undo.otherChange(this);
	}

	/**
	 * Method to tell whether an end of ArcInst has its ends extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
	 * @return true if that end of this ArcInst iss extended.
	 */
	public boolean isExtended(int connIndex)
	{
		switch (connIndex)
		{
			case TAILEND: return isTailExtended();
			case HEADEND: return isHeadExtended();
			default: throw new IllegalArgumentException("Bad end " + connIndex);
		}
	}

	/**
	 * Method to tell whether the tail of this arc is extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @return true if the tail of this arc is extended.
	 */
	public boolean isTailExtended()
	{
		return (userBits & TAILNOEXTEND) == 0;
	}

	/**
	 * Method to tell whether the head of this arc is extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @return true if the head of this arc is extended.
	 */
	public boolean isHeadExtended()
	{
		return (userBits & HEADNOEXTEND) == 0;
	}

	/**
	 * Method to set whether an end of this arc is extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
	 * @param e true to set that end of this arc to be extended.
	 */
	public void setExtended(int connIndex, boolean e)
	{
		switch (connIndex)
		{
			case TAILEND: setTailExtended(e); break;
			case HEADEND: setHeadExtended(e); break;
			default: throw new IllegalArgumentException("Bad end " + connIndex);
		}
	}

	/**
	 * Method to set whether the tail of this arc is extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @param e true to set the tail of this arc to be extended.
	 */
	public void setTailExtended(boolean e)
	{
        checkChanging();
		if (e) userBits &= ~TAILNOEXTEND; else
			userBits |= TAILNOEXTEND;
		if (isLinked()) updateGeometric();
        Undo.otherChange(this);         
	}

	/**
	 * Method to set whether the head of this arc is extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @param e true to set the head of this arc to be extended.
	 */
	public void setHeadExtended(boolean e)
	{
        checkChanging();
		if (e) userBits &= ~HEADNOEXTEND; else
			userBits |= HEADNOEXTEND;
		if (isLinked()) updateGeometric();
        Undo.otherChange(this);
	}

	/**
	 * Method to tell whether an end of this arc is negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
	 * @return true if set that end of this arc is negated.
	 */
	public boolean isNegated(int connIndex)
	{
		switch (connIndex)
		{
			case TAILEND: return isTailNegated();
			case HEADEND: return isHeadNegated();
			default: throw new IllegalArgumentException("Bad end " + connIndex);
		}
	}

	/**
	 * Method to tell whether the tail of this arc is negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 * @return true if set the tail of this arc is negated.
	 */
	public boolean isTailNegated()
	{
		return (userBits & ISTAILNEGATED) != 0;
	}

	/**
	 * Method to tell whether the head of this arc is negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 * @return true if set the head of this arc is negated.
	 */
	public boolean isHeadNegated()
	{
		return (userBits & ISHEADNEGATED) != 0;
	}

	/**
	 * Method to set whether an end of this arc is negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
	 * @param n true to set that end of this arc to be negated.
	 */
	public void setNegated(int connIndex, boolean n)
	{
		switch (connIndex)
		{
			case TAILEND: setTailNegated(n); break;
			case HEADEND: setHeadNegated(n); break;
			default: throw new IllegalArgumentException("Bad end " + connIndex);
		}
	}

	/**
	 * Method to set whether the tail of this arc is negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 * @param n true to set the tail of this arc to be negated.
	 */
	public void setTailNegated(boolean n)
	{
        checkChanging();
		if (n)
		{
			// only allow if negation is supported on this port
//			PortProto pp = tailPortInst.getPortProto();
//			if (pp instanceof PrimitivePort && ((PrimitivePort)pp).isNegatable())
				userBits |= ISTAILNEGATED;
		} else
		{
			userBits &= ~ISTAILNEGATED;
		}
        Undo.otherChange(this);
	}

	/**
	 * Method to set whether the head of this arc is negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 * @param n true to set the head of this arc to be negated.
	 */
	public void setHeadNegated(boolean n)
	{
        checkChanging();
		if (n)
		{
			// only allow if negation is supported on this port
// 			PortProto pp = headPortInst.getPortProto();
// 			if (pp instanceof PrimitivePort && ((PrimitivePort)pp).isNegatable())
				userBits |= ISHEADNEGATED;
		} else
		{
			userBits &= ~ISHEADNEGATED;
		}
        Undo.otherChange(this);
	}

	/**
	 * Method to convert ELIB userbits to database "userbits".
	 * The "userbits" are a set of bits that describes constraints and other properties,
	 * and are stored in ELIB files.
	 * The negation, directionality, and end-extension must be converted.
	 * @param bits the disk userbits.
     * @return the database userbits
	 */
	public static int fromElibBits(int bits)
	{
        int newBits = bits & COMMON_BITS;
		if ((bits&ISTAILNEGATED) != 0)
		{
			newBits |= (bits&DISK_REVERSEEND) == 0 ? ISTAILNEGATED : ISHEADNEGATED;
		}
		if ((bits&ISHEADNEGATED) != 0)
		{
            newBits |= (bits&DISK_REVERSEEND) == 0 ? ISHEADNEGATED : ISTAILNEGATED;
		}

		if ((bits&DISK_NOEXTEND) != 0)
		{
			if ((bits&DISK_NOTEND0) == 0) newBits |= TAILNOEXTEND;
			if ((bits&DISK_NOTEND1) == 0) newBits |= HEADNOEXTEND;
		}

		if ((bits&DISK_ISDIRECTIONAL) != 0)
		{
            newBits |= BODYARROW;
			if ((bits&DISK_REVERSEEND) == 0)
			{
				if ((bits&DISK_NOTEND1) == 0) newBits |= HEADARROW;
			} else
			{
				if ((bits&DISK_NOTEND0) == 0) newBits |= TAILARROW;
			}
		}
        int angle = (bits & DISK_AANGLE) >> DISK_AANGLESH;
        angle = (angle % 360)*10;
        newBits |= angle << AANGLESH;
        
        return newBits;
	}

	/**
	 * Method to apply a set of "userbits" to an arc.
	 * The "userbits" are a set of bits that describes constraints and other properties,
	 * and are stored in ELIB files.
	 * The negation, directionality, and end-extension must be converted.
	 * @param ai the ArcInst to modify.
	 * @param bits the userbits to apply to the arc.
	 */
//	public static void applyELIBArcBits(ArcInst ai, int bits)
//	{
//		ai.lowLevelSetUserbits(bits & ARC_BITS);
//		if ((bits&ELIBConstants.ISNEGATED) != 0)
//		{
//			if ((bits&ELIBConstants.REVERSEEND) != 0) ai.setHeadNegated(true); else
//				ai.setTailNegated(true);
//		}
//		if ((bits&ELIBConstants.ISHEADNEGATED) != 0)
//		{
//			if ((bits&ELIBConstants.REVERSEEND) != 0) ai.setTailNegated(true); else
//				ai.setHeadNegated(true);
//		}
//
//		if ((bits&ELIBConstants.NOEXTEND) != 0)
//		{
//			ai.setHeadExtended(false);
//			ai.setTailExtended(false);
//			if ((bits&ELIBConstants.NOTEND0) != 0) ai.setTailExtended(true);
//			if ((bits&ELIBConstants.NOTEND1) != 0) ai.setHeadExtended(true);
//		} else
//		{
//			ai.setHeadExtended(true);
//			ai.setTailExtended(true);
//		}
//
//		ai.setBodyArrowed(false);
//		ai.setTailArrowed(false);
//		ai.setHeadArrowed(false);
//		if ((bits&ELIBConstants.ISDIRECTIONAL) != 0)
//		{
//			ai.setBodyArrowed(true);
//			if ((bits&ELIBConstants.REVERSEEND) != 0)
//			{
//				if ((bits&ELIBConstants.NOTEND0) == 0) ai.setTailArrowed(true);
//			} else
//			{
//				if ((bits&ELIBConstants.NOTEND1) == 0) ai.setHeadArrowed(true);
//			}
//		}
//	}
    
	/**
	 * Method to compute the "userbits" to use for a given ArcInst.
	 * The "userbits" are a set of bits that describes constraints and other properties,
	 * and are stored in ELIB files.
	 * The negation, directionality, and end-extension must be converted.
	 * @param ai the ArcInst to analyze.
	 * @return the "userbits" for that ArcInst.
	 */
	public int makeELIBArcBits()
	{
		int diskBits = userBits & COMMON_BITS;
	
		// adjust bits for extension
		if (!isHeadExtended() || !isTailExtended())
		{
			diskBits |= DISK_NOEXTEND;
			if (isHeadExtended() != isTailExtended())
			{
				if (isTailExtended()) diskBits |= DISK_NOTEND0;
				if (isHeadExtended()) diskBits |= DISK_NOTEND1;
			}
		}
	
		// adjust bits for directionality
		if (isHeadArrowed() || isTailArrowed() || isBodyArrowed())
		{
			diskBits |= DISK_ISDIRECTIONAL;
			if (isTailArrowed()) diskBits |= DISK_REVERSEEND;
			if (!isHeadArrowed() && !isTailArrowed()) diskBits |= DISK_NOTEND1;
		}

		// adjust bits for negation
        boolean normalEnd = (diskBits & DISK_REVERSEEND) == 0;
		if (isTailNegated()) diskBits |= (normalEnd ? ISTAILNEGATED : ISHEADNEGATED);
		if (isHeadNegated()) diskBits |= (normalEnd ? ISHEADNEGATED : ISTAILNEGATED);
        
		//        int angle = getAngle() / 10;
		int angle = (int)(getAngle()/10.0 + 0.5);
		if (angle >= 360) angle -= 360;
        diskBits |= angle << DISK_AANGLESH;
        
        return diskBits;
	}

	/****************************** MISCELLANEOUS ******************************/

	/**
	 * Method to check and repair data structure errors in this ArcInst.
	 */
	public int checkAndRepair(boolean repair, ErrorLogger errorLogger)
	{
		int errorCount = 0;

		// see if the ends are in their ports
		if (!headStillInPort(headLocation, false))
		{
			Poly poly = headPortInst.getPoly();
			String msg = "Cell " + parent.describe() + ", arc " + describe() +
				": head not in port, is at (" + headLocation.getX() + "," + headLocation.getY() +
				") distance to port is " + poly.polyDistance(headLocation.getX(), headLocation.getY()) +
				" port center is (" + poly.getCenterX() + "," + poly.getCenterY() + ")";
			System.out.println(msg);
			if (errorLogger != null)
			{
				ErrorLogger.MessageLog error = errorLogger.logError(msg, parent, 1);
				error.addGeom(this, true, parent, null);
				error.addGeom(headPortInst.getNodeInst(), true, parent, null);
			}
			if (repair)
			{
				headLocation = new EPoint(poly.getCenterX(), poly.getCenterY());
				updateGeometric();
			}
			errorCount++;
		}
		if (!tailStillInPort(tailLocation, false))
		{
			Poly poly = tailPortInst.getPoly();
			String msg = "Cell " + parent.describe() + ", arc " + describe() +
				": tail not in port, is at (" + tailLocation.getX() + "," + tailLocation.getY() +
				") distance to port is " + poly.polyDistance(tailLocation.getX(), tailLocation.getY()) +
				" port center is (" + poly.getCenterX() + "," + poly.getCenterY() + ")";
			System.out.println(msg);
			if (errorLogger != null)
			{
				ErrorLogger.MessageLog error = errorLogger.logError(msg, parent, 1);
				error.addGeom(this, true, parent, null);
				error.addGeom(tailPortInst.getNodeInst(), true, parent, null);
			}
			if (repair)
			{
				tailLocation = new EPoint(poly.getCenterX(), poly.getCenterY());
				updateGeometric();
			}
			errorCount++;
		}

		// make sure width is not negative
		if (getWidth() < 0)
		{
			String msg = "Cell " + parent.describe() + ", arc " + describe() +
				": has negative width (" + getWidth() + ")";
			System.out.println(msg);
			if (errorLogger != null)
			{
				ErrorLogger.MessageLog error = errorLogger.logError(msg, parent, 1);
				error.addGeom(this, true, parent, null);
			}
			if (repair)
			{
				checkChanging();
				width = DBMath.round(Math.abs(width));
			}
			errorCount++;
		}
		return errorCount;
	}

	/**
	 * Method to check invariants in this ArcInst.
	 * @exception AssertionError if invariants are not valid
	 */
	public void check()
	{
		assert name != null;
		assert duplicate >= 0;

		assert headEnd.getArc() == this;
		assert tailEnd.getArc() == this;
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
     * Returns true if this ArcInst is linked into database.
     * @return true if this ArcInst is linked into database.
     */
	public boolean isLinked()
	{
		try
		{
			return parent != null && parent.isLinked() && parent.getArc(arcIndex) == this;
		} catch (IndexOutOfBoundsException e)
		{
			return false;
		}
	}

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
        copyTextDescriptorFrom(fromAi, ArcInst.ARC_NAME_TD);
    }

    /**
     * Copies constraints (Rigid, Ends Extended, etc) from another arcinst to this arcinst
     * It copies also attributes of Connections (arrow/negated/extended)
     * @param fromAi the arcinst from which to copy constraints
     */
    public void copyConstraintsFrom(ArcInst fromAi) {
        checkChanging();
        if (fromAi == null) return;
        int newBits = tailLocation.equals(headLocation) ? fromAi.userBits : (fromAi.userBits & ~AANGLE) | (this.userBits & AANGLE);
        newBits &= DATABASE_BITS;
		boolean extensionChanged = (this.userBits&(TAILNOEXTEND|HEADNOEXTEND)) != (userBits&(TAILNOEXTEND|HEADNOEXTEND));
		if (isLinked() && extensionChanged) updateGeometric();
        Undo.otherChange(this);
//		setHeadNegated(fromAi.isHeadNegated());
//		setTailNegated(fromAi.isTailNegated());
    }

//	/**
//	 * Low-level method to get the user bits.
//	 * The "user bits" are a collection of flags that are more sensibly accessed
//	 * through special methods.
//	 * This general access to the bits is required because the ELIB
//	 * file format stores it as a full integer.
//	 * This should not normally be called by any other part of the system.
//	 * @return the "user bits".
//	 */
//    public int lowLevelGetUserbits() { return userBits; }
//
//	/**
//	 * Low-level method to set the user bits.
//	 * The "user bits" are a collection of flags that are more sensibly accessed
//	 * through special methods.
//	 * This general access to the bits is required because the ELIB
//	 * file format stores it as a full integer.
//	 * This should not normally be called by any other part of the system.
//	 * @param diskBits the new "user bits".
//	 */
// 	public void lowLevelSetUserbits(int userBits)
// 	{
// 		boolean extensionChanged = (this.userBits&(TAILNOEXTEND|HEADNOEXTEND)) != (userBits&(TAILNOEXTEND|HEADNOEXTEND));
// 		this.userBits = userBits & DATABASE_BITS;
// 		if (isLinked() && extensionChanged) updateGeometric();
// 	}

//	/**
//	 * Method to copy the various state bits from another ArcInst to this ArcInst.
//	 * @param ai the other ArcInst to copy.
//	 */
//	public void copyStateBits(ArcInst ai) { checkChanging(); this.userBits = ai.userBits; Undo.otherChange(this); }

	/**
	 * Method to set default constraint information on this ArcInst.
	 */
	private void setDefaultConstraints()
	{
        setRigid(protoType.isRigid());
        setFixedAngle(protoType.isFixedAngle());
        setSlidable(protoType.isSlidable());
        setHeadExtended(protoType.isExtended());
        setTailExtended(protoType.isExtended());
        setHeadArrowed(protoType.isDirectional());
        setBodyArrowed(protoType.isDirectional());
	}

	/**
	 * Method to set this ArcInst to be hard-to-select.
	 * Hard-to-select ArcInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
     * @param state
     */
	public void setHardSelect(boolean state) {
        checkChanging();
        if (state)
            userBits |= HARDSELECTA;
        else
            userBits &= ~HARDSELECTA;
        Undo.otherChange(this);
    }

	/**
	 * Method to tell whether this ArcInst is hard-to-select.
	 * Hard-to-select ArcInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 * @return true if this ArcInst is hard-to-select.
	 */
	public boolean isHardSelect() { return (userBits & HARDSELECTA) != 0; }

	/**
	 * Method to set a timestamp for constraint propagation on this ArcInst.
	 * This is used by the Layout constraint system.
	 * @param changeClock the timestamp for constraint propagation.
	 */
	public void setChangeClock(int changeClock) { this.changeClock = changeClock; }

	/**
	 * Method to get the timestamp for constraint propagation on this ArcInst.
	 * This is used by the Layout constraint system.
	 * @return the timestamp for constraint propagation on this ArcInst.
	 */
	public int getChangeClock() { return changeClock; }

	/**
	 * Method to set a Change object on this ArcInst.
	 * This is used during constraint propagation to tell whether this object has already been changed and by how much.
	 * @param change the Change object to be set on this ArcInst.
	 */
	public void setChange(Undo.Change change) { this.change = change; }

	/**
	 * Method to get the Change object on this ArcInst.
	 * This is used during constraint propagation to tell whether this object has already been changed and by how much.
	 * @return the Change object on this ArcInst.
	 */
	public Undo.Change getChange() { return change; }

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

	/**
	 * Method to crop given polygon against a connecting transistor. Function similar to Quick.cropActiveArc
	 * @param poly
	 * @return new polygon if was cropped otherwise the original
	 */
	public Poly cropPerLayer(Poly poly)
	{
		// must be manhattan
		Rectangle2D polyBounds = poly.getBox();
		if (polyBounds == null) return poly;
		polyBounds = new Rectangle2D.Double(polyBounds.getMinX(), polyBounds.getMinY(), polyBounds.getWidth(), polyBounds.getHeight());

		// search for adjoining transistor in the cell
		for(int i=0; i<2; i++)
		{
			PortInst pi = getPortInst(i);
			NodeInst ni = pi.getNodeInst();
			//if (!ni.isFET()) continue;

			// crop the arc against this transistor
			AffineTransform trans = ni.rotateOut();
			Technology tech = ni.getProto().getTechnology();
			Poly [] activeCropPolyList = tech.getShapeOfNode(ni);
			int nTot = activeCropPolyList.length;
			for(int k=0; k<nTot; k++)
			{
				Poly nPoly = activeCropPolyList[k];
				if (nPoly.getLayer() != poly.getLayer()) continue;
				nPoly.transform(trans);
				Rectangle2D nPolyBounds = nPoly.getBox();
				if (nPolyBounds == null) continue;
				int result = Poly.cropBoxComplete(polyBounds, nPolyBounds, true);
				if (result == 1)
				{
					// Empty polygon
					return null;
				}
				if (result == -2)
					System.out.println("When is this case?");
				Poly newPoly = new Poly(polyBounds);
				newPoly.setLayer(poly.getLayer());
				newPoly.setStyle(poly.getStyle());
				return newPoly;
			}
		}
		return poly;
	}

    /**
     * Method to determin if arc contains active diffusion
     * @return True if contains active diffusion
     */
    public boolean isDiffusionArc()
    {
        return getProto().getFunction().isDiffusion();
    }
}

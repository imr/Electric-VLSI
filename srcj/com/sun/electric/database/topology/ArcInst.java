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

import com.sun.electric.database.CellId;
import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableElectricObject;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.DisplayedText;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

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
public class ArcInst extends Geometric implements Comparable<ArcInst>
{
    /** empty array of ArcInsts. */                     public static final ArcInst[] NULL_ARRAY = {};
	/** The index of the tail of this ArcInst. */		public static final int TAILEND = 0;
	/** The index of the head of this ArcInst. */		public static final int HEADEND = 1;
    

	/** Key of the obsolete variable holding arc name.*/public static final Variable.Key ARC_NAME = Variable.newKey("ARC_name");
	/** Key of Varible holding arc curvature. */		public static final Variable.Key ARC_RADIUS = Variable.newKey("ARC_radius");

	/** Minimal distance of arc end to port polygon. */	static final double MINPORTDISTANCE = DBMath.getEpsilon()*0.71; // sqrt(0.5)

	// -------------------------- private data ----------------------------------

    /** persistent data of this ArcInst. */             ImmutableArcInst d;
	/** bounds after transformation. */					private final Rectangle2D visBounds = new Rectangle2D.Double();

	/** PortInst on tail end of this arc instance */	/*package*/final PortInst tailPortInst;
	/** tail connection of this arc instance */			private final TailConnection tailEnd;

	/** PortInst on head end of this arc instance */	/*package*/final PortInst headPortInst;
	/** head connection of this arc instance */			private final HeadConnection headEnd;

	/** 0-based index of this ArcInst in cell. */		private int arcIndex = -1;

	/**
	 * Private constructor of ArcInst.
     * @param parent the parent Cell of this ArcInst
     * @param d persistent data of ArcIInst
	 * @param headPort the head end PortInst.
	 * @param tailPort the tail end PortInst.
	 */
	public ArcInst(Cell parent, ImmutableArcInst d, PortInst headPort, PortInst tailPort)
	{
		super(parent);

        // initialize this object
		assert parent == headPort.getNodeInst().getParent();
        assert parent == tailPort.getNodeInst().getParent();
        assert d.headNodeId == headPort.getNodeInst().getD().nodeId;
        assert d.tailNodeId == tailPort.getNodeInst().getD().nodeId;
        assert d.headPortId == headPort.getPortProto().getId();
        assert d.tailPortId == tailPort.getPortProto().getId();
        
        this.d = d;

		// create node/arc connections and place them properly
		tailPortInst = tailPort;
		tailEnd = new TailConnection(this);

		headPortInst = headPort;
		headEnd = new HeadConnection(this);
	}

    private Object writeReplace() throws ObjectStreamException { return new ArcInstKey(this); }
    private Object readResolve() throws ObjectStreamException { throw new InvalidObjectException("ArcInst"); }
    
    private static class ArcInstKey extends EObjectInputStream.Key {
        Cell cell;
        int arcId;
        
        private ArcInstKey(ArcInst ai) {
            assert ai.isLinked();
            cell = ai.getParent();
            arcId = ai.getD().arcId;
        }
        
        protected Object readResolveInDatabase(EDatabase database) throws InvalidObjectException {
            ArcInst ai = cell.getArcById(arcId);
            if (ai == null) throw new InvalidObjectException("ArcInst");
            return ai;
        }
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
        return newInstance(type, width, head, tail, null, null, null, 0, type.getDefaultConstraints());
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
		return newInstance(type, width, head, tail, headPt, tailPt, name, 0, type.getDefaultConstraints());
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
			System.out.println("Cannot find pin for " + ap);
			return null;
		}

		// create the head node
		EPoint xPH = new EPoint(-arcLength/2, 0);
		NodeInst niH = NodeInst.makeDummyInstance(npEnd, xPH, npEnd.getDefWidth(), npEnd.getDefHeight(), Orientation.IDENT);
		PortInst piH = niH.getOnlyPortInst();

		// create the tail node
		EPoint xPT = new EPoint(arcLength/2, 0);
		NodeInst niT = NodeInst.makeDummyInstance(npEnd, xPT, npEnd.getDefWidth(), npEnd.getDefHeight(), Orientation.IDENT);
		PortInst piT = niT.getOnlyPortInst();

		// create the arc that connects them
        ImmutableArcInst d = ImmutableArcInst.newInstance(0, ap, ImmutableArcInst.BASENAME, TextDescriptor.getArcTextDescriptor(),
                niT.getD().nodeId, piT.getPortProto().getId(), xPT,
                niH.getD().nodeId, piH.getPortProto().getId(), xPH,
                ap.getDefaultWidth(), 0, 0);
 		ArcInst ai = new ArcInst(null, d, piH, piT);

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
		return newInstance(type, width, head, tail, null, null, null, 0, 0);
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
        return newInstance(type, width, head, tail, headPt, tailPt, name, defAngle, 0);
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
     * @param flags flags of thew new ArcInst
     * @return the newly created ArcInst, or null if there is an error.
	 */
	public static ArcInst newInstance(ArcProto type, double width, PortInst head, PortInst tail,
	                                  Point2D headPt, Point2D tailPt, String name, int defAngle, int flags)
	{
//        if (type.isNotUsed())
//        {
////            System.out.println("Cannot create arc instance of " + type + " because prototype is unused");
////            return null;
//        }
        // if points are null, create them as would newInstance
		EPoint headP;
        if (headPt == null)
        {
//            Rectangle2D headBounds = head.getBounds();
            headP = head.getCenter(); //new EPoint(headBounds.getCenterX(), headBounds.getCenterY());
        } else
		{
			headP = EPoint.snap(headPt);
		}
		EPoint tailP;
        if (tailPt == null)
        {
//            Rectangle2D tailBounds = tail.getBounds();
            tailP = tail.getCenter(); // new EPoint(tailBounds.getCenterX(), tailBounds.getCenterY());
        } else
		{
			tailP = EPoint.snap(tailPt);
		}

		// make sure points are valid
        Cell parent = head.getNodeInst().getParent();
        Poly headPoly = head.getPoly();
        if (!stillInPoly(headP, headPoly)) {
			System.out.println("Error in " + parent + ": head of " + type.getName() +
				" arc at (" + headP.getX() + "," + headP.getY() + ") does not fit in " +
				head + " which is centered at (" + headPoly.getCenterX() + "," + headPoly.getCenterY() + ")");
			return null;
		}
        Poly tailPoly = tail.getPoly();
		if (!stillInPoly(tailP, tailPoly))
		{
			System.out.println("Error in " + parent + ": tail of " + type.getName() +
				" arc at (" + tailP.getX() + "," + tailP.getY() + ") does not fit in " +
				tail + " which is centered at (" + tailPoly.getCenterX() + "," + tailPoly.getCenterY() + ")");
			return null;
		}
        
        return newInstance(parent, type, name, null, head, tail, headP, tailP, width, defAngle, flags);
	}

	/**
	 * Method to create a new ArcInst connecting two PortInsts at specified locations.
	 * This is more general than the version that does not take coordinates.
     * @param parent the parent Cell of this ArcInst
	 * @param protoType the ArcProto of this ArcInst.
	 * @param name the name of this ArcInst
     * @param nameDescriptor text descriptor of name of this ArcInst
	 * @param headPort the head end PortInst.
	 * @param tailPort the tail end PortInst.
	 * @param headPt the coordinate of the head end PortInst.
	 * @param tailPt the coordinate of the tail end PortInst.
	 * @param width the width of this ArcInst.
     * @param angle angle in tenth-degrees.
     * @param flags flag bits.
     * @return the newly created ArcInst, or null if there is an error.
	 */
	public static ArcInst newInstance(Cell parent, ArcProto protoType, String name, TextDescriptor nameDescriptor,
        PortInst headPort, PortInst tailPort, EPoint headPt, EPoint tailPt, double width, int angle, int flags)
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
			System.out.println("Cannot create " + protoType + " in " + parent +
				" because it cannot connect to port " + headProto.getName());
			return null;
		}
		PortProto tailProto = tailPort.getPortProto();
		PrimitivePort tailPrimPort = tailProto.getBasePort();
		if (!tailPrimPort.connectsTo(protoType))
		{
			System.out.println("Cannot create " + protoType + " in " + parent +
				" because it cannot connect to port " + tailProto.getName());
			return null;
		}

        if (nameDescriptor == null) nameDescriptor = TextDescriptor.getArcTextDescriptor();
        Name nameKey = name != null ? Name.findName(name) : null;
		if (nameKey == null || nameKey.isTempname() && (!parent.isUniqueName(nameKey, ArcInst.class, null)) || checkNameKey(nameKey, parent))
		{
            nameKey = parent.getArcAutoname();
		} else
		{
			// adjust the name descriptor for "smart" text placement
			TextDescriptor smartDescriptor = getSmartTextDescriptor(angle, width, nameDescriptor);
			if (smartDescriptor != null) nameDescriptor = smartDescriptor;
		}
		if (width < 0)
			width = protoType.getWidth();
       
        CellId parentId = (CellId)parent.getId();
        // search for spare arcId
        int arcId;
        do {
            arcId = parentId.newArcId();
        } while (parent.getArcById(arcId) != null);
        ImmutableArcInst d = ImmutableArcInst.newInstance(arcId, protoType, nameKey, nameDescriptor,
                tailPort.getNodeInst().getD().nodeId, tailProto.getId(), tailPt,
                headPort.getNodeInst().getD().nodeId, headProto.getId(), headPt,
                width, angle, flags);
        ArcInst ai = new ArcInst(parent, d, headPort, tailPort);
		ai.lowLevelLink();

		// handle change control, constraint, and broadcast
		Constraints.getCurrent().newObject(ai);
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
		Constraints.getCurrent().killObject(this);
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
        ImmutableArcInst oldD = d;

		// change the arc
        EPoint tail = d.tailLocation;
		if (dTailX != 0 || dTailY != 0)
			tail = new EPoint(tail.getX() + dTailX, tail.getY() + dTailY);
        EPoint head = d.headLocation;
        if (dHeadX != 0 || dHeadY != 0)
            head = new EPoint(head.getX() + dHeadX, head.getY() + dHeadY);
		lowLevelModify(d.withWidth(d.width + dWidth).withLocations(tail, head));

		// track the change
        Constraints.getCurrent().modifyArcInst(this, oldD);
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
			System.out.println("Cannot replace " + this + " with one of type " + ap.getName() +
				" because the nodes cannot connect to it");
			return null;
		}

		// compute the new width
		double newwid = getWidth() - getProto().getWidthOffset() + ap.getWidthOffset();

		// first create the new nodeinst in place
		ArcInst newar = ArcInst.newInstance(ap, newwid, headPortInst, tailPortInst, d.headLocation, d.tailLocation, null, 0);
		if (newar == null)
		{
			System.out.println("Cannot replace " + this + " with one of type " + ap.getName() +
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
     * Returns persistent data of this ArcInst.
     * @return persistent data of this ArcInst.
     */
    public ImmutableArcInst getD() { return d; }
    
    /**
     * Modifies persistend data of this ArcInst.
     * @param newD new persistent data.
     * @param notify true to notify Undo system.
     * @return true if persistent data was modified.
     */
    public boolean setD(ImmutableArcInst newD, boolean notify) {
        checkChanging();
        ImmutableArcInst oldD = d;
        if (newD == oldD) return false;
        d = newD;
        if (parent != null) {
            parent.setContentsModified();
            if (notify)
                Constraints.getCurrent().modifyArcInst(this, oldD);
        }
        return true;
    }
    
    public void setDInUndo(ImmutableArcInst newD) {
        checkUndoing();
        d = newD;
    }

    /**
     * Returns persistent data of this ElectricObject with Variables.
     * @return persistent data of this ElectricObject.
     */
    public ImmutableElectricObject getImmutable() { return d; }
    
    /**
     * Method to add a Variable on this ArcInst.
     * It may add repaired copy of this Variable in some cases.
     * @param var Variable to add.
     */
    public void addVar(Variable var) {
        if (setD(d.withVariable(var), true))
        {
            // check for side-effects of the change
            checkPossibleVariableEffects(var.getKey());
        }
    }
	
	/**
	 * Method to handle special case side-effects of setting variables on this NodeInst.
	 * Overrides the general method on ElectricObject.
	 * Currently it handles changes to the number-of-degrees on a circle node.
	 * @param key the Variable key that has changed on this NodeInst.
	 */
	public void checkPossibleVariableEffects(Variable.Key key)
	{
        if (key == ARC_RADIUS) {
			lowLevelModify(d);
        }
	}

	/**
	 * Method to delete a Variable from this ArcInst.
	 * @param key the key of the Variable to delete.
	 */
	public void delVar(Variable.Key key)
	{
        setD(d.withoutVariable(key), true);
	}
    
	/**
	 * Low-level method to link the ArcInst into its Cell.
	 */
	public void lowLevelLink()
	{
        assert getDatabase() != null;
        checkChanging();

		// attach this arc to the two nodes it connects
		headPortInst.getNodeInst().addConnection(headEnd);
		tailPortInst.getNodeInst().addConnection(tailEnd);

		// fill in the geometry
		updateGeometric();

		// add this arc to the cell
		parent.addArc(this);
		parent.linkArc(this);
	}

	/**
	 * Low-level method to unlink the ArcInst from its Cell.
	 */
	public void lowLevelUnlink()
	{
        checkChanging();
		// remove this arc from the two nodes it connects
		headPortInst.getNodeInst().removeConnection(headEnd);
		tailPortInst.getNodeInst().removeConnection(tailEnd);

		// remove this arc from the cell
		parent.removeArc(this);
		parent.unLinkArc(this);
	}

	/**
	 * Low-level method to change the width and end locations of this ArcInst.
     * New persistent data may differ from old one only by width and end locations 
	 * @param d the new persistent data of this ArcInst.
	 */
	public void lowLevelModify(ImmutableArcInst d)
	{
		// first remove from the R-Tree structure
		parent.unLinkArc(this);
        boolean renamed = this.d.name != d.name;
        if (renamed)
            parent.removeArc(this);

		// now make the change
        setD(d, false);
        if (renamed)
            parent.addArc(this);
		updateGeometric();

		// update end shrinkage information
		headPortInst.getNodeInst().updateShrinkage();
		tailPortInst.getNodeInst().updateShrinkage();

		// reinsert in the R-Tree structure
		parent.linkArc(this);
	}

	/****************************** GRAPHICS ******************************/

	/**
	 * Method to return the width of this ArcInst.
	 * @return the width of this ArcInst.
	 */
	public double getWidth() { return d.width; }

	/**
	 * Method to return the length of this ArcInst.
	 * @return the length of this ArcInst.
	 */
	public double getLength() { return d.length; }

	/**
	 * Method to return the rotation angle of this ArcInst.
	 * @return the rotation angle of this ArcInst (in tenth-degrees).
	 */
	public int getAngle() { return d.angle; }

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
        ImmutableArcInst oldD = d;
        lowLevelModify(d.withAngle(angle));
        if (parent != null) Constraints.getCurrent().modifyArcInst(this, oldD);
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
	 * @param width the width of the ArcInst.
	 * @param style the style of the ArcInst.
	 * @return a Poly that describes the ArcInst.
	 */
	public Poly makePoly(double width, Poly.Type style)
	{
		return makePolyForArc(this, d.length, width, d.headLocation, d.tailLocation, style);
	}
	
	/**
	 * Method to create a Poly object that describes an ArcInst.
	 * The ArcInst is described by its length, width, endpoints, and style.
	 * @param real the real ArcInst object needed because this is a static method).
	 * @param length the length of the ArcInst.
	 * @param width the width of the ArcInst.
	 * @param headPt the head of the ArcInst.
	 * @param tailPt the tail of the ArcInst.
	 * @param style the style of the ArcInst.
	 * @return a Poly that describes the ArcInst.
	 */
	public static Poly makePolyForArc(ArcInst real, double length, double width, EPoint headPt, EPoint tailPt, Poly.Type style)
	{
		if (real.getProto().isCurvable())
		{
			// get the radius information on the arc
			Double radiusDouble = real.getRadius();
			if (radiusDouble != null)
			{
				Poly curvedPoly = real.curvedArcOutline(style, width, radiusDouble.doubleValue());
				if (curvedPoly != null) return curvedPoly;
			}
		}

		// zero-width polygons are simply lines
		if (width == 0)
		{
			Poly poly = new Poly(new Point2D.Double[]{headPt.mutable(), tailPt.mutable()});
			if (style == Poly.Type.FILLED) style = Poly.Type.OPENED;
			poly.setStyle(style);
			return poly;
		}

		// determine the end extension on each end
		double extendH = 0;
		if (real.isHeadExtended())
		{
			extendH = width/2;
            byte headShrink = real.getHeadPortInst().getNodeInst().shrink;
			if (headShrink != 0)
				extendH = getExtendFactor(width, headShrink);
		}
		double extendT = 0;
		if (real.isTailExtended())
		{
			extendT = width/2;
            byte tailShrink = real.getTailPortInst().getNodeInst().shrink;
			if (tailShrink != 0)
				extendT = getExtendFactor(width, tailShrink);
		}

		// make the polygon
		Poly poly = Poly.makeEndPointPoly(length, width, real.getAngle(), headPt, extendH, tailPt, extendT, style);
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
	public Poly curvedArcOutline(Poly.Type style, double wid, double radius)
	{
		// get information about the curved arc
		double pureRadius = Math.abs(radius);

		// see if the radius can work with these arc ends
		if (pureRadius*2 < d.length) return null;

		// determine the center of the circle
		Point2D [] centers = DBMath.findCenters(pureRadius, d.headLocation, d.tailLocation, d.length);
		if (centers == null) return null;

		Point2D centerPt = centers[1];
		if (radius < 0)
		{
			centerPt = centers[0];
		}

		// determine the base and range of angles
		int angleBase = DBMath.figureAngle(centerPt, d.headLocation);
		int angleRange = DBMath.figureAngle(centerPt, d.tailLocation);
		angleRange -= angleBase;
		if (angleRange < 0) angleRange += 3600;

		// force the curvature to be the smaller part of a circle (used to determine this by the reverse-ends bit)
		if (angleRange > 1800)
		{
			angleBase = DBMath.figureAngle(centerPt, d.tailLocation);
			angleRange = DBMath.figureAngle(centerPt, d.headLocation);
			angleRange -= angleBase;
			if (angleRange < 0) angleRange += 3600;
		}

		// determine the number of intervals to use for the arc
		int pieces = angleRange;
		while (pieces > MAXARCPIECES) pieces /= 2;
		if (pieces == 0) return null;

		// initialize the polygon
		int points = (pieces+1) * 2;
		Point2D [] pointArray = new Point2D[points];

		// get the inner and outer radii of the arc
		double outerRadius = pureRadius + wid / 2;
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
	public Poly [] getAllText(boolean hardToSelect, EditWindow0 wnd)
	{
		int dispVars = numDisplayableVariables(false);
		int totalText = dispVars;
		if (totalText == 0) return null;
		Poly [] polys = new Poly[totalText];
		addDisplayableVariables(getBounds(), polys, 0, wnd, false);
		return polys;
	}

	/**
	 * Method to return the number of displayable Variables on this ArcInst.
	 * A displayable Variable is one that will be shown with its object.
	 * @return the number of displayable Variables on this ArcInst.
	 */
	public int numDisplayableVariables(boolean multipleStrings)
	{
		return super.numDisplayableVariables(multipleStrings) + (isUsernamed()?1:0);
	}

	/**
	 * Method to add all displayable Variables on this Electric object to an array of Poly objects.
	 * @param rect a rectangle describing the bounds of the object on which the Variables will be displayed.
	 * @param polys an array of Poly objects that will be filled with the displayable Variables.
	 * @param start the starting index in the array of Poly objects to fill with displayable Variables.
	 * @return the number of Variables that were added.
	 */
	public int addDisplayableVariables(Rectangle2D rect, Poly [] polys, int start, EditWindow0 wnd, boolean multipleStrings)
	{
		int numVars = 0;
		if (isUsernamed())
		{
			double cX = rect.getCenterX();
			double cY = rect.getCenterY();
			TextDescriptor td = d.nameDescriptor;
			double offX = td.getXOff();
			double offY = td.getYOff();
			TextDescriptor.Position pos = td.getPos();
			Poly.Type style = pos.getPolyType();

			Point2D [] pointList = null;
			if (style == Poly.Type.TEXTBOX)
			{
				pointList = Poly.makePoints(rect);
			} else
			{
				pointList = new Point2D.Double[1];
				pointList[0] = new Point2D.Double(cX+offX, cY+offY);
			}
			polys[start] = new Poly(pointList);
			polys[start].setStyle(style);
			polys[start].setString(getNameKey().toString());
			polys[start].setTextDescriptor(td);
			polys[start].setLayer(null);
			polys[start].setDisplayedText(new DisplayedText(this, ARC_NAME));
			numVars = 1;
		}
		return super.addDisplayableVariables(rect, polys, start+numVars, wnd, multipleStrings) + numVars;
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
	 * Method to recompute the Geometric information on this ArcInst.
	 */
	public void updateGeometric()
	{
		checkChanging();

		// compute the bounds
		Poly poly = makePoly(d.width, Poly.Type.FILLED);
		visBounds.setRect(poly.getBounds2D());

		// the cell must recompute its bounds
		if (parent != null) parent.setDirty();
	}
	
    public void updateGeometricInUndo() {
        checkUndoing();
		// compute the bounds
		Poly poly = makePoly(d.width, Poly.Type.FILLED);
		visBounds.setRect(poly.getBounds2D());
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
	public EPoint getTailLocation() { return d.tailLocation; }

	/**
	 * Method to return the Location on head of this ArcInst.
	 * @return the Location on head.
	 */
	public EPoint getHeadLocation() { return d.headLocation; }

	/**
	 * Method to return the Location on an end of this ArcInst.
	 * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
	 * @return the Location on an end.
	 */
	public EPoint getLocation(int connIndex)
	{
		switch (connIndex)
		{
			case TAILEND: return d.tailLocation;
			case HEADEND: return d.headLocation;
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
    
    private static boolean stillInPoly(Point2D pt, Poly poly) { return poly.isInside(pt) || poly.polyDistance(pt.getX(), pt.getY()) < MINPORTDISTANCE; }
    
    /****************************** TEXT ******************************/

	/**
	 * Method to return the name of this ArcInst.
	 * @return the name of this ArcInst.
	 */
	public String getName()	{ return d.name.toString(); }

	/**
	 * Retruns true if this ArcInst was named by user.
	 * @return true if this ArcInst was named by user.
	 */		
	public boolean isUsernamed() { return !d.name.isTempname();	}

	/**
	 * Method to return the name key of this ArcInst.
	 * @return the name key of this ArcInst, null if there is no name.
	 */
	public Name getNameKey() { return d.name; }

	/**
	 * Method to rename this ArcInst.
	 * This ArcInst must be linked to database.
	 * @param name new name of this geometric.
	 * @return true on error
	 */
	public boolean setName(String name)
	{
		assert isLinked();
		Name key;
		boolean doSmart = false;
		if (name != null && name.length() > 0)
		{
			if (name.equals(getName())) return false;
			if (!isUsernamed()) doSmart = true;
			key = Name.findName(name);
		} else
		{
			if (!isUsernamed()) return false;
			key = parent.getArcAutoname();
		}
		if (checkNameKey(key, parent)) return true;
        ImmutableArcInst oldD = d;
        lowLevelModify(d.withName(key));
        if (doSmart)
        {
    		TextDescriptor smartDescriptor = getSmartTextDescriptor(d.angle, d.width, d.nameDescriptor);
        	if (smartDescriptor != null) setTextDescriptor(ARC_NAME, smartDescriptor);
        }

        // apply constraints
        Constraints.getCurrent().modifyArcInst(this, oldD);
		return false;
	}

	/**
	 * Method to return a "smart" text descriptor for an arc.
	 * @param angle the angle of the arc (in tenths of a degree).
	 * @param width the width of the arc.
	 * @param prev the former text descriptor of the arc.
	 * @return a new text descriptor that handles smart placement.
	 * Returns null if no change was made.
	 */
	private static TextDescriptor getSmartTextDescriptor(int angle, double width, TextDescriptor prev)
	{
		// assigning valid name: do smart text placement
		if ((angle%1800) == 0)
		{
			// horizontal arc
			int smart = User.getSmartHorizontalPlacementArc();
			if (smart == 1)
			{
				// arc text above
				return prev.withPos(TextDescriptor.Position.UP).withOff(0, width/2);
			} else if (smart == 2)
			{
				// arc text below
				return prev.withPos(TextDescriptor.Position.DOWN).withOff(0, -width/2);
			}
		} else if ((angle%1800) == 900)
		{
			// vertical arc
			int smart = User.getSmartVerticalPlacementArc();
			if (smart == 1)
			{
				// arc text to the left
				return prev.withPos(TextDescriptor.Position.LEFT).withOff(-width/2, 0);
			} else if (smart == 2)
			{
				// arc text to the right
				return prev.withPos(TextDescriptor.Position.RIGHT).withOff(width/2, 0);
			}
		}
		return null;
	}

	/**
	 * Method to check the new name key of an ArcInst.
	 * @param name new name key of this ArcInst.
     * @param parent parent Cell used for error message
	 * @return true on error.
	 */
	protected static boolean checkNameKey(Name name, Cell parent)
	{
		if (!name.isValid())
		{
			System.out.println(parent + ": Invalid name \""+name+"\" wasn't assigned to arc" + " :" + Name.checkName(name.toString()));
			return true;
		}
		if (name.isTempname() && name.getBasename() != ImmutableArcInst.BASENAME)
		{
			System.out.println(parent + ": Temporary arc name \""+name+"\" must have prefix net@");
			return true;
		}
		if (name.hasEmptySubnames())
		{
			if (name.isBus())
				System.out.println(parent + ": Name \""+name+"\" with empty subnames wasn't assigned to arc");
			else
				System.out.println(parent + ": Cannot assign empty name \""+name+"\" to arc");
			return true;
		}
		if (parent.hasTempArcName(name))
		{
			System.out.println(parent + " already has ArcInst with temporary name \""+name+"\"");
			return true;
		}
		return false;
	}

	/**
	 * Returns the TextDescriptor on this ArcInst selected by variable key.
	 * This key may be a key of variable on this ArcInst or the
	 * special keys:
	 * <code>ArcInst.ARC_NAME</code>
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param varKey key of variable or special key.
	 * @return the TextDescriptor on this ArcInst.
	 */
	public TextDescriptor getTextDescriptor(Variable.Key varKey)
	{
		if (varKey == ARC_NAME) return d.nameDescriptor;
		return super.getTextDescriptor(varKey);
	}

	/**
	 * Updates the TextDescriptor on this ArcInst selected by varKey.
	 * The varKey may be a key of variable on this ArcInst or
     * the special key ArcInst.ARC_NAME.
	 * If varKey doesn't select any text descriptor, no action is performed.
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param varKey key of variable or special key.
	 * @param td new value TextDescriptor
	 */
	public void setTextDescriptor(Variable.Key varKey, TextDescriptor td)
	{
        if (varKey == ARC_NAME) {
			setD(d.withNameDescriptor(td), true);
            return;
        }
        super.setTextDescriptor(varKey, td);
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
     * @param withQuotes to wrap description between quotes
	 * @return a description of this ArcInst.
	 */
	public String describe(boolean withQuotes)
	{
		String description = getProto().describe();
		String name = (withQuotes) ? "'"+getName()+"'" : getName();
		if (name != null) description += "[" + name + "]";
		return description;
	}

    /**
     * Compares ArcInsts by their Cells and names.
     * @param that the other ArcInst.
     * @return a comparison between the ArcInsts.
     */
	public int compareTo(ArcInst that)
	{
		int cmp;
		if (this.parent != that.parent)
		{
			cmp = this.parent.compareTo(that.parent);
			if (cmp != 0) return cmp;
		}
		cmp = this.getName().compareTo(that.getName());
		if (cmp != 0) return cmp;
		return this.d.arcId - that.d.arcId;
	}

	/**
	 * Returns a printable version of this ArcInst.
	 * @return a printable version of this ArcInst.
	 */
	public String toString()
	{
        return "arc " + describe(true);
	}

	/****************************** CONSTRAINTS ******************************/

    private void setFlag(ImmutableArcInst.Flag flag, boolean state) {
        checkChanging();
        ImmutableArcInst oldD = d;
        lowLevelModify(d.withFlag(flag, state));
        if (parent != null) Constraints.getCurrent().modifyArcInst(this, oldD);
    }
    
	/**
	 * Method to set this ArcInst to be rigid.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
     * @param state
     */
	public void setRigid(boolean state) { setFlag(ImmutableArcInst.RIGID, state); }

	/**
	 * Method to tell whether this ArcInst is rigid.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
	 * @return true if this ArcInst is rigid.
	 */
	public boolean isRigid() { return d.is(ImmutableArcInst.RIGID); }

	/**
	 * Method to set this ArcInst to be fixed-angle.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
     * @param state
     */
	public void setFixedAngle(boolean state) { setFlag(ImmutableArcInst.FIXED_ANGLE, state); }

	/**
	 * Method to tell whether this ArcInst is fixed-angle.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
	 * @return true if this ArcInst is fixed-angle.
	 */
	public boolean isFixedAngle() { return d.is(ImmutableArcInst.FIXED_ANGLE); }

	/**
	 * Method to set this ArcInst to be slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
     * @param state
     */
	public void setSlidable(boolean state) { setFlag(ImmutableArcInst.SLIDABLE, state); }

	/**
	 * Method to tell whether this ArcInst is slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
	 * @return true if this ArcInst is slidable.
	 */
	public boolean isSlidable() { return d.is(ImmutableArcInst.SLIDABLE); }

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
		return d.is(ImmutableArcInst.TAIL_ARROWED);
	}

	/**
	 * Method to determine whether this ArcInst is directional, with an arrow on the head.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @return true if the arc's head has a directional arrow on it.
     */
	public boolean isHeadArrowed()
	{
		return d.is(ImmutableArcInst.HEAD_ARROWED);
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
		return d.is(ImmutableArcInst.BODY_ARROWED);
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
	public void setTailArrowed(boolean state) { setFlag(ImmutableArcInst.TAIL_ARROWED, state); }

	/**
	 * Method to set this ArcInst to be directional, with an arrow on the head.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
     * @param state true to show a directional arrow on the head.
     */
	public void setHeadArrowed(boolean state) { setFlag(ImmutableArcInst.HEAD_ARROWED, state); }

	/**
	 * Method to set this ArcInst to be directional, with an arrow line drawn down the center.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * The body is typically drawn when one of the ends has an arrow on it, but it may be
	 * drawin without an arrow head in order to continue an attached arc that has an arrow.
     * @param state true to show a directional line on this arc.
     */
	public void setBodyArrowed(boolean state) { setFlag(ImmutableArcInst.BODY_ARROWED, state); }

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
	public boolean isTailExtended() { return d.is(ImmutableArcInst.TAIL_EXTENDED); }

	/**
	 * Method to tell whether the head of this arc is extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @return true if the head of this arc is extended.
	 */
	public boolean isHeadExtended() { return d.is(ImmutableArcInst.HEAD_EXTENDED); }

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
	public void setTailExtended(boolean e) {
        setFlag(ImmutableArcInst.TAIL_EXTENDED, e);
//        if (isLinked()) updateGeometric();
    }

	/**
	 * Method to set whether the head of this arc is extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @param e true to set the head of this arc to be extended.
	 */
	public void setHeadExtended(boolean e) {
        setFlag(ImmutableArcInst.HEAD_EXTENDED, e);
//        if (isLinked()) updateGeometric();
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
	public boolean isTailNegated() { return d.is(ImmutableArcInst.TAIL_NEGATED); }

	/**
	 * Method to tell whether the head of this arc is negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 * @return true if set the head of this arc is negated.
	 */
	public boolean isHeadNegated() { return d.is(ImmutableArcInst.HEAD_NEGATED); }

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
	public void setTailNegated(boolean n) {
        setFlag(ImmutableArcInst.TAIL_NEGATED, n);
			// only allow if negation is supported on this port
//			PortProto pp = tailPortInst.getPortProto();
//			if (pp instanceof PrimitivePort && ((PrimitivePort)pp).isNegatable())
	}

	/**
	 * Method to set whether the head of this arc is negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 * @param n true to set the head of this arc to be negated.
	 */
	public void setHeadNegated(boolean n) {
        setFlag(ImmutableArcInst.HEAD_NEGATED,  n);
			// only allow if negation is supported on this port
// 			PortProto pp = headPortInst.getPortProto();
// 			if (pp instanceof PrimitivePort && ((PrimitivePort)pp).isNegatable())
	}

	/****************************** MISCELLANEOUS ******************************/

    /**
	 * Method to check and repair data structure errors in this ArcInst.
	 */
	public int checkAndRepair(boolean repair, List<Geometric> list, ErrorLogger errorLogger)
	{
		int errorCount = 0;
        ArcProto ap = getProto();

        if (ap.isNotUsed())
        {
//            if (repair)
            if (errorLogger != null)
            {
                String msg = "Prototype of arc " + getName() + " is unused";
                if (repair) {
                    // Can't put this arc into error logger because it will be deleted.
                    Poly poly = makePoly(getWidth() - ap.getWidthOffset(), Poly.Type.CLOSED);
                    errorLogger.logError(msg, poly, parent, 1);
                } else {
                    errorLogger.logError(msg, this, parent, null, 1);
                }
            }
            if (repair) list.add(this);
            // This counts as 1 error, ignoring other errors
            return 1;
        }

		// see if the ends are in their ports
		if (!headStillInPort(d.headLocation, false))
		{
			Poly poly = headPortInst.getPoly();
			String msg = parent + ", " + this +
				": head not in port, is at (" + d.headLocation.getX() + "," + d.headLocation.getY() +
				") distance to port is " + poly.polyDistance(d.headLocation.getX(), d.headLocation.getY()) +
				" port center is (" + poly.getCenterX() + "," + poly.getCenterY() + ")";
			System.out.println(msg);
			if (errorLogger != null)
			{
                if (repair) {
                    errorLogger.logError(msg, Collections.singletonList((Geometric)headPortInst.getNodeInst()), null, null, null,
                            Collections.singletonList((PolyBase)makePoly(getWidth() - ap.getWidthOffset(), Poly.Type.CLOSED)), parent, 1);
                } else {
                    List<Geometric> geomList = new ArrayList<Geometric>();
                    geomList.add(this);
                    geomList.add(headPortInst.getNodeInst());
                    errorLogger.logError(msg, geomList, null, parent, 1);
               }
			}
			if (repair)
			{
                Constraints.getCurrent().modifyArcInst(this, getD());
//				setD(d.withLocations(d.tailLocation, new EPoint(poly.getCenterX(), poly.getCenterY())), false);
//				updateGeometric();
			}
			errorCount++;
		}
		if (!tailStillInPort(d.tailLocation, false))
		{
			Poly poly = tailPortInst.getPoly();
			String msg = parent + ", " + this +
				": tail not in port, is at (" + d.tailLocation.getX() + "," + d.tailLocation.getY() +
				") distance to port is " + poly.polyDistance(d.tailLocation.getX(), d.tailLocation.getY()) +
				" port center is (" + poly.getCenterX() + "," + poly.getCenterY() + ")";
			System.out.println(msg);
			if (errorLogger != null)
			{
                if (repair) {
                    errorLogger.logError(msg, Collections.singletonList((Geometric)tailPortInst.getNodeInst()), null, null, null,
                            Collections.singletonList((PolyBase)makePoly(getWidth() - ap.getWidthOffset(), Poly.Type.CLOSED)), parent, 1);
                } else {
                    List<Geometric> geomList = new ArrayList<Geometric>();
                    geomList.add(this);
                    geomList.add(tailPortInst.getNodeInst());
                    errorLogger.logError(msg, geomList, null, parent, 1);
               }
			}
			if (repair)
			{
                Constraints.getCurrent().modifyArcInst(this, getD());
//				setD(d.withLocations(new EPoint(poly.getCenterX(), poly.getCenterY()), d.headLocation), false);
//				updateGeometric();
			}
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
	 * Method to return the prototype of this ArcInst.
	 * @return the prototype of this ArcInst.
	 */
	public ArcProto getProto() { return d.protoType; }

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
        copyTextDescriptorFrom(fromAi, ArcInst.ARC_NAME);
    }

    /**
     * Copies constraints (Rigid, Ends Extended, etc) from another arcinst to this arcinst
     * It copies also attributes of Connections (arrow/negated/extended)
     * @param fromAi the arcinst from which to copy constraints
     */
    public void copyConstraintsFrom(ArcInst fromAi) {
        checkChanging();
        if (fromAi == null) return;
        ImmutableArcInst oldD = d;
        lowLevelModify(d.withFlags(fromAi.d.flags).withAngle(fromAi.d.angle));
		if (parent != null) Constraints.getCurrent().modifyArcInst(this, oldD);
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
	private void setDefaultConstraints(ArcProto protoType)
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
	public void setHardSelect(boolean state) { setFlag(ImmutableArcInst.HARD_SELECT, state); }

	/**
	 * Method to tell whether this ArcInst is hard-to-select.
	 * Hard-to-select ArcInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 * @return true if this ArcInst is hard-to-select.
	 */
	public boolean isHardSelect() { return d.is(ImmutableArcInst.HARD_SELECT); }

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
         if (getProto().getClass() != a.getProto().getClass())
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
        List<Poly> noCheckAgain = new ArrayList<Poly>();
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
				int result = Poly.cropBoxComplete(polyBounds, nPolyBounds);
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

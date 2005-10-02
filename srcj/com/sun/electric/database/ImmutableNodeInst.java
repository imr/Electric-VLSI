/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableNodeInst.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.database;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProtoId;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Immutable class ImmutableNodeInst represents a node instance.
 * 
 * @promise "requiresColor DBChanger;" for with*(**) | newInstance(**)
 * @promise "requiresColor (DBChanger | DBExaminer | AWT);" for check()
 */
public class ImmutableNodeInst extends ImmutableElectricObject {
    /** 
     * Class to access user bits of ImmutableNodeInst.
     */
    public static class Flag {
        private final int mask;

        private Flag(int mask) {
            this.mask = mask;
        }
        
        /**
         * Returns true if this Flag is set in userBits.
         * @param userBits user bits.
         * @return true if this Flag is set in userBits;
         */
        public boolean is(int userBits) {
            return (userBits & mask) != 0;
        }
        
        /**
         * Updates this flag in userBits.
         * @param userBits old user bits.
         * @param value new value of flag.
         * @return updates userBits.
         */
        public int set(int userBits, boolean value) {
            return value ? userBits | mask : userBits & ~mask;
        }
    }
    
// -------------------------- constants --------------------------------
//	/** node is not in use */								private static final int DEADN =                     01;
//	/** node has text that is far away */					private static final int NHASFARTEXT =               02;
//	/** if on, draw node expanded */						private static final int NEXPAND =                   04;
//	/** set if node not drawn due to wiping arcs */			private static final int NWIPED =                   010;
//	/** set if node is to be drawn shortened */				private static final int NSHORT =                   020;
	//  used by database:                                                                                      0140
//	/** if on, this nodeinst is marked for death */			private static final int KILLN =                   0200;
//	/** nodeinst re-drawing is scheduled */					private static final int REWANTN =                 0400;
//	/** only local nodeinst re-drawing desired */			private static final int RELOCLN =                01000;
//	/** transparent nodeinst re-draw is done */				private static final int RETDONN =                02000;
//	/** opaque nodeinst re-draw is done */					private static final int REODONN =                04000;
//	/** general flag used in spreading and highlighting */	private static final int NODEFLAGBIT =           010000;
//	/** if on, nodeinst wants to be (un)expanded */			private static final int WANTEXP =               020000;
//	/** temporary flag for nodeinst display */				private static final int TEMPFLG =               040000;
	/** set if hard to select */							private static final int HARDSELECTN =          0100000;
	/** set if node only visible inside cell */				private static final int NVISIBLEINSIDE =     040000000;
	/** technology-specific bits for primitives */			private static final int NTECHBITS =          037400000;
	/** right-shift of NTECHBITS */							private static final int NTECHBITSSH =               17;
	/** set if node is locked (can't be changed) */			private static final int NILOCKED =          0100000000;

 	private static final int FLAG_BITS = HARDSELECTN | NVISIBLEINSIDE | NILOCKED;

	/**
	 * Method to set an ImmutableNodeInst to be hard-to-select.
	 * Hard-to-select ImmutableNodeInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 */
	public static final Flag HARD_SELECT = new Flag(HARDSELECTN);
	/**
	 * Flag to set an ImmutableNodeInst to be visible-inside.
	 * An ImmutableNodeInst that is "visible inside" is only drawn when viewing inside of the Cell.
	 * It is not visible from outside (meaning from higher-up the hierarchy).
	 */
	public static final Flag VIS_INSIDE = new Flag(NVISIBLEINSIDE);
	/**
	 * Method to set this ImmutableNodeInst to be locked.
	 * Locked ImmutableNodeInsts cannot be modified or deleted.
	 */
	public static final Flag LOCKED = new Flag(NILOCKED);
    
    /** id of this NodeInst in parent. */                           public final int nodeId;
	/** Prototype id. */                                            public final NodeProtoId protoId;
	/** name of this ImmutableNodeInst. */							public final Name name;
    /** duplicate index of this ImmutableNodeInst in the Cell */    public final int duplicate;
	/** The text descriptor of name of ImmutableNodeInst. */		public final TextDescriptor nameDescriptor;
	/** Orientation of this ImmutableNodeInst. */                   public final Orientation orient;
	/** anchor coordinate of this ImmutableNodeInst. */				public final EPoint anchor;
	/** size of this ImmutableNodeInst . */                         public final double width, height;
	/** Flag bits for this ImmutableNodeInst. */                    public final int flags;
    /** Tech specifiic bits for this ImmutableNodeInsts. */         public final byte techBits;
	/** Text descriptor of prototype name. */                       public final TextDescriptor protoDescriptor;
 
	/**
	 * The private constructor of ImmutableNodeInst. Use the factory "newInstance" instead.
     * @param nodeId id of this NodeInst in parent.
	 * @param protoId the NodeProtoId of which this is an instance.
	 * @param name name of new ImmutableNodeInst.
	 * @param duplicate duplicate index of this ImmutableNodeInst.
     * @param nameDescriptor TextDescriptor of name of this ImmutableNodeInst.
     * @param orient Orientation of this ImmutableNodeInst.
	 * @param anchor the anchor location of this ImmutableNodeInst.
	 * @param width the width of this ImmutableNodeInst.
	 * @param height the height of this ImmutableNodeInst.
     * @param flags flag bits for thisImmutableNdoeIsnt.
	 * @param techBits tech speicfic bits of this ImmutableNodeInst.
     * @param protoDescriptor TextDescriptor of prototype name of this ImmutableNodeInst
     * @param vars array of Variables of this ImmutableNodeInst
	 */
    ImmutableNodeInst(int nodeId, NodeProtoId protoId,
            Name name, int duplicate, TextDescriptor nameDescriptor,
            Orientation orient, EPoint anchor, double width, double height,
            int flags, byte techBits, TextDescriptor protoDescriptor, Variable[] vars) {
        super(vars);
        this.nodeId = nodeId;
        this.protoId = protoId;
        this.name = name;
        this.duplicate = duplicate;
        this.nameDescriptor = nameDescriptor;
        this.orient = orient;
        this.anchor = anchor;
        this.width = width;
        this.height = height;
        this.flags = flags;
        this.techBits = techBits;
        this.protoDescriptor = protoDescriptor;
        check();
    }

	/**
	 * Returns new ImmutableNodeInst object.
     * @param nodeId id of this NodeInst in parent.
	 * @param protoId the NodeProtoId of which this is an instance.
	 * @param name name of new ImmutableNodeInst.
	 * @param duplicate duplicate index of this ImmutableNodeInst.
     * @param nameDescriptor TextDescriptor of name of this ImmutableNodeInst.
     * @param orient Orientation of this ImmutableNodeInst.
	 * @param anchor the anchor location of this ImmutableNodeInst.
	 * @param width the width of this ImmutableNodeInst.
	 * @param height the height of this ImmutableNodeInst.
     * @param flags flags of this NodeInst.
	 * @param techBits bits associated to different technologies
     * @param protoDescriptor TextDescriptor of name of this ImmutableNodeInst
	 * @return new ImmutableNodeInst object.
	 * @throws NullPointerException if protoId, name, orient or anchor is null.
     * @throws IllegalArgumentException if duplicate, or size is bad.
	 */
    public static ImmutableNodeInst newInstance(int nodeId, NodeProtoId protoId,
            Name name, int duplicate, TextDescriptor nameDescriptor,
            Orientation orient, EPoint anchor, double width, double height,
            int flags, int techBits, TextDescriptor protoDescriptor) {
		if (protoId == null) throw new NullPointerException("protoId");
		if (name == null) throw new NullPointerException("name");
        if (!name.isValid() || name.hasEmptySubnames() || name.isTempname() && name.isBus()) throw new IllegalArgumentException("name");
        if (name.hasDuplicates()) throw new IllegalArgumentException("name");
        if (duplicate < 0) throw new IllegalArgumentException("duplicate");
        if (nameDescriptor != null)
            nameDescriptor = nameDescriptor.withDisplayWithoutParamAndCode();
        if (orient == null) throw new NullPointerException("orient");
		if (anchor == null) throw new NullPointerException("anchor");
        if (!(width >= 0)) throw new IllegalArgumentException("width");
        if (!(height >= 0)) throw new IllegalArgumentException("height");
        if (protoId instanceof CellId)
            width = height = 0;
        if (protoId == Generic.tech.cellCenterNode) {
            orient = Orientation.IDENT;
            anchor = EPoint.ORIGIN;
            width = height = 0;
        }
        width = DBMath.round(width);
        height = DBMath.round(height);
        if (width == -0.0) width = +0.0;
        if (height == -0.0) height = +0.0;
        flags &= FLAG_BITS;
        techBits &= NTECHBITS >> NTECHBITSSH;
        if (protoDescriptor != null)
            protoDescriptor = protoDescriptor.withDisplayWithoutParamAndCode();
		return new ImmutableNodeInst(nodeId, protoId, name, duplicate, nameDescriptor,
                orient, anchor, width, height, flags, (byte)techBits, protoDescriptor, Variable.NULL_ARRAY);
    }

//	/**
//	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by protoId.
//	 * @param protoId node protoId.
//	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by protoId.
//	 * @throws ArrayIndexOutOfBoundsException if protoId is negative.
//	 */
//	public ImmutableNodeInst withProto(int protoId) {
//		if (this.protoId == protoId) return this;
//		if (protoId < 0) throw new ArrayIndexOutOfBoundsException(protoId);
//		return new ImmutableNodeInst(protoId, this.name, this.duplicate, this.nameDescriptor,
//                this.orient, this.anchor, this.width, this.height, this.userBits, this.protoDescriptor);
//	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by name and duplicate.
	 * @param name node name key.
     * @param duplicate duplicate of the name
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by name and duplicate.
	 * @throws NullPointerException if name is null
     * @throws IllegalArgumentException if duplicate is negative.
	 */
	public ImmutableNodeInst withName(Name name, int duplicate) {
		if (this.name.equals(name) && this.duplicate == duplicate) return this;
		if (name == null) throw new NullPointerException("name");
        if (!name.isValid() || name.hasEmptySubnames() || name.isTempname() && name.isBus()) throw new IllegalArgumentException("name");
        if (name.hasDuplicates()) throw new IllegalArgumentException("name");
        if (duplicate < 0) throw new IllegalArgumentException("duplicate");
		return new ImmutableNodeInst(this.nodeId, this.protoId, name, duplicate, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.flags, this.techBits, this.protoDescriptor, getVars());
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by name descriptor.
     * @param td TextDescriptor of name
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by name descriptor.
	 */
	public ImmutableNodeInst withNameDescriptor(TextDescriptor nameDescriptor) {
        if (nameDescriptor != null)
            nameDescriptor = nameDescriptor.withDisplayWithoutParamAndCode();
        if (this.nameDescriptor == nameDescriptor) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.duplicate, nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.flags, this.techBits, this.protoDescriptor, getVars());
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by orientation.
     * @param orient Orientation.
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by orientation.
	 * @throws NullPointerException if orient is null.
	 */
	public ImmutableNodeInst withOrient(Orientation orient) {
        if (this.orient == orient) return this;
        if (orient == null) throw new NullPointerException("orient");
        if (protoId == Generic.tech.cellCenterNode) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.duplicate, this.nameDescriptor,
                orient, this.anchor, this.width, this.height, this.flags, this.techBits, this.protoDescriptor, getVars());
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by anchor point.
	 * @param anchor node anchor point.
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by anchor point.
	 * @throws NullPointerException if anchor is null.
	 */
	public ImmutableNodeInst withAnchor(EPoint anchor) {
		if (this.anchor.equals(anchor)) return this;
		if (anchor == null) throw new NullPointerException("anchor");
        if (protoId == Generic.tech.cellCenterNode) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.duplicate, this.nameDescriptor,
                this.orient, anchor, this.width, this.height, this.flags, this.techBits, this.protoDescriptor, getVars());
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by width and height.
	 * @param width node width.
     * @param height node height.
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by width and height.
     * @throws IllegalArgumentException if width or height is negative.
	 */
	public ImmutableNodeInst withSize(double width, double height) {
		if (this.width == width && this.height == height) return this;
        if (!(width >= 0)) throw new IllegalArgumentException("width");
        if (!(height >= 0)) throw new IllegalArgumentException("height");
        if (protoId == Generic.tech.cellCenterNode) return this;
        if (protoId instanceof CellId) return this;
        width = DBMath.round(width);
        height = DBMath.round(height);
        if (width == -0.0) width = +0.0;
        if (height == -0.0) height = +0.0;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.duplicate, this.nameDescriptor,
                this.orient, this.anchor, width, height, this.flags, this.techBits, this.protoDescriptor, getVars());
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by flag bits.
	 * @param flags flag bits defined by ImmutableNodeInst.Flag.
     * @return ImmutableNodeInst which differs from this ImmutableNodeInst by flag bit.
	 */
    public ImmutableNodeInst withFlags(int flags) {
        flags &= FLAG_BITS;
        if (this.flags == flags) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.duplicate, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, flags, this.techBits, this.protoDescriptor, getVars());
    }

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by flag bit.
	 * @param flag Flag selector.
     * @param value new value of flag.
     * @return ImmutableNodeInst which differs from this ImmutableNodeInst by flag bit.
	 */
    public ImmutableNodeInst withFlag(Flag flag, boolean value) {
        return withFlags(flag.set(this.flags, value));
    }

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by tech specific bits.
	 * This is mostly used by the Schematics technology which allows variations
	 * on a NodeInst to be stored.
	 * For example, the Transistor primitive uses these bits to distinguish nMOS, pMOS, etc.
	 * @param techBits the Technology-specific value to store on this NodeInst.
     * @return ImmutableNodeInst which differs from this ImmutableNodeInst by tech bits.
	 */
	public ImmutableNodeInst withTechSpecific(int techBits) {
        techBits &= NTECHBITS >> NTECHBITSSH;
        if (this.techBits == techBits) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.duplicate, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.flags, (byte)techBits, this.protoDescriptor, getVars());
    }
    
    /**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by proto descriptor.
     * @param protoDescriptor TextDescriptor of proto
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by proto descriptor.
	 */
	public ImmutableNodeInst withProtoDescriptor(TextDescriptor protoDescriptor) {
        if (protoDescriptor != null)
            protoDescriptor = protoDescriptor.withDisplayWithoutParamAndCode();
        if (this.protoDescriptor == protoDescriptor) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.duplicate, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.flags, this.techBits, protoDescriptor, getVars());
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by additional Variable.
     * If this ImmutableNideInst has Variable with the same key as new, the old variable will not be in new
     * ImmutableNodeInst.
	 * @param var additional Variable.
	 * @return ImmutableNodeInst with additional Variable.
	 * @throws NullPointerException if var is null
	 */
    public ImmutableNodeInst withVariable(Variable var) {
        if (var.descriptor.isParam())
            var = var.withParam(false);
        Variable[] vars = arrayWithVariable(var);
        if (this.getVars() == vars) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.duplicate, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.flags, this.techBits, this.protoDescriptor, vars);
    }
    
	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by removing Variable
     * with the specified key. Returns this ImmutableNodeInst if it doesn't contain variable with the specified key.
	 * @param key Variable Key to remove.
	 * @return ImmutableNodeInst without Variable with the specified key.
	 * @throws NullPointerException if var is null
	 */
    public ImmutableNodeInst withoutVariable(Variable.Key key) {
        Variable[] vars = arrayWithoutVariable(key);
        if (this.getVars() == vars) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.duplicate, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.flags, this.techBits, this.protoDescriptor, vars);
    }
    
//    /**
//     * Returns flags of this ImmutableNodeInst.
//     * This flags are defined by ImmutableNodeInst.Flag .
//     * @return flags of this ImmutableNodeInst.
//     */
//    public int getFlags() { return flags; }
    
    /**
     * Tests specific flag is set on this ImmutableNodeInst.
     * @param flag flag selector.
     * @return true if specific flag is set,
     */
    public boolean is(Flag flag) { return flag.is(flags); }
    
//	/**
//	 * Method to return the Technology-specific value on this ImmutableNodeInst.
//	 * This is mostly used by the Schematics technology which allows variations
//	 * on a NodeInst to be stored.
//	 * For example, the Transistor primitive uses these bits to distinguish nMOS, pMOS, etc.
//	 * @return the Technology-specific value on this ImmutableNodeInst.
//	 */
//	public byte getTechSpecific() { return techBits; }

	/**
	 * Checks invariant of this ImmutableNodeInst.
	 * @throws AssertionError if invariant is broken.
	 */
	public void check() {
		assert protoId != null;
		assert name != null;
        assert name.isValid() && !name.hasEmptySubnames();
        assert !(name.isTempname() && name.isBus());
        assert !name.hasDuplicates();
        assert duplicate >= 0;
        if (nameDescriptor != null)
            assert nameDescriptor.isDisplay() && !nameDescriptor.isCode() && !nameDescriptor.isParam();
        assert orient != null;
        assert anchor != null;
        assert width > 0 || width == 0 && 1/width > 0;
        assert height > 0 || height == 0 && 1/height > 0;
        assert DBMath.round(width) == width;
        assert DBMath.round(height) == height;
        assert (flags & ~FLAG_BITS) == 0;
        assert (techBits & ~(NTECHBITS >> NTECHBITSSH)) == 0;
        if (protoDescriptor != null)
            assert protoDescriptor.isDisplay() && !protoDescriptor.isCode() && !protoDescriptor.isParam();
        if (protoId instanceof CellId) {
            assert width == 0 && height == 0;
        }
        if (protoId == Generic.tech.cellCenterNode) {
            assert orient == Orientation.IDENT && anchor == EPoint.ORIGIN && width == 0 && height == 0;
        }
	}

    /**
     * Returns ELIB user bits of this ImmutableNodeInst in ELIB.
     * @return ELIB user bits of this ImmutableNodeInst.
     */
    public int getElibBits() { return flags | (techBits << NTECHBITSSH); }
    
    /**
     * Get flag bits from ELIB user bits.
     * @param elibBits ELIB user bits.
     * @return flag bits.
     */
    public static int flagsFromElib(int elibBits) { return elibBits & FLAG_BITS; }
    
    /**
     * Get tech specific bits from ELIB user bits.
     * @param elibBits ELIB user bits.
     * @return tech specific bits.
     */
    public static int techSpecificFromElib(int elibBits) { return (elibBits & NTECHBITS) >> NTECHBITSSH; }
    
    public Rectangle2D computeBounds(NodeInst real)
	{
		// handle cell bounds
		if (protoId instanceof CellId)
		{
			// offset by distance from cell-center to the true center
			Cell subCell = (Cell)real.getProto();
			Rectangle2D bounds = subCell.getBounds();
			Point2D shift = new Point2D.Double(-bounds.getCenterX(), -bounds.getCenterY());
			AffineTransform trans = orient.pureRotate();
//			AffineTransform trans = pureRotate(orient.getAngle(), isXMirrored(), isYMirrored());
			trans.transform(shift, shift);
			double cX = anchor.getX(), cY = anchor.getY();
			cX -= shift.getX();
			cY -= shift.getY();
			Poly poly = new Poly(cX, cY, bounds.getWidth(), bounds.getHeight());
			trans = orient.rotateAbout(cX, cY);
//			trans = rotateAbout(orient.getAngle(), cX, cY, getXSizeWithMirror(), getYSizeWithMirror());
			poly.transform(trans);
			return poly.getBounds2D();
		}

		// if zero size, set the bounds directly
		if (width == 0 && height == 0)
		{
			return new Rectangle2D.Double(anchor.getX(), anchor.getY(), 0, 0);
		}

		PrimitiveNode pn = (PrimitiveNode)protoId;

		// special case for arcs of circles
		if (pn == Artwork.tech.circleNode || pn == Artwork.tech.thickCircleNode)
		{
			// see if this circle is only a partial one
			double [] angles = real.getArcDegrees();
			if (angles[0] != 0.0 || angles[1] != 0.0)
			{
				Point2D [] pointList = Artwork.fillEllipse(anchor, width, height, angles[0], angles[1]);
				Poly poly = new Poly(pointList);
				poly.setStyle(Poly.Type.OPENED);
				poly.transform(orient.rotateAbout(anchor.getX(), anchor.getY()));
				return poly.getBounds2D();
			}
		}

		// special case for pins that become steiner points
		if (pn.isWipeOn1or2() && real.getNumExports() == 0)
		{
			if (real.pinUseCount())
			{
				return new Rectangle2D.Double(anchor.getX(), anchor.getY(), 0, 0);
			}
		}

		// special case for polygonally-defined nodes: compute precise geometry
		if (pn.isHoldsOutline() && real.getTrace() != null)
		{
			AffineTransform trans = orient.rotateAbout(anchor.getX(), anchor.getY());
			Poly[] polys = pn.getTechnology().getShapeOfNode(real);
			Rectangle2D bounds = new Rectangle2D.Double();
			for (int i = 0; i < polys.length; i++)
			{
				Poly poly = polys[i];
				poly.transform(trans);
				if (i == 0)
					bounds.setRect(poly.getBounds2D());
				else
					Rectangle2D.union(poly.getBounds2D(), bounds, bounds);
			}
			return bounds;
		}

		// normal bounds computation
		Poly poly = new Poly(anchor.getX(), anchor.getY(), width, height);
		AffineTransform trans = orient.rotateAbout(anchor.getX(), anchor.getY());
		poly.transform(trans);
		return poly.getBounds2D();
	}
}

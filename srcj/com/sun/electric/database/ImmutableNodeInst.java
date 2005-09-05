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
import com.sun.electric.database.variable.ImmutableTextDescriptor;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Immutable class ImmutableNodeInst represents a node instance.
 * It contains the id of prototype cell, the name, the anchor point.
 * 
 * @promise "requiresColor DBChanger;" for with*(**) | newInstance(**)
 * @promise "requiresColor (DBChanger | DBExaminer | AWT);" for check()
 */
public class ImmutableNodeInst
{
	// -------------------------- constants --------------------------------
//	/** node is not in use */								public static final int DEADN =                     01;
//	/** node has text that is far away */					public static final int NHASFARTEXT =               02;
	/** if on, draw node expanded */						public static final int NEXPAND =                   04;
	/** set if node not drawn due to wiping arcs */			public static final int WIPED =                    010;
//	/** set if node is to be drawn shortened */				public static final int NSHORT =                   020;
	//  used by database:                                                                                      0140
//	/** if on, this nodeinst is marked for death */			public static final int KILLN =                   0200;
//	/** nodeinst re-drawing is scheduled */					public static final int REWANTN =                 0400;
//	/** only local nodeinst re-drawing desired */			public static final int RELOCLN =                01000;
//	/** transparent nodeinst re-draw is done */				public static final int RETDONN =                02000;
//	/** opaque nodeinst re-draw is done */					public static final int REODONN =                04000;
//	/** general flag used in spreading and highlighting */	public static final int NODEFLAGBIT =           010000;
//	/** if on, nodeinst wants to be (un)expanded */			public static final int WANTEXP =               020000;
//	/** temporary flag for nodeinst display */				public static final int TEMPFLG =               040000;
	/** set if hard to select */							public static final int HARDSELECTN =          0100000;
	/** set if node only visible inside cell */				public static final int NVISIBLEINSIDE =     040000000;
	/** technology-specific bits for primitives */			public static final int NTECHBITS =          037400000;
	/** right-shift of NTECHBITS */							public static final int NTECHBITSSH =               17;
	/** set if node is locked (can't be changed) */			public static final int NILOCKED =          0100000000;
    
	public static final int NODE_BITS = NEXPAND | WIPED | /*NSHORT |*/ HARDSELECTN | NVISIBLEINSIDE | NTECHBITS | NILOCKED;

    /** id of this NodeInst in parent. */                           public final int nodeId;
	/** Prototype id. */                                            public final NodeProtoId protoId;
	/** name of this ImmutableNodeInst. */							public final Name name;
    /** duplicate index of this ImmutableNodeInst in the Cell */    public final int duplicate;
	/** The text descriptor of name of ImmutableNodeInst. */		public final ImmutableTextDescriptor nameDescriptor;
	/** Orientation of this ImmutableNodeInst. */                   public final Orientation orient;
	/** anchor coordinate of this ImmutableNodeInst. */				public final EPoint anchor;
	/** size of this ImmutableNodeInst . */                         public final double width, height;
	/** Flag bits for this ImmutableNodeInst. */                    public final int userBits;
	/** Text descriptor of prototype name. */                       public final ImmutableTextDescriptor protoDescriptor;
 
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
	 * @param userBits flag bits of this ImmutableNodeInst.
     * @param protoDescriptor TextDescriptor of prototype name of this ImmutableNodeInst
	 */
    ImmutableNodeInst(int nodeId, NodeProtoId protoId,
            Name name, int duplicate, ImmutableTextDescriptor nameDescriptor,
            Orientation orient, EPoint anchor, double width, double height,
            int userBits, ImmutableTextDescriptor protoDescriptor) {
        this.nodeId = nodeId;
        this.protoId = protoId;
        this.name = name;
        this.duplicate = duplicate;
        this.nameDescriptor = nameDescriptor;
        this.orient = orient;
        this.anchor = anchor;
        this.width = width;
        this.height = height;
        this.userBits = userBits;
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
	 * @param userBits bits associated to different technologies
     * @param protoDescriptor TextDescriptor of name of this ImmutableNodeInst
	 * @return new ImmutableNodeInst object.
	 * @throws NullPointerException if protoId, name, orient or anchor is null.
     * @throws IllegalArgumentException if duplicate, or size is bad.
	 */
    public static ImmutableNodeInst newInstance(int nodeId, NodeProtoId protoId,
            Name name, int duplicate, ImmutableTextDescriptor nameDescriptor,
            Orientation orient, EPoint anchor, double width, double height,
            int userBits, ImmutableTextDescriptor protoDescriptor) {
		if (protoId == null) throw new NullPointerException("protoId");
		if (name == null) throw new NullPointerException("name");
        if (duplicate < 0) throw new IllegalArgumentException("duplicate");
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
        userBits &= NODE_BITS;
		return new ImmutableNodeInst(nodeId, protoId, name, duplicate, nameDescriptor, orient, anchor, width, height, userBits, protoDescriptor);
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
		if (this.name == name && this.duplicate == duplicate) return this;
		if (name == null) throw new NullPointerException("name");
        if (duplicate < 0) throw new IllegalArgumentException("duplicate");
		return new ImmutableNodeInst(this.nodeId, this.protoId, name, duplicate, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.userBits, this.protoDescriptor);
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by name descriptor.
     * @param nameDescriptor TextDescriptor of name
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by name descriptor.
	 */
	public ImmutableNodeInst withNameDescriptor(ImmutableTextDescriptor nameDescriptor) {
        if (this.nameDescriptor == nameDescriptor) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.duplicate, nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.userBits, this.protoDescriptor);
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
                orient, this.anchor, this.width, this.height, this.userBits, this.protoDescriptor);
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by anchor point.
	 * @param anchor node anchor point.
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by anchor point.
	 * @throws NullPointerException if anchor is null.
	 */
	public ImmutableNodeInst withAnchor(EPoint anchor) {
		if (this.anchor == anchor) return this;
		if (anchor == null) throw new NullPointerException("anchor");
        if (protoId == Generic.tech.cellCenterNode) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.duplicate, this.nameDescriptor,
                this.orient, anchor, this.width, this.height, this.userBits, this.protoDescriptor);
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
                this.orient, this.anchor, width, height, this.userBits, this.protoDescriptor);
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by user bits.
	 * @param userBits flag bits of this ImmutableNodeInst.
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by user bits.
	 */
	public ImmutableNodeInst withUserBits(int userBits) {
        userBits &= NODE_BITS;
		if (this.userBits == userBits) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.duplicate, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, userBits, this.protoDescriptor);
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by proto descriptor.
     * @param protoDescriptor TextDescriptor of proto
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by proto descriptor.
	 */
	public ImmutableNodeInst withProtoDescriptor(ImmutableTextDescriptor protoDescriptor) {
        if (this.protoDescriptor == protoDescriptor) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.duplicate, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.userBits, protoDescriptor);
	}

	/**
	 * Checks invariant of this ImmutableNodeInst.
	 * @throws AssertionError if invariant is broken.
	 */
	public void check() {
		assert protoId != null;
		assert name != null;
		assert anchor != null;
        assert duplicate >= 0;
        assert orient != null;
        assert anchor != null;
        assert width > 0 || width == 0 && 1/width > 0;
        assert height > 0 || height == 0 && 1/height > 0;
        assert DBMath.round(width) == width;
        assert DBMath.round(height) == height;
        assert (userBits & ~NODE_BITS) == 0;
        if (protoId instanceof CellId) {
            assert width == 0 && height == 0;
        }
        if (protoId == Generic.tech.cellCenterNode) {
            assert orient == Orientation.IDENT && anchor == EPoint.ORIGIN && width == 0 && height == 0;
        }
	}

//	/**
//	 * Checks that protoId of this ImmutableNodeInst is contained in cells.
//	 * @param cells array with cells, may contain nulls.
//	 * @throws ArrayIndexOutOfBoundsException if protoId is not contained.
//	 */
//	void checkProto(ImmutableCell[] cells) {
//		if (cells[protoId] == null)
//			throw new ArrayIndexOutOfBoundsException(protoId);
//	}

	/**
	 * Parses JELIB string with node user bits.
     * @param jelibUserBits JELIB string.
	 * @return node user bust.
     * @throws NumberFormatException
	 */
    public static int parseJelibUserBits(String jelibUserBits) {
        int userBits = 0;
        // parse state information in jelibUserBits 
        for(int i=0; i<jelibUserBits.length(); i++) {
            char chr = jelibUserBits.charAt(i);
            switch (chr) {
                case 'E': userBits |= NEXPAND; break;
                case 'L': userBits |= NILOCKED; break;
                case 'S': /*userBits |= NSHORT;*/ break; // deprecated
                case 'V': userBits |= NVISIBLEINSIDE; break;
                case 'W': userBits |= WIPED; break;
                case 'A': userBits |= HARDSELECTN; break;
                default:
                    if (Character.isDigit(chr)) {
                        jelibUserBits = jelibUserBits.substring(i);
                        int techBits = Integer.parseInt(jelibUserBits);
                        return userBits | (techBits << NTECHBITSSH) & NTECHBITS;
                    }
            }
        }
        return userBits;
     }

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

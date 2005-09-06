/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableArcInst.java
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
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ImmutableTextDescriptor;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Immutable class ImmutableArcInst represents an arc instance.
 */
public class ImmutableArcInst
{
	// -------------------------- constants --------------------------------
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

    /** id of this ArcInst in parent. */                            public final int arcId;
	/** Arc prototype. */                                           public final ArcProto ap;
	/** name of this ImmutableArcInst. */							public final Name name;
    /** duplicate index of this ImmutableArcInst in the Cell */     public final int duplicate;
	/** The text descriptor of name of ImmutableArcInst. */         public final ImmutableTextDescriptor nameDescriptor;
    
	/** NodeId on tail end of this ImmutableArcInst. */             public final int tailNodeId;
    /** PortProtoId on tail end of this ImmutableArcInst. */        public final PortProtoId tailPortId;
	/** Location of tail end of this ImmutableArcInst. */           public final EPoint tailLocation;

	/** NodeId on head end of this ImmutableArcInst. */             public final int headNodeId;
    /** PortProtoId on head end of this ImmutableArcInst. */        public final PortProtoId headPortId;
	/** Location of head end of this ImmutableArcInst. */           public final EPoint headLocation;

	/** width of this ImmutableArcInst. */                          public final double width;
	/** Flag bits for this ImmutableArcInst. */                     public final int userBits;
 
	/**
	 * The private constructor of ImmutableArcInst. Use the factory "newInstance" instead.
     * @param arcId id of this ArcInst in parent.
	 * @param ap arc prototype.
	 * @param name name of this ImmutableArcInst.
	 * @param duplicate duplicate index of this ImmutableArcInst.
     * @param nameDescriptor TextDescriptor of name of this ImmutableArcInst.
     * @param tailNodeId NodeId on tail end of this ImmutableArcInst.
     * @param tailPortProtoId PortProtoId on tail end of this ImmutableArcInst.
     * @param tailLocation Location of tail end of this ImmutableArcInst.
     * @param headNodeId NodeId on head end of this ImmutableArcInst.
     * @param headPortProtoId PortProtoId on head end of this ImmutableArcInst.
     * @param headLocation Location of head end of this ImmutableArcInst.
	 * @param width the width of this ImmutableArcInst.
	 * @param userBits flag bits of this ImmutableNodeInst.
	 */
    ImmutableArcInst(int arcId, ArcProto ap,
            Name name, int duplicate, ImmutableTextDescriptor nameDescriptor,
            int tailNodeId, PortProtoId tailPortId, EPoint tailLocation,
            int headNodeId, PortProtoId headPortId, EPoint headLocation,
            double width, int userBits) {
        this.arcId = arcId;
        this.ap = ap;
        this.name = name;
        this.duplicate = duplicate;
        this.nameDescriptor = nameDescriptor;
        this.tailNodeId = tailNodeId;
        this.tailPortId = tailPortId;
        this.tailLocation = tailLocation;
        this.headNodeId = headNodeId;
        this.headPortId = headPortId;
        this.headLocation = headLocation;
        this.width = width;
        this.userBits = userBits;
        check();
    }

	/**
	 * Returns new ImmutableArcInst object.
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
    public static ImmutableArcInst newInstance(int arcId, ArcProto ap,
            Name name, int duplicate, ImmutableTextDescriptor nameDescriptor,
            int tailNodeId, PortProtoId tailPortId, EPoint tailLocation,
            int headNodeId, PortProtoId headPortId, EPoint headLocation,
            double width, int userBits) {
		if (ap == null) throw new NullPointerException("ap");
		if (name == null) throw new NullPointerException("name");
        if (duplicate < 0) throw new IllegalArgumentException("duplicate");
        if (!(width >= 0)) throw new IllegalArgumentException("width");
        if (tailPortId == null) throw new NullPointerException("tailPortId");
        if (tailLocation == null) throw new NullPointerException("tailLocation");
        if (headPortId == null) throw new NullPointerException("tailPortId");
        if (headLocation == null) throw new NullPointerException("tailLocation");
        width = DBMath.round(width);
        if (width == -0.0) width = +0.0;
        userBits &= DATABASE_BITS;
		return new ImmutableArcInst(arcId, ap, name, duplicate, nameDescriptor,
                tailNodeId, tailPortId, tailLocation,
                headNodeId, headPortId, headLocation,
                width, updateAngle(userBits, tailLocation, headLocation));
    }

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by name and duplicate.
	 * @param name node name key.
     * @param duplicate duplicate of the name
	 * @return ImmutableArcInst which differs from this ImmutableArcInst by name and duplicate.
	 * @throws NullPointerException if name is null
     * @throws IllegalArgumentException if duplicate is negative.
	 */
	public ImmutableArcInst withName(Name name, int duplicate) {
		if (this.name == name && this.duplicate == duplicate) return this;
		if (name == null) throw new NullPointerException("name");
        if (duplicate < 0) throw new IllegalArgumentException("duplicate");
		return new ImmutableArcInst(this.arcId, this.ap, name, duplicate, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.width, this.userBits);
	}

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by name descriptor.
     * @param nameDescriptor TextDescriptor of name
	 * @return ImmutableArcInst which differs from this ImmutableArcInst by name descriptor.
	 */
	public ImmutableArcInst withNameDescriptor(ImmutableTextDescriptor nameDescriptor) {
        if (this.nameDescriptor == nameDescriptor) return this;
		return new ImmutableArcInst(this.arcId, this.ap, this.name, this.duplicate, nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.width, this.userBits);
	}

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by tailLocation.
	 * @param tailLocation new tail location.
	 * @return ImmutableArcInst which differs from this ImmutableArcInst by tailLocation.
	 * @throws NullPointerException if tailLocation is null.
	 */
	public ImmutableArcInst withTailLocation(EPoint tailLocation) {
		if (this.tailLocation == tailLocation) return this;
		if (tailLocation == null) throw new NullPointerException("tailLocation");
		return new ImmutableArcInst(this.arcId, this.ap, this.name, this.duplicate, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.width, updateAngle(this.userBits, tailLocation, headLocation));
	}

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by headLocation.
	 * @param headLocation new head location.
	 * @return ImmutableArcInst which differs from this ImmutableArcInst by headLocation.
	 * @throws NullPointerException if headLocation is null.
	 */
	public ImmutableArcInst withHeadLocation(EPoint headLocation) {
		if (this.headLocation == headLocation) return this;
		if (headLocation == null) throw new NullPointerException("headLocation");
		return new ImmutableArcInst(this.arcId, this.ap, this.name, this.duplicate, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, headLocation,
                this.width, updateAngle(this.userBits, tailLocation, headLocation));
	}

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by width.
	 * @param width arc width.
	 * @return ImmutableArcInst which differs from this ImmutableArcInst by width.
     * @throws IllegalArgumentException if width is negative.
	 */
	public ImmutableArcInst withWidth(double width) {
		if (this.width == width) return this;
        if (!(width >= 0)) throw new IllegalArgumentException("width");
        width = DBMath.round(width);
        if (width == -0.0) width = +0.0;
		return new ImmutableArcInst(this.arcId, this.ap, this.name, this.duplicate, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                width, this.userBits);
	}

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by user bits.
	 * @param arcBits flag bits of this ImmutableArcInst.
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by user bits.
	 */
	public ImmutableArcInst withUserBits(int userBits) {
        userBits &= DATABASE_BITS;
		if (this.userBits == userBits) return this;
		return new ImmutableArcInst(this.arcId, this.ap, this.name, this.duplicate, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.width, userBits);
	}

    private static int updateAngle(int userBits, EPoint tailLocation, EPoint headLocation) {
        if (tailLocation.equals(headLocation)) return userBits;
        int angle = DBMath.figureAngle(tailLocation, headLocation);
        return (userBits & ~AANGLE) | (angle << AANGLESH);
    }
    
	/**
	 * Checks invariant of this ImmutableNodeInst.
	 * @throws AssertionError if invariant is broken.
	 */
	public void check() {
		assert ap != null;
		assert name != null;
        assert duplicate >= 0;
        assert tailPortId != null;
        assert tailLocation != null;
        assert headPortId != null;
        assert headLocation != null;
        assert width > 0 || width == 0 && 1/width > 0;
        assert DBMath.round(width) == width;
        assert (userBits & ~DATABASE_BITS) == 0;
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

//	/**
//	 * Parses JELIB string with node user bits.
//     * @param jelibUserBits JELIB string.
//	 * @return node user bust.
//     * @throws NumberFormatException
//	 */
//    public static int parseJelibUserBits(String jelibUserBits) {
//        int userBits = 0;
//        // parse state information in jelibUserBits 
//        for(int i=0; i<jelibUserBits.length(); i++) {
//            char chr = jelibUserBits.charAt(i);
//            switch (chr) {
//                case 'E': userBits |= NEXPAND; break;
//                case 'L': userBits |= NILOCKED; break;
//                case 'S': /*userBits |= NSHORT;*/ break; // deprecated
//                case 'V': userBits |= NVISIBLEINSIDE; break;
//                case 'W': userBits |= WIPED; break;
//                case 'A': userBits |= HARDSELECTN; break;
//                default:
//                    if (Character.isDigit(chr)) {
//                        jelibUserBits = jelibUserBits.substring(i);
//                        int techBits = Integer.parseInt(jelibUserBits);
//                        return userBits | (techBits << NTECHBITSSH) & NTECHBITS;
//                    }
//            }
//        }
//        return userBits;
//     }
}

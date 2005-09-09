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
    /** 
     * Class to access a flag in user bits of ImmutableNodeInst.
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
    
    /**
     * Class to access a flag in user bits of ImmutableArcInst which is true by default.
     */
    private static class FlagInv extends Flag {
        private FlagInv(int mask) { super(mask); }
        
        /**
         * Returns true if this Flag is set in userBits.
         * @param userBits user bits.
         * @return true if this Flag is set in userBits;
         */
        public boolean is(int userBits) { return !super.is(userBits); }
        
        /**
         * Updates this flag in userBits.
         * @param userBits old user bits.
         * @param value new value of flag.
         * @return updates userBits.
         */
        public int set(int userBits, boolean value) { return super.set(userBits, !value); }
    } 
    
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

	/**
	 * Flag to set an ImmutableArcInst to be rigid.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
     */
    public static final Flag RIGID = new Flag(FIXED);
	/**
	 * Flag to set an ImmutableArcInst to be fixed-angle.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
     */
	public static final Flag FIXED_ANGLE = new Flag(FIXANG);
	/**
	 * Flag to set an ImmutableArcInst to be slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
     */
	public static final Flag SLIDABLE = new FlagInv(CANTSLIDE);
	/**
	 * Flag to set an ImmutableArcInst to be directional, with an arrow on the tail.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
     */
	public static final Flag TAIL_ARROWED = new Flag(TAILARROW);
	/**
	 * Flag to set an ImmutableArcInst to be directional, with an arrow on the head.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
     */
	public static final Flag HEAD_ARROWED = new Flag(HEADARROW);
	/**
	 * Flag to set an ImmutableArcInst to be directional, with an arrow line drawn down the center.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * The body is typically drawn when one of the ends has an arrow on it, but it may be
	 * drawin without an arrow head in order to continue an attached arc that has an arrow.
     */
	 public static final Flag BODY_ARROWED = new Flag(BODYARROW);
	/**
	 * Flag to set the tail of an ImmutableArcInst to be is extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 */
     public static final Flag TAIL_EXTENDED = new FlagInv(TAILNOEXTEND);
	/**
	 * Flag to set the head of an ImmutableArcInst to be extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 */
     public static final Flag HEAD_EXTENDED = new FlagInv(HEADNOEXTEND);
	/**
	 * Flag to set the tail of an ImmutableArcInst to be negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 */
     public static final Flag TAIL_NEGATED = new Flag(ISTAILNEGATED);
	/**
	 * Flag to set the head of an ImmutableArcInst to be negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 */
     public static final Flag HEAD_NEGATED = new Flag(ISHEADNEGATED);
	/**
	 * Flag to set an ImmutableArcInst to be hard-to-select.
	 * Hard-to-select ArcInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
     */
     public static final Flag HARD_SELECT = new Flag(HARDSELECTA);

    /** bits with common meaniong in disk and database */   private static int COMMON_BITS = FIXED | FIXANG | CANTSLIDE | HARDSELECTA;  
    /** bits used in database */                            public static final int DATABASE_BITS = COMMON_BITS | AANGLE | BODYARROW |
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
	 * Method to compute the "userbits" to use for a given ArcInst.
	 * The "userbits" are a set of bits that describes constraints and other properties,
	 * and are stored in ELIB files.
	 * The negation, directionality, and end-extension must be converted.
	 * @return the "userbits" for that ArcInst.
	 */
	public static int makeELIBArcBits(int userBits)
	{
		int diskBits = userBits & COMMON_BITS;
	
		// adjust bits for extension
		if (!HEAD_EXTENDED.is(userBits) || !TAIL_EXTENDED.is(userBits))
		{
			diskBits |= DISK_NOEXTEND;
			if (HEAD_EXTENDED.is(userBits) != TAIL_EXTENDED.is(userBits))
			{
				if (TAIL_EXTENDED.is(userBits)) diskBits |= DISK_NOTEND0;
				if (HEAD_EXTENDED.is(userBits)) diskBits |= DISK_NOTEND1;
			}
		}
	
		// adjust bits for directionality
		if (HEAD_ARROWED.is(userBits) || TAIL_ARROWED.is(userBits) || BODY_ARROWED.is(userBits))
		{
			diskBits |= DISK_ISDIRECTIONAL;
			if (TAIL_ARROWED.is(userBits)) diskBits |= DISK_REVERSEEND;
			if (!HEAD_ARROWED.is(userBits) && !TAIL_ARROWED.is(userBits)) diskBits |= DISK_NOTEND1;
		}

		// adjust bits for negation
        boolean normalEnd = (diskBits & DISK_REVERSEEND) == 0;
		if (TAIL_NEGATED.is(userBits)) diskBits |= (normalEnd ? ISTAILNEGATED : ISHEADNEGATED);
		if (HEAD_NEGATED.is(userBits)) diskBits |= (normalEnd ? ISHEADNEGATED : ISTAILNEGATED);
        
		//        int angle = getAngle() / 10;
		int angle = (int)(getAngle(userBits)/10.0 + 0.5);
		if (angle >= 360) angle -= 360;
        diskBits |= angle << DISK_AANGLESH;
        
        return diskBits;
	}

	private static int getAngle(int userBits) { return (userBits & AANGLE) >> AANGLESH; }
}

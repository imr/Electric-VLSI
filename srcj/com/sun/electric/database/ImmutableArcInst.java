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
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.AbstractShapeBuilder;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.FPGA;

import java.awt.geom.Point2D;
import java.io.IOException;

/**
 * Immutable class ImmutableArcInst represents an arc instance.
 */
public class ImmutableArcInst extends ImmutableElectricObject {
	/** The index of the tail of this ArcInst. */		public static final int TAILEND = 0;
	/** The index of the head of this ArcInst. */		public static final int HEADEND = 1;
 	/** Key of Varible holding arc curvature. */		public static final Variable.Key ARC_RADIUS = Variable.newKey("ARC_radius");

    /** 
     * Class to access a flag in user bits of ImmutableNodeInst.
     */
    public static class Flag {
        final int mask;
        final char jelibChar;
        final boolean jelibDefault;

        private Flag(int mask, char jelibChar, boolean jelibDefault) {
            this.mask = mask;
            this.jelibChar = jelibChar;
            this.jelibDefault = jelibDefault;
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
    
    private static final short EXTEND_90 = 0;
    private static final short EXTEND_0 = 1;
    private static final short EXTEND_ANY = 2;
    
    
	/** fixed-length arc */                                 private static final int ELIB_FIXED =                01;
	/** fixed-angle arc */                                  private static final int ELIB_FIXANG =               02;
//	/** arc has text that is far away */                    private static final int AHASFARTEXT =               04;
//	/** arc is not in use */                                private static final int DEADA =                    020;
	/** DISK: angle of arc from end 0 to end 1 */           private static final int ELIB_AANGLE =           037740;
	/** DISK: bits of right shift for DISK_AANGLE field */  private static final int ELIB_AANGLESH =              5;
//	/** set if arc is to be drawn shortened */              private static final int ASHORT =                040000;
	/** set if head end of ArcInst is negated */            private static final int ELIB_ISHEADNEGATED =   0200000;
	/** DISK: set if ends do not extend by half width */    private static final int ELIB_NOEXTEND =        0400000;
	/** set if tail end of ArcInst is negated */            private static final int ELIB_ISTAILNEGATED =  01000000;
    /** DISK: set if arc aims from end 0 to end 1 */        private static final int ELIB_ISDIRECTIONAL =  02000000; 
    /** DISK: no extension/negation/arrows on end 0 */      private static final int ELIB_NOTEND0 =        04000000;
    /** DISK: no extension/negation/arrows on end 1 */      private static final int ELIB_NOTEND1 =       010000000;
	/** DISK: reverse extension/negation/arrow ends */      private static final int ELIB_REVERSEEND =    020000000;
	/** set if arc can't slide around in ports */           private static final int ELIB_CANTSLIDE =     040000000;
//	/** set if afixed arc was changed */                    private static final int FIXEDMOD =          0100000000;
//	/** only local arcinst re-drawing desired */            private static final int RELOCLA =          01000000000;
//	/**transparent arcinst re-draw is done */               private static final int RETDONA =          02000000000;
//	/** opaque arcinst re-draw is done */                   private static final int REODONA =          04000000000;
//	/** general flag for spreading and highlighting */      private static final int ARCFLAGBIT =      010000000000;
	/** set if hard to select */                            private static final int ELIB_HARDSELECTA =020000000000;

    private static final int TAIL_ARROWED_MASK  = 0x001;
    private static final int HEAD_ARROWED_MASK  = 0x002;
    private static final int TAIL_EXTENDED_MASK = 0x004;
    private static final int HEAD_EXTENDED_MASK = 0x008;
    private static final int TAIL_NEGATED_MASK  = 0x010;
    private static final int HEAD_NEGATED_MASK  = 0x020;
    private static final int BODY_ARROWED_MASK  = 0x040;
    private static final int RIGID_MASK         = 0x080;
    private static final int FIXED_ANGLE_MASK   = 0x100;
    private static final int SLIDABLE_MASK      = 0x200;
    private static final int HARD_SELECT_MASK   = 0x400;
    private static final int DATABASE_FLAGS     = 0x7ff;
    
    private static final int EASY_MASK          = 0x800;
    
	/**
	 * Flag to set an ImmutableArcInst to be rigid.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
     */
    public static final Flag RIGID = new Flag(RIGID_MASK, 'R', false);
	/**
	 * Flag to set an ImmutableArcInst to be fixed-angle.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
     */
	public static final Flag FIXED_ANGLE = new Flag(FIXED_ANGLE_MASK, 'F', true);
	/**
	 * Flag to set an ImmutableArcInst to be slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
     */
	public static final Flag SLIDABLE = new Flag(SLIDABLE_MASK, 'S', false);
	/**
	 * Flag to set an ImmutableArcInst to be directional, with an arrow on the tail.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
     */
	public static final Flag TAIL_ARROWED = new Flag(TAIL_ARROWED_MASK, 'Y', false);
	/**
	 * Flag to set an ImmutableArcInst to be directional, with an arrow on the head.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
     */
	public static final Flag HEAD_ARROWED = new Flag(HEAD_ARROWED_MASK, 'X', false);
	/**
	 * Flag to set an ImmutableArcInst to be directional, with an arrow line drawn down the center.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * The body is typically drawn when one of the ends has an arrow on it, but it may be
	 * drawin without an arrow head in order to continue an attached arc that has an arrow.
     */
	 public static final Flag BODY_ARROWED = new Flag(BODY_ARROWED_MASK, 'B', false);
	/**
	 * Flag to set the tail of an ImmutableArcInst to be is extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 */
     public static final Flag TAIL_EXTENDED = new Flag(TAIL_EXTENDED_MASK, 'J', true);
	/**
	 * Flag to set the head of an ImmutableArcInst to be extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 */
     public static final Flag HEAD_EXTENDED = new Flag(HEAD_EXTENDED_MASK, 'I', true);
	/**
	 * Flag to set the tail of an ImmutableArcInst to be negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 */
     public static final Flag TAIL_NEGATED = new Flag(TAIL_NEGATED_MASK, 'N', false);
	/**
	 * Flag to set the head of an ImmutableArcInst to be negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 */
     public static final Flag HEAD_NEGATED = new Flag(HEAD_NEGATED_MASK, 'G', false);
	/**
	 * Flag to set an ImmutableArcInst to be hard-to-select.
	 * Hard-to-select ArcInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
     */
     public static final Flag HARD_SELECT = new Flag(HARD_SELECT_MASK, 'A', false);

    /** initial bits */                                     public static final int DEFAULT_FLAGS = SLIDABLE.mask | HEAD_EXTENDED.mask | TAIL_EXTENDED.mask;

	/** prefix for autonameing. */                          public static final Name BASENAME = Name.findName("net@0");

    public final static ImmutableArcInst[] NULL_ARRAY = {};
    public final static ImmutableArrayList<ImmutableArcInst> EMPTY_LIST = new ImmutableArrayList<ImmutableArcInst>(NULL_ARRAY);
    
    /** id of this ArcInst in parent. */                            public final int arcId;
	/** Arc prototype. */                                           public final ArcProto protoType;
	/** name of this ImmutableArcInst. */							public final Name name;
	/** The text descriptor of name of ImmutableArcInst. */         public final TextDescriptor nameDescriptor;
    
	/** NodeId on tail end of this ImmutableArcInst. */             public final int tailNodeId;
    /** PortProtoId on tail end of this ImmutableArcInst. */        public final PortProtoId tailPortId;
	/** Location of tail end of this ImmutableArcInst. */           public final EPoint tailLocation;

	/** NodeId on head end of this ImmutableArcInst. */             public final int headNodeId;
    /** PortProtoId on head end of this ImmutableArcInst. */        public final PortProtoId headPortId;
	/** Location of head end of this ImmutableArcInst. */           public final EPoint headLocation;

	/** width of this ImmutableArcInst in grid units. */            private final int gridFullWidth;
    /** Angle if this ImmutableArcInst (in tenth-degrees). */       private final short angle;
 
    /**
     * The private constructor of ImmutableArcInst. Use the factory "newInstance" instead.
     *
     * @param arcId id of this ArcInst in parent.
     * @param protoType arc prototype.
     * @param name name of this ImmutableArcInst.
     * @param nameDescriptor TextDescriptor of name of this ImmutableArcInst.
     * @param tailNodeId NodeId on tail end of this ImmutableArcInst.
     * @param tailPortProtoId PortProtoId on tail end of this ImmutableArcInst.
     * @param tailLocation Location of tail end of this ImmutableArcInst.
     * @param headNodeId NodeId on head end of this ImmutableArcInst.
     * @param headPortProtoId PortProtoId on head end of this ImmutableArcInst.
     * @param headLocation Location of head end of this ImmutableArcInst.
     * @param gridFullWidth the full width of this ImmutableArcInst in grid units.
     * @param angle the angle if this ImmutableArcInst (in tenth-degrees).
     * @param flags flag bits of this ImmutableArcInst.
     * @param vars array of Variables of this ImmutableArcInst
     */
    ImmutableArcInst(int arcId, ArcProto protoType, Name name, TextDescriptor nameDescriptor,
            int tailNodeId, PortProtoId tailPortId, EPoint tailLocation,
            int headNodeId, PortProtoId headPortId, EPoint headLocation,
            int gridFullWidth, short angle, int flags, Variable[] vars) {
        super(vars, flags);
        this.arcId = arcId;
        this.protoType = protoType;
        this.name = name;
        this.nameDescriptor = nameDescriptor;
        this.tailNodeId = tailNodeId;
        this.tailPortId = tailPortId;
        this.tailLocation = tailLocation;
        this.headNodeId = headNodeId;
        this.headPortId = headPortId;
        this.headLocation = headLocation;
        this.gridFullWidth = gridFullWidth;
        this.angle = angle;
        check();
    }
    
	/**
	 * Retruns true if this ImmutableArcInst was named by user.
	 * @return true if this ImmutableArcInst was named by user.
	 */		
	public boolean isUsernamed() { return !name.isTempname();	}

    /**
     * Returns full width of this ImmutableArcInst in lambda units.
     * @return full width of this ImmutableArcInst in lambda units.
     */
    public double getLambdaFullWidth() { return DBMath.gridToLambda(getGridFullWidth()); }
    
    /**
     * Returns full width of this ImmutableArcInst in grid units.
     * @return full width of this ImmutableArcInst in grid units.
     */
    public long getGridFullWidth() { return gridFullWidth; }
    
    /**
     * Returns base width of this ImmutableArcInst in lambda units.
     * @return base width of this ImmutableArcInst in lambda units.
     */
    public double getLambdaBaseWidth() { return DBMath.gridToLambda(getGridBaseWidth()); }
    
    /**
     * Returns base width of this ImmutableArcInst in grid units.
     * @return base width of this ImmutableArcInst in grid units.
     */
    public long getGridBaseWidth() { return gridFullWidth - protoType.getGridWidthOffset(); }
    
    /**
     * Returns length of this ImmutableArcInst in lambda units.
     * @return length of this ImmutableArcInst in lambda units.
     */
    public double getLambdaLength() { return tailLocation.lambdaDistance(headLocation); }
    
    /**
     * Returns length of this ImmutableArcInst in grid units.
     * @return length of this ImmutableArcInst in grid units.
     */
    public double getGridLength() { return tailLocation.gridDistance(headLocation); }
    
	/**
	 * Method to return the rotation angle of this ImmutableArcInst.
     * This is an angle of direction from tailLocation to headLocation.
	 * @return the rotation angle of this ImmutableArcInst (in tenth-degrees).
	 */
	public int getAngle() { return angle; }

    /**
     * Tests specific flag is set on this ImmutableArcInst.
     * @param flag flag selector.
     * @return true if specific flag is set,
     */
    public boolean is(Flag flag) { return (flags & flag.mask) != 0; }
    
	/**
	 * Method to tell whether this ImmutableArcInst is rigid.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
	 * @return true if this ImmutableArcInst is rigid.
	 */
	public boolean isRigid() { return (flags & RIGID_MASK) != 0; }

	/**
	 * Method to tell whether this ImmutableArcInst is fixed-angle.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
	 * @return true if this ImmutableArcInst is fixed-angle.
	 */
	public boolean isFixedAngle() { return (flags & FIXED_ANGLE_MASK) != 0; }

	/**
	 * Method to tell whether this ImmutableArcInst is slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
	 * @return true if this ImmutableArcInst is slidable.
	 */
	public boolean isSlidable() { return (flags & SLIDABLE_MASK) != 0; }

	/**
	 * Method to tell whether this ArcInst is hard-to-select.
	 * Hard-to-select ArcInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 * @return true if this ArcInst is hard-to-select.
	 */
	public boolean isHardSelect() { return (flags & HARD_SELECT_MASK) != 0; }

	/****************************** PROPERTIES ******************************/

	/**
	 * Method to determine whether this ImmutableArcInst is directional, with an arrow on one end.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
	 * @return true if that end has a directional arrow on it.
     */
    public boolean isArrowed(int connIndex) {
        if ((connIndex & ~1) != 0) throw new IllegalArgumentException("Bad end " + connIndex);
        return ((flags >> connIndex) & TAIL_ARROWED_MASK) != 0;
    }

	/**
	 * Method to determine whether this ImmutableArcInst is directional, with an arrow on the tail.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @return true if the arc's tail has a directional arrow on it.
     */
	public boolean isTailArrowed() { return (flags & TAIL_ARROWED_MASK) != 0; }

	/**
	 * Method to determine whether this ImmutableArcInst is directional, with an arrow on the head.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @return true if the arc's head has a directional arrow on it.
     */
	public boolean isHeadArrowed() { return (flags & HEAD_ARROWED_MASK) != 0; }

	/**
	 * Method to determine whether this ArcInst is directional, with an arrow line drawn down the center.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * It is only for documentation purposes and does not affect the circuit.
	 * The body is typically drawn when one of the ends has an arrow on it, but it may be
	 * drawin without an arrow head in order to continue an attached arc that has an arrow.
	 * @return true if the arc's tail has an arrow line on it.
     */
	public boolean isBodyArrowed() { return (flags & BODY_ARROWED_MASK) != 0; }
	
	/**
	 * Method to tell whether an end of ImmutableArcInst has its ends extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
	 * @return true if that end of this ArcInst iss extended.
	 */
    public boolean isExtended(int connIndex) {
        if ((connIndex & ~1) != 0) throw new IllegalArgumentException("Bad end " + connIndex);
        return ((flags >> connIndex) & TAIL_EXTENDED_MASK) != 0;
    }

	/**
	 * Method to tell whether the tail of this arc is extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @return true if the tail of this arc is extended.
	 */
	public boolean isTailExtended() { return (flags & TAIL_EXTENDED_MASK) != 0; }

	/**
	 * Method to tell whether the head of this arc is extended.
	 * Extended arcs continue past their endpoint by half of their width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @return true if the head of this arc is extended.
	 */
	public boolean isHeadExtended() { return (flags & HEAD_EXTENDED_MASK) != 0; }

	/**
	 * Method to tell whether an end of this arc is negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
	 * @return true if set that end of this arc is negated.
	 */
    public boolean isNegated(int connIndex) {
        if ((connIndex & ~1) != 0) throw new IllegalArgumentException("Bad end " + connIndex);
        return ((flags >> connIndex) & TAIL_NEGATED_MASK) != 0;
    }

	/**
	 * Method to tell whether the tail of this arc is negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 * @return true if set the tail of this arc is negated.
	 */
	public boolean isTailNegated() { return (flags & TAIL_NEGATED_MASK) != 0; }

	/**
	 * Method to tell whether the head of this arc is negated.
	 * Negated arc have a negating bubble on them to indicate negation.
	 * This is only valid in schematics technologies.
	 * @return true if set the head of this arc is negated.
	 */
	public boolean isHeadNegated() { return (flags & HEAD_NEGATED_MASK) != 0; }

    private boolean isManhattan() {
        if (headLocation.getGridX() == tailLocation.getGridX()) {
            return headLocation.getGridY() != tailLocation.getGridY() ||
                    (angle == 0 || angle == 900 || angle == 1800 || angle == 2700);
        } else {
            return tailLocation.getGridY() == headLocation.getGridY();
        }
    }
    
    public boolean isEasyShape() { return (flags & EASY_MASK) != 0; }
    
    public void explainEasyShape() {
        Technology tech = protoType.getTechnology();
        if (tech == Artwork.tech && searchVar(Artwork.ART_COLOR) >= 0) {
            System.out.println("ART_COLOR");
            return;
        }
        if (tech == Artwork.tech && searchVar(Artwork.ART_PATTERN) >= 0) {
            System.out.println("ART_PATTERN");
            return;
        }
        if (tech == FPGA.tech) {
            System.out.println("FPGA");
            return;
        }
        if ((flags & (BODY_ARROWED_MASK|TAIL_ARROWED_MASK|HEAD_ARROWED_MASK)) != 0) {
            System.out.println("ARROWED");
            return;
        }
        if ((flags & (TAIL_NEGATED_MASK|HEAD_NEGATED_MASK)) != 0) {
            System.out.println("NEGATED");
            return;
        }
        if (protoType.isCurvable() && searchVar(ARC_RADIUS) >= 0) {
            System.out.println("CURVABLE");
            return;
        }
        if (!(tailLocation.isSmall() && headLocation.isSmall() && GenMath.isSmallInt(gridFullWidth))) {
            System.out.println("LARGE " + tailLocation + " " + headLocation + " " + gridFullWidth);
            return;
        }
        int width = (int)gridFullWidth;
        if (width == 0) {
            if (protoType.getNumArcLayers() != 1) {
                System.out.println(protoType + " many zero-width layers");
                return;
            }
            assert false;
        }
        if (width <= protoType.getMaxLayerGridOffset()) {
            System.out.println(protoType + " has zero-width layer");
            return;
        }
        for (int i = 0, numArcLayers = protoType.getNumArcLayers(); i < numArcLayers; i++) {
            Technology.ArcLayer arcLayer = protoType.getArcLayer(i);
            if (arcLayer.getStyle() != Poly.Type.FILLED) {
                System.out.println("Wide should be filled");
                return;
            }
        }
        int tx = (int)tailLocation.getGridX();
        int ty = (int)tailLocation.getGridY();
        int hx = (int)headLocation.getGridX();
        int hy = (int)headLocation.getGridY();
        switch (angle) {
            case 0:
                if (ty != hy) {
                    System.out.println("NEAR MANHATTAN " + tailLocation + " " + headLocation + " " + angle);
                    return;
                }
                break;
            case 900:
                if (tx != hx) {
                    System.out.println("NEAR MANHATTAN " + tailLocation + " " + headLocation + " " + angle);
                    return;
                }
                break;
            case 1800:
                if (ty != hy) {
                    System.out.println("NEAR MANHATTAN " + tailLocation + " " + headLocation + " " + angle);
                    return;
                }
                break;
            case 2700:
                if (tx != hx) {
                    System.out.println("NEAR MANHATTAN " + tailLocation + " " + headLocation + " " + angle);
                    return;
                }
                break;
            default:
                System.out.println("NON-MANHATTAN");
                return;
        }
        assert false;
    }
    
    
    private static int updateEasyShape(ArcProto protoType, int gridFullWidth, EPoint tailLocation, EPoint headLocation, short angle, Variable[] vars, int flags) {
        Technology tech = protoType.getTechnology();
        flags = flags & DATABASE_FLAGS;
        if (tech == Artwork.tech && (searchVar(vars, Artwork.ART_COLOR) >= 0 || searchVar(vars, Artwork.ART_PATTERN) >= 0))
            return flags;
        if (tech == FPGA.tech)
            return flags;
        if ((flags & (BODY_ARROWED_MASK|TAIL_ARROWED_MASK|HEAD_ARROWED_MASK|TAIL_NEGATED_MASK|HEAD_NEGATED_MASK)) != 0)
            return flags;
        if (protoType.isCurvable() && searchVar(vars, ARC_RADIUS) >= 0)
            return flags;
        if (!(tailLocation.isSmall() && headLocation.isSmall() && GenMath.isSmallInt(gridFullWidth)))
            return flags;
        int width = (int)gridFullWidth;
        if (width == 0) {
            if (protoType.getNumArcLayers() != 1) return flags;
            return flags | EASY_MASK;
        }
        if (width <= protoType.getMaxLayerGridOffset())
            return flags;
        for (int i = 0, numArcLayers = protoType.getNumArcLayers(); i < numArcLayers; i++) {
            Technology.ArcLayer arcLayer = protoType.getArcLayer(i);
            if (arcLayer.getStyle() != Poly.Type.FILLED) return flags;
        }
        int tx = (int)tailLocation.getGridX();
        int ty = (int)tailLocation.getGridY();
        int hx = (int)headLocation.getGridX();
        int hy = (int)headLocation.getGridY();
        switch (angle) {
            case 0:
                if (ty != hy) return flags;
                break;
            case 900:
                if (tx != hx) return flags;
                break;
            case 1800:
                if (ty != hy) return flags;
                break;
            case 2700:
                if (tx != hx) return flags;
                break;
            default:
                return flags;
        }
        return flags | EASY_MASK;
    }
    
    /**
     * Returns new ImmutableArcInst object.
     * @param arcId id of this ArcInst in parent.
     * @param protoType arc prototype.
     * @param name name of this ImmutableArcInst.
     * @param nameDescriptor TextDescriptor of name of this ImmutableArcInst.
     * @param tailNodeId NodeId on tail end of this ImmutableArcInst.
     * @param tailPortId PortProtoId on tail end of this ImmutableArcInst.
     * @param tailLocation Location of tail end of this ImmutableArcInst.
     * @param headNodeId NodeId on head end of this ImmutableArcInst.
     * @param headPortId PortProtoId on head end of this ImmutableArcInst.
     * @param headLocation Location of head end of this ImmutableArcInst.
     * @param gridFullWidth the full width of this ImmutableArcInst in grid units.
     * @param angle the angle if this ImmutableArcInst (in tenth-degrees).
     * @param flags flag bits of this ImmutableNodeInst.
     * @return new ImmutableArcInst object.
     * @throws NullPointerException if protoType, name, tailPortId, headPortId, tailLocation, headLocation is null.
     * @throws IllegalArgumentException if arcId, tailNodeId, headNodeId or name is not valid, or width is bad.
     */
    public static ImmutableArcInst newInstance(int arcId, ArcProto protoType, Name name, TextDescriptor nameDescriptor,
            int tailNodeId, PortProtoId tailPortId, EPoint tailLocation,
            int headNodeId, PortProtoId headPortId, EPoint headLocation,
            long gridFullWidth, int angle, int flags) {
        if (arcId < 0) throw new IllegalArgumentException("arcId");
        if (protoType == null) throw new NullPointerException("protoType");
        if (name == null) throw new NullPointerException("name");
        if (!name.isValid() || name.hasEmptySubnames() || name.isTempname() && name.getBasename() != BASENAME) throw new IllegalArgumentException("name");
        if (nameDescriptor != null)
            nameDescriptor = nameDescriptor.withDisplayWithoutParamAndCode();
        if (tailNodeId < 0) throw new IllegalArgumentException("tailNodeId");
        if (tailPortId == null) throw new NullPointerException("tailPortId");
        if (tailLocation == null) throw new NullPointerException("tailLocation");
        if (headNodeId < 0) throw new IllegalArgumentException("headNodeId");
        if (headPortId == null) throw new NullPointerException("headPortId");
        if (headLocation == null) throw new NullPointerException("headLocation");
        if (gridFullWidth < 0 || gridFullWidth < protoType.getMaxLayerGridOffset() || gridFullWidth > Integer.MAX_VALUE || (gridFullWidth&1) != 0) throw new IllegalArgumentException("gridFullWidth");
        int intGridWidth = (int)gridFullWidth;
        angle %= 3600;
        if (angle < 0) angle += 3600;
        short shortAngle = updateAngle((short)angle, tailLocation, headLocation);
        flags &= DATABASE_FLAGS;
        if (!(tailPortId instanceof PrimitivePort && ((PrimitivePort)tailPortId).isNegatable()))
            flags &= ~TAIL_NEGATED_MASK;
        if (!(headPortId instanceof PrimitivePort && ((PrimitivePort)headPortId).isNegatable()))
            flags &= ~HEAD_NEGATED_MASK;
        if (protoType.getTechnology().isNoNegatedArcs())
            flags &= ~(TAIL_NEGATED_MASK|HEAD_NEGATED_MASK);
        flags = updateEasyShape(protoType, intGridWidth, tailLocation, headLocation, shortAngle, Variable.NULL_ARRAY, flags);
        return new ImmutableArcInst(arcId, protoType, name, nameDescriptor,
                tailNodeId, tailPortId, tailLocation,
                headNodeId, headPortId, headLocation,
                intGridWidth, shortAngle, flags, Variable.NULL_ARRAY);
    }

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by name.
	 * @param name node name key.
	 * @return ImmutableArcInst which differs from this ImmutableArcInst by name.
	 * @throws NullPointerException if name is null
     * @throws IllegalArgumentException if name is not valid.
	 */
	public ImmutableArcInst withName(Name name) {
		if (this.name.toString().equals(name.toString())) return this;
		if (name == null) throw new NullPointerException("name");
        if (!name.isValid() || name.hasEmptySubnames() || name.isTempname() && name.getBasename() != BASENAME) throw new IllegalArgumentException("name");
		return new ImmutableArcInst(this.arcId, this.protoType, name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridFullWidth, this.angle, this.flags, getVars());
	}

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by name descriptor.
     * @param nameDescriptor TextDescriptor of name
	 * @return ImmutableArcInst which differs from this ImmutableArcInst by name descriptor.
	 */
	public ImmutableArcInst withNameDescriptor(TextDescriptor nameDescriptor) {
        if (nameDescriptor != null)
            nameDescriptor = nameDescriptor.withDisplayWithoutParamAndCode();
        if (this.nameDescriptor == nameDescriptor) return this;
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridFullWidth, this.angle, this.flags, getVars());
	}

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by tailLocation and headLocation.
	 * @param tailLocation new tail location.
     * @param headLocation new head location.
	 * @return ImmutableArcInst which differs from this ImmutableArcInst by tailLocation and headLocation.
	 * @throws NullPointerException if tailLocation is null.
	 */
	public ImmutableArcInst withLocations(EPoint tailLocation, EPoint headLocation) {
		if (this.tailLocation.equals(tailLocation) && this.headLocation.equals(headLocation)) return this;
		if (tailLocation == null) throw new NullPointerException("tailLocation");
		if (headLocation == null) throw new NullPointerException("headLocation");
        short angle = updateAngle(this.angle, tailLocation, headLocation);
        int flags = updateEasyShape(this.protoType, this.gridFullWidth, tailLocation, headLocation, angle, getVars(), this.flags);
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, tailLocation,
                this.headNodeId, this.headPortId, headLocation,
                this.gridFullWidth, angle, flags, getVars());
	}

	/**
     * Returns ImmutableArcInst which differs from this ImmutableArcInst by width.
     * @param gridFullWidth full arc width in grid units.
     * @return ImmutableArcInst which differs from this ImmutableArcInst by width.
     * @throws IllegalArgumentException if width is negative.
     */
	public ImmutableArcInst withGridFullWidth(long gridFullWidth) {
		if (getGridFullWidth() == gridFullWidth) return this;
        if (gridFullWidth < 0 || gridFullWidth < protoType.getMaxLayerGridOffset() || gridFullWidth > Integer.MAX_VALUE || (gridFullWidth&1) != 0) throw new IllegalArgumentException("gridWidth");
        if (this.gridFullWidth == gridFullWidth) return this;
        int flags = updateEasyShape(this.protoType, (int)gridFullWidth, this.tailLocation, this.headLocation, this.angle, getVars(), this.flags);
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                (int)gridFullWidth, this.angle, flags, getVars());
	}

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by angke.
     * For arc with non-zero length returns ths ImmutableArcInst
	 * @param angle angle in tenth-degrees.
	 * @return ImmutableArcInst which differs from this ImmutableArcInst by user bits.
	 */
	public ImmutableArcInst withAngle(int angle) {
        if (!tailLocation.equals(headLocation)) return this;
        angle %= 3600;
        if (angle < 0) angle += 3600;
		if (this.angle == angle) return this;
        short shortAngle = (short)angle;
        int flags = updateEasyShape(this.protoType, this.gridFullWidth, this.tailLocation, this.headLocation, shortAngle, getVars(), this.flags);
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridFullWidth, shortAngle, flags, getVars());
	}

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by user bits.
	 * @param flags flag bits of this ImmutableArcInst.
	 * @return ImmutableArcInst which differs from this ImmutableArcInst by user bits.
	 */
	public ImmutableArcInst withFlags(int flags) {
        flags &= DATABASE_FLAGS;
        if (!(tailPortId instanceof PrimitivePort && ((PrimitivePort)tailPortId).isNegatable()))
            flags &= ~TAIL_NEGATED_MASK;
        if (!(headPortId instanceof PrimitivePort && ((PrimitivePort)headPortId).isNegatable()))
            flags &= ~HEAD_NEGATED_MASK;
        if (protoType.getTechnology().isNoNegatedArcs())
            flags &= ~(TAIL_NEGATED_MASK|HEAD_NEGATED_MASK);
        flags = updateEasyShape(this.protoType, this.gridFullWidth, this.tailLocation, this.headLocation, this.angle, getVars(), flags);
        if (this.flags == flags) return this;
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridFullWidth, this.angle, flags, getVars());
	}

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by flag bit.
	 * @param flag Flag selector.
     * @param value new value of flag.
     * @return ImmutableArcInst which differs from this ImmutableArcInst by flag bit.
	 */
    public ImmutableArcInst withFlag(Flag flag, boolean value) {
        return withFlags(flag.set(this.flags, value));
    }

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by additional Variable.
     * If this ImmutableArcInst has Variable with the same key as new, the old variable will not be in new
     * ImmutableArcInst.
	 * @param var additional Variable.
	 * @return ImmutableArcInst with additional Variable.
	 * @throws NullPointerException if var is null
	 */
    public ImmutableArcInst withVariable(Variable var) {
        Variable[] vars = arrayWithVariable(var.withParam(false));
        if (this.getVars() == vars) return this;
        int flags = this.flags;
        Variable.Key key = var.getKey();
        if (key == Artwork.ART_COLOR || key == Artwork.ART_PATTERN || key == ARC_RADIUS)
            flags = updateEasyShape(this.protoType, this.gridFullWidth, this.tailLocation, this.headLocation, this.angle, vars, flags);
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridFullWidth, this.angle, flags, vars);
    }
    
	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by removing Variable
     * with the specified key. Returns this ImmutableArcInst if it doesn't contain variable with the specified key.
	 * @param key Variable Key to remove.
	 * @return ImmutableArcInst without Variable with the specified key.
	 * @throws NullPointerException if key is null
	 */
    public ImmutableArcInst withoutVariable(Variable.Key key) {
        Variable[] vars = arrayWithoutVariable(key);
        if (this.getVars() == vars) return this;
        int flags = this.flags;
        if (key == Artwork.ART_COLOR || key == Artwork.ART_PATTERN || key == ARC_RADIUS)
            flags = updateEasyShape(this.protoType, this.gridFullWidth, this.tailLocation, this.headLocation, this.angle, vars, flags);
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridFullWidth, this.angle, flags, vars);
    }
    
	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by renamed Ids.
	 * @param idMapper a map from old Ids to new Ids.
     * @return ImmutableArcInst with renamed Ids.
	 */
    ImmutableArcInst withRenamedIds(IdMapper idMapper) {
        Variable[] vars = arrayWithRenamedIds(idMapper);
        PortProtoId tailPortId = this.tailPortId;
        PortProtoId headPortId = this.headPortId;
        if (tailPortId instanceof ExportId)
            tailPortId = idMapper.get((ExportId)tailPortId);
        if (headPortId instanceof ExportId)
            headPortId = idMapper.get((ExportId)headPortId);
        if (getVars() == vars && this.tailPortId == tailPortId && this.headPortId == headPortId) return this;
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, tailPortId, this.tailLocation,
                this.headNodeId, headPortId, this.headLocation,
                this.gridFullWidth, this.angle, this.flags, vars);
    }
    
    private static short updateAngle(short angle, EPoint tailLocation, EPoint headLocation) {
        if (tailLocation.equals(headLocation)) return angle;
        return (short)GenMath.figureAngle(headLocation.getGridX() - tailLocation.getGridX(), headLocation.getGridY() - tailLocation.getGridY());
    }
    
    /**
     * Writes this ImmutableArcInst to SnapshotWriter.
     * @param writer where to write.
     */
    void write(SnapshotWriter writer) throws IOException {
        writer.writeArcId(arcId);
        writer.writeArcProto(protoType);
        writer.writeNameKey(name);
        writer.writeTextDescriptor(nameDescriptor);
        writer.writeNodeId(tailNodeId);
        writer.writePortProtoId(tailPortId);
        writer.writePoint(tailLocation);
        writer.writeNodeId(headNodeId);
        writer.writePortProtoId(headPortId);
        writer.writePoint(headLocation);
        writer.writeInt(gridFullWidth);
        writer.writeShort(angle);
        writer.writeInt(flags);
        super.write(writer);
    }
    
    /**
     * Reads ImmutableArcInst from SnapshotReader.
     * @param reader where to read.
     */
    static ImmutableArcInst read(SnapshotReader reader) throws IOException {
        int arcId = reader.readNodeId();
        ArcProto protoType = reader.readArcProto();
        Name name = reader.readNameKey();
        TextDescriptor nameDescriptor = reader.readTextDescriptor();
        int tailNodeId = reader.readNodeId();
        PortProtoId tailPortId = reader.readPortProtoId();
        EPoint tailLocation = reader.readPoint();
        int headNodeId = reader.readNodeId();
        PortProtoId headPortId = reader.readPortProtoId();
        EPoint headLocation = reader.readPoint();
        int gridFullWidth = reader.readInt();
        short angle = reader.readShort();
        int flags = reader.readInt();
        boolean hasVars = reader.readBoolean();
        Variable[] vars = hasVars ? readVars(reader) : Variable.NULL_ARRAY;
        return new ImmutableArcInst(arcId, protoType, name, nameDescriptor,
                tailNodeId, tailPortId, tailLocation, headNodeId, headPortId, headLocation, gridFullWidth,
                updateAngle((short)angle, tailLocation, headLocation), flags, vars);
    }
    
    /**
     * Return a hash code value for fields of this object.
     * Variables of objects are not compared
     */
    public int hashCodeExceptVariables() { return arcId; }

    /**
     * Indicates whether fields of other ImmutableElectricObject are equal to fileds of this object.
     * Variables of objects are not compared.
     * @param o other ImmutableElectricObject.
     * @return true if fields of objects are equal.
     */
    public boolean equalsExceptVariables(ImmutableElectricObject o) {
        if (this == o) return true;
        if (!(o instanceof ImmutableArcInst)) return false;
        ImmutableArcInst that = (ImmutableArcInst)o;
        return this.arcId == that.arcId && this.protoType == that.protoType &&
                this.name == that.name && this.nameDescriptor == that.nameDescriptor &&
                this.tailNodeId == that.tailNodeId && this.tailPortId == that.tailPortId && this.tailLocation == that.tailLocation &&
                this.headNodeId == that.headNodeId && this.headPortId == that.headPortId && this.headLocation == that.headLocation &&
                this.gridFullWidth == that.gridFullWidth && this.angle == that.angle && this.flags == that.flags;
    }
    
	/**
	 * Method to create a Poly object that describes this ImmutableArcInst in grid units.
	 * The Poly is described by its width, and style.
     * @param m data for size computation in a CellBackup
	 * @param gridWidth the gridWidth of the Poly.
	 * @param style the style of the Poly.
	 * @return a Poly that describes this ImmutableArcInst in grid units.
	 */
    public Poly makeGridPoly(CellBackup.Memoization m, long gridWidth, Poly.Type style) {
        if (protoType.isCurvable()) {
            // get the radius information on the arc
            Double radiusDouble = getRadius();
            if (radiusDouble != null) {
                Poly curvedPoly = curvedArcGridOutline(style, gridWidth, DBMath.lambdaToGrid(radiusDouble));
                if (curvedPoly != null) return curvedPoly;
            }
        }
        
        // zero-width polygons are simply lines
        if (gridWidth == 0) {
            Poly poly = new Poly(new Point2D.Double[]{ tailLocation.gridMutable(), headLocation.gridMutable()});
            if (style == Poly.Type.FILLED) style = Poly.Type.OPENED;
            poly.setStyle(style);
            return poly;
        }
        
        // make the polygon
		int w2 = ((int)gridWidth) >>> 1;
        short shrinkT = isTailExtended() ? m.getShrinkage(tailNodeId) : EXTEND_0;
        short shrinkH = isHeadExtended() ? m.getShrinkage(headNodeId) : EXTEND_0;
        Point2D.Double lT = new Point2D.Double();
        Point2D.Double rT = new Point2D.Double();
        Point2D.Double rH = new Point2D.Double();
        Point2D.Double lH = new Point2D.Double();

        double w2x = DBMath.roundShapeCoord(w2*GenMath.cos(angle));
        double w2y = DBMath.roundShapeCoord(w2*GenMath.sin(angle));
        
        double tx = tailLocation.getGridX();
        double ty = tailLocation.getGridY();
        if (shrinkT >= EXTEND_ANY) {
            Point2D e = computeExtension(w2, -w2x, -w2y, angle >= 1800 ? angle - 1800 : angle + 1800, shrinkT);
            tx += e.getX();
            ty += e.getY();
        }
        double hx = headLocation.getGridX();
        double hy = headLocation.getGridY();
        if (shrinkH >= EXTEND_ANY) {
            Point2D e = computeExtension(w2, w2x, w2y, angle, shrinkH);
            hx += e.getX();
            hy += e.getY();
        }
        if (isManhattan()) {
            switch (angle) {
                case 0:
                    if (shrinkT == EXTEND_90) tx -= w2;
                    lT.setLocation(tx, ty + w2);
                    rT.setLocation(tx, ty - w2);
                    if (shrinkH == EXTEND_90) hx += w2;
                    rH.setLocation(hx, hy - w2);
                    lH.setLocation(hx, hy + w2);
                    break;
                case 900:
                    if (shrinkT == EXTEND_90) ty -= w2;
                    lT.setLocation(tx - w2, ty);
                    rT.setLocation(tx + w2, ty);
                    if (shrinkH == EXTEND_90) hy += w2;
                    rH.setLocation(hx + w2, hy);
                    lH.setLocation(hx - w2, hy);
                    break;
                case 1800:
                    if (shrinkT == EXTEND_90) tx += w2;
                    lT.setLocation(tx, ty - w2);
                    rT.setLocation(tx, ty + w2);
                    if (shrinkH == EXTEND_90) hx -= w2;
                    rH.setLocation(hx, hy + w2);
                    lH.setLocation(hx, hy - w2);
                    break;
                case 2700:
                    if (shrinkT == EXTEND_90) ty += w2;
                    lT.setLocation(tx + w2, ty);
                    rT.setLocation(tx - w2, ty);
                    if (shrinkH == EXTEND_90) hy -= w2;
                    rH.setLocation(hx - w2, hy);
                    lH.setLocation(hx + w2, hy);
                    break;
                default:
                    assert false;
            }
        } else {
            if (shrinkT == EXTEND_90) {
                tx -= w2x;
                ty -= w2y;
            }
            lT.setLocation(tx - w2y, ty + w2x);
            rT.setLocation(tx + w2y, ty - w2x);

            if (shrinkH == EXTEND_90) {
                hx += w2x;
                hy += w2y;
            }
            rH.setLocation(hx + w2y, hy - w2x);
            lH.setLocation(hx - w2y, hy + w2x);
        }
        
		// somewhat simpler if rectangle is manhattan
		Point2D.Double[] points;
        if (gridWidth != 0 && style.isOpened())
            points = new Point2D.Double[] { lT, rT, rH, lH, (Point2D.Double)lT.clone() };
        else
            points = new Point2D.Double[] { lT, rT, rH, lH };
		Poly poly = new Poly(points);
		poly.setStyle(style);
		return poly;
    }
    
	/**
	 * Method to fill in an AbstractShapeBuilder a polygon that describes this ImmutableArcInst in grid units.
	 * The polygon is described by its width, and style.
     * @param b shape builder.
	 * @param gridWidth the gridWidth of the Poly.
	 * @param style the style of the Poly.
	 */
    public void makeGridPoly(AbstractShapeBuilder b, long gridWidth, Poly.Type style, Layer layer) {
        long[] result;
        if (protoType.isCurvable()) {
            // get the radius information on the arc
            Double radiusDouble = getRadius();
            if (radiusDouble != null && curvedArcGridOutline(b, gridWidth, DBMath.lambdaToGrid(radiusDouble))) {
                b.pushPoly(style, layer);
                return;
            }
        }
        
        // zero-width polygons are simply lines
        if (gridWidth <= 0) {
            b.pushPoint(tailLocation);
            b.pushPoint(headLocation);
            if (style == Poly.Type.FILLED) style = Poly.Type.OPENED;
            b.pushPoly(style, layer);
            return;
        }
        
        // make the polygon
		int w2 = ((int)gridWidth) >>> 1;
        CellBackup.Memoization m = b.getMemoization();
        short shrinkT = isTailExtended() ? m.getShrinkage(tailNodeId) : EXTEND_0;
        short shrinkH = isHeadExtended() ? m.getShrinkage(headNodeId) : EXTEND_0;

        double w2x = DBMath.roundShapeCoord(w2*GenMath.cos(angle));
        double w2y = DBMath.roundShapeCoord(w2*GenMath.sin(angle));
        double tx = 0;
        double ty = 0;
        if (shrinkT == EXTEND_90) {
            tx = -w2x;
            ty = -w2y;
        } else if (shrinkT != EXTEND_0) {
            Point2D e = computeExtension(w2, -w2x, -w2y, angle >= 1800 ? angle - 1800 : angle + 1800, shrinkT);
            tx = e.getX();
            ty = e.getY();
        }
        double hx = 0;
        double hy = 0;
        if (shrinkH == EXTEND_90) {
            hx = w2x;
            hy = w2y;
        } else if (shrinkH != EXTEND_0) {
            Point2D e = computeExtension(w2, w2x, w2y, angle, shrinkH);
            hx = e.getX();
            hy = e.getY();
        }
        
        b.pushPoint(tailLocation, tx - w2y, ty + w2x);
        b.pushPoint(tailLocation, tx + w2y, ty - w2x);
        b.pushPoint(headLocation, hx + w2y, hy - w2x);
        b.pushPoint(headLocation, hx - w2y, hy + w2x);
        
        // somewhat simpler if rectangle is manhattan
        if (gridWidth != 0 && style.isOpened())
            b.pushPoint(tailLocation, tx - w2y, ty + w2x);
        b.pushPoly(style, layer);
    }
    
    /**
     * Generate shape of this ImmutableArcInst in easy case.
     * @param b AbstractShapeBuilder to generate to.
     * @return true if shape was generated.
     */
    public boolean genShapeEasy(AbstractShapeBuilder b) {
        if (!isEasyShape()) return false;
        Layer.Function.Set onlyTheseLayers = b.getOnlyTheseLayers();
        int[] intCoords = b.intCoords;
        if (gridFullWidth == 0) {
            Technology.ArcLayer primLayer = protoType.getArcLayer(0);
            Layer layer = primLayer.getLayer();
            if (onlyTheseLayers != null && onlyTheseLayers.contains(layer.getFunction())) return true;
            Poly.Type style = primLayer.getStyle();
            if (style == Poly.Type.FILLED) style = Poly.Type.OPENED;
            intCoords[0] = (int)tailLocation.getGridX();
            intCoords[1] = (int)tailLocation.getGridY();
            intCoords[2] = (int)headLocation.getGridX();
            intCoords[3] = (int)headLocation.getGridY();
            b.addIntLine(intCoords, style, primLayer.getLayer());
            return true;
        }
        boolean tailExtended = false;
        if (isTailExtended()) {
            short shrinkT = b.getMemoization().getShrinkage(tailNodeId);
            if (shrinkT == EXTEND_90)
                tailExtended = true;
            else if (shrinkT != EXTEND_0)
                return false;
        }
        boolean headExtended = false;
        if (isHeadExtended()) {
            short shrinkH = b.getMemoization().getShrinkage(headNodeId);
            if (shrinkH == EXTEND_90)
                headExtended = true;
            else if (shrinkH != EXTEND_0)
                return false;
        }
        for (int i = 0, n = protoType.getNumArcLayers(); i < n; i++) {
            Technology.ArcLayer primLayer = protoType.getArcLayer(i);
            Layer layer = primLayer.getLayer();
            assert primLayer.getStyle() == Poly.Type.FILLED;
            if (onlyTheseLayers != null && onlyTheseLayers.contains(layer.getFunction())) continue;
            makeGridBoxInt(b.intCoords, tailExtended, headExtended, gridFullWidth - (int)primLayer.getGridOffset());
            b.addIntBox(intCoords, layer);
        }
        return true;
    }
    
    /**
     * Generate bounds of this ImmutableArcInst in easy case.
     * @param m data to determine shrinkage.
     * @param intCoords integer coords to fill.
     * @return true if bounds were generated.
     */
    public boolean genBoundsEasy(CellBackup.Memoization m, int[] intCoords) {
        if (!isEasyShape()) return false;
        if (gridFullWidth == 0) {
            int x1 = (int)tailLocation.getGridX();
            int y1 = (int)tailLocation.getGridY();
            int x2 = (int)headLocation.getGridX();
            int y2 = (int)headLocation.getGridY();
            if (x1 <= x2) {
                intCoords[0] = x1;
                intCoords[2] = x2;
            } else {
                intCoords[0] = x2;
                intCoords[2] = x1;
            }
            if (y1 <= y2) {
                intCoords[1] = y1;
                intCoords[3] = y2;
            } else {
                intCoords[1] = y2;
                intCoords[3] = y1;
            }
        } else {
            boolean tailExtended = false;
            if (isTailExtended()) {
                short shrinkT = m.getShrinkage(tailNodeId);
                if (shrinkT == EXTEND_90)
                    tailExtended = true;
                else if (shrinkT != EXTEND_0)
                    return false;
            }
            boolean headExtended = false;
            if (isHeadExtended()) {
                short shrinkH = m.getShrinkage(headNodeId);
                if (shrinkH == EXTEND_90)
                    headExtended = true;
                else if (shrinkH != EXTEND_0)
                    return false;
            }
            makeGridBoxInt(intCoords, tailExtended, headExtended, gridFullWidth);
        }
        return true;
    }
    
	/**
	 * Method to fill in an AbstractShapeBuilder a polygon that describes this ImmutableArcInst in grid units.
	 * The polygon is described by its width, and style.
     * @param b shape builder.
	 * @param gridWidth the gridWidth of the Poly.
	 * @param style the style of the Poly.
	 */
    private void makeGridBoxInt(int[] intCoords, boolean tailExtended, boolean headExtended, int gridWidth) {
        // make the box
        int w2 = gridWidth >>> 1;
        assert w2 > 0;
        
        int et = tailExtended ? w2 : 0;
        int eh = headExtended ? w2 : 0;
        int x, y;
        assert intCoords.length == 4;
        switch (angle) {
            case 0:
                y = (int)tailLocation.getGridY();
                intCoords[0] = (int)tailLocation.getGridX() - et;
                intCoords[1] = y - w2;
                intCoords[2] = (int)headLocation.getGridX() + eh;
                intCoords[3] = y + w2;
                break;
            case 900:
                x = (int)tailLocation.getGridX();
                intCoords[0] = x - w2;
                intCoords[1] = (int)tailLocation.getGridY() - et;
                intCoords[2] = x + w2;
                intCoords[3] = (int)headLocation.getGridY() + eh;
                break;
            case 1800:
                y = (int)tailLocation.getGridY();
                intCoords[0] = (int)headLocation.getGridX() - eh;
                intCoords[1] = y - w2;
                intCoords[2] = (int)tailLocation.getGridX() + et;
                intCoords[3] = y + w2;
                break;
            case 2700:
                x = (int)tailLocation.getGridX();
                intCoords[0] = x - w2;
                intCoords[1] = (int)headLocation.getGridY() - eh;
                intCoords[2] = x + w2;
                intCoords[3] = (int)tailLocation.getGridY() + et;
                break;
            default:
                throw new AssertionError();
        }
    }
    
    /**
     * Computes extension vector of wire, 
     */
    private static Point2D computeExtension(int w2, double ix1, double iy1, int angle, short shrink) {
        if (shrink == EXTEND_90) return new Point2D.Double(ix1, iy1);
        if (shrink == EXTEND_0) return new Point2D.Double(0, 0);
        assert shrink >= EXTEND_ANY;
        int angle2 = (shrink - EXTEND_ANY) - angle;
        if (angle2 < 0)
            angle2 += 3600;
        double x1 = ix1;
        double y1 = iy1;
        double s1;
        if (y1 == 0) {
            s1 = x1;
            if (x1 == 0) return new Point2D.Double(0, 0);
            x1 = x1 > 0 ? 1 : -1;
        } else if (x1 == 0) {
            s1 = y1;
            y1 = y1 > 0 ? 1 : -1;
        } else {
            s1 = x1*x1 + y1*y1;
        }
        
        double x2 = DBMath.roundShapeCoord(w2*GenMath.cos(angle2));
        double y2 = DBMath.roundShapeCoord(w2*GenMath.sin(angle2));
        double s2;
        if (y2 == 0) {
            s2 = x2;
            if (x2 == 0) return new Point2D.Double(0, 0);
            x2 = x2 > 0 ? 1 : -1;
        } else if (x2 == 0) {
            s2 = y2;
            y2 = y2 > 0 ? 1 : -1;
        } else {
            s2 = x2*x2 + y2*y2;
        }
        
        double det = x1*y2 - y1*x2;
        if (det == 0) return new Point2D.Double(0, 0);
        double x = (x2*s1 + x1*s2)/det;
        double y = (y2*s1 + y1*s2)/det;
        x = DBMath.roundShapeCoord(x);
        y = DBMath.roundShapeCoord(y);
        x = x + iy1;
        y = y - ix1;
        if (det < 0) {
            x = -x;
            y = -y;
        }
        return new Point2D.Double(x, y);
    }
    
    void registerShrinkage(int shrinkageState[]) {
        // shrinkage
        if (getGridFullWidth() == 0) return;
        if (tailNodeId == headNodeId && tailPortId == headPortId) {
            // Fake register for full shrinkage
            registerArcEnd(shrinkageState, tailNodeId, (short)0, false, false);
            return;
        }
        boolean is90 = isManhattan();
        int tailAngle = angle < 1800 ? angle + 1800 : angle - 1800;
        registerArcEnd(shrinkageState, tailNodeId, (short)tailAngle, is90, isTailExtended());
        registerArcEnd(shrinkageState, headNodeId, angle, is90, isHeadExtended());
    }
    
    private static final int ANGLE_SHIFT = 12;
    private static final int ANGLE_MASK = (1 << ANGLE_SHIFT) - 1;
    private static final int ANGLE_DIAGONAL_MASK = 1 << (ANGLE_SHIFT*2);
    private static final int ANGLE_COUNT_SHIFT = ANGLE_SHIFT*2 + 1;
    
    private void registerArcEnd(int[] angles, int nodeId, short angle, boolean is90, boolean extended) {
        assert angle >= 0 && angle < 3600;
        int ang = angles[nodeId];
        if (extended) {
            int count = ang >>> ANGLE_COUNT_SHIFT;
            switch (count) {
                case 0:
                    ang |= angle;
                    ang += (1 << ANGLE_COUNT_SHIFT);
                    break;
                case 1:
                    ang |= (angle << ANGLE_SHIFT);
                    ang += (1 << ANGLE_COUNT_SHIFT);
                    break;
                case 2:
                    ang += (1 << ANGLE_COUNT_SHIFT);
                    break;
            }
            if (!is90)
                ang |= ANGLE_DIAGONAL_MASK;
        } else {
            ang |= (3 << ANGLE_COUNT_SHIFT);
        }
        angles[nodeId] = ang;
    }
    
    static short computeShrink(int angs) {
        boolean hasAny = (angs&ANGLE_DIAGONAL_MASK) != 0;
        int count = angs >>> ANGLE_COUNT_SHIFT;
        
        if (hasAny && count == 2) {
            int ang0 = angs & ANGLE_MASK;
            int ang1 = (angs >> ANGLE_SHIFT) & ANGLE_MASK;
            int da = ang0 > ang1 ? ang0 - ang1 : ang1 - ang0;
            if (da == 900 || da == 2700) return EXTEND_90;
            if (da == 1800) return EXTEND_0;
            if (900 < da && da < 2700) {
                int a = ang0 + ang1;
                if (a >= 3600)
                    a -= 3600;
                return (short)(EXTEND_ANY + a);
            }
        }
        return EXTEND_90;
    }
        
	/**
	 * Method to get the curvature radius on this ImmutableArcInst.
	 * The curvature (used in artwork and round-cmos technologies) lets an arc
	 * curve.
	 * @return the curvature radius on this ImmutableArcInst.
	 * Returns null if there is no curvature information.
	 */
    public Double getRadius() {
        Variable var = getVar(ARC_RADIUS);
        if (var == null) return null;
        
        // get the radius of the circle, check for validity
        Object obj = var.getObject();
        
        if (obj instanceof Double)
            return (Double)obj;
        if (obj instanceof Integer)
            return new Double(((Integer)obj).intValue() / 2000.0);
        return null;
	}
    
	/**
	 * when arcs are curved, the number of line segments will be
	 * between this value, and half of this value.
	 */
	private static final int MAXARCPIECES = 16;

	/**
     * Method to fill polygon "poly" with the outline in grid units of the curved arc in
     * this ImmutableArcInst whose width in grid units is "gridWidth".  The style of the polygon is set to "style".
     * If there is no curvature information in the arc, the routine returns null,
     * otherwise it returns the curved polygon.
     */
    public Poly curvedArcGridOutline(Poly.Type style, long gridWidth, long gridRadius) {
        CurvedArcOutlineBuilder outlineBuilder = new CurvedArcOutlineBuilder();
        if (!curvedArcGridOutline(outlineBuilder, gridWidth, gridRadius)) return null;
        outlineBuilder.pushPoly(style, null);
        return outlineBuilder.poly;
    }
    
    private static class CurvedArcOutlineBuilder extends AbstractShapeBuilder {
        Poly poly;
        
        @Override
        public void addDoublePoly(int numPoints, Poly.Type style, Layer layer) {
            Point2D[] points = new Point2D[numPoints];
            for (int i = 0; i < numPoints; i++)
                points[i] = new Point2D.Double(doubleCoords[i*2], doubleCoords[i*2 + 1]);
            poly = new Poly(points);
            poly.setStyle(style);
            poly.setLayer(layer);
        }
    
        @Override
        public void addIntLine(int[] coords, Poly.Type style, Layer layer) { throw new UnsupportedOperationException(); }
        @Override
        public void addIntBox(int[] coords, Layer layer) { throw new UnsupportedOperationException(); }
        
    }
    
	/**
     * Method to fill polygon "poly" with the outline in grid units of the curved arc in
     * this ImmutableArcInst whose width in grid units is "gridWidth".
     * If there is no curvature information in the arc, the routine returns false,
     * otherwise it returns the curved polygon.
     * @param b builder to fill points
     * @param gridWidth width in grid units.
     * @param gridRadius radius in grid units.
     * @return true if point were filled to the buuilder
     */
    public boolean curvedArcGridOutline(AbstractShapeBuilder b, long gridWidth, long gridRadius) {
        // get information about the curved arc
        long pureGridRadius = Math.abs(gridRadius);
        double gridLength = getGridLength();
        
        // see if the lambdaRadius can work with these arc ends
        if (pureGridRadius*2 < gridLength) return false;
        
        // determine the center of the circle
        Point2D [] centers = DBMath.findCenters(pureGridRadius, headLocation.gridMutable(), tailLocation.gridMutable());
        if (centers == null) return false;
        
        Point2D centerPt = centers[1];
        if (gridRadius < 0) {
            centerPt = centers[0];
        }
        double centerX = centerPt.getX();
        double centerY = centerPt.getY();
        
        // determine the base and range of angles
        int angleBase = DBMath.figureAngle(headLocation.getGridX() - centerX, headLocation.getGridY() - centerY);
        int angleRange = DBMath.figureAngle(tailLocation.getGridX() - centerX, tailLocation.getGridY() - centerY);
        angleRange -= angleBase;
        if (angleRange < 0) angleRange += 3600;
        
        // force the curvature to be the smaller part of a circle (used to determine this by the reverse-ends bit)
        if (angleRange > 1800) {
            angleBase += angleRange;
            if (angleBase < 0) angleBase += 3600;
            angleRange = 3600 - angleRange;
        }
        
        // determine the number of intervals to use for the arc
        int pieces = angleRange;
        while (pieces > MAXARCPIECES) pieces /= 2;
        if (pieces == 0) return false;
        
        // get the inner and outer radii of the arc
        double outerRadius = pureGridRadius + gridWidth / 2;
        double innerRadius = outerRadius - gridWidth;
        
        // fill the polygon
        for(int i=0; i<=pieces; i++) {
            int a = (angleBase + i * angleRange / pieces) % 3600;
            b.pushPoint(DBMath.cos(a) * innerRadius + centerX, DBMath.sin(a) * innerRadius + centerY);
        }
        for(int i=pieces; i>=0; i--) {
            int a = (angleBase + i * angleRange / pieces) % 3600;
            b.pushPoint(DBMath.cos(a) * outerRadius + centerX, DBMath.sin(a) * outerRadius + centerY);
        }
        return true;
    }
    
	/**
	 * Checks invariant of this ImmutableArcInst.
	 * @throws AssertionError if invariant is broken.
	 */
	public void check() {
        check(false);
        assert arcId >= 0;
		assert protoType != null;
		assert name != null;
        assert name.isValid() && !name.hasEmptySubnames();
        if (name.isTempname())
            assert name.getBasename() == BASENAME && !name.isBus();
        if (nameDescriptor != null)
            assert nameDescriptor.isDisplay() && !nameDescriptor.isCode() && !nameDescriptor.isParam();
        assert tailNodeId >= 0;
        assert tailPortId != null;
        assert tailLocation != null;
        assert headNodeId >= 0;
        assert headPortId != null;
        assert headLocation != null;
        assert gridFullWidth >= 0 && gridFullWidth >= protoType.getMaxLayerGridOffset() && (gridFullWidth&1) == 0;
        assert (flags & ~(DATABASE_FLAGS|EASY_MASK)) == 0;
        assert flags == updateEasyShape(protoType, gridFullWidth, tailLocation, headLocation, angle, getVars(), flags);
        if (isTailNegated())
            assert tailPortId instanceof PrimitivePort && ((PrimitivePort)tailPortId).isNegatable() && !protoType.getTechnology().isNoNegatedArcs();
        if (isHeadNegated())
            assert headPortId instanceof PrimitivePort && ((PrimitivePort)headPortId).isNegatable() && !protoType.getTechnology().isNoNegatedArcs();
        assert 0 <= angle && angle < 3600;
        if (!tailLocation.equals(headLocation))
            assert angle == GenMath.figureAngle(headLocation.getGridX() - tailLocation.getGridX(), headLocation.getGridY() - tailLocation.getGridY());
	}

	/**
	 * Method to compute the "userbits" to use for a given ArcInst.
	 * The "userbits" are a set of bits that describes constraints and other properties,
	 * and are stored in ELIB files.
	 * The negation, directionality, and end-extension must be converted.
	 * @return the "userbits" for that ArcInst.
	 */
	public int getElibBits()
	{
		int elibBits = 0;
        
        if (isRigid()) elibBits |= ELIB_FIXED;
        if (isFixedAngle()) elibBits |= ELIB_FIXANG;
        if (!isSlidable()) elibBits |= ELIB_CANTSLIDE;
        if (isHardSelect()) elibBits |= ELIB_HARDSELECTA;
        
		// adjust bits for extension
		if (!isHeadExtended() || !isTailExtended())
		{
			elibBits |= ELIB_NOEXTEND;
			if (isHeadExtended() != isTailExtended())
			{
				if (isTailExtended()) elibBits |= ELIB_NOTEND0;
				if (isHeadExtended()) elibBits |= ELIB_NOTEND1;
			}
		}
	
		// adjust bits for directionality
		if (isHeadArrowed() || isTailArrowed() || isBodyArrowed())
		{
			elibBits |= ELIB_ISDIRECTIONAL;
			if (isTailArrowed()) elibBits |= ELIB_REVERSEEND;
			if (!isHeadArrowed() && !isTailArrowed()) elibBits |= ELIB_NOTEND1;
		}

		// adjust bits for negation
        boolean normalEnd = (elibBits & ELIB_REVERSEEND) == 0;
		if (isTailNegated()) elibBits |= (normalEnd ? ELIB_ISTAILNEGATED : ELIB_ISHEADNEGATED);
		if (isHeadNegated()) elibBits |= (normalEnd ? ELIB_ISHEADNEGATED : ELIB_ISTAILNEGATED);
        
		int elibAngle = (angle + 5)/10;
		if (elibAngle >= 360) elibAngle -= 360;
        
        return elibBits | (elibAngle << ELIB_AANGLESH);
	}
	/**
	 * Method to convert ELIB userbits to database flags.
	 * The flags are a set of bits that describes constraints and other properties.
	 * and are stored in ELIB files.
	 * The negation, directionality, and end-extension must be converted.
	 * @param elibBits the disk userbits.
     * @return the database flags
	 */
	public static int flagsFromElib(int elibBits)
	{
        int newBits = 0;
        if ((elibBits & ELIB_FIXED) != 0) newBits |= RIGID.mask;
        if ((elibBits & ELIB_FIXANG) != 0) newBits |= FIXED_ANGLE.mask;
        if ((elibBits & ELIB_CANTSLIDE) == 0) newBits |= SLIDABLE.mask;
        if ((elibBits & ELIB_HARDSELECTA) != 0) newBits |= HARD_SELECT.mask;
	
		if ((elibBits&ELIB_ISTAILNEGATED) != 0)
		{
			newBits |= (elibBits&ELIB_REVERSEEND) == 0 ? TAIL_NEGATED.mask : HEAD_NEGATED.mask;
		}
		if ((elibBits&ELIB_ISHEADNEGATED) != 0)
		{
            newBits |= (elibBits&ELIB_REVERSEEND) == 0 ? HEAD_NEGATED.mask : TAIL_NEGATED.mask;
		}

		if ((elibBits&ELIB_NOEXTEND) != 0)
		{
			if ((elibBits&ELIB_NOTEND0) != 0) newBits |= TAIL_EXTENDED.mask;
			if ((elibBits&ELIB_NOTEND1) != 0) newBits |= HEAD_EXTENDED.mask;
		} else {
            newBits |= (TAIL_EXTENDED.mask | HEAD_EXTENDED.mask);
        }

		if ((elibBits&ELIB_ISDIRECTIONAL) != 0)
		{
            newBits |= BODY_ARROWED.mask;
			if ((elibBits&ELIB_REVERSEEND) == 0)
			{
				if ((elibBits&ELIB_NOTEND1) == 0) newBits |= HEAD_ARROWED.mask;
			} else
			{
				if ((elibBits&ELIB_NOTEND0) == 0) newBits |= TAIL_ARROWED.mask;
			}
		}
        return newBits;
	}
    
    /**
     * Get angle from ELIB user bits.
     * @param elibBits ELIB user bits.
     * @return tech specific bits.
     */
	public static int angleFromElib(int elibBits)
	{
        int angle = (elibBits & ELIB_AANGLE) >> ELIB_AANGLESH;
        return (angle % 360)*10;
	}
}

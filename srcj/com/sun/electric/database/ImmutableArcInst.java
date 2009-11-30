/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableArcInst.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
import com.sun.electric.database.id.ArcProtoId;
import com.sun.electric.database.id.ExportId;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.id.PortProtoId;
import com.sun.electric.database.id.PrimitivePortId;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;

import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import java.io.IOException;

/**
 * Immutable class ImmutableArcInst represents an arc instance.
 */
public class ImmutableArcInst extends ImmutableElectricObject {

    /** The index of the tail of this ArcInst. */
    public static final int TAILEND = 0;
    /** The index of the head of this ArcInst. */
    public static final int HEADEND = 1;
    /** Key of Varible holding arc curvature. */
    public static final Variable.Key ARC_RADIUS = Variable.newKey("ARC_radius");
    /** Maximal extend of arc over minimal-width arc */
    private static final int MAX_EXTEND = Integer.MAX_VALUE / 8;

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
    /** fixed-length arc */
    private static final int ELIB_FIXED = 01;
    /** fixed-angle arc */
    private static final int ELIB_FIXANG = 02;
//	/** arc has text that is far away */                    private static final int AHASFARTEXT =               04;
//	/** arc is not in use */                                private static final int DEADA =                    020;
    /** DISK: angle of arc from end 0 to end 1 */
    private static final int ELIB_AANGLE = 037740;
    /** DISK: bits of right shift for DISK_AANGLE field */
    private static final int ELIB_AANGLESH = 5;
//	/** set if arc is to be drawn shortened */              private static final int ASHORT =                040000;
    /** set if head end of ArcInst is negated */
    private static final int ELIB_ISHEADNEGATED = 0200000;
    /** DISK: set if ends do not extend by half width */
    private static final int ELIB_NOEXTEND = 0400000;
    /** set if tail end of ArcInst is negated */
    private static final int ELIB_ISTAILNEGATED = 01000000;
    /** DISK: set if arc aims from end 0 to end 1 */
    private static final int ELIB_ISDIRECTIONAL = 02000000;
    /** DISK: no extension/negation/arrows on end 0 */
    private static final int ELIB_NOTEND0 = 04000000;
    /** DISK: no extension/negation/arrows on end 1 */
    private static final int ELIB_NOTEND1 = 010000000;
    /** DISK: reverse extension/negation/arrow ends */
    private static final int ELIB_REVERSEEND = 020000000;
    /** set if arc can't slide around in ports */
    private static final int ELIB_CANTSLIDE = 040000000;
//	/** set if afixed arc was changed */                    private static final int FIXEDMOD =          0100000000;
//	/** only local arcinst re-drawing desired */            private static final int RELOCLA =          01000000000;
//	/**transparent arcinst re-draw is done */               private static final int RETDONA =          02000000000;
//	/** opaque arcinst re-draw is done */                   private static final int REODONA =          04000000000;
//	/** general flag for spreading and highlighting */      private static final int ARCFLAGBIT =      010000000000;
    /** set if hard to select */
    private static final int ELIB_HARDSELECTA = 020000000000;
    private static final int TAIL_ARROWED_MASK = 0x001;
    private static final int HEAD_ARROWED_MASK = 0x002;
    private static final int TAIL_EXTENDED_MASK = 0x004;
    private static final int HEAD_EXTENDED_MASK = 0x008;
    private static final int TAIL_NEGATED_MASK = 0x010;
    private static final int HEAD_NEGATED_MASK = 0x020;
    private static final int BODY_ARROWED_MASK = 0x040;
    private static final int RIGID_MASK = 0x080;
    private static final int FIXED_ANGLE_MASK = 0x100;
    private static final int SLIDABLE_MASK = 0x200;
    private static final int HARD_SELECT_MASK = 0x400;
    private static final int DATABASE_FLAGS = 0x7ff;
    private static final int MANHATTAN_MASK = 0x800;
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
    /** initial bits */
    public static final int DEFAULT_FLAGS = SLIDABLE.mask | HEAD_EXTENDED.mask | TAIL_EXTENDED.mask;
    /** initial factory bits: FIXED_ANGLE=SLIDABLE=TAIL_EXTENDED=HEAD_EXTENDED=true, others false */
    public static final int FACTORY_DEFAULT_FLAGS = FIXED_ANGLE.mask | SLIDABLE.mask | HEAD_EXTENDED.mask | TAIL_EXTENDED.mask;
    /** prefix for autonaming. */
    public static final Name BASENAME = Name.findName("net@0");
    public final static ImmutableArcInst[] NULL_ARRAY = {};
    public final static ImmutableArrayList<ImmutableArcInst> EMPTY_LIST = new ImmutableArrayList<ImmutableArcInst>(NULL_ARRAY);
    /** id of this ArcInst in parent. */
    public final int arcId;
    /** Arc prototype. */
    public final ArcProtoId protoId;
    /** name of this ImmutableArcInst. */
    public final Name name;
    /** The text descriptor of name of ImmutableArcInst. */
    public final TextDescriptor nameDescriptor;
    /** NodeId on tail end of this ImmutableArcInst. */
    public final int tailNodeId;
    /** PortProtoId on tail end of this ImmutableArcInst. */
    public final PortProtoId tailPortId;
    /** Location of tail end of this ImmutableArcInst. */
    public final EPoint tailLocation;
    /** NodeId on head end of this ImmutableArcInst. */
    public final int headNodeId;
    /** PortProtoId on head end of this ImmutableArcInst. */
    public final PortProtoId headPortId;
    /** Location of head end of this ImmutableArcInst. */
    public final EPoint headLocation;
    /** extend of this ImmutableArcInst over minimal-width arc in grid units. */
    private final int gridExtendOverMin;
    /** Angle if this ImmutableArcInst (in tenth-degrees). */
    private final short angle;

    /**
     * The private constructor of ImmutableArcInst. Use the factory "newInstance" instead.
     *
     * @param arcId id of this ArcInst in parent.
     * @param protoId Id pf arc prototype.
     * @param name name of this ImmutableArcInst.
     * @param nameDescriptor TextDescriptor of name of this ImmutableArcInst.
     * @param tailNodeId NodeId on tail end of this ImmutableArcInst.
     * @param tailPortProtoId PortProtoId on tail end of this ImmutableArcInst.
     * @param tailLocation Location of tail end of this ImmutableArcInst.
     * @param headNodeId NodeId on head end of this ImmutableArcInst.
     * @param headPortProtoId PortProtoId on head end of this ImmutableArcInst.
     * @param headLocation Location of head end of this ImmutableArcInst.
     * @param gridExtendOverMin the extend of this ImmutableArcInst over minimal-width arc of this type in grid units.
     * @param angle the angle if this ImmutableArcInst (in tenth-degrees).
     * @param flags flag bits of this ImmutableArcInst.
     * @param vars array of Variables of this ImmutableArcInst
     */
    ImmutableArcInst(int arcId, ArcProtoId protoId, Name name, TextDescriptor nameDescriptor,
            int tailNodeId, PortProtoId tailPortId, EPoint tailLocation,
            int headNodeId, PortProtoId headPortId, EPoint headLocation,
            int gridExtendOverMin, short angle, int flags, Variable[] vars) {
        super(vars, flags);
        this.arcId = arcId;
        this.protoId = protoId;
        this.name = name;
        this.nameDescriptor = nameDescriptor;
        this.tailNodeId = tailNodeId;
        this.tailPortId = tailPortId;
        this.tailLocation = tailLocation;
        this.headNodeId = headNodeId;
        this.headPortId = headPortId;
        this.headLocation = headLocation;
        this.gridExtendOverMin = gridExtendOverMin;
        this.angle = angle;
        check();
    }

    /**
     * Retruns true if this ImmutableArcInst was named by user.
     * @return true if this ImmutableArcInst was named by user.
     */
    public boolean isUsernamed() {
        return !name.isTempname();
    }

    /**
     * Returns extend of this ImmutableArcInst over minimal-width arc of this type in lambda units.
     * @return extend of this ImmutableArcInst over minimal-width arc of this type in lambda units.
     */
    public double getLambdaExtendOverMin() {
        return DBMath.gridToLambda(getGridExtendOverMin());
    }

    /**
     * Returns extend of this ImmutableArcInst over minimal-width arc of this type in grid units.
     * @return extend of this ImmutableArcInst over minimal-width arc of this type in grid units.
     */
    public long getGridExtendOverMin() {
        return gridExtendOverMin;
    }

    /**
     * Returns length of this ImmutableArcInst in lambda units.
     * @return length of this ImmutableArcInst in lambda units.
     */
    public double getLambdaLength() {
        return tailLocation.lambdaDistance(headLocation);
    }

    /**
     * Returns length of this ImmutableArcInst in grid units.
     * @return length of this ImmutableArcInst in grid units.
     */
    public double getGridLength() {
        return tailLocation.gridDistance(headLocation);
    }

    /**
     * Returns true if length of this ImmutableArcInst is zero.
     * @return true if length of this ImmutableArcInst is zero.
     */
    public boolean isZeroLength() {
        return tailLocation.equals(headLocation);
    }

    /**
     * Method to return the rotation angle of this ImmutableArcInst.
     * This is an angle of direction from tailLocation to headLocation.
     * @return the rotation angle of this ImmutableArcInst (in tenth-degrees).
     */
    public int getAngle() {
        return angle;
    }

    /**
     * Method to return the opposite rotation angle of this ImmutableArcInst.
     * This is an angle of direction from headLocation to tailLocation.
     * @return the opposite rotation angle of this ImmutableArcInst (in tenth-degrees).
     */
    public int getOppositeAngle() {
        return angle >= 1800 ? angle - 1800 : angle + 1800;
    }

    /**
     * Tests specific flag is set on this ImmutableArcInst.
     * @param flag flag selector.
     * @return true if specific flag is set,
     */
    public boolean is(Flag flag) {
        return (flags & flag.mask) != 0;
    }

    /**
     * Method to tell whether this ImmutableArcInst is rigid.
     * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
     * @return true if this ImmutableArcInst is rigid.
     */
    public boolean isRigid() {
        return (flags & RIGID_MASK) != 0;
    }

    /**
     * Method to tell whether this ImmutableArcInst is fixed-angle.
     * Fixed-angle arcs cannot change their angle, so if one end moves,
     * the other may also adjust to keep the arc angle constant.
     * @return true if this ImmutableArcInst is fixed-angle.
     */
    public boolean isFixedAngle() {
        return (flags & FIXED_ANGLE_MASK) != 0;
    }

    /**
     * Method to tell whether this ImmutableArcInst is slidable.
     * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
     * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
     * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
     * @return true if this ImmutableArcInst is slidable.
     */
    public boolean isSlidable() {
        return (flags & SLIDABLE_MASK) != 0;
    }

    /**
     * Method to tell whether this ArcInst is hard-to-select.
     * Hard-to-select ArcInsts cannot be selected by clicking on them.
     * Instead, the "special select" command must be given.
     * @return true if this ArcInst is hard-to-select.
     */
    public boolean isHardSelect() {
        return (flags & HARD_SELECT_MASK) != 0;
    }

    /****************************** PROPERTIES ******************************/
    /**
     * Method to determine whether this ImmutableArcInst is directional, with an arrow on one end.
     * Directional arcs have an arrow drawn on them to indicate flow.
     * It is only for documentation purposes and does not affect the circuit.
     * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
     * @return true if that end has a directional arrow on it.
     */
    public boolean isArrowed(int connIndex) {
        if ((connIndex & ~1) != 0) {
            throw new IllegalArgumentException("Bad end " + connIndex);
        }
        return ((flags >> connIndex) & TAIL_ARROWED_MASK) != 0;
    }

    /**
     * Method to determine whether this ImmutableArcInst is directional, with an arrow on the tail.
     * Directional arcs have an arrow drawn on them to indicate flow.
     * It is only for documentation purposes and does not affect the circuit.
     * @return true if the arc's tail has a directional arrow on it.
     */
    public boolean isTailArrowed() {
        return (flags & TAIL_ARROWED_MASK) != 0;
    }

    /**
     * Method to determine whether this ImmutableArcInst is directional, with an arrow on the head.
     * Directional arcs have an arrow drawn on them to indicate flow.
     * It is only for documentation purposes and does not affect the circuit.
     * @return true if the arc's head has a directional arrow on it.
     */
    public boolean isHeadArrowed() {
        return (flags & HEAD_ARROWED_MASK) != 0;
    }

    /**
     * Method to determine whether this ArcInst is directional, with an arrow line drawn down the center.
     * Directional arcs have an arrow drawn on them to indicate flow.
     * It is only for documentation purposes and does not affect the circuit.
     * The body is typically drawn when one of the ends has an arrow on it, but it may be
     * drawin without an arrow head in order to continue an attached arc that has an arrow.
     * @return true if the arc's tail has an arrow line on it.
     */
    public boolean isBodyArrowed() {
        return (flags & BODY_ARROWED_MASK) != 0;
    }

    /**
     * Method to tell whether an end of ImmutableArcInst has its ends extended.
     * Extended arcs continue past their endpoint by half of their width.
     * Most layout arcs want this so that they make clean connections to orthogonal arcs.
     * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
     * @return true if that end of this ArcInst iss extended.
     */
    public boolean isExtended(int connIndex) {
        if ((connIndex & ~1) != 0) {
            throw new IllegalArgumentException("Bad end " + connIndex);
        }
        return ((flags >> connIndex) & TAIL_EXTENDED_MASK) != 0;
    }

    /**
     * Method to tell whether the tail of this arc is extended.
     * Extended arcs continue past their endpoint by half of their width.
     * Most layout arcs want this so that they make clean connections to orthogonal arcs.
     * @return true if the tail of this arc is extended.
     */
    public boolean isTailExtended() {
        return (flags & TAIL_EXTENDED_MASK) != 0;
    }

    /**
     * Method to tell whether the head of this arc is extended.
     * Extended arcs continue past their endpoint by half of their width.
     * Most layout arcs want this so that they make clean connections to orthogonal arcs.
     * @return true if the head of this arc is extended.
     */
    public boolean isHeadExtended() {
        return (flags & HEAD_EXTENDED_MASK) != 0;
    }

    /**
     * Method to tell whether an end of this arc is negated.
     * Negated arc have a negating bubble on them to indicate negation.
     * This is only valid in schematics technologies.
     * @param connIndex TAILEND (0) for the tail of this ArcInst, HEADEND (1) for the head.
     * @return true if set that end of this arc is negated.
     */
    public boolean isNegated(int connIndex) {
        if ((connIndex & ~1) != 0) {
            throw new IllegalArgumentException("Bad end " + connIndex);
        }
        return ((flags >> connIndex) & TAIL_NEGATED_MASK) != 0;
    }

    /**
     * Method to tell whether the tail of this arc is negated.
     * Negated arc have a negating bubble on them to indicate negation.
     * This is only valid in schematics technologies.
     * @return true if set the tail of this arc is negated.
     */
    public boolean isTailNegated() {
        return (flags & TAIL_NEGATED_MASK) != 0;
    }

    /**
     * Method to tell whether the head of this arc is negated.
     * Negated arc have a negating bubble on them to indicate negation.
     * This is only valid in schematics technologies.
     * @return true if set the head of this arc is negated.
     */
    public boolean isHeadNegated() {
        return (flags & HEAD_NEGATED_MASK) != 0;
    }

    /**
     * Returns true if this ImmutableArcInst is either horizontal or vertical.
     * @return true if this ImmutableArcInst is either horizontal or vertical.
     */
    public boolean isManhattan() {
        return (flags & MANHATTAN_MASK) != 0;
    }

    private static int updateManhattan(int flags, EPoint headLocation, EPoint tailLocation, int angle) {
        return isManhattan(headLocation, tailLocation, angle) ? flags | MANHATTAN_MASK : flags & ~MANHATTAN_MASK;
    }

    private static boolean isManhattan(EPoint headLocation, EPoint tailLocation, int angle) {
        if (headLocation.getGridX() == tailLocation.getGridX()) {
            return headLocation.getGridY() != tailLocation.getGridY()
                    || (angle == 0 || angle == 900 || angle == 1800 || angle == 2700);
        } else {
            return tailLocation.getGridY() == headLocation.getGridY();
        }
    }

    /**
     * Returns new ImmutableArcInst object.
     * @param arcId id of this ArcInst in parent.
     * @param protoId Id of arc prototype.
     * @param name name of this ImmutableArcInst.
     * @param nameDescriptor TextDescriptor of name of this ImmutableArcInst.
     * @param tailNodeId NodeId on tail end of this ImmutableArcInst.
     * @param tailPortId PortProtoId on tail end of this ImmutableArcInst.
     * @param tailLocation Location of tail end of this ImmutableArcInst.
     * @param headNodeId NodeId on head end of this ImmutableArcInst.
     * @param headPortId PortProtoId on head end of this ImmutableArcInst.
     * @param headLocation Location of head end of this ImmutableArcInst.
     * @param gridExtendOverMin the extend of this ImmutableArcInst over minimal-width arc of this type in grid units.
     * @param angle the angle if this ImmutableArcInst (in tenth-degrees).
     * @param flags flag bits of this ImmutableNodeInst.
     * @return new ImmutableArcInst object.
     * @throws NullPointerException if protoType, name, tailPortId, headPortId, tailLocation, headLocation is null.
     * @throws IllegalArgumentException if arcId, tailNodeId, headNodeId or name is not valid, or width is bad.
     */
    public static ImmutableArcInst newInstance(int arcId, ArcProtoId protoId, Name name, TextDescriptor nameDescriptor,
            int tailNodeId, PortProtoId tailPortId, EPoint tailLocation,
            int headNodeId, PortProtoId headPortId, EPoint headLocation,
            long gridExtendOverMin, int angle, int flags) {
        if (arcId < 0) {
            throw new IllegalArgumentException("arcId");
        }
        if (protoId == null) {
            throw new NullPointerException("protoId");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (!name.isValid() || name.hasEmptySubnames()
                || name.isTempname() && (name.getBasename() != BASENAME || name.isBus())) {
            throw new IllegalArgumentException("name");
        }
        if (nameDescriptor != null) {
            nameDescriptor = nameDescriptor.withDisplayWithoutParam();
        }
        if (tailNodeId < 0) {
            throw new IllegalArgumentException("tailNodeId");
        }
        if (tailPortId == null) {
            throw new NullPointerException("tailPortId");
        }
        if (tailLocation == null) {
            throw new NullPointerException("tailLocation");
        }
        if (headNodeId < 0) {
            throw new IllegalArgumentException("headNodeId");
        }
        if (headPortId == null) {
            throw new NullPointerException("headPortId");
        }
        if (headLocation == null) {
            throw new NullPointerException("headLocation");
        }
        if (gridExtendOverMin <= -MAX_EXTEND || gridExtendOverMin >= MAX_EXTEND) {
            throw new IllegalArgumentException("gridExtendOverMin");
        }
        int intGridExtendOverMin = (int) gridExtendOverMin;
        angle %= 3600;
        if (angle < 0) {
            angle += 3600;
        }
        short shortAngle = updateAngle((short) angle, tailLocation, headLocation);
        flags &= DATABASE_FLAGS;
        if (!(tailPortId instanceof PrimitivePortId)) {
            flags &= ~TAIL_NEGATED_MASK;
        }
        if (!(headPortId instanceof PrimitivePortId)) {
            flags &= ~HEAD_NEGATED_MASK;
        }
        flags = updateManhattan(flags, headLocation, tailLocation, angle);
        return new ImmutableArcInst(arcId, protoId, name, nameDescriptor,
                tailNodeId, tailPortId, tailLocation,
                headNodeId, headPortId, headLocation,
                intGridExtendOverMin, shortAngle, flags, Variable.NULL_ARRAY);
    }

    /**
     * Returns ImmutableArcInst which differs from this ImmutableArcInst by name.
     * @param name node name key.
     * @return ImmutableArcInst which differs from this ImmutableArcInst by name.
     * @throws NullPointerException if name is null
     * @throws IllegalArgumentException if name is not valid.
     */
    public ImmutableArcInst withName(Name name) {
        if (this.name.toString().equals(name.toString())) {
            return this;
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (!name.isValid() || name.hasEmptySubnames() || name.isTempname() && (name.getBasename() != BASENAME || name.isBus())) {
            throw new IllegalArgumentException("name");
        }
        return new ImmutableArcInst(this.arcId, this.protoId, name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridExtendOverMin, this.angle, this.flags, getVars());
    }

    /**
     * Returns ImmutableArcInst which differs from this ImmutableArcInst by name descriptor.
     * @param nameDescriptor TextDescriptor of name
     * @return ImmutableArcInst which differs from this ImmutableArcInst by name descriptor.
     */
    public ImmutableArcInst withNameDescriptor(TextDescriptor nameDescriptor) {
        if (nameDescriptor != null) {
            nameDescriptor = nameDescriptor.withDisplayWithoutParam();
        }
        if (this.nameDescriptor == nameDescriptor) {
            return this;
        }
        return new ImmutableArcInst(this.arcId, this.protoId, this.name, nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridExtendOverMin, this.angle, this.flags, getVars());
    }

    /**
     * Returns ImmutableArcInst which differs from this ImmutableArcInst by tailLocation and headLocation.
     * @param tailLocation new tail location.
     * @param headLocation new head location.
     * @return ImmutableArcInst which differs from this ImmutableArcInst by tailLocation and headLocation.
     * @throws NullPointerException if tailLocation is null.
     */
    public ImmutableArcInst withLocations(EPoint tailLocation, EPoint headLocation) {
        if (this.tailLocation.equals(tailLocation) && this.headLocation.equals(headLocation)) {
            return this;
        }
        if (tailLocation == null) {
            throw new NullPointerException("tailLocation");
        }
        if (headLocation == null) {
            throw new NullPointerException("headLocation");
        }
        short angle = updateAngle(this.angle, tailLocation, headLocation);
        int flags = updateManhattan(this.flags, headLocation, tailLocation, angle);
        return new ImmutableArcInst(this.arcId, this.protoId, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, tailLocation,
                this.headNodeId, this.headPortId, headLocation,
                this.gridExtendOverMin, angle, flags, getVars());
    }

    /**
     * Returns ImmutableArcInst which differs from this ImmutableArcInst by width.
     * @param gridExtendOverMin extend of this arc over minimal arc of this type in grid units.
     * @return ImmutableArcInst which differs from this ImmutableArcInst by width.
     * @throws IllegalArgumentException if gridExtendOverMin is negative.
     */
    public ImmutableArcInst withGridExtendOverMin(long gridExtendOverMin) {
        if (this.gridExtendOverMin == gridExtendOverMin) {
            return this;
        }
        if (gridExtendOverMin <= -MAX_EXTEND || gridExtendOverMin >= MAX_EXTEND) {
            throw new IllegalArgumentException("gridWidth");
        }
        return new ImmutableArcInst(this.arcId, this.protoId, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                (int) gridExtendOverMin, this.angle, this.flags, getVars());
    }

    /**
     * Returns ImmutableArcInst which differs from this ImmutableArcInst by angke.
     * For arc with non-zero length returns ths ImmutableArcInst
     * @param angle angle in tenth-degrees.
     * @return ImmutableArcInst which differs from this ImmutableArcInst by user bits.
     */
    public ImmutableArcInst withAngle(int angle) {
        if (!tailLocation.equals(headLocation)) {
            return this;
        }
        angle %= 3600;
        if (angle < 0) {
            angle += 3600;
        }
        if (this.angle == angle) {
            return this;
        }
        short shortAngle = (short) angle;
        int flags = updateManhattan(this.flags, this.headLocation, this.tailLocation, shortAngle);
        return new ImmutableArcInst(this.arcId, this.protoId, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridExtendOverMin, shortAngle, flags, getVars());
    }

    /**
     * Returns ImmutableArcInst which differs from this ImmutableArcInst by user bits.
     * @param flags flag bits of this ImmutableArcInst.
     * @return ImmutableArcInst which differs from this ImmutableArcInst by user bits.
     */
    public ImmutableArcInst withFlags(int flags) {
        flags &= DATABASE_FLAGS;
        if (!(tailPortId instanceof PrimitivePortId)) {
            flags &= ~TAIL_NEGATED_MASK;
        }
        if (!(headPortId instanceof PrimitivePortId)) {
            flags &= ~HEAD_NEGATED_MASK;
        }
        if ((this.flags & DATABASE_FLAGS) == flags) {
            return this;
        }
        flags |= this.flags & MANHATTAN_MASK;
        return new ImmutableArcInst(this.arcId, this.protoId, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridExtendOverMin, this.angle, flags, getVars());
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
        Variable[] vars = arrayWithVariable(var.withParam(false).withInherit(false));
        if (this.getVars() == vars) {
            return this;
        }
        return new ImmutableArcInst(this.arcId, this.protoId, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridExtendOverMin, this.angle, this.flags, vars);
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
        if (this.getVars() == vars) {
            return this;
        }
        return new ImmutableArcInst(this.arcId, this.protoId, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridExtendOverMin, this.angle, this.flags, vars);
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
        if (tailPortId instanceof ExportId) {
            tailPortId = idMapper.get((ExportId) tailPortId);
        }
        if (headPortId instanceof ExportId) {
            headPortId = idMapper.get((ExportId) headPortId);
        }
        if (getVars() == vars && this.tailPortId == tailPortId && this.headPortId == headPortId) {
            return this;
        }
        return new ImmutableArcInst(this.arcId, this.protoId, this.name, this.nameDescriptor,
                this.tailNodeId, tailPortId, this.tailLocation,
                this.headNodeId, headPortId, this.headLocation,
                this.gridExtendOverMin, this.angle, this.flags, vars);
    }

    private static short updateAngle(short angle, EPoint tailLocation, EPoint headLocation) {
        if (tailLocation.equals(headLocation)) {
            return angle;
        }
        return (short) GenMath.figureAngle(headLocation.getGridX() - tailLocation.getGridX(), headLocation.getGridY() - tailLocation.getGridY());
    }

    /**
     * Writes this ImmutableArcInst to IdWriter.
     * @param writer where to write.
     */
    void write(IdWriter writer) throws IOException {
        writer.writeArcId(arcId);
        writer.writeArcProtoId(protoId);
        writer.writeNameKey(name);
        writer.writeTextDescriptor(nameDescriptor);
        writer.writeNodeId(tailNodeId);
        writer.writePortProtoId(tailPortId);
        writer.writePoint(tailLocation);
        writer.writeNodeId(headNodeId);
        writer.writePortProtoId(headPortId);
        writer.writePoint(headLocation);
        writer.writeInt(gridExtendOverMin);
        writer.writeShort(angle);
        writer.writeInt(flags);
        super.write(writer);
    }

    /**
     * Reads ImmutableArcInst from SnapshotReader.
     * @param reader where to read.
     */
    static ImmutableArcInst read(IdReader reader) throws IOException {
        int arcId = reader.readNodeId();
        ArcProtoId protoId = reader.readArcProtoId();
        Name name = reader.readNameKey();
        TextDescriptor nameDescriptor = reader.readTextDescriptor();
        int tailNodeId = reader.readNodeId();
        PortProtoId tailPortId = reader.readPortProtoId();
        EPoint tailLocation = reader.readPoint();
        int headNodeId = reader.readNodeId();
        PortProtoId headPortId = reader.readPortProtoId();
        EPoint headLocation = reader.readPoint();
        int gridExtendOverMin = reader.readInt();
        short angle = reader.readShort();
        int flags = reader.readInt();
        boolean hasVars = reader.readBoolean();
        Variable[] vars = hasVars ? readVars(reader) : Variable.NULL_ARRAY;
        return new ImmutableArcInst(arcId, protoId, name, nameDescriptor,
                tailNodeId, tailPortId, tailLocation, headNodeId, headPortId, headLocation, gridExtendOverMin,
                updateAngle(angle, tailLocation, headLocation), flags, vars);
    }

    /**
     * Return a hash code value for fields of this object.
     * Variables of objects are not compared
     */
    public int hashCodeExceptVariables() {
        return arcId;
    }

    /**
     * Indicates whether fields of other ImmutableElectricObject are equal to fileds of this object.
     * Variables of objects are not compared.
     * @param o other ImmutableElectricObject.
     * @return true if fields of objects are equal.
     */
    public boolean equalsExceptVariables(ImmutableElectricObject o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ImmutableArcInst)) {
            return false;
        }
        ImmutableArcInst that = (ImmutableArcInst) o;
        return this.arcId == that.arcId && this.protoId == that.protoId
                && this.name == that.name && this.nameDescriptor == that.nameDescriptor
                && this.tailNodeId == that.tailNodeId && this.tailPortId == that.tailPortId && this.tailLocation == that.tailLocation
                && this.headNodeId == that.headNodeId && this.headPortId == that.headPortId && this.headLocation == that.headLocation
                && this.gridExtendOverMin == that.gridExtendOverMin && this.angle == that.angle && this.flags == that.flags;
    }

    /**
     * Method to fill in an AbstractShapeBuilder a polygon that describes this ImmutableArcInst in grid units.
     * The polygon is described by its width, and style.
     */
    public void makeGridBoxInt(int[] intCoords, boolean tailExtended, boolean headExtended, int gridExtend) {
        // make the box
        int w2 = gridExtend;
        assert w2 > 0;

        int et = tailExtended ? w2 : 0;
        int eh = headExtended ? w2 : 0;
        int x, y;
        assert intCoords.length == 4;
        switch (angle) {
            case 0:
                y = (int) tailLocation.getGridY();
                intCoords[0] = (int) tailLocation.getGridX() - et;
                intCoords[1] = y - w2;
                intCoords[2] = (int) headLocation.getGridX() + eh;
                intCoords[3] = y + w2;
                break;
            case 900:
                x = (int) tailLocation.getGridX();
                intCoords[0] = x - w2;
                intCoords[1] = (int) tailLocation.getGridY() - et;
                intCoords[2] = x + w2;
                intCoords[3] = (int) headLocation.getGridY() + eh;
                break;
            case 1800:
                y = (int) tailLocation.getGridY();
                intCoords[0] = (int) headLocation.getGridX() - eh;
                intCoords[1] = y - w2;
                intCoords[2] = (int) tailLocation.getGridX() + et;
                intCoords[3] = y + w2;
                break;
            case 2700:
                x = (int) tailLocation.getGridX();
                intCoords[0] = x - w2;
                intCoords[1] = (int) headLocation.getGridY() - eh;
                intCoords[2] = x + w2;
                intCoords[3] = (int) tailLocation.getGridY() + et;
                break;
            default:
                throw new AssertionError();
        }
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
        if (var == null) {
            return null;
        }

        // get the radius of the circle, check for validity
        Object obj = var.getObject();

        if (obj instanceof Double) {
            return (Double) obj;
        }
        if (obj instanceof Integer) {
            return new Double(((Integer) obj).intValue() / 2000.0);
        }
        return null;
    }

    public boolean check(TechPool techPool) {
        ArcProto protoType = techPool.getArcProto(protoId);
        if (protoType == null) {
            return false;
        }
        if (name.isBus()) {
            Technology tech = protoType.getTechnology();
            if (!(tech instanceof Schematics) || protoType != ((Schematics)tech).bus_arc)
                return false;
        }
        if (isTailNegated()) {
            if (!techPool.getPrimitivePort((PrimitivePortId) tailPortId).isNegatable()) {
                return false;
            }
            if (protoType.getTechnology().isNoNegatedArcs()) {
                return false;
            }
        }
        if (isHeadNegated()) {
            if (!techPool.getPrimitivePort((PrimitivePortId) headPortId).isNegatable()) {
                return false;
            }
            if (protoType.getTechnology().isNoNegatedArcs()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks invariant of this ImmutableArcInst.
     * @throws AssertionError if invariant is broken.
     */
    public void check() {
        super.check(false);
        assert arcId >= 0;
        assert protoId != null;
        assert name != null;
        assert name.isValid() && !name.hasEmptySubnames();
        if (name.isTempname()) {
            assert name.getBasename() == BASENAME && !name.isBus();
        }
        if (nameDescriptor != null) {
            assert nameDescriptor.isDisplay() && !nameDescriptor.isParam();
        }
        assert tailNodeId >= 0;
        assert tailPortId != null;
        assert tailLocation != null;
        assert headNodeId >= 0;
        assert headPortId != null;
        assert headLocation != null;
        assert -MAX_EXTEND < gridExtendOverMin && gridExtendOverMin < MAX_EXTEND;
        assert (flags & ~(DATABASE_FLAGS | MANHATTAN_MASK)) == 0;
        assert isManhattan() == isManhattan(headLocation, tailLocation, angle);
        if (isTailNegated()) {
            assert tailPortId instanceof PrimitivePortId;
        }
        if (isHeadNegated()) {
            assert headPortId instanceof PrimitivePortId;
        }
        assert 0 <= angle && angle < 3600;
        if (!tailLocation.equals(headLocation)) {
            assert angle == GenMath.figureAngle(headLocation.getGridX() - tailLocation.getGridX(), headLocation.getGridY() - tailLocation.getGridY());
        }
    }

    /**
     * Method to compute the "userbits" to use for a given ArcInst.
     * The "userbits" are a set of bits that describes constraints and other properties,
     * and are stored in ELIB files.
     * The negation, directionality, and end-extension must be converted.
     * @return the "userbits" for that ArcInst.
     */
    public int getElibBits() {
        int elibBits = 0;

        if (isRigid()) {
            elibBits |= ELIB_FIXED;
        }
        if (isFixedAngle()) {
            elibBits |= ELIB_FIXANG;
        }
        if (!isSlidable()) {
            elibBits |= ELIB_CANTSLIDE;
        }
        if (isHardSelect()) {
            elibBits |= ELIB_HARDSELECTA;
        }

        // adjust bits for extension
        if (!isHeadExtended() || !isTailExtended()) {
            elibBits |= ELIB_NOEXTEND;
            if (isHeadExtended() != isTailExtended()) {
                if (isTailExtended()) {
                    elibBits |= ELIB_NOTEND0;
                }
                if (isHeadExtended()) {
                    elibBits |= ELIB_NOTEND1;
                }
            }
        }

        // adjust bits for directionality
        if (isHeadArrowed() || isTailArrowed() || isBodyArrowed()) {
            elibBits |= ELIB_ISDIRECTIONAL;
            if (isTailArrowed()) {
                elibBits |= ELIB_REVERSEEND;
            }
            if (!isHeadArrowed() && !isTailArrowed()) {
                elibBits |= ELIB_NOTEND1;
            }
        }

        // adjust bits for negation
        boolean normalEnd = (elibBits & ELIB_REVERSEEND) == 0;
        if (isTailNegated()) {
            elibBits |= (normalEnd ? ELIB_ISTAILNEGATED : ELIB_ISHEADNEGATED);
        }
        if (isHeadNegated()) {
            elibBits |= (normalEnd ? ELIB_ISHEADNEGATED : ELIB_ISTAILNEGATED);
        }

        int elibAngle = (angle + 5) / 10;
        if (elibAngle >= 360) {
            elibAngle -= 360;
        }

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
    public static int flagsFromElib(int elibBits) {
        int newBits = 0;
        if ((elibBits & ELIB_FIXED) != 0) {
            newBits |= RIGID.mask;
        }
        if ((elibBits & ELIB_FIXANG) != 0) {
            newBits |= FIXED_ANGLE.mask;
        }
        if ((elibBits & ELIB_CANTSLIDE) == 0) {
            newBits |= SLIDABLE.mask;
        }
        if ((elibBits & ELIB_HARDSELECTA) != 0) {
            newBits |= HARD_SELECT.mask;
        }

        if ((elibBits & ELIB_ISTAILNEGATED) != 0) {
            newBits |= (elibBits & ELIB_REVERSEEND) == 0 ? TAIL_NEGATED.mask : HEAD_NEGATED.mask;
        }
        if ((elibBits & ELIB_ISHEADNEGATED) != 0) {
            newBits |= (elibBits & ELIB_REVERSEEND) == 0 ? HEAD_NEGATED.mask : TAIL_NEGATED.mask;
        }

        if ((elibBits & ELIB_NOEXTEND) != 0) {
            if ((elibBits & ELIB_NOTEND0) != 0) {
                newBits |= TAIL_EXTENDED.mask;
            }
            if ((elibBits & ELIB_NOTEND1) != 0) {
                newBits |= HEAD_EXTENDED.mask;
            }
        } else {
            newBits |= (TAIL_EXTENDED.mask | HEAD_EXTENDED.mask);
        }

        if ((elibBits & ELIB_ISDIRECTIONAL) != 0) {
            newBits |= BODY_ARROWED.mask;
            if ((elibBits & ELIB_REVERSEEND) == 0) {
                if ((elibBits & ELIB_NOTEND1) == 0) {
                    newBits |= HEAD_ARROWED.mask;
                }
            } else {
                if ((elibBits & ELIB_NOTEND0) == 0) {
                    newBits |= TAIL_ARROWED.mask;
                }
            }
        }
        return newBits;
    }

    /**
     * Get angle from ELIB user bits.
     * @param elibBits ELIB user bits.
     * @return tech specific bits.
     */
    public static int angleFromElib(int elibBits) {
        int angle = (elibBits & ELIB_AANGLE) >> ELIB_AANGLESH;
        return (angle % 360) * 10;
    }
}

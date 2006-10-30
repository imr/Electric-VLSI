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
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitivePort;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

/**
 * Immutable class ImmutableArcInst represents an arc instance.
 */
public class ImmutableArcInst extends ImmutableElectricObject {
	/** Key of Varible holding arc curvature. */		public static final Variable.Key ARC_RADIUS = Variable.newKey("ARC_radius");
    /** 
     * Class to access a flag in user bits of ImmutableNodeInst.
     */
    public static class Flag {
        final int mask;

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
        public boolean is(int userBits) {
            return (userBits & mask) == 0;
        }
        
        /**
         * Updates this flag in userBits.
         * @param userBits old user bits.
         * @param value new value of flag.
         * @return updates userBits.
         */
        public int set(int userBits, boolean value) {
            return value ? userBits & ~mask : userBits | mask;
        }
    }
   
	// -------------------------- constants --------------------------------
    
    private static final short EXTEND_90 = 0;
    private static final short EXTEND_0 = 1;
    private static final short EXTEND_45 = 2;
    private static final short EXTEND_ANY = 3;
    
    
	/** fixed-length arc */                                 private static final int FIXED =                     01;
	/** fixed-angle arc */                                  private static final int FIXANG =                    02;
//	/** arc has text that is far away */                    private static final int AHASFARTEXT =               04;
//	/** arc is not in use */                                private static final int DEADA =                    020;
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
    /** bits used in database */                            private static final int DATABASE_FLAGS = COMMON_BITS | /*AANGLE |*/ BODYARROW |
            ISTAILNEGATED | TAILNOEXTEND | TAILARROW | ISHEADNEGATED | HEADNOEXTEND | HEADARROW; 

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
    /** length of this ImmutableArcInst in lambda units. */         private final double gridLength;
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
     * @param gridLength the length of this ImmutableArcInst in grid units.
     * @param angle the angle if this ImmutableArcInst (in tenth-degrees).
     * @param flags flag bits of this ImmutableArcInst.
     * @param vars array of Variables of this ImmutableArcInst
     */
    ImmutableArcInst(int arcId, ArcProto protoType, Name name, TextDescriptor nameDescriptor,
            int tailNodeId, PortProtoId tailPortId, EPoint tailLocation,
            int headNodeId, PortProtoId headPortId, EPoint headLocation,
            int gridFullWidth, double gridLength, short angle, int flags, Variable[] vars) {
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
        this.gridLength = gridLength;
        this.angle = angle;
//        check();
    }
    
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
    public double getLambdaLength() { return DBMath.gridToLambda(gridLength); }
    
    /**
     * Returns length of this ImmutableArcInst in grid units.
     * @return length of this ImmutableArcInst in grid units.
     */
    public double getGridLength() { return gridLength; }
    
	/**
	 * Method to return the rotation angle of this ImmutableArcInst.
     * This is an angle of direction from tailLocation to headLocation.
	 * @return the rotation angle of this ImmutableArcInst (in tenth-degrees).
	 */
	public int getAngle() { return angle; }

    private boolean isManhattan() {
        if (headLocation.getGridX() == tailLocation.getGridX()) {
            return headLocation.getGridY() != tailLocation.getGridY() ||
                    (angle == 0 || angle == 900 || angle == 1800 || angle == 2700);
        } else {
            return tailLocation.getGridY() == headLocation.getGridY();
        }
    }
    
    /**
     * Returns new ImmutableArcInst object.
     *
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
        if (gridFullWidth < 0 || gridFullWidth > Integer.MAX_VALUE || (gridFullWidth&1) != 0) throw new IllegalArgumentException("gridFullWidth");
        angle %= 3600;
        if (angle < 0) angle += 3600;
        flags &= DATABASE_FLAGS;
        if (!(tailPortId instanceof PrimitivePort && ((PrimitivePort)tailPortId).isNegatable()))
            flags = TAIL_NEGATED.set(flags, false);
        if (!(headPortId instanceof PrimitivePort && ((PrimitivePort)headPortId).isNegatable()))
            flags = HEAD_NEGATED.set(flags, false);
        return new ImmutableArcInst(arcId, protoType, name, nameDescriptor,
                tailNodeId, tailPortId, tailLocation,
                headNodeId, headPortId, headLocation,
                (int)gridFullWidth, tailLocation.gridDistance(headLocation), updateAngle((short)angle, tailLocation, headLocation), flags, Variable.NULL_ARRAY);
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
                this.gridFullWidth, this.gridLength, this.angle, this.flags, getVars());
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
                this.gridFullWidth, this.gridLength, this.angle, this.flags, getVars());
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
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, tailLocation,
                this.headNodeId, this.headPortId, headLocation,
                this.gridFullWidth, tailLocation.gridDistance(headLocation), updateAngle(this.angle, tailLocation, headLocation), this.flags, getVars());
	}

	/**
     * Returns ImmutableArcInst which differs from this ImmutableArcInst by width.
     * @param gridFullWidth full arc width in grid units.
     * @return ImmutableArcInst which differs from this ImmutableArcInst by width.
     * @throws IllegalArgumentException if width is negative.
     */
	public ImmutableArcInst withGridFullWidth(long gridFullWidth) {
		if (getGridFullWidth() == gridFullWidth) return this;
        if (gridFullWidth < 0 || gridFullWidth > Integer.MAX_VALUE || (gridFullWidth&1) != 0) throw new IllegalArgumentException("gridWidth");
        if (this.gridFullWidth == gridFullWidth) return this;
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                (int)gridFullWidth, this.gridLength, this.angle, this.flags, getVars());
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
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridFullWidth, this.gridLength, (short)angle, this.flags, getVars());
	}

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by user bits.
	 * @param flags flag bits of this ImmutableArcInst.
	 * @return ImmutableArcInst which differs from this ImmutableArcInst by user bits.
	 */
	public ImmutableArcInst withFlags(int flags) {
        flags &= DATABASE_FLAGS;
        if (!(tailPortId instanceof PrimitivePort && ((PrimitivePort)tailPortId).isNegatable()))
            flags = TAIL_NEGATED.set(flags, false);
        if (!(headPortId instanceof PrimitivePort && ((PrimitivePort)headPortId).isNegatable()))
            flags = HEAD_NEGATED.set(flags, false);
        if (this.flags == flags) return this;
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridFullWidth, this.gridLength, this.angle, flags, getVars());
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
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridFullWidth, this.gridLength, this.angle, this.flags, vars);
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
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.gridFullWidth, this.gridLength, this.angle, this.flags, vars);
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
                this.gridFullWidth, this.gridLength, this.angle, this.flags, vars);
    }
    
    private static short updateAngle(short angle, EPoint tailLocation, EPoint headLocation) {
        if (tailLocation.equals(headLocation)) return angle;
        return (short)DBMath.figureAngle(tailLocation, headLocation);
    }
    
    /**
     * Tests specific flag is set on this ImmutableArcInst.
     * @param flag flag selector.
     * @return true if specific flag is set,
     */
    public boolean is(Flag flag) { return flag.is(flags); }
    
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
                tailLocation.gridDistance(headLocation), updateAngle((short)angle, tailLocation, headLocation), flags, vars);
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
    
    public double[] computeGridBounds(CellBackup.Memoization m, double[] result) {
        if (result == null)
            result = new double[4];
     
        if (protoType.isCurvable()) {
            // get the radius information on the arc
            Double radiusDouble = getRadius();
            if (radiusDouble != null) {
                Poly curvedPoly = curvedArcLambdaOutline(Poly.Type.FILLED, getLambdaFullWidth(), radiusDouble.doubleValue());
                if (curvedPoly != null) {
                    Rectangle2D newBounds = curvedPoly.getBounds2D();
                    result[0] = DBMath.lambdaToGrid(newBounds.getMinX());
                    result[1] = DBMath.lambdaToGrid(newBounds.getMinY());
                    result[2] = DBMath.lambdaToGrid(newBounds.getMaxX());
                    result[3] = DBMath.lambdaToGrid(newBounds.getMaxY());
                    return result;
                }
            }
        }
        
        long tx = tailLocation.getGridX();
        long ty = tailLocation.getGridY();
        long hx = headLocation.getGridX();
        long hy = headLocation.getGridY();
        
       // zero-width polygons are simply lines
        int gridWidth = (int)getGridFullWidth();
        if (gridWidth == 0) {
            if (tx < hx) {
                result[0] = tx;
                result[2] = hx;
            } else {
                result[0] = hx;
                result[2] = tx;
            }
            if (ty < hy) {
                result[1] = ty;
                result[3] = hy;
            } else {
                result[1] = hy;
                result[3] = ty;
            }
            return result;
        }
        
		int w2 = gridWidth >>> 1;
        short shrinkT = (flags & TAILNOEXTEND) == 0 /* is(TAIL_EXTENDED */ ? m.getShrinkage(tailNodeId) : EXTEND_0;
        short shrinkH = (flags & HEADNOEXTEND) == 0 /* is(HEAD_EXTENDED */ ? m.getShrinkage(headNodeId) : EXTEND_0;

        if (shrinkH <= EXTEND_0 && shrinkT <= EXTEND_0 && isManhattan()) {
            switch (angle) {
                case 0:
                    assert tx <= hx && ty == hy;
                    result[0] = shrinkT == EXTEND_90 ? tx - w2 : tx;
                    result[1] = ty - w2;
                    result[2] = shrinkH == EXTEND_90 ? hx + w2 : hx;
                    result[3] = ty + w2;
                    break;
                case 900:
                    assert tx == hx && ty <= hy;
                    result[0] = tx - w2;
                    result[1] = shrinkT == EXTEND_90 ? ty - w2 : ty;
                    result[2] = tx + w2;
                    result[3] = shrinkH == EXTEND_90 ? hy + w2 : hy;
                    break;
                case 1800:
                    assert hx <= tx && ty == hy;
                    result[0] = shrinkH == EXTEND_90 ? hx - w2 : hx;
                    result[1] = ty - w2;
                    result[2] = shrinkT == EXTEND_90 ? tx + w2 : tx;
                    result[3] = ty + w2;
                    break;
                case 2700:
                    assert tx == hx && hy <= ty;
                    result[0] = tx - w2;
                    result[1] = shrinkH == EXTEND_90 ? hy - w2 : hy;
                    result[2] = tx + w2;
                    result[3] = shrinkT == EXTEND_90 ? ty + w2 : ty;
                    break;
                default:
                    assert false;
            }
        } else {
            long w2xy = GenMath.polarToXY(w2, angle);
            int w2x = GenMath.getX(w2xy);
            int w2y = GenMath.getY(w2xy);
            
            if (shrinkT == EXTEND_90) {
                tx -= w2x;
                ty -= w2y;
            } else if (shrinkT >= EXTEND_ANY) {
                int angle = this.angle >= 1800 ? this.angle - 1800 : this.angle + 1800;
                int angle2 = (shrinkT - EXTEND_ANY) - angle;
                if (angle2 < 0)
                    angle2 += 3600;
                long e = computeExtension(GenMath.packXY(-w2x, -w2y), GenMath.polarToXY(w2, angle2));
                tx += GenMath.getX(e);
                ty += GenMath.getY(e);
            }
            if (shrinkH == EXTEND_90) {
                hx += w2x;
                hy += w2y;
            } else if (shrinkH >= EXTEND_ANY) {
                int angle2 = (shrinkH - EXTEND_ANY) - angle;
                if (angle2 < 0)
                    angle2 += 3600;
                long e = computeExtension(w2xy, GenMath.polarToXY(w2, angle2));
                hx += GenMath.getX(e);
                hy += GenMath.getY(e);
            }
            if (w2x < 0) w2x = -w2x;
            if (w2y < 0) w2y = -w2y;
            if (tx <= hx) {
                result[0] = tx - w2y;
                result[2] = hx + w2y;
            } else {
                result[0] = hx - w2y;
                result[2] = tx + w2y;
            }
            if (ty <= hy) {
                result[1] = ty - w2x;
                result[3] = hy + w2x;
            } else {
                result[1] = hy - w2x;
                result[3] = ty + w2x;
            }
        }
        return result;
    }
    
	/**
	 * Method to create a Poly object that describes this ImmutableArcInst in grid units.
	 * The ArcInst is described by its width, and style.
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
                Poly curvedPoly = curvedArcLambdaOutline(style, DBMath.gridToLambda(gridWidth), radiusDouble.doubleValue());
                if (curvedPoly != null) {
                    for (Point2D p: curvedPoly.getPoints())
                        p.setLocation(DBMath.lambdaToGrid(p.getX()), DBMath.lambdaToGrid(p.getY()));
                    return curvedPoly;
                }
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
        short shrinkT = (flags & TAILNOEXTEND) == 0 /* is(TAIL_EXTENDED */ ? m.getShrinkage(tailNodeId) : EXTEND_0;
        short shrinkH = (flags & HEADNOEXTEND) == 0 /* is(HEAD_EXTENDED */ ? m.getShrinkage(headNodeId) : EXTEND_0;
        Point2D.Double lT = new Point2D.Double();
        Point2D.Double rT = new Point2D.Double();
        Point2D.Double rH = new Point2D.Double();
        Point2D.Double lH = new Point2D.Double();

        long tx = tailLocation.getGridX();
        long ty = tailLocation.getGridY();
        long hx = headLocation.getGridX();
        long hy = headLocation.getGridY();
        if (shrinkH <= EXTEND_0 && shrinkT <= EXTEND_0 && isManhattan()) {
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
            long w2xy = GenMath.polarToXY(w2, angle);
            int w2x = GenMath.getX(w2xy);
            int w2y = GenMath.getY(w2xy);
            
            if (shrinkT == EXTEND_90) {
                tx -= w2x;
                ty -= w2y;
            } else if (shrinkT >= EXTEND_ANY) {
                int angle = this.angle >= 1800 ? this.angle - 1800 : this.angle + 1800;
                int angle2 = (shrinkT - EXTEND_ANY) - angle;
                if (angle2 < 0)
                    angle2 += 3600;
                long e = computeExtension(GenMath.packXY(-w2x, -w2y), GenMath.polarToXY(w2, angle2));
                tx += GenMath.getX(e);
                ty += GenMath.getY(e);
            }
            lT.setLocation(tx - w2y, ty + w2x);
            rT.setLocation(tx + w2y, ty - w2x);

            if (shrinkH == EXTEND_90) {
                hx += w2x;
                hy += w2y;
            } else if (shrinkH >= EXTEND_ANY) {
                int angle2 = (shrinkH - EXTEND_ANY) - angle;
                if (angle2 < 0)
                    angle2 += 3600;
                long e = computeExtension(w2xy, GenMath.polarToXY(w2, angle2));
                hx += GenMath.getX(e);
                hy += GenMath.getY(e);
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
     * Computes extension vector of wire, 
     */
    private static long computeExtension(long xy1, long xy2) {
        int ix1 = GenMath.getX(xy1);
        int iy1 = GenMath.getY(xy1);
        double x1 = GenMath.getX(xy1);
        double y1 = GenMath.getY(xy1);
        double x2 = GenMath.getX(xy2);
        double y2 = GenMath.getY(xy2);
        
        double det = x1*y2 - y1*x2;
        if (det == 0) return 0;
        double s1 = x1*x1 + y1*y1;
        double s2 = x2*x2 + y2*y2;
        double xx = (x2*s1 + x1*s2)/det;
        double yy = (y2*s1 + y1*s2)/det;
        int x = (int)(xx >= 0 ? xx + 0.5 : xx - 0.5);
        int y = (int)(yy >= 0 ? yy + 0.5 : yy - 0.5);
        int mx = x + iy1;
        int my = y - ix1;
        if (det < 0) {
            mx = -mx;
            my = -my;
        }
        return GenMath.packXY(mx, my);
    }
    
    void registerShrinkage(int shrinkageState[]) {
        // shrinkage
        if (getGridFullWidth() == 0) return;
        long dx = headLocation.getGridX() - tailLocation.getGridX();
        long dy = headLocation.getGridY() - tailLocation.getGridY();
        boolean is90, is45;
        if (dx != 0 || dy != 0) {
            is90 = dx == 0 || dy == 0;
            is45 = dx == dy || dx == -dy;
        } else {
            is90 = angle % 900 == 0;
            is45 = angle % 900 == 450;
        }
        int tailAngle = angle < 1800 ? angle + 1800 : angle - 1800;
        registerArcEnd(shrinkageState, tailNodeId, (short)tailAngle, is90, is45, is(ImmutableArcInst.TAIL_EXTENDED));
        registerArcEnd(shrinkageState, headNodeId, angle, is90, is45, is(ImmutableArcInst.HEAD_EXTENDED));
    }
    
    private static final int ANGLE_SHIFT = 12;
    private static final int ANGLE_MASK = (1 << ANGLE_SHIFT) - 1;
    private static final int ANGLE_90_MASK = 1 << (ANGLE_SHIFT*2);
    private static final int ANGLE_45_MASK = 1 << (ANGLE_SHIFT*2 + 1);
    private static final int ANGLE_ANY_MASK = 1 << (ANGLE_SHIFT*2 + 2);
//        private static final int ANGLE_FULL_SHRINK = -1;
    private static final int ANGLE_COUNT_SHIFT = ANGLE_SHIFT*2 + 4;
    
    private void registerArcEnd(int[] angles, int nodeId, short angle, boolean is90, boolean is45, boolean extended) {
        assert angle >= 0 && angle < 3600;
        int ang = angles[nodeId];
//            if (ang == ANGLE_FULL_SHRINK) return;
//            if (extended) {
        int ang0 = ang & ANGLE_MASK;
        int ang1 = (ang >> ANGLE_SHIFT) & ANGLE_MASK;
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
        if (is90)
            ang |= ANGLE_90_MASK;
        else if (is45)
            ang |= ANGLE_45_MASK;
        else
            ang |= ANGLE_ANY_MASK;
//            } else {
//                ang = ANGLE_FULL_SHRINK;
//            }
        angles[nodeId] = ang;
    }
    
    static short computeShrink(int angs) {
//            boolean has90 = (angs&ANGLE_90_MASK) != 0;
        boolean has45 = (angs&ANGLE_45_MASK) != 0;
        boolean hasAny = (angs&ANGLE_ANY_MASK) != 0;
        int count = angs >>> ANGLE_COUNT_SHIFT;
        
        if (hasAny || has45) {
            if (count == 2) {
                int ang0 = angs & ANGLE_MASK;
                int ang1 = (angs >> ANGLE_SHIFT) & ANGLE_MASK;
                int da = ang0 > ang1 ? ang0 - ang1 : ang1 - ang0;
                if (da == 900 || da == 2700) return EXTEND_90;
                if (da == 0 || da == 1800) return EXTEND_0;
                if (900 < da && da < 2700) {
                    int a = ang0 + ang1;
                    if (a >= 3600)
                        a -= 3600;
                    return (short)(EXTEND_ANY + a);
                }
            }
//            } else if (has90 && has45) {
//                return EXTEND_45;
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
     * Method to fill polygon "poly" with the outline of the curved arc in
     * this ImmutableArcInst whose width is "lambdaWid".  The style of the polygon is set to "style".
     * If there is no curvature information in the arc, the routine returns null,
     * otherwise it returns the curved polygon.
     */
    public Poly curvedArcLambdaOutline(Poly.Type style, double lambdaWid, double lambdaRadius) {
        // get information about the curved arc
        double pureRadius = Math.abs(lambdaRadius);
        double lambdaLength = getLambdaLength();
        
        // see if the lambdaRadius can work with these arc ends
        if (pureRadius*2 < lambdaLength) return null;
        
        // determine the center of the circle
        Point2D [] centers = DBMath.findCenters(pureRadius, headLocation, tailLocation, lambdaLength);
        if (centers == null) return null;
        
        Point2D centerPt = centers[1];
        if (lambdaRadius < 0) {
            centerPt = centers[0];
        }
        
        // determine the base and range of angles
        int angleBase = DBMath.figureAngle(centerPt, headLocation);
        int angleRange = DBMath.figureAngle(centerPt, tailLocation);
        angleRange -= angleBase;
        if (angleRange < 0) angleRange += 3600;
        
        // force the curvature to be the smaller part of a circle (used to determine this by the reverse-ends bit)
        if (angleRange > 1800) {
            angleBase = DBMath.figureAngle(centerPt, tailLocation);
            angleRange = DBMath.figureAngle(centerPt, headLocation);
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
        double outerRadius = pureRadius + lambdaWid / 2;
        double innerRadius = outerRadius - lambdaWid;
        
        // fill the polygon
        for(int i=0; i<=pieces; i++) {
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
        assert gridFullWidth >= 0 && (gridFullWidth&1) == 0;
        assert (flags & ~DATABASE_FLAGS) == 0;
        if (is(TAIL_NEGATED))
            assert tailPortId instanceof PrimitivePort && ((PrimitivePort)tailPortId).isNegatable();
        if (is(HEAD_NEGATED))
            assert headPortId instanceof PrimitivePort && ((PrimitivePort)headPortId).isNegatable();
        assert 0 <= angle && angle < 3600;
		assert gridLength == tailLocation.gridDistance(headLocation);
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
		int elibBits = flags & COMMON_BITS;
	
		// adjust bits for extension
		if (!HEAD_EXTENDED.is(flags) || !TAIL_EXTENDED.is(flags))
		{
			elibBits |= DISK_NOEXTEND;
			if (HEAD_EXTENDED.is(flags) != TAIL_EXTENDED.is(flags))
			{
				if (TAIL_EXTENDED.is(flags)) elibBits |= DISK_NOTEND0;
				if (HEAD_EXTENDED.is(flags)) elibBits |= DISK_NOTEND1;
			}
		}
	
		// adjust bits for directionality
		if (HEAD_ARROWED.is(flags) || TAIL_ARROWED.is(flags) || BODY_ARROWED.is(flags))
		{
			elibBits |= DISK_ISDIRECTIONAL;
			if (TAIL_ARROWED.is(flags)) elibBits |= DISK_REVERSEEND;
			if (!HEAD_ARROWED.is(flags) && !TAIL_ARROWED.is(flags)) elibBits |= DISK_NOTEND1;
		}

		// adjust bits for negation
        boolean normalEnd = (elibBits & DISK_REVERSEEND) == 0;
		if (TAIL_NEGATED.is(flags)) elibBits |= (normalEnd ? ISTAILNEGATED : ISHEADNEGATED);
		if (HEAD_NEGATED.is(flags)) elibBits |= (normalEnd ? ISHEADNEGATED : ISTAILNEGATED);
        
		int elibAngle = (angle + 5)/10;
		if (elibAngle >= 360) elibAngle -= 360;
        
        return elibBits | (elibAngle << DISK_AANGLESH);
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
        int newBits = elibBits & COMMON_BITS;
		if ((elibBits&ISTAILNEGATED) != 0)
		{
			newBits |= (elibBits&DISK_REVERSEEND) == 0 ? ISTAILNEGATED : ISHEADNEGATED;
		}
		if ((elibBits&ISHEADNEGATED) != 0)
		{
            newBits |= (elibBits&DISK_REVERSEEND) == 0 ? ISHEADNEGATED : ISTAILNEGATED;
		}

		if ((elibBits&DISK_NOEXTEND) != 0)
		{
			if ((elibBits&DISK_NOTEND0) == 0) newBits |= TAILNOEXTEND;
			if ((elibBits&DISK_NOTEND1) == 0) newBits |= HEADNOEXTEND;
		}

		if ((elibBits&DISK_ISDIRECTIONAL) != 0)
		{
            newBits |= BODYARROW;
			if ((elibBits&DISK_REVERSEEND) == 0)
			{
				if ((elibBits&DISK_NOTEND1) == 0) newBits |= HEADARROW;
			} else
			{
				if ((elibBits&DISK_NOTEND0) == 0) newBits |= TAILARROW;
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
        int angle = (elibBits & DISK_AANGLE) >> DISK_AANGLESH;
        return (angle % 360)*10;
	}
}

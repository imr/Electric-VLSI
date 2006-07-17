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
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;

import java.io.IOException;

/**
 * Immutable class ImmutableArcInst represents an arc instance.
 */
public class ImmutableArcInst extends ImmutableElectricObject {
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

	/** width of this ImmutableArcInst. */                          public final double width;
    /** length of this ImmutableArcInst. */                         public final double length;
    /** Angle if this ImmutableArcInst (in tenth-degrees). */       public final short angle;
 
	/**
     * The private constructor of ImmutableArcInst. Use the factory "newInstance" instead.
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
     * @param width the width of this ImmutableArcInst.
     * @param length the length of this ImmutableArcInst.
     * @param angle the angle if this ImmutableArcInst (in tenth-degrees).
     * @param flags flag bits of this ImmutableArcInst.
     * @param vars array of Variables of this ImmutableArcInst
     */
     ImmutableArcInst(int arcId, ArcProto protoType, Name name, TextDescriptor nameDescriptor,
            int tailNodeId, PortProtoId tailPortId, EPoint tailLocation,
            int headNodeId, PortProtoId headPortId, EPoint headLocation,
            double width, double length, short angle, int flags, Variable[] vars) {
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
        this.width = width;
        this.length = length;
        this.angle = angle;
//        check();
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
     * @param width the width of this ImmutableArcInst.
     * @param angle the angle if this ImmutableArcInst (in tenth-degrees).
     * @param flags flag bits of this ImmutableNodeInst.
     * @return new ImmutableArcInst object.
     * @throws NullPointerException if protoType, name, tailPortId, headPortId, tailLocation, headLocation is null.
     * @throws IllegalArgumentException if arcId, tailNodeId, headNodeId or name is not valid, or width is bad.
     */
    public static ImmutableArcInst newInstance(int arcId, ArcProto protoType, Name name, TextDescriptor nameDescriptor,
            int tailNodeId, PortProtoId tailPortId, EPoint tailLocation,
            int headNodeId, PortProtoId headPortId, EPoint headLocation,
            double width, int angle, int flags) {
        if (arcId < 0) throw new IllegalArgumentException("arcId");
		if (protoType == null) throw new NullPointerException("protoType");
		if (name == null) throw new NullPointerException("name");
        if (!name.isValid() || name.hasEmptySubnames() || name.isTempname() && name.getBasename() != BASENAME) throw new IllegalArgumentException("name");
        if (nameDescriptor != null)
            nameDescriptor = nameDescriptor.withDisplayWithoutParamAndCode();
        if (!(width >= 0)) throw new IllegalArgumentException("width");
        if (tailNodeId < 0) throw new IllegalArgumentException("tailNodeId");
        if (tailPortId == null) throw new NullPointerException("tailPortId");
        if (tailLocation == null) throw new NullPointerException("tailLocation");
        if (headNodeId < 0) throw new IllegalArgumentException("headNodeId");
        if (headPortId == null) throw new NullPointerException("headPortId");
        if (headLocation == null) throw new NullPointerException("headLocation");
        width = DBMath.round(width);
        if (width == -0.0) width = +0.0;
        angle %= 3600;
        if (angle < 0) angle += 3600;
        flags &= DATABASE_FLAGS;
		return new ImmutableArcInst(arcId, protoType, name, nameDescriptor,
                tailNodeId, tailPortId, tailLocation,
                headNodeId, headPortId, headLocation,
                width, tailLocation.distance(headLocation), updateAngle((short)angle, tailLocation, headLocation), flags, Variable.NULL_ARRAY);
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
                this.width, this.length, this.angle, this.flags, getVars());
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
                this.width, this.length, this.angle, this.flags, getVars());
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
                this.width, tailLocation.distance(headLocation), updateAngle(this.angle, tailLocation, headLocation), this.flags, getVars());
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
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                width, this.length, this.angle, this.flags, getVars());
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
                this.width, this.length, (short)angle, this.flags, getVars());
	}

	/**
	 * Returns ImmutableArcInst which differs from this ImmutableArcInst by user bits.
	 * @param flags flag bits of this ImmutableArcInst.
	 * @return ImmutableArcInst which differs from this ImmutableArcInst by user bits.
	 */
	public ImmutableArcInst withFlags(int flags) {
        flags &= DATABASE_FLAGS;
		if (this.flags == flags) return this;
		return new ImmutableArcInst(this.arcId, this.protoType, this.name, this.nameDescriptor,
                this.tailNodeId, this.tailPortId, this.tailLocation,
                this.headNodeId, this.headPortId, this.headLocation,
                this.width, this.length, this.angle, flags, getVars());
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
                this.width, this.length, this.angle, this.flags, vars);
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
                this.width, this.length, this.angle, this.flags, vars);
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
                this.width, this.length, this.angle, this.flags, vars);
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
        writer.writeCoord(width);
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
        double width = reader.readCoord();
        short angle = reader.readShort();
        int flags = reader.readInt();
        boolean hasVars = reader.readBoolean();
        Variable[] vars = hasVars ? readVars(reader) : Variable.NULL_ARRAY;
        return new ImmutableArcInst(arcId, protoType, name, nameDescriptor,
                tailNodeId, tailPortId, tailLocation, headNodeId, headPortId, headLocation, width,
                tailLocation.distance(headLocation), updateAngle((short)angle, tailLocation, headLocation), flags, vars);
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
                this.width == that.width && this.angle == that.angle && this.flags == that.flags;
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
        assert width > 0 || width == 0 && 1/width > 0;
        assert DBMath.round(width) == width;
        assert (flags & ~DATABASE_FLAGS) == 0;
        assert 0 <= angle && angle < 3600;
		assert length == tailLocation.distance(headLocation);
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

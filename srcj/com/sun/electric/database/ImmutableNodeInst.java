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
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Arrays;

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
    
    public final static ImmutableNodeInst[] NULL_ARRAY = {};
    public final static ImmutableArrayList<ImmutableNodeInst> EMPTY_LIST = new ImmutableArrayList<ImmutableNodeInst>(NULL_ARRAY);
    
    /** id of this NodeInst in parent. */                           public final int nodeId;
	/** Prototype id. */                                            public final NodeProtoId protoId;
	/** name of this ImmutableNodeInst. */							public final Name name;
	/** The text descriptor of name of ImmutableNodeInst. */		public final TextDescriptor nameDescriptor;
	/** Orientation of this ImmutableNodeInst. */                   public final Orientation orient;
	/** anchor coordinate of this ImmutableNodeInst. */				public final EPoint anchor;
	/** size of this ImmutableNodeInst . */                         public final double width, height;
    /** Tech specifiic bits for this ImmutableNodeInsts. */         public final byte techBits;
	/** Text descriptor of prototype name. */                       public final TextDescriptor protoDescriptor;
    /** Variables on PortInsts. */                                  final ImmutablePortInst[] ports;
 
	/**
	 * The private constructor of ImmutableNodeInst. Use the factory "newInstance" instead.
     * @param nodeId id of this NodeInst in parent.
	 * @param protoId the NodeProtoId of which this is an instance.
	 * @param name name of new ImmutableNodeInst.
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
     ImmutableNodeInst(int nodeId, NodeProtoId protoId, Name name, TextDescriptor nameDescriptor,
            Orientation orient, EPoint anchor, double width, double height,
            int flags, byte techBits, TextDescriptor protoDescriptor, Variable[] vars, ImmutablePortInst[] ports) {
        super(vars, flags);
        this.nodeId = nodeId;
        this.protoId = protoId;
        this.name = name;
        this.nameDescriptor = nameDescriptor;
        this.orient = orient;
        this.anchor = anchor;
        this.width = width;
        this.height = height;
        this.techBits = techBits;
        this.protoDescriptor = protoDescriptor;
        this.ports = ports;
//        check();
    }

	/**
	 * Returns new ImmutableNodeInst object.
     * @param nodeId id of this NodeInst in parent.
	 * @param protoId the NodeProtoId of which this is an instance.
	 * @param name name of new ImmutableNodeInst.
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
     * @throws IllegalArgumentException if nodeId or size is bad.
	 */
    public static ImmutableNodeInst newInstance(int nodeId, NodeProtoId protoId, Name name, TextDescriptor nameDescriptor,
            Orientation orient, EPoint anchor, double width, double height,
            int flags, int techBits, TextDescriptor protoDescriptor) {
        if (nodeId < 0) throw new IllegalArgumentException("nodeId");
		if (protoId == null) throw new NullPointerException("protoId");
		if (name == null) throw new NullPointerException("name");
        if (!name.isValid() || name.hasEmptySubnames() || name.isTempname() && name.isBus())
        	throw new IllegalArgumentException("bad name: "+name);
        if (name.hasDuplicates()) throw new IllegalArgumentException("name");
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
		return new ImmutableNodeInst(nodeId, protoId, name, nameDescriptor,
                orient, anchor, width, height, flags, (byte)techBits, protoDescriptor, Variable.NULL_ARRAY, ImmutablePortInst.NULL_ARRAY);
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
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by name.
	 * @param name node name key.
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by name.
	 * @throws NullPointerException if name is null.
	 */
	public ImmutableNodeInst withName(Name name) {
		if (this.name.toString().equals(name.toString())) return this;
		if (name == null) throw new NullPointerException("name");
        if (!name.isValid() || name.hasEmptySubnames() || name.isTempname() && name.isBus()) throw new IllegalArgumentException("name");
        if (name.hasDuplicates()) throw new IllegalArgumentException("name");
		return new ImmutableNodeInst(this.nodeId, this.protoId, name, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.flags, this.techBits, this.protoDescriptor, getVars(), this.ports);
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by name descriptor.
     * @param nameDescriptor TextDescriptor of name
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by name descriptor.
	 */
	public ImmutableNodeInst withNameDescriptor(TextDescriptor nameDescriptor) {
        if (nameDescriptor != null)
            nameDescriptor = nameDescriptor.withDisplayWithoutParamAndCode();
        if (this.nameDescriptor == nameDescriptor) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.flags, this.techBits, this.protoDescriptor, getVars(), this.ports);
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
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.nameDescriptor,
                orient, this.anchor, this.width, this.height, this.flags, this.techBits, this.protoDescriptor, getVars(), this.ports);
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
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.nameDescriptor,
                this.orient, anchor, this.width, this.height, this.flags, this.techBits, this.protoDescriptor, getVars(), this.ports);
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
        if (!(width >= 0)) throw new IllegalArgumentException("width is " + TextUtils.formatDouble(width));
        if (!(height >= 0)) throw new IllegalArgumentException("height is " + TextUtils.formatDouble(height));
        if (protoId == Generic.tech.cellCenterNode) return this;
        if (protoId instanceof CellId) return this;
        width = DBMath.round(width);
        height = DBMath.round(height);
        if (width == -0.0) width = +0.0;
        if (height == -0.0) height = +0.0;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.nameDescriptor,
                this.orient, this.anchor, width, height, this.flags, this.techBits, this.protoDescriptor, getVars(), this.ports);
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by flag bits.
	 * @param flags flag bits defined by ImmutableNodeInst.Flag.
     * @return ImmutableNodeInst which differs from this ImmutableNodeInst by flag bit.
	 */
    public ImmutableNodeInst withFlags(int flags) {
        flags &= FLAG_BITS;
        if (this.flags == flags) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, flags, this.techBits, this.protoDescriptor, getVars(), this.ports);
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
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.flags, (byte)techBits, this.protoDescriptor, getVars(), this.ports);
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
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.flags, this.techBits, protoDescriptor, getVars(), this.ports);
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
        Variable[] vars = arrayWithVariable(var.withParam(false));
        if (this.getVars() == vars) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.flags, this.techBits, this.protoDescriptor, vars, this.ports);
    }
    
	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by removing Variable
     * with the specified key. Returns this ImmutableNodeInst if it doesn't contain variable with the specified key.
	 * @param key Variable Key to remove.
	 * @return ImmutableNodeInst without Variable with the specified key.
	 * @throws NullPointerException if key is null
	 */
    public ImmutableNodeInst withoutVariable(Variable.Key key) {
        Variable[] vars = arrayWithoutVariable(key);
        if (this.getVars() == vars) return this;
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.flags, this.techBits, this.protoDescriptor, vars, this.ports);
    }
    
	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by renamed Ids.
	 * @param idMapper a map from old Ids to new Ids.
     * @return ImmutableNodeInst with renamed Ids.
	 */
    ImmutableNodeInst withRenamedIds(IdMapper idMapper) {
        Variable[] vars = arrayWithRenamedIds(idMapper);
        NodeProtoId protoId = this.protoId;
        ImmutablePortInst[] ports = portsWithRenamedIds(idMapper);
        if (protoId instanceof CellId)
            protoId = idMapper.get((CellId)protoId);
        if (getVars() == vars && this.protoId == protoId && this.ports == ports) return this;
		return new ImmutableNodeInst(this.nodeId, protoId, this.name, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.flags, this.techBits, this.protoDescriptor, vars, ports);
    }
    
	/**
	 * Returns array of ImmutablePortInst which differs from array of this ImmutableNodeInst by renamed Ids.
     * Returns array of this ImmutableNodeInst if it doesn't contain reanmed Ids.
	 * @param idMapper a map from old Ids to new Ids.
     * @return array of ImmutablePortInst with renamed Ids.
	 */
    private ImmutablePortInst[] portsWithRenamedIds(IdMapper idMapper) {
        if (ports.length == 0) {
            assert ports == ImmutablePortInst.NULL_ARRAY;
            return ports;
        }
        if (protoId instanceof CellId) {
            boolean chronIndexChanged = false;
            int maxChronIndex = -1;
            CellId subCellId = (CellId)protoId;
            for (int chronIndex = 0; chronIndex < ports.length; chronIndex++) {
                ImmutablePortInst oldPort = ports[chronIndex];
                if (oldPort == ImmutablePortInst.EMPTY) continue;
                ExportId oldExportId = subCellId.getPortId(chronIndex);
                assert oldExportId.chronIndex == chronIndex;
                ExportId newExportId = idMapper.get(oldExportId);
                maxChronIndex = Math.max(maxChronIndex, newExportId.chronIndex);
                if (newExportId.chronIndex != chronIndex)
                    chronIndexChanged = true;
            }
            if (chronIndexChanged) {
                ImmutablePortInst[] newPorts = new ImmutablePortInst[maxChronIndex + 1];
                assert newPorts.length > 0;
                Arrays.fill(newPorts, ImmutablePortInst.EMPTY);
                for (int chronIndex = 0; chronIndex < ports.length; chronIndex++) {
                    ImmutablePortInst oldPort = ports[chronIndex];
                    if (oldPort == ImmutablePortInst.EMPTY) continue;
                    newPorts[idMapper.get(subCellId.getPortId(chronIndex)).chronIndex] = oldPort.withRenamedIds(idMapper);
                }
                return newPorts;
            }
        }
        
        ImmutablePortInst[] newPorts = null;
        for (int i = 0; i < ports.length; i++) {
            ImmutablePortInst oldPort = ports[i];
            ImmutablePortInst newPort = oldPort.withRenamedIds(idMapper);
            if (newPort != oldPort && newPorts == null) {
                newPorts = new ImmutablePortInst[ports.length];
                System.arraycopy(ports, 0, newPorts, 0, i);
            }
            if (newPorts != null)
                newPorts[i] = newPort;
        }
        return newPorts != null ? newPorts : ports;
    }
    
	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by additional Variable on PortInst.
     * If this ImmutableNideInst has Variable on PortInst with the same key as new, the old variable will not be in new
     * ImmutableNodeInst.
     * @param portProtoId PortProtoId of port instance.
	 * @return ImmutableNodeInst with additional Variable.
	 * @throws NullPointerException if var is null
	 */
    public ImmutableNodeInst withPortInst(PortProtoId portProtoId, ImmutablePortInst portInst) {
        if (portProtoId.getParentId() != protoId) throw new IllegalArgumentException("portProtoId");
        int portChronIndex = portProtoId.getChronIndex();
        ImmutablePortInst[] newPorts;
        if (portChronIndex < ports.length) {
            if (ports[portChronIndex] == portInst) return this;
            if (portInst == ImmutablePortInst.EMPTY && portChronIndex == ports.length - 1) {
                int newLength = ports.length -1;
                while (newLength > 0 && ports[newLength - 1] == ImmutablePortInst.EMPTY)
                    newLength--;
                if (newLength > 0) {
                    newPorts = new ImmutablePortInst[newLength];
                    System.arraycopy(ports, 0, newPorts, 0, newLength);
                } else {
                    newPorts = ImmutablePortInst.NULL_ARRAY;
                }
            } else {
                newPorts = (ImmutablePortInst[])ports.clone();
                newPorts[portChronIndex] = portInst;
            }
        } else {
            if (portInst == ImmutablePortInst.EMPTY) return this;
            newPorts = new ImmutablePortInst[portChronIndex + 1];
            System.arraycopy(ports, 0, newPorts, 0, ports.length);
            Arrays.fill(newPorts, ports.length, portChronIndex, ImmutablePortInst.EMPTY);
            newPorts[portChronIndex] = portInst;
        }
		return new ImmutableNodeInst(this.nodeId, this.protoId, this.name, this.nameDescriptor,
                this.orient, this.anchor, this.width, this.height, this.flags, this.techBits, this.protoDescriptor, getVars(), newPorts);
    }

	/**
	 * Returns ImmutablePortInst of this ImmutableNodeInst with the specified PortProtoId.
     * @param portProtoId PortProtoId of port instance.
	 * @return ImmutablePortInst of this ImmutableNodeInst with the specified PortProtoId.
	 * @throws NullPointerException if portProtoId is null.
     * @throws IlleagalArgumentException if parent of portProtoId is not protoId of this ImmutableNodeInst.
	 */
    public ImmutablePortInst getPortInst(PortProtoId portProtoId) {
        if (portProtoId.getParentId() != protoId) throw new IllegalArgumentException("portProtoId");
        int portChronIndex = portProtoId.getChronIndex();
        return portChronIndex < ports.length ? ports[portChronIndex] : ImmutablePortInst.EMPTY;
    }
    
    /**
     * Returns true if this ImmutableNodeInst has variables on port instances.
     * @return true if this ImmutableNodeInst has variables on port instances.
     */
    public boolean hasPortInstVariables() { return ports.length > 0; }
    
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
     * Writes this ImmutableNodeInst to SnapshotWriter.
     * @param writer where to write.
     */
    void write(SnapshotWriter writer) throws IOException {
        writer.writeNodeId(nodeId);
        writer.writeNodeProtoId(protoId);
        writer.writeNameKey(name);
        writer.writeTextDescriptor(nameDescriptor);
        writer.writeOrientation(orient);
        writer.writePoint(anchor);
        writer.writeCoord(width);
        writer.writeCoord(height);
        writer.writeInt(flags);
        writer.writeByte(techBits);
        writer.writeTextDescriptor(protoDescriptor);
        for (int i = ports.length - 1; i >= 0; i--) {
            if (ports[i] == ImmutablePortInst.EMPTY) continue;
            writer.writeInt(i);
            ports[i].writeVars(writer);
        }
        writer.writeInt(-1);
        super.write(writer);
    }
    
    /**
     * Reads ImmutableNodeInst from SnapshotReader.
     * @param reader where to read.
     */
    static ImmutableNodeInst read(SnapshotReader reader) throws IOException {
        int nodeId = reader.readNodeId();
        NodeProtoId protoId = reader.readNodeProtoId();
        Name name = reader.readNameKey();
        TextDescriptor nameDescriptor = reader.readTextDescriptor();
        Orientation orient = reader.readOrientation();
        EPoint anchor = reader.readPoint();
        double width = reader.readCoord();
        double height = reader.readCoord();
        int flags = reader.readInt();
        byte techBits = reader.readByte();
        TextDescriptor protoDescriptor = reader.readTextDescriptor();
        ImmutablePortInst[] ports = ImmutablePortInst.NULL_ARRAY;
        for (;;) {
            int i = reader.readInt();
            if (i == -1) break;
            if (i >= ports.length) {
                ImmutablePortInst[] newPorts = new ImmutablePortInst[i + 1];
                System.arraycopy(ports, 0, newPorts, 0, ports.length);
                Arrays.fill(newPorts, ports.length, newPorts.length, ImmutablePortInst.EMPTY);
                ports = newPorts;
            }
            ports[i] = ImmutablePortInst.read(reader);
        }
        boolean hasVars = reader.readBoolean();
        Variable[] vars = hasVars ? readVars(reader) : Variable.NULL_ARRAY;
        return new ImmutableNodeInst(nodeId, protoId, name, nameDescriptor, orient, anchor, width, height,
                flags, techBits, protoDescriptor, vars, ports);
    }
    
    /**
     * Return a hash code value for fields of this object.
     * Variables of objects are not compared
     */
    public int hashCodeExceptVariables() { return nodeId; }

    /**
     * Indicates whether fields of other ImmutableElectricObject are equal to fileds of this object.
     * Variables of objects are not compared.
     * @param o other ImmutableElectricObject.
     * @return true if fields of objects are equal.
     */
    public boolean equalsExceptVariables(ImmutableElectricObject o) {
        if (this == o) return true;
        if (!(o instanceof ImmutableNodeInst)) return false;
        ImmutableNodeInst that = (ImmutableNodeInst)o;
        return this.nodeId == that.nodeId && this.protoId == that.protoId &&
                this.name == that.name && this.nameDescriptor == that.nameDescriptor &&
                this.orient == that.orient && this.anchor == that.anchor &&
                this.width == that.width && this.height == that.height &&
                this.flags == that.flags && this.techBits == that.techBits &&
                this.protoDescriptor == that.protoDescriptor;
    }
    
    /**
	 * Checks invariant of this ImmutableNodeInst.
	 * @throws AssertionError if invariant is broken.
	 */
	public void check() {
        check(false);
        assert nodeId >= 0;
		assert protoId != null;
		assert name != null;
        assert name.isValid() && !name.hasEmptySubnames();
        assert !(name.isTempname() && name.isBus());
        assert !name.hasDuplicates();
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
        for (int i = 0; i < ports.length; i++) {
            ImmutablePortInst portInst = ports[i];
            if (portInst.getNumVariables() != 0)
                portInst.check();
            else
                assert portInst == ImmutablePortInst.EMPTY;
        }
        if (ports.length > 0)
            assert ports[ports.length - 1].getNumVariables() > 0;
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
    
    public void computeBounds(NodeInst real, Rectangle2D.Double dstBounds)
	{
		// handle cell bounds
		if (protoId instanceof CellId)
		{
			// offset by distance from cell-center to the true center
			Cell subCell = (Cell)real.getProto();
			Rectangle2D bounds = subCell.getBounds();
            orient.rectangleBounds(bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY(), anchor.getX(), anchor.getY(), dstBounds);
            return;
		}

		// if zero size, set the bounds directly
		if (width == 0 && height == 0)
		{
			dstBounds.setRect(anchor.getX(), anchor.getY(), 0, 0);
            return;
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
				dstBounds.setRect(poly.getBounds2D());
                return;
			}
		}

		// special case for pins that become steiner points
		if (pn.isWipeOn1or2())
		{
			// schematic bus pins are so complex that only the technology knows their true size
			if (real.getProto() == Schematics.tech.busPinNode)
			{
				Poly [] polys = Schematics.tech.getShapeOfNode(real, null, null, false, false, null, null);
				if (polys.length > 0)
				{
					Rectangle2D bounds = polys[0].getBounds2D();
					dstBounds.setRect(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
					return;
				}
			}
			if (!real.hasExports() && real.pinUseCount())
//			if (real.getNumExports() == 0 && real.pinUseCount())
			{
				dstBounds.setRect(anchor.getX(), anchor.getY(), 0, 0);
                return;
			}
		}

		// special case for polygonally-defined nodes: compute precise geometry
		if (pn.isHoldsOutline() && real.getTrace() != null)
		{
			AffineTransform trans = orient.rotateAbout(anchor.getX(), anchor.getY());
			Poly[] polys = pn.getTechnology().getShapeOfNode(real);
			for (int i = 0; i < polys.length; i++)
			{
				Poly poly = polys[i];
				poly.transform(trans);
				if (i == 0)
					dstBounds.setRect(poly.getBounds2D());
				else
					Rectangle2D.union(poly.getBounds2D(), dstBounds, dstBounds);
			}
			return;
		}

		// normal bounds computation
        orient.rectangleBounds(-width/2, -height/2, width/2, height/2, anchor.getX(), anchor.getY(), dstBounds);
	}

    /**
	 * Method to return the "outline" information on this ImmutableNodeInst.
	 * Outline information is a set of coordinate points that further
	 * refines the NodeInst description.  It is typically used in
	 * Artwork primitives to give them a precise shape.  It is also
	 * used by pure-layer nodes in all layout technologies to allow
	 * them to take any shape.  It is even used by many MOS
	 * transistors to allow a precise gate path to be specified.
	 * @return an array of EPoint in database coordinates.
	 */
	public EPoint [] getTrace()
	{
        Variable var = getVar(NodeInst.TRACE);
        if (var == null) return null;
        Object obj = var.getObject();
        if (obj instanceof EPoint[]) return (EPoint[])obj;
		return null;
	}
    
	/**
	 * Method to return the length of this serpentine transistor.
	 * @return the transistor's length
	 * Returns -1 if this is not a serpentine transistor, or if the length cannot be found.
	 */
	public double getSerpentineTransistorLength()
	{
		Variable var = getVar(NodeInst.TRANSISTOR_LENGTH_KEY);
		if (var == null) return -1;
		Object obj = var.getObject();
		if (obj instanceof Integer)
		{
			// C Electric stored this as a "fraction", scaled by 120
			return ((Integer)obj).intValue() / 120;
		}
		if (obj instanceof Double)
		{
			return ((Double)obj).doubleValue();
		}
		if (obj instanceof String)
		{
			return TextUtils.atof((String)obj);
		}
		return -1;
	}

}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PortProto.java
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
package com.sun.electric.database.prototype;

import com.sun.electric.database.text.Name;
import com.sun.electric.technology.PrimitivePort;

import com.sun.electric.technology.ArcProto;

/**
 * The PortProto interface defines a type of PortInst.
 * It is be implemented as PrimitivePort (for primitives from Technologies)
 * or as Export (for cells in Libraries).
 * <P>
 * Every port in the database appears as one <I>prototypical</I> object and many <I>instantiative</I> objects.
 * Thus, for a PrimitivePort such as the "poly-left" port on a CMOS P-transistor there is one object (called a PrimitivePort, which is a PortProto)
 * that describes the transistor prototype and there are many objects (called PortInsts),
 * one for every port on an instance of that transistor in a circuit.
 * Similarly, for every Export, there is one object (called an Export, which is a PortProto)
 * that describes the Export on a Cell and there are many objects (also called PortInsts)
 * for every port on a use of that Cell in some other Cell.
 * PrimitivePorts are statically created and placed in the Technology objects,
 * but complex Exports are created by the tools and placed in Library objects.
 * <P>
 * A PortProto has a descriptive name, which may be overridden by another PortProto
 * at a higher level in the schematic or layout hierarchy.
 * The PortProto also has a parent cell, characteristics, and more.
 */
public interface PortProto
{
    /** Method to return PortProtoId of this PortProto.
     * PortProtoId identifies PortProto independently of threads.
     * @return PortProtoId of this PortProto.
     */
    public PortProtoId getId();
    
	/**
	 * Method to get the index of this PortProto.
	 * This is a zero-based index of ports on the NodeProto.
	 * @return the index of this PortProto.
	 */
	public int getPortIndex();

	/**
	 * Method to return the name key of this PortProto.
	 * @return the Name key of this PortProto.
	 */
	public Name getNameKey();

	/**
	 * Method to return the name of this PortProto.
	 * @return the name of this PortProto.
	 */
	public String getName();

	/**
	 * Method to return the parent NodeProto of this PortProto.
	 * @return the parent NodeProto of this PortProto.
	 */
	public NodeProto getParent();

	/**
	 * Method to return the PortCharacteristic of this PortProto.
	 * @return the PortCharacteristic of this PortProto.
	 */
	public PortCharacteristic getCharacteristic();

	/**
	 * Method to determine whether this PortProto is of type Power.
	 * This is determined by either having the proper Characteristic, or by
	 * having the proper name (starting with "vdd", "vcc", "pwr", or "power").
	 * @return true if this PortProto is of type Power.
	 */
	public boolean isPower();

	/**
	 * Method to determine whether this PortProto is of type Ground.
	 * This is determined by either having the proper PortCharacteristic, or by
	 * having the proper name (starting with "vss", "gnd", or "ground").
	 * @return true if this PortProto is of type Ground.
	 */
	public boolean isGround();

	/**
	 * method to return the base-level port that this PortProto is created from.
	 * For a PrimitivePort, it simply returns itself.
	 * For an Export, it returns the base port of its sub-port, the port on the NodeInst
	 * from which the Export was created.
	 * @return the base-level port that this PortProto is created from.
	 */
	public PrimitivePort getBasePort();

	/**
	 * Method to return true if the specified ArcProto can connect to this PortProto.
	 * @param arc the ArcProto to test for connectivity.
	 * @return true if this PortProto can connect to the ArcProto, false if it can't.
	 */
	public boolean connectsTo(ArcProto arc);
}

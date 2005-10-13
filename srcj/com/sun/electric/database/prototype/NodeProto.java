/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NodeProto.java
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
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;

import java.util.Iterator;

/**
 * The NodeProto interface defines a type of NodeInst.
 * It can be implemented as PrimitiveNode (for primitives from Technologies)
 * or as Cell (for cells in Libraries).
 * <P>
 * Every node in the database appears as one <I>prototypical</I> object and many <I>instantiative</I> objects.
 * Thus, for a PrimitiveNode such as the CMOS P-transistor there is one object (called a PrimitiveNode, which is a NodeProto)
 * that describes the transistor prototype and there are many objects (called NodeInsts),
 * one for every instance of a transistor that appears in a circuit.
 * Similarly, for every Cell, there is one object (called a Cell, which is a NodeProto)
 * that describes the Cell with everything in it and there are many objects (also called NodeInsts)
 * for every use of that Cell in some other Cell.
 * PrimitiveNodes are statically created and placed in the Technology objects,
 * but complex Cells are created by the tools and placed in Library objects.
 * <P>
 * The basic NodeProto has a list of varibales, a list of ports, the bounds and much more.
 */
public interface NodeProto
{
    /** Method to return NodeProtoId of this NodeProto.
     * NodeProtoId identifies NodeProto independently of threads.
     * @return NodeProtoId of this NodeProto.
     */
    public NodeProtoId getId();
    
	/**
	 * Method to return the function of this NodeProto.
	 * The Function is a technology-independent description of the behavior of this NodeProto.
	 * @return the function of this NodeProto.
	 */
	public PrimitiveNode.Function getFunction();

	/**
	 * Method to return the default width of this NodeProto.
	 * Cells return the actual width of the contents.
	 * PrimitiveNodes return the default width of new instances of this NodeProto.
	 * @return the width to use when creating new NodeInsts of this NodeProto.
	 */
	public double getDefWidth();

	/**
	 * Method to return the default height of this NodeProto.
	 * Cells return the actual height of the contents.
	 * PrimitiveNodes return the default height of new instances of this NodeProto.
	 * @return the height to use when creating new NodeInsts of this NodeProto.
	 */
	public double getDefHeight();

	/**
	 * Method to size offset of this NodeProto.
	 * @return the size offset of this NodeProto.  It is always zero for cells.
	 */
	public SizeOffset getProtoSizeOffset();

	/**
	 * Method to return the Technology to which this NodeProto belongs.
	 * For Cells, the Technology varies with the View and contents.
	 * For PrimitiveNodes, the Technology is simply the one that owns it.
	 * @return the Technology associated with this NodeProto.
	 */
	public Technology getTechnology();

	/**
	 * Method to find the PortProto that has a particular name.
	 * @return the PortProto, or null if there is no PortProto with that name.
	 */
	public PortProto findPortProto(String name);

	/**
	 * Method to find the PortProto that has a particular Name.
	 * @return the PortProto, or null if there is no PortProto with that name.
	 */
	public PortProto findPortProto(Name name);

	/**
	 * Method to return an iterator over all PortProtos of this NodeProto.
	 * @return an iterator over all PortProtos of this NodeProto.
	 */
	public Iterator<PortProto> getPorts();

	/**
	 * Method to return the number of PortProtos on this NodeProto.
	 * @return the number of PortProtos on this NodeProto.
	 */
	public int getNumPorts();

	/**
	 * Method to return the PortProto at specified position.
	 * @param portIndex specified position of PortProto.
	 * @return the PortProto at specified position..
	 */
	public PortProto getPort(int portIndex);

	/**
	 * Method to return the PortProto by thread-independent PortProtoId.
	 * @param portProtoId thread-independent PortProtoId.
	 * @return the PortProto.
	 */
	public PortProto getPort(PortProtoId portProtoId);

	/**
	 * Method to describe this NodeProto as a string.
	 * PrimitiveNodes may prepend their Technology name if it is
	 * not the current technology (for example, "mocmos:N-Transistor").
	 * Cells may prepend their Library if it is not the current library,
	 * and they will include view and version information
	 * (for example: "Wires:wire100{ic}").
     * @param withQuotes to wrap description between quotes
	 * @return a String describing this NodeProto.
	 */
	public String describe(boolean withQuotes);

	/**
	 * Method to return the name of this NodeProto.
	 * When this is a PrimitiveNode, the name is just its name in
	 * the Technology.
	 * When this is a Cell, the name is the pure cell name, without
	 * any view or version information.
	 * @return the prototype name of this NodeProto.
	 */
	public String getName();
}

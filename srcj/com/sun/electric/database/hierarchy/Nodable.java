/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Nodable.java
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
package com.sun.electric.database.hierarchy;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;

/**
 * This interface defines real or virtual instance of NodeProto in a Cell..
 */
public interface Nodable
{
	// ------------------------ public methods -------------------------------

	/**
	 * Routine to return the NodeUsage of this Nodable.
	 * @return the NodeUsage of this Nodable.
	 */
	public NodeUsage getNodeUsage();

	/**
	 * Routine to return the prototype of this Nodable.
	 * @return the prototype of this Nodable.
	 */
	public NodeProto getProto();

	/**
	 * Routine to return the Cell that contains this Nodable.
	 * @return the Cell that contains this Nodable.
	 */
	public Cell getParent();

	/**
	 * Routine to return the name of this Nodable.
	 * @return the name of this Nodable.
	 */
	public String getName();

	/**
	 * Routine to return the Name object of this Nodable.
	 * @return the name of this Nodable.
	 */
	public Name getNameLow();

	/**
	 * Routine to return the Variable on this Nodable with a given name.
	 * @param name the name of the Variable.
	 * @return the Variable with that name, or null if there is no such Variable.
	 */
	public Variable getVar(String name);

	/**
	 * Routine to get network by PortProto and bus index.
	 * @param portProto PortProto in protoType.
	 * @param busIndex index in bus.
	 */
	public JNetwork getNetwork(PortProto portProto, int busIndex);

	/**
	 * Returns a printable version of this Nodable.
	 * @return a printable version of this Nodable.
	 */
	public String toString();

}

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

import java.util.Iterator;

/**
 * This interface defines real or virtual instance of NodeProto in a Cell..
 */
public interface Nodable
{
	// ------------------------ public methods -------------------------------

	/**
	 * Method to return the prototype of this Nodable.
	 * @return the prototype of this Nodable.
	 */
	public NodeProto getProto();

	/**
	 * Method to return the number of actual NodeProtos which
	 * produced this Nodable.
	 * @return number of actual NodeProtos.
	 */
	public int getNumActualProtos();

	/**
	 * Method to return the i-th actual NodeProtos which produced
	 * this Nodable.
	 * @param i specified index of actual NodeProto.
	 * @return actual NodeProto.
	 */
	public NodeProto getActualProto(int i);

	/**
	 * Method to return the Cell that contains this Nodable.
	 * @return the Cell that contains this Nodable.
	 */
	public Cell getParent();

	/**
	 * Method to return the name of this Nodable.
	 * @return the name of this Nodable.
	 */
	public String getName();

	/**
	 * Method to return the name key of this Nodable.
	 * @return the name key of this Nodable.
	 */
	public Name getNameKey();

	/**
	 * Method to return the Variable on this Nodable with a given name.
	 * @param name the name of the Variable.
	 * @return the Variable with that name, or null if there is no such Variable.
	 */
	public Variable getVar(String name);

	/**
	 * Method to return the Variable on this Nodable with a given name.
	 * @param name the name of the Variable.
	 * @return the Variable with that name, or null if there is no such Variable.
	 */
	public Variable getVar(Variable.Key key);

	/**
	 * Method to return an iterator over all Variables on this Nodable.
	 * @return an iterator over all Variables on this Nodable.
	 */
	public Iterator getVariables();

	/**
	 * Returns a printable version of this Nodable.
	 * @return a printable version of this Nodable.
	 */
	public String toString();

    // JKG: trying this out
    /**
     * Returns true if this Nodable wraps NodeInst ni.
     * Note that this Nodable may actually *be* ni, or
     * it may simply wrap it and other NodeInsts and act
     * as a proxy.
     * @param ni a NodeInst
     * @return true if this Nodable contains ni, false otherwise
     */
    public boolean contains(NodeInst ni, int arrayIndex);

}

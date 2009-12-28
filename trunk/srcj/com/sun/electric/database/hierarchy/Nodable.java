/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Nodable.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.hierarchy;

import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;

import java.util.Iterator;

/**
 * This interface defines real or virtual instance of NodeProto in a Cell..
 */
public interface Nodable {
    // ------------------------ public methods -------------------------------

    /**
     * Method to return the prototype of this Nodable.
     * @return the prototype of this Nodable.
     */
    public NodeProto getProto();

    /**
     * Method to tell whether this Nodable is a cell instance.
     * @return true if this Nodable is a cell instance, false if it is a primitive
     */
    public boolean isCellInstance();

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
     * Method to return the Variable on this ElectricObject with a given key.
     * @param key the key of the Variable.
     * @return the Variable with that key, or null if there is no such Variable.
     */
    public Variable getVar(Variable.Key key);

//	/**
//	 * Method to return an iterator over all Variables on this Nodable.
//	 * @return an iterator over all Variables on this Nodable.
//	 */
//	public Iterator<Variable> getVariables();
    /**
     * Method to return the Parameter on this Nodable with the given key.
     * If the parameter is not found on this Nodable, it
     * is also searched for on the default var owner.
     * @param key the key of the Parameter
     * @return the Parameter with that key, that may exist either on this object
     * or the default owner.  Returns null if none found.
     */
    public Variable getParameter(Variable.Key key);

    /**
     * Method to return the Parameter or Variable on this Nodable with a given key.
     * @param key the key of the Parameter or Variable.
     * @return the Parameter or Variable with that key, or null if there is no such Parameter or Variable Variable.
     * @throws NullPointerException if key is null
     */
    public Variable getParameterOrVariable(Variable.Key key);

    /**
     * Method to tell if the Variable.Key is a defined parameters of this Nodable.
     * Parameters which are not defined on IconNodeInst take default values from Icon Cell.
     * @param key the key of the parameter
     * @return true if the key is a definded parameter of this Nodable
     */
    public boolean isDefinedParameter(Variable.Key key);

    /**
     * Method to return an Iterator over all Parameters on this Nodable.
     * This may also include any parameters on the defaultVarOwner object that are not on this Nodable.
     * @return an Iterator over all Parameters on this Nodable.
     */
    public Iterator<Variable> getParameters();

    /**
     * Method to return an Iterator over defined Parameters on this Nodable.
     * This doesn't include any parameters on the defaultVarOwner object that are not on this Nodable.
     * @return an Iterator over defined Parameters on this Nodable.
     */
    public Iterator<Variable> getDefinedParameters();

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

    /**
     * Get the NodeInst associated with this Nodable
     * @return the NodeInst associate with this Nodable
     */
    public NodeInst getNodeInst();

    /**
     * Get array index of this Nodable
     * @return the array index of this Nodable
     */
    public int getNodableArrayIndex();
}

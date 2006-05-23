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

import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;

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

	/**
	 * Method to return an iterator over all Variables on this Nodable.
	 * @return an iterator over all Variables on this Nodable.
	 */
	public Iterator<Variable> getVariables();

    /**
     * Method to return the Variable on this ElectricObject with the given key
     * that is a parameter.  If the variable is not found on this object, it
     * is also searched for on the default var owner.
     * @param key the key of the variable
     * @return the Variable with that key, that may exist either on this object
     * or the default owner.  Returns null if none found.
     */
    public Variable getParameter(Variable.Key key);

    /**
     * Method to return an Iterator over all Variables marked as parameters on this ElectricObject.
     * This may also include any parameters on the defaultVarOwner object that are not on this object.
     * @return an Iterator over all Variables on this ElectricObject.
     */
    public Iterator<Variable> getParameters();

    /**
     * Method to create a Variable on this ElectricObject with the specified values.
     * @param name the name of the Variable.
     * @param value the object to store in the Variable.
     * @return the Variable that has been created.
     */
    public Variable newVar(String name, Object value);

    /**
     * Method to create a Variable on this ElectricObject with the specified values.
     * @param key the key of the Variable.
     * @param value the object to store in the Variable.
     * @return the Variable that has been created.
     */
    public Variable newVar(Variable.Key key, Object value);

//    /**
//     * Method to put an Object into an entry in an arrayed Variable on this ElectricObject.
//     * @param key the key of the arrayed Variable.
//     * @param value the object to store in an entry of the arrayed Variable.
//     * @param index the location in the arrayed Variable to store the value.
//     */
//    public void setVar(Variable.Key key, Object value, int index);

    /**
     * Method to delete a Variable from this ElectricObject.
     * @param key the key of the Variable to delete.
     */
    public void delVar(Variable.Key key);

    /**
     * This method can be overridden by extending objects.
     * For objects (such as instances) that have instance variables that are
     * inherited from some Object that has the default variables, this gets
     * the object that has the default variables. From that object the
     * default values of the variables can then be found.
     * @return the object that holds the default variables and values.
     */
    public Cell getVarDefaultOwner();

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

}

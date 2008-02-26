/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IconNodeInst.java
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
package com.sun.electric.database.topology;

import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.variable.Variable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Class defines NodeInsts that are icons.
 */
class IconNodeInst extends NodeInst
{
	/**
	 * The constructor of IconNodeInst. Use the factory "newInstance" instead.
     * @param d persistent data of this IconNodeInst.
	 * @param parent the Cell in which this IconNodeInst will reside.
	 */
    IconNodeInst(ImmutableNodeInst d, Cell parent) {
        super(d, parent);
    }

 	/**
	 * Method to add a Variable on this ElectricObject.
	 * It may add a repaired copy of this Variable in some cases.
	 * @param var Variable to add.
	 */
    @Override
	public void addVar(Variable var) {
        Cell icon = (Cell)getProto();
        Variable iconParam = icon.getParameter(var.getKey());
        if (iconParam != null)
            var = composeInstParam(iconParam, var);
        super.addVar(var);
    }

	/**
	 * Method to return the Variable on this ElectricObject with a given key and type.
	 * @param key the key of the Variable. Returns null if key is null.
	 * @param type the required type of the Variable. Ignored if null.
	 * @return the Variable with that key and type, or null if there is no such Variable
	 * or default Variable value.
	 */
    @Override
	public Variable getVar(Variable.Key key, Class type)
	{
		checkExamine();
		if (key == null) return null;
		Variable instVar = getD().getVar(key);
        if (instVar == null)
            return null;
        Cell icon = (Cell)getProto();
        Variable iconParam = icon.getParameter(key);
        if (iconParam != null)
            instVar = composeInstParam(iconParam, instVar);
        // null type means any type
        return type == null || type.isInstance(instVar.getObject()) ? instVar : null;
	}

	/**
	 * Method to return an Iterator over all Variables on this ElectricObject.
	 * @return an Iterator over all Variables on this ElectricObject.
	 */
    @Override
	public synchronized Iterator<Variable> getVariables() {
        Cell icon = (Cell)getProto();
        if (!icon.hasParameters())
            return super.getVariables();

        ArrayList<Variable> vars = new ArrayList<Variable>();
        for (Iterator<Variable> it = super.getVariables(); it.hasNext(); ) {
            Variable instVar = it.next();
            Variable iconParam = icon.getParameter(instVar.getKey());
            if (iconParam != null)
                instVar = composeInstParam(iconParam, instVar);
            vars.add(instVar);
        }
        return vars.iterator();
    }

	/**
	 * Method to return the number of Variables on this ElectricObject.
	 * @return the number of Variables on this ElectricObject.
	 */
    @Override
	public synchronized int getNumVariables() {
        return super.getNumVariables();
    }

    /**
     * Method to return the Parameter on this IconNodeInst with the given key.
     * If the Parameter is not found on this IconNodeInst, it
     * is also searched for on the default var owner.
     * @param key the key of the parameter
     * @return the Parameter with that key, that may exist either on this IconNodeInst
     * or the default owner.  Returns null if none found.
     */
    @Override
    public Variable getParameter(Variable.Key key) {
        Cell icon = (Cell)getProto();
        Variable iconParam = icon.getParameter(key);
        if (iconParam == null) return null;
		Variable instVar = getD().getVar(key);
        return composeInstParam(iconParam, instVar);
    }

    /**
     * Method to tell if the Variable.Key is a defined parameters of this IconNodeInst.
     * Parameters which are not defined on IconNodeInst take default values from Icon Cell.
     * @param key the key of the parameter
     * @return true if the key is a definded parameter of this IconNodeInst
     */
    @Override
    public boolean isDefinedParameter(Variable.Key key) {
        return isParam(key) && getD().getVar(key) != null;
    }

    /**
     * Method to return an Iterator over all Parameters on this IconNodeInst.
     * This may also include any parameters on the defaultVarOwner object that are not on this object.
     * @return an Iterator over all Parameters on this IconNodeInst.
     */
    @Override
    public Iterator<Variable> getParameters() {
        Cell icon = (Cell)getProto();
        if (!icon.hasParameters())
            return ArrayIterator.emptyIterator();

        ArrayList<Variable> params = new ArrayList<Variable>();
        // get all parameters on this object
        for (Iterator<Variable> it = icon.getParameters(); it.hasNext(); ) {
            Variable iconParam = it.next();
            Variable instVar = getD().getVar(iconParam.getKey());
            params.add(composeInstParam(iconParam, instVar));
        }
        return params.iterator();
    }

    /**
     * Method to return an Iterator over defined Parameters on this IconNodeInst.
     * This doesn't include any parameters on the defaultVarOwner object that are not on this object.
     * @return an Iterator over defined Parameters on this IconNodeInst.
     */
    public Iterator<Variable> getDefinedParameters() {
        Cell icon = (Cell)getProto();
        if (!icon.hasParameters())
            return ArrayIterator.emptyIterator();

        ArrayList<Variable> params = new ArrayList<Variable>();
        // get all parameters on this object
        for (Iterator<Variable> it = icon.getParameters(); it.hasNext(); ) {
            Variable iconParam = it.next();
            Variable instVar = getD().getVar(iconParam.getKey());
            if (instVar == null) continue;
            params.add(composeInstParam(iconParam, instVar));
        }
        return params.iterator();
    }

    /**
     * Method to delete a defined Parameter from this IconNodeInst.
     * The Parameter becomes a default parameter with value inherited from the default owner
     * @param key the key of the Variable to delete.
     */
    public void delParameter(Variable.Key key) {
        delVar(key);
    }

	/**
	 * Method to return true if the Variable on this NodeInst with given key is a parameter.
	 * Parameters are those Variables that have values on instances which are
	 * passed down the hierarchy into the contents.
     * @param varKey key to test
	 * @return true if the Variable with given key is a parameter.
	 */
    @Override
    public boolean isParam(Variable.Key varKey) {
        Cell icon = (Cell)getProto();
        return icon != null && icon.getParameter(varKey) != null;
    }

    private static Variable composeInstParam(Variable iconParam, Variable instVar) {
        boolean display = !iconParam.isInterior();
        iconParam = iconParam.withInherit(false).withInterior(false).withDisplay(display);
        if (instVar != null)
            iconParam = iconParam.withObject(instVar.getObject());
        return iconParam;
    }
}

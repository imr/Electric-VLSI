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

import com.sun.electric.database.ImmutableIconInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.variable.Variable;

import java.util.ArrayList;
import java.util.Iterator;

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
     * Returns persistent data of this IconNodeInst.
     * @return persistent data of this IconNodeInst.
     */
    @Override
    public ImmutableIconInst getD() { return (ImmutableIconInst)super.getD(); }

 	/**
	 * Method to add a Variable on this IconNodeInst.
	 * It may add a repaired copy of this Variable in some cases.
	 * @param var Variable to add.
	 */
    @Override
	public void addVar(Variable var) {
        Cell icon = (Cell)getProto();
        Variable iconParam = icon.getParameter(var.getKey());
        if (iconParam != null) {
            // ToDo: delete
            var = composeInstParam(iconParam, var);
            if (setD(getD().withParam(var), true))
                // check for side-effects of the change
                checkPossibleVariableEffects(var.getKey());
        } else {
            super.addVar(var.withParam(false).withInherit(false));
        }
    }

	/**
	 * Method to delete a Variable from this IconNodeInst.
	 * @param key the key of the Variable to delete.
	 */
	public void delVar(Variable.Key key)
	{
        // ToDo: delete
        if (key.isAttribute())
            delParameter((Variable.AttrKey)key);
        super.delVar(key);
	}

	/**
	 * Method to return the Variable on this ElectricObject with a given key.
	 * @param key the key of the Variable.
	 * @return the Variable with that key and type, or null if there is no such Variable
	 * or default Variable value.
     * @throws NullPointerException if key is null
	 */
    @Override
	public Variable getVar(Variable.Key key)
	{
		checkExamine();
        if (key.isAttribute()) {
            // ToDo: delete
            Variable param = getParameter((Variable.AttrKey)key);
            if (param != null)
                return param;
        }
		return getD().getVar(key);
	}

	/**
	 * Method to return the Parameter or Variable on this ElectricObject with a given key.
	 * @param key the key of the Parameter or Variable.
	 * @return the Parameter or Variable with that key, or null if there is no such Parameter or Variable Variable.
     * @throws NullPointerException if key is null
	 */
    @Override
	public Variable getParameterOrVariable(Variable.Key key) {
        checkExamine();
        if (key.isAttribute()) {
            Variable param = getParameter((Variable.AttrKey)key);
            if (param != null)
                return param;
        }
		return getD().getVar(key);
	}

	/**
	 * Method to return an Iterator over all Variables on this ElectricObject.
	 * @return an Iterator over all Variables on this ElectricObject.
	 */
    @Override
	public synchronized Iterator<Variable> getVariables() {
        if (getD().getNumDefinedParameters() == 0)
            return super.getVariables();

        ArrayList<Variable> vars = new ArrayList<Variable>();
        for (Iterator<Variable> it = getDefinedParameters(); it.hasNext(); )
            vars.add(it.next());
        for (Iterator<Variable> it = super.getVariables(); it.hasNext(); ) {
            vars.add(it.next());
        }
        return vars.iterator();
    }

	/**
	 * Method to return the number of Variables on this ElectricObject.
	 * @return the number of Variables on this ElectricObject.
	 */
    @Override
	public synchronized int getNumVariables() {
        return super.getNumVariables() + getD().getNumDefinedParameters();
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
        if (!(key instanceof Variable.AttrKey))
            return null;
        Variable instParam = getD().getDefinedParameter((Variable.AttrKey)key);
        if (instParam != null)
            return instParam;
        Cell icon = (Cell)getProto();
        Variable iconParam = icon.getParameter(key);
        return iconParam != null ? composeInstParam(iconParam, null) : null;
    }

    /**
     * Method to tell if the Variable.Key is a defined parameters of this IconNodeInst.
     * Parameters which are not defined on IconNodeInst take default values from Icon Cell.
     * @param key the key of the parameter
     * @return true if the key is a definded parameter of this IconNodeInst
     */
    @Override
    public boolean isDefinedParameter(Variable.Key key) {
        if (!(key instanceof Variable.AttrKey))
            return false;
        return getD().getDefinedParameter((Variable.AttrKey)key) != null;
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
            Variable instVar = getD().getDefinedParameter((Variable.AttrKey)iconParam.getKey());
            params.add(composeInstParam(iconParam, instVar));
        }
        return params.iterator();
    }

    /**
     * Method to return an Iterator over defined Parameters on this IconNodeInst.
     * This doesn't include any parameters on the defaultVarOwner object that are not on this object.
     * @return an Iterator over defined Parameters on this IconNodeInst.
     */
    @Override
    public Iterator<Variable> getDefinedParameters() {
        return getD().getDefinedParameters();
    }

    /**
     * Method to add a Parameter to this NodeInst.
     * Overridden in IconNodeInst
     * @param key the key of the Variable to delete.
     */
    public void addParameter(Variable param) {
        if (param.getTextDescriptor().isParam() && setD(getD().withParam(param), true))
            // check for side-effects of the change
            checkPossibleVariableEffects(param.getKey());
    }

    /**
     * Method to delete a defined Parameter from this IconNodeInst.
     * The Parameter becomes a default parameter with value inherited from the default owner
     * @param key the key of the Variable to delete.
     */
    @Override
    public void delParameter(Variable.Key key) {
        if (key instanceof Variable.AttrKey && setD(getD().withoutParam((Variable.AttrKey)key), true))
            // check for side-effects of the change
            checkPossibleVariableEffects(key);
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

//    public void addParam(Variable var) {
//        assert var.getTextDescriptor().isParam() && var.isInherit();
//        if (isIcon()) {
//            // Remove variables with the same name as new parameter
//            for (Iterator<NodeInst> it = getInstancesOf(); it.hasNext(); ) {
//                NodeInst ni = it.next();
//                ni.delVar(var.getKey());
//            }
//        }
//        setD(getD().withoutVariable(var.getKey()).withParam(var));
//    }
//
    private static Variable composeInstParam(Variable iconParam, Variable instVar) {
        boolean display = !iconParam.isInterior();
        if (instVar != null)
            return instVar.withParam(true).withInherit(false).withInterior(false).withDisplay(display).withUnit(iconParam.getUnit());
        return iconParam.withInherit(false).withInterior(false).withDisplay(display);
    }
}

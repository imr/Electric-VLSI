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
	 * Method to add a Variable on this ElectricObject.
	 * It may add a repaired copy of this Variable in some cases.
	 * @param var Variable to add.
	 */
    @Override
	public void addVar(Variable var) {
        Cell icon = (Cell)getProto();
        if (VIRTUAL_PARAMETERS && icon.hasParameters()) {
            Variable param = icon.getParameter(var.getKey());
            if (param != null)
                var = param.withObject(var.getObject());
        }
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
		Variable var;
		synchronized(this) {
			var = getD().getVar(key);
		}
        Cell icon = (Cell)getProto();
        if (VIRTUAL_PARAMETERS && icon.hasParameters()) {
            Variable iconParam = icon.getParameter(key);
            if (iconParam != null) {
                var = var != null ? iconParam.withObject(var.getObject()) : iconParam;
            }
        }
		if (var != null) {
			if (type == null) return var;				   // null type means any type
			if (type.isInstance(var.getObject())) return var;
		}
		return null;
	}

	/**
	 * Method to return an Iterator over all Variables on this ElectricObject.
	 * @return an Iterator over all Variables on this ElectricObject.
	 */
    @Override
	public synchronized Iterator<Variable> getVariables() {
        Cell icon = (Cell)getProto();
        Iterator<Variable> instVariables = super.getVariables();
        if (!VIRTUAL_PARAMETERS || !icon.hasParameters())
            return instVariables;
        Iterator<Variable> iconParameters = icon.getParameters();
        Variable instVariable = instVariables.hasNext() ? instVariables.next() : null;
        ArrayList<Variable> vars = new ArrayList<Variable>();
        for (Iterator<Variable> iconIt = icon.getParameters(); iconIt.hasNext(); ) {
            Variable param = iconIt.next();
            Variable.Key paramKey = param.getKey();
            while (instVariable != null && instVariable.getKey().compareTo(paramKey) < 0) {
                vars.add(instVariable);
                instVariable = instVariables.hasNext() ? instVariables.next() : null;
            }
            if (instVariable != null && instVariable.getKey() == paramKey) {
                param = param.withObject(instVariable.getObject());
                instVariable = instVariables.hasNext() ? instVariables.next() : null;
            }
            vars.add(param);
        }
        if (instVariable != null) {
            vars.add(instVariable);
            while (instVariables.hasNext())
                vars.add(instVariables.next());
        }
        return vars.iterator();
    }

	/**
	 * Method to return the number of Variables on this ElectricObject.
	 * @return the number of Variables on this ElectricObject.
	 */
    @Override
	public synchronized int getNumVariables() {
        Cell icon = (Cell)getProto();
        int numVariables = super.getNumVariables();
        if (VIRTUAL_PARAMETERS && icon.hasParameters()) {
            for (Iterator<Variable> it = icon.getParameters(); it.hasNext(); ) {
                Variable var = it.next();
                if (super.getVar(var.getKey()) == null)
                    numVariables++;
            }
        }
        return numVariables;
    }
}

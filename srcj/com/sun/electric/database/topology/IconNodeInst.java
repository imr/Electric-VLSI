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
package com.sun.electric.database.topology;

import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;

/**
 * Class defines NodeInsts that are icons.
 */
public class IconNodeInst extends NodeInst
{
	/**
	 * The constructor of IconNodeInst. Use the factory "newInstance" instead.
     * @param d persistent data of this IconNodeInst.
	 * @param parent the Cell in which this IconNodeInst will reside.
	 */
    IconNodeInst(ImmutableNodeInst d, Cell parent) {
        super(d, parent);
    }
    
    public Variable newVar(Variable.Key key, Object value, TextDescriptor td)
    {
        Variable var = super.newVar(key, value, td);
        parent.newVar(key, value, td);
        return var;
    }

    /**
     * Rename a Variable. Note that this creates a new variable of
     * the new name and copies all values from the old variable, and
     * then deletes the old variable.
     * @param name the name of the var to rename
     * @param newName the new name of the variable
     * @return the new renamed variable
     */
    public Variable renameVar(String name, String newName) {
        parent.renameVar(name, newName);
        return (super.renameVar(name, newName));
    }

    /**
	 * Method to update a Variable on this ElectricObject with the specified values.
	 * If the Variable already exists, only the value is changed; the displayable attributes are preserved.
	 * @param key the key of the Variable.
	 * @param value the object to store in the Variable.
	 * @return the Variable that has been updated.
	 */
	public Variable updateVar(Variable.Key key, Object value) {
        parent.updateVar(key, value);
        return super.updateVar(key, value);
    }

    /**
	 * Overwriting NodeInst.setTextDescriptor for handling icons.
	 * @param varName name of variable or special name.
	 * @param td new value TextDescriptor
	 */
//	public void setTextDescriptor(String varName, TextDescriptor td)
//    {
//        // td is cloned inside setTextDescriptor
//        parent.setTextDescriptor(varName, td);
//        super.setTextDescriptor(varName, td);
//    }

//    public void setVar(Variable.Key key, Object value, int index)
//    {
//        System.out.println("Overwrite setVar");
//    }
}

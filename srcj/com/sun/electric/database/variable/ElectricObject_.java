/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ElectricObject_.java
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

package com.sun.electric.database.variable;

import com.sun.electric.database.ImmutableElectricObject;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;

/**
 *
 */
public abstract class ElectricObject_ extends ElectricObject {

    private ImmutableElectricObject immutable = ImmutableElectricObject.EMPTY;
    
    /** Creates a new instance of ElectricObject_ */
    protected ElectricObject_() {
    }
    
    /**
     * Returns persistent data of this ElectricObject with Variables.
     * @return persistent data of this ElectricObject.
     */
    public ImmutableElectricObject getImmutable() { return immutable; }
    
    public void lowLevelModifyVariables(ImmutableElectricObject newImmutable) { this.immutable = immutable; }
        
 	/**
	 * Method to add a Variable on this ElectricObject.
     * It may add repaired copy of this Variable in some cases.
	 * @param var Variable to add.
	 */
    public void addVar(Variable var) {
        if (!(this instanceof Cell))
            var = var.withParam(false);
        
        checkChanging();
        ImmutableElectricObject oldImmutable = immutable;
        immutable = immutable.withVariable_(var);
        if (immutable == oldImmutable) return;
        if (isDatabaseObject())
            Undo.modifyVariables(this, oldImmutable);
    }

	/**
	 * Method to delete a Variable from this NodeInst.
	 * @param key the key of the Variable to delete.
	 */
	public void delVar(Variable.Key key)
	{
		checkChanging();
        ImmutableElectricObject oldImmutable = immutable;
        immutable = immutable.withoutVariable_(key);
        if (immutable == oldImmutable) return;
		if (isDatabaseObject())
			Undo.modifyVariables(this, oldImmutable);
	}
    
}

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
    
    /**
     * Changes persistent data of this ElectricObject with Variables.
     * @param immutable new persistent data of this ElectricObject.
     */
    protected void setImmutable(ImmutableElectricObject immutable) { this.immutable = immutable; }
    
    /**
     * Updates persistent data of this ElectricObject by adding specified Variable.
     * @param var Variable to add.
     * @return updated persistent data.
     */
    protected ImmutableElectricObject withVariable(Variable var) {
        immutable = immutable.withVariable_(var);
        return immutable;
    }
    
    /**
     * Updates persistent data of this ElectricObject by removing Variable with specified key.
     * @param key key to remove.
     * @return updated persistent data.
     */
    protected ImmutableElectricObject withoutVariable(Variable.Key key) {
        immutable = immutable.withoutVariable_(key);
        return immutable;
    }
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableCell.java
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
package com.sun.electric.database;

import com.sun.electric.database.variable.Variable;

/**
 * Immutable class ImmutableCell represents a cell.
 */
public class ImmutableCell extends ImmutableElectricObject {
    
	/** CellId of this ImmutableCell. */                        public final CellId cellId;
    
	/**
	 * The private constructor of ImmutableCell. Use the factory "newInstance" instead.
     * @param cellId id of this ImmutableCell.
     * @param vars array of Variables of this ImmutableCell
	 */
     private ImmutableCell(CellId cellId, Variable[] vars) {
        super(vars);
        this.cellId = cellId;
        check();
    }

	/**
	 * Returns new ImmutableCell object.
     * @param cellId id of this ImmutableCell.
	 * @return new ImmutableCell object.
	 * @throws NullPointerException if cellId is null.
	 */
    public static ImmutableCell newInstance(CellId cellId) {
        if (cellId == null) throw new NullPointerException("cellId");
		return new ImmutableCell(cellId, Variable.NULL_ARRAY);
    }

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by additional Variable.
     * If this ImmutableCell has Variable with the same key as new, the old variable will not be in new
     * ImmutableCell.
	 * @param var additional Variable.
	 * @return ImmutableCell with additional Variable.
	 * @throws NullPointerException if var is null
	 */
    public ImmutableCell withVariable(Variable var) {
        Variable[] vars = arrayWithVariable(var);
        if (this.getVars() == vars) return this;
		return new ImmutableCell(this.cellId, vars);
    }
    
	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by removing Variable
     * with the specified key. Returns this ImmutableCell if it doesn't contain variable with the specified key.
	 * @param key Variable Key to remove.
	 * @return ImmutableCell without Variable with the specified key.
	 * @throws NullPointerException if key is null
	 */
    public ImmutableCell withoutVariable(Variable.Key key) {
        Variable[] vars = arrayWithoutVariable(key);
        if (this.getVars() == vars) return this;
		return new ImmutableCell(this.cellId, vars);
    }
    
	/**
	 * Checks invariant of this ImmutableCell.
	 * @throws AssertionError if invariant is broken.
	 */
	public void check() {
        check(true);
        assert cellId != null;
	}
}

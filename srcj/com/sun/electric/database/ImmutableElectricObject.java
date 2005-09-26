/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableNodeInst.java
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

import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.variable.Variable;
import java.util.Arrays;

import java.util.Iterator;
import java.util.SortedSet;

/**
 * This immutable class is the base class of all Electric immutable objects that can be extended with "ImmutableVariables".
 */
public class ImmutableElectricObject {
    
    public final static ImmutableElectricObject EMPTY = new ImmutableElectricObject(ImmutableVariable.NULL_ARRAY); 
    
    /** array of immutable variables sorted by their keys. */
    private final ImmutableVariable[] vars;
    
	/**
	 * The package-private constructor of ImmutableElectricObject.
     * Use the factory "newInstance" instead.
     * @param vars array of ImmutableVariable sorted by their keys.
	 */
    ImmutableElectricObject(ImmutableVariable[] vars) {
        this.vars = vars;
        check();
    }
    
	/**
	 * Returns new ImmutableElectricObject object.
     * @param varSet a SortedSet of ImmutableVariables with natural ordering.
	 * @return new ImmutableElectricObject.
	 * @throws NullPointerException if varSet is null.
     * @throws IllegalArgumentException if varSet has not natural ordering.
     * @throws    ArrayStoreException varType contains elements that are not ImmuatbleVariable.
	 */
    public static ImmutableElectricObject newInstance(SortedSet/*<ImmutableVariable>*/ varSet) {
        if (varSet.comparator() != null) throw new IllegalArgumentException("need natural ordering");
        return new ImmutableElectricObject((ImmutableVariable[])varSet.toArray(ImmutableVariable.NULL_ARRAY));
    }

	/**
	 * Returns ImmutableElectricObject which differs from this ImmutableElectricObject by additional ImmutableVariable.
     * If this ImmutableElectricObject has ImmutableVariable with the same key as new, the old variable will not be in new
     * ImmutableElectricObject.
	 * @param var additional ImmutableVariable.
	 * @return ImmutableElectricObject with additional ImmutableVariable.
	 * @throws NullPointerException if var is null
	 */
    public ImmutableElectricObject withVariable(ImmutableVariable var) {
        int varIndex = searchVar(var.key);
        int newLength = vars.length;
        if (varIndex < 0) {
            varIndex = ~varIndex;
            newLength++;
        } else if (vars[varIndex] == var) return this;
        ImmutableVariable[] newVars = new ImmutableVariable[newLength];
        System.arraycopy(vars, 0, newVars, 0, varIndex);
        newVars[varIndex] = var;
        int tailLength = newLength - (varIndex + 1);
        System.arraycopy(vars, vars.length - tailLength, newVars, varIndex + 1, tailLength);
        return new ImmutableElectricObject(newVars);
    }
    
	/**
	 * Returns ImmutableElectricObject which differs from this ImmutableElectricObject by removing ImmutableVariable
     * with the specified key. Returns this ImmutableElectricObject if it doesn't contain variable with the specified key.
	 * @param key Variable Key to remove.
	 * @return ImmutableElectricObject without ImmutableVariable with the specified key.
	 * @throws NullPointerException if var is null
	 */
    public ImmutableElectricObject withoutVariable(Variable.Key key) {
        int varIndex = searchVar(key);
        if (varIndex < 0) return this;
        if (vars.length == 1 && varIndex == 0) return EMPTY;
        ImmutableVariable[] newVars = new ImmutableVariable[vars.length - 1];
        System.arraycopy(vars, 0, newVars, 0, varIndex);
        System.arraycopy(vars, varIndex + 1, newVars, varIndex, newVars.length - varIndex);
        return new ImmutableElectricObject(newVars);
    }
    
	/**
	 * Method to return the ImmutableVariable on this ImmuatbleElectricObject with a given key.
	 * @param key the key of the Variable.
	 * @return the Variable with that key, or null if there is no such Variable.
	 */
	public ImmutableVariable getVar(Variable.Key key)
	{
        int varIndex = searchVar(key);
        return varIndex >= 0 ? vars[varIndex] : null;
	}

	/**
	 * Method to return an Iterator over all ImmutableVariables on this ImmutableElectricObject.
	 * @return an Iterator over all ImmutableVariables on this ImmutableElectricObject.
	 */
	public Iterator getVariables() { return ArrayIterator.iterator(vars); }

	/**
	 * Method to return an array of all ImmutableVariables on this ImmutableElectricObject.
	 * @return an array of all ImmutableVariables on this ImmutableElectricObject.
	 */
	public ImmutableVariable[] toVariableArray() {
        return vars.length == 0 ? vars : (ImmutableVariable[])vars.clone();
    }

	/**
	 * Method to return the number of ImmutableVariables on this ImmutableElectricObject.
	 * @return the number of ImmutableVariables on this ImmutableElectricObject.
	 */
	public int getNumVariables() { return vars.length; }

	/**
	 * Method to return the ImmutableVariable by its varIndex.
     * @param varIndex index of ImmutableVariable.
	 * @return the ImmutableVariable with given varIndex.
     * @throws ArrayIndexOutOfBoundesException if varIndex out of bounds.
	 */
	public ImmutableVariable getVar(int varIndex) { return vars[varIndex]; }

    /**
     * Searches the variables for the specified variable key using the binary
     * search algorithm.
     * @param key the variable key to be searched.
     * @return index of the search variable, if it is contained in the vars;
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       NodeInst would be inserted into the list: the index of the first
     *	       element greater than the key, or <tt>nodes.size()</tt>, if all
     *	       elements in the list are less than the specified name.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the ImmutableVariable is found.
     */
	public int searchVar(Variable.Key key) { return searchVar(vars, key); }

    /**
     * Searches the ordered array of variables for the specified variable key using the binary
     * search algorithm.
     * @param vars the ordered array of variables.
     * @param key the variable key to be searched.
     * @return index of the search variable, if it is contained in the vars;
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       NodeInst would be inserted into the list: the index of the first
     *	       element greater than the key, or <tt>nodes.size()</tt>, if all
     *	       elements in the list are less than the specified name.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the ImmutableVariable is found.
     */
	public static int searchVar(ImmutableVariable[] vars, Variable.Key key)
	{
        int low = 0;
        int high = vars.length-1;
		while (low <= high) {
			int mid = (low + high) >> 1; // try in a middle
			ImmutableVariable var = vars[mid];
			int cmp = var.key.compareTo(key);

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // ImmutableVariable found
		}
		return -(low + 1);  // ImmutableVariable not found.
    }

	/**
	 * Checks invariant of this ImmutableElectricObject.
	 * @throws AssertionError if invariant is broken.
	 */
	public void check() {
        if (vars.length == 0)
            assert vars == ImmutableVariable.NULL_ARRAY;
        else {
            vars[0].check();
            for (int i = 1; i < vars.length; i++) {
                vars[i].check();
                assert vars[i - 1].key.compareTo(vars[i].key) < 0;
            }
        }
    }
}

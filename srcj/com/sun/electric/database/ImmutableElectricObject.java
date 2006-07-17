/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableElectricObject.java
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
import java.io.IOException;

import java.util.Iterator;

/**
 * This immutable class is the base class of all Electric immutable objects that can be extended with Variables.
 */
public abstract class ImmutableElectricObject {
    
    /** array of variables sorted by their keys. */
    private final Variable[] vars;
	/** flags of this IimmutableElectricObject. */
    public final int flags;
    
	/**
	 * The package-private constructor of ImmutableElectricObject.
     * Use the factory "newInstance" instead.
     * @param vars array of Variables sorted by their keys.
     * @param flags flags of this IimmutableElectricObject.
	 */
    ImmutableElectricObject(Variable[] vars, int flags) {
        this.vars = vars;
        this.flags = flags;
    }

	/**
	 * Returns array of Variables which differs from array of this ImmutableElectricObject by additional Variable.
     * If this ImmutableElectricObject has Variable with the same key as new, the old variable will not be in new array.
	 * @param var additional Variable.
	 * @return array of Variables with additional Variable.
	 * @throws NullPointerException if var is null
	 */
    Variable[] arrayWithVariable(Variable var) {
        int varIndex = searchVar(var.getKey());
        int newLength = vars.length;
        if (varIndex < 0) {
            varIndex = ~varIndex;
            newLength++;
        } else if (vars[varIndex] == var) return vars;
        Variable[] newVars = new Variable[newLength];
        System.arraycopy(vars, 0, newVars, 0, varIndex);
        newVars[varIndex] = var;
        int tailLength = newLength - (varIndex + 1);
        System.arraycopy(vars, vars.length - tailLength, newVars, varIndex + 1, tailLength);
        return newVars;
    }
    
	/**
	 * Returns array of Variable which differs from array of this ImmutableElectricObject by removing Variable
     * with the specified key. Returns array of this ImmutableElectricObject if it doesn't contain variable with the specified key.
	 * @param key Variable Key to remove.
	 * @return array of Variables without Variable with the specified key.
	 * @throws NullPointerException if key is null
	 */
    Variable[] arrayWithoutVariable(Variable.Key key) {
        int varIndex = searchVar(key);
        if (varIndex < 0) return vars;
        if (vars.length == 1 && varIndex == 0) return Variable.NULL_ARRAY;
        Variable[] newVars = new Variable[vars.length - 1];
        System.arraycopy(vars, 0, newVars, 0, varIndex);
        System.arraycopy(vars, varIndex + 1, newVars, varIndex, newVars.length - varIndex);
        return newVars;
    }
    
	/**
	 * Returns array of Variable which differs from array of this ImmutableElectricObject by renamed Ids.
     * Returns array of this ImmutableElectricObject if it doesn't contain reanmed Ids.
	 * @param idMapper a map from old Ids to new Ids.
     * @return array of Variable with renamed Ids.
	 */
    Variable[] arrayWithRenamedIds(IdMapper idMapper) {
        Variable[] newVars = null;
        for (int i = 0; i < vars.length; i++) {
            Variable oldVar = vars[i];
            Variable newVar = oldVar.withRenamedIds(idMapper);
            if (newVar != oldVar && newVars == null) {
                newVars = new Variable[vars.length];
                System.arraycopy(vars, 0, newVars, 0, i);
            }
            if (newVars != null)
                newVars[i] = newVar;
        }
        return newVars != null ? newVars : vars;
    }
    
	/**
	 * Method to return the Variable on this ImmuatbleElectricObject with a given key.
	 * @param key the key of the Variable.
	 * @return the Variable with that key, or null if there is no such Variable.
	 * @throws NullPointerException if key is null
	 */
	public Variable getVar(Variable.Key key)
	{
        int varIndex = searchVar(key);
        return varIndex >= 0 ? vars[varIndex] : null;
	}

	/**
	 * Method to return an Iterator over all Variables on this ImmutableElectricObject.
	 * @return an Iterator over all Variables on this ImmutableElectricObject.
	 */
	public Iterator<Variable> getVariables() { return ArrayIterator.iterator(vars); }

	/**
	 * Method to return an array of all Variables on this ImmutableElectricObject.
	 * @return an array of all Variables on this ImmutableElectricObject.
	 */
	public Variable[] toVariableArray() {
        return vars.length == 0 ? vars : (Variable[])vars.clone();
    }

	/**
	 * Method to return the number of Variables on this ImmutableElectricObject.
	 * @return the number of Variables on this ImmutableElectricObject.
	 */
	public int getNumVariables() { return vars.length; }

	/**
	 * Method to return the Variable by its varIndex.
     * @param varIndex index of Variable.
	 * @return the Variable with given varIndex.
     * @throws ArrayIndexOutOfBoundesException if varIndex out of bounds.
	 */
	public Variable getVar(int varIndex) { return vars[varIndex]; }

	/**
	 * The package-private method to get Variable array.
     * @return Variable array of this ImmutableElectricObject.
	 */
    Variable[] getVars() { return vars; }

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
     *	       and only if the Variable is found.
	 * @throws NullPointerException if key is null
     */
	public int searchVar(Variable.Key key) {
        if (key == null) throw new NullPointerException("key");
        return searchVar(vars, key);
    }

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
     *	       and only if the Variable is found.
     */
	private static int searchVar(Variable[] vars, Variable.Key key)
	{
        int low = 0;
        int high = vars.length-1;
		while (low <= high) {
			int mid = (low + high) >> 1; // try in a middle
			Variable var = vars[mid];
			int cmp = var.getKey().compareTo(key);

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // Variable found
		}
		return -(low + 1);  // Variable not found.
    }
    
    /**
     * Writes optional variable part of this ImmutableElectricObject.
     * @param writer where to write.
     */
    void write(SnapshotWriter writer) throws IOException {
        boolean hasVars = vars.length > 0;
        writer.writeBoolean(hasVars);
        if (hasVars)
            writeVars(writer);
    }
    
    /**
     * Writes variables of this ImmutableElectricObject.
     * @param writer where to write.
     */
    void writeVars(SnapshotWriter writer) throws IOException {
        writer.writeInt(vars.length);
        for (int i = 0; i < vars.length; i++)
            vars[i].write(writer);
    }
    
    /**
     * Reads variables of this ImmutableElectricObject.
     * @param reader where to read.
     */
    static Variable[] readVars(SnapshotReader reader) throws IOException {
        int length = reader.readInt();
        if (length == 0) return Variable.NULL_ARRAY;
        Variable[] vars = new Variable[length];
        for (int i = 0; i < length; i++)
            vars[i] = Variable.read(reader);
        return vars;
    }
    
    /**
     * Return a hash code value for fields of this object.
     * Variables of objects are not compared
     */
    public abstract int hashCodeExceptVariables();

    /**
     * Indicates whether fields of other ImmutableElectricObject are equal to fileds of this object.
     * Variables of objects are not compared.
     * @param o other ImmutableElectricObject.
     * @return true if fields of objects are equal.
     */
    public abstract boolean equalsExceptVariables(ImmutableElectricObject o);
    
    /**
	 * Checks invariant of this ImmutableElectricObject.
     * @param paramAllowed true if Variables with parameter flag are allowed on this ImmutableElectricObject
	 * @throws AssertionError if invariant is broken.
	 */
	void check(boolean paramAllowed) {
        if (vars.length == 0) return;
        vars[0].check(paramAllowed);
        for (int i = 1; i < vars.length; i++) {
            vars[i].check(paramAllowed);
            assert vars[i - 1].getKey().compareTo(vars[i].getKey()) < 0;
        }
    }
}

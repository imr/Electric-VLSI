/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableIconInst.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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
package com.sun.electric.database;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.id.NodeProtoId;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;

import java.io.IOException;
import java.util.Iterator;

/**
 *
 */
public class ImmutableIconInst extends ImmutableNodeInst {

    /** Parameters of this ImmutableIconInst. */
    final Variable[] params;

    /**
     * The private constructor of ImmutableIconInst. Use the factory "newInstance" instead.
     * @param nodeId id of this NodeInst in parent.
     * @param protoId the NodeProtoId of which this is an instance.
     * @param name name of new ImmutableIconInst.
     * @param nameDescriptor TextDescriptor of name of this ImmutableIconInst.
     * @param orient Orientation of this ImmutableIconInst.
     * @param anchor the anchor location of this ImmutableIconInst.
     * @param width the width of this ImmutableIconInst.
     * @param height the height of this ImmutableIconInst.
     * @param flags flag bits for thisImmutableNdoeIsnt.
     * @param techBits tech speicfic bits of this ImmutableIconInst.
     * @param protoDescriptor TextDescriptor of prototype name of this ImmutableIconInst
     * @param vars array of Variables of this ImmutableIconInst
     */
    ImmutableIconInst(int nodeId, NodeProtoId protoId, Name name, TextDescriptor nameDescriptor,
            Orientation orient, EPoint anchor, EPoint size,
            int flags, byte techBits, TextDescriptor protoDescriptor,
            Variable[] vars, ImmutablePortInst[] ports, Variable[] params) {
        super(nodeId, protoId, name, nameDescriptor, orient, anchor, size, flags, techBits, protoDescriptor, vars, ports);
        this.params = params;
//        check();
    }

    /**
     * Method to return the Parameter on this ImmuatbleIconInst with a given key.
     * @param key the key of the Variable.
     * @return the Parameter with that key, or null if there is no such Variable.
     * @throws NullPointerException if key is null
     */
    public Variable getDefinedParameter(Variable.AttrKey key) {
        int paramIndex = searchVar(params, key);
        return paramIndex >= 0 ? params[paramIndex] : null;
    }

    /**
     * Method to return an Iterator over all Parameters on this ImmutableIconInst.
     * @return an Iterator over all Parameters on this ImmutableIconInst.
     */
    public Iterator<Variable> getDefinedParameters() {
        return ArrayIterator.iterator(params);
    }

    /**
     * Method to return the number of Parameters on this ImmutableIconInst.
     * @return the number of Parametes on this ImmutableIconInst.
     */
    public int getNumDefinedParameters() {
        return params.length;
    }

//	/**
//	 * Method to return the Parameter by its paramIndex.
//     * @param paramIndex index of Parameter.
//	 * @return the Parameter with given paramIndex.
//     * @throws ArrayIndexOutOfBoundesException if paramIndex out of bounds.
//	 */
//	public Variable getParameter(int paramIndex) { return params[paramIndex]; }
    /**
     * Returns ImmutableIconInst which differs from this ImmutableIconInst by additional parameter.
     * If this ImmutableIconInst has parameter with the same key as new, the old variable will not be in new
     * ImmutableIconInst.
     * @param var additional Variable.
     * @return ImmutableIconInst with additional Variable.
     * @throws NullPointerException if var is null
     */
    public ImmutableIconInst withParam(Variable var) {
        if (!var.getTextDescriptor().isParam()) {
            throw new IllegalArgumentException("Variable " + var + " is not param");
        }
        if (searchVar(var.getKey()) >= 0) {
            throw new IllegalArgumentException(this + " has variable with the same name as parameter " + var);
        }
        Variable[] params = arrayWithVariable(this.params, var.withInherit(false));
        if (this.params == params) {
            return this;
        }
        return new ImmutableIconInst(this.nodeId, this.protoId, this.name, this.nameDescriptor,
                this.orient, this.anchor, this.size, this.flags, this.techBits, this.protoDescriptor,
                getVars(), this.ports, params);
    }

    /**
     * Returns ImmutableIconInst which differs from this ImmutableIconInst by removing parameter
     * with the specified key. Returns this ImmutableIconInst if it doesn't contain parameter with the specified key.
     * @param key Variable Key to remove.
     * @return ImmutableIconInst without Variable with the specified key.
     * @throws NullPointerException if key is null
     */
    public ImmutableIconInst withoutParam(Variable.AttrKey key) {
        Variable[] params = arrayWithoutVariable(this.params, key);
        if (this.params == params) {
            return this;
        }
        return new ImmutableIconInst(this.nodeId, this.protoId, this.name, this.nameDescriptor,
                this.orient, this.anchor, this.size, this.flags, this.techBits, this.protoDescriptor,
                getVars(), this.ports, params);
    }

    /**
     * Returns ImmutableIconInst which differs from this ImmutableIconInst by additional Variable.
     * If this ImmutableNideInst has Variable with the same key as new, the old variable will not be in new
     * ImmutableNodeInst.
     * @param var additional Variable.
     * @return ImmutableNodeInst with additional Variable.
     * @throws NullPointerException if var is null
     * @throws IllegalArgumentException if this ImmutableIconInst has a parameter with the same name as new variable
     */
    @Override
    public ImmutableNodeInst withVariable(Variable var) {
        if (var.getTextDescriptor().isParam()) {
            throw new IllegalArgumentException("Variable " + var + " is param");
        }
        if (var.isAttribute() && searchVar(params, var.getKey()) >= 0) {
            throw new IllegalArgumentException(var + " is already parameter");
        }
        return super.withVariable(var);
    }

    /**
     * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by renamed Ids.
     * @param idMapper a map from old Ids to new Ids.
     * @return ImmutableNodeInst with renamed Ids.
     */
    @Override
    ImmutableNodeInst withRenamedIds(IdMapper idMapper) {
        CellId newProtoId = idMapper.get((CellId) protoId);
        if (!newProtoId.isIcon() && params != Variable.NULL_ARRAY) {
            throw new IllegalArgumentException("Icon params");
        }
        return super.withRenamedIds(idMapper);
    }

    @Override
    Variable[] getDefinedParams() {
        return params;
    }

    /**
     * Writes this ImmutableIconInst to IdWriter.
     * @param writer where to write.
     */
    @Override
    void write(IdWriter writer) throws IOException {
        super.write(writer);
        writeVars(params, writer);
    }

    /**
     * Indicates whether fields of other ImmutableElectricObject are equal to fields of this object.
     * Variables of objects are not compared.
     * @param o other ImmutableElectricObject.
     * @return true if fields of objects are equal.
     */
    @Override
    public boolean equalsExceptVariables(ImmutableElectricObject o) {
        return super.equalsExceptVariables(o) && params == ((ImmutableIconInst) o).params;
    }

    /**
     * Checks invariant of this ImmutableIconInst.
     * @throws AssertionError if invariant is broken.
     */
    @Override
    public void check() {
        super.check();
        for (int i = 0; i < params.length; i++) {
            Variable param = params[i];
            param.check(true, false);
            assert param.getTextDescriptor().isParam() && !param.getTextDescriptor().isInherit();
            if (i > 0) {
                assert params[i - 1].getKey().compareTo(param.getKey()) < 0;
            }
            assert searchVar(param.getKey()) < 0;
        }
        if (params.length > 0) {
            for (Variable var : getVars()) {
                if (var.isAttribute()) {
                    assert searchVar(params, var.getKey()) < 0;
                }
            }
        } else {
            assert params == Variable.NULL_ARRAY;
        }
    }
}

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
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.id.NodeProtoId;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;

import java.io.IOException;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 */
public class ImmutableIconInst extends ImmutableNodeInst {
    public SortedMap<Variable.AttrKey,Object> params;

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
            Variable[] vars, ImmutablePortInst[] ports, SortedMap<Variable.AttrKey,Object> params) {
        super(nodeId, protoId, name, nameDescriptor, orient, anchor, size, flags, techBits, protoDescriptor, vars, ports);
        this.params = params;
        check();
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
        if (var.isAttribute() && params.get(var.getKey()) != null)
            throw new IllegalArgumentException(var + " is already parameter");
        return super.withVariable(var);
    }

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by renamed Ids.
	 * @param idMapper a map from old Ids to new Ids.
     * @return ImmutableNodeInst with renamed Ids.
	 */
    @Override
    ImmutableNodeInst withRenamedIds(IdMapper idMapper) {
        CellId newProtoId = idMapper.get((CellId)protoId);
        if (!newProtoId.isIcon() && !params.isEmpty())
            throw new IllegalArgumentException("Icon params");
        return super.withRenamedIds(idMapper);
    }

	/**
	 * Method to return the defined parameters on this ImmutableIconInst.
     * This is a map from Variable.Key to objects allowed as parameter values.
	 * @return the defined parameters on this ImmutableNodeInst.
	 */
    @Override
    public SortedMap<Variable.AttrKey,Object> getDefinedParams() {
        return params;
    }

	/**
	 * Returns Map of defined parameters which differs from Map of defined parameters of this ImmutableIconInst by renamed Ids.
     * Returns Map of defined parameters of this ImmutableIconInst if it doesn't contain renamed Ids.
	 * @param idMapper a map from old Ids to new Ids.
     * @return Map of defined parametes with renamed Ids.
	 */
    @Override
    SortedMap<Variable.AttrKey,Object> getDefinedParamsWithRenamedIds(IdMapper idMapper) {
        boolean renamed = false;
        for (Object o: params.values()) {
            if (Variable.paramValueWithRenamedIds(idMapper, o) != o) {
                renamed = true;
                break;
            }
        }
        if (!renamed)
            return params;
        TreeMap<Variable.AttrKey,Object> renamedParams = new TreeMap<Variable.AttrKey,Object>();
        for (SortedMap.Entry<Variable.AttrKey,Object> e: params.entrySet()) {
            Object renamedValue = Variable.paramValueWithRenamedIds(idMapper, e.getValue());
            renamedParams.put(e.getKey(), renamedValue);
        }
        return Collections.unmodifiableSortedMap(renamedParams);
    }

    /**
     * Writes this ImmutableIconInst to IdWriter.
     * @param writer where to write.
     */
    @Override
    void write(IdWriter writer) throws IOException {
        super.write(writer);
        writer.writeInt(params.size());
        for (SortedMap.Entry<Variable.AttrKey,Object> e: params.entrySet()) {
            writer.writeVariableKey(e.getKey());
            Variable.writeParamValue(writer, e.getValue());
        }
    }

    static SortedMap<Variable.AttrKey,Object> readParams(IdReader reader) throws IOException {
        int length = reader.readInt();
        if (length == 0) return EMPTY_PARAMS;
        TreeMap<Variable.AttrKey,Object> params = new TreeMap<Variable.AttrKey,Object>();
        for (int i = 0; i < length; i++) {
            Variable.AttrKey paramKey = (Variable.AttrKey)reader.readVariableKey();
            Object value = Variable.readParamValue(reader);
            params.put(paramKey, value);
        }
        return Collections.unmodifiableSortedMap(params);
    }

    /**
     * Indicates whether fields of other ImmutableElectricObject are equal to fileds of this object.
     * Variables of objects are not compared.
     * @param o other ImmutableElectricObject.
     * @return true if fields of objects are equal.
     */
    @Override
    public boolean equalsExceptVariables(ImmutableElectricObject o) {
        return super.equals(o) && params.equals(((ImmutableIconInst)o).params);
    }

    /**
	 * Checks invariant of this ImmutableIconInst.
	 * @throws AssertionError if invariant is broken.
	 */
    @Override
	public void check() {
        super.check();
        if (params.isEmpty())
            assert params == EMPTY_PARAMS;
        for (SortedMap.Entry<Variable.AttrKey,Object> e: params.entrySet()) {
            Variable.Key paramKey = e.getKey();
            Object paramValue = e.getValue();
            assert getVar(paramKey) == null;
            assert Variable.isParamValue(paramValue);
        }
	}
}

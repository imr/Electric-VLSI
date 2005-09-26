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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.variable.ImmutableTextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Tool;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Immutable class ImmutableVariable an Electric variable.
 */
public final class ImmutableVariable {
    
    /** empty array of ImmutableVariables. */
    public static final ImmutableVariable[] NULL_ARRAY = {};
    
    private final static byte ARRAY = 1;
    private final static byte SIMPLE = 0;
    private final static byte LIBRARY = 2;
    private final static byte CELL = 4;
    private final static byte EXPORT = 6;
    /** Valid type of value. */
    private static final HashMap/*<Class,Byte>*/ validClasses = new HashMap/*<Class,Byte>*/();

    
    /** key of this ImmutableVariable. */                           public final Variable.Key key;
	/** Text descriptor of this ImmutableVariable. */               public final ImmutableTextDescriptor descriptor;
	/** Value of this ImmutableVariable. */							private final Object value;
                                                                    private final byte type;
    
    /**
     * Creates a new instance of ImmutableVariable.
     * @param key key of this ImmutableVariable.
     * @param descriptor text descriptor of this ImmutableVariable.
     * @param value value of this ImmutableVariable.
     * @param type type of the value
     */
    private ImmutableVariable(Variable.Key key, ImmutableTextDescriptor descriptor, Object value, byte type) {
        this.key = key;
        this.descriptor = descriptor;
        this.value = value;
        this.type = type;
        check();
    }
    
	/**
	 * Returns new ImmutableVariable object.
     * @param key key of this ImmutableVariable.
     * @param descriptor text descriptor of this ImmutableVariable.
     * @param value value of this ImmutableVariable.
	 * @return new ImmutableVariable object.
	 * @throws NullPointerException if key, descriptor or value is null.
     * @throws IllegalArgumentException if value has invalid type
	 */
    public static ImmutableVariable newInstance(Variable.Key key, ImmutableTextDescriptor descriptor, Object value) {
        if (key == null) throw new NullPointerException("key");
        if (descriptor == null) throw new NullPointerException("descriptor");
        byte type;
        if (value instanceof Object[]) {
            Byte typeByte = (Byte)validClasses.get(value.getClass().getComponentType());
            if (typeByte != null) {
                value = ((Object[])value).clone();
                type = (byte)(typeByte.byteValue()|ARRAY);
            } else if (value instanceof Library[]) {
                Library[] libs = (Library[])value;
                LibId[] libIds = new LibId[libs.length];
                for (int i = 0; i < libs.length; i++)
                    if (libs[i] != null) libIds[i] = libs[i].getId();
                value = libIds;
                type = LIBRARY|ARRAY;
            } else if (value instanceof Cell[]) {
                Cell[] cells = (Cell[])value;
                CellId[] cellIds = new CellId[cells.length];
                for (int i = 0; i < cells.length; i++)
                    if (cells[i] != null) cellIds[i] = (CellId)cells[i].getId();
                value = cellIds;
                type = CELL|ARRAY;
            } else if (value instanceof Export[]) {
                Export[] exports = (Export[])value;
                ExportId[] exportIds = new ExportId[exports.length];
                for (int i = 0; i < exports.length; i++)
                    if (exports[i] != null) exportIds[i] = (ExportId)exports[i].getId();
                value = exportIds;
                type = EXPORT|ARRAY;
            } else if (value instanceof Point2D[]) {
                Point2D[] points = (Point2D[])value;
                EPoint[] epoints = new EPoint[points.length];
                for (int i = 0; i < points.length; i++)
                    if (points[i] != null) epoints[i] = EPoint.snap(points[i]);
                value = epoints;
                type = SIMPLE|ARRAY;
            } else {
                throw new IllegalArgumentException(value.getClass().toString());
            }
        } else {
            Byte typeByte = (Byte)validClasses.get(value.getClass());
            if (typeByte != null) {
                type = typeByte.byteValue();
            } else if (value instanceof Library) {
                value = ((Library)value).getId();
                type = LIBRARY;
            } else if (value instanceof Cell) {
                value = ((Cell)value).getId();
                type = CELL;
            } else if (value instanceof Export) {
                value = ((Export)value).getId();
                type = EXPORT;
            } else if (value instanceof Point2D) {
                value = EPoint.snap((Point2D)value);
                type = SIMPLE;
            } else {
                throw new IllegalArgumentException(value.getClass().toString());
            }
        }
        if (descriptor.isCode() && !(value instanceof String || value instanceof String[])) {
            descriptor = descriptor.withoutCode();
        }
		return new ImmutableVariable(key, descriptor, value, type);
    }
    
	/**
	 * Returns ImmutableVariable which differs from this ImmutableVariable by name descriptor.
     * @param nameDescriptor TextDescriptor of name
	 * @return ImmutableVariable which differs from this ImmutableVariable by name descriptor.
	 */
	public ImmutableVariable withDescriptor(ImmutableTextDescriptor descriptor) {
        if (this.descriptor == descriptor) return this;
        if (descriptor.isCode() && !(value instanceof String || value instanceof String[])) {
            descriptor = descriptor.withoutCode();
        }
		return new ImmutableVariable(this.key, descriptor, this.value, this.type);
	}

	/**
	 * Returns ImmutableVariable which differs from this ImmutableVariable by value.
     * @param value new value
	 * @return ImmutableVariable which differs from this ImmutableVariable by value.
	 * @throws NullPointerException if value is null.
     * @throws IllegalArgumentException if value has invalid type
	 */
	public ImmutableVariable withValue(Object value) {
        if (this.value.equals(value)) return this;
        if ((type & ARRAY) != 0 && value instanceof Object[] &&
                Arrays.equals((Object[])this.value, (Object[])value) &&
                this.value.getClass().getComponentType() == value.getClass().getComponentType())
            return this;
        return newInstance(this.key, this.descriptor, value);
	}

    /**
     * Returns thread-independent value of this Variable.
     * @return thread-independent value of this variable.
     */
    public Object getValue() {
        return (type & ARRAY) != 0 ? ((Object[])value).clone() : value;
    }
    
    /**
     * Returns value of this Variable in current thread.
     * @return value of this variable in current thread.
     */
    public Object getValueInCurrentThread() {
        switch (type) {
            case SIMPLE: return value;
            case LIBRARY: return ((LibId)value).inCurrentThread();
            case CELL: return ((CellId)value).inCurrentThread();
            case EXPORT: return ((ExportId)value).inCurrentThread();
            case SIMPLE|ARRAY: return ((Object[])value).clone();
            case LIBRARY|ARRAY:
                LibId[] libIds = (LibId[])value;
                Library[] libs = new Library[libIds.length];
                for (int i = 0; i < libIds.length; i++)
                    if (libIds[i] != null) libs[i] = libIds[i].inCurrentThread();
                return libs;
            case CELL|ARRAY:
                CellId[] cellIds = (CellId[])value;
                Cell[] cells = new Cell[cellIds.length];
                for (int i = 0; i < cellIds.length; i++)
                    if (cellIds[i] != null) cells[i] = (Cell)cellIds[i].inCurrentThread();
                return cells;
            case EXPORT|ARRAY:
                ExportId[] exportIds = (ExportId[])value;
                Export[] exports = new Export[exportIds.length];
                for (int i = 0; i < exportIds.length; i++)
                    if (exportIds[i] != null) exports[i] = (Export)exportIds[i].inCurrentThread();
                return exports;
        }
        throw new AssertionError();
    }

    /**
     * Returns length of value array or -1 if value is scalar.
     * @return length of value array or -1.
     */
    public int getValueLength() { return (type & ARRAY) != 0 ? ((Object[])value).length : -1; }
    
    /**
     * Returns thread-independent element of array value of this Variable.
     * @param specified index of array
     * @return element of array value.
     * @throws ArrayIndexOutOfBoundsException if index is scalar of value is out of bounds.
     */
    public Object getValue(int index) {
        if ((type & ARRAY) == 0) throw new ArrayIndexOutOfBoundsException(index); 
        return ((Object[])value)[index];
    }
    
    /**
     * Returns element of array value of this Variable in current thread.
     * @param specified index of array
     * @return element of array value.
     * @throws ArrayIndexOutOfBoundsException if index is scalar of value is out of bounds.
     */
    public Object getValueInCurrentThread(int index) {
        switch (type) {
            case SIMPLE|ARRAY: return ((Object[])value)[index];
            case LIBRARY|ARRAY:
                LibId libId = ((LibId[])value)[index];
                return libId != null ? libId.inCurrentThread() : null;
            case CELL|ARRAY:
                CellId cellId = ((CellId[])value)[index];
                return cellId != null ? cellId.inCurrentThread() : null;
            case EXPORT|ARRAY:
                ExportId exportId = ((ExportId[])value)[index];
                return exportId != null ? exportId.inCurrentThread() : null;
            default:
                throw new ArrayIndexOutOfBoundsException(index);
        }
    }
    /**
	 * Checks invariant of this ImmutableVariable.
	 * @throws AssertionError or NullPointerException if invariant is broken.
	 */
	public void check() {
		assert key != null;
        assert descriptor != null;
        assert value != null;
        if (value instanceof Object[]) {
            Byte typeByte = (Byte)validClasses.get(value.getClass().getComponentType());
            assert type == (byte)(typeByte.byteValue()|ARRAY);
        } else {
            Byte typeByte = (Byte)validClasses.get(value.getClass());
            assert type == typeByte.byteValue();
        }
        if (descriptor.isCode())
            assert value instanceof String || value instanceof String[];
	}
    
    static {
        validClasses.put(String.class, new Byte(SIMPLE));
        validClasses.put(Double.class, new Byte(SIMPLE));
        validClasses.put(Float.class, new Byte(SIMPLE));
        validClasses.put(Long.class, new Byte(SIMPLE));
        validClasses.put(Integer.class, new Byte(SIMPLE));
        validClasses.put(Short.class, new Byte(SIMPLE));
        validClasses.put(Byte.class, new Byte(SIMPLE));
        validClasses.put(Boolean.class, new Byte(SIMPLE));
        validClasses.put(EPoint.class, new Byte(SIMPLE));
        validClasses.put(Tool.class, new Byte(SIMPLE));
        validClasses.put(Technology.class, new Byte(SIMPLE));
        validClasses.put(PrimitiveNode.class, new Byte(SIMPLE));
        validClasses.put(ArcProto.class, new Byte(SIMPLE));
        validClasses.put(LibId.class, new Byte(LIBRARY));
        validClasses.put(CellId.class, new Byte(CELL));
        validClasses.put(ExportId.class, new Byte(EXPORT));
    }
}



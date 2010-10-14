/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Parameter.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.util.config.model;

/**
 * @author fschmidt
 * 
 */
public class Parameter {

    public enum Attributes {
        name, ref, value, type;
    }

    public enum Type {
        String, Integer, Double, Boolean
    }

    private String name;
    private String ref;
    private String value;
    private Type type;

    /**
     * @param name
     * @param ref
     * @param value
     * @param type
     */
    public Parameter(String name, String ref, String value, Type type) {
        super();
        this.name = name;
        this.ref = ref;
        this.value = value;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Object createObject() {
        Object obj = null;

        if (this.ref == null) {
            obj = createByType(type, value);
        } else {
            
        }

        return obj;
    }

    private Object createByType(Type type, String value) {

        if (type.equals(Type.Boolean)) {
            return Boolean.parseBoolean(value);
        } else if(type.equals(Type.Double)) {
            return Double.parseDouble(value);
        } else if(type.equals(Type.Integer)) {
            return Integer.parseInt(value);
        } else if(type.equals(Type.String)) {
            return value;
        }

        return null;
    }

}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LEInst.java
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

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.technology.PrimitiveNode;

/**
 * Class to describe an instance in Logical Effort.
 */
public class LEInst {

    public static final Variable.Key ATTR_LEGATE = Variable.newKey("ATTR_LEGATE");
    public static final Variable.Key ATTR_LEKEEPER = Variable.newKey("ATTR_LEKEEPER");
    public static final Variable.Key ATTR_LEWIRE = Variable.newKey("ATTR_LEWIRE");
    public static final Variable.Key ATTR_LESETTINGS = Variable.newKey("ATTR_LESETTINGS");
    public static final Variable.Key ATTR_LEIGNORE = Variable.newKey("ATTR_LEIGNORE");

    /** Type is a typesafe enum class that describes the type of Instance this is */
    public static class Type {
        private final String name;
        private Type(String name) { this.name = name; }
        public String toString() { return name; }

        /** LeGate */       public static final Type LEGATE = new Type("LE Gate");
        /** LeKeeper */     public static final Type LEKEEPER = new Type("LE Keeper");
        /** NotSizeable */  public static final Type WIRE = new Type("Wire");
        /** NotSizeable */  public static final Type TRANSISTOR = new Type("Transistor");
        /** NotSizeable */  public static final Type CAPACITOR = new Type("Capacitor");
        /** Ingore */       public static final Type IGNORE = new Type("LE Ingore");
        /** NotSizeable */  public static final Type LESETTINGS = new Type("LE Settings");
        /** NotSizeable */  public static final Type UNKNOWN = new Type("Unknown");
    }

    // ------------------------- Netlisting ------------------------------------

    /**
     * Get the LENodable type of this Nodable. If it is not a valid type, return Type.UNKNOWN.
     * @param no the Nodable to examine
     * @param context the current VarContext
     * @return the LENodable type
     */
    public static Type getType(Nodable no, VarContext context) {

        if (isVarValueOne(no, context, ATTR_LEGATE)) {
            return Type.LEGATE;
        }
        else if (isVarValueOne(no, context, ATTR_LEKEEPER)) {
            return Type.LEKEEPER;
        }
        else if (isVarValueOne(no, context, ATTR_LEWIRE)) {
            return Type.WIRE;
        }
        else if ((no.getProto() != null) && (no.getProto().getFunction().isTransistor())) {
            return Type.TRANSISTOR;
        }
        else if ((no.getProto() != null) && (no.getProto().getFunction() == PrimitiveNode.Function.CAPAC)) {
            return Type.CAPACITOR;
        }
        else if (isVarValueOne(no, context, ATTR_LEIGNORE)) {
            return Type.IGNORE;
        }
        else if (isVarValueOne(no, context, ATTR_LESETTINGS)) {
            return Type.LESETTINGS;
        }
        return Type.UNKNOWN;
    }

    // return true if the var "name" evaluates to the integer value 1
    private static boolean isVarValueOne(Nodable no, VarContext context, Variable.Key key) {
        Variable var = no.getParameter(key);
        if (var == null) return false;
        int val = VarContext.objectToInt(context.evalVar(var), 1);
        if (val == 1) return true;
        return false;
    }


}

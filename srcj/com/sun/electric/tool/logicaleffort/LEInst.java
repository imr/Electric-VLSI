package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.technology.PrimitiveNode;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 4, 2005
 * Time: 11:28:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class LEInst {

    public static final Variable.Key ATTR_LEGATE = ElectricObject.newKey("ATTR_LEGATE");
    public static final Variable.Key ATTR_LEKEEPER = ElectricObject.newKey("ATTR_LEKEEPER");
    public static final Variable.Key ATTR_LEWIRE = ElectricObject.newKey("ATTR_LEWIRE");
    public static final Variable.Key ATTR_LESETTINGS = ElectricObject.newKey("ATTR_LESETTINGS");
    public static final Variable.Key ATTR_LEIGNORE = ElectricObject.newKey("ATTR_LEIGNORE");

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
        Variable var = no.getParameter(key.getName());
        if (var == null) return false;
        int val = VarContext.objectToInt(context.evalVar(var), 1);
        if (val == 1) return true;
        return false;
    }


}

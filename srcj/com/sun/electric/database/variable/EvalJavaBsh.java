/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ElectricObject.java
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
/*
 * EvalJavaBsh.java
 *
 * Created on November 6, 2003, 10:26 AM
 */

package com.sun.electric.database.variable;

import com.sun.electric.database.variable.VarContext;

import bsh.Interpreter;
import bsh.InterpreterError;

import java.util.regex.*;

/**
 * Used for evaluating Java expressions in Variables
 * It is meant to be invoked from the Variable context;
 * these methods should not be used from other contexts, and
 * thus are declared protected.
 *
 * @author  gainsley
 */
public class EvalJavaBsh {
    
    // ------------------------ private data ------------------------------------
    
    /** The bean shell interpreter */               private static Interpreter env = init();
    /** For replacing @variable */ private static final Pattern atPat = Pattern.compile("@(\\w+)");
    
    // ------------------------ private and protected methods -------------------
    
    /** the contructor is not used */
    private EvalJavaBsh() {
    }
    
    /** Initialize bean shell */
    protected static Interpreter init() {
        env = new Interpreter();
        try {
            
            env.eval("Object P(String par) { return context.lookupVarEval(par); }");
            env.eval("Object PAR(String par) { return context.lookupVarFarEval(par); }");
            
        } catch (bsh.EvalError e) {
            handleBshError(e);
        }
        return env;
    }
    
    /** Handle errors.  Sends it to system.out */
    protected static void handleBshError(bsh.EvalError e) {
        System.out.println("Bean Shell eval error: " + e.getMessage());
    }
    
    /** Get the interpreter so other tools may add methods to it. There is only
     * one interpreter, so be careful that separate tools do not conflict in 
     * terms of namespace.  I recommend when adding objects or methods to the
     * Interpreter you prepend the object or method names with the Tool name.
     */
    public static Interpreter getInterpreter() { return env; }
    
    /** Evaluate Object as if it were a String containing java code.
     * @param obj the object to be evaluated (toString() must apply).
     * @param context the context in which the object will be evaluated.
     * @param info used to pass additional info from Electric to the interpreter, if needed.
     * @return the evaluated object.
     */
    protected static Object eval(Object obj, VarContext context, Object info) {
        try {
            if (context == null) context = VarContext.globalContext;
            env.set("context", context);            // set context
            env.set("info", info);                  // set info
            String expr = replace(obj.toString());  // change @var calls to P(var)
            Object ret = env.eval(expr);            // ask bsh to eval
            env.unset("context");                   // remove context
            env.unset("info");                      // remove info
            return ret;
        } catch (bsh.EvalError e) {
            handleBshError(e);
        }
        return null;
    }
    
    /**
     * Replaces @var calls to P("var")
     * @param expr the expression
     * @return replaced expression
     */
    protected static String replace(String expr) {
       Matcher mat = atPat.matcher(expr);       
       StringBuffer sb = new StringBuffer();
       while(mat.find()) {
           mat.appendReplacement(sb, "P(\"ATTR_"+mat.group(1)+"\")");
       }
       mat.appendTail(sb);
       return sb.toString();
    }
}

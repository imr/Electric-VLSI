/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EvalJavaBsh.java
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

package com.sun.electric.database.variable;

import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;

import bsh.Interpreter;
import bsh.InterpreterError;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Stack;

/**
 * Used for evaluating Java expressions in Variables
 * It is meant to be invoked from the Variable context;
 * these methods should not be used from other contexts, and
 * thus are declared protected.
 *
 * @author  gainsley
 */
public class EvalJavaBsh
{
    
    // ------------------------ private data ------------------------------------

	/** the singleton object of this class. */		private static EvalJavaBsh thisOne = new EvalJavaBsh();
	/** The bean shell interpreter */               private static Interpreter env;
    /** For replacing @variable */					private static final Pattern atPat = Pattern.compile("@(\\w+)");
    /** For replacing @variable */					private static final Pattern pPat = Pattern.compile("(P|PAR)\\(\"(\\w+)\"\\)");

    /** Context stack for recursive eval calls */   private static Stack contextStack = new Stack();
    /** Info stack for recursive eval calls */      private static Stack infoStack = new Stack();

    /** turn on Bsh verbose debug stmts */          private static boolean debug = true;

    // ------------------------ private and protected methods -------------------

    /** the contructor */
    private EvalJavaBsh()
    {
        env = new Interpreter();
        try {
            env.set("evalJavaBsh", this);
            env.eval("Object P(String par) { return evalJavaBsh.P(par); }");
            env.eval("Object PAR(String par) { return evalJavaBsh.PAR(par); }");
            
            // the following is for running scripts
            env.eval("import com.sun.electric.tool.user.MenuCommands;");
            env.eval("import com.sun.electric.database.hierarchy.*;");
            env.eval("import com.sun.electric.database.prototype.*;");
            env.eval("import com.sun.electric.database.topology.*;");
            env.eval("import com.sun.electric.database.variable.ElectricObject;");
            env.eval("import com.sun.electric.database.variable.FlagSet;");
            env.eval("import com.sun.electric.database.variable.TextDescriptor;");
            env.eval("import com.sun.electric.database.variable.VarContext;");
            env.eval("import com.sun.electric.database.variable.Variable;");
            // do not import variable.EvalJavaBsh, because calling EvalJavaBsh.runScript
            // will spawn jobs in an unexpected order
            env.eval("import com.sun.electric.tool.io.*;");

        } catch (bsh.EvalError e) {
            handleBshError(e);
        }
    }
	public static void setEnv(String name, Tool tool)
	{
        try {            
            env.set(name, tool);
        } catch (bsh.EvalError e) {
            System.out.println("Tool " + tool.getName() + ": Bean shell error: "+e.getMessage());
        }
	}

    /** Handle errors.  Sends it to system.out */
    public static void handleBshError(bsh.EvalError e) {
        //System.out.println("Bean Shell eval error: " + e.getMessage());
        if (debug) {
            if (e instanceof bsh.TargetError) {
                bsh.TargetError te = (bsh.TargetError)e;
                Throwable t = te.getTarget();
                System.out.println("  Exception thrown: "+t+"; "+t.getMessage());
                e.printStackTrace(System.out);
            }
        }
    }

    /** Get the interpreter so other tools may add methods to it. There is only
     * one interpreter, so be careful that separate tools do not conflict in 
     * terms of namespace.  I recommend when adding objects or methods to the
     * Interpreter you prepend the object or method names with the Tool name.
     */
    public static Interpreter getInterpreter() { return env; }

    /**
     * See what the current context of eval is.
     * @return a VarContext.
     */
    public static VarContext getCurrentContext() { return (VarContext)contextStack.peek(); }

    /**
     * See what the current info of eval is.
     * @return an Object.
     */
    public static Object getCurrentInfo() { return infoStack.peek(); }

    /**
     * Replaces @var calls to P("var")
     * Replaces P("var") calls to P("ATTR_var")
     * Replaces PAR("var") calls to PAR("ATTR_var")
     * @param expr the expression
     * @return replaced expression
     */
    protected static String replace(String expr) {
       StringBuffer sb = new StringBuffer();
       Matcher atMat = atPat.matcher(expr); 
       while(atMat.find()) {
           atMat.appendReplacement(sb, "P(\""+atMat.group(1)+"\")");
       }
       atMat.appendTail(sb);

       expr = sb.toString();
       sb = new StringBuffer();
       Matcher pMat = pPat.matcher(expr);
       while(pMat.find()) {
           if (pMat.group(2).startsWith("ATTR_"))
                pMat.appendReplacement(sb, pMat.group(0));
           else
                pMat.appendReplacement(sb, pMat.group(1)+"(\"ATTR_"+pMat.group(2)+"\")");
       }
       pMat.appendTail(sb);
       
       return sb.toString();
    }

    /** Evaluate Object as if it were a String containing java code.
     * Note that this function may call itself recursively.
     * @param obj the object to be evaluated (toString() must apply).
     * @param context the context in which the object will be evaluated.
     * @param info used to pass additional info from Electric to the interpreter, if needed.
     * @return the evaluated object.
     */
    public static Object eval(Object obj, VarContext context, Object info) {
        String expr = replace(obj.toString());  // change @var calls to P(var)
        try {
            if (context == null) context = VarContext.globalContext;
            contextStack.push(context);             // push context
            infoStack.push(info);                   // push info
			Object ret = env.eval(expr);            // ask bsh to eval
            contextStack.pop();                     // pop context
            infoStack.pop();                        // pop info
            //System.out.println("BSH: "+expr.toString()+" --> "+ret);
            return ret;
        } catch (bsh.EvalError e) {
            // filter out Operator: "'x'" inappropriate for objects, and
            // "illegal use of null value or null literal messages
            //Matcher mat = errorFilter.matcher(e.getMessage());
            //Matcher mat2 = errorFilter2.matcher(e.getMessage());
            //if (!mat.matches() && !mat2.matches())
                handleBshError(e);
        }
        return null;
    }
    //private static final Pattern errorFilter = Pattern.compile(".*?Operator:.*?inappropriate for objects.*");
    //private static final Pattern errorFilter2 = Pattern.compile(".*?illegal use of null value or 'null' literal.*");

    //------------------Methods that may be called through Interpreter--------------

    /** Lookup variable for evaluation
     * @return an evaluated object
     */
    public Object P(String name) {
        VarContext context = (VarContext)contextStack.peek();
        return context.lookupVarEval(name);
    }

	public Object PAR(String name) {
        VarContext context = (VarContext)contextStack.peek();
        return context.lookupVarFarEval(name);
    }

	//---------------------------Running Scripts-------------------------------------

	/** Run a Java Bean Shell script */
    public static void runScript(String script) {
        runScriptJob job = new runScriptJob(script);
    }

	static class runScriptJob extends Job
    {
        String script;
        protected runScriptJob(String script) {
            super("JavaBsh script: "+script, User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.script = script;
            this.startJob();
        }

        public void doIt() {
            try {
                env.source(script);
            } catch (bsh.EvalError e) {
                handleBshError(e);
            } catch (FileNotFoundException e) {
                System.out.println("Cannot run Java Bean Shell script '"+script+"': "+e.getMessage());
            } catch (IOException e) {
                System.out.println("IO Error trying to run Java Bean Shell script '"+script+"': "+e.getMessage());
            }
        }
    }

}

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

import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;

import bsh.Interpreter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
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

	private static final boolean EXTERNAL_BSH = true;

    // ------------------------ private data ------------------------------------

	/** the singleton object of this class. */		private static EvalJavaBsh thisOne = new EvalJavaBsh();
	/** The bean shell interpreter */               private static Interpreter env;

	/** The bean shell interpreter object */        private static Object envObject;
	/** The bean shell interpreter eval method */   private static Method evalMethod;
	/** The bean shell interpreter source method */ private static Method sourceMethod;
	/** The bean shell interpreter set method */    private static Method setMethod;
	/** The bean shell TargetError getTarget method */ private static Method getTargetMethod;
	/** The bean shell interpreter class */         private static Class interpreterClass;
	/** The bean shell TargetError class */         private static Class targetErrorClass;

    /** For replacing @variable */					private static final Pattern atPat = Pattern.compile("@(\\w+)");
    /** For replacing @variable */					private static final Pattern pPat = Pattern.compile("(P|PAR)\\(\"(\\w+)\"\\)");

    /** Context stack for recursive eval calls */   private static Stack contextStack = new Stack();
    /** Info stack for recursive eval calls */      private static Stack infoStack = new Stack();

    /** turn on Bsh verbose debug stmts */          private static boolean debug = false;

    // ------------------------ private and protected methods -------------------

    /** the contructor */
    private EvalJavaBsh()
    {
		initBSH();

		setVariable("evalJavaBsh", this);
		doEval("Object P(String par) { return evalJavaBsh.P(par); }");
		doEval("Object PAR(String par) { return evalJavaBsh.PAR(par); }");
        
        // the following is for running scripts
		doEval("import com.sun.electric.tool.user.MenuCommands;");
		doEval("import com.sun.electric.database.hierarchy.*;");
		doEval("import com.sun.electric.database.prototype.*;");
		doEval("import com.sun.electric.database.topology.*;");
		doEval("import com.sun.electric.database.variable.ElectricObject;");
		doEval("import com.sun.electric.database.variable.FlagSet;");
		doEval("import com.sun.electric.database.variable.TextDescriptor;");
		doEval("import com.sun.electric.database.variable.VarContext;");
		doEval("import com.sun.electric.database.variable.Variable;");

        // do not import variable.EvalJavaBsh, because calling EvalJavaBsh.runScript
        // will spawn jobs in an unexpected order
		doEval("import com.sun.electric.tool.io.*;");
    }

	/** Get the interpreter so other tools may add methods to it. There is only
	 * one interpreter, so be careful that separate tools do not conflict in 
	 * terms of namespace.  I recommend when adding objects or methods to the
	 * Interpreter you prepend the object or method names with the Tool name.
	 */
//	  public static Interpreter getInterpreter() { return env; }

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
		if (context == null) context = VarContext.globalContext;
		contextStack.push(context);             // push context
		infoStack.push(info);                   // push info
		Object ret = doEval(expr);              // ask bsh to eval
		contextStack.pop();                     // pop context
		infoStack.pop();                        // pop info
		//System.out.println("BSH: "+expr.toString()+" --> "+ret);
		if (ret instanceof Number) {
			// get rid of lots of decimal places on floats and doubles
			ret = Variable.format((Number)ret, 3);
		}
		return ret;
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
			doSource(script);
		}
	}

	// ****************************** REFLECTION FOR ACCESSING THE BEAN SHELL ******************************

	private void initBSH()
	{
		if (EXTERNAL_BSH)
		{
			// find the BSH classes
			try
			{
				interpreterClass = Class.forName("bsh.Interpreter");
				targetErrorClass = Class.forName("bsh.TargetError");
			} catch (ClassNotFoundException e)
			{
				System.out.println("Can't find the Bean Shell: " + e.getMessage());
				interpreterClass = null;
				return;
			}

			// find the necessary methods on the BSH class
			try
			{
				evalMethod = interpreterClass.getMethod("eval", new Class[] {String.class});
				sourceMethod = interpreterClass.getMethod("source", new Class[] {String.class});
				setMethod = interpreterClass.getMethod("set", new Class[] {String.class, Object.class});
				getTargetMethod = targetErrorClass.getMethod("getTarget", new Class[] {});
			} catch (NoSuchMethodException e)
			{
				System.out.println("Can't find methods in the Bean Shell: " + e.getMessage());
				interpreterClass = null;
				return;
			}

			// create the BSH object
			try
			{
				envObject = interpreterClass.newInstance();
			} catch (Exception e)
			{
				System.out.println("Can't create an instance of the Bean Shell: " + e.getMessage());
				interpreterClass = null;
				return;
			}
		} else
		{
			// the old way
			env = new Interpreter();
		}
	}

	public static Object doEval(String line)
	{
		Object returnVal = null;
		try
		{
			if (EXTERNAL_BSH)
			{
				if (interpreterClass != null)
				{
					returnVal = evalMethod.invoke(envObject, new Object[] {line});
				}
			} else
			{
				// the old way
				returnVal = env.eval(line);
			}
		} catch (Exception e)
		{
			System.out.println("Bean shell error evaluating " + line + ": " + e.getMessage());
			handleBshError(e);
		}
		return returnVal;
	}

	private static void doSource(String file)
	{
		try
		{
			if (EXTERNAL_BSH)
			{
				if (interpreterClass != null)
				{
					sourceMethod.invoke(envObject, new Object[] {file});
				}
			} else
			{
				// the old way
				env.source(file);
			}
		} catch (FileNotFoundException e) {
			System.out.println("Cannot run Bean Shell script '"+file+"': "+e.getMessage());
		} catch (IOException e) {
			System.out.println("IO Error trying to run Bean Shell script '"+file+"': "+e.getMessage());
		} catch (Exception e)
		{
			System.out.println("Bean shell error sourcing '" + file + "': " + e.getMessage());
			handleBshError(e);
		}
	}

	public static void setVariable(String name, Object value)
	{
		try
		{
			if (EXTERNAL_BSH)
			{
				if (interpreterClass != null)
				{
					setMethod.invoke(envObject, new Object[] {name, value});
				}
			} else
			{
				// the old way
				env.set(name, value);
			}
		} catch (Exception e)
		{
			System.out.println("Bean shell error setting " + name + " on "+ value + ": " + e.getMessage());
		}
	}

	private static Throwable doGetTarget(Object ex)
	{
		Throwable returnVal = null;
		if (interpreterClass != null)
		{
			try
			{
				returnVal = (Throwable)getTargetMethod.invoke(ex, new Object[] {});
			} catch (Exception e)
			{
				System.out.println("Bean shell error getting exception target: " + e.getMessage());
				handleBshError(e);
			}
		}
		return returnVal;
	}

    /** Handle errors.  Sends it to system.out */
    public static void handleBshError(Exception e)
	{
        //System.out.println("Bean Shell eval error: " + e.getMessage());
        if (debug)
		{
			if (EXTERNAL_BSH)
			{
				if (targetErrorClass.isInstance(e))
				{
					Throwable t = doGetTarget(e);
					if (t != null)
						System.out.println("  Exception thrown: "+t+"; "+t.getMessage());
					e.printStackTrace(System.out);
				}
			} else
			{
	            if (e instanceof bsh.TargetError)
				{
	                bsh.TargetError te = (bsh.TargetError)e;
	                Throwable t = te.getTarget();
	                System.out.println("  Exception thrown: "+t+"; "+t.getMessage());
	                e.printStackTrace(System.out);
				}
            }
        }
    }

}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VarContext.java
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

import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.prototype.NodeProto;

import java.lang.Number;
import java.lang.NumberFormatException;
import java.lang.Exception;

/**
 * VarContext represents a hierarchical path of NodeInsts.  Its
 * primary use is to determine the value of variables which contain
 * Java code.  In particular, the syntax @foo expands to P("foo")
 * which looks for the variable called foo in the NodeInst of this
 * VarContext.
 * 
 * <p>A VarContext can also be used to recover the names of instances
 * along a hierarchical path.  LE.getdrive() is an example of a
 * routine that does this.
 *
 * <p> The VarContext object that represents the base top level is
 * VarContext.globalContext.  You can get a new VarContext with an extra
 * NodeInst context attached by calling push(NodeInst) on the parent
 * context.  You can get a VarContext with the most recent NodeInst
 * removed by calling pop().  Note that individual VarContexts are
 * immutable:  you get new ones by calling push and pop;  push and pop
 * do not edit their own VarContexts.
 * 
 * <p>Retrieve a Variable by calling getVar(String name) on any 
 * ElectricObject.
 *
 * <p>If the one knows that the Variable contains an object that
 * does not need to be evaluated, that object can be retrieved using
 * Variable.getObject().
 *
 * <p>On the other hand, if the object may need to be evaluated because
 * it is type Java, TCL, or Lisp, then such evaluation may be hierarchy
 * dependent and one must call context.evalVar(variable).
 * 
 * <p>Extra variables defined in the interpreter:<br>
 * 
 * <p>Extra functions defined in the interpreter:<br>
 *
 * P(name) -- get the value of variable name on the most recent NodeInst.
 * Defaults to Integer(0).<br>
 * PD(name, default) -- get the value of variable name on the most recent
 * NodeInst.  Defaults to default.<br>
 * PAR(name) -- get the value of variable name on any NodeInst, starting
 * with the most recent.  Defaults to Integer(0).<br>
 * PARD(name, default) -- get the value of variable name on any NodeInst,
 * starting with the most recent.  Defaults to default.
 */
public class VarContext
{
	private VarContext prev;
	private NodeInst ni;
    
	/**
	 * The blank VarContext that is the parent of all VarContext chains.
	 */
	public static VarContext globalContext = new VarContext();

	/**
	 * get a new VarContext that consists of the current VarContext with
	 * the given NodeInst pushd onto the stack
	 */
	public VarContext push(NodeInst ni)
	{
		return new VarContext(ni, this);
	}

	private VarContext(NodeInst ni, VarContext prev)
	{
		this.ni = ni;
		this.prev = prev;
	}

	// For the global context.
	private VarContext()
	{
		ni = null;
		prev = this;
	}

	/**
	 * get the VarContext that existed before you called push on it.
	 * may return globalContext if the stack is empty.
	 */
	public VarContext pop()
	{
		return prev;
	}
    
    /**
     * Gets the value of Variable @param var.
     * If variable is Java, uses EvalJavaBsh to evaluate
     * If variable is TCL, uses ... to evaluate
     * If variable is Lisp, uses ... to evaluate
     * otherwise, just returns the Variable's object
     * @return the evlauated Object
     */
    public Object evalVar(Variable var)
    {
        return evalVar(var, null);
    }
    /** Same as evalVar, except an additional object 'info'
     * is passed to the evaluator.  'info' may be or contain 
     * additional information necessary for proper evaluation.
     */
    public Object evalVar(Variable var, Object info)
    {
        if (var == null) return null;
        if (var.isJava()) return(EvalJavaBsh.tool.eval(var.getObject(), this, info));
        // TODO: if(var.isTCL()) { }
        // TODO: if(var.isLisp()) { }
        return var.getObject();
    }
        
    /**
     * Lookup Variable one level up the hierarchy and evaluate. 
     * Looks for the var on the most recent NodeInst on the
     * hierarchy stack.  If not found, look for the default
     * Variable of the same name on the NodeProto.
     * @param name the name of the variable
     * @return an object representing the evaluated variable,
     * or null if no var or default var found.
     */
    public Object lookupVarEval(String name)
    {
        if (ni == null) return null;
        Variable var = ni.getVar(name);
        if (var == null) {
            NodeProto np = ni.getProto();
            var = np.getVar(name);
        }
        if (var == null) return null;
        // evaluate var in it's context
        return this.pop().evalVar(var, ni);
    }
    
    /** 
     * Lookup Variable on all levels up the hierarchy and evaluate.
     * Looks for var on all NodeInsts on the stack, starting
     * with the most recent.  At each NodeInst, if no Variable
     * found, looks for default Variable on NodeProto.
     * @param name the name of the variable
     * @return evaluated Object, or null if not found
     */
    public Object lookupVarFarEval(String name)
    {
		// look up the entire stack, starting with end
		VarContext scan = this;
        
        while (scan != null && ni != null)
		{
            NodeInst ni = scan.getNodeInst();
            if (ni == null) return null;
            
            Variable var = ni.getVar(name);             // look up var
			if (var != null)
				return scan.pop().evalVar(var, scan.getNodeInst());
            NodeProto np = ni.getProto();               // look up default var
            var = np.getVar(name);
            if (var != null)
                return scan.pop().evalVar(var, scan.getNodeInst());
			scan = scan.prev;
		}
		return null;
	}
    
	/**
	 * Return the Node Instance that provides the context for the
	 * variable evaluation for this level.
	 */
	public NodeInst getNodeInst()
	{
		return ni;
	}
	
	/** Return the concatonation of all instances names left to right
	 * from the root to the leaf. Begin with the string with a separator
	 * and place a separator between adjacent instance names.
	 * @param sep the separator string.
	 */
	public String getInstPath(String sep) 
	{
        if (this==globalContext) return "";

        String prefix = pop()==globalContext ? "" : pop().getInstPath(sep);
        NodeInst ni = getNodeInst();
        if (ni==null) {
            System.out.println("VarContext.getInstPath: context with null NodeInst?");
        }
        String me = ni.getName();
        if (me==null) {
            //System.out.println("VarContext.getInstPath: NodeInst in VarContext with no name!!!");
            me = ni.describe();
        }
        if (prefix.equals("")) return me;
        return prefix + sep + me;
    }

    /** Helper method to convert an Object to a float, if possible.
     * if not possible, return @param def.
     */
    public static float objectToFloat(Object obj, float def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number)obj).floatValue();
        try {
            return Float.valueOf(obj.toString()).floatValue();
        } catch (NumberFormatException e) {}
        return def;
    }

    /** Helper method to convert an Object to an integer, if possible.
     * if not possible, return @param def.
     */
    public static int objectToInt(Object obj, int def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number)obj).intValue();
        try {
            return Integer.valueOf(obj.toString()).intValue();
        } catch (NumberFormatException e) {}
        return def;
    }
    
    /** Helper method to convert an Object to a short, if possible.
     * if not possible, return @param def.
     */
    public static short objectToShort(Object obj, short def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number)obj).shortValue();
        try {
            return Short.valueOf(obj.toString()).shortValue();
        } catch (NumberFormatException e) {}
        return def;
    }
    
   /** Helper method to convert an Object to a double, if possible.
     * if not possible, return @param def.
     */
    public static double objectToDouble(Object obj, double def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number)obj).doubleValue();
        try {
            return Double.valueOf(obj.toString()).doubleValue();
        } catch (NumberFormatException e) {}
        return def;
    }
    
}

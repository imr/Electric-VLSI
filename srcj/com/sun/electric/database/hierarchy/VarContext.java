package com.sun.electric.database.hierarchy;

import com.sun.electric.database.topology.NodeInst;

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
 * <p>Use a VarContext by calling getVar(String name, VarContext context)
 * on any Electric object.
 * 
 * <p>Extra variables defined in the interpreter:<br>
 *
 * LE -- instance of com.sun.dbmirror.JoseLogicalEffort.
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
	VarContext prev;
	NodeInst ni;

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
	 * get the value of a variable on the most recent NodeInst on this
	 * stack.  If the variable isn't present, return the default object,
	 * def.
	 * @param name the name of the variable
	 * @param def the default object to return if the variable isn't
	 * present on the most recent NodeInst of this stack.
	 */
	public Object getVal(String name, Object def)
	{
		// look only at stack[stack.size()-1] (end of stack)
		if (ni == null)
			return def;

		Object val = ni.getVar(name, prev);
		return val == null ? def : val;
	}

	/**
	 * get the value of a variable on any NodeInst on this stack, starting
	 * with the most recent.  If the variable isn't present on any
	 * NodeInst, return the default object, def.
	 * @param name the name of the variable
	 * @param def the default object to return if the variable isn't
	 * present on ANY NodeInst of this stack.
	 */
	public Object getArVal(String name, Object def)
	{
		// look up the entire stack, starting with end
		VarContext scan = this;
		while (scan != null && ni != null)
		{
			Object val = ni.getVar(name, scan.prev);
			if (val != null)
				return val;
			scan = scan.prev;
		}
		return def;
	}

	/**
	 * Return the Node Instance that provides the context for the
	 * variable evaluation for this level.
	 */
	public NodeInst getNodeInst()
	{
		return ni;
	}
}

package com.sun.electric.database.variables;

import com.sun.electric.database.hierarchy.VarContext;
import com.sun.electric.user.Electric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * This class represents all Electric structures in C that are
 * reflected in the Java-side database.  At this high level of the
 * hierarchy, the only common information is the address of the
 * corresponding C structure and a hashtable of extra information.
 * You should never need a direct instance of an ElectricObject--
 * everything interesting will be an instance of a subclass.
 */
public class ElectricObject
{
	// ------------------------ private data ------------------------------------
	private HashMap vars; // extra variables (null if no variables yet)

	// ------------------------ private and protected methods -------------------

	/** utility function to remove all Electric elements from an ArrayList */
	static public void removeAll(ArrayList l)
	{
		while (l.size() > 0)
		{
			ElectricObject e = (ElectricObject) l.get(l.size() - 1);
			l.remove(e);
		}
	}

	/** utility function to remove all Electric elements from a HashMap */
	static void removeAll(HashMap hm)
	{
		Object[] keys = hm.keySet().toArray();
		for (int i = 0; i < keys.length; i++)
		{
			if (hm.containsKey(keys[i]))
			{
				ElectricObject e = (ElectricObject) hm.get(keys[i]);
				hm.remove(keys[i]);
			}
		}
	}

	// Create an object that represents a C-side Electric structure
	protected ElectricObject()
	{
	}

	/** put an object value associated with a name */
	protected void putVar(String name, Object value)
	{
		if (vars == null)
		{
			vars = new HashMap();
		}
		vars.put(name, value);
	}

	/** put an object into an array value associated with a name */
	protected void putVar(String name, Object value, int idx)
	{
		Object obj = getVar(name);
		if (obj != null && obj instanceof Object[])
		{
			((Object[]) obj)[idx] = value;
		}
	}

	/** delete a variable from the vars list */
	protected void removeVar(String name)
	{
		if (vars != null)
		{
			vars.remove(name);
		}
	}

	/** Print detailed information about this object. */
	public final void showInfo()
	{
		System.out.println(this);
		getInfo();
	}

	/** Print a full description of this object (in response to an INFO
	 * request). */
	protected void getInfo()
	{
		if (vars != null)
		{
			System.out.println("Variables:");
			Iterator it = new TreeSet(vars.keySet()).iterator();
			while (it.hasNext())
			{
				String key = (String) it.next();
				Object val = vars.get(key);
				if (val instanceof Object[])
				{
					Object[] ary = (Object[]) val;
					System.out.print("   " + key + "= (" + ary.length + ")[");
					for (int i = 0; i < ary.length - 1; i++)
					{
						System.out.print("\"" + ary[i] + "\", ");
						if (i > 2 && ary.length != 4)
						{
							System.out.print("..., ");
							break;
						}
					}
					if (ary.length > 0)
					{
						System.out.print("\"" + ary[ary.length - 1] + "\"");
					}
					System.out.println("]");
				} else if (val instanceof int[])
				{
					int[] ary = (int[]) val;
					System.out.print("   " + key + "= (" + ary.length + ")[");
					for (int i = 0; i < ary.length - 1; i++)
					{
						System.out.print(ary[i] + ", ");
						if (i > 8 && ary.length != 10)
						{
							System.out.print("..., ");
							break;
						}
					}
					if (ary.length > 0)
					{
						System.out.print(ary[ary.length - 1]);
					}
					System.out.println("]");
				} else
				{
					System.out.println("   " + key + "= " + vars.get(key));
				}
			}
		}
	}

	public void setVar(String name, ElectricObject value)
	{
		error(name == null, "ElectricObject.setVar: null name");
		error(value == null, "ElectricObject.setVar: null value");
		//Electric.setVariable(getAddr(), getVType(this), name, value.getAddr(), getVType(value));
	}

	public void setVar(String name, String value)
	{
		error(name == null, "ElectricObject.setVar: null name");
		error(value == null, "ElectricObject.setVar: null value");
		//Electric.setVariableString(getAddr(), getVType(this), name, value);
	}

	public void setVar(String name, int value)
	{
		error(name == null, "ElectricObject.setVar: null name");
		//Electric.setVariable(getAddr(), getVType(this), name, value, 1);
		// VINTEGER
	}

	public void delVar(String name)
	{
		error(name == null, "ElectricObject.delVar: null name");
		//Electric.delVariable(getAddr(), getVType(this), name);
	}

	/** Remove this ElectricObject from Electric's and Jose's data base.
	 */
//	public void delete()
//	{
//		error(this instanceof Tool, "Tools can't be deleted");
//		error(this instanceof PrimitivePort, "PrimitivePorts can't be deleted");
//		error(this instanceof Connection,
//			"Connections can't be deleted, " + "delete entire ArcInst instead");
//		error(this instanceof ArcProto, "ArcProtos can't be deleted");
//
//		boolean err;
//		if (this instanceof Export)
//		{
//			Cell f = (Cell) ((Export) this).getParent();
//			err = Electric.delElectricObject(getAddr(), getVType(this), f.getAddr());
//		} else
//		{
//			err = Electric.delElectricObject(getAddr(), getVType(this), 0);
//		}
//		error(err, "delete failed");
//	}

	/** Retrieve an object from the array variable. <br>
	 * Get the object that is the value of a variable on this Electric
	 * object, with respect to a stack of NodeInsts.  That object must
	 * be an Object[].  Return the specified array element.
	 * @param name the name of the variable
	 * @param idx index into the array that is the value of the variable
	 * @param context the stack of NodeInsts
	 * @return the object at the index
	 * @throws RuntimeException if the variable doesn't exist, or
	 * the value of the variable isn't an object array. */
	public Object getVar(String name, int idx, VarContext context)
	{
		Object v = getVar(name, context);
		if (v == null)
			error("No such variable: " + name + " on: " + this);
		if (!(v instanceof Object[]))
			error("Not an Object[], variable: " + name + " on: " + this);

		return ((Object[]) v)[idx];
	}

	/** Equivalent to getVar(name, idx, VarContext.globalContext). */
	public Object getVar(String name, int idx)
	{
		return getVar(name, idx, VarContext.globalContext);
	}

	/** Retrieve the specified int from the array variable. <br>
	 * Get the object that is the value of a variable on this Electric
	 * object, with respect to a stack of NodeInsts.  That object must
	 * be an int[].  Return the specified array element.
	 * @param name the name of the variable
	 * @param idx index into the array that is the value of the variable
	 * @param context the stack of NodeInsts
	 * @return the int value
	 * @throws RuntimeException if the variable doesn't exist, or
	 * the value of the variable isn't an int array. */
	public int getIntVar(String name, int idx, VarContext context)
	{
		Object v = getVar(name, context);
		if (v == null)
			error("No such variable: " + name + " on: " + this);
		if (!(v instanceof int[]))
			error("Not an int[], variable: " + name + " on: " + this);

		return ((int[]) v)[idx];
	}

	/** Equivalent to getIntVar(name, idx, VarContext.globalContext). */
	public int getIntVar(String name, int idx)
	{
		return getIntVar(name, idx, VarContext.globalContext);
	}

	/** Retrieve the object that is the value of a variable on this
	 * Electric object, with respect to a stack of NodeInsts.  The
	 * context stack is only used for code variables that try to read
	 * other properties from a NodeInst or its parents.
	 * @param name the name of the variable
	 * @param context the stack of NodeInsts
	 * @return the object stored at the variable, or null if there is no
	 * such object. */
	public Object getVar(String name, VarContext context)
	{
//		if (vars == null)
//			return null;
//
//		Object v = vars.get(name);
//		if (!(v instanceof VarCode))
//			return v;
//
//		return ((VarCode) v).eval(context);
		return null;
	}

	/** Equivalent to getVar(name, VarContext.globalContext). */
	public Object getVar(String name)
	{
		return getVar(name, VarContext.globalContext);
	}

	/** Retrieve the int that is the value of a variable on this
	 * Electric object, with respect to a stack of NodeInsts.  The
	 * context stack is only used for code variables that try to read
	 * other properties from a NodeInst or its parents.
	 * @param name the name of the variable
	 * @param context the stack of NodeInsts
	 * @return the integer stored at the variable
	 * @throws RuntimeException if the variable doesn't exist, or
	 * the variable isn't an Integer. */
	public int getIntVar(String name, VarContext context)
	{
		Object v = getVar(name);
		if (v == null)
			error("No such variable: " + name + " on: " + this);
		if (v instanceof Integer)
			return ((Integer) v).intValue();

		error("Not an Integer, variable: " + name + " on: " + this);
		return 0;
	}

	/** Equivalent to getIntVar(name, VarContext.globalContext). */
	public int getIntVar(String name)
	{
		return getIntVar(name, VarContext.globalContext);
	}

	/** Return all the Variable names. */
	public Iterator getVarNames()
	{
		if (vars == null)
			return (new ArrayList()).iterator();
		return vars.keySet().iterator();
	}

	public static void error(boolean pred, String msg)
	{
		Electric.error(pred, msg);
	}

	public static void error(String msg)
	{
		Electric.error(msg);
	}

	/** This object as a string.  Will always contain the type of the
	 * object and the c-side address as a Hexidecimal number.  May also
	 * include more interesting information.  For objects with names,
	 * use their getName method if you want their name instead. */
	public String toString()
	{
		return getClass().getName();
	}

//	/**  Print out all the Electric variables on this object.
//	 *
//	 * <p>
//	 * @param context A design hierarchy path that identifies a
//	 * specific instance.  This is needed, for example, to get Logical
//	 * Effort device sizes.  If context is null then printVariables()
//	 * uses VarContext.globalContext. */
//	public void printVariables(VarContext context)
//	{
//		if (context == null)
//			context = VarContext.globalContext;
//		System.out.println("Printing all variables:");
//		Iterator varNms = getVarNames();
//		while (varNms.hasNext())
//		{
//			String varNm = (String) varNms.next();
//			error(varNm == null, "variable with no name?");
//			System.out.print("       " + varNm + ": ");
//			Object val = getVar(varNm, context);
//			printVal(val);
//		}
//		System.out.println("Done Printing all variables:");
//	}

//	private void printVal(Object val)
//	{
//		if (val == null)
//		{
//			System.out.print("null");
//		} else if (val instanceof Double)
//		{
//			System.out.print("Double: ");
//		} else if (val instanceof Float)
//		{
//			System.out.print("Float: ");
//		} else if (val instanceof Integer)
//		{
//			System.out.print("Integer: ");
//		} else if (val instanceof String)
//		{
//			System.out.print("String: ");
//		} else if (val instanceof Double[])
//		{
//			System.out.print("Double[]: ");
//		} else if (val instanceof Float[])
//		{
//			System.out.print("Float[]: ");
//		} else if (val instanceof Integer[])
//		{
//			System.out.print("Integer[]: ");
//		} else if (val instanceof String[])
//		{
//			System.out.print("String[]: ");
//		} else
//		{
//			System.out.print("Unknown type: ");
//		}
//		System.out.println(val);
//	}

}

package com.sun.electric.database.variable;

import com.sun.electric.database.hierarchy.VarContext;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.tool.user.Electric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This class is the superclass of all Electric objects that can be extended with "Variables".
 */
public class ElectricObject
{
	// ------------------------ private data ------------------------------------

	/** extra variables (null if no variables yet) */		private HashMap vars;

	// ------------------------ private and protected methods -------------------

	/** The constructor is not used */
	protected ElectricObject()
	{
	}

	// ------------------------ public methods -------------------

	/** Retrieve the object that is the value of a variable on this Electric object.
	 * @param name the name of the variable
	 * @return the object stored at the variable, or null if there is no such object.
	 */
	public Variable getVal(String name)
	{
		if (vars == null) return null;
		Variable val = (Variable)vars.get(name);
		return val;
	}

	/** Retrieve the object that is the value of a variable on this Electric object.
	 * @param name the name of the variable
	 * @return the object stored at the variable, or null if there is no such object.
	 */
	public Variable getVal(String name, Class type)
	{
		Variable val = getVal(name);
		if (val == null) return null;
		if (!type.isInstance(val.getObject())) return null;
		return val;
	}

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
	public Object getVal(String name, int index)
	{
		Variable v = getVal(name);
		if (v == null) return null;
		Object addr = v.getObject();
		if (!(addr instanceof Object[])) return null;
		return ((Object[]) addr)[index];
	}

	/**
	 * Routine to return the number of displayable variables on this Electric object.
	 */
	public int numDisplayableVariables()
	{
		if (vars == null) return 0;

		int numVars = 0;
		for(Iterator it = vars.values().iterator(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isDisplay()) numVars++;
		}
		return numVars;
	}
	
	/**
	 * Routine to add displayable variables on this Electric object to the polygon list in "polys",
	 * starting at entry "start".
	 */
	public void addDisplayableVariables(Rectangle2D rect, Poly [] polys, int start)
	{
		if (vars == null) return;
		
		double cX = rect.getCenterX();
		double cY = rect.getCenterY();
		for(Iterator it = vars.values().iterator(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDisplay()) continue;
			TextDescriptor td = var.getTextDescriptor();
			double offX = (double)td.TDGetXOff() / 4;
			double offY = (double)td.TDGetYOff() / 4;
			Point2D.Double [] pointList = new Point2D.Double[1];
			pointList[0] = new Point2D.Double(cX+offX, cY+offY);
			polys[start] = new Poly(pointList);
			polys[start].setStyle(Poly.Type.TEXTCENT);
			polys[start].setString(var.describe());
			polys[start].setLayer(null);
			start++;
		}
	}

	/** put an object value associated with a name */
	public Variable setVal(String name, Object value)
	{
		if (vars == null)
		{
			vars = new HashMap();
		}
		Variable v = new Variable(value, new TextDescriptor());
		vars.put(name, v);
		return v;
	}

	/** put an object into an array value associated with a name */
	public void setVal(String name, Object value, int index)
	{
		Variable v = getVal(name);
		if (v == null) return;
		Object addr = v.getObject();
		if (addr instanceof Object[])
		{
			((Object[]) addr)[index] = value;
		}
	}

	/** delete a variable from the vars list */
	public void delVal(String name)
	{
		if (vars == null) return;
		vars.remove(name);
	}

	public boolean isdeprecatedvariable(String name)
	{
		return false;
	}

	/** Return all the Variable names. */
	public Iterator getVariables()
	{
		if (vars == null)
			return (new ArrayList()).iterator();
		return vars.keySet().iterator();
	}

	/** Print a full description of this object (in response to an INFO request). */
	protected void getInfo()
	{
		if (vars == null) return;
		System.out.println("Variables:");
		for(Iterator it = vars.keySet().iterator(); it.hasNext() ;)
		{
			String key = (String) it.next();
			Variable val = (Variable)vars.get(key);
			if (val == null) continue;
			Object addr = val.getObject();
			if (addr instanceof Object[])
			{
				Object[] ary = (Object[]) addr;
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
			} else
			{
				System.out.println("   " + key + "= " + addr);
			}
		}
	}

	/** This object as a string.  Will always contain the type of the
	 * object and the c-side address as a Hexidecimal number.  May also
	 * include more interesting information.  For objects with names,
	 * use their getName method if you want their name instead. */
	public String toString()
	{
		return getClass().getName();
	}

}

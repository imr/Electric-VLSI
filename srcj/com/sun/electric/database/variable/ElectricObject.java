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
package com.sun.electric.database.variable;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This class is the base class of all Electric objects that can be extended with "Variables".
 */
public class ElectricObject
{
	// ------------------------ private data ------------------------------------

	/** a list of all variable names */						private static HashMap varNames = new HashMap();

	/** extra variables (null if no variables yet) */		private HashMap vars;

	// ------------------------ private and protected methods -------------------

	/**
	 * The constructor is not used.
	 */
	protected ElectricObject()
	{
	}

	// ------------------------ public methods -------------------

	/**
	 * Routine to return the Variable on this ElectricObject with a given name.
	 * @param name the name of the Variable.
	 * @return the Variable with that name, or null if there is no such Variable.
	 */
	public Variable getVar(String name)
	{
		if (vars == null) return null;
		Variable var = (Variable)vars.get(name);
		return var;
	}

	/**
	 * Routine to return the Variable on this ElectricObject with a given name and type.
	 * @param name the name of the Variable.
	 * @param type the required type of the Variable.
	 * @return the Variable with that name and type, or null if there is no such Variable.
	 */
	public Variable getVar(String name, Class type)
	{
		Variable var = getVar(name);
		if (var == null) return null;
		if (!type.isInstance(var.getObject())) return null;
		return var;
	}

	/**
	 * Routine to return an entry in an arrayed Variable on this ElectricObject.
	 * @param name the name of the Variable.
	 * @param index the required entry in the Variable array.
	 * @return the Object in that entry of the Variable, or null if there is no such Variable.
	 */
/*
    public Object getVal(String name, int index)
	{
		Variable v = getVar(name);
		if (v == null) return null;
		Object addr = v.getObject();
		if (!(addr instanceof Object[])) return null;
		return ((Object[]) addr)[index];
	}
*/
	/**
	 * Routine to return the number of displayable Variables on this ElectricObject.
	 * A displayable Variable is one that will be shown with its object.
	 * Displayable Variables can only sensibly exist on NodeInst and ArcInst objects.
	 * @return the number of displayable Variables on this ElectricObject.
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
	 * Routine to add all displayable Variables on this Electric object to an array of Poly objects.
	 * @param rect a rectangle describing the bounds of the object on which the Variables will be displayed.
	 * @param polys an array of Poly objects that will be filled with the displayable Variables.
	 * @param start the starting index in the array of Poly objects to fill with displayable Variables.
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
			double offX = (double)td.getXOff() / 4;
			double offY = (double)td.getYOff() / 4;
			Point2D.Double [] pointList = new Point2D.Double[1];
			pointList[0] = new Point2D.Double(cX+offX, cY+offY);
			polys[start] = new Poly(pointList);
			polys[start].setStyle(Poly.Type.TEXTCENT);
			polys[start].setString(var.describe());
			polys[start].setTextDescriptor(td);
			polys[start].setLayer(null);
			start++;
		}
	}

	/**
	 * Routine to create a Variable on this ElectricObject with the specified values.
	 * @param name the name of the Variable.
	 * @param value the object to store in the Variable.
	 * @return the Variable that has been created.
	 */
	public Variable setVal(String name, Object value)
	{
		Variable.Name vn = findName(name);
		if (vn == null)
		{
			vn = new Variable.Name(name);
			varNames.put(name, vn);
		}

		if (vars == null)
		{
			vars = new HashMap();
		}
		Variable v = new Variable(value, new TextDescriptor(), vn);
		vars.put(name, v);
		return v;
	}

	/**
	 * Routine to put an Object into an entry in an arrayed Variable on this ElectricObject.
	 * @param name the name of the arrayed Variable.
	 * @param value the object to store in an entry of the arrayed Variable.
	 * @param index the location in the arrayed Variable to store the value.
	 */
	public void setVal(String name, Object value, int index)
	{
		Variable v = getVar(name);
		if (v == null) return;
		Object addr = v.getObject();
		if (addr instanceof Object[])
		{
			((Object[]) addr)[index] = value;
		}
	}

	/**
	 * Routine to delete a Variable from this ElectricObject.
	 * @param name the name of the Variable to delete.
	 */
	public void delVal(String name)
	{
		if (vars == null) return;
		vars.remove(name);
	}

	/**
	 * Routine to determine whether a Variable name on this object is deprecated.
	 * Deprecated Variable names are those that were used in old versions of Electric,
	 * but are no longer valid.
	 * @param name the name of the Variable.
	 * @return true if the Variable name is deprecated.
	 */
	public boolean isDeprecatedVariable(String name)
	{
		return false;
	}

	/**
	 * Routine to return an Iterator over all Variables on this ElectricObject.
	 * @return an Iterator over all Variables on this ElectricObject.
	 */
	public Iterator getVariables()
	{
		if (vars == null)
			return (new ArrayList()).iterator();
		return vars.values().iterator();
	}

	/**
	 * Routine to return the number of Variables on this ElectricObject.
	 * @return the number of Variables on this ElectricObject.
	 */
	public int getNumVariables()
	{
		if (vars == null) return 0;
		return vars.entrySet().size();
	}

	/**
	 * Routine to return an Iterator over all Variable names on this ElectricObject.
	 * @return an Iterator over all Variable names on this ElectricObject.
	 */
	public static Iterator getVariableNames()
	{
		return varNames.keySet().iterator();
	}

	/**
	 * Routine to return the total number of different Variable names on all ElectricObjects.
	 * @return the total number of different Variable names on all ElectricObjects.
	 */
	public static int getNumVariableNames()
	{
		return varNames.keySet().size();
	}

	/**
	 * Routine to indicate that changes are starting on this ElectricObjects.
	 */
	public void startChange()
	{
		// handle change control, constraint, and broadcast
		if (Undo.recordChange())
		{
			// tell all tools about this change
			Undo.Change ch = Undo.newChange(this, Undo.Type.OBJECTSTART, 0, 0, 0, 0, 0, 0);
		}
		Undo.clearNextChangeQuiet();
	}

	/**
	 * Routine to indicate that changes are ending on this ElectricObjects.
	 */
	public void endChange()
	{
		// handle change control, constraint, and broadcast
		if (Undo.recordChange())
		{
			// tell all tools about this change
			Undo.Change ch = Undo.newChange(this, Undo.Type.OBJECTEND, 0, 0, 0, 0, 0, 0);
		}
		Undo.clearNextChangeQuiet();
	}

	/**
	 * Routine to return the Name object for a given Variable name.
	 * Variable Name objects are caches of the actual string name of the Variable.
	 * @return the Name object for a given Variable name.
	 */
	public static Variable.Name findName(String name)
	{
		Variable.Name vn = (Variable.Name)varNames.get(name);
		return vn;
	}

	/*
	 * Routine to determine the appropriate Cell associated with this ElectricObject.
	 * @return the appropriate Cell associated with this ElectricObject.
	 * Returns null if no Cell can be found.
	 */
	public Cell whichCell()
	{
		if (this instanceof NodeInst) return ((NodeInst)this).getParent();
		if (this instanceof ArcInst) return ((ArcInst)this).getParent();
		if (this instanceof Cell) return (Cell)this;
		if (this instanceof Export) return (Cell)((Export)this).getParent();
		return null;
	}

	/*
	 * Routine to write a description of this ElectricObject (lists all Variables).
	 * Displays the description in the Messages Window.
	 */
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
				System.out.print("   " + key + "(" + ary.length + ") = [");
				for (int i = 0; i < ary.length; i++)
				{
					if (i > 4)
					{
						System.out.print("...");
						break;
					}
					if (ary[i] instanceof String) System.out.print("\"");
					System.out.print(ary[i]);
					if (ary[i] instanceof String) System.out.print("\"");
					if (i < ary.length-1) System.out.print(", ");
				}
				System.out.println("]");
			} else
			{
				System.out.println("   " + key + "= " + addr);
			}
		}
	}

	/**
	 * Returns a printable version of this ElectricObject.
	 * @return a printable version of this ElectricObject.
	 */
	public String toString()
	{
		return getClass().getName();
	}

}

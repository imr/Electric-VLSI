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
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.EditWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.font.GlyphVector;

/**
 * This class is the base class of all Electric objects that can be extended with "Variables".
 */
public class ElectricObject
{
	// ------------------------ private data ------------------------------------

	/** extra variables (null if no variables yet) */		private HashMap vars;

	/** a list of all variable keys */						private static HashMap varKeys = new HashMap();
	/** key for NODE_NAME */								public static final Variable.Key nodeNameKey = new Variable.Key(NodeInst.NODE_NAME);
	/** key for VAR_ARC_NAME */								public static final Variable.Key arcNameKey = new Variable.Key(ArcInst.VAR_ARC_NAME);
	{
		varKeys.put(nodeNameKey.getName(), nodeNameKey);
		varKeys.put(arcNameKey.getName(), arcNameKey);
	}

	// ------------------------ private and protected methods -------------------

	/**
	 * The constructor is not used.
	 */
	protected ElectricObject() {}

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

//	/**
//	 * Routine to return an entry in an arrayed Variable on this ElectricObject.
//	 * @param name the name of the Variable.
//	 * @param index the required entry in the Variable array.
//	 * @return the Object in that entry of the Variable, or null if there is no such Variable.
//	 */
//    public Object getVal(String name, int index)
//	{
//		Variable v = getVar(name);
//		if (v == null) return null;
//		Object addr = v.getObject();
//		if (!(addr instanceof Object[])) return null;
//		return ((Object[]) addr)[index];
//	}

	/**
	 * Routine to return the number of displayable Variables on this ElectricObject.
	 * A displayable Variable is one that will be shown with its object.
	 * Displayable Variables can only sensibly exist on NodeInst and ArcInst objects.
	 * @return the number of displayable Variables on this ElectricObject.
	 */
	public int numDisplayableVariables(boolean multipleStrings)
	{
		if (vars == null) return 0;

		int numVars = 0;
		for(Iterator it = vars.values().iterator(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isDisplay())
			{
				int len = var.getLength();
				if (!multipleStrings) len = 1;
				numVars += len;
			}
		}
		return numVars;
	}

	/**
	 * Routine to add all displayable Variables on this Electric object to an array of Poly objects.
	 * @param rect a rectangle describing the bounds of the object on which the Variables will be displayed.
	 * @param polys an array of Poly objects that will be filled with the displayable Variables.
	 * @param start the starting index in the array of Poly objects to fill with displayable Variables.
	 * @return the number of Variables that were added.
	 */
	public int addDisplayableVariables(Rectangle2D rect, Poly [] polys, int start, EditWindow wnd, boolean multipleStrings)
	{
		int numAddedVariables = 0;
		if (vars == null) return numAddedVariables;

		double cX = rect.getCenterX();
		double cY = rect.getCenterY();
		for(Iterator it = vars.values().iterator(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDisplay()) continue;
			TextDescriptor td = var.getTextDescriptor();
			double offX = (double)td.getXOff() / 4;
			double offY = (double)td.getYOff() / 4;
			int varLength = var.getLength();
			double height = 0;
			TextDescriptor.Position pos = td.getPos();
			Poly.Type style = pos.getPolyType();
			if (varLength > 1)
			{
				// compute text height
				GlyphVector gv = wnd.getGlyphs(var.describe(0, -1, wnd.getVarContext(), this), td);
				Rectangle2D glyphBounds = gv.getVisualBounds();
				height = glyphBounds.getHeight() / wnd.getScale();
				if (multipleStrings)
				{
					if (style == Poly.Type.TEXTCENT || style == Poly.Type.TEXTLEFT || style == Poly.Type.TEXTRIGHT)
						cY += height * (varLength-1) / 2;
					if (style == Poly.Type.TEXTBOT || style == Poly.Type.TEXTBOTLEFT || style == Poly.Type.TEXTBOTRIGHT)
						cY += height * (varLength-1);
				} else
				{
					if (style == Poly.Type.TEXTCENT || style == Poly.Type.TEXTLEFT || style == Poly.Type.TEXTRIGHT)
						cY -= height * (varLength-1) / 2;
					if (style == Poly.Type.TEXTTOP || style == Poly.Type.TEXTTOPLEFT || style == Poly.Type.TEXTTOPRIGHT)
						cY -= height * (varLength-1);
					varLength = 1;
				}
			}
			for(int i=0; i<varLength; i++)
			{
				int index = start + numAddedVariables;
				numAddedVariables++;
				Point2D [] pointList = new Point2D.Double[1];
				pointList[0] = new Point2D.Double(cX+offX, cY+offY);
				polys[index] = new Poly(pointList);
				polys[index].setStyle(style);
				polys[index].setString(var.describe(i, -1, wnd.getVarContext(), this));
				polys[index].setTextDescriptor(td);
				polys[index].setLayer(null);
				polys[index].setVariable(var);
				cY -= height;
			}
		}
		return numAddedVariables;
	}

	/**
	 * Routine to create a Variable on this ElectricObject with the specified values.
	 * @param name the name of the Variable.
	 * @param value the object to store in the Variable.
	 * @return the Variable that has been created.
	 */
	public Variable setVar(String name, Object value)
	{
		checkChanging();
		Variable.Key key = findKey(name);
		if (key == null)
		{
			key = new Variable.Key(name);
			varKeys.put(name, key);
		}

		if (vars == null)
		{
			vars = new HashMap();
		}
		Variable v = (Variable) vars.get(name);
		if (v == null)
		{
			v = new Variable(this, value, TextDescriptor.newNodeArcDescriptor(), key);
			vars.put(name, v);
		} else
		{
			v.setObject(value);
		}
		return v;
	}

	/**
	 * Routine to put an Object into an entry in an arrayed Variable on this ElectricObject.
	 * @param name the name of the arrayed Variable.
	 * @param value the object to store in an entry of the arrayed Variable.
	 * @param index the location in the arrayed Variable to store the value.
	 */
	public void setVar(String name, Object value, int index)
	{
		checkChanging();
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
	public void delVar(String name)
	{
		checkChanging();
		if (vars == null) return;
		Variable v = getVar(name);
		if (v != null)
		{
			v.kill();
			vars.remove(name);
		}
	}

	/**
	 * Routine to copy all variables from another ElectricObject to this ElectricObject.
	 * @param other the other ElectricObject from which to copy Variables.
	 */
	public void copyVars(ElectricObject other)
	{
		checkChanging();
		for(Iterator it = other.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			Variable.Key key = var.getKey();
			Object obj = var.getObject();
			int flags = var.lowLevelGetFlags();
			TextDescriptor td = var.getTextDescriptor();
			Variable newVar = this.setVar(key.getName(), obj);
			if (newVar != null)
			{
				newVar.lowLevelSetFlags(flags);
				newVar.setDescriptor(td);
			}
		}

		// variables may affect geometry size
//		if (this instanceof NodeInst || this instanceof ArcInst)
//		{
//			if (this instanceof NodeInst)
//			{
//				NodeInst ni = (NodeInst)this;
//				geom = ni->geom;
//				Cell np = ni.getParent();
//			} else
//			{
//				ArcInst ai = (ArcInst)this;
//				geom = ai->geom;
//				Cell np = ai.getParent();
//			}
//			boundobj(geom, &lx, &hx, &ly, &hy);
//			if (lx != geom->lowx || hx != geom->highx ||
//				ly != geom->lowy || hy != geom->highy)
//					updategeom(geom, np);
//		}
	}

	/**
	 * routine to return a unique object name in cell "cell" starting with the
	 * name "name".
	 */
	public static String uniqueObjectName(String name, Cell cell, Class cls, ElectricObject exclude)
	{
		// first see if the name is unique
		if (cell.isUniqueName(name, cls, exclude)) return name;

		char separatechar = '_';

		// now see if the name ends in "]"
		int possibleend = 0;
		int nameLen = name.length();
		if (name.endsWith("]"))
		{
			// see if the array contents can be incremented
			int possiblestart = -1;
			int endpos = nameLen-1;
			for(;;)
			{
				// find the range of characters in square brackets
				int startpos = name.lastIndexOf('[', endpos);
				if (startpos < 0) break;

				// see if there is a comma in the bracketed expression
				int i = name.indexOf(',', startpos);
				if (i >= 0 && i < endpos)
				{
					// this bracketed expression cannot be incremented: move on
					if (startpos > 0 && name.charAt(startpos-1) == ']')
					{
						endpos = startpos-1;
						continue;
					}
					break;
				}

				// see if there is a colon in the bracketed expression
				i = name.indexOf(':', startpos);
				if (i >= 0 && i < endpos)
				{
					// colon: make sure there are two numbers
					String firstIndex = name.substring(startpos+1, i);
					String secondIndex = name.substring(i+1, endpos);
					if (EMath.isANumber(firstIndex) && EMath.isANumber(secondIndex))
					{
						int startIndex = EMath.atoi(firstIndex);
						int endindex = EMath.atoi(secondIndex);
						int spacing = Math.abs(endindex - startIndex) + 1;
						for(int nextindex = 1; ; nextindex++)
						{
							String newname = name.substring(0, startpos) + "[" + (startIndex+spacing*nextindex) +
								":" + (endindex+spacing*nextindex) + name.substring(endpos);
							if (cell.isUniqueName(newname, cls, null)) return newname;
						}
					}

					// this bracketed expression cannot be incremented: move on
					if (startpos > 0 && name.charAt(startpos-1) == ']')
					{
						endpos = startpos-1;
						continue;
					}
					break;
				}

				// see if this bracketed expression is a pure number
				String bracketedExpression = name.substring(startpos+1, endpos);
				if (EMath.isANumber(bracketedExpression))
				{
					int nextindex = EMath.atoi(bracketedExpression) + 1;
					for(; ; nextindex++)
					{
						String newname = name.substring(0, startpos) + "[" + nextindex + name.substring(endpos);
						if (cell.isUniqueName(newname, cls, null)) return newname;
					}
				}

				// remember the first index that could be incremented in a pinch
				if (possiblestart < 0)
				{
					possiblestart = startpos;
					possibleend = endpos;
				}

				// this bracketed expression cannot be incremented: move on
				if (startpos > 0 && name.charAt(startpos-1) == ']')
				{
					endpos = startpos-1;
					continue;
				}
				break;
			}

			// if there was a possible place to increment, do it
			if (possiblestart >= 0)
			{
				// nothing simple, but this one can be incremented
				int i;
				for(i=possibleend-1; i>possiblestart; i--)
					if (!Character.isDigit(name.charAt(i))) break;
				int nextindex = EMath.atoi(name.substring(i+1)) + 1;
				int startpos = i+1;
				if (name.charAt(startpos-1) == separatechar) startpos--;
				for(; ; nextindex++)
				{
					String newname = name.substring(0, startpos) + separatechar + nextindex + name.substring(possibleend);
					if (cell.isUniqueName(newname, cls, null)) return newname;
				}
			}
		}

		// array contents cannot be incremented: increment base name
		int startpos = 0;
		for( ; startpos < name.length(); startpos++)
			if (name.charAt(startpos) == '[') break;
		int endpos = startpos;

		// if there is a numeric part at the end, increment that
		char [] oneChar = new char[1];
		oneChar[0] = separatechar;
		String localSepString = new String(oneChar);
		while (startpos > 0 && Character.isDigit(name.charAt(startpos-1))) startpos--;
		int nextindex = 1;
		if (startpos >= endpos)
		{
			if (startpos > 0 && name.charAt(startpos-1) == separatechar) startpos--;
		} else
		{
			nextindex = EMath.atoi(name.substring(startpos)) + 1;
			localSepString = "";
		}

		for(; ; nextindex++)
		{
			String newname = name.substring(0,startpos) + localSepString + nextindex + name.substring(endpos);
			if (cell.isUniqueName(newname, cls, null)) return newname;
		}
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
	public static Iterator getVariableKeys()
	{
		return varKeys.keySet().iterator();
	}

	/**
	 * Routine to return the total number of different Variable names on all ElectricObjects.
	 * @return the total number of different Variable names on all ElectricObjects.
	 */
	public static int getNumVariableKeys()
	{
		return varKeys.keySet().size();
	}

//	/**
//	 * Routine to indicate that changes are starting on this ElectricObjects.
//	 */
//	public void startChange()
//	{
//		checkChanging();
//		// handle change control, constraint, and broadcast
//		if (Undo.recordChange())
//		{
//			// tell all tools about this change
//			Undo.Change ch = Undo.newChange(this, Undo.Type.OBJECTSTART);
//		}
//	}

//	/**
//	 * Routine to indicate that changes are ending on this ElectricObjects.
//	 */
//	public void endChange()
//	{
//		checkChanging();
//		// handle change control, constraint, and broadcast
//		if (Undo.recordChange())
//		{
//			// tell all tools about this change
//			Undo.Change ch = Undo.newChange(this, Undo.Type.OBJECTEND);
//		}
//	}

	/**
	 * Routing to check whether changing of this cell allowed or not.
	 * By default checks whole database change. Overriden in subclasses.
	 */
	public void checkChanging()
	{
		Cell cell = whichCell();
		if (cell != null)
			cell.checkChanging();
		else
			Job.checkChanging();
	}


	/**
	 * Routine to return the Key object for a given Variable name.
	 * Variable Key objects are caches of the actual string name of the Variable.
	 * @return the Key object for a given Variable name.
	 */
	public static Variable.Key findKey(String name)
	{
		Variable.Key key = (Variable.Key)varKeys.get(name);
		return key;
	}

	/*
	 * Routine to determine the appropriate Cell associated with this ElectricObject.
	 * @return the appropriate Cell associated with this ElectricObject.
	 * Returns null if no Cell can be found.
	 */
	public Cell whichCell() { return null; }

	/*
	 * Routine to write a description of this ElectricObject (lists all Variables).
	 * Displays the description in the Messages Window.
	 */
	public void getInfo()
	{
		if (vars == null) return;
		boolean firstvar = true;
		for(Iterator it = vars.keySet().iterator(); it.hasNext() ;)
		{
			String key = (String) it.next();
			Variable val = (Variable)vars.get(key);
			if (val == null) continue;
			if (firstvar) System.out.println("Variables:");   firstvar = false;
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

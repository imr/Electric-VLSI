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
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.User;

import java.awt.Font;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This class is the base class of all Electric objects that can be extended with "Variables".
 * <P>
 * This class should be thread-safe.
 */
public abstract class ElectricObject
{
	// ------------------------ private data ------------------------------------

	/** extra variables (null if no variables yet) */		private TreeMap vars;
	/** if object is linked into database */                private boolean linked;

	/** a list of all variable keys */						private static final HashMap varKeys = new HashMap();
	/** all variable keys addressed by lower case name */	private static final HashMap varLowCaseKeys = new HashMap();

	// ------------------------ private and protected methods -------------------

	/**
	 * The constructor is not used.
	 */
	protected ElectricObject() { linked = false; }

	// ------------------------ public methods -------------------

    /**
     * Returns true if object is linked into database
     */
    public boolean isLinked() { return linked; }

    /**
     * Sets the flag that says the object is linked into the database.
     * This should only be called by extending objects in their
     * lowLevelLink and lowLevelUnlink methods.
     * @param linked true if object is now linked, false if not.
     */
    protected void setLinked(boolean linked) { this.linked = linked; }
	
    /**
     * Returns true if this ElectricObject is completely linked into database.
	 * This means there is path to this ElectricObjects through a chain of lists.
	 * Below is a chain of lists for every subclass of ElectricObjects:
	 *
	 * Library&#46;libraries-> Library
	 * Library&#46;libraries->Library&#46;cells-> Cell
	 * Library&#46;libraries->Library&#46;cells->Cell&#46;ports-> Export
	 * Library&#46;libraries->Library&#46;cells->Cell&#46;nodes-> NodeInst
	 * Library&#46;libraries->Library&#46;cells->Cell&#46;nodes->NodeInst&#46;portInsts-> PortInst
	 * Library&#46;libraries->Library&#46;cells->Cell&#46;arcs-> ArcInst
     */
	public abstract boolean isActuallyLinked();
	
	/**
	 * Method to return the Variable on this ElectricObject with a given name.
	 * @param name the name of the Variable.
	 * @return the Variable with that name, or null if there is no such Variable.
	 */
	public Variable getVar(String name)
    {
        Variable.Key key = findKey(name);
        return getVar(key, null);
    }

	/**
	 * Method to return the Variable on this ElectricObject with a given key.
	 * @param key the key of the Variable.
	 * @return the Variable with that key, or null if there is no such Variable.
	 */
	public Variable getVar(Variable.Key key)
	{
		return getVar(key, null);
	}

	/**
	 * Method to return the Variable on this ElectricObject with a given name and type.
	 * @param name the name of the Variable.
	 * @param type the required type of the Variable.
	 * @return the Variable with that name and type, or null if there is no such Variable.
	 */
	public Variable getVar(String name, Class type)
    {
        Variable.Key key = findKey(name);
        return getVar(key, type);
    }

	/**
	 * Method to return the Variable on this ElectricObject with a given key and type.
	 * @param key the key of the Variable. Returns null if key is null.
	 * @param type the required type of the Variable. Ignored if null.
	 * @return the Variable with that key and type, or null if there is no such Variable
     * or default Variable value.
	 */
	public Variable getVar(Variable.Key key, Class type)
	{
        checkExamine();
        if (key == null) return null;
        Variable var;
        synchronized(this) {
            if (vars == null) return null;
            var = (Variable)vars.get(key);
        }
		if (var != null) {
            if (type == null) return var;                   // null type means any type
            if (type.isInstance(var.getObject())) return var;
        }
        return null;
    }

	/**
	 * Returns the TextDescriptor on this ElectricObject selected by name.
	 * This name may be a name of variable on this ElectricObject or one of the
	 * special names:
	 * <code>NodeInst.NODE_NAME_TD</code>
	 * <code>NodeInst.NODE_PROTO_TD</code>
	 * <code>ArcInst.ARC_NAME_TD</code>
	 * <code>Export.EXPORT_NAME_TD</code>
	 * Other strings are not considered special, even they are equal to one of the
	 * special name. In other words, special names are compared by "==" other than
	 * by "equals".
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param varName name of variable or special name.
	 * @return the TextDescriptor on this ElectricObject.
	 */
	public TextDescriptor getTextDescriptor(String varName)
	{
		Variable var = getVar(varName);
		if (var == null) return null;
		return var.getTextDescriptor();
	}

	/**
	 * Returns the TextDescriptor on this ElectricObject selected by name.
	 * This name may be a name of variable on this ElectricObject or one of the
	 * special names:
	 * <code>NodeInst.NODE_NAME_TD</code>
	 * <code>NodeInst.NODE_PROTO_TD</code>
	 * <code>ArcInst.ARC_NAME_TD</code>
	 * <code>Export.EXPORT_NAME_TD</code>
	 * Other strings are not considered special, even they are equal to one of the
	 * special name. In other words, special names are compared by "==" other than
	 * by "equals".
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param varName name of variable or special name.
	 * @return the TextDescriptor on this ElectricObject.
	 */
	public MutableTextDescriptor getMutableTextDescriptor(String varName)
	{
		TextDescriptor td = getTextDescriptor(varName);
		if (td == null) return null;
		return new MutableTextDescriptor(td);
	}

    private static int debugGetParameterRecurse = 0;
    /**
     * Method to return the Variable on this ElectricObject with the given key
     * that is a parameter.  If the variable is not found on this object, it
     * is also searched for on the default var owner.
     * @param name the name of the Variable
     * @return the Variable with that key, that may exist either on this object
     * or the default owner.  Returns null if none found.
     */
    public Variable getParameter(String name)
    {
        if (debugGetParameterRecurse > 3)
            ActivityLogger.logException(new Exception("GetParameter recurse error: "+debugGetParameterRecurse));
        debugGetParameterRecurse++;

        Variable.Key key = findKey(name);
        if (key == null) { debugGetParameterRecurse--; return null; }
        Variable var = getVar(key, null);
        if (var != null)
            if (var.getTextDescriptor().isParam()) {
                debugGetParameterRecurse--;
                return var;
            }
        // look on default var owner
        ElectricObject defOwner = getVarDefaultOwner();
        if (defOwner == null) { debugGetParameterRecurse--; return null; }
        if (defOwner == this) { debugGetParameterRecurse--; return null; }

        Variable var2 = defOwner.getParameter(name);
        debugGetParameterRecurse--;
        return var2;
    }

    private static int debugGetParametersRecurse = 0;
    /**
     * Method to return an Iterator over all Variables marked as parameters on this ElectricObject.
     * This may also include any parameters on the defaultVarOwner object that are not on this object.
     * @return an Iterator over all Variables on this ElectricObject.
     */
    public Iterator getParameters() {
        if (debugGetParametersRecurse > 3)
            ActivityLogger.logException(new Exception("GetParameters recurse error: "+debugGetParametersRecurse));
        debugGetParametersRecurse++;

        TreeMap keysToVars = new TreeMap();
        // get all parameters on this object
        for (Iterator it = getVariables(); it.hasNext(); ) {
            Variable v = (Variable)it.next();
            if (!v.getTextDescriptor().isParam()) continue;
            keysToVars.put(v.getKey(), v);
        }
        // look on default var owner
        ElectricObject defOwner = getVarDefaultOwner();
        if (defOwner == null) { debugGetParametersRecurse--; return keysToVars.values().iterator(); }
        if (defOwner == this) { debugGetParametersRecurse--; return keysToVars.values().iterator(); }
        for (Iterator it = defOwner.getParameters(); it.hasNext(); ) {
            Variable v = (Variable)it.next();
            if (keysToVars.get(v.getKey()) == null)
                keysToVars.put(v.getKey(), v);
        }
        debugGetParametersRecurse--;
        return keysToVars.values().iterator();
    }

    /**
     * This method can be overridden by extending objects.
     * For objects (such as instances) that have instance variables that are
     * inherited from some Object that has the default variables, this gets
     * the object that has the default variables. From that object the
     * default values of the variables can then be found.
     * @return the object that holds the default variables and values.
     */
    public ElectricObject getVarDefaultOwner() {
        //checkExamine();
        return this;
    }

// 	/**
// 	 * Method to return the number of persistent Variables on this ElectricObject.
// 	 * A persistent Variable is one that will be saved with the library when written to disk.
// 	 * @return the number of persistent Variables on this ElectricObject.
// 	 */
// 	public int numPersistentVariables()
// 	{
//         //checkExamine();
// 		int numVars = 0;
// 		for (Iterator it = getVariables(); it.hasNext(); )
// 		{
// 			Variable var = (Variable)it.next();
// 			if (var.isDontSave()) continue;
// 			numVars++;
// 		}
// 		return numVars;
// 	}

	/**
	 * Method to return the number of displayable Variables on this ElectricObject.
	 * A displayable Variable is one that will be shown with its object.
	 * Displayable Variables can only sensibly exist on NodeInst and ArcInst objects.
	 * @return the number of displayable Variables on this ElectricObject.
	 */
	public int numDisplayableVariables(boolean multipleStrings)
	{
        //checkExamine();
		int numVars = 0;
		for (Iterator it = getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isDisplay())
			{
				int len = var.getLength();
				if (len > 1 && var.getTextDescriptor().getDispPart() == TextDescriptor.DispPos.NAMEVALUE) len++;
				if (!multipleStrings) len = 1;
				numVars += len;
			}
		}
		return numVars;
	}
	
	/**
	 * Method to handle special case side-effects of setting variables on this ElectricObject.
	 * @param key the Variable key that has changed on this ElectricObject.
	 */
	public void checkPossibleVariableEffects(Variable.Key key)
	{
	}

	/**
	 * Method to add all displayable Variables on this Electric object to an array of Poly objects.
	 * @param rect a rectangle describing the bounds of the object on which the Variables will be displayed.
	 * @param polys an array of Poly objects that will be filled with the displayable Variables.
	 * @param start the starting index in the array of Poly objects to fill with displayable Variables.
	 * @param wnd window in which the Variables will be displayed.
	 * @param multipleStrings true to break multiline text into multiple Polys.
	 * @return the number of Polys that were added.
	 */
	public int addDisplayableVariables(Rectangle2D rect, Poly [] polys, int start, EditWindow wnd, boolean multipleStrings)
	{
        checkExamine();
		int numAddedVariables = 0;
        double cX = rect.getCenterX();
        double cY = rect.getCenterY();
		for (Iterator it = getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDisplay()) continue;
			Poly [] polyList = getPolyList(var, cX, cY, wnd, multipleStrings);
			for(int i=0; i<polyList.length; i++)
			{
				int index = start + numAddedVariables;
				polys[index] = polyList[i];
polys[index].setStyle(Poly.rotateType(polys[index].getStyle(), this));
				numAddedVariables++;
			}
		}
		return numAddedVariables;
	}

	/**
	 * Method to compute a Poly that describes text.
	 * The text can be described by an ElectricObject (Exports or cell instance names).
	 * The text can be described by a node or arc name.
	 * The text can be described by a variable on an ElectricObject.
	 * @param wnd the EditWindow in which the text will be drawn.
	 * @param var the Variable on the ElectricObject (may be null).
	 * @param name the Name of the node or arc in the ElectricObject (may be null).
	 * @return a Poly that covers the text completely.
	 * Even though the Poly is scaled for a particular EditWindow,
	 * its coordinates are in object space, not screen space.
	 */
	public Poly computeTextPoly(EditWindow wnd, Variable var, Name name)
	{
        checkExamine();
		Poly poly = null;
		if (var != null)
		{
			if (this instanceof Export)
			{
				Export pp = (Export)this;
				PortInst pi = pp.getOriginalPort();
				Rectangle2D bounds = pi.getPoly().getBounds2D();
				Poly [] polys = pp.getPolyList(var, bounds.getCenterX(), bounds.getCenterY(), wnd, false);
				if (polys.length > 0)
				{
					poly = polys[0];
					poly.transform(pi.getNodeInst().rotateOut());
				}
			} else if (this instanceof PortInst)
			{
				PortInst pi = (PortInst)this;
				Rectangle2D bounds = pi.getPoly().getBounds2D();
				Poly [] polys = pi.getPolyList(var, bounds.getCenterX(), bounds.getCenterY(), wnd, false);
				if (polys.length > 0)
				{
					poly = polys[0];
					poly.transform(pi.getNodeInst().rotateOut());
				}
			} else if (this instanceof Geometric)
			{
				Geometric geom = (Geometric)this;
				double x = geom.getTrueCenterX();
				double y = geom.getTrueCenterY();
				if (geom instanceof NodeInst)
				{
					NodeInst ni = (NodeInst)geom;
					Rectangle2D uBounds = ni.getUntransformedBounds();
					x = uBounds.getCenterX();
					y = uBounds.getCenterY();
				}
				Poly [] polys = geom.getPolyList(var, x, y, wnd, false);
				if (polys.length > 0)
				{
					poly = polys[0];
					if (geom instanceof NodeInst)
					{
						NodeInst ni = (NodeInst)geom;
						poly.transform(ni.rotateOut());
					}
				}
			} else if (this instanceof Cell)
			{
				Cell cell = (Cell)this;
				Rectangle2D bounds = cell.getBounds();
				Poly [] polys = cell.getPolyList(var, 0, 0, wnd, false);
				if (polys.length > 0) poly = polys[0];
			}
		} else
		{
			if (name != null)
			{
				if (!(this instanceof Geometric)) return null;
				Geometric geom = (Geometric)this;
				TextDescriptor td = geom.getTextDescriptor(this instanceof NodeInst ? NodeInst.NODE_NAME_TD : ArcInst.ARC_NAME_TD);
				Poly.Type style = td.getPos().getPolyType();
				Point2D [] pointList = null;
				if (style == Poly.Type.TEXTBOX)
				{
					pointList = Poly.makePoints(geom.getBounds());
				} else
				{
					pointList = new Point2D.Double[1];
					pointList[0] = new Point2D.Double(geom.getTrueCenterX()+td.getXOff(), geom.getTrueCenterY()+td.getYOff());
				}
				poly = new Poly(pointList);
				poly.setStyle(style);
				if (geom instanceof NodeInst)
				{
					poly.transform(((NodeInst)geom).rotateOutAboutTrueCenter());
				}
				poly.setTextDescriptor(td);
				poly.setString(name.toString());
			} else
			{
				if (this instanceof Export)
				{
					Export pp = (Export)this;
					poly = pp.getNamePoly();
				} else
				{
					// cell instance name
					if (!(this instanceof NodeInst)) return null;
					NodeInst ni = (NodeInst)this;
					TextDescriptor td = ni.getTextDescriptor(NodeInst.NODE_PROTO_TD);
					Poly.Type style = td.getPos().getPolyType();
					Point2D [] pointList = null;
					if (style == Poly.Type.TEXTBOX)
					{
						pointList = Poly.makePoints(ni.getBounds());
					} else
					{
						pointList = new Point2D.Double[1];
						pointList[0] = new Point2D.Double(ni.getTrueCenterX()+td.getXOff(), ni.getTrueCenterY()+td.getYOff());
					}
					poly = new Poly(pointList);
					poly.setStyle(style);
					poly.setTextDescriptor(td);
					poly.setString(ni.getProto().describe());
				}
			}
		}
		if (poly != null)
			poly.setExactTextBounds(wnd, this);
		return poly;
	}

	/**
	 * Method to create an array of Poly objects that describes a displayable Variables on this Electric object.
	 * @param var the Variable on this ElectricObject to describe.
	 * @param cX the center X coordinate of the ElectricObject.
	 * @param cY the center Y coordinate of the ElectricObject.
	 * @param wnd window in which the Variable will be displayed.
	 * @param multipleStrings true to break multiline text into multiple Polys.
	 * @return an array of Poly objects that describe the Variable. May return zero length array.
	 */
	public Poly [] getPolyList(Variable var, double cX, double cY, EditWindow wnd, boolean multipleStrings)
	{
		double offX = var.getXOff();
		double offY = var.getYOff();
		int varLength = var.getLength();
		double height = 0;
		Poly.Type style = var.getPos().getPolyType();
		TextDescriptor td = var.getTextDescriptor();
		if (this instanceof NodeInst && (offX != 0 || offY != 0))
		{
			MutableTextDescriptor mtd = new MutableTextDescriptor(td);
			mtd.setOff(0, 0);
			td = mtd;
		}
		boolean headerString = false;
		Font font = null;
		double scale = 1;
		if (wnd != null)
		{
			font = wnd.getFont(td);
			scale = wnd.getScale();
		}
		if (varLength > 1)
		{
			// compute text height
			if (font == null) height = 1 / scale; else
				height = font.getSize2D() / scale;
			scale *= User.getGlobalTextScale();
			if (td.getDispPart() == TextDescriptor.DispPos.NAMEVALUE)
			{
				headerString = true;
				varLength++;
			}
			if (multipleStrings)
			{
				if (style == Poly.Type.TEXTCENT || style == Poly.Type.TEXTBOX ||
					style == Poly.Type.TEXTLEFT || style == Poly.Type.TEXTRIGHT)
						cY += height * (varLength-1) / 2;
				if (style == Poly.Type.TEXTBOT || style == Poly.Type.TEXTBOTLEFT || style == Poly.Type.TEXTBOTRIGHT)
					cY += height * (varLength-1);
//				if (style == Poly.Type.TEXTTOP || style == Poly.Type.TEXTTOPLEFT || style == Poly.Type.TEXTTOPRIGHT)
//					cY -= height*2;
			} else
			{
				if (style == Poly.Type.TEXTCENT || style == Poly.Type.TEXTBOX ||
					style == Poly.Type.TEXTLEFT || style == Poly.Type.TEXTRIGHT)
						cY -= height * (varLength-1) / 2;
				if (style == Poly.Type.TEXTTOP || style == Poly.Type.TEXTTOPLEFT || style == Poly.Type.TEXTTOPRIGHT)
					cY -= height * (varLength-1);
				varLength = 1;
				headerString = false;
			}
		}

		Poly [] polys = new Poly[varLength];
		for(int i=0; i<varLength; i++)
		{
			String message = null;
			VarContext context = null;
			if (wnd != null) context = wnd.getVarContext();
			TextDescriptor entryTD = td;
			if (varLength > 1 && headerString)
			{
				if (i == 0)
				{
					message = var.getTrueName()+ "[" + (varLength-1) + "]:";
					MutableTextDescriptor mtd = new MutableTextDescriptor(td);
					mtd.setUnderline(true);
					entryTD = mtd;
				} else
				{
					message = var.describe(i-1, context, this);
				}
			} else
			{
				message = var.describe(i, context, this);
			}

			Point2D [] pointList = null;
			if (style == Poly.Type.TEXTBOX && this instanceof Geometric)
			{
				Geometric geom = (Geometric)this;
				Rectangle2D bounds = geom.getBounds();
				pointList = Poly.makePoints(bounds);
			} else
			{
				if (font == null)
				{
					// text too small: make it "greek"
					double fakeWidth = message.length() * entryTD.getTrueSize(wnd) * 0.75 / scale;
					pointList = new Point2D.Double[2];
					pointList[0] = new Point2D.Double(cX+offX-fakeWidth/2, cY+offY);
					pointList[1] = new Point2D.Double(cX+offX+fakeWidth/2, cY+offY);
				} else
				{
					pointList = new Point2D.Double[1];
					pointList[0] = new Point2D.Double(cX+offX, cY+offY);
				}
			}
			polys[i] = new Poly(pointList);
			if (font == null) polys[i].setStyle(Poly.Type.OPENED); else
			{
				polys[i].setString(message);
				polys[i].setStyle(style);
				polys[i].setTextDescriptor(entryTD);
			}
			polys[i].setVariable(var);
			polys[i].setLayer(null);
			cY -= height;
		}
		return polys;
	}

	/**
	 * Method to create a Variable on this ElectricObject with the specified values.
	 * @param name the name of the Variable.
	 * @param value the object to store in the Variable.
	 * @return the Variable that has been created.
	 */
	public Variable newVar(String name, Object value) { return newVar(newKey(name), value); }

	/**
	 * Method to create a Variable on this ElectricObject with the specified values.
	 * @param key the key of the Variable.
	 * @param value the object to store in the Variable.
	 * @return the Variable that has been created.
	 */
	public Variable newVar(Variable.Key key, Object value)
	{
		if (isDeprecatedVariable(key))
		{
			System.out.println("Deprecated variable " + key.getName() + " on " + this);
		}
		checkChanging();
        Variable oldVar;
        synchronized(this) {
            if (vars == null)
                vars = new TreeMap();
            oldVar = (Variable) vars.get(key);
        }
		if (oldVar != null)
		{
			lowLevelUnlinkVar(oldVar);
			if (isDatabaseObject())
				Undo.killVariable(this, oldVar);
		}
		TextDescriptor td = null;
		if (this instanceof Cell) td = TextDescriptor.getCellTextDescriptor(this); else
			if (this instanceof Export) td = TextDescriptor.getExportTextDescriptor(this); else
				if (this instanceof NodeInst) td = TextDescriptor.getNodeTextDescriptor(this); else
					if (this instanceof ArcInst) td = TextDescriptor.getArcTextDescriptor(this); else
						td = TextDescriptor.getAnnotationTextDescriptor(this);
		Variable v = new Variable(this, value, td, key);
		lowLevelLinkVar(v);
		if (isDatabaseObject())
			Undo.newVariable(this, v);
		return v;
	}

	/**
	 * Method to update a Variable on this ElectricObject with the specified values.
	 * If the Variable already exists, only the value is changed; the displayable attributes are preserved.
	 * @param name the name of the Variable.
	 * @param value the object to store in the Variable.
	 * @return the Variable that has been updated.
	 */
	public Variable updateVar(String name, Object value) { return updateVar(newKey(name), value); }

	/**
	 * Method to update a Variable on this ElectricObject with the specified values.
	 * If the Variable already exists, only the value is changed; the displayable attributes are preserved.
	 * @param key the key of the Variable.
	 * @param value the object to store in the Variable.
	 * @return the Variable that has been updated.
	 */
	public Variable updateVar(Variable.Key key, Object value)
	{
		Variable var = getVar(key);
		if (var == null)
		{
			return newVar(key, value);
		}

		// set the variable
		Variable newVar = newVar(key, value);
		if (newVar == null) return null;

		// restore values
		newVar.setTextDescriptor(var.getTextDescriptor());
        newVar.copyFlags(var);
		lowLevelModVar(var);
		return newVar;
	}

	/**
	 * Updates the TextDescriptor on this ElectricObject selected by varName.
	 * The varName may be a name of variable on this ElectricObject or one of the
	 * special names:
	 * NodeInst.NODE_NAME_TD
	 * NodeInst.NODE_PROTO_TD
	 * ArcInst.ARC_NAME_TD
	 * Export.EXPORT_NAME_TD
	 * Other strings are not considered special, even they are equal to one of the
	 * special name. In other words, special names are compared by "==" other than
	 * by "equals".
	 * If varName doesn't select any text descriptor, no action is performed.
	 * The TextDescriptor gives information for displaying the Variable.
	 * @param varName name of variable or special name.
	 * @param td new value TextDescriptor
	 */
	public void setTextDescriptor(String varName, TextDescriptor td)
	{
		checkChanging();
		Variable var = getVar(varName);
		if (var == null) return;
		var.setTextDescriptor(td);
	}

	/**
	 * Method to set the X and Y offsets of the text in the TextDescriptor selected by name of
	 * variable or special name.
	 * The values are scaled by 4, so a value of 3 indicates a shift of 0.75 and a value of 4 shifts by 1.
	 * @param varName name of variable or special name.
	 * @param xd the X offset of the text in the TextDescriptor.
	 * @param yd the Y offset of the text in the TextDescriptor.
	 * @see #setTextDescriptor(java.lang.String,com.sun.electric.database.variable.TextDescriptor)
	 * @see com.sun.electric.database.variable.Variable#setOff(double,double)
	 */
	public synchronized void setOff(String varName, double xd, double yd)
	{
		MutableTextDescriptor td = getMutableTextDescriptor(varName);
		if (td == null) return;
		td.setOff(xd, yd);
		setTextDescriptor(varName, td);
	}

	/**
	 * Method to copy text descriptor from another ElectricObject to this ElectricObject.
	 * @param other the other ElectricObject from which to copy Variables.
	 * @param varName selector of textdescriptor
	 */
	public void copyTextDescriptorFrom(ElectricObject other, String varName)
	{
		TextDescriptor td = other.getTextDescriptor(varName);
		if (td == null) return;
		setTextDescriptor(varName, td);
	}

    /**
     * Rename a Variable. Note that this creates a new variable of
     * the new name and copies all values from the old variable, and
     * then deletes the old variable.
     * @param name the name of the var to rename
     * @param newName the new name of the variable
     * @return the new renamed variable
     */
    public Variable renameVar(String name, String newName) {
        return renameVar(findKey(name), newName);
    }

    /**
     * Rename a Variable. Note that this creates a new variable of
     * the new name and copies all values from the old variable, and
     * then deletes the old variable.
     * @param key the name key of the var to rename
     * @param newName the new name of the variable
     * @return the new renamed variable, or null on error (no action taken)
     */
    public Variable renameVar(Variable.Key key, String newName) {
        // see if newName exists already
        Variable var = getVar(newName);
        if (var != null) return null;            // name already exists

        // get current Variable
        Variable oldvar = getVar(key);
        if (oldvar == null) return null;

        // create new var
        Variable newVar = newVar(newName, oldvar.getObject());
        if (newVar == null) return null;
        // copy settings from old var to new var
        newVar.setTextDescriptor(oldvar.getTextDescriptor());
        newVar.copyFlags(oldvar);
        // delete old var
        delVar(oldvar.getKey());

        return newVar;
    }

	/**
	 * Method to delete a Variable from this ElectricObject.
	 * @param key the key of the Variable to delete.
	 */
	public void delVar(Variable.Key key)
	{
		checkChanging();
		Variable v = getVar(key);
		if (v == null) return;
		lowLevelUnlinkVar(v);
		if (isDatabaseObject())
			Undo.killVariable(this, v);
	}

	/**
	 * Low-level access method to link a Variable into this ElectricObject.
	 * @param var Variable to link
	 */
	public void lowLevelLinkVar(Variable var)
	{
        synchronized(this) {
		    vars.put(var.getKey(), var);
        }
        var.setLinked(true);

		// check for side-effects of the change
		checkPossibleVariableEffects(var.getKey());
	}

	/**
	 * Low-level access method to unlink a Variable from this ElectricObject.
	 * @param var Variable to unlink.
	 */
	public void lowLevelUnlinkVar(Variable var)
	{
        synchronized(this) {
		    vars.remove(var.getKey());
        }
        var.setLinked(false);

		// check for side-effects of the change
		checkPossibleVariableEffects(var.getKey());
	}

	/**
	 * Low-level access method to change a Variable from this ElectricObject.
	 * @param var Variable that changed.
	 */
	public void lowLevelModVar(Variable var)
	{
		// check for side-effects of the change
		checkPossibleVariableEffects(var.getKey());
	}

	/**
	 * Method to put an Object into an entry in an arrayed Variable on this ElectricObject.
	 * @param key the key of the arrayed Variable.
	 * @param value the object to store in an entry of the arrayed Variable.
	 * @param index the location in the arrayed Variable to store the value.
	 */
	public void setVar(Variable.Key key, Object value, int index)
	{
		checkChanging();
		Variable v = getVar(key);
		if (v == null) return;
		Object addr = v.getObject();
		if (addr instanceof Object[])
		{
			Object[] arr = (Object[])addr;
			Object oldVal = arr[index];
			arr[index] = value;
			if (isDatabaseObject())
				Undo.modifyVariable(this, v, index, oldVal);
		}
		lowLevelModVar(v);
	}

	/**
	 * Method to insert an Object into an arrayed Variable on this ElectricObject.
	 * @param key the key of the arrayed Variable.
	 * @param index the location in the arrayed Variable to insert the value.
	 * @param value the object to insert into the arrayed Variable.
	 */
	public void insertInVar(Variable.Key key, int index, Object value)
	{
		checkChanging();
		Variable v = getVar(key);
		if (v == null) return;
		Object addr = v.getObject();
		if (addr instanceof Object[])
		{
			v.lowLevelInsert(index, value);
			if (isDatabaseObject())
				Undo.insertVariable(this, v, index);
		}
		lowLevelModVar(v);
	}

	/**
	 * Method to delete an Object from an arrayed Variable on this ElectricObject.
	 * @param key the key of the arrayed Variable.
	 * @param index the location in the arrayed Variable to delete the value.
	 */
	public void deleteFromVar(Variable.Key key, int index)
	{
		checkChanging();
		Variable v = getVar(key);
		if (v == null) return;
		Object addr = v.getObject();
		if (addr instanceof Object[])
		{
			Object oldVal = ((Object[])addr)[index];
			v.lowLevelDelete(index);
			if (isDatabaseObject())
				Undo.deleteVariable(this, v, index, oldVal);
		}
		lowLevelModVar(v);
	}

	/**
	 * Method to copy all variables from another ElectricObject to this ElectricObject.
	 * @param other the other ElectricObject from which to copy Variables.
	 */
	public void copyVarsFrom(ElectricObject other)
	{
		checkChanging();
        Iterator it = other.getVariables();
        synchronized(this) {
            while(it.hasNext())
            {
                Variable var = (Variable)it.next();
                Variable newVar = this.newVar(var.getKey(), var.getObject());
                if (newVar != null)
                {
                    newVar.copyFlags(var);
                    newVar.setTextDescriptor(var.getTextDescriptor());
                }
            }
        }
	}

	private static class ArrayName
	{
		private String baseName;
		private String indexPart;
	}

	/**
	 * Method to return a unique object name in a Cell.
	 * @param name the original name that is not unique.
	 * @param cell the Cell in which this name resides.
	 * @param cls the class of the object on which this name resides.
	 * @return a unique name for that class in that Cell.
	 */
	public static String uniqueObjectName(String name, Cell cell, Class cls)
	{
		// first see if the name is unique
		if (cell.isUniqueName(name, cls, null)) return name;

		// see if there is a "++" anywhere to tell us what to increment
		int plusPlusPos = name.indexOf("++");
		if (plusPlusPos >= 0)
		{
			int numStart = plusPlusPos;
			while (numStart > 0 && TextUtils.isDigit(name.charAt(numStart-1))) numStart--;
			if (numStart < plusPlusPos)
			{
				int nextIndex = TextUtils.atoi(name.substring(numStart)) + 1;
				for( ; ; nextIndex++)
				{
					String newname = name.substring(0, numStart) + nextIndex + name.substring(plusPlusPos);
					if (cell.isUniqueName(newname, cls, null)) return newname;
				}
			}
		}

		// see if there is a "--" anywhere to tell us what to decrement
		int minusMinusPos = name.indexOf("--");
		if (minusMinusPos >= 0)
		{
			int numStart = minusMinusPos;
			while (numStart > 0 && TextUtils.isDigit(name.charAt(numStart-1))) numStart--;
			if (numStart < minusMinusPos)
			{
				int nextIndex = TextUtils.atoi(name.substring(numStart)) - 1;
				for( ; nextIndex >= 0; nextIndex--)
				{
					String newname = name.substring(0, numStart) + nextIndex + name.substring(minusMinusPos);
					if (cell.isUniqueName(newname, cls, null)) return newname;
				}
			}
		}

		char separateChar = '_';

		// break the string into a list of ArrayName objects
		List names = new ArrayList();
		boolean inBracket = false;
		int len = name.length();
		int startOfBase = 0;
		int startOfIndex = -1;
		for(int i=0; i<len; i++)
		{
			char ch = name.charAt(i);
			if (ch == '[')
			{
				if (startOfIndex < 0) startOfIndex = i;
				inBracket = true;
			}
			if (ch == ']') inBracket = false;
			if ((ch == ',' && !inBracket) || i == len-1)
			{
				// remember this arrayname
				if (i == len-1) i++;
				ArrayName an = new ArrayName();
				int endOfBase = startOfIndex;
				if (endOfBase < 0) endOfBase = i;
				an.baseName = name.substring(startOfBase, endOfBase);
				if (startOfIndex >= 0) an.indexPart = name.substring(startOfIndex, i);
				names.add(an);
				startOfBase = i+1;
				startOfIndex = -1;
			}
		}

		for(Iterator it = names.iterator(); it.hasNext(); )
		{
			ArrayName an = (ArrayName)it.next();

			// adjust the index part if possible
			boolean indexAdjusted = false;
			String index = an.indexPart;
			if (index != null)
			{
				int possibleEnd = 0;
				int nameLen = index.length();

				// see if the index part can be incremented
				int possibleStart = -1;
				int endPos = nameLen-1;
				for(;;)
				{
					// find the range of characters in square brackets
					int startPos = index.lastIndexOf('[', endPos);
					if (startPos < 0) break;
	
					// see if there is a comma in the bracketed expression
					int i = index.indexOf(',', startPos);
					if (i >= 0 && i < endPos)
					{
						// this bracketed expression cannot be incremented: move on
						if (startPos > 0 && index.charAt(startPos-1) == ']')
						{
							endPos = startPos-1;
							continue;
						}
						break;
					}
	
					// see if there is a colon in the bracketed expression
					i = index.indexOf(':', startPos);
					if (i >= 0 && i < endPos)
					{
						// colon: make sure there are two numbers
						String firstIndex = index.substring(startPos+1, i);
						String secondIndex = index.substring(i+1, endPos);
						if (TextUtils.isANumber(firstIndex) && TextUtils.isANumber(secondIndex))
						{
							int startIndex = TextUtils.atoi(firstIndex);
							int endIndex = TextUtils.atoi(secondIndex);
							int spacing = Math.abs(endIndex - startIndex) + 1;
							for(int nextIndex = 1; ; nextIndex++)
							{
								String newIndex = index.substring(0, startPos) + "[" + (startIndex+spacing*nextIndex) +
									":" + (endIndex+spacing*nextIndex) + index.substring(endPos);
								if (cell.isUniqueName(an.baseName + newIndex, cls, null))
								{
									indexAdjusted = true;
									an.indexPart = newIndex;
									break;
								}
							}
							if (indexAdjusted) break;
						}
	
						// this bracketed expression cannot be incremented: move on
						if (startPos > 0 && index.charAt(startPos-1) == ']')
						{
							endPos = startPos-1;
							continue;
						}
						break;
					}
	
					// see if this bracketed expression is a pure number
					String bracketedExpression = index.substring(startPos+1, endPos);
					if (TextUtils.isANumber(bracketedExpression))
					{
						int nextIndex = TextUtils.atoi(bracketedExpression) + 1;
						for(; ; nextIndex++)
						{
							String newIndex = index.substring(0, startPos) + "[" + nextIndex + index.substring(endPos);
							if (cell.isUniqueName(an.baseName + newIndex, cls, null))
							{
								indexAdjusted = true;
								an.indexPart = newIndex;
								break;
							}
						}
						if (indexAdjusted) break;
					}
	
					// remember the first index that could be incremented in a pinch
					if (possibleStart < 0)
					{
						possibleStart = startPos;
						possibleEnd = endPos;
					}
	
					// this bracketed expression cannot be incremented: move on
					if (startPos > 0 && index.charAt(startPos-1) == ']')
					{
						endPos = startPos-1;
						continue;
					}
					break;
				}
	
				// if there was a possible place to increment, do it
				if (!indexAdjusted && possibleStart >= 0)
				{
					// nothing simple, but this one can be incremented
					int i;
					for(i=possibleEnd-1; i>possibleStart; i--)
						if (!TextUtils.isDigit(index.charAt(i))) break;
					int nextIndex = TextUtils.atoi(index.substring(i+1)) + 1;
					int startPos = i+1;
					if (index.charAt(startPos-1) == separateChar) startPos--;
					for(; ; nextIndex++)
					{
						String newIndex = index.substring(0, startPos) + separateChar + nextIndex + index.substring(possibleEnd);
						if (cell.isUniqueName(an.baseName + newIndex, cls, null))
						{
							indexAdjusted = true;
							an.indexPart = newIndex;
							break;
						}
					}
				}
			}

			// if the index was not adjusted, adjust the base part
			if (!indexAdjusted)
			{
				// array contents cannot be incremented: increment base name
				String base = an.baseName;
				int startPos = base.length();
				int endPos = base.length();
	
				// if there is a numeric part at the end, increment that
				String localSepString = String.valueOf(separateChar);
				while (startPos > 0 && TextUtils.isDigit(base.charAt(startPos-1))) startPos--;
				int nextIndex = 1;
				if (startPos >= endPos)
				{
					if (startPos > 0 && base.charAt(startPos-1) == separateChar) startPos--;
				} else
				{
					nextIndex = TextUtils.atoi(base.substring(startPos)) + 1;
					localSepString = "";
				}
	
				// find the unique index to use
				String prefix = base.substring(0, startPos) + localSepString;
				int uniqueIndex = cell.getUniqueNameIndex(prefix, cls, nextIndex);
				an.baseName = prefix + uniqueIndex + base.substring(endPos);
			}
		}
		StringBuffer result = new StringBuffer();
		boolean first = true;
		for(Iterator it = names.iterator(); it.hasNext(); )
		{
			if (first) first = false; else
				result.append(",");
			ArrayName an = (ArrayName)it.next();
			result.append(an.baseName);
			if (an.indexPart != null) result.append(an.indexPart);
		}
		return result.toString();
	}

	/**
	 * Method to determine whether a Variable key on this object is deprecated.
	 * Deprecated Variable keys are those that were used in old versions of Electric,
	 * but are no longer valid.
	 * @param key the key of the Variable.
	 * @return true if the Variable key is deprecated.
	 */
	public boolean isDeprecatedVariable(Variable.Key key)
	{
		String name = key.getName();
		if (name.length() == 0) return true;
		if (name.length() == 1)
		{
			char chr = name.charAt(0);
			if (!Character.isLetter(chr)) return true;
		}
		return false;
	}

	/**
	 * Method to return an Iterator over all Variables on this ElectricObject.
	 * @return an Iterator over all Variables on this ElectricObject.
	 */
	public synchronized Iterator getVariables()
	{
        //checkExamine();
		if (vars == null)
			return (new ArrayList()).iterator();
		return (new ArrayList(vars.values())).iterator();
	}

	/**
	 * Method to return the number of Variables on this ElectricObject.
	 * @return the number of Variables on this ElectricObject.
	 */
	public synchronized int getNumVariables()
	{
        //checkExamine();
		if (vars == null) return 0;
		return vars.size();
	}

	/**
	 * Method to return an Iterator over all Variable keys.
	 * @return an Iterator over all Variable keys.
	 */
	public static synchronized Iterator getVariableKeys()
	{
		return varKeys.values().iterator();
	}

	/**
	 * Method to return the total number of different Variable names on all ElectricObjects.
	 * @return the total number of different Variable names on all ElectricObjects.
	 */
	public static synchronized int getNumVariableKeys()
	{
		return varKeys.keySet().size();
	}

	/**
	 * Routing to check whether changing of this cell allowed or not.
	 * By default checks whole database change. Overriden in subclasses.
	 */
	public void checkChanging() {
//        if (!isDatabaseObject()) return;
        Job.checkChanging();
    }

    public void checkExamine() {
        if (!isDatabaseObject()) return;
        Job.checkExamine();
    }

	/**
	 * Method which indicates that this object is in database.
	 * Some objects are not in database, for example Geometrics in PaletteFrame.
	 * @return true if this object is in database, false if it is not a database object,
     * or if it is a dummy database object (considered not to be in the database).
	 */
	protected boolean isDatabaseObject() { return true; }

	/**
	 * Method to return the Key object for a given Variable name.
	 * Variable Key objects are caches of the actual string name of the Variable.
	 * @return the Key object for a given Variable name.
	 */
	private static synchronized Variable.Key findKey(String name)
	{
		Variable.Key key = (Variable.Key)varKeys.get(name);
		if (key == null)
		{
			String lowCaseName = name.toLowerCase();
			if (!lowCaseName.equals(name))
				key = (Variable.Key)varLowCaseKeys.get(lowCaseName);
		}
		return key;
	}

	/**
	 * Method to find or create the Key object for a given Variable name.
	 * Variable Key objects are caches of the actual string name of the Variable.
	 * @param name given Variable name.
	 * @return the Key object for a given Variable name.
	 */
	public static synchronized Variable.Key newKey(String name)
	{
		Variable.Key key = findKey(name);
		if (key != null) return key;
		key = new Variable.Key(name);
		varKeys.put(name, key);
		varLowCaseKeys.put(name.toLowerCase(), key);
		return key;
	}

	/**
	 * Method to determine the appropriate Cell associated with this ElectricObject.
	 * @return the appropriate Cell associated with this ElectricObject.
	 * Returns null if no Cell can be found.
	 */
	public Cell whichCell() { return null; }

	/**
	 * Method to write a description of this ElectricObject (lists all Variables).
	 * Displays the description in the Messages Window.
	 */
	public void getInfo()
	{
        checkExamine();
		boolean firstvar = true;
		for(Iterator it = getVariables(); it.hasNext() ;)
		{
            Variable val = (Variable)it.next();
            Variable.Key key = val.getKey();
			if (val == null) continue;
			if (firstvar) System.out.println("Variables:");   firstvar = false;
			Object addr = val.getObject();
            TextDescriptor td = val.getTextDescriptor();
            String par = td.isParam() ? "(param)" : "";
			if (addr instanceof Object[])
			{
				Object[] ary = (Object[]) addr;
				System.out.print("   " + key.getName() + "(" + ary.length + ") = [");
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
				System.out.println("] "+par);
			} else
			{
				System.out.println("   " + key.getName() + "= " + addr + " "+par);
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

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Geometric.java
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
package com.sun.electric.database.geometry;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 * This class is the superclass for the Electric classes that have visual
 * bounds on the screen, specifically NodeInst and ArcInst.
 */
public abstract class Geometric extends ElectricObject
{
	// ------------------------------- private data ------------------------------

	/** Cell containing this Geometric object. */			protected Cell parent;

	// ------------------------ private and protected methods--------------------

	/**
	 * The constructor is only called from subclasses.
	 */
	protected Geometric() {}

	/**
	 * Method to set the parent Cell of this Geometric.
	 * @param parent the parent Cell of this Geometric.
	 */
	protected void setParent(Cell parent) { this.parent = parent; }

	/**
	 * Method to describe this Geometric as a string.
	 * This method is overridden by NodeInst and ArcInst.
	 * @return a description of this Geometric as a string.
	 */
	public String describe() { return "?"; }

	/**
	 * Routing to check whether changing of this cell allowed or not.
	 * By default checks whole database change. Overriden in subclasses.
	 */
	public void checkChanging() { if (parent != null) parent.checkChanging(); }

	/**
	 * Method to determine the appropriate Cell associated with this ElectricObject.
	 * @return the appropriate Cell associated with this ElectricicObject.
	 */
	public Cell whichCell() { return parent; }

	/**
	 * Method to determine which page of a multi-page schematic this Geometric is on.
	 * @return the page number (0-based).
	 */
	public int whichMultiPage()
	{
		int pageNo = 0;
		if (parent.isMultiPage())
		{
			double cY = getBounds().getCenterY();
			pageNo = (int)((cY+Cell.FrameDescription.MULTIPAGESEPARATION/2) / Cell.FrameDescription.MULTIPAGESEPARATION);
		}
		return pageNo;
	}

	/**
	 * Method which indicates that this object is in database.
	 * Some objects are not in database, for example Geometrics in PaletteFrame.
	 * @return true if this object is in database.
	 */
	protected boolean isDatabaseObject() { return parent != null; }

	/**
	 * Method to write a description of this Geometric.
	 * Displays the description in the Messages Window.
	 */
	public void getInfo()
	{
		Rectangle2D visBounds = getBounds();
		System.out.println(" Bounds: (" + visBounds.getCenterX() + "," + visBounds.getCenterY() + "), size: " +
			visBounds.getWidth() + "x" + visBounds.getHeight());
		System.out.println(" Parent cell: " + parent.describe());
        super.getInfo();
	}

	// ------------------------ public methods -----------------------------

	/**
	 * Method to return the Cell that contains this Geometric object.
	 * @return the Cell that contains this Geometric object.
	 */
	public Cell getParent() { return parent; }

	/**
	 * Method to return the name of this Geometric.
	 * @return the name of this Geometric, null if there is no name.
	 */
	public String getName()
	{
		Name name = getNameKey();
		return name != null ? name.toString() : null;
	}

	/**
	 * Method to return the name key of this Geometric.
	 * @return the name key of this Geometric, null if there is no name.
	 */
	public abstract Name getNameKey();

	/**
	 * Method to return the duplicate index of this Geometric.
	 * @return the duplicate index of this Geometric.
	 */
	public abstract int getDuplicate();

	/**
	 * Method to rename this Geometric.
	 * This Geometric must be linked to database.
	 * @param name new name of this geometric.
	 * @return true on error
	 */
	public boolean setName(String name)
	{
		assert isLinked();
		Name key = null;
		if (name != null && name.length() > 0)
		{
			if (name.equals(getName())) return false;
			key = Name.findName(name);
		} else
		{
			if (!isUsernamed()) return false;
			key = parent.getAutoname(getBasename());
		}
		if (checkNameKey(key)) return true;
		if (parent.hasTempName(key) && !name.equalsIgnoreCase(getName()))
		{
			System.out.println(parent + " already has Geometric with temporary name \""+name+"\"");
			return true;
		}
		Name oldName = getNameKey();
		int oldDuplicate = getDuplicate();
		lowLevelRename(key, -1);
		Undo.renameGeometric(this, oldName, oldDuplicate);
		return false;
	}

	/**
	 * Method to check the new name key of this Geometric.
	 * @param name new name key of this geometric.
	 * @return true on error.
	 */
	protected boolean checkNameKey(Name name)
	{
		if (!name.isValid())
		{
			System.out.println(parent + ": Invalid name \""+name+"\" wasn't assigned to " +
				(this instanceof NodeInst ? "node" : "arc") + " :" + Name.checkName(name.toString()));
			return true;
		}
		if (name.isTempname() && name.isBus())
		{
			System.out.println(parent + ": Temporary name \""+name+"\" can't be bus");
			return true;
		}
		if (name.hasEmptySubnames())
		{
			if (name.isBus())
				System.out.println(parent + ": Name \""+name+"\" with empty subnames wasn't assigned to " +
					(this instanceof NodeInst ? "node" : "arc"));
			else
				System.out.println(parent + ": Cannot assign empty name \""+name+"\" to " +
					(this instanceof NodeInst ? "node" : "arc"));
			return true;
		}
		return false;
	}

	/**
	 * Low-level access method to change name of this Geometric.
	 * @param name new name of this Geometric.
	 * @param duplicate new duplicate number of this Geometric or negative value.
	 */
	public abstract void lowLevelRename(Name name, int duplicate);

	/**
	 * Abstract method gives prefix for autonaming.
	 * @return true if this Geometric is linked to parent Cell.
	 */
	public abstract Name getBasename();

	/**
	 * Retruns true if this Geometric was named by user.
	 * @return true if this Geometric was named by user.
	 */		
	public boolean isUsernamed()
	{
		Name name = getNameKey();
		return name != null && !name.isTempname();
	}

	/**
	 * Method to return the bounds of this Geometric.
	 * @return the bounds of this Geometric.
	 */
	public abstract Rectangle2D getBounds();

	/**
	 * Method to return the center X coordinate of this Geometric.
	 * @return the center X coordinate of this Geometric.
	 */
	public double getTrueCenterX() { return getBounds().getCenterX(); }

	/**
	 * Method to return the center Y coordinate of this Geometric.
	 * @return the center Y coordinate of this Geometric.
	 */
	public double getTrueCenterY() { return getBounds().getCenterY(); }

	/**
	 * Method to return the center coordinate of this Geometric.
	 * @return the center coordinate of this Geometric.
	 */
	public Point2D getTrueCenter() { return new Point2D.Double(getTrueCenterX(), getTrueCenterY()); }

	/**
	 * Method to return the number of displayable Variables on this ElectricObject.
	 * A displayable Variable is one that will be shown with its object.
	 * Displayable Variables can only sensibly exist on NodeInst and ArcInst objects.
	 * @return the number of displayable Variables on this ElectricObject.
	 */
	public int numDisplayableVariables(boolean multipleStrings)
	{
		return super.numDisplayableVariables(multipleStrings) + (isUsernamed()?1:0);
	}

	/**
	 * Method to add all displayable Variables on this Electric object to an array of Poly objects.
	 * @param rect a rectangle describing the bounds of the object on which the Variables will be displayed.
	 * @param polys an array of Poly objects that will be filled with the displayable Variables.
	 * @param start the starting index in the array of Poly objects to fill with displayable Variables.
	 * @return the number of Variables that were added.
	 */
	public int addDisplayableVariables(Rectangle2D rect, Poly [] polys, int start, EditWindow wnd, boolean multipleStrings)
	{
		int numVars = 0;
		if (isUsernamed())
		{
			double cX = rect.getCenterX();
			double cY = rect.getCenterY();
			String varName = this instanceof NodeInst ? NodeInst.NODE_NAME_TD : ArcInst.ARC_NAME_TD;
			TextDescriptor td = getTextDescriptor(varName);
			double offX = td.getXOff();
			double offY = td.getYOff();
			TextDescriptor.Position pos = td.getPos();
			Poly.Type style = pos.getPolyType();

			if (this instanceof NodeInst && (offX != 0 || offY != 0))
			{
				MutableTextDescriptor mtd = new MutableTextDescriptor(td);
				mtd.setOff(0, 0);
				td = mtd;
			    style = Poly.rotateType(style, this);
			}

			Point2D [] pointList = null;
			if (style == Poly.Type.TEXTBOX)
			{
				pointList = Poly.makePoints(rect);
			} else
			{
				pointList = new Point2D.Double[1];
				pointList[0] = new Point2D.Double(cX+offX, cY+offY);
			}
			polys[start] = new Poly(pointList);
			polys[start].setStyle(style);
			polys[start].setString(getName());
			polys[start].setTextDescriptor(td);
			polys[start].setLayer(null);
			//polys[start].setVariable(var); ???
			polys[start].setName(getNameKey());
			numVars = 1;
		}
		return super.addDisplayableVariables(rect, polys, start+numVars, wnd, multipleStrings) + numVars;
	}

    /**
     * Method to tell whether the objects at geometry modules "geom1" and "geom2"
     * touch directly (that is, an arcinst connected to a nodeinst).  The method
     * returns true if they touch. Used by DRC and Parasitic tools
     */
    public static boolean objectsTouch(Geometric geom1, Geometric geom2)
    {
        if (geom1 instanceof NodeInst)
        {
            if (geom2 instanceof NodeInst) return false;
            Geometric temp = geom1;   geom1 = geom2;   geom2 = temp;
        }
        if (!(geom2 instanceof NodeInst))
            return false;

        // see if the arcinst at "geom1" touches the nodeinst at "geom2"
        NodeInst ni = (NodeInst)geom2;
        ArcInst ai = (ArcInst)geom1;
        for(int i=0; i<2; i++)
        {
            Connection con = ai.getConnection(i);
            if (con.getPortInst().getNodeInst() == ni) return true;
        }
        return false;
    }
}

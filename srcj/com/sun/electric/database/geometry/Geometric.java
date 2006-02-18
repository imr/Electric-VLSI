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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This class is the superclass for the Electric classes that have visual
 * bounds on the screen, specifically NodeInst and ArcInst.
 */
public abstract class Geometric extends ElectricObject
{
	// ------------------------------- private data ------------------------------

	/** Cell containing this Geometric object. */			protected final Cell parent;

	// ------------------------ private and protected methods--------------------

	/**
	 * The constructor is only called from subclasses.
	 */
	protected Geometric(Cell parent) {
        this.parent = parent;
    }

	/**
	 * Method to describe this Geometric as a string.
	 * This method is overridden by NodeInst and ArcInst.
     * @param withQuotes to wrap description between quotes
	 * @return a description of this Geometric as a string.
	 */
	public String describe(boolean withQuotes) { return "?"; }

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
	 * Returns database to which this Geometric belongs.
	 * Some objects are not in database, for example Geometrics in PaletteFrame.
     * Method returns null for non-database objects.
     * @return database to which this Geometric belongs.
	 */
	public EDatabase getDatabase() { return parent != null ? parent.getDatabase() : null; }

	/**
	 * Method to write a description of this Geometric.
	 * Displays the description in the Messages Window.
	 */
	public void getInfo()
	{
		Rectangle2D visBounds = getBounds();
		System.out.println(" Bounds: (" + visBounds.getCenterX() + "," + visBounds.getCenterY() + "), size: " +
			visBounds.getWidth() + "x" + visBounds.getHeight());
		System.out.println(" Parent: " + parent);
        super.getInfo();
	}

	// ------------------------ public methods -----------------------------

	/**
	 * Method to return the Cell that contains this Geometric object.
	 * @return the Cell that contains this Geometric object.
	 */
	public Cell getParent() { return parent; }

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
            if (ai.getPortInst(i).getNodeInst() == ni) return true;
        }
        return false;
    }
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PortInst.java
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
package com.sun.electric.database.topology;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * The PortInst class represents an instance of a Port.  It is the
 * combination of a NodeInst and a PortProto.
 * <P>
 * This class is thread-safe.
 */
public class PortInst extends ElectricObject
{
	// ------------------------ private data ------------------------

	private final NodeInst nodeInst;
	private final PortProto portProto;

	// -------------------protected or private methods ---------------

	private PortInst(PortProto portProto, NodeInst nodeInst) {
        this.portProto = portProto;
        this.nodeInst = nodeInst;
        setLinked(false);
    }

	// ------------------------ public methods -------------------------

	/**
	 * Method to create a PortInst object.
	 * @param portProto the PortProto on the prototype of the NodeInst.
	 * @param nodeInst the NodeInst that owns the port.
	 * @return the newly created PortInst.
	 */
	public static PortInst newInstance(PortProto portProto, NodeInst nodeInst)
	{
		PortInst pi = new PortInst(portProto, nodeInst);
		return pi;
	}

	/**
	 * Method to return the NodeInst that this PortInst resides on.
	 * @return the NodeInst that this PortInst resides on.
	 */
	public NodeInst getNodeInst() { return nodeInst; }

	/**
	 * Method to return the PortProto that this PortInst is an instance of.
	 * @return the PortProto that this PortInst is an instance of.
	 */
	public PortProto getPortProto() { return portProto; }

	/**
	 * Method to get the index of this PortInst in NodeInst ports.
	 * @return index of this PortInst in NodeInst ports.
	 */
	public final int getPortIndex() { return portProto.getPortIndex(); }

    /**
     * Get iterator of all Connections
     * that connect to this PortInst
     * @return an iterator over associated Connections
     */
    public Iterator getConnections() {
        List connections = new ArrayList();
        // get connections on NodeInst
        for (Iterator it = nodeInst.getConnections(); it.hasNext(); ) {
            Connection c = (Connection)it.next();
            if (c.getPortInst() == this)
                connections.add(c);
        }
        return connections.iterator();
    }

	/**
	 ** Method to return the equivalent PortProto of this PortInst's PortProto.
	 * This is typically used to find the PortProto in the schematic view.
	 * @return the equivalent PortProto of this PortInst's PortProto, or null if not found.
	 */
    public PortProto getProtoEquivalent() 
    {
		return portProto instanceof Export ? ((Export)portProto).getEquivalent() : portProto;
    }
    
	/**
	 * Method to return the bounds of this PortInst.
	 * The bounds are determined by getting the Poly and bounding it.
	 * @return the bounds of this PortInst.
	 */
	public Rectangle2D getBounds()
	{
		Rectangle2D r = nodeInst.getShapeOfPort(portProto).getBounds2D();
		return r;
	}

	/**
	 * Method to return the Poly that describes this PortInst.
	 * @return the Poly that describes this PortInst.
	 */
	public Poly getPoly()
	{
		return nodeInst.getShapeOfPort(portProto);
	}

	/**
	 * Method to add all displayable Variables on this PortInsts to an array of Poly objects.
	 * @param rect a rectangle describing the bounds of the NodeInst on which the PortInsts reside.
	 * @param polys an array of Poly objects that will be filled with the displayable Variables.
	 * @param start the starting index in the array of Poly objects to fill with displayable Variables.
	 * @param wnd window in which the Variables will be displayed.
	 * @param multipleStrings true to break multiline text into multiple Polys.
	 * @return the number of Polys that were added.
	 */
	public int addDisplayableVariables(Rectangle2D rect, Poly [] polys, int start, EditWindow wnd, boolean multipleStrings)
	{
		if (super.numDisplayableVariables(multipleStrings) == 0) return 0;

		Poly portPoly = getPoly();
		int justAdded = super.addDisplayableVariables(portPoly.getBounds2D(), polys, start, wnd, multipleStrings);
		for(int i=0; i<justAdded; i++)
			polys[start+i].setPort(getPortProto());
		return justAdded;
	}

	/**
	 * Method to describe this NodeInst as a string.
	 * @return a description of this NodeInst as a string.
	 */
	public String describe()
	{
		return nodeInst.describe() + "." + portProto.getName();
	}

	/**
	 * Returns a printable version of this PortInst.
	 * @return a printable version of this PortInst.
	 */
	public String toString()
	{
		return "PortInst " + describe();
	}

	/**
	 * This function is to compare PortInst elements. Initiative CrossLibCopy
 	 * @param obj Object to compare to
	 * @param buffer To store comparison messages in case of failure
	 * @return True if objects represent same PortInst
	 */
    public boolean compare(Object obj, StringBuffer buffer)
	{
		if (this == obj) return (true);

        // Better if compare classes? but it will crash with obj=null
        if (obj == null || getClass() != obj.getClass())
            return (false);

        PortInst no = (PortInst)obj;
        Set noCheckAgain = new HashSet();
		for (Iterator it = getConnections(); it.hasNext(); )
		{
			Connection c = (Connection)it.next();
			boolean found = false;
			for (Iterator noIt = no.getConnections(); noIt.hasNext(); )
			{
				Connection noC = (Connection)noIt  .next();
				if (noCheckAgain.contains(noC)) continue;
				if (c.getLocation().equals(noC.getLocation()))
				{
					found = true;
                    noCheckAgain.add(noC);
                    break;
				}
			}

            // No correspoding NodeInst found
            if (!found)
            {
	            if (buffer != null)
	                buffer.append("No corresponding port " + this + " found in " + no + "\n");
	            return (false);
            }
		}
        
		// @TODO GVG Check this
		// Just compare connections?? or just poly for now?
		Poly poly = getPoly();
		Poly noPoly = no.getPoly();
		boolean check = poly.compare(noPoly, buffer);

		if (!check && buffer != null)
			buffer.append("No same ports detected in " + portProto.getName() + " and " + no.getPortProto().getName() + "\n");
        return (check);
    }

    /**
     * Overrides ElectricObject.isLinked().  This is because a PortInst is a derived
     * database object, and is never explicitly linked or unlinked.  It represents a NodeInst
     * and PortProto pair. So, this method really returns it's nodeinst's isLinked()
     * value.
     * @return true if the object is linked into the database, false if not.
     */
    public boolean isLinked() {
        return nodeInst.isLinked();
    }

    /**
     * Returns true if this PortInst is completely linked into database.
	 * This means there is path to this ElectricObjects through lists:
	 * Library&#46;libraries->Library&#46;cells->Cell&#46;nodes->NodeInst&#46;portInsts-> PortInst
     */
	public boolean isActuallyLinked() {
		int portIndex = getPortIndex();
		return nodeInst != null && nodeInst.isActuallyLinked() &&
			0 <= portIndex && portIndex < nodeInst.getNumPortInsts() && nodeInst.getPortInst(portIndex) == this;
	}

    public Poly computeTextPoly(EditWindow wnd, Variable var, Name name)
    {
        Poly poly = null;
        if (var != null)
        {
            Rectangle2D bounds = getPoly().getBounds2D();
            Poly [] polys = getPolyList(var, bounds.getCenterX(), bounds.getCenterY(), wnd, false);
            if (polys.length > 0)
            {
                poly = polys[0];
                poly.transform(getNodeInst().rotateOut());
            }
        }
        if (poly != null)
            poly.setExactTextBounds(wnd, this);
        return poly;
    }

}

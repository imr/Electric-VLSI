/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Connection.java
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

import com.sun.electric.database.variable.ElectricObject;

import java.awt.geom.Point2D;

/**
 * A Connection is the link between a PortInst on a NodeInst and an ArcInst.
 * A Connection has an location indicating the endpoint of the
 * ArcInst, a pointer to the PortInst on a NodeInst and a pointer to that ArcInst.
 * To find the arc(s) associated with a particular port on a node, ask
 * the node for a list of its connections.  The connections that point
 * to the portproto are also connected to the wires of interest.
 */
public class Connection extends ElectricObject
{
	// ------------------------- private data --------------------------------

	/** the arc on one side of this connection */	private ArcInst arc;
	/** the PortInst on the connected node */		private PortInst portInst;
	/** the location of this connection */			private Point2D location;

	/**
	 * The constructor creates a new Connection from the given values.
	 * @param arc the ArcInst that makes a Connection.
	 * @param portInst the PortInst on a NodeInst that makes a Connection.
	 * @param pt the coordinate on the NodeInst.
	 */
	protected Connection(ArcInst arc, PortInst portInst, Point2D pt)
	{
		this.arc = arc;
		this.portInst = portInst;
		this.location = (Point2D)pt.clone();
	}

	// --------------------------- public methods --------------------------

	/**
	 * Routine to return the ArcInst on this Connection.
	 * @return the ArcInst on this Connection.
	 */
	public ArcInst getArc() { return arc; }

	/**
	 * Routine to return the PortInst on this Connection.
	 * @return the PortInst on this Connection.
	 */
	public PortInst getPortInst() { return portInst; }

	/**
	 * Routine to return the location on this Connection.
	 * @return the location on this Connection.
	 */
	public Point2D getLocation() { return location; }

	/**
	 * Routine to set the location on this Connection.
	 * @param pt the location on this Connection.
	 */
	public void setLocation(Point2D pt) { location.setLocation(pt.getX(), pt.getY()); }

	/**
	 * Routine to determine whether this Connection is on the head end of the ArcInst.
	 * @return true if this Connection is on the head of the ArcInst.
	 */
	public boolean isHeadEnd()
	{
		return arc.getHead() == this;
	}
}

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

import com.sun.electric.database.geometry.EPoint;

/**
 * An abstract class Connection is the link between a PortInst on a NodeInst
 * and an ArcInst. Its subclasses are TailConnection and HeadConnection.
 * A Connection has an methods to get location indicating the endpoint of the
 * ArcInst, to get the PortInst on a NodeInst and to get that ArcInst.
 * It has also methods to get and modify propery bits on this end of the ArcInst.
 * To find the arc(s) associated with a particular port on a node, ask
 * the PortInst for a list of its connections.
 *
 * This class and its subclasses are immutable.
 */
public abstract class Connection
{
	// ------------------------- private data --------------------------------

	/** the arc on one side of this Connection */	/*package*/ final ArcInst arc;

	/**
	 * The constructor creates a new Connection of the given ArcInst.
	 * @param arc the ArcInst that makes a Connection.
	 */
	/*package*/ Connection(ArcInst arc)
	{
		this.arc = arc;
	}

	// --------------------------- public methods --------------------------

	/**
	 * Method to return the ArcInst on this Connection.
	 * @return the ArcInst on this Connection.
	 */
	public ArcInst getArc() { return arc; }

	/**
	 * Method to return the PortInst on this Connection.
	 * @return the PortInst on this Connection.
	 */
	public abstract PortInst getPortInst();

	/**
	 * Method to return the location on this Connection.
	 * @return the location on this Connection.
	 */
	public abstract EPoint getLocation();

	/**
	 * Method to tell whether this connection is arrowed.
	 * @return true if this connection is arrowed.
	 */
	public abstract boolean isArrowed();

	/**
	 * Method to set whether this connection is arrowed.
	 * @param state true to set that end of this arc to be arrowed.
	 */
	public abstract void setArrowed(boolean state);

	/**
	 * Method to tell whether this connection is extended.
	 * @return true if this connection is negated.
	 */
	public abstract boolean isExtended();

	/**
	 * Method to set whether this connection is extended.
	 * @param e true to set that end of this arc to be extended.
	 */
	public abstract void setExtended(boolean e);

	/**
	 * Method to tell whether this connection is negated.
	 * @return true if this connection is negated.
	 */
	public abstract boolean isNegated();

	/**
	 * Method to set whether this connection is negated.
	 * @param negated true if this connection is negated.
	 */
	public abstract void setNegated(boolean negated);

	/**
	 * Method to determine the index of this Connection on its ArcInst.
	 * @return HEADEND if this Connection is on the head; TAILEND if this Connection is on the head.
	 */
	public abstract int getEndIndex();
}

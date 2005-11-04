/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HeadConnection.java
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
 * A HeadConnection represents connection on the head end of the ArcInstance.
 */
public class HeadConnection extends Connection
{
	// ------------------------- private data --------------------------------

	/**
	 * The constructor creates a new HeadConnection of the given ArcInst.
	 * @param arc the ArcInst that makes a HeadConnection.
	 */
	HeadConnection(ArcInst arc)
	{
		super(arc);
	}

	// --------------------------- public methods --------------------------

	/**
	 * Method to return the PortInst on this HeadConnection.
	 * @return the PortInst on this HeadConnection.
	 */
	public PortInst getPortInst() { return arc.headPortInst; }

	/**
	 * Method to return the location on this HeadConnection.
	 * @return the location on this HeadConnection.
	 */
	public EPoint getLocation() { return arc.d.headLocation; }

	/**
	 * Method to tell whether this connection is arrowed.
	 * @return true if this connection is arrowed.
	 */
	public boolean isArrowed() { return arc.isHeadArrowed(); }

	/**
	 * Method to set whether this connection is arrowed.
	 * @param state true to set that end of this arc to be arrowed.
	 */
	public void setArrowed(boolean state) { arc.setHeadArrowed(state); }

	/**
	 * Method to tell whether this connection is extended.
	 * @return true if this connection is negated.
	 */
	public boolean isExtended() { return arc.isHeadExtended(); }

	/**
	 * Method to set whether this connection is extended.
	 * @param e true to set that end of this arc to be extended.
	 */
	public void setExtended(boolean e) { arc.setTailExtended(e); }

	/**
	 * Method to tell whether this connection is negated.
	 * @return true if this connection is negated.
	 */
	public boolean isNegated() { return arc.isHeadNegated(); }

	/**
	 * Method to set whether this connection is negated.
	 * @param negated true if this connection is negated.
	 */
	public void setNegated(boolean negated) { arc.setHeadNegated(negated); }

	/**
	 * Method to determine the index of this HeadConnection on its ArcInst.
	 * @return HEADEND
	 */
	public int getEndIndex()
	{
		return ArcInst.HEADEND;
	}

	/**
	 * Returns a printable version of this HeadConnection.
	 * @return a printable version of this HeadConnection.
	 */
	public String toString()
	{
		return "HeadConnection " + arc.describe(true);
	}
}

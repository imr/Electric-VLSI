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

import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;

import java.awt.geom.Point2D;

/**
 * A Connection is the link between a PortInst on a NodeInst and an ArcInst.
 * A Connection has an location indicating the endpoint of the
 * ArcInst, a pointer to the PortInst on a NodeInst and a pointer to that ArcInst.
 * To find the arc(s) associated with a particular port on a node, ask
 * the node for a list of its connections.  The connections that point
 * to the portproto are also connected to the wires of interest.
 *
 * This class is thread-safe.
 */
public class Connection
{
	// ------------------------- private data --------------------------------

	/** the arc on one side of this Connection */	private final ArcInst arc;
	/** the PortInst on the connected node */		private final PortInst portInst;
	/** the location of this Connection */			private final Point2D location;
	/** flags for this Connection */				private short flags;

	/** the shrinkage is from 0 to 90 */			private static final int SHRINKAGE = 0177;
	/** set if the end is negated */				private static final int NEGATED   = 0200;

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
		this.flags = 0;
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
	public PortInst getPortInst() { return portInst; }

	/**
	 * Method to return the location on this Connection.
	 * @return the location on this Connection.
	 */
	public synchronized Point2D getLocation() { return (Point2D)location.clone(); }

	/**
	 * Method to set the location on this Connection.
	 * @param pt the location on this Connection.
	 */
	synchronized void setLocation(Point2D pt)
	{
		arc.checkChanging();
		location.setLocation(pt.getX(), pt.getY());
	}

	/**
	 * Method to return the shrinkage happening because of angled arcs on this Connection.
	 * @return the shrinkage for this Connection.
	 */
	synchronized int getEndShrink() { return (int)(flags & SHRINKAGE); }

	/**
	 * Method to set the shrinkage happening because of angled arcs on this Connection.
	 * @param endShrink the shrinkage for this Connection.
	 */
	synchronized void setEndShrink(int endShrink)
	{
		arc.checkChanging();
		flags = (short)((flags & ~SHRINKAGE) | (endShrink & SHRINKAGE));
	}

	/**
	 * Method to tell whether this connection is arrowed.
	 * @return true if this connection is arrowed.
	 */
	public boolean isArrowed() { return arc.isArrowed(getEndIndex()); }

	/**
	 * Method to set whether this connection is arrowed.
	 * @param state true to set that end of this arc to be arrowed.
	 */
	public void setArrowed(boolean state) { arc.setArrowed(getEndIndex(), state); }

	/**
	 * Method to tell whether this connection is extended.
	 * @return true if this connection is negated.
	 */
	public boolean isExtended() { return arc.isExtended(getEndIndex()); }

	/**
	 * Method to set whether this connection is extended.
	 * @param e true to set that end of this arc to be extended.
	 */
	public void setExtended(boolean e) { arc.setExtended(getEndIndex(), e); }

	/**
	 * Method to tell whether this connection is negated.
	 * @return true if this connection is negated.
	 */
	public synchronized boolean isNegated() { return (flags & NEGATED) != 0; }

	/**
	 * Method to set whether this connection is negated.
	 * @param negated true if this connection is negated.
	 */
	public void setNegated(boolean negated)
	{
		if (negated)
		{
			// only allow if negation is not supported on this port
			if (portInst.getNodeInst().getProto() instanceof PrimitiveNode)
			{
				PrimitivePort pp = (PrimitivePort)portInst.getPortProto();
				if (pp.isNegatable())
				{					
					// Check to ensure at least one end of negated arc is attached to functional
					PortCharacteristic characteristic = portInst.getPortProto().getCharacteristic();
					if (characteristic != PortCharacteristic.IN &&
						characteristic != PortCharacteristic.OUT) return;

					// negate the connection
                    setNegatedSafe(true);
				}
			}
		} else
		{
            setNegatedSafe(false);
		}
	}

	private synchronized void setNegatedSafe(boolean negated)
    {
		arc.checkChanging();
        if (negated)
            flags |= NEGATED;
        else
            flags &= ~NEGATED;
    }

	/**
	 * Method to determine the index of this Connection on its ArcInst.
	 * @return HEADEND if this Connection is on the head; TAILEND if this Connection is on the head.
	 */
	public int getEndIndex()
	{
		return arc.getHead() == this ? ArcInst.HEADEND : ArcInst.TAILEND;
	}

	/**
	 * Returns a printable version of this Connection.
	 * @return a printable version of this Connection.
	 */
	public String toString()
	{
		return "Connection";
	}
}

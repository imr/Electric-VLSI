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

import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.geometry.Poly;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;

/** The PortInst class represents an instance of a Port.  It is the
 * combination of a NodeInst and a PortProto. There is no
 * corresponding object in Electric.  I'm adding it because I've found
 * that the PortInst objects simplify client programs considerably. */
public class PortInst
{
	// ------------------------ private data ------------------------

	private NodeInst nodeInst;
	private PortProto portProto;
	private int index;
	private JNetwork network;

	// -------------------protected or private methods ---------------

	private PortInst()
	{
	}

	// ------------------------ public methods -------------------------

	public static PortInst newInstance(PortProto portProto, NodeInst nodeInst)
	{
		PortInst pi = new PortInst();
		pi.portProto = portProto;
		pi.nodeInst = nodeInst;
		return pi;
	}

	public NodeInst getNodeInst() { return nodeInst; }

	public PortProto getPortProto() { return portProto; }

	public JNetwork getNetwork() { return network; }
	public void setNetwork(JNetwork net) { network = net; }

	public int getIndex() { return index; }
	public void setIndex(int i) { index = i; }

	public Rectangle2D getBounds()
	{
		Rectangle2D r = portProto.getPoly(nodeInst).getBounds2D();
		return r;
	}
	public Poly getPoly()
	{
		return portProto.getPoly(nodeInst);
	}

	/** Can this PortInst be connected to a particular type of arc? */
//	public boolean connectsTo(ArcProto arc)
//	{
//		return portProto.connectsTo(arc);
//	}

	/** Return a list of ArcInsts attached to this PortInst */
//	public Iterator getArcs()
//	{
//		ArrayList arcs = new ArrayList();
//		for (Iterator it = nodeInst.getConnections(); it.hasNext();)
//		{
//			Connection c = (Connection) it.next();
//			if (c.getPortInst() == this)
//				arcs.add(c.getArc());
//		}
//		return arcs.iterator();
//	}
}

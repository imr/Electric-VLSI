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

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.HashMap;

/** A Connection is the bond between a node and an arc.
 * 
 * <p> A Connection has an x,y location indicating the endpoint of the
 * arc, a pointer to the NodeInst and ArcInst, and a pointer to the
 * PortProto that represents the port this connection belongs to.  To
 * find the arc(s) associated with a particular port on a node, ask
 * the node for a list of its connections.  The connections that point
 * to the portproto are also connected to the wires of interest.
 *
 * <p> Most users will find JNetworks a more convenient way to
 * discover connectivity. */
public class Connection extends ElectricObject
{
	// ------------------------- private data --------------------------------
//	private static final JNetwork NO_SUBNET = new JNetwork(null);

	private ArcInst arc; // the arc on one side of this connection 
	private PortInst portInst;
	private Point2D.Double location; // the location of this connection

	protected Connection(ArcInst arc, PortInst portInst, double x, double y)
	{
		this.arc = arc;
		this.portInst = portInst;
		this.location = new Point2D.Double(x, y);
		portInst.getNodeInst().addConnection(this);
	}

	/** disconnect this connection from all database parts */
	void remove()
	{
		portInst.getNodeInst().removeConnection(this);
		arc.removeConnection(this, getArcEnd());
	}

	// --------------------------- public methods --------------------------
	/** the arc this connection connects to */
	public ArcInst getArc()
	{
		return arc;
	}

	/** Get the PortInst connected to by this Connection */
	public PortInst getPortInst()
	{
		return portInst;
	}

	/** The location of this connection */
	public Point2D.Double getLocation()
	{
		return location;
	}

	/** Which end of the arc this connection connects to.  Arcs have
	 * true ends and false ends. No more meaning can be read into the
	 * end. */
	public boolean getArcEnd()
	{
		return arc.getConnection(true) == this;
	}

//	Iterator getConnections()
//	{
//		return this.iterator();
//	}

	/** Get the location of this Connection. */
//	public Point2D.Double getLocation()
//	{
//		Point2D.Double rp = node.getParent().getReferencePoint();
//		Point el = getElectricLocation();
//		double x = el.x - rp.getX();
//		double y = el.y - rp.getY();
//		return new Point2D.Double(x, y);
//	}

	/** Set the location of this Connection. This must lie within the
	 * PortInst's boundary.
	 * TODO: not yet implemented */
	/*
	public void setLocation(Point2D loc) {
	  error(true, "not yet implemented");
	}
	*/

	// Return null if there is no subnet.
//	protected JNetwork getSubNet(HashMap equivPorts)
//	{
//		// first check to see if user has specified that this
//		// PortProto is part of some equivalence class.
//		JNetwork subnet = (JNetwork) equivPorts.get(port);
//
//		// No user defined equivalence.  Look for equivalence due to
//		// PrimitivePorts or icon's schematics.
//		if (subnet == null)
//		{
//			PortProto equivPort = port.getEquivalent();
//
//			// An Icon without a Schematic or a Schematic without a
//			// corresponding port
//			if (equivPort == null)
//				subnet = null;
//
//			subnet = equivPort.getNetwork();
//		}
//		return subnet;
//	}

	/** Trace out a network from a wire onto other connections on this
	 * port and other connections on other connected ports on this
	 * nodeinst. */
	/*
	void followNetwork(JNetwork net, HashMap equivPorts, HashMap visited) {
	  if (visited.get(this)!=null) return;	// already visited this conn
	  visited.put(this, this);
	  
	  JNetwork subnet = getSubNet(equivPorts);
	  // Ensure that we don't treat Connections with no subnets as if
	  // they're all connected.
	  if (subnet==null)  subnet=NO_SUBNET;
	  
	  // trace net onto other connections
	  Iterator it= this.node.getConnections();
	  while (it.hasNext()) {
	    Connection c= (Connection)it.next();
	    if (c.getSubNet(equivPorts)==subnet) {
		c.getArc().followNetwork(net, equivPorts, visited);
	    }
	  }
	  
	  // Trace net onto exports attached to this NodeInst.  This doesn't
	  // handle all Exports not connected to ArcInsts.  I have to fix
	  // that up later (what a kludge!).
	  Iterator eIt= this.node.getExports();
	  while (eIt.hasNext()) {
	    Export e= (Export) eIt.next();
	    if (e.getOriginal().getEquivalent().getNetwork()==subnet) {
		// Careful! If node has two internally connected ports
		// then the Export's subnet will match for both ports.
		// However, we only want to add the Export to the
		// Network once.
		if (e.getNetwork()==null) {
		  //e.setNetwork(net);
		  //net.addPart(e);
		}
	    }
	  }
	}
	*/
}

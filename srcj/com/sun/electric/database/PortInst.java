package com.sun.electric.database;

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
	private JNetwork network;

	// -------------------protected or private methods ---------------

	private PortInst(PortProto portProto, NodeInst nodeInst)
	{
		this.portProto = portProto;
		this.nodeInst = nodeInst;
	}

	// ------------------------ public methods -------------------------

	public static PortInst newInstance(PortProto portProto, NodeInst nodeInst)
	{
		PortInst pi = new PortInst(portProto, nodeInst);
		return pi;
	}

	public NodeInst getNodeInst() { return nodeInst; }

	public PortProto getPortProto() { return portProto; }

	public JNetwork getNetwork() { return network; }
	public void setNetwork(JNetwork net) { network = net; }

	public Rectangle2D getBounds()
	{
		Rectangle2D r = portProto.getPoly(nodeInst).getBounds2D();
		return r;
	}

//	public double getCenterX()
//	{
//		Poly p = portProto.getPoly(nodeInst);
//		return p.getCenterX();
//	}
//
//	public double getCenterY()
//	{
//		Poly p = portProto.getPoly(nodeInst);
//		return p.getCenterY();
//	}

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

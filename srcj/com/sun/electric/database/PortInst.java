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

	public NodeInst getNodeInst()
	{
		return nodeInst;
	}

	public PortProto getPortProto()
	{
		return portProto;
	}

	public JNetwork getNetwork()
	{
		return network;
	}
	public void setNetwork(JNetwork net)
	{
		network = net;
	}

	public Rectangle2D getBounds()
	{
		Rectangle r = portProto.getBounds(nodeInst).getBounds();
		// Adjust bounds relative to Cell's reference point.
		Point2D rp = nodeInst.getParent().getReferencePoint();
		return new Rectangle2D.Double(
			r.x - rp.getX(),
			r.y - rp.getY(),
			r.width,
			r.height);
	}

	public double getCenterX()
	{
		Poly p = portProto.getBounds(nodeInst);
//		Point2D rp = nodeInst.getParent().getReferencePoint();
		return p.getCenterX(); // - rp.getX();
	}

	public double getCenterY()
	{
		Poly p = portProto.getBounds(nodeInst);
//		Point2D rp = nodeInst.getParent().getReferencePoint();
		return p.getCenterY(); // - rp.getY();
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

	// Find the width of the widest wire connected hierarchically to a
	// particular port on a particular instance.  Return -1 if no wire
	// found.
//	private double widestWireWidth1(NodeInst ni, PortProto port)
//	{
//		double maxWid = -1;
//		for (Iterator cons = ni.getConnections(); cons.hasNext();)
//		{
//			Connection c = (Connection) cons.next();
//			if (c.getPort() == port)
//				maxWid = Math.max(maxWid, c.getArc().getWidth());
//		}
//		if (port instanceof Export)
//		{
//			double check =
//				widestWireWidth1(
//					((Export) port).getInstOwner(),
//					((Export) port).getOriginal());
//			maxWid = Math.max(maxWid, check);
//		}
//		return maxWid;
//	}

	/** Find the width of the widest wire connected hierarchically to a
	 * particular port on a particular instance.
	 *@return width of widest wire. return -1 if no wire found */
//	public double widestWireWidth()
//	{
//		return widestWireWidth1(nodeInst, portProto);
//	}
}

package com.sun.electric.database.prototype;

import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.network.Networkable;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.technology.PrimitivePort;

/**
 * A PortProto object lives at the PrimitiveNode level as a PrimitivePort,
 * or at the Cell level as an Export.  A PortProto has a descriptive name,
 * which may be overridden by another PortProto at a higher level in the
 * schematic or layout hierarchy.  PortProtos are expressed at the instance
 * level by Connections.
 */

public abstract class PortProto extends ElectricObject implements Networkable
{
	/**
	 * Function is a typesafe enum class that describes the function of a portproto.
	 */
	public static class Function
	{
		private final String name;

		private Function(String name) { this.name = name; }

		public String toString() { return name; }

		/**   unknown port */						public static final Function UNKNOWN = new Function("unknown");
		/**   un-phased clock port */				public static final Function CLK = new Function("clock");
		/**   clock phase 1 */						public static final Function C1 = new Function("clock1");
		/**   clock phase 2 */						public static final Function C2 = new Function("clock2");
		/**   clock phase 3 */						public static final Function C3 = new Function("clock3");
		/**   clock phase 4 */						public static final Function C4 = new Function("clock4");
		/**   clock phase 5 */						public static final Function C5 = new Function("clock5");
		/**   clock phase 6 */						public static final Function C6 = new Function("clock6");
		/**   input port */							public static final Function IN = new Function("input");
		/**   output port */						public static final Function OUT = new Function("output");
		/**   bidirectional port */					public static final Function BIDIR = new Function("bidirectional");
		/**   power port */							public static final Function PWR = new Function("power");
		/**   ground port */						public static final Function GND = new Function("ground");
		/**   bias-level reference output port */	public static final Function REFOUT = new Function("refout");
		/**   bias-level reference input port */	public static final Function REFIN = new Function("refin");
		/**   bias-level reference base port */		public static final Function REFBASE = new Function("refbase");
	}

	// ------------------------ private data --------------------------

	/** port name */								protected String protoName;
	/** flag bits */								protected int userBits;
	/** parent NodeProto */							protected NodeProto parent;

	/** Network that this port belongs to (in case two ports are permanently
	 * connected, like the two ends of the gate in a MOS transistor.
	 * The Network node pointed to here does not have any connections itself;
	 * it just serves as a common marker. */
	private JNetwork network;

	// ----------------------- public constants -----------------------
	/** angle of this port from node center */			public static final int PORTANGLE=               0777;
	/** right shift of PORTANGLE field */				public static final int PORTANGLESH=                0;
	/** range of valid angles about port angle */		public static final int PORTARANGE=           0377000;
	/** right shift of PORTARANGE field */				public static final int PORTARANGESH=               9;
	/** electrical net of primitive port (0-30) */		public static final int PORTNET=           0177400000;
           /* 31 stands for one-port net */
	/** right shift of PORTNET field */					public static final int PORTNETSH=                 17;
	/** set if arcs to this port do not connect */		public static final int PORTISOLATED=      0200000000;
	/** set if this port should always be drawn */		public static final int PORTDRAWN=         0400000000;
	/** set to exclude this port from the icon */		public static final int BODYONLY=         01000000000;

	// ------------------ protected and private methods ---------------------

	protected PortProto()
	{
		this.parent = null;
		this.network = null;
		this.userBits = 0;
		//if (network!=null)  network.addPart(this);
	}

	/** Get the bounds for this port with respect to some NodeInst.  A
	 * port can scale with its owner, so it doesn't make sense to ask
	 * for the bounds of a port without a NodeInst.  The NodeInst should
	 * be an instance of the NodeProto that this port belongs to,
	 * although this isn't checked. */
	abstract public Poly getPoly(NodeInst ni);

	// Set the network associated with this Export.
	void setNetwork(JNetwork net)
	{
		this.network = net;
	}

	/** Get the NodeProto (Cell or PrimitiveNode) that owns this port. */
	public void setParent(NodeProto parent)
	{
		this.parent = parent;
		parent.addPort(this);
	}

	/** Remove this portproto.  Also removes this port from the parent
	 * NodeProto's ports list. */
	public void remove()
	{
		parent.removePort(this);
	}

	protected void getInfo()
	{
		System.out.println(" Parent: " + parent);
		System.out.println(" Name: " + protoName);
		System.out.println(" Network: " + network);
		super.getInfo();
	}

	// ---------------------- public methods -------------------------

	public String getProtoName() { return protoName; }

	/** Get the NodeProto (Cell or PrimitiveNode) that owns this port. */
	public NodeProto getParent() { return parent; }

	/** Get the network associated with this port. */
	public JNetwork getNetwork() { return network; }

	/** Get the first "real" (non-zero width) ArcProto style that can
	 * connect to this port. */
	public ArcProto getWire()
	{
		return getBasePort().getWire();
	}

	public abstract PrimitivePort getBasePort();

	/** Can this port connect to a particular arc type?
	 * @param arc the arc type to test for
	 * @return true if this port can connect to the arc, false if it can't */
	public boolean connectsTo(ArcProto arc)
	{
		return getBasePort().connectsTo(arc);
	}

	public abstract PortProto getEquivalent();

	public String toString()
	{
		return "PortProto " + protoName;
	}
}

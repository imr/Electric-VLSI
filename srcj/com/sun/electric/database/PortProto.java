package com.sun.electric.database;

//import java.awt.Rectangle;

/**
 * A PortProto object lives at the PrimitiveNode level as a PrimitivePort,
 * or at the Cell level as an Export.  A PortProto has a descriptive name,
 * which may be overridden by another PortProto at a higher level in the
 * schematic or layout hierarchy.  PortProtos are expressed at the instance
 * level by Connections.
 */

public abstract class PortProto extends ElectricObject implements Networkable
{
	// ------------------------ private data --------------------------
	private String name;
	private int userbits;

	/** Network that this port belongs to (in case two ports are permanently
	 * connected, like the two ends of the gate in a MOS transistor.
	 * The Network node pointed to here does not have any connections itself;
	 * it just serves as a common marker. */
	private JNetwork network;

	/** NodeProto that this port belongs to.  Will be a PrimitiveNode for
	 * a PrimitivePort, or a Cell for an Export */
	private NodeProto parent;

	/** role position in usrbits */
	private static final int ROLESHIFT = 28;

	/** role bit mask before shifting */
	private static final int ROLEMASK = 036000000000;

	/** role bit mask after shifting */
	private static final int ROLEBASEMASK = 15;

	private static final String roleNames[] =
		{
			"UNK",
			"CLK",
			"CLK1",
			"CLK2",
			"CLK3",
			"CLK4",
			"CLK5",
			"CLK6",
			"IN",
			"OUT",
			"BIDIR",
			"PWR",
			"GND",
			"REFOUT",
			"REFIN" };

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
	/** input/output/power/ground/clock state: */		public static final int STATEBITS=       036000000000;
	/** right shift of STATEBITS */						public static final int STATEBITSSH=               27;
	/**   un-phased clock port */						public static final int CLKPORT=          02000000000;
	/**   clock phase 1 */								public static final int C1PORT=           04000000000;
	/**   clock phase 2 */								public static final int C2PORT=           06000000000;
	/**   clock phase 3 */								public static final int C3PORT=          010000000000;
	/**   clock phase 4 */								public static final int C4PORT=          012000000000;
	/**   clock phase 5 */								public static final int C5PORT=          014000000000;
	/**   clock phase 6 */								public static final int C6PORT=          016000000000;
	/**   input port */									public static final int INPORT=          020000000000;
	/**   output port */								public static final int OUTPORT=         022000000000;
	/**   bidirectional port */							public static final int BIDIRPORT=       024000000000;
	/**   power port */									public static final int PWRPORT=         026000000000;
	/**   ground port */								public static final int GNDPORT=         030000000000;
	/**   bias-level reference output port */			public static final int REFOUTPORT=      032000000000;
	/**   bias-level reference input port */			public static final int REFINPORT=       034000000000;
	/**   bias-level reference base port */				public static final int REFBASEPORT=     036000000000;

	// ------------------ protected and private methods ---------------------

	protected PortProto()
	{
//		super(cptr);
	}

	/** Initialize this PortProto with the parent NodeProto
	 * (PrimitiveNode for PrimitivePorts, Cell for Exports) we're a
	 * port of. */
	protected void init(
		NodeProto parent,
		JNetwork network,
		String name,
		int userbits)
	{
		boolean first = this.parent == null;
		this.parent = parent;
		this.network = network;
		this.name = name;
		this.userbits = userbits;
		if (first)
		{
			parent.addPort(this);
			//if (network!=null)  network.addPart(this);
		}
	}

	/** Get the bounds for this port with respect to some NodeInst.  A
	 * port can scale with its owner, so it doesn't make sense to ask
	 * for the bounds of a port without a NodeInst.  The NodeInst should
	 * be an instance of the NodeProto that this port belongs to,
	 * although this isn't checked. */
	abstract Poly getBounds(NodeInst ni);

	protected boolean putPrivateVar(String var, Object value)
	{
		if (var.equals("userbits"))
		{
			userbits = ((Integer) value).intValue();
			return true;
		}
		return false;
	}

	// Set the network associated with this Export.
	void setNetwork(JNetwork net)
	{
		this.network = net;
	}

	/** Remove this portproto.  Also removes this port from the parent
	 * NodeProto's ports list. */
	void remove()
	{
		parent.removePort(this);
	}

	protected void getInfo()
	{
		System.out.println(" Parent: " + parent);
		System.out.println(" Name: " + name);
		/*kaoTest System.out.println(" Shape: "+proxy);*/
		System.out.println(" Network: " + network);
		System.out.println(" Role: " + getRoleName());
		super.getInfo();
	}

	// ---------------------- public methods -------------------------
	/** Get the function of this PortProto (e.g. CLK, GND, PWR, IN, OUT
	 * etc) */
	public int getRole()
	{
		return userbits & ROLEMASK;
	}

	/** Set the function of this PortProto (e.g. CLK, GND, PWR, IN, OUT
	 * etc) */
	public void setRole(int role)
	{
		int newUserBits = (userbits & ~ROLEMASK) | (role & ROLEMASK);
		setVar("userbits", newUserBits);
	}

	/** Get a string representing the role of this PortProto */
	public String getRoleName()
	{
		int idx = (userbits >> ROLESHIFT) & ROLEBASEMASK;
		if (idx > 0 && idx < roleNames.length)
		{
			return roleNames[idx];
		} else
		{
			return "bad role value";
		}
	}

	/** Get the network associated with this port. */
	public JNetwork getNetwork()
	{
		return network;
	}

	/** Can this port connect to a particular arc type?
	 * @param arc the arc type to test for
	 * @return true if this port can connect to the arc, false if it can't */
	public boolean connectsTo(ArcProto arc)
	{
		return getBasePort().connectsTo(arc);
	}

	/** Get the first "real" (non-zero width) ArcProto style that can
	 * connect to this port. */
	public ArcProto getWire()
	{
		return getBasePort().getWire();
	}

	/** Get the NodeProto (Cell or PrimitiveNode) that owns this port. */
	public NodeProto getParent()
	{
		return parent;
	}

	public String getName()
	{
		return name;
	}

	public abstract PrimitivePort getBasePort();

	/** If this PortProto belongs to an Icon View Cell then return the
	 * PortProto with the same name on the corresponding Schematic View
	 * Cell.
	 *
	 * <p> If this PortProto doesn't belong to an Icon View Cell then
	 * return this PortProto.
	 *
	 * <p> If the Icon View Cell has no corresponding Schematic View
	 * Cell then return null. If the corresponding Schematic View Cell
	 * has no port with the same name then return null.
	 *
	 * <p> If there are multiple versions of the Schematic Cell return
	 * the latest. */
	public PortProto getEquivalent()
	{
		NodeProto equiv = parent.getEquivalent();
		if (equiv == parent)
			return this;
		if (equiv == null)
			return null;
		return equiv.findPort(name);
	}

	public String toString()
	{
		return "PortProto " + name;
	}

	/** sanity check.  Make sure parent nodeproto knows about us */
	public void checkobj(SanityAnswer ans)
	{
		if (parent == null)
		{
			ans.oops("Parent of " + this +" not set");
		} else if (!parent.containsPort(this))
		{
			ans.oops(
				"Parent " + parent + " of " + this +" doesn't know about it");
		}
		// TODO: check network to make sure it's the same as any wires on
		// this port.
		if (network == null)
		{
			ans.oops("No network found on " + this);
		}
	}
}

package com.sun.electric.database;

/**
 * An Export is a PortProto at the Cell level.  It points to the
 * PortProto that got exported, and the NodeInst for that port.
 * It also allows instant access to the PrimitivePort that grounds
 * out the PortProto chain.
 */
public class Export extends PortProto
{
	// -------------------------- private data ---------------------------
	private PortProto originalPort; // The PortProto that got exported to create this Export
	private NodeInst owner; // the node that the exported port belongs to

	// -------------------- protected and private methods --------------
	protected Export()
	{
	}

	/** Initialize this Export with a parent (the Cell we're a port of),
	 * the original PortProto we're based on, the NodeInst that
	 * original port belongs to, and an appropriate Network. */
	protected void init(
		Cell parent,
		PortProto basis,
		NodeInst owner,
		String name,
		int userbits)
	{
//		super.init(parent, null, name, userbits);
		boolean first = this.originalPort == null;
		this.originalPort = basis;
		this.owner = owner;
		if (first)
		{
			owner.addExport(this);
		}
	}

	protected void remove()
	{
		owner.removeExport(this);
		super.remove();
	}

	/** Get the PortProto that was exported to create this Export. */
	PortProto getOriginal()
	{
		return originalPort;
	}

	/** Get the NodeInst that the port returned by getOriginal belongs to */
	NodeInst getInstOwner()
	{
		return owner;
	}

	/** Get the outline of this Export, relative to some arbitrary
	 * instance of this Export's parent, passed in as a Geometric
	 * object.
	 * @param ni the instance of the port's parent
	 * @return the shape of the port, transformed into the coordinates
	 * of the instance's Cell. */
	Poly getPoly(NodeInst ni)
	{
		// We just figure out where our basis thinks it is, and ask ni to
		// transform it for us.
//		return ni.xformPoly(originalPort.getPoly(owner));
		return null;
	}

	protected void getInfo()
	{
		System.out.println(" Original: " + originalPort);
		System.out.println(" Base: " + getBasePort());
		System.out.println(" Cell: " + owner.getParent());
		System.out.println(" Instance: " + owner);
		super.getInfo();
	}

	// ----------------------- public methods ----------------------------

	/** Get the base PrimitivePort that generated this Export */
	public PrimitivePort getBasePort()
	{
		return originalPort.getBasePort();
	}

	/** Get the PortInst exported by this Export */
	public PortInst getPortInst()
	{
		return owner.findPort(originalPort.getName());
	}

	public JNetwork getNetwork()
	{
		return getPortInst().getNetwork();
	}

	public String toString()
	{
		return "Export " + protoName;
	}

	// Return the first internal connection that exists on the port this
	// Export was exported from, or null if no such Connection exists.
//	Connection findFirstConnection() {
//	  Iterator i= owner.getConnections();
//	  while (i.hasNext()) {
//	    Connection c= (Connection)i.next();
//	    if (c.getPort()== originalPort)  return c;
//	  }
//	  return null;
//	}
}

package com.sun.electric.database;

//import java.awt.geom.AffineTransform;
import java.awt.Point;
import java.awt.geom.Point2D;

//import java.util.*;

/**
 * An ArcInst is an instance of an ArcProto (a wire type)
 * An ArcInst points to its prototype, the Cell on which it has been
 * instantiated, and the connection at either end of the wire.
 * The geometry of the wire (width and length) is captured in the
 * bounds of the Geometric portion of this object.
 */
public class ArcInst extends Geometric /*implements Networkable*/
{
	/** fixed-length arc */								public static final int FIXED=                     01;
	/** fixed-angle arc */								public static final int FIXANG=                    02;
	/** arc has text that is far away */				public static final int AHASFARTEXT=               04;
	/** arc is not in use */							public static final int DEADA=                    020;
	/** angle of arc from end 0 to end 1 */				public static final int AANGLE=                037740;
	/** bits of right shift for AANGLE field */			public static final int AANGLESH=                   5;
	/** set if arc is to be drawn shortened */			public static final int ASHORT=                040000;
	/** set if ends do not extend by half width */		public static final int NOEXTEND=             0400000;
	/** set if ends are negated */						public static final int ISNEGATED=           01000000;
	/** set if arc aims from end 0 to end 1 */			public static final int ISDIRECTIONAL=       02000000;
	/** no extension/negation/arrows on end 0 */		public static final int NOTEND0=             04000000;
	/** no extension/negation/arrows on end 1 */		public static final int NOTEND1=            010000000;
	/** reverse extension/negation/arrow ends */		public static final int REVERSEEND=         020000000;
	/** set if arc can't slide around in ports */		public static final int CANTSLIDE=          040000000;

	// -------------------------- private data ----------------------------------
	private static final String VAR_ARC_NAME = "ARC_name";
	// Name of the variable
	// holding the ArcInst's
	// name.
	private int userbits; // Electric ArcInst's userbits
	private ArcProto protoType;
	private Connection ends[];
	// connections on either side
	//private JNetwork net;    			// The network on which this Arc
	// resides.

	// -------------------- private and protected methods ------------------------
	public ArcInst(ArcProto protoType, double width, int userbits,
		PortInst a, double aX, double aY,
		PortInst b, double bX, double bY,
		Cell parent)
	{
		this.protoType = protoType;
		this.userbits = userbits;
		Connection c1 = new Connection(this, a, aX, aY);
		Connection c2 = new Connection(this, b, bX, bY);
		ends = new Connection[] {c1, c2};
	}

	/** recalculate the transform on this arc to get the endpoints
	 * where they should be. */
	private void updateEnds(int width)
	{
		voidBounds();
		Point2D.Double p1 = ends[0].getLocation();
		Point2D.Double p2 = ends[1].getLocation();
		double dx = p2.x - p1.x;
		double dy = p2.y - p1.y;
		double len = Math.sqrt(dx * dx + dy * dy);
		// affine transform takes (0,0) to p1, (0,1) to p2, and
		// (1,0) to a distance of width
		this.sX = width;
		this.sY = (int) len;
		this.dX = (p1.x + p2.x) / 2;
		this.dY = (p1.y + p2.y) / 2;
		this.cos = dy / len;
		this.sin = -dx / len;
	}

	/** set the connection on a particular end */
	void setConnection(Connection c, boolean end)
	{
		ends[end ? 1 : 0] = c;
	}

	//void setNetwork(JNetwork net) {this.net=net;}
	//void clearNetwork() {this.net= null;}	// clear the network pointer

	/** recursively visit this arc in order to build JNetworks */
	/*
	void followNetwork(JNetwork net, HashMap equivPorts, HashMap visited) {
	  if (this.net!=null) return;	// already visited this arc
	  this.net = net;
	  //this.net.addPart(this);
	  
	  // now visit both ends
	  ends[0].followNetwork(net, equivPorts, visited);
	  ends[1].followNetwork(net, equivPorts, visited);
	}
	*/

	protected boolean putPrivateVar(String var, Object value)
	{
		if (var.equals("userbits"))
		{
			userbits = ((Integer) value).intValue();
			return true;
		}
		return false;
	}

	// Remove this ArcInst.  Will also remove the connections on either
	// side.
	void remove()
	{
		ends[0].remove();
		ends[1].remove();
		getParent().removeArc(this);
		super.remove();
	}

	/** remove the connection from a particular end */
	void removeConnection(Connection c, boolean end)
	{
		/* safety check */
		if (ends[end ? 1 : 0] != c)
		{
			error(
				"Tried to remove the wrong connection from a wire end: "
					+ c
					+ " on "
					+ this);
		}
		ends[end ? 1 : 0] = null;
	}

	protected void getInfo()
	{
		System.out.println(" Proto: " + protoType);
		System.out.println(" EndT: " + ends[0].getPortInst());
		System.out.println("       on " + ends[0].getPortInst().getNodeInst());
		Point2D loc = ends[0].getLocation();
		System.out.println("       at " + loc.getX() + ", " + loc.getY());
		System.out.println(" EndF: " + ends[1].getPortInst());
		System.out.println("       on " + ends[1].getPortInst().getNodeInst());
		loc = ends[1].getLocation();
		System.out.println("       at " + loc.getX() + ", " + loc.getY());
		super.getInfo();
	}

	// -------------------------- public methods -----------------------------

	/** Get the width of this ArcInst in Lambda units.
	 *
	 * <p> Note that this call excludes material surrounding this
	 * ArcInst. For example, if this is a diffusion ArcInst then return
	 * the width of the diffusion and ignore the width of well and
	 * select. */
	public double getWidth()
	{
		return round(sX) - protoType.getWidthOffset();
	}

	/** Get the Ends-Extend flag. If the Ends-Extend flag is true then
	 * this ArcInst extends by 1/2 of its width past its two end points.*/
	public boolean getEndsExtend()
	{
		return (userbits & NOEXTEND) == 0;
	}

	/** Set the Ends-Extend flag. If the Ends-Extend flag is true then
	 * this ArcInst extends by 1/2 of its width past its two end
	 * points.*/
	public void setEndsExtend(boolean e)
	{
		if (e)
		{
			setVar("userbits", userbits & ~NOEXTEND);
		} else
		{
			setVar("userbits", userbits | NOEXTEND);
		}
	}

	/** Change the width of this ArcInst.
	 * @param width the new width.  width must be >=0. A width of zero
	 * means "use the default width". Lambda units. */
	public void alterShape(double width)
	{
		error(width < 0, "negative ArcInst widths not allowed");
		if (width == 0)
			width = protoType.getWidth();

		width += protoType.getWidthOffset();

//		Electric.modifyArcInst(getAddr(), lambdaToBase(width));
	}

	/** Get the connection to the specified end of this ArcInst. */
	public Connection getConnection(boolean end)
	{
		return ends[end ? 1 : 0];
	}

	/** Get the ArcProto of this ArcInst. */
	public ArcProto getProto()
	{
		return protoType;
	}
	/** Get the network to which this ArcInst belongs.  An ArcInst
	 * becomes part of a JNetwork only after you call
	 * Cell.rebuildNetworks(). */
	//public JNetwork getNetwork() {return net;}
	public void setName(String name)
	{
		setVar(VAR_ARC_NAME, name);
	}
	public String getName()
	{
		return (String) getVar(VAR_ARC_NAME);
	}

	/** Printable version of this ArcInst */
	public String toString()
	{
		return "ArcInst " + protoType.getProtoName();
	}
}

package com.sun.electric.database;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The NodeProto class encapsulates the Electric NodeProto data structure.
 * NodeProtos don't get usede.  The subclasses
 * of NodeProto, Cell and PrimitiveNode, are used instead.
 * The NodeProto portion records the ports associated with this node,
 * the bounds of the node prototype, a list of instances of this node
 * prototype in use in all open libraries, and a list of equivalent nodes
 * for transforming between schematics and icons, or between equivalently
 * structured layouts.
 */
public abstract class NodeProto extends ElectricObject
{
	/** node is unknown type */								public static final int NPUNKNOWN=             0;
	/** node is a single-layer pin */						public static final int NPPIN=                 1;
	/** node is a two-layer contact (one point) */			public static final int NPCONTACT=             2;
	/** node is a single-layer node */						public static final int NPNODE=                3;
	/** node connects all ports */							public static final int NPCONNECT=             4;
	/** node is MOS enhancement transistor */				public static final int NPTRANMOS=             5;
	/** node is MOS depletion transistor */					public static final int NPTRADMOS=             6;
	/** node is MOS complementary transistor */				public static final int NPTRAPMOS=             7;
	/** node is NPN junction transistor */					public static final int NPTRANPN=              8;
	/** node is PNP junction transistor */					public static final int NPTRAPNP=              9;
	/** node is N-channel junction transistor */			public static final int NPTRANJFET=           10;
	/** node is P-channel junction transistor */			public static final int NPTRAPJFET=           11;
	/** node is MESFET depletion transistor */				public static final int NPTRADMES=            12;
	/** node is MESFET enhancement transistor */			public static final int NPTRAEMES=            13;
	/** node is prototype-defined transistor */				public static final int NPTRANSREF=           14;
	/** node is undetermined transistor */					public static final int NPTRANS=              15;
	/** node is 4-port MOS enhancement transistor */		public static final int NPTRA4NMOS=           16;
	/** node is 4-port MOS depletion transistor */			public static final int NPTRA4DMOS=           17;
	/** node is 4-port MOS complementary transistor */		public static final int NPTRA4PMOS=           18;
	/** node is 4-port NPN junction transistor */			public static final int NPTRA4NPN=            19;
	/** node is 4-port PNP junction transistor */			public static final int NPTRA4PNP=            20;
	/** node is 4-port N-channel junction transistor */		public static final int NPTRA4NJFET=          21;
	/** node is 4-port P-channel junction transistor */		public static final int NPTRA4PJFET=          22;
	/** node is 4-port MESFET depletion transistor */		public static final int NPTRA4DMES=           23;
	/** node is 4-port MESFET enhancement transistor */		public static final int NPTRA4EMES=           24;
	/** node is E2L transistor */							public static final int NPTRANS4=             25;
	/** node is resistor */									public static final int NPRESIST=             26;
	/** node is capacitor */								public static final int NPCAPAC=              27;
	/** node is electrolytic capacitor */					public static final int NPECAPAC=             28;
	/** node is diode */									public static final int NPDIODE=              29;
	/** node is zener diode */								public static final int NPDIODEZ=             30;
	/** node is inductor */									public static final int NPINDUCT=             31;
	/** node is meter */									public static final int NPMETER=              32;
	/** node is transistor base */							public static final int NPBASE=               33;
	/** node is transistor emitter */						public static final int NPEMIT=               34;
	/** node is transistor collector */						public static final int NPCOLLECT=            35;
	/** node is buffer */									public static final int NPBUFFER=             36;
	/** node is AND gate */									public static final int NPGATEAND=            37;
	/** node is OR gate */									public static final int NPGATEOR=             38;
	/** node is XOR gate */									public static final int NPGATEXOR=            39;
	/** node is flip-flop */								public static final int NPFLIPFLOP=           40;
	/** node is multiplexor */								public static final int NPMUX=                41;
	/** node is connected to power */						public static final int NPCONPOWER=           42;
	/** node is connected to ground */						public static final int NPCONGROUND=          43;
	/** node is source */									public static final int NPSOURCE=             44;
	/** node is connected to substrate */					public static final int NPSUBSTRATE=          45;
	/** node is connected to well */						public static final int NPWELL=               46;
	/** node is pure artwork */								public static final int NPART=                47;
	/** node is an array */									public static final int NPARRAY=              48;
	/** node is an alignment object */						public static final int NPALIGN=              49;
	/** node is a current-controlled voltage source */		public static final int NPCCVS=               50;
	/** node is a current-controlled current source */		public static final int NPCCCS=               51;
	/** node is a voltage-controlled voltage source */		public static final int NPVCVS=               52;
	/** node is a voltage-controlled current source */		public static final int NPVCCS=               53;
	/** node is a transmission line */						public static final int NPTLINE=              54;

	/** set if nonmanhattan instances shrink */				public static final int NODESHRINK=           01;
	/** set if instances should be expanded */				public static final int WANTNEXPAND=          02;
	/** node function (from efunction.h) */					public static final int NFUNCTION=          0774;
	/** right shift for NFUNCTION */						public static final int NFUNCTIONSH=           2;
	/** set if instances can be wiped */					public static final int ARCSWIPE=          01000;
	/** set if node is to be kept square in size */			public static final int NSQUARE=           02000;
	/** primitive can hold trace information */				public static final int HOLDSTRACE=        04000;
	/** set to reevaluate this cell's network */			public static final int REDOCELLNET=      010000;
	/** set to erase if connected to 1 or 2 arcs */			public static final int WIPEON1OR2=       020000;
	/** set if primitive is lockable (cannot move) */		public static final int LOCKEDPRIM=       040000;
	/** set if primitive is selectable by edge, not area */	public static final int NEDGESELECT=     0100000;
	/** set if nonmanhattan arcs on this shrink */			public static final int ARCSHRINK=       0200000;
	//  used by database:                                                                           01400000
	/** set if not used (don't put in menu) */				public static final int NNOTUSED=       02000000;
	/** set if everything in cell is locked */				public static final int NPLOCKED=       04000000;
	/** set if instances in cell are locked */				public static final int NPILOCKED=     010000000;
	/** set if cell is part of a "cell library" */			public static final int INCELLLIBRARY= 020000000;
	/** set if cell is from a technology-library */			public static final int TECEDITCELL=   040000000;

	// ---------------------- inner classes ------------------------------
	static class ElectricPosition
	{
		int lx, ly, hx, hy, angle, transpose;
		public String toString()
		{
			return "ElectricPosition {\n"
				+ "    x: [" + lx + " : " + hx + "]\n"
				+ "    y: [" + ly + " : " + hy + "]\n"
				+ "    angle: " + angle + "\n"
				+ "    transpose: " + transpose + "\n}\n";
		}
	}

	// ------------------------ private data --------------------------

	/** the name of the NodeProto */				protected String protoName;
	/** the exports in the NodeProto */				private ArrayList ports;
	/** the bounds of the NodeProto */				protected Rectangle2D.Double elecBounds;
	/** JNetworks that comprise this NodeProto */	private ArrayList networks;
	/** All instances of this NodeProto */			private ArrayList instances;

	// ----------------- protected and private methods -----------------

	protected NodeProto()
	{
		ports = new ArrayList();
		instances = new ArrayList();
		networks = new ArrayList();
		elecBounds = new Rectangle2D.Double(0, 0, 0, 0);
	}

	// Get the Electric bounds. This includes invisible widths. Base units.
	Rectangle2D.Double getElecBounds()
	{
		return elecBounds;
	}

	/**
	 * Add a port prototype (Export for Cells, PrimitivePort for
	 * PrimitiveNodes) to this NodeProto.
	 */
	void addPort(PortProto port)
	{
		ports.add(port);
	}

	/** remove a port prototype from this node prototype */
	void removePort(PortProto port)
	{
		ports.remove(port);
	}

	/** Add a Network to this Cell */
	void addNetwork(JNetwork n)
	{
		if (networks.contains(n))
		{
			error("Cell " + this +" already contains network " + n);
		}
		networks.add(n);
	}

	/** Remove a Network from this Cell */
	void removeNetwork(JNetwork n)
	{
		if (!networks.contains(n))
		{
			error("Cell " + this +" doesn't contain network " + n);
		}
		networks.remove(n);
	}

	void removeAllNetworks()
	{
		networks.clear();
	}

	/** Add an instance of this nodeproto to its instances list */
	void addInstance(NodeInst inst)
	{
		if (instances == null)
		{
			System.out.println("Hmm.  Instances is *still* null!");
		}
		instances.add(inst);
	}

	/** Remove an instance of this nodeproto from its instances list */
	void removeInstance(NodeInst inst)
	{
		instances.remove(inst);
	}

	/** Remove this NodeProto.  Also offs the ports associated with this
	 * nodeproto. */
	void remove()
	{
		// kill ports
		removeAll(ports);
		// unhook from networks
		while (networks.size() > 0)
		{
			removeNetwork((JNetwork) networks.get(networks.size() - 1));
		}
	}

	/** Does the ports list contain a particular port?
	 * Used by PortProto's sanity check method */
	boolean containsPort(PortProto port)
	{
		return ports.contains(port);
	}

	// ----------------------- public methods -----------------------

	/** A NodeProto's <i>reference point</i> is (0, 0) unless the
	 * NodeProto is a Cell containing an instance of a Cell-Center in
	 * which case the reference point is the location of that
	 * Cell-Center instance.  Base units. */
	abstract Point2D.Double getRefPointBase();

	public abstract SizeOffset getSizeOffset();

	public abstract Technology getTechnology();

	/** A NodeProto's <i>reference point</i> is (0, 0) unless the
	 * NodeProto is a Cell containing an instance of a Cell-Center in
	 * which case the reference point is the location of that
	 * Cell-Center instance.  Lambda units. */
	public abstract Point2D.Double getReferencePoint();

	/** Can this node connect to a particular arc?
	 * @param arc the type of arc to test for
	 * @return the first port that can connect to the arc, or null,
	 * if this node cannot connect to the given arc */
	public PortProto connectsTo(ArcProto arc)
	{
		for (int i = 0; i < ports.size(); i++)
		{
			PortProto pp = (PortProto) ports.get(i);
			if (pp.connectsTo(arc))
				return pp;
		}
		return null;
	}

	/** If this is an Icon View Cell then return the the corresponding
	 * Schematic View Cell.
	 *
	 * <p> If this isn't an Icon View Cell then return this NodeProto.
	 *
	 * <p> If an Icon View Cell has no Schematic View Cell then return
	 * null.
	 *
	 * <p> If there are multiple versions of the Schematic View then
	 * return the latest version. */
	public abstract NodeProto getEquivalent();

	/** Get the PortProto that has a particular name.
	 * @return the PortProto, or null if there is no PortProto with that
	 * name. */
	public PortProto findPort(String name)
	{
		for (int i = 0; i < ports.size(); i++)
		{
			PortProto pp = (PortProto) ports.get(i);
			if (pp.getName().equals(name))
				return pp;
		}
		return null;
	}

	/**
	 * Get an iterator over all PortProtos of this NodeProto
	 */
	public Iterator getPorts()
	{
		return ports.iterator();
	}

	/**
	 * Get an iterator over all of the NodeInsts in all open Libraries
	 * that instantiate this NodeProto.
	 */
	public Iterator getInstances()
	{
		return instances.iterator();
	}

	public abstract String describe();

	/**
	 * Get the name of this NodeProto.
	 */
	public String getProtoName()
	{
		return protoName;
	}

	/** Get an iterator over all of the JNetworks of this NodeProto.
	 * 
	 * <p> Warning: before getNetworks() is called, JNetworks must be
	 * build by calling Cell.rebuildNetworks() */
	public Iterator getNetworks()
	{
		return networks.iterator();
	}

	// From Jose's position variables generate the equivalent Electric
	// position variables.
	//
	// This NodeProto is the child. parent is the Cell that will
	// contain an instance of this NodeProto.
	//
	// Jose positions objects by:
	// 1) scaling by SX and SY
	// 2) rotating counter-clockwise by angleJ
	// 3) translating by DX and DY.  DX and DY are Cell-Center relative.
	// 
	// See SML# 2003-0379 for description of Jose position mathematics.
	// Base units.
//	ElectricPosition joseToElecPosition(double SX, double SY, int DX, int DY,
//		double angleJ, Cell parent)
//	{
//		ElectricPosition ep = new ElectricPosition();
//
//		error(
//			(this instanceof Cell)
//				&& ((SX != 1 && SX != -1) || (SY != 1 && SY != -1)),
//			"Cells must be scaled by 1 or -1.");
//
//		Point g = parent.getRefPointBase(); // parent reference point
//		Point f = getRefPointBase(); // child reference point
//
//		double sin = Math.sin(angleJ * Math.PI / 180);
//		double cos = Math.cos(angleJ * Math.PI / 180);
//
//		// dX and dY are absolute.
//		// DX and DY are from the Memo's equation 35.
//		double dX = -f.x * SX * cos + f.y * SY * sin + DX + g.x;
//		double dY = -f.x * SX * sin - f.y * SY * cos + DY + g.y;
//
//		double signX = SX >= 0 ? 1 : -1;
//		double signY = SY >= 0 ? 1 : -1;
//
//		// Electric's transpose
//		ep.transpose = signX != signY ? 1 : 0;
//
//		// Electric's angle
//		double angleE = 0;
//		if (SX >= 0 && SY >= 0)
//		{
//			angleE = angleJ;
//		} else if (SX < 0 && SY < 0)
//		{
//			angleE = 180 + angleJ;
//		} else if (SX < 0 && SY >= 0)
//		{
//			angleE = 90 - angleJ;
//		} else if (SX >= 0 && SY < 0)
//		{
//			angleE = 270 - angleJ;
//		} else
//		{
//			error("programming error: all cases should have been covered");
//		}
//		ep.angle = round((720 + angleE) * 10) % 3600;
//
//		// Electric's scale
//		double seX = signX * SX;
//		double seY = signY * SY;
//
//		// Proto
//		Rectangle2D.Double r = elecBounds;
//
//		// RKao debug
//		//System.out.println("Proto bounds in electric units: "+r);
//
//		double pX = r.x + r.width / 2.0; // center
//		double pY = r.y + r.height / 2.0;
//		double pW = r.width; // width and height
//		double pH = r.height;
//
//		// Electric's center
//		double cX = dX + signX * pX * cos - signY * pY * sin;
//		double cY = dY + signX * pX * sin + signY * pY * cos;
//
//		// RKao debug
//		//System.out.println("(cX, cY): ("+cX+", "+cY+")");
//
//		// Electric's bounding box
//		ep.lx = round(cX - (seX * pW) / 2);
//		ep.hx = round(cX + (seX * pW) / 2);
//		ep.ly = round(cY - (seY * pH) / 2);
//		ep.hy = round(cY + (seY * pH) / 2);
//
//		return ep;
//	}

	// Modify the scales to make it look as if the client is scaling
	// only the visible portion of the NodeProto. For example a 1X 5
	// lambda metal-1/metal-2 via consists of a 4 lambda metal-1/metal-2
	// plus a 1/2 lambda invisible surround. If a client requests a 2X
	// scaling of that give him an 8 lambda metal-1/metal-2 plus a 1/2
	// lambda invisible surround.  Without the following two routines a
	// 2X scaling yields a 9 lambda metal-1/metal-2 via plus a 1/2
	// lambda surround because the surround doesn't scale.
//	double hideInvisScaleX(double clientScaleX)
//	{
//		int invisW = 0;   // sizeOffset.lx + sizeOffset.hx;
//		double totW = elecBounds.width;
//		double joseScaleX =
//			totW == 0
//				? 1
//				: (invisW + (totW - invisW) * Math.abs(clientScaleX)) / totW;
//		return clientScaleX < 0 ? -joseScaleX : joseScaleX;
//	}
//	double hideInvisScaleY(double clientScaleY)
//	{
//		int invisH = 0;   // sizeOffset.ly + sizeOffset.hy;
//		double totH = elecBounds.height;
//		double joseScaleY =
//			totH == 0
//				? 1
//				: (invisH + (totH - invisH) * Math.abs(clientScaleY)) / totH;
//		return clientScaleY < 0 ? -joseScaleY : joseScaleY;
//	}

	// Modify width and height to make it look as if the client is
	// specifying only the visible portion of the NodeProto. Base units.
//	double hideInvisWidToScale(int clientW)
//	{
//		double defW = elecBounds.width;
//		double scaleX =
//			(defW == 0 || clientW == 0)
//				? 1
//				: (Math.abs(clientW) + sizeOffset.lx + sizeOffset.hx) / defW;
//		return clientW < 0 ? -scaleX : scaleX;
//	}
//	double hideInvisHeiToScale(int clientH)
//	{
//		double defH = elecBounds.height;
//		double scaleY =
//			(defH == 0 || clientH == 0)
//				? 1
//				: (Math.abs(clientH) + sizeOffset.ly + sizeOffset.hy) / defH;
//		return clientH < 0 ? -scaleY : scaleY;
//	}

	/**
	 * Add an equivalent NodeProto, which can be swapped with ease with
	 * some other NodeProto.  Don't create this unless it's needed
	 * TODO: implement this, or toss it. (Not an exact match to the c-side
	 * structures
	 */
	/* RKao comment this out until I understand the intention
	   public void addEquivalent(NodeProto other) {
	   // TODO: make sure other isn't already in there
	   if (equiv==null) {
	   equiv= new ArrayList();
	   }
	   equiv.add(other);
	   }
	*/

	/**
	 * remove a NodeProto that was in the equivalent list.  See
	 * <code>addEquivalent</code> for more info
	 */
	/* RKao comment this out until I understand the intention
	   public void removeEquivalent(NodeProto np) {
	   // TODO: make sure the other is in the list
	   if (equiv!=null) {
	   equiv.remove(np);
	   }
	   }
	*/

	/**
	 * get a list of equivalent nodes, as an iterator.  See 
	 * <code>addEquivalent</code> for more info
	 */
	/* RKao comment this out until I understand the intention
	   public Iterator getEquivalents() {
	   return equiv.iterator();
	   }
	*/

	/** Get the default bounding box in Lambda units.
	 *
	 * <p> This is the default bounding box that would result if this
	 * NodeProto were instantiated with scaleX=scaleY=1, (x,y) = (0, 0),
	 * and angle=0.
	 * 
	 * <p> If this is a Cell with instances of two or more
	 * Essential-Bounds PrimitiveNodes then return the smallest bounding
	 * box that contains all of them.
	 *
	 * <p> Note that this routine excludes materials, real and
	 * imaginary, that surround Electric's NodeProtos. 
	 *
	 * <p> The coordinates are relative to this NodeProto's reference
	 * point.
	public Rectangle2D getBounds()
	{
		if (this instanceof Cell)
			 ((Cell) this).updateBounds();

		Point2D rp = getReferencePoint();

		Rectangle v = getVisBounds();
		if (this instanceof Cell)
		{
			Rectangle eb = ((Cell) this).findEssentialBounds();
			if (eb != null)
				v = eb;
		}

		return new Rectangle2D.Double(
			v.x - rp.getX(),
			v.y - rp.getY(),
			v.width,
			v.height);
	} */
}

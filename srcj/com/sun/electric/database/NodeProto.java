package com.sun.electric.database;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
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
	/**
	 * Function is a typesafe enum class that describes the function of an arcproto.
	 */
	public static class Function
	{
		private final String name;
		private final String shortName;
		private final String constantName;

		private Function(String name, String shortName, String constantName)
		{
			this.name = name;
			this.shortName = shortName;
			this.constantName = constantName;
		}

		public String toString() { return name; }

		/** node is unknown type */
			public static final Function UNKNOWN=   new Function("unknown",						"node",     "NPUNKNOWN");
		/** node is a single-layer pin */
			public static final Function PIN=       new Function("pin",							"pin",      "NPPIN");
		/** node is a two-layer contact (one point) */
			public static final Function CONTACT=   new Function("contact",						"contact",  "NPCONTACT");
		/** node is a single-layer node */
			public static final Function NODE=      new Function("pure-layer-node",				"plnode",   "NPNODE");
		/** node connects all ports */
			public static final Function CONNECT=   new Function("connection",					"conn",     "NPCONNECT");
		/** node is MOS enhancement transistor */
			public static final Function TRANMOS=   new Function("nMOS-transistor",				"nmos",     "NPTRANMOS");
		/** node is MOS depletion transistor */
			public static final Function TRADMOS=   new Function("DMOS-transistor",				"dmos",     "NPTRADMOS");
		/** node is MOS complementary transistor */
			public static final Function TRAPMOS=   new Function("pMOS-transistor",				"pmos",     "NPTRAPMOS");
		/** node is NPN junction transistor */
			public static final Function TRANPN=    new Function("NPN-transistor",				"npn",      "NPTRANPN");
		/** node is PNP junction transistor */
			public static final Function TRAPNP=    new Function("PNP-transistor",				"pnp",      "NPTRAPNP");
		/** node is N-channel junction transistor */
			public static final Function TRANJFET=  new Function("n-type-JFET-transistor",		"njfet",    "NPTRANJFET");
		/** node is P-channel junction transistor */
			public static final Function TRAPJFET=  new Function("p-type-JFET-transistor",		"pjfet",    "NPTRAPJFET");
		/** node is MESFET depletion transistor */
			public static final Function TRADMES=   new Function("depletion-mesfet",			"dmes",     "NPTRADMES");
		/** node is MESFET enhancement transistor */
			public static final Function TRAEMES=   new Function("enhancement-mesfet",			"emes",     "NPTRAEMES");
		/** node is prototype-defined transistor */
			public static final Function TRANSREF=  new Function("prototype-defined-transistor","tref",     "NPTRANSREF");
		/** node is undetermined transistor */
			public static final Function TRANS=     new Function("transistor",					"trans",    "NPTRANS");
		/** node is 4-port MOS enhancement transistor */
			public static final Function TRA4NMOS=  new Function("4-port-nMOS-transistor",		"nmos4p",   "NPTRA4NMOS");
		/** node is 4-port MOS depletion transistor */
			public static final Function TRA4DMOS=  new Function("4-port-DMOS-transistor",		"dmos4p",   "NPTRA4DMOS");
		/** node is 4-port MOS complementary transistor */
			public static final Function TRA4PMOS=  new Function("4-port-pMOS-transistor",		"pmos4p",   "NPTRA4PMOS");
		/** node is 4-port NPN junction transistor */
			public static final Function TRA4NPN=   new Function("4-port-NPN-transistor",		"npn4p",    "NPTRA4NPN");
		/** node is 4-port PNP junction transistor */
			public static final Function TRA4PNP=   new Function("4-port-PNP-transistor",		"pnp4p",    "NPTRA4PNP");
		/** node is 4-port N-channel junction transistor */
			public static final Function TRA4NJFET= new Function("4-port-n-type-JFET-transistor","njfet4p", "NPTRA4NJFET");
		/** node is 4-port P-channel junction transistor */
			public static final Function TRA4PJFET= new Function("4-port-p-type-JFET-transistor","pjfet4p", "NPTRA4PJFET");
		/** node is 4-port MESFET depletion transistor */
			public static final Function TRA4DMES=  new Function("4-port-depletion-mesfet",		"dmes4p",   "NPTRA4DMES");
		/** node is 4-port MESFET enhancement transistor */
			public static final Function TRA4EMES=  new Function("4-port-enhancement-mesfet",	"emes4p",   "NPTRA4EMES");
		/** node is E2L transistor */
			public static final Function TRANS4=    new Function("4-port-transistor",			"trans4p",  "NPTRANS4");
		/** node is resistor */
			public static final Function RESIST=    new Function("resistor",					"res",      "NPRESIST");
		/** node is capacitor */
			public static final Function CAPAC=     new Function("capacitor",					"cap",      "NPCAPAC");
		/** node is electrolytic capacitor */
			public static final Function ECAPAC=    new Function("electrolytic-capacitor",		"ecap",     "NPECAPAC");
		/** node is diode */
			public static final Function DIODE=     new Function("diode",						"diode",    "NPDIODE");
		/** node is zener diode */
			public static final Function DIODEZ=    new Function("zener-diode",					"zdiode",   "NPDIODEZ");
		/** node is inductor */
			public static final Function INDUCT=    new Function("inductor",					"ind",      "NPINDUCT");
		/** node is meter */
			public static final Function METER=     new Function("meter",						"meter",    "NPMETER");
		/** node is transistor base */
			public static final Function BASE=      new Function("base",						"base",     "NPBASE");
		/** node is transistor emitter */
			public static final Function EMIT=      new Function("emitter",						"emit",     "NPEMIT");
		/** node is transistor collector */
			public static final Function COLLECT=   new Function("collector",					"coll",     "NPCOLLECT");
		/** node is buffer */
			public static final Function BUFFER=    new Function("buffer",						"buf",      "NPBUFFER");
		/** node is AND gate */
			public static final Function GATEAND=   new Function("AND-gate",					"and",      "NPGATEAND");
		/** node is OR gate */
			public static final Function GATEOR=    new Function("OR-gate",						"or",       "NPGATEOR");
		/** node is XOR gate */
			public static final Function GATEXOR=   new Function("XOR-gate",					"xor",      "NPGATEXOR");
		/** node is flip-flop */
			public static final Function FLIPFLOP=  new Function("flip-flop",					"ff",       "NPFLIPFLOP");
		/** node is multiplexor */
			public static final Function MUX=       new Function("multiplexor",					"mux",      "NPMUX");
		/** node is connected to power */
			public static final Function CONPOWER=  new Function("power",						"pwr",      "NPCONPOWER");
		/** node is connected to ground */
			public static final Function CONGROUND= new Function("ground",						"gnd",      "NPCONGROUND");
		/** node is source */
			public static final Function SOURCE=    new Function("source",						"source",   "NPSOURCE");
		/** node is connected to substrate */
			public static final Function SUBSTRATE= new Function("substrate",					"substr",   "NPSUBSTRATE");
		/** node is connected to well */
			public static final Function WELL=      new Function("well",						"well",     "NPWELL");
		/** node is pure artwork */
			public static final Function ART=       new Function("artwork",						"art",      "NPART");
		/** node is an array */
			public static final Function ARRAY=     new Function("array",						"array",    "NPARRAY");
		/** node is an alignment object */
			public static final Function ALIGN=     new Function("align",						"align",    "NPALIGN");
		/** node is a current-controlled voltage source */
			public static final Function CCVS=      new Function("ccvs",						"ccvs",     "NPCCVS");
		/** node is a current-controlled current source */
			public static final Function CCCS=      new Function("cccs",						"cccs",     "NPCCCS");
		/** node is a voltage-controlled voltage source */
			public static final Function VCVS=      new Function("vcvs",						"vcvs",     "NPVCVS");
		/** node is a voltage-controlled current source */
			public static final Function VCCS=      new Function("vccs",						"vccs",     "NPVCCS");
		/** node is a transmission line */
			public static final Function TLINE=     new Function("transmission-line",			"transm",   "NPTLINE");
	}
	// ------------------------ private data --------------------------

	/** set if nonmanhattan instances shrink */				private static final int NODESHRINK=           01;
	/** set if instances should be expanded */				private static final int WANTNEXPAND=          02;
	/** node function (from efunction.h) */					private static final int NFUNCTION=          0774;
	/** right shift for NFUNCTION */						private static final int NFUNCTIONSH=           2;
	/** set if instances can be wiped */					private static final int ARCSWIPE=          01000;
	/** set if node is to be kept square in size */			private static final int NSQUARE=           02000;
	/** primitive can hold trace information */				private static final int HOLDSTRACE=        04000;
//	/** set to reevaluate this cell's network */			private static final int REDOCELLNET=      010000;
	/** set to erase if connected to 1 or 2 arcs */			private static final int WIPEON1OR2=       020000;
	/** set if primitive is lockable (cannot move) */		private static final int LOCKEDPRIM=       040000;
	/** set if primitive is selectable by edge, not area */	private static final int NEDGESELECT=     0100000;
	/** set if nonmanhattan arcs on this shrink */			private static final int ARCSHRINK=       0200000;
	//  used by database:                                                                            01400000
	/** set if not used (don't put in menu) */				private static final int NNOTUSED=       02000000;
	/** set if everything in cell is locked */				private static final int NPLOCKED=       04000000;
	/** set if instances in cell are locked */				private static final int NPILOCKED=     010000000;
	/** set if cell is part of a "cell library" */			private static final int INCELLLIBRARY= 020000000;
	/** set if cell is from a technology-library */			private static final int TECEDITCELL=   040000000;

	/** the name of the NodeProto */						protected String protoName;
	/** the exports in the NodeProto */						private List ports;
	/** JNetworks that comprise this NodeProto */			private List networks;
	/** All instances of this NodeProto */					private List instances;
	/** flag bits */										private int userBits;
	/** the function of this NodeProto */					private Function function;

	// ----------------- protected and private methods -----------------

	protected NodeProto()
	{
		ports = new ArrayList();
		instances = new ArrayList();
		networks = new ArrayList();
		function = Function.UNKNOWN;
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
//		removeAll(ports);

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

	/** Set the Shrink bit */
	public void setShrunk() { userBits |= NODESHRINK; }
	/** Clear the Shrink bit */
	public void clearShrunk() { userBits &= ~NODESHRINK; }
	/** Get the Shrink bit */
	public boolean isShrunk() { return (userBits & NODESHRINK) != 0; }

	/** Set the Want-Expansion bit */
	public void setWantExpanded() { userBits |= WANTNEXPAND; }
	/** Clear the Want-Expansion bit */
	public void clearWantExpanded() { userBits &= ~WANTNEXPAND; }
	/** Get the Want-Expansion bit */
	public boolean isWantExpanded() { return (userBits & WANTNEXPAND) != 0; }

	/** Set the Arc function */
	public void setFunction(Function function) { this.function = function; }
	/** Get the Arc function */
	public Function getFunction() { return function; }

	/** Set the Arcs-Wipe bit */
	public void setArcsWipe() { userBits |= ARCSWIPE; }
	/** Clear the Arcs-Wipe bit */
	public void clearArcsWipe() { userBits &= ~ARCSWIPE; }
	/** Get the Arcs-Wipe bit */
	public boolean isArcsWipe() { return (userBits & ARCSWIPE) != 0; }

	/** Set the Square-Node bit */
	public void setSquare() { userBits |= NSQUARE; }
	/** Clear the Square-Node bit */
	public void clearSquare() { userBits &= ~NSQUARE; }
	/** Get the Square-Node bit */
	public boolean isSquare() { return (userBits & NSQUARE) != 0; }

	/** Set the Holds-Outline bit */
	public void setHoldsOutline() { userBits |= HOLDSTRACE; }
	/** Clear the Holds-Outline bit */
	public void clearHoldsOutline() { userBits &= ~HOLDSTRACE; }
	/** Get the Holds-Outline bit */
	public boolean isHoldsOutline() { return (userBits & HOLDSTRACE) != 0; }

	/** Set the Wipes (erases) if 1 or 2 arcs connected bit */
	public void setWipeOn1or2() { userBits |= WIPEON1OR2; }
	/** Clear the Wipes (erases) if 1 or 2 arcs connected bit */
	public void clearWipeOn1or2() { userBits &= ~WIPEON1OR2; }
	/** Get the Wipes (erases) if 1 or 2 arcs connected bit */
	public boolean isWipeOn1or2() { return (userBits & WIPEON1OR2) != 0; }

	/** Set the Locked-Primitive bit */
	public void setLockedPrim() { userBits |= LOCKEDPRIM; }
	/** Clear the Locked-Primitive bit */
	public void clearLockedPrim() { userBits &= ~LOCKEDPRIM; }
	/** Get the Locked-Primitive bit */
	public boolean isLockedPrim() { return (userBits & LOCKEDPRIM) != 0; }

	/** Set the Edge-Select bit */
	public void setEdgeSelect() { userBits |= NEDGESELECT; }
	/** Clear the Edge-Select bit */
	public void clearEdgeSelect() { userBits &= ~NEDGESELECT; }
	/** Get the Edge-Select bit */
	public boolean isEdgeSelect() { return (userBits & NEDGESELECT) != 0; }

	/** Set the bit if nonmanhattan arcs can shrink this node */
	public void setArcsShrink() { userBits |= ARCSHRINK; }
	/** Clear the bit if nonmanhattan arcs can shrink this node */
	public void clearArcsShrink() { userBits &= ~ARCSHRINK; }
	/** Get the bit if nonmanhattan arcs can shrink this node */
	public boolean isArcsShrink() { return (userBits & ARCSHRINK) != 0; }

	/** Set the All-Contents-Locked bit */
	public void setAllLocked() { userBits |= NPLOCKED; }
	/** Clear the All-Contents-Locked bit */
	public void clearAllLocked() { userBits &= ~NPLOCKED; }
	/** Get the All-Contents-Locked bit */
	public boolean isAllLocked() { return (userBits & NPLOCKED) != 0; }

	/** Set the Not-Used bit */
	public void setNotUsed() { userBits |= NNOTUSED; }
	/** Clear the Not-Used bit */
	public void clearNotUsed() { userBits &= ~NNOTUSED; }
	/** Get the Not-Used bit */
	public boolean isNotUsed() { return (userBits & NNOTUSED) != 0; }

	/** Set the Instances-Locked bit */
	public void setInstancesLocked() { userBits |= NPILOCKED; }
	/** Clear the Instances-Locked bit */
	public void clearInstancesLocked() { userBits &= ~NPILOCKED; }
	/** Get the Instances-Locked bit */
	public boolean isInstancesLocked() { return (userBits & NPILOCKED) != 0; }

	/** Set the In-Cell-Library bit */
	public void setInCellLibrary() { userBits |= INCELLLIBRARY; }
	/** Clear the In-Cell-Library bit */
	public void clearInCellLibrary() { userBits &= ~INCELLLIBRARY; }
	/** Get the In-Cell-Library bit */
	public boolean isInCellLibrary() { return (userBits & INCELLLIBRARY) != 0; }

	/** Set the In-Technology-Library bit */
	public void setInTechnologyLibrary() { userBits |= TECEDITCELL; }
	/** Clear the In-Technology-Library bit */
	public void clearInTechnologyLibrary() { userBits &= ~TECEDITCELL; }
	/** Get the In-Technology-Library bit */
	public boolean isInTechnologyLibrary() { return (userBits & TECEDITCELL) != 0; }

	public abstract double getDefWidth();
	public abstract double getDefHeight();
	public abstract double getWidthOffset();
	public abstract double getHeightOffset();

	public abstract Technology getTechnology();

	/** A NodeProto's <i>reference point</i> is (0, 0) unless the
	 * NodeProto is a Cell containing an instance of a Cell-Center in
	 * which case the reference point is the location of that
	 * Cell-Center instance. */
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
			if (pp.getProtoName().equals(name))
				return pp;
		}
		return null;
	}

	public static NodeProto findNodeProto(String line)
	{
		Technology tech = Technology.getCurrent();
		Library lib = Library.getCurrent();
		boolean saidtech = false;
		boolean saidlib = false;
		int colon = line.indexOf(':');
		String withoutPrefix;
		if (colon == -1) withoutPrefix = line; else
		{
			String prefix = line.substring(0, colon);
			Technology t = Technology.findTechnology(prefix);
			if (t != null)
			{
				tech = t;
				saidtech = true;
			}
			Library l = Library.findLibrary(prefix);
			if (l != null)
			{
				lib = l;
				saidlib = true;
			}
			withoutPrefix = line.substring(colon+1);
		}

		/* try primitives in the technology */
		if (!saidlib)
		{
			PrimitiveNode np = tech.findNodeProto(withoutPrefix);
			if (np != null) return np;
		}
		
		if (!saidtech)
		{
			Cell np = lib.findNodeProto(withoutPrefix);
			if (np != null) return np;
		}
		return null;
	}

//		/* get the view and version information */
//		for(pt = line; *pt != 0; pt++) if (*pt == ';' || *pt == '{') break;
//		nameend = pt;
//		wantversion = -1;
//		wantview = el_unknownview;
//		if (*pt == ';')
//		{
//			wantversion = myatoi(pt+1);
//			for(pt++; *pt != 0; pt++) if (*pt == '{') break;
//		}
//		if (*pt == '{')
//		{
//			viewname = pt = (pt + 1);
//			for(; *pt != 0; pt++) if (*pt == '}') break;
//			if (*pt != '}') return(NONODEPROTO);
//			*pt = 0;
//			for(v = el_views; v != NOVIEW; v = v->nextview)
//				if (namesame(v->sviewname, viewname) == 0 || namesame(v->viewname, viewname) == 0) break;
//			*pt = '}';
//			if (v == NOVIEW) return(NONODEPROTO);
//			wantview = v;
//		}
//		save = *nameend;
//		*nameend = 0;
//		np = db_findnodeprotoname(line, wantview, lib);
//		if (np == NONODEPROTO && wantview == el_unknownview)
//		{
//			/* search for any view */
//			for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//				if (namesame(line, np->protoname) == 0) break;
//		}
//		*nameend = (CHAR)save;
//		if (np == NONODEPROTO) return(NONODEPROTO);
//		if (wantversion < 0 || np->version == wantversion) return(np);
//		for(np = np->prevversion; np != NONODEPROTO; np = np->prevversion)
//			if (np->version == wantversion) return(np);
//		return(NONODEPROTO);
//	}

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

	/** printable version of this object */
	public String toString()
	{
		return "NodeProto " + describe();
	}
}

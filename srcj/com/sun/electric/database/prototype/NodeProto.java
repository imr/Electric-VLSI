/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NodeProto.java
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
package com.sun.electric.database.prototype;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The NodeProto class defines a type of NodeInst.
 * It is an abstract class that can be implemented as PrimitiveNode (for primitives from Technologies)
 * or as Cell (for cells in Libraries).
 * <P>
 * Every node in the database appears as one <I>prototypical</I> object and many <I>instantiative</I> objects.
 * Thus, for a PrimitiveNode such as the CMOS P-transistor there is one object (called a PrimitiveNode, which is a NodeProto)
 * that describes the transistor prototype and there are many objects (called NodeInsts),
 * one for every instance of a transistor that appears in a circuit.
 * Similarly, for every Cell, there is one object (called a Cell, which is a NodeProto)
 * that describes the Cell with everything in it and there are many objects (also called NodeInsts)
 * for every use of that Cell in some other Cell.
 * PrimitiveNodes are statically created and placed in the Technology objects,
 * but complex Cells are created by the tools and placed in Library objects.
 * <P>
 * The basic NodeProto has a list of ports, the bounds, a list of instances of this NodeProto
 * as a NodeInst in other Cells, and much more.
 */
public abstract class NodeProto extends ElectricObject
{
	/**
	 * Function is a typesafe enum class that describes the function of a NodeProto.
	 * Functions are technology-independent and include different types of transistors,
	 * contacts, and other circuit elements.
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

		/**
		 * Returns a printable version of this Function.
		 * @return a printable version of this Function.
		 */
		public String toString() { return name; }

		/** Describes a node with unknown type. */
			public static final Function UNKNOWN=   new Function("unknown",						"node",     "NPUNKNOWN");
		/** Describes a single-layer pin. */
			public static final Function PIN=       new Function("pin",							"pin",      "NPPIN");
		/** Describes a two-layer contact. */
			public static final Function CONTACT=   new Function("contact",						"contact",  "NPCONTACT");
		/** Describes a single-layer node. */
			public static final Function NODE=      new Function("pure-layer-node",				"plnode",   "NPNODE");
		/** node a node that connects all ports. */
			public static final Function CONNECT=   new Function("connection",					"conn",     "NPCONNECT");
		/** Describes a MOS enhancement transistor. */
			public static final Function TRANMOS=   new Function("nMOS-transistor",				"nmos",     "NPTRANMOS");
		/** Describes a MOS depletion transistor. */
			public static final Function TRADMOS=   new Function("DMOS-transistor",				"dmos",     "NPTRADMOS");
		/** Describes a MOS complementary transistor. */
			public static final Function TRAPMOS=   new Function("pMOS-transistor",				"pmos",     "NPTRAPMOS");
		/** Describes a NPN junction transistor. */
			public static final Function TRANPN=    new Function("NPN-transistor",				"npn",      "NPTRANPN");
		/** Describes a PNP junction transistor. */
			public static final Function TRAPNP=    new Function("PNP-transistor",				"pnp",      "NPTRAPNP");
		/** Describes a N-channel junction transistor. */
			public static final Function TRANJFET=  new Function("n-type-JFET-transistor",		"njfet",    "NPTRANJFET");
		/** Describes a P-channel junction transistor. */
			public static final Function TRAPJFET=  new Function("p-type-JFET-transistor",		"pjfet",    "NPTRAPJFET");
		/** Describes a MESFET depletion transistor. */
			public static final Function TRADMES=   new Function("depletion-mesfet",			"dmes",     "NPTRADMES");
		/** Describes a MESFET enhancement transistor. */
			public static final Function TRAEMES=   new Function("enhancement-mesfet",			"emes",     "NPTRAEMES");
		/** Describes a prototype-defined transistor. */
			public static final Function TRANSREF=  new Function("prototype-defined-transistor","tref",     "NPTRANSREF");
		/** Describes an undetermined transistor. */
			public static final Function TRANS=     new Function("transistor",					"trans",    "NPTRANS");
		/** Describes a 4-port MOS enhancement transistor. */
			public static final Function TRA4NMOS=  new Function("4-port-nMOS-transistor",		"nmos4p",   "NPTRA4NMOS");
		/** Describes a 4-port MOS depletion transistor. */
			public static final Function TRA4DMOS=  new Function("4-port-DMOS-transistor",		"dmos4p",   "NPTRA4DMOS");
		/** Describes a 4-port MOS complementary transistor. */
			public static final Function TRA4PMOS=  new Function("4-port-pMOS-transistor",		"pmos4p",   "NPTRA4PMOS");
		/** Describes a 4-port NPN junction transistor. */
			public static final Function TRA4NPN=   new Function("4-port-NPN-transistor",		"npn4p",    "NPTRA4NPN");
		/** Describes a 4-port PNP junction transistor. */
			public static final Function TRA4PNP=   new Function("4-port-PNP-transistor",		"pnp4p",    "NPTRA4PNP");
		/** Describes a 4-port N-channel junction transistor. */
			public static final Function TRA4NJFET= new Function("4-port-n-type-JFET-transistor","njfet4p", "NPTRA4NJFET");
		/** Describes a 4-port P-channel junction transistor. */
			public static final Function TRA4PJFET= new Function("4-port-p-type-JFET-transistor","pjfet4p", "NPTRA4PJFET");
		/** Describes a 4-port MESFET depletion transistor. */
			public static final Function TRA4DMES=  new Function("4-port-depletion-mesfet",		"dmes4p",   "NPTRA4DMES");
		/** Describes a 4-port MESFET enhancement transistor. */
			public static final Function TRA4EMES=  new Function("4-port-enhancement-mesfet",	"emes4p",   "NPTRA4EMES");
		/** Describes an E2L transistor. */
			public static final Function TRANS4=    new Function("4-port-transistor",			"trans4p",  "NPTRANS4");
		/** Describes a resistor. */
			public static final Function RESIST=    new Function("resistor",					"res",      "NPRESIST");
		/** Describes a capacitor. */
			public static final Function CAPAC=     new Function("capacitor",					"cap",      "NPCAPAC");
		/** Describes an electrolytic capacitor. */
			public static final Function ECAPAC=    new Function("electrolytic-capacitor",		"ecap",     "NPECAPAC");
		/** Describes a diode. */
			public static final Function DIODE=     new Function("diode",						"diode",    "NPDIODE");
		/** Describes a zener diode. */
			public static final Function DIODEZ=    new Function("zener-diode",					"zdiode",   "NPDIODEZ");
		/** Describes an inductor. */
			public static final Function INDUCT=    new Function("inductor",					"ind",      "NPINDUCT");
		/** Describes a meter. */
			public static final Function METER=     new Function("meter",						"meter",    "NPMETER");
		/** Describes a transistor base. */
			public static final Function BASE=      new Function("base",						"base",     "NPBASE");
		/** Describes a transistor emitter. */
			public static final Function EMIT=      new Function("emitter",						"emit",     "NPEMIT");
		/** Describes a transistor collector. */
			public static final Function COLLECT=   new Function("collector",					"coll",     "NPCOLLECT");
		/** Describes a buffer. */
			public static final Function BUFFER=    new Function("buffer",						"buf",      "NPBUFFER");
		/** Describes an AND gate. */
			public static final Function GATEAND=   new Function("AND-gate",					"and",      "NPGATEAND");
		/** Describes an OR gate. */
			public static final Function GATEOR=    new Function("OR-gate",						"or",       "NPGATEOR");
		/** Describes an XOR gate. */
			public static final Function GATEXOR=   new Function("XOR-gate",					"xor",      "NPGATEXOR");
		/** Describes a flip-flop. */
			public static final Function FLIPFLOP=  new Function("flip-flop",					"ff",       "NPFLIPFLOP");
		/** Describes a multiplexor. */
			public static final Function MUX=       new Function("multiplexor",					"mux",      "NPMUX");
		/** Describes a power connection. */
			public static final Function CONPOWER=  new Function("power",						"pwr",      "NPCONPOWER");
		/** Describes a ground connection. */
			public static final Function CONGROUND= new Function("ground",						"gnd",      "NPCONGROUND");
		/** Describes voltage or current source. */
			public static final Function SOURCE=    new Function("source",						"source",   "NPSOURCE");
		/** Describes a substrate contact. */
			public static final Function SUBSTRATE= new Function("substrate",					"substr",   "NPSUBSTRATE");
		/** Describes a well contact. */
			public static final Function WELL=      new Function("well",						"well",     "NPWELL");
		/** Describes a pure artwork. */
			public static final Function ART=       new Function("artwork",						"art",      "NPART");
		/** Describes an array. */
			public static final Function ARRAY=     new Function("array",						"array",    "NPARRAY");
		/** Describes an alignment object. */
			public static final Function ALIGN=     new Function("align",						"align",    "NPALIGN");
		/** Describes a current-controlled voltage source. */
			public static final Function CCVS=      new Function("ccvs",						"ccvs",     "NPCCVS");
		/** Describes a current-controlled current source. */
			public static final Function CCCS=      new Function("cccs",						"cccs",     "NPCCCS");
		/** Describes a voltage-controlled voltage source. */
			public static final Function VCVS=      new Function("vcvs",						"vcvs",     "NPVCVS");
		/** Describes a voltage-controlled current source. */
			public static final Function VCCS=      new Function("vccs",						"vccs",     "NPVCCS");
		/** Describes a transmission line. */
			public static final Function TLINE=     new Function("transmission-line",			"transm",   "NPTLINE");
	}

	// ------------------------ private data --------------------------

	/** set if nonmanhattan instances shrink */				private static final int NODESHRINK =           01;
	/** set if instances should be expanded */				private static final int WANTNEXPAND =          02;
	/** node function (from efunction.h) */					private static final int NFUNCTION =          0774;
	/** right shift for NFUNCTION */						private static final int NFUNCTIONSH =           2;
	/** set if instances can be wiped */					private static final int ARCSWIPE =          01000;
	/** set if node is to be kept square in size */			private static final int NSQUARE =           02000;
	/** primitive can hold trace information */				private static final int HOLDSTRACE =        04000;
//	/** set to reevaluate this cell's network */			private static final int REDOCELLNET =      010000;
	/** set to erase if connected to 1 or 2 arcs */			private static final int WIPEON1OR2 =       020000;
	/** set if primitive is lockable (cannot move) */		private static final int LOCKEDPRIM =       040000;
	/** set if primitive is selectable by edge, not area */	private static final int NEDGESELECT =     0100000;
	/** set if nonmanhattan arcs on this shrink */			private static final int ARCSHRINK =       0200000;
	//  used by database:                                                                             01400000
	/** set if not used (don't put in menu) */				private static final int NNOTUSED =       02000000;
	/** set if everything in cell is locked */				private static final int NPLOCKED =       04000000;
	/** set if instances in cell are locked */				private static final int NPILOCKED =     010000000;
	/** set if cell is part of a "cell library" */			private static final int INCELLLIBRARY = 020000000;
	/** set if cell is from a technology-library */			private static final int TECEDITCELL =   040000000;

	/** The name of the NodeProto. */						protected String protoName;
	/** This NodeProto's Technology. */						protected Technology tech;
	/** A list of exports on the NodeProto. */				private List ports;
	/** A list of JNetworks in this NodeProto. */			private List networks;
	/** A list of instances of this NodeProto. */			private List instances;
	/** Internal flag bits. */								protected int userBits;
	/** The function of this NodeProto. */					private Function function;

	/** The temporary integer value. */						private int tempInt;
	/** The temporary Object. */							private Object tempObj;
	/** The temporary flag bits. */							private int flagBits;
	/** The object used to request flag bits. */			private static FlagSet.Generator flagGenerator;

	// ----------------- protected and private methods -----------------

	/**
	 * This constructor should not be called.
	 * Use the subclass factory methods to create Cell or PrimitiveNode objects.
	 */
	protected NodeProto()
	{
		ports = new ArrayList();
		instances = new ArrayList();
		networks = new ArrayList();
		function = Function.UNKNOWN;
		tempObj = null;
	}

	/**
	 * Add a PortProto to this NodeProto.
	 * Adds Exports for Cells, PrimitivePorts for PrimitiveNodes.
	 * @param port the PortProto to add to this NodeProto.
	 */
	void addPort(PortProto port)
	{
		ports.add(port);
	}

	/**
	 * Removes a PortProto from this NodeProto.
	 * @param port the PortProto to remove from this NodeProto.
	 */
	void removePort(PortProto port)
	{
		ports.remove(port);
	}

	/**
	 * Add a Network to this NodeProto.
	 * @param n the JNetwork to add to this NodeProto.
	 */
	public void addNetwork(JNetwork n)
	{
		if (networks.contains(n))
		{
			System.out.println("Cell " + this +" already contains network " + n);
		}
		networks.add(n);
	}

	/**
	 * Removes a Network from this NodeProto.
	 * @param n the Network to remove from this NodeProto.
	 */
	void removeNetwork(JNetwork n)
	{
		if (!networks.contains(n))
		{
			System.out.println("Cell " + this +" doesn't contain network " + n);
		}
		networks.remove(n);
	}

	/**
	 * Removes all Networks from this NodeProto.
	 */
	public void removeAllNetworks()
	{
		networks.clear();
	}

	/**
	 * Add to the list of instances of this NodeProto.
	 * @param ni the NodeInst which is an instance of this NodeProto.
	 */
	public void addInstance(NodeInst ni)
	{
		if (instances == null)
		{
			System.out.println("Hmm.  Instances is *still* null!");
		}
		instances.add(ni);
	}

	/**
	 * Removes a NodeInst from the list of instances of this NodeProto.
	 * @param ni the NodeInst which is an instance of this NodeProto.
	 */
	public void removeInstance(NodeInst ni)
	{
		instances.remove(ni);
	}

	/**
	 * Remove this NodeProto.
	 * Also deletes the ports associated with this NodeProto.
	 */
	public void remove()
	{
		// kill ports
//		removeAll(ports);

		// unhook from networks
		while (networks.size() > 0)
		{
			removeNetwork((JNetwork) networks.get(networks.size() - 1));
		}
	}

	/**
	 * Routine to determine whether this NodeProto contains a PortProto.
	 * @param port the port being sought on this NodeProto.
	 * @return true if the port is on this NodeProto.
	 */
	boolean containsPort(PortProto port)
	{
		return ports.contains(port);
	}

	// ----------------------- public methods -----------------------

	/**
	 * Routine to set this NodeProto to be "shrunk".
	 * A shrunk node is one that may be reduced in size to accomodate its environment.
	 */
	public void setShrunk() { userBits |= NODESHRINK; }

	/**
	 * Routine to set this NodeProto to be "not shrunk".
	 * A shrunk node is one that may be reduced in size to accomodate its environment.
	 */
	public void clearShrunk() { userBits &= ~NODESHRINK; }

	/**
	 * Routine to tell if this NodeProto is "shrunk".
	 * A shrunk node is one that may be reduced in size to accomodate its environment.
	 * @return true if this NodeProto is "shrunk".
	 */
	public boolean isShrunk() { return (userBits & NODESHRINK) != 0; }

	/**
	 * Routine to set this NodeProto so that instances of it are "expanded" by when created.
	 */
	public void setWantExpanded() { userBits |= WANTNEXPAND; }

	/**
	 * Routine to set this NodeProto so that instances of it are "not expanded" by when created.
	 */
	public void clearWantExpanded() { userBits &= ~WANTNEXPAND; }

	/**
	 * Routine to tell if instances of it are "not expanded" by when created.
	 * @return true if instances of it are "not expanded" by when created.
	 */
	public boolean isWantExpanded() { return (userBits & WANTNEXPAND) != 0; }

	/**
	 * Routine to set the function of this NodeProto.
	 * The Function is a technology-independent description of the behavior of this NodeProto.
	 * @param function the new function of this NodeProto.
	 */
	public void setFunction(Function function) { this.function = function; }

	/**
	 * Routine to return the function of this NodeProto.
	 * The Function is a technology-independent description of the behavior of this NodeProto.
	 * @return function the function of this NodeProto.
	 */
	public Function getFunction() { return function; }

	/**
	 * Routine to set this NodeProto so that instances of it are "arc-wipable".
	 * An arc-wipable node is one that is not drawn if there are connecting arcs
	 * because the arcs cover it.  Typically, pins are wiped.
	 */
	public void setArcsWipe() { userBits |= ARCSWIPE; }

	/**
	 * Routine to set this NodeProto so that instances of it are not "arc-wipable".
	 * An arc-wipable node is one that is not drawn if there are connecting arcs
	 * because the arcs cover it.  Typically, pins are wiped.
	 */
	public void clearArcsWipe() { userBits &= ~ARCSWIPE; }

	/**
	 * Routine to tell if instances of this NodeProto are "arc-wipable" by when created.
	 * An arc-wipable node is one that is not drawn if there are connecting arcs
	 * because the arcs cover it.  Typically, pins are wiped.
	 * @return true if instances of this NodeProto are "arc-wipable" by when created.
	 */
	public boolean isArcsWipe() { return (userBits & ARCSWIPE) != 0; }

	/**
	 * Routine to set this NodeProto so that instances of it are "square".
	 * Square nodes must have the same X and Y size.
	 */
	public void setSquare() { userBits |= NSQUARE; }

	/**
	 * Routine to set this NodeProto so that instances of it are not "square".
	 * Square nodes must have the same X and Y size.
	 */
	public void clearSquare() { userBits &= ~NSQUARE; }

	/**
	 * Routine to tell if instances of this NodeProto are square.
	 * Square nodes must have the same X and Y size.
	 * @return true if instances of this NodeProto are square.
	 */
	public boolean isSquare() { return (userBits & NSQUARE) != 0; }

	/**
	 * Routine to set this NodeProto so that instances of it may hold outline information.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 */
	public void setHoldsOutline() { userBits |= HOLDSTRACE; }

	/**
	 * Routine to set this NodeProto so that instances of it may not hold outline information.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 */
	public void clearHoldsOutline() { userBits &= ~HOLDSTRACE; }

	/**
	 * Routine to tell if instances of this NodeProto can hold an outline.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 * @return true if nstances of this NodeProto can hold an outline.
	 */
	public boolean isHoldsOutline() { return (userBits & HOLDSTRACE) != 0; }

	/**
	 * Routine to set this NodeProto so that instances of it are wiped when 1 or 2 arcs connect.
	 * This is used in Schematics pins, which are not shown if 1 or 2 arcs connect, but are shown
	 * when standing alone, or when 3 or more arcs make a "T" or other connection to it.
	 */
	public void setWipeOn1or2() { userBits |= WIPEON1OR2; }

	/**
	 * Routine to set this NodeProto so that instances of it are not wiped when 1 or 2 arcs connect.
	 * Only Schematics pins enable this state.
	 */
	public void clearWipeOn1or2() { userBits &= ~WIPEON1OR2; }

	/**
	 * Routine to tell if instances of this NodeProto are wiped when 1 or 2 arcs connect.
	 * This is used in Schematics pins, which are not shown if 1 or 2 arcs connect, but are shown
	 * when standing alone, or when 3 or more arcs make a "T" or other connection to it.
	 * @return true if instances of this NodeProto are wiped when 1 or 2 arcs connect.
	 */
	public boolean isWipeOn1or2() { return (userBits & WIPEON1OR2) != 0; }

	/**
	 * Routine to set this NodeProto so that instances of it are locked.
	 * Locked instances are used in FPGA technologies, which place instances that cannot be moved
	 * because it is part of the background circuitry.
	 */
	public void setLockedPrim() { userBits |= LOCKEDPRIM; }

	/**
	 * Routine to set this NodeProto so that instances of it are not locked.
	 * Locked instances are used in FPGA technologies, which place instances that cannot be moved
	 * because it is part of the background circuitry.
	 */
	public void clearLockedPrim() { userBits &= ~LOCKEDPRIM; }

	/**
	 * Routine to tell if instances of this NodeProto are loced.
	 * Locked instances are used in FPGA technologies, which place instances that cannot be moved
	 * because it is part of the background circuitry.
	 * @return true if instances of this NodeProto are loced.
	 */
	public boolean isLockedPrim() { return (userBits & LOCKEDPRIM) != 0; }

	/**
	 * Routine to set this NodeProto so that instances of it are selectable only by their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 */
	public void setEdgeSelect() { userBits |= NEDGESELECT; }

	/**
	 * Routine to set this NodeProto so that instances of it are not selectable only by their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 */
	public void clearEdgeSelect() { userBits &= ~NEDGESELECT; }

	/**
	 * Routine to tell if instances of this NodeProto are selectable on their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 * @return true if instances of this NodeProto are selectable on their edges.
	 */
	public boolean isEdgeSelect() { return (userBits & NEDGESELECT) != 0; }

	/**
	 * Routine to set this NodeProto so that instances of it are shrinkable if connected in nonManhattan ways.
	 * An example of a shrinkable node is the MOS transistor, whose tabs shrink slightly if connected to a nonmanhattan arc.
	 * Note that the nonManhattan arc must also shrink, and then the connection is smooth.
	 */
	public void setArcsShrink() { userBits |= ARCSHRINK; }

	/**
	 * Routine to set this NodeProto so that instances of it are not shrinkable if connected in nonManhattan ways.
	 * An example of a shrinkable node is the MOS transistor, whose tabs shrink slightly if connected to a nonmanhattan arc.
	 * Note that the nonManhattan arc must also shrink, and then the connection is smooth.
	 */
	public void clearArcsShrink() { userBits &= ~ARCSHRINK; }

	/**
	 * Routine to tell if instances of this NodeProto are shrinkable if connected in nonManhattan ways.
	 * An example of a shrinkable node is the MOS transistor, whose tabs shrink slightly if connected to a nonmanhattan arc.
	 * Note that the nonManhattan arc must also shrink, and then the connection is smooth.
	 * @return true if instances of this NodeProto are shrinkable if connected in nonManhattan ways.
	 */
	public boolean isArcsShrink() { return (userBits & ARCSHRINK) != 0; }

	/**
	 * Routine to set this NodeProto so that everything inside of it is locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 */
	public void setAllLocked() { userBits |= NPLOCKED; }

	/**
	 * Routine to set this NodeProto so that everything inside of it is not locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 */
	public void clearAllLocked() { userBits &= ~NPLOCKED; }

	/**
	 * Routine to tell if the contents of this NodeProto are locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 * @return true if the contents of this NodeProto are locked.
	 */
	public boolean isAllLocked() { return (userBits & NPLOCKED) != 0; }

	/**
	 * Routine to set this NodeProto so that all instances inside of it are locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 */
	public void setInstancesLocked() { userBits |= NPILOCKED; }

	/**
	 * Routine to set this NodeProto so that all instances inside of it are not locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 */
	public void clearInstancesLocked() { userBits &= ~NPILOCKED; }

	/**
	 * Routine to tell if the sub-instances in this NodeProto are locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 * @return true if the sub-instances in this NodeProto are locked.
	 */
	public boolean isInstancesLocked() { return (userBits & NPILOCKED) != 0; }

	/**
	 * Routine to set this NodeProto so that it is not used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 */
	public void setNotUsed() { userBits |= NNOTUSED; }

	/**
	 * Routine to set this NodeProto so that it is used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 */
	public void clearNotUsed() { userBits &= ~NNOTUSED; }

	/**
	 * Routine to tell if this NodeProto is used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 * @return true if this NodeProto is used.
	 */
	public boolean isNotUsed() { return (userBits & NNOTUSED) != 0; }

	/**
	 * Routine to set this NodeProto so that it is part of a cell library.
	 * Cell libraries are those libraries that contain sets of cells for use
	 * in other circuits, and do not contain complete circuitry.
	 */
	public void setInCellLibrary() { userBits |= INCELLLIBRARY; }

	/**
	 * Routine to set this NodeProto so that it is not part of a cell library.
	 * Cell libraries are those libraries that contain sets of cells for use
	 * in other circuits, and do not contain complete circuitry.
	 */
	public void clearInCellLibrary() { userBits &= ~INCELLLIBRARY; }

	/**
	 * Routine to tell if this NodeProto is part of a cell library.
	 * Cell libraries are those libraries that contain sets of cells for use
	 * in other circuits, and do not contain complete circuitry.
	 * @return true if this NodeProto is part of a cell library.
	 */
	public boolean isInCellLibrary() { return (userBits & INCELLLIBRARY) != 0; }

	/**
	 * Routine to set this NodeProto so that it is part of a technology library.
	 * Technology libraries are those libraries that contain Cells with
	 * graphical descriptions of the nodes, arcs, and layers of a technology.
	 */
	public void setInTechnologyLibrary() { userBits |= TECEDITCELL; }

	/**
	 * Routine to set this NodeProto so that it is not part of a technology library.
	 * Technology libraries are those libraries that contain Cells with
	 * graphical descriptions of the nodes, arcs, and layers of a technology.
	 */
	public void clearInTechnologyLibrary() { userBits &= ~TECEDITCELL; }

	/**
	 * Routine to tell if this NodeProto is part of a Technology Library.
	 * Technology libraries are those libraries that contain Cells with
	 * graphical descriptions of the nodes, arcs, and layers of a technology.
	 * @return true if this NodeProto is part of a Technology Library.
	 */
	public boolean isInTechnologyLibrary() { return (userBits & TECEDITCELL) != 0; }

	/**
	 * Routine to get access to flag bits on this NodeProto.
	 * Flag bits allow NodeProtos to be marked and examined more conveniently.
	 * However, multiple competing activities may want to mark the nodes at
	 * the same time.  To solve this, each activity that wants to mark nodes
	 * must create a FlagSet that allocates bits in the node.  When done,
	 * the FlagSet must be released.
	 * @param numBits the number of flag bits desired.
	 * @return a FlagSet object that can be used to mark and test the NodeProto.
	 */
	public static FlagSet getFlagSet(int numBits) { return FlagSet.getFlagSet(flagGenerator, numBits); }

	/**
	 * Routine to free a set of flag bits on this NodeProto.
	 * Flag bits allow NodeProtos to be marked and examined more conveniently.
	 * However, multiple competing activities may want to mark the nodes at
	 * the same time.  To solve this, each activity that wants to mark nodes
	 * must create a FlagSet that allocates bits in the node.  When done,
	 * the FlagSet must be released.
	 * @param fs the flag bits that are no longer needed.
	 */
	public static void freeFlagSet(FlagSet fs) { fs.freeFlagSet(flagGenerator); }

	/**
	 * Routine to set the specified flag bits on this NodeProto.
	 * @param set the flag bits that are to be set on this NodeProto.
	 */
	public void setBit(FlagSet set) { flagBits = flagBits | set.getMask(); }

	/**
	 * Routine to set the specified flag bits on this NodeProto.
	 * @param set the flag bits that are to be cleared on this NodeProto.
	 */
	public void clearBit(FlagSet set) { flagBits = flagBits & set.getUnmask(); }

	/**
	 * Routine to test the specified flag bits on this NodeProto.
	 * @param set the flag bits that are to be tested on this NodeProto.
	 * @return true if the flag bits are set.
	 */
	public boolean isBit(FlagSet set) { return (flagBits & set.getMask()) != 0; }

	/**
	 * Routine to set an arbitrary integer in a temporary location on this NodeProto.
	 * @param tempInt the integer to be set on this NodeProto.
	 */
	public void setTempInt(int tempInt) { this.tempInt = tempInt; }

	/**
	 * Routine to get the temporary integer on this NodeProto.
	 * @return the temporary integer on this NodeProto.
	 */
	public int getTempInt() { return tempInt; }

	/**
	 * Routine to set an arbitrary Object in a temporary location on this NodeProto.
	 * @param tempObj the Object to be set on this NodeProto.
	 */
	public void setTempObj(Object tempObj) { this.tempObj = tempObj; }

	/**
	 * Routine to get the temporary Object on this NodeProto.
	 * @return the temporary Object on this NodeProto.
	 */
	public Object getTempObj() { return tempObj; }

	/**
	 * Abstract routine to return the default width of this NodeProto.
	 * Cells return the actual width of the contents.
	 * PrimitiveNodes return the default width of new instances of this NodeProto.
	 * @return the width to use when creating new NodeInsts of this NodeProto.
	 */
	public abstract double getDefWidth();

	/**
	 * Abstract routine to return the default height of this NodeProto.
	 * Cells return the actual height of the contents.
	 * PrimitiveNodes return the default height of new instances of this NodeProto.
	 * @return the height to use when creating new NodeInsts of this NodeProto.
	 */
	public abstract double getDefHeight();

	/**
	 * Abstract routine to return the Low X offset between the actual NodeProto edge and the reported/selected bound.
	 * Cells always implement this by returning zero.
	 * PrimitiveNodes may have a nonzero offset if the reported and selected size is smaller than the actual size.
	 * @return the the inset from the left edge of the reported/selected area.
	 */
	public abstract double getLowXOffset();

	/**
	 * Abstract routine to return the High X offset between the actual NodeProto edge and the reported/selected bound.
	 * Cells always implement this by returning zero.
	 * PrimitiveNodes may have a nonzero offset if the reported and selected size is smaller than the actual size.
	 * @return the the inset from the right edge of the reported/selected area.
	 */
	public abstract double getHighXOffset();

	/**
	 * Abstract routine to return the Low Y offset between the actual NodeProto edge and the reported/selected bound.
	 * Cells always implement this by returning zero.
	 * PrimitiveNodes may have a nonzero offset if the reported and selected size is smaller than the actual size.
	 * @return the the inset from the bottom edge of the reported/selected area.
	 */
	public abstract double getLowYOffset();

	/**
	 * Abstract routine to return the High Y offset between the actual NodeProto edge and the reported/selected bound.
	 * Cells always implement this by returning zero.
	 * PrimitiveNodes may have a nonzero offset if the reported and selected size is smaller than the actual size.
	 * @return the the inset from the top edge of the reported/selected area.
	 */
	public abstract double getHighYOffset();

	/**
	 * Abstract routine to return the Technology to which this NodeProto belongs.
	 * For Cells, the Technology varies with the View and contents.
	 * For PrimitiveNodes, the Technology is simply the one that owns it.
	 * @return the Technology associated with this NodeProto.
	 */
	public abstract Technology getTechnology();

	/**
	 * Routine to set the Technology to which this NodeProto belongs
	 * It can only be called for Cells because PrimitiveNodes have fixed Technology membership.
	 * @param tech the new technology for this NodeProto (Cell).
	 */
	public void setTechnology(Technology tech)
	{
		if (this instanceof Cell)
			this.tech = tech;
	}

	/**
	 * Routine to return the PortProto on this NodeProto that can connect to an arc of the specified type.
	 * The routine finds a PortProto that can make the connection.
	 * @param arc the type of arc to connect to an instance of this NodeProto.
	 * @return a PortProto that can connect to this type of ArcProto.
	 * Returns null if this ArcProto cannot connect to anything on this NodeProto.
	 */
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

	/**
	 * Routine to find the PortProto that has a particular name.
	 * @return the PortProto, or null if there is no PortProto with that name.
	 */
	public PortProto findPortProto(String name)
	{
		for (int i = 0; i < ports.size(); i++)
		{
			PortProto pp = (PortProto) ports.get(i);
			if (pp.getProtoName().equals(name))
				return pp;
		}
		return null;
	}

	/**
	 * Routine to find the NodeProto with the given name.
	 * This can be a PrimitiveNode (and can be prefixed by a Technology name),
	 * or it can be a Cell (and be prefixed by a Library name).
	 * @param line the name of the NodeProto.
	 * @return the specified NodeProto, or null if none can be found.
	 */
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

	/**
	 * Routine to return an iterator over all PortProtos of this NodeProto.
	 * @return an iterator over all PortProtos of this NodeProto.
	 */
	public Iterator getPorts()
	{
		return ports.iterator();
	}

	/**
	 * Routine to return the number of PortProtos on this NodeProto.
	 * @return the number of PortProtos on this NodeProto.
	 */
	public int getNumPorts()
	{
		return ports.size();
	}

	/**
	 * Routine to return an iterator over all instances of this NodeProto.
	 * @return an iterator over all instances of this NodeProto.
	 */
	public Iterator getInstances()
	{
		return instances.iterator();
	}

	/**
	 * Abstract routine to describe this NodeProto as a string.
	 * PrimitiveNodes may prepend their Technology name if it is
	 * not the current technology (for example, "mocmos:N-Transistor").
	 * Cells may prepend their Library if it is not the current library,
	 * and they will include view and version information
	 * (for example: "Wires:wire100{ic}").
	 * @return a String describing this NodeProto.
	 */
	public abstract String describe();

	/**
	 * Routine to return the name of this NodeProto.
	 * When this is a PrimitiveNode, the name is just its name in
	 * the Technology.
	 * When this is a Cell, the name is the pure cell name, without
	 * any view or version information.
	 * @return the prototype name of this NodeProto.
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

	/**
	 * Routine to determine whether a variable name on NodeProtos is deprecated.
	 * Deprecated variable names are those that were used in old versions of Electric,
	 * but are no longer valid.
	 * @param name the name of the variable.
	 * @return true if the variable name is deprecated.
	 */
	public boolean isdeprecatedvariable(String name)
	{
		if (name == "NET_last_good_ncc" ||
			name == "NET_last_good_ncc_facet" ||
			name == "SIM_window_signal_order") return true;
		return false;
	}

	/**
	 * Low-level routine to get the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the "user bits".
	 */
	public int lowLevelGetUserbits() { return userBits; }

	/**
	 * Low-level routine to set the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param userBits the new "user bits".
	 */
	public void lowLevelSetUserbits(int userBits) { this.userBits = userBits; }

	/**
	 * Returns a printable version of this NodeProto.
	 * @return a printable version of this NodeProto.
	 */
	public String toString()
	{
		return "NodeProto " + describe();
	}
}

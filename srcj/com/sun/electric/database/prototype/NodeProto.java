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
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.user.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
		private final Name basename;
		private final String constantName;

		private Function(String name, String shortName, String constantName)
		{
			this.name = name;
			this.shortName = shortName;
			this.constantName = constantName;
			this.basename = Name.findName(shortName+'@').getBasename();
		}

		/**
		 * Returns a name of this Function.
		 * @return a name of this Function.
		 */
		public String getName() { return name; }

		/**
		 * Returns a short name of this Function.
		 * @return a short name of this Function.
		 */
		public String getShortName() { return shortName; }

		/**
		 * Returns a base name of this Function for autonaming.
		 * @return a base name of this Function for autonaming.
		 */
		public Name getBasename() { return basename; }

		/**
		 * Returns a base name of this Function for autonaming.
		 * @return a base name of this Function for autonaming.
		 */
		public boolean isTransistor()
		{
			if (this == TRANMOS || this == TRAPMOS || this == TRADMOS ||
				this == TRA4NMOS || this == TRA4PMOS || this == TRA4DMOS ||
				this == TRANPN || this == TRAPNP || this == TRANJFET || this == TRAPJFET || this == TRAEMES || this == TRADMES ||
				this == TRA4NPN || this == TRA4PNP || this == TRA4NJFET || this == TRA4PJFET || this == TRA4EMES || this == TRA4DMES ||
				this == TRANSREF || this == TRANS || this == TRANS4) return true;
			return false;
		}

		/**
		 * Returns a printable version of this Function.
		 * @return a printable version of this Function.
		 */
		public String toString() { return name; }

		/**
		 * Describes a node with unknown behavior.
		 */
		public static final Function UNKNOWN =   new Function("unknown",						"node",     "NPUNKNOWN");
		/**
		 * Describes a single-layer pin.
		 * Pins connects wires of a single layer, have no geometry, and connect in the center of the node.
		 */
		public static final Function PIN =       new Function("pin",							"pin",      "NPPIN");
		/**
		 * Describes a two-layer contact.
		 * Contacts connects wires of two different layers in the center of the node.
		 */
		public static final Function CONTACT =   new Function("contact",						"contact",  "NPCONTACT");
		/**
		 * Describes a pure-layer node.
		 * Pure-layer nodes have a solid piece of geometry on a single layer.
		 */
		public static final Function NODE =      new Function("pure-layer-node",				"plnode",   "NPNODE");
		/**
		 * node a node that connects all ports.
		 */
		public static final Function CONNECT =   new Function("connection",					"conn",     "NPCONNECT");
		/**
		 * Describes a MOS enhancement transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port.
		 */
		public static final Function TRANMOS =   new Function("nMOS-transistor",				"nmos",     "NPTRANMOS");
		/**
		 * Describes a MOS depletion transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port.
		 */
		public static final Function TRADMOS =   new Function("DMOS-transistor",				"dmos",     "NPTRADMOS");
		/**
		 * Describes a MOS complementary transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port.
		 */
		public static final Function TRAPMOS =   new Function("pMOS-transistor",				"pmos",     "NPTRAPMOS");
		/**
		 * Describes a NPN junction transistor.
		 * It has base on the first port, emitter on the second port, and collector on the third port.
		 */
		public static final Function TRANPN =    new Function("NPN-transistor",				"npn",      "NPTRANPN");
		/**
		 * Describes a PNP junction transistor.
		 * It has base on the first port, emitter on the second port, and collector on the third port.
		 */
		public static final Function TRAPNP =    new Function("PNP-transistor",				"pnp",      "NPTRAPNP");
		/**
		 * Describes a N-channel junction transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 */
		public static final Function TRANJFET =  new Function("n-type-JFET-transistor",		"njfet",    "NPTRANJFET");
		/**
		 * Describes a P-channel junction transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 */
		public static final Function TRAPJFET =  new Function("p-type-JFET-transistor",		"pjfet",    "NPTRAPJFET");
		/**
		 * Describes a MESFET depletion transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 */
		public static final Function TRADMES =   new Function("depletion-mesfet",			"dmes",     "NPTRADMES");
		/**
		 * Describes a MESFET enhancement transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 */
		public static final Function TRAEMES =   new Function("enhancement-mesfet",			"emes",     "NPTRAEMES");
		/**
		 * Describes a general-purpose transistor.
		 * It is defined self-referentially by the prototype name of the primitive.
		 */
		public static final Function TRANSREF =  new Function("prototype-defined-transistor","tref",     "NPTRANSREF");
		/**
		 * Describes an undetermined transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 * The specific transistor type can be determined by examining the value from the NodeInst's "getTechSpecific" method.
		 */
		public static final Function TRANS =     new Function("transistor",					"trans",    "NPTRANS");
		/**
		 * Describes a 4-port MOS enhancement transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4NMOS =  new Function("4-port-nMOS-transistor",		"nmos4p",   "NPTRA4NMOS");
		/**
		 * Describes a 4-port MOS depletion transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4DMOS =  new Function("4-port-DMOS-transistor",		"dmos4p",   "NPTRA4DMOS");
		/**
		 * Describes a 4-port MOS complementary transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4PMOS =  new Function("4-port-pMOS-transistor",		"pmos4p",   "NPTRA4PMOS");
		/**
		 * Describes a 4-port NPN junction transistor.
		 * It has base on the first port, emitter on the second port, collector on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4NPN =   new Function("4-port-NPN-transistor",		"npn4p",    "NPTRA4NPN");
		/**
		 * Describes a 4-port PNP junction transistor.
		 * It has base on the first port, emitter on the second port, collector on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4PNP =   new Function("4-port-PNP-transistor",		"pnp4p",    "NPTRA4PNP");
		/**
		 * Describes a 4-port N-channel junction transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4NJFET = new Function("4-port-n-type-JFET-transistor","njfet4p", "NPTRA4NJFET");
		/**
		 * Describes a 4-port P-channel junction transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4PJFET = new Function("4-port-p-type-JFET-transistor","pjfet4p", "NPTRA4PJFET");
		/**
		 * Describes a 4-port MESFET depletion transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4DMES =  new Function("4-port-depletion-mesfet",		"dmes4p",   "NPTRA4DMES");
		/**
		 * Describes a 4-port MESFET enhancement transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4EMES =  new Function("4-port-enhancement-mesfet",	"emes4p",   "NPTRA4EMES");
		/**
		 * Describes a general-purpose transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 * The specific transistor type can be determined by examining the value from the NodeInst's "getTechSpecific" method.
		 */
		public static final Function TRANS4 =    new Function("4-port-transistor",			"trans4p",  "NPTRANS4");
		/**
		 * Describes a resistor.
		 */
		public static final Function RESIST =    new Function("resistor",					"res",      "NPRESIST");
		/**
		 * Describes a capacitor.
		 */
		public static final Function CAPAC =     new Function("capacitor",					"cap",      "NPCAPAC");
		/**
		 * Describes an electrolytic capacitor.
		 */
		public static final Function ECAPAC =    new Function("electrolytic-capacitor",		"ecap",     "NPECAPAC");
		/**
		 * Describes a diode.
		 */
		public static final Function DIODE =     new Function("diode",						"diode",    "NPDIODE");
		/**
		 * Describes a zener diode.
		 */
		public static final Function DIODEZ =    new Function("zener-diode",					"zdiode",   "NPDIODEZ");
		/**
		 * Describes an inductor.
		 */
		public static final Function INDUCT =    new Function("inductor",					"ind",      "NPINDUCT");
		/**
		 * Describes a meter.
		 */
		public static final Function METER =     new Function("meter",						"meter",    "NPMETER");
		/**
		 * Describes a transistor base.
		 */
		public static final Function BASE =      new Function("base",						"base",     "NPBASE");
		/**
		 * Describes a transistor emitter.
		 */
		public static final Function EMIT =      new Function("emitter",						"emit",     "NPEMIT");
		/**
		 * Describes a transistor collector.
		 */
		public static final Function COLLECT =   new Function("collector",					"coll",     "NPCOLLECT");
		/**
		 * Describes a buffer.
		 * It has input on the first port, clocking on the second port, and output on the third port.
		 */
		public static final Function BUFFER =    new Function("buffer",						"buf",      "NPBUFFER");
		/**
		 * Describes an AND gate.
		 * It has inputs on the first port and output on the second port.
		 */
		public static final Function GATEAND =   new Function("AND-gate",					"and",      "NPGATEAND");
		/**
		 * Describes an OR gate.
		 * It has inputs on the first port and output on the second port.
		 */
		public static final Function GATEOR =    new Function("OR-gate",						"or",       "NPGATEOR");
		/**
		 * Describes an XOR gate.
		 * It has inputs on the first port and output on the second port.
		 */
		public static final Function GATEXOR =   new Function("XOR-gate",					"xor",      "NPGATEXOR");
		/**
		 * Describes a flip-flop.
		 * The specific flip-flop type can be determined by examining the value from the NodeInst's "getTechSpecific" method.
		 */
		public static final Function FLIPFLOP =  new Function("flip-flop",					"ff",       "NPFLIPFLOP");
		/**
		 * Describes a multiplexor.
		 */
		public static final Function MUX =       new Function("multiplexor",					"mux",      "NPMUX");
		/**
		 * Describes a power connection.
		 */
		public static final Function CONPOWER =  new Function("power",						"pwr",      "NPCONPOWER");
		/**
		 * Describes a ground connection.
		 */
		public static final Function CONGROUND = new Function("ground",						"gnd",      "NPCONGROUND");
		/**
		 * Describes voltage or current source.
		 */
		public static final Function SOURCE =    new Function("source",						"source",   "NPSOURCE");
		/**
		 * Describes a substrate contact.
		 */
		public static final Function SUBSTRATE = new Function("substrate",					"substr",   "NPSUBSTRATE");
		/**
		 * Describes a well contact.
		 */
		public static final Function WELL =      new Function("well",						"well",     "NPWELL");
		/**
		 * Describes a pure artwork.
		 */
		public static final Function ART =       new Function("artwork",						"art",      "NPART");
		/**
		 * Describes an array.
		 */
		public static final Function ARRAY =     new Function("array",						"array",    "NPARRAY");
		/**
		 * Describes an alignment object.
		 */
		public static final Function ALIGN =     new Function("align",						"align",    "NPALIGN");
		/**
		 * Describes a current-controlled voltage source.
		 */
		public static final Function CCVS =      new Function("ccvs",						"ccvs",     "NPCCVS");
		/**
		 * Describes a current-controlled current source.
		 */
		public static final Function CCCS =      new Function("cccs",						"cccs",     "NPCCCS");
		/**
		 * Describes a voltage-controlled voltage source.
		 */
		public static final Function VCVS =      new Function("vcvs",						"vcvs",     "NPVCVS");
		/**
		 * Describes a voltage-controlled current source.
		 */
		public static final Function VCCS =      new Function("vccs",						"vccs",     "NPVCCS");
		/**
		 * Describes a transmission line.
		 */
		public static final Function TLINE =     new Function("transmission-line",			"transm",   "NPTLINE");
	}

	// ------------------------ private data --------------------------

	/** set if nonmanhattan instances shrink */				private static final int NODESHRINK =           01;
	/** set if instances should be expanded */				private static final int WANTNEXPAND =          02;
	/** node function (from efunction.h) */					private static final int NFUNCTION =          0774;
	/** right shift for NFUNCTION */						private static final int NFUNCTIONSH =           2;
	/** set if instances can be wiped */					private static final int ARCSWIPE =          01000;
	/** set if node is to be kept square in size */			private static final int NSQUARE =           02000;
	/** primitive can hold trace information */				private static final int HOLDSTRACE =        04000;
	/** set if this primitive can be zero-sized */			private static final int CANBEZEROSIZE =    010000;
	/** set to erase if connected to 1 or 2 arcs */			private static final int WIPEON1OR2 =       020000;
	/** set if primitive is lockable (cannot move) */		private static final int LOCKEDPRIM =       040000;
	/** set if primitive is selectable by edge, not area */	private static final int NEDGESELECT =     0100000;
	/** set if nonmanhattan arcs on this shrink */			private static final int ARCSHRINK =       0200000;
//	/** set if this cell is open in the explorer tree */	private static final int OPENINEXPLORER =  0400000;
//	/** set if this cell is open in the explorer tree */	private static final int VOPENINEXPLORER =01000000;
	/** set if not used (don't put in menu) */				private static final int NNOTUSED =       02000000;
	/** set if everything in cell is locked */				private static final int NPLOCKED =       04000000;
	/** set if instances in cell are locked */				private static final int NPILOCKED =     010000000;
	/** set if cell is part of a "cell library" */			private static final int INCELLLIBRARY = 020000000;
	/** set if cell is from a technology-library */			private static final int TECEDITCELL =   040000000;

	/** The name of the NodeProto. */						protected String protoName;
	/** This NodeProto's Technology. */						protected Technology tech;
	/** A list of PortProtos on the NodeProto. */			private List ports;
	/** A list of NodeUsages of this NodeProto. */			private List usagesOf;
	/** Internal flag bits. */								protected int userBits;
	/** The function of this NodeProto. */					private Function function;
	/** The temporary integer value. */						private int tempInt;
	/** The temporary Object. */							private Object tempObj;
	/** The temporary flag bits. */							private int flagBits;
	/** The object used to request flag bits. */			private static FlagSet.Generator flagGenerator = new FlagSet.Generator("NodeProto");

	// ----------------- protected and private methods -----------------

	/**
	 * This constructor should not be called.
	 * Use the subclass factory methods to create Cell or PrimitiveNode objects.
	 */
	protected NodeProto()
	{
		ports = new ArrayList();
		usagesOf = new ArrayList();
		function = Function.UNKNOWN;
		tempObj = null;
	}

	/**
	 * Add a PortProto to this NodeProto.
	 * Adds Exports for Cells, PrimitivePorts for PrimitiveNodes.
	 * @param port the PortProto to add to this NodeProto.
	 * @param oldPortInsts a collection of PortInsts to Undo or null.
	 */
	public void addPort(PortProto port, Collection oldPortInsts)
	{
		checkChanging();
		port.setPortIndex(ports.size());
		ports.add(port);

		// create a PortInst for every instance of this node
		if (oldPortInsts != null)
		{
			for(Iterator it = oldPortInsts.iterator(); it.hasNext(); )
			{
				PortInst pi = (PortInst)it.next();
				pi.getNodeInst().linkPortInst(pi);
			}
		} else
		{
			for(Iterator it = getInstancesOf(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.addPortInst(port);
			}
		}
	}

	/**
	 * Removes a PortProto from this NodeProto.
	 * @param port the PortProto to remove from this NodeProto.
	 * @return collection of deleted PortInsts of the PortProto.
	 */
	public Collection removePort(PortProto port)
	{
		checkChanging();
		int portIndex = port.getPortIndex();
		ports.remove(portIndex);
		for (; portIndex < ports.size(); portIndex++)
		{
			((PortProto)ports.get(portIndex)).setPortIndex(portIndex);
		}

		Collection portInsts = new ArrayList();
		// remove the PortInst from every instance of this node
		for(Iterator it = getInstancesOf(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			portInsts.add(ni.removePortInst(port));
		}

		port.setPortIndex(-1);
		return portInsts;
	}

	/**
	 * Add to the list of usages of this NodeProto.
	 * @param nu the NodeUsage which is an usage of this NodeProto.
	 */
	public void addUsageOf(NodeUsage nu)
	{
		usagesOf.add(nu);
	}

	/**
	 * Removes a NodeUsage from the list of usages of this NodeProto.
	 * @param nu the NodeUsage which is an usage of this NodeProto.
	 */
	public void removeUsageOf(NodeUsage nu)
	{
		usagesOf.remove(nu);
	}

	private class NodeInstsIterator implements Iterator
	{
		private Iterator uit;
		private NodeUsage nu;
		private int i, n;

		NodeInstsIterator()
		{
			uit = getUsagesOf();
			i = n = 0;
			while (i >= n && uit.hasNext())
			{
				nu = (NodeUsage)uit.next();
				n = nu.getNumInsts();
			}
		}

		public boolean hasNext() { return i < n; }

		public Object next()
		{
			if (i >= n) uit.next(); // throw NoSuchElementException
			NodeInst ni = nu.getInst(i);
			i++;
			while (i >= n && uit.hasNext())
			{
				nu = (NodeUsage)uit.next();
				n = nu.getNumInsts();
				i = 0;
			}
			return ni;
		}

		public void remove() { throw new UnsupportedOperationException("NodeInstsIterator.remove()"); };
	}

	/**
	 * Method to determine whether this NodeProto contains a PortProto.
	 * @param port the port being sought on this NodeProto.
	 * @return true if the port is on this NodeProto.
	 */
	boolean containsPort(PortProto port)
	{
		return ports.contains(port);
	}

	/**
	 * Method to check and repair data structure errors in this NodeProto.
	 */
	protected int checkAndRepair()
	{
		int errorCount = 0;

		for (int i = 0; i < ports.size(); i++)
		{
			PortProto pp = (PortProto)ports.get(i);
			if (pp.getPortIndex() != i)
			{
				System.out.println(this + ", " + pp + " has wrong index");
				errorCount++;
			}
		}
		return errorCount;
	}

	// ----------------------- public methods -----------------------

	/**
	 * Method to allow instances of this NodeProto to shrink.
	 * Shrinkage occurs on MOS transistors when they are connected to wires at angles that are not manhattan
	 * (the angle between the transistor and the wire is not a multiple of 90 degrees).
	 * The actual transistor must be shrunk back appropriately to prevent little tabs from emerging at the connection site.
	 * This state is only set on primitive node prototypes.
	 * If the actual NodeInst is to shrink, it must be marked with "setShortened".
	 * Note that shrinkage does not apply if there is no arc connected.
	 */
	public void setCanShrink() { checkChanging(); userBits |= NODESHRINK; }

	/**
	 * Method to prevent instances of this NodeProto from shrinking.
	 * Shrinkage occurs on MOS transistors when they are connected to wires at angles that are not manhattan
	 * (the angle between the transistor and the wire is not a multiple of 90 degrees).
	 * The actual transistor must be shrunk back appropriately to prevent little tabs from emerging at the connection site.
	 * This state is only set on primitive node prototypes.
	 * If the actual NodeInst is to shrink, it must be marked with "setShortened".
	 * Note that shrinkage does not apply if there is no arc connected.
	 */
	public void clearCanShrink() { checkChanging(); userBits &= ~NODESHRINK; }

	/**
	 * Method to tell if instances of this NodeProto can shrink.
	 * Shrinkage occurs on MOS transistors when they are connected to wires at angles that are not manhattan
	 * (the angle between the transistor and the wire is not a multiple of 90 degrees).
	 * The actual transistor must be shrunk back appropriately to prevent little tabs from emerging at the connection site.
	 * This state is only set on primitive node prototypes.
	 * If the actual NodeInst is to shrink, it must be marked with "setShortened".
	 * Note that shrinkage does not apply if there is no arc connected.
	 * @return true if instances of this NodeProto can shrink.
	 */
	public boolean canShrink() { return (userBits & NODESHRINK) != 0; }

	/**
	 * Method to set this NodeProto so that instances of it are "expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * The setting has no meaning for PrimitiveNode instances.
	 */
	public void setWantExpanded() { checkChanging(); userBits |= WANTNEXPAND; }

	/**
	 * Method to set this NodeProto so that instances of it are "not expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * The setting has no meaning for PrimitiveNode instances.
	 */
	public void clearWantExpanded() { checkChanging(); userBits &= ~WANTNEXPAND; }

	/**
	 * Method to tell if instances of it are "not expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * The setting has no meaning for PrimitiveNode instances.
	 * @return true if instances of it are "not expanded" by when created.
	 */
	public boolean isWantExpanded() { return (userBits & WANTNEXPAND) != 0; }

	/**
	 * Method to set the function of this NodeProto.
	 * The Function is a technology-independent description of the behavior of this NodeProto.
	 * @param function the new function of this NodeProto.
	 */
	public void setFunction(Function function) { checkChanging(); this.function = function; }

	/**
	 * Method to return the function of this NodeProto.
	 * The Function is a technology-independent description of the behavior of this NodeProto.
	 * @return the function of this NodeProto.
	 */
	public Function getFunction() { return function; }

	/*
	 * Method to return the function of this NodeProto, grouped according to its
	 * general function.
	 * For example, all transistors return the same value.
	 * @return the group function of this NodeProto.
	 */
	public Function getGroupFunction()
	{
		if (function == Function.TRANMOS || function == Function.TRA4NMOS ||
			function == Function.TRAPMOS || function == Function.TRA4PMOS ||
			function == Function.TRADMOS || function == Function.TRA4DMOS ||
			function == Function.TRANPN || function == Function.TRA4NPN ||
			function == Function.TRAPNP || function == Function.TRA4PNP ||
			function == Function.TRANJFET || function == Function.TRA4NJFET ||
			function == Function.TRAPJFET || function == Function.TRA4PJFET ||
			function == Function.TRADMES || function == Function.TRA4DMES ||
			function == Function.TRAEMES || function == Function.TRA4EMES ||
			function == Function.TRANS4)
				return Function.TRANS;
		if (function == Function.RESIST || function == Function.CAPAC ||
			function == Function.ECAPAC || function == Function.DIODE ||
			function == Function.DIODEZ || function == Function.INDUCT)
				return Function.INDUCT;
		if (function == Function.CCVS || function == Function.CCCS ||
			function == Function.VCVS || function == Function.VCCS ||
			function == Function.TLINE)
				return Function.TLINE;
		if (function == Function.BASE || function == Function.EMIT ||
			function == Function.COLLECT)
				return Function.COLLECT;
		if (function == Function.BUFFER || function == Function.GATEAND ||
			function == Function.GATEOR || function == Function.MUX ||
			function == Function.GATEXOR)
				return Function.GATEXOR;
		if (function == Function.CONPOWER || function == Function.CONGROUND)
			return Function.CONGROUND;
		if (function == Function.METER || function == Function.SOURCE)
			return Function.SOURCE;
		if (function == Function.SUBSTRATE || function == Function.WELL)
			return Function.WELL;
		return function;
	}

	/**
	 * Method to set this NodeProto so that instances of it are "arc-wipable".
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Therefore, pin prototypes have this state set, and when instances of the
	 * appropriate arc prototypes connect to instances of these pins, they stop being drawn.
	 * It is necessary for the arc prototype to enable wiping (with setWipable).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 * @see ArcProto#setWipable
	 * @see NodeInst#setWiped
	 */
	public void setArcsWipe() { checkChanging(); userBits |= ARCSWIPE; }

	/**
	 * Method to set this NodeProto so that instances of it are not "arc-wipable".
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Therefore, pin prototypes have this state set, and when instances of the
	 * appropriate arc prototypes connect to instances of these pins, they stop being drawn.
	 * It is necessary for the arc prototype to enable wiping (with setWipable).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 * @see ArcProto#setWipable
	 * @see NodeInst#setWiped
	 */
	public void clearArcsWipe() { checkChanging(); userBits &= ~ARCSWIPE; }

	/**
	 * Method to tell if instances of this NodeProto are "arc-wipable" by when created.
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Therefore, pin prototypes have this state set, and when instances of the
	 * appropriate arc prototypes connect to instances of these pins, they stop being drawn.
	 * It is necessary for the arc prototype to enable wiping (with setWipable).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 * @return true if instances of this NodeProto are "arc-wipable" by when created.
	 * @see ArcProto#setWipable
	 * @see NodeInst#setWiped
	 */
	public boolean isArcsWipe() { return (userBits & ARCSWIPE) != 0; }

	/**
	 * Method to set this NodeProto so that instances of it are "square".
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
	 */
	public void setSquare() { checkChanging(); userBits |= NSQUARE; }

	/**
	 * Method to set this NodeProto so that instances of it are not "square".
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
	 */
	public void clearSquare() { checkChanging(); userBits &= ~NSQUARE; }

	/**
	 * Method to tell if instances of this NodeProto are square.
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
	 * @return true if instances of this NodeProto are square.
	 */
	public boolean isSquare() { return (userBits & NSQUARE) != 0; }

	/**
	 * Method to set this NodeProto so that instances of it may hold outline information.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 */
	public void setHoldsOutline() { checkChanging(); userBits |= HOLDSTRACE; }

	/**
	 * Method to set this NodeProto so that instances of it may not hold outline information.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 */
	public void clearHoldsOutline() { checkChanging(); userBits &= ~HOLDSTRACE; }

	/**
	 * Method to tell if instances of this NodeProto can hold an outline.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 * @return true if instances of this NodeProto can hold an outline.
	 */
	public boolean isHoldsOutline() { return (userBits & HOLDSTRACE) != 0; }

	
	/**
	 * Method to set this NodeProto so that it can be zero in size.
	 * The display system uses this to eliminate zero-size nodes that cannot be that way.
	 */
	public void setCanBeZeroSize() { checkChanging(); userBits |= CANBEZEROSIZE; }

	/**
	 * Method to set this NodeProto so that it cannot be zero in size.
	 * The display system uses this to eliminate zero-size nodes that cannot be that way.
	 */
	public void clearCanBeZeroSize() { checkChanging(); userBits &= ~CANBEZEROSIZE; }

	/**
	 * Method to tell if instances of this NodeProto can be zero in size.
	 * The display system uses this to eliminate zero-size nodes that cannot be that way.
	 * @return true if instances of this NodeProto can be zero in size.
	 */
	public boolean isCanBeZeroSize() { return (userBits & CANBEZEROSIZE) != 0; }

	/**
	 * Method to set this NodeProto so that instances of it are wiped when 1 or 2 arcs connect.
	 * This is used in Schematics pins, which are not shown if 1 or 2 arcs connect, but are shown
	 * when standing alone, or when 3 or more arcs make a "T" or other connection to it.
	 */
	public void setWipeOn1or2() { checkChanging(); userBits |= WIPEON1OR2; }

	/**
	 * Method to set this NodeProto so that instances of it are not wiped when 1 or 2 arcs connect.
	 * Only Schematics pins enable this state.
	 */
	public void clearWipeOn1or2() { checkChanging(); userBits &= ~WIPEON1OR2; }

	/**
	 * Method to tell if instances of this NodeProto are wiped when 1 or 2 arcs connect.
	 * This is used in Schematics pins, which are not shown if 1 or 2 arcs connect, but are shown
	 * when standing alone, or when 3 or more arcs make a "T" or other connection to it.
	 * @return true if instances of this NodeProto are wiped when 1 or 2 arcs connect.
	 */
	public boolean isWipeOn1or2() { return (userBits & WIPEON1OR2) != 0; }

	/**
	 * Method to set this NodeProto so that instances of it are locked.
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
	 */
	public void setLockedPrim() { checkChanging(); userBits |= LOCKEDPRIM; }

	/**
	 * Method to set this NodeProto so that instances of it are not locked.
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
	 */
	public void clearLockedPrim() { checkChanging(); userBits &= ~LOCKEDPRIM; }

	/**
	 * Method to tell if instances of this NodeProto are loced.
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
	 * @return true if instances of this NodeProto are loced.
	 */
	public boolean isLockedPrim() { return (userBits & LOCKEDPRIM) != 0; }

	/**
	 * Method to set this NodeProto so that instances of it are selectable only by their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 */
	public void setEdgeSelect() { checkChanging(); userBits |= NEDGESELECT; }

	/**
	 * Method to set this NodeProto so that instances of it are not selectable only by their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 */
	public void clearEdgeSelect() { checkChanging(); userBits &= ~NEDGESELECT; }

	/**
	 * Method to tell if instances of this NodeProto are selectable on their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 * @return true if instances of this NodeProto are selectable on their edges.
	 */
	public boolean isEdgeSelect() { return (userBits & NEDGESELECT) != 0; }

	/**
	 * Method to set this NodeProto so that arcs connected to instances will shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 */
	public void setArcsShrink() { checkChanging(); userBits |= ARCSHRINK; }

	/**
	 * Method to set this NodeProto so that arcs connected to instances will not shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 */
	public void clearArcsShrink() { checkChanging(); userBits &= ~ARCSHRINK; }

	/**
	 * Method to tell if instances of this NodeProto cause arcs to shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 * @return true if instances of this NodeProto cause arcs to shrink in nonmanhattan situations.
	 */
	public boolean isArcsShrink() { return (userBits & ARCSHRINK) != 0; }

	/**
	 * Method to set this NodeProto so that everything inside of it is locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 */
	public void setAllLocked() { checkChanging(); userBits |= NPLOCKED; }

	/**
	 * Method to set this NodeProto so that everything inside of it is not locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 */
	public void clearAllLocked() { checkChanging(); userBits &= ~NPLOCKED; }

	/**
	 * Method to tell if the contents of this NodeProto are locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 * @return true if the contents of this NodeProto are locked.
	 */
	public boolean isAllLocked() { return (userBits & NPLOCKED) != 0; }

	/**
	 * Method to set this NodeProto so that all instances inside of it are locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 */
	public void setInstancesLocked() { checkChanging(); userBits |= NPILOCKED; }

	/**
	 * Method to set this NodeProto so that all instances inside of it are not locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 */
	public void clearInstancesLocked() { checkChanging(); userBits &= ~NPILOCKED; }

	/**
	 * Method to tell if the sub-instances in this NodeProto are locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 * @return true if the sub-instances in this NodeProto are locked.
	 */
	public boolean isInstancesLocked() { return (userBits & NPILOCKED) != 0; }

	/**
	 * Method to set this NodeProto so that it is not used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding primitives that the user should not use.
	 */
	public void setNotUsed() { checkChanging(); userBits |= NNOTUSED; }

	/**
	 * Method to set this NodeProto so that it is used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding primitives that the user should not use.
	 */
	public void clearNotUsed() { checkChanging(); userBits &= ~NNOTUSED; }

	/**
	 * Method to tell if this NodeProto is used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding primitives that the user should not use.
	 * @return true if this NodeProto is used.
	 */
	public boolean isNotUsed() { return (userBits & NNOTUSED) != 0; }

	/**
	 * Method to set this NodeProto so that it is part of a cell library.
	 * Cell libraries are simply libraries that contain standard cells but no hierarchy
	 * (as opposed to libraries that define a complete circuit).
	 * Certain commands exclude facets from cell libraries, so that the actual circuit hierarchy can be more clearly seen.
	 */
	public void setInCellLibrary() { checkChanging(); userBits |= INCELLLIBRARY; }

	/**
	 * Method to set this NodeProto so that it is not part of a cell library.
	 * Cell libraries are simply libraries that contain standard cells but no hierarchy
	 * (as opposed to libraries that define a complete circuit).
	 * Certain commands exclude facets from cell libraries, so that the actual circuit hierarchy can be more clearly seen.
	 */
	public void clearInCellLibrary() { checkChanging(); userBits &= ~INCELLLIBRARY; }

	/**
	 * Method to tell if this NodeProto is part of a cell library.
	 * Cell libraries are simply libraries that contain standard cells but no hierarchy
	 * (as opposed to libraries that define a complete circuit).
	 * Certain commands exclude facets from cell libraries, so that the actual circuit hierarchy can be more clearly seen.
	 * @return true if this NodeProto is part of a cell library.
	 */
	public boolean isInCellLibrary() { return (userBits & INCELLLIBRARY) != 0; }

	/**
	 * Method to set this NodeProto so that it is part of a technology library.
	 * Technology libraries are those libraries that contain Cells with
	 * graphical descriptions of the nodes, arcs, and layers of a technology.
	 */
	public void setInTechnologyLibrary() { checkChanging(); userBits |= TECEDITCELL; }

	/**
	 * Method to set this NodeProto so that it is not part of a technology library.
	 * Technology libraries are those libraries that contain Cells with
	 * graphical descriptions of the nodes, arcs, and layers of a technology.
	 */
	public void clearInTechnologyLibrary() { checkChanging(); userBits &= ~TECEDITCELL; }

	/**
	 * Method to tell if this NodeProto is part of a Technology Library.
	 * Technology libraries are those libraries that contain Cells with
	 * graphical descriptions of the nodes, arcs, and layers of a technology.
	 * @return true if this NodeProto is part of a Technology Library.
	 */
	public boolean isInTechnologyLibrary() { return (userBits & TECEDITCELL) != 0; }

	/**
	 * Method to tell if this NodeProto is icon cell which is a part of multi-part icon.
	 * @return true if this NodeProto is part of multi-part icon.
	 */
	public boolean isMultiPartIcon() { return false; }

	/**
	 * Method to get access to flag bits on this NodeProto.
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
	 * Method to set the specified flag bits on this NodeProto.
	 * @param set the flag bits that are to be set on this NodeProto.
	 */
	public void setBit(FlagSet set) { /*checkChanging();*/ flagBits = flagBits | set.getMask(); }

	/**
	 * Method to set the specified flag bits on this NodeProto.
	 * @param set the flag bits that are to be cleared on this NodeProto.
	 */
	public void clearBit(FlagSet set) { /*checkChanging();*/ flagBits = flagBits & set.getUnmask(); }

	/**
	 * Method to test the specified flag bits on this NodeProto.
	 * @param set the flag bits that are to be tested on this NodeProto.
	 * @return true if the flag bits are set.
	 */
	public boolean isBit(FlagSet set) { return (flagBits & set.getMask()) != 0; }

	/**
	 * Method to set an arbitrary integer in a temporary location on this NodeProto.
	 * @param tempInt the integer to be set on this NodeProto.
	 */
	public void setTempInt(int tempInt) { checkChanging(); this.tempInt = tempInt; }

	/**
	 * Method to get the temporary integer on this NodeProto.
	 * @return the temporary integer on this NodeProto.
	 */
	public int getTempInt() { return tempInt; }

	/**
	 * Method to set an arbitrary Object in a temporary location on this NodeProto.
	 * @param tempObj the Object to be set on this NodeProto.
	 */
	public void setTempObj(Object tempObj) { checkChanging(); this.tempObj = tempObj; }

	/**
	 * Method to get the temporary Object on this NodeProto.
	 * @return the temporary Object on this NodeProto.
	 */
	public Object getTempObj() { return tempObj; }

	/**
	 * Abstract method to return the default rotation for new instances of this NodeProto.
	 * @return the angle, in tenth-degrees to use when creating new NodeInsts of this NodeProto.
	 * If the value is 3600 or greater, it means that X should be mirrored.
	 */
	public int getDefPlacementAngle()
	{
		int defAngle = User.getNewNodeRotation();
		Variable var = getVar(User.PLACEMENT_ANGLE, Integer.class);
		if (var != null)
		{
			Integer rot = (Integer)var.getObject();
			defAngle = rot.intValue();
		}
		return defAngle;
	}

	/**
	 * Abstract method to return the default width of this NodeProto.
	 * Cells return the actual width of the contents.
	 * PrimitiveNodes return the default width of new instances of this NodeProto.
	 * @return the width to use when creating new NodeInsts of this NodeProto.
	 */
	public abstract double getDefWidth();

	/**
	 * Abstract method to return the default height of this NodeProto.
	 * Cells return the actual height of the contents.
	 * PrimitiveNodes return the default height of new instances of this NodeProto.
	 * @return the height to use when creating new NodeInsts of this NodeProto.
	 */
	public abstract double getDefHeight();

	/**
	 * Method to size offset of this Cell.
	 * @return the size offset of this Cell.  It is always zero for cells.
	 */
	public abstract SizeOffset getSizeOffset();

	/**
	 * Abstract method to return the Technology to which this NodeProto belongs.
	 * For Cells, the Technology varies with the View and contents.
	 * For PrimitiveNodes, the Technology is simply the one that owns it.
	 * @return the Technology associated with this NodeProto.
	 */
	public abstract Technology getTechnology();

	/**
	 * Method to set the Technology to which this NodeProto belongs
	 * It can only be called for Cells because PrimitiveNodes have fixed Technology membership.
	 * @param tech the new technology for this NodeProto (Cell).
	 */
	public void setTechnology(Technology tech)
	{
		if (this instanceof Cell)
			this.tech = tech;
	}

	/**
	 * Method to return the PortProto on this NodeProto that can connect to an arc of the specified type.
	 * The method finds a PortProto that can make the connection.
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
	 * Method to find the PortProto that has a particular name.
	 * @return the PortProto, or null if there is no PortProto with that name.
	 */
	public PortProto findPortProto(String name)
	{
		return findPortProto(Name.findName(name));
	}

	/**
	 * Method to find the PortProto that has a particular Name.
	 * @return the PortProto, or null if there is no PortProto with that name.
	 */
	public PortProto findPortProto(Name name)
	{
		name = name.lowerCase();
		for (int i = 0; i < ports.size(); i++)
		{
			PortProto pp = (PortProto) ports.get(i);
			if (pp.getProtoNameKey().lowerCase() == name)
				return pp;
		}
		return null;
	}

	/**
	 * Method to find the NodeProto with the given name.
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
	 * Method to return an iterator over all PortProtos of this NodeProto.
	 * @return an iterator over all PortProtos of this NodeProto.
	 */
	public Iterator getPorts()
	{
		return ports.iterator();
	}

    /**
     * Method to return the list of PortProtos.  Useful for passing
     * to Cell.rebuildNetworks()
     * @return a List of this NodeProto's PortProtos.
     */
    public List getPortsList()
    {
        return ports;
    }
    
	/**
	 * Method to return the number of PortProtos on this NodeProto.
	 * @return the number of PortProtos on this NodeProto.
	 */
	public int getNumPorts()
	{
		return ports.size();
	}

	/**
	 * Method to return the PortProto at specified position.
	 * @param portIndex specified position of PortProto.
	 * @return the PortProto at specified position..
	 */
	public final PortProto getPort(int portIndex)
	{
//		if (portIndex < 0 || portIndex >= ports.size())
//		{
//			System.out.println("Wanted port index " + portIndex + " on node " + protoName);
//			return (PortProto)ports.get(0);
//		}
		return (PortProto)ports.get(portIndex);
	}

	/**
	 * Method to check if NodeProto has isolated ports.
	 * @return true if  NodeProto has isolated ports.
	 */
	public boolean hasIsolatedPorts()
	{
		for (int i = 0; i < ports.size(); i++)
			if (getPort(i).isIsolated()) return true;
		return false;
	}

	/**
	 * Method to return an iterator over all instances of this NodeProto.
	 * @return an iterator over all instances of this NodeProto.
	 */
	public Iterator getInstancesOf()
	{
		return new NodeInstsIterator();
	}

	/**
	 * Method to return an iterator over all usages of this NodeProto.
	 * @return an iterator over all usages of this NodeProto.
	 */
	public Iterator getUsagesOf()
	{
		return usagesOf.iterator();
	}

	/**
	 * Method to check if NodeProto is a child of other NodeProto.
	 * @param granny other NodeProto
	 * @return true if this NodeProto is a child of NodeProto np.
	 */
// 	public boolean isChildOf(NodeProto granny)
// 	{
// 		if (granny == this) return true;
// 		return isChildOf(granny, new HashSet());
// 	}

	/**
	 * Recursive method to check if NodeProto is a child of other NodeProto.
	 * @param granny other NodeProto
	 * @param checked set of NodeProtos which either are beeing checked or
	 *              are surely not children of np
	 * @return true if this NodeProto is a child of NodeProto np.
	 */
// 	private boolean isChildOf(NodeProto granny, HashSet checked)
// 	{
// 		for (Iterator it = getUsagesOf(); it.hasNext(); )
// 		{
// 			NodeUsage nu = (NodeUsage)it.next();
// 			if (nu.isIconOfParent()) continue;
// 			NodeProto father = nu.getProto();
// 			if (father == granny) return true;
// 			if (checked.contains(father)) continue;
// 			checked.add(father);
// 			if (father.isChildOf(granny, checked)) return true;
// 		}
// 		return false;
// 	}

	/**
	 * Method to determine whether this NodeProto  is an icon Cell.
	 * overriden in Cell
	 * @return true if this NodeProto is an icon  Cell.
	 */
	public boolean isIcon() { return false; }

	/**
	 * Method to determine whether this NodeProto  is an icon of another Cell.
	 * overriden in Cell
	 * @param cell the other cell which this may be an icon of.
	 * @return true if this NodeProto is an icon of that other Cell.
	 */
	public boolean isIconOf(Cell cell) { return false; }

	/**
	 * Abstract method to describe this NodeProto as a string.
	 * PrimitiveNodes may prepend their Technology name if it is
	 * not the current technology (for example, "mocmos:N-Transistor").
	 * Cells may prepend their Library if it is not the current library,
	 * and they will include view and version information
	 * (for example: "Wires:wire100{ic}").
	 * @return a String describing this NodeProto.
	 */
	public abstract String describe();

	/**
	 * Method to return the name of this NodeProto.
	 * When this is a PrimitiveNode, the name is just its name in
	 * the Technology.
	 * When this is a Cell, the name is the pure cell name, without
	 * any view or version information.
	 * @return the prototype name of this NodeProto.
	 */
	public String getProtoName() { return protoName; }

	/**
	 * Method to determine whether a variable key on NodeProtos is deprecated.
	 * Deprecated variable keys are those that were used in old versions of Electric,
	 * but are no longer valid.
	 * @param key the key of the variable.
	 * @return true if the variable key is deprecated.
	 */
	public boolean isDeprecatedVariable(Variable.Key key)
	{
		String name = key.getName();
		if (name.equals("NET_last_good_ncc") ||
			name.equals("NET_last_good_ncc_facet") ||
			name.equals("SIM_window_signal_order")) return true;
		return false;
	}

	/**
	 * Low-level method to get the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the "user bits".
	 */
	public int lowLevelGetUserbits() { return userBits; }

	/**
	 * Low-level method to set the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param userBits the new "user bits".
	 */
	public void lowLevelSetUserbits(int userBits) { checkChanging(); this.userBits = userBits; }

	/**
	 * Returns a printable version of this NodeProto.
	 * @return a printable version of this NodeProto.
	 */
	public String toString()
	{
		return "NodeProto " + describe();
	}
}

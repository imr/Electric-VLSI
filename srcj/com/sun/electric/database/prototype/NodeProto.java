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
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.text.Name;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
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

		/**
		 * Describes a node with unknown behavior.
		 */
		public static final Function UNKNOWN=   new Function("unknown",						"node",     "NPUNKNOWN");
		/**
		 * Describes a single-layer pin.
		 * Pins connects wires of a single layer, have no geometry, and connect in the center of the node.
		 */
		public static final Function PIN=       new Function("pin",							"pin",      "NPPIN");
		/**
		 * Describes a two-layer contact.
		 * Contacts connects wires of two different layers in the center of the node.
		 */
		public static final Function CONTACT=   new Function("contact",						"contact",  "NPCONTACT");
		/**
		 * Describes a pure-layer node.
		 * Pure-layer nodes have a solid piece of geometry on a single layer.
		 */
		public static final Function NODE=      new Function("pure-layer-node",				"plnode",   "NPNODE");
		/**
		 * node a node that connects all ports.
		 */
		public static final Function CONNECT=   new Function("connection",					"conn",     "NPCONNECT");
		/**
		 * Describes a MOS enhancement transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port.
		 */
		public static final Function TRANMOS=   new Function("nMOS-transistor",				"nmos",     "NPTRANMOS");
		/**
		 * Describes a MOS depletion transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port.
		 */
		public static final Function TRADMOS=   new Function("DMOS-transistor",				"dmos",     "NPTRADMOS");
		/**
		 * Describes a MOS complementary transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port.
		 */
		public static final Function TRAPMOS=   new Function("pMOS-transistor",				"pmos",     "NPTRAPMOS");
		/**
		 * Describes a NPN junction transistor.
		 * It has base on the first port, emitter on the second port, and collector on the third port.
		 */
		public static final Function TRANPN=    new Function("NPN-transistor",				"npn",      "NPTRANPN");
		/**
		 * Describes a PNP junction transistor.
		 * It has base on the first port, emitter on the second port, and collector on the third port.
		 */
		public static final Function TRAPNP=    new Function("PNP-transistor",				"pnp",      "NPTRAPNP");
		/**
		 * Describes a N-channel junction transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 */
		public static final Function TRANJFET=  new Function("n-type-JFET-transistor",		"njfet",    "NPTRANJFET");
		/**
		 * Describes a P-channel junction transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 */
		public static final Function TRAPJFET=  new Function("p-type-JFET-transistor",		"pjfet",    "NPTRAPJFET");
		/**
		 * Describes a MESFET depletion transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 */
		public static final Function TRADMES=   new Function("depletion-mesfet",			"dmes",     "NPTRADMES");
		/**
		 * Describes a MESFET enhancement transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 */
		public static final Function TRAEMES=   new Function("enhancement-mesfet",			"emes",     "NPTRAEMES");
		/**
		 * Describes a general-purpose transistor.
		 * It is defined self-referentially by the prototype name of the primitive.
		 */
		public static final Function TRANSREF=  new Function("prototype-defined-transistor","tref",     "NPTRANSREF");
		/**
		 * Describes an undetermined transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 * The specific transistor type can be determined by examining the value from the NodeInst's "getTechSpecific" routine.
		 */
		public static final Function TRANS=     new Function("transistor",					"trans",    "NPTRANS");
		/**
		 * Describes a 4-port MOS enhancement transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4NMOS=  new Function("4-port-nMOS-transistor",		"nmos4p",   "NPTRA4NMOS");
		/**
		 * Describes a 4-port MOS depletion transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4DMOS=  new Function("4-port-DMOS-transistor",		"dmos4p",   "NPTRA4DMOS");
		/**
		 * Describes a 4-port MOS complementary transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4PMOS=  new Function("4-port-pMOS-transistor",		"pmos4p",   "NPTRA4PMOS");
		/**
		 * Describes a 4-port NPN junction transistor.
		 * It has base on the first port, emitter on the second port, collector on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4NPN=   new Function("4-port-NPN-transistor",		"npn4p",    "NPTRA4NPN");
		/**
		 * Describes a 4-port PNP junction transistor.
		 * It has base on the first port, emitter on the second port, collector on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4PNP=   new Function("4-port-PNP-transistor",		"pnp4p",    "NPTRA4PNP");
		/**
		 * Describes a 4-port N-channel junction transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4NJFET= new Function("4-port-n-type-JFET-transistor","njfet4p", "NPTRA4NJFET");
		/**
		 * Describes a 4-port P-channel junction transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4PJFET= new Function("4-port-p-type-JFET-transistor","pjfet4p", "NPTRA4PJFET");
		/**
		 * Describes a 4-port MESFET depletion transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4DMES=  new Function("4-port-depletion-mesfet",		"dmes4p",   "NPTRA4DMES");
		/**
		 * Describes a 4-port MESFET enhancement transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		public static final Function TRA4EMES=  new Function("4-port-enhancement-mesfet",	"emes4p",   "NPTRA4EMES");
		/**
		 * Describes a general-purpose transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 * The specific transistor type can be determined by examining the value from the NodeInst's "getTechSpecific" routine.
		 */
		public static final Function TRANS4=    new Function("4-port-transistor",			"trans4p",  "NPTRANS4");
		/**
		 * Describes a resistor.
		 */
		public static final Function RESIST=    new Function("resistor",					"res",      "NPRESIST");
		/**
		 * Describes a capacitor.
		 */
		public static final Function CAPAC=     new Function("capacitor",					"cap",      "NPCAPAC");
		/**
		 * Describes an electrolytic capacitor.
		 */
		public static final Function ECAPAC=    new Function("electrolytic-capacitor",		"ecap",     "NPECAPAC");
		/**
		 * Describes a diode.
		 */
		public static final Function DIODE=     new Function("diode",						"diode",    "NPDIODE");
		/**
		 * Describes a zener diode.
		 */
		public static final Function DIODEZ=    new Function("zener-diode",					"zdiode",   "NPDIODEZ");
		/**
		 * Describes an inductor.
		 */
		public static final Function INDUCT=    new Function("inductor",					"ind",      "NPINDUCT");
		/**
		 * Describes a meter.
		 */
		public static final Function METER=     new Function("meter",						"meter",    "NPMETER");
		/**
		 * Describes a transistor base.
		 */
		public static final Function BASE=      new Function("base",						"base",     "NPBASE");
		/**
		 * Describes a transistor emitter.
		 */
		public static final Function EMIT=      new Function("emitter",						"emit",     "NPEMIT");
		/**
		 * Describes a transistor collector.
		 */
		public static final Function COLLECT=   new Function("collector",					"coll",     "NPCOLLECT");
		/**
		 * Describes a buffer.
		 * It has input on the first port, clocking on the second port, and output on the third port.
		 */
		public static final Function BUFFER=    new Function("buffer",						"buf",      "NPBUFFER");
		/**
		 * Describes an AND gate.
		 * It has inputs on the first port and output on the second port.
		 */
		public static final Function GATEAND=   new Function("AND-gate",					"and",      "NPGATEAND");
		/**
		 * Describes an OR gate.
		 * It has inputs on the first port and output on the second port.
		 */
		public static final Function GATEOR=    new Function("OR-gate",						"or",       "NPGATEOR");
		/**
		 * Describes an XOR gate.
		 * It has inputs on the first port and output on the second port.
		 */
		public static final Function GATEXOR=   new Function("XOR-gate",					"xor",      "NPGATEXOR");
		/**
		 * Describes a flip-flop.
		 * The specific flip-flop type can be determined by examining the value from the NodeInst's "getTechSpecific" routine.
		 */
		public static final Function FLIPFLOP=  new Function("flip-flop",					"ff",       "NPFLIPFLOP");
		/**
		 * Describes a multiplexor.
		 */
		public static final Function MUX=       new Function("multiplexor",					"mux",      "NPMUX");
		/**
		 * Describes a power connection.
		 */
		public static final Function CONPOWER=  new Function("power",						"pwr",      "NPCONPOWER");
		/**
		 * Describes a ground connection.
		 */
		public static final Function CONGROUND= new Function("ground",						"gnd",      "NPCONGROUND");
		/**
		 * Describes voltage or current source.
		 */
		public static final Function SOURCE=    new Function("source",						"source",   "NPSOURCE");
		/**
		 * Describes a substrate contact.
		 */
		public static final Function SUBSTRATE= new Function("substrate",					"substr",   "NPSUBSTRATE");
		/**
		 * Describes a well contact.
		 */
		public static final Function WELL=      new Function("well",						"well",     "NPWELL");
		/**
		 * Describes a pure artwork.
		 */
		public static final Function ART=       new Function("artwork",						"art",      "NPART");
		/**
		 * Describes an array.
		 */
		public static final Function ARRAY=     new Function("array",						"array",    "NPARRAY");
		/**
		 * Describes an alignment object.
		 */
		public static final Function ALIGN=     new Function("align",						"align",    "NPALIGN");
		/**
		 * Describes a current-controlled voltage source.
		 */
		public static final Function CCVS=      new Function("ccvs",						"ccvs",     "NPCCVS");
		/**
		 * Describes a current-controlled current source.
		 */
		public static final Function CCCS=      new Function("cccs",						"cccs",     "NPCCCS");
		/**
		 * Describes a voltage-controlled voltage source.
		 */
		public static final Function VCVS=      new Function("vcvs",						"vcvs",     "NPVCVS");
		/**
		 * Describes a voltage-controlled current source.
		 */
		public static final Function VCCS=      new Function("vccs",						"vccs",     "NPVCCS");
		/**
		 * Describes a transmission line.
		 */
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
	/** A list of NodeUsages of this NodeProto. */			private List usagesOf;
	/** Internal flag bits. */								protected int userBits;
	/** The function of this NodeProto. */					private Function function;

	/**
     * Equivalence of ports. equivPorts.size == ports.size.
	 * equivPorts[i] contains minimal index among ports of its group.
     */														private int[] equivPorts;
	/** Time stamp when @equivPorts map was modified */		private int equivPortsUpdateTime;
	/** Time stamp when @equivPorts map was checked */		private int equivPortsCheckTime;

	/** The temporary integer value. */						private int tempInt;
	/** The temporary Object. */							private Object tempObj;
	/** The temporary flag bits. */							private int flagBits;
	/** The object used to request flag bits. */			private static FlagSet.Generator flagGenerator = new FlagSet.Generator();

	// ----------------- protected and private methods -----------------

	/**
	 * This constructor should not be called.
	 * Use the subclass factory methods to create Cell or PrimitiveNode objects.
	 */
	protected NodeProto()
	{
		ports = new ArrayList();
		usagesOf = new ArrayList();
		networks = new ArrayList();
		function = Function.UNKNOWN;
		equivPortsUpdateTime = equivPortsCheckTime = 0;
		tempObj = null;
	}

	/**
	 * Add a PortProto to this NodeProto.
	 * Adds Exports for Cells, PrimitivePorts for PrimitiveNodes.
	 * @param port the PortProto to add to this NodeProto.
	 */
	public void addPort(PortProto port)
	{
		ports.add(port);
		notifyCellsNetworks();
	}

	/**
	 * Removes a PortProto from this NodeProto.
	 * @param port the PortProto to remove from this NodeProto.
	 */
	public void removePort(PortProto port)
	{
		ports.remove(port);
		notifyCellsNetworks();
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
	 * Remove this NodeProto.
	 * Also deletes the ports associated with this NodeProto.
	 */
	public void kill()
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

	/**
	 * Notify all cells in which this NodeProto is the instance
	 * about modification of interface of this NodeProto.
	 */
	private void notifyCellsNetworks()
	{
		if (this instanceof Cell) ((Cell)this).setNetworksDirty();
		for (Iterator it = getUsagesOf(); it.hasNext();)
		{
			NodeUsage nu = (NodeUsage) it.next();
			nu.getParent().setNetworksDirty();
		}
	}

	/**
	 * Merge classes of equivalence map to which elements a1 and a2 belong.
	 */
	protected static void connectMap(int[] map, int a1, int a2)
	{
		int m1, m2, m;

		for (m1 = a1; map[m1] != m1; m1 = map[m1]);
		for (m2 = a2; map[m2] != m2; m2 = map[m2]);
		m = m1 < m2 ? m1 : m2;

		for (;;)
		{
			int k = map[a1];
			map[a1] = m;
			if (a1 == k) break;
			a1 = k;
		}
		for (;;)
		{
			int k = map[a2];
			map[a2] = m;
			if (a2 == k) break;
			a2 = k;
		}
	}

	/**
	 * Obtain canonical representation of equivalence map.
	 */
	private static void closureMap(int[] map)
	{
		for (int i = 0; i < map.length; i++)
		{
			map[i] = map[map[i]];
		}
	}

	/**
	 * Obtain canonical representation of equivalence map.
	 */
	private static boolean equalMaps(int[] map1, int[] map2)
	{
		if (map1.length != map2.length) return false;
		for (int i = 0; i < map1.length; i++)
		{
			if (map1[i] != map2[i]) return false;
		}
		return true;
	}

	/**
	 * Add connections inside node proto to map of equivalent ports newEquivPort.
	 * Assume that each PortProto.getTempInt() contains sequential index of
     * port in this NodeProto.
	 */
	protected abstract void connectEquivPorts(int[] newEquivPorrs);

	/**
	 * Update map of equivalent ports newEquivPort.
	 * @param userEquivPorts HashMap (PortProto -> JNetwork) of user-specified equivalent ports
	 * @param currentTime time stamp of current network reevaluation
	 * This routine will always set equivPortsCheckTime to currentTime.
     * equivPortsUpdateTime will either change to currentTime if map will change,
	 * or will be kept untouched if not.
	 */
	public void updateEquivPorts(HashMap userEquivPorts, int currentTime)
	{
		int[] newEquivPorts = new int[ports.size()];
		int i;

		/* Initialize new equiv map. Mark PortProto's . */
		i = 0;
		for (Iterator it = ports.iterator(); it.hasNext(); i++)
		{
			PortProto pp = (PortProto)it.next();
			pp.setTempInt(i);
			newEquivPorts[i] = i;
		}

		/* Connect ports connected by node proto subnets */
		connectEquivPorts(newEquivPorts);

		/* Connect user equivalent ports */
		HashMap listToPort = new HashMap(); // equivList -> Integer
		i = 0;
		for (Iterator it = ports.iterator(); it.hasNext(); i++)
		{
			PortProto pp = (PortProto) it.next();
			Object equivList = userEquivPorts.get(pp.getEquivalent());
			if (equivList != null)
			{
				Integer iOld = (Integer) listToPort.get(equivList);
				if (iOld != null)
				{
					connectMap(newEquivPorts, iOld.intValue(), i);
				} else
				{
					listToPort.put(equivList, new Integer(i));
				}
			}
		}

		closureMap(newEquivPorts);

		/* set time stamps */
		if (equivPorts == null || !equalMaps(equivPorts, newEquivPorts))
		{
			equivPorts = newEquivPorts;
			equivPortsUpdateTime = currentTime;
		}
		equivPortsCheckTime = currentTime;
	}

	/**
	 * Show map of equivalent ports newEquivPort.
	 * @param userEquivMap HashMap (PortProto -> JNetwork) of user-specified equivalent ports
	 * @param currentTime.time stamp of current network reevaluation
	 * This routine will always set equivPortsCheckTime to currentTime.
     * equivPortsUpdateTime will either change to currentTime if map will change,
	 * or will be kept untouched if not.
	 */
	private void showEquivPorts()
	{
		System.out.println("Equivalent ports of "+this+" updateTime="+equivPortsUpdateTime+" checkTime="+equivPortsCheckTime);
		String s = "\t";
		for (int i = 0; i < equivPorts.length; i++)
		{
			if (equivPorts[i] != i) continue;
			PortProto pi = (PortProto)ports.get(i);
			boolean found = false;
			for (int j = i+1; j < equivPorts.length; j++)
			{
				if (equivPorts[i] != equivPorts[j]) continue;
				PortProto pj = (PortProto)ports.get(j);
				if (!found) s = s+" ( "+pi.getProtoName();
				found = true;
				s = s+" "+pj.getProtoName();
			}
			if (found)
				s = s+")";
			else
				s = s+" "+pi.getProtoName();
		}
		System.out.println(s);
	}

	// ----------------------- public methods -----------------------

	/**
	 * Routine to allow instances of this NodeProto to shrink.
	 * Shrinkage occurs on MOS transistors when they are connected to wires at angles that are not manhattan
	 * (the angle between the transistor and the wire is not a multiple of 90 degrees).
	 * The actual transistor must be shrunk back appropriately to prevent little tabs from emerging at the connection site.
	 * This state is only set on primitive node prototypes.
	 * If the actual NodeInst is to shrink, it must be marked with "setShortened".
	 * Note that shrinkage does not apply if there is no arc connected.
	 */
	public void setCanShrink() { userBits |= NODESHRINK; }

	/**
	 * Routine to prevent instances of this NodeProto from shrinking.
	 * Shrinkage occurs on MOS transistors when they are connected to wires at angles that are not manhattan
	 * (the angle between the transistor and the wire is not a multiple of 90 degrees).
	 * The actual transistor must be shrunk back appropriately to prevent little tabs from emerging at the connection site.
	 * This state is only set on primitive node prototypes.
	 * If the actual NodeInst is to shrink, it must be marked with "setShortened".
	 * Note that shrinkage does not apply if there is no arc connected.
	 */
	public void clearCanShrink() { userBits &= ~NODESHRINK; }

	/**
	 * Routine to tell if instances of this NodeProto can shrink.
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
	 * Routine to set this NodeProto so that instances of it are "expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * The setting has no meaning for PrimitiveNode instances.
	 */
	public void setWantExpanded() { userBits |= WANTNEXPAND; }

	/**
	 * Routine to set this NodeProto so that instances of it are "not expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * The setting has no meaning for PrimitiveNode instances.
	 */
	public void clearWantExpanded() { userBits &= ~WANTNEXPAND; }

	/**
	 * Routine to tell if instances of it are "not expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * The setting has no meaning for PrimitiveNode instances.
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
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Therefore, pin prototypes have this state set, and when instances of the
	 * appropriate arc prototypes connect to instances of these pins, they stop being drawn.
	 * It is necessary for the arc prototype to enable wiping (with setWipable).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 * @see ArcProto#setWipable
	 * @see NodeInst#setWiped
	 */
	public void setArcsWipe() { userBits |= ARCSWIPE; }

	/**
	 * Routine to set this NodeProto so that instances of it are not "arc-wipable".
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Therefore, pin prototypes have this state set, and when instances of the
	 * appropriate arc prototypes connect to instances of these pins, they stop being drawn.
	 * It is necessary for the arc prototype to enable wiping (with setWipable).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 * @see ArcProto#setWipable
	 * @see NodeInst#setWiped
	 */
	public void clearArcsWipe() { userBits &= ~ARCSWIPE; }

	/**
	 * Routine to tell if instances of this NodeProto are "arc-wipable" by when created.
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
	 * Routine to set this NodeProto so that instances of it are "square".
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
	 */
	public void setSquare() { userBits |= NSQUARE; }

	/**
	 * Routine to set this NodeProto so that instances of it are not "square".
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
	 */
	public void clearSquare() { userBits &= ~NSQUARE; }

	/**
	 * Routine to tell if instances of this NodeProto are square.
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
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
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
	 */
	public void setLockedPrim() { userBits |= LOCKEDPRIM; }

	/**
	 * Routine to set this NodeProto so that instances of it are not locked.
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
	 */
	public void clearLockedPrim() { userBits &= ~LOCKEDPRIM; }

	/**
	 * Routine to tell if instances of this NodeProto are loced.
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
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
	 * Routine to set this NodeProto so that arcs connected to instances will shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 */
	public void setArcsShrink() { userBits |= ARCSHRINK; }

	/**
	 * Routine to set this NodeProto so that arcs connected to instances will not shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 */
	public void clearArcsShrink() { userBits &= ~ARCSHRINK; }

	/**
	 * Routine to tell if instances of this NodeProto cause arcs to shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 * @return true if instances of this NodeProto cause arcs to shrink in nonmanhattan situations.
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
	 * The state is useful for hiding primitives that the user should not use.
	 */
	public void setNotUsed() { userBits |= NNOTUSED; }

	/**
	 * Routine to set this NodeProto so that it is used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding primitives that the user should not use.
	 */
	public void clearNotUsed() { userBits &= ~NNOTUSED; }

	/**
	 * Routine to tell if this NodeProto is used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding primitives that the user should not use.
	 * @return true if this NodeProto is used.
	 */
	public boolean isNotUsed() { return (userBits & NNOTUSED) != 0; }

	/**
	 * Routine to set this NodeProto so that it is part of a cell library.
	 * Cell libraries are simply libraries that contain standard cells but no hierarchy
	 * (as opposed to libraries that define a complete circuit).
	 * Certain commands exclude facets from cell libraries, so that the actual circuit hierarchy can be more clearly seen.
	 */
	public void setInCellLibrary() { userBits |= INCELLLIBRARY; }

	/**
	 * Routine to set this NodeProto so that it is not part of a cell library.
	 * Cell libraries are simply libraries that contain standard cells but no hierarchy
	 * (as opposed to libraries that define a complete circuit).
	 * Certain commands exclude facets from cell libraries, so that the actual circuit hierarchy can be more clearly seen.
	 */
	public void clearInCellLibrary() { userBits &= ~INCELLLIBRARY; }

	/**
	 * Routine to tell if this NodeProto is part of a cell library.
	 * Cell libraries are simply libraries that contain standard cells but no hierarchy
	 * (as opposed to libraries that define a complete circuit).
	 * Certain commands exclude facets from cell libraries, so that the actual circuit hierarchy can be more clearly seen.
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
	 * Routine to size offset of this Cell.
	 * @return the size offset of this Cell.  It is always zero for cells.
	 */
	public abstract SizeOffset getSizeOffset();

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
		return findPortProto(Name.findName(name));
	}

	/**
	 * Routine to find the PortProto that has a particular Name.
	 * @return the PortProto, or null if there is no PortProto with that name.
	 */
	public PortProto findPortProto(Name name)
	{
		for (int i = 0; i < ports.size(); i++)
		{
			PortProto pp = (PortProto) ports.get(i);
			if (pp.getProtoNameLow() == name)
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
	 * Routine to set tempInt of every PortProto of this NodeProto to
	 * sequential index of PortProto.
	 */
	public void numeratePorts()
	{
		int i = 0;
		for (Iterator it = ports.iterator(); it.hasNext();)
		{
			PortProto pp = (PortProto)it.next();
			pp.setTempInt(i++);
		}
	}

	/**
	 * Routine to return an iterator over all instances of this NodeProto.
	 * @return an iterator over all instances of this NodeProto.
	 */
	public Iterator getInstancesOf()
	{
		return new NodeInstsIterator();
	}

	/**
	 * Routine to return an iterator over all usages of this NodeProto.
	 * @return an iterator over all usages of this NodeProto.
	 */
	public Iterator getUsagesOf()
	{
		return usagesOf.iterator();
	}

	/**
	 * Routine to check if NodeProto is a child of other NodeProto.
	 * @param granny other NodeProto
	 * @return true if this NodeProto is a child of NodeProto np.
	 */
// 	public boolean isChildOf(NodeProto granny)
// 	{
// 		if (granny == this) return true;
// 		return isChildOf(granny, new HashSet());
// 	}

	/**
	 * Recursive routine to check if NodeProto is a child of other NodeProto.
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
	 * Routine to determine whether this NodeProto  is an icon of another Cell.
	 * overriden in Cell
	 * @param cell the other cell which this may be an icon of.
	 * @return true if this NodeProto is an icon of that other Cell.
	 */
	public boolean isIconOf(Cell cell) { return false; }

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
	public boolean isDeprecatedVariable(String name)
	{
		if (name.equals("NET_last_good_ncc") ||
			name.equals("NET_last_good_ncc_facet") ||
			name.equals("SIM_window_signal_order")) return true;
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
	 * Routine to return map of equivalent ports of this NodeProto.
	 */
    public int[] getEquivPorts() { return equivPorts; }

	/**
	 * Routime to return time stamp when map of equivalent ports of
	 * this NodeProto was modified.
	 */
	public int getEquivPortsUpdateTime() { return equivPortsUpdateTime; }

	/**
	 * Routime to return time stamp when map of equivalent ports of
	 * this NodeProto was checked.
	 */
	public int getEquivPortsCheckTime() { return equivPortsCheckTime; }

	/**
	 * Returns a printable version of this NodeProto.
	 * @return a printable version of this NodeProto.
	 */
	public String toString()
	{
		return "NodeProto " + describe();
	}
}

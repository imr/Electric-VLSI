/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PortProto.java
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

import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.network.Networkable;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.PrimitivePort;

import java.util.HashMap;

/**
 * A PortProto object lives at the PrimitiveNode level as a PrimitivePort,
 * or at the Cell level as an Export.  A PortProto has a descriptive name,
 * which may be overridden by another PortProto at a higher level in the
 * schematic or layout hierarchy.  PortProtos are expressed at the instance
 * level by Connections.
 */

public abstract class PortProto extends ElectricObject implements Networkable
{
	/** angle of this port from node center */			public static final int PORTANGLE =               0777;
	/** right shift of PORTANGLE field */				public static final int PORTANGLESH =                0;
	/** range of valid angles about port angle */		public static final int PORTARANGE =           0377000;
	/** right shift of PORTARANGE field */				public static final int PORTARANGESH =               9;
	/** electrical net of primitive port (0-30) */		public static final int PORTNET =           0177400000;
           /* 31 stands for one-port net */
	/** right shift of PORTNET field */					public static final int PORTNETSH =                 17;
	/** set if arcs to this port do not connect */		public static final int PORTISOLATED =      0200000000;
	/** set if this port should always be drawn */		public static final int PORTDRAWN =         0400000000;
	/** set to exclude this port from the icon */		public static final int BODYONLY =         01000000000;
	/** input/output/power/ground/clock state */		public static final int STATEBITS =       036000000000;
	/** right shift of STATEBITS */						public static final int STATEBITSSH =               27;
	/** un-phased clock port */							public static final int CLKPORT =          02000000000;
	/** clock phase 1 */								public static final int C1PORT =           04000000000;
	/** clock phase 2 */								public static final int C2PORT =           06000000000;
	/** clock phase 3 */								public static final int C3PORT =          010000000000;
	/** clock phase 4 */								public static final int C4PORT =          012000000000;
	/** clock phase 5 */								public static final int C5PORT =          014000000000;
	/** clock phase 6 */								public static final int C6PORT =          016000000000;
	/** input port */									public static final int INPORT =          020000000000;
	/** output port */									public static final int OUTPORT =         022000000000;
	/** bidirectional port */							public static final int BIDIRPORT =       024000000000;
	/** power port */									public static final int PWRPORT =         026000000000;
	/** ground port */									public static final int GNDPORT =         030000000000;
	/** bias-level reference output port */				public static final int REFOUTPORT =      032000000000;
	/** bias-level reference input port */				public static final int REFINPORT =       034000000000;
	/** bias-level reference base port */				public static final int REFBASEPORT =     036000000000;

	/**
	 * Characteristic is a typesafe enum class that describes the characteristics of a portproto.
	 */
	public static class Characteristic
	{
		private final String name;
		private final int    bits;

		private static HashMap characteristicList = new HashMap();

		private Characteristic(String name, int bits)
		{
			this.name = name;
			this.bits = bits;
			characteristicList.put(new Integer(bits), this);
		}
		public int getBits() { return bits; }
		public static Characteristic findCharacteristic(int bits)
		{
			Object obj = characteristicList.get(new Integer(bits));
			if (obj == null) return null;
			return (Characteristic)obj;
		}

		public String toString() { return name; }

		/**   unknown port */						public static final Characteristic UNKNOWN = new Characteristic("unknown", 0);
		/**   un-phased clock port */				public static final Characteristic CLK = new Characteristic("clock", CLKPORT);
		/**   clock phase 1 */						public static final Characteristic C1 = new Characteristic("clock1", C1PORT);
		/**   clock phase 2 */						public static final Characteristic C2 = new Characteristic("clock2", C2PORT);
		/**   clock phase 3 */						public static final Characteristic C3 = new Characteristic("clock3", C3PORT);
		/**   clock phase 4 */						public static final Characteristic C4 = new Characteristic("clock4", C4PORT);
		/**   clock phase 5 */						public static final Characteristic C5 = new Characteristic("clock5", C5PORT);
		/**   clock phase 6 */						public static final Characteristic C6 = new Characteristic("clock6", C6PORT);
		/**   input port */							public static final Characteristic IN = new Characteristic("input", INPORT);
		/**   output port */						public static final Characteristic OUT = new Characteristic("output", OUTPORT);
		/**   bidirectional port */					public static final Characteristic BIDIR = new Characteristic("bidirectional", BIDIRPORT);
		/**   power port */							public static final Characteristic PWR = new Characteristic("power", PWRPORT);
		/**   ground port */						public static final Characteristic GND = new Characteristic("ground", GNDPORT);
		/**   bias-level reference output port */	public static final Characteristic REFOUT = new Characteristic("refout", REFOUTPORT);
		/**   bias-level reference input port */	public static final Characteristic REFIN = new Characteristic("refin", REFINPORT);
		/**   bias-level reference base port */		public static final Characteristic REFBASE = new Characteristic("refbase", REFBASEPORT);
	}

	// ------------------------ private data --------------------------

	/** port name */								protected String protoName;
	/** flag bits */								protected int userBits;
	/** parent NodeProto */							protected NodeProto parent;
	/** Text descriptor */							protected TextDescriptor descriptor;
	/** temporary integer value */					protected int tempInt;
	

	/** Network that this port belongs to (in case two ports are permanently
	 * connected, like the two ends of the gate in a MOS transistor.
	 * The Network node pointed to here does not have any connections itself;
	 * it just serves as a common marker. */
	private JNetwork network;

	// ----------------------- public constants -----------------------

	// ------------------ protected and private methods ---------------------

	protected PortProto()
	{
		this.parent = null;
		this.network = null;
		this.userBits = 0;
		this.descriptor = new TextDescriptor();
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

	/** Get the Text Descriptor associated with this port. */
	public TextDescriptor getTextDescriptor() { return descriptor; }

	/** Get the Text Descriptor associated with this port. */
	public void setTextDescriptor(TextDescriptor descriptor) { this.descriptor = descriptor; }

	public Characteristic getCharacteristic()
	{
		Characteristic characteristic = Characteristic.findCharacteristic(userBits&STATEBITS);
		return characteristic;
	}


	public void setCharacteristic(Characteristic characteristic)
	{
		userBits = (userBits & ~STATEBITS) | characteristic.getBits();
	}

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

	/** Low-level routine to get the user bits.  Should not normally be called. */
	public int lowLevelGetUserbits() { return userBits; }
	/** Low-level routine to set the user bits.  Should not normally be called. */
	public void lowLevelSetUserbits(int userBits) { this.userBits = userBits; }

	public int getTempInt() { return tempInt; }
	public void setTempInt(int tempInt) { this.tempInt = tempInt; }

	public String toString()
	{
		return "PortProto " + protoName;
	}
}

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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.PrimitivePort;

import java.awt.Color;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

/**
 * The PortProto class defines a type of PortInst.
 * It is an abstract class that can be implemented as PrimitivePort (for primitives from Technologies)
 * or as Export (for cells in Libraries).
 * <P>
 * Every port in the database appears as one <I>prototypical</I> object and many <I>instantiative</I> objects.
 * Thus, for a PrimitivePort such as the "poly-left" port on a CMOS P-transistor there is one object (called a PrimitivePort, which is a PortProto)
 * that describes the transistor prototype and there are many objects (called PortInsts),
 * one for every port on an instance of that transistor in a circuit.
 * Similarly, for every Export, there is one object (called an Export, which is a PortProto)
 * that describes the Export on a Cell and there are many objects (also called PortInsts)
 * for every port on a use of that Cell in some other Cell.
 * PrimitivePorts are statically created and placed in the Technology objects,
 * but complex Exports are created by the tools and placed in Library objects.
 * <P>
 * A PortProto has a descriptive name, which may be overridden by another PortProto
 * at a higher level in the schematic or layout hierarchy.
 * The PortProto also has a parent cell, characteristics, and more.
 */
public abstract class PortProto extends ElectricObject
{
	/** angle of this port from node center */			private static final int PORTANGLE =               0777;
	/** right shift of PORTANGLE field */				private static final int PORTANGLESH =                0;
	/** range of valid angles about port angle */		private static final int PORTARANGE =           0377000;
	/** right shift of PORTARANGE field */				private static final int PORTARANGESH =               9;
	/** electrical net of primitive port (0-30) */		private static final int PORTNET =           0177400000;
           /* 31 stands for one-port net */
	/** right shift of PORTNET field */					private static final int PORTNETSH =                 17;
	/** set if arcs to this port do not connect */		private static final int PORTISOLATED =      0200000000;
	/** set if this port should always be drawn */		private static final int PORTDRAWN =         0400000000;
	/** set to exclude this port from the icon */		private static final int BODYONLY =         01000000000;
	/** input/output/power/ground/clock state */		private static final int STATEBITS =       036000000000;
	/** input/output/power/ground/clock state */		private static final int STATEBITSSHIFTED =         036;
	/** input/output/power/ground/clock state */		private static final int STATEBITSSH =               27;
	/** un-phased clock port */							private static final int CLKPORT =                    2;
	/** clock phase 1 */								private static final int C1PORT =                     4;
	/** clock phase 2 */								private static final int C2PORT =                     6;
	/** clock phase 3 */								private static final int C3PORT =                     8;
	/** clock phase 4 */								private static final int C4PORT =                    10;
	/** clock phase 5 */								private static final int C5PORT =                    12;
	/** clock phase 6 */								private static final int C6PORT =                    14;
	/** input port */									private static final int INPORT =                    16;
	/** output port */									private static final int OUTPORT =                   18;
	/** bidirectional port */							private static final int BIDIRPORT =                 20;
	/** power port */									private static final int PWRPORT =                   22;
	/** ground port */									private static final int GNDPORT =                   24;
	/** bias-level reference output port */				private static final int REFOUTPORT =                26;
	/** bias-level reference input port */				private static final int REFINPORT =                 28;
	/** bias-level reference base port */				private static final int REFBASEPORT =               30;

	/**
	 * Characteristic is a typesafe enum class that describes the function of a PortProto.
	 * Characteristics are technology-independent and describe the nature of the port (input, output, etc.)
	 */
	public static class Characteristic
	{
		private final String name;
		private final String fullName;
		private final int bits;
		private final int order;
		private static int ordering = 0;

		private static HashMap characteristicList = new HashMap();

		private Characteristic(String fullName, String name, int bits)
		{
			this.fullName = fullName;
			this.name = name;
			this.bits = bits;
			this.order = ordering++;
			characteristicList.put(new Integer(bits), this);
		}

		/**
		 * Method to return the bit value associated with this Characteristic.
		 * @return the bit value associated with this Characteristic.
		 */
		public int getBits() { return bits; }

		/**
		 * Method to return the ordering of this Characteristic.
		 * @return the order number of this Characteristic.
		 */
		public int getOrder() { return order; }

		/**
		 * Method to return the full name of this Characteristic.
		 * @return the full name of this Characteristic.
		 */
		public String getFullName() { return fullName; }

		/**
		 * Method to return the short name of this Characteristic.
		 * @return the short name of this Characteristic.
		 */
		public String getName() { return name; }

		/**
		 * Method to tell whether this Characteristic is "reference".
		 * Reference exports have an extra name that identifies the reference export.
		 * @return true if this Characteristic is "reference".
		 */
		public boolean isReference()
		{
			if (this == REFIN || this == REFOUT || this == REFBASE) return true;
			return false;
		}

		/**
		 * Method to find the characteristic associated with the given bit value.
		 * @param bits the bit value associated with a Characteristic.
		 * @return the desired Characteristic (null if not found).
		 */
		public static Characteristic findCharacteristic(int bits)
		{
			Object obj = characteristicList.get(new Integer(bits));
			if (obj == null) return null;
			return (Characteristic)obj;
		}

		/**
		 * Method to find the characteristic associated with the given name.
		 * @param wantName the name of a Characteristic.
		 * @return the desired Characteristic (null if not found).
		 */
		public static Characteristic findCharacteristic(String wantName)
		{
			for(Iterator it = characteristicList.values().iterator(); it.hasNext(); )
			{
				Characteristic ch = (Characteristic)it.next();
				if (ch.name.equals(wantName)) return ch;
			}
			return null;
		}

		/**
		 * Method to return an iterator over all of the PortProto Characteristics.
		 * @return an iterator over all of the PortProto Characteristics.
		 */
		public static List getOrderedCharacteristics()
		{
			List orderedList = new ArrayList();
			for(Iterator it = characteristicList.values().iterator(); it.hasNext(); )
				orderedList.add(it.next());
			Collections.sort(orderedList, new CharacteristicOrder());
			return orderedList;
		}

		static class CharacteristicOrder implements Comparator
		{
			public int compare(Object o1, Object o2)
			{
				Characteristic c1 = (Characteristic)o1;
				Characteristic c2 = (Characteristic)o2;
				return c1.order - c2.order;
			}
		}

		/**
		 * Returns a printable version of this Characteristic.
		 * @return a printable version of this Characteristic.
		 */
		public String toString() { return name; }

		/** Describes an unknown port. */
			public static final Characteristic UNKNOWN = new Characteristic("Unknown", "unknown", 0);
		/** Describes an input port. */
			public static final Characteristic IN      = new Characteristic("Input", "input", INPORT);
		/** Describes an output port. */
			public static final Characteristic OUT     = new Characteristic("Output", "output", OUTPORT);
		/** Describes a bidirectional port. */
			public static final Characteristic BIDIR   = new Characteristic("Bidirectional", "bidirectional", BIDIRPORT);
		/** Describes a power port. */
			public static final Characteristic PWR     = new Characteristic("Power", "power", PWRPORT);
		/** Describes a ground port. */
			public static final Characteristic GND     = new Characteristic("Ground", "ground", GNDPORT);
		/** Describes an un-phased clock port. */
			public static final Characteristic CLK     = new Characteristic("Clock", "clock", CLKPORT);
		/** Describes a clock phase 1 port. */
			public static final Characteristic C1      = new Characteristic("Clock Phase 1", "clock1", C1PORT);
		/** Describes a clock phase 2 port. */
			public static final Characteristic C2      = new Characteristic("Clock Phase 2", "clock2", C2PORT);
		/** Describes a clock phase 3 port. */
			public static final Characteristic C3      = new Characteristic("Clock Phase 3", "clock3", C3PORT);
		/** Describes a clock phase 4 port. */
			public static final Characteristic C4      = new Characteristic("Clock Phase 4", "clock4", C4PORT);
		/** Describes a clock phase 5 port. */
			public static final Characteristic C5      = new Characteristic("Clock Phase 5", "clock5", C5PORT);
		/** Describes a clock phase 6 port. */
			public static final Characteristic C6      = new Characteristic("Clock Phase 6", "clock6", C6PORT);
		/** Describes a bias-level reference output port. */
			public static final Characteristic REFOUT  = new Characteristic("Reference Output", "refout", REFOUTPORT);
		/** Describes a bias-level reference input port. */
			public static final Characteristic REFIN   = new Characteristic("Reference Input", "refin", REFINPORT);
		/** Describes a bias-level reference base port. */
			public static final Characteristic REFBASE = new Characteristic("Reference Base", "refbase", REFBASEPORT);
	}

	// ------------------------ private data --------------------------

	/** The name of this PortProto. */							private Name protoName;
	/** Internal flag bits of this PortProto. */				protected int userBits;
	/** The parent NodeProto of this PortProto. */				protected NodeProto parent;
	/** Index of this PortProto in NodeProto ports. */			private int portIndex;
	/** The text descriptor of this PortProto. */				private TextDescriptor descriptor;
	/** A temporary integer value of this PortProto. */			private int tempInt;
	/** The temporary Object. */								private Object tempObj;
	/** The temporary flag bits. */								private int flagBits;
	/** The object used to request flag bits. */				private static FlagSet.Generator flagGenerator = new FlagSet.Generator("PortProto");

	// ----------------------- public constants -----------------------

	// ------------------ protected and private methods ---------------------

	/**
	 * This constructor should not be called.
	 * Use the subclass factory methods to create Export or PrimitivePort objects.
	 */
	protected PortProto()
	{
		this.parent = null;
		this.userBits = 0;
		this.descriptor = TextDescriptor.newExportDescriptor(this);
		this.tempObj = null;
	}

	/**
	 * Method to set the parent NodeProto that this PortProto belongs to.
	 * @param parent the parent NodeProto that this PortProto belongs to.
	 */
	public void setParent(NodeProto parent)
	{
		this.parent = parent;
	}

	/**
	 * Method to set the name of this PortProto.
	 * @param protoName string with new name of this PortProto.
	 */
	public void setProtoName(String protoName)
	{
		this.protoName = Name.findName(protoName);
	}

	/**
	 * Method to set an index of this PortProto in NodeProto ports.
	 * This is a zero-based index of ports on the NodeProto.
	 * @param portIndex an index of this PortProto in NodeProto ports.
	 */
	void setPortIndex(int portIndex) { this.portIndex = portIndex; }

	/**
	 * Method to get the index of this PortProto.
	 * This is a zero-based index of ports on the NodeProto.
	 * @return the index of this PortProto.
	 */
	public final int getPortIndex() { return portIndex; }

	/**
	 * Method to remove this PortProto from its parent NodeProto.
	 */
	public void kill()
	{
		parent.removePort(this);
	}

	/*
	 * Method to write a description of this PortProto.
	 * Displays the description in the Messages Window.
	 */
	public void getInfo()
	{
		System.out.println(" Parent: " + parent);
		System.out.println(" Name: " + protoName);
		super.getInfo();
	}

	// ---------------------- public methods -------------------------

	/**
	 * Method to return the name key of this PortProto.
	 * @return the Name of this PortProto.
	 */
	public Name getProtoNameKey() { return protoName; }

	/**
	 * Method to return the name of this PortProto.
	 * @return the name of this PortProto.
	 */
	public String getProtoName() { return protoName.toString(); }

	/**
	 * Method to return the short name of this PortProto.
	 * The short name is everything up to the first nonalphabetic character.
	 * @return the short name of this PortProto.
	 */
	public String getShortProtoName()
	{
		String name = protoName.toString();
		int len = name.length();
		for(int i=0; i<len; i++)
		{
			char ch = name.charAt(i);
			if (Character.isLetterOrDigit(ch)) continue;
			return name.substring(0, i);
		}
		return name;
	}

	/**
	 * Method to return the parent NodeProto of this PortProto.
	 * @return the parent NodeProto of this PortProto.
	 */
	public NodeProto getParent() { return parent; }

	/**
	 * Method to return the Text Descriptor of this PortProto.
	 * Text Descriptors tell how to display the port name.
	 * @return the Text Descriptor of this PortProto.
	 */
	public TextDescriptor getTextDescriptor() { return descriptor; }

	/**
	 * Method to set the Text Descriptor of this PortProto.
	 * Text Descriptors tell how to display the port name.
	 * @param descriptor the Text Descriptor of this PortProto.
	 */
	public void setTextDescriptor(TextDescriptor descriptor) { this.descriptor.copy(descriptor); }

	/**
	 * Method to return the Characteristic of this PortProto.
	 * @return the Characteristic of this PortProto.
	 */
	public Characteristic getCharacteristic()
	{
		Characteristic characteristic = Characteristic.findCharacteristic((userBits>>STATEBITSSH)&STATEBITSSHIFTED);
		return characteristic;
	}

	/**
	 * Method to set the Characteristic of this PortProto.
	 * @param characteristic the Characteristic of this PortProto.
	 */
	public void setCharacteristic(Characteristic characteristic)
	{
		userBits = (userBits & ~STATEBITS) | (characteristic.getBits() << STATEBITSSH);
	}

	/**
	 * Method to determine whether this PortProto is of type Power.
	 * This is determined by either having the proper Characteristic, or by
	 * having the proper name (starting with "vdd", "vcc", "pwr", or "power").
	 * @return true if this PortProto is of type Power.
	 */
	public boolean isPower()
	{
		Characteristic ch = getCharacteristic();
		if (ch == Characteristic.PWR) return true;
		if (ch != Characteristic.UNKNOWN) return false;
		String name = getProtoName().toLowerCase();
		if (name.startsWith("vdd")) return true;
		if (name.startsWith("vcc")) return true;
		if (name.startsWith("pwr")) return true;
		if (name.startsWith("power")) return true;
		return false;
	}

	/**
	 * Method to determine whether this PortProto is of type Ground.
	 * This is determined by either having the proper Characteristic, or by
	 * having the proper name (starting with "vss", "gnd", or "ground").
	 * @return true if this PortProto is of type Ground.
	 */
	public boolean isGround()
	{
		Characteristic ch = getCharacteristic();
		if (ch == Characteristic.GND) return true;
		if (ch != Characteristic.UNKNOWN) return false;
		String name = getProtoName().toLowerCase();
		if (name.startsWith("vss")) return true;
		if (name.startsWith("gnd")) return true;
		if (name.startsWith("ground")) return true;
		return false;
	}

	/**
	 * Method to return the angle of this PortProto.
	 * This is the primary angle that the PortProto faces on the NodeProto.
	 * It is only used on PrimitivePorts, and is set during Technology creation.
	 * @return the angle of this PortProto.
	 */
	public int getAngle()
	{
		return (userBits & PORTANGLE) >> PORTANGLESH;
	}

	/**
	 * Method to set the angle of this PortProto.
	 * This is the primary angle that the PortProto faces on the NodeProto.
	 * It is only used on PrimitivePorts, and is set during Technology creation.
	 * @param angle the angle of this PortProto.
	 */
	protected void setAngle(int angle)
	{
		userBits = (userBits & ~PORTANGLE) | (angle << PORTANGLESH);
	}

	/**
	 * Method to set the angle range of this PortProto.
	 * This is the range about the angle of allowable connections.
	 * When this value is 180, then all angles are permissible, since arcs
	 * can connect at up to 180 degrees in either direction from the port angle.
	 * It is only used on PrimitivePorts, and is set during Technology creation.
	 * @param angleRange the angle range of this PortProto.
	 */
	protected void setAngleRange(int angleRange)
	{
		userBits = (userBits & ~PORTARANGE) | (angleRange << PORTARANGESH);
	}

	/**
	 * Method to set the topology of this PortProto.
	 * This is a small integer that is unique among PortProtos on this NodeProto.
	 * When two PortProtos have the same topology number, it indicates that these
	 * ports are connected.
	 * It is only used on PrimitivePorts, and is set during Technology creation.
	 * @param topologyIndex the topology of this PortProto.
	 */
	protected void setTopology(int topologyIndex)
	{
		userBits = (userBits & ~PORTNET) | (topologyIndex << PORTNETSH);
	}

	/**
	 * Method to get the topology of this PortProto.
	 * This is a small integer that is unique among PortProtos on this NodeProto.
	 * When two PortProtos have the same topology number, it indicates that these
	 * ports are connected.
	 * It is only used on PrimitivePorts, and is set during Technology creation.
	 * @return the topology of this PortProto.
	 */
	public int getTopology()
	{
		return (userBits & PORTNET) >> PORTNETSH;
	}

	/**
	 * Method to set this PortProto to be isolated.
	 * Isolated ports do not electrically connect their arcs.
	 * This occurs in the multiple inputs to a schematic gate that all connect to the same port but do not themselves connect.
	 */
	public void setIsolated() { userBits |= PORTISOLATED; }

	/**
	 * Method to set this PortProto to be not isolated.
	 * Isolated ports do not electrically connect their arcs.
	 * This occurs in the multiple inputs to a schematic gate that all connect to the same port but do not themselves connect.
	 */
	public void clearIsolated() { userBits &= ~PORTISOLATED; }

	/**
	 * Method to tell whether this PortProto is isolated.
	 * Isolated ports do not electrically connect their arcs.
	 * This occurs in the multiple inputs to a schematic gate that all connect to the same port but do not themselves connect.
	 * @return true if this PortProto is isolated.
	 */
	public boolean isIsolated() { return (userBits & PORTISOLATED) != 0; }

	/**
	 * Method to set this PortProto to be always drawn.
	 * Ports that are always drawn have their name displayed at all times, even when an arc is connected to them.
	 */
	public void setAlwaysDrawn() { userBits |= PORTDRAWN; }

	/**
	 * Method to set this PortProto to be not always drawn.
	 * Ports that are always drawn have their name displayed at all times, even when an arc is connected to them.
	 */
	public void clearAlwaysDrawn() { userBits &= ~PORTDRAWN; }

	/**
	 * Method to tell whether this PortProto is always drawn.
	 * Ports that are always drawn have their name displayed at all times, even when an arc is connected to them.
	 * @return true if this PortProto is always drawn.
	 */
	public boolean isAlwaysDrawn() { return (userBits & PORTDRAWN) != 0; }

	/**
	 * Method to set this PortProto to exist only in the body of a cell.
	 * Ports that exist only in the body do not have an equivalent in the icon.
	 * This is used by simulators and icon generators to recognize less significant ports.
	 */
	public void setBodyOnly() { userBits |= BODYONLY; }

	/**
	 * Method to set this PortProto to exist in the body and icon of a cell.
	 * Ports that exist only in the body do not have an equivalent in the icon.
	 * This is used by simulators and icon generators to recognize less significant ports.
	 */
	public void clearBodyOnly() { userBits &= ~BODYONLY; }

	/**
	 * Method to tell whether this PortProto exists only in the body of a cell.
	 * Ports that exist only in the body do not have an equivalent in the icon.
	 * This is used by simulators and icon generators to recognize less significant ports.
	 * @return true if this PortProto exists only in the body of a cell.
	 */
	public boolean isBodyOnly() { return (userBits & BODYONLY) != 0; }

	/**
	 * Abstract method to return the base-level port that this PortProto is created from.
	 * For a PrimitivePort, it simply returns itself.
	 * For an Export, it returns the base port of its sub-port, the port on the NodeInst
	 * from which the Export was created.
	 * @return the base-level port that this PortProto is created from.
	 */
	public abstract PrimitivePort getBasePort();

	/**
	 * Method to return true if the specified ArcProto can connect to this PortProto.
	 * @param arc the ArcProto to test for connectivity.
	 * @return true if this PortProto can connect to the ArcProto, false if it can't.
	 */
	public boolean connectsTo(ArcProto arc)
	{
		return getBasePort().connectsTo(arc);
	}

	/**
	 * Method to compute the color of this PortProto.
	 * Uses the PrimitivePort at the bottom of the hierarchy.
	 * @return the color to use for this PortProto.
	 */
	public Color colorOfPort()
	{
		return getBasePort().getPortColor();
	}

	/**
	 * Low-level method to get the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the "user bits".
	 */
	public int lowLevelGetUserbits() { return userBits; }

	/**
	 * Low-level method to set the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param userBits the new "user bits".
	 */
	public void lowLevelSetUserbits(int userBits) { this.userBits = userBits; }

	/**
	 * Method to set an arbitrary integer in a temporary location on this PortProto.
	 * @param tempInt the integer to be set on this PortProto.
	 */
	public void setTempInt(int tempInt) { this.tempInt = tempInt; }

	/**
	 * Method to get the temporary integer on this PortProto.
	 * @return the temporary integer on this PortProto.
	 */
	public int getTempInt() { return tempInt; }

	/**
	 * Method to set an arbitrary Object in a temporary location on this PortProto.
	 * @param tempObj the Object to be set on this PortProto.
	 */
	public void setTempObj(Object tempObj) { this.tempObj = tempObj; }

	/**
	 * Method to get the temporary Object on this PortProto.
	 * @return the temporary Object on this PortProto.
	 */
	public Object getTempObj() { return tempObj; }

	/**
	 * Method to get access to flag bits on this PortProto.
	 * Flag bits allow NodeProtos to be marked and examined more conveniently.
	 * However, multiple competing activities may want to mark the nodes at
	 * the same time.  To solve this, each activity that wants to mark nodes
	 * must create a FlagSet that allocates bits in the node.  When done,
	 * the FlagSet must be released.
	 * @param numBits the number of flag bits desired.
	 * @return a FlagSet object that can be used to mark and test the PortProto.
	 */
	public static FlagSet getFlagSet(int numBits) { return FlagSet.getFlagSet(flagGenerator, numBits); }

	/**
	 * Method to set the specified flag bits on this PortProto.
	 * @param set the flag bits that are to be set on this PortProto.
	 */
	public void setBit(FlagSet set) { /*checkChanging();*/ flagBits = flagBits | set.getMask(); }

	/**
	 * Method to set the specified flag bits on this PortProto.
	 * @param set the flag bits that are to be cleared on this PortProto.
	 */
	public void clearBit(FlagSet set) { /*checkChanging();*/ flagBits = flagBits & set.getUnmask(); }

	/**
	 * Method to test the specified flag bits on this PortProto.
	 * @param set the flag bits that are to be tested on this PortProto.
	 * @return true if the flag bits are set.
	 */
	public boolean isBit(FlagSet set) { return (flagBits & set.getMask()) != 0; }

	/**
	 * Method to return the PortProto that is equivalent to this in the
	 * corresponding NodeProto. It is overrideen in Export.
	 * @return the PortProto that is equivalent to this in the corresponding Cell.
	 */
	public PortProto getEquivalent()
	{
		return this;
	}

	/**
	 * Returns a printable version of this PortProto.
	 * @return a printable version of this PortProto.
	 */
	public String toString()
	{
		return "PortProto " + protoName;
	}
}

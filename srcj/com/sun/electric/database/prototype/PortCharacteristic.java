/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PortCharacteristic.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * PortCharacteristic is a typesafe enum class that describes the function of a PortProto.
 * PortCharacteristics are technology-independent and describe the nature of the port (input, output, etc.)
 */
public class PortCharacteristic
{
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


	private final String name;
	private final String shortName;
	private final String fullName;
	private final int bits;
	private final int order;
	private static int ordering = 0;

	private static HashMap<Integer,PortCharacteristic> characteristicList = new HashMap<Integer,PortCharacteristic>();

	private PortCharacteristic(String shortName, String fullName, String name, int bits)
	{
		this.shortName = shortName;
		this.fullName = fullName;
		this.name = name;
		this.bits = bits;
		this.order = ordering++;
		characteristicList.put(new Integer(bits), this);
	}

	/**
	 * Method to return the bit value associated with this PortCharacteristic.
	 * @return the bit value associated with this PortCharacteristic.
	 */
	public int getBits() { return bits; }

	/**
	 * Method to return the ordering of this PortCharacteristic.
	 * @return the order number of this PortCharacteristic.
	 */
	public int getOrder() { return order; }

	/**
	 * Method to return the full name of this PortCharacteristic.
	 * @return the full name of this PortCharacteristic.
	 */
	public String getFullName() { return fullName; }

	/**
	 * Method to return the short name of this PortCharacteristic.
	 * The short name is one or two characters, used in JELIB files.
	 * @return the short name of this PortCharacteristic.
	 */
	public String getShortName() { return shortName; }

	/**
	 * Method to return the short name of this PortCharacteristic.
	 * @return the short name of this PortCharacteristic.
	 */
	public String getName() { return name; }

	/**
	 * Method to tell whether this PortCharacteristic is "reference".
	 * Reference exports have an extra name that identifies the reference export.
	 * @return true if this PortCharacteristic is "reference".
	 */
	public boolean isReference()
	{
		if (this == REFIN || this == REFOUT || this == REFBASE) return true;
		return false;
	}

	/**
	 * Method to find the characteristic associated with the given bit value.
	 * @param bits the bit value associated with a PortCharacteristic.
	 * @return the desired PortCharacteristic (null if not found).
	 */
	public static PortCharacteristic findCharacteristic(int bits)
	{
		Object obj = characteristicList.get(new Integer(bits));
		if (obj == null) return null;
		return (PortCharacteristic)obj;
	}

	/**
	 * Method to find the characteristic associated with the given name.
	 * @param wantName the name of a PortCharacteristic.
	 * @return the desired PortCharacteristic (null if not found).
	 */
	public static PortCharacteristic findCharacteristic(String wantName)
	{
		for(PortCharacteristic ch : characteristicList.values())
		{
			if (ch.name.equals(wantName)) return ch;
		}
		return null;
	}

	/**
	 * Method to find the characteristic associated with the given short name.
	 * The short name is one or two characters, used in JELIB files.
	 * @param shortName the short name of a PortCharacteristic.
	 * @return the desired PortCharacteristic (null if not found).
	 */
	public static PortCharacteristic findCharacteristicShort(String shortName)
	{
		for(PortCharacteristic ch : characteristicList.values())
		{
			if (ch.shortName.equals(shortName)) return ch;
		}
		return null;
	}

	/**
	 * Method to return an iterator over all of the PortCharacteristics.
	 * @return an iterator over all of the PortCharacteristics.
	 */
	public static List<PortCharacteristic> getOrderedCharacteristics()
	{
		List<PortCharacteristic> orderedList = new ArrayList<PortCharacteristic>();
		for(PortCharacteristic ch : characteristicList.values())
			orderedList.add(ch);
		Collections.sort(orderedList, new CharacteristicOrder());
		return orderedList;
	}

	static class CharacteristicOrder implements Comparator<PortCharacteristic>
	{
		public int compare(PortCharacteristic c1, PortCharacteristic c2)
		{
			return c1.order - c2.order;
		}
	}

	/**
	 * Returns a printable version of this PortCharacteristic.
	 * @return a printable version of this PortCharacteristic.
	 */
	public String toString() { return name; }

	/** Describes an unknown port. */
		public static final PortCharacteristic UNKNOWN = new PortCharacteristic("U", "Unknown", "unknown", 0);
	/** Describes an input port. */
		public static final PortCharacteristic IN      = new PortCharacteristic("I", "Input", "input", INPORT);
	/** Describes an output port. */
		public static final PortCharacteristic OUT     = new PortCharacteristic("O", "Output", "output", OUTPORT);
	/** Describes a bidirectional port. */
		public static final PortCharacteristic BIDIR   = new PortCharacteristic("B", "Bidirectional", "bidirectional", BIDIRPORT);
	/** Describes a power port. */
		public static final PortCharacteristic PWR     = new PortCharacteristic("P", "Power", "power", PWRPORT);
	/** Describes a ground port. */
		public static final PortCharacteristic GND     = new PortCharacteristic("G", "Ground", "ground", GNDPORT);
	/** Describes an un-phased clock port. */
		public static final PortCharacteristic CLK     = new PortCharacteristic("C", "Clock", "clock", CLKPORT);
	/** Describes a clock phase 1 port. */
		public static final PortCharacteristic C1      = new PortCharacteristic("C1", "Clock Phase 1", "clock1", C1PORT);
	/** Describes a clock phase 2 port. */
		public static final PortCharacteristic C2      = new PortCharacteristic("C2", "Clock Phase 2", "clock2", C2PORT);
	/** Describes a clock phase 3 port. */
		public static final PortCharacteristic C3      = new PortCharacteristic("C3", "Clock Phase 3", "clock3", C3PORT);
	/** Describes a clock phase 4 port. */
		public static final PortCharacteristic C4      = new PortCharacteristic("C4", "Clock Phase 4", "clock4", C4PORT);
	/** Describes a clock phase 5 port. */
		public static final PortCharacteristic C5      = new PortCharacteristic("C5", "Clock Phase 5", "clock5", C5PORT);
	/** Describes a clock phase 6 port. */
		public static final PortCharacteristic C6      = new PortCharacteristic("C6", "Clock Phase 6", "clock6", C6PORT);
	/** Describes a bias-level reference output port. */
		public static final PortCharacteristic REFOUT  = new PortCharacteristic("RO", "Reference Output", "refout", REFOUTPORT);
	/** Describes a bias-level reference input port. */
		public static final PortCharacteristic REFIN   = new PortCharacteristic("RI", "Reference Input", "refin", REFINPORT);
	/** Describes a bias-level reference base port. */
		public static final PortCharacteristic REFBASE = new PortCharacteristic("RB", "Reference Base", "refbase", REFBASEPORT);
}

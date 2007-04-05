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
public enum PortCharacteristic
{
	/** Describes an unknown port. */                   UNKNOWN("U", "Unknown", "unknown"),
	/** Describes an un-phased clock port. */           CLK("C", "Clock", "clock"),
	/** Describes a clock phase 1 port. */              C1("C1", "Clock Phase 1", "clock1"),
	/** Describes a clock phase 2 port. */              C2("C2", "Clock Phase 2", "clock2"),
	/** Describes a clock phase 3 port. */              C3("C3", "Clock Phase 3", "clock3"),
	/** Describes a clock phase 4 port. */              C4("C4", "Clock Phase 4", "clock4"),
	/** Describes a clock phase 5 port. */              C5("C5", "Clock Phase 5", "clock5"),
	/** Describes a clock phase 6 port. */              C6("C6", "Clock Phase 6", "clock6"),
	/** Describes an input port. */                     IN("I", "Input", "input"),
	/** Describes an output port. */                    OUT("O", "Output", "output"),
	/** Describes a bidirectional port. */              BIDIR("B", "Bidirectional", "bidirectional"),
	/** Describes a power port. */                      PWR("P", "Power", "power"),
	/** Describes a ground port. */                     GND("G", "Ground", "ground"),
	/** Describes a bias-level reference output port. */REFOUT("RO", "Reference Output", "refout"),
	/** Describes a bias-level reference input port. */ REFIN("RI", "Reference Input", "refin"),
	/** Describes a bias-level reference base port. */  REFBASE("RB", "Reference Base", "refbase");
        
	private final String name;
	private final String shortName;
	private final String fullName;
	private final int bits;
	private static int ordering = 0;

	private PortCharacteristic(String shortName, String fullName, String name)
	{
		this.shortName = shortName;
		this.fullName = fullName;
		this.name = name;
		this.bits = ordinal() << 1;
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
	public int getOrder() { return ordinal(); }

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
	 * Method to tell whether this PortCharacteristic is a "clock".
	 * @return true if this PortCharacteristic is a "clock".
	 */
	public boolean isClock()
	{
		if (this == CLK || this == C1 || this == C2 || this == C3 || this == C4 || this == C5 || this == C6) return true;
		return false;
	}

	/**
	 * Method to find the characteristic associated with the given bit value.
	 * @param bits the bit value associated with a PortCharacteristic.
	 * @return the desired PortCharacteristic (null if not found).
	 */
	public static PortCharacteristic findCharacteristic(int bits)
	{
        if ((bits & 1) != 0) return null;
        int index = bits >>> 1;
        PortCharacteristic[] allCharacteristics = PortCharacteristic.class.getEnumConstants();
        return index < allCharacteristics.length ? allCharacteristics[index] : null;
	}

	/**
	 * Method to find the characteristic associated with the given name.
	 * @param wantName the name of a PortCharacteristic.
	 * @return the desired PortCharacteristic (null if not found).
	 */
	public static PortCharacteristic findCharacteristic(String wantName)
	{
		for(PortCharacteristic ch : PortCharacteristic.class.getEnumConstants())
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
		for(PortCharacteristic ch : PortCharacteristic.class.getEnumConstants())
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
		for(PortCharacteristic ch : PortCharacteristic.class.getEnumConstants())
			orderedList.add(ch);
		Collections.sort(orderedList, new CharacteristicOrder());
		return orderedList;
	}

	static class CharacteristicOrder implements Comparator<PortCharacteristic>
	{
		public int compare(PortCharacteristic c1, PortCharacteristic c2)
		{
			return c1.ordinal() - c2.ordinal();
		}
	}

	/**
	 * Returns a printable version of this PortCharacteristic.
	 * @return a printable version of this PortCharacteristic.
	 */
	public String toString() { return name; }

}

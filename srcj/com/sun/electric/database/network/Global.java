/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Global.java
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
package com.sun.electric.database.network;

import com.sun.electric.database.text.Name;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A Global define global signal in a network tool.
 */
public class Global
{

	/** the name key */							private Name name;
	/** the 0-based index of this Global. */	private int index;

	/** All Globals. */							private static Global[] allGlobals = new Global[0];
	/** Buffer for construction Global.Set. */	private static boolean[] globalsBuf;
	/** Map Name -> Global. */					private static Map globalsByName = new HashMap();

	/** global signal ground. */				public static final Global ground = newGlobal("Ground");
	/** global signal power. */					public static final Global power = newGlobal("Power");

	/**
	 * Constructs Global signal with given name key.
	 * @param name name key
	 */
	private Global(Name name) {
		this.name = name;
		index = allGlobals.length;
		Global[] newGlobals = new Global[index + 1];
		boolean[] newGlobalsBuf = new boolean[index + 1];
		for (int i = 0; i < index; i++) {
			newGlobals[i] = allGlobals[i];
			newGlobalsBuf[i] = globalsBuf[i];
		}
		newGlobals[index] = this;
		allGlobals = newGlobals;
		globalsBuf = newGlobalsBuf;
		globalsByName.put(name.lowerCase(), this);
	}

	/**
	 * Returns name of this Global signal.
	 * @return name of this Global signal.
	 */
	public String getName() { return name.toString(); }

	/**
	 * Returns name key of this Global signal.
	 * @return name key of this Global signal.
	 */
	public Name getNameKey() { return name; }

	/**
	 * Returns a printable version of this Global.
	 * @return a printable version of this Global.
	 */
	public String toString() {
		return "Global "+name;
	}

	/**
	 * Finds or creates global signal with given name.
	 * @param name name
	 * @return Global signal with given name.
	 */
	public static Global newGlobal(String name) {
		Name nameKey = Name.findName(name);
		Global g = (Global) globalsByName.get(nameKey);
		if (g == null)
			g = new Global(nameKey);
		return g;
	}

	/**
	 * A Global.Set defines a set of Global signals.
	 */
	public static class Set implements Comparable {

		/** bitmap which defines set. */			private boolean[] elemMap;
		/** map localInd->globalInd. */				private Global[] elems;
		/** map globalInd->localInd. */				private int[] indexOf;

		/** Map Set->Set of all Global.Sets. */		private static Map allSets = new TreeMap();
		/** A private set for search in allSets. */	private static Global.Set fakeSet = new Global.Set(new boolean[0]);
		/** A private set for search in allSets. */	public static final Global.Set empty = newSet(new boolean[0]);

		/**
		 * Constructs Global.Set with specified bitmap of Globals.
		 * @param elemMap bitmap which specifies which elemenst are in set.
		 */
		private Set(boolean[] elemMap) {
			int maxElem = -1;
			int numElem = 0;

			for (int i = 0; i < elemMap.length; i++) {
				if (!elemMap[i]) continue;
				maxElem = i;
				numElem++;
			}

			this.elemMap = new boolean[maxElem + 1];
			elems = new Global[numElem];
			indexOf = new int[maxElem + 1];
			Arrays.fill(indexOf, -1);
			List elemList = new ArrayList(numElem);

			int local = 0;
			for (int i = 0; i < elemMap.length; i++) {
				if (!elemMap[i]) continue;
				this.elemMap[i] = true;
				elems[local] = allGlobals[i];
				indexOf[i] = local;
				local++;
			}
		}

		/**
		 * Returns <tt>true</tt> if this Global.Set contains a specified Global.
		 * @param global The Global whose presence in this set is to be tested
		 * @return <tt>true</tt> if this Global Set contains a specified Global.
		 */
		public final boolean contains(Global global) { return global.index < elemMap.length && elemMap[global.index]; }

		/**
		 * Returns the number of Globals in this Global.Set.
		 * @return the number of Globals in this Global.Set.
		 */
		public final int size() { return elems.length; }

		/**
		 * Returns a Global from this set by specified index.
		 * @param i specified index of global.
		 * @return a Global from this set by specified index.
		 */
		public final Global get(int i) { return elems[i]; }

		/**
		 * Returns an index of specified Global in this set.
		 * Returns -1 if Global is not in set.
		 * @param g specified global.
		 * @return a Global from this set by specified index.
		 */
		public final int indexOf(Global g) { return g.index < indexOf.length ? indexOf[g.index] : -1; }

		/**
		 * Returns the maximal index of Globals in this Set.
		 * @return the maximal index of Globals in this Set or (-1) for empty Set.
		 */
		public final int maxElement() { return elemMap.length - 1; }

		/**
		 * Compares this Global.Set with the specified Global.Set for order.  Returns a
		 * negative integer, zero, or a positive integer as this object is less
		 * than, equal to, or greater than the specified object.<p>
		 * @param   set the Global.Set to be compared.
		 * @return  a negative integer, zero, or a positive integer as this object
		 *		is less than, equal to, or greater than the specified object.
		 */
		public int compareTo(Global.Set set)
		{
			int minLen = Math.min(this.elemMap.length, set.elemMap.length);
			int i;
			for (i = 0; i < minLen; i++) {
				if (this.elemMap[i] != set.elemMap[i]) break;
			}
			if (i < minLen) return (this.elemMap[i] ? 1 : -1);
			for (; i < this.elemMap.length; i++)
				if (this.elemMap[i]) return 1;
			for (; i < set.elemMap.length; i++)
				if (set.elemMap[i]) return -1;
			return 0;
		}

		/**
		 * Compares this Global.set with the specified object for order.  Returns a
		 * negative integer, zero, or a positive integer as this object is less
		 * than, equal to, or greater than the specified object.<p>
		 * @param   o the Object to be compared.
		 * @return  a negative integer, zero, or a positive integer as this object
		 *		is less than, equal to, or greater than the specified object.
		 * 
		 * @throws ClassCastException if the specified object's type prevents it
		 *         from being compared to this Object.
		 */
		public int compareTo(Object o)
		{
			return compareTo((Global.Set)o);
		}

		public String toString() {
			String s = "Global.Set {";
			for (int i = 0; i < size(); i++)
				s += " " + get(i);
			s += "}";
			return s;
		}

		/**
		 * Finds or creates Global.Set with specified bitmap of Globals.
		 * @param elemMap bitmap which specifies which elemenst are in set.
		 * @return set of specified Globals.
		 */
		private static Global.Set newSet(boolean[] elemMap) {
			fakeSet.elemMap = elemMap;
			Global.Set set = (Global.Set) allSets.get(fakeSet);
			if (set == null) {
				set = new Global.Set(elemMap);
				allSets.put(set, set);
			}
			return set;
		}
	}

	// Global.Set buffer for constructing sets of Global.

	/**
	 * Clear all Globals from the buffer.
	 */
	static void clearBuf() {
		Arrays.fill(globalsBuf, false);
	}

	/**
	 * Add specified Global to the buffer.
	 * @param g specified Global.
	 */
	static void addToBuf(Global g) {
		globalsBuf[g.index] = true;
	}

	/**
	 * Add specified Global.Set to the buffer.
	 * @param sey specified Global.Set.
	 */
	static void addToBuf(Global.Set set) {
		for (int i = 0; i < set.elems.length; i++)
			globalsBuf[set.elems[i].index] = true;
	}

	/**
	 * Returns Global.Set of element in the buffer.
	 * @return set of Globals in the buffer.
	 */
	static Global.Set getBuf() { return Global.Set.newSet(globalsBuf); }
}

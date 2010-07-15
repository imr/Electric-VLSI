/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SignalCollection.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.simulation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *  This class represents a collection of Signals in a Stimuli.
 */
public class SignalCollection
{
	private Map<String,Signal<?>> sigsInCollection;
	private String collectionName;
	private String[] sweepNames;

	/**
	 * Constructor to build a new Simulation Data object.
	 */
	public SignalCollection(String name)
	{
		collectionName = name;
		sigsInCollection = new HashMap<String,Signal<?>>();
	}

	public void addSignal(String name, Signal<?> sig) { sigsInCollection.put(name, sig); }

	public Signal<?> findSignal(String name) { return sigsInCollection.get(name); }

	public String getName() { return collectionName; }

	public Set<String> getNames() { return sigsInCollection.keySet(); }

	public Collection<Signal<?>> getSignals() { return sigsInCollection.values(); }

	public void setSweepNames(String[] names) { sweepNames = names; }

	public String[] getSweepNames() { return sweepNames; }
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Instansable.java
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
package com.sun.electric.database.hierarchy;

import com.sun.electric.database.text.Name;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.NodeInst;

/**
 * This interface defines real or virtual instance of NodeProto in a Cell..
 */
public interface Instancable
{
	// ------------------------ public methods -------------------------------

	/**
	 * Routine to return the NodeUsage of this Instancable.
	 * @return the NodeUsage of this Instancable.
	 */
	public NodeUsage getNodeUsage();

	/**
	 * Routine to return the prototype of this Instancable.
	 * @return the prototype of this Instancable.
	 */
	public NodeProto getProto();

	/**
	 * Routine to return the Cell that contains this Instancable.
	 * @return the Cell that contains this Instancable.
	 */
	public Cell getParent();

	/**
	 * Routine to return the name of this Instancable.
	 * @return the name of this Instancable, null if there is no name.
	 */
	public String getName();

	/**
	 * Routine to return the Name object of this Instancable.
	 * @return the name of this Instancable, null if there is no name.
	 */
	public Name getNameLow();

	/**
	 * Returns a printable version of this Instancable.
	 * @return a printable version of this Instancable.
	 */
	public String toString();

}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FoldedNmos.java
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
package com.sun.electric.tool.generator.layout;

import com.sun.electric.database.hierarchy.Cell;

public class FoldedNmos extends FoldedMos {
	/** By default the FoldedNmos shifts the diffusion contact to the
	 * top of the transistor */
	public FoldedNmos(double x, double y, int nbFolds, int nbSeries,
                      double gateWidth, Cell f, StdCellParams stdCell) {
		super('N', x, y, nbFolds, nbSeries, gateWidth, null, 'T', f, stdCell);
	}
	public FoldedNmos(double x, double y, int nbFolds, int nbSeries,
                      double gateWidth, GateSpace gateSpace,
                      char justifyDiffCont, Cell f, StdCellParams stdCell) {
		super('N', x, y, nbFolds, nbSeries, gateWidth, gateSpace, 
		      justifyDiffCont, f, stdCell);
	}
}

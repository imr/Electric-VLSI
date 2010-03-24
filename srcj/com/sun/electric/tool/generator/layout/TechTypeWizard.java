/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechTypeWizard.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.generator.layout;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.technology.Technology;

/**
 * The TechType class holds technology dependent information for the layout
 * generators. Technology is like those generated by technology Wizard.
 * Technology dependent information
 */
public class TechTypeWizard extends TechType {

    public TechTypeWizard(Technology tech) {
        super(tech, null, null);
    }

    public int getNumMetals() {
        throw new UnsupportedOperationException();
    }

    /** round to avoid MOCMOS CIF resolution errors */
    public double roundToGrid(double x) {
        throw new UnsupportedOperationException();
    }

    public MosInst newNmosInst(double x, double y, double w, double l, Cell parent) {
        throw new UnsupportedOperationException();
    }

    public MosInst newPmosInst(double x, double y, double w, double l, Cell parent) {
        throw new UnsupportedOperationException();
    }

    public String name() {
        throw new UnsupportedOperationException();
    }

    public double reservedToLambda(int layer, double nbTracks) {
        throw new UnsupportedOperationException();
    }
}

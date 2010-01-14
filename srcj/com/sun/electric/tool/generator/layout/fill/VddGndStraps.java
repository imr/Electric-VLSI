/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VddGndStraps.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.generator.layout.fill;

import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.generator.layout.TechType;
import com.sun.electric.database.topology.PortInst;

/** Give access to the metal straps inside a MetalLayer or CapLayer */
public abstract class VddGndStraps {
    public static ArcProto[] METALS; // =
//        {null, Tech.m1(), Tech.m2(), Tech.m3(), Tech.m4(), Tech.m5(), Tech.m6(), Tech.m7(), Tech.m8(), Tech.m9()};
    public static PrimitiveNode[] PINS; // =
//        {null, Tech.m1pin(), Tech.m2pin(), Tech.m3pin(), Tech.m4pin(), Tech.m5pin(), Tech.m6pin(),
//                Tech.m7pin(), Tech.m8pin(), Tech.m9pin()};
    // Defined here so sequence with PINS is kept aligned
    public static PrimitiveNode[] fillContacts; // = {null, null, Tech.m2m3(), Tech.m3m4(), Tech.m4m5(), Tech.m5m6()};
    protected TechType tech;

    VddGndStraps(TechType t)
    {
        tech = t;
        METALS = new ArcProto[]{null, tech.m1(), tech.m2(), tech.m3(), tech.m4(), tech.m5(), tech.m6(),
        tech.m7(), tech.m8(), tech.m9()}; // 10 for now
        PINS = new PrimitiveNode[]{null, tech.m1pin(), tech.m2pin(), tech.m3pin(), tech.m4pin(), tech.m5pin(), tech.m6pin(),
                tech.m7pin(), tech.m8pin(), tech.m9pin()};
        fillContacts = new PrimitiveNode[] {null, null, tech.m2m3(), tech.m3m4(), tech.m4m5(), tech.m5m6()};
    }

    /** are metal straps horizontal? */		abstract boolean isHorizontal();

    /** how many Vdd straps? */				abstract int numVdd();
    /** get nth Vdd strap */				abstract PortInst getVdd(int n, int pos);
    /** if horizontal get Y else get X */	abstract double getVddCenter(int n);
    /** how wide is nth Vdd metal strap */	abstract double getVddWidth(int n);

    /** how many Gnd straps? */ 			abstract int numGnd();
    /** get nth Gnd strap */				abstract PortInst getGnd(int n, int pos);
    /** if horizontal get Y else X */ 		abstract double getGndCenter(int n);
    /** how wide is nth Gnd strap? */ 		abstract double getGndWidth(int n);

    abstract PrimitiveNode getPinType();
    abstract ArcProto getMetalType();
    abstract double getCellWidth();
    abstract double getCellHeight();
    abstract boolean addExtraArc(); /** To create an export on new pin connected with a zero length arc */
}

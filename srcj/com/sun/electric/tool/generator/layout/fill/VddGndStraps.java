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
package com.sun.electric.tool.generator.layout.fill;

import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.database.topology.PortInst;

/** Give access to the metal straps inside a MetalLayer or CapLayer */
public interface VddGndStraps {
    static final ArcProto[] METALS =
        {null, Tech.m1, Tech.m2, Tech.m3, Tech.m4, Tech.m5, Tech.m6};
    static final PrimitiveNode[] PINS =
        {null, Tech.m1pin, Tech.m2pin, Tech.m3pin, Tech.m4pin, Tech.m5pin, Tech.m6pin};
    // Defined here so sequence with PINS is kept aligned
    static final PrimitiveNode[] fillContacts = {null, null, Tech.m2m3, Tech.m3m4, Tech.m4m5, Tech.m5m6};
    /** are metal straps horizontal? */		boolean isHorizontal();

    /** how many Vdd straps? */				int numVdd();
    /** get nth Vdd strap */				PortInst getVdd(int n, int pos);
    /** if horizontal get Y else get X */	double getVddCenter(int n);
    /** how wide is nth Vdd metal strap */	double getVddWidth(int n);

    /** how many Gnd straps? */ 			int numGnd();
    /** get nth Gnd strap */				PortInst getGnd(int n, int pos);
    /** if horizontal get Y else X */ 		double getGndCenter(int n);
    /** how wide is nth Gnd strap? */ 		double getGndWidth(int n);

    PrimitiveNode getPinType();
    ArcProto getMetalType();
    double getCellWidth();
    double getCellHeight();
    boolean addExtraArc(); /** To create an export on new pin connected with a zero length arc */
}

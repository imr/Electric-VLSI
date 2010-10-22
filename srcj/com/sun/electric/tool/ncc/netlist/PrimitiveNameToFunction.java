/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PrimitiveNameToFunction.java
 *
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.electric.tool.ncc.netlist;

import java.util.HashMap;
import java.util.Map;

import com.sun.electric.technology.PrimitiveNode.Function;

/** Translate the names found in "transistorType" and "resistorType" NCC 
 * declarations into the appropriate PrimitiveNode.Function enum.  This 
 * is for backwards compatibility only. In the Future, transistors and resistors 
 * should have the correct Function. */
public class PrimitiveNameToFunction {
	private static PrimitiveNameToFunction nmToF = 
		new PrimitiveNameToFunction(); 
	
	private Map<String, Function> nameToEnum = new HashMap<String, Function>();
	
    private void add(Function f, String nm) {nameToEnum.put(nm, f);}
    
    private PrimitiveNameToFunction() {
    	add(Function.TRANMOSVTH, "VTH-N-Transistor");
    	add(Function.TRANMOSVTL, "VTL-N-Transistor");
    	add(Function.TRANMOSHV1, "OD18-N-Transistor");
    	add(Function.TRANMOSHV2, "OD25-N-Transistor");
    	add(Function.TRANMOSHV3, "OD33-N-Transistor");
    	add(Function.TRANMOSNT, "NT-N-Transistor");
    	add(Function.TRANMOSNTHV1, "NT-OD18-N-Transistor");
    	add(Function.TRANMOSNTHV2, "NT-OD25-N-Transistor");
    	add(Function.TRANMOSNTHV3, "NT-OD33-N-Transistor");

    	add(Function.TRAPMOSVTH, "VTH-P-Transistor");
    	add(Function.TRAPMOSVTL, "VTL-P-Transistor");
    	add(Function.TRAPMOSHV1, "OD18-P-Transistor");
    	add(Function.TRAPMOSHV2, "OD25-P-Transistor");
    	add(Function.TRAPMOSHV3, "OD33-P-Transistor");
    	
    	add(Function.RESNPOLY, "N-Poly-RPO-Resistor");
    	add(Function.RESPPOLY, "P-Poly-RPO-Resistor");
    	add(Function.RESNWELL, "N-Well-RPO-Resistor");
    }
    
    private Function nameToFunc(String nm) {return nameToEnum.get(nm);}
    
    public static Function nameToFunction(String nm) {
    	return nmToF.nameToFunc(nm);
    }
	
}

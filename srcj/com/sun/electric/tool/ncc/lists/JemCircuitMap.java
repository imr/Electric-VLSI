/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemCircuitMap.java
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
//  Updated 20 January 2004 when spread becomes map.  

package com.sun.electric.tool.ncc.lists;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.trees.JemCircuit;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;

public class JemCircuitMap extends JemMap{
    private JemCircuitMap(int i){
        super(i);
		return;
    } //end of JemCircuitMap constructor
	
	public static JemCircuitMap mapPlease(int i){
		JemCircuitMap mm= new JemCircuitMap(i);
		return mm;
	} //end of mapPlease

    protected boolean classOK(Object x){
        return (x instanceof JemCircuit);
    } //end of classOK

} //end of JemCircuitMap

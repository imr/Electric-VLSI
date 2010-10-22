/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Function4PortTo3Port.java
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

/** PrimitiveNode.Function distinguishes between 4-port and 3-port transistors. 
 * NCC doesn't care about this distinction. Therefore translate 4-port Functions 
 * into 3-port Functions. */
public class Function4PortTo3Port {
	private Map<Function, Function> fourPortToThreePort = new HashMap<Function,Function>();
	public Function4PortTo3Port() {
		Map<String,Function> nameToFunc = new HashMap<String,Function>();
		for (Function f : Function.getFunctions()) {
			nameToFunc.put(f.enumName(), f);
		}
		for (String name : nameToFunc.keySet()) {
			if (name.startsWith("TRA4")) {
				String name3port = "TRA"+name.substring(4);
				if (nameToFunc.containsKey(name3port)) {
					fourPortToThreePort.put(nameToFunc.get(name), 
							                nameToFunc.get(name3port));
				}
			}
		}
	}
	Function translate(Function in) {
		Function out = fourPortToThreePort.get(in);
		return out!=null ? out : in;
	}
}

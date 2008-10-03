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

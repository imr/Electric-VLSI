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

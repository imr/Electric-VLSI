/*
 * Created on Jul 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.sun.electric.tool.ncc.netlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.sun.electric.tool.generator.layout.LayoutLib;

/**
 * Transistor and resistor types have long and short names. In Electric, 
 * schematic and layout primitive transistors have different names. NCC uses 
 * the long name to match types between the schematic and layout. Except for 
 * the basic NMOS and PMOS types, each schematic transistor contains an 
 * annotation specifying which layout transistor it matches.
 * The short names are used internally for NCC print-outs because the long
 * names are too verbose.
 * 
 * Scalable transistors, in the layout, have a different long name than
 * regular transistor but map to the same type as ordinary layout transistors.
 */
public class PartTypeTable {
	private int numTypes = 0;
	private int log2NumTypes;
	private ArrayList<PartType> types = new ArrayList<PartType>();
	private HashMap<String,PartType> nameToType = new HashMap<String,PartType>();
	/** Long type names are used only to identify transistors when NCC creates
	 *  the net lists. */
	private HashMap<String,PartType> longNameToType = new HashMap<String,PartType>();
	private void add(String typeName, String longTypeName) {
		PartType t = nameToType.get(typeName);
		if (t==null) {
			t = new PartType(numTypes++, typeName);
			nameToType.put(typeName, t);
		}
		LayoutLib.error(longNameToType.containsKey(longTypeName), 
        				"duplicate long type name");
		longNameToType.put(longTypeName, t);
		types.add(t);

		log2NumTypes = (int) Math.ceil( Math.log(numTypes)/Math.log(2) );
	}
	public PartTypeTable(String[][] typeNames) {
		for (int i=0; i<typeNames.length; i++) {
			add(typeNames[i][0], typeNames[i][1]);
		}
	}
	public int log2NumTypes() {return log2NumTypes;}
	public Iterator<PartType> iterator() {return types.iterator();}
	public PartType get(String nm) {return nameToType.get(nm);}
	public PartType getTypeFromLongName(String nm) {
		return longNameToType.get(nm);
	}
}

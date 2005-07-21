/*
 * Created on Jul 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.sun.electric.tool.ncc.netlist;

/**
 * transistors and resistors have a rich set of types
 */
public class PartType {
	private final String name;
	private final int ordinal;

	public PartType(int ord, String s) {ordinal=ord; name=s;}
	public String getName() {return name;}
	public int getOrdinal() {return ordinal;}
}

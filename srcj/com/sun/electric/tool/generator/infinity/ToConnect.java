package com.sun.electric.tool.generator.infinity;

import java.util.ArrayList;
import java.util.List;

import com.sun.electric.database.topology.PortInst;

/** A list of PortInsts that need to be connected by the router */
public class ToConnect {
	private List<PortInst> ports = new ArrayList<PortInst>();
	private boolean exported = false;
	
	public ToConnect() {}
	public void addPortInst(PortInst pi) {ports.add(pi);}
	public List<PortInst> getPortInsts() {return ports;}
	public int size() {return ports.size();}
	public boolean isExported() {return exported;}
	public void setExported() {exported=true;}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (exported) sb.append("exported ");
		for (PortInst pi : ports) {
			sb.append(pi.toString()+" ");
		}
		return sb.toString();
	}
}

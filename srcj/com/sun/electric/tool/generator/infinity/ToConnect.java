package com.sun.electric.tool.generator.infinity;

import java.util.ArrayList;
import java.util.List;

import com.sun.electric.database.topology.PortInst;

/** A list of PortInsts that need to be connected by the router */
public class ToConnect {
	private List<PortInst> ports = new ArrayList<PortInst>();
	public ToConnect() {}
	public void addPortInst(PortInst pi) {ports.add(pi);}
	public List<PortInst> getPortInsts() {return ports;}
	public int size() {return ports.size();}
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (PortInst pi : ports) {
			sb.append(pi.toString()+" ");
		}
		return sb.toString();
	}
}

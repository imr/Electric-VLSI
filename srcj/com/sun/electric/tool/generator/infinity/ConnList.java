package com.sun.electric.tool.generator.infinity;

import java.util.ArrayList;
import java.util.List;

import com.sun.electric.database.topology.PortInst;

public class ConnList {
	private List<PortInst> ports = new ArrayList<PortInst>();
	public ConnList() {}
	public void addPortInst(PortInst pi) {ports.add(pi);}
	public List<PortInst> getPortInsts() {return ports;}
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (PortInst pi : ports) {
			sb.append(pi.toString()+" ");
		}
		return sb.toString();
	}
}

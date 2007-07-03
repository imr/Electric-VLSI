package com.sun.electric.tool.generator.infinity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.database.topology.PortInst;

/** A list of PortInsts that need to be connected by the router */
public class ToConnect {
	private List<PortInst> ports = new ArrayList<PortInst>();
	private List<String> exportNames;
	
	public ToConnect(Iterator<String> expNmIt) {
		if (expNmIt!=null && expNmIt.hasNext()) {
			exportNames = new ArrayList<String>();
			while (expNmIt.hasNext()) {
				exportNames.add(expNmIt.next());
			}
		}
	}
	public void addPortInst(PortInst pi) {ports.add(pi);}
	public List<PortInst> getPortInsts() {return ports;}
	public int size() {return ports.size();}
	public boolean isExported() {return exportNames!=null;}
	public Collection<String> getExportName() {return exportNames;}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("ToConnect: ");
		if (isExported()) {
			sb.append("Exports:");
			for (String expNm : exportNames) sb.append(" "+expNm);
			sb.append(", ");
		}
		sb.append("Ports: ");
		for (PortInst pi : ports) {
			sb.append(pi.toString()+" ");
		}
		return sb.toString();
	}
}

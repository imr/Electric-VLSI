package com.sun.electric.tool.generator.flag.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.generator.flag.Utils;

/** A list of PortInsts that need to be connected by the router */
public class ToConnect {
	private List<PortInst> ports = new ArrayList<PortInst>();
	private List<String> exportNames = new ArrayList<String>();
	
	public ToConnect() { }
	public ToConnect(List<String> expNms) {
		for (String expNm : expNms)  exportNames.add(expNm);
	}
	public void addPortInst(PortInst pi) {ports.add(pi);}
	public List<PortInst> getPortInsts() {return ports;}
	public int numPortInsts() {return ports.size();}
	public boolean isExported() {return exportNames.size()!=0;}
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
	public boolean isPowerOrGround() {
		for (PortInst pi : getPortInsts()) {
			if (Utils.isPwrGnd(pi)) return true;
		}
		return false;
	}

}

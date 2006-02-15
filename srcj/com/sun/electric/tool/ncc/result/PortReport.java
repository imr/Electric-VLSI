package com.sun.electric.tool.ncc.result;

import com.sun.electric.tool.ncc.netlist.Port;

public class PortReport extends NetObjReport {
	static final long serialVersionUID = 0;
	
	private final String wireName;
	private final String exportNamesString;
	private final boolean isImplied;
	public PortReport(Port p) {
		super(p);
		wireName = p.getWire().getName();
		exportNamesString = p.exportNamesString();
		isImplied = p.isImplied();
	}
	public String getWireName() {return wireName;}
	public String exportNamesString() {return exportNamesString;}
	public boolean isImplied() {return isImplied;}
}

package com.sun.electric.tool.ncc.result;

import com.sun.electric.tool.ncc.netlist.Wire;
import com.sun.electric.tool.ncc.netlist.NccNameProxy.WireNameProxy;

public class WireReport extends NetObjReport {
	static final long serialVersionUID = 0;
	
	private final WireNameProxy nameProxy;
	public WireReport(Wire w) {
		super(w);
		nameProxy = w.getNameProxy();
	}
	public WireNameProxy getNameProxy() {return nameProxy;}
}

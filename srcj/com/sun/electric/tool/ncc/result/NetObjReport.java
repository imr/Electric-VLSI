package com.sun.electric.tool.ncc.result;

import java.io.Serializable;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.netlist.Port;
import com.sun.electric.tool.ncc.netlist.Wire;

public abstract class NetObjReport implements Serializable {
	private final String instanceDescription;
	private final String fullDescription;
	private final String name;
	NetObjReport(NetObject n) {
		instanceDescription = n.instanceDescription();
		fullDescription = n.fullDescription();
		name = n.getName();
	}
	public String fullDescription() {return fullDescription;}
	public String instanceDescription() {return instanceDescription;}
	public String getName() {return name;}
	public static NetObjReport newNetObjReport(NetObject no) {
		if (no instanceof Part) return new PartReport((Part)no);
		else if (no instanceof Wire) return new WireReport((Wire)no);
		else if (no instanceof Port) return new PortReport((Port)no);
		LayoutLib.error(true, "unrecognized NetObject");
		return null;
	}
}

package com.sun.electric.tool.ncc.result;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.netlist.Mos;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.netlist.Resistor;
import com.sun.electric.tool.ncc.netlist.NccNameProxy.PartNameProxy;


public class PartReport extends NetObjReport {
	static final long serialVersionUID = 0;
	
	private final PartNameProxy nameProxy;
	private final String typeString;
	private boolean isMos, isResistor;
	private double width, length;
	private void checkLenWidValid() {
		LayoutLib.error(!isMos && !isResistor, 
				        "PartReport has no width or length");
	}
	
	public PartReport(Part p) {
		super(p);
		nameProxy = p.getNameProxy();
		typeString = p.typeString();
		if (p instanceof Mos) {
			isMos = true;
			width = ((Mos)p).getWidth();
			length = ((Mos)p).getLength();
		} else if (p instanceof Resistor) {
			isResistor = true;
			width = ((Resistor)p).getWidth();
			length = ((Resistor)p).getLength();
		}
	}
	public PartNameProxy getNameProxy() {return nameProxy;}
	public boolean isMos() {return isMos;}
	public boolean isResistor() {return isResistor;}
	public double getWidth() {
		checkLenWidValid();
		return width;
	}
	public double getLength() {
		checkLenWidValid();
		return length;
	}
	public String getTypeString() {
		return typeString;
	}
}

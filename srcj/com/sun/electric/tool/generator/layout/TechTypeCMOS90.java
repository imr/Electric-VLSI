package com.sun.electric.tool.generator.layout;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.technology.Technology;

public class TechTypeCMOS90 extends TechType {
	private static final long serialVersionUID = 0;
	
	private static final String[] LAYER_NAMES = {"Polysilicon", "Metal-1", 
		"Metal-2", "Metal-3", "Metal-4", "Metal-5", "Metal-6", "Metal-7", "Metal-8", "Metal-9"};

//	private static void error(boolean pred, String msg) {
//		LayoutLib.error(pred, msg);
//	}

	private TechTypeCMOS90() {
		super(Technology.findTechnology("CMOS90"), LAYER_NAMES);
//        error(wellWidth != 14, "wrong value in Tech");
//	    wellWidth = 14;
	    wellSurroundDiff = Double.NaN;
	    gateExtendPastMOS = 3.25;
	    p1Width = 2;
	    p1ToP1Space = 3.6;
	    p1M1Width = Double.NaN;
	    gateToGateSpace = 4;
	    gateToDiffContSpace = 5.6 - 5.2/2 - 2/2;
	    diffContWidth = 5.2;
        gateLength = 2;
        offsetLShapePolyContact = 2.5 /* half poly contact height */ - 1 /*half poly arc width*/;
        offsetTShapePolyContact = 2.5 /* half poly contact height */ + 1 /*half poly arc width*/;
        selectSpace = 4.8; // TSMC rule, see CMOS90.java
        selectSurroundDiffInTrans = 1.3;
        selectSurround = 4.4;
        selectSurroundDiffInActiveContact = Double.NaN;
        selectSurroundDiffAlongGateInTrans = 3.6;
	}

	/** Singleton class */
	public static final TechType CMOS90 = new TechTypeCMOS90();
	// Singleton class: Don't deserialize
    private Object readResolve() {return CMOS90;}
	
	@Override
	public double roundToGrid(double x) {return x;}
	
	@Override
	public MosInst newNmosInst(double x, double y, 
							   double w, double l, Cell parent) {
		return new MosInst.MosInstV('n', x, y, w, l, parent);
	}
	@Override
	public MosInst newPmosInst(double x, double y, 
							   double w, double l, Cell parent) {
		return new MosInst.MosInstV('p', x, y, w, l, parent);
	}
	@Override
	public String name() {return "CMOS90";}
    @Override
    public int getNumMetals() {return 9;}

}

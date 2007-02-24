package com.sun.electric.tool.generator.layout;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.technology.Technology;

public class TechTypeTSMC180 extends TechType {
	private static final long serialVersionUID = 0;
	
	private static final String[] LAYER_NAMES = {"Polysilicon-1", "Metal-1", 
	    "Metal-2", "Metal-3", "Metal-4", "Metal-5", "Metal-6"};

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	private TechTypeTSMC180() {
		super(Technology.findTechnology("MOCMOS"), LAYER_NAMES);
        error(wellWidth != 17, "wrong value in Tech");
	    wellSurroundDiff = 4.3;
	    gateExtendPastMOS = 2.5;
	    p1Width = 1.8;
	    p1ToP1Space = 4.5 - .9 - .9;
	    p1M1Width = 5;
	    gateToGateSpace = 3;
	    gateToDiffContSpace = 4.5 - 2.5 - .9;
	    diffContWidth = 5;
        gateLength = 1.8;
        offsetLShapePolyContact = 2.5 /* half poly contact height */ - 0.9 /*half poly arc width*/;
        offsetTShapePolyContact = 2.5 /* half poly contact height */ + 0.9 /*half poly arc width*/;
        selectSpace = 4.4;
        selectSurroundDiffInTrans = 1.8;
        selectSurround = 0;
        selectSurroundDiffInActiveContact = 1;
        selectSurroundDiffAlongGateInTrans = 3.6;
	}
	
	/** Singleton class */
	public static final TechType TSMC180 = new TechTypeTSMC180();
	// Singleton class: Don't deserialize
    private Object readResolve() {return TSMC180;}

	@Override
	public double roundToGrid(double x)	{return Math.rint(x * 2) / 2;}

	@Override
	public MosInst newNmosInst(double x, double y, 
							   double w, double l, Cell parent) {
		return new MosInst.MosInstH('n', x, y, w, l, parent);
	}
	@Override
	public MosInst newPmosInst(double x, double y, 
							   double w, double l, Cell parent) {
		return new MosInst.MosInstH('p', x, y, w, l, parent);
	}
	@Override
	public String name() {return "TSMC180";}
    @Override
    public int getNumMetals() {return 6;}

    // for fill generator
    @Override
    public double reservedToLambda(int layer, double nbTracks)
    {
        double m1via = 4;
        double m1sp = 3;
        double m1SP = 6;
        double m6via = 5;
        double m6sp = 4;
        double m6SP = 8;
        if (layer!=6) return 2*m1SP - m1sp + nbTracks*(m1via+m1sp);
        return 2*m6SP - m6sp + nbTracks*(m6via+m6sp);
    }
}

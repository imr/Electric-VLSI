package com.sun.electric.tool.generator.layout;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;

public class TechTypeMoCMOS extends TechType {
	private static final long serialVersionUID = 0;
    private static boolean singletonCreated = false;
	
	private static final String[] LAYER_NAMES = {"Polysilicon-1", "Metal-1", 
	    "Metal-2", "Metal-3", "Metal-4", "Metal-5", "Metal-6"};

	private static void error(boolean pred, String msg) {
		Job.error(pred, msg);
	}
	
	public TechTypeMoCMOS(TechTypeEnum techEnum) {
		super(Technology.getMocmosTechnology(), techEnum, LAYER_NAMES);
		
        // Make sure that not more than one instance of this class gets created
        if (singletonCreated) 
        	throw new RuntimeException("Only one instance of TechTypeMoCMOS is allowed");
        singletonCreated = true;
        
	    wellSurroundDiff = 3;
	    gateExtendPastMOS = 2;
	    p1Width = 2;
	    p1ToP1Space = 3;
	    gateToGateSpace = 3;
	    gateToDiffContSpace = .5;
	    gateToDiffContSpaceDogBone = 1;
        gateLength = 2;
        offsetLShapePolyContact = 2.5 /* half poly contact height */ - 1 /*half poly arc width*/;
        offsetTShapePolyContact = 2.5 /* half poly contact height */ + 1 /*half poly arc width*/;
        selectSpace = 2;
        selectSurroundDiffInTrans = 2;
        selectSurround = -Double.NaN; // no valid value
        selectSurroundDiffInActiveContact = 2;
        selectSurroundDiffAlongGateInTrans = 2;
        m1MinArea = 0;
        diffCont_m1Width = 4;
        diffContIncr = 5;
	}

	@Override
	public double roundToGrid(double x)	{return Math.rint(x * 2) / 2;}

	@Override
	public MosInst newNmosInst(double x, double y, 
							   double w, double l, Cell parent) {
		return new MosInst.MosInstH1('n', x, y, w, l, this, parent);
	}
	@Override
	public MosInst newPmosInst(double x, double y, 
							   double w, double l, Cell parent) {
		return new MosInst.MosInstH1('p', x, y, w, l, this, parent);
	}
	@Override
	public String name() {return "MOCMOS";}
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

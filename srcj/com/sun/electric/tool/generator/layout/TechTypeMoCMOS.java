package com.sun.electric.tool.generator.layout;

import java.io.ObjectStreamException;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.technology.Technology;

public class TechTypeMoCMOS extends TechType {
	private static final long serialVersionUID = 0;
	
	private static final String[] LAYER_NAMES = {"Polysilicon-1", "Metal-1", 
	    "Metal-2", "Metal-3", "Metal-4", "Metal-5", "Metal-6"};

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	
	private TechTypeMoCMOS() {
		super(Technology.findTechnology("MOCMOS"), LAYER_NAMES);
        error(wellWidth != 17, "wrong value in Tech");
	    wellSurroundDiff = 3;
	    gateExtendPastMOS = 2;
	    p1Width = 2;
	    p1ToP1Space = 3;
	    p1M1Width = 5;
	    gateToGateSpace = 3;
	    gateToDiffContSpace = .5;
	    diffContWidth = 5;
        gateLength = 2;
        offsetLShapePolyContact = 2.5 /* half poly contact height */ - 1 /*half poly arc width*/;
        offsetTShapePolyContact = 2.5 /* half poly contact height */ + 1 /*half poly arc width*/;
        selectSpace = 2;
        selectSurroundDiffInTrans = 2;
        selectSurround = -Double.NaN; // no valid value
        selectSurroundDiffInActiveContact = 2;
        selectSurroundDiffAlongGateInTrans = 2;
	}

	/** Singleton class */
	public static final TechType MOCMOS = new TechTypeMoCMOS();
	// Singleton class: Don't deserialize
    private Object readResolve() {return MOCMOS;}

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
	public String name() {return "MOCMOS";}

}

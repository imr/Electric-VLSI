package com.sun.electric.tool.generator.layout;

import com.sun.electric.technology.Technology;

public class TechTypeMoCMOS extends TechType {
	private static final long serialVersionUID = 0;
    private static boolean singletonCreated = false;
	
	public TechTypeMoCMOS() {
		super(Technology.getMocmosTechnology());
		
        // Make sure that not more than one instance of this class gets created
        if (singletonCreated) 
        	throw new RuntimeException("Only one instance of TechTypeMoCMOS is allowed");
        singletonCreated = true;

        assert getNumMetals() == 6;
        assert getTechnology() == Technology.getMocmosTechnology();

        /** arcs */
        assert pdiff().getName().equals("P-Active");
        assert ndiff().getName().equals("N-Active");
        assert p1().getName().equals("Polysilicon-1");
        assert m1().getName().equals("Metal-1");
        assert m2().getName().equals("Metal-2");
        assert m3().getName().equals("Metal-3");
        assert m4().getName().equals("Metal-4");
        assert m5().getName().equals("Metal-5");
        assert m6().getName().equals("Metal-6");
        assert m7() == null;
        assert m8() == null;
        assert m9() == null;
        assert ndiff18() == null;
        assert pdiff18() == null;
        assert ndiff25() == null;
        assert pdiff25() == null;
        assert ndiff33() == null;
        assert pdiff33() == null;
        
        /** pins */
        assert ndpin().getName().equals("N-Active-Pin");
        assert pdpin().getName().equals("P-Active-Pin");
        assert p1pin().getName().equals("Polysilicon-1-Pin");
        assert m1pin().getName().equals("Metal-1-Pin");
        assert m2pin().getName().equals("Metal-2-Pin");
        assert m3pin().getName().equals("Metal-3-Pin");
        assert m4pin().getName().equals("Metal-4-Pin");
        assert m5pin().getName().equals("Metal-5-Pin");
        assert m6pin().getName().equals("Metal-6-Pin");
        assert m7pin() == null;
        assert m8pin() == null;
        assert m9pin() == null;

        /** vias */
        assert nwm1().getName().equals("Metal-1-N-Well-Con");
        assert pwm1().getName().equals("Metal-1-P-Well-Con");
        assert nwm1Y() == null;
        assert pwm1Y() == null;
        assert ndm1().getName().equals("Metal-1-N-Active-Con");
        assert pdm1().getName().equals("Metal-1-P-Active-Con");
        assert p1m1().getName().equals("Metal-1-Polysilicon-1-Con");
        assert m1m2().getName().equals("Metal-1-Metal-2-Con");
        assert m2m3().getName().equals("Metal-2-Metal-3-Con");
        assert m3m4().getName().equals("Metal-3-Metal-4-Con");
        assert m4m5().getName().equals("Metal-4-Metal-5-Con");
        assert m5m6().getName().equals("Metal-5-Metal-6-Con");
        assert m6m7() == null;
        assert m7m8() == null;
        assert m8m9() == null;
        
        /** Transistors */
        assert nmos().getName().equals("N-Transistor");
        assert pmos().getName().equals("P-Transistor");
        assert nmos18().getName().equals("Thick-N-Transistor");
        assert pmos18().getName().equals("Thick-P-Transistor");
        assert nmos25() == null;
        assert pmos25() == null;
        assert nmos33() == null;
        assert pmos33() == null;
        
        /** special threshold transistor contacts */
        assert nmos18contact() == null;
        assert pmos18contact() == null;
        assert nmos25contact() == null;
        assert pmos25contact() == null;
        assert nmos33contact() == null;
        assert pmos33contact() == null;

        /** Well */
        assert nwell().getName().equals("N-Well-Node");
        assert pwell().getName().equals("P-Well-Node");

        /** Layer nodes are sometimes used to patch notches */
        assert m1Node().getName().equals("Metal-1-Node");
        assert m2Node().getName().equals("Metal-2-Node");
        assert m3Node().getName().equals("Metal-3-Node");
        assert m4Node().getName().equals("Metal-4-Node");
        assert m5Node().getName().equals("Metal-5-Node");
        assert m6Node().getName().equals("Metal-6-Node");
        assert m7Node() == null;
        assert m8Node() == null;
        assert m9Node() == null;
        assert p1Node().getName().equals("Polysilicon-1-Node");
        assert pdNode().getName().equals("P-Active-Node");
        assert ndNode().getName().equals("N-Active-Node");
        assert pselNode().getName().equals("P-Select-Node");
        assert nselNode().getName().equals("N-Select-Node");

        /** Transistor layer nodes */
        assert od18() == null;
        assert od25() == null;
        assert od33() == null;
        assert vth() == null;
        assert vtl() == null;

        assert name().equals("MOCMOS");

//	    wellSurroundDiff = 3;
//	    gateExtendPastMOS = 2;
//	    p1Width = 2;
//	    p1ToP1Space = 3;
//	    gateToGateSpace = 3;
//	    gateToDiffContSpace = .5;
	    gateToDiffContSpaceDogBone = 1;
//        gateLength = 2;
//        offsetLShapePolyContact = 2.5 /* half poly contact height */ - 1 /*half poly arc width*/;
//        offsetTShapePolyContact = 2.5 /* half poly contact height */ + 1 /*half poly arc width*/;
//        selectSpace = 2;
//        selectSurroundDiffInTrans = 2;
//        selectSurround = -Double.NaN; // no valid value
//        selectSurroundDiffInActiveContact = 2;
//        selectSurroundDiffAlongGateInTrans = 2;
//        m1MinArea = 0;
//        diffCont_m1Width = 4;
//        diffContIncr = 5;

        assert getWellWidth() == 17;
	    assert getWellSurroundDiffInWellContact() == 3;
	    assert getGateExtendPastMOS() == 2;
	    assert getP1Width() == 2;
	    assert getP1ToP1Space() == 3;
	    assert getGateToGateSpace() == 3;
	    assert getGateToDiffContSpace() == .5;
	    assert getGateToDiffContSpaceDogBone() == 1;
        assert getWellContWidth() == 5;
        assert getDiffContWidth() == 5;
        assert getP1M1Width() == 5;
        assert getGateLength() == 2;
        assert selectSurroundDiffInWellContact() == 2;
        assert selectSurroundDiffInDiffContact() == 2;
        assert selectSurroundDiffAlongGateInTrans() == 2;
        assert getPolyLShapeOffset() == 2.5 /* half poly contact height */ - 1 /*half poly arc width*/;
        assert getPolyTShapeOffset() == 2.5 /* half poly contact height */ + 1 /*half poly arc width*/;
        assert getSelectSpacingRule() == 2;
        assert getSelectSurroundDiffInTrans() == 2;
        assert Double.isNaN(getSelectSurroundOverPoly()); // no valid value
        assert getM1MinArea() == 0;
        assert getDiffCont_m1Width() == 4;
        assert getDiffContIncr() == 5;
	}

	@Override
	public double roundToGrid(double x)	{return Math.rint(x * 2) / 2;}

	@Override
	public String name() {return "MOCMOS";}

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

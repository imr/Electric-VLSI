package com.sun.electric.tool.generator.layout.gates;

import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.database.hierarchy.Cell;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Oct 29, 2004
 * Time: 5:22:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class MoCMOSGenerator {


    /**
     * Generate a Gate
     * @param gateType the gate type ("inv", "nand2", etc)
     * @param Xstrength the drive strength of the gate
     * @param sc the standard cell parameters
     * @return the generated Cell
     */
    public static Cell makeGate(String gateType, double Xstrength, StdCellParams sc) {

        double x = Xstrength;
        String pNm = gateType;
        Cell gate = null;

        if      (pNm == null)                 return null;
        else if (pNm.equals("nms1"))          gate = Nms1.makePart(x, sc);
        else if (pNm.equals("nms1K"))         gate = Nms1.makePart(x, sc);
        else if (pNm.equals("nms2"))          gate = Nms2.makePart(x, sc);
        else if (pNm.equals("nms2_sy"))       gate = Nms2_sy.makePart(x, sc);
        else if (pNm.equals("nms3_sy3"))      gate = Nms3_sy3.makePart(x, sc);
        else if (pNm.equals("pms1"))          gate = Pms1.makePart(x, sc);
        else if (pNm.equals("pms1K"))         gate = Pms1.makePart(x, sc);
        else if (pNm.equals("pms2"))          gate = Pms2.makePart(x, sc);
        else if (pNm.equals("pms2_sy"))       gate = Pms2_sy.makePart(x, sc);
        else if (pNm.equals("inv"))           gate = Inv.makePart(x, sc);
        else if (pNm.equals("invCTLn"))       gate = InvCTLn.makePart(x, sc);
        else if (pNm.equals("inv_passgate"))  gate = Inv_passgate.makePart(x, sc);
        else if (pNm.equals("inv2i"))         gate = Inv2i.makePart(x, sc);
        else if (pNm.equals("inv2iKp"))       gate = Inv2iKp.makePart(x, sc);
        else if (pNm.equals("inv2iKn"))       gate = Inv2iKn.makePart(x, sc);
        else if (pNm.equals("invLT"))         gate = InvLT.makePart(x, sc);
        else if (pNm.equals("invHT"))         gate = InvHT.makePart(x, sc);
        else if (pNm.equals("invCLK"))        gate =  InvCLK.makePart(x, sc);
        else if (pNm.equals("nand2"))         gate = Nand2.makePart(x, sc);
        else if (pNm.equals("nand2k"))        gate = Nand2.makePart(x, sc);
        else if (pNm.equals("nand2en"))       gate = Nand2en.makePart(x, sc);
        else if (pNm.equals("nand2en_sy"))    gate = Nand2en_sy.makePart(x, sc);
        else if (pNm.equals("nand2HLT"))      gate = Nand2HLT.makePart(x, sc);
        else if (pNm.equals("nand2LT"))       gate = Nand2LT.makePart(x, sc);
        else if (pNm.equals("nand2_sy"))      gate = Nand2_sy.makePart(x, sc);
        else if (pNm.equals("nand2HLT_sy"))   gate = Nand2HLT_sy.makePart(x, sc);
        else if (pNm.equals("nand2LT_sy"))    gate = Nand2LT_sy.makePart(x, sc);
        else if (pNm.equals("nand2PH"))       gate = Nand2PH.makePart(x, sc);
        else if (pNm.equals("nand2PHfk"))     gate = Nand2PHfk.makePart(x, sc);
        else if (pNm.equals("nand3"))         gate = Nand3.makePart(x, sc);
        else if (pNm.equals("nand3MLT"))      gate = Nand3MLT.makePart(x, sc);
        else if (pNm.equals("nand3LT"))       gate = Nand3LT.makePart(x, sc);
        else if (pNm.equals("nand3_sy3"))     gate = Nand3_sy3.makePart(x, sc);
        else if (pNm.equals("nand3en_sy"))    gate = Nand3en_sy.makePart(x, sc);
        else if (pNm.equals("nand3LT_sy3"))   gate = Nand3LT_sy3.makePart(x, sc);
        else if (pNm.equals("nand3en_sy3"))   gate = Nand3en_sy3.makePart(x, sc);
        else if (pNm.equals("nor2"))          gate = Nor2.makePart(x, sc);
        else if (pNm.equals("nor2LT"))        gate = Nor2LT.makePart(x, sc);
        else if (pNm.equals("nor2kresetV"))   gate = Nor2kresetV.makePart(x, sc);

        return gate;
    }

    private static int gateNb;

    private static void tracePass(double x) {
        System.out.println("\nbegin pass x="+x);
        gateNb=0;
    }

    private static void traceGate() {
        System.out.print(" "+gateNb++);
        System.out.flush();
    }

    /**
    * Generates all gates with the given size and standard cell params
    * @param x the drive strength
    * @param stdCell the standard cell parameters
    */
    public static void generateAllGates(double x, StdCellParams stdCell) {
        tracePass(x);

        MullerC_sy.makePart(x, stdCell); 	traceGate();
        Nms1.makePart(x, stdCell); 			traceGate();
        Nms2.makePart(x, stdCell); 			traceGate();
        Nms2_sy.makePart(x, stdCell); 		traceGate();
        Nms3_sy3.makePart(x, stdCell); 		traceGate();
        Pms1.makePart(x, stdCell); 			traceGate();
        Pms2.makePart(x, stdCell); 			traceGate();
        Pms2_sy.makePart(x, stdCell); 		traceGate();
        Inv_passgate.makePart(x, stdCell); 	traceGate();
        Inv.makePart(x, stdCell); 			traceGate();
        Inv2_star.makePart(x, "", stdCell); traceGate();
        InvCTLn.makePart(x, stdCell); 		traceGate();
        InvLT.makePart(x, stdCell); 		traceGate();
        InvHT.makePart(x, stdCell); 		traceGate();
        Inv2iKp.makePart(x, stdCell); 		traceGate();
        Inv2iKn.makePart(x, stdCell); 		traceGate();
        Inv2i.makePart(x, stdCell); 		traceGate();
        Nor2.makePart(x, stdCell); 			traceGate();
        //Nor2LT.makePart(x, stdCell); 		traceGate(); no purple schematic
        Nor2kresetV.makePart(x, stdCell); 	traceGate();
        Nand2.makePart(x, stdCell); 		traceGate();
        Nand2en.makePart(x, stdCell); 		traceGate();
        Nand2PH.makePart(x, stdCell); 		traceGate();
        //Nand2HLT.makePart(x, stdCell); 	traceGate(); no purple schematic
        //Nand2PHfk.makePart(x, stdCell); 	traceGate(); mirroring doesn't work
        Nand2LT.makePart(x, stdCell); 		traceGate();
        Nand2_sy.makePart(x, stdCell); 		traceGate();
        Nand2HLT_sy.makePart(x, stdCell); 	traceGate();
        Nand2LT_sy.makePart(x, stdCell); 	traceGate();
        //Nand2en_sy.makePart(x, stdCell); 	traceGate(); no purple schematic
        Nand3.makePart(x, stdCell); 		traceGate();
        Nand3LT.makePart(x, stdCell); 		traceGate();
        Nand3MLT.makePart(x, stdCell); 		traceGate();
        //Nand3_sy3.makePart(x, stdCell); 	traceGate(); no purple schematic
        Nand3LT_sy3.makePart(x, stdCell); 	traceGate();
        //Nand3en_sy.makePart(x, stdCell); 	traceGate(); // real NCC mismatch
        //Nand3LTen_sy.makePart(x, stdCell); traceGate(); // real NCC mismatch
        Nand3en.makePart(x, stdCell); 		traceGate();
        Nand3LTen.makePart(x, stdCell); 	traceGate();

//		//if (x>=1.7) Nand3en_sy3.makePart(x, stdCell);
//		//if (x>=2.5) Nand3LTen_sy3.makePart(x, stdCell);
//
//		// Test gates that can double strap MOS gates
//		stdCell.setDoubleStrapGate(true);
//		Inv.makePart(x, stdCell); traceGate();
//		InvLT.makePart(x, stdCell); traceGate();
//		InvHT.makePart(x, stdCell); traceGate();
//		Nms1.makePart(x, stdCell); traceGate();
//		Pms1.makePart(x, stdCell); traceGate();
//		stdCell.setDoubleStrapGate(false);
    }

}

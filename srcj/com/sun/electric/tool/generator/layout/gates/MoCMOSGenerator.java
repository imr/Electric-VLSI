/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MoCMOSGenerator.java
 * Written by: Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.generator.layout.gates;

import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.database.hierarchy.Cell;

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
		else if (pNm.equals("inv"))           gate = Inv.makePart(x, sc);
		else if (pNm.equals("inv2i"))         gate = Inv2i.makePart(x, sc);
		else if (pNm.equals("inv2iKn"))       gate = Inv2iKn.makePart(x, sc);
		else if (pNm.equals("inv2iKp"))       gate = Inv2iKp.makePart(x, sc);
		else if (pNm.equals("invCLK"))        gate = InvCLK.makePart(x, sc);
		else if (pNm.equals("invCTLn"))       gate = InvCTLn.makePart(x, sc);
		else if (pNm.equals("invHT"))         gate = InvHT.makePart(x, sc);
		else if (pNm.equals("invLT"))         gate = InvLT.makePart(x, sc);
		else if (pNm.equals("inv_passgate"))  gate = Inv_passgate.makePart(x, sc);
		else if (pNm.equals("mullerC_sy"))	  gate = MullerC_sy.makePart(x, sc);
		else if (pNm.equals("nand2"))         gate = Nand2.makePart(x, sc);
		//else if (pNm.equals("nand2HLT"))      gate = Nand2HLT.makePart(x, sc);
		else if (pNm.equals("nand2HLT_sy"))   gate = Nand2HLT_sy.makePart(x, sc);
		else if (pNm.equals("nand2LT"))       gate = Nand2LT.makePart(x, sc);
		else if (pNm.equals("nand2LT_sy"))    gate = Nand2LT_sy.makePart(x, sc);
		else if (pNm.equals("nand2PH"))       gate = Nand2PH.makePart(x, sc);    
		//else if (pNm.equals("nand2PHfk"))     gate = Nand2PHfk.makePart(x, sc);    
		else if (pNm.equals("nand2_sy"))      gate = Nand2_sy.makePart(x, sc);
		else if (pNm.equals("nand2en"))       gate = Nand2en.makePart(x, sc);
		else if (pNm.equals("nand2LTen"))     gate = Nand2LTen.makePart(x, sc);
		else if (pNm.equals("nand2HTen"))     gate = Nand2HTen.makePart(x, sc);
		//else if (pNm.equals("nand2en_sy"))    gate = Nand2en_sy.makePart(x, sc);
		else if (pNm.equals("nand3"))         gate = Nand3.makePart(x, sc);
		else if (pNm.equals("nand3LT"))       gate = Nand3LT.makePart(x, sc);
		else if (pNm.equals("nand3LT_sy3"))   gate = Nand3LT_sy3.makePart(x, sc);
		else if (pNm.equals("nand3LTen"))     gate = Nand3LTen.makePart(x, sc);
		//else if (pNm.equals("nand3LTen_sy"))  gate = Nand3LTen_sy.makePart(x, sc);
		//else if (pNm.equals("nand3LTen_sy3")) gate = Nand3LTen_sy3.makePart(x, sc);
		else if (pNm.equals("nand3MLT"))      gate = Nand3MLT.makePart(x, sc);
		//else if (pNm.equals("nand3_sy3"))     gate = Nand3_sy3.makePart(x, sc);
		else if (pNm.equals("nand3en"))       gate = Nand3en.makePart(x, sc);
		//else if (pNm.equals("nand3en_sy"))    gate = Nand3en_sy.makePart(x, sc);
		//else if (pNm.equals("nand3en_sy3"))   gate = Nand3en_sy3.makePart(x, sc);
		else if (pNm.equals("nms1"))          gate = Nms1.makePart(x, sc);
		else if (pNm.equals("nms2"))          gate = Nms2.makePart(x, sc);
		else if (pNm.equals("nms2_sy"))       gate = Nms2_sy.makePart(x, sc);
		else if (pNm.equals("nms3_sy3"))      gate = Nms3_sy3.makePart(x, sc);
		else if (pNm.equals("nor2"))          gate = Nor2.makePart(x, sc);
		//else if (pNm.equals("nor2LT"))        gate = Nor2LT.makePart(x, sc);
		else if (pNm.equals("nor2kresetV"))   gate = Nor2kresetV.makePart(x, sc);
		else if (pNm.equals("pms1"))          gate = Pms1.makePart(x, sc);
		else if (pNm.equals("pms2"))          gate = Pms2.makePart(x, sc);
		else if (pNm.equals("pms2_sy"))       gate = Pms2_sy.makePart(x, sc);

		// The layout of these keeper gates are identical to normal gates. 
		// They're just treated differently by Logical Effort tool.
		else if (pNm.equals("invK"))          gate = Inv.makePart(x, sc);
		else if (pNm.equals("nand2k"))        gate = Nand2.makePart(x, sc);
		else if (pNm.equals("nms1K"))         gate = Nms1.makePart(x, sc);
		else if (pNm.equals("nms2K"))         gate = Nms2.makePart(x, sc);
		else if (pNm.equals("pms1K"))         gate = Pms1.makePart(x, sc);
		
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
		Inv.makePart(x, stdCell); 			traceGate();
		Inv_star_wideOutput.makePart(x, "", stdCell); traceGate();
		Inv2i.makePart(x, stdCell); 		traceGate();
		Inv2iKn.makePart(x, stdCell); 		traceGate();
		Inv2iKp.makePart(x, stdCell); 		traceGate();
		InvCLK.makePart(x, stdCell); 		traceGate();
		InvCTLn.makePart(x, stdCell); 		traceGate();
		InvHT.makePart(x, stdCell); 		traceGate();
		InvLT.makePart(x, stdCell); 		traceGate();
		Inv_passgate.makePart(x, stdCell); 	traceGate();
		MullerC_sy.makePart(x, stdCell); 	traceGate();
		Nand2.makePart(x, stdCell); 		traceGate();
		//Nand2HLT.makePart(x, stdCell); 		traceGate(); //no purple schematic
		Nand2HLT_sy.makePart(x, stdCell); 	traceGate();
		Nand2LT.makePart(x, stdCell); 		traceGate();
		Nand2LT_sy.makePart(x, stdCell); 	traceGate();
		Nand2PH.makePart(x, stdCell); 		traceGate();
		//Nand2PHfk.makePart(x, stdCell); 	traceGate(); //DRC error because mirroring doesn't work
		Nand2_sy.makePart(x, stdCell); 		traceGate();
		Nand2en.makePart(x, stdCell); 		traceGate();
		//Nand2en_sy.makePart(x, stdCell); 	traceGate(); // no purple schematic
		Nand3.makePart(x, stdCell); 		traceGate();
		Nand3LT.makePart(x, stdCell); 		traceGate();
		Nand3LT_sy3.makePart(x, stdCell); 	traceGate();
		Nand3LTen.makePart(x, stdCell); 	traceGate();
		//Nand3LTen_sy.makePart(x, stdCell); 	traceGate(); // real NCC mismatch
		//Nand3LTen_sy3.makePart(x, stdCell);	traceGate(); // no purple schematic
		Nand3MLT.makePart(x, stdCell); 		traceGate();
		//Nand3_sy3.makePart(x, stdCell); 	traceGate(); //no purple schematic
		Nand3en.makePart(x, stdCell); 		traceGate();
		//Nand3en_sy.makePart(x, stdCell); 	traceGate(); // real NCC mismatch
		//Nand3en_sy3.makePart(x, stdCell); 	traceGate();// no purple schematic
		Nms1.makePart(x, stdCell); 			traceGate();
		Nms2.makePart(x, stdCell); 			traceGate();
		Nms2_sy.makePart(x, stdCell); 		traceGate();
		Nms3_sy3.makePart(x, stdCell); 		traceGate(); //DRC error: doesn't fit in 42 lambda 
		Nor2.makePart(x, stdCell); 			traceGate();
		//Nor2LT.makePart(x, stdCell); 		traceGate(); //no purple schematic
		Nor2kresetV.makePart(x, stdCell); 	traceGate();
		Pms1.makePart(x, stdCell); 			traceGate();
		Pms2.makePart(x, stdCell); 			traceGate();
		Pms2_sy.makePart(x, stdCell); 		traceGate();

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

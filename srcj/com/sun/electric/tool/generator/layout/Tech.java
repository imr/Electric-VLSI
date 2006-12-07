/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Tech.java
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
package com.sun.electric.tool.generator.layout;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;

/**
 * The Tech class used to hold global data. I did this when I initially 
 * constructed the gate layout generators because I thought it would be too
 * cumbersome to pass around a "tech" parameter.
 * However, I've since been modifying Tech.java to make it easier to eliminate
 * this global data. 
 */
public class Tech {
	//---------------------------- private data ----------------------------------
	private static TechType techType = null;

	//----------------------------- public data ----------------------------------
	public static final Variable.Key ATTR_X = Variable.newKey("ATTR_X");
	public static final Variable.Key ATTR_S = Variable.newKey("ATTR_S");
	public static final Variable.Key ATTR_SN = Variable.newKey("ATTR_SN");
	public static final Variable.Key ATTR_SP = Variable.newKey("ATTR_SP");

	//----------------------------- public methods  ------------------------------

    /** layers */
	public static ArcProto pdiff() {return techType.pdiff();}
	public static ArcProto ndiff() {return techType.ndiff();}
	public static ArcProto p1() {return techType.p1();}
	public static ArcProto m1() {return techType.m1();}
	public static ArcProto m2() {return techType.m2();}
	public static ArcProto m3() {return techType.m3();}
	public static ArcProto m4() {return techType.m4();}
	public static ArcProto m5() {return techType.m5();}
	public static ArcProto m6() {return techType.m6();}
	public static ArcProto m7() {return techType.m7();}
	public static ArcProto m8() {return techType.m8();}
	public static ArcProto m9() {return techType.m9();}
	public static ArcProto ndiff18() {return techType.ndiff18();}
	public static ArcProto pdiff18() {return techType.pdiff18();}
	public static ArcProto ndiff25() {return techType.ndiff25();}
	public static ArcProto pdiff25() {return techType.pdiff25();}
	public static ArcProto ndiff33() {return techType.ndiff33();}
	public static ArcProto pdiff33() {return techType.pdiff33();}

	/** pins */
	public static PrimitiveNode ndpin() {return techType.ndpin();}
	public static PrimitiveNode pdpin() {return techType.pdpin();}
	public static PrimitiveNode p1pin() {return techType.p1pin();}
	public static PrimitiveNode m1pin() {return techType.m1pin();}
	public static PrimitiveNode m2pin() {return techType.m2pin();}
	public static PrimitiveNode m3pin() {return techType.m3pin();}
	public static PrimitiveNode m4pin() {return techType.m4pin();}
	public static PrimitiveNode m5pin() {return techType.m5pin();}
	public static PrimitiveNode m6pin() {return techType.m6pin();}
	public static PrimitiveNode m7pin() {return techType.m7pin();}
	public static PrimitiveNode m8pin() {return techType.m8pin();}
	public static PrimitiveNode m9pin() {return techType.m9pin();}

	/** vias */
	public static PrimitiveNode nwm1() {return techType.nwm1();}
	public static PrimitiveNode pwm1() {return techType.pwm1();}
	public static PrimitiveNode nwm1Y() {return techType.nwm1Y();}
	public static PrimitiveNode pwm1Y() {return techType.pwm1Y();}
	public static PrimitiveNode ndm1() {return techType.ndm1();}
	public static PrimitiveNode pdm1() {return techType.pdm1();}
	public static PrimitiveNode p1m1() {return techType.p1m1();}
	public static PrimitiveNode m1m2() {return techType.m1m2();}
	public static PrimitiveNode m2m3() {return techType.m2m3();}
	public static PrimitiveNode m3m4() {return techType.m3m4();}
	public static PrimitiveNode m4m5() {return techType.m4m5();}
	public static PrimitiveNode m5m6() {return techType.m5m6();}
	public static PrimitiveNode m6m7() {return techType.m6m7();}
	public static PrimitiveNode m7m8() {return techType.m7m8();}
	public static PrimitiveNode m8m9() {return techType.m8m9();}

	/** Transistors */
	public static PrimitiveNode nmos() {return techType.nmos();}
	public static PrimitiveNode pmos() {return techType.pmos();}
	public static PrimitiveNode nmos18() {return techType.nmos18();}
	public static PrimitiveNode pmos18() {return techType.pmos18();}
	public static PrimitiveNode nmos25() {return techType.nmos25();}
	public static PrimitiveNode pmos25() {return techType.pmos25();}
	public static PrimitiveNode nmos33() {return techType.nmos33();}
	public static PrimitiveNode pmos33() {return techType.pmos33();}
	
    /** special threshold transistor contacts */
    public static PrimitiveNode nmos18contact() {return techType.nmos18contact();}
    public static PrimitiveNode pmos18contact() {return techType.pmos18contact();}
    public static PrimitiveNode nmos25contact() {return techType.nmos25contact();}
    public static PrimitiveNode pmos25contact() {return techType.pmos25contact();}
    public static PrimitiveNode nmos33contact() {return techType.nmos33contact();}
    public static PrimitiveNode pmos33contact() {return techType.pmos33contact();}

	/** Well */
	public static PrimitiveNode nwell() {return techType.nwell();} 
	public static PrimitiveNode pwell() {return techType.pwell();} 

	/** Layer nodes are sometimes used to patch notches */
	public static PrimitiveNode m1Node() {return techType.m1Node();} 
	public static PrimitiveNode m2Node() {return techType.m2Node();} 
	public static PrimitiveNode m3Node() {return techType.m3Node();} 
	public static PrimitiveNode m4Node() {return techType.m4Node();} 
	public static PrimitiveNode m5Node() {return techType.m5Node();} 
	public static PrimitiveNode m6Node() {return techType.m6Node();} 
	public static PrimitiveNode m7Node() {return techType.m7Node();} 
	public static PrimitiveNode m8Node() {return techType.m8Node();} 
	public static PrimitiveNode m9Node() {return techType.m9Node();} 
	public static PrimitiveNode p1Node() {return techType.p1Node();} 
	public static PrimitiveNode pdNode() {return techType.pdNode();} 
	public static PrimitiveNode ndNode() {return techType.ndNode();} 
	public static PrimitiveNode pselNode() {return techType.pselNode();} 
	public static PrimitiveNode nselNode() {return techType.nselNode();} 

	/** Transistor layer nodes */
	public static PrimitiveNode od18() {return techType.od18();} 
	public static PrimitiveNode od25() {return techType.od25();} 
	public static PrimitiveNode od33() {return techType.od33();} 
	public static PrimitiveNode vth() {return techType.vth();} 
	public static PrimitiveNode vtl() {return techType.vtl();} 
	
	/** Essential-Bounds */
	public static PrimitiveNode essentialBounds() {return techType.essentialBounds();} 

	/** Facet-Center */
	public static PrimitiveNode facetCenter() {return techType.facetCenter();} 

	/** round to avoid MOCMOS CIF resolution errors */
	public static double roundToGrid(double x) {
		return (is90nm()) ? x : (Math.rint(x * 2) / 2);
	}
	public static TechType.MosInst newNmosInst(double x, double y, 
			                          double w, double l, Cell parent) {
		return techType.newNmosInst(x, y, w, l, parent);
	}
	public static TechType.MosInst newPmosInst(double x, double y, 
                                      double w, double l, Cell parent) {
		return techType.newPmosInst(x, y, w, l, parent);
	}

    public static TechType getTechnology() { return techType; }

    /** Change to a new technology. If the requested technology is the same
     * as the current technology then do nothing.
     * @param requestedTechnology technology to change to.
     */
    public static void setTechnology(TechType requestedTechnology) {
		if (requestedTechnology == techType) return;
		
        techType = requestedTechnology;
	}

    public static boolean is90nm() {return techType==TechType.CMOS90;}

    public static PrimitiveNode getViaFor(ArcProto a1, ArcProto a2) {
    	return techType.getViaFor(a1, a2);
    }
    
	/** layer may only be poly or metal */
	public static ArcProto closestLayer(PortProto port, ArcProto layer) {
		return techType.closestLayer(port, layer);
	}
	public static int layerHeight(ArcProto p) {
		return techType.layerHeight(p);
	}
	public static PrimitiveNode viaAbove(int layHeight) {
		return techType.viaAbove(layHeight);
	}
	public static ArcProto layerAtHeight(int layHeight) {
		return techType.layerAtHeight(layHeight);
	}
	
	/** Here is my first attempt to embed design rules into Tech. This is useful for
	 * distinguising MoCMOS from tsmc180. RKao */

	/** @return min width of Well */
	public static double getWellWidth() {return techType.wellWidth;}
	/** @return amount that well surrounds diffusion */
	public static double getWellSurroundDiff() {return techType.wellSurroundDiff;}
	/** @return MOS edge to gate edge */
    public static double getGateExtendPastMOS() {return techType.gateExtendPastMOS;}
    /** @return min width of polysilicon 1 */
    public static double getP1Width() {return techType.p1Width;}
    /** @return min spacing between polysilicon 1 */
    public static double getP1ToP1Space() {return techType.p1ToP1Space;}
    /** @return min spacing between gates of series transistors */ 
    public static double getGateToGateSpace() {return techType.gateToGateSpace;}
    /** @return min spacing between MOS gate and diffusion edge of diff contact */
    public static double getGateToDiffContSpace() {return techType.gateToDiffContSpace;}
    /** @return min width of diffusion surrounding diff contact */
    public static double getDiffContWidth() {return techType.diffContWidth;}
    /** @return min width of poly contact */
    public static double getP1M1Width() {return techType.p1M1Width;}
    /** @return gate length that depends on foundry */
    public static double getGateLength() {return techType.gateLength;}
    /** @return amount that select surrounds diffusion in well? */
    public static double selectSurroundDiffInActiveContact() {return techType.selectSurroundDiffInActiveContact;}
    /** @return amount that Select surrounds MOS, along gate width dimension */
    public static double selectSurroundDiffAlongGateInTrans() {return techType.selectSurroundDiffAlongGateInTrans;}
    /** @return y offset of poly arc connecting poly contact and gate in a L-Shape case */
    public static double getPolyLShapeOffset() {return techType.offsetLShapePolyContact;}
    /** @return y offset of poly arc connecting poly contact and gate in a T-Shape case */
    public static double getPolyTShapeOffset() {return techType.offsetTShapePolyContact;}
    /** @return select spacing rule */
    public static double getSelectSpacingRule() {return techType.selectSpace;}
    /** @return select surround active in transistors but not along the gate */
    public static double getSelectSurroundDiffInTrans() {return techType.selectSurroundDiffInTrans;}
    /** @return selecct surround over poly */
    public static double getSelectSurroundOverPoly() {return techType.selectSurround;}
}


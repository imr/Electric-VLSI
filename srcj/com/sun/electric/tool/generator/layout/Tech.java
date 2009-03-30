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
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;

/**
 * The Tech class used to hold global data. I did this when I initially 
 * constructed the gate layout generators because I thought it would be too
 * cumbersome to pass around a "tech" parameter.
 * However, I've since been modifying Tech.java to make it easier to eliminate
 * this global data. 
 * @Deprecated please use TechType instead
 */
public class Tech {
	//---------------------------- private data ----------------------------------
	private static TechType techType = null;

	//----------------------------- public methods  ------------------------------

    /** layers */
	/** @Deprecated please use TechType instead */
	public static ArcProto pdiff() {return techType.pdiff();}
	/** @Deprecated please use TechType instead */
	public static ArcProto ndiff() {return techType.ndiff();}
	/** @Deprecated please use TechType instead */
	public static ArcProto p1() {return techType.p1();}
	/** @Deprecated please use TechType instead */
	public static ArcProto m1() {return techType.m1();}
	/** @Deprecated please use TechType instead */
	public static ArcProto m2() {return techType.m2();}
	/** @Deprecated please use TechType instead */
	public static ArcProto m3() {return techType.m3();}
	/** @Deprecated please use TechType instead */
	public static ArcProto m4() {return techType.m4();}
	/** @Deprecated please use TechType instead */
	public static ArcProto m5() {return techType.m5();}
	/** @Deprecated please use TechType instead */
	public static ArcProto m6() {return techType.m6();}
	/** @Deprecated please use TechType instead */
	public static ArcProto m7() {return techType.m7();}
	/** @Deprecated please use TechType instead */
	public static ArcProto m8() {return techType.m8();}
	/** @Deprecated please use TechType instead */
	public static ArcProto m9() {return techType.m9();}
	/** @Deprecated please use TechType instead */
	public static ArcProto ndiff18() {return techType.ndiff18();}
	/** @Deprecated please use TechType instead */
	public static ArcProto pdiff18() {return techType.pdiff18();}
	/** @Deprecated please use TechType instead */
	public static ArcProto ndiff25() {return techType.ndiff25();}
	/** @Deprecated please use TechType instead */
	public static ArcProto pdiff25() {return techType.pdiff25();}
	/** @Deprecated please use TechType instead */
	public static ArcProto ndiff33() {return techType.ndiff33();}
	/** @Deprecated please use TechType instead */
	public static ArcProto pdiff33() {return techType.pdiff33();}

	/** pins */
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode ndpin() {return techType.ndpin();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode pdpin() {return techType.pdpin();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode p1pin() {return techType.p1pin();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m1pin() {return techType.m1pin();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m2pin() {return techType.m2pin();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m3pin() {return techType.m3pin();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m4pin() {return techType.m4pin();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m5pin() {return techType.m5pin();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m6pin() {return techType.m6pin();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m7pin() {return techType.m7pin();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m8pin() {return techType.m8pin();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m9pin() {return techType.m9pin();}

	/** vias */
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode nwm1() {return techType.nwm1();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode pwm1() {return techType.pwm1();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode nwm1Y() {return techType.nwm1Y();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode pwm1Y() {return techType.pwm1Y();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode ndm1() {return techType.ndm1();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode pdm1() {return techType.pdm1();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode p1m1() {return techType.p1m1();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m1m2() {return techType.m1m2();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m2m3() {return techType.m2m3();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m3m4() {return techType.m3m4();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m4m5() {return techType.m4m5();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m5m6() {return techType.m5m6();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m6m7() {return techType.m6m7();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m7m8() {return techType.m7m8();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode m8m9() {return techType.m8m9();}

	/** Transistors */
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode nmos() {return techType.nmos();}
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode nmos25() {return techType.nmos25();}
	
    /** special threshold transistor contacts */
	/** @Deprecated please use TechType instead */
    public static PrimitiveNode nmos25contact() {return techType.nmos25contact();}
	/** @Deprecated please use TechType instead */
    public static PrimitiveNode pmos25contact() {return techType.pmos25contact();}

	/** Well */
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode pwell() {return techType.pwell();} 

	/** Layer nodes are sometimes used to patch notches */
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode nselNode() {return techType.nselNode();} 

	/** Transistor layer nodes */
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode od25() {return techType.od25();} 
	
	/** Essential-Bounds */
	/** @Deprecated please use TechType instead */
	public static PrimitiveNode essentialBounds() {return techType.essentialBounds();} 
	
	/** @Deprecated please use TechType instead */
	public static TechType getTechType() {return techType;}

    /** Change to a new technology. If the requested technology is the same
     * as the current technology then do nothing.
     * @param requestedTechnology technology to change to.
     */
	/** @Deprecated please use TechType instead */
    public static void setTechType(TechType requestedTechnology) {
		if (requestedTechnology == techType) return;
		
        techType = requestedTechnology;
	}

	/** @Deprecated please use TechType instead */
    public static boolean is90nm() {return techType==TechType.TechTypeEnum.CMOS90.getTechType();}

	/** @Deprecated please use TechType instead */
    public static PrimitiveNode getViaFor(ArcProto a1, ArcProto a2) {
    	return techType.getViaFor(a1, a2);
    }
    
}


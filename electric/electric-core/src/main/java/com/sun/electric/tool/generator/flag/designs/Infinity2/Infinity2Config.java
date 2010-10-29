/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Infinity2Config.java
 *
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.generator.flag.designs.Infinity2;

import com.sun.electric.tool.generator.flag.FlagConfig;
import com.sun.electric.tool.generator.flag.scan.ScanChain;
import com.sun.electric.tool.generator.layout.TechType;

public class Infinity2Config extends FlagConfig {
	private Infinity2Config() {
	    m2PwrGndWid = 9;
	    m3PwrGndWid = 21;
	    m3PwrGndPitch = 132;
	    signalWid = 2.8;
	    trackPitch = 6;
		pinHeight = 12; 
		rowPitch = 144;
		fillCellWidth = 264;
		minM2Len = 10;

		chains.add(new ScanChain("sid[1:9]", "sod[1:9]", ""));
		chains.add(new ScanChain("sic[1:9]", "soc[1:9]", ""));
		chains.add(new ScanChain("sir[1:9]", "sor[1:9]", ""));

	}
    public TechType tech() {
        return TechType.getCMOS90();
    }
    
	public static Infinity2Config CONFIG = new Infinity2Config();
}

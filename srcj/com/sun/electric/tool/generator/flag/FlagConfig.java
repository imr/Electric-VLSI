/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FlagConfig.java
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
package com.sun.electric.tool.generator.flag;

import java.util.ArrayList;
import java.util.List;

import com.sun.electric.tool.generator.flag.scan.ScanChain;
import com.sun.electric.tool.generator.layout.TechType;

public abstract class FlagConfig {
    public double m2PwrGndWid;
    public double m3PwrGndWid;
    public double m3PwrGndPitch;
    public double signalWid;
    public double trackPitch;
    public double pinHeight; 
    public double rowPitch;
    public double fillCellWidth;
    public double minM2Len;
    
    public List<ScanChain> chains = new ArrayList<ScanChain>();

    public abstract TechType tech();
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: InfinityA.java
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

import java.util.List;

import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.generator.flag.FlagConstructorData;
import com.sun.electric.tool.generator.flag.FlagDesign;
import com.sun.electric.tool.generator.flag.LayoutNetlist;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.LayoutLib.Corner;

/** Physical design for the Ring */
public class InfinityA extends FlagDesign {

	private void stackInsts(List<NodeInst> layInsts) {
        NodeInst prev = null;
        for (NodeInst me : layInsts) {
        	if (prev!=null) {
        		LayoutLib.alignCorners(prev, Corner.TL, me, Corner.BL, 0, 0);
        	}
        	prev = me;
        }
	}

	// Constructor does everything
	public InfinityA(FlagConstructorData data) {
		super(Infinity2Config.CONFIG, data);
		
        LayoutNetlist layNets = createLayoutInstancesFromSchematic(data);
        
        stackInsts(layNets.getLayoutInstancesSortedBySchematicPosition());
        
        addEssentialBounds(layNets.getLayoutCell());

        stitchScanChains(layNets);
        
        routeSignals(layNets);
        
        reexportPowerGround(layNets.getLayoutCell());
        
        reexportSignals(layNets);
        
        addNccVddGndExportsConnectedByParent(layNets.getLayoutCell());
        
	}
}

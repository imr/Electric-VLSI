package com.sun.electric.tool.generator.flag.designs.Infinity2;

import java.util.List;

import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.generator.flag.FlagConstructorData;
import com.sun.electric.tool.generator.flag.FlagDesign;
import com.sun.electric.tool.generator.flag.LayoutNetlist;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.LayoutLib.Corner;

/** Physical design for the Ring */
public class InfinityB extends FlagDesign {

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
	public InfinityB(FlagConstructorData data) {
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

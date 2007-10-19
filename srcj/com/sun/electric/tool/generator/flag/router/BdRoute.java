package com.sun.electric.tool.generator.flag.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.generator.layout.TechType;

/** Brain Dead router. Only do simple interconnections */
public class BdRoute {
	private TechType tech;
	private List<Channel> m2channels, m3channels;
	private BdRoute(TechType tech) {this.tech=tech;}
	
	/** List sorted in increasing Y */
	private List<NodeInst> getStages(Cell layCell) {
		List<NodeInst> stages = new ArrayList<NodeInst>();
		for (Iterator nIt=layCell.getNodes(); nIt.hasNext();) {
			NodeInst ni = (NodeInst) nIt.next();
			if (ni.getProto() instanceof Cell) stages.add(ni);
		}
		// Sort in increasing Y
		Collections.sort(stages, new Comparator<NodeInst>() {
			public int compare(NodeInst n1, NodeInst n2) {
				double delta = n1.getAnchorCenterY() -
				               n2.getAnchorCenterY();
				return (int) Math.signum(delta);
			}
		});
		return stages;
	}
	
	
	private void setUpChannels(Cell layCell) {
		List<NodeInst> stages = getStages(layCell);
		
	}
	
	private void route1(Cell layCell, List<ToConnect> work) {
		
	}
	public static void route(Cell layCell, List<ToConnect> work, TechType tech) {
		BdRoute br = new BdRoute(tech);
		br.route1(layCell, work);
	}
}

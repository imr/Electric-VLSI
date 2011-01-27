package com.sun.electric.tool.routing.metrics;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.technologies.Generic;

import java.util.Iterator;

/**
 * Class to calculate number of unrouted arcs in a cell
 * @author Gilda Garreton
 */
public class UnroutedArcsMetric extends RoutingMetric<Integer>
{
    /* (non-Javadoc)
	 * @see com.sun.electric.tool.routing.metrics.UnroutedArcsMetric#calculate(com.sun.electric.database.hierarchy.Cell)
	 */
	public Integer calculate(Cell cell)
    {
		return processNets(cell, 0);
	}

	/* (non-Javadoc)
	 * @see com.sun.electric.tool.routing.metrics.RoutingMetric#reduce(java.lang.Object, com.sun.electric.database.topology.ArcInst)
	 */
	@Override
	protected Integer reduce(Integer result, ArcInst instance, Network net)
    {
		int isUnrouted = (instance.getProto() == Generic.tech().unrouted_arc) ? 1 : 0;
		return result + isUnrouted;
	}
}

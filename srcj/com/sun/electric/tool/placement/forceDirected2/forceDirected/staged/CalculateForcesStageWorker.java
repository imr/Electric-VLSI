/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CalculateForcesStageWorker.java
 * Written by Team 7: Felix Schmidt
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.placement.forceDirected2.forceDirected.staged;

import com.sun.electric.database.geometry.GenMath.MutableInteger;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.PlacementFrame.PlacementPort;
import com.sun.electric.tool.placement.forceDirected2.AdditionalNodeData;
import com.sun.electric.tool.placement.forceDirected2.PlacementForceDirectedStaged;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.SpringForce;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.CheckboardingField;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.Force2D;
import com.sun.electric.tool.placement.forceDirected2.utils.concurrent.EmptyException;
import com.sun.electric.tool.placement.forceDirected2.utils.concurrent.StageWorker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker to calculate the forces
 */
public class CalculateForcesStageWorker extends StageWorker {

	protected Map<PlacementNode, Map<PlacementNode, MutableInteger>> connectivityMap;
	protected List<PlacementNetwork> allNetworks;

	/**
	 * Constructor
	 * 
	 * @param connectivity
	 * @param allNetworks
	 */
	public CalculateForcesStageWorker(Map<PlacementNode, Map<PlacementNode, MutableInteger>> connectivity, List<PlacementNetwork> allNetworks) {
		this.connectivityMap = connectivity;
		this.allNetworks = allNetworks;
	}

	/**
	 * 
	 * @param node
	 * @return
	 */
	private Force2D calculateForces(PlacementNode node) {
		Force2D result = new Force2D();

		// Map<PlacementNode, MutableInteger> map = connectivityMap.get(node);

		boolean newList = false;

		AdditionalNodeData data = PlacementForceDirectedStaged.getNodeData().get(node);

		if (data.getNetworks() == null) {
			data.setNetworks(new HashMap<PlacementNetwork, PlacementPort>());

			newList = true;
		}

		if (newList) {
			for (PlacementNetwork network : this.allNetworks) {
				for (PlacementPort port : network.getPortsOnNet()) {

					if (node.equals(port.getPlacementNode())) {

						if (newList) {
							data.getNetworks().put(network, port);

						}
					}
				}
			}
		}

		result = new Force2D();
		for (PlacementNetwork network : data.getNetworks().keySet()) {
			for (PlacementPort port : network.getPortsOnNet()) {
				if (node != port.getPlacementNode()) {
					result = result.add(SpringForce.calculate(data.getNetworks().get(network), port, 1, 0));
				}
			}
		}
		return result;
	}

//	private int getMin(Force2D[] forces) {
//		int result = -1;
//		Force2D min = null;
//		int i = 0;
//		for (Force2D force : forces) {
//			if (result == -1) {
//				min = force;
//				result = i;
//			} else {
//				if (force.getLength() < min.getLength()) {
//					min = force;
//					result = i;
//				}
//			}
//			i++;
//		}
//		return result;
//	}

	/**
	 * run method
	 */
	public void run() {

		while (!this.abort.booleanValue()) {
			try {
				PlacementDTO data = this.stage.getInput(this).get();
				List<CheckboardingField> fields = data.getFieldsList();

				if (fields != null) {
					for (CheckboardingField field : fields) {
						if ((field != null) && (field.getNode() != null)) {
							Force2D force = this.calculateForces(field.getNode());
							data.getForces().put(field.getNode(), force);
						}
					}
				}
				this.stage.sendToNextStage(data);
			} catch (EmptyException ex) {
				Thread.yield();
			}
		}
	}
}

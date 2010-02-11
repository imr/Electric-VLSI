/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlaceNodesStageWorker.java
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

import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.CheckboardingField;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.Force2D;
import com.sun.electric.tool.placement.forceDirected2.utils.concurrent.EmptyException;
import com.sun.electric.tool.placement.forceDirected2.utils.concurrent.StageWorker;

import java.util.List;

public class PlaceNodesStageWorker extends StageWorker {

	@SuppressWarnings("unused")
	private double velocity = 0.0;

	public PlaceNodesStageWorker(double velocity) {
		this.velocity = velocity;
	}

	public void run() {

		while (!this.abort.booleanValue()) {

			try {
				PlacementDTO data = this.stage.getInput(this).get();
				List<CheckboardingField> fields = data.getFieldsList();

				if (fields != null) {
					for (CheckboardingField field : fields) {
						if ((field != null) && (field.getNode() != null)) {
							Force2D force = data.getForces().get(field.getNode());
							// System.out.println(force.toString());
							force = force.mult(data.getVelocityFactor());
							PlacementNode node = field.getNode();

							node.setPlacement(node.getPlacementX() + force.getX(), node.getPlacementY() + force.getY());
						}
					}
				}
				this.stage.sendToNextStage(data);

			} catch (EmptyException e) {
				Thread.yield();
			}

		}

	}
}

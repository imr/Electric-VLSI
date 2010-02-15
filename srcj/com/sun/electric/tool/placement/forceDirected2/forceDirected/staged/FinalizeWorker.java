/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FinalizeWorker.java
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
import com.sun.electric.tool.placement.forceDirected2.utils.PlacementProperties;
import com.sun.electric.tool.placement.forceDirected2.utils.concurrent.EmptyException;
import com.sun.electric.tool.placement.forceDirected2.utils.concurrent.StageWorker;

public class FinalizeWorker extends StageWorker {

	private int max;
	private double padding = PlacementProperties.getInstance().getPadding();

	public FinalizeWorker(int max) {
		this.max = max;
	}

	private void placeWithOffsetY(PlacementNode node, double offset) {
		node.setPlacement(node.getPlacementX(), node.getPlacementY() + offset);
	}

	public void run() {
		while (!this.abort.booleanValue()) {
			try {
				PlacementDTO dto = this.stage.getInput(this).get();
				CheckboardingField[][] fields = dto.getFieldPart();

				if (fields != null) {

					for (int i = 0; i < fields.length; i++) {
						if ((fields[i][dto.getIndex()] != null) && (fields[i][dto.getIndex()].getNode() != null)) {
							fields[i][dto.getIndex()].getNode().setPlacement(fields[i][dto.getIndex()].getNode().getPlacementX(), 0);
						}
					}

					for (int i = 1; i < fields.length; i++) {
						CheckboardingField variable = fields[i][dto.getIndex()];
						CheckboardingField fix = null;

						if ((variable != null) && (variable.getNode() != null)) {
							int tmp = i;
							while (((tmp--) > 0) && ((fix == null) || (fix.getNode() == null))) {
								fix = fields[tmp][dto.getIndex()];
							}

							if ((fix != null) && (fix.getNode() != null)) {
								double offset = fix.getNode().getPlacementY() - variable.getNode().getPlacementY();
								offset -= fix.getNode().getHeight() / 2;
								offset -= variable.getNode().getHeight() / 2;
								offset -= this.padding;
								this.placeWithOffsetY(variable.getNode(), offset);
							}
						}
					}
				}
				this.stage.incObjectCounter();
				if (this.stage.getObjectCounter() == this.max) {

					double maxWidth = 0;
					Double x = null;

					for (int i = 0; i < fields[0].length; i++) {

						maxWidth = 0;
						if (i == 0) {
							for (int j = 0; (j < fields.length) && (x == null); j++) {
								if (fields[j][i].getNode() != null) {
									x = new Double(fields[j][i].getNode().getPlacementX());
								}
							}
						} else {

							for (int k = i; (k >= 0) && (maxWidth == 0); k++) {
								for (int j = 0; j < fields.length; j++) {
									PlacementNode tmpNode = fields[j][k - 1].getNode();
									if (tmpNode != null) {
										if (maxWidth < tmpNode.getWidth()) {
											maxWidth = tmpNode.getWidth();
										}
									}
								}
							}

							double maxWidthNew = 0;
							for (int j = 0; j < fields.length; j++) {
								if (fields[j][i].getNode() != null) {
									if (maxWidthNew < fields[j][i].getNode().getWidth()) {
										maxWidthNew = fields[j][i].getNode().getWidth();
									}
								}
							}

							for (int j = 0; j < fields.length; j++) {
								if (fields[j][i].getNode() != null) {
									x = new Double(x.doubleValue() + maxWidth / 2 + maxWidthNew / 2);
									break;
								}
							}
						}
						for (int j = 0; j < fields.length; j++) {
							PlacementNode tmpNode = fields[j][i].getNode();
							if (tmpNode != null) {
								tmpNode.setPlacement(x.doubleValue(), tmpNode.getPlacementY());
							}
						}
					}

					this.stage.stop();
				}
			} catch (EmptyException e) {

			}
		}

	}

}

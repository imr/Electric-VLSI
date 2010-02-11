/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OverlapWorker.java
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
import com.sun.electric.tool.placement.forceDirected2.PlacementForceDirectedStaged;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.OverlapDirection;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.CheckboardingField;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.history.IOverlapHistory;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.history.OverlapHistorySync;
import com.sun.electric.tool.placement.forceDirected2.utils.concurrent.EmptyException;
import com.sun.electric.tool.placement.forceDirected2.utils.concurrent.StageWorker;

public class OverlapWorker extends StageWorker {

	private double threshold;

//	private PlacementNode node = null;
//
//	private Random rand;

	@SuppressWarnings("unused")
	private int index;

	public OverlapWorker(double threshold, int index) {
		this.threshold = threshold;
//		this.rand = new Random(System.currentTimeMillis());
		this.index = index;
	}

	private void exchangeAndPlace(CheckboardingField field1, CheckboardingField field2, CheckboardingField[][] fields) {
		IOverlapHistory<PlacementNode> history = OverlapHistorySync.getInstance();

		PlacementNode node1 = field1.getNode();
		PlacementNode node2 = field2.getNode();

		if ((field1 != field2) && history.isMovementInHistory(node1, node2)) {

			this.exchangeAndPlace(field1, field1, fields);
			this.exchangeAndPlace(field2, field2, fields);

		} else {
			field2.placeCentralized(node1);
			field1.placeCentralized(node2);
			history.saveHistory(node1, node2);
			PlacementForceDirectedStaged.incMovementCounter();

		}

	}

	private void handleOverlappingInX(CheckboardingField[][] fieldPart, int i, int j) {
		double ovX = fieldPart[i][j].getOverlappingFractionInX();
		double ovY = fieldPart[i][j].getOverlappingFractionInY();

		OverlapDirection dir = fieldPart[i][j].getOverlapDirection();

		if (ovX > this.threshold) {

			int newX;
			CheckboardingField newField = null;
			if (OverlapDirection.isEast(dir)) {

				newX = j + 1;
				if (newX < fieldPart[i].length) {
					newField = fieldPart[i][newX];
				}

			} else if (OverlapDirection.isWest(dir)) {

				newX = j - 1;
				if (newX >= 0) {
					newField = fieldPart[i][newX];
				}

			}

			if (newField == null) {
				newField = fieldPart[i][j];
			}

			this.exchangeAndPlace(fieldPart[i][j], newField, fieldPart);

		} else if (ovY > this.threshold) {
			this.handleOverlappingInY(fieldPart, i, j);
		}
	}

	private void handleOverlappingInY(CheckboardingField[][] fieldPart, int i, int j) {
		double ovX = fieldPart[i][j].getOverlappingFractionInX();
		double ovY = fieldPart[i][j].getOverlappingFractionInY();

		OverlapDirection dir = fieldPart[i][j].getOverlapDirection();

		if (ovY > this.threshold) {

			int newY;
			CheckboardingField newField = null;
			if (OverlapDirection.isNorth(dir)) {

				newY = i + 1;
				if (newY < fieldPart.length) {
					newField = fieldPart[newY][j];
				}

			} else if (OverlapDirection.isSouth(dir)) {

				newY = j - 1;
				if (newY >= 0) {
					newField = fieldPart[newY][j];
				}

			}

			if (newField == null) {
				newField = fieldPart[i][j];
			}

			this.exchangeAndPlace(fieldPart[i][j], newField, fieldPart);
		} else if (ovX > this.threshold) {
			this.handleOverlappingInX(fieldPart, i, j);
		}

	}

	public void run() {
		while (!this.abort.booleanValue()) {

			try {
				PlacementDTO data = this.stage.getInput(this).get();
				CheckboardingField[][] fieldPart = data.getFieldPart();

				for (int i = 0; i < fieldPart.length; i++) {
					for (int j = 0; j < fieldPart[i].length; j++) {
						if ((fieldPart[i][j] != null) && (fieldPart[i][j].isOverlappingBiggerThreshold(this.threshold))) {
							int proc = fieldPart[i][j].getCoordX() + fieldPart[i][j].getCoordY();
							if ((proc % 2) == 0) {
								this.handleOverlappingInX(fieldPart, i, j);
							} else {
								this.handleOverlappingInY(fieldPart, i, j);
							}
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
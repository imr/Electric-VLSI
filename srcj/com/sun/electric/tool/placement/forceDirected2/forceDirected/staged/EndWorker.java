/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EndWorker.java
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

import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.CheckboardingField;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.CheckboardingPattern;
import com.sun.electric.tool.placement.forceDirected2.metrics.AbstractMetric;
import com.sun.electric.tool.placement.forceDirected2.utils.GlobalVars;
import com.sun.electric.tool.placement.forceDirected2.utils.concurrent.EmptyException;
import com.sun.electric.tool.placement.forceDirected2.utils.concurrent.StageWorker;
import com.sun.electric.tool.placement.forceDirected2.utils.output.PNGOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EndWorker extends StageWorker {

	private double velocityFactor;
	private Map<PlacementDTO, Integer> elementCounter;
	private Map<PlacementDTO, Integer> formatEnlargeElementCounter;
	private Map<PlacementDTO, Integer> formatEnsmallElementCounter;
	private StartUpStage startUpStage;
	private int width;
	private int height;
	private CheckboardingPattern checkPattern;
	private int currentStep;
	private static long finalTimeStamp;

	public EndWorker(int iterations, StartUpStage stage, int width, int height, CheckboardingPattern checkPattern, double velocity, PNGOutput out,
			AbstractMetric metric, long finalTimeStamp) {
		this.elementCounter = new HashMap<PlacementDTO, Integer>();
		this.formatEnlargeElementCounter = new HashMap<PlacementDTO, Integer>();
		this.formatEnsmallElementCounter = new HashMap<PlacementDTO, Integer>();
		this.startUpStage = stage;
		this.height = height;
		EndWorker.finalTimeStamp = finalTimeStamp;

		this.velocityFactor = velocity * 2;

		this.width = width;
		this.checkPattern = checkPattern;
	}

	private List<PlacementDTO> enlargeDTO() {

		List<PlacementDTO> dtos = new ArrayList<PlacementDTO>();

		int stepWidth = 11;

		CheckboardingField[][] fstFields = this.checkPattern.getFields(0, 0, stepWidth + 1, stepWidth + 1);
		PlacementDTO fstTmp = new PlacementDTO(fstFields, -1);
		fstTmp.setCounter(this.currentStep);
		dtos.add(fstTmp);

		for (int i = stepWidth + 1; i < this.width; i += stepWidth) {
			CheckboardingField[][] fields = this.checkPattern.getFields(i, 0, stepWidth, stepWidth + 1);
			PlacementDTO tmp = new PlacementDTO(fields, -1);
			tmp.setCounter(this.currentStep);
			dtos.add(tmp);
		}

		for (int i = stepWidth + 1, j = 0; i < this.height; i += stepWidth, j++) {
			CheckboardingField[][] fields = this.checkPattern.getFields(0, i, stepWidth + 1, stepWidth);
			PlacementDTO tmp = new PlacementDTO(fields, -1);
			tmp.setCounter(this.currentStep);
			dtos.add(tmp);
		}

		for (int i = stepWidth + 1; i < this.height; i += stepWidth) {
			for (int j = stepWidth + 1; j < this.width; j += stepWidth) {
				CheckboardingField[][] fields = this.checkPattern.getFields(j, i, stepWidth, stepWidth);
				PlacementDTO tmp = new PlacementDTO(fields, -1);
				tmp.setCounter(this.currentStep);
				dtos.add(tmp);
			}
		}

		return dtos;

	}

	private List<PlacementDTO> ensmallDTO() {
		List<PlacementDTO> dtos = new ArrayList<PlacementDTO>();

		int stepWidth = 10; // random.nextInt(10) + 2;
		for (int i = 0; i < this.height; i += stepWidth) {
			for (int j = 0; j < this.width; j += stepWidth) {
				CheckboardingField[][] fields = this.checkPattern.getFields(j, i, stepWidth, stepWidth);
				PlacementDTO tmp = new PlacementDTO(fields, -1);
				tmp.setVelocityFactor(this.velocityFactor);
				tmp.setCounter(this.currentStep);
				dtos.add(tmp);
			}
		}

		return dtos;

	}

	public void run() {

		while (!this.abort.booleanValue()) {

			try {
				PlacementDTO data = this.stage.getInput(this).get();
				data.incCounter();

				this.elementCounter.put(data, new Integer(data.getCounter()));

				// current timestamp
				long now = System.currentTimeMillis();

				if (now < finalTimeStamp) {
					// if (data.getCounter() < iterations) {
					data.setTimestamp(now);
					if ((data.getCounter() % 6) == 0) {
						this.formatEnsmallElementCounter.put(data, new Integer(data.getIndex()));

						if (this.elementCounter.size() == this.formatEnsmallElementCounter.size()) {
							// PlacementForceDirectedStaged.setMovementCounter(0);
							this.currentStep = data.getCounter();
							List<PlacementDTO> dtos = this.ensmallDTO();
							this.elementCounter.clear();

							for (PlacementDTO dto : dtos) {
								this.stage.sendToNextStage(dto);
								this.elementCounter.put(dto, new Integer(dto.getIndex()));
							}

							this.formatEnsmallElementCounter.clear();
						}
					} else if ((data.getCounter() % 5) == 0) {
						this.formatEnlargeElementCounter.put(data, new Integer(data.getIndex()));
						if (this.elementCounter.size() == this.formatEnlargeElementCounter.size()) {
							this.currentStep = data.getCounter();
							List<PlacementDTO> dtos = this.enlargeDTO();
							this.elementCounter.clear();

							for (PlacementDTO dto : dtos) {
								dto.setVelocityFactor(this.velocityFactor);
								this.stage.sendToNextStage(dto);
								this.elementCounter.put(dto, new Integer(dto.getIndex()));
							}

							this.formatEnlargeElementCounter.clear();
						}
					} else {
						this.stage.sendToNextStage(data);
					}

				} else {
					GlobalVars.rounds = new Integer(data.getCounter());
					this.startUpStage.stop();
				}

			} catch (EmptyException e) {
				Thread.yield();
			}

		}
	}

}

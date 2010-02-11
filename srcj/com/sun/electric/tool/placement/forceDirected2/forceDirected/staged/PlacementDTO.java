/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementDTO.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlacementDTO implements Cloneable {

	private CheckboardingField[][] fieldPart;
	private int counter = 0;
	private double velocityFactor = 0;
	private long timestamp = 0;
	private Map<PlacementNode, Force2D> forces;

	private int index = 0;

	public PlacementDTO(CheckboardingField[][] fields, int index) {

		this.fieldPart = fields;
		this.index = index;
		this.timestamp = System.currentTimeMillis();
		this.setForces(new HashMap<PlacementNode, Force2D>());

	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		PlacementDTO result = new PlacementDTO(this.fieldPart, this.index);
		return result;
	}

	public int getCounter() {
		return this.counter;
	}

	public CheckboardingField[][] getFieldPart() {
		return this.fieldPart;
	}

	public List<CheckboardingField> getFieldsList() {
		if (this.fieldPart == null) {
			return null;
		}

		List<CheckboardingField> fields = new ArrayList<CheckboardingField>();

		for (int i = 0; i < this.fieldPart.length; i++) {
			for (int j = 0; j < this.fieldPart[i].length; j++) {
				fields.add(this.fieldPart[i][j]);
			}
		}

		return fields;
	}

	public Map<PlacementNode, Force2D> getForces() {
		return this.forces;
	}

	public int getIndex() {
		return this.index;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public double getVelocityFactor() {
		return this.velocityFactor;
	}

	public void incCounter() {
		this.counter++;
	}

	public void setCounter(int counter) {
		this.counter = counter;
	}

	public void setFieldPart(CheckboardingField[][] fieldPart) {
		this.fieldPart = fieldPart;
	}

	public void setForces(Map<PlacementNode, Force2D> forces) {
		this.forces = forces;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void setVelocityFactor(double velocityFactor) {
		this.velocityFactor = velocityFactor;
	}

}

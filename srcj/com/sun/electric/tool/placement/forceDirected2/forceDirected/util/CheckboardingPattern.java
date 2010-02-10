/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CheckboardingPattern.java
 * Written by Team 7: Felix Schmidt, Daniel Lechner
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
package com.sun.electric.tool.placement.forceDirected2.forceDirected.util;

import java.awt.geom.Point2D;

/**
 * Parallel Placement
 * 
 * Checkboarding Pattern
 */
public class CheckboardingPattern {

	private CheckboardingField[][] field;
	private int width;
	private int height;

	public CheckboardingPattern(int width, int height, double sizeX, double sizeY) {
		this.field = new CheckboardingField[height][width];
		this.width = width;
		this.height = height;

		for (int i = 0; i < this.height; i++) {
			for (int j = 0; j < this.width; j++) {
				Point2D location = new Point2D.Double(j * sizeX, i * sizeY);
				this.field[i][j] = new CheckboardingField(location, sizeX, sizeY, i * width + j, j, i);
			}
		}
	}

	public CheckboardingField[][] getAll() {
		return this.field;
	}

	public CheckboardingField getField(int x, int y) {
		return this.field[y][x];
	}

	public CheckboardingField[][] getFields(int x, int y, int lengthX, int lengthY) {
		CheckboardingField[][] result = new CheckboardingField[lengthY][lengthX];

		for (int i = 0; i < lengthY; i++) {
			for (int j = 0; j < lengthX; j++) {
				if (((y + i) < this.field.length) && ((x + j) < this.field[i].length)) {
					result[i][j] = this.field[y + i][x + j];
				} else {
					result[i][j] = null;
				}
			}
		}

		return result;
	}

}

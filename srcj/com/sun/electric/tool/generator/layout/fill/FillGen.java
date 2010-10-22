/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FillGen.java
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.generator.layout.fill;

import com.sun.electric.technology.Technology;
import com.sun.electric.tool.generator.layout.fill.FillGenConfig.FillGenType;
import com.sun.electric.tool.generator.layout.fill.FillGeneratorTool.Units;
import com.sun.electric.tool.JobException;


/** Fill Generation for bean shell scripts */
public class FillGen {
	private FillGenConfig config;
	private FillGeneratorTool fgt = new FillGeneratorTool();
	
    /** Reserve space in the middle of the Vdd and ground straps for signals.
     * @param layer the layer number. This may be 2, 3, 4, 5, or 6. The layer
     * number 1 is reserved to mean "capacitor between Vdd and ground".
     * @param vddReserved space to reserve in the middle of the central Vdd
     * strap.
     * The value 0 makes the Vdd strap one large strap instead of two smaller
     * adjacent straps.
     * @param vddUnits LAMBDA or TRACKS
     * @param gndReserved space to reserve between the ground strap of this
     * cell and the ground strap of the adjacent fill cell. The value 0 means
     * that these two ground straps should abut to form a single large strap
     * instead of two smaller adjacent straps.
     * @param gndUnits LAMBDA or TRACKS
     * param tiledSizes an array of sizes. The default value is null.  The
     * value null means don't generate anything. */
	public void reserveSpaceOnLayer(int layer,
									double vddReserved, Units vddUnits,
							   		double gndReserved, Units gndUnits) {
		config.reserveSpaceOnLayer(config.getTechType().getTechnology(),
				                   layer, vddReserved, vddUnits, 
				                   gndReserved, gndUnits);
	}
	public FillGen(Technology tech) {
		config = new FillGenConfig(tech, FillGeneratorTool.FillTypeEnum.INVALID,
            null, null, -1, -1,
				                   Double.NaN, Double.NaN, 
				                   false, null, false, Double.NaN, Double.NaN, false, false,
				                   false, Double.NaN, FillGenType.INTERNAL, -1);
	}
	public void setFillLibrary(String libName) {
		config.fillLibName = libName;
	}
	public void setFillCellWidth(double w) {
		config.width = w;
	}
	public void setFillCellHeight(double h) {
		config.height = h;
	}
	public void makeEvenLayersHorizontal(boolean b) {
		config.evenLayersHorizontal = b;
	}
	public void makeFillCell(int loLayer, int hiLayer,
			                 ExportConfig exportConfig,
			                 int[] tiledSizes) {
		fgt.setConfig(config);
		fgt.standardMakeFillCell(loLayer, hiLayer, 
                				 config.getTechType(),
                                 exportConfig, 
                                 tiledSizes, false);
	}
	public void makeGallery() {
		fgt.makeGallery();
	}
	public void writeLibrary(int backupScheme) throws JobException {
		fgt.writeLibrary(backupScheme);
	}
}

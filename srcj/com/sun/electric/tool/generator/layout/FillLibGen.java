/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TrackRouter.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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
package com.sun.electric.tool.generator.layout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;


/**
 * Generate fill cells.
 * Create a library called fillCells. 
 */
public class FillLibGen extends Job {
	public void doIt() {
		System.out.println("Begin FillCell");
		
		// these constants make the following code more compact
		FillGenerator.Units LAMBDA = FillGenerator.LAMBDA;
		FillGenerator.Units TRACKS = FillGenerator.TRACKS;
		FillGenerator.ExportConfig PERIMETER = FillGenerator.PERIMETER;
		FillGenerator.ExportConfig PERIMETER_AND_INTERNAL = FillGenerator.PERIMETER_AND_INTERNAL;
		FillGenerator.PowerType VDD = FillGenerator.VDD;
		FillGenerator.PowerType POWER = FillGenerator.POWER;

		FillGenerator fg = new FillGenerator();
		fg.setFillLibrary("fillLib");
		fg.setFillCellWidth(245);
		fg.setFillCellHeight(175);
		
		fg.setTiledCellSizes(new int[] {2, 3, 4, 5, 10, 12});

		// Arguments to reserveSpaceOnLayer:
		//     layer number, 
		//     vdd space, 
		//     vdd space units (TRACKS or LAMBDA), 
		//     gnd space, 
		//     gnd space units (TRACKS or LAMBDA) 
		fg.reserveSpaceOnLayer(2, 3, TRACKS, 3, TRACKS);
		fg.reserveSpaceOnLayer(3, 3, TRACKS, 3, TRACKS);
		fg.reserveSpaceOnLayer(4, 3, TRACKS, 3, TRACKS);
		fg.reserveSpaceOnLayer(5, 3, TRACKS, 3, TRACKS);
		fg.reserveSpaceOnLayer(6, 2, TRACKS, 2, TRACKS);
		
		// Arguments to makeFillCell:
		//     low layer number
		//     high layer number
		//     export configuration (PERIMETER or PERIMETER_AND_INTERNAL)
		//     power type (VDD or POWER)
		fg.makeFillCell(1, 6, PERIMETER, VDD);
		fg.makeFillCell(2, 6, PERIMETER, VDD);
		fg.makeFillCell(3, 6, PERIMETER, VDD);
		fg.makeFillCell(4, 6, PERIMETER, VDD);
		fg.makeFillCell(5, 6, PERIMETER, VDD);
		fg.makeFillCell(6, 6, PERIMETER, VDD);

		fg.makeFillCell(2, 6, PERIMETER_AND_INTERNAL, VDD);
		fg.makeFillCell(3, 6, PERIMETER_AND_INTERNAL, VDD);
		fg.makeFillCell(4, 6, PERIMETER_AND_INTERNAL, VDD);
		fg.makeFillCell(5, 6, PERIMETER_AND_INTERNAL, VDD);
		fg.makeFillCell(6, 6, PERIMETER_AND_INTERNAL, VDD);

		fg.makeFillCell(1, 4, PERIMETER, POWER);
		fg.makeFillCell(1, 3, PERIMETER, POWER);
		fg.makeFillCell(3, 4, PERIMETER, POWER);
		fg.makeFillCell(3, 4, PERIMETER_AND_INTERNAL, POWER);
		fg.makeFillCell(5, 6, PERIMETER, POWER);
		fg.makeFillCell(5, 6, PERIMETER_AND_INTERNAL, POWER);
		fg.makeFillCell(4, 6, PERIMETER, POWER);
		fg.makeFillCell(4, 6, PERIMETER_AND_INTERNAL, POWER);

		fg.makeGallery();
		
		System.out.println("Done FillCell");
	}
	
	public FillLibGen() {
		super("Generate Fill Cell Library", User.tool, Job.Type.CHANGE, 
			  null, null, Job.Priority.ANALYSIS);
		startJob();
	}
}

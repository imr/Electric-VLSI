/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Gates.java
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
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.Point2D;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.network.*;
import com.sun.electric.technology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;

import com.sun.electric.tool.generator.layout.gates.*;

/*
 * Regression test for gate generators
 */
public class Justin extends Job {
	// specify which gates shouldn't be surrounded by DRC rings
	private static final DrcRings.Filter FILTER = new DrcRings.Filter() {
		public boolean skip(NodeInst ni) {
			// well tie cells don't pass DRC with DrcRings
	        return ni.getProto().getProtoName().indexOf("mosWellTie_") != -1;
		}
	};

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	private static boolean osIsWindows() {
		Properties props = System.getProperties();
		String osName = ((String) props.get("os.name")).toLowerCase();
		return osName.indexOf("windows") != -1;
	}
	
	public void doIt() {
		System.out.println("begin execution of Gates");
		boolean nfsWedged = true;
		String homeDir;
		if (!osIsWindows()) {
			homeDir = "/home/rkao/";
		} else if (nfsWedged) {
			homeDir = "c:/a1/kao/Sun/";
		} else {
			homeDir = "x:/";
		}

		Library scratchLib = 
		    LayoutLib.openLibForModify("fullswing_inverters", 
									   homeDir+"fullswing_inverters");

		StdCellParams stdCell = new StdCellParams(scratchLib);
//		stdCell.enableNCC(
//			homeDir + "work/async/electric-jkl-28august/purpleFour.elib");
		stdCell.setSizeQuantizationError(0.05);
		stdCell.setMaxMosWidth(1000);

		stdCell.setVddY(21);
		stdCell.setGndY(-21);
		stdCell.setNmosWellHeight(42);
		stdCell.setPmosWellHeight(42);
		InvV.makePart(24.4467, 25.3333, stdCell);	// 3x13x50000
		InvV.makePart(51.4667, 53.3333, stdCell);	// 3x13x50000D
		InvV.makePart(25.7333, 26.6667, stdCell);	// 3x13x70850
		InvV.makePart(28.95, 30, stdCell);			// 3x3x33330
		InvV.makePart(32.1667, 33.3333, stdCell);	// 3x3x50000
		InvV.makePart(69.1583, 71.6667, stdCell);	// 3x3x50000D
		InvV.makePart(23.8033, 24.6667, stdCell);	// 3x7x50000
		InvV.makePart(26.3767, 27.3333, stdCell);	// 3x7x60000
		
		Cell gallery = Gallery.makeGallery(scratchLib);
		DrcRings.addDrcRings(gallery, FILTER);
		
		LayoutLib.writeLibrary(scratchLib);

		System.out.println("done.");
	}
	public Justin() {
		super("Build inverter library for Justin", User.tool, Job.Type.CHANGE, 
			  null, null, Job.Priority.ANALYSIS);
		startJob();
	}
}

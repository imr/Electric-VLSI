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
import java.util.HashSet;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.generator.layout.gates.MoCMOSGenerator;
import com.sun.electric.tool.generator.layout.gates90nm.TSMC90Generator;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.technology.Technology;
import com.sun.electric.plugins.tsmc90.TSMC90;

/*
 * Regression test for gate generators
 */
public class GateLayoutGenerator extends Job {
    private Technology technology;

	// specify which gates shouldn't be surrounded by DRC rings
	private static final DrcRings.Filter FILTER = new DrcRings.Filter() {
		public boolean skip(NodeInst ni) {
			// well tie cells don't pass DRC with DrcRings
	        return ni.getProto().getName().indexOf("mosWellTie_") != -1;
		}
	};

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	private Cell findCell(Library lib, String cellName) {
		Cell c = lib.findNodeProto(cellName);
		LayoutLib.error(c==null, "can't find: "+lib.getName()+":"+cellName);
		return c;
	}
	
	private Library generateLayout(Library outLib, Cell cell, Technology technology) {
        StdCellParams stdCell;
        if (technology == TSMC90.tech) {
            Tech.setTechnology(Tech.TSMC90);
            stdCell = sportParams(outLib);
        } else {
            Tech.setTechnology(Tech.MOCMOS);
            stdCell = locoParams(outLib);
        }

		// Due to NCC deficiency in C-Electric, gates with "_pwr" suffix need 
		// power bus assigned Characteristic "IN". This is my hack until I have 
		// a properly functioning version of NCC.
		StdCellParams stdCellPwr = locoParams(outLib);
        // JKG: NCC in Jelectric does not use "power" type: hack removed
		//stdCellPwr.setVddExportName("power");
		//stdCellPwr.setVddExportRole(PortCharacteristic.IN);

		GenerateLayoutForGatesInSchematic visitor =
			new GenerateLayoutForGatesInSchematic(stdCell, stdCellPwr);
		HierarchyEnumerator.enumerateCell(cell, null, null, visitor);
			
		return outLib;
	}
	
	private static StdCellParams locoParams(Library outLib) {
		StdCellParams stdCell = new StdCellParams(outLib, Tech.MOCMOS);
//		stdCell.enableNCC(
//			homeDir + "work/async/electric-jkl-28august/purpleFour.elib");
		stdCell.setSizeQuantizationError(0);
		stdCell.setMaxMosWidth(1000);
		stdCell.setVddY(21);
		stdCell.setGndY(-21);
		stdCell.setNmosWellHeight(42);
		stdCell.setPmosWellHeight(42);
		stdCell.setSimpleName(true);
		return stdCell;
	}

    private static StdCellParams sportParams(Library outLib) {
        StdCellParams stdCell = new StdCellParams(outLib, Tech.TSMC90);
        stdCell.setSizeQuantizationError(0);
        stdCell.setMaxMosWidth(1000);
        stdCell.setVddY(24.5);
        stdCell.setGndY(-24.5);
        stdCell.setNmosWellHeight(84);
        stdCell.setPmosWellHeight(84);
        stdCell.setSimpleName(true);
        return stdCell;
    }

	public boolean doIt() {
		String outLibNm = "autoGenLib";
		String outLibDir = "";
		Library outLib = LayoutLib.openLibForWrite(outLibNm, outLibDir+outLibNm);

		EditWindow wnd = EditWindow.getCurrent();
		Cell cell = wnd.getCell();
		if (cell==null) {
			System.out.println("Please open the schematic for which you " +
				               "want to generate gate layouts.");
			return false;		
		}
		if (!cell.isSchematic()) {
			System.out.println("The current cell isn't a schematic. This " +
				               "command only works on schematics.");
			return false;
		}
		System.out.println("Generating layouts for gates in the schematic: "+
		                   cell.getName()+" and its descendents");
		System.out.println("Output goes to library: autoGenLib");
		//Library outLib = cell.getLibrary();

		generateLayout(outLib, cell, technology);
		Cell gallery = Gallery.makeGallery(outLib);
		DrcRings.addDrcRings(gallery, FILTER);
		
		System.out.println("done.");
		return true;
	}
	public GateLayoutGenerator(Technology technology) {
		super("Generate gate layouts", User.tool, Job.Type.CHANGE, 
			  null, null, Job.Priority.ANALYSIS);
        this.technology = technology;
		startJob();
	}
}

/** Traverse a schematic hierarchy and generate Cells that we recognize. */
class GenerateLayoutForGatesInSchematic extends HierarchyEnumerator.Visitor {
	private final StdCellParams stdCell;
	private final StdCellParams stdCellPwr;
	private final HashSet visitedCells = new HashSet();
	
	/**
	 * Construct a Visitor that will walk a schematic and generate layout for Cells.
	 * @param libraryName the name of the library that contains the Cells for 
	 * which we want to generate layout. For example: "redFour" or "purpleFour".
	 * @param cellNames the names of the Cells for which we want to generate layout
	 */
	public GenerateLayoutForGatesInSchematic(StdCellParams stdCell,
	                          				 StdCellParams stdCellPwr) {
		this.stdCell = stdCell;
		this.stdCellPwr = stdCellPwr;
	}

	private static double getNumericVal(Object val) {
		if (val==null) {
			//System.out.println("null value detected, using 40");
			return -1;
		} if (val instanceof Number) {
			return ((Number)val).doubleValue();
		}
		LayoutLib.error(true, "not a numeric value: "+val);
		return 0;
	}
	
	private void generateCell(Nodable iconInst, VarContext context) {
		String pNm = iconInst.getProto().getName();
		Variable var = iconInst.getVar("ATTR_X");
		double x = getNumericVal(context.evalVar(var, iconInst));
		if (x==-1) return;
		
		StdCellParams sc = stdCell;

		boolean unknown = false;
		
		int pwr = pNm.indexOf("_pwr");
		if (pwr!=-1) {
			pNm = pNm.substring(0, pwr);
			sc = stdCellPwr;
		}

        if (Tech.isTSMC90())
            TSMC90Generator.makeGate(pNm, x, sc);
        else
            MoCMOSGenerator.makeGate(pNm, x, sc);

		//if (!unknown) System.out.println("Gate Type: " + pNm + ", Gate Size: " + x);
	}

	public boolean enterCell(HierarchyEnumerator.CellInfo info) {
		Cell cell = info.getCell();
		if (visitedCells.contains(cell)) return false;
		visitedCells.add(cell);
		return true;
	}
	public void exitCell(HierarchyEnumerator.CellInfo info) {}
	public boolean visitNodeInst(Nodable no,
								 HierarchyEnumerator.CellInfo info) {
		// we never generate layout for PrimitiveNodes
		if (no instanceof NodeInst) return false;
		
		Cell cell = (Cell) no.getProto();
		Library lib = cell.getLibrary();
		String libNm = lib.getName();
		if (libNm.equals("redFour") || libNm.equals("power2_gates")) {
			generateCell(no, info.getContext());	
			return false;
		}
		return true;
	}
}

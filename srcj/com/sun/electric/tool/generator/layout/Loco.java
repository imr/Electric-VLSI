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

import com.sun.electric.database.variable.*;
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
public class Loco extends Job {
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
	private Library doJustin(String homeDir) {
		Library lib = LayoutLib.openLibForWrite("fullswing_inverters", 
									  			homeDir+"fullswing_inverters");
		StdCellParams stdCell = locoParams(lib);
		InvV.makePart(24.4467, 25.3333, stdCell);	// 3x13x50000
		InvV.makePart(51.4667, 53.3333, stdCell);	// 3x13x50000D
		InvV.makePart(25.7333, 26.6667, stdCell);	// 3x13x70850
		InvV.makePart(28.95, 30, stdCell);			// 3x3x33330
		InvV.makePart(32.1667, 33.3333, stdCell);	// 3x3x50000
		InvV.makePart(69.1583, 71.6667, stdCell);	// 3x3x50000D
		InvV.makePart(23.8033, 24.6667, stdCell);	// 3x7x50000
		InvV.makePart(26.3767, 27.3333, stdCell);	// 3x7x60000
		
		return lib;
	}
	
	private Cell findCell(Library lib, String cellName) {
		Cell c = lib.findNodeProto(cellName);
		LayoutLib.error(c==null, "can't find: "+lib.getLibName()+":"+cellName);
		return c;
	}
	
//	private Library doTom() {
//		String outLibNm = "tomsAutoGenLib";
//		String outLibDir = "c:/a1/kao/Sun/";
//		Library outLib = LayoutLib.openLibForWrite(outLibNm, outLibDir+outLibNm);
//		StdCellParams stdCell = locoParams(outLib);
//
//		String inLibNm = "rcexp";
//		String inLibDir = "x:/links/async/cad/2004/loco/ronho/electric/";
//		inLibDir = "x:/links/toneill/tmp/tgo-0525/";
//		Library inLib = LayoutLib.openLibForRead(inLibNm, inLibDir+inLibNm+".elib");
//		
//		HashSet cellNames = new HashSet();
//		cellNames.add("inv");
//		cellNames.add("invLT");
//		cellNames.add("invHT");
//		cellNames.add("invCLK");
//		GenerateLayoutForGatesInSchematic visitor =
//			new GenerateLayoutForGatesInSchematic("redFour", cellNames, stdCell);
//		
//		Cell c = findCell(inLib, "rcexp_mux{sch}");
//		HierarchyEnumerator.enumerateCell(c, null, null, visitor);
//		c = findCell(inLib, "rcexp_driver{sch}");
//		HierarchyEnumerator.enumerateCell(c, null, null, visitor);
//		c = findCell(inLib, "rcexp_edgegen{sch}"); 
//		HierarchyEnumerator.enumerateCell(c, null, null, visitor);
//			
//		return outLib;
//	}
	
	private Library doLoco() {
		String outLibNm = "locoAutoGenLib";
		String outLibDir = "c:/a1/kao/Sun/";
		Library outLib = LayoutLib.openLibForWrite(outLibNm, outLibDir+outLibNm);
		StdCellParams stdCell = locoParams(outLib);
		StdCellParams stdCellPwr = locoParams(outLib);
		stdCellPwr.setVddExportName("power");
		stdCellPwr.setVddExportRole(PortProto.Characteristic.IN);

		String inLibNm = "loco_jkl";
		String inLibDir = "x:/links/async/cad/2004/loco/integrate-0527/";
		Library inLib = LayoutLib.openLibForRead(inLibNm, inLibDir+inLibNm+".elib");
		
		HashSet cellNames = new HashSet();
		cellNames.add("inv");
		cellNames.add("invLT");
		cellNames.add("invHT");
		cellNames.add("invCLK");
		cellNames.add("nand2");
		GenerateLayoutForGatesInSchematic visitor =
			new GenerateLayoutForGatesInSchematic("redFour", cellNames, stdCell, 
			                                      stdCellPwr);
		
		Cell c = findCell(inLib, "loco_core{sch}");
		HierarchyEnumerator.enumerateCell(c, null, null, visitor);
			
		return outLib;
	}
	
	StdCellParams locoParams(Library outLib) {
		StdCellParams stdCell = new StdCellParams(outLib);
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
	
	public void doIt() {
		System.out.println("begin execution of Gates");
		boolean nfsWedged = true;
		String homeDir;
		if (!osIsWindows()) {
			homeDir = "/home/rkao/";
		} else if (nfsWedged) {
			homeDir = "c:/a1/kao/Sun";
		} else {
			homeDir = "x:/";
		}

		//Library lib = doJustin(homeDir, stdCell);
		//Library lib = doTom();
		Library lib = doLoco();
		Cell gallery = Gallery.makeGallery(lib);
		DrcRings.addDrcRings(gallery, FILTER);
		
		//LayoutLib.writeLibrary(scratchLib);

		System.out.println("done.");
	}
	public Loco() {
		super("Build inverter library for Justin", User.tool, Job.Type.CHANGE, 
			  null, null, Job.Priority.ANALYSIS);
		startJob();
	}
}

/** Traverse a schematic hierarchy and generate Cells that we recognize. */
class GenerateLayoutForGatesInSchematic extends HierarchyEnumerator.Visitor {
	private final String libName;
	private final HashSet cellNames;
	private final StdCellParams stdCell;
	private final StdCellParams stdCellPwr;
	private final HashSet visitedCells = new HashSet();
	
	/**
	 * Construct a Visitor that will walk a schematic and generate layout for Cells.
	 * @param libraryName the name of the library that contains the Cells for 
	 * which we want to generate layout. For example: "redFour" or "purpleFour".
	 * @param cellNames the names of the Cells for which we want to generate layout
	 */
	public GenerateLayoutForGatesInSchematic(String libraryName, 
											 HashSet cellNames,
	                          				 StdCellParams stdCell,
	                          				 StdCellParams stdCellPwr) {
		libName = libraryName;
		this.cellNames = cellNames;
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
		String pNm = iconInst.getProto().getProtoName();
		Variable var = iconInst.getVar("ATTR_X");
		double x = getNumericVal(context.evalVar(var));
		if (x==-1) return;
		
		StdCellParams sc = stdCell;
//		if (pNm.equals("inv"))         InvHT.makePart(x, sc);
//		else if (pNm.equals("invHT"))         InvHT.makePart(x, sc);
//		else if (pNm.equals("invLT"))         InvHT.makePart(x, sc);
//		else if (pNm.equals("invCLK"))         InvHT.makePart(x, sc);
//		else if (pNm.equals("nand2"))    Nand2.makePart(x, sc);

		boolean unknown = false;
		
		int pwr = pNm.indexOf("_pwr");
		if (pwr!=-1) {
			pNm = pNm.substring(0, pwr);
			sc = stdCellPwr;
		}
		
		if      (pNm.equals("nms1"))          Nms1.makePart(x, sc);
		else if (pNm.equals("nms1K"))         Nms1.makePart(x, sc);
		else if (pNm.equals("nms2"))          Nms2.makePart(x, sc);
		else if (pNm.equals("nms2_sy"))       Nms2_sy.makePart(x, sc);
		else if (pNm.equals("nms3_sy3"))      Nms3_sy3.makePart(x, sc);
		else if (pNm.equals("pms1"))          Pms1.makePart(x, sc);
		else if (pNm.equals("pms1K"))         Pms1.makePart(x, sc);
		else if (pNm.equals("pms2"))          Pms2.makePart(x, sc);
		else if (pNm.equals("pms2_sy"))       Pms2_sy.makePart(x, sc);
		else if (pNm.equals("inv"))           Inv.makePart(x, sc);
		else if (pNm.equals("invCTLn"))       InvCTLn.makePart(x, sc);
		else if (pNm.equals("inv_passgate"))  Inv_passgate.makePart(x, sc);
		else if (pNm.equals("inv2i"))         Inv2i.makePart(x, sc);
		else if (pNm.equals("inv2iKp"))       Inv2iKp.makePart(x, sc);
		else if (pNm.equals("inv2iKn"))       Inv2iKn.makePart(x, sc);
		else if (pNm.equals("invLT"))         InvLT.makePart(x, sc);
		else if (pNm.equals("invHT"))         InvHT.makePart(x, sc);
		else if (pNm.equals("invCLK"))         InvCLK.makePart(x, sc);
		else if (pNm.equals("nand2"))         Nand2.makePart(x, sc);
		else if (pNm.equals("nand2k"))        Nand2.makePart(x, sc);
		else if (pNm.equals("nand2en"))       Nand2en.makePart(x, sc);
		else if (pNm.equals("nand2en_sy"))    Nand2en_sy.makePart(x, sc);
		else if (pNm.equals("nand2HLT"))      Nand2HLT.makePart(x, sc);
		else if (pNm.equals("nand2LT"))       Nand2LT.makePart(x, sc);
		else if (pNm.equals("nand2_sy"))      Nand2_sy.makePart(x, sc);
		else if (pNm.equals("nand2HLT_sy"))   Nand2HLT_sy.makePart(x, sc);
		else if (pNm.equals("nand2LT_sy"))    Nand2LT_sy.makePart(x, sc);
		else if (pNm.equals("nand2PH"))       Nand2PH.makePart(x, sc);    
		else if (pNm.equals("nand2PHfk"))     Nand2PHfk.makePart(x, sc);    
		else if (pNm.equals("nand3"))         Nand3.makePart(x, sc);
		else if (pNm.equals("nand3MLT"))      Nand3MLT.makePart(x, sc);
		else if (pNm.equals("nand3LT"))       Nand3LT.makePart(x, sc);
		else if (pNm.equals("nand3_sy3"))     Nand3_sy3.makePart(x, sc);
		else if (pNm.equals("nand3en_sy"))    Nand3en_sy.makePart(x, sc);
		else if (pNm.equals("nand3LT_sy3"))   Nand3LT_sy3.makePart(x, sc);
		else if (pNm.equals("nand3en_sy3"))   Nand3en_sy3.makePart(x, sc);
		else if (pNm.equals("nor2"))          Nor2.makePart(x, sc);
		else if (pNm.equals("nor2LT"))        Nor2LT.makePart(x, sc);
		else if (pNm.equals("nor2kresetV"))   Nor2kresetV.makePart(x, sc);
		else unknown = true;
		
		if (!unknown) System.out.println("Gate Type: " + pNm + ", Gate Size: " + x);
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
		String libNm = lib.getLibName();
		if (libNm.equals("redFour") || libNm.equals("power2_gates")) {
			String cellName = cell.getProtoName();
			//if (cellNames.contains(cellName)) {
				// Generate layout for this Cell
				generateCell(no, info.getContext());	
			//}
			return false;
		}
		return true;
	}
}

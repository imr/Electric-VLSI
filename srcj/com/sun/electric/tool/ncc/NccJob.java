/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccJob.java
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

package com.sun.electric.tool.ncc;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.User;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;

import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.generator.layout.LayoutLib;

class NccMismatchException extends RuntimeException {
	NccMismatchException() {super("NCC mismatch found");}
}

class NccBotUp extends HierarchyEnumerator.Visitor {
	private HashSet enteredCells = new HashSet();
	private static HashSet nccedCells = new HashSet();
	public boolean enterCell(HierarchyEnumerator.CellInfo info) {
		Cell cell = info.getCell();
		if (enteredCells.contains(cell))  return false;

		enteredCells.add(cell);
		return true;
	}
	public void exitCell(HierarchyEnumerator.CellInfo info) {
		Cell cell = info.getCell();
		if (nccedCells.contains(cell)) return;
		
		Cell[] schLay = NccUtils.findSchematicAndLayout(cell);

		if (schLay!=null) {
			Cell schematic = schLay[0];
			Cell layout = schLay[1];
			if (nccedCells.contains(schematic)) return;

			if (NccUtils.hasSkipAnnotation(schematic)) return;
			if (NccUtils.hasSkipAnnotation(layout)) return;
			
			boolean ok = NccUtils.nccTwoCells(schematic, layout);
			if (!ok) throw new NccMismatchException();
			nccedCells.add(cell);
		}
	}
	public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) {
		return true;
	}
} 

public class NccJob extends Job {
	private final int numWindows;
	private final boolean bottomUp;
	private final boolean hierarchical;
	
	private boolean bottomUp() {
		Cell rootCell = NccUtils.getCurrentCell();
		if (rootCell==null) {
			System.out.println("Please open the Cell you want to NCC.");
			return false;
		} else {
			try {
				bottomUp(rootCell);
			} catch (NccMismatchException e) {
				System.out.println(e);
				return false;
			}
			return true;
		}
	}
	
	private void bottomUp(Cell rootCell) {
		Netlist rootNetlist = rootCell.getNetlist(true);
		NccBotUp visitor = new NccBotUp();
		HierarchyEnumerator.enumerateCell(rootCell, null, rootNetlist, visitor);											   
	}
	
	private boolean nccSchAndLayViewsOfCurrentCell() {
		Cell curCell = NccUtils.getCurrentCell();
		if (curCell==null) {
			System.out.println("Please open the Cell you wish to NCC");
			return false;
		} 
		Cell[] schLay = NccUtils.findSchematicAndLayout(curCell);
		if (schLay==null) {
			System.out.println("current Cell doesn't have both schematic and layout views");
			return false;
		}
		Cell sch = schLay[0];
		Cell lay = schLay[1];
		if (hierarchical) {
			return NccHierarchical.compareHierarchical(sch, lay);
		} else {
			return NccUtils.nccTwoCells(sch, lay);
		}
	}
	
	private boolean isSchemOrLay(Cell c) {
		View v = c.getView();
		boolean ok = v==View.SCHEMATIC || v==View.LAYOUT;
		if (!ok) System.out.println("Cell: "+NccUtils.fullName(c)+
									" isn't schematic or layout");
		return ok;
	}
	
	private boolean nccCellsFromTwoWindows() {
		List cells = NccUtils.getCellsFromWindows();
		if (cells.size()<2) {
			System.out.println("Two Cells aren't open in two windows");
			return false;
		} 
		Cell c1 = (Cell) cells.get(0);
		Cell c2 = (Cell) cells.get(1);
		if (!isSchemOrLay(c1) || isSchemOrLay(c2)) return false;
		if (hierarchical) {
			return NccHierarchical.compareHierarchical(c1, c2);
		} else {
			return NccUtils.nccTwoCells(c1, c2);
		}
	}

    public boolean doIt() {
		System.out.println("Ncc starting");
	
		boolean ok;
		if (bottomUp) {
			ok = bottomUp();
		} else if (numWindows==1) {
			ok = nccSchAndLayViewsOfCurrentCell();
		} else {
			ok = nccCellsFromTwoWindows();
		}
		
		System.out.println("Ncc done");
		return ok;
    }

	// ------------------------- public method --------------------------------
	public NccJob(int numWindows, boolean bottomUp, boolean hierarchical) {
		super("Run NCC", User.tool, Job.Type.CHANGE, 
			  null, null, Job.Priority.ANALYSIS);
		this.numWindows = numWindows;
		this.bottomUp = bottomUp;
		this.hierarchical = hierarchical;
		startJob();
	}
}

//String homeDir;
//if (!LayoutLib.osIsWindows()) {
//	homeDir = "/home/rkao/";
//} else {
//	homeDir = "c:/";
//}
//String testDir = homeDir+"ivanTest/qFourP1/electric-final/";
//
//doOneCell(testDir);		
////doLib(testDir, "purple");		
////doLib(testDir, "inv");		
////doLib(testDir, "invDrive");		
////doLib(testDir, "psDrive");
////doLib(testDir, "srNand");
////doLib(testDir, "gasp");
////doLib(testDir, "senseAmp");
////doLib(testDir, "scanChainFour");
////doLib(testDir, "latches");
////doLib(testDir, "gasP_COR");
////doLib(testDir, "rings");
////doLib(testDir, "senseAmp");
////doLib(testDir, "senseReg");
////doLib(testDir, "rxPads");


//private void compareCellToSelf(Cell cell) {
//	  messenger.println("SelfComparison of: "+fullName(cell));
//	  NccOptions options = new NccOptions();
//	  options.checkSizes = false;
//	  options.mergeParallelCells = true;
//
//	  List cells = new ArrayList();
//	  cells.add(cell);  cells.add(cell);
//	  List contexts = new ArrayList();
//	  contexts.add(null);  contexts.add(null);
//	  List netlists = new ArrayList();
//	  Netlist netlist = cell.getNetlist(true);
//	  netlists.add(netlist);  netlists.add(netlist);
//
//	  boolean matched = NccEngine.compare(cells, contexts, netlists, options);
//	  messenger.println("Ncc "+(matched ? "succeeds" : "fails"));
//	  messenger.error(!matched, "Ncc fails");
//}
//	
//private void doOneCell(String testDir) {
////		Library lib = 
////			LayoutLib.openLibForRead("qFourP1", testDir+"qFourP1.elib");
////		Cell cell = lib.findNodeProto("expTail{lay}");
//	  Library lib = 
//		  LayoutLib.openLibForRead("rxPads", testDir+"rings.elib");
//	  Cell cell = lib.findNodeProto("rxPadArray{lay}");
//	  compareCellToSelf(cell);
//}
//	
//	
//private void doLib(String testDir, String libName) {
//	  Library lib = LayoutLib.openLibForRead(libName, 
//											 testDir+libName+".elib");
//	  for (Iterator it=lib.getCells(); it.hasNext();) {
//		  Cell cell = (Cell) it.next();
//		  View view = cell.getView();
//		  if (view==View.ICON)  continue;
//		  // galleries
//		  if (cell.getName().equals("gallery"))  continue;
//		  if (cell.getName().equals("halfKeepIndex"))  continue;
//		  if (cell.getName().equals("aScanIndex"))  continue;
//		  if (cell.getName().equals("aScanIndexB"))  continue;
//			
//		  // paralleled gates need random matching
//		  // These were eliminated by the Cell based parallel merging
////			if (cell.getName().equals("gaspRowC"))  continue;
////			if (cell.getName().equals("gaspRing1"))  continue;
////			if (cell.getName().equals("gaspRowD"))  continue;
//
//		  // paralleling hidden from my merger!
//		  if (cell.getName().equals("ringTx") &&
//			  view==View.LAYOUT)  continue;
//				
//		  // I don't understand this one				
//		  if (cell.getName().equals("rxPadArray") &&
//			  view==View.LAYOUT)  continue;
//			
//		  compareCellToSelf(cell);									
//	  }
//}


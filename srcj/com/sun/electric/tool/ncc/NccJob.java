/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: jemini_2.java
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


import java.util.*;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.technology.*;
import com.sun.electric.tool.io.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.database.hierarchy.View;

import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.generator.layout.LayoutLib;

class NccBotUp extends HierarchyEnumerator.Visitor {
	private HashSet enteredCells = new HashSet();
	private HashSet nccedCells = new HashSet();
	public boolean enterCell(HierarchyEnumerator.CellInfo info) {
		Cell cell = info.getCell();
		if (enteredCells.contains(cell)) {
			return false;
		} else {
			enteredCells.add(cell);
			return true;
		}
	}
	public void exitCell(HierarchyEnumerator.CellInfo info) {
		Cell cell = info.getCell();
		if (nccedCells.contains(cell)) return;
		
		Cell.CellGroup group = cell.getCellGroup();
		Cell layout=null, schematic=null;
		for (Iterator it=group.getCells(); it.hasNext();) {
			Cell c = (Cell) it.next();
			if (c.getView()==View.SCHEMATIC)   schematic=c;
			else if (c.getView()==View.LAYOUT) layout=c;
		}
		if (layout!=null && schematic!=null) {
			System.out.println("Comparing schematic: "+
							   schematic.getLibrary().getName() +
							   " : "+
			                   schematic.getName()+
                               " with layout: "+layout.getLibrary().getName()+
                               " : "+
                               layout.getName());
			NccOptions options = new NccOptions();
			options.verbose = false;
			boolean ok = NccEngine.compare(schematic, null, layout, null, 
			                               options);
			LayoutLib.error(!ok, "NccJob finds mismatch");
			nccedCells.add(cell);
		}
	}
	public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) {
		return true;
	}
} 

public class NccJob extends Job {
	private Messenger messenger;
	
	private String fullName(Cell cell) {
		String name = cell.getName();
		int ver = cell.getVersion();
		name += ";"+ver;
		View view = cell.getView();
		name += "{"+view.getAbbreviation()+"}";
		return name;
	}
	
	private void compareCellToSelf(Cell cell) {
		messenger.println("SelfComparison of: "+fullName(cell));
		NccOptions options = new NccOptions();
		options.checkSizes = false;
		options.mergeParallelCells = true;

		List cells = new ArrayList();
		cells.add(cell);  cells.add(cell);
		List contexts = new ArrayList();
		contexts.add(null);  contexts.add(null);
		List netlists = new ArrayList();
		Netlist netlist = cell.getNetlist(true);
		netlists.add(netlist);  netlists.add(netlist);

		boolean matched = NccEngine.compare(cells, contexts, netlists, options);
		messenger.println("Ncc "+(matched ? "succeeds" : "fails"));
		messenger.error(!matched, "Ncc fails");
	}
	
	private void doOneCell(String testDir) {
//		Library lib = 
//			LayoutLib.openLibForRead("qFourP1", testDir+"qFourP1.elib");
//		Cell cell = lib.findNodeProto("expTail{lay}");
		Library lib = 
			LayoutLib.openLibForRead("rxPads", testDir+"rings.elib");
		Cell cell = lib.findNodeProto("rxPadArray{lay}");
		compareCellToSelf(cell);
	}
	
	
	private void doLib(String testDir, String libName) {
		Library lib = LayoutLib.openLibForRead(libName, 
											   testDir+libName+".elib");
		for (Iterator it=lib.getCells(); it.hasNext();) {
			Cell cell = (Cell) it.next();
			View view = cell.getView();
			if (view==View.ICON)  continue;
			// galleries
			if (cell.getName().equals("gallery"))  continue;
			if (cell.getName().equals("halfKeepIndex"))  continue;
			if (cell.getName().equals("aScanIndex"))  continue;
			if (cell.getName().equals("aScanIndexB"))  continue;
			
			// paralleled gates need random matching
			// These were eliminated by the Cell based parallel merging
//			if (cell.getName().equals("gaspRowC"))  continue;
//			if (cell.getName().equals("gaspRing1"))  continue;
//			if (cell.getName().equals("gaspRowD"))  continue;

			// paralleling hidden from my merger!
			if (cell.getName().equals("ringTx") &&
				view==View.LAYOUT)  continue;
				
			// I don't understand this one				
			if (cell.getName().equals("rxPadArray") &&
				view==View.LAYOUT)  continue;
			
			compareCellToSelf(cell);									
		}
	}
	
	private void bottomUp(String testDir, String libName) {
		Library lib = LayoutLib.openLibForRead(libName, 
											   testDir+libName+".elib");
		Cell rootCell = lib.findNodeProto("loco_top{sch}");
		LayoutLib.error(rootCell==null, "can't find root Cell");
		Netlist rootNetlist = rootCell.getNetlist(true);
		NccBotUp visitor = new NccBotUp();
		HierarchyEnumerator.enumerateCell(rootCell, null, rootNetlist, visitor);											   
	}
	
    public boolean doIt() {
		System.out.println("Ncc starting");
		
		String testDir = "c:/a1/kao/Sun/loco-final/";
		bottomUp(testDir, "pads2");

//		String homeDir;
//		if (!LayoutLib.osIsWindows()) {
//			homeDir = "/home/rkao/";
//		} else {
//			homeDir = "c:/";
//		}
//		String testDir = homeDir+"ivanTest/qFourP1/electric-final/";
//
//		doOneCell(testDir);		
//		//doLib(testDir, "purple");		
//		//doLib(testDir, "inv");		
//		//doLib(testDir, "invDrive");		
//		//doLib(testDir, "psDrive");
//		//doLib(testDir, "srNand");
//		//doLib(testDir, "gasp");
//		//doLib(testDir, "senseAmp");
//		//doLib(testDir, "scanChainFour");
//		//doLib(testDir, "latches");
//		//doLib(testDir, "gasP_COR");
//		//doLib(testDir, "rings");
//		//doLib(testDir, "senseAmp");
//		//doLib(testDir, "senseReg");
//		//doLib(testDir, "rxPads");
		
		System.out.println("Ncc done");
		return true;
    }

	// ------------------------- public method --------------------------------
	public NccJob(){
		super("Run Jemini", User.tool, Job.Type.CHANGE, 
			  null, null, Job.Priority.ANALYSIS);
		startJob();
	}
}

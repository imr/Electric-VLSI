/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccUtils.java
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

package com.sun.electric.tool.ncc.basic;


import java.util.*;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.technology.*;
import com.sun.electric.tool.io.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.User;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.database.variable.VarContext;

import com.sun.electric.tool.ncc.NccEngine;
import com.sun.electric.tool.ncc.NccJob;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.generator.layout.LayoutLib;


public class NccUtils {
	public static String fullName(Cell c) {
		return c.getLibrary().getName() +
			   ":"+
			   c.getName()+
			   "{"+
			   c.getView().getAbbreviation()+
			   "}";
	}
	public static Cell[] findSchematicAndLayout(Cell cell) {
		Cell.CellGroup group = cell.getCellGroup();
		Cell layout=null, schematic=null;
		for (Iterator it=group.getCells(); it.hasNext();) {
			Cell c = (Cell) it.next();
			if (c.getView()==View.SCHEMATIC)   schematic=c;
			else if (c.getView()==View.LAYOUT) layout=c;
		}
		if (schematic!=null && layout!=null)  return new Cell[] {schematic, layout};
		else return null;
	}
	
	public static CellContext getCurrentCellContext() {
		EditWindow wnd = EditWindow.getCurrent();
		return getCellContext(wnd);
	}

	public static CellContext getCellContext(EditWindow wnd) {
		if (wnd==null) return null;
		Cell cell = wnd.getCell();
		if (cell==null) {
			System.out.println("window has no Cell");
			return null;
		}
		VarContext context = wnd.getVarContext();
		return new CellContext(cell, context);
	}
	
	public static List getCellContextsFromWindows() {
		List cellCtxts = new ArrayList();

		// first is always current window
		EditWindow wnd = EditWindow.getCurrent();
		CellContext curCellCtxt = getCellContext(wnd);

		if (curCellCtxt==null)  return cellCtxts;
		cellCtxts.add(curCellCtxt);

		for(Iterator it=WindowFrame.getWindows(); it.hasNext();) {
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			EditWindow wnd2 = (EditWindow) content;
			if (wnd2==wnd) continue;
			CellContext cc = getCellContext((EditWindow)content);
			cellCtxts.add(cc);
		}

		return cellCtxts;
	}

	public static boolean hasSkipAnnotation(Cell c) {
		NccCellAnnotations ann = NccCellAnnotations.getAnnotations(c);
		if (ann==null) return false;
		String reason = ann.getSkipReason();
		if (reason!=null) {
			System.out.println("Skip NCC of "+NccUtils.fullName(c)+
							   " because "+reason);
			return true;							   
		}
		return false;
	}
}

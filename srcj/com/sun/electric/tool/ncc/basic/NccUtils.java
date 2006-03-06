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


import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.ncc.Aborter;
import com.sun.electric.tool.ncc.NccEngine;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.processing.HierarchyInfo;
import com.sun.electric.tool.ncc.result.NccResult;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;


public class NccUtils {
	private static long pow(int x, int y) {
		long prod = 1;
		for (int i=0; i<y; i++)  prod*=x;
		return prod;
	}
	public static String fullName(Cell c) {return c.libDescribe();}
	private static Cell findLayout(Cell.CellGroup group) {
		for (Iterator<Cell> it=group.getCells(); it.hasNext();) {
			Cell c = (Cell) it.next();
			if (c.getView()==View.LAYOUT) return c.getNewestVersion();
		}
		return null;
	}
	/** If cell is layout then pair it with the main schematic cell of cell's
	 * CellGroup. If cell is schematic then pair it with some layout cell from
	 * cell's CellGroup. If cell is neither schematic nor layout then return 
	 * the main schematic and some layout cell. Always put the schematic at
	 * index 0 */
	public static Cell[] findSchematicAndLayout(Cell cell) {
		Cell.CellGroup group = cell.getCellGroup();
		Cell layout=null, schematic=null;
		if (cell.isSchematic()) {
			// have schematic, find layout
			schematic = cell;
			layout = findLayout(group);
		} else if (cell.getView()==View.LAYOUT) {
			// have layout, find schematic
			layout = cell;
			schematic = group.getMainSchematics();
		} else {
			schematic = group.getMainSchematics();
			layout = findLayout(group);
		}

		if (schematic!=null && layout!=null)  return new Cell[] {schematic, layout};
		else return null;
	}
	
	public static CellContext getCurrentCellContext() {
		UserInterface ui = Job.getUserInterface();
		EditWindow_ wnd = ui.getCurrentEditWindow_();
		return getCellContext(wnd);
	}

	public static CellContext getCellContext(EditWindow_ wnd) {
		if (wnd==null) return null;
		Cell cell = wnd.getCell();
		if (cell==null) {
			System.out.println("window has no Cell");
			return null;
		}
		VarContext context = wnd.getVarContext();
		return new CellContext(cell, context);
	}
	
	public static List<CellContext> getCellContextsFromWindows() {
		List<CellContext> cellCtxts = new ArrayList<CellContext>();

		// first is always current window
		UserInterface ui = Job.getUserInterface();
		EditWindow_ wnd = ui.getCurrentEditWindow_();
		CellContext curCellCtxt = getCellContext(wnd);

		if (curCellCtxt==null)  return cellCtxts;
		cellCtxts.add(curCellCtxt);

		for(Iterator<WindowFrame> it=WindowFrame.getWindows(); it.hasNext();) {
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow_)) continue;
			EditWindow_ wnd2 = (EditWindow_) content;
			if (wnd2==wnd) continue;
			CellContext cc = getCellContext((EditWindow_)content);
			cellCtxts.add(cc);
		}

		return cellCtxts;
	}

	/** print hours minutes seconds */
	public static String hourMinSec(Date start, Date stop) {
		final int msecPerHour = 1000*60*60;
		final int msecPerMin = 1000*60;
		long elapsedMsec = stop.getTime() - start.getTime();
		long hours = elapsedMsec / msecPerHour;
		elapsedMsec = elapsedMsec % msecPerHour;
		long mins = elapsedMsec / msecPerMin;
		elapsedMsec = elapsedMsec % msecPerMin;
		double secs = elapsedMsec / 1000.0;
		String time = "";
		if (hours!=0) time += hours + " hours ";
		if (mins!=0) time += mins + " minutes ";
		time += secs + " seconds";
		return time; 
	}
	
	public static NccResult buildBlackBoxes(CellContext c1, CellContext c2,
								            HierarchyInfo hierInfo, 
								            NccOptions options, 
								            Aborter aborter) {
		System.out.println("Build black boxes for: "+NccUtils.fullName(c1.cell)+
						   " and: "+NccUtils.fullName(c2.cell));
		System.out.flush();
		NccResult r = 
			NccEngine.buildBlackBoxes(c1.cell, c1.context, c2.cell, c2.context, 
									  hierInfo, options, aborter);	                                 
		System.out.println(r.match() ? "Done" : "Failed");
		System.out.flush();
		return r;
	}

	public static boolean sizesMatch(double w1, double w2, NccOptions opt) {
		double maxWidth = Math.max(w1, w2);
		double minWidth = Math.min(w1, w2);
		double absWidErr = maxWidth - minWidth;
		double relWidErr = absWidErr / minWidth;
		return absWidErr<=opt.absoluteSizeTolerance || 
		       relWidErr<=opt.relativeSizeTolerance;
	}
    /** Wait forever. This allows me to explore memory usage */
    public static void hang(String msg) {
        final long YEAR = 1000*60*60*24*365;
        try {
            System.out.println(msg+" now going to sleep for 1 year");
            System.out.flush();
            Thread.sleep(1*YEAR);
        } catch (Exception e) {
            System.out.println("Huh? I woke up.");
        }
    }
	public static double round(double x, int places) {
		long m = pow(10, places);
		return Math.rint(x*m)/m;
	}
}


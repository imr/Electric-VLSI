/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DisplayedCells.java
 * Project management tool
 * Written by: Steven M. Rubin
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.project;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This is the Project Management tool.
 */
public class DisplayedCells implements Serializable
{
	/** the list of windows being displayed. */		private List<Integer> formerWindows;
	/** the list of cells in the window */			private List<Cell> formerCellsInWindows;
	/** the list of cells that were created */		private Set<Cell> createdCells;

	/**
	 * The constructor returna an object that captures the cells being displayed.
	 * It is called on the client side, generally in the constructor of a Job.
	 * It captures the currently displayed windows, and returns this object that
	 * can be passed to the server for access during the "doIt()" method.
	 * During thd "doIt()" method, all replacements of cells can are marked by using "swap()".
	 * Finally, back on the client during the "terminateIt()" method,
	 * call "updateWindows()" to display the replacements.
	 */
	public DisplayedCells()
	{
		// cache former state of windows
		formerWindows = new ArrayList<Integer>();
		formerCellsInWindows = new ArrayList<Cell>();
		createdCells = new HashSet<Cell>();
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			formerWindows.add(new Integer(wf.getIndex()));
			Cell displayedCell = wf.getContent().getCell();
			formerCellsInWindows.add(displayedCell);
		}

		// cache former state of windows
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			Cell displayedCell = wf.getContent().getCell();
			if (formerCellsInWindows.contains(displayedCell))
    			wf.getContent().setCell(null, null, null);
		}
	}

	void setCellsToBeChanged(List<Cell> checkOutCells)
	{
		// cache former state of windows
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			Cell displayedCell = wf.getContent().getCell();
			if (checkOutCells.contains(displayedCell))
    			wf.getContent().setCell(null, null, null);
		}
	}

	/**
	 * Method to update the displayed cell list to account for a replacement.
	 * This method is called during the "doIt()" (on the server).
	 * @param oldVers the old version of the cell.
	 * @param newVers the new version to display wherever the old version used to be.
	 */
	public void swap(Cell oldVers, Cell newVers)
	{
		// keep track of displayed cells to show the new one
		for(int i=0; i<formerCellsInWindows.size(); i++)
		{
			if (formerCellsInWindows.get(i) == oldVers)
				formerCellsInWindows.set(i, newVers);
		}
    	createdCells.add(newVers);
	}

	/**
	 * Method to finish changes to displayed cells.
	 * This method is called during the "terminateIt()" (back on the client).
	 */
	public void updateWindows()
	{
		// update user interface for the changed cells
		for(int i=0; i<formerCellsInWindows.size(); i++)
		{
			WindowFrame wf = WindowFrame.findFromIndex(formerWindows.get(i).intValue());
			if (wf == null) continue;
			Cell newVers = formerCellsInWindows.get(i);
			Cell displayedCell = wf.getContent().getCell();
			if (displayedCell != newVers)
			{
				WindowFrame.DisplayAttributes da = new WindowFrame.DisplayAttributes();
				da.scale = 1;
				if (wf.getContent() instanceof EditWindow_)
				{
					EditWindow_ wnd = (EditWindow_)wf.getContent();
					da.scale = wnd.getScale();
					da.offX = wnd.getOffset().getX();
					da.offY = wnd.getOffset().getY();
				}
				wf.getContent().setCell(newVers, VarContext.globalContext, da);
			}
		}
	}

	/**
	 * Method to return a list of cells that were created (those that were swapped-in).
	 */
	public Iterator<Cell> getCreatedCells()
	{
		return createdCells.iterator();
	}
}

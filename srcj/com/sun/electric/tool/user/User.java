/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: User.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.dialogs.GetInfoNode;
import com.sun.electric.tool.user.dialogs.GetInfoArc;
import com.sun.electric.tool.user.dialogs.GetInfoExport;
import com.sun.electric.tool.user.dialogs.GetInfoText;
import com.sun.electric.tool.user.dialogs.GetInfoMulti;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.PaletteFrame;

import java.util.Iterator;

/**
 * This is the User Interface tool.
 */
public class User extends Tool
{
	// ---------------------- private and protected methods -----------------

	/** the User Interface tool. */		public static User tool = new User();

	private ArcProto currentArcProto = null;
	private NodeProto currentNodeProto = null;

	/**
	 * The constructor sets up the User tool.
	 */
	private User()
	{
		super("user");
	}

	/**
	 * Routine to initialize the User Interface tool.
	 */
	public void init()
	{
		// the user interface tool is always on
		setOn();
		setIncremental();

		// initialize the display
		TopLevel.Initialize();
		PaletteFrame pf = TopLevel.getPaletteFrame();

		pf.loadForTechnology();
	}

	/**
	 * Routine to return the "current" NodeProto, as maintained by the user interface.
	 * @return the "current" NodeProto, as maintained by the user interface.
	 */
	public NodeProto getCurrentNodeProto() { return currentNodeProto; }

	/**
	 * Routine to set the "current" NodeProto, as maintained by the user interface.
	 * @param np the new "current" NodeProto.
	 */
	public void setCurrentNodeProto(NodeProto np) { currentNodeProto = np; }

	/**
	 * Routine to return the "current" ArcProto, as maintained by the user interface.
	 * The current ArcProto is highlighted with a bolder red border in the component menu on the left.
	 * @return the "current" ArcProto, as maintained by the user interface.
	 */
	public ArcProto getCurrentArcProto() { return currentArcProto; }

	/**
	 * Routine to set the "current" ArcProto, as maintained by the user interface.
	 * The current ArcProto is highlighted with a bolder red border in the component menu on the left.
	 * @param np the new "current" ArcProto.
	 */
	public void setCurrentArcProto(ArcProto ap) { currentArcProto = ap; }

	/**
	 * Daemon routine called when an object is to be redrawn.
	 */
	public void redrawObject(ElectricObject obj)
	{
		if (obj instanceof Geometric)
		{
			Geometric geom = (Geometric)obj;
			Cell parent = geom.getParent();
			markCellForRedraw(parent, true);
		}
	}

	/**
	 * Routine to recurse flag all windows showing a cell to redraw.
	 * @param cell the Cell that changed.
	 * @param recurseUp true to recurse up the hierarchy, redrawing cells that show this one.
	 */
	private void markCellForRedraw(Cell cell, boolean recurseUp)
	{
		for(Iterator wit = WindowFrame.getWindows(); wit.hasNext(); )
		{
			WindowFrame window = (WindowFrame)wit.next();
			EditWindow win = window.getEditWindow();
			Cell winCell = win.getCell();
			if (winCell == cell) win.redraw();
		}

		if (recurseUp)
		{
			for(Iterator it = cell.getInstancesOf(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (ni.isExpanded()) markCellForRedraw(ni.getParent(), recurseUp);
			}
		}
	}

	/**
	 * Daemon routine called when a batch of changes ends.
	 */
	public void endBatch()
	{
		// redraw all windows with Cells that changed
		for(Iterator it = Undo.ChangeCell.getIterator(); it.hasNext(); )
		{
			Undo.ChangeCell cc = (Undo.ChangeCell)it.next();
			Cell cell = cc.getCell();
			markCellForRedraw(cell, false);
		}

		// update any Get-Info dialogs
		GetInfoNode.load();
		GetInfoArc.load();
		GetInfoExport.load();
		GetInfoText.load();
		GetInfoMulti.load();
	}
}

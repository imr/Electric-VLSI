/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WindowContent.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;

import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class defines the right-side of a windowframe (the contents, as opposed to the explorer tree).
 */
public interface WindowContent
{
	public abstract void finished();
	public abstract void requestRepaint();
	public abstract void fillScreen();
	public abstract void zoomOutContents();
	public abstract void zoomInContents();
	public abstract void focusOnHighlighted();

	public abstract void setCell(Cell cell, VarContext context);
	public abstract Cell getCell();

	/**
	 * Method to get the actual contents JPanel for this WindowContent.
	 */
	public abstract JPanel getPanel();
	public abstract void bottomScrollChanged(int e);
	public abstract void rightScrollChanged(int e);
	public abstract void fireCellHistoryStatus();
	public abstract void loadExplorerTree(DefaultMutableTreeNode rootNode);
	public abstract boolean cellHistoryCanGoBack();
	public abstract boolean cellHistoryCanGoForward();
	public abstract void cellHistoryGoBack();
	public abstract void cellHistoryGoForward();
	public abstract void setWindowTitle();
}

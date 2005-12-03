/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UserInterfaceMain.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Point2D;

/**
 * Class to build the UserInterface for the main GUI version of the user interface.
 */
public class UserInterfaceMain implements UserInterface
{
	public EditWindow_ getCurrentEditWindow_() { return EditWindow.getCurrent(); }

	public EditWindow_ needCurrentEditWindow_() { return EditWindow.needCurrent(); }

	public Cell getCurrentCell() { return WindowFrame.getCurrentCell(); }

	public Cell needCurrentCell() { return WindowFrame.needCurCell(); }

	public void repaintAllEditWindows() { EditWindow.repaintAllContents(); }

	public void alignToGrid(Point2D pt) { EditWindow.gridAlign(pt); }

	public int getDefaultTextSize() { return EditWindow.getDefaultFontSize(); }

//	public Highlighter getHighlighter();

	public EditWindow_ displayCell(Cell cell)
	{
		WindowFrame wf = WindowFrame.createEditWindow(cell);
		if (wf.getContent() instanceof EditWindow_) return (EditWindow_)wf.getContent();
		return null;
	}	
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WindowContextClass.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.variable.VarContext;

import javax.swing.*;

/**
 * This class defines the right-side of a windowframe (the contents, as opposed to the explorer tree).
 */
public abstract class WindowContextClass extends JPanel
        implements HighlightListener
{
	/** the cell that is in the window */					protected Cell cell;
	/** Highlighter for this window */                      protected Highlighter highlighter;
	/** the window frame containing this editwindow */      protected WindowFrame wf;

    public WindowContextClass(Cell c, WindowFrame wf)
    {
        this.cell = c;
        this.wf = wf;

		highlighter = new Highlighter(Highlighter.SELECT_HIGHLIGHTER, wf);
        Highlighter.addHighlightListener(this);
    }


    /**
     * Method to return the cell that is shown in this window.
     * @return the cell that is shown in this window.
     */
    public Cell getCell() { return cell; }

    public void setCell(Cell cell)
    {
        this.cell = cell;
    }

    /**
	 * Centralized version of naming windows. Might move it to class
	 * that would replace WindowContext
	 * @param prefix a prefix for the title.
	 */
	public String composeTitle(String prefix, int pageNo)
	{
		// StringBuffer should be more efficient
		StringBuffer title = new StringBuffer();

		if (cell != null)
		{
			title.append(prefix + cell.libDescribe());

			if (cell.isMultiPage())
			{
				title.append(" - Page " + (pageNo+1));
			}
            Library curLib = Library.getCurrent();
			if (cell.getLibrary() != curLib && curLib != null)
				title.append(" - Current library: " + curLib.getName());
		}
		else
			title.append("***NONE***");
		return (title.toString());
	}
}

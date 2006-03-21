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

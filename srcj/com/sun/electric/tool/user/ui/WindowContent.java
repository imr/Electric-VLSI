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
import com.sun.electric.tool.user.Highlighter;

import java.util.Set;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class defines the right-side of a windowframe (the contents, as opposed to the explorer tree).
 */
public interface WindowContent
{
	/**
	 * Method to initialize for a new text search.
	 * @param search the string to locate.
	 * @param caseSensitive true to match only where the case is the same.
	 */
	public abstract void initTextSearch(String search, boolean caseSensitive,
	                                    boolean regExp, Set whatToSearch);

	/**
	 * Method to find the next occurrence of a string.
	 * @param reverse true to find in the reverse direction.
	 * @return true if something was found.
	 */
	public abstract boolean findNextText(boolean reverse);

	/**
	 * Method to replace the text that was just selected with findNextText().
	 * @param replace the new text to replace.
	 */
	public abstract void replaceText(String replace);

	/**
	 * Method to replace all selected text.
	 * @param replace the new text to replace everywhere.
	 */
	public abstract void replaceAllText(String replace);

	// the "flags" for searchText():
//	/** set to replace all occurrences. */					public static final int REPLACE_ALL = 2;
//	/** set if search is case-sensitive. */					public static final int CASE_SENSITIVE = 4;
//	/** set search backwards. */							public static final int FIND_REVERSE = 8;

	public abstract void finished();
	public abstract void repaint();
	public abstract void fullRepaint();
	public abstract void fillScreen();
	public abstract void zoomOutContents();
	public abstract void zoomInContents();
	public abstract void focusOnHighlighted();

	public abstract void setCell(Cell cell, VarContext context);
	public abstract Cell getCell();

    /** Get the Highlighter for this window */
    public abstract Highlighter getHighlighter();
    
	/**
	 * Method to return the top-level JPanel for this WindowContent.
	 * Although the classes that implement this interface may also extend JPanel,
	 * it is not always the case that those classes will be the JPanel that this
	 * method returns.  For example, the actual EditWindow object is below the top level, surrounded by scroll bars.
	 * @return the top-level JPanel for this WindowContent.
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

	/**
	 * Method to print window using offscreen canvas
	 * @param ep Image observer plus printable object
	 * @return Printable.NO_SUCH_PAGE or Printable.PAGE_EXISTS
	 */
	public abstract BufferedImage getOffScreenImage(ElectricPrinter ep); // For printing and export purposes
}

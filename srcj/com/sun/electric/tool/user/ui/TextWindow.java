/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TextWindow.java
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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLog;

import java.awt.BorderLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class defines a text window for displaying text cells.
 */
public class TextWindow
	implements WindowContent
{
	/** the cell that is in the window */					private Cell cell;
	/** the window frame containing this editwindow */      private WindowFrame wf;
	/** the overall panel with disp area and sliders */		private JPanel overall;
	private JTextArea textArea;
	private JScrollPane scrollPane;

	/**
	 * Factory method to create a new TextWindow with a given cell, in a given WindowFrame.
	 * @param cell the cell in this TextWindow.
	 * @param wf the WindowFrame that this TextWindow lives in.
	 * @return the new TextWindow.
	 */
	public TextWindow(Cell cell, WindowFrame wf)
	{
		textArea = new JTextArea();
		TextWindowDocumentListener twDocumentListener = new TextWindowDocumentListener(this);
		textArea.getDocument().addDocumentListener(twDocumentListener);
		scrollPane = new JScrollPane(textArea);
		overall = new JPanel();
		overall.setLayout(new BorderLayout());
		overall.add(scrollPane, BorderLayout.CENTER);

		setCell(cell, VarContext.globalContext);
		this.wf = wf;
	}

	/**
	 * Class to handle special changes to changes to design rules.
	 */
	private static class TextWindowDocumentListener implements DocumentListener
	{
		TextWindow tw;

		TextWindowDocumentListener(TextWindow tw) { this.tw = tw; }

		public void changedUpdate(DocumentEvent e) { tw.textWindowContentChanged(); }
		public void insertUpdate(DocumentEvent e) { tw.textWindowContentChanged(); }
		public void removeUpdate(DocumentEvent e) { tw.textWindowContentChanged(); }
	}

	private void textWindowContentChanged()
	{
		System.out.println("Document changed");
	}

	public void bottomScrollChanged(int value)
	{
	}

	/** Returns true if we can go back in history list, false otherwise */
	public boolean cellHistoryCanGoBack()
	{
		return false;
	}

	/** Returns true if we can go forward in history list, false otherwise */
	public boolean cellHistoryCanGoForward()
	{
		return false;
	}

	public void loadExplorerTree(DefaultMutableTreeNode rootNode)
	{
		wf.libraryExplorerNode = ExplorerTree.makeLibraryTree();
		wf.jobExplorerNode = Job.getExplorerTree();
		wf.errorExplorerNode = ErrorLog.getExplorerTree();
		wf.signalExplorerNode = null;
		rootNode.add(wf.libraryExplorerNode);
		rootNode.add(wf.jobExplorerNode);
		rootNode.add(wf.errorExplorerNode);
	}

	public void requestRepaint() {  }
	
	public JPanel getPanel() { return overall; }

	/**
	 * Method to get rid of this EditWindow.  Called by WindowFrame when
	 * that windowFrame gets closed.
	 */
	public void finished()
	{
	}

	public void rightScrollChanged(int value)
	{
	}

	/**
	 * Method to set the window title.
	 */
	public void setWindowTitle()
	{
		if (wf == null) return;

		if (cell == null)
		{
			wf.setTitle("***NONE***");
			return;
		}

		String title = cell.describe();
		if (cell.getLibrary() != Library.getCurrent())
			title += " - Current library: " + Library.getCurrent().getLibName();
		wf.setTitle(title);
	}

	/**
	 * Method to return the cell that is shown in this window.
	 * @return the cell that is shown in this window.
	 */
	public Cell getCell() { return cell; }

	/**
	 * Method to set the cell that is shown in the window to "cell".
	 */
	public void setCell(Cell cell, VarContext context)
	{
		StringBuffer sb = new StringBuffer();
		Variable var = cell.getVar(Cell.CELL_TEXT_KEY);
		if (var == null) return;
		if (!(var.getObject() instanceof String[])) return;
		String [] lines = (String [])var.getObject();
		int len = var.getLength();
		for(int i=0; i<len; i++)
		{
			sb.append(lines[i]);
			sb.append("\n");
		}
		textArea.setText(sb.toString());
	}

	/**
	 * Method to pan and zoom the screen so that the entire cell is displayed.
	 */
	public void fillScreen()
	{
	}

	public void zoomOutContents()
	{
	}

	public void zoomInContents()
	{
	}

	public void focusOnHighlighted()
	{
	}

    /**
     * Used when new tool bar is created with existing edit window
     * (when moving windows across displays).  Updates back/forward
     * button states.
     */
    public void fireCellHistoryStatus()
	{
    }
}

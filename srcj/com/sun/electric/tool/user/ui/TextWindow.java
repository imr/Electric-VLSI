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
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ErrorLog;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.MenuBar;

import java.awt.BorderLayout;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
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
import javax.swing.text.Document;

/**
 * This class defines a text window for displaying text cells.
 */
public class TextWindow
	implements WindowContent
{
	/** the cell that is in the window */					private Cell cell;
	/** the window frame containing this editwindow */      private WindowFrame wf;
	/** the overall panel with disp area and sliders */		private JPanel overall;
	/** true if the text in the window changed. */			private boolean dirty;
	/** true if the text in the window is closing. */		private boolean finishing;
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
		scrollPane = new JScrollPane(textArea);
		overall = new JPanel();
		overall.setLayout(new BorderLayout());
		overall.add(scrollPane, BorderLayout.CENTER);

		setCell(cell, VarContext.globalContext);
		this.wf = wf;
		finishing = false;

		TextWindowDocumentListener twDocumentListener = new TextWindowDocumentListener(this);
		textArea.getDocument().addDocumentListener(twDocumentListener);
		textArea.addFocusListener(twDocumentListener);
	}

	/**
	 * Class to handle special changes to changes to design rules.
	 */
	private static class TextWindowDocumentListener implements DocumentListener, FocusListener
	{
		TextWindow tw;

		TextWindowDocumentListener(TextWindow tw) { this.tw = tw; }

		public void changedUpdate(DocumentEvent e) { tw.textWindowContentChanged(); }
		public void insertUpdate(DocumentEvent e) { tw.textWindowContentChanged(); }
		public void removeUpdate(DocumentEvent e) { tw.textWindowContentChanged(); }
		public void focusGained(FocusEvent e)
		{
			TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
			MenuBar mb = top.getTheMenuBar();
			mb.setIgnoreTextEditKeys(true);
		}
		public void focusLost(FocusEvent e)
		{
			TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
			MenuBar mb = top.getTheMenuBar();
			mb.setIgnoreTextEditKeys(false);
		}
	}

	private void textWindowContentChanged()
	{
		cell.getLibrary().setChangedMajor();
		dirty = true;
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
	
	public JPanel getPanel() { return overall; }

	/**
	 * Method to get rid of this EditWindow.  Called by WindowFrame when
	 * that windowFrame gets closed.
	 */
	public void finished()
	{
		if (dirty)
		{
			finishing = true;
			SaveCellText job = new SaveCellText(this);
		}
	}

	/**
	 * Method to save all text windows back into the database.
	 * Call it before saving libraries so that the cells are current.
	 * Also, the method must be called inside of a Job since it modifies the database.
	 */
	public static void saveAllTextWindows()
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (content instanceof TextWindow)
			{
				TextWindow tw = (TextWindow)content;
				if (tw.dirty)
				{
					Cell cell = tw.getCell();
					String [] strings = tw.convertToStrings();
					cell.setTextViewContents(strings);
					tw.dirty = false;
				}
			}
		}
	}

	/**
	 * Class to save a cell's text in a new thread.
	 */
	protected static class SaveCellText extends Job
	{
		TextWindow tw;

		protected SaveCellText(TextWindow tw)
		{
			super("Save Cell Text", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.tw = tw;
			startJob();
		}

		public void doIt()
		{
			Cell cell = tw.getCell();
			String [] strings = tw.convertToStrings();
			cell.setTextViewContents(strings);
			tw.dirty = false;
		}
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
		this.cell = cell;
		String [] lines = cell.getTextViewContents();
		if (lines == null) return;
		textArea.setText(makeOneString(lines));
	}

	private void putTextBack()
	{
	}

	/**
	 * Method to get the text (for a textual cell) that is being edited.
	 * @param cell a text-view Cell.
	 * @return the text for the cell.
	 * If that text is not being edited (or has not changed), returns null. 
	 */
	public static String [] getEditedText(Cell cell)
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (content instanceof TextWindow)
			{
				if (content.getCell() == cell)
				{
					TextWindow tw = (TextWindow)content;
					if (tw.dirty)
					{
						String [] strings = tw.convertToStrings();
						return strings;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Method to update text for a cell (if it is being displayed).
	 * @param cell the Cell whose text changed.
	 * @param strings the new text for that cell.
	 */
	public static void updateText(Cell cell, String [] strings)
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (content instanceof TextWindow)
			{
				if (content.getCell() == cell)
				{
					TextWindow tw = (TextWindow)content;
					if (!tw.finishing)
					{
						tw.textArea.setText(makeOneString(strings));
						tw.dirty = false;
					}
				}
			}
		}
	}

	/**
	 * Method to convert an array of strings to a single string.
	 * @param strings the array of strings.
	 * @return the single string.
	 */
	private static String makeOneString(String [] strings)
	{
		StringBuffer sb = new StringBuffer();
		int len = strings.length;
		for(int i=0; i<len; i++)
		{
			sb.append(strings[i]);
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Method to convert the document in this window to an array of strings.
	 * @return an array of strings with the current text.
	 */
	private String [] convertToStrings()
	{
		String entire = textArea.getText();
		int len = entire.length();
		int total = 0;
		char lastCh = 0;
		for(int i=0; i<len; i++)
		{
			lastCh = entire.charAt(i);
			if (lastCh == '\n') total++;
		}
		if (lastCh != '\n') total++;

		String [] strings = new String[total];
		int fill = 0;
		int start = 0;
		for(int i=0; i<len; i++)
		{
			lastCh = entire.charAt(i);
			if (lastCh == '\n')
			{
				strings[fill] = entire.substring(start, i);
				start = i+1;
				fill++;
			}
		}
		if (start < len) strings[fill] = entire.substring(start, len);
		return strings;
	}

	public void rightScrollChanged(int value)
	{
	}

	public void bottomScrollChanged(int value)
	{
	}

	public void requestRepaint() {  }

	/** Returns true if we can go back in history list, false otherwise */
	public boolean cellHistoryCanGoBack() { return false; }

	/** Returns true if we can go forward in history list, false otherwise */
	public boolean cellHistoryCanGoForward() { return false; }

	public void cellHistoryGoBack() {}

	public void cellHistoryGoForward() {}

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
